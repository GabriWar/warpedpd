/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package xyz.gabriwar.warpedpixeldungeon.tiles;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.StormStrikes;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.gltextures.SmartTexture;
import com.watabou.glwrap.Quad;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

import java.nio.Buffer;
import java.nio.FloatBuffer;

/**
 * The wind in the world's trees, bushes and tall grass. A swaying tile's top shifts sideways by
 * whole pixels, so the art stays crisp, while its foot stays put: a pine's top nine rows, an oak's
 * crown whole (its trunk, on the cell below, stands still), the top half of a bush or a tuft of
 * tall grass. Two pixels out, the lower half of the moving rows goes one, so the tree bends.
 *
 * The sway is a wave running downwind over the land: a cell's phase is its place along the wind
 * (the wind's direction taken to the nearest of the eight compass steps, so the place is a whole
 * number of cells and the pattern is the world's, unmoved by a window slide), a little of its own,
 * less how far the waves have run. Its offset is that sine rounded, and the sine peaks on whole
 * pixels: as the run only ever grows, a tile steps through its offsets in turn and never flips
 * between two. The run is read TICKS times a second, so a tile changes a few times a second at
 * most. Nothing moves below STILL m/s; a breeze sways a pixel either way; from STRONG m/s, and in a
 * storm, two - leaning downwind where the wind crosses the screen, two either way (swung half as
 * often) where it blows straight up or down it. The waves run faster in a stronger wind and in the
 * gusts (WeatherSprites.gust). Only under an open sky (Fx.openSky), and a cell nobody has seen
 * stands still, so no top leans out of the fog that hides it.
 *
 * A tilemap holding such tiles (OverworldDress.Layer's, TerrainFeaturesTilemap, RaisedTerrainTilemap)
 * leaves them out of its chunks and draws them through a Batch right after them, at its own depth.
 * Visual only: a co-op guest's world sways by the host's wind as it does on the host. Render thread.
 */
public final class WindSway {

	private WindSway(){}

	//m/s: still below the first, a breeze up to the second, strong from it; a step up comes a little past
	//its threshold and a step down a little short of it
	static final float STILL = 2f, STRONG = 8f, HYSTERESIS = 0.4f;
	//degrees past the edge of the wind's compass step before the waves turn to the next one
	static final float TURN = 10f;
	//cells from one crest to the next along the wind, and how far a cell's own phase strays, radians
	static final int WAVE = 8;
	static final double OWN = 1.0;
	//the waves' pace, cycles a second: quicker the stronger the wind, swelling with its gusts
	static final float MIN_HZ = 0.5f, MAX_HZ = 1.2f, GUST = 0.3f;
	//how often a frame's offsets are worked out afresh
	static final int TICKS = 20;

	//the eight compass steps the wind can run along, north first and clockwise: screen x, screen y
	private static final int[] STEP_X = { 0, 1, 1, 1, 0, -1, -1, -1 };
	private static final int[] STEP_Y = { -1, -1, 0, 1, 1, 1, 0, -1 };

	//the sway every batch draws this frame: how hard, which way, how far the waves have run (as read
	//at the last tick), and a stamp bumped whenever any of it changed
	private static int strength;
	private static int step = -1;
	private static double travel, shown;
	private static float sinceTick;
	private static int stamp;

	/** Once a frame, after Fx.frame (GameScene.update): the wind looked up for every batch. */
	public static void frame(){
		boolean open = Fx.openSky();
		float wind = open ? ClimateManager.localWindSpeed() : 0f;
		int s = strengthFor( strength, wind, open && ClimateManager.isStorming() );
		int d = stepFor( step, ClimateManager.surfaceWindDir() );
		//time stopped (an hourglass, a swiftthistle) holds the trees with everything else
		if (s > 0 && !Emitter.freezeEmitters){
			double hz = Math.min( MAX_HZ, Math.max( MIN_HZ, 0.45f + 0.035f * wind ) * (1f + GUST * WeatherSprites.gust()) );
			//two pixels either way (a strong wind up or down the screen) is twice the steps a swing:
			//swung half as often, a tile steps as often as in any other wind
			if (s == 2 && STEP_X[d] == 0) hz /= 2;
			travel = (travel + 2 * Math.PI * hz * Game.elapsed) % (2 * Math.PI);
			sinceTick += Game.elapsed;
		}
		//whatever changed the offsets, the next change waits a whole tick
		if (s != strength || d != step || sinceTick >= 1f / TICKS){
			strength = s;
			step = d;
			shown = travel;
			sinceTick = 0;
			stamp++;
		}
	}

	/**
	 * How hard the wind sways things: 0 still, 1 a breeze, 2 strong (a storm always is). A wind
	 * hovering on a threshold keeps what it had.
	 */
	static int strengthFor( int now, float wind, boolean storm ){
		if (storm) return 2;
		int up = wind >= STRONG + HYSTERESIS ? 2 : wind >= STILL + HYSTERESIS ? 1 : 0;
		int down = wind >= STRONG - HYSTERESIS ? 2 : wind >= STILL - HYSTERESIS ? 1 : 0;
		return up > now ? up : down < now ? down : now;
	}

	/**
	 * Which of the eight compass steps (0 north, clockwise) the waves run along, for a wind blowing
	 * TOWARDS dir degrees: the nearest, but the one they had until the wind is well into the next.
	 */
	static int stepFor( int now, float dir ){
		int nearest = Math.floorMod( Math.round( dir / 45f ), 8 );
		if (now < 0 || nearest == now) return nearest;
		float off = Math.abs( ((dir - now * 45f) % 360f + 540f) % 360f - 180f );
		return off > 22.5f + TURN ? nearest : now;
	}

	/** The sway of a tile on world cell (wx, wy) this frame, whole pixels across the screen. */
	static int offsetAt( int wx, int wy ){
		return step < 0 ? 0 : offset( strength, STEP_X[step], STEP_Y[step], shown, wx, wy );
	}

	/**
	 * The sway of a tile on world cell (wx, wy), whole pixels across the screen (east positive), for
	 * a wind of this strength running along (dx, dy) whose waves have run travel radians.
	 */
	static int offset( int strength, int dx, int dy, double travel, int wx, int wy ){
		if (strength <= 0) return 0;
		double s = Math.sin( Math.floorMod( wx * dx + wy * dy, WAVE ) * (2 * Math.PI / WAVE) + own( wx, wy ) - travel );
		if (strength == 1) return (int) Math.round( s );
		//strong: a pixel's sway about a pixel's lean downwind, or two either way in a wind blowing
		//straight up or down the screen
		return dx != 0 ? dx + (int) Math.round( s ) : (int) Math.round( 2 * s );
	}

	//a cell's own share of the phase, so the crests are not ruled lines
	private static double own( int wx, int wy ){
		int h = wx * 0x27D4EB2D ^ wy * 0x165667B1;
		h ^= h >>> 15;
		h *= 0x2C1B3C6D;
		h ^= h >>> 12;
		return ((h & 0xFF) / 255.0 - 0.5) * OWN;
	}

	/**
	 * Writes one swaying tile at (x, y) as quads into out, 16 floats each from the start, and returns
	 * how many: the top `rows` of its rows shifted s pixels across (at two, the lower half of them
	 * one), the rest where the tile stands. Still, it is the tile whole.
	 */
	static int quads( float[] out, float x, float y, RectF uv, int rows, int s ){
		float v = (uv.bottom - uv.top) / SIZE;
		int n = 0;
		if (s == 0) return put( out, 0, x, y, 0, SIZE, uv, v );
		if (s == 1 || s == -1){
			n = put( out, n, x + s, y, 0, rows, uv, v );
		} else {
			int mid = rows / 2;
			n = put( out, n, x + s, y, 0, mid, uv, v );
			n = put( out, n, x + Integer.signum( s ), y, mid, rows, uv, v );
		}
		if (rows < SIZE) n = put( out, n, x, y, rows, SIZE, uv, v );
		return n;
	}

	//rows [from, to) of a tile drawn at (x, y), as quad n; returns n + 1
	private static int put( float[] out, int n, float x, float y, int from, int to, RectF uv, float v ){
		int i = n * 16;
		float y1 = y + from, y2 = y + to, v1 = uv.top + from * v, v2 = uv.top + to * v;
		out[i] = x;           out[i+1] = y1;  out[i+2] = uv.left;   out[i+3] = v1;
		out[i+4] = x + SIZE;  out[i+5] = y1;  out[i+6] = uv.right;  out[i+7] = v1;
		out[i+8] = x + SIZE;  out[i+9] = y2;  out[i+10] = uv.right; out[i+11] = v2;
		out[i+12] = x;        out[i+13] = y2; out[i+14] = uv.left;  out[i+15] = v2;
		return n + 1;
	}

	private static final int SIZE = DungeonTilemap.SIZE;

	/** A batch for a tilemap of the level being shown; null where no wind blows on its tiles. */
	public static Batch batch( int[] rows ){
		return StormStrikes.underOpenSky( Dungeon.level ) ? new Batch( rows ) : null;
	}

	/**
	 * The swaying tiles of one tilemap. rows[tile]: how many of a tile's top rows sway, 0 for a tile
	 * that stands still (the tilemap draws those itself). The quads are built again when the wave
	 * ticks, the camera leaves the cells they were built for, or the map's tiles there change; in
	 * between, the last ones are drawn again.
	 */
	public static final class Batch {

		//cells around the view: a top two pixels over the edge of a cell just out of it, the camera's shake
		private static final float PAD = 2 * SIZE;
		//cells more built than the view needs, so a moving camera does not build every frame
		private static final int MARGIN = 2;

		private final int[] rows;
		private final RectF[] uvs;

		private FloatBuffer quads;
		private Vertexbuffer buffer;
		private int capacity, count;
		private final float[] scratch = new float[3 * 16];

		//what the quads were built from: the stamp, the window's place in the world, the map's shape,
		//the cells (right and bottom exclusive) and their tiles, row by row
		private int builtStamp = -1, builtX, builtY, builtW, builtLen;
		private int bx0, by0, bx1, by1;
		private int[] seen = new int[0];

		Batch( int[] rows ){
			this.rows = rows;
			uvs = new RectF[rows.length];
		}

		/** Whether a tile sways: its tilemap leaves it to the batch. */
		public boolean moves( int tile ){
			return tile >= 0 && tile < rows.length && rows[tile] > 0;
		}

		/** Draws the swaying tiles of a map after its own (its model matrix, script, texture, frames and tiles). */
		public void draw( Visual map, float[] matrix, NoosaScript script, SmartTexture texture,
		                  TextureFilm film, int[] data, int w, int h ){
			if (data == null || !(Dungeon.level instanceof OverworldLevel)) return;
			OverworldLevel level = (OverworldLevel) Dungeon.level;
			int x0 = 0, y0 = 0, x1 = w, y1 = h;
			Camera c = map.camera();
			if (c != null && map.angle == 0 && map.scale.x == 1f && map.scale.y == 1f){
				float vx = c.scroll.x - map.x, vy = c.scroll.y - map.y;
				x0 = Math.max( 0, (int) Math.floor( (vx - PAD) / SIZE ) );
				y0 = Math.max( 0, (int) Math.floor( (vy - PAD) / SIZE ) );
				x1 = Math.min( w, (int) Math.floor( (vx + c.width + PAD) / SIZE ) + 1 );
				y1 = Math.min( h, (int) Math.floor( (vy + c.height + PAD) / SIZE ) + 1 );
				if (x0 >= x1 || y0 >= y1) return;
			}
			//mid-slide the level is in the next window while the map still shows this one
			//(OverworldLevel.rebase): the quads built before hold until the scene moves too
			if (!level.fogHeld() && (builtStamp != stamp || builtX != level.worldX || builtY != level.worldY
					|| builtW != w || builtLen != data.length
					|| x0 < bx0 || y0 < by0 || x1 > bx1 || y1 > by1 || changed( data, w ))){
				build( level, film, data, w, h, x0, y0, x1, y1 );
			}
			if (count == 0) return;
			texture.bind();
			script.lighting( map.rm, map.gm, map.bm, map.am, map.ra, map.ga, map.ba, map.aa );
			script.camera( map.camera );
			script.uModel.valueM4( matrix );
			script.drawQuadSetLarge( buffer, count );
		}

		//whether the map's tiles over the built cells are not the ones the quads were built from
		private boolean changed( int[] data, int w ){
			int i = 0;
			for (int y = by0; y < by1; y++){
				for (int p = y * w + bx0, end = y * w + bx1; p < end; p++){
					if (data[p] != seen[i++]) return true;
				}
			}
			return false;
		}

		void build( OverworldLevel level, TextureFilm film, int[] data, int w, int h, int x0, int y0, int x1, int y1 ){
			bx0 = Math.max( 0, x0 - MARGIN );
			by0 = Math.max( 0, y0 - MARGIN );
			bx1 = Math.min( w, x1 + MARGIN );
			by1 = Math.min( h, y1 + MARGIN );
			builtStamp = stamp;
			builtX = level.worldX;
			builtY = level.worldY;
			builtW = w;
			builtLen = data.length;
			int area = (bx1 - bx0) * (by1 - by0);
			if (seen.length < area) seen = new int[area];
			//the tiles are read once: the actor thread may change one under the build
			int moving = 0, i = 0;
			for (int y = by0; y < by1; y++){
				for (int p = y * w + bx0, end = y * w + bx1; p < end; p++){
					int t = data[p];
					seen[i++] = t;
					if (moves( t )) moving++;
				}
			}
			count = 0;
			if (moving == 0) return;
			//three quads a tile at most: the top, the band under it, the foot
			if (capacity < moving * 3){
				capacity = Math.max( 64, moving * 4 );
				quads = Quad.createSet( capacity );
				if (buffer != null){
					buffer.delete();
					buffer = null;
				}
			}
			((Buffer) quads).position( 0 );
			i = 0;
			for (int y = by0; y < by1; y++){
				for (int x = bx0; x < bx1; x++){
					int t = seen[i++];
					if (!moves( t )) continue;
					RectF uv = uvs[t];
					if (uv == null){
						uv = film.get( t );
						if (uv == null) continue;
						uvs[t] = uv;
					}
					int cell = x + y * w;
					int s = known( level, cell ) ? offsetAt( level.worldX + x, level.worldY + y ) : 0;
					int n = quads( scratch, x * SIZE, y * SIZE, uv, rows[t], s );
					quads.put( scratch, 0, n * 16 );
					count += n;
				}
			}
			((Buffer) quads).position( 0 );
			if (count == 0) return;
			if (buffer == null) buffer = new Vertexbuffer( quads );
			else buffer.updateVertices( quads, 0, count * 16 );
		}

		//a cell the hero has seen, or one never fogged: anything else stays as still as its fog
		private static boolean known( OverworldLevel level, int cell ){
			boolean[] v = level.visited, m = level.mapped;
			return level.plainFogAt( cell )
					|| (v != null && cell < v.length && v[cell])
					|| (m != null && cell < m.length && m[cell]);
		}

		/** The tilemap is gone: so is the vertex buffer. */
		public void destroy(){
			if (buffer != null){
				buffer.delete();
				buffer = null;
			}
			count = 0;
			builtStamp = -1;
		}

		//what the last build laid down, for the tests: quads, and their vertices
		int count(){ return count; }
		FloatBuffer vertices(){ return quads; }
	}
}

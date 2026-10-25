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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WetlandSites;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Quad;
import com.watabou.glwrap.Texture;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;

import java.nio.Buffer;
import java.nio.FloatBuffer;

/**
 * The swamp's low mist (levels/overworld/WetlandSites): a sheet of it lying on every swamp cell in
 * view - over the ground, under everyone standing in it, the trees' crowns and the walls above it -
 * and thinning out over the two cells round the swamp's edge, so it never ends on a ruler line. Two
 * layers of one seamless tile (effects/swamp_mist.png, tools/assets/wetland_art.py), the deeper one
 * further along it, fainter and slower, drift with the wind on whole texels: the mist creeps, it
 * never flickers. How thick it lies follows the hour (WetlandSites.mistDensity: a haze by day,
 * thick through the night, thickest at dawn), eased over a few seconds.
 *
 * Its cells are the window's, laid where the scene draws that window now: the overworld's window
 * slide moves it with the floor layer until the window and the scene agree again (the sheet's own
 * place is the sentinel: it lies at 0, 0 when laid, so a slide shows as an offset), and a network
 * mirror's re-labelled window is followed by hand. Render thread only; nothing of it is saved.
 */
public class SwampMist extends Gizmo {

	/** The mist's colour, as lit (the tile is white): a pale grey-green. */
	public static final float R = 0.84f, G = 0.9f, B = 0.82f;
	/** The near and the deep layer's alpha where the mist lies its thickest. */
	public static final float NEAR = 0.34f, DEEP = 0.24f;
	/** A cell's share of the mist by how far it lies off the swamp: on it, beside it, two out. */
	public static final float[] SHARE = { 1f, 0.5f, 0.2f };
	//how fast the shown thickness eases toward the hour's, per second
	private static final float EASE = 0.25f;
	//how often the cells are laid again at the least, seconds (a cell changing biome with a window)
	private static final float RELAY = 1f;

	private final OverworldLevel level;
	private final Sheet sheet = new Sheet();
	//the world cell the scene draws at its cell (0, 0)
	private int visX, visY;
	private int laidX0 = Integer.MIN_VALUE, laidY0, laidX1, laidY1, laidVersion = -1;
	private float relayIn;

	public SwampMist( OverworldLevel level ){
		this.level = level;
		visX = level.worldX();
		visY = level.worldY();
	}

	/** How strong the mist's sheet lies for this much mist (0..1): its thickness itself. */
	public static float shown( float density ){
		return Math.max( 0f, Math.min( 1f, density ) );
	}

	@Override
	public void update(){
		super.update();
		if (Dungeon.level != level || level.altitude() != 0) return;
		if (sheet.parent == null){
			GameScene.floorEffect( sheet );
			if (sheet.parent == null) return;
			sheet.parent.sendToBack( sheet );
		}
		//a slide of the window under the scene moved the sheet with the floor: the scene's origin moved
		if (sheet.x != 0f || sheet.y != 0f){
			visX -= Math.round( sheet.x / DungeonTilemap.SIZE );
			visY -= Math.round( sheet.y / DungeonTilemap.SIZE );
			laidX0 = Integer.MIN_VALUE;
		}
		//a network mirror re-labels its window without sliding the scene: the ground moved under it
		if (NetManager.isNetClient() && (level.worldX() != visX || level.worldY() != visY)){
			sheet.x += (visX - level.worldX()) * DungeonTilemap.SIZE;
			sheet.y += (visY - level.worldY()) * DungeonTilemap.SIZE;
			visX = level.worldX();
			visY = level.worldY();
			laidX0 = Integer.MIN_VALUE;
		}
		if (Emitter.freezeEmitters) return;
		float dt = Game.elapsed;
		//the hour's thickness, eased
		float want = shown( WetlandSites.mistDensity( DayNightCycle.phase(), DayNightCycle.phaseProgress() ) );
		sheet.shown += Math.max( -EASE * dt, Math.min( EASE * dt, want - sheet.shown ) );
		//the wind's drift, a little slower than the clouds'
		double rad = Math.toRadians( ClimateManager.surfaceWindDir() );
		float pace = Math.max( 1f, Math.min( 6f, 1f + 0.3f * ClimateManager.localWindSpeed() ) );
		sheet.drift( (float)Math.sin( rad ) * pace * dt, -(float)Math.cos( rad ) * pace * dt );

		Camera cam = Camera.main;
		if (cam == null || level.worldX() != visX || level.worldY() != visY) return;
		int w = level.width(), h = level.height();
		int x0 = Math.max( 1, (int)(cam.scroll.x / DungeonTilemap.SIZE) - 2 );
		int y0 = Math.max( 1, (int)(cam.scroll.y / DungeonTilemap.SIZE) - 2 );
		int x1 = Math.min( w - 2, (int)((cam.scroll.x + cam.width) / DungeonTilemap.SIZE) + 2 );
		int y1 = Math.min( h - 2, (int)((cam.scroll.y + cam.height) / DungeonTilemap.SIZE) + 2 );
		relayIn -= dt;
		if (x0 == laidX0 && y0 == laidY0 && x1 == laidX1 && y1 == laidY1
				&& level.windowVersion() == laidVersion && relayIn > 0) return;
		relayIn = RELAY;
		laidX0 = x0;
		laidY0 = y0;
		laidX1 = x1;
		laidY1 = y1;
		laidVersion = level.windowVersion();
		sheet.cover( level, x0, y0, x1, y1, visX, visY );
	}

	@Override
	public void destroy(){
		super.destroy();
		sheet.killAndErase();
		sheet.destroy();
	}

	/** How far off the swamp a window cell lies (0 on it, 1 or 2 beside it), -1 beyond: the
	 *  nearest swamp cell within two, Chebyshev. Pure over the level's biome map. */
	public static int reach( OverworldLevel level, int x, int y ){
		int w = level.width(), h = level.height();
		if (level.swampAt( x + y * w )) return 0;
		int best = -1;
		for (int r = 1; r <= 2 && best == -1; r++){
			for (int dy = -r; dy <= r && best == -1; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int nx = x + dx, ny = y + dy;
					if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
					if (level.swampAt( nx + ny * w )){
						best = r;
						break;
					}
				}
			}
		}
		return best;
	}

	//the picture: the tile over every misty cell laid, in two layers
	private static final class Sheet extends Visual {

		//the tile's side, pixels: a power of two (it repeats), dividing a window slide's 512
		private static final int T = 128;
		//the deep layer's place along the tile and its pace against the near one
		private static final int DEEP_U = 61, DEEP_V = 37;
		private static final float DEEP_PACE = 0.55f;

		private SmartTexture texture;
		//the cells laid, by how far off the swamp (SHARE's index): window cell indices
		private int[][] cells = { new int[64], new int[64], new int[64] };
		private final int[] counts = new int[3];
		private int w, originX, originY;
		private float driftX, driftY;
		//its thickness now (SwampMist eases it)
		float shown;
		private int du = Integer.MIN_VALUE, dv, deepU, deepV;
		private boolean dirty;

		private final float[] vertices = new float[16];
		private FloatBuffer quads;
		private Vertexbuffer buffer;
		private int capacity;
		//what was built: each class's quads per layer
		private final int[] built = new int[3];

		Sheet(){
			super( 0, 0, 0, 0 );
		}

		//no size of its own (its cells are laid where the scene draws them): the visual's own
		//culling would see a point at the window's origin
		@Override
		public boolean isVisible(){
			return visible && camera() != null;
		}

		void drift( float dx, float dy ){
			driftX = (driftX + dx) % (T * 64);
			driftY = (driftY + dy) % (T * 64);
			int nu = Math.floorMod( (int)Math.floor( driftX ), T ), nv = Math.floorMod( (int)Math.floor( driftY ), T );
			int ndu = Math.floorMod( (int)Math.floor( driftX * DEEP_PACE ), T ), ndv = Math.floorMod( (int)Math.floor( driftY * DEEP_PACE ), T );
			if (nu != du || nv != dv || ndu != deepU || ndv != deepV){
				du = nu;
				dv = nv;
				deepU = ndu;
				deepV = ndv;
				dirty = true;
			}
		}

		//the misty cells of the window rect [x0..x1] x [y0..y1], laid for the window at (ox, oy)
		void cover( OverworldLevel level, int x0, int y0, int x1, int y1, int ox, int oy ){
			java.util.Arrays.fill( counts, 0 );
			w = level.width();
			for (int y = y0; y <= y1; y++){
				for (int x = x0; x <= x1; x++){
					int k = reach( level, x, y );
					if (k < 0) continue;
					if (counts[k] == cells[k].length) cells[k] = java.util.Arrays.copyOf( cells[k], cells[k].length * 2 );
					cells[k][counts[k]++] = x + y * w;
				}
			}
			originX = ox;
			originY = oy;
			x = y = 0f;
			dirty = true;
		}

		private void build(){
			int per = counts[0] + counts[1] + counts[2];
			int need = 2 * per;
			if (capacity < need){
				capacity = Math.max( 64, need + need / 4 );
				quads = Quad.createSet( capacity );
				if (buffer != null){
					buffer.delete();
					buffer = null;
				}
			}
			((Buffer)quads).position( 0 );
			final int S = DungeonTilemap.SIZE;
			//the deep layer first, drawn first; each layer its cells on the swamp, then the two rims
			for (int layer = 0; layer < 2; layer++){
				int u0 = layer == 0 ? DEEP_U - deepU : -du, v0 = layer == 0 ? DEEP_V - deepV : -dv;
				for (int k = 0; k < 3; k++){
					for (int i = 0; i < counts[k]; i++){
						int c = cells[k][i];
						quad( (c % w) * S, (c / w) * S, u0, v0 );
					}
				}
			}
			((Buffer)quads).position( 0 );
			if (buffer == null) buffer = new Vertexbuffer( quads );
			else buffer.updateVertices( quads, 0, need * 16 );
			System.arraycopy( counts, 0, built, 0, 3 );
			dirty = false;
		}

		//a cell's square at window pixels (x1, y1), showing the tile at its world pixels moved by
		//(u0, v0): the same mist over the same bog in every window
		private void quad( int x1, int y1, int u0, int v0 ){
			final int S = DungeonTilemap.SIZE;
			float u = Math.floorMod( originX * S + x1 + u0, T ) / (float)T;
			float v = Math.floorMod( originY * S + y1 + v0, T ) / (float)T;
			float u2 = u + S / (float)T, v2 = v + S / (float)T;
			vertices[0] = x1;      vertices[1] = y1;      vertices[2] = u;   vertices[3] = v;
			vertices[4] = x1 + S;  vertices[5] = y1;      vertices[6] = u2;  vertices[7] = v;
			vertices[8] = x1 + S;  vertices[9] = y1 + S;  vertices[10] = u2; vertices[11] = v2;
			vertices[12] = x1;     vertices[13] = y1 + S; vertices[14] = u;  vertices[15] = v2;
			quads.put( vertices );
		}

		@Override
		public void draw(){
			if (shown <= 0.004f || counts[0] + counts[1] + counts[2] == 0) return;
			if (texture == null){
				texture = TextureCache.get( Assets.Effects.SWAMP_MIST );
				texture.wrap( Texture.REPEAT, Texture.REPEAT );
			}
			super.draw();
			if (dirty || buffer == null) build();
			NoosaScript script = NoosaScript.get();
			texture.bind();
			script.camera( camera() );
			script.uModel.valueM4( matrix );
			int per = built[0] + built[1] + built[2];
			for (int layer = 0; layer < 2; layer++){
				float a = shown * (layer == 0 ? DEEP : NEAR);
				int at = layer * per;
				for (int k = 0; k < 3; k++){
					if (built[k] > 0){
						script.lighting( R, G, B, a * SHARE[k], 0, 0, 0, 0 );
						script.drawQuadSet( buffer, built[k], at );
					}
					at += built[k];
				}
			}
		}

		@Override
		public void destroy(){
			super.destroy();
			if (buffer != null){
				buffer.delete();
				buffer = null;
			}
		}
	}
}

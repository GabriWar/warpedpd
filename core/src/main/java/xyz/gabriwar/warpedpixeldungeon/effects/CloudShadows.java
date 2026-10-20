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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxCurves;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.glwrap.Quad;
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
 * The clouds' shadows drifting over the surface (levels/overworld/OverworldLevel.addVisuals): the
 * sea of clouds' own deck (CloudDeck, the cumulus the peaks look down on, here seen from below)
 * laid over the land in world pixels as a dark cool shade (COLOR, at most SHADE: about a sixth
 * darker), drifting with the wind at the sea of clouds' pace (PeakLife.clouds). The weather decides
 * how strong it lies: none under a clear sky, rising through a fair one, whole under a partly
 * cloudy one and gone again as the sky greys over (under a full overcast the whole land lies in
 * the one shade); none by night, in rain or fog, or while the sun is eclipsed. Every change eases
 * over a couple of seconds; the deck is painted for the game's seed the first time a shadow is
 * wanted, and nothing shows until it is.
 *
 * Its picture lies over everything standing on the land (the ground, the trees and their crowns,
 * walls and roofs, everyone on it) and under the critters, the gases, the light and the weather:
 * at the back of the scene's effects, as the peaks' eagle's shadow is. On the floor it would shade
 * a tree's body and leave its crown, drawn over the hero, in the sun. The level's visuals are made
 * before that layer is, so this is a Gizmo among them that lays its picture there on its first
 * update and drives it. The picture is laid from the camera as it draws (so it never lags the
 * view) and samples the deck at the world pixel under each of its own (the window's origin plus
 * the scene's), whole texels only: the art stays pixel art and a sliding window's rebase, which
 * moves the window 512 px (one deck) at a time, moves nothing. Render thread only.
 */
public class CloudShadows extends Gizmo {

	/** The shade's colour (the ad's cloud shadows) and its alpha at its strongest. */
	public static final int COLOR = 0x20243A;
	public static final float SHADE = 0.16f;
	/** The cloud cover (ClimateManager.cloudCover) it starts to show at, is whole from and to, and is gone by. */
	public static final float COVER_FROM = 0.08f, COVER_FULL = 0.32f, COVER_GREY = 0.6f, COVER_GONE = 0.85f;
	/** The rain (ClimateManager.localPrecipRate) that has wiped it out. */
	public static final float RAIN_GONE = 0.1f;
	/** How fast it eases toward the strength it should have, alpha a second: none to whole in 2 s. */
	public static final float EASE = SHADE / 2f;

	private final OverworldLevel level;
	private final Shade shade;

	public CloudShadows( OverworldLevel level ){
		this.level = level;
		shade = new Shade( level, Dungeon.seed );
	}

	// ------------------------------------------------------------ the rules

	/** How strongly the clouds shade the land under this much cover, 0..1: none under a clear sky,
	 *  rising through a fair one, whole through a partly cloudy one, gone as it greys over. */
	public static float coverShare( float cover ){
		return FxCurves.smooth( (cover - COVER_FROM) / (COVER_FULL - COVER_FROM) )
				* (1f - FxCurves.smooth( (cover - COVER_GREY) / (COVER_GONE - COVER_GREY) ));
	}

	/** The sun's share by the hour, 0..1: none at night, rising through the dawn's last part (the
	 *  sun clearing the horizon), whole by day, gone by the first part of the dusk. */
	public static float daylight( Phase phase, float progress ){
		switch (phase){
			case DAWN: return FxCurves.smooth( (progress - 0.6f) / 0.4f );
			case DAY:  return 1f;
			case DUSK: return 1f - FxCurves.smooth( progress / 0.4f );
			default:   return 0f;
		}
	}

	/** How strong the shade lies, 0..1: the clouds' share by the sun's, none in rain, fog or an eclipse. */
	public static float strength( float cover, float rain, boolean foggy, boolean eclipse, Phase phase, float progress ){
		if (foggy || eclipse) return 0f;
		return coverShare( cover ) * daylight( phase, progress ) * (1f - FxCurves.smooth( rain / RAIN_GONE ));
	}

	/** How fast the clouds drift, scene pixels a second, in a wind of `wind` m/s: the sea of clouds' pace. */
	public static float pace( float wind ){
		return Math.max( 1.5f, Math.min( 12f, 1.5f + 0.6f * wind ) );
	}

	/** Where the deck starts over the scene, mod its size: the texel under scene pixel s is
	 *  (s + phase) mod CloudDeck.SIZE, for the window's origin cell and the clouds' drift. */
	public static int phase( int originCell, float drift ){
		return Math.floorMod( originCell * DungeonTilemap.SIZE - (int)Math.floor( drift ), CloudDeck.SIZE );
	}

	// ------------------------------------------------------------ the life

	@Override
	public void update(){
		super.update();
		if (Dungeon.level != level) return;
		if (shade.parent == null){
			GameScene.effect( shade );
			if (shade.parent != null) shade.parent.sendToBack( shade );
		}
		//time stands still (the hourglass, swiftthistle): so do the clouds
		if (Emitter.freezeEmitters) return;
		float dt = Game.elapsed;
		double rad = Math.toRadians( ClimateManager.surfaceWindDir() );
		float v = pace( ClimateManager.localWindSpeed() ) * dt;
		shade.drift( (float)Math.sin( rad ) * v, -(float)Math.cos( rad ) * v );
		float want = SHADE * strength( ClimateManager.cloudCover(), ClimateManager.localPrecipRate(), ClimateManager.isFoggy(),
				ClimateManager.isSolarEclipse(), DayNightCycle.phase(), DayNightCycle.phaseProgress() );
		//nothing shows before the deck is painted (it eases in once it is)
		if (want > 0f && !shade.ready()) want = 0f;
		shade.shown += Math.max( -EASE * dt, Math.min( EASE * dt, want - shade.shown ) );
	}

	@Override
	public void destroy(){
		super.destroy();
		shade.killAndErase();
		shade.destroy();
	}

	//the picture: the deck over the camera's view, flat in the shade's colour, each pixel the world's
	private static final class Shade extends Visual {

		private static final int T = CloudDeck.SIZE;
		//past the view on every side: the camera's shake and its sub-pixel scroll
		private static final int MARGIN = 8;
		private static final float R = ((COLOR >> 16) & 0xFF) / 255f, G = ((COLOR >> 8) & 0xFF) / 255f, B = (COLOR & 0xFF) / 255f;

		private final OverworldLevel level;
		private final long seed;
		private SmartTexture texture;
		//its alpha now (CloudShadows eases it)
		float shown;
		//the clouds' drift, world pixels, kept within one deck
		private float driftX, driftY;

		private final float[] vertices = new float[16];
		private FloatBuffer quads;
		private Vertexbuffer buffer;
		private int capacity, count;
		//what the quads were laid for: the view's edges and the deck's phase on both axes
		private int laidL = Integer.MIN_VALUE, laidT, laidR, laidB, laidX, laidY;

		Shade( OverworldLevel level, long seed ){
			super( 0, 0, 0, 0 );
			this.level = level;
			this.seed = seed;
		}

		//the deck, asked for until it is painted (CloudDeck paints it on a worker the first time)
		boolean ready(){
			if (texture == null){
				SmartTexture[] deck = CloudDeck.get( seed );
				if (deck != null) texture = deck[0];
			}
			return texture != null;
		}

		void drift( float dx, float dy ){
			driftX = (driftX + dx) % T;
			driftY = (driftY + dy) % T;
		}

		//no size of its own: it covers whatever the camera sees, so the visual's own culling (a
		//point at the scene's origin) is bypassed
		@Override
		public boolean isVisible(){
			return visible && camera() != null;
		}

		@Override
		public void draw(){
			Camera cam = camera();
			if (shown <= 0.004f || texture == null || cam == null || Dungeon.level != level) return;
			//the view and a margin on whole pixels, and where the deck lies over the scene now
			int l = (int)Math.floor( cam.scroll.x ) - MARGIN, t = (int)Math.floor( cam.scroll.y ) - MARGIN;
			int r = (int)Math.ceil( cam.scroll.x + cam.width ) + MARGIN, b = (int)Math.ceil( cam.scroll.y + cam.height ) + MARGIN;
			int px = phase( level.worldX(), driftX ), py = phase( level.worldY(), driftY );
			if (l != laidL || t != laidT || r != laidR || b != laidB || px != laidX || py != laidY){
				lay( l, t, r, b, px, py );
			}
			//laid by the camera and the world, never by its place: a rebase's slide of the effects is undone
			x = y = 0;
			super.draw();
			NoosaScript script = NoosaScript.get();
			texture.bind();
			script.camera( cam );
			script.uModel.valueM4( matrix );
			script.lighting( 0, 0, 0, shown, R, G, B, 0 );
			script.drawQuadSet( buffer, count, 0 );
		}

		//one quad for each repeat of the deck the view takes in, its UVs within the deck (0..1), so
		//even a phone's low-precision UVs land on whole texels
		private void lay( int l, int t, int r, int b, int px, int py ){
			int x0 = l - Math.floorMod( l + px, T ), y0 = t - Math.floorMod( t + py, T );
			int n = ((r - x0 + T - 1) / T) * ((b - y0 + T - 1) / T);
			if (capacity < n){
				capacity = n + 4;
				quads = Quad.createSet( capacity );
				if (buffer != null){
					buffer.delete();
					buffer = null;
				}
			}
			((Buffer)quads).position( 0 );
			for (int ty = y0; ty < b; ty += T){
				int y1 = Math.max( t, ty ), y2 = Math.min( b, ty + T );
				float v1 = (y1 - ty) / (float)T, v2 = (y2 - ty) / (float)T;
				for (int tx = x0; tx < r; tx += T){
					int x1 = Math.max( l, tx ), x2 = Math.min( r, tx + T );
					float u1 = (x1 - tx) / (float)T, u2 = (x2 - tx) / (float)T;
					vertices[0] = x1;  vertices[1] = y1;  vertices[2] = u1;  vertices[3] = v1;
					vertices[4] = x2;  vertices[5] = y1;  vertices[6] = u2;  vertices[7] = v1;
					vertices[8] = x2;  vertices[9] = y2;  vertices[10] = u2; vertices[11] = v2;
					vertices[12] = x1; vertices[13] = y2; vertices[14] = u1; vertices[15] = v2;
					quads.put( vertices );
				}
			}
			((Buffer)quads).position( 0 );
			if (buffer == null) buffer = new Vertexbuffer( quads );
			else buffer.updateVertices( quads, 0, n * 16 );
			count = n;
			laidL = l;
			laidT = t;
			laidR = r;
			laidB = b;
			laidX = px;
			laidY = py;
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

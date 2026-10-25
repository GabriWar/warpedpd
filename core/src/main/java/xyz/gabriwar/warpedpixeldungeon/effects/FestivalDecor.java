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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.LightRef;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * What a village hangs round its well for its festival (levels/overworld/FestivalEvent): a maypole
 * crowned with flowers on the well, and from near its top a rope to the roof of each of the
 * nearest houses, pennants and paper lanterns strung along its sag. From dusk to dawn the lanterns
 * are lit, and a warm glow hangs at each rope's middle lantern and in the crown (effects/fx
 * FxLight: lasting lights, asked for only while the kit has room). The level works out on the
 * actor thread what should be shown and posts it here, positions taken there and then; this brings
 * the scene in line on the render thread. Everything lives in the groups a window slide moves (the
 * pole and the ropes among the effects, the glows among the lights), so once hung nothing is placed
 * again; a co-op guest's mirror keeps one of its own and slides it by hand (slide).
 * tools/assets/events_life_art.py draws the parts and previews this very layout.
 */
public final class FestivalDecor {

	/** effects/festival_decor.png, 32x32: the maypole (x 0-7), four pennants (6x7 at x 8, 14, 20,
	 *  26, y 0) and the paper lanterns (5x8 at y 8: red and gold unlit at x 8 and 13, lit at 18 and 23). */
	public static final String TEXTURE = "effects/festival_decor.png";

	/** A festival as posted: its well cell's top-left in scene pixels, and each rope's end - by the
	 *  world cell it is tied over (what the rope is known by, the same whatever the window) and that
	 *  cell's top-left in scene pixels, in the same order - and whether its lanterns are lit. */
	public static final class Fete {
		final long id;
		final float wellX, wellY;
		final long[] ends;
		final float[] endXY;
		final boolean lit;
		public Fete( long id, float wellX, float wellY, long[] ends, float[] endXY, boolean lit ){
			this.id = id;
			this.wellX = wellX;
			this.wellY = wellY;
			this.ends = ends;
			this.endXY = endXY;
			this.lit = lit;
		}
	}

	/** Do two posts show the same thing (where it is aside: a window slide moves what is shown)? */
	public static boolean same( List<Fete> a, List<Fete> b ){
		if (a.size() != b.size()) return false;
		for (int i = 0; i < a.size(); i++){
			Fete x = a.get( i ), y = b.get( i );
			if (x.id != y.id || x.lit != y.lit || !Arrays.equals( x.ends, y.ends )) return false;
		}
		return true;
	}

	//the lanterns' glow: the warm orange of a candle behind paper
	private static final int GLOW = 0xFFB04A;
	private static final float GLOW_LEVEL = 0.6f;

	//one festival's pole, ropes and glows, and the ends it was strung to
	private static final class FeteFx {
		long[] ends;
		boolean lit;
		final ArrayList<Image> parts = new ArrayList<>();
		final ArrayList<LightRef> lights = new ArrayList<>();
	}

	//the scene what is shown belongs to, by identity (never the scene itself: no dead scene is kept)
	private int sceneId;
	private final HashMap<Long, FeteFx> fetes = new HashMap<>();

	/** Render thread: what is on the scene (of identity sceneId) is brought in line with a post. */
	public void show( int sceneId, List<Fete> list ){
		if (!(Game.scene() instanceof GameScene) || System.identityHashCode( Game.scene() ) != sceneId) return;
		if (this.sceneId != sceneId){
			//the old scene took everything it showed with it
			fetes.clear();
			this.sceneId = sceneId;
		}
		HashSet<Long> want = new HashSet<>();
		for (Fete f : list){
			want.add( f.id );
			FeteFx fx = fetes.get( f.id );
			if (fx != null && fx.lit == f.lit && Arrays.equals( fx.ends, f.ends )) continue;
			if (fx != null) takeDown( fx );
			fx = new FeteFx();
			fx.ends = f.ends;
			fx.lit = f.lit;
			hang( f, fx );
			fetes.put( f.id, fx );
		}
		for (Iterator<Map.Entry<Long, FeteFx>> it = fetes.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<Long, FeteFx> e = it.next();
			if (want.contains( e.getKey() )) continue;
			takeDown( e.getValue() );
			it.remove();
		}
	}

	/** Render thread: everything shown moved by (dx, dy) scene pixels (a co-op guest's mirror,
	 *  which re-labels its window without sliding the scene; the host's slide moves the groups). */
	public void slide( float dx, float dy ){
		for (FeteFx fx : fetes.values()){
			for (Image i : fx.parts){
				i.x += dx;
				i.y += dy;
			}
			for (LightRef r : fx.lights){
				FxLight l = r.get();
				if (l == null) continue;
				l.x += dx;
				l.y += dy;
			}
		}
	}

	//off the scene at once, buffers freed: an erased image keeps them till the next scene change,
	//and the surface is one long scene. the glows fade out
	private static void takeDown( FeteFx fx ){
		for (Image i : fx.parts){
			if (i.parent == null) continue;
			i.killAndErase();
			i.destroy();
		}
		fx.parts.clear();
		for (LightRef r : fx.lights) r.release( 0.5f );
		fx.lights.clear();
	}

	/**
	 * The ropes, then the pole in front of them. Each rope runs from near the pole's top to just
	 * above its roof's cell, sagging in the middle; along it pennants and lanterns take turns, a
	 * lantern at every other place, so the middle one of a lit rope carries its glow. The pole's
	 * foot stands on the ground line a character's feet stand on, its crown above the well.
	 */
	private static void hang( Fete f, FeteFx fx ){
		float ax = f.wellX + 8, ay = f.wellY - 16;
		boolean room = LightRef.room( f.ends.length + 1, f.ends.length + 1 );
		for (int k = 0; k < f.ends.length; k++){
			float bx = f.endXY[2 * k] + 8, by = f.endXY[2 * k + 1] - 4;
			float length = (float) Math.hypot( bx - ax, by - ay );
			int n = Math.max( 3, Math.round( length / 6 ) );
			float sag = 4 + length / 12;
			//the lantern nearest the rope's middle carries its glow
			int glowAt = -1;
			for (int i = 1; i < n; i++){
				if ((i + k) % 2 == 1 && (glowAt == -1 || Math.abs( 2 * i - n ) < Math.abs( 2 * glowAt - n ))) glowAt = i;
			}
			for (int i = 1; i < n; i++){
				float t = i / (float) n;
				float px = ax + (bx - ax) * t;
				float py = ay + (by - ay) * t + sag * 4 * t * (1 - t);
				Image part;
				if ((i + k) % 2 == 1){
					int colour = (i / 2 + k) % 2;
					part = new Image( TEXTURE, (f.lit ? 18 : 8) + 5 * colour, 8, 5, 8 );
					part.x = Math.round( px - 2 );
					part.y = Math.round( py );
					if (f.lit && room && i == glowAt) glow( fx, part.x + 2.5f, part.y + 5 );
				} else {
					part = new Image( TEXTURE, 8 + 6 * ((i / 2 + k) % 4), 0, 6, 7 );
					part.x = Math.round( px - 3 );
					part.y = Math.round( py - 1 );
				}
				GameScene.effect( part );
				fx.parts.add( part );
			}
		}
		Image pole = new Image( TEXTURE, 0, 0, 8, 32 );
		pole.x = f.wellX + 4;
		pole.y = f.wellY - 22;
		GameScene.effect( pole );
		fx.parts.add( pole );
		if (f.lit && room) glow( fx, f.wellX + 8, f.wellY - 19 );
	}

	private static void glow( FeteFx fx, float x, float y ){
		fx.lights.add( new LightRef().setLasting(
				FxLight.glow( x, y, 23, GLOW, GLOW_LEVEL ).flicker( FxLight.BREATH ).persistent() ) );
	}
}

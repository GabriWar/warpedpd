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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.nature;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

import java.util.IdentityHashMap;

/**
 * The nature-life-holy area's common ground: its palette (the language's NATURE, HOLY and HEAL
 * with the colours of the light each use throws), the day and the night as its effects read them,
 * the leaves' and the blades' colours, and the light layer's holders its light particles go to.
 */
public final class Nature {

	private Nature(){}

	// ------------------------------------------------------------------ the palette

	/** The colours of the light, by use: holy, healing, a spirit's, the sun's, the moon's. */
	public static final int HOLY = 0xFFE9A8, HEAL = 0x9CFFB0, SPIRIT = 0xC8E0FF, SUN = 0xFFFDE8, MOON = 0xC8D8FF;
	/** Pollen and a lantern's warm light; a sun fleck; the halo's gold; dew. */
	public static final int POLLEN = 0xFFF0A0, LANTERN = 0xFFF0A0, FLECK = 0xFFF0B0, HALO = 0xFFE9A0, DEW = 0xFFFFFF;
	/** The colour an autumn leaf turns toward as it ages. */
	public static final int AUTUMN = 0xC88A40;
	/** Seed fluff, and the pale gold of a holy mark. */
	public static final int FLUFF = 0xF4F0E0, MARK = 0xFFF0B8;
	/** The water drawn up by a well, a drip off growth. */
	public static final int WELL_DROP = 0xBFE0FF, DRIP = 0xA8D8FF;
	/** Blossom, lightest first (NATURE's bits). */
	public static final Ramp BLOSSOM = Element.NATURE.ember;
	/** The leaves of a plain cast (LeafParticle.GENERAL's: 004400 to 88CC44). */
	public static final int LEAF_DARK = 0x004400, LEAF_LIGHT = 0x88CC44;

	// ------------------------------------------------------------------ day and night

	/** Night for the area's effects: the night's tint past 0.4 (a dungeon's depth tint is 0.33). */
	public static boolean night(){
		return GameScene.nightTintAlpha() > 0.4f;
	}

	/** Day: the night's tint under 0.2. */
	public static boolean day(){
		return GameScene.nightTintAlpha() < 0.2f;
	}

	//the season, looked up at most once a frame (the calendar's date makes an array)
	private static float seasonAt = -1;
	private static GameCalendar.Season season;

	/** Autumn under an open sky: the leaves age toward AUTUMN. */
	public static boolean autumn(){
		if (!Fx.openSky()) return false;
		if (seasonAt != Game.timeTotal){
			seasonAt = Game.timeTotal;
			try {
				season = GameCalendar.season();
			} catch (RuntimeException e){
				season = null;
			}
		}
		return season == GameCalendar.Season.AUTUMN;
	}

	// ------------------------------------------------------------------ leaves and blades

	/** A colour's value scaled by k (0.9-1.1 keeps a leaf's hue and moves its shade). */
	public static int value( int c, float k ){
		int r = Math.min( 255, Math.round( ((c >> 16) & 0xFF) * k ) );
		int g = Math.min( 255, Math.round( ((c >> 8) & 0xFF) * k ) );
		int b = Math.min( 255, Math.round( (c & 0xFF) * k ) );
		return (r << 16) | (g << 8) | b;
	}

	/** Somewhere between two colours, its value +-10%. */
	public static int between( int a, int b ){
		return value( Ramp.lerp( a, b, FxRandom.Float() ), FxRandom.Float( 0.9f, 1.1f ) );
	}

	/** What a grey-shaded matter frame is lit by so its body (painted at 200 of 255) shows the
	 *  colour itself, its crown a step lighter and its underside darker: a leaf as bright as the
	 *  flat square it was. */
	public static final float BODY = 255f / 200f;

	/** A leaf of the level's own: between its two colours, lit so its body shows it. */
	public static int levelLeaf(){
		Level l = Dungeon.level;
		return value( l == null ? between( LEAF_DARK, LEAF_LIGHT ) : between( l.color1, l.color2 ), BODY );
	}

	/** A leaf of a plain cast: the lighter two thirds of 004400 to 88CC44 (the darkest end reads
	 *  as a hole on a dark floor), lit so its body shows it. */
	public static int generalLeaf(){
		return value( Nature.between( Ramp.lerp( LEAF_DARK, LEAF_LIGHT, 0.33f ), LEAF_LIGHT ), BODY );
	}

	/** A blade of the level's grass: one of three steps between its colours (matter steps), lit so
	 *  its body shows it. */
	public static int blade(){
		Level l = Dungeon.level;
		int a = l == null ? LEAF_DARK : l.color1, b = l == null ? LEAF_LIGHT : l.color2;
		return value( Ramp.lerp( a, b, 0.35f + 0.3f * FxRandom.Int( 3 ) ), BODY );
	}

	/** How a leaf ages at 70% of its life: 15% darker, or 15% of the way to AUTUMN in an autumn. */
	public static int aged( int c ){
		return autumn() ? Ramp.lerp( c, AUTUMN, 0.15f ) : value( c, 0.85f );
	}

	// ------------------------------------------------------------------ holders

	/** The light layer's holder of loose light, or `fallback` off the game's scene. */
	public static Emitter glow( Emitter fallback ){
		FxEmitter g = Fx.glowAir();
		return g != null ? g : fallback;
	}

	/** The effects layer's holder of loose matter, or `fallback` off the game's scene. */
	public static Emitter air( Emitter fallback ){
		FxEmitter a = Fx.air();
		return a != null ? a : fallback;
	}

	private static FxEmitter floor;

	/** The floor's holder of loose matter (under everyone: sprouts, petals lying), or `fallback`
	 *  off the game's scene. It never emits nor dies. Render thread. */
	public static Emitter floor( Emitter fallback ){
		if (floor == null || !floor.exists || floor.parent == null){
			floor = GameScene.floorFxEmitter();
			if (floor != null){
				floor.on = false;
				floor.autoKill = false;
			}
		}
		return floor != null ? floor : fallback;
	}

	/** A kit particle made in an emitter (its own reset run: set it up from there). */
	public static FxParticle make( Emitter e, float x, float y ){
		return ((FxParticle) e.recycle( FxParticle.class )).reset( x, y );
	}

	/** A light-sheet particle made in an emitter. */
	public static LitFrame lit( Emitter e, float x, float y ){
		return ((LitFrame) e.recycle( LitFrame.class )).reset( x, y );
	}

	private static final IdentityHashMap<Emitter.Factory, Emitter.Factory> litWraps = new IdentityHashMap<>();

	/**
	 * A light factory whose particles go to the light layer (over the night's tint) whichever
	 * emitter asks: the wrapped factory emits into the scene's holder of loose light, or into its
	 * caller's emitter off the game's scene. The same wrapper for the same factory every time.
	 */
	public static synchronized Emitter.Factory litWrap( final Emitter.Factory f ){
		Emitter.Factory w = litWraps.get( f );
		if (w == null){
			w = new Emitter.Factory(){
				@Override
				public void emit( Emitter emitter, int index, float x, float y ){
					f.emit( glow( emitter ), index, x, y );
				}

				@Override
				public boolean lightMode(){
					return true;
				}
			};
			litWraps.put( f, w );
		}
		return w;
	}
}

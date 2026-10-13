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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BanditKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MineSentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Sentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SokobanSentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogDzewa;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.MovieClip;
import com.watabou.utils.Reflection;

import java.lang.reflect.Field;
import java.util.HashMap;

//A creature's own sprite with no creature behind it, for the Descent Guide's pictures and scenes:
//it idles, and plays its attack, its walk, its death or any animation of its own when told, going
//back to idling after one that ends. Here, beside CharSprite, for its animations. Its listener is
//its own and never reads ch: CharSprite.onComplete would tell a creature that is not there its
//attack ended (ch.onAttackComplete), and an attack slot aliased to the looped idle would never end
//at all, so such a slot is not played.
public final class GuidePuppet {

	private GuidePuppet(){}

	/** The creature's own sprite with no creature behind it, idling; null if it cannot be made. */
	public static CharSprite make( Class<? extends Mob> mob ){
		CharSprite s;
		try {
			s = sprite( mob );
		} catch (Exception e){
			//no game state to build it from (the title screen), or a sprite that needs a level
			return null;
		}
		return s == null ? null : puppet( s );
	}

	/** The hero's look in a class and an armour tier, with no hero behind it, idling; null if it cannot be made. */
	public static CharSprite hero( HeroClass cls, int tier ){
		try {
			return puppet( new HeroSprite( cls, tier ) );
		} catch (Exception e){
			return null;
		}
	}

	//its listener made its own; idling
	private static CharSprite puppet( final CharSprite sprite ){
		sprite.listener = new MovieClip.Listener() {
			@Override
			public void onComplete( MovieClip.Animation anim ) {
				//a death stays on its last frame
				if (anim != sprite.idle && anim != sprite.die && !anim.looped && playable( sprite.idle )){
					sprite.play( sprite.idle, true );
				}
			}
		};
		try {
			if (playable( sprite.idle )) sprite.play( sprite.idle, true );
		} catch (Exception e){
			return null;
		}
		return sprite;
	}

	//the sprite classes of creatures whose sprite() is Mob's own: after the first, their sprite is
	//made straight from it. Making the creature is the costly part, and some draw the game's
	//Random as they are made (a tint, a pelt): the guide makes each of them once
	private static final HashMap<Class<? extends Mob>, Class<? extends CharSprite>> spriteClasses = new HashMap<>();

	//creatures whose making changes the run, drawn straight from their sprite class and never made:
	//the statues arm themselves from the game's item generator (Generator.random), a bandit king
	//takes the spork off the run, and Yog-Dzewa pushes a seeded Random to place its fists
	private static final HashMap<Class<? extends Mob>, Class<? extends CharSprite>> UNMADE = new HashMap<>();
	static {
		UNMADE.put( Sentinel.class, SentinelSprite.class );
		UNMADE.put( MineSentinel.class, SentinelSprite.class );
		UNMADE.put( SokobanSentinel.class, SokobanSentinelSprite.class );
		UNMADE.put( BanditKing.class, BanditKingSprite.class );
		UNMADE.put( YogDzewa.class, YogSprite.class );
	}

	/** The sprite class of a creature the guide must never make (see UNMADE), else null. */
	public static Class<? extends CharSprite> unmade( Class<?> mob ){
		return UNMADE.get( mob );
	}

	private static CharSprite sprite( Class<? extends Mob> mob ) throws Exception {
		Class<? extends CharSprite> cls = UNMADE.get( mob );
		if (cls != null) return Reflection.newInstanceUnhandled( cls );
		synchronized (spriteClasses){
			cls = spriteClasses.get( mob );
		}
		if (cls != null) return Reflection.newInstanceUnhandled( cls );
		Mob m = Reflection.newInstanceUnhandled( mob );
		if (m.spriteClass != null && mob.getMethod( "sprite" ).getDeclaringClass() == Mob.class){
			synchronized (spriteClasses){
				spriteClasses.put( mob, m.spriteClass );
			}
		}
		return m.sprite();
	}

	/** Plays its attack (else zap, else operate) once and goes back to idle; false when it has none distinct from idle. */
	public static boolean act( CharSprite s ){
		if (s == null) return false;
		MovieClip.Animation a = once( s, s.attack );
		if (a == null) a = once( s, s.zap );
		if (a == null) a = once( s, s.operate );
		if (a == null) return false;
		//play, never attack(cell)/zap(cell): those turn to a cell of a level and record for co-op
		s.play( a, true );
		return true;
	}

	/** What a guide scene tells a puppet to play (Demo's steps). */
	public enum Slot { IDLE, RUN, ATTACK, ZAP, OPERATE, DIE }

	/** Plays one of its animations from its start, false when it has none of its own: a walk (else the
	 *  idle) loops, an attack, zap or operate plays once and goes back to idling, a death stays on its
	 *  last frame. A slot that is the looped idle, or has no frames, is never played. */
	public static boolean play( CharSprite s, Slot slot ){
		MovieClip.Animation a;
		switch (slot){
			case IDLE:    a = playable( s.idle ) ? s.idle : null; break;
			case RUN:     a = playable( s.run ) ? s.run : playable( s.idle ) ? s.idle : null; break;
			case ATTACK:  a = once( s, s.attack ); break;
			case ZAP:     a = once( s, s.zap ); break;
			case OPERATE: a = once( s, s.operate ); break;
			default:      a = once( s, s.die ); break;
		}
		if (a == null) return false;
		//forced: the same animation again starts over, and a death played before is left behind
		s.play( a, true );
		return true;
	}

	/** Plays an animation of the sprite's own (named()), from its start; false for none. */
	public static boolean play( CharSprite s, MovieClip.Animation a ){
		if (!playable( a )) return false;
		s.play( a, true );
		return true;
	}

	/** An animation a sprite class keeps for itself, by its field's name (GooSprite's "pump",
	 *  DM300Sprite's "slam"), read from this sprite; null when it has none by that name. */
	public static MovieClip.Animation named( CharSprite s, String field ){
		for (Class<?> c = s.getClass(); c != null && c != MovieClip.class; c = c.getSuperclass()){
			try {
				Field f = c.getDeclaredField( field );
				if (f.getType() != MovieClip.Animation.class) return null;
				f.setAccessible( true );
				MovieClip.Animation a = (MovieClip.Animation) f.get( s );
				return playable( a ) ? a : null;
			} catch (NoSuchFieldException e){
				//not this class's: its parent's, maybe
			} catch (Exception e){
				return null;
			}
		}
		return null;
	}

	/** How far it stands raised off its cell's floor, in whole pixels (as a level draws it). */
	public static int raise( CharSprite s ){
		return Math.round( s.perspectiveRaise * DungeonTilemap.SIZE );
	}

	/** Its shadow on the floor under it, as a creature on a level has. */
	public static void shadow( CharSprite s ){
		s.renderShadow = true;
	}

	//an animation that plays through once and ends: not the idle, not looped, with frames to show
	private static MovieClip.Animation once( CharSprite s, MovieClip.Animation a ){
		return a != null && a != s.idle && !a.looped && playable( a ) ? a : null;
	}

	private static boolean playable( MovieClip.Animation a ){
		return a != null && a.frames != null && a.frames.length > 0
				&& a.delay > 0 && !Float.isInfinite( a.delay );
	}
}

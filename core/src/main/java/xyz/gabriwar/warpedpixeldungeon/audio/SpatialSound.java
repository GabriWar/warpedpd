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

package xyz.gabriwar.warpedpixeldungeon.audio;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.DeviceCompat;

/**
 * The game's sound effects from where they happen. While spatial sound is on (Settings, Audio)
 * a sound is panned to the side of the hero it is on and is quieter the further off it is; off,
 * every one plays exactly as it always has, centred and at its own volume. So does a sound at
 * the hero's own cell, or one with nowhere to be (no hero or level, a cell off the map, no char).
 *
 * The hero is this game's own: a guest's, their own hero; a spectator's, the host's. Callable
 * from the actor thread as from the render thread, as Sample is: it reads two cells, and with
 * room acoustics on (RoomAcoustics) the map round the hero, again once he moves or half a second
 * has gone by.
 *
 * Every game sound with a place to come from goes out through here; the interface's own, the
 * stingers (a boss, death) and what has no place (a badge, a time freeze) stay on Sample,
 * centred.
 */
public final class SpatialSound {

	private SpatialSound(){}

	/** Is spatial sound on? WPDSettings sets it at boot and from its switch. */
	public static volatile boolean on = false;

	//a sound PAN_CELLS or more to one side is panned as far as it goes, MAX_PAN: never wholly
	//into one ear (see desktop)
	public static final float PAN_CELLS = 5f, MAX_PAN = 0.8f;
	//a sound HALF_CELLS away has lost half of what distance can take from it, and none drops
	//below FLOOR: whatever the game tells by ear is still heard from across the map
	public static final float HALF_CELLS = 6f, FLOOR = 0.3f;

	/** The pan of a sound this many cells to the hero's right (negative: left): 0 while off. */
	public static float pan( float dx ){
		if (!on) return 0f;
		return Math.max( -MAX_PAN, Math.min( MAX_PAN, dx / PAN_CELLS ) );
	}

	/** How much of its volume a sound this many cells off the hero keeps: all of it while off. */
	public static float level( float dx, float dy ){
		if (!on) return 1f;
		float d = (dx * dx + dy * dy) / (HALF_CELLS * HALF_CELLS);
		return FLOOR + (1f - FLOOR) / (1f + d);
	}

	//Does this device pan as libGDX's desktop audio does? It pans a sound by moving it round
	//the listener, from right behind (pan 0) to one side (pan 1), and OpenAL Soft's stereo mix
	//makes that sound louder in all (+4.4 dB at pan 0.8) and all but silent in the far ear from
	//pan 0.7 (-45 dB at 0.8); Android's SoundPool only turns the far side down, to 1 - pan. So
	//on desktop a pan is halved and its volume eased back by sqrt(1 + pan^2): a placed sound is
	//as loud in all as the same one centred, and pan 0.8 leaves the far ear 15 dB down, as
	//Android's does 14. A test may set it.
	static boolean desktop = DeviceCompat.isDesktop();

	/** The pan to hand the device for a sound panned `pan`: as it is while spatial sound is
	 *  off, and on Android; halved on desktop (see desktop). Every placed sound, effect or
	 *  ambience, reaches the device through this and deviceVolume. */
	public static float devicePan( float pan ){
		return on && desktop ? pan * 0.5f : pan;
	}

	/** The volume to hand the device for a sound at `volume` panned `pan` (see devicePan). */
	public static float deviceVolume( float volume, float pan ){
		return on && desktop ? volume / (float)Math.sqrt( 1f + pan * pan ) : volume;
	}

	/**
	 * Of two cells a sound could come from, the one nearer the hero; -1 is none. A sound that
	 * stands for a whole area (a field of flames, a storm of sparks) comes from the nearest
	 * place it happened: one on the hero is heard right on him.
	 */
	public static int nearer( int a, int b ){
		if (a < 0) return b;
		if (b < 0) return a;
		Level l = Dungeon.level;
		Hero hero = Dungeon.hero;
		if (l == null || hero == null) return a;
		return l.trueDistance( b, hero.pos ) < l.trueDistance( a, hero.pos ) ? b : a;
	}

	/** Plays a sound from a cell of the current level. */
	public static void play( Object id, int cell ){
		playDelayed( id, 0f, cell, 1f, 1f );
	}

	public static void play( Object id, int cell, float volume ){
		playDelayed( id, 0f, cell, volume, 1f );
	}

	public static void play( Object id, int cell, float volume, float pitch ){
		playDelayed( id, 0f, cell, volume, pitch );
	}

	/** Plays a sound from where a char is: centred with no char. */
	public static void play( Object id, Char ch ){
		playDelayed( id, 0f, cellOf( ch ), 1f, 1f );
	}

	public static void play( Object id, Char ch, float volume ){
		playDelayed( id, 0f, cellOf( ch ), volume, 1f );
	}

	public static void play( Object id, Char ch, float volume, float pitch ){
		playDelayed( id, 0f, cellOf( ch ), volume, pitch );
	}

	/** Plays a sound from a cell of the current level, `delay` seconds from now. */
	public static void playDelayed( Object id, float delay, int cell ){
		playDelayed( id, delay, cell, 1f, 1f );
	}

	public static void playDelayed( Object id, float delay, int cell, float volume, float pitch ){
		Level l = Dungeon.level;
		Hero hero = Dungeon.hero;
		boolean placed = l != null && hero != null && cell >= 0 && cell < l.length()
				&& hero.pos >= 0 && hero.pos < l.length();
		float dx = 0f, dy = 0f;
		if (on && placed){
			int w = l.width();
			dx = cell % w - hero.pos % w;
			dy = cell / w - hero.pos / w;
		}
		float lvl = level( dx, dy ), p = pan( dx );
		//room acoustics plays the effects of its table itself, the room's reverb and echo with them
		if (placed && RoomAcoustics.on && RoomAcoustics.play( id, delay, cell, volume, lvl, pitch, p, l, hero.pos )) return;
		playPanned( id, delay, volume * lvl, pitch, p );
	}

	//a sound out of the hero's sight (a fight round the corner, a door opening behind him, a trap
	//going off down the hall) is heard UNSEEN_LOUD as loud as it is up to UNSEEN_NEAR cells off,
	//dying away smoothly to nothing at UNSEEN_CELLS, spatial sound on or off: no floor, as what
	//is in sight has (FLOOR)
	public static final float UNSEEN_CELLS = 10f, UNSEEN_NEAR = 2f, UNSEEN_LOUD = 0.8f;

	/** How much of its volume a sound out of sight keeps this many cells off: UNSEEN_LOUD near,
	 *  dying away smoothly (smoothstep) to nothing at UNSEEN_CELLS. */
	public static float unseen( float cells ){
		if (cells <= UNSEEN_NEAR) return UNSEEN_LOUD;
		if (cells >= UNSEEN_CELLS) return 0f;
		float t = (UNSEEN_CELLS - cells) / (UNSEEN_CELLS - UNSEEN_NEAR);
		return UNSEEN_LOUD * t * t * (3f - 2f * t);
	}

	/**
	 * Plays a sound from a cell the hero does not see (Char's blows out of sight, a door, a trap):
	 * as loud as unseen() lets it that far off, and through what stands between as room acoustics
	 * hears it (Earshot: through a door 0.8, through the rock 0.6, muffled where it has the
	 * copies; without them only that much quieter), from its side while spatial sound is on.
	 * Nothing from UNSEEN_CELLS on, nor with no place to come from.
	 */
	public static void playUnseen( Object id, int cell, float volume, float pitch ){
		Level l = Dungeon.level;
		Hero hero = Dungeon.hero;
		if (l == null || hero == null || cell < 0 || cell >= l.length() || hero.pos < 0 || hero.pos >= l.length()) return;
		int w = l.width();
		float dx = cell % w - hero.pos % w, dy = cell / w - hero.pos / w;
		float lvl = unseen( (float)Math.sqrt( dx * dx + dy * dy ) );
		if (lvl <= 0f) return;
		float p = pan( dx );
		if (RoomAcoustics.on && RoomAcoustics.play( id, 0f, cell, volume, lvl, pitch, p, l, hero.pos )) return;
		Earshot.Snapshot s = Earshot.of( l, hero.pos );
		Earshot.Relation through = s == null ? Earshot.Relation.CLEAR : s.relation( cell );
		float wall = through == Earshot.Relation.DOOR ? 0.8f : through == Earshot.Relation.WALL ? 0.6f : 1f;
		playPanned( id, 0f, volume * lvl * wall, pitch, p );
	}

	/** Plays a sound from where a char is, `delay` seconds from now: centred with no char. */
	public static void playDelayed( Object id, float delay, Char ch ){
		playDelayed( id, delay, cellOf( ch ), 1f, 1f );
	}

	public static void playDelayed( Object id, float delay, Char ch, float volume, float pitch ){
		playDelayed( id, delay, cellOf( ch ), volume, pitch );
	}

	/**
	 * Plays a sound at a volume and a pan the caller worked out itself, `delay` seconds from
	 * now: the weather's thunder, which dulls with distance its own way, or an overworld event
	 * too far off to be in the window. Its pan comes from pan(), so it is centred while off.
	 */
	public static void playPanned( Object id, float delay, float volume, float pitch, float pan ){
		out.play( id, delay, volume, pitch, pan );
	}

	private static int cellOf( Char ch ){
		return ch == null ? -1 : ch.pos;
	}

	/** Where a placed sound goes: the effects' channel. A test may listen in instead. */
	interface Out {
		void play( Object id, float delay, float volume, float pitch, float pan );
	}

	static final Out SAMPLE = ( id, delay, volume, pitch, pan ) -> {
		float v = deviceVolume( volume, pan ), p = devicePan( pan );
		float left = left( v, p ), right = right( v, p );
		if (delay > 0f) Sample.INSTANCE.playDelayed( id, delay, left, right, pitch );
		else            Sample.INSTANCE.play( id, left, right, pitch );
	};

	static Out out = SAMPLE;

	//Sample plays the louder of a left and a right volume, panned by right - left: these two
	//give back exactly this volume at exactly this pan (at pan 0, both are the volume itself)
	static float left( float volume, float pan ){
		return volume - Math.max( pan, 0f );
	}

	static float right( float volume, float pan ){
		return volume + Math.min( pan, 0f );
	}
}

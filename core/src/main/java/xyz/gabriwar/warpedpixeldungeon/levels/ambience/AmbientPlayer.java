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

package xyz.gabriwar.warpedpixeldungeon.levels.ambience;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;

/**
 * Plays an ambience sound where it happens: louder the nearer its cell is to the hero, panned
 * to the side it is on, a take picked at random and its pitch nudged so a repeat never sounds
 * like a loop. Everything goes out on the ambience channel (Sample.playAmbient), so the
 * ambience volume and switch govern it, never the effects'.
 *
 * Render-thread code: it rolls its own dice, never the dungeon's seeded generators.
 */
public final class AmbientPlayer {

	private AmbientPlayer(){}

	/** the render thread's dice */
	public static final java.util.Random RNG = new java.util.Random();

	//a source at the hero's cell plays at its full mix; at HALF_CELLS it is at half, and past
	//HEARD_CELLS it is not heard at all (the last stretch fades out rather than cutting off)
	public static final float HALF_CELLS = 5f, HEARD_CELLS = 16f;
	//a source PAN_CELLS to one side is panned as far as it goes, MAX_PAN: never fully into one ear
	public static final float PAN_CELLS = 8f, MAX_PAN = 0.75f;
	//while spatial sound is on (SpatialSound.on) the curve is the wide one: half at
	//WIDE_HALF_CELLS, and panned as far as WIDE_MAX_PAN from WIDE_PAN_CELLS to a side, as the
	//effects are: a sound pans the same on either channel
	public static final float WIDE_HALF_CELLS = 4f, WIDE_PAN_CELLS = SpatialSound.PAN_CELLS,
			WIDE_MAX_PAN = SpatialSound.MAX_PAN;
	//how far off pitch a play may be, either way
	public static final float PITCH_SPREAD = 0.06f;

	/** The level a source this many cells away is heard at: 1 at the hero, 0 from HEARD_CELLS. */
	public static float falloff( float cells ){
		if (cells <= 0) return 1f;
		if (cells >= HEARD_CELLS) return 0f;
		float d = cells / (SpatialSound.on ? WIDE_HALF_CELLS : HALF_CELLS);
		float near = 1f / (1f + d * d);
		//the last quarter of the reach fades to nothing
		float edge = Math.min( 1f, (HEARD_CELLS - cells) / (HEARD_CELLS * 0.25f) );
		return near * edge;
	}

	/** The pan of a source this many cells to the hero's right (negative: left). */
	public static float pan( float dx ){
		boolean wide = SpatialSound.on;
		float max = wide ? WIDE_MAX_PAN : MAX_PAN;
		return Math.max( -max, Math.min( max, dx / (wide ? WIDE_PAN_CELLS : PAN_CELLS) ) );
	}

	/** Is the ambience heard at all right now: on, not at zero, and the game not in the background. */
	public static boolean audible(){
		return Sample.INSTANCE.ambientAudible() && !Music.INSTANCE.paused();
	}

	/** Plays a sound with no place: the air of the floor itself, centred. */
	public static void play( AmbientSound sound, float volume ){
		play( sound, volume, 1f, 0f );
	}

	/** Plays a sound at a level, a pitch (before the random nudge) and a pan. */
	public static void play( AmbientSound sound, float volume, float pitch, float pan ){
		play( sound.takes, sound.gain, volume, pitch, pan );
	}

	/**
	 * Plays one of a sound's takes at its mix `gain`: an AmbientSound's, or one of the game's own
	 * effects heard as part of a place (the prison's chains), at a level, a pitch (before the
	 * random nudge) and a pan.
	 */
	public static void play( String[] takes, float gain, float volume, float pitch, float pan ){
		playTake( takes[RNG.nextInt( takes.length )], gain, volume, nudge( RNG, pitch ), pan );
	}

	/**
	 * Plays exactly this take at exactly this pitch: no take picked, no nudge. For a sound that
	 * must repeat the very one before it, as an echo does, its caller picks both once (nudge).
	 */
	public static void playTake( String take, float gain, float volume, float pitch, float pan ){
		if (!audible() || volume <= 0) return;
		//as the device pans (SpatialSound.devicePan): as given while spatial sound is off
		Sample.INSTANCE.playAmbient( take, SpatialSound.deviceVolume( gain * volume, pan ), pitch,
				SpatialSound.devicePan( pan ) );
	}

	/** A pitch nudged a little off, either way, on these dice: a repeat never sounds like a loop. */
	public static float nudge( java.util.Random rng, float pitch ){
		return pitch * (1f + (rng.nextFloat() * 2f - 1f) * PITCH_SPREAD);
	}

	/** The level a source at this cell of the current level is heard at (the falloff of its
	 *  distance from the hero): 0 off the map, out of reach, or with no hero. */
	public static float levelAt( int cell ){
		if (Dungeon.level == null || Dungeon.hero == null || cell < 0 || cell >= Dungeon.level.length()) return 0f;
		int w = Dungeon.level.width();
		float dx = cell % w - Dungeon.hero.pos % w, dy = cell / w - Dungeon.hero.pos / w;
		return falloff( (float)Math.sqrt( dx * dx + dy * dy ) );
	}

	/** The pan of a source at this cell of the current level: to the hero's side it lies on. */
	public static float panAt( int cell ){
		if (Dungeon.level == null || Dungeon.hero == null) return 0f;
		int w = Dungeon.level.width();
		return pan( cell % w - Dungeon.hero.pos % w );
	}

	/**
	 * Plays a sound from a cell of the current level: it falls off with the cell's distance from
	 * the hero and is panned to its side. Returns whether it was close enough to be heard.
	 */
	public static boolean playAt( AmbientSound sound, int cell, float volume ){
		return playAt( sound, cell, volume, 1f );
	}

	/** As playAt, at a pitch (before the random nudge). */
	public static boolean playAt( AmbientSound sound, int cell, float volume, float pitch ){
		return playAt( sound.takes, sound.gain, cell, volume, pitch );
	}

	/** As playAt, for one of a sound's takes at its mix `gain` (see play). */
	public static boolean playAt( String[] takes, float gain, int cell, float volume, float pitch ){
		float level = levelAt( cell );
		if (level <= 0) return false;
		play( takes, gain, volume * level, pitch, panAt( cell ) );
		return true;
	}
}

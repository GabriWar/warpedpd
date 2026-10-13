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

import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;

/**
 * An instrument played as it is used: each note is its one sample (tools/effect_sound_gen.py)
 * pitched to a random step of a major pentatonic scale, up to an octave above, never the
 * same step twice running, so whatever comes out sounds like a tune.
 */
public final class Notes {

	private static final int[] SCALE = { 0, 2, 4, 7, 9, 12 };
	private static final float GAP = 0.16f;

	private static int last = -1;

	private Notes(){}

	/** the pitch of the next note, as a playback rate */
	public static float next(){
		int step;
		do {
			step = Random.Int( SCALE.length );
		} while (step == last);
		last = step;
		return (float)Math.pow( 2, SCALE[step] / 12.0 );
	}

	/** one note, from where `ch` stands */
	public static void play( String note, Char ch ){
		SpatialSound.play( note, ch, 1f, next() );
	}

	/** a short run of notes, one after another */
	public static void phrase( String note, Char ch, int count ){
		for (int i = 0; i < count; i++){
			SpatialSound.playDelayed( note, GAP * i, ch, 1f, next() );
		}
	}
}

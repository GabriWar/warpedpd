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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

/**
 * The effects' own random numbers: unseeded, one generator per thread, and never the game's
 * (com.watabou.utils.Random). The game's generator is a seeded stack the actor thread draws its
 * dungeon from, and a visual drawing from it on either thread would shift every later roll; a
 * flame's sway or a spark's angle draws from here instead, as freely as it likes.
 *
 * The same shapes as Random's (Float's ranges, a triangular NormalFloat, an inclusive IntRange),
 * so a particle moved over from it reads the same.
 */
public final class FxRandom {

	private FxRandom(){}

	private static final ThreadLocal<java.util.Random> RNG = new ThreadLocal<java.util.Random>(){
		@Override
		protected java.util.Random initialValue(){
			return new java.util.Random();
		}
	};

	/** Test only: seeds this thread's generator, so a test sees the same numbers every run. */
	public static void seedForTests( long seed ){
		RNG.set( new java.util.Random( seed ) );
	}

	/** Uniform in [0, 1). */
	public static float Float(){
		return RNG.get().nextFloat();
	}

	/** Uniform in [0, max). */
	public static float Float( float max ){
		return Float() * max;
	}

	/** Uniform in [min, max). */
	public static float Float( float min, float max ){
		return min + Float() * (max - min);
	}

	/** Triangular in [min, max): the middle most likely, as Random.NormalFloat. */
	public static float NormalFloat( float min, float max ){
		java.util.Random r = RNG.get();
		return min + (r.nextFloat() + r.nextFloat()) * (max - min) / 2f;
	}

	/** Uniform in [0, max); 0 for a max of 0 or less. */
	public static int Int( int max ){
		return max <= 0 ? 0 : RNG.get().nextInt( max );
	}

	/** Uniform in [min, max], both ends included. */
	public static int IntRange( int min, int max ){
		return min + Int( max - min + 1 );
	}

	/** True with probability p. */
	public static boolean chance( float p ){
		return Float() < p;
	}

	/** -1 or +1, evenly. */
	public static float sign(){
		return RNG.get().nextBoolean() ? 1f : -1f;
	}

	/** An angle in radians, uniform in [0, 2 pi). */
	public static float angle(){
		return Float() * (float)(2 * Math.PI);
	}

	/** One of the values, evenly. */
	public static int element( int[] values ){
		return values[Int( values.length )];
	}

	/** One of the values, evenly. */
	public static <T> T element( T[] values ){
		return values[Int( values.length )];
	}
}

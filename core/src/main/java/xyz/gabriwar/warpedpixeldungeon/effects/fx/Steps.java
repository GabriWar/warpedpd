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
 * The steps the sprites took lately, as the render side sees them (each sprite's move, a co-op
 * guest's replayed ones too): from where to where, and whether it was seen. What reacts to a step
 * through it - a gas stirred by someone walking through it (BlobEmitter's walk-through) - reads
 * the ones after the last it read. A ring of SIZE, the oldest giving way; any thread.
 */
public final class Steps {

	private Steps(){}

	public static final int SIZE = 32;

	private static final int[] from = new int[SIZE], to = new int[SIZE];
	private static final boolean[] seen = new boolean[SIZE];
	private static final long[] number = new long[SIZE];
	private static long last;

	/** A sprite stepped from one cell to the next, seen or not. */
	public static synchronized void record( int fromCell, int toCell, boolean visible ){
		last++;
		int i = (int)(last % SIZE);
		from[i] = fromCell;
		to[i] = toCell;
		seen[i] = visible;
		number[i] = last;
	}

	/** The number of the last step taken (0 for none yet). */
	public static synchronized long last(){
		return last;
	}

	/**
	 * The seen steps after step `after`, oldest first, as (from, to) pairs into `out`, as many as
	 * it holds; how many. Steps older than the ring are gone.
	 */
	public static synchronized int seenAfter( long after, int[] out ){
		int n = 0;
		long first = Math.max( after + 1, last - SIZE + 1 );
		for (long s = first; s <= last && 2 * n + 1 < out.length; s++){
			int i = (int)(s % SIZE);
			if (number[i] != s || !seen[i]) continue;
			out[2 * n] = from[i];
			out[2 * n + 1] = to[i];
			n++;
		}
		return n;
	}

	/** A new scene: no steps. */
	public static synchronized void reset(){
		for (int i = 0; i < SIZE; i++) number[i] = 0;
	}
}

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
 * An element's colours, hottest (or lightest) first: what a flame cools through, what an ember
 * dies down through. Light lerps between neighbouring stops (at), matter steps from one to the
 * next (step): a puff of smoke darkens in painted steps, a flame's colour flows. Never a random
 * colour.
 *
 * The stops lie evenly from 0 to 1, or where keyed() puts them.
 */
public final class Ramp {

	private final int[] stops;
	//where each stop lies along 0..1, increasing; evenly, unless keyed
	private final float[] at;
	private final boolean even;

	public Ramp( int... stops ){
		if (stops.length == 0) throw new IllegalArgumentException( "a ramp needs a stop" );
		this.stops = stops.clone();
		this.at = new float[stops.length];
		for (int i = 0; i < stops.length; i++){
			at[i] = stops.length == 1 ? 0 : i / (float)(stops.length - 1);
		}
		even = true;
	}

	private Ramp( int[] stops, float[] at ){
		this.stops = stops;
		this.at = at;
		even = false;
	}

	/** The same stops at the positions given (one each, increasing from 0 to 1). A flame's tongue
	 *  holds its first stops longer: keyed(0, .18f, .45f, .7f, 1) of its five hottest. */
	public Ramp keyed( float... positions ){
		if (positions.length > stops.length) throw new IllegalArgumentException( "more positions than stops" );
		int[] s = new int[positions.length];
		System.arraycopy( stops, 0, s, 0, positions.length );
		return new Ramp( s, positions.clone() );
	}

	/** How many stops. */
	public int size(){
		return stops.length;
	}

	/** The i-th stop's colour, 0xRRGGBB. */
	public int stop( int i ){
		return stops[Math.max( 0, Math.min( stops.length - 1, i ) )];
	}

	/** The colour at p (0..1), lerped between the stops either side: light. */
	public int at( float p ){
		if (p <= at[0]) return stops[0];
		int n = stops.length;
		if (p >= at[n - 1]) return stops[n - 1];
		for (int i = 1; i < n; i++){
			if (p <= at[i]){
				float span = at[i] - at[i - 1];
				float k = span <= 0 ? 1 : (p - at[i - 1]) / span;
				return lerp( stops[i - 1], stops[i], k );
			}
		}
		return stops[n - 1];
	}

	/** The stop p (0..1) falls on, no blending: matter. */
	public int step( float p ){
		int n = stops.length;
		if (p <= 0) return stops[0];
		if (p >= 1) return stops[n - 1];
		if (even) return stops[Math.min( n - 1, (int)(p * n) )];
		//keyed: the last stop at or before p
		int i = 0;
		while (i + 1 < n && at[i + 1] <= p) i++;
		return stops[i];
	}

	/** Two colours mixed: k 0 is a, 1 is b. */
	public static int lerp( int a, int b, float k ){
		k = k < 0 ? 0 : (k > 1 ? 1 : k);
		int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		int r = Math.round( ar + (br - ar) * k );
		int g = Math.round( ag + (bg - ag) * k );
		int bl = Math.round( ab + (bb - ab) * k );
		return (r << 16) | (g << 8) | bl;
	}
}

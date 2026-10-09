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
 * The effects' easing and flicker shapes, all pure and allocation free: expansions ease out,
 * pops overshoot (as an offset or a frame swap, never a scale), flickers are keyed tables or the
 * hearth's three sines, telegraphs a slow sine. t runs 0..1 unless said.
 */
public final class FxCurves {

	private FxCurves(){}

	private static float clamp( float t ){
		return t < 0 ? 0 : (t > 1 ? 1 : t);
	}

	/** Fast then slow: 1 - (1 - t)^2. */
	public static float easeOut( float t ){
		t = clamp( t );
		float u = 1 - t;
		return 1 - u * u;
	}

	/** Faster then slower: 1 - (1 - t)^3. */
	public static float easeOut3( float t ){
		t = clamp( t );
		float u = 1 - t;
		return 1 - u * u * u;
	}

	/** Slow then fast: t^2. */
	public static float easeIn( float t ){
		t = clamp( t );
		return t * t;
	}

	/** Smoothstep: slow at both ends. */
	public static float smooth( float t ){
		t = clamp( t );
		return t * t * (3 - 2 * t);
	}

	/** Out past 1 and back: a pop. The kit's s of 1.15 overshoots by about a twentieth (Penner's
	 *  1.70158 by a tenth). */
	public static float backOut( float t, float s ){
		t = clamp( t ) - 1;
		return t * t * ((s + 1) * t + s) + 1;
	}

	/** Up and down again: 0 at both ends, 1 in the middle. */
	public static float tri( float t ){
		t = clamp( t );
		return 1 - Math.abs( 2 * t - 1 );
	}

	/** t seconds into a strike: up to 1 over `attack` s, then down to 0 over `release` s. */
	public static float attackRelease( float t, float attack, float release ){
		if (t <= 0) return 0;
		if (t < attack) return t / attack;
		if (release <= 0) return t <= attack ? 1 : 0;
		return Math.max( 0, 1 - (t - attack) / release );
	}

	/**
	 * A keyed table of (time, value) pairs joined by straight runs, as Lightning's flicker: the
	 * first value before the first key, the last after the last.
	 */
	public static float keyed( float[] keys, float t ){
		if (t <= keys[0]) return keys[1];
		for (int i = 2; i < keys.length; i += 2){
			if (t <= keys[i]){
				float t0 = keys[i - 2], v0 = keys[i - 1];
				float span = keys[i] - t0;
				return span <= 0 ? keys[i + 1] : v0 + (keys[i + 1] - v0) * (t - t0) / span;
			}
		}
		return keys[keys.length - 1];
	}

	/** t in n even steps: floor(t n) / n, so a fade can step as painted frames do. */
	public static float step( float t, int n ){
		t = clamp( t );
		if (n <= 1) return t >= 1 ? 1 : 0;
		return Math.min( 1f, (float)Math.floor( t * n ) / n );
	}

	/**
	 * The hearth's flicker (HearthLight): three sines at 1.7, 4.3 and 9.1 rad/s from the phases a,
	 * b, c, of 0.035, 0.025 and 0.015, so within +-0.075 of 0.
	 */
	public static float hearth( float t, float a, float b, float c ){
		return 0.035f * (float)Math.sin( 1.7f * t + a )
				+ 0.025f * (float)Math.sin( 4.3f * t + b )
				+ 0.015f * (float)Math.sin( 9.1f * t + c );
	}

	/** Smooth value noise in -1..1 along t (a new value each whole t, eased between), its own
	 *  for each seed; no allocation. */
	public static float noise1( int seed, float t ){
		int i = (int)Math.floor( t );
		float f = t - i;
		float a = hash( seed, i ), b = hash( seed, i + 1 );
		float s = f * f * (3 - 2 * f);
		return a + (b - a) * s;
	}

	//a value in -1..1 for an integer pair
	private static float hash( int seed, int i ){
		int h = seed * 0x27D4EB2D ^ i * 0x165667B1;
		h ^= h >>> 15;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		h *= 0xC2B2AE35;
		h ^= h >>> 16;
		return (h & 0xFFFFFF) / (float)0x7FFFFF - 1f;
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.gas;

import com.watabou.noosa.Visual;

/**
 * Lights a grey-shaded matter frame so two of its greys come out as two given colours: the frame's
 * top grey as the matter's crown, its lower grey as its underside, the greys between falling
 * between them. A plain hardlight only scales the greys (a crown lit in a colour leaves the
 * underside a duller copy of it); this sets each channel's multiplier and offset (Visual's rm/ra)
 * so a cloud of toxic gas has a pale yellow-green crown over a deep green underside, and a goo
 * drop a pale glint over its black.
 *
 * The kit's matter greys: crown 255, body 200, underside 150, rim 105 (docs/fx/README.md).
 */
public final class Shade {

	private Shade(){}

	/** The kit's matter greys, as a share of white. */
	public static final float CROWN = 1f, BODY = 200 / 255f, UNDER = 150 / 255f, RIM = 105 / 255f;

	/** Lights v so the grey `hiGrey` shows `hi` and the grey `loGrey` shows `lo` (0xRRGGBB). */
	public static void map( Visual v, int hi, float hiGrey, int lo, float loGrey ){
		float span = hiGrey - loGrey;
		v.rm = mul( hi >> 16, lo >> 16, span );
		v.gm = mul( hi >> 8, lo >> 8, span );
		v.bm = mul( hi, lo, span );
		v.ra = channel( hi >> 16 ) - v.rm * hiGrey;
		v.ga = channel( hi >> 8 ) - v.gm * hiGrey;
		v.ba = channel( hi ) - v.bm * hiGrey;
	}

	/** The colour a grey comes out as once mapped (what map gives a pixel of that grey). */
	public static int shown( int hi, float hiGrey, int lo, float loGrey, float grey ){
		float span = hiGrey - loGrey;
		int r = out( hi >> 16, lo >> 16, span, hiGrey, grey );
		int g = out( hi >> 8, lo >> 8, span, hiGrey, grey );
		int b = out( hi, lo, span, hiGrey, grey );
		return (r << 16) | (g << 8) | b;
	}

	/** c scaled by k (0..1), 0xRRGGBB. */
	public static int darker( int c, float k ){
		int r = Math.round( ((c >> 16) & 0xFF) * k ), g = Math.round( ((c >> 8) & 0xFF) * k ), b = Math.round( (c & 0xFF) * k );
		return (r << 16) | (g << 8) | b;
	}

	private static float channel( int c ){
		return (c & 0xFF) / 255f;
	}

	private static float mul( int hi, int lo, float span ){
		return (channel( hi ) - channel( lo )) / span;
	}

	private static int out( int hi, int lo, float span, float hiGrey, float grey ){
		float m = mul( hi, lo, span );
		float v = channel( hi ) - m * hiGrey + m * grey;
		return Math.max( 0, Math.min( 255, Math.round( v * 255f ) ) );
	}
}

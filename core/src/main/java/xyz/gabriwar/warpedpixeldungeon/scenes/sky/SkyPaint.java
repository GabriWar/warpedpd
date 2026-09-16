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

package xyz.gabriwar.warpedpixeldungeon.scenes.sky;

import com.badlogic.gdx.graphics.Pixmap;

/**
 * Pixel-level helpers shared by the sky painters. Everything here works on plain
 * 0xRRGGBB ints and on Pixmaps whose blending is switched off, so translucency is
 * done by hand and the result is exactly the pixel that was asked for.
 */
public final class SkyPaint {

	private SkyPaint(){}

	public static int rgba( int rgb, int a ){
		return (rgb << 8) | (a & 0xFF);
	}

	public static int r( int rgb ){ return (rgb >> 16) & 0xFF; }
	public static int g( int rgb ){ return (rgb >> 8) & 0xFF; }
	public static int b( int rgb ){ return rgb & 0xFF; }

	public static int rgb( int r, int g, int b ){
		return (clamp( r ) << 16) | (clamp( g ) << 8) | clamp( b );
	}

	public static int clamp( int v ){
		return v < 0 ? 0 : (v > 255 ? 255 : v);
	}

	public static float clamp01( float v ){
		return v < 0 ? 0 : (v > 1 ? 1 : v);
	}

	public static int mix( int a, int b, float f ){
		f = clamp01( f );
		return rgb( (int)(r( a ) + (r( b ) - r( a )) * f + 0.5f),
				(int)(g( a ) + (g( b ) - g( a )) * f + 0.5f),
				(int)(b( a ) + (b( b ) - b( a )) * f + 0.5f) );
	}

	/** multiply every channel, k above one brightens */
	public static int scale( int rgb, float k ){
		return rgb( Math.round( r( rgb ) * k ), Math.round( g( rgb ) * k ), Math.round( b( rgb ) * k ) );
	}

	public static float lum( int rgb ){
		return (0.299f * r( rgb ) + 0.587f * g( rgb ) + 0.114f * b( rgb )) / 255f;
	}

	public static int desaturate( int rgb, float k ){
		int l = Math.round( lum( rgb ) * 255 );
		return mix( rgb, (l << 16) | (l << 8) | l, k );
	}

	// ------------------------------------------------------------- noise

	/** a small integer hash, so a painter can ask the same question twice and get
	 *  the same answer without touching the game's random generator */
	public static int hash( long seed, int x, int y ){
		long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL);
		h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
		return (int)(h & 0x7FFFFFFF);
	}

	/** 0..1 */
	public static float noise( long seed, int x, int y ){
		return (hash( seed, x, y ) & 0xFFFF) / 65535f;
	}

	/** smooth 1d value noise, 0..1, wavelength in pixels */
	public static float smooth1( long seed, float x, float wavelength ){
		float t = x / wavelength;
		int i = (int)Math.floor( t );
		float f = t - i;
		f = f * f * (3 - 2 * f);
		float a = noise( seed, i, 0 ), b = noise( seed, i + 1, 0 );
		return a + (b - a) * f;
	}

	// ------------------------------------------------------------ dither

	private static final int[] BAYER = {
			 0,  8,  2, 10,
			12,  4, 14,  6,
			 3, 11,  1,  9,
			15,  7, 13,  5 };

	/** quantises a colour to a coarse palette with an ordered dither, which is what
	 *  turns a smooth gradient into one that reads as pixel art instead of a photo */
	public static int dither( int x, int y, float r, float g, float b, int levels ){
		float t = (BAYER[(x & 3) + (y & 3) * 4] + 0.5f) / 16f - 0.5f;
		float step = 255f / (levels - 1);
		return rgb( q( r, t, step ), q( g, t, step ), q( b, t, step ) );
	}

	private static int q( float v, float t, float step ){
		return Math.round( Math.round( (v + t * step) / step ) * step );
	}

	// ----------------------------------------------------------- pixmaps

	public static boolean inside( Pixmap pm, int x, int y ){
		return x >= 0 && y >= 0 && x < pm.getWidth() && y < pm.getHeight();
	}

	public static void px( Pixmap pm, int x, int y, int rgb ){
		if (inside( pm, x, y )) pm.drawPixel( x, y, rgba( rgb, 0xFF ) );
	}

	/** manual source-over: the painters keep blending off so they can read back
	 *  exactly what they wrote, and do the mixing themselves here */
	public static void blend( Pixmap pm, int x, int y, int rgb, int a ){
		if (!inside( pm, x, y ) || a <= 0) return;
		if (a >= 255){ px( pm, x, y, rgb ); return; }
		int dst = pm.getPixel( x, y );
		int da = dst & 0xFF;
		int drgb = dst >>> 8;
		if (da == 0){
			pm.drawPixel( x, y, rgba( rgb, a ) );
		} else {
			float sa = a / 255f, dA = da / 255f;
			float outA = sa + dA * (1 - sa);
			float w = sa / outA;
			pm.drawPixel( x, y, rgba( mix( drgb, rgb, w ), Math.round( outA * 255 ) ) );
		}
	}

	/** the colour under a pixel, or -1 where nothing has been painted */
	public static int get( Pixmap pm, int x, int y ){
		if (!inside( pm, x, y )) return -1;
		int p = pm.getPixel( x, y );
		return (p & 0xFF) == 0 ? -1 : p >>> 8;
	}

	public static void hline( Pixmap pm, int x0, int x1, int y, int rgb ){
		if (x0 > x1){ int t = x0; x0 = x1; x1 = t; }
		for (int x = x0; x <= x1; x++) px( pm, x, y, rgb );
	}

	public static void vline( Pixmap pm, int x, int y0, int y1, int rgb ){
		if (y0 > y1){ int t = y0; y0 = y1; y1 = t; }
		for (int y = y0; y <= y1; y++) px( pm, x, y, rgb );
	}

	public static void disc( Pixmap pm, float cx, float cy, float r, int rgb ){
		for (int y = (int)Math.floor( cy - r ); y <= Math.ceil( cy + r ); y++){
			for (int x = (int)Math.floor( cx - r ); x <= Math.ceil( cx + r ); x++){
				if (Math.hypot( x - cx, y - cy ) <= r) px( pm, x, y, rgb );
			}
		}
	}

	public static void line( Pixmap pm, int x0, int y0, int x1, int y1, int rgb ){
		int dx = Math.abs( x1 - x0 ), dy = -Math.abs( y1 - y0 );
		int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
		int err = dx + dy;
		while (true){
			px( pm, x0, y0, rgb );
			if (x0 == x1 && y0 == y1) break;
			int e2 = 2 * err;
			if (e2 >= dy){ err += dy; x0 += sx; }
			if (e2 <= dx){ err += dx; y0 += sy; }
		}
	}

	/** source-over one pixmap onto another, with an extra alpha multiplier */
	public static void blit( Pixmap dst, Pixmap src, int dx, int dy, float alphaMul ){
		for (int y = 0; y < src.getHeight(); y++){
			for (int x = 0; x < src.getWidth(); x++){
				int p = src.getPixel( x, y );
				int a = Math.round( (p & 0xFF) * alphaMul );
				if (a > 0) blend( dst, dx + x, dy + y, p >>> 8, a );
			}
		}
	}

	public static int nextPow2( int v ){
		int p = 1;
		while (p < v) p <<= 1;
		return p;
	}

	// ------------------------------------------------------------ canvas

	/**
	 * A tiny scratch canvas for sprites: -1 is transparent. Painting into an int
	 * grid first is what makes a one-pixel outline cheap, since the silhouette is
	 * known before anything reaches the texture.
	 */
	public static final class Canvas {
		public final int w, h;
		public final int[] px;

		public Canvas( int w, int h ){
			this.w = w; this.h = h;
			px = new int[w * h];
			java.util.Arrays.fill( px, -1 );
		}

		public void set( int x, int y, int rgb ){
			if (x >= 0 && y >= 0 && x < w && y < h) px[x + y * w] = rgb;
		}

		public int get( int x, int y ){
			return (x >= 0 && y >= 0 && x < w && y < h) ? px[x + y * w] : -1;
		}

		public boolean solid( int x, int y ){
			return get( x, y ) != -1;
		}

		/** a one-pixel rim of the given colour around everything painted so far */
		public void outline( int rgb ){
			boolean[] was = new boolean[px.length];
			for (int i = 0; i < px.length; i++) was[i] = px[i] != -1;
			for (int y = 0; y < h; y++){
				for (int x = 0; x < w; x++){
					if (was[x + y * w]) continue;
					boolean touch = (x > 0 && was[x - 1 + y * w]) || (x < w - 1 && was[x + 1 + y * w])
							|| (y > 0 && was[x + (y - 1) * w]) || (y < h - 1 && was[x + (y + 1) * w]);
					if (touch) px[x + y * w] = rgb;
				}
			}
		}

		public void blitTo( Pixmap pm, int dx, int dy ){
			for (int y = 0; y < h; y++){
				for (int x = 0; x < w; x++){
					int c = px[x + y * w];
					if (c != -1) SkyPaint.px( pm, dx + x, dy + y, c );
				}
			}
		}

		public void blitTo( Pixmap pm, int dx, int dy, int alpha ){
			for (int y = 0; y < h; y++){
				for (int x = 0; x < w; x++){
					int c = px[x + y * w];
					if (c != -1) SkyPaint.blend( pm, dx + x, dy + y, c, alpha );
				}
			}
		}
	}
}

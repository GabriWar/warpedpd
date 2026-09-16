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

package xyz.gabriwar.warpedpixeldungeon.debug;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.GdxNativesLoader;

import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Headless look at the weather art: the sprite sheet blown up on a dark ground,
 * a rainbow over a mock dungeon, and the aurora curtain's frames. The painters
 * are the real ones; only the composition is faked.
 *
 * java -cp ... xyz.gabriwar.warpedpixeldungeon.debug.WeatherPreview <out.png> [scale]
 */
public class WeatherPreview {

	public static void main( String[] args ) throws Exception {
		GdxNativesLoader.load();
		File out = new File( args.length > 0 ? args[0] : "/tmp/weather.png" );
		int scale = args.length > 1 ? Integer.parseInt( args[1] ) : 4;

		//the sheet, painted through the private painter so no GL is needed
		Pixmap sheet = new Pixmap( 64, 32, Pixmap.Format.RGBA8888 );
		sheet.setBlending( Pixmap.Blending.None );
		sheet.setColor( 0 ); sheet.fill();
		Method paint = WeatherSprites.class.getDeclaredMethod( "paint", Pixmap.class );
		paint.setAccessible( true );
		paint.invoke( null, sheet );

		int bowW = 240, bowH = 96;
		Pixmap bow = new Pixmap( bowW, bowH, Pixmap.Format.RGBA8888 );
		bow.setBlending( Pixmap.Blending.None );
		bow.setColor( 0 ); bow.fill();
		WeatherSprites.paintBow( bow, bowW, bowH, bowW / 2 + 20, bowH + 28, 100 );

		int cw = 96, ch = 48, frames = 6;
		Pixmap curtain = new Pixmap( cw * frames, ch, Pixmap.Format.RGBA8888 );
		curtain.setBlending( Pixmap.Blending.None );
		curtain.setColor( 0 ); curtain.fill();
		for (int f = 0; f < frames; f++) WeatherSprites.paintCurtain( curtain, f * cw, cw, ch, 0xA0A0L, f * (float)(Math.PI * 2 / frames) );

		int W = Math.max( bowW, cw * frames ) + 8, H = 32 + 8 + bowH + 8 + ch + 8;
		Pixmap sheetOut = new Pixmap( W * scale, H * scale, Pixmap.Format.RGBA8888 );
		sheetOut.setBlending( Pixmap.Blending.None );
		//a dungeon floor: dark, a little noisy, with a tile grid
		for (int y = 0; y < H; y++) for (int x = 0; x < W; x++){
			int v = 0x2c2a34 + (SkyPaint.hash( 3, x, y ) % 7) * 0x010101;
			if (x % 16 == 0 || y % 16 == 0) v = 0x1e1c24;
			fill( sheetOut, x, y, scale, v, 255, false );
		}
		//the sheet on grey (so white shapes read) and tinted samples
		for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++){
			int p = sheet.getPixel( x, y ); int a = p & 0xFF; if (a == 0) continue;
			fill( sheetOut, 4 + x, 4 + y, scale, p >>> 8, a, false );
		}
		//tinted copies: a rain streak in blue, leaves in autumn, a firefly glow additive
		tint( sheetOut, sheet, WeatherSprites.RAIN_S2, 80, 6, scale, 0xA8C8F0, false );
		tint( sheetOut, sheet, WeatherSprites.SPLASH_1, 86, 6, scale, 0xA8C8F0, false );
		tint( sheetOut, sheet, WeatherSprites.LEAF_A, 96, 6, scale, 0xD08030, false );
		tint( sheetOut, sheet, WeatherSprites.LEAF_B, 102, 6, scale, 0xE0A030, false );
		tint( sheetOut, sheet, WeatherSprites.LEAF_C, 108, 6, scale, 0xB84820, false );
		tint( sheetOut, sheet, WeatherSprites.PETAL, 114, 6, scale, 0xF0A0C0, false );
		tint( sheetOut, sheet, WeatherSprites.GLOW_5, 122, 6, scale, 0xF8F060, true );
		tint( sheetOut, sheet, WeatherSprites.GLOW_5, 126, 8, scale, 0xC8FF60, true );
		tint( sheetOut, sheet, WeatherSprites.FLAKE_5, 134, 6, scale, 0xFFFFFF, false );
		tint( sheetOut, sheet, WeatherSprites.FLAKE_3, 141, 6, scale, 0xFFFFFF, false );
		tint( sheetOut, sheet, WeatherSprites.WISP_L, 146, 6, scale, 0xE0E4EC, false );
		tint( sheetOut, sheet, WeatherSprites.EMBER, 162, 6, scale, 0xFF8040, true );
		tint( sheetOut, sheet, WeatherSprites.GLOW_3, 161, 5, scale, 0xFF6020, true );
		//the bow, additive over the floor
		for (int y = 0; y < bowH; y++) for (int x = 0; x < bowW; x++){
			int p = bow.getPixel( x, y ); int a = p & 0xFF; if (a == 0) continue;
			fill( sheetOut, 4 + x, 44 + y, scale, p >>> 8, a, true );
		}
		//the curtains, additive over a night floor
		for (int y = 0; y < ch; y++) for (int x = 0; x < cw * frames; x++){
			int p = curtain.getPixel( x, y ); int a = p & 0xFF; if (a == 0) continue;
			fill( sheetOut, 4 + x, 44 + bowH + 8 + y, scale, p >>> 8, a, true );
		}
		PixmapIO.writePNG( new FileHandle( out ), sheetOut );
		System.out.println( "wrote " + out );
	}

	private static void tint( Pixmap dst, Pixmap sheet, int[] f, int x, int y, int scale, int col, boolean add ){
		for (int yy = 0; yy < f[3]; yy++) for (int xx = 0; xx < f[2]; xx++){
			int p = sheet.getPixel( f[0] + xx, f[1] + yy ); int a = p & 0xFF; if (a == 0) continue;
			int grey = (p >>> 8) & 0xFF;
			int c = SkyPaint.rgb( SkyPaint.r( col ) * grey / 255, SkyPaint.g( col ) * grey / 255, SkyPaint.b( col ) * grey / 255 );
			fill( dst, x + xx, y + yy, scale, c, a, add );
		}
	}

	private static void fill( Pixmap dst, int x, int y, int scale, int rgb, int alpha, boolean add ){
		for (int sy = 0; sy < scale; sy++) for (int sx = 0; sx < scale; sx++){
			int px = x * scale + sx, py = y * scale + sy;
			if (add){
				int d = dst.getPixel( px, py ) >>> 8;
				int r = Math.min( 255, SkyPaint.r( d ) + SkyPaint.r( rgb ) * alpha / 255 );
				int g = Math.min( 255, SkyPaint.g( d ) + SkyPaint.g( rgb ) * alpha / 255 );
				int b = Math.min( 255, SkyPaint.b( d ) + SkyPaint.b( rgb ) * alpha / 255 );
				dst.drawPixel( px, py, SkyPaint.rgba( SkyPaint.rgb( r, g, b ), 255 ) );
			} else {
				SkyPaint.blend( dst, px, py, rgb, alpha );
			}
		}
	}
}

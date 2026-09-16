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

package xyz.gabriwar.warpedpixeldungeon.effects;

import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Image;

import static xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint.*;

/**
 * The supporter emblems, one per tier, painted once at native size: a heart in a
 * bronze rim, a heart in a silver rim with a sparkle, a heart in a gold rim wearing
 * a crown. And the halo that glows behind whichever one is shown.
 */
public final class SupporterArt {

	private SupporterArt(){}

	public static final int EMBLEM = 24;
	public static final int[] HALO = { 72, 0, 32, 32 };

	public static final Object KEY = "supporter-emblems";
	private static final int W = 128, H = 32;

	//rim light, rim shade, halo, by tier (index 0 unused)
	public static final int[] RIM_LIGHT = { 0, 0xd09060, 0xe0e4ec, 0xffe070 };
	public static final int[] RIM_SHADE = { 0, 0x7a4a24, 0x7a7e88, 0x9a6a10 };
	public static final int[] HALO_COLOR = { 0, 0xc07840, 0xc8ccd8, 0xffd040 };
	public static final float[] HALO_ALPHA = { 0, 0.45f, 0.55f, 0.8f };

	private static SmartTexture sheet;

	public static SmartTexture get(){
		if (sheet == null || !TextureCache.contains( KEY )){
			sheet = TextureCache.create( KEY, W, H );
			Pixmap pm = sheet.bitmap;
			pm.setBlending( Pixmap.Blending.None );
			pm.setColor( 0 ); pm.fill();
			paintSheet( pm );
			sheet.bitmap( pm );
			sheet.filter( Texture.NEAREST, Texture.NEAREST );
		}
		return sheet;
	}

	public static int[] emblem( int tier ){
		tier = Math.max( 1, Math.min( 3, tier ) );
		return new int[]{ (tier - 1) * EMBLEM, 0, EMBLEM, EMBLEM };
	}

	public static void frame( Image img, int[] f ){
		img.frame( f[0], f[1], f[2], f[3] );
	}

	// ------------------------------------------------------------ painting

	public static void paintSheet( Pixmap pm ){
		for (int t = 1; t <= 3; t++) heart( pm, (t - 1) * EMBLEM, 0, t );
		//the halo: a soft disc, white, added to what is under it when drawn
		for (int y = 0; y < 32; y++){
			for (int x = 0; x < 32; x++){
				double d = Math.hypot( x + 0.5 - 16, y + 0.5 - 16 ) / 16;
				if (d >= 1) continue;
				pm.drawPixel( HALO[0] + x, HALO[1] + y, rgba( 0xFFFFFF, (int)(220 * (1 - d) * (1 - d)) ) );
			}
		}
	}

	private static boolean inHeart( int px, int py, int ox, int oy ){
		double x = (px - ox + 0.5 - 12) / 7.0;
		double y = (13.5 - (py - oy + 0.5)) / 7.0;
		double a = x * x + y * y - 1;
		return a * a * a - x * x * y * y * y < 0;
	}

	private static void heart( Pixmap pm, int ox, int oy, int tier ){
		int outline = 0x0a0a0e;
		int light = RIM_LIGHT[tier], shade = RIM_SHADE[tier];
		int fill = 0xd83a3a, dark = 0x8a1e22, hi = 0xff9a9a;
		for (int y = 0; y < EMBLEM; y++){
			for (int x = 0; x < EMBLEM; x++){
				int px = ox + x, py = oy + y;
				boolean in = inHeart( px, py, ox, oy );
				boolean edgeIn = in && !(inHeart( px - 1, py, ox, oy ) && inHeart( px + 1, py, ox, oy )
						&& inHeart( px, py - 1, ox, oy ) && inHeart( px, py + 1, ox, oy ));
				boolean edgeOut = !in && (inHeart( px - 1, py, ox, oy ) || inHeart( px + 1, py, ox, oy )
						|| inHeart( px, py - 1, ox, oy ) || inHeart( px, py + 1, ox, oy ));
				if (edgeOut){
					px( pm, px, py, outline );
				} else if (edgeIn){
					//the rim is lit from the top-left, the way every socket and frame is
					boolean lit = inHeart( px + 1, py + 1, ox, oy ) && !(inHeart( px - 1, py, ox, oy ) && inHeart( px, py - 1, ox, oy ));
					px( pm, px, py, lit ? light : (y < 10 ? mix( light, shade, 0.5f ) : shade) );
				} else if (in){
					float k = clamp01( (x + y - 12) / 18f );
					int col = mix( fill, dark, k );
					int n = hash( 0x4EA7 + tier, x, y ) % 5 - 2;
					px( pm, px, py, rgb( r( col ) + n, g( col ) + n, b( col ) + n ) );
				}
			}
		}
		//a gleam on the left lobe
		px( pm, ox + 8, oy + 8, hi ); px( pm, ox + 9, oy + 8, hi ); px( pm, ox + 8, oy + 9, hi );
		px( pm, ox + 7, oy + 9, mix( hi, fill, 0.5f ) );

		if (tier == 2){
			//a four-point sparkle off the right lobe
			int sx = ox + 19, sy = oy + 5;
			px( pm, sx, sy, 0xffffff );
			px( pm, sx - 1, sy, 0xe0e4ec ); px( pm, sx + 1, sy, 0xe0e4ec );
			px( pm, sx, sy - 1, 0xe0e4ec ); px( pm, sx, sy + 1, 0xe0e4ec );
			px( pm, sx - 2, sy, 0x9a9eaa ); px( pm, sx + 2, sy, 0x9a9eaa );
			px( pm, sx, sy - 2, 0x9a9eaa ); px( pm, sx, sy + 2, 0x9a9eaa );
		}
		if (tier == 3){
			//a crown over the heart: three points, a band, three gems
			String[] rows = {
					"....X.....X.....X...",
					"...XLX...XLX...XLX..",
					"...XLXX.XXLXX.XXLX..",
					"..XLLLXXXLLLXXXLLLX.",
					"..XLLLLLLLLLLLLLLLX.",
					"..XLrLLLbLLLLgLLLLX.",
					"...XXXXXXXXXXXXXXX..",
			};
			for (int y = 0; y < rows.length; y++){
				for (int x = 0; x < rows[y].length(); x++){
					char c = rows[y].charAt( x );
					int col;
					switch (c){
						case 'X': col = outline; break;
						case 'L': col = (x + y) % 5 == 0 ? 0xfff4b0 : (y >= 4 ? mix( light, shade, 0.35f ) : light); break;
						case 'r': col = 0xe04040; break;
						case 'b': col = 0x4080e0; break;
						case 'g': col = 0x40c060; break;
						default: continue;
					}
					px( pm, ox + 2 + x, oy + y - 1, col );
				}
			}
		}
	}
}

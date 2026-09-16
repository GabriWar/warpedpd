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

package xyz.gabriwar.warpedpixeldungeon.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Image;

import static xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint.*;

/**
 * The skill tree's own art, painted once at native pixel size: the sockets a
 * node sits in, bevelled stone with a rim that says what the node is (locked,
 * open, ready to take, bronze, silver, gold, an emblem), the pips that count its
 * points, the glow round a node you can spend on, and the stone the whole tree
 * is laid on.
 */
public final class SkillTreeArt {

	private SkillTreeArt(){}

	public static final int SOCKET = 20;

	//socket frames, by rim
	public static final int LOCKED = 0, IDLE = 1, AVAIL = 2, L1 = 3, L2 = 4, L3 = 5, ROOT = 6, L1A = 7, L2A = 8, L3A = 9;
	private static final int SOCKETS = 10;

	public static final int[] GLOW  = { 100, 22, 26, 26 };
	public static final int[] PLATE = { 128, 22, 7, 7 };
	public static final int[] HALO  = { 136, 22, 32, 32 };

	public static final Object KEY = "skilltree-art";
	public static final Object STONE_KEY = "skilltree-stone";
	private static final int W = 256, H = 64;
	public static final int TILE = 32;

	private static SmartTexture sheet, stone;

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

	/** the stone the tree lies on; a tile that repeats */
	public static SmartTexture stone(){
		if (stone == null || !TextureCache.contains( STONE_KEY )){
			stone = TextureCache.create( STONE_KEY, TILE, TILE );
			Pixmap pm = stone.bitmap;
			pm.setBlending( Pixmap.Blending.None );
			paintStone( pm );
			stone.bitmap( pm );
			stone.filter( Texture.NEAREST, Texture.NEAREST );
			stone.wrap( Texture.REPEAT, Texture.REPEAT );
		}
		return stone;
	}

	/** frame rectangle of a socket */
	public static int[] socket( int which ){
		return new int[]{ which * SOCKET, 0, SOCKET, SOCKET };
	}

	/** frame rectangle of a pip strip: max pips 2 to 4, level of them filled */
	public static int[] pips( int max, int level ){
		max = Math.max( 2, Math.min( 4, max ) );
		level = Math.max( 0, Math.min( max, level ) );
		int w = max * 4 - 1;
		int y = max == 3 ? 22 : max == 2 ? 25 : 28;
		return new int[]{ level * (w + 1), y, w, 2 };
	}

	public static void frame( Image img, int[] f ){
		img.frame( f[0], f[1], f[2], f[3] );
	}

	// ------------------------------------------------------------ painting

	public static void paintSheet( Pixmap pm ){
		//rim light, rim shade, fill
		int[][] rims = {
				{ 0x34343c, 0x16161a, 0x121216 },   //locked
				{ 0x62626c, 0x2a2a32, 0x1c1c22 },   //idle
				{ 0x8ce08c, 0x2c5a2c, 0x1a281a },   //ready to take
				{ 0xd09060, 0x5c3a1e, 0x221c18 },   //bronze, one point
				{ 0xe0e4ec, 0x62666e, 0x1e2026 },   //silver, two
				{ 0xffe070, 0x7a5a10, 0x2a2416 },   //gold, three
				{ 0xe8c880, 0x6a5020, 0x262030 },   //an emblem
				{ 0xd09060, 0x5c3a1e, 0x163a16 },   //bronze, running
				{ 0xe0e4ec, 0x62666e, 0x163a16 },   //silver, running
				{ 0xffe070, 0x7a5a10, 0x163a16 },   //gold, running
		};
		for (int i = 0; i < SOCKETS; i++){
			socket( pm, i * SOCKET, 0, rims[i][0], rims[i][1], rims[i][2], i == ROOT, i >= L1A, i == L3 || i == L3A );
		}
		//pip strips: for each max, one frame per level
		for (int max = 2; max <= 4; max++){
			for (int lvl = 0; lvl <= max; lvl++){
				int[] f = pips( max, lvl );
				for (int k = 0; k < max; k++){
					pip( pm, f[0] + k * 4, f[1], k < lvl );
				}
			}
		}
		//the glow: a soft ring just outside the socket, white so it can be tinted
		float c = 13f;
		for (int y = 0; y < 26; y++){
			for (int x = 0; x < 26; x++){
				double d = Math.hypot( x + 0.5 - c, y + 0.5 - c );
				double k = 1 - Math.abs( d - 11.4 ) / 2.8;
				if (k > 0) pm.drawPixel( GLOW[0] + x, GLOW[1] + y, rgba( 0xFFFFFF, (int)(210 * k * k) ) );
			}
		}
		//a small dark plate for a number to sit on
		for (int y = 0; y < 7; y++) for (int x = 0; x < 7; x++){
			boolean corner = (x == 0 || x == 6) && (y == 0 || y == 6);
			if (!corner) pm.drawPixel( PLATE[0] + x, PLATE[1] + y, rgba( 0x000000, 0xC0 ) );
		}
		//the halo behind a node with points in it: a soft rounded square that fades
		//out over six pixels past the socket's edge, white, added to what is under it
		float hc = 16f;
		for (int y = 0; y < 32; y++){
			for (int x = 0; x < 32; x++){
				double dx = Math.abs( x + 0.5 - hc ), dy = Math.abs( y + 0.5 - hc );
				double d = Math.cbrt( dx * dx * dx + dy * dy * dy );
				double k = Math.max( 0, (d - 9) / 7 );
				if (k >= 1) continue;
				pm.drawPixel( HALO[0] + x, HALO[1] + y, rgba( 0xFFFFFF, (int)(200 * (1 - k) * (1 - k)) ) );
			}
		}
	}

	private static void pip( Pixmap pm, int x, int y, boolean full ){
		if (full){
			px( pm, x, y, 0xffe890 ); px( pm, x + 1, y, 0xffd040 ); px( pm, x + 2, y, 0xffd040 );
			px( pm, x, y + 1, 0xc09020 ); px( pm, x + 1, y + 1, 0xa07818 ); px( pm, x + 2, y + 1, 0xa07818 );
		} else {
			px( pm, x, y, 0x34343c ); px( pm, x + 1, y, 0x2e2e36 ); px( pm, x + 2, y, 0x2e2e36 );
			px( pm, x, y + 1, 0x1c1c22 ); px( pm, x + 1, y + 1, 0x18181c ); px( pm, x + 2, y + 1, 0x18181c );
		}
	}

	/** a bevelled socket: black outline, lit rim top-left, shaded rim bottom-right,
	 *  a fainter bevel inside that, and a stone fill with a grain to it */
	private static void socket( Pixmap pm, int ox, int oy, int hl, int sh, int fl, boolean emblem, boolean running, boolean gold ){
		int ol = 0x0a0a0e;
		int md = mix( hl, sh, 0.5f );
		for (int y = 0; y < SOCKET; y++){
			for (int x = 0; x < SOCKET; x++){
				boolean cx = x == 0 || x == SOCKET - 1, cy = y == 0 || y == SOCKET - 1;
				if (cx && cy) continue;
				int col;
				if (cx || cy){
					col = ol;
				} else if (x == 1 || y == 1 || x == SOCKET - 2 || y == SOCKET - 2){
					boolean top = y == 1, left = x == 1, bottom = y == SOCKET - 2, right = x == SOCKET - 2;
					if ((top && right) || (left && bottom)) col = md;
					else if (top || left) col = hl;
					else col = sh;
					if (emblem && ((top || bottom) && (x == 4 || x == SOCKET - 5) || (left || right) && (y == 4 || y == SOCKET - 5))) col = 0xfff0c0;
				} else if (x == 2 || y == 2 || x == SOCKET - 3 || y == SOCKET - 3){
					boolean lit = y == 2 || x == 2;
					col = mix( fl, lit ? hl : sh, lit ? 0.30f : 0.45f );
				} else {
					int n = hash( 0x5EED, ox + x, oy + y ) % 5 - 2;
					col = rgb( r( fl ) + n, g( fl ) + n, b( fl ) + n );
					if (running && ((x + y) & 3) == 0) col = mix( col, 0x2a6a2a, 0.35f );
				}
				px( pm, ox + x, oy + y, col );
			}
		}
		if (gold){
			//a glint in the rim
			px( pm, ox + 3, oy + 1, 0xffffff ); px( pm, ox + 1, oy + 3, 0xffffff );
		}
	}

	/** dark stone blocks with a grain and a fine mortar line, 32 x 32 and seamless */
	private static void paintStone( Pixmap pm ){
		for (int y = 0; y < TILE; y++){
			for (int x = 0; x < TILE; x++){
				int n = hash( 0x570E, x, y ) % 7 - 3;
				int base = 0x17171c;
				//two courses of blocks, the second offset half a block, mortar between
				boolean mortar = (y % 16 == 0) || ((x + (y / 16) * 8) % 16 == 0);
				int col = mortar ? 0x101014 : rgb( r( base ) + n, g( base ) + n, b( base ) + n );
				if (!mortar && (y % 16 == 1 || (x + (y / 16) * 8) % 16 == 1)) col = mix( col, 0x24242a, 0.5f );
				pm.drawPixel( x, y, rgba( col, 0xFF ) );
			}
		}
	}
}

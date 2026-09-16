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

import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint;
import xyz.gabriwar.warpedpixeldungeon.ui.SkillTreeArt;

import java.io.File;
import java.lang.reflect.Method;

/**
 * A headless mock of the skill tree pane: the real sockets, pips, glow and stone,
 * real icons off the sheets, laid out on the pane's grid and wired the way the
 * pane wires them, in the states a tree gets into. For judging the art without
 * launching the game.
 *
 * java -cp ... xyz.gabriwar.warpedpixeldungeon.debug.SkillTreePreview <out.png> [scale]
 */
public class SkillTreePreview {

	static final int NODE = 20, COL_W = 26, ROW_H = 27, MARGIN = 6, HEAD = 14, TOP = HEAD + 4, PIPS = 3, RUN = NODE + 4;

	//gx, gy, kind (0 skill 1 talent 2 root), level, canSpend, unlocked, active, icon index, parent index, max
	static final int[][] NODES = {
		{ 3, 0, 2, 9, 0, 1, 0, 48, -1, 0 },  //trunk
		{ 0, 2, 0, 3, 0, 1, 0, 1, 0, 3 },
		{ 0, 3, 0, 2, 1, 1, 0, 2, 1, 3 },
		{ 0, 4, 0, 0, 1, 1, 0, 3, 2, 3 },
		{ 0, 5, 0, 0, 0, 0, 0, 4, 3, 3 },
		{ 2, 1, 1, 3, 0, 1, 0, 64, 0, 3 },
		{ 2, 2, 1, 1, 1, 1, 0, 65, 5, 3 },
		{ 2, 3, 1, 0, 1, 1, 0, 66, 6, 4 },
		{ 2, 4, 1, 0, 0, 0, 0, 67, 7, 3 },
		{ 3, 1, 2, 4, 0, 1, 0, 49, 0, 0 },
		{ 3, 2, 0, 2, 0, 1, 1, 10, 9, 3 },   //an active toggle, running
		{ 3, 3, 0, 1, 1, 1, 0, 11, 10, 3 },
		{ 3, 4, 0, 0, 0, 0, 0, 12, 11, 3 },
		{ 4, 4, 0, 0, 0, 0, 0, 13, 11, 3 },  //a fork
		{ 5, 1, 1, 2, 1, 1, 0, 68, 0, 3 },
		{ 5, 2, 1, 0, 0, 0, 0, 69, 14, 3 },
		{ 6, 1, 2, 0, 0, 1, 0, 50, 0, 0 },
		{ 6, 2, 0, 0, 1, 1, 0, 20, 16, 3 },
		{ 6, 3, 0, 0, 0, 0, 0, 21, 17, 3 },
		{ 8, 1, 2, 0, 0, 1, 0, 51, 0, 0 },
		{ 8, 2, 0, 0, 1, 1, 0, 30, 19, 3 },
		{ 8, 3, 0, 0, 0, 0, 0, 31, 20, 3 },
	};
	//costs, by node index, for the price tags
	static final int[] COST = { 0, 1, 2, 2, 3, 1, 1, 1, 1, 0, 1, 2, 2, 2, 1, 1, 0, 1, 2, 0, 2, 3 };

	public static void main( String[] args ) throws Exception {
		GdxNativesLoader.load();
		File out = new File( args.length > 0 ? args[0] : "/tmp/skilltree.png" );
		int scale = args.length > 1 ? Integer.parseInt( args[1] ) : 4;

		Pixmap sheet = fresh( 256, 64 );
		SkillTreeArt.paintSheet( sheet );
		Pixmap stone = new Pixmap( 32, 32, Pixmap.Format.RGBA8888 );
		Method ps = SkillTreeArt.class.getDeclaredMethod( "paintStone", Pixmap.class );
		ps.setAccessible( true );
		ps.invoke( null, stone );
		Pixmap skills = new Pixmap( new FileHandle( new File( "core/src/main/assets/sprites/hero_skills.png" ) ) );
		Pixmap talents = new Pixmap( new FileHandle( new File( "core/src/main/assets/interfaces/talent_icons.png" ) ) );

		int cols = 9, rows = 6;
		int W = MARGIN * 2 + cols * COL_W, H = TOP + rows * ROW_H + MARGIN;
		Pixmap pm = fresh( W, H );
		//stone
		for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) pm.drawPixel( x, y, stone.getPixel( x % 32, y % 32 ) );
		//talent lanes with their labels
		int[][] lanes = { { 2, 1, 4, 1 }, { 5, 2, 2, 6 } };   //col, tier, last row, level needed
		for (int[] l : lanes){
			int x0 = MARGIN + l[0] * COL_W - 3, y0 = TOP - 2, y1 = TOP + l[2] * ROW_H + NODE + PIPS + 3;
			for (int y = y0; y < y1; y++) for (int x = x0; x < x0 + COL_W; x++) SkyPaint.blend( pm, x, y, 0x0a1020, 0x8c );
			int col = 0x9fb0d8;
			text( pm, "TIER " + l[1], MARGIN + l[0] * COL_W + (NODE - textWidth( "TIER " + l[1] )) / 2, TOP + 1, col );
			if (l[3] > 1) text( pm, "LV " + l[3], MARGIN + l[0] * COL_W + (NODE - textWidth( "LV " + l[3] )) / 2, TOP + 8, col );
		}

		//wires first, the longest underneath
		Integer[] order = new Integer[NODES.length];
		for (int i = 0; i < order.length; i++) order[i] = i;
		java.util.Arrays.sort( order, ( a, b ) -> {
			int sa = NODES[a][8] < 0 ? -1 : NODES[a][1] - NODES[NODES[a][8]][1];
			int sb = NODES[b][8] < 0 ? -1 : NODES[b][1] - NODES[NODES[b][8]][1];
			return sa != sb ? sb - sa : rank( NODES[a] ) - rank( NODES[b] );
		} );
		for (int i : order){
			int[] n = NODES[i];
			if (n[8] < 0) continue;
			int[] p = NODES[n[8]];
			int px = MARGIN + p[0] * COL_W + NODE / 2, top = TOP + p[1] * ROW_H + NODE + (p[2] == 2 ? 0 : PIPS);
			int run = TOP + p[1] * ROW_H + RUN;
			int cx = MARGIN + n[0] * COL_W + NODE / 2, bottom = TOP + n[1] * ROW_H;
			int core = n[3] > 0 ? 0xe0b840 : (n[4] == 1 ? 0x77dd77 : (n[5] == 1 ? 0x6a5a30 : 0x2c2c34));
			if (px == cx){
				seg( pm, px, top, px, bottom - 1, 0x0c0c10, 1 );
				seg( pm, px, top, px, bottom - 1, core, 0 );
			} else {
				seg( pm, px, top, px, run - 1, 0x0c0c10, 1 );
				seg( pm, px, run, cx, run, 0x0c0c10, 1 );
				seg( pm, cx, run + 1, cx, bottom - 1, 0x0c0c10, 1 );
				seg( pm, px, top, px, run - 1, core, 0 );
				seg( pm, px, run, cx, run, core, 0 );
				seg( pm, cx, run + 1, cx, bottom - 1, core, 0 );
			}
		}
		//nodes
		for (int i = 0; i < NODES.length; i++){
			int[] n = NODES[i];
			int x = MARGIN + n[0] * COL_W, y = TOP + n[1] * ROW_H;
			int rim;
			if (n[5] == 0) rim = SkillTreeArt.LOCKED;
			else if (n[2] == 2) rim = SkillTreeArt.ROOT;
			else if (n[3] <= 0) rim = n[4] == 1 ? SkillTreeArt.AVAIL : SkillTreeArt.IDLE;
			else {
				int metal = n[3] >= n[9] ? 2 : n[3] == 1 ? 0 : 1;
				rim = (n[6] == 1 ? SkillTreeArt.L1A : SkillTreeArt.L1) + metal;
			}
			//the halo by the points in the node, then the ring if one can go in, both added as light
			if (n[5] == 1 && n[3] > 0){
				int hc; float ha;
				if (n[2] == 2){ hc = 0xffd040; ha = 0.22f; }
				else if (n[3] >= n[9]){ hc = 0xffd040; ha = 0.75f; }
				else if (n[3] == 1){ hc = 0xc07840; ha = 0.35f; }
				else { hc = 0xc8ccd8; ha = 0.5f; }
				addBlit( pm, sheet, SkillTreeArt.HALO, x - 6, y - 6, hc, ha );
			}
			if (n[4] == 1) addBlit( pm, sheet, SkillTreeArt.GLOW, x - 3, y - 3, 0x66ff66, 0.8f );
			blit( pm, sheet, SkillTreeArt.socket( rim ), x, y, 0xffffff, 1f );
			Pixmap src = n[2] == 1 ? talents : skills;
			int per = src.getWidth() / 16;
			int ix = (n[7] % per) * 16, iy = (n[7] / per) * 16;
			float a = n[5] == 0 ? 0.25f : (n[2] == 0 && n[3] == 0 ? 0.55f : 1f);
			for (int yy = 0; yy < 16; yy++) for (int xx = 0; xx < 16; xx++){
				int p = src.getPixel( ix + xx, iy + yy ); int al = Math.round( (p & 0xFF) * a );
				if (al > 0) SkyPaint.blend( pm, x + 2 + xx, y + 2 + yy, p >>> 8, al );
			}
			if (n[2] != 2){
				int[] f = SkillTreeArt.pips( n[9], n[3] );
				blit( pm, sheet, f, x + (NODE - f[2]) / 2, y + NODE + 1, 0xffffff, 1f );
				if (n[5] == 1 && n[3] < n[9] && COST[i] > 1){
					blit( pm, sheet, SkillTreeArt.PLATE, x - 1, y - 1, 0xffffff, 1f );
					text( pm, Integer.toString( COST[i] ), x + 1, y, n[4] == 1 ? 0x8ce08c : 0xdd8877 );
				}
			} else if (n[3] > 0){
				blit( pm, sheet, SkillTreeArt.PLATE, x + NODE - 7, y + NODE - 7, 0xffffff, 1f );
				text( pm, Integer.toString( n[3] ), x + NODE - 5, y + NODE - 6, 0xffffaa );
			}
		}
		//the fixed strip over it all
		for (int y = 0; y < HEAD; y++) for (int x = 0; x < W; x++) SkyPaint.blend( pm, x, y, 0x000000, 0xB3 );
		for (int x = 0; x < W; x++) SkyPaint.px( pm, x, HEAD, 0x3a3a44 );
		text( pm, "12 POINTS TO SPEND", 18, 4, 0xffff44 );
		text( pm, "LEVEL 14", W - 5 - textWidth( "LEVEL 14" ), 4, 0xaaaaaa );
		for (int y = 0; y < 13; y++) for (int x = 0; x < 13; x++){
			boolean star = Math.abs( x - 6 ) + Math.abs( y - 6 ) <= 4 || (x == 6 || y == 6);
			if (star) SkyPaint.px( pm, 2 + x, y, 0xffff88 );
		}

		Pixmap big = fresh( W * scale, H * scale );
		for (int y = 0; y < H; y++) for (int x = 0; x < W; x++){
			int p = pm.getPixel( x, y );
			for (int sy = 0; sy < scale; sy++) for (int sx = 0; sx < scale; sx++) big.drawPixel( x * scale + sx, y * scale + sy, p );
		}
		PixmapIO.writePNG( new FileHandle( out ), big );
		System.out.println( "wrote " + out + " " + W + "x" + H );
	}

	static int rank( int[] n ){
		return n[3] > 0 ? 3 : n[4] == 1 ? 2 : n[5] == 1 ? 1 : 0;
	}

	static void seg( Pixmap pm, int x0, int y0, int x1, int y1, int col, int pad ){
		for (int y = Math.min( y0, y1 ) - pad; y <= Math.max( y0, y1 ) + pad; y++)
			for (int x = Math.min( x0, x1 ) - pad; x <= Math.max( x0, x1 ) + pad; x++) SkyPaint.px( pm, x, y, col );
	}

	static void blit( Pixmap dst, Pixmap sheet, int[] f, int x, int y, int tint, float alpha ){
		for (int yy = 0; yy < f[3]; yy++) for (int xx = 0; xx < f[2]; xx++){
			int p = sheet.getPixel( f[0] + xx, f[1] + yy ); int a = Math.round( (p & 0xFF) * alpha ); if (a == 0) continue;
			int c = p >>> 8;
			c = SkyPaint.rgb( SkyPaint.r( c ) * SkyPaint.r( tint ) / 255, SkyPaint.g( c ) * SkyPaint.g( tint ) / 255, SkyPaint.b( c ) * SkyPaint.b( tint ) / 255 );
			SkyPaint.blend( dst, x + xx, y + yy, c, a );
		}
	}

	/** additive: the tinted source is added to what is there, the way the pane draws its glows */
	static void addBlit( Pixmap dst, Pixmap sheet, int[] f, int x, int y, int tint, float alpha ){
		for (int yy = 0; yy < f[3]; yy++) for (int xx = 0; xx < f[2]; xx++){
			int p = sheet.getPixel( f[0] + xx, f[1] + yy ); float a = (p & 0xFF) * alpha / 255f; if (a <= 0) continue;
			int px = x + xx, py = y + yy;
			if (px < 0 || py < 0 || px >= dst.getWidth() || py >= dst.getHeight()) continue;
			int d = dst.getPixel( px, py ) >>> 8;
			int r = Math.min( 255, SkyPaint.r( d ) + Math.round( SkyPaint.r( tint ) * a ) );
			int g = Math.min( 255, SkyPaint.g( d ) + Math.round( SkyPaint.g( tint ) * a ) );
			int b = Math.min( 255, SkyPaint.b( d ) + Math.round( SkyPaint.b( tint ) * a ) );
			dst.drawPixel( px, py, SkyPaint.rgba( SkyPaint.rgb( r, g, b ), 0xFF ) );
		}
	}

	static Pixmap fresh( int w, int h ){
		Pixmap p = new Pixmap( w, h, Pixmap.Format.RGBA8888 );
		p.setBlending( Pixmap.Blending.None );
		p.setColor( 0 ); p.fill();
		return p;
	}

	static int textWidth( String s ){ return s.length() * 4 - 1; }
	static void text( Pixmap pm, String s, int x, int y, int col ){ SundialPreview.text( pm, s, x, y, col ); }
}

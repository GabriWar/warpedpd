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

import xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint.Canvas;

import com.badlogic.gdx.graphics.Pixmap;

import java.util.ArrayList;
import java.util.Collections;

import static xyz.gabriwar.warpedpixeldungeon.scenes.sky.SkyPaint.*;

/**
 * The town, seen from its own square. Three depths: the roofs along the palisade
 * at the back with the temple's spire over them, a few houses with the well and
 * the market in the middle distance, and two big houses cut off by the edges of
 * the screen right beside you. No two houses share a size, a style or a way of
 * facing you, and the near ones show their timber, their jettied upper floors,
 * their shutters and their lanterns.
 */
public final class SkyTown {

	private SkyTown(){}

	private enum Wall { TIMBER, PLASTER, STONE }
	private enum Roof { TILE, THATCH, SLATE }

	/** one house as a sprite: wall style, roof style, gable end or long side to us */
	private static final class House {
		int w, wallH, roofH;
		Wall wall; Roof roof;
		boolean gable;      //the pointed end faces us
		boolean jetty;      //the upper floor overhangs the lower
		int win = 3;        //window size: the nearer the house, the bigger its panes
		int bay = 6;        //spacing between windows
	}

	private static Pixmap land;
	private static int top;
	//the last house's chimney, in its own canvas coordinates, for the smoke
	private static int[] chimneyMark;

	private static void put( int x, int y, int rgb ){ px( land, x, y - top, rgb ); }

	public static void paint( SkyContext c, Pixmap landPm ){
		land = landPm; top = c.landTop;
		long s = c.seed ^ 0x70A4L;
		boolean night = c.light < 0.35f;
		int lit = c.litSide();

		// ---- the far row: the palisade, then roofs at three slightly different depths ----
		int farY = c.skyHorizon + 3;
		palisade( c, s, farY - 2 );
		ArrayList<int[]> order = new ArrayList<>();   //{x, dy, index}
		ArrayList<Canvas> sprites = new ArrayList<>();
		int templeX = c.W / 2 - Math.round( c.H * 0.06f );
		int x = -8 - hash( s, 0, 1 ) % 8;
		boolean templeDone = false;
		int idx = 0;
		while (x < c.W + 8){
			boolean temple = !templeDone && x >= templeX;
			Canvas cv;
			int dy;
			if (temple){
				cv = temple( c, s, night, lit, 0.55f );
				dy = 4;
				templeDone = true;
			} else {
				House h = design( c, s ^ x, 0.3f + 0.32f * noise( s, x, 3 ) );
				cv = house( c, s ^ x, h, night, lit );
				dy = hash( s, x, 4 ) % 7;
			}
			order.add( new int[]{ x, dy, idx++ } );
			sprites.add( cv );
			//houses crowd: sometimes the next one starts before this one ends
			x += cv.w - 2 + hash( s, x, 5 ) % 7;
		}
		//the ones lower on the screen are nearer, so they are painted last
		Collections.sort( order, ( a, b ) -> a[1] - b[1] );
		for (int[] o : order){
			Canvas cv = sprites.get( o[2] );
			hazeCanvas( cv, c, o[0] + cv.w / 2, c.haze( 0.6f - 0.05f * o[1] ) );
			cv.blitTo( land, o[0], farY + o[1] - cv.h - top );
		}

		// ---- the square: a strip of trodden earth under the far houses, then the
		//      path to the temple, the well, the market, a tree ----
		int midY = c.skyHorizon + (c.groundTop - c.skyHorizon) / 2 + 3;
		float midHz = c.haze( 0.2f );
		{
			int earth = c.snow ? 0xd8dee8 : 0x8a7a5c;
			int span = Math.max( 3, (c.groundTop - c.skyHorizon) / 6 );
			for (int y = c.skyHorizon + 4; y < c.skyHorizon + 4 + span; y++){
				float t = (y - c.skyHorizon - 4) / (float)span;
				for (int xx = 0; xx < c.W; xx++){
					if (noise( s ^ 0xEA47L, xx, y ) < t * t) continue;   //it frays into the stones
					int col = c.lit( earth, 0.6f );
					col = rgb( r( col ) + hash( s, xx, y ) % 9 - 4, g( col ) + hash( s, xx, y ) % 9 - 4, b( col ) + hash( s, xx, y ) % 7 - 3 );
					put( xx, y, mix( col, c.horizonColorAt( xx ), c.haze( 0.5f ) ) );
				}
			}
		}
		path( c, s, midY, templeX + Math.round( c.H * 0.08f ) );
		int third = c.W / 3;
		Canvas w = well( c, s, lit );
		hazeCanvas( w, c, third / 2, midHz );
		int wx = third / 2 + hash( s, 1, 6 ) % 9 - 4, wy = midY - 2;
		w.blitTo( land, wx - w.w / 2, wy - w.h - top );

		Canvas st = stall( c, s, lit );
		hazeCanvas( st, c, c.W - third / 2, midHz );
		int sx = c.W - third / 2 - hash( s, 2, 6 ) % 9, sy = midY + 1;
		st.blitTo( land, sx - st.w / 2, sy - st.h - top );

		House mh = design( c, s ^ 0x77L, 0.62f );
		Canvas mc = house( c, s ^ 0x77L, mh, night, lit );
		hazeCanvas( mc, c, c.W / 2 + third / 4, midHz );
		int mx = c.W / 2 + third / 4 - mc.w / 2, my = midY - 5 - mc.h;
		mc.blitTo( land, mx, my - top );
		c.chimneys.add( new int[]{ mx + chimneyMark[0], my + chimneyMark[1], 0 } );

		Canvas tree = c.winter() ? SkyHorizon.oak( c, s ^ 0x7EEL, Math.round( c.H * 0.075f ), lit )
				: SkyHorizon.oak( c, s ^ 0x7EEL, Math.round( c.H * 0.075f ), lit );
		hazeCanvas( tree, c, third + 6, midHz * 0.8f );
		tree.blitTo( land, third + 4 - tree.w / 2, midY + 4 - tree.h - top );

		Canvas cart = cart( c, s, lit );
		hazeCanvas( cart, c, c.W / 2 - 8, midHz * 0.6f );
		cart.blitTo( land, c.W / 2 - 8 - cart.w / 2, midY + 12 - cart.h - top );
		if (c.autumn() || c.summer()){
			Canvas hay = SkyHorizon.haystack( c, s ^ 0x4A7L, 7 );
			hazeCanvas( hay, c, c.W - third + 4, midHz * 0.6f );
			hay.blitTo( land, c.W - third + 4 - hay.w / 2, midY + 10 - hay.h - top );
		}

		// ---- right beside you: two big houses cut off by the edges ----
		int nearY = c.groundTop + 4;
		House left = design( c, s ^ 0xA11L, 1.6f );
		left.gable = true;
		//the near walls are timber or plaster: stone would sink into the square behind
		if (left.wall == Wall.STONE) left.wall = Wall.TIMBER;
		left.jetty = true;
		Canvas lc = house( c, s ^ 0xA11L, left, night, lit );
		int lx = -lc.w * 2 / 5, ly = nearY - lc.h;
		lc.blitTo( land, lx, ly - top );
		if (chimneyMark[0] + lx >= 2) c.chimneys.add( new int[]{ lx + chimneyMark[0], ly + chimneyMark[1], 0 } );

		House right = design( c, s ^ 0xB22L, 1.5f );
		right.gable = false;
		if (right.wall == Wall.STONE) right.wall = Wall.PLASTER;
		Canvas rc = house( c, s ^ 0xB22L, right, night, lit );
		int rx = c.W - rc.w * 3 / 5, ry = nearY - rc.h;
		rc.blitTo( land, rx, ry - top );
		if (chimneyMark[0] + rx < c.W - 2) c.chimneys.add( new int[]{ rx + chimneyMark[0], ry + chimneyMark[1], 0 } );

		//planters along the near walls: a box of earth with something growing in it
		if (!c.snow){
			planter( c, s ^ 1, lx + lc.w + 22, c.groundTop + 6 );
			planter( c, s ^ 2, rx - 30, c.groundTop + 6 );
		}
		//the clutter against the near walls: barrels, crates, a signpost, a lamp
		Canvas b = barrels( c, s, lit );
		b.blitTo( land, lx + lc.w + 1, c.groundTop + 5 - b.h - top );
		Canvas cr = crates( c, s, lit );
		cr.blitTo( land, rx - cr.w - 2, c.groundTop + 4 - cr.h - top );
		signpost( c, s, lx + lc.w + 14, c.groundTop + 3, lit );
		lamp( c, rx - 16, c.groundTop + 3, night );
		if (c.snow && c.winter()) snowman( c, c.W / 2 + 10, c.groundTop + 2 );

		// ---- the townsfolk: a few figures at their business, fewer after dark ----
		int folk = night ? 1 + hash( s, 3, 3 ) % 2 : 3 + hash( s, 3, 3 ) % 3;
		for (int i = 0; i < folk; i++){
			float t = 0.25f + 0.7f * noise( s, i, 60 );   //how near
			int fy = c.skyHorizon + Math.round( t * (c.groundTop + 2 - c.skyHorizon) );
			int fx = Math.round( c.W * (0.15f + 0.7f * noise( s, i, 61 )) );
			int size = 5 + Math.round( 6 * t );
			Canvas f = figure( c, s ^ (i * 31L), size, night && i == 0 );
			hazeCanvas( f, c, fx, c.haze( 0.5f * (1f - t) ) );
			f.blitTo( land, fx - f.w / 2, fy - f.h - top );
		}
	}

	/** a villager: head, tunic, legs, and after dark a lantern in one hand */
	private static Canvas figure( SkyContext c, long s, int h, boolean lantern ){
		int w = Math.max( 3, h / 2 + 1 ) | 1;
		Canvas cv = new Canvas( w + 2 + (lantern ? 2 : 0), h + 2 );
		int cx = w / 2 + 1;
		int[] tunics = { 0x4a6aa8, 0xa84a3a, 0x5a8a4a, 0x8a6a9a, 0xb89858, 0x6a6a72 };
		int tunic = tunics[hash( s, 1, 1 ) % tunics.length];
		int skin = hash( s, 2, 2 ) % 3 == 0 ? 0xa87a58 : 0xe8b890;
		int hair = hash( s, 3, 3 ) % 2 == 0 ? 0x3a2a1a : 0x8a6a3a;
		int headH = Math.max( 2, h / 4 ), legH = Math.max( 2, h / 4 ), bodyH = h - headH - legH;
		int lit = c.litSide();
		//head with hair
		for (int y = 1; y <= headH; y++) for (int x = cx - 1; x <= cx; x++) cv.set( x, y, c.lit( y == 1 ? hair : skin, 0.6f + 0.2f * (x == cx ? lit : -lit) ) );
		//tunic, wider than the head, belted
		int bw = Math.max( 2, w - 2 );
		for (int y = headH + 1; y <= headH + bodyH; y++){
			for (int x = cx - bw / 2; x <= cx - bw / 2 + bw - 1; x++){
				float side = bw == 1 ? 0 : ((x - (cx - bw / 2)) / (float)(bw - 1) * 2 - 1) * lit;
				int col = c.lit( tunic, 0.55f + 0.3f * side );
				if (y == headH + bodyH - 1 && bodyH > 3) col = c.lit( 0x3a2a1a, 0.5f );   //the belt
				cv.set( x, y, col );
			}
		}
		//legs
		for (int y = headH + bodyH + 1; y <= h; y++){ cv.set( cx - 1, y, c.lit( 0x3a3038, 0.5f ) ); cv.set( cx, y, c.lit( 0x3a3038, 0.6f ) ); }
		if (lantern){
			int lx = cx + bw / 2 + 2, ly = headH + bodyH / 2 + 1;
			cv.set( lx, ly - 1, c.lit( 0x3a3a44, 0.5f ) );
			cv.set( lx, ly, 0xffc860 );
			c.chimneys.add( new int[]{ -1, -1, 2 } );   //no glow sprite: the figure is static art
		}
		cv.outline( c.rim( tunic ) );
		return cv;
	}

	private static void hazeCanvas( Canvas cv, SkyContext c, int atX, float hz ){
		int skyCol = c.horizonColorAt( atX );
		for (int k = 0; k < cv.px.length; k++) if (cv.px[k] != -1) cv.px[k] = mix( cv.px[k], skyCol, hz );
	}

	// -------------------------------------------------------------- houses

	private static House design( SkyContext c, long s, float scale ){
		House h = new House();
		int base = Math.max( 14, Math.round( c.H * 0.12f ) );
		h.w = Math.round( base * scale * (0.7f + 0.6f * noise( s, 1, 1 )) );
		h.wallH = Math.round( base * scale * (0.45f + 0.35f * noise( s, 2, 2 )) );
		h.gable = noise( s, 3, 3 ) > 0.45f;
		h.roofH = h.gable ? h.w / 2 + 1 : Math.max( 4, h.wallH / 2 );
		int r = hash( s, 4, 4 ) % 10;
		h.wall = r < 5 ? Wall.TIMBER : r < 8 ? Wall.PLASTER : Wall.STONE;
		int q = hash( s, 5, 5 ) % 10;
		h.roof = q < 5 ? Roof.TILE : q < 8 ? Roof.THATCH : Roof.SLATE;
		h.jetty = h.wall != Wall.STONE && h.wallH >= 12 && hash( s, 6, 6 ) % 2 == 0;
		h.win = scale >= 1.2f ? 4 : scale >= 0.55f ? 3 : 2;
		h.bay = scale >= 1.2f ? 10 : scale >= 0.55f ? 7 : 6;
		return h;
	}

	private static int roofColor( Roof r ){
		switch (r){
			case THATCH: return 0xb08a3c;
			case SLATE:  return 0x5a6a88;
			default:     return 0x9a3c2c;
		}
	}

	private static int wallColor( Wall w ){
		switch (w){
			case TIMBER: return 0xc8b494;
			case STONE:  return 0x8a8c92;
			default:     return 0xd8ccb0;
		}
	}

	private static Canvas house( SkyContext c, long s, House h, boolean night, int lit ){
		int w = h.w, wallH = h.wallH, roofH = h.roofH;
		int totalH = wallH + roofH + 10;
		Canvas cv = new Canvas( w + 6, totalH );
		int x0 = 3;
		int base = totalH - 1;
		int wallTop = base - wallH;
		int wallCol = wallColor( h.wall ), roofCol = roofColor( h.roof );
		int beam = 0x4a3222, frame = h.wall == Wall.TIMBER ? 0x3a2a1c : 0x6a5a48;
		int jettyY = h.jetty ? wallTop + wallH / 2 : -1;

		// ---- the wall ----
		for (int y = wallTop; y <= base; y++){
			//the jettied upper floor stands a pixel proud on both sides
			int ox = (h.jetty && y < jettyY) ? 1 : 0;
			for (int x = x0 - ox; x < x0 + w + ox; x++){
				float side = ((x - x0) / (float)(w - 1) * 2 - 1) * lit;
				float exposure = 0.55f + 0.3f * side;
				int col = c.lit( wallCol, exposure );
				int lx = x - x0, ly = y - wallTop;
				if (h.wall == Wall.STONE){
					//blocks: a seam every third row, offset every other course
					boolean seam = ly % 3 == 2 || ((lx + (ly / 3) * 2) % 5 == 0);
					if (seam) col = c.lit( 0x5a5c64, exposure * 0.8f );
					else if (noise( s, x, y ) > 0.8f) col = c.lit( 0x9a9ca4, exposure );
				} else if (h.wall == Wall.TIMBER){
					//the frame: posts, a sill, and a cross brace or two
					boolean post = lx <= 0 || lx >= w - 1 || lx % 7 == 3;
					boolean sill = ly == 0 || ly == wallH / 2 || ly == wallH - 1;
					boolean brace = (lx + ly) % 9 == 0 && lx > 2 && lx < w - 3;
					if (post || sill || brace) col = c.lit( beam, exposure );
				} else {
					if (noise( s, x, y ) > 0.9f) col = c.lit( wallCol, exposure - 0.12f );
				}
				//the stone footing
				if (y >= base - 1) col = c.lit( 0x6a6a70, exposure );
				//the shadow under the jetty
				if (h.jetty && y == jettyY) col = c.lit( wallCol, 0.2f );
				cv.set( x, y, col );
			}
		}
		//the shadow the eaves throw on the wall
		for (int x = x0 - 1; x <= x0 + w; x++) if (cv.solid( x, wallTop )) cv.set( x, wallTop, c.lit( wallCol, 0.22f ) );

		// ---- windows and the door ----
		int pitch = h.win + 3;
		int rows = Math.max( 1, (wallH - 3) / pitch );
		int cols = Math.max( 1, (w - 4) / h.bay );
		int doorX = x0 + w / 2 - 1 + (hash( s, 1, 9 ) % 3) - 1;
		for (int r = 0; r < rows; r++){
			int wy = wallTop + 2 + r * pitch;
			if (h.jetty && wy <= jettyY && wy + h.win + 1 >= jettyY) continue;   //not across the jetty line
			for (int k = 0; k < cols; k++){
				int wx = x0 + 2 + k * ((w - 4) / cols) + ((w - 4) / cols) / 2 - h.win / 2;
				boolean groundRow = r == rows - 1;
				if (groundRow && Math.abs( wx - doorX ) < h.win + 2) continue;   //the door goes here
				if (hash( s, k, r + 30 ) % 3 == 0) continue;                      //a blank bay now and then
				window( c, cv, s ^ (k * 13 + r), wx, wy, h.win, night, frame );
			}
		}
		door( c, cv, s, doorX, base, Math.min( 6, wallH - 2 ), night );

		// ---- the roof ----
		int ridgeY = wallTop - roofH;
		if (h.gable){
			for (int r = 0; r <= roofH; r++){
				int half = Math.round( r * (w / 2f + 2f) / roofH );
				int cx = x0 + w / 2;
				for (int x = cx - half; x <= cx + half; x++){
					cv.set( x, ridgeY + r, roofPixel( c, s, h.roof, roofCol, Math.signum( x - cx ) * lit, r, x - cx, ridgeY + r ) );
				}
				//the bargeboard along the edge
				cv.set( cx - half, ridgeY + r, c.lit( 0x4a3222, 0.5f ) );
				cv.set( cx + half, ridgeY + r, c.lit( 0x4a3222, 0.5f ) );
			}
			cv.set( x0 + w / 2, ridgeY, c.lit( roofCol, 1f ) );
		} else {
			//the long side faces us: a trapezoid with the eaves out past the wall
			int inset = Math.max( 2, w / 5 );
			for (int r = 0; r <= roofH; r++){
				float t = r / (float)roofH;
				int left = Math.round( x0 + inset * (1 - t) - 2f * t ), right = Math.round( x0 + w - 1 - inset * (1 - t) + 2f * t );
				for (int x = left; x <= right; x++){
					cv.set( x, ridgeY + r, roofPixel( c, s, h.roof, roofCol, lit * (x < x0 + w / 2 ? -1 : 1) * 0.3f, r, x - x0 - w / 2, ridgeY + r ) );
				}
			}
			for (int x = x0 + inset; x < x0 + w - inset; x++) cv.set( x, ridgeY, c.lit( roofCol, 1f ) );
			//a dormer on the bigger roofs
			if (w >= 24 && roofH >= 8){
				int dx = x0 + w / 2 + (hash( s, 7, 7 ) % 2 == 0 ? -w / 5 : w / 5);
				for (int y = ridgeY + 2; y <= ridgeY + roofH - 1; y++) for (int xx = dx - 2; xx <= dx + 2; xx++) cv.set( xx, y, c.lit( wallCol, 0.6f ) );
				for (int r = 0; r < 3; r++) for (int xx = dx - r; xx <= dx + r; xx++) cv.set( xx, ridgeY - 1 + r, c.lit( roofCol, 0.7f ) );
				window( c, cv, s ^ 99, dx - 1, ridgeY + 3, 3, night, frame );
			}
		}
		if (c.snow) snowOn( c, cv, s, wallTop );

		// ---- the chimney ----
		int chX = x0 + (h.gable ? w / 2 + (hash( s, 2, 2 ) % 2 == 0 ? -w / 4 : w / 4) : w / 4 + hash( s, 2, 3 ) % Math.max( 1, w / 2 ));
		int chTop = ridgeY + (h.gable ? roofH / 2 - 3 : -2);
		for (int y = chTop; y <= chTop + 6; y++){ cv.set( chX, y, c.lit( 0x5a4a44, 0.5f - 0.2f * lit ) ); cv.set( chX + 1, y, c.lit( 0x5a4a44, 0.5f + 0.2f * lit ) ); }
		cv.set( chX - 1, chTop, c.lit( 0x6a5a54, 0.6f ) ); cv.set( chX + 2, chTop, c.lit( 0x6a5a54, 0.6f ) );
		chimneyMark = new int[]{ chX, chTop - 2 };

		cv.outline( c.rim( wallCol ) );
		return cv;
	}

	private static void snowOn( SkyContext c, Canvas cv, long s, int wallTop ){
		for (int x = 0; x < cv.w; x++){
			for (int y = 0; y < cv.h; y++){
				if (cv.solid( x, y ) && !cv.solid( x, y - 1 ) && y <= wallTop){
					cv.set( x, y, c.lit( 0xe8eef6, 0.9f ) );
					if (noise( s, x, y ) > 0.5f && cv.solid( x, y + 1 )) cv.set( x, y + 1, c.lit( 0xe8eef6, 0.8f ) );
				}
			}
		}
	}

	private static int roofPixel( SkyContext c, long s, Roof roof, int roofCol, float side, int row, int dx, int y ){
		float exposure = 0.55f + 0.3f * side;
		switch (roof){
			case THATCH:
				//straw: horizontal strokes in two tones, ragged at the eaves
				if (noise( s, dx, y ) > 0.7f) exposure -= 0.18f;
				if ((y % 2) == 0 && noise( s, dx + 7, y ) > 0.4f) exposure += 0.1f;
				break;
			case SLATE:
				if (row % 2 == 1 && (dx + row / 2) % 3 == 0) exposure -= 0.2f;
				break;
			default:
				//the courses of tiles
				if (row % 3 == 2) exposure -= 0.18f;
				else if ((dx + row) % 4 == 0) exposure -= 0.08f;
		}
		return c.lit( roofCol, exposure );
	}

	private static void window( SkyContext c, Canvas cv, long s, int x, int y, int size, boolean night, int frame ){
		boolean litWin = night && hash( s, 1, 1 ) % 4 != 0;
		int glass = litWin ? mix( 0xffc860, 0xffe8a0, noise( s, 2, 2 ) ) : c.lit( 0x2e3448, 0.5f );
		int h = size, w = size - 1;
		for (int yy = 0; yy < h; yy++){
			for (int xx = 0; xx < w; xx++){
				int col = glass;
				if (size >= 4 && (xx == w / 2 || yy == h / 2)) col = c.lit( frame, 0.5f );
				//a glint on the pane by day
				if (!litWin && xx == 0 && yy == 0) col = mix( glass, 0xffffff, 0.35f * c.light );
				cv.set( x + xx, y + yy, col );
			}
		}
		for (int yy = -1; yy <= h; yy++){ cv.set( x - 1, y + yy, c.lit( frame, 0.5f ) ); cv.set( x + w, y + yy, c.lit( frame, 0.5f ) ); }
		for (int xx = 0; xx < w; xx++){ cv.set( x + xx, y - 1, c.lit( frame, 0.55f ) ); cv.set( x + xx, y + h, c.lit( frame, 0.4f ) ); }
		//shutters on some, a flower box on others
		int extra = hash( s, 3, 3 ) % 5;
		if (extra == 0){
			int sh = c.lit( 0x4a6a4a, 0.55f );
			for (int yy = 0; yy < h; yy++){ cv.set( x - 2, y + yy, sh ); cv.set( x + w + 1, y + yy, sh ); }
		} else if (extra == 1 && (c.spring() || c.summer())){
			for (int xx = -1; xx <= w; xx++) cv.set( x + xx, y + h + 1, c.lit( 0x5a3a24, 0.5f ) );
			for (int xx = 0; xx < w; xx++) cv.set( x + xx, y + h, c.lit( hash( s, xx, 4 ) % 2 == 0 ? 0xe04858 : 0x4a8a3a, 0.8f ) );
		}
		if (litWin){
			//light spills onto the wall either side
			for (int dx : new int[]{ -2, w + 1 }){
				int under = cv.get( x + dx, y + h / 2 );
				if (under != -1) cv.set( x + dx, y + h / 2, mix( under, 0xffc860, 0.3f ) );
			}
		}
	}

	private static void door( SkyContext c, Canvas cv, long s, int x, int base, int h, boolean night ){
		int wood = 0x5a3a22;
		for (int yy = 0; yy < h; yy++){
			for (int xx = 0; xx < 3; xx++){
				int col = c.lit( wood, 0.45f + 0.15f * (xx % 2) );
				if (yy == h - 1 && xx == 1) col = c.lit( wood, 0.7f );   //the arch
				cv.set( x + xx, base - 1 - yy, col );
			}
		}
		cv.set( x + 2, base - 1 - h / 2, c.lit( 0xd8b040, 0.9f ) );   //the handle
		for (int xx = -1; xx <= 3; xx++) cv.set( x + xx, base - 1, c.lit( 0x7a7a80, 0.6f ) );   //the step
		//a lantern by the door
		int lx = x + 5, ly = base - h;
		cv.set( lx, ly - 1, c.lit( 0x3a3a44, 0.5f ) );
		cv.set( lx, ly, night ? 0xffc860 : c.lit( 0xe8e0c8, 0.6f ) );
		if (night){
			for (int dx : new int[]{ -1, 1 }){
				int under = cv.get( lx + dx, ly );
				if (under != -1) cv.set( lx + dx, ly, mix( under, 0xffc860, 0.4f ) );
			}
		}
	}

	// -------------------------------------------------------------- temple

	/** the temple: a stone nave with a rose window, buttresses, an arched door,
	 *  and a narrow bell tower with a tall slate spire beside it */
	private static Canvas temple( SkyContext c, long s, boolean night, int lit, float scale ){
		int unit = Math.max( 14, Math.round( c.H * 0.12f ) );
		int w = Math.round( unit * scale * 2.2f ), wallH = Math.round( unit * scale * 1.1f );
		int roofH = w / 2 - 2;
		int towerW = Math.max( 5, w / 5 ) | 1, towerH = wallH + roofH + Math.round( unit * scale * 0.9f );
		int spireH = towerW + 8;
		int totalH = towerH + spireH + 6;
		Canvas cv = new Canvas( w + towerW + 6, totalH );
		int x0 = 2, base = totalH - 1, wallTop = base - wallH;
		int stone = 0x8e9098, dark = 0x5a5c66, slate = 0x506080;
		int naveX = x0 + towerW - 1;   //the tower stands at the left end, the nave runs right

		//the nave wall with its courses and buttresses
		for (int y = wallTop; y <= base; y++){
			for (int x = naveX; x < naveX + w; x++){
				float side = ((x - naveX) / (float)(w - 1) * 2 - 1) * lit;
				float exposure = 0.55f + 0.28f * side;
				int lx = x - naveX, ly = y - wallTop;
				int col = c.lit( stone, exposure );
				boolean seam = ly % 3 == 2 || ((lx + (ly / 3) * 2) % 5 == 0);
				if (seam) col = c.lit( dark, exposure * 0.85f );
				//buttresses: stepped piers that only reach two thirds of the way up
				if ((lx % 8 == 4 || lx % 8 == 5) && ly > wallH / 3) col = c.lit( stone, exposure + 0.15f * (lx % 8 == 4 ? lit : -lit) - 0.05f );
				if (y >= base - 1) col = c.lit( 0x6a6a70, exposure );
				cv.set( x, y, col );
			}
		}
		//tall arched windows between the buttresses
		for (int lx = 8; lx < w - 4; lx += 8){
			int wx = naveX + lx - 2;
			int wy = wallTop + 2;
			int wh = Math.max( 4, wallH - 5 );
			int glass = night ? 0xffc860 : c.lit( 0x2e3448, 0.5f );
			for (int yy = 0; yy < wh; yy++) for (int xx = 0; xx < 2; xx++){
				int col = glass;
				if (night && (yy + xx) % 3 == 0) col = mix( glass, 0xe04040, 0.5f );   //stained
				cv.set( wx + xx, wy + yy, col );
			}
			cv.set( wx, wy - 1, glass ); cv.set( wx + 1, wy - 1, glass );
			cv.set( wx - 1, wy, c.lit( dark, 0.5f ) ); cv.set( wx + 2, wy, c.lit( dark, 0.5f ) );
		}
		//the door: arched, with steps
		int doorX = naveX + w / 2 - 1;
		for (int yy = 0; yy < 6; yy++) for (int xx = 0; xx < 3; xx++) cv.set( doorX + xx, base - 1 - yy, c.lit( 0x4a3020, 0.45f + 0.1f * (xx % 2) ) );
		cv.set( doorX + 1, base - 7, c.lit( 0x4a3020, 0.5f ) );
		for (int xx = -2; xx <= 4; xx++) cv.set( doorX + xx, base - 1, c.lit( 0x7a7a80, 0.6f ) );
		for (int xx = -1; xx <= 3; xx++) cv.set( doorX + xx, base - 2, c.lit( 0x7a7a80, 0.55f ) );

		//the nave roof: a long slate gable seen side-on, with a rose window in the end
		int ridgeY = wallTop - roofH;
		int inset = 2;
		for (int r = 0; r <= roofH; r++){
			float t = r / (float)roofH;
			int left = Math.round( naveX + inset * (1 - t) - 1.5f * t ), right = Math.round( naveX + w - 1 - inset * (1 - t) + 1.5f * t );
			for (int x = left; x <= right; x++){
				float exposure = 0.55f + 0.2f * lit * (x < naveX + w / 2 ? -1 : 1) * 0.5f;
				if (r % 2 == 1 && ((x - naveX) + r / 2) % 3 == 0) exposure -= 0.2f;
				cv.set( x, ridgeY + r, c.lit( slate, exposure ) );
			}
		}
		for (int x = naveX + inset; x < naveX + w - inset; x++) cv.set( x, ridgeY, c.lit( slate, 0.95f ) );

		//the tower: narrow, higher than the ridge, with the belfry and the spire
		int towerTop = base - towerH;
		int tx = x0;
		for (int y = towerTop; y <= base; y++){
			for (int x = tx; x < tx + towerW; x++){
				float side = ((x - tx) / (float)(towerW - 1) * 2 - 1) * lit;
				float exposure = 0.55f + 0.3f * side;
				int ly = y - towerTop;
				int col = c.lit( stone, exposure );
				if (ly % 3 == 2 || ((x - tx + (ly / 3) * 2) % 4 == 0)) col = c.lit( dark, exposure * 0.8f );
				if (x == tx || x == tx + towerW - 1) col = c.lit( dark, exposure );
				cv.set( x, y, col );
			}
		}
		//the belfry: an arch with the bell hanging in it
		int bx = tx + towerW / 2, by = towerTop + 3;
		for (int yy = 0; yy < 4; yy++) for (int xx = -1; xx <= 1; xx++){
			if (yy == 0 && xx != 0) continue;
			cv.set( bx + xx, by + yy, c.lit( 0x1e2028, 0.3f ) );
		}
		cv.set( bx, by + 2, night ? 0xffd070 : c.lit( 0xd8b050, 0.9f ) );
		//a clock face below the belfry
		if (towerW >= 7){
			int cy = by + 7;
			disc( cv, bx, cy, 2 );
			cv.set( bx, cy - 1, c.lit( 0x2a2a30, 0.5f ) ); cv.set( bx + 1, cy, c.lit( 0x2a2a30, 0.5f ) );
		}
		//the spire and the gilded finial
		for (int r = 0; r < spireH; r++){
			int half = Math.round( r * (towerW / 2f + 1) / spireH );
			for (int x = bx - half; x <= bx + half; x++){
				float exposure = 0.55f + 0.3f * Math.signum( x - bx ) * lit;
				if ((x - bx + r) % 3 == 0) exposure -= 0.12f;
				cv.set( x, towerTop - spireH + r, c.lit( slate, exposure ) );
			}
		}
		cv.set( bx, towerTop - spireH - 1, c.lit( 0xe8d070, 1f ) );
		cv.set( bx, towerTop - spireH - 2, c.lit( 0xe8d070, 0.9f ) );
		cv.set( bx - 1, towerTop - spireH - 1, c.lit( 0xe8d070, 0.8f ) ); cv.set( bx + 1, towerTop - spireH - 1, c.lit( 0xe8d070, 0.8f ) );
		if (c.snow) snowOn( c, cv, s, wallTop );
		chimneyMark = new int[]{ -99, -99 };
		cv.outline( c.rim( stone ) );
		return cv;
	}

	private static void disc( Canvas cv, int cx, int cy, int r ){
		for (int y = -r; y <= r; y++) for (int x = -r; x <= r; x++){
			if (x * x + y * y <= r * r + 1) cv.set( cx + x, cy + y, 0xe8e4d8 );
		}
	}

	// ------------------------------------------------------------ the square

	private static void palisade( SkyContext c, long s, int y ){
		//sharpened logs, the town's wall, only its top showing over the roofs
		int log = 0x6a4a30;
		for (int x = 0; x < c.W; x += 2){
			int h = 6 + hash( s, x, 50 ) % 3;
			for (int yy = y - h; yy <= y; yy++){
				put( x, yy, c.lit( log, 0.55f + 0.15f * ((x / 2) % 2) ) );
				put( x + 1, yy, c.lit( log, 0.4f ) );
			}
			put( x, y - h, c.lit( 0x8a6a48, 0.8f ) );
		}
	}

	private static void path( SkyContext c, long s, int midY, int toX ){
		//a worn run of lighter, flatter stones from your feet up to the temple door
		int from = c.W / 2 + 6;
		for (int y = c.skyHorizon + 4; y < c.H; y++){
			float t = clamp01( (y - (c.skyHorizon + 4)) / (float)(c.H - c.skyHorizon - 4) );
			int cx = Math.round( toX + (from - toX) * t );
			int half = Math.round( 2 + 15 * t * t ) + (hash( s, y, 70 ) % 3) - 1;
			for (int x = cx - half; x <= cx + half; x++){
				if (x < 0 || x >= c.W) continue;
				int under = get( land, x, y - top );
				if (under == -1) continue;
				boolean edge = Math.abs( x - cx ) >= half - 1;
				if (edge && hash( s, x, y ) % 3 == 0) continue;   //a worn, uneven edge
				int col = edge ? scale( under, 0.86f ) : mix( under, c.lit( 0xb4ac9a, 0.75f ), 0.45f );
				put( x, y, col );
			}
		}
	}

	private static Canvas well( SkyContext c, long s, int lit ){
		Canvas cv = new Canvas( 15, 22 );
		int stone = 0x7a7c84, wood = 0x6a4a30;
		//the ring, round
		for (int y = 14; y < 21; y++) for (int x = 2; x < 13; x++){
			boolean seam = (y % 2 == 0 && (x + y) % 3 == 0);
			cv.set( x, y, c.lit( seam ? 0x4a4c54 : stone, 0.5f + 0.3f * ((x - 7) / 6f) * lit ) );
		}
		for (int x = 3; x < 12; x++) cv.set( x, 13, c.lit( stone, 0.85f ) );
		cv.set( 6, 15, c.lit( 0x1a1c24, 0.3f ) ); cv.set( 7, 15, c.lit( 0x1a1c24, 0.3f ) ); cv.set( 8, 15, c.lit( 0x1a1c24, 0.3f ) );   //the dark inside
		//the posts, the windlass and the little roof
		for (int y = 4; y < 14; y++){ cv.set( 2, y, c.lit( wood, 0.5f ) ); cv.set( 12, y, c.lit( wood, 0.5f ) ); }
		for (int x = 3; x < 12; x++) cv.set( x, 8, c.lit( 0x8a6a48, 0.6f ) );
		cv.set( 13, 7, c.lit( 0x3a3a44, 0.5f ) ); cv.set( 14, 8, c.lit( 0x3a3a44, 0.5f ) );   //the crank
		for (int r = 0; r < 4; r++) for (int x = 7 - r * 2 - 1; x <= 7 + r * 2 + 1; x++) cv.set( x, r, c.lit( 0x9a3c2c, 0.6f + 0.3f * Math.signum( x - 7 ) * lit - (r == 3 ? 0.15f : 0) ) );
		cv.set( 7, 9, c.lit( 0x3a3a44, 0.5f ) ); cv.set( 7, 10, c.lit( 0x3a3a44, 0.5f ) );
		cv.set( 6, 11, c.lit( 0x5a5a64, 0.6f ) ); cv.set( 7, 11, c.lit( 0x5a5a64, 0.6f ) ); cv.set( 8, 11, c.lit( 0x5a5a64, 0.6f ) );   //the bucket
		cv.outline( c.rim( stone ) );
		return cv;
	}

	private static Canvas stall( SkyContext c, long s, int lit ){
		Canvas cv = new Canvas( 21, 17 );
		int wood = 0x7a5a3a;
		int a = hash( s, 1, 1 ) % 2 == 0 ? 0xd84848 : 0x4878c8, b = 0xf0ece0;
		//the awning, striped and scalloped
		for (int x = 1; x < 20; x++){
			int col = ((x / 2) % 2 == 0) ? a : b;
			cv.set( x, 1, c.lit( col, 0.85f ) ); cv.set( x, 2, c.lit( col, 0.75f ) ); cv.set( x, 3, c.lit( col, 0.65f ) );
			if (x % 4 == 1) cv.set( x, 4, c.lit( col, 0.6f ) );
		}
		for (int y = 4; y < 16; y++){ cv.set( 1, y, c.lit( wood, 0.5f ) ); cv.set( 19, y, c.lit( wood, 0.5f ) ); }
		//the counter and the goods heaped on it
		for (int x = 2; x < 19; x++){ cv.set( x, 10, c.lit( wood, 0.65f ) ); cv.set( x, 11, c.lit( wood, 0.45f ) ); }
		int[] goods = { 0xe0a030, 0xc03030, 0x60a040, 0xe0d0a0, 0xd06020 };
		for (int x = 3; x < 18; x += 2){
			cv.set( x, 9, c.lit( goods[hash( s, x, 2 ) % goods.length], 0.85f ) );
			if (hash( s, x, 3 ) % 2 == 0) cv.set( x, 8, c.lit( goods[hash( s, x, 4 ) % goods.length], 0.9f ) );
		}
		for (int y = 12; y < 16; y++) for (int x = 2; x < 19; x++) if ((x + y) % 3 != 0) cv.set( x, y, c.lit( 0x5a4a34, 0.45f ) );
		cv.outline( c.rim( wood ) );
		return cv;
	}

	private static Canvas cart( SkyContext c, long s, int lit ){
		Canvas cv = new Canvas( 22, 14 );
		int wood = 0x8a6a44;
		//the bed, with its planks
		for (int y = 3; y < 8; y++) for (int x = 3; x < 17; x++) cv.set( x, y, c.lit( wood, 0.5f + 0.15f * (y % 2) ) );
		for (int x = 3; x < 17; x++) cv.set( x, 3, c.lit( wood, 0.75f ) );
		for (int x : new int[]{ 3, 9, 16 }) for (int y = 3; y < 8; y++) cv.set( x, y, c.lit( 0x5a4028, 0.5f ) );
		//the load: hay in summer and autumn, sacks otherwise
		if (c.summer() || c.autumn()){
			for (int y = 0; y < 3; y++) for (int x = 4 + y / 2; x < 16 - y / 2; x++) if (noise( s, x, y ) > 0.15f) cv.set( x, 2 - y, c.lit( 0xd8b040, 0.7f + 0.2f * (y % 2) ) );
		} else {
			for (int x = 5; x < 15; x += 4){ cv.set( x, 2, c.lit( 0xa89870, 0.7f ) ); cv.set( x + 1, 2, c.lit( 0xa89870, 0.6f ) ); cv.set( x, 1, c.lit( 0xa89870, 0.75f ) ); cv.set( x + 1, 1, c.lit( 0xa89870, 0.6f ) ); }
		}
		//two spoked wheels
		for (int cx : new int[]{ 6, 14 }){
			for (int y = 7; y < 13; y++) for (int x = cx - 2; x <= cx + 2; x++){
				int d = Math.abs( x - cx ) + Math.abs( y - 10 );
				if (d == 3 || (d == 2 && (x == cx || y == 10))) cv.set( x, y, c.lit( 0x4a3a2a, 0.5f ) );
				else if (d < 3 && (x == cx || y == 10)) cv.set( x, y, c.lit( 0x6a5a44, 0.55f ) );
			}
			cv.set( cx, 10, c.lit( 0x2a2018, 0.4f ) );
		}
		//the shafts
		cv.set( 17, 5, c.lit( wood, 0.5f ) ); cv.set( 18, 6, c.lit( wood, 0.5f ) ); cv.set( 19, 7, c.lit( wood, 0.5f ) ); cv.set( 20, 8, c.lit( wood, 0.5f ) );
		cv.outline( c.rim( wood ) );
		return cv;
	}

	private static Canvas barrels( SkyContext c, long s, int lit ){
		Canvas cv = new Canvas( 14, 10 );
		int wood = 0x7a5232;
		for (int b = 0; b < 2; b++){
			int x0 = b * 7, y0 = b == 1 ? 1 : 0;
			for (int y = 0; y < 8; y++){
				int ww = (y == 0 || y == 7) ? 4 : 6;
				int xs = x0 + (6 - ww) / 2;
				for (int x = xs; x < xs + ww; x++){
					boolean hoop = y == 1 || y == 6;
					float side = ((x - x0 - 2.5f) / 2.5f) * lit;
					cv.set( x, y + y0, c.lit( hoop ? 0x4a4a52 : wood, 0.5f + 0.3f * side ) );
				}
			}
		}
		cv.outline( c.rim( wood ) );
		return cv;
	}

	private static Canvas crates( SkyContext c, long s, int lit ){
		Canvas cv = new Canvas( 15, 12 );
		int wood = 0x9a7a4a;
		int[][] boxes = { { 0, 5, 7 }, { 8, 6, 6 }, { 3, 0, 6 } };   //x, y, size
		for (int[] bx : boxes){
			for (int y = 0; y < bx[2]; y++) for (int x = 0; x < bx[2]; x++){
				boolean edge = x == 0 || y == 0 || x == bx[2] - 1 || y == bx[2] - 1;
				boolean cross = x == y || x == bx[2] - 1 - y;
				float side = ((x / (float)(bx[2] - 1)) * 2 - 1) * lit;
				cv.set( bx[0] + x, bx[1] + y, c.lit( edge || cross ? 0x6a4a2a : wood, 0.5f + 0.25f * side ) );
			}
		}
		cv.outline( c.rim( wood ) );
		return cv;
	}

	private static void planter( SkyContext c, long s, int x, int baseY ){
		int wood = 0x6a4a30;
		for (int xx = 0; xx < 8; xx++){ put( x + xx, baseY - 1, c.lit( wood, 0.6f ) ); put( x + xx, baseY, c.lit( wood, 0.45f ) ); }
		put( x, baseY - 2, c.lit( wood, 0.55f ) ); put( x + 7, baseY - 2, c.lit( wood, 0.55f ) );
		for (int xx = 1; xx < 7; xx++) put( x + xx, baseY - 2, c.lit( 0x3a2a1a, 0.5f ) );
		int[] heads = { 0xf05a6a, 0xf8d04a, 0xe890d8, 0xf6f6f6 };
		for (int xx = 1; xx < 7; xx++){
			int gh = 1 + hash( s, xx, 1 ) % 3;
			for (int k = 1; k <= gh; k++) put( x + xx, baseY - 2 - k, c.lit( 0x4a8a3a, 0.6f + 0.1f * k ) );
			if (c.spring() || c.summer()) put( x + xx, baseY - 3 - gh, c.lit( heads[hash( s, xx, 2 ) % heads.length], 0.9f ) );
		}
	}

	private static void signpost( SkyContext c, long s, int x, int baseY, int lit ){
		int wood = 0x6a4a30;
		for (int y = baseY - 12; y <= baseY; y++) put( x, y, c.lit( wood, 0.5f ) );
		//two boards pointing different ways
		for (int xx = x - 5; xx <= x + 1; xx++){ put( xx, baseY - 11, c.lit( 0x9a7a4a, 0.7f ) ); put( xx, baseY - 10, c.lit( 0x9a7a4a, 0.55f ) ); }
		put( x - 6, baseY - 11, c.lit( 0x9a7a4a, 0.6f ) );
		for (int xx = x; xx <= x + 6; xx++){ put( xx, baseY - 8, c.lit( 0x9a7a4a, 0.7f ) ); put( xx, baseY - 7, c.lit( 0x9a7a4a, 0.55f ) ); }
		put( x + 7, baseY - 8, c.lit( 0x9a7a4a, 0.6f ) );
		put( x - 3, baseY - 11, c.lit( 0x3a2a1c, 0.5f ) ); put( x + 3, baseY - 8, c.lit( 0x3a2a1c, 0.5f ) );
	}

	private static void lamp( SkyContext c, int x, int baseY, boolean night ){
		int iron = 0x3a3a44;
		for (int y = baseY - 15; y <= baseY; y++) put( x, y, c.lit( iron, 0.5f ) );
		put( x - 1, baseY, c.lit( iron, 0.5f ) ); put( x + 1, baseY, c.lit( iron, 0.5f ) );
		put( x - 1, baseY - 16, c.lit( iron, 0.6f ) ); put( x, baseY - 16, c.lit( iron, 0.6f ) ); put( x + 1, baseY - 16, c.lit( iron, 0.6f ) );
		int glow = night ? 0xffc860 : c.lit( 0xe8e0c8, 0.6f );
		put( x - 1, baseY - 15, glow ); put( x, baseY - 15, glow ); put( x + 1, baseY - 15, glow );
		put( x, baseY - 14, glow );
		if (night) c.chimneys.add( new int[]{ x, baseY - 16, 1 } );
	}

	private static void snowman( SkyContext c, int x, int baseY ){
		int snow = 0xeef2f8;
		SkyPaint.disc( land, x, baseY - 3 - top, 3.2f, c.lit( snow, 0.8f ) );
		SkyPaint.disc( land, x, baseY - 8 - top, 2.3f, c.lit( snow, 0.9f ) );
		put( x - 1, baseY - 9, 0x202028 ); put( x + 1, baseY - 9, 0x202028 );
		put( x, baseY - 8, 0xe07020 );
		put( x, baseY - 4, 0x202028 ); put( x, baseY - 2, 0x202028 );
		for (int y = baseY - 12; y <= baseY - 10; y++){ put( x - 1, y, 0x2a2a30 ); put( x, y, 0x2a2a30 ); put( x + 1, y, 0x2a2a30 ); }
		for (int xx = x - 2; xx <= x + 2; xx++) put( xx, baseY - 10, 0x2a2a30 );
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.homestead.building;

import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;

/**
 * The shapes the build bar lays a piece along (BuildPlacer): a straight line from one cell toward
 * another, snapped to the nearest of the eight directions, and a rectangle between two corners,
 * filled (floors, roofs, farmland, paths) or only its rim (walls and anything else that stands in
 * the way: a hollow room's shell, a pen). Pure: cells of a map `w` wide and `h` high, nothing read
 * from the game, so the tests read them.
 *
 * Every shape lists its cells in the order they are built: from the first cell tapped onward. One
 * go builds at most {@link #MAX_CELLS}: a line is cut there, a rectangle bigger than that is
 * refused whole (BuildSelection.tooBig).
 */
public final class BuildShapes {

	/** The most cells one line or one rectangle builds. */
	public static final int MAX_CELLS = 64;

	private BuildShapes(){}

	/**
	 * A straight line from `from` toward `to`: the direction is the nearest of the eight (east,
	 * south-east, ... by the angle to `to`), and the line runs to the cell of that direction
	 * nearest `to`. It stops at the map's edge and after MAX_CELLS cells. A line onto its own
	 * start is that one cell.
	 */
	public static int[] line( int w, int h, int from, int to ){
		int fx = from % w, fy = from / w;
		int dx = to % w - fx, dy = to / w - fy;
		if (dx == 0 && dy == 0) return new int[]{ from };
		//the angle snapped to a whole eighth of a turn
		int octant = (int) Math.round( Math.atan2( dy, dx ) / (Math.PI / 4) );
		int sx = (int) Math.round( Math.cos( octant * Math.PI / 4 ) );
		int sy = (int) Math.round( Math.sin( octant * Math.PI / 4 ) );
		//how far along that direction lies the cell nearest `to`
		int steps = sx != 0 && sy != 0
				? (int) Math.round( (dx * sx + dy * sy) / 2.0 )
				: (sx != 0 ? dx * sx : dy * sy);
		steps = Math.max( 0, steps );
		int n = 0;
		int[] out = new int[Math.min( steps + 1, MAX_CELLS )];
		for (int i = 0; i <= steps && n < out.length; i++){
			int x = fx + sx * i, y = fy + sy * i;
			if (x < 0 || y < 0 || x >= w || y >= h) break;
			out[n++] = x + y * w;
		}
		return n == out.length ? out : java.util.Arrays.copyOf( out, n );
	}

	/** How many cells the rectangle with corners a and b covers. */
	public static int rectCount( int w, int a, int b ){
		return (Math.abs( a % w - b % w ) + 1) * (Math.abs( a / w - b / w ) + 1);
	}

	/**
	 * Every cell of the rectangle with corners a and b, row by row from a's row toward b's, each
	 * row from a's column toward b's: built outward from the first corner tapped.
	 */
	public static int[] rect( int w, int a, int b ){
		int ax = a % w, ay = a / w, bx = b % w, by = b / w;
		int sx = bx >= ax ? 1 : -1, sy = by >= ay ? 1 : -1;
		int[] out = new int[rectCount( w, a, b )];
		int n = 0;
		for (int y = ay; ; y += sy){
			for (int x = ax; ; x += sx){
				out[n++] = x + y * w;
				if (x == bx) break;
			}
			if (y == by) break;
		}
		return out;
	}

	/** How many cells the rim of the rectangle with corners a and b has. */
	public static int outlineCount( int w, int a, int b ){
		int rw = Math.abs( a % w - b % w ) + 1, rh = Math.abs( a / w - b / w ) + 1;
		return rw == 1 || rh == 1 ? rw * rh : 2 * (rw + rh) - 4;
	}

	/**
	 * The rim of the rectangle with corners a and b, walked once round from a: along a's row to
	 * b's column, along b's column to b's row, back along b's row, and back up a's column to the
	 * cell before a. A rectangle one cell thin is its whole line.
	 */
	public static int[] outline( int w, int a, int b ){
		int ax = a % w, ay = a / w, bx = b % w, by = b / w;
		if (ax == bx || ay == by) return rect( w, a, b );
		int sx = bx > ax ? 1 : -1, sy = by > ay ? 1 : -1;
		int[] out = new int[outlineCount( w, a, b )];
		int n = 0;
		for (int x = ax; x != bx; x += sx) out[n++] = x + ay * w;
		for (int y = ay; y != by; y += sy) out[n++] = bx + y * w;
		for (int x = bx; x != ax; x -= sx) out[n++] = x + by * w;
		for (int y = by; y != ay; y -= sy) out[n++] = ax + y * w;
		return out;
	}

	/** Does an area of this piece lay only the rectangle's rim: a piece that stands in the way
	 *  (a wall, a fence), so an area of it is a hollow shell. A floor, a roof or a bed of
	 *  farmland fills it. */
	public static boolean areaIsOutline( BuildPiece piece ){
		return piece != null && piece.kind == BuildPiece.Kind.TERRAIN && piece.terrain >= 0
				&& (Terrain.flags[piece.terrain] & Terrain.SOLID) != 0;
	}
}

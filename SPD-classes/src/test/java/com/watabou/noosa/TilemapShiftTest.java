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

package com.watabou.noosa;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The pure parts of a sliding-window content shift: cells move by (dcx, dcy), and the chunk
 * relabel must hand every new grid slot exactly the old chunk whose cells moved into it.
 * The window and step the overworld uses (176 cells, steps of 32) are covered explicitly.
 */
public class TilemapShiftTest {

	private static int[] grid( int w, int h ){
		int[] g = new int[w * h];
		for (int i = 0; i < g.length; i++) g[i] = i;
		return g;
	}

	@Test
	public void shiftCellsMovesContentTowardTheHero(){
		int w = 8, h = 6;
		int[] src = grid( w, h );
		int[] dst = new int[w * h];
		java.util.Arrays.fill( dst, -1 );
		Tilemap.shiftCells( src, dst, w, h, 2, -1 );
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				int sx = x + 2, sy = y - 1;
				int expected = sx < w && sy >= 0 ? sx + sy * w : -1;
				assertEquals( "cell " + x + "," + y, expected, dst[x + y * w] );
			}
		}
	}

	@Test
	public void shiftCellsInPlaceMatchesOutOfPlace(){
		int w = 11, h = 9;
		int[][] deltas = { {3, 0}, {-3, 0}, {0, 2}, {0, -2}, {4, 4}, {-4, 4}, {4, -4}, {-4, -4} };
		for (int[] d : deltas){
			int[] a = grid( w, h );
			int[] b = grid( w, h );
			int[] out = new int[w * h];
			System.arraycopy( b, 0, out, 0, out.length );
			Tilemap.shiftCells( a, a, w, h, d[0], d[1] );
			Tilemap.shiftCells( b, out, w, h, d[0], d[1] );
			for (int i = 0; i < out.length; i++){
				int x = i % w, y = i / w;
				int sx = x + d[0], sy = y + d[1];
				if (sx >= 0 && sy >= 0 && sx < w && sy < h){
					assertEquals( "delta " + d[0] + "," + d[1] + " cell " + i, out[i], a[i] );
				}
			}
		}
	}

	@Test
	public void relabelAgreesWithCellShiftAtChunkGranularity(){
		int cols = 11, rows = 11;   //the overworld window: 176 / 16
		int[][] steps = { {2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, -2}, {2, -2} };   //32 / 16
		for (int[] s : steps){
			int[] source = Tilemap.relabelSources( cols, rows, s[0], s[1] );
			int[] cells = grid( cols, rows );
			int[] moved = new int[cols * rows];
			java.util.Arrays.fill( moved, -1 );
			Tilemap.shiftCells( cells, moved, cols, rows, s[0], s[1] );
			for (int i = 0; i < source.length; i++){
				assertEquals( "step " + s[0] + "," + s[1] + " slot " + i, moved[i], source[i] );
			}
			//every old chunk lands in at most one slot: the relabel is a permutation of the
			//overlap, so no buffer is ever shared or lost
			boolean[] used = new boolean[source.length];
			int fresh = 0;
			for (int src : source){
				if (src < 0){ fresh++; continue; }
				assertTrue( "chunk " + src + " used twice", !used[src] );
				used[src] = true;
			}
			int expectedFresh = cols * rows - (cols - Math.abs( s[0] )) * (rows - Math.abs( s[1] ));
			assertEquals( "fresh slots for step " + s[0] + "," + s[1], expectedFresh, fresh );
		}
	}

	@Test
	public void overworldGeometryIsChunkAligned(){
		assertEquals( 0, 176 % Tilemap.CHUNK );
		assertEquals( 0, 32 % Tilemap.CHUNK );
	}
}

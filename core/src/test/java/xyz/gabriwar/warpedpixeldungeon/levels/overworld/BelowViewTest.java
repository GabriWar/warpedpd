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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BelowViewTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	//a window with a drop down from band 2 in it, and ground: searched, so no seed's luck is assumed
	private static int[] mountainOrigin(){
		for (int oy = -2000; oy < 2000; oy += 400){
			for (int ox = -2000; ox < 2000; ox += 400){
				WindowGenerator.Window w = WindowGenerator.generate( SEED, 2, ox, oy, 0f );
				int drops = 0, ground = 0;
				for (int c = 0; c < W * H; c++){
					//not the terrain: the window's solid border ring is drawn over drops too
					if (w.below[c] != -1) drops++; else ground++;
				}
				if (drops > 500 && ground > 500) return new int[]{ ox, oy };
			}
		}
		throw new AssertionError( "no mountain window found for the test seed" );
	}

	private static WindowGenerator.Window mountainWindow( int[] o ){
		return WindowGenerator.generate( SEED, 2, o[0], o[1], 0f );
	}

	private static int group( WindowGenerator.Window w, int c ){
		return Math.min( WindowGenerator.BELOW_LAYERS, w.belowDepth[c] ) - 1;
	}

	@Test
	public void everyDropShowsTheGroundBelow(){
		int[] o = mountainOrigin();
		WindowGenerator.Window w = mountainWindow( o );
		WindowGenerator.Views v = WindowGenerator.belowLayers( SEED, o[0], o[1], w );
		assertNotNull( v );
		assertEquals( WindowGenerator.BELOW_LAYERS, v.depth.length );
		int deeper = 0;
		for (int c = 0; c < W * H; c++){
			if (w.below[c] == -1) continue;
			//the ground a drop falls to is in exactly the layer of its depth
			int d = group( w, c );
			assertEquals( "cell " + c, w.below[c], v.depth[d].tiles[c] );
			if (d > 0) deeper++;
		}
		for (int c = 0; c < W * H; c++){
			if (w.terrain[c] != Terrain.CHASM) continue;
			boolean any = false;
			for (WindowGenerator.View layer : v.depth) any |= layer.tiles[c] != -1;
			assertTrue( "a drop with nothing under it at " + c, any );
		}
		assertTrue( "only one band down in the whole window", deeper > 0 );
	}

	@Test
	public void eachDropInOneLayerAndShadedFromItsRims(){
		int[] o = mountainOrigin();
		WindowGenerator.Window w = mountainWindow( o );
		WindowGenerator.Views v = WindowGenerator.belowLayers( SEED, o[0], o[1], w );
		int rims = 0;
		for (int c = 0; c < W * H; c++){
			int in = 0;
			for (WindowGenerator.View layer : v.depth) if (layer.tiles[c] != -1) in++;
			if (w.below[c] == -1){
				assertEquals( "ground drawn as a drop at " + c, 0, in );
				assertEquals( "shade on ground at " + c, -1, v.shade[c] );
				for (WindowGenerator.View layer : v.depth){
					assertEquals( "a blend on ground at " + c, -1, layer.blends[c] );
					assertEquals( "a corner on ground at " + c, -1, layer.corners[c] );
				}
				continue;
			}
			assertEquals( "cell " + c, 1, in );
			//shaded where a neighbour (diagonals too) stands higher: ground, or a nearer ledge
			int x = c % W, y = c / W, g = group( w, c );
			boolean rim = false;
			for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++){
				int nx = x + dx, ny = y + dy;
				if ((dx == 0 && dy == 0) || nx < 0 || ny < 0 || nx >= W || ny >= H) continue;
				int nc = nx + ny * W;
				rim |= w.below[nc] == -1 || group( w, nc ) < g;
			}
			assertEquals( "cell " + c, rim, v.shade[c] != -1 );
			if (rim) rims++;
		}
		assertTrue( rims > 0 );
	}

	//the dressing's blends lie on the view as they do underfoot: the snow laps onto a tarn's
	//ice, and only within one depth
	@Test
	public void theViewIsDressed(){
		int blends = 0, iceEdges = 0, iceEdgesBlended = 0;
		for (int oy = -2000; oy < 2000; oy += 400){
			for (int ox = -2000; ox < 2000; ox += 400){
				WindowGenerator.Window w = WindowGenerator.generate( SEED, 2, ox, oy, 0f );
				WindowGenerator.Views v = WindowGenerator.belowLayers( SEED, ox, oy, w );
				if (v == null) continue;
				for (int d = 0; d < v.depth.length; d++){
					WindowGenerator.View layer = v.depth[d];
					for (int c = W; c < W * (H - 1); c++){
						//the window's ring is never dressed (nor seen)
						if (c % W == 0 || c % W == W - 1) continue;
						if (layer.blends[c] != -1 || layer.corners[c] != -1){
							blends++;
							assertTrue( "a blend off its depth at " + c, layer.tiles[c] != -1 );
						}
						if (layer.tiles[c] != DungeonTileSheet.FROZEN_WATER) continue;
						//ice with snow beside it in the same depth is lapped by it
						boolean snowBeside = false;
						for (int n : new int[]{ c - W, c + 1, c + W, c - 1 }){
							snowBeside |= layer.tiles[n] == DungeonTileSheet.SNOW_TILE;
						}
						if (!snowBeside) continue;
						iceEdges++;
						if (layer.blends[c] != -1) iceEdgesBlended++;
					}
				}
			}
		}
		assertTrue( "no tarn seen from above in the sample", iceEdges > 20 );
		assertEquals( iceEdges, iceEdgesBlended );
		assertTrue( blends > 100 );
	}

	@Test
	public void onlyMountainsLookDown(){
		assertNull( WindowGenerator.belowLayers( SEED, 0, 0, WindowGenerator.generate( SEED, 0, 0, 0, 0f ) ) );
		assertNull( WindowGenerator.belowLayers( SEED, 0, 0, WindowGenerator.generate( SEED, -1, 0, 0, 0f ) ) );
	}

	@Test
	public void cost(){
		int[] o = mountainOrigin();
		WindowGenerator.Window w = mountainWindow( o );
		java.lang.management.ThreadMXBean mx = java.lang.management.ManagementFactory.getThreadMXBean();
		for (int i = 0; i < 3; i++) WindowGenerator.belowLayers( SEED, o[0], o[1], w );
		long t = mx.getCurrentThreadCpuTime();
		for (int i = 0; i < 10; i++) WindowGenerator.belowLayers( SEED, o[0], o[1], w );
		System.out.println( "[below] belowLayers " + (mx.getCurrentThreadCpuTime() - t) / 10 / 1e6 + " ms cpu per call" );
	}
}

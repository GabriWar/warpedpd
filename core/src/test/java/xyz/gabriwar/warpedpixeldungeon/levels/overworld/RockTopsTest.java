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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RockTopsTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
	private static final int[][] ORIGINS = { {-2000,-2000}, {-1200,-800}, {400,-1600}, {-400,1200}, {1600,400}, {0,0}, {800,800} };

	@Test
	public void everyNaturalRockHasATop(){
		for (int alt : new int[]{ 1, 3, 6 }){
			for (int[] o : ORIGINS){
				WindowGenerator.Window w = WindowGenerator.generate( SEED, alt, o[0], o[1], 0f );
				for (int c = 0; c < W * H; c++){
					int x = c % W, y = c / W;
					if (x == 0 || y == 0 || x == W - 1 || y == H - 1) continue;
					//natural rock and the walls a place builds alike: nothing under the open sky is black
					if (w.terrain[c] == Terrain.WALL) assertTrue( "a wall with no top at " + c, w.top[c] != -1 );
					if (w.top[c] != -1 && w.rock[c]) assertTrue( "a top on band " + w.band[c] + " at " + c, w.band[c] > alt );
				}
			}
		}
		assertNull( WindowGenerator.generate( SEED, -2, 0, 0, 0f ).top );
	}

	//the top of the rock is the ground the slice above stands on there: climb it and it is the same
	@Test
	public void aTopIsTheGroundOfTheSliceAbove(){
		int same = 0, all = 0;
		for (int alt : new int[]{ 0, 2 }){
			for (int[] o : ORIGINS){
				WindowGenerator.Window w = WindowGenerator.generate( SEED, alt, o[0], o[1], 0f );
				WindowGenerator.Window up = WindowGenerator.generate( SEED, alt + 1, o[0], o[1], 0f );
				for (int c = 0; c < W * H; c++){
					if (w.top[c] == -1 || up.band[c] != alt + 1) continue;
					all++;
					if (WindowGenerator.viewTile( up.terrain[c], up.frozen[c] ) == w.top[c]) same++;
				}
			}
		}
		assertTrue( "no rock one band up in the sample", all > 1000 );
		//the slice above lays its stairs, places and trees' snow over a few of its cells
		assertTrue( same + " of " + all, same > all * 0.9 );
	}

	//the tops are dressed as the ground above is: a tarn's ice up there is lapped by the snow
	@Test
	public void topsAreDressed(){
		int iceEdges = 0, blended = 0, blends = 0;
		for (int alt : new int[]{ 1, 3 }){
			for (int[] o : ORIGINS){
				WindowGenerator.Window w = WindowGenerator.generate( SEED, alt, o[0], o[1], 0f );
				for (int c = W; c < W * (H - 1); c++){
					if (c % W == 0 || c % W == W - 1) continue;
					boolean drawn = w.top[c] != -1 && w.terrain[c + W] == Terrain.WALL;
					if (!drawn){
						assertEquals( "a blend on no top at " + c, -1, w.topBlend[c] );
						continue;
					}
					if (w.topBlend[c] != -1) blends++;
					if (w.top[c] != xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.FROZEN_WATER) continue;
					boolean snowBeside = false;
					for (int n : new int[]{ c - W, c + 1, c + W, c - 1 }){
						snowBeside |= w.top[n] == xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.SNOW_TILE
								&& w.terrain[n + W] == Terrain.WALL;
					}
					if (!snowBeside) continue;
					iceEdges++;
					if (w.topBlend[c] != -1) blended++;
				}
			}
		}
		assertTrue( "no tarn on a top in the sample", iceEdges > 20 );
		assertEquals( iceEdges, blended );
		assertTrue( blends > 100 );
	}

	@Test
	public void veinsAndTallScarpsAreStone(){
		for (int alt : new int[]{ 0, 1, 2 }){
			for (int[] o : ORIGINS){
				WindowGenerator.Window w = WindowGenerator.generate( SEED, alt, o[0], o[1], 0f );
				for (int c = 0; c < W * H; c++){
					if (!w.earth[c]) continue;
					assertTrue( w.top[c] != -1 );
					if (w.veins != null) assertEquals( "earth over a vein at " + c, 0, w.veins[c] );
				}
			}
		}
	}

	//not a check: how much of each height's scarps the soil model makes earth
	@Test
	public void earthBySliceAndHeight(){
		for (int alt = 0; alt <= 8; alt += 2){
			int[] earth = new int[4], all = new int[4];
			for (int[] o : ORIGINS){
				WindowGenerator.Window w = WindowGenerator.generate( SEED, alt, o[0], o[1], 0f );
				for (int c = 0; c < W * (H - 1); c++){
					if (w.top[c] == -1 || w.terrain[c + W] == Terrain.WALL) continue;
					int band = WorldLayers.band( WorldModel.elevation( SEED, o[0] + c % W, o[1] + c / W ) );
					int h = Math.min( 3, Math.max( 0, band - alt ) );
					all[h]++;
					if (w.earth[c]) earth[h]++;
				}
			}
			System.out.println( "[scarps] slice " + alt + ": 1 band " + earth[1] + "/" + all[1]
					+ ", 2 bands " + earth[2] + "/" + all[2] + ", 3+ " + earth[3] + "/" + all[3] );
			assertEquals( "an earthen scarp three bands tall", 0, earth[3] );
		}
	}
}

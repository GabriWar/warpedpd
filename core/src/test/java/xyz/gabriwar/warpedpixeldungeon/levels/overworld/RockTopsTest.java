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

	//a plateau over the north half of an open field, the hero on the field: the look runs into
	//the rock TOP_SIGHT cells deep and no further, and a tree in the way stops it
	@Test
	public void sightRunsOnlyAFewCellsIntoTheRock(){
		int w = 41, h = 41;
		boolean[] rock = new boolean[w * h], blocking = new boolean[w * h];
		for (int y = 0; y < 20; y++) for (int x = 0; x < w; x++){
			rock[x + y * w] = true;
			blocking[x + y * w] = true;
		}
		int hero = 20 + 30 * w;
		boolean[] fov = new boolean[w * h];
		OverworldLevel.seeIntoRock( w, h, hero, 20, OverworldLevel.TOP_SIGHT, c -> rock[c], blocking, fov );
		int d = OverworldLevel.TOP_SIGHT;
		//straight north: rows 19 down to 20 - d are seen, the one past them is not
		for (int y = 19; y > 19 - d; y--) assertTrue( "row " + y, fov[20 + y * w] );
		assertFalse( fov[20 + (19 - d) * w] );
		//nothing deep in the plateau, whatever the angle
		for (int x = 0; x < w; x++) for (int y = 0; y <= 19 - d; y++) assertFalse( x + "," + y, fov[x + y * w] );

		//a tree on the field straight north of the hero hides the rock behind it
		blocking[20 + 25 * w] = true;
		fov = new boolean[w * h];
		OverworldLevel.seeIntoRock( w, h, hero, 20, OverworldLevel.TOP_SIGHT, c -> rock[c], blocking, fov );
		assertFalse( fov[20 + 19 * w] );
		//the field itself is the shadowcaster's: this only ever adds rock
		assertFalse( fov[20 + 22 * w] );
	}

	//nothing under the open sky keeps the dungeon's black roof: every wall with a wall in front
	//of it (the rock's, a lair's rim, a ruin's or a barrow's dressed stone, a peak's hut) wears a
	//top, on the surface and on the peaks, and the surface's rock is only where a band above is
	@Test
	public void noWallUnderTheSkyIsRoofedBlack(){
		for (int alt : new int[]{ 0, 2, 5 }){
			for (int[] o : ORIGINS){
				WindowGenerator.Window w = WindowGenerator.generate( SEED, alt, o[0], o[1], 0f );
				for (int c = W; c < W * (H - 1); c++){
					int x = c % W;
					if (x == 0 || x == W - 1) continue;
					int t = w.terrain[c];
					if (!xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.wallStitcheable( t )
							|| !xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.wallStitcheable( w.terrain[c + W] )) continue;
					String at = "alt " + alt + " origin " + o[0] + "," + o[1] + " cell " + x + "," + (c / W) + " terrain " + t;
					assertTrue( at, WindowGenerator.builtWall( t ) && w.top[c] != -1 );
				}
			}
		}
	}

	//under the mountain line the surface is never rock: the slice above sees open air there
	@Test
	public void theSurfacesRockIsUnderABand(){
		for (int[] o : ORIGINS){
			WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, o[0], o[1], 0f );
			WorldModel.Sample s = new WorldModel.Sample();
			for (int c = W; c < W * (H - 1); c++){
				int x = c % W;
				if (x == 0 || x == W - 1 || w.terrain[c] != Terrain.WALL) continue;
				int wx = o[0] + x, wy = o[1] + c / W;
				if (WorldStructures.terrainAt( SEED, wx, wy, Terrain.EMPTY ) != -1) continue;
				WorldModel.sample( SEED, wx, wy, 0f, s );
				assertTrue( "band 0 rock at " + wx + "," + wy, WorldLayers.band( s.elev ) >= 1 );
			}
		}
	}
}

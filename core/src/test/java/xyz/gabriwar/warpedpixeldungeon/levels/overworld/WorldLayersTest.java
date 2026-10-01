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

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The world's slices are generated one window at a time by pure functions;
 * these pin what the slices promise each other: a way between two slices is
 * the same cell on both, a fall always lands on ground, deep water never
 * touches the shore, and two windows agree wherever they overlap.
 */
public class WorldLayersTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private static WindowGenerator.Window gen( int altitude, int ox, int oy ){
		return WindowGenerator.generate( SEED, altitude, ox, oy, 0f );
	}

	//a window origin whose surface holds mountains and lowland both: the
	//generator is searched for one so the test does not depend on a seed's luck
	private static int[] mountainOrigin(){
		for (int oy = -2000; oy < 2000; oy += 400){
			for (int ox = -2000; ox < 2000; ox += 400){
				int walls = 0;
				for (int y = 8; y < H; y += 16){
					for (int x = 8; x < W; x += 16){
						if (WorldLayers.band( WorldModel.elevation( SEED, ox + x, oy + y ) ) >= 2) walls++;
					}
				}
				if (walls >= 6 && walls <= 90) return new int[]{ ox, oy };
			}
		}
		throw new AssertionError( "no mountain window found for the test seed" );
	}

	@Test
	public void depthsAndAltitudesRoundTrip(){
		assertEquals( 97, WorldLayers.depthOf( 0 ) );
		assertEquals( 96, WorldLayers.depthOf( 1 ) );
		assertEquals( 87, WorldLayers.depthOf( WorldLayers.MAX_ABOVE ) );
		assertEquals( 101, WorldLayers.depthOf( -1 ) );
		assertEquals( 112, WorldLayers.depthOf( -WorldLayers.MAX_BELOW ) );
		for (int a = -WorldLayers.MAX_BELOW; a <= WorldLayers.MAX_ABOVE; a++){
			assertEquals( a, WorldLayers.altitudeOf( WorldLayers.depthOf( a ) ) );
			assertTrue( WorldLayers.isLayerDepth( WorldLayers.depthOf( a ) ) );
		}
		//the neighbours of the slice depths are other levels
		assertFalse( WorldLayers.isLayerDepth( 98 ) );
		assertFalse( WorldLayers.isLayerDepth( 99 ) );
		assertFalse( WorldLayers.isLayerDepth( 100 ) );
		assertFalse( WorldLayers.isLayerDepth( 86 ) );
		assertFalse( WorldLayers.isLayerDepth( 113 ) );
		assertEquals( 0, WorldLayers.band( 0.5f ) );
		assertEquals( 1, WorldLayers.band( 0.71f ) );
		assertEquals( WorldLayers.MAX_ABOVE, WorldLayers.band( 1f ) );
	}

	@Test
	public void generationIsDeterministic(){
		for (int a : new int[]{ 0, 2, -1, -6 }){
			WindowGenerator.Window w1 = gen( a, 100, -300 ), w2 = gen( a, 100, -300 );
			assertArrayEquals( w1.terrain, w2.terrain );
			assertArrayEquals( w1.waterDepth, w2.waterDepth );
			assertArrayEquals( w1.link, w2.link );
			assertTrue( Arrays.equals( w1.frozen, w2.frozen ) );
		}
	}

	@Test
	public void overlappingWindowsAgree(){
		for (int a : new int[]{ 0, 1, -1, -3 }){
			WindowGenerator.Window w1 = gen( a, 0, 0 ), w2 = gen( a, 32, -32 );
			int checked = 0;
			//the interiors of both windows, cells at least one in from each ring
			for (int y = 2; y < H - 2; y++){
				for (int x = 2; x < W - 2; x++){
					int x2 = x - 32, y2 = y + 32;
					if (x2 < 2 || y2 < 2 || x2 >= W - 2 || y2 >= H - 2) continue;
					int c1 = x + y * W, c2 = x2 + y2 * W;
					assertEquals( "slice " + a + " cell " + x + "," + y, w1.terrain[c1], w2.terrain[c2] );
					assertEquals( w1.waterDepth[c1], w2.waterDepth[c2] );
					assertEquals( w1.link[c1], w2.link[c2] );
					checked++;
				}
			}
			assertTrue( checked > 10000 );
		}
	}

	@Test
	public void ringIsSolidOnEverySlice(){
		for (int a : new int[]{ 0, 3, -1, -12 }){
			WindowGenerator.Window w = gen( a, -700, 250 );
			for (int x = 0; x < W; x++){
				assertEquals( Terrain.WALL, w.terrain[x] );
				assertEquals( Terrain.WALL, w.terrain[x + (H-1) * W] );
			}
			for (int y = 0; y < H; y++){
				assertEquals( Terrain.WALL, w.terrain[y * W] );
				assertEquals( Terrain.WALL, w.terrain[W-1 + y * W] );
			}
		}
	}

	//a window origin whose surface holds a good stretch of open water
	private static int[] waterOrigin(){
		for (int oy = -2000; oy < 2000; oy += 400){
			for (int ox = -2000; ox < 2000; ox += 400){
				int wet = 0;
				for (int y = 8; y < H; y += 16){
					for (int x = 8; x < W; x += 16){
						if (WorldModel.biomeAt( SEED, ox + x, oy + y ) == WorldModel.Biome.OCEAN) wet++;
					}
				}
				if (wet >= 20) return new int[]{ ox, oy };
			}
		}
		throw new AssertionError( "no ocean window found for the test seed" );
	}

	@Test
	public void deepWaterNeverTouchesTheShore(){
		int[] o = waterOrigin();
		for (int a : new int[]{ 0, -2 }){
			WindowGenerator.Window w = gen( a, o[0], o[1] );
			int deep = 0, shallow = 0;
			for (int y = 1; y < H-1; y++){
				for (int x = 1; x < W-1; x++){
					int c = x + y * W;
					if (w.terrain[c] == Terrain.WATER){
						shallow++;
						assertEquals( 1, w.waterDepth[c] );
					}
					if (w.terrain[c] != Terrain.DEEP_WATER) continue;
					deep++;
					assertTrue( w.waterDepth[c] >= 2 && w.waterDepth[c] <= WorldModel.WATER_TIERS );
					for (int dy = -1; dy <= 1; dy++){
						for (int dx = -1; dx <= 1; dx++){
							int t = w.terrain[c + dx + dy * W];
							assertTrue( "deep water beside land on slice " + a,
									t == Terrain.WATER || t == Terrain.DEEP_WATER );
						}
					}
				}
			}
			//the surface window was picked for its ocean; caves always have pools somewhere
			assertTrue( "no water to test on slice " + a, shallow > 0 );
			if (a == 0) assertTrue( deep > 0 );
		}
	}

	@Test
	public void mountainStairsMatchOnBothSlices(){
		int[] o = mountainOrigin();
		WindowGenerator.Window surface = gen( 0, o[0], o[1] );
		WindowGenerator.Window[] above = new WindowGenerator.Window[WorldLayers.MAX_ABOVE + 1];
		for (int a = 1; a <= WorldLayers.MAX_ABOVE; a++) above[a] = gen( a, o[0], o[1] );
		int stairs = 0;
		for (int y = 1; y < H-1; y++){
			for (int x = 1; x < W-1; x++){
				int c = x + y * W;
				//the foot of a mountain: up from the surface, down from slice 1
				if (surface.link[c] == WindowGenerator.LINK_STAIR_UP){
					assertEquals( Terrain.ENTRANCE, surface.terrain[c] );
					assertEquals( Terrain.EXIT, above[1].terrain[c] );
					assertEquals( WindowGenerator.LINK_STAIR_DOWN, above[1].link[c] );
					stairs++;
				}
				for (int a = 1; a < WorldLayers.MAX_ABOVE; a++){
					if (above[a].link[c] == WindowGenerator.LINK_STAIR_UP){
						assertEquals( Terrain.ENTRANCE, above[a].terrain[c] );
						assertEquals( Terrain.EXIT, above[a+1].terrain[c] );
						assertEquals( WindowGenerator.LINK_STAIR_DOWN, above[a+1].link[c] );
						stairs++;
					}
					if (above[a].link[c] == WindowGenerator.LINK_STAIR_DOWN){
						//a way down from a slice is the way up of the one below it
						WindowGenerator.Window below = a == 1 ? surface : above[a-1];
						assertEquals( WindowGenerator.LINK_STAIR_UP, below.link[c] );
					}
				}
				//open air on a slice is exactly the ground of a lower band: a fall
				//from it lands on a lower slice's ground (rock scattered there is
				//broken through on arrival, see OverworldLevel.landingCell)
				int band = WorldLayers.band( WorldModel.elevation( SEED, o[0] + x, o[1] + y ) );
				for (int a = 1; a <= WorldLayers.MAX_ABOVE; a++){
					int t = above[a].terrain[c];
					if (t == Terrain.CHASM) assertTrue( band < a );
					else if (t == Terrain.WALL) assertTrue( band > a );
					else if (t == Terrain.ENTRANCE) assertEquals( a + 1, band );   //steps carved into the cliff
					//a tunnel is a floor cut through the rock of the higher bands
					else if (t == Terrain.DIRT_PATH && band > a) assertTrue( WorldModel.tunnelAt( SEED, o[0] + x, o[1] + y, a ) );
					else assertEquals( a, band );
				}
			}
		}
		assertTrue( "a mountain window should have stairs", stairs > 0 );
	}

	@Test
	public void caveWaysMatchOnBothSlices(){
		int ox = -1200, oy = 800;
		WindowGenerator.Window surface = gen( 0, ox, oy );
		WindowGenerator.Window[] caves = new WindowGenerator.Window[WorldLayers.MAX_BELOW + 1];
		for (int k = 1; k <= WorldLayers.MAX_BELOW; k++) caves[k] = gen( -k, ox, oy );
		int mouths = 0, ladders = 0, pits = 0, open = 0;
		for (int y = 1; y < H-1; y++){
			for (int x = 1; x < W-1; x++){
				int c = x + y * W;
				if (surface.link[c] == WindowGenerator.LINK_MOUTH){
					assertEquals( Terrain.EXIT, surface.terrain[c] );
					assertEquals( Terrain.ENTRANCE, caves[1].terrain[c] );
					assertEquals( WindowGenerator.LINK_CAVE_EXIT, caves[1].link[c] );
					mouths++;
				}
				if (caves[1].link[c] == WindowGenerator.LINK_CAVE_EXIT){
					assertEquals( WindowGenerator.LINK_MOUTH, surface.link[c] );
				}
				for (int k = 1; k <= WorldLayers.MAX_BELOW; k++){
					int t = caves[k].terrain[c];
					if ((Terrain.flags[t] & Terrain.SOLID) == 0) open++;
					if (caves[k].link[c] == WindowGenerator.LINK_LADDER_UP){
						assertTrue( k > 1 );
						assertEquals( Terrain.ENTRANCE, t );
						assertEquals( Terrain.EXIT, caves[k-1].terrain[c] );
						assertEquals( WindowGenerator.LINK_LADDER_DOWN, caves[k-1].link[c] );
						ladders++;
					}
					if (caves[k].link[c] == WindowGenerator.LINK_LADDER_DOWN){
						assertTrue( k < WorldLayers.MAX_BELOW );
						assertEquals( WindowGenerator.LINK_LADDER_UP, caves[k+1].link[c] );
					}
					if (t == Terrain.CHASM){
						//a pit opens onto an open cell of the slice below (a boulder
						//scattered there is broken through on arrival)
						assertTrue( k < WorldLayers.MAX_BELOW );
						assertTrue( WorldModel.caveOpen( SEED, ox + x, oy + y, -(k+1) ) );
						assertTrue( caves[k+1].terrain[c] != Terrain.WALL );
						pits++;
					}
				}
			}
		}
		float openness = open / (float)(WorldLayers.MAX_BELOW * (W-2) * (H-2));
		assertTrue( "cave openness " + openness, openness > 0.12f && openness < 0.65f );
		assertTrue( "ladders " + ladders, ladders > 0 );
		assertTrue( "pits " + pits, pits > 0 );
		assertTrue( "mouths " + mouths, mouths > 0 );
	}

	@Test
	public void generationIsFastEnoughToStream(){
		//warm up the JIT and the sector caches, then time one window per slice class
		gen( 0, 5000, 5000 ); gen( 2, 5000, 5000 ); gen( -2, 5000, 5000 );
		for (int a : new int[]{ 0, 2, -2 }){
			long t0 = System.nanoTime();
			gen( a, 5000 + 32, 5000 );
			long ms = (System.nanoTime() - t0) / 1000000;
			System.out.println( "window at altitude " + a + ": " + ms + " ms" );
			assertTrue( "slice " + a + " took " + ms + " ms", ms < 2000 );
		}
	}
}

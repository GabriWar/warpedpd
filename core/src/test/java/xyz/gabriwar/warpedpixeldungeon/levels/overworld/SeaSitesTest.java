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

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The places of the sea (SeaSites) are pure functions of the seed that move nothing the world held
 * before: an island stands only in open, never-frozen sea well clear of every shore, and the caves
 * under it never learn of it; a wreck or a lighthouse on a mainland shore takes no cell of a road, a
 * village, its fields, a barrow or the town, and no deep water; two windows agree on every island
 * cell they share.
 */
public class SeaSitesTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final long[] SEEDS = { SEED, 12345L, 987654321L };
	//sectors each way round the origin the tests look over
	private static final int R = 14;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private static ArrayList<SeaSites.Sector> all( long seed ){
		return SeaSites.sectors( seed, -R, -R, R, R );
	}

	private static SeaSites.Island firstIsland( long seed, boolean wreck, boolean light ){
		for (SeaSites.Sector s : all( seed )){
			if (s.island == null) continue;
			if (wreck && s.island.wreck == null) continue;
			if (light && s.island.light == null) continue;
			return s.island;
		}
		return null;
	}

	@Test
	public void theSeaHoldsEveryKindOfPlace(){
		int islands = 0, islandLights = 0, islandWrecks = 0, treasure = 0, shoreWrecks = 0, shoreLights = 0;
		for (long seed : SEEDS){
			for (SeaSites.Sector s : all( seed )){
				if (s.island != null){
					islands++;
					if (s.island.light != null) islandLights++;
					if (s.island.wreck != null) islandWrecks++;
					if (s.island.digX != Integer.MIN_VALUE) treasure++;
				} else {
					if (s.wreck != null) shoreWrecks++;
					if (s.light != null) shoreLights++;
				}
			}
		}
		assertTrue( "islands " + islands, islands >= 20 );
		assertTrue( "island lighthouses " + islandLights, islandLights >= 3 );
		assertTrue( "island wrecks " + islandWrecks, islandWrecks >= 3 );
		assertTrue( "buried treasure " + treasure, treasure >= 3 );
		assertTrue( "shore wrecks " + shoreWrecks, shoreWrecks >= 2 );
		assertTrue( "headland lighthouses " + shoreLights, shoreLights >= 1 );
	}

	@Test
	public void theSeaIsTheSameEveryTime(){
		ArrayList<SeaSites.Sector> first = all( SEED );
		//another seed in between empties the cache: everything is worked out again from nothing
		all( 12345L );
		ArrayList<SeaSites.Sector> again = all( SEED );
		assertEquals( first.size(), again.size() );
		for (int i = 0; i < first.size(); i++){
			SeaSites.Sector a = first.get( i ), b = again.get( i );
			assertEquals( a.type, b.type );
			assertEquals( a.island == null, b.island == null );
			if (a.island != null){
				assertEquals( a.island.cx, b.island.cx );
				assertEquals( a.island.cy, b.island.cy );
				assertEquals( a.island.radius, b.island.radius );
				assertEquals( a.island.climate, b.island.climate );
				assertEquals( a.island.digX, b.island.digX );
				assertEquals( a.island.digY, b.island.digY );
				for (int y = a.island.y0; y <= a.island.y1; y += 2){
					for (int x = a.island.x0; x <= a.island.x1; x += 2){
						assertEquals( a.island.terrain( SEED, x, y ), b.island.terrain( SEED, x, y ) );
					}
				}
			}
			if (a.wreck != null){
				assertEquals( a.wreck.holdX, b.wreck.holdX );
				assertEquals( a.wreck.holdY, b.wreck.holdY );
				assertEquals( a.wreck.upright, b.wreck.upright );
			}
			if (a.light != null){
				assertEquals( a.light.x, b.light.x );
				assertEquals( a.light.y, b.light.y );
			}
		}
	}

	@Test
	public void islandsStandWellClearOfEveryShore(){
		WorldModel.Sample s = new WorldModel.Sample();
		int checked = 0;
		for (SeaSites.Sector sec : all( SEED )){
			SeaSites.Island i = sec.island;
			if (i == null) continue;
			//no land of the world's own within CLEAR of any cell it lays (a margin for the lattice)
			int r = SeaSites.REACH + SeaSites.CLEAR - 4;
			for (int dy = -r; dy <= r; dy += 2){
				for (int dx = -r; dx <= r; dx += 2){
					if (dx * dx + dy * dy > r * r) continue;
					float e = WorldModel.elevationOf( SEED, i.cx + dx, i.cy + dy, s );
					assertTrue( "land by the island at " + i.cx + "," + i.cy, e < WorldModel.SEA );
				}
			}
			//its own cells lay only on the deep sea of the world (never a tier-1 shelf a hero could wade)
			for (int y = i.y0; y <= i.y1; y++){
				for (int x = i.x0; x <= i.x1; x++){
					if (!i.covers( x, y )) continue;
					assertTrue( WorldModel.waterTier( WorldModel.elevationOf( SEED, x, y, s ) ) >= 2 );
				}
			}
			checked++;
		}
		assertTrue( checked > 0 );
	}

	@Test
	public void islandSeasNeverFreeze(){
		WorldModel.Sample s = new WorldModel.Sample();
		float coldest = WorldModel.WINTER_SHIFT - WorldModel.CLIMATE_SHIFT;
		for (SeaSites.Sector sec : all( SEED )){
			SeaSites.Island i = sec.island;
			if (i == null) continue;
			for (int y = i.y0; y <= i.y1; y += 3){
				for (int x = i.x0; x <= i.x1; x += 3){
					WorldModel.sample( SEED, x, y, coldest, s );
					assertTrue( s.temperature >= WorldModel.FREEZE );
				}
			}
		}
	}

	@Test
	public void theCavesNeverLearnOfTheSea(){
		for (SeaSites.Sector sec : all( SEED )){
			if (sec.island != null){
				//the surface's water over the caves is the world's own: the island is the surface pass's alone
				SeaSites.Island i = sec.island;
				assertEquals( -1, WorldStructures.terrainAt( SEED, i.cx, i.cy ) );
				assertTrue( WindowGenerator.surfaceWaterTier( SEED, i.cx, i.cy ) >= 2 );
			} else if (sec.wreck != null){
				//a hull on the shelf at most: no water under it floods a slice below
				for (int y = sec.wreck.y0; y <= sec.wreck.y1(); y++){
					for (int x = sec.wreck.x0; x <= sec.wreck.x1(); x++){
						assertTrue( WorldModel.waterTier( WorldModel.elevation( SEED, x, y ) ) <= 1 );
					}
				}
			}
		}
	}

	@Test
	public void theShoresPlacesTakeNothingOfTheWorlds(){
		int checked = 0;
		for (int sy = -R; sy <= R; sy++){
			for (int sx = -R; sx <= R; sx++){
				WorldStructures.Site land = WorldStructures.landType( SEED, sx, sy );
				WorldStructures.Site site = WorldStructures.siteType( SEED, sx, sy );
				//a sector of the land's (the world's own, the wetlands', the desert's) keeps its place;
				//the sea's only ever take an empty one
				if (land != WorldStructures.Site.NONE) assertEquals( land, site );
				if (!WorldStructures.seaSite( site )) continue;
				SeaSites.Sector sec = SeaSites.sector( SEED, sx, sy );
				int x0, y0, x1, y1;
				if (sec.wreck != null){
					x0 = sec.wreck.x0 - 1; y0 = sec.wreck.y0 - 1; x1 = sec.wreck.x1() + 1; y1 = sec.wreck.y1() + 1;
				} else {
					assertNotNull( sec.light );
					x0 = sec.light.x - 1; y0 = sec.light.y - 2; x1 = sec.light.x + 1; y1 = sec.light.y + 1;
				}
				for (int y = y0; y <= y1; y++){
					for (int x = x0; x <= x1; x++){
						assertFalse( WorldStructures.onRoad( SEED, x, y ) );
						assertFalse( WorldStructures.landTerrainAround( SEED, sx, sy, x, y ) );
						assertEquals( -1, WorldStructures.townCell( x, y ) );
						assertFalse( WindowGenerator.mouthAt( SEED, x, y ) );
					}
				}
				//the place sits inside its own sector: two never touch
				assertEquals( sx, Math.floorDiv( x0, WorldStructures.SECTOR ) );
				assertEquals( sx, Math.floorDiv( x1, WorldStructures.SECTOR ) );
				assertEquals( sy, Math.floorDiv( y0, WorldStructures.SECTOR ) );
				assertEquals( sy, Math.floorDiv( y1, WorldStructures.SECTOR ) );
				checked++;
			}
		}
		assertTrue( checked > 0 );
	}

	@Test
	public void theSeasPlacesKeepNoStarOffItsOldGround(){
		//a fallen star's clearance (WorldEvents.nearSite), as a hunt's ground (HuntEvent.ground), reads
		//the world's own sites: a sector the sea took was an empty one, and a star could fall on it
		int checked = 0;
		for (int sy = -R; sy <= R; sy++){
			for (int sx = -R; sx <= R; sx++){
				if (!WorldStructures.seaSite( WorldStructures.siteType( SEED, sx, sy ) )) continue;
				boolean alone = true;
				for (int dy = -2; dy <= 2 && alone; dy++){
					for (int dx = -2; dx <= 2 && alone; dx++){
						alone = WorldStructures.baseType( SEED, sx + dx, sy + dy ) == WorldStructures.Site.NONE;
					}
				}
				if (!alone) continue;
				assertFalse( WorldEvents.nearSite( SEED, WorldStructures.siteX( SEED, sx, sy ), WorldStructures.siteY( SEED, sx, sy ) ) );
				checked++;
			}
		}
		assertTrue( checked > 0 );
	}

	@Test
	public void anIslandIsLaidWhole(){
		SeaSites.Island i = firstIsland( SEED, false, false );
		assertNotNull( i );
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, i.cx - W / 2, i.cy - H / 2, 0f );
		int ox = i.cx - W / 2, oy = i.cy - H / 2;
		assertNotNull( w.island );
		boolean listed = false;
		for (SeaSites.Sector s : w.sea) listed |= s.island == i;
		assertTrue( listed );
		int land = 0;
		for (int y = i.y0; y <= i.y1; y++){
			for (int x = i.x0; x <= i.x1; x++){
				int c = (x - ox) + (y - oy) * W;
				int t = i.terrain( SEED, x, y );
				if (t == -1) continue;
				assertEquals( t, w.terrain[c] );
				if (t == Terrain.WATER){
					//the shallows: wadeable, never deepened by the tiering
					assertEquals( 1, w.waterDepth[c] );
				} else {
					assertTrue( w.island[c] != 0 );
					land++;
				}
			}
		}
		assertTrue( land >= 50 );
		//the middle is dry land, and the open sea round it is deep
		assertTrue( i.landAt( i.cx, i.cy ) );
		assertEquals( Terrain.DEEP_WATER, w.terrain[(i.x0 - 3 - ox) + (i.cy - oy) * W] );
	}

	@Test
	public void overlappingWindowsAgreeOnAnIsland(){
		SeaSites.Island i = firstIsland( SEED, true, false );
		assertNotNull( i );
		int ox1 = i.cx - W / 2, oy1 = i.cy - H / 2, ox2 = ox1 + 64, oy2 = oy1 - 32;
		WindowGenerator.Window a = WindowGenerator.generate( SEED, 0, ox1, oy1, 0f );
		WindowGenerator.Window b = WindowGenerator.generate( SEED, 0, ox2, oy2, 0f );
		int checked = 0;
		for (int y = i.y0; y <= i.y1; y++){
			for (int x = i.x0; x <= i.x1; x++){
				int ca = (x - ox1) + (y - oy1) * W, cb = (x - ox2) + (y - oy2) * W;
				if (x - ox2 < 1 || y - oy2 < 1 || x - ox2 >= W - 1 || y - oy2 >= H - 1) continue;
				assertEquals( a.terrain[ca], b.terrain[cb] );
				assertEquals( a.waterDepth[ca], b.waterDepth[cb] );
				assertEquals( a.island[ca], b.island[cb] );
				checked++;
			}
		}
		assertTrue( checked > 100 );
	}

	@Test
	public void aWrecksHoldAndALighthousesApronCanBeReached(){
		for (long seed : SEEDS){
			for (SeaSites.Sector s : all( seed )){
				if (s.wreck != null){
					SeaSites.Wreck wr = s.wreck;
					assertEquals( Terrain.CUSTOM_DECO_EMPTY, wr.terrain( wr.holdX, wr.holdY ) );
					int open = 0;
					for (int y = wr.y0; y <= wr.y1(); y++){
						for (int x = wr.x0; x <= wr.x1(); x++){
							if (wr.terrain( x, y ) == Terrain.CUSTOM_DECO_EMPTY) open++;
						}
					}
					assertEquals( 1, open );
					//the cells before its hold: one at least the hero can stand on
					WindowGenerator.Window w = WindowGenerator.generate( seed, 0, wr.holdX - W / 2, wr.holdY - H / 2, 0f );
					int ox = wr.holdX - W / 2, oy = wr.holdY - H / 2;
					boolean way = false;
					for (int[] m : wr.mouth()){
						int t = w.terrain[(m[0] - ox) + (m[1] - oy) * W];
						way |= (Terrain.flags[t] & Terrain.PASSABLE) != 0;
					}
					assertTrue( "no way into the hold at " + wr.holdX + "," + wr.holdY, way );
				}
				if (s.light != null){
					SeaSites.Light l = s.light;
					assertEquals( Terrain.CUSTOM_DECO, l.terrain( l.x, l.y ) );
					assertEquals( Terrain.CUSTOM_DECO, l.terrain( l.x, l.y - 1 ) );
					for (int dx = -1; dx <= 1; dx++) assertEquals( Terrain.CUSTOM_DECO_EMPTY, l.terrain( l.x + dx, l.y + 1 ) );
				}
				if (s.island != null && s.island.digX != Integer.MIN_VALUE){
					assertEquals( Terrain.EMPTY_SP, s.island.terrain( seed, s.island.digX, s.island.digY ) );
				}
			}
		}
	}

	@Test
	public void namesAreTheSeeds(){
		assertEquals( SeaSites.islandName( SEED, 3, -4 ), SeaSites.islandName( SEED, 3, -4 ) );
		assertEquals( SeaSites.shipName( SEED, 3, -4 ), SeaSites.shipName( SEED, 3, -4 ) );
		assertFalse( SeaSites.islandName( SEED, 3, -4 ).isEmpty() );
		assertFalse( SeaSites.lightName( SEED, 3, -4 ).isEmpty() );
	}
}

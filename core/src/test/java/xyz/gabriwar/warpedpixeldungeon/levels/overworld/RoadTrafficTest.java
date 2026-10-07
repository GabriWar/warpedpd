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

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The roads' life as pure functions (RoadTraffic): who walks which road when, door to door
 * and at their own pace; the town watch's beat out and back; one caravan in six fallen on;
 * the stalls exactly where they always stood; rumours that only ever point at real places;
 * and a traffic day that is the world's day on both clocks.
 */
public class RoadTrafficTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W0 = 150, W1_SUMMER = 1850, W1_WINTER = 1050;

	private int challenges, cycleTurn;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		challenges = Dungeon.challenges;
		cycleTurn = Dungeon.cycleTurn;
		Dungeon.challenges = 0;
	}

	@After
	public void restore(){
		Dungeon.challenges = challenges;
		Dungeon.cycleTurn = cycleTurn;
	}

	private static ArrayList<int[]> travelled( ArrayList<int[]> roads ){
		ArrayList<int[]> out = new ArrayList<>();
		for (int[] r : roads) if (RoadTraffic.travelled( SEED, r[0], r[1], r[2], r[3] )) out.add( r );
		return out;
	}

	@Test
	public void roadsAreTheCaravansPairs(){
		ArrayList<int[]> roads = RoadTraffic.roads( SEED, -8, -8, 8, 8 );
		assertTrue( "roads: " + roads.size(), roads.size() >= 20 );
		for (int[] r : roads){
			long nv = WorldStructures.roadNeighbour( SEED, r[0], r[1] );
			assertEquals( nv, WorldStructures.sectorOf( r[2], r[3] ) );
			assertTrue( WorldStructures.sectorOf( r[0], r[1] ) <= nv );
		}
	}

	@Test
	public void travelPairsAreEveryDrawnRoadOnce(){
		ArrayList<int[]> pairs = RoadTraffic.travelPairs( SEED, -8, -8, 8, 8 );
		HashSet<String> seen = new HashSet<>();
		for (int[] p : pairs){
			//a drawn road: one end is the other's nearest village; named lower sector first
			assertTrue( WorldStructures.roadNeighbour( SEED, p[0], p[1] ) == WorldStructures.sectorOf( p[2], p[3] )
					|| WorldStructures.roadNeighbour( SEED, p[2], p[3] ) == WorldStructures.sectorOf( p[0], p[1] ) );
			assertTrue( WorldStructures.sectorOf( p[0], p[1] ) <= WorldStructures.sectorOf( p[2], p[3] ) );
			assertTrue( "twice: " + p[0] + "," + p[1] + "-" + p[2] + "," + p[3], seen.add( p[0] + "," + p[1] + "-" + p[2] + "," + p[3] ) );
		}
		//every caravan road is walked too, and every village's road is there
		for (int[] r : RoadTraffic.roads( SEED, -8, -8, 8, 8 )){
			assertTrue( seen.contains( r[0] + "," + r[1] + "-" + r[2] + "," + r[3] ) );
		}
		int extra = 0;
		for (int sy = -8; sy <= 8; sy++){
			for (int sx = -8; sx <= 8; sx++){
				if (WorldStructures.siteType( SEED, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				long nv = WorldStructures.roadNeighbour( SEED, sx, sy );
				if (nv == Long.MIN_VALUE) continue;
				int nx = (int)(nv >> 32), ny = (int) nv;
				boolean lower = WorldStructures.sectorOf( sx, sy ) <= nv;
				String k = lower ? sx + "," + sy + "-" + nx + "," + ny : nx + "," + ny + "-" + sx + "," + sy;
				assertTrue( "missing " + k, seen.contains( k ) );
				if (!lower && WorldStructures.roadNeighbour( SEED, nx, ny ) != WorldStructures.sectorOf( sx, sy )) extra++;
			}
		}
		assertTrue( "no road beyond the caravans' in the sample", extra > 0 );
	}

	@Test
	public void travellersKeepToTheClock(){
		ArrayList<int[]> roads = travelled( RoadTraffic.travelPairs( SEED, -8, -8, 8, 8 ) );
		int trips = 0;
		for (int i = 0; i < 24 && i < roads.size(); i++){
			int[] r = roads.get( i );
			int lanes = RoadTraffic.lanes( SEED, r[0], r[1], r[2], r[3] );
			for (int lane = 0; lane < lanes; lane++){
				for (int day = 0; day < 10; day++){
					for (int w1 : new int[]{ W1_SUMMER, W1_WINTER }){
						long lastKey = 0;
						int lastArrive = Integer.MIN_VALUE;
						float lastS = -1;
						for (int tod = 0; tod < DayNightCycle.FULL_CYCLE; tod += 2){
							RoadTraffic.Trip t = RoadTraffic.tripAt( SEED, r[0], r[1], r[2], r[3], lane, day, tod, W0, w1 );
							if (tod < W0 || tod >= w1){
								assertNull( t );
								continue;
							}
							if (t == null) continue;
							RoadTraffic.Trip again = RoadTraffic.tripAt( SEED, r[0], r[1], r[2], r[3], lane, day, tod, W0, w1 );
							assertEquals( t.key, again.key );
							assertEquals( t.kind, again.kind );
							assertEquals( t.depart, again.depart );
							assertEquals( t.arrive, again.arrive );
							float s = t.distanceAt( tod );
							if (t.key != lastKey){
								//the next walker of the lane: after the last one is in, and a rest
								if (lastArrive != Integer.MIN_VALUE) assertTrue( t.depart >= lastArrive + 30 );
								assertTrue( t.depart >= W0 );
								assertTrue( t.arrive <= w1 );
								assertEquals( 0f, t.distanceAt( t.depart ), 1e-3f );
								assertEquals( RoadTraffic.length( t.route ), t.distanceAt( t.arrive ), 1e-3f );
								assertEquals( t.day, day );
								//from one end of the road to the other
								boolean fwd = t.fromSx == r[0] && t.fromSy == r[1] && t.toSx == r[2] && t.toSy == r[3];
								boolean back = t.fromSx == r[2] && t.fromSy == r[3] && t.toSx == r[0] && t.toSy == r[1];
								assertTrue( fwd || back );
								lastKey = t.key;
								lastArrive = t.arrive;
								trips++;
							} else {
								assertTrue( "walked back", s >= lastS );
							}
							assertTrue( tod >= t.depart && tod < t.arrive );
							lastS = s;
						}
					}
				}
			}
		}
		assertTrue( "trips: " + trips, trips > 300 );
	}

	@Test
	public void aboutOneTravellerPerRoadByDay(){
		ArrayList<int[]> roads = travelled( RoadTraffic.travelPairs( SEED, -8, -8, 8, 8 ) );
		long samples = 0, out = 0;
		for (int i = 0; i < 60 && i < roads.size(); i++){
			int[] r = roads.get( i );
			int lanes = RoadTraffic.lanes( SEED, r[0], r[1], r[2], r[3] );
			for (int day = 0; day < 20; day++){
				for (int tod = W0; tod < W1_SUMMER; tod += 10){
					samples++;
					for (int lane = 0; lane < lanes; lane++){
						if (RoadTraffic.tripAt( SEED, r[0], r[1], r[2], r[3], lane, day, tod, W0, W1_SUMMER ) != null) out++;
					}
				}
			}
		}
		double mean = out / (double) samples;
		System.out.println( "[traffic] travellers per road by day: " + mean + " over " + Math.min( 60, roads.size() ) + " roads" );
		assertTrue( "travellers per road by day: " + mean, mean >= 0.5 && mean <= 1.6 );
	}

	//a settlement can roll no house at all (WorldStructures.settlementLayout): nobody walks out of
	//or into such a place, and every road that is walked, far and wide over several worlds, has a
	//door at each end for its walkers (a walk picks one by dividing by the lived-in houses)
	@Test
	public void everyWalkedRoadHasADoorAtEachEnd(){
		int bare = 0, walked = 0;
		for (long seed = 1; seed <= 6; seed++){
			for (int[] r : RoadTraffic.travelPairs( seed, -25, -25, 25, 25 )){
				boolean peaceful = WorldStructures.faction( seed, r[0], r[1] ) != WorldStructures.Faction.BANDIT
						&& WorldStructures.faction( seed, r[2], r[3] ) != WorldStructures.Faction.BANDIT;
				if (RoadTraffic.populatedHouses( seed, r[0], r[1] ) == 0 || RoadTraffic.populatedHouses( seed, r[2], r[3] ) == 0){
					if (peaceful) bare++;
					assertFalse( "a walk to a place with no house: " + seed + " " + Arrays.toString( r ),
							RoadTraffic.travelled( seed, r[0], r[1], r[2], r[3] ) );
					continue;
				}
				if (!RoadTraffic.travelled( seed, r[0], r[1], r[2], r[3] )) continue;
				walked++;
				for (int lane = 0; lane < RoadTraffic.lanes( seed, r[0], r[1], r[2], r[3] ); lane++){
					for (int tod : new int[]{ W0, 600, 1200, W1_SUMMER - 1 }){
						RoadTraffic.tripAt( seed, r[0], r[1], r[2], r[3], lane, 7, tod, W0, W1_SUMMER );
					}
				}
				//the road scene's own walkers, from the first house at each end
				RoadTraffic.forcedTrip( seed, r[0], r[1], r[2], r[3], RoadTraffic.PILGRIM, 7, 600,
						WorldStructures.siteX( seed, r[0], r[1] ), WorldStructures.siteY( seed, r[0], r[1] ), 10, 1L );
			}
		}
		System.out.println( "[traffic] " + walked + " walked roads, " + bare + " left to a place with no house" );
		assertTrue( "no peaceful settlement without a house in the sample", bare > 0 );
		assertTrue( "walked " + walked, walked > 1000 );
	}

	@Test
	public void aRealClockDayKeepsItsLanesGoing(){
		//a real day's daylight is some 80,000 traffic turns: lanes walk on until dusk
		int w0 = 6 * 3600 * RoadTraffic.TURNS_PER_SECOND, w1 = 20 * 3600 * RoadTraffic.TURNS_PER_SECOND;
		ArrayList<int[]> roads = travelled( RoadTraffic.travelPairs( SEED, -6, -6, 6, 6 ) );
		int late = 0, lanes = 0;
		for (int i = 0; i < 20 && i < roads.size(); i++){
			int[] r = roads.get( i );
			for (int lane = 0; lane < RoadTraffic.lanes( SEED, r[0], r[1], r[2], r[3] ); lane++){
				lanes++;
				for (int tod = w1 - 3000; tod < w1 - 1000; tod += 100){
					if (RoadTraffic.tripAt( SEED, r[0], r[1], r[2], r[3], lane, 5, tod, w0, w1 ) != null){
						late++;
						break;
					}
				}
			}
		}
		assertTrue( "lanes still walked late in a real day: " + late + " of " + lanes, late * 2 >= lanes );
	}

	@Test
	public void routesRunDoorToDoor(){
		ArrayList<int[]> roads = travelled( RoadTraffic.travelPairs( SEED, -10, -10, 10, 10 ) );
		int checked = 0;
		for (int i = 0; checked < 30 && i < roads.size(); i++){
			int[] r = roads.get( i );
			RoadTraffic.Trip t = null;
			for (int tod = W0; t == null && tod < W1_SUMMER; tod += 25){
				t = RoadTraffic.tripAt( SEED, r[0], r[1], r[2], r[3], 0, 2, tod, W0, W1_SUMMER );
			}
			if (t == null) continue;
			float[] p = t.route;
			assertEquals( 8, p.length );
			assertEquals( Terrain.DOOR, WorldStructures.terrainAt( SEED, Math.round( p[0] ), Math.round( p[1] ) ) );
			assertEquals( Terrain.DOOR, WorldStructures.terrainAt( SEED, Math.round( p[6] ), Math.round( p[7] ) ) );
			assertEquals( WorldStructures.siteX( SEED, t.fromSx, t.fromSy ), Math.round( p[2] ) );
			assertEquals( WorldStructures.siteY( SEED, t.fromSx, t.fromSy ), Math.round( p[3] ) );
			assertEquals( WorldStructures.siteX( SEED, t.toSx, t.toSy ), Math.round( p[4] ) );
			assertEquals( WorldStructures.siteY( SEED, t.toSx, t.toSy ), Math.round( p[5] ) );
			//the doors are of houses with folk in them
			assertEquals( WorldStructures.sectorOf( t.fromSx, t.fromSy ), WorldStructures.houseSector( SEED, Math.round( p[0] ), Math.round( p[1] ) ) );
			assertEquals( WorldStructures.sectorOf( t.toSx, t.toSy ), WorldStructures.houseSector( SEED, Math.round( p[6] ), Math.round( p[7] ) ) );
			checked++;
		}
		assertEquals( 30, checked );
	}

	@Test
	public void routeGeometryIsConsistent(){
		float[] route = { 0, 0, 10, 0, 10, 20 };
		assertEquals( 30f, RoadTraffic.length( route ), 1e-4f );
		assertArrayEquals( new float[]{ 5, 0 }, RoadTraffic.point( route, 5 ), 1e-4f );
		assertArrayEquals( new float[]{ 10, 5 }, RoadTraffic.point( route, 15 ), 1e-4f );
		assertArrayEquals( new float[]{ 0, 0 }, RoadTraffic.point( route, -3 ), 1e-4f );
		assertArrayEquals( new float[]{ 10, 20 }, RoadTraffic.point( route, 99 ), 1e-4f );
		for (float s = 0; s <= 30; s += 0.5f){
			float[] p = RoadTraffic.point( route, s );
			assertEquals( s, RoadTraffic.project( route, p[0], p[1] ), 1e-3f );
		}
		assertEquals( 15f, RoadTraffic.project( route, 14, 5 ), 1e-3f );
	}

	//the roads' DIRT_PATH really runs along the straight line between the wells the walkers'
	//clock is laid on - within the six cells OverworldLevel.trafficCell looks
	@Test
	public void roadsStayNearTheirLine(){
		ArrayList<int[]> roads = travelled( RoadTraffic.roads( SEED, -8, -8, 8, 8 ) );
		int used = 0, samples = 0, hits = 0;
		for (int[] r : roads){
			if (used >= 6) break;
			int ax = WorldStructures.siteX( SEED, r[0], r[1] ), ay = WorldStructures.siteY( SEED, r[0], r[1] );
			int bx = WorldStructures.siteX( SEED, r[2], r[3] ), by = WorldStructures.siteY( SEED, r[2], r[3] );
			double len = Math.sqrt( (double)(bx - ax) * (bx - ax) + (double)(by - ay) * (by - ay) );
			if (len > 150) continue;
			used++;
			int ra = WorldStructures.settlementLayout( SEED, r[0], r[1] )[0], rb = WorldStructures.settlementLayout( SEED, r[2], r[3] )[0];
			int ox = (ax + bx) / 2 - WindowGenerator.WIDTH / 2, oy = (ay + by) / 2 - WindowGenerator.HEIGHT / 2;
			WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, 0f );
			for (int i = 0; i <= (int) len; i++){
				float x = ax + (bx - ax) * (float)(i / len), y = ay + (by - ay) * (float)(i / len);
				if (Math.hypot( x - ax, y - ay ) < ra + 4 || Math.hypot( x - bx, y - by ) < rb + 4) continue;
				int lx = Math.round( x ) - ox, ly = Math.round( y ) - oy;
				if (lx < 8 || ly < 8 || lx >= WindowGenerator.WIDTH - 8 || ly >= WindowGenerator.HEIGHT - 8) continue;
				samples++;
				boolean near = false;
				for (int dy = -6; dy <= 6 && !near; dy++){
					for (int dx = -6; dx <= 6 && !near; dx++){
						int t = w.terrain[(lx + dx) + (ly + dy) * WindowGenerator.WIDTH];
						near = t == Terrain.DIRT_PATH || t == Terrain.BRIDGE;
					}
				}
				if (near) hits++;
			}
		}
		System.out.println( "[traffic] road near its line at " + hits + " of " + samples );
		assertTrue( "samples: " + samples, samples >= 100 );
		assertTrue( "road near the line at " + hits + " of " + samples, hits >= samples * 0.75 );
	}

	@Test
	public void aboutOneCaravanInSixIsAmbushed(){
		ArrayList<int[]> roads = RoadTraffic.roads( SEED, -8, -8, 8, 8 );
		int n = 0, hit = 0;
		for (int[] r : roads){
			for (int day = 0; day < 200; day++){
				boolean a = RoadTraffic.ambushed( SEED, r[0], r[1], r[2], r[3], day );
				assertEquals( a, RoadTraffic.ambushed( SEED, r[0], r[1], r[2], r[3], day ) );
				//the same road, named from its other end
				assertEquals( a, RoadTraffic.ambushed( SEED, r[2], r[3], r[0], r[1], day ) );
				n++;
				if (a) hit++;
			}
		}
		double rate = hit / (double) n;
		System.out.println( "[traffic] ambush rate " + rate + " over " + n + " road-days" );
		assertTrue( "ambush rate " + rate, rate >= 0.13 && rate <= 0.20 );
	}

	//the stall where OverworldLevel.placeCaravans has always pitched it (its old arithmetic, kept here)
	private static int[] oldStall( int sx, int sy, int nx, int ny, int today, int dayOfSeason ){
		long ph = OverworldLevel.structHash( sx * 7919L + nx, sy * 7919L + ny );
		if ((((ph >>> 3) ^ dayOfSeason) & 1L) != 0) return null;
		float t = (today + 1) / 8f;
		if ((ph & 1L) != 0) t = 1f - t;
		int ax = WorldStructures.siteX( SEED, sx, sy );
		int ay = WorldStructures.siteY( SEED, sx, sy );
		int bx = WorldStructures.siteX( SEED, nx, ny );
		int by = WorldStructures.siteY( SEED, nx, ny );
		return new int[]{ Math.round( ax + (bx - ax) * t ), Math.round( ay + (by - ay) * t ) };
	}

	//the caravan's morning leg (RoadTraffic.caravanLeg): out exactly on the days its stall stands,
	//from where yesterday's stood (on the week's first day, the well it is walked from) to today's
	//spot, toward the village it is bound for and never back, setting out after the walkers do
	//and in by mid-morning, the stall's hour; and pure
	@Test
	public void theCaravansCartKeepsToItsLeg(){
		ArrayList<int[]> roads = RoadTraffic.roads( SEED, -8, -8, 8, 8 );
		int legs = 0, fromWells = 0;
		for (int i = 0; i < roads.size() && i < 30; i++){
			int[] r = roads.get( i );
			for (int dos = 1; dos <= 4; dos++){
				for (int wd = 0; wd < 7; wd++){
					int day = 100 + dos * 7 + wd;
					int[] spot = RoadTraffic.caravanSpot( SEED, r[0], r[1], r[2], r[3], wd, dos );
					RoadTraffic.Trip leg = RoadTraffic.caravanLeg( SEED, r[0], r[1], r[2], r[3], wd, dos, day, W0 );
					if (spot == null){
						assertNull( leg );
						continue;
					}
					assertNotNull( leg );
					legs++;
					assertEquals( RoadTraffic.CARAVAN, leg.kind );
					assertEquals( day, leg.day );
					assertEquals( spot[0], leg.route[2], 0f );
					assertEquals( spot[1], leg.route[3], 0f );
					//its two villages are the road's, the one it is bound for at the stall's end
					assertTrue( (leg.fromSx == r[0] && leg.fromSy == r[1] && leg.toSx == r[2] && leg.toSy == r[3])
							|| (leg.fromSx == r[2] && leg.fromSy == r[3] && leg.toSx == r[0] && leg.toSy == r[1]) );
					float fx = WorldStructures.siteX( SEED, leg.fromSx, leg.fromSy ), fy = WorldStructures.siteY( SEED, leg.fromSx, leg.fromSy );
					float tx = WorldStructures.siteX( SEED, leg.toSx, leg.toSy ), ty = WorldStructures.siteY( SEED, leg.toSx, leg.toSy );
					if (wd == 0){
						fromWells++;
						assertEquals( fx, leg.route[0], 0.01f );
						assertEquals( fy, leg.route[1], 0.01f );
					} else {
						int[] yesterday = RoadTraffic.caravanSpot( SEED, r[0], r[1], r[2], r[3], wd - 1, dos );
						assertNotNull( yesterday );
						assertTrue( Math.abs( yesterday[0] - leg.route[0] ) <= 0.5f && Math.abs( yesterday[1] - leg.route[1] ) <= 0.5f );
					}
					assertTrue( "walked away from where it is bound",
							Math.hypot( spot[0] - tx, spot[1] - ty ) < Math.hypot( leg.route[0] - tx, leg.route[1] - ty ) );
					//out after the walkers set out, in by mid-morning
					float len = RoadTraffic.length( leg.route );
					assertTrue( leg.depart >= W0 && leg.depart <= W0 + 150 );
					assertEquals( Math.max( 1, (int) Math.ceil( len / RoadTraffic.CARAVAN_PACE ) ), leg.arrive - leg.depart );
					assertTrue( "in at " + leg.arrive, leg.arrive < DayNightCycle.phaseDuration( DayNightCycle.Phase.DAWN ) + 250 );
					//along the road and never back: still till it sets out, at the spot from its arrival
					float last = -1f;
					for (int tod = 0; tod < DayNightCycle.FULL_CYCLE; tod++){
						float s = leg.distanceAt( tod );
						assertTrue( s >= last );
						last = s;
						if (tod <= leg.depart) assertEquals( 0f, s, 0f );
						if (tod >= leg.arrive) assertEquals( len, s, 1e-3f );
					}
					RoadTraffic.Trip again = RoadTraffic.caravanLeg( SEED, r[0], r[1], r[2], r[3], wd, dos, day, W0 );
					assertArrayEquals( leg.route, again.route, 0f );
					assertEquals( leg.depart, again.depart );
					assertEquals( leg.arrive, again.arrive );
					assertEquals( leg.key, again.key );
				}
			}
		}
		assertTrue( "legs " + legs, legs > 100 );
		assertTrue( fromWells > 0 );
	}

	@Test
	public void caravansStandOnHalfTheRoads(){
		ArrayList<int[]> roads = RoadTraffic.roads( SEED, -8, -8, 8, 8 );
		int n = 0, standing = 0;
		for (int[] r : roads){
			int ax = WorldStructures.siteX( SEED, r[0], r[1] ), ay = WorldStructures.siteY( SEED, r[0], r[1] );
			int bx = WorldStructures.siteX( SEED, r[2], r[3] ), by = WorldStructures.siteY( SEED, r[2], r[3] );
			for (int dos = 1; dos <= 90; dos++){
				for (int wd = 0; wd < 7; wd++){
					int[] spot = RoadTraffic.caravanSpot( SEED, r[0], r[1], r[2], r[3], wd, dos );
					int[] old = oldStall( r[0], r[1], r[2], r[3], wd, dos );
					n++;
					if (old == null){
						assertNull( spot );
						continue;
					}
					assertArrayEquals( old, spot );
					standing++;
					//on the segment between the wells, within a cell
					double vx = bx - ax, vy = by - ay;
					double t = ((spot[0] - ax) * vx + (spot[1] - ay) * vy) / (vx * vx + vy * vy);
					assertTrue( t >= 0 && t <= 1 );
					double ex = spot[0] - (ax + vx * t), ey = spot[1] - (ay + vy * t);
					assertTrue( Math.sqrt( ex * ex + ey * ey ) <= 1.0 );
				}
			}
		}
		double rate = standing / (double) n;
		assertTrue( "stall rate " + rate, rate >= 0.40 && rate <= 0.60 );
	}

	//the watch walks the whole road: from a door past its own well to the neighbouring village's
	//well, a stand there and home again, each beat over before the dusk - two a summer's day,
	//and at least one on a winter's
	@Test
	public void theWatchWalksTheWholeRoadAndBack(){
		int towns = 0;
		for (int sy = -10; sy <= 10 && towns < 6; sy++){
			for (int sx = -10; sx <= 10 && towns < 6; sx++){
				if (!RoadTraffic.patrolTown( SEED, sx, sy )) continue;
				towns++;
				RoadTraffic.Beat beat = RoadTraffic.beat( SEED, sx, sy, 3, W0, W1_SUMMER );
				assertNotNull( beat );
				long nv = WorldStructures.roadNeighbour( SEED, sx, sy );
				assertEquals( "turns at the neighbour's well", WorldStructures.siteX( SEED, (int)(nv >> 32), (int) nv ), Math.round( beat.route[4] ) );
				assertEquals( WorldStructures.siteY( SEED, (int)(nv >> 32), (int) nv ), Math.round( beat.route[5] ) );
				assertTrue( "two beats on a summer's day: " + beat.starts.length, beat.starts.length >= 2 );
				RoadTraffic.Beat winter = RoadTraffic.beat( SEED, sx, sy, 3, W0, W1_WINTER );
				assertNotNull( "no beat fits a winter's day", winter );
				assertTrue( winter.starts[winter.starts.length - 1] + winter.cycle() <= W1_WINTER );
				//home by dusk: neither of the pair out once the day's walking is over
				for (int tod = W1_WINTER; tod < W1_WINTER + 50; tod++){
					assertEquals( -1f, winter.distanceAt( tod, 0 ), 0f );
					assertEquals( -1f, winter.distanceAt( tod, 1 ), 0f );
				}
				int[] st = beat.starts;
				assertTrue( st.length > 0 );
				assertTrue( st[0] >= W0 + 40 );
				for (int i = 1; i < st.length; i++) assertTrue( st[i] - st[i-1] >= beat.cycle() + RoadTraffic.Beat.REST );
				assertTrue( st[st.length - 1] + beat.cycle() <= W1_SUMMER );
				float len = RoadTraffic.length( beat.route );
				int walk = (int) Math.ceil( len / RoadTraffic.Beat.PACE );
				for (int tod = 0; tod < st[0]; tod++) assertEquals( -1f, beat.distanceAt( tod, 0 ), 0f );
				for (int i = 0; i < st.length; i++){
					float last = -1;
					for (int u = 0; u < beat.cycle(); u++){
						int tod = st[i] + u;
						float s0 = beat.distanceAt( tod, 0 ), s1 = beat.distanceAt( tod, 1 );
						assertTrue( s0 >= 0 && s0 <= len );
						assertTrue( s1 >= 0 && s1 <= len );
						assertTrue( Math.abs( s1 - s0 ) <= 1.5f + 1e-3f );
						if (u < walk){
							assertTrue( s0 >= last );
							assertEquals( 1, beat.heading( tod ) );
						} else if (u < walk + RoadTraffic.Beat.PAUSE){
							assertEquals( len, s0, 1e-3f );
							assertEquals( 0, beat.heading( tod ) );
						} else {
							assertTrue( s0 <= last );
							assertEquals( -1, beat.heading( tod ) );
						}
						last = s0;
					}
					//indoors between two beats
					int next = i + 1 < st.length ? st[i + 1] : W1_SUMMER;
					for (int tod = st[i] + beat.cycle(); tod < next; tod++) assertEquals( -1f, beat.distanceAt( tod, 0 ), 0f );
				}
				assertEquals( Terrain.DOOR, WorldStructures.terrainAt( SEED, Math.round( beat.route[0] ), Math.round( beat.route[1] ) ) );
				assertEquals( WorldStructures.siteX( SEED, sx, sy ), Math.round( beat.route[2] ) );
				assertEquals( WorldStructures.siteY( SEED, sx, sy ), Math.round( beat.route[3] ) );
				//and it is deterministic
				RoadTraffic.Beat again = RoadTraffic.beat( SEED, sx, sy, 3, W0, W1_SUMMER );
				assertArrayEquals( beat.starts, again.starts );
				assertArrayEquals( beat.route, again.route, 0f );
			}
		}
		assertTrue( "towns with a watch: " + towns, towns >= 3 );
	}

	//a big human town whose road runs to an outlaw village keeps its watch at home (the travellers
	//keep off that road too): nobody nobody can hurt goes pacing into the outlaws' camp
	@Test
	public void theWatchKeepsOffTheOutlawsRoad(){
		int kept = 0, towns = 0;
		for (long seed = 1; seed <= 40; seed++){
			for (int sy = -6; sy <= 6; sy++){
				for (int sx = -6; sx <= 6; sx++){
					if (WorldStructures.siteType( seed, sx, sy ) != WorldStructures.Site.VILLAGE
							|| WorldStructures.faction( seed, sx, sy ) != WorldStructures.Faction.HUMAN
							|| (WorldStructures.settlementLayout( seed, sx, sy ).length - 1) / 2 < 9) continue;
					long nv = WorldStructures.roadNeighbour( seed, sx, sy );
					if (nv == Long.MIN_VALUE) continue;
					if (WorldStructures.faction( seed, (int)(nv >> 32), (int) nv ) == WorldStructures.Faction.BANDIT){
						kept++;
						assertFalse( RoadTraffic.patrolTown( seed, sx, sy ) );
						assertNull( RoadTraffic.beatRoute( seed, sx, sy ) );
						assertNull( RoadTraffic.beat( seed, sx, sy, 3, W0, W1_SUMMER ) );
					} else {
						towns++;
						assertTrue( RoadTraffic.patrolTown( seed, sx, sy ) );
					}
				}
			}
		}
		System.out.println( "[traffic] " + towns + " towns walk their road, " + kept + " keep off an outlaw village's" );
		assertTrue( "no town by an outlaw village in the sample", kept > 0 );
		assertTrue( "towns " + towns, towns > kept );
	}

	//the site of a kind nearest a world cell, searched round it
	private static boolean siteAt( int x, int y, WorldStructures.Site type, WorldStructures.Faction faction ){
		int sx0 = Math.floorDiv( x, WorldStructures.SECTOR ), sy0 = Math.floorDiv( y, WorldStructures.SECTOR );
		for (int sy = sy0 - 4; sy <= sy0 + 4; sy++){
			for (int sx = sx0 - 4; sx <= sx0 + 4; sx++){
				if (WorldStructures.siteType( SEED, sx, sy ) != type) continue;
				if (faction != null && WorldStructures.faction( SEED, sx, sy ) != faction) continue;
				if (WorldStructures.siteX( SEED, sx, sy ) == x && WorldStructures.siteY( SEED, sx, sy ) == y) return true;
			}
		}
		return false;
	}

	@Test
	public void rumoursPointAtRealPlaces(){
		HashSet<Long> none = new HashSet<>(), dragons = new HashSet<>();
		int told = 0;
		HashMap<Integer, Integer> kinds = new HashMap<>();
		for (int sy = -6; sy <= 6; sy++){
			for (int sx = -6; sx <= 6; sx++){
				if (WorldStructures.siteType( SEED, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				int wx = WorldStructures.siteX( SEED, sx, sy ), wy = WorldStructures.siteY( SEED, sx, sy );
				for (long pick = 0; pick < 8; pick++){
					int day = 11, weekday = (int)(pick % 7), dos = 3 + (int) pick;
					int[] r = RoadTraffic.rumour( SEED, wx, wy, pick << 7, day, weekday, dos, none );
					if (r == null) continue;
					told++;
					kinds.merge( r[0], 1, Integer::sum );
					double d = Math.hypot( r[1] - wx, r[2] - wy );
					assertTrue( "distance " + d, d >= 24 && d <= 320 );
					switch (r[0]){
						case RoadTraffic.RUMOUR_DRAGON:
							assertTrue( siteAt( r[1], r[2], WorldStructures.Site.DRAGON, null ) );
							break;
						case RoadTraffic.RUMOUR_RUIN:
							assertTrue( siteAt( r[1], r[2], WorldStructures.Site.RUIN, null ) );
							break;
						case RoadTraffic.RUMOUR_OUTLAWS:
							assertTrue( siteAt( r[1], r[2], WorldStructures.Site.VILLAGE, WorldStructures.Faction.BANDIT ) );
							break;
						default:
							//today's stall on a road in range, and outlaws there exactly when they lie in wait
							boolean found = false;
							int rsx = Math.floorDiv( wx, WorldStructures.SECTOR ), rsy = Math.floorDiv( wy, WorldStructures.SECTOR );
							for (int[] road : RoadTraffic.roads( SEED, rsx - 3, rsy - 3, rsx + 3, rsy + 3 )){
								int[] spot = RoadTraffic.caravanSpot( SEED, road[0], road[1], road[2], road[3], weekday, dos );
								if (spot == null || spot[0] != r[1] || spot[1] != r[2]) continue;
								found = true;
								assertEquals( r[0] == RoadTraffic.RUMOUR_WATCHED,
										RoadTraffic.ambushed( SEED, road[0], road[1], road[2], road[3], day ) );
							}
							assertTrue( "no stall at " + r[1] + "," + r[2], found );
							//where a stall can go up: road within four cells
							assertTrue( RoadTraffic.roadNear( SEED, r[1], r[2] ) );
					}
				}
			}
		}
		System.out.println( "[traffic] rumours told " + told + ", by kind " + kinds );
		assertTrue( "rumours: " + told, told >= 10 );
		assertTrue( "kinds told: " + kinds, kinds.size() >= 3 );
		assertTrue( "no caravan rumour: " + kinds, kinds.containsKey( RoadTraffic.RUMOUR_CARAVAN ) || kinds.containsKey( RoadTraffic.RUMOUR_WATCHED ) );

		//a slain dragon's lair is never sent to
		for (int sy = -12; sy <= 12; sy++){
			for (int sx = -12; sx <= 12; sx++){
				if (WorldStructures.siteType( SEED, sx, sy ) == WorldStructures.Site.DRAGON) dragons.add( WorldStructures.sectorOf( sx, sy ) );
			}
		}
		for (int sy = -6; sy <= 6; sy++){
			for (int sx = -6; sx <= 6; sx++){
				if (WorldStructures.siteType( SEED, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
				int wx = WorldStructures.siteX( SEED, sx, sy ), wy = WorldStructures.siteY( SEED, sx, sy );
				for (long pick = 0; pick < 8; pick++){
					int[] r = RoadTraffic.rumour( SEED, wx, wy, pick << 7, 11, 2, 5, dragons );
					if (r != null) assertNotEquals( RoadTraffic.RUMOUR_DRAGON, r[0] );
				}
			}
		}
	}

	@Test
	public void compassReadsTheMap(){
		assertEquals( 0, RoadTraffic.compass( 10, 0 ) );
		assertEquals( 1, RoadTraffic.compass( 10, -10 ) );
		assertEquals( 2, RoadTraffic.compass( 0, -10 ) );
		assertEquals( 3, RoadTraffic.compass( -10, -10 ) );
		assertEquals( 4, RoadTraffic.compass( -10, 0 ) );
		assertEquals( 5, RoadTraffic.compass( -10, 10 ) );
		assertEquals( 6, RoadTraffic.compass( 0, 10 ) );
		assertEquals( 7, RoadTraffic.compass( 10, 10 ) );
	}

	@Test
	public void theTrafficDayIsTheWorldsDay(){
		//a turn-counted run: the day and night cycle's own day, starting at dawn
		for (int turn : new int[]{ 0, 1, 2499, 2500, 12_345, 99_999, 250_000 }){
			Dungeon.cycleTurn = turn;
			long now = RoadTraffic.now();
			assertEquals( turn, now );
			assertEquals( WorldClock.day(), RoadTraffic.day( now ) );
			assertEquals( turn % DayNightCycle.FULL_CYCLE, RoadTraffic.turnOfDay( now ) );
		}
		assertEquals( 150, RoadTraffic.setOut() );
		//a real-clock run: the local calendar day, two traffic turns to the second
		for (long ms : new long[]{ 0L, 1L, 86_399_999L, 86_400_000L, -1L, -86_400_001L, 1_790_000_000_123L }){
			assertEquals( Math.floorDiv( ms, 86_400_000L ), RoadTraffic.realDay( ms ) );
			assertEquals( Math.floorDiv( ms, 1000L ) * 2, RoadTraffic.realNow( ms ) );
		}
		Dungeon.challenges = Challenges.REAL_CLOCK;
		assertEquals( 86_400 * RoadTraffic.TURNS_PER_SECOND, RoadTraffic.cycle() );
		assertEquals( WorldClock.day(), RoadTraffic.day( RoadTraffic.now() ) );
		assertTrue( RoadTraffic.setOut() > 0 && RoadTraffic.setOut() < RoadTraffic.indoors()
				&& RoadTraffic.indoors() < RoadTraffic.cycle() );
	}
}

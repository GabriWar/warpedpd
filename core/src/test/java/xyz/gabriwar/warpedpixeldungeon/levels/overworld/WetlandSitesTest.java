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

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The wetlands' places on the world's grid (WetlandSites): each kind is laid in every world tried,
 * on its own ground (a swamp for a hut and a shrine, a beach by the sea for the pools), clear of the
 * town, the roads, every older site's grounds and every village's fields, the same however often
 * and in whatever order it is asked; no older site changes for them; their footprints come out as
 * the generator lays them, the water freezing with the ground. And their rules: the tide, the pools'
 * refill, the shrine's wait and blessing, the swamp's mist.
 */
public class WetlandSitesTest {

	//world seeds: the live tests' and the probes' (dungeon seeds 1, 2, 3, 0x6E1DE, 0xB055 made world seeds)
	private static final long[] SEEDS = { 0x5EED0F7EA7L, OverworldLevel.worldSeedOf( 1 ), OverworldLevel.worldSeedOf( 2 ),
			OverworldLevel.worldSeedOf( 3 ), OverworldLevel.worldSeedOf( 0x6E1DEL ), OverworldLevel.worldSeedOf( 0xB055L ) };
	//the sectors round the origin each world is searched over
	private static final int R = 15;

	private static ArrayList<WetlandSites.Place> places( long seed ){
		ArrayList<WetlandSites.Place> out = new ArrayList<>();
		for (int sy = -R; sy <= R; sy++){
			for (int sx = -R; sx <= R; sx++){
				WorldStructures.Site t = WorldStructures.siteType( seed, sx, sy );
				if (!WetlandSites.isWetland( t )) continue;
				WetlandSites.Place p = WetlandSites.place( seed, sx, sy );
				assertNotNull( p );
				assertEquals( t, p.kind );
				out.add( p );
			}
		}
		return out;
	}

	@Test
	public void everyKindIsLaidInEveryWorld(){
		for (long seed : SEEDS){
			EnumMap<WorldStructures.Site, Integer> n = new EnumMap<>( WorldStructures.Site.class );
			for (WetlandSites.Place p : places( seed )) n.merge( p.kind, 1, Integer::sum );
			for (WorldStructures.Site k : new WorldStructures.Site[]{ WorldStructures.Site.WITCH_HUT,
					WorldStructures.Site.SUNKEN_SHRINE, WorldStructures.Site.TIDE_POOLS }){
				assertTrue( k + " in world " + seed, n.getOrDefault( k, 0 ) >= 1 );
			}
		}
	}

	@Test
	public void placesStandOnTheirGroundClearOfEverythingOlder(){
		WorldModel.Sample s = new WorldModel.Sample();
		for (long seed : SEEDS){
			for (WetlandSites.Place p : places( seed )){
				String where = p.kind + " at " + p.cx + "," + p.cy + " (world " + seed + ")";
				//inside its own sector, its grounds too
				assertEquals( where, p.sx, Math.floorDiv( p.cx - WetlandSites.REACH, WorldStructures.SECTOR ) );
				assertEquals( where, p.sx, Math.floorDiv( p.cx + WetlandSites.REACH, WorldStructures.SECTOR ) );
				assertEquals( where, p.sy, Math.floorDiv( p.cy - WetlandSites.REACH, WorldStructures.SECTOR ) );
				assertEquals( where, p.sy, Math.floorDiv( p.cy + WetlandSites.REACH, WorldStructures.SECTOR ) );
				//on its own ground, at the year's mean
				WorldModel.Biome b = WorldModel.sample( seed, p.cx, p.cy, 0f, s ).biome;
				assertEquals( where, p.kind == WorldStructures.Site.TIDE_POOLS ? WorldModel.Biome.BEACH : WorldModel.Biome.SWAMP, b );
				//clear of the town
				assertTrue( where, Math.max( Math.abs( p.cx ), Math.abs( p.cy ) ) > WetlandSites.TOWN_CLEAR );
				//no road on the grounds or a cell round them
				for (int y = p.cy - WetlandSites.REACH - 1; y <= p.cy + WetlandSites.REACH + 1; y++){
					for (int x = p.cx - WetlandSites.REACH - 1; x <= p.cx + WetlandSites.REACH + 1; x++){
						assertFalse( where + " road at " + x + "," + y, WorldStructures.onRoad( seed, x, y ) );
					}
				}
				//clear of every older site's grounds and of every village's houses, fence and fields
				for (int dy = -2; dy <= 2; dy++){
					for (int dx = -2; dx <= 2; dx++){
						WorldStructures.Site t = WorldStructures.baseType( seed, p.sx + dx, p.sy + dy );
						if (t == WorldStructures.Site.NONE) continue;
						int d = Math.max( Math.abs( WorldStructures.siteX( seed, p.sx + dx, p.sy + dy ) - p.cx ),
								Math.abs( WorldStructures.siteY( seed, p.sx + dx, p.sy + dy ) - p.cy ) );
						int reach = t == WorldStructures.Site.VILLAGE
								? WorldStructures.settlementLayout( seed, p.sx + dx, p.sy + dy )[0] + WorldStructures.FIELD_REACH + WetlandSites.REACH
								: 6 + WetlandSites.REACH;
						assertTrue( where + " by a " + t, d > reach );
						//no field of that village on its grounds
						if (t == WorldStructures.Site.VILLAGE){
							int[] plots = WorldStructures.fieldPlots( seed, p.sx + dx, p.sy + dy );
							for (int y = p.cy - WetlandSites.REACH; y <= p.cy + WetlandSites.REACH; y++){
								for (int x = p.cx - WetlandSites.REACH; x <= p.cx + WetlandSites.REACH; x++){
									assertFalse( where, WorldStructures.inField( plots, x, y ) );
								}
							}
						}
					}
				}
			}
		}
	}

	//the wetlands only ever take a sector nothing older took: every older site stays what it was
	@Test
	public void noOlderSiteChanges(){
		for (long seed : SEEDS){
			for (int sy = -R; sy <= R; sy++){
				for (int sx = -R; sx <= R; sx++){
					WorldStructures.Site base = WorldStructures.baseType( seed, sx, sy );
					WorldStructures.Site t = WorldStructures.siteType( seed, sx, sy );
					if (base != WorldStructures.Site.NONE) assertEquals( base, t );
					else {
						//the wetlands' places come first after the older sites; a sector they leave
						//empty may hold a later one (the desert's, the sea's)
						WetlandSites.Place p = WetlandSites.place( seed, sx, sy );
						if (p != null) assertEquals( p.kind, t );
						else assertFalse( WetlandSites.isWetland( t ) );
					}
					//where an older site stands, its place is its anchor
					if (!WetlandSites.isWetland( t )){
						assertEquals( WorldStructures.siteX( seed, sx, sy ), WorldStructures.placeX( seed, sx, sy ) );
						assertEquals( WorldStructures.siteY( seed, sx, sy ), WorldStructures.placeY( seed, sx, sy ) );
					}
				}
			}
		}
	}

	@Test
	public void theSamePlacesWhateverTheOrder(){
		long seed = SEEDS[0];
		ArrayList<WetlandSites.Place> first = places( seed );
		//another world in between empties every cache
		places( SEEDS[1] );
		//...and this one asked for back to front
		ArrayList<String> again = new ArrayList<>();
		for (int sy = R; sy >= -R; sy--){
			for (int sx = R; sx >= -R; sx--){
				if (!WetlandSites.isWetland( WorldStructures.siteType( seed, sx, sy ) )) continue;
				WetlandSites.Place p = WetlandSites.place( seed, sx, sy );
				again.add( p.kind + "@" + p.cx + "," + p.cy + ":" + java.util.Arrays.toString( p.pools ) + java.util.Arrays.toString( p.rocks ) );
			}
		}
		ArrayList<String> before = new ArrayList<>();
		for (WetlandSites.Place p : first){
			before.add( p.kind + "@" + p.cx + "," + p.cy + ":" + java.util.Arrays.toString( p.pools ) + java.util.Arrays.toString( p.rocks ) );
		}
		java.util.Collections.sort( before );
		java.util.Collections.sort( again );
		assertEquals( before, again );
	}

	@Test
	public void footprintsAreWhatTheGeneratorLays(){
		long seed = SEEDS[0];
		WetlandSites.Place hut = WetlandSites.nearest( seed, WorldStructures.Site.WITCH_HUT, 0, 0, 40 );
		WetlandSites.Place shrine = WetlandSites.nearest( seed, WorldStructures.Site.SUNKEN_SHRINE, 0, 0, 40 );
		WetlandSites.Place pools = WetlandSites.nearest( seed, WorldStructures.Site.TIDE_POOLS, 0, 0, 40 );
		assertNotNull( hut );
		assertNotNull( shrine );
		assertNotNull( pools );

		//the hut: a house of the villages' kind in a moat, a boardwalk from its door to the bank
		assertEquals( Terrain.EMPTY_SP, WetlandSites.terrain( hut, hut.cx, hut.cy ) );
		assertEquals( Terrain.DOOR, WetlandSites.terrain( hut, hut.cx, hut.cy + 2 ) );
		for (int dy = 3; dy < WetlandSites.WALK; dy++) assertEquals( Terrain.BRIDGE, WetlandSites.terrain( hut, hut.cx, hut.cy + dy ) );
		assertEquals( Terrain.GRASS, WetlandSites.terrain( hut, hut.cx, hut.cy + WetlandSites.WALK ) );
		for (int d = -2; d <= 2; d++){
			assertEquals( Terrain.WALL, WetlandSites.terrain( hut, hut.cx + d, hut.cy - 2 ) );
			assertEquals( Terrain.WATER, WetlandSites.terrain( hut, hut.cx + d, hut.cy - 3 ) );
		}
		int[] witch = WetlandSites.witchCell( hut ), pot = WetlandSites.cauldronCell( hut );
		assertEquals( Terrain.EMPTY_SP, WetlandSites.terrain( hut, witch[0], witch[1] ) );
		assertEquals( Terrain.EMPTY_SP, WetlandSites.terrain( hut, pot[0], pot[1] ) );
		//her cauldron out of her shelf's reach (Shopkeeper.restock lays her wares round her)
		assertTrue( Math.max( Math.abs( witch[0] - pot[0] ), Math.abs( witch[1] - pot[1] ) ) > 1 );
		assertEquals( -1, WetlandSites.terrain( hut, hut.cx + WetlandSites.REACH + 1, hut.cy ) );

		//the shrine: its idol in the flooded middle, a statue at each corner of its ring, the way in open
		int[] idol = WetlandSites.idolCell( shrine );
		assertEquals( Terrain.WATER, WetlandSites.terrain( shrine, idol[0], idol[1] ) );
		assertEquals( Terrain.STATUE, WetlandSites.terrain( shrine, shrine.cx - 2, shrine.cy - 2 ) );
		assertEquals( Terrain.STATUE, WetlandSites.terrain( shrine, shrine.cx + 2, shrine.cy + 2 ) );
		assertEquals( Terrain.WATER, WetlandSites.terrain( shrine, shrine.cx, shrine.cy + 2 ) );
		//its broken ring plain stone (the surface's dressed wall is gold ore), and the shrine's own
		int walls = 0;
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				int t = WetlandSites.terrain( shrine, shrine.cx + dx, shrine.cy + dy );
				assertTrue( t != Terrain.WALL_DECO );
				if (t != Terrain.WALL) continue;
				walls++;
				assertEquals( WorldStructures.Site.SUNKEN_SHRINE, WorldStructures.wallSite( seed, shrine.cx + dx, shrine.cy + dy ) );
			}
		}
		assertTrue( walls >= 3 );

		//the pools: three to five, water among rocks, each with a way in
		assertTrue( pools.pools.length / 2 >= WetlandSites.MIN_POOLS );
		for (int i = 0; i < pools.pools.length; i += 2){
			assertEquals( Terrain.WATER, WetlandSites.terrain( pools, pools.pools[i], pools.pools[i + 1] ) );
		}
		for (int i = 0; i < pools.rocks.length; i += 2){
			assertEquals( Terrain.BOULDER, WetlandSites.terrain( pools, pools.rocks[i], pools.rocks[i + 1] ) );
		}

		//the structure layer lays them (the world's terrain at a cell)
		assertEquals( Terrain.DOOR, WorldModel.terrainAt( seed, hut.cx, hut.cy + 2 ) );
		assertEquals( Terrain.STATUE, WorldModel.terrainAt( seed, shrine.cx - 2, shrine.cy - 2 ) );
		assertEquals( WorldStructures.Site.WITCH_HUT, WorldStructures.wallSite( seed, hut.cx - 2, hut.cy ) );
	}

	@Test
	public void theWindowLaysThemAndTheirWaterFreezes(){
		long seed = SEEDS[0];
		WetlandSites.Place hut = WetlandSites.nearest( seed, WorldStructures.Site.WITCH_HUT, 0, 0, 40 );
		assertNotNull( hut );
		int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
		int ox = hut.cx - w / 2, oy = hut.cy - h / 2;
		WindowGenerator.Window win = WindowGenerator.generate( seed, 0, ox, oy, 0f );
		assertNotNull( win.biome );
		for (int dy = -WetlandSites.REACH; dy <= WetlandSites.REACH; dy++){
			for (int dx = -WetlandSites.REACH; dx <= WetlandSites.REACH; dx++){
				int t = WetlandSites.terrain( hut, hut.cx + dx, hut.cy + dy );
				if (t == -1) continue;
				int c = (hut.cx + dx - ox) + (hut.cy + dy - oy) * w;
				int got = win.terrain[c];
				if (t == Terrain.WATER) assertTrue( got == Terrain.WATER || got == Terrain.FROZEN_WATER );
				else assertEquals( t, got );
			}
		}
		//the hut's middle is in the swamp, by the window's own biome map
		assertEquals( WorldModel.Biome.SWAMP.ordinal(), win.biome[(hut.cx - ox) + (hut.cy - oy) * w] );
		//...and the hut's water freezes with the ground in a hard winter
		WindowGenerator.Window cold = WindowGenerator.generate( seed, 0, ox, oy, -0.6f );
		int moat = (hut.cx - ox) + (hut.cy - 3 - oy) * w;
		if (cold.frozen[moat]) assertEquals( Terrain.FROZEN_WATER, cold.terrain[moat] );
		else assertEquals( Terrain.WATER, cold.terrain[moat] );
		//the same window twice is the same window
		WindowGenerator.Window again = WindowGenerator.generate( seed, 0, ox, oy, 0f );
		assertTrue( java.util.Arrays.equals( win.terrain, again.terrain ) );
		assertTrue( java.util.Arrays.equals( win.biome, again.biome ) );
		//the hut's walls are a house's (no ground laid over them); a shrine's stand in the bog
		//with moss on them, never the water
		assertEquals( -1, win.top[(hut.cx - 2 - ox) + (hut.cy - oy) * w] );
		WetlandSites.Place shrine = WetlandSites.nearest( seed, WorldStructures.Site.SUNKEN_SHRINE, 0, 0, 40 );
		assertNotNull( shrine );
		int sx = shrine.cx - w / 2, sy = shrine.cy - h / 2;
		WindowGenerator.Window bog = WindowGenerator.generate( seed, 0, sx, sy, 0f );
		int walls = 0;
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				int c = (shrine.cx + dx - sx) + (shrine.cy + dy - sy) * w;
				if (bog.terrain[c] != Terrain.WALL) continue;
				walls++;
				assertTrue( bog.top[c] != -1 );
				assertTrue( bog.topTerrain[c] != Terrain.WATER && bog.topTerrain[c] != Terrain.DEEP_WATER
						&& bog.topTerrain[c] != Terrain.FROZEN_WATER );
			}
		}
		assertTrue( walls >= 3 );
	}

	@Test
	public void theTideTurnsTwiceALunarDay(){
		float period = WetlandSites.TIDE_PERIOD;
		assertEquals( DayNightCycle.FULL_CYCLE * (24f + 50f / 60f) / 24f / 2f, period, 0.001f );
		//high water at 0, low water half a period on, high again a period on
		assertEquals( 1f, WetlandSites.tideLevel( 0 ), 0.001f );
		assertEquals( -1f, WetlandSites.tideLevel( Math.round( period / 2 ) ), 0.01f );
		assertFalse( WetlandSites.tideOut( 0 ) );
		assertTrue( WetlandSites.tideOut( Math.round( period / 2 ) ) );
		//out half the time
		int out = 0, n = 0;
		for (long t = 0; t < 20 * DayNightCycle.FULL_CYCLE; t += 7, n++) if (WetlandSites.tideOut( t )) out++;
		assertEquals( 0.5f, out / (float) n, 0.02f );
		//it turns when it says it will
		for (long t : new long[]{ 0, 100, 500, 1234, 5000, 99999 }){
			int k = WetlandSites.turnsToTurn( t );
			assertTrue( k >= 1 && k <= Math.ceil( period / 2 ) + 1 );
			assertEquals( WetlandSites.tideOut( t ), WetlandSites.tideOut( t + k - 1 ) );
			assertTrue( WetlandSites.tideOut( t ) != WetlandSites.tideOut( t + k ) );
		}
		//low water walks round the clock: a day later it comes later in the day
		long low0 = firstLow( 0 ), low1 = firstLow( DayNightCycle.FULL_CYCLE ) - DayNightCycle.FULL_CYCLE;
		assertTrue( low1 > low0 );
	}

	private static long firstLow( long from ){
		long t = from;
		while (WetlandSites.tideOut( t )) t++;
		while (!WetlandSites.tideOut( t )) t++;
		return t;
	}

	@Test
	public void thePoolsRefillToTheirCap(){
		assertEquals( WetlandSites.POOL_CAP, WetlandSites.refilled( 0, Integer.MIN_VALUE, 10 ) );
		assertEquals( 0, WetlandSites.refilled( 0, 10, 10 ) );
		assertEquals( WetlandSites.POOL_REFILL, WetlandSites.refilled( 0, 10, 11 ) );
		assertEquals( 2 * WetlandSites.POOL_REFILL + 1, WetlandSites.refilled( 1, 10, 12 ) );
		assertEquals( WetlandSites.POOL_CAP, WetlandSites.refilled( 0, 10, 40 ) );
		assertEquals( WetlandSites.POOL_CAP, WetlandSites.refilled( WetlandSites.POOL_CAP, 10, 11 ) );
		//a clock that ran back never takes any away
		assertEquals( 3, WetlandSites.refilled( 3, 10, 8 ) );
	}

	@Test
	public void theShrinesRules(){
		assertEquals( 0, WetlandSites.shrineWait( Integer.MIN_VALUE, 5 ) );
		assertEquals( WetlandSites.SHRINE_DAYS, WetlandSites.shrineWait( 5, 5 ) );
		assertEquals( 1, WetlandSites.shrineWait( 5, 5 + WetlandSites.SHRINE_DAYS - 1 ) );
		assertEquals( 0, WetlandSites.shrineWait( 5, 5 + WetlandSites.SHRINE_DAYS ) );
		assertEquals( WetlandSites.BLESS_TURNS, WetlandSites.blessTurns( WetlandSites.OFFER_WORTH ), 0.001f );
		assertEquals( WetlandSites.BLESS_MAX, WetlandSites.blessTurns( 10 * WetlandSites.OFFER_WORTH ), 0.001f );
		assertEquals( WetlandSites.BLESS_MAX, WetlandSites.blessTurns( 100000 ), 0.001f );
		assertTrue( WetlandSites.blessTurns( 75 ) > WetlandSites.BLESS_TURNS );
		for (int night = 0; night < 30; night++){
			int g = WetlandSites.guardCount( 0x1234L, night );
			assertTrue( g == 2 || g == 3 );
			assertEquals( g, WetlandSites.guardCount( 0x1234L, night ) );
		}
	}

	@Test
	public void theMistFollowsTheHourWithoutAJump(){
		DayNightCycle.Phase[] order = { DayNightCycle.Phase.DAWN, DayNightCycle.Phase.DAY, DayNightCycle.Phase.DUSK, DayNightCycle.Phase.NIGHT };
		for (int i = 0; i < order.length; i++){
			DayNightCycle.Phase p = order[i], next = order[(i + 1) % order.length];
			//each phase ends where the next begins
			assertEquals( p + " into " + next, WetlandSites.mistDensity( p, 1f ), WetlandSites.mistDensity( next, 0f ), 0.001f );
			for (float t = 0; t <= 1f; t += 0.05f){
				float d = WetlandSites.mistDensity( p, t );
				assertTrue( d > 0f && d <= 1f );
			}
		}
		//a haze by day, thick at night, thickest at dawn
		assertEquals( 1, WetlandSites.mistSight( WetlandSites.mistDensity( DayNightCycle.Phase.DAY, 0.5f ) ) );
		assertEquals( 3, WetlandSites.mistSight( WetlandSites.mistDensity( DayNightCycle.Phase.DAWN, 0.1f ) ) );
		assertTrue( WetlandSites.mistSight( WetlandSites.mistDensity( DayNightCycle.Phase.NIGHT, 0.3f ) ) >= 2 );
		assertTrue( WetlandSites.mistDensity( DayNightCycle.Phase.NIGHT, 0.5f ) >= WetlandSites.THICK );
		assertTrue( WetlandSites.mistDensity( DayNightCycle.Phase.DAWN, 0.5f ) >= WetlandSites.THICK );
		assertTrue( WetlandSites.mistDensity( DayNightCycle.Phase.DAY, 0.5f ) < WetlandSites.THICK );
		assertEquals( 0, WetlandSites.mistSight( 0f ) );
	}

	@Test
	public void nearestFindsTheClosest(){
		long seed = SEEDS[0];
		WetlandSites.Place p = WetlandSites.nearest( seed, WorldStructures.Site.TIDE_POOLS, 0, 0, 40 );
		assertNotNull( p );
		long d = (long) p.cx * p.cx + (long) p.cy * p.cy;
		for (WetlandSites.Place q : places( seed )){
			if (q.kind != WorldStructures.Site.TIDE_POOLS) continue;
			assertTrue( (long) q.cx * q.cx + (long) q.cy * q.cy >= d );
		}
		assertNull( WetlandSites.nearest( seed, WorldStructures.Site.WITCH_HUT, 0, 0, -1 ) );
		//and placesIn finds it from inside its grounds
		ArrayList<WetlandSites.Place> in = WetlandSites.placesIn( seed, p.cx, p.cy, p.cx, p.cy );
		assertEquals( 1, in.size() );
		assertTrue( in.get( 0 ).holds( p.cx + WetlandSites.REACH, p.cy - WetlandSites.REACH ) );
		assertFalse( in.get( 0 ).holds( p.cx + WetlandSites.REACH + 1, p.cy ) );
	}
}

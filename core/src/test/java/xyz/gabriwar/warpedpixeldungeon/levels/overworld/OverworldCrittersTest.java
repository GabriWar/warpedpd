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

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldCritters.Route;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldCritters.Sky;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldCritters.Species;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The surface's critters are pictures, but what may turn up where and when is a set of
 * pure rules: the weather, the hour, the season and the biome decide the kind, the ground
 * decides the cell, the caps decide how many, and a hare's dash never crosses a wall.
 */
public class OverworldCrittersTest {

	private static final int W = 30, H = 30;

	private static int[] field( int terrain ){
		int[] map = new int[W * H];
		Arrays.fill( map, terrain );
		return map;
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	@Test
	public void theWeatherKeepsThemIn(){
		assertEquals( Sky.CLEAR, OverworldCritters.skyOf( false, 1f, PrecipType.NONE, 0f ) );
		//a rate with no precipitation behind it is no rain
		assertEquals( Sky.CLEAR, OverworldCritters.skyOf( false, 1f, PrecipType.NONE, 0.9f ) );
		assertEquals( Sky.WET, OverworldCritters.skyOf( false, 0.5f, PrecipType.RAIN, 0.2f ) );
		assertEquals( Sky.FOUL, OverworldCritters.skyOf( false, 0.5f, PrecipType.RAIN, 0.5f ) );
		assertEquals( Sky.FOUL, OverworldCritters.skyOf( true, 1f, PrecipType.NONE, 0f ) );
		//the fauna's own blizzard rule (weatherFactor 0) closes the sky too
		assertEquals( Sky.FOUL, OverworldCritters.skyOf( false, 0f, PrecipType.SNOW, 0.1f ) );
		assertEquals( 1f, OverworldCritters.skyFactor( Sky.CLEAR ), 0f );
		assertEquals( 0.5f, OverworldCritters.skyFactor( Sky.WET ), 0f );
		assertEquals( 0f, OverworldCritters.skyFactor( Sky.FOUL ), 0f );
	}

	@Test
	public void eachBiomeHasItsBirdsAndHours(){
		assertEquals( Species.CROW, OverworldCritters.birdFor( Biome.MEADOW, Phase.DAY ) );
		assertEquals( Species.CROW, OverworldCritters.birdFor( Biome.PLAINS, Phase.DAWN ) );
		assertEquals( Species.CROW, OverworldCritters.birdFor( Biome.FOREST, Phase.DAY ) );
		assertNull( OverworldCritters.birdFor( Biome.MEADOW, Phase.NIGHT ) );
		assertNull( OverworldCritters.birdFor( Biome.MEADOW, Phase.DUSK ) );
		assertEquals( Species.GULL, OverworldCritters.birdFor( Biome.BEACH, Phase.DUSK ) );
		assertNull( OverworldCritters.birdFor( Biome.BEACH, Phase.NIGHT ) );
		assertEquals( Species.FINCH, OverworldCritters.birdFor( Biome.DESERT, Phase.DAWN ) );
		assertEquals( Species.FINCH, OverworldCritters.birdFor( Biome.DESERT, Phase.DUSK ) );
		assertNull( OverworldCritters.birdFor( Biome.DESERT, Phase.DAY ) );
		for (Phase p : Phase.values()){
			assertNull( OverworldCritters.birdFor( Biome.SNOWFIELD, p ) );
			assertNull( OverworldCritters.birdFor( Biome.SWAMP, p ) );
			assertNull( OverworldCritters.birdFor( Biome.OCEAN, p ) );
		}
		assertTrue( OverworldCritters.birdHour( Species.OWL, Phase.NIGHT, Sky.CLEAR ) );
		assertTrue( OverworldCritters.birdHour( Species.OWL, Phase.NIGHT, Sky.WET ) );
		assertFalse( OverworldCritters.birdHour( Species.OWL, Phase.DAY, Sky.CLEAR ) );
		assertFalse( OverworldCritters.birdHour( Species.CROW, Phase.DAY, Sky.FOUL ) );
		assertFalse( OverworldCritters.birdHour( Species.CROW, Phase.DUSK, Sky.CLEAR ) );
		assertTrue( OverworldCritters.birdHour( Species.FINCH, Phase.DUSK, Sky.WET ) );
	}

	@Test
	public void haresButterfliesAndFishKeepTheirSeasons(){
		assertTrue( OverworldCritters.hareHome( Biome.TUNDRA ) );
		assertTrue( OverworldCritters.hareHome( Biome.MEADOW ) );
		assertFalse( OverworldCritters.hareHome( Biome.DESERT ) );
		assertFalse( OverworldCritters.hareHome( Biome.SWAMP ) );
		assertTrue( OverworldCritters.hareHour( Phase.DAWN, Sky.WET ) );
		assertFalse( OverworldCritters.hareHour( Phase.NIGHT, Sky.CLEAR ) );
		assertFalse( OverworldCritters.hareHour( Phase.DAY, Sky.FOUL ) );

		assertTrue( OverworldCritters.butterflyHour( Phase.DAY, Season.SUMMER, Sky.CLEAR, 3f ) );
		assertTrue( OverworldCritters.butterflyHour( Phase.DAY, Season.SPRING, Sky.CLEAR, 0f ) );
		assertFalse( OverworldCritters.butterflyHour( Phase.DAY, Season.AUTUMN, Sky.CLEAR, 3f ) );
		assertFalse( OverworldCritters.butterflyHour( Phase.DAY, Season.SUMMER, Sky.WET, 3f ) );
		assertFalse( OverworldCritters.butterflyHour( Phase.DUSK, Season.SUMMER, Sky.CLEAR, 3f ) );
		assertFalse( OverworldCritters.butterflyHour( Phase.DAY, Season.SUMMER, Sky.CLEAR, 12f ) );
		assertTrue( OverworldCritters.butterflyGround( Biome.FOREST, Terrain.FLOWER_PATCH ) );
		assertFalse( OverworldCritters.butterflyGround( Biome.FOREST, Terrain.GRASS ) );
		assertTrue( OverworldCritters.butterflyGround( Biome.MEADOW, Terrain.HIGH_GRASS ) );
		assertFalse( OverworldCritters.butterflyGround( Biome.MEADOW, Terrain.WATER ) );

		float dawn = OverworldCritters.fishRate( Phase.DAWN, Sky.CLEAR );
		float day = OverworldCritters.fishRate( Phase.DAY, Sky.CLEAR );
		float night = OverworldCritters.fishRate( Phase.NIGHT, Sky.CLEAR );
		assertEquals( dawn, OverworldCritters.fishRate( Phase.DUSK, Sky.CLEAR ), 0f );
		assertTrue( dawn > day && day > night && night > 0f );
		assertTrue( OverworldCritters.fishRate( Phase.DAY, Sky.WET ) < day );
		assertEquals( 0f, OverworldCritters.fishRate( Phase.DAWN, Sky.FOUL ), 0f );
	}

	@Test
	public void eachKindStandsOnItsOwnGround(){
		assertTrue( OverworldCritters.groundFor( Species.CROW, Terrain.GRASS ) );
		assertTrue( OverworldCritters.groundFor( Species.CROW, Terrain.DIRT_PATH ) );
		assertFalse( OverworldCritters.groundFor( Species.CROW, Terrain.WATER ) );
		assertFalse( OverworldCritters.groundFor( Species.CROW, Terrain.HIGH_GRASS ) );
		assertFalse( OverworldCritters.groundFor( Species.CROW, Terrain.EMPTY_SP ) );
		assertTrue( OverworldCritters.groundFor( Species.GULL, Terrain.EMPTY_SP ) );
		assertFalse( OverworldCritters.groundFor( Species.FINCH, Terrain.GRASS ) );
		assertTrue( OverworldCritters.groundFor( Species.HARE, Terrain.SNOW ) );
		assertFalse( OverworldCritters.groundFor( Species.HARE, Terrain.DEEP_WATER ) );
		for (Species s : Species.values()){
			assertFalse( OverworldCritters.groundFor( s, Terrain.WALL ) );
			assertFalse( OverworldCritters.groundFor( s, Terrain.TREE_OAK ) );
		}

		int[] map = field( Terrain.GRASS );
		assertTrue( OverworldCritters.clearBelow( map, W, at( 5, 5 ) ) );
		map[at( 5, 6 )] = Terrain.TREE_OAK;
		assertFalse( OverworldCritters.clearBelow( map, W, at( 5, 5 ) ) );

		map = field( Terrain.GRASS );
		assertTrue( OverworldCritters.openAround( map, W, at( 5, 5 ) ) );
		map[at( 6, 6 )] = Terrain.WALL;
		assertFalse( OverworldCritters.openAround( map, W, at( 5, 5 ) ) );
		map[at( 6, 6 )] = Terrain.BARRICADE;
		assertFalse( OverworldCritters.openAround( map, W, at( 5, 5 ) ) );
	}

	@Test
	public void hutCentreIsNotOpen(){
		//a village hut as WorldStructures draws it: a 5x5 WALL ring, a door in the middle of
		//one side, a 3x3 EMPTY_SP floor - the floor a gull or a finch would happily stand on
		int[] map = field( Terrain.GRASS );
		for (int y = 10; y <= 14; y++){
			for (int x = 10; x <= 14; x++){
				boolean ring = x == 10 || x == 14 || y == 10 || y == 14;
				map[at( x, y )] = ring ? Terrain.WALL : Terrain.EMPTY_SP;
			}
		}
		map[at( 12, 14 )] = Terrain.DOOR;
		for (int y = 11; y <= 13; y++){
			for (int x = 11; x <= 13; x++){
				assertFalse( "inside the hut at " + x + "," + y, OverworldCritters.openAround( map, W, at( x, y ) ) );
			}
		}
		//three cells out from the ring the grass is open again, on every side
		assertTrue( OverworldCritters.openAround( map, W, at( 17, 12 ) ) );
		assertTrue( OverworldCritters.openAround( map, W, at( 12, 17 ) ) );
		assertTrue( OverworldCritters.openAround( map, W, at( 7, 12 ) ) );
		//two out is still under the eaves
		assertFalse( OverworldCritters.openAround( map, W, at( 16, 12 ) ) );
		//a statue or a sign only matters alongside
		map = field( Terrain.GRASS );
		map[at( 7, 5 )] = Terrain.STATUE;
		assertTrue( OverworldCritters.openAround( map, W, at( 5, 5 ) ) );
		map[at( 6, 5 )] = Terrain.SIGN;
		assertFalse( OverworldCritters.openAround( map, W, at( 5, 5 ) ) );
	}

	@Test
	public void critterKeepsClearOfPeople(){
		//a guard at (100, 100) and a caravaneer at (300, 40), in scene pixels
		float[] people = { 100f, 100f, 300f, 40f };
		float birds = 52f + 16f, hares = 68f + 16f;
		//a flock: a cell and more past the guard's scare reach is fine, within it is not
		assertTrue( OverworldCritters.clearOfChars( 100f + birds + 1f, 100f, people, birds ) );
		assertFalse( OverworldCritters.clearOfChars( 100f + birds, 100f, people, birds ) );
		//Chebyshev, like every reach here: a diagonal counts its longer side
		assertFalse( OverworldCritters.clearOfChars( 150f, 150f, people, birds ) );
		//a hare keeps farther off than a bird
		assertTrue( OverworldCritters.clearOfChars( 175f, 100f, people, birds ) );
		assertFalse( OverworldCritters.clearOfChars( 175f, 100f, people, hares ) );
		//every one counts, not only the first
		assertFalse( OverworldCritters.clearOfChars( 300f, 90f, people, birds ) );
		//nobody about: anywhere will do
		assertTrue( OverworldCritters.clearOfChars( 0f, 0f, new float[0], hares ) );
	}

	@Test
	public void crowsKeepToTheWoodsEdge(){
		int[] map = field( Terrain.GRASS );
		assertFalse( OverworldCritters.forestEdge( map, W, H, at( 10, 10 ) ) );
		map[at( 12, 10 )] = Terrain.TREE_PINE;
		assertTrue( OverworldCritters.forestEdge( map, W, H, at( 10, 10 ) ) );
		map[at( 11, 11 )] = Terrain.TREE_OAK;
		assertFalse( OverworldCritters.forestEdge( map, W, H, at( 10, 10 ) ) );
	}

	@Test
	public void fishLeapOnlyFromOpenWater(){
		int[] map = field( Terrain.GRASS );
		map[at( 10, 10 )] = Terrain.WATER;
		assertFalse( OverworldCritters.fishWater( map, W, H, at( 10, 10 ) ) );   //a puddle
		for (int y = 9; y <= 11; y++) for (int x = 9; x <= 11; x++) map[at( x, y )] = Terrain.DEEP_WATER;
		assertTrue( OverworldCritters.fishWater( map, W, H, at( 10, 10 ) ) );
		for (int y = 9; y <= 11; y++) for (int x = 9; x <= 11; x++) map[at( x, y )] = Terrain.FROZEN_WATER;
		assertFalse( OverworldCritters.fishWater( map, W, H, at( 10, 10 ) ) );   //ice is no water
		assertFalse( OverworldCritters.fishWater( map, W, H, at( 0, 0 ) ) );
	}

	@Test
	public void theCapsHold(){
		int[] alive = new int[OverworldCritters.CAP.length];
		alive[OverworldCritters.FLOCKS] = 1;
		assertTrue( OverworldCritters.roomFor( Species.CROW, alive ) );
		alive[OverworldCritters.FLOCKS] = 2;
		//crows, gulls and finches share the flocks' cap
		assertFalse( OverworldCritters.roomFor( Species.CROW, alive ) );
		assertFalse( OverworldCritters.roomFor( Species.GULL, alive ) );
		assertFalse( OverworldCritters.roomFor( Species.FINCH, alive ) );
		assertTrue( OverworldCritters.roomFor( Species.OWL, alive ) );
		alive[OverworldCritters.OWLS] = 1;
		assertFalse( OverworldCritters.roomFor( Species.OWL, alive ) );
		alive[OverworldCritters.HARES] = 2;
		assertFalse( OverworldCritters.roomFor( Species.HARE, alive ) );
		alive[OverworldCritters.BUTTERFLIES] = 3;
		assertTrue( OverworldCritters.roomFor( Species.BUTTERFLY, alive ) );
		alive[OverworldCritters.BUTTERFLIES] = 4;
		assertFalse( OverworldCritters.roomFor( Species.BUTTERFLY, alive ) );
		alive[OverworldCritters.FISH] = 2;
		assertFalse( OverworldCritters.roomFor( Species.FISH, alive ) );
	}

	@Test
	public void nothingTurnsUpOnHisPath(){
		assertTrue( OverworldCritters.nearSegment( 5f, 5f, 0f, 5f, 10f, 5f, 1.5f ) );
		assertTrue( OverworldCritters.nearSegment( 5f, 6.4f, 0f, 5f, 10f, 5f, 1.5f ) );
		assertFalse( OverworldCritters.nearSegment( 5f, 8f, 0f, 5f, 10f, 5f, 1.5f ) );
		//past the end of the segment
		assertFalse( OverworldCritters.nearSegment( 13f, 5f, 0f, 5f, 10f, 5f, 1.5f ) );
		//a segment of no length is a point
		assertTrue( OverworldCritters.nearSegment( 1f, 1f, 1f, 1.5f, 1f, 1.5f, 1.5f ) );
	}

	//every waypoint stands on ground a hare can cross
	private static void assertRunnable( int[] map, Route r ){
		for (int i = 0; i + 1 < r.pts.length; i += 2){
			int c = (int)Math.floor( r.pts[i] ) + (int)Math.floor( r.pts[i+1] ) * W;
			assertTrue( "waypoint on " + map[c], OverworldCritters.runnable( map[c] ) );
		}
	}

	@Test
	public void aHareRunsIntoCoverAhead(){
		int[] map = field( Terrain.GRASS );
		for (int y = 0; y < H; y++) for (int x = 16; x <= 18; x++) map[at( x, y )] = Terrain.HIGH_GRASS;
		for (int seed = 0; seed < 20; seed++){
			Route r = OverworldCritters.boltRoute( map, W, H, at( 10, 15 ), 7.5f, 15.5f, new java.util.Random( seed ) );
			assertTrue( r.cover );
			assertRunnable( map, r );
			float lx = r.pts[r.pts.length - 2], ly = r.pts[r.pts.length - 1];
			assertEquals( Terrain.HIGH_GRASS, map[(int)lx + (int)ly * W] );
			assertTrue( lx > 10.5f );
		}
	}

	@Test
	public void aHareInTheOpenRunsOutOfSight(){
		int[] map = field( Terrain.GRASS );
		for (int seed = 0; seed < 20; seed++){
			Route r = OverworldCritters.boltRoute( map, W, H, at( 10, 15 ), 12.5f, 15.5f, new java.util.Random( seed ) );
			assertFalse( r.cover );
			assertRunnable( map, r );
			float lx = r.pts[r.pts.length - 2], ly = r.pts[r.pts.length - 1];
			//away from the hero, a good way off
			assertTrue( lx < 10.5f );
			assertTrue( Math.hypot( lx - 10.5f, ly - 15.5f ) >= 6f );
		}
	}

	@Test
	public void aHareNeverRunsThroughAWall(){
		int[] map = field( Terrain.GRASS );
		for (int y = 0; y < H; y++) map[at( 14, y )] = Terrain.WALL;
		//and water is no road either
		for (int y = 0; y < H; y++) map[at( 6, y )] = Terrain.WATER;
		for (int seed = 0; seed < 20; seed++){
			Route r = OverworldCritters.boltRoute( map, W, H, at( 10, 15 ), 7.5f, 15.5f, new java.util.Random( seed ) );
			assertFalse( r.cover );
			assertRunnable( map, r );
			for (int i = 0; i < r.pts.length; i += 2) assertTrue( r.pts[i] < 14f && r.pts[i] >= 7f );
		}
		//no map (mid-rebase): straight out, nothing to aim for
		Route r = OverworldCritters.boltRoute( null, W, H, at( 10, 15 ), 7.5f, 15.5f, new java.util.Random( 1 ) );
		assertFalse( r.cover );
		assertTrue( r.pts[r.pts.length - 2] > 10.5f );
	}

	@Test
	public void aHareComesOutOfTheCoverBesideIt(){
		int[] map = field( Terrain.GRASS );
		assertEquals( -1, OverworldCritters.coverNextTo( map, W, at( 5, 5 ) ) );
		map[at( 6, 6 )] = Terrain.SHRUB;
		assertEquals( at( 6, 6 ), OverworldCritters.coverNextTo( map, W, at( 5, 5 ) ) );
	}

	@Test
	public void theOwlPerchesOnlyOnADrawnCrown(){
		assertTrue( OverworldCritters.crownDrawn( Terrain.GRASS ) );
		assertFalse( OverworldCritters.crownDrawn( Terrain.DOOR ) );
		assertFalse( OverworldCritters.crownDrawn( Terrain.SIGN ) );
	}

	@Test
	public void firefliesLightTheSummerMeadowsToo(){
		//the Field's own dice, never the dungeon's seeded ones
		java.util.Random rng = new java.util.Random( 7 );
		boolean none = false, one = false, two = false, three = false;
		for (int i = 0; i < 200; i++){
			int n = OverworldFauna.fireflyCount( Biome.SWAMP, Season.WINTER, rng );
			assertTrue( n >= 2 && n <= 3 );
			two |= n == 2;
			three |= n == 3;
			int m = OverworldFauna.fireflyCount( Biome.MEADOW, Season.SUMMER, rng );
			assertTrue( m >= 0 && m <= 1 );
			none |= m == 0;
			one |= m == 1;
			assertTrue( OverworldFauna.fireflyCount( Biome.FOREST, Season.SUMMER, rng ) <= 1 );
			assertEquals( 0, OverworldFauna.fireflyCount( Biome.MEADOW, Season.SPRING, rng ) );
			assertEquals( 0, OverworldFauna.fireflyCount( Biome.DESERT, Season.SUMMER, rng ) );
		}
		assertTrue( none && one && two && three );
	}

	@Test
	public void theSceneFindsAMeadowByARiverAndAWood(){
		long[] seeds = { 0x5EED0F7EA7L, 1L, 2L, 3L, 4L, 5L };
		int found = 0;
		WorldModel.Sample s = new WorldModel.Sample();
		for (long seed : seeds){
			int[] spot = OverworldCritters.findCritterGround( seed, 0f );
			if (spot == null) continue;
			found++;
			//a meadow whatever the weather's band adds to the calendar
			assertEquals( Biome.MEADOW, WorldModel.sample( seed, spot[0], spot[1], 0f, s ).biome );
			assertEquals( Biome.MEADOW, WorldModel.sample( seed, spot[0], spot[1], -WorldModel.CLIMATE_SHIFT, s ).biome );
			assertEquals( Biome.MEADOW, WorldModel.sample( seed, spot[0], spot[1], WorldModel.CLIMATE_SHIFT, s ).biome );
			assertEquals( -1, WorldStructures.townCell( spot[0], spot[1] ) );
			assertFalse( OverworldFauna.nearSettlement( seed, spot[0], spot[1] ) );
		}
		assertTrue( found > 0 );
		assertNotNull( OverworldCritters.findCritterGround( seeds[0], 0f ) );
	}
}

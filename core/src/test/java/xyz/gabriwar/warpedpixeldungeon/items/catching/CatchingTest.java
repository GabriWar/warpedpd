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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CritterSprite;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.CookingPot;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Hearth;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipe;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipes;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Workbench;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.MapMarks;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.IronFittings;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.IntPredicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The bug net and the fishing rod (items/catching): what bites where and when, and what else comes
 * up; how long a line waits; which waters are which (a lake against the sea, by the world alone);
 * the net's thinning by stretch of land; the run's record and a line surviving a save; a bottle's
 * cache, placed by the world seed alone on open ground in reach; the stations' recipes; the
 * fishers' prices; the strings in English and Portuguese. A line is cast, waits, bites, gets away
 * and is pulled in on a real window of the surface, derived from a seed.
 */
public class CatchingTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int depth, branch, challenges, startDay, turn;
	private DayNightCycle.Phase override;
	private float shift;
	private boolean shiftHeld;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level savedLevel;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		challenges = Dungeon.challenges;
		startDay = Dungeon.calendarStartDay;
		turn = Dungeon.cycleTurn;
		override = DayNightCycle.debugPhaseOverride;
		shift = WorldModel.seasonShift();
		shiftHeld = WorldModel.seasonShiftHeld();
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Random.pushGenerator( 0xF15CL );
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		Dungeon.cycleTurn = 100;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		HomesteadWorld.reset();
		hero = new Hero();
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Random.popGenerator();
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.challenges = challenges;
		Dungeon.calendarStartDay = startDay;
		Dungeon.cycleTurn = turn;
		DayNightCycle.debugPhaseOverride = override;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		HomesteadWorld.reset();
	}

	// ------------------------------------------------------------------ what bites

	@Test
	public void everyWaterHoldsAFishAndEveryFishAWater(){
		for (FishingWaters.Kind k : FishingWaters.Kind.values()){
			boolean any = false;
			for (FishSpecies f : FishSpecies.values()) any |= f.livesIn( k );
			assertTrue( k + " holds no fish", any );
		}
		for (FishSpecies f : FishSpecies.values()){
			boolean any = false;
			for (FishingWaters.Kind k : FishingWaters.Kind.values()) any |= f.livesIn( k );
			assertTrue( f + " lives nowhere", any );
		}
		//whatever the bait, the hour, the season and the weather, something always comes up
		for (FishingWaters.Kind k : FishingWaters.Kind.values()){
			for (Bait b : Bait.values()){
				for (DayNightCycle.Phase p : DayNightCycle.Phase.values()){
					for (GameCalendar.Season s : GameCalendar.Season.values()){
						for (VillageRoutine.Weather w : VillageRoutine.Weather.values()){
							for (float roll : new float[]{ 0f, 0.5f, 0.9999f }){
								FishingRules.Catch c = FishingRules.pick( k, b, p, s, w, 0.999f, roll );
								assertNotNull( k + " " + b + " " + p + " " + s + " " + w, c );
								assertNotNull( c.fish );
								assertTrue( c.fish.livesIn( k ) );
							}
						}
					}
				}
			}
		}
	}

	@Test
	public void theBaitTheHourTheSeasonAndTheWeatherCount(){
		DayNightCycle.Phase DAWN = DayNightCycle.Phase.DAWN, DAY = DayNightCycle.Phase.DAY,
				DUSK = DayNightCycle.Phase.DUSK, NIGHT = DayNightCycle.Phase.NIGHT;
		GameCalendar.Season SPRING = GameCalendar.Season.SPRING, SUMMER = GameCalendar.Season.SUMMER,
				AUTUMN = GameCalendar.Season.AUTUMN, WINTER = GameCalendar.Season.WINTER;
		VillageRoutine.Weather FAIR = VillageRoutine.Weather.FAIR, RAIN = VillageRoutine.Weather.RAIN,
				STORM = VillageRoutine.Weather.STORM;
		//a live frog at dusk in autumn is the pike's
		assertTrue( FishingRules.weight( FishSpecies.PIKE, FishingWaters.Kind.LAKE, Bait.LIVE, DUSK, AUTUMN, FAIR )
				> 10 * FishingRules.weight( FishSpecies.PIKE, FishingWaters.Kind.LAKE, Bait.FLY, DAY, SUMMER, FAIR ) );
		//the likeliest of each water at its own best
		assertSame( FishSpecies.TROUT, likeliest( FishingWaters.Kind.RIVER, Bait.FLY, DAWN, SPRING, RAIN ) );
		assertSame( FishSpecies.PERCH, likeliest( FishingWaters.Kind.LAKE, Bait.WORM, DAY, SUMMER, FAIR ) );
		assertSame( FishSpecies.PIKE, likeliest( FishingWaters.Kind.LAKE, Bait.LIVE, DUSK, AUTUMN, FAIR ) );
		assertSame( FishSpecies.CATFISH, likeliest( FishingWaters.Kind.SWAMP, Bait.WORM, NIGHT, SUMMER, RAIN ) );
		assertSame( FishSpecies.CHAR, likeliest( FishingWaters.Kind.ICE, Bait.GLOW, DAY, WINTER, FAIR ) );
		assertSame( FishSpecies.TROUT, likeliest( FishingWaters.Kind.TARN, Bait.FLY, DAWN, SUMMER, FAIR ) );
		//the sea holds the mackerel alone
		for (FishSpecies f : FishSpecies.values()){
			assertEquals( f == FishSpecies.MACKEREL, FishingRules.weight( f, FishingWaters.Kind.SEA, Bait.GRUB, DAY, SUMMER, FAIR ) > 0f );
		}
		//a storm keeps the fish down, the night the trout
		assertTrue( FishingRules.keenness( FishingWaters.Kind.RIVER, Bait.FLY, DAY, SPRING, STORM )
				< FishingRules.keenness( FishingWaters.Kind.RIVER, Bait.FLY, DAY, SPRING, FAIR ) );
		assertTrue( FishingRules.weight( FishSpecies.TROUT, FishingWaters.Kind.RIVER, Bait.FLY, NIGHT, SPRING, FAIR )
				< FishingRules.weight( FishSpecies.TROUT, FishingWaters.Kind.RIVER, Bait.FLY, DAWN, SPRING, FAIR ) );
	}

	@Test
	public void theCavesKnowNoHourNoSeasonNoWeather(){
		float w = FishingRules.weight( FishSpecies.CAVEFISH, FishingWaters.Kind.CAVE, Bait.GLOW,
				DayNightCycle.Phase.DAY, GameCalendar.Season.SUMMER, VillageRoutine.Weather.FAIR );
		assertTrue( w > 0f );
		for (DayNightCycle.Phase p : DayNightCycle.Phase.values()){
			for (GameCalendar.Season s : GameCalendar.Season.values()){
				for (VillageRoutine.Weather sky : VillageRoutine.Weather.values()){
					assertEquals( w, FishingRules.weight( FishSpecies.CAVEFISH, FishingWaters.Kind.CAVE, Bait.GLOW, p, s, sky ), 1e-6f );
				}
			}
		}
		assertSame( FishSpecies.CAVEFISH, likeliest( FishingWaters.Kind.CAVE, Bait.GLOW,
				DayNightCycle.Phase.NIGHT, GameCalendar.Season.WINTER, VillageRoutine.Weather.STORM ) );
		//a glowing bait draws the blind fish best of all
		for (Bait b : Bait.values()){
			assertTrue( b.toString(), FishingRules.weight( FishSpecies.CAVEFISH, FishingWaters.Kind.CAVE, Bait.GLOW,
					DayNightCycle.Phase.DAY, GameCalendar.Season.SUMMER, VillageRoutine.Weather.FAIR )
					>= FishingRules.weight( FishSpecies.CAVEFISH, FishingWaters.Kind.CAVE, b,
					DayNightCycle.Phase.DAY, GameCalendar.Season.SUMMER, VillageRoutine.Weather.FAIR ) );
		}
	}

	@Test
	public void whatComesUpIsDrawnByWeight(){
		FishingWaters.Kind lake = FishingWaters.Kind.LAKE;
		DayNightCycle.Phase p = DayNightCycle.Phase.DUSK;
		GameCalendar.Season s = GameCalendar.Season.AUTUMN;
		VillageRoutine.Weather sky = VillageRoutine.Weather.RAIN;
		float[] w = FishingRules.weights( lake, Bait.WORM, p, s, sky );
		float sum = 0f;
		for (float x : w) sum += x;
		int n = 4000;
		int[] got = new int[w.length];
		for (int i = 0; i < n; i++){
			FishingRules.Catch c = FishingRules.pick( lake, Bait.WORM, p, s, sky, 0.999f, (i + 0.5f) / n );
			got[c.fish.ordinal()]++;
		}
		for (FishSpecies f : FishSpecies.values()){
			assertEquals( f.toString(), w[f.ordinal()] / sum * n, got[f.ordinal()], 2f );
		}
	}

	@Test
	public void junkComesUpRarelyAndWhereItShould(){
		for (FishingWaters.Kind k : FishingWaters.Kind.values()){
			float total = 0f;
			for (FishingRules.Junk j : FishingRules.Junk.values()) total += FishingRules.junkChance( j, k );
			assertTrue( k + ": " + total, total > 0f && total < 0.08f );
			//the first slice of the junk roll is the boot's, wherever one is lost
			assertSame( FishingRules.junkChance( FishingRules.Junk.BOOT, k ) > 0f ? FishingRules.Junk.BOOT : FishingRules.Junk.PURSE,
					FishingRules.pick( k, Bait.WORM, DayNightCycle.Phase.DAY, GameCalendar.Season.SUMMER, VillageRoutine.Weather.FAIR, 0f, 0.5f ).junk );
		}
		//bottles drift out of the sea and down the rivers, never into the caves, the ice or the peaks
		for (FishingWaters.Kind k : new FishingWaters.Kind[]{ FishingWaters.Kind.CAVE, FishingWaters.Kind.ICE,
				FishingWaters.Kind.TARN, FishingWaters.Kind.SWAMP }){
			assertEquals( 0f, FishingRules.junkChance( FishingRules.Junk.BOTTLE, k ), 0f );
		}
		assertTrue( FishingRules.junkChance( FishingRules.Junk.BOTTLE, FishingWaters.Kind.SEA )
				> FishingRules.junkChance( FishingRules.Junk.BOTTLE, FishingWaters.Kind.RIVER ) );
		float boot = FishingRules.junkChance( FishingRules.Junk.BOOT, FishingWaters.Kind.SEA );
		assertSame( FishingRules.Junk.BOTTLE, FishingRules.pick( FishingWaters.Kind.SEA, Bait.WORM, DayNightCycle.Phase.DAY,
				GameCalendar.Season.SUMMER, VillageRoutine.Weather.FAIR, boot + 0.001f, 0.5f ).junk );
		//no boot is ever lost down in the caves
		assertEquals( 0f, FishingRules.junkChance( FishingRules.Junk.BOOT, FishingWaters.Kind.CAVE ), 0f );
	}

	@Test
	public void keenerFishBiteSooner(){
		float[] keen = { 0.5f, 2f, 8f, FishingRules.KEEN, 60f, 200f, 2000f };
		for (float roll : new float[]{ 0f, 0.3f, 0.6f, 0.999f }){
			int last = Integer.MAX_VALUE;
			for (float k : keen){
				int t = FishingRules.waitTurns( k, roll );
				assertTrue( t >= FishingRules.WAIT_FLOOR && t <= FishingRules.WAIT_CAP );
				assertTrue( k + " at " + roll, t <= last );
				last = t;
			}
			int ordinary = FishingRules.waitTurns( FishingRules.KEEN, roll );
			assertTrue( ordinary >= FishingRules.WAIT_MIN && ordinary <= FishingRules.WAIT_MAX );
		}
		assertEquals( FishingRules.WAIT_CAP, FishingRules.waitTurns( 0f, 0.5f ) );
		assertEquals( FishingRules.WAIT_FLOOR, FishingRules.waitTurns( 1e6f, 0.999f ) );
	}

	// ------------------------------------------------------------------ the waters

	@Test
	public void theWatersAreKnownByTheirSliceAndBiome(){
		assertSame( FishingWaters.Kind.CAVE, FishingWaters.kindOf( SEED, -3, Terrain.WATER, null, 0, 0 ) );
		assertSame( FishingWaters.Kind.TARN, FishingWaters.kindOf( SEED, 4, Terrain.WATER, null, 0, 0 ) );
		assertSame( FishingWaters.Kind.ICE, FishingWaters.kindOf( SEED, 0, Terrain.FROZEN_WATER, WorldModel.Biome.RIVER, 0, 0 ) );
		assertSame( FishingWaters.Kind.ICE, FishingWaters.kindOf( SEED, -2, Terrain.FROZEN_WATER, null, 0, 0 ) );
		assertSame( FishingWaters.Kind.RIVER, FishingWaters.kindOf( SEED, 0, Terrain.WATER, WorldModel.Biome.RIVER, 0, 0 ) );
		assertSame( FishingWaters.Kind.RIVER, FishingWaters.kindOf( SEED, 0, Terrain.DEEP_WATER, WorldModel.Biome.RIVER, 0, 0 ) );
		assertSame( FishingWaters.Kind.SWAMP, FishingWaters.kindOf( SEED, 0, Terrain.WATER, WorldModel.Biome.SWAMP, 0, 0 ) );
		assertSame( FishingWaters.Kind.LAKE, FishingWaters.kindOf( SEED, 0, Terrain.WATER, WorldModel.Biome.MEADOW, 0, 0 ) );
		assertNull( FishingWaters.kindOf( SEED, 0, Terrain.GRASS, WorldModel.Biome.MEADOW, 0, 0 ) );
		assertNull( FishingWaters.kindOf( SEED, -1, Terrain.EMPTY, null, 0, 0 ) );
	}

	@Test
	public void theOpenSeaIsNoLakeAndAnEnclosedWaterIs(){
		WorldModel.Sample s = new WorldModel.Sample();
		int[] sea = null, lake = null;
		for (long seed : new long[]{ SEED, 42L, 12345L }){
			sea = null;
			lake = null;
			for (int r = 0; r <= 24 && (sea == null || lake == null); r++){
				for (int j = -r; j <= r; j++){
					for (int i = -r; i <= r; i++){
						if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
						int x = i * 150, y = j * 150;
						if (sea == null && openSea( seed, x, y, s )) sea = new int[]{ x, y };
						if (lake == null){
							for (int dy = 0; dy < 150 && lake == null; dy += 10){
								for (int dx = 0; dx < 150 && lake == null; dx += 10){
									if (enclosed( seed, x + dx, y + dy, s )) lake = new int[]{ x + dx, y + dy };
								}
							}
						}
					}
				}
			}
			if (sea != null && lake != null){
				assertFalse( "the open sea at " + Arrays.toString( sea ) + " is no lake", FishingWaters.lake( seed, sea[0], sea[1] ) );
				assertTrue( "water ringed by land at " + Arrays.toString( lake ) + " is a lake", FishingWaters.lake( seed, lake[0], lake[1] ) );
				//the same answer every time
				assertFalse( FishingWaters.lake( seed, sea[0], sea[1] ) );
				assertTrue( FishingWaters.lake( seed, lake[0], lake[1] ) );
				return;
			}
		}
		throw new AssertionError( "no open sea and no enclosed water near the origin of three worlds" );
	}

	//the sea's biome all over a box of 600 cells round the point (the open sea, far from any shore)
	private static boolean openSea( long seed, int x, int y, WorldModel.Sample s ){
		for (int dy = -300; dy <= 300; dy += 60){
			for (int dx = -300; dx <= 300; dx += 60){
				if (WorldModel.sample( seed, x + dx, y + dy, 0f, s ).biome != WorldModel.Biome.OCEAN) return false;
			}
		}
		return true;
	}

	//the sea's biome on the point, and none of it on a ring 40 cells round it: water closed in by land
	private static boolean enclosed( long seed, int x, int y, WorldModel.Sample s ){
		if (WorldModel.sample( seed, x, y, 0f, s ).biome != WorldModel.Biome.OCEAN) return false;
		for (int d = -40; d <= 40; d += 2){
			for (int[] p : new int[][]{ { x + d, y - 40 }, { x + d, y + 40 }, { x - 40, y + d }, { x + 40, y + d } }){
				if (WorldModel.sample( seed, p[0], p[1], 0f, s ).biome == WorldModel.Biome.OCEAN) return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ the net

	@Test
	public void theNetThinsOutInAStretchOfLand(){
		float c = 0.8f;
		for (int n = 0; n < CritterCatch.FREE; n++) assertEquals( c, CritterCatch.thinned( c, n ), 1e-6f );
		assertEquals( c * 0.75f, CritterCatch.thinned( c, 3 ), 1e-6f );
		assertEquals( c * 0.5f, CritterCatch.thinned( c, 4 ), 1e-6f );
		assertEquals( c * 0.25f, CritterCatch.thinned( c, 5 ), 1e-6f );
		for (int n = CritterCatch.CAP; n < CritterCatch.CAP + 5; n++) assertEquals( 0f, CritterCatch.thinned( c, n ), 0f );
	}

	@Test
	public void stretchesOfLandAreKeptApart(){
		HashSet<Long> keys = new HashSet<>();
		int[][] areas = { { 97, 0, 0, 0 }, { 97, 0, 1, 0 }, { 97, 0, 0, 1 }, { 97, 0, -1, -1 }, { 96, 0, 0, 0 },
				{ 101, 0, 0, 0 }, { 5, 0, 0, 0 }, { 5, 1, 0, 0 }, { 6, 0, 0, 0 } };
		for (int[] a : areas) assertTrue( Arrays.toString( a ), keys.add( CritterCatch.area( a[0], a[1], a[2], a[3] ) ) );
		//on a window of the surface: cells of one 64-cell square share a stretch, the next square is another
		OverworldLevel ow = windowAt( 0, 400 );
		int w = ow.width();
		int base = ((64 - Math.floorMod( ow.worldX(), 64 )) % 64 + 2) + (H / 2) * w;
		long here = CritterCatch.areaKey( ow, base );
		assertEquals( here, CritterCatch.areaKey( ow, base + 30 ) );
		assertNotEquals( here, CritterCatch.areaKey( ow, base + 64 ) );
	}

	@Test
	public void aButterflyIsNamedByItsColour(){
		HashSet<Critter> seen = new HashSet<>();
		for (int i = 0; i < CritterSprite.Butterfly.COLOURS.length; i++){
			Critter c = Critter.butterfly( i );
			assertEquals( i, c.butterflyColour() );
			assertTrue( seen.add( c ) );
			assertSame( Critter.Jar.WINGS, c.jar );
		}
		assertEquals( -1, Critter.BEETLE.butterflyColour() );
		assertSame( Critter.butterfly( 0 ), Critter.butterfly( -5 ) );
	}

	@Test
	public void aCritterIsOnACellWithinItsSlack(){
		int w = 50, cell = 7 + 9 * w;
		float x0 = 7 * 16, y0 = 9 * 16;
		assertTrue( CritterCatch.on( cell, w, x0 + 8, y0 + 8 ) );
		assertTrue( CritterCatch.on( cell, w, x0 - CritterCatch.SLACK, y0 + 8 ) );
		assertFalse( CritterCatch.on( cell, w, x0 - CritterCatch.SLACK - 0.5f, y0 + 8 ) );
		assertTrue( CritterCatch.on( cell, w, x0 + 16 + CritterCatch.SLACK - 0.5f, y0 + 15 ) );
		assertFalse( CritterCatch.on( cell, w, x0 + 8, y0 + 16 + CritterCatch.SLACK ) );
	}

	// ------------------------------------------------------------------ the record

	@Test
	public void theCatchWorldSurvivesASaveAndForgetsYesterday(){
		assertNotNull( "the catching keeps its record in the run's", CatchWorld.get() );
		CatchWorld world = new CatchWorld();
		long a = CritterCatch.area( 97, 0, 2, 3 ), b = CritterCatch.area( 97, 0, 2, 4 );
		world.netted( a, 5 );
		world.netted( a, 5 );
		world.netted( b, 5 );
		assertEquals( 2, world.nettedOn( a, 5 ) );
		assertEquals( 1, world.nettedOn( b, 5 ) );
		assertEquals( 0, world.nettedOn( a, 6 ) );
		world.addCache( 10, 20 );
		world.addCache( 30, -40 );
		world.addCache( 10, 20 );
		assertEquals( 2, world.caches().size() );
		assertEquals( 0, world.readNote() );
		assertEquals( 1, world.readNote() );

		Bundle bundle = new Bundle();
		world.storeInBundle( bundle );
		CatchWorld back = new CatchWorld();
		back.restoreFromBundle( bundle );
		assertEquals( 2, back.nettedOn( a, 5 ) );
		assertEquals( 1, back.nettedOn( b, 5 ) );
		assertEquals( 2, back.caches().size() );
		assertArrayEquals( new int[]{ 30, -40 }, back.cacheNear( 31, -39 ) );
		assertNull( back.cacheNear( 32, -40 ) );
		assertEquals( 2, back.readNote() );

		//a new day's catch: the old days are forgotten
		back.netted( a, 6 );
		assertEquals( 1, back.nettedOn( a, 6 ) );
		assertEquals( 0, back.nettedOn( b, 5 ) );
		assertTrue( back.removeCache( 10, 20 ) );
		assertFalse( back.removeCache( 10, 20 ) );
		assertEquals( 1, back.caches().size() );
	}

	private static void assertArrayEquals( int[] want, int[] got ){
		assertNotNull( got );
		assertEquals( Arrays.toString( want ), Arrays.toString( got ) );
	}

	@Test
	public void anOldSaveHasNoCatchRecord(){
		CatchWorld world = new CatchWorld();
		world.restoreFromBundle( new Bundle() );
		assertEquals( 0, world.nettedOn( CritterCatch.area( 97, 0, 0, 0 ), 1 ) );
		assertTrue( world.caches().isEmpty() );
		assertEquals( 0, world.readNote() );
	}

	@Test
	public void theMapMarksTheCachesOnTheSurface(){
		CatchWorld world = CatchWorld.get();
		world.addCache( 120, -60 );
		List<MapMarks.Mark> marks = MapMarks.all( WorldLayers.SURFACE_DEPTH );
		MapMarks.Mark mine = null;
		for (MapMarks.Mark m : marks) if (m.wx == 120 && m.wy == -60) mine = m;
		assertNotNull( mine );
		assertEquals( BottleMarks.COLOR, mine.color );
		assertEquals( 1f, mine.alpha, 0f );
		assertFalse( mine.name.contains( Messages.NO_TEXT_FOUND ) );
		//seen from a cave slice it is faded and says it is on the surface
		for (MapMarks.Mark m : MapMarks.all( WorldLayers.depthOf( -3 ) )){
			if (m.wx == 120 && m.wy == -60){
				assertTrue( m.alpha < 1f );
				assertNotEquals( mine.name, m.name );
			}
		}
	}

	// ------------------------------------------------------------------ the bottles

	@Test
	public void aNotesCacheIsPureAndInReachOnOpenGround(){
		WorldModel.Sample s = new WorldModel.Sample();
		int[] from = landNear( SEED, 900, 600 );
		ArrayList<String> spots = new ArrayList<>();
		for (int note = 0; note < 6; note++){
			int[] spot = BottleCaches.place( SEED, from[0], from[1], note );
			assertNotNull( "note " + note + " from " + Arrays.toString( from ), spot );
			assertEquals( Arrays.toString( spot ), Arrays.toString( BottleCaches.place( SEED, from[0], from[1], note ) ) );
			double d = Math.hypot( spot[0] - from[0], spot[1] - from[1] );
			assertTrue( "at " + d, d >= BottleCaches.NEAR - 1 && d <= BottleCaches.FAR + 1 );
			assertTrue( BottleCaches.diggable( SEED, spot[0], spot[1], s ) );
			assertEquals( -1, WorldStructures.townCell( spot[0], spot[1] ) );
			assertFalse( OverworldFauna.nearSettlement( SEED, spot[0], spot[1] ) );
			assertEquals( 0f, WorldModel.townInfluence( SEED, spot[0], spot[1], WorldModel.TOWN_LAND ), 0f );
			spots.add( Arrays.toString( spot ) );
		}
		assertTrue( "the notes lead apart", new HashSet<>( spots ).size() > 3 );
		//nothing is buried in the town
		assertFalse( BottleCaches.diggable( SEED, 0, 0, s ) );
	}

	//a meadow, plain or wood near (x, y), out of the town's land
	private static int[] landNear( long seed, int x, int y ){
		WorldModel.Sample s = new WorldModel.Sample();
		for (int r = 0; r <= 40; r++){
			for (int j = -r; j <= r; j++){
				for (int i = -r; i <= r; i++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int px = x + i * 64, py = y + j * 64;
					WorldModel.Biome b = WorldModel.sample( seed, px, py, 0f, s ).biome;
					if ((b == WorldModel.Biome.MEADOW || b == WorldModel.Biome.PLAINS || b == WorldModel.Biome.FOREST)
							&& WorldModel.townInfluence( seed, px, py, WorldModel.TOWN_LAND ) == 0f) return new int[]{ px, py };
				}
			}
		}
		throw new AssertionError( "no open land near " + x + "," + y );
	}

	// ------------------------------------------------------------------ the items

	@Test
	public void caughtThingsKeepWhatTheyAreThroughASave(){
		CritterJar jar = (CritterJar) new CritterJar( Critter.FIREFLY ).quantity( 3 );
		CritterJar back = (CritterJar) roundTrip( jar );
		assertSame( Critter.FIREFLY, back.critter );
		assertEquals( 3, back.quantity() );
		assertEquals( ItemSpriteSheet.JAR_WINGS, back.image() );
		assertNotNull( "a firefly's jar glows", back.glowing() );
		assertNull( new CritterJar( Critter.BEETLE ).glowing() );
		assertTrue( jar.isSimilar( back ) );
		assertFalse( jar.isSimilar( new CritterJar( Critter.MOTH ) ) );

		CaughtFish fish = new CaughtFish( FishSpecies.CHAR );
		CaughtFish f = (CaughtFish) roundTrip( fish );
		assertSame( FishSpecies.CHAR, f.species );
		assertEquals( ItemSpriteSheet.FISH_CHAR, f.image() );
		assertEquals( FishSpecies.Size.MEDIUM.raw, f.energy, 0f );
		assertFalse( fish.isSimilar( new CaughtFish( FishSpecies.PIKE ) ) );

		CaughtFish two = (CaughtFish) roundTrip( new CaughtFish( FishSpecies.PIKE ).quantity( 2 ) );
		assertEquals( 2, two.quantity() );
		CookedFish cooked = two.cooked( true );
		assertEquals( 2, cooked.quantity() );
		CookedFish c = (CookedFish) roundTrip( cooked );
		assertSame( FishSpecies.PIKE, c.species );
		assertTrue( c.poached );
		assertEquals( FishSpecies.Size.LARGE.cooked, c.energy, 0f );
		assertFalse( c.isSimilar( new CookedFish( FishSpecies.PIKE, false ) ) );
		assertTrue( "cooked feeds more than raw", FishSpecies.Size.SMALL.cooked > FishSpecies.Size.LARGE.raw );

		//a rod remembers the bait chosen
		FishingRod rod = new FishingRod();
		setField( rod, "preferred", "jar_FROG" );
		FishingRod r = (FishingRod) roundTrip( rod );
		assertEquals( "jar_FROG", getField( r, "preferred" ) );
		assertNull( getField( roundTrip( new FishingRod() ), "preferred" ) );

		//a name a later version no longer knows comes back as the plainest
		Bundle odd = new Bundle();
		new CritterJar( Critter.SNAIL ).storeInBundle( odd );
		odd.put( "critter", "WYVERN" );
		CritterJar unknown = new CritterJar();
		unknown.restoreFromBundle( odd );
		assertSame( Critter.BEETLE, unknown.critter );
	}

	private static Item roundTrip( Item item ){
		Bundle b = new Bundle();
		b.put( "item", item );
		return (Item) b.get( "item" );
	}

	private static void setField( Object o, String name, Object value ){
		try {
			Field f = o.getClass().getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, value );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static Object getField( Object o, String name ){
		try {
			Field f = o.getClass().getDeclaredField( name );
			f.setAccessible( true );
			return f.get( o );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void theStationsMakeNetsRodsAndCookedFish(){
		StationRecipe net = StationRecipes.byId( "bug_net" ), rod = StationRecipes.byId( "fishing_rod" );
		assertSame( Workbench.class, net.station );
		assertSame( Workbench.class, rod.station );
		assertTrue( net.produce( Collections.<Item>emptyList() ).get( 0 ) instanceof BugNet );
		assertTrue( rod.produce( Collections.<Item>emptyList() ).get( 0 ) instanceof FishingRod );
		assertTrue( takes( net, new Plank() ) && takes( net, new Rope() ) );
		assertTrue( takes( rod, new Plank() ) && takes( rod, new Rope() ) && takes( rod, new IronFittings() ) );

		StationRecipe grill = StationRecipes.byId( "grilled_fish" ), poach = StationRecipes.byId( "poached_fish" );
		assertSame( Hearth.class, grill.station );
		assertTrue( grill.needsHeat );
		assertSame( CookingPot.class, poach.station );
		for (FishSpecies sp : FishSpecies.values()){
			List<Item> g = grill.produce( Collections.<Item>singletonList( new CaughtFish( sp ) ) );
			assertEquals( 1, g.size() );
			assertSame( sp, ((CookedFish) g.get( 0 )).species );
			assertFalse( ((CookedFish) g.get( 0 )).poached );
			List<Item> p = poach.produce( Collections.<Item>singletonList( new CaughtFish( sp ) ) );
			assertTrue( ((CookedFish) p.get( 0 )).poached );
		}
		for (StationRecipe r : new StationRecipe[]{ net, rod, grill, poach }){
			assertFalse( r.id, r.name().contains( Messages.NO_TEXT_FOUND ) );
			for (StationRecipe.Input in : r.inputs) assertFalse( r.id, in.label().contains( Messages.NO_TEXT_FOUND ) );
		}
	}

	private static boolean takes( StationRecipe r, Item item ){
		for (StationRecipe.Input in : r.inputs) if (in.matches( item, null )) return true;
		return false;
	}

	@Test
	public void theSpadeTurnsUpWorms(){
		float chance = ClimateManager.isRaining() ? Earthworm.RAIN_CHANCE : Earthworm.CHANCE;
		int found = 0, trials = 3000;
		for (int i = 0; i < trials; i++){
			Earthworm w = Earthworm.turnedUp();
			if (w == null) continue;
			found++;
			assertTrue( w.quantity() >= Earthworm.MIN && w.quantity() <= Earthworm.MAX );
		}
		assertEquals( chance, found / (float) trials, 0.03f );
		assertSame( Bait.WORM, Catching.baitOf( new Earthworm() ) );
		assertSame( Bait.LIVE, Catching.baitOf( new CritterJar( Critter.FROG ) ) );
		assertNull( Catching.baitOf( new CaughtFish() ) );
	}

	@Test
	public void fishersPayHalfAgainForRawFish(){
		hero.belongings.backpack.items.add( new CaughtFish( FishSpecies.PIKE ).quantity( 2 ) );
		hero.belongings.backpack.items.add( new CaughtFish( FishSpecies.PERCH ) );
		hero.belongings.backpack.items.add( new CookedFish( FishSpecies.TROUT, false ) );
		assertEquals( 3, FisherTrade.fishCount( hero ) );
		assertEquals( Math.round( 2 * 18 * 1.5f ) + Math.round( 6 * 1.5f ), FisherTrade.fishOffer( hero ) );
		int deepest = xyz.gabriwar.warpedpixeldungeon.Statistics.deepestFloor;
		try {
			xyz.gabriwar.warpedpixeldungeon.Statistics.deepestFloor = 0;
			assertEquals( FisherTrade.ROD, FisherTrade.rodPrice() );
			assertEquals( FisherTrade.WORMS, FisherTrade.wormsPrice() );
			xyz.gabriwar.warpedpixeldungeon.Statistics.deepestFloor = 10;
			assertEquals( 3 * FisherTrade.ROD, FisherTrade.rodPrice() );
		} finally {
			xyz.gabriwar.warpedpixeldungeon.Statistics.deepestFloor = deepest;
		}
	}

	// ------------------------------------------------------------------ words

	@Test
	public void everyCritterAndFishHasItsWords(){
		for (Critter c : Critter.values()){
			String key = c.name().toLowerCase( Locale.ENGLISH );
			assertWords( CritterJar.class, key );
			assertWords( CritterJar.class, key + "_desc" );
			assertWords( CritterJar.class, "bait_" + c.bait.name().toLowerCase( Locale.ENGLISH ) );
			assertFalse( new CritterJar( c ).info().contains( Messages.NO_TEXT_FOUND ) );
		}
		for (FishSpecies f : FishSpecies.values()){
			assertWords( CaughtFish.class, f.name().toLowerCase( Locale.ENGLISH ) );
			assertWords( CaughtFish.class, f.name().toLowerCase( Locale.ENGLISH ) + "_desc" );
			assertFalse( new CaughtFish( f ).info().contains( Messages.NO_TEXT_FOUND ) );
			assertFalse( new CookedFish( f, true ).info().contains( Messages.NO_TEXT_FOUND ) );
			assertFalse( new CookedFish( f, false ).name().contains( Messages.NO_TEXT_FOUND ) );
		}
		for (FishingWaters.Kind k : FishingWaters.Kind.values()) assertWords( FishingRod.class, "water_" + k.name().toLowerCase( Locale.ENGLISH ) );
		for (int i = 0; i < MessageBottle.VOICES; i++) assertWords( MessageBottle.class, "note_" + i );
		for (String dir : new String[]{ "north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest" }){
			assertWords( MessageBottle.class, dir );
		}
		assertEquals( "north", MessageBottle.compass( 0, -10 ) );
		assertEquals( "east", MessageBottle.compass( 10, 0 ) );
		assertEquals( "southwest", MessageBottle.compass( -10, 10 ) );
		assertEquals( 80, MessageBottle.paces( 0, 0, 60, 53 ) );
		for (Item i : new Item[]{ new BugNet(), new FishingRod(), new Earthworm(), new OldBoot(), new MessageBottle() }){
			assertFalse( i.getClass().getSimpleName(), i.info().contains( Messages.NO_TEXT_FOUND ) );
			assertFalse( i.getClass().getSimpleName(), i.name().contains( Messages.NO_TEXT_FOUND ) );
		}
	}

	private static void assertWords( Class<?> cls, String key ){
		assertFalse( cls.getSimpleName() + "." + key, Messages.get( cls, key ).equals( Messages.NO_TEXT_FOUND ) );
	}

	@Test
	public void theBundlesMatchAndUseNoDashes() throws IOException {
		Properties en = load( "src/main/assets/messages/catching/catching.properties" );
		Properties pt = load( "src/main/assets/messages/catching/catching_pt.properties" );
		assertEquals( en.stringPropertyNames(), pt.stringPropertyNames() );
		Properties guideEn = load( "src/main/assets/messages/guide/guide.properties" );
		Properties guidePt = load( "src/main/assets/messages/guide/guide_pt.properties" );
		ArrayList<String> words = new ArrayList<>();
		for (Properties p : new Properties[]{ en, pt }) for (String k : p.stringPropertyNames()) words.add( p.getProperty( k ) );
		for (String page : new String[]{ "hs_catching", "hs_fishing", "hs_fish" }){
			for (Properties g : new Properties[]{ guideEn, guidePt }){
				for (String k : new String[]{ "guide." + page, "guide.title." + page, "guide.sub." + page }){
					assertNotNull( k, g.getProperty( k ) );
					words.add( g.getProperty( k ) );
				}
			}
		}
		for (String w : words){
			assertFalse( w, w.contains( "—" ) || w.contains( "–" ) );
		}
	}

	private static Properties load( String path ) throws IOException {
		Properties p = new Properties();
		try (InputStreamReader r = new InputStreamReader( new FileInputStream( path ), StandardCharsets.UTF_8 )){
			p.load( r );
		}
		return p;
	}

	// ------------------------------------------------------------------ a line on the surface

	@Test
	public void aLineBitesThenTheFishGetsAway(){
		int[] spot = riverBank();
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		FishingWaters.Kind kind = FishingWaters.kindAt( ow, spot[1] );
		assertSame( FishingWaters.Kind.RIVER, kind );
		FishingLine line = FishingLine.cast( hero, ow, spot[1], kind, Bait.FLY );
		assertSame( line, hero.buff( FishingLine.class ) );
		assertEquals( spot[1] % W - spot[0] % W, line.dx() );
		assertEquals( spot[1] / W - spot[0] / W, line.dy() );
		assertFalse( line.ice() );
		assertTrue( line.fresh );

		int turns = 0;
		while (!(line.state == FishingLine.State.BITE) && turns < 40){
			line.act();
			turns++;
			if (!(line.state == FishingLine.State.BITE) && hero.buff( FishingLine.class ) == null) throw new AssertionError( "the line came in before a bite" );
		}
		assertTrue( "a bite within the longest wait", (line.state == FishingLine.State.BITE) && turns <= FishingRules.WAIT_CAP );
		assertSame( FishingLine.State.BITE, line.state );
		//a bite holds BITE_TURNS: reel within them, or it is gone with the bait
		for (int i = 1; i < FishingRules.BITE_TURNS; i++){
			line.act();
			assertTrue( (line.state == FishingLine.State.BITE) );
		}
		line.act();
		assertNull( hero.buff( FishingLine.class ) );
		assertSame( FishingLine.End.ESCAPED, line.end );
	}

	@Test
	public void aLineIsPulledInWhenTheHeroSteps(){
		int[] spot = riverBank();
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		FishingLine line = FishingLine.cast( hero, ow, spot[1], FishingWaters.Kind.RIVER, Bait.WORM );
		line.act();
		hero.pos = spot[0] + (spot[0] % W > 2 ? -1 : 1);
		line.act();
		assertNull( hero.buff( FishingLine.class ) );
		assertSame( FishingLine.End.PULLED, line.end );

		//and when the water under the bobber turns to ice
		hero.pos = spot[0];
		line = FishingLine.cast( hero, ow, spot[1], FishingWaters.Kind.RIVER, Bait.WORM );
		ow.map[spot[1]] = Terrain.FROZEN_WATER;
		line.act();
		assertSame( FishingLine.End.PULLED, line.end );
		ow.map[spot[1]] = Terrain.WATER;

		//and when the rod leaves his pack (dropped, sold, stolen): nothing is left to reel in with
		line = FishingLine.cast( hero, ow, spot[1], FishingWaters.Kind.RIVER, Bait.WORM );
		line.act();
		assertSame( line, hero.buff( FishingLine.class ) );
		hero.belongings.backpack.items.remove( hero.belongings.getItem( FishingRod.class ) );
		line.act();
		assertNull( hero.buff( FishingLine.class ) );
		assertSame( FishingLine.End.PULLED, line.end );
	}

	@Test
	public void aCastNamesItsWaterAndItsBait(){
		for (FishingWaters.Kind k : FishingWaters.Kind.values()){
			String said = FishingRod.castLog( k, "QQBAIT" );
			assertFalse( k + ": " + said, said.contains( Messages.NO_TEXT_FOUND ) );
			//the bait on the hook, never the water in its place (a hole in the ice names none)
			assertTrue( k + ": " + said, said.contains( "QQBAIT" ) );
			if (k != FishingWaters.Kind.ICE){
				assertTrue( k + ": " + said, said.contains( Messages.get( FishingRod.class, "water_" + k.name().toLowerCase( Locale.ENGLISH ) ) ) );
			}
		}
	}

	@Test
	public void aLineSurvivesASave(){
		int[] spot = riverBank();
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		FishingLine line = FishingLine.cast( hero, ow, spot[1], FishingWaters.Kind.RIVER, Bait.GLOW );
		while (!(line.state == FishingLine.State.BITE)) line.act();
		Bundle bundle = new Bundle();
		line.storeInBundle( bundle );
		FishingLine back = new FishingLine();
		back.restoreFromBundle( bundle );
		assertTrue( (back.state == FishingLine.State.BITE) );
		assertSame( FishingLine.State.BITE, back.state );
		assertFalse( "a loaded line is simply in the water", back.fresh );
		assertEquals( line.dx(), back.dx() );
		assertEquals( line.dy(), back.dy() );
		Bundle again = new Bundle();
		back.storeInBundle( again );
		assertEquals( bundle.toString(), again.toString() );
		//on its own slice, its bobber where it went in
		assertTrue( back.onThisSlice() );
		assertEquals( spot[1], back.bobberCell() );
		Dungeon.depth = WorldLayers.depthOf( -2 );
		assertFalse( back.onThisSlice() );
		assertEquals( -1, back.bobberCell() );
	}

	@Test
	public void castingIsRefusedWhereNoLineGoes(){
		Dungeon.level = null;
		assertEquals( Messages.get( FishingRod.class, "no_fish_here" ), FishingRod.refusal( hero, 0 ) );
		int[] spot = riverBank();
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		Arrays.fill( ow.heroFOV, true );
		assertNull( FishingRod.refusal( hero, spot[1] ) );
		assertEquals( Messages.get( FishingRod.class, "not_water" ), FishingRod.refusal( hero, spot[0] ) );
		int far = find( ow, c -> FishingWaters.kindAt( ow, c ) != null && ow.map[c] != Terrain.FROZEN_WATER
				&& ow.distance( spot[0], c ) > FishingRules.CAST_RANGE );
		assertTrue( far != -1 );
		assertEquals( Messages.get( FishingRod.class, "reach", FishingRules.CAST_RANGE ), FishingRod.refusal( hero, far ) );
		ow.heroFOV[spot[1]] = false;
		assertNotNull( "only water the hero sees", FishingRod.refusal( hero, spot[1] ) );
		ow.heroFOV[spot[1]] = true;
		//ice is cut only beside the hero
		ow.map[spot[1]] = Terrain.FROZEN_WATER;
		assertEquals( ow.distance( spot[0], spot[1] ) <= FishingRules.ICE_RANGE ? null
				: Messages.get( FishingRod.class, "ice_reach" ), FishingRod.refusal( hero, spot[1] ) );
		ow.map[spot[1]] = Terrain.WATER;
	}

	//a river's water cell with the hero on dry ground two steps from it, on a live window, his rod in
	//his pack (a line stays out only while he has it): {hero, water}
	private int[] riverBank(){
		if (hero.belongings.getItem( FishingRod.class ) == null) hero.belongings.backpack.items.add( new FishingRod() );
		WorldModel.Sample s = new WorldModel.Sample();
		for (int r = 0; r <= 60; r++){
			for (int j = -r; j <= r; j++){
				for (int i = -r; i <= r; i++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int x = i * 64, y = j * 64;
					if (WorldModel.sample( SEED, x, y, WorldModel.calendarShift(), s ).biome != WorldModel.Biome.RIVER
							|| WorldStructures.townCell( x, y ) != -1 || OverworldFauna.nearSettlement( SEED, x, y )) continue;
					OverworldLevel ow = windowAt( x, y );
					int bank = find( ow, c -> dry( ow, c ) && riverWithin( ow, c, 2 ) != -1 );
					if (bank == -1) continue;
					hero.pos = bank;
					return new int[]{ bank, riverWithin( ow, bank, 2 ) };
				}
			}
		}
		throw new AssertionError( "no river bank near the origin" );
	}

	private static boolean dry( OverworldLevel ow, int c ){
		return ow.passable[c] && (Terrain.flags[ow.map[c]] & Terrain.LIQUID) == 0 && ow.map[c] != Terrain.FROZEN_WATER;
	}

	//a river's open water at exactly `d` steps from the cell, -1 for none
	private static int riverWithin( OverworldLevel ow, int c, int d ){
		int w = ow.width();
		for (int dy = -d; dy <= d; dy++){
			for (int dx = -d; dx <= d; dx++){
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != d) continue;
				int n = c + dx + dy * w;
				if (n < 0 || n >= ow.length()) continue;
				if (FishingWaters.openWater( ow.map[n] ) && ow.biomeAtCell( n ) == WorldModel.Biome.RIVER) return n;
			}
		}
		return -1;
	}

	//the live surface window round a world cell, built as a mirror's is and made the host's own
	private static OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		float s = WorldModel.calendarShift();
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, s );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, s, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		try {
			Field f = OverworldLevel.class.getDeclaredField( "network" );
			f.setAccessible( true );
			f.set( ow, false );
		} catch (Exception e){
			throw new AssertionError( e );
		}
		Dungeon.level = ow;
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	//the window cell nearest its middle the test likes (inside the rim), -1 for none
	private static int find( OverworldLevel ow, IntPredicate test ){
		int mid = W / 2 + (H / 2) * W;
		for (int r = 0; r < W / 2 - 4; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int c = mid + dx + dy * W;
					if (c >= 0 && c < ow.length() && test.test( c )) return c;
				}
			}
		}
		return -1;
	}

	//the fish a water, a bait, an hour, a season and a weather favour most
	private static FishSpecies likeliest( FishingWaters.Kind k, Bait b, DayNightCycle.Phase p, GameCalendar.Season s,
			VillageRoutine.Weather sky ){
		FishSpecies best = null;
		float most = -1f;
		for (FishSpecies f : FishSpecies.values()){
			float w = FishingRules.weight( f, k, b, p, s, sky );
			if (w > most){
				most = w;
				best = f;
			}
		}
		return best;
	}
}

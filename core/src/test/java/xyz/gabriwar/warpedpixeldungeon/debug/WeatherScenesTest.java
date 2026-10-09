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

package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.WeatherState;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldCritters;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The weather scenes (DebugScenes: weather-*): all thirteen are listed; each holds its sky whatever
 * scene ran before it, so nothing another one set is left over, and weather-clear releases every
 * override; the climate reads what each promises (the bed's band, a storm, a gale); the fog scene's
 * walk lands on fog or the clearing after rain, still there after the arrival's turn; the plains and
 * the desert are found near the origin, the same biome a band either side; the snow and the blizzard
 * go to frozen ground, where the overlay draws them, and sleet to thawed; and by the river the hero
 * is set down with water and trees close.
 */
public class WeatherScenesTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private static final String[] IDS = { "weather-rain-light", "weather-rain", "weather-rain-heavy", "weather-storm",
			"weather-gale", "weather-sandstorm", "weather-snow", "weather-sleet", "weather-hail", "weather-blizzard",
			"weather-fog", "weather-indoors-rain", "weather-clear" };

	private Level saved;
	private int turn, challenges;
	private float duration;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		saved = Dungeon.level;
		turn = Dungeon.cycleTurn;
		challenges = Dungeon.challenges;
		duration = Statistics.duration;
		Dungeon.challenges = 0;
		ClimateManager.reset();
	}

	@After
	public void tearDown(){
		Dungeon.level = saved;
		Dungeon.cycleTurn = turn;
		Dungeon.challenges = challenges;
		Statistics.duration = duration;
		DebugScenes.clearWeather();
		ClimateManager.reset();
	}

	private static DebugScenes.WeatherScene scene( String id ){
		DebugScenes.Scene s = DebugScenes.byId( id );
		assertNotNull( id + " is listed", s );
		assertTrue( id + " is a weather scene", s instanceof DebugScenes.WeatherScene );
		return (DebugScenes.WeatherScene) s;
	}

	//what the climate reads under the overrides, the ones a weather scene sets and the two it leaves alone
	private static String read(){
		return ClimateManager.localPrecipType() + " " + ClimateManager.localPrecipRate() + " " + ClimateManager.localWindSpeed()
				+ " " + ClimateManager.surfaceWindDir() + " " + ClimateManager.isStorming()
				+ " | " + ClimateManager.debugTempOverride + " " + ClimateManager.debugCloudOverride;
	}

	@Test
	public void theScenesAreListed(){
		for (String id : IDS) scene( id );
		int weather = 0;
		for (DebugScenes.Scene s : DebugScenes.SCENES) if (s instanceof DebugScenes.WeatherScene) weather++;
		assertEquals( IDS.length, weather );
	}

	@Test
	public void eachSceneHoldsItsSkyWhateverRanBefore(){
		ClimateManager.debugTempOverride = 7f;
		ClimateManager.debugCloudOverride = 0.4f;
		for (String id : IDS){
			if (scene( id ).sky == null) continue;
			scene( id ).sky.hold();
			String alone = read();
			for (String before : IDS){
				if (scene( before ).sky == null) continue;
				scene( before ).sky.hold();
				scene( id ).sky.hold();
				assertEquals( id + " after " + before, alone, read() );
			}
		}
		//the temperature and the clouds are no weather scene's: left as they were
		assertEquals( 7f, ClimateManager.debugTempOverride, 0f );
		assertEquals( 0.4f, ClimateManager.debugCloudOverride, 0f );
	}

	@Test
	public void weatherClearReleasesEveryOverride(){
		assertNull( scene( "weather-clear" ).sky );
		scene( "weather-storm" ).sky.hold();
		ClimateManager.debugTempOverride = -12f;
		ClimateManager.debugCloudOverride = 1f;
		DebugScenes.clearWeather();
		assertNull( ClimateManager.debugPrecipTypeOverride );
		assertTrue( Float.isNaN( ClimateManager.debugPrecipOverride ) );
		assertTrue( Float.isNaN( ClimateManager.debugWindOverride ) );
		assertTrue( Float.isNaN( ClimateManager.debugWindDirOverride ) );
		assertTrue( Float.isNaN( ClimateManager.debugTempOverride ) );
		assertTrue( Float.isNaN( ClimateManager.debugCloudOverride ) );
		assertFalse( ClimateManager.debugForceStorm );
		assertFalse( "the sky is the climate's own again", ClimateManager.isStorming() );
	}

	@Test
	public void theClimateReadsWhatEachScenePromises(){
		//the rain scenes, one to each rain bed's band (below 0.15, below 0.35, above), the wind under the gusts'
		float[][] bands = { { 0.01f, 0.15f }, { 0.15f, 0.35f }, { 0.35f, 1.01f } };
		String[] rains = { "weather-rain-light", "weather-rain", "weather-rain-heavy" };
		for (int i = 0; i < rains.length; i++){
			scene( rains[i] ).sky.hold();
			assertEquals( PrecipType.RAIN, ClimateManager.localPrecipType() );
			assertTrue( rains[i], ClimateManager.localPrecipRate() >= bands[i][0] && ClimateManager.localPrecipRate() < bands[i][1] );
			assertTrue( ClimateManager.localWindSpeed() < 7f );
			assertFalse( ClimateManager.isStorming() );
		}
		scene( "weather-storm" ).sky.hold();
		assertTrue( "the scene forces its storm", ClimateManager.isStorming() );
		assertEquals( PrecipType.RAIN, ClimateManager.localPrecipType() );
		assertTrue( ClimateManager.localPrecipRate() >= 0.35f && ClimateManager.localWindSpeed() >= 10f );
		for (String dry : new String[]{ "weather-gale", "weather-sandstorm" }){
			scene( dry ).sky.hold();
			assertTrue( dry, ClimateManager.localPrecipRate() < 0.01f && ClimateManager.localWindSpeed() >= 10f );
			assertFalse( ClimateManager.isStorming() );
		}
		PrecipType[] types = { PrecipType.SNOW, PrecipType.SLEET, PrecipType.HAIL, PrecipType.BLIZZARD };
		String[] ids = { "weather-snow", "weather-sleet", "weather-hail", "weather-blizzard" };
		for (int i = 0; i < ids.length; i++){
			scene( ids[i] ).sky.hold();
			assertEquals( types[i], ClimateManager.localPrecipType() );
			assertTrue( ClimateManager.localPrecipRate() >= 0.01f );
		}
		assertTrue( "the blizzard blows", ClimateManager.localWindSpeed() >= 10f );
		scene( "weather-fog" ).sky.hold();
		assertTrue( ClimateManager.localPrecipRate() < 0.01f && ClimateManager.localWindSpeed() < 7f );
		scene( "weather-indoors-rain" ).sky.hold();
		assertEquals( PrecipType.RAIN, ClimateManager.localPrecipType() );
		assertTrue( ClimateManager.localPrecipRate() >= 0.01f );
		assertTrue( "the wind left to the climate: none indoors", Float.isNaN( ClimateManager.debugWindOverride ) );
	}

	@Test
	public void theWindIsHeardFromWhereItComes(){
		//blowing toward the east, it comes from the west: the left (pan -sin(dir))
		assertTrue( DebugScenes.windSide( 90f ).endsWith( "from the west, on your left" ) );
		assertTrue( DebugScenes.windSide( 270f ).endsWith( "from the east, on your right" ) );
		assertTrue( DebugScenes.windSide( 0f ).endsWith( "from the south, in both ears alike" ) );
		assertEquals( "north-east", DebugScenes.compass( 45f ) );
		assertEquals( "north", DebugScenes.compass( 359f ) );
		assertEquals( "north-west", DebugScenes.compass( -45f ) );
	}

	@Test
	public void theFogWalkLandsOnFogOrAClearing(){
		ArrayList<String> misses = new ArrayList<>(), gone = new ArrayList<>();
		for (long seed = 1; seed <= 40; seed++){
			for (int day : new int[]{ 0, 40, 100, 160, 220, 300 }){
				ClimateManager.reset();
				ClimateManager.onNewGame( seed );
				Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + 600;
				ClimateManager.onHeroTurn();
				int before = Dungeon.cycleTurn;
				Statistics.duration = 0f;
				boolean found = DebugScenes.walkToClearing( 4 * DayNightCycle.FULL_CYCLE );
				assertTrue( "the clock never runs back", Dungeon.cycleTurn >= before );
				assertEquals( "the game's duration moves with it", Dungeon.cycleTurn - before, Statistics.duration, 0.01f );
				WeatherState s = ClimateManager.weatherState();
				if (found) assertTrue( s == WeatherState.FOG || s == WeatherState.CLEARING );
				if (!found){
					misses.add( "seed " + seed + " day " + day + " (" + GameCalendar.season() + ")" );
					continue;
				}
				//the arrival's own turn steps the climate once more (DayNightCycle.onHeroTurn), and the
				//developer takes a few steps to listen: what the walk found is still there
				for (int t = 0; t < 3; t++){
					Dungeon.cycleTurn++;
					ClimateManager.onHeroTurn();
					s = ClimateManager.weatherState();
					if (s != WeatherState.FOG && s != WeatherState.CLEARING){
						gone.add( "seed " + seed + " day " + day + " after " + (t + 1) + " turns" );
						break;
					}
				}
			}
		}
		assertTrue( gone.size() + " fogs or clearings were gone by the arrival: " + gone, gone.isEmpty() );
		//four days now and then bring neither (the scene says so): rarely
		assertTrue( misses.size() + " walks found no fog or clearing: " + misses, misses.size() * 50 <= 40 * 6 );
	}

	@Test
	public void aRealClockRunKeepsItsOwnSky(){
		Dungeon.challenges = Challenges.REAL_CLOCK;
		Dungeon.cycleTurn = 1234;
		assertFalse( DebugScenes.walkToClearing( 4 * DayNightCycle.FULL_CYCLE ) );
		assertEquals( 1234, Dungeon.cycleTurn );
	}

	@Test
	public void thePlainsAndTheDesertAreFound(){
		WorldModel.Sample s = new WorldModel.Sample();
		for (long seed : new long[]{ SEED, 1L, 2L, 3L }){
			for (WorldModel.Biome biome : new WorldModel.Biome[]{ WorldModel.Biome.PLAINS, WorldModel.Biome.DESERT }){
				long t = System.currentTimeMillis();
				int[] spot = DebugScenes.biomeGround( seed, 0f, biome );
				System.out.println( "[weather scenes] seed " + seed + " " + biome + " at "
						+ (spot == null ? "none" : spot[0] + "," + spot[1]) + " in " + (System.currentTimeMillis() - t) + " ms" );
				assertNotNull( biome + " for seed " + seed, spot );
				for (float shift : new float[]{ 0f, -WorldModel.CLIMATE_SHIFT, WorldModel.CLIMATE_SHIFT }){
					assertEquals( biome, WorldModel.sample( seed, spot[0], spot[1], shift, s ).biome );
				}
				WorldModel.sample( seed, spot[0], spot[1], 0f, s );
				int ground = WorldModel.wildTerrain( seed, spot[0], spot[1], s );
				assertTrue( (Terrain.flags[ground] & Terrain.PASSABLE) != 0 && !DebugScenes.water( ground ) );
				assertEquals( -1, WorldStructures.townCell( spot[0], spot[1] ) );
				assertFalse( OverworldFauna.nearSettlement( seed, spot[0], spot[1] ) );
			}
		}
	}

	@Test
	public void eachFallIsDrawnOnTheGroundItsSceneFinds(){
		//on the surface the overlay draws snow and the blizzard only over frozen ground and rain (and
		//sleet's own streaks) only over thawed ground (WeatherOverlay's GroundEmitter, by the window's
		//frozen mask: the world's temperature under FREEZE, which no climate override reaches): the
		//ground round the spot is the one its fall shows on, at the seasonal shift and a band either side
		String[] frozen = { "weather-snow", "weather-blizzard" }, thawed = { "weather-sleet" };
		WorldModel.Sample s = new WorldModel.Sample();
		for (long seed : new long[]{ SEED, 1L, 2L, 3L }){
			for (String id : IDS){
				boolean cold = Arrays.asList( frozen ).contains( id );
				if (!cold && !Arrays.asList( thawed ).contains( id )) continue;
				long t = System.currentTimeMillis();
				int[] spot = DebugScenes.WeatherScene.find( scene( id ).ground, seed, 0f );
				System.out.println( "[weather scenes] seed " + seed + " " + id + " at "
						+ (spot == null ? "none" : spot[0] + "," + spot[1]) + " in " + (System.currentTimeMillis() - t) + " ms" );
				assertNotNull( id + " for seed " + seed, spot );
				for (float shift : new float[]{ 0f, -WorldModel.CLIMATE_SHIFT, WorldModel.CLIMATE_SHIFT }){
					for (int y = -8; y <= 8; y += 4){
						for (int x = -8; x <= 8; x += 4){
							float temp = WorldModel.sample( seed, spot[0] + x, spot[1] + y, shift, s ).temperature;
							assertEquals( id + " for seed " + seed + " at " + x + "," + y + " shift " + shift,
									cold, temp < WorldModel.FREEZE );
						}
					}
				}
			}
		}
	}

	@Test
	public void byTheRiverTheHeroHasWaterAndTreesClose() throws Exception {
		float shift = WorldModel.calendarShift();
		int[] spot = OverworldCritters.findCritterGround( SEED, shift );
		assertNotNull( spot );
		PathFinder.setMapSize( W, H );
		int ox = spot[0] - W / 2, oy = spot[1] - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, shift );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, shift, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		Field net = OverworldLevel.class.getDeclaredField( "network" );
		net.setAccessible( true );
		net.set( ow, false );
		Dungeon.level = ow;
		int landing = W / 2 + (H / 2) * W;
		int cell = DebugScenes.byWaterAndTrees( ow, landing, 12 );
		assertTrue( "no dry ground with water and trees close within 12 cells", cell != -1 );
		assertTrue( ow.passable[cell] && !ow.pit[cell] && !DebugScenes.water( ow.map[cell] ) );
		int cx = cell % W, cy = cell / W;
		boolean water = false, tree = false;
		for (int y = cy - 5; y <= cy + 5; y++){
			for (int x = cx - 5; x <= cx + 5; x++){
				int t = ow.map[x + y * W], d = Math.max( Math.abs( x - cx ), Math.abs( y - cy ) );
				if (d <= 3 && DebugScenes.water( t )) water = true;
				if (DebugScenes.tree( t )) tree = true;
			}
		}
		assertTrue( "water within three cells", water );
		assertTrue( "a tree within five", tree );
	}
}

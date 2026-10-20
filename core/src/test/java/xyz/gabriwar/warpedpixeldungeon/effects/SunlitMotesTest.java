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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.WeatherOverlayAmbient;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SunMoteParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The sunlit motes: a clear summer's day on the surface is theirs where nothing else has the air,
 * a clear spring day keeps its petals (the motes fly among them), and autumn, winter, the night, the
 * peaks and the caves have none; they rise off the surface's grass and green floor, never off sand,
 * snow, water or rock; and a mote drifts slowly the way the wind blows, rises a little, sits on whole
 * pixels and glints without a flicker, dressing to the budget.
 */
public class SunlitMotesTest {

	private static final float DT = 1f / 60f;

	private Level level;
	private int turn, startDay, challenges;
	private DayNightCycle.Phase phase;
	private float wind, dir, elapsed;

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		level = Dungeon.level;
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		phase = DayNightCycle.debugPhaseOverride;
		wind = ClimateManager.debugWindOverride;
		dir = ClimateManager.debugWindDirOverride;
		elapsed = Game.elapsed;
		Dungeon.challenges = 0;
		Dungeon.cycleTurn = 0;
	}

	@After
	public void restore(){
		Dungeon.level = level;
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.challenges = challenges;
		DayNightCycle.debugPhaseOverride = phase;
		ClimateManager.debugWindOverride = wind;
		ClimateManager.debugWindDirOverride = dir;
		Game.elapsed = elapsed;
		ClimateManager.setLocalFog( false );
		ClimateManager.reset();
		Fx.frame();
	}

	//the calendar on a day of this season
	private static void season( GameCalendar.Season s ){
		for (int d = 0; d < 800; d++){
			Dungeon.calendarStartDay = d;
			if (GameCalendar.season() == s) return;
		}
		fail( "no day of " + s );
	}

	@Test
	public void aClearSummerDayIsTheMotesAndEveryOtherAirKeepsItsOwn(){
		//a clear, mild day on the surface itself (the climate's depth 0, 13 C)
		ClimateManager.reset();
		ClimateManager.setLocalFog( false );
		ClimateManager.onLevelChange( 0 );
		Dungeon.level = new OverworldLevel();
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;

		season( GameCalendar.Season.SUMMER );
		assertTrue( ClimateManager.sunlitMotes() );
		assertEquals( WeatherOverlayAmbient.MOTES, ClimateManager.ambientType() );

		//the spring's clear days keep their petals: the motes fly among them, in their air
		season( GameCalendar.Season.SPRING );
		assertTrue( ClimateManager.sunlitMotes() );
		assertEquals( WeatherOverlayAmbient.SPRING_PETALS, ClimateManager.ambientType() );

		for (GameCalendar.Season s : new GameCalendar.Season[]{ GameCalendar.Season.AUTUMN, GameCalendar.Season.WINTER }){
			season( s );
			assertFalse( s + "", ClimateManager.sunlitMotes() );
			assertNotEquals( WeatherOverlayAmbient.MOTES, ClimateManager.ambientType() );
		}

		//by night the fireflies have the summer's air
		season( GameCalendar.Season.SUMMER );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		assertFalse( ClimateManager.sunlitMotes() );
		assertEquals( WeatherOverlayAmbient.FIREFLIES, ClimateManager.ambientType() );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DUSK;
		assertFalse( ClimateManager.sunlitMotes() );

		//the surface's own: not the peaks, not the caves
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		for (int altitude : new int[]{ 3, -2 }){
			Dungeon.level = new OverworldLevel( altitude );
			assertFalse( "altitude " + altitude, ClimateManager.sunlitMotes() );
			assertNotEquals( WeatherOverlayAmbient.MOTES, ClimateManager.ambientType() );
		}
	}

	@Test
	public void motesRiseOffTheGrassAndNeverOffSandSnowWaterOrRock(){
		OverworldLevel ow = new OverworldLevel();
		ow.setSize( 8, 8 );
		Dungeon.level = ow;
		int cell = 1 + 8, S = DungeonTilemap.SIZE;
		float px = S + 5, py = S + 9;
		for (int t : new int[]{ Terrain.GRASS, Terrain.HIGH_GRASS, Terrain.FURROWED_GRASS, Terrain.FLOWER_PATCH, Terrain.EMPTY }){
			ow.map[cell] = t;
			assertTrue( "terrain " + t, WeatherOverlay.moteGround( px, py ) );
		}
		for (int t : new int[]{ Terrain.EMPTY_SP, Terrain.SNOW, Terrain.WATER, Terrain.WALL, Terrain.TREE_OAK, Terrain.DIRT_PATH }){
			ow.map[cell] = t;
			assertFalse( "terrain " + t, WeatherOverlay.moteGround( px, py ) );
		}
		ow.map[cell] = Terrain.GRASS;
		assertFalse( "off the window", WeatherOverlay.moteGround( -1f, py ) );
		assertFalse( "off the window", WeatherOverlay.moteGround( 8 * S + 1, py ) );

		//anywhere but the overworld, none
		Level plain = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		plain.setSize( 8, 8 );
		plain.map[cell] = Terrain.GRASS;
		Dungeon.level = plain;
		assertFalse( WeatherOverlay.moteGround( px, py ) );
	}

	@Test
	public void aMoteDriftsSlowlyTheWindsWayOnWholePixelsAndGlintsWithoutAFlicker(){
		Dungeon.level = new OverworldLevel();
		ClimateManager.debugWindOverride = 8f;
		Game.elapsed = DT;
		final int n = 200;
		for (float toward : new float[]{ 90f, 270f }){
			ClimateManager.debugWindDirOverride = toward;
			Fx.frame();
			float drift = 0f, rise = 0f, brightest = 0f;
			for (int i = 0; i < n; i++){
				SunMoteParticle m = new SunMoteParticle().drift( 300f, 300f );
				assertEquals( "dressing to the budget", FxBudget.P3, m.priority() );
				float x0 = m.px, y0 = m.py;
				m.update();
				float last = m.am;
				//three seconds of its five to eight
				for (int f = 1; f < 180; f++){
					m.update();
					assertTrue( m.alive );
					assertTrue( "a flicker of " + Math.abs( m.am - last ), Math.abs( m.am - last ) < 0.05f );
					assertTrue( m.am <= 1f );
					last = m.am;
					brightest = Math.max( brightest, m.am );
					assertEquals( "on whole pixels", Math.round( m.x ), m.x, 0f );
					assertEquals( "on whole pixels", Math.round( m.y ), m.y, 0f );
				}
				drift += m.px - x0;
				rise += y0 - m.py;
				m.kill();
			}
			drift /= n;
			rise /= n;
			if (toward == 90f) assertTrue( "east: " + drift, drift > 4f && drift < 30f );
			else assertTrue( "west: " + drift, drift < -4f && drift > -30f );
			assertTrue( "rising a little: " + rise, rise > 0f && rise < 12f );
			//now and then one turns to the sun, brighter than its own alpha
			assertTrue( "the glints: " + brightest, brightest > SunMoteParticle.PEAK * 1.3f );
		}
	}
}

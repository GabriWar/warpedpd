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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The debug scenes' clock walk (DebugScenes.walkClock), which the critter scenes and the
 * later overworld-life scenes lean on: from any day of the year and any hour it lands on
 * the season and the phase asked for, only ever forward, and leaves a real-clock run alone.
 */
public class WalkClockTest {

	private int turn, startDay, challenges;
	private float duration;
	private Phase override;

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		duration = Statistics.duration;
		override = DayNightCycle.debugPhaseOverride;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
	}

	@After
	public void restore(){
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.challenges = challenges;
		Statistics.duration = duration;
		DayNightCycle.debugPhaseOverride = override;
		ClimateManager.reset();
	}

	//every day of two years (the first one a leap year), early in the day and late at night
	private static ArrayList<String> misses( Season season, Phase phase ){
		ArrayList<String> out = new ArrayList<>();
		for (int day = 0; day < 730; day++){
			for (int pos : new int[]{ 100, 1300, 2400 }){
				Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + pos;
				Season was = GameCalendar.season();
				int before = Dungeon.cycleTurn;
				//from zero each time: a float this test kept adding to would lose the turns to rounding
				Statistics.duration = 0f;
				assertTrue( DebugScenes.walkClock( season, phase, 0.3f ) );
				assertTrue( "the clock never runs back", Dungeon.cycleTurn >= before );
				assertEquals( "the game's duration moves with it", Dungeon.cycleTurn - before, Statistics.duration, 0.01f );
				if (GameCalendar.season() != season || DayNightCycle.phase() != phase){
					out.add( "day " + day + " turn " + pos + " (" + was + ") -> " + GameCalendar.season() + " " + DayNightCycle.phase() );
				}
			}
		}
		return out;
	}

	@Test
	public void theClockWalksOnToASummerDayFromAnyHour(){
		ArrayList<String> m = misses( Season.SUMMER, Phase.DAY );
		assertTrue( m.size() + " misses, e.g. " + (m.isEmpty() ? "" : m.get( 0 )) + " " + m, m.isEmpty() );
	}

	@Test
	public void theClockWalksOnToASummerNightFromAnyHour(){
		ArrayList<String> m = misses( Season.SUMMER, Phase.NIGHT );
		assertTrue( m.size() + " misses, e.g. " + (m.isEmpty() ? "" : m.get( 0 )) + " " + m, m.isEmpty() );
	}

	@Test
	public void theDebugHourOverrideIsCleared(){
		DayNightCycle.debugPhaseOverride = Phase.DUSK;
		Dungeon.cycleTurn = 10 * DayNightCycle.FULL_CYCLE;
		assertTrue( DebugScenes.walkClock( null, Phase.DAY, 0.3f ) );
		assertEquals( Phase.DAY, DayNightCycle.phase() );
	}

	@Test
	public void aRealClockRunKeepsItsOwnTime(){
		Dungeon.challenges = Challenges.REAL_CLOCK;
		Dungeon.cycleTurn = 1234;
		float dur = Statistics.duration;
		assertFalse( DebugScenes.walkClock( Season.SUMMER, Phase.DAY, 0.3f ) );
		assertEquals( 1234, Dungeon.cycleTurn );
		assertEquals( dur, Statistics.duration, 0f );
	}
}

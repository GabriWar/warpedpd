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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The fortune teller reads three items a week (TownLedger.readingSpent / spendReading). */
public class FortuneReadingTest {

	private int turn, startDay, challenges, week, used;

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		week = TownLedger.readingWeek;
		used = TownLedger.readingsThisWeek;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		Dungeon.cycleTurn = 0;
		TownLedger.reset();
	}

	@After
	public void restore(){
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.challenges = challenges;
		TownLedger.readingWeek = week;
		TownLedger.readingsThisWeek = used;
	}

	private static void day( int d ){
		Dungeon.cycleTurn = d * DayNightCycle.FULL_CYCLE + 100;
	}

	//the first day of a week, so the rest of it lies ahead
	private static int weekStart(){
		day( 0 );
		int w = GameCalendar.weekIndex();
		int d = 0;
		while (GameCalendar.weekIndex() == w){
			d++;
			day( d );
		}
		return d;
	}

	@Test
	public void threeReadingsUntilTheWeekTurns(){
		int start = weekStart();
		day( start );
		for (int i = 0; i < TownLedger.READINGS_PER_WEEK; i++){
			assertFalse( TownLedger.readingSpent() );
			assertEquals( TownLedger.READINGS_PER_WEEK - i, TownLedger.readingsLeft() );
			day( start + i );
			TownLedger.spendReading();
		}
		assertTrue( TownLedger.readingSpent() );

		//the last day of the week is still that week
		day( start + 6 );
		assertTrue( TownLedger.readingSpent() );

		day( start + 7 );
		assertFalse( TownLedger.readingSpent() );
		assertEquals( TownLedger.READINGS_PER_WEEK, TownLedger.readingsLeft() );
	}

	@Test
	public void theReadingsAreSavedAndOldSavesStartAFreshWeek(){
		day( weekStart() );
		for (int i = 0; i < TownLedger.READINGS_PER_WEEK; i++) TownLedger.spendReading();
		Bundle bundle = new Bundle();
		TownLedger.storeInBundle( bundle );
		TownLedger.reset();
		assertFalse( TownLedger.readingSpent() );
		TownLedger.restoreFromBundle( bundle );
		assertTrue( TownLedger.readingSpent() );

		//a save from the one-a-season days: the season it read in, no week
		Bundle old = new Bundle();
		old.put( "town_meal_day", -1 );
		old.put( "town_reading_season", GameCalendar.seasonIndex() );
		TownLedger.restoreFromBundle( old );
		assertFalse( TownLedger.readingSpent() );
	}
}

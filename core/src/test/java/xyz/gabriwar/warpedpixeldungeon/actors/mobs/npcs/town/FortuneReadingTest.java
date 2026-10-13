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

/** The fortune teller reads one item a season (TownLedger.readingSpent / spendReading). */
public class FortuneReadingTest {

	private int turn, startDay, challenges, reading;

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		reading = TownLedger.readingSeason;
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
		TownLedger.readingSeason = reading;
	}

	private static void day( int d ){
		Dungeon.cycleTurn = d * DayNightCycle.FULL_CYCLE + 100;
	}

	@Test
	public void oneReadingUntilTheSeasonTurns(){
		day( 0 );
		assertEquals( Season.SPRING, GameCalendar.season() );
		assertFalse( TownLedger.readingSpent() );
		TownLedger.spendReading();
		assertTrue( TownLedger.readingSpent() );

		//the last day of spring is still spring
		day( GameCalendar.daysInSeason( Season.SPRING, 0 ) - 1 );
		assertEquals( Season.SPRING, GameCalendar.season() );
		assertTrue( TownLedger.readingSpent() );

		day( GameCalendar.daysInSeason( Season.SPRING, 0 ) );
		assertEquals( Season.SUMMER, GameCalendar.season() );
		assertFalse( TownLedger.readingSpent() );
	}

	@Test
	public void theSameSeasonNextYearIsANewOne(){
		day( 0 );
		TownLedger.spendReading();

		day( GameCalendar.daysInYear( 0 ) );
		assertEquals( Season.SPRING, GameCalendar.season() );
		assertEquals( 1, GameCalendar.year() );
		assertFalse( TownLedger.readingSpent() );
	}

	@Test
	public void winterIntoSpringIsANewSeason(){
		int winter = GameCalendar.daysInYear( 0 ) - 1;
		day( winter );
		assertEquals( Season.WINTER, GameCalendar.season() );
		TownLedger.spendReading();
		assertTrue( TownLedger.readingSpent() );
		day( winter + 1 );
		assertFalse( TownLedger.readingSpent() );
	}

	@Test
	public void theReadingIsSavedAndOldSavesHaveOne(){
		day( 0 );
		TownLedger.spendReading();
		Bundle bundle = new Bundle();
		TownLedger.storeInBundle( bundle );
		TownLedger.reset();
		assertFalse( TownLedger.readingSpent() );
		TownLedger.restoreFromBundle( bundle );
		assertTrue( TownLedger.readingSpent() );

		//a save from before the limit: the ledger's books but no reading kept
		Bundle old = new Bundle();
		old.put( "town_meal_day", -1 );
		TownLedger.restoreFromBundle( old );
		assertFalse( TownLedger.readingSpent() );
	}
}

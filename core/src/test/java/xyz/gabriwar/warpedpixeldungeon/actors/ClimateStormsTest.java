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

package xyz.gabriwar.warpedpixeldungeon.actors;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * The climate brings storms of its own: about 60 a year, mostly in summer and autumn, each holding long
 * enough to be heard rather than flickering with the wind's wobble, and now and then a cold one
 * turns to hail. Before the rain rate was raised no front could reach a storm at all.
 */
public class ClimateStormsTest {

	private int turn, startDay, challenges;

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
	}

	@After
	public void restore(){
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.challenges = challenges;
		ClimateManager.reset();
	}

	//4 runs of a whole year each
	@Test
	public void stormsComeRarelyHoldAndCanHail(){
		long wet = 0, storm = 0, storms = 0, hail = 0;
		for (int seed = 1; seed <= 4; seed++){
			Dungeon.cycleTurn = 0;
			ClimateManager.onNewGame( seed * 7919L );
			boolean was = false;
			for (int t = 0; t < 364 * DayNightCycle.FULL_CYCLE; t++){
				Dungeon.cycleTurn = t;
				ClimateManager.onHeroTurn();
				boolean is = ClimateManager.weatherState() == WeatherState.STORM;
				if (ClimateManager.precipRate() > 0.05f) wet++;
				if (is) storm++;
				if (is && !was) storms++;
				if (ClimateManager.precipType() == PrecipType.HAIL) hail++;
				was = is;
			}
		}
		float share = storm / (float) wet;
		assertTrue( "storms come at all: " + storms, storms >= 150 );
		assertTrue( "storms stay a minority of the rain: " + share + " of wet turns", share > 0.03f && share < 0.08f );
		assertTrue( "a storm holds a while: " + storm / (float) storms + " turns", storm / (float) storms >= 90 );
		assertTrue( "a cold storm can hail", hail > 0 );
	}
}

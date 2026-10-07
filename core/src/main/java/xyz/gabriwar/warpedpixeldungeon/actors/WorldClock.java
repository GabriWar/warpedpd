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

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;

/**
 * Which day it is, and which night, for everything in the world that keeps a calendar of
 * its own: the same answer on a turn-counted run and on a real-clock one. A real-clock run
 * never moves Dungeon.cycleTurn (DayNightCycle.onHeroTurn), so nothing may count days off
 * it directly. Local time is the epoch plus the zone's offset, as GameCalendar.weekIndex
 * reads it, without a Calendar: the villages ask every turn.
 */
public final class WorldClock {

	private WorldClock(){}

	private static final long DAY_MS = 86_400_000L;

	private static long localMillis(){
		long ms = System.currentTimeMillis();
		return ms + java.util.TimeZone.getDefault().getOffset( ms );
	}

	/** The day: on a turn-counted run it starts at dawn, on a real-clock run at local midnight. */
	public static int day(){
		if (Dungeon.isChallenged( Challenges.REAL_CLOCK )){
			return (int) Math.floorDiv( localMillis(), DAY_MS );
		}
		return Math.floorDiv( Dungeon.cycleTurn, DayNightCycle.FULL_CYCLE );
	}

	/**
	 * The night that is on, or that last fell: it begins halfway through the day before
	 * it, so the evening, the dark and the dawn after it all count as one night. On a
	 * turn-counted run the turn of the change is 500 of the cycle, inside the DAY phase in
	 * every season (DAY always covers [250, 850)); on a real-clock run it is local noon.
	 */
	public static int night(){
		if (Dungeon.isChallenged( Challenges.REAL_CLOCK )){
			return (int) Math.floorDiv( localMillis() - DAY_MS / 2, DAY_MS );
		}
		return Math.floorDiv( Dungeon.cycleTurn - 500, DayNightCycle.FULL_CYCLE );
	}

	/**
	 * The world's turn, FULL_CYCLE of them to a day of day(): what the world's timed events
	 * (levels/overworld/WorldEvents) start and end by. On a turn-counted run it is the game's
	 * own turn; on a real-clock run the local day and the share of it gone by, in turns.
	 */
	public static int turn(){
		if (Dungeon.isChallenged( Challenges.REAL_CLOCK )){
			long local = localMillis();
			return (int)( Math.floorDiv( local, DAY_MS ) * DayNightCycle.FULL_CYCLE
					+ Math.floorMod( local, DAY_MS ) * DayNightCycle.FULL_CYCLE / DAY_MS );
		}
		return Dungeon.cycleTurn;
	}

	/** The world turn (turn()) the night of this index (night()) begins at: on the day of the
	 *  same index, at its turn 500 on a turn-counted run and at local noon on a real-clock one. */
	public static int nightStart( int night ){
		if (Dungeon.isChallenged( Challenges.REAL_CLOCK )){
			return night * DayNightCycle.FULL_CYCLE + DayNightCycle.FULL_CYCLE / 2;
		}
		return night * DayNightCycle.FULL_CYCLE + 500;
	}
}

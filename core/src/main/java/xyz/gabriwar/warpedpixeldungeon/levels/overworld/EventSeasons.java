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

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The seasons as the world's seasonal events keep them (FestivalEvent, MigrationEvent): the
 * season a day of the world's clock falls in, from its first day to its last. The world's other
 * events keep the season out of their reckoning, as a real-clock run's season used to come off
 * the wall clock; GameCalendar.seasonOfDay gives it for any day instead - on a turn-counted run
 * through the run's calendar start, on a real-clock run through that day's date - and both are
 * fixed for the run and shipped to co-op guests (StateSerializer), so every machine and every
 * thread finds the same seasons on the same days. An event keyed to the season of its own days
 * never straddles two, so nothing flickers as a season turns.
 *
 * A span is found by walking out from the day while the season holds (a season is 90 to 92
 * days on either clock), and kept for every day of it.
 */
final class EventSeasons {

	private EventSeasons(){}

	/** One season of the world's clock: its first day, how many days it has, and which it is. */
	static final class Span {
		final int first, days;
		final GameCalendar.Season season;

		Span( int first, int days, GameCalendar.Season season ){
			this.first = first;
			this.days = days;
			this.season = season;
		}

		/** The day after its last. */
		int end(){
			return first + days;
		}
	}

	//no season runs past this many days on either clock; one before a turn-counted calendar's
	//start is taken as this long
	private static final int LONGEST = 100, BEFORE = 91;

	//the spans found, by every day they hold, for the run they were found in (its calendar start
	//and whether it keeps the real clock): any thread may ask
	private static volatile long run = Long.MIN_VALUE;
	private static final ConcurrentHashMap<Integer, Span> spans = new ConcurrentHashMap<>();

	/** The season the day (WorldClock.day) falls in. Any thread. */
	static Span of( int day ){
		long now = ((long) Dungeon.calendarStartDay << 1) | (Dungeon.isChallenged( Challenges.REAL_CLOCK ) ? 1 : 0);
		if (now != run){
			synchronized (spans){
				if (now != run){
					spans.clear();
					run = now;
				}
			}
		}
		Span s = spans.get( day );
		if (s != null) return s;
		//a turn-counted run's calendar begins with the spring of its year 0: the days before that
		//(never played, only looked back on from a run's first days) are whole seasons going back
		int floor = Dungeon.isChallenged( Challenges.REAL_CLOCK ) ? Integer.MIN_VALUE : -Dungeon.calendarStartDay;
		if (day < floor){
			int k = Math.floorDiv( day - floor, BEFORE );
			s = new Span( floor + BEFORE * k, BEFORE, GameCalendar.Season.values()[Math.floorMod( k, 4 )] );
		} else {
			GameCalendar.Season season = GameCalendar.seasonOfDay( day );
			int first = day, last = day;
			while (first > floor && day - first < LONGEST && GameCalendar.seasonOfDay( first - 1 ) == season) first--;
			while (last - day < LONGEST && GameCalendar.seasonOfDay( last + 1 ) == season) last++;
			s = new Span( first, last - first + 1, season );
		}
		//a long real-clock run asks of a few years at most: kept within bounds all the same
		if (spans.size() > 4096) spans.clear();
		for (int d = s.first; d < s.end(); d++) spans.put( d, s );
		return s;
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import com.watabou.utils.Bundle;

//what the town remembers about this run: who has bought what today, which services
//are spent, and the guard's open bounty. Saved with the game next to Statistics.
public class TownLedger {

	//the servant's meals and the cinema's shows climb in price through the day and
	//reset at dawn; the librarian's pages climb for the whole run
	public static int mealDay,  mealsToday;
	public static int showDay,  showsToday;
	public static int pagesBought;

	//one a day each: the tithe, the bard's song, the keepsake sale
	public static int titheDay, songDay, keepsakeDay;

	//the mercenary teaches one lesson per boss you have put down
	public static int lessonsTaken;

	//three bets a day, before the drunkard loses count
	public static int betDay, betsToday;

	//the guard posts one bounty a day; null target means nothing is posted
	public static int bountyDay;
	public static Class<? extends Mob> bountyTarget;
	public static int bountyReward;

	//which of the town's services this run has used at least once, one bit each,
	//for the Town Regular badge
	public static final int MEAL = 1, SHOW = 2, PAGE = 4, TITHE = 8, SONG = 16,
			KEEPSAKE = 32, LESSON = 64, BET = 128, BOUNTY = 256, BED = 512;
	public static int servicesUsed;

	public static void used( int service ){
		servicesUsed |= service;
		xyz.gabriwar.warpedpixeldungeon.Badges.validateTownRegular();
	}

	public static void reset(){
		mealDay = showDay = titheDay = songDay = keepsakeDay = betDay = bountyDay = -1;
		mealsToday = showsToday = pagesBought = lessonsTaken = betsToday = 0;
		bountyTarget = null;
		bountyReward = 0;
		servicesUsed = 0;
	}

	//the calendar day, so the once-a-day services roll over at the same moment
	//the townsfolk wake
	public static int today(){
		return Dungeon.cycleTurn / DayNightCycle.FULL_CYCLE;
	}

	//the town charges what the dungeon pays: prices step up with the deepest floor
	//reached, on the same step the surface shops use (Shopkeeper.sellPrice)
	public static int scaled( int base ){
		return base * (Statistics.deepestFloor / 5 + 1);
	}

	//each repeat of a service costs half again as much as the last
	public static int escalated( int base, int bought ){
		return Math.round( scaled( base ) * (float)Math.pow( 1.5, bought ) );
	}

	//how many of the five bosses are down: a boss only banks its score on death
	public static int bossesSlain(){
		int n = 0;
		for (int score : Statistics.bossScores) if (score > 0) n++;
		return n;
	}

	private static final String MEAL_DAY     = "town_meal_day";
	private static final String MEALS        = "town_meals";
	private static final String SHOW_DAY     = "town_show_day";
	private static final String SHOWS        = "town_shows";
	private static final String PAGES        = "town_pages";
	private static final String TITHE_DAY    = "town_tithe_day";
	private static final String SONG_DAY     = "town_song_day";
	private static final String KEEPSAKE_DAY = "town_keepsake_day";
	private static final String LESSONS      = "town_lessons";
	private static final String BET_DAY      = "town_bet_day";
	private static final String BETS         = "town_bets";
	private static final String BOUNTY_DAY   = "town_bounty_day";
	private static final String BOUNTY_TARGET = "town_bounty";
	private static final String REWARD       = "town_bounty_reward";
	private static final String SERVICES     = "town_services";

	public static void storeInBundle( Bundle bundle ){
		bundle.put( MEAL_DAY, mealDay );
		bundle.put( MEALS, mealsToday );
		bundle.put( SHOW_DAY, showDay );
		bundle.put( SHOWS, showsToday );
		bundle.put( PAGES, pagesBought );
		bundle.put( TITHE_DAY, titheDay );
		bundle.put( SONG_DAY, songDay );
		bundle.put( KEEPSAKE_DAY, keepsakeDay );
		bundle.put( LESSONS, lessonsTaken );
		bundle.put( BET_DAY, betDay );
		bundle.put( BETS, betsToday );
		bundle.put( BOUNTY_DAY, bountyDay );
		if (bountyTarget != null) bundle.put( BOUNTY_TARGET, bountyTarget );
		bundle.put( REWARD, bountyReward );
		bundle.put( SERVICES, servicesUsed );
	}

	@SuppressWarnings("unchecked")
	public static void restoreFromBundle( Bundle bundle ){
		reset();
		if (!bundle.contains( MEAL_DAY )) return; //a save from before the town kept books
		mealDay     = bundle.getInt( MEAL_DAY );
		mealsToday  = bundle.getInt( MEALS );
		showDay     = bundle.getInt( SHOW_DAY );
		showsToday  = bundle.getInt( SHOWS );
		pagesBought = bundle.getInt( PAGES );
		titheDay    = bundle.getInt( TITHE_DAY );
		songDay     = bundle.getInt( SONG_DAY );
		keepsakeDay = bundle.getInt( KEEPSAKE_DAY );
		lessonsTaken = bundle.getInt( LESSONS );
		betDay      = bundle.getInt( BET_DAY );
		betsToday   = bundle.getInt( BETS );
		bountyDay   = bundle.getInt( BOUNTY_DAY );
		bountyTarget = bundle.contains( BOUNTY_TARGET ) ? (Class<? extends Mob>) bundle.getClass( BOUNTY_TARGET ) : null;
		bountyReward = bundle.getInt( REWARD );
		servicesUsed = bundle.getInt( SERVICES );
	}
}

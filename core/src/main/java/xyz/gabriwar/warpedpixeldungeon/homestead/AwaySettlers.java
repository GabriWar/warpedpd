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


package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;
import java.util.List;

/**
 * What happened at a home while the hero was away, SETTLED WHEN HE COMES BACK: nothing of the world
 * runs where he is not, so a raid, the pests in the fields or a storm on the roof are worked out
 * on his return, from the home and the days he was gone. The hero is AT a home when he stands on
 * its slice within REACH cells of its hearthstone; every world turn he is, the home is marked
 * visited (Home.visited). Coming back after at least one full day away (a whole
 * DayNightCycle.FULL_CYCLE of the world's turns, WorldClock.turn), every SETTLER is called with the
 * home and the whole days away, and the lines they return are written in that home's ledger
 * (HomesteadWorld.Home.note) and told to the hero.
 *
 * A settler must be a pure function of the world's seed, the home and the days (and the day: it
 * may read home.lastVisitDay, still the day he left, and WorldClock.day()), so a reload settles
 * the same way, and it changes the world itself (takes a chest's things, tramples a crop) before
 * it returns its lines. The raids keep each absence's loss to one chest's contents at most.
 *
 * Settlers are looked up by class name from PROVIDERS (a missing one is skipped), each a public
 * class with a public no-argument constructor implementing Settler; one made in code (a test, a
 * debug scene) is added with register. Only the host settles (Homestead.worldTurn), and only for
 * his own hero.
 *
 * <pre>
 * public class RaidSettler implements AwaySettlers.Settler {
 *     public List<String> settle( HomesteadWorld.Home home, int daysAway ){
 *         ...roll each night away from (seed, home, night)...
 *         return Collections.singletonList( Messages.get( this, "raided", lost ) );
 *     }
 * }
 * </pre>
 */
public final class AwaySettlers {

	private AwaySettlers(){}

	/** How near its hearthstone the hero is at a home, in cells (the larger of the two steps). */
	public static final int REACH = HomeAnchors.RADIUS;

	/** A part of the homesteads that settles what happened at a home while the hero was away. */
	public interface Settler {
		/** What happened at the home over `daysAway` whole days (at least 1): a line for the ledger
		 *  each, none (empty or null) for nothing. */
		List<String> settle( HomesteadWorld.Home home, int daysAway );
	}

	private static final String PKG = "xyz.gabriwar.warpedpixeldungeon.homestead.";

	/** The settlers, by class name, in the order they settle. */
	public static final String[] PROVIDERS = {
			PKG + "threats.ThreatSettler",   //threats: raids and break-ins, a chest's worth at most
			PKG + "farming.FarmSettler"      //farming: pests and frost in the fields
	};

	private static final Providers<Settler> providers = new Providers<>( Settler.class, PROVIDERS );

	/** Adds a settler made in code (a test, a debug scene). */
	public static void register( Settler s ){
		providers.add( s );
	}

	public static void unregister( Settler s ){
		providers.remove( s );
	}

	/**
	 * Once a world turn on a slice (Homestead.worldTurn, the host's): every home of the slice the
	 * hero is at is visited, and one he comes back to after a full day or more away is settled
	 * first and the hero told what happened.
	 */
	public static void visit( OverworldLevel level ){
		Hero hero = Dungeon.hero;
		if (hero == null || level.isMirror() || Dungeon.level != level || !hero.isAlive()) return;
		int depth = WorldLayers.depthOf( level.altitude() );
		int w = level.width();
		int hx = level.worldX + hero.pos % w, hy = level.worldY + hero.pos / w;
		int now = WorldClock.turn();
		for (HomesteadWorld.Home home : HomesteadWorld.homes()){
			if (home.depth != depth || Math.abs( home.wx - hx ) > REACH || Math.abs( home.wy - hy ) > REACH) continue;
			int days = Math.floorDiv( now - home.lastVisitTurn, DayNightCycle.FULL_CYCLE );
			if (days >= 1){
				List<String> lines = settle( home, days );
				String one = days == 1 ? "_one" : "";
				if (lines.isEmpty()){
					GLog.i( Messages.get( AwaySettlers.class, "back_quiet" + one, home.title(), days ) );
				} else {
					GLog.w( Messages.get( AwaySettlers.class, "back_news" + one, home.title(), days ) );
					for (String line : lines) GLog.w( line );
				}
			}
			home.visited();
		}
	}

	/** Settles a home for `daysAway` whole days away: every settler's lines, written in its ledger
	 *  under today and returned. */
	public static List<String> settle( HomesteadWorld.Home home, int daysAway ){
		ArrayList<String> out = new ArrayList<>();
		for (Settler s : providers.all()){
			List<String> lines = s.settle( home, daysAway );
			if (lines == null) continue;
			for (String line : lines) if (line != null && !line.isEmpty()) out.add( line );
		}
		int day = WorldClock.day();
		for (String line : out) home.note( day, line );
		return out;
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.homestead.farming;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.AwaySettlers;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What became of a home's fields while the hero was away (AwaySettlers), settled when he is back:
 * every crop within AREA cells of the hearthstone is brought up to date (Farming.tend: its growth
 * through the days he was gone, and the frost of any winter day for one in the open air: no
 * glass, no room round it, a sky over it), then for every night that went by outside winter the
 * pests eat one crop that no fence shuts in - which one is the night's die (the world's seed,
 * the home, the day), so a reload settles the same.
 * The crops of such a home wait for this (Farming.waiting) rather than catch up by themselves,
 * unless the hero comes near them first (pending).
 */
public class FarmSettler implements AwaySettlers.Settler {

	/** How far from its hearthstone a home's fields reach, in cells (the larger of the two steps). */
	public static final int AREA = 20;

	@Override
	public List<String> settle( HomesteadWorld.Home home, int daysAway ){
		if (!(Dungeon.level instanceof OverworldLevel)) return null;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		FarmSlice f = Farming.fields( ow );
		if (f == null || WorldLayers.depthOf( ow.altitude() ) != home.depth) return null;

		ArrayList<Crop> area = new ArrayList<>();
		for (Crop c : f.in( home.wx - AREA, home.wy - AREA, home.wx + AREA + 1, home.wy + AREA + 1 )){
			if (ow.localCell( c.wx, c.wy ) != -1) area.add( c );
		}
		if (area.isEmpty()) return null;
		//the same order every time: by world cell
		Collections.sort( area, ( a, b ) -> Long.compare( OverworldLevel.worldKey( a.wx, a.wy ), OverworldLevel.worldKey( b.wx, b.wy ) ) );

		int frost = 0, lost = 0;
		ArrayList<Crop> standing = new ArrayList<>();
		for (Crop c : area){
			int cell = ow.localCell( c.wx, c.wy );
			Farming.Outcome o = Farming.tend( ow, cell, c, false );
			if (o == Farming.Outcome.GROWING){
				standing.add( c );
				continue;
			}
			if (o == Farming.Outcome.FROSTED) frost++;
			else lost++;
			Farming.dies( ow, cell, c, o, false );
		}

		int eaten = 0, today = WorldClock.day();
		for (int d = Math.max( home.lastVisitDay + 1, today - daysAway ); d <= today; d++){
			if (GameCalendar.seasonOfDay( d ) == GameCalendar.Season.WINTER) continue;
			ArrayList<Crop> open = new ArrayList<>();
			for (Crop c : standing){
				if (!Farming.fenced( ow, ow.localCell( c.wx, c.wy ) )) open.add( c );
			}
			if (open.isEmpty()) break;
			Crop c = open.get( FarmRules.pick( ow.worldSeed(), OverworldLevel.worldKey( home.wx, home.wy ), d, open.size() ) );
			standing.remove( c );
			Farming.dies( ow, ow.localCell( c.wx, c.wy ), c, Farming.Outcome.EATEN, false );
			eaten++;
		}

		ArrayList<String> lines = new ArrayList<>();
		if (frost > 0) lines.add( Messages.get( this, frost == 1 ? "frost_one" : "frost", frost ) );
		if (eaten > 0) lines.add( Messages.get( this, eaten == 1 ? "pests_one" : "pests", eaten ) );
		if (lost > 0) lines.add( Messages.get( this, lost == 1 ? "withered_one" : "withered", lost ) );
		return lines;
	}

	/**
	 * Is a world cell of this slice within AREA of a home the hero has been away from for a full
	 * day or more (it will be settled when he is back at it), and the hero not within AREA of the
	 * cell himself? The settling waits for him to come within AwaySettlers.REACH of the hearthstone;
	 * a crop he walks up to before that catches up by itself (Farming.tend: the same growth and the
	 * same frost), so it never stands still in front of him. The pests are still the settling's.
	 */
	public static boolean pending( OverworldLevel ow, int wx, int wy ){
		Hero hero = Dungeon.hero;
		if (hero != null && Dungeon.level == ow && hero.pos >= 0 && hero.pos < ow.length()){
			int w = ow.width();
			if (Math.abs( ow.worldX + hero.pos % w - wx ) <= AREA && Math.abs( ow.worldY + hero.pos / w - wy ) <= AREA) return false;
		}
		int depth = WorldLayers.depthOf( ow.altitude() ), now = WorldClock.turn();
		for (HomesteadWorld.Home home : HomesteadWorld.homes()){
			if (home.depth != depth || Math.abs( home.wx - wx ) > AREA || Math.abs( home.wy - wy ) > AREA) continue;
			if (now - home.lastVisitTurn >= DayNightCycle.FULL_CYCLE) return true;
		}
		return false;
	}
}

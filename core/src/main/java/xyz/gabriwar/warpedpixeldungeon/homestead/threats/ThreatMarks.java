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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.MapMarks;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.TravelShare;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.List;

/**
 * The world map's pin on a home under attack (travel/MapMarks, loaded by this name): a blinking red
 * dot on every home a siege or a raid is under way at, named for what is there. What is under way
 * is the host's (ThreatWorld); a co-op guest's map reads the host's shipped copy (TravelShare.threat).
 */
public class ThreatMarks implements MapMarks.Provider {

	/** No siege or raid under way (threatOf). */
	public static final int NONE = -1;

	/** The red of a home under attack. */
	public static final int COLOR = 0xFF3333;

	@Override
	public void marks( int depth, List<MapMarks.Mark> out ){
		for (HomesteadWorld.Home h : TravelShare.homes()){
			int kind = TravelShare.threat( h );
			if (kind == NONE) continue;
			String name = Messages.get( this, kind == ThreatWorld.Event.SIEGE ? "siege" : "raid", h.title() );
			out.add( new MapMarks.Mark( MapMarks.Icon.DOT, h.depth, h.wx, h.wy, COLOR, name, true,
					h.depth == depth ? 1f : MapMarks.OTHER_SLICE, true ) );
		}
	}

	/** Host: what is under way at a home now, a siege before a raid (ThreatWorld.Event.SIEGE or
	 *  RAID), or NONE. */
	public static int threatOf( HomesteadWorld.Home home ){
		ThreatWorld world = ThreatWorld.get();
		if (world == null) return NONE;
		int kind = NONE;
		for (ThreatWorld.Event e : world.events( home )){
			if (e.came == 0) continue;
			if (e.kind == ThreatWorld.Event.SIEGE) return e.kind;
			kind = e.kind;
		}
		return kind;
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.homestead.stations;

import xyz.gabriwar.warpedpixeldungeon.homestead.RoomTraits;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

/**
 * The WORKSHOP (RoomTraits' provider): a room with a workbench and at least one other station in
 * it. Every station there works HomesteadStation.WORKSHOP_SPEED times as fast. Read off the slice's
 * register of stations (StationSlice.workshop), so it costs a look at each of the window's few
 * stations, not at every floor cell.
 */
public class StationRooms implements RoomTraits.Provider {

	/** The workshop's label (RoomTraits.Traits.is). */
	public static final String WORKSHOP = "workshop";

	@Override
	public void describe( OverworldLevel level, Rooms.Room room, RoomTraits.Traits out ){
		StationSlice s = StationSlice.of( level );
		if (s == null) return;
		if (s.workshop( level, room )){
			out.label( WORKSHOP, Messages.get( this, "workshop" ) );
			out.line( Messages.get( this, "workshop_line", Math.round( (HomesteadStation.WORKSHOP_SPEED - 1f) * 100 ) ) );
		}
	}
}

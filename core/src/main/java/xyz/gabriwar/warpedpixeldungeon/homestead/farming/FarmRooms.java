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

import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.RoomTraits;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.RoofTiles;

/**
 * The farming's room trait (RoomTraits): a GREENHOUSE is a room with glass over at least half of
 * its floor. Every crop in it grows at summer's pace all year and never freezes (Farming), glass
 * over its own cell or not.
 */
public class FarmRooms implements RoomTraits.Provider {

	/** The greenhouse label: what Farming.greenhouse asks a room for. */
	public static final String GREENHOUSE = "greenhouse";

	@Override
	public void describe( OverworldLevel level, Rooms.Room room, RoomTraits.Traits out ){
		int glass = 0;
		for (int c : room.cells) if (Homestead.roofAt( level, c ) == RoofTiles.GLASS) glass++;
		if (glass * 2 < room.size()) return;
		out.label( GREENHOUSE, Messages.get( this, "greenhouse" ) );
		out.line( Messages.get( this, "greenhouse_line" ) );
	}
}

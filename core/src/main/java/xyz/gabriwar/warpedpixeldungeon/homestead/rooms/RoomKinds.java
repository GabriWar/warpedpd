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

package xyz.gabriwar.warpedpixeldungeon.homestead.rooms;

import xyz.gabriwar.warpedpixeldungeon.homestead.RoomTraits;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

/**
 * The rooms' own traits (RoomTraits.PROVIDERS names it first): every type of RoomTypes.OWN a room is
 * as a label - the first names it - and a line telling what it does, and its comfort (Comfort) as a
 * word and a score. What the types do is asked for where it happens (a bed, a meal, a chest), so
 * they add no per-turn effect, and the world turn's asking (Traits.wantsText false) costs nothing.
 * The workshop and the greenhouse are the stations' and the farming's own traits.
 */
public class RoomKinds implements RoomTraits.Provider {

	@Override
	public void describe( OverworldLevel level, Rooms.Room room, RoomTraits.Traits out ){
		if (!out.wantsText()) return;
		RoomTypes.Contents contents = RoomTypes.Contents.of( level, room );
		for (String type : RoomTypes.of( contents )){
			out.label( type, Messages.get( RoomKinds.class, type ) );
			out.line( Messages.get( RoomKinds.class, type + "_line" ) );
		}
		int comfort = Comfort.score( level, room, contents );
		out.line( Messages.get( RoomKinds.class, "comfort", Comfort.levelOf( comfort ).title(), comfort, Comfort.MAX ) );
	}
}

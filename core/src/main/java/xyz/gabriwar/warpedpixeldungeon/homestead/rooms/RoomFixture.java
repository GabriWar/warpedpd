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

/**
 * What a homestead actor is to the room it stands in (RoomTypes): a mob (a stove, a piece of
 * furniture) implementing it says so itself. The other parts' actors are recognised without it
 * too - by their build piece's id or their class's name (RoomTypes.fixtureOf) - so this is for a
 * fixture whose name says nothing: an altar, a decoration, a second kind of stove. (A workshop's
 * stations are the stations' own to count: stations.StationRooms.)
 *
 * <pre>
 * public class CookingPot extends NPC implements HomesteadActor, RoomFixture {
 *     public Kind roomFixture(){ return Kind.COOKING_POT; }
 * }
 * </pre>
 */
public interface RoomFixture {

	enum Kind {
		/** a bed: makes a bedroom (every Bedroll is one already) */
		BED,
		/** a hearth: makes a kitchen */
		HEARTH,
		/** a cooking pot: makes a kitchen */
		COOKING_POT,
		/** a chest: two make a storehouse (every StorageChest is one already) */
		CHEST,
		/** an altar: makes a shrine, as a statue does */
		ALTAR,
		/** a decoration: counts toward a room's comfort as a rug or a statue does */
		DECOR
	}

	/** What it is to its room. */
	Kind roomFixture();
}

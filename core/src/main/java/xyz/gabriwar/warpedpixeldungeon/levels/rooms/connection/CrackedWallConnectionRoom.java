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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.connection;

import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;

//ported from cursed-pixel-dungeon: a burnt-out passage sealed behind secret doors,
//with a bomb seeded on the level to open it up.
//cursed used its own CRACKED_SECRET door type; Warped has no equivalent, so these
//are plain HIDDEN doors.
public class CrackedWallConnectionRoom extends TunnelRoom {

	@Override
	public void paint(Level level) {
		paint( this, level, Terrain.EMBERS );
		level.addItemToSpawn( new Bomb() );

		for (Room.Door door : connected.values()) {
			door.set( Room.Door.Type.HIDDEN );
		}
	}
}

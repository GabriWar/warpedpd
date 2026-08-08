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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.temple;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.StandardRoom;
import com.watabou.utils.Point;

//Caves-side way into the Temple gauntlet (Re-ARranged port).
//Same shape as FrozenEntranceRoom / BlacksmithRoom: a BRANCH_EXIT that sets the branch.
public class TempleEntranceRoom extends StandardRoom {

	public static final int TEMPLE_BRANCH = 3;

	@Override
	public float[] sizeCatProbs() {
		return new float[]{0, 1, 0};
	}

	@Override
	public void paint(Level level) {
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );

		for (Room.Door door : connected.values()) {
			door.set( Room.Door.Type.REGULAR );
		}

		int entrance;
		do {
			entrance = level.pointToCell( random( 2 ) );
		} while (level.findMob( entrance ) != null || level.heaps.get( entrance ) != null);

		level.transitions.add(new LevelTransition(level,
				entrance,
				LevelTransition.Type.BRANCH_EXIT,
				Dungeon.depth,
				TEMPLE_BRANCH,
				LevelTransition.Type.BRANCH_ENTRANCE));

		Painter.set( level, entrance, Terrain.EXIT );

		//worn flagstones lead up to the doorway
		for (Point p : getPoints()) {
			int cell = level.pointToCell( p );
			if (level.map[cell] == Terrain.EMPTY && level.distance( cell, entrance ) <= 2) {
				Painter.set( level, p, Terrain.EMPTY_SP );
			}
		}
	}

	@Override
	public int maxConnections(int direction) {
		if (direction == ALL) return 2;
		return super.maxConnections(direction);
	}

	@Override
	public boolean canMerge(Level l, Room other, Point p, int mergeTerrain) {
		return false;
	}
}

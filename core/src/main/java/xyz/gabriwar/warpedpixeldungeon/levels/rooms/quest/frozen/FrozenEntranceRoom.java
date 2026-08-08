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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.quest.frozen;

import xyz.gabriwar.warpedpixeldungeon.levels.FrozenLevel;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.StandardRoom;
import com.watabou.utils.Point;

//Halls-side mouth of the frozen branch: a rimed opening in the wall leading down
//into the ice. Mirrors BlacksmithRoom, which is how the mining branch is entered.
public class FrozenEntranceRoom extends StandardRoom {

	public static final int FROZEN_BRANCH = 2;

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
				FrozenLevel.FIRST_DEPTH,
				FROZEN_BRANCH,
				LevelTransition.Type.BRANCH_ENTRANCE));

		Painter.set( level, entrance, Terrain.EXIT );

		//a little frost creeps out around the opening
		for (Point p : getPoints()) {
			int cell = level.pointToCell( p );
			if (level.map[cell] == Terrain.EMPTY && level.distance( cell, entrance ) <= 2) {
				Painter.set( level, p, Terrain.EMPTY_DECO );
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

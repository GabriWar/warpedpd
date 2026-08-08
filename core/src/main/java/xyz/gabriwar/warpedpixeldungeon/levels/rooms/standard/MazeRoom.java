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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard;

import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Maze;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;

//ported from cursed-pixel-dungeon: a standard room filled with a proper maze.
//cursed left an upstream TODO about where this belongs; here it is a standard room.
public class MazeRoom extends StandardRoom {

	@Override
	public float[] sizeCatProbs() {
		//mazes need room to be a maze
		return new float[]{0, 3, 1};
	}

	@Override
	public void paint( Level level ) {
		Painter.fill( level, this, 1, Terrain.EMPTY );

		Maze.allowDiagonals = false;
		boolean[][] maze = Maze.generate( this );

		for (int x = 0; x < maze.length; x++) {
			for (int y = 0; y < maze[0].length; y++) {
				if (maze[x][y] == Maze.FILLED) {
					Painter.fill( level, x + left, y + top, 1, 1, Terrain.WALL );
				}
			}
		}

		//carve the doorways back open so the maze is always enterable
		for (Door door : connected.values()) {
			door.set( Door.Type.REGULAR );
			Painter.drawInside( level, this, door, 1, Terrain.EMPTY );
		}
	}
}

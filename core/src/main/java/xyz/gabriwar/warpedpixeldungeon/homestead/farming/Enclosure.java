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

import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;

import java.util.HashSet;

/**
 * Is a cell shut in by what the hero built: a field behind its fence (no pest gets at it), a pen
 * (an animal in it stays). The ground an animal could walk from the cell is flooded the way it
 * walks, diagonals and all, through every cell it could step on or into (water and open doors
 * too); the area is closed when the flood runs out within MAX_CELLS cells, short of the window's
 * rim, and at least one of the cells that stopped it is built (a fence, a palisade, a gate, a built
 * wall, window or barred door) - a pocket the land alone closes is nobody's pen.
 *
 * A gate standing open is a way out, unless `gatesShut`: an animal already penned keeps to its
 * pen while the hero holds the gate (it never steps into a gateway, Roping), so its pen is looked
 * at as if the gate were shut.
 */
public final class Enclosure {

	private Enclosure(){}

	/** The largest area a fence closes, in cells. */
	public static final int MAX_CELLS = 400;

	//the flood's marks (a cell is seen this flood when its stamp is the flood's number) and its
	//queue, kept between floods: the beasts ask every few turns, on the actor thread
	private static int[] stamp = new int[0];
	private static int flood = 0;
	private static final int[] queue = new int[MAX_CELLS];

	/** Is the cell inside a closed area (see the class)? */
	public static synchronized boolean closed( Level level, int cell, boolean gatesShut ){
		return fill( level, cell, gatesShut ) > 0;
	}

	/** The cells of the closed area round a cell, null when it is open (or the cell is no ground). */
	public static synchronized HashSet<Integer> area( Level level, int cell, boolean gatesShut ){
		int n = fill( level, cell, gatesShut );
		if (n <= 0) return null;
		HashSet<Integer> out = new HashSet<>();
		for (int i = 0; i < n; i++) out.add( queue[i] );
		return out;
	}

	//floods from a cell: the closed area's size (its cells first in the queue), 0 when it is open
	private static int fill( Level level, int cell, boolean gatesShut ){
		if (cell < 0 || cell >= level.length() || !way( level, cell, gatesShut )) return 0;
		if (stamp.length != level.length()){
			stamp = new int[level.length()];
			flood = 0;
		}
		if (++flood == Integer.MAX_VALUE){
			java.util.Arrays.fill( stamp, 0 );
			flood = 1;
		}
		int w = level.width(), h = level.height();
		int head = 0, tail = 0;
		queue[tail++] = cell;
		stamp[cell] = flood;
		boolean built = false;
		while (head < tail){
			int c = queue[head++];
			int x = c % w, y = c / w;
			//the window's rim: the world goes on past it
			if (x <= 0 || y <= 0 || x >= w - 1 || y >= h - 1) return 0;
			for (int n : PathFinder.NEIGHBOURS8){
				int m = c + n;
				if (stamp[m] == flood) continue;
				stamp[m] = flood;
				if (way( level, m, gatesShut )){
					if (tail >= MAX_CELLS) return 0;
					queue[tail++] = m;
				} else if (!built && barrier( level.map[m] )){
					built = true;
				}
			}
		}
		return built ? tail : 0;
	}

	//a cell an animal could step on: walkable ground or water, an open door; a shut gate is no way,
	//and with the gates held shut no gateway is
	private static boolean way( Level level, int cell, boolean gatesShut ){
		int t = level.map[cell];
		if (gatesShut && t == Terrain.FENCE_GATE_OPEN) return false;
		return level.passable[cell] || level.avoid[cell];
	}

	//what the hero built to keep something in or out
	private static boolean barrier( int t ){
		return t == Terrain.FENCE || t == Terrain.PALISADE || BuiltTerrain.gate( t ) || t == Terrain.BUILT_DOOR_BARRED
				|| BuiltTerrain.wallLike( t );
	}
}

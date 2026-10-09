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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;

/**
 * Which cells changed their terrain, as the render side sees it: grass burnt, water frozen, a wall
 * broken, a door opened. The scene says a cell may have changed (GameScene.updateMap(cell), from any
 * thread: a co-op guest's terrain changes come the same way); once a frame, on the render thread,
 * the cells whose terrain really differs from the last seen are handed to every area
 * (FxModules.terrainChanged) as old and new. A whole map redrawn (a level made, a rebase's slide)
 * is taken in silently. At most QUEUE cells wait between two frames; past them the oldest are
 * dropped (their change is still taken in, unannounced, at the next whole look).
 */
public final class TerrainWatch {

	private TerrainWatch(){}

	public static final int QUEUE = 256;

	private static final Object LOCK = new Object();

	//the terrain last seen, the level it is of
	private static int[] seen;
	private static Level of;

	//the cells waiting
	private static final int[] waiting = new int[QUEUE];
	private static int head, count;

	/** A cell's terrain may have changed. Any thread. */
	public static void changed( int cell ){
		synchronized (LOCK){
			if (count == QUEUE){
				head = (head + 1) % QUEUE;
				count--;
			}
			waiting[(head + count) % QUEUE] = cell;
			count++;
		}
	}

	/** Takes the level's whole map in as seen, announcing nothing: a new level, a redrawn map, a
	 *  window slid. Any thread. */
	public static void resync(){
		Level level = Dungeon.level;
		synchronized (LOCK){
			count = 0;
			if (level == null || level.map == null){
				seen = null;
				of = null;
				return;
			}
			if (seen == null || seen.length != level.map.length) seen = new int[level.map.length];
			System.arraycopy( level.map, 0, seen, 0, level.map.length );
			of = level;
		}
	}

	/** Once a frame, on the render thread: every waiting cell whose terrain differs from the last
	 *  seen is taken in and announced to the areas (outside the lock); how many were. */
	public static int drain(){
		Level level = Dungeon.level;
		if (level == null || level.map == null) return 0;
		boolean fresh;
		synchronized (LOCK){
			fresh = level != of || seen == null || seen.length != level.map.length;
		}
		if (fresh){
			resync();
			return 0;
		}
		int announced = 0;
		while (true){
			int cell, was, now;
			synchronized (LOCK){
				if (count == 0 || seen == null) return announced;
				cell = waiting[head];
				head = (head + 1) % QUEUE;
				count--;
				if (cell < 0 || cell >= seen.length) continue;
				was = seen[cell];
				now = level.map[cell];
				if (was == now) continue;
				seen[cell] = now;
			}
			FxModules.terrainChanged( cell, was, now );
			announced++;
		}
	}

	/** The terrain last seen at a cell, -1 off the map or before anything was seen. */
	public static int seen( int cell ){
		synchronized (LOCK){
			return seen == null || cell < 0 || cell >= seen.length ? -1 : seen[cell];
		}
	}
}

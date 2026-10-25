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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

/**
 * Roads are quicker going: out in the world (any slice) the hero moves SPEED_BONUS faster on a
 * cobbled path he laid (PATH_STONE), a plank bridge he built (BRIDGE_PLANK), and the world's own
 * roads and village footpaths (DIRT_PATH) and their bridges (BRIDGE) - so a road to a home is worth
 * building. The tunnels through the mountains are floored with the same path (WindowGenerator), and
 * look it: they are as quick going, on purpose, so the bonus is always where a path is drawn. Hero.speed multiplies by speedFactor, as it does by every other speed modifier; the
 * bonus is for the cell he stands on after the step, so each step onto a road is the quick one.
 * Nothing changes in the dungeon, and not while he flies over the road.
 */
public final class Paths {

	private Paths(){}

	/** How much faster the going is on a road: a tenth. */
	public static final float SPEED_BONUS = 1.1f;
	/** ...and how much slower in mud (a flood's, levels/overworld/FloodEvent): a step takes a third
	 *  longer, on a road under the water too. */
	public static final float MUD_SLOW = 0.75f;

	/** Is this terrain a road, a path or a bridge (the world's or one built)? */
	public static boolean isPath( int terrain ){
		return terrain == Terrain.PATH_STONE || terrain == Terrain.BRIDGE_PLANK
				|| terrain == Terrain.DIRT_PATH || terrain == Terrain.BRIDGE;
	}

	/** SPEED_BONUS for a walker on a road of the world's slices, MUD_SLOW in mud, 1 anywhere else. */
	public static float speedFactor( Char ch ){
		Level level = Dungeon.level;
		if (!(level instanceof OverworldLevel) || ch == null || ch.flying) return 1f;
		int cell = ch.pos;
		if (cell < 0 || cell >= level.length()) return 1f;
		if (level.map[cell] == Terrain.MUD) return MUD_SLOW;
		return isPath( level.map[cell] ) ? SPEED_BONUS : 1f;
	}
}

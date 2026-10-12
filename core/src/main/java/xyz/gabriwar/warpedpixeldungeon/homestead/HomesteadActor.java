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

package xyz.gabriwar.warpedpixeldungeon.homestead;

/**
 * A mob that belongs to a homestead: a station, a chest, a crop, a penned or roped animal. The
 * overworld keeps it for good (OverworldLevel.keepsForever): never pruned from the parked store,
 * never culled with the hour's wildlife, carried through every rebase and jump of the window like
 * the settlements' people, and put back on its own cell when the window comes back over it.
 *
 * A mob that is only sometimes the homestead's (an animal, until it is roped) answers
 * {@link #homesteadKept()} with the moment's truth.
 */
public interface HomesteadActor {

	/** Is it the homestead's right now (kept for good)? */
	default boolean homesteadKept(){
		return true;
	}
}

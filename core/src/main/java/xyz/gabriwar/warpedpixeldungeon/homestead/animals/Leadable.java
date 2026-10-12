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

package xyz.gabriwar.warpedpixeldungeon.homestead.animals;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor;

/**
 * A peaceful beast of the world the hero can put a rope on (Roping): the deer, the goat, the
 * bunny. Its class hands its turn, a tap on it and whether a tap talks to it over to Animals first.
 * It is the homestead's (kept for good, never culled with the wildlife) while it is roped, tied
 * to a post or penned.
 */
public interface Leadable extends HomesteadActor {

	@Override
	default boolean homesteadKept(){
		return Roping.held( (Mob) this );
	}
}

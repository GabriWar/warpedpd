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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Pests;

/**
 * Where a Leadable beast's own class hands over to the homesteads: its turn (a roped, tied or
 * penned one is Roping's, a grazer off to a crop at night is Pests'), a tap on it, and whether a
 * tap talks to it rather than strikes it (the deer and the goat are struck, unless held).
 */
public final class Animals {

	private Animals(){}

	/** Its turn, from the top of its act(): the time the homesteads spent of it, 0 when it is
	 *  its own (paralysed or asleep it always is). */
	public static float act( Mob m ){
		if (m.paralysed > 0 || m.state == m.SLEEPING) return 0f;
		float t = Roping.act( m );
		return t > 0f ? t : Pests.act( m );
	}

	/** Does a tap talk to it: when the homestead holds it (its menu). */
	public static boolean shouldInteract( Mob m ){
		return Roping.held( m );
	}

	/** A tap on it: its menu when the homestead holds it (true); false for its own ways. */
	public static boolean interact( Mob m, Char c ){
		return Roping.interact( m, c );
	}
}

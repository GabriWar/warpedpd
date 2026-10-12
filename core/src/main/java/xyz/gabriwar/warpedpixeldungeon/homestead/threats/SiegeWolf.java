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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

/**
 * A wolf of a siege's pack (WolfSiege): the forest's brown wolf (SiegeBrownWolf) or the snow's grey
 * one (SiegeGrayWolf), each its own kind's stats and ways with the siege's on top. WolfSiege.act
 * decides its turn; the wolf carries the moves out with what only a mob may do.
 */
public interface SiegeWolf {

	/** The siege it came with. */
	WolfSiege.Pack pack();

	/** A turn as any wolf of its kind: the hunt, its pack's call (the kind's own act). */
	boolean wolfAct();

	/** A step onto a cell beside it. */
	boolean wolfStep( int cell );

	/** A step away from a cell, its turn spent: false (no turn spent) when it has nowhere to go. */
	boolean wolfAway( int from );

	/** A blow at a fence or a gate (Mob.obeyBreaker). */
	boolean wolfBite( Breaker.Order bite );

	/** A turn spent where it stands. */
	boolean wolfWait();

	/** Its sight worked out for this turn. */
	void wolfLook();
}

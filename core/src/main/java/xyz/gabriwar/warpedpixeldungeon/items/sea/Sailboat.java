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

package xyz.gabriwar.warpedpixeldungeon.items.sea;

import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

/** A planked hull with a mast and a hide sail, built at a fired forge (homestead/stations/
 *  StationRecipes) or bought from a village's fisher: quicker, and sturdier, than a raft. */
public class Sailboat extends Boat {

	{
		image = ItemSpriteSheet.SAILBOAT;
	}

	@Override
	public Kind kind(){
		return Kind.SAILBOAT;
	}

	//about twice the worth of what it is built from, as the raft's is (12 planks, 3 rope, 2 iron
	//fittings, 2 hides: 64): within reach on a fisher's shelf, and no gold mine to build and sell
	@Override
	protected int worth(){
		return 120;
	}
}

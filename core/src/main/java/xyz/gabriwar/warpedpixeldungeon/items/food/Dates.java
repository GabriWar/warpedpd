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

package xyz.gabriwar.warpedpixeldungeon.items.food;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

/**
 * Dates from an oasis' palms (levels/overworld/DrylandsLife lays them at the palms' feet every
 * other day; the oasis trader sells them): sweet, light, eaten in a moment.
 */
public class Dates extends Food {

	{
		image = ItemSpriteSheet.DATES;
		energy = Hunger.HUNGRY / 3f;   //100 food value, as a berry
		bones = false;
	}

	@Override
	protected float eatingTime(){
		return 1;
	}

	@Override
	public int value() {
		return 6 * quantity;
	}
}

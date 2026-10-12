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

package xyz.gabriwar.warpedpixeldungeon.items.bags;

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

/**
 * The builder's satchel: a bag that holds the homesteads' building materials and nothing else
 * (items/materials), so logs and stone do not fill the backpack. Building spends from it first
 * (Material.spend).
 */
public class BuildersSatchel extends Bag {

	{
		image = ItemSpriteSheet.BUILDERS_SATCHEL;
	}

	@Override
	public boolean canHold( Item item ){
		return item instanceof Material && super.canHold( item );
	}

	@Override
	public int capacity(){
		//one stack of each material, with room to spare
		return 24;
	}

	@Override
	public int value(){
		return 40;
	}
}

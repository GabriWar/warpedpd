/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

import xyz.gabriwar.warpedpixeldungeon.items.Amulet;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfDead;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfLife;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfTranscendence;
import xyz.gabriwar.warpedpixeldungeon.items.CavesKey;
import xyz.gabriwar.warpedpixeldungeon.items.CityKey;
import xyz.gabriwar.warpedpixeldungeon.items.HallsKey;
import xyz.gabriwar.warpedpixeldungeon.items.SewersKey;
import xyz.gabriwar.warpedpixeldungeon.items.TenguKey;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.keys.Key;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class KeyRing extends Bag {

	{
		image = ItemSpriteSheet.KEYRING;
	}

	@Override
	public boolean canHold( Item item ) {
		if (item instanceof Key
				|| item instanceof Amulet
				|| item instanceof SewersKey || item instanceof TenguKey
				|| item instanceof CavesKey || item instanceof CityKey
				|| item instanceof HallsKey
				|| item instanceof BookOfDead || item instanceof BookOfLife
				|| item instanceof BookOfTranscendence){
			return super.canHold(item);
		} else {
			return false;
		}
	}

	public int capacity(){
		return 42;
	}

	@Override
	public int value() {
		return 50;
	}
}

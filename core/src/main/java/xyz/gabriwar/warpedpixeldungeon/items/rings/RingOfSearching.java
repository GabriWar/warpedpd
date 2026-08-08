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

package xyz.gabriwar.warpedpixeldungeon.items.rings;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

//ported from Unleashed PD: brings out-of-place things into focus, widening searches
public class RingOfSearching extends Ring {

	{
		icon = ItemSpriteSheet.Icons.RING_SEARCHING;
		buffClass = EasySearch.class;
	}

	public String statsInfo() {
		if (isIdentified()){
			String info = Messages.get(this, "stats", searchDistanceBonus(soloBuffedBonus()));
			if (isEquipped(Dungeon.hero) && soloBuffedBonus() != combinedBuffedBonus(Dungeon.hero)){
				info += "\n\n" + Messages.get(this, "combined_stats", searchDistanceBonus(combinedBuffedBonus(Dungeon.hero)));
			}
			return info;
		} else {
			return Messages.get(this, "typical_stats", 1);
		}
	}

	public String upgradeStat1(int level){
		if (cursed && cursedKnown) level = Math.min(-1, level-3);
		return Integer.toString(searchDistanceBonus(level+1));
	}

	//every 2 ring levels widens the intentional-search radius by 1
	private static int searchDistanceBonus(int bonus){
		return bonus > 0 ? (1 + (bonus - 1) / 2) : bonus;
	}

	public static int searchDistanceBonus( Char target ){
		return searchDistanceBonus(getBuffedBonus(target, EasySearch.class));
	}

	public class EasySearch extends RingBuff {
	}
}

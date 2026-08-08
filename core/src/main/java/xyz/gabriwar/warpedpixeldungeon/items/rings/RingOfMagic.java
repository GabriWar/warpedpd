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

package xyz.gabriwar.warpedpixeldungeon.items.rings;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class RingOfMagic extends Ring {

	{
		icon = ItemSpriteSheet.Icons.RING_ENERGY;
		buffClass = Magic.class;
	}

	public String statsInfo() {
		if (isIdentified()){
			return Messages.get(this, "stats", soloBuffedBonus() + 1);
		} else {
			return Messages.get(this, "typical_stats", 1);
		}
	}

	public String upgradeStat1(int level){
		if (cursed && cursedKnown) level = Math.min(-1, level-3);
		return "" + (level + 2);
	}

	@Override
	protected RingBuff buff( ) {
		return new Magic();
	}

	public static int manaBonus( Char target ){
		return getBuffedBonus(target, Magic.class);
	}

	public static float manaRegenMultiplier( Char target ){
		return (float)Math.pow(1.2, getBuffedBonus(target, Magic.class));
	}

	public class Magic extends RingBuff {
	}
}

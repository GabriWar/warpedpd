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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

public class Paralysis extends Weapon.Enchantment {

	private static ItemSprite.Glowing YELLOW = new ItemSprite.Glowing(0xCCAA44);

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		int level = Math.max(0, weapon.buffedLvl());

		// lvl 0 - 13%
		// lvl 1 - 22%
		// lvl 2 - 30%
		float procChance = (level + 1f) / (level + 8f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance) {
			Buff.prolong(defender,
					xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis.class,
					Random.Float(1, 1.5f + level) * power()); //scales with enchantment level
		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return YELLOW;
	}

}

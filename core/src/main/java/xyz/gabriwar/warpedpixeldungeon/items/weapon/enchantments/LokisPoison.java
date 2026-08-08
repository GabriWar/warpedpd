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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

public class LokisPoison extends Weapon.Enchantment {

	private static ItemSprite.Glowing PURPLE = new ItemSprite.Glowing(0x7700CC);

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		int level = Math.max(0, weapon.buffedLvl());
		int distance = 1 + level * 2 + level()/2; //scales with enchantment level

		// lvl 0 - 33%
		// lvl 1 - 50%
		// lvl 2 - 60%
		float procChance = (level + 1f) / (level + 3f) * procChanceMultiplier(attacker);

		for (Mob mob : Dungeon.level.mobs) {
			if (Dungeon.level.distance(attacker.pos, mob.pos) < distance
					&& mob != defender
					&& Random.Float() < procChance) {
				Buff.affect(mob,
						xyz.gabriwar.warpedpixeldungeon.actors.buffs.LokisPoison.class)
						.set(Math.round((level + 1) * power())); //scales with enchantment level
			}
		}

		if (Random.Float() < procChance) {
			Buff.affect(defender,
					xyz.gabriwar.warpedpixeldungeon.actors.buffs.LokisPoison.class)
					.set(Math.round((level + 1) * power())); //scales with enchantment level
		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return PURPLE;
	}

}

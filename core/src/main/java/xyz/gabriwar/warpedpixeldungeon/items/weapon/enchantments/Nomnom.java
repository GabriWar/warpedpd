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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

public class Nomnom extends Weapon.Enchantment {

	private static ItemSprite.Glowing RED = new ItemSprite.Glowing(0xCC2266);

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		int level = Math.max(0, weapon.buffedLvl() * 3);

		// lvl 0 - 25%
		// lvl 1 ~ 27%
		// lvl 2 ~ 29%
		float procChance = (level + 25f) / (level + 100f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance) {

			defender.damage(defender.HP, this);

			int maxValue = Math.round(damage * (level + 2) / (level + 3) * power()); //scales with enchantment level
			int effValue = Math.min(Random.IntRange(0, maxValue), attacker.HT - attacker.HP);

			if (effValue > 0) {
				attacker.HP += effValue;
				attacker.sprite.emitter().start(Speck.factory(Speck.HEALING), 0.4f, 1);
				attacker.sprite.showStatus(CharSprite.POSITIVE, Integer.toString(effValue));
			}

			Hunger hunger = attacker.buff(Hunger.class);
			if (hunger != null) {
				hunger.satisfy(maxValue);
				SpellSprite.show(attacker, SpellSprite.FOOD);
			}
		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return RED;
	}

}

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
import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Chainsaw;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

public class BuzzSaw extends Weapon.Enchantment {

	private static ItemSprite.Glowing RED = new ItemSprite.Glowing(0xBB3300);

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		Waterskin vial = Dungeon.hero.belongings.getItem(Waterskin.class);
		Chainsaw saw = Dungeon.hero.belongings.getItem(Chainsaw.class);

		if (vial == null || saw == null) {
			return damage;
		}

		int vol = vial.getVolume();
		if (vol <= 0) {
			GLog.n(Messages.get(this, "no_fuel"));
			return damage;
		}

		if (!saw.turnedOn) {
			GLog.n(Messages.get(this, "not_on"));
			return damage;
		}

		int hits = Random.Int(Math.max(1, vol / 10)) + level()/2; //scales with enchantment level
		for (int i = 1; i <= hits + 1; i++) {
			if (vial.getVolume() > 0 && saw.turnedOn) {
				vial.consumeVolume(1);
				int dmg = Math.max(1, Math.round((attacker.damageRoll() - i) * 2 * power())); //scales with enchantment level
				defender.damage(dmg, this);
				GLog.h(Messages.get(this, "vrr"));
			} else if (vial.getVolume() == 0) {
				GLog.n(Messages.get(this, "no_fuel"));
				break;
			} else {
				break;
			}
			if (!defender.isAlive()) break;
		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return RED;
	}
}

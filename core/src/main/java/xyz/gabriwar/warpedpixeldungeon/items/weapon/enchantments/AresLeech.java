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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.RelicMeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

public class AresLeech extends Weapon.Enchantment {

	private static ItemSprite.Glowing PURPLE = new ItemSprite.Glowing(0x660066);

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		if (!(weapon instanceof RelicMeleeWeapon)) {
			return damage;
		}

		RelicMeleeWeapon relic = (RelicMeleeWeapon) weapon;
		int level = Math.max(0, weapon.buffedLvl());

		int distance = 1 + level * 2 + level()/2; //scales with enchantment level
		int maxValue = Math.round((damage * (level + 2) / (level + 6)) * power()); //scales with enchantment level
		int effValue = Math.min(Random.IntRange(0, maxValue), attacker.HT - attacker.HP);

		int drains = 0;

		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (Dungeon.level.distance(attacker.pos, mob.pos) < distance
					&& mob.isAlive()
					&& mob.alignment == Char.Alignment.ENEMY) {
				if (effValue < mob.HP) {
					mob.damage(effValue, weapon);
					relic.charge++;
					drains++;
				}
			}
		}

		if (drains > 0) {
			GLog.i(Messages.get(this, "drains", drains));
		}

		if (effValue > 0) {
			attacker.HP += effValue;
			attacker.sprite.emitter().start(Speck.factory(Speck.HEALING), 0.4f, 1);
			attacker.sprite.showStatus(CharSprite.POSITIVE, Integer.toString(effValue));
		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return PURPLE;
	}
}

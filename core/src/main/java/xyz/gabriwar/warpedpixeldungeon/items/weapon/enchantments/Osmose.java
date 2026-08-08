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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite.Glowing;
import com.watabou.utils.Random;

public class Osmose extends Weapon.Enchantment {

	private static ItemSprite.Glowing BLUE = new ItemSprite.Glowing(0x2244CC);

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {

		if (!(attacker instanceof Hero)) return damage;

		Hero hero = (Hero) attacker;
		int level = Math.max(0, weapon.buffedLvl());

		// lvl 0 - 33%, lvl 1 - 43%, lvl 2 - 50%
		int maxValue = Math.round(damage * (level + 2) / (level + 6) * power()); //scales with enchantment level
		int effValue = Math.min(Random.IntRange(0, maxValue), hero.MT - hero.MP);

		if (effValue > 0) {
			hero.MP += effValue;
			attacker.sprite.showStatusWithIcon(CharSprite.POSITIVE,
					Integer.toString(effValue), FloatingText.HEALING);
		}

		return damage;
	}

	@Override
	public Glowing glowing() {
		return BLUE;
	}
}

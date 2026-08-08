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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

//ported from Unleashed PD: scourge of the undead and the demonic
public class Holy extends Weapon.Enchantment {

	private static ItemSprite.Glowing WHITE = new ItemSprite.Glowing( 0xFFFFFF );

	@Override
	public int proc( Weapon weapon, Char attacker, Char defender, int damage ) {
		int level = Math.max( 0, weapon.buffedLvl() );
		float multi = procChanceMultiplier( attacker );

		if (defender.properties().contains( Char.Property.UNDEAD )) {
			if (Random.Float() < ((level + 1f) / (level + 4f)) * multi) {
				int bonus = Math.round( Random.NormalIntRange( 0, damage + level ) * power() );
				defender.damage( bonus, this );
			}
		} else if (defender.properties().contains( Char.Property.DEMONIC )) {
			if (Random.Float() < (1f / (level + 4f)) * multi) {
				int bonus = Math.round( Random.NormalIntRange( 0, (damage + level) / 2 ) * power() );
				defender.damage( bonus, this );
			}
		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return WHITE;
	}
}

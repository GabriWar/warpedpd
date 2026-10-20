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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import com.watabou.utils.Random;

//ported from Unleashed PD: a butchering weapon.
//UL keyed this off its TYPE_ANIMAL flag, which modern SPD has no equivalent for,
//so it targets flesh-and-blood creatures instead (not undead/demonic/inorganic/plant).
public class Hunting extends Weapon.Enchantment {

	private static ItemSprite.Glowing RED = new ItemSprite.Glowing( 0xFF3333 );

	public static boolean isPrey( Char defender ) {
		return !defender.properties().contains( Char.Property.UNDEAD )
				&& !defender.properties().contains( Char.Property.DEMONIC )
				&& !defender.properties().contains( Char.Property.INORGANIC )
				&& !defender.properties().contains( Char.Property.PLANT );
	}

	@Override
	public int proc( Weapon weapon, Char attacker, Char defender, int damage ) {
		if (!isPrey( defender )) {
			return damage;
		}

		int level = Math.max( 0, weapon.buffedLvl() );

		int bonus = Math.round( Random.NormalIntRange( 0, level + 3 ) * power() );
		defender.damage( bonus, this );
		if (bonus * 2 >= Math.round( (level + 3) * power() ) && bonus > 0) EnchantFx.proc( EnchantLooks.of( getClass() ), attacker, defender, EnchantFx.strength( level(), bonus, defender.HT, power() ), level(), Quality.isVestige( weapon, this ) ? EnchantFx.ECHO : 0, ShadowRows.HUNTING );

		//butchering a kill sometimes yields meat
		if (damage + bonus >= defender.HP) {
			float meatChance = (1f / (4f * (level + 5f))) * procChanceMultiplier( attacker );
			if (Random.Float() < meatChance) {
				Dungeon.level.drop( new MysteryMeat(), defender.pos ).sprite.drop();
				ShadowRows.huntMeat( defender.pos );
			}
		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return RED;
	}
}

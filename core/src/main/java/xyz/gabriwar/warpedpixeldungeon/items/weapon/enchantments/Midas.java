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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

//ported from Unleashed PD: enemies bleed gold
public class Midas extends Weapon.Enchantment {

	private static ItemSprite.Glowing GOLDEN = new ItemSprite.Glowing( 0xFFD700 );

	@Override
	public int proc( Weapon weapon, Char attacker, Char defender, int damage ) {
		int level = Math.max( 0, weapon.buffedLvl() );

		int goldValue = 0;
		if (damage >= defender.HP) {
			//killing blow always pays out, scaled by weapon level
			goldValue = Math.round( Random.IntRange( 1 + level, 1 + level * 5 ) * power() );
		} else {
			float procChance = (1f / (level + 3f)) * procChanceMultiplier( attacker );
			if (Random.Float() < procChance) {
				goldValue = Math.round( Random.IntRange( 1, level + 2 ) * power() );
			}
		}

		if (goldValue > 0) {
			Dungeon.level.drop( new Gold( goldValue ), defender.pos ).sprite.drop();
			defender.sprite.showStatus( CharSprite.NEUTRAL, Integer.toString( goldValue ) );
			Sample.INSTANCE.play( Assets.Sounds.GOLD );
		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return GOLDEN;
	}
}

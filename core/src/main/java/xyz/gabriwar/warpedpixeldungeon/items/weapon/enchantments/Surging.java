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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite.Glowing;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//ported from SPS-PD's energy enchantment: successive strikes build a charge
//that adds bonus damage to further procs
public class Surging extends Weapon.Enchantment {

	private static ItemSprite.Glowing GOLD = new ItemSprite.Glowing( 0xFFCC00 );

	@Override
	public int proc( Weapon weapon, Char attacker, Char defender, int damage ) {

		int level = Math.max( 0, weapon.buffedLvl() );

		// lvl 0 - 25%
		// lvl 1 - 40%
		// lvl 2 - 50%
		float procChance = (level+1f)/(level+4f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance) {

			float powerMulti = Math.max(1f, procChance);

			SurgeTracker surge = Buff.prolong(attacker, SurgeTracker.class, SurgeTracker.DURATION);

			int bonus = Math.round( surge.stacks * powerMulti * power() ); //scales with enchantment level
			if (bonus > 0){
				damage += bonus;
				attacker.sprite.emitter().burst( Speck.factory( Speck.UP ), 3 );
			}

			if (surge.stacks < SurgeTracker.MAX_STACKS){
				surge.stacks++;
			}

		}

		return damage;
	}

	@Override
	public Glowing glowing() {
		return GOLD;
	}

	public static class SurgeTracker extends FlavourBuff {

		public static final float DURATION = 6f;
		public static final int MAX_STACKS = 5;

		{
			type = buffType.POSITIVE;
		}

		public int stacks = 1;

		private static final String STACKS = "stacks";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( STACKS, stacks );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			stacks = bundle.getInt( STACKS );
		}
	}
}

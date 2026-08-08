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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

//ported from Unleashed PD: heavy bonus damage, but the spikes cut the wielder too
public class Vicious extends Weapon.Enchantment {

	private static ItemSprite.Glowing BLOODY = new ItemSprite.Glowing( 0x991111 );

	@Override
	public int proc( Weapon weapon, Char attacker, Char defender, int damage ) {
		int level = Math.max( 0, weapon.buffedLvl() );

		int bonus = Math.round( Random.NormalIntRange( weapon.min(), weapon.max() ) * 0.5f * power() );
		defender.damage( bonus, this );

		//self-injury chance drops as the weapon is mastered, and empowering reduces it further
		float backfireChance = 1f / ((level / 2f) + 5f + enchLevelSafety());
		if (attacker.isAlive() && Random.Float() < backfireChance) {
			attacker.damage( Random.NormalIntRange( 0, Math.max( 1, damage / 2 ) ), this );
			if (attacker.isAlive()) {
				Buff.affect( attacker, Bleeding.class ).set( Random.NormalIntRange( 2, 5 ) );
				Buff.prolong( attacker, Cripple.class, Cripple.DURATION );
			}
			if (attacker == xyz.gabriwar.warpedpixeldungeon.Dungeon.hero) {
				GLog.w( Messages.get( this, "backfire" ) );
			}
		}

		return damage;
	}

	//higher empowerment = safer to wield
	private float enchLevelSafety() {
		return 2f * (power() - 1f);
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return BLOODY;
	}
}

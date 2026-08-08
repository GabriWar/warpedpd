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
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//ported from Unleashed PD: the weapon attunes itself to its wielder, self-upgrading
//on kills. UL gated this on its per-weapon levelCap; modern SPD has no cap, so the
//self-upgrade is capped at SELF_UPGRADE_CAP to keep it out of the scroll economy.
public class Ancient extends Weapon.Enchantment {

	private static final int KILLS_NEEDED     = 10;
	private static final int SELF_UPGRADE_CAP = 3;

	private static ItemSprite.Glowing LTGRAY = new ItemSprite.Glowing( 0x888888 );

	private int numKills = 0;
	private int selfUpgrades = 0;

	@Override
	public int proc( Weapon weapon, Char attacker, Char defender, int damage ) {
		int level = Math.max( 0, weapon.buffedLvl() );

		int bonus = 0;
		if (Random.Float() < ((level + 1f) / (level + 3f)) * procChanceMultiplier( attacker )) {
			bonus = Math.round( Random.NormalIntRange( 1, level + 2 ) * power() );
			defender.damage( bonus, this );
		}

		if (damage + bonus >= defender.HP && selfUpgrades < SELF_UPGRADE_CAP) {
			numKills++;
			if (numKills >= KILLS_NEEDED) {
				numKills = 0;
				//later attunements get progressively harder to earn
				if (selfUpgrades == 0 || Random.Int( selfUpgrades + 1 ) == 0) {
					selfUpgrades++;
					weapon.upgrade();
					GLog.p( Messages.get( this, "attune", weapon.name() ) );
				}
			}
		}

		return damage;
	}

	private static final String NUM_KILLS    = "num_kills";
	private static final String SELF_UPGRADE = "self_upgrades";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( NUM_KILLS, numKills );
		bundle.put( SELF_UPGRADE, selfUpgrades );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		numKills = bundle.getInt( NUM_KILLS );
		selfUpgrades = bundle.getInt( SELF_UPGRADE );
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return LTGRAY;
	}
}

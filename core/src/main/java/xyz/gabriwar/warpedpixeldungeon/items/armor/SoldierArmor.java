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

package xyz.gabriwar.warpedpixeldungeon.items.armor;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EnergyParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//soldier's vest: taking a hit has a chance to snap the wearer into
//combat focus, blessing them for a short time
public class SoldierArmor extends Armor {

	{
		image = ItemSpriteSheet.SOLDIER_ARMOR;
	}

	public SoldierArmor() {
		super( 4 );
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {

		damage = super.proc(attacker, defender, damage);

		if (Random.Int(8) == 0) {
			Buff.prolong(defender, Bless.class, 10f);
			if (defender.sprite != null) {
				defender.sprite.centerEmitter().burst(EnergyParticle.FACTORY, 10);
			}
		}

		return damage;
	}

	//padded fabric vest: warmer and less conductive than scale
	@Override
	public float thermalOffset() {
		return super.thermalOffset() + 1.0f;
	}

	@Override
	public float thermalMass() {
		return 0.8f;
	}
}

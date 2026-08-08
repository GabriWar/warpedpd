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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class WindBottle extends MeleeWeapon {

	{
		image = ItemSpriteSheet.WIND_BOTTLE;
		hitSound = Assets.Sounds.HIT_MAGIC;
		hitSoundPitch = 1.1f;

		tier = 3;
		RCH = 2;    //extra reach
	}

	@Override
	public int max(int lvl) {
		return  5*(tier) +      //15 base, down from 20
				lvl*(tier);     //+3 per level, down from +4
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//knocks the target back 1 tile
		int oppositeDefender = defender.pos + (defender.pos - attacker.pos);
		Ballistica trajectory = new Ballistica(defender.pos, oppositeDefender, Ballistica.MAGIC_BOLT);
		WandOfBlastWave.throwChar(defender, trajectory, 1, true, false, this);

		//deals its damage a second time against magically shielded targets
		if (defender.shielding() > 0){
			defender.damage(damage, this);
		}
		//blinds on hit, or deals 50% bonus damage to already blinded targets
		if (defender.buff(Blindness.class) != null) {
			defender.damage(Math.round(damage * 0.5f), this);
		} else {
			Buff.prolong(defender, Blindness.class, 5f);
		}
		return super.proc(attacker, defender, damage);
	}

}

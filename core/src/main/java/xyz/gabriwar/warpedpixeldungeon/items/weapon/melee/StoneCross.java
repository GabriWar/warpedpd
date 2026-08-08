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
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

public class StoneCross extends MeleeWeapon {

	{
		image = ItemSpriteSheet.STONE_CROSS;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.9f;

		tier = 5;
		ACC = 0.8f; //0.8x accuracy
		DLY = 1.2f; //~0.83x speed
	}

	private static final int CHARGE_CAP = 20;

	private int charge = 0;

	@Override
	public int max(int lvl) {
		return  6*(tier+1) +    //36 base, up from 30
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	public int damageRoll(Char owner) {
		int damage = super.damageRoll(owner);
		if (charge >= CHARGE_CAP) {
			damage *= 5;
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
		}
		return damage;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (charge >= CHARGE_CAP) {
			charge = 0;
		}
		charge++;

		return super.proc(attacker, defender, damage);
	}

	@Override
	public String statsInfo() {
		return Messages.get(this, "stats_desc") + " " + Messages.get(this, "charge", charge, CHARGE_CAP);
	}

	//SPS-PD weapon: no Duelist ability was ever designed for it
	@Override
	public boolean hasDuelistAbility() {
		return false;
	}

	private static final String CHARGE = "charge";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE, charge);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		charge = bundle.getInt(CHARGE);
	}
}

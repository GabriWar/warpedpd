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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class RelicMeleeWeapon extends MeleeWeapon {

	{
		tier = 6;
		bones = false;
		unique = true;
	}

	public int charge = 0;
	public int chargeCap = 1000;
	protected int levelCap = 15;

	protected Buff passiveBuff;

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		passiveBuff = passiveBuff();
		if (passiveBuff != null) {
			passiveBuff.attachTo(ch);
		}
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)) {
			if (passiveBuff != null) {
				passiveBuff.detach();
				passiveBuff = null;
			}
			return true;
		}
		return false;
	}

	protected WeaponBuff passiveBuff() {
		return null;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		// Relic weapons deal bonus damage to bosses/mini-bosses
		if (defender instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gullin
				|| defender instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Kupua
				|| defender instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.MineSentinel
				|| defender instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Otiluke
				|| defender instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zot
				|| defender instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.ZotPhase) {
			defender.damage(Random.Int(damage, damage * 2), this);
		}
		//continue the modern proc chain (MeleeWeapon seed-poison -> Weapon enchant/
		//Holy/Smite/trinity/use-ID). The port dropped this, which killed every
		//relic enchant granted by the Alter (AresLeech/CromLuck/LokisPoison/NeptuneShock).
		return super.proc(attacker, defender, damage);
	}

	@Override
	public Item upgrade() {
		if (level() < levelCap) {
			return super.upgrade();
		}
		return this;
	}

	@Override
	public String status() {
		if (charge >= chargeCap) {
			return Messages.get(this, "status_ready");
		} else {
			return charge + "/" + chargeCap;
		}
	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc");
		if (charge >= chargeCap) {
			desc += "\n\n" + Messages.get(this, "desc_charged");
		} else {
			desc += "\n\n" + Messages.get(this, "desc_charging", charge, chargeCap);
		}
		return desc;
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

	public class WeaponBuff extends Buff {

		public int level() {
			return RelicMeleeWeapon.this.level();
		}

		public boolean isCursed() {
			return cursed;
		}
	}
}

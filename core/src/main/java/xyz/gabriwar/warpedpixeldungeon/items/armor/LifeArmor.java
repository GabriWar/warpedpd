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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

//living leaf armor: stores damage taken as charge, then heals the wearer
//for the stored amount after 20 turns without being hit
public class LifeArmor extends Armor {

	{
		image = ItemSpriteSheet.LIFE_ARMOR;
	}

	public LifeArmor() {
		super( 1 );
	}

	public int charge = 0;
	public int time = 0;

	private static final String CHARGE = "charge";
	private static final String TIME = "time";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE, charge);
		bundle.put(TIME, time);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		charge = bundle.getInt(CHARGE);
		time = bundle.getInt(TIME);
	}

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		Buff.affect(ch, LifeCharge.class);
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)) {
			LifeCharge buff = hero.buff(LifeCharge.class);
			if (buff != null) {
				buff.detach();
			}
			charge = 0;
			time = 0;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {

		damage = super.proc(attacker, defender, damage);

		charge += damage;
		time = 20;

		return damage;
	}

	public static class LifeCharge extends Buff {

		@Override
		public boolean act() {

			Armor armor = target instanceof Hero ? ((Hero)target).belongings.armor() : null;
			if (!(armor instanceof LifeArmor)) {
				detach();
				return true;
			}

			LifeArmor lifeArmor = (LifeArmor)armor;
			if (lifeArmor.time > 1) {
				lifeArmor.time--;
			} else if (lifeArmor.charge > 0) {
				int heal = Math.min(target.HT - target.HP, lifeArmor.charge);
				if (heal > 0) {
					target.HP += heal;
					target.sprite.emitter().burst(Speck.factory(Speck.HEALING), 1);
				}
				lifeArmor.charge = 0;
			}

			spend(TICK);
			return true;
		}
	}

	//a living armor: sap-warm, and its flesh dampens temperature swings
	@Override
	public float thermalOffset() {
		return super.thermalOffset() + 0.5f;
	}

	@Override
	public float thermalMass() {
		return 0.9f;
	}
}

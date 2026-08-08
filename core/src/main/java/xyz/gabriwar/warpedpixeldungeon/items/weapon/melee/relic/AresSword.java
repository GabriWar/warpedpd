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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BerryRegeneration;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

public class AresSword extends RelicMeleeWeapon {

	{
		image = ItemSpriteSheet.RELIC_ARESSWORD;
	}

	public static final String AC_REGEN = "REGEN";

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && charge >= chargeCap)
			actions.add(AC_REGEN);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_REGEN)) {
			GLog.p(Messages.get(this, "activate"));
			charge = 0;
			Buff.affect(hero, BerryRegeneration.class).set(level() * 2);
			updateQuickslot();
		}
	}

	@Override
	protected WeaponBuff passiveBuff() {
		return new RegenCounter();
	}

	public class RegenCounter extends WeaponBuff {
		@Override
		public boolean act() {
			if (charge < chargeCap) {
				charge += level();
				if (charge >= chargeCap) {
					charge = chargeCap;
					GLog.w(Messages.get(AresSword.class, "charged"));
				}
				updateQuickslot();
			}
			spend(TICK);
			return true;
		}
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

public class BerryRegeneration extends Buff {

	{
		type = buffType.POSITIVE;
	}

	private int regenLeft = 0;

	private static final String REGENLEFT = "regenleft";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(REGENLEFT, regenLeft);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		regenLeft = bundle.getInt(REGENLEFT);
	}

	public void set(int value) {
		if (regenLeft < value) {
			regenLeft = value;
		}
	}

	@Override
	public boolean act() {
		if (target.isAlive()) {
			if (target.HP < target.HT) {
				target.HP += Math.min(1 + Math.round(regenLeft / 25f), target.HT - target.HP);
			}
			spend(TICK);
			if (--regenLeft <= 0) {
				detach();
			}
		} else {
			detach();
		}
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.HEALING;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(regenLeft);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", regenLeft);
	}
}

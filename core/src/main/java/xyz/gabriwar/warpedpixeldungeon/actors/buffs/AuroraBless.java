/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

/**
 * Granted by the aurora borealis — an extremely rare winter night event.
 * Greatly boosts luck (equivalent to Ring of Wealth +3) and accuracy (+10%).
 * Lasts as long as the aurora is visible, refreshed each turn.
 */
public class AuroraBless extends FlavourBuff {

	public static final float DURATION = 5f;

	/** Equivalent Ring of Wealth level for drop chance calculations. */
	public static final int WEALTH_LEVEL = 3;

	/** Accuracy multiplier (1.10 = +10%). */
	public static final float ACCURACY_MULT = 1.10f;

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	/** Drop chance multiplier — same formula as Ring of Wealth. */
	public float dropChanceMultiplier() {
		return (float) Math.pow(1.20f, WEALTH_LEVEL);
	}

	@Override
	public int icon() {
		return BuffIndicator.AURORA_BLESS;
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns());
	}
}

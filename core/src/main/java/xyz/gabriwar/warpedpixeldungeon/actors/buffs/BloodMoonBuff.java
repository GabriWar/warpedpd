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
 * Active during a lunar eclipse (blood moon). The crimson light empowers the dark:
 * - Hero deals +25% damage to demonic enemies (blood fury)
 * - Undead/demonic mobs deal 2x damage and spawn much faster (handled externally)
 * - All enemies have +20% accuracy against the hero
 * - Drop rates slightly increased (blood moon fortune: +15%)
 */
public class BloodMoonBuff extends FlavourBuff {

	public static final float DURATION = 5f;

	/** Damage multiplier against demonic enemies. */
	public static final float DEMONIC_DMG_MULT = 1.25f;

	/** Enemies get this accuracy bonus against the hero. */
	public static final float ENEMY_ACC_MULT = 1.20f;

	/** Drop chance multiplier (modest luck). */
	public static final float DROP_MULT = 1.15f;

	{
		type = buffType.NEUTRAL;
		announced = true;
	}

	public float dropChanceMultiplier() {
		return DROP_MULT;
	}

	@Override
	public int icon() {
		return BuffIndicator.BLOOD_MOON;
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

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
 * Active during a solar eclipse. The unnatural mid-day darkness is disorienting:
 * - Accuracy reduced by 15%
 * - View distance reduced (handled by viewDistanceModifier)
 * - Stealth increased by 3 (darkness advantage)
 * - Undead/demonic mobs deal +50% damage and spawn faster (handled externally)
 */
public class SolarEclipseBuff extends FlavourBuff {

	public static final float DURATION = 5f;

	public static final float ACCURACY_MULT = 0.85f;
	public static final float EVASION_MULT  = 1.15f;
	public static final int   STEALTH_BONUS = 3;

	{
		type = buffType.NEUTRAL;
		announced = true;
	}

	@Override
	public int icon() {
		return BuffIndicator.SOLAR_ECLIPSE;
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

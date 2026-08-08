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

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/**
 * Applied when wind speed exceeds gale force (> 15).
 * Effects:
 * - Ranged accuracy reduced (scaling with wind: -10% breeze, -25% windy, -40% gale/storm)
 * - Small chance of stumbling each turn (lose a turn) at storm force
 * Duration-based: refreshed while in strong wind.
 */
public class Windswept extends FlavourBuff {

	public static final float DURATION = 10f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	/**
	 * Returns a ranged accuracy multiplier based on current wind speed.
	 * Called from Hero.attackSkill() for missile weapons.
	 */
	public float rangedAccuracyFactor() {
		float wind = ClimateManager.localWindSpeed();
		if (wind > 20)  return 0.60f;  // storm: -40%
		if (wind > 15)  return 0.75f;  // gale: -25%
		if (wind > 10)  return 0.90f;  // windy: -10%
		return 1f;
	}

	@Override
	public int icon() {
		return BuffIndicator.WINDSWEPT;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.7f, 0.9f, 0.7f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public String desc() {
		float wind = ClimateManager.localWindSpeed();
		String windDesc;
		if (wind > 20)       windDesc = Messages.get(this, "storm");
		else if (wind > 15)  windDesc = Messages.get(this, "gale");
		else if (wind > 10)  windDesc = Messages.get(this, "windy");
		else                 windDesc = Messages.get(this, "breeze");
		return Messages.get(this, "desc", windDesc, dispTurns());
	}
}

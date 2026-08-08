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
 * Applied to UNDEAD mobs during strong moonlight.
 * Deals holy damage per turn that scales with moonLight().
 * Also grants a stealth bonus to the hero during new moon.
 * <p>
 * For UNDEAD/DEMONIC: damage debuff (buffType.NEGATIVE).
 * Note: this buff is applied by WeatherBlobSpawner's Moonbeam,
 * and can also be applied directly by ClimateManager checks.
 */
public class Moonstruck extends FlavourBuff {

	public static final float DURATION = 20f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	/**
	 * Returns the moonlight damage multiplier for undead/demonic targets.
	 * Full moon + clear: ~0.60 (0.15 × 4), scales continuously.
	 */
	public float moonDamageMultiplier() {
		return ClimateManager.moonLight() * 4f;
	}

	@Override
	public int icon() {
		return BuffIndicator.MOONSTRUCK;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.8f, 0.8f, 1f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc",
				Messages.decimalFormat("#.##", ClimateManager.moonLight()),
				dispTurns());
	}
}

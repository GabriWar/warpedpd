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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * Applied when the hero wades through enough water tiles (2% per step).
 *
 * Effects:
 * - -2 stealth  (squelching sounds give away position)
 * - -4°C to feelsLikeAt  (cold seeps through wet shoes, hastening Hypothermia)
 *
 * Dries naturally over ~75 turns on dry ground.
 * Dries at 3× speed when the tile temperature exceeds 22°C (warm areas),
 * at 8× speed beside embers or open fire, at 3× while carrying a burning
 * torch, and not at all in rain or standing water.
 * Each additional water step while soaked adds 10 turns (keeps you soaked in swampy areas).
 */
public class SoakedShoes extends Buff {

	public static final float MAX_DURATION   = 300f;
	public static final float STEP_DURATION  = 150f; // added on first soak (~75 steps of drying)
	public static final float STEP_REFRESH   = 10f;  // added per water step while already soaked
	public static final float WARM_TEMP      = 22f;  // tile temp above which shoes dry 2× faster

	/** Cold penalty applied to feelsLikeAt(). */
	public static final float COLD_PENALTY   = -4f;
	/** Stealth penalty (negative = noisier). */
	public static final int   STEALTH_PENALTY = -2;

	private float left = 0f;

	private static final String LEFT = "left";

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	/** Add duration (on initial soak or per water step). Capped at MAX_DURATION. */
	public void add( float duration ) {
		left = Math.min(left + duration, MAX_DURATION);
	}

	@Override
	public boolean act() {
		if (left <= 0f) {
			detach();
			return true;
		}
		float tickCost;
		if (Dungeon.level.water[target.pos]
				|| (ClimateManager.isRaining() && ClimateManager.localPrecipRate() > 0f)) {
			// standing in water or rain keeps them soaked
			tickCost = 0f;
		} else if (TileTemperature.nearFire(target.pos)) {
			// a campfire dries shoes in a couple of dozen steps
			tickCost = 8f;
		} else {
			// warm ground, or a torch burning in your hand, dries them faster
			xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light light = target.buff(Light.class);
			boolean torch = light != null && light.flame;
			tickCost = (torch || TileTemperature.tileTemp(target.pos) > WARM_TEMP) ? 3f : 2f;
		}
		left -= tickCost;
		spend(TICK);
		return true;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(LEFT, left);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		left = bundle.getFloat(LEFT);
	}

	@Override
	public int icon() {
		return BuffIndicator.SOAKED_SHOES;
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0f, (MAX_DURATION - left) / MAX_DURATION);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString((int) left);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", (int) left);
	}
}

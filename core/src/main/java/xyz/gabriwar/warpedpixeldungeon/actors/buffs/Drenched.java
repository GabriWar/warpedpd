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

/**
 * Applied by heavy rain or water exposure.
 * Effects:
 * - Scroll reading has a 15% failure chance (wet pages)
 * - Cold damage vulnerability +50% (cold attacks deal 1.5× damage)
 * - Electric conductivity +20% (electric attacks deal 1.2× damage)
 * Duration-based: refreshed every turn in heavy rain, expires otherwise.
 */
public class Drenched extends FlavourBuff {

	public static final float DURATION = 15f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	@Override
	public boolean attachTo(xyz.gabriwar.warpedpixeldungeon.actors.Char target) {
		// Being drenched extinguishes burning
		Buff.detach(target, Burning.class);
		return super.attachTo(target);
	}

	/**
	 * Heat dries you off. A flavour buff only wakes up to expire, so the wet
	 * cannot hasten itself - the hero's turn calls this, and it eats into what
	 * is left: a quarter of it per turn beside a fire, a tenth by torchlight.
	 */
	public void dryOff() {
		if (target == null || xyz.gabriwar.warpedpixeldungeon.Dungeon.level == null) return;
		if (xyz.gabriwar.warpedpixeldungeon.Dungeon.level.water[target.pos]) return;

		float bite;
		if (xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature.nearFire( target.pos )) {
			bite = cooldown() / 4f;
		} else {
			Light light = target.buff( Light.class );
			bite = (light != null && light.flame) ? cooldown() / 10f : 0f;
		}
		if (bite > 0.01f) spend( -bite );
	}

	/** Whether a scroll reading attempt should fail due to wet pages. */
	public static boolean scrollFails() {
		return com.watabou.utils.Random.Float() < 0.15f;
	}

	/** Cold damage multiplier when drenched. */
	public float coldVulnerability() {
		return 1.5f;
	}

	/** Electric damage multiplier when drenched. */
	public float electricVulnerability() {
		return 1.2f;
	}

	@Override
	public int icon() {
		return BuffIndicator.DRENCHED;
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

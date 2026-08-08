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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * Applied when feelsLikeTemp < -5°C and the hero is not near fire or wearing warm armor.
 * Deals 1 frost damage every 20 turns and reduces movement speed by 15%.
 * Clears itself when the hero warms up (feelsLike > 0°C or near fire).
 */
public class Hypothermia extends Buff implements Hero.Doom {

	private float partialDamage = 0f;
	private int turnsSinceDmg = 0;
	private int turnsSinceFreeze = 0;

	private static final int DMG_INTERVAL   = 20;
	private static final int FREEZE_INTERVAL = 10;

	/** Feels-like (°C) below which the hero is warned they are getting dangerously cold. */
	public static final float WARN_TEMP = 0f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	@Override
	public boolean attachTo(xyz.gabriwar.warpedpixeldungeon.actors.Char target) {
		// Conflicting: can't be burning and hypothermic
		Buff.detach(target, Burning.class);
		return super.attachTo(target);
	}

	@Override
	public boolean act() {
		if (target.isAlive()) {

			// Clear if warmed up — uses bodyTemp (gradual convergence) for all chars
			float temp = Float.isNaN(target.bodyTemp)
					? TileTemperature.feelsLikeAt(target.pos)
					: target.bodyTemp;
			boolean nearFire = target.buff(Burning.class) != null
					|| target.buff(FireImbue.class) != null;

			if (temp > 0f || nearFire) {
				detach();
				return true;
			}

			// Sleeping in the cold: the body cannot fight it, hypothermia progresses twice as fast
			boolean asleep = Sleep.isAsleep(target);
			int step = asleep ? 2 : 1;

			turnsSinceDmg += step;
			if (turnsSinceDmg >= DMG_INTERVAL) {
				turnsSinceDmg = 0;
				int dmg = 1 + Dungeon.scalingDepth() / 10;
				target.damage(dmg, this);
				if (asleep && target instanceof Hero) {
					GLog.w(Messages.get(this, "asleep"));
				}
			}

			// Occasionally freeze solid
			turnsSinceFreeze += step;
			if (turnsSinceFreeze >= FREEZE_INTERVAL) {
				turnsSinceFreeze = 0;
				if (Random.Int(3) == 0) {
					Buff.prolong(target, Frost.class, Frost.DURATION / 2f);
					if (target instanceof Hero) {
						GLog.w(Messages.get(this, "freeze"));
					}
				}
			}

		} else {
			detach();
		}

		spend(TICK);
		return true;
	}

	public float speedFactor() {
		return 0.85f; // 15% speed reduction
	}

	@Override
	public int icon() {
		return BuffIndicator.HYPOTHERMIA;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.4f, 0.7f, 1f);
	}

	@Override
	public void fx(boolean on) {
		if (on) target.sprite.add(xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.State.HYPOTHERMIA);
		else    target.sprite.remove(xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.State.HYPOTHERMIA);
	}

	@Override
	public String desc() {
		float displayTemp = Float.isNaN(target.bodyTemp)
				? TileTemperature.feelsLikeAt(target.pos)
				: target.bodyTemp;
		return Messages.get(this, "desc", Messages.decimalFormat("#.#", displayTemp));
	}

	@Override
	public void onDeath() {
		Dungeon.fail(this);
		GLog.n(Messages.get(this, "ondeath"));
	}

	private static final String PARTIAL = "partialDamage";
	private static final String TURNS   = "turnsSinceDmg";
	private static final String FREEZE  = "turnsSinceFreeze";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(PARTIAL, partialDamage);
		bundle.put(TURNS, turnsSinceDmg);
		bundle.put(FREEZE, turnsSinceFreeze);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		partialDamage = bundle.getFloat(PARTIAL);
		turnsSinceDmg = bundle.getInt(TURNS);
		turnsSinceFreeze = bundle.getInt(FREEZE);
	}
}

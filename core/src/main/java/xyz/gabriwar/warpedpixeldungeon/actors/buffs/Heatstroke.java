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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * Applied when feelsLikeTemp > 35°C.
 *
 * Damage tiers (all scale with depth):
 *   35–45°C  Mild      — no direct damage; hunger drains 20% faster
 *   45–55°C  Severe    — damage accumulates; ~1 dmg per 20 turns; hunger 25% faster
 *   55–65°C  Critical  — ~1 dmg per 10 turns; hunger 35% faster
 *   65°C+    Lethal    — ~1 dmg per 6 turns; hunger 50% faster; may faint
 *
 * Fainting (Paralysis 3t) triggers randomly above 45°C every 8 turns (25% chance).
 * Clears when temp drops below 30°C.
 */
public class Heatstroke extends Buff implements Hero.Doom {

	private float partialDmg     = 0f;
	private int   turnsSinceFaint = 0;

	private static final int   FAINT_INTERVAL = 8;
	/** Feels-like (°C) above which the hero is warned they are getting dangerously hot. */
	public static final float  WARN_TEMP      = 30f;
	// damage accumulation divisor — lower = more damage per tick
	// excess = (temp - 35). partialDmg += excess / DIVISOR * depthScale each turn.
	// at 45°C (excess=10), DIVISOR=200: 10/200 = 0.05/turn → 1 dmg per 20 turns
	// at 55°C (excess=20): 0.10/turn → 1 dmg per 10 turns
	// at 65°C (excess=30): 0.15/turn → 1 dmg per ~7 turns
	private static final float DMG_DIVISOR    = 200f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	@Override
	public boolean attachTo(xyz.gabriwar.warpedpixeldungeon.actors.Char target) {
		Buff.detach(target, Hypothermia.class);
		Buff.detach(target, Chill.class);
		Buff.detach(target, Frost.class);
		return super.attachTo(target);
	}

	@Override
	public boolean act() {
		if (target.isAlive()) {

			float temp = TileTemperature.feelsLikeAt(target.pos);

			// Clear once the body itself has cooled, not just the tile. The buff is
			// applied on bodyTemp > 35, so clearing on bodyTemp < 30 gives proper
			// hysteresis - clearing on the tile's feels-like made it flicker off and
			// re-announce every turn whenever you stepped somewhere cooler while still
			// overheated.
			if (!Float.isNaN(target.bodyTemp) && target.bodyTemp < 30f) {
				detach();
				return true;
			}

			// Damage: accumulates proportionally to heat excess above 35°C.
			// Scales with dungeon depth so later floors are more threatening.
			float excess = Math.max(0f, temp - 35f);
			if (excess > 0f) {
				float depthScale = 1f + Dungeon.scalingDepth() / 20f;
				partialDmg += (excess / DMG_DIVISOR) * depthScale;
				if (partialDmg >= 1f) {
					int dmg = (int) partialDmg;
					partialDmg -= dmg;
					target.damage(dmg, this);
					if (target instanceof Hero) {
						if      (temp >= 65f) GLog.w(Messages.get(this, "dmg_lethal"));
						else if (temp >= 55f) GLog.w(Messages.get(this, "dmg_critical"));
						else if (temp >= 45f) GLog.w(Messages.get(this, "dmg_severe"));
						else                  GLog.w(Messages.get(this, "dmg_mild"));
					}
				}
			}

			// Fainting: above 45°C, every 8 turns, 25% chance
			if (temp > 45f) {
				turnsSinceFaint++;
				if (turnsSinceFaint >= FAINT_INTERVAL) {
					turnsSinceFaint = 0;
					if (Random.Int(4) == 0) {
						Buff.prolong(target, Paralysis.class, 3f);
						if (target instanceof Hero) {
							GLog.w(Messages.get(this, "faint"));
						}
					}
				}
			}

		} else {
			detach();
		}

		spend(TICK);
		return true;
	}

	/**
	 * Hunger drains faster the hotter it is.
	 * 35°C: +20%  |  45°C: +25%  |  55°C: +35%  |  65°C: +50%
	 */
	public float hungerFactor() {
		float temp = TileTemperature.feelsLikeAt(target.pos);
		if      (temp >= 65f) return 1.5f;
		else if (temp >= 55f) return 1.35f;
		else if (temp >= 45f) return 1.25f;
		else                  return 1.2f;
	}

	@Override
	public int icon() {
		return BuffIndicator.HEATSTROKE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 0.5f, 0.2f);
	}

	@Override
	public void fx(boolean on) {
		if (on) target.sprite.add(xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.State.HEATSTROKE);
		else    target.sprite.remove(xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite.State.HEATSTROKE);
	}

	@Override
	public String desc() {
		float temp = TileTemperature.feelsLikeAt(target.pos);
		String tier;
		if      (temp >= 65f) tier = Messages.get(this, "tier_lethal");
		else if (temp >= 55f) tier = Messages.get(this, "tier_critical");
		else if (temp >= 45f) tier = Messages.get(this, "tier_severe");
		else                  tier = Messages.get(this, "tier_mild");
		return Messages.get(this, "desc", Messages.decimalFormat("#.#", temp), tier);
	}

	@Override
	public void onDeath() {
		Dungeon.fail(this);
		GLog.n(Messages.get(this, "ondeath"));
	}

	private static final String PARTIAL = "partialDmg";
	private static final String FAINT   = "turnsSinceFaint";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(PARTIAL, partialDmg);
		bundle.put(FAINT,   turnsSinceFaint);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		partialDmg      = bundle.getFloat(PARTIAL);
		turnsSinceFaint = bundle.getInt(FAINT);
	}
}

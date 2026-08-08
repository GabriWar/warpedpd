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
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfChallenge;
import xyz.gabriwar.warpedpixeldungeon.levels.VaultLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;

public class Sleepiness extends Buff {

	//tuned against the day length: FULL_CYCLE turns == 1 in-game day (24h).
	//Drowsiness sets in after 1.5 days awake, full exhaustion (collapse) after 3 days.
	public static final float DROWSY	= DayNightCycle.FULL_CYCLE * 1.5f;
	public static final float COMATOSE	= DayNightCycle.FULL_CYCLE * 3f;

	//how much tiredness a single melee attack adds
	private static final float EXERT_AMOUNT = 2f;

	private float level;
	//set once the hero collapses at COMATOSE; cleared once rested back below DROWSY,
	//so the forced sleep only fires once per exhaustion rather than every turn.
	private boolean collapsed;

	private static final String LEVEL		= "level";
	private static final String COLLAPSED	= "collapsed";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put( LEVEL, level );
		bundle.put( COLLAPSED, collapsed );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		level = bundle.getFloat( LEVEL );
		collapsed = bundle.getBoolean( COLLAPSED );
	}

	@Override
	public boolean act() {

		if (Dungeon.level.locked
				|| WPDSettings.intro()
				|| target.buff(ScrollOfChallenge.ChallengeArena.class) != null
				|| Dungeon.level instanceof VaultLevel){
			spend(TICK);
			return true;
		}

		if (target.isAlive() && target instanceof Hero) {

			Hero hero = (Hero)target;

			//sleeping (forced collapse or manual rest) recovers tiredness, faster at night.
			//rest is quick: full 3-day exhaustion clears in ~0.1 day, ~30% faster than
			//the original base recovery feel.
			if (target.buff(MagicalSleep.class) != null || hero.resting) {

				float recovery = 30f;
				if (DayNightCycle.isNight()) recovery *= 1.5f;
				level = Math.max(0, level - recovery);
				if (collapsed && level < DROWSY) collapsed = false;

			} else {

				float wakeDelay = 1f;
				//circadian: staying awake at night tires faster, daytime a little slower.
				//kept gentle so total time-to-collapse stays within the ~6-8h window.
				if (DayNightCycle.isNight()) {
					wakeDelay /= 1.25f;
				} else if (DayNightCycle.isBright()) {
					wakeDelay *= 1.1f;
				}
				//climate stress: braving hostile temperatures wears the hero down
				if (target.buff(Hypothermia.class) != null || target.buff(Heatstroke.class) != null) {
					wakeDelay /= 1.35f;
				}
				//exertion: starving or badly wounded makes it harder to stay sharp
				Hunger hunger = target.buff(Hunger.class);
				if (hunger != null && hunger.isStarving()) {
					wakeDelay /= 1.25f;
				}
				if (hero.HP < hero.HT / 3f) {
					wakeDelay /= 1.25f;
				}

				float newLevel = level + (1f / wakeDelay);

				if (newLevel >= COMATOSE) {
					boolean wasMaxed = level >= COMATOSE;
					newLevel = COMATOSE;
					//forced collapse only happens at night; in daylight the hero just
					//maxes out (heavy penalties) until darkness lets them sleep.
					if (!collapsed && DayNightCycle.isNight()) {
						collapsed = true;
						GLog.n( Messages.get(this, "oncomatose") );
						hero.interrupt();
						Buff.affect(target, MagicalSleep.class);
					} else if (!wasMaxed && !DayNightCycle.isNight()) {
						GLog.n( Messages.get(this, "oncomatoseday") );
						hero.interrupt();
					}
				} else if (newLevel >= DROWSY && level < DROWSY) {
					GLog.w( Messages.get(this, "ondrowsy") );
				}

				level = newLevel;
			}

			spend( TICK );

		} else {

			diactivate();

		}

		return true;
	}

	//flat tiredness bump from physical exertion (e.g. a melee swing)
	public void exert() {
		affectSleep( EXERT_AMOUNT );
	}

	//positive amount tires the hero, negative (a future stimulant) wakes them
	public void affectSleep( float amount ) {
		level = GameMath.gate( 0, level + amount, COMATOSE );
		if (collapsed && level < DROWSY) collapsed = false;
		BuffIndicator.refreshHero();
	}

	public void wake( float amount ) {
		affectSleep( -amount );
	}

	public float level() {
		return level;
	}

	public boolean isDrowsy() {
		return level >= DROWSY;
	}

	public boolean isComatose() {
		return level >= COMATOSE;
	}

	@Override
	public int icon() {
		if (level < DROWSY) {
			return BuffIndicator.NONE;
		} else if (level < COMATOSE) {
			return BuffIndicator.SLEEPINESS;
		} else {
			return BuffIndicator.EXHAUSTED;
		}
	}

	@Override
	public float iconFadePercent() {
		//fill the icon as tiredness climbs from DROWSY toward COMATOSE
		return GameMath.gate(0, (COMATOSE - level) / (COMATOSE - DROWSY), 1);
	}

	@Override
	public String name() {
		if (level < COMATOSE) {
			return Messages.get(this, "drowsy");
		} else {
			return Messages.get(this, "exhausted");
		}
	}

	@Override
	public String desc() {
		String result;
		if (level < COMATOSE) {
			result = Messages.get(this, "desc_intro_drowsy");
		} else {
			result = Messages.get(this, "desc_intro_exhausted");
		}
		result += Messages.get(this, "desc");
		return result;
	}
}

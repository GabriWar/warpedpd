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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

/**
 * Granted by spring petal ambiance on clear spring days.
 * Nature's vitality grants passive healing: 1 HP every 10 turns.
 * Refreshed each turn while petals are drifting.
 */
public class SpringBloom extends FlavourBuff {

	public static final float DURATION = 5f;

	/** Heal 1 HP every this many turns. */
	private static final int HEAL_INTERVAL = 10;

	private int turnCounter = 0;

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	@Override
	public boolean act() {
		turnCounter++;
		if (turnCounter >= HEAL_INTERVAL && target instanceof Hero) {
			Hero hero = (Hero) target;
			if (hero.HP < hero.HT) {
				hero.HP = Math.min(hero.HT, hero.HP + 1);
				hero.sprite.emitter().burst(
						xyz.gabriwar.warpedpixeldungeon.effects.Speck.factory(
								xyz.gabriwar.warpedpixeldungeon.effects.Speck.HEALING), 1);
			}
			turnCounter = 0;
		}
		return super.act();
	}

	@Override
	public int icon() {
		return BuffIndicator.SPRING_BLOOM;
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

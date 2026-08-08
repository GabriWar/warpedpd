/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

public class ManaRegen extends Buff {

	private static final float REGENERATION_DELAY = 100;

	{
		actPriority = HERO_PRIO - 1;
	}

	@Override
	public boolean act() {
		if (target.isAlive()) {
			Hero hero = (Hero) target;
			int effectiveMT = hero.MT + RingOfMagic.manaBonus(hero);
			if (hero.MP < effectiveMT && !hero.isStarving()) {
				hero.MP += 1;
			}

			int mLevel = Dungeon.hero.magicLevel + 1;
			float regenDelay = REGENERATION_DELAY / mLevel;
			regenDelay /= RingOfMagic.manaRegenMultiplier(hero);

			//skill tree: Mage's Meditation
			int skillRegen = hero.heroSkills.allManaRegen();
			if (skillRegen > 0) regenDelay /= Math.pow( 1.2, skillRegen );
			spend(regenDelay);
		} else {
			detach();
		}
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}
}

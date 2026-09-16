/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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
import xyz.gabriwar.warpedpixeldungeon.actors.WeatherAttunement;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * Silent hero buff, always attached like ManaRegen: each turn the waterskin
 * condenses dew out of the air when the hero owns the Tinkerer's condenser or
 * carries a Dewcharge (the Dew Draw spell). Both together condense twice as fast.
 */
public class DewCondenser extends Buff {

	{
		actPriority = HERO_PRIO - 1;
	}

	private float partial = 0;

	@Override
	public boolean act() {
		if (!target.isAlive()){
			detach();
			return true;
		}
		Hero hero = (Hero) target;
		int strength = (Dungeon.dewCondenser ? 1 : 0) + (hero.buff(Dewcharge.class) != null ? 1 : 0);
		Waterskin skin = strength > 0 ? hero.belongings.getItem(Waterskin.class) : null;
		if (skin != null && !skin.isFull()){
			partial += WeatherAttunement.condenseRate() * strength;
			if (partial >= 1f){
				int whole = (int) partial;
				partial -= whole;
				skin.condense(whole);
			}
		} else {
			partial = 0;
		}
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	private static final String PARTIAL = "partial";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( PARTIAL, partial );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		partial = bundle.getFloat( PARTIAL );
	}
}

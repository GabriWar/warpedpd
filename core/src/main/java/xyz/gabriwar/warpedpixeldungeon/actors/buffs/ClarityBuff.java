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

import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//elixir of clarity: the next few kills that grant experience grant half again as much
public class ClarityBuff extends Buff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	public static final int KILLS = 3;
	public static final float EXP_BONUS = 0.5f;

	private int left = KILLS;

	public void reset() {
		left = KILLS;
	}

	//called with the experience a kill is about to grant, returns the boosted amount
	public int boost( int exp ) {
		if (exp <= 0) return exp;
		exp += Math.round( exp * EXP_BONUS );
		left--;
		if (left <= 0) {
			detach();
		} else {
			BuffIndicator.refreshHero();
		}
		return exp;
	}

	@Override
	public int icon() {
		return BuffIndicator.LIGHT;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 0.5f, 0.8f, 1f );
	}

	@Override
	public float iconFadePercent() {
		return (KILLS - left) / (float) KILLS;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString( left );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc", left );
	}

	private static final String LEFT = "left";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEFT, left );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		left = bundle.getInt( LEFT );
	}
}

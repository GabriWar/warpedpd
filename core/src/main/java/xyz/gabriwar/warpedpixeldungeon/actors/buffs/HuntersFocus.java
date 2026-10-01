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

import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

//elixir of the hunt: faster movement and sharper aim
public class HuntersFocus extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	public static final float DURATION = 20f;

	public static final float SPEED_MULT = 1.2f;
	public static final float ACCURACY_MULT = 1.15f;

	@Override
	public int icon() {
		return BuffIndicator.MARK;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 1f, 0.75f, 0.2f );
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, (DURATION - visualcooldown()) / DURATION );
	}
}

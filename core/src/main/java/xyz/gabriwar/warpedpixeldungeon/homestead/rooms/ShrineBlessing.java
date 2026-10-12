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

package xyz.gabriwar.warpedpixeldungeon.homestead.rooms;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/**
 * A night's sleep in a SHRINE (a room with a statue or an altar: RoomTypes) leaves a quiet blessing
 * on the hero: REGEN_BONUS faster natural healing (Regeneration) for one in-game day.
 */
public class ShrineBlessing extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	public static final float REGEN_BONUS = 0.1f;
	/** One in-game day. */
	public static final float DURATION = DayNightCycle.FULL_CYCLE;

	/** How much faster a blessed hero heals (Regeneration divides its delay by it). */
	public static float regenFactor( Char ch ){
		return ch.buff( ShrineBlessing.class ) != null ? 1f + REGEN_BONUS : 1f;
	}

	@Override
	public int icon(){
		return BuffIndicator.BLESS;
	}

	@Override
	public void tintIcon( Image icon ){
		icon.hardlight( 0.7f, 0.85f, 1f );
	}

	@Override
	public float iconFadePercent(){
		return Math.max( 0, (DURATION - visualcooldown()) / DURATION );
	}
}

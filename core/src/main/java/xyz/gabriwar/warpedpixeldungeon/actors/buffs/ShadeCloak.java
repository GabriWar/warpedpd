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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//elixir of the shade: the first few attacks made while invisible do not break invisibility
public class ShadeCloak extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	public static final float DURATION = Invisibility.DURATION;
	public static final int CHARGES = 3;

	private int charges = CHARGES;

	public void reset() {
		charges = CHARGES;
	}

	//called where an attack would dispel invisibility. Returns true if the cloak kept it up.
	public static boolean absorbDispel( Char ch ) {
		ShadeCloak cloak = ch.buff( ShadeCloak.class );
		if (cloak == null || ch.buff( Invisibility.class ) == null) return false;

		//an assassin's preparation is still spent by the strike
		Preparation prep = ch.buff( Preparation.class );
		if (prep != null) prep.detach();

		cloak.charges--;
		if (cloak.charges <= 0) {
			cloak.detach();
		}
		BuffIndicator.refreshHero();
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.INVISIBLE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 0.6f, 0.3f, 1f );
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, (DURATION - visualcooldown()) / DURATION );
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString( charges );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc", charges, dispTurns() );
	}

	private static final String CHARGES_KEY = "charges";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( CHARGES_KEY, charges );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		charges = bundle.getInt( CHARGES_KEY );
	}
}

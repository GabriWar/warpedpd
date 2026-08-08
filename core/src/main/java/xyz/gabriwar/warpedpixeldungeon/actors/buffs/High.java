/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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
import com.watabou.utils.Random;

// OV: intoxication buff — random positive/negative effects each turn.
// Use Buff.affect(target, High.class).set(duration) instead of Buff.prolong().
public class High extends Buff {

	{
		type = buffType.NEUTRAL;
		announced = true;
	}

	public static final float DURATION = 10f;

	protected float left;

	private static final String LEFT = "left";

	public void set( float duration ) {
		left = duration;
	}

	public void extend( float duration ) {
		left += duration;
	}

	public void prolong( float duration ) {
		left = Math.max( left, duration );
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEFT, left );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		left = bundle.getFloat( LEFT );
	}

	@Override
	public boolean act() {
		// random intoxication effect each turn
		switch (Random.Int( 4 )) {
			case 0:
				// stumble — random movement
				Buff.prolong( target, Vertigo.class, 2f );
				break;
			case 1:
				// rush — brief speed boost
				Buff.prolong( target, Haste.class, 2f );
				break;
			// cases 2 and 3: nothing this turn
		}

		spend( TICK );
		left -= TICK;
		if (left <= 0) detach();
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.VERTIGO;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 0.4f, 1f, 0.4f );
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, (DURATION - left) / DURATION );
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString( (int) left );
	}

	@Override
	public String toString() {
		return Messages.get( this, "name" );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc", dispTurns( left ) );
	}
}

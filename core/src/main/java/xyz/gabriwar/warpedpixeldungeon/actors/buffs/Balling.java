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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

public class Balling extends FlavourBuff {

	{
		type = buffType.NEUTRAL;
		announced = true;
	}

	public static final float DURATION = 20f;

	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )) {
			if (target.sprite != null) {
				target.sprite.angularSpeed = Random.Int(2) == 0 ? 720 : -720;
				target.sprite.origin.set( target.sprite.width / 2f, target.sprite.height / 2f );
			}
			return true;
		}
		return false;
	}

	@Override
	public void detach() {
		if (target.sprite != null) {
			target.sprite.angularSpeed = 0;
			target.sprite.origin.set( 0, 0 );
		}
		super.detach();
	}

	@Override
	public int icon() {
		return BuffIndicator.HASTE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 1f, 0.5f, 0f );
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, (DURATION - visualcooldown()) / DURATION );
	}

	@Override
	public String toString() {
		return Messages.get( this, "name" );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc", dispTurns() );
	}
}

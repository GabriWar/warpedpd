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

import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CorrosiveGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ParalyticGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

//wicked brew, drunk: the body shrugs off the gases the brew is made from
public class GasWard extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;

		immunities.add( ToxicGas.class );
		immunities.add( ParalyticGas.class );
		immunities.add( CorrosiveGas.class );
	}

	public static final float DURATION = 40f;

	@Override
	public int icon() {
		return BuffIndicator.IMMUNITY;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 0.6f, 1f, 0.4f );
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, (DURATION - visualcooldown()) / DURATION );
	}
}

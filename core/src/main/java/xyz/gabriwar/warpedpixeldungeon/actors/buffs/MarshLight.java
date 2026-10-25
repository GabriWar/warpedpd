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

import xyz.gabriwar.warpedpixeldungeon.items.wetlands.BogLantern;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/**
 * A bog lantern's marsh light (items/wetlands/BogLantern) carried by the hero: its cold green glow
 * burns through the swamp's mist, which takes nothing off his sight while it lasts
 * (WetlandSites.mistPenalty). The lantern lights the dark as well, through the Light it lays on
 * beside this.
 */
public class MarshLight extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	@Override
	public int icon(){
		return BuffIndicator.LIGHT;
	}

	@Override
	public void tintIcon( Image icon ){
		icon.hardlight( 0.45f, 1f, 0.55f );
	}

	@Override
	public float iconFadePercent(){
		return Math.max( 0, (BogLantern.DURATION - visualcooldown()) / BogLantern.DURATION );
	}
}

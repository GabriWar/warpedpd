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

/**
 * A sunken shrine's blessing (actors/mobs/npcs/SunkenIdol, levels/overworld/WetlandSites): what the
 * bog does to the living passes over the blessed. No poison, no rot (ooze) and no parasite takes
 * hold of him, and the bog's water drags at his legs no more (WetlandSites.bogFactor).
 */
public class BogBlessing extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;

		immunities.add( Poison.class );
		immunities.add( Ooze.class );
		immunities.add( ParasiticInfection.class );
	}

	//its length when laid on, for the icon's fade (an offering sets it: WetlandSites.blessTurns)
	private float laid = 500f;

	public void laid( float turns ){
		laid = Math.max( 1f, turns );
	}

	@Override
	public boolean attachTo( xyz.gabriwar.warpedpixeldungeon.actors.Char target ){
		if (super.attachTo( target )){
			//what the bog had already put in him is washed out with the blessing
			Buff.detach( target, Poison.class );
			Buff.detach( target, ParasiticInfection.class );
			Buff.detach( target, Ooze.class );
			return true;
		}
		return false;
	}

	@Override
	public int icon(){
		return BuffIndicator.BLESS;
	}

	@Override
	public void tintIcon( Image icon ){
		icon.hardlight( 0.35f, 0.85f, 0.6f );
	}

	@Override
	public float iconFadePercent(){
		return Math.max( 0, (laid - visualcooldown()) / laid );
	}

	private static final String LAID = "laid";

	@Override
	public void storeInBundle( com.watabou.utils.Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( LAID, laid );
	}

	@Override
	public void restoreFromBundle( com.watabou.utils.Bundle bundle ){
		super.restoreFromBundle( bundle );
		if (bundle.contains( LAID )) laid = bundle.getFloat( LAID );
	}
}

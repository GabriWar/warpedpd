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

import xyz.gabriwar.warpedpixeldungeon.actors.Sandstorm;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * A hero in a sandstorm (actors/Sandstorm): put on and refreshed by Sandstorm.onHeroTurn while
 * the sand blows where he stands, taken off as soon as it does not. It only shows the storm and
 * what it is doing to him - his sight, his speed and his eyes are Sandstorm's to work out - and
 * keeps the few turns of rest after the sand last got in his eyes. Its fields are saved, so a game
 * loaded in the storm shows the storm it was saved in until the next turn sets them again.
 */
public class Sandblown extends Buff {

	{
		type = buffType.NEGATIVE;
	}

	private float strength;
	private boolean worn, lee;
	private int rest = 0;

	/** This turn's storm over him, and whether his goggles or scarf, or a wall upwind, keep it off. */
	public void set( float strength, boolean worn, boolean lee ){
		this.strength = strength;
		this.worn = worn;
		this.lee = lee;
		type = worn || lee ? buffType.NEUTRAL : buffType.NEGATIVE;
	}

	/** The sand may get in his eyes this turn (its last time is Sandstorm.BLIND_REST turns past). */
	public boolean mayBlind(){
		return rest <= 0;
	}

	public void blinded(){
		rest = Sandstorm.BLIND_REST;
	}

	@Override
	public boolean act(){
		if (rest > 0) rest--;
		spend( TICK );
		return true;
	}

	@Override
	public int icon(){
		return BuffIndicator.DAZE;
	}

	@Override
	public void tintIcon( Image icon ){
		//the sand's own tan; paler while it is kept off
		if (worn || lee) icon.hardlight( 0.85f, 0.8f, 0.6f );
		else icon.hardlight( 1f, 0.78f, 0.4f );
	}

	@Override
	public String desc(){
		if (worn) return Messages.get( this, "desc_worn" );
		if (lee) return Messages.get( this, "desc_lee" );
		float e = strength;
		return Messages.get( this, "desc",
				Math.round( 100 * (1f - Sandstorm.sightFactor( e )) ),
				Math.round( 100 * (1f - Sandstorm.speedFactor( e )) ),
				Math.round( 100 * Sandstorm.blindChance( e ) ) );
	}

	private static final String STRENGTH = "strength", WORN = "worn", LEE = "lee", REST = "rest";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( STRENGTH, strength );
		bundle.put( WORN, worn );
		bundle.put( LEE, lee );
		bundle.put( REST, rest );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		set( bundle.getFloat( STRENGTH ), bundle.getBoolean( WORN ), bundle.getBoolean( LEE ) );
		rest = bundle.getInt( REST );
	}
}

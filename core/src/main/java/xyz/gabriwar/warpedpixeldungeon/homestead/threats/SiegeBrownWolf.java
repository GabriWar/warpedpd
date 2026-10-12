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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import com.watabou.utils.Bundle;

/**
 * A brown wolf of a siege's pack (WolfSiege): every bit the brown wolf of the forest, with the siege's
 * ways on top - it circles a hero it cannot reach, tries the fences and gates, and slinks off at
 * dawn.
 */
public class SiegeBrownWolf extends BrownWolf implements SiegeWolf {

	private final WolfSiege.Pack pack = new WolfSiege.Pack();

	@Override
	public WolfSiege.Pack pack(){
		return pack;
	}

	@Override
	protected boolean act(){
		return WolfSiege.act( this, this );
	}

	@Override
	public boolean wolfAct(){
		return super.act();
	}

	@Override
	public boolean wolfStep( int cell ){
		int old = pos;
		move( cell );
		spend( 1 / speed() );
		return sprite == null || moveSprite( old, pos );
	}

	@Override
	public boolean wolfAway( int from ){
		int old = pos;
		if (!getFurther( from )) return false;
		spend( 1 / speed() );
		if (sprite != null) moveSprite( old, pos );
		return true;
	}

	@Override
	public boolean wolfBite( Breaker.Order bite ){
		return obeyBreaker( bite );
	}

	@Override
	public boolean wolfWait(){
		spend( TICK );
		return true;
	}

	@Override
	public void wolfLook(){
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView( this, fieldOfView );
	}

	@Override
	public void die( Object cause ){
		super.die( cause );
		WolfSiege.died( this );
	}

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		pack.store( bundle );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		pack.restore( bundle );
	}
}

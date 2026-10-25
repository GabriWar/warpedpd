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

package xyz.gabriwar.warpedpixeldungeon.sea;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

/**
 * In deep water with no boat under him (a boat sunk, a hero thrown overboard): the hero swims, at half
 * his pace (Seafaring.speedFactor), soaked through. For GRACE turns he keeps his head up; after that the
 * sea wears him down, a twenty-fifth of his health every other turn, until he reaches the shallows or
 * the shore - or drowns. Sharks go for anyone in the water (actors/mobs/Shark).
 */
public class Swimming extends Buff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	/** Turns he swims before the sea starts to tire him. */
	public static final int GRACE = 12;
	/** The share of his health the sea takes, every other turn past GRACE. */
	public static final int SHARE = 25;

	public int turns = 0;

	@Override
	public boolean act(){
		if (!(target instanceof Hero) || !Seafaring.swimming( target )){
			detach();
			return true;
		}
		Hero hero = (Hero) target;
		turns++;
		Buff.prolong( hero, Drenched.class, 2f );
		if (turns == GRACE){
			NetManager.heroLog( hero, GLog.NEGATIVE + Messages.get( this, "tiring" ) );
		}
		if (turns > GRACE && (turns - GRACE) % 2 == 0){
			hero.damage( Math.max( 1, hero.HT / SHARE ), this );
			if (!hero.isAlive() && hero == Dungeon.hero){
				Dungeon.fail( this );
				GLog.n( Messages.get( this, "ondeath" ) );
			}
		}
		spend( TICK );
		return true;
	}

	@Override
	public int icon(){
		return BuffIndicator.SWIMMING;
	}

	@Override
	public float iconFadePercent(){
		return Math.min( 1f, turns / (float) GRACE );
	}

	@Override
	public String iconTextDisplay(){
		return Integer.toString( Math.max( 0, GRACE - turns ) );
	}

	@Override
	public String desc(){
		return Messages.get( this, "desc", Math.max( 0, GRACE - turns ) );
	}

	private static final String TURNS = "turns";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( TURNS, turns );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		turns = bundle.getInt( TURNS );
	}
}

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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.MasterThief;
import xyz.gabriwar.warpedpixeldungeon.effects.MasterThiefCoinsFX;

/** Master Thief: the coins circling the hero, waiting to be flicked. The count is bundled by CounterBuff. */
public class MasterThiefCoins extends CounterBuff {

	public static final int MAX_COINS = 6;

	private MasterThiefCoinsFX visual;

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public int coins(){
		return Math.min( MAX_COINS, (int) count() );
	}

	public void load( int amount, int max ){
		countUp( Math.max( 0, Math.min( amount, Math.min( max, MAX_COINS ) - (int) count() ) ) );
	}

	public void spendCoin(){
		countDown( 1 );
		if (count() < 1) detach();
	}

	//only Master Thief flicks the coins away: once it is gone (taken away in the debug window) they go too
	@Override
	public boolean act(){
		if (CurrentSkills.skillLevel( target, MasterThief.class ) <= 0){
			detach();
			return true;
		}
		spend( TICK );
		return true;
	}

	@Override
	public void fx( boolean on ){
		if (visual != null){ visual.killAndErase(); visual = null; }
		if (on && target.sprite != null && target.sprite.parent != null){
			visual = new MasterThiefCoinsFX( target, this );
			target.sprite.parent.add( visual );
		}
	}
}

/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;

public class ManaRegen extends Buff {

	//a share of the pool comes back every turn: 1% while you stand still or rest,
	//half that while you move or fight, so a full pool is 100 turns of stillness
	//or 200 turns of walking. Small pools get a floor so they are not stuck at
	//fractions: at least one point every 6 turns still, every 12 on the move
	private static final float STILL_RATE = 0.010f;
	private static final float MOVING_RATE = 0.005f;
	private static final float STILL_FLOOR = 1 / 6f;
	private static final float MOVING_FLOOR = 1 / 12f;
	//a full belly steadies the mind: well fed, the pool fills half again as fast
	public static final float WELL_FED_BOOST = 1.5f;

	{
		actPriority = HERO_PRIO - 1;
	}

	private float partial = 0;
	private int lastPos = -1;

	@Override
	public boolean act() {
		if (target.isAlive()) {
			Hero hero = (Hero) target;
			int effectiveMT = hero.MT + RingOfMagic.manaBonus(hero);

			boolean still = hero.resting || (lastPos == hero.pos);
			lastPos = hero.pos;

			if (hero.MP < effectiveMT && !hero.isStarving()) {
				float rate = still ? STILL_RATE : MOVING_RATE;
				float gain = Math.max( effectiveMT * rate, still ? STILL_FLOOR : MOVING_FLOOR );
				//what speeds it: the magic level, the ring, Meditation's points and a full belly
				gain *= 1f + 0.15f * Dungeon.hero.magicLevel;
				gain *= RingOfMagic.manaRegenMultiplier(hero);
				int skillRegen = hero.heroSkills.allManaRegen();
				if (skillRegen > 0) gain *= Math.pow( 1.2, skillRegen );
				if (hero.buff( WellFed.class ) != null) gain *= WELL_FED_BOOST;
				partial += gain;
				if (partial >= 1){
					int whole = (int) partial;
					partial -= whole;
					hero.MP = Math.min( effectiveMT, hero.MP + whole );
				}
			} else {
				partial = 0;
			}
			spend(TICK);
		} else {
			detach();
		}
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	private static final String PARTIAL = "partial";

	@Override
	public void storeInBundle( com.watabou.utils.Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( PARTIAL, partial );
	}

	@Override
	public void restoreFromBundle( com.watabou.utils.Bundle bundle ) {
		super.restoreFromBundle( bundle );
		partial = bundle.getFloat( PARTIAL );
		//a save from the old scheme could be parked up to a hundred turns ahead: act soon
		float ahead = cooldown();
		if (ahead > 1) spendConstant( 1 - ahead );
	}
}

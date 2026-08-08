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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;

public class DoubleStab extends ActiveSkill1 {

	{
		name = "Double Strike";
		castText = "Too slow";
		image = 61;
		tier = 1;
		mana = 5;
	}

	/** whoever the current swing landed on; the on-hit pass always runs before doubleStab() */
	private Char lastTarget = null;

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged) lastTarget = enemy;
		return damage;
	}

	@Override
	public boolean doubleStab(){
		if (!active) return false;
		int cost = costAgainst( lastTarget );
		if (Dungeon.hero.MP < cost)
			return false;
		Dungeon.hero.MP -= cost;
		return true;
	}

	/** at mastery a poisoned mark is already half dead: the follow-up costs half as much */
	private int costAgainst( Char enemy ){
		int cost = getManaCost();
		if (level >= Skill.MAX_LEVEL && enemy != null && enemy.buff( Poison.class ) != null){
			cost = (int)Math.ceil( cost / 2f );
		}
		return cost;
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.55 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}

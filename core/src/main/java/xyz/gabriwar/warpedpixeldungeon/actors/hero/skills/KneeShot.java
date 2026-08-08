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


import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import com.watabou.utils.Random;

public class KneeShot extends PassiveSkillB2 {

	{
		name = "Knee Shot";
		castText = "Easy Target";
		image = 82;
		tier = 2;
	}

	@Override
	public boolean cripple(){
		if (Random.Int(100) < 10 * level){
			castTextYell();
			return true;
		}
		return false;
	}

	//the damage payoff against hobbled prey belongs to Groundwork; this is the follow-up
	//wound instead - a shot into a leg that is already dragging opens it right up
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (ranged && level >= 3 && enemy != null && enemy.buff( Cripple.class ) != null){
			Buff.affect( enemy, Bleeding.class ).set( 1 + level );
		}
		return damage;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}

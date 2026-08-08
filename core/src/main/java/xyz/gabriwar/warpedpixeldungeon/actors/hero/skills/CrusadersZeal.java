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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import com.watabou.utils.Random;

public class CrusadersZeal extends Skill {

	{
		name = "Crusader's Zeal";
		tag = "CBA";
		image = 152;
		tier = 4;
		level = 0;
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public float damageModifier(){
		return 1f + 0.05f * level;
	}

	//the hero's cripple() roll only fires on the ranged path, so the melee cripple is applied here instead
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level > 0 && !ranged && enemy != null && enemy.isAlive()
				&& Random.Int(100) < 8 * level){
			Buff.prolong( enemy, Cripple.class, Cripple.DURATION );
		}
		return damage;
	}
}

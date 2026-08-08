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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import com.watabou.utils.Random;

public class IronTip extends PassiveSkillB3 {

	{
		name = "Iron Tip";
		castText = "Don't forget to share...";
		image = 83;
		tier = 3;
	}

	@Override
	public int passThroughTargets(boolean shout){
		if (!shout)
			return level;
		if (level > 0 && Random.Int(level + 1) > 0){
			multiTargetActive = true;
			return level;
		}
		multiTargetActive = false;
		return 0;
	}

	//fully forged tips do not just punch through, they open the armour on the way
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (ranged && level >= 3 && enemy != null){
			Buff.prolong( enemy, Vulnerable.class, 3f );
		}
		return damage;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}

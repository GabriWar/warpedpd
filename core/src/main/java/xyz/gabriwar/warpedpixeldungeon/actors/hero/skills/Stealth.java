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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import com.watabou.utils.Random;

public class Stealth extends PassiveSkillA2 {

	{
		name = "Stealth";
		image = 50;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int stealthBonus(){ return level; }

	//Hero.defenseSkill() only reaches this for non-adjacent attackers, so it is
	//strictly a way to slip something thrown or shot at you
	@Override
	public boolean dodgeChance(){
		if (Random.Int(100) >= 6 * level) return false;
		//at mastery, being shot at is how you disappear
		if (level >= Skill.MAX_LEVEL && Dungeon.hero != null){
			Buff.affect( Dungeon.hero, Invisibility.class, 2 );
		}
		return true;
	}
}

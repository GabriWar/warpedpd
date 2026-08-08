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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Recharging;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class SpiritArmor extends PassiveSkillA3 {

	{
		name = "Spirit Armor";
		tier = 3;
		image = 27;
		level = 0;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (!active && level > 0)
			actions.add(AC_ACTIVATE);
		else if (level > 0)
			actions.add(AC_DEACTIVATE);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_ACTIVATE)){
			active = true;
		} else if (action.equals(Skill.AC_DEACTIVATE)){
			active = false;
		}
	}

	@Override
	public int incomingDamageReduction(int damage){
		if (!active)
			return 0;
		int maxReduction = (int)(damage * 0.1f * level);
		if (maxReduction == 0 && damage > 0)
			maxReduction = 1;
		if (Dungeon.hero.MP > maxReduction)
			Dungeon.hero.MP -= maxReduction;
		else {
			maxReduction = Dungeon.hero.MP;
			Dungeon.hero.MP = 0;
		}
		if (maxReduction != 0)
			GLog.p(" (Spirit Armor absorbed " + maxReduction + " damage) ");
		//arcane backwash: a heavy absorption sometimes washes back into your wands
		if (level >= MAX_LEVEL && maxReduction >= 3 && Random.Int(100) < 20)
			Buff.affect( Dungeon.hero, Recharging.class, 3f );
		return maxReduction;
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

import java.util.ArrayList;

public class WarriorPassiveA extends BranchSkill {

	{
		name = "Warrior";
		image = 160;
		level = 0;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (canUpgrade())
			actions.add(AC_ADVANCE);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (action.equals(Skill.AC_ADVANCE))
			hero.heroSkills.advance(CurrentSkills.BRANCHES.PASSIVEA);
	}

	@Override
	protected int totalSpent(){
		return Dungeon.hero.heroSkills.totalSpent(CurrentSkills.BRANCHES.PASSIVEA);
	}

	@Override
	protected int nextUpgradeCost(){
		return Dungeon.hero.heroSkills.nextUpgradeCost(CurrentSkills.BRANCHES.PASSIVEA);
	}

	@Override
	protected boolean canUpgrade(){
		return Dungeon.hero.heroSkills.canUpgrade(CurrentSkills.BRANCHES.PASSIVEA);
	}
}

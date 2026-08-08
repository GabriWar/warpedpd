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

public class DoubleShot extends ActiveSkill2 {

	{
		name = "Double Shot";
		castText = "Two for one";
		image = 90;
		tier = 2;
		mana = 5;
	}

	private boolean onDouble = false; // prevent infinite loop

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			hero.heroSkills.active3.active = false; // Disable Bombvoyage
		}
	}

	@Override
	public boolean doubleShot(){
		if (!active || Dungeon.hero.MP < getManaCost())
			return false;
		else if (!onDouble){
			onDouble = true;
			castTextYell();
			Dungeon.hero.MP -= getManaCost();
			return true;
		}
		onDouble = false;
		return false;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}
}

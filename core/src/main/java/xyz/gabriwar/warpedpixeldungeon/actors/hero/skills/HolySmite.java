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

public class HolySmite extends ActiveSkill1 {

	{
		name = "Holy Smite";
		castText = "Smite!";
		image = 111;
		mana = 3;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			hero.heroSkills.active2.active = false;
			hero.heroSkills.active3.active = false;
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.55 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public float damageModifier(){
		if (!active || Dungeon.hero.MP < getManaCost())
			return 1f;
		else {
			castTextYell();
			Dungeon.hero.MP -= getManaCost();
			return 1f + 0.1f * level;
		}
	}

	//smite splash: at max rank the blow carries into every neighbour, for a second helping of mana
	@Override
	public boolean AoEDamage(){
		if (!active || level < MAX_LEVEL || Dungeon.hero.MP < getManaCost())
			return false;
		Dungeon.hero.MP -= getManaCost();
		return true;
	}
}

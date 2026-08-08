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


public class Mastery extends PassiveSkillB3 {

	{
		name = "Mastery";
		image = 11;
		tier = 3;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//completing the branch is worth one more effective weapon level than cherry-picking
	@Override
	public int weaponLevelBonus(){
		if (level <= 0) return 0;
		return level + (CurrentSkills.skillLevel(FirmHand.class) == Skill.MAX_LEVEL ? 1 : 0);
	}
}

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


public class Poise extends PassiveSkillA2 {

	{
		name = "Poise";
		image = 84;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//the regen hooks are exponents (delay /= 1.2^bonus), so this stays small on purpose:
	//a maxed poise standing on an invested parry stance recovers one step faster still
	@Override
	public int healthRegenerationBonus(){
		return level + (level == MAX_LEVEL && CurrentSkills.skillLevel(ParryStance.class) > 0 ? 1 : 0);
	}
}

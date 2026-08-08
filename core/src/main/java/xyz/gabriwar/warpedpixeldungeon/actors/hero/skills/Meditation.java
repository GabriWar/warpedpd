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

public class Meditation extends PassiveSkillA2 {

	{
		name = "Meditation";
		image = 26;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//this is an exponent on the mana regeneration delay, not a flat gain: the deep trance
	//kicker is deliberately +1, since Meditation and Serene Focus already sum to the cap
	@Override
	public int manaRegenerationBonus(){
		if (level >= MAX_LEVEL && Dungeon.hero != null && Dungeon.hero.MP * 4 < Dungeon.hero.MT)
			return level + 1;
		return level;
	}
}

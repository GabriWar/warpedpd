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

public class Faith extends PassiveSkillA1 {

	{
		name = "Faith";
		image = 103;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){
		Dungeon.hero.MP += 5;
		Dungeon.hero.MT += 5;
		return true;
	}

	//devout reserve: the regen hook is an exponent (delay /= 1.2^bonus), and Grace + Ascetic Vow
	//already reach the sane ceiling of 6 on their own, so faith only lends its point when the
	//hero is actually in trouble - the moment Sanctuary and Last Rites need mana most
	@Override
	public int manaRegenerationBonus(){
		if (level < MAX_LEVEL || Dungeon.hero == null)
			return 0;
		return Dungeon.hero.HP < Dungeon.hero.HT / 2 ? 1 : 0;
	}
}

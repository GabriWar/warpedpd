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

public class MasterThief extends Skill {

	{
		tag = "PA4";
		name = "Master Thief";
		image = 70;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public int lootBonus( int gold ){
		if (level <= 0) return 0;
		//at max level the take itself sharpens your focus
		if (level >= Skill.MAX_LEVEL && Dungeon.hero != null){
			Dungeon.hero.MP = Math.min( Dungeon.hero.MT, Dungeon.hero.MP + gold / 25 );
		}
		return Math.round( gold * 0.15f * level );
	}

	@Override
	public int stealthBonus(){ return level; }
}

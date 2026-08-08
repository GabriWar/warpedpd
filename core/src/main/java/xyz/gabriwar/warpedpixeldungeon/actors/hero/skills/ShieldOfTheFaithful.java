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


import com.watabou.utils.Random;

public class ShieldOfTheFaithful extends Skill {

	{
		name = "Shield of the Faithful";
		tag = "CBB";
		image = 157;
		tier = 4;
		level = 0;
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public float incomingDamageModifier(){
		return 1f - 0.05f * level;
	}

	//the hero only rolls dodgeChance() against attackers that are not adjacent
	@Override
	public boolean dodgeChance(){
		return level > 0 && Random.Int(100) < 5 * level;
	}
}

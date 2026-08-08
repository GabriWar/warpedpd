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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;

public class SoulTether extends Skill {

	{
		tag = "PB4";
		name = "Soul Tether";
		tier = 4;
		image = 46;
		level = 0;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	private int tethered(){
		if (level <= 0 || Dungeon.level == null || Dungeon.level.mobs == null)
			return 0;
		int pets = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof SummonedPet && m.isAlive()) pets++;
		}
		return Math.min( 3, pets );
	}

	@Override
	public float wandDamageBonus(){
		return 1f + 0.03f * level * tethered();
	}

	@Override
	public float wandRechargeSpeedReduction(){
		return 1f - 0.02f * level * tethered();
	}
}

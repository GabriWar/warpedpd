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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;

public class LoneWolf extends Skill {

	{
		tag = "PA5A";
		name = "Lone Wolf";
		image = 133;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	private boolean alone(){
		if (level <= 0) return false;

		Hero hero = Dungeon.hero;
		if (hero == null || Dungeon.level == null || Dungeon.level.mobs == null)
			return false;

		for (Mob m : Dungeon.level.mobs){
			//any summon of yours on this floor breaks it, however far away
			if (m instanceof SummonedPet && m.isAlive()) return false;
			if (m.alignment == Char.Alignment.ALLY
					&& Dungeon.level.distance( hero.pos, m.pos ) <= 8){
				return false;
			}
		}
		return true;
	}

	@Override
	public float damageModifier(){
		return alone() ? 1f + 0.07f * level : 1f;
	}

	@Override
	public float rangedDamageModifier(){
		return alone() ? 1f + 0.07f * level : 1f;
	}

	@Override
	public float incomingDamageModifier(){
		return alone() ? 1f - 0.04f * level : 1f;
	}
}

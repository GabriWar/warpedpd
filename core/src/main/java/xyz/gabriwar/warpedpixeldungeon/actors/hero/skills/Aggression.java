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


import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;

public class Aggression extends PassiveSkillB2 {

	{
		name = "Aggression";
		image = 9;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public float damageModifier(){
		return 1f + 0.1f * level;
	}

	//fully trained, aggression becomes the payoff for everything the tree breaks:
	//melee swings hit an already-broken foe 10% harder
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (ranged || level < Skill.MAX_LEVEL || enemy == null) return damage;
		if (enemy.buff( Vulnerable.class ) != null
				|| enemy.buff( Cripple.class ) != null
				|| enemy.buff( Weakness.class ) != null
				|| enemy.buff( Bleeding.class ) != null){
			return Math.round( damage * 1.10f );
		}
		return damage;
	}
}

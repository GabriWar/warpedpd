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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import com.watabou.utils.Random;

public class Hamstring extends Skill {

	{
		tag = "D1";
		name = "Hamstring";
		castText = "Stay down";
		image = 12;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	// the engine's cripple() hook only rolls on the ranged attack path, so this
	// proc goes through onHitProc to actually fire on melee swings
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged && level > 0 && enemy != null && enemy.isAlive()
				&& Random.Int( 100 ) < 8 * level){
			Buff.prolong( enemy, Cripple.class, 3 + level );
			castTextYell();
		}
		return damage;
	}
}

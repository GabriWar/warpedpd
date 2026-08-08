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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import com.watabou.utils.Random;

public class Necrotoxin extends Skill {

	{
		tag = "PB4";
		name = "Necrotoxin";
		castText = "Rot";
		image = 71;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level > 0 && enemy != null && enemy.buff( Poison.class ) != null){
			if (Random.Int( 100 ) < 20 * level){
				Buff.affect( enemy, Poison.class ).extend( 2 + level );
				castTextYell();
			}
			return damage + Math.round( damage * 0.05f * level );
		}
		return damage;
	}
}

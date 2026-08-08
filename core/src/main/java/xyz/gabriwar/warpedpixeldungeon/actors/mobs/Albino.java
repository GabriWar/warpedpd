/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat;
import xyz.gabriwar.warpedpixeldungeon.sprites.AlbinoSprite;
import com.watabou.utils.Random;

public class Albino extends Rat {

	{
		spriteClass = AlbinoSprite.class;
		
		HP = HT = 10 + (Dungeon.depth * Random.NormalIntRange(1, 3));
		EXP = 2;

		loot = MonsterMeat.class;
		lootChance = 0.1f;
	}
	
	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (damage > 0 && Random.Int( 2 ) == 0) {
			Buff.affect( enemy, Bleeding.class ).set( Random.NormalFloat(2, 3) );
		}
		
		return damage;
	}
}

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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.sprites.SpiderGuardSprite;
import com.watabou.utils.Random;

//ported from Remixed PD's Spider Nest: heavier brood that can stun
public class SpiderGuard extends Mob {

	{
		spriteClass = SpiderGuardSprite.class;

		HP = HT = 35;
		defenseSkill = 15;

		baseSpeed = 1.2f;

		EXP = 4;
		maxLvl = 10;

		loot = MysteryMeat.class;
		lootChance = 0.067f;

	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 8, 14 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 17;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 7 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (Random.Int( 10 ) == 0) {
			Buff.prolong( enemy, Paralysis.class, 3f );
		}
		return damage;
	}
}

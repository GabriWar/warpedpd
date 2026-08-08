/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.sprites.OniSprite;
import com.watabou.utils.Random;

public class Oni extends Mob {

	{
		spriteClass = OniSprite.class;
		state = SLEEPING;

		HP = HT = 200 + (Dungeon.depth * 10);
		defenseSkill = 18 + (Math.round(Dungeon.depth / 2));

		EXP = 22;
		maxLvl = 40;

		immunities.add( Amok.class );
		immunities.add( Terror.class );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 50, 90 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 35;
	}

	@Override
	public float attackDelay() {
		return 1.5f;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 32 );
	}

	@Override
	public float spawningWeight() {
		return 0;
	}
}

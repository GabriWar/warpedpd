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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.sprites.SpiderServantSprite;
import com.watabou.utils.Random;

//ported from Remixed PD's Spider Nest: the common brood, poisonous and quick
public class SpiderServant extends Mob {

	//how many spider brood the nest and eggs are allowed to keep alive at once,
	//so the looping farm branch cannot flood itself
	public static final int POPULATION_CAP = 6;

	{
		spriteClass = SpiderServantSprite.class;

		HP = HT = 25;
		defenseSkill = 5;

		baseSpeed = 1.1f;

		EXP = 2;
		maxLvl = 9;

		loot = MysteryMeat.class;
		lootChance = 0.03f;

	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 4, 6 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 11;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 5 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (Random.Int( 4 ) == 0) {
			Buff.affect( enemy, Poison.class ).set( Random.NormalIntRange( 2, 3 ) );
		}
		return damage;
	}

	//current brood on the level, used by SpiderEgg and SpiderNest to respect the cap
	public static int population() {
		int n = 0;
		for (Mob m : Dungeon.level.mobs) {
			if (m instanceof SpiderServant || m instanceof SpiderGuard || m instanceof SpiderExploding) {
				n++;
			}
		}
		return n;
	}
}

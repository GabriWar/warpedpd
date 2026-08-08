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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blackberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blueberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cloudberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.Moonberry;
import xyz.gabriwar.warpedpixeldungeon.sprites.BrownBatSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

public class BrownBat extends Mob {

	{
		spriteClass = BrownBatSprite.class;

		HP = HT = 4;
		defenseSkill = 1;
		baseSpeed = 2f;

		EXP = 1;
		maxLvl = 15;

		flying = true;

		loot = new MonsterMeat();
		lootChance = 0.05f;
	}

	@Override
	public void rollToDropLoot() {
		super.rollToDropLoot();
		// Sprouted BERRY category: Blackberry(20), Blueberry(4), Cloudberry(16), Moonberry(2)
		if (Random.Float() < 0.05f) {
			int roll = Random.Int(42);
			Food berry;
			if (roll < 20)      berry = new Blackberry();
			else if (roll < 24) berry = new Blueberry();
			else if (roll < 40) berry = new Cloudberry();
			else                berry = new Moonberry();
			Dungeon.level.drop( berry, pos ).sprite.drop();
		}
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1, 4 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 5 + Dungeon.depth;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 1 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		if (Random.Int(10) == 0) {
			Buff.prolong( enemy, Blindness.class, Random.IntRange(3, 10) );
			GLog.w( Messages.get(this, "blind") );
			Dungeon.observe();
			state = FLEEING;
		}
		return damage;
	}

	@Override
	public void die( Object cause ) {
		if (Random.Int(5) == 0) {
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (Random.Int(2) == 0 && enemy != null) {
					mob.beckon( enemy.pos );
				}
			}
			GLog.w( Messages.get(this, "shriek") );
		}
		super.die( cause );
	}

	@Override
	public float spawningWeight() {
		return 0;
	}
}

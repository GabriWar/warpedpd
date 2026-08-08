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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLiquidFlame;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkeletonHand2Sprite;
import com.watabou.utils.Random;

public class SkeletonHand2 extends Mob {

	{
		spriteClass = SkeletonHand2Sprite.class;

		HP = HT = 200;
		defenseSkill = 30;

		EXP = 10;
		maxLvl = 20;

		flying = true;

		loot = PotionOfLiquidFlame.class;
		lootChance = 0.1f;

		properties.add(Property.UNDEAD);

		immunities.add(Burning.class);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(10, 30);
	}

	@Override
	public int attackSkill(Char target) {
		return 25;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 15);
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		if (Random.Int(2) == 0) {
			if (enemy == Dungeon.hero) {
				Buff.prolong(enemy, Roots.class, 20f);
				state = FLEEING;
			}
		}
		return damage;
	}

	@Override
	protected boolean act() {
		if (state == FLEEING
				&& buff(Terror.class) == null
				&& enemySeen
				&& enemy != null
				&& enemy.buff(Roots.class) == null) {
			state = HUNTING;
		}
		return super.act();
	}
}

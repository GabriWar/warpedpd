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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.items.food.Berry;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatBossSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class RatBoss extends Mob {

	{
		spriteClass = RatBossSprite.class;

		HP = HT = 12 + Dungeon.depth * Random.NormalIntRange(2, 5);
		defenseSkill = 5 + Dungeon.depth / 4;

		EXP = 1;

		loot = new Berry();
		lootChance = 0.5f;
	}

	private boolean spawnedRats = false;

	private static final String SPAWNED_RATS = "spawned_rats";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(SPAWNED_RATS, spawnedRats);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		spawnedRats = bundle.getBoolean(SPAWNED_RATS);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(2 + Dungeon.depth / 2, 8 + Dungeon.depth);
	}

	@Override
	public int attackSkill(Char target) {
		return 11 + Dungeon.depth;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, Dungeon.depth / 2);
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public void notice() {
		super.notice();
		yell(Messages.get(this, "notice"));
		if (!spawnedRats) {
			spawnRatPack();
			spawnedRats = true;
		}
	}

	public void spawnRatPack() {
		for (int n : PathFinder.NEIGHBOURS4) {
			int cell = pos + n;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) {
				Rat rat = new Rat();
				rat.pos = cell;
				rat.state = rat.HUNTING;
				GameScene.add(rat);
			}
		}
	}
}

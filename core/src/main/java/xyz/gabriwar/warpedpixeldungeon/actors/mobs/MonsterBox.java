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
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.MonsterBoxSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class MonsterBox extends Mob {

	private int level;

	{
		spriteClass = MonsterBoxSprite.class;

		EXP = 5;
	}

	public ArrayList<Item> items = new ArrayList<>();

	public void adjustStats(int level) {
		this.level = level;
		HT = (3 + level) * 4;
		HP = HT;
		EXP = 2 + 2 * (level - 1) / 5;
		defenseSkill = attackSkill(null) / 2;
		enemySeen = true;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(HT / 10, HT / 4);
	}

	@Override
	public int attackSkill(Char target) {
		return 9 + level;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, level);
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
	}

	@Override
	protected void dropExtraLoot() {
		for (int i = 0; i < items.size(); i++) {
			trackedDrop(items.get(i), i);
		}
	}

	private static final String ITEMS = "items";
	private static final String LEVEL = "level";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ITEMS, items);
		bundle.put(LEVEL, level);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		for (Bundlable b : bundle.getCollection(ITEMS)) {
			items.add((Item) b);
		}
		int savedHP = HP;
		adjustStats(bundle.getInt(LEVEL));
		HP = savedHP;
	}

	public static MonsterBox spawnAt(int pos, ArrayList<Item> items) {
		MonsterBox box = new MonsterBox();
		box.items = items;
		box.pos = pos;
		box.adjustStats(Dungeon.depth);
		GameScene.add(box);
		return box;
	}
}

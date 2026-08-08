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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blackberry;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.DwarfLichSprite;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class DwarfLich extends Mob {

	{
		spriteClass = DwarfLichSprite.class;

		HP = HT = 100;
		defenseSkill = 24;

		EXP = 14;

		properties.add(Property.UNDEAD);

		loot = new PotionOfHealing();
		lootChance = 0.2f;

		lootOther = new Blackberry();
		lootChanceOther = 0.333f;

		resistances.add(Poison.class);
		resistances.add(Vampiric.class);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(20, 32);
	}

	@Override
	public int attackSkill(Char target) {
		return 36;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 16);
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	protected boolean canAttack(Char enemy) {
		return !Dungeon.level.adjacent(pos, enemy.pos)
				&& new Ballistica(pos, enemy.pos, Ballistica.STOP_SOLID).collisionPos == enemy.pos;
	}

	@Override
	protected boolean getCloser(int target) {
		if (state == HUNTING) {
			return enemySeen && getFurther(target);
		} else {
			return super.getCloser(target);
		}
	}

	private static final float SPAWN_DELAY = 2f;

	public static DwarfLich spawnAt(int pos) {
		if (Dungeon.level.passable[pos] && Actor.findChar(pos) == null) {
			DwarfLich l = new DwarfLich();
			l.pos = pos;
			l.state = l.HUNTING;
			GameScene.add(l, SPAWN_DELAY);
			return l;
		}
		return null;
	}

	public static void spawnAround(int pos) {
		for (int n : PathFinder.NEIGHBOURS4) {
			int cell = pos + n;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) {
				spawnAt(cell);
			}
		}
	}
}

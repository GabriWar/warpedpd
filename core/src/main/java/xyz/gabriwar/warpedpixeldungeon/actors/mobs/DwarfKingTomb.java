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
import xyz.gabriwar.warpedpixeldungeon.items.ArmorKit;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.keys.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.DwarfKingTombSprite;
import com.watabou.utils.Random;

public class DwarfKingTomb extends Mob {

	{
		spriteClass = DwarfKingTombSprite.class;

		HP = HT = 600;
		defenseSkill = 5;

		EXP = 10;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.UNDEAD);

		loot = xyz.gabriwar.warpedpixeldungeon.items.RedDewdrop.class;
		lootChance = 0.05f;
	}

	@Override
	public int damageRoll() {
		return 0;
	}

	@Override
	public int attackSkill(Char target) {
		return 0;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 18);
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	public void beckon(int cell) {
		// ignore
	}

	@Override
	public void damage(int dmg, Object src) {
		// Check if any King mob exists on the level
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof DwarfKing && mob.isAlive()) {
				yell(Messages.get(this, "immortal"));
				return;
			}
		}
		super.damage(dmg, src);
	}

	@Override
	public void die(Object cause) {
		// Kill all DwarfKing, DwarfLich and Wraith on the level
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob instanceof DwarfKing || mob instanceof DwarfLich || mob instanceof Wraith) {
				mob.die(cause);
			}
		}

		GameScene.bossSlain();
		Dungeon.level.drop(new ArmorKit(), pos).sprite.drop();
		Dungeon.level.drop(new SkeletonKey(Dungeon.depth), pos).sprite.drop();
		Dungeon.level.drop(new Gold(Random.IntRange(4900, 10000)), pos).sprite.drop();

		super.die(cause);
	}
}

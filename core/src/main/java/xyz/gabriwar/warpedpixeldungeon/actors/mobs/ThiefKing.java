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
import xyz.gabriwar.warpedpixeldungeon.items.AdamantRing;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ThiefKingSprite;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class ThiefKing extends Mob implements Callback {

	{
		spriteClass = ThiefKingSprite.class;

		HP = HT = 500;
		defenseSkill = 28;

		EXP = 16;

		flying = true;

		loot = Generator.Category.SCROLL;
		lootChance = 0.33f;

		maxLvl = 14;

		properties.add(Property.BOSS);

		resistances.add(Electricity.class);

		declareExtraLoot(AdamantRing.class, 1f);
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(20, 70);
	}

	@Override
	public int attackSkill( Char target ) {
		return 25;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 15);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return new Ballistica(pos, enemy.pos, Ballistica.STOP_SOLID).collisionPos == enemy.pos;
	}

	@Override
	public void notice() {
		super.notice();
		if (enemy == null) return;
		yell(Messages.get(this, "notice"));
	}

	@Override
	public void die( Object cause ) {
		Dungeon.banditkingkilled = true;
		GameScene.bossSlain();
		yell(Messages.get(this, "die"));
		super.die(cause);
	}

	@Override
	protected void dropExtraLoot() {
		trackedDrop(new AdamantRing(), 0);
		trackedDrop(new Gold(Random.IntRange(1900, 4000)), 1);
	}

	@Override
	public void call() {
		next();
	}
}

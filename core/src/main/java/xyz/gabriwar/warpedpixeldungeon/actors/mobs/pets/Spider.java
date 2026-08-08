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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Web;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.PetSpiderSprite;
import com.watabou.utils.Random;

public class Spider extends PET {

	{
		spriteClass = PetSpiderSprite.class;

		flying = false;
		type = 1;
		level = 1;
		cooldown = 1000;

		regen = 1;
		regenChance = 0.1f;
	}

	@Override
	public void adjustStats(int level) {
		this.level = level;
		HT = (2 + level) * 5;
		HP = Math.min(HP, HT);
		defenseSkill = 1 + level;
	}

	@Override
	public int attackSkill(Char target) {
		return defenseSkill;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(HT / 5, HT / 2);
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);

		if (cooldown == 0) {
			Buff.affect(enemy, Poison.class).set(Random.IntRange(10, 25));
			GameScene.add(Blob.seed(enemy.pos, Random.IntRange(8, 9), Web.class));
			damage *= 2;
			cooldown = 1000;
			yell(Messages.get(this, "special"));
		} else if (Random.Int(10) == 0) {
			Buff.affect(enemy, Poison.class).set(Random.IntRange(7, 9));
			GameScene.add(Blob.seed(enemy.pos, Random.IntRange(5, 7), Web.class));
		}

		return damage;
	}

	@Override
	protected boolean act() {
		if (cooldown > 0) {
			cooldown = Math.max(cooldown - (level * level), 0);
			if (cooldown == 0) {
				yell(Messages.get(this, "ready"));
			}
		}
		return super.act();
	}
}

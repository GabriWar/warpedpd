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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.PetScorpionSprite;
import com.watabou.utils.Random;

public class Scorpion extends PET {

	{
		spriteClass = PetScorpionSprite.class;

		flying = false;
		type = 8;
		level = 1;
		cooldown = 1000;

		regen = 1;
		regenChance = 0.1f;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, level * 2);
	}

	@Override
	public void adjustStats(int level) {
		this.level = level;
		HT = (2 + level) * 8;
		HP = Math.min(HP, HT);
		defenseSkill = 1 + level * 2;
	}

	@Override
	public int attackSkill(Char target) {
		return defenseSkill;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(HT / 4, HT);
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);

		if (cooldown == 0) {
			Buff.prolong(enemy, Paralysis.class, Random.Float(1, 1.5f + level));

			// Heal self
			HP = Math.min(HP + damage, HT);
			sprite.showStatus(0x00FF00, Integer.toString(damage));
			sprite.emitter().start(Speck.factory(Speck.HEALING), 0.4f, 1);

			// Heal hero
			Dungeon.hero.HP = Math.min(Dungeon.hero.HP + damage, Dungeon.hero.HT);
			Dungeon.hero.sprite.showStatus(0x00FF00, Integer.toString(damage));
			Dungeon.hero.sprite.emitter().start(Speck.factory(Speck.HEALING), 0.4f, 1);

			damage *= 2;
			cooldown = 1000;
			yell(Messages.get(this, "special"));
		} else if (Random.Int(10) == 0) {
			Buff.prolong(enemy, Paralysis.class, Random.Float(1, 1.5f + level));
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

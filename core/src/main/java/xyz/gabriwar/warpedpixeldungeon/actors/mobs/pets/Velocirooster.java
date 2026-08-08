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
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.VelociroosterSprite;
import com.watabou.utils.Random;

public class Velocirooster extends PET {

	{
		spriteClass = VelociroosterSprite.class;

		flying = false;
		type = 3;
		level = 1;
		cooldown = 1000;

		regen = 1;
		regenChance = 0.05f;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, level * 3);
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
		if (cooldown == 0) {
			int dmg = Random.NormalIntRange(HT / 2, HT);
			yell(Messages.get(this, "special"));
			cooldown = 1000;
			return dmg;
		}
		return Random.NormalIntRange(HT / 5, HT / 2);
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

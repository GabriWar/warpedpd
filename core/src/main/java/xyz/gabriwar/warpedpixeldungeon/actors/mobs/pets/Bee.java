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
import xyz.gabriwar.warpedpixeldungeon.sprites.SteelBeeSprite;
import com.watabou.utils.Random;

public class Bee extends PET {

	{
		spriteClass = SteelBeeSprite.class;

		flying = true;
		type = 2;
		level = 1;

		regen = 1;
		regenChance = 0.1f;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, level * 4);
	}

	@Override
	public void adjustStats(int level) {
		this.level = level;
		defenseSkill = 1 + level * 2;
		HT = (2 + level) * 8;
		HP = Math.min(HP, HT);
	}

	@Override
	public int attackSkill(Char target) {
		return defenseSkill;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(HT / 5, HT / 2);
	}
}

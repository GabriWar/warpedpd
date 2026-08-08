/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.sprites.MagicOrbSprite;

public class MagicOrb extends NPC {

	{
		spriteClass = MagicOrbSprite.class;
		HP = HT = 1;
	}

	@Override
	public void damage(int dmg, Object src) {
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob != this && Dungeon.level.heroFOV[mob.pos]) {
				mob.damage(dmg, src);
				mob.sprite.burst(0x66CCFF, 5);
			}
		}
	}

	@Override
	public boolean add(Buff buff) {
		damage(1, this);
		HP = 0;
		destroy();
		sprite.die();
		return true;
	}

	@Override
	public boolean interact(Char hero) {
		// Spend the interactor's turn, not the host's hero turn (was Dungeon.hero
		// previously which burned the host's turn when a remote player clicked).
		if (hero instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero) {
			((xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero) hero).spendAndNext(1f);
		}
		return true;
	}
}

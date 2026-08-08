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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

public class SleepParalysisDemon extends NPC {

	{
		spriteClass = CharSprite.class;
		HP = HT = 1;
	}

	@Override
	public void damage(int dmg, Object src) {
	}

	@Override
	public boolean add(Buff buff) {
		return true;
	}

	@Override
	public boolean interact(Char hero) {
		// Apply turn-burn + paralysis to the interactor, not Dungeon.hero (host).
		// Previously a remote player who clicked this demon paralyzed the host.
		if (hero instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero) {
			xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h =
					(xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero) hero;
			h.spendAndNext(1f);
			Buff.prolong(h, Paralysis.class, Paralysis.DURATION);
		}
		destroy();
		sprite.die();
		return true;
	}
}

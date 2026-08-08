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

package xyz.gabriwar.warpedpixeldungeon.items.food;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

public class GoldenJelly extends Food {

	{
		image = ItemSpriteSheet.GOLDEN_JELLY;
		energy = (Hunger.STARVING - Hunger.HUNGRY) / 10f;
		bones = false;
	}

	@Override
	public void execute(Hero hero, String action) {
		if (action.equals(AC_EAT) && Dungeon.bossLevel()) {
			GLog.w( Messages.get(this, "preventing") );
			return;
		}
		super.execute(hero, action);
	}

	@Override
	protected void satisfy(Hero hero) {
		super.satisfy(hero);

		GLog.w( Messages.get(this, "effect") );

		switch (Random.Int(10)) {
			case 1:
				for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
					Buff.prolong(mob, Roots.class, 20);
				}
				Buff.affect(hero, Vertigo.class, 1f);
				break;
			default:
				for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
					Buff.prolong(mob, Roots.class, 10);
				}
				Buff.affect(hero, Vertigo.class, 3f);
				break;
		}
	}

	@Override
	public int value() {
		return 20 * quantity;
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BerryRegeneration;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Levitation;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

public class Cloudberry extends Food {

	private static final float LEVITATION_DURATION = 10f;

	{
		image = ItemSpriteSheet.CLOUDBERRY_FOOD;
		energy = (Hunger.STARVING - Hunger.HUNGRY) / 10f; //same as every other berry
		bones = false;
	}

	@Override
	protected void satisfy(Hero hero) {
		super.satisfy(hero);

		switch (Random.Int(10)) {
			case 0: case 1: case 2: case 3: case 4: case 5:
				Buff.affect(hero, Haste.class, Haste.DURATION);
				GLog.p( Messages.get(this, "haste") );
				break;
			case 6: case 7: case 8:
				Buff.affect(hero, Haste.class, Haste.DURATION);
				if (Dungeon.depth < 51) {
					Buff.affect(hero, Levitation.class, LEVITATION_DURATION);
					GLog.p( Messages.get(this, "float") );
				}
				GLog.p( Messages.get(this, "haste") );
				break;
			case 9:
				Buff.affect(hero, Haste.class, Haste.DURATION);
				if (Dungeon.depth < 51) {
					Buff.affect(hero, Levitation.class, LEVITATION_DURATION * 2);
					GLog.p( Messages.get(this, "float") );
				}
				GLog.p( Messages.get(this, "haste") );
				GLog.p( Messages.get(this, "energy") );
				Buff.affect(hero, BerryRegeneration.class).set(hero.HT);
				break;
		}
	}

	@Override
	public int value() {
		return 20 * quantity;
	}
}

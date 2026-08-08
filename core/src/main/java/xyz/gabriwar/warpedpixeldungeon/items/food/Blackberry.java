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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

public class Blackberry extends Food {

	{
		image = ItemSpriteSheet.BLACKBERRY_FOOD;
		energy = (Hunger.STARVING - Hunger.HUNGRY) / 10f;
		bones = false;
	}

	@Override
	protected void satisfy(Hero hero) {
		super.satisfy(hero);

		if (Random.Int(10) == 1) {
			GLog.p( Messages.get(this, "trippy") );
			Buff.affect(hero, MindVision.class, MindVision.DURATION);
			Dungeon.observe();

			if (Dungeon.level.mobs.size() > 0) {
				GLog.i( Messages.get(this, "sense_others") );
			} else {
				GLog.i( Messages.get(this, "sense_alone") );
			}
			Buff.affect(hero, BerryRegeneration.class).set(hero.HT * 2);
			GLog.p( Messages.get(this, "energy") );
		} else {
			GLog.p( Messages.get(this, "energy") );
			Buff.affect(hero, BerryRegeneration.class).set(hero.HT / 2);
		}
	}

	@Override
	public int value() {
		return 20 * quantity;
	}
}

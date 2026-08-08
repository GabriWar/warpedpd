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

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barkskin;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

public class ToastedNut extends Nut {

	{
		image = ItemSpriteSheet.TOASTED_NUT;
		energy = Hunger.STARVING - Hunger.HUNGRY;
	}

	@Override
	protected void satisfy(Hero hero) {
		super.satisfy(hero);

		GLog.p( Messages.get(this, "blessing") );
		switch (Random.Int(2)) {
			case 0:
				Barkskin.conditionallyAppend(hero, hero.HT, 1);
				break;
			case 1:
				Barkskin.conditionallyAppend(hero, hero.HT * 2, 1);
				break;
		}
	}

	@Override
	public int value() {
		return 20 * quantity;
	}

	public static Food cook(Nut ingredient) {
		ToastedNut result = new ToastedNut();
		result.quantity = ingredient.quantity();
		return result;
	}
}

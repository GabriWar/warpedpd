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

package xyz.gabriwar.warpedpixeldungeon.items.potions;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfMana extends Potion {

	{
		icon = ItemSpriteSheet.Icons.POTION_MANA;
	}

	{
		bones = true;
	}

	@Override
	public void apply(Hero hero) {
		identify();
		restoreMana(Dungeon.hero);
	}

	public static void restoreMana(Hero hero) {
		int restored = hero.MT - hero.MP;
		hero.MP = hero.MT;

		if (restored > 0) {
			hero.sprite.emitter().start(Speck.factory(Speck.UP), 0.2f, 3);
			GLog.p(Messages.get(PotionOfMana.class, "mana_restored", restored));
		} else {
			GLog.i(Messages.get(PotionOfMana.class, "mana_full"));
		}
	}

	@Override
	public int value() {
		return isIdentified() ? 30 * quantity : super.value();
	}
}

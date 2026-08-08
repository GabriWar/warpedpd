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

package xyz.gabriwar.warpedpixeldungeon.items.potions.exotic;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfHoly extends ExoticPotion {

	{
		icon = ItemSpriteSheet.Icons.POTION_HOLY;
	}

	@Override
	public void apply(Hero hero) {
		identify();

		GameScene.flash(0x80FFFFFF);

		//clearing the `cursed` flag by hand is not the same as lifting a curse: curse enchants
		//and glyphs survived it, cursed wands kept their sabotaged level, cursedKnown stayed
		//false, and HT/quickslot never refreshed. ScrollOfRemoveCurse.uncurse() does all of it.
		ArrayList<Item> cursed = new ArrayList<>();
		for (Item item : hero.belongings) {
			if (item != null && item.cursed) {
				cursed.add(item);
			}
		}
		for (Heap heap : Dungeon.level.heaps.valueList()) {
			for (Item item : heap.items) {
				if (item.cursed) {
					cursed.add(item);
				}
			}
		}

		int count = cursed.size();
		if (count > 0) {
			ScrollOfRemoveCurse.uncurse(hero, cursed.toArray(new Item[0]));
			GLog.p(Messages.get(this, "uncursed", count));
		} else {
			GLog.i(Messages.get(this, "no_objects"));
		}
	}
}

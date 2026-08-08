/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

package xyz.gabriwar.warpedpixeldungeon.items.spells;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

public class MagicalPorter extends InventorySpell {

	{
		image = ItemSpriteSheet.MAGIC_PORTER;
		talentChance = 1 / (float) Recipe.OUT_QUANTITY;
	}

	@Override
	protected void onCast(Hero hero) {
		int portDepth = 5 * (1 + Dungeon.depth / 5);
		if (portDepth > 25) {
			GLog.w(Messages.get(this, "nowhere"));
		} else {
			super.onCast(hero);
		}
	}

	@Override
	protected boolean usableOnItem(Item item) {
		//an equipped item is not in the backpack, so detachAll() below would leave it
		//equipped while still queueing a copy at the destination — duplicating it.
		if (Dungeon.hero != null && item.isEquipped(Dungeon.hero)) return false;
		return !(item instanceof MagicalPorter);
	}

	@Override
	protected void onItemSelected(Item item) {
		int portDepth = 5 * (1 + Dungeon.depth / 5);
		Item result = item.detachAll(curUser.belongings.backpack);
		//defence in depth: never queue a copy of something we failed to take
		if (result == null) return;
		ArrayList<Item> dropped = Dungeon.droppedItems.get(portDepth);
		if (dropped == null) {
			Dungeon.droppedItems.put(portDepth, dropped = new ArrayList<>());
		}
		dropped.add(result);
	}

	@Override
	public int value() {
		return Math.round(quantity * ((5 + 40) / 8f));
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		static final int OUT_QUANTITY = 8;

		{
			inputs     = new Class[]{ScrollOfTeleportation.class, ScrollOfMagicMapping.class};
			inQuantity = new int[]{1, 1};
			cost       = 4;
			output     = MagicalPorter.class;
			outQuantity = OUT_QUANTITY;
		}
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.items.scrolls;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class ScrollOfMagicalInfusion extends InventoryScroll {

	{
		icon = ItemSpriteSheet.Icons.SCROLL_MAGICINFUSE;
	}

	{
		unique = true;
		bones = true;
	}

	@Override
	protected boolean usableOnItem(Item item) {
		return item instanceof Weapon || item instanceof Armor;
	}

	@Override
	protected void onItemSelected(Item item) {

		ScrollOfRemoveCurse.uncurse(curUser, item);

		if (item instanceof Weapon) {
			((Weapon) item).upgrade(true);
		} else {
			((Armor) item).upgrade(true);
		}

		GLog.p(Messages.get(this, "infuse", item.name()));

		Badges.validateItemLevelAquired(item);

		curUser.sprite.emitter().start(Speck.factory(Speck.UP), 0.2f, 3);
	}

	@Override
	public int value() {
		return isKnown() ? 60 * quantity : super.value();
	}
}

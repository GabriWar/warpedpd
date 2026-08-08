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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfImmortality extends ExoticPotion {

	{
		icon = ItemSpriteSheet.Icons.POTION_IMMORTALITY;
	}

	@Override
	public void apply(Hero hero) {
		identify();

		//the dew vial was reworked into the Waterskin — top it off (the hero always
		//carries one), reviving this potion which otherwise no-ops on a vial nobody holds
		Waterskin skin = hero.belongings.getItem(Waterskin.class);
		if (skin != null) {
			skin.fill();
			GLog.p(Messages.get(this, "filled", hero.STR));
		} else {
			GLog.n(Messages.get(this, "no_vial"));
		}
	}
}

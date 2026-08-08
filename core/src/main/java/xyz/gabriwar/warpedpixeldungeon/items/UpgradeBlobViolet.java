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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;

import java.util.ArrayList;

public class UpgradeBlobViolet extends Item {

	private static final float TIME_TO_UPGRADE = 2;
	private static final int UPGRADES = 5;

	private static final String AC_UPGRADE = "UPGRADE";

	{
		image = ItemSpriteSheet.UPGRADEGOO_VIOLET;
		stackable = true;
		bones = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_UPGRADE);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_UPGRADE)) {
			curUser = hero;
			GameScene.selectItem(itemSelector);
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	private void upgrade(Item item) {
		detach(curUser.belongings.backpack);

		//reinforced items can be pushed past +15; normal items are capped at +15
		if (item.reinforced) {
			item.upgrade(UPGRADES);
		} else {
			item.upgrade(Math.min(UPGRADES, 15 - item.level()));
		}

		GLog.p( Messages.get(UpgradeBlobViolet.class, "upgraded", item.name()) );

		curUser.sprite.operate(curUser.pos);
		curUser.sprite.emitter().start(Speck.factory(Speck.UP), 0.2f, 3);
		Badges.validateItemLevelAquired(item);

		curUser.spend(TIME_TO_UPGRADE);
		curUser.busy();
	}

	@Override
	public int value() {
		return 30 * quantity;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	private final WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(UpgradeBlobViolet.class, "select");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item.isUpgradable();
		}

		@Override
		public void onSelect(Item item) {
			if (item != null) {
				UpgradeBlobViolet.this.upgrade(item);
			}
		}
	};
}

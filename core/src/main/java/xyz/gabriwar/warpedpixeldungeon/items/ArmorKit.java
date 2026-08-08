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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClassArmor;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class ArmorKit extends Item {

	private static final float TIME_TO_UPGRADE = 2;

	private static final String AC_APPLY = "APPLY";

	{
		image = ItemSpriteSheet.KIT;

		unique = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_APPLY);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_APPLY)) {
			curUser = hero;
			GameScene.selectItem(itemSelector);
		}
	}

	private void upgrade(Armor armor) {

		detach(curUser.belongings.backpack);

		curUser.sprite.centerEmitter().start(Speck.factory(Speck.EVOKE), 0.05f, 10);
		curUser.spend(TIME_TO_UPGRADE);
		curUser.busy();

		GLog.w(Messages.get(this, "upgraded", armor.name()));

		ClassArmor classArmor = ClassArmor.upgrade(curUser, armor);
		if (curUser.belongings.armor == armor) {

			curUser.belongings.armor = classArmor;
			((HeroSprite) curUser.sprite).updateArmor();

		} else {

			armor.detach(curUser.belongings.backpack);
			classArmor.collect(curUser.belongings.backpack);

		}

		curUser.sprite.operate(curUser.pos);
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 50 * quantity;
	}

	private final WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return Messages.get(ArmorKit.class, "select");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof Armor && !(item instanceof ClassArmor);
		}

		@Override
		public void onSelect(Item item) {
			if (item != null) {
				ArmorKit.this.upgrade((Armor) item);
			}
		}
	};
}

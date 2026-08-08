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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Belongings;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantArmor;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantRing;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWand;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.ItemButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndBlacksmith2 extends Window {

	private static final int WIDTH    = 120;
	private static final int BTN_SIZE = 32;
	private static final float GAP     = 2;
	private static final float BTN_GAP = 5;

	private ItemButton btnPressed;

	private ItemButton btnItem1;
	private ItemButton btnItem2;
	private RedButton btnReforge;

	public WndBlacksmith2( Blacksmith2 troll, Hero hero ) {

		super();

		IconTitle titlebar = new IconTitle();
		titlebar.icon( troll.sprite() );
		titlebar.label( Messages.titleCase( troll.name() ) );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		RenderedTextBlock message = PixelScene.renderTextBlock( Messages.get(this, "prompt"), 6 );
		message.maxWidth( WIDTH );
		message.setPos( 0, titlebar.bottom() + GAP );
		add( message );

		btnItem1 = new ItemButton() {
			@Override
			protected void onClick() {
				btnPressed = btnItem1;
				GameScene.selectItem( itemSelector );
			}
		};
		btnItem1.setRect( (WIDTH - BTN_GAP) / 2 - BTN_SIZE, message.top() + message.height() + BTN_GAP, BTN_SIZE, BTN_SIZE );
		add( btnItem1 );

		btnItem2 = new ItemButton() {
			@Override
			protected void onClick() {
				btnPressed = btnItem2;
				GameScene.selectItem( adamantSelector );
			}
		};
		btnItem2.setRect( btnItem1.right() + BTN_GAP, btnItem1.top(), BTN_SIZE, BTN_SIZE );
		add( btnItem2 );

		btnReforge = new RedButton( Messages.get(this, "reinforce") ) {
			@Override
			protected void onClick() {
				Blacksmith2.upgrade( btnItem1.item(), btnItem2.item() );
				hide();
			}
		};
		btnReforge.enable( false );
		btnReforge.setRect( 0, btnItem1.bottom() + BTN_GAP, WIDTH, 20 );
		add( btnReforge );

		resize( WIDTH, (int) btnReforge.bottom() );
	}

	private WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return Messages.get(WndBlacksmith2.class, "select_item");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable( Item item ) {
			return item.isIdentified() && !item.cursed && !item.reinforced
					&& item.isUpgradable() && item.level() >= 0;
		}

		@Override
		public void onSelect( Item item ) {
			if (item != null && btnPressed.parent != null) {
				btnPressed.item( item );
				checkReady();
			}
		}
	};

	private WndBag.ItemSelector adamantSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return Messages.get(WndBlacksmith2.class, "select_adamant");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable( Item item ) {
			return item instanceof AdamantArmor
					|| item instanceof AdamantWeapon
					|| item instanceof AdamantWand
					|| item instanceof AdamantRing;
		}

		@Override
		public void onSelect( Item item ) {
			if (item != null && btnPressed.parent != null) {
				btnPressed.item( item );
				checkReady();
			}
		}
	};

	private void checkReady() {
		if (btnItem1.item() != null && btnItem2.item() != null) {
			String result = Blacksmith2.verify( btnItem1.item(), btnItem2.item() );
			if (result != null) {
				GameScene.show( new WndMessage( result ) );
				btnReforge.enable( false );
			} else {
				btnReforge.enable( true );
			}
		}
	}
}

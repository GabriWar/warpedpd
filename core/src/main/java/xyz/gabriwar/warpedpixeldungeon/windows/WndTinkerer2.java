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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer2;
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo2;
import xyz.gabriwar.warpedpixeldungeon.items.InactiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.Mushroom;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

public class WndTinkerer2 extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 20;
	private static final float GAP      = 2;

	public WndTinkerer2( final Tinkerer2 tinkerer, final Item item, final Item mrd ) {

		super();

		IconTitle titlebar = new IconTitle();
		titlebar.icon( new ItemSprite( item.image(), null ) );
		titlebar.label( Messages.titleCase( item.name() ) );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		if (mrd instanceof InactiveMrDestructo) {

			RenderedTextBlock message = PixelScene.renderTextBlock(
					Messages.get(this, "message_inactive"), 6 );
			message.maxWidth( WIDTH );
			message.setPos( 0, titlebar.bottom() + GAP );
			add( message );

			RedButton btnUpgrade = new RedButton( Messages.get(this, "upgrade") ) {
				@Override
				protected void onClick() {
					selectUpgrade( tinkerer );
				}
			};
			btnUpgrade.setRect( 0, message.top() + message.height() + GAP, WIDTH, BTN_HEIGHT );
			add( btnUpgrade );

			RedButton btnRecharge = new RedButton( Messages.get(this, "recharge") ) {
				@Override
				protected void onClick() {
					selectRecharge( tinkerer );
				}
			};
			btnRecharge.setRect( 0, btnUpgrade.bottom() + GAP, WIDTH, BTN_HEIGHT );
			add( btnRecharge );

			resize( WIDTH, (int) btnRecharge.bottom() );

		} else if (mrd instanceof ActiveMrDestructo) {

			RenderedTextBlock message = PixelScene.renderTextBlock(
					Messages.get(this, "message_active"), 6 );
			message.maxWidth( WIDTH );
			message.setPos( 0, titlebar.bottom() + GAP );
			add( message );

			RedButton btnUpgrade = new RedButton( Messages.get(this, "upgrade") ) {
				@Override
				protected void onClick() {
					selectUpgradePlus( tinkerer );
				}
			};
			btnUpgrade.setRect( 0, message.top() + message.height() + GAP, WIDTH, BTN_HEIGHT );
			add( btnUpgrade );

			resize( WIDTH, (int) btnUpgrade.bottom() );

		} else {

			RenderedTextBlock message = PixelScene.renderTextBlock(
					Messages.get(this, "message_new"), 6 );
			message.maxWidth( WIDTH );
			message.setPos( 0, titlebar.bottom() + GAP );
			add( message );

			RedButton btnNew = new RedButton( Messages.get(this, "take_new") ) {
				@Override
				protected void onClick() {
					selectNew( tinkerer );
				}
			};
			btnNew.setRect( 0, message.top() + message.height() + GAP, WIDTH, BTN_HEIGHT );
			add( btnNew );

			resize( WIDTH, (int) btnNew.bottom() );
		}
	}

	private void selectUpgrade( Tinkerer2 tinkerer ) {
		hide();

		Mushroom mushroom = Dungeon.hero.belongings.getItem( Mushroom.class );
		mushroom.detach( Dungeon.hero.belongings.backpack );

		InactiveMrDestructo inmrd = Dungeon.hero.belongings.getItem( InactiveMrDestructo.class );
		inmrd.detach( Dungeon.hero.belongings.backpack );

		ActiveMrDestructo2 mrd2 = new ActiveMrDestructo2();
		if (mrd2.doPickUp( Dungeon.hero )) {
			GLog.i( Messages.get(this, "you_now_have", mrd2.name()) );
		} else {
			Dungeon.level.drop( mrd2, Dungeon.hero.pos ).sprite.drop();
		}

		farewell( tinkerer );
	}

	private void selectUpgradePlus( Tinkerer2 tinkerer ) {
		hide();

		Mushroom mushroom = Dungeon.hero.belongings.getItem( Mushroom.class );
		mushroom.detach( Dungeon.hero.belongings.backpack );

		ActiveMrDestructo2 mrd2 = new ActiveMrDestructo2();
		if (mrd2.doPickUp( Dungeon.hero )) {
			GLog.i( Messages.get(this, "you_now_have", mrd2.name()) );
		} else {
			Dungeon.level.drop( mrd2, Dungeon.hero.pos ).sprite.drop();
		}

		farewell( tinkerer );
	}

	private void selectRecharge( Tinkerer2 tinkerer ) {
		hide();

		Mushroom mushroom = Dungeon.hero.belongings.getItem( Mushroom.class );
		mushroom.detach( Dungeon.hero.belongings.backpack );

		InactiveMrDestructo inmrd = Dungeon.hero.belongings.getItem( InactiveMrDestructo.class );
		inmrd.detach( Dungeon.hero.belongings.backpack );

		ActiveMrDestructo mrd = new ActiveMrDestructo();
		if (mrd.doPickUp( Dungeon.hero )) {
			GLog.i( Messages.get(this, "you_now_have", mrd.name()) );
		} else {
			Dungeon.level.drop( mrd, Dungeon.hero.pos ).sprite.drop();
		}

		ActiveMrDestructo mrds = new ActiveMrDestructo();
		if (mrds.doPickUp( Dungeon.hero )) {
			GLog.i( Messages.get(this, "you_now_have", mrds.name()) );
		} else {
			Dungeon.level.drop( mrds, Dungeon.hero.pos ).sprite.drop();
		}

		farewell( tinkerer );
	}

	private void selectNew( Tinkerer2 tinkerer ) {
		hide();

		Mushroom mushroom = Dungeon.hero.belongings.getItem( Mushroom.class );
		mushroom.detach( Dungeon.hero.belongings.backpack );

		ActiveMrDestructo mrd = new ActiveMrDestructo();
		if (mrd.doPickUp( Dungeon.hero )) {
			GLog.i( Messages.get(this, "you_now_have", mrd.name()) );
		} else {
			Dungeon.level.drop( mrd, Dungeon.hero.pos ).sprite.drop();
		}

		farewell( tinkerer );
	}

	private void farewell( Tinkerer2 tinkerer ) {
		tinkerer.yell( Messages.get(this, "farewell", Dungeon.hero.name()) );
		tinkerer.destroy();
		tinkerer.sprite.die();
	}
}

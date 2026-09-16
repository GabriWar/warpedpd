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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer1;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.Mushroom;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndTinkerer extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 20;
	private static final float GAP      = 2;

	public WndTinkerer( final Tinkerer1 tinkerer, final Item item ) {

		super();

		IconTitle titlebar = new IconTitle();
		titlebar.icon( new ItemSprite( item.image(), null ) );
		titlebar.label( Messages.titleCase( item.name() ) );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		RenderedTextBlock message = PixelScene.renderTextBlock(
				Messages.get(this, "message"), 6 );
		message.maxWidth( WIDTH );
		message.setPos( 0, titlebar.bottom() + GAP );
		add( message );

		RedButton btnCondenser = new RedButton( Messages.get(this, "condenser") ) {
			@Override
			protected void onClick() {
				selectUpgrade( tinkerer, 1 );
			}
		};
		btnCondenser.setRect( 0, message.top() + message.height() + GAP, WIDTH, BTN_HEIGHT );
		add( btnCondenser );

		RedButton btnMeasure = new RedButton( Messages.get(this, "measure") ) {
			@Override
			protected void onClick() {
				selectUpgrade( tinkerer, 2 );
			}
		};
		btnMeasure.setRect( 0, btnCondenser.bottom() + GAP, WIDTH, BTN_HEIGHT );
		add( btnMeasure );

		RedButton btnInfo = new RedButton( Messages.get(this, "info") ) {
			@Override
			protected void onClick() {
				GameScene.show( new WndTinkererInfo() );
			}
		};
		btnInfo.setRect( 0, btnMeasure.bottom() + GAP, WIDTH, BTN_HEIGHT );
		add( btnInfo );

		resize( WIDTH, (int) btnInfo.bottom() );
	}

	private void selectUpgrade( Tinkerer1 tinkerer, int type ) {

		hide();

		Mushroom mushroom = Dungeon.hero.belongings.getItem( Mushroom.class );
		mushroom.detach( Dungeon.hero.belongings.backpack );

		if (type == 1) {
			Dungeon.dewCondenser = true;
			tinkerer.yell( Messages.get(this, "farewell_condenser", Dungeon.hero.name()) );
		} else {
			Dungeon.measuredDraught = true;
			tinkerer.yell( Messages.get(this, "farewell_measure", Dungeon.hero.name()) );
		}

		tinkerer.destroy();
		tinkerer.sprite.die();
	}
}

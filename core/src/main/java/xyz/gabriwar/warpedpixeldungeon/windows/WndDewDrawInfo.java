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

import xyz.gabriwar.warpedpixeldungeon.items.DewVial;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndDewDrawInfo extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 20;
	private static final float GAP      = 2;

	public WndDewDrawInfo() {

		super();

		DewVial dewvial = new DewVial();

		IconTitle titlebar = new IconTitle();
		titlebar.icon( new ItemSprite( dewvial.image(), null ) );
		titlebar.label( Messages.titleCase( dewvial.name() ) );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		RenderedTextBlock message1 = PixelScene.renderTextBlock(
				Messages.get(this, "message1"), 6 );
		message1.maxWidth( WIDTH );
		message1.setPos( 0, titlebar.bottom() + GAP );
		add( message1 );

		RenderedTextBlock message2 = PixelScene.renderTextBlock(
				Messages.get(this, "message2"), 6 );
		message2.maxWidth( WIDTH );
		message2.setPos( 0, message1.top() + message1.height() + GAP );
		add( message2 );

		RenderedTextBlock message3 = PixelScene.renderTextBlock(
				Messages.get(this, "message3"), 6 );
		message3.maxWidth( WIDTH );
		message3.setPos( 0, message2.top() + message2.height() + GAP );
		add( message3 );

		RenderedTextBlock message4 = PixelScene.renderTextBlock(
				Messages.get(this, "message4"), 6 );
		message4.maxWidth( WIDTH );
		message4.setPos( 0, message3.top() + message3.height() + GAP );
		add( message4 );

		RedButton btnOkay = new RedButton( Messages.get(this, "okay") ) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		btnOkay.setRect( 0, message4.top() + message4.height() + GAP, WIDTH, BTN_HEIGHT );
		add( btnOkay );

		resize( WIDTH, (int) btnOkay.bottom() );
	}
}

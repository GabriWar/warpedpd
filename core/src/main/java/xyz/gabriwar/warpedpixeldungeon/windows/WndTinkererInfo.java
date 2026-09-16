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

import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndTinkererInfo extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 20;
	private static final float GAP      = 2;

	public WndTinkererInfo() {

		super();

		Waterskin skin = new Waterskin();

		IconTitle titlebar = new IconTitle();
		titlebar.icon( new ItemSprite( skin.image(), null ) );
		titlebar.label( Messages.titleCase( skin.name() ) );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		RenderedTextBlock condenser = PixelScene.renderTextBlock(
				Messages.get(this, "condenser"), 6 );
		condenser.maxWidth( WIDTH );
		condenser.setPos( 0, titlebar.bottom() + GAP );
		add( condenser );

		RenderedTextBlock attune = PixelScene.renderTextBlock(
				Messages.get(this, "measure"), 6 );
		attune.maxWidth( WIDTH );
		attune.setPos( 0, condenser.bottom() + GAP * 2 );
		add( attune );

		RedButton btnOkay = new RedButton( Messages.get(this, "okay") ) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		btnOkay.setRect( 0, attune.bottom() + GAP, WIDTH, BTN_HEIGHT );
		add( btnOkay );

		resize( WIDTH, (int) btnOkay.bottom() );
	}
}

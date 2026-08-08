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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.PET;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfHaste;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

public class WndPetHaste extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 20;
	private static final float GAP      = 2;

	public WndPetHaste( final PET pet, final RingOfHaste ring ) {

		super();

		IconTitle titlebar = new IconTitle();
		titlebar.icon( pet.sprite() );
		titlebar.label( Messages.titleCase( pet.name() ) );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		RenderedTextBlock message = PixelScene.renderTextBlock(
				Messages.get(this, "message"), 6 );
		message.maxWidth( WIDTH );
		message.setPos( 0, titlebar.bottom() + GAP );
		add( message );

		RedButton btnYes = new RedButton( Messages.get(this, "yes") ) {
			@Override
			protected void onClick() {
				Dungeon.petHasteLevel = ring.buffedLvl();
				GLog.p( Messages.get(WndPetHaste.class, "hasted", pet.name()) );
				hide();
			}
		};
		btnYes.setRect( 0, message.top() + message.height() + GAP, WIDTH, BTN_HEIGHT );
		add( btnYes );

		RedButton btnNo = new RedButton( Messages.get(this, "no") ) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		btnNo.setRect( 0, btnYes.bottom() + GAP, WIDTH, BTN_HEIGHT );
		add( btnNo );

		resize( WIDTH, (int) btnNo.bottom() );
	}
}

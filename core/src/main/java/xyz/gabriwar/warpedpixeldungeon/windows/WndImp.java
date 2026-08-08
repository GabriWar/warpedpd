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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Imp;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DwarfToken;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndImp extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 20;
	private static final int GAP        = 2;

	// Local (host's own hero): applies via Imp.Quest.claimReward on confirm.
	public WndImp( final Imp imp, final Hero hero ) {
		this( hero, -1 );
	}

	// Remote (client): confirm ships the choice; the host applies it.
	public WndImp( int dialogId ) {
		this( Dungeon.hero, dialogId );
	}

	private WndImp( final Hero hero, final int dialogId ) {

		super();

		DwarfToken tokens = hero == null ? null : hero.belongings.getItem( DwarfToken.class );
		int img = tokens != null ? tokens.image() : new DwarfToken().image();

		IconTitle titlebar = new IconTitle();
		titlebar.icon( new ItemSprite( img, null ) );
		titlebar.label( Messages.titleCase( new DwarfToken().name() ) );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		RenderedTextBlock message = PixelScene.renderTextBlock( Messages.get(this, "message"), 6 );
		message.maxWidth(WIDTH);
		message.setPos(0, titlebar.bottom() + GAP);
		add( message );

		RedButton btnReward = new RedButton( Messages.get(this, "reward") ) {
			@Override
			protected void onClick() {
				hide();
				if (dialogId >= 0) {
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.sendDialogChoice( dialogId, "take" );
				} else {
					Imp.Quest.claimReward( hero );
				}
			}
		};
		btnReward.setRect( 0, message.top() + message.height() + GAP, WIDTH, BTN_HEIGHT );
		add( btnReward );

		resize( WIDTH, (int)btnReward.bottom() );
	}
}

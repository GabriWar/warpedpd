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

import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.SupporterScene;
import xyz.gabriwar.warpedpixeldungeon.services.payments.Payments;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndSupportPrompt extends Window {

	protected static final int WIDTH_P    = 120;
	protected static final int WIDTH_L    = 200;

	public WndSupportPrompt(){

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		IconTitle title = new IconTitle(Icons.get(Icons.WPD), Messages.get(WndSupportPrompt.class, "title"));
		title.setRect( 0, 0, width, 0 );
		add(title);

		String message = Messages.get(WndSupportPrompt.class, "intro");
		//store builds must not name external payment pages (Play policy), so they
		//get the in-app pitch; everything else points at Ko-fi
		message += "\n\n" + Messages.get(SupporterScene.class,
				Payments.service != null ? "store_msg" : "patreon_msg");
		message += "\n\n\n- GabriWar";

		RenderedTextBlock text = PixelScene.renderTextBlock( 6 );
		text.text( message, width );
		text.setPos( title.left(), title.bottom() + 4 );
		add( text );

		//store builds must never link out to external payment pages (Play policy);
		//with a payment provider installed the button leads to the in-app tiers instead
		String btnText = Payments.service != null
				? Messages.get(SupporterScene.class, "title")
				: Messages.get(SupporterScene.class, "supporter_link");
		RedButton link = new RedButton(btnText){
			@Override
			protected void onClick() {
				super.onClick();
				WPDSettings.supportNagged(true);
				WndSupportPrompt.super.hide();
				if (Payments.service != null) {
					WarpedPixelDungeon.switchScene( SupporterScene.class );
				} else {
					String link = Payments.KOFI_LINK
							+ "?utm_source=warpedpd&utm_medium=supporter_prompt&utm_campaign=ingame_link";
					WarpedPixelDungeon.platform.openURI(link);
				}
			}
		};
		link.setRect(0, text.bottom() + 4, width, 18);
		add(link);

		RedButton close = new RedButton(Messages.get(this, "close")){
			@Override
			protected void onClick() {
				super.onClick();
				WPDSettings.supportNagged(true);
				WndSupportPrompt.super.hide();
			}
		};
		close.setRect(0, link.bottom() + 2, width, 18);
		add(close);

		resize(width, (int)close.bottom());

	}

	@Override
	public void hide() {
		//do nothing, have to close via the close button
	}
}

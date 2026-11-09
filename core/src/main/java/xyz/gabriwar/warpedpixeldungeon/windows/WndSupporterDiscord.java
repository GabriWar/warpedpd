/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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

import com.watabou.noosa.Game;
import org.json.JSONObject;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.relay.RelayLink;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.services.payments.Payments;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

/**
 * A supporter's line to the developer: his own Discord account to message, and the game's
 * server, where each tier has its role and the supporters their channel. Shown from the
 * thank-you after a purchase, from the supporter page while a subscription is active, and
 * once by itself to every supporter, those who subscribed before it existed too.
 */
public class WndSupporterDiscord extends Window {

	public static final String USERNAME = "gabriwar";
	public static final String PROFILE_LINK = "https://discord.com/users/313753423256354816";
	public static final String SERVER_LINK = "https://discord.gg/5FJsmWDjxW";

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 180;
	private static final int BTN_HEIGHT = 18;

	/** tells a supporter about it the first time a scene sees an active subscription */
	public static void tellOnce( PixelScene scene ){
		if (WPDSettings.supporterDiscordTold()) return;
		if (Payments.receipt() == null) return;
		//a thank-you about to show carries the same news, with its own button to it
		if (WPDSettings.supporterThanksTier() > 0) return;
		WPDSettings.supporterDiscordTold( true );
		scene.addToFront( new WndSupporterDiscord() );
	}

	public WndSupporterDiscord(){
		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		IconTitle title = new IconTitle( Icons.get( Icons.WPD ), Messages.get( this, "title" ) );
		title.setRect( 0, 0, width, 0 );
		add( title );

		RenderedTextBlock text = PixelScene.renderTextBlock( 6 );
		text.text( Messages.get( this, "body", USERNAME ), width );
		text.setPos( title.left(), title.bottom() + 4 );
		add( text );

		//the relay checks the subscription and GabriBot gives the tier's role, adding the
		//player to the server on the way if they are not in it yet (relay/src/discord.rs)
		RedButton role = new RedButton( Messages.get( this, "role" ) ){
			@Override
			protected void onClick() {
				super.onClick();
				enable( false );
				text( Messages.get( WndSupporterDiscord.class, "role_wait" ) );
				new Thread( () -> {
					String url = null;
					String failed = "role_fail";
					try (java.net.Socket socket = RelayLink.dial()) {
						JSONObject reply = RelayLink.handshake( socket, RelayLink.base( "discord" ) );
						url = reply.optString( "url", null );
					} catch (xyz.gabriwar.warpedpixeldungeon.net.relay.RelayException e) {
						//the relay said no: no live subscription, or this one's role is
						//already on another Discord account
						failed = "already_linked".equals( e.code ) ? "role_linked"
								: "supporter_required".equals( e.code ) ? "role_paid" : "role_fail";
					} catch (Exception e) {
						url = null;
					}
					final String link = url;
					final String why = failed;
					Game.runOnRenderThread( () -> {
						enable( true );
						if (link != null && link.startsWith( "https://discord.com/" )) {
							text( Messages.get( WndSupporterDiscord.class, "role" ) );
							WarpedPixelDungeon.platform.openURI( link );
						} else {
							text( Messages.get( WndSupporterDiscord.class, why ) );
						}
					} );
				}, "discord-role" ).start();
			}
		};
		role.icon( Icons.get( Icons.DISCORD ) );
		role.setRect( 0, text.bottom() + 6, width, BTN_HEIGHT );
		add( role );

		RedButton profile = new RedButton( Messages.get( this, "profile", USERNAME ) ){
			@Override
			protected void onClick() {
				super.onClick();
				WarpedPixelDungeon.platform.openURI( PROFILE_LINK );
			}
		};
		profile.setRect( 0, role.bottom() + 2, width, BTN_HEIGHT );
		add( profile );

		RedButton server = new RedButton( Messages.get( this, "server" ) ){
			@Override
			protected void onClick() {
				super.onClick();
				WarpedPixelDungeon.platform.openURI( SERVER_LINK );
			}
		};
		server.setRect( 0, profile.bottom() + 2, width, BTN_HEIGHT );
		add( server );

		RedButton close = new RedButton( Messages.get( this, "close" ) ){
			@Override
			protected void onClick() {
				super.onClick();
				hide();
			}
		};
		close.setRect( 0, server.bottom() + 2, width, BTN_HEIGHT );
		add( close );

		resize( width, (int) close.bottom() );
	}
}

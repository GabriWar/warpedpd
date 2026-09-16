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

package xyz.gabriwar.warpedpixeldungeon.net.ui;

import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

public class WndNetError extends Window {

	private static final int WIDTH = 140;
	private static final int MARGIN = 4;
	private static final int BTN_HEIGHT = 18;

	public WndNetError(String title, String message, Runnable onDismiss) {
		super();

		float pos = MARGIN;

		RenderedTextBlock titleBlock = PixelScene.renderTextBlock(title, 9);
		titleBlock.hardlight(0xFF4444);
		titleBlock.maxWidth(WIDTH - MARGIN * 2);
		titleBlock.setPos((WIDTH - titleBlock.width()) / 2, pos);
		add(titleBlock);
		pos = titleBlock.bottom() + MARGIN;

		RenderedTextBlock msgBlock = PixelScene.renderTextBlock(message, 6);
		msgBlock.maxWidth(WIDTH - MARGIN * 2);
		msgBlock.setPos(MARGIN, pos);
		add(msgBlock);
		pos = msgBlock.bottom() + MARGIN * 2;

		RedButton btnOk = new RedButton("OK") {
			@Override
			protected void onClick() {
				hide();
				if (onDismiss != null) {
					onDismiss.run();
				}
			}
		};
		btnOk.setRect(MARGIN, pos, WIDTH - MARGIN * 2, BTN_HEIGHT);
		add(btnOk);
		pos = btnOk.bottom() + MARGIN;

		resize(WIDTH, (int) pos);
	}

	/**
	 * Turns a relay refusal into something worth reading. The relay's codes are stable,
	 * so a player gets told what actually happened instead of "connection failed".
	 */
	public static String explain(String code) {
		if (code == null) return "unknown error";
		switch (code) {
			case "no_such_room":    return "no game with that code — check it and try again";
			case "room_full":       return "that game is full";
			case "host_timeout":    return "the host stopped responding";
			case "rate_limited":    return "too many attempts, wait a moment";
			case "already_hosting": return "you're already hosting a game";
			case "already_joined":  return "you're already in a game";
			case "server_full":     return "the relay is busy, try again shortly";
			case "too_slow":        return "the connection was using too much bandwidth";
			case "supporter_required":
				return "connecting distant players runs on a server supporters pay for — playing on the same network is always free";
			case "bad_request":     return "this version can't talk to the relay — update the game";
			case "expired":         return "the room expired";
			default:                return code;
		}
	}

	public static String explain(Throwable error) {
		if (error instanceof xyz.gabriwar.warpedpixeldungeon.net.relay.RelayException) {
			return explain(((xyz.gabriwar.warpedpixeldungeon.net.relay.RelayException) error).code);
		}
		String msg = error == null ? null : error.getMessage();
		return msg == null || msg.isEmpty() ? "could not reach the relay" : msg;
	}
}

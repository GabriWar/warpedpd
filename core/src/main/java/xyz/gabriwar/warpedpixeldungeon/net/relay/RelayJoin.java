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

package xyz.gabriwar.warpedpixeldungeon.net.relay;

import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;

import xyz.gabriwar.warpedpixeldungeon.net.SocketSource;

/**
 * The joining end of an online game: present a room code, wait for the relay to pair the
 * connection with the host, and hand back a socket that behaves like a direct one.
 *
 * <p>Used as a {@link SocketSource} so the client's reconnect loop works unchanged: each
 * attempt simply presents the code again.
 */
public class RelayJoin implements SocketSource {

	//exactly the alphabet the relay generates from, minus the shapes a player could
	//misread: no I, L, O or U
	private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTVWXYZ";
	public static final int CODE_LENGTH = 6;

	private final String code;
	private volatile String hostName = "";

	public RelayJoin(String code) {
		this.code = code;
	}

	/**
	 * Upper-cases and trims what the player typed, and returns null if it could not be a
	 * room code. Ambiguous letters are refused rather than mapped to a digit: quietly
	 * turning an O into a 0 doubles the chance of landing in a stranger's game.
	 */
	public static String normalise(String raw) {
		if (raw == null) return null;
		String up = raw.trim().toUpperCase(java.util.Locale.ENGLISH);
		if (up.length() != CODE_LENGTH) return null;
		for (int i = 0; i < up.length(); i++) {
			if (ALPHABET.indexOf(up.charAt(i)) < 0) return null;
		}
		return up;
	}

	public String hostName() {
		return hostName;
	}

	public String code() {
		return code;
	}

	@Override
	public Socket open() throws IOException {
		Socket socket = RelayLink.dial();
		JSONObject request = RelayLink.base("join");
		try {
			request.put("code", code);
		} catch (org.json.JSONException e) {
			throw new IOException(e);
		}
		//the relay holds this open while it asks the host to dial back, so the handshake
		//deliberately outlasts a normal round trip
		JSONObject welcome = RelayLink.handshake(socket, request);
		hostName = welcome.optString("host_name", "");
		socket.setSoTimeout(0);
		return socket;
	}

	@Override
	public String describe() {
		return "relay " + code;
	}
}

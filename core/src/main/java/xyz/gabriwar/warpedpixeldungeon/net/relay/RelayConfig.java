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

import xyz.gabriwar.warpedpixeldungeon.WPDSettings;

/**
 * Where the relay lives and how to reach it.
 *
 * <p>In production the relay is an origin behind a Cloudflare tunnel, so a session is
 * ordinary HTTPS on port 443 with the game's bytes inside WebSocket frames. Cloudflare
 * terminates TLS and publishes the name, which is why there is no port to forward, no
 * certificate to ship and no address to hand out.
 *
 * <p>Pointing the setting at a host on this machine switches to plain TCP against a
 * locally built relay, which is how the server is exercised without the tunnel.
 */
public final class RelayConfig {

	public static final String DEFAULT_HOST = "warpedserver.gabriwar.xyz";
	private static final int HTTPS_PORT = 443;
	private static final int DIRECT_PORT = 38471;

	private RelayConfig() {}

	public static String host() {
		String custom = WPDSettings.relayHost();
		return custom.isEmpty() ? DEFAULT_HOST : custom;
	}

	public static int port() {
		return local() ? DIRECT_PORT : HTTPS_PORT;
	}

	/** The path the tunnel routes; the relay ignores it, but proxies care. */
	public static String path() {
		return "/";
	}

	public static boolean useTLS() {
		return !local();
	}

	public static boolean useWebSocket() {
		return !local();
	}

	/**
	 * A relay on this machine is a developer running the server by hand. Everything else
	 * is the real one, and gets TLS with the hostname checked.
	 */
	private static boolean local() {
		String h = host();
		return h.equals("127.0.0.1") || h.equals("localhost") || h.equals("::1");
	}
}

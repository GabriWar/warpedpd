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

package xyz.gabriwar.warpedpixeldungeon.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * How a client reaches its host. Reconnecting is just calling {@link #open()} again, so
 * the same retry loop serves a direct LAN dial and a relay room code.
 */
public interface SocketSource {

	Socket open() throws IOException;

	/** For logs and error messages; never shown as-is to the player. */
	String describe();

	/** A direct connection to a host on the same network. */
	class Direct implements SocketSource {

		private final String host;
		private final int port;
		private final int timeoutMs;

		public Direct(String host, int port, int timeoutMs) {
			this.host = host;
			this.port = port;
			this.timeoutMs = timeoutMs;
		}

		@Override
		public Socket open() throws IOException {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(host, port), timeoutMs);
			return socket;
		}

		@Override
		public String describe() {
			return host + ":" + port;
		}
	}
}

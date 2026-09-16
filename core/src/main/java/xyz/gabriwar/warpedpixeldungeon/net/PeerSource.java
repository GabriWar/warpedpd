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
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Where a host's incoming players come from. On a LAN that is a listening socket; online
 * it is the relay dialling back. Everything downstream of {@link #accept()} treats the
 * two identically, which is what keeps the transport change out of the game code.
 */
public interface PeerSource {

	/** Blocks until a player arrives, or throws once the source is finished. */
	Socket accept() throws IOException;

	void close();

	/** The original behaviour: a port on this machine, reachable from the same network. */
	class Lan implements PeerSource {

		private final ServerSocket server;

		public Lan(int port) throws IOException {
			server = new ServerSocket(port);
		}

		@Override
		public Socket accept() throws IOException {
			return server.accept();
		}

		@Override
		public void close() {
			try {
				server.close();
			} catch (IOException ignored) {}
		}
	}
}

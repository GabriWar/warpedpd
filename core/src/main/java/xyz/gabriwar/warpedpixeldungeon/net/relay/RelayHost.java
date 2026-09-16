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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.net.PeerSource;

/**
 * The host end of an online game. Opens a control connection to the relay, is handed a
 * room code to pass to friends, and dials back one data connection per player who turns
 * up. Those connections come out of {@link #accept()} looking exactly like the ones a
 * LAN {@code ServerSocket} produces, so nothing downstream has to know the difference.
 */
public class RelayHost implements PeerSource {

	public interface Listener {
		void onRoomOpen(String code);
		void onRoomClosed(String reason);
	}

	//a joiner that the accept loop has not collected yet is a joiner already waiting on
	//the relay's dial-back timer, so this never needs to be deep
	private final LinkedBlockingQueue<Socket> incoming = new LinkedBlockingQueue<>(8);

	private volatile Socket control;
	private volatile boolean running;
	private volatile String code;
	private volatile String closedReason;
	private Thread controlThread;
	private final Listener listener;

	public RelayHost(Listener listener) {
		this.listener = listener;
	}

	/** Blocks until the relay hands back a room code, then keeps the room alive. */
	public void open() throws IOException {
		Socket socket = RelayLink.dial();
		JSONObject request = RelayLink.base("host");
		try {
			request.put("name", WPDSettings.multiplayerName());
		} catch (org.json.JSONException ignored) {
			//a missing name only costs the joiner a nicer label
		}
		JSONObject hello = RelayLink.handshake(socket, request);

		control = socket;
		code = hello.optString("code", "");
		running = true;
		//the relay pings on its own schedule; a read timeout here would fight it
		socket.setSoTimeout(0);

		controlThread = new Thread(this::pump, "Relay-Control");
		controlThread.setDaemon(true);
		controlThread.start();

		if (listener != null) {
			listener.onRoomOpen(code);
		}
	}

	public String code() {
		return code;
	}

	private void pump() {
		String reason = "closed";
		try {
			while (running) {
				JSONObject event = RelayLink.read(control.getInputStream());
				String ev = event.optString("ev", "");
				if ("peer".equals(ev)) {
					dialBack(event.optString("peer", ""), event.optString("nonce", ""));
				} else if ("ping".equals(ev)) {
					JSONObject pong = new JSONObject();
					pong.put("ev", "pong");
					RelayLink.write(control.getOutputStream(), pong);
				} else if ("expired".equals(ev)) {
					reason = "expired";
					break;
				} else if ("error".equals(ev)) {
					reason = event.optString("code", "error");
					break;
				}
				//peer_gone needs no action: the socket for that player has already died
				//and HostServer noticed it the same way it would on a LAN
			}
		} catch (Exception e) {
			reason = running ? "closed" : "stopped";
			NetManager.log("[RELAY] control ended: " + e.getMessage());
		}
		boolean announce = running;
		running = false;
		closedReason = reason;
		//unblocks a thread parked in accept()
		incoming.offer(CLOSED);
		if (announce && listener != null) {
			listener.onRoomClosed(reason);
		}
	}

	/**
	 * One fresh connection per joiner, tagged with the single-use nonce the relay just
	 * issued. Runs off the control thread so a slow dial cannot stall the room.
	 */
	private void dialBack(final String peer, final String nonce) {
		Thread dialer = new Thread(() -> {
			try {
				Socket data = RelayLink.dial();
				JSONObject request = RelayLink.base("host_data");
				request.put("code", code);
				request.put("peer", peer);
				request.put("nonce", nonce);
				RelayLink.handshake(data, request);
				//back to blocking reads: from here it is an ordinary game connection
				data.setSoTimeout(0);
				if (!incoming.offer(data, 10, TimeUnit.SECONDS)) {
					data.close();
				}
			} catch (Exception e) {
				NetManager.log("[RELAY] dial-back failed for peer " + peer + ": " + e.getMessage());
			}
		}, "Relay-DialBack");
		dialer.setDaemon(true);
		dialer.start();
	}

	//a sentinel rather than an interrupt, so accept() can report the room's own reason
	private static final Socket CLOSED = new Socket();

	@Override
	public Socket accept() throws IOException {
		try {
			Socket next = incoming.take();
			if (next == CLOSED) {
				//put it back for any other waiter, then report why the room ended
				incoming.offer(CLOSED);
				throw new IOException("relay room closed: " + closedReason);
			}
			return next;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("interrupted");
		}
	}

	@Override
	public void close() {
		running = false;
		try {
			if (control != null) {
				JSONObject bye = new JSONObject();
				bye.put("ev", "close");
				RelayLink.write(control.getOutputStream(), bye);
			}
		} catch (Exception ignored) {
			//best effort: the relay drops the room when the socket dies anyway
		}
		try {
			if (control != null) control.close();
		} catch (IOException ignored) {}
		incoming.offer(CLOSED);
	}
}

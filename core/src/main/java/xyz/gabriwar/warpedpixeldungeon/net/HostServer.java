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

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class HostServer {

	private static final int MAX_CLIENTS = 8;

	private final int port;
	//null when the host is online: the relay supplies the peers instead of a local port
	private final PeerSource externalSource;
	private PeerSource peers;
	private Thread acceptThread;
	private volatile boolean running;
	private final CopyOnWriteArrayList<ClientConnection> clients = new CopyOnWriteArrayList<>();

	/** The live connections, for the host to walk when the game starts. */
	public java.util.List<ClientConnection> clients() {
		return clients;
	}
	private Callback onClientConnected;
	private Callback onClientDisconnected;
	private MessageCallback onClientMessage;

	public interface Callback {
		void onConnected(ClientConnection client);
	}

	public interface MessageCallback {
		void onMessage(ClientConnection client, Protocol.Message msg);
	}

	public HostServer(int port) {
		this.port = port;
		this.externalSource = null;
	}

	/** Online host: players arrive through the relay rather than a port on this machine. */
	public HostServer(PeerSource source) {
		this.port = 0;
		this.externalSource = source;
	}

	public void start() throws IOException {
		peers = externalSource != null ? externalSource : new PeerSource.Lan(port);
		running = true;
		acceptThread = new Thread(() -> {
			while (running) {
				try {
					Socket socket = peers.accept();
					if (clients.size() >= MAX_CLIENTS) {
						try { socket.close(); } catch (IOException ignored) {}
						continue;
					}
					ClientConnection client = new ClientConnection(socket);
					clients.add(client);
					client.start();
					if (onClientConnected != null) {
						onClientConnected.onConnected(client);
					}
				} catch (IOException e) {
					if (running) {
						e.printStackTrace();
					}
				}
			}
		}, "Net-Accept");
		acceptThread.setDaemon(true);
		acceptThread.start();
	}

	public void broadcast(int type, JSONObject data) {
		byte[] encoded;
		try {
			JSONObject envelope = new JSONObject();
			envelope.put("t", type);
			envelope.put("d", data != null ? data : new JSONObject());
			encoded = envelope.toString().getBytes("UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		byte[] framed = new byte[4 + encoded.length];
		framed[0] = (byte) (encoded.length >> 24);
		framed[1] = (byte) (encoded.length >> 16);
		framed[2] = (byte) (encoded.length >> 8);
		framed[3] = (byte) encoded.length;
		System.arraycopy(encoded, 0, framed, 4, encoded.length);

		for (ClientConnection client : clients) {
			client.enqueue(framed);
		}
	}

	public void stop() {
		stop(0);
	}

	/**
	 * Stop, giving each connection up to graceMs for what is already queued - a goodbye,
	 * a last hero copy - to reach the wire. Writes go through a queue on another thread,
	 * so closing right after enqueueing would drop exactly the messages that matter most.
	 */
	public void stop(long graceMs) {
		running = false;
		long deadline = System.currentTimeMillis() + graceMs;
		for (ClientConnection client : clients) {
			client.closeAfterFlush(deadline);
		}
		clients.clear();
		if (peers != null) peers.close();
	}

	public int getClientCount() {
		return clients.size();
	}

	public void setOnClientConnected(Callback callback) {
		this.onClientConnected = callback;
	}

	public void setOnClientDisconnected(Callback callback) {
		this.onClientDisconnected = callback;
	}

	public void setOnClientMessage(MessageCallback callback) {
		this.onClientMessage = callback;
	}

	private void removeClient(ClientConnection client) {
		clients.remove(client);
		if (onClientDisconnected != null) {
			onClientDisconnected.onConnected(client);
		}
	}

	public class ClientConnection {
		private final Socket socket;
		private final LinkedBlockingQueue<byte[]> writeQueue = new LinkedBlockingQueue<>(256);
		private Thread writeThread;
		private Thread readThread;
		private volatile boolean alive = true;
		//frames enqueued and not yet flushed, so a shutdown can wait for them
		private final java.util.concurrent.atomic.AtomicInteger inFlight = new java.util.concurrent.atomic.AtomicInteger();

		/** 0 = unidentified, 1 = spectator (sent JOIN), 2 = player (JOIN_AS_PLAYER accepted) */
		public volatile int connectionRole = 0;
		/** A JOIN_AS_PLAYER that arrived while the host was still in the lobby. Held so
		 *  the claim can be answered once there is a game to join, rather than lost. */
		public volatile org.json.JSONObject pendingPlayerJoin = null;
		public static final int ROLE_UNIDENTIFIED = 0;
		public static final int ROLE_SPECTATOR    = 1;
		public static final int ROLE_PLAYER       = 2;

		ClientConnection(Socket socket) {
			this.socket = socket;
		}

		void start() {
			writeThread = new Thread(() -> {
				try {
					OutputStream out = socket.getOutputStream();
					while (alive) {
						byte[] framed = writeQueue.take();
						out.write(framed);
						out.flush();
						inFlight.decrementAndGet();
					}
				} catch (InterruptedException ie) {
					// Expected on close() — alive is already false, just exit cleanly.
				} catch (Exception e) {
					if (alive) {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] write thread died: "
								+ e.getClass().getSimpleName() + ": " + e.getMessage());
						close();
					}
				}
			}, "Net-Write");
			writeThread.setDaemon(true);
			writeThread.start();

			readThread = new Thread(() -> {
				try {
					InputStream in = socket.getInputStream();
					while (alive) {
						Protocol.Message msg = Protocol.readMessage(in);
						if (msg.type == Protocol.PING) {
							sendMessage(Protocol.PONG, msg.data);
						} else if (onClientMessage != null) {
							onClientMessage.onMessage(this, msg);
						}
					}
				} catch (Exception e) {
					if (alive) {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] read thread died: "
								+ e.getClass().getSimpleName() + ": " + e.getMessage());
						close();
					}
				}
			}, "Net-Read");
			readThread.setDaemon(true);
			readThread.start();
		}

		public void enqueue(byte[] framed) {
			if (!alive) return;
			inFlight.incrementAndGet();
			if (!writeQueue.offer(framed)) {
				inFlight.decrementAndGet();
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.log(
						"[NET-HOST] write queue full — disconnecting stalled client");
				close();
			}
		}

		public void sendMessage(int type, JSONObject data) {
			try {
				JSONObject envelope = new JSONObject();
				envelope.put("t", type);
				envelope.put("d", data != null ? data : new JSONObject());
				byte[] encoded = envelope.toString().getBytes("UTF-8");
				byte[] framed = new byte[4 + encoded.length];
				framed[0] = (byte) (encoded.length >> 24);
				framed[1] = (byte) (encoded.length >> 16);
				framed[2] = (byte) (encoded.length >> 8);
				framed[3] = (byte) encoded.length;
				System.arraycopy(encoded, 0, framed, 4, encoded.length);
				enqueue(framed);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/** Close once everything queued has been written, or the deadline passes. */
		public void closeAfterFlush(long deadlineMillis) {
			while (alive && inFlight.get() > 0 && System.currentTimeMillis() < deadlineMillis) {
				try { Thread.sleep(5); } catch (InterruptedException e) { break; }
			}
			close();
		}

		public void close() {
			if (!alive) return; // idempotent — close() can be called from either thread's catch
			alive = false;
			try { socket.close(); } catch (IOException ignored) {}
			// Interrupt both threads explicitly. socket.close() unblocks readThread (the
			// InputStream throws), but writeThread is parked in writeQueue.take() which
			// isn't tied to the socket — without an interrupt it would hang forever
			// after a half-close, leaving a ghost ClientConnection in the host's state.
			if (writeThread != null) writeThread.interrupt();
			if (readThread  != null) readThread.interrupt();
			removeClient(this);
		}
	}
}

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
import java.net.Socket;

public class SpectatorClient {

	private static final int CONNECT_TIMEOUT_MS = 5000;
	private static final int PING_INTERVAL_MS = 3000;
	private static final int MAX_RECONNECT_ATTEMPTS = 5;
	private static final int RECONNECT_DELAY_MS = 3000;

	private final String host;
	private final int port;
	private Socket socket;
	private Thread readThread;
	private Thread pingThread;
	private volatile boolean connected;
	private volatile boolean intentionalDisconnect;
	private MessageHandler handler;
	private volatile long lastPingMs = -1;
	private volatile long pendingPingTime = -1;
	private int reconnectAttempts = 0;
	private final SocketSource source;

	public interface MessageHandler {
		void onMessage(Protocol.Message message);
		void onDisconnected(String reason);
		void onConnected();
		void onReconnecting(int attempt, int max);
	}

	public SpectatorClient(String host, int port) {
		this.host = host;
		this.port = port;
		this.source = new SocketSource.Direct(host, port, CONNECT_TIMEOUT_MS);
	}

	/**
	 * Online: the socket comes from a relay room instead of a direct dial. Reconnecting
	 * re-presents the room code, which is why this is a source rather than a socket.
	 */
	public SpectatorClient(SocketSource source) {
		this.host = source.describe();
		this.port = 0;
		this.source = source;
	}

	public void connect() {
		intentionalDisconnect = false;
		readThread = new Thread(() -> {
			try {
				socket = source.open();
				connected = true;
				reconnectAttempts = 0;

				// Send JOIN message
				Protocol.writeMessage(socket.getOutputStream(), getJoinType(), getJoinData());

				if (handler != null) {
					handler.onConnected();
				}

				startPingLoop();

				InputStream in = socket.getInputStream();
				while (connected) {
					Protocol.Message msg = Protocol.readMessage(in);
					if (msg.type == Protocol.PONG) {
						if (pendingPingTime > 0) {
							lastPingMs = System.currentTimeMillis() - pendingPingTime;
							pendingPingTime = -1;
						}
					} else if (handler != null) {
						handler.onMessage(msg);
					}
				}
			} catch (IOException e) {
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-CLI] read loop IOException: " + e.getMessage()
						+ " (intentional=" + intentionalDisconnect + ")");
				if (connected || !intentionalDisconnect) {
					connected = false;
					stopPingLoop();
					if (!intentionalDisconnect) {
						attemptReconnect(e.getMessage());
					}
				}
			}
		}, "Net-Spectator");
		readThread.setDaemon(true);
		readThread.start();
	}

	/** Refusals the relay will keep giving, however long we knock: the room is gone, or
	 *  it will not have us. Retrying one of these is a loop with no exit. */
	private static boolean permanent(String error) {
		if (error == null) return false;
		return error.startsWith("no_such_room")
				|| error.startsWith("room_full")
				|| error.startsWith("already_joined")
				|| error.startsWith("already_hosting")
				|| error.startsWith("supporter_required");
	}

	private void attemptReconnect(String originalError) {
		if (intentionalDisconnect) return;

		//a host who closed the game took the room with them: knocking again cannot bring
		//it back, and the old code will never be valid once the relay has forgotten it
		if (permanent(originalError)) {
			if (handler != null) handler.onDisconnected(originalError);
			return;
		}

		while (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && !intentionalDisconnect) {
			reconnectAttempts++;
			if (handler != null) {
				handler.onReconnecting(reconnectAttempts, MAX_RECONNECT_ATTEMPTS);
			}
			try {
				Thread.sleep(RECONNECT_DELAY_MS);
			} catch (InterruptedException e) {
				return;
			}
			if (intentionalDisconnect) return;

			try {
				socket = source.open();
				connected = true;

				Protocol.writeMessage(socket.getOutputStream(), getJoinType(), getJoinData());

				//only now is the attempt spent rather than merely started. Resetting on
				//the open alone meant a refusal reset the counter too, so five tries
				//became an endless one - the log filled with "Reconnecting... (1/5)"
				reconnectAttempts = 0;

				if (handler != null) {
					handler.onConnected();
				}

				startPingLoop();

				InputStream in = socket.getInputStream();
				while (connected) {
					Protocol.Message msg = Protocol.readMessage(in);
					if (msg.type == Protocol.PONG) {
						if (pendingPingTime > 0) {
							lastPingMs = System.currentTimeMillis() - pendingPingTime;
							pendingPingTime = -1;
						}
					} else if (handler != null) {
						handler.onMessage(msg);
					}
				}
			} catch (IOException e) {
				connected = false;
				stopPingLoop();
				//the relay answering "that room is gone" is an answer, not a dropped
				//connection: stop rather than spend the remaining tries on it
				if (permanent(e.getMessage())) {
					if (!intentionalDisconnect && handler != null) {
						handler.onDisconnected(e.getMessage());
					}
					return;
				}
			}
		}

		if (!intentionalDisconnect && handler != null) {
			handler.onDisconnected(originalError);
		}
	}

	private void startPingLoop() {
		stopPingLoop();
		pingThread = new Thread(() -> {
			while (connected && !intentionalDisconnect) {
				try {
					Thread.sleep(PING_INTERVAL_MS);
					if (connected && socket != null && !socket.isClosed()) {
						pendingPingTime = System.currentTimeMillis();
						JSONObject pingData = new JSONObject();
						pingData.put("t", pendingPingTime);
						Protocol.writeMessage(socket.getOutputStream(), Protocol.PING, pingData);
					}
				} catch (Exception e) {
					break;
				}
			}
		}, "Net-Ping");
		pingThread.setDaemon(true);
		pingThread.start();
	}

	private void stopPingLoop() {
		if (pingThread != null) {
			pingThread.interrupt();
			pingThread = null;
		}
	}

	public void setMessageHandler(MessageHandler handler) {
		this.handler = handler;
	}

	public boolean isConnected() {
		return connected;
	}

	public long getPingMs() {
		return lastPingMs;
	}

	/** Connect as a player, sending JOIN_AS_PLAYER with hero data instead of JOIN */
	public void connectAsPlayer(JSONObject heroData) {
		this.playerHeroData = heroData;
		connect();
	}

	private JSONObject playerHeroData = null;
	private volatile String sessionToken = null;

	public void setSessionToken(String token) {
		this.sessionToken = token;
	}

	/** Get the join message type — JOIN for spectators, JOIN_AS_PLAYER for players */
	int getJoinType() {
		return playerHeroData != null ? Protocol.JOIN_AS_PLAYER : Protocol.JOIN;
	}

	/** Get the join data — empty for spectators, hero data for players */
	JSONObject getJoinData() {
		if (playerHeroData == null) return new JSONObject();
		if (sessionToken != null && !sessionToken.isEmpty()) {
			try { playerHeroData.put("token", sessionToken); } catch (Exception ignored) {}
		}
		return playerHeroData;
	}

	/** Send a message to the host */
	public void sendMessage(int type, JSONObject data) {
		if (!connected || socket == null || socket.isClosed()) return;
		try {
			Protocol.writeMessage(socket.getOutputStream(), type, data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void disconnect() {
		intentionalDisconnect = true;
		connected = false;
		stopPingLoop();
		try {
			if (socket != null) socket.close();
		} catch (IOException ignored) {}
	}
}

package xyz.gabriwar.warpedpixeldungeon.net;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
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

	public interface MessageHandler {
		void onMessage(Protocol.Message message);
		void onDisconnected(String reason);
		void onConnected();
		void onReconnecting(int attempt, int max);
	}

	public SpectatorClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void connect() {
		intentionalDisconnect = false;
		readThread = new Thread(() -> {
			try {
				socket = new Socket();
				socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
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

	private void attemptReconnect(String originalError) {
		if (intentionalDisconnect) return;

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
				socket = new Socket();
				socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
				connected = true;
				reconnectAttempts = 0;

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
				connected = false;
				stopPingLoop();
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

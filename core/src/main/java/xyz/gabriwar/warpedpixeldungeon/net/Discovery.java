package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;

import com.watabou.utils.DeviceCompat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

public class Discovery {

	private static final int UDP_PORT = NetManager.PORT + 1;
	private static final String MAGIC = "WPD1";
	private static final int BROADCAST_INTERVAL = 2000;
	private static final int EXPIRY_MS = 6000;

	private static Thread broadcastThread;
	private static Thread listenThread;
	private static volatile boolean broadcasting;
	private static volatile boolean listening;

	// key -> FoundGame (key is "ip" for remote, "127.0.0.1" for localhost)
	private static final ConcurrentHashMap<String, FoundGame> foundGames = new ConcurrentHashMap<>();

	public static class FoundGame {
		public final String ip;
		public int depth;
		public String heroClass;
		public int spectators;
		public long lastSeen;

		public FoundGame(String ip, int depth, String heroClass, int spectators) {
			this.ip = ip;
			this.depth = depth;
			this.heroClass = heroClass;
			this.spectators = spectators;
			this.lastSeen = System.currentTimeMillis();
		}

		public boolean isExpired() {
			return System.currentTimeMillis() - lastSeen > EXPIRY_MS;
		}
	}

	public static void startBroadcasting() {
		stopBroadcasting();
		// iOS blocks UDP broadcast/multicast without the (Apple-granted) multicast
		// entitlement, so LAN auto-discovery can't work there. Hosting still accepts
		// direct connections; peers join by typing the host's address.
		if (DeviceCompat.isiOS()) return;
		broadcasting = true;
		broadcastThread = new Thread(() -> {
			try (DatagramSocket socket = new DatagramSocket()) {
				socket.setBroadcast(true);
				while (broadcasting) {
					try {
						String heroClass = "?";
						int depth = 0;
						int spectators = NetManager.getClientCount();
						if (Dungeon.hero != null) {
							heroClass = Dungeon.hero.heroClass.name();
							depth = Dungeon.depth;
						}

						String msg = MAGIC + "|" + depth + "|" + heroClass + "|" + spectators;
						byte[] data = msg.getBytes(StandardCharsets.UTF_8);

						// Send to global broadcast
						try {
							socket.send(new DatagramPacket(
									data, data.length,
									InetAddress.getByName("255.255.255.255"), UDP_PORT));
						} catch (Exception ignored) {}

						// Send to localhost so same-machine instances can discover us
						try {
							socket.send(new DatagramPacket(
									data, data.length,
									InetAddress.getByName("127.0.0.1"), UDP_PORT));
						} catch (Exception ignored) {}

						// Send to subnet broadcast addresses
						broadcastToSubnets(socket, data);

					} catch (Exception ignored) {}
					Thread.sleep(BROADCAST_INTERVAL);
				}
			} catch (Exception ignored) {}
		}, "Net-Discovery-Broadcast");
		broadcastThread.setDaemon(true);
		broadcastThread.start();
	}

	public static void startListening() {
		stopListening();
		// See startBroadcasting(): iOS can't receive LAN broadcasts, so there's
		// nothing to listen for. Peers join by typing the host's address instead.
		if (DeviceCompat.isiOS()) return;
		listening = true;
		foundGames.clear();
		listenThread = new Thread(() -> {
			try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
				socket.setSoTimeout(1000);
				socket.setBroadcast(true);
				byte[] buf = new byte[256];

				while (listening) {
					try {
						DatagramPacket packet = new DatagramPacket(buf, buf.length);
						socket.receive(packet);

						String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
						if (!msg.startsWith(MAGIC + "|")) continue;

						// Don't filter by IP — we want same-machine discovery.
						// The listener is only started when we're NOT hosting,
						// so we won't see our own broadcasts.

						String senderIP = packet.getAddress().getHostAddress();

						String[] parts = msg.split("\\|");
						if (parts.length < 4) continue;

						int depth;
						int spectators;
						try {
							depth = Integer.parseInt(parts[1]);
							spectators = Integer.parseInt(parts[3]);
						} catch (NumberFormatException ignored) {
							continue;
						}
						String heroClass = parts[2];

						// For same-machine (loopback), the connect IP should be 127.0.0.1
						// For remote, use the sender's real IP
						boolean isLoopback = senderIP.equals("127.0.0.1")
								|| senderIP.startsWith("127.")
								|| senderIP.equals("0.0.0.0");

						// Use the real IP as display, but if it came via loopback
						// we need to resolve what to connect to
						String connectIP = isLoopback ? "127.0.0.1" : senderIP;

						FoundGame existing = foundGames.get(connectIP);
						if (existing != null) {
							existing.depth = depth;
							existing.heroClass = heroClass;
							existing.spectators = spectators;
							existing.lastSeen = System.currentTimeMillis();
						} else {
							foundGames.put(connectIP, new FoundGame(connectIP, depth, heroClass, spectators));
						}

						// Prune expired
						foundGames.entrySet().removeIf(e -> e.getValue().isExpired());

					} catch (SocketTimeoutException ignored) {
						foundGames.entrySet().removeIf(e -> e.getValue().isExpired());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, "Net-Discovery-Listen");
		listenThread.setDaemon(true);
		listenThread.start();
	}

	public static ConcurrentHashMap<String, FoundGame> getFoundGames() {
		return foundGames;
	}

	public static void stopBroadcasting() {
		broadcasting = false;
		if (broadcastThread != null) {
			broadcastThread.interrupt();
			broadcastThread = null;
		}
	}

	public static void stopListening() {
		listening = false;
		if (listenThread != null) {
			listenThread.interrupt();
			listenThread = null;
		}
	}

	public static void stopAll() {
		stopBroadcasting();
		stopListening();
		foundGames.clear();
	}

	private static void broadcastToSubnets(DatagramSocket socket, byte[] data) {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				if (iface.isLoopback() || !iface.isUp()) continue;
				for (java.net.InterfaceAddress addr : iface.getInterfaceAddresses()) {
					InetAddress broadcast = addr.getBroadcast();
					if (broadcast != null) {
						try {
							socket.send(new DatagramPacket(data, data.length, broadcast, UDP_PORT));
						} catch (Exception ignored) {}
					}
				}
			}
		} catch (Exception ignored) {}
	}

	public static String getLocalIP() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				if (iface.isLoopback() || !iface.isUp()) continue;
				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if (addr instanceof Inet4Address) {
						return addr.getHostAddress();
					}
				}
			}
		} catch (Exception ignored) {}
		return "127.0.0.1";
	}
}

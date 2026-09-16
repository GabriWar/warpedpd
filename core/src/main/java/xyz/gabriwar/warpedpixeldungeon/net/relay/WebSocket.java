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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Just enough WebSocket to carry the relay's byte stream over ordinary HTTPS.
 *
 * <p>The relay sits behind a Cloudflare tunnel, and a tunnel only carries HTTP for
 * ordinary clients — its raw TCP mode needs cloudflared running on the player's machine.
 * Wrapping the same bytes in WebSocket frames makes the session look like a web request,
 * which is what gets it through.
 *
 * <p>Written by hand rather than pulled from a library: the game already ships on
 * Android API 23, where {@code java.net.http} and {@code java.util.Base64} do not exist,
 * and this needs binary frames and nothing else.
 */
final class WebSocket {

	private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	//the relay never sends anything close to this; it exists so a broken or hostile
	//peer cannot ask us to allocate an arbitrary buffer
	private static final int MAX_FRAME = 1 << 20;

	private WebSocket() {}

	/**
	 * Performs the upgrade and returns a socket whose streams carry the payloads. The
	 * caller keeps treating it as an ordinary socket, which is what lets the relay code
	 * be identical on both transports.
	 */
	static Socket upgrade(Socket raw, String host, String path) throws IOException {
		byte[] nonce = new byte[16];
		new SecureRandom().nextBytes(nonce);
		String key = base64(nonce);

		String request = "GET " + path + " HTTP/1.1\r\n"
				+ "Host: " + host + "\r\n"
				+ "Upgrade: websocket\r\n"
				+ "Connection: Upgrade\r\n"
				+ "Sec-WebSocket-Key: " + key + "\r\n"
				+ "Sec-WebSocket-Version: 13\r\n"
				+ "\r\n";
		OutputStream rawOut = raw.getOutputStream();
		rawOut.write(request.getBytes(StandardCharsets.UTF_8));
		rawOut.flush();

		String response = readHead(raw.getInputStream());
		if (!response.startsWith("HTTP/1.1 101")) {
			throw new IOException("relay did not upgrade: " + response.split("\r\n")[0]);
		}
		//proves we are talking to something that actually understood the handshake, not
		//a proxy that happened to answer 101
		String expected = accept(key);
		if (!headerEquals(response, "sec-websocket-accept", expected)) {
			throw new IOException("relay sent a bad websocket accept");
		}
		return new WsSocket(raw);
	}

	private static String readHead(InputStream in) throws IOException {
		ByteArrayOutputStream head = new ByteArrayOutputStream();
		int b;
		while ((b = in.read()) != -1) {
			head.write(b);
			int size = head.size();
			if (size >= 4) {
				byte[] tail = head.toByteArray();
				if (tail[size - 4] == '\r' && tail[size - 3] == '\n'
						&& tail[size - 2] == '\r' && tail[size - 1] == '\n') {
					return new String(tail, StandardCharsets.UTF_8);
				}
			}
			//an endless header is a stuck or hostile server, not a big response
			if (size > 8192) throw new IOException("relay response header too long");
		}
		throw new IOException("relay closed during the websocket handshake");
	}

	private static boolean headerEquals(String head, String name, String value) {
		for (String line : head.split("\r\n")) {
			int colon = line.indexOf(':');
			if (colon <= 0) continue;
			if (line.substring(0, colon).trim().equalsIgnoreCase(name)) {
				return line.substring(colon + 1).trim().equals(value);
			}
		}
		return false;
	}

	private static String accept(String key) throws IOException {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			sha1.update((key + GUID).getBytes(StandardCharsets.UTF_8));
			return base64(sha1.digest());
		} catch (Exception e) {
			throw new IOException("no SHA-1 available", e);
		}
	}

	//java.util.Base64 is API 26; android.util.Base64 is not on desktop. Sixteen lines
	//beats a platform split.
	private static final char[] B64 =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

	private static String base64(byte[] data) {
		StringBuilder out = new StringBuilder(((data.length + 2) / 3) * 4);
		for (int i = 0; i < data.length; i += 3) {
			int chunk = (data[i] & 0xFF) << 16;
			int have = 1;
			if (i + 1 < data.length) { chunk |= (data[i + 1] & 0xFF) << 8; have++; }
			if (i + 2 < data.length) { chunk |= (data[i + 2] & 0xFF);      have++; }
			out.append(B64[(chunk >> 18) & 0x3F]);
			out.append(B64[(chunk >> 12) & 0x3F]);
			out.append(have > 1 ? B64[(chunk >> 6) & 0x3F] : '=');
			out.append(have > 2 ? B64[chunk & 0x3F] : '=');
		}
		return out.toString();
	}

	/**
	 * A socket whose streams frame and unframe. Only the methods the net code actually
	 * calls are delegated; the rest of {@link Socket} is deliberately left alone so a
	 * mistaken call fails loudly instead of silently talking to the wrong endpoint.
	 */
	private static final class WsSocket extends Socket {

		private final Socket delegate;
		private final InputStream in;
		private final OutputStream out;

		WsSocket(Socket delegate) throws IOException {
			this.delegate = delegate;
			this.in = new WsInputStream(delegate.getInputStream(), delegate.getOutputStream());
			this.out = new WsOutputStream(delegate.getOutputStream());
		}

		@Override public InputStream getInputStream() { return in; }
		@Override public OutputStream getOutputStream() { return out; }
		@Override public void close() throws IOException { delegate.close(); }
		@Override public boolean isClosed() { return delegate.isClosed(); }
		@Override public boolean isConnected() { return delegate.isConnected(); }
		@Override public void setSoTimeout(int timeout) throws SocketException { delegate.setSoTimeout(timeout); }
		@Override public int getSoTimeout() throws SocketException { return delegate.getSoTimeout(); }
		@Override public void setTcpNoDelay(boolean on) throws SocketException { delegate.setTcpNoDelay(on); }
		@Override public void shutdownOutput() throws IOException { delegate.shutdownOutput(); }
		@Override public java.net.SocketAddress getRemoteSocketAddress() { return delegate.getRemoteSocketAddress(); }
	}

	/** Every client frame is masked, as the protocol requires. */
	private static final class WsOutputStream extends OutputStream {

		private final OutputStream out;
		private final SecureRandom random = new SecureRandom();

		WsOutputStream(OutputStream out) {
			this.out = out;
		}

		@Override
		public synchronized void write(int b) throws IOException {
			write(new byte[]{ (byte) b }, 0, 1);
		}

		@Override
		public synchronized void write(byte[] data, int off, int len) throws IOException {
			byte[] mask = new byte[4];
			random.nextBytes(mask);

			ByteArrayOutputStream frame = new ByteArrayOutputStream(len + 14);
			frame.write(0x82);                       //FIN + binary
			if (len < 126) {
				frame.write(0x80 | len);
			} else if (len <= 0xFFFF) {
				frame.write(0x80 | 126);
				frame.write((len >> 8) & 0xFF);
				frame.write(len & 0xFF);
			} else {
				frame.write(0x80 | 127);
				for (int shift = 56; shift >= 0; shift -= 8) {
					frame.write((int) (((long) len >> shift) & 0xFF));
				}
			}
			frame.write(mask, 0, 4);
			for (int i = 0; i < len; i++) {
				frame.write(data[off + i] ^ mask[i & 3]);
			}
			//one write per frame: a partially written frame would desynchronise the peer
			out.write(frame.toByteArray());
		}

		@Override
		public void flush() throws IOException {
			out.flush();
		}

		@Override
		public void close() throws IOException {
			out.close();
		}
	}

	private static final class WsInputStream extends InputStream {

		private final InputStream in;
		private final OutputStream control;
		private byte[] payload = new byte[0];
		private int offset;
		private boolean closed;

		WsInputStream(InputStream in, OutputStream control) {
			this.in = in;
			this.control = control;
		}

		@Override
		public int read() throws IOException {
			byte[] one = new byte[1];
			return read(one, 0, 1) == 1 ? one[0] & 0xFF : -1;
		}

		@Override
		public int read(byte[] dest, int off, int len) throws IOException {
			while (offset >= payload.length) {
				if (closed) return -1;
				if (!nextFrame()) return -1;
			}
			int n = Math.min(len, payload.length - offset);
			System.arraycopy(payload, offset, dest, off, n);
			offset += n;
			return n;
		}

		@Override
		public int available() {
			return payload.length - offset;
		}

		/** @return false at end of stream. */
		private boolean nextFrame() throws IOException {
			int first = in.read();
			if (first < 0) return false;
			int second = readByte();
			int opcode = first & 0x0F;
			//the server never masks, per the protocol; if it did, this length would be
			//wrong and the stream would desynchronise, so treat it as fatal
			if ((second & 0x80) != 0) throw new IOException("relay masked a frame");

			long len = second & 0x7F;
			if (len == 126) {
				len = (readByte() << 8) | readByte();
			} else if (len == 127) {
				len = 0;
				for (int i = 0; i < 8; i++) len = (len << 8) | readByte();
			}
			if (len < 0 || len > MAX_FRAME) throw new IOException("relay frame too large: " + len);

			byte[] body = new byte[(int) len];
			int read = 0;
			while (read < body.length) {
				int n = in.read(body, read, body.length - read);
				if (n < 0) return false;
				read += n;
			}

			switch (opcode) {
				case 0x0: case 0x1: case 0x2:
					payload = body;
					offset = 0;
					return true;
				case 0x8:
					closed = true;
					return false;
				case 0x9:
					pong(body);
					return nextFrame();
				case 0xA:
					return nextFrame();
				default:
					throw new IOException("relay sent opcode " + opcode);
			}
		}

		private void pong(byte[] body) throws IOException {
			byte[] mask = new byte[4];
			new SecureRandom().nextBytes(mask);
			ByteArrayOutputStream frame = new ByteArrayOutputStream(body.length + 6);
			frame.write(0x8A);
			frame.write(0x80 | Math.min(body.length, 125));
			frame.write(mask, 0, 4);
			for (int i = 0; i < Math.min(body.length, 125); i++) {
				frame.write(body[i] ^ mask[i & 3]);
			}
			synchronized (control) {
				control.write(frame.toByteArray());
				control.flush();
			}
		}

		private int readByte() throws IOException {
			int b = in.read();
			if (b < 0) throw new IOException("relay closed mid-frame");
			return b;
		}

		@Override
		public void close() throws IOException {
			in.close();
		}
	}
}

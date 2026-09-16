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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import xyz.gabriwar.warpedpixeldungeon.services.payments.PaymentService;
import xyz.gabriwar.warpedpixeldungeon.services.payments.Payments;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Dialling the relay and speaking its handshake. The handshake is the only part of an
 * online session the relay understands; once it answers, the socket carries ordinary
 * {@link xyz.gabriwar.warpedpixeldungeon.net.Protocol} traffic and the relay is just a
 * pipe.
 */
public final class RelayLink {

	//a handshake carrying a store receipt is the biggest frame either side ever sends —
	//an RSA-2048 signature alone is 344 base64 characters. Anything past it is a broken
	//or hostile server, not a big message.
	private static final int MAX_FRAME = 2048;
	private static final int CONNECT_TIMEOUT_MS = 10000;
	private static final int HANDSHAKE_TIMEOUT_MS = 15000;

	private RelayLink() {}

	/**
	 * A TLS connection to the relay with the hostname actually checked. Without the
	 * endpoint identification algorithm the JDK verifies the chain but not who it was
	 * issued to, which would let anyone with any valid certificate sit in the middle.
	 */
	public static Socket dial() throws IOException {
		Socket socket;
		if (RelayConfig.useTLS()) {
			SSLSocket ssl = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
			ssl.connect(new InetSocketAddress(RelayConfig.host(), RelayConfig.port()), CONNECT_TIMEOUT_MS);
			SSLParameters params = ssl.getSSLParameters();
			params.setEndpointIdentificationAlgorithm("HTTPS");
			ssl.setSSLParameters(params);
			ssl.startHandshake();
			socket = ssl;
		} else {
			socket = new Socket();
			socket.connect(new InetSocketAddress(RelayConfig.host(), RelayConfig.port()), CONNECT_TIMEOUT_MS);
		}
		socket.setTcpNoDelay(true);
		if (RelayConfig.useWebSocket()) {
			//the handshake has to finish inside a deadline of its own: a proxy that
			//accepts the connection and then says nothing must not park a thread forever
			socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
			socket = WebSocket.upgrade(socket, RelayConfig.host(), RelayConfig.path());
		}
		return socket;
	}

	public static void write(OutputStream out, JSONObject body) throws IOException {
		byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
		if (bytes.length > MAX_FRAME) {
			throw new IOException("relay frame too large");
		}
		DataOutputStream dos = new DataOutputStream(out);
		dos.writeInt(bytes.length);
		dos.write(bytes);
		dos.flush();
	}

	public static JSONObject read(InputStream in) throws IOException {
		DataInputStream dis = new DataInputStream(in);
		int length = dis.readInt();
		if (length <= 0 || length > MAX_FRAME) {
			throw new IOException("bad relay frame length: " + length);
		}
		byte[] bytes = new byte[length];
		dis.readFully(bytes);
		try {
			return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
		} catch (JSONException e) {
			throw new IOException("bad relay frame", e);
		}
	}

	/**
	 * Sends one handshake and returns the reply, turning a refusal into a
	 * {@link RelayException} carrying the relay's stable error code so the UI can say
	 * something specific rather than "connection failed".
	 */
	public static JSONObject handshake(Socket socket, JSONObject request) throws IOException {
		socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
		write(socket.getOutputStream(), request);
		JSONObject reply = read(socket.getInputStream());
		if (!reply.optBoolean("ok", false)) {
			throw new RelayException(reply.optString("code", "bad_request"), reply.optString("msg", ""));
		}
		return reply;
	}

	public static JSONObject base(String role) {
		JSONObject body = new JSONObject();
		try {
			body.put("v", 1);
			body.put("role", role);
			body.put("client", ClientId.get());
			//the relay costs money to run, so it asks for the store's signed proof that
			//this player subscribes. Local network play never comes through here.
			PaymentService.Receipt receipt = Payments.receipt();
			String devToken = WPDSettings.relayDevToken();
			if (receipt != null) {
				JSONObject ent = new JSONObject();
				ent.put("store", receipt.store);
				ent.put("payload", receipt.payload);
				ent.put("sig", receipt.signature);
				body.put("ent", ent);
			} else if (!devToken.isEmpty() && com.watabou.utils.DeviceCompat.isDebug()) {
				//the developer testing online play without a subscription. The relay only
				//honours this if it was started with the matching secret, so an empty or
				//wrong token here is simply no proof at all - and a store build never
				//presents one, whatever its settings file says
				JSONObject ent = new JSONObject();
				ent.put("store", "dev");
				ent.put("payload", devToken);
				ent.put("sig", "");
				body.put("ent", ent);
			}
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
		return body;
	}
}

package xyz.gabriwar.warpedpixeldungeon.net;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Protocol {

	public static final int FULL_STATE    = 0;
	public static final int DELTA         = 1;
	public static final int LEVEL_CHANGE  = 2;
	public static final int PING          = 3;
	public static final int PONG          = 4;
	public static final int HERO_NAME     = 5;
	public static final int LOBBY_INFO    = 6;
	public static final int YOUR_TURN      = 7;   // Host→Client: your hero's turn
	public static final int PLAYER_ACTION  = 8;   // Client→Host: cell click
	public static final int SHOW_DIALOG    = 9;   // Host→Client: open an interaction window {dialogId, kind, payload}
	public static final int DIALOG_CHOICE  = 10;  // Client→Host: a dialog was resolved {dialogId, choice}
	public static final int JOIN          = 100;
	public static final int JOIN_AS_PLAYER = 101;  // Client→Host: join with hero data
	public static final int PEEK_CHARACTER          = 110; // Client→Host: do you have a stashed hero for this name?
	public static final int PEEK_CHARACTER_RESPONSE = 111; // Host→Client: {found, cls, lvl, hp, ht, items}

	public static void writeMessage(OutputStream out, int type, JSONObject data) throws IOException {
		JSONObject envelope = new JSONObject();
		try {
			envelope.put("t", type);
			envelope.put("d", data != null ? data : new JSONObject());
		} catch (JSONException e) {
			throw new IOException("JSON encoding error", e);
		}
		byte[] bytes = envelope.toString().getBytes(StandardCharsets.UTF_8);
		DataOutputStream dos = new DataOutputStream(out);
		dos.writeInt(bytes.length);
		dos.write(bytes);
		dos.flush();
	}

	public static Message readMessage(InputStream in) throws IOException {
		DataInputStream dis = new DataInputStream(in);
		int length = dis.readInt();
		if (length <= 0 || length > 1024 * 1024) {
			throw new IOException("Invalid message length: " + length);
		}
		byte[] bytes = new byte[length];
		dis.readFully(bytes);
		String json = new String(bytes, StandardCharsets.UTF_8);
		try {
			JSONObject envelope = new JSONObject(json);
			return new Message(envelope.getInt("t"), envelope.getJSONObject("d"));
		} catch (JSONException e) {
			throw new IOException("JSON decoding error", e);
		}
	}

	public static class Message {
		public final int type;
		public final JSONObject data;

		public Message(int type, JSONObject data) {
			this.type = type;
			this.data = data;
		}
	}
}

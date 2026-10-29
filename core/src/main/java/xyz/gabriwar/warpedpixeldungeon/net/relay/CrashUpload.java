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

import java.io.File;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * A crash report sent to the relay as the game dies: the report's text and the run's save
 * (debug/CrashSave), so it arrives whether or not the player has an email app or sends the
 * mail. The relay keeps it and pings the developer; nothing comes back but an id.
 */
public final class CrashUpload {

	//the relay refuses bigger (RELAY_CRASH_MAX_REPORT / _SAVE); a report past this is cut
	private static final int MAX_REPORT = 512 * 1024;
	private static final int MAX_SAVE = 8 * 1024 * 1024;

	private CrashUpload() {}

	/** Sends it and returns the relay's id for it, or null if it could not go. Gives up after
	 *  `deadlineMs`: a dying game must not hang on the network. Never throws. */
	public static String send( String report, File save, long deadlineMs ){
		final String[] id = new String[1];
		Thread t = new Thread( () -> id[0] = sendNow( report, save ), "WPD Crash Upload" );
		t.setDaemon( true );
		t.start();
		try {
			t.join( deadlineMs );
		} catch (InterruptedException ignored) {}
		return id[0];
	}

	//a write is one WebSocket frame, and the relay takes none past 1 MiB (ws.rs MAX_PAYLOAD)
	private static void chunked( OutputStream out, byte[] bytes ) throws java.io.IOException {
		for (int off = 0; off < bytes.length; off += 64 * 1024){
			out.write( bytes, off, Math.min( 64 * 1024, bytes.length - off ) );
		}
	}

	private static String sendNow( String report, File save ){
		try {
			byte[] text = report.getBytes( StandardCharsets.UTF_8 );
			if (text.length > MAX_REPORT) text = java.util.Arrays.copyOf( text, MAX_REPORT );
			byte[] zip = save != null && save.isFile() && save.length() <= MAX_SAVE
					? Files.readAllBytes( save.toPath() ) : new byte[0];

			JSONObject request = new JSONObject();
			request.put( "v", 1 );
			request.put( "role", "crash" );
			request.put( "client", ClientId.get() );
			request.put( "report", text.length );
			request.put( "save", zip.length );

			try (Socket socket = RelayLink.dial()) {
				socket.setSoTimeout( 30000 );
				OutputStream out = socket.getOutputStream();
				RelayLink.write( out, request );
				chunked( out, text );
				chunked( out, zip );
				out.flush();
				JSONObject reply = RelayLink.read( socket.getInputStream() );
				return reply.optBoolean( "ok", false ) ? reply.optString( "id", "?" ) : null;
			}
		} catch (Throwable t) {
			return null;
		}
	}
}

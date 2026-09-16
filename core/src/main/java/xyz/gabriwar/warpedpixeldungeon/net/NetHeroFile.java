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

import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import org.json.JSONObject;

import java.io.IOException;

/**
 * The player's own copy of the hero they play online. The host is the authority on that
 * hero while a game runs, and keeps it in its save between games; this file is what the
 * player still has when the host is a different person, or lost the save. One file per
 * player name, written whenever the host sends a HERO_SAVE.
 *
 * It never enters a single-player slot: a hero made online stays online.
 */
public class NetHeroFile {

	private static final String DIR = "multiplayer";

	private static final String NAME  = "name";
	private static final String CLASS = "cls";
	private static final String LEVEL = "lvl";
	private static final String HOST  = "host";
	private static final String TOKEN = "token";
	private static final String HERO  = "hero";
	private static final String SAVED = "saved_at";

	public static String path(String playerName) {
		StringBuilder safe = new StringBuilder();
		for (char c : playerName.toCharArray()) {
			safe.append(Character.isLetterOrDigit(c) ? c : '_');
		}
		return DIR + "/" + safe + ".dat";
	}

	/** Store what the host sent: {name, cls, lvl, host, token, hero:{...}}. */
	public static void save(JSONObject snapshot) {
		String name = snapshot.optString(NAME, "");
		JSONObject hero = snapshot.optJSONObject(HERO);
		if (name.isEmpty() || hero == null) return;
		Bundle b = new Bundle();
		b.put(NAME, name);
		b.put(CLASS, snapshot.optInt(CLASS, 0));
		b.put(LEVEL, snapshot.optInt(LEVEL, 1));
		b.put(HOST, snapshot.optString(HOST, ""));
		b.put(TOKEN, snapshot.optString(TOKEN, ""));
		//kept as text: the hero's classes are resolved on the host that loads it, not here
		b.put(HERO, hero.toString());
		b.put(SAVED, System.currentTimeMillis());
		try {
			FileUtils.bundleToFile(path(name), b);
			NetManager.log("[NET-CLI] hero saved name=" + name + " lvl=" + snapshot.optInt(LEVEL, 1));
		} catch (IOException e) {
			NetManager.log("[NET-CLI] hero save failed: " + e.getMessage());
		}
	}

	/** The saved hero for this name, or null when there is none. */
	public static Bundle load(String playerName) {
		if (playerName == null || playerName.isEmpty()) return null;
		if (!FileUtils.fileExists(path(playerName))) return null;
		try {
			return FileUtils.bundleFromFile(path(playerName));
		} catch (IOException e) {
			return null;
		}
	}

	public static boolean exists(String playerName) {
		return load(playerName) != null;
	}

	public static void delete(String playerName) {
		if (playerName == null || playerName.isEmpty()) return;
		FileUtils.deleteFile(path(playerName));
	}

	public static int heroClass(Bundle b)  { return b.getInt(CLASS); }
	public static int level(Bundle b)      { return b.getInt(LEVEL); }
	public static String host(Bundle b)    { return b.getString(HOST); }
	public static String token(Bundle b)   { return b.getString(TOKEN); }

	/** The session token the last host issued for this name, or null. */
	public static String token(String playerName) {
		Bundle b = load(playerName);
		if (b == null) return null;
		String t = b.getString(TOKEN);
		return t == null || t.isEmpty() ? null : t;
	}

	/** The hero as the host serialised it, ready to go back in a JOIN_AS_PLAYER. */
	public static JSONObject hero(Bundle b) {
		try {
			return new JSONObject(b.getString(HERO));
		} catch (Exception e) {
			return null;
		}
	}
}

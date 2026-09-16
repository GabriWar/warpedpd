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

import java.security.SecureRandom;

import xyz.gabriwar.warpedpixeldungeon.WPDSettings;

/**
 * A random per-install id, used by the relay to hold a player to one hosted room and one
 * join at a time. It is not a secret and not a credential: anyone can make one up, which
 * is why the relay's real limits are per address. It exists so that an honest client
 * cannot accidentally occupy several sessions at once.
 */
public final class ClientId {

	private ClientId() {}

	public static synchronized String get() {
		String stored = WPDSettings.relayClientId();
		if (stored.length() == 32) {
			return stored;
		}
		byte[] raw = new byte[16];
		new SecureRandom().nextBytes(raw);
		StringBuilder hex = new StringBuilder(32);
		for (byte b : raw) {
			hex.append(Character.forDigit((b >> 4) & 0xF, 16));
			hex.append(Character.forDigit(b & 0xF, 16));
		}
		String id = hex.toString();
		WPDSettings.relayClientId(id);
		return id;
	}
}

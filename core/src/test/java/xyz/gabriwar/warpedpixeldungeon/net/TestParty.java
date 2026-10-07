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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * A co-op party for headless tests: the game made a host with a guest's hero in play, as
 * NetManager has it once a player has joined - without a socket, a relay or a thread. Every
 * join is undone by leave(), which a test calls in a finally (or an @After).
 */
public final class TestParty {

	private TestParty(){}

	/** The game is a host from now on, and this hero one of his guests'. */
	public static void join( Hero guest ) throws Exception {
		heroes().put( new HostServer( 0 ).new ClientConnection( null ), guest );
		mode().set( null, NetManager.NetMode.HOST );
	}

	/** The game is a guest's from now on: a player's client of somebody else's host. */
	public static void guest() throws Exception {
		mode().set( null, NetManager.NetMode.PLAYER );
	}

	/** Back to a game of one: no guests, no host. */
	public static void leave() throws Exception {
		heroes().clear();
		mode().set( null, NetManager.NetMode.OFF );
	}

	@SuppressWarnings("unchecked")
	private static Map<HostServer.ClientConnection, Hero> heroes() throws Exception {
		Field f = NetManager.class.getDeclaredField( "netHeroes" );
		f.setAccessible( true );
		return (Map<HostServer.ClientConnection, Hero>) f.get( null );
	}

	private static Field mode() throws Exception {
		Field f = NetManager.class.getDeclaredField( "mode" );
		f.setAccessible( true );
		return f;
	}
}

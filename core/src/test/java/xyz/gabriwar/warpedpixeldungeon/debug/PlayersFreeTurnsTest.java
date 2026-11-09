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

package xyz.gabriwar.warpedpixeldungeon.debug;

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The freeze watchdog's spin is the turn loop going round with the clock stuck and nobody able to
 * stop it. Turns the player takes for free are not one: a hero stopped before a lock or in roots and
 * tapped again and again, and his walk through a time freeze, left the clock where it was for ten
 * seconds and closed a game that never hung (some ninety reports). A real spin, an actor that goes
 * on for free with no one waiting on the player, is still caught.
 */
public class PlayersFreeTurnsTest {

	private static final Actor SOMEONE = new Actor(){
		@Override protected boolean act(){ return false; }
	};

	private static void set( String field, boolean value ) throws Exception {
		Field f = FreezeWatchdog.class.getDeclaredField( field );
		f.setAccessible( true );
		f.set( null, value );
	}

	@Before
	public void up() throws Exception {
		set( "inGame", false );
		FreezeWatchdog.tick();     //out of a run: every count back to nothing
		set( "inGame", true );
	}

	@After
	public void down() throws Exception {
		set( "inGame", false );
		FreezeWatchdog.tick();
		FreezeWatchdog.tracing = false;
	}

	//what the watchdog says after these seconds of turns at one stuck game time, or null
	private static String seconds( int n, Runnable eachSecond ){
		for (int s = 0; s < n; s++){
			FreezeWatchdog.frame();
			for (int turn = 0; turn < 4; turn++) FreezeWatchdog.turn( SOMEONE, 61f );
			eachSecond.run();
			String what = FreezeWatchdog.tick();
			if (what != null) return what;
		}
		return null;
	}

	@Test
	public void anActorGoingOnForFreeIsStillASpin(){
		String what = seconds( 15, () -> {} );
		assertNotNull( "a loop round and round at 61 with no one waiting on the player", what );
		assertTrue( what, what.contains( "turn loop spinning" ) );
	}

	@Test
	public void aPlayerTappingAtALockIsNoSpin(){
		assertNull( seconds( 15, FreezeWatchdog::playerWait ) );
	}

	@Test
	public void aWalkInStoppedTimeIsNoSpin(){
		assertNull( seconds( 15, FreezeWatchdog::clockHeld ) );
	}
}

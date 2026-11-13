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
 * A crowd at one game time holds the clock still without the game hanging. Fifty albino piranhas set
 * on each other by a scroll of rage, all in sight through mind vision, held it at 100.5: thirty bit
 * there one after another, each bite waiting on its animation before Mob.onAttackComplete spent the
 * turn and handed it back, and after ten seconds of that the freeze watchdog closed the game. A second
 * whose turns paid for themselves is no spin; a turn handed back for free still is.
 */
public class CrowdAtOneTimeTest {

	//a fish of the shoal: its turn waits on its bite (act() returned false), and the bite's end hands
	//the turn back, paid for as Mob.onAttackComplete does, or not
	private static class Biter extends Actor {
		@Override protected boolean act(){ return false; }
		void bite( boolean pays ){
			if (pays) spend( 1f );
			next();
		}
	}

	private static void set( String field, boolean value ) throws Exception {
		Field f = FreezeWatchdog.class.getDeclaredField( field );
		f.setAccessible( true );
		f.set( null, value );
	}

	private static void makeCurrent( Actor a ) throws Exception {
		Field f = Actor.class.getDeclaredField( "current" );
		f.setAccessible( true );
		f.set( null, a );
	}

	@Before
	public void up() throws Exception {
		Actor.clear();
		set( "inGame", false );
		FreezeWatchdog.tick();     //out of a run: every count back to nothing
		set( "inGame", true );
		FreezeWatchdog.frame();
		FreezeWatchdog.tick();     //a second without turns: an earlier test's trace goes too
	}

	@After
	public void down() throws Exception {
		set( "inGame", false );
		FreezeWatchdog.tick();
		FreezeWatchdog.tracing = false;
		makeCurrent( null );
		Actor.clear();
	}

	//one bite: the loop picks the fish at the stuck time, its act() waits, the bite hands the turn back
	private static void bite( boolean pays ) throws Exception {
		Biter b = new Biter();
		FreezeWatchdog.turn( b, Actor.now() );
		makeCurrent( b );
		b.bite( pays );
	}

	//what the watchdog says after these seconds of bites at one stuck game time (two a second: a
	//bite's animation is nine frames at twenty a second), or null
	private static String seconds( int n, boolean[] eachSecond ) throws Exception {
		for (int s = 0; s < n; s++){
			FreezeWatchdog.frame();
			for (boolean pays : eachSecond) bite( pays );
			String what = FreezeWatchdog.tick();
			if (what != null) return what;
		}
		return null;
	}

	@Test
	public void thirtyFishBitingInTurnIsNoSpin() throws Exception {
		assertNull( seconds( 15, new boolean[]{ true, true } ) );
	}

	@Test
	public void fishHandingTheirTurnsBackForFreeIsStillASpin() throws Exception {
		String what = seconds( 15, new boolean[]{ false, false } );
		assertNotNull( "the clock stuck and no turn paid", what );
		assertTrue( what, what.contains( "turn loop spinning" ) );
		assertTrue( what, what.contains( "Biter handed its turn back without spending time" ) );
	}

	@Test
	public void oneGoingForFreeAmongThePaidIsStillASpin() throws Exception {
		String what = seconds( 15, new boolean[]{ true, false } );
		assertNotNull( "a turn back for free each second, whatever the others paid", what );
		assertTrue( what, what.contains( "turn loop spinning" ) );
	}
}

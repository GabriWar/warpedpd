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
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//a freeze report names its cause: the line that handed a turn back for free while the clock stood
//still, and the game round it (the last turns, the log)
public class ReportCauseTest {

	//an actor that hands its turn back, for free or after a turn's wait
	private static class Giver extends Actor {
		@Override protected boolean act(){ return false; }
		void giveBackForFree(){ next(); }
		void giveBackAfterAWait(){ spend( 1f ); next(); }
	}

	private static void makeCurrent( Actor a ) throws Exception {
		Field f = Actor.class.getDeclaredField( "current" );
		f.setAccessible( true );
		f.set( null, a );
	}

	@After
	public void tearDown() throws Exception {
		FreezeWatchdog.tracing = false;
		makeCurrent( null );
	}

	@Test
	public void aFreeHandBackIsCaughtAtItsLine() throws Exception {
		Giver g = new Giver();
		FreezeWatchdog.tracing = true;
		makeCurrent( g );
		g.giveBackForFree();
		String cause = FreezeWatchdog.cause();
		assertTrue( cause, cause.contains( "Giver handed its turn back without spending time" ) );
		assertTrue( "the exact line: " + cause, cause.contains( "Giver.giveBackForFree(ReportCauseTest.java:" ) );
	}

	@Test
	public void aTurnThatSpentTimeIsNoCause() throws Exception {
		Giver g = new Giver();
		FreezeWatchdog.tracing = true;
		makeCurrent( g );
		g.giveBackAfterAWait();
		assertFalse( FreezeWatchdog.cause().contains( "giveBackAfterAWait" ) );
	}

	@Test
	public void theStateNamesTheLastTurnsAndTheLog(){
		Giver g = new Giver();
		CrashContext.begin( g, 98.5f, null );
		CrashContext.end( 98.5f, false );
		CrashContext.begin( g, 98.5f, null );
		CrashContext.log( "You feel a strange calm." );
		String state = CrashContext.describe();
		assertTrue( state, state.contains( "Giver 98.5 -> 98.5 wait" ) );
		assertTrue( "a turn that never came back: " + state, state.contains( "Giver 98.5 -> (still acting)" ) );
		assertTrue( state, state.contains( "You feel a strange calm." ) );
	}
}

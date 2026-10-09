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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.function.LongSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * No strobing: a storm's flashes 0.7 s apart at least, the game's 2 s, a heavy blow's red 0.35 s,
 * and the storm's and the game's together never more than three in ten seconds.
 */
public class FlashGateTest {

	private LongSupplier saved;
	private double now;

	@Before
	public void setUp(){
		saved = FlashGate.clock;
		now = 1000;
		FlashGate.clock = () -> (long)(now * 1e9);
		FlashGate.reset();
	}

	@After
	public void tearDown(){
		FlashGate.clock = saved;
		FlashGate.reset();
	}

	private void at( double t ){
		now = 1000 + t;
	}

	@Test
	public void eachKindKeepsItsGap(){
		at( 0 );
		assertTrue( FlashGate.storm() );
		at( 0.69 );
		assertFalse( FlashGate.storm() );
		at( 0.71 );
		assertTrue( FlashGate.storm() );

		FlashGate.reset();
		at( 20 );
		assertTrue( FlashGate.game() );
		at( 21.9 );
		assertFalse( FlashGate.game() );
		at( 22.01 );
		assertTrue( FlashGate.game() );

		at( 40 );
		assertTrue( FlashGate.damage() );
		at( 40.3 );
		assertFalse( FlashGate.damage() );
		at( 40.36 );
		assertTrue( FlashGate.damage() );
	}

	@Test
	public void theStormAndTheGameShareThreeInTenSeconds(){
		at( 0 );
		assertTrue( FlashGate.storm() );
		at( 0.1 );
		assertTrue( "the game's own gap is its own", FlashGate.game() );
		at( 0.8 );
		assertTrue( FlashGate.storm() );
		at( 1.6 );
		assertFalse( "a fourth within ten seconds", FlashGate.storm() );
		at( 2.2 );
		assertFalse( FlashGate.game() );
		//the damage flash is not counted
		assertTrue( FlashGate.damage() );
		at( 10.01 );
		assertTrue( "the first is ten seconds old", FlashGate.storm() );
		at( 10.2 );
		assertTrue( FlashGate.game() );
		//the window now holds 0.8, 10.01 and 10.2: the next once 0.8 is ten seconds gone
		at( 10.75 );
		assertFalse( FlashGate.storm() );
		at( 10.81 );
		assertTrue( FlashGate.storm() );
	}

	@Test
	public void aStormFlashingAllNightNeverStrobes(){
		//asked every frame for a minute: never more than three in any ten seconds
		java.util.ArrayList<Double> passed = new java.util.ArrayList<>();
		for (int f = 0; f < 60 * 60; f++){
			at( f / 60.0 );
			if (FlashGate.storm()) passed.add( f / 60.0 );
		}
		for (int i = 3; i < passed.size(); i++){
			assertTrue( passed.get( i ) - passed.get( i - 3 ) >= 10 - 1e-9 );
		}
		for (int i = 1; i < passed.size(); i++){
			assertTrue( passed.get( i ) - passed.get( i - 1 ) >= FlashGate.STORM_GAP - 1e-9 );
		}
		assertEquals( 18, passed.size() );
	}
}

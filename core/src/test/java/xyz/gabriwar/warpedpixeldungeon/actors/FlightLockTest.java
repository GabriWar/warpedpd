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

package xyz.gabriwar.warpedpixeldungeon.actors;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Frozen or paralysed, a flier drops, and takes off again only once every lock
 * is gone; flight buffs that start or end in the meantime are honoured.
 */
public class FlightLockTest {

	private static Char walker(){
		return new Char(){
			@Override public boolean act(){ return true; }
		};
	}

	@Test
	public void innateFlierLandsAndTakesOffWhenEveryLockEnds(){
		Char c = walker();
		c.flying = true;
		c.loseFlight();                 //frozen
		assertFalse( c.flying );
		c.loseFlight();                 //and paralysed
		c.regainFlight();               //the frost thaws
		assertFalse( "still paralysed", c.flying );
		c.regainFlight();               //paralysis ends
		assertTrue( c.flying );
	}

	@Test
	public void levitationEndingWhileGroundedIsNotHandedBack(){
		Char c = walker();
		c.grantFlight();                //levitating
		c.loseFlight();                 //frozen mid-air
		assertFalse( c.flying );
		c.endFlight();                  //levitation runs out while frozen
		c.regainFlight();               //thaw
		assertFalse( "levitation is over", c.flying );
	}

	@Test
	public void levitationStartingWhileGroundedTakesEffectOnRelease(){
		Char c = walker();
		c.loseFlight();                 //paralysed on the ground
		c.grantFlight();                //a levitation potion takes hold
		assertFalse( c.flying );
		c.regainFlight();
		assertTrue( c.flying );
	}

	@Test
	public void walkerStaysOnTheGround(){
		Char c = walker();
		c.loseFlight();
		c.regainFlight();
		assertFalse( c.flying );
		c.regainFlight();               //an unmatched release changes nothing
		assertFalse( c.flying );
	}
}

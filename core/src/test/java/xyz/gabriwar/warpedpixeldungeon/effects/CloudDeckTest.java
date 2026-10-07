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

package xyz.gabriwar.warpedpixeldungeon.effects;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CloudDeckTest {

	private static float cover( int[] px ){
		int n = 0;
		for (int c : px) if (c != 0) n++;
		return n / (float) px.length;
	}

	@Test
	public void aDeckIsItsSeeds(){
		int[] a = CloudDeck.paint( 42L, 128 ), b = CloudDeck.paint( 42L, 128 ), c = CloudDeck.paint( 43L, 128 );
		assertArrayEquals( a, b );
		assertFalse( java.util.Arrays.equals( a, c ) );
	}

	//about two thirds cloud with lanes of open air, on the small tile and the real one alike
	@Test
	public void aboutTwoThirdsCloud(){
		for (int size : new int[]{ 128, CloudDeck.SIZE }){
			float cover = cover( CloudDeck.paint( 7L, size ) );
			assertTrue( size + ": " + cover, cover > 0.35f && cover < 0.9f );
		}
	}

	@Test
	public void paintsInReasonableTime(){
		CloudDeck.paint( 1L, CloudDeck.SIZE );
		long t = System.nanoTime();
		CloudDeck.paint( 2L, CloudDeck.SIZE );
		long ms = (System.nanoTime() - t) / 1_000_000;
		System.out.println( "[clouds] deck painted in " + ms + " ms" );
		assertTrue( "painting took " + ms + " ms", ms < 3000 );
	}
}

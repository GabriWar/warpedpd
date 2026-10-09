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

package xyz.gabriwar.warpedpixeldungeon.windows;

import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugMenu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

public class WndDebugPickerTest {

	private static int depthOf( String label ){
		for (Object[] l : WndDebugPicker.TRAVEL_LEVELS) if (l[2].equals( label )) return (int)l[0];
		throw new AssertionError( "no travel entry " + label );
	}

	//it went to 98, a dead end: the showcase is at 85, like the Travel tab's own button
	@Test
	public void theRoomsShowcaseEntryGoesWhereTheShowcaseIs(){
		assertEquals( DebugMenu.DEV_ROOMS_DEPTH, depthOf( "Rooms Showcase (debug)" ) );
		assertEquals( 86, depthOf( "Warped Rooms (debug)" ) );
		assertEquals( DebugMenu.OVERWORLD_DEPTH, depthOf( "Overworld (debug)" ) );
		for (Object[] l : WndDebugPicker.TRAVEL_LEVELS) assertNotEquals( 98, l[0] );
	}

	@Test
	public void everyTravelEntryIsAPlaceOfItsOwn(){
		HashSet<String> places = new HashSet<>();
		for (Object[] l : WndDebugPicker.TRAVEL_LEVELS){
			assertTrue( Arrays.toString( l ), places.add( l[0] + "/" + l[1] ) );
		}
	}

	private static boolean[] room( int w, int h ){
		boolean[] open = new boolean[w * h];
		Arrays.fill( open, true );
		return open;
	}

	//they all stood on the tapped cell
	@Test
	public void spawnedMobsEachGetACell(){
		boolean[] open = room( 5, 5 );
		ArrayList<Integer> cells = WndDebugPicker.spawnCells( 12, 5, 5, open, c -> false );
		assertEquals( 5, cells.size() );
		assertEquals( 12, (int)cells.get( 0 ) );
		assertEquals( 5, new HashSet<>( cells ).size() );
		for (int c : cells){
			assertTrue( Math.abs( c % 5 - 2 ) <= 1 && Math.abs( c / 5 - 2 ) <= 1 );
		}
	}

	@Test
	public void anOccupiedCellIsSkipped(){
		boolean[] open = room( 5, 5 );
		ArrayList<Integer> cells = WndDebugPicker.spawnCells( 12, 3, 5, open, c -> c == 12 );
		assertEquals( 3, cells.size() );
		assertFalse( cells.contains( 12 ) );
	}

	@Test
	public void wallsAndRowEndsAreNotCrossed(){
		//x=0 open, x=1 wall, x=2 open: from the left column the right one is out of reach
		int w = 3, h = 3;
		boolean[] open = room( w, h );
		for (int y = 0; y < h; y++) open[y * w + 1] = false;
		ArrayList<Integer> cells = WndDebugPicker.spawnCells( 3, 10, w, open, c -> false );
		assertEquals( 3, cells.size() );
		for (int c : cells) assertEquals( 0, c % w );
	}

	//a single bat dropped over a chasm, or a monster put into a wall, goes where it was tapped,
	//as it always did; only the ones after it need open floor
	@Test
	public void theTappedCellTakesTheFirstMobWhateverItIs(){
		int w = 3, h = 3;
		boolean[] open = room( w, h );
		open[4] = false;
		ArrayList<Integer> cells = WndDebugPicker.spawnCells( 4, 2, w, open, c -> false );
		assertEquals( 2, cells.size() );
		assertEquals( 4, (int)cells.get( 0 ) );
		assertTrue( open[cells.get( 1 )] );

		//alone in a wall: still placed
		boolean[] walls = new boolean[w * h];
		assertEquals( Arrays.asList( 4 ), WndDebugPicker.spawnCells( 4, 1, w, walls, c -> false ) );
		//but never on top of someone
		assertTrue( WndDebugPicker.spawnCells( 4, 1, w, walls, c -> c == 4 ).isEmpty() );
	}
}

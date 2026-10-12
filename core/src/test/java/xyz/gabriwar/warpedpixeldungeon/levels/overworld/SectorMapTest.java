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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import org.junit.Test;

import java.util.HashMap;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The diff store's sector index (SectorMap): a window derived from the indexed store, or from the
 * worker's snapshot of the sectors under it, gets exactly the map the old pass over every edit in
 * the world gave, with the edits spread over thousands of cells; and the index keeps up with
 * every write.
 */
public class SectorMapTest {

	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	//edits far and wide, and a crowd of them inside and on the edges of the window at (ox, oy)
	private static HashMap<Long, Integer> spread( int ox, int oy, long seed ){
		Random r = new Random( seed );
		HashMap<Long, Integer> edits = new HashMap<>();
		for (int i = 0; i < 4000; i++){
			edits.put( OverworldLevel.worldKey( r.nextInt( 20000 ) - 10000, r.nextInt( 20000 ) - 10000 ), 1 + r.nextInt( 120 ) );
		}
		for (int i = 0; i < 3000; i++){
			edits.put( OverworldLevel.worldKey( ox - 3 + r.nextInt( W + 6 ), oy - 3 + r.nextInt( H + 6 ) ), 1 + r.nextInt( 120 ) );
		}
		return edits;
	}

	private static int[] laid( HashMap<Long, Integer> edits, int ox, int oy ){
		int[] map = new int[W * H];
		WindowGenerator.overlayDiffs( map, edits, ox, oy );
		return map;
	}

	@Test
	public void theIndexedStoreLaysTheSameWindowAsEveryEdit(){
		int[][] origins = { { -88, -88 }, { 4000, -7000 }, { -9000, 9000 }, { 63, 64 }, { -64, -65 } };
		for (int[] o : origins){
			HashMap<Long, Integer> plain = spread( o[0], o[1], o[0] * 31L + o[1] );
			SectorMap<Integer> indexed = new SectorMap<>( plain );
			assertEquals( plain.size(), indexed.size() );
			int[] old = laid( plain, o[0], o[1] );
			assertArrayEquals( "the indexed store at " + o[0] + "," + o[1], old, laid( indexed, o[0], o[1] ) );
			HashMap<Long, Integer> snap = indexed.snapshotIn( o[0], o[1], o[0] + W, o[1] + H );
			assertTrue( "the worker's snapshot holds only the window's sectors", snap.size() < plain.size() / 2 );
			assertArrayEquals( "the worker's snapshot at " + o[0] + "," + o[1], old, laid( snap, o[0], o[1] ) );
		}
	}

	@Test
	public void theIndexKeepsUpWithEveryWrite(){
		HashMap<Long, Integer> plain = spread( 0, 0, 7L );
		SectorMap<Integer> indexed = new SectorMap<>();
		for (HashMap.Entry<Long, Integer> e : plain.entrySet()) indexed.put( e.getKey(), e.getValue() );
		//half of them taken back, some written again
		Random r = new Random( 11 );
		for (Long k : plain.keySet().toArray( new Long[0] )){
			if (r.nextBoolean()){
				plain.remove( k );
				indexed.remove( k );
			} else if (r.nextInt( 4 ) == 0){
				plain.put( k, 99 );
				indexed.put( k, 99 );
			}
		}
		assertArrayEquals( laid( plain, 0, 0 ), laid( indexed, 0, 0 ) );
		assertArrayEquals( laid( plain, 0, 0 ), laid( indexed.snapshotIn( 0, 0, W, H ), 0, 0 ) );
		int seen[] = { 0 };
		indexed.forEachIn( -100000, -100000, 100000, 100000, (x, y, v) -> seen[0]++ );
		assertEquals( "every entry is in its sector, once", indexed.size(), seen[0] );
		indexed.clear();
		assertEquals( 0, indexed.sectorCount() );
		assertFalse( indexed.containsKey( OverworldLevel.worldKey( 0, 0 ) ) );
	}

	@Test
	public void nothingWritesBehindTheIndex(){
		SectorMap<Integer> indexed = new SectorMap<>();
		indexed.put( OverworldLevel.worldKey( 5, 5 ), 1 );
		try {
			java.util.Iterator<java.util.Map.Entry<Long, Integer>> it = indexed.entrySet().iterator();
			it.next();
			it.remove();
			fail( "no entry is dropped through a view" );
		} catch (UnsupportedOperationException expected){
			//as it should
		}
		try {
			indexed.keySet().clear();
			fail( "the key view is read-only" );
		} catch (UnsupportedOperationException expected){
			//as it should
		}
	}
}

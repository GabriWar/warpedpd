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

package xyz.gabriwar.warpedpixeldungeon.tiles;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowSlideTest;
import com.watabou.noosa.Tilemap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * The wall blockers' border stays cleared through a window slide: sliding the window north moved
 * a blocker onto the first cell under the top row, which the slide did not recompute, and the next
 * fog update there read the map at -1 (crashed on the surface as grass regrew).
 */
public class WallBlockingSlideTest {

	private static final int W = 32, H = 32;
	private static final int CLEARED = -2;

	private Level savedLevel;

	@BeforeClass
	public static void boot() throws Exception {
		WindowSlideTest.boot();
	}

	@AfterClass
	public static void unboot(){
		WindowSlideTest.unboot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		Level level = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		//solid rock, all of it seen but the very first cell
		level.setSize( W, H );
		level.discoverable = new boolean[W * H];
		Arrays.fill( level.discoverable, true );
		Arrays.fill( level.visited, true );
		level.visited[0] = false;
		Dungeon.level = level;
	}

	@After
	public void tearDown(){
		Dungeon.level = savedLevel;
	}

	private static int[] data( WallBlockingTilemap t ) throws Exception {
		Field f = Tilemap.class.getDeclaredField( "data" );
		f.setAccessible( true );
		return (int[]) f.get( t );
	}

	@Test
	public void aSlideNorthLeavesTheBorderCleared() throws Exception {
		WallBlockingTilemap blocking = new WallBlockingTilemap();
		blocking.updateMap();
		//the rock's left edge is blocked inside, the border is not
		assertNotEquals( CLEARED, data( blocking )[2 * W] );
		assertEquals( CLEARED, data( blocking )[W] );

		blocking.applyShift( blocking.prepareShift( 0, 1 ) );
		assertEquals( CLEARED, data( blocking )[W] );
		//the fog's update round that cell
		blocking.updateArea( W, 1 );
		assertEquals( CLEARED, data( blocking )[W] );
	}

	@Test
	public void aBlockerOnTheBorderIsClearedByTheFogNotReadPast() throws Exception {
		WallBlockingTilemap blocking = new WallBlockingTilemap();
		blocking.updateMap();
		//however one got there
		data( blocking )[W] = data( blocking )[2 * W];
		blocking.updateArea( W, 1 );
		assertEquals( CLEARED, data( blocking )[W] );
	}
}

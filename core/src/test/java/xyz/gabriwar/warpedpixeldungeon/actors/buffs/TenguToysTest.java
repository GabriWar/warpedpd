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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TenguToysTest {

	private static final int W = 15, MID = 7 * W + 7;

	private Level previous;
	private String previousVersion;

	@Before public void setup(){
		previous = Dungeon.level;
		previousVersion = Game.version;
		Game.version = "test";
		Level floor = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){}
			@Override protected void createItems(){}
		};
		floor.setSize( W, W );
		floor.traps = new com.watabou.utils.SparseArray<>();
		floor.plants = new com.watabou.utils.SparseArray<>();
		floor.heaps = new com.watabou.utils.SparseArray<>();
		floor.blobs = new java.util.HashMap<>();
		java.util.Arrays.fill( floor.solid, true );
		for (int y = 1; y < W - 1; y++) for (int x = 1; x < W - 1; x++) Level.set( y * W + x, Terrain.EMPTY, floor );
		Dungeon.level = floor;
	}

	@After public void cleanup(){
		Dungeon.level = previous;
		Game.version = previousVersion;
		if (previous != null) PathFinder.setMapSize( previous.width(), previous.height() );
	}

	@Test public void shockerArcsEveryWayTwoTilesOut(){
		ArrayList<Integer> cells = TenguShockBomb.arcCells( MID );
		assertEquals( 16, cells.size() );
		for (int dir : PathFinder.NEIGHBOURS8){
			assertTrue( cells.contains( MID + dir ) );
			assertTrue( cells.contains( MID + 2 * dir ) );
		}
		assertFalse( cells.contains( MID + 2 * W + 1 ) ); //a knight's move is not on any line
	}

	@Test public void shockerArcsStopAtWalls(){
		Level.set( MID + 1, Terrain.WALL, Dungeon.level );
		ArrayList<Integer> cells = TenguShockBomb.arcCells( MID );
		assertFalse( cells.contains( MID + 1 ) );
		assertFalse( cells.contains( MID + 2 ) );
		assertEquals( 14, cells.size() );
	}

	@Test public void fireBombReachesTwoTilesOnlyAtMastery(){
		assertEquals( 9, TenguFireBomb.fireCells( MID, 2 ).size() );
		ArrayList<Integer> mastered = TenguFireBomb.fireCells( MID, 3 );
		assertEquals( 25, mastered.size() );
		assertTrue( mastered.contains( MID + 2 * W + 2 ) );
	}
}

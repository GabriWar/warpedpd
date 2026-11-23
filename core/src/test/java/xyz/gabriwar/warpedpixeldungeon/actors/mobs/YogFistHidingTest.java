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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * A Yog fist knocked to half health flees to a cell out of the hero's sight. It picked cells at
 * random until one was hidden, so on a floor with every cell in sight (the town and its inn have
 * no fog of war) the loop never ended and the game froze (2 reports, fists in the town's inn).
 * It now prefers a hidden cell, takes any open one when none is hidden, and stays put with none.
 */
public class YogFistHidingTest {

	private static final int W = 10;
	private static final int FIST = 2 + 2 * W;

	private int depth;
	private Level savedLevel;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		savedLevel = Dungeon.level;
		Actor.clear();
		Dungeon.depth = 25;
		floor();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.level = savedLevel;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	private static YogFist fist(){
		YogFist fist = new YogFist.BrightFist();
		fist.pos = FIST;
		Dungeon.level.mobs.add( fist );
		Actor.add( fist );
		return fist;
	}

	@Test
	public void onAFloorAllInSightItStillFindsACell(){
		Arrays.fill( Dungeon.level.heroFOV, true );
		int cell = fist().hidingCell();
		assertNotEquals( "every cell in sight: it takes an open one", -1, cell );
		assertFalse( Dungeon.level.solid[cell] );
		assertNotEquals( FIST, cell );
	}

	@Test
	public void itPrefersACellOutOfSight(){
		Arrays.fill( Dungeon.level.heroFOV, true );
		int hidden = 7 + 7 * W;
		Dungeon.level.heroFOV[hidden] = false;
		assertEquals( hidden, fist().hidingCell() );
	}

	@Test
	public void withNoFreeCellItStaysPut(){
		Level l = Dungeon.level;
		for (int i = 0; i < l.length(); i++) if (i != FIST) l.map[i] = Terrain.WALL;
		l.buildFlagMaps();
		Arrays.fill( l.heroFOV, true );
		assertEquals( -1, fist().hidingCell() );
		assertTrue( l.passable[FIST] );
	}

	//open floor walled round, no exit (the inn has none the fist could path to)
	private static void floor(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int x = 0; x < W; x++) l.map[x] = l.map[x + (W - 1) * W] = Terrain.WALL;
		for (int y = 0; y < W; y++) l.map[y * W] = l.map[W - 1 + y * W] = Terrain.WALL;
		l.buildFlagMaps();
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
	}
}

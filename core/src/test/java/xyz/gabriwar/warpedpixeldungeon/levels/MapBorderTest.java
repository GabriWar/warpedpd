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

package xyz.gabriwar.warpedpixeldungeon.levels;

import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Water;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.tiles.TownInteriors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The map's border is never walked on: every neighbour lookup (PathFinder's steps, a hero's
 * eight neighbours) reads one cell out, so a hero on the border reads off the map. The town's
 * library has a ring of floor round its walls, on the border; a tester broke the library's
 * wall, read a Scroll of Regrowth (which greens every bare floor cell) and walked out to the
 * map's edge, and the game crashed.
 */
public class MapBorderTest {

	private static final int W = TownInteriors.LIBRARY_W, H = TownInteriors.LIBRARY_H;

	private Level previous;
	private Hero previousHero;
	private TownLibraryLevel library;

	@BeforeClass public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before public void setup(){
		previous = Dungeon.level;
		previousHero = Dungeon.hero;
		Actor.clear();
		library = new TownLibraryLevel();
		assertTrue( library.applyNetworkMap( TownInteriors.libraryMap(), W, H ) );
		Dungeon.level = library;
		//in from the door, a step into the room
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = TownInteriors.LIBRARY_ENTRANCE - W;
	}

	@After public void cleanup(){
		Actor.clear();
		Dungeon.level = previous;
		Dungeon.hero = previousHero;
		if (previous != null) PathFinder.setMapSize( previous.width(), previous.height() );
	}

	private static boolean onBorder( int cell ){
		int x = cell % W, y = cell / W;
		return x == 0 || y == 0 || x == W - 1 || y == H - 1;
	}

	private void assertBorderShut(){
		for (int c = 0; c < library.length(); c++){
			if (!onBorder( c )) continue;
			assertFalse( "border cell " + c + " is walkable", library.passable[c] );
			assertFalse( "border cell " + c + " is walkable", library.avoid[c] );
			assertTrue( "border cell " + c + " is not solid", library.solid[c] );
		}
	}

	@Test public void theLibrarysFloorRingIsOnTheBorder(){
		//what makes the library the place it happened: bare floor on the map's edge
		assertEquals( Terrain.EMPTY, library.map[0] );
		assertEquals( Terrain.EMPTY, library.map[(H - 1) * W + W - 1] );
		assertBorderShut();
	}

	@Test public void regrowthGreensTheRingButLeavesItShut(){
		//the scroll seeds water on every cell; the water greens every bare floor cell it lies on
		for (int c = 0; c < library.length(); c++){
			Blob.seed( c, 40, Water.class, library );
		}
		library.blobs.get( Water.class ).act();

		assertEquals( Terrain.HIGH_GRASS, library.map[0] );
		assertEquals( Terrain.HIGH_GRASS, library.map[(H - 1) * W + W - 2] );
		assertBorderShut();

		//the room's bottom right corner, outside the building: a hero there is not let onto the
		//edge beside him (Hero.getCloser steps onto a neighbour that is passable or avoided), where
		//his next step would read the row below the map
		int inside = (H - 2) * W + W - 2, edge = (H - 1) * W + W - 2;
		assertTrue( library.passable[inside] );
		assertTrue( library.adjacent( inside, edge ) );
		assertFalse( library.passable[edge] || library.avoid[edge] );
		assertTrue( edge + PathFinder.NEIGHBOURS8[7] >= library.length() );
	}

	@Test public void aBlastAtTheDoorwayLeavesTheBorderWallsStanding(){
		//the doorway sits one row above the bottom edge, walled on three sides
		int door = TownInteriors.LIBRARY_ENTRANCE;
		assertEquals( Terrain.WALL, library.map[door + W] );

		new Bomb().explode( door );

		//the walls beside the doorway come down, as a blast's walls do...
		assertEquals( Terrain.EMPTY, library.map[door - 1] );
		assertEquals( Terrain.EMPTY, library.map[door + 1] );
		//...but the map's own frame below it stands, as it does a pick
		assertEquals( Terrain.WALL, library.map[door + W - 1] );
		assertEquals( Terrain.WALL, library.map[door + W] );
		assertEquals( Terrain.WALL, library.map[door + W + 1] );
		assertBorderShut();
	}

	@Test public void anyFloorLaidOnTheBorderStaysShut(){
		//whatever else lays floor on the edge (a fire's embers, a builder's tool): never a way out
		for (int c = 0; c < library.length(); c++){
			if (onBorder( c )) Level.set( c, Terrain.EMPTY_SP, library );
		}
		assertBorderShut();
		for (int c = 0; c < library.length(); c++){
			if (onBorder( c )) assertTrue( library.losBlocking[c] );
		}
	}
}

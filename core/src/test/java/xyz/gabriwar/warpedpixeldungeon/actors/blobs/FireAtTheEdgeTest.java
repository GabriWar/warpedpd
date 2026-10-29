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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
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

import static org.junit.Assert.assertTrue;

/**
 * Fire burning on the row (or column) next to the map's outer wall: its spread is scanned one
 * cell past what burns, onto the wall ring, and asks whether a cell can catch before it looks at
 * the neighbours, so the ring's missing neighbours past the map's edge are never read. The Dwarf
 * King's entry room (15x48, bookshelves on row 46) crashed every turn when they burned.
 */
public class FireAtTheEdgeTest {

	private static final int W = 15, H = 48;

	private Level savedLevel;
	private Hero savedHero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	//a floor walled round whose cells next to the walls are all grass
	private static Level floor(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( W, H );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int x = 0; x < W; x++) l.map[x + W] = l.map[x + (H - 2) * W] = Terrain.GRASS;
		for (int y = 0; y < H; y++) l.map[1 + y * W] = l.map[W - 2 + y * W] = Terrain.GRASS;
		for (int x = 0; x < W; x++) l.map[x] = l.map[x + (H - 1) * W] = Terrain.WALL;
		for (int y = 0; y < H; y++) l.map[y * W] = l.map[W - 1 + y * W] = Terrain.WALL;
		l.buildFlagMaps();
		PathFinder.setMapSize( W, H );
		Dungeon.level = l;
		//burnt grass opens the view, which is looked at again from the hero
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = W / 2 + (H / 2) * W;
		return l;
	}

	@Test
	public void fireNextToTheWallsBurnsOutWithoutReadingPastTheMap(){
		Level l = floor();
		Fire fire = Blob.seed( 5 + (H - 2) * W, 4, Fire.class, l );
		Blob.seed( 5 + W, 4, Fire.class, l );
		Blob.seed( 1 + 20 * W, 4, Fire.class, l );
		Blob.seed( W - 2 + 30 * W, 4, Fire.class, l );
		for (int t = 0; t < 400 && fire.volume > 0; t++) fire.act();
		assertTrue( "the fire burnt out", fire.volume == 0 );
		for (int x = 1; x < W - 1; x++){
			assertTrue( "the grass by the bottom wall burnt", l.map[x + (H - 2) * W] != Terrain.GRASS );
		}
	}
}

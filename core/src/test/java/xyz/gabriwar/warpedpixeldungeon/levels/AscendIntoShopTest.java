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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
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

/**
 * A hero climbing with the Amulet walks into a town shop: the shop (branch 6) shares a floor's depth
 * number, and the Ascension took it for a floor of the climb and spawned a monster on it, though a
 * shop makes none (createMob null), which crashed the way in.
 */
public class AscendIntoShopTest {

	private static final int W = 16;
	private Level savedLevel;
	private Hero savedHero;
	private int savedDepth, savedBranch, savedAscent;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedDepth = Dungeon.depth;
		savedBranch = Dungeon.branch;
		savedAscent = Statistics.highestAscent;
		Actor.clear();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Dungeon.depth = savedDepth;
		Dungeon.branch = savedBranch;
		Statistics.highestAscent = savedAscent;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	//a walled room that makes no monsters, as a shop
	private static Level shop(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public Mob createMob(){ return null; }
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
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int i = 0; i < W; i++){
			l.map[i] = l.map[i + (W - 1) * W] = l.map[i * W] = l.map[W - 1 + i * W] = Terrain.WALL;
		}
		l.buildFlagMaps();
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
		Hero hero = new Hero();
		hero.pos = 3 + 3 * W;
		Dungeon.hero = hero;
		return l;
	}

	@Test
	public void aLevelWithNoMonstersSpawnsNone(){
		Level l = shop();
		assertFalse( l.spawnMob( 12 ) );
		assertEquals( 0, l.mobs.size() );
	}

	@Test
	public void aTownShopIsNoStepOfTheAscent(){
		Level l = shop();
		Dungeon.depth = 4;
		Dungeon.branch = 6;
		Statistics.highestAscent = 10;
		new AscensionChallenge().onLevelSwitch();
		assertEquals( 10, Statistics.highestAscent );
		assertEquals( 0, l.mobs.size() );
	}
}

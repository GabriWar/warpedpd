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

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.plants.Chillisnapper;
import xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * State a char carried over from somewhere else, crashing the game (11261 reports): a hunting
 * mob's target cell from a bigger floor (a succubus at depth 22, 5 reports), a hero's sight
 * still sized for the floor he left when a plant went off under him (the surface, 1 report),
 * and a char with no sprite bled by a hit (depth 11, 3 reports).
 */
public class LeftoverStateTest {

	private static final int W = 10;

	private int depth;
	private Level savedLevel;
	private Hero savedHero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.depth = 22;
		floor();
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 1 + W;
		Dungeon.hero.sprite = stub();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	@Test
	public void aTargetOffTheFloorIsDropped(){
		Succubus s = new Succubus();
		s.pos = 5 + 5 * W;
		s.sprite = stub();
		s.state = s.HUNTING;
		s.target = W * W + 50;
		//a wall between them: out of its sight, it heads for its target
		for (int y = 1; y < W - 1; y++) Dungeon.level.map[4 + y * W] = Terrain.WALL;
		Dungeon.level.buildFlagMaps();
		Actor.add( Dungeon.hero );
		s.enemy = Dungeon.hero; //still hunting the hero, who came back
		Dungeon.level.mobs.add( s );
		Actor.add( s );
		Dungeon.level.updateFieldOfView( Dungeon.hero, Dungeon.hero.fieldOfView = new boolean[Dungeon.level.length()] );

		s.act();

		assertTrue( "a cell of this floor, or none: " + s.target, s.target < Dungeon.level.length() );
	}

	@Test
	public void aPlantReadsNoSightLeftFromAnotherFloor(){
		Rat r = new Rat();
		r.pos = 8 + 8 * W;
		Dungeon.level.mobs.add( r );
		Dungeon.hero.fieldOfView = new boolean[20]; //the last floor's, smaller than this one
		Arrays.fill( Dungeon.hero.fieldOfView, true );

		Chillisnapper c = new Chillisnapper();
		c.pos = Dungeon.hero.pos;
		c.activate( Dungeon.hero );
	}

	@Test
	public void aCharWithNoSpriteStillBleeds(){
		Rat r = new Rat();
		r.pos = 2 + W;
		r.HP = r.HT = 100;
		Dungeon.level.mobs.add( r );
		Actor.add( r );
		Buff.affect( Dungeon.hero, Sickle.HarvestBleedTracker.class );

		r.damage( 10, Dungeon.hero );

		assertNotNull( r.buff( Bleeding.class ) );
		assertEquals( 100, r.HP );
	}

	private static MobSprite stub(){
		return new MobSprite(){
			@Override public void place( int cell ){ }
			@Override public void update(){ }
			@Override public void killAndErase(){ }
			@Override public void showLost(){ }
		};
	}

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

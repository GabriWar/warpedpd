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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The Tengu Den's boss dies at 0 HP. Tengu.isAlive kept every Tengu on a level's mob list alive, for the
 * prison arena whose progress() takes him off it; nothing in the den does, so he stood at 0/400 for good.
 * One already left at 0 HP falls on his own next turn, wherever he stands.
 */
public class TenguDenDeathTest {

	private static final int W = 16;
	private Level savedLevel;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
	}

	@After
	public void tearDown(){
		Dungeon.level = savedLevel;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	//a bare floor walled round, standing in for the den (anything but the prison arena)
	private static Level floor(){
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
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int i = 0; i < W; i++){
			l.map[i] = l.map[i + (W - 1) * W] = l.map[i * W] = l.map[W - 1 + i * W] = Terrain.WALL;
		}
		l.buildFlagMaps();
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
		return l;
	}

	@Test
	public void theDenBossAtNoHealthIsDeadThoughStillOnTheLevel(){
		Level l = floor();
		Tengu tengu = new Tengu();
		tengu.makeDenBoss();
		tengu.pos = 3 + 3 * W;
		l.mobs.add( tengu );
		assertTrue( tengu.isAlive() );
		tengu.HP = 0;
		assertFalse( "0/400 and on the den's mob list: dead, so Char.damage calls die()", tengu.isAlive() );
	}

	@Test
	public void aDenBossSavedAtNoHealthFallsOnHisOwnTurn(){
		Level l = floor();
		final boolean[] died = new boolean[1];
		Tengu tengu = new Tengu(){
			@Override
			public void die( Object cause ){
				died[0] = true;
			}
		};
		tengu.makeDenBoss();
		tengu.pos = 3 + 3 * W;
		l.mobs.add( tengu );
		tengu.HP = 0;
		assertTrue( tengu.act() );
		assertTrue( "he falls without being hit again", died[0] );
	}
}

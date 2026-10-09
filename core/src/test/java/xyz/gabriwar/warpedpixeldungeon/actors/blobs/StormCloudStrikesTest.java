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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.StormStrikes;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
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
import static org.junit.Assert.assertTrue;

/**
 * Every storm cloud strikes (StormCloud's owed strikes): at least once in its life and twice
 * three times in four, the natural strikes counting toward it; a patch the weather lays at once is
 * one cloud, a cloud seeded apart is another, and what a cloud owes is kept over a save. A fiery
 * one in its rain on the world is quenched by its health, not by the world's slot depth.
 */
public class StormCloudStrikesTest {

	private static final int W = 40, CENTER = 20 + 20 * W;

	private Level savedLevel;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		Actor.clear();
		drain();
		ClimateManager.reset();
		//a dungeon floor no rain reaches: a cloud rains itself out in a few turns
		ClimateManager.debugPrecipOverride = 0f;
	}

	@After
	public void tearDown(){
		Actor.clear();
		drain();
		Dungeon.level = savedLevel;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
		ClimateManager.debugPrecipTypeOverride = null;
		ClimateManager.debugPrecipOverride = Float.NaN;
		ClimateManager.reset();
	}

	private static int drain(){
		int n = 0;
		StormStrikes.Bolt b;
		while ((b = StormStrikes.next()) != null) if (b.cloud) n++;
		return n;
	}

	//a bare stone floor walled round, nothing on it
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
		Arrays.fill( l.map, Terrain.EMPTY_SP );
		for (int i = 0; i < W; i++){
			l.map[i] = l.map[i + (W - 1) * W] = l.map[i * W] = l.map[W - 1 + i * W] = Terrain.WALL;
		}
		l.buildFlagMaps();
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
		return l;
	}

	//the cloud's turns until it is gone (200 at most); its strikes
	private static int live( StormCloud cloud ){
		int n = 0;
		for (int t = 0; t < 200 && cloud.volume > 0; t++){
			cloud.act();
			n += drain();
		}
		return n;
	}

	//strikes of a potion's cloud (1000 on one cell), each of `runs` runs
	private static int[] potions( int runs ){
		int[] strikes = new int[runs];
		for (int r = 0; r < runs; r++){
			Random.pushGenerator( 1000 + r );
			try {
				Actor.clear();
				Level l = floor();
				strikes[r] = live( Blob.seed( CENTER, 1000, StormCloud.class, l ) );
			} finally {
				Random.popGenerator();
			}
		}
		return strikes;
	}

	@Test
	public void aCloudAlwaysStrikesAndMostlyTwice(){
		int runs = 400, once = 0, twice = 0;
		for (int s : potions( runs )){
			assertTrue( "a cloud that never struck", s >= 1 );
			if (s == 1) once++;
			else twice++;
		}
		//3 in 4 owe two, and the cloud's own strikes add a few more
		assertTrue( "twice or more: " + twice + " of " + runs, twice > runs * 0.7f && twice < runs * 0.95f );
		assertTrue( "only once: " + once, once > runs * 0.05f );
	}

	@Test
	public void aPatchLaidAtOnceIsOneCloudAndOneApartIsAnother(){
		int[] alone = new int[300], two = new int[300];
		for (int r = 0; r < 300; r++){
			Random.pushGenerator( 5000 + r );
			try {
				//the weather's patch: a dozen seeds round one cell, the same moment
				Actor.clear();
				Level l = floor();
				StormCloud cloud = null;
				for (int c : PathFinder.NEIGHBOURS9) cloud = Blob.seed( CENTER + c, 70, StormCloud.class, l );
				for (int c : PathFinder.NEIGHBOURS4) cloud = Blob.seed( CENTER + 2 * c, 35, StormCloud.class, l );
				alone[r] = live( cloud );

				//two potions thrown far apart
				Actor.clear();
				l = floor();
				Blob.seed( CENTER - 12, 1000, StormCloud.class, l );
				two[r] = live( Blob.seed( CENTER + 12, 1000, StormCloud.class, l ) );
			} finally {
				Random.popGenerator();
			}
		}
		int patchTwice = 0;
		for (int s : alone){
			assertTrue( s >= 1 );
			if (s >= 2) patchTwice++;
		}
		assertTrue( "a patch owes what one cloud does, not a dozen clouds' worth: " + patchTwice,
				patchTwice < 300 * 0.95f );
		for (int s : two) assertTrue( "two clouds, each strikes: " + s, s >= 2 );
	}

	@Test
	public void whatACloudOwesIsKeptOverASave(){
		int owedAfterLoad = 0;
		for (int r = 0; r < 200; r++){
			Random.pushGenerator( 9000 + r );
			try {
				Actor.clear();
				Level l = floor();
				StormCloud cloud = Blob.seed( CENTER, 1000, StormCloud.class, l );
				//saved before it ever struck
				Bundle b = new Bundle();
				cloud.storeInBundle( b );
				StormCloud loaded = new StormCloud();
				loaded.restoreFromBundle( b );
				l.blobs.put( StormCloud.class, loaded );
				int n = live( loaded );
				assertTrue( "a loaded cloud still strikes", n >= 1 );
				owedAfterLoad += n;
			} finally {
				Random.popGenerator();
			}
		}
		assertTrue( "and mostly twice: " + owedAfterLoad, owedAfterLoad > 200 * 1.7f );
	}

	@Test
	public void theRainQuenchesAFieryOneByItsHealthOnTheWorld(){
		Char fiery = new Char(){
			{
				properties.add( Property.FIERY );
				HT = HP = 60;
			}
			@Override
			protected boolean act(){
				return true;
			}
		};
		int depth = Dungeon.depth;
		try {
			Dungeon.level = new OverworldLevel( 0 );
			Dungeon.depth = 97;
			assertEquals( "a fourteenth of its health on the surface, not 20", 4, StormCloud.quench( fiery ) );
			floor();
			Dungeon.depth = 20;
			assertEquals( "1 + depth/5 on a dungeon floor, as ever", 5, StormCloud.quench( fiery ) );
		} finally {
			Dungeon.depth = depth;
		}
	}

	@Test
	public void aClearedCloudOwesNothing(){
		Random.pushGenerator( 77 );
		try {
			Level l = floor();
			StormCloud cloud = Blob.seed( CENTER, 1000, StormCloud.class, l );
			cloud.fullyClear();
			assertEquals( 0, live( cloud ) );
			for (int t = 0; t < 10; t++) cloud.act();
			assertEquals( 0, drain() );
		} finally {
			Random.popGenerator();
		}
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * The sewers' ancient key (and every other key, coin, book and beacon of its kind) kept the depth
 * and the cell it was used on but not the branch: used on a floor that shares its depth with the
 * dungeon (a barrow's, the town's shop is depth 4 of the town's buildings) it brought the hero
 * back to the dungeon's floor of that depth instead, onto a cell of the other floor's layout,
 * inside a wall. The key keeps the branch now, and a return trip never lands inside a wall.
 */
public class SewersKeyReturnTest {

	private static boolean standIn;

	private long seed;
	private int depth, branch;
	private Level level;
	private InterlevelScene.Mode mode;
	private int returnDepth, returnBranch, returnPos;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
		//an item's action cancels the scene's cell selector first: headless a bare one stands in
		//(as PieceFxTest's)
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		if (sel.get( null ) == null){
			sel.set( null, bare( CellSelector.class ) );
			standIn = true;
		}
	}

	@AfterClass
	public static void unboot() throws Exception {
		if (!standIn) return;
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		sel.set( null, null );
	}

	@SuppressWarnings("unchecked")
	private static <T> T bare( Class<T> c ){
		try {
			Field u = Class.forName( "sun.misc.Unsafe" ).getDeclaredField( "theUnsafe" );
			u.setAccessible( true );
			Object unsafe = u.get( null );
			return (T) unsafe.getClass().getMethod( "allocateInstance", Class.class ).invoke( unsafe, c );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	@Before
	public void setUp(){
		seed = Dungeon.seed; depth = Dungeon.depth; branch = Dungeon.branch; level = Dungeon.level;
		mode = InterlevelScene.mode;
		returnDepth = InterlevelScene.returnDepth;
		returnBranch = InterlevelScene.returnBranch;
		returnPos = InterlevelScene.returnPos;
		GamesInProgress.selectedClass = HeroClass.ROGUE;
		Dungeon.seed = 0x5E3E25L;
		Dungeon.init();
	}

	@After
	public void tearDown(){
		Dungeon.seed = seed; Dungeon.depth = depth; Dungeon.branch = branch; Dungeon.level = level;
		InterlevelScene.mode = mode;
		InterlevelScene.returnDepth = returnDepth;
		InterlevelScene.returnBranch = returnBranch;
		InterlevelScene.returnPos = returnPos;
		Actor.clear();
	}

	private static Level floor( int d, int b ){
		Dungeon.depth = d; Dungeon.branch = b;
		Actor.clear();
		Dungeon.level = Dungeon.newLevel();
		return Dungeon.level;
	}

	//where the hero stands when he turns the key: only its depth and branch matter
	private static Level standingIn( int d, int b ){
		Dungeon.depth = d; Dungeon.branch = b;
		Actor.clear();
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( 32, 32 );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		Dungeon.level = l;
		return l;
	}

	//a cell the hero can stand on and walk to from the floor's entrance
	private static boolean reachable( Level l, int cell ){
		PathFinder.setMapSize( l.width(), l.height() );
		PathFinder.buildDistanceMap( l.entrance(), BArray.or( l.passable, l.avoid, null ) );
		return l.passable[cell] && PathFinder.distance[cell] != Integer.MAX_VALUE;
	}

	@Test
	public void theKeyBringsTheHeroBackToTheFloorHeLeft(){
		//the town's shop: depth 4, on the town buildings' branch
		standingIn( 4, 6 );
		int stood = 300;
		Dungeon.hero.pos = stood;
		SewersKey key = new SewersKey();
		key.execute( Dungeon.hero, SewersKey.AC_PORT );
		assertEquals( "off to the Field", InterlevelScene.Mode.PORTSEWERS, InterlevelScene.mode );

		//the game is saved in the Field, and another trip left its own branch behind
		Bundle b = new Bundle();
		key.storeInBundle( b );
		key = new SewersKey();
		key.restoreFromBundle( b );
		standingIn( 27, 0 );
		InterlevelScene.returnBranch = 0;

		key.execute( Dungeon.hero, SewersKey.AC_PORT );
		assertEquals( InterlevelScene.Mode.RETURN, InterlevelScene.mode );
		assertEquals( 4, InterlevelScene.returnDepth );
		assertEquals( "back to the town's shop, not to the sewers' depth 4", 6, InterlevelScene.returnBranch );
		assertEquals( stood, InterlevelScene.returnPos );
	}

	@Test
	public void aKeyFromAnOlderSaveReturnsToTheDungeon(){
		Bundle b = new Bundle();
		new SewersKey().storeInBundle( b );
		b.put( "depth", 4 );
		b.put( "pos", 123 );
		SewersKey key = new SewersKey();
		key.restoreFromBundle( b );

		standingIn( 27, 0 );
		InterlevelScene.returnBranch = 6;
		key.execute( Dungeon.hero, SewersKey.AC_PORT );
		assertEquals( "no branch kept: the dungeon's own floor, as before", 0, InterlevelScene.returnBranch );
	}

	@Test
	public void aReturnIntoAWallLandsOnTheNearestFloor(){
		Level l = floor( 4, 0 );
		//a wall cell beside a floor the hero can walk to
		int wall = -1;
		for (int i = l.width(); i < l.length() - l.width() && wall == -1; i++){
			if (l.map[i] != Terrain.WALL || !l.insideMap( i )) continue;
			for (int n : PathFinder.NEIGHBOURS4){
				if (l.passable[i + n] && !l.avoid[i + n] && l.findMob( i + n ) == null && reachable( l, i + n )){
					wall = i;
					break;
				}
			}
		}
		assertTrue( wall != -1 );

		int landed = l.returnLanding( wall );
		assertNotEquals( "not inside the wall", wall, landed );
		assertTrue( "on a floor he can walk from", reachable( l, landed ) );
		assertEquals( "the nearest one", 1f, l.trueDistance( wall, landed ), 0.001f );

		//deep in the rock, as a cell of a bigger floor's layout can be: still somewhere to stand
		int corner = 0;
		int far = l.returnLanding( corner );
		assertTrue( far >= 0 && reachable( l, far ) );
	}

	@Test
	public void aReturnWhereHeCanBeIsKept(){
		Level l = floor( 4, 0 );
		int ground = l.entrance();
		assertEquals( "a floor: his own cell", ground, l.returnLanding( ground ) );
		assertEquals( "-1, the entrance: switchLevel's own", -1, l.returnLanding( -1 ) );
		assertEquals( "-2, the exit: switchLevel's own", -2, l.returnLanding( -2 ) );

		int pit = -1;
		for (int i = 0; i < l.length() && pit == -1; i++){
			if (l.map[i] == Terrain.EMPTY && l.insideMap( i ) && i != ground) pit = i;
		}
		Level.set( pit, Terrain.CHASM, l );
		assertEquals( "a chasm he comes back to: he falls, as a beacon's trip always let him", pit, l.returnLanding( pit ) );
	}
}

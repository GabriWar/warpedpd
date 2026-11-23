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

package xyz.gabriwar.warpedpixeldungeon.actors.hero;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowSlideTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

/**
 * A boss's fight locks its own door (Tengu's cell, the Dwarf King's hall) and counts on the floor's one
 * iron key being spent getting in. A branch's key of the same depth fits it too, so a hero holding a
 * spare opened Tengu's cell mid-fight and walked out; Tengu's next jump laid his traps around a hero
 * outside the cell and the game crashed (PrisonBossLevel.placeTrapsInTenguCell). No key turns a lock
 * on a sealed floor, as the skeleton key already refused to.
 */
public class SealedFloorLockTest {

	private static final int W = 8;
	private static final int DOOR = 3 + 2 * W;
	private static final int HERO_AT = 3 + 3 * W;

	private int depth, branch;
	private Level savedLevel;
	private Hero savedHero;
	private Object savedSelector;

	@BeforeClass
	public static void boot() throws Exception {
		WindowSlideTest.boot();
	}

	@AfterClass
	public static void unboot(){
		WindowSlideTest.unboot();
	}

	@Before
	public void setUp() throws Exception {
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Notes.reset();
		Dungeon.depth = 10;
		Dungeon.branch = 0;
		floor();
		Hero hero = new Hero();
		hero.pos = HERO_AT;
		hero.sprite = new CharSprite(){
			@Override public void place( int cell ){}
			@Override public void turnTo( int from, int to ){}
			@Override public void showStatus( int color, String text, Object... args ){}
		};
		hero.sprite.ch = hero;
		Dungeon.hero = hero;
		Actor.add( hero );
		Notes.add( new IronKey( 10 ) );
		//a hero turned back is made ready, which refreshes the attack button and the cell selector:
		//headless there is no scene, so a live button and a bare selector stand in (as in ArmorTraitTest)
		new AttackIndicator();
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		savedSelector = sel.get( null );
		if (savedSelector == null) sel.set( null, bare( CellSelector.class ) );
	}

	@After
	public void tearDown() throws Exception {
		Field indicator = AttackIndicator.class.getDeclaredField( "instance" );
		indicator.setAccessible( true );
		indicator.set( null, null );
		Field sel = GameScene.class.getDeclaredField( "cellSelector" );
		sel.setAccessible( true );
		sel.set( null, savedSelector );
		Actor.clear();
		Notes.reset();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	@Test
	public void aKeyDoesNotOpenALockOnASealedFloor() throws Exception {
		Dungeon.level.locked = true;
		unlock();
		assertEquals( "the boss's door stays shut", Terrain.LOCKED_DOOR, Dungeon.level.map[DOOR] );
		assertEquals( "the key is kept", 1, Notes.keyCount( new IronKey( 10 ) ) );
	}

	@Test
	public void aKeyStillOpensALockOnAnOpenFloor() throws Exception {
		unlock();
		assertEquals( Terrain.DOOR, Dungeon.level.map[DOOR] );
		assertEquals( 0, Notes.keyCount( new IronKey( 10 ) ) );
	}

	//the hero's unlock as the game runs it: his turn's unlock action (Hero.act hands it to actUnlock),
	//then the end of the turning-the-key animation
	private static void unlock() throws Exception {
		Hero hero = Dungeon.hero;
		HeroAction.Unlock action = new HeroAction.Unlock( DOOR );
		hero.curAction = action;
		Method act = Hero.class.getDeclaredMethod( "actUnlock", HeroAction.Unlock.class );
		act.setAccessible( true );
		act.invoke( hero, action );
		hero.onOperateComplete();
	}

	@SuppressWarnings("unchecked")
	private static <T> T bare( Class<T> c ) throws ReflectiveOperationException {
		Field u = Class.forName( "sun.misc.Unsafe" ).getDeclaredField( "theUnsafe" );
		u.setAccessible( true );
		Object unsafe = u.get( null );
		return (T) unsafe.getClass().getMethod( "allocateInstance", Class.class ).invoke( unsafe, c );
	}

	//floor walled round, a locked door in the wall above the hero
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
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int x = 0; x < W; x++) l.map[x] = l.map[x + (W - 1) * W] = l.map[x + 2 * W] = Terrain.WALL;
		for (int y = 0; y < W; y++) l.map[y * W] = l.map[W - 1 + y * W] = Terrain.WALL;
		l.map[DOOR] = Terrain.LOCKED_DOOR;
		l.buildFlagMaps();
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
	}
}

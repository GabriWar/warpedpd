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

package xyz.gabriwar.warpedpixeldungeon.items.wands;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * A monster thrown twice before either throw landed (two pushes under way at once) crashed the game
 * when the first threw it over a chasm: it lived through the fall and went down to the floor below,
 * taking its sprite with it, and the second throw, finding it moved, put back a sprite it no longer
 * had. A throw whose creature has left the floor ends there.
 */
public class ThrownTwiceIntoChasmTest {

	private static final int W = 8;
	private static final int FROM = 3 + 3 * W;
	private static final int CHASM = FROM + 1;

	private int depth, branch;
	private Level savedLevel;
	private Hero savedHero;
	private SparseArray<ArrayList<xyz.gabriwar.warpedpixeldungeon.levels.features.FallenMob>> savedFallen;
	private Level level;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedFallen = Dungeon.fallenMobs;
		Actor.clear();
		Dungeon.depth = 5;
		Dungeon.branch = 0;
		Dungeon.fallenMobs = new SparseArray<>();
		level = floor();
		Hero hero = new Hero();
		hero.pos = 1 + W;
		Dungeon.hero = hero;
		Actor.add( hero );
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Dungeon.fallenMobs = savedFallen;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	@Test
	public void aMonsterThrownIntoAChasmWhileThrownAgainIsGone() throws Exception {
		Rat rat = new Rat();
		rat.pos = FROM;
		rat.sprite = stub();
		level.mobs.add( rat );
		Actor.add( rat );

		//east over the chasm, and north, before either has landed
		WandOfBlastWave.throwChar( rat, new Ballistica( FROM, CHASM, Ballistica.MAGIC_BOLT ), 1, false, false, null );
		WandOfBlastWave.throwChar( rat, new Ballistica( FROM, FROM - W, Ballistica.MAGIC_BOLT ), 1, false, false, null );

		Pushing overChasm = null, north = null;
		for (Actor a : Actor.all()){
			if (a instanceof Pushing){
				if (to( (Pushing) a ) == CHASM) overChasm = (Pushing) a;
				else north = (Pushing) a;
			}
		}

		//each lands as its effect ends, the one over the chasm first
		landing( overChasm ).call();
		assertNull( "it fell through, sprite and all", rat.sprite );
		landing( north ).call();

		assertFalse( level.mobs.contains( rat ) );
		assertEquals( CHASM, rat.pos );
	}

	private static int to( Pushing p ) throws Exception {
		Field f = Pushing.class.getDeclaredField( "to" );
		f.setAccessible( true );
		return (int) f.get( p );
	}

	private static Callback landing( Pushing p ) throws Exception {
		Field f = Pushing.class.getDeclaredField( "callback" );
		f.setAccessible( true );
		return (Callback) f.get( p );
	}

	//a sprite that draws nothing
	private static MobSprite stub(){
		MobSprite sp = new MobSprite(){
			@Override public void place( int cell ){ }
			@Override public void fall(){ }
			@Override public void update(){ }
		};
		sp.visible = false;
		return sp;
	}

	//an open floor walled round, a chasm cell in it, all of it in the hero's sight
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
		l.transitions = new ArrayList<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int i = 0; i < W; i++){
			l.map[i] = l.map[i + (W - 1) * W] = l.map[i * W] = l.map[W - 1 + i * W] = Terrain.WALL;
		}
		l.map[CHASM] = Terrain.CHASM;
		l.buildFlagMaps();
		Arrays.fill( l.heroFOV, true );
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
		return l;
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

/**
 * What burns, drips or smokes on a wall goes when the wall does (a pick, a bomb): the prison's
 * torches, the sewers' running pipes, the city's smoking vents and green flames, the frozen
 * halls' melt. Before, they kept burning and dripping over the rubble.
 */
public class WallVisualsTest {

	private static final int W = 12, WALL = 5 + 3 * W;

	private GL20 savedGl, savedGl20;
	private float savedElapsed;
	private Level savedLevel;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		savedElapsed = Game.elapsed;
		savedLevel = Dungeon.level;
		//textures without a GPU
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			return null;
		} );
		Game.elapsed = 0.05f;
	}

	@After
	public void tearDown(){
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
	}

	//a walled room, one wall cell of it `deco`, all of it seen
	private static Level room( int deco ){
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
		for (int x = 1; x < W - 1; x++) l.map[x + 3 * W] = Terrain.WALL;
		l.map[WALL] = deco;
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		Dungeon.level = l;
		return l;
	}

	private interface Visuals {
		void add( Level level, Group group );
	}

	private static int living( Group g ){
		int n = 0;
		for (Gizmo m : g.membersView()) if (m != null && m.alive) n++;
		return n;
	}

	//the visual on the wall burns while the wall stands and goes with it, the rest of the room's
	//visuals left as they are
	private static void goesWithItsWall( String what, int deco, Visuals visuals ){
		Level l = room( deco );
		Group g = new Group();
		visuals.add( l, g );
		int before = living( g );
		assertEquals( what + ": one on the wall", true, before >= 1 );
		for (int f = 0; f < 5; f++) g.update();
		assertEquals( what + ": burning while the wall stands", before, living( g ) );
		Level.set( WALL, Terrain.EMPTY, l );
		g.update();
		assertEquals( what + ": gone with the wall", before - 1, living( g ) );
	}

	@Test
	public void whatIsOnAWallGoesWhenItBreaks(){
		goesWithItsWall( "the prison's torch", Terrain.WALL_DECO, PrisonLevel::addPrisonVisuals );
		goesWithItsWall( "the sewers' pipe", Terrain.WALL_DECO, SewerLevel::addSewerVisuals );
		goesWithItsWall( "the city's vent", Terrain.WALL_DECO, CityLevel::addCityVisuals );
		goesWithItsWall( "the city's green flame", Terrain.REGION_DECO, CityLevel::addCityWallVisuals );
		goesWithItsWall( "the frozen wall's melt", Terrain.WALL_DECO, FrozenLevel::addFrozenVisuals );
	}

	@Test
	public void aTorchByTheSewerBossExitBurnsOnItsPlainWallTillItBreaks(){
		Level l = room( Terrain.WALL );
		Group g = new Group();
		g.add( new PrisonLevel.Torch( WALL ) );
		g.update();
		assertEquals( "a plain wall is still its wall", 1, living( g ) );
		Level.set( WALL, Terrain.EMPTY, l );
		g.update();
		assertEquals( 0, living( g ) );
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BatSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The effects' areas and the kit's hooks into the game: eight areas with keys of their own, each
 * with a gallery page of at least six exhibits, the gallery's scenes among the debug scenes with
 * no id twice; a sprite stepping out of water still leaves today's ring (moved into the water's
 * area, unchanged), a jumper landing in it too, a flier none; the terrain watch tells each change
 * once and a whole new map none; and a sprite's orbits come and go.
 */
public class FxModulesTest {

	private static final int W = 12;

	private GL20 savedGl, savedGl20;
	private Camera savedCam;
	private Level savedLevel;
	private float savedElapsed;
	private Group[] layers;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		savedCam = Camera.main;
		savedLevel = Dungeon.level;
		savedElapsed = Game.elapsed;
		//a GPU that answers yes: shaders compile and link, so the orbits can be drawn
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			String n = m.getName();
			if ((n.equals( "glGetShaderiv" ) || n.equals( "glGetProgramiv" )) && a[2] instanceof IntBuffer){
				((IntBuffer) a[2]).put( 0, 1 );
				return null;
			}
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			if (r == String.class) return "";
			return null;
		} );
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		Camera.main.fullScreen = true;
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		FxRing.reset();
		WaterFX.sceneCreated();
		Dungeon.level = pond();
		Game.elapsed = 1 / 30f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		FxRing.reset();
		WaterFX.sceneCreated();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Camera.main = savedCam;
		Dungeon.level = savedLevel;
		Game.elapsed = savedElapsed;
	}

	//water in columns 5-8 of rows 3-8, all of it seen
	private static Level pond(){
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
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int y = 3; y <= 8; y++) for (int x = 5; x <= 8; x++) l.map[x + y * W] = Terrain.WATER;
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		l.visited = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		return l;
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	private int ripples(){
		int n = 0;
		for (Gizmo g : layers[GameScene.Layer.SURFACE.ordinal()].membersView()) if (g instanceof Ripple && g.alive) n++;
		return n;
	}

	//a sprite on a stage group, linked by hand to its character (no camera-dependent link)
	private static CharSprite sprite( CharSprite s, Mob ch, int cell, Group stage ){
		//standing still, unaware of nothing: no wandering mark over it (it needs the game's scene)
		ch.state = ch.PASSIVE;
		ch.pos = cell;
		s.ch = ch;
		ch.sprite = s;
		stage.add( s );
		s.visible = true;
		return s;
	}

	@Test
	public void eightAreasEachWithAPageOfTheirOwn(){
		assertEquals( FxModules.COUNT, FxModules.ALL.length );
		HashSet<String> keys = new HashSet<>();
		for (FxModule m : FxModules.ALL){
			assertTrue( "a key twice: " + m.key(), keys.add( m.key() ) );
			assertFalse( m.title().isEmpty() );
			FxGallery.Page page = FxGallery.page( m );
			assertTrue( m.key() + " has " + page.size() + " exhibits", page.size() >= 6 );
			for (int i = 0; i < page.size(); i++){
				assertFalse( page.name( i ).isEmpty() );
				assertTrue( page.period( i ) > 0 );
			}
			assertSame( m, FxModules.byKey( m.key() ) );
		}
		assertNull( FxModules.byKey( "no-such-area" ) );
		assertEquals( "water-life", FxModules.ALL[FxModules.WATER_LIFE].key() );
		assertEquals( "skills", FxModules.ALL[FxModules.SKILLS].key() );
	}

	@Test
	public void theGallerysScenesAreAmongTheDebugScenesNoIdTwice(){
		HashSet<String> ids = new HashSet<>();
		for (DebugScenes.Scene s : DebugScenes.SCENES) assertTrue( "twice: " + s.id(), ids.add( s.id() ) );
		assertTrue( ids.contains( "fx-gallery" ) );
		assertTrue( ids.contains( "fx-busy" ) );
		for (FxModule m : FxModules.ALL) assertTrue( ids.contains( "fx-gallery-" + m.key() ) );
		for (DebugScenes.Scene s : FxGallery.SCENES) assertSame( s, DebugScenes.byId( s.id() ) );
		//right after the lightning-water scene, the old ones all still there
		int lw = -1, fx = -1;
		for (int i = 0; i < DebugScenes.SCENES.length; i++){
			if (DebugScenes.SCENES[i].id().equals( "lightning-water" )) lw = i;
			if (DebugScenes.SCENES[i].id().equals( "fx-gallery" )) fx = i;
		}
		assertEquals( lw + 1, fx );
		assertNotNull( DebugScenes.byId( "weather-clear" ) );
		assertNotNull( DebugScenes.byId( "blank-room" ) );
	}

	@Test
	public void aWaderSteppingOutOfTheWaterStillLeavesItsRing(){
		Group stage = new Group();
		Rat rat = new Rat();
		CharSprite s = sprite( new RatSprite(), rat, at( 6, 5 ), stage );
		//into the water: no ring, as ever
		s.move( at( 4, 5 ), at( 5, 5 ) );
		assertEquals( 0, ripples() );
		//through it and out of it: a ring on the cell left
		WaterFX.frame();
		s.move( at( 5, 5 ), at( 6, 5 ) );
		assertEquals( "a middling ring and its second", 2, ripples() );
		WaterFX.frame();
		s.move( at( 8, 5 ), at( 9, 5 ) );
		assertEquals( 4, ripples() );
		//unseen: nothing
		s.visible = false;
		WaterFX.frame();
		s.move( at( 7, 5 ), at( 8, 5 ) );
		assertEquals( 4, ripples() );

		//a flier makes none
		Bat bat = new Bat();
		CharSprite b = sprite( new BatSprite(), bat, at( 6, 6 ), stage );
		WaterFX.frame();
		b.move( at( 6, 6 ), at( 7, 6 ) );
		assertEquals( 4, ripples() );
		//and the step is written down for what reacts to steps (a gas walked through)
		assertTrue( Steps.last() > 0 );
	}

	@Test
	public void aJumperLandingInTheWaterRingsItAsEver(){
		Group stage = new Group();
		Rat rat = new Rat();
		CharSprite s = sprite( new RatSprite(), rat, at( 2, 5 ), stage );
		rat.pos = at( 6, 4 );
		s.jump( at( 2, 5 ), at( 6, 4 ), null );
		for (int i = 0; i < 30; i++) stage.update();
		assertTrue( ripples() >= 1 );
	}

	@Test
	public void theTerrainWatchTellsEachChangeOnceAndANewMapNone(){
		Level l = Dungeon.level;
		TerrainWatch.resync();
		assertEquals( 0, TerrainWatch.drain() );
		Level.set( at( 2, 2 ), Terrain.GRASS );
		TerrainWatch.changed( at( 2, 2 ) );
		TerrainWatch.changed( at( 2, 2 ) );
		Level.set( at( 6, 4 ), Terrain.EMPTY );
		TerrainWatch.changed( at( 6, 4 ) );
		//said changed but unchanged: nothing
		TerrainWatch.changed( at( 3, 3 ) );
		assertEquals( 2, TerrainWatch.drain() );
		assertEquals( Terrain.GRASS, TerrainWatch.seen( at( 2, 2 ) ) );
		assertEquals( 0, TerrainWatch.drain() );
		//a whole map taken in: nothing told
		Level.set( at( 1, 1 ), Terrain.GRASS );
		TerrainWatch.changed( at( 1, 1 ) );
		TerrainWatch.resync();
		assertEquals( 0, TerrainWatch.drain() );
		assertEquals( Terrain.GRASS, TerrainWatch.seen( at( 1, 1 ) ) );
		//a new level is taken in as it is
		Dungeon.level = pond();
		TerrainWatch.changed( at( 2, 2 ) );
		assertEquals( 0, TerrainWatch.drain() );
		assertEquals( Terrain.EMPTY, TerrainWatch.seen( at( 2, 2 ) ) );
		//from any thread, never more than its queue
		for (int i = 0; i < 1000; i++) TerrainWatch.changed( i % l.length() );
		TerrainWatch.drain();
		assertEquals( -1, TerrainWatch.seen( -5 ) );
	}

	@Test
	public void aSpritesOrbitsComeAndGo(){
		Group stage = new Group();
		CharSprite s = sprite( new RatSprite(), new Rat(), at( 2, 2 ), stage );
		assertEquals( 0, FxOrbit.count( s ) );
		FxOrbit.Ring stance = FxOrbit.attach( s, FxOrbit.Slot.STANCE ).motes( 4, FxFrames.MOTE_2, 0xFFD060, true )
				.trail( 1 ).breath( 0.7f, 1f, 0.5f );
		assertSame( stance, FxOrbit.attach( s, FxOrbit.Slot.STANCE ) );
		assertSame( stance, FxOrbit.get( s, FxOrbit.Slot.STANCE ) );
		assertEquals( 1, FxOrbit.count( s ) );
		for (FxOrbit.Slot slot : new FxOrbit.Slot[]{ FxOrbit.Slot.COINS, FxOrbit.Slot.ELEMENTAL, FxOrbit.Slot.MARK }){
			FxOrbit.attach( s, slot ).motes( 3, FxFrames.MOTE_1, 0xFFFFFF, true );
		}
		assertEquals( 4, FxOrbit.count( s ) );
		//drawn about the sprite, its far half before it and its near half after, frame after frame
		for (int f = 0; f < 10; f++){
			FxModules.beforeDraw( s );
			FxModules.afterDraw( s );
		}
		stance.flare( 1f, 0.5f, 2 );
		FxModules.beforeDraw( s );
		stance.detach();
		assertTrue( stance.isDetached() );
		assertNull( FxOrbit.get( s, FxOrbit.Slot.STANCE ) );
		assertEquals( 3, FxOrbit.count( s ) );
		FxModules.beforeDraw( s );
		FxModules.afterDraw( s );
		//a near half with no far half drawn first (the sprite unseen) draws nothing, and throws nothing
		FxModules.afterDraw( s );
		//killed: every orbit let go of
		FxOrbit.Ring coins = FxOrbit.get( s, FxOrbit.Slot.COINS );
		s.kill();
		assertEquals( 0, FxOrbit.count( s ) );
		assertTrue( coins.isDetached() );
		//a detached orbit takes no more motes
		coins.motes( 3, FxFrames.MOTE_1, 0xFFFFFF, true );
		assertEquals( 0, FxOrbit.count( s ) );
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.water;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The live surface headless (stand-in layers and a stand-in water plane): the palette cycle steps
 * through its eight frames at its liquid's rate, stops with time (the hourglass), leaves a water it
 * was not painted for alone, and nothing touches the GPU while it runs.
 */
public class WaterLiveTest {

	static final int W = 24, H = 16;

	private GL20 savedGl, savedGl20;
	private float savedElapsed, savedScroll;
	private Level savedLevel;
	private Hero savedHero;
	private boolean savedFreeze;

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
		savedHero = Dungeon.hero;
		savedScroll = GameScene.waterScroll;
		savedFreeze = Emitter.freezeEmitters;
		Emitter.freezeEmitters = false;
		Group[] layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		WaterFX.sceneCreated();
		WaterLive.debugLiquid = null;
		fullTier();
		Game.elapsed = 1 / 30f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		WaterLive.planeForTests = null;
		WaterLive.debugLiquid = null;
		WaterFX.sceneCreated();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		GameScene.waterScroll = savedScroll;
		Emitter.freezeEmitters = savedFreeze;
	}

	/** A floor showing `tex` for its water: a pool in columns 6-15, rows 4-9; all of it seen. */
	static Level pond( final String tex ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public String waterTex(){ return tex; }
		};
		l.setSize( W, H );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int y = 4; y < 10; y++) for (int x = 6; x < 16; x++) l.map[x + y * W] = Terrain.WATER;
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		l.visited = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		Arrays.fill( l.visited, true );
		return l;
	}

	/** The budget's governor back at FULL (another test may have left it struggling): six seconds of
	 *  fast frames, three a tier, and nothing of the kit's alive. */
	static void fullTier(){
		for (int i = 0; i < 650; i++) xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget.frame( 0.01f );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget.reset();
	}

	static void failOnGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, a) -> { throw new AssertionError( "GL call: " + m.getName() ); } );
	}

	private SkinnedBlock plane( String tex ){
		Dungeon.level = pond( tex );
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 3 + 3 * W;
		SkinnedBlock p = new SkinnedBlock( W * 16, H * 16, tex );
		WaterLive.planeForTests = p;
		return p;
	}

	private static void run( WaterLive live, float seconds ){
		for (float t = 0; t < seconds; t += Game.elapsed) live.update();
	}

	@Test
	public void theCycleStepsThroughItsEightFramesAtItsLiquidsRate(){
		failOnGl();
		SkinnedBlock p = plane( Assets.Environment.WATER_CAVES );
		SmartTexture own = p.texture;
		WaterLive live = new WaterLive();
		live.update();
		String strip = Assets.Environment.WATER_CAVES_CYCLE;
		assertSame( "frame 0 at once", TextureCache.get( strip + "#0" ), p.texture );
		assertNotSame( own, p.texture );
		//the caves' water cycles at four frames a second: eight different frames in two seconds
		HashSet<SmartTexture> seen = new HashSet<>();
		Game.elapsed = 0.05f;
		for (int i = 0; i < 40; i++){
			live.update();
			seen.add( p.texture );
			assertEquals( "a frame of the strip", 32, p.texture.width );
		}
		assertEquals( 8, seen.size() );
		assertEquals( Element.Liquid.CAVE.scroll, GameScene.waterScroll, 0 );
	}

	@Test
	public void theCycleStandsStillWithTime(){
		SkinnedBlock p = plane( Assets.Environment.WATER_SEWERS );
		WaterLive live = new WaterLive();
		run( live, 0.4f );
		SmartTexture held = p.texture;
		Emitter.freezeEmitters = true;
		run( live, 2f );
		assertSame( held, p.texture );
		Emitter.freezeEmitters = false;
		run( live, 0.5f );
		assertNotSame( held, p.texture );
	}

	@Test
	public void waterItWasNotPaintedForIsLeftAlone(){
		//the ice keeps still, and so does a water the liquids do not know (read as the sewers')
		for (String tex : new String[]{ Assets.Environment.WATER_FROZEN, "environment/water_unknown.png" }){
			SkinnedBlock p = new SkinnedBlock( 32, 32, Assets.Environment.WATER_SEWERS );
			Dungeon.level = pond( tex );
			WaterLive.planeForTests = p;
			SmartTexture own = p.texture;
			WaterLive live = new WaterLive();
			run( live, 1f );
			assertSame( tex, own, p.texture );
		}
	}

	private static java.util.List<WaterLive.Plane> planes( WaterLive live, boolean visibleOnly ){
		java.util.ArrayList<WaterLive.Plane> out = new java.util.ArrayList<>();
		for (com.watabou.noosa.Gizmo g : live.membersView()){
			if (g instanceof WaterLive.Plane && (!visibleOnly || g.visible)) out.add( (WaterLive.Plane) g );
		}
		return out;
	}

	@Test
	public void theSheensAreCutToTheWaterAndStayPutInTheWorld(){
		failOnGl();
		plane( Assets.Environment.WATER_SEWERS );
		WaterLive live = new WaterLive();
		live.update();
		java.util.List<WaterLive.Plane> shown = planes( live, true );
		assertEquals( "sheen A and sheen B", 2, shown.size() );
		for (WaterLive.Plane p : shown){
			//the pool is columns 6-15, rows 4-9
			assertEquals( 6 * 16, p.x, 0 );
			assertEquals( 4 * 16, p.y, 0 );
			assertEquals( 10 * 16, p.width, 0 );
			assertEquals( 6 * 16, p.height, 0 );
			assertTrue( "added, never past 0.30: " + p.am, p.additive && p.am > 0 && p.am <= 0.30f + 1e-4f );
		}
		//anchored: the texel at a world point does not depend on where the box begins
		WaterLive.Plane a = shown.get( 0 );
		float texelAt200 = (a.offsetX() + (200 - a.x)) % 64;
		Dungeon.level.map[2 + 4 * W] = Terrain.WATER;
		Dungeon.level.buildFlagMaps();
		Game.elapsed = 0f;
		live.update();
		assertEquals( 2 * 16, a.x, 0 );
		assertEquals( texelAt200, (a.offsetX() + (200 - a.x)) % 64, 1e-3f );
		//no water in view: hidden
		Arrays.fill( Dungeon.level.map, Terrain.EMPTY );
		Dungeon.level.buildFlagMaps();
		live.update();
		assertEquals( 0, planes( live, true ).size() );
	}

	@Test
	public void theLavaHasNoSheenAndTheCavesADimmerOne(){
		plane( Assets.Environment.WATER_HALLS );
		WaterLive live = new WaterLive();
		live.update();
		//only its glow, within its swell
		assertEquals( 1, planes( live, true ).size() );
		WaterLive.Plane glow = planes( live, true ).get( 0 );
		assertEquals( 64, glow.texture.width );
		assertTrue( glow.am >= WaterLive.GLOW_MIN - 0.001f && glow.am <= WaterLive.GLOW_MAX + 0.001f );
		plane( Assets.Environment.WATER_CAVES );
		live = new WaterLive();
		live.update();
		//brighter than its liquid's own 0.12, still dimmer than the sewers' 0.22
		assertTrue( WaterLive.CAVE_SHEEN_A < Element.Liquid.SEWER.sheenA );
		assertEquals( 2, planes( live, true ).size() );
		for (WaterLive.Plane p : planes( live, true )){
			assertTrue( p.am <= WaterLive.CAVE_SHEEN_A * 1.13f );
			assertTrue( "it shows underground: " + p.am, p.am >= WaterLive.CAVE_SHEEN_B * WaterGeom.UNDERGROUND * 0.87f );
		}
	}

	@Test
	public void aStrugglingPhoneDropsTheSheensOneByOne(){
		plane( Assets.Environment.WATER_SEWERS );
		WaterLive live = new WaterLive();
		live.update();
		assertEquals( 2, planes( live, true ).size() );
		try {
			//a second and more of slow frames: REDUCED, sheen B goes
			for (int i = 0; i < 40; i++) xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget.frame( 0.05f );
			live.update();
			assertEquals( 1, planes( live, true ).size() );
			for (int i = 0; i < 40; i++) xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget.frame( 0.05f );
			live.update();
			assertEquals( 0, planes( live, true ).size() );
		} finally {
			//and back up to FULL: three seconds of fast frames a tier
			for (int i = 0; i < 1000; i++) xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget.frame( 0.01f );
		}
	}

	@Test
	public void theLavaCrawlsAndTheCarouselShowsAnyLiquid(){
		SkinnedBlock p = plane( Assets.Environment.WATER_HALLS );
		WaterLive live = new WaterLive();
		live.update();
		assertEquals( 2f, GameScene.waterScroll, 0 );
		assertSame( TextureCache.get( Assets.Environment.WATER_HALLS_CYCLE + "#0" ), p.texture );
		//the gallery's carousel: the temple's water on the halls' floor, then the ice, then back
		WaterLive.debugLiquid = Element.Liquid.TEMPLE;
		live.update();
		assertSame( TextureCache.get( Assets.Environment.WATER_TEMPLE_CYCLE + "#0" ), p.texture );
		assertEquals( 5f, GameScene.waterScroll, 0 );
		WaterLive.debugLiquid = Element.Liquid.FROZEN;
		live.update();
		assertSame( TextureCache.get( Assets.Environment.WATER_FROZEN ), p.texture );
		WaterLive.debugLiquid = null;
		live.update();
		assertTrue( p.texture == TextureCache.get( Assets.Environment.WATER_HALLS_CYCLE + "#0" )
				|| p.texture == TextureCache.get( Assets.Environment.WATER_HALLS_CYCLE + "#1" ) );
	}
}

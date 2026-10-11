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
import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.particles.Emitter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The regions' own water headless: the lava's glow swells within its bounds, its bubbles, crust,
 * heat rays and spits keep to their caps, its banks light the six nearest the hero; the city's oil
 * holds its rings longer; the cold water's slush keeps its cap and its mist comes only at night;
 * water coming and going rings and foams, or throws up droplets and mist - never for ice, never
 * out of sight; and none of it touches the GPU while it runs.
 */
public class WaterRegionsTest {

	static final int W = WaterLiveTest.W, H = WaterLiveTest.H;

	private GL20 savedGl, savedGl20;
	private float savedElapsed, savedTint;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp() throws Exception {
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		savedElapsed = Game.elapsed;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedTint = GameScene.nightTintAlpha();
		Emitter.freezeEmitters = false;
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		WaterFX.sceneCreated();
		WaterLive.sceneCreated();
		WaterLiveTest.fullTier();
		FxRing.reset();
		FxLight.reset();
		FxRandom.seedForTests( 5 );
		Game.elapsed = 1 / 30f;
	}

	@After
	public void tearDown() throws Exception {
		GameScene.useLayersForTests( null );
		WaterLive.planeForTests = null;
		WaterFX.sceneCreated();
		WaterLive.sceneCreated();
		FxBudget.reset();
		FxRing.reset();
		FxLight.reset();
		tint( savedTint );
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//the night's tint as the scene would have it
	static void tint( float a ) throws Exception {
		Field f = GameScene.class.getDeclaredField( "curTintA" );
		f.setAccessible( true );
		f.setFloat( null, a );
	}

	private WaterLive pond( String tex ){
		Dungeon.level = WaterLiveTest.pond( tex );
		Dungeon.hero = new Hero();
		//on the pool's north bank, mid-way along
		Dungeon.hero.pos = 10 + 3 * W;
		WaterLive.planeForTests = new SkinnedBlock( W * 16, H * 16, tex );
		return new WaterLive();
	}

	//frames of the live surface and of every layer it lays things in
	private void run( WaterLive live, float seconds, Runnable each ){
		for (float t = 0; t < seconds; t += Game.elapsed){
			FxBudget.frame( 1 / 60f );
			WaterFX.frame();
			live.update();
			for (Group g : layers) g.update();
			Game.timeTotal += Game.elapsed;
			if (each != null) each.run();
		}
	}

	private WaterLive.Plane glow( WaterLive live ){
		for (Gizmo g : live.membersView()){
			if (g instanceof WaterLive.Plane && ((WaterLive.Plane) g).additive && g.visible) return (WaterLive.Plane) g;
		}
		return null;
	}

	@Test
	public void theLavaGlowsBubblesCrustsAndShimmersWithinItsCaps(){
		WaterLiveTest.failOnGl();
		WaterLive live = pond( Assets.Environment.WATER_HALLS );
		final int[] most = new int[4];
		final float[] glowAm = { 1f, 0f };
		run( live, 12f, () -> {
			most[0] = Math.max( most[0], Marks.live( Marks.BUBBLE ) );
			most[1] = Math.max( most[1], Marks.live( Marks.CRUST ) );
			most[2] = Math.max( most[2], Marks.live( Marks.HEAT ) );
			WaterLive.Plane g = glow( live );
			if (g != null){
				glowAm[0] = Math.min( glowAm[0], g.am );
				glowAm[1] = Math.max( glowAm[1], g.am );
			}
		} );
		assertTrue( "bubbles: " + most[0], most[0] > 0 && most[0] <= Marks.CAPS[Marks.BUBBLE] );
		assertTrue( "crust: " + most[1], most[1] > 0 && most[1] <= Marks.CAPS[Marks.CRUST] );
		assertTrue( "heat rays: " + most[2], most[2] > 0 && most[2] <= Marks.CAPS[Marks.HEAT] );
		assertTrue( "the glow swells: " + glowAm[0] + ".." + glowAm[1],
				glowAm[0] >= WaterLive.GLOW_MIN - 0.001f && glowAm[1] <= WaterLive.GLOW_MAX + 0.001f
						&& glowAm[1] - glowAm[0] > 0.1f );
		//the lava never lays the water's glints or foam
		assertEquals( 0, Marks.live( Marks.GLINT ) + Marks.live( Marks.FOAM ) );
	}

	@Test
	public void theLavasBanksLightTheSixNearestTheHero(){
		WaterLive live = pond( Assets.Environment.WATER_HALLS );
		run( live, 1f, null );
		assertEquals( WaterLava.EDGES, live.banksLitForTests() );
		assertEquals( WaterLava.EDGES, FxLight.livePersistent() );
		//another liquid shown: all put out
		WaterLive.debugLiquid = xyz.gabriwar.warpedpixeldungeon.effects.fx.Element.Liquid.SEWER;
		run( live, 2f, null );
		WaterLive.debugLiquid = null;
		assertEquals( 0, live.banksLitForTests() );
		assertEquals( 0, FxLight.livePersistent() );
	}

	@Test
	public void noMoreThanTenSpitsAreInTheAirAtOnce(){
		pond( Assets.Environment.WATER_HALLS );
		int made = 0;
		for (int i = 0; i < 30; i++) if (WaterLava.spit( 10 * 16 + 8, 6 * 16 + 8 )) made++;
		assertEquals( WaterLava.SPITS, made );
		//off the lava: none
		Game.timeTotal += 1f;
		assertFalse( WaterLava.spit( 2 * 16 + 8, 2 * 16 + 8 ) );
		assertTrue( WaterLava.spit( 10 * 16 + 8, 6 * 16 + 8 ) );
	}

	@Test
	public void theCitysOilHoldsItsRingsLonger(){
		pond( Assets.Environment.WATER_SEWERS );
		float sewer = WaterFX.lifeOf( WaterFX.M );
		pond( Assets.Environment.WATER_CITY );
		assertEquals( sewer * WaterFX.OIL_LIFE, WaterFX.lifeOf( WaterFX.M ), 0.0001f );
		Ripple r = WaterFX.ring( 10 * 16 + 8, 6 * 16 + 8, WaterFX.M );
		Group surface = layers[GameScene.Layer.SURFACE.ordinal()];
		float t = 0;
		while (r.alive && t < 3f){
			surface.update();
			t += Game.elapsed;
		}
		assertTrue( "lived " + t, t >= WaterFX.LIFE[WaterFX.M] * 1.2f );
	}

	@Test
	public void theColdWatersSlushKeepsItsCapAndItsMistComesOnlyAtNight() throws Exception {
		WaterLive live = pond( Assets.Environment.WATER_FROZEN );
		tint( 0f );
		final int[] most = new int[2];
		run( live, 8f, () -> {
			most[0] = Math.max( most[0], Marks.live( Marks.SLUSH ) );
			most[1] = Math.max( most[1], Marks.live( Marks.MIST ) );
		} );
		assertTrue( "slush: " + most[0], most[0] > 0 && most[0] <= Marks.CAPS[Marks.SLUSH] );
		assertEquals( "no mist by day", 0, most[1] );
		tint( 0.54f );
		run( live, 8f, () -> most[1] = Math.max( most[1], Marks.live( Marks.MIST ) ) );
		assertTrue( "mist at night: " + most[1], most[1] > 0 && most[1] <= Marks.CAPS[Marks.MIST] );
	}

	private int rings( int size ){
		int n = 0;
		for (Gizmo g : layers[GameScene.Layer.SURFACE.ordinal()].membersView()){
			if (g instanceof Ripple && g.alive && ((Ripple) g).sizeMade() == size && !((Ripple) g).isSecond()) n++;
		}
		return n;
	}

	@Test
	public void waterComingRingsAndFoamsAndWaterGoingThrowsUpDroplets(){
		WaterLiveTest.failOnGl();
		pond( Assets.Environment.WATER_SEWERS );
		Level l = Dungeon.level;
		int dry = 3 + 12 * W, wet = 10 + 6 * W;
		l.map[dry] = Terrain.WATER;
		l.buildFlagMaps();
		WaterTerrain.changed( dry, Terrain.EMPTY, Terrain.WATER );
		assertEquals( 1, rings( WaterFX.M ) );
		assertEquals( 4, Marks.live( Marks.FOAM ) );

		l.map[wet] = Terrain.EMPTY;
		l.buildFlagMaps();
		int before = FxBudget.live( FxBudget.DROPLETS );
		WaterTerrain.changed( wet, Terrain.WATER, Terrain.EMPTY );
		assertEquals( before + 3, FxBudget.live( FxBudget.DROPLETS ) );

		//ice is the cold's; and nothing where the hero does not see
		FxRing.reset();
		layers[GameScene.Layer.SURFACE.ordinal()].clear();
		Marks.reset();
		WaterTerrain.changed( 12 + 6 * W, Terrain.WATER, Terrain.FROZEN_WATER );
		WaterTerrain.changed( 12 + 6 * W, Terrain.FROZEN_WATER, Terrain.WATER );
		l.heroFOV[13 + 6 * W] = false;
		WaterTerrain.changed( 13 + 6 * W, Terrain.EMPTY, Terrain.WATER );
		assertEquals( 0, rings( WaterFX.M ) );
		assertEquals( 0, Marks.live( Marks.FOAM ) );
	}
}

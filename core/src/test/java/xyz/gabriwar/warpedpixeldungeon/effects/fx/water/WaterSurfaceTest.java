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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * What the live surface lays on the water headless: glints on the open water and never in heavy
 * rain, lap foam along the banks in the liquid's foam and on the dry sides only, every kind under
 * its cap, a bubble that pops into a ring, and none of it on the GPU while it is made.
 */
public class WaterSurfaceTest {

	static final int W = WaterLiveTest.W;

	private GL20 savedGl, savedGl20;
	private float savedElapsed, savedRain;
	private PrecipType savedType;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;

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
		savedRain = ClimateManager.debugPrecipOverride;
		savedType = ClimateManager.debugPrecipTypeOverride;
		Emitter.freezeEmitters = false;
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx.sceneCreated();
		WaterFX.sceneCreated();
		WaterLiveTest.fullTier();
		FxRing.reset();
		Marks.reset();
		FxRandom.seedForTests( 11 );
		Dungeon.level = WaterLiveTest.pond( Assets.Environment.WATER_SEWERS );
		Dungeon.hero = new Hero();
		//on the pool's bank, mid-way along: everything within reach of the budget's nearest share
		Dungeon.hero.pos = 10 + 3 * W;
		WaterLive.planeForTests = new SkinnedBlock( W * 16, WaterLiveTest.H * 16, Assets.Environment.WATER_SEWERS );
		Game.elapsed = 1 / 30f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		WaterLive.planeForTests = null;
		WaterFX.sceneCreated();
		FxBudget.reset();
		FxRing.reset();
		Marks.reset();
		ClimateManager.debugPrecipOverride = savedRain;
		ClimateManager.debugPrecipTypeOverride = savedType;
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//the frames of a live surface: its own and the surface slot's
	private void run( WaterLive live, float seconds, int[] most ){
		Group surface = layers[GameScene.Layer.SURFACE.ordinal()];
		for (float t = 0; t < seconds; t += Game.elapsed){
			//the phone keeps its 60 frames a second: the governor stays at FULL
			FxBudget.frame( 1 / 60f );
			WaterFX.frame();
			live.update();
			surface.update();
			if (most != null) for (int k = 0; k < most.length; k++) most[k] = Math.max( most[k], Marks.live( k ) );
		}
	}

	private int marks( int[] frame ){
		int n = 0;
		for (Gizmo g : layers[GameScene.Layer.SURFACE.ordinal()].membersView()){
			if (!(g instanceof Group)) continue;
			for (Gizmo m : ((Group) g).membersView()){
				if (m instanceof Marks.Mark && m.alive && ((Marks.Mark) m).width == frame[2] && ((Marks.Mark) m).height == frame[3]) n++;
			}
		}
		return n;
	}

	@Test
	public void glintsTwinkleOnTheOpenWaterAndLapFoamAlongItsBanks(){
		WaterLiveTest.failOnGl();
		WaterLive live = new WaterLive();
		int[] most = new int[Marks.KINDS];
		int foamH = 0, foamV = 0;
		for (int i = 0; i < 300; i++){
			run( live, 1 / 30f, most );
			foamH = Math.max( foamH, marks( FxFrames.FOAM_H[0] ) );
			foamV = Math.max( foamV, marks( FxFrames.FOAM_V[0] ) );
		}
		assertTrue( "glints in ten seconds", most[Marks.GLINT] > 0 );
		assertTrue( "foam along the north and south banks", foamH > 0 );
		assertTrue( "foam along the east and west banks", foamV > 0 );
		for (int k = 0; k < Marks.KINDS; k++) assertTrue( "kind " + k + " at " + most[k], most[k] <= Marks.CAPS[k] );
	}

	@Test
	public void foamLiesOnlyAlongTheDrySidesInTheLiquidsFoam(){
		WaterLive live = new WaterLive();
		run( live, 0.1f, null );
		Marks.reset();
		layers[GameScene.Layer.SURFACE.ordinal()].clear();
		//a cell on the pool's north edge, mid-way along: dry to the north only
		int c = 10 + 4 * W;
		new WaterAmbience().lapAt( Dungeon.level, WaterFX.liquid(), c );
		assertEquals( 1, Marks.live( Marks.FOAM ) );
		Marks.Mark m = null;
		for (Gizmo g : layers[GameScene.Layer.SURFACE.ordinal()].membersView()){
			if (g instanceof Group) for (Gizmo x : ((Group) g).membersView()) if (x instanceof Marks.Mark) m = (Marks.Mark) x;
		}
		assertEquals( 10 * 16 + 8, m.px, 0.01f );
		assertEquals( 4 * 16 + 2, m.py, 0.01f );
		assertEquals( 16, m.width, 0 );
		assertEquals( WaterFX.liquid().foam, m.colorNow() );
		//the pool's north-west corner: two dry sides
		new WaterAmbience().lapAt( Dungeon.level, WaterFX.liquid(), 6 + 4 * W );
		assertEquals( 3, Marks.live( Marks.FOAM ) );
	}

	@Test
	public void noGlintsInHeavyRain(){
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		ClimateManager.debugPrecipOverride = 0.6f;
		WaterLive live = new WaterLive();
		int[] most = new int[Marks.KINDS];
		run( live, 6f, most );
		assertEquals( 0, most[Marks.GLINT] );
	}

	@Test
	public void aBubblePopsIntoARing(){
		WaterAmbience.bubble( 10 * 16 + 8, 6 * 16 + 8, 0xFFFFFF );
		assertEquals( 1, Marks.live( Marks.BUBBLE ) );
		Group surface = layers[GameScene.Layer.SURFACE.ordinal()];
		boolean ringed = false;
		for (int i = 0; i < 30; i++){
			WaterFX.frame();
			surface.update();
			for (Gizmo g : surface.membersView()) if (g instanceof Ripple && g.alive) ringed = true;
		}
		assertTrue( ringed );
		assertEquals( 0, Marks.live( Marks.BUBBLE ) );
	}

	@Test
	public void aGnatsDipRingsTheWaterAndAFishRisesToOneInEight(){
		Group effects = layers[GameScene.Layer.EFFECTS.ordinal()], surface = layers[GameScene.Layer.SURFACE.ordinal()];
		int small = 0, middling = 0;
		for (int k = 0; k < 80; k++){
			WaterAmbience.dip( 10 * 16 + 8, 6 * 16 + 8 );
			for (int i = 0; i < 20; i++){
				WaterFX.frame();
				effects.update();
				surface.update();
			}
			small += count( surface, WaterFX.S );
			middling += count( surface, WaterFX.M );
			surface.clear();
			FxRing.reset();
		}
		assertTrue( "most dips ring small: " + small, small >= 50 );
		assertTrue( "a fish rose now and then: " + middling, middling >= 2 && middling <= 30 );
		//the dip off the water: nothing at all
		surface.clear();
		WaterAmbience.dip( 2 * 16 + 8, 2 * 16 + 8 );
		for (int i = 0; i < 20; i++){
			WaterFX.frame();
			effects.update();
			surface.update();
		}
		assertEquals( 0, count( surface, WaterFX.S ) + count( surface, WaterFX.M ) );
	}

	//the rings of a size made (alive, or spent), a ring's first being its own
	private static int count( Group surface, int size ){
		int n = 0;
		for (Gizmo g : surface.membersView()) if (g instanceof Ripple && ((Ripple) g).sizeMade() == size && !((Ripple) g).isSecond()) n++;
		return n;
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SplashParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The weather on the water: the rain's rings lie in the water's surface, never more at once than
 * twice the water in view (24 at most); a splash on the water is a ring there, not the weather's;
 * hail rings harder and throws a drop up; the pocks come with heavy rain; and a raindrop's life
 * never draws the game's own random numbers.
 */
public class WaterRainTest {

	static final int W = WaterLiveTest.W;

	private float savedElapsed, savedTime, savedRain;
	private PrecipType savedType;
	private Group[] layers;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedElapsed = Game.elapsed;
		savedTime = Game.timeTotal;
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
		WaterLive.sceneCreated();
		FxRandom.seedForTests( 3 );
		Dungeon.level = WaterLiveTest.pond( Assets.Environment.WATER_SEWERS );
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 10 + 3 * W;
		Game.elapsed = 1 / 60f;
		Game.timeTotal = 10f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		WaterLive.planeForTests = null;
		WaterFX.sceneCreated();
		FxBudget.reset();
		FxRing.reset();
		WaterLive.sceneCreated();
		ClimateManager.debugPrecipOverride = savedRain;
		ClimateManager.debugPrecipTypeOverride = savedType;
		Game.elapsed = savedElapsed;
		Game.timeTotal = savedTime;
		Dungeon.level = null;
		Dungeon.hero = null;
	}

	private int living( Class<?> c ){
		int n = 0;
		for (Group g : layers){
			for (Gizmo m : g.membersView()){
				if (m != null && m.alive && c.isInstance( m )) n++;
				if (m instanceof Group) for (Gizmo k : ((Group) m).membersView()) if (k != null && k.alive && c.isInstance( k )) n++;
			}
		}
		return n;
	}

	private void frame(){
		Game.timeTotal += Game.elapsed;
		FxBudget.frame( Game.elapsed );
		WaterFX.frame();
		for (Group g : layers) g.update();
	}

	@Test
	public void theRainsRingsKeepToTwiceTheWaterInView(){
		WaterAmbience.seen = 3;
		int most = 0;
		for (int f = 0; f < 120; f++){
			for (int i = 0; i < 4; i++) WaterRain.drop( (7 + i) * 16 + 8, 6 * 16 + 8, false );
			frame();
			most = Math.max( most, WaterRain.spreading() );
		}
		assertEquals( 6, most );
		assertTrue( "rings on the water's surface: " + living( Ripple.class ), living( Ripple.class ) <= 6 );
		//and 24 at most however much water there is
		WaterAmbience.seen = 200;
		for (int f = 0; f < 60; f++){
			for (int i = 0; i < 6; i++) WaterRain.drop( (7 + i) * 16 + 8, 6 * 16 + 8, false );
			frame();
			most = Math.max( most, WaterRain.spreading() );
		}
		assertEquals( 24, most );
		//none off the water
		WaterRain.reset();
		assertFalse( WaterRain.drop( 2 * 16, 2 * 16, false ) );
	}

	@Test
	public void aSplashOnTheWaterIsARingInItsSurface(){
		Group effects = layers[GameScene.Layer.EFFECTS.ordinal()];
		SplashParticle.splash( effects, 10 * 16 + 8, 6 * 16 + 8, 0xFFFFFF, 0.6f );
		//(the weather's own crown, made on dry ground, paints its sheet on the GPU: not here)
		assertEquals( 0, living( SplashParticle.class ) );
		assertEquals( 1, living( Ripple.class ) );
	}

	@Test
	public void hailRingsHarderAndThrowsADropBackUp(){
		WaterRain.hail( 10 * 16 + 8, 6 * 16 + 8 );
		assertEquals( "a middling ring and its second", 2, living( Ripple.class ) );
		assertEquals( 1, living( FxParticle.class ) );
	}

	@Test
	public void heavyRainLaysThePocksOverTheWater(){
		WaterLive.planeForTests = new SkinnedBlock( W * 16, WaterLiveTest.H * 16, Assets.Environment.WATER_SEWERS );
		WaterLive live = new WaterLive();
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		ClimateManager.debugPrecipOverride = 0.3f;
		live.update();
		int light = 0, laid = 0;
		for (Gizmo g : live.membersView()) if (g instanceof WaterLive.Plane && g.visible){
			if (((WaterLive.Plane) g).additive) light++;
			else laid++;
		}
		assertEquals( "light rain: no pocks", 0, laid );
		ClimateManager.debugPrecipOverride = 0.7f;
		live.update();
		WaterLive.Plane pocks = null;
		for (Gizmo g : live.membersView()) if (g instanceof WaterLive.Plane && g.visible && !((WaterLive.Plane) g).additive) pocks = (WaterLive.Plane) g;
		assertTrue( pocks != null );
		assertEquals( WaterGeom.pockAlpha( 0.7f ), pocks.am, 1e-4f );
		assertTrue( pocks.am > 0.15f && pocks.am < 0.45f );
		//its frames swap every 0.08 s
		Object t0 = pocks.texture;
		Game.elapsed = 0.09f;
		live.update();
		assertTrue( t0 != pocks.texture );
	}

	@Test
	public void aDownpourDimsTheCalmShineOfTheSheens(){
		WaterLive.planeForTests = new SkinnedBlock( W * 16, WaterLiveTest.H * 16, Assets.Environment.WATER_SEWERS );
		WaterLive live = new WaterLive();
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		ClimateManager.debugPrecipOverride = 0f;
		Game.elapsed = 0f;
		live.update();
		float dry = 0f;
		for (Gizmo g : live.membersView()) if (g instanceof WaterLive.Plane && g.visible && ((WaterLive.Plane) g).additive) dry = Math.max( dry, ((WaterLive.Plane) g).am );
		assertTrue( dry > 0f );
		ClimateManager.debugPrecipOverride = 1f;
		live.update();
		for (Gizmo g : live.membersView()){
			if (!(g instanceof WaterLive.Plane) || !g.visible || !((WaterLive.Plane) g).additive) continue;
			assertTrue( "a sheen at " + ((WaterLive.Plane) g).am + " in a downpour", ((WaterLive.Plane) g).am <= dry * 0.31f );
		}
	}

	//the weather's particles paint their sheet on the GPU when made, so they cannot be made here:
	//their sources are read instead
	@Test
	public void theWeathersDropsDrawNoneOfTheGamesRandomNumbers() throws java.io.IOException {
		String dir = "src/main/java/xyz/gabriwar/warpedpixeldungeon/effects/particles/";
		for (String f : new String[]{ "RainParticle", "SleetParticle", "HailParticle", "SplashParticle" }){
			String src = new String( java.nio.file.Files.readAllBytes( java.nio.file.Paths.get( dir + f + ".java" ) ) );
			assertFalse( f + " draws the game's Random", src.contains( "com.watabou.utils.Random" ) );
		}
		//and the water's own rings draw none either
		Random.pushGenerator( 77 );
		float expected = Random.Float();
		Random.popGenerator();
		Random.pushGenerator( 77 );
		WaterAmbience.seen = 40;
		for (int f = 0; f < 60; f++){
			WaterRain.drop( 10 * 16 + 8, 6 * 16 + 8, f % 2 == 0 );
			WaterRain.hail( 9 * 16 + 8, 7 * 16 + 8 );
			frame();
		}
		assertEquals( expected, Random.Float(), 0f );
		Random.popGenerator();
	}
}

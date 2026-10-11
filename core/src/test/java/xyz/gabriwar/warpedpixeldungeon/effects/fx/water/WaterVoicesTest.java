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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Place;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The places' water voices seen where they are heard: a drip's drop rings the water, a lap foams
 * the bank, a gurgle bubbles up at the drain, a fountain rings its basin three times; nothing for
 * a cell out of the hero's sight; none of the game's random numbers drawn.
 */
public class WaterVoicesTest {

	static final int W = WaterLiveTest.W;

	private float savedElapsed;
	private Camera savedCam;
	private Group[] layers;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedElapsed = Game.elapsed;
		savedCam = Camera.main;
		Camera.main = null;
		Emitter.freezeEmitters = false;
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		WaterFX.sceneCreated();
		WaterLiveTest.fullTier();
		FxRing.reset();
		WaterLive.sceneCreated();
		FxRandom.seedForTests( 13 );
		Dungeon.level = WaterLiveTest.pond( Assets.Environment.WATER_SEWERS );
		//a drain pipe in the wall over the pool's north-east corner
		Dungeon.level.map[15 + 3 * W] = Terrain.WALL_DECO;
		Dungeon.level.buildFlagMaps();
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 10 + 3 * W;
		Game.elapsed = 1 / 60f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		WaterFX.sceneCreated();
		FxRing.reset();
		WaterLive.sceneCreated();
		FxBudget.reset();
		Camera.main = savedCam;
		Game.elapsed = savedElapsed;
		Dungeon.level = null;
		Dungeon.hero = null;
	}

	private int rings(){
		int n = 0;
		for (Gizmo g : layers[GameScene.Layer.SURFACE.ordinal()].membersView()) if (g instanceof Ripple && g.alive) n++;
		return n;
	}

	private int most( float seconds ){
		int most = 0;
		for (float t = 0; t < seconds; t += Game.elapsed){
			FxBudget.frame( 1 / 60f );
			WaterFX.frame();
			WaterVoices.update( Game.elapsed );
			for (Group g : layers) g.update();
			most = Math.max( most, rings() );
		}
		return most;
	}

	@Test
	public void aDripsDropRingsTheWater(){
		WaterVoices.played( AmbientSound.DRIP, 10 + 6 * W, Place.SEWERS );
		assertEquals( 0, rings() );
		assertTrue( most( 0.4f ) >= 1 );
	}

	@Test
	public void aLapFoamsTheBankAtOnce(){
		WaterLive live = new WaterLive();
		layers[GameScene.Layer.SHEEN.ordinal()].add( live );
		WaterVoices.played( AmbientSound.LAP, 10 + 4 * W, Place.SHORE );
		assertEquals( 1, Marks.live( Marks.FOAM ) );
	}

	@Test
	public void aGurgleBubblesUpAtTheDrain(){
		WaterVoices.played( AmbientSound.GURGLE, 15 + 3 * W, Place.SEWERS );
		assertTrue( WaterVoices.queued() >= 2 );
		int bubbles = 0;
		for (int i = 0; i < 60; i++){
			WaterVoices.update( 1 / 60f );
			bubbles = Math.max( bubbles, Marks.live( Marks.BUBBLE ) );
		}
		assertTrue( bubbles >= 1 );
		assertEquals( 0, WaterVoices.queued() );
	}

	@Test
	public void aFountainRingsItsBasinThreeTimes(){
		WaterVoices.played( AmbientSound.TRICKLE, 10 + 6 * W, Place.CITY );
		int rings = 0, last = 0;
		for (float t = 0; t < 3.5f; t += Game.elapsed){
			FxBudget.frame( 1 / 60f );
			WaterFX.frame();
			WaterVoices.update( Game.elapsed );
			int n = rings();
			if (n > last) rings += n - last;
			last = n;
			for (Group g : layers) g.update();
			last = rings();
		}
		assertEquals( 3, rings );
	}

	@Test
	public void nothingOutOfSightAndNoneOfTheGamesRandomNumbers(){
		Arrays.fill( Dungeon.level.heroFOV, false );
		WaterVoices.played( AmbientSound.DRIP, 10 + 6 * W, Place.SEWERS );
		WaterVoices.played( AmbientSound.GURGLE, 15 + 3 * W, Place.SEWERS );
		assertEquals( 0, WaterVoices.queued() );
		assertEquals( 0, most( 0.5f ) );
		Arrays.fill( Dungeon.level.heroFOV, true );
		Random.pushGenerator( 5 );
		float expected = Random.Float();
		Random.popGenerator();
		Random.pushGenerator( 5 );
		for (AmbientSound s : new AmbientSound[]{ AmbientSound.DRIP, AmbientSound.SPLASH, AmbientSound.GURGLE, AmbientSound.TRICKLE }){
			WaterVoices.played( s, 10 + 6 * W, Place.CAVES );
		}
		most( 2f );
		assertEquals( expected, Random.Float(), 0f );
		Random.popGenerator();
	}
}

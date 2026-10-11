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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Source;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The sewers' pipes and the pits' waterfalls headless: a trickle's drops land at its foot and ring
 * the water there, a pour churns foam and rings it harder, both as Source hears them; an edge's
 * streaks fall 10-14 px into the pit and never more than 8 an edge or 64 in all; edges far off
 * are half as busy; none of it draws the game's random numbers or touches the GPU.
 */
public class WaterFallsTest {

	static final int W = WaterLiveTest.W;

	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedElapsed = Game.elapsed;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Emitter.freezeEmitters = false;
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		WaterFX.sceneCreated();
		WaterLive.sceneCreated();
		WaterLiveTest.fullTier();
		FxRing.reset();
		FxRandom.seedForTests( 3 );
		Dungeon.level = WaterLiveTest.pond( Assets.Environment.WATER_SEWERS );
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 10 + 2 * W;
		Game.elapsed = 1 / 60f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		WaterFX.sceneCreated();
		WaterLive.sceneCreated();
		FxBudget.reset();
		FxRing.reset();
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//a wall with a pipe in it over the pool's north edge, at column x: pouring or not, as asked
	private int pipeCell( boolean pours ){
		for (int x = 6; x < 16; x++){
			int c = x + 3 * W;
			if (Source.pours( c ) == pours){
				Dungeon.level.map[c] = Terrain.WALL_DECO;
				Dungeon.level.buildFlagMaps();
				return c;
			}
		}
		throw new AssertionError( "no such pipe" );
	}

	private int rings( int size ){
		int n = 0;
		for (Gizmo g : layers[GameScene.Layer.SURFACE.ordinal()].membersView()){
			if (g instanceof Ripple && g.alive && ((Ripple) g).sizeMade() == size && !((Ripple) g).isSecond()) n++;
		}
		return n;
	}

	@Test
	public void aTricklesDropsLandAtItsFootAndRingTheWaterThere(){
		WaterLiveTest.failOnGl();
		int c = pipeCell( false );
		WaterFalls.Pipe p = WaterFalls.pipe( Dungeon.level, c );
		assertTrue( p.overWater );
		assertEquals( (c / W + 1) * 16 + WaterFalls.FOOT_IN, p.footY, 0.001f );
		FxEmitter e = new FxEmitter();
		e.autoKill = false;
		layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
		Random.pushGenerator( 77 );
		float before = Random.Float();
		Random.popGenerator();
		Random.pushGenerator( 77 );
		int ringed = 0;
		for (int i = 0; i < 120; i++){
			WaterFX.frame();
			WaterFalls.pipe( p, e, Game.elapsed );
			e.update();
			layers[GameScene.Layer.SURFACE.ordinal()].update();
			for (Gizmo g : e.membersView()){
				if (g instanceof FxParticle && g.alive) assertTrue( ((FxParticle) g).py <= p.footY + 0.001f );
			}
			ringed = Math.max( ringed, rings( WaterFX.S ) );
		}
		assertEquals( "none of the game's random numbers drawn", before, Random.Float(), 0 );
		Random.popGenerator();
		assertTrue( "it rang the water", ringed > 0 );
		assertEquals( "and never a middling ring", 0, rings( WaterFX.M ) );
	}

	@Test
	public void aPourChurnsFoamAndRingsTheWaterHarder(){
		int c = pipeCell( true );
		WaterFalls.Pipe p = WaterFalls.pipe( Dungeon.level, c );
		assertTrue( p.pours );
		FxEmitter e = new FxEmitter();
		e.autoKill = false;
		layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
		int most = 0, middling = 0, foam = 0;
		for (int i = 0; i < 120; i++){
			WaterFX.frame();
			WaterFalls.pipe( p, e, Game.elapsed );
			e.update();
			layers[GameScene.Layer.SURFACE.ordinal()].update();
			int live = 0;
			for (Gizmo g : e.membersView()) if (g.alive) live++;
			most = Math.max( most, live );
			middling = Math.max( middling, rings( WaterFX.M ) );
			foam = Math.max( foam, Marks.live( Marks.POUR ) );
		}
		assertTrue( "a sheet: " + most, most >= 6 && most <= 16 );
		assertTrue( middling > 0 );
		assertTrue( foam > 0 && foam <= Marks.CAPS[Marks.POUR] );
	}

	@Test
	public void anEdgesStreaksFallIntoThePitWithinItsCaps(){
		Level l = Dungeon.level;
		//a pit under the pool's south row, all along it
		for (int x = 6; x < 16; x++) l.map[x + 10 * W] = Terrain.CHASM;
		l.buildFlagMaps();
		Dungeon.hero.pos = 10 + 11 * W;
		WaterFalls.Fall[] falls = new WaterFalls.Fall[10];
		FxEmitter[] es = new FxEmitter[10];
		for (int i = 0; i < 10; i++){
			falls[i] = WaterFalls.fall( l, 6 + i + 9 * W );
			es[i] = new FxEmitter();
			es[i].autoKill = false;
			layers[GameScene.Layer.EFFECTS.ordinal()].add( es[i] );
		}
		int most = 0;
		for (int f = 0; f < 600; f++){
			Game.timeTotal += Game.elapsed;
			for (int i = 0; i < 10; i++){
				WaterFalls.fall( falls[i], es[i], true, Game.elapsed );
				es[i].update();
				assertTrue( falls[i].live() <= WaterFalls.PER_EDGE );
				for (Gizmo g : es[i].membersView()){
					if (!(g instanceof WaterFalls.FallDrop) || !g.alive) continue;
					assertTrue( ((FxParticle) g).py <= falls[i].lipY + WaterFalls.REACH_MAX + 8.001f );
				}
			}
			most = Math.max( most, WaterFalls.falling() );
		}
		assertTrue( "falling: " + most, most > 10 && most <= WaterFalls.ALL );
		assertTrue( falls[0].lip().visible );
		WaterFalls.fall( falls[0], es[0], false, Game.elapsed );
		assertTrue( !falls[0].lip().visible );
	}

	@Test
	public void edgesOutOfSightLetTheirDropsGoSoTheOthersStillFall(){
		Level l = Dungeon.level;
		for (int x = 6; x < 16; x++) l.map[x + 10 * W] = Terrain.CHASM;
		l.buildFlagMaps();
		Dungeon.hero.pos = 10 + 11 * W;
		WaterFalls.Fall[] falls = new WaterFalls.Fall[10];
		FxEmitter[] es = new FxEmitter[10];
		for (int i = 0; i < 10; i++){
			falls[i] = WaterFalls.fall( l, 6 + i + 9 * W );
			es[i] = new FxEmitter();
			es[i].autoKill = false;
			layers[GameScene.Layer.EFFECTS.ordinal()].add( es[i] );
		}
		//the first eight seen a while, as the hero passes
		for (int f = 0; f < 240; f++){
			Game.timeTotal += Game.elapsed;
			FxBudget.frame( Game.elapsed );
			for (int i = 0; i < 8; i++){
				WaterFalls.fall( falls[i], es[i], true, Game.elapsed );
				es[i].update();
			}
		}
		assertTrue( WaterFalls.falling() > 0 );
		//out of his sight (their emitters stop updating what they hold): none of theirs left
		Game.timeTotal += Game.elapsed;
		for (int i = 0; i < 8; i++) WaterFalls.fall( falls[i], es[i], false, Game.elapsed );
		assertEquals( 0, WaterFalls.falling() );
		for (int i = 0; i < 8; i++) assertEquals( 0, falls[i].live() );
		//and the two still seen fall as ever
		int most = 0;
		for (int f = 0; f < 120; f++){
			Game.timeTotal += Game.elapsed;
			FxBudget.frame( Game.elapsed );
			for (int i = 0; i < 8; i++) WaterFalls.fall( falls[i], es[i], false, Game.elapsed );
			for (int i = 8; i < 10; i++){
				WaterFalls.fall( falls[i], es[i], true, Game.elapsed );
				es[i].update();
			}
			most = Math.max( most, falls[8].live() + falls[9].live() );
		}
		assertTrue( "the others fall: " + most, most > 4 );
	}

	@Test
	public void anEdgeFarFromTheHeroIsHalfAsBusy(){
		Level l = Dungeon.level;
		Dungeon.hero.pos = 1 + 1 * W;
		WaterFalls.Fall near = WaterFalls.fall( l, 6 + 4 * W ), far = WaterFalls.fall( l, 15 + 9 * W );
		assertEquals( 1f, WaterFalls.share( near ), 0 );
		assertEquals( 0.5f, WaterFalls.share( far ), 0 );
	}
}

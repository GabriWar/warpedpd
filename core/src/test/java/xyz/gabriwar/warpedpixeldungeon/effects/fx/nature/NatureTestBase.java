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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.nature;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FireflyParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LightParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SunlightParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The area's tests' common ground, headless: stand-in layers for the game's scene, a small garden
 * level (a pool, a row of high grass, all of it seen), the budget at its fullest, a GPU that
 * answers or one that fails on any call, and a check that something drew nothing of the game's
 * Random.
 */
public abstract class NatureTestBase {

	static final int W = 16;

	private GL20 savedGl, savedGl20;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	Group[] layers;

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
		stubGl();
		fullBudget();
		FxLight.reset();
		FxRing.reset();
		WaterFX.sceneCreated();
		Emitter.freezeEmitters = false;
		FxRandom.seedForTests( 7 );
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		Dungeon.level = garden();
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 2 + 2 * W;
		Game.elapsed = 1 / 30f;
		GrowthPulse.reset();
		HealFx.reset();
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		FxBudget.reset();
		FxLight.reset();
		FxRing.reset();
		WaterFX.sceneCreated();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//the budget at its fullest: nothing thinned by load or a struggling frame
	static void fullBudget(){
		FxBudget.reset();
		try {
			Method m = FxBudget.class.getDeclaredMethod( "resetGovernor" );
			m.setAccessible( true );
			m.invoke( null );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
		FxBudget.frame( 1 / 60f );
	}

	//a floor with a pool (columns 8-13, rows 4-11), a row of high grass (row 1); all seen
	static Level garden(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public String waterTex(){ return Assets.Environment.WATER_CAVES; }
		};
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int y = 4; y < 12; y++) for (int x = 8; x < 14; x++) l.map[x + y * W] = Terrain.WATER;
		for (int x = 1; x < 7; x++) l.map[x + W] = Terrain.HIGH_GRASS;
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		l.visited = new boolean[l.length()];
		l.mapped = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		Arrays.fill( l.visited, true );
		return l;
	}

	static void failOnGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, a) -> { throw new AssertionError( "GL call while building: " + m.getName() ); } );
	}

	static void stubGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			return null;
		} );
	}

	void frames( int n ){
		for (int i = 0; i < n; i++){
			FxBudget.frame( Game.elapsed );
			WaterFX.frame();
			for (Group g : layers) g.update();
		}
	}

	static int living( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.alive && c.isInstance( m )) n++;
			if (m instanceof Group) n += living( (Group) m, c );
		}
		return n;
	}

	int living( Class<?> c ){
		int n = 0;
		for (Group g : layers) n += living( g, c );
		return n;
	}

	static float cx( int cell ){
		return (cell % W + 0.5f) * 16;
	}

	static float cy( int cell ){
		return (cell / W + 0.5f) * 16;
	}

	//the game's next random number, unmoved by whatever ran
	static float nextGameRandom( long seed, Runnable r ){
		Random.pushGenerator( seed );
		r.run();
		float f = Random.Float();
		Random.popGenerator();
		return f;
	}

	static void assertNoGameRandom( Runnable r ){
		assertEquals( "the game's Random untouched", nextGameRandom( 99, () -> {} ), nextGameRandom( 99, r ), 0f );
	}
}

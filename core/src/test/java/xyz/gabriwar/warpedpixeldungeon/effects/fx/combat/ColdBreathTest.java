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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RatSprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;
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
import static org.junit.Assert.assertTrue;

/**
 * A hero's breath in the cold air (ColdBreath): out under the open sky while the air is below
 * freezing - never in the caves or on a dungeon floor, nor for the dead - a puff every BREATH_MIN to
 * BREATH_MAX s; none while a chill's look breathes for him, nor while his sprite is hidden.
 */
public class ColdBreathTest {

	private static final int W = 30, CENTER = 15 + 15 * W;

	private GL20 savedGl, savedGl20;
	private Camera savedCam;
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
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		savedCam = Camera.main;
		savedElapsed = Game.elapsed;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		//the hero where the breath is: the budget thins nothing of it by its distance
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = CENTER;
		//a GPU that answers yes: a sprite's texture can be made
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
		for (int i = 0; i < 1200; i++) FxBudget.frame( 1 / 120f );
		FxBudget.reset();
		Emitter.freezeEmitters = false;
		FxRandom.seedForTests( 5 );
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		Actor.clear();
		Game.elapsed = 1 / 30f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		FxBudget.reset();
		ClimateManager.debugTempOverride = Float.NaN;
		Actor.clear();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Camera.main = savedCam;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	//a slice of grass with rock round its edge
	private static <L extends Level> L grass( L l ){
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.GRASS );
		for (int i = 0; i < W; i++){
			l.map[i] = l.map[i + (W - 1) * W] = l.map[i * W] = l.map[W - 1 + i * W] = Terrain.WALL;
		}
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
		return l;
	}

	//one standing in the middle, his sprite seen
	private static CharSprite body(){
		Rat rat = new Rat();
		rat.state = rat.PASSIVE;
		rat.pos = CENTER;
		CharSprite s = new RatSprite();
		s.ch = rat;
		rat.sprite = s;
		s.place( CENTER );
		s.visible = true;
		return s;
	}

	//the puffs breathed over `seconds`: each one new in the air (they live 0.9 s, under the gap)
	private int puffs( ColdBreath b, boolean chilled, float seconds ){
		int n = 0, was = 0;
		for (float t = 0; t < seconds; t += Game.elapsed){
			FxBudget.frame( 1 / 60f );
			b.update( chilled );
			for (Group g : layers) g.update();
			int now = living( layers[GameScene.Layer.EFFECTS.ordinal()], FxParticle.class );
			if (now > was) n += now - was;
			was = now;
		}
		return n;
	}

	private static int living( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.alive && c.isInstance( m )) n++;
			if (m instanceof Group) n += living( (Group) m, c );
		}
		return n;
	}

	@Test
	public void theAirIsFreezingOnlyOutUnderTheOpenSkyBelowZero(){
		Rat rat = new Rat();
		rat.pos = CENTER;
		OverworldLevel surface = grass( new OverworldLevel( 0 ) );
		ClimateManager.debugTempOverride = -8f;
		assertTrue( ColdBreath.freezing( surface, rat ) );
		ClimateManager.debugTempOverride = 4f;
		assertFalse( ColdBreath.freezing( surface, rat ) );

		ClimateManager.debugTempOverride = -8f;
		assertTrue( "the peaks", ColdBreath.freezing( grass( new OverworldLevel( 4 ) ), rat ) );
		assertFalse( "the caves", ColdBreath.freezing( grass( new OverworldLevel( -3 ) ), rat ) );
		assertFalse( "a dungeon floor", ColdBreath.freezing( grass( new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		} ), rat ) );
		rat.HP = 0;
		assertFalse( "the dead", ColdBreath.freezing( surface, rat ) );
	}

	@Test
	public void aPuffInTheColdEveryOneAndAHalfSecondsOrSo(){
		grass( new OverworldLevel( 0 ) );
		ClimateManager.debugTempOverride = -8f;
		CharSprite s = body();
		int n = puffs( new ColdBreath( s ), false, 9f );
		//9 s at one every 1.2-1.6 s, the first within 1.2 s
		assertTrue( "puffs: " + n, n >= 5 && n <= 8 );
	}

	@Test
	public void noPuffInTheWarmWhileChilledOrUnseen(){
		grass( new OverworldLevel( 0 ) );
		CharSprite s = body();
		ClimateManager.debugTempOverride = 4f;
		assertEquals( "in the warm", 0, puffs( new ColdBreath( s ), false, 6f ) );
		ClimateManager.debugTempOverride = -8f;
		assertEquals( "a chill's look breathes for him", 0, puffs( new ColdBreath( s ), true, 6f ) );
		s.visible = false;
		assertEquals( "unseen", 0, puffs( new ColdBreath( s ), false, 6f ) );
	}
}

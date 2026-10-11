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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The sparks (SparkParticle: the struck spark, static, electric, hot metal), the crackle where a
 * lightning lands (Crackle), electrified water (ElectricityFx) and the lightning made render-side:
 * painted and thrown as the language says, made without the GPU, drawing none of the game's
 * random numbers, and kept to their caps.
 */
public class SparksAndChargeTest {

	private static final int W = 16;

	private GL20 savedGl, savedGl20;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;

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
		//back to the full tier whatever the governor was left at
		java.lang.reflect.Method governor = FxBudget.class.getDeclaredMethod( "resetGovernor" );
		governor.setAccessible( true );
		governor.invoke( null );
		FxBudget.reset();
		Crackle.reset();
		Afterglow.reset();
		StrikeLights.reset();
		ElectricityFx.sceneCreated();
		Fx.sceneCreated();
		FxRandom.seedForTests( 11 );
		Dungeon.level = pond();
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 2 + 2 * W;
		Game.elapsed = 1 / 60f;
	}

	@After
	public void tearDown(){
		FxBudget.reset();
		ElectricityFx.sceneCreated();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//a floor with a pool: water in columns 6-13, rows 4-9; all of it seen
	private static Level pond(){
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
		for (int y = 4; y < 10; y++) for (int x = 6; x < 14; x++) l.map[x + y * W] = Terrain.WATER;
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		l.visited = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		Arrays.fill( l.visited, true );
		return l;
	}

	private static void failOnGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, a) -> { throw new AssertionError( "GL call: " + m.getName() ); } );
	}

	private static void stubGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			return null;
		} );
	}

	//the game's next random number, and whether it is the one a fresh generator of the seed gives:
	//nothing drew from the game's Random since the seed was pushed
	private static void assertUntouched( long seed ){
		float next = Random.Float();
		Random.popGenerator();
		Random.pushGenerator( seed );
		float fresh = Random.Float();
		Random.popGenerator();
		assertEquals( "the game's random numbers untouched", fresh, next, 0f );
	}

	private static ArrayList<SparkParticle> living( Emitter e ){
		ArrayList<SparkParticle> out = new ArrayList<>();
		for (Gizmo g : e.membersView()) if (g instanceof SparkParticle && g.alive) out.add( (SparkParticle) g );
		return out;
	}

	// ------------------------------------------------------------------ sparks

	@Test
	public void theSparksAreMadeWithoutTheGpuNorTheGamesRandom(){
		failOnGl();
		Random.pushGenerator( 4242 );
		Emitter e = new Emitter();
		Emitter.Factory[] kinds = { SparkParticle.FACTORY, SparkParticle.STATIC, SparkParticle.ELECTRIC, SparkParticle.METAL };
		for (Emitter.Factory f : kinds){
			for (int i = 0; i < 6; i++) f.emit( e, i, 40f, 40f );
			assertTrue( f.lightMode() );
		}
		for (int f = 0; f < 60; f++) e.update();
		assertUntouched( 4242 );
	}

	@Test
	public void aStruckSparkIsAStreakThrownUpThatFallsAndBounces(){
		Emitter e = new Emitter();
		for (int i = 0; i < 40; i++) SparkParticle.FACTORY.emit( e, i, 40f, 40f );
		ArrayList<SparkParticle> sparks = living( e );
		assertEquals( 40, sparks.size() );
		for (SparkParticle s : sparks){
			float v = (float)Math.sqrt( s.vx * s.vx + s.vy * s.vy );
			assertTrue( "speed " + v, v >= SparkParticle.SPEED_MIN - 1e-3f && v <= SparkParticle.SPEED_MAX + 1e-3f );
			assertTrue( "into the upper half", s.vy <= 1e-3f );
			//white at first, a painted frame at its own size
			assertEquals( 1f, s.scale.x, 0f );
			assertEquals( 1f, s.rm, 1e-3f );
		}
		//falling: half a second on, every one still alive is lower than it rose to, none past its
		//bounce point by more than a pixel
		float[] top = new float[sparks.size()];
		Arrays.fill( top, Float.MAX_VALUE );
		for (int f = 0; f < 30; f++){
			e.update();
			for (int i = 0; i < sparks.size(); i++) top[i] = Math.min( top[i], sparks.get( i ).py );
		}
		for (int i = 0; i < sparks.size(); i++){
			SparkParticle s = sparks.get( i );
			if (!s.alive) continue;
			assertTrue( s.py <= 40f + SparkParticle.BOUNCE_MAX + 1f );
		}
		//all gone by 0.7 s (and their one-frame twinkles)
		for (int f = 0; f < 30; f++) e.update();
		for (SparkParticle s : sparks) assertFalse( s.alive );
	}

	@Test
	public void staticCrackleStaysPutDotPlusDot(){
		Emitter e = new Emitter();
		SparkParticle.STATIC.emit( e, 0, 30f, 30f );
		SparkParticle s = living( e ).get( 0 );
		assertEquals( 0f, s.vx, 0f );
		assertEquals( 0f, s.vy, 0f );
		boolean plus = false;
		for (int f = 0; f < 30 && s.alive; f++){
			e.update();
			assertEquals( 30f, s.px, 0f );
			plus |= s.width == FxFrames.PLUS_3[2];
		}
		assertTrue( "a plus between its dots", plus );
		assertFalse( "gone within 0.4 s", s.alive );
	}

	@Test
	public void theFactoriesAreTheSameObjectsEveryTime(){
		Emitter.Factory f = SparkParticle.FACTORY;
		assertSame( f, SparkParticle.FACTORY );
		assertNotNull( SparkParticle.ELECTRIC );
		assertNotNull( SparkParticle.METAL );
	}

	// ------------------------------------------------------------------ the crackle

	@Test
	public void theCrackleIsCappedAndGoesAfterItsMoment(){
		stubGl();
		Group sky = new Group();
		Crackle first = null;
		for (int i = 0; i < Crackle.MOST + 4; i++){
			Crackle c = Crackle.at( sky, 40f + 16f * i, 40f, 0.9f );
			if (i == 0) first = c;
			if (i < Crackle.MOST) assertNotNull( c );
			else assertNull( c );
		}
		assertEquals( Crackle.MOST, Crackle.alive() );
		//its twigs within reach of the strike, re-thrown every 0.05 s
		Image line = (Image) first.membersView().get( 0 );
		float x0 = line.x + line.origin.x, y0 = line.y + line.origin.y;
		assertTrue( Math.abs( x0 - 40f ) <= Crackle.REACH && Math.abs( y0 - 40f ) <= Crackle.REACH );
		Game.elapsed = 0.06f;
		sky.update();
		float x1 = line.x + line.origin.x;
		assertTrue( "thrown anew", x1 != x0 || line.y + line.origin.y != y0 );
		sky.update();
		sky.update();
		assertEquals( "gone after 0.15 s", 0, Crackle.alive() );
		//and the next strike reuses them
		assertNotNull( Crackle.at( sky, 40f, 40f, 0.9f ) );
		int crackles = 0;
		for (Gizmo g : sky.membersView()) if (g instanceof Crackle) crackles++;
		assertEquals( Crackle.MOST, crackles );
	}

	@Test
	public void aSecondCrackleOnThePlaceIsTheFirstRenewed(){
		stubGl();
		Group sky = new Group();
		Crackle first = Crackle.at( sky, 40f, 40f, 0.85f );
		assertSame( "a struck sprite's and its lightning's", first, Crackle.at( sky, 41f, 39f, 0.9f ) );
		assertEquals( 1, Crackle.alive() );
		//one further off, or on another layer, is its own
		assertNotSame( first, Crackle.at( sky, 40f + Crackle.REACH + 2f, 40f, 0.9f ) );
		assertNotSame( first, Crackle.at( new Group(), 40f, 40f, 0.9f ) );
		assertEquals( 3, Crackle.alive() );
		//renewed, it lasts its moment from then
		Game.elapsed = 0.1f;
		sky.update();
		assertSame( first, Crackle.at( sky, 40f, 40f, 0.9f ) );
		sky.update();
		assertTrue( ((Gizmo) first).alive );
		sky.update();
		assertFalse( ((Gizmo) first).alive );
	}

	// ------------------------------------------------------------------ electrified water

	@Test
	public void electricitysSparksAreCappedAtOneHundredFifty(){
		Electricity blob = new Electricity();
		ElectricityFx fx = new ElectricityFx( blob );
		Emitter e = new Emitter();
		for (int tick = 0; tick < 40; tick++){
			FxBudget.reset();
			for (int cell = 0; cell < 30; cell++) fx.emit( e, cell, 40f + cell, 60f );
			assertTrue( ElectricityFx.ChargeSpark.count() <= ElectricityFx.SPARKS );
			assertTrue( living( e ).size() <= ElectricityFx.SPARKS );
		}
		assertEquals( ElectricityFx.SPARKS, ElectricityFx.ChargeSpark.count() );
		//they die, and are counted out
		for (int f = 0; f < 60; f++) e.update();
		assertEquals( 0, ElectricityFx.ChargeSpark.count() );
		assertTrue( fx.lightMode() );
		assertEquals( 1f, fx.density( 1 ), 0f );
	}

	//every water cell of the pond charged, its washes and crawls run for `frames` frames, the
	//cells ticked every 0.2 s
	private static void charge( ElectricityFx.Field f, int frames ){
		Level l = Dungeon.level;
		for (int i = 0; i < frames; i++){
			if (i % 12 == 0){
				for (int c = 0; c < l.length(); c++) if (l.water[c]) f.mark( c );
			}
			f.parent.update();
		}
	}

	@Test
	public void theChargedWaterKeepsToItsNearestWashesAndItsCrawlsDrawNoGameRandom(){
		stubGl();
		Group surface = new Group(), lights = new Group();
		ElectricityFx.Field f = ElectricityFx.Field.forTests( surface, lights );
		Random.pushGenerator( 777 );
		charge( f, 60 );
		for (int i = 0; i < 60; i++) lights.update();
		assertUntouched( 777 );
		//the 24 nearest the hero of the pond's 48 cells
		assertEquals( ElectricityFx.WASHES, f.washes() );
		int seen = 0, maxNear = 0;
		for (Gizmo g : f.membersView()){
			if (!(g instanceof ElectricityFx.ChargeWash) || !g.visible) continue;
			ElectricityFx.ChargeWash w = (ElectricityFx.ChargeWash) g;
			int cell = (int)(w.x / 16) + (int)(w.y / 16) * W;
			maxNear = Math.max( maxNear, Dungeon.level.distance( Dungeon.hero.pos, cell ) );
			assertTrue( "flicker within its range: " + w.am, w.am <= ElectricityFx.WASH_ALPHA * (1 + ElectricityFx.WASH_FLICKER) + 1e-4f );
			seen++;
		}
		assertEquals( ElectricityFx.WASHES, seen );
		assertTrue( "nearest first: " + maxNear, maxNear <= 9 );
		//its crawls: faint one-leg arcs, six at most
		int crawls = 0;
		for (Gizmo g : lights.membersView()) if (g instanceof Lightning && g.alive) crawls++;
		assertTrue( "crawls " + crawls, crawls <= ElectricityFx.CRAWLS );
		assertTrue( f.crawls() <= ElectricityFx.CRAWLS );
	}

	@Test
	public void theChargedWaterFlaresWhenTheChargeChangesAndFadesWhenItGoes(){
		stubGl();
		Group surface = new Group(), lights = new Group();
		ElectricityFx.Field f = ElectricityFx.Field.forTests( surface, lights );
		charge( f, 12 );
		float calm = 0;
		for (Gizmo g : f.membersView()) if (g instanceof ElectricityFx.ChargeWash && g.visible) calm = Math.max( calm, ((Image) g).am );
		f.boost();
		surface.update();
		float flared = 0;
		for (Gizmo g : f.membersView()) if (g instanceof ElectricityFx.ChargeWash && g.visible) flared = Math.max( flared, ((Image) g).am );
		assertTrue( "flared " + flared + " over " + calm, flared > calm * 1.15f );
		//no longer ticked: gone within its hold and fade
		for (int i = 0; i < 60; i++) surface.update();
		assertEquals( 0, f.washes() );
	}

	@Test
	public void theChargedWaterFlaresWhenTheChargeRisesNotAsItRunsDown(){
		stubGl();
		Group[] layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		try {
			Electricity blob = new Electricity();
			ElectricityFx fx = new ElectricityFx( blob );
			Emitter e = new Emitter();
			blob.volume = 100;
			fx.emit( e, 0, 40f, 40f );
			ElectricityFx.Field f = ElectricityFx.Field.get();
			assertFalse( "nothing to flare from", f.flaring() );
			//a turn's decay
			blob.volume = 90;
			fx.emit( e, 0, 40f, 40f );
			assertFalse( "running down", f.flaring() );
			//charged again
			blob.volume = 140;
			fx.emit( e, 0, 40f, 40f );
			assertTrue( f.flaring() );
			Game.elapsed = 0.2f;
			f.update();
			assertFalse( f.flaring() );
			blob.volume = 130;
			fx.emit( e, 0, 40f, 40f );
			assertFalse( f.flaring() );
		} finally {
			GameScene.useLayersForTests( null );
		}
	}

	@Test
	public void aLightningMadeRenderSideDrawsNoGameRandom(){
		stubGl();
		Random.pushGenerator( 99 );
		Group stage = new Group();
		for (int i = 0; i < 5; i++){
			PointF a = DungeonTilemap.tileCenterToWorld( 6 + 4 * W ), b = new PointF( 200f, 140f );
			stage.add( Lightning.visual( Arrays.asList( Lightning.Arc.visual( a, b ), Lightning.Arc.visual( b, a ).crawl( 1 ) ), null ).major() );
		}
		Game.elapsed = 1 / 60f;
		for (int f = 0; f < 30; f++) stage.update();
		assertUntouched( 99 );
	}
}

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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Ripple;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
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

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The kit's pictures at work, headless (stand-in layers for the game's scene): nothing touches the
 * GPU while they are made, on any thread; particles fall, bounce, settle and land on the water as
 * they should and count themselves; lights show by day and night and where the hero sees them;
 * rings step through their painted frames; and every pool keeps to its cap.
 */
public class FxRuntimeTest {

	private static final int W = 16;

	private GL20 savedGl, savedGl20;
	private float savedElapsed, savedScale;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;
	private Level level;

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
		savedScale = FxBudget.scale;
		FxBudget.scale = 1f;
		FxBudget.reset();
		FxBudget.resetGovernor();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		Afterimage.reset();
		WaterFX.sceneCreated();
		Emitter.freezeEmitters = false;
		FxRandom.seedForTests( 5 );
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		level = pond();
		Dungeon.level = level;
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 2 + 2 * W;
		Game.elapsed = 1 / 30f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		FxBudget.reset();
		FxBudget.scale = savedScale;
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		Afterimage.reset();
		WaterFX.sceneCreated();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//a floor with a pool: water in columns 8-13, rows 4-11; all of it seen
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
		for (int y = 4; y < 12; y++) for (int x = 8; x < 14; x++) l.map[x + y * W] = Terrain.WATER;
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		l.visited = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		Arrays.fill( l.visited, true );
		return l;
	}

	private static void failOnGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, a) -> { throw new AssertionError( "GL call while building: " + m.getName() ); } );
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

	private void frames( int n ){
		for (int i = 0; i < n; i++){
			FxBudget.frame( Game.elapsed );
			WaterFX.frame();
			for (Group g : layers) g.update();
		}
	}

	private static int living( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.alive && c.isInstance( m )) n++;
			if (m instanceof Group) n += living( (Group) m, c );
		}
		return n;
	}

	private static float px( int cell ){
		return (cell % W + 0.5f) * 16;
	}

	private static float py( int cell ){
		return (cell / W + 0.5f) * 16;
	}

	@Test
	public void nothingTouchesTheGpuWhileItIsMade(){
		failOnGl();
		FxEmitter e = new FxEmitter();
		FxParticle p = new FxParticle().reset( 10, 10 ).look( FxFrames.TONGUE_M, 12 ).ramp( Element.FIRE.ramp, true );
		assertNotNull( p );
		e.add( p );
		FxLight.glow( 40, 40, 23, Element.FIRE.light, 1f ).flicker( FxLight.HEARTH ).persistent().reflect();
		FxLight.pool( 40, 48, 31, Element.FIRE.light, 1f ).life( 0, 0.2f, 0.4f );
		FxDecal.at( 20, 20, FxFrames.SCORCH_M, FxRecipes.SCORCH_COLOR, 0.7f, 1, 1, false ).revealCrop( FxDecal.FROM_LEFT, 0.5f );
		FxRing.ground( layers[0], 30, 30, 9, 23, 0.35f, 0xFFFFFF, 0.6f, true );
		Ripple r = WaterFX.ring( px( 9 + 5 * W ), py( 9 + 5 * W ), WaterFX.L );
		assertNotNull( r );
		WaterFX.splash( px( 10 + 6 * W ), py( 10 + 6 * W ), WaterFX.M, Element.NONE );
		WaterFX.droplets( 50, 50, 6, 30, 60, 100, 0xFFFFFF );
		WaterFX.wash( 9 + 5 * W, 0x880000, 0.5f, 1f );
		FxRecipes.impact( 60, 60, Element.ELECTRIC, 1f );
		FxRecipes.castBloom( 60, 60, Element.ARCANE );
		FxRecipes.bloom( 60, 60, 0xFFFFFF, 126, 0.8f, 0.6f );
		FxRecipes.scorch( 60, 60, FxRecipes.LARGE );
		FxRecipes.puff( 60, 60, Element.FIRE, 3 );
		FxRecipes.dust( 60, 60, 3 );
		CharSprite s = new CharSprite(){
			{
				texture( Assets.Effects.FX_SHEET );
				frame( 0, 0, 8, 8 );
			}
		};
		assertNotNull( Afterimage.of( s, 0xFFFFFF, 0.6f, 0.3f, 0, 0 ) );
		//and the first frames run without it too: the GPU is first touched where they are drawn
		frames( 3 );
	}

	@Test
	public void aDropFallsBouncesOnceAndSettles(){
		FxEmitter e = new FxEmitter();
		FxParticle p = ((FxParticle) e.recycle( FxParticle.class )).reset( 20, 20 ).look( FxFrames.PEBBLE_2 )
				.vel( 0, -40 ).gravity( 240 ).ground( 28, FxParticle.BOUNCE, 0.3f ).life( 5f );
		int counted = FxBudget.live( FxBudget.PARTICLES );
		assertTrue( counted >= 1 );
		float lowest = 0;
		boolean bounced = false;
		for (int i = 0; i < 120; i++){
			float vy = p.vy;
			e.update();
			lowest = Math.max( lowest, p.py );
			assertTrue( "never below its ground: " + p.py, p.py <= 28.0001f );
			if (vy > 0 && p.vy < 0) bounced = true;
		}
		assertTrue( bounced );
		assertEquals( 28f, p.py, 0.001f );
		assertEquals( 0f, p.vy, 0f );
		//slow: on whole pixels
		assertEquals( Math.round( p.x ), p.x, 0f );
		p.kill();
		assertEquals( counted - 1, FxBudget.live( FxBudget.PARTICLES ) );
	}

	@Test
	public void matterFadesInPaintedStepsAndLightSmoothly(){
		FxEmitter e = new FxEmitter();
		FxParticle m = ((FxParticle) e.recycle( FxParticle.class )).reset( 5, 5 ).look( FxFrames.PEBBLE_2 )
				.life( 1f ).alpha( 1f, 0, 0.4f );
		FxParticle l = ((FxParticle) e.recycle( FxParticle.class )).reset( 5, 5 ).look( FxFrames.MOTE_1 ).light( true )
				.life( 1f ).alpha( 1f, 0, 0.4f );
		HashSet<Float> steps = new HashSet<>();
		Game.elapsed = 0.02f;
		for (int i = 0; i < 48; i++){
			e.update();
			if (m.alive) steps.add( m.am );
		}
		for (float a : steps) assertTrue( "a painted step: " + a, a == 1f || a == 0.6f || a == 0.3f || a == 0f );
		assertTrue( steps.contains( 0.6f ) && steps.contains( 0.3f ) );
		assertTrue( l.am > 0 && l.am < 1 && l.am != 0.6f && l.am != 0.3f );
	}

	@Test
	public void aFlameShrinksThroughItsSizesAndFlowsDownItsRamp(){
		FxEmitter e = new FxEmitter();
		FxParticle p = ((FxParticle) e.recycle( FxParticle.class )).reset( 30, 30 ).look( FxFrames.TONGUE_L, 12 )
				.then( 0.55f, FxFrames.TONGUE_M ).then( 0.8f, FxFrames.TONGUE_S )
				.ramp( FxFactories.tongueRamp( Element.FIRE ), true ).life( 1f ).vel( 0, -10 ).gravity( -28 );
		Game.elapsed = 0.05f;
		assertEquals( 7f, p.width, 0 );
		for (int i = 0; i < 12; i++) e.update();
		assertEquals( "medium past 55%", 5f, p.width, 0 );
		for (int i = 0; i < 5; i++) e.update();
		assertEquals( "small past 80%", 3f, p.width, 0 );
		assertTrue( p.py < 30 );
		//the colour cooled from the white-hot first stop
		assertTrue( p.colorNow() != Element.FIRE.ramp.stop( 0 ) );
	}

	@Test
	public void theFactoriesAreCachedAndDrawNoGameRandom(){
		assertTrue( FxFactories.tongue( Element.FIRE ) == FxFactories.tongue( Element.FIRE ) );
		assertTrue( FxFactories.tongue( Element.FIRE ) != FxFactories.tongue( Element.HELL ) );
		assertTrue( FxFactories.mote( 0xFFFFFF, true ) != FxFactories.mote( 0xFFFFFF, false ) );
		assertTrue( FxFactories.tongue( Element.FIRE ).lightMode() );
		assertFalse( FxFactories.puff( Element.FIRE ).lightMode() );

		Random.pushGenerator( 31 );
		float expected = Random.Float();
		Random.popGenerator();
		Random.pushGenerator( 31 );
		FxEmitter e = new FxEmitter();
		e.pos( 40, 40, 16, 16 );
		Emitter.Factory[] all = { FxFactories.tongue( Element.FIRE ), FxFactories.lick( Element.FIRE ),
				FxFactories.ember( Element.FIRE ), FxFactories.spark( Element.ELECTRIC ), FxFactories.mote( 0xFF00FF, true ),
				FxFactories.glint( 0xFFFFFF ), FxFactories.puff( Element.SMOKE ), FxFactories.steam(),
				FxFactories.debris( 0x808080, 0x404040 ), FxFactories.droplet( 0xFFFFFF ), FxFactories.shard( true ) };
		for (Emitter.Factory f : all){
			FxBudget.frame( 1 / 60f );
			e.burst( f, 4 );
			for (int i = 0; i < 40; i++) e.update();
		}
		assertEquals( expected, Random.Float(), 0f );
		Random.popGenerator();
	}

	@Test
	public void aDropOnTheWaterRingsAndOneOnTheFloorLeavesAWetMark(){
		int water = 10 + 6 * W, dry = 3 + 3 * W;
		Group air = layers[GameScene.Layer.EFFECTS.ordinal()];
		FxEmitter e = new FxEmitter();
		air.add( e );
		((FxParticle) e.recycle( FxParticle.class )).reset( px( water ), py( water ) - 10 ).look( FxFrames.DROP_2 )
				.gravity( 240 ).ground( py( water ), FxParticle.RING, 0 ).landDry( FxParticle.WET ).life( 3 );
		((FxParticle) e.recycle( FxParticle.class )).reset( px( dry ), py( dry ) - 10 ).look( FxFrames.DROP_2 )
				.gravity( 240 ).ground( py( dry ), FxParticle.RING, 0 ).landDry( FxParticle.WET ).life( 3 );
		frames( 15 );
		Group surface = layers[GameScene.Layer.SURFACE.ordinal()];
		Group floor = layers[GameScene.Layer.FLOOR.ordinal()];
		assertTrue( living( surface, Ripple.class ) >= 1 );
		assertEquals( 1, living( floor, FxDecal.class ) );
		//the wet mark is gone in 0.8 s
		frames( 30 );
		assertEquals( 0, living( floor, FxDecal.class ) );
	}

	@Test
	public void ringsStepThroughTheirPaintedFramesAndTheMiddlingHaveASecond(){
		int water = 10 + 6 * W;
		Ripple m = WaterFX.ring( px( water ), py( water ), WaterFX.M );
		Group surface = layers[GameScene.Layer.SURFACE.ordinal()];
		assertEquals( "a middling ring and its second", 2, living( surface, Ripple.class ) );
		assertEquals( 9f, m.width, 0 );
		assertTrue( "mirrored on water", m.flipVertical );
		Game.elapsed = 0.02f;
		float widest = 0;
		Ripple second = null;
		for (Gizmo g : surface.membersView()) if (g != m && g instanceof Ripple && g.alive) second = (Ripple) g;
		assertNotNull( second );
		assertFalse( "the second waits 0.12 s", second.visible );
		for (int i = 0; i < 29; i++){
			frames( 1 );
			widest = Math.max( widest, m.width );
		}
		assertEquals( 23f, widest, 0 );
		assertTrue( second.visible );
		frames( 10 );
		assertEquals( 0, living( surface, Ripple.class ) );
		//a small one alone, a large one to 41 px
		WaterFX.frame();
		WaterFX.ring( px( water ), py( water ), WaterFX.S );
		assertEquals( 1, living( surface, Ripple.class ) );
		//the caller's nudge is kept
		Ripple nudged = GameScene.ripple( water );
		float y0 = nudged.y;
		nudged.y -= 8;
		frames( 2 );
		assertEquals( y0 - 8, nudged.y, 1.0001f );
	}

	@Test
	public void thePoolsKeepToTheirCaps(){
		int water = 10 + 6 * W;
		for (int i = 0; i < 1000; i++){
			WaterFX.drop( px( water ) + i % 5, py( water ), (i % 3) / 2.5f, Element.NONE );
			if (i % 7 == 0) WaterFX.frame();
		}
		assertTrue( "rings " + FxRing.live(), FxRing.live() <= FxBudget.RINGS_CAP );
		for (int i = 0; i < 200; i++) FxDecal.at( 40, 40, FxFrames.SPLAT[0], 0x880000, 0.8f, 5f, 1f, false );
		frames( 12 );
		assertTrue( "decals " + FxDecal.live(), FxDecal.live() <= FxBudget.DECALS_CAP );
		for (int i = 0; i < 100; i++) FxLight.glow( 40, 40, 15, 0xFFFFFF, 1f ).life( 0, 1f, 1f );
		assertTrue( FxLight.live() <= FxBudget.LIGHTS_CAP );
		for (int i = 0; i < 40; i++) FxLight.glow( 40, 40, 15, 0xFFFFFF, 1f ).persistent();
		assertTrue( FxLight.live() <= FxBudget.LIGHTS_CAP );
		assertEquals( FxBudget.LIGHTS_PERSISTENT, FxLight.livePersistent() );
		for (int f = 0; f < 10; f++){
			FxBudget.frame( 1 / 60f );
			WaterFX.droplets( 40, 40, 10, 30, 60, 100, 0xFFFFFF );
		}
		assertTrue( "droplets " + FxBudget.live( FxBudget.DROPLETS ), FxBudget.live( FxBudget.DROPLETS ) <= FxBudget.DROPLETS_CAP );
		CharSprite s = new CharSprite(){
			{
				texture( Assets.Effects.FX_SHEET );
				frame( 0, 0, 8, 8 );
			}
		};
		for (int i = 0; i < 40; i++) Afterimage.of( s, 0xFFFFFF, 0.5f, 1f, 0, 0 );
		assertEquals( FxBudget.AFTERIMAGES_CAP, Afterimage.live() );
		for (int i = 0; i < 40; i++) WaterFX.wash( water, 0x880000, 0.5f, 2f );
		assertTrue( living( layers[GameScene.Layer.SURFACE.ordinal()], WaterFX.Wash.class ) <= WaterFX.WASHES );
	}

	@Test
	public void aLightShowsWhereTheHeroSeesItAndFullerAtNight(){
		int cell = 4 + 4 * W;
		FxLight t = FxLight.glow( px( cell ), py( cell ) - 6, 23, 0xFFFFFF, 1f );
		FxLight p = FxLight.glow( px( cell ), py( cell ) - 6, 23, 0xFFFFFF, 1f ).persistent();
		frames( 1 );
		float dayT = t.am, dayP = p.am;
		assertEquals( FxLight.GLOW_CAP * Fx.nightMul( Fx.DAY_TRANSIENT, 0 ), dayT, 1e-4 );
		assertEquals( FxLight.GLOW_CAP * Fx.nightMul( Fx.DAY_PERSISTENT, 0 ), dayP, 1e-4 );
		//out of sight: the transient goes dark, the persistent keeps 0.8 of itself where explored
		Arrays.fill( level.heroFOV, false );
		frames( 1 );
		assertEquals( 0f, t.am, 0 );
		assertFalse( t.visible );
		assertEquals( dayP * 0.8f, p.am, 1e-4 );
		//a pool is capped lower, a core higher
		Arrays.fill( level.heroFOV, true );
		FxLight pool = FxLight.pool( 50, 50, 31, 0xFFFFFF, 1f ).day( 1f );
		FxLight core = FxLight.glow( 50, 50, 11, 0xFFFFFF, 1f ).day( 1f ).core();
		frames( 1 );
		assertEquals( FxLight.POOL_CAP, pool.am, 1e-4 );
		assertEquals( 1f, core.am, 1e-4 );
		//a transient lives its life and goes
		FxLight brief = FxLight.glow( 50, 50, 11, 0xFFFFFF, 1f ).life( 0.05f, 0.05f, 0.1f );
		frames( 8 );
		assertFalse( brief.alive );
		//a hearth flickers within 12%
		FxLight hearth = FxLight.glow( 50, 50, 23, 0xFFFFFF, 1f ).day( 1f ).flicker( FxLight.HEARTH );
		Game.elapsed = 0.05f;
		for (int i = 0; i < 400; i++){
			frames( 1 );
			assertTrue( "flicker " + hearth.am, hearth.am >= FxLight.GLOW_CAP * 0.88f - 1e-4 && hearth.am <= FxLight.GLOW_CAP + 1e-4 );
		}
	}

	@Test
	public void aLightOverWaterThrowsAReflectionOffTheShoreLip(){
		//a glow standing two rows above the pool's top row, over its west edge column
		int ground = 8 + 2 * W;
		FxLight l = FxLight.glow( px( ground ), py( ground ) - 6, 23, 0xFFE0A0, 1f ).day( 1f ).reflect();
		frames( 2 );
		assertEquals( 1, FxLight.reflecting() );
		com.watabou.noosa.Image r = null;
		for (Gizmo g : layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
			if (g instanceof com.watabou.noosa.Image && !(g instanceof FxLight) && g.visible) r = (com.watabou.noosa.Image) g;
		}
		assertNotNull( r );
		//on the pool's first water row (4), off its dry west lip and its dry top
		assertTrue( r.x >= 8 * 16 + 2 );
		assertEquals( 4 * 16 + 2f, r.y, 0 );
		assertTrue( r.am <= FxLight.POOL_CAP + 1e-4 );
		//at most six at once
		for (int i = 0; i < 12; i++) FxLight.glow( px( ground + 1 ), py( ground ) - 6, 23, 0xFFE0A0, 1f ).day( 1f ).reflect();
		frames( 2 );
		assertEquals( FxLight.REFLECTIONS, FxLight.reflecting() );
		l.putOut( 0.05f );
		frames( 3 );
		assertFalse( l.alive );
	}

	@Test
	public void theDisturbancesAreSafeOnAnyThreadAndReadAlikeByAll() throws Exception {
		Runnable stir = () -> {
			for (int i = 0; i < 10000; i++) WaterFX.disturb( i, i, 10, i % 2 == 0 );
		};
		Thread a = new Thread( stir ), b = new Thread( stir );
		a.start();
		b.start();
		for (int i = 0; i < 200; i++) WaterFX.frame();
		a.join();
		b.join();
		WaterFX.frame();
		float[] one = new float[WaterFX.BUS * 4], two = new float[WaterFX.BUS * 4];
		int n1 = WaterFX.disturbances( one ), n2 = WaterFX.disturbances( two );
		assertEquals( n1, n2 );
		assertTrue( n1 <= WaterFX.BUS );
		assertTrue( Arrays.equals( one, two ) );
		//a ring disturbs the water round it: the frame after, a drainer sees it
		WaterFX.frame();
		int water = 10 + 6 * W;
		WaterFX.ring( px( water ), py( water ), WaterFX.L );
		WaterFX.disturb( 1, 2, 3, true );
		WaterFX.frame();
		float[] out = new float[WaterFX.BUS * 4];
		assertEquals( 2, WaterFX.disturbances( out ) );
		assertEquals( 64f, out[2], 0 );
		assertEquals( 1f, out[7], 0 );
		WaterFX.frame();
		assertEquals( 0, WaterFX.disturbances( out ) );
	}

	@Test
	public void aSlideOfTheWindowCarriesThemAlong(){
		//the overworld's rebase slides every picture of a layer (GameScene.shiftWorldVisuals): the
		//kit's place themselves each frame, so they must take the slide in rather than snap back
		FxEmitter e = new FxEmitter();
		layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
		FxParticle p = ((FxParticle) e.recycle( FxParticle.class )).reset( 40, 40 ).look( FxFrames.PEBBLE_2 )
				.life( 5f ).ground( 50, FxParticle.SETTLE, 0 );
		FxLight l = FxLight.glow( 60, 60, 15, 0xFFFFFF, 1f ).day( 1f );
		FxDecal d = FxDecal.at( 80, 80, FxFrames.SCORCH_S, 0x202020, 0.7f, 5f, 1f, false );
		FxRing r = FxRing.ground( layers[GameScene.Layer.FLOOR.ordinal()], 30, 30, 9, 41, 2f, 0xFFFFFF, 0.6f, false );
		frames( 1 );
		float[] before = { p.px, l.centerX(), d.x, r.centerX() };
		for (com.watabou.noosa.Visual v : new com.watabou.noosa.Visual[]{ p, l, d, r }){
			v.x -= 64;
			v.y += 32;
		}
		frames( 2 );
		assertEquals( before[0] - 64, p.px, 0.01f );
		assertEquals( before[1] - 64, l.centerX(), 0.01f );
		assertEquals( before[2] - 64, d.x, 0.01f );
		assertEquals( before[3] - 64, r.centerX(), 0.01f );
	}

	@Test
	public void nothingShowsOnTheWaterOffTheLevel(){
		assertFalse( WaterFX.onWater( -5, 10 ) );
		assertFalse( WaterFX.onWater( 3 * 16, 3 * 16 ) );
		assertTrue( WaterFX.onWater( px( 10 + 6 * W ), py( 10 + 6 * W ) ) );
		assertEquals( Element.Liquid.CAVE, WaterFX.liquid() );
		Dungeon.level = null;
		assertFalse( WaterFX.onWater( 10, 10 ) );
	}
}

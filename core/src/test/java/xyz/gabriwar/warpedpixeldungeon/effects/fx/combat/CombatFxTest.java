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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.CheckedCell;
import xyz.gabriwar.warpedpixeldungeon.effects.DarkBlock;
import xyz.gabriwar.warpedpixeldungeon.effects.IceBlock;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.ShieldHalo;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.Surprise;
import xyz.gabriwar.warpedpixeldungeon.effects.TargetedCell;
import xyz.gabriwar.warpedpixeldungeon.effects.TorchHalo;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
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
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The characters-and-combat area, headless (stand-in layers for the game's scene, a GPU that says
 * yes): its numbers and curves; nothing touches the GPU while its pictures are made; a blow's
 * pictures and every state's look draw nothing of the game's Random, and a splash exactly one
 * draw, at most 24 particles, each splash keeping its own way; a push keeps its 0.15 s and the frame
 * its callback runs on; a recoil moves the sprite on the screen, never its x and y; every look lets
 * go of its pictures as its state goes (or its sprite is killed); a burning body keeps to its cap
 * and only the first six burning bodies are lit.
 */
public class CombatFxTest {

	private static final int W = 16;

	private GL20 savedGl, savedGl20;
	private Camera savedCam;
	private float savedElapsed;
	private Level savedLevel;
	private Group[] layers;
	private Group stage;

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
		yesGl();
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		Camera.main.fullScreen = true;
		FxBudget.reset();
		fullTier();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		Afterimage.reset();
		WaterFX.sceneCreated();
		Emitter.freezeEmitters = false;
		FxRandom.seedForTests( 7 );
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		Dungeon.level = room();
		stage = new Group();
		Game.elapsed = 1 / 30f;
		Looks.Burning.rich.set( 0 );
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		FxBudget.reset();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		Afterimage.reset();
		WaterFX.sceneCreated();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Camera.main = savedCam;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
	}

	//the budget's governor back at FULL (an earlier test may have stepped it down)
	private static void fullTier(){
		try {
			java.lang.reflect.Method m = FxBudget.class.getDeclaredMethod( "resetGovernor" );
			m.setAccessible( true );
			m.invoke( null );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	//a room with a pool (columns 9-12, rows 4-8) and a wall (column 2); all of it seen
	private static Level room(){
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
		for (int y = 4; y <= 8; y++) for (int x = 9; x <= 12; x++) l.map[x + y * W] = Terrain.WATER;
		for (int y = 0; y < W; y++) l.map[2 + y * W] = Terrain.WALL;
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		l.visited = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		Arrays.fill( l.visited, true );
		return l;
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	//a GPU that answers yes: shaders compile and link, pictures can be drawn
	private static void yesGl(){
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
	}

	private static void failOnGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, a) -> { throw new AssertionError( "GL call while building: " + m.getName() ); } );
	}

	//a sprite on the stage, linked by hand to its character
	private CharSprite sprite( CharSprite s, Rat ch, int cell ){
		ch.state = ch.PASSIVE;
		ch.pos = cell;
		s.ch = ch;
		ch.sprite = s;
		stage.add( s );
		s.place( cell );
		s.visible = true;
		return s;
	}

	private CharSprite rat( int cell ){
		return sprite( new RatSprite(), new Rat(), cell );
	}

	//frames of the stage and every layer
	private void frames( int n ){
		for (int i = 0; i < n; i++){
			//the frame time a phone at its rate reports (the governor stays at FULL), whatever step
			//the frames take
			FxBudget.frame( 1 / 60f );
			WaterFX.frame();
			stage.update();
			for (Group g : layers) g.update();
		}
	}

	private static int living( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.alive && c.isInstance( m )) n++;
			if (m instanceof Group && m.alive) n += living( (Group) m, c );
		}
		return n;
	}

	//living kit particles on a body (within the douse's reach of its middle)
	private static int near( Group g, float cx, float cy ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m instanceof FxParticle && m.alive && Math.abs( ((FxParticle) m).x - cx ) <= Looks.Burning.DOUSED_X
					&& Math.abs( ((FxParticle) m).y - cy ) <= Looks.Burning.DOUSED_Y) n++;
			if (m instanceof Group && m.alive) n += near( (Group) m, cx, cy );
		}
		return n;
	}

	private int particles(){
		int n = 0;
		for (Group g : layers) n += living( g, FxParticle.class );
		return n;
	}

	//how many of the game's Random a run draws: the next number after it, against a fresh
	//generator of the same seed drawn k times
	private static int draws( Runnable run ){
		Random.pushGenerator( 1234 );
		run.run();
		float next = Random.Float();
		Random.popGenerator();
		for (int k = 0; k < 64; k++){
			Random.pushGenerator( 1234 );
			for (int i = 0; i < k; i++) Random.Float();
			float f = Random.Float();
			Random.popGenerator();
			if (f == next) return k;
		}
		return -1;
	}

	// ------------------------------------------------------------------ numbers

	@Test
	public void itsNumbersAndCurves(){
		//a blow's style by its sound, and which way it went
		assertEquals( HitFx.SLASH, HitFx.style( Assets.Sounds.HIT_SLASH ) );
		assertEquals( HitFx.STAB, HitFx.style( Assets.Sounds.HIT_STAB ) );
		assertEquals( HitFx.CRUSH, HitFx.style( Assets.Sounds.HIT_CRUSH ) );
		assertEquals( HitFx.ARROW, HitFx.style( Assets.Sounds.HIT_ARROW ) );
		assertEquals( HitFx.MAGIC, HitFx.style( Assets.Sounds.HIT_MAGIC ) );
		assertEquals( HitFx.PARRY, HitFx.style( Assets.Sounds.HIT_PARRY ) );
		assertEquals( HitFx.STRONG, HitFx.style( Assets.Sounds.HIT_STRONG ) );
		assertEquals( HitFx.HIT, HitFx.style( Assets.Sounds.HIT ) );
		assertEquals( HitFx.HIT, HitFx.style( null ) );
		assertEquals( HitFx.HIT, HitFx.styleOf( new Rat() ) );
		//the light a blow throws: white, a third of the way to the blood
		assertEquals( 0xFFA6A6, HitFx.bloomColor( 0xFFFF0000 ) );
		assertEquals( 0xFFFFFF, HitFx.bloomColor( 0xFFFFFFFF ) );
		assertEquals( 0, HitFx.octant( 1, 0 ) );
		assertEquals( 1, HitFx.octant( 1, 1 ) );
		assertEquals( 2, HitFx.octant( 0, 1 ) );
		assertEquals( 4, HitFx.octant( -1, 0 ) );
		assertEquals( 6, HitFx.octant( 0, -1 ) );
		assertEquals( 7, HitFx.octant( 1, -1 ) );
		assertTrue( HitFx.heavy( 10, 30 ) );
		assertFalse( HitFx.heavy( 9, 30 ) );
		assertFalse( HitFx.heavy( 0, 30 ) );

		//what splashes as what
		assertEquals( Spray.LIQUID, Spray.kind( 0xFFBB0000 ) );
		assertEquals( Spray.LIQUID, Spray.kind( 0xFF44AA22 ) );
		assertEquals( Spray.LIQUID, Spray.kind( 0xB2D6FF ) );
		assertEquals( Spray.LIQUID, Spray.kind( 0x000000 ) );
		assertEquals( Spray.LIQUID, Spray.kind( 0xFFFFEA80 ) );
		assertEquals( Spray.DUST, Spray.kind( 0xFFcdcdb7 ) );
		assertEquals( Spray.DUST, Spray.kind( 0x555555 ) );
		assertEquals( Spray.SPARK, Spray.kind( 0xFFFFFF ) );
		assertEquals( Spray.SPARK, Spray.kind( 0xCC99FFFF ) );

		//a fire catching: 0 to 0.6 in 0.1 s, settling to 0.5 by 0.3 s
		assertEquals( 0f, Looks.Burning.level( 0 ), 1e-6f );
		assertEquals( 0.6f, Looks.Burning.level( 0.1f ), 1e-4f );
		assertEquals( 0.5f, Looks.Burning.level( 0.3f ), 1e-4f );
		assertEquals( 0.5f, Looks.Burning.level( 5f ), 1e-6f );
		for (float t = 0; t < 1; t += 0.01f){
			float l = Looks.Burning.level( t );
			assertTrue( l >= 0 && l <= Looks.Burning.PEAK + 1e-6f );
		}

		//a shiver: a pixel either way at 8 Hz for 0.2 s every 2 s, nothing between
		int changes = 0, last = Looks.Cold.shiver( 0 );
		for (float t = 0; t < 0.2f; t += 1 / 120f){
			int s = Looks.Cold.shiver( t );
			assertTrue( s == 1 || s == -1 );
			if (s != last) changes++;
			last = s;
		}
		assertTrue( "about 3 swings in 0.2 s at 8 Hz: " + changes, changes >= 2 && changes <= 4 );
		for (float t = 0.21f; t < 1.99f; t += 0.05f) assertEquals( 0, Looks.Cold.shiver( t ) );

		//breathing and beating lights stay in their bands; no light pulses past 4 Hz
		for (float t = 0; t < 4; t += 0.01f){
			assertTrue( DarkBlock.brightness( t ) >= DarkBlock.LOW - 1e-5f && DarkBlock.brightness( t ) <= DarkBlock.HIGH + 1e-5f );
			assertTrue( Looks.Healing.alpha( t ) >= Looks.Healing.LOW - 1e-5f && Looks.Healing.alpha( t ) <= Looks.Healing.HIGH + 1e-5f );
			float r = Looks.Marked.runeAlpha( t );
			assertTrue( r >= Looks.Marked.RUNE_ALPHA * 0.88f - 1e-5f && r <= Looks.Marked.RUNE_ALPHA * 1.12f + 1e-5f );
			assertTrue( TargetedCell.poolAlpha( t ) >= TargetedCell.POOL_LOW - 1e-5f
					&& TargetedCell.poolAlpha( t ) <= TargetedCell.POOL_HIGH + 1e-5f );
		}
		assertTrue( DarkBlock.HZ <= 4f && Looks.Healing.HZ <= 4f && Looks.Marked.RUNE_HZ <= 4f && TargetedCell.HZ <= 1f );

		//the telegraph's ring closes 23 -> 9 in the second half of each beat
		assertEquals( -1, TargetedCell.closing( 0.2f ) );
		assertEquals( 0, TargetedCell.closing( 0.55f ) );
		assertEquals( 3, TargetedCell.closing( 0.99f ) );
		for (float t = 0.5f, prev = -1; t < 1f; t += 0.01f){
			int c = TargetedCell.closing( t );
			assertTrue( c >= prev );
			prev = c;
		}

		//the ice, the chill
		assertEquals( 0f, IceBlock.tint( 0 ), 1e-6f );
		assertEquals( IceBlock.TINT, IceBlock.tint( 3 ), 1e-6f );
		assertEquals( 0.7f, IceBlock.TINT, 1e-6f );
		assertEquals( 0.15f, StateLooks.chillStrength( 0 ), 1e-6f );
		assertEquals( 0.35f, StateLooks.chillStrength( 10 ), 1e-6f );
		assertEquals( 0.35f, StateLooks.chillStrength( 99 ), 1e-6f );

		//whole pixels and whole scales: a wound's length, a search's squares, a drop of blood
		assertEquals( 2, Wound.stretch( 0.9f ) );
		assertEquals( 1, Wound.stretch( 0.3f ) );
		assertEquals( 45f, Wound.snap( 40 ), 1e-6f );
		assertEquals( -90f, Wound.snap( -100 ), 1e-6f );
		assertEquals( 0.35f, Wound.TIME_TO_FADE, 1e-6f );
		int[] sides = { CheckedCell.side( 0.5f ), CheckedCell.side( 0.3f ), CheckedCell.side( 0.2f ), CheckedCell.side( 0.05f ) };
		assertTrue( Arrays.equals( new int[]{ 16, 12, 8, 4 }, sides ) );
		assertTrue( Arrays.equals( new int[]{ 4, 3, 2, 1 },
				new int[]{ BloodParticle.side( 0.9f ), BloodParticle.side( 0.6f ), BloodParticle.side( 0.4f ), BloodParticle.side( 0.1f ) } ) );
		//a surprise holds, then fades in painted steps
		assertEquals( 1f, Surprise.alphaLeft( 0.5f ), 1e-6f );
		assertEquals( 0.6f, Surprise.alphaLeft( 0.15f ), 1e-6f );
		assertEquals( 0.3f, Surprise.alphaLeft( 0.1f ), 1e-6f );
		assertEquals( 0f, Surprise.alphaLeft( 0.05f ), 1e-6f );
		assertEquals( 90f, Surprise.quarter( 80 ), 1e-6f );

		//a shield's bubble by the sprite's size, its arc's eight steps
		assertEquals( 0, ShieldHalo.bubbleFor( 16, 16 ) );
		assertEquals( 1, ShieldHalo.bubbleFor( 24, 24 ) );
		assertEquals( 2, ShieldHalo.bubbleFor( 40, 40 ) );
		assertEquals( 0, ShieldHalo.arcStep( 0 ) );
		assertEquals( 7, ShieldHalo.arcStep( 0.39f ) );
		assertEquals( -1, ShieldHalo.arcStep( 1f ) );

		//vines grow in 0.2 s and withdraw in 0.15
		assertEquals( 0.5f, Looks.Rooted.grown( 0.1f, -1 ), 1e-5f );
		assertEquals( 1f, Looks.Rooted.grown( 3f, -1 ), 1e-6f );
		assertEquals( 0f, Looks.Rooted.grown( 3f, 0.15f ), 1e-6f );

		//a push stopped by a wall beyond where it lands
		Level l = Dungeon.level;
		assertTrue( Pushing.stopped( l, at( 4, 3 ), at( 3, 3 ) ) );
		assertFalse( Pushing.stopped( l, at( 5, 3 ), at( 6, 3 ) ) );
		assertFalse( Pushing.stopped( l, at( 5, 3 ), at( 5, 3 ) ) );
	}

	// ------------------------------------------------------------------ the GPU, the Random

	@Test
	public void nothingTouchesTheGpuWhileItIsMade(){
		CharSprite s = rat( at( 6, 6 ) );
		failOnGl();
		assertNotNull( new ShieldHalo( s ) );
		assertNotNull( new TorchHalo( s ) );
		assertNotNull( new Surprise() );
		assertNotNull( new Wound() );
		assertNotNull( new Spray( 0xBB0000, 0, 1 ) );
		assertNotNull( IceBlock.freeze( s ) );
		StateLooks looks = new StateLooks( s );
		for (CharSprite.State st : CharSprite.State.values()) looks.add( st );
		Splash.at( new PointF( 40, 40 ), 0xFFBB0000, 8 );
		HitFx.hit( s, 20, 100, 6, HitFx.SLASH, false );
		HitFx.hit( s, 20, 100, 0, HitFx.CRUSH, false );
		HitFx.dodged( s, 20, 100 );
		assertNotNull( LightFlash.at( 40, 40, xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames.Light.RIM_ARC_23[0], 0xFFFFFF, 0.5f, 0.15f ) );
		yesGl();
	}

	@Test
	public void aBlowDrawsNothingOfTheGamesRandom(){
		CharSprite s = rat( at( 6, 6 ) );
		Rat other = new Rat();
		for (int style = HitFx.HIT; style <= HitFx.STRONG; style++){
			final int st = style;
			assertEquals( "style " + style, 0, draws( () -> {
				s.hitFx( 40, 100, 1, st, true );
				s.hitFx( 40, 100, 0, st, false );
				s.hitFx( 40, 100, 9000, st, true );
				s.dodgeFx( 40, 100 );
				HitFx.styleOf( other );
				frames( 20 );
			} ) );
		}
		//its pictures are made at once: a star and the slash's crescent at least
		frames( 60 );
		int before = particles();
		s.hitFx( 40, 100, 1, HitFx.SLASH, false );
		assertTrue( particles() >= before + 2 );
		//a blocked blow's glint
		s.hitFx( 40, 100, 0, HitFx.SLASH, false );
		assertTrue( living( layers[GameScene.Layer.LIGHTS.ordinal()], LightFlash.class ) >= 1 );
	}

	@Test
	public void aBlowThrowsAMomentOfLightAndAFireGoesOutInWater(){
		CharSprite s = rat( at( 6, 6 ) );
		int before = FxLight.live();
		//a blow that lands: one light, gone within its 0.2 s
		s.hitFx( 40, 100, 1, HitFx.HIT, false );
		assertEquals( before + 1, FxLight.live() );
		frames( 15 );
		assertEquals( before, FxLight.live() );
		//blocked or missed: none
		s.hitFx( 40, 100, 0, HitFx.HIT, false );
		s.dodgeFx( 40, 100 );
		assertEquals( before, FxLight.live() );
		frames( 15 );

		//a burning rat standing in the pool, put out: its flames go that frame
		CharSprite w = rat( at( 10, 6 ) );
		w.add( CharSprite.State.BURNING );
		frames( 20 );
		float cx = w.x + w.width() / 2f, cy = w.y + w.height() / 2f;
		assertTrue( near( layers[GameScene.Layer.LIGHTS.ordinal()], cx, cy ) > 0 );
		w.remove( CharSprite.State.BURNING );
		frames( 1 );
		assertEquals( 0, near( layers[GameScene.Layer.LIGHTS.ordinal()], cx, cy ) );
	}

	@Test
	public void aSplashDrawsTheGamesRandomOnceAndThrowsAtMost24(){
		assertEquals( 1, draws( () -> Splash.at( new PointF( 60, 60 ), 0xFFBB0000, 100 ) ) );
		assertEquals( 1, draws( () -> Splash.at( new PointF( 60, 60 ), 1f, 0.5f, 0xFFcdcdb7, 9 ) ) );
		assertEquals( 1, draws( () -> Splash.at( new PointF( 60, 60 ), 1f, 0.5f, 0xFFFFFF, 9, 0.05f ) ) );
		CharSprite r = rat( at( 6, 6 ) );
		assertEquals( 1, draws( () -> Splash.around( r, 0x44AA22, 5 ) ) );
		assertEquals( 0, draws( () -> Splash.at( new PointF( 60, 60 ), 0xFFBB0000, 0 ) ) );
		//what they throw, on the render thread, draws none
		assertEquals( 0, draws( () -> frames( 60 ) ) );

		FxBudget.reset();
		FxDecal.reset();
		GameScene.useLayersForTests( null );
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Splash.at( new PointF( 60, 60 ), 0xFFBB0000, 100 );
		int most = 0;
		for (int f = 0; f < 30; f++){
			frames( 1 );
			most = Math.max( most, living( layers[GameScene.Layer.EFFECTS.ordinal()], FxParticle.class ) );
		}
		assertTrue( "a splash threw " + most, most > 0 && most <= Spray.MOST );
		//its drops landed: splats on the floor, at most three
		assertTrue( FxDecal.live() >= 1 && FxDecal.live() <= 3 );
	}

	@Test
	public void twoSplashesOfAColourInOneFrameKeepTheirOwnWays(){
		Splash.at( new PointF( 60, 60 ), 0f, 0.3f, 0xFFBB0000, 10 );
		Splash.at( new PointF( 60, 60 ), (float)Math.PI, 0.3f, 0xFFBB0000, 10 );
		frames( 1 );
		int east = 0, west = 0, mixed = 0;
		for (Gizmo g : layers[GameScene.Layer.EFFECTS.ordinal()].membersView()){
			if (!(g instanceof FxEmitter)) continue;
			int e = 0, w = 0;
			for (Gizmo p : ((FxEmitter) g).membersView()){
				if (!(p instanceof FxParticle) || !p.alive) continue;
				if (((FxParticle) p).vx > 0) e++;
				else w++;
			}
			if (e > 0 && w > 0) mixed++;
			if (e > 0 && w == 0) east++;
			if (w > 0 && e == 0) west++;
		}
		assertEquals( 0, mixed );
		assertEquals( 1, east );
		assertEquals( 1, west );
	}

	@Test
	public void theStatesLooksDrawNothingOfTheGamesRandom(){
		CharSprite s = rat( at( 6, 6 ) );
		//(healing, hearts, levitation and the electric crackle pour the Speck's and the spark's own
		//particles, an aura its Flare: not this area's; the ice and the butter splash off as they go,
		//each its one draw as ever, below)
		CharSprite.State[] mine = { CharSprite.State.BURNING, CharSprite.State.HALOMETHANEBURNING, CharSprite.State.HEATSTROKE,
				CharSprite.State.FROZEN, CharSprite.State.CHILLED, CharSprite.State.HYPOTHERMIA, CharSprite.State.DARKENED,
				CharSprite.State.MARKED, CharSprite.State.GLOWING, CharSprite.State.ILLUMINATED, CharSprite.State.TORCHLIT,
				CharSprite.State.BUTTER, CharSprite.State.POISONED, CharSprite.State.CORRODED, CharSprite.State.OOZED,
				CharSprite.State.ROOTED, CharSprite.State.BARKED, CharSprite.State.VERDANT, CharSprite.State.HERB,
				CharSprite.State.SHIELDED };
		assertEquals( 0, draws( () -> {
			for (CharSprite.State st : mine) s.add( st );
			frames( 60 );
			s.flash();
			frames( 10 );
			for (CharSprite.State st : mine){
				if (st != CharSprite.State.FROZEN && st != CharSprite.State.BUTTER) s.remove( st );
			}
			frames( 30 );
		} ) );
		//the ice melting, the butter going: a splash each, its one draw, as they always made
		assertEquals( 1, draws( () -> {
			s.remove( CharSprite.State.FROZEN );
			frames( 30 );
		} ) );
		assertEquals( 1, draws( () -> {
			s.remove( CharSprite.State.BUTTER );
			frames( 30 );
		} ) );
	}

	// ------------------------------------------------------------------ timing, the screen

	@Test
	public void aPushKeepsItsTimeAndTheFrameItsCallbackRunsOn(){
		Game.elapsed = 1 / 60f;
		Rat ch = new Rat();
		CharSprite s = sprite( new RatSprite(), ch, at( 4, 3 ) );
		final int[] fired = { -1 };
		final int[] frame = { 0 };
		//thrown from where it stands, as a knockback is (its cell changes as it lands)
		Pushing p = new Pushing( ch, at( 4, 3 ), at( 3, 3 ), () -> fired[0] = frame[0] );
		ch.pos = at( 3, 3 );
		p.new Effect();
		//the frame it lands on, counted as the push counts: its delay summed until it reaches 0.15
		float delay = 0;
		int expected = 0;
		while ((delay += Game.elapsed) < 0.15f) expected++;
		float x0 = s.x;
		for (frame[0] = 0; frame[0] < 30 && fired[0] < 0; frame[0]++){
			stage.update();
			for (Group g : layers) g.update();
		}
		assertEquals( expected, fired[0] );
		PointF end = s.worldToCamera( at( 3, 3 ) );
		assertEquals( end.x, s.x, 1e-4f );
		assertEquals( end.y, s.y, 1e-4f );
		assertTrue( x0 != s.x );
		//stopped by the wall beyond: squashed into it on the screen only, its x and y the cell's
		int[] off = new int[2];
		s.screenOffset( off );
		assertEquals( -1, off[0] );
		assertEquals( 0, off[1] );
		assertEquals( end.x, s.x, 1e-4f );
	}

	/** A sprite whose matrix can be read. */
	private static final class Probe extends RatSprite {
		float screenX(){
			updateMatrix();
			return matrix[12];
		}
	}

	@Test
	public void aRecoilMovesTheSpriteOnTheScreenNeverItsXOrY(){
		Probe s = (Probe) sprite( new Probe(), new Rat(), at( 6, 6 ) );
		float x = s.x, y = s.y;
		assertEquals( x, s.screenX(), 1e-4f );
		//struck from the west: a pixel east, for 0.08 s
		s.hitFx( s.x - 30, s.y + s.height() / 2f, 1, HitFx.HIT, false );
		assertEquals( x, s.x, 0 );
		assertEquals( y, s.y, 0 );
		assertEquals( x + 1, s.screenX(), 1e-4f );
		frames( 4 );
		assertEquals( x, s.x, 0 );
		assertEquals( y, s.y, 0 );
		assertEquals( x, s.screenX(), 1e-4f );
		//a heavy one: two pixels
		s.hitFx( s.x - 30, s.y + s.height() / 2f, 9000, HitFx.HIT, false );
		assertEquals( x + 2, s.screenX(), 1e-4f );
		assertEquals( x, s.x, 0 );
		//a shiver: across and back, x untouched
		s.add( CharSprite.State.HYPOTHERMIA );
		frames( 6 );
		boolean shivered = false;
		for (int f = 0; f < 70; f++){
			frames( 1 );
			if (s.screenX() != x) shivered = true;
			assertEquals( x, s.x, 0 );
		}
		assertTrue( shivered );
		s.remove( CharSprite.State.HYPOTHERMIA );
		frames( 2 );
		assertEquals( x, s.screenX(), 1e-4f );
	}

	// ------------------------------------------------------------------ letting go, caps

	private int pouring(){
		int n = 0;
		for (Group g : layers){
			for (Gizmo m : g.membersView()){
				if (m instanceof FxEmitter && m.alive && ((FxEmitter) m).on) n++;
			}
		}
		return n;
	}

	@Test
	public void everyLookLetsGoOfItsPicturesAsItsStateGoes(){
		CharSprite s = rat( at( 6, 6 ) );
		Fx.air();
		Fx.glowAir();
		int holders = pouring();
		for (CharSprite.State st : CharSprite.State.values()){
			if (st == CharSprite.State.PARALYSED || st == CharSprite.State.INVISIBLE) continue;
			if (st == CharSprite.State.AURA) s.aura( 0xFFD060, 6 );
			else s.add( st );
			frames( 15 );
			s.remove( st );
			frames( 40 );
			assertEquals( st + ": a look left", 0, s.looksCount() );
			assertEquals( st + ": an emitter left pouring", holders, pouring() );
			assertEquals( st + ": a light left", 0, FxLight.live() );
		}
		//every state at once, then the sprite killed: everything let go of at once
		for (CharSprite.State st : CharSprite.State.values()){
			if (st == CharSprite.State.AURA) s.aura( 0xFFD060, 6 );
			else s.add( st );
		}
		frames( 15 );
		assertTrue( s.looksCount() > 10 );
		assertTrue( FxLight.live() > 0 );
		s.kill();
		assertEquals( 0, s.looksCount() );
		frames( 40 );
		assertEquals( 0, FxLight.live() );
		assertEquals( holders, pouring() );
	}

	@Test
	public void aBurningBodyKeepsToItsCapAndOnlyTheFirstSixAreLit(){
		CharSprite s = rat( at( 6, 6 ) );
		s.add( CharSprite.State.BURNING );
		frames( 30 );
		int most = 0;
		float sum = 0;
		int n = 0;
		for (int f = 0; f < 90; f++){
			frames( 1 );
			int p = particles();
			most = Math.max( most, p );
			sum += p;
			n++;
		}
		assertTrue( "on average " + sum / n, sum / n <= Looks.Burning.CAP );
		assertTrue( "at most " + most, most <= Looks.Burning.CAP + 4 );
		assertEquals( 2, FxLight.live() );
		s.remove( CharSprite.State.BURNING );
		frames( 30 );

		CharSprite[] many = new CharSprite[8];
		for (int i = 0; i < many.length; i++){
			many[i] = rat( at( 4 + i, 12 ) );
			many[i].add( CharSprite.State.BURNING );
		}
		frames( 10 );
		assertEquals( Looks.Burning.RICH, Looks.Burning.rich.get() );
		assertEquals( 2 * Looks.Burning.RICH, FxLight.live() );
		for (CharSprite c : many) c.remove( CharSprite.State.BURNING );
		frames( 30 );
		assertEquals( 0, Looks.Burning.rich.get() );
		assertEquals( 0, FxLight.live() );
	}

	// ------------------------------------------------------------------ the review's fixes

	//the budget's governor at a tier (as a struggling phone's would step it)
	private static void tier( int t ){
		try {
			java.lang.reflect.Field f = FxBudget.class.getDeclaredField( "tier" );
			f.setAccessible( true );
			f.setInt( null, t );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	private FxLight lightNear( float x, float y ){
		for (Gizmo g : layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
			if (g instanceof FxLight && g.alive && Math.abs( ((FxLight) g).centerX() - x ) < 1f
					&& Math.abs( ((FxLight) g).centerY() - y ) < 1f) return (FxLight) g;
		}
		return null;
	}

	@Test
	public void heldLightsNeverPutOutOneAnotherNorDoBlows(){
		tier( FxBudget.MINIMAL );
		int cap = FxBudget.lightCap();
		//only as many burning bodies lit as leave the cap room
		assertTrue( Looks.Burning.rich() <= cap / 4 );

		//the cap filled by held lights, one a rat, and one more rat asking
		CharSprite[] rats = new CharSprite[cap + 1];
		HeldLight[] held = new HeldLight[cap + 1];
		for (int i = 0; i <= cap; i++){
			rats[i] = rat( at( 3 + i % 12, 1 + 2 * (i / 12) ) );
			held[i] = new HeldLight( false, 15, 0xFF8040, FxLight.STEADY, false );
		}
		float dt = 1 / 60f;
		for (int i = 0; i < cap; i++) held[i].update( rats[i], 0, 0, 0.5f, dt );
		assertEquals( cap, FxLight.live() );
		//the one more asks and blows land on every rat: none of the held lights goes out, and no
		//blow throws a light into a full cap
		for (int f = 0; f < 60; f++){
			tier( FxBudget.MINIMAL );
			for (int i = 0; i <= cap; i++) held[i].update( rats[i], 0, 0, 0.5f, dt );
			rats[f % cap].hitFx( 0, 0, 1, HitFx.HIT, false );
			frames( 1 );
			for (int i = 0; i < cap; i++) assertTrue( "held light " + i + " went out at frame " + f, held[i].held() );
			assertFalse( held[cap].held() );
			assertEquals( cap, FxLight.live() );
		}
		held[cap].putOut( 0f );

		//a light from elsewhere takes a held one's place; once there is room again, that one comes
		//back after RETRY and fades in over BACK_IN instead of popping on
		FxLight.glow( 200, 200, 15, 0xFFFFFF, 1f ).life( 0f, 0.05f, 0.05f );
		int lost = -1;
		for (int i = 0; i < cap; i++){
			held[i].update( rats[i], 0, 0, 0.5f, dt );
			if (!held[i].held()) lost = i;
		}
		assertTrue( lost >= 0 );
		float cx = rats[lost].x + rats[lost].width() / 2f, cy = rats[lost].y + rats[lost].height() / 2f;
		float gone = 0, first = -1, settled = -1;
		for (int f = 0; f < 90; f++){
			tier( FxBudget.MINIMAL );
			for (int i = 0; i < cap; i++) held[i].update( rats[i], 0, 0, 0.5f, dt );
			frames( 1 );
			FxLight l = held[lost].held() ? lightNear( cx, cy ) : null;
			if (l == null){
				gone += dt;
				continue;
			}
			if (first < 0) first = l.shown();
			settled = l.shown();
		}
		assertTrue( "back after " + gone, gone >= HeldLight.RETRY - 3 * dt && gone < HeldLight.RETRY + 0.1f );
		assertTrue( "faded in from " + first + " to " + settled, first > 0 && first < settled * 0.5f );
		for (HeldLight h : held) h.putOut( 0f );
		tier( FxBudget.FULL );
	}

	@Test
	public void aBurningBodyThatJumpsKeepsItsLightsAndLeavesNoneBehind(){
		//a burning rat, lit: its glow on the body and its pool at the feet
		CharSprite s = rat( at( 3, 3 ) );
		s.add( CharSprite.State.BURNING );
		frames( 20 );
		assertEquals( 2, FxLight.live() );
		//it is somewhere else from one frame to the next (a teleport, the world's window sliding,
		//a long frame mid-push): its lights are still its own, so it keeps the same two
		s.ch.pos = at( 12, 12 );
		s.place( at( 12, 12 ) );
		frames( 40 );
		assertEquals( 2, FxLight.live() );
		//the fire out: both go, none stays stuck on the body
		s.remove( CharSprite.State.BURNING );
		frames( 30 );
		assertEquals( 0, FxLight.live() );
	}

	@Test
	public void onlyAThrownCreatureSlamsIntoAWall(){
		Game.elapsed = 1 / 60f;
		//a split or a summon put down beside a wall: already on its cell as it slides out
		Rat ch = new Rat();
		CharSprite s = sprite( new RatSprite(), ch, at( 3, 3 ) );
		new Pushing( ch, at( 4, 3 ), at( 3, 3 ) ).new Effect();
		int[] off = new int[2];
		boolean squashed = false;
		for (int f = 0; f < 12; f++){
			stage.update();
			for (Group g : layers) g.update();
			s.screenOffset( off );
			if (off[0] != 0 || off[1] != 0) squashed = true;
		}
		assertFalse( squashed );

		//thrown from where it stands into the same wall: squashed into it
		Rat k = new Rat();
		CharSprite t = sprite( new RatSprite(), k, at( 4, 5 ) );
		new Pushing( k, at( 4, 5 ), at( 3, 5 ) ).new Effect();
		k.pos = at( 3, 5 );
		squashed = false;
		for (int f = 0; f < 12; f++){
			stage.update();
			for (Group g : layers) g.update();
			t.screenOffset( off );
			if (off[0] == -1) squashed = true;
		}
		assertTrue( squashed );
	}

	@Test
	public void aSmallShakeNeverCutsABigOneShort(){
		float saved = Game.timeTotal;
		try {
			//well past any shake an earlier test asked for
			Game.timeTotal = saved + 1000f;
			float big = Fx.shake( 2f, 0.25f );
			assertTrue( big > 0 );
			assertEquals( 0f, Fx.shake( 1f, 0.1f ), 0 );
			//most of the big one spent: a small one shakes again
			Game.timeTotal += 0.2f;
			assertTrue( Fx.shake( 1f, 0.1f ) > 0 );
			assertEquals( 1f, Fx.shakeLeft( 2f, 0.25f, 0.125f ), 1e-6f );
			assertEquals( 0f, Fx.shakeLeft( 2f, 0.25f, 0.25f ), 0 );
		} finally {
			Game.timeTotal = saved;
		}
		//a blow shakes the camera for a STRONG weapon with the hero in the fight, or a heavy blow
		//on the hero; the hero's heavy blows on a monster do not
		assertTrue( HitFx.shakes( HitFx.STRONG, false, true, false ) );
		assertTrue( HitFx.shakes( HitFx.HIT, true, true, true ) );
		assertFalse( HitFx.shakes( HitFx.SLASH, true, true, false ) );
		assertFalse( HitFx.shakes( HitFx.STRONG, true, false, false ) );
	}

	@Test
	public void theGamesOwnShakesAndTheEffectsNeverCutEachOtherShort(){
		float saved = Game.timeTotal;
		try {
			Game.timeTotal = saved + 2000f;
			//a boss's stomp through PixelScene.shake holds off a blow's small shake
			xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene.shake( 3f, 0.7f );
			assertEquals( 0f, Fx.shake( 1f, 0.1f ), 0 );
			//and once it has died down, the blow shakes
			Game.timeTotal += 0.69f;
			assertTrue( Fx.shake( 1f, 0.1f ) > 0 );

			//an effect's big shake is not cut short by a small one of the game's
			Game.timeTotal += 10f;
			assertTrue( Fx.shake( 4f, 0.5f ) > 0 );
			assertFalse( Fx.claimShake( 0.5f, 0.5f ) );
			Game.timeTotal += 0.5f;
			assertTrue( Fx.claimShake( 0.5f, 0.5f ) );
		} finally {
			Game.timeTotal = saved;
		}
	}

	@Test
	public void anIlluminatedBodyGlowsByDayAsPlainlyAsTheOldHalo(){
		CharSprite s = rat( at( 6, 6 ) );
		s.add( CharSprite.State.ILLUMINATED );
		frames( 60 );
		FxLight l = lightNear( s.x + s.width() / 2f, s.y + s.height() / 2f );
		assertNotNull( l );
		//the old TorchHalo showed 0.15 by day
		assertTrue( "shown " + l.shown(), l.shown() >= 0.13f );
		s.remove( CharSprite.State.ILLUMINATED );
		frames( 40 );
	}

	private int particlesAround( float x, float y, float r ){
		int n = 0;
		for (Group g : layers) n += around( g, x, y, r );
		return n;
	}

	private static int around( Group g, float x, float y, float r ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m instanceof FxParticle && m.alive && Math.abs( ((FxParticle) m).x - x ) <= r
					&& Math.abs( ((FxParticle) m).y - y ) <= r) n++;
			if (m instanceof Group && m.alive) n += around( (Group) m, x, y, r );
		}
		return n;
	}

	@Test
	public void aDousedBodyHissesOnTheWaterItSteppedInto(){
		//its cell the pool's, its sprite still mid-step on dry ground four cells off
		CharSprite w = rat( at( 6, 6 ) );
		w.ch.pos = at( 10, 6 );
		w.add( CharSprite.State.BURNING );
		frames( 20 );
		float wx = 10 * 16 + 8, wy = 6 * 16 + 14;
		int before = particlesAround( wx, wy, 8 );
		w.remove( CharSprite.State.BURNING );
		frames( 1 );
		assertTrue( particlesAround( wx, wy, 8 ) > before );
	}

	@Test
	public void aSurpriseKeepsToTheBudget(){
		CharSprite s = rat( at( 6, 6 ) );
		Fx.glowAir();
		int lights = living( layers[GameScene.Layer.LIGHTS.ordinal()], FxParticle.class );
		//past HARD: no crosses
		FxBudget.add( FxBudget.PARTICLES, FxBudget.hard() );
		Surprise.hit( s.ch );
		stage.update();
		assertEquals( lights, living( layers[GameScene.Layer.LIGHTS.ordinal()], FxParticle.class ) );
		FxBudget.add( FxBudget.PARTICLES, -FxBudget.hard() );
		frames( 30 );
		//room: all six, from a surprise recycled from the first (gone out hidden)
		lights = living( layers[GameScene.Layer.LIGHTS.ordinal()], FxParticle.class );
		FxBudget.frame( 1 / 60f );
		Surprise.hit( s.ch );
		stage.update();
		assertEquals( lights + 6, living( layers[GameScene.Layer.LIGHTS.ordinal()], FxParticle.class ) );
	}

	//a sprite that shows no blow: Char.attack's pictures off
	private static final class Blind extends RatSprite {
		@Override
		public void hitFx( float fromX, float fromY, int damage, int style, boolean heroInvolved ){ }

		@Override
		public void dodgeFx( float fromX, float fromY ){ }
	}

	//the hero, sword in hand, striking a sturdy rat a dozen times; the game's Random's next number
	//after it
	private long fight( boolean pictures ){
		Hero h = new Hero();
		Dungeon.hero = h;
		h.belongings.weapon = new Sword();
		h.pos = at( 5, 6 );
		//(a mob's sprite stands in for his, off the stage: only where it stands is read)
		h.sprite = pictures ? new RatSprite() : new Blind();
		h.sprite.place( h.pos );
		Rat r = new Rat();
		r.HP = r.HT = 9999;
		CharSprite rs = sprite( pictures ? new RatSprite() : new Blind(), r, at( 6, 6 ) );
		Random.pushGenerator( 4321 );
		try {
			for (int i = 0; i < 12; i++) h.attack( r );
			frames( 10 );
			return Random.Long();
		} finally {
			Random.popGenerator();
			rs.killAndErase();
		}
	}

	@Test
	public void aFightDrawsTheSameOfTheGamesRandomWithItsPicturesAsWithout(){
		Hero saved = Dungeon.hero;
		try {
			assertEquals( fight( false ), fight( true ) );
			//the style the call site reads draws none either
			Hero h = new Hero();
			h.belongings.weapon = new Sword();
			assertEquals( 0, draws( () -> assertEquals( HitFx.SLASH, HitFx.styleOf( h ) ) ) );
		} finally {
			Dungeon.hero = saved;
		}
	}
}

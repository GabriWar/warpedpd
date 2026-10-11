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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.fire;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.FiendFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ForgeCoals;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Freezing;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.HellFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.MagicFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Steam;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SteamParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.json.JSONArray;
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
import static org.junit.Assert.assertTrue;

/**
 * Fire and heat (effects/fx/fire): making the burning blobs draws as many of the game's random
 * numbers as before (a fire 1, a coal bed 0, steam 1) and burning, blasting and steaming none;
 * a bomb's burst stays within its cap; the fires' lights within theirs, the floor's glows within
 * theirs, the places' lights within theirs; a cell going out (or put out by frost) ends once;
 * the blasts' classes and crowding; the firedamp front's steps; a burst counted whole; a quench
 * read off its burst; nothing touching the GPU while it is made.
 */
public class FireHeatTest {

	private static final int W = 16;
	//the real frame time the budget is told: a phone keeping up (the governor stays at FULL)
	private static final float FAST_FRAME = 1 / 120f;
	//a phone falling behind (the governor steps down a tier a second)
	private static final float SLOW_FRAME = 1 / 10f;
	//the real frame time frames() tells the budget
	private float realFrame = FAST_FRAME;

	private GL20 savedGl, savedGl20;
	private float savedElapsed;
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
		stubGl();
		//the budget's tier back to FULL (a fast device's frames for longer than its steps up take)
		for (int i = 0; i < 1200; i++) FxBudget.frame( FAST_FRAME );
		FxBudget.reset();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		WaterFX.sceneCreated();
		FireField.sceneCreated();
		BurstWatch.reset();
		PlaceLight.reset();
		Reflection.reset();
		LightRef.reset();
		Explosion.forget();
		Emitter.freezeEmitters = false;
		FxRandom.seedForTests( 11 );
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		level = floor();
		Dungeon.level = level;
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 8 + 8 * W;
		Game.elapsed = 1 / 30f;
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		FxBudget.reset();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		WaterFX.sceneCreated();
		FireField.sceneCreated();
		BurstWatch.reset();
		PlaceLight.reset();
		Reflection.reset();
		LightRef.reset();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//a bare floor with a pool in its south-east corner (columns 11-14, rows 11-14), all of it seen
	private static Level floor(){
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
		for (int y = 11; y < 15; y++) for (int x = 11; x < 15; x++) l.map[x + y * W] = Terrain.WATER;
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		l.visited = new boolean[l.length()];
		l.mapped = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		Arrays.fill( l.visited, true );
		return l;
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	private static void failOnGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, a) -> { throw new AssertionError( "GL call while building: " + m.getName() ); } );
	}

	//the weather's sheet (the shimmer's rays) goes to the GPU when first drawn on: a stand-in
	private static void stubGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			return null;
		} );
	}

	//frames of the scene: the budget's, then every layer and the given emitters
	private void frames( int n, Emitter... emitters ){
		for (int i = 0; i < n; i++){
			FxBudget.frame( realFrame );
			for (Emitter e : emitters) e.update();
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

	//the game's random number after `n` drawn from a seed
	private static float after( long seed, int n ){
		Random.pushGenerator( seed );
		for (int i = 0; i < n; i++) Random.Float();
		float next = Random.Float();
		Random.popGenerator();
		return next;
	}

	private static <T extends Blob> T blob( Class<T> kind, int amount, int... cells ){
		T b = com.watabou.utils.Reflection.newInstance( kind );
		for (int c : cells) b.seed( Dungeon.level, c, amount );
		Dungeon.level.blobs.put( kind, b );
		return b;
	}

	// ------------------------------------------------------------------ the game's random numbers

	@Test
	public void makingTheBurningBlobsDrawsAsManyOfTheGamesRandomAsBefore(){
		float fire = after( 99, 1 ), coals = after( 99, 0 ), steam = after( 99, 1 );

		Random.pushGenerator( 99 );
		new BlobEmitter( blob( Fire.class, 6, at( 4, 4 ), at( 5, 4 ) ) );
		assertEquals( "a fire's use() draws one, as ever", fire, Random.Float(), 0f );
		Random.popGenerator();

		Random.pushGenerator( 99 );
		new BlobEmitter( blob( ForgeCoals.class, 124, at( 6, 6 ) ) );
		assertEquals( "a coal bed's draws none, as ever", coals, Random.Float(), 0f );
		Random.popGenerator();

		Random.pushGenerator( 99 );
		new BlobEmitter( blob( Steam.class, 30, at( 8, 4 ) ) );
		assertEquals( "steam's draws one, as ever", steam, Random.Float(), 0f );
		Random.popGenerator();
	}

	@Test
	public void burningBlastingAndSteamingDrawNoneOfTheGamesRandom(){
		BlobEmitter fire = new BlobEmitter( blob( Fire.class, 9, at( 3, 3 ), at( 4, 3 ), at( 3, 4 ), at( 4, 4 ), at( 11, 11 ) ) );
		BlobEmitter coals = new BlobEmitter( blob( ForgeCoals.class, 124, at( 7, 3 ) ) );
		BlobEmitter steam = new BlobEmitter( blob( Steam.class, 30, at( 9, 3 ) ) );
		layers[GameScene.Layer.LIGHTS.ordinal()].add( fire );
		layers[GameScene.Layer.LIGHTS.ordinal()].add( coals );
		layers[GameScene.Layer.EFFECTS.ordinal()].add( steam );
		float first = after( 7, 0 );
		Random.pushGenerator( 7 );
		FxEmitter blast = new FxEmitter();
		layers[GameScene.Layer.EFFECTS.ordinal()].add( blast );
		blast.pos( 100, 60 );
		blast.burst( BlastParticle.FACTORY, 30 );
		FxEmitter quench = new FxEmitter();
		layers[GameScene.Layer.EFFECTS.ordinal()].add( quench );
		quench.pos( 60, 100 );
		quench.burst( SteamParticle.FACTORY, SteamParticle.QUENCH );
		frames( 90 );
		assertEquals( first, Random.Float(), 0f );
		Random.popGenerator();
		assertTrue( "it burnt", living( layers[GameScene.Layer.LIGHTS.ordinal()], FxParticle.class ) > 0 );
	}

	// ------------------------------------------------------------------ caps

	@Test
	public void aBombsBurstStaysWithinItsCap(){
		Group lights = layers[GameScene.Layer.LIGHTS.ordinal()], effects = layers[GameScene.Layer.EFFECTS.ordinal()];
		FxEmitter blast = new FxEmitter();
		effects.add( blast );
		blast.pos( 100, 60 );
		int before = FxBudget.live( FxBudget.PARTICLES );
		blast.burst( BlastParticle.FACTORY, 30 );
		int peak = 0;
		for (int i = 0; i < 60; i++){
			frames( 1 );
			peak = Math.max( peak, FxBudget.live( FxBudget.PARTICLES ) - before );
		}
		assertTrue( "a bomb shows: " + peak, peak > 30 );
		assertTrue( "at most " + Explosion.CAP + ": " + peak, peak <= Explosion.CAP );
		assertEquals( "and its watch is gone", 0, living( lights, BurstWatch.class ) );
	}

	@Test
	public void theFiresLightsAndFloorGlowsStayWithinTheirCaps(){
		int[] cells = new int[100];
		for (int i = 0; i < 100; i++) cells[i] = at( 1 + i % 10, 1 + i / 10 );
		Class<?>[] kinds = { Fire.class, HellFire.class, FiendFire.class, MagicFire.class };
		Emitter[] ems = new Emitter[kinds.length];
		for (int k = 0; k < kinds.length; k++){
			@SuppressWarnings("unchecked")
			Blob b = blob( (Class<? extends Blob>) kinds[k], 10, cells );
			ems[k] = new BlobEmitter( b );
			layers[GameScene.Layer.LIGHTS.ordinal()].add( ems[k] );
		}
		for (int i = 0; i < 60; i++){
			frames( 1 );
			assertTrue( "fire lights " + FireField.fireLights(), FireField.fireLights() <= FireField.fireLightCap() );
		}
		assertTrue( "they light the place", FireField.fireLights() > 0 );
		Group floor = layers[GameScene.Layer.FLOOR.ordinal()];
		int fields = living( floor, FireField.class );
		assertEquals( kinds.length, fields );
		assertTrue( living( floor, FloorGlow.class ) <= FireField.FLOOR_GLOWS * fields );
		assertTrue( living( floor, FloorGlow.class ) > 0 );
	}

	@Test
	public void onlyTheNearestPlacesHoldLights(){
		PlaceLight[] places = new PlaceLight[12];
		for (int i = 0; i < places.length; i++){
			int c = at( 2 + i % 6, 6 + i / 6 );
			places[i] = new PlaceLight( c, 40 + i * 16, 100, 23, 40 + i * 16, 110, 15, 0xFFB060, 0.6f, FxLight.TORCH );
		}
		for (int f = 0; f < 10; f++){
			for (PlaceLight p : places) p.update();
			frames( 1 );
			assertTrue( PlaceLight.holding() <= PlaceLight.PLACES );
		}
		assertEquals( PlaceLight.PLACES, PlaceLight.holding() );
		//no longer updated (out of sight): their lights die by themselves
		frames( Math.round( (LightRef.HOLD + LightRef.FADE) * 30 ) + 5 );
		assertEquals( 0, PlaceLight.holding() );
	}

	@Test
	public void aFireIsLitAsOneBlobNotAsTiles(){
		//a 3x3 fire: one group, one glow and one pool on its middle
		int[] square = { at( 3, 3 ), at( 4, 3 ), at( 5, 3 ), at( 3, 4 ), at( 4, 4 ), at( 5, 4 ), at( 3, 5 ), at( 4, 5 ), at( 5, 5 ) };
		BlobEmitter em = new BlobEmitter( blob( Fire.class, 6, square ) );
		layers[GameScene.Layer.LIGHTS.ordinal()].add( em );
		frames( 15 );
		FireField field = field();
		assertEquals( 1, field.groups() );
		assertEquals( 2, field.lightsHeld() );
		int pools = 0;
		for (Gizmo g : layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
			if (!(g instanceof FxLight) || !g.alive) continue;
			FxLight l = (FxLight) g;
			assertEquals( "on the fire's middle", 4.5f * 16, l.centerX(), 0.01f );
			//sized to it: its pool the 63 px one
			if (l.height() < l.width()){
				pools++;
				assertEquals( FireField.GROUP_POOL[1], l.width(), 0.01f );
			}
		}
		assertEquals( 1, pools );
	}

	@Test
	public void aLargeFireIsLitInEvenPiecesWithinItsLights(){
		//a 10x6 fire: cut 3 across and 2 down into pieces of 3-4 cells, the 4 busiest lit
		int[] cells = new int[60];
		for (int i = 0; i < 60; i++) cells[i] = at( 1 + i % 10, 1 + i / 10 );
		BlobEmitter em = new BlobEmitter( blob( Fire.class, 6, cells ) );
		layers[GameScene.Layer.LIGHTS.ordinal()].add( em );
		frames( 15 );
		FireField field = field();
		assertEquals( 6, field.groups() );
		assertEquals( 2 * FireField.LIGHTS, field.lightsHeld() );
	}

	@Test
	public void onAStrugglingPhoneTheLightsHoldStill(){
		float savedTime = Game.timeTotal;
		try {
			//the governor stepped down to its lowest tier
			for (int i = 0; i < 100 && FxBudget.tier() != FxBudget.MINIMAL; i++) FxBudget.frame( SLOW_FRAME );
			assertEquals( FxBudget.MINIMAL, FxBudget.tier() );
			realFrame = SLOW_FRAME;
			//four fires of 2x2 apart from each other beside six torches, all near the hero
			int[] cells = new int[16];
			for (int i = 0; i < 16; i++){
				int patch = i / 4;
				cells[i] = at( 1 + (patch % 2) * 3 + i % 2, 1 + (patch / 2) * 3 + (i / 2) % 2 );
			}
			BlobEmitter em = new BlobEmitter( blob( Fire.class, 10, cells ) );
			layers[GameScene.Layer.LIGHTS.ordinal()].add( em );
			PlaceLight[] torches = new PlaceLight[6];
			for (int i = 0; i < torches.length; i++){
				int c = at( 7 + i, 10 );
				torches[i] = new PlaceLight( c, (7 + i) * 16 + 8, 168, 31, (7 + i) * 16 + 8, 180, 15, 0xFFB060, 0.6f, FxLight.TORCH );
			}
			Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
			//the game's clock runs (the holders' retries go by it)
			for (int f = 0; f < 30; f++){
				for (PlaceLight t : torches) t.update();
				frames( 1 );
				Game.timeTotal += Game.elapsed;
			}
			HashSet<Gizmo> first = litLights( lights );
			assertTrue( "the fire is lit", FireField.fireLights() > 0 );
			assertTrue( "and some torches", PlaceLight.holding() > 0 );
			//five seconds: the same lights all the while, within the tier's caps
			for (int f = 0; f < 150; f++){
				for (PlaceLight t : torches) t.update();
				frames( 1 );
				Game.timeTotal += Game.elapsed;
				assertTrue( "within the lights' cap", FxLight.live() <= FxBudget.lightCap() );
				//nobody's light taken for another's: each fire's glow and pool, each torch's, whole
				assertEquals( "every holder's lights whole", FireField.fireLights() + 2 * PlaceLight.holding(), FxLight.live() );
				assertTrue( "within the fires' cap", FireField.fireLights() <= FireField.fireLightCap() );
				assertTrue( "within the places'", PlaceLight.holding() <= PlaceLight.places() );
				assertEquals( "frame " + f + ": the same lights", first, litLights( lights ) );
			}
		} finally {
			Game.timeTotal = savedTime;
			realFrame = FAST_FRAME;
			for (int i = 0; i < 1200; i++) FxBudget.frame( FAST_FRAME );
		}
	}

	private static HashSet<Gizmo> litLights( Group g ){
		HashSet<Gizmo> out = new HashSet<>();
		for (Gizmo m : g.membersView()) if (m instanceof FxLight && m.alive) out.add( m );
		return out;
	}

	//the one fire field on the floor
	private FireField field(){
		for (Gizmo g : layers[GameScene.Layer.FLOOR.ordinal()].membersView()){
			if (g instanceof FireField && g.alive) return (FireField) g;
		}
		throw new AssertionError( "no fire field" );
	}

	@Test
	public void aFireGoneLeavesNothingBehind(){
		Fire fire = blob( Fire.class, 6, at( 3, 3 ), at( 4, 3 ), at( 3, 4 ), at( 4, 4 ) );
		BlobEmitter em = new BlobEmitter( fire );
		layers[GameScene.Layer.LIGHTS.ordinal()].add( em );
		frames( 30 );
		Group floor = layers[GameScene.Layer.FLOOR.ordinal()];
		assertEquals( 1, living( floor, FireField.class ) );
		assertTrue( living( floor, FloorGlow.class ) > 0 );
		assertTrue( FireField.fireLights() > 0 );
		//its emitter gone (the blob burnt out): its glows fade, its lights go, then it is let go
		em.killAndErase();
		frames( 45 );
		assertEquals( 0, living( floor, FireField.class ) );
		assertEquals( 0, living( floor, FloorGlow.class ) );
		assertEquals( 0, FireField.fireLights() );
	}

	@Test
	public void aLightKeptEveryFrameStillFlickers(){
		//a torch's light, kept every frame for three seconds: its flicker runs on (keeping it
		//never restarts its clock), within the torch's +-12%
		PlaceLight torch = new PlaceLight( at( 8, 7 ), 136, 116, 23, 136, 128, 15, 0xFFB060, 0.6f, FxLight.TORCH );
		float lo = Float.MAX_VALUE, hi = 0;
		for (int f = 0; f < 90; f++){
			torch.update();
			frames( 1 );
			if (f < 30) continue;
			FxLight glow = null;
			//the glow in the air: the torch's persistent light
			for (Gizmo g : layers[GameScene.Layer.LIGHTS.ordinal()].membersView()){
				if (g instanceof FxLight && g.alive && ((FxLight) g).isPersistent()) glow = (FxLight) g;
			}
			assertNotNull( "the torch is lit", glow );
			lo = Math.min( lo, glow.shown() );
			hi = Math.max( hi, glow.shown() );
		}
		assertTrue( "it flickers: " + lo + ".." + hi, hi - lo > 0.01f );
		assertTrue( "within a torch's swing: " + lo + ".." + hi, hi / lo < 1.12f / 0.88f + 0.01f );
	}

	@Test
	public void aFrozenGameKeepsItsLights(){
		PlaceLight torch = new PlaceLight( at( 8, 7 ), 136, 116, 23, 136, 128, 15, 0xFFB060, 0.6f, FxLight.TORCH );
		torch.update();
		frames( 2 );
		assertEquals( 1, PlaceLight.holding() );
		//a window open: nothing updates its holder, and the watch's clock stands with it
		float savedTime = Game.timeTotal;
		Game.timeTotal = 100;
		Emitter.freezeEmitters = true;
		try {
			frames( 60 );
		} finally {
			Emitter.freezeEmitters = false;
			Game.timeTotal = savedTime;
		}
		assertEquals( 1, PlaceLight.holding() );
	}

	// ------------------------------------------------------------------ a cell's end

	//a field counting each cell's end, and how it ended
	private static final class Ends extends FireField {
		final int[] ends = new int[W * W];

		Ends( Blob blob, BlobEmitter emitter ){
			super( blob, emitter, FireStyle.FIRE );
		}

		@Override
		void goingOut( Level level, int c ){
			ends[c]++;
			super.goingOut( level, c );
		}
	}

	@Test
	public void aCellGoingOutAndOnePutOutByFrostEndOnce(){
		int a = at( 4, 4 ), b = at( 5, 4 ), c = at( 6, 4 );
		Fire fire = blob( Fire.class, 4, a, b, c );
		BlobEmitter em = new BlobEmitter( fire );
		Ends f = new Ends( fire, em );
		FxBudget.frame( FAST_FRAME );
		f.scan();
		assertEquals( 3, f.seen() );

		//burnt out
		fire.clear( a );
		f.scan();
		f.scan();
		assertEquals( 1, f.ends[a] );

		//put out by frost: steam, not smoke
		Freezing frost = blob( Freezing.class, 5, b );
		fire.clear( b );
		int steamBefore = steam( em.matter() );
		FxBudget.frame( FAST_FRAME );
		f.scan();
		f.scan();
		assertEquals( 1, f.ends[b] );
		assertEquals( FireField.FROST_WISPS, steam( em.matter() ) - steamBefore );
		assertNotNull( frost );

		//a cell still burning has not ended
		assertEquals( 0, f.ends[c] );
	}

	private static int steam( Emitter e ){
		int n = 0;
		for (Gizmo g : e.membersView()){
			if (g instanceof FxParticle && g.alive && ((FxParticle) g).colorNow() == FxFactories.STEAM) n++;
		}
		return n;
	}

	// ------------------------------------------------------------------ blasts, fronts, bursts

	@Test
	public void blastsAreSizedByTheirBurstAndCrowdEachOther(){
		assertEquals( Explosion.GUNS, Explosion.classOf( 3 ) );
		assertEquals( Explosion.SMALL, Explosion.classOf( 10 ) );
		assertEquals( Explosion.SMALL, Explosion.classOf( 29 ) );
		assertEquals( Explosion.BOMB, Explosion.classOf( 30 ) );
		assertEquals( Explosion.BIG, Explosion.classOf( 100 ) );
		//two at once play as they are, a third a class smaller; later on, whole again
		assertEquals( Explosion.BOMB, Explosion.crowd( Explosion.BOMB, 10f ) );
		assertEquals( Explosion.BOMB, Explosion.crowd( Explosion.BOMB, 10.05f ) );
		assertEquals( Explosion.SMALL, Explosion.crowd( Explosion.BOMB, 10.1f ) );
		assertEquals( Explosion.GUNS, Explosion.crowd( Explosion.GUNS, 10.12f ) );
		assertEquals( Explosion.BIG, Explosion.crowd( Explosion.BIG, 11f ) );
	}

	@Test
	public void theFiredampFrontRunsOutBreadthFirst(){
		//a line of four, a cell beside its second, and one apart from them all
		int[] cells = { at( 2, 2 ), at( 3, 2 ), at( 4, 2 ), at( 5, 2 ), at( 3, 3 ), at( 9, 9 ) };
		int[] d = FiredampFront.distances( cells, W );
		assertEquals( 0, d[0] );
		assertEquals( 1, d[1] );
		assertEquals( 2, d[2] );
		assertEquals( 3, d[3] );
		assertEquals( 1, d[4] );
		assertEquals( 4, d[5] );
		//its whole run within IGNITE_MAX
		FiredampFront.ignition( cells );
		Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
		assertEquals( 1, living( lights, FiredampFront.class ) );
		frames( Math.round( FiredampFront.IGNITE_MAX * 30 ) + 2 );
		assertEquals( 0, living( lights, FiredampFront.class ) );
	}

	//a watch writing down what it counted
	private static final class Counted extends BurstWatch {
		int n = -1;
		float x, y;

		@Override
		protected void play( int n, float x, float y ){
			assertEquals( "played once", -1, this.n );
			this.n = n;
			this.x = x;
			this.y = y;
		}
	}

	@Test
	public void aBurstIsCountedWholeAndPlaysOnce(){
		FxEmitter e = new FxEmitter();
		Counted w = new Counted();
		assertTrue( BurstWatch.open( w, e, 10, 20 ) );
		for (int i = 1; i < 20; i++) assertTrue( BurstWatch.count( Counted.class, e, 30, 20 ) );
		//another emitter's are not its
		assertFalse( BurstWatch.count( Counted.class, new FxEmitter(), 0, 0 ) );
		frames( 1 );
		assertEquals( 20, w.n );
		assertEquals( (10 + 19 * 30) / 20f, w.x, 0.001f );
		assertEquals( 20f, w.y, 0.001f );
		//closed: nothing more is counted for it
		assertFalse( BurstWatch.count( Counted.class, e, 0, 0 ) );
	}

	@Test
	public void everyBurstPlaysHoweverManyOpenInOneFrame(){
		//a shrapnel bomb: far more bursts in one frame than watches open at once, each counted whole
		Counted[] small = new Counted[3 * BurstWatch.OPEN];
		for (int i = 0; i < small.length; i++){
			FxEmitter e = new FxEmitter();
			small[i] = new Counted();
			assertTrue( "watch " + i + " opens", BurstWatch.open( small[i], e, i, 0 ) );
			for (int k = 1; k < 5; k++) assertTrue( BurstWatch.count( Counted.class, e, i, 0 ) );
		}
		//and the bomb's own burst after them all
		Group effects = layers[GameScene.Layer.EFFECTS.ordinal()];
		FxEmitter blast = new FxEmitter();
		effects.add( blast );
		blast.pos( 100, 60 );
		blast.burst( BlastParticle.FACTORY, 30 );
		frames( 2 );
		for (Counted w : small) assertEquals( "each played once, whole", 5, w.n );
		assertTrue( "the bomb played: its heat ring", living( layers[GameScene.Layer.FLOOR.ordinal()], FxRing.class ) > 0 );
		assertEquals( "and no watch is left", 0, living( layers[GameScene.Layer.LIGHTS.ordinal()], BurstWatch.class ) );
	}

	@Test
	public void aQuenchIsReadOffItsBurst(){
		//beside the hero (no thinning for the distance)
		Group effects = layers[GameScene.Layer.EFFECTS.ordinal()];
		FxEmitter small = new FxEmitter(), big = new FxEmitter();
		effects.add( small );
		effects.add( big );
		small.pos( 104, 136 );
		small.burst( SteamParticle.FACTORY, 4 );
		frames( 2 );
		int few = living( effects, SteamParticle.class );
		assertEquals( "a small burst is only its wisps", 4, few );
		big.pos( 152, 136 );
		big.burst( SteamParticle.FACTORY, SteamParticle.QUENCH );
		frames( 2 );
		int early = living( effects, SteamParticle.class ) - few;
		//its own wisps and the lingering puffs at once; the jet spat out over JET_FOR s
		assertTrue( "a quench: " + early, early >= SteamParticle.QUENCH + SteamParticle.LINGER );
		frames( Math.round( SteamParticle.JET_FOR * 30 ) + 4 );
		int all = living( effects, SteamParticle.class ) - few;
		assertEquals( "its wisps, the jet's and the puffs", SteamParticle.QUENCH + SteamParticle.JET + SteamParticle.LINGER, all );
		assertTrue( all <= 20 );
	}

	@Test
	public void aFlameIsHiddenWhereTheHeroDoesNotSee(){
		FxEmitter e = new FxEmitter();
		layers[GameScene.Layer.EFFECTS.ordinal()].add( e );
		Arrays.fill( level.heroFOV, false );
		e.pos( 40, 40 );
		e.burst( FlameParticle.FACTORY, 6 );
		frames( 2 );
		//off the scene's world camera the flames stay in their caller; none of them is drawn
		int shown = 0;
		for (Group g : layers) shown += shownFlames( g );
		assertEquals( 0, shown );
	}

	private static int shownFlames( Group g ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m instanceof FlameParticle && m.alive && m.visible) n++;
			if (m instanceof Group) n += shownFlames( (Group) m );
		}
		return n;
	}

	// ------------------------------------------------------------------ co-op

	@Test
	public void aGuestSeesWhatTheHostsCallsShow() throws Exception {
		Hero guest = new Hero();
		guest.pos = at( 2, 2 );
		TestParty.join( guest );
		try {
			NetVisuals.clear();
			int[] pocket = { at( 2, 2 ), at( 3, 2 ), at( 4, 2 ) };
			FireHits.firebolt( at( 5, 5 ) );
			FireHits.heat( at( 6, 5 ), FireHits.BLAST_EMBERS, FireHits.BLAST_RAYS );
			Ash.flakes( at( 7, 5 ), 3 );
			RiftFx.glow( new int[]{ at( 9, 9 ), at( 10, 9 ) } );
			RiftFx.telegraph( at( 9, 9 ) );
			RiftFx.flare( at( 10, 9 ) );
			FiredampFront.arming( pocket );
			FiredampFront.ignition( pocket );
			Explosion.meteor( at( 8, 8 ) );
			JSONArray sent = fireEvents();
			int[] kinds = { FireNet.FIREBOLT, FireNet.HEAT, FireNet.ASH, FireNet.RIFT_GLOW, FireNet.RIFT_TELEGRAPH, FireNet.RIFT_FLARE,
					FireNet.DAMP_ARMING, FireNet.DAMP_IGNITION, FireNet.METEOR };
			assertEquals( kinds.length, sent.length() );
			for (int i = 0; i < kinds.length; i++) assertEquals( kinds[i], sent.getJSONObject( i ).getInt( "k" ) );
			assertEquals( FireHits.BLAST_RAYS, sent.getJSONObject( 1 ).getInt( "m" ) );
			assertEquals( pocket.length, sent.getJSONObject( 7 ).getJSONArray( "a" ).length() );

			//the guest plays them all, and sends nothing on
			TestParty.guest();
			Explosion.forget();
			Group lights = layers[GameScene.Layer.LIGHTS.ordinal()], floor = layers[GameScene.Layer.FLOOR.ordinal()];
			int fronts = living( lights, FiredampFront.class ), rings = living( floor, FxRing.class );
			NetVisuals.replay( sent );
			assertEquals( "the front, arming and going up", fronts + 2, living( lights, FiredampFront.class ) );
			assertTrue( "the bolt's and the flare's rings", living( floor, FxRing.class ) >= rings + 2 );
			assertTrue( "the meteor's blast waits", Explosion.hinting() );
			assertEquals( 0, fireEvents().length() );
			//its blast taken by the burst on that cell's middle
			FxEmitter blast = new FxEmitter();
			layers[GameScene.Layer.EFFECTS.ordinal()].add( blast );
			blast.pos( 8.5f * 16, 8.5f * 16 );
			blast.burst( BlastParticle.FACTORY, 30 );
			frames( 2 );
			assertFalse( Explosion.hinting() );
		} finally {
			TestParty.leave();
			NetVisuals.clear();
		}
	}

	//the fire events the host has sent since the last drain
	private static JSONArray fireEvents() throws Exception {
		JSONArray out = new JSONArray();
		JSONArray events = NetVisuals.drainEvents();
		if (events == null) return out;
		for (int i = 0; i < events.length(); i++){
			if (events.getJSONObject( i ).getString( "t" ).equals( "fh" )) out.put( events.getJSONObject( i ) );
		}
		return out;
	}

	// ------------------------------------------------------------------ the GPU

	@Test
	public void nothingTouchesTheGpuWhileItIsMade(){
		failOnGl();
		new BlobEmitter( blob( Fire.class, 6, at( 3, 3 ) ) );
		new BlobEmitter( blob( ForgeCoals.class, 124, at( 5, 5 ) ) );
		new BlobEmitter( blob( Steam.class, 20, at( 7, 7 ) ) );
		Fire fire = blob( Fire.class, 6, at( 4, 3 ) );
		FireField.attach( fire, new BlobEmitter( fire ), FireStyle.FIRE );
		FxEmitter e = new FxEmitter();
		Explosion.burst( e, 0, 50, 50 );
		new LightFrame().setup( xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames.Light.SHAFT_40, 50, 50, 0x6AA8FF, 0.8f );
		new FloorGlow().reset( 50, 50, xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames.Light.POOL_15, 0xFF8A30, 0.35f, FxLight.HEARTH );
		new PlaceLight( at( 2, 2 ), 40, 40, 23, 40, 48, 15, 0xFFB060, 0.6f, FxLight.TORCH ).update();
		FiredampFront.arming( new int[]{ at( 2, 2 ), at( 3, 2 ) } );
		new BodyGlow( BodyGlow.FIRE );
		new Reflection();
		new LightRef();
	}
}

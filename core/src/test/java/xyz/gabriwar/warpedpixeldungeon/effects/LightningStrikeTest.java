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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.StormStrikes;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FlashGate;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Afterglow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.Crackle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.StrikeGround;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning.StrikeLights;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Camera;
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

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * A lightning of the game striking in the hero's sight (Lightning, WeatherOverlay.struck): a major
 * one (a wand's, a potion's) flashes the screen as it strikes, under the one gate a storm's bolts
 * flash under (0.7 s) and FlashGate's for the game's own (2 s apart, and the storm's and the game's
 * three in any ten seconds), softer than a storm's, and cracks as the storm does for a bolt in
 * sight, at most once in 2.5 s; a minor one (a creature's zap) only cracks; none for one he does
 * not see, for a cross through a cell or a hop of a cell, or for a storm's own bolt (the overlay's,
 * which flashes as its bolt strikes after its leader). It is drawn on the weather's layer, over the
 * day/night tint, by a stand-in that goes when it does. A major's leader runs down its channel
 * before it strikes; its callback is as late as ever; its afterglow lingers a moment, three at
 * most, and is let go of. The gates themselves, on a clock handed in.
 */
public class LightningStrikeTest {

	private static final int W = 20;

	private GL20 savedGl, savedGl20;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	private Camera savedCam;
	private Supplier<WeatherOverlay> savedLayer;
	private LongSupplier savedClock;

	private Level level;
	private WeatherOverlay sky;
	private Group stage;
	//FlashGate's clock, ns, moved on with the overlay's frames (run)
	private final long[] now = { 1_000_000_000_000L };

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	private static Field gateClock() throws Exception {
		Field f = FlashGate.class.getDeclaredField( "clock" );
		f.setAccessible( true );
		return f;
	}

	@Before
	public void setUp() throws Exception {
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		savedElapsed = Game.elapsed;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		savedCam = Camera.main;
		savedLayer = Lightning.layer;
		savedClock = (LongSupplier) gateClock().get( null );
		gateClock().set( null, (LongSupplier) () -> now[0] );
		FlashGate.reset();
		FxBudget.reset();
		Lightning.sceneCreated();
		Afterglow.reset();
		Crackle.reset();
		Random.pushGenerator( 77 );
		//textures without a GPU
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class }, (p, m, a) -> {
			Class<?> r = m.getReturnType();
			if (r == int.class) return 1;
			if (r == boolean.class) return false;
			if (r == float.class) return 0f;
			return null;
		} );
		Camera.main = new Camera( 0, 0, 320, 180, 1 );
		ClimateManager.debugPrecipOverride = 0f;
		level = floor();
		Dungeon.level = level;
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = at( 4, 10 );
		sky = new WeatherOverlay();
		sky.setup();
		Lightning.layer = () -> sky;
		stage = new Group();
		Game.elapsed = 0.05f;
	}

	@After
	public void tearDown() throws Exception {
		Lightning.layer = savedLayer;
		gateClock().set( null, savedClock );
		FlashGate.reset();
		Lightning.sceneCreated();
		ClimateManager.debugPrecipOverride = Float.NaN;
		Random.popGenerator();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Camera.main = savedCam;
	}

	private static int at( int x, int y ){
		return x + y * W;
	}

	//an open floor walled round, all of it in the hero's sight
	private static Level floor(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public boolean fogHeld(){ return false; }
			@Override public int climateDepth(){ return 5; }
		};
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int i = 0; i < W; i++){
			l.map[i] = l.map[i + (W - 1) * W] = l.map[i * W] = l.map[W - 1 + i * W] = Terrain.WALL;
		}
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		return l;
	}

	private int count( Class<?> c ){
		int n = 0;
		for (Gizmo g : sky.membersView()) if (g != null && g.alive && c.isInstance( g )) n++;
		return n;
	}

	//the flashes the overlay shows now
	private int flashes(){
		int n = 0;
		for (Gizmo g : sky.membersView()) if (g instanceof LightningFlash && g.alive && g.visible) n++;
		return n;
	}

	private int carried(){
		int n = 0;
		for (Gizmo g : sky.membersView()){
			if (g != null && g.alive && g.getClass().getSimpleName().equals( "Carried" )) n++;
		}
		return n;
	}

	//a major lightning (a wand's) from the hero to a cell, put on the stage and run its first frame
	private Lightning zap( int to ){
		Lightning l = new Lightning( Dungeon.hero.pos, to, null ).major();
		stage.add( l );
		stage.update();
		return l;
	}

	//a minor one (a creature's zap)
	private Lightning minor( int to ){
		Lightning l = new Lightning( Dungeon.hero.pos, to, null );
		stage.add( l );
		stage.update();
		return l;
	}

	//the overlay's frames, its clock with them, and FlashGate's
	private void run( float seconds ){
		for (float t = 0f; t < seconds - 1e-4f; t += Game.elapsed){
			now[0] += (long)(Game.elapsed * 1e9);
			sky.update();
		}
	}

	//a storm's bolt in sight, and the overlay's frames as its leader comes down and it strikes
	private void stormBolt( int cell ){
		StormStrikes.show( level, cell, true );
		sky.update();
		sky.update();
	}

	@Test
	public void theGatesLetOneThroughAGap(){
		WeatherOverlay.Gate g = new WeatherOverlay.Gate( 0.7f );
		assertTrue( g.open( 0f ) );
		assertTrue( g.pass( 0f ) );
		assertFalse( g.pass( 0.69f ) );
		assertFalse( g.open( 0.69f ) );
		assertTrue( g.pass( 0.7f ) );
		//one let through regardless (sheet lightning) closes it all the same
		g.close( 5f );
		assertFalse( g.pass( 5.5f ) );
		assertTrue( g.pass( 5.75f ) );
		assertEquals( 0.7f, WeatherOverlay.FLASH_GAP, 0f );
		assertEquals( 2.5f, WeatherOverlay.THUNDER_GAP, 0f );
	}

	@Test
	public void theCrackIsTheStormsForABoltInSight(){
		//THUNDER_NEAR as loud as it is meant close by, as dull as a strike that far off
		assertEquals( WeatherOverlay.CRACK, WeatherOverlay.crackVolume( 0f, WeatherOverlay.CRACK ), 1e-6f );
		assertEquals( WeatherOverlay.CRACK * (1f - 0.035f * 6f), WeatherOverlay.crackVolume( 6f, WeatherOverlay.CRACK ), 1e-6f );
		assertEquals( WeatherOverlay.CRACK * 0.25f, WeatherOverlay.crackVolume( 40f, WeatherOverlay.CRACK ), 1e-6f );
		//over a sword's hit, a wand's own over that
		assertTrue( WeatherOverlay.CRACK > AmbientSound.THUNDER_NEAR.gain );
		assertEquals( 1f, WeatherOverlay.WAND_CRACK, 0f );
	}

	@Test
	public void aMajorStrikeInSightFlashesSoftlyAndCracks(){
		zap( at( 9, 10 ) );
		assertEquals( 1, flashes() );
		assertFalse( "cracked", sky.cracks.open( 0f ) );
		assertTrue( sky.cracks.open( WeatherOverlay.THUNDER_GAP ) );
		//the game's, softer than a storm's, tinted
		sky.update();
		for (Gizmo g : sky.membersView()){
			if (g instanceof LightningFlash){
				assertTrue( ((LightningFlash) g).am <= LightningFlash.PEAK_GAME + 1e-4f );
				assertTrue( ((LightningFlash) g).am > 0.3f );
			}
		}
	}

	@Test
	public void aZapNeverFlashesTheScreenItOnlyCracks(){
		for (int i = 0; i < 5; i++){
			minor( at( 9, 10 ) );
			run( 1f );
		}
		assertEquals( "no flash for a creature's zap", 0, count( LightningFlash.class ) );
		//its crack, under the same gap
		assertFalse( sky.cracks.open( 0f ) );
		//and its stand-in over the tint, as every lightning's
		Lightning l = minor( at( 10, 12 ) );
		assertTrue( carried() > 0 );
		assertFalse( l.isMajor() );
	}

	@Test
	public void aWandsZapCracksEveryTimeAndFlashesUnderTheGate(){
		//a hop of a cell, which cracks for nothing else, cracks for a wand of lightning
		int near = at( 5, 10 );
		Lightning first = new Lightning( Dungeon.hero.pos, near, null ).thunderous( near );
		stage.add( first );
		stage.update();
		assertEquals( 1, sky.cracked );
		assertEquals( 1, count( LightningFlash.class ) );
		assertFalse( "and holds the others' cracks off", sky.cracks.open( 0f ) );
		//another at once, where the hero does not see: it cracks all the same, with no flash
		int far = at( 12, 10 );
		level.heroFOV[far] = false;
		Lightning second = new Lightning( Dungeon.hero.pos, far, null ).thunderous( far );
		stage.add( second );
		stage.update();
		assertEquals( 2, sky.cracked );
		assertEquals( 1, count( LightningFlash.class ) );
		//while any other lightning's crack waits out its gap
		zap( at( 9, 10 ) );
		assertEquals( 2, sky.cracked );
	}

	@Test
	public void noneForOneUnseenACrossOrAHop(){
		int far = at( 12, 10 );
		level.heroFOV[far] = false;
		zap( far );
		Lightning cross = new Lightning( at( 8, 10 ), at( 10, 10 ), null ).noGlow().major();
		stage.add( cross );
		stage.update();
		zap( at( 5, 10 ) );
		assertEquals( 0, count( LightningFlash.class ) );
		assertTrue( "no crack", sky.cracks.open( 0f ) );
		//but drawn over the tint, all three
		assertEquals( 3, carried() );
	}

	@Test
	public void aStormsOwnBoltLeavesItsFlashToTheOverlay(){
		Lightning own = new Lightning( at( 4, 6 ), at( 9, 10 ), null ).stormsOwn();
		sky.add( own );
		sky.update();
		assertEquals( 0, count( LightningFlash.class ) );
		assertTrue( sky.cracks.open( 0f ) );
		assertEquals( "already on the overlay", 0, carried() );
	}

	@Test
	public void aStormsBoltFlashesAsItStrikesAfterItsLeader(){
		StormStrikes.show( level, at( 12, 12 ), true );
		sky.update();
		assertEquals( "its leader coming down: no flash yet", 0, flashes() );
		sky.update();
		assertEquals( "struck", 1, flashes() );
	}

	@Test
	public void theSkyFlashesOnceAGapForStormsAndLightningAlike(){
		zap( at( 9, 10 ) );
		zap( at( 10, 12 ) );
		assertEquals( "one flash for two at once", 1, flashes() );
		//a storm's bolt in sight inside the gap: drawn, but no flash of its own either
		stormBolt( at( 12, 12 ) );
		assertEquals( 1, flashes() );

		//the gap gone by (and the first flash with it): a storm's flashes, then a major inside its gap
		//does not
		run( 0.75f );
		assertEquals( 0, flashes() );
		stormBolt( at( 12, 12 ) );
		assertEquals( 1, flashes() );
		zap( at( 9, 10 ) );
		assertEquals( 1, flashes() );
		assertFalse( "the crack's gap is longer", sky.cracks.open( 0.8f ) );
	}

	@Test
	public void theGamesFlashesAreTwoSecondsApartAndThreeInTenWithTheStorms(){
		zap( at( 9, 10 ) );
		assertEquals( 1, flashes() );
		//past the overlay's gap but inside the game's: none
		run( 0.75f );
		zap( at( 9, 10 ) );
		assertEquals( 0, flashes() );
		//past the game's two seconds: one
		run( 1.3f );
		zap( at( 9, 10 ) );
		assertEquals( 1, flashes() );
		//a storm's, its own gap gone by: the third in ten seconds
		run( 0.75f );
		stormBolt( at( 12, 12 ) );
		assertEquals( 1, flashes() );
		//a fourth inside ten seconds: none, storm's or game's
		run( 2.1f );
		stormBolt( at( 12, 12 ) );
		zap( at( 9, 10 ) );
		assertEquals( 0, flashes() );
		//a storm flashing all night never strobes: one a gap at most
		run( 10f );
		int shown = 0;
		for (int i = 0; i < 40; i++){
			StormStrikes.show( level, at( 12, 12 ), true );
			sky.update();
			sky.update();
			int f = flashes();
			assertTrue( f <= 1 );
			if (f > 0){
				shown++;
				run( 0.4f );
			}
			run( 0.1f );
		}
		assertTrue( "flashed " + shown, shown > 0 && shown <= 5 );
	}

	@Test
	public void itIsDrawnOverTheTintTillItEnds(){
		Lightning l = zap( at( 9, 10 ) );
		assertEquals( 1, carried() );
		//its frames are still its own stage's: it ends there, and its stand-in with it
		Game.elapsed = 0.1f;
		for (int f = 0; f < 4; f++) stage.update();
		assertFalse( l.alive );
		assertEquals( 0, carried() );

		//one a window shows (not on the world's camera) stays in the window
		Group window = new Group();
		window.camera = new Camera( 0, 0, 100, 100, 2 );
		window.add( new Lightning( Dungeon.hero.pos, at( 9, 10 ), null ).major() );
		window.update();
		assertEquals( 0, carried() );
	}

	@Test
	public void itSparksOnTheOverlayWhereItStrikes(){
		zap( at( 9, 10 ) );
		//the overlay's own emitters, in a group of their own: they burst on its next frame
		sky.update();
		int sparks = 0;
		for (Gizmo g : sky.membersView()){
			if (g == null || g.getClass() != Group.class) continue;
			for (Gizmo e : ((Group) g).membersView()){
				if (e instanceof Emitter) sparks += ((Emitter) e).countLiving();
			}
		}
		assertEquals( "three where it struck", 3, sparks );
	}

	@Test
	public void aMajorsLeaderRunsDownItsChannelBeforeItStrikes(){
		Lightning l = new Lightning( Dungeon.hero.pos, at( 14, 10 ), null ).major();
		Lightning.Arc arc = l.arcs().get( 0 );
		stage.add( l );
		Game.elapsed = 0.011f;
		stage.update();
		//its first legs faint, the rest not yet, no glow, no flash
		assertNull( arc.glow );
		assertEquals( 0, flashes() );
		Image first = (Image) arc.membersView().get( 0 ), last = (Image) arc.membersView().get( 2 * arc.joints - 1 );
		assertEquals( Lightning.LEADER_LIGHT, first.am, 1e-4f );
		assertTrue( first.visible );
		assertFalse( "not yet down", last.visible );
		stage.update();
		assertNull( arc.glow );
		//the strike, within 0.033 s of its making: the flash, the glow, the full stroke
		stage.update();
		assertNotNull( arc.glow );
		assertEquals( 1, flashes() );
		assertTrue( first.am > 0.8f );
		assertTrue( last.visible );
	}

	@Test
	public void aRichMajorStrikesAtItsFullAndItsRunStepsOnFromItsStrike(){
		Lightning.Arc main = new Lightning.Arc( Dungeon.hero.pos, at( 12, 10 ) );
		Lightning.Arc step = new Lightning.Arc( at( 12, 10 ), at( 15, 10 ) ).crawl( 1 );
		Lightning l = new Lightning( Arrays.asList( main, step ), null ).major();
		stage.add( l );
		Game.elapsed = 0.011f;
		stage.update();
		stage.update();
		stage.update();
		assertNotNull( "struck", main.glow );
		//its peak on the strike frame, as a zap's on its first
		Image core = (Image) main.membersView().get( 0 ), stepCore = (Image) step.membersView().get( 0 );
		assertEquals( 1f, core.am, 1e-4f );
		//the run's first step lights STEP_LAG after the strike, not with it
		assertEquals( 0f, stepCore.am, 0f );
		stage.update();
		stage.update();
		assertEquals( "0.022 s after the strike", 0f, stepCore.am, 0f );
		stage.update();
		assertTrue( "0.033 s after the strike", stepCore.am > 0f );
	}

	@Test
	public void aPoolLitAgainIsNeverItsFirstOwnersAgainWhateverIsLaidAfter(){
		Group[] layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		FxLight.reset();
		StrikeLights.reset();
		try {
			Object zap = new Object(), again = new Object(), major = new Object();
			Game.timeTotal = 20f;
			FxLight p = StrikeLights.minor( zap, 40f, 40f, Lightning.POOL_MINOR, Lightning.POOL_COLOR, 0.5f, 0.3f, 0.06f );
			assertEquals( p, StrikeLights.minor( again, 44f, 40f, Lightning.POOL_MINOR, Lightning.POOL_COLOR, 0.5f, 0.3f, 0.06f ) );
			assertFalse( StrikeLights.drives( zap, p ) );
			//a rich major's pools, all it may lay, right after
			for (int i = 0; i < Lightning.POOLS + Lightning.JOINT_POOLS_ALL; i++){
				StrikeLights.pool( major, 100f + 20f * i, 200f, Lightning.POOL_JOINT, Lightning.POOL_COLOR, 0.5f, 0.3f, 0.06f );
			}
			assertFalse( "the zap that lit it first drives it no more", StrikeLights.drives( zap, p ) );
			assertTrue( StrikeLights.drives( again, p ) );
		} finally {
			GameScene.useLayersForTests( null );
			FxLight.reset();
			StrikeLights.reset();
		}
	}

	@Test
	public void itIsSeenWhereTheHeroSeesAnEndOfAnArc(){
		Arrays.fill( level.heroFOV, false );
		Lightning l = new Lightning( Arrays.asList( new Lightning.Arc( at( 4, 6 ), at( 9, 6 ) ), new Lightning.Arc( at( 9, 6 ), at( 14, 6 ) ) ), null );
		assertFalse( l.seen() );
		level.heroFOV[at( 14, 6 )] = true;
		assertTrue( l.seen() );
	}

	@Test
	public void aMajorCallsBackAsLateAsEverLeaderAndAll(){
		int[] took = new int[2];
		for (boolean major : new boolean[]{ false, true }){
			final int[] calls = { 0 };
			Lightning l = new Lightning( Dungeon.hero.pos, at( 12, 10 ), () -> calls[0]++ );
			if (major) l.major();
			stage.add( l );
			Game.elapsed = 1 / 60f;
			int frames = 0;
			while (calls[0] == 0 && frames < 100){
				stage.update();
				frames++;
			}
			assertFalse( l.alive );
			took[major ? 1 : 0] = frames;
			Lightning.sceneCreated();
		}
		//the frame its 0.3 s run out, as ever: a major's leader is inside them, not added to them
		assertEquals( took[0], took[1] );
		assertTrue( "about 0.3 s: " + took[0], took[0] >= 18 && took[0] <= 19 );
	}

	private static int living( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m != null && m.alive && c.isInstance( m )) n++;
			if (m instanceof Group) n += living( (Group) m, c );
		}
		return n;
	}

	private static int pools( Group lights, int width ){
		int n = 0;
		for (Gizmo m : lights.membersView()){
			if (m instanceof FxLight && m.alive && ((FxLight) m).width == width) n++;
		}
		return n;
	}

	@Test
	public void theGroundAndTheWaterAnswerWhereItStrikes(){
		Group[] layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		WaterFX.sceneCreated();
		StrikeLights.reset();
		try {
			for (int y = 13; y < 18; y++) for (int x = 9; x < 16; x++) level.map[x + y * W] = Terrain.WATER;
			level.buildFlagMaps();
			Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
			Group floor = layers[GameScene.Layer.FLOOR.ordinal()];
			Group surface = layers[GameScene.Layer.SURFACE.ordinal()];

			//a major on dry floor: its pool, pools along its joints, a scorch, a crackle
			zap( at( 9, 10 ) );
			assertEquals( 1, pools( lights, Lightning.POOL_MAJOR ) );
			assertTrue( "along its joints", pools( lights, Lightning.POOL_JOINT ) >= 1 );
			assertEquals( "a scorch", 1, living( floor, FxDecal.class ) );
			assertTrue( Crackle.alive() >= 1 );
			for (Gizmo g : lights.membersView()){
				if (g instanceof FxLight && g.alive) assertTrue( "under its caps", ((FxLight) g).am <= FxLight.POOL_CAP + 1e-4f );
			}

			//a major into the water: washes through it (two cells round), a ring, and the fish dart off
			zap( at( 12, 15 ) );
			int washes = living( surface, WaterFX.Wash.class );
			assertTrue( "washes " + washes, washes > 9 && washes <= StrikeGround.WASHES );
			assertTrue( living( surface, Ripple.class ) >= 1 );
			assertEquals( "no scorch on the water", 1, living( floor, FxDecal.class ) );
			WaterFX.frame();
			float[] out = new float[WaterFX.BUS * 4];
			int n = WaterFX.disturbances( out ), darts = 0;
			for (int i = 0; i < n; i++) if (out[4 * i + 2] == StrikeGround.DISTURB) darts++;
			assertEquals( 1, darts );

			//a creature's zaps striking twice in a place within 0.35 s: one small pool, lit again
			int small = pools( lights, Lightning.POOL_MINOR );
			Game.timeTotal = 50f;
			minor( at( 4, 15 ) );
			assertEquals( small + 1, pools( lights, Lightning.POOL_MINOR ) );
			Game.timeTotal = 50.1f;
			minor( at( 4, 15 ) );
			assertEquals( "lit again, not doubled", small + 1, pools( lights, Lightning.POOL_MINOR ) );
			//and no scorch for a zap
			assertEquals( 1, living( floor, FxDecal.class ) );
		} finally {
			GameScene.useLayersForTests( null );
			FxLight.reset();
			FxDecal.reset();
			FxRing.reset();
			WaterFX.sceneCreated();
			StrikeLights.reset();
		}
	}

	@Test
	public void itsAfterglowLingersAMomentThreeAtMostAndGoes(){
		Lightning[] majors = new Lightning[5];
		for (int i = 0; i < majors.length; i++) majors[i] = zap( at( 8 + i, 12 ) );
		assertEquals( "extras for the first three majors", 3, Lightning.richAlive() );
		Game.elapsed = 0.05f;
		for (int f = 0; f < 6; f++) stage.update();
		for (Lightning l : majors) assertFalse( l.alive );
		assertEquals( 0, Lightning.richAlive() );
		int glows = count( Afterglow.class );
		assertEquals( 3, glows );
		assertTrue( Afterglow.alive() <= Afterglow.MOST );
		//its lines are the cores alone, faint, its colour
		for (Gizmo g : sky.membersView()){
			if (!(g instanceof Afterglow)) continue;
			Afterglow a = (Afterglow) g;
			assertTrue( a.lines() > 0 && a.lines() <= Afterglow.LINES );
		}
		//gone after its 0.12 s, its group let go of
		Game.elapsed = 0.05f;
		for (int f = 0; f < 3; f++) sky.update();
		assertEquals( 0, count( Afterglow.class ) );
		assertEquals( 0, Afterglow.alive() );
		for (Gizmo g : sky.membersView()) assertFalse( g instanceof Afterglow );
	}

	private Group[] lightLayers(){
		Group[] layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		WaterFX.sceneCreated();
		StrikeLights.reset();
		return layers;
	}

	private static void noLightLayers(){
		GameScene.useLayersForTests( null );
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		WaterFX.sceneCreated();
		StrikeLights.reset();
	}

	private static float level( FxLight l ) throws Exception {
		Field f = FxLight.class.getDeclaredField( "level" );
		f.setAccessible( true );
		return f.getFloat( l );
	}

	@Test
	public void itsPoolsNeverMakeWayForEachOtherNorLightAnotherLight() throws Exception {
		Group[] layers = lightLayers();
		try {
			Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
			//every light the budget allows already lit, faint, out of the way
			for (int i = 0; i < FxBudget.lightCap(); i++) FxLight.glow( 300f, 300f, 15, 0xFFFFFF, 0.05f );
			//a wand's chain to three: a pool where each arc strikes, none of them making way for
			//another of its own nor for a pool along its joints
			int a = Dungeon.hero.pos, b = at( 9, 10 ), c = at( 14, 10 ), d = at( 14, 15 );
			Lightning l = new Lightning( Arrays.asList( new Lightning.Arc( a, b ), new Lightning.Arc( b, c ),
					new Lightning.Arc( c, d ) ), null ).major();
			stage.add( l );
			stage.update();
			stage.update();
			assertEquals( 3, pools( lights, Lightning.POOL_MAJOR ) );
			assertTrue( FxLight.live() <= FxBudget.lightCap() );

			//one of them let go of (it made way for another) and made another light elsewhere: the
			//lightning drives it no more
			FxLight mine = null;
			for (Gizmo g : lights.membersView()){
				if (g instanceof FxLight && g.alive && ((FxLight) g).width == Lightning.POOL_MAJOR){
					mine = (FxLight) g;
					break;
				}
			}
			assertNotNull( mine );
			mine.kill();
			FxLight other = null;
			for (int i = 0; i < 2 * FxBudget.lightCap() && other != mine; i++){
				other = FxLight.glow( 200f + i, 260f, 23, 0xFF8000, 0.2f );
			}
			assertSame( "the same light, made anew", mine, other );
			stage.update();
			assertEquals( 0.2f, level( other ), 0f );
		} finally {
			noLightLayers();
		}
	}

	@Test
	public void aZapNeverLightsAgainAPoolMadeIntoAnotherLight(){
		Group[] layers = lightLayers();
		try {
			Game.timeTotal = 30f;
			FxLight p = StrikeLights.minor( new Object(), 40f, 40f, Lightning.POOL_MINOR, Lightning.POOL_COLOR, 0.5f, 0.3f, 0.06f );
			p.kill();
			FxLight fire = FxLight.glow( 41f, 40f, 23, 0xFF8000, 0.2f ).persistent();
			assertSame( "the same light, made anew", p, fire );
			FxLight q = StrikeLights.minor( new Object(), 44f, 40f, Lightning.POOL_MINOR, Lightning.POOL_COLOR, 0.5f, 0.3f, 0.06f );
			assertTrue( "a pool of its own, not the fire's light", q != fire );
			assertTrue( fire.isPersistent() );
		} finally {
			noLightLayers();
		}
	}

	@Test
	public void aBoltOutOfTheSkyLightsOnlyTheGroundItStrikes(){
		Group[] layers = lightLayers();
		try {
			Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
			//a storm call's drop, seven cells out of the air: its pool where it lands, none along its
			//joints up the view
			int cell = at( 9, 14 );
			PointF foot = DungeonTilemap.tileCenterToWorld( cell );
			stage.add( new Lightning( new PointF( foot.x, foot.y - DungeonTilemap.SIZE * 7 ), cell, null ).major().aloft() );
			stage.update();
			assertEquals( 1, pools( lights, Lightning.POOL_MAJOR ) );
			assertEquals( "none up the view", 0, pools( lights, Lightning.POOL_JOINT ) );
			//nor a storm's bolt
			Lightning bolt = Lightning.visual( Arrays.asList( Lightning.Arc.visual( new PointF( foot.x + 60f, foot.y - 120f ),
					DungeonTilemap.tileCenterToWorld( at( 14, 14 ) ), 6, 1f, 0f, false ).storm() ), null ).stormsOwn( sky );
			sky.add( bolt );
			sky.update();
			assertEquals( 1, pools( lights, Lightning.POOL_STORM ) );
			assertEquals( 0, pools( lights, Lightning.POOL_JOINT ) );
		} finally {
			noLightLayers();
		}
	}

	@Test
	public void aDropOutOfTheAirStrikesHoweverShort(){
		Group[] layers = lightLayers();
		try {
			Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
			//charged steam's bolt, 24 px down onto a sprite: the sky's answer, a pool, a crackle
			PointF to = DungeonTilemap.tileCenterToWorld( at( 9, 10 ) );
			stage.add( new Lightning( new PointF( to.x, to.y - 24f ), to, null ).major().aloft() );
			stage.update();
			assertEquals( 1, flashes() );
			assertFalse( "cracked", sky.cracks.open( 0f ) );
			assertEquals( 1, pools( lights, Lightning.POOL_MAJOR ) );
			assertTrue( Crackle.alive() >= 1 );
		} finally {
			noLightLayers();
		}
	}

	@Test
	public void nothingOfItShowsBeforeItsFirstFrame(){
		Lightning l = new Lightning( Dungeon.hero.pos, at( 12, 10 ), null ).major();
		int images = 0;
		for (Lightning.Arc arc : l.arcs()){
			for (Gizmo g : arc.membersView()){
				if (!(g instanceof Image)) continue;
				images++;
				assertFalse( "shown before it is run", g.visible && ((Image) g).am > 0f );
			}
		}
		assertTrue( images > 2 );
	}
}

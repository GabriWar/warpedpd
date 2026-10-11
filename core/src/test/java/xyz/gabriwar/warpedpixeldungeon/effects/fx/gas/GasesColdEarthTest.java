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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.gas;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.*;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PitfallParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.WebParticle;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.GooSprite;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The gases, the cold and the earth, headless: every cloud's blob draws as many of the game's random
 * numbers as it ever did (its one pour) and none as it emits; smoke keeps to 8 a cell and every gas
 * together to its share of the effects' budget, spending no spawns once full; a web is one picture a cell; the actor-side frost and poison bursts are as they
 * were; the 62 plant poisons draw no game randomness as they flicker, in whole pixels; nothing is
 * made on the GPU; the curves (thickness, the confusion wheel, the shading, the trickle of falling
 * rocks) are what the docs say.
 */
public class GasesColdEarthTest {

	private static final int W = 16;
	//the real frame time the budget's governor is told: a phone keeping 60 fps (told the game's
	//1/30 s steps it would think the phone struggling and thin everything)
	private static final float FAST_FRAME = 1 / 60f;

	private GL20 savedGl, savedGl20;
	private float savedElapsed, savedScale;
	private Level savedLevel;
	private Group[] layers;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp() throws Exception {
		savedGl = Gdx.gl;
		savedGl20 = Gdx.gl20;
		stubGl();
		savedElapsed = Game.elapsed;
		savedLevel = Dungeon.level;
		savedScale = scale( 1f );
		FxBudget.reset();
		Method gov = FxBudget.class.getDeclaredMethod( "resetGovernor" );
		gov.setAccessible( true );
		gov.invoke( null );
		Emitter.freezeEmitters = false;
		FxRandom.seedForTests( 11 );
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		CellOverlay.reset();
		GasPuff.reset();
		GasGlow.reset();
		Dungeon.level = floor( W );
		Game.elapsed = 1 / 30f;
	}

	@After
	public void tearDown() throws Exception {
		GameScene.useLayersForTests( null );
		CellOverlay.reset();
		GasPuff.reset();
		GasGlow.reset();
		FxBudget.reset();
		scale( savedScale );
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
	}

	//the budget's device scale (1.5 on a desktop) set, the old one returned: the phone's numbers
	private static float scale( float s ) throws Exception {
		Field f = FxBudget.class.getDeclaredField( "scale" );
		f.setAccessible( true );
		float was = f.getFloat( null );
		f.setFloat( null, s );
		return was;
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

	private static void failOnGl(){
		Gdx.gl = Gdx.gl20 = (GL20) Proxy.newProxyInstance( GL20.class.getClassLoader(), new Class<?>[]{ GL20.class },
				(p, m, a) -> { throw new AssertionError( "GL call while building: " + m.getName() ); } );
	}

	private static Level floor( int w ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public String waterTex(){ return Assets.Environment.WATER_CAVES; }
		};
		l.setSize( w, w );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		l.visited = new boolean[l.length()];
		l.mapped = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		Arrays.fill( l.visited, true );
		return l;
	}

	private static Blob cloud( Class<? extends Blob> kind, int x0, int y0, int size, int amount ) throws Exception {
		Blob b = kind.getDeclaredConstructor().newInstance();
		int w = Dungeon.level.width();
		for (int y = y0; y < y0 + size; y++) for (int x = x0; x < x0 + size; x++) b.seed( Dungeon.level, x + y * w, amount );
		return b;
	}

	private void frame( Emitter e ){
		FxBudget.frame( FAST_FRAME );
		e.update();
		for (Group g : layers) g.update();
	}

	private static int living( Group g ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.alive) continue;
			if (m instanceof Group) n += living( (Group) m );
			else n++;
		}
		return n;
	}

	private static int living( Group g, Class<?> c ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.alive) continue;
			if (c.isInstance( m )) n++;
			if (m instanceof Group) n += living( (Group) m, c );
		}
		return n;
	}

	@SuppressWarnings("unchecked")
	private static final Class<? extends Blob>[] CLOUDS = new Class[]{ ToxicGas.class, ParalyticGas.class, ConfusionGas.class,
			CorrosiveGas.class, StenchGas.class, Smoke.class, SmokeScreen.class, PoisonGas.class, DigestingAcidCloud.class,
			GrassSmokeGas.class, Withercloud.class, CorruptGas.class, CloudOfCorruption.class, Depressant.class, Dirtcloud.class,
			Miasma.class, Slownesscloud.class, PopGas.class, BallGas.class, IceStorm.class, Freezing.class, Blizzard.class,
			Snowstorm.class, Snowwhirlwind.class, Web.class, OozeGas.class, GooWarn.class, SkeletonKey.KeyWall.class };

	@Test
	public void everyCloudsBlobDrawsTheGamesRandomOnceAsEver() throws Exception {
		Random.pushGenerator( 99 );
		Random.Float();
		float second = Random.Float();
		Random.popGenerator();
		for (Class<? extends Blob> kind : CLOUDS){
			Blob b = cloud( kind, 5, 5, 3, 50 );
			Random.pushGenerator( 99 );
			new BlobEmitter( b );
			assertEquals( kind.getSimpleName() + "'s use() drew one, as ever", second, Random.Float(), 0f );
			Random.popGenerator();
		}
	}

	@Test
	public void cloudsDrawNoneOfTheGamesRandomAsTheyEmit() throws Exception {
		for (Class<? extends Blob> kind : CLOUDS){
			//the blizzard sheds another area's particles, which still draw the game's Random as they
			//are made: its snow (AmbientSnowParticle, the weather's)
			if (kind == Blizzard.class) continue;
			//the last cloud's pictures, left behind undrawn, no longer count against the budget
			FxBudget.reset();
			GasPuff.reset();
			BlobEmitter e = new BlobEmitter( cloud( kind, 5, 5, 3, 200 ) );
			Random.pushGenerator( 7 );
			float first = Random.Float();
			Random.popGenerator();
			Random.pushGenerator( 7 );
			for (int i = 0; i < 90; i++) frame( e );
			assertEquals( kind.getSimpleName() + " emitting drew none of the game's Random", first, Random.Float(), 0f );
			Random.popGenerator();
			assertTrue( kind.getSimpleName() + " shows something", living( e ) + living( layers[GameScene.Layer.FLOOR.ordinal()] ) > 0 );
		}
	}

	@Test
	public void smokeKeepsToEightACell() throws Exception {
		BlobEmitter e = new BlobEmitter( cloud( Smoke.class, 3, 3, 5, 1000 ) );
		float sum = 0;
		int samples = 0, peak = 0;
		for (int i = 0; i < 30 * 12; i++){
			frame( e );
			if (i >= 30 * 4){
				int n = living( e );
				sum += n;
				samples++;
				peak = Math.max( peak, n );
			}
		}
		float mean = sum / samples / 25f;
		assertTrue( "smoke holds " + mean + " a cell on average", mean <= 8f );
		assertTrue( "and not much more at its fullest: " + peak / 25f, peak / 25f <= 10f );
		assertTrue( "but it is there: " + mean, mean >= 5f );
	}

	//two big clouds hold their bodies to the gases' share of the budget, leaving the rest of it
	//(a fight's sparks, fire, lightning) unthinned
	@Test
	public void everyGasTogetherKeepsToItsShareOfTheBudget() throws Exception {
		Dungeon.level = floor( 34 );
		BlobEmitter[] es = { new BlobEmitter( cloud( ToxicGas.class, 1, 1, 32, 1000 ) ),
				new BlobEmitter( cloud( CorrosiveGas.class, 1, 1, 32, 1000 ) ) };
		int peak = 0;
		for (int i = 0; i < 30 * 8; i++){
			FxBudget.frame( FAST_FRAME );
			for (BlobEmitter e : es) e.update();
			//the layers too: what the clouds shed there (an acid drip's sizzle) lives and dies
			for (Group g : layers) g.update();
			peak = Math.max( peak, GasPuff.live() );
			assertTrue( "gas bodies " + GasPuff.live(), GasPuff.live() <= GasPuff.cap() );
			assertTrue( "kit particles " + FxBudget.live( FxBudget.PARTICLES ), FxBudget.live( FxBudget.PARTICLES ) <= 0.8f * FxBudget.soft() );
		}
		assertTrue( "the share is under the point where the budget thins", GasPuff.cap() <= 0.4f * FxBudget.soft() );
		assertTrue( "a big cloud is drawn: " + peak, peak > 100 );
	}

	//once the gases' share is full a cloud makes nothing and takes none of the frame's spawns
	@Test
	public void aFullCloudTakesNoSpawns() throws Exception {
		Emitter filler = new Emitter();
		while (GasPuff.room()) GasPuff.make( filler, 10, 10 ).body( GasStyle.TOXIC, 100, 0, 0 );
		BlobEmitter e = new BlobEmitter( cloud( ToxicGas.class, 2, 2, 10, 1000 ) );
		for (int i = 0; i < 30; i++){
			FxBudget.frame( FAST_FRAME );
			e.update();
			assertEquals( "no spawns taken", FxBudget.spawns(), FxBudget.spawnsLeft() );
		}
		assertEquals( "and nothing made", 0, living( e ) );
	}

	//a cloud of more cells than a frame's spawns: every quarter of it gets bodies, not only the
	//cells first in the scan
	@Test
	public void aBigCloudIsDrawnAllOver() throws Exception {
		BlobEmitter e = new BlobEmitter( cloud( ToxicGas.class, 2, 2, 12, 1000 ) );
		for (int i = 0; i < 30 * 3; i++) frame( e );
		int[] quarters = new int[4];
		float mid = 8 * 16;
		for (Gizmo g : e.membersView()){
			if (!(g instanceof GasPuff) || !g.alive) continue;
			GasPuff p = (GasPuff) g;
			quarters[(p.x < mid ? 0 : 1) + (p.y < mid ? 0 : 2)]++;
		}
		int least = Math.min( Math.min( quarters[0], quarters[1] ), Math.min( quarters[2], quarters[3] ) );
		int most = Math.max( Math.max( quarters[0], quarters[1] ), Math.max( quarters[2], quarters[3] ) );
		assertTrue( "every quarter has bodies: " + Arrays.toString( quarters ), least > 0 );
		assertTrue( "and about as many: " + Arrays.toString( quarters ), least * 2 >= most );
	}

	//the emitter's scan visits every cell of any area once, spread out
	@Test
	public void theScanVisitsEveryCellOnce() throws Exception {
		Method stride = BlobEmitter.class.getDeclaredMethod( "stride", int.class );
		stride.setAccessible( true );
		for (int n = 1; n <= 400; n++){
			int st = (int) stride.invoke( null, n );
			boolean[] seen = new boolean[n];
			int s = n / 3;
			for (int k = 0; k < n; k++, s = (s + st) % n){
				assertFalse( "n " + n + " visits " + s + " twice", seen[s] );
				seen[s] = true;
			}
			if (n >= 16) assertTrue( "n " + n + " strides far: " + st, st > n / 3 && st < n * 4 / 5 );
		}
	}

	//the cell pictures (a web, rime) are held on every cell without taking the frame's spawns
	@Test
	public void cellPicturesTakeNoneOfTheFramesSpawns() throws Exception {
		BlobEmitter web = new BlobEmitter( cloud( Web.class, 2, 2, 8, 30 ) );
		Freezing f = (Freezing) cloud( Freezing.class, 2, 2, 6, 10 );
		BlobEmitter frost = new BlobEmitter( f );
		for (int i = 0; i < 30; i++){
			FxBudget.frame( FAST_FRAME );
			web.update();
			assertEquals( "the webs took no spawns", FxBudget.spawns(), FxBudget.spawnsLeft() );
			frost.update();
			for (Group g : layers) g.update();
		}
		assertEquals( "a web on each cell", 64, CellOverlay.live( CellOverlay.WEB ) );
		assertEquals( "rime on each cell", 36, CellOverlay.live( CellOverlay.RIME ) );
	}

	@Test
	public void aWebIsOnePictureACell() throws Exception {
		BlobEmitter e = new BlobEmitter( cloud( Web.class, 4, 4, 2, 30 ) );
		for (int i = 0; i < 90; i++) frame( e );
		assertEquals( "no strands", 0, living( e ) );
		assertEquals( "one web a cell", 4, CellOverlay.live( CellOverlay.WEB ) );
		assertEquals( "on the floor", 4, living( layers[GameScene.Layer.FLOOR.ordinal()], CellOverlay.class ) );
		//walked through: it shakes, two motes of silk, still one picture
		int cell = 4 + 4 * W;
		CellOverlay.jiggle( cell );
		assertEquals( 4, CellOverlay.live( CellOverlay.WEB ) );
		//burnt away: gone within its hold and fade
		e.on = false;
		for (int i = 0; i < 30 * 2; i++) frame( e );
		assertEquals( "faded once no longer kept", 0, CellOverlay.live( CellOverlay.WEB ) );
	}

	@Test
	public void frostLaysRimeAndLetsItGo() throws Exception {
		Freezing f = (Freezing) cloud( Freezing.class, 4, 4, 2, 10 );
		BlobEmitter e = new BlobEmitter( f );
		for (int i = 0; i < 30; i++) frame( e );
		assertEquals( "rime on each cell", 4, CellOverlay.live( CellOverlay.RIME ) );
		int perCell = living( e ) / 4;
		assertTrue( "about ten a cell, not 25: " + perCell, perCell <= 14 );
		f.fullyClear();
		for (int i = 0; i < 30 * 2; i++) frame( e );
		assertEquals( "thawed", 0, CellOverlay.live( CellOverlay.RIME ) );
	}

	//the actor's bursts in Freezing and PoisonGas (and Poison's) as they were before this pass
	@Test
	public void theActorsFrostAndPoisonBurstsAreAsTheyWere() throws Exception {
		String freezing = source( "actors/blobs/Freezing.java" ), poisonGas = source( "actors/blobs/PoisonGas.java" ),
				poison = source( "actors/buffs/Poison.java" );
		assertTrue( freezing.contains( "burst(SnowParticle.RISING_FACTORY, Dungeon.level.water[cell] ? 4 : 2)" ) );
		assertTrue( freezing.contains( "burst(SnowParticle.RISING_FACTORY, 8)" ) );
		assertTrue( freezing.contains( "burst(Speck.factory(Speck.BLIZZARD), 3)" ) );
		assertTrue( freezing.contains( "burst(SnowParticle.FACTORY, 4)" ) );
		assertTrue( freezing.contains( "start( SnowParticle.FACTORY, 0.2f, 6 )" ) );
		assertTrue( poisonGas.contains( "burst( PoisonParticle.SPLASH, 3 )" ) );
		assertTrue( poison.contains( "burst( PoisonParticle.SPLASH, 5 )" ) );
	}

	private static String source( String path ) throws Exception {
		File f = new File( "src/main/java/xyz/gabriwar/warpedpixeldungeon/" + path );
		if (!f.exists()) f = new File( "core/src/main/java/xyz/gabriwar/warpedpixeldungeon/" + path );
		return new String( Files.readAllBytes( f.toPath() ), StandardCharsets.UTF_8 );
	}

	@Test
	public void thePlantPoisonsDrawNoGameRandomAndFlickerInWholePixels() throws Exception {
		File dir = new File( "src/main/java/xyz/gabriwar/warpedpixeldungeon/effects/particles/poisonparticles" );
		if (!dir.exists()) dir = new File( "core/" + dir.getPath() );
		String[] names = dir.list();
		assertNotNull( names );
		assertEquals( 62, names.length );
		Emitter e = new Emitter();
		for (String n : names){
			Class<?> c = Class.forName( "xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles." + n.replace( ".java", "" ) );
			int color = c.getField( "COLOR" ).getInt( null );
			Emitter.Factory f = (Emitter.Factory) c.getField( "FACTORY" ).get( null );
			PixelParticle p = (PixelParticle) c.getDeclaredConstructor().newInstance();
			assertEquals( n + "'s colour", color, p.getColor() & 0xFFFFFF );
			Random.pushGenerator( 3 );
			float first = Random.Float();
			Random.popGenerator();
			Random.pushGenerator( 3 );
			f.emit( e, 0, 10, 10 );
			PixelParticle q = (PixelParticle) e.membersView().get( e.membersView().size() - 1 );
			for (int i = 0; i < 20; i++){
				q.update();
				if (q.alive) assertEquals( n + " sized in whole pixels", Math.round( q.width() ), q.width(), 0f );
			}
			assertEquals( n + " drew none of the game's Random", first, Random.Float(), 0f );
			Random.popGenerator();
		}
	}

	@Test
	public void nothingIsMadeOnTheGpu(){
		failOnGl();
		Emitter e = new Emitter();
		GasPuff.make( e, 10, 10 ).body( GasStyle.TOXIC, 100, 1, 0 );
		GasPuff.make( e, 10, 10 ).wisp( GasStyle.SMOKE, 100, 1, 0 );
		assertNotNull( new GasGlow() );
		assertNotNull( new CellOverlay() );
		WebParticle.strung( 3 + 3 * W );
		PoisonParticle.SPLASH.emit( e, 0, 20, 20 );
		ShadowParticle.CURSE.emit( e, 0, 20, 20 );
		EarthParticle.FALLING.emit( e, 0, 20, 20 );
		PitfallParticle.FACTORY8.emit( e, 0, 20, 20 );
		GooSprite.GooParticle.FACTORY.emit( e, 0, 20, 20 );
		assertTrue( living( e ) > 0 );
	}

	//Goo's pumping up is a warning the player reads: its drops are drawn however busy the screen
	@Test
	public void goosWarningIsNeverThinned() throws Exception {
		FxBudget.add( FxBudget.PARTICLES, FxBudget.hard() );
		try {
			BlobEmitter e = new BlobEmitter( cloud( GooWarn.class, 4, 4, 3, 50 ) );
			for (int i = 0; i < 30; i++) frame( e );
			assertTrue( "the warning shows past the budget's HARD", living( e ) > 0 );
			Emitter pump = new Emitter();
			for (int i = 0; i < 10; i++) GooSprite.GooParticle.WARN.emit( pump, i, 20, 20 );
			assertEquals( "every pump-up drop made", 10, living( pump ) );
		} finally {
			FxBudget.add( FxBudget.PARTICLES, -FxBudget.hard() );
		}
	}

	//a confusion cloud shows the wheel's colours at once, not one hue all over
	@Test
	public void confusionShowsManyColoursAtOnce() throws Exception {
		Field at = GasPuff.class.getDeclaredField( "wheelAt" );
		at.setAccessible( true );
		Emitter e = new Emitter();
		float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
		for (int i = 0; i < 40; i++){
			float w = at.getFloat( GasPuff.make( e, 10, 10 ).body( GasStyle.CONFUSION, 100, 0, 0 ) );
			lo = Math.min( lo, w );
			hi = Math.max( hi, w );
		}
		assertTrue( "bodies spread round the wheel: " + lo + ".." + hi, hi - lo > GasStyle.WHEEL_SECONDS * 0.6f );
	}

	//the overworld's window slides: each cell picture follows its cell, one off the window fades
	@Test
	public void cellPicturesFollowTheWindowsSlide(){
		CellOverlay a = CellOverlay.keep( CellOverlay.WEB, 5 + 5 * W, 88, 88, FxFrames.WEB[0], false, 0xFFFFFF, 0.8f, 0, 1, 1, false );
		CellOverlay b = CellOverlay.keep( CellOverlay.ETCH, 1 + 1 * W, 24, 24, FxFrames.ETCH, false, 0xFFFFFF, 0.5f, 0, 1, 1, false );
		assertNotNull( a );
		assertNotNull( b );
		CellOverlay.shiftAll( 2, 1, W, W );
		assertEquals( "the web's cell slid", 3 + 4 * W, a.cell() );
		assertEquals( a, CellOverlay.on( CellOverlay.WEB, 3 + 4 * W ) );
		assertEquals( null, CellOverlay.on( CellOverlay.WEB, 5 + 5 * W ) );
		assertEquals( "off the window: no cell", -1, b.cell() );
		//kept again where it now lies: still the one picture
		CellOverlay.keep( CellOverlay.WEB, 3 + 4 * W, 56, 72, FxFrames.WEB[0], false, 0xFFFFFF, 0.8f, 0, 1, 1, false );
		assertEquals( 1, CellOverlay.live( CellOverlay.WEB ) );
		//the one off the window fades within its fade
		for (int i = 0; i < 40; i++) b.update();
		assertEquals( 0, CellOverlay.live( CellOverlay.ETCH ) );
	}

	@Test
	public void theCurvesAreTheDocs(){
		//thickness: 0.55 of the peak at a volume of 1, all of it from 1000 up
		assertEquals( 0.5f * 0.55f, GasStyle.thickness( 0.5f, 1 ), 1e-5f );
		assertEquals( 0.5f * 0.7f, GasStyle.thickness( 0.5f, 10 ), 1e-4f );
		assertEquals( 0.5f, GasStyle.thickness( 0.5f, 1000 ), 1e-5f );
		assertEquals( 0.5f, GasStyle.thickness( 0.5f, 100000 ), 1e-5f );
		//the wheel goes round in 6 s through its four colours
		assertEquals( GasStyle.WHEEL[0], GasStyle.wheel( 0 ) );
		assertEquals( GasStyle.WHEEL[1], GasStyle.wheel( 1.5f ) );
		assertEquals( GasStyle.WHEEL[0], GasStyle.wheel( 6f ) );
		//the shading: the frame's top grey shows the crown, its lower grey the underside
		assertEquals( 0xA8F07C, Shade.shown( 0xA8F07C, Shade.BODY, 0x3E9038, Shade.RIM, Shade.BODY ) );
		assertEquals( 0x3E9038, Shade.shown( 0xA8F07C, Shade.BODY, 0x3E9038, Shade.RIM, Shade.RIM ) );
		//every gas pours at about its cap a cell: interval x cap within its bodies' mean life
		for (GasStyle s : GasStyle.ALL){
			assertTrue( s.name + " pours", s.pour > 0.2f && s.pour < 0.5f );
			assertTrue( s.name + " caps", s.cap >= 4 && s.cap <= 8 );
		}
	}

	//a falling rock only ever falls: none is thrown up above where it starts, a burst's or a pour's
	@Test
	public void fallingRocksNeverRise(){
		Emitter e = new Emitter();
		e.pos( 40, 40 );
		e.pour( EarthParticle.FALLING, 0.1f );
		for (int i = 0; i < 90; i++){
			e.update();
			for (Gizmo g : e.membersView()){
				if (!(g instanceof EarthParticle) || !g.alive) continue;
				EarthParticle p = (EarthParticle) g;
				assertTrue( "falling, not thrown up: " + p.vy, p.vy >= 0 );
				assertTrue( "never above its start: " + p.py, p.py >= 40 - EarthParticle.FALL_FROM - 0.01f );
			}
		}
		Emitter b = new Emitter();
		for (int k = 0; k < 4; k++) EarthParticle.FALLING.emit( b, k, 40, 40 );
		for (int i = 0; i < 90; i++){
			for (Gizmo g : b.membersView()){
				if (!(g instanceof EarthParticle) || !g.alive) continue;
				EarthParticle p = (EarthParticle) g;
				g.update();
				assertTrue( "falling, not thrown up: " + p.vy, p.vy >= 0 );
				assertTrue( "never above its start: " + p.py, p.py >= 40 - EarthParticle.FALL_FROM - 0.01f );
			}
		}
	}

	//a pour's rocks (a rock fall's warning, a frame apart) each fall at once
	@Test
	public void aPoursRocksFallAtOnce(){
		Emitter e = new Emitter();
		float t0 = Game.timeTotal;
		try {
			for (int k = 0; k < 5; k++){
				Game.timeTotal += Game.elapsed;
				EarthParticle.FALLING.emit( e, k, 40, 40 );
			}
		} finally {
			Game.timeTotal = t0;
		}
		assertEquals( 5, e.membersView().size() );
		for (Gizmo g : e.membersView()) assertTrue( "shown from the start", ((EarthParticle) g).waiting() == 0 );
	}

	@Test
	public void fallingRocksTrickleAboutEveryThirdOfASecond(){
		Emitter e = new Emitter();
		float[] landed = new float[5];
		for (int k = 0; k < 5; k++) EarthParticle.FALLING.emit( e, k, 40, 40 );
		float t = 0;
		Arrays.fill( landed, -1 );
		while (t < 3f){
			t += Game.elapsed;
			for (int k = 0; k < e.membersView().size(); k++){
				Gizmo g = e.membersView().get( k );
				if (!(g instanceof EarthParticle)) continue;
				EarthParticle p = (EarthParticle) g;
				boolean was = p.alive;
				p.update();
				if (was && !p.alive && k < 5 && landed[k] < 0) landed[k] = t;
			}
		}
		for (int k = 1; k < 5; k++){
			assertTrue( "rock " + k + " lands after rock " + (k - 1), landed[k] > landed[k - 1] );
			assertEquals( "about " + EarthParticle.TRICKLE + " s apart", EarthParticle.TRICKLE, landed[k] - landed[k - 1], 0.12f );
		}
	}

	@Test
	public void theCursesFirstStrandFlashesOnceAndTheCapsHold(){
		Emitter e = new Emitter();
		for (int i = 0; i < 12; i++) ShadowParticle.CURSE.emit( e, i, 40, 40 );
		for (int i = 0; i < 30; i++){
			e.update();
		}
		Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
		for (int i = 0; i < 3; i++) lights.update();
		//the flash went into the light layer's loose light, one PLUS_5
		int flashes = 0;
		for (Gizmo g : lights.membersView()){
			if (g instanceof Group) flashes += living( (Group) g, FxParticle.class );
		}
		assertTrue( "at most one flash", flashes <= 1 );
		//glows and cell pictures keep to their caps
		for (int i = 0; i < 100; i++) GasGlow.at( i, i, 0xFFFFFF, 0.1f );
		assertTrue( GasGlow.live() <= GasGlow.CAP );
		for (int c = 0; c < W * W; c++) CellOverlay.keep( CellOverlay.TINT, c, 8, 8, FxFrames.Light.POOL_15, true, 0, 0.1f, 0, 1, 1, false );
		assertEquals( CellOverlay.CAPS[CellOverlay.TINT], CellOverlay.live( CellOverlay.TINT ) );
		assertFalse( CellOverlay.anyWeb() );
	}
}

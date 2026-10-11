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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Chains;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Identification;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EnergyParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.InfusionParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LaserParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PurpleParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.RainbowParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
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
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The arcane area at work, headless (stand-in layers for the game's scene, a floor with a pool):
 * nothing touches the GPU while its pictures are made or run; a bolt draws the game's Random once,
 * in its reset, and calls back on the very frame it always did, its impact made first; beams,
 * flares and specks draw none of it, the glyph of knowing its twenty; every cap holds; beams and
 * chains let go of what they built; and the pure parts (the prism's timeline, a flare's sizes,
 * a beam's thickness, a chain's sag, a sign's pop, the particles' steps) are what they say.
 */
public class ArcaneFxTest {

	private static final int W = 24;

	private GL20 savedGl, savedGl20;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	private Group[] layers;
	private Group world;

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
		Emitter.freezeEmitters = false;
		FxRandom.seedForTests( 9 );
		newScene();
		Dungeon.level = pond();
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 2 + 6 * W;
		Game.elapsed = 1 / 30f;
	}

	//a new game scene, as on a change of level: new layers and a new world, the kit's counts reset,
	//and nothing of the last one let go of
	private void newScene(){
		FxBudget.reset();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		WaterFX.sceneCreated();
		layers = new Group[GameScene.Layer.values().length];
		for (int i = 0; i < layers.length; i++) layers[i] = new Group();
		GameScene.useLayersForTests( layers );
		Fx.sceneCreated();
		world = new Group();
	}

	@After
	public void tearDown(){
		GameScene.useLayersForTests( null );
		FxBudget.reset();
		FxLight.reset();
		FxDecal.reset();
		FxRing.reset();
		WaterFX.sceneCreated();
		Gdx.gl = savedGl;
		Gdx.gl20 = savedGl20;
		Game.elapsed = savedElapsed;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	//a floor with a pool in columns 12-17, rows 2-10; all of it seen
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
		for (int y = 2; y < 11; y++) for (int x = 12; x < 18; x++) l.map[x + y * W] = Terrain.WATER;
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

	private void frame(){
		FxBudget.frame( Game.elapsed );
		WaterFX.frame();
		world.update();
		for (Group g : layers) g.update();
	}

	private void frames( int n ){
		for (int i = 0; i < n; i++) frame();
	}

	//pictures alive in a group and the groups in it (an emitter's particles too)
	private static int pictures( Group g ){
		int n = 0;
		for (Gizmo m : g.membersView()){
			if (m == null || !m.alive) continue;
			if (m instanceof Group) n += pictures( (Group) m );
			else if (m instanceof Visual) n++;
		}
		return n;
	}

	private int picturesEverywhere(){
		int n = pictures( world );
		for (Group g : layers) n += pictures( g );
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

	private int livingEverywhere( Class<?> c ){
		int n = living( world, c );
		for (Group g : layers) n += living( g, c );
		return n;
	}

	//the game's Random, seeded, `skip` draws on: what its next Float would be
	private static float after( long seed, int skip ){
		Random.pushGenerator( seed );
		for (int i = 0; i < skip; i++) Random.Float();
		float f = Random.Float();
		Random.popGenerator();
		return f;
	}

	// ------------------------------------------------------------------ bolts

	private static final int[] BOLTS = { MagicMissile.MAGIC_MISSILE, MagicMissile.FROST, MagicMissile.FIRE,
			MagicMissile.CORROSION, MagicMissile.FOLIAGE, MagicMissile.FORCE, MagicMissile.BEACON, MagicMissile.SHADOW,
			MagicMissile.RAINBOW, MagicMissile.EARTH, MagicMissile.WARD, MagicMissile.SHAMAN_RED, MagicMissile.SHAMAN_BLUE,
			MagicMissile.SHAMAN_PURPLE, MagicMissile.SHAMAN_ELMO, MagicMissile.ELMO, MagicMissile.POISON,
			MagicMissile.LIGHT_MISSILE, MagicMissile.MAGIC_MISS_CONE, MagicMissile.FIRE_CONE, MagicMissile.FOLIAGE_CONE,
			MagicMissile.PURPLE_CONE, MagicMissile.SPECK + Speck.TOXIC };

	//the frame the bolt of the old code called back on: time -= dt till it is not above 0
	private static int oldArrival( float dist, float dt ){
		float time = dist / 200f;
		int n = 0;
		while (true){
			n++;
			if ((time -= dt) <= 0) return n;
		}
	}

	//the bolts whose trails are this area's own (the rest pour other areas' particles, as ever)
	private static boolean ownTrail( int type ){
		switch (type){
			case MagicMissile.CORROSION: case MagicMissile.SHADOW: case MagicMissile.ELMO: case MagicMissile.POISON:
				return false;
			default:
				return true;
		}
	}

	@Test
	public void aBoltDrawsOneRandomInItsResetAndCallsBackOnItsOldFrameItsImpactMadeFirst(){
		for (int type : BOLTS){
			tearDown();
			setUp();
			failOnGl();
			PointF from = new PointF( 40, 100 ), to = new PointF( 40 + 150, 100 - 30 );
			float dist = (float)Math.hypot( 150, 30 );
			final int[] calledAt = { -1 };
			final int[] frameNow = { 0 };
			final int[] rings = { -1 };
			float expected = after( 7, 1 );

			//its reset draws one
			Random.pushGenerator( 7 );
			MagicMissile m = (MagicMissile) world.recycle( MagicMissile.class );
			m.reset( type, from, to, () -> {
				calledAt[0] = frameNow[0];
				rings[0] = FxRing.live();
			} );
			float next = Random.Float();
			Random.popGenerator();
			assertEquals( "type " + type + ": its reset drew one", expected, next, 0f );

			//and its flight none of its own (its trail's particles too, where they are this area's)
			Random.pushGenerator( 7 );
			int arrival = oldArrival( dist, Game.elapsed );
			for (int i = 1; i <= arrival + 30; i++){
				frameNow[0] = i;
				frame();
			}
			float later = Random.Float();
			Random.popGenerator();
			if (ownTrail( type )){
				assertEquals( "type " + type + ": its flight drew none", after( 7, 0 ), later, 0f );
			}
			assertEquals( "type " + type + " calls back on the frame it always did", arrival, calledAt[0] );
			if (type == MagicMissile.MAGIC_MISSILE){
				assertTrue( "its impact's ring is there as it calls back", rings[0] > 0 );
			}
			assertEquals( "type " + type + ": its rich count let go", 0, BoltFx.richLive() );
			assertEquals( "type " + type + ": its cone count let go", 0, BoltFx.conesLive() );
		}
	}

	@Test
	public void aBoltsTrailKeepsToFifty(){
		for (int i = 0; i < 18; i++){
			tearDown();
			setUp();
			failOnGl();
			MagicMissile b = (MagicMissile) world.recycle( MagicMissile.class );
			b.reset( BOLTS[i], new PointF( 30, 30 ), new PointF( 330, 30 ), null );
			int most = 0;
			for (int f = 0; f < 60; f++){
				frame();
				if (b.on) most = Math.max( most, b.countLiving() );
			}
			assertTrue( "type " + BOLTS[i] + ": a trail of " + most, most <= 50 );
		}
	}

	@Test
	public void onlyTheFirstFourBoltsAtOnceAreRich(){
		failOnGl();
		MagicMissile[] bolts = new MagicMissile[8];
		for (int i = 0; i < bolts.length; i++){
			bolts[i] = (MagicMissile) world.recycle( MagicMissile.class );
			bolts[i].reset( BOLTS[i], new PointF( 30, 30 + 12 * i ), new PointF( 330, 30 + 12 * i ), null );
		}
		int mostRich = 0;
		for (int f = 0; f < 50; f++){
			frame();
			mostRich = Math.max( mostRich, BoltFx.richLive() );
		}
		assertEquals( BoltFx.RICH, mostRich );
		frames( 60 );
		assertEquals( 0, BoltFx.richLive() );
		assertEquals( "every head let go of", 0, livingEverywhere( ArcImage.class ) - livingEverywhere( ArcFlash.class ) );
	}

	@Test
	public void aConeOfManyRaysPoursAtMostAHundredAndSixty(){
		failOnGl();
		MagicMissile[] rays = new MagicMissile[12];
		for (int i = 0; i < rays.length; i++){
			rays[i] = (MagicMissile) world.recycle( MagicMissile.class );
			rays[i].reset( MagicMissile.MAGIC_MISS_CONE, new PointF( 30, 100 ), new PointF( 130, 40 + 10 * i ), i == 6 ? () -> {} : null );
		}
		int most = 0;
		for (int f = 0; f < 40; f++){
			frame();
			int n = 0;
			for (MagicMissile r : rays) n += r.countLiving();
			most = Math.max( most, n );
		}
		assertTrue( "a cone of " + most, most <= 160 );
		assertTrue( most > 0 );
	}

	// ------------------------------------------------------------------ beams

	@Test
	public void beamsDrawNoRandomTouchNoGpuKeepThreeFullAndLetGoOfEverything(){
		failOnGl();
		float expected = after( 3, 0 );
		Random.pushGenerator( 3 );
		PointF s = new PointF( 20, 20 );
		Beam[] beams = { new Beam.DeathRay( s, new PointF( 180, 20 ) ), new Beam.LightRay( s, new PointF( 180, 40 ) ),
				new Beam.SunRay( s, new PointF( 180, 60 ) ), new Beam.HealthRay( s, new PointF( 180, 80 ) ),
				new Beam.SuperNovaRay( s, new PointF( 180, 100 ), 2f ), new Beam.DeathRay( s, new PointF( 180, 120 ) ).lite() };
		for (Beam b : beams) world.add( b );
		frame();
		assertTrue( RayFx.fullLive() <= RayFx.FULL );
		assertEquals( RayFx.FULL, RayFx.fullLive() );
		frames( 90 );
		assertEquals( "no Random drawn", expected, Random.Float(), 0f );
		Random.popGenerator();
		for (Beam b : beams) assertFalse( "a beam gone", b.exists );
		assertEquals( 0, RayFx.fullLive() );
		assertEquals( "its halos, beads and pulses let go of", 0, livingEverywhere( ArcImage.class ) - livingEverywhere( ArcFlash.class ) );
	}

	@Test
	public void theHeavierSupernovaShotsAreAsThickAsTheirWeight(){
		failOnGl();
		PointF s = new PointF( 20, 20 );
		Beam.SuperNovaRay[] rays = { new Beam.SuperNovaRay( s, new PointF( 180, 20 ), 1f ),
				new Beam.SuperNovaRay( s, new PointF( 180, 60 ), 2f ), new Beam.SuperNovaRay( s, new PointF( 180, 100 ), 3f ) };
		for (Beam b : rays) world.add( b );
		frames( 5 );
		for (int i = 0; i < rays.length; i++) assertEquals( "weight " + (i + 1), i + 1, rays[i].scale.y, 0f );
		//and it thins in whole rows
		float last = Float.MAX_VALUE;
		for (int f = 0; f < 60 && rays[2].timeLeft > 0; f++){
			float rows = rays[2].scale.y * rays[2].height;
			assertEquals( Math.round( rows ), rows, 1e-4f );
			assertTrue( rows <= last );
			last = rows;
			frame();
		}
		assertEquals( 0.5f, Beam.rowsThick( 0.25f, 2f, 8 ), 0f );
		assertEquals( 1 / 8f, Beam.rowsThick( 0.25f, 0.4f, 8 ), 0f );
	}

	@Test
	public void aNewSceneCountsItsOwnFullBeamsAndRichBolts(){
		failOnGl();
		PointF s = new PointF( 20, 20 );
		for (int i = 0; i < RayFx.FULL; i++) world.add( new Beam.HealthRay( s, new PointF( 180, 20 + 20 * i ) ) );
		for (int i = 0; i < BoltFx.RICH; i++){
			MagicMissile m = (MagicMissile) world.recycle( MagicMissile.class );
			m.reset( MagicMissile.MAGIC_MISSILE, new PointF( 30, 30 + 12 * i ), new PointF( 330, 30 + 12 * i ), null );
		}
		frame();
		assertEquals( RayFx.FULL, RayFx.fullLive() );
		assertEquals( BoltFx.RICH, BoltFx.richLive() );

		//the hero takes the stairs with all of them still showing: they go with the old scene,
		//never let go of
		newScene();
		assertEquals( 0, RayFx.fullLive() );
		assertEquals( 0, BoltFx.richLive() );
		world.add( new Beam.HealthRay( s, new PointF( 180, 20 ) ) );
		MagicMissile m = (MagicMissile) world.recycle( MagicMissile.class );
		m.reset( MagicMissile.MAGIC_MISSILE, new PointF( 30, 30 ), new PointF( 330, 30 ), null );
		frame();
		assertEquals( "the new scene's beam is full", 1, RayFx.fullLive() );
		assertEquals( "the new scene's bolt is rich", 1, BoltFx.richLive() );
		frames( 90 );
		assertEquals( 0, RayFx.fullLive() );
		assertEquals( 0, BoltFx.richLive() );
	}

	@Test
	public void aBeamStrikesInHoldsThenThinsInWholeSteps(){
		assertEquals( 0.25f, Beam.thickness( 0f, 1f ), 0f );
		assertEquals( 0.5f, Beam.thickness( 0.03f, 1f ), 0f );
		assertEquals( 1f, Beam.thickness( 0.1f, 0.9f ), 0f );
		float last = 1f;
		for (float left = 0.5f; left > 0; left -= 0.01f){
			float t = Beam.thickness( 1f, left );
			assertTrue( t == 1f || t == 0.75f || t == 0.5f || t == 0.25f );
			assertTrue( "it only thins", t <= last );
			last = t;
		}
		assertEquals( 1f, Beam.coreAlpha( 0.8f ), 0f );
		assertEquals( 0.4f, Beam.coreAlpha( 0f ), 1e-6f );
	}

	@Test
	public void thePrismLaserKeepsItsTimelineAndItsThirtyFiveDraws(){
		assertEquals( 1, PrismFx.coreWidth( 0f ) );
		assertEquals( 3, PrismFx.coreWidth( 0.02f ) );
		assertEquals( 5, PrismFx.coreWidth( 0.04f ) );
		assertEquals( 5, PrismFx.coreWidth( 0.44f ) );
		assertEquals( 3, PrismFx.coreWidth( 0.45f ) );
		assertEquals( 1, PrismFx.coreWidth( 0.7f ) );
		assertEquals( 3, PrismFx.fringeOffset( 0.39f ) );
		assertEquals( 4, PrismFx.fringeOffset( 0.4f ) );
		assertEquals( 5, PrismFx.fringeOffset( 0.6f ) );
		assertEquals( 0f, PrismFx.fringeAlpha( 0.03f ), 0f );
		assertEquals( 0.9f, PrismFx.fringeAlpha( 0.2f ), 0f );
		assertEquals( 0f, PrismFx.coreAlpha( PrismFx.CORE_END ), 1e-6f );
		assertEquals( Element.PRISM.ramp.stop( 0 ), PrismFx.hueAt( 0f ) );
		assertEquals( Element.PRISM.ramp.stop( Element.PRISM.ramp.size() - 1 ), PrismFx.hueAt( 1f ) );

		failOnGl();
		float expected = after( 5, 0 );
		Random.pushGenerator( 5 );
		//over the floor: 11 cells of it, the pool's well clear
		Beam.PrismRay ray = new Beam.PrismRay( new PointF( 20, 200 ), new PointF( 196, 200 ) );
		world.add( ray );
		int most = 0;
		for (int f = 0; f < 40; f++){
			frame();
			//its pictures: the ones it made (pools, glints, ghosts) and the light layer's few carriers
			most = Math.max( most, picturesEverywhere() );
		}
		assertTrue( "the prism at its fullest drew " + most, most <= PrismFx.DRAWS + 2 );
		assertEquals( "no Random drawn", expected, Random.Float(), 0f );
		Random.popGenerator();
		frames( 10 );
		assertFalse( ray.exists );
	}

	@Test
	public void thePrismLaserWashesTheWaterItCrosses(){
		failOnGl();
		//across the pool's rows: 4 water cells under it
		Beam.PrismRay ray = new Beam.PrismRay( new PointF( 150, 100 ), new PointF( 290, 100 ) );
		world.add( ray );
		frame();
		assertTrue( living( layers[GameScene.Layer.SURFACE.ordinal()], WaterFX.Wash.class ) > 0 );
		//its washes and their glints are within its draws all the same
		int most = picturesEverywhere();
		for (int f = 0; f < 40; f++){
			frame();
			most = Math.max( most, picturesEverywhere() );
		}
		assertTrue( "the prism over the pool drew " + most, most <= PrismFx.DRAWS + 2 );
	}

	@Test
	public void thePrismLaserDownALongWaterKeepsItsThirtyFiveDraws(){
		failOnGl();
		//down the pool's middle column: 9 water cells under it, the hero beside it (none of its
		//glints thinned for distance)
		Dungeon.hero.pos = 11 + 6 * W;
		Beam.PrismRay ray = new Beam.PrismRay( new PointF( 14 * 16 + 8, 4 ), new PointF( 14 * 16 + 8, 200 ) );
		world.add( ray );
		int most = 0;
		for (int f = 0; f < 40; f++){
			frame();
			most = Math.max( most, picturesEverywhere() );
		}
		//its draws, and its core's carrier in the light layer
		assertTrue( "the prism down the pool drew " + most, most <= PrismFx.DRAWS + 1 );
		//every wet cell washed while GLINTS allow, and their glints out of what is left
		assertEquals( 9, PrismFx.washes( 9 ) );
		assertEquals( 7, PrismFx.wetGlints( 9 ) );
		assertEquals( 10, PrismFx.wetGlints( 5 ) );
		assertEquals( Math.min( PrismFx.GLINTS, WaterFX.WASHES ), PrismFx.washes( 40 ) );
		for (int wet = 0; wet < 40; wet++){
			int w = PrismFx.washes( wet );
			assertTrue( w + PrismFx.wetGlints( w ) <= PrismFx.GLINTS );
		}
	}

	// ------------------------------------------------------------------ flares

	@Test
	public void flaresPaintTheNearestStarWithoutGoingOverAndMakeNoTexture() throws Exception {
		assertEquals( 1, Flare.scaleFor( 12 ) );
		assertEquals( 2, Flare.scaleFor( 13 ) );
		int[] star8 = { 15, 23, 31, 47 };
		//32 px of radius: 64 across, a 31 px star at x2
		assertEquals( 2, Flare.frameFor( star8, 32 ) );
		//12: 24 across, the 23 at x1
		assertEquals( 1, Flare.frameFor( star8, 12 ) );
		//20: 40 across, 20 at x2: the 15
		assertEquals( 0, Flare.frameFor( star8, 20 ) );
		//a burst steps up through the sizes, holds, and steps back one at 70%
		assertEquals( 0, Flare.sizeAt( 2, 0f ) );
		assertEquals( 2, Flare.sizeAt( 2, 0.5f ) );
		assertEquals( 1, Flare.sizeAt( 2, 0.8f ) );
		for (float p = 0; p < 0.25f; p += 0.01f) assertTrue( Flare.sizeAt( 2, p ) <= Flare.sizeAt( 2, p + 0.01f ) );
		assertEquals( 0f, Flare.alphaAt( 1f ), 1e-6f );

		java.lang.reflect.Field all = TextureCache.class.getDeclaredField( "all" );
		all.setAccessible( true );
		failOnGl();
		float expected = after( 13, 0 );
		Random.pushGenerator( 13 );
		int before = ((Map<?, ?>) all.get( null )).size();
		for (int i = 0; i < 100; i++) new Flare( 4 + i % 9, 8 + i );
		assertEquals( "no texture of its own", before, ((Map<?, ?>) all.get( null )).size() );
		assertEquals( "no Random drawn", expected, Random.Float(), 0f );
		Random.popGenerator();
	}

	@Test
	public void aBurstOnASpriteStaysBehindItAndOneAtAPointIsCarriedOverTheTint(){
		failOnGl();
		Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
		Image sprite = new Image();
		sprite.x = 40;
		sprite.y = 40;
		world.add( sprite );
		Flare behind = new Flare( 6, 32 ).show( sprite, 2f );
		frame();
		assertSame( "behind its sprite, in its group", world, behind.parent );
		assertTrue( "drawn before the sprite", world.indexOf( behind ) < world.indexOf( sprite ) );
		assertEquals( "nothing carries it over the sprite", 0, lights.length - living( lights, FxLight.class ) );
		assertEquals( "a pool of its light at its feet", 1, living( lights, FxLight.class ) );

		new Flare( 6, 32 ).show( world, new PointF( 80, 40 ), 1f );
		frame();
		assertEquals( "one at a point is carried over the tint", 1, lights.length - living( lights, FxLight.class ) );
	}

	@Test
	public void aDarkFlareAtAPointStaysUnderTheTint(){
		failOnGl();
		Group lights = layers[GameScene.Layer.LIGHTS.ordinal()];
		new Flare( 6, 32 ).color( 0x000000, false ).show( world, new PointF( 80, 40 ), 1f );
		frame();
		assertEquals( "nothing carries it over the tint", 0, lights.length - living( lights, FxLight.class ) );
	}

	@Test
	public void anAurasPoolLiesUnderItOnlyWhileItsCharShows(){
		failOnGl();
		Image sprite = new Image();
		sprite.x = 40;
		sprite.y = 40;
		world.add( sprite );
		Flare aura = new Flare( 5, 16 ).color( 0xFFFF00, true ).show( sprite, 0 );
		frame();
		assertEquals( "a pool under a shown aura", 1, FxLight.livePersistent() );
		//its char walks under the fog: its sprite, and with it its aura, hidden, while it follows on
		aura.visible = false;
		for (int f = 0; f < 10; f++){
			aura.point( new PointF( 40 + 4 * f, 40 ) );
			frame();
			assertEquals( "no light marks where it walks", 0, FxLight.livePersistent() );
			assertEquals( 0, living( layers[GameScene.Layer.LIGHTS.ordinal()], FxLight.class ) );
		}
		aura.visible = true;
		frame();
		assertEquals( "its pool back as it shows again", 1, FxLight.livePersistent() );
		aura.killAndErase();
		frames( 10 );
		assertEquals( 0, FxLight.livePersistent() );
	}

	@Test
	public void aFlaresInnerStarIsHalfItsSpan(){
		//24 px of radius: a 23 px star at x2, its inner the 9 at x2 (not the 23, as big as itself)
		assertEquals( 0, Flare.innerFrameFor( 24 ) );
		assertEquals( 0, Flare.innerFrameFor( 28 ) );
		assertEquals( 1, Flare.innerFrameFor( 32 ) );
		assertEquals( 0, Flare.innerFrameFor( 10 ) );
	}

	@Test
	public void pastTwelveBurstsTheOldestAreCutShort(){
		failOnGl();
		Flare[] f = new Flare[Flare.MOST + 3];
		for (int i = 0; i < f.length; i++){
			f[i] = new Flare( 6, 16 ).show( world, new PointF( 50, 50 ), 2f );
		}
		frame();
		assertEquals( Flare.MOST, Flare.bursts() );
		frames( (int)(Flare.CUT / Game.elapsed) + 2 );
		assertFalse( "the oldest was cut short", f[0].exists );
		assertTrue( "the newest plays on", f[f.length - 1].exists );
	}

	// ------------------------------------------------------------------ specks

	private static final int[] SPECKS = { Speck.HEALING, Speck.STAR, Speck.LIGHT, Speck.QUESTION, Speck.UP,
			Speck.SCREAM, Speck.BONE, Speck.WOOL, Speck.ROCK, Speck.NOTE, Speck.CHANGE, Speck.HEART, Speck.BUBBLE,
			Speck.STEAM, Speck.COIN, Speck.DOWN, Speck.HUNGER, Speck.DISCOVER, Speck.EVOKE, Speck.MASK, Speck.CROWN,
			Speck.RATTLE, Speck.JET, Speck.TOXIC, Speck.CORROSION, Speck.PARALYSIS, Speck.DUST, Speck.STENCH,
			Speck.FORGE, Speck.CONFUSION, Speck.RED_LIGHT, Speck.CALM, Speck.SMOKE, Speck.STORM, Speck.INFERNO,
			Speck.BLIZZARD, Speck.YELLOW_LIGHT, Speck.BLUE_LIGHT, Speck.POPGAS, Speck.SLOW, Speck.MIASMA, Speck.NECTAR,
			Speck.DEPRESSANT, Speck.FIREWIND, Speck.DIRTCLOUD, Speck.KNOWWIND, Speck.CLOUDOFCORR, Speck.THUNDER_STORM };

	@Test
	public void aSpeckOfLightIsAddedWhicheverWayItWasFirstAskedFor(){
		assertFalse( Speck.factory( Speck.YELLOW_LIGHT, false ).lightMode() );
		assertTrue( Speck.factory( Speck.YELLOW_LIGHT ).lightMode() );
		assertTrue( Speck.factory( Speck.LIGHT ).lightMode() );
		assertFalse( Speck.factory( Speck.LIGHT, false ).lightMode() );
		assertTrue( Speck.factory( Speck.LIGHT ).lightMode() );
		assertTrue( Speck.factory( Speck.DISCOVER ).lightMode() );
		assertTrue( Speck.factory( Speck.RED_LIGHT ).lightMode() );
		assertTrue( Speck.factory( Speck.BLUE_LIGHT ).lightMode() );
		assertFalse( Speck.factory( Speck.STEAM ).lightMode() );
		assertSame( Speck.factory( Speck.LIGHT ), Speck.factory( Speck.LIGHT, true ) );
		assertEquals( Speck.LIGHT, Speck.typeForFactory( Speck.factory( Speck.LIGHT ) ) );
	}

	@Test
	public void everySpeckStaysOnWholeScalesAndQuarterTurnsAndDrawsNoRandom(){
		failOnGl();
		float expected = after( 17, 0 );
		Random.pushGenerator( 17 );
		FxEmitter e = new FxEmitter();
		world.add( e );
		for (int type : SPECKS){
			e.pos( 60, 60, 8, 8 );
			e.burst( Speck.factory( type ), 3 );
			for (int f = 0; f < 45; f++){
				frame();
				for (Gizmo g : e.membersView()){
					if (!(g instanceof Speck) || !g.alive) continue;
					Speck s = (Speck) g;
					float sx = Math.abs( s.scale.x ), sy = Math.abs( s.scale.y );
					assertTrue( "type " + type + " scaled " + sx, sx == 1 || sx == 2 || sx == 3 );
					assertEquals( "type " + type, sx, sy, 0f );
					assertEquals( "type " + type + " turned " + s.angle, 0f, s.angle % 90f, 0f );
				}
			}
		}
		assertEquals( "no Random drawn", expected, Random.Float(), 0f );
		Random.popGenerator();
	}

	// ------------------------------------------------------------------ the arcane

	@Test
	public void theGlyphOfKnowingDrawsItsTwentyAndNoMoreAndGoesWhenDone(){
		failOnGl();
		float expected = after( 21, 20 );
		Random.pushGenerator( 21 );
		Identification id = new Identification( new PointF( 60, 60 ) );
		world.add( id );
		frames( 80 );
		assertEquals( "twenty in the constructor, none after", expected, Random.Float(), 0f );
		Random.popGenerator();
		assertFalse( id.exists );
	}

	//the frame the chains of the old code called back on: spent += dt past its duration
	private static int oldBite( float dist, float dt ){
		float duration = dist / 320f + 0.2f, spent = 0;
		int n = 0;
		while (true){
			n++;
			if ((spent += dt) > duration) return n;
		}
	}

	@Test
	public void chainsBiteOnTheirOldFrameThenPullBackAndLetGo(){
		failOnGl();
		final int[] bitAt = { -1 };
		final int[] now = { 0 };
		PointF from = new PointF( 30, 60 ), to = new PointF( 150, 90 );
		Chains plain = new Chains( from, to, Effects.Type.CHAIN, () -> bitAt[0] = now[0] );
		Chains ethereal = new Chains( from, new PointF( 150, 30 ), Effects.Type.ETHEREAL_CHAIN, null );
		world.add( plain );
		world.add( ethereal );
		int bite = oldBite( (float)Math.hypot( 120, 30 ), Game.elapsed );
		for (int i = 1; i <= bite + 20; i++){
			now[0] = i;
			frame();
		}
		assertEquals( bite, bitAt[0] );
		assertFalse( plain.exists );
		assertFalse( ethereal.exists );
		assertEquals( "their heads and glows let go of", 0, livingEverywhere( ArcImage.class ) - livingEverywhere( ArcFlash.class ) );
		assertEquals( Chains.SAG, Chains.sag( 0f ), 1e-6f );
		assertEquals( Chains.SAG_TIGHT, Chains.sag( 1f ), 1e-6f );
		assertTrue( Chains.sag( 0.5f ) < Chains.SAG && Chains.sag( 0.5f ) > Chains.SAG_TIGHT );
	}

	@Test
	public void aSpellsSignPopsUpThreeAndSettlesOne(){
		assertEquals( 0f, SpellSprite.pop( 0f ), 0f );
		assertEquals( SpellSprite.RISE, SpellSprite.pop( 0.6f ), 1e-6f );
		assertEquals( SpellSprite.RISE - SpellSprite.SETTLE, SpellSprite.pop( 1f ), 1e-6f );
		//it floats while it shows: whole pixels, a pixel either way over a cycle
		int lo = 0, hi = 0;
		for (float t = 0; t < 1f / SpellSprite.FLOAT_HZ; t += 0.01f){
			lo = Math.min( lo, SpellSprite.floatAt( t ) );
			hi = Math.max( hi, SpellSprite.floatAt( t ) );
		}
		assertEquals( -1, lo );
		assertEquals( 1, hi );
	}

	@Test
	public void theArcaneParticlesStepInWholePixelsAndDrawNoRandom(){
		assertEquals( 1, PurpleParticle.sizeAt( 0f ) );
		assertEquals( 3, PurpleParticle.sizeAt( 0.5f ) );
		assertEquals( 5, PurpleParticle.sizeAt( 0.99f ) );
		assertEquals( 0xFFFFFF, PurpleParticle.colorAt( 0f ) );
		assertEquals( PurpleParticle.ROSE, PurpleParticle.colorAt( PurpleParticle.HOT_FOR ) );
		for (int i = 0; i < 7; i++) assertEquals( Element.PRISM.ramp.stop( i ), RainbowParticle.hue( i ) );
		assertSame( InfusionParticle.factory( 0x123456 ), InfusionParticle.factory( 0x123456 ) );

		failOnGl();
		float expected = after( 23, 0 );
		Random.pushGenerator( 23 );
		Emitter e = new FxEmitter();
		world.add( e );
		Emitter.Factory[] all = { PurpleParticle.BURST, PurpleParticle.MISSILE, EnergyParticle.FACTORY, LaserParticle.FACTORY,
				LaserParticle.BURST, RainbowParticle.BURST, InfusionParticle.factory( 0x40C0FF ), InfusionParticle.spiralIn( 0xFFE070 ),
				MagicMissile.MagicParticle.FACTORY, MagicMissile.MagicParticle.ATTRACTING, MagicMissile.EarthParticle.BURST,
				MagicMissile.EarthParticle.ATTRACT, MagicMissile.ShamanParticle.RED, MagicMissile.WhiteParticle.FACTORY,
				MagicMissile.WhiteParticle.WALL, MagicMissile.SlowParticle.FACTORY, MagicMissile.ForceParticle.FACTORY,
				MagicMissile.WardParticle.UP, BoltTrails.FIRE, BoltTrails.FIRE_CONE, BoltTrails.LEAF, BoltTrails.LEAF_CONE };
		for (Emitter.Factory f : all){
			e.pos( 50, 50, 6, 6 );
			e.burst( f, 8 );
			for (int i = 0; i < 40; i++){
				frame();
				for (Gizmo g : e.membersView()){
					if (g instanceof Image && g.alive && !(g instanceof xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle)){
						float s = ((Image) g).scale.x;
						assertEquals( "whole pixels: " + s, Math.round( s ), s, 0f );
					}
				}
			}
		}
		assertEquals( "no Random drawn", expected, Random.Float(), 0f );
		Random.popGenerator();
	}

	@Test
	public void theItemsEffectsDrawNoRandomAndTouchNoGpu(){
		failOnGl();
		float expected = after( 29, 0 );
		Random.pushGenerator( 29 );
		ItemFx.shatter( 40, 40, 0x9050FF );
		ItemFx.shatter( 13 + 5 * W, 0x9050FF );
		ItemFx.teleport( 3 + 3 * W, 14 + 4 * W, true, true );
		ItemFx.secret( 5 + 5 * W );
		ItemFx.bloom( 4 + 4 * W );
		BoltFx.impact( MagicMissile.FIRE, 100, 100, 1f );
		BoltFx.impact( MagicMissile.FROST, 14 * 16 + 8, 5 * 16, 1f );
		frames( 60 );
		assertEquals( "no Random drawn", expected, Random.Float(), 0f );
		Random.popGenerator();
		assertTrue( FxBudget.live( FxBudget.PARTICLES ) >= 0 );
	}
}

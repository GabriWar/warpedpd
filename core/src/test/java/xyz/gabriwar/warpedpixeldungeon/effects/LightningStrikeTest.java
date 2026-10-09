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
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.watabou.noosa.Camera;
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
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A lightning of the game striking in the hero's sight (Lightning, WeatherOverlay.struck): on its
 * first frame the storm's flash, under the one gate a storm's bolts flash under (0.7 s), and the
 * storm's crack for a bolt in sight, at most once in 2.5 s; none for one he does not see, for a
 * cross through a cell or a hop of a cell, or for a storm's own bolt (the overlay's). It is drawn
 * on the weather's layer, over the day/night tint, by a stand-in that goes when it does. The gates
 * themselves, on a clock handed in.
 */
public class LightningStrikeTest {

	private static final int W = 20;

	private GL20 savedGl, savedGl20;
	private float savedElapsed;
	private Level savedLevel;
	private Hero savedHero;
	private Camera savedCam;
	private Supplier<WeatherOverlay> savedLayer;

	private Level level;
	private WeatherOverlay sky;
	private Group stage;

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
		savedCam = Camera.main;
		savedLayer = Lightning.layer;
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
	public void tearDown(){
		Lightning.layer = savedLayer;
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

	private int carried(){
		int n = 0;
		for (Gizmo g : sky.membersView()){
			if (g != null && g.alive && g.getClass().getSimpleName().equals( "Carried" )) n++;
		}
		return n;
	}

	//a lightning from the hero to a cell, put on the stage and run its first frame
	private Lightning zap( int to ){
		Lightning l = new Lightning( Dungeon.hero.pos, to, null );
		stage.add( l );
		stage.update();
		return l;
	}

	//the overlay's frames, its clock with them
	private void run( float seconds ){
		for (float t = 0f; t < seconds - 1e-4f; t += Game.elapsed) sky.update();
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
	public void aStrikeInSightFlashesAndCracksOnItsFirstFrame(){
		zap( at( 9, 10 ) );
		assertEquals( 1, count( LightningFlash.class ) );
		assertFalse( "cracked", sky.cracks.open( 0f ) );
		assertTrue( sky.cracks.open( WeatherOverlay.THUNDER_GAP ) );
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
		Lightning cross = new Lightning( at( 8, 10 ), at( 10, 10 ), null ).noGlow();
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
	public void theSkyFlashesOnceAGapForStormsAndLightningAlike(){
		zap( at( 9, 10 ) );
		zap( at( 10, 12 ) );
		assertEquals( "one flash for two at once", 1, count( LightningFlash.class ) );
		//a storm's bolt in sight inside the gap: drawn, but no flash of its own either
		StormStrikes.show( level, at( 12, 12 ), true );
		sky.update();
		assertEquals( 1, count( LightningFlash.class ) );

		//the gap gone by (and the first flash with it): the next flashes, but its crack is held
		run( 0.75f );
		assertEquals( 0, count( LightningFlash.class ) );
		zap( at( 9, 10 ) );
		assertEquals( 1, count( LightningFlash.class ) );
		assertFalse( "the crack's gap is longer", sky.cracks.open( 0.8f ) );

		//a storm's bolt that flashes closes the gate for lightning of the game too
		run( 0.75f );
		StormStrikes.show( level, at( 12, 12 ), true );
		sky.update();
		assertEquals( 1, count( LightningFlash.class ) );
		zap( at( 9, 10 ) );
		assertEquals( 1, count( LightningFlash.class ) );
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
		window.add( new Lightning( Dungeon.hero.pos, at( 9, 10 ), null ) );
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
}

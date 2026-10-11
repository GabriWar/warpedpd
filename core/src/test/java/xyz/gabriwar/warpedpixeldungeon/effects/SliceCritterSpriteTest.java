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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The slices' critters drawn the right way round, headless: the circling eagle banks into
 * the turn it is flying (its foreshortened wing on the inside of the circle), and a marmot
 * dives head first toward the rock its burrow is under, whichever way it was looking. A fish
 * under the water bursts away from a stir of it within its reach and keeps to the water, comes
 * to look at food, and is gone once its water freezes.
 */
public class SliceCritterSpriteTest {

	private static final float DT = 1f / 60f;
	private float elapsed;
	private boolean freeze;

	@BeforeClass
	public static void up(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void save(){
		elapsed = Game.elapsed;
		freeze = Emitter.freezeEmitters;
		Emitter.freezeEmitters = false;
	}

	@After
	public void restore(){
		Game.elapsed = elapsed;
		Emitter.freezeEmitters = freeze;
	}

	private static void frame( CritterSprite c ){
		Game.elapsed = DT;
		c.update();
	}

	@Test
	public void theEagleBanksIntoItsTurn(){
		int right = 0, left = 0;
		for (int i = 0; i < 40; i++){
			SliceCritterSprite.Eagle e = new SliceCritterSprite.Eagle( 400f, 400f );
			frame( e );
			float x0 = e.gx(), y0 = e.gy();
			frame( e );
			float x1 = e.gx(), y1 = e.gy();
			frame( e );
			float x2 = e.gx(), y2 = e.gy();
			//y down: a positive cross product is a clockwise circle on screen, a right turn
			float cross = (x1 - x0) * (y2 - y1) - (y1 - y0) * (x2 - x1);
			assertTrue( cross != 0f );
			//the bank frame is drawn turning left: a right turn is the mirrored frame
			assertEquals( "mirrored for a right turn", cross > 0f, e.flipHorizontal );
			if (cross > 0f) right++;
			else left++;
		}
		assertTrue( "both ways round", right > 0 && left > 0 );
	}

	// ------------------------------------------------------------ the fish under the water

	//a floor 20 x 20 of dry ground with a pool in columns and rows 4-15, all of it seen
	private static Level pool(){
		return pool( null );
	}

	//the same pool of a water texture (null: the level's own)
	private static Level pool( final String tex ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public String waterTex(){ return tex != null ? tex : super.waterTex(); }
		};
		l.setSize( 20, 20 );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int y = 4; y < 16; y++) for (int x = 4; x < 16; x++) l.map[x + y * 20] = Terrain.WATER;
		l.buildFlagMaps();
		l.heroFOV = new boolean[l.length()];
		Arrays.fill( l.heroFOV, true );
		return l;
	}

	private static void tick( CritterSprite c ){
		Game.timeTotal += DT;
		WaterFX.frame();
		frame( c );
	}

	private static boolean wet( CritterSprite c ){
		int cell = (int)(c.gx() / 16) + (int)((c.gy() - 1) / 16) * 20;
		return Dungeon.level.water[cell];
	}

	@Test
	public void aSwimmerBurstsAwayFromAStirWithinItsReachAndNeverLeavesTheWater(){
		Level was = Dungeon.level;
		try {
			Dungeon.level = pool();
			WaterFX.sceneCreated();
			SliceCritterSprite.Swimmer s = new SliceCritterSprite.Swimmer( 160f, 160f, 0f );
			for (int i = 0; i < 30; i++) tick( s );
			assertTrue( s.calm() );
			//a stir far off: nothing
			WaterFX.disturb( s.gx() + 60f, s.gy(), 24f, false );
			tick( s );
			tick( s );
			assertTrue( s.calm() );
			//a stone into the water beside it: off it bursts, away from it
			float x0 = s.gx();
			WaterFX.disturb( s.gx() - 10f, s.gy(), 40f, false );
			tick( s );
			tick( s );
			assertFalse( s.calm() );
			for (int i = 0; i < 12; i++) tick( s );
			assertTrue( "away and fast: " + (s.gx() - x0), s.gx() - x0 > 6f );
			//its tilt is level or 20 degrees, never between; it stays in the water
			while (s.exists){
				assertTrue( "tilt " + s.angle, s.angle == 0 || Math.abs( s.angle ) == 20 );
				assertTrue( "out of the water at " + s.gx() + "," + s.gy(), wet( s ) );
				tick( s );
			}
		} finally {
			Dungeon.level = was;
			WaterFX.sceneCreated();
		}
	}

	@Test
	public void foodDrawsASwimmerAndIceSendsItAway(){
		Level was = Dungeon.level;
		try {
			Dungeon.level = pool();
			WaterFX.sceneCreated();
			SliceCritterSprite.Swimmer s = new SliceCritterSprite.Swimmer( 150f, 150f, (float)Math.PI / 2 );
			for (int i = 0; i < 20; i++) tick( s );
			float fx = s.gx() + 30f, fy = s.gy();
			float before = Math.abs( fx - s.gx() );
			WaterFX.disturb( fx, fy, 24f, true );
			tick( s );
			tick( s );
			assertTrue( "food never startles", s.calm() );
			for (int i = 0; i < 40; i++) tick( s );
			assertTrue( "it came to look", Math.abs( fx - s.gx() ) < before );
			//the pool ices over under it: gone in 0.4 s
			for (int y = 4; y < 16; y++) for (int x = 4; x < 16; x++) Dungeon.level.map[x + y * 20] = Terrain.FROZEN_WATER;
			Dungeon.level.buildFlagMaps();
			float t = 0f;
			while (s.exists && t < 2f){
				tick( s );
				t += DT;
			}
			assertFalse( s.exists );
			assertTrue( "gone in " + t, t <= 0.4f + 2 * DT );
		} finally {
			Dungeon.level = was;
			WaterFX.sceneCreated();
		}
	}

	//a colour's luma, 0-255
	private static float luma( float r, float g, float b ){
		return 255f * (0.2126f * r + 0.7152f * g + 0.0722f * b);
	}

	private static float luma( int c ){
		return luma( ((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f );
	}

	@Test
	public void aSwimmerShowsPaleOnDarkWaterAndDarkOnLightWater(){
		Level was = Dungeon.level;
		try {
			//the caves' and the city's water is dark: the fish a pale ghost over its lightest
			for (String tex : new String[]{ Assets.Environment.WATER_CAVES, Assets.Environment.WATER_CITY }){
				Dungeon.level = pool( tex );
				WaterFX.sceneCreated();
				Element.Liquid l = WaterFX.liquid();
				SliceCritterSprite.Swimmer s = new SliceCritterSprite.Swimmer( 160f, 160f, 0f );
				float fish = luma( s.rm, s.gm, s.bm ), water = luma( l.body.stop( l.body.size() - 1 ) );
				assertTrue( l + ": pale, " + fish + " over " + water, fish > water + 40f );
				for (int i = 0; i < 40; i++) tick( s );
				assertTrue( l + " shown at " + s.shown, s.shown >= 0.4f );
			}
			//the sewers' and the prison's is lighter: a shadow under its darkest
			for (String tex : new String[]{ Assets.Environment.WATER_SEWERS, Assets.Environment.WATER_PRISON }){
				Dungeon.level = pool( tex );
				WaterFX.sceneCreated();
				Element.Liquid l = WaterFX.liquid();
				SliceCritterSprite.Swimmer s = new SliceCritterSprite.Swimmer( 160f, 160f, 0f );
				assertTrue( l + ": dark", luma( s.rm, s.gm, s.bm ) < luma( l.body.stop( 0 ) ) );
			}
		} finally {
			Dungeon.level = was;
			WaterFX.sceneCreated();
		}
	}

	@Test
	public void aMarmotDivesHeadFirstToItsRock(){
		for (int bx : new int[]{ 1, -1 }){
			SliceCritterSprite.Marmot m = new SliceCritterSprite.Marmot( 300f, 300f, bx, 0 );
			//it comes up looking out, away from its rock
			assertEquals( bx > 0, m.flipHorizontal );
			m.peek();
			for (int k = 0; k < 45; k++) frame( m );
			assertTrue( m.up() );
			//the hero on its open side: it turns to him, then dives
			m.alert( true, 300f - bx * 60f );
			assertEquals( bx > 0, m.flipHorizontal );
			m.dive();
			assertFalse( m.up() );
			assertEquals( "head first toward the rock at " + bx, bx < 0, m.flipHorizontal );
			float x = m.gx();
			for (int k = 0; k < 12; k++) frame( m );
			assertEquals( "and it hops that way", bx > 0, m.gx() > x );
		}
	}
}

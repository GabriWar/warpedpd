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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Rain mends the burnt ground under the open sky (OverworldLevel.rainMends): grass and brush
 * burnt to embers grow back while it rains, never while it is dry or snowing, never in the
 * caves, never while the cell still burns, and never a fallen star's scorch or embers that were
 * never growth.
 */
public class RainMendsTest {

	private static final int W = OverworldLevel.WIDTH, H = OverworldLevel.HEIGHT;
	private static final int[] BURNT = new int[20];
	static {
		for (int i = 0; i < BURNT.length; i++) BURNT[i] = 20 + i * 3 + (40 + i) * W;
	}

	private Level savedLevel;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		savedLevel = Dungeon.level;
		Actor.clear();
		ClimateManager.reset();
		Random.pushGenerator( 777 );
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		ClimateManager.debugPrecipOverride = 0.6f;
	}

	@After
	public void tearDown(){
		Random.popGenerator();
		Actor.clear();
		Dungeon.level = savedLevel;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
		ClimateManager.debugPrecipTypeOverride = null;
		ClimateManager.debugPrecipOverride = Float.NaN;
		ClimateManager.reset();
	}

	//a window of grass (the pristine land too), the burnt cells turned to embers
	private static OverworldLevel burnt( int altitude ) throws Exception {
		OverworldLevel l = new OverworldLevel( altitude );
		l.setSize( W, H );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		Arrays.fill( l.map, Terrain.GRASS );
		set( l, "pristine", l.map.clone() );
		for (int c : BURNT) l.map[c] = Terrain.EMBERS;
		l.buildFlagMaps();
		PathFinder.setMapSize( W, H );
		Dungeon.level = l;
		return l;
	}

	private static void set( Object o, String name, Object value ) throws Exception {
		Field f = OverworldLevel.class.getDeclaredField( name );
		f.setAccessible( true );
		f.set( o, value );
	}

	private static int embers( OverworldLevel l, int turns ){
		for (int t = 0; t < turns; t++) l.rainMends();
		int n = 0;
		for (int c : BURNT) if (l.map[c] == Terrain.EMBERS) n++;
		return n;
	}

	@Test
	public void theRainGrowsTheBurntGrassBack() throws Exception {
		OverworldLevel l = burnt( 0 );
		int left = embers( l, 20 );
		assertEquals( "a downpour mends some in 20 turns, not all: " + left, true, left > 0 && left < BURNT.length );
		assertEquals( "all of it in 400", 0, embers( l, 380 ) );
		for (int c : BURNT) assertEquals( Terrain.GRASS, l.map[c] );
		assertEquals( "the peaks are open sky too", 0, embers( burnt( 4 ), 400 ) );
	}

	@Test
	public void nothingMendsWithoutRain() throws Exception {
		ClimateManager.debugPrecipOverride = 0f;
		assertEquals( "dry", BURNT.length, embers( burnt( 0 ), 400 ) );
		ClimateManager.debugPrecipOverride = 0.6f;
		ClimateManager.debugPrecipTypeOverride = PrecipType.SNOW;
		assertEquals( "snow", BURNT.length, embers( burnt( 0 ), 400 ) );
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		assertEquals( "the caves", BURNT.length, embers( burnt( -3 ), 400 ) );
	}

	@Test
	public void whatStillBurnsOrWasNeverGrowthStays() throws Exception {
		OverworldLevel l = burnt( 0 );
		//still burning
		Blob fire = Blob.seed( BURNT[0], 4, Fire.class, l );
		//a campfire's embers on bare ground
		int[] pristine = (int[]) field( l, "pristine" );
		pristine[BURNT[1]] = Terrain.EMPTY;
		//a fallen star's scorch
		@SuppressWarnings("unchecked") Set<Long> scorch = (Set<Long>) field( l, "laidScorch" );
		scorch.add( OverworldLevel.worldKey( l.worldX() + BURNT[2] % W, l.worldY() + BURNT[2] / W ) );
		embers( l, 400 );
		assertEquals( "still burning", Terrain.EMBERS, l.map[BURNT[0]] );
		assertEquals( "never growth", Terrain.EMBERS, l.map[BURNT[1]] );
		assertEquals( "a star's scorch", Terrain.EMBERS, l.map[BURNT[2]] );
		assertEquals( Terrain.GRASS, l.map[BURNT[3]] );
		fire.fullyClear();
		embers( l, 400 );
		assertEquals( "once it is out", Terrain.GRASS, l.map[BURNT[0]] );
	}

	private static Object field( Object o, String name ) throws Exception {
		Field f = OverworldLevel.class.getDeclaredField( name );
		f.setAccessible( true );
		return f.get( o );
	}
}

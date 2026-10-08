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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.effects.CloudSea;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.AmbientSound;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldCritters.Sky;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SliceLife.Kind;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The caves' and the peaks' small life (SliceLife, CaveLife, PeakLife): which kinds may be
 * out where and when, where each may turn up (bats only on a face over open ground, fish only
 * in deep pools, marmots only by a boulder on thawed meadow, clouds only over open air), the
 * caps, the glows that keep to their patches, the sound gate, the gates that kept the slices
 * lifeless gone, and the debug scenes' places found.
 */
public class SliceLifeTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private Level saved;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		saved = Dungeon.level;
	}

	@After
	public void tearDown(){
		WorldModel.releaseSeasonShift();
		Dungeon.level = saved;
	}

	//a map of `w` x `h` all of `fill`
	private static int[] map( int w, int h, int fill ){
		int[] m = new int[w * h];
		Arrays.fill( m, fill );
		return m;
	}

	private static boolean[] all( int n, boolean v ){
		boolean[] b = new boolean[n];
		Arrays.fill( b, v );
		return b;
	}

	@Test
	public void everySliceHasItsOwnLife(){
		assertEquals( OverworldCritters.Slice.CAVES, OverworldCritters.sliceOf( -1 ) );
		assertEquals( OverworldCritters.Slice.CAVES, OverworldCritters.sliceOf( -12 ) );
		assertEquals( OverworldCritters.Slice.SURFACE, OverworldCritters.sliceOf( 0 ) );
		assertEquals( OverworldCritters.Slice.PEAKS, OverworldCritters.sliceOf( 1 ) );
		assertEquals( OverworldCritters.Slice.PEAKS, OverworldCritters.sliceOf( 10 ) );
	}

	//the two gates that kept the slices lifeless: the field is put on every slice, and a fight
	//on a slice is heard
	@Test
	public void slicesHaveCritters() throws Exception {
		Field noises = OverworldCritters.class.getDeclaredField( "NOISES" );
		noises.setAccessible( true );
		for (int a : new int[]{ -3, 3 }){
			PathFinder.setMapSize( W, H );
			WindowGenerator.Window w = WindowGenerator.generate( SEED, a, 100, 100, 0f );
			OverworldLevel ow = OverworldLevel.forNetwork( a, SEED, 100, 100, 0f, GameCalendar.Season.SUMMER, w.terrain, W, H );
			assertNotNull( ow );
			assertTrue( "the field is put on slice " + a, ow.hasCritters() );
			Dungeon.level = ow;
			OverworldCritters.Field f = new OverworldCritters.Field( ow );
			try {
				assertNotNull( "slice " + a + " has its own life", f.life );
				assertTrue( a < 0 ? f.life instanceof CaveLife : f.life instanceof PeakLife );
				OverworldCritters.noise( W * 40 + 40 );
				@SuppressWarnings("unchecked") ArrayList<Long> heard = (ArrayList<Long>) noises.get( null );
				synchronized (heard){
					assertEquals( "a fight on slice " + a + " is heard", 1, heard.size() );
					assertEquals( OverworldLevel.worldKey( ow.worldX() + 40, ow.worldY() + 40 ), (long) heard.get( 0 ) );
				}
			} finally {
				f.destroy();
			}
		}
	}

	@Test
	public void cavesKnowNoHourOrWeather(){
		Kind[] cave = { Kind.BATS, Kind.FLITTER, Kind.DRIP, Kind.GLOW, Kind.MOTE, Kind.FISH, Kind.SOUND };
		for (Kind k : cave){
			for (Phase p : Phase.values()){
				for (Sky s : Sky.values()){
					assertTrue( k + " at -1", SliceLife.allowed( k, -1, p, s, false, 0f ) );
					assertTrue( k + " at -12", SliceLife.allowed( k, -12, p, s, true, 20f ) );
				}
			}
			assertFalse( SliceLife.allowed( k, 0, Phase.DAY, Sky.CLEAR, false, 0f ) );
			assertFalse( SliceLife.allowed( k, 3, Phase.DAY, Sky.CLEAR, false, 0f ) );
		}
		for (Kind k : new Kind[]{ Kind.SPORE, Kind.EMBER }){
			assertFalse( SliceLife.allowed( k, -4, Phase.NIGHT, Sky.CLEAR, false, 0f ) );
			assertTrue( SliceLife.allowed( k, OverworldFauna.DEEP_CAVES, Phase.NIGHT, Sky.CLEAR, false, 0f ) );
			assertTrue( SliceLife.allowed( k, -12, Phase.DAY, Sky.FOUL, false, 0f ) );
		}
	}

	@Test
	public void peaksKeepTheirHours(){
		for (Phase p : Phase.values()){
			for (Sky s : Sky.values()){
				assertEquals( p == Phase.DAY && s != Sky.FOUL, SliceLife.allowed( Kind.EAGLE, 5, p, s, false, 0f ) );
				assertEquals( (p == Phase.DAWN || p == Phase.DAY) && s != Sky.FOUL,
						SliceLife.allowed( Kind.MARMOT, 3, p, s, false, 0f ) );
				assertFalse( "no marmot on frozen ground", SliceLife.allowed( Kind.MARMOT, 3, p, s, true, 0f ) );
			}
		}
		assertFalse( SliceLife.allowed( Kind.EAGLE, -2, Phase.DAY, Sky.CLEAR, false, 0f ) );
		assertFalse( SliceLife.allowed( Kind.EAGLE, 0, Phase.DAY, Sky.CLEAR, false, 0f ) );
		assertTrue( SliceLife.allowed( Kind.PLUME, 6, Phase.NIGHT, Sky.FOUL, true, 6f ) );
		assertFalse( SliceLife.allowed( Kind.PLUME, 6, Phase.DAY, Sky.CLEAR, true, 5.9f ) );
		assertFalse( SliceLife.allowed( Kind.PLUME, 6, Phase.DAY, Sky.CLEAR, false, 12f ) );
		assertFalse( SliceLife.allowed( Kind.PLUME, -6, Phase.DAY, Sky.CLEAR, true, 12f ) );
		assertFalse( SliceLife.allowed( Kind.CLOUDS, 3, Phase.DAY, Sky.CLEAR, false, 0f ) );
		assertTrue( SliceLife.allowed( Kind.CLOUDS, 4, Phase.NIGHT, Sky.FOUL, false, 0f ) );
		assertTrue( SliceLife.allowed( Kind.CLOUDS, 10, Phase.DAWN, Sky.CLEAR, false, 0f ) );
	}

	@Test
	public void batsHangWhereTheirFaceShows(){
		int w = 5;
		int[] m = map( w, 5, Terrain.WALL );
		int face = 1 * w + 2, below = 2 * w + 2;
		m[below] = Terrain.EMPTY;
		assertTrue( CaveLife.batWall( m, w, face ) );
		m[below] = Terrain.WATER;
		assertTrue( CaveLife.batWall( m, w, face ) );
		m[face] = Terrain.WALL_DECO;
		m[below] = Terrain.GRASS;
		assertTrue( CaveLife.batWall( m, w, face ) );
		m[face] = Terrain.WALL;
		m[below] = Terrain.WALL;
		assertFalse( "no face shows over rock", CaveLife.batWall( m, w, face ) );
		m[below] = Terrain.CHASM;
		assertFalse( "nor over a pit", CaveLife.batWall( m, w, face ) );
		m[below] = Terrain.EMPTY;
		m[face] = Terrain.MINE_CRYSTAL;
		assertFalse( CaveLife.batWall( m, w, face ) );
		m[face] = Terrain.EMPTY;
		assertFalse( CaveLife.batWall( m, w, face ) );
		//the bottom row has nothing under it
		m[4 * w + 2] = Terrain.WALL;
		assertFalse( CaveLife.batWall( m, w, 4 * w + 2 ) );
	}

	@Test
	public void blindFishOnlyInDeepPools(){
		int w = 9, h = 9;
		int[] pool = map( w, h, Terrain.EMPTY );
		for (int y = 1; y <= 7; y++) for (int x = 1; x <= 7; x++) pool[x + y * w] = Terrain.WATER;
		pool[4 + 4 * w] = Terrain.DEEP_WATER;
		assertTrue( CaveLife.caveFishWater( pool, w, h, 4 + 4 * w ) );
		assertTrue( "the shelf beside the deep", CaveLife.caveFishWater( pool, w, h, 3 + 4 * w ) );
		assertTrue( CaveLife.caveFishWater( pool, w, h, 2 + 3 * w ) );
		assertFalse( "too far from the deep", CaveLife.caveFishWater( pool, w, h, 1 + 1 * w ) );
		int[] shelf = map( w, h, Terrain.EMPTY );
		for (int y = 1; y <= 7; y++) for (int x = 1; x <= 7; x++) shelf[x + y * w] = Terrain.WATER;
		assertFalse( "a wadeable pool has no blind fish", CaveLife.caveFishWater( shelf, w, h, 4 + 4 * w ) );
		int[] puddle = map( w, h, Terrain.EMPTY );
		puddle[4 + 4 * w] = Terrain.DEEP_WATER;
		assertFalse( CaveLife.caveFishWater( puddle, w, h, 4 + 4 * w ) );
		assertFalse( CaveLife.caveFishWater( puddle, w, h, 3 + 4 * w ) );
	}

	@Test
	public void dripsFallIntoPoolsAndByTheWalls(){
		int w = 7, h = 7;
		int[] m = map( w, h, Terrain.EMPTY );
		m[3 + 3 * w] = Terrain.WATER;
		assertEquals( CaveLife.DRIP_POOL, CaveLife.dripKind( m, w, h, 3 + 3 * w ) );
		m[3 + 3 * w] = Terrain.DEEP_WATER;
		assertEquals( CaveLife.DRIP_POOL, CaveLife.dripKind( m, w, h, 3 + 3 * w ) );
		assertEquals( "in an open middle", CaveLife.DRIP_NONE, CaveLife.dripKind( m, w, h, 1 + 5 * w ) );
		m[0 + 4 * w] = Terrain.WALL;
		assertEquals( CaveLife.DRIP_FLOOR, CaveLife.dripKind( m, w, h, 1 + 5 * w ) );
		m[0 + 4 * w] = Terrain.MINE_CRYSTAL;
		assertEquals( CaveLife.DRIP_FLOOR, CaveLife.dripKind( m, w, h, 1 + 5 * w ) );
		assertEquals( "not into the rock itself", CaveLife.DRIP_NONE, CaveLife.dripKind( m, w, h, 0 + 4 * w ) );
		m[0 + 4 * w] = Terrain.WALL;
		assertEquals( CaveLife.DRIP_NONE, CaveLife.dripKind( m, w, h, 0 + 4 * w ) );

		assertEquals( 2f, CaveLife.dripInterval( 0f ), 1e-6f );
		assertEquals( 1f, CaveLife.wetness( 48 ), 1e-6f );
		float last = Float.MAX_VALUE;
		for (int i = 0; i <= 100; i++){
			float wet = i / 100f, iv = CaveLife.dripInterval( wet );
			assertTrue( iv <= last );
			last = iv;
			assertTrue( 1f / iv <= CaveLife.MAX_DRIPS + 1e-4f );
			//jittered either way, never more than six a second
			for (int r = 0; r <= 10; r++) assertTrue( CaveLife.nextDrip( wet, r / 10f ) >= 1f / CaveLife.MAX_DRIPS - 1e-6f );
		}
	}

	@Test
	public void glowsAreTheNearestSeenTwelve(){
		int w = 31, h = 31, hcell = 15 + 15 * w;
		java.util.Random rng = new java.util.Random( 7 );
		int[] m = map( w, h, Terrain.EMPTY );
		boolean[] seen = all( w * h, true );
		int placedM = 0, placedC = 0;
		while (placedM < 20 || placedC < 10){
			int c = 1 + rng.nextInt( w - 2 ) + (1 + rng.nextInt( h - 2 )) * w;
			if (m[c] != Terrain.EMPTY || c == hcell) continue;
			if (placedM < 20){ m[c] = Terrain.MUSHROOM_PATCH; placedM++; }
			else { m[c] = Terrain.MINE_CRYSTAL; placedC++; }
		}
		for (int i = 0; i < seen.length; i += 3) seen[i] = false;
		java.util.function.IntUnaryOperator glint = c -> m[c] == Terrain.MINE_CRYSTAL ? 0xFFFFFF : 0;
		int[] pick = CaveLife.pickGlows( m, seen, w, h, hcell, c -> true, glint, c -> false );
		assertTrue( pick.length <= CaveLife.CAP[CaveLife.GLOWS] );
		int glints = 0, last = 0;
		for (int c : pick){
			assertTrue( "only cells he has seen", seen[c] );
			if (m[c] == Terrain.MINE_CRYSTAL) glints++;
			int d = Math.max( Math.abs( c % w - 15 ), Math.abs( c / w - 15 ) );
			assertTrue( "nearest first", d >= last );
			assertTrue( d <= CaveLife.GLOW_RADIUS );
			last = d;
		}
		assertTrue( glints <= CaveLife.MAX_GLINTS );

		//only the patches that glow at all
		HashSet<Integer> odd = new HashSet<>();
		for (int c : CaveLife.pickGlows( m, all( w * h, true ), w, h, hcell, c -> c % 2 == 1, glint, c -> false )){
			assertTrue( c % 2 == 1 );
			odd.add( c );
		}
		assertFalse( odd.isEmpty() );

		//a vein's glint (whatever OverworldLevel.oreGlint says) is lit like a crystal's
		int[] rock = map( w, h, Terrain.EMPTY );
		rock[16 + 15 * w] = Terrain.WALL;
		int[] vein = CaveLife.pickGlows( rock, all( w * h, true ), w, h, hcell, c -> true,
				c -> c == 16 + 15 * w ? 0xF2C028 : 0, c -> false );
		assertArrayEquals( new int[]{ 16 + 15 * w }, vein );

		//three patches: exactly those three
		int[] few = map( w, h, Terrain.EMPTY );
		int[] three = { 14 + 15 * w, 15 + 18 * w, 20 + 12 * w };
		for (int c : three) few[c] = Terrain.MUSHROOM_PATCH;
		int[] got = CaveLife.pickGlows( few, all( w * h, true ), w, h, hcell, c -> true, c -> 0, c -> false );
		Arrays.sort( got );
		int[] want = three.clone();
		Arrays.sort( want );
		assertArrayEquals( want, got );

		//one lit at ten cells keeps glowing as he walks off; a new one there is not lit
		int[] two = map( w, h, Terrain.EMPTY );
		int far = 25 + 15 * w, other = 5 + 15 * w;
		two[far] = two[other] = Terrain.MUSHROOM_PATCH;
		int[] kept = CaveLife.pickGlows( two, all( w * h, true ), w, h, hcell, c -> true, c -> 0, c -> c == far );
		assertArrayEquals( new int[]{ far }, kept );
	}

	@Test
	public void glowsKeepToTheirPatches(){
		int lit = 0;
		for (int wx = -60; wx < 60; wx++){
			for (int wy = -60; wy < 60; wy++){
				boolean g = CaveLife.glows( SEED, wx, wy );
				assertEquals( "the same patch every time", g, CaveLife.glows( SEED, wx, wy ) );
				if (g) lit++;
			}
		}
		float share = lit / (120f * 120f);
		assertTrue( "about one in three: " + share, share > 0.30f && share < 0.37f );
	}

	@Test
	public void aGlowGoesWithWhatItLit(){
		int w = 5, h = 5;
		int[] m = map( w, h, Terrain.EMPTY );
		int c = 2 + 2 * w;
		m[c] = Terrain.MINE_CRYSTAL;
		assertFalse( CaveLife.outlived( m, w, h, c, Terrain.MINE_CRYSTAL ) );
		m[c] = Terrain.EMPTY;
		assertTrue( "the crystal was mined", CaveLife.outlived( m, w, h, c, Terrain.MINE_CRYSTAL ) );
		m[c] = Terrain.EMBERS;
		assertTrue( "the patch burnt", CaveLife.outlived( m, w, h, c, Terrain.MUSHROOM_PATCH ) );
		assertTrue( "off the window", CaveLife.outlived( m, w, h, -1, Terrain.EMPTY ) );
		assertTrue( "on its ring", CaveLife.outlived( m, w, h, 0, Terrain.EMPTY ) );
	}

	@Test
	public void deeperIsBluerAndStranger(){
		int a = CaveLife.glowColour( -2 ), b = CaveLife.glowColour( -6 ), c = CaveLife.glowColour( -10 );
		assertTrue( a != b && b != c && a != c );
		assertTrue( (b & 0xFF) > (a & 0xFF) );
		assertTrue( (c >> 16) > (b >> 16) );
		int x = CaveLife.glintColour( -2 ), y = CaveLife.glintColour( -6 ), z = CaveLife.glintColour( -10 );
		assertTrue( x != y && y != z && x != z );
		assertTrue( (z >> 16) > (y >> 16) );
		assertEquals( CaveLife.glowColour( OverworldFauna.DEEP_CAVES + 1 ), a );
	}

	@Test
	public void theCaveSoundsWaitTheirTurn(){
		//the first sound is due at once, wherever the clock stands
		assertTrue( CaveLife.soundDue( 0, -CaveLife.SOUND_TURNS ) );
		assertTrue( CaveLife.soundDue( 2_000_000_000, -CaveLife.SOUND_TURNS ) );
		assertFalse( CaveLife.soundDue( 1005, 1000 ) );
		assertTrue( CaveLife.soundDue( 1006, 1000 ) );
		//turns pass by whichever clock moves: the world's turn stands still under REAL_CLOCK, the
		//actors' clock on a spectator; a hero who neither moves nor lets time pass counts none
		CaveLife.Turns t = new CaveLife.Turns();
		t.see( 500, 40, 77 );
		int first = t.count;
		for (int i = 1; i <= CaveLife.SOUND_TURNS; i++) t.see( 500, 40 + i, 77 );
		assertTrue( "the actors' clock alone", CaveLife.soundDue( t.count, first ) );
		first = t.count;
		for (int i = 1; i <= CaveLife.SOUND_TURNS; i++) t.see( 500 + i, 46, 77 );
		assertTrue( "the world's turn alone", CaveLife.soundDue( t.count, first ) );
		first = t.count;
		for (int i = 1; i <= CaveLife.SOUND_TURNS; i++) t.see( 506, 46, 77 + i );
		assertTrue( "the hero's steps alone", CaveLife.soundDue( t.count, first ) );
		first = t.count;
		t.see( 506, 47, 84 );
		for (int i = 0; i < 50; i++) t.see( 506, 47, 84 );
		assertFalse( "nothing moved: one turn, not fifty", CaveLife.soundDue( t.count, first ) );
		t.see( 0, 47, 84 );
		assertEquals( "a clock set back still counts forward", first + 2, t.count );
		for (int i = 0; i < 100; i++){
			int s = CaveLife.pickSound( i / 100f, 0.5f, -3, true );
			assertTrue( s >= CaveLife.SOUND_DRIP && s <= CaveLife.SOUND_CHIRP );
		}
		assertEquals( CaveLife.SOUND_DRIP, CaveLife.pickSound( 0f, 0f, -3, false ) );
		assertEquals( CaveLife.SOUND_CHIRP, CaveLife.pickSound( 0.999f, 0f, -3, true ) );
		//heard on the ambience channel: the ambience's own drip, rumble and bats
		assertEquals( AmbientSound.DRIP, CaveLife.SOUND[CaveLife.SOUND_DRIP] );
		assertEquals( AmbientSound.RUMBLE, CaveLife.SOUND[CaveLife.SOUND_RUMBLE] );
		assertEquals( AmbientSound.BAT, CaveLife.SOUND[CaveLife.SOUND_CHIRP] );
		assertEquals( 2, CaveLife.motes( -9, 0.99f ) );
		assertEquals( 0, CaveLife.motes( -2, 0.99f ) );
	}

	@Test
	public void marmotsWantABoulderAndThaw(){
		int w = 5;
		int[] m = map( w, 5, Terrain.GRASS );
		int c = 2 + 2 * w;
		m[c + 1] = Terrain.BOULDER;
		assertTrue( PeakLife.marmotGround( m, w, c, false ) );
		assertEquals( c + 1, PeakLife.boulderBeside( m, w, c ) );
		assertFalse( "frozen", PeakLife.marmotGround( m, w, c, true ) );
		m[c] = Terrain.SNOW;
		assertFalse( "on snow", PeakLife.marmotGround( m, w, c, false ) );
		m[c] = Terrain.FLOWER_PATCH;
		assertTrue( PeakLife.marmotGround( m, w, c, false ) );
		m[c + 1] = Terrain.GRASS;
		assertFalse( "no boulder", PeakLife.marmotGround( m, w, c, false ) );
		m[c + 1 + w] = Terrain.BOULDER;
		assertFalse( "only a diagonal one", PeakLife.marmotGround( m, w, c, false ) );
		m[c + 1 + w] = Terrain.GRASS;
		m[c + w] = Terrain.BOULDER;
		assertFalse( "a boulder below would be drawn over it", PeakLife.marmotGround( m, w, c, false ) );
		m[c - w] = Terrain.BOULDER;
		assertFalse( "not even with another above", PeakLife.marmotGround( m, w, c, false ) );
		m[c + w] = Terrain.GRASS;
		assertTrue( PeakLife.marmotGround( m, w, c, false ) );
		assertEquals( c - w, PeakLife.boulderBeside( m, w, c ) );
	}

	@Test
	public void plumesTearOffTheLee(){
		int W = 50;
		assertEquals( -W, PeakLife.downwind( W, 0f ) );
		assertEquals( 1, PeakLife.downwind( W, 90f ) );
		assertEquals( W, PeakLife.downwind( W, 180f ) );
		assertEquals( -1, PeakLife.downwind( W, 270f ) );
		assertEquals( 1 - W, PeakLife.downwind( W, 45f ) );
		for (int d = 0; d < 360; d += 5) assertTrue( PeakLife.downwind( W, d ) != 0 );
		int w = 5, h = 5;
		int[] m = map( w, h, Terrain.SNOW );
		int c = 2 + 2 * w;
		m[c] = Terrain.WALL;
		assertTrue( PeakLife.ridge( m, w, h, c, true, w ) );
		assertFalse( "unfrozen", PeakLife.ridge( m, w, h, c, false, w ) );
		m[c + w] = Terrain.WALL;
		assertFalse( "rock on the lee", PeakLife.ridge( m, w, h, c, true, w ) );
		assertTrue( PeakLife.ridge( m, w, h, c, true, -w ) );
		m[c] = Terrain.SNOW;
		assertFalse( "no rock", PeakLife.ridge( m, w, h, c, true, -w ) );
		//the snow leaves the crest the hero sees: the face's top unless the lee lies north
		int top = (c / w) * 16;
		assertEquals( top - 5, PeakLife.plumeY( c, w, PeakLife.downwind( w, 0f ) ) );
		assertEquals( top - 5, PeakLife.plumeY( c, w, PeakLife.downwind( w, 315f ) ) );
		for (float dir : new float[]{ 90f, 135f, 180f, 225f, 270f }){
			assertEquals( "wind toward " + dir, top, PeakLife.plumeY( c, w, PeakLife.downwind( w, dir ) ) );
		}
	}

	@Test
	public void cloudsLieOnlyOverOpenAir(){
		int w = 6, h = 7;
		int[] m = map( w, h, Terrain.CHASM );
		for (int x = 0; x < w; x++) m[x + 2 * w] = Terrain.SNOW;
		m[4 + 2 * w] = Terrain.GRASS;
		m[5 + 2 * w] = Terrain.WALL;
		m[3 + 5 * w] = Terrain.SNOW;
		assertEquals( "air under air: the whole cell, nothing cut", 0, PeakLife.cloudEdge( m, w, 1 + w ) );
		assertEquals( "no cloud on the ground", -1, PeakLife.cloudEdge( m, w, 1 + 2 * w ) );
		assertEquals( "the top row has no air above it", -1, PeakLife.cloudEdge( m, w, 1 ) );
		//under the ground's edge the cloud tucks in below what the row draws, frayed at the top
		assertEquals( 4 | CloudSea.CUT_TOP, PeakLife.cloudEdge( m, w, 1 + 3 * w ) );
		assertEquals( "below a meadow's lip", 4 | CloudSea.CUT_TOP, PeakLife.cloudEdge( m, w, 4 + 3 * w ) );
		assertEquals( "below a rock's face", 14 | CloudSea.CUT_TOP, PeakLife.cloudEdge( m, w, 5 + 3 * w ) );
		//frayed at the sides where ground stands beside it
		assertEquals( CloudSea.CUT_RIGHT, PeakLife.cloudEdge( m, w, 2 + 5 * w ) );
		assertEquals( CloudSea.CUT_LEFT, PeakLife.cloudEdge( m, w, 4 + 5 * w ) );
		assertEquals( CloudSea.CUT_TOP | 4, PeakLife.cloudEdge( m, w, 3 + 6 * w ) );
		assertEquals( 14, PeakLife.cloudLip( Terrain.WALL ) );
		assertEquals( 4, PeakLife.cloudLip( Terrain.SNOW ) );
		assertEquals( 4, PeakLife.cloudLip( Terrain.EMPTY ) );
	}

	//on random mountainsides, every cell the sea of clouds is drawn on is chasm, and under the
	//ground's edge it starts below the row's lip
	@Test
	public void cloudsNeverCoverGround(){
		java.util.Random rng = new java.util.Random( 11 );
		int w = 40, h = 30;
		int[] out = new int[w * h], edges = new int[w * h];
		for (int seed = 0; seed < 200; seed++){
			int[] m = new int[w * h];
			for (int c = 0; c < m.length; c++){
				float r = rng.nextFloat();
				m[c] = r < 0.45f ? Terrain.CHASM : r < 0.7f ? Terrain.SNOW : r < 0.85f ? Terrain.WALL : r < 0.95f ? Terrain.GRASS : Terrain.BOULDER;
			}
			int x0 = rng.nextInt( 10 ) - 3, y0 = rng.nextInt( 10 ) - 3;
			int n = PeakLife.seaCells( m, w, h, x0, y0, x0 + 30, y0 + 20, out, edges );
			int expect = 0;
			for (int y = Math.max( 1, y0 ); y <= Math.min( h - 2, y0 + 20 ); y++){
				for (int x = Math.max( 1, x0 ); x <= Math.min( w - 2, x0 + 30 ); x++){
					if (m[x + y * w] == Terrain.CHASM) expect++;
				}
			}
			assertEquals( expect, n );
			for (int i = 0; i < n; i++){
				int c = out[i], e = edges[i];
				assertEquals( Terrain.CHASM, m[c] );
				assertEquals( PeakLife.cloudEdge( m, w, c ), e );
				if (m[c - w] != Terrain.CHASM){
					assertTrue( (e & CloudSea.CUT_TOP) != 0 );
					assertTrue( "clear of the row's lip", (e & 31) >= (m[c - w] == Terrain.WALL ? 14 : 4) );
				} else {
					assertEquals( 0, e & (31 | CloudSea.CUT_TOP) );
				}
				assertEquals( m[c - 1] != Terrain.CHASM, (e & CloudSea.CUT_LEFT) != 0 );
				assertEquals( m[c + 1] != Terrain.CHASM, (e & CloudSea.CUT_RIGHT) != 0 );
			}
		}
	}

	@Test
	public void cloudsThickenWithHeightAndDarkenAtNight(){
		assertEquals( 0f, PeakLife.cloudAlpha( 3, 1f ), 0f );
		assertTrue( PeakLife.cloudAlpha( 4, 0f ) > 0f );
		assertTrue( PeakLife.cloudAlpha( 4, 0f ) < PeakLife.cloudAlpha( 7, 0f ) );
		for (int a = 4; a <= 10; a++){
			for (float c = 0f; c <= 1f; c += 0.1f) assertTrue( PeakLife.cloudAlpha( a, c ) <= 0.75f );
		}
		assertTrue( PeakLife.deepAlpha( 0f ) < PeakLife.deepAlpha( 1f ) );
		assertTrue( luma( PeakLife.cloudTint( Phase.NIGHT, 0f ) ) < luma( PeakLife.cloudTint( Phase.DAY, 0.5f ) ) );
		int dawn = PeakLife.cloudTint( Phase.DAWN, 0.2f );
		assertTrue( "rosy at dawn", ((dawn >> 16) & 0xFF) > (dawn & 0xFF) );
		assertEquals( 0x8A94B8, PeakLife.cloudTint( Phase.DUSK, 1f ) );
	}

	private static float luma( int c ){
		return 0.3f * ((c >> 16) & 0xFF) + 0.59f * ((c >> 8) & 0xFF) + 0.11f * (c & 0xFF);
	}

	@Test
	public void capsAreTheSpecs(){
		assertArrayEquals( new int[]{ 2, 2, 2, 12 }, CaveLife.CAP );
		assertEquals( 6, CaveLife.MAX_GLINTS );
		assertEquals( 6f, CaveLife.MAX_DRIPS, 0f );
		assertArrayEquals( new int[]{ 1, 3, 6 }, PeakLife.CAP );
		assertEquals( 192f, SliceLife.DESPAWN_PX, 0f );
	}

	//the debug scenes' places, checked on the windows the game would build there
	@Test
	public void scenesFindTheirPlaces(){
		for (int a : new int[]{ -3, -9 }){
			long t0 = System.nanoTime();
			int[] spot = CaveLife.findChamber( SEED, a );
			assertTrue( "the chamber search at " + a + " is quick", System.nanoTime() - t0 < 5_000_000_000L );
			assertNotNull( "a chamber at " + a, spot );
			int[] m = WindowGenerator.generate( SEED, a, spot[0] - 88, spot[1] - 88, 0f ).terrain;
			int land = 88 + 88 * W;
			assertTrue( (Terrain.flags[m[land]] & Terrain.PASSABLE) != 0 );
			assertTrue( "pools by the chamber at " + a, within( m, land, 12, c -> m[c] == Terrain.WATER || m[c] == Terrain.DEEP_WATER ) );
			assertTrue( "fungus at " + a, within( m, land, 12, c -> m[c] == Terrain.MUSHROOM_PATCH ) );
			assertTrue( "crystal at " + a, within( m, land, 12, c -> m[c] == Terrain.MINE_CRYSTAL ) );
			assertTrue( "a deep pool for the fish at " + a, within( m, land, 12, c -> CaveLife.caveFishWater( m, W, H, c ) ) );
		}

		float shift = WorldModel.calendarShift( GameCalendar.Season.SUMMER, 0.5f );
		long t0 = System.nanoTime();
		int[] meadow = PeakLife.findMeadowEdge( SEED, 3, shift );
		assertTrue( "the meadow search is quick", System.nanoTime() - t0 < 5_000_000_000L );
		assertNotNull( meadow );
		WindowGenerator.Window mw = WindowGenerator.generate( SEED, 3, meadow[0] - 88, meadow[1] - 88, shift );
		int land = 88 + 88 * W;
		assertTrue( (Terrain.flags[mw.terrain[land]] & Terrain.PASSABLE) != 0 );
		assertFalse( mw.frozen[land] );
		assertTrue( "the band's edge", within( mw.terrain, land, 6, c -> mw.terrain[c] == Terrain.CHASM ) );
		assertTrue( "boulders", within( mw.terrain, land, 8, c -> mw.terrain[c] == Terrain.BOULDER ) );
		assertTrue( "a marmot's ground", within( mw.terrain, land, 9, c -> PeakLife.marmotGround( mw.terrain, W, c, mw.frozen[c] ) ) );

		t0 = System.nanoTime();
		int[] peak = PeakLife.findPeak( SEED, 7, shift );
		assertTrue( "the summit search is quick", System.nanoTime() - t0 < 5_000_000_000L );
		assertNotNull( peak );
		assertTrue( peak[2] >= PeakLife.CLOUD_ALTITUDE );
		int[] pm = WindowGenerator.generate( SEED, peak[2], peak[0] - 88, peak[1] - 88, shift ).terrain;
		assertTrue( (Terrain.flags[pm[land]] & Terrain.PASSABLE) != 0 );
		int air = 0, n = 0;
		for (int dy = -8; dy <= 8; dy++){
			for (int dx = -8; dx <= 8; dx++){
				n++;
				if (pm[land + dx + dy * W] == Terrain.CHASM) air++;
			}
		}
		assertTrue( "air all round the summit: " + air + " of " + n, air * 10 >= n * 4 );
	}

	private static boolean within( int[] m, int cell, int r, java.util.function.IntPredicate p ){
		for (int dy = -r; dy <= r; dy++){
			for (int dx = -r; dx <= r; dx++){
				if (p.test( cell + dx + dy * W )) return true;
			}
		}
		return false;
	}
}

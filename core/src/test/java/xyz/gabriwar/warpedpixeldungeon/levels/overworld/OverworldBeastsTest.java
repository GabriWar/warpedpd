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

import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Alligator;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bananaspider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bison;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Camel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Pike;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Scorpio;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Slime;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Vulture;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna.Habitat;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * The surface's new beasts in OverworldFauna's tables: bison herds on the plains and meadows by
 * day, camels and vultures in the desert by day, alligators in the swamp's open water and pike in
 * the rivers' at any hour, every older row still there; the surface's open water is ground of its
 * own (SHALLOWS) that only the water's beasts roll on; a parked water beast comes back only into
 * water; the windows the game builds are peopled that way, the same each time for the same dice;
 * and the night's cull takes the day's beasts, never the water's.
 */
public class OverworldBeastsTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int turn, depth, branch, challenges;
	private float precip;
	private Phase override;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		challenges = Dungeon.challenges;
		precip = ClimateManager.debugPrecipOverride;
		override = DayNightCycle.debugPhaseOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		DayNightCycle.debugPhaseOverride = null;
		ClimateManager.debugPrecipOverride = 0f;
		hero = new Hero();
		hero.lvl = 30;
		hero.sprite = stub();
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.cycleTurn = turn;
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.challenges = challenges;
		ClimateManager.debugPrecipOverride = precip;
		DayNightCycle.debugPhaseOverride = override;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	private static CharSprite stub(){
		CharSprite sp = new CharSprite(){
			@Override public void place( int cell ){ }
			@Override public void update(){ }
			@Override public void showStatus( int color, String text, Object... args ){ }
			@Override public void showStatusWithIcon( int color, String text, int icon, Object... args ){ }
		};
		sp.visible = false;
		return sp;
	}

	private static void setField( Object o, String name, Object v ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, v );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static void populate( OverworldLevel ow ){
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( "populateFauna" );
			m.setAccessible( true );
			m.invoke( ow );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	//the surface's window round a world cell, built as a mirror's is and made the host's own
	private OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		float shift = WorldModel.calendarShift();
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, shift );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, shift, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		setField( ow, "network", false );
		Dungeon.depth = WorldLayers.depthOf( 0 );
		Dungeon.level = ow;
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	//a world cell of the biome, of its open water or of its dry ground, well out of the town round
	//the origin and out of every village; null if none within the search
	private static int[] spotOf( WorldModel.Biome biome, boolean water ){
		WorldModel.Sample s = new WorldModel.Sample();
		float shift = WorldModel.calendarShift();
		for (int r = 20; r <= 300; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = dx * 16, y = dy * 16;
					if (WorldModel.sample( SEED, x, y, shift, s ).biome != biome) continue;
					int t = WorldModel.wildTerrain( SEED, x, y, s );
					if (water != (t == Terrain.WATER)) continue;
					if (!water && (Terrain.flags[t] & Terrain.PASSABLE) == 0) continue;
					if (OverworldFauna.nearSettlement( SEED, x, y )) continue;
					//well inside the biome, so the window round it is mostly of it
					boolean inside = true;
					for (int oy = -32; oy <= 32 && inside; oy += 32){
						for (int ox = -32; ox <= 32 && inside; ox += 32){
							inside = WorldModel.sample( SEED, x + ox, y + oy, shift, new WorldModel.Sample() ).biome == biome;
						}
					}
					if (!inside && !water) continue;
					return new int[]{ x, y };
				}
			}
		}
		return null;
	}

	//what many rolls bring out on a ground at an hour: every kind, and its pack sizes seen
	private static HashMap<Class<?>, HashSet<Integer>> rolls( WorldModel.Biome biome, Phase phase, int ground ){
		HashMap<Class<?>, HashSet<Integer>> out = new HashMap<>();
		for (int i = 0; i < 3000; i++){
			ArrayList<Mob> got = OverworldFauna.roll( 0, biome, phase, ground );
			if (got.isEmpty()) continue;
			for (Mob m : got) assertSame( "a pack is of one kind", got.get( 0 ).getClass(), m.getClass() );
			out.computeIfAbsent( got.get( 0 ).getClass(), k -> new HashSet<>() ).add( got.size() );
		}
		return out;
	}

	private static void packs( HashMap<Class<?>, HashSet<Integer>> got, Class<?> cls, int min, int max ){
		assertTrue( cls.getSimpleName() + " rolls", got.containsKey( cls ) );
		for (int n : got.get( cls )) assertTrue( cls.getSimpleName() + " pack of " + n, n >= min && n <= max );
	}

	// ------------------------------------------------------------------ the tables

	@Test
	public void theNewRowsRollWhereAndWhenTheyShould(){
		int dry = Habitat.DRY.bit, wet = Habitat.SHALLOWS.bit;
		Random.pushGenerator( 11 );
		try {
			HashMap<Class<?>, HashSet<Integer>> got = rolls( WorldModel.Biome.PLAINS, Phase.DAY, dry );
			packs( got, Bison.class, 3, 5 );
			assertTrue( got.containsKey( Bunny.class ) );
			got = rolls( WorldModel.Biome.PLAINS, Phase.NIGHT, dry );
			assertFalse( "no herds after dark", got.containsKey( Bison.class ) );
			assertTrue( got.containsKey( AlbinoRat.class ) );

			got = rolls( WorldModel.Biome.MEADOW, Phase.DAWN, dry );
			packs( got, Bison.class, 2, 4 );
			assertTrue( got.containsKey( Deer.class ) );
			assertTrue( got.containsKey( Bunny.class ) );

			got = rolls( WorldModel.Biome.DESERT, Phase.DAY, dry );
			packs( got, Camel.class, 1, 3 );
			packs( got, Vulture.class, 1, 2 );
			assertTrue( got.containsKey( Scorpio.class ) );
			got = rolls( WorldModel.Biome.DESERT, Phase.DUSK, dry );
			assertFalse( got.containsKey( Camel.class ) );
			assertFalse( got.containsKey( Vulture.class ) );
			assertTrue( got.containsKey( Snake.class ) );

			for (Phase p : Phase.values()){
				got = rolls( WorldModel.Biome.SWAMP, p, dry );
				assertFalse( "no alligator on dry ground", got.containsKey( Alligator.class ) );
				assertTrue( got.containsKey( Snake.class ) && got.containsKey( Slime.class ) && got.containsKey( Bananaspider.class ) );
				got = rolls( WorldModel.Biome.SWAMP, p, wet );
				packs( got, Alligator.class, 1, 1 );
				assertEquals( "the swamp's water is the alligators' alone", 1, got.size() );

				got = rolls( WorldModel.Biome.RIVER, p, wet );
				packs( got, Pike.class, 1, 1 );
				assertEquals( 1, got.size() );
				assertTrue( "nothing on a frozen river's ice", rolls( WorldModel.Biome.RIVER, p, dry ).isEmpty() );
				assertTrue( "the sea's water is none of these", rolls( WorldModel.Biome.OCEAN, p, wet ).isEmpty() );
			}
			assertTrue( "nobody else walks the water", rolls( WorldModel.Biome.PLAINS, Phase.DAY, wet ).isEmpty() );
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void theRollsArePureFunctionsOfTheDice(){
		ArrayList<String> first = new ArrayList<>(), second = new ArrayList<>();
		for (ArrayList<String> out : Arrays.asList( first, second )){
			Random.pushGenerator( 0xB15011L );
			try {
				for (WorldModel.Biome b : WorldModel.Biome.values()){
					for (int ground : new int[]{ Habitat.DRY.bit, Habitat.SHALLOWS.bit }){
						for (int i = 0; i < 40; i++){
							ArrayList<Mob> got = OverworldFauna.roll( 0, b, Phase.DAY, ground );
							out.add( got.isEmpty() ? "-" : got.get( 0 ).getClass().getSimpleName() + got.size() );
						}
					}
				}
			} finally {
				Random.popGenerator();
			}
		}
		assertEquals( first, second );
	}

	@Test
	public void theSurfacesOpenWaterIsGroundOfItsOwn(){
		int w = 9, h = 9;
		int[] map = new int[w * h];
		Arrays.fill( map, Terrain.EMPTY );
		map[4 + 4 * w] = Terrain.WATER;
		map[5 + 4 * w] = Terrain.DEEP_WATER;
		map[2 + 2 * w] = Terrain.FROZEN_WATER;
		map[6 + 6 * w] = Terrain.WATER;
		assertEquals( Habitat.SHALLOWS.bit, OverworldFauna.habitat( map, null, w, h, 4 + 4 * w, false, 0 ) );
		assertEquals( "open water, deep beside it or not", Habitat.SHALLOWS.bit, OverworldFauna.habitat( map, null, w, h, 6 + 6 * w, false, 0 ) );
		assertEquals( "deep water is no one's ground", 0, OverworldFauna.habitat( map, null, w, h, 5 + 4 * w, false, 0 ) );
		assertEquals( "ice is dry ground", Habitat.DRY.bit, OverworldFauna.habitat( map, null, w, h, 2 + 2 * w, false, 0 ) );
		assertEquals( Habitat.DRY.bit, OverworldFauna.habitat( map, null, w, h, 1 + w, false, 0 ) );
		//a slice's water is as it was: a deep pool's shelf, nothing else
		assertEquals( Habitat.POOL.bit, OverworldFauna.habitat( map, null, w, h, 4 + 4 * w, false, -5 ) );
		assertEquals( 0, OverworldFauna.habitat( map, null, w, h, 6 + 6 * w, false, -5 ) );
	}

	@Test
	public void theNewBeastsAreFaunaKeepingToTheirGround(){
		for (Mob m : new Mob[]{ new Bison(), new Camel(), new Vulture(), new Alligator(), new Pike() }){
			assertTrue( m.getClass().getSimpleName(), OverworldFauna.isFauna( m ) );
		}
		assertSame( Habitat.SHALLOWS, OverworldFauna.home( Pike.class ) );
		assertSame( Habitat.SHALLOWS, OverworldFauna.home( Alligator.class ) );
		assertSame( Habitat.DRY, OverworldFauna.home( Bison.class ) );
		assertSame( Habitat.DRY, OverworldFauna.home( Vulture.class ) );
		//the slices' kinds keep theirs
		assertSame( Habitat.POOL, OverworldFauna.home( xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoPiranha.class ) );
		assertSame( Habitat.CLIFF, OverworldFauna.home( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat.class ) );
	}

	// ------------------------------------------------------------------ the windows

	//what peoples a fresh window round a world cell with these dice, in order
	private ArrayList<String> peopled( int[] at, long dice ){
		Actor.clear();
		OverworldLevel ow = windowAt( at[0], at[1] );
		hero.pos = W / 2 + (H / 2) * W;
		Random.pushGenerator( dice );
		try {
			populate( ow );
		} finally {
			Random.popGenerator();
		}
		ArrayList<String> out = new ArrayList<>();
		for (Mob m : ow.mobs) out.add( m.getClass().getSimpleName() + "@" + m.pos );
		//the level keeps its mobs in a set: the order they are listed in is not the dice's
		out.sort( null );
		return out;
	}

	@Test
	public void theWindowsArePeopledOnTheirOwnGround(){
		DayNightCycle.debugPhaseOverride = Phase.DAY;
		int[][] spots = { spotOf( WorldModel.Biome.SWAMP, false ), spotOf( WorldModel.Biome.RIVER, true ),
				spotOf( WorldModel.Biome.PLAINS, false ), spotOf( WorldModel.Biome.DESERT, false ) };
		HashSet<Class<?>> seen = new HashSet<>();
		for (int[] at : spots){
			if (at == null) continue;
			assertEquals( "the same dice people a window the same way", peopled( at, 0xFA0A ), peopled( at, 0xFA0A ) );

			OverworldLevel ow = windowAt( at[0], at[1] );
			hero.pos = W / 2 + (H / 2) * W;
			Random.pushGenerator( 0xBEA57 + at[0] );
			//the water's beasts have only the window's few water cells, and the land's herds share the
			//cap with them (the wild horses too): rounds go on until the spot's one shows
			Class<?> want = at == spots[0] ? Alligator.class : at == spots[1] ? Pike.class : null;
			try {
				for (int round = 0; round < 30 || (want != null && !seen.contains( want ) && round < 200); round++){
					Actor.clear();
					ow.mobs.clear();
					populate( ow );
					int fauna = 0;
					for (Mob m : ow.mobs){
						if (!OverworldFauna.isFauna( m )) continue;
						fauna++;
						seen.add( m.getClass() );
						String what = m.getClass().getSimpleName();
						if (m instanceof Alligator || m instanceof Pike){
							assertEquals( what + " in the open water", Terrain.WATER, ow.map[m.pos] );
						} else {
							assertFalse( what + " on dry ground", ow.water[m.pos] );
						}
						if (m instanceof Pike) assertSame( WorldModel.Biome.RIVER, ow.biomeAtCell( m.pos ) );
						if (m instanceof Alligator) assertSame( WorldModel.Biome.SWAMP, ow.biomeAtCell( m.pos ) );
						//a pack's leader is set down 16 or more from him, its members round it
						assertTrue( "too near the hero", ow.distance( m.pos, hero.pos ) >= 14 );
					}
					assertTrue( "the cap, a herd at most over it: " + fauna, fauna < 12 + 5 );
				}
			} finally {
				Random.popGenerator();
			}
		}
		assumeTrue( "no swamp and river near the origin", spots[0] != null && spots[1] != null );
		assertTrue( "alligators in the swamp's water", seen.contains( Alligator.class ) );
		assertTrue( "pike in the river", seen.contains( Pike.class ) );
		if (spots[2] != null) assertTrue( "bison on the plains", seen.contains( Bison.class ) );
		if (spots[3] != null) assertTrue( "camels and vultures in the desert", seen.contains( Camel.class ) && seen.contains( Vulture.class ) );
	}

	@Test
	public void aParkedWaterBeastComesBackOnlyIntoWater(){
		int[] at = spotOf( WorldModel.Biome.SWAMP, false );
		assumeTrue( "no swamp near the origin", at != null );
		OverworldLevel ow = windowAt( at[0], at[1] );
		int pool = -1, land = -1;
		for (int c = 3 * W + 3; c < ow.length() - 3 * W - 3 && (pool == -1 || land == -1); c++){
			if (c % W < 3 || c % W > W - 4) continue;
			if (pool == -1 && ow.map[c] == Terrain.WATER) pool = c;
			if (land == -1 && ow.passable[c] && !ow.water[c]) land = c;
		}
		assertTrue( pool != -1 && land != -1 );
		assertTrue( OverworldFauna.standsAt( ow, new Pike(), pool ) );
		assertFalse( OverworldFauna.standsAt( ow, new Pike(), land ) );
		assertFalse( OverworldFauna.standsAt( ow, new Alligator(), land ) );
		assertTrue( OverworldFauna.standsAt( ow, new Bison(), land ) );
	}

	@Test
	public void theNightCullsTheDaysBeastsNotTheWaters(){
		int[] at = spotOf( WorldModel.Biome.SWAMP, false );
		assumeTrue( "no swamp near the origin", at != null );
		OverworldLevel ow = windowAt( at[0], at[1] );
		hero.pos = W / 2 + (H / 2) * W;
		Mob[] beasts = { new Bison(), new Camel(), new Vulture(), new Alligator(), new Pike() };
		int x = 10;
		for (Mob m : beasts){
			m.pos = x + 10 * W;
			x += 3;
			ow.mobs.add( m );
		}
		DayNightCycle.debugPhaseOverride = Phase.NIGHT;
		OverworldFauna.cull( ow );
		assertFalse( ow.mobs.contains( beasts[0] ) );
		assertFalse( ow.mobs.contains( beasts[1] ) );
		assertFalse( ow.mobs.contains( beasts[2] ) );
		assertTrue( "the water's beasts keep every hour", ow.mobs.contains( beasts[3] ) );
		assertTrue( ow.mobs.contains( beasts[4] ) );
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * An independent check of the ore unit: a generated window's veins are the world's own on every
 * slice and for more than one seed (so the host, a reload and a mirror all agree), a mined vein or gem crystal stays mined through the diff
 * store and a slide, the art's ids are their own, and every ore and gem sells.
 */
public class OreVerifyTest {

	private static final long[] SEEDS = { 0x5EED0F7EA7L, 0x13579BDF2468ACEL };
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private Level saved;
	private Hero savedHero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	@Before
	public void setUp(){
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.hero = new Hero();
	}

	@After
	public void tearDown(){
		Actor.clear();
		WorldModel.releaseSeasonShift();
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	//the window walks its veins over the generator's rock bits; the cell function asks the world: the
	//veins must be the same, on every slice that has any, for any seed
	@Test
	public void memoAgreesWithTheWorldOnEverySlice(){
		for (long seed : SEEDS){
			Random rnd = new Random( seed );
			for (int a = -WorldLayers.MAX_BELOW; a < WorldLayers.MAX_ABOVE; a++){
				if (a == 0) continue;
				int ox, oy;
				int[] peak = a > 0 ? OresTest.rockyPeak( seed, a ) : null;
				if (peak != null){
					ox = peak[0] - W / 2;
					oy = peak[1] - H / 2;
				} else {
					ox = rnd.nextInt( 4000 ) - 2000;
					oy = rnd.nextInt( 4000 ) - 2000;
				}
				WindowGenerator.Window w = WindowGenerator.generate( seed, a, ox, oy, 0f );
				byte[] asked = new byte[W * H];
				Ores.veins( seed, a, ox, oy, W, H, null, asked );
				assertArrayEquals( "seed " + seed + " slice " + a + " at " + ox + "," + oy, asked, w.veins );
				//and the cell function says the same of a sample of cells
				for (int c = W; c < W * (H - 1); c += 97){
					Ores.Kind k = Ores.oreAt( seed, a, ox + c % W, oy + c / W );
					assertEquals( k == null ? 0 : k.ordinal() + 1, w.veins[c] );
				}
			}
		}
	}

	//a mirror-like level of the slice at (ox, oy) with the given map, made the host's own
	private static OverworldLevel window( long seed, int altitude, int ox, int oy, int[] map ){
		PathFinder.setMapSize( W, H );
		OverworldLevel ow = OverworldLevel.forNetwork( altitude, seed, ox, oy, 0f, GameCalendar.Season.SUMMER, map, W, H );
		assertNotNull( ow );
		try {
			Field f = OverworldLevel.class.getDeclaredField( "network" );
			f.setAccessible( true );
			f.set( ow, false );
		} catch (Exception e){
			throw new AssertionError( e );
		}
		Dungeon.level = ow;
		DungeonTileSheet.setupVariance( ow.length(), 7L );
		return ow;
	}

	@SuppressWarnings("unchecked")
	private static HashMap<Long, Integer> captured( OverworldLevel ow ) throws Exception {
		Method m = OverworldLevel.class.getDeclaredMethod( "captureDiffs" );
		m.setAccessible( true );
		m.invoke( ow );
		Field f = OverworldLevel.class.getDeclaredField( "diffs" );
		f.setAccessible( true );
		return (HashMap<Long, Integer>) f.get( ow );
	}

	private static int glint( OverworldLevel ow, int cell ) throws Exception {
		Field f = OverworldLevel.class.getDeclaredField( "dressGround" );
		f.setAccessible( true );
		return ((OverworldDress.Layer) f.get( ow )).get( cell );
	}

	//mined rock and crystal are player edits: kept by the diff store, laid again over a fresh
	//window of the same place and over a slid one, so the ore and the gem never come back
	@Test
	public void aMinedVeinNeverComesBack() throws Exception {
		long seed = SEEDS[0];
		int ox = 200, oy = -300;
		for (int a : new int[]{ -6, -9 }){
			WindowGenerator.Window w = WindowGenerator.generate( seed, a, ox, oy, 0f );
			OverworldLevel ow = window( seed, a, ox, oy, w.terrain.clone() );
			int vein = -1, gem = -1;
			for (int c = W + 1; c < W * (H - 1); c++){
				int x = c % W;
				if (x < 40 || x > W - 40) continue;
				if (vein == -1 && ow.map[c] == Terrain.WALL && Ores.faceKind( glint( ow, c ) ) != null) vein = c;
				if (gem == -1 && ow.map[c] == Terrain.MINE_CRYSTAL
						&& Ores.gemAt( seed, a, ox + x, oy + c / W ) != null) gem = c;
			}
			assertTrue( "slice " + a + ": no glinting vein", vein != -1 );
			assertNotNull( ow.oreFrom( vein ) );
			Level.set( vein, Terrain.EMPTY_DECO, ow );
			ow.redressRock( vein );
			assertEquals( -1, glint( ow, vein ) );
			if (gem != -1){
				assertNotNull( ow.gemFrom( gem ) );
				Level.set( gem, Terrain.EMPTY, ow );
			}
			HashMap<Long, Integer> diffs = new HashMap<>( captured( ow ) );
			assertEquals( Integer.valueOf( Terrain.EMPTY_DECO ), diffs.get( OverworldLevel.worldKey( ox + vein % W, oy + vein / W ) ) );
			if (gem != -1){
				assertEquals( Integer.valueOf( Terrain.EMPTY ), diffs.get( OverworldLevel.worldKey( ox + gem % W, oy + gem / W ) ) );
			}

			//the same place again (a reload) and the window slid 32 cells east
			for (int shift : new int[]{ 0, 32 }){
				int nx = ox + shift;
				int[] map = WindowGenerator.generate( seed, a, nx, oy, 0f ).terrain.clone();
				WindowGenerator.overlayDiffs( map, diffs, nx, oy );
				OverworldLevel back = window( seed, a, nx, oy, map );
				int v = vein - shift;
				assertEquals( Terrain.EMPTY_DECO, back.map[v] );
				assertNull( back.oreFrom( v ) );
				assertEquals( -1, glint( back, v ) );
				if (gem != -1){
					int g = gem - shift;
					assertEquals( Terrain.EMPTY, back.map[g] );
					assertNull( back.gemFrom( g ) );
				}
			}
		}
	}

	//nature heals trampled grass, never a mined face or crystal
	@Test
	public void minedRockIsNoNaturalDecay() throws Exception {
		Method m = OverworldLevel.class.getDeclaredMethod( "naturalDecay", int.class, int.class );
		m.setAccessible( true );
		assertFalse( (Boolean) m.invoke( null, Terrain.WALL, Terrain.EMPTY_DECO ) );
		assertFalse( (Boolean) m.invoke( null, Terrain.MINE_CRYSTAL, Terrain.EMPTY ) );
	}

	//the item cells and the dress ids of the ore are nobody else's
	@Test
	public void artSlotsAreTheirOwn() throws Exception {
		HashMap<Integer, String> items = new HashMap<>();
		for (Field f : ItemSpriteSheet.class.getDeclaredFields()){
			if (f.getType() != int.class || !Modifier.isStatic( f.getModifiers() ) || !Modifier.isPublic( f.getModifiers() )) continue;
			f.setAccessible( true );
			int v = f.getInt( null );
			if (v < ItemSpriteSheet.ORE_COPPER || v > ItemSpriteSheet.GEM_DIAMOND) continue;
			String was = items.put( v, f.getName() );
			assertNull( f.getName() + " shares cell " + v + " with " + was, was );
			assertTrue( f.getName() + " sits on an ore cell", f.getName().startsWith( "ORE_" ) || f.getName().startsWith( "GEM_" ) );
		}
		assertEquals( 11, items.size() );
		for (Ores.Kind k : Ores.Kind.values()){
			assertTrue( items.containsKey( Reflection.newInstance( k.item ).image() ) );
		}
		for (Ores.GemKind g : Ores.GemKind.values()){
			assertTrue( items.containsKey( Reflection.newInstance( g.item ).image() ) );
		}

		int lo = OverworldDress.ORE_FACE, hi = lo + OverworldDress.ORE_VERSIONS * Ores.Kind.values().length;
		for (Field f : OverworldDress.class.getDeclaredFields()){
			if (f.getType() != int.class || !Modifier.isStatic( f.getModifiers() )) continue;
			if (f.getName().equals( "ORE_FACE" ) || f.getName().equals( "ORE_VERSIONS" )) continue;
			f.setAccessible( true );
			int v = f.getInt( null );
			assertTrue( f.getName() + " = " + v + " is an ore glint id", v < lo || v >= hi );
		}
	}

	//every lump and stone is something a shop takes and the pack stacks, and gems feed alchemy
	@Test
	public void everyFindSells(){
		for (Ores.Kind k : Ores.Kind.values()){
			Item i = Reflection.newInstance( k.item );
			assertTrue( k + " sells", Shopkeeper.canSell( i ) );
			assertTrue( i.stackable && !i.unique );
		}
		for (Ores.GemKind g : Ores.GemKind.values()){
			Item i = Reflection.newInstance( g.item );
			assertTrue( g + " sells", Shopkeeper.canSell( i ) );
			assertTrue( g + " gives energy", i.energyVal() > 0 );
		}
	}
}

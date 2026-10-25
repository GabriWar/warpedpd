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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.LighthouseKeeper;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Boat;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sea.Afloat;
import xyz.gabriwar.warpedpixeldungeon.tiles.SeaArt;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A slice's sea at run time (SeaWorld), on real windows of the surface derived from a seed: a sea
 * chart marks the islands round its lighthouse, a wreck gives up its cargo once, a buried chest comes
 * up once and its mark goes, what was found and dug comes back through a save, a co-op guest's mirror
 * shows the host's places, moorings and dug marks, and the art lays a tile on every cell the sea's
 * places stand on.
 */
public class SeaWorldTest {

	public static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int depth, branch, startDay, turn;
	private DayNightCycle.Phase override;
	private float shift;
	private boolean shiftHeld;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level savedLevel;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		startDay = Dungeon.calendarStartDay;
		turn = Dungeon.cycleTurn;
		override = DayNightCycle.debugPhaseOverride;
		shift = WorldModel.seasonShift();
		shiftHeld = WorldModel.seasonShiftHeld();
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Random.pushGenerator( 4243 );
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.calendarStartDay = 0;
		Dungeon.cycleTurn = 100;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		HomesteadWorld.reset();
		hero = new Hero();
		hero.lvl = 10;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Random.popGenerator();
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.calendarStartDay = startDay;
		Dungeon.cycleTurn = turn;
		DayNightCycle.debugPhaseOverride = override;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		HomesteadWorld.reset();
	}

	// ------------------------------------------------------------------ helpers (the sea's other tests use them too)

	/** The nearest island to the world's origin that suits, ring by ring of sectors. */
	public static SeaSites.Island island( Predicate<SeaSites.Island> suits ){
		for (int r = 0; r <= 30; r++){
			for (int sy = -r; sy <= r; sy++){
				for (int sx = -r; sx <= r; sx++){
					if (Math.max( Math.abs( sx ), Math.abs( sy ) ) != r) continue;
					SeaSites.Island i = SeaSites.sector( SEED, sx, sy ).island;
					if (i != null && suits.test( i )) return i;
				}
			}
		}
		throw new AssertionError( "no such island near the origin" );
	}

	/** The surface's window centred on a world cell, as a host holds it (built as a mirror's is, then
	 *  made the host's own; network false), the level of the game. */
	public static OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		float s = WorldModel.calendarShift();
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, s );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, s, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		setNetwork( ow, false );
		Dungeon.level = ow;
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	/** The same window as a co-op guest's mirror holds it (network true), not the level of the game. */
	public static OverworldLevel mirrorAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		float s = WorldModel.calendarShift();
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, s );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, s, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		return ow;
	}

	static void setNetwork( OverworldLevel ow, boolean on ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( "network" );
			f.setAccessible( true );
			f.set( ow, on );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static int count( Heap h, Class<? extends Item> cls ){
		int n = 0;
		if (h == null) return 0;
		for (Item i : h.items) if (i.getClass() == cls) n += i.quantity();
		return n;
	}

	//the found places' keys, in the order found
	private static long[] keys( SeaWorld sea ){
		SeaWorld.Found[] f = sea.found();
		long[] out = new long[f.length];
		for (int i = 0; i < f.length; i++) out[i] = f[i].key;
		return out;
	}

	// ------------------------------------------------------------------ the tests

	@Test
	public void aChartMarksTheIslandsRoundItsLighthouse(){
		SeaSites.Island isl = island( i -> true );
		OverworldLevel ow = windowAt( isl.cx, isl.cy );
		assertEquals( 0, ow.sea().found().length );
		int marked = ow.sea().chart( isl.cx + 30, isl.cy, 400 );
		assertTrue( "marked " + marked, marked >= 1 );
		assertTrue( ow.sea().isFound( isl.key() ) );
		if (isl.wreck != null) assertTrue( ow.sea().isFound( isl.wreck.key() ) );
		//the same chart again marks nothing new
		assertEquals( 0, ow.sea().chart( isl.cx + 30, isl.cy, 400 ) );
		//one too far off marks nothing of this island
		OverworldLevel other = windowAt( isl.cx, isl.cy );
		other.sea().chart( isl.cx + 5000, isl.cy, 400 );
		assertFalse( other.sea().isFound( isl.key() ) );
	}

	@Test
	public void aWreckGivesUpItsCargoOnce(){
		SeaSites.Island isl = island( i -> i.wreck != null );
		OverworldLevel ow = windowAt( isl.cx, isl.cy );
		int hold = ow.localCell( isl.wreck.holdX, isl.wreck.holdY );
		assertNotEquals( -1, hold );
		assertEquals( Terrain.CUSTOM_DECO_EMPTY, ow.map[hold] );
		ow.sea().populate();
		Heap h = ow.heaps.get( hold );
		assertNotNull( "a chest in the hold", h );
		assertEquals( Heap.Type.CHEST, h.type );
		int planks = count( h, Plank.class ), rope = count( h, Rope.class ), gold = count( h, Gold.class );
		assertTrue( "planks " + planks, planks >= 4 && planks <= 8 );
		assertTrue( "rope " + rope, rope >= 1 && rope <= 3 );
		assertTrue( "gold " + gold, gold >= 60 && gold <= 200 );
		//taken, and the window comes round again: nothing more
		h.destroy();
		ow.sea().populate();
		assertNull( ow.heaps.get( hold ) );
	}

	@Test
	public void aLighthouseGetsItsKeeperUntilHeIsDrivenOff(){
		SeaSites.Island isl = island( i -> i.light != null );
		OverworldLevel ow = windowAt( isl.cx, isl.cy );
		long key = isl.light.key();
		ow.sea().populate();
		assertEquals( "a keeper", 1, keepers( ow, key ) );
		LighthouseKeeper keeper = null;
		for (Mob m : ow.mobs) if (m instanceof LighthouseKeeper && ((LighthouseKeeper) m).lightKey == key) keeper = (LighthouseKeeper) m;
		assertTrue( ow.distance( keeper.pos, ow.localCell( isl.light.x, isl.light.y + 1 ) ) <= 2 );
		//he is about: no second one
		ow.sea().populate();
		assertEquals( 1, keepers( ow, key ) );
		//driven off, he never comes back
		ow.mobs.remove( keeper );
		ow.markSiteCleared( key );
		ow.sea().populate();
		assertEquals( 0, keepers( ow, key ) );
	}

	//the keepers of one lighthouse on the level
	private static int keepers( OverworldLevel ow, long key ){
		int n = 0;
		for (Mob m : ow.mobs) if (m instanceof LighthouseKeeper && ((LighthouseKeeper) m).lightKey == key) n++;
		return n;
	}

	@Test
	public void aBuriedChestComesUpOnceAndItsMarkGoes(){
		SeaSites.Island isl = island( i -> i.digX != Integer.MIN_VALUE );
		OverworldLevel ow = windowAt( isl.cx, isl.cy );
		int dig = ow.localCell( isl.digX, isl.digY );
		assertEquals( Terrain.EMPTY_SP, ow.map[dig] );
		assertTrue( ow.sea().treasureAt( dig ) );
		assertEquals( "dig_spot", ow.sea().cellKey( dig ) );
		ow.sea().unearth( dig );
		Heap h = ow.heaps.get( dig );
		assertNotNull( h );
		assertEquals( Heap.Type.CHEST, h.type );
		int gold = count( h, Gold.class );
		assertTrue( "gold " + gold, gold >= 150 && gold <= 400 );
		assertTrue( "gold and one thing more", h.items.size() >= 2 );
		assertFalse( ow.sea().treasureAt( dig ) );
		assertNull( ow.sea().cellKey( dig ) );
		assertTrue( ow.sea().dug( isl ) );
		//the sand stays sand
		assertEquals( Terrain.EMPTY_SP, ow.map[dig] );
		//no second chest
		h.destroy();
		ow.sea().unearth( dig );
		assertNull( ow.heaps.get( dig ) );
	}

	@Test
	public void whatWasFoundAndDugComesBackThroughASave(){
		SeaSites.Island isl = island( i -> i.digX != Integer.MIN_VALUE );
		OverworldLevel ow = windowAt( isl.cx, isl.cy );
		ow.sea().chart( isl.cx, isl.cy, 400 );
		ow.sea().unearth( ow.localCell( isl.digX, isl.digY ) );
		Bundle b = new Bundle();
		ow.sea().storeInBundle( b );
		long[] before = keys( ow.sea() );
		assertTrue( before.length >= 1 );

		OverworldLevel again = windowAt( isl.cx, isl.cy );
		again.sea().restoreFromBundle( b );
		assertArrayEquals( before, keys( again.sea() ) );
		for (int i = 0; i < before.length; i++){
			assertEquals( ow.sea().found()[i].kind, again.sea().found()[i].kind );
			assertEquals( ow.sea().found()[i].name, again.sea().found()[i].name );
		}
		assertTrue( again.sea().dug( isl ) );
		assertFalse( again.sea().treasureAt( again.localCell( isl.digX, isl.digY ) ) );

		//an old save had no sea: it loads with nothing found and nothing dug
		OverworldLevel old = windowAt( isl.cx, isl.cy );
		old.sea().restoreFromBundle( new Bundle() );
		assertEquals( 0, old.sea().found().length );
		assertFalse( old.sea().dug( isl ) );
	}

	@Test
	public void aGuestsMirrorShowsTheHostsSea(){
		SeaSites.Island isl = island( i -> i.digX != Integer.MIN_VALUE );
		OverworldLevel host = windowAt( isl.cx, isl.cy );
		long empty = host.sharedSeaSig();
		host.sea().chart( isl.cx, isl.cy, 400 );
		host.sea().unearth( host.localCell( isl.digX, isl.digY ) );
		//the host's hero has a raft moored off the island
		int water = -1;
		for (int c = 0; c < host.length() && water == -1; c++){
			if (host.map[c] == Terrain.WATER && host.insideMap( c )) water = c;
		}
		assertNotEquals( -1, water );
		int land = -1;
		for (int c = 0; c < host.length() && land == -1; c++){
			if (host.passable[c] && host.map[c] != Terrain.WATER && host.insideMap( c )) land = c;
		}
		hero.pos = land;
		Afloat a = Buff.affect( hero, Afloat.class );
		a.kind = Boat.Kind.SAILBOAT;
		a.wx = host.worldX + water % W;
		a.wy = host.worldY + water / W;
		host.sea().boatsChanged();
		assertEquals( 1, host.sea().moorings().length );
		assertNotEquals( empty, host.sharedSeaSig() );

		OverworldLevel guest = mirrorAt( isl.cx, isl.cy );
		guest.adoptSharedSea( host.sharedSea() );
		assertArrayEquals( keys( host.sea() ), keys( guest.sea() ) );
		assertEquals( 1, guest.sea().moorings().length );
		assertArrayEquals( host.sea().moorings()[0], guest.sea().moorings()[0] );
		assertTrue( guest.sea().dug( isl ) );
		assertEquals( host.sharedSeaSig(), guest.sea().sharedSig() );
		//a guest digs, finds and spawns nothing of his own
		assertFalse( guest.sea().treasureAt( guest.localCell( isl.digX, isl.digY ) ) );
	}

	@Test
	public void aMooredBoatShowsOnASurfaceRestoredBeforeItIsTheGamesLevel(){
		//a save loaded, a climb back up from the caves: the surface adopts its window before it is the
		//game's level, and the host's boat moored on it is drawn, named and pinned all the same
		SeaSites.Island isl = island( i -> true );
		OverworldLevel ow = windowAt( isl.cx, isl.cy );
		int water = -1;
		for (int c = 0; c < ow.length() && water == -1; c++){
			if (ow.map[c] == Terrain.WATER && ow.insideMap( c )) water = c;
		}
		assertNotEquals( -1, water );
		Afloat a = Buff.affect( hero, Afloat.class );
		a.kind = Boat.Kind.RAFT;
		a.wx = ow.worldX + water % W;
		a.wy = ow.worldY + water / W;
		Dungeon.level = null;
		ow.sea().boatsChanged();
		assertEquals( 1, ow.sea().moorings().length );
		assertEquals( "moored_raft", ow.sea().cellKey( water ) );
	}

	@Test
	public void theArtLaysATileOnEveryPlaceOfTheSea(){
		//a wreck's hull, a lighthouse's lamp and a buried chest's mark
		SeaSites.Island isl = island( i -> i.wreck != null );
		int[][] laid = lay( windowAt( isl.cx, isl.cy ) );
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		SeaSites.Wreck wr = isl.wreck;
		for (int y = wr.y0; y <= wr.y1(); y++){
			for (int x = wr.x0; x <= wr.x1(); x++){
				assertNotEquals( "the hull at " + x + "," + y, -1, laid[0][ow.localCell( x, y )] );
			}
		}
		if (isl.digX != Integer.MIN_VALUE){
			assertEquals( SeaArt.DIG, laid[0][ow.localCell( isl.digX, isl.digY )] );
		}
		SeaSites.Island lit = island( i -> i.light != null );
		lay( windowAt( lit.cx, lit.cy ) );
		assertEquals( 1, lamps( (OverworldLevel) Dungeon.level, lit.light ) );

		//a palm isle's palms: a trunk on the ground, fronds over the cell above
		SeaSites.Island warm = island( i -> i.climate == SeaSites.Climate.PALM );
		laid = lay( windowAt( warm.cx, warm.cy ) );
		ow = (OverworldLevel) Dungeon.level;
		int palms = 0;
		for (int c = W; c < ow.length(); c++){
			if (!ow.sea().palmAt( c )) continue;
			palms++;
			assertNotEquals( -1, laid[0][c] );
			assertNotEquals( "fronds over a palm", -1, laid[1][c - W] );
		}
		assertTrue( "palms " + palms, palms > 0 );
	}

	//the art's two layers for the window: {ground, over}
	private static int[][] lay( OverworldLevel ow ){
		int[] ground = new int[ow.length()], over = new int[ow.length()];
		Arrays.fill( ground, -1 );
		Arrays.fill( over, -1 );
		SeaArt.lay( ow, ground, over, new ArrayList<>() );
		return new int[][]{ ground, over };
	}

	//how many lamps the art lights on one lighthouse
	private static int lamps( OverworldLevel ow, SeaSites.Light l ){
		int[] ground = new int[ow.length()], over = new int[ow.length()];
		ArrayList<int[]> lamps = new ArrayList<>();
		SeaArt.lay( ow, ground, over, lamps );
		int n = 0;
		for (int[] p : lamps) if (p[0] == l.x && p[1] == l.y - 2) n++;
		return n;
	}
}

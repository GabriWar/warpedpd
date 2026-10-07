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
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant;
import xyz.gabriwar.warpedpixeldungeon.effects.WorldEventDecor;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ChimneySmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.StarFragment;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ExoticMerchantSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RelicMerchantSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.TravellingMerchantSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndWorldMap;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Signal;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * What WorldEventsTest leaves to the side: a star comes down in the night of every season of
 * the game's own calendar; a settled event (a fragment picked up, a market chased off) stays
 * settled through a real save written to bytes and read back, and through a far trip and back;
 * an unlooted fragment survives a far trip once and is taken back from the heap store while
 * the hero is away; the populator's own pass over a window with a market up adds no stall
 * twice; a trader and a fragment come back from their bundles (and from older ones without
 * their keys); and the map's event pins have colours of their own.
 *
 * And what holds beyond one machine and one clock: a real-clock run's stars fall and its markets
 * open at the right hours of every day of the year; an event over stays over when the wall
 * clock runs back, through a save as well; a co-op guest's mirror pins what the host's map does
 * from the log the host ships, and each player hears of an event in his own words from where his
 * own hero stands; a star heard of once its crater cooled is told of as cold; each trader's sprite
 * is a class a guest can build; and a crater's smoke never takes a chimney's column.
 */
public class WorldEventsPersistenceTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int FC = WorldEvents.FC;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int turn, startDay, challenges, depth, branch;
	private DayNightCycle.Phase override;
	private float shift;
	private boolean shiftHeld;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		override = DayNightCycle.debugPhaseOverride;
		shift = WorldModel.seasonShift();
		shiftHeld = WorldModel.seasonShiftHeld();
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		DayNightCycle.debugPhaseOverride = null;
		hero = new Hero();
		hero.lvl = 12;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.challenges = challenges;
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		DayNightCycle.debugPhaseOverride = override;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	private static Object call( Object o, String name, Class<?>[] types, Object... args ){
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( o, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	private static Object field( Class<?> c, Object o, String name ){
		try {
			Field f = c.getDeclaredField( name );
			f.setAccessible( true );
			return f.get( o );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static WorldEventLog log( OverworldLevel ow ){
		return (WorldEventLog) field( OverworldLevel.class, ow, "eventLog" );
	}

	@SuppressWarnings("unchecked")
	private static HashMap<Long, Heap> stored( OverworldLevel ow ){
		return (HashMap<Long, Heap>) field( OverworldLevel.class, ow, "storedHeaps" );
	}

	//the live surface window round a world cell, built as a mirror's is and made the host's own
	private OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		float s = WorldModel.calendarShift();
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, s );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, s, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		try {
			Field f = OverworldLevel.class.getDeclaredField( "network" );
			f.setAccessible( true );
			f.set( ow, false );
		} catch (Exception e){
			throw new AssertionError( e );
		}
		Dungeon.level = ow;
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	//the hero on an open cell between minD and maxD cells (Chebyshev) from a world cell
	private void standNear( OverworldLevel ow, int wx, int wy, int minD, int maxD ){
		int target = ow.localCell( wx, wy );
		assertNotEquals( -1, target );
		for (int r = minD; r <= maxD; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int c = target + dx + dy * W;
					if (ow.passable[c] && !ow.occupied( c ) && ow.heaps.get( c ) == null){
						hero.pos = c;
						return;
					}
				}
			}
		}
		throw new AssertionError( "nowhere to stand near " + wx + "," + wy );
	}

	//a far trip: the window jumps to a world cell, the hero somewhere in it
	private void jump( OverworldLevel ow, int wx, int wy ){
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, wx, wy );
		hero.pos = ow.localCell( wx, wy );
	}

	//the level as a real save has it: written to bytes, read back into a fresh level
	private static OverworldLevel saveAndLoad( OverworldLevel ow ) throws Exception {
		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		//a headless Game carries no version code: stamp the save as a current one
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		Bundle read = Bundle.read( new ByteArrayInputStream( out.toByteArray() ) );
		Actor.clear();
		OverworldLevel back = new OverworldLevel();
		back.restoreFromBundle( read );
		Dungeon.level = back;
		return back;
	}

	//every piece of a star's own fragment: on the window's heaps and in the heap store
	private static int fragmentsOnWindow( OverworldLevel ow, long id ){
		int n = 0;
		for (Heap h : ow.heaps.valueList()) n += count( h, id );
		return n;
	}

	private static int fragmentsStored( OverworldLevel ow, long id ){
		int n = 0;
		for (Heap h : stored( ow ).values()) n += count( h, id );
		return n;
	}

	private static int count( Heap h, long id ){
		int n = 0;
		for (Item i : h.items){
			if (i instanceof StarFragment && ((StarFragment) i).eventId == id) n += i.quantity();
		}
		return n;
	}

	private static ArrayList<TravellingMerchant> traders( OverworldLevel ow ){
		ArrayList<TravellingMerchant> out = new ArrayList<>();
		for (Mob m : ow.mobs) if (m instanceof TravellingMerchant) out.add( (TravellingMerchant) m );
		return out;
	}

	private static int forSale( OverworldLevel ow ){
		int n = 0;
		for (Heap h : ow.heaps.valueList()) if (h.type == Heap.Type.FOR_SALE) n++;
		return n;
	}

	private static boolean pinned( OverworldLevel ow, long id ){
		for (WorldEvents.Event m : ow.mapEvents( ow.worldX - 400, ow.worldY - 400, 1024 )){
			if (m.id == id) return true;
		}
		return false;
	}

	//a co-op guest's mirror of a host's window: the same ground, built as the network builds it
	private static OverworldLevel mirrorOf( OverworldLevel host ){
		float s = WorldModel.calendarShift();
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, host.worldX, host.worldY, s );
		OverworldLevel m = OverworldLevel.forNetwork( 0, SEED, host.worldX, host.worldY, s, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( m );
		assertTrue( (Boolean) field( OverworldLevel.class, m, "network" ) );
		return m;
	}

	//the log's lines while `run` runs
	private static ArrayList<String> logged( Runnable run ){
		final ArrayList<String> lines = new ArrayList<>();
		Signal.Listener<String> l = lines::add;
		GLog.update.add( l );
		try {
			run.run();
		} finally {
			GLog.update.remove( l );
		}
		return lines;
	}

	private static int lines( ArrayList<String> lines, String key ){
		String text = Messages.get( WorldEvents.class, key );
		int n = 0;
		for (String l : lines) if (l.contains( text )) n++;
		return n;
	}

	//what a hero on a world cell hears of an event at a turn
	private static String line( OverworldLevel ow, WorldEvents.Event e, int turn, int hwx, int hwy ){
		return (String) call( ow, "eventLine", new Class<?>[]{ WorldEvents.Event.class, int.class, int.class, int.class },
				e, turn, hwx, hwy );
	}

	//picks a star's fragment up off the window, as the hero would; the gift's one-shot actor is let go
	private void pickUpFragment( OverworldLevel ow, long id ){
		Heap heap = null;
		for (Heap h : ow.heaps.valueList()) if (count( h, id ) > 0) heap = h;
		assertNotNull( heap );
		hero.pos = heap.pos;
		StarFragment f = (StarFragment) heap.peek();
		assertTrue( f.doPickUp( hero, heap.pos ) );
		heap.pickUp();
		Actor.clear();
	}

	//the first star of the test seed past its second day
	private static WorldEvents.Event aStar(){
		for (WorldEvents.Event e : WorldEventsTest.stars()){
			if (e.startDay >= 2) return e;
		}
		throw new AssertionError( "no star for the test seed" );
	}

	//a human village of a few houses near the origin, with a market due there on some day
	private static WorldEvents.Event aMarket(){
		for (int p = 0; p < 40; p++){
			for (int ry = -3; ry <= 3; ry++){
				for (int rx = -3; rx <= 3; rx++){
					WorldEvents.Event e = WorldEvents.market( SEED, rx, ry, p );
					if (e != null && e.startDay >= 2 && (WorldStructures.settlementLayout( SEED, e.sx, e.sy ).length - 1) / 2 >= 4) return e;
				}
			}
		}
		throw new AssertionError( "no market near the origin" );
	}

	// ------------------------------------------------------------------ the tests

	//the fall window (turns 2200-2499 of a day) is inside the NIGHT phase of every season's day
	//on a turn-counted run, and so is every star's own fall turn
	@Test
	public void starsFallInTheNightOfEverySeason(){
		HashSet<GameCalendar.Season> seasons = new HashSet<>();
		for (int start = 0; start < 364; start += 13){
			Dungeon.calendarStartDay = start;
			for (int tod : new int[]{ WorldEvents.STAR_FALL_FROM, WorldEvents.STAR_FALL_FROM + WorldEvents.STAR_FALL_SPAN - 1 }){
				Dungeon.cycleTurn = 40 * FC + tod;
				seasons.add( GameCalendar.season() );
				assertEquals( "calendar start " + start + ", turn " + tod + " of the day, " + GameCalendar.season(),
						DayNightCycle.Phase.NIGHT, DayNightCycle.phase() );
			}
		}
		assertEquals( 4, seasons.size() );
		int n = 0;
		for (WorldEvents.Event e : WorldEventsTest.stars()){
			if (n++ >= 400) break;
			Dungeon.calendarStartDay = (int) Math.floorMod( e.id, 364L );
			Dungeon.cycleTurn = e.startTurn;
			assertEquals( e.id + " in " + GameCalendar.season(), DayNightCycle.Phase.NIGHT, DayNightCycle.phase() );
		}
	}

	@Test
	public void aLootedStarStaysSettledThroughASaveAndAFarTrip() throws Exception {
		WorldEvents.Event e = aStar();
		Dungeon.cycleTurn = e.startTurn + 100;
		OverworldLevel ow = windowAt( e.wx - 15, e.wy );
		standNear( ow, e.wx, e.wy, 10, 16 );
		ow.pollEvents();
		assertEquals( 1, fragmentsOnWindow( ow, e.id ) );
		assertTrue( pinned( ow, e.id ) );

		Heap heap = null;
		for (Heap h : ow.heaps.valueList()) if (count( h, e.id ) > 0) heap = h;
		assertNotNull( heap );
		hero.pos = heap.pos;
		StarFragment f = (StarFragment) heap.peek();
		assertTrue( f.doPickUp( hero, heap.pos ) );
		heap.pickUp();
		assertTrue( log( ow ).resolved.containsKey( e.id ) );
		//the bonus' one-shot actor is not part of what is tested here
		Actor.clear();

		//a real save and load: still settled, not laid, heard of or pinned again
		OverworldLevel back = saveAndLoad( ow );
		assertTrue( log( back ).resolved.containsKey( e.id ) );
		assertTrue( log( back ).announced.containsKey( e.id ) );
		assertFalse( log( back ).lootAt.containsKey( e.id ) );
		standNear( back, e.wx, e.wy, 10, 16 );
		back.pollEvents();
		back.pollEvents();
		assertEquals( 0, fragmentsOnWindow( back, e.id ) );
		assertFalse( pinned( back, e.id ) );

		//a far trip and back, still within the loot's three days
		jump( back, e.wx + 700, e.wy );
		back.pollEvents();
		jump( back, e.wx - 15, e.wy );
		standNear( back, e.wx, e.wy, 10, 16 );
		back.pollEvents();
		assertEquals( 0, fragmentsOnWindow( back, e.id ) );
		assertEquals( 0, fragmentsStored( back, e.id ) );
		assertFalse( pinned( back, e.id ) );
		//the crater itself stays its nine days whatever became of the loot
		int[] cells = WorldEvents.craterCells( SEED, e );
		boolean scorched = false;
		for (int i = 0; i < cells.length; i += 2){
			int c = back.localCell( cells[i], cells[i + 1] );
			scorched |= c != -1 && back.map[c] == Terrain.EMBERS;
		}
		assertTrue( scorched );
	}

	@Test
	public void aChasedOffMarketStaysOverThroughASaveAndAFarTrip() throws Exception {
		WorldEvents.Event e = aMarket();
		Dungeon.cycleTurn = e.startTurn + 400;
		OverworldLevel ow = windowAt( e.wx, e.wy + 3 );
		standNear( ow, e.wx, e.wy + 3, 0, 8 );
		ow.pollEvents();
		ArrayList<TravellingMerchant> t = traders( ow );
		assertTrue( t.size() >= 2 );
		for (TravellingMerchant m : t) m.restock();
		assertTrue( pinned( ow, e.id ) );
		t.get( 0 ).flee();
		assertTrue( traders( ow ).isEmpty() );
		assertEquals( 0, forSale( ow ) );
		assertTrue( log( ow ).resolved.containsKey( e.id ) );

		OverworldLevel back = saveAndLoad( ow );
		assertTrue( log( back ).resolved.containsKey( e.id ) );
		assertTrue( traders( back ).isEmpty() );
		standNear( back, e.wx, e.wy + 3, 0, 8 );
		back.pollEvents();
		back.pollEvents();
		assertTrue( traders( back ).isEmpty() );
		assertFalse( back.marketActiveAt( WorldStructures.sectorOf( e.sx, e.sy ) ) );
		assertFalse( pinned( back, e.id ) );

		jump( back, e.wx + 700, e.wy );
		back.pollEvents();
		jump( back, e.wx, e.wy + 3 );
		standNear( back, e.wx, e.wy + 3, 0, 8 );
		back.pollEvents();
		assertTrue( traders( back ).isEmpty() );
		assertFalse( back.marketActiveAt( WorldStructures.sectorOf( e.sx, e.sy ) ) );
	}

	@Test
	public void anUnlootedFragmentComesBackOnceAndIsTakenBackWhileAway(){
		WorldEvents.Event e = aStar();
		Dungeon.cycleTurn = e.startTurn + 5;
		OverworldLevel ow = windowAt( e.wx - 15, e.wy );
		standNear( ow, e.wx, e.wy, 10, 16 );
		ow.pollEvents();
		assertEquals( 1, fragmentsOnWindow( ow, e.id ) );

		//away: the fragment waits in the heap store, once
		jump( ow, e.wx + 700, e.wy );
		ow.pollEvents();
		assertEquals( 0, fragmentsOnWindow( ow, e.id ) );
		assertEquals( 1, fragmentsStored( ow, e.id ) );

		//back: on the ground again, once, and the steps lay no second one
		jump( ow, e.wx - 15, e.wy );
		standNear( ow, e.wx, e.wy, 10, 16 );
		ow.pollEvents();
		ow.pollEvents();
		assertEquals( 1, fragmentsOnWindow( ow, e.id ) + fragmentsStored( ow, e.id ) );
		assertEquals( 1, fragmentsOnWindow( ow, e.id ) );

		//away again as the three days run out: taken back out of the store, nothing left to find
		jump( ow, e.wx + 700, e.wy );
		Dungeon.cycleTurn = e.endTurn + 1;
		ow.pollEvents();
		assertEquals( 0, fragmentsStored( ow, e.id ) );
		assertFalse( log( ow ).lootAt.containsKey( e.id ) );
		jump( ow, e.wx - 15, e.wy );
		standNear( ow, e.wx, e.wy, 10, 16 );
		ow.pollEvents();
		assertEquals( 0, fragmentsOnWindow( ow, e.id ) );
		assertFalse( pinned( ow, e.id ) );
	}

	//populate() - the pass every rebase runs - over a window whose market is already up
	@Test
	public void theRebasePopulatorNeverPitchesAStallTwice(){
		WorldEvents.Event e = aMarket();
		Dungeon.cycleTurn = e.startTurn + 400;
		OverworldLevel ow = windowAt( e.wx, e.wy + 3 );
		standNear( ow, e.wx, e.wy + 3, 0, 8 );
		ow.pollEvents();
		ArrayList<TravellingMerchant> t = traders( ow );
		assertTrue( t.size() >= 2 );
		for (TravellingMerchant m : t) m.restock();
		int wares = 0;
		for (Heap h : ow.heaps.valueList()){
			if (h.type == Heap.Type.FOR_SALE) wares++;
		}
		call( ow, "populate", new Class<?>[0] );
		call( ow, "populate", new Class<?>[0] );
		ow.pollEvents();
		assertEquals( t.size(), traders( ow ).size() );
		for (TravellingMerchant m : traders( ow )) assertTrue( t.contains( m ) );
		//the populator's own village vendors may lay their shelves; the traders' stay as they were
		int theirs = 0;
		for (Heap h : ow.heaps.valueList()){
			if (h.type != Heap.Type.FOR_SALE) continue;
			for (TravellingMerchant m : t) if (ow.distance( h.pos, m.pos ) <= 1){ theirs++; break; }
		}
		assertEquals( wares, theirs );
	}

	@Test
	public void aTraderAndAFragmentComeBackFromTheirBundles() throws Exception {
		TravellingMerchant m = TravellingMerchant.of( 2, 0x1234567890L, 98765 );
		m.stallX = -40;
		m.stallY = 77;
		m.openShop();
		StarFragment f = new StarFragment();
		f.eventId = -99L;
		f.eventEnd = 4321;

		Bundle b = new Bundle();
		b.put( "trader", m );
		b.put( "fragment", f );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		Bundle read = Bundle.read( new ByteArrayInputStream( out.toByteArray() ) );

		TravellingMerchant mb = (TravellingMerchant) read.get( "trader" );
		assertEquals( 2, mb.speciality );
		assertEquals( ExoticMerchantSprite.class, mb.spriteClass );
		assertEquals( 0x1234567890L, mb.eventId );
		assertEquals( 98765, mb.endTurn );
		assertEquals( -40, mb.stallX );
		assertEquals( 77, mb.stallY );
		assertEquals( true, field( TravellingMerchant.class, mb, "firstStock" ) );
		assertEquals( false, field( TravellingMerchant.class, mb, "stocked" ) );
		StarFragment fb = (StarFragment) read.get( "fragment" );
		assertEquals( -99L, fb.eventId );
		assertEquals( 4321, fb.eventEnd );

		//bundles without the new keys: a plain trader struck at the next expiry, a plain fragment
		Bundle tb = read.getBundle( "trader" );
		for (String k : new String[]{ "first_stock", "speciality", "event_id", "end_turn", "stocked", "stall_x", "stall_y" }){
			assertTrue( k, tb.contains( k ) );
			tb.remove( k );
		}
		TravellingMerchant bare = new TravellingMerchant();
		bare.restoreFromBundle( tb );
		assertEquals( TravellingMerchant.CURIOS, bare.speciality );
		assertEquals( TravellingMerchantSprite.class, bare.spriteClass );
		assertEquals( 0L, bare.eventId );
		assertEquals( 0, bare.endTurn );
		assertEquals( Integer.MIN_VALUE, bare.stallX );
		assertEquals( false, field( TravellingMerchant.class, bare, "firstStock" ) );
		assertEquals( true, field( TravellingMerchant.class, bare, "stocked" ) );
		Bundle fbb = read.getBundle( "fragment" );
		fbb.remove( "event_id" );
		fbb.remove( "event_end" );
		StarFragment plain = new StarFragment();
		plain.restoreFromBundle( fbb );
		assertEquals( 0L, plain.eventId );
		assertEquals( 0, plain.eventEnd );
	}

	@Test
	public void eachEventKindHasAPinColourOfItsOwn(){
		HashSet<Integer> colours = new HashSet<>();
		colours.add( (Integer) field( WndWorldMap.class, null, "MARKER" ) );
		colours.add( (Integer) field( WndWorldMap.class, null, "WAYPOINT_MARKER" ) );
		for (WorldEvents.Type t : WorldEvents.Type.values()){
			assertTrue( t + " shares a pin colour", colours.add( t.pin ) );
			assertEquals( 0xFF000000, t.pin & 0xFF000000 );
		}
	}

	//a real-clock run reads turn t of a day as t * 24 / FC hours past local midnight
	//(WorldClock.turn), and its night as DayNightCycle.clockPhase does: from 45 minutes after
	//sunset to 45 minutes before sunrise. on every day of the year the stars fall in that night
	//and the markets open (and roll on) after it, never shut for the night; and on a turn-counted
	//run the markets' morning is daytime in every season
	@Test
	public void starsFallAndMarketsOpenAtTheirHoursOnARealClockToo(){
		float fallFrom = WorldEvents.STAR_FALL_FROM * 24f / FC;
		float fallTo = (WorldEvents.STAR_FALL_FROM + WorldEvents.STAR_FALL_SPAN) * 24f / FC;
		float open = WorldEvents.MARKET_OPEN * 24f / FC;
		for (int doy = 1; doy <= 366; doy++){
			float[] sun = GameCalendar.sunriseSunset( doy );
			float nightFrom = sun[1] + 0.75f, nightTo = sun[0] - 0.75f;
			assertTrue( "day " + doy + ": night from " + nightFrom + ", a star from " + fallFrom, fallFrom >= nightFrom );
			assertTrue( fallTo <= 24f );
			assertTrue( "day " + doy + ": night to " + nightTo + ", a market at " + open, open >= nightTo && open < nightFrom );
		}
		HashSet<GameCalendar.Season> seasons = new HashSet<>();
		for (int start = 0; start < 364; start += 13){
			Dungeon.calendarStartDay = start;
			for (int tod : new int[]{ WorldEvents.MARKET_OPEN - 1, WorldEvents.MARKET_OPEN }){
				Dungeon.cycleTurn = 40 * FC + tod;
				seasons.add( GameCalendar.season() );
				assertNotEquals( "calendar start " + start + ", turn " + tod + ", " + GameCalendar.season(),
						DayNightCycle.Phase.NIGHT, DayNightCycle.phase() );
			}
		}
		assertEquals( 4, seasons.size() );
	}

	//a real-clock run's wall clock can run back (a time zone crossed, the device's clock turned
	//back): the events' clock never does, so a star looted and then forgotten as over is not
	//laid, heard of or pinned again when the clock runs back into its days - its loot and its
	//gift are had once - and the same after a save. an older save, without the clock, starts it
	//from the wall's
	@Test
	public void anEventOverStaysOverWhenTheClockRunsBack() throws Exception {
		WorldEvents.Event e = aStar();
		Dungeon.cycleTurn = e.startTurn + 100;
		OverworldLevel ow = windowAt( e.wx - 15, e.wy );
		standNear( ow, e.wx, e.wy, 10, 16 );
		ow.pollEvents();
		pickUpFragment( ow, e.id );
		assertTrue( log( ow ).resolved.containsKey( e.id ) );

		//past its three days: the log forgets it
		int ahead = e.endTurn + 50;
		Dungeon.cycleTurn = ahead;
		ow.pollEvents();
		assertFalse( log( ow ).resolved.containsKey( e.id ) );
		assertFalse( log( ow ).announced.containsKey( e.id ) );

		//the wall clock back inside them: the events' clock stays where it got to
		Dungeon.cycleTurn = e.startTurn + 200;
		assertEquals( ahead, ow.eventTurn() );
		ArrayList<String> heard = logged( ow::pollEvents );
		assertTrue( heard.toString(), heard.isEmpty() );
		assertEquals( 0, fragmentsOnWindow( ow, e.id ) );
		assertFalse( pinned( ow, e.id ) );

		OverworldLevel back = saveAndLoad( ow );
		assertEquals( ahead, back.eventTurn() );
		standNear( back, e.wx, e.wy, 10, 16 );
		heard = logged( back::pollEvents );
		assertTrue( heard.toString(), heard.isEmpty() );
		assertEquals( 0, fragmentsOnWindow( back, e.id ) );
		assertEquals( 0, fragmentsStored( back, e.id ) );
		assertFalse( pinned( back, e.id ) );

		//the wall clock catching up moves it on again
		Dungeon.cycleTurn = ahead + 10;
		assertEquals( ahead + 10, back.eventTurn() );

		//a save from before the clock: the wall's turn, as it is
		Bundle b = new Bundle();
		back.customTerrain = new ArrayList<>();
		back.storeInBundle( b );
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		assertTrue( b.remove( "event_clock" ) );
		Actor.clear();
		Dungeon.cycleTurn = e.startTurn + 300;
		OverworldLevel old = new OverworldLevel();
		old.restoreFromBundle( b );
		Dungeon.level = old;
		assertEquals( e.startTurn + 300, old.eventTurn() );
	}

	//the host ships his log's shared part, the guest's mirror adopts it, and the guest's world
	//map pins what the host's does - and lets it go when the host's does. the host's own log is
	//never overwritten by what is shipped, and an unreadable packet leaves the guest's as it was
	@Test
	public void aCoopGuestsMapPinsWhatTheHostsDoes(){
		WorldEvents.Event e = aStar();
		Dungeon.cycleTurn = e.startTurn + 100;
		OverworldLevel ow = windowAt( e.wx - 15, e.wy );
		standNear( ow, e.wx, e.wy, 10, 16 );
		long unheard = ow.sharedEventsSig();
		ow.pollEvents();
		assertTrue( pinned( ow, e.id ) );
		long heard = ow.sharedEventsSig();
		assertNotEquals( unheard, heard );
		assertEquals( heard, ow.sharedEventsSig() );

		OverworldLevel guest = mirrorOf( ow );
		assertFalse( pinned( guest, e.id ) );
		guest.adoptSharedEvents( ow.sharedEvents() );
		assertTrue( pinned( guest, e.id ) );
		assertEquals( log( ow ).announced, log( guest ).announced );

		pickUpFragment( ow, e.id );
		assertNotEquals( heard, ow.sharedEventsSig() );
		guest.adoptSharedEvents( ow.sharedEvents() );
		assertTrue( log( guest ).resolved.containsKey( e.id ) );
		assertFalse( pinned( guest, e.id ) );

		//a forced event goes along too
		WorldEvents.Event fs = WorldEvents.forcedStar( e.wx + 20, e.wy, Dungeon.cycleTurn );
		ow.forceEvent( fs );
		guest.adoptSharedEvents( ow.sharedEvents() );
		assertEquals( 1, log( guest ).forced.size() );
		assertEquals( fs.id, log( guest ).forced.get( 0 ).id );

		ow.adoptSharedEvents( new Bundle().toString() );
		assertTrue( log( ow ).resolved.containsKey( e.id ) );
		assertTrue( log( ow ).announced.containsKey( e.id ) );
		guest.adoptSharedEvents( "not a bundle" );
		assertTrue( log( guest ).resolved.containsKey( e.id ) );
		assertEquals( 1, log( guest ).forced.size() );
	}

	//every player hears of an event in his own words, from where his own hero stands: a co-op
	//guest 60 cells east of a host who is close by a falling star is told it fell to his west.
	//heard of later, the crater glows while it is hot and is cold after; a market is "here" by
	//its well and named with its way from three sectors off
	@Test
	public void eachPlayerHearsOfAnEventFromWhereHeStands(){
		WorldEvents.Event e = aStar();
		Dungeon.cycleTurn = e.startTurn + 5;
		OverworldLevel ow = windowAt( e.wx - 15, e.wy );
		String west = Messages.get( WorldEvents.class, "dir_w" );
		int turn = e.startTurn + 5;
		assertEquals( Messages.get( WorldEvents.class, "star_falls_near" ), line( ow, e, turn, e.wx - 15, e.wy ) );
		String guest = line( ow, e, turn, e.wx + 45, e.wy );
		assertTrue( guest, guest.equals( Messages.get( WorldEvents.class, "star_falls", west ) )
				|| guest.equals( Messages.get( WorldEvents.class, "star_falls_clouded", west ) ) );

		turn = e.startTurn + WorldEvents.STAR_HOT - 1;
		assertEquals( Messages.get( WorldEvents.class, "star_glow_near" ), line( ow, e, turn, e.wx - 15, e.wy ) );
		assertEquals( Messages.get( WorldEvents.class, "star_glow", west ), line( ow, e, turn, e.wx + 45, e.wy ) );
		turn = e.startTurn + WorldEvents.STAR_HOT;
		assertEquals( Messages.get( WorldEvents.class, "star_cold_near" ), line( ow, e, turn, e.wx - 15, e.wy ) );
		assertEquals( Messages.get( WorldEvents.class, "star_cold", west ), line( ow, e, turn, e.wx + 45, e.wy ) );

		WorldEvents.Event m = aMarket();
		String name = WorldStructures.villageName( SEED, m.sx, m.sy );
		assertEquals( Messages.get( WorldEvents.class, "market_here", name ), line( ow, m, m.startTurn, m.wx, m.wy + 3 ) );
		assertEquals( Messages.get( WorldEvents.class, "market_opens", name, Messages.get( WorldEvents.class, "dir_s" ) ),
				line( ow, m, m.startTurn, m.wx, m.wy - 3 * WorldStructures.SECTOR ) );
	}

	//a star first heard of a day after its fall, its crater cooled: told of as cold, not smoking
	@Test
	public void aStarHeardOfOnceItsCraterCooledIsToldOfAsCold(){
		WorldEvents.Event e = aStar();
		Dungeon.cycleTurn = e.startTurn + WorldEvents.STAR_HOT + 10;
		OverworldLevel ow = windowAt( e.wx - 15, e.wy );
		standNear( ow, e.wx, e.wy, 10, 16 );
		ArrayList<String> heard = logged( ow::pollEvents );
		assertEquals( heard.toString(), 1, lines( heard, "star_cold_near" ) );
		assertEquals( 0, lines( heard, "star_glow_near" ) );
		assertEquals( 0, lines( heard, "star_falls_near" ) );
		//and what it tells of is there to be found
		assertEquals( 1, fragmentsOnWindow( ow, e.id ) );
		assertTrue( pinned( ow, e.id ) );
	}

	//SpectatorReceiver.resolveSpriteClass: a guest builds a mob's sprite from its class's simple
	//name in the sprites package, with no arguments - so each speciality has a class of its own
	@Test
	public void eachTraderHasASpriteAGuestCanBuild() throws Exception {
		HashSet<Class<?>> kinds = new HashSet<>();
		for (int k = -1; k <= TravellingMerchant.SPECIALITIES; k++){
			TravellingMerchant m = TravellingMerchant.of( k, 7L, 9 );
			Class<?> c = m.spriteClass;
			assertSame( c, Class.forName( "xyz.gabriwar.warpedpixeldungeon.sprites." + c.getSimpleName() ) );
			assertTrue( Modifier.isPublic( c.getModifiers() ) );
			assertTrue( Modifier.isPublic( c.getConstructor().getModifiers() ) );
			kinds.add( c );
			Bundle b = new Bundle();
			b.put( "trader", m );
			assertSame( c, ((TravellingMerchant) b.get( "trader" )).spriteClass );
		}
		assertEquals( TravellingMerchant.SPECIALITIES, kinds.size() );
		assertEquals( TravellingMerchantSprite.class, TravellingMerchant.of( TravellingMerchant.CURIOS, 7L, 9 ).spriteClass );
		assertEquals( RelicMerchantSprite.class, TravellingMerchant.of( TravellingMerchant.RELICS, 7L, 9 ).spriteClass );
		assertEquals( ExoticMerchantSprite.class, TravellingMerchant.of( TravellingMerchant.EXOTICS, 7L, 9 ).spriteClass );
	}

	//the scene hands a dead emitter back only to its own exact class: a chimney's column that has
	//faded (SettlementAmbience may still hold it) is never relit as a crater's smoke, nor the other
	//way round - and the crater's kind can be made by the scene's reflection
	@Test
	public void aCratersSmokeIsNeverLitInAChimneysColumn(){
		Group overFog = new Group();
		ChimneySmokeParticle.Column chimney = new ChimneySmokeParticle.Column();
		overFog.add( chimney );
		chimney.kill();
		Gizmo crater = overFog.recycle( WorldEventDecor.CraterSmoke.class );
		assertNotNull( crater );
		assertNotSame( chimney, crater );
		assertTrue( crater instanceof WorldEventDecor.CraterSmoke );
		crater.kill();
		assertSame( chimney, overFog.recycle( ChimneySmokeParticle.Column.class ) );
		chimney.kill();
		assertSame( crater, overFog.recycle( WorldEventDecor.CraterSmoke.class ) );
	}
}

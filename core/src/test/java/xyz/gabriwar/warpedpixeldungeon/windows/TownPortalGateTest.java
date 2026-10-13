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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Portals;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Waystones;
import xyz.gabriwar.warpedpixeldungeon.levels.SpiderNestLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The town's portal gate (Portals.TOWN): its key, always lit and known and first in every list;
 * old saves getting it in the registry and in the town; where it stands on the square and that it
 * blocks no way through; leaving with the window and coming back on its cell; and a trip to the
 * town landing beside it, the way every trip to the surface lands, while a shop's still lands at
 * its own gate. Every window is a real one of the world, derived from a seed.
 */
public class TownPortalGateTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private static final int SHOP6 = Portals.key( 6, 0 ), SHOP11 = Portals.key( 11, 0 ), SHOP16 = Portals.key( 16, 0 );
	private static final int NEST = Portals.key( 6, SpiderNestLevel.SPIDER_BRANCH );

	private int depth, branch, challenges, startDay, turn;
	private DayNightCycle.Phase override;
	private float shift;
	private boolean shiftHeld;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level savedLevel;
	private Hero savedHero;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		challenges = Dungeon.challenges;
		startDay = Dungeon.calendarStartDay;
		turn = Dungeon.cycleTurn;
		override = DayNightCycle.debugPhaseOverride;
		shift = WorldModel.seasonShift();
		shiftHeld = WorldModel.seasonShiftHeld();
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Random.pushGenerator( 4242 );
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		Hero hero = new Hero();
		hero.lvl = 10;
		Dungeon.hero = hero;
		HomesteadWorld.reset();
		Portals.reset();
	}

	@After
	public void tearDown(){
		//an arrival a test set and no level took
		if (OverworldLevel.hasPendingArrival()) windowAt( 0, 0 ).getTransition( null );
		NetManager.setPendingRelocationAnchor( -1 );
		Portals.reset();
		HomesteadWorld.reset();
		Random.popGenerator();
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.challenges = challenges;
		Dungeon.calendarStartDay = startDay;
		Dungeon.cycleTurn = turn;
		DayNightCycle.debugPhaseOverride = override;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	//the live window of the surface round a world cell, built as a mirror's is and made the host's own
	private static OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		float s = WorldModel.calendarShift();
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, s );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, s, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		set( ow, "network", false );
		Dungeon.level = ow;
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	//the window round the town, peopled as a built or loaded one is
	private static OverworldLevel town(){
		OverworldLevel ow = windowAt( WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ),
				WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) );
		call( ow, "populateTown", new Class<?>[0] );
		return ow;
	}

	private static int gateCell( OverworldLevel ow ){
		return ow.localCell( WorldStructures.townWorldX( WorldStructures.TOWN_PORTAL ),
				WorldStructures.townWorldY( WorldStructures.TOWN_PORTAL ) );
	}

	private static List<PortalGate> gates( OverworldLevel ow ){
		ArrayList<PortalGate> out = new ArrayList<>();
		for (Mob m : ow.mobs) if (m instanceof PortalGate) out.add( (PortalGate) m );
		return out;
	}

	@SuppressWarnings("unchecked")
	private static HashMap<Long, Mob> parked( OverworldLevel ow ){
		return (HashMap<Long, Mob>) get( ow, "parkedMobs" );
	}

	private static void set( Object o, String name, Object value ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, value );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static Object get( Object o, String name ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			return f.get( o );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static Object call( Object o, String name, Class<?>[] types, Object... args ){
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( o, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	private static OverworldLevel saveAndLoad( OverworldLevel ow ) throws Exception {
		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
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

	private static Bundle throughASave( Bundle b ) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		return Bundle.read( new ByteArrayInputStream( out.toByteArray() ) );
	}

	//every cell reachable from a cell over passable ground by the given steps, the blocked cell held shut
	private static HashSet<Integer> reach( OverworldLevel ow, int from, int blocked, int[][] steps ){
		HashSet<Integer> seen = new HashSet<>();
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		seen.add( from );
		queue.add( from );
		while (!queue.isEmpty()){
			int c = queue.poll();
			int x = c % W, y = c / W;
			for (int[] s : steps){
				int nx = x + s[0], ny = y + s[1];
				if (nx <= 0 || ny <= 0 || nx >= W - 1 || ny >= H - 1) continue;
				int n = nx + ny * W;
				if (n == blocked || !ow.passable[n] || !seen.add( n )) continue;
				queue.add( n );
			}
		}
		return seen;
	}

	private static final int[][] STRAIGHT = { {1,0}, {-1,0}, {0,1}, {0,-1} };
	private static final int[][] ALL_WAYS = { {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1} };

	// ------------------------------------------------------------------ the registry

	@Test
	public void theTownIsKeyedByTheSurfaceWhichNoDungeonFloorShares(){
		assertEquals( Portals.key( OverworldLevel.DEPTH, 0 ), Portals.TOWN );
		assertEquals( WorldLayers.SURFACE_DEPTH, Portals.depthOf( Portals.TOWN ) );
		assertEquals( 0, Portals.branchOf( Portals.TOWN ) );
		assertTrue( "the surface is a world slice, not a dungeon floor", WorldLayers.isLayerDepth( Portals.depthOf( Portals.TOWN ) ) );
		for (int key : new int[]{ SHOP6, SHOP11, SHOP16, Portals.key( 20, 0 ), NEST }){
			assertNotEquals( Portals.TOWN, key );
		}
	}

	@Test
	public void theTownIsAlwaysLitKnownAndFirstInTheList(){
		Portals.Record town = Portals.get( Portals.TOWN );
		assertNotNull( "a new game knows the town", town );
		assertEquals( PortalGate.State.ACTIVE, town.state );
		assertTrue( town.discovered );

		//the first shop lit from the start, a second one paid, the spider nest's, all found
		Portals.register( SHOP6, 40 ).discovered = true;
		Portals.register( SHOP11, 41 ).discovered = true;
		Portals.setState( SHOP11, PortalGate.State.ACTIVE );
		Portals.register( NEST, 42 ).discovered = true;
		Portals.setState( NEST, PortalGate.State.ACTIVE );
		//one lit but never found, one found but never lit: neither is a way
		Portals.register( SHOP16, 43 );
		Portals.setState( SHOP16, PortalGate.State.ACTIVE );
		Portals.register( Portals.key( 20, 0 ), 44 ).discovered = true;

		assertEquals( Arrays.asList( Portals.TOWN, SHOP11, NEST ), Portals.travelable( SHOP6 ) );
		assertEquals( Arrays.asList( SHOP6, SHOP11, NEST ), Portals.travelable( Portals.TOWN ) );

		//nothing puts it out: a trader fleeing on the surface marks the gate here dead
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Portals.markDead( Portals.keyHere() );
		Portals.setState( Portals.TOWN, PortalGate.State.DEAD );
		Portals.setState( Portals.TOWN, PortalGate.State.LOCKED );
		assertEquals( PortalGate.State.ACTIVE, Portals.getState( Portals.TOWN ) );
		assertEquals( Portals.TOWN, (int) Portals.travelable( SHOP6 ).get( 0 ) );
		//while a shop's still goes dark with its keeper
		Portals.markDead( SHOP11 );
		assertEquals( Arrays.asList( Portals.TOWN, NEST ), Portals.travelable( SHOP6 ) );

		//and its gate itself stays lit
		PortalGate gate = new PortalGate();
		gate.state = PortalGate.State.ACTIVE;
		gate.setState( PortalGate.State.DEAD );
		assertEquals( PortalGate.State.ACTIVE, gate.state );
	}

	@Test
	public void theTownGateIsNamedForTheTown(){
		assertEquals( "Dolyahaven", WndPortal.depthLabel( Portals.TOWN ) );
		assertEquals( "Prison — Floor 6", WndPortal.depthLabel( SHOP6 ) );
	}

	@Test
	public void anOldSaveKnowsTheTownToo() throws Exception {
		//the registry as a save from before the town had a gate holds it: two shops, no town
		Bundle old = new Bundle();
		old.put( "portal_depths", new int[]{ SHOP6, SHOP11 } );
		old.put( "portal_states", new int[]{ PortalGate.State.ACTIVE.ordinal(), PortalGate.State.DEAD.ordinal() } );
		old.put( "portal_cells", new int[]{ 300, 301 } );
		old.put( "portal_discovered", new boolean[]{ true, true } );
		Portals.restoreFromBundle( throughASave( old ) );
		assertEquals( PortalGate.State.ACTIVE, Portals.getState( Portals.TOWN ) );
		assertTrue( Portals.get( Portals.TOWN ).discovered );
		assertEquals( PortalGate.State.DEAD, Portals.getState( SHOP11 ) );
		assertEquals( Arrays.asList( Portals.TOWN ), Portals.travelable( SHOP6 ) );

		//a save with no gate in it at all
		Portals.restoreFromBundle( throughASave( new Bundle() ) );
		assertEquals( PortalGate.State.ACTIVE, Portals.getState( Portals.TOWN ) );

		//and a new save keeps it once, with its cell
		Portals.register( Portals.TOWN, 1234 );
		Bundle b = new Bundle();
		Portals.storeInBundle( b );
		Portals.reset();
		Portals.restoreFromBundle( throughASave( b ) );
		assertEquals( 1234, Portals.get( Portals.TOWN ).cell );
		int towns = 0;
		for (java.util.Map.Entry<Integer, Portals.Record> e : Portals.entries()) if (e.getKey() == Portals.TOWN) towns++;
		assertEquals( 1, towns );
	}

	// ------------------------------------------------------------------ the gate in the town

	@Test
	public void theGateStandsOnTheSquareAndBlocksNoWay(){
		OverworldLevel ow = town();
		List<PortalGate> gates = gates( ow );
		assertEquals( "one gate", 1, gates.size() );
		PortalGate gate = gates.get( 0 );
		int cell = gateCell( ow );
		assertEquals( "on its own cell", cell, gate.pos );
		assertTrue( ow.inTown( cell ) );
		assertEquals( PortalGate.State.ACTIVE, gate.state );
		assertEquals( "the registry knows where it stands", cell, Portals.get( Portals.TOWN ).cell );

		//open ground of the square's, near the town's middle, on no cell the town uses for anything else
		assertEquals( Terrain.EMPTY, ow.map[cell] );
		assertTrue( "near the middle of the town", Math.max(
				Math.abs( WorldStructures.TOWN_PORTAL % WorldStructures.TOWN_SIZE - WorldStructures.TOWN_SIZE / 2 ),
				Math.abs( WorldStructures.TOWN_PORTAL / WorldStructures.TOWN_SIZE - WorldStructures.TOWN_SIZE / 2 ) ) <= 4 );
		ArrayList<Integer> used = new ArrayList<>( Arrays.asList( WorldStructures.TOWN_PLAZA, WorldStructures.TOWN_MINE_GATE,
				WorldStructures.TOWN_DUNGEON_GATE, WorldStructures.TOWN_TEMPLE_DOOR, WorldStructures.TOWN_ALTAR, Waystones.TOWN_CELL ) );
		for (int d : WorldStructures.TOWN_DOORS) used.add( d );
		for (Object[] home : ow.sleeperHomes()) used.add( (Integer) home[1] );
		for (int c : (int[]) staticField( "TOWN_CHESTS" )) used.add( c );
		assertFalse( "a cell the town already uses", used.contains( WorldStructures.TOWN_PORTAL ) );
		for (int n : PathFinder.NEIGHBOURS8){
			assertNull( "no doorway or stairs beside it", ow.getTransition( cell + n ) );
			assertNotEquals( Terrain.DOOR, ow.map[cell + n] );
		}

		//shut, it cuts nobody off: whatever could be reached from the plaza still can, walking
		//straight or on the diagonals
		int plaza = ow.localCell( WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ),
				WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) );
		for (int[][] steps : new int[][][]{ STRAIGHT, ALL_WAYS }){
			HashSet<Integer> open = reach( ow, plaza, -1, steps );
			assertTrue( open.contains( cell ) );
			open.remove( cell );
			assertEquals( open, reach( ow, plaza, cell, steps ) );
		}

		//every pass over the town keeps the one gate
		call( ow, "populateTown", new Class<?>[0] );
		call( ow, "populateTown", new Class<?>[0] );
		assertEquals( 1, gates( ow ).size() );
		assertEquals( cell, gates( ow ).get( 0 ).pos );
	}

	private static Object staticField( String name ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			return f.get( null );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void theGateGoesWithTheWindowAndComesBackOnItsCell(){
		OverworldLevel ow = town();
		int wx = WorldStructures.townWorldX( WorldStructures.TOWN_PORTAL ), wy = WorldStructures.townWorldY( WorldStructures.TOWN_PORTAL );
		assertEquals( 1, gates( ow ).size() );

		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, wx + 900, wy );
		assertTrue( "gone with the town", gates( ow ).isEmpty() );
		for (Mob m : parked( ow ).values()) assertFalse( "never parked", m instanceof PortalGate );
		assertEquals( "away", -1, Portals.get( Portals.TOWN ).cell );

		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, wx + 20, wy - 10 );
		assertEquals( 1, gates( ow ).size() );
		assertEquals( ow.localCell( wx, wy ), gates( ow ).get( 0 ).pos );
		assertEquals( gates( ow ).get( 0 ).pos, Portals.get( Portals.TOWN ).cell );
	}

	@Test
	public void aSaveKeepsOneGateAndASaveFromBeforeItGetsOne() throws Exception {
		OverworldLevel ow = town();
		OverworldLevel loaded = saveAndLoad( ow );
		assertEquals( 1, gates( loaded ).size() );
		assertEquals( gateCell( loaded ), gates( loaded ).get( 0 ).pos );
		assertEquals( PortalGate.State.ACTIVE, gates( loaded ).get( 0 ).state );

		//a town saved before it had a gate
		loaded.mobs.removeIf( m -> m instanceof PortalGate );
		Portals.reset();
		OverworldLevel old = saveAndLoad( loaded );
		assertEquals( "put up as the save comes back", 1, gates( old ).size() );
		assertEquals( gateCell( old ), gates( old ).get( 0 ).pos );
		assertEquals( gates( old ).get( 0 ).pos, Portals.get( Portals.TOWN ).cell );
	}

	@Test
	public void somethingOnItsCellMovesTheGateAsideNeverOnTopOfIt(){
		OverworldLevel ow = windowAt( WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ),
				WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) );
		int cell = gateCell( ow );
		ow.drop( new xyz.gabriwar.warpedpixeldungeon.items.Gold( 5 ), cell );
		call( ow, "populateTown", new Class<?>[0] );
		PortalGate gate = gates( ow ).get( 0 );
		assertNotEquals( cell, gate.pos );
		assertTrue( ow.adjacent( cell, gate.pos ) );
		assertNotNull( ow.heaps.get( cell ) );
	}

	// ------------------------------------------------------------------ going

	@Test
	public void aTripToTheTownLandsBesideItsGate(){
		InterlevelScene.Mode mode = InterlevelScene.mode;
		int returnDepth = InterlevelScene.returnDepth, returnBranch = InterlevelScene.returnBranch, returnPos = InterlevelScene.returnPos;
		try {
			//leaving from somewhere off in the world
			windowAt( 2000, 2000 );
			WndPortal.leaveFor( Portals.TOWN, Portals.get( Portals.TOWN ) );
			assertEquals( InterlevelScene.Mode.RETURN, InterlevelScene.mode );
			assertEquals( OverworldLevel.DEPTH, InterlevelScene.returnDepth );
			assertEquals( 0, InterlevelScene.returnBranch );
			assertEquals( "the surface places the arrival itself", -1, InterlevelScene.returnPos );
			assertTrue( OverworldLevel.hasPendingArrival() );

			//the surface takes the arrival: beside the gate, never on it
			OverworldLevel ow = town();
			int gate = gates( ow ).get( 0 ).pos;
			int land = ow.getTransition( null ).cell();
			assertFalse( OverworldLevel.hasPendingArrival() );
			assertNotEquals( gate, land );
			assertTrue( ow.adjacent( land, gate ) );
			assertTrue( ow.passable[land] );

			//a shop's floor is still entered at its gate
			Portals.Record shop = Portals.register( SHOP11, 321 );
			WndPortal.leaveFor( SHOP11, shop );
			assertEquals( InterlevelScene.Mode.RETURN, InterlevelScene.mode );
			assertEquals( 11, InterlevelScene.returnDepth );
			assertEquals( 0, InterlevelScene.returnBranch );
			assertEquals( 321, InterlevelScene.returnPos );
			assertFalse( OverworldLevel.hasPendingArrival() );
		} finally {
			InterlevelScene.mode = mode;
			InterlevelScene.returnDepth = returnDepth;
			InterlevelScene.returnBranch = returnBranch;
			InterlevelScene.returnPos = returnPos;
		}
	}
}

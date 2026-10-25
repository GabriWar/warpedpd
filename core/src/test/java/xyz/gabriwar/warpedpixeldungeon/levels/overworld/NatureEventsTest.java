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

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Wildfire;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Paths;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Signal;
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The weather's and the sky's events (WildfireEvent, FloodEvent, EclipseEvent): where and when they
 * can happen is a pure function of the world and the day; a wildfire's spread is a pure function of
 * the fire, the cells kept from it and the rain, and never changes what came before a cell was kept
 * from it; what they lay on the land comes and goes with them and is never the hero's edit; the log
 * and the climate keep them through a save (and an old save loads without them); and the numbers
 * the climate gives them (fire weather in summer, floods in the wet seasons) come round as the
 * guide says.
 */
public class NatureEventsTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int FC = WorldEvents.FC;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int turn, startDay, challenges, depth, branch;
	private float shift;
	private boolean shiftHeld;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero;
	private long savedSeed;
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
		shift = WorldModel.seasonShift();
		shiftHeld = WorldModel.seasonShiftHeld();
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		savedSeed = Dungeon.seed;
		Actor.clear();
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		//no fire weather of its own and no flood on record: only what a test forces happens
		ClimateManager.reset();
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
		Dungeon.seed = savedSeed;
		ClimateManager.debugTempOverride = Float.NaN;
		ClimateManager.reset();
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	private static int[] spotCache, bankCache;

	//woodland or meadow a fire burns out from, some way out from the town
	private static int[] spot(){
		if (spotCache == null) spotCache = WildfireEvent.findSpot( SEED, 420, 260, 1400 );
		assertNotNull( "no woodland for a fire on the test world", spotCache );
		return spotCache;
	}

	//a low river bank
	private static int[] bank(){
		if (bankCache == null) bankCache = FloodEvent.findBank( SEED, 300, -200, 2, 2400 );
		assertNotNull( "no river bank on the test world", bankCache );
		return bankCache;
	}

	//the live surface window round a world cell, built as a mirror's is and made the host's own,
	//the hero on the nearest open cell (WorldEventsTest.windowAt)
	private OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, 0f );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, 0f, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		setField( ow, "network", false );
		Dungeon.level = ow;
		//the run whose world this is (OverworldLevel.worldSeedOf undoes itself)
		Dungeon.seed = OverworldLevel.worldSeedOf( SEED );
		hero.pos = -1;
		for (int r = 0; r <= 8 && hero.pos == -1; r++){
			for (int dy = -r; dy <= r && hero.pos == -1; dy++){
				for (int dx = -r; dx <= r && hero.pos == -1; dx++){
					int cell = ow.localCell( wx + dx, wy + dy );
					if (cell != -1 && ow.passable[cell] && !ow.occupied( cell )) hero.pos = cell;
				}
			}
		}
		assertNotEquals( -1, hero.pos );
		Arrays.fill( ow.heroFOV, false );
		return ow;
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

	@SuppressWarnings("unchecked")
	private static HashMap<Long, Integer> diffs( OverworldLevel ow ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( "diffs" );
			f.setAccessible( true );
			return (HashMap<Long, Integer>) f.get( ow );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static void captureDiffs( OverworldLevel ow ){
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( "captureDiffs" );
			m.setAccessible( true );
			m.invoke( ow );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	//is the event among the world map's pins over the window?
	private static boolean pinned( OverworldLevel ow, WorldEvents.Event e ){
		for (WorldEvents.Event p : ow.mapEvents( ow.worldX, ow.worldY, W )) if (p.id == e.id) return true;
		return false;
	}

	private static WorldEvents.Event fireAt( int[] at, int start ){
		return WildfireEvent.fire( at[0], at[1], start, WorldEvents.idOf( WorldEvents.Type.WILDFIRE,
				Math.floorDiv( at[0], WorldStructures.SECTOR ), Math.floorDiv( at[1], WorldStructures.SECTOR ), WorldEvents.day( start ) ) );
	}

	// ------------------------------------------------------------------ wildfires

	@Test
	public void aCampsFireIsPureAndStartsOnItsOwnLand(){
		HashSet<Long> ids = new HashSet<>();
		int candidates = 0, chances = 0;
		for (int sy = -12; sy <= 12; sy++){
			for (int sx = -12; sx <= 12; sx++){
				int[] at = WildfireEvent.spot( SEED, sx, sy );
				if (at != null){
					assertArrayEquals( at, WildfireEvent.spot( SEED, sx, sy ) );
					WorldModel.Biome b = WorldModel.baseBiomeAt( SEED, at[0], at[1] );
					assertTrue( b == WorldModel.Biome.FOREST || b == WorldModel.Biome.MEADOW );
					assertTrue( WildfireEvent.burns( WorldEvents.baseTerrain( SEED, at[0], at[1] ) ) );
					assertTrue( WildfireEvent.inLand( SEED, at[0], at[1] ) );
					assertEquals( sx, Math.floorDiv( at[0], WorldStructures.SECTOR ) );
					assertEquals( sy, Math.floorDiv( at[1], WorldStructures.SECTOR ) );
				}
				for (int d = 0; d < 60; d++){
					WorldEvents.Event c = WildfireEvent.candidate( SEED, sx, sy, d );
					if (at != null) chances++;
					if (c == null) continue;
					assertNotNull( at );
					WorldEvents.Event again = WildfireEvent.candidate( SEED, sx, sy, d );
					assertEquals( c.id, again.id );
					assertEquals( c.startTurn, again.startTurn );
					assertEquals( d, WorldEvents.day( c.startTurn ) );
					assertEquals( at[0], c.wx );
					assertTrue( ids.add( c.id ) );
					candidates++;
				}
			}
		}
		float share = candidates / (float) chances;
		assertTrue( "a spot's day comes one in " + WildfireEvent.ODDS + ": " + share,
				share > 0.6f / WildfireEvent.ODDS && share < 1.5f / WildfireEvent.ODDS );
	}

	@Test
	public void aFireSpreadsOverFuelOnlyInsideItsBoxAndItsTime(){
		int[] at = spot();
		WorldEvents.Event e = fireAt( at, 50 * FC );
		WorldEventLog log = new WorldEventLog();
		WildfireEvent.Blaze b = WildfireEvent.blaze( SEED, e, log );
		int r = WildfireEvent.radiusOf( e.id );
		assertEquals( 2 * r + 1, b.side );
		assertTrue( "it caught but spread nowhere: " + b.extent(), b.extent() > 20 );
		int prev = Integer.MIN_VALUE;
		int spreadEnd = e.startTurn + WildfireEvent.spreadOf( e.id );
		for (int i : b.order){
			int wx = b.wx( i ), wy = b.wy( i );
			assertTrue( Math.abs( wx - e.wx ) <= r && Math.abs( wy - e.wy ) <= r );
			assertTrue( WildfireEvent.burns( WorldEvents.baseTerrain( SEED, wx, wy ) ) );
			assertTrue( WildfireEvent.inLand( SEED, wx, wy ) );
			assertTrue( b.caught[i] >= e.startTurn && b.caught[i] < spreadEnd );
			assertTrue( b.out[i] > b.caught[i] && b.out[i] <= e.endTurn );
			assertTrue( b.caught[i] >= prev );
			prev = b.caught[i];
		}
		//the same fire, worked out afresh in another log: the very same burn
		WildfireEvent.Blaze again = WildfireEvent.blaze( SEED, e, new WorldEventLog() );
		assertArrayEquals( b.caught, again.caught );
		assertArrayEquals( b.out, again.out );
		//it reaches beyond its heart's eight cells and still burns at its hour, then not
		assertTrue( b.burningAt( b.order[0], e.startTurn ) );
		assertFalse( b.burningAt( b.order[0], b.out[b.order[0]] ) );
	}

	@Test
	public void aCellKeptFromTheFireChangesNothingBefore(){
		int[] at = spot();
		WorldEvents.Event e = fireAt( at, 50 * FC );
		WorldEventLog log = new WorldEventLog();
		WildfireEvent.Blaze before = WildfireEvent.blaze( SEED, e, log );
		//a cell well out from the heart, caught halfway through: kept from the fire from just before it caught
		int pick = before.order[before.order.length / 2];
		int t = before.caught[pick] - 1;
		long key = OverworldLevel.worldKey( before.wx( pick ), before.wy( pick ) );
		log.bar( key, t, e.lingersUntil() );
		WildfireEvent.Blaze after = WildfireEvent.blaze( SEED, e, log );
		assertEquals( WildfireEvent.NEVER, after.caught[pick] );
		for (int i = 0; i < before.caught.length; i++){
			if (before.caught[i] < t) assertEquals( "history changed at " + i, before.caught[i], after.caught[i] );
			//nothing new catches, and nothing earlier
			if (after.caught[i] != WildfireEvent.NEVER) assertTrue( after.caught[i] >= before.caught[i] );
		}
		//a burning cell doused: put out from that turn, and its flames spread no further
		int burning = before.order[before.order.length / 3];
		int when = before.caught[burning] + 1;
		WorldEventLog wet = new WorldEventLog();
		wet.bar( OverworldLevel.worldKey( before.wx( burning ), before.wy( burning ) ), when, e.lingersUntil() );
		WildfireEvent.Blaze doused = WildfireEvent.blaze( SEED, e, wet );
		assertEquals( before.caught[burning], doused.caught[burning] );
		assertTrue( doused.out[burning] <= Math.max( when, doused.caught[burning] + 1 ) );
		assertFalse( doused.burningAt( burning, when ) );
	}

	@Test
	public void rainPutsTheWholeFireOut(){
		int[] at = spot();
		WorldEvents.Event e = fireAt( at, 50 * FC );
		WorldEventLog log = new WorldEventLog();
		WildfireEvent.Blaze dry = WildfireEvent.blaze( SEED, e, log );
		int q = dry.caught[dry.order[dry.order.length / 2]];
		log.quench.put( e.id, q );
		WildfireEvent.Blaze wet = WildfireEvent.blaze( SEED, e, log );
		assertTrue( wet.extent() < dry.extent() );
		for (int i : wet.order){
			assertTrue( wet.caught[i] < q );
			assertTrue( wet.out[i] <= Math.max( q, wet.caught[i] + 1 ) );
			assertFalse( wet.burningAt( i, q + 1 ) );
		}
	}

	@Test
	public void aFiresScarGrowsBack(){
		int[] at = spot();
		WorldEvents.Event e = fireAt( at, 50 * FC );
		WildfireEvent.Blaze b = WildfireEvent.blaze( SEED, e, new WorldEventLog() );
		int trees = 0, ground = 0;
		for (int i : b.order){
			int base = WorldEvents.baseTerrain( SEED, b.wx( i ), b.wy( i ) );
			int out = b.out[i];
			assertEquals( -1, b.scar( i, b.caught[i] ) );
			int scar = b.scar( i, out );
			assertTrue( WorldEvents.layable( scar, base ) );
			if (base == Terrain.TREE_OAK || base == Terrain.TREE_PINE){
				trees++;
				assertEquals( base == Terrain.TREE_OAK ? Terrain.STUMP_OAK : Terrain.STUMP_PINE, scar );
				int sap = b.scar( i, out + WildfireEvent.SAPLING_TURNS );
				assertEquals( base == Terrain.TREE_OAK ? Terrain.SAPLING_OAK : Terrain.SAPLING_PINE, sap );
				assertTrue( WorldEvents.layable( sap, base ) );
				assertEquals( -1, b.scar( i, out + WildfireEvent.TREE_TURNS ) );
			} else {
				ground++;
				assertEquals( Terrain.EMBERS, scar );
				assertEquals( -1, b.scar( i, out + WildfireEvent.ASH_TURNS ) );
			}
		}
		assertTrue( ground > 0 );
		assertTrue( b.stage( e.startTurn ) < b.stage( e.endTurn ) );
		assertEquals( b.changes.length, b.stage( e.lingersUntil() ) );
		//the event keeps its scar about until the last of the trees is back
		assertTrue( e.lingersUntil() >= e.endTurn + WildfireEvent.TREE_TURNS );
		assertTrue( trees + ground == b.extent() );
	}

	@Test
	public void fireWeatherIsHotAndDry(){
		int[] at = spot();
		int now = 200 * FC;
		Dungeon.cycleTurn = now;
		ClimateManager.debugTempOverride = 30f;
		assertTrue( "dry since ever and hot", WildfireEvent.fireWeather( SEED, at[0], at[1], now ) );
		ClimateManager.debugTempOverride = 18f;
		assertFalse( "too cool", WildfireEvent.fireWeather( SEED, at[0], at[1], now ) );
		ClimateManager.debugTempOverride = 30f;
		setClimate( "lastRainTurn", now - WildfireEvent.DRY_TURNS / 2 );
		assertFalse( "it rained this morning", WildfireEvent.fireWeather( SEED, at[0], at[1], now ) );
		setClimate( "lastRainTurn", now - WildfireEvent.DRY_TURNS - 1 );
		assertTrue( WildfireEvent.fireWeather( SEED, at[0], at[1], now ) );
	}

	private static void setClimate( String name, Object v ){
		try {
			Field f = ClimateManager.class.getDeclaredField( name );
			f.setAccessible( true );
			f.set( null, v );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static Object climate( String name ){
		try {
			Field f = ClimateManager.class.getDeclaredField( name );
			f.setAccessible( true );
			return f.get( null );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void aLiveFireLaysItsScarNeverAsTheHerosEditAndTakesItBack(){
		int[] at = spot();
		Dungeon.cycleTurn = 60 * FC;
		OverworldLevel ow = windowAt( at[0] - 8, at[1] );
		WorldEvents.Event e = WildfireEvent.force( ow, at[0], at[1], FC / 2 );
		ow.pollEvents();
		int now = ow.eventTurn();
		WildfireEvent.Blaze b = WildfireEvent.blaze( SEED, e, ow.eventLog() );
		int laid = 0, burning = 0;
		for (int i : b.order){
			int c = ow.localCell( b.wx( i ), b.wy( i ) );
			if (c == -1) continue;
			if (b.burningAt( i, now )) burning++;
			int t = b.scar( i, now );
			if (t == -1 || !WorldEvents.layable( t, ow.pristineAt( c ) )) continue;
			assertEquals( "scar not laid at " + i, t, ow.map[c] );
			assertEquals( t, ow.laidAt( c ) );
			laid++;
		}
		assertTrue( "nothing burnt yet: " + laid, laid > 10 );
		assertTrue( "nothing alight: " + burning, burning > 0 );
		//the flames are the blob's, where the fire's reckoning has them
		Blob flames = ow.blobs.get( Wildfire.class );
		assertNotNull( flames );
		assertTrue( flames.volume > 0 );
		//pinned and heard of
		assertTrue( ow.eventAnnounced( e.id ) );
		assertTrue( pinned( ow, e ) );
		//never the hero's own edit
		captureDiffs( ow );
		for (int i : b.order){
			assertFalse( diffs( ow ).containsKey( OverworldLevel.worldKey( b.wx( i ), b.wy( i ) ) ) );
		}
		//water on a burning cell puts it out there and keeps the fire off it and its sides
		int wetCell = -1;
		for (int i : b.order){
			int c = ow.localCell( b.wx( i ), b.wy( i ) );
			if (c != -1 && b.burningAt( i, now )){
				wetCell = i;
				break;
			}
		}
		int c = ow.localCell( b.wx( wetCell ), b.wy( wetCell ) );
		assertTrue( WildfireEvent.doused( ow, c ) );
		assertTrue( ow.eventLog().barred( OverworldLevel.worldKey( b.wx( wetCell ), b.wy( wetCell ) ), now ) );
		assertTrue( ow.eventLog().barred( OverworldLevel.worldKey( b.wx( wetCell ) + 1, b.wy( wetCell ) ), now ) );
		assertFalse( WildfireEvent.blaze( SEED, e, ow.eventLog() ).burningAt( wetCell, now + 1 ) );
		//all grown back once its scar is gone
		Dungeon.cycleTurn = e.lingersUntil() + 1;
		ow.pollEvents();
		for (int i : b.order){
			int cell = ow.localCell( b.wx( i ), b.wy( i ) );
			if (cell == -1) continue;
			assertEquals( ow.pristineAt( cell ), ow.map[cell] );
		}
		captureDiffs( ow );
		assertTrue( diffs( ow ).isEmpty() );
	}

	@Test
	public void theHerosWorkInTheBoxIsAFirebreak(){
		int[] at = spot();
		Dungeon.cycleTurn = 60 * FC;
		OverworldLevel ow = windowAt( at[0] - 8, at[1] );
		WorldEvents.Event probe = fireAt( at, ow.eventTurn() );
		WildfireEvent.Blaze free = WildfireEvent.blaze( SEED, probe, new WorldEventLog() );
		//a ring of the hero's stone floor round the fire's heart, laid before it catches
		int heart = ow.localCell( at[0], at[1] );
		int w = ow.width();
		for (int dy = -3; dy <= 3; dy++){
			for (int dx = -3; dx <= 3; dx++){
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != 3) continue;
				xyz.gabriwar.warpedpixeldungeon.levels.Level.set( heart + dx + dy * w, Terrain.FLOOR_STONE, ow );
			}
		}
		captureDiffs( ow );
		WorldEvents.Event e = WildfireEvent.force( ow, at[0], at[1], 0 );
		WildfireEvent.Blaze ringed = WildfireEvent.blaze( SEED, e, ow.eventLog() );
		assertTrue( free.extent() > ringed.extent() );
		for (int i : ringed.order){
			assertTrue( "it jumped the ring", Math.max( Math.abs( ringed.wx( i ) - at[0] ), Math.abs( ringed.wy( i ) - at[1] ) ) < 3 );
		}
	}

	//a tree a storm struck flares into a fire of its own: never under its sector's camp chance of the
	//day, which may be settled already (a fire under it would burn on, put out by nothing, pinned nowhere)
	@Test
	public void aStruckTreeFlaresIntoAFireOfItsOwn(){
		int[] at = spot();
		Dungeon.cycleTurn = 64 * FC + 900;
		OverworldLevel ow = windowAt( at[0] - 8, at[1] );
		int now = ow.eventTurn();
		//that sector's camp chance today, settled: the weather was not the fire's at its hour
		long chance = WorldEvents.idOf( WorldEvents.Type.WILDFIRE, Math.floorDiv( at[0], WorldStructures.SECTOR ),
				Math.floorDiv( at[1], WorldStructures.SECTOR ), WorldEvents.day( now ) );
		ow.resolveEvent( chance, now + FC );
		//a tree struck a day ago smoulders, and it has been hot and dry since
		ow.eventLog().holdovers.add( new int[]{ at[0], at[1], now - FC } );
		ClimateManager.debugTempOverride = 30f;
		ow.pollEvents();
		WorldEvents.Event fire = null;
		for (WorldEvents.Event e : ow.eventLog().forced){
			if (e.type == WorldEvents.Type.WILDFIRE && e.wx == at[0] && e.wy == at[1]) fire = e;
		}
		assertNotNull( "the struck tree never flared", fire );
		assertTrue( ow.eventLog().holdovers.isEmpty() );
		assertNotEquals( chance, fire.id );
		assertFalse( ow.eventResolved( fire.id ) );
		assertTrue( pinned( ow, fire ) );
		//water on its flames puts them out there
		WildfireEvent.Blaze b = WildfireEvent.blaze( SEED, fire, ow.eventLog() );
		assertTrue( b.burningAt( b.index( at[0], at[1] ), now ) );
		assertTrue( WildfireEvent.doused( ow, ow.localCell( at[0], at[1] ) ) );
	}

	//a homestead by where a fire caught is told so in words of its own: never a way with no name
	@Test
	public void aHomesteadByTheFireIsWarned(){
		int[] at = spot();
		Dungeon.cycleTurn = 66 * FC;
		OverworldLevel ow = windowAt( at[0] - 8, at[1] );
		//a fence piece six cells west of where the fire catches (the debug scene's lay-out)
		int fence = -1;
		for (int dy = 0; dy <= 6 && fence == -1; dy++){
			for (int s = -1; s <= 1 && fence == -1; s += 2){
				int c = ow.localCell( at[0] - 6, at[1] + s * dy );
				if (c != -1 && c != hero.pos && ow.passable[c] && !ow.water[c] && ow.heaps.get( c ) == null) fence = c;
			}
		}
		assertNotEquals( -1, fence );
		Homestead.build( ow, fence, BuildCatalog.byId( "fence" ), hero );
		final ArrayList<String> lines = new ArrayList<>();
		Signal.Listener<String> listen = lines::add;
		GLog.update.add( listen );
		try {
			WildfireEvent.force( ow, at[0], at[1], 0 );
			ow.pollEvents();
		} finally {
			GLog.update.remove( listen );
		}
		String warn = Messages.get( WildfireEvent.class, "warn_here" );
		boolean warned = false;
		for (String l : lines){
			assertFalse( l, l.contains( Messages.NO_TEXT_FOUND ) );
			warned |= l.contains( warn );
		}
		assertTrue( "no warning: " + lines, warned );
	}

	// ------------------------------------------------------------------ floods

	@Test
	public void aFloodRisesStandsAndDrainsLeavingMud(){
		int s = 10 * FC, peak = 5;
		assertEquals( 0, FloodEvent.waterLevel( s, peak, s - 1 ) );
		assertEquals( 1, FloodEvent.waterLevel( s, peak, s ) );
		int last = 0;
		for (int t = s; t < s + FloodEvent.RISE_TURNS; t++){
			int l = FloodEvent.waterLevel( s, peak, t );
			assertTrue( l >= last && l <= peak );
			last = l;
			assertEquals( 0, FloodEvent.mudLevel( s, peak, t ) );
		}
		int high = s + FloodEvent.RISE_TURNS;
		assertEquals( peak, FloodEvent.waterLevel( s, peak, high ) );
		assertEquals( peak, FloodEvent.waterLevel( s, peak, high + FloodEvent.HIGH_TURNS - 1 ) );
		int fall = high + FloodEvent.HIGH_TURNS;
		assertTrue( FloodEvent.waterLevel( s, peak, fall ) < peak );
		assertEquals( peak, FloodEvent.mudLevel( s, peak, fall ) );
		assertEquals( 0, FloodEvent.waterLevel( s, peak, FloodEvent.endOf( s ) ) );
		assertEquals( peak, FloodEvent.mudLevel( s, peak, FloodEvent.endOf( s ) + FloodEvent.MUD_TURNS - 1 ) );
		assertEquals( 0, FloodEvent.mudLevel( s, peak, FloodEvent.endOf( s ) + FloodEvent.MUD_TURNS ) );

		int packed = FloodEvent.pack( 3, 5 );
		assertEquals( 3, FloodEvent.water( packed ) );
		assertEquals( 5, FloodEvent.mud( packed ) );
		assertEquals( Terrain.WATER, FloodEvent.laid( Terrain.GRASS, 2, false, packed ) );
		assertEquals( Terrain.MUD, FloodEvent.laid( Terrain.DIRT_PATH, 2, false, packed ) );
		assertEquals( Terrain.MUD, FloodEvent.laid( Terrain.GRASS, 4, false, packed ) );
		assertEquals( -1, FloodEvent.laid( Terrain.GRASS, 6, false, packed ) );
		assertEquals( -1, FloodEvent.laid( Terrain.GRASS, 0, false, packed ) );
		assertEquals( -1, FloodEvent.laid( Terrain.TREE_OAK, 1, false, packed ) );
		assertEquals( -1, FloodEvent.laid( Terrain.GRASS, 1, true, packed ) );
		assertEquals( -1, FloodEvent.laid( Terrain.GRASS, 1, false, 0 ) );
		assertTrue( WorldEvents.layable( Terrain.MUD, Terrain.DIRT_PATH ) );
		assertFalse( WorldEvents.layable( Terrain.WATER, Terrain.DIRT_PATH ) );

		//the wet seasons only, the ground soaked, above freezing, a gap between
		assertTrue( FloodEvent.rises( GameCalendar.Season.SPRING, FloodEvent.SOAK_FLOOD, 10f, FloodEvent.NONE, 0 ) );
		assertTrue( FloodEvent.rises( GameCalendar.Season.AUTUMN, FloodEvent.SOAK_FLOOD + 1, 10f, FloodEvent.NONE, 0 ) );
		assertFalse( FloodEvent.rises( GameCalendar.Season.SUMMER, FloodEvent.SOAK_CAP, 20f, FloodEvent.NONE, 0 ) );
		assertFalse( FloodEvent.rises( GameCalendar.Season.WINTER, FloodEvent.SOAK_CAP, 5f, FloodEvent.NONE, 0 ) );
		assertFalse( FloodEvent.rises( GameCalendar.Season.SPRING, FloodEvent.SOAK_FLOOD - 1, 10f, FloodEvent.NONE, 0 ) );
		assertFalse( FloodEvent.rises( GameCalendar.Season.SPRING, FloodEvent.SOAK_FLOOD, 1f, FloodEvent.NONE, 0 ) );
		assertFalse( FloodEvent.rises( GameCalendar.Season.SPRING, FloodEvent.SOAK_FLOOD, 10f, 100, 100 + FloodEvent.GAP_TURNS - 1 ) );
		assertTrue( FloodEvent.rises( GameCalendar.Season.SPRING, FloodEvent.SOAK_FLOOD, 10f, 100, 100 + FloodEvent.GAP_TURNS ) );
		assertEquals( FloodEvent.MIN_PEAK, FloodEvent.peakFor( FloodEvent.SOAK_FLOOD ) );
		assertEquals( FloodEvent.LEVELS, FloodEvent.peakFor( FloodEvent.SOAK_CAP ) );
	}

	@Test
	public void theBanksAreTheRiversOwnAndPure(){
		int[] b = bank();
		int ox = b[0] - W / 2, oy = b[1] - H / 2;
		WindowGenerator.Window one = WindowGenerator.generate( SEED, 0, ox, oy, 0f );
		WindowGenerator.Window two = WindowGenerator.generate( SEED, 0, ox, oy, 0f );
		assertArrayEquals( one.bank, two.bank );
		WorldModel.Sample s = new WorldModel.Sample();
		int banks = 0;
		for (int c = 0; c < one.bank.length; c++){
			if (one.bank[c] == 0) continue;
			banks++;
			assertTrue( one.bank[c] >= 1 && one.bank[c] <= FloodEvent.LEVELS );
			int t = one.terrain[c];
			assertTrue( t != Terrain.WATER && t != Terrain.DEEP_WATER && t != Terrain.FROZEN_WATER );
			if (banks % 7 != 0) continue;
			WorldModel.sample( SEED, ox + c % W, oy + c / W, 0f, s );
			assertTrue( s.river > 0f );
		}
		assertTrue( "no bank about a river: " + banks, banks > 20 );
		assertNull( WindowGenerator.generate( SEED, 3, ox, oy, 0f ).bank );
	}

	@Test
	public void aFloodLaysWaterThenMudAndNeverAsTheHerosEdit(){
		int[] b = bank();
		Dungeon.cycleTurn = 30 * FC;
		OverworldLevel ow = windowAt( b[0], b[1] );
		int now = ow.eventTurn();
		FloodEvent.force( now - FloodEvent.RISE_TURNS, 5 );
		ow.pollEvents();
		int water = 0;
		for (int c = 0; c < ow.length(); c++){
			int lvl = ow.bankAt( c );
			int want = FloodEvent.laid( ow.pristineAt( c ), lvl, ow.frozenAt( c ), FloodEvent.levels( now ) );
			if (want == -1 || ow.localCell( ow.worldX + c % W, ow.worldY + c / W ) == -1) continue;
			if (ow.linkAt( c ) != WindowGenerator.LINK_NONE) continue;
			assertEquals( want, ow.map[c] );
			if (want == Terrain.WATER) water++;
		}
		assertTrue( "nothing flooded: " + water, water > 10 );
		//heard of and pinned
		WorldEvents.Event e = FloodEvent.of( ow.eventLog(), now - FloodEvent.RISE_TURNS );
		assertNotNull( e );
		assertTrue( ow.eventAnnounced( e.id ) );
		captureDiffs( ow );
		assertTrue( diffs( ow ).isEmpty() );
		//drained: mud on the banks
		Dungeon.cycleTurn = FloodEvent.endOf( now - FloodEvent.RISE_TURNS ) + 10;
		ow.pollEvents();
		int mud = 0;
		for (int c = 0; c < ow.length(); c++){
			if (ow.map[c] == Terrain.WATER && ow.laidAt( c ) == Terrain.WATER) throw new AssertionError( "water left at " + c );
			if (ow.map[c] == Terrain.MUD) mud++;
		}
		assertTrue( mud >= water );
		captureDiffs( ow );
		assertTrue( diffs( ow ).isEmpty() );
		//and the land its own again after the mud's days
		Dungeon.cycleTurn = FloodEvent.endOf( now - FloodEvent.RISE_TURNS ) + FloodEvent.MUD_TURNS + 10;
		ow.pollEvents();
		for (int c = 0; c < ow.length(); c++) assertNotEquals( Terrain.MUD, ow.map[c] );
	}

	@Test
	public void mudIsSlowGoing(){
		assertTrue( Paths.MUD_SLOW < 1f );
		assertTrue( (Terrain.flags[Terrain.MUD] & Terrain.PASSABLE) != 0 );
		assertEquals( 0, Terrain.flags[Terrain.MUD] & (Terrain.SOLID | Terrain.FLAMABLE | Terrain.LIQUID) );
		assertFalse( WildfireEvent.burns( Terrain.MUD ) );
	}

	//a road under a flood's mud is still the road its walkers keep to (RoadWalker, roadMask); a bank's
	//mud off the road is not
	@Test
	public void aRoadUnderMudIsStillTheRoad(){
		int[] road = null;
		for (int r = WorldEvents.TOWN_CLEAR + 8; r < 400 && road == null; r++){
			for (int k = -r; k <= r && road == null; k++){
				int[][] ring = { { k, -r }, { k, r }, { -r, k }, { r, k } };
				for (int[] p : ring){
					if (WorldEvents.baseTerrain( SEED, p[0], p[1] ) == Terrain.DIRT_PATH){
						road = p;
						break;
					}
				}
			}
		}
		assertNotNull( "no road out of the town", road );
		OverworldLevel ow = windowAt( road[0], road[1] );
		int c = ow.localCell( road[0], road[1] );
		assertEquals( Terrain.DIRT_PATH, ow.pristineAt( c ) );
		int off = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			if (FloodEvent.floodable( ow.pristineAt( c + n ) )){
				off = c + n;
				break;
			}
		}
		assertNotEquals( "no open ground beside the road", -1, off );
		Level.set( c, Terrain.MUD, ow );
		Level.set( off, Terrain.MUD, ow );
		setField( ow, "roadMask", null );
		boolean[] mask = ow.roadMask();
		assertTrue( mask[c] );
		assertFalse( mask[off] );
	}

	// ------------------------------------------------------------------ eclipses

	@Test
	public void eclipsesComeAtNewMoonsByDayOnEitherClock(){
		int eclipses = 0, newMoons = 0;
		for (int d = 0; d < 400; d++){
			boolean nm = EclipseEvent.newMoonDay( d );
			if (nm) newMoons++;
			int[] w = EclipseEvent.window( SEED, d );
			assertEquals( w != null, EclipseEvent.scheduled( SEED, d ) );
			if (w == null) continue;
			assertTrue( nm );
			eclipses++;
			assertEquals( EclipseEvent.TURNS, w[1] - w[0] );
			//inside the day's DAY phase, its own season's
			int dayFrom = d * FC + DayNightCycle.phaseDuration( DayNightCycle.Phase.DAWN );
			int dayTo = dayFrom + DayNightCycle.dayTurns( GameCalendar.seasonOfDay( d ) );
			assertTrue( w[0] >= dayFrom && w[1] <= dayTo );
			assertArrayEquals( w, EclipseEvent.window( SEED, d ) );
		}
		assertTrue( "a new moon a lunation: " + newMoons, newMoons >= 13 && newMoons <= 14 );
		assertTrue( "about one new moon in two: " + eclipses, eclipses >= 3 && eclipses <= 11 );

		//a real-clock run: the real moon's new moons, around local noon (daylight all year)
		Dungeon.challenges = Challenges.REAL_CLOCK;
		int real = 0, realMoons = 0;
		for (int d = 19000; d < 19365; d++){
			if (EclipseEvent.newMoonDay( d )) realMoons++;
			int[] w = EclipseEvent.window( SEED, d );
			if (w == null) continue;
			real++;
			float from = (w[0] - d * FC) * 24f / FC, to = (w[1] - d * FC) * 24f / FC;
			assertTrue( from >= 10.5f && to <= 13.5f );
		}
		assertTrue( realMoons >= 12 && realMoons <= 13 );
		assertTrue( real >= 2 && real <= 11 );
		Dungeon.challenges = 0;
	}

	@Test
	public void theDarkComesInAndGoesOut(){
		int from = 1000, to = from + EclipseEvent.TURNS;
		assertEquals( 0f, EclipseEvent.depth( from, to, from - 1 ), 0f );
		assertEquals( 0f, EclipseEvent.depth( from, to, to ), 0f );
		assertEquals( 1f, EclipseEvent.depth( from, to, (from + to) / 2 ), 0f );
		float last = -1f;
		for (int t = from; t <= (from + to) / 2; t++){
			float d = EclipseEvent.depth( from, to, t );
			assertTrue( d >= last );
			last = d;
		}
		assertEquals( 1f - EclipseEvent.DEPTH, EclipseEvent.light( from, to, (from + to) / 2 ), 1e-6f );
		assertEquals( 1f, EclipseEvent.light( from, to, from - 5 ), 0f );
		//dark magic runs strong while the sun is covered, and the beasts keep the night's hour
		assertEquals( 1f, EclipseEvent.corruptionBoost(), 0f );
		DayNightCycle.Phase was = DayNightCycle.debugPhaseOverride;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		assertEquals( DayNightCycle.Phase.DAY, OverworldFauna.phaseNow( 0 ) );
		ClimateManager.debugToggleSolarEclipse();
		try {
			assertEquals( EclipseEvent.CORRUPTION, EclipseEvent.corruptionBoost(), 0f );
			assertEquals( DayNightCycle.Phase.NIGHT, OverworldFauna.phaseNow( 0 ) );
			assertEquals( DayNightCycle.Phase.NIGHT, OverworldFauna.phaseNow( 4 ) );
			//under the rock there is no sky to darken
			assertEquals( DayNightCycle.Phase.DAY, OverworldFauna.phaseNow( -3 ) );
		} finally {
			ClimateManager.debugToggleSolarEclipse();
			DayNightCycle.debugPhaseOverride = was;
		}
	}

	@Test
	public void anEclipseIsHeraldedByTheEldersAndDarkensTheSun(){
		Dungeon.cycleTurn = 70 * FC + 400;
		int[] v = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 1, 10 ).get( 0 );
		OverworldLevel ow = windowAt( v[2] + 6, v[3] );
		WorldEvents.Event e = EclipseEvent.force( ow, 20 );
		assertEquals( EclipseEvent.TURNS, e.endTurn - EclipseEvent.darkFrom( e ) );
		assertEquals( v[2], e.wx );
		assertTrue( ow.eventAnnounced( e.id ) );
		//pinned from the word on to the end of the dark
		assertTrue( pinned( ow, e ) );
		int now = ow.eventTurn();
		assertNull( EclipseEvent.windowAt( now ) );
		assertArrayEquals( new int[]{ EclipseEvent.darkFrom( e ), e.endTurn }, EclipseEvent.windowAt( now + 20 ) );
		//the climate darkens the sun through it
		Dungeon.cycleTurn = now + 20 + EclipseEvent.TURNS / 2;
		ClimateManager.onHeroTurn();
		assertTrue( ClimateManager.isSolarEclipse() );
		assertTrue( ClimateManager.eclipseDepth() > 0.9f );
		Dungeon.cycleTurn = e.endTurn + 1;
		ClimateManager.onHeroTurn();
		assertFalse( ClimateManager.isSolarEclipse() );
		assertEquals( 0f, ClimateManager.eclipseDepth(), 0f );
	}

	// ------------------------------------------------------------------ saves

	@Test
	public void theLogKeepsTheFiresThroughASaveAndAnOldSaveLoads(){
		WorldEventLog log = new WorldEventLog();
		int[] at = spot();
		WorldEvents.Event fire = fireAt( at, 40 * FC );
		WorldEvents.Event flood = new WorldEvents.Event( WorldEvents.Type.FLOOD, 1, 2, 41, 41 * FC, FloodEvent.endOf( 41 * FC ), 100, 200, 77L );
		WorldEvents.Event dark = new WorldEvents.Event( WorldEvents.Type.ECLIPSE, 3, 4, 42, 41 * FC, 42 * FC + 900, 300, 400, 78L );
		log.force( fire );
		log.force( flood );
		log.force( dark );
		log.quench.put( fire.id, 40 * FC + 777 );
		log.bar( OverworldLevel.worldKey( at[0] + 1, at[1] ), 40 * FC + 5, fire.lingersUntil() );
		log.holdovers.add( new int[]{ 10, 20, 40 * FC } );
		Bundle b = new Bundle();
		log.storeInBundle( b );
		WorldEventLog back = new WorldEventLog();
		back.restoreFromBundle( b );
		assertEquals( 3, back.forced.size() );
		HashSet<WorldEvents.Type> types = new HashSet<>();
		for (WorldEvents.Event e : back.forced) types.add( e.type );
		assertTrue( types.contains( WorldEvents.Type.WILDFIRE ) && types.contains( WorldEvents.Type.FLOOD ) && types.contains( WorldEvents.Type.ECLIPSE ) );
		assertEquals( Integer.valueOf( 40 * FC + 777 ), back.quench.get( fire.id ) );
		assertTrue( back.barred( OverworldLevel.worldKey( at[0] + 1, at[1] ), 40 * FC + 5 ) );
		assertFalse( back.barred( OverworldLevel.worldKey( at[0] + 1, at[1] ), 40 * FC + 4 ) );
		assertEquals( 1, back.holdovers.size() );
		assertArrayEquals( new int[]{ 10, 20, 40 * FC }, back.holdovers.get( 0 ) );
		//the burn worked out in the restored log is the same burn
		assertArrayEquals( WildfireEvent.blaze( SEED, fire, log ).caught, WildfireEvent.blaze( SEED, fire, back ).caught );
		//a save from before the wildfires: none of it, nothing broken
		for (String k : new String[]{ "event_quench_ids", "event_quench_turns", "event_bar_keys", "event_bar_turns", "event_hold_data" }){
			b.remove( k );
		}
		WorldEventLog old = new WorldEventLog();
		old.restoreFromBundle( b );
		assertTrue( old.quench.isEmpty() && old.bars.isEmpty() && old.holdovers.isEmpty() );
		//forgotten once their scars are gone
		back.prune( fire.lingersUntil() + FC * 30 );
		assertTrue( back.forced.isEmpty() && back.quench.isEmpty() && back.bars.isEmpty() && back.holdovers.isEmpty() );
	}

	@Test
	public void theClimateKeepsTheRainsMemoryAndTheFlood(){
		setClimate( "lastRainTurn", 12345 );
		setClimate( "rainStarts", new int[]{ 100, 2000, 9000 } );
		setClimate( "soak", 321f );
		ClimateManager.debugFlood( 7000, 4 );
		setClimate( "soak", 321f );
		Bundle b = new Bundle();
		ClimateManager.storeInBundle( b );
		ClimateManager.reset();
		assertEquals( Integer.MIN_VALUE, ClimateManager.lastRainTurn() );
		assertEquals( FloodEvent.NONE, ClimateManager.floodStart() );
		ClimateManager.restoreFromBundle( b );
		assertEquals( 12345, ClimateManager.lastRainTurn() );
		assertEquals( 2000, ClimateManager.firstRainAfter( 1000 ) );
		assertEquals( Integer.MAX_VALUE, ClimateManager.firstRainAfter( 9001 ) );
		assertEquals( 321f, ClimateManager.soak(), 0f );
		assertEquals( 7000, ClimateManager.floodStart() );
		assertEquals( 4, ClimateManager.floodPeak() );
		//a save from before: dry since ever, no flood
		Bundle inner = b.getBundle( "climate" );
		for (String k : new String[]{ "rainLast", "rainStarts", "raining", "soak", "floodStart", "floodPeak" }) inner.remove( k );
		ClimateManager.restoreFromBundle( b );
		assertEquals( Integer.MIN_VALUE, ClimateManager.lastRainTurn() );
		assertEquals( Integer.MAX_VALUE, ClimateManager.firstRainAfter( 0 ) );
		assertEquals( FloodEvent.NONE, ClimateManager.floodStart() );
		assertEquals( 0f, ClimateManager.soak(), 0f );
	}

	//the world's eclipse a save was made in ends with its window after a load, however late (a
	//real-clock run's next morning): never counted down as the sky's chance one, all but black
	@Test
	public void anEclipseSavedInItsDarkEndsWithItsWindow(){
		setClimate( "scheduledFrom", 1000 );
		setClimate( "scheduledTo", 1000 + EclipseEvent.TURNS );
		setClimate( "solarEclipseActive", true );
		setClimate( "solarEclipseRemaining", 200 );
		Bundle b = new Bundle();
		ClimateManager.storeInBundle( b );
		ClimateManager.reset();
		//loaded long after it: day 2 is no new moon's (calendarStartDay 0), and no chance eclipse
		//is rolled off the turn 50s. The load works the light out at once, then the turns go on
		Dungeon.level = null;
		Dungeon.cycleTurn = 2 * FC + 901;
		ClimateManager.restoreFromBundle( b );
		ClimateManager.onHeroTurn();
		assertFalse( ClimateManager.isSolarEclipse() );
		assertEquals( 0f, ClimateManager.eclipseDepth(), 0f );
	}

	// ------------------------------------------------------------------ the climate's numbers

	//a year of the climate for three worlds: summer brings fire weather on some days, not all; the
	//wet seasons bring a flood or two a year, never in summer or winter
	@Test
	public void theClimateBringsFireWeatherAndFloodsInTheirSeasons(){
		Dungeon.level = null;
		int fireDays = 0, summerDays = 0, floods = 0, wetFloods = 0;
		for (int seed = 1; seed <= 3; seed++){
			ClimateManager.reset();
			Dungeon.cycleTurn = 0;
			ClimateManager.onNewGame( seed * 7919L );
			int lastFlood = FloodEvent.NONE;
			boolean fireToday = false;
			for (int t = 0; t < 364 * FC; t++){
				Dungeon.cycleTurn = t;
				ClimateManager.onHeroTurn();
				GameCalendar.Season s = GameCalendar.season();
				if (t % FC == 0){
					if (fireToday) fireDays++;
					fireToday = false;
					if (s == GameCalendar.Season.SUMMER) summerDays++;
				}
				if (s == GameCalendar.Season.SUMMER && !fireToday && t % 25 == 0){
					fireToday = WildfireEvent.dryFor( t ) >= WildfireEvent.DRY_TURNS
							&& ClimateManager.surfaceTempIn( WorldModel.Biome.MEADOW ) >= WildfireEvent.FIRE_C;
				}
				if (ClimateManager.floodStart() != lastFlood){
					lastFlood = ClimateManager.floodStart();
					floods++;
					if (FloodEvent.wetSeason( s )) wetFloods++;
				}
			}
		}
		float share = fireDays / (float) summerDays;
		System.out.println( "fire weather on " + fireDays + " of " + summerDays + " summer days, " + floods + " floods in 3 years" );
		assertTrue( "fire weather on " + share + " of summer days", share > 0.05f && share < 0.9f );
		assertEquals( floods, wetFloods );
		assertTrue( "floods in three years: " + floods, floods >= 2 && floods <= 30 );
	}

	@Test
	public void eachKindHasItsOwnSaltAndPin() throws Exception {
		HashSet<Long> salts = new HashSet<>();
		HashSet<Integer> pins = new HashSet<>();
		for (String marker : new String[]{ "MARKER", "WAYPOINT_MARKER" }){
			Field f = xyz.gabriwar.warpedpixeldungeon.windows.WndWorldMap.class.getDeclaredField( marker );
			f.setAccessible( true );
			pins.add( (Integer) f.get( null ) );
		}
		for (WorldEvents.Type t : WorldEvents.Type.values()){
			assertTrue( t + " shares a salt", salts.add( t.salt ) );
			assertTrue( t + " shares a pin colour", pins.add( t.pin ) );
			assertEquals( 0xFF000000, t.pin & 0xFF000000 );
			assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( WorldEvents.class, "map_" + t.key ) );
		}
		//each says itself: none is announced by the stars' and markets' rule
		assertEquals( -1, WorldEvents.Type.WILDFIRE.announceSectors );
		assertEquals( -1, WorldEvents.Type.FLOOD.announceSectors );
		assertEquals( -1, WorldEvents.Type.ECLIPSE.announceSectors );
	}

	@Test
	public void everyNatureStringResolves(){
		for (WorldEvents.Type t : new WorldEvents.Type[]{ WorldEvents.Type.WILDFIRE, WorldEvents.Type.FLOOD, WorldEvents.Type.ECLIPSE }){
			assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( WorldEvents.class, "map_" + t.key ) );
		}
		for (String k : new String[]{ "starts_camp", "starts_lightning" }){
			assertTrue( Messages.get( WildfireEvent.class, k, "north" ).contains( "north" ) );
		}
		for (String k : new String[]{ "starts_here_camp", "starts_here_lightning", "quenched", "burnt_out", "close", "warn_here" }){
			assertNotEquals( k, Messages.NO_TEXT_FOUND, Messages.get( WildfireEvent.class, k ) );
		}
		assertTrue( Messages.get( WildfireEvent.class, "warn", "east" ).contains( "east" ) );
		assertTrue( Messages.get( FloodEvent.class, "rises", "west" ).contains( "west" ) );
		assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( FloodEvent.class, "rises_here" ) );
		assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( FloodEvent.class, "falls" ) );
		String herald = Messages.get( EclipseEvent.class, "herald", "Ashford", Messages.get( EclipseEvent.class, "tomorrow" ) );
		assertTrue( herald.contains( "Ashford" ) && herald.contains( "tomorrow" ) );
		for (String k : new String[]{ "herald_road", "begins", "ends", "today" }){
			assertNotEquals( k, Messages.NO_TEXT_FOUND, Messages.get( EclipseEvent.class, k, "today" ) );
		}
		for (String k : new String[]{ "mud", "mud_desc", "floodwater", "floodwater_desc", "burnt_stump", "burnt_stump_desc" }){
			assertNotEquals( k, Messages.NO_TEXT_FOUND, Messages.get( OverworldLevel.class, k ) );
		}
		assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( Wildfire.class, "desc" ) );
		for (int k = 0; k < 3; k++){
			assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager.class, "eclipse_" + k ) );
			assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.GnollVillager.class, "eclipse_" + k ) );
		}
		for (int k = 0; k < 2; k++){
			assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager.class, "eclipse_now_" + k ) );
			assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.GnollVillager.class, "eclipse_now_" + k ) );
		}
		for (String k : new String[]{ "drowned", "scorched" }){
			assertTrue( Messages.get( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop.class, k, "cornwheat" ).contains( "cornwheat" ) );
		}
	}
}

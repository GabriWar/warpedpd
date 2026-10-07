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
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant;
import xyz.gabriwar.warpedpixeldungeon.effects.StarStreak;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.StarFragment;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.Artifact;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.ExoticPotion;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ExoticScroll;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndWorldMap;
import com.watabou.noosa.Game;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The world's timed events on paper (WorldEvents, WorldEventLog): placement is a pure function
 * of the seed, the sector and the day; the stars fall about every four to six days near a hero,
 * at night, on open land away from water, the town and every settlement; the markets come to
 * human villages for three days every week or two; a forced event goes through the very same
 * enumeration; an event is announced once and pinned while it is on, heard of and unsettled;
 * and the log survives a save while an older save loads without it.
 *
 * Then on real windows of the world (OverworldLevel): a star comes down on its crater - only on
 * ground the window's own season left open, never captured as the player's edit - with its
 * tagged fragment, which settles the star once picked up, cools back to the land on its own
 * clock, and survives a save without doubling; a market pitches its stalls once, keeps them
 * through a far trip and back, strikes only their own shelves when it ends or is chased off;
 * the traders' stock, words and prices; the streak's trail; and the troll's star-forging.
 */
public class WorldEventsTest {

	private static final long SEED = 0x5EED0F7EA7L, SEED2 = 0xC0FFEE1234L;
	private static final int FC = WorldEvents.FC;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
	private static final List<WorldEvents.Event> NONE = Collections.emptyList();

	private int turn, startDay, challenges, depth, branch, gold;
	private DayNightCycle.Phase override;
	private float shift;
	private boolean shiftHeld;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Generator.fullReset();
		xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll.initLabels();
		xyz.gabriwar.warpedpixeldungeon.items.potions.Potion.initColors();
		xyz.gabriwar.warpedpixeldungeon.items.rings.Ring.initGems();
		//picking up and identifying reach the badges
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		gold = Dungeon.gold;
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
		Dungeon.gold = gold;
		DayNightCycle.debugPhaseOverride = override;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	private static ArrayList<int[]> landCache;

	//hero sectors on a grid round the origin where the 3x3 about them is mostly land
	static ArrayList<int[]> landLocations(){
		if (landCache != null) return landCache;
		ArrayList<int[]> out = new ArrayList<>();
		for (int sy = -20; sy <= 20; sy += 5){
			for (int sx = -20; sx <= 20; sx += 5){
				if (sx == 0 && sy == 0) continue;
				int land = 0;
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						WorldModel.Biome b = WorldModel.baseBiomeAt( SEED, (sx + dx) * WorldStructures.SECTOR + 48,
								(sy + dy) * WorldStructures.SECTOR + 48 );
						if (b != WorldModel.Biome.OCEAN && b != WorldModel.Biome.RIVER && b != WorldModel.Biome.MOUNTAIN) land++;
					}
				}
				if (land >= 7) out.add( new int[]{ sx, sy } );
			}
		}
		if (out.size() < 6) throw new AssertionError( "only " + out.size() + " land locations for the test seed" );
		landCache = out;
		return out;
	}

	private static ArrayList<WorldEvents.Event> starCache;
	private static int starLocations;

	//every star that fell in the 3x3 about each land location over a thousand days
	static ArrayList<WorldEvents.Event> stars(){
		if (starCache != null) return starCache;
		ArrayList<WorldEvents.Event> out = new ArrayList<>();
		HashSet<Long> seen = new HashSet<>();
		for (int[] l : landLocations()){
			for (int sy = l[1] - 1; sy <= l[1] + 1; sy++){
				for (int sx = l[0] - 1; sx <= l[0] + 1; sx++){
					for (int d = 0; d < 1000; d++){
						WorldEvents.Event e = WorldEvents.fallenStar( SEED, sx, sy, d );
						if (e != null && seen.add( e.id )) out.add( e );
					}
				}
			}
		}
		starLocations = landLocations().size();
		starCache = out;
		return out;
	}

	private static String describe( WorldEvents.Event e ){
		return e.id + ":" + e.type + ":" + e.sx + "," + e.sy + ":" + e.startDay + ":" + e.startTurn + "-" + e.endTurn
				+ "@" + e.wx + "," + e.wy;
	}

	private static WorldEvents.Event star( int sx, int sy, int startTurn, long id ){
		return new WorldEvents.Event( WorldEvents.Type.FALLEN_STAR, sx, sy, WorldEvents.day( startTurn ), startTurn,
				startTurn + 3 * FC, sx * WorldStructures.SECTOR + 40, sy * WorldStructures.SECTOR + 40, id );
	}

	private static WorldEvents.Event market( int sx, int sy, int day, long id ){
		return new WorldEvents.Event( WorldEvents.Type.MARKET, sx, sy, day, day * FC, (day + 3) * FC,
				sx * WorldStructures.SECTOR + 50, sy * WorldStructures.SECTOR + 50, id );
	}

	// ------------------------------------------------------------------ placement

	@Test
	public void eventsAreAPureFunctionOfTheirInputs(){
		for (int[] l : landLocations().subList( 0, 2 )){
			ArrayList<String> first = new ArrayList<>(), second = new ArrayList<>();
			for (int d = 0; d <= 40; d++){
				for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, l[0], l[1], 3, d, NONE )) first.add( d + "/" + describe( e ) );
			}
			ArrayList<ArrayList<String>> byDay = new ArrayList<>();
			for (int d = 40; d >= 0; d--){
				ArrayList<String> day = new ArrayList<>();
				for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, l[0], l[1], 3, d, NONE )) day.add( d + "/" + describe( e ) );
				byDay.add( 0, day );
			}
			for (ArrayList<String> day : byDay) second.addAll( day );
			assertEquals( first, second );
			assertFalse( "nothing at all in 41 days round " + Arrays.toString( l ), first.isEmpty() );
		}
		HashSet<Long> a = new HashSet<>(), b = new HashSet<>();
		int[] l = landLocations().get( 0 );
		for (int d = 0; d < 60; d++){
			for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, l[0], l[1], 3, d, NONE )) a.add( e.id );
			for (WorldEvents.Event e : WorldEvents.eventsNear( SEED2, l[0], l[1], 3, d, NONE )) b.add( e.id );
		}
		assertNotEquals( a, b );
	}

	@Test
	public void idsAreStableAndDistinct(){
		HashSet<Long> ids = new HashSet<>();
		int n = 0;
		for (WorldEvents.Type t : WorldEvents.Type.values()){
			for (int sy = -10; sy <= 10; sy++){
				for (int sx = -10; sx <= 10; sx++){
					for (int d = 0; d < 60; d++){
						long id = WorldEvents.idOf( t, sx, sy, d );
						assertEquals( id, WorldEvents.idOf( t, sx, sy, d ) );
						ids.add( id );
						n++;
					}
				}
			}
		}
		assertEquals( n, ids.size() );
		WorldEvents.Event fs = WorldEvents.forcedStar( 7 * 96 + 50, 7 * 96 + 50, 30 * FC + 2000 );
		assertNotEquals( WorldEvents.idOf( WorldEvents.Type.FALLEN_STAR, fs.sx, fs.sy, fs.startDay ), fs.id );
		WorldEvents.Event fm = WorldEvents.forcedMarket( SEED, 3, 4, 30 * FC + 300 );
		assertNotEquals( WorldEvents.idOf( WorldEvents.Type.MARKET, 3, 4, 30 ), fm.id );
	}

	@Test
	public void starsFallAboutEveryFourToSixDays(){
		int count = stars().size();
		assertTrue( count > 0 );
		double interval = 1000.0 * starLocations / count;
		System.out.println( "[events] " + count + " stars over 1000 days round " + starLocations + " places: one every " + interval + " days" );
		assertTrue( "a star every " + interval + " days in a hero's 3x3 sectors", interval >= 3 && interval <= 8 );
	}

	@Test
	public void starsFallAtNight(){
		for (WorldEvents.Event e : stars()){
			int t = Math.floorMod( e.startTurn, FC );
			assertTrue( describe( e ), t >= 2200 && t < 2500 );
			assertTrue( describe( e ), t >= WorldEvents.STAR_FALL_FROM && t < WorldEvents.STAR_FALL_FROM + WorldEvents.STAR_FALL_SPAN );
			assertEquals( e.startDay, WorldEvents.day( e.startTurn ) );
			assertEquals( e.startTurn + 3 * FC, e.endTurn );
			assertEquals( e.startTurn + WorldEvents.STAR_SCAR_DAYS * FC, e.lingersUntil() );
		}
	}

	@Test
	public void starsKeepOffWaterTownAndSettlements(){
		for (WorldEvents.Event e : stars()){
			assertTrue( describe( e ), WorldEvents.starGround( SEED, e.wx, e.wy ) );
			WorldModel.Biome b = WorldModel.baseBiomeAt( SEED, e.wx, e.wy );
			assertTrue( describe( e ), b != WorldModel.Biome.OCEAN && b != WorldModel.Biome.RIVER && b != WorldModel.Biome.MOUNTAIN );
			assertTrue( describe( e ), Math.max( Math.abs( e.wx ), Math.abs( e.wy ) ) > 64 );
			for (int sy = e.sy - 1; sy <= e.sy + 1; sy++){
				for (int sx = e.sx - 1; sx <= e.sx + 1; sx++){
					if (WorldStructures.siteType( SEED, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
					int r = WorldStructures.settlementLayout( SEED, sx, sy )[0] + 6;
					assertTrue( describe( e ), Math.abs( e.wx - WorldStructures.siteX( SEED, sx, sy ) ) > r
							|| Math.abs( e.wy - WorldStructures.siteY( SEED, sx, sy ) ) > r );
				}
			}
			int[] cells = WorldEvents.craterCells( SEED, e );
			assertTrue( cells.length >= 2 && cells.length % 2 == 0 );
			for (int i = 0; i < cells.length; i += 2){
				int t = WorldEvents.baseTerrain( SEED, cells[i], cells[i + 1] );
				assertTrue( describe( e ) + " " + t, WorldEvents.scorchable( t ) );
				assertTrue( t != Terrain.WATER && t != Terrain.DEEP_WATER && t != Terrain.FROZEN_WATER
						&& t != Terrain.DIRT_PATH && t != Terrain.BRIDGE );
				assertFalse( WindowGenerator.mouthAt( SEED, cells[i], cells[i + 1] ) );
				assertTrue( Math.max( Math.abs( cells[i] - e.wx ), Math.abs( cells[i + 1] - e.wy ) ) <= 2 );
			}
		}
	}

	@Test
	public void marketsComeToHumanVillagesForThreeDays(){
		int found = 0;
		for (int ry = -6; ry <= 6; ry++){
			for (int rx = -6; rx <= 6; rx++){
				for (int p = 0; p <= 30; p++){
					WorldEvents.Event e = WorldEvents.market( SEED, rx, ry, p );
					if (e == null) continue;
					found++;
					assertEquals( WorldStructures.Site.VILLAGE, WorldStructures.siteType( SEED, e.sx, e.sy ) );
					assertEquals( WorldStructures.Faction.HUMAN, WorldStructures.faction( SEED, e.sx, e.sy ) );
					assertEquals( rx, Math.floorDiv( e.sx, WorldEvents.MARKET_REGION ) );
					assertEquals( ry, Math.floorDiv( e.sy, WorldEvents.MARKET_REGION ) );
					assertEquals( 3 * FC, e.endTurn - e.startTurn );
					//it comes on a morning and rolls on the morning three days on, still in its period
					assertEquals( e.startDay * FC + WorldEvents.MARKET_OPEN, e.startTurn );
					assertTrue( e.startDay >= p * WorldEvents.MARKET_PERIOD );
					assertTrue( e.endTurn <= (p + 1) * WorldEvents.MARKET_PERIOD * FC );
					assertEquals( WorldStructures.siteX( SEED, e.sx, e.sy ), e.wx );
					assertEquals( WorldStructures.siteY( SEED, e.sx, e.sy ), e.wy );
				}
			}
		}
		assertTrue( found > 0 );
	}

	@Test
	public void marketsAreSeenEveryWeekOrTwo(){
		long total = 0;
		int locations = 0;
		for (int[] l : landLocations()){
			HashSet<Long> ids = new HashSet<>();
			//the markets eventsNear reports within three sectors, over a thousand days
			for (int p = 0; p * WorldEvents.MARKET_PERIOD < 1000; p++){
				for (int ry = Math.floorDiv( l[1] - 3, 3 ); ry <= Math.floorDiv( l[1] + 3, 3 ); ry++){
					for (int rx = Math.floorDiv( l[0] - 3, 3 ); rx <= Math.floorDiv( l[0] + 3, 3 ); rx++){
						WorldEvents.Event e = WorldEvents.market( SEED, rx, ry, p );
						if (e != null && e.startDay < 1000 && WorldEvents.within( e, l[0], l[1], 3 )) ids.add( e.id );
					}
				}
			}
			total += ids.size();
			locations++;
		}
		assertTrue( total > 0 );
		double interval = 1000.0 * locations / total;
		System.out.println( "[events] " + total + " markets within three sectors over 1000 days round " + locations + " places: one every " + interval + " days" );
		assertTrue( "a new market every " + interval + " days within three sectors", interval >= 4 && interval <= 25 );
		//and the same markets come through eventsNear on their days
		int[] l = landLocations().get( 0 );
		for (int d = 0; d < 84; d++){
			for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, l[0], l[1], 3, d, NONE )){
				if (e.type != WorldEvents.Type.MARKET) continue;
				assertTrue( e.overlapsDay( d ) );
				assertTrue( WorldEvents.within( e, l[0], l[1], 3 ) );
			}
		}
	}

	@Test
	public void forcedEventsGoThroughTheSameEnumeration(){
		int fall = 40 * FC + 1000;
		int wx = 7 * WorldStructures.SECTOR + 48 + 15, wy = 7 * WorldStructures.SECTOR + 48;
		WorldEvents.Event s = WorldEvents.forcedStar( wx, wy, fall );
		WorldEvents.Event m = WorldEvents.forcedMarket( SEED, 8, 7, fall );
		ArrayList<WorldEvents.Event> forced = new ArrayList<>( Arrays.asList( s, m ) );
		ArrayList<Long> ids = new ArrayList<>();
		for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, 7, 7, 1, 40, forced )) ids.add( e.id );
		assertTrue( ids.contains( s.id ) );
		assertTrue( ids.contains( m.id ) );
		for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, 12, 7, 1, 40, forced )){
			assertNotEquals( s.id, e.id );
			assertNotEquals( m.id, e.id );
		}
		//the star's crater stays its scar's nine days, the market only its three (to the morning)
		for (int d = 41; d <= 49; d++){
			boolean star = false, market = false;
			for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, 7, 7, 1, d, forced )){
				star |= e.id == s.id;
				market |= e.id == m.id;
			}
			assertTrue( "day " + d, star );
			assertEquals( "day " + d, d <= 43, market );
		}
		for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, 7, 7, 1, 50, forced )) assertNotEquals( s.id, e.id );
		assertFalse( s.activeAt( 43 * FC + 1000 ) );
		assertTrue( s.scarredAt( 43 * FC + 1000 ) );
		assertTrue( m.activeAt( 43 * FC + WorldEvents.MARKET_OPEN - 1 ) );
		assertFalse( m.activeAt( 43 * FC + WorldEvents.MARKET_OPEN ) );
		//a forced event already there naturally is listed once
		WorldEvents.Event natural = null;
		int[] l = landLocations().get( 0 );
		for (int d = 0; d < 200 && natural == null; d++){
			for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, l[0], l[1], 2, d, NONE )) natural = e;
		}
		assertNotNull( natural );
		int count = 0;
		for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, l[0], l[1], 2, natural.startDay,
				Collections.singletonList( natural ) )) if (e.id == natural.id) count++;
		assertEquals( 1, count );
	}

	@Test
	public void announcementFiresOncePerEvent(){
		int turn = 30 * FC + 2100;
		WorldEvents.Event s = star( 0, 0, turn - 10, 11 );
		HashMap<Long, Integer> heard = new HashMap<>(), done = new HashMap<>();
		List<WorldEvents.Event> near = Collections.singletonList( s );
		//the window far off: only the sector distance counts
		assertEquals( near, WorldEvents.toAnnounce( near, heard, done, 0, 0, turn, 5000, 5000, 176, 176 ) );
		heard.put( s.id, s.endTurn );
		assertTrue( WorldEvents.toAnnounce( near, heard, done, 0, 0, turn, 5000, 5000, 176, 176 ).isEmpty() );
		heard.clear();
		//a star two sectors off is not heard of - unless its crater is in the window
		assertTrue( WorldEvents.toAnnounce( near, heard, done, 2, 0, turn, 5000, 5000, 176, 176 ).isEmpty() );
		assertEquals( 1, WorldEvents.toAnnounce( near, heard, done, 2, 0, turn, s.wx - 100, s.wy - 50, 176, 176 ).size() );
		//a market three off is
		WorldEvents.Event m = market( 3, 0, 30, 12 );
		assertEquals( 1, WorldEvents.toAnnounce( Collections.singletonList( m ), heard, done, 0, 0, turn, 5000, 5000, 176, 176 ).size() );
		assertTrue( WorldEvents.toAnnounce( Collections.singletonList( m ), heard, done, 0, 4, turn, 5000, 5000, 176, 176 ).isEmpty() );
		//a settled one, or one not yet begun, is not
		done.put( s.id, s.endTurn );
		assertTrue( WorldEvents.toAnnounce( near, heard, done, 0, 0, turn, 5000, 5000, 176, 176 ).isEmpty() );
		done.clear();
		assertTrue( WorldEvents.toAnnounce( near, heard, done, 0, 0, s.startTurn - 1, 5000, 5000, 176, 176 ).isEmpty() );
		assertTrue( WorldEvents.toAnnounce( near, heard, done, 0, 0, s.endTurn, 5000, 5000, 176, 176 ).isEmpty() );
	}

	@Test
	public void mapPinsShowLiveHeardUnresolvedEventsInTheChart(){
		int turn = 30 * FC + 2200;
		WorldEvents.Event heard = star( 1, 1, turn - 100, 1 );
		WorldEvents.Event unheard = star( 2, 1, turn - 100, 2 );
		WorldEvents.Event settled = star( 1, 2, turn - 100, 3 );
		WorldEvents.Event over = star( 2, 2, turn - 4 * FC, 4 );
		WorldEvents.Event outside = star( 30, 1, turn - 100, 5 );
		WorldEvents.Event fair = market( 3, 3, 30, 6 );
		List<WorldEvents.Event> all = Arrays.asList( heard, unheard, settled, over, outside, fair );
		HashMap<Long, Integer> announced = new HashMap<>(), resolved = new HashMap<>();
		for (WorldEvents.Event e : all) if (e != unheard) announced.put( e.id, e.endTurn );
		resolved.put( settled.id, settled.endTurn );
		assertEquals( Arrays.asList( heard, fair ), WorldEvents.mapMarkers( all, announced, resolved, turn, -100, -100, 1024 ) );
		assertTrue( WorldEvents.mapMarkers( NONE, announced, resolved, turn, -100, -100, 1024 ).isEmpty() );
	}

	@Test
	public void directionWords(){
		assertEquals( "n", WorldEvents.direction( 0, -50 ) );
		assertEquals( "ne", WorldEvents.direction( 50, -50 ) );
		assertEquals( "e", WorldEvents.direction( 50, 0 ) );
		assertEquals( "s", WorldEvents.direction( 0, 50 ) );
		assertEquals( "w", WorldEvents.direction( -50, 0 ) );
		assertEquals( "sw", WorldEvents.direction( -40, 40 ) );
		assertEquals( "nw", WorldEvents.direction( -40, -40 ) );
		assertEquals( "se", WorldEvents.direction( 40, 40 ) );
		assertEquals( "here", WorldEvents.direction( 10, -12 ) );
		assertEquals( "here", WorldEvents.direction( 16, 16 ) );
		assertEquals( "e", WorldEvents.direction( 17, 0 ) );
	}

	@Test
	public void starBonusIsFixedPerStar(){
		int bonus = 0, artifacts = 0;
		for (long id = 1; id <= 10000; id++){
			long h = WorldEvents.mix( id, 77, 3 );
			int k = WorldEvents.starBonus( h );
			assertEquals( k, WorldEvents.starBonus( h ) );
			assertTrue( k >= -1 && k <= 2 );
			if (k >= 0) bonus++;
			if (k == 2) artifacts++;
		}
		assertTrue( "bonus " + bonus, bonus >= 3700 && bonus <= 4300 );
		assertTrue( "artifacts " + artifacts, artifacts >= 800 && artifacts <= 1200 );
	}

	// ------------------------------------------------------------------ the log

	@Test
	public void logSurvivesABundleRoundTrip() throws Exception {
		WorldEventLog log = new WorldEventLog();
		log.announced.put( 101L, 5000 );
		log.announced.put( -202L, 7500 );
		log.resolved.put( 303L, 9000 );
		log.lootAt.put( 404L, OverworldLevel.worldKey( -1234, 5678 ) );
		log.lootEnds.put( 404L, 12345 );
		WorldEvents.Event s = WorldEvents.forcedStar( 700, -300, 50 * FC + 1960 );
		WorldEvents.Event m = WorldEvents.forcedMarket( SEED, 5, 6, 50 * FC + 300 );
		log.force( s );
		log.force( m );

		Bundle b = new Bundle();
		log.storeInBundle( b );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		Bundle read = Bundle.read( new ByteArrayInputStream( out.toByteArray() ) );
		WorldEventLog back = new WorldEventLog();
		back.restoreFromBundle( read );

		assertEquals( log.announced, back.announced );
		assertEquals( log.resolved, back.resolved );
		assertEquals( log.lootAt, back.lootAt );
		assertEquals( log.lootEnds, back.lootEnds );
		assertEquals( 2, back.forced.size() );
		for (int i = 0; i < 2; i++){
			assertEquals( describe( log.forced.get( i ) ), describe( back.forced.get( i ) ) );
		}
	}

	@Test
	public void oldSavesWithoutEventKeysLoad(){
		WorldEventLog log = new WorldEventLog();
		log.announced.put( 1L, 1 );
		log.restoreFromBundle( new Bundle() );
		assertTrue( log.announced.isEmpty() );
		assertTrue( log.resolved.isEmpty() );
		assertTrue( log.lootAt.isEmpty() );
		assertTrue( log.lootEnds.isEmpty() );
		assertTrue( log.forced.isEmpty() );
		//a group half there is left out whole
		Bundle half = new Bundle();
		half.put( "event_announced_ids", new long[]{ 5L, 6L } );
		half.put( "event_forced_ids", new long[]{ 9L } );
		half.put( "event_forced_types", new String[]{ "FALLEN_STAR" } );
		log.restoreFromBundle( half );
		assertTrue( log.announced.isEmpty() );
		assertTrue( log.forced.isEmpty() );
		//a kind this build does not know is dropped, the rest kept
		Bundle odd = new Bundle();
		odd.put( "event_forced_ids", new long[]{ 9L, 10L } );
		odd.put( "event_forced_types", new String[]{ "COMET", "MARKET" } );
		odd.put( "event_forced_data", new int[]{ 1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7 } );
		log.restoreFromBundle( odd );
		assertEquals( 1, log.forced.size() );
		assertEquals( WorldEvents.Type.MARKET, log.forced.get( 0 ).type );
		assertEquals( 10L, log.forced.get( 0 ).id );
	}

	@Test
	public void pruneForgetsFinishedEvents(){
		WorldEventLog log = new WorldEventLog();
		log.announced.put( 1L, 100 );
		log.announced.put( 2L, 300 );
		log.resolved.put( 3L, 200 );
		log.resolved.put( 4L, 201 );
		WorldEvents.Event m = WorldEvents.forcedMarket( SEED, 1, 1, 10 );
		WorldEvents.Event s = WorldEvents.forcedStar( 500, 500, 10 );
		log.force( m );
		log.force( s );
		int v = log.forcedVersion();
		log.prune( 200 );
		assertFalse( log.announced.containsKey( 1L ) );
		assertTrue( log.announced.containsKey( 2L ) );
		assertFalse( log.resolved.containsKey( 3L ) );
		assertTrue( log.resolved.containsKey( 4L ) );
		assertEquals( v, log.forcedVersion() );
		log.prune( m.endTurn );
		assertEquals( Collections.singletonList( s ), new ArrayList<>( log.forced ) );
		assertNotEquals( v, log.forcedVersion() );
		//the star's crater outlives its loot: it is forgotten only when its scar is gone
		log.prune( s.endTurn );
		assertEquals( 1, log.forced.size() );
		log.prune( s.lingersUntil() );
		assertTrue( log.forced.isEmpty() );
	}

	// ------------------------------------------------------------------ a real window

	private static Object call( Object o, String name, Class<?>[] types, Object... args ){
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( o, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	private static Object field( Object o, String name ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			return f.get( o );
		} catch (Exception e){
			throw new AssertionError( e );
		}
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

	private static WorldEventLog log( OverworldLevel ow ){
		return (WorldEventLog) field( ow, "eventLog" );
	}

	@SuppressWarnings("unchecked")
	private static HashMap<Long, Integer> diffs( OverworldLevel ow ){
		return (HashMap<Long, Integer>) field( ow, "diffs" );
	}

	//the live surface window round a world cell at a seasonal shift: built as a mirror's is (no
	//populator), then made the host's own, the hero on the nearest open cell
	private OverworldLevel windowAt( int wx, int wy, float shift ){
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, shift );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, shift, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		setField( ow, "network", false );
		Dungeon.level = ow;
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

	//puts the hero on an open cell between minD and maxD cells (Chebyshev) from a world cell
	private void standNear( OverworldLevel ow, int wx, int wy, int minD, int maxD ){
		int target = ow.localCell( wx, wy );
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

	//an open cell near the hero with open ground all round it, for a stall
	private int openCell( OverworldLevel ow ){
		for (int r = 3; r < 30; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					int c = hero.pos + dx + dy * W;
					boolean open = ow.passable[c] && !ow.occupied( c ) && ow.heaps.get( c ) == null;
					for (int n : PathFinder.NEIGHBOURS8) open &= ow.passable[c + n] && !ow.water[c + n] && ow.heaps.get( c + n ) == null;
					if (open) return c;
				}
			}
		}
		throw new AssertionError( "no open ground near the hero" );
	}

	//the lines among these about a star
	private static int starLines( ArrayList<String> lines, String key, Object... args ){
		String text = Messages.get( WorldEvents.class, key, args );
		int n = 0;
		for (String l : lines) if (l.contains( text )) n++;
		return n;
	}

	//the heaps of a window holding a star's own fragment
	private static ArrayList<Heap> fragments( OverworldLevel ow, long id ){
		ArrayList<Heap> out = new ArrayList<>();
		for (Heap h : ow.heaps.valueList()){
			for (Item i : h.items){
				if (i instanceof StarFragment && ((StarFragment) i).eventId == id) out.add( h );
			}
		}
		return out;
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

	//a star of the test seed, the clock just after its fall: the first one on a biome given
	//(any when null)
	private static WorldEvents.Event starOn( WorldModel.Biome... biomes ){
		for (WorldEvents.Event e : stars()){
			if (e.startDay < 2) continue;
			if (biomes.length == 0) return e;
			WorldModel.Biome b = WorldModel.baseBiomeAt( SEED, e.wx, e.wy );
			for (WorldModel.Biome want : biomes) if (b == want) return e;
		}
		return null;
	}

	@Test
	public void aStarComesDownOnItsCraterAndTheLandTakesItBack(){
		WorldEvents.Event e = starOn();
		Dungeon.cycleTurn = e.startTurn + 5;
		OverworldLevel ow = windowAt( e.wx - 15, e.wy, WorldModel.calendarShift() );
		standNear( ow, e.wx, e.wy, 10, 16 );
		int[] before = ow.map.clone();
		int[] pristine = (int[]) field( ow, "pristine" );
		byte[] link = (byte[]) field( ow, "link" );

		ArrayList<String> lines = logged( ow::pollEvents );
		//heard of once, close by, as it falls
		assertEquals( lines.toString(), 1, starLines( lines, "star_falls_near" ) );
		assertTrue( log( ow ).announced.containsKey( e.id ) );

		//the scorch lies on the ground the window really has, and nowhere else
		int[] cells = WorldEvents.craterCells( SEED, e );
		ArrayList<Integer> scorched = new ArrayList<>();
		for (int i = 0; i < cells.length; i += 2){
			int c = ow.localCell( cells[i], cells[i + 1] );
			boolean takes = WorldEvents.scorchable( pristine[c] ) && link[c] == WindowGenerator.LINK_NONE && before[c] == pristine[c];
			assertEquals( takes ? Terrain.EMBERS : before[c], ow.map[c] );
			if (takes) scorched.add( c );
		}
		assertFalse( scorched.isEmpty() );

		//the fragment, tagged with its star, once, on the crater's open ground
		ArrayList<Heap> found = fragments( ow, e.id );
		assertEquals( 1, found.size() );
		Heap heap = found.get( 0 );
		assertTrue( ow.distance( heap.pos, ow.localCell( e.wx, e.wy ) ) <= 2 );
		assertFalse( ow.water[heap.pos] );
		assertEquals( 1, heap.items.size() );
		assertEquals( e.endTurn, ((StarFragment) heap.peek()).eventEnd );

		//the event's own work, never the player's edit
		call( ow, "captureDiffs", new Class<?>[0] );
		for (int c : scorched){
			assertFalse( diffs( ow ).containsKey( OverworldLevel.worldKey( ow.worldX + c % W, ow.worldY + c / W ) ) );
		}

		//another step: nothing heard again, nothing laid twice
		assertEquals( 0, starLines( logged( ow::pollEvents ), "star_falls_near" ) );
		assertEquals( 1, fragments( ow, e.id ).size() );

		//on the map while it is on
		boolean pinned = false;
		for (WorldEvents.Event m : ow.mapEvents( ow.worldX - 400, ow.worldY - 400, 1024 )) pinned |= m.id == e.id;
		assertTrue( pinned );

		//three days on, nobody came: the fragment is taken back, the scorch stays
		Dungeon.cycleTurn = e.endTurn;
		ow.pollEvents();
		assertTrue( fragments( ow, e.id ).isEmpty() );
		assertNull( ow.heaps.get( heap.pos ) );
		for (int c : scorched) assertEquals( Terrain.EMBERS, ow.map[c] );
		for (WorldEvents.Event m : ow.mapEvents( ow.worldX - 400, ow.worldY - 400, 1024 )) assertNotEquals( e.id, m.id );
		assertFalse( log( ow ).lootAt.containsKey( e.id ) );

		//its nine: the land has its own ground back, and nothing of it is stored
		Dungeon.cycleTurn = e.lingersUntil();
		ow.pollEvents();
		for (int c : scorched) assertEquals( pristine[c], ow.map[c] );
		call( ow, "captureDiffs", new Class<?>[0] );
		for (int c : scorched){
			assertFalse( diffs( ow ).containsKey( OverworldLevel.worldKey( ow.worldX + c % W, ow.worldY + c / W ) ) );
		}
	}

	@Test
	public void pickingUpTheFragmentSettlesTheStarForGood(){
		//a star whose fixed bonus is something, so the gift's way is seen too
		WorldEvents.Event e = null;
		for (WorldEvents.Event s : stars()){
			if (s.startDay >= 2 && WorldEvents.starBonus( s.id ) >= 0){
				e = s;
				break;
			}
		}
		assertNotNull( e );
		Dungeon.cycleTurn = e.startTurn + 100;
		OverworldLevel ow = windowAt( e.wx - 15, e.wy, WorldModel.calendarShift() );
		standNear( ow, e.wx, e.wy, 10, 16 );
		ArrayList<String> lines = logged( ow::pollEvents );
		//heard of after the fall: only its glow is left to tell of it
		assertEquals( 1, starLines( lines, "star_glow_near" ) );
		assertEquals( 0, starLines( lines, "star_falls_near" ) );

		Heap heap = fragments( ow, e.id ).get( 0 );
		hero.pos = heap.pos;
		StarFragment f = (StarFragment) heap.peek();
		assertTrue( f.doPickUp( hero, heap.pos ) );
		heap.pickUp();
		assertEquals( 0, f.eventId );
		assertTrue( log( ow ).resolved.containsKey( e.id ) );
		assertFalse( log( ow ).lootAt.containsKey( e.id ) );
		assertSame( f, hero.belongings.getItem( StarFragment.class ) );
		for (WorldEvents.Event m : ow.mapEvents( ow.worldX - 400, ow.worldY - 400, 1024 )) assertNotEquals( e.id, m.id );

		//the gift turns up where the fragment lay, on the next actor turn
		Actor gift = null;
		for (Actor a : Actor.all()) if (!(a instanceof Char)) gift = a;
		assertNotNull( gift );
		try {
			Method act = Actor.class.getDeclaredMethod( "act" );
			act.setAccessible( true );
			assertTrue( (Boolean) act.invoke( gift ) );
		} catch (Exception ex){
			throw new AssertionError( ex );
		}
		assertFalse( Actor.all().contains( gift ) );
		Heap left = ow.heaps.get( heap.pos );
		assertNotNull( left );
		Item it = left.peek();
		int kind = WorldEvents.starBonus( e.id );
		assertTrue( kind == 0 ? it instanceof Ring : kind == 1 ? it instanceof Wand : it instanceof Artifact || it instanceof Ring );
		assertFalse( it.cursed );

		//settled: not laid, heard of or pinned again, however often the hero steps
		ow.pollEvents();
		assertTrue( fragments( ow, e.id ).isEmpty() );
		//a plain fragment and a crater's own never stack into one
		StarFragment plain = new StarFragment(), tagged = new StarFragment();
		tagged.eventId = 77;
		assertTrue( plain.isSimilar( new StarFragment() ) );
		assertFalse( plain.isSimilar( tagged ) );
	}

	//the scorch at three points of the year, the climate's own extremes among them: wherever a
	//star falls on swamp, tundra or snowfield, its crater keeps off the water, the ice, the
	//trees and the ways between slices the window really has, and its fragment never lies wet
	@Test
	public void theScorchKeepsOffWaterAndWaysAtEverySeason(){
		ArrayList<WorldEvents.Event> some = new ArrayList<>();
		for (WorldModel.Biome b : new WorldModel.Biome[]{ WorldModel.Biome.SWAMP, WorldModel.Biome.TUNDRA,
				WorldModel.Biome.SNOWFIELD, WorldModel.Biome.BEACH, WorldModel.Biome.FOREST, WorldModel.Biome.MEADOW }){
			WorldEvents.Event e = starOn( b );
			if (e != null) some.add( e );
		}
		assertTrue( some.size() >= 4 );
		float[] shifts = { WorldModel.WINTER_SHIFT - WorldModel.CLIMATE_SHIFT, 0f, WorldModel.SUMMER_SHIFT + WorldModel.CLIMATE_SHIFT };
		for (WorldEvents.Event e : some){
			for (float s : shifts){
				Actor.clear();
				Dungeon.cycleTurn = e.startTurn + 5;
				OverworldLevel ow = windowAt( e.wx - 15, e.wy, s );
				int[] pristine = (int[]) field( ow, "pristine" );
				byte[] link = (byte[]) field( ow, "link" );
				int[] before = ow.map.clone();
				ow.pollEvents();
				for (int c = 0; c < ow.map.length; c++){
					if (ow.map[c] == before[c]) continue;
					assertEquals( Terrain.EMBERS, ow.map[c] );
					int t = pristine[c];
					String where = e.id + " at shift " + s + ": " + t;
					assertTrue( where, t != Terrain.WATER && t != Terrain.DEEP_WATER && t != Terrain.FROZEN_WATER );
					assertTrue( where, t != Terrain.ENTRANCE && t != Terrain.EXIT );
					assertTrue( where, t != Terrain.TREE_PINE && t != Terrain.TREE_OAK );
					assertEquals( where, WindowGenerator.LINK_NONE, link[c] );
				}
				for (Heap h : fragments( ow, e.id )){
					assertFalse( ow.water[h.pos] );
					assertTrue( WorldEvents.scorchable( pristine[h.pos] ) );
				}
			}
		}
	}

	//a human village of a few houses near the origin, with a market due there on some day
	private static WorldEvents.Event marketDue(){
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

	//every trader of a window's market, shelves stocked as on their first turn
	private ArrayList<TravellingMerchant> openedMarket( OverworldLevel ow, WorldEvents.Event e ){
		ow.pollEvents();
		ArrayList<TravellingMerchant> t = traders( ow );
		for (TravellingMerchant m : t) m.restock();
		return t;
	}

	@Test
	public void aMarketPitchesItsStallsOnceAndStrikesOnlyThem(){
		WorldEvents.Event e = marketDue();
		Dungeon.cycleTurn = e.startTurn + 400;
		OverworldLevel ow = windowAt( e.wx, e.wy + 3, WorldModel.calendarShift() );
		ArrayList<String> lines = logged( () -> openedMarket( ow, e ) );
		assertEquals( lines.toString(), 1, starLines( lines, "market_here", WorldStructures.villageName( SEED, e.sx, e.sy ) ) );

		ArrayList<TravellingMerchant> t = traders( ow );
		assertTrue( t.size() == 2 || t.size() == 3 );
		assertTrue( ow.marketActiveAt( WorldStructures.sectorOf( e.sx, e.sy ) ) );
		int[] layout = WorldStructures.settlementLayout( SEED, e.sx, e.sy );
		HashSet<Integer> kinds = new HashSet<>();
		for (TravellingMerchant m : t){
			assertEquals( e.id, m.eventId );
			assertEquals( e.endTurn, m.endTurn );
			assertTrue( kinds.add( m.speciality ) );
			int mx = ow.worldX + m.pos % W - e.wx, my = ow.worldY + m.pos / W - e.wy;
			int ring = Math.max( Math.abs( mx ), Math.abs( my ) );
			assertTrue( ring >= 4 && ring <= 12 );
			for (int i = 1; i + 1 < layout.length; i += 2){
				assertTrue( Math.max( Math.abs( mx - layout[i] ), Math.abs( my - layout[i + 1] ) ) >= 5 );
			}
			for (TravellingMerchant o : t) assertTrue( o == m || ow.distance( o.pos, m.pos ) >= 4 );
			int t0 = ow.map[m.pos];
			assertTrue( t0 != Terrain.DOOR && t0 != Terrain.OPEN_DOOR && t0 != Terrain.SIGN && t0 != Terrain.BRIDGE );
		}
		int shelves = forSale( ow );
		assertTrue( shelves >= 2 * t.size() );

		//a village vendor's shelf, which no strike of theirs may touch
		int house = ow.localCell( e.wx + layout[1], e.wy + layout[2] );
		Heap theirs = ow.drop( new Food(), house );
		theirs.type = Heap.Type.FOR_SALE;

		//more steps, the stalls stay as they are
		ow.pollEvents();
		assertEquals( t.size(), traders( ow ).size() );

		//a far trip and back: parked with their goods, put back with them, nothing doubled
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, e.wx + 700, e.wy );
		assertTrue( traders( ow ).isEmpty() );
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, e.wx, e.wy + 3 );
		hero.pos = ow.localCell( e.wx, e.wy + 3 );
		ow.pollEvents();
		assertEquals( t.size(), traders( ow ).size() );
		for (TravellingMerchant m : traders( ow )) assertTrue( t.contains( m ) );
		assertEquals( shelves + 1, forSale( ow ) );

		//the third morning on: the wagons roll on with their own goods and nothing else
		Dungeon.cycleTurn = e.endTurn;
		ow.pollEvents();
		assertTrue( traders( ow ).isEmpty() );
		assertEquals( 1, forSale( ow ) );
		assertNotNull( ow.heaps.get( house ) );
		assertFalse( ow.marketActiveAt( WorldStructures.sectorOf( e.sx, e.sy ) ) );
	}

	@Test
	public void aMarketThatEndsWhileParkedLeavesNothingBehind(){
		WorldEvents.Event e = marketDue();
		Dungeon.cycleTurn = e.startTurn + 400;
		OverworldLevel ow = windowAt( e.wx, e.wy + 3, WorldModel.calendarShift() );
		openedMarket( ow, e );
		int traders = traders( ow ).size();
		assertTrue( traders >= 2 );
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, e.wx + 700, e.wy );
		@SuppressWarnings("unchecked") HashMap<Long, Mob> parked = (HashMap<Long, Mob>) field( ow, "parkedMobs" );
		@SuppressWarnings("unchecked") HashMap<Long, Heap> stored = (HashMap<Long, Heap>) field( ow, "storedHeaps" );
		int sales = 0;
		for (Heap h : stored.values()) if (h.type == Heap.Type.FOR_SALE) sales++;
		assertTrue( sales >= traders );
		Dungeon.cycleTurn = e.endTurn + 10;
		call( ow, "expireEvents", new Class<?>[]{ int.class, boolean.class }, Dungeon.cycleTurn, false );
		for (Mob m : parked.values()) assertFalse( m instanceof TravellingMerchant );
		for (Heap h : stored.values()) assertNotEquals( Heap.Type.FOR_SALE, h.type );
	}

	@Test
	public void aTraderChasedOffEndsTheWholeMarket(){
		WorldEvents.Event e = marketDue();
		Dungeon.cycleTurn = e.startTurn + 400;
		OverworldLevel ow = windowAt( e.wx, e.wy + 3, WorldModel.calendarShift() );
		ArrayList<TravellingMerchant> t = openedMarket( ow, e );
		int[] layout = WorldStructures.settlementLayout( SEED, e.sx, e.sy );
		int house = ow.localCell( e.wx + layout[1], e.wy + layout[2] );
		ow.drop( new Food(), house ).type = Heap.Type.FOR_SALE;

		ArrayList<String> lines = logged( () -> t.get( 0 ).flee() );
		assertEquals( Collections.singletonList( GLog.NEGATIVE + Messages.get( TravellingMerchant.class, "flee" ) ), lines );
		assertTrue( traders( ow ).isEmpty() );
		assertEquals( 1, forSale( ow ) );
		assertTrue( log( ow ).resolved.containsKey( e.id ) );
		assertFalse( ow.marketActiveAt( WorldStructures.sectorOf( e.sx, e.sy ) ) );
		//it is not set up again, nor heard of
		assertEquals( 0, starLines( logged( ow::pollEvents ), "market_here", WorldStructures.villageName( SEED, e.sx, e.sy ) ) );
		assertTrue( traders( ow ).isEmpty() );

		//a destroy() of one goes the same way: never the level-wide sweep of a shopkeeper's
		Actor.clear();
		WorldEvents.Event f = WorldEvents.forcedMarket( SEED, e.sx, e.sy, Dungeon.cycleTurn );
		ow.forceEvent( f );
		ArrayList<TravellingMerchant> again = openedMarket( ow, f );
		assertFalse( again.isEmpty() );
		again.get( 0 ).destroy();
		assertTrue( traders( ow ).isEmpty() );
		assertEquals( 1, forSale( ow ) );
		assertTrue( log( ow ).resolved.containsKey( f.id ) );
	}

	@Test
	public void travellingTradersCarryRareGoodsAtAMarkUp(){
		Dungeon.cycleTurn = 10 * FC + 700;
		WorldEvents.Event e = marketDue();
		OverworldLevel ow = windowAt( e.wx, e.wy + 3, WorldModel.calendarShift() );
		for (int k = -2; k < 6; k++){
			TravellingMerchant m = TravellingMerchant.of( k, 5, 6 );
			assertTrue( m.speciality >= 0 && m.speciality < TravellingMerchant.SPECIALITIES );
			assertNotEquals( Messages.NO_TEXT_FOUND, m.name() );
			assertNotEquals( Messages.NO_TEXT_FOUND, m.chatTextFor( hero ) );
			assertFalse( m.name().contains( Messages.NO_TEXT_FOUND ) );
		}
		Method stock;
		try {
			stock = TravellingMerchant.class.getDeclaredMethod( "stockSource" );
			stock.setAccessible( true );
			for (int k = 0; k < TravellingMerchant.SPECIALITIES; k++){
				@SuppressWarnings("unchecked") ArrayList<Item> wares = (ArrayList<Item>) stock.invoke( TravellingMerchant.of( k, 5, 6 ) );
				assertEquals( 4, wares.size() );
				for (Item it : wares){
					assertFalse( it.cursed );
					if (k == TravellingMerchant.CURIOS) assertTrue( it instanceof Ring || it instanceof Wand );
					if (k == TravellingMerchant.RELICS) assertTrue( it instanceof Artifact || it instanceof Ring );
					if (k == TravellingMerchant.EXOTICS) assertTrue( it.getClass().getName(), it instanceof ExoticPotion || it instanceof ExoticScroll );
				}
			}
		} catch (Exception ex){
			throw new AssertionError( ex );
		}
		//a quarter dearer than the shelf price, the scroll of upgrade's own ladder aside
		TravellingMerchant m = TravellingMerchant.of( 0, 5, 6 );
		Item ring = Generator.random( Generator.Category.RING );
		ring.identify( false );
		int base = Shopkeeper.sellPrice( ring, hero );
		assertEquals( Shopkeeper.discounted( base, 1.25f ), Shopkeeper.sellPrice( ring, hero, m ) );
		assertTrue( Shopkeeper.sellPrice( ring, hero, m ) > base );
		//stocked once: the week's turn brings no second roll of rare goods
		m.pos = openCell( ow );
		ow.mobs.add( m );
		m.openShop();
		m.restock();
		int laid = forSale( ow );
		assertTrue( laid > 0 );
		m.restock();
		assertEquals( laid, forSale( ow ) );
	}

	@Test
	public void theStarSaveAndTheMarketSaveNeitherDoubleNorReturn(){
		//a star and a human village in the same window
		WorldEvents.Event star = null;
		int[] village = null;
		for (WorldEvents.Event e : stars()){
			if (e.startDay < 2) continue;
			ArrayList<int[]> v = WorldStructures.settlementsNear( SEED, e.wx, e.wy, WorldStructures.Faction.HUMAN, 3, 1 );
			if (!v.isEmpty() && Math.abs( v.get( 0 )[2] - e.wx ) < 50 && Math.abs( v.get( 0 )[3] - e.wy ) < 50){
				star = e;
				village = v.get( 0 );
				break;
			}
		}
		assertNotNull( star );
		Dungeon.cycleTurn = star.startTurn + 5;
		OverworldLevel ow = windowAt( (star.wx + village[2]) / 2, (star.wy + village[3]) / 2, WorldModel.calendarShift() );
		WorldEvents.Event fair = WorldEvents.forcedMarket( SEED, village[0], village[1], Dungeon.cycleTurn );
		ow.forceEvent( fair );
		openedMarket( ow, fair );
		int traders = traders( ow ).size();
		assertTrue( traders >= 2 );
		assertEquals( 1, fragments( ow, star.id ).size() );
		int sales = forSale( ow );
		int[] cells = WorldEvents.craterCells( SEED, star );
		ArrayList<Long> burnt = new ArrayList<>();
		for (int i = 0; i < cells.length; i += 2){
			int c = ow.localCell( cells[i], cells[i + 1] );
			if (ow.map[c] == Terrain.EMBERS) burnt.add( OverworldLevel.worldKey( cells[i], cells[i + 1] ) );
		}
		assertFalse( burnt.isEmpty() );

		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		//a headless Game carries no version code: stamp the save as a current one
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		for (long k : burnt) assertFalse( diffs( ow ).containsKey( k ) );

		Actor.clear();
		OverworldLevel back = new OverworldLevel();
		back.restoreFromBundle( b );
		Dungeon.level = back;
		assertEquals( log( ow ).announced, log( back ).announced );
		assertEquals( 1, log( back ).forced.size() );
		assertEquals( fair.id, log( back ).forced.get( 0 ).id );
		assertEquals( traders, traders( back ).size() );
		assertEquals( 1, fragments( back, star.id ).size() );
		assertEquals( sales, forSale( back ) );
		for (long k : burnt){
			assertEquals( Terrain.EMBERS, back.map[back.localCell( (int)(k & 0xFFFFFFFFL), (int)(k >> 32) )] );
			assertFalse( diffs( back ).containsKey( k ) );
		}
		//the first steps after the load lay nothing a second time, and announce nothing again
		assertTrue( logged( back::pollEvents ).isEmpty() );
		assertEquals( traders, traders( back ).size() );
		assertEquals( 1, fragments( back, star.id ).size() );

		//a save from before the events loads, with nothing of them
		for (String k : new ArrayList<>( b.getKeys() )) if (k.startsWith( "event_" )) b.remove( k );
		OverworldLevel old = new OverworldLevel();
		old.restoreFromBundle( b );
		assertTrue( log( old ).announced.isEmpty() );
		assertTrue( log( old ).forced.isEmpty() );
		assertTrue( log( old ).lootAt.isEmpty() );
	}

	@Test
	public void theStreaksTrailIsSeen(){
		float elapsed = Game.elapsed;
		try {
			Game.elapsed = 0.016f;
			StarStreak.Trail core = new StarStreak.Trail();
			core.core( 10, 10 );
			core.update();
			assertTrue( core.scale.x > 0 );
			StarStreak.Trail ember = new StarStreak.Trail();
			ember.ember( 10, 10 );
			ember.update();
			assertTrue( ember.scale.x > 0 );
			assertTrue( ember.alive );
		} finally {
			Game.elapsed = elapsed;
		}
	}

	@Test
	public void theTrollForgesTwoFragmentsIntoACore(){
		Dungeon.cycleTurn = 10 * FC + 700;
		WorldEvents.Event e = marketDue();
		windowAt( e.wx, e.wy + 3, WorldModel.calendarShift() );
		Blacksmith2 smith = new Blacksmith2();
		try {
			Method forge = Blacksmith2.class.getDeclaredMethod( "starForge" );
			forge.setAccessible( true );
			StarFragment three = new StarFragment();
			three.quantity( 3 );
			assertTrue( three.collect( hero.belongings.backpack ) );
			hero.sprite = null;
			forge.invoke( smith );
		} catch (java.lang.reflect.InvocationTargetException ex){
			//the burst of light about the hero needs his sprite on a scene; the work is done by then
			if (!(ex.getCause() instanceof NullPointerException)) throw new AssertionError( ex.getCause() );
		} catch (Exception ex){
			throw new AssertionError( ex );
		}
		assertEquals( 1, hero.belongings.getItem( StarFragment.class ).quantity() );
		assertEquals( 1, hero.belongings.getItem( xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore.class ).quantity() );
	}

	@Test
	public void everyEventStringResolves(){
		for (String k : new String[]{ "star_falls_near", "star_glow_near", "star_cold_near", "star_bonus", "map_fallen_star", "map_market",
				"dir_n", "dir_ne", "dir_e", "dir_se", "dir_s", "dir_sw", "dir_w", "dir_nw" }){
			assertNotEquals( k, Messages.NO_TEXT_FOUND, Messages.get( WorldEvents.class, k ) );
		}
		for (WorldEvents.Type t : WorldEvents.Type.values()){
			assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( WorldEvents.class, "map_" + t.key ) );
		}
		String opens = Messages.get( WorldEvents.class, "market_opens", "Ashford", "north" );
		assertTrue( opens.contains( "Ashford" ) && opens.contains( "north" ) );
		assertTrue( Messages.get( WorldEvents.class, "market_here", "Ashford" ).contains( "Ashford" ) );
		for (String k : new String[]{ "star_falls", "star_falls_clouded", "star_glow", "star_cold" }){
			assertTrue( k, Messages.get( WorldEvents.class, k, "south-west" ).contains( "south-west" ) );
		}
		assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( StarFragment.class, "name" ) );
		assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( StarFragment.class, "desc" ) );
		for (int k = 0; k < TravellingMerchant.SPECIALITIES; k++){
			assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( TravellingMerchant.class, "name_" + k ) );
			assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( TravellingMerchant.class, "talk_" + k ) );
		}
		for (String k : new String[]{ "desc", "flee", "closed", "warn" }){
			assertNotEquals( k, Messages.NO_TEXT_FOUND, Messages.get( TravellingMerchant.class, k ) );
		}
		for (String k : new String[]{ "opt_starforge", "no_fragments", "starforged" }){
			assertNotEquals( k, Messages.NO_TEXT_FOUND, Messages.get( Blacksmith2.class, k ) );
		}
		assertTrue( Messages.get( WndWorldMap.class, "waypoint_event", "Market" ).contains( "Market" ) );
	}
}

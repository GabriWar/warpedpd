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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FestivalChampion;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FestivalCook;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.FestivalChampionSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.FestivalCookSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
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
 * The villages' festivals (FestivalEvent) on paper: once a season in every human village with folk
 * in it and nowhere else, a pure function of the seed, the village and the season on both clocks,
 * from one morning to the next and never into another season, heard of about as often as a
 * market; a forced one goes through the same enumeration. And the parts that make one: the folk's
 * festival day (VillageRoutine.festivalSlot) and the dance's rings, the stalls' cheap fare and
 * the champion's friendly bout, which nobody loses more than two thirds of his health to - and
 * what of them a save keeps.
 */
public class FestivalEventTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int FC = WorldEvents.FC;
	private static final List<WorldEvents.Event> NONE = Collections.emptyList();

	private int turn, startDay, challenges;
	private DayNightCycle.Phase override;
	private Level saved;
	private Hero savedHero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		override = DayNightCycle.debugPhaseOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.challenges = challenges;
		DayNightCycle.debugPhaseOverride = override;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	//the human villages with folk in them within ten sectors of the origin, as {sx, sy}
	private static ArrayList<int[]> villages(){
		ArrayList<int[]> out = new ArrayList<>();
		for (int[] v : WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 1, 10 )){
			if (FestivalEvent.keepsFestivals( SEED, v[0], v[1] )) out.add( new int[]{ v[0], v[1] } );
		}
		assertTrue( "no human villages near the origin", out.size() >= 5 );
		return out;
	}

	@Test
	public void onceASeasonInEveryHumanVillage(){
		for (int[] v : villages()){
			int seasons = 0;
			for (int d = 0; d < 2 * 364; ){
				EventSeasons.Span s = EventSeasons.of( d );
				int found = 0;
				for (int k = s.first; k < s.end(); k++){
					WorldEvents.Event e = FestivalEvent.festivalEvent( SEED, v[0], v[1], k );
					if (e == null) continue;
					found++;
					//from the morning of its day to the next morning, inside its season
					assertEquals( k * FC + FestivalEvent.OPEN, e.startTurn );
					assertEquals( (k + 1) * FC + FestivalEvent.OPEN, e.endTurn );
					assertTrue( k >= s.first + FestivalEvent.EDGE );
					assertTrue( k + 1 < s.end() - 1 );
					assertEquals( WorldStructures.siteX( SEED, v[0], v[1] ), e.wx );
					assertEquals( WorldStructures.siteY( SEED, v[0], v[1] ), e.wy );
					assertEquals( WorldEvents.idOf( WorldEvents.Type.FESTIVAL, v[0], v[1], k ), e.id );
				}
				if (s.first >= 0){
					assertEquals( "village " + v[0] + "," + v[1] + " in the season from " + s.first, 1, found );
					seasons++;
				}
				d = s.end();
			}
			assertTrue( seasons >= 7 );
		}
	}

	@Test
	public void neverOutsideAHumanVillageWithFolk(){
		for (int sy = -10; sy <= 10; sy++){
			for (int sx = -10; sx <= 10; sx++){
				if (FestivalEvent.keepsFestivals( SEED, sx, sy )) continue;
				for (int d = 0; d < 364; d++) assertNull( FestivalEvent.festivalEvent( SEED, sx, sy, d ) );
			}
		}
	}

	@Test
	public void theSeasonsHoldOnARealClockToo(){
		Dungeon.challenges = Challenges.REAL_CLOCK;
		//a real-clock day is a local day from the epoch: 2026 has its seasons on the calendar's months
		int day = 20500;   //2026-02-16
		EventSeasons.Span winter = EventSeasons.of( day );
		assertEquals( GameCalendar.Season.WINTER, winter.season );
		assertEquals( GameCalendar.Season.SPRING, EventSeasons.of( winter.end() ).season );
		assertEquals( GameCalendar.Season.AUTUMN, GameCalendar.seasonOfDay( winter.first - 1 ) );
		assertTrue( winter.days >= 90 && winter.days <= 91 );
		int[] v = villages().get( 0 );
		int found = 0;
		for (int k = winter.first; k < winter.end(); k++) if (FestivalEvent.festivalEvent( SEED, v[0], v[1], k ) != null) found++;
		assertEquals( 1, found );
	}

	@Test
	public void festivalsArePureAndListedOnTheirDays(){
		int[] v = villages().get( 1 );
		ArrayList<String> first = new ArrayList<>(), second = new ArrayList<>();
		for (int d = 0; d < 400; d++){
			WorldEvents.Event e = FestivalEvent.festivalEvent( SEED, v[0], v[1], d );
			if (e != null) first.add( e.id + "@" + e.startTurn );
		}
		//another run's calendar in between: the seasons are worked out again
		Dungeon.calendarStartDay = 37;
		EventSeasons.of( 5 );
		Dungeon.calendarStartDay = 0;
		for (int d = 399; d >= 0; d--){
			WorldEvents.Event e = FestivalEvent.festivalEvent( SEED, v[0], v[1], d );
			if (e != null) second.add( 0, e.id + "@" + e.startTurn );
		}
		assertEquals( first, second );
		assertFalse( first.isEmpty() );
		//on its day and into the next morning, it is among the world's events
		WorldEvents.Event e = null;
		for (int d = 0; d < 200 && e == null; d++) e = FestivalEvent.festivalEvent( SEED, v[0], v[1], d );
		assertNotNull( e );
		for (int d = e.startDay; d <= e.startDay + 1; d++){
			boolean listed = false;
			for (WorldEvents.Event o : WorldEvents.eventsNear( SEED, v[0], v[1], 1, d, NONE )) listed |= o.id == e.id;
			assertTrue( "day " + d, listed );
		}
		//it says itself, never one of the world's own announcements
		ArrayList<WorldEvents.Event> near = new ArrayList<>();
		near.add( e );
		assertTrue( WorldEvents.toAnnounce( near, new HashMap<>(), new HashMap<>(), v[0], v[1], e.startTurn + 10,
				e.wx - 80, e.wy - 80, 176, 176 ).isEmpty() );
	}

	@Test
	public void aHeroHearsOfOneEveryFewWeeks(){
		//from a random spot of the land, how often is a festival on within HEAR sectors?
		java.util.Random rng = new java.util.Random( 11 );
		int samples = 0, on = 0;
		for (int k = 0; k < 400; k++){
			int sx = rng.nextInt( 41 ) - 20, sy = rng.nextInt( 41 ) - 20;
			WorldModel.Biome b = WorldModel.baseBiomeAt( SEED, sx * WorldStructures.SECTOR + 48, sy * WorldStructures.SECTOR + 48 );
			if (b == WorldModel.Biome.OCEAN || b == WorldModel.Biome.MOUNTAIN) continue;
			for (int d = 30; d < 400; d += 9){
				samples++;
				for (WorldEvents.Event e : WorldEvents.eventsNear( SEED, sx, sy, FestivalEvent.HEAR, d, NONE )){
					if (e.type == WorldEvents.Type.FESTIVAL && e.activeAt( d * FC + 1500 )){
						on++;
						break;
					}
				}
			}
		}
		//a festival lasts a day: the share of days one is on is how often a new one is heard of
		double every = samples / (double) Math.max( 1, on );
		System.out.println( "[festivals] one on within " + FestivalEvent.HEAR + " sectors on one day in " + Math.round( every ) );
		assertTrue( "too often: every " + every, every >= 4 );
		assertTrue( "too seldom: every " + every, every <= 45 );
	}

	@Test
	public void aForcedFestivalHasAnIdOfItsOwn(){
		int[] v = villages().get( 0 );
		int t = 77 * FC + 1900;
		WorldEvents.Event f = FestivalEvent.forcedFestival( SEED, v[0], v[1], t );
		assertEquals( WorldEvents.Type.FESTIVAL, f.type );
		assertEquals( t, f.startTurn );
		assertEquals( 78 * FC + FestivalEvent.OPEN, f.endTurn );
		assertNotEquals( WorldEvents.idOf( WorldEvents.Type.FESTIVAL, v[0], v[1], 77 ), f.id );
		ArrayList<WorldEvents.Event> forced = new ArrayList<>();
		forced.add( f );
		boolean listed = false;
		for (WorldEvents.Event o : WorldEvents.eventsNear( SEED, v[0], v[1], 1, 77, forced )) listed |= o.id == f.id;
		assertTrue( listed );
		//its bunting runs to the nearest houses' walls
		int[] ends = FestivalEvent.ropeEnds( SEED, f );
		assertTrue( ends.length >= 2 && ends.length <= 2 * FestivalEvent.ROPES );
		for (int i = 0; i + 1 < ends.length; i += 2){
			assertTrue( Math.max( Math.abs( ends[i] - f.wx ), Math.abs( ends[i + 1] - f.wy ) ) <= FestivalEvent.ROPE_REACH );
		}
	}

	// ------------------------------------------------------------------ the folk's day

	private static VillageRoutine.Clock at( DayNightCycle.Phase phase, float progress, VillageRoutine.Weather w ){
		return new VillageRoutine.Clock( phase, progress, w, false, 10, 100 );
	}

	@Test
	public void theFolkFeastByDayAndDanceFromDusk(){
		VillageRoutine.Weather fair = VillageRoutine.Weather.FAIR, rain = VillageRoutine.Weather.RAIN;
		int p = 0;
		for (VillageRoutine.Role r : VillageRoutine.Role.values()){
			if (VillageRoutine.belongs( r, true )) continue;
			//by day the work is put by for the well
			assertEquals( VillageRoutine.Slot.GATHER, VillageRoutine.festivalSlot( r, VillageRoutine.Slot.WORK, at( DayNightCycle.Phase.DAY, 0.4f, fair ), p ) );
			//from dusk they dance, but for the old folk
			VillageRoutine.Slot dusk = VillageRoutine.festivalSlot( r, VillageRoutine.Slot.GATHER, at( DayNightCycle.Phase.DUSK, 0.3f, fair ), p );
			assertEquals( r == VillageRoutine.Role.ELDER ? VillageRoutine.Slot.GATHER : VillageRoutine.Slot.DANCE, dusk );
			//into the night until each one's late hour
			assertEquals( r == VillageRoutine.Role.ELDER ? VillageRoutine.Slot.BED : VillageRoutine.Slot.DANCE,
					VillageRoutine.festivalSlot( r, VillageRoutine.Slot.BED, at( DayNightCycle.Phase.NIGHT, 0.1f, fair ), p ) );
			assertEquals( VillageRoutine.Slot.BED,
					VillageRoutine.festivalSlot( r, VillageRoutine.Slot.BED, at( DayNightCycle.Phase.NIGHT, 0.95f, fair ), p ) );
			//the weather still wins, and the dawn is slept through
			assertEquals( VillageRoutine.Slot.INDOORS, VillageRoutine.festivalSlot( r, VillageRoutine.Slot.INDOORS, at( DayNightCycle.Phase.DUSK, 0.3f, rain ), p ) );
			assertEquals( VillageRoutine.Slot.BED, VillageRoutine.festivalSlot( r, VillageRoutine.Slot.BED, at( DayNightCycle.Phase.NIGHT, 0.1f, rain ), p ) );
			assertEquals( VillageRoutine.Slot.BED, VillageRoutine.festivalSlot( r, VillageRoutine.Slot.BED, at( DayNightCycle.Phase.DAWN, 0.1f, fair ), p ) );
			assertEquals( "dance", VillageRoutine.chatStem( r, VillageRoutine.Slot.DANCE, at( DayNightCycle.Phase.DUSK, 0.3f, fair ), true ) );
		}
		for (int i = 0; i < 3; i++){
			assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager.class, "dance_" + i ) );
			assertNotEquals( Messages.NO_TEXT_FOUND, Messages.get( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager.class, "festival_" + i ) );
		}
	}

	@Test
	public void theDanceRingsGoRoundTheWell(){
		for (int r : new int[]{ VillageRoutine.DANCE_INNER, VillageRoutine.DANCE_OUTER }){
			HashSet<Integer> seen = new HashSet<>();
			for (int k = 0; k < 8 * r; k++){
				int dx = VillageRoutine.ringX( r, k ), dy = VillageRoutine.ringY( r, k );
				assertEquals( r + ":" + k, k, VillageRoutine.ringIndex( r, dx, dy ) );
				seen.add( dx * 100 + dy );
				//the next place round is always a step away
				int nx = VillageRoutine.ringX( r, (k + 1) % (8 * r) ), ny = VillageRoutine.ringY( r, (k + 1) % (8 * r) );
				assertEquals( 1, Math.max( Math.abs( nx - dx ), Math.abs( ny - dy ) ) );
			}
			assertEquals( 8 * r, seen.size() );
		}
		//half the village on each ring
		int inner = 0;
		for (long s = 0; s < 1000; s++) if (VillageRoutine.danceRing( VillageRoutine.spotHash( s * 7919, 1 ) ) == VillageRoutine.DANCE_INNER) inner++;
		assertTrue( inner > 400 && inner < 600 );
	}

	@Test
	public void theDanceGoesOnPastWhatStandsOnItsRings(){
		//open ground round a well: a house's walls across the outer ring to the east, and a tuft
		//of tall grass on the inner ring to the west
		int w = 48, cx = 24, cy = 24;
		OverworldLevel ow = new OverworldLevel();
		ow.setSize( w, w );
		ow.heaps = new SparseArray<>();
		ow.plants = new SparseArray<>();
		java.util.Arrays.fill( ow.map, Terrain.GRASS );
		java.util.Arrays.fill( ow.passable, true );
		for (int y = cy - 2; y <= cy + 2; y++){
			for (int x = cx + 4; x <= cx + 8; x++){
				ow.map[x + y * w] = Terrain.WALL;
				ow.passable[x + y * w] = false;
			}
		}
		ow.map[cx - 3 + cy * w] = Terrain.HIGH_GRASS;
		VillageRoutine.Settlement s = new VillageRoutine.Settlement( 0L, 0, 0, cx, cy, false, new int[]{ 10 }, new int[0] );
		for (int ring : new int[]{ VillageRoutine.DANCE_INNER, VillageRoutine.DANCE_OUTER }){
			//the places of its ring a dancer can stand on, from the top in the way it turns: the
			//inner ring clockwise, the outer the other way
			int turn = ring == VillageRoutine.DANCE_INNER ? 1 : -1, top = VillageRoutine.ringIndex( ring, 0, -ring );
			ArrayList<Integer> free = new ArrayList<>();
			for (int j = 0; j < 8 * ring; j++){
				int k = Math.floorMod( top + turn * j, 8 * ring );
				int c = cx + VillageRoutine.ringX( ring, k ) + (cy + VillageRoutine.ringY( ring, k )) * w;
				if (VillageRoutine.standable( ow, c )) free.add( c );
			}
			assertTrue( free.size() < 8 * ring );
			assertEquals( cx + turn + (cy - ring) * w, (int) free.get( 1 ) );
			//round and back to the top: every place it can stand on in turn, on past the others
			int pos = free.get( 0 );
			for (int i = 1; i <= free.size(); i++){
				pos = VillageRoutine.danceStep( ow, s, pos, ring );
				assertEquals( "ring " + ring + ", step " + i, (int) free.get( i % free.size() ), pos );
			}
			//off its ring, it has none
			assertEquals( -1, VillageRoutine.danceStep( ow, s, cx + (cy - ring - 1) * w, ring ) );
		}
	}

	// ------------------------------------------------------------------ the stalls and the bout

	//a cook's shelf, as his first act lays it out (FestivalCook.stockSource)
	@SuppressWarnings("unchecked")
	private static ArrayList<Item> stock( FestivalCook c ){
		try {
			java.lang.reflect.Method m = FestivalCook.class.getDeclaredMethod( "stockSource" );
			m.setAccessible( true );
			return (ArrayList<Item>) m.invoke( c );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void theCooksSellMealsAtHalfPriceAndComeBackFromTheirBundles(){
		for (int stall = 0; stall < FestivalCook.STALLS; stall++){
			FestivalCook c = FestivalCook.of( stall, 1234L, 99 * FC );
			assertSame( FestivalCookSprite.class, c.spriteClass );
			assertEquals( FestivalCook.FESTIVAL_PRICE, c.priceFactor(), 0f );
			ArrayList<Item> stock = stock( c );
			assertEquals( 4, stock.size() );
			for (Item it : stock) assertTrue( it.getClass().getName(), it instanceof Food );
			Bundle b = new Bundle();
			b.put( "cook", c );
			FestivalCook back = (FestivalCook) b.get( "cook" );
			assertEquals( stall, back.stall );
			assertEquals( 1234L, back.eventId );
			assertEquals( 99 * FC, back.endTurn );
			assertSame( FestivalCookSprite.class, back.spriteClass );
			assertNotEquals( Messages.NO_TEXT_FOUND, back.chatTextFor( null ) );
		}
		for (String k : new String[]{ "name", "desc", "flee" }){
			assertNotEquals( k, Messages.NO_TEXT_FOUND, Messages.get( FestivalCook.class, k ) );
		}
	}

	private static Level field(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( 32, 32 );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		Painter.fill( l, 1, 1, 30, 30, Terrain.EMPTY );
		l.buildFlagMaps();
		return l;
	}

	@Test
	public void theBoutEndsAtAThirdAndNobodyFalls(){
		Dungeon.level = field();
		Hero hero = new Hero();
		hero.lvl = 10;
		hero.HT = hero.HP = 70;
		hero.pos = 16 + 16 * 32;
		Dungeon.hero = hero;
		Actor.add( hero );
		FestivalChampion c = FestivalChampion.of( 77L, 99 * FC, 0 );
		c.pos = 17 + 16 * 32;
		Dungeon.level.mobs.add( c );
		Actor.add( c );

		//out of a bout nothing touches him, and a tap is a word
		assertTrue( c.isInvulnerable( Hero.class ) );
		assertTrue( c.heroShouldInteract() );
		c.interact( hero );
		assertFalse( c.inBout() );
		c.interact( hero );
		assertTrue( "a second word starts it", c.inBout() );
		assertFalse( c.heroShouldInteract() );
		assertFalse( c.isInvulnerable( Hero.class ) );
		//matched to the hero
		assertEquals( 20 + 6 * 10, c.HT );
		assertEquals( c.HT, c.HP );

		//his blows stop at a third of the hero's health, and end the bout
		int stop = Math.round( hero.HT * FestivalChampion.YIELD );
		hero.HP = stop + 5;
		assertEquals( 5, c.attackProc( hero, 30 ) );
		assertFalse( c.inBout() );
		assertEquals( c.HT, c.HP );

		//a fresh bout: brought down to a third, he yields - never dies
		hero.HP = hero.HT;
		c.interact( hero );
		c.interact( hero );
		assertTrue( c.inBout() );
		c.damage( 1000, hero );
		assertTrue( c.isAlive() );
		assertFalse( c.inBout() );
		assertEquals( c.HT, c.HP );

		//a hero worn down to a third is told to rest first
		hero.HP = stop;
		c.interact( hero );
		c.interact( hero );
		assertFalse( c.inBout() );

		//a save keeps his festival, never a bout
		Bundle b = new Bundle();
		b.put( "champion", c );
		FestivalChampion back = (FestivalChampion) b.get( "champion" );
		assertEquals( 77L, back.festival );
		assertEquals( 99 * FC, back.endTurn );
		assertSame( FestivalChampionSprite.class, back.spriteClass );
		assertFalse( back.inBout() );
		for (String k : new String[]{ "name", "desc", "terms", "begin", "yield", "won", "off", "beaten", "rest" }){
			assertNotEquals( k, Messages.NO_TEXT_FOUND, Messages.get( FestivalChampion.class, k ) );
		}
		for (String k : new String[]{ "heard", "heard_here", "cheer", "prize", "bested", "season_spring", "season_summer",
				"season_autumn", "season_winter" }){
			assertNotEquals( k, Messages.NO_TEXT_FOUND, Messages.get( FestivalEvent.class, k ) );
		}
	}
}

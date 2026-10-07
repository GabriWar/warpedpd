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
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Thief;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaravanTrain;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RoadPatrol;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Traveller;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CaravanTrainSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MessengerSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.PilgrimSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The roads' people with no screen: a caravan's ambush springing once and settling SAVED,
 * ROBBED or SPARED by where the heroes are; the rescued stall's discount reaching its own
 * shelf only; outlaws cut down by the watch paying nobody; the ambush party going for the
 * hero; and, on a real window of the world, travellers set down where their clock says and
 * never twice, walking the road at their pace, the watch falling on an outlaw, walkers
 * dropped (never parked) when the window slides, and every new field surviving a save and an
 * older save loading.
 */
public class RoadLifeTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
	private static final long STALL = 4242L;

	private int turn, startDay, challenges, depth, branch;
	private DayNightCycle.Phase override;
	private Level saved;
	private Hero savedHero;

	private Level level;
	private Hero hero;
	private Caravaneer c;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Generator.fullReset();
		xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll.initLabels();
		xyz.gabriwar.warpedpixeldungeon.items.potions.Potion.initColors();
		xyz.gabriwar.warpedpixeldungeon.items.rings.Ring.initGems();
		//identifying the caravaneer's gift reaches the badges
		Field global = Badges.class.getDeclaredField( "global" );
		global.setAccessible( true );
		global.set( null, new HashSet<>( Arrays.asList( Badges.Badge.values() ) ) );
	}

	private static Level bareLevel( int w, int h ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public boolean waterCanFreeze(){ return true; }
		};
		l.setSize( w, h );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		return l;
	}

	private static int at( int x, int y ){
		return x + y * 64;
	}

	@Before
	public void setUp(){
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		override = DayNightCycle.debugPhaseOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		DayNightCycle.debugPhaseOverride = null;
		Dungeon.cycleTurn = 10 * DayNightCycle.FULL_CYCLE + 700;
		hero = new Hero();
		hero.lvl = 12;
		Dungeon.hero = hero;

		level = bareLevel( 64, 64 );
		Painter.fill( level, 1, 1, 62, 62, Terrain.EMPTY );
		level.buildFlagMaps();
		Dungeon.level = level;
		Arrays.fill( level.heroFOV, true );

		c = new Caravaneer();
		c.pos = at( 14, 14 );
		c.stall = STALL;
		c.pitched = 10;
		c.ambush = Caravaneer.AMBUSH_PENDING;
		level.mobs.add( c );
		for (int cell : new int[]{ at( 14, 13 ), at( 14, 15 ), at( 13, 14 ), at( 15, 14 ) }){
			level.drop( new Food(), cell ).type = Heap.Type.FOR_SALE;
		}
		hero.pos = at( 26, 14 );
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
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	private ArrayList<OverworldBandit> raiders( long stall ){
		ArrayList<OverworldBandit> out = new ArrayList<>();
		for (Mob m : level.mobs){
			if (m instanceof OverworldBandit && ((OverworldBandit) m).ambushOf == stall) out.add( (OverworldBandit) m );
		}
		return out;
	}

	private int forSale(){
		int n = 0;
		for (Heap h : level.heaps.valueList()) if (h.type == Heap.Type.FOR_SALE) n++;
		return n;
	}

	private static OverworldBandit raider( long stall, int cell ){
		OverworldBandit b = new OverworldBandit();
		b.ambushOf = stall;
		b.pos = cell;
		return b;
	}

	//a sprite that draws nothing: the act loop only places, turns and fells it
	private static CharSprite stub(){
		CharSprite sp = new CharSprite(){
			@Override public void place( int cell ){ }
			@Override public void turnTo( int from, int to ){ }
			@Override public void update(){ }
			@Override public void die(){ }
			@Override public void showStatus( int color, String text, Object... args ){ }
		};
		sp.visible = false;
		return sp;
	}

	private static final Method ACT, CHOOSE;
	static {
		try {
			ACT = Actor.class.getDeclaredMethod( "act" );
			ACT.setAccessible( true );
			CHOOSE = Mob.class.getDeclaredMethod( "chooseEnemy" );
			CHOOSE.setAccessible( true );
		} catch (Exception e){
			throw new RuntimeException( e );
		}
	}

	private static boolean act( Actor a ){
		try {
			return (Boolean) ACT.invoke( a );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	private static Char choose( Mob m ){
		try {
			return (Char) CHOOSE.invoke( m );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	// ------------------------------------------------------------ the ambush

	@Test
	public void ambushSpringsOnceAroundTheStall(){
		assertTrue( CaravanAmbush.due( level, c ) );
		int n = CaravanAmbush.spring( level, c );
		assertTrue( "sprang " + n, n >= 2 && n <= 4 );
		ArrayList<OverworldBandit> rs = raiders( STALL );
		assertEquals( n, rs.size() );
		HashSet<Integer> cells = new HashSet<>();
		int menacers = 0;
		for (OverworldBandit b : rs){
			int d = level.distance( b.pos, c.pos );
			assertTrue( "at " + d, d >= 2 && d <= 4 );
			assertTrue( level.passable[b.pos] );
			assertNull( level.heaps.get( b.pos ) );
			assertTrue( level.distance( b.pos, hero.pos ) >= 3 );
			assertTrue( cells.add( b.pos ) );
			if (b.menace) menacers++;
			assertSame( b.menace ? c : hero, b.getEnemy() );
		}
		assertEquals( 1, menacers );
		assertEquals( Caravaneer.AMBUSH_BESIEGED, c.ambush );
		//never twice
		assertFalse( CaravanAmbush.due( level, c ) );
		assertEquals( 0, CaravanAmbush.spring( level, c ) );
		assertEquals( n, raiders( STALL ).size() );
		//no trading through a siege, and no harm reaches the keeper
		assertNotNull( c.tradeBlock() );
		assertEquals( Messages.get( Caravaneer.class, "talk_besieged" ), c.tradeBlock() );
		assertFalse( c.add( new xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison() ) );
		assertEquals( Char.INFINITE_EVASION, c.defenseSkill( rs.get( 0 ) ) );
	}

	@Test
	public void anyHeroInSightSpringsTwoToFour(){
		//nobody is let pass, a hero fresh out of town no more than a seasoned one
		hero.lvl = 1;
		assertTrue( CaravanAmbush.due( level, c ) );
		//out of sight, or too far: not yet
		Arrays.fill( level.heroFOV, false );
		assertFalse( CaravanAmbush.due( level, c ) );
		Arrays.fill( level.heroFOV, true );
		hero.pos = at( 29, 14 );
		assertFalse( CaravanAmbush.due( level, c ) );
		//two, three or four of them, whoever the hero
		hero.pos = at( 26, 14 );
		HashSet<Integer> sizes = new HashSet<>();
		for (int i = 0; i < 60; i++){
			level.mobs.removeAll( raiders( STALL ) );
			c.ambush = Caravaneer.AMBUSH_PENDING;
			hero.lvl = 1 + i % 20;
			int n = CaravanAmbush.spring( level, c );
			assertTrue( "sprang " + n, n >= 2 && n <= 4 );
			sizes.add( n );
		}
		assertEquals( new HashSet<>( Arrays.asList( 2, 3, 4 ) ), sizes );
	}

	//nothing chases the keeper off his stall through a siege (a thief's lift gone wrong, the
	//ascent's flight): he and his goods stay until it is over, so it always settles
	@Test
	public void nothingChasesTheKeeperOffThroughASiege(){
		Actor.add( c );
		CaravanAmbush.spring( level, c );
		c.flee();
		assertTrue( level.mobs.contains( c ) );
		assertTrue( Actor.all().contains( c ) );
		assertEquals( 4, forSale() );
		assertEquals( Caravaneer.AMBUSH_BESIEGED, c.ambush );
	}

	@Test
	public void nowhereToStandMeansNoAmbush(){
		for (int y = 10; y <= 18; y++){
			for (int x = 10; x <= 18; x++){
				if (Math.max( Math.abs( x - 14 ), Math.abs( y - 14 ) ) >= 2) Painter.set( level, x, y, Terrain.WALL );
			}
		}
		level.buildFlagMaps();
		assertEquals( 0, CaravanAmbush.spring( level, c ) );
		assertEquals( Caravaneer.AMBUSH_NONE, c.ambush );
		assertTrue( raiders( STALL ).isEmpty() );
	}

	@Test
	public void savedWhenTheLastRaiderFallsNearby(){
		CaravanAmbush.spring( level, c );
		HashMap<Long, Mob> parked = new HashMap<>();
		HashMap<Long, Float> parkedAt = new HashMap<>();
		assertFalse( CaravanAmbush.settle( level, c, parked, parkedAt ) );
		assertEquals( Caravaneer.AMBUSH_BESIEGED, c.ambush );
		level.mobs.removeAll( raiders( STALL ) );
		HashSet<Integer> before = new HashSet<>();
		for (Heap h : level.heaps.valueList()) before.add( h.pos );
		assertTrue( CaravanAmbush.settle( level, c, parked, parkedAt ) );
		assertEquals( Caravaneer.AMBUSH_SAVED, c.ambush );
		int gifts = 0;
		for (Heap h : level.heaps.valueList()){
			if (before.contains( h.pos )) continue;
			gifts++;
			assertNotEquals( Heap.Type.FOR_SALE, h.type );
			assertTrue( level.adjacent( h.pos, c.pos ) );
		}
		assertEquals( 1, gifts );
		assertEquals( 4, forSale() );
		assertEquals( 0.7f, c.priceFactor(), 0f );
		assertEquals( 70, Shopkeeper.discounted( 100, 0.7f ) );
		assertEquals( 1, Shopkeeper.discounted( 1, 0.7f ) );
		assertEquals( Messages.get( Caravaneer.class, "talk_saved" ), c.chatTextFor( null ) );
		assertFalse( c.chatTextFor( null ).contains( Messages.NO_TEXT_FOUND ) );
		assertNull( c.tradeBlock() );
		//a stall pitched on another day keeps its prices
		c.pitched = 9;
		assertEquals( 1f, c.priceFactor(), 0f );
		assertFalse( c.chatTextFor( null ).equals( Messages.get( Caravaneer.class, "talk_saved" ) ) );
	}

	@Test
	public void savedEvenWhenAScaredRaiderRanOffPastTheWindow(){
		CaravanAmbush.spring( level, c );
		HashMap<Long, Mob> parked = new HashMap<>();
		HashMap<Long, Float> parkedAt = new HashMap<>();
		parked.put( 99L, raider( STALL, -1 ) );
		parkedAt.put( 99L, 0f );
		level.mobs.removeAll( raiders( STALL ) );
		assertTrue( CaravanAmbush.settle( level, c, parked, parkedAt ) );
		assertEquals( Caravaneer.AMBUSH_SAVED, c.ambush );
		assertTrue( parked.isEmpty() );
		assertTrue( parkedAt.isEmpty() );
	}

	@Test
	public void robbedWhenTheHeroWalksAway(){
		CaravanAmbush.spring( level, c );
		HashMap<Long, Mob> parked = new HashMap<>();
		HashMap<Long, Float> parkedAt = new HashMap<>();
		parked.put( 77L, raider( STALL, -1 ) );
		parkedAt.put( 77L, 0f );
		Arrays.fill( level.heroFOV, false );
		hero.pos = at( 50, 50 );
		assertTrue( CaravanAmbush.settle( level, c, parked, parkedAt ) );
		assertEquals( Caravaneer.AMBUSH_ROBBED, c.ambush );
		assertEquals( 2, forSale() );
		assertTrue( raiders( STALL ).isEmpty() );
		assertTrue( parked.isEmpty() );
		assertTrue( parkedAt.isEmpty() );
		assertEquals( Messages.get( Caravaneer.class, "talk_robbed" ), c.chatTextFor( null ) );
		assertEquals( 1f, c.priceFactor(), 0f );
	}

	@Test
	public void robbersInSightMakeOffWithTheirMark(){
		CaravanAmbush.spring( level, c );
		OverworldBandit near = raider( STALL, at( 45, 45 ) );
		level.mobs.add( near );
		hero.pos = at( 50, 50 );
		assertTrue( CaravanAmbush.settle( level, c, new HashMap<>(), new HashMap<>() ) );
		assertEquals( Caravaneer.AMBUSH_ROBBED, c.ambush );
		//the one the hero could be watching runs for the trees, still the stall's
		assertTrue( level.mobs.contains( near ) );
		assertTrue( near.retreating );
		assertEquals( STALL, near.ambushOf );
		assertNull( near.getEnemy() );
		assertEquals( 1, raiders( STALL ).size() );
	}

	@Test
	public void sparedWhenOthersCutThemDown(){
		CaravanAmbush.spring( level, c );
		level.mobs.removeAll( raiders( STALL ) );
		hero.pos = at( 37, 14 );
		assertFalse( CaravanAmbush.settle( level, c, new HashMap<>(), new HashMap<>() ) );
		assertEquals( Caravaneer.AMBUSH_BESIEGED, c.ambush );
		hero.pos = at( 50, 50 );
		int heaps = level.heaps.valueList().size();
		assertTrue( CaravanAmbush.settle( level, c, new HashMap<>(), new HashMap<>() ) );
		assertEquals( Caravaneer.AMBUSH_SPARED, c.ambush );
		assertEquals( heaps, level.heaps.valueList().size() );
		assertEquals( 4, forSale() );
		assertEquals( Messages.get( Caravaneer.class, "talk_spared" ), c.chatTextFor( null ) );
	}

	@Test
	public void theDayOutlastedIsARobbery(){
		CaravanAmbush.spring( level, c );
		CaravanAmbush.outlastedTheDay( level, c );
		assertEquals( Caravaneer.AMBUSH_ROBBED, c.ambush );
		assertEquals( 2, forSale() );
	}

	@Test
	public void orphanSweepTouchesOnlyStaleParties(){
		OverworldBandit kept = raider( STALL, at( 18, 18 ) );
		OverworldBandit gone = raider( 777L, at( 60, 60 ) );
		OverworldBandit watched = raider( 777L, at( 30, 14 ) );
		OverworldBandit camp = raider( OverworldBandit.NO_AMBUSH, at( 58, 58 ) );
		OverworldBandit leaving = raider( 777L, at( 59, 59 ) );
		leaving.retreating = true;
		OverworldBandit turned = raider( 777L, at( 60, 58 ) );
		turned.alignment = Char.Alignment.ALLY;
		level.mobs.addAll( Arrays.asList( kept, gone, watched, camp, leaving, turned ) );
		HashMap<Long, Mob> parked = new HashMap<>();
		HashMap<Long, Float> parkedAt = new HashMap<>();
		parked.put( 1L, raider( 777L, -1 ) );
		parked.put( 2L, raider( STALL, -1 ) );
		OverworldBandit turnedAway = raider( 777L, -1 );
		turnedAway.alignment = Char.Alignment.ALLY;
		parked.put( 3L, turnedAway );
		parkedAt.put( 1L, 0f );
		parkedAt.put( 2L, 0f );
		parkedAt.put( 3L, 0f );
		HashSet<Long> standing = new HashSet<>();
		standing.add( STALL );
		CaravanAmbush.disbandOrphans( level, standing, parked, parkedAt );
		//one the hero turned stays his, near or far, live or parked: an outlaw of no party now
		assertTrue( level.mobs.contains( turned ) );
		assertEquals( OverworldBandit.NO_AMBUSH, turned.ambushOf );
		assertSame( turnedAway, parked.get( 3L ) );
		assertEquals( OverworldBandit.NO_AMBUSH, turnedAway.ambushOf );
		parked.remove( 3L );
		parkedAt.remove( 3L );
		assertTrue( level.mobs.contains( kept ) );
		assertEquals( STALL, kept.ambushOf );
		assertFalse( level.mobs.contains( gone ) );
		//one a hero could see stays on, an ordinary outlaw now
		assertTrue( level.mobs.contains( watched ) );
		assertEquals( OverworldBandit.NO_AMBUSH, watched.ambushOf );
		assertTrue( level.mobs.contains( camp ) );
		assertTrue( level.mobs.contains( leaving ) );
		assertFalse( parked.containsKey( 1L ) );
		assertTrue( parked.containsKey( 2L ) );
		assertEquals( 1, parkedAt.size() );
	}

	//a raider the hero turned (corruption) is his: once the siege is over, saved or spared, he is
	//nobody's party any more, live or parked past the window's edge, and nothing sweeps him off
	@Test
	public void aRaiderTheHeroTurnedStaysHis(){
		for (boolean near : new boolean[]{ true, false }){
			level.mobs.removeAll( raiders( STALL ) );
			hero.pos = at( 26, 14 );
			c.ambush = Caravaneer.AMBUSH_PENDING;
			assertTrue( CaravanAmbush.spring( level, c ) >= 2 );
			ArrayList<OverworldBandit> rs = raiders( STALL );
			OverworldBandit turned = rs.get( 0 );
			turned.alignment = Char.Alignment.ALLY;
			for (OverworldBandit b : rs) if (b != turned) level.mobs.remove( b );
			HashMap<Long, Mob> parked = new HashMap<>();
			HashMap<Long, Float> parkedAt = new HashMap<>();
			OverworldBandit away = raider( STALL, -1 );
			away.alignment = Char.Alignment.ALLY;
			parked.put( 1L, away );
			parkedAt.put( 1L, 0f );
			if (near){
				//one still against the stall that a scare sent off past the edge: not coming back
				parked.put( 2L, raider( STALL, -1 ) );
				parkedAt.put( 2L, 0f );
			} else {
				hero.pos = at( 50, 50 );
			}
			assertTrue( CaravanAmbush.settle( level, c, parked, parkedAt ) );
			assertEquals( near ? Caravaneer.AMBUSH_SAVED : Caravaneer.AMBUSH_SPARED, c.ambush );
			for (OverworldBandit b : new OverworldBandit[]{ turned, away }){
				assertEquals( OverworldBandit.NO_AMBUSH, b.ambushOf );
				assertFalse( b.menace );
			}
			assertTrue( level.mobs.contains( turned ) );
			assertSame( away, parked.get( 1L ) );
			assertFalse( parked.containsKey( 2L ) );
			assertEquals( parked.keySet(), parkedAt.keySet() );
			level.mobs.remove( turned );
		}
	}

	@Test
	public void theAmbushPartyGoesForTheHero(){
		Actor.add( c );
		OverworldBandit b = raider( STALL, at( 16, 16 ) );
		b.fieldOfView = new boolean[level.length()];
		Arrays.fill( b.fieldOfView, true );
		level.mobs.add( b );
		Actor.add( b );
		//locked on to the stall it cannot touch, with the hero right there: the hero
		b.aggro( c );
		hero.pos = at( 16, 14 );
		assertSame( hero, choose( b ) );
		//the hero in plain sight further off: still the hero
		b.aggro( c );
		hero.pos = at( 24, 20 );
		assertSame( hero, choose( b ) );
		//out of sight and away from the stall: nobody (it keeps about the stall, not on it)
		Arrays.fill( b.fieldOfView, false );
		b.aggro( c );
		assertNull( choose( b ) );
		//the one menacing the stall keeps at it while the hero stands off...
		OverworldBandit m = raider( STALL, at( 15, 15 ) );
		m.menace = true;
		m.fieldOfView = new boolean[level.length()];
		Arrays.fill( m.fieldOfView, true );
		level.mobs.add( m );
		Actor.add( m );
		m.aggro( c );
		assertSame( c, choose( m ) );
		//...and turns on him once he comes up to the stall
		hero.pos = at( 16, 13 );
		assertSame( hero, choose( m ) );
		//a camp's outlaw, or one fleeing, is none of this
		OverworldBandit camp = raider( OverworldBandit.NO_AMBUSH, at( 30, 30 ) );
		camp.fieldOfView = new boolean[level.length()];
		level.mobs.add( camp );
		Actor.add( camp );
		camp.aggro( c );
		assertSame( c, choose( camp ) );
	}

	@Test
	public void forfeitGivesNothingButReturnsTheLoot(){
		OverworldBandit b = raider( OverworldBandit.NO_AMBUSH, at( 30, 30 ) );
		level.mobs.add( b );
		Food stolen = new Food();
		((Thief) b).item = stolen;
		b.forfeit();
		assertEquals( Char.Alignment.NEUTRAL, b.alignment );
		assertNull( ((Thief) b).item );
		Heap h = level.heaps.get( b.pos );
		assertNotNull( h );
		assertTrue( h.items.contains( stolen ) );
	}

	// ------------------------------------------------------------ prices

	@Test
	public void discountReachesTheStallOnly(){
		Actor.add( c );
		c.ambush = Caravaneer.AMBUSH_SAVED;
		Food food = new Food();
		int base = Shopkeeper.sellPrice( food, null );
		int shelf = at( 14, 13 );
		assertSame( c, Shopkeeper.sellerOf( shelf ) );
		assertEquals( Shopkeeper.discounted( base, 0.7f ), Shopkeeper.sellPrice( food, null, Shopkeeper.sellerOf( shelf ) ) );
		assertTrue( Shopkeeper.sellPrice( food, null, Shopkeeper.sellerOf( shelf ) ) < base );
		int far = at( 24, 14 );
		assertNull( Shopkeeper.sellerOf( far ) );
		assertEquals( base, Shopkeeper.sellPrice( food, null, Shopkeeper.sellerOf( far ) ) );
		c.ambush = Caravaneer.AMBUSH_ROBBED;
		assertEquals( base, Shopkeeper.sellPrice( food, null, Shopkeeper.sellerOf( shelf ) ) );
		//the scroll of upgrade keeps its own ladder
		c.ambush = Caravaneer.AMBUSH_SAVED;
		xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade sou = new xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade();
		assertEquals( Shopkeeper.sellPrice( sou, null ), Shopkeeper.sellPrice( sou, null, c ) );
		//two keepers within reach: the nearer one's shelf
		Caravaneer other = new Caravaneer();
		other.pos = at( 17, 13 );
		level.mobs.add( other );
		Actor.add( other );
		assertSame( other, Shopkeeper.sellerOf( at( 16, 13 ) ) );
		assertSame( c, Shopkeeper.sellerOf( at( 15, 13 ) ) );
	}

	// ------------------------------------------------------------ saves

	@Test
	public void bundlesRoundTrip(){
		c.ambush = Caravaneer.AMBUSH_SAVED;
		c.road = 0x1234_5678_9ABCL;
		Bundle b = new Bundle();
		c.storeInBundle( b );
		Caravaneer back = new Caravaneer();
		back.restoreFromBundle( b );
		assertEquals( Caravaneer.AMBUSH_SAVED, back.ambush );
		assertEquals( 10, back.pitched );
		assertEquals( STALL, back.stall );
		assertEquals( c.road, back.road );
		for (String k : new String[]{ "ambush", "pitched", "stall", "road" }) assertTrue( k, b.remove( k ) );
		Caravaneer old = new Caravaneer();
		old.restoreFromBundle( b );
		assertEquals( Caravaneer.AMBUSH_NONE, old.ambush );
		assertEquals( -1, old.pitched );
		assertEquals( OverworldBandit.NO_AMBUSH, old.stall );
		assertEquals( Long.MIN_VALUE, old.road );

		OverworldBandit r = raider( STALL, at( 5, 5 ) );
		r.menace = true;
		r.retreating = true;
		b = new Bundle();
		r.storeInBundle( b );
		OverworldBandit rb = new OverworldBandit();
		rb.restoreFromBundle( b );
		assertEquals( STALL, rb.ambushOf );
		assertTrue( rb.menace );
		assertTrue( rb.retreating );
		for (String k : new String[]{ "ambush_of", "menace", "retreating" }) assertTrue( k, b.remove( k ) );
		OverworldBandit ro = new OverworldBandit();
		ro.restoreFromBundle( b );
		assertEquals( OverworldBandit.NO_AMBUSH, ro.ambushOf );
		assertFalse( ro.menace );
		assertFalse( ro.retreating );

		float[] route = { 1, 2, 30, 40, 90, 41, 95, 47 };
		Traveller t = Traveller.of( new RoadTraffic.Trip( 0xABCDEFL, RoadTraffic.MESSENGER, 1, 2, 3, 4, 12, 300, 600, 0.9f, route ) );
		assertEquals( MessengerSprite.class, t.spriteClass );
		b = new Bundle();
		t.storeInBundle( b );
		Traveller tb = new Traveller();
		tb.restoreFromBundle( b );
		assertEquals( RoadTraffic.MESSENGER, tb.kind );
		assertEquals( MessengerSprite.class, tb.spriteClass );
		assertEquals( 1, tb.fromSx );
		assertEquals( 2, tb.fromSy );
		assertEquals( 3, tb.toSx );
		assertEquals( 4, tb.toSy );
		assertEquals( 12, tb.day );
		assertEquals( 300, tb.depart );
		assertEquals( 600, tb.arrive );
		assertEquals( 0.9f, tb.pace, 0f );
		assertEquals( 0xABCDEFL, tb.key );
		assertArrayEquals( route, tb.route, 0f );
		for (String k : new String[]{ "route", "day", "kind", "from_x", "from_y", "to_x", "to_y", "depart", "arrive", "pace", "trip_key" }){
			assertTrue( k, b.remove( k ) );
		}
		Traveller to = new Traveller();
		to.restoreFromBundle( b );
		assertEquals( RoadTraffic.PILGRIM, to.kind );
		assertEquals( PilgrimSprite.class, to.spriteClass );
		assertEquals( 0, to.route.length );
		assertEquals( -1, to.day );
		assertEquals( 0, to.depart );
		assertEquals( 0, to.arrive );
		assertEquals( RoadTraffic.PACE[RoadTraffic.PILGRIM], to.pace, 0f );
		assertEquals( 0L, to.key );

		RoadTraffic.Beat beat = new RoadTraffic.Beat( route, new int[]{ 200, 900 }, 12 );
		RoadPatrol p = RoadPatrol.of( beat, 0x0000000300000004L, 1, 5 );
		b = new Bundle();
		p.storeInBundle( b );
		RoadPatrol pb = new RoadPatrol();
		pb.restoreFromBundle( b );
		assertArrayEquals( route, pb.route, 0f );
		assertArrayEquals( new int[]{ 200, 900 }, pb.starts );
		assertEquals( 12, pb.day );
		assertEquals( 0x0000000300000004L, pb.homeSector );
		assertEquals( 1, pb.rank );
		assertEquals( 5, pb.tint );
		for (String k : new String[]{ "route", "day", "home_sector", "rank", "tint", "starts" }) assertTrue( k, b.remove( k ) );
		RoadPatrol po = new RoadPatrol();
		po.restoreFromBundle( b );
		assertEquals( 0, po.route.length );
		assertEquals( 0, po.starts.length );
		assertEquals( -1, po.day );
		assertEquals( Long.MIN_VALUE, po.homeSector );
		assertEquals( 0, po.rank );
		assertEquals( 0, po.tint );
	}

	// ------------------------------------------------------------ a real window

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

	//the live surface window round a world cell: built as a mirror's is (no populator), then
	//made the host's own, the hero on the nearest open cell
	private OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		float shift = WorldModel.calendarShift();
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

	//a travelled road whose wells both fit in one window, and a time of day a lane's walker is
	//out on it at least `off` cells from its middle
	private static int[] road(){
		for (int[] r : RoadTraffic.travelPairs( SEED, -6, -6, 6, 6 )){
			if (!RoadTraffic.travelled( SEED, r[0], r[1], r[2], r[3] )) continue;
			int ax = WorldStructures.siteX( SEED, r[0], r[1] ), ay = WorldStructures.siteY( SEED, r[0], r[1] );
			int bx = WorldStructures.siteX( SEED, r[2], r[3] ), by = WorldStructures.siteY( SEED, r[2], r[3] );
			if (Math.abs( ax - bx ) < 120 && Math.abs( ay - by ) < 120 && Math.hypot( ax - bx, ay - by ) > 60) return r;
		}
		throw new AssertionError( "no road" );
	}

	@Test
	public void travellersComeOnTheClockAndNeverTwice(){
		int[] r = road();
		int mx = (WorldStructures.siteX( SEED, r[0], r[1] ) + WorldStructures.siteX( SEED, r[2], r[3] )) / 2;
		int my = (WorldStructures.siteY( SEED, r[0], r[1] ) + WorldStructures.siteY( SEED, r[2], r[3] )) / 2;
		OverworldLevel ow = windowAt( mx, my );
		int day = 10, w0 = RoadTraffic.setOut(), w1 = RoadTraffic.indoors();
		int placedAt = -1;
		for (int tod = w0; tod < w1 && placedAt == -1; tod += 37){
			Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + tod;
			call( ow, "placeTravellers", new Class<?>[]{ boolean.class }, true );
			int n = 0;
			HashSet<Long> keys = new HashSet<>();
			for (Mob m : ow.mobs){
				if (!(m instanceof Traveller)) continue;
				Traveller t = (Traveller) m;
				n++;
				assertTrue( "twice: " + t.key, keys.add( t.key ) );
				assertEquals( day, t.day );
				assertTrue( tod >= t.depart && tod < t.arrive );
				//where the clock says, or just out of his door
				float s = Math.max( 0f, Math.min( RoadTraffic.length( t.route ), (tod - t.depart) * t.pace ) );
				float[] p = RoadTraffic.point( t.route, s );
				int mark = ow.trafficCell( p[0], p[1] );
				boolean onMark = mark != -1 && ow.distance( t.pos, mark ) <= 2;
				int door = ow.localCell( Math.round( t.route[0] ), Math.round( t.route[1] ) );
				boolean outOfDoor = s <= 12 && door != -1 && ow.distance( t.pos, door ) <= 3;
				assertTrue( "traveller " + t.key + " off his mark", onMark || outOfDoor );
				//never out of thin air within the hero's sight
				assertTrue( outOfDoor || ow.distance( t.pos, hero.pos ) > 21 );
				assertTrue( ow.passable[t.pos] );
			}
			assertTrue( n <= 4 );
			if (n > 0) placedAt = tod;
			//the next look the same turn sets nobody down twice
			call( ow, "placeTravellers", new Class<?>[]{ boolean.class }, true );
			int again = 0;
			for (Mob m : ow.mobs) if (m instanceof Traveller) again++;
			assertEquals( n, again );
		}
		assertNotEquals( "nobody on the road all day", -1, placedAt );

		//the debug menu's night: nobody new sets out
		for (Mob m : ow.mobs.toArray( new Mob[0] )) if (m instanceof Traveller) OverworldLevel.vanish( ow, m );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		call( ow, "placeTravellers", new Class<?>[]{ boolean.class }, true );
		for (Mob m : ow.mobs) assertFalse( m instanceof Traveller );
		DayNightCycle.debugPhaseOverride = null;
		//and nobody is out at night
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + w1 + 10;
		call( ow, "placeTravellers", new Class<?>[]{ boolean.class }, true );
		for (Mob m : ow.mobs) assertFalse( m instanceof Traveller );
	}

	@Test
	public void aTravellerWalksTheRoadAtHisPace(){
		int[] r = road();
		int ax = WorldStructures.siteX( SEED, r[0], r[1] ), ay = WorldStructures.siteY( SEED, r[0], r[1] );
		int bx = WorldStructures.siteX( SEED, r[2], r[3] ), by = WorldStructures.siteY( SEED, r[2], r[3] );
		OverworldLevel ow = windowAt( (ax + bx) / 2, (ay + by) / 2 );
		//the hero off to one side, out of the way and seeing nothing
		int day = 10, tod = 600;
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + tod;
		RoadTraffic.Trip trip = RoadTraffic.forcedTrip( SEED, r[0], r[1], r[2], r[3], RoadTraffic.FARMER, day, tod,
				ax + (bx - ax) * 0.3f, ay + (by - ay) * 0.3f, 0, 1L );
		Traveller t = Traveller.of( trip );
		float[] p = RoadTraffic.point( trip.route, trip.distanceAt( tod ) );
		int mark = ow.trafficCell( p[0], p[1] );
		assertNotEquals( -1, mark );
		t.pos = mark;
		t.sprite = stub();
		ow.mobs.add( t );
		Actor.add( t );
		hero.pos = ow.localCell( ax + (bx - ax) / 2 + 30, ay + (by - ay) / 2 + 30 );
		if (hero.pos == -1 || ow.distance( hero.pos, t.pos ) < 25) hero.pos = 2 + 2 * W;

		boolean[] road = ow.roadMask();
		float start = RoadTraffic.project( trip.route, ow.worldX() + t.pos % W, ow.worldY() + t.pos / W );
		int steps = 0, onRoad = 0, jumps = 0, turns = 300;
		for (int turn = 1; turn <= turns && ow.mobs.contains( t ); turn++){
			Dungeon.cycleTurn++;
			while (t.cooldown() < turn && ow.mobs.contains( t )){
				float before = t.cooldown();
				int was = t.pos;
				assertTrue( act( t ) );
				//every turn of his spends time: the actor loop never spins on a walker
				assertTrue( "no time spent", t.cooldown() > before || !ow.mobs.contains( t ) );
				if (t.pos != was){
					if (ow.distance( was, t.pos ) > 1) jumps++;
					else {
						steps++;
						if (road[t.pos]) onRoad++;
					}
				}
			}
		}
		float end = RoadTraffic.project( trip.route, ow.worldX() + t.pos % W, ow.worldY() + t.pos / W );
		//where his clock had got to when he last moved, or the end of the walk
		float ghost = Math.min( RoadTraffic.length( trip.route ), trip.distanceAt( Math.min( tod + turns, trip.arrive ) ) );
		System.out.println( "[traffic] farmer walked " + steps + " steps (" + onRoad + " on road, " + jumps
				+ " set down) and got " + (end - start) + " cells on in " + turns + " turns, his clock "
				+ (ghost - start) + (ow.mobs.contains( t ) ? "" : ", and went in at the far door") );
		assertTrue( "steps " + steps, steps > 40 );
		assertTrue( "on the road " + onRoad + " of " + steps, onRoad >= steps * 0.8 );
		assertTrue( "jumps " + jumps, jumps <= 1 );
		//he keeps pace with his clock: half a cell a turn, a cell or two behind it
		assertTrue( "fell behind: " + (end - start) + " of " + (ghost - start), end >= ghost - 4f );
		assertTrue( "ran ahead: " + (end - start) + " of " + (ghost - start), end <= ghost + 1.5f );

		//his walk over and nobody watching: gone into the far house, the door left shut
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + t.arrive + 1;
		act( t );
		assertFalse( ow.mobs.contains( t ) );
		assertFalse( Actor.all().contains( t ) );
	}

	@Test
	public void theWatchCutsDownAnOutlawForNobody(){
		int[] r = road();
		int ax = WorldStructures.siteX( SEED, r[0], r[1] ), ay = WorldStructures.siteY( SEED, r[0], r[1] );
		OverworldLevel ow = windowAt( ax, ay );
		int day = 10, tod = 600;
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + tod;
		//a beat out along the road from this well, just set out
		float[] route = { ax, ay, ax + 30, ay };
		RoadPatrol g = RoadPatrol.of( new RoadTraffic.Beat( route, new int[]{ tod - 4 }, day ), WorldStructures.sectorOf( r[0], r[1] ), 0, 2 );
		int at = -1;
		for (int dx = 0; dx < 6 && at == -1; dx++){
			int cell = ow.localCell( ax + 2 + dx, ay );
			if (cell != -1 && ow.passable[cell] && !ow.occupied( cell ) && ow.passable[cell + 1] && !ow.occupied( cell + 1 )) at = cell;
		}
		assertNotEquals( -1, at );
		g.pos = at;
		g.sprite = stub();
		ow.mobs.add( g );
		Actor.add( g );
		OverworldBandit b = new OverworldBandit();
		b.pos = at + 1;
		b.sprite = stub();
		b.state = b.WANDERING;
		ow.mobs.add( b );
		Actor.add( b );
		int exp = hero.exp, slain = Statistics.enemiesSlain;
		hero.pos = ow.localCell( ax - 40, ay - 40 );
		for (int i = 0; i < 80 && b.isAlive(); i++){
			act( g );
			if (b.isAlive()) assertTrue( b.HP <= b.HT );
		}
		assertFalse( "the outlaw still stands", b.isAlive() );
		assertEquals( Char.Alignment.NEUTRAL, b.alignment );
		assertEquals( exp, hero.exp );
		assertEquals( slain, Statistics.enemiesSlain );
		assertFalse( ow.mobs.contains( b ) );
		//and the watch took nothing from it in return
		assertTrue( g.isAlive() );
		assertEquals( g.HT, g.HP );
	}

	@Test
	public void theWindowDropsWalkersButKeepsAnAmbushParty(){
		int[] r = road();
		OverworldLevel ow = windowAt( WorldStructures.siteX( SEED, r[0], r[1] ), WorldStructures.siteY( SEED, r[0], r[1] ) );
		@SuppressWarnings("unchecked") HashMap<Long, Mob> parked = (HashMap<Long, Mob>) field( ow, "parkedMobs" );
		parked.clear();
		Class<?>[] sig = { Mob.class, int.class, int.class };

		Traveller t = Traveller.of( new RoadTraffic.Trip( 5L, RoadTraffic.PEDLAR, r[0], r[1], r[2], r[3], 10, 0, 999, 0.45f, new float[]{ 0, 0, 9, 9 } ) );
		t.pos = hero.pos + 3;
		ow.mobs.add( t );
		call( ow, "park", sig, t, 1000, 1000 );
		assertFalse( ow.mobs.contains( t ) );
		assertTrue( parked.isEmpty() );

		OverworldBandit party = raider( STALL, hero.pos + 4 );
		ow.mobs.add( party );
		ow.mobs.remove( party );
		call( ow, "park", sig, party, 1001, 1000 );
		assertEquals( 1, parked.size() );
		//never pruned while its stall's day lasts, however long or far
		call( ow, "pruneParked", new Class<?>[]{ int.class, int.class }, 100_000, 100_000 );
		assertEquals( 1, parked.size() );

		OverworldBandit leaving = raider( STALL, hero.pos + 5 );
		leaving.retreating = true;
		ow.mobs.add( leaving );
		call( ow, "park", sig, leaving, 1002, 1000 );
		assertFalse( ow.mobs.contains( leaving ) );
		assertEquals( 1, parked.size() );

		//an ambush party is no camp's: a camp's census does not count it
		assertFalse( (Boolean) call( ow, "mobNear", new Class<?>[]{ Class.class, int.class, int.class },
				OverworldBandit.class, ow.localCell( 1001, 1000 ) == -1 ? hero.pos : ow.localCell( 1001, 1000 ), 3 ) );
	}

	@Test
	public void caravansCarryTheirRoadAndAmbushOncePerDay(){
		//a window over a caravan road with its stall up today and its outlaws waiting
		int dos = GameCalendar.dayOfSeason(), wd = GameCalendar.weekday().ordinal();
		int[] found = null, spot = null;
		for (int day = 10; day < 400 && found == null; day++){
			Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + 700;
			dos = GameCalendar.dayOfSeason();
			wd = GameCalendar.weekday().ordinal();
			for (int[] r : RoadTraffic.roads( SEED, -4, -4, 4, 4 )){
				int[] s = RoadTraffic.caravanSpot( SEED, r[0], r[1], r[2], r[3], wd, dos );
				if (s == null || !RoadTraffic.ambushed( SEED, r[0], r[1], r[2], r[3], WorldClock.day() )
						|| !RoadTraffic.roadNear( SEED, s[0], s[1] )) continue;
				found = r;
				spot = s;
				break;
			}
		}
		assertNotNull( found );
		OverworldLevel ow = windowAt( spot[0], spot[1] );
		call( ow, "placeCaravans", new Class<?>[0] );
		Caravaneer stall = null;
		for (Mob m : ow.mobs){
			if (m instanceof Caravaneer && ow.distance( m.pos, ow.localCell( spot[0], spot[1] ) ) <= 5) stall = (Caravaneer) m;
		}
		assertNotNull( "no stall pitched", stall );
		assertEquals( WorldClock.day(), stall.pitched );
		assertEquals( RoadTraffic.roadHash( SEED, found[0], found[1], found[2], found[3] ), stall.road );
		assertNotEquals( OverworldBandit.NO_AMBUSH, stall.stall );
		assertEquals( Caravaneer.AMBUSH_PENDING, stall.ambush );
		assertFalse( ow.caravanAtPeace( spot[0], spot[1] ) );

		//fought out, and its keeper chased off: pitched again the same day, nobody waits for it
		stall.ambush = Caravaneer.AMBUSH_BESIEGED;
		ow.mobs.add( raider( stall.stall, -5 ) );   //a raider off in the trees, so the siege holds
		for (Mob m : ow.mobs.toArray( new Mob[0] )) if (m instanceof OverworldBandit) ow.mobs.remove( m );
		hero.pos = stall.pos + 2;
		Actor.add( stall );
		ow.settleSiege( stall );
		assertEquals( Caravaneer.AMBUSH_SAVED, stall.ambush );
		assertTrue( ow.caravanAtPeace( spot[0], spot[1] ) );
		OverworldLevel.vanish( ow, stall );
		call( ow, "placeCaravans", new Class<?>[0] );
		Caravaneer again = null;
		for (Mob m : ow.mobs) if (m instanceof Caravaneer && m != stall && m.pos == stall.pos) again = (Caravaneer) m;
		assertNotNull( again );
		assertEquals( Caravaneer.AMBUSH_NONE, again.ambush );

		//the fought-out roads go into the save with their day
		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		assertEquals( WorldClock.day(), b.getInt( "ambush_done_day" ) );
		long[] fought = b.getLongArray( "ambush_done" );
		assertEquals( 1, fought.length );
		assertEquals( again.road, fought[0] );
	}

	private static Caravaneer stallOf( OverworldLevel ow, long road ){
		Caravaneer found = null;
		for (Mob m : ow.mobs){
			if (m instanceof Caravaneer && ((Caravaneer) m).road == road){
				assertNull( "two stalls on one road", found );
				found = (Caravaneer) m;
			}
		}
		return found;
	}

	private static ArrayList<CaravanTrain> cartsOf( OverworldLevel ow, long road ){
		ArrayList<CaravanTrain> out = new ArrayList<>();
		for (Mob m : ow.mobs) if (m instanceof CaravanTrain && ((CaravanTrain) m).road == road) out.add( (CaravanTrain) m );
		return out;
	}

	//the caravan's morning leg on a real window: no stall while its cart is on the way; the cart
	//set down where its clock says, and never twice; it drives the road to its spot, every turn
	//spending time, and the leg over it pitches the stall there itself - one stall, its road's and
	//today's, its outlaws rolled - and neither the traffic's next look nor a new window's pitch
	//puts up a second. The cart comes back from a save, and an older save's fields fall back
	@Test
	public void aCaravansCartDrivesItsLegAndPitchesItsStallOnce(){
		int[] found = null;
		RoadTraffic.Trip leg = null;
		int day = -1;
		for (int d = 10; d < 200 && found == null; d++){
			Dungeon.cycleTurn = d * DayNightCycle.FULL_CYCLE;
			int dos = GameCalendar.dayOfSeason(), wd = GameCalendar.weekday().ordinal();
			for (int[] r : RoadTraffic.roads( SEED, -4, -4, 4, 4 )){
				RoadTraffic.Trip l = RoadTraffic.caravanLeg( SEED, r[0], r[1], r[2], r[3], wd, dos, d, RoadTraffic.setOut() );
				if (l == null || RoadTraffic.length( l.route ) < 10
						|| !RoadTraffic.roadNear( SEED, Math.round( l.route[0] ), Math.round( l.route[1] ) )
						|| !RoadTraffic.roadNear( SEED, Math.round( l.route[2] ), Math.round( l.route[3] ) )) continue;
				found = r;
				leg = l;
				day = d;
				break;
			}
		}
		assertNotNull( "no caravan leg in 190 days", found );
		long road = RoadTraffic.roadHash( SEED, found[0], found[1], found[2], found[3] );
		int mid = leg.depart + (leg.arrive - leg.depart) / 3;
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + mid;
		int weekday = GameCalendar.weekday().ordinal();
		OverworldLevel ow = windowAt( Math.round( leg.route[2] ), Math.round( leg.route[3] ) );
		int spot = ow.localCell( Math.round( leg.route[2] ), Math.round( leg.route[3] ) );
		//the hero far off, seeing nothing
		hero.pos = -1;
		for (int y = 4; y < H - 4 && hero.pos == -1; y++){
			for (int x = 4; x < W - 4 && hero.pos == -1; x++){
				int c = x + y * W;
				if (ow.passable[c] && !ow.occupied( c ) && ow.distance( c, spot ) > 50) hero.pos = c;
			}
		}
		assertNotEquals( -1, hero.pos );

		//no stall while the cart is on its way, by a new window's pitch or the traffic's look
		call( ow, "placeCaravans", new Class<?>[0] );
		assertNull( stallOf( ow, road ) );
		call( ow, "pitchCaravans", new Class<?>[]{ boolean.class }, true );
		assertNull( stallOf( ow, road ) );

		//the cart, where its clock says, and only once
		call( ow, "placeCaravanTrains", new Class<?>[]{ boolean.class }, true );
		ArrayList<CaravanTrain> carts = cartsOf( ow, road );
		assertEquals( 1, carts.size() );
		CaravanTrain cart = carts.get( 0 );
		assertEquals( CaravanTrainSprite.class, cart.spriteClass );
		assertEquals( day, cart.day );
		assertEquals( leg.depart, cart.depart );
		assertEquals( leg.arrive, cart.arrive );
		float[] p = RoadTraffic.point( leg.route, leg.distanceAt( mid ) );
		int mark = ow.trafficCell( p[0], p[1] );
		assertNotEquals( -1, mark );
		assertTrue( "off its mark", ow.distance( cart.pos, mark ) <= 2 );
		call( ow, "placeCaravanTrains", new Class<?>[]{ boolean.class }, true );
		assertEquals( 1, cartsOf( ow, road ).size() );

		//on to its spot, and the stall goes up when the cart is in - not before
		cart.sprite = stub();
		Actor.add( cart );
		int turns = leg.arrive - mid + 60, steps = 0, jumps = 0;
		boolean[] roadCells = ow.roadMask();
		int onRoad = 0;
		for (int turn = 1; turn <= turns && ow.mobs.contains( cart ); turn++){
			Dungeon.cycleTurn++;
			while (cart.cooldown() < turn && ow.mobs.contains( cart )){
				float before = cart.cooldown();
				int was = cart.pos;
				assertTrue( act( cart ) );
				assertTrue( "no time spent", cart.cooldown() > before || !ow.mobs.contains( cart ) );
				if (ow.mobs.contains( cart ) && cart.pos != was){
					if (ow.distance( was, cart.pos ) > 1) jumps++;
					else {
						steps++;
						if (roadCells[cart.pos]) onRoad++;
					}
				}
			}
			if (ow.mobs.contains( cart )) assertNull( "a stall before its cart came in", stallOf( ow, road ) );
		}
		System.out.println( "[traffic] the cart drove " + steps + " steps (" + onRoad + " on road, " + jumps
				+ " set down) of a " + RoadTraffic.length( leg.route ) + "-cell leg" );
		assertTrue( "steps " + steps, steps >= 3 );
		assertTrue( "on the road " + onRoad + " of " + steps, onRoad >= steps * 0.8 );
		assertTrue( "jumps " + jumps, jumps <= 1 );
		assertFalse( "the cart never pulled up", ow.mobs.contains( cart ) );
		assertFalse( Actor.all().contains( cart ) );
		assertTrue( RoadTraffic.turnOfDay( Dungeon.cycleTurn ) >= leg.arrive );
		Caravaneer stall = stallOf( ow, road );
		assertNotNull( "no stall pitched", stall );
		assertTrue( "the stall went up " + ow.distance( stall.pos, spot ) + " cells off its spot", ow.distance( stall.pos, spot ) <= 5 );
		assertEquals( WorldClock.day(), stall.pitched );
		assertEquals( weekday, stall.day );
		assertEquals( RoadTraffic.ambushed( SEED, found[0], found[1], found[2], found[3], WorldClock.day() )
				? Caravaneer.AMBUSH_PENDING : Caravaneer.AMBUSH_NONE, stall.ambush );

		//and never a second: the next look, a new window's pitch, the next look for carts
		call( ow, "pitchCaravans", new Class<?>[]{ boolean.class }, true );
		call( ow, "placeCaravans", new Class<?>[0] );
		call( ow, "placeCaravanTrains", new Class<?>[]{ boolean.class }, true );
		assertSame( stall, stallOf( ow, road ) );
		assertTrue( cartsOf( ow, road ).isEmpty() );

		//its keeper chased off: the traffic's look does not put him straight back up
		OverworldLevel.vanish( ow, stall );
		call( ow, "pitchCaravans", new Class<?>[]{ boolean.class }, true );
		assertNull( stallOf( ow, road ) );

		//a stall a save brought back before its cart's hour (the ledger of stalls up is not
		//saved): the new day's first pitch finds it, and no cart sets out for it
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + mid;
		OverworldLevel loadedWindow = windowAt( Math.round( leg.route[2] ), Math.round( leg.route[3] ) );
		hero.pos = -1;
		for (int y = 4; y < H - 4 && hero.pos == -1; y++){
			for (int x = 4; x < W - 4 && hero.pos == -1; x++){
				int c = x + y * W;
				if (loadedWindow.passable[c] && !loadedWindow.occupied( c ) && loadedWindow.distance( c, spot ) > 50) hero.pos = c;
			}
		}
		Caravaneer loaded = new Caravaneer();
		loaded.road = road;
		loaded.day = weekday;
		loaded.pitched = WorldClock.day();
		loaded.pos = loadedWindow.freeSpotWithin( loadedWindow.localCell( Math.round( leg.route[2] ), Math.round( leg.route[3] ) ), 3 );
		assertNotEquals( -1, loaded.pos );
		loadedWindow.mobs.add( loaded );
		call( loadedWindow, "placeCaravans", new Class<?>[0] );
		call( loadedWindow, "placeCaravanTrains", new Class<?>[]{ boolean.class }, true );
		assertSame( loaded, stallOf( loadedWindow, road ) );
		assertTrue( "a cart set out for a stall already up", cartsOf( loadedWindow, road ).isEmpty() );

		//a save keeps the cart's leg; an older one's missing fields fall back
		Bundle b = new Bundle();
		cart.storeInBundle( b );
		CaravanTrain back = new CaravanTrain();
		back.restoreFromBundle( b );
		assertEquals( road, back.road );
		assertEquals( cart.fromSx, back.fromSx );
		assertEquals( cart.fromSy, back.fromSy );
		assertEquals( cart.toSx, back.toSx );
		assertEquals( cart.toSy, back.toSy );
		assertEquals( leg.depart, back.depart );
		assertEquals( leg.arrive, back.arrive );
		assertEquals( leg.pace, back.pace, 0f );
		assertEquals( day, back.day );
		assertArrayEquals( leg.route, back.route, 0f );
		for (String k : new String[]{ "road", "from_x", "from_y", "to_x", "to_y", "depart", "arrive", "pace" }) assertTrue( k, b.remove( k ) );
		CaravanTrain old = new CaravanTrain();
		old.restoreFromBundle( b );
		assertEquals( Long.MIN_VALUE, old.road );
		assertEquals( 0, old.arrive );
		assertEquals( RoadTraffic.CARAVAN_PACE, old.pace, 0f );
		for (String k : new String[]{ "name", "desc", "line" }){
			assertFalse( k, Messages.get( CaravanTrain.class, k, "A", "B" ).contains( Messages.NO_TEXT_FOUND ) );
		}
	}

	@Test
	public void theSurfaceKeeperTakesOnlyHisOwnShelf(){
		int[] r = road();
		OverworldLevel ow = windowAt( WorldStructures.siteX( SEED, r[0], r[1] ) + 20, WorldStructures.siteY( SEED, r[0], r[1] ) );
		int a = hero.pos + 4 * W, bpos = hero.pos + 4 * W + 7;
		Caravaneer one = new Caravaneer(), two = new Caravaneer();
		one.pos = a;
		two.pos = bpos;
		for (Caravaneer k : new Caravaneer[]{ one, two }){
			ow.mobs.add( k );
			Actor.add( k );
			for (int n : new int[]{ -1, 1 }){
				Heap h = ow.drop( new Food(), k.pos + n );
				h.type = Heap.Type.FOR_SALE;
			}
		}
		one.destroy();
		assertNull( ow.heaps.get( a - 1 ) );
		assertNull( ow.heaps.get( a + 1 ) );
		assertNotNull( ow.heaps.get( bpos - 1 ) );
		assertNotNull( ow.heaps.get( bpos + 1 ) );
		assertEquals( Heap.Type.FOR_SALE, ow.heaps.get( bpos - 1 ).type );
	}
}

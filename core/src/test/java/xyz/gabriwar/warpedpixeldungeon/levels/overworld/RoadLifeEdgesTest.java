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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Caravaneer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RoadPatrol;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Traveller;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The road's life at its edges, as a verifier saw them: every new line resolving, a
 * traveller naming the real villages of his walk, a siege outlasting the day (robbed, struck,
 * its raiders swept) and the window sliding off it and back (nothing doubled, nothing lost,
 * nothing sprung twice), the watch indoors by night and never twice on its beat, never
 * raising a hand to the hero or the folk while the outlaws do turn on it, a bandit's blow
 * never sending the keeper packing, the rescued stall's discount on the host's own purchase
 * path, the surface's ambush ledger surviving a save and an older save loading without it,
 * and a real-clock day's traffic look staying cheap.
 */
public class RoadLifeEdgesTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int turn, startDay, challenges, depth, branch, gold;
	private DayNightCycle.Phase override;
	private Level saved;
	private Hero savedHero;

	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Generator.fullReset();
		xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll.initLabels();
		xyz.gabriwar.warpedpixeldungeon.items.potions.Potion.initColors();
		xyz.gabriwar.warpedpixeldungeon.items.rings.Ring.initGems();
		//identifying a bought or gifted item reaches the badges
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
		hero.HP = hero.HT = 40;
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
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	private static Level bareLevel( int w, int h ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
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
		Painter.fill( l, 1, 1, w - 2, h - 2, Terrain.EMPTY );
		l.buildFlagMaps();
		Arrays.fill( l.heroFOV, true );
		return l;
	}

	//a sprite that draws nothing
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

	private static Object call( Object o, String name, Class<?>[] types, Object... args ){
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( o, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	private static Object field( Object o, Class<?> cls, String name ){
		try {
			Field f = cls.getDeclaredField( name );
			f.setAccessible( true );
			return f.get( o );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static void setField( Object o, Class<?> cls, String name, Object v ){
		try {
			Field f = cls.getDeclaredField( name );
			f.setAccessible( true );
			f.set( o, v );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	@SuppressWarnings("unchecked")
	private static HashMap<Long, Mob> parked( OverworldLevel ow ){
		return (HashMap<Long, Mob>) field( ow, OverworldLevel.class, "parkedMobs" );
	}

	private static void park( OverworldLevel ow, Mob m ){
		int wx = ow.worldX() + m.pos % W, wy = ow.worldY() + m.pos / W;
		ow.mobs.remove( m );
		call( ow, "park", new Class<?>[]{ Mob.class, int.class, int.class }, m, wx, wy );
	}

	//the live surface window round a world cell (as RoadLifeTest builds it): a mirror's window
	//made the host's own, the hero on the nearest open cell, seeing nothing
	private OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		int ox = wx - W / 2, oy = wy - H / 2;
		float shift = WorldModel.calendarShift();
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, shift );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, shift, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		setField( ow, OverworldLevel.class, "network", false );
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

	//a travelled road whose wells both fit in one window
	private static int[] road(){
		for (int[] r : RoadTraffic.travelPairs( SEED, -6, -6, 6, 6 )){
			if (!RoadTraffic.travelled( SEED, r[0], r[1], r[2], r[3] )) continue;
			int ax = WorldStructures.siteX( SEED, r[0], r[1] ), ay = WorldStructures.siteY( SEED, r[0], r[1] );
			int bx = WorldStructures.siteX( SEED, r[2], r[3] ), by = WorldStructures.siteY( SEED, r[2], r[3] );
			if (Math.abs( ax - bx ) < 120 && Math.abs( ay - by ) < 120 && Math.hypot( ax - bx, ay - by ) > 60) return r;
		}
		throw new AssertionError( "no road" );
	}

	//a window over a stall that stands today with its outlaws waiting, the clock walked to that
	//day's morning: {window-ready world x, y} of the spot, its road in found[0]
	private int[] ambushedStallSpot( int[][] roadOut ){
		for (int day = 10; day < 400; day++){
			Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + 700;
			int dos = GameCalendar.dayOfSeason(), wd = GameCalendar.weekday().ordinal();
			for (int[] r : RoadTraffic.roads( SEED, -4, -4, 4, 4 )){
				int[] s = RoadTraffic.caravanSpot( SEED, r[0], r[1], r[2], r[3], wd, dos );
				if (s == null || !RoadTraffic.ambushed( SEED, r[0], r[1], r[2], r[3], WorldClock.day() )
						|| !RoadTraffic.roadNear( SEED, s[0], s[1] )) continue;
				roadOut[0] = r;
				return s;
			}
		}
		throw new AssertionError( "no ambushed stall in 400 days" );
	}

	//the stall placeCaravans pitched at (or beside) a world spot, sprung on by its outlaws with
	//the hero eight cells off and every cell in sight
	private Caravaneer springAt( OverworldLevel ow, int[] spot ){
		call( ow, "placeCaravans", new Class<?>[0] );
		Caravaneer stall = null;
		for (Mob m : ow.mobs){
			if (m instanceof Caravaneer && ow.distance( m.pos, ow.localCell( spot[0], spot[1] ) ) <= 5) stall = (Caravaneer) m;
		}
		assertNotNull( "no stall pitched", stall );
		assertEquals( Caravaneer.AMBUSH_PENDING, stall.ambush );
		hero.pos = -1;
		for (int r = 8; r <= 12 && hero.pos == -1; r++){
			for (int ofs : PathFinder.NEIGHBOURS8){
				int cell = stall.pos + ofs * r;
				if (cell > 0 && cell < ow.length() && ow.passable[cell] && !ow.occupied( cell ) && ow.distance( cell, stall.pos ) == r){
					hero.pos = cell;
					break;
				}
			}
		}
		assertNotEquals( -1, hero.pos );
		Arrays.fill( ow.heroFOV, true );
		assertTrue( CaravanAmbush.due( ow, stall ) );
		int n = CaravanAmbush.spring( ow, stall );
		assertTrue( "sprang " + n, n >= 2 );
		assertEquals( Caravaneer.AMBUSH_BESIEGED, stall.ambush );
		return stall;
	}

	private static ArrayList<OverworldBandit> raiders( java.util.Collection<Mob> mobs, long stall ){
		ArrayList<OverworldBandit> out = new ArrayList<>();
		for (Mob m : mobs){
			if (m instanceof OverworldBandit && ((OverworldBandit) m).ambushOf == stall) out.add( (OverworldBandit) m );
		}
		return out;
	}

	// ------------------------------------------------------------------ strings

	private static void resolves( String s ){
		assertNotNull( s );
		assertFalse( s, s.contains( Messages.NO_TEXT_FOUND ) );
		assertFalse( s, s.trim().isEmpty() );
	}

	@Test
	public void everyNewLineResolves(){
		for (int k = 0; k < RoadTraffic.KINDS; k++){
			resolves( Messages.get( Traveller.class, "name_" + k ) );
			resolves( Messages.get( Traveller.class, "desc_" + k ) );
			String line = Messages.get( Traveller.class, "line_" + k, "Toward", "Fromby" );
			resolves( line );
			assertTrue( line, line.contains( "Toward" ) );
			assertTrue( line, line.contains( "Fromby" ) );
		}
		for (String r : new String[]{ "rumour_dragon", "rumour_ruin", "rumour_outlaws", "rumour_caravan", "rumour_watched" }){
			String s = Messages.get( Traveller.class, r, "FARAWAY", "SOMEWHERE" );
			resolves( s );
			assertTrue( s, s.contains( "FARAWAY" ) && s.contains( "SOMEWHERE" ) );
		}
		for (String d : new String[]{ "dist_near", "dist_half", "dist_far" }) resolves( Messages.get( Traveller.class, d ) );
		for (int d = 0; d < 8; d++) resolves( Messages.get( Traveller.class, "dir_" + d ) );
		for (String k : new String[]{ "name", "desc", "def_verb", "line_0", "line_1", "line_2", "spotted" }){
			resolves( Messages.get( RoadPatrol.class, k ) );
		}
		for (String k : new String[]{ "def_verb", "ambush_yell", "ambush_log", "talk_besieged", "saved_thanks", "saved_log",
				"talk_saved", "robbed_log", "talk_robbed", "talk_spared" }){
			resolves( Messages.get( Caravaneer.class, k ) );
		}
		//and the names and words the mobs themselves give
		for (int k = 0; k < RoadTraffic.KINDS; k++){
			Traveller t = Traveller.of( new RoadTraffic.Trip( 8L, k, 0, 0, 1, 1, 10, 0, 99, RoadTraffic.PACE[k], new float[]{ 0, 0, 5, 5 } ) );
			resolves( t.name() );
			resolves( t.description() );
		}
		RoadPatrol p = new RoadPatrol();
		resolves( p.name() );
		resolves( p.description() );
		resolves( p.defenseVerb() );
		resolves( new Caravaneer().defenseVerb() );
	}

	//what a traveller on a real road says: where he is bound and from, by the villages' own
	//names, and - for every other one - a rumour that reads as a whole sentence
	@Test
	public void aTravellerNamesTheVillagesOfHisWalk() throws Exception {
		int[] r = road();
		int ax = WorldStructures.siteX( SEED, r[0], r[1] ), ay = WorldStructures.siteY( SEED, r[0], r[1] );
		int bx = WorldStructures.siteX( SEED, r[2], r[3] ), by = WorldStructures.siteY( SEED, r[2], r[3] );
		OverworldLevel ow = windowAt( (ax + bx) / 2, (ay + by) / 2 );
		Method talk = Traveller.class.getDeclaredMethod( "talk", OverworldLevel.class );
		talk.setAccessible( true );
		String to = WorldStructures.villageName( SEED, r[2], r[3] ), from = WorldStructures.villageName( SEED, r[0], r[1] );
		int rumours = 0;
		for (long key = 0; key < 64; key++){
			int kind = (int)(key % RoadTraffic.KINDS);
			Traveller t = Traveller.of( new RoadTraffic.Trip( key, kind, r[0], r[1], r[2], r[3], 10, 0, 999,
					RoadTraffic.PACE[kind], RoadTraffic.travellerRoute( SEED, r[0], r[1], r[2], r[3], 0, 0 ) ) );
			t.pos = hero.pos;
			String s = (String) talk.invoke( t, ow );
			resolves( s );
			String line = Messages.get( Traveller.class, "line_" + kind, to, from );
			assertTrue( s, s.startsWith( line ) );
			if (((key >>> 3) & 1L) != 0){
				assertEquals( "only every other one has news", line, s );
			} else if (!s.equals( line )){
				rumours++;
				String news = s.substring( line.length() );
				assertTrue( news, news.startsWith( "\n\n" ) && news.length() > 10 );
			}
		}
		assertTrue( "no traveller had any news on this road", rumours > 0 );
	}

	// ------------------------------------------------------------ the ambush

	//the day turns on a siege still on: the outlaws had the goods first (ROBBED, half the shelf
	//gone), the stall is struck, and its raiders are nobody's party any more - none left tagged,
	//live or parked, so nothing keeps them forever or counts them for a siege that is over
	@Test
	public void theDayTurningOnASiegeRobsStrikesAndSweepsIt(){
		int[][] found = new int[1][];
		int[] spot = ambushedStallSpot( found );
		OverworldLevel ow = windowAt( spot[0], spot[1] );
		Caravaneer stall = springAt( ow, spot );
		long tag = stall.stall;
		int n = raiders( ow.mobs, tag ).size();
		//one of them ran off past the window's edge and sits in the store
		OverworldBandit away = raiders( ow.mobs, tag ).get( 0 );
		park( ow, away );
		assertEquals( 1, raiders( parked( ow ).values(), tag ).size() );
		int shelf = stall.pos;

		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE;
		call( ow, "placeCaravans", new Class<?>[0] );

		assertEquals( Caravaneer.AMBUSH_ROBBED, stall.ambush );
		assertFalse( "the stall stands on", ow.mobs.contains( stall ) );
		for (int ofs : PathFinder.NEIGHBOURS9){
			Heap h = ow.heaps.get( shelf + ofs );
			assertTrue( "goods left for sale by a struck stall", h == null || h.type != Heap.Type.FOR_SALE );
		}
		assertTrue( "raiders still tagged in the window", raiders( ow.mobs, tag ).isEmpty() );
		assertTrue( "raiders still tagged in the store", raiders( parked( ow ).values(), tag ).isEmpty() );
		//whoever stayed on is an ordinary outlaw now, and the camps' census counts him again
		for (Mob m : ow.mobs){
			if (!(m instanceof OverworldBandit)) continue;
			assertFalse( ((OverworldBandit) m).sentOnEvent() );
			assertFalse( ((OverworldBandit) m).menace );
		}
		assertTrue( n >= 2 );
		//today's stalls are today's
		for (Mob m : ow.mobs){
			if (m instanceof Caravaneer){
				assertEquals( WorldClock.day(), ((Caravaneer) m).pitched );
				assertNotEquals( tag, ((Caravaneer) m).stall );
			}
		}
	}

	//the window slides off a siege and back (as a rebase parks and unparks them): nothing is
	//doubled or lost, no second stall goes up on the road meanwhile, the stall is still under
	//siege, and nothing springs twice
	@Test
	public void aSiegeOutlivesTheWindowSlidingOffAndBack(){
		int[][] found = new int[1][];
		int[] spot = ambushedStallSpot( found );
		OverworldLevel ow = windowAt( spot[0], spot[1] );
		Caravaneer stall = springAt( ow, spot );
		long tag = stall.stall;
		ArrayList<OverworldBandit> party = raiders( ow.mobs, tag );
		int n = party.size();

		park( ow, stall );
		for (OverworldBandit b : party) park( ow, b );
		assertTrue( raiders( ow.mobs, tag ).isEmpty() );
		assertEquals( n, raiders( parked( ow ).values(), tag ).size() );
		//however long and far the hero is gone the same day, the store keeps them
		call( ow, "pruneParked", new Class<?>[]{ int.class, int.class }, 1_000_000, 1_000_000 );
		assertEquals( n, raiders( parked( ow ).values(), tag ).size() );
		assertTrue( parked( ow ).containsValue( stall ) );

		//the rebase's populate pitches nothing new on that road while its stall is parked
		call( ow, "placeCaravans", new Class<?>[0] );
		for (Mob m : ow.mobs){
			assertFalse( "a second stall on the road", m instanceof Caravaneer
					&& ow.distance( m.pos, ow.localCell( spot[0], spot[1] ) ) <= 10 );
		}

		call( ow, "unparkMobs", new Class<?>[0] );
		assertTrue( ow.mobs.contains( stall ) );
		int stalls = 0;
		for (Mob m : ow.mobs) if (m instanceof Caravaneer && ((Caravaneer) m).stall == tag) stalls++;
		assertEquals( 1, stalls );
		assertEquals( n, raiders( ow.mobs, tag ).size() );
		assertTrue( raiders( parked( ow ).values(), tag ).isEmpty() );
		assertEquals( Caravaneer.AMBUSH_BESIEGED, stall.ambush );
		assertFalse( CaravanAmbush.due( ow, stall ) );
		assertEquals( 0, CaravanAmbush.spring( ow, stall ) );
		assertEquals( n, raiders( ow.mobs, tag ).size() );
		//and a populate after the unpark adds nothing either
		call( ow, "placeCaravans", new Class<?>[0] );
		stalls = 0;
		for (Mob m : ow.mobs) if (m instanceof Caravaneer && ow.distance( m.pos, stall.pos ) <= 10) stalls++;
		assertEquals( 1, stalls );
	}

	//the stall watches for the heroes on its own turn: a hero comes in sight of it while nobody
	//steps (the host resting, a co-op guest walking up) and the outlaws break cover all the same;
	//a network mirror never springs one, the host does
	@Test
	public void theStallSpringsItsOutlawsOnItsOwnTurn(){
		int[][] found = new int[1][];
		int[] spot = ambushedStallSpot( found );
		OverworldLevel ow = windowAt( spot[0], spot[1] );
		call( ow, "placeCaravans", new Class<?>[0] );
		Caravaneer stall = null;
		for (Mob m : ow.mobs){
			if (m instanceof Caravaneer && ow.distance( m.pos, ow.localCell( spot[0], spot[1] ) ) <= 5) stall = (Caravaneer) m;
		}
		assertNotNull( "no stall pitched", stall );
		assertEquals( Caravaneer.AMBUSH_PENDING, stall.ambush );
		stall.sprite = stub();
		Actor.add( stall );
		hero.pos = -1;
		for (int r = 8; r <= 12 && hero.pos == -1; r++){
			for (int ofs : PathFinder.NEIGHBOURS8){
				int cell = stall.pos + ofs * r;
				if (cell > 0 && cell < ow.length() && ow.passable[cell] && !ow.occupied( cell ) && ow.distance( cell, stall.pos ) == r){
					hero.pos = cell;
					break;
				}
			}
		}
		assertNotEquals( -1, hero.pos );

		//nobody sees the stall yet: its turn passes quietly, and spends
		float before = stall.cooldown();
		assertTrue( act( stall ) );
		assertTrue( stall.cooldown() > before );
		assertEquals( Caravaneer.AMBUSH_PENDING, stall.ambush );

		//a mirror of the host's window: the stall's turn springs nothing there
		Arrays.fill( ow.heroFOV, true );
		setField( ow, OverworldLevel.class, "network", true );
		act( stall );
		assertEquals( Caravaneer.AMBUSH_PENDING, stall.ambush );
		assertTrue( raiders( ow.mobs, stall.stall ).isEmpty() );
		setField( ow, OverworldLevel.class, "network", false );

		//in sight now, with no hero step taken: the stall's own turn springs it
		before = stall.cooldown();
		assertTrue( act( stall ) );
		assertTrue( stall.cooldown() > before );
		assertEquals( Caravaneer.AMBUSH_BESIEGED, stall.ambush );
		int n = raiders( ow.mobs, stall.stall ).size();
		assertTrue( "sprang " + n, n >= 2 && n <= 4 );
		//and never twice: its next turns settle the siege instead
		act( stall );
		act( stall );
		assertEquals( Caravaneer.AMBUSH_BESIEGED, stall.ambush );
		assertEquals( n, raiders( ow.mobs, stall.stall ).size() );
	}

	//a bandit's blow (or one landing while the siege is on, from anyone) never warns or chases
	//off the keeper: the old Shopkeeper harm path stays shut
	@Test
	public void noBlowInTheSiegeSendsTheKeeperPacking(){
		Level level = bareLevel( 40, 40 );
		Dungeon.level = level;
		hero.pos = 5 + 5 * 40;
		Caravaneer c = new Caravaneer();
		c.pos = 20 + 20 * 40;
		c.sprite = stub();
		level.mobs.add( c );
		Actor.add( c );
		OverworldBandit b = new OverworldBandit();
		b.pos = 21 + 20 * 40;
		b.sprite = stub();
		level.mobs.add( b );
		Actor.add( b );

		c.ambush = Caravaneer.AMBUSH_PENDING;
		for (int i = 0; i < 3; i++) c.damage( 5, b );
		assertEquals( -1, field( c, Shopkeeper.class, "turnsSinceHarmed" ) );
		for (int i = 0; i < 20; i++) assertFalse( "a bandit's blow landed on the keeper", b.attack( c ) );
		assertEquals( -1, field( c, Shopkeeper.class, "turnsSinceHarmed" ) );
		assertTrue( level.mobs.contains( c ) );

		c.ambush = Caravaneer.AMBUSH_BESIEGED;
		c.damage( 5, hero );
		c.damage( 5, hero );
		assertEquals( -1, field( c, Shopkeeper.class, "turnsSinceHarmed" ) );
		assertTrue( level.mobs.contains( c ) );

		//outside a siege the hero's own harm still warns him, as it always has
		c.ambush = Caravaneer.AMBUSH_SAVED;
		c.damage( 5, hero );
		assertEquals( 0, field( c, Shopkeeper.class, "turnsSinceHarmed" ) );
	}

	//the host's own purchase path (a co-op client's confirmed buy) charges the rescued keeper's
	//marked-down price, and refuses through a siege
	@Test
	public void theHostsPurchaseChargesTheRescuedPrice(){
		Level level = bareLevel( 40, 40 );
		Dungeon.level = level;
		Caravaneer c = new Caravaneer();
		c.pos = 20 + 20 * 40;
		c.sprite = stub();
		c.pitched = WorldClock.day();
		level.mobs.add( c );
		Actor.add( c );
		int shelf = 20 + 19 * 40;
		Food food = new Food();
		level.drop( food, shelf ).type = Heap.Type.FOR_SALE;
		hero.pos = 20 + 18 * 40;
		int full = Shopkeeper.sellPrice( food, hero );
		assertTrue( full > 1 );

		c.ambush = Caravaneer.AMBUSH_BESIEGED;
		Dungeon.gold = 1000;
		Shopkeeper.hostBuyFromHeap( hero, shelf );
		assertEquals( "sold through a siege", 1000, Dungeon.gold );
		assertNotNull( level.heaps.get( shelf ) );

		c.ambush = Caravaneer.AMBUSH_SAVED;
		Shopkeeper.hostBuyFromHeap( hero, shelf );
		assertEquals( Shopkeeper.discounted( full, Caravaneer.SAVED_DISCOUNT ), 1000 - Dungeon.gold );
		assertTrue( 1000 - Dungeon.gold < full );
	}

	// ------------------------------------------------------------ the watch

	//the town nearest the origin that keeps a watch, and a turn of day 10 its beat is half way out
	private static int[] watchTown(){
		for (int r = 0; r <= 8; r++){
			for (int sy = -r; sy <= r; sy++){
				for (int sx = -r; sx <= r; sx++){
					if (Math.max( Math.abs( sx ), Math.abs( sy ) ) != r) continue;
					if (RoadTraffic.patrolTown( SEED, sx, sy )) return new int[]{ sx, sy };
				}
			}
		}
		throw new AssertionError( "no town with a watch" );
	}

	//a window cell at least `far` from every point of the route
	private static int farFrom( OverworldLevel ow, float[] route, int far ){
		float len = RoadTraffic.length( route );
		for (int y = 10; y < H - 10; y++){
			for (int x = 10; x < W - 10; x++){
				int cell = x + y * W;
				if (!ow.passable[cell] || ow.occupied( cell )) continue;
				boolean ok = true;
				for (float s = 0; s <= len && ok; s += 1f){
					float[] p = RoadTraffic.point( route, s );
					ok = Math.max( Math.abs( p[0] - ow.worldX() - x ), Math.abs( p[1] - ow.worldY() - y ) ) >= far;
				}
				if (ok) return cell;
			}
		}
		return -1;
	}

	private static ArrayList<RoadPatrol> watch( OverworldLevel ow, long home ){
		ArrayList<RoadPatrol> out = new ArrayList<>();
		for (Mob m : ow.mobs) if (m instanceof RoadPatrol && ((RoadPatrol) m).homeSector == home) out.add( (RoadPatrol) m );
		return out;
	}

	//out on its beat by day as a pair, never twice; nobody of the watch out at night or under the
	//debug menu's pinned night; one still out when the day ends goes in at its door
	@Test
	public void theWatchWalksByDayAsAPairAndGoesInByNight(){
		int[] town = watchTown();
		long home = WorldStructures.sectorOf( town[0], town[1] );
		int day = 10, w0 = RoadTraffic.setOut(), w1 = RoadTraffic.indoors();
		RoadTraffic.Beat beat = RoadTraffic.beat( SEED, town[0], town[1], day, w0, w1 );
		assertNotNull( beat );
		int walk = (int) Math.ceil( RoadTraffic.length( beat.route ) / RoadTraffic.Beat.PACE );
		int tod = beat.starts[0] + walk / 2;
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + tod;
		OverworldLevel ow = windowAt( WorldStructures.siteX( SEED, town[0], town[1] ), WorldStructures.siteY( SEED, town[0], town[1] ) );
		hero.pos = farFrom( ow, beat.route, 30 );
		assertNotEquals( -1, hero.pos );

		call( ow, "placePatrols", new Class<?>[]{ boolean.class }, true );
		ArrayList<RoadPatrol> pair = watch( ow, home );
		assertEquals( "the pair on the beat", 2, pair.size() );
		assertNotEquals( pair.get( 0 ).rank, pair.get( 1 ).rank );
		int all = 0;
		for (Mob m : ow.mobs) if (m instanceof RoadPatrol) all++;
		call( ow, "placePatrols", new Class<?>[]{ boolean.class }, true );
		int again = 0;
		for (Mob m : ow.mobs) if (m instanceof RoadPatrol) again++;
		assertEquals( "set down twice", all, again );
		assertTrue( all <= 6 );

		//the debug menu's night: nobody goes out
		for (Mob m : ow.mobs.toArray( new Mob[0] )) if (m instanceof RoadPatrol) OverworldLevel.vanish( ow, m );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		call( ow, "placePatrols", new Class<?>[]{ boolean.class }, true );
		for (Mob m : ow.mobs) assertFalse( m instanceof RoadPatrol );
		DayNightCycle.debugPhaseOverride = null;
		//nor after dusk
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + w1 + 10;
		call( ow, "placePatrols", new Class<?>[]{ boolean.class }, true );
		for (Mob m : ow.mobs) assertFalse( m instanceof RoadPatrol );

		//one still out as the day ends, nobody watching: in at his door, every turn spending time
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + tod;
		RoadPatrol g = RoadPatrol.of( beat, home, 0, 1 );
		float[] p = RoadTraffic.point( beat.route, beat.distanceAt( tod, 0 ) );
		int at = ow.trafficCell( p[0], p[1] );
		assertNotEquals( -1, at );
		g.pos = at;
		g.sprite = stub();
		ow.mobs.add( g );
		Actor.add( g );
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + w1 + 10;
		for (int i = 0; i < 5 && ow.mobs.contains( g ); i++){
			float before = g.cooldown();
			assertTrue( act( g ) );
			assertTrue( "no time spent", g.cooldown() > before || !ow.mobs.contains( g ) );
		}
		assertFalse( "the watch stayed out after dark", ow.mobs.contains( g ) );
		assertFalse( Actor.all().contains( g ) );
	}

	//a whole beat walked on a real window: out from the door the whole road to the neighbouring
	//village's well, a stand there, back the way it came, and in at the door once it is over -
	//every turn spending time
	@Test
	public void theWatchWalksTheWholeRoadToTheNextVillageAndBack(){
		int[] town = watchTown();
		long home = WorldStructures.sectorOf( town[0], town[1] );
		int day = 10, w0 = RoadTraffic.setOut(), w1 = RoadTraffic.indoors();
		RoadTraffic.Beat beat = RoadTraffic.beat( SEED, town[0], town[1], day, w0, w1 );
		int start = beat.starts[0];
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + start;
		long nv = WorldStructures.roadNeighbour( SEED, town[0], town[1] );
		int nx = (int)(nv >> 32), ny = (int) nv;
		//the window over the middle of the road, both wells in it
		OverworldLevel ow = windowAt( (WorldStructures.siteX( SEED, town[0], town[1] ) + WorldStructures.siteX( SEED, nx, ny )) / 2,
				(WorldStructures.siteY( SEED, town[0], town[1] ) + WorldStructures.siteY( SEED, nx, ny )) / 2 );
		hero.pos = farFrom( ow, beat.route, 30 );
		assertNotEquals( -1, hero.pos );
		float len = RoadTraffic.length( beat.route );
		float[] end = RoadTraffic.point( beat.route, len );
		assertEquals( "the beat turns at the neighbour's well", WorldStructures.siteX( SEED, nx, ny ), Math.round( end[0] ) );
		assertEquals( WorldStructures.siteY( SEED, nx, ny ), Math.round( end[1] ) );
		assertTrue( "the neighbour's well lies outside the window", ow.inWindow( end[0], end[1] ) );

		RoadPatrol g = RoadPatrol.of( beat, home, 0, 1 );
		int door = ow.localCell( Math.round( beat.route[0] ), Math.round( beat.route[1] ) );
		int at = -1;
		for (int r = 1; r <= 2 && at == -1; r++){
			for (int ofs : PathFinder.NEIGHBOURS8){
				int c = door + ofs * r;
				if (ow.passable[c] && !ow.occupied( c ) && ow.map[c] != Terrain.DOOR){
					at = c;
					break;
				}
			}
		}
		assertNotEquals( -1, at );
		g.pos = at;
		g.sprite = stub();
		ow.mobs.add( g );
		Actor.add( g );

		float furthest = 0, lastOut = len, nearestBack = len;
		boolean wentIn = false;
		int span = beat.cycle() + 20;
		for (int t = 1; t <= span && !wentIn; t++){
			Dungeon.cycleTurn++;
			while (g.cooldown() < t && ow.mobs.contains( g )){
				float before = g.cooldown();
				assertTrue( act( g ) );
				assertTrue( "no time spent", g.cooldown() > before || !ow.mobs.contains( g ) );
			}
			if (!ow.mobs.contains( g )){
				wentIn = true;
				break;
			}
			float s = RoadTraffic.project( beat.route, ow.worldX() + g.pos % W, ow.worldY() + g.pos / W );
			int u = RoadTraffic.turnOfDay( Dungeon.cycleTurn ) - start;
			int walk = (int) Math.ceil( len / RoadTraffic.Beat.PACE );
			if (u < walk + RoadTraffic.Beat.PAUSE) furthest = Math.max( furthest, s );
			else nearestBack = Math.min( nearestBack, s );
			lastOut = s;
		}
		System.out.println( "[traffic] the watch got " + furthest + " of " + len + " cells out, came back to "
				+ nearestBack + (wentIn ? ", and went in" : ", and is still out at " + lastOut) );
		assertTrue( "only got " + furthest + " of " + len + " out", furthest >= len - 3 );
		assertTrue( "only came back to " + nearestBack, nearestBack <= 6 );
		assertTrue( "still out when the beat was over", wentIn );
	}

	//on its beat right beside the hero and a villager, with no outlaw about, the watch never
	//raises a hand to either and never makes either its foe
	@Test
	public void theWatchNeverTurnsOnTheHeroOrTheFolk(){
		int[] town = watchTown();
		long home = WorldStructures.sectorOf( town[0], town[1] );
		int day = 10, w0 = RoadTraffic.setOut(), w1 = RoadTraffic.indoors();
		RoadTraffic.Beat beat = RoadTraffic.beat( SEED, town[0], town[1], day, w0, w1 );
		int tod = beat.starts[0] + 10;
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + tod;
		OverworldLevel ow = windowAt( WorldStructures.siteX( SEED, town[0], town[1] ), WorldStructures.siteY( SEED, town[0], town[1] ) );
		Arrays.fill( ow.heroFOV, true );
		RoadPatrol g = RoadPatrol.of( beat, home, 0, 1 );
		g.pos = -1;
		Villager v = new Villager();
		v.pos = -1;
		for (int ofs : PathFinder.NEIGHBOURS8){
			int c = hero.pos + ofs;
			if (!ow.passable[c] || ow.occupied( c )) continue;
			if (g.pos == -1) g.pos = c;
			else if (v.pos == -1 && c != g.pos){
				v.pos = c;
				break;
			}
		}
		assertNotEquals( -1, g.pos );
		assertNotEquals( -1, v.pos );
		g.sprite = stub();
		v.sprite = stub();
		hero.sprite = stub();
		ow.mobs.add( g );
		ow.mobs.add( v );
		Actor.add( g );
		Actor.add( v );
		int heroHP = hero.HP, folkHP = v.HP;
		for (int i = 0; i < 40 && ow.mobs.contains( g ); i++){
			act( g );
			assertNotSame( hero, g.getEnemy() );
			assertNotSame( v, g.getEnemy() );
		}
		assertEquals( heroHP, hero.HP );
		assertEquals( folkHP, v.HP );
	}

	//the outlaws turn on the watch they see when there is no hero to be had - the hero first
	//when he is; one the hero has turned, or one frightened, is none of this
	@Test
	public void outlawsTurnOnTheWatchButNotWhenTurnedOrAfraid(){
		Level level = bareLevel( 40, 40 );
		Dungeon.level = level;
		hero.pos = 5 + 5 * 40;
		RoadPatrol g = new RoadPatrol();
		g.pos = 25 + 20 * 40;
		level.mobs.add( g );
		Actor.add( g );
		OverworldBandit b = new OverworldBandit();
		b.pos = 20 + 20 * 40;
		b.state = b.WANDERING;
		b.fieldOfView = new boolean[level.length()];
		Arrays.fill( b.fieldOfView, true );
		b.fieldOfView[hero.pos] = false;
		level.mobs.add( b );
		Actor.add( b );
		assertSame( g, choose( b ) );
		//the hero in sight: him first
		b.fieldOfView[hero.pos] = true;
		assertSame( hero, choose( b ) );
		b.fieldOfView[hero.pos] = false;
		//out of its sight, the watch is nobody's quarrel
		b.fieldOfView[g.pos] = false;
		assertNotSame( g, choose( b ) );
		b.fieldOfView[g.pos] = true;
		//turned by the hero
		b.alignment = Char.Alignment.ALLY;
		assertNotSame( g, choose( b ) );
		b.alignment = Char.Alignment.ENEMY;
		//frightened
		Buff.affect( b, Terror.class, 5f );
		assertNotSame( g, choose( b ) );
	}

	//the watch goes after an outlaw only within its leash of where its beat has got to - measured
	//from that point even when it lies past the window's edge, where no cell stands for it
	@Test
	public void theWatchKeepsToItsLeashWhereverItsBeatHasGot() throws Exception {
		int[] r = road();
		OverworldLevel ow = windowAt( WorldStructures.siteX( SEED, r[0], r[1] ), WorldStructures.siteY( SEED, r[0], r[1] ) );
		Method foe = RoadPatrol.class.getDeclaredMethod( "foe", OverworldLevel.class, float[].class );
		foe.setAccessible( true );
		RoadPatrol g = new RoadPatrol();
		g.pos = hero.pos;
		g.fieldOfView = new boolean[ow.length()];
		Arrays.fill( g.fieldOfView, true );
		OverworldBandit near = new OverworldBandit(), far = new OverworldBandit();
		near.pos = hero.pos + 5;
		far.pos = hero.pos + 20 * W;
		ow.mobs.add( far );
		float[] here = { ow.worldX() + hero.pos % W, ow.worldY() + hero.pos / W };
		//one well off the beat is let be
		assertNull( foe.invoke( g, ow, here ) );
		ow.mobs.add( near );
		assertSame( near, foe.invoke( g, ow, here ) );
		//the beat gone on past the window's edge: nobody in the window is within its leash
		float[] beyond = { ow.worldX() - 40, ow.worldY() - 40 };
		assertFalse( ow.inWindow( beyond[0], beyond[1] ) );
		assertNull( foe.invoke( g, ow, beyond ) );
	}

	// ------------------------------------------------------------ saves

	//the surface's ledger of fought-out ambushes comes back from a save, and an older save,
	//which never had one, loads to an empty ledger that the day then takes over
	@Test
	public void theAmbushLedgerSurvivesASaveAndOldSavesLoadWithoutIt(){
		int[][] found = new int[1][];
		int[] spot = ambushedStallSpot( found );
		OverworldLevel ow = windowAt( spot[0], spot[1] );
		Caravaneer stall = springAt( ow, spot );
		for (OverworldBandit b : raiders( ow.mobs, stall.stall )) ow.mobs.remove( b );
		ow.settleSiege( stall );
		assertEquals( Caravaneer.AMBUSH_SAVED, stall.ambush );
		long road = stall.road;
		assertNotEquals( Long.MIN_VALUE, road );

		ow.customTerrain = new ArrayList<>();
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		//a headless Game carries no version code: stamp the save as a current one
		b.put( "version", xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon.v3_1_1 + 1 );

		OverworldLevel back = new OverworldLevel();
		back.restoreFromBundle( b );
		@SuppressWarnings("unchecked") HashSet<Long> ledger = (HashSet<Long>) field( back, OverworldLevel.class, "ambushDone" );
		assertTrue( ledger.contains( road ) );
		assertEquals( WorldClock.day(), field( back, OverworldLevel.class, "ambushDoneDay" ) );

		assertTrue( b.remove( "ambush_done" ) );
		assertTrue( b.remove( "ambush_done_day" ) );
		OverworldLevel old = new OverworldLevel();
		old.restoreFromBundle( b );
		@SuppressWarnings("unchecked") HashSet<Long> none = (HashSet<Long>) field( old, OverworldLevel.class, "ambushDone" );
		assertTrue( none.isEmpty() );
		assertEquals( Integer.MIN_VALUE, field( old, OverworldLevel.class, "ambushDoneDay" ) );
	}

	// ------------------------------------------------------------ cost

	//a real-clock run's day is some 170,000 traffic turns, so late in it every lane has walked a
	//couple of hundred walks: the look for walkers due on a window's roads must stay cheap anyway
	@Test
	public void aRealClockLookStaysCheap(){
		int w0 = 6 * 3600 * RoadTraffic.TURNS_PER_SECOND, w1 = 20 * 3600 * RoadTraffic.TURNS_PER_SECOND;
		int tod = w1 - 500;
		//more than a window's sectors and the one beyond (placeTravellers searches some 7x7)
		int sx0 = -6, sy0 = -6, sx1 = 6, sy1 = 6;
		ArrayList<int[]> pairs = RoadTraffic.travelPairs( SEED, sx0, sy0, sx1, sy1 );
		long best = Long.MAX_VALUE;
		int lanes = 0;
		for (int rep = 0; rep < 5; rep++){
			long t0 = System.nanoTime();
			lanes = 0;
			for (int[] r : pairs){
				if (!RoadTraffic.travelled( SEED, r[0], r[1], r[2], r[3] )) continue;
				for (int lane = 0; lane < RoadTraffic.lanes( SEED, r[0], r[1], r[2], r[3] ); lane++){
					RoadTraffic.tripAt( SEED, r[0], r[1], r[2], r[3], lane, 20_000, tod, w0, w1 );
					lanes++;
				}
			}
			best = Math.min( best, System.nanoTime() - t0 );
		}
		double ms = best / 1e6;
		System.out.println( "[traffic] a real-clock look late in the day: " + ms + " ms over " + lanes + " lanes" );
		assertTrue( "lanes " + lanes, lanes > 5 );
		assertTrue( "a look took " + ms + " ms", ms < 100 );
	}
}

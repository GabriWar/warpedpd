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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HuntPack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Raider;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.ToIntFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The raids' and the hunts' edges RaidEventTest and HuntEventTest leave alone: a band beaten
 * off right after a real save is loaded (before the hero has taken a step) still saves the
 * village and never brings a second band out; today's raiders outlast the parked store's
 * pruning however far the hero goes, and yesterday's do not; a raid's band is never counted as
 * an outlaw camp's own; the raid and the hunt are pinned on the world map exactly while they
 * are on and unsettled; a hunt's wolves are spared the dawn cull as its deer are the dusk's;
 * and a real chase, the wolves and the deer taking their own turns, ends with the deer down, the
 * meat on the ground and the pack feeding by it.
 *
 * And what the review of them found: a raid known the moment the surface is read back (its
 * shops shut, its folk in, its fires burning) before any step; a real-clock run's wall clock
 * turned back in the middle of a raid leaving the band, the parked store and the favour on the
 * events' day; a co-op guest's mirror showing the fires the host's screen does; a pack put to
 * sleep left asleep by its hunt's end; a co-op guest starting a hunt and keeping it running with
 * the host far off; and a hunt's beasts set down where no hero sees.
 */
public class RaidHuntEdgesTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int FC = WorldEvents.FC;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int turn, startDay, challenges, depth, branch;
	private long dungeonSeed;
	private float precip;
	private DayNightCycle.Phase override;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level saved;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WarpedRoomsTest.boot();
		Generator.fullReset();
		xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll.initLabels();
		xyz.gabriwar.warpedpixeldungeon.items.potions.Potion.initColors();
		Ring.initGems();
		//the raid's prize is identified, and identifying reaches the badges
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
		dungeonSeed = Dungeon.seed;
		precip = ClimateManager.debugPrecipOverride;
		override = DayNightCycle.debugPhaseOverride;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		Dungeon.seed = OverworldLevel.worldSeedOf( SEED );
		DayNightCycle.debugPhaseOverride = null;
		ClimateManager.debugPrecipOverride = 0f;
		hero = new Hero();
		hero.lvl = 12;
		hero.sprite = stub();
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
		Dungeon.seed = dungeonSeed;
		ClimateManager.debugPrecipOverride = precip;
		DayNightCycle.debugPhaseOverride = override;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	//a town of the test seed with a watch, and a day it is raided with no other raid on it from
	//three days before to four after: {sx, sy, day} (as RaidEventTest finds it)
	private static int[] townRaid;

	private static int[] townRaid(){
		if (townRaid != null) return townRaid;
		for (int r = 0; r <= 10 && townRaid == null; r++){
			for (int sy = -r; sy <= r && townRaid == null; sy++){
				for (int sx = -r; sx <= r && townRaid == null; sx++){
					if (Math.max( Math.abs( sx ), Math.abs( sy ) ) != r) continue;
					if (WorldStructures.siteType( SEED, sx, sy ) != WorldStructures.Site.VILLAGE
							|| WorldStructures.faction( SEED, sx, sy ) != WorldStructures.Faction.HUMAN
							|| (WorldStructures.settlementLayout( SEED, sx, sy ).length - 1) / 2 < 9) continue;
					for (int d = 10; d <= 1500 && townRaid == null; d++){
						if (!RaidEvent.raidedOn( SEED, sx, sy, d )) continue;
						boolean alone = true;
						for (int o = d - 3; o <= d + 4 && alone; o++) alone = o == d || !RaidEvent.raidedOn( SEED, sx, sy, o );
						if (alone) townRaid = new int[]{ sx, sy, d };
					}
				}
			}
		}
		assertNotNull( "no raided town near the origin for the test seed", townRaid );
		return townRaid;
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

	private static Object invoke( Object o, Class<?> cls, String name, Class<?>[] types, Object... args ){
		try {
			Method m = cls.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( o, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	//the live surface window round a world cell, built as a mirror's is and made the host's own;
	//the hero on the nearest open cell to it, seeing nothing
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

	//the raided town's window on its raid's day, by day, the hero by its well
	private OverworldLevel raidedTown(){
		int[] t = townRaid();
		Dungeon.cycleTurn = t[2] * FC + 600;
		return windowAt( WorldStructures.siteX( SEED, t[0], t[1] ), WorldStructures.siteY( SEED, t[0], t[1] ) );
	}

	//the level as a real save has it: written to bytes, read back into a fresh level (as
	//WorldEventsPersistenceTest saves one)
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

	private static ArrayList<Raider> raiders( OverworldLevel ow ){
		ArrayList<Raider> out = new ArrayList<>();
		for (Mob m : ow.mobs) if (m instanceof Raider) out.add( (Raider) m );
		return out;
	}

	private static CharSprite stub(){
		CharSprite sp = new CharSprite(){
			@Override public void place( int cell ){ }
			@Override public void turnTo( int from, int to ){ }
			@Override public void update(){ }
			@Override public void die(){ }
			@Override public void move( int from, int to ){ }
			@Override public void showStatus( int color, String text, Object... args ){ }
			@Override public void showAlert(){ }
			@Override public void hideAlert(){ }
			@Override public void showLost(){ }
		};
		sp.visible = false;
		return sp;
	}

	private static boolean pinned( OverworldLevel ow, long id, int wx, int wy ){
		for (WorldEvents.Event e : ow.mapEvents( wx - 128, wy - 128, 256 )) if (e.id == id) return true;
		return false;
	}

	private static void park( OverworldLevel ow, Mob m, int wx, int wy ){
		invoke( ow, OverworldLevel.class, "park", new Class<?>[]{ Mob.class, int.class, int.class }, m, wx, wy );
	}

	// ------------------------------------------------------------------ the raid across a load

	//a player loads a save in the middle of a raid and the band, already in the streets, comes at
	//him: he cuts the last of them down before taking a step. that is the raid beaten off
	@Test
	public void aBandBeatenOffRightAfterALoadSavesTheVillage() throws Exception {
		int[] t = townRaid();
		long id = WorldEvents.idOf( WorldEvents.Type.RAID, t[0], t[1], t[2] );
		OverworldLevel ow = raidedTown();
		RaidEvent.onHeroStep( ow );
		assertFalse( "the band is out", raiders( ow ).isEmpty() );

		OverworldLevel back = saveAndLoad( ow );
		ArrayList<Raider> band = raiders( back );
		assertEquals( "the band comes back with the save", raiders( ow ).size(), band.size() );
		for (Raider r : band){
			r.sprite = stub();
			r.die( hero );
		}
		assertTrue( "every raider dead with the hero in the streets: the village is saved", back.eventResolved( id ) );
	}

	//...and whatever settles it, his first step afterwards never lets a fresh band out on a
	//village he has just cleared
	@Test
	public void noSecondBandAfterTheFirstIsBeatenOffRightAfterALoad() throws Exception {
		OverworldLevel ow = raidedTown();
		RaidEvent.onHeroStep( ow );
		int first = raiders( ow ).size();
		assertTrue( first > 0 );

		OverworldLevel back = saveAndLoad( ow );
		for (Raider r : raiders( back )){
			r.sprite = stub();
			r.die( hero );
		}
		assertTrue( raiders( back ).isEmpty() );
		RaidEvent.onHeroStep( back );
		assertEquals( "a second band came out of the doors of a village just cleared", 0, raiders( back ).size() );
	}

	//a reload in the middle of a raid, the band left standing: the step after it counts the band
	//that came back with the save and adds nobody
	@Test
	public void aLoadMidRaidKeepsTheOneBand() throws Exception {
		OverworldLevel ow = raidedTown();
		RaidEvent.onHeroStep( ow );
		int first = raiders( ow ).size();
		OverworldLevel back = saveAndLoad( ow );
		RaidEvent.onHeroStep( back );
		assertEquals( first, raiders( back ).size() );
	}

	//the surface read back from its save in the middle of a raid: the raid is known before the
	//hero takes a step - the shops shut (and a keeper deaf to a fight's splash), the folk in and
	//the fires burning, as they were the moment before the save
	@Test
	public void aRaidIsKnownTheMomentTheSurfaceIsReadBack() throws Exception {
		int[] t = townRaid();
		long sector = WorldStructures.sectorOf( t[0], t[1] );
		OverworldLevel ow = raidedTown();
		RaidEvent.onHeroStep( ow );
		assertTrue( RaidEvent.ongoing( sector ) );

		OverworldLevel back = saveAndLoad( ow );
		assertTrue( "the raid unknown until the hero's first step", RaidEvent.ongoing( sector ) );
		assertEquals( "the shops trading", Messages.get( RaidEvent.class, "shut_raid" ), RaidEvent.shutReason( sector ) );
		assertTrue( "the folk out in the streets", RaidEvent.shelters( sector, 1 ) );
		assertFalse( "the fires out", RaidEvent.burningHouses( back ).isEmpty() );
	}

	//a co-op guest's screen shows the raid's fires as the host's does: his mirror of the window
	//takes no stock of the raids, and works out the same burning houses from the world and what
	//his host shipped him - and puts them out once the host's party has beaten the band off
	@Test
	public void aGuestsMirrorShowsTheFiresTheHostDoes(){
		OverworldLevel ow = raidedTown();
		RaidEvent.onHeroStep( ow );
		ArrayList<String> host = new ArrayList<>();
		for (int[] h : RaidEvent.burningHouses( ow )) host.add( Arrays.toString( h ) );
		assertFalse( "no fires on the host's screen", host.isEmpty() );

		float shift = WorldModel.calendarShift();
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ow.worldX, ow.worldY, shift );
		OverworldLevel mirror = OverworldLevel.forNetwork( 0, SEED, ow.worldX, ow.worldY, shift, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( mirror );
		mirror.adoptSharedEvents( ow.sharedEvents() );
		ArrayList<String> guest = new ArrayList<>();
		for (int[] h : RaidEvent.burningHouses( mirror )) guest.add( Arrays.toString( h ) );
		assertEquals( "the guest's fires", host, guest );

		for (Raider r : raiders( ow )){
			r.sprite = stub();
			r.die( hero );
		}
		mirror.adoptSharedEvents( ow.sharedEvents() );
		assertTrue( "the guest's fires still burning once the band is beaten off", RaidEvent.burningHouses( mirror ).isEmpty() );
	}

	// ------------------------------------------------------------------ the clock turned back

	//a real-clock run's wall clock goes back past midnight (west across a time zone) in the middle
	//of a raid. the world's events never run back, and the raid keeps their day: the band comes out
	//and stays to fight instead of making off as it comes out (with a new one out of the doors
	//every step after), the parked store keeps it, and the village saved owes its prices
	@Test
	public void aWallClockTurnedBackKeepsTheRaidsDay() throws Exception {
		int[] t = townRaid();
		long sector = WorldStructures.sectorOf( t[0], t[1] );
		OverworldLevel ow = raidedTown();
		//the events saw the raid's morning; the wall clock is back to the evening before it
		ow.eventTurn();
		Dungeon.cycleTurn = t[2] * FC - 300;
		assertEquals( t[2] - 1, WorldClock.day() );
		assertEquals( "the raids read the wall clock's day", t[2], RaidEvent.day() );

		RaidEvent.onHeroStep( ow );
		ArrayList<Raider> band = raiders( ow );
		assertFalse( "no band out on the raid's day", band.isEmpty() );
		for (Raider r : band){
			assertEquals( t[2], r.day );
			assertTrue( "let go by the parked store", r.eventHeld() );
			r.sprite = stub();
			Actor.add( r );
			invoke( r, Actor.class, "act", new Class<?>[0] );
			assertFalse( "made off as it came out", r.retreating );
		}
		RaidEvent.onHeroStep( ow );
		assertEquals( "a second band out of the doors", band.size(), raiders( ow ).size() );

		//the store swept of past days' raiders keeps today's
		Raider kept = band.get( 0 );
		ow.mobs.remove( kept );
		park( ow, kept, ow.worldX + kept.pos % W, ow.worldY + kept.pos / W );
		Field forgot = RaidEvent.class.getDeclaredField( "forgotDay" );
		forgot.setAccessible( true );
		forgot.setInt( null, Integer.MIN_VALUE );
		RaidEvent.onHeroStep( ow );
		assertTrue( "today's raider swept from the store as a past day's", ow.parked().contains( kept ) );
		assertEquals( band.size() - 1, raiders( ow ).size() );

		//beaten off (the parked one cut down where it lies), the favour is the raid's day's too
		ow.parked().remove( kept );
		for (Raider r : raiders( ow )) r.die( hero );
		assertTrue( ow.eventResolved( WorldEvents.idOf( WorldEvents.Type.RAID, t[0], t[1], t[2] ) ) );
		assertTrue( "no favour owed", RaidEvent.favoured( sector ) );
		assertEquals( RaidEvent.FAVOUR_PRICE, RaidEvent.priceFactor( sector ), 0f );
	}

	// ------------------------------------------------------------------ the parked store

	//the hero walks off far enough that the store forgets parked wildlife: today's raiders are
	//kept (or the band would come out whole again on his return), a past day's are not
	@Test
	public void todaysRaidersOutlastThePrune(){
		int[] t = townRaid();
		OverworldLevel ow = raidedTown();
		RaidEvent.onHeroStep( ow );
		ArrayList<Raider> band = raiders( ow );
		int size = band.size();
		Raider kept = band.get( 0 );
		ow.mobs.remove( kept );
		int kx = ow.worldX + kept.pos % W, ky = ow.worldY + kept.pos / W;
		park( ow, kept, kx, ky );
		Raider old = new Raider();
		old.sector = kept.sector;
		old.day = t[2] - 1;
		park( ow, old, kx + 40, ky + 40 );
		assertTrue( ow.parked().contains( kept ) && ow.parked().contains( old ) );

		int far = 3 * W + 50;
		invoke( ow, OverworldLevel.class, "pruneParked", new Class<?>[]{ int.class, int.class }, kx + far, ky + far );
		assertTrue( "today's raider forgotten by the prune", ow.parked().contains( kept ) );
		assertFalse( "a past raid's raider kept", ow.parked().contains( old ) );

		//the hero back in the streets: the band is the one he left, not a new one
		RaidEvent.onHeroStep( ow );
		assertEquals( size - 1, raiders( ow ).size() );
	}

	//an outlaw camp's census never counts a raid's band, in the window or in the store
	@Test
	public void aRaidsBandIsNoCampsOwn(){
		OverworldLevel ow = raidedTown();
		RaidEvent.onHeroStep( ow );
		Raider r = raiders( ow ).get( 0 );
		Class<?>[] types = { Class.class, int.class, int.class };
		assertFalse( "a raider in the streets counted as a camp's",
				(Boolean) invoke( ow, OverworldLevel.class, "mobNear", types, OverworldBandit.class, r.pos, 2 ) );
		ow.mobs.remove( r );
		park( ow, r, ow.worldX + r.pos % W, ow.worldY + r.pos / W );
		assertFalse( "a parked raider counted as a camp's",
				(Boolean) invoke( ow, OverworldLevel.class, "mobNear", types, OverworldBandit.class, r.pos, 2 ) );
		//a camp's own outlaw on the same spot is counted
		OverworldBandit own = new OverworldBandit();
		own.pos = r.pos;
		ow.mobs.add( own );
		assertTrue( (Boolean) invoke( ow, OverworldLevel.class, "mobNear", types, OverworldBandit.class, r.pos, 2 ) );
	}

	// ------------------------------------------------------------------ the world map

	//the raid is on the chart from the moment it is heard of until it is beaten off
	@Test
	public void theRaidIsPinnedWhileItIsOn(){
		int[] t = townRaid();
		long id = WorldEvents.idOf( WorldEvents.Type.RAID, t[0], t[1], t[2] );
		int cx = WorldStructures.siteX( SEED, t[0], t[1] ), cy = WorldStructures.siteY( SEED, t[0], t[1] );
		OverworldLevel ow = raidedTown();
		assertFalse( "pinned before it was heard of", pinned( ow, id, cx, cy ) );
		RaidEvent.onHeroStep( ow );
		assertTrue( "not pinned once heard of", pinned( ow, id, cx, cy ) );
		//unanswered, it is off the chart with its day (the map only peeks at the clock)
		Dungeon.cycleTurn = (t[2] + 1) * FC + 10;
		assertFalse( "still pinned the day after", pinned( ow, id, cx, cy ) );
		Dungeon.cycleTurn = t[2] * FC + 600;
		assertTrue( pinned( ow, id, cx, cy ) );
		for (Raider r : raiders( ow )){
			r.sprite = stub();
			r.die( hero );
		}
		assertTrue( ow.eventResolved( id ) );
		assertFalse( "still pinned once beaten off", pinned( ow, id, cx, cy ) );
	}

	//a hunt is on the chart from its start until it is over
	@Test
	public void theHuntIsPinnedWhileItIsOn(){
		Dungeon.cycleTurn = 40 * FC + 2000;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		//somewhere open, well off the town, a hunt forced ten cells from the hero: the first such
		//spot where it finds room to start (and no hunt of the world's own gets in first)
		OverworldLevel ow = null;
		WorldEvents.Event e = null;
		for (int k = 0; k < 20 && e == null; k++){
			ow = windowAt( 400 + k * 200, -300 );
			int[] ground = HuntEvent.debugGround( ow, hero.pos, 10 );
			if (ground == null) continue;
			WorldEvents.Event f = WorldEvents.forcedHunt( ground[0], ground[1], ow.eventTurn(),
					xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.nightStart( xyz.gabriwar.warpedpixeldungeon.actors.WorldClock.night() + 1 ) );
			ow.forceEvent( f );
			assertFalse( "pinned before it started", pinned( ow, f.id, f.wx, f.wy ) );
			HuntEvent.onHeroStep( ow );
			if (ow.eventBegun( f.id )) e = f;
		}
		assertNotNull( "no forced hunt found room to start", e );
		assertTrue( "not pinned while it runs", pinned( ow, e.id, e.wx, e.wy ) );
		//the dawn ends it, and with it its pin
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAWN;
		HuntEvent.onHeroStep( ow );
		assertTrue( ow.eventResolved( e.id ) );
		assertFalse( "still pinned once over", pinned( ow, e.id, e.wx, e.wy ) );
	}

	//the first window of a few, somewhere open and well off the town, where a hunt forced ten
	//cells from the cell `party` returns (after placing the party in the window; -1 to pass the
	//window over) finds room to start on the hero's step pass, at night: {the window, the hunt}
	private Object[] forcedHunt( ToIntFunction<OverworldLevel> party ){
		Dungeon.cycleTurn = 40 * FC + 2000;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		for (int k = 0; k < 20; k++){
			OverworldLevel ow = windowAt( 400 + k * 200, -300 );
			int near = party.applyAsInt( ow );
			if (near == -1) continue;
			int[] ground = HuntEvent.debugGround( ow, near, 10 );
			if (ground == null) continue;
			WorldEvents.Event f = WorldEvents.forcedHunt( ground[0], ground[1], ow.eventTurn(),
					WorldClock.nightStart( WorldClock.night() + 1 ) );
			ow.forceEvent( f );
			HuntEvent.onHeroStep( ow );
			if (ow.eventBegun( f.id )) return new Object[]{ ow, f };
		}
		fail( "no forced hunt found room to start" );
		return null;
	}

	private static ArrayList<Mob> beastsOf( OverworldLevel ow, long hunt, boolean wolvesOnly ){
		ArrayList<Mob> out = new ArrayList<>();
		for (Mob m : ow.mobs){
			HuntPack p = m.buff( HuntPack.class );
			if (p != null && p.hunt == hunt && (!wolvesOnly || HuntPack.isWolf( m ))) out.add( m );
		}
		return out;
	}

	//an open cell of the window by (x, y), or -1
	private static int openCellNear( OverworldLevel ow, int x, int y ){
		for (int r = 0; r <= 8; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					int cx = x + dx, cy = y + dy;
					if (cx < 3 || cy < 3 || cx >= W - 3 || cy >= H - 3) continue;
					int c = cx + cy * W;
					if (ow.passable[c] && !ow.occupied( c )) return c;
				}
			}
		}
		return -1;
	}

	//the dawn ends a hunt whose pack the hero put to sleep: the wolves are let go as ordinary
	//wolves, but left asleep - a lullaby's sleep lasts only while a wolf's state says so - and
	//only one still on the deer's trail is called off it
	@Test
	public void aPackPutToSleepSleepsThroughItsHuntsEnd(){
		Object[] run = forcedHunt( ow -> hero.pos );
		OverworldLevel ow = (OverworldLevel) run[0];
		WorldEvents.Event e = (WorldEvents.Event) run[1];
		ArrayList<Mob> wolves = beastsOf( ow, e.id, true );
		assertTrue( "a pack of " + wolves.size(), wolves.size() >= 2 );
		Mob awake = wolves.get( 0 );
		awake.state = awake.HUNTING;
		for (int i = 1; i < wolves.size(); i++){
			Buff.affect( wolves.get( i ), MagicalSleep.class );
			assertSame( wolves.get( i ).SLEEPING, wolves.get( i ).state );
		}
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAWN;
		HuntEvent.onHeroStep( ow );
		assertTrue( ow.eventResolved( e.id ) );
		for (Mob w : wolves){
			assertTrue( "a wolf by the hero slipped away", ow.mobs.contains( w ) );
			assertNull( "still on the hunt", w.buff( HuntPack.class ) );
		}
		assertSame( "the chase not called off", awake.WANDERING, awake.state );
		for (int i = 1; i < wolves.size(); i++){
			Mob w = wolves.get( i );
			assertSame( "woken by the hunt's end", w.SLEEPING, w.state );
			assertNotNull( w.buff( MagicalSleep.class ) );
		}
	}

	//a co-op guest by a hunting ground starts its hunt with the host seventy cells off, and keeps
	//it running however far the host goes: the hunts go by wherever any hero of the party stands
	@Test
	public void aGuestStartsAndKeepsAHunt() throws Exception {
		Hero guest = new Hero();
		TestParty.join( guest );
		try {
			Object[] run = forcedHunt( ow -> {
				int far = openCellNear( ow, hero.pos % W + 70, hero.pos / W );
				if (far == -1) return -1;
				guest.pos = hero.pos;
				hero.pos = far;
				return guest.pos;
			} );
			OverworldLevel ow = (OverworldLevel) run[0];
			WorldEvents.Event e = (WorldEvents.Event) run[1];
			int hx = ow.worldX + hero.pos % W, hy = ow.worldY + hero.pos / W;
			assertTrue( "the host by the hunt", Math.max( Math.abs( e.wx - hx ), Math.abs( e.wy - hy ) ) > HuntEvent.FAR );
			int beasts = beastsOf( ow, e.id, false ).size();
			assertTrue( beasts >= 3 );
			HuntEvent.onHeroStep( ow );
			assertFalse( "ended by the host's distance with the guest by it", ow.eventResolved( e.id ) );
			assertEquals( beasts, beastsOf( ow, e.id, false ).size() );
		} finally {
			TestParty.leave();
		}
	}

	//a hunt starting where the hero sees all about him for twelve cells (the dark's edge): its
	//deer and its wolves are set down out of his sight, and not even next to what he sees, so
	//none of them is seen to come out of nothing
	@Test
	public void aHuntsBeastsComeOutOfNoHerosSight(){
		Object[] run = forcedHunt( ow -> {
			int hx = hero.pos % W, hy = hero.pos / W;
			for (int c = 0; c < ow.length(); c++){
				ow.heroFOV[c] = Math.max( Math.abs( c % W - hx ), Math.abs( c / W - hy ) ) <= 12;
			}
			return hero.pos;
		} );
		OverworldLevel ow = (OverworldLevel) run[0];
		ArrayList<Mob> beasts = beastsOf( ow, ((WorldEvents.Event) run[1]).id, false );
		assertTrue( beasts.size() >= 3 );
		for (Mob m : beasts){
			assertFalse( m.getClass().getSimpleName() + " set down in plain sight", ow.heroFOV[m.pos] );
			for (int n : PathFinder.NEIGHBOURS8){
				assertFalse( m.getClass().getSimpleName() + " set down a step from sight", ow.heroFOV[m.pos + n] );
			}
		}
	}

	// ------------------------------------------------------------------ the cull

	//by day the cull takes the night's wolves out of sight - but not a hunt's, which the hunt
	//itself winds down
	@Test
	public void huntWolvesAreSparedTheDawnCull(){
		OverworldLevel ow = new OverworldLevel();
		ow.setSize( 64, 64 );
		ow.mobs = new HashSet<>();
		Dungeon.level = ow;
		hero.pos = 32 + 32 * 64;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAWN;
		GrayWolf plain = new GrayWolf();
		plain.pos = 2 + 2 * 64;
		GrayWolf grey = new GrayWolf();
		grey.pos = 60 + 60 * 64;
		Buff.affect( grey, HuntPack.class ).hunt = 1L;
		BrownWolf brown = new BrownWolf();
		brown.pos = 2 + 60 * 64;
		Buff.affect( brown, HuntPack.class ).hunt = 1L;
		ow.mobs.add( plain );
		ow.mobs.add( grey );
		ow.mobs.add( brown );
		OverworldFauna.cull( ow );
		assertFalse( "a night wolf out of sight by day is culled", ow.mobs.contains( plain ) );
		assertTrue( "a hunt's grey wolf is the hunt's to end", ow.mobs.contains( grey ) );
		assertTrue( "a hunt's brown wolf is the hunt's to end", ow.mobs.contains( brown ) );
	}

	// ------------------------------------------------------------------ the chase

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

	private static float timeOf( Mob m ){
		try {
			Field f = Actor.class.getDeclaredField( "time" );
			f.setAccessible( true );
			return f.getFloat( m );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	//the scheduler's clock set to an actor's turn, as Actor.process sets it before it acts
	private static void nowAt( float t ){
		try {
			Field f = Actor.class.getDeclaredField( "now" );
			f.setAccessible( true );
			f.setFloat( null, t );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	//a pack of two loosed on a deer four cells ahead of it in an open field, every beast taking
	//its own turns by its own clock (the hero standing well off): the deer is run down within a
	//few dozen turns, every turn taken spends time, the carcass leaves the pack's share of meat,
	//and the pack feeds by it paying the far-off hero no mind
	@Test
	public void aRealChaseEndsInTheKillAndTheFeeding(){
		Level l = bareLevel( 64, 64 );
		Painter.fill( l, 1, 1, 62, 62, Terrain.EMPTY );
		l.buildFlagMaps();
		Dungeon.level = l;
		Dungeon.depth = 3;
		hero.pos = 30 + 56 * 64;
		Arrays.fill( l.heroFOV, false );

		Deer deer = new Deer();
		deer.pos = 20 + 30 * 64;
		deer.sprite = stub();
		Buff.affect( deer, HuntPack.class ).hunt = 3L;
		l.mobs.add( deer );
		Actor.add( deer );
		ArrayList<Mob> pack = new ArrayList<>();
		for (int k = 0; k < 2; k++){
			GrayWolf w = new GrayWolf();
			w.pos = 16 + (30 + 2 * k) * 64;
			w.sprite = stub();
			l.mobs.add( w );
			Actor.add( w );
			HuntPack p = Buff.affect( w, HuntPack.class );
			p.hunt = 3L;
			HuntPack.loose( w, deer );
			pack.add( w );
		}
		int deerAt = -1, turns = 0;
		for (; turns < 400 && deer.isAlive(); turns++){
			Mob next = null;
			for (Mob m : l.mobs){
				if (m.isAlive() && (next == null || timeOf( m ) < timeOf( next ))) next = m;
			}
			float before = timeOf( next );
			nowAt( before );
			if (next == deer) deerAt = deer.pos;
			boolean done = (Boolean) invoke( next, Actor.class, "act", new Class<?>[0] );
			assertTrue( next.getClass().getSimpleName() + " waited on an animation that never comes", done );
			assertTrue( next.getClass().getSimpleName() + " took a turn without spending time", !next.isAlive() || timeOf( next ) > before );
		}
		assertFalse( "the pack never ran the deer down in " + turns + " turns", deer.isAlive() );
		System.out.println( "[hunt] the deer went down after " + turns + " actor turns" );

		//its carcass: the pack's share of meat by where it fell
		int meat = 0;
		for (Heap h : l.heaps.valueList()){
			if (l.distance( h.pos, deerAt ) > 3) continue;
			for (Item i : h.items) if (i instanceof MysteryMeat) meat += i.quantity();
		}
		assertTrue( "meat: " + meat, meat >= 2 );

		//the pack feeds, at ease: a few dozen of its turns later it is still by the kill and still
		//not after the hero, who stands far off
		for (Mob w : pack){
			HuntPack p = w.buff( HuntPack.class );
			assertNotNull( p );
			assertTrue( "the pack feeds", p.feeding );
		}
		for (int i = 0; i < 60; i++){
			Mob next = null;
			for (Mob m : pack) if (next == null || timeOf( m ) < timeOf( next )) next = m;
			nowAt( timeOf( next ) );
			invoke( next, Actor.class, "act", new Class<?>[0] );
		}
		for (Mob w : pack){
			assertTrue( "a feeding wolf strayed from the kill", l.distance( w.pos, deerAt ) <= 3 );
			assertFalse( "a feeding wolf went for the far-off hero", w.buff( HuntPack.class ).defending );
		}
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.bounty;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Roaming;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.TravelSlice;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Waystone;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Waystones;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoamerEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEvents;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The boards in the world and what the hero does at them (BountySlice, NoticeBoard, BountyActions,
 * BountyWorld, BountyShare): one board by the well of every village of people, put down once, kept
 * through window moves and saves, clear of the waystone and the guards' posts; the run's record of
 * notices and standing through a save; a hunt counted near its village and paid at its board, a
 * gathering's goods taken, a parcel handed to an elder - on time and late - and given up; the better
 * prices and friendlier guards standing brings; and a co-op guest's copy. Every window is a real one
 * of the world, derived from a seed.
 */
public class BountyBoardsTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
	//the guards' posts by day (VillageRoutine)
	private static final int[][] GUARD_POSTS = { {-2,-2}, {2,-2}, {2,0}, {0,2} };

	private int depth, branch, challenges, startDay, turn, gold, deepest;
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
		challenges = Dungeon.challenges;
		startDay = Dungeon.calendarStartDay;
		turn = Dungeon.cycleTurn;
		gold = Dungeon.gold;
		deepest = Statistics.deepestFloor;
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
		Dungeon.cycleTurn = 40 * DayNightCycle.FULL_CYCLE + 900;
		Dungeon.gold = 0;
		Statistics.deepestFloor = 0;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		hero = new Hero();
		hero.lvl = 10;
		Dungeon.hero = hero;
		HomesteadWorld.reset();
		BountyActions.refreshReady();
	}

	@After
	public void tearDown(){
		if (OverworldLevel.hasPendingArrival()) windowAt( 0, 0 ).getTransition( null );
		HomesteadWorld.reset();
		Random.popGenerator();
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.challenges = challenges;
		Dungeon.calendarStartDay = startDay;
		Dungeon.cycleTurn = turn;
		Dungeon.gold = gold;
		Statistics.deepestFloor = deepest;
		DayNightCycle.debugPhaseOverride = override;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		BountyActions.refreshReady();
	}

	// ------------------------------------------------------------------ helpers

	//the live window of a slice round a world cell, built as a mirror's is and made the host's own
	private static OverworldLevel windowAt( int altitude, int wx, int wy ){
		PathFinder.setMapSize( W, H );
		float s = WorldModel.calendarShift();
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, altitude, ox, oy, s );
		OverworldLevel ow = OverworldLevel.forNetwork( altitude, SEED, ox, oy, s, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		set( ow, "network", false );
		Dungeon.level = ow;
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	private static OverworldLevel windowAt( int wx, int wy ){
		return windowAt( 0, wx, wy );
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

	//the run's record through a real save and back
	private static void saveAndLoadWorld() throws Exception {
		Bundle g = new Bundle();
		HomesteadWorld.storeInBundle( g );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( g, out, false ) );
		HomesteadWorld.reset();
		HomesteadWorld.restoreFromBundle( Bundle.read( new ByteArrayInputStream( out.toByteArray() ) ) );
	}

	//the first village of people near the origin with this many houses or more, {sx, sy, cx, cy, houses}
	private static int[] human( int minHouses ){
		ArrayList<int[]> near = WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, minHouses, 8 );
		assertFalse( "a human village of " + minHouses + " houses near the origin of the test world", near.isEmpty() );
		return near.get( 0 );
	}

	private static long key( int[] s ){
		return WorldStructures.sectorOf( s[0], s[1] );
	}

	private static BountySlice slice( OverworldLevel ow ){
		BountySlice s = ow.homestead().section( BountySlice.class );
		assertNotNull( "the slice keeps the bounty section", s );
		return s;
	}

	private static List<NoticeBoard> boards( OverworldLevel ow, long key ){
		ArrayList<NoticeBoard> out = new ArrayList<>();
		for (Mob m : ow.mobs) if (m instanceof NoticeBoard && ((NoticeBoard) m).key == key) out.add( (NoticeBoard) m );
		return out;
	}

	//the offer of a kind on a village's board today, or null
	private static Bounty offerOf( long key, Bounty.Kind kind ){
		for (Bounty b : Bounties.openOffers( SEED, key, WorldClock.day(), BountyWorld.get() )) if (b.kind == kind) return b;
		return null;
	}

	//a village near the origin whose board shows a notice of this kind today
	private static int[] villageWith( Bounty.Kind kind ){
		for (int[] s : WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 0, 8 )){
			if (offerOf( key( s ), kind ) != null) return s;
		}
		throw new AssertionError( "no board near the origin shows a " + kind + " today" );
	}

	private void give( Class<? extends Item> cls, int n ){
		for (int i = 0; i < n; ){
			Item it = cls == Plant.Seed.class ? new xyz.gabriwar.warpedpixeldungeon.plants.Sungrass.Seed() : Reflection.newInstance( cls );
			assertNotNull( it );
			int q = it.stackable ? n - i : 1;
			it.quantity( q );
			assertTrue( it.collect( hero.belongings.backpack ) );
			i += q;
		}
	}

	private int carried( Class<? extends Item> cls ){
		int n = 0;
		for (Item i : hero.belongings.backpack) if (cls.isInstance( i )) n += i.quantity();
		return n;
	}

	// ------------------------------------------------------------------ the boards

	@Test
	public void aVillageOfPeopleGetsOneBoardByItsWellOnceAndForAll() throws Exception {
		int[] s = human( 4 );
		long key = key( s );
		OverworldLevel ow = windowAt( s[2], s[3] );
		slice( ow ).materialize( ow );
		slice( ow ).materialize( ow );
		List<NoticeBoard> found = boards( ow, key );
		assertEquals( "one board", 1, found.size() );
		NoticeBoard board = found.get( 0 );
		int dx = board.wx - s[2], dy = board.wy - s[3];
		assertTrue( "by the well", Math.max( Math.abs( dx ), Math.abs( dy ) ) <= 2 && (dx != 0 || dy != 0) );
		for (int[] post : GUARD_POSTS) assertFalse( "on a guard's post", post[0] == dx && post[1] == dy );
		assertEquals( ow.localCell( board.wx, board.wy ), board.pos );
		assertEquals( WorldStructures.villageName( SEED, s[0], s[1] ), board.place );
		assertTrue( Char.hasProp( board, Char.Property.IMMOVABLE ) );
		assertTrue( slice( ow ).placed( key ) );

		//the same world puts it on the same cell
		OverworldLevel again = windowAt( s[2], s[3] );
		slice( again ).materialize( again );
		assertEquals( board.wx, boards( again, key ).get( 0 ).wx );
		assertEquals( board.wy, boards( again, key ).get( 0 ).wy );

		//the window going far off and back: parked and put back, never a second
		Dungeon.level = ow;
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, s[2] + 900, s[3] );
		assertTrue( boards( ow, key ).isEmpty() );
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, s[2] + 10, s[3] - 5 );
		assertEquals( 1, boards( ow, key ).size() );
		assertEquals( ow.localCell( board.wx, board.wy ), boards( ow, key ).get( 0 ).pos );

		//and a save
		OverworldLevel loaded = saveAndLoad( ow );
		assertEquals( 1, boards( loaded, key ).size() );
		slice( loaded ).materialize( loaded );
		assertEquals( 1, boards( loaded, key ).size() );
		assertEquals( board.wx, boards( loaded, key ).get( 0 ).wx );
		assertEquals( key, boards( loaded, key ).get( 0 ).key );
	}

	@Test
	public void noBoardForGnollsOutlawsOrOffTheSurface(){
		int checked = 0;
		for (int[] s : WorldStructures.settlementsNear( SEED, 0, 0, null, 0, 6 )){
			if (WorldStructures.faction( SEED, s[0], s[1] ) == WorldStructures.Faction.HUMAN) continue;
			OverworldLevel ow = windowAt( s[2], s[3] );
			slice( ow ).materialize( ow );
			assertTrue( boards( ow, key( s ) ).isEmpty() );
			if (++checked >= 2) break;
		}
		assertTrue( "the test world has gnolls or outlaws near the origin", checked > 0 );
		int[] s = human( 1 );
		OverworldLevel cave = windowAt( -1, s[2], s[3] );
		slice( cave ).materialize( cave );
		for (Mob m : cave.mobs) assertFalse( m instanceof NoticeBoard );
	}

	@Test
	public void theWaystoneKeepsItsCellBesideTheBoard(){
		int[] s = human( Waystones.MIN_HOUSES );
		long key = key( s );
		//the waystone alone
		OverworldLevel alone = windowAt( s[2], s[3] );
		alone.homestead().section( TravelSlice.class ).materialize( alone );
		Waystone stone = null;
		for (Mob m : alone.mobs) if (m instanceof Waystone && ((Waystone) m).key == key) stone = (Waystone) m;
		assertNotNull( stone );
		//the waystones' section first, as HomesteadStore lists them, then the boards'
		List<String> order = Arrays.asList( xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadStore.SECTIONS );
		assertTrue( order.indexOf( TravelSlice.class.getName() ) < order.indexOf( BountySlice.class.getName() ) );
		OverworldLevel both = windowAt( s[2], s[3] );
		both.homestead().section( TravelSlice.class ).materialize( both );
		slice( both ).materialize( both );
		Waystone same = null;
		for (Mob m : both.mobs) if (m instanceof Waystone && ((Waystone) m).key == key) same = (Waystone) m;
		assertNotNull( same );
		assertEquals( stone.wx, same.wx );
		assertEquals( stone.wy, same.wy );
		NoticeBoard board = boards( both, key ).get( 0 );
		assertNotEquals( same.pos, board.pos );
	}

	//a board never goes down on a hero, who is no mob of the level: one standing where it would go
	//sends it on to the next cell, and so does an older save read back with him by the well
	@Test
	public void aBoardIsNeverPutDownOnAHero(){
		int[] s = human( 4 );
		long key = key( s );
		OverworldLevel ow = windowAt( s[2], s[3] );
		int first = BountySlice.cellFor( ow, s[0], s[1] );
		assertNotEquals( -1, first );
		hero.pos = first;
		slice( ow ).materialize( ow );
		assertEquals( 1, boards( ow, key ).size() );
		assertNotEquals( "not on the hero", first, boards( ow, key ).get( 0 ).pos );

		//a level being read back is nobody's yet (Dungeon.loadLevel): the hero's saved cell is his
		OverworldLevel loading = windowAt( s[2], s[3] );
		Dungeon.level = null;
		slice( loading ).materialize( loading );
		assertEquals( 1, boards( loading, key ).size() );
		assertNotEquals( "not on the hero", first, boards( loading, key ).get( 0 ).pos );
	}

	// ------------------------------------------------------------------ the record

	@Test
	public void theRunsRecordSurvivesASave() throws Exception {
		int[] s = villageWith( Bounty.Kind.DELIVER );
		long key = key( s );
		OverworldLevel ow = windowAt( s[2], s[3] );
		Statistics.deepestFloor = 7;
		Bounty deliver = offerOf( key, Bounty.Kind.DELIVER );
		assertTrue( BountyActions.take( hero, SEED, deliver ) );
		assertEquals( Bounties.reward( deliver ), deliver.reward );
		assertEquals( WorldClock.turn() + Bounties.deliverTime( deliver.dist ), deliver.deadline );
		Bounty other = null;
		for (Bounty b : Bounties.openOffers( SEED, key, WorldClock.day(), BountyWorld.get() )){
			if (b.kind != Bounty.Kind.DELIVER){
				other = b;
				break;
			}
		}
		assertNotNull( other );
		assertTrue( BountyActions.take( hero, SEED, other ) );
		if (other.kind == Bounty.Kind.HUNT) BountyWorld.get().count( other );
		BountyWorld.get().tell( other, Bounty.TOLD_READY, true );
		BountyWorld.get().setPoints( key, 4 );
		assertTrue( BountyWorld.get().isClosed( deliver.id ) );

		//the parcel in the pack, through a save of its own
		Parcel parcel = BountyActions.parcel( hero, deliver );
		assertNotNull( parcel );
		Bundle pb = new Bundle();
		pb.put( "parcel", parcel );
		ByteArrayOutputStream pout = new ByteArrayOutputStream();
		assertTrue( Bundle.write( pb, pout, false ) );
		Parcel back = (Parcel) Bundle.read( new ByteArrayInputStream( pout.toByteArray() ) ).get( "parcel" );
		assertEquals( parcel.bounty, back.bounty );
		assertEquals( parcel.to, back.to );
		assertEquals( parcel.from, back.from );
		assertEquals( parcel.deadline, back.deadline );

		saveAndLoadWorld();
		BountyWorld w = BountyWorld.get();
		assertEquals( 2, w.takenCount() );
		Bounty d = w.takenById( deliver.id ), o = w.takenById( other.id );
		assertNotNull( d );
		assertNotNull( o );
		assertEquals( Bounty.Kind.DELIVER, d.kind );
		assertEquals( deliver.dest, d.dest );
		assertEquals( deliver.dist, d.dist );
		assertEquals( deliver.reward, d.reward );
		assertEquals( deliver.deadline, d.deadline );
		assertEquals( deliver.takenTurn, d.takenTurn );
		assertEquals( other.kind, o.kind );
		assertEquals( other.target, o.target );
		assertEquals( other.need, o.need );
		assertEquals( other.bonus, o.bonus );
		assertEquals( other.kind == Bounty.Kind.HUNT ? 1 : 0, o.progress );
		assertTrue( o.told( Bounty.TOLD_READY ) );
		assertEquals( 4, w.points( key ) );
		assertTrue( w.isClosed( deliver.id ) && w.isClosed( other.id ) );
		//closed notices are forgotten once their period is over
		w.prune( Bounties.periodEnd( SEED, key, deliver.period ) );
		assertFalse( w.isClosed( deliver.id ) );

		//a save from before the boards has no record: nothing taken, no standing
		HomesteadWorld.reset();
		HomesteadWorld.restoreFromBundle( new Bundle() );
		assertEquals( 0, BountyWorld.get().takenCount() );
		assertEquals( 0, BountyWorld.get().points( key ) );
	}

	// ------------------------------------------------------------------ at the board

	@Test
	public void aHuntCountsNearItsVillageAndPaysAtItsBoard(){
		int[] s = villageWith( Bounty.Kind.HUNT );
		long key = key( s );
		OverworldLevel ow = windowAt( s[2], s[3] );
		Bounty hunt = offerOf( key, Bounty.Kind.HUNT );
		Bounties.Prey prey = Bounties.prey( hunt.target );
		assertTrue( BountyActions.take( hero, SEED, hunt ) );
		assertNull( "taken off its board", offerOf( key, Bounty.Kind.HUNT ) );
		int well = ow.localCell( s[2], s[3] );

		//a kill of another kind, or nobody's, counts for nothing
		Class<? extends Mob> otherKind = prey.cls == xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake.class
				? xyz.gabriwar.warpedpixeldungeon.actors.mobs.Slime.class : xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake.class;
		Mob other = Reflection.newInstance( otherKind );
		other.pos = well + 3;
		BountyActions.onKill( other, hero );
		Mob beast = Reflection.newInstance( prey.cls );
		beast.pos = well + 3;
		BountyActions.onKill( beast, null );
		assertEquals( 0, hunt.progress );
		assertFalse( BountyActions.ready( hero, hunt ) );

		//the hero's kills near the village count, up to what it asks and no further
		for (int i = 0; i < hunt.need + 2; i++){
			Mob m = Reflection.newInstance( prey.cls );
			m.pos = well + 3 + W * (i % 3);
			BountyActions.onKill( m, hero );
		}
		assertEquals( hunt.need, hunt.progress );
		assertTrue( BountyActions.ready( hero, hunt ) );
		assertTrue( BountyActions.readyVillages().contains( key ) );

		int before = Dungeon.gold;
		assertTrue( BountyActions.turnIn( hero, SEED, hunt ) );
		assertEquals( before + hunt.reward, Dungeon.gold );
		assertEquals( 1, BountyWorld.get().points( key ) );
		assertEquals( 0, BountyWorld.get().takenCount() );
		assertNull( "done notices stay off the board for their days", offerOf( key, Bounty.Kind.HUNT ) );
		assertFalse( BountyActions.readyVillages().contains( key ) );
	}

	//the hunt a village's board posts for a great beast now, or null
	private static Bounty beastNotice( OverworldLevel ow, long key ){
		for (Bounty b : Bounties.openOffers( ow, key, WorldClock.day(), BountyWorld.get() )){
			if (Bounties.beastKind( b ) != null) return b;
		}
		return null;
	}

	@Test
	public void aGreatBeastNearAVillageIsHuntedWhereverItFalls(){
		int[] s = villageWith( Bounty.Kind.HUNT );
		long key = key( s );
		OverworldLevel ow = windowAt( s[2], s[3] );
		int now = WorldClock.turn();
		//a great beast two sectors off, half a day into its days (one of the world's own may roam
		//nearer: the board asks for the nearest)
		int[] lair = RoamerEvent.debugLair( SEED, s[2], s[3], 2 * WorldStructures.SECTOR );
		assertNotNull( lair );
		WorldEvents.Event e = RoamerEvent.forcedRoamer( lair[0], lair[1], now - WorldEvents.FC / 2 );
		ow.forceEvent( e );
		Bounty hunt = beastNotice( ow, key );
		assertNotNull( hunt );
		assertTrue( hunt.deadline > now );
		assertEquals( 1, hunt.need );
		assertNotEquals( Bounty.Bonus.NONE, hunt.bonus );
		assertTrue( Bounties.title( SEED, hunt ).contains( RoamerEvent.title( Bounties.beastKind( hunt ), RoamerEvent.nameOf( hunt.beast ) ) ) );
		//the regular notices are as they were, and it survives a save with its beast
		assertEquals( Bounties.openOffers( SEED, key, WorldClock.day(), BountyWorld.get() ).size() + 1,
				Bounties.openOffers( ow, key, WorldClock.day(), BountyWorld.get() ).size() );
		Bundle bundle = new Bundle();
		hunt.storeInBundle( bundle );
		Bounty back = new Bounty();
		back.restoreFromBundle( bundle );
		assertEquals( hunt.beast, back.beast );
		assertTrue( Bounties.known( back ) );
		assertTrue( BountyActions.take( hero, SEED, hunt ) );
		assertNull( "taken off its board", beastNotice( ow, key ) );

		//another of its kind counts for nothing; the beast itself counts, far from the well
		RoamerEvent.Kind kind = Bounties.beastKind( hunt );
		Mob plain = kind.make();
		plain.pos = W / 2 + (H / 2) * W;
		BountyActions.onKill( plain, hero );
		assertEquals( 0, hunt.progress );
		Mob beast = kind.make();
		beast.pos = plain.pos;
		Buff.affect( beast, Roaming.class ).event = hunt.beast;
		windowAt( s[2] + Bounties.HUNT_REACH + 40, s[3] );
		BountyActions.onKill( beast, hero );
		assertEquals( 1, hunt.progress );
		int before = Dungeon.gold;
		assertTrue( BountyActions.turnIn( hero, SEED, hunt ) );
		assertEquals( before + hunt.reward, Dungeon.gold );

		//one whose days run out before it falls: the notice goes, and nothing is paid
		ow = windowAt( s[2], s[3] );
		//the first was slain (RoamerEvent.slain settles it): no longer abroad
		ow.resolveEvent( hunt.beast, hunt.deadline );
		WorldEvents.Event gone = RoamerEvent.forcedRoamer( lair[0], lair[1], now - WorldEvents.FC / 3 );
		ow.forceEvent( gone );
		Bounty late = beastNotice( ow, key );
		assertNotNull( late );
		assertNotEquals( hunt.beast, late.beast );
		assertTrue( BountyActions.take( hero, SEED, late ) );
		late.deadline = WorldClock.turn();
		BountyActions.tick( ow );
		assertEquals( 0, BountyWorld.get().takenCount() );
	}

	@Test
	public void aKillFarFromTheVillageOrUnderTheGroundDoesNotCount(){
		int[] s = villageWith( Bounty.Kind.HUNT );
		long key = key( s );
		Bounty hunt = offerOf( key, Bounty.Kind.HUNT );
		Bounties.Prey prey = Bounties.prey( hunt.target );
		assertTrue( BountyActions.take( hero, SEED, hunt ) );
		Mob m = Reflection.newInstance( prey.cls );
		m.pos = W / 2 + (H / 2) * W;
		//the middle of a window past the hunt's reach of the well
		windowAt( s[2] + Bounties.HUNT_REACH + 4, s[3] );
		BountyActions.onKill( m, hero );
		assertEquals( 0, hunt.progress );
		//under the village, in the caves
		windowAt( -1, s[2] + 3, s[3] );
		BountyActions.onKill( m, hero );
		assertEquals( 0, hunt.progress );
		//within reach on the surface it counts
		windowAt( s[2] + Bounties.HUNT_REACH - 10, s[3] );
		BountyActions.onKill( m, hero );
		assertEquals( 1, hunt.progress );
	}

	@Test
	public void aGatheringTakesItsGoodsFromThePack(){
		int[] s = villageWith( Bounty.Kind.GATHER );
		long key = key( s );
		windowAt( s[2], s[3] );
		Bounty gather = offerOf( key, Bounty.Kind.GATHER );
		Bounties.Good good = Bounties.good( gather.target );
		assertTrue( BountyActions.take( hero, SEED, gather ) );
		assertFalse( BountyActions.ready( hero, gather ) );
		assertFalse( BountyActions.turnIn( hero, SEED, gather ) );
		give( good.cls, gather.need + 2 );
		assertTrue( BountyActions.ready( hero, gather ) );
		int before = Dungeon.gold;
		assertTrue( BountyActions.turnIn( hero, SEED, gather ) );
		assertEquals( 2, carried( good.cls ) );
		assertEquals( before + gather.reward, Dungeon.gold );
		assertEquals( 1, BountyWorld.get().points( key ) );
	}

	//a quest's own piece never goes to a board: the rotberry seed the wandmaker waits for is no seed
	//for a gathering, neither counted nor taken
	@Test
	public void aQuestsOwnSeedIsNeverHandedIn(){
		int[] s = human( 1 );
		windowAt( s[2], s[3] );
		Bounty seeds = new Bounty();
		seeds.village = key( s );
		seeds.id = Bounties.offerId( seeds.village, -1, 7 );
		seeds.kind = Bounty.Kind.GATHER;
		seeds.target = "seeds";
		seeds.need = 2;
		seeds.coin = 48;
		assertTrue( BountyActions.take( hero, SEED, seeds ) );
		Bounties.Good good = Bounties.good( "seeds" );
		assertTrue( new xyz.gabriwar.warpedpixeldungeon.plants.Rotberry.Seed().collect( hero.belongings.backpack ) );
		give( Plant.Seed.class, 1 );
		assertEquals( 1, BountyActions.carried( hero, good ) );
		assertFalse( BountyActions.ready( hero, seeds ) );
		assertFalse( BountyActions.turnIn( hero, SEED, seeds ) );

		give( Plant.Seed.class, 1 );
		assertTrue( BountyActions.ready( hero, seeds ) );
		assertTrue( BountyActions.turnIn( hero, SEED, seeds ) );
		assertEquals( "the quest's seed stays", 1, carried( xyz.gabriwar.warpedpixeldungeon.plants.Rotberry.Seed.class ) );
		assertEquals( 0, carried( xyz.gabriwar.warpedpixeldungeon.plants.Sungrass.Seed.class ) );
	}

	@Test
	public void fiveNoticesAtOnce(){
		ArrayList<Bounty> took = new ArrayList<>();
		windowAt( 0, 0 );
		for (int[] s : WorldStructures.settlementsNear( SEED, 0, 0, WorldStructures.Faction.HUMAN, 0, 8 )){
			for (Bounty b : Bounties.openOffers( SEED, key( s ), WorldClock.day(), BountyWorld.get() )){
				if (took.size() < Bounties.MAX_TAKEN){
					assertNull( BountyActions.refusal( b ) );
					assertTrue( BountyActions.take( hero, SEED, b ) );
					took.add( b );
				} else {
					assertNotNull( BountyActions.refusal( b ) );
					assertFalse( BountyActions.take( hero, SEED, b ) );
					return;
				}
			}
		}
		throw new AssertionError( "not enough notices near the origin" );
	}

	// ------------------------------------------------------------------ the parcels

	//a window round a parcel's village with an elder of it standing by the well, the parcel taken
	private Villager parcelAndElder( Bounty deliver ){
		OverworldLevel ow = windowAt( Bounties.wellX( SEED, deliver.dest ), Bounties.wellY( SEED, deliver.dest ) );
		assertTrue( BountyActions.take( hero, SEED, deliver ) );
		assertNotNull( BountyActions.parcel( hero, deliver ) );
		VillageRoutine.Settlement v = VillageRoutine.settlement( SEED, deliver.dest );
		Villager elder = new Villager();
		elder.homeSector = deliver.dest;
		elder.role = VillageRoutine.Role.ELDER.ordinal();
		int h = Bounties.elderHouse( SEED, deliver.dest );
		elder.homeX = v.houseX( h );
		elder.homeY = v.houseY( h );
		elder.pos = ow.localCell( v.cx + 1, v.cy + 3 );
		ow.mobs.add( elder );
		return elder;
	}

	@Test
	public void aParcelGoesToAnElder(){
		int[] s = villageWith( Bounty.Kind.DELIVER );
		long from = key( s );
		Bounty deliver = offerOf( from, Bounty.Kind.DELIVER );
		Villager elder = parcelAndElder( deliver );
		OverworldLevel ow = (OverworldLevel) Dungeon.level;

		//anyone else of the village points the way to the elder's house
		Villager other = new Villager();
		other.homeSector = deliver.dest;
		other.role = VillageRoutine.Role.FARMER.ordinal();
		other.pos = ow.localCell( Bounties.wellX( SEED, deliver.dest ) - 1, Bounties.wellY( SEED, deliver.dest ) + 3 );
		ow.mobs.add( other );
		assertTrue( BountyActions.settlerTalk( other ) );
		assertNotNull( "still carried", BountyActions.parcel( hero, deliver ) );
		//a guard of the village too, whatever the standing
		OverworldGuard guard = OverworldGuard.random( deliver.dest );
		guard.pos = ow.localCell( Bounties.wellX( SEED, deliver.dest ) + 2, Bounties.wellY( SEED, deliver.dest ) );
		assertNotNull( BountyActions.guardLine( guard ) );
		//a villager of another village has nothing to say about it
		Villager stranger = new Villager();
		stranger.homeSector = from;
		assertFalse( BountyActions.settlerTalk( stranger ) );

		int before = Dungeon.gold;
		assertTrue( BountyActions.settlerTalk( elder ) );
		assertNull( "handed over", BountyActions.parcel( hero, deliver ) );
		assertEquals( before + deliver.reward, Dungeon.gold );
		assertEquals( "the sender's standing", 1, BountyWorld.get().points( from ) );
		assertEquals( 0, BountyWorld.get().takenCount() );
	}

	@Test
	public void aLateParcelEarnsNothing(){
		int[] s = villageWith( Bounty.Kind.DELIVER );
		long from = key( s );
		Bounty deliver = offerOf( from, Bounty.Kind.DELIVER );
		Villager elder = parcelAndElder( deliver );
		OverworldLevel ow = (OverworldLevel) Dungeon.level;

		//the turn's word: due soon, then overdue
		Dungeon.cycleTurn = deliver.deadline - 10;
		BountyActions.tick( ow );
		assertTrue( deliver.told( Bounty.TOLD_DUE ) );
		assertFalse( deliver.late );
		Dungeon.cycleTurn = deliver.deadline + 1;
		BountyActions.tick( ow );
		assertTrue( deliver.late );
		assertTrue( BountyActions.parcel( hero, deliver ).desc().length() > 0 );

		int before = Dungeon.gold;
		assertTrue( BountyActions.settlerTalk( elder ) );
		assertEquals( before, Dungeon.gold );
		assertEquals( 0, BountyWorld.get().points( from ) );
		assertEquals( 0, BountyWorld.get().takenCount() );
		assertNull( BountyActions.parcel( hero, deliver ) );
	}

	@Test
	public void givingUpPutsTheNoticeBackAndLeavesTheParcel(){
		int[] s = villageWith( Bounty.Kind.DELIVER );
		long from = key( s );
		windowAt( s[2], s[3] );
		Bounty deliver = offerOf( from, Bounty.Kind.DELIVER );
		assertTrue( BountyActions.take( hero, SEED, deliver ) );
		assertNull( offerOf( from, Bounty.Kind.DELIVER ) );
		assertNotNull( BountyActions.parcel( hero, deliver ) );
		BountyActions.abandon( hero, SEED, deliver );
		assertNull( BountyActions.parcel( hero, deliver ) );
		assertEquals( 0, BountyWorld.get().takenCount() );
		assertNotNull( "back up on its board", offerOf( from, Bounty.Kind.DELIVER ) );
		assertEquals( 0, BountyWorld.get().points( from ) );
	}

	// ------------------------------------------------------------------ standing

	@Test
	public void standingBringsBetterPricesAndFriendlierGuards(){
		//a village whose board shows a hunt today, so the guard's word on its beasts is heard
		int[] s = villageWith( Bounty.Kind.HUNT );
		long key = key( s );
		OverworldLevel ow = windowAt( s[2], s[3] );
		OverworldShopkeeper keeper = OverworldShopkeeper.of( OverworldShopkeeper.FOOD, key );
		OverworldGuard guard = OverworldGuard.random( key );
		guard.pos = ow.localCell( s[2] + 2, s[3] );
		assertEquals( 1f, keeper.priceFactor(), 1e-6f );
		assertNull( "a stranger gets the guard's own lines", BountyActions.guardLine( guard ) );

		BountyWorld.get().setPoints( key, 1 );
		assertEquals( 0.95f, keeper.priceFactor(), 1e-6f );
		String known = BountyActions.guardLine( guard );
		assertNotNull( known );
		assertFalse( known.contains( "NO TEXT" ) );

		//trusted, with a hunt for the village under way: where its beasts were last seen
		BountyWorld.get().setPoints( key, 3 );
		assertEquals( 0.9f, keeper.priceFactor(), 1e-6f );
		Bounty hunt = offerOf( key, Bounty.Kind.HUNT );
		assertNotNull( hunt );
		assertTrue( BountyActions.take( hero, SEED, hunt ) );
		String none = BountyActions.guardLine( guard );
		Mob beast = Reflection.newInstance( Bounties.prey( hunt.target ).cls );
		beast.pos = guard.pos + 6;
		ow.mobs.add( beast );
		String seen = BountyActions.guardLine( guard );
		assertNotEquals( none, seen );
		assertTrue( seen.contains( "6" ) );
		assertFalse( seen.contains( "NO TEXT" ) );

		BountyWorld.get().setPoints( key, 6 );
		assertEquals( 0.85f, keeper.priceFactor(), 1e-6f );
		//another village's traders know nothing of it
		OverworldShopkeeper elsewhere = OverworldShopkeeper.of( OverworldShopkeeper.FOOD, WorldStructures.sectorOf( s[0] + 40, s[1] ) );
		assertEquals( 1f, elsewhere.priceFactor(), 1e-6f );
	}

	// ------------------------------------------------------------------ co-op

	@Test
	public void aGuestReadsTheHostsCopy() throws Exception {
		int[] s = villageWith( Bounty.Kind.HUNT );
		long key = key( s );
		OverworldLevel ow = windowAt( s[2], s[3] );
		Bounty hunt = offerOf( key, Bounty.Kind.HUNT );
		assertTrue( BountyActions.take( hero, SEED, hunt ) );
		for (int i = 0; i < hunt.need; i++) BountyWorld.get().count( hunt );
		BountyWorld.get().setPoints( key, 2 );
		BountyActions.refreshReady();
		long sig = BountyShare.sig();

		BountyShare.adopt( BountyShare.text() );
		Field f = BountyShare.class.getDeclaredField( "guestWorld" );
		f.setAccessible( true );
		BountyWorld guest = (BountyWorld) f.get( null );
		assertEquals( 1, guest.takenCount() );
		assertEquals( hunt.need, guest.takenById( hunt.id ).progress );
		assertEquals( 2, guest.points( key ) );
		assertTrue( guest.isClosed( hunt.id ) );
		Field r = BountyShare.class.getDeclaredField( "guestReady" );
		r.setAccessible( true );
		assertTrue( Arrays.toString( (long[]) r.get( null ) ), Arrays.asList( toBoxed( (long[]) r.get( null ) ) ).contains( key ) );

		//the board's words for a guest, with the notices still up
		String text = BountyActions.boardText( SEED, key );
		assertFalse( text.contains( "NO TEXT" ) );
		assertTrue( text.length() > 40 );

		//the fingerprint moves with what the guests see
		BountyWorld.get().setPoints( key, 3 );
		assertNotEquals( sig, BountyShare.sig() );
		//garbage is ignored, the copy kept
		BountyShare.adopt( "" );
		BountyShare.adopt( null );
		assertEquals( 1, ((BountyWorld) f.get( null )).takenCount() );
	}

	private static Long[] toBoxed( long[] a ){
		Long[] out = new Long[a.length];
		for (int i = 0; i < a.length; i++) out[i] = a[i];
		return out;
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Languages;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The villages' notice boards as a function of the world (Bounties): which villages keep one, the
 * notices a board shows - the same on every call, turning over every three days on each village's own
 * day - asking only for beasts that live round the village (the surface fauna's own hostile rows) and
 * goods its land gives, sending parcels only to villages with an elder; the reward's step with the
 * floors, the standing's ranks and prices; and every word of it in English and Portuguese.
 */
public class BountiesTest {

	private static final long SEED = 0x5EED0F7EA7L, SEED2 = 0xC0FFEE1234L;

	private int depth, deepest;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		deepest = Statistics.deepestFloor;
		Dungeon.depth = OverworldLevel.DEPTH;
		Random.pushGenerator( 777 );
	}

	@After
	public void tearDown(){
		Random.popGenerator();
		Dungeon.depth = depth;
		Statistics.deepestFloor = deepest;
		Messages.setup( Languages.ENGLISH );
	}

	//the human villages within some sectors of the origin, {sx, sy, cx, cy, houses}
	private static ArrayList<int[]> villages( long seed, int sectors ){
		return WorldStructures.settlementsNear( seed, 0, 0, WorldStructures.Faction.HUMAN, 0, sectors );
	}

	private static long key( int[] s ){
		return WorldStructures.sectorOf( s[0], s[1] );
	}

	private static String sig( Bounty b ){
		return b.id + "/" + b.village + "/" + b.period + "/" + b.slot + "/" + b.kind + "/" + b.target + "/" + b.need
				+ "/" + b.coin + "/" + b.bonus + "/" + b.dest + "/" + b.dist;
	}

	@Test
	public void onlyVillagesOfPeopleKeepABoard(){
		int boards = 0, others = 0;
		for (int[] s : WorldStructures.settlementsNear( SEED, 0, 0, null, 0, 6 )){
			boolean human = WorldStructures.faction( SEED, s[0], s[1] ) == WorldStructures.Faction.HUMAN;
			assertEquals( human, Bounties.keepsBoard( SEED, s[0], s[1] ) );
			if (human) boards++;
			else others++;
		}
		assertTrue( "the test world has human villages near the origin", boards >= 5 );
		assertTrue( "and gnoll clans or outlaws", others >= 1 );
		//no site at all, and no key
		assertFalse( Bounties.keepsBoard( SEED, Long.MIN_VALUE ) );
		assertTrue( Bounties.offers( SEED, Long.MIN_VALUE, 3 ).isEmpty() );
	}

	@Test
	public void aBoardsNoticesAreAPureFunctionOfTheWorld(){
		for (int[] s : villages( SEED, 4 )){
			long key = key( s );
			for (int period = -2; period < 6; period++){
				ArrayList<Bounty> a = Bounties.offers( SEED, key, period );
				ArrayList<Bounty> b = Bounties.offers( SEED, key, period );
				assertEquals( a.size(), b.size() );
				for (int i = 0; i < a.size(); i++) assertEquals( sig( a.get( i ) ), sig( b.get( i ) ) );
			}
		}
		//the land round a village is the world's: another world's board is another board
		long key = key( villages( SEED, 4 ).get( 0 ) );
		StringBuilder one = new StringBuilder(), two = new StringBuilder();
		for (int p = 0; p < 8; p++){
			for (Bounty b : Bounties.offers( SEED, key, p )) one.append( sig( b ) );
			Bounties.land( SEED2, key );
			for (Bounty b : Bounties.offers( SEED, key, p )) two.append( sig( b ) );
		}
		assertEquals( "a cache kept for another seed changes nothing", one.toString(), two.toString() );
	}

	@Test
	public void aBoardShowsThreeNoticesTwoInAHamletEachItsOwn(){
		HashSet<Long> ids = new HashSet<>();
		int hamlets = 0, delivering = 0;
		for (int[] s : villages( SEED, 6 )){
			long key = key( s );
			for (int period = 0; period < 12; period++){
				ArrayList<Bounty> offers = Bounties.offers( SEED, key, period );
				int want = s[4] < Bounties.SMALL_HOUSES ? Bounties.SMALL_NOTICES : Bounties.BOARD_NOTICES;
				assertEquals( want, offers.size() );
				HashSet<String> asked = new HashSet<>();
				HashSet<Bounty.Kind> kinds = new HashSet<>();
				for (int slot = 0; slot < offers.size(); slot++){
					Bounty b = offers.get( slot );
					assertEquals( slot, b.slot );
					assertEquals( key, b.village );
					assertEquals( period, b.period );
					assertEquals( Bounties.offerId( key, period, slot ), b.id );
					assertTrue( "ids are unique", ids.add( b.id ) );
					assertTrue( Bounties.known( b ) );
					assertTrue( b.need >= 1 && b.coin > 0 );
					assertTrue( "one notice per beast or good on a board", b.kind == Bounty.Kind.DELIVER || asked.add( b.target ) );
					kinds.add( b.kind );
				}
				//the three kinds, while the land allows them all and the board has room
				if (offers.size() == 3 && !Bounties.deliverTargets( SEED, key ).isEmpty()
						&& huntable( Bounties.land( SEED, key ) )){
					assertEquals( 3, kinds.size() );
				}
				if (kinds.contains( Bounty.Kind.DELIVER )) delivering++;
			}
			if (s[4] < Bounties.SMALL_HOUSES) hamlets++;
		}
		assertTrue( "the test world has a hamlet near the origin", hamlets > 0 );
		assertTrue( "and parcels to carry", delivering > 0 );
	}

	private static boolean huntable( EnumSet<WorldModel.Biome> land ){
		for (Bounties.Prey p : Bounties.PREY){
			for (WorldModel.Biome b : p.land) if (land.contains( b )) return true;
		}
		return false;
	}

	@Test
	public void huntsAskForBeastsOfTheLandAndGatheringsForItsGoods(){
		int hunts = 0, gatherings = 0;
		HashSet<String> prey = new HashSet<>(), goods = new HashSet<>();
		for (int[] s : villages( SEED, 6 )){
			long key = key( s );
			EnumSet<WorldModel.Biome> land = Bounties.land( SEED, key );
			for (int period = 0; period < 10; period++){
				for (Bounty b : Bounties.offers( SEED, key, period )){
					if (b.kind == Bounty.Kind.HUNT){
						Bounties.Prey p = Bounties.prey( b.target );
						boolean lives = false;
						for (WorldModel.Biome bio : p.land) lives |= land.contains( bio );
						assertTrue( b.target + " lives round " + Bounties.villageName( SEED, key ), lives );
						assertTrue( b.need >= p.min && b.need <= p.max );
						assertEquals( 20 + b.need * p.coin, b.coin );
						hunts++;
						prey.add( b.target );
					} else if (b.kind == Bounty.Kind.GATHER){
						Bounties.Good g = Bounties.good( b.target );
						if (g.land != null){
							boolean grows = false;
							for (WorldModel.Biome bio : g.land) grows |= land.contains( bio );
							assertTrue( b.target + " comes from the land round " + Bounties.villageName( SEED, key ), grows );
						}
						assertTrue( b.need >= g.min && b.need <= g.max );
						assertEquals( 20 + b.need * g.coin, b.coin );
						gatherings++;
						goods.add( b.target );
					}
				}
			}
		}
		assertTrue( hunts > 20 && gatherings > 20 );
		assertTrue( "a variety of beasts: " + prey, prey.size() >= 3 );
		assertTrue( "a variety of goods: " + goods, goods.size() >= 5 );
	}

	//every beast a hunt may ask for is one the surface's fauna puts down, hostile, on each of its lands
	@Test
	public void thePreyIsTheSurfaceFaunasOwn(){
		HashMap<WorldModel.Biome, HashSet<Class<?>>> hostile = new HashMap<>();
		for (WorldModel.Biome b : WorldModel.Biome.values()){
			HashSet<Class<?>> kinds = new HashSet<>();
			for (DayNightCycle.Phase ph : DayNightCycle.Phase.values()){
				for (int i = 0; i < 300; i++){
					for (Mob m : OverworldFauna.ambushers( 0, b, ph, OverworldFauna.Habitat.DRY.bit )) kinds.add( m.getClass() );
				}
			}
			hostile.put( b, kinds );
		}
		for (Bounties.Prey p : Bounties.PREY){
			for (WorldModel.Biome b : p.land){
				boolean found = false;
				for (Class<?> c : hostile.get( b )) found |= p.cls.isAssignableFrom( c );
				assertTrue( p.id + " is a hostile beast of the " + b + " (" + hostile.get( b ) + ")", found );
			}
		}
	}

	@Test
	public void parcelsGoToVillagesWithAnElder(){
		int checked = 0;
		for (int[] s : villages( SEED, 5 )){
			long key = key( s );
			List<Long> targets = Bounties.deliverTargets( SEED, key );
			for (long t : targets){
				assertNotEquals( key, t );
				assertTrue( Bounties.keepsBoard( SEED, t ) );
				assertTrue( Bounties.elderHouse( SEED, t ) >= 0 );
				assertTrue( Math.abs( (int)(t >> 32) - s[0] ) <= Bounties.DELIVER_SECTORS );
				assertTrue( Math.abs( (int) t - s[1] ) <= Bounties.DELIVER_SECTORS );
			}
			for (int period = 0; period < 10; period++){
				for (Bounty b : Bounties.offers( SEED, key, period )){
					if (b.kind != Bounty.Kind.DELIVER) continue;
					assertTrue( targets.contains( b.dest ) );
					assertEquals( Bounties.distance( SEED, key, b.dest ), b.dist );
					assertEquals( 25 + b.dist / 3, b.coin );
					checked++;
				}
			}
		}
		assertTrue( checked > 5 );
	}

	//the elder's house is one whose family, as the village puts them down, has an elder in it
	@Test
	public void anElderLivesInTheElderHouse(){
		int elders = 0, none = 0;
		for (int[] s : villages( SEED, 6 )){
			long key = key( s );
			int h = Bounties.elderHouse( SEED, key );
			VillageRoutine.Settlement v = VillageRoutine.settlement( SEED, key );
			boolean any = false;
			for (int house = v.firstFamily(); house < v.lastFamily(); house++){
				long fh = VillageRoutine.familyHash( v.sx, v.sy, house );
				int members = 1 + (int) Math.floorMod( fh, 3L );
				boolean here = false;
				for (int m = 0; m < members; m++) here |= VillageRoutine.roleOf( fh, m, false ) == VillageRoutine.Role.ELDER;
				if (here && !any){
					assertEquals( "the first such house", house, h );
					any = true;
				}
			}
			if (!any) assertEquals( -1, h );
			if (any) elders++;
			else none++;
		}
		assertTrue( elders > 3 );
		assertTrue( "some villages have no elder", none > 0 );
	}

	@Test
	public void boardsTurnOverEveryThreeDaysEachOnItsOwnDay(){
		HashSet<Integer> phases = new HashSet<>();
		for (int[] s : villages( SEED, 6 )){
			long key = key( s );
			int ph = Bounties.phase( SEED, key );
			assertTrue( ph >= 0 && ph < Bounties.BOARD_DAYS );
			phases.add( ph );
			int turns = 0;
			for (int day = -10; day < 40; day++){
				int p = Bounties.period( SEED, key, day );
				if (Bounties.period( SEED, key, day + 1 ) != p){
					turns++;
					assertEquals( day + 1, Bounties.periodEnd( SEED, key, p ) );
					assertEquals( p + 1, Bounties.period( SEED, key, day + 1 ) );
				}
				assertTrue( Bounties.periodEnd( SEED, key, p ) > day );
				assertTrue( Bounties.periodEnd( SEED, key, p ) - day <= Bounties.BOARD_DAYS );
			}
			assertTrue( turns >= 16 && turns <= 17 );
		}
		assertTrue( "the villages do not all turn over on the same day", phases.size() > 1 );
	}

	@Test
	public void rewardsStandingAndPrices(){
		Bounty b = new Bounty();
		b.coin = 84;
		Statistics.deepestFloor = 0;
		assertEquals( 84, Bounties.reward( b ) );
		Statistics.deepestFloor = 12;
		assertEquals( 84 * 3, Bounties.reward( b ) );

		assertEquals( 0, Bounties.rank( 0 ) );
		assertEquals( 1, Bounties.rank( 1 ) );
		assertEquals( 1, Bounties.rank( 2 ) );
		assertEquals( 2, Bounties.rank( 3 ) );
		assertEquals( 2, Bounties.rank( 5 ) );
		assertEquals( 3, Bounties.rank( 6 ) );
		assertEquals( 3, Bounties.rank( 600 ) );
		assertEquals( 1f, Bounties.priceAt( 0 ), 1e-6f );
		assertEquals( 0.95f, Bounties.priceAt( 1 ), 1e-6f );
		assertEquals( 0.85f, Bounties.priceAt( 3 ), 1e-6f );

		assertEquals( Bounties.DELIVER_BASE + Bounties.DELIVER_PER_CELL * 100, Bounties.deliverTime( 100 ) );
		assertEquals( 24, Bounties.hours( xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEvents.FC ) );
		assertEquals( 1, Bounties.hours( 1 ) );

		//the bonus comes on about one notice in three
		int bonus = 0, all = 0;
		for (int[] s : villages( SEED, 6 )){
			for (int p = 0; p < 20; p++){
				for (Bounty o : Bounties.offers( SEED, key( s ), p )){
					all++;
					if (o.bonus != Bounty.Bonus.NONE) bonus++;
				}
			}
		}
		assertTrue( bonus + " of " + all, bonus > all / 5 && bonus < all / 2 );
	}

	@Test
	public void theEightWays(){
		assertEquals( Messages.get( Bounties.class, "dir_n" ), Bounties.direction( 0, -7 ) );
		assertEquals( Messages.get( Bounties.class, "dir_s" ), Bounties.direction( 1, 9 ) );
		assertEquals( Messages.get( Bounties.class, "dir_e" ), Bounties.direction( 8, 1 ) );
		assertEquals( Messages.get( Bounties.class, "dir_w" ), Bounties.direction( -8, 0 ) );
		assertEquals( Messages.get( Bounties.class, "dir_ne" ), Bounties.direction( 5, -5 ) );
		assertEquals( Messages.get( Bounties.class, "dir_sw" ), Bounties.direction( -4, 5 ) );
	}

	private static Properties load( String path ) throws IOException {
		Properties p = new Properties();
		try (InputStreamReader r = new InputStreamReader( new FileInputStream( path ), StandardCharsets.UTF_8 )){
			p.load( r );
		}
		return p;
	}

	//every word of the boards, in both languages: the same keys, no dash of the long kind, and every
	//key the code builds resolves
	@Test
	public void everyWordInEnglishAndPortuguese() throws IOException {
		Properties en = load( "src/main/assets/messages/bounty/bounty.properties" );
		Properties pt = load( "src/main/assets/messages/bounty/bounty_pt.properties" );
		assertEquals( en.stringPropertyNames(), pt.stringPropertyNames() );
		for (Properties p : new Properties[]{ en, pt }){
			for (String k : p.stringPropertyNames()){
				assertFalse( k, p.getProperty( k ).contains( "—" ) || p.getProperty( k ).contains( "–" ) );
			}
		}
		//a translation takes the same things in: the same format specifiers, in any order
		java.util.regex.Pattern spec = java.util.regex.Pattern.compile( "%(\\d+\\$)?[sd]" );
		for (String k : en.stringPropertyNames()){
			ArrayList<String> a = new ArrayList<>(), b = new ArrayList<>();
			java.util.regex.Matcher m = spec.matcher( en.getProperty( k ) );
			while (m.find()) a.add( m.group() );
			m = spec.matcher( pt.getProperty( k ) );
			while (m.find()) b.add( m.group() );
			java.util.Collections.sort( a );
			java.util.Collections.sort( b );
			assertEquals( k, a, b );
		}
		for (Languages lang : new Languages[]{ Languages.ENGLISH, Languages.PORTUGUESE }){
			Messages.setup( lang );
			//every key is loaded for the language (the bundle is registered)
			for (String k : en.stringPropertyNames()){
				String got = Messages.get( k );
				assertFalse( lang + ": " + k, got.contains( "NO TEXT" ) );
				if (lang == Languages.PORTUGUESE) assertEquals( k, pt.getProperty( k ), got );
			}
			ArrayList<String> words = new ArrayList<>();
			for (Bounties.Prey p : Bounties.PREY){
				words.add( Messages.get( Bounties.class, "prey_" + p.id ) );
				words.add( Messages.get( Bounties.class, "prey_" + p.id + "_1" ) );
				words.add( Messages.get( Bounties.class, "hint_prey_" + p.id ) );
			}
			for (Bounties.Good g : Bounties.GOODS){
				words.add( Messages.get( Bounties.class, "good_" + g.id ) );
				words.add( Messages.get( Bounties.class, "hint_good_" + g.id ) );
			}
			for (Bounty.Kind k : Bounty.Kind.values()){
				words.add( Messages.get( Bounties.class, "title_" + k.name().toLowerCase( java.util.Locale.ENGLISH ), "x" ) );
			}
			for (Bounty.Bonus b : Bounty.Bonus.values()) words.add( Bounties.pay( 10, b ) );
			for (int r = 1; r <= 3; r++){
				words.add( Bounties.rankName( r ) );
				words.add( Messages.get( Bounties.class, "rank_up_" + r, "Belford", 5 * r ) );
				for (int i = 0; i < 3; i++) words.add( Messages.get( Bounties.class, "guard_rank" + r + "_" + i, "Belford" ) );
			}
			for (int[] d : new int[][]{ {0,-1}, {1,-1}, {1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1} }){
				words.add( Bounties.direction( d[0], d[1] ) );
			}
			for (int[] s : villages( SEED, 3 )){
				for (Bounty b : Bounties.offers( SEED, key( s ), 4 )){
					words.add( Bounties.title( SEED, b ) );
					words.add( Bounties.hint( SEED, b ) );
				}
			}
			words.add( new NoticeBoard().name() );
			words.add( new NoticeBoard().description() );
			Parcel parcel = new Parcel();
			words.add( parcel.name() );
			words.add( parcel.desc() );
			for (String w : words){
				assertNotNull( w );
				assertFalse( lang + ": " + w, w.contains( "NO TEXT" ) );
			}
		}
	}
}

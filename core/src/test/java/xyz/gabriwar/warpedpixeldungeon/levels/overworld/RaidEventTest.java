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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Raider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The bandit raids (RaidEvent, Raider): on paper - a pure function of the seed, the sector and
 * the day, rare, on human villages only, never two near each other, and likelier beside the
 * outlaws - and on a real window of the world: the smoke heard of once, the band let out of the
 * doors once and never doubled while it lives (in the window or parked), the folk indoors and
 * the shops shut, the watch wearing raiders down without finishing one, the band beaten off for
 * a purse and a prize and three days of better prices, a raid nobody answered shutting the shops
 * for two days, the leftovers making off with the day; and the raid's state through a save, and
 * an older save without it.
 */
public class RaidEventTest {

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
		//the prize is identified, and identifying reaches the badges
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
		//no rain: a shower would send the folk indoors on its own
		ClimateManager.debugPrecipOverride = 0f;
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
		Dungeon.seed = dungeonSeed;
		ClimateManager.debugPrecipOverride = precip;
		DayNightCycle.debugPhaseOverride = override;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers

	//a town of the test seed with a watch (nine houses or more), and a day it is raided with no
	//other raid on it from three days before to four after: {sx, sy, day}
	private static int[] townRaid;

	private static int[] townRaid(){
		if (townRaid != null) return townRaid;
		for (int r = 0; r <= 10 && townRaid == null; r++){
			for (int sy = -r; sy <= r && townRaid == null; sy++){
				for (int sx = -r; sx <= r && townRaid == null; sx++){
					if (Math.max( Math.abs( sx ), Math.abs( sy ) ) != r) continue;
					if (WorldStructures.siteType( SEED, sx, sy ) != WorldStructures.Site.VILLAGE
							|| WorldStructures.faction( SEED, sx, sy ) != WorldStructures.Faction.HUMAN
							|| houses( sx, sy ) < 9) continue;
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

	private static int houses( int sx, int sy ){
		return (WorldStructures.settlementLayout( SEED, sx, sy ).length - 1) / 2;
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

	private static boolean act( Actor a ){
		return (Boolean) invoke( a, Actor.class, "act", new Class<?>[0] );
	}

	//the live surface window round a world cell, built as a mirror's is (no populator) and made
	//the host's own; the hero on the nearest open cell to it, seeing nothing
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

	private static ArrayList<Raider> raiders( OverworldLevel ow ){
		ArrayList<Raider> out = new ArrayList<>();
		for (Mob m : ow.mobs) if (m instanceof Raider) out.add( (Raider) m );
		return out;
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

	//a sprite that draws nothing and keeps the status lines shown over it
	private static CharSprite recorder( final ArrayList<String> shown ){
		CharSprite sp = new CharSprite(){
			@Override public void place( int cell ){ }
			@Override public void turnTo( int from, int to ){ }
			@Override public void update(){ }
			@Override public void die(){ }
			@Override public void showStatus( int color, String text, Object... args ){ shown.add( text ); }
		};
		sp.visible = false;
		return sp;
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

	private static boolean said( ArrayList<String> lines, String text ){
		for (String l : lines) if (l.contains( text )) return true;
		return false;
	}

	// ------------------------------------------------------------------ the schedule

	@Test
	public void raidScheduleIsAPureFunction(){
		ArrayList<String> first = new ArrayList<>(), second = new ArrayList<>();
		for (int d = 0; d < 40; d++){
			for (int sy = -6; sy <= 6; sy++){
				for (int sx = -6; sx <= 6; sx++){
					if (RaidEvent.raidedOn( SEED, sx, sy, d )) first.add( sx + "," + sy + "@" + d );
				}
			}
		}
		//another world's sites and outlaws in between: the caches are dropped
		WorldStructures.siteType( SEED ^ 1, 0, 0 );
		RaidEvent.nearBandits( SEED ^ 1, 0, 0 );
		for (int d = 0; d < 40; d++){
			for (int sy = 6; sy >= -6; sy--){
				for (int sx = 6; sx >= -6; sx--){
					if (RaidEvent.raidedOn( SEED, sx, sy, d )) second.add( sx + "," + sy + "@" + d );
				}
			}
		}
		java.util.Collections.sort( first );
		java.util.Collections.sort( second );
		assertEquals( first, second );
		assertFalse( "no raid at all in 40 days round the origin", first.isEmpty() );
		//the event the world reads is that raid, its whole day, by the well, with its natural id
		String[] one = first.get( 0 ).split( "[,@]" );
		int sx = Integer.parseInt( one[0] ), sy = Integer.parseInt( one[1] ), d = Integer.parseInt( one[2] );
		WorldEvents.Event e = RaidEvent.raidEvent( SEED, sx, sy, d );
		assertNotNull( e );
		assertEquals( WorldEvents.Type.RAID, e.type );
		assertEquals( d * FC, e.startTurn );
		assertEquals( (d + 1) * FC, e.endTurn );
		assertEquals( WorldStructures.siteX( SEED, sx, sy ), e.wx );
		assertEquals( WorldStructures.siteY( SEED, sx, sy ), e.wy );
		assertEquals( WorldEvents.idOf( WorldEvents.Type.RAID, sx, sy, d ), e.id );
		boolean listed = false;
		for (WorldEvents.Event n : WorldEvents.eventsNear( SEED, sx, sy, 1, d, null )) listed |= n.id == e.id;
		assertTrue( "the world's enumeration lists the raid", listed );
	}

	@Test
	public void raidsAreRareHumanAndNeverCrowd(){
		int raids = 0, villageDays = 0;
		for (int d = 0; d < 150; d++){
			ArrayList<int[]> today = new ArrayList<>();
			for (int sy = -12; sy <= 12; sy++){
				for (int sx = -12; sx <= 12; sx++){
					boolean human = WorldStructures.siteType( SEED, sx, sy ) == WorldStructures.Site.VILLAGE
							&& WorldStructures.faction( SEED, sx, sy ) == WorldStructures.Faction.HUMAN;
					if (human) villageDays++;
					if (!RaidEvent.raidedOn( SEED, sx, sy, d )) continue;
					assertTrue( "a raid on no human village at " + sx + "," + sy, human );
					raids++;
					today.add( new int[]{ sx, sy } );
				}
			}
			for (int i = 0; i < today.size(); i++){
				for (int j = i + 1; j < today.size(); j++){
					int[] a = today.get( i ), b = today.get( j );
					assertTrue( "two raids within " + RaidEvent.EXCLUSION + " sectors on day " + d,
							Math.max( Math.abs( a[0] - b[0] ), Math.abs( a[1] - b[1] ) ) > RaidEvent.EXCLUSION );
				}
			}
		}
		double rate = raids / (double) villageDays;
		System.out.println( "[raids] " + raids + " raids in " + villageDays + " human village-days: one in "
				+ Math.round( 1 / rate ) );
		assertTrue( "no raids at all", raids > 0 );
		assertTrue( "raids too common: " + rate, rate <= 1.0 / RaidEvent.ODDS_NEAR_BANDITS );
		assertTrue( "raids too rare: " + rate, rate >= 1.0 / 2000 );

		//from a hero's side: how often one is on within his hearing (HEAR sectors of his own), and
		//never two at once
		int heroDays = 0, heard = 0;
		for (int hy = -8; hy <= 8; hy++){
			for (int hx = -8; hx <= 8; hx++){
				for (int d = 0; d < 150; d++){
					heroDays++;
					int within = 0;
					for (int sy = hy - RaidEvent.HEAR; sy <= hy + RaidEvent.HEAR; sy++){
						for (int sx = hx - RaidEvent.HEAR; sx <= hx + RaidEvent.HEAR; sx++){
							if (RaidEvent.raidedOn( SEED, sx, sy, d )) within++;
						}
					}
					assertTrue( "two raids within a hero's hearing on day " + d, within <= 1 );
					if (within > 0) heard++;
				}
			}
		}
		double every = heroDays / (double) heard;
		System.out.println( "[raids] a hero hears of one every " + Math.round( every * 10 ) / 10.0 + " days" );
		assertTrue( "heard of too often: every " + every + " days", every >= 3 );
		assertTrue( "heard of too seldom: every " + every + " days", every <= 12 );
	}

	@Test
	public void banditNeighboursDrawMoreRaids(){
		int nearDays = 0, nearRaids = 0, farDays = 0, farRaids = 0;
		for (int sy = -12; sy <= 12; sy++){
			for (int sx = -12; sx <= 12; sx++){
				if (WorldStructures.siteType( SEED, sx, sy ) != WorldStructures.Site.VILLAGE
						|| WorldStructures.faction( SEED, sx, sy ) != WorldStructures.Faction.HUMAN) continue;
				boolean near = RaidEvent.nearBandits( SEED, sx, sy );
				for (int d = 0; d < 150; d++){
					boolean raided = RaidEvent.raidedOn( SEED, sx, sy, d );
					if (near){
						nearDays++;
						if (raided) nearRaids++;
					} else {
						farDays++;
						if (raided) farRaids++;
					}
				}
			}
		}
		System.out.println( "[raids] beside outlaws: " + nearRaids + " in " + nearDays + " village-days; away from them: "
				+ farRaids + " in " + farDays );
		org.junit.Assume.assumeTrue( "too few villages on one side of the split to compare", nearDays >= 300 && farDays >= 300 );
		assertTrue( "the outlaws' neighbours are raided less often than the rest",
				nearRaids / (double) nearDays >= farRaids / (double) farDays );
	}

	//a city or a metropolis can roll no house at all (WorldStructures.settlementLayout): no raid
	//ever falls on one, over several worlds - it has nothing to burn and no door for a band to
	//come out of, and the fires and the band pick their houses by dividing by how many there are
	@Test
	public void noVillageWithoutAHouseIsEverRaided(){
		int bare = 0, drawn = 0;
		for (long seed = 1; seed <= 4; seed++){
			for (int sy = -40; sy <= 40; sy++){
				for (int sx = -40; sx <= 40; sx++){
					if (WorldStructures.siteType( seed, sx, sy ) != WorldStructures.Site.VILLAGE
							|| WorldStructures.faction( seed, sx, sy ) != WorldStructures.Faction.HUMAN
							|| (WorldStructures.settlementLayout( seed, sx, sy ).length - 1) / 2 != 0) continue;
					bare++;
					for (int d = 0; d < 200; d++){
						//its own roll would have struck it
						if (RaidEvent.roll( seed, sx, sy, d ) < 1.0 / RaidEvent.ODDS) drawn++;
						assertFalse( "a raid on a village with no house: " + seed + " (" + sx + ", " + sy + ") day " + d,
								RaidEvent.raidedOn( seed, sx, sy, d ) );
					}
				}
			}
		}
		System.out.println( "[raids] " + bare + " human villages with no house, " + drawn + " days their roll came up" );
		assertTrue( "no human village without a house in the sample", bare > 0 );
		assertTrue( "none of them ever drew a raid's roll", drawn > 0 );
	}

	// ------------------------------------------------------------------ on the ground

	@Test
	public void aRaidGoesUpInSmokeAndItsBandComesOutOnce(){
		int[] t = townRaid();
		long sector = WorldStructures.sectorOf( t[0], t[1] );
		long id = WorldEvents.idOf( WorldEvents.Type.RAID, t[0], t[1], t[2] );
		OverworldLevel ow = raidedTown();
		String name = WorldStructures.villageName( SEED, t[0], t[1] );

		ArrayList<String> lines = logged( () -> RaidEvent.onHeroStep( ow ) );
		assertTrue( "the smoke is heard of", said( lines, Messages.get( RaidEvent.class, "smoke_here", name ) ) );
		assertTrue( "the band's coming out is told", said( lines, Messages.get( RaidEvent.class, "live", name ) ) );
		assertTrue( "pinned: heard of", ow.eventAnnounced( id ) );
		assertTrue( RaidEvent.ongoing( sector ) );

		ArrayList<Raider> band = raiders( ow );
		RaidEvent.Raid raid = new RaidEvent.Raid( SEED, RaidEvent.raidEvent( SEED, t[0], t[1], t[2] ) );
		assertEquals( raid.raiders(), band.size() );
		assertTrue( band.size() >= RaidEvent.MIN_BAND && band.size() <= RaidEvent.MAX_BAND );
		int fighters = 0;
		for (Raider r : band){
			assertEquals( sector, r.sector );
			assertEquals( t[2], r.day );
			assertEquals( Char.Alignment.ENEMY, r.alignment );
			assertTrue( "never counted in a camp", r.sentOnEvent() );
			assertTrue( "kept in the parked store on its day", r.eventHeld() );
			int x = ow.worldX + r.pos % W, y = ow.worldY + r.pos / W;
			assertTrue( "out in the village's streets", Math.max( Math.abs( x - raid.cx ), Math.abs( y - raid.cy ) ) <= raid.berth() + 2 );
			if (r.role == Raider.FIGHTER) fighters++;
		}
		assertEquals( "a town with a watch sends every other raider at the guards", band.size() / 2, fighters );

		//the next step, and one with a raider parked by a slide: nobody new
		RaidEvent.onHeroStep( ow );
		assertEquals( band.size(), raiders( ow ).size() );
		Raider gone = band.get( 0 );
		ow.mobs.remove( gone );
		invoke( ow, OverworldLevel.class, "park", new Class<?>[]{ Mob.class, int.class, int.class },
				gone, ow.worldX + gone.pos % W, ow.worldY + gone.pos / W );
		assertTrue( "today's raider waits in the store", ow.parked().contains( gone ) );
		ArrayList<String> again = logged( () -> RaidEvent.onHeroStep( ow ) );
		assertEquals( band.size() - 1, raiders( ow ).size() );
		assertFalse( "told once", said( again, Messages.get( RaidEvent.class, "live", name ) ) );
		assertFalse( "heard of once", said( again, Messages.get( RaidEvent.class, "smoke_here", name ) ) );

		//the village keeps in and shuts its shops; two of its lived-in houses burn
		assertEquals( Messages.get( RaidEvent.class, "shut_raid" ), RaidEvent.shutReason( sector ) );
		assertTrue( RaidEvent.shelters( sector, 0 ) && RaidEvent.shelters( sector, 1 ) );
		Villager v = Villager.random( sector );
		int at = v.arrive( ow, raid.houseX( 3 ), raid.houseY( 3 ) );
		assertNotEquals( -1, at );
		assertEquals( "the folk are indoors", xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine.Slot.INDOORS.ordinal(), v.slot );
		ArrayList<int[]> fires = RaidEvent.burningHouses( ow );
		assertEquals( 2, fires.size() );
		HashSet<Long> lived = new HashSet<>();
		for (int k = 0; k < WorldStructures.populatedHouses( raid.houses ); k++) lived.add( OverworldLevel.worldKey( raid.houseX( k ), raid.houseY( k ) ) );
		for (int[] f : fires) assertTrue( "a lived-in house burns", lived.contains( OverworldLevel.worldKey( f[0], f[1] ) ) );
		assertNotEquals( OverworldLevel.worldKey( fires.get( 0 )[0], fires.get( 0 )[1] ), OverworldLevel.worldKey( fires.get( 1 )[0], fires.get( 1 )[1] ) );
		//...as soot over the fog, through the settlements' own smoke
		ArrayList<SettlementAmbience.Light> lights = new ArrayList<>();
		ArrayList<SettlementAmbience.Smoke> smoke = new ArrayList<>();
		invoke( null, SettlementAmbience.class, "gather", new Class<?>[]{ OverworldLevel.class, int.class, HashSet.class,
				ArrayList.class, ArrayList.class, HashSet.class }, ow, hero.pos, new HashSet<Long>(), lights, smoke, new HashSet<Long>() );
		int soot = 0;
		for (SettlementAmbience.Smoke s : smoke) if (s.soot){
			soot++;
			assertFalse( "seen from afar, over the fog", s.gated );
		}
		assertEquals( 2, soot );
	}

	@Test
	public void beatingTheBandOffSavesTheVillage(){
		int[] t = townRaid();
		long sector = WorldStructures.sectorOf( t[0], t[1] );
		long id = WorldEvents.idOf( WorldEvents.Type.RAID, t[0], t[1], t[2] );
		OverworldLevel ow = raidedTown();
		RaidEvent.onHeroStep( ow );
		ArrayList<Raider> band = raiders( ow );
		assertFalse( band.isEmpty() );
		hero.sprite = stub();
		//a keeper of the village, his shelf beside him
		OverworldShopkeeper k = OverworldShopkeeper.of( 0, sector );
		k.pos = hero.pos + 3;
		k.sprite = stub();
		ow.mobs.add( k );
		Actor.add( k );
		int shelf = k.pos + 1;
		Food food = new Food();
		int shelfPrice = Shopkeeper.sellPrice( food, hero );
		assertEquals( "shut in the raid", Messages.get( RaidEvent.class, "shut_raid" ), Shopkeeper.sellerOf( shelf ).tradeBlock() );

		//no watch in this window's streets: the village's elder speaks for it, not the farmer nearer by
		ArrayList<String> elderSaid = new ArrayList<>(), farmerSaid = new ArrayList<>();
		Villager elder = Villager.random( sector ), farmer = Villager.random( sector );
		elder.role = VillageRoutine.Role.ELDER.ordinal();
		farmer.role = VillageRoutine.Role.FARMER.ordinal();
		elder.sprite = recorder( elderSaid );
		farmer.sprite = recorder( farmerSaid );
		elder.pos = hero.pos - 6 * W;
		farmer.pos = hero.pos - 2 * W;
		ow.mobs.add( elder );
		ow.mobs.add( farmer );

		final String name = WorldStructures.villageName( SEED, t[0], t[1] );
		ArrayList<String> lines = logged( () -> {
			for (Raider r : band){
				r.sprite = stub();
				r.die( hero );
			}
		} );
		assertTrue( "beaten off: settled", ow.eventResolved( id ) );
		assertTrue( said( lines, Messages.get( RaidEvent.class, "reward", name ) ) );
		assertTrue( said( lines, Messages.get( RaidEvent.class, "thanks", name ) ) );
		assertTrue( "the elder cheers", elderSaid.contains( Messages.get( RaidEvent.class, "cheer" ) ) );
		assertTrue( farmerSaid.isEmpty() );
		assertFalse( RaidEvent.ongoing( sector ) );
		assertNull( RaidEvent.shutReason( sector ) );
		assertTrue( "the fires are out", RaidEvent.burningHouses( ow ).isEmpty() );
		//the purse and the prize at the hero's feet (or as near as is clear of a shelf)
		int gold = 0;
		Item prize = null;
		for (Heap h : ow.heaps.valueList()){
			if (ow.distance( h.pos, hero.pos ) > 3) continue;
			for (Item i : h.items){
				if (i instanceof Gold) gold += i.quantity();
				else if (i instanceof Weapon || i instanceof Armor || i instanceof Wand || i instanceof Ring) prize = i;
			}
		}
		int houses = houses( t[0], t[1] );
		assertTrue( "the purse: " + gold, gold >= Math.round( (60 + 15 * houses) * 0.85f ) );
		assertNotNull( "the prize", prize );
		assertFalse( prize.cursed );
		assertFalse( prize instanceof Weapon && ((Weapon) prize).hasCurseEnchant() );
		assertFalse( prize instanceof Armor && ((Armor) prize).hasCurseGlyph() );
		//no band comes out again
		RaidEvent.onHeroStep( ow );
		assertTrue( raiders( ow ).isEmpty() );

		//the favour: the shelf's price marked down, today and three days more
		for (int d = t[2]; d <= t[2] + RaidEvent.FAVOUR_DAYS; d++){
			Dungeon.cycleTurn = d * FC + 600;
			assertEquals( "day " + d, RaidEvent.FAVOUR_PRICE, k.priceFactor(), 0f );
			assertNull( k.tradeBlock() );
			assertEquals( Shopkeeper.discounted( shelfPrice, RaidEvent.FAVOUR_PRICE ),
					Shopkeeper.sellPrice( food, hero, Shopkeeper.sellerOf( shelf ) ) );
			assertTrue( Shopkeeper.sellPrice( food, hero, Shopkeeper.sellerOf( shelf ) ) < shelfPrice );
		}
		Dungeon.cycleTurn = (t[2] + RaidEvent.FAVOUR_DAYS + 1) * FC + 600;
		assertEquals( 1f, k.priceFactor(), 0f );
		assertEquals( shelfPrice, Shopkeeper.sellPrice( food, hero, Shopkeeper.sellerOf( shelf ) ) );
		//another village's keepers know nothing of it
		Dungeon.cycleTurn = t[2] * FC + 600;
		assertEquals( 1f, OverworldShopkeeper.of( 0, WorldStructures.sectorOf( t[0] + 1, t[1] ) ).priceFactor(), 0f );
		//the watch is grateful
		OverworldGuard g = OverworldGuard.random( sector );
		g.sprite = stub();
		assertTrue( said( logged( () -> g.interact( hero ) ), Messages.get( OverworldGuard.class, "grateful" ) ) );
	}

	//the prize, whatever it rolls: no curse on it, and none worked into a weapon or an armour -
	//their random() lays one on three in ten, and it bites whatever the cursed flag says - its
	//make known
	@Test
	public void aSavedVillagesPrizeCarriesNoCurse(){
		int weapons = 0, armours = 0;
		for (int i = 0; i < 400; i++){
			Item p = RaidEvent.prize();
			assertFalse( p.cursed );
			assertTrue( p.isIdentified() );
			if (p instanceof Weapon){
				weapons++;
				assertFalse( "a cursed edge: " + p.name(), ((Weapon) p).hasCurseEnchant() );
			} else if (p instanceof Armor){
				armours++;
				assertFalse( "a cursed weave: " + p.name(), ((Armor) p).hasCurseGlyph() );
			} else {
				assertTrue( p.getClass().getSimpleName(), p instanceof Wand || p instanceof Ring );
			}
		}
		assertTrue( "weapons " + weapons + ", armours " + armours, weapons >= 50 && armours >= 50 );
	}

	@Test
	public void aRaidNobodyAnsweredShutsTheShopsForTwoDays(){
		int[] t = townRaid();
		long sector = WorldStructures.sectorOf( t[0], t[1] );
		OverworldLevel ow = raidedTown();
		RaidEvent.onHeroStep( ow );
		//its day: everyone in
		assertTrue( RaidEvent.shelters( sector, 1 ) );
		String mending = Messages.get( RaidEvent.class, "shut_recovering" );
		for (int d = t[2] + 1; d <= t[2] + RaidEvent.RECOVERY_DAYS; d++){
			Dungeon.cycleTurn = d * FC + 600;
			assertFalse( RaidEvent.ongoing( sector ) );
			assertTrue( "day " + d, RaidEvent.recovering( sector ) );
			assertEquals( mending, RaidEvent.shutReason( sector ) );
			assertEquals( mending, OverworldShopkeeper.of( 0, sector ).tradeBlock() );
			assertEquals( 1f, RaidEvent.priceFactor( sector ), 0f );
			//half the families keep in
			assertTrue( RaidEvent.shelters( sector, 0 ) );
			assertFalse( RaidEvent.shelters( sector, 1 ) );
		}
		Dungeon.cycleTurn = (t[2] + RaidEvent.RECOVERY_DAYS + 1) * FC + 600;
		assertFalse( RaidEvent.recovering( sector ) );
		assertNull( RaidEvent.shutReason( sector ) );
		assertFalse( RaidEvent.shelters( sector, 0 ) );
	}

	@Test
	public void theWatchWearsRaidersDownButLeavesTheKillToTheHero(){
		int[] t = townRaid();
		long sector = WorldStructures.sectorOf( t[0], t[1] );
		OverworldLevel ow = raidedTown();
		RaidEvent.onHeroStep( ow );
		Raider r = raiders( ow ).get( 0 );
		r.sprite = stub();
		//a guard of the town beside it, the hero close enough that he keeps his place
		OverworldGuard g = OverworldGuard.random( sector );
		g.sprite = stub();
		g.pos = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = r.pos + n;
			if (ow.passable[c] && !ow.occupied( c )){
				g.pos = c;
				break;
			}
		}
		assertNotEquals( -1, g.pos );
		ow.mobs.add( g );
		Actor.add( g );
		Actor.add( r );
		for (int i = 0; i < 80 && r.HP > 1; i++){
			int at = g.pos;
			act( g );
			if (g.pos != at) break;
			assertTrue( "never finished by the watch", r.isAlive() && r.HP >= 1 );
		}
		assertEquals( "worn down to its last", 1, r.HP );
		//the watch's blow is spent on it no more
		int hp = r.HP;
		act( g );
		assertEquals( hp, r.HP );
		r.damage( 10, g );
		assertEquals( 1, r.HP );
		assertTrue( r.isAlive() );
		assertFalse( ow.eventResolved( WorldEvents.idOf( WorldEvents.Type.RAID, t[0], t[1], t[2] ) ) );
	}

	@Test
	public void raidersMakeOffWhenTheDayIsOver(){
		int[] t = townRaid();
		OverworldLevel ow = raidedTown();
		RaidEvent.onHeroStep( ow );
		ArrayList<Raider> band = raiders( ow );
		for (Raider b : band) b.sprite = stub();
		//the next day: whatever is left of the band makes off, and the raid is no longer on
		Dungeon.cycleTurn = (t[2] + 1) * FC + 600;
		Raider r = band.get( 0 );
		r.sprite = null;
		assertFalse( r.eventHeld() );
		Actor.add( r );
		assertTrue( act( r ) );
		assertTrue( r.retreating );
		assertFalse( "gone, unseen", ow.mobs.contains( r ) );
		//one turned by the hero stays his
		Raider turned = band.get( 1 );
		turned.alignment = Char.Alignment.ALLY;
		Actor.add( turned );
		act( turned );
		assertFalse( turned.retreating );
		//a raider of a past day waiting in the store is forgotten with the day
		Raider stale = band.get( 2 );
		ow.mobs.remove( stale );
		@SuppressWarnings("unchecked")
		HashMap<Long, Mob> parked = (HashMap<Long, Mob>) field( ow, "parkedMobs" );
		parked.put( OverworldLevel.worldKey( 3, 3 ), stale );
		RaidEvent.onHeroStep( ow );
		assertFalse( ow.parked().contains( stale ) );
	}

	//a host who stands - resting, searching, reading - while a guest walks up still runs the
	//surface: a turn he took no step on ticks it as his next begins (DayNightCycle.onHeroTurn), so
	//the band comes out with nobody stepping. once a turn: not again as the turn he waited in for
	//the player begins again at the same time, nor after a turn his own step ticked; never on a mirror
	@Test
	public void aHostStandingStillStillTicksTheSurface() throws Exception {
		int[] t = townRaid();
		OverworldLevel ow = raidedTown();
		String name = WorldStructures.villageName( SEED, t[0], t[1] );
		Field now = Actor.class.getDeclaredField( "now" );
		now.setAccessible( true );
		now.set( null, 10f );

		//a mirror ticks nothing: its host does
		setField( ow, "network", true );
		OverworldLevel.onHeroTurn();
		assertTrue( raiders( ow ).isEmpty() );
		assertEquals( Long.MIN_VALUE, field( ow, "nextTraffic" ) );
		setField( ow, "network", false );

		//nobody has stepped, and the band comes out all the same; the road's life looked at too
		ArrayList<String> lines = logged( OverworldLevel::onHeroTurn );
		assertTrue( "the band's coming out is told", said( lines, Messages.get( RaidEvent.class, "live", name ) ) );
		RaidEvent.Raid raid = new RaidEvent.Raid( SEED, RaidEvent.raidEvent( SEED, t[0], t[1], t[2] ) );
		assertEquals( raid.raiders(), raiders( ow ).size() );
		assertNotEquals( Long.MIN_VALUE, field( ow, "nextTraffic" ) );

		//the same turn begun again as the player acts: no second tick
		setField( ow, "nextTraffic", Long.MIN_VALUE );
		OverworldLevel.onHeroTurn();
		assertEquals( Long.MIN_VALUE, field( ow, "nextTraffic" ) );

		//the next turn, after one he stood through: ticked
		now.set( null, 11f );
		OverworldLevel.onHeroTurn();
		assertNotEquals( Long.MIN_VALUE, field( ow, "nextTraffic" ) );

		//a step of his ticks the surface itself, and the turn after it begins without a second tick
		ow.occupyCell( hero );
		assertNotEquals( Long.MIN_VALUE, field( ow, "nextTraffic" ) );
		setField( ow, "nextTraffic", Long.MIN_VALUE );
		now.set( null, 12f );
		OverworldLevel.onHeroTurn();
		assertEquals( Long.MIN_VALUE, field( ow, "nextTraffic" ) );
		//...and the one after that, stood through, ticks again
		now.set( null, 13f );
		OverworldLevel.onHeroTurn();
		assertNotEquals( Long.MIN_VALUE, field( ow, "nextTraffic" ) );
		assertEquals( "nobody new from all the ticks", raid.raiders(), raiders( ow ).size() );
	}

	// ------------------------------------------------------------------ saves

	@Test
	public void resolutionSurvivesASaveAndOldSavesLoad(){
		int[] t = townRaid();
		long sector = WorldStructures.sectorOf( t[0], t[1] );
		OverworldLevel ow = new OverworldLevel();
		ow.worldSeed = SEED;
		Dungeon.level = ow;
		ow.resolveEvent( WorldEvents.idOf( WorldEvents.Type.RAID, t[0], t[1], t[2] ), (t[2] + 1 + RaidEvent.FAVOUR_DAYS) * FC );
		Bundle b = new Bundle();
		((WorldEventLog) field( ow, "eventLog" )).storeInBundle( b );

		OverworldLevel back = new OverworldLevel();
		back.worldSeed = SEED;
		((WorldEventLog) field( back, "eventLog" )).restoreFromBundle( b );
		Dungeon.level = back;
		Dungeon.cycleTurn = (t[2] + 1) * FC + 600;
		assertEquals( RaidEvent.FAVOUR_PRICE, RaidEvent.priceFactor( sector ), 0f );
		assertFalse( "beaten off: no mending", RaidEvent.recovering( sector ) );

		OverworldLevel old = new OverworldLevel();
		old.worldSeed = SEED;
		((WorldEventLog) field( old, "eventLog" )).restoreFromBundle( new Bundle() );
		Dungeon.level = old;
		assertEquals( 1f, RaidEvent.priceFactor( sector ), 0f );
		assertTrue( "unanswered in an older save's world", RaidEvent.recovering( sector ) );
	}

	@Test
	public void raiderRoundTripAndOldBundle(){
		Raider r = new Raider();
		r.sector = WorldStructures.sectorOf( -3, 7 );
		r.day = 41;
		r.role = Raider.FIGHTER;
		r.cx = -250;
		r.cy = 700;
		Bundle b = new Bundle();
		b.put( "r", r );
		Raider back = (Raider) b.get( "r" );
		assertEquals( r.sector, back.sector );
		assertEquals( 41, back.day );
		assertEquals( Raider.FIGHTER, back.role );
		assertEquals( -250, back.cx );
		assertEquals( 700, back.cy );
		assertTrue( back.sentOnEvent() );

		Bundle ob = new Bundle();
		new OverworldBandit().storeInBundle( ob );
		Raider old = new Raider();
		old.restoreFromBundle( ob );
		assertEquals( Long.MIN_VALUE, old.sector );
		assertEquals( Integer.MIN_VALUE, old.day );
		assertEquals( Raider.LOOTER, old.role );
		assertFalse( "a raid long over", old.eventHeld() );
	}

	@Test
	public void everyRaidStringResolves(){
		String[] keys = { "smoke", "smoke_here", "live", "thanks", "cheer", "reward", "reward_well", "withdraw",
				"shut_raid", "shut_recovering" };
		for (String k : keys) assertResolves( Messages.get( RaidEvent.class, k, "Testford", "north" ) );
		assertResolves( Messages.get( WorldEvents.class, "map_raid" ) );
		assertResolves( Messages.get( Villager.class, "raid" ) );
		assertResolves( Messages.get( Villager.class, "recovering" ) );
		assertResolves( Messages.get( OverworldGuard.class, "raid" ) );
		assertResolves( Messages.get( OverworldGuard.class, "grateful" ) );
		assertResolves( new Raider().name() );
		assertResolves( new Raider().description() );
		assertEquals( "raider", new Raider().name() );
	}

	private static void assertResolves( String s ){
		assertFalse( s, s.contains( Messages.NO_TEXT_FOUND ) );
	}
}

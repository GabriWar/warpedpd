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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CampFire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.homestead.AwaySettlers;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCategory;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomeActions;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.bags.VelvetPouch;
import xyz.gabriwar.warpedpixeldungeon.items.materials.IronFittings;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The threats on homes (homestead/threats): a home's notice part by part and its caps, the raid
 * line and the nights it brings sieges and raids, a raid's band going for the richest chest and
 * running with it, a breaker's way in through the cheapest built piece and the rest waiting
 * outside, a siege's wolves biting only fences and gates, what an absence settles - the same
 * every time and never more than one chest - and the ledger kept with the run, the alarm bell
 * waking a sleeper before the ambush lands, the spike traps sparing the hero and his allies, and
 * the watchtower's sight. Every window is a real one of a slice, derived from a seed.
 */
public class HomesteadThreatsTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int depth, branch, challenges, startDay, turn, deepest;
	private DayNightCycle.Phase override;
	private float shift;
	private boolean shiftHeld;
	private Level savedLevel;
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
		Dungeon.cycleTurn = 0;
		Statistics.deepestFloor = 10;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		HomesteadWorld.reset();
		hero = new Hero();
		hero.lvl = 10;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		Random.popGenerator();
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.challenges = challenges;
		Dungeon.calendarStartDay = startDay;
		Dungeon.cycleTurn = turn;
		Statistics.deepestFloor = deepest;
		DayNightCycle.debugPhaseOverride = override;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		HomesteadWorld.reset();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
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
		try {
			Field f = OverworldLevel.class.getDeclaredField( "network" );
			f.setAccessible( true );
			f.set( ow, false );
		} catch (Exception e){
			throw new AssertionError( e );
		}
		Dungeon.level = ow;
		Arrays.fill( ow.heroFOV, false );
		return ow;
	}

	//the top-left cell of a w x h stretch of open, unedited, unprotected ground (and a cell round
	//it) in the window, nearest its middle; -1 for none
	private static int openGround( OverworldLevel ow, int w, int h ){
		int mid = W / 2 + (H / 2) * W;
		for (int r = 0; r < 70; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int o = mid + dx + dy * W;
					if (fits( ow, o, w, h )) return o;
				}
			}
		}
		return -1;
	}

	private static boolean fits( OverworldLevel ow, int o, int w, int h ){
		int ox = o % W, oy = o / W;
		for (int y = oy - 1; y <= oy + h; y++){
			for (int x = ox - 1; x <= ox + w; x++){
				if (x < 3 || y < 3 || x >= W - 3 || y >= H - 3) return false;
				int c = x + y * W;
				int t = ow.map[c];
				if (!ow.passable[c] || ow.pit[c] || (Terrain.flags[t] & Terrain.LIQUID) != 0 || t == Terrain.FROZEN_WATER
						|| t == Terrain.BRIDGE || Terrain.homestead( t ) || ow.protectedForBuilding( c )
						|| ow.pristineAt( c ) != t || ow.heaps.get( c ) != null) return false;
			}
		}
		return true;
	}

	//a surface window with open ground of w x h in it, and that ground's top-left cell
	private OverworldLevel site( int w, int h, int[] origin ){
		for (int[] at : new int[][]{ { 300, 300 }, { -400, 260 }, { 600, -500 }, { -900, -700 }, { 1500, 900 } }){
			OverworldLevel ow = windowAt( 0, at[0], at[1] );
			int o = openGround( ow, w, h );
			if (o != -1){
				origin[0] = o;
				return ow;
			}
		}
		throw new AssertionError( "no open ground in any test window" );
	}

	private static void build( OverworldLevel ow, int cell, String id ){
		Homestead.build( ow, cell, BuildCatalog.byId( id ), null );
	}

	//a ring of `wall` w x h at o, `door` in the middle of its south side, `floor` inside: the door's cell
	private static int house( OverworldLevel ow, int o, int w, int h, String wall, String door ){
		int d = -1;
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				int c = o + x + y * W;
				boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
				if (!edge) build( ow, c, "floor_plank" );
				else if (x == w / 2 && y == h - 1){
					build( ow, c, door );
					d = c;
				} else build( ow, c, wall );
			}
		}
		return d;
	}

	private static void roof( OverworldLevel ow, int o, int w, int h, String kind ){
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++) build( ow, o + x + y * W, kind );
		}
	}

	//a home whose hearthstone is on this window cell, visited now
	private static HomesteadWorld.Home home( OverworldLevel ow, int cell ){
		HomesteadWorld.Home h = new HomesteadWorld.Home( WorldLayers.depthOf( ow.altitude() ),
				ow.worldX + cell % W, ow.worldY + cell / W, "Oakhold", WorldClock.day() );
		h.visited();
		HomesteadWorld.addHome( h );
		return h;
	}

	//a chest on a cell holding these things, on the level
	private static StorageChest chest( OverworldLevel ow, int cell, Item... things ){
		StorageChest c = new StorageChest();
		c.pos = cell;
		for (Item i : things) assertTrue( i.collect( c.storage ) );
		ow.mobs.add( c );
		return c;
	}

	private static <T extends Mob> T put( OverworldLevel ow, T mob, int cell ){
		mob.pos = cell;
		ow.mobs.add( mob );
		return mob;
	}

	private void stand( OverworldLevel ow, int cell ){
		hero.pos = cell;
		hero.fieldOfView = new boolean[ow.length()];
		Arrays.fill( hero.fieldOfView, true );
	}

	// ------------------------------------------------------------------ the notice

	@Test
	public void theNoticeCountsWhatAHomeHoldsUpToEachCap(){
		int[] o = new int[1];
		OverworldLevel ow = site( 9, 7, o );
		int hut = o[0] + 1 + W;
		house( ow, hut, 7, 5, "wall_plank", "door" );
		int inside = hut + 3 + 2 * W;
		HomesteadWorld.Home h = home( ow, inside );

		Notice.Score empty = Notice.of( ow, h, false );
		//the ring's 20 cells and 15 of floor: 35 pieces
		assertEquals( 35 / Notice.BUILT_EACH, empty.get( Notice.Part.BUILT ) );
		assertEquals( 0, empty.get( Notice.Part.CHESTS ) );
		assertEquals( "plank walls keep nobody out", 0, empty.get( Notice.Part.WALLS ) );
		assertFalse( empty.aboveLine() );

		//a chest's things: a point a hundred, and never more than the cap
		chest( ow, inside - 1, new Gold( 1550 ) );
		assertEquals( 15, Notice.of( ow, h, false ).get( Notice.Part.CHESTS ) );
		chest( ow, inside + 1, new Gold( 99999 ) );
		assertEquals( Notice.CHEST_CAP, Notice.of( ow, h, false ).get( Notice.Part.CHESTS ) );
		//a chest past the home's ground is not the home's
		StorageChest far = chest( ow, inside + (HomeThreats.AREA + 2), new Gold( 5000 ) );
		assertFalse( HomeThreats.inArea( ow, h, far.pos ) );

		//above the line now: 40 and the built ones
		Notice.Score rich = Notice.of( ow, h, false );
		assertEquals( Notice.CHEST_CAP + 35 / Notice.BUILT_EACH, rich.total() );
		assertTrue( rich.aboveLine() );
		assertTrue( rich.total() > HomeThreats.RAID_LINE );

		//its light, by night only: a fire burning (a lit hearth) inside
		Blob.seed( inside, 100 + 400, CampFire.class, ow );
		assertEquals( 0, Notice.of( ow, h, false ).get( Notice.Part.LIGHT ) );
		assertEquals( Notice.LIGHT_EACH, Notice.of( ow, h, true ).get( Notice.Part.LIGHT ) );

		//kept for the nights away: the run's records and the facade agree
		Notice.refresh( ow, h );
		assertEquals( Notice.of( ow, h, DayNightCycle.isNight() ).total(), ThreatWorld.get().notice( h ).total() );
		assertEquals( Notice.of( ow, h, false ).total(), Notice.of( h ).total() );
	}

	@Test
	public void defencesTakeFromTheNoticeUpToTheirCapsAndNeverBelowNothing(){
		int[] o = new int[1];
		OverworldLevel ow = site( 13, 13, o );
		int w = W;
		int c0 = o[0];
		//a stone house with a barred door in the middle: of its 20 shell cells 19 are stone wall
		int hearth = c0 + 6 + 6 * w;
		house( ow, hearth - 3 - 2 * w, 7, 5, "wall_stone", "door_barred" );
		HomesteadWorld.Home h = home( ow, hearth );
		Notice.Score s = Notice.of( ow, h, false );
		assertEquals( -Math.round( Notice.WALLS_MAX * 19 / 20f ), s.get( Notice.Part.WALLS ) );
		assertEquals( -Notice.BARRED_EACH, s.get( Notice.Part.BARRED ) );
		assertEquals( "nothing held, defences only: never below nothing", 0, s.total() );

		//three more barred doors: the cap
		for (int k = 1; k <= 3; k++) build( ow, c0 + k * 2 + 2 * w, "door_barred" );
		assertEquals( -Notice.BARRED_CAP, Notice.of( ow, h, false ).get( Notice.Part.BARRED ) );

		//a palisade half round the hearthstone, then all round it
		int r = 6, hx = hearth % w, hy = hearth / w;
		for (int x = hx - r; x <= hx + r; x++) build( ow, x + (hy - r) * w, "palisade" );
		float half = Notice.palisadeShare( ow, hearth );
		assertTrue( "a side of a ring: " + half, half > 0.15f && half < 0.5f );
		assertEquals( -Math.round( Notice.PALISADE_MAX * half ), Notice.of( ow, h, false ).get( Notice.Part.PALISADE ) );
		for (int x = hx - r; x <= hx + r; x++) build( ow, x + (hy + r) * w, x == hx ? "gate" : "palisade" );
		for (int y = hy - r + 1; y < hy + r; y++){
			build( ow, hx - r + y * w, "palisade" );
			build( ow, hx + r + y * w, "palisade" );
		}
		assertEquals( "a closed ring, a gate in it", 1f, Notice.palisadeShare( ow, hearth ), 0.0001f );
		assertEquals( -Notice.PALISADE_MAX, Notice.of( ow, h, false ).get( Notice.Part.PALISADE ) );

		//a watchtower and a bell on its ground
		put( ow, new Watchtower(), hearth - 3 * w - 4 );
		put( ow, new AlarmBell(), hearth - 3 * w + 4 );
		Notice.Score all = Notice.of( ow, h, false );
		assertEquals( -Notice.WATCHTOWER_OFF, all.get( Notice.Part.WATCHTOWER ) );
		assertEquals( -Notice.BELL_OFF, all.get( Notice.Part.BELL ) );
		for (Notice.Part p : Notice.Part.values()){
			assertTrue( p + " within its cap", Math.abs( all.get( p ) ) <= p.cap() );
			assertTrue( p + " pulls its own way", p.draws() ? all.get( p ) >= 0 : all.get( p ) <= 0 );
			assertFalse( p.title().contains( Messages.NO_TEXT_FOUND ) );
		}
	}

	// ------------------------------------------------------------------ the nights

	@Test
	public void raidsComeOnlyAboveTheLineAndSiegesOnlyOnWinterNightsInWolfCountry(){
		long seed = OverworldLevel.worldSeedOf( 1 );
		//a home in the forest, and one on open grass
		HomesteadWorld.Home forest = null, plain = null;
		for (int k = 0; k < 4000 && (forest == null || plain == null); k++){
			int wx = (k % 63) * 37 - 1100, wy = (k / 63) * 41 - 1300;
			WorldModel.Biome b = WorldModel.biomeAt( seed, wx, wy );
			if (forest == null && b == WorldModel.Biome.FOREST) forest = new HomesteadWorld.Home( WorldLayers.SURFACE_DEPTH, wx, wy, "", 0 );
			if (plain == null && (b == WorldModel.Biome.PLAINS || b == WorldModel.Biome.MEADOW)) plain = new HomesteadWorld.Home( WorldLayers.SURFACE_DEPTH, wx, wy, "", 0 );
		}
		assertNotNull( forest );
		assertNotNull( plain );
		assertTrue( HomeThreats.wolfCountry( seed, forest ) );
		assertFalse( HomeThreats.wolfCountry( seed, plain ) );
		assertFalse( "the caves have no wolf nights", HomeThreats.wolfCountry( seed,
				new HomesteadWorld.Home( WorldLayers.depthOf( -2 ), forest.wx, forest.wy, "", 0 ) ) );

		int raids = 0, raidsNear = 0, winter = 0, sieges = 0, siegesNoticed = 0;
		final int NIGHTS = 1460;
		for (int n = 0; n < NIGHTS; n++){
			assertFalse( "at the line, no raid", HomeThreats.raidNight( seed, forest, n, HomeThreats.RAID_LINE ) );
			if (HomeThreats.raidNight( seed, forest, n, HomeThreats.RAID_LINE + 1 )) raids++;
			assertEquals( "the same night rolls the same", HomeThreats.raidNight( seed, forest, n, 50 ),
					HomeThreats.raidNight( seed, forest, n, 50 ) );
			assertFalse( "no wolves on the grass", HomeThreats.siegeNight( seed, plain, n, 99 ) );
			boolean w = GameCalendar.seasonOfDay( n ) == GameCalendar.Season.WINTER;
			if (w) winter++;
			if (HomeThreats.siegeNight( seed, forest, n, 0 )){
				assertTrue( "a siege falls in winter", w );
				sieges++;
			}
			if (HomeThreats.siegeNight( seed, forest, n, 99 )) siegesNoticed++;
			int size = HomeThreats.packSize( seed, forest, n );
			assertTrue( size >= HomeThreats.PACK_MIN && size <= HomeThreats.PACK_MAX );
			size = HomeThreats.bandSize( seed, forest, n );
			assertTrue( size >= HomeThreats.BAND_MIN && size <= HomeThreats.BAND_MAX );
		}
		float rate = raids / (float) NIGHTS;
		float want = HomeThreats.nearBandits( seed, forest.wx, forest.wy ) ? 1f / HomeThreats.RAID_ODDS_BANDITS : 1f / HomeThreats.RAID_ODDS;
		assertTrue( "a raid a night in " + Math.round( 1 / want ) + ": " + rate, Math.abs( rate - want ) < 0.05f );
		assertTrue( "four years: a winter each", winter > 300 );
		float siegeRate = sieges / (float) winter, noticedRate = siegesNoticed / (float) winter;
		assertTrue( "a siege a winter night in four: " + siegeRate, siegeRate > 0.18f && siegeRate < 0.32f );
		assertTrue( "one in two above the line: " + noticedRate, noticedRate > 0.42f && noticedRate < 0.58f );

		//near an outlaw village: a raid a night in three
		HomesteadWorld.Home bandits = null;
		for (int k = 0; k < 4000 && bandits == null; k++){
			int wx = (k % 63) * 97 - 3000, wy = (k / 63) * 89 - 3000;
			if (HomeThreats.nearBandits( seed, wx, wy )) bandits = new HomesteadWorld.Home( WorldLayers.SURFACE_DEPTH, wx, wy, "", 0 );
		}
		assertNotNull( "an outlaw village somewhere", bandits );
		for (int n = 0; n < NIGHTS; n++) if (HomeThreats.raidNight( seed, bandits, n, 99 )) raidsNear++;
		float near = raidsNear / (float) NIGHTS;
		assertTrue( "a raid a night in three by the outlaws: " + near, near > 0.27f && near < 0.40f );
	}

	// ------------------------------------------------------------------ a raid live

	@Test
	public void aRaiderGoesForTheRichestChestTakesItsDearestThingsAndRuns(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int[] o = new int[1];
		OverworldLevel ow = site( 9, 7, o );
		int hut = o[0] + 1 + W;
		house( ow, hut, 7, 5, "wall_plank", "door" );
		int inside = hut + 3 + 2 * W;
		HomesteadWorld.Home h = home( ow, inside );
		StorageChest poor = chest( ow, inside - 2, new Gold( 40 ) );
		Item ring = xyz.gabriwar.warpedpixeldungeon.items.Generator.random( xyz.gabriwar.warpedpixeldungeon.items.Generator.Category.RING );
		StorageChest rich = chest( ow, inside + 2, new Gold( 900 ), ring,
				new IronFittings().quantity( 3 ), new Log().quantity( 2 ), new Rope() );
		ThreatWorld.Event e = ThreatWorld.get().begin( h, ThreatWorld.Event.RAID, WorldClock.night() );
		//the band comes for the richest chest on the ground, and that one only
		assertSame( "the richest", rich, HomeRaider.richest( ow, h, inside ) );
		e.chest = OverworldLevel.worldKey( ow.worldX + rich.pos % W, ow.worldY + rich.pos / W );

		HomeRaider r = put( ow, new HomeRaider(), inside + 1 + W );
		r.home = HomeThreats.key( h );
		r.event = e.id;
		assertSame( rich, r.goal() );
		assertEquals( HomeRaider.GRAB, r.grab( rich ) );
		assertTrue( "off with it", r.fleeing );
		assertEquals( "the cheapest left", 1, rich.storage.items.size() );
		assertTrue( "the dearest went first", r.carried.get( 0 ) instanceof Gold && r.carried.get( 0 ).quantity() == 900 );
		for (Item left : rich.storage.items){
			for (Item took : r.carried) assertTrue( Notice.valueOf( took ) >= Notice.valueOf( left ) );
		}

		//the next one goes for what is left in the same chest, never on to the other one
		HomeRaider next = put( ow, new HomeRaider(), inside - 1 - W );
		next.home = r.home;
		next.event = e.id;
		assertTrue( "the poor chest is the richer now", Notice.valueOf( poor ) > Notice.valueOf( rich ) );
		assertSame( rich, next.goal() );

		//cut down, it drops all it carried (no hero's sprite to show his experience)
		r.EXP = 0;
		r.die( null );
		int coins = 0;
		boolean ringBack = false;
		for (Heap hp : ow.heaps.valueList()){
			for (Item i : hp.items){
				if (i instanceof Gold) coins += i.quantity();
				if (i == ring) ringBack = true;
			}
		}
		//its own purse may have fallen with them, onto the same heap
		assertTrue( "the 900 coins are on the ground: " + coins, coins >= 900 );
		assertTrue( "and the ring", ringBack );
		assertTrue( r.carried.isEmpty() );
		assertEquals( 1, e.slain );

		//one that gets away takes its haul with it: the raid's tally
		next.grab( rich );
		int took = next.carried.size();
		assertTrue( rich.storage.items.isEmpty() );
		HomeRaider third = put( ow, new HomeRaider(), inside - 1 + W );
		third.home = r.home;
		third.event = e.id;
		assertNull( "the chest emptied: the rest make off, the other chest untouched", third.goal() );
		assertEquals( 1, poor.storage.items.size() );
		next.escape();
		assertEquals( took, e.stolen );
		assertFalse( ow.mobs.contains( next ) );
		assertEquals( "raid_stolen", ThreatSlice.ledgerKey( e ) );
		assertFalse( Messages.get( ThreatSlice.class, "raid_stolen", h.title(), e.slain, e.stolen ).contains( Messages.NO_TEXT_FOUND ) );
	}

	// ------------------------------------------------------------------ breaking in

	@Test
	public void aBreakerHitsTheCheapestWayInAndTheRestWaitOutside(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int[] o = new int[1];
		OverworldLevel ow = site( 13, 13, o );
		//a closed stone ring 7x7 (a barred door south) round the hero, three cells in from the ground's edge
		int ring = o[0] + 3 + 3 * W;
		house( ow, ring, 7, 7, "wall_stone", "door_barred" );
		int inside = ring + 3 + 3 * W;
		stand( ow, inside );
		//one log wall on its north side, the hunter at the west
		int log = ring + 3;
		Level.set( log, Terrain.FLOOR_PLANK, ow );
		build( ow, log, "wall_log" );
		int west = ring - 2 + 3 * W;

		int[] way = Breaker.path( ow, west, inside, Breaker.Rule.ALL );
		assertNotNull( way );
		int first = firstBuilt( ow, way, inside );
		assertEquals( "round to the log wall, not through the stone", log, first );

		//a stone wall worn down to almost nothing is cheaper still
		int westWall = ring + 3 * W;
		Homestead.damage( ow, westWall, 115, null );
		assertEquals( 5, Homestead.hpAt( ow, westWall ) );
		way = Breaker.path( ow, west, inside, Breaker.Rule.ALL );
		assertEquals( westWall, firstBuilt( ow, way, inside ) );

		//beside it: a blow; a cell off: a step toward it
		Breaker.Order o1 = Breaker.plan( ow, west + 1, inside, Breaker.Rule.ALL );
		assertEquals( Breaker.Order.Kind.STRIKE, o1.kind );
		assertEquals( westWall, o1.cell );
		Breaker.Order o2 = Breaker.plan( ow, west, inside, Breaker.Rule.ALL );
		assertEquals( Breaker.Order.Kind.STEP, o2.kind );
		assertTrue( "the open cell before the wall", ow.adjacent( o2.cell, west ) && ow.adjacent( o2.cell, westWall ) );

		//who breaks: a yeti, a raider; a rat waits outside instead
		Yeti yeti = put( ow, new Yeti(), west + 1 );
		assertEquals( Breaker.Rule.ALL, Breaker.ruleOf( yeti ) );
		assertEquals( Breaker.Rule.ALL, Breaker.ruleOf( new HomeRaider() ) );
		assertEquals( Breaker.Rule.NONE, Breaker.ruleOf( new Rat() ) );
		Breaker.Order yo = Breaker.walledOut( yeti, hero, inside, false );
		assertEquals( Breaker.Order.Kind.STRIKE, yo.kind );
		int before = Homestead.hpAt( ow, westWall );
		Breaker.strike( yeti, yo.cell, 3 );
		assertEquals( before - 3, Homestead.hpAt( ow, westWall ) );

		Rat rat = put( ow, new Rat(), west - W );
		Breaker.Order ro = Breaker.walledOut( rat, hero, inside, false );
		assertNotNull( "walled out by what was built: it waits outside", ro );
		assertTrue( ro.kind != Breaker.Order.Kind.STRIKE );
		assertNotNull( rat.buff( Breaker.Lurking.class ) );
		if (ro.kind == Breaker.Order.Kind.STEP){
			//the walls are three cells round him: it paces just outside them
			int d = ow.distance( ro.cell, inside );
			assertTrue( "pacing as near as it can get: " + d, d >= 4 && d <= 5 );
			assertTrue( ow.passable[ro.cell] );
		}
		//its patience runs out
		for (int k = 0; k < Breaker.LURK_TURNS; k++) Breaker.walledOut( rat, hero, inside, false );
		assertNull( "back to its wandering", Breaker.walledOut( rat, hero, inside, false ) );
		//and stays out while it is still walled out; no longer walled out, it forgets
		Breaker.Lurking patience = rat.buff( Breaker.Lurking.class );
		assertNotNull( patience );
		for (int k = 0; k <= Breaker.FORGET; k++) patience.act();
		assertNull( "it got in or gave up: ready to wait again another time", rat.buff( Breaker.Lurking.class ) );

		//nothing built in the way: no breaking
		assertNull( Breaker.plan( ow, west, west + 2 * W, Breaker.Rule.ALL ) );
	}

	//the first cell of a way that is a built piece standing in it
	private static int firstBuilt( OverworldLevel ow, int[] way, int goal ){
		for (int c : way) if (c != goal && !ow.passable[c] && Homestead.isBuilt( ow, c )) return c;
		return -1;
	}

	@Test
	public void aSiegesWolvesBiteFencesAndGatesAndNeverWallsOrDoors(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int[] o = new int[1];
		OverworldLevel ow = site( 9, 7, o );
		int hut = o[0] + 1 + W;
		int door = house( ow, hut, 5, 4, "wall_log", "door_barred" );
		//a fence and a gate south of the door, a palisade beside them
		int fence = door + 2 * W - 1, gate = door + 2 * W, palisade = door + 2 * W + 1;
		build( ow, fence, "fence" );
		build( ow, gate, "gate" );
		build( ow, palisade, "palisade" );
		HomesteadWorld.Home h = home( ow, hut + 2 + W );
		ThreatWorld.Event e = ThreatWorld.get().begin( h, ThreatWorld.Event.SIEGE, WorldClock.night() );

		SiegeBrownWolf wolf = put( ow, new SiegeBrownWolf(), door + W );
		wolf.pack().home = HomeThreats.key( h );
		wolf.pack().event = e.id;
		assertEquals( Breaker.Rule.FENCES, Breaker.ruleOf( wolf ) );
		for (int c : new int[]{ door, door - 1, door + 1, palisade }){
			assertFalse( "never a wall, a door or a palisade", Breaker.breakable( ow, c, Breaker.Rule.FENCES ) );
		}
		assertTrue( Breaker.breakable( ow, fence, Breaker.Rule.FENCES ) );
		assertTrue( Breaker.breakable( ow, gate, Breaker.Rule.FENCES ) );

		int[] walls = { door, door - 1, door + 1, palisade };
		int[] hp = new int[walls.length];
		for (int i = 0; i < walls.length; i++) hp[i] = Homestead.hpAt( ow, walls[i] );
		int fenceHp = Homestead.hpAt( ow, fence ) + Homestead.hpAt( ow, gate );
		for (int k = 0; k < 40; k++){
			int c = WolfSiege.fenceBeside( ow, wolf.pos );
			if (c == -1) break;
			assertTrue( c == fence || c == gate );
			wolf.wolfBite( Breaker.Order.strike( c, Math.max( 1, wolf.damageRoll() / WolfSiege.TEST_SHARE ) ) );
		}
		for (int i = 0; i < walls.length; i++) assertEquals( hp[i], Homestead.hpAt( ow, walls[i] ) );
		assertTrue( "the fences took it", Homestead.hpAt( ow, fence ) + Homestead.hpAt( ow, gate ) < fenceHp );

		//going round the hero, a wolf keeps the ring
		stand( ow, hut + 2 + W );
		int from = hut + 2 + W + 6;
		for (int dir : new int[]{ 1, -1 }){
			int step = WolfSiege.roundRing( ow, from, hero.pos, dir );
			if (step == -1) continue;
			int d = ow.distance( step, hero.pos );
			assertTrue( d >= WolfSiege.RING_MIN && d <= WolfSiege.RING_MAX );
			assertTrue( ow.adjacent( step, from ) );
		}

		//killed, its siege counts it (no hero's sprite to show his experience)
		wolf.EXP = 0;
		wolf.die( null );
		assertEquals( 1, e.slain );
	}

	// ------------------------------------------------------------------ away

	@Test
	public void anAbsenceSettlesTheSameEveryTimeAndCostsOneChestAtMost(){
		List<String> first = absence();
		assertTrue( "a raid came: " + first, first.size() > 0 );
		List<String> again = absence();
		assertEquals( "pure: the same nights, the same home, the same seed", first, again );
	}

	//thirty days away from a rich home: what is settled
	private List<String> absence(){
		HomesteadWorld.reset();
		Actor.clear();
		int[] o = new int[1];
		OverworldLevel ow = site( 13, 9, o );
		int hut = o[0] + 1 + W;
		int door = house( ow, hut, 11, 7, "wall_plank", "door" );
		int inside = hut + 5 + 3 * W;
		StorageChest[] chests = {
				chest( ow, inside - 3, new Gold( 3000 ), new Log().quantity( 5 ) ),
				chest( ow, inside, new Gold( 500 ) ),
				chest( ow, inside + 3, new Gold( 700 ), new Plank().quantity( 4 ) ) };
		//a hearthstone whose month away brings a raid (each cell rolls its own nights)
		Dungeon.cycleTurn = 40 * DayNightCycle.FULL_CYCLE + 700;
		HomesteadWorld.Home h = raidedHome( ow, inside, 10 * DayNightCycle.FULL_CYCLE + 600 );
		int doorHp = Homestead.hpAt( ow, door );

		List<String> lines = new ThreatSettler().settle( h, 30 );
		int emptied = 0;
		for (StorageChest c : chests) if (c.storage.items.isEmpty()) emptied++;
		assertEquals( "one chest, never more", 1, emptied );
		assertTrue( "the richest", chests[0].storage.items.isEmpty() );
		assertEquals( "the door nearest it forced: half its health", doorHp - BuildCatalog.byId( "door" ).maxHp / 2, Homestead.hpAt( ow, door ) );
		int robbed = 0, again = 0;
		for (String l : lines){
			assertFalse( l, l.contains( Messages.NO_TEXT_FOUND ) );
			if (l.startsWith( "Bandits broke" ) || l.startsWith( "Bandits forced" )) robbed++;
			if (l.startsWith( "Bandits came again" )) again++;
		}
		assertEquals( 1, robbed );
		//every night settled is rolled: none of them comes again, live or settled
		int rolled = ThreatWorld.get().rolled( h );
		assertEquals( "every night up to last night", WorldClock.night() - 1, rolled );
		assertTrue( new ThreatSettler().settle( h, 30 ).isEmpty() );
		return lines;
	}

	//a hearthstone near a cell, last visited at a turn, whose nights since bring a raid at the notice
	//its ground has now (each cell rolls its own nights): added to the run's homes
	private static HomesteadWorld.Home raidedHome( OverworldLevel ow, int near, int lastVisitTurn ){
		for (int k = 0; k < 60; k++){
			int c = near - 4 + (k % 9) + (k / 9 % 3 - 1) * W;
			HomesteadWorld.Home probe = new HomesteadWorld.Home( WorldLayers.SURFACE_DEPTH, ow.worldX + c % W, ow.worldY + c / W,
					"Oakhold", Math.floorDiv( lastVisitTurn, DayNightCycle.FULL_CYCLE ) );
			probe.lastVisitTurn = lastVisitTurn;
			int notice = Notice.of( ow, probe, true ).total();
			for (int n = ThreatSettler.firstNight( probe.lastVisitTurn ); ThreatSettler.past( n ); n++){
				if (HomeThreats.raidNight( ow.worldSeed, probe, n, notice )){
					HomesteadWorld.addHome( probe );
					return probe;
				}
			}
		}
		throw new AssertionError( "no hearthstone round the hut is raided in those nights" );
	}

	@Test
	public void anAbsenceIsSettledEvenWhenTheHomeComesIntoViewAtNightFirst(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int[] o = new int[1];
		OverworldLevel ow = site( 13, 9, o );
		int hut = o[0] + 1 + W;
		house( ow, hut, 11, 7, "wall_plank", "door" );
		int inside = hut + 5 + 3 * W;
		StorageChest rich = chest( ow, inside - 3, new Gold( 3000 ) );
		StorageChest poor = chest( ow, inside + 3, new Gold( 900 ) );
		//a week away, back in the dark: the home comes into his window while he is still far off
		Dungeon.cycleTurn = 40 * DayNightCycle.FULL_CYCLE + 2400;
		HomesteadWorld.Home h = raidedHome( ow, inside, 33 * DayNightCycle.FULL_CYCLE + 600 );
		int hearth = ow.localCell( h.wx, h.wy );
		stand( ow, hearth + 40 );
		assertFalse( HomeThreats.atHome( ow, h, hero.pos ) );

		ThreatSlice slice = ThreatSlice.of( ow );
		slice.worldTurn( ow );
		ThreatWorld world = ThreatWorld.get();
		assertEquals( "the week settled and tonight rolled live", WorldClock.night(), world.rolled( h ) );
		assertTrue( "the richest chest went with the week", rich.storage.items.isEmpty() );
		assertFalse( "one chest, never more", poor.storage.items.isEmpty() );
		assertTrue( "not told yet: he is not home", h.ledger().isEmpty() );

		//home at last: told, and written in the ledger
		stand( ow, hearth + 1 );
		AwaySettlers.visit( ow );
		boolean robbed = false;
		for (HomesteadWorld.Entry e : h.ledger()){
			assertFalse( e.text.contains( Messages.NO_TEXT_FOUND ) );
			if (e.text.startsWith( "Bandits broke" ) || e.text.startsWith( "Bandits forced" )) robbed = true;
		}
		assertTrue( "the robbery in the ledger: " + h.ledger().size(), robbed );
		int lines = h.ledger().size();
		slice.worldTurn( ow );
		assertEquals( "told once", lines, h.ledger().size() );
	}

	//a rich plank house with a hearthstone whose night `night` brings a raid, the hero beside it in the
	//day before it, the night before done with: the hearthstone's home
	private HomesteadWorld.Home sleeper( OverworldLevel ow, int inside, StorageChest[] chests ){
		chests[0] = chest( ow, inside - 3, new Gold( 3000 ) );
		chests[1] = chest( ow, inside + 3, new Gold( 900 ) );
		for (int k = 0; k < 60; k++){
			int c = inside - 4 + (k % 9) + (k / 9 % 3 - 1) * W;
			HomesteadWorld.Home probe = new HomesteadWorld.Home( WorldLayers.SURFACE_DEPTH, ow.worldX + c % W, ow.worldY + c / W, "Oakhold", 0 );
			int notice = Notice.of( ow, probe, true ).total();
			for (int night = 41; night < 140; night++){
				if (!HomeThreats.raidNight( ow.worldSeed, probe, night, notice )) continue;
				//its day, mid-morning: he was here all along
				Dungeon.cycleTurn = night * DayNightCycle.FULL_CYCLE + 300;
				probe.visited();
				HomesteadWorld.addHome( probe );
				assertEquals( night - 1, ThreatWorld.get().settledTo( probe ) );
				stand( ow, inside );
				return probe;
			}
		}
		throw new AssertionError( "no hearthstone round the house is raided in those nights" );
	}

	@Test
	public void aBellWakesTheSleeperAsTheNightsRaidComes(){
		DayNightCycle.debugPhaseOverride = null;
		int[] o = new int[1];
		OverworldLevel ow = site( 13, 9, o );
		int hut = o[0] + 1 + W;
		house( ow, hut, 11, 7, "wall_plank", "door" );
		int inside = hut + 5 + 3 * W;
		StorageChest[] chests = new StorageChest[2];
		HomesteadWorld.Home h = sleeper( ow, inside, chests );
		int night = WorldClock.day();
		put( ow, new AlarmBell(), inside + 1 );

		//lying down until dawn: the bell wakes him in the dark of that night instead
		int toDawn = (night + 1) * DayNightCycle.FULL_CYCLE - Dungeon.cycleTurn;
		int slept = AlarmBell.sleepFor( ow, hero, toDawn );
		assertTrue( "cut short: " + slept + " of " + toDawn, slept < toDawn );
		assertEquals( "pure: the same every time", slept, AlarmBell.sleepFor( ow, hero, toDawn ) );
		Dungeon.cycleTurn += slept;
		assertEquals( "woken in the dark", DayNightCycle.Phase.NIGHT, DayNightCycle.phase() );
		assertEquals( night, WorldClock.night() );

		hero.resting = true;
		ThreatSlice.of( ow ).worldTurn( ow );
		ThreatWorld world = ThreatWorld.get();
		assertEquals( "rolled once, live", night, world.rolled( h ) );
		boolean band = false;
		for (ThreatWorld.Event e : world.events( h )) if (e.kind == ThreatWorld.Event.RAID && e.came > 0) band = true;
		assertTrue( "the band came", band );
		assertFalse( "the bell woke him", hero.resting );
		assertFalse( "nothing taken yet", chests[0].storage.items.isEmpty() );
	}

	@Test
	public void withNoBellANightSleptThroughIsSettledOnWaking(){
		DayNightCycle.debugPhaseOverride = null;
		int[] o = new int[1];
		OverworldLevel ow = site( 13, 9, o );
		int hut = o[0] + 1 + W;
		house( ow, hut, 11, 7, "wall_plank", "door" );
		int inside = hut + 5 + 3 * W;
		StorageChest[] chests = new StorageChest[2];
		HomesteadWorld.Home h = sleeper( ow, inside, chests );
		int night = WorldClock.day();

		//no bell: he sleeps until dawn, and the night is settled as if he had been away
		int toDawn = (night + 1) * DayNightCycle.FULL_CYCLE - Dungeon.cycleTurn;
		assertEquals( toDawn, AlarmBell.sleepFor( ow, hero, toDawn ) );
		Dungeon.cycleTurn += toDawn;
		assertTrue( ThreatSettler.past( night ) );
		ThreatSlice.of( ow ).worldTurn( ow );
		assertEquals( "rolled once, settled", night, ThreatWorld.get().rolled( h ) );
		assertTrue( "the richest chest is gone", chests[0].storage.items.isEmpty() );
		assertFalse( chests[1].storage.items.isEmpty() );
		assertFalse( "told on waking", h.ledger().isEmpty() );
		assertTrue( ThreatWorld.get().events( h ).isEmpty() );
	}

	@Test
	public void aPalisadeRoundTheWholeGroundCountsAndKeepsTheBandsOutside(){
		int[] o = new int[1];
		OverworldLevel ow = site( 9, 7, o );
		int hearth = o[0] + 4 + 3 * W;
		//a square ring of palisade just past the home's ground, where bands come in
		int r = HomeThreats.AREA + 3;
		for (int dy = -r; dy <= r; dy++){
			for (int dx = -r; dx <= r; dx++){
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) == r) Level.set( hearth + dx + dy * W, Terrain.PALISADE, ow );
			}
		}
		assertEquals( "a closed ring round the whole ground", 1f, Notice.palisadeShare( ow, hearth ), 0.0001f );
		//the band comes in outside it: nowhere it could walk to the hearthstone from
		for (int k = 0; k < 8; k++){
			int c = HomeThreats.edgeCell( ow, hearth, k / 8f );
			assertTrue( c != -1 );
			PathFinder.buildDistanceMap( hearth, ow.passable );
			assertEquals( "outside the ring: " + ow.distance( c, hearth ), Integer.MAX_VALUE, PathFinder.distance[c] );
		}

		//a bag in a chest is worth what it holds
		Bag pouch = new VelvetPouch();
		pouch.items.add( new Gold( 500 ) );
		assertTrue( Notice.valueOf( pouch ) >= 500 );
	}

	@Test
	public void theWayAPackOrABandComesFromAlwaysReads(){
		for (int r : new int[]{ 1, HomeThreats.EDGE_MIN, HomeThreats.EDGE_MAX, Watchtower.SPOT_RANGE }){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					String from = HomeThreats.bearing( dx, dy );
					assertFalse( dx + "," + dy, from.contains( Messages.NO_TEXT_FOUND ) );
					for (String line : new String[]{
							Messages.get( WolfSiege.class, "came", "Oakhold", from ),
							Messages.get( HomeRaider.class, "came", "Oakhold", from ),
							Messages.get( ThreatSlice.class, "spotted_wolves", from ),
							Messages.get( ThreatSlice.class, "spotted_raiders", from ) }){
						assertFalse( line, line.contains( Messages.NO_TEXT_FOUND ) );
					}
				}
			}
		}
		assertEquals( "south", HomeThreats.bearing( -3, 16 ) );
		assertEquals( "north-east", HomeThreats.bearing( 16, -16 ) );
	}

	@Test
	public void theThreatsRecordsAndTheLedgerAreKeptWithTheRun() throws Exception {
		int[] o = new int[1];
		OverworldLevel ow = site( 9, 7, o );
		HomesteadWorld.Home h = home( ow, o[0] + 4 + 3 * W );
		h.note( 3, "Wolves circled Oakhold." );
		ThreatWorld world = ThreatWorld.get();
		Notice.Score s = new Notice.Score();
		s.parts[Notice.Part.CHESTS.ordinal()] = 22;
		s.parts[Notice.Part.BARRED.ordinal()] = -5;
		world.keepNotice( h, s );
		world.roll( h, 17 );
		ThreatWorld.Event e = world.begin( h, ThreatWorld.Event.RAID, 17 );
		e.came = 4;
		e.slain = 2;
		e.stolen = 3;

		Bundle b = new Bundle();
		HomesteadWorld.storeInBundle( b );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		HomesteadWorld.reset();
		assertNull( ThreatWorld.get().event( HomeThreats.key( h ), e.id ) );
		HomesteadWorld.restoreFromBundle( Bundle.read( new ByteArrayInputStream( out.toByteArray() ) ) );

		HomesteadWorld.Home back = HomesteadWorld.homeAt( h.depth, h.wx, h.wy );
		assertEquals( "Wolves circled Oakhold.", back.ledger().get( 0 ).text );
		ThreatWorld w2 = ThreatWorld.get();
		assertEquals( 22, w2.notice( back ).get( Notice.Part.CHESTS ) );
		assertEquals( -5, w2.notice( back ).get( Notice.Part.BARRED ) );
		assertEquals( 17, w2.rolled( back ) );
		ThreatWorld.Event e2 = w2.event( HomeThreats.key( back ), e.id );
		assertNotNull( e2 );
		assertEquals( 4, e2.came );
		assertEquals( 2, e2.slain );
		assertEquals( 3, e2.stolen );
		assertEquals( ThreatWorld.Event.RAID, e2.kind );

		//its window in the menu: the threats' own, in the built-in one's place
		List<HomeActions.Action> actions = HomeActions.forHome( back, hero );
		int ledgers = 0;
		for (HomeActions.Action a : actions){
			if (a.id.equals( ThreatActions.LEDGER )){
				ledgers++;
				assertEquals( ThreatActions.class, a.owner );
			}
			assertFalse( a.label().contains( Messages.NO_TEXT_FOUND ) );
		}
		assertEquals( 1, ledgers );

		//a home taken down takes its records with it
		HomesteadWorld.removeHome( back.depth, back.wx, back.wy );
		Bundle c = new Bundle();
		HomesteadWorld.storeInBundle( c );
		HomesteadWorld.restoreFromBundle( c );
		assertEquals( Integer.MIN_VALUE, ThreatWorld.get().rolled( back ) );
	}

	// ------------------------------------------------------------------ the defences

	@Test
	public void anAlarmBellWakesTheSleeperBeforeTheAmbushLands(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int[] o = new int[1];
		OverworldLevel ow = site( 9, 7, o );
		int hearth = o[0] + 4 + 3 * W;
		HomesteadWorld.Home h = home( ow, hearth );
		stand( ow, hearth + 1 );
		ArrayList<Mob> found = new ArrayList<>( Arrays.asList( new Rat(), new Rat() ) );

		assertFalse( "no bell: the ambush as it was", AlarmBell.wakeSleeper( ow, hero, found ) );
		AlarmBell bell = put( ow, new AlarmBell(), hearth - 2 );
		hero.resting = true;
		assertTrue( AlarmBell.wakeSleeper( ow, hero, found ) );
		assertFalse( "awake", hero.resting );
		for (Mob m : found){
			assertTrue( ow.mobs.contains( m ) );
			assertTrue( "met at the edge of the home's ground, not beside him",
					HomeThreats.distance( ow, h, m.pos ) >= HomeThreats.EDGE_MIN );
			assertEquals( m.HUNTING, m.state );
		}
		//off the home's ground there is no bell to ring
		stand( ow, hearth + (HomeThreats.AREA + 3) );
		assertFalse( AlarmBell.wakeSleeper( ow, hero, new ArrayList<>( Arrays.asList( new Rat() ) ) ) );

		//awake or not, it rings when something hostile comes onto the ground while he sleeps there
		stand( ow, hearth + 1 );
		ow.mobs.removeAll( found );
		ThreatSlice slice = ThreatSlice.of( ow );
		slice.worldTurn( ow );
		Buff.affect( hero, MagicalSleep.class );
		hero.resting = true;
		put( ow, new Rat(), hearth + 5 );
		slice.worldTurn( ow );
		assertFalse( hero.resting );
		assertNull( "woken from a magical sleep too", hero.buff( MagicalSleep.class ) );
		assertNotNull( bell );
	}

	@Test
	public void spikeTrapsBiteMonstersAndSpareTheHeroAndHisAllies(){
		int[] o = new int[1];
		OverworldLevel ow = site( 9, 7, o );
		int cell = o[0] + 4 + 3 * W;
		BuildPiece spikes = BuildCatalog.byId( ThreatSlice.SPIKE_PIECE );
		assertNotNull( spikes );
		assertEquals( BuildCategory.DEFENCE, spikes.category );
		Homestead.build( ow, cell, spikes, null );
		assertTrue( "on walkable ground", ow.passable[cell] );
		assertTrue( ow.traps.get( cell ) instanceof SpikeTrap );
		assertSame( spikes, Homestead.pieceAt( ow, cell ) );
		SpikeTrap trap = (SpikeTrap) ow.traps.get( cell );
		assertTrue( trap.visible );
		assertTrue( trap.armed() );

		Rat rat = new Rat();
		Rat ally = new Rat();
		ally.alignment = Char.Alignment.ALLY;
		Rat neutral = new Rat();
		neutral.alignment = Char.Alignment.NEUTRAL;
		assertFalse( SpikeTrap.springsFor( hero ) );
		assertFalse( SpikeTrap.springsFor( ally ) );
		assertFalse( SpikeTrap.springsFor( neutral ) );
		assertTrue( SpikeTrap.springsFor( rat ) );

		//the hero and an ally walk over it: nothing
		hero.pos = cell;
		ow.pressCell( cell );
		assertTrue( trap.armed() );
		hero.pos = cell + 3;
		put( ow, ally, cell );
		Actor.add( ally );
		int allyHp = ally.HP;
		ow.pressCell( cell );
		assertEquals( allyHp, ally.HP );
		assertTrue( trap.armed() );
		ow.mobs.remove( ally );
		Actor.remove( ally );

		//a monster: impaled, crippled, the trap sprung for REARM turns
		put( ow, rat, cell );
		Actor.add( rat );
		rat.HP = rat.HT = 500;
		ow.pressCell( cell );
		assertTrue( rat.HP < 500 );
		assertFalse( trap.armed() );
		assertEquals( SpikeTrap.SPRUNG, trap.color );
		int hp = rat.HP;
		ow.pressCell( cell );
		assertEquals( "sprung, it waits", hp, rat.HP );

		//taken down: no trap left behind
		Homestead.build( ow, cell + 1, spikes, null );
		assertTrue( ow.traps.get( cell + 1 ) instanceof SpikeTrap );
		ThreatSlice.of( ow ).lift( ow, cell + 1 );
		assertNull( ow.traps.get( cell + 1 ) );
	}

	@Test
	public void theWatchtowerShowsFartherFromItsTopAndTheDefencesAreInTheMenu(){
		int[] o = new int[1];
		OverworldLevel ow = site( 9, 7, o );
		int base = o[0] + 4 + 3 * W;
		Watchtower tower = put( ow, new Watchtower(), base );
		stand( ow, base + 1 );
		int view = ow.viewDistance;
		hero.viewDistance = view;
		Lookout up = Lookout.climb( hero, ow );
		assertEquals( view + Watchtower.VIEW_BONUS, hero.viewDistance );
		assertSame( tower, up.tower() );
		up.act();
		assertNotNull( "still up", hero.buff( Lookout.class ) );
		assertEquals( view + Watchtower.VIEW_BONUS, hero.viewDistance );
		assertFalse( up.desc().contains( Messages.NO_TEXT_FOUND ) );

		//a step off and he is down
		hero.pos = base + 2;
		up.act();
		assertNull( hero.buff( Lookout.class ) );
		assertEquals( view, hero.viewDistance );

		//the pieces as the build menu lists them
		BuildPiece t = BuildCatalog.byId( "watchtower" );
		assertEquals( BuildPiece.Kind.ACTOR, t.kind );
		assertEquals( 80, t.maxHp );
		assertEquals( 8, cost( t, Plank.class ) );
		assertEquals( 4, cost( t, Log.class ) );
		assertEquals( 2, cost( t, Rope.class ) );
		BuildPiece bell = BuildCatalog.byId( "alarm_bell" );
		assertEquals( 40, bell.maxHp );
		assertEquals( 2, cost( bell, IronFittings.class ) );
		assertEquals( 2, cost( bell, Plank.class ) );
		assertEquals( 1, cost( bell, Rope.class ) );
		BuildPiece spikes = BuildCatalog.byId( ThreatSlice.SPIKE_PIECE );
		assertEquals( 2, cost( spikes, IronFittings.class ) );
		assertEquals( 1, cost( spikes, Plank.class ) );
		for (BuildPiece p : new BuildPiece[]{ t, bell, spikes }){
			assertEquals( BuildCategory.DEFENCE, p.category );
			assertFalse( p.name().contains( Messages.NO_TEXT_FOUND ) );
			assertFalse( p.desc().contains( Messages.NO_TEXT_FOUND ) );
		}
		assertFalse( tower.name().contains( Messages.NO_TEXT_FOUND ) );
		assertFalse( new AlarmBell().name().contains( Messages.NO_TEXT_FOUND ) );
		assertFalse( new SiegeGrayWolf().name().contains( Messages.NO_TEXT_FOUND ) );
		assertFalse( new HomeRaider().description().contains( Messages.NO_TEXT_FOUND ) );
	}

	private static int cost( BuildPiece p, Class<? extends Item> material ){
		for (BuildPiece.Cost c : p.cost) if (c.material == material) return c.quantity;
		return 0;
	}
}

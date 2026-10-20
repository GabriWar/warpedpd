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

package xyz.gabriwar.warpedpixeldungeon.homestead.gathering;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.items.BuildersTool;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Clay;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GlassPane;
import xyz.gabriwar.warpedpixeldungeon.items.materials.IronFittings;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.PineLog;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Resin;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Sand;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.items.ore.IronOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Ore;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DarkGold;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Axe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.BattleAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greataxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MasonsHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MinersTool;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shovel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WoodcuttersAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.MeisterHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.CromCruachAxe;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.MiningLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.TownShopLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.net.TestParty;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.json.JSONArray;
import org.json.JSONObject;
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
import java.util.HashSet;
import java.util.function.IntPredicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The gathering (homestead/gathering): each tool's turns, what felling, quarrying, digging and
 * reaping give and cost, the tree falling on whoever stands beyond it, the axe's noise, the work
 * kept when called away, the cells nobody may work, stumps growing back only through spring and
 * summer (and through a save), the beasts' hides, the town store's wares and prices and the
 * village carpenter. Every window is a real one of the surface, derived from a seed.
 */
public class GatheringTest {

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
		Dungeon.cycleTurn = 100;
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
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		HomesteadWorld.reset();
	}

	// ------------------------------------------------------------------ helpers

	//the live surface window round a world cell, built as a mirror's is and made the host's own
	private static OverworldLevel windowAt( int wx, int wy ){
		PathFinder.setMapSize( W, H );
		float s = WorldModel.calendarShift();
		int ox = wx - W / 2, oy = wy - H / 2;
		WindowGenerator.Window w = WindowGenerator.generate( SEED, 0, ox, oy, s );
		OverworldLevel ow = OverworldLevel.forNetwork( 0, SEED, ox, oy, s, GameCalendar.season(), w.terrain, W, H );
		assertNotNull( ow );
		set( ow, "network", false );
		Dungeon.level = ow;
		Arrays.fill( ow.heroFOV, false );
		return ow;
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

	private static Object get( Object o, String name ){
		try {
			Field f = OverworldLevel.class.getDeclaredField( name );
			f.setAccessible( true );
			return f.get( o );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	//the nearest world point to the origin (on a 64-cell grid) of a biome, its window
	private static OverworldLevel windowIn( WorldModel.Biome biome ){
		for (int r = 0; r <= 40; r++){
			for (int j = -r; j <= r; j++){
				for (int i = -r; i <= r; i++){
					if (Math.max( Math.abs( i ), Math.abs( j ) ) != r) continue;
					int x = i * 64, y = j * 64;
					if (WorldModel.biomeAt( SEED, x, y ) != biome || WorldStructures.townCell( x, y ) != -1
							|| OverworldFauna.nearSettlement( SEED, x, y )) continue;
					return windowAt( x, y );
				}
			}
		}
		throw new AssertionError( "no " + biome + " near the origin" );
	}

	//the window cell nearest its middle the test likes (inside the rim), -1 for none
	private static int find( OverworldLevel ow, IntPredicate test ){
		int mid = W / 2 + (H / 2) * W;
		for (int r = 0; r < W / 2 - 4; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int c = mid + dx + dy * W;
					if (test.test( c )) return c;
				}
			}
		}
		return -1;
	}

	//open, unedited, unprotected ground the hero may stand on
	private static boolean ground( OverworldLevel ow, int c ){
		int t = ow.map[c];
		return ow.passable[c] && !ow.pit[c] && (Terrain.flags[t] & Terrain.LIQUID) == 0 && t != Terrain.FROZEN_WATER
				&& !Terrain.homestead( t ) && !ow.protectedForBuilding( c ) && ow.pristineAt( c ) == t
				&& Actor.findChar( c ) == null && ow.heaps.get( c ) == null;
	}

	//a cell of ground beside a cell (and one past it on the other side for the fall), -1 for none
	private static int standBeside( OverworldLevel ow, int c ){
		for (int n : PathFinder.NEIGHBOURS4){
			if (ground( ow, c + n )) return c + n;
		}
		return -1;
	}

	//a tree of a kind, free to fell (no cell of a place, no village's), with ground beside it
	private int tree( OverworldLevel ow, int kind ){
		int c = find( ow, x -> ow.map[x] == kind && standBeside( ow, x ) != -1 && !ow.protectedForBuilding( x )
				&& !OverworldFauna.nearSettlement( SEED, ow.worldX + x % W, ow.worldY + x / W ) );
		assertNotEquals( "a tree to fell", -1, c );
		return c;
	}

	//the hero standing on a cell, seeing everything, on the actors' roll
	private void stand( OverworldLevel ow, int cell ){
		hero.pos = cell;
		hero.fieldOfView = new boolean[ow.length()];
		Arrays.fill( hero.fieldOfView, true );
		if (!Actor.all().contains( hero )) Actor.add( hero );
	}

	//into the pack. A tool handed over bare stays bare, as the starting kit's are: the hand-over
	//rolls it a type (Quality.roll), and a typed axe or pick works faster (typed)
	private void give( Item item ){
		boolean bare = item.quality == null;
		item.identify( false );
		assertTrue( item.collect( hero.belongings.backpack ) );
		if (bare) item.quality = null;
	}

	//a tool of a type: a Woodsman's axe or a Quarry's hammer, pick or shovel works in fewer strokes
	private static <T extends Item> T typed( T item, ItemType type ){
		Quality.roll( item, Rarity.COMMON, type, 0 );
		assertEquals( type, Quality.of( item ).type );
		return item;
	}

	//every stroke of a job to its end; how many it took
	private int work( int cell, Gathering.Job job ){
		final boolean[] done = { false };
		int strokes = 0;
		while (!done[0]){
			assertTrue( "stroke " + (strokes + 1) + " of " + job, Gathering.stroke( hero, cell, job, d -> done[0] = d ) );
			strokes++;
			assertTrue( "a job ends", strokes < 20 );
		}
		return strokes;
	}

	private static int count( Heap h, Class<? extends Item> cls ){
		int n = 0;
		if (h == null) return 0;
		for (Item i : h.items) if (i.getClass() == cls) n += i.quantity();
		return n;
	}

	private static void clear( OverworldLevel ow, int cell ){
		Heap h = ow.heaps.get( cell );
		if (h != null) ow.heaps.remove( cell );
	}

	//a monster that only takes note of where it is called (no sprite to show its alert)
	public static class Listener extends Rat {
		public int called = -1;
		@Override public void beckon( int cell ){
			called = cell;
		}
	}

	private Listener listener( OverworldLevel ow, int cell ){
		Listener l = new Listener();
		l.pos = cell;
		ow.mobs.add( l );
		Actor.add( l );
		return l;
	}

	//the first day (from `from` on) after which `n` days in a row are of a season (calendarStartDay 0)
	private static int seasonRun( GameCalendar.Season season, int n ){
		return seasonRun( season, n, 0 );
	}

	private static int seasonRun( GameCalendar.Season season, int n, int from ){
		for (int d = from; d < from + 2000; d++){
			boolean all = true;
			for (int k = 1; k <= n && all; k++) all = GameCalendar.seasonOfDay( d + k ) == season;
			if (all) return d;
		}
		throw new AssertionError( "no run of " + season );
	}

	private static void toDay( int day ){
		Dungeon.cycleTurn = day * DayNightCycle.FULL_CYCLE + 100;
		assertEquals( day, WorldClock.day() );
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

	// ------------------------------------------------------------------ the tools

	@Test
	public void eachAxeHasItsTurnsAndTheOneInHandComesFirst(){
		assertEquals( 8, Gathering.axeTurns( new HandAxe() ) );
		assertEquals( 6, Gathering.axeTurns( new Axe() ) );
		assertEquals( 5, Gathering.axeTurns( new BattleAxe() ) );
		assertEquals( 4, Gathering.axeTurns( new Greataxe() ) );
		assertEquals( 3, Gathering.axeTurns( new WoodcuttersAxe() ) );
		assertEquals( 0, Gathering.axeTurns( new Pickaxe() ) );
		assertEquals( 0, Gathering.axeTurns( null ) );

		assertNull( Gathering.axe( hero ) );
		HandAxe hand = new HandAxe();
		give( hand );
		give( new BattleAxe() );
		assertEquals( "the quickest in the pack", BattleAxe.class, Gathering.axe( hero ).getClass() );
		hero.belongings.weapon = hand;
		assertEquals( "the axe in his hand first", hand, Gathering.axe( hero ) );
		hero.belongings.weapon = new Sickle();
		assertEquals( "a weapon that is no axe is passed over", BattleAxe.class, Gathering.axe( hero ).getClass() );
	}

	@Test
	public void aBoulderBreaksQuickerUnderTheHammer(){
		assertNull( Gathering.quarryTool( hero ) );
		give( new Pickaxe() );
		assertEquals( 3, Gathering.quarryTurns( Gathering.quarryTool( hero ) ) );
		give( new MasonsHammer() );
		assertTrue( Gathering.quarryTool( hero ) instanceof MasonsHammer );
		assertEquals( 2, Gathering.quarryTurns( Gathering.quarryTool( hero ) ) );
		assertEquals( 3, Gathering.HAMMER_WALL_TURNS );
	}

	@Test
	public void aTapBesideATreeOrABoulderPutsTheHeroToWork(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		int at = standBeside( ow, oak );
		stand( ow, at );
		assertNull( "no axe, no work", Gathering.tapJob( hero, oak ) );
		give( new HandAxe() );
		assertEquals( Gathering.Job.CHOP, Gathering.tapJob( hero, oak ) );
		stand( ow, at + (at - oak) );
		assertNull( "a tree farther off is only walked to", Gathering.tapJob( hero, oak ) );

		stand( ow, at );
		int rock = find( ow, c -> ground( ow, c ) && ow.adjacent( c, at ) && c != oak );
		Level.set( rock, Terrain.BOULDER, ow );
		assertNull( Gathering.tapJob( hero, rock ) );
		give( new Pickaxe() );
		assertEquals( Gathering.Job.QUARRY, Gathering.tapJob( hero, rock ) );
	}

	// ------------------------------------------------------------------ felling

	@Test
	public void anOakFallsAfterItsStrokesForLogsAndHunger(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		stand( ow, standBeside( ow, oak ) );
		Hunger hunger = Buff.affect( hero, Hunger.class );
		int before = hunger.hunger();
		give( new WoodcuttersAxe() );
		final boolean[] done = { false };
		for (int i = 1; i <= 2; i++){
			assertTrue( Gathering.stroke( hero, oak, Gathering.Job.CHOP, d -> done[0] = d ) );
			assertFalse( done[0] );
			assertEquals( Terrain.TREE_OAK, ow.map[oak] );
			assertEquals( i, Gathering.strokesAt( hero, oak, Gathering.Job.CHOP ) );
		}
		assertTrue( Gathering.stroke( hero, oak, Gathering.Job.CHOP, d -> done[0] = d ) );
		assertTrue( "down on the third stroke of the woodcutter's axe", done[0] );
		assertEquals( Terrain.STUMP_OAK, ow.map[oak] );
		int logs = count( ow.heaps.get( oak ), Log.class );
		assertTrue( "3-4 oak logs at the stump: " + logs, logs >= 3 && logs <= 4 );
		assertEquals( before + Gathering.CHOP_HUNGER, hunger.hunger() );
		GatheringSlice slice = ow.homestead().section( GatheringSlice.class );
		assertEquals( (Integer) WorldClock.day(), slice.felledOn( ow.worldX + oak % W, ow.worldY + oak / W ) );
		assertEquals( 0, Gathering.strokesAt( hero, oak, Gathering.Job.CHOP ) );
		//the stump left is walked over on a plain tap; a held one chops it out with the axe
		assertNull( Gathering.tapJob( hero, oak ) );
		assertEquals( Gathering.Job.CHOP, heldTapJob( hero, oak ) );
		assertEquals( Gathering.lightChop( Gathering.WOODCUTTERS_AXE_TURNS ), Gathering.turns( hero, oak, Gathering.Job.CHOP ) );
	}

	@Test
	public void eachAxeTakesItsOwnStrokes(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		stand( ow, standBeside( ow, oak ) );
		MeleeWeapon[] axes = { new HandAxe(), new Axe(), new BattleAxe(), new Greataxe(), new WoodcuttersAxe() };
		int[] turns = { 8, 6, 5, 4, 3 };
		for (int i = 0; i < axes.length; i++){
			hero.belongings.weapon = axes[i];
			Level.set( oak, Terrain.TREE_OAK, ow );
			assertEquals( axes[i].getClass().getSimpleName(), turns[i], work( oak, Gathering.Job.CHOP ) );
			assertEquals( Terrain.STUMP_OAK, ow.map[oak] );
		}
	}

	@Test
	public void aPineGivesPineLogsAndSometimesResin(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int pine = tree( ow, Terrain.TREE_PINE );
		stand( ow, standBeside( ow, pine ) );
		int resin = 0, trials = 400;
		for (int i = 0; i < trials; i++){
			Level.set( pine, Terrain.TREE_PINE, ow );
			clear( ow, pine );
			Gathering.fell( hero, ow, pine );
			assertEquals( Terrain.STUMP_PINE, ow.map[pine] );
			Heap h = ow.heaps.get( pine );
			int logs = count( h, PineLog.class );
			assertTrue( "2-3 pine logs: " + logs, logs >= 2 && logs <= 3 );
			assertEquals( 0, count( h, Log.class ) );
			resin += count( h, Resin.class );
		}
		assertTrue( "resin about 3 in 10: " + resin, Math.abs( resin / (float) trials - Gathering.RESIN_CHANCE ) < 0.07f );
	}

	@Test
	public void theTreeFallsOnWhoeverStandsBeyondIt(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		int at = standBeside( ow, oak );
		stand( ow, at );
		int landing = oak + (oak - at);
		Listener beyond = listener( ow, landing );
		beyond.HP = 30;
		Listener behind = listener( ow, at + (at - oak) );
		behind.HP = 30;
		give( new WoodcuttersAxe() );
		work( oak, Gathering.Job.CHOP );
		assertEquals( "a third of its health, away from the hero", 20, beyond.HP );
		assertEquals( "nothing behind the hero", 30, behind.HP );
	}

	@Test
	public void theAxeWakesMonstersWithinEightCells(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		stand( ow, standBeside( ow, oak ) );
		int ox = oak % W, oy = oak / W;
		Listener near = listener( ow, (ox + Gathering.NOISE_RANGE) + oy * W );
		Listener far = listener( ow, (ox - Gathering.NOISE_RANGE - 1) + oy * W );
		Listener friend = listener( ow, ox + (oy + 2) * W );
		friend.alignment = xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ALLY;
		give( new HandAxe() );
		assertTrue( Gathering.stroke( hero, oak, Gathering.Job.CHOP, d -> {} ) );
		assertEquals( "eight cells off it hears the axe", oak, near.called );
		assertEquals( "nine cells off it does not", -1, far.called );
		assertEquals( "an ally is not called", -1, friend.called );
	}

	@Test
	public void theStrokesStruckAreKeptWhenCalledAway(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		int pine = tree( ow, Terrain.TREE_PINE );
		stand( ow, standBeside( ow, oak ) );
		give( new HandAxe() );
		for (int i = 0; i < 5; i++) Gathering.stroke( hero, oak, Gathering.Job.CHOP, d -> {} );
		assertEquals( 5, Gathering.strokesAt( hero, oak, Gathering.Job.CHOP ) );
		//called away (the work interrupted), then back to the same tree: three more fell it
		assertEquals( 3, work( oak, Gathering.Job.CHOP ) );
		assertEquals( Terrain.STUMP_OAK, ow.map[oak] );
		//another tree starts afresh
		stand( ow, standBeside( ow, pine ) );
		Gathering.stroke( hero, pine, Gathering.Job.CHOP, d -> {} );
		assertEquals( 1, Gathering.strokesAt( hero, pine, Gathering.Job.CHOP ) );
		assertEquals( 0, Gathering.strokesAt( hero, oak, Gathering.Job.CHOP ) );
	}

	// ------------------------------------------------------------------ quarrying

	@Test
	public void aBoulderGivesRoughStone(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int at = find( ow, c -> ground( ow, c ) && ground( ow, c + 1 ) );
		stand( ow, at );
		int rock = at + 1;
		Hunger hunger = Buff.affect( hero, Hunger.class );
		give( new Pickaxe() );
		Level.set( rock, Terrain.BOULDER, ow );
		int before = hunger.hunger();
		assertEquals( "the pick takes three", 3, work( rock, Gathering.Job.QUARRY ) );
		assertEquals( Terrain.EMPTY_DECO, ow.map[rock] );
		int stone = count( ow.heaps.get( rock ), RoughStone.class );
		assertTrue( "2-3 rough stone: " + stone, stone >= 2 && stone <= 3 );
		assertEquals( before + Gathering.QUARRY_HUNGER, hunger.hunger() );

		give( new MasonsHammer() );
		clear( ow, rock );
		Level.set( rock, Terrain.BOULDER, ow );
		assertEquals( "the hammer two", 2, work( rock, Gathering.Job.QUARRY ) );
	}

	@Test
	public void theWorldsRockGivesStoneAndAVillagesWallsNone(){
		OverworldLevel ow = windowIn( WorldModel.Biome.MOUNTAIN );
		int rock = find( ow, c -> ow.map[c] == Terrain.WALL && !ow.protectedForBuilding( c ) && ow.heaps.get( c ) == null );
		assertNotEquals( "rock on the mountain", -1, rock );
		Level.set( rock, Terrain.EMPTY_DECO, ow );
		Gathering.wallStone( ow, rock, hero );
		int stone = count( ow.heaps.get( rock ), RoughStone.class );
		assertTrue( "1-2 rough stone: " + stone, stone >= 1 && stone <= 2 );

		long[] village = humanVillage();
		VillageRoutine.Settlement s = VillageRoutine.settlement( SEED, village[0] );
		OverworldLevel vw = windowAt( s.cx, s.cy );
		int wall = find( vw, c -> vw.map[c] == Terrain.WALL && vw.protectedForBuilding( c ) && vw.heaps.get( c ) == null );
		assertNotEquals( "a house wall in the village", -1, wall );
		Gathering.wallStone( vw, wall, hero );
		assertNull( "a house's wall is masonry, not the land's rock", vw.heaps.get( wall ) );
	}

	// ------------------------------------------------------------------ digging and reaping

	@Test
	public void anAxeClearsABushAndChopsOutAStump(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		int at = standBeside( ow, oak );
		stand( ow, at );
		give( new WoodcuttersAxe() );

		//a bush beside him: a tap puts him to clearing it, half a tree's strokes, grass left
		//any cell beside him but the oak's, made a bush (whatever stood there)
		int bush = -1;
		for (int d : com.watabou.utils.PathFinder.NEIGHBOURS8){
			int c = at + d;
			if (bush == -1 && c != oak && Actor.findChar( c ) == null && !ow.protectedForBuilding( c )) bush = c;
		}
		assertNotEquals( "a cell beside him for a bush", -1, bush );
		clear( ow, bush );
		Level.set( bush, Terrain.SHRUB, ow );
		assertEquals( Gathering.Job.CHOP, Gathering.tapJob( hero, bush ) );
		assertEquals( Gathering.lightChop( Gathering.WOODCUTTERS_AXE_TURNS ), work( bush, Gathering.Job.CHOP ) );
		assertEquals( Terrain.GRASS, ow.map[bush] );

		//the oak felled, its stump chopped out by the axe too: grass, one more log, no regrowth
		work( oak, Gathering.Job.CHOP );
		assertEquals( Terrain.STUMP_OAK, ow.map[oak] );
		int logs = count( ow.heaps.get( oak ), Log.class );
		assertNull( Gathering.tapJob( hero, oak ) );
		assertEquals( Gathering.Job.CHOP, heldTapJob( hero, oak ) );
		assertEquals( Gathering.lightChop( Gathering.WOODCUTTERS_AXE_TURNS ), work( oak, Gathering.Job.CHOP ) );
		assertEquals( Terrain.GRASS, ow.map[oak] );
		assertEquals( logs + 1, count( ow.heaps.get( oak ), Log.class ) );
		GatheringSlice slice = ow.homestead().section( GatheringSlice.class );
		assertNull( slice.felledOn( ow.worldX + oak % W, ow.worldY + oak / W ) );

		//a hand axe takes half its tree strokes, rounded up
		assertEquals( 4, Gathering.lightChop( Gathering.HAND_AXE_TURNS ) );
	}

	@Test
	public void seedsArePlantedAndGrowIntoTreesAndBushes(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int at = find( ow, c -> ground( ow, c ) && ow.map[c] == Terrain.GRASS );
		stand( ow, at );
		int[] spots = new int[3];
		int n = 0;
		for (int d : com.watabou.utils.PathFinder.NEIGHBOURS8){
			int c = at + d;
			if (n < 3 && ground( ow, c ) && ow.map[c] == Terrain.GRASS && Actor.findChar( c ) == null) spots[n++] = c;
		}
		assertEquals( "three grass cells beside him", 3, n );
		xyz.gabriwar.warpedpixeldungeon.items.treeseeds.TreeSeed[] seeds = {
				new xyz.gabriwar.warpedpixeldungeon.items.treeseeds.Acorn(),
				new xyz.gabriwar.warpedpixeldungeon.items.treeseeds.PineCone(),
				new xyz.gabriwar.warpedpixeldungeon.items.treeseeds.BushSeed() };
		int[] sapling = { Terrain.SAPLING_OAK, Terrain.SAPLING_PINE, Terrain.SAPLING_SHRUB };
		int[] grown = { Terrain.TREE_OAK, Terrain.TREE_PINE, Terrain.SHRUB };
		GatheringSlice slice = ow.homestead().section( GatheringSlice.class );
		int summer = seasonRun( GameCalendar.Season.SUMMER, 20 );
		for (int i = 0; i < 3; i++){
			give( seeds[i] );
			assertNull( xyz.gabriwar.warpedpixeldungeon.items.treeseeds.TreeSeed.refusal( hero, spots[i] ) );
			seeds[i].plant( hero, spots[i] );
			assertEquals( sapling[i], ow.map[spots[i]] );
			assertNotNull( "followed from the day it went in", slice.felledOn( ow.worldX + spots[i] % W, ow.worldY + spots[i] / W ) );
			slice.planted( ow, spots[i], summer );
		}
		//the bush fills out after its 6 growing days; the trees still wait
		slice.regrow( ow, summer + GatheringSlice.BUSH_DAYS );
		assertEquals( Terrain.SAPLING_OAK, ow.map[spots[0]] );
		assertEquals( Terrain.SAPLING_PINE, ow.map[spots[1]] );
		assertEquals( Terrain.SHRUB, ow.map[spots[2]] );
		//the trees after 14
		slice.regrow( ow, summer + GatheringSlice.TREE_DAYS );
		for (int i = 0; i < 3; i++) assertEquals( grown[i], ow.map[spots[i]] );

		//no seed on water, on a tree, or out of reach
		int water = find( ow, c -> ow.map[c] == Terrain.WATER );
		if (water != -1 && !ow.adjacent( at, water )) assertNotNull( xyz.gabriwar.warpedpixeldungeon.items.treeseeds.TreeSeed.refusal( hero, water ) );
		assertEquals( "wrong_ground", xyz.gabriwar.warpedpixeldungeon.items.treeseeds.TreeSeed.refusal( hero, spots[0] ) );
	}

	@Test
	public void fellingAndClearingDropTheirSeedsHalfTheTime(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		stand( ow, standBeside( ow, oak ) );
		int acorns = 0, bushSeeds = 0, trials = 400;
		for (int i = 0; i < trials; i++){
			Level.set( oak, Terrain.TREE_OAK, ow );
			clear( ow, oak );
			Gathering.fell( hero, ow, oak );
			if (count( ow.heaps.get( oak ), xyz.gabriwar.warpedpixeldungeon.items.treeseeds.Acorn.class ) > 0) acorns++;
			Level.set( oak, Terrain.SHRUB, ow );
			clear( ow, oak );
			Gathering.clearBush( hero, ow, oak );
			if (count( ow.heaps.get( oak ), xyz.gabriwar.warpedpixeldungeon.items.treeseeds.BushSeed.class ) > 0) bushSeeds++;
		}
		assertEquals( "an acorn about half the time: " + acorns, Gathering.TREE_SEED_CHANCE, acorns / (float) trials, 0.08f );
		assertEquals( "a bush seed about half the time: " + bushSeeds, Gathering.BUSH_SEED_CHANCE, bushSeeds / (float) trials, 0.08f );
	}

	@Test
	public void aTorchIsSetInTheGroundAnchorsTheWildAndComesBack(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int at = find( ow, c -> ground( ow, c ) && ow.map[c] == Terrain.GRASS );
		stand( ow, at );
		int spot = -1;
		for (int d : com.watabou.utils.PathFinder.NEIGHBOURS8){
			int c = at + d;
			if (spot == -1 && ground( ow, c ) && ow.passable[c] && Actor.findChar( c ) == null) spot = c;
		}
		assertNotEquals( -1, spot );
		assertNull( xyz.gabriwar.warpedpixeldungeon.items.Torch.placeRefusal( hero, spot ) );
		assertEquals( "place_too_far", xyz.gabriwar.warpedpixeldungeon.items.Torch.placeRefusal( hero, spot + 3 * W ) );

		xyz.gabriwar.warpedpixeldungeon.homestead.torches.StandingTorch torch = new xyz.gabriwar.warpedpixeldungeon.homestead.torches.StandingTorch();
		torch.pos = spot;
		ow.mobs.add( torch );
		Actor.add( torch );
		assertTrue( "it keeps for good", torch instanceof xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadActor );
		final boolean[] anchored = { false };
		new xyz.gabriwar.warpedpixeldungeon.homestead.torches.TorchAnchors().anchors( ow, c -> anchored[0] |= c == torch.pos );
		assertTrue( "a standing torch anchors the wild", anchored[0] );
		assertEquals( "place_occupied", xyz.gabriwar.warpedpixeldungeon.items.Torch.placeRefusal( hero, spot ) );
	}

	@Test
	public void aTreeBurntDownLeavesCharcoal(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		clear( ow, oak );
		ow.destroy( oak );
		assertEquals( Terrain.EMBERS, ow.map[oak] );
		int coal = count( ow.heaps.get( oak ), xyz.gabriwar.warpedpixeldungeon.items.materials.Charcoal.class );
		assertTrue( "1-2 charcoal in the ashes: " + coal, coal >= 1 && coal <= 2 );
		//the workbench makes torches from it
		boolean found = false;
		for (xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipe r : xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipes.all()){
			if (r.id.equals( "torches_charcoal" )) found = true;
		}
		assertTrue( "a charcoal torch recipe", found );
	}

	@Test
	public void theShovelTillsAndDigsOutStumps(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int grass = find( ow, c -> ow.map[c] == Terrain.GRASS && ground( ow, c ) );
		stand( ow, grass );
		assertEquals( Messages.get( Gathering.class, "no_shovel" ), Gathering.refusal( hero, grass, Gathering.Job.DIG ) );
		give( new Shovel() );
		assertEquals( Gathering.Dig.TILL, Gathering.dig( ow, grass ) );
		assertEquals( "two spadefuls, on the cell he stands on", 2, work( grass, Gathering.Job.DIG ) );
		assertEquals( Terrain.FARMLAND, ow.map[grass] );
		assertNotNull( "farmland stays tilled", Gathering.refusal( hero, grass, Gathering.Job.DIG ) );

		//grass turned over brings up a seed one time in ten
		int seeds = 0, trials = 600;
		for (int i = 0; i < trials; i++){
			Level.set( grass, Terrain.GRASS, ow );
			clear( ow, grass );
			Gathering.dug( ow, grass );
			if (ow.heaps.get( grass ) != null) seeds++;
		}
		assertTrue( "a seed about one time in ten: " + seeds, Math.abs( seeds / (float) trials - Gathering.SEED_CHANCE ) < 0.04f );

		//frozen ground takes no spade
		Level.set( grass, Terrain.GRASS, ow );
		boolean[] frozen = (boolean[]) get( ow, "frozen" );
		if (frozen == null) set( ow, "frozen", frozen = new boolean[ow.length()] );
		frozen[grass] = true;
		assertEquals( Messages.get( Gathering.class, "frozen" ), Gathering.refusal( hero, grass, Gathering.Job.DIG ) );
		frozen[grass] = false;

		//a stump: three spadefuls, a last log, the ground back and the regrowth forgotten
		int oak = tree( ow, Terrain.TREE_OAK );
		stand( ow, standBeside( ow, oak ) );
		give( new WoodcuttersAxe() );
		work( oak, Gathering.Job.CHOP );
		int logs = count( ow.heaps.get( oak ), Log.class );
		assertEquals( Gathering.Dig.STUMP, Gathering.dig( ow, oak ) );
		assertEquals( 3, work( oak, Gathering.Job.DIG ) );
		assertEquals( Terrain.GRASS, ow.map[oak] );
		assertEquals( logs + 1, count( ow.heaps.get( oak ), Log.class ) );
		assertNull( ow.homestead().section( GatheringSlice.class ).felledOn( ow.worldX + oak % W, ow.worldY + oak / W ) );
	}

	@Test
	public void aBankGivesClayAndABeachSand(){
		give( new Shovel() );
		OverworldLevel river = windowIn( WorldModel.Biome.RIVER );
		int bank = find( river, c -> Gathering.dig( river, c ) == Gathering.Dig.CLAY && ground( river, c ) && !river.frozenAt( c ) );
		assertNotEquals( "a river bank", -1, bank );
		stand( river, bank );
		int before = river.map[bank];
		assertEquals( 2, work( bank, Gathering.Job.DIG ) );
		assertEquals( "the bank stays", before, river.map[bank] );
		assertEquals( 1, count( river.heaps.get( bank ), Clay.class ) );

		OverworldLevel beach = windowIn( WorldModel.Biome.BEACH );
		int sand = find( beach, c -> Gathering.dig( beach, c ) == Gathering.Dig.SAND && ground( beach, c ) );
		assertNotEquals( "beach sand", -1, sand );
		stand( beach, sand );
		assertEquals( 2, work( sand, Gathering.Job.DIG ) );
		assertEquals( Terrain.EMPTY_SP, beach.map[sand] );
		assertEquals( 1, count( beach.heaps.get( sand ), Sand.class ) );
	}

	@Test
	public void theSickleReapsTallGrassForThatch(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int tall = find( ow, c -> ow.map[c] == Terrain.HIGH_GRASS && standBeside( ow, c ) != -1 && !ow.protectedForBuilding( c ) );
		stand( ow, standBeside( ow, tall ) );
		assertEquals( Messages.get( Gathering.class, "no_sickle" ), Gathering.refusal( hero, tall, Gathering.Job.REAP ) );
		give( new Sickle() );
		assertEquals( 1, work( tall, Gathering.Job.REAP ) );
		assertEquals( Terrain.GRASS, ow.map[tall] );
		int thatch = count( ow.heaps.get( tall ), Thatch.class );
		assertTrue( "1-2 thatch: " + thatch, thatch >= 1 && thatch <= 2 );
		assertEquals( Messages.get( Gathering.class, "nothing_to_reap" ), Gathering.refusal( hero, tall, Gathering.Job.REAP ) );
	}

	// ------------------------------------------------------------------ what is not his to take

	@Test
	public void protectedCellsAndVillageTreesAreRefused(){
		give( new WoodcuttersAxe() );
		give( new MasonsHammer() );
		give( new Shovel() );
		give( new Sickle() );
		OverworldLevel town = windowAt( WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ),
				WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) );
		int c = find( town, x -> town.inTown( x ) && town.passable[x] );
		stand( town, c + 1 );
		Level.set( c, Terrain.TREE_OAK, town );
		assertEquals( Messages.get( Gathering.class, "protected_chop" ), Gathering.refusal( hero, c, Gathering.Job.CHOP ) );
		assertFalse( Gathering.stroke( hero, c, Gathering.Job.CHOP, d -> {} ) );
		assertEquals( Terrain.TREE_OAK, town.map[c] );
		Level.set( c, Terrain.BOULDER, town );
		assertEquals( Messages.get( Gathering.class, "protected_quarry" ), Gathering.refusal( hero, c, Gathering.Job.QUARRY ) );
		Level.set( c, Terrain.GRASS, town );
		assertEquals( Messages.get( Gathering.class, "protected_dig" ), Gathering.refusal( hero, c, Gathering.Job.DIG ) );
		Level.set( c, Terrain.HIGH_GRASS, town );
		assertEquals( Messages.get( Gathering.class, "protected_reap" ), Gathering.refusal( hero, c, Gathering.Job.REAP ) );

		//a tree within a village's bounds, on no cell of its own
		long[] village = humanVillage();
		VillageRoutine.Settlement s = VillageRoutine.settlement( SEED, village[0] );
		OverworldLevel vw = windowAt( s.cx, s.cy );
		int t = find( vw, x -> !vw.protectedForBuilding( x ) && vw.passable[x]
				&& OverworldFauna.nearSettlement( SEED, vw.worldX + x % W, vw.worldY + x / W ) );
		assertNotEquals( -1, t );
		Level.set( t, Terrain.TREE_PINE, vw );
		assertEquals( Messages.get( Gathering.class, "villagers" ), Gathering.refusal( hero, t, Gathering.Job.CHOP ) );
	}

	// ------------------------------------------------------------------ growing back

	@Test
	public void stumpsGrowOnlyInSpringAndSummer(){
		//calendarStartDay 0: day 0 is the first of spring, the year's seasons 91 days long
		//(winter 92 in a leap year)
		assertEquals( GameCalendar.Season.SPRING, GameCalendar.seasonOfDay( 0 ) );
		assertEquals( GameCalendar.Season.SUMMER, GameCalendar.seasonOfDay( 91 ) );
		assertEquals( GameCalendar.Season.AUTUMN, GameCalendar.seasonOfDay( 182 ) );
		assertEquals( GameCalendar.Season.WINTER, GameCalendar.seasonOfDay( 273 ) );

		int summer = seasonRun( GameCalendar.Season.SUMMER, 20 );
		assertEquals( 0, GatheringSlice.stage( summer, summer + 3 ) );
		assertEquals( "a sapling after four growing days", 1, GatheringSlice.stage( summer, summer + 4 ) );
		assertEquals( 1, GatheringSlice.stage( summer, summer + 13 ) );
		assertEquals( "the tree after ten more", 2, GatheringSlice.stage( summer, summer + 14 ) );

		int autumn = seasonRun( GameCalendar.Season.AUTUMN, 90 );
		assertEquals( "an autumn grows nothing", 0, GatheringSlice.growingDays( autumn, autumn + 90, 100 ) );
		int winter = seasonRun( GameCalendar.Season.WINTER, 30 );
		assertEquals( 0, GatheringSlice.stage( winter, winter + 30 ) );
		//felled in the last days of a winter, only the spring's days count
		int spring = seasonRun( GameCalendar.Season.SPRING, 20, 300 );
		assertTrue( spring > 4 );
		assertEquals( GameCalendar.Season.WINTER, GameCalendar.seasonOfDay( spring ) );
		assertEquals( 3, GatheringSlice.growingDays( spring - 5, spring + 3, 100 ) );
		assertEquals( 1, GatheringSlice.stage( spring - 5, spring + 4 ) );
	}

	@Test
	public void aStumpGrowsBackOnTheWindowAndThroughASave() throws Exception {
		int summer = seasonRun( GameCalendar.Season.SUMMER, 30 );
		toDay( summer );
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		int wx = ow.worldX + oak % W, wy = ow.worldY + oak / W;
		stand( ow, standBeside( ow, oak ) );
		give( new WoodcuttersAxe() );
		work( oak, Gathering.Job.CHOP );
		clear( ow, oak );
		GatheringSlice slice = ow.homestead().section( GatheringSlice.class );
		slice.worldTurn( ow );
		assertEquals( Terrain.STUMP_OAK, ow.map[oak] );

		toDay( summer + 4 );
		slice.worldTurn( ow );
		assertEquals( "a sapling four summer days on", Terrain.SAPLING_OAK, ow.map[oak] );

		//a save between: the sapling and its felling day both kept
		OverworldLevel loaded = saveAndLoad( ow );
		int cell = loaded.localCell( wx, wy );
		assertEquals( Terrain.SAPLING_OAK, loaded.map[cell] );
		GatheringSlice back = loaded.homestead().section( GatheringSlice.class );
		assertEquals( (Integer) summer, back.felledOn( wx, wy ) );

		//someone standing on it holds the tree back
		toDay( summer + 14 );
		hero.pos = cell;
		Actor.add( hero );
		back.worldTurn( loaded );
		assertEquals( Terrain.SAPLING_OAK, loaded.map[cell] );
		Actor.remove( hero );
		toDay( summer + 15 );
		back.worldTurn( loaded );
		assertEquals( "the oak again", Terrain.TREE_OAK, loaded.map[cell] );
		assertEquals( loaded.pristineAt( cell ), loaded.map[cell] );
		assertNull( "no longer followed", back.felledOn( wx, wy ) );
	}

	@Test
	public void aStumpBuiltOverIsForgotten(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		stand( ow, standBeside( ow, oak ) );
		give( new WoodcuttersAxe() );
		work( oak, Gathering.Job.CHOP );
		clear( ow, oak );
		xyz.gabriwar.warpedpixeldungeon.homestead.Homestead.build( ow, oak,
				xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog.byId( "floor_plank" ), hero );
		assertNull( ow.homestead().section( GatheringSlice.class ).felledOn( ow.worldX + oak % W, ow.worldY + oak / W ) );
	}

	// ------------------------------------------------------------------ hides

	@Test
	public void hidesDropAtTheirRates(){
		Mob[] beasts = { new Deer(), new Goat(), new Yeti(), new BrownWolf(), new GrayWolf(), new Rat() };
		float[] rates = { 1f, 0.5f, 0.5f, 1 / 3f, 1 / 3f, 0f };
		Random.pushGenerator( 777 );
		try {
			for (int b = 0; b < beasts.length; b++){
				int n = 0, trials = 6000;
				for (int i = 0; i < trials; i++) if (Hides.dropped( beasts[b] )) n++;
				assertEquals( beasts[b].getClass().getSimpleName(), rates[b], n / (float) trials, 0.025f );
			}
		} finally {
			Random.popGenerator();
		}
	}

	//a beast of a kind killed on a cell, `n` times: the hides it left there or beside it
	private int hidesLeft( OverworldLevel ow, Class<? extends Mob> kind, int cell, int n ){
		int hides = 0;
		for (int i = 0; i < n; i++){
			for (int c : PathFinder.NEIGHBOURS9) clear( ow, cell + c );
			Mob m = com.watabou.utils.Reflection.newInstance( kind );
			m.pos = cell;
			ow.mobs.add( m );
			Actor.add( m );
			m.die( null );
			Actor.remove( m );
			ow.mobs.remove( m );
			for (int c : PathFinder.NEIGHBOURS9) hides += count( ow.heaps.get( cell + c ), xyz.gabriwar.warpedpixeldungeon.items.materials.Hide.class );
		}
		return hides;
	}

	@Test
	public void aBeastKilledLeavesItsHideAndTheBestiaryListsIt(){
		OverworldLevel ow = windowIn( WorldModel.Biome.MEADOW );
		int open = find( ow, c -> {
			for (int n : PathFinder.NEIGHBOURS9) if (!ground( ow, c + n )) return false;
			return true;
		} );
		assertNotEquals( "open ground", -1, open );
		stand( ow, open + 3 );
		//well past the wolves' and the yeti's level: their usual loot is held back, the hide is not
		hero.lvl = 40;
		Random.pushGenerator( 31337 );
		try {
			assertEquals( "a deer always", 60, hidesLeft( ow, Deer.class, open, 60 ) );
			int goats = hidesLeft( ow, Goat.class, open, 300 );
			assertEquals( "a goat one time in two: " + goats, 0.5f, goats / 300f, 0.09f );
			//no sprite here to show the experience a kill pays: the hero is out of it (Mob.destroy),
			//the loot rolled all the same
			hero.HP = 0;
			int yetis = hidesLeft( ow, Yeti.class, open, 300 );
			assertEquals( "a yeti one time in two: " + yetis, 0.5f, yetis / 300f, 0.09f );
			int wolves = hidesLeft( ow, BrownWolf.class, open, 300 ) + hidesLeft( ow, GrayWolf.class, open, 300 );
			assertEquals( "a wolf one time in three: " + wolves, 1 / 3f, wolves / 600f, 0.07f );
		} finally {
			Random.popGenerator();
		}
		//each lists the hide among its drops, at its chance
		Class<?>[] kinds = { Deer.class, Goat.class, Yeti.class, BrownWolf.class, GrayWolf.class };
		for (Class<?> k : kinds){
			Mob m = (Mob) com.watabou.utils.Reflection.newInstance( k );
			boolean listed = false;
			for (Mob.DropInfo d : m.getDrops()){
				if (d.item instanceof xyz.gabriwar.warpedpixeldungeon.items.materials.Hide){
					listed = true;
					assertEquals( k.getSimpleName(), Hides.chance( m.getClass() ), d.chance, 0.0001f );
				}
			}
			assertTrue( k.getSimpleName() + " lists its hide", listed );
		}
	}

	// ------------------------------------------------------------------ the shelves

	@Test
	public void theTownStoreStocksTheBuildersWaresAtTheTownsPrices(){
		//a new builder: the tool and the satchel first, then the rack, four spots at most
		HashSet<Class<?>> stocked = new HashSet<>();
		ArrayList<Class<?>> order = new ArrayList<>();
		for (Item it; (it = Wares.townStock( stocked )) != null; ){
			assertTrue( it.getClass().getSimpleName() + " is worth something, so a tap buys it", it.value() > 0 );
			stocked.add( it.getClass() );
			order.add( it.getClass() );
		}
		assertEquals( Arrays.asList( BuildersTool.class, BuildersSatchel.class, WoodcuttersAxe.class, MasonsHammer.class ), order );
		assertEquals( Wares.TOWN_SPOTS, order.size() );

		//the tool and the satchel bought: the whole rack of tools is on the counter
		give( new BuildersTool() );
		new BuildersSatchel().collect( hero.belongings.backpack );
		stocked.clear();
		order.clear();
		for (Item it; (it = Wares.townStock( stocked )) != null; ){
			stocked.add( it.getClass() );
			order.add( it.getClass() );
		}
		assertEquals( Arrays.asList( WoodcuttersAxe.class, MasonsHammer.class, Shovel.class, Sickle.class ), order );

		//an axe bought: the rope comes up in its place
		give( new WoodcuttersAxe() );
		stocked.remove( WoodcuttersAxe.class );
		Item next = Wares.townStock( stocked );
		assertTrue( next instanceof Rope );
		assertEquals( Wares.ROPE_COILS, next.quantity() );

		//what he already carries is not offered again; rope always is
		give( new MasonsHammer() );
		give( new Shovel() );
		give( new Sickle() );
		Item only = Wares.townStock( new HashSet<>() );
		assertTrue( "rope, which he can always use", only instanceof Rope );
		HashSet<Class<?>> rope = new HashSet<>();
		rope.add( Rope.class );
		assertNull( Wares.townStock( rope ) );

		//the town's own prices, scaled by the deepest floor reached
		Statistics.deepestFloor = 12;
		Dungeon.level = new TownShopLevel();
		assertEquals( 150 * 3, Wares.shelfPrice( new BuildersTool() ) );
		assertEquals( 200 * 3, Wares.shelfPrice( new WoodcuttersAxe() ) );
		assertEquals( 200 * 3, Wares.shelfPrice( new MasonsHammer() ) );
		assertEquals( 120 * 3, Wares.shelfPrice( new BuildersSatchel() ) );
		assertEquals( 12 * 3 * 5, Wares.shelfPrice( new Rope().quantity( 5 ) ) );
		assertEquals( "a shovel keeps a weapon's price", -1, Wares.shelfPrice( new Shovel() ) );
		assertEquals( -1, Wares.shelfPrice( new Food() ) );
		//any building material, anywhere: twice its worth
		Dungeon.level = windowIn( WorldModel.Biome.FOREST );
		assertEquals( 2 * 2 * 10, Wares.shelfPrice( new Plank().quantity( 10 ) ) );
		assertEquals( "rope outside the town is a material", 2 * 4 * 5, Wares.shelfPrice( new Rope().quantity( 5 ) ) );
		assertEquals( -1, Wares.shelfPrice( new BuildersTool() ) );
	}

	//a human settlement big enough for a trader near the origin: its sector key
	private static long[] humanVillage(){
		for (int r = 0; r <= 20; r++){
			for (int sy = -r; sy <= r; sy++){
				for (int sx = -r; sx <= r; sx++){
					if (Math.max( Math.abs( sx ), Math.abs( sy ) ) != r) continue;
					long key = WorldStructures.sectorOf( sx, sy );
					VillageRoutine.Settlement s = VillageRoutine.settlement( SEED, key );
					if (s != null && !s.gnoll && VillageRoutine.vendorHouses( s.houses() ) >= 1
							&& WorldStructures.townCell( s.cx, s.cy ) == -1) return new long[]{ key };
				}
			}
		}
		throw new AssertionError( "no village near the origin" );
	}

	@Test
	public void everyVillageGetsOneCarpenter(){
		long key = humanVillage()[0];
		VillageRoutine.Settlement s = VillageRoutine.settlement( SEED, key );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		Carpentry.settle( ow, key );
		Carpentry.settle( ow, key );
		int carpenters = 0;
		OverworldShopkeeper keeper = null;
		for (Mob m : ow.mobs){
			if (m instanceof OverworldShopkeeper && ((OverworldShopkeeper) m).speciality == OverworldShopkeeper.CARPENTER){
				carpenters++;
				keeper = (OverworldShopkeeper) m;
			}
		}
		assertEquals( "one, however often the window lays the village", 1, carpenters );
		assertEquals( key, keeper.homeSector );
		assertTrue( "by the well", Math.max( Math.abs( ow.worldX + keeper.pos % W - s.cx ), Math.abs( ow.worldY + keeper.pos / W - s.cy ) ) <= Carpentry.STALL_REACH + 3 );
		assertEquals( "carpenter", keeper.name() );
		assertTrue( HomesteadWorld.section( GatheringWorld.class ).hasCarpenter( key ) );

		//once set up, never again: driven off, he is gone for good
		ow.mobs.remove( keeper );
		Carpentry.settle( ow, key );
		for (Mob m : ow.mobs) assertFalse( m instanceof OverworldShopkeeper && ((OverworldShopkeeper) m).speciality == OverworldShopkeeper.CARPENTER );

		//a village peopled for the first time keeps its families and traders, the carpenter with them
		HomesteadWorld.reset();
		OverworldLevel fresh = windowAt( s.cx, s.cy );
		try {
			Method populate = OverworldLevel.class.getDeclaredMethod( "populateSettlements" );
			populate.setAccessible( true );
			populate.invoke( fresh );
		} catch (Exception e){
			throw new AssertionError( e );
		}
		int folk = 0, traders = 0, joiners = 0;
		for (Mob m : fresh.mobs){
			if (m instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager
					&& ((xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager) m).homeSector == key) folk++;
			if (m instanceof OverworldShopkeeper && ((OverworldShopkeeper) m).homeSector == key){
				if (((OverworldShopkeeper) m).speciality == OverworldShopkeeper.CARPENTER) joiners++;
				else traders++;
			}
		}
		assertTrue( "its families", folk > 0 );
		assertEquals( "its usual traders", VillageRoutine.vendorHouses( s.houses() ), traders );
		assertEquals( "and its carpenter", 1, joiners );

		HashSet<Class<?>> goods = new HashSet<>();
		for (Item i : Carpentry.stock()) goods.add( i.getClass() );
		assertEquals( new HashSet<>( Arrays.asList( Plank.class, Rope.class, Thatch.class, IronFittings.class, GlassPane.class ) ), goods );
	}

	//the town store as the game makes it, for a hero who carries nothing yet
	private TownShopLevel townShop(){
		TownShopLevel shop = new TownShopLevel();
		Dungeon.level = shop;
		shop.create();
		PathFinder.setMapSize( shop.width(), shop.height() );
		return shop;
	}

	private static void storeStock( TownShopLevel shop ){
		try {
			Method m = TownShopLevel.class.getDeclaredMethod( "storeStock" );
			m.setAccessible( true );
			m.invoke( shop );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	//the wares for sale on a level, by kind: the cell of each
	private static java.util.HashMap<Class<?>, Integer> forSale( Level level ){
		java.util.HashMap<Class<?>, Integer> out = new java.util.HashMap<>();
		for (Heap h : level.heaps.valueList()){
			if (h.type == Heap.Type.FOR_SALE && !h.isEmpty()) out.put( h.peek().getClass(), h.pos );
		}
		return out;
	}

	@Test
	public void theTownStoresBuildersWaresAreBoughtByATap(){
		TownShopLevel shop = townShop();
		java.util.HashMap<Class<?>, Integer> wares = forSale( shop );
		for (Class<?> c : new Class<?>[]{ BuildersTool.class, BuildersSatchel.class, WoodcuttersAxe.class, MasonsHammer.class }){
			assertTrue( c.getSimpleName() + " on the counter", wares.containsKey( c ) );
		}
		int others = 0;
		for (Class<?> c : wares.keySet()){
			if (c != BuildersTool.class && c != BuildersSatchel.class && c != WoodcuttersAxe.class && c != MasonsHammer.class
					&& c != xyz.gabriwar.warpedpixeldungeon.items.Fertilizer.class && c != xyz.gabriwar.warpedpixeldungeon.items.BulletBelt.class
					&& c != xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade.class) others++;
		}
		assertTrue( "the store keeps room for its other goods", others >= 3 );

		//a tap on each is a purchase, never a free pick-up
		hero.pos = shop.entrance();
		hero.fieldOfView = new boolean[shop.length()];
		Arrays.fill( hero.fieldOfView, true );
		for (Class<?> c : new Class<?>[]{ BuildersTool.class, BuildersSatchel.class, WoodcuttersAxe.class, MasonsHammer.class }){
			hero.curAction = null;
			assertTrue( hero.handle( wares.get( c ) ) );
			assertTrue( c.getSimpleName() + ": " + hero.curAction, hero.curAction instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Buy );
		}

		//the tool and the satchel bought: the shovel and the sickle come up in their spots
		for (Class<?> c : new Class<?>[]{ BuildersTool.class, BuildersSatchel.class }){
			shop.heaps.get( wares.get( c ) ).destroy();
		}
		give( new BuildersTool() );
		new BuildersSatchel().collect( hero.belongings.backpack );
		storeStock( shop );
		wares = forSale( shop );
		for (Class<?> c : new Class<?>[]{ WoodcuttersAxe.class, MasonsHammer.class, Shovel.class, Sickle.class }){
			assertTrue( c.getSimpleName() + " on the counter", wares.containsKey( c ) );
			hero.curAction = null;
			hero.handle( wares.get( c ) );
			assertTrue( c.getSimpleName(), hero.curAction instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Buy );
		}
		assertFalse( wares.containsKey( BuildersTool.class ) );
	}

	@Test
	public void theCarpenterLaysOutAllHisGoodsEveryRestock(){
		long key = humanVillage()[0];
		VillageRoutine.Settlement s = VillageRoutine.settlement( SEED, key );
		OverworldLevel ow = windowAt( s.cx, s.cy );
		Carpentry.settle( ow, key );
		OverworldShopkeeper keeper = null;
		for (Mob m : ow.mobs){
			if (m instanceof OverworldShopkeeper && ((OverworldShopkeeper) m).speciality == OverworldShopkeeper.CARPENTER) keeper = (OverworldShopkeeper) m;
		}
		assertNotNull( keeper );
		java.util.List<Class<?>> five = Arrays.asList( Plank.class, Rope.class, Thatch.class, IronFittings.class, GlassPane.class );
		for (int round = 0; round < 3; round++){
			keeper.restock();
			HashSet<Class<?>> shelf = new HashSet<>();
			int heaps = 0;
			for (Heap h : ow.heaps.valueList()){
				if (h.type != Heap.Type.FOR_SALE || ow.distance( h.pos, keeper.pos ) > 2) continue;
				heaps++;
				shelf.add( h.peek().getClass() );
			}
			assertEquals( "round " + round, new HashSet<>( five ), shelf );
			assertEquals( "one of each, never doubled", 5, heaps );
			//the planks sell out: the next restock brings them back
			for (Heap h : ow.heaps.valueList().toArray( new Heap[0] )){
				if (h.type == Heap.Type.FOR_SALE && h.peek() instanceof Plank && ow.distance( h.pos, keeper.pos ) <= 2) h.destroy();
			}
		}
	}

	@Test
	public void aTapGoesThroughTheHerosOwnHandling(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		int at = standBeside( ow, oak );
		stand( ow, at );
		give( new HandAxe() );
		assertTrue( hero.handle( oak ) );
		assertTrue( String.valueOf( hero.curAction ), hero.curAction instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Work );
		assertEquals( Gathering.Job.CHOP, ((xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Work) hero.curAction).job );

		//the world's rock with only a mason's hammer: the pick's blow, three turns long
		OverworldLevel mt = windowIn( WorldModel.Biome.MOUNTAIN );
		int rock = find( mt, c -> mt.map[c] == Terrain.WALL && !mt.protectedForBuilding( c ) && !mt.unbreakable( c )
				&& standBeside( mt, c ) != -1 );
		assertNotEquals( "rock on the mountain", -1, rock );
		Hero h2 = new Hero();
		Dungeon.hero = hero = h2;
		stand( mt, standBeside( mt, rock ) );
		give( new MasonsHammer() );
		hero.handle( rock );
		assertTrue( String.valueOf( hero.curAction ), hero.curAction instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Mine );
		assertEquals( Gathering.HAMMER_WALL_TURNS, ((xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Mine) hero.curAction).turns, 0f );
		give( new Pickaxe() );
		hero.curAction = null;
		hero.handle( rock );
		assertEquals( "the pick's one blow", 1f, ((xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Mine) hero.curAction).turns, 0f );
	}

	@Test
	public void aStumpDugInWinterLeavesGrassNotSnow(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		stand( ow, standBeside( ow, oak ) );
		give( new WoodcuttersAxe() );
		give( new Shovel() );
		work( oak, Gathering.Job.CHOP );
		boolean[] frozen = (boolean[]) get( ow, "frozen" );
		if (frozen == null) set( ow, "frozen", frozen = new boolean[ow.length()] );
		frozen[oak] = true;
		work( oak, Gathering.Job.DIG );
		assertEquals( "grass, kept as the edit whatever the season", Terrain.GRASS, ow.map[oak] );
		assertEquals( "shown as snow while the ground is frozen", Terrain.SNOW, WindowGenerator.visualGround( ow.map, frozen, oak ) );
		frozen[oak] = false;
		assertEquals( "and as grass once it thaws", Terrain.GRASS, WindowGenerator.visualGround( ow.map, frozen, oak ) );
	}

	@Test
	public void theWorksLookLeavesTheGamesRandomAlone(){
		Random.pushGenerator( 99 );
		long expected;
		try {
			expected = Random.Long();
		} finally {
			Random.popGenerator();
		}
		Random.pushGenerator( 99 );
		try {
			for (int kind = GatheringFx.CHOP; kind <= GatheringFx.WALL_SPARK; kind++){
				GatheringFx.play( kind, 10, GatheringFx.direction( 1, 0, true ), GatheringFx.picture( 58, 57 ) );
			}
			//a typed tool's strokes, their piece more
			for (int kind : new int[]{ GatheringFx.CHOP, GatheringFx.QUARRY, GatheringFx.DIG }){
				GatheringFx.play( kind, 10, GatheringFx.TYPED, 0 );
			}
			assertEquals( "no scene: nothing drawn, nothing drawn from the game's Random", expected, Random.Long() );
		} finally {
			Random.popGenerator();
		}
		//a fall's numbers come back as they went
		int n = GatheringFx.direction( -1, 1, true ), d = n % 9;
		assertEquals( -1, d % 3 - 1 );
		assertEquals( 1, d / 3 - 1 );
		assertTrue( n >= 9 );
		int m = GatheringFx.picture( 54, -1 );
		assertEquals( 54, m & 0xFFF );
		assertEquals( -1, (m >> 12) - 1 );
	}

	@Test
	public void everyStringResolves(){
		for (String k : new String[]{ "guest", "protected_chop", "protected_quarry", "protected_dig", "protected_reap",
				"villagers", "no_axe", "no_pick", "no_shovel", "no_sickle", "nothing_to_dig", "nothing_to_reap", "frozen" }){
			assertFalse( k, Messages.get( Gathering.class, k ).contains( "NO TEXT" ) );
		}
		for (Item i : new Item[]{ new WoodcuttersAxe(), new MasonsHammer() }){
			assertFalse( i.name().contains( "NO TEXT" ) );
			assertFalse( i.desc().contains( "NO TEXT" ) );
		}
		assertFalse( Messages.get( Shovel.class, "dig_prompt" ).contains( "NO TEXT" ) );
		assertFalse( Messages.get( Sickle.class, "reap_prompt" ).contains( "NO TEXT" ) );
		assertFalse( Messages.get( Sickle.class, "ac_reap" ).contains( "NO TEXT" ) );
		assertFalse( OverworldShopkeeper.of( OverworldShopkeeper.CARPENTER, 0 ).name().contains( "NO TEXT" ) );
	}

	// ------------------------------------------------------------------ typed tools

	@Test
	public void woodsmanFellsInFewerStrokes(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		stand( ow, standBeside( ow, oak ) );
		//untyped, gamma, beta, alpha: 20% fewer strokes a type rank, never under one
		Class<?>[] axes = { HandAxe.class, Axe.class, BattleAxe.class, Greataxe.class, CromCruachAxe.class, WoodcuttersAxe.class };
		int[][] trees = { { 8, 6, 5, 3 }, { 6, 5, 4, 2 }, { 5, 4, 3, 2 }, { 4, 3, 2, 2 }, { 4, 3, 2, 2 }, { 3, 2, 2, 1 } };
		ItemType[] types = { null, ItemType.GAMMA, ItemType.BETA, ItemType.ALPHA };
		for (int i = 0; i < axes.length; i++){
			for (int j = 0; j < types.length; j++){
				Item a = (Item) Reflection.newInstance( axes[i] );
				if (types[j] != null) typed( a, types[j] );
				hero.belongings.weapon = (MeleeWeapon) a;
				String what = axes[i].getSimpleName() + " " + types[j];
				assertEquals( what, trees[i][j], Gathering.turns( hero, oak, Gathering.Job.CHOP ) );
				//a bush or a stump: half a tree's, then the same share fewer
				assertEquals( what, Quality.workStrokes( a, Gathering.lightChop( trees[i][0] ) ),
						lightChopTurns( ow, oak ) );
			}
		}
		assertEquals( "an alpha hand axe's bush: 4 strokes, 2", 2, Quality.workStrokes( typed( new HandAxe(), ItemType.ALPHA ), 4 ) );

		//the strokes are counted as they are struck: an alpha hand axe fells in three
		hero.belongings.weapon = typed( new HandAxe(), ItemType.ALPHA );
		assertEquals( 3, work( oak, Gathering.Job.CHOP ) );
		assertEquals( Terrain.STUMP_OAK, ow.map[oak] );

		//a typed weapon of another kind changes nothing
		assertEquals( 7, Quality.workStrokes( typed( new Sickle(), ItemType.ALPHA ), 7 ) );
		assertEquals( 7, Quality.workStrokes( null, 7 ) );
	}

	//the strokes a bush beside the hero takes (and gone again: the oak's cell is put back as it was)
	private int lightChopTurns( OverworldLevel ow, int oak ){
		Level.set( oak, Terrain.SHRUB, ow );
		int t = Gathering.turns( hero, oak, Gathering.Job.CHOP );
		Level.set( oak, Terrain.TREE_OAK, ow );
		return t;
	}

	@Test
	public void cromCruachAxeFells(){
		assertEquals( Gathering.GREATAXE_TURNS, Gathering.axeTurns( new CromCruachAxe() ) );
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		stand( ow, standBeside( ow, oak ) );
		CromCruachAxe relic = new CromCruachAxe();
		hero.belongings.weapon = relic;
		assertEquals( relic, Gathering.axe( hero ) );
		assertEquals( Gathering.Job.CHOP, Gathering.tapJob( hero, oak ) );
		assertEquals( Gathering.GREATAXE_TURNS, work( oak, Gathering.Job.CHOP ) );
		assertEquals( Terrain.STUMP_OAK, ow.map[oak] );
	}

	@Test
	public void theQuickestAxeCarriedCountsItsType(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		stand( ow, standBeside( ow, oak ) );
		hero.belongings.weapon = new Sickle();
		//a gamma battle axe's 5 strokes come down to 4, an alpha axe's 6 to 2
		BattleAxe battle = typed( new BattleAxe(), ItemType.GAMMA );
		give( battle );
		Axe alpha = typed( new Axe(), ItemType.ALPHA );
		give( alpha );
		assertEquals( "the alpha axe in the pack, quicker for its type", alpha, Gathering.axe( hero ) );
		assertEquals( 2, Gathering.turns( hero, oak, Gathering.Job.CHOP ) );
		//the axe in his hand still comes first, however slow
		HandAxe held = new HandAxe();
		hero.belongings.weapon = held;
		assertEquals( held, Gathering.axe( hero ) );
		assertEquals( Gathering.HAND_AXE_TURNS, Gathering.turns( hero, oak, Gathering.Job.CHOP ) );
	}

	@Test
	public void aTypedToolsStrokeGoesToTheGuestsTyped() throws Exception {
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		stand( ow, standBeside( ow, oak ) );
		int at = find( ow, c -> ground( ow, c ) && ground( ow, c + 1 ) && c != oak && c + 1 != oak );
		int rock = at + 1;
		Level.set( rock, Terrain.BOULDER, ow );
		TestParty.join( new Hero() );
		try {
			NetVisuals.clear();
			hero.belongings.weapon = new HandAxe();
			assertTrue( Gathering.stroke( hero, oak, Gathering.Job.CHOP, d -> {} ) );
			assertEquals( "a bare axe's stroke", 0, sentN( GatheringFx.CHOP ) );
			hero.belongings.weapon = typed( new HandAxe(), ItemType.GAMMA );
			assertTrue( Gathering.stroke( hero, oak, Gathering.Job.CHOP, d -> {} ) );
			assertEquals( "a typed axe's stroke", GatheringFx.TYPED, sentN( GatheringFx.CHOP ) );

			stand( ow, at );
			hero.belongings.weapon = new Pickaxe();
			assertTrue( Gathering.stroke( hero, rock, Gathering.Job.QUARRY, d -> {} ) );
			assertEquals( "a bare pick's stroke", 0, sentN( GatheringFx.QUARRY ) );
			hero.belongings.weapon = typed( new Pickaxe(), ItemType.GAMMA );
			assertTrue( Gathering.stroke( hero, rock, Gathering.Job.QUARRY, d -> {} ) );
			assertEquals( "a typed pick's stroke", GatheringFx.TYPED, sentN( GatheringFx.QUARRY ) );
		} finally {
			TestParty.leave();
			NetVisuals.clear();
		}
	}

	//the n the host sent with the one gathering event of a kind drained now (0 when none was put)
	private static int sentN( int kind ) throws Exception {
		JSONArray sent = NetVisuals.drainEvents();
		assertNotNull( "something sent", sent );
		int found = 0, n = 0;
		for (int i = 0; i < sent.length(); i++){
			JSONObject e = sent.getJSONObject( i );
			if (!"gw".equals( e.getString( "t" ) ) || e.getInt( "k" ) != kind) continue;
			found++;
			n = e.optInt( "n", 0 );
		}
		assertEquals( "one stroke sent", 1, found );
		return n;
	}

	@Test
	public void theQuickestQuarryToolIsUsedHeldFirstOnATie(){
		assertNull( Gathering.quarryTool( hero ) );
		WarHammer war = new WarHammer();
		hero.belongings.weapon = war;
		Pickaxe pick = new Pickaxe();
		give( pick );
		assertEquals( "three strokes each: the one in his hand", war, Gathering.quarryTool( hero ) );
		MasonsHammer mason = new MasonsHammer();
		give( mason );
		assertEquals( "the mason's hammer's two beat the held three: never slower than before", mason, Gathering.quarryTool( hero ) );
		hero.belongings.weapon = typed( new WarHammer(), ItemType.ALPHA );
		assertEquals( "an alpha war hammer's one", hero.belongings.weapon, Gathering.quarryTool( hero ) );
		hero.belongings.weapon = typed( new Pickaxe(), ItemType.BETA );
		assertEquals( 2, Quality.workStrokes( hero.belongings.weapon, Gathering.PICK_QUARRY_TURNS ) );
		assertEquals( "a beta pick's two tie the mason's hammer's: the held one", hero.belongings.weapon, Gathering.quarryTool( hero ) );
		hero.belongings.weapon = new Sickle();
		assertEquals( "a held weapon that breaks no stone is passed over", mason, Gathering.quarryTool( hero ) );
		//a typed tool in the pack counts as much as one in hand
		MasonsHammer alpha = typed( new MasonsHammer(), ItemType.ALPHA );
		give( alpha );
		assertEquals( alpha, Gathering.quarryTool( hero ) );
	}

	@Test
	public void aHeldWarHammerBreaksABoulder(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int at = find( ow, c -> ground( ow, c ) && ground( ow, c + 1 ) );
		stand( ow, at );
		int rock = at + 1;
		Level.set( rock, Terrain.BOULDER, ow );
		hero.belongings.weapon = new Spade();
		assertNull( "a spade breaks no boulder", Gathering.tapJob( hero, rock ) );
		assertEquals( Messages.get( Gathering.class, "no_pick" ), Gathering.refusal( hero, rock, Gathering.Job.QUARRY ) );
		for (MeleeWeapon tool : new MeleeWeapon[]{ new WarHammer(), new MeisterHammer(), new MinersTool() }){
			hero.belongings.weapon = tool;
			clear( ow, rock );
			Level.set( rock, Terrain.BOULDER, ow );
			String what = tool.getClass().getSimpleName();
			assertEquals( what, tool, Gathering.quarryTool( hero ) );
			assertEquals( what, Gathering.Job.QUARRY, Gathering.tapJob( hero, rock ) );
			assertNull( what, Gathering.refusal( hero, rock, Gathering.Job.QUARRY ) );
			assertEquals( what, Gathering.PICK_QUARRY_TURNS, work( rock, Gathering.Job.QUARRY ) );
			assertEquals( what, Terrain.EMPTY_DECO, ow.map[rock] );
			int stone = count( ow.heaps.get( rock ), RoughStone.class );
			assertTrue( what + ": 2-3 rough stone: " + stone, stone >= 2 && stone <= 3 );
		}
	}

	@Test
	public void digAndStumpTakeFewerStrokesWithAQuarryShovel(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int grass = find( ow, c -> ow.map[c] == Terrain.GRASS && ground( ow, c ) );
		stand( ow, grass );
		int[] dig = { 2, 2, 1, 1 }, stump = { 3, 2, 2, 1 };
		ItemType[] types = { null, ItemType.GAMMA, ItemType.BETA, ItemType.ALPHA };
		for (int j = 0; j < types.length; j++){
			Shovel s = new Shovel();
			if (types[j] != null) typed( s, types[j] );
			hero.belongings.weapon = s;
			assertEquals( String.valueOf( types[j] ), dig[j], Gathering.turns( hero, grass, Gathering.Job.DIG ) );
			Level.set( grass, Terrain.STUMP_OAK, ow );
			assertEquals( String.valueOf( types[j] ), stump[j], Gathering.turns( hero, grass, Gathering.Job.DIG ) );
			Level.set( grass, Terrain.GRASS, ow );
		}
		//the shovel in his hand first, else the one in the pack
		hero.belongings.weapon = new Shovel();
		give( typed( new Shovel(), ItemType.ALPHA ) );
		assertEquals( "the bare one in his hand", 2, Gathering.turns( hero, grass, Gathering.Job.DIG ) );
		hero.belongings.weapon = new Sickle();
		assertEquals( "the alpha one in the pack", 1, Gathering.turns( hero, grass, Gathering.Job.DIG ) );
		assertEquals( "one spadeful", 1, work( grass, Gathering.Job.DIG ) );
		assertEquals( Terrain.FARMLAND, ow.map[grass] );
	}

	@Test
	public void aTypedMasonsHammerBreaksTheWorldsRockInFewerStrokes(){
		OverworldLevel mt = windowIn( WorldModel.Biome.MOUNTAIN );
		int rock = find( mt, c -> mt.map[c] == Terrain.WALL && !mt.protectedForBuilding( c ) && !mt.unbreakable( c )
				&& standBeside( mt, c ) != -1 );
		assertNotEquals( "rock on the mountain", -1, rock );
		stand( mt, standBeside( mt, rock ) );
		give( typed( new MasonsHammer(), ItemType.ALPHA ) );
		hero.handle( rock );
		assertTrue( String.valueOf( hero.curAction ), hero.curAction instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Mine );
		assertEquals( "three turns, 60% fewer", 1f, ((xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroAction.Mine) hero.curAction).turns, 0f );
		assertEquals( "the hammer when there is no pick", Quality.of( hero.belongings.getItem( MasonsHammer.class ) ),
				Quality.of( Gathering.wallTool( hero ) ) );
		Pickaxe pick = new Pickaxe();
		give( pick );
		assertEquals( "the pick first, as the hero's handling picks it", pick, Gathering.wallTool( hero ) );
	}

	@Test
	public void extraLogStoneAndOreDrawOnlyFromATypedTool(){
		OverworldLevel ow = windowIn( WorldModel.Biome.FOREST );
		int oak = tree( ow, Terrain.TREE_OAK );
		stand( ow, standBeside( ow, oak ) );

		//an untyped axe draws from the game's Random exactly what felling drew before typed tools
		//(as with no axe at all: nothing asked); a typed one draws once more
		long none = afterFelling( ow, oak, null );
		assertEquals( "a bare axe draws nothing more", none, afterFelling( ow, oak, new HandAxe() ) );
		assertNotEquals( "a typed axe rolls its extra log", none, afterFelling( ow, oak, typed( new HandAxe(), ItemType.GAMMA ) ) );

		//an alpha axe: one more log 45% of the time, never two more; a bare one never
		hero.belongings.weapon = typed( new HandAxe(), ItemType.ALPHA );
		int more = 0, trials = 400;
		for (int i = 0; i < trials; i++){
			Level.set( oak, Terrain.TREE_OAK, ow );
			clear( ow, oak );
			Gathering.fell( hero, ow, oak );
			int logs = count( ow.heaps.get( oak ), Log.class );
			assertTrue( "3-4 logs and maybe one more: " + logs, logs >= 3 && logs <= 5 );
			if (logs == 5) more++;
		}
		//a fifth log only when the 3-4 came up 4 (half the time) and the extra came too
		assertEquals( "a fifth log about 0.45 x 0.5 of the time: " + more, 0.225f, more / (float) trials, 0.07f );
		//a stump chopped out gives its one log, typed axe or not
		for (int i = 0; i < 50; i++){
			Level.set( oak, Terrain.STUMP_OAK, ow );
			clear( ow, oak );
			Gathering.uproot( ow, oak );
			assertEquals( 1, count( ow.heaps.get( oak ), Log.class ) );
		}

		//a boulder: a bare pick draws what it did, a typed one once more; an alpha pick's 2-3 stone sometimes 4
		int at = find( ow, c -> ground( ow, c ) && ground( ow, c + 1 ) );
		stand( ow, at );
		int rock = at + 1;
		long noTool = afterQuarrying( ow, rock, null );
		assertEquals( noTool, afterQuarrying( ow, rock, new Pickaxe() ) );
		assertNotEquals( noTool, afterQuarrying( ow, rock, typed( new Pickaxe(), ItemType.GAMMA ) ) );
		hero.belongings.weapon = typed( new Pickaxe(), ItemType.ALPHA );
		boolean four = false;
		for (int i = 0; i < 200; i++){
			Level.set( rock, Terrain.BOULDER, ow );
			clear( ow, rock );
			Gathering.quarry( hero, ow, rock );
			int stone = count( ow.heaps.get( rock ), RoughStone.class );
			assertTrue( "2-3 stone and maybe one more: " + stone, stone >= 2 && stone <= 4 );
			four |= stone == 4;
		}
		assertTrue( "an alpha pick brings out a fourth stone", four );

		//the world's rock: the pick or the hammer the hero breaks it with (wallTool)
		OverworldLevel mt = windowIn( WorldModel.Biome.MOUNTAIN );
		int wall = find( mt, c -> mt.map[c] == Terrain.WALL && !mt.protectedForBuilding( c ) && mt.heaps.get( c ) == null );
		assertNotEquals( "rock on the mountain", -1, wall );
		hero.belongings.weapon = null;
		long bareWall = afterWallStone( mt, wall );
		Pickaxe pick = new Pickaxe();
		give( pick );
		assertEquals( "a bare pick draws what it did", bareWall, afterWallStone( mt, wall ) );
		pick.quality = typed( new Pickaxe(), ItemType.ALPHA ).quality;
		assertNotEquals( "a typed pick draws once more", bareWall, afterWallStone( mt, wall ) );
		boolean three = false;
		for (int i = 0; i < 200; i++){
			clear( mt, wall );
			Gathering.wallStone( mt, wall, hero );
			int stone = count( mt.heaps.get( wall ), RoughStone.class );
			assertTrue( "1-2 stone and maybe one more: " + stone, stone >= 1 && stone <= 3 );
			three |= stone == 3;
		}
		assertTrue( "an alpha pick brings out a third stone", three );

		//a vein's ore: one more lump now and then, never from a bare pick
		pick.quality = null;
		for (int i = 0; i < 50; i++){
			Ore ore = (Ore) new IronOre().quantity( 2 );
			Gathering.extraOre( hero, ore, wall );
			assertEquals( "a bare pick's ore as it was", 2, ore.quantity() );
		}
		pick.quality = typed( new Pickaxe(), ItemType.ALPHA ).quality;
		int lumps = 0;
		for (int i = 0; i < trials; i++){
			Ore ore = (Ore) new IronOre().quantity( 2 );
			Gathering.extraOre( hero, ore, wall );
			assertTrue( ore.quantity() == 2 || ore.quantity() == 3 );
			lumps += ore.quantity() - 2;
		}
		assertEquals( "one more lump about 45% of the time: " + lumps, 0.45f, lumps / (float) trials, 0.08f );
	}

	//the next long the game's Random gives after a tree is felled with this axe in hand (a fixed seed)
	private long afterFelling( OverworldLevel ow, int oak, Item axe ){
		hero.belongings.weapon = (MeleeWeapon) axe;
		Level.set( oak, Terrain.TREE_OAK, ow );
		clear( ow, oak );
		Random.pushGenerator( 77 );
		try {
			Gathering.fell( hero, ow, oak );
			return Random.Long();
		} finally {
			Random.popGenerator();
		}
	}

	//the same for a boulder broken with this tool in hand
	private long afterQuarrying( OverworldLevel ow, int rock, Item tool ){
		hero.belongings.weapon = (MeleeWeapon) tool;
		Level.set( rock, Terrain.BOULDER, ow );
		clear( ow, rock );
		Random.pushGenerator( 78 );
		try {
			Gathering.quarry( hero, ow, rock );
			return Random.Long();
		} finally {
			Random.popGenerator();
		}
	}

	//the same for the rough stone of the world's rock, with the tools the hero carries now
	private long afterWallStone( OverworldLevel ow, int wall ){
		clear( ow, wall );
		Random.pushGenerator( 79 );
		try {
			Gathering.wallStone( ow, wall, hero );
			return Random.Long();
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void darkGoldIsNeverDoubled(){
		//the Blacksmith's dark gold is mined from the mines' decorated rock (Hero.actMine): no
		//vein's ore, so Gathering.extraOre can never be handed it, and the mines are no world slice
		assertFalse( Ore.class.isAssignableFrom( DarkGold.class ) );
		assertFalse( OverworldLevel.class.isAssignableFrom( MiningLevel.class ) );
		assertFalse( MiningLevel.class.isAssignableFrom( OverworldLevel.class ) );
		//on a slice only a vein in plain rock gives ore: decorated rock gives none to double
		OverworldLevel mt = windowIn( WorldModel.Biome.MOUNTAIN );
		int rock = find( mt, c -> mt.map[c] == Terrain.WALL );
		assertNotEquals( -1, rock );
		Level.set( rock, Terrain.WALL_DECO, mt );
		assertNull( mt.oreFrom( rock ) );
	}

	//what a held press on the cell puts the hero to (CellSelector.heldTap)
	private static Gathering.Job heldTapJob( Hero hero, int cell ){
		xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector.heldTap = true;
		try {
			return Gathering.tapJob( hero, cell );
		} finally {
			xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector.heldTap = false;
		}
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.homestead.stations;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Comfy;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCategory;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomeAnchors;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.RoomTraits;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.homestead.Shelter;
import xyz.gabriwar.warpedpixeldungeon.homestead.pieces.StationPieces;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.ChargrilledMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Charcoal;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Clay;
import xyz.gabriwar.warpedpixeldungeon.items.materials.CutStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.FiredBrick;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GlassPane;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Sand;
import xyz.gabriwar.warpedpixeldungeon.items.ore.IronOre;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLiquidFlame;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.IntConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The homesteads' stations (homestead/stations, homestead/pieces/StationPieces): the recipes and
 * their checks, crafting that takes everything at once and makes it batch by batch, fire that
 * only works while it burns and burns on by the global clock while the hero is away, the
 * workshop's speed, the hearth's heat in a closed room and its anchor, the gold pieces at the
 * town's rate, a station standing on a floor, saving one, and what it drops when it comes down.
 * Every window is a real one of the surface, derived from a seed; time passes by the global clock
 * (FarmCrop.clock: Statistics.duration).
 */
public class HomesteadStationsTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int depth, branch, challenges, startDay, turn, deepest, gold;
	private float duration;
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
		deepest = Statistics.deepestFloor;
		gold = Dungeon.gold;
		duration = Statistics.duration;
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
		Dungeon.gold = 0;
		Statistics.deepestFloor = 1;
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
		Dungeon.gold = gold;
		Statistics.deepestFloor = deepest;
		Statistics.duration = duration;
		DayNightCycle.debugPhaseOverride = override;
		ClimateManager.debugTempOverride = Float.NaN;
		ClimateManager.debugWindOverride = Float.NaN;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		HomesteadWorld.reset();
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
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
			OverworldLevel ow = windowAt( at[0], at[1] );
			int o = openGround( ow, w, h );
			if (o != -1){
				origin[0] = o;
				return ow;
			}
		}
		throw new AssertionError( "no open ground in any test window" );
	}

	private static void build( OverworldLevel ow, int cell, String id ){
		BuildPiece p = BuildCatalog.byId( id );
		assertNotNull( id, p );
		Homestead.build( ow, cell, p, null );
	}

	//a station of a piece laid on a cell, and the station
	@SuppressWarnings("unchecked")
	private static <T extends HomesteadStation> T station( OverworldLevel ow, int cell, String id ){
		build( ow, cell, id );
		for (Mob m : ow.mobs){
			if (m.pos == cell && m instanceof HomesteadStation) return (T) m;
		}
		throw new AssertionError( "no station on " + cell );
	}

	//a closed w x h room of `wall` round a plank floor, a door south, under `roof`
	private static void room( OverworldLevel ow, int o, int w, int h, String wall, String roof ){
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				int c = o + x + y * W;
				boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
				build( ow, c, !edge ? "floor_plank" : x == w / 2 && y == h - 1 ? "door" : wall );
			}
		}
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++) build( ow, o + x + y * W, roof );
		}
	}

	private void stand( OverworldLevel ow, int cell ){
		hero.pos = cell;
		hero.fieldOfView = new boolean[ow.length()];
		Arrays.fill( hero.fieldOfView, true );
	}

	private void give( Class<? extends Item> cls, int n ){
		Item i = Reflection.newInstance( cls );
		i.quantity( n );
		assertTrue( i.collect( hero.belongings.backpack ) );
	}

	private int count( Class<? extends Item> cls ){
		return Material.count( hero, cls );
	}

	//time passing on the global clock (FarmCrop.clock)
	private static void pass( float turns ){
		Statistics.duration += turns;
	}

	private static int made( HomesteadStation s, Class<? extends Item> cls ){
		int n = 0;
		for (Item i : s.output()) if (i.getClass() == cls) n += i.quantity();
		return n;
	}

	// ------------------------------------------------------------------ the recipes

	@Test
	public void everyRecipeIsWellFormedAndTheRegistryChecksThem(){
		HashSet<String> ids = new HashSet<>();
		HashSet<Class<?>> stations = new HashSet<>();
		for (StationRecipe r : StationRecipes.all()){
			StationRecipes.validate( r );
			assertTrue( "unique: " + r.id, ids.add( r.id ) );
			stations.add( r.station );
			assertFalse( r.id + " is named", r.name().contains( "NO TEXT" ) );
			for (StationRecipe.Input in : r.inputs) assertFalse( r.id + " input is named", in.label().contains( "NO TEXT" ) );
			if (r.needsHeat) assertTrue( r.id, HeatStation.class.isAssignableFrom( r.station ) );
		}
		for (Class<?> s : new Class<?>[]{ Workbench.class, MasonsBench.class, Kiln.class, Hearth.class,
				CookingPot.class, TanningRack.class, Forge.class, Quern.class }){
			assertTrue( s.getSimpleName() + " makes something", stations.contains( s ) );
		}
		//every recipe of a fire station needs the fire
		for (StationRecipe r : StationRecipes.forStation( new Kiln() )) assertTrue( r.id, r.needsHeat );
		assertEquals( 4, StationRecipes.forStation( new Kiln() ).size() );
		assertSame( StationRecipes.byId( "bricks" ), StationRecipes.forStation( new Kiln() ).get( 0 ) );

		//the gathering's tools are forged
		assertNotNull( StationRecipes.byId( "woodcutters_axe" ) );
		assertNotNull( StationRecipes.byId( "masons_hammer" ) );
		assertSame( Forge.class, StationRecipes.byId( "woodcutters_axe" ).station );
		assertSame( Forge.class, StationRecipes.byId( "masons_hammer" ).station );

		//what the registry refuses
		refused( StationRecipe.at( "t_nothing_in", Workbench.class ).out( Plank.class, 1 ).turns( 5 ).done() );
		refused( StationRecipe.at( "t_no_work", Workbench.class ).in( Log.class, 1 ).out( Plank.class, 1 ).turns( 0 ).done() );
		refused( StationRecipe.at( "t_zero_in", Workbench.class ).in( Log.class, 0 ).out( Plank.class, 1 ).turns( 5 ).done() );
		refused( StationRecipe.at( "t_nothing_out", Workbench.class ).in( Log.class, 1 ).turns( 5 ).done() );
		refused( StationRecipe.at( "t_cold_fire", Workbench.class ).in( Log.class, 1 ).out( Plank.class, 1 ).turns( 5 ).heat().done() );
		refused( StationRecipe.at( "bricks", Kiln.class ).in( Clay.class, 1 ).out( FiredBrick.class, 1 ).turns( 5 ).heat().done() );

		//one that is fine goes in, and is listed at its station
		StationRecipe extra = StationRecipe.at( "t_extra", Quern.class ).in( Sand.class, 1 ).out( Clay.class, 1 ).turns( 3 ).done();
		StationRecipes.register( extra );
		try {
			assertSame( extra, StationRecipes.byId( "t_extra" ) );
			assertTrue( StationRecipes.forStation( new Quern() ).contains( extra ) );
			assertFalse( StationRecipes.forStation( new Workbench() ).contains( extra ) );
		} finally {
			StationRecipes.unregister( "t_extra" );
		}
		assertNull( StationRecipes.byId( "t_extra" ) );
	}

	private static void refused( StationRecipe r ){
		try {
			StationRecipes.register( r );
		} catch (IllegalArgumentException e){
			return;
		}
		StationRecipes.unregister( r.id );
		fail( r.id + " should have been refused" );
	}

	@Test
	public void noStationEverMakesAMasterworkCore(){
		for (StationRecipe r : StationRecipes.all()){
			assertFalse( r.id, r.output.sample() instanceof MasterworkCore );
		}
		for (StationRecipe r : StationRecipes.forStation( new Forge() )){
			assertFalse( r.id, MasterworkCore.class.isAssignableFrom( r.output.sample().getClass() ) );
		}
		//not even one added later, at the forge or anywhere
		refused( StationRecipe.at( "t_core", Forge.class ).in( IronOre.class, 1 ).out( MasterworkCore.class, 1 ).turns( 5 ).heat().done() );
		refused( StationRecipe.at( "t_core_bench", Workbench.class ).in( Plank.class, 1 ).out( MasterworkCore.class, 1 ).turns( 5 ).done() );
	}

	// ------------------------------------------------------------------ crafting

	@Test
	public void craftingTakesEverythingAtOnceAndMakesItBatchByBatch(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		Workbench bench = station( ow, o[0] + 2 + W, "workbench" );
		StationRecipe planks = StationRecipes.byId( "planks_oak" );
		give( Log.class, 3 );

		assertEquals( HomesteadStation.Craft.NO_MATERIALS, bench.queue( hero, planks, 4, null ) );
		assertEquals( "nothing taken", 3, count( Log.class ) );
		assertEquals( HomesteadStation.Craft.BAD_BATCH, bench.queue( hero, planks, 0, null ) );
		assertEquals( HomesteadStation.Craft.NOT_HERE, bench.queue( hero, StationRecipes.byId( "bricks" ), 1, null ) );

		assertEquals( HomesteadStation.Craft.OK, bench.queue( hero, planks, 2, null ) );
		assertEquals( "both batches' logs taken at once", 1, count( Log.class ) );
		assertEquals( 2, bench.jobs().get( 0 ).held.get( 0 ).quantity() );
		assertTrue( bench.working() );

		pass( 15 );
		bench.catchUp( ow );
		assertEquals( "one batch done", 3, made( bench, Plank.class ) );
		assertEquals( 1, bench.jobs().get( 0 ).batches );
		assertEquals( 0.5f, bench.jobs().get( 0 ).fraction(), 0.001f );
		pass( 30 );
		bench.catchUp( ow );
		assertEquals( 6, made( bench, Plank.class ) );
		assertTrue( "the job is done", bench.jobs().isEmpty() );
		assertFalse( bench.working() );

		assertEquals( 1, bench.takeAll( hero ) );
		assertEquals( 6, count( Plank.class ) );
		assertTrue( bench.output().isEmpty() );

		//four jobs at most; one called off gives back what it held
		give( Log.class, 4 );
		for (int i = 0; i < HomesteadStation.MAX_JOBS; i++) assertEquals( HomesteadStation.Craft.OK, bench.queue( hero, planks, 1, null ) );
		assertEquals( HomesteadStation.Craft.QUEUE_FULL, bench.queue( hero, planks, 1, null ) );
		assertEquals( 1, count( Log.class ) );
		assertTrue( bench.cancel( 3, hero ) );
		assertEquals( 2, count( Log.class ) );
		assertFalse( bench.cancel( 7, hero ) );
	}

	@Test
	public void theCookingPotCooksABlandfruitWithTheSeedChosen(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		CookingPot pot = station( ow, o[0] + 2 + W, "cooking_pot" );
		StationRecipe fruit = StationRecipes.byId( "blandfruit" );
		give( Blandfruit.class, 1 );
		give( Firebloom.Seed.class, 1 );
		give( Sungrass.Seed.class, 1 );
		assertEquals( "the seed is the hero's to choose", HomesteadStation.Craft.NO_MATERIALS, pot.queue( hero, fruit, 1, null ) );
		assertEquals( 1, fruit.affordable( hero, Firebloom.Seed.class ) );
		assertEquals( HomesteadStation.Craft.OK, pot.queue( hero, fruit, 1, Firebloom.Seed.class ) );
		assertEquals( 0, count( Firebloom.Seed.class ) );
		assertEquals( "the other seed is left", 1, count( Sungrass.Seed.class ) );
		pass( 15 );
		pot.catchUp( ow );
		List<Item> out = pot.output();
		assertEquals( 1, out.size() );
		assertTrue( out.get( 0 ) instanceof Blandfruit );
		assertTrue( ((Blandfruit) out.get( 0 )).potionAttrib instanceof PotionOfLiquidFlame );

		//a stew from monster meat as from mystery meat, no energy asked
		give( MysteryMeat.class, 2 );
		assertEquals( HomesteadStation.Craft.OK, pot.queue( hero, StationRecipes.byId( "stew" ), 2, null ) );
		pass( 20 );
		pot.catchUp( ow );
		assertEquals( 2, made( pot, xyz.gabriwar.warpedpixeldungeon.items.food.StewedMeat.class ) );
	}

	// ------------------------------------------------------------------ fire

	@Test
	public void heatWorkOnlyGoesOnWhileTheFireBurns(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		Kiln kiln = station( ow, o[0] + 2 + W, "kiln" );
		give( Clay.class, 4 );
		assertEquals( HomesteadStation.Craft.OK, kiln.queue( hero, StationRecipes.byId( "bricks" ), 2, null ) );
		assertFalse( "queued work does not light it", kiln.lit() );
		assertFalse( kiln.working() );

		pass( 200 );
		kiln.catchUp( ow );
		assertEquals( "cold, nothing done", 0, made( kiln, FiredBrick.class ) );
		assertEquals( 0f, kiln.jobs().get( 0 ).progress, 0.001f );
		assertFalse( "nothing to burn", kiln.light() );

		give( Log.class, 1 );
		assertEquals( 1, kiln.addFuel( hero, hero.belongings.getItem( Log.class ) ) );
		assertEquals( 0, count( Log.class ) );
		assertEquals( HeatStation.LOG_TURNS, kiln.fuelTurns(), 0.001f );
		assertTrue( kiln.light() );
		assertTrue( kiln.working() );
		assertTrue( kiln.busy() );

		pass( 60 );
		kiln.catchUp( ow );
		assertEquals( "a batch in sixty turns of fire", 2, made( kiln, FiredBrick.class ) );
		assertEquals( HeatStation.LOG_TURNS - 60, kiln.fuelTurns(), 0.001f );

		kiln.putOut();
		assertFalse( kiln.lit() );
		pass( 500 );
		kiln.catchUp( ow );
		assertEquals( "put out, it burns nothing", HeatStation.LOG_TURNS - 60, kiln.fuelTurns(), 0.001f );
		assertEquals( 2, made( kiln, FiredBrick.class ) );
		assertTrue( "the half-burnt log lights again", kiln.light() );
		pass( 60 );
		kiln.catchUp( ow );
		assertEquals( 4, made( kiln, FiredBrick.class ) );
	}

	@Test
	public void theFireBurnsOnAndTheWorkCatchesUpOverALongAbsence(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		Kiln kiln = station( ow, o[0] + 2 + W, "kiln" );
		give( Clay.class, 10 );
		give( Sand.class, 4 );
		assertEquals( HomesteadStation.Craft.OK, kiln.queue( hero, StationRecipes.byId( "bricks" ), 5, null ) );
		assertEquals( HomesteadStation.Craft.OK, kiln.queue( hero, StationRecipes.byId( "glass" ), 2, null ) );
		//two logs and a lump of charcoal: 200 + 200 + 400 turns of fire, for 420 turns of work
		kiln.addFuel( hero, new Log().quantity( 2 ) );
		kiln.addFuel( hero, new Charcoal() );
		assertEquals( 3, kiln.fuelCount() );
		assertEquals( 2 * HeatStation.LOG_TURNS + HeatStation.CHARCOAL_TURNS, kiln.fuelTurns(), 0.001f );
		assertTrue( kiln.light() );

		//away for days: everything fired, and the fire out once the fuel was gone
		pass( 10000 );
		kiln.catchUp( ow );
		assertEquals( 10, made( kiln, FiredBrick.class ) );
		assertEquals( 2, made( kiln, GlassPane.class ) );
		assertTrue( kiln.jobs().isEmpty() );
		assertFalse( "burnt out", kiln.lit() );
		assertEquals( 0f, kiln.fuelTurns(), 0.001f );
		assertEquals( 0, kiln.fuelCount() );

		//too little fuel for the work: as far as the fire lasted, the rest waiting for more
		Kiln other = station( ow, o[0] + 2, "kiln" );
		give( Clay.class, 20 );
		assertEquals( HomesteadStation.Craft.OK, other.queue( hero, StationRecipes.byId( "bricks" ), 10, null ) );
		other.addFuel( hero, new Log().quantity( 2 ) );
		assertTrue( other.light() );
		pass( 5000 );
		other.catchUp( ow );
		//400 turns of fire, 60 a batch: six batches and two thirds of the seventh
		assertEquals( 12, made( other, FiredBrick.class ) );
		assertEquals( 4, other.jobs().get( 0 ).batches );
		assertEquals( 40f, other.jobs().get( 0 ).progress, 0.01f );
		assertFalse( other.lit() );
		assertTrue( "waiting on the fire", other.status().length() > 0 );

		//charcoal burns twice as long as a log
		Kiln third = station( ow, o[0] + 4, "kiln" );
		third.addFuel( hero, new Charcoal() );
		assertTrue( third.light() );
		pass( HeatStation.CHARCOAL_TURNS - 1 );
		third.catchUp( ow );
		assertTrue( third.lit() );
		pass( 2 );
		third.catchUp( ow );
		assertFalse( third.lit() );
	}

	@Test
	public void theFuelSlotTakesOnlyFuelAndOnlySoMuch(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		Forge forge = station( ow, o[0] + 2 + W, "forge" );
		assertEquals( 0, forge.addFuel( hero, new Plank().quantity( 5 ) ) );
		give( Log.class, 30 );
		assertEquals( HeatStation.FUEL_CAP, forge.addFuel( hero, hero.belongings.getItem( Log.class ) ) );
		assertEquals( 10, count( Log.class ) );
		assertEquals( 0, forge.fuelRoom() );
		assertEquals( 0, forge.addFuel( hero, hero.belongings.getItem( Log.class ) ) );
		assertTrue( HeatStation.isFuel( new Charcoal() ) );
		assertFalse( HeatStation.isFuel( new Plank() ) );
	}

	@Test
	public void theHearthGrillsWhatIsDroppedInItsFire(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		Hearth hearth = station( ow, o[0] + 2 + W, "hearth_brick" );
		ow.drop( new MysteryMeat(), hearth.pos );
		hearth.liveTurn( ow );
		assertTrue( "cold, nothing cooks", ow.heaps.get( hearth.pos ).peek() instanceof MysteryMeat );
		hearth.addFuel( hero, new Log() );
		assertTrue( hearth.light() );
		hearth.liveTurn( ow );
		Heap h = ow.heaps.get( hearth.pos );
		assertNotNull( h );
		assertTrue( h.peek() instanceof ChargrilledMeat );
	}

	// ------------------------------------------------------------------ rooms

	@Test
	public void aWorkshopWorksAQuarterFaster(){
		int[] o = new int[1];
		OverworldLevel ow = site( 16, 5, o );
		room( ow, o[0], 7, 5, "wall_stone", "roof_slate" );
		int inside = o[0] + 3 + 2 * W;
		assertNotNull( Rooms.at( ow, inside ) );

		Workbench bench = station( ow, o[0] + 1 + W, "workbench" );
		bench.sync( ow );
		assertFalse( "a workbench alone is no workshop", RoomTraits.is( ow, Rooms.at( ow, inside ), StationRooms.WORKSHOP ) );
		assertEquals( 1f, bench.speed( ow ), 0.0001f );

		Quern quern = station( ow, o[0] + 5 + W, "quern" );
		quern.sync( ow );
		Rooms.Room room = Rooms.at( ow, inside );
		assertTrue( RoomTraits.is( ow, room, StationRooms.WORKSHOP ) );
		assertTrue( Rooms.describe( ow, inside ).contains( "Workshop" ) );
		assertEquals( HomesteadStation.WORKSHOP_SPEED, bench.speed( ow ), 0.0001f );
		assertEquals( HomesteadStation.WORKSHOP_SPEED, quern.speed( ow ), 0.0001f );

		//the same ten turns of sawing take eight in the workshop
		Workbench outside = station( ow, o[0] + 12 + 2 * W, "workbench" );
		outside.sync( ow );
		give( Log.class, 2 );
		assertEquals( HomesteadStation.Craft.OK, bench.queue( hero, StationRecipes.byId( "planks_oak" ), 1, null ) );
		assertEquals( HomesteadStation.Craft.OK, outside.queue( hero, StationRecipes.byId( "planks_oak" ), 1, null ) );
		pass( 8 );
		bench.catchUp( ow );
		outside.catchUp( ow );
		assertEquals( 3, made( bench, Plank.class ) );
		assertEquals( 0, made( outside, Plank.class ) );

		//the speed is kept between turns, and worked out again once the room's stations change
		assertTrue( Homestead.damage( ow, quern.pos, 1000, null ) );
		assertFalse( ow.mobs.contains( quern ) );
		assertEquals( "the quern gone, no workshop", 1f, bench.speed( ow ), 0.0001f );
		Quern again = station( ow, o[0] + 5 + W, "quern" );
		again.sync( ow );
		assertEquals( HomesteadStation.WORKSHOP_SPEED, bench.speed( ow ), 0.0001f );
		//and once the room itself changes: a wall broken open, no room, no workshop
		assertTrue( Homestead.damage( ow, o[0] + 3, 1000, null ) );
		assertEquals( "the room broken open", 1f, bench.speed( ow ), 0.0001f );
	}

	@Test
	public void aLitHearthWarmsItsClosedRoomAndKeepsTheWildAway(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		room( ow, o[0], 7, 5, "wall_log", "roof_thatch" );
		ClimateManager.debugTempOverride = -15f;
		ClimateManager.debugWindOverride = 0f;
		int inside = o[0] + 3 + 2 * W;
		Rooms.Room room = Rooms.at( ow, inside );
		assertNotNull( room );
		float cold = TileTemperature.tileTemp( inside );
		assertEquals( -15f + 30f * room.insulation, cold, 0.01f );

		Hearth hearth = station( ow, o[0] + 3 + W, "hearth_stone" );
		hearth.sync( ow );
		assertEquals( "unlit, no heat", 0f, Rooms.at( ow, inside ).heat(), 0.001f );
		assertFalse( Shelter.heated( ow, inside ) );

		hearth.addFuel( hero, new Log().quantity( 3 ) );
		assertTrue( hearth.light() );
		hearth.sync( ow );
		assertEquals( Hearth.ROOM_HEAT, Rooms.at( ow, inside ).heat(), 0.001f );
		assertEquals( "the room's air warmed by the fire", cold + Hearth.ROOM_HEAT, TileTemperature.tileTemp( inside ), 0.01f );
		assertTrue( TileTemperature.tileTemp( inside ) > 0f );
		assertTrue( Shelter.heated( ow, inside ) );
		hero.pos = inside;
		Comfy.check( hero );
		assertNotNull( hero.buff( Comfy.class ) );

		//no wild animal is set down near a lit hearth
		assertTrue( HomeAnchors.near( ow, hearth.pos + 6 ) );
		assertTrue( HomeAnchors.near( ow, hearth.pos + HomeAnchors.RADIUS * W ) );
		final int[] anchors = { 0 };
		IntConsumer counted = c -> anchors[0]++;
		new StationAnchors().anchors( ow, counted );
		assertEquals( 1, anchors[0] );
		hearth.putOut();
		anchors[0] = 0;
		new StationAnchors().anchors( ow, counted );
		assertEquals( "out, it anchors nothing", 0, anchors[0] );
		assertEquals( 0f, new StationHeat().heatAt( ow, hearth.pos ), 0.001f );

		//the kiln and the forge warm their rooms a little
		assertEquals( Kiln.ROOM_HEAT, litHeat( ow, o[0] + 1 + W, "kiln" ), 0.001f );
		assertEquals( Forge.ROOM_HEAT, litHeat( ow, o[0] + 5 + W, "forge" ), 0.001f );
	}

	private float litHeat( OverworldLevel ow, int cell, String id ){
		HeatStation s = station( ow, cell, id );
		s.addFuel( hero, new Log() );
		assertTrue( s.light() );
		s.sync( ow );
		return new StationHeat().heatAt( ow, cell );
	}

	// ------------------------------------------------------------------ the pieces

	@Test
	public void theStationsAreInTheBuildMenu(){
		String[] ids = { "workbench", "masons_bench", "kiln", "hearth_brick", "hearth_stone", "cooking_pot",
				"tanning_rack", "forge", "quern", "alchemy_pot", "enchanting_pedestal" };
		for (String id : ids){
			BuildPiece p = BuildCatalog.byId( id );
			assertNotNull( id, p );
			assertEquals( BuildCategory.STATIONS, p.category );
			assertFalse( id, p.name().contains( "NO TEXT" ) );
			assertFalse( id, p.desc().contains( "NO TEXT" ) );
		}
		assertEquals( 40, BuildCatalog.byId( "workbench" ).maxHp );
		assertTrue( BuildCatalog.byId( "workbench" ).flammable );
		assertTrue( BuildCatalog.byId( "tanning_rack" ).flammable );
		assertFalse( BuildCatalog.byId( "kiln" ).flammable );
		assertEquals( 160, BuildCatalog.byId( "forge" ).maxHp );
		assertEquals( 8, BuildCatalog.byId( "hearth_stone" ).cost.get( 0 ).quantity );
		assertEquals( 0, BuildCatalog.byId( "alchemy_pot" ).maxHp );
		assertEquals( Terrain.ALCHEMY, BuildCatalog.byId( "alchemy_pot" ).terrain );
		//their names on screen
		for (Class<? extends HomesteadStation> s : java.util.Arrays.<Class<? extends HomesteadStation>>asList( Workbench.class,
				MasonsBench.class, Kiln.class, Hearth.class, CookingPot.class, TanningRack.class, Forge.class, Quern.class )){
			HomesteadStation st = Reflection.newInstance( s );
			assertFalse( s.getSimpleName(), st.name().contains( "NO TEXT" ) );
			assertFalse( s.getSimpleName(), st.description().contains( "NO TEXT" ) );
		}
	}

	@Test
	public void theGoldPiecesCostTheTownsRateAndGiveNoGoldBack(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		int cell = o[0] + 2 + W;
		stand( ow, o[0] + W );
		BuildPiece pot = BuildCatalog.byId( "alchemy_pot" );
		BuildPiece.Cost price = pot.cost.get( 0 );
		assertTrue( price.gold() );
		assertEquals( StationPieces.ALCHEMY_GOLD, price.quantity );
		Statistics.deepestFloor = 12;
		assertEquals( "the town's rate at floor 12: three times", StationPieces.ALCHEMY_GOLD * 3, price.amount() );
		assertEquals( "the bricks, never the gold, are what comes back", FiredBrick.class, pot.mainCost().material );

		give( FiredBrick.class, 4 );
		Dungeon.gold = StationPieces.ALCHEMY_GOLD * 3 - 1;
		assertEquals( Homestead.Reason.NO_MATERIALS, Homestead.canPlace( hero, cell, pot ) );
		Dungeon.gold = StationPieces.ALCHEMY_GOLD * 3 + 5;
		assertEquals( Homestead.Reason.OK, Homestead.place( hero, cell, pot ) );
		assertEquals( 5, Dungeon.gold );
		assertEquals( 0, count( FiredBrick.class ) );
		assertEquals( Terrain.ALCHEMY, ow.map[cell] );
		assertSame( pot, Homestead.pieceAt( ow, cell ) );
		assertFalse( "nothing breaks it", Homestead.damage( ow, cell, 500, null ) );
		assertEquals( Homestead.Reason.OK, Homestead.remove( hero, cell ) );
		assertEquals( 5, Dungeon.gold );
		assertEquals( "half the bricks", 2, count( FiredBrick.class ) );
		for (BuildPiece.Cost c : Homestead.refundOf( pot )) assertFalse( c.gold() );
		for (BuildPiece.Cost c : Homestead.repairCost( pot )) assertFalse( c.gold() );

		//the pedestal: its station on its stone, both gone when it comes down
		BuildPiece pedestal = BuildCatalog.byId( "enchanting_pedestal" );
		int ground = ow.map[cell];
		give( CutStone.class, 4 );
		Dungeon.gold = StationPieces.PEDESTAL_GOLD * 3;
		assertEquals( Homestead.Reason.OK, Homestead.place( hero, cell, pedestal ) );
		assertEquals( 0, Dungeon.gold );
		assertEquals( Terrain.PEDESTAL, ow.map[cell] );
		assertSame( pedestal, Homestead.pieceAt( ow, cell ) );
		boolean station = false;
		for (Mob m : ow.mobs) if (m instanceof EnchantingStation && m.pos == cell) station = true;
		assertTrue( station );
		assertEquals( Homestead.Reason.OK, Homestead.remove( hero, cell ) );
		assertEquals( ground, ow.map[cell] );
		for (Mob m : ow.mobs) assertFalse( m instanceof EnchantingStation && m.pos == cell );
		assertEquals( 2, count( CutStone.class ) );
		assertEquals( 0, Dungeon.gold );
	}

	@Test
	public void aStationStandsOnAFloorAndGivesItBack(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		int cell = o[0] + 2 + W;
		stand( ow, o[0] + W );
		give( Plank.class, 1 );
		assertEquals( Homestead.Reason.OK, Homestead.place( hero, cell, BuildCatalog.byId( "floor_plank" ) ) );
		give( Plank.class, 6 );
		assertEquals( Homestead.Reason.OK, Homestead.place( hero, cell, BuildCatalog.byId( "workbench" ) ) );
		assertEquals( "the floor is not taken up", 0, count( Plank.class ) );
		assertEquals( Terrain.FLOOR_PLANK, ow.map[cell] );
		assertSame( BuildCatalog.byId( "workbench" ), Homestead.pieceAt( ow, cell ) );
		assertEquals( Homestead.Reason.OK, Homestead.remove( hero, cell ) );
		assertEquals( 3, count( Plank.class ) );
		assertSame( "the floor is the cell's piece again", BuildCatalog.byId( "floor_plank" ), Homestead.pieceAt( ow, cell ) );
		assertEquals( Homestead.Reason.OK, Homestead.canRemove( hero, cell ) );
	}

	// ------------------------------------------------------------------ saved, hurt, wrecked

	@Test
	public void aStationKeepsItsWorkThroughASave(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		Kiln kiln = station( ow, o[0] + 2 + W, "kiln" );
		give( Clay.class, 6 );
		kiln.queue( hero, StationRecipes.byId( "bricks" ), 3, null );
		kiln.addFuel( hero, new Log().quantity( 2 ) );
		kiln.light();
		pass( 90 );
		kiln.catchUp( ow );

		Bundle b = new Bundle();
		kiln.storeInBundle( b );
		Kiln back = new Kiln();
		back.restoreFromBundle( b );
		assertTrue( back.lit() );
		assertTrue( back.busy() );
		assertEquals( kiln.fuelTurns(), back.fuelTurns(), 0.001f );
		assertEquals( 1, back.fuelCount() );
		assertEquals( 2, made( back, FiredBrick.class ) );
		assertEquals( 1, back.jobs().size() );
		assertEquals( 2, back.jobs().get( 0 ).batches );
		assertEquals( 30f, back.jobs().get( 0 ).progress, 0.01f );
		assertEquals( 4, back.jobs().get( 0 ).held.get( 0 ).quantity() );
		//and goes on from where it was by the same clock
		pass( 30 );
		back.catchUp( ow );
		assertEquals( 4, made( back, FiredBrick.class ) );
	}

	@Test
	public void noBlowHurtsAStationButItBurnsAndBreaksAsAPiece(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		int cell = o[0] + 2 + W;
		Workbench bench = station( ow, cell, "workbench" );
		bench.damage( 1000, hero );
		assertTrue( ow.mobs.contains( bench ) );
		assertFalse( bench.add( new Comfy() ) );

		//in the flames it burns down a piece at a time (Fire.evolve: Homestead.burn)
		Blob.seed( cell, 10, Fire.class, ow );
		assertTrue( "it keeps the fire burning on it", Homestead.burn( cell ) );
		assertEquals( 40 - Homestead.BURN_DAMAGE, Homestead.hpAt( ow, cell ) );

		//wrecked, what it held lands where it stood
		give( Log.class, 2 );
		bench.queue( hero, StationRecipes.byId( "planks_oak" ), 2, null );
		pass( 10 );
		bench.catchUp( ow );
		assertTrue( Homestead.damage( ow, cell, 100, null ) );
		assertFalse( ow.mobs.contains( bench ) );
		assertNull( Homestead.pieceAt( ow, cell ) );
		Heap h = ow.heaps.get( cell );
		assertNotNull( h );
		int planks = 0, logs = 0;
		for (Item i : h.items){
			if (i instanceof Plank) planks += i.quantity();
			if (i instanceof Log) logs += i.quantity();
		}
		assertEquals( "the batch made", 3, planks );
		assertEquals( "the batch still to make", 1, logs );
	}

	@Test
	public void theSliceRegisterIsMadeAgainFromTheWindowsStations(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		Hearth hearth = station( ow, o[0] + 2 + W, "hearth_brick" );
		hearth.addFuel( hero, new Log() );
		hearth.light();
		StationSlice slice = StationSlice.of( ow );
		assertNotNull( slice );
		slice.materialize( ow );
		assertEquals( Hearth.ROOM_HEAT, slice.heatAt( ow, hearth.pos ), 0.001f );
		assertSame( Hearth.class, slice.kindAt( ow, hearth.pos ) );
		ArrayList<Integer> cells = new ArrayList<>();
		slice.anchors( ow, cells::add );
		assertEquals( 1, cells.size() );
		assertEquals( hearth.pos, (int) cells.get( 0 ) );

		//a station gone from the window while it was away is dropped once the window arrives
		ow.mobs.remove( hearth );
		slice.materialize( ow );
		assertNull( slice.kindAt( ow, hearth.pos ) );
		assertEquals( 0f, slice.heatAt( ow, hearth.pos ), 0.001f );
	}

	//OverworldLevel's own homesteadLeaving / homesteadArrived, as a rebase, a jump or the season
	//turning call them round the window's move and the peopling of the new window
	private static void lifecycle( OverworldLevel ow, String which ){
		try {
			java.lang.reflect.Method m = OverworldLevel.class.getDeclaredMethod( which );
			m.setAccessible( true );
			m.invoke( ow );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	@Test
	public void aLitHearthKeepsTheWildAwayWhileTheWindowIsPeopled(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		Hearth hearth = station( ow, o[0] + 2 + W, "hearth_stone" );
		hearth.addFuel( hero, new Log() );
		assertTrue( hearth.light() );
		int near = hearth.pos + 5;
		assertTrue( HomeAnchors.near( ow, near ) );

		//the window moves: between the sections let go of it and being told it has arrived, the
		//new window's wildlife is set down (populate: populateFauna asks HomeAnchors.near)
		lifecycle( ow, "homesteadLeaving" );
		//a turn on, so the anchors are worked out afresh (HomeAnchors keeps them for a moment)
		pass( 1 );
		assertTrue( "a lit hearth anchors while the window is peopled", HomeAnchors.near( ow, near ) );
		assertEquals( Hearth.ROOM_HEAT, new StationHeat().heatAt( ow, hearth.pos ), 0.001f );
		lifecycle( ow, "homesteadArrived" );
		assertTrue( HomeAnchors.near( ow, near ) );
	}

	@Test
	public void aHearthsFireEndsOnTheClockWhetherItActsOrNot(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		Hearth hearth = station( ow, o[0] + 2 + W, "hearth_stone" );
		hearth.addFuel( hero, new Log() );
		assertTrue( hearth.light() );
		hearth.sync( ow );
		int near = hearth.pos + 5;
		pass( HeatStation.LOG_TURNS - 10 );
		assertTrue( "still burning", HomeAnchors.near( ow, near ) );
		assertEquals( Hearth.ROOM_HEAT, new StationHeat().heatAt( ow, hearth.pos ), 0.001f );
		//its one log burnt away, with the hearth parked off the window and never acting
		pass( 20 );
		assertFalse( "burnt out, it anchors nothing", HomeAnchors.near( ow, near ) );
		assertEquals( 0f, new StationHeat().heatAt( ow, hearth.pos ), 0.001f );
		//fuel put on while it burns keeps it burning longer
		Hearth other = station( ow, o[0] + 2 + 2 * W, "hearth_brick" );
		other.addFuel( hero, new Log() );
		assertTrue( other.light() );
		other.addFuel( hero, new Log() );
		pass( HeatStation.LOG_TURNS + 50 );
		assertEquals( Hearth.ROOM_HEAT, new StationHeat().heatAt( ow, other.pos ), 0.001f );
	}

	@Test
	public void aWoodenStationCatchesFireOnStoneAndBurnsDown(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		int wall = o[0] + 1 + W, cell = o[0] + 2 + W, beside = o[0] + 3 + W;
		build( ow, wall, "wall_plank" );
		build( ow, cell, "floor_stone" );
		build( ow, beside, "floor_stone" );
		Workbench bench = station( ow, cell, "workbench" );
		Quern quern = station( ow, beside, "quern" );
		stand( ow, o[0] );
		assertFalse( "stone does not burn", ow.flamable[cell] );
		assertTrue( Homestead.catches( cell ) );
		assertFalse( "a quern is stone", Homestead.catches( beside ) );

		//the fire beside it goes on until it catches (it catches one turn in eight on the overworld)
		Fire fire = Blob.seed( wall, 4, Fire.class, ow );
		int turns = 0;
		while (fire.cur[cell] <= 0 && turns < 300){
			if (fire.cur[wall] <= 0) Blob.seed( wall, 4, Fire.class, ow );
			fire.act();
			turns++;
		}
		assertTrue( "the workbench caught", fire.cur[cell] > 0 );
		int lastHp = Homestead.hpAt( ow, cell );
		while (ow.mobs.contains( bench ) && turns < 400){
			fire.act();
			turns++;
			if (ow.mobs.contains( bench )){
				assertTrue( "it burns down a little a turn", Homestead.hpAt( ow, cell ) < lastHp );
				lastHp = Homestead.hpAt( ow, cell );
			}
		}
		assertFalse( "burnt down", ow.mobs.contains( bench ) );
		assertEquals( "its stone floor stays", Terrain.FLOOR_STONE, ow.map[cell] );
		assertTrue( ow.mobs.contains( quern ) );
		assertEquals( "the quern never catches", 100, Homestead.hpAt( ow, beside ) );
	}

	@Test
	public void aStationTakenDownTakesNothingMore(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		Kiln kiln = station( ow, o[0] + 2 + W, "kiln" );
		assertTrue( kiln.standing() );
		assertTrue( Homestead.damage( ow, kiln.pos, 1000, null ) );
		assertFalse( "wrecked, though a window may still be open on it", kiln.standing() );

		give( Clay.class, 2 );
		give( Log.class, 3 );
		assertEquals( HomesteadStation.Craft.GONE, kiln.queue( hero, StationRecipes.byId( "bricks" ), 1, null ) );
		assertEquals( 2, count( Clay.class ) );
		assertEquals( 0, kiln.addFuel( hero, hero.belongings.getItem( Log.class ) ) );
		assertEquals( 3, count( Log.class ) );
		assertFalse( kiln.light() );
		assertFalse( HomesteadStation.Craft.GONE.message().contains( "NO TEXT" ) );
	}

	@Test
	public void eachStationHasItsOwnSpriteForCoopGuests(){
		HomesteadStation[] all = { new Workbench(), new MasonsBench(), new Quern(), new TanningRack(),
				new CookingPot(), new Kiln(), new Forge(), new Hearth() };
		HashSet<Class<?>> seen = new HashSet<>();
		for (HomesteadStation s : all){
			assertNotNull( s.spriteClass );
			assertTrue( xyz.gabriwar.warpedpixeldungeon.sprites.StationSprite.class.isAssignableFrom( s.spriteClass ) );
			assertTrue( s.getClass().getSimpleName() + " shares a sprite", seen.add( s.spriteClass ) );
			//a guest is sent the sprite's name and finds it again by it (SpectatorReceiver.resolveSpriteClass)
			String wire = xyz.gabriwar.warpedpixeldungeon.net.NetSprites.wireName( s.spriteClass );
			try {
				assertSame( s.spriteClass, Class.forName( xyz.gabriwar.warpedpixeldungeon.net.NetSprites.PACKAGE + wire ) );
			} catch (ClassNotFoundException e){
				throw new AssertionError( wire, e );
			}
		}
	}
}

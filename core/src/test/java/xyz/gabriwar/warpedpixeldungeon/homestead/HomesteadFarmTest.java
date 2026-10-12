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

package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Animals;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.CompostBin;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Crop;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Enclosure;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.FarmRules;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.FarmSettler;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.FarmSlice;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Farming;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Grazing;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Pests;
import xyz.gabriwar.warpedpixeldungeon.items.Fertilizer;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.FusedTomato;
import xyz.gabriwar.warpedpixeldungeon.items.food.Aprico;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cornwheatshaft;
import xyz.gabriwar.warpedpixeldungeon.items.food.Grape;
import xyz.gabriwar.warpedpixeldungeon.items.food.PlantEgg;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.plants.Apricobush;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Cornwheat;
import xyz.gabriwar.warpedpixeldungeon.plants.Eggbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Gobgrape;
import xyz.gabriwar.warpedpixeldungeon.plants.Rotberry;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.plants.Tomatobush;
import xyz.gabriwar.warpedpixeldungeon.tiles.RoofTiles;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The fields of the world's slices (homestead/farming): the pieces, a crop's record through window
 * moves and a save, the seasons, the glass and the water, frost while the hero is there and while
 * he is away, what the sickle reaps, the cycles after ripening, the compost bin, fences, and the
 * pests at night and while he is away.
 */
public class HomesteadFarmTest extends FarmTestBase {

	//a crop of a seed on a fresh farm plot
	private static FarmCrop crop( OverworldLevel ow, int cell, Class<? extends xyz.gabriwar.warpedpixeldungeon.plants.Plant.Seed> seed ){
		build( ow, cell, "farm_plot" );
		return Farming.plant( ow, cell, seed );
	}

	// ------------------------------------------------------------------ pieces and rules

	@Test
	public void theFarmPiecesAreListed(){
		BuildPiece plot = BuildCatalog.byId( "farm_plot" );
		assertEquals( BuildCategory.FARMING, plot.category );
		assertTrue( plot.cost.isEmpty() );
		assertEquals( 2, plot.buildTurns );
		assertTrue( plot.canArea );
		BuildPiece channel = BuildCatalog.byId( "channel" );
		assertEquals( Terrain.WATER, channel.terrain );
		assertTrue( channel.canLine );
		assertEquals( 4, channel.buildTurns );
		assertEquals( BuildPiece.Placement.NEXT_TO_WATER, channel.placement );
		BuildPiece well = BuildCatalog.byId( "well" );
		assertEquals( Terrain.WELL, well.terrain );
		assertEquals( 8, well.cost.get( 0 ).quantity );
		assertEquals( 1, well.cost.get( 1 ).quantity );
		assertEquals( 30, BuildCatalog.byId( "compost_bin" ).maxHp );
		assertEquals( 4, BuildCatalog.byId( "compost_bin" ).cost.get( 0 ).quantity );
		assertEquals( 2, BuildCatalog.byId( "tie_post" ).cost.get( 0 ).quantity );
		for (String id : new String[]{ "farm_plot", "channel", "well", "compost_bin", "tie_post" }){
			BuildPiece p = BuildCatalog.byId( id );
			assertFalse( id + " is named", p.name().contains( "NO TEXT" ) );
			assertFalse( id + " is described", p.desc().contains( "NO TEXT" ) );
		}
	}

	@Test
	public void theSeasonsTheGlassAndTheWaterSetThePace(){
		assertEquals( 1.25f, FarmRules.seasonRate( GameCalendar.Season.SPRING, false ), 0f );
		assertEquals( 1f, FarmRules.seasonRate( GameCalendar.Season.SUMMER, false ), 0f );
		assertEquals( 0.75f, FarmRules.seasonRate( GameCalendar.Season.AUTUMN, false ), 0f );
		assertEquals( 0f, FarmRules.seasonRate( GameCalendar.Season.WINTER, false ), 0f );
		assertEquals( 1f, FarmRules.seasonRate( GameCalendar.Season.WINTER, true ), 0f );

		//firebloom (500 turns) is no water lover: half the bonus; sungrass (900) takes all of it
		assertEquals( 500, FarmRules.cycleTurns( Firebloom.class, 0 ) );
		assertEquals( 400, FarmRules.cycleTurns( Firebloom.class, 4 ) );
		assertEquals( 540, FarmRules.cycleTurns( Sungrass.class, 4 ) );
		assertEquals( 15, FarmRules.witherChance( Firebloom.class, 0 ) );
		assertEquals( 5, FarmRules.witherChance( Firebloom.class, 2 ) );
		assertEquals( FarmRules.MIN_WITHER, FarmRules.witherChance( Sungrass.class, 4 ) );

		//the calendar's days by season
		assertEquals( GameCalendar.Season.SPRING, GameCalendar.seasonOfDay( SPRING_DAY ) );
		assertEquals( GameCalendar.Season.SUMMER, GameCalendar.seasonOfDay( SUMMER_DAY ) );
		assertEquals( GameCalendar.Season.AUTUMN, GameCalendar.seasonOfDay( AUTUMN_DAY ) );
		assertEquals( GameCalendar.Season.WINTER, GameCalendar.seasonOfDay( WINTER_DAY ) );
		setDay( WINTER_DAY );
		assertEquals( GameCalendar.seasonOfDay( WorldClock.day() ), GameCalendar.season() );
		assertTrue( FarmRules.winterBetween( AUTUMN_DAY, WINTER_DAY ) );
		assertFalse( FarmRules.winterBetween( SPRING_DAY, AUTUMN_DAY ) );
		assertTrue( "a year away always holds a winter", FarmRules.winterBetween( SPRING_DAY, SPRING_DAY + 400 ) );
		//across an absence each day grows at its own season's pace
		assertEquals( 2500 * 1.25f + 2500 * 1f, FarmRules.growth( 2 * 2500, 90, 91, true, false ), 0.01f );
	}

	@Test
	public void theWaterAndTheWellsWetTheFarmland(){
		int[] at = site( 9, 7 );
		OverworldLevel ow = quiet( at );
		int o = at[2];
		int dry = o + 1 + W, byWell = o + 6 + 3 * W, well = o + 8 + 3 * W;
		build( ow, dry, "farm_plot" );
		build( ow, byWell, "farm_plot" );
		assertEquals( Terrain.FARMLAND, ow.map[byWell] );
		assertEquals( 0, Farming.waterAt( ow, byWell ) );
		build( ow, well, "well" );
		assertEquals( FarmRules.WELL_WATER, Farming.waterAt( ow, byWell ) );
		assertEquals( Terrain.FARMLAND_WET, ow.map[byWell] );
		assertEquals( "the plot still stands, watered", BuildCatalog.byId( "farm_plot" ), Homestead.pieceAt( ow, byWell ) );
		assertEquals( Terrain.FARMLAND, ow.map[dry] );

		//a channel dug off the well runs water beside the dry plot
		for (int x = 8; x >= 2; x--) build( ow, o + x + 2 * W, "channel" );
		assertEquals( Terrain.WATER, ow.map[o + 2 + 2 * W] );
		assertTrue( Farming.waterAt( ow, dry ) > 0 );
		assertEquals( Terrain.FARMLAND_WET, ow.map[dry] );

		//filled in, the ground comes back, the plot dries
		stand( ow, o + 5 + 3 * W );
		for (int x = 2; x <= 8; x++){
			assertEquals( Homestead.Reason.OK, Homestead.remove( hero, o + x + 2 * W ) );
			assertNotEquals( Terrain.WATER, ow.map[o + x + 2 * W] );
		}
		assertEquals( Terrain.FARMLAND, ow.map[dry] );
	}

	@Test
	public void aChannelIsDugOnlyBesideWaterOrAWell(){
		int[] at = site( 5, 5 );
		OverworldLevel ow = quiet( at );
		int o = at[2];
		stand( ow, o + 2 + 2 * W );
		BuildPiece channel = BuildCatalog.byId( "channel" );
		assertEquals( Homestead.Reason.WRONG_GROUND, Homestead.canPlace( hero, o + 1 + W, channel ) );
		build( ow, o + W, "well" );
		assertEquals( Homestead.Reason.OK, Homestead.canPlace( hero, o + 1 + W, channel ) );
		assertEquals( Homestead.Reason.OK, Homestead.place( hero, o + 1 + W, channel ) );
		assertEquals( Homestead.Reason.OK, Homestead.canPlace( hero, o + 2 + W, channel ) );
		assertEquals( "nobody may stand where a well goes up", Homestead.Reason.OCCUPIED,
				Homestead.canPlace( hero, hero.pos, BuildCatalog.byId( "well" ) ) );
	}

	// ------------------------------------------------------------------ the crop's record

	@Test
	public void aCropOutlivesTheWindowMovingAwayAndASave() throws Exception {
		int[] at = site( 3, 3 );
		OverworldLevel ow = quiet( at );
		int c = at[2] + 1 + W;
		int wx = ow.worldX + c % W, wy = ow.worldY + c / W;
		FarmCrop m = crop( ow, c, Firebloom.Seed.class );
		Crop rec = Farming.cropAt( ow, c );
		assertNotNull( rec );
		assertSame( rec, m.crop );
		rec.growth = 123f;

		jump( ow, wx + 900, wy );
		assertEquals( -1, ow.localCell( wx, wy ) );
		jump( ow, wx + 40, wy - 30 );
		int back = ow.localCell( wx, wy );
		assertNotEquals( c, back );
		assertEquals( Terrain.FARMLAND, ow.map[back] );
		FarmCrop shown = Farming.mobAt( ow, back );
		assertNotNull( "the crop is laid back on its cell", shown );
		assertSame( Farming.cropAt( ow, back ), shown.crop );
		assertEquals( 123f, shown.crop.growth, 0f );

		OverworldLevel loaded = saveAndLoad( ow );
		int cell = loaded.localCell( wx, wy );
		Crop again = Farming.cropAt( loaded, cell );
		assertNotNull( again );
		assertEquals( Firebloom.Seed.class, again.seed );
		assertEquals( 123f, again.growth, 0f );
		FarmCrop after = Farming.mobAt( loaded, cell );
		assertNotNull( after );
		assertSame( "the save's crop takes the record over", again, after.crop );
		assertTrue( Farming.owns( loaded, after ) );
	}

	@Test
	public void aCropWithNoRecordLeavesAndARecordWithNoCropIsLaid(){
		int[] at = site( 3, 3 );
		OverworldLevel ow = quiet( at );
		int c = at[2] + 1 + W;
		FarmCrop m = crop( ow, c, Firebloom.Seed.class );
		Crop rec = m.crop;
		//a stray: another sowing's crop on the cell
		FarmCrop stray = new FarmCrop();
		stray.crop = new Crop( Sungrass.Seed.class, 5f, 0 );
		stray.pos = c;
		assertFalse( Farming.owns( ow, stray ) );
		//the crop gone from the window: laid again from the record
		ow.mobs.remove( m );
		Farming.fields( ow ).materialize( ow );
		FarmCrop laid = Farming.mobAt( ow, c );
		assertNotNull( laid );
		assertSame( rec, laid.crop );
	}

	@Test
	public void takingTheFarmlandAwayUprootsTheCrop(){
		int[] at = site( 3, 3 );
		OverworldLevel ow = quiet( at );
		int c = at[2] + 1 + W;
		crop( ow, c, Firebloom.Seed.class );
		stand( ow, c + 1 );
		assertEquals( Homestead.Reason.OK, Homestead.remove( hero, c ) );
		assertNull( Farming.cropAt( ow, c ) );
		assertNull( Farming.mobAt( ow, c ) );
	}

	// ------------------------------------------------------------------ growing

	@Test
	public void theSeasonsGrowTheCropAndGlassKeepsSummerAllYear(){
		int[] at = site( 5, 3 );
		OverworldLevel ow = quiet( at );
		int open = at[2] + 1 + W, glass = at[2] + 3 + W;
		Crop a = crop( ow, open, Firebloom.Seed.class ).crop;
		Crop g = crop( ow, glass, Firebloom.Seed.class ).crop;
		build( ow, glass, "roof_glass" );
		assertTrue( Farming.greenhouse( ow, glass ) );
		assertFalse( Farming.greenhouse( ow, open ) );

		float[] expect = { 12.5f, 10f, 7.5f, 0f };
		int[] days = { SPRING_DAY, SUMMER_DAY, AUTUMN_DAY, WINTER_DAY };
		for (int i = 0; i < days.length; i++){
			setDay( days[i] );
			for (Crop c : new Crop[]{ a, g }){
				c.growth = 0;
				c.day = days[i];
				c.clock = FarmCrop.clock() - 10;
			}
			assertEquals( Farming.Outcome.GROWING, Farming.tend( ow, open, a, true ) );
			assertEquals( Farming.Outcome.GROWING, Farming.tend( ow, glass, g, true ) );
			assertEquals( season() + " in the open", expect[i], a.growth, 0.001f );
			assertEquals( season() + " under glass", 10f, g.growth, 0.001f );
		}
	}

	@Test
	public void frostKillsACropInTheOpenNotUnderGlass(){
		int[] at = site( 5, 3 );
		OverworldLevel ow = quiet( at );
		int open = at[2] + 1 + W, glass = at[2] + 3 + W;
		Crop a = crop( ow, open, Firebloom.Seed.class ).crop;
		Crop g = crop( ow, glass, Firebloom.Seed.class ).crop;
		build( ow, glass, "roof_glass" );

		//the hero there: the cell's own temperature
		setDay( WINTER_DAY );
		a.day = g.day = WINTER_DAY;
		a.clock = g.clock = FarmCrop.clock() - 1;
		ClimateManager.debugTempOverride = 0f;
		assertEquals( "above -2C it lives", Farming.Outcome.GROWING, Farming.tend( ow, open, a, true ) );
		a.clock = g.clock = FarmCrop.clock() - 1;
		ClimateManager.debugTempOverride = -5f;
		assertEquals( Farming.Outcome.FROSTED, Farming.tend( ow, open, a, true ) );
		assertEquals( Farming.Outcome.GROWING, Farming.tend( ow, glass, g, true ) );

		//away: any winter day that went by kills it, whatever the air when he is back; under glass
		//it grew all the while (a rotberry's 6000 turns ripen in the three days)
		ClimateManager.debugTempOverride = 15f;
		Crop b = new Crop( Firebloom.Seed.class, FarmCrop.clock(), WINTER_DAY - 3 );
		Crop h = new Crop( Rotberry.Seed.class, FarmCrop.clock(), WINTER_DAY - 3 );
		advance( 3 * 2500f );
		assertEquals( Farming.Outcome.FROSTED, Farming.tend( ow, open, b, false ) );
		assertEquals( Farming.Outcome.GROWING, Farming.tend( ow, glass, h, false ) );
		assertTrue( h.ripe() );
		//...and a spell away within autumn does not
		Crop c = new Crop( Firebloom.Seed.class, FarmCrop.clock(), AUTUMN_DAY - 10 );
		setDay( AUTUMN_DAY );
		advance( 10 * 2500f );
		assertNotEquals( Farming.Outcome.FROSTED, Farming.tend( ow, open, c, false ) );
	}

	@Test
	public void aRipeCropWaitsAndThenSeedsUntilItIsSpent(){
		int[] at = site( 5, 5 );
		OverworldLevel ow = quiet( at );
		int cell = at[2] + 2 + 2 * W;
		Crop c = crop( ow, cell, Firebloom.Seed.class ).crop;
		c.clock = FarmCrop.clock() - 10;
		c.growth = 495;
		assertEquals( Farming.Outcome.GROWING, Farming.tend( ow, cell, c, true ) );
		assertTrue( c.ripe() );
		assertEquals( 0, c.seeds );

		//cycle after cycle: a seed beside it, or it withers - the same for the same crop every time
		Crop twin = new Crop( c.seed, c.planted, c.day );
		twin.wx = c.wx;
		twin.wy = c.wy;
		twin.cycles = c.cycles;
		twin.growth = c.growth;
		twin.clock = c.clock;
		advance( 5000 );
		Farming.Outcome o = Farming.tend( ow, cell, c, false );
		Farming.Outcome o2 = Farming.tend( ow, cell, twin, false );
		assertEquals( o, o2 );
		assertEquals( c.seeds, twin.seeds );
		assertNotEquals( "ten cycles: spent or withered", Farming.Outcome.GROWING, o );
		int seeds = 0;
		for (Heap h : ow.heaps.valueList()) for (Item i : h.items) if (i instanceof Firebloom.Seed) seeds += i.quantity();
		assertEquals( c.seeds + twin.seeds, seeds );
		assertTrue( c.seeds <= FarmRules.maxSeeds( Firebloom.class ) );
	}

	@Test
	public void theSickleReapsFoodOrSeed(){
		assertYield( BlandfruitBush.class, BlandfruitBush.Seed.class, Blandfruit.class, 1, 2 );
		assertYield( Cornwheat.class, Cornwheat.Seed.class, Cornwheatshaft.class, 2, 3 );
		assertYield( Apricobush.class, Apricobush.Seed.class, Aprico.class, 1, 1 );
		assertYield( Gobgrape.class, Gobgrape.Seed.class, Grape.class, 2, 2 );
		assertYield( Eggbloom.class, Eggbloom.Seed.class, PlantEgg.class, 1, 1 );
		assertYield( Tomatobush.class, Tomatobush.Seed.class, FusedTomato.class, 1, 2 );
		assertYield( Firebloom.class, Firebloom.Seed.class, Firebloom.Seed.class, 1, 1 );
		assertTrue( FarmRules.food( Cornwheat.class ) );
		assertFalse( FarmRules.food( Firebloom.class ) );

		int[] at = site( 3, 3 );
		OverworldLevel ow = quiet( at );
		int cell = at[2] + 1 + W;
		FarmCrop m = crop( ow, cell, Cornwheat.Seed.class );
		m.crop.cycles = 1;
		stand( ow, cell + 1 );
		give( new Sickle() );
		assertTrue( Farming.hasSickle( hero ) );
		Farming.harvest( hero, m );
		assertNull( Farming.cropAt( ow, cell ) );
		Heap h = ow.heaps.get( cell );
		assertNotNull( "the yield is left on the crop's cell", h );
		assertTrue( h.peek() instanceof Cornwheatshaft );
	}

	private static void assertYield( Class<? extends xyz.gabriwar.warpedpixeldungeon.plants.Plant> plant,
			Class<? extends xyz.gabriwar.warpedpixeldungeon.plants.Plant.Seed> seed, Class<? extends Item> what, int min, int max ){
		for (int i = 0; i < 20; i++){
			List<Item> out = FarmRules.harvest( plant, seed );
			assertEquals( 1, out.size() );
			assertEquals( what, out.get( 0 ).getClass() );
			assertTrue( plant.getSimpleName(), out.get( 0 ).quantity() >= min && out.get( 0 ).quantity() <= max );
		}
	}

	@Test
	public void plantFoodHurriesAGrowingCrop(){
		int[] at = site( 3, 3 );
		OverworldLevel ow = quiet( at );
		int cell = at[2] + 1 + W;
		Crop c = crop( ow, cell, Firebloom.Seed.class ).crop;
		assertTrue( Farming.feed( ow, cell, c ) );
		assertEquals( 125f, c.growth, 0.01f );
		assertEquals( 125f, c.fed, 0.01f );
		c.cycles = 1;
		assertFalse( "a ripe crop needs no food", Farming.feed( ow, cell, c ) );
	}

	// ------------------------------------------------------------------ the compost bin

	@Test
	public void theCompostBinTurnsFoodIntoPlantFood(){
		CompostBin bin = new CompostBin();
		bin.add( 12 );
		assertEquals( 2, bin.loose );
		assertEquals( 2, bin.rotting.size() );
		advance( CompostBin.ROT_TURNS - 1 );
		bin.settle();
		assertEquals( 0, bin.ready );
		assertEquals( 1, bin.nextReadyIn() );
		advance( 1 );
		bin.settle();
		assertEquals( 2, bin.ready );
		//away for days: what filled since is ready when he is back
		bin.add( 3 );
		assertEquals( 1, bin.rotting.size() );
		advance( 10 * 2500 );
		bin.settle();
		assertEquals( 3, bin.ready );
		assertTrue( CompostBin.compostable( new Cornwheatshaft() ) );
		assertTrue( CompostBin.compostable( new Firebloom.Seed() ) );
		assertFalse( CompostBin.compostable( new Sickle() ) );

		//taken down, what is ready is left on its cell
		int[] at = site( 3, 3 );
		OverworldLevel ow = quiet( at );
		int cell = at[2] + 1 + W;
		build( ow, cell, "compost_bin" );
		CompostBin built = null;
		for (Mob m : ow.mobs) if (m instanceof CompostBin) built = (CompostBin) m;
		assertNotNull( built );
		built.ready = 4;
		stand( ow, cell + 1 );
		assertEquals( Homestead.Reason.OK, Homestead.remove( hero, cell ) );
		Heap h = ow.heaps.get( cell );
		assertNotNull( h );
		assertTrue( h.peek() instanceof Fertilizer );
		assertEquals( 4, h.peek().quantity() );
	}

	// ------------------------------------------------------------------ fences and pests

	@Test
	public void aFenceShutsAFieldIn(){
		int[] at = site( 7, 7 );
		OverworldLevel ow = quiet( at );
		int o = at[2];
		int inside = o + 3 + 3 * W;
		assertFalse( Enclosure.closed( ow, inside, false ) );
		ArrayList<Integer> fence = ring( ow, o, 7, 7, "fence" );
		assertTrue( Enclosure.closed( ow, inside, false ) );
		assertEquals( 25, Enclosure.area( ow, inside, false ).size() );
		//a gate shut is part of it; open, it is a way out - unless held shut for a penned beast
		int gate = o + 3 + 6 * W;
		ow.map[gate] = Terrain.FENCE_GATE_OPEN;
		ow.buildFlagMaps();
		assertFalse( Enclosure.closed( ow, inside, false ) );
		assertTrue( Enclosure.closed( ow, inside, true ) );
		ow.map[gate] = Terrain.FENCE_GATE;
		ow.buildFlagMaps();
		assertTrue( Enclosure.closed( ow, inside, false ) );
		//a gap in a corner: the beasts walk the diagonals
		stand( ow, inside );
		assertEquals( Homestead.Reason.OK, Homestead.remove( hero, fence.get( 0 ) ) );
		assertFalse( Enclosure.closed( ow, inside, false ) );

		//too big a ring closes nothing: on a stretch of the window cleared to grass, 20x20 inside a
		//fence is a pen, 21x21 is not
		int mid = W / 2 - 12 + (H / 2 - 12) * W;
		for (int y = -1; y <= 24; y++){
			for (int x = -1; x <= 24; x++) ow.map[mid + x + y * W] = Terrain.GRASS;
		}
		ow.buildFlagMaps();
		ring( ow, mid, 22, 22, "fence" );
		assertTrue( "400 cells is a pen", Enclosure.closed( ow, mid + 11 + 11 * W, false ) );
		for (int y = -1; y <= 24; y++) for (int x = -1; x <= 24; x++) ow.map[mid + x + y * W] = Terrain.GRASS;
		ow.buildFlagMaps();
		ring( ow, mid, 23, 23, "fence" );
		assertFalse( "441 cells is more than a pen", Enclosure.closed( ow, mid + 11 + 11 * W, false ) );
	}

	@Test
	public void grazersEatAnOpenCropAtNightButNotAFencedOne(){
		int[] at = site( 9, 14 );
		OverworldLevel ow = quiet( at );
		int o = at[2];
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int open = o + 1 + 2 * W;
		crop( ow, open, Firebloom.Seed.class );
		//a fenced crop on the far side
		ring( ow, o + 5, 3, 3, "fence" );
		int fenced = o + 6 + W;
		crop( ow, fenced, Firebloom.Seed.class );
		Bunny bunny = put( ow, new Bunny(), o + 1 + 9 * W );
		//the hero away beyond a grazer's shyness, seeing it all
		stand( ow, o + 8 + 13 * W );
		actorsOn( ow );

		Farming.fields( ow ).worldTurn( ow );
		Grazing g = bunny.buff( Grazing.class );
		assertNotNull( "the bunny sets off for the open crop", g );
		assertEquals( ow.worldX + open % W, g.wx );
		for (int i = 0; i < 40 && Farming.cropAt( ow, open ) != null; i++) Animals.act( bunny );
		assertNull( "eaten", Farming.cropAt( ow, open ) );
		assertNull( Farming.mobAt( ow, open ) );
		assertNull( bunny.buff( Grazing.class ) );

		//the fenced one is nobody's dinner
		FarmSlice f = Farming.fields( ow );
		for (int i = 0; i < 30; i++){
			advanceNow( Pests.EVERY );
			f.worldTurn( ow );
			assertNull( bunny.buff( Grazing.class ) );
			Animals.act( bunny );
		}
		assertNotNull( Farming.cropAt( ow, fenced ) );

		//by day nobody comes
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		int another = o + 2 + 2 * W;
		crop( ow, another, Firebloom.Seed.class );
		f.worldTurn( ow );
		assertNull( bunny.buff( Grazing.class ) );
	}

	@Test
	public void awayThePestsTakeOneOpenCropANightAndTheFrostTheRest(){
		String first = null;
		for (int run = 0; run < 2; run++){
			HomesteadWorld.reset();
			setDay( SUMMER_DAY );
			int[] at = site( 9, 5 );
			OverworldLevel ow = quiet( at );
			int o = at[2];
			ArrayList<Integer> open = new ArrayList<>();
			//rotberries: their 6000-turn cycle outlasts the three days away
			for (int x = 0; x < 6; x++){
				open.add( o + x + 4 * W );
				crop( ow, o + x + 4 * W, Rotberry.Seed.class );
			}
			ring( ow, o + 6, 3, 3, "fence" );
			int fenced = o + 7 + W;
			crop( ow, fenced, Rotberry.Seed.class );
			int hx = ow.worldX + (o + 3) % W, hy = ow.worldY + (o + 3) / W;
			HomesteadWorld.Home home = new HomesteadWorld.Home( 97, hx, hy, "Test", SUMMER_DAY );
			HomesteadWorld.addHome( home );
			assertFalse( FarmSettler.pending( ow, hx, hy ) );

			//three days and nights away in summer
			setDay( SUMMER_DAY + 3 );
			advance( 3 * 2500f );
			assertTrue( FarmSettler.pending( ow, hx, hy ) );
			assertTrue( Farming.waiting( ow, Farming.cropAt( ow, open.get( 0 ) ) ) );
			List<String> lines = new FarmSettler().settle( home, 3 );
			int left = 0;
			StringBuilder which = new StringBuilder();
			for (int c : open){
				if (Farming.cropAt( ow, c ) != null) left++;
				else which.append( c - o ).append( ',' );
			}
			assertEquals( "one a night", 3, left );
			assertNotNull( "the fenced one stands", Farming.cropAt( ow, fenced ) );
			assertNotNull( lines );
			assertFalse( lines.isEmpty() );
			if (first == null) first = which.toString();
			else assertEquals( "the same nights take the same crops", first, which.toString() );
		}

		//into the winter: the frost has every crop in the open; one under glass behind a fence lives
		HomesteadWorld.reset();
		int lastOfAutumn = 271;
		setDay( lastOfAutumn );
		int[] at = site( 9, 5 );
		OverworldLevel ow = quiet( at );
		int o = at[2];
		for (int x = 0; x < 4; x++) crop( ow, o + x + 4 * W, Rotberry.Seed.class );
		ring( ow, o + 5, 3, 3, "fence" );
		int glass = o + 6 + W;
		crop( ow, glass, Rotberry.Seed.class );
		build( ow, glass, "roof_glass" );
		HomesteadWorld.Home home = new HomesteadWorld.Home( 97, ow.worldX + (o + 3) % W, ow.worldY + (o + 3) / W, "Test", lastOfAutumn );
		HomesteadWorld.addHome( home );
		setDay( lastOfAutumn + 3 );
		advance( 3 * 2500f );
		List<String> lines = new FarmSettler().settle( home, 3 );
		for (int x = 0; x < 4; x++) assertNull( Farming.cropAt( ow, o + x + 4 * W ) );
		assertNotNull( Farming.cropAt( ow, glass ) );
		assertTrue( lines.toString(), lines.toString().contains( "frost" ) );
	}

	@Test
	public void roomsWithGlassOverHalfAreGreenhouses(){
		int[] at = site( 7, 6 );
		OverworldLevel ow = quiet( at );
		int o = at[2];
		for (int y = 0; y < 5; y++){
			for (int x = 0; x < 6; x++){
				boolean edge = x == 0 || y == 0 || x == 5 || y == 4;
				build( ow, o + x + y * W, !edge ? "farm_plot" : x == 2 && y == 4 ? "door" : "wall_plank" );
			}
		}
		for (int y = 0; y < 5; y++) for (int x = 0; x < 6; x++) build( ow, o + x + y * W, x < 3 ? "roof_glass" : "roof_plank" );
		int plankRoofed = o + 4 + 2 * W;
		assertEquals( RoofTiles.PLANK, Homestead.roofAt( ow, plankRoofed ) );
		assertNotNull( Rooms.at( ow, plankRoofed ) );
		assertTrue( "half the floor under glass: a greenhouse, every cell of it", Farming.greenhouse( ow, plankRoofed ) );
		for (int y = 0; y < 5; y++) build( ow, o + 1 + y * W, "roof_plank" );
		assertFalse( Farming.greenhouse( ow, plankRoofed ) );
	}

	// ------------------------------------------------------------------ fixes after review

	@Test
	public void aCropTheHeroWalksUpToGrowsThoughHisHomeIsNotSettledYet(){
		int[] at = site( 3, 3 );
		OverworldLevel ow = quiet( at );
		int o = at[2];
		int cell = o + 1 + W;
		//rotberries: their 6000-turn cycle outlasts the two days away
		crop( ow, cell, Rotberry.Seed.class );
		Crop c = Farming.cropAt( ow, cell );
		//the hearthstone 15 cells off: the crop is in its fields, the hero at it is not at the home
		int hx = c.wx + 15, hy = c.wy;
		HomesteadWorld.addHome( new HomesteadWorld.Home( 97, hx, hy, "Test", SUMMER_DAY ) );
		setDay( SUMMER_DAY + 2 );
		advance( 2 * 2500f );

		//the hero far off: the crop waits for the settling
		stand( ow, (o % W) < W / 2 ? o + 60 : o - 60 );
		assertTrue( Farming.waiting( ow, c ) );
		//three cells from it (and eighteen from the hearthstone): it catches up by itself
		stand( ow, cell + 3 * W );
		assertTrue( ow.distance( hero.pos, ow.localCell( hx, hy ) ) > AwaySettlers.REACH );
		assertFalse( Farming.waiting( ow, c ) );
		assertEquals( Farming.Outcome.GROWING, Farming.tend( ow, cell, c, true ) );
		assertTrue( "it grew through the days away", c.growth >= 4999f );
		assertEquals( FarmCrop.clock(), c.clock, 0.01f );
	}

	@Test
	public void awayTheWinterSparesACropInARoom(){
		int lastOfAutumn = 271;
		setDay( lastOfAutumn );
		int[] at = site( 9, 6 );
		OverworldLevel ow = quiet( at );
		int o = at[2];
		//a plank room roofed over, a crop in it; another in the open beside it
		for (int y = 0; y < 5; y++){
			for (int x = 0; x < 5; x++){
				boolean edge = x == 0 || y == 0 || x == 4 || y == 4;
				build( ow, o + x + y * W, !edge ? "farm_plot" : x == 2 && y == 4 ? "door" : "wall_plank" );
			}
		}
		for (int y = 0; y < 5; y++) for (int x = 0; x < 5; x++) build( ow, o + x + y * W, "roof_plank" );
		int inside = o + 2 + 2 * W, outside = o + 7 + 2 * W;
		assertNotNull( Rooms.at( ow, inside ) );
		Farming.plant( ow, inside, Rotberry.Seed.class );
		crop( ow, outside, Rotberry.Seed.class );
		setDay( lastOfAutumn + 3 );
		advance( 3 * 2500f );
		assertEquals( Farming.Outcome.FROSTED, Farming.tend( ow, outside, Farming.cropAt( ow, outside ), false ) );
		assertEquals( "walls and a roof keep the winter off", Farming.Outcome.GROWING,
				Farming.tend( ow, inside, Farming.cropAt( ow, inside ), false ) );
	}

	@Test
	public void theCompostBinTakesProduceAndRawMeatAndAsksHowMany(){
		assertTrue( CompostBin.compostable( new Firebloom.Seed() ) );
		assertTrue( CompostBin.compostable( new xyz.gabriwar.warpedpixeldungeon.items.food.Blueberry() ) );
		assertTrue( CompostBin.compostable( new Blandfruit() ) );
		assertTrue( CompostBin.compostable( new Grape() ) );
		assertTrue( CompostBin.compostable( new xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat() ) );
		assertTrue( CompostBin.compostable( new xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat() ) );
		assertTrue( CompostBin.compostable( new xyz.gabriwar.warpedpixeldungeon.items.food.Nut() ) );
		assertFalse( "a lasting gift", CompostBin.compostable( new xyz.gabriwar.warpedpixeldungeon.items.food.PotionOfConstitution() ) );
		assertFalse( CompostBin.compostable( new xyz.gabriwar.warpedpixeldungeon.items.food.GoldenNut() ) );
		assertFalse( CompostBin.compostable( new xyz.gabriwar.warpedpixeldungeon.items.food.SupplyRation() ) );
		assertFalse( CompostBin.compostable( new xyz.gabriwar.warpedpixeldungeon.items.food.Pasty() ) );
		assertFalse( CompostBin.compostable( new xyz.gabriwar.warpedpixeldungeon.items.food.StewedMeat() ) );
		Blandfruit brewed = new Blandfruit();
		brewed.potionAttrib = new xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing();
		assertFalse( "a blandfruit brewed with a seed is a potion now", CompostBin.compostable( brewed ) );

		int[] at = site( 3, 3 );
		OverworldLevel ow = quiet( at );
		stand( ow, at[2] + 1 + W );
		Firebloom.Seed seeds = new Firebloom.Seed();
		seeds.quantity( 12 );
		give( seeds );
		CompostBin bin = new CompostBin();
		bin.put( hero, seeds, CompostBin.BATCH );
		assertEquals( "a batch of the stack, the rest kept", 7, seeds.quantity() );
		assertTrue( hero.belongings.contains( seeds ) );
		assertEquals( 1, bin.rotting.size() );
		bin.put( hero, seeds, 1 );
		assertEquals( 6, seeds.quantity() );
		assertEquals( 1, bin.loose );
		bin.put( hero, seeds, 99 );
		assertFalse( "all of it, no more", hero.belongings.contains( seeds ) );
		assertEquals( 2, bin.loose );
		assertEquals( 2, bin.rotting.size() );
	}

	@Test
	public void aGrazerOffToACropIsSparedTheNightsCull(){
		int[] at = site( 3, 3 );
		OverworldLevel ow = quiet( at );
		int o = at[2];
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		Bunny sent = put( ow, new Bunny(), o + W );
		Bunny idle = put( ow, new Bunny(), o + 2 + W );
		xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.affect( sent, Grazing.class );
		//the hero far off, seeing nothing
		stand( ow, (o % W) < W / 2 ? o + 60 : o - 60 );
		java.util.Arrays.fill( ow.heroFOV, false );
		xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna.cull( ow );
		assertTrue( ow.mobs.contains( sent ) );
		assertFalse( ow.mobs.contains( idle ) );
	}
}

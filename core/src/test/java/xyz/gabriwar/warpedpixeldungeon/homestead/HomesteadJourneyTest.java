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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Comfy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Animals;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Roping;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildPlan;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildShapes;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Farming;
import xyz.gabriwar.warpedpixeldungeon.homestead.rooms.HomeBed;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Hearth;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.HomesteadStation;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Kiln;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.MasonsBench;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipes;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Workbench;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.SpikeTrap;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.ThreatSlice;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Hearthstone;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.TravelSlice;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Waystone;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Clay;
import xyz.gabriwar.warpedpixeldungeon.items.materials.CutStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GlassPane;
import xyz.gabriwar.warpedpixeldungeon.items.materials.IronFittings;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Leather;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Sand;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The homesteads end to end, as a player goes through them in one window of the surface and only
 * through the calls the game's own screens make: the stations put up from the build menu turn
 * raw materials into planks, cut stone and glass; a stone house goes up from those through the
 * build bar's shapes (a wall outline, a floor area, a slate roof laid from its walls in), with a
 * barred door and a window; the inspector calls it a sealed room; a hearth lit in it warms it on a
 * winter night and makes the hero comfy; a fur bed in it is never found; a hearthstone makes it a
 * home; all of it outlasts a save and the window moving away and back; and a week away is settled
 * once, on the way back, for one chest at most; and two goats led on ropes into a fenced pen stay
 * there through a save and the world moving; and the town's slice, its waystone and a house beside
 * it, is read back from its save as a game loads it, with no level on show.
 */
public class HomesteadJourneyTest extends FarmTestBase {

	private float wind;

	@After
	public void restoreWind(){
		ClimateManager.debugWindOverride = wind;
	}

	// ------------------------------------------------------------------ helpers

	private void give( Class<? extends Item> cls, int n ){
		Item i = Reflection.newInstance( cls );
		i.quantity( n );
		give( i );
	}

	private int count( Class<? extends Item> cls ){
		return Material.count( hero, cls );
	}

	private static BuildPiece piece( String id ){
		BuildPiece p = BuildCatalog.byId( id );
		assertNotNull( id, p );
		return p;
	}

	//one piece from the build bar's single mode: placed as a tap places it
	private void place( int cell, String id ){
		assertEquals( id + " at " + cell, Homestead.Reason.OK, Homestead.place( hero, cell, piece( id ) ) );
	}

	//a shape from the build bar's line and area modes: planned whole, then built a piece at a time
	//in the plan's order from where the hero planned it (BuildPlacer's job), every cell taken
	private int buildShape( String id, int[] cells ){
		BuildPiece p = piece( id );
		BuildPlan plan = BuildPlan.of( hero, p, cells );
		assertEquals( id + ": every cell of the shape takes it", cells.length, plan.valid );
		assertEquals( id + ": and the materials stretch to all of them", plan.valid, plan.affordable );
		int built = 0;
		for (int c : plan.buildableCells()){
			assertEquals( id + " at " + c, Homestead.Reason.OK, Homestead.placeUnseen( hero, c, p ) );
			built++;
		}
		return built;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Mob> T mobAt( OverworldLevel ow, int cell, Class<T> cls ){
		for (Mob m : ow.mobs) if (m.pos == cell && cls.isInstance( m )) return (T) m;
		return null;
	}

	//a station's batches run: the global clock moves on and the station catches up, as its act does
	private static void work( OverworldLevel ow, HomesteadStation s, float turns ){
		advance( turns );
		s.catchUp( ow );
	}

	/** The house: a 5x5 stone ring with a window north and a barred door south, a plank floor
	 *  inside and a slate roof over it all, built from the materials the stations made. The cells:
	 *  {top-left, door, window, middle of the floor}. */
	private int[] raiseTheHouse( OverworldLevel ow, int o ){
		int door = o + 2 + 4 * W, window = o + 2, middle = o + 2 + 2 * W;
		stand( ow, middle );

		//the walls: the area mode's hollow outline, door and window gaps left out of it
		int[] ring = BuildShapes.outline( W, o, o + 4 + 4 * W );
		assertEquals( 16, ring.length );
		assertTrue( BuildShapes.areaIsOutline( piece( "wall_stone" ) ) );
		ArrayList<Integer> walls = new ArrayList<>();
		for (int c : ring) if (c != door && c != window) walls.add( c );
		int[] wallCells = new int[walls.size()];
		for (int i = 0; i < wallCells.length; i++) wallCells[i] = walls.get( i );
		assertEquals( 14, buildShape( "wall_stone", wallCells ) );
		place( window, "window_stone" );
		place( door, "door_barred" );

		//the floor: an area filled
		assertFalse( BuildShapes.areaIsOutline( piece( "floor_plank" ) ) );
		assertEquals( 9, buildShape( "floor_plank", BuildShapes.rect( W, o + 1 + W, o + 3 + 3 * W ) ) );

		//the roof: an area over everything, held up from the walls inward
		assertEquals( 25, buildShape( "roof_slate", BuildShapes.rect( W, o, o + 4 + 4 * W ) ) );
		return new int[]{ o, door, window, middle };
	}

	// ------------------------------------------------------------------ the journey

	@Test
	public void fromRawMaterialsToAWarmSealedHomeThatOutlastsASaveAndTheWorldMoving() throws Exception {
		wind = ClimateManager.debugWindOverride;
		int[] at = site( 13, 8 );
		OverworldLevel ow = quiet( at );
		int o = at[2];

		//what a new run gathers and buys: felled logs, quarried stone, clay and sand dug from a bank
		//and a beach, the carpenter's planks and the forge's fittings, the tanner's leather
		give( new BuildersSatchel() );
		give( Log.class, 12 );
		give( RoughStone.class, 130 );
		give( Clay.class, 6 );
		give( Sand.class, 4 );
		give( Plank.class, 16 );
		give( IronFittings.class, 2 );
		give( Leather.class, 3 );

		// ---- the stations, east of where the house goes, each from the build menu
		int yard = o + 10 + 3 * W;
		stand( ow, yard );
		place( yard - 2 - 2 * W, "workbench" );
		place( yard + 2 - 2 * W, "masons_bench" );
		place( yard + 2 * W, "kiln" );
		Workbench bench = mobAt( ow, yard - 2 - 2 * W, Workbench.class );
		MasonsBench mason = mobAt( ow, yard + 2 - 2 * W, MasonsBench.class );
		Kiln kiln = mobAt( ow, yard + 2 * W, Kiln.class );
		assertNotNull( bench );
		assertNotNull( mason );
		assertNotNull( kiln );
		assertTrue( "the build menu's work counts as building", HomesteadWorld.homes().isEmpty() );

		//planks from logs
		assertEquals( HomesteadStation.Craft.OK, bench.queue( hero, StationRecipes.byId( "planks_oak" ), 4, null ) );
		work( ow, bench, 40 );
		assertEquals( 1, bench.takeAll( hero ) );
		assertEquals( "the workbench and the mason's bench took 8, four batches made 12", 16 - 8 + 12, count( Plank.class ) );

		//cut stone for the slate, two full jobs (twenty batches at most each) and a short one
		assertEquals( HomesteadStation.Craft.OK, mason.queue( hero, StationRecipes.byId( "cut_stone" ), 20, null ) );
		assertEquals( HomesteadStation.Craft.OK, mason.queue( hero, StationRecipes.byId( "cut_stone" ), 7, null ) );
		work( ow, mason, 270 );
		mason.takeAll( hero );
		assertEquals( 27, count( CutStone.class ) );

		//glass in the lit kiln: logs in the fuel slot, the fire lit by hand
		assertEquals( HomesteadStation.Craft.OK, kiln.queue( hero, StationRecipes.byId( "glass" ), 1, null ) );
		work( ow, kiln, 60 );
		assertTrue( "no glass while the kiln is cold", kiln.output().isEmpty() );
		assertTrue( kiln.addFuel( hero, new Log().quantity( 2 ) ) > 0 );
		assertTrue( kiln.light() );
		work( ow, kiln, 60 );
		kiln.takeAll( hero );
		assertEquals( 1, count( GlassPane.class ) );

		// ---- the house, through the build bar's shapes
		int[] house = raiseTheHouse( ow, o );
		int door = house[1], middle = house[3];
		assertEquals( Terrain.BUILT_DOOR_BARRED, ow.map[door] );
		assertEquals( Terrain.BUILT_WINDOW_STONE, ow.map[house[2]] );
		assertEquals( 0, count( GlassPane.class ) );
		assertEquals( "two cut stone left over the roof", 2, count( CutStone.class ) );

		Rooms.Room room = Rooms.at( ow, middle );
		assertNotNull( "a sealed stone room: " + Rooms.check( ow, middle ), room );
		assertEquals( 9, room.size() );
		assertTrue( room.sealed() );
		String inspected = Rooms.describe( ow, middle );
		assertTrue( inspected, inspected.contains( Messages.get( Rooms.class, "sealed" ) ) );
		assertFalse( inspected, inspected.contains( Messages.NO_TEXT_FOUND ) );

		// ---- furnished: a hearth and a fur bed on the floor
		place( middle - 1 - W, "hearth_stone" );
		place( middle + 1 - W, "bed_fur" );
		Hearth hearth = mobAt( ow, middle - 1 - W, Hearth.class );
		HomeBed bed = mobAt( ow, middle + 1 - W, HomeBed.class );
		assertNotNull( hearth );
		assertNotNull( bed );
		assertEquals( HomeBed.Kind.FUR, bed.kind );
		assertNotNull( "a station stands on its floor and leaves it a room", Rooms.at( ow, middle ) );
		assertTrue( "nothing finds a sleeper in a sealed room", bed.safe() );

		// ---- a winter night: the hearth warms the room and the hero
		setDay( WINTER_DAY );
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		ClimateManager.debugTempOverride = -15f;
		ClimateManager.debugWindOverride = 0f;
		float cold = TileTemperature.tileTemp( middle );
		assertTrue( "the walls hold some of the day's warmth: " + cold, cold > -15f );
		assertTrue( hearth.addFuel( hero, new Log().quantity( 2 ) ) > 0 );
		assertTrue( hearth.light() );
		assertEquals( Hearth.ROOM_HEAT, Rooms.at( ow, middle ).heat(), 0.001f );
		assertEquals( cold + Hearth.ROOM_HEAT, TileTemperature.tileTemp( middle ), 0.01f );
		assertTrue( Shelter.heated( ow, middle ) );
		assertFalse( "no wind in a room", Shelter.windReaches( ow, middle ) );
		Comfy.check( hero );
		assertNotNull( "comfy by the fire", hero.buff( Comfy.class ) );
		assertTrue( "a lit hearth keeps the wild off", HomeAnchors.near( ow, middle ) );

		// ---- a home: the hearthstone outside the door
		int stone = door + 2 * W;
		stand( ow, door + W );
		place( stone, "hearthstone" );
		assertEquals( 1, HomesteadWorld.homes().size() );
		HomesteadWorld.Home home = HomesteadWorld.homes().get( 0 );
		assertNotNull( mobAt( ow, stone, Hearthstone.class ) );
		assertSame( home, mobAt( ow, stone, Hearthstone.class ).home() );

		// ---- the barred door opened by hand unseals the room; shut, it is sealed again
		assertTrue( Homestead.openByHand( hero, door ) );
		assertEquals( Terrain.BUILT_DOOR_BARRED_OPEN, ow.map[door] );
		assertFalse( "an open door lets the night in", bed.safe() );
		Level.set( door, Terrain.BUILT_DOOR_BARRED, ow );
		assertTrue( bed.safe() );

		// ---- a save and a load: everything as it was
		int pieces = countBuilt( ow );
		OverworldLevel back = saveAndLoad( ow );
		actorsOn( back );
		stand( back, middle );
		assertEquals( pieces, countBuilt( back ) );
		assertNotNull( Rooms.at( back, middle ) );
		assertTrue( Rooms.at( back, middle ).sealed() );
		Hearth hearth2 = mobAt( back, middle - 1 - W, Hearth.class );
		assertNotNull( hearth2 );
		assertTrue( "the fire burns on through the save", hearth2.lit() );
		HomeBed bed2 = mobAt( back, middle + 1 - W, HomeBed.class );
		assertNotNull( bed2 );
		assertEquals( HomeBed.Kind.FUR, bed2.kind );
		assertTrue( bed2.safe() );
		assertNotNull( mobAt( back, stone, Hearthstone.class ) );
		assertEquals( 25, roofs( back, o ) );

		// ---- the window jumps far away and comes back: nothing lost, nothing doubled
		int wx = back.worldX + middle % W, wy = back.worldY + middle / W;
		jump( back, wx + 900, wy - 700 );
		assertEquals( "nothing of the house in a far window", 0, countBuilt( back ) );
		//saved and loaded while it lies parked out of the window, then walked back to
		back = saveAndLoad( back );
		actorsOn( back );
		jump( back, at[0], at[1] );
		int mid = back.localCell( wx, wy );
		int o2 = mid - 2 - 2 * W;
		assertEquals( pieces, countBuilt( back ) );
		assertEquals( 25, roofs( back, o2 ) );
		assertNotNull( Rooms.at( back, mid ) );
		int stations = 0, beds = 0, stones = 0;
		for (Mob m : back.mobs){
			if (m instanceof HomesteadStation) stations++;
			if (m instanceof HomeBed) beds++;
			if (m instanceof Hearthstone) stones++;
		}
		assertEquals( "the hearth, the workbench, the mason's bench and the kiln, once each", 4, stations );
		assertEquals( 1, beds );
		assertEquals( 1, stones );
		assertEquals( 1, HomesteadWorld.homes().size() );
	}

	@Test
	public void aWeekAwayIsSettledOnceOnTheWayBackForOneChestAtMost(){
		int[] at = site( 9, 9 );
		OverworldLevel ow = quiet( at );
		int o = at[2];
		give( new BuildersSatchel() );
		give( RoughStone.class, 80 );
		give( Plank.class, 20 );
		give( IronFittings.class, 2 );
		give( GlassPane.class, 1 );
		give( CutStone.class, 25 );
		int[] house = raiseTheHouse( ow, o );
		int middle = house[3], door = house[1];

		//three chests of gold in the house, the richest first
		StorageChest[] chests = new StorageChest[3];
		int[] spots = { middle - 1 - W, middle + 1 - W, middle - 1 + W };
		int[] gold = { 3000, 400, 200 };
		for (int i = 0; i < chests.length; i++){
			StorageChest c = new StorageChest();
			c.pos = spots[i];
			assertTrue( new Gold( gold[i] ).collect( c.storage ) );
			ow.mobs.add( c );
			chests[i] = c;
		}

		stand( ow, door + W );
		give( RoughStone.class, 10 );
		give( CutStone.class, 2 );
		place( door + 2 * W, "hearthstone" );
		HomesteadWorld.Home home = HomesteadWorld.homes().get( 0 );
		int left = home.lastVisitTurn;
		assertEquals( "visited as it went up", WorldClock.turn(), left );

		//a week passes elsewhere; the hero walks back in
		Dungeon.cycleTurn += 7 * DayNightCycle.FULL_CYCLE;
		advance( 7 * DayNightCycle.FULL_CYCLE );
		stand( ow, middle );
		int entries = home.ledger().size();
		AwaySettlers.visit( ow );
		assertTrue( "the week is settled and the visit noted", home.lastVisitTurn > left );
		int emptied = 0;
		for (StorageChest c : chests) if (c.storage.items.isEmpty()) emptied++;
		assertTrue( "one chest at most: " + emptied, emptied <= 1 );
		if (emptied == 1) assertTrue( "the richest, if any", chests[0].storage.items.isEmpty() );
		for (HomesteadWorld.Entry e : home.ledger()) assertFalse( e.text, e.text.contains( Messages.NO_TEXT_FOUND ) );

		//and only once: walking about at home settles nothing more
		int after = home.ledger().size();
		assertTrue( after >= entries );
		AwaySettlers.visit( ow );
		assertEquals( after, home.ledger().size() );
	}

	@Test
	public void twoGoatsLedOnRopesIntoAFencedPenStayThereForGood() throws Exception {
		int[] at = site( 9, 12 );
		OverworldLevel ow = quiet( at );
		int o = at[2];
		give( new BuildersSatchel() );
		give( Plank.class, 30 );
		give( new Rope().quantity( 4 ) );

		//the pen: the area mode's hollow outline of fence, a gate in the gap south
		int gate = o + 3 + 6 * W, middle = o + 3 + 3 * W;
		stand( ow, middle );
		assertTrue( BuildShapes.areaIsOutline( piece( "fence" ) ) );
		ArrayList<Integer> sides = new ArrayList<>();
		for (int c : BuildShapes.outline( W, o, o + 6 + 6 * W )) if (c != gate) sides.add( c );
		int[] fence = new int[sides.size()];
		for (int i = 0; i < fence.length; i++) fence[i] = sides.get( i );
		assertEquals( 23, buildShape( "fence", fence ) );
		place( gate, "gate" );
		assertEquals( Terrain.FENCE_GATE, ow.map[gate] );

		//two goats grazing south of it, roped from the gate's side
		int outside = gate + 2 * W;
		stand( ow, outside );
		Goat a = put( ow, new Goat(), outside + 3 * W - 1 );
		Goat b = put( ow, new Goat(), outside + 3 * W + 1 );
		assertTrue( Roping.lasso( hero, a.pos ) );
		assertTrue( Roping.lasso( hero, b.pos ) );
		assertEquals( "one for the gate, one on each goat", 1, Material.count( hero, Rope.class ) );

		//he lifts the gate's bar and walks in, the goats after him
		assertTrue( Homestead.openByHand( hero, gate ) );
		for (int c : new int[]{ gate + W, gate, gate - W, gate - 2 * W, middle }){
			hero.pos = c;
			for (int i = 0; i < 3; i++){
				Animals.act( a );
				Animals.act( b );
			}
		}
		HashSet<Integer> pen = new HashSet<>();
		for (int y = 1; y < 6; y++) for (int x = 1; x < 6; x++) pen.add( o + x + y * W );
		assertTrue( "the first goat followed him in: " + a.pos, pen.contains( a.pos ) );
		assertTrue( "the second goat followed him in: " + b.pos, pen.contains( b.pos ) );

		//the ropes off; he walks out and the gate swings shut behind him
		Roping.letGo( hero, a );
		Roping.letGo( hero, b );
		assertEquals( 3, Material.count( hero, Rope.class ) );
		hero.pos = outside;
		Level.set( gate, Terrain.FENCE_GATE, ow );
		Roping.lookAtPen( ow, a, true );
		Roping.lookAtPen( ow, b, true );
		assertTrue( "penned", a.homesteadKept() );
		assertTrue( "penned", b.homesteadKept() );
		for (int i = 0; i < 200; i++){
			Animals.act( a );
			Animals.act( b );
			assertTrue( pen.contains( a.pos ) );
			assertTrue( pen.contains( b.pos ) );
		}

		//through a save and the window moving away and back, still in their pen
		OverworldLevel back = saveAndLoad( ow );
		actorsOn( back );
		stand( back, outside );
		int penned = 0;
		for (Mob m : back.mobs) if (m instanceof Goat && ((Goat) m).homesteadKept() && pen.contains( m.pos )) penned++;
		assertEquals( 2, penned );
		int wx = back.worldX + middle % W, wy = back.worldY + middle / W;
		jump( back, wx + 900, wy );
		jump( back, at[0], at[1] );
		int mid = back.localCell( wx, wy );
		penned = 0;
		for (Mob m : back.mobs){
			if (m instanceof Goat && ((Goat) m).homesteadKept()){
				assertTrue( "in the pen: " + m.pos, Math.abs( m.pos % W - mid % W ) <= 2 && Math.abs( m.pos / W - mid / W ) <= 2 );
				penned++;
			}
		}
		assertEquals( 2, penned );
	}

	@Test
	public void theTownsSliceWithAHomeBesideItIsReadBackWithNoLevelOnShowAndItsWaystoneLaid() throws Exception {
		//the surface round the town's plaza: its waystone laid as the window is, and a house beside it
		int tx = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), ty = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
		OverworldLevel ow = windowAt( tx, ty + 30 );
		ow.mobs.clear();
		//laid as a window is in play: the homesteads' parts put their things down (the waystone)
		jump( ow, tx, ty + 30 );
		int o = openGround( ow, 9, 5 );
		assertTrue( "open ground by the town", o != -1 );
		give( new BuildersSatchel() );
		give( RoughStone.class, 80 );
		give( Plank.class, 20 );
		give( IronFittings.class, 2 );
		give( GlassPane.class, 1 );
		give( CutStone.class, 25 );
		raiseTheHouse( ow, o );
		//and beside it something of every part that keeps its own on the slice: a lit kiln, a crop
		//on its farmland, a spike trap
		build( ow, o + 6 + W, "kiln" );
		Kiln kiln = mobAt( ow, o + 6 + W, Kiln.class );
		kiln.addFuel( hero, new Log().quantity( 2 ) );
		assertTrue( kiln.light() );
		build( ow, o + 8 + W, "farm_plot" );
		Farming.plant( ow, o + 8 + W,
				Sungrass.Seed.class );
		build( ow, o + 7 + 3 * W, ThreatSlice.SPIKE_PIECE );
		int stones = waystones( ow );
		assertEquals( "the town's waystone, once", 1, stones );

		//read back as a game loads it: no level on show while the slice is restored. And as a save
		//from before the stones: no record of them, so the town's is laid again as it loads
		ow.customTerrain = new ArrayList<>();
		for (Mob m : ow.mobs.toArray( new Mob[0] )) if (m instanceof Waystone) ow.mobs.remove( m );
		Bundle b = new Bundle();
		ow.storeInBundle( b );
		assertTrue( b.remove( TravelSlice.class.getName() ) );
		b.put( "version", WarpedPixelDungeon.v3_1_1 + 1 );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		Bundle read = Bundle.read( new ByteArrayInputStream( out.toByteArray() ) );
		Actor.clear();
		Dungeon.level = null;
		OverworldLevel back = new OverworldLevel();
		back.restoreFromBundle( read );
		Dungeon.level = back;
		assertEquals( "laid as it loads, once", 1, waystones( back ) );
		assertNotNull( Rooms.at( back, o + 2 + 2 * W ) );
		Kiln kilnBack = mobAt( back, o + 6 + W, Kiln.class );
		assertNotNull( kilnBack );
		assertTrue( kilnBack.lit() );
		assertNotNull( "the crop is laid again", Farming.mobAt( back, o + 8 + W ) );
		assertTrue( "the spike trap is set again",
				back.traps.get( o + 7 + 3 * W ) instanceof SpikeTrap );
	}

	private static int waystones( OverworldLevel ow ){
		int n = 0;
		for (Mob m : ow.mobs) if (m instanceof Waystone) n++;
		return n;
	}

	// ------------------------------------------------------------------ counting

	private static int countBuilt( OverworldLevel ow ){
		final int[] n = { 0 };
		ow.homestead().forEachBuilt( ow.worldX, ow.worldY, ow.worldX + W, ow.worldY + H, (x, y, b) -> n[0]++ );
		return n[0];
	}

	private static int roofs( OverworldLevel ow, int o ){
		int n = 0;
		for (int y = 0; y < 5; y++) for (int x = 0; x < 5; x++) if (Homestead.roofAt( ow, o + x + y * W ) != -1) n++;
		return n;
	}
}

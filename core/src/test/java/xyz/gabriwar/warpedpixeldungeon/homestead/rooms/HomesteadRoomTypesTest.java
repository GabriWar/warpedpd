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

package xyz.gabriwar.warpedpixeldungeon.homestead.rooms;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WellFed;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Bedroll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.HeatSources;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.RoomTraits;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.homestead.SleepGuards;
import xyz.gabriwar.warpedpixeldungeon.items.PortableChest;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.food.MeatPie;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The homesteads' room types, comfort and beds (homestead/rooms): what makes a bedroom, a kitchen,
 * a storehouse and a shrine, and the workshop and greenhouse asked of their own parts' traits;
 * furniture standing in a room without breaking it, and a shelf's open front; comfort's score; what
 * a bed heals, when its sleeper is found or roused first, its furs and its burning; Well Rested and
 * the shrine's blessing; a storehouse's chests; a kitchen's meals; and a hero in a sealed room
 * hidden from the monsters outside but for its windows. Every window is a real one of a slice,
 * derived from a seed.
 */
public class HomesteadRoomTypesTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int depth, branch, challenges, startDay, turn, deepest;
	private DayNightCycle.Phase override;
	private float shift;
	private boolean shiftHeld;
	private Level savedLevel;
	private Hero savedHero;
	private Hero hero;

	//fixtures of the other parts, told apart by their class's simple name or by RoomFixture
	public static class Hearth extends NPC {}
	public static class Altar extends NPC implements RoomFixture {
		@Override public Kind roomFixture(){ return Kind.ALTAR; }
	}

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
		ClimateManager.debugTempOverride = Float.NaN;
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
		BuildPiece p = BuildCatalog.byId( id );
		assertNotNull( id, p );
		Homestead.build( ow, cell, p, null );
	}

	//a ring of `wall` w x h at o, a door of `door` in the middle of its south side, `floor` (or the
	//bare ground) inside, and `roof` over it all; the door's cell
	private static int house( OverworldLevel ow, int o, int w, int h, String wall, String door, String floor, String roof ){
		int d = -1;
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				int c = o + x + y * W;
				boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
				if (!edge){
					if (floor != null) build( ow, c, floor );
				} else if (x == w / 2 && y == h - 1){
					build( ow, c, door );
					d = c;
				} else build( ow, c, wall );
			}
		}
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++) build( ow, o + x + y * W, roof );
		}
		return d;
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

	//a bed whose sleeper is always found, unless the room keeps him safe: it notes how the night's
	//visitors came instead of setting them down (one set down beckons with a sprite a test has not)
	private static final class Watched extends HomeBed {
		int ambushes;
		boolean roused;

		Watched( Kind kind ){
			this.kind = kind;
		}

		@Override protected float findChance( int turns, boolean fire ){
			return 1f;
		}

		@Override protected void ambush( Hero hero, ArrayList<Mob> found, boolean roused ){
			ambushes++;
			this.roused = roused;
		}
	}

	// ------------------------------------------------------------------ the types

	@Test
	public void eachTypeIsToldFromWhatStandsInTheRoom(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		house( ow, o[0], 7, 5, "wall_stone", "door", "floor_plank", "roof_slate" );
		int a = o[0] + 1 + W, b = o[0] + 2 + W, c = o[0] + 3 + W, d = o[0] + 4 + W, e = o[0] + 5 + W;
		int inside = o[0] + 3 + 2 * W;
		Rooms.Room room = Rooms.at( ow, inside );
		assertNotNull( room );
		assertTrue( "a bare room is no type", RoomTypes.of( ow, room ).isEmpty() );
		assertTrue( Rooms.describe( ow, inside ), Rooms.describe( ow, inside ).startsWith( "A room" ) );

		build( ow, a, "bed_straw" );
		assertEquals( Arrays.asList( RoomTypes.BEDROOM ), RoomTypes.of( ow, room ) );
		String look = Rooms.describe( ow, inside );
		assertTrue( look, look.startsWith( "Bedroom" ) );
		assertTrue( look, look.contains( "Comfort:" ) );
		assertFalse( look, look.contains( "NO TEXT" ) );

		//a hearth (known by its name) makes a kitchen too; the bed still names it
		Mob hearth = put( ow, new Hearth(), b );
		assertTrue( RoomTypes.has( ow, room, RoomTypes.KITCHEN ) );
		assertEquals( RoomTypes.BEDROOM, RoomTypes.of( ow, room ).get( 0 ) );
		ow.mobs.remove( hearth );
		assertFalse( RoomTypes.has( ow, room, RoomTypes.KITCHEN ) );

		//one chest is no storehouse, two are
		put( ow, new StorageChest(), d );
		assertFalse( RoomTypes.has( ow, room, RoomTypes.STOREHOUSE ) );
		put( ow, new StorageChest(), e );
		assertTrue( RoomTypes.has( ow, room, RoomTypes.STOREHOUSE ) );

		//a statue (furniture: the room stays a room) or an altar makes a shrine
		assertFalse( RoomTypes.has( ow, room, RoomTypes.SHRINE ) );
		int statue = o[0] + 5 + 3 * W;
		build( ow, statue, "statue" );
		room = Rooms.at( ow, inside );
		assertNotNull( "a statue inside is no gap", room );
		assertTrue( room.contains( statue ) );
		assertTrue( RoomTypes.has( ow, room, RoomTypes.SHRINE ) );
		Homestead.build( ow, statue, BuildCatalog.byId( "floor_plank" ), null );
		room = Rooms.at( ow, inside );
		assertFalse( RoomTypes.has( ow, room, RoomTypes.SHRINE ) );
		put( ow, new Altar(), o[0] + 1 + 3 * W );
		assertTrue( RoomTypes.has( ow, room, RoomTypes.SHRINE ) );

		//every type's name and line resolve
		for (String type : RoomTypes.OWN){
			assertFalse( type, xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( RoomKinds.class, type ).contains( "NO TEXT" ) );
			assertFalse( type, xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( RoomKinds.class, type + "_line" ).contains( "NO TEXT" ) );
		}
	}

	@Test
	public void theWorkshopAndTheGreenhouseAreTheirOwnPartsTraits(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		house( ow, o[0], 7, 5, "wall_stone", "door", "floor_plank", "roof_slate" );
		int inside = o[0] + 3 + 2 * W;
		Rooms.Room room = Rooms.at( ow, inside );
		//this part names neither: no line of its own for them in a look at the room
		assertFalse( RoomTypes.has( ow, room, RoomTypes.WORKSHOP ) );
		assertFalse( RoomTypes.has( ow, room, RoomTypes.GREENHOUSE ) );
		assertFalse( Arrays.asList( RoomTypes.OWN ).contains( RoomTypes.WORKSHOP ) );
		assertFalse( Arrays.asList( RoomTypes.OWN ).contains( RoomTypes.GREENHOUSE ) );

		//the stations' and the farming's traits label them (stations.StationRooms, farming.FarmRooms)
		RoomTraits.Provider owners = ( level, r, out ) -> {
			out.label( RoomTypes.WORKSHOP, "Workshop" );
			out.label( RoomTypes.GREENHOUSE, "Greenhouse" );
		};
		RoomTraits.register( owners );
		try {
			assertTrue( RoomTypes.has( ow, room, RoomTypes.WORKSHOP ) );
			assertTrue( RoomTypes.has( ow, room, RoomTypes.GREENHOUSE ) );
			assertTrue( RoomTypes.at( ow, inside, RoomTypes.GREENHOUSE ) );
		} finally {
			RoomTraits.unregister( owners );
		}
	}

	// ------------------------------------------------------------------ furniture in a room

	@Test
	public void aStatueOrShelfStandsInARoomButIsNoWallOfIt(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		house( ow, o[0], 7, 5, "wall_stone", "door_barred", "floor_plank", "roof_slate" );
		int inside = o[0] + 3 + 2 * W;
		int shelf = o[0] + 1 + W, statue = o[0] + 5 + W;
		build( ow, shelf, "bookshelf" );
		build( ow, statue, "statue" );
		assertEquals( Terrain.EMPTY_BOOKSHELF, ow.map[shelf] );
		assertEquals( Rooms.Verdict.ROOM, Rooms.check( ow, inside ) );
		Rooms.Room room = Rooms.at( ow, inside );
		assertTrue( room.contains( shelf ) && room.contains( statue ) );
		assertTrue( room.sealed() );
		//asked on the statue itself it is the same room
		assertEquals( room.id, Rooms.at( ow, statue ).id );

		//a roof may go over the furniture
		Homestead.clearRoof( ow, statue );
		stand( ow, inside );
		assertEquals( Homestead.Reason.OK, Homestead.canPlaceSite( hero, statue, BuildCatalog.byId( "roof_slate" ) ) );

		//a statue in the walls is a gap in them, no wall
		int wall = o[0] + 6 + 2 * W;
		Homestead.build( ow, wall, BuildCatalog.byId( "statue" ), null );
		assertEquals( Terrain.STATUE, ow.map[wall] );
		assertNull( Rooms.at( ow, inside ) );

		//a shelf needs open floor in front of it (south): above a wall it would show no face
		int againstSouth = o[0] + 2 + 3 * W, againstNorth = o[0] + 2 + W;
		stand( ow, inside );
		assertEquals( Homestead.Reason.WRONG_GROUND, Homestead.canPlaceSite( hero, againstSouth, BuildCatalog.byId( "bookshelf" ) ) );
		assertEquals( Homestead.Reason.OK, Homestead.canPlaceSite( hero, againstNorth, BuildCatalog.byId( "bookshelf" ) ) );
		//a built shelf keeps its own fog, never blacked out as the inside of a wall
		assertTrue( xyz.gabriwar.warpedpixeldungeon.tiles.HomesteadVisuals.ownFog( ow, shelf ) );
		assertFalse( xyz.gabriwar.warpedpixeldungeon.tiles.HomesteadVisuals.ownFog( ow, inside ) );

		//a built shelf burns into wooden rubble
		assertEquals( Terrain.RUBBLE_WOOD, xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain.rubbleOf( Terrain.EMPTY_BOOKSHELF ) );
		//the land's terrains a piece lays are pictured as the surface's sheet draws them
		assertEquals( xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.WOOL_RUG_TILE,
				xyz.gabriwar.warpedpixeldungeon.tiles.HomesteadVisuals.landFrame( Terrain.WOOL_RUG ) );
		assertEquals( xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.RAISED_STATUE,
				xyz.gabriwar.warpedpixeldungeon.tiles.HomesteadVisuals.landFrame( Terrain.STATUE ) );
		//one with no preview of its own (the stations' alchemy pot) is still made
		assertNotNull( BuildPiece.terrain( "test_pot", xyz.gabriwar.warpedpixeldungeon.homestead.BuildCategory.STATIONS,
				Terrain.ALCHEMY ).done() );
		assertEquals( xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet.RAISED_ALCHEMY_POT,
				xyz.gabriwar.warpedpixeldungeon.tiles.HomesteadVisuals.landFrame( Terrain.ALCHEMY ) );

		//and its pieces resolve
		for (String id : new String[]{ "bed_straw", "bed_wood", "bed_fur", "rug", "bookshelf", "statue" }){
			BuildPiece p = BuildCatalog.byId( id );
			assertFalse( id, p.name().contains( "NO TEXT" ) );
			assertFalse( id, p.desc().contains( "NO TEXT" ) );
		}
	}

	// ------------------------------------------------------------------ comfort

	@Test
	public void comfortScoresSizeWallsFloorsDecorAndHeat(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		int inside = o[0] + 3 + 2 * W;

		//a log hut on the bare ground: 15 cells (2), log walls (1), no floor, nothing in it, cold
		house( ow, o[0], 7, 5, "wall_log", "door", null, "roof_thatch" );
		Rooms.Room room = Rooms.at( ow, inside );
		assertEquals( 2, Comfort.size( room ) );
		assertEquals( 1, Comfort.walls( ow, room ) );
		assertEquals( 0, Comfort.floors( ow, room ) );
		assertEquals( 3, Comfort.score( ow, room ) );
		assertEquals( Comfort.Level.BARE, Comfort.levelOf( 3 ) );

		//the same in brick with plank floors: 2 + 4 + 4
		OverworldLevel ow2 = site( 7, 5, o );
		house( ow2, o[0], 7, 5, "wall_brick", "door", "floor_plank", "roof_tile" );
		inside = o[0] + 3 + 2 * W;
		room = Rooms.at( ow2, inside );
		assertEquals( 4, Comfort.walls( ow2, room ) );
		assertEquals( 4, Comfort.floors( ow2, room ) );
		assertEquals( 10, Comfort.score( ow2, room ) );
		assertEquals( Comfort.Level.COSY, Comfort.levelOf( 10 ) );

		//two rugs and a statue: decor 3, the floor still all laid (the statue is no floor)
		build( ow2, o[0] + 2 + 2 * W, "rug" );
		build( ow2, o[0] + 4 + 2 * W, "rug" );
		build( ow2, o[0] + 5 + W, "statue" );
		room = Rooms.at( ow2, inside );
		assertEquals( 4, Comfort.floors( ow2, room ) );
		assertEquals( 13, Comfort.score( ow2, room ) );
		assertEquals( Comfort.Level.COMFORTABLE, Comfort.levelOf( 13 ) );

		//a fire warming it
		final int fireCell = o[0] + 1 + 3 * W;
		HeatSources.Provider fire = ( level, cell ) -> cell == fireCell ? HeatSources.COALS : 0f;
		HeatSources.register( fire );
		try {
			assertEquals( 16, Comfort.score( ow2, room ) );
			assertEquals( Comfort.Level.FINE, Comfort.levelOf( 16 ) );
		} finally {
			HeatSources.unregister( fire );
		}

		assertEquals( 20, Comfort.MAX );
		assertEquals( 0f, Comfort.fraction( -3 ), 0f );
		assertEquals( 1f, Comfort.fraction( 99 ), 0f );
		for (Comfort.Level l : Comfort.Level.values()) assertFalse( l.name(), l.title().contains( "NO TEXT" ) );
		assertEquals( Comfort.Level.PLAIN, Comfort.levelOf( 4 ) );
		assertEquals( Comfort.Level.FINE, Comfort.levelOf( 20 ) );
	}

	// ------------------------------------------------------------------ beds

	@Test
	public void aBedHealsByItsKindRaisedByComfortNeverPastHalf(){
		assertEquals( 0.25f, HomeBed.healFraction( HomeBed.Kind.STRAW, 0 ), 1e-5f );
		assertEquals( 1/3f, HomeBed.healFraction( HomeBed.Kind.WOOD, 0 ), 1e-5f );
		assertEquals( 0.5f, HomeBed.healFraction( HomeBed.Kind.FUR, 0 ), 1e-5f );
		assertEquals( 0.375f, HomeBed.healFraction( HomeBed.Kind.STRAW, Comfort.MAX ), 1e-5f );
		assertEquals( 1/3f * 1.25f, HomeBed.healFraction( HomeBed.Kind.WOOD, Comfort.MAX / 2 ), 1e-5f );
		assertEquals( "never past half", 0.5f, HomeBed.healFraction( HomeBed.Kind.WOOD, Comfort.MAX ), 1e-5f );
		assertEquals( 0.5f, HomeBed.healFraction( HomeBed.Kind.FUR, Comfort.MAX ), 1e-5f );

		//a bed in a room heals by that room's comfort; out of one by its own
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		house( ow, o[0], 7, 5, "wall_brick", "door", "floor_plank", "roof_tile" );
		HomeBed bed = put( ow, HomeBed.of( HomeBed.Kind.STRAW ), o[0] + 1 + W );
		Rooms.Room room = Rooms.at( ow, bed.pos );
		int comfort = Comfort.score( ow, room );
		assertTrue( comfort > 0 );
		assertEquals( comfort, bed.comfort() );
		assertTrue( bed.description(), bed.description().contains( Math.round( HomeBed.healFraction( HomeBed.Kind.STRAW, comfort ) * 100 ) + "%" ) );
		assertFalse( bed.name(), bed.name().contains( "NO TEXT" ) );
		bed.pos = o[0] - 2 * W;
		assertEquals( 0, bed.comfort() );

		//kept with the game
		HomeBed fur = HomeBed.of( HomeBed.Kind.FUR );
		Bundle b = new Bundle();
		fur.storeInBundle( b );
		HomeBed back = new HomeBed();
		back.restoreFromBundle( b );
		assertEquals( HomeBed.Kind.FUR, back.kind );
	}

	@Test
	public void aSleeperInASealedRoomIsNeverFoundButOneBehindAnOpenDoorIs(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		int door = house( ow, o[0], 7, 5, "wall_stone", "door", "floor_plank", "roof_slate" );
		Watched bed = put( ow, new Watched( HomeBed.Kind.WOOD ), o[0] + 1 + W );
		stand( ow, o[0] + 1 + 2 * W );
		hero.HP = hero.HT;
		assertTrue( bed.safe() );
		assertFalse( "shut in: nobody finds him", bed.sleep( hero, true ) );
		assertEquals( 0, bed.ambushes );
		assertNotNull( "a whole night: well rested", hero.buff( WellRested.class ) );
		Buff.detach( hero, WellRested.class );

		//the door left open: the wild walks in
		Level.set( door, Terrain.BUILT_DOOR_OPEN, ow );
		assertFalse( bed.safe() );
		assertTrue( bed.sleep( hero, true ) );
		assertEquals( 1, bed.ambushes );
		assertFalse( "found asleep", bed.roused );
		assertNull( "a broken night leaves him no better rested", hero.buff( WellRested.class ) );

		//a wall battered below half its health: no longer sealed either
		Level.set( door, Terrain.BUILT_DOOR, ow );
		assertTrue( bed.safe() );

		//the wild seen through a window does not keep him from lying down in a sealed room; one in
		//the room with him does, and so does any in sight of a bed that is not shut in
		GrayWolf outside = new GrayWolf(), in = new GrayWolf();
		outside.pos = o[0] + 3 - 2 * W;
		in.pos = o[0] + 3 + 2 * W;
		assertTrue( Bedroll.shutOut( ow, bed.pos, Arrays.asList( outside ) ) );
		assertFalse( Bedroll.shutOut( ow, bed.pos, Arrays.asList( outside, in ) ) );
		Level.set( door, Terrain.BUILT_DOOR_OPEN, ow );
		assertFalse( Bedroll.shutOut( ow, bed.pos, Arrays.asList( outside ) ) );
		Level.set( door, Terrain.BUILT_DOOR, ow );

		int wall = o[0] + 6 + 2 * W;
		Homestead.damage( ow, wall, 70, null );
		assertFalse( bed.safe() );
	}

	@Test
	public void aGuardRousesTheSleeperBeforeTheAmbushLands(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int[] o = new int[1];
		OverworldLevel ow = site( 12, 12, o );
		int at = o[0] + 6 + 6 * W;
		Watched bed = put( ow, new Watched( HomeBed.Kind.STRAW ), at );
		stand( ow, at + 1 );
		hero.HP = hero.HT;

		//found asleep
		assertTrue( bed.sleep( hero, true ) );
		assertEquals( 1, bed.ambushes );
		assertFalse( bed.roused );

		//an alarm: it rings first, and he is roused before they reach him
		final int[] asked = { 0 };
		SleepGuards.Guard bell = ( level, cell, h, found ) -> {
			asked[0]++;
			assertEquals( at, cell );
			assertNotNull( found );
			return SleepGuards.Roused.woke( "The bell rings!" );
		};
		SleepGuards.register( bell );
		try {
			assertTrue( "still found: only roused first", bed.sleep( hero, true ) );
			assertEquals( 1, asked[0] );
			assertEquals( 2, bed.ambushes );
			assertTrue( bed.roused );
			assertNull( "a broken night all the same", hero.buff( WellRested.class ) );

			//no guard is asked of a sleeper nobody finds
			HomeBed calm = put( ow, new HomeBed(){
				@Override protected float findChance( int turns, boolean fire ){
					return 0f;
				}
			}, at + 2 );
			assertFalse( calm.sleep( hero, true ) );
			assertEquals( 1, asked[0] );
		} finally {
			SleepGuards.unregister( bell );
		}

		//a guard that meets them itself (the threats' bell, at the edge of the home's ground): the
		//bed sets nobody down
		SleepGuards.Guard placer = ( level, cell, h, found ) -> SleepGuards.Roused.placed( null );
		SleepGuards.register( placer );
		try {
			assertTrue( bed.sleep( hero, true ) );
			assertEquals( "the guard placed them", 2, bed.ambushes );
		} finally {
			SleepGuards.unregister( placer );
		}

		//where they turn up: on top of a sleeper, farther off from a roused one
		PathFinder.buildDistanceMap( hero.pos, ow.passable );
		int[] dist = PathFinder.distance.clone();
		ArrayList<Integer> near = Bedroll.ambushSpots( hero.pos, 2, 3 ), far = Bedroll.ambushSpots( hero.pos, 4, 6 );
		assertFalse( near.isEmpty() );
		assertFalse( far.isEmpty() );
		for (int c : near) assertTrue( dist[c] >= 2 && dist[c] <= 3 );
		for (int c : far) assertTrue( dist[c] >= 4 && dist[c] <= 6 );
	}

	@Test
	public void aFurBedWakesItsSleeperColdOnlyInABitterRoom(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		HomeBed fur = put( ow, HomeBed.of( HomeBed.Kind.FUR ), o[0] + 2 + 2 * W );
		HomeBed straw = put( ow, HomeBed.of( HomeBed.Kind.STRAW ), o[0] + 4 + 2 * W );
		stand( ow, o[0] + 3 + 2 * W );
		try {
			//-5C: the straw's sleeper wakes cold, the fur's does not
			ClimateManager.debugTempOverride = -5f;
			assertTrue( call( straw, hero ) );
			assertFalse( call( fur, hero ) );
			//-20C: even the furs are not enough
			ClimateManager.debugTempOverride = -20f;
			assertTrue( call( fur, hero ) );
		} finally {
			ClimateManager.debugTempOverride = Float.NaN;
		}
	}

	private static boolean call( HomeBed bed, Hero hero ){
		return bed.wakesCold( hero );
	}

	@Test
	public void aFurBedSleepsOffAChill(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		HomeBed fur = put( ow, new HomeBed(){
			{ kind = Kind.FUR; }
			@Override protected float findChance( int turns, boolean fire ){
				return 0f;
			}
		}, o[0] + 2 + 2 * W );
		stand( ow, o[0] + 3 + 2 * W );
		try {
			ClimateManager.debugTempOverride = -5f;
			Buff.affect( hero, Hypothermia.class );
			assertFalse( fur.sleep( hero, true ) );
			assertNull( "slept off under furs", hero.buff( Hypothermia.class ) );
		} finally {
			ClimateManager.debugTempOverride = Float.NaN;
		}
	}

	@Test
	public void aBedBurnsDownATurnAtATime(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		int cell = o[0] + 3 + 2 * W;
		build( ow, cell, "bed_straw" );
		HomeBed bed = null;
		for (Mob m : ow.mobs) if (m instanceof HomeBed && m.pos == cell) bed = (HomeBed) m;
		assertNotNull( bed );
		BuildPiece straw = BuildCatalog.byId( "bed_straw" );
		assertTrue( straw.flammable );

		//no fire, no harm
		assertFalse( bed.smoulder( ow ) );
		assertEquals( straw.maxHp, Homestead.hpAt( ow, cell ) );

		Fire fire = new Fire();
		fire.cur = new int[ow.length()];
		fire.cur[cell] = 3;
		fire.volume = 3;
		ow.blobs.put( Fire.class, fire );
		int turns = 0;
		while (ow.mobs.contains( bed ) && turns < 10){
			assertTrue( bed.smoulder( ow ) );
			turns++;
		}
		assertEquals( "a straw bed lasts its health over the flames' bite",
				(straw.maxHp + Homestead.BURN_DAMAGE - 1) / Homestead.BURN_DAMAGE, turns );
		assertFalse( ow.mobs.contains( bed ) );
		assertNull( Homestead.pieceAt( ow, cell ) );
	}

	// ------------------------------------------------------------------ the rest it gives

	@Test
	public void wellRestedLastsToDuskByComfortAndTheShrineADay(){
		Dungeon.cycleTurn = 0;
		int dusk = DayNightCycle.phaseDuration( DayNightCycle.Phase.DAWN ) + DayNightCycle.phaseDuration( DayNightCycle.Phase.DAY );
		assertEquals( dusk, WellRested.turnsToDusk() );
		Dungeon.cycleTurn = dusk + 10;
		assertEquals( "dusk now: the next one", DayNightCycle.FULL_CYCLE - 10, WellRested.turnsToDusk() );
		Dungeon.cycleTurn = 100;
		assertEquals( Math.round( (dusk - 100) * 0.5f ), WellRested.duration( 0 ) );
		assertEquals( dusk - 100, WellRested.duration( Comfort.MAX ) );
		assertEquals( Math.round( (dusk - 100) * 0.75f ), WellRested.duration( Comfort.MAX / 2 ) );

		//a night in a shrine bedroom: well rested by its comfort, and blessed for a day
		DayNightCycle.debugPhaseOverride = null;
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		house( ow, o[0], 7, 5, "wall_brick", "door_barred", "floor_plank", "roof_tile" );
		build( ow, o[0] + 5 + W, "statue" );
		HomeBed bed = put( ow, HomeBed.of( HomeBed.Kind.FUR ), o[0] + 1 + W );
		stand( ow, o[0] + 1 + 2 * W );
		hero.HP = hero.HT;
		//a nap to the next part of the day only heals: no rest, no blessing
		DayNightCycle.Phase napTo = DayNightCycle.phase().next();
		if (napTo != DayNightCycle.Phase.DAWN){
			assertFalse( bed.sleep( hero, false ) );
			assertNull( "a nap", hero.buff( WellRested.class ) );
			assertNull( "a nap", hero.buff( ShrineBlessing.class ) );
		}
		assertFalse( bed.sleep( hero, true ) );
		assertEquals( "woke at dawn", DayNightCycle.Phase.DAWN, DayNightCycle.phase() );
		WellRested rested = hero.buff( WellRested.class );
		assertNotNull( rested );
		assertEquals( WellRested.duration( bed.comfort() ), rested.visualcooldown() - 1f, 0.01f );
		ShrineBlessing blessed = hero.buff( ShrineBlessing.class );
		assertNotNull( blessed );
		assertEquals( ShrineBlessing.DURATION, blessed.visualcooldown() - 1f, 0.01f );
		assertFalse( rested.desc().contains( "NO TEXT" ) );
		assertFalse( blessed.desc().contains( "NO TEXT" ) );
		assertFalse( rested.name().contains( "NO TEXT" ) );
		assertFalse( blessed.name().contains( "NO TEXT" ) );

		//both quicken healing a tenth; well rested teaches a tenth more
		assertEquals( 1.1f, WellRested.regenFactor( hero ), 1e-5f );
		assertEquals( 1.1f, ShrineBlessing.regenFactor( hero ), 1e-5f );
		int exp = hero.exp;
		hero.earnExp( 10, GrayWolf.class );
		assertEquals( exp + 11, hero.exp );
		//the tenths are kept until they make a whole point
		assertEquals( 0, WellRested.bonusExp( hero, 5 ) );
		assertEquals( 1, WellRested.bonusExp( hero, 5 ) );
		Buff.detach( hero, WellRested.class );
		Buff.detach( hero, ShrineBlessing.class );
		assertEquals( 1f, WellRested.regenFactor( hero ), 0f );
		assertEquals( 0, WellRested.bonusExp( hero, 50 ) );
	}

	// ------------------------------------------------------------------ chests and meals

	@Test
	public void aStorehouseChestHoldsTenMore(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		house( ow, o[0], 7, 5, "wall_stone", "door", "floor_plank", "roof_slate" );
		int base = PortableChest.capacityFor( PortableChest.WOOD );
		StorageChest one = put( ow, new StorageChest(), o[0] + 1 + W );
		assertEquals( base, one.capacity() );
		StorageChest two = put( ow, new StorageChest(), o[0] + 5 + W );
		assertEquals( base + RoomTypes.STOREHOUSE_SLOTS, one.capacity() );
		assertEquals( base + RoomTypes.STOREHOUSE_SLOTS, two.capacity() );
		StorageChest outside = put( ow, new StorageChest(), o[0] - 2 * W );
		assertEquals( base, outside.capacity() );
	}

	@Test
	public void aKitchenMealKeepsTheHeroWellFedHalfAgainAsLong(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		house( ow, o[0], 7, 5, "wall_stone", "door", "floor_plank", "roof_slate" );
		int pot = o[0] + 1 + W;
		put( ow, new Hearth(), pot );
		Food pie = new MeatPie();
		assertTrue( pie.grantsWellFed() );

		//eaten out in the open: as ever
		stand( ow, o[0] - 2 * W );
		new MeatPie().meal( hero );
		int plain = hero.buff( WellFed.class ).turnsLeft();
		Buff.detach( hero, WellFed.class );

		//eaten in the kitchen: half again as long
		stand( ow, o[0] + 3 + 2 * W );
		new MeatPie().meal( hero );
		int kitchen = Math.round( plain * RoomTypes.KITCHEN_WELL_FED );
		assertEquals( kitchen, hero.buff( WellFed.class ).turnsLeft() );
		//a second pie in the kitchen, the first's still running: well fed afresh, half again as
		//long, never cut back to a plain pie's
		new MeatPie().meal( hero );
		assertEquals( kitchen, hero.buff( WellFed.class ).turnsLeft() );
		//and while better fed than a fresh kitchen pie gives, it leaves him as he was
		hero.buff( WellFed.class ).extend( 100 );
		new MeatPie().meal( hero );
		assertEquals( kitchen + 100, hero.buff( WellFed.class ).turnsLeft() );
		Buff.detach( hero, WellFed.class );

		//a food that leaves nobody well fed is untouched by a kitchen
		Food ration = new Food();
		assertFalse( ration.grantsWellFed() );
		ration.meal( hero );
		assertNull( hero.buff( WellFed.class ) );

		//cooked in the kitchen, eaten anywhere
		Food cooked = new MeatPie();
		RoomTypes.cookedAt( cooked, ow, o[0] - 2 * W );
		assertFalse( "cooked out of doors", cooked.kitchenMade );
		RoomTypes.cookedAt( cooked, ow, pot );
		assertTrue( cooked.kitchenMade );
		Food plainCooked = new Food();
		RoomTypes.cookedAt( plainCooked, ow, pot );
		assertFalse( "only a meal that leaves him well fed remembers the kitchen", plainCooked.kitchenMade );
		stand( ow, o[0] - 2 * W );
		cooked.meal( hero );
		assertEquals( kitchen, hero.buff( WellFed.class ).turnsLeft() );

		//it remembers, and keeps to its own stack
		Bundle b = new Bundle();
		cooked.storeInBundle( b );
		MeatPie back = new MeatPie();
		back.restoreFromBundle( b );
		assertTrue( back.kitchenMade );
		assertFalse( back.isSimilar( new MeatPie() ) );
		assertTrue( back.isSimilar( cooked ) );
		assertFalse( back.desc().contains( "NO TEXT" ) );
	}

	// ------------------------------------------------------------------ hiding

	@Test
	public void aHeroInASealedRoomIsHiddenFromTheWildOutsideButForItsWindows(){
		int[] o = new int[1];
		OverworldLevel ow = site( 9, 9, o );
		//a 7x5 stone room two cells in, a window in the middle of its north wall
		int room = o[0] + 1 + 2 * W;
		house( ow, room, 7, 5, "wall_stone", "door_barred", "floor_plank", "roof_slate" );
		int window = room + 3;
		build( ow, window, "window_stone" );
		int inside = room + 3 + 2 * W;
		stand( ow, inside );
		assertTrue( Rooms.sealedAt( ow, inside ) );

		//a wolf east of the room, level with the hero: the wall hides him
		GrayWolf wolf = put( ow, new GrayWolf(), room + 9 + 2 * W );
		wolf.fieldOfView = new boolean[ow.length()];
		ow.updateFieldOfView( wolf, wolf.fieldOfView );
		assertFalse( "seen through a stone wall", wolf.fieldOfView[inside] );

		//north of the window, in line with it: seen through the glass
		wolf.pos = window - 2 * W;
		ow.updateFieldOfView( wolf, wolf.fieldOfView );
		assertTrue( "the window shows him", wolf.fieldOfView[inside] );

		//no way in past the barred door: a wolf that lost sight of him has nowhere to go but away
		PathFinder.buildDistanceMap( inside, ow.passable );
		assertEquals( Integer.MAX_VALUE, PathFinder.distance[room + 9 + 2 * W] );
	}
}

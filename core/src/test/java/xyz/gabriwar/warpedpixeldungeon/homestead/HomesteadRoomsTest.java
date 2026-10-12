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
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Comfy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Windswept;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Bedroll;
import xyz.gabriwar.warpedpixeldungeon.items.spells.BeaconOfReturning;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldFauna;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.tiles.RoofTiles;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The homesteads' rooms and shelter (homestead/Rooms, Shelter and the registries round them):
 * what closes a room and what does not, its answers cached and forgotten only round a change, the
 * air, the wind and the weather inside one and under a roof, no wildlife in a home, the weather's
 * harm on the slices scaled by the deepest floor, the bedroll's ambushers out in the world and its
 * safety in a sealed room, the beacon's world cell across a window move, and a home settled when
 * the hero comes back. Every window is a real one of a slice, derived from a seed.
 */
public class HomesteadRoomsTest {

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
		ClimateManager.debugWindOverride = Float.NaN;
		ClimateManager.debugPrecipOverride = Float.NaN;
		ClimateManager.debugPrecipTypeOverride = null;
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

	private static Object call( Object o, String name, Class<?>[] types, Object... args ){
		try {
			Method m = o.getClass().getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( o, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
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

	//a ring of `wall` w x h at o, a door of `door` in the middle of its south side, plank floor inside;
	//the door's cell
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

	//a roof of a kind over every cell of a w x h rect at o
	private static void roof( OverworldLevel ow, int o, int w, int h, String kind ){
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++) build( ow, o + x + y * W, kind );
		}
	}

	private void stand( OverworldLevel ow, int cell ){
		hero.pos = cell;
		hero.fieldOfView = new boolean[ow.length()];
		Arrays.fill( hero.fieldOfView, true );
	}

	@SuppressWarnings("unchecked")
	private static HashMap<Integer, ?> cached( OverworldLevel ow ){
		try {
			Field f = Rooms.Cache.class.getDeclaredField( "byCell" );
			f.setAccessible( true );
			return (HashMap<Integer, ?>) f.get( ow.homestead().roomCache );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	// ------------------------------------------------------------------ what a room is

	@Test
	public void aClosedRoofedStoneRingIsARoomAndItsDoorSealsIt(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		int door = house( ow, o[0], 7, 5, "wall_stone", "door_barred" );
		int inside = o[0] + 3 + 2 * W;
		assertEquals( "walls but no roof", Rooms.Verdict.UNROOFED, Rooms.check( ow, inside ) );
		roof( ow, o[0], 7, 5, "roof_slate" );

		Rooms.Room room = Rooms.at( ow, inside );
		assertNotNull( room );
		assertEquals( Rooms.Verdict.ROOM, Rooms.check( ow, inside ) );
		assertEquals( 15, room.size() );
		assertTrue( room.contains( o[0] + 1 + W ) );
		assertFalse( room.contains( door ) );
		assertEquals( 1f, room.roofFraction, 0.0001f );
		//16 cells close it: 15 of stone and the door
		assertEquals( 16, room.walls.length );
		assertEquals( (15 * Rooms.STONE + Rooms.DOOR) / 16f, room.insulation, 0.0001f );
		int first = o[0] + 1 + W;
		assertEquals( OverworldLevel.worldKey( ow.worldX + first % W, ow.worldY + first / W ), room.id );
		assertSame( "the same room from any of its cells", room, Rooms.at( ow, first ) );
		assertNull( "a wall is in no room", Rooms.at( ow, o[0] ) );
		assertEquals( Rooms.Verdict.SOLID, Rooms.check( ow, o[0] ) );
		assertTrue( room.sealed() );
		assertTrue( Rooms.sealedAt( ow, inside ) );

		//the door open: still a room, no longer sealed; shut again, sealed
		Level.set( door, Terrain.BUILT_DOOR_BARRED_OPEN, ow );
		assertNotNull( Rooms.at( ow, inside ) );
		assertFalse( Rooms.sealedAt( ow, inside ) );
		Level.set( door, Terrain.BUILT_DOOR_BARRED, ow );
		assertTrue( Rooms.sealedAt( ow, inside ) );

		//a wall broken below half its health unseals it
		assertTrue( Homestead.damage( ow, o[0] + 2, 70, null ) );
		assertFalse( Rooms.sealedAt( ow, inside ) );
		assertNotNull( "broken, still standing", Rooms.at( ow, inside ) );
	}

	@Test
	public void aGapOrAFenceInTheRingIsNoRoom(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		house( ow, o[0], 7, 5, "wall_log", "door" );
		roof( ow, o[0], 7, 5, "roof_thatch" );
		int inside = o[0] + 3 + 2 * W;
		assertNotNull( Rooms.at( ow, inside ) );

		//a fence where a wall stood: a gap
		int east = o[0] + 6 + 2 * W;
		Level.set( east, Terrain.FENCE, ow );
		assertNull( Rooms.at( ow, inside ) );
		assertEquals( Rooms.Verdict.GAP, Rooms.check( ow, inside ) );

		//open ground where it stood: the space runs out of the house
		Level.set( east, Terrain.GRASS, ow );
		assertNull( Rooms.at( ow, inside ) );
		Rooms.Verdict v = Rooms.check( ow, inside );
		assertTrue( v.name(), v == Rooms.Verdict.TOO_BIG || v == Rooms.Verdict.OPEN || v == Rooms.Verdict.GAP );

		//walled again: a room again
		build( ow, east, "wall_log" );
		assertNotNull( Rooms.at( ow, inside ) );
	}

	@Test
	public void halfARoofIsNoRoom(){
		int[] o = new int[1];
		OverworldLevel ow = site( 6, 6, o );
		house( ow, o[0], 6, 6, "wall_plank", "door" );
		//16 floor cells; the roof over the north half only (8 of them)
		roof( ow, o[0], 6, 3, "roof_plank" );
		int south = o[0] + 2 + 4 * W;
		assertEquals( Rooms.Verdict.UNROOFED, Rooms.check( ow, south ) );
		assertNull( Rooms.at( ow, south ) );
		//13 of 16 roofed is 81%: a room
		build( ow, o[0] + 1 + 3 * W, "roof_plank" );
		build( ow, o[0] + 2 + 3 * W, "roof_plank" );
		build( ow, o[0] + 3 + 3 * W, "roof_plank" );
		build( ow, o[0] + 4 + 3 * W, "roof_plank" );
		build( ow, o[0] + 1 + 4 * W, "roof_plank" );
		Rooms.Room room = Rooms.at( ow, south );
		assertNotNull( room );
		assertEquals( 13 / 16f, room.roofFraction, 0.0001f );
		//one roof off: 12 of 16, 75%
		Homestead.clearRoof( ow, o[0] + 1 + 4 * W );
		assertNull( Rooms.at( ow, south ) );
	}

	@Test
	public void aPocketDugInTheCaveRockAndShutWithADoorIsARoom(){
		OverworldLevel ow = null;
		int block = -1;
		for (int[] at : new int[][]{ { 0, 0 }, { 300, -200 }, { -500, 400 }, { 900, 900 } }){
			ow = windowAt( -1, at[0], at[1] );
			assertFalse( ow.openSky() );
			block = rockBlock( ow, 5, 4 );
			if (block != -1) break;
		}
		assertTrue( "a 5x4 block of cave rock in a test window", block != -1 );
		//dug out: a 3x2 pocket in the rock
		for (int y = 1; y <= 2; y++){
			for (int x = 1; x <= 3; x++) Level.set( block + x + y * W, Terrain.EMPTY, ow );
		}
		int inside = block + 2 + W;
		assertEquals( "the land alone closes it", Rooms.Verdict.NO_BUILT_WALL, Rooms.check( ow, inside ) );
		//a door in its north wall: a room, roofed by the cave without a roof built
		build( ow, block + 2, "door" );
		Rooms.Room room = Rooms.at( ow, inside );
		assertNotNull( room );
		assertEquals( 6, room.size() );
		assertEquals( 1f, room.roofFraction, 0.0001f );
		assertEquals( 10, room.walls.length );
		assertEquals( (9 * Rooms.ROCK + Rooms.DOOR) / 10f, room.insulation, 0.0001f );
		//the pick a cell further into its east wall: a bigger room
		Level.set( block + 4 + W, Terrain.EMPTY, ow );
		assertEquals( 7, Rooms.at( ow, inside ).size() );
		//the door gone: the rock alone closes it again
		Level.set( block + 2, Terrain.EMPTY, ow );
		assertEquals( Rooms.Verdict.NO_BUILT_WALL, Rooms.check( ow, inside ) );
	}

	//the top-left cell of a w x h block of natural rock away from the window's rim, -1 for none
	private static int rockBlock( OverworldLevel ow, int w, int h ){
		for (int y = 10; y < H - 10 - h; y++){
			for (int x = 10; x < W - 10 - w; x++){
				boolean rock = true;
				for (int dy = -1; dy <= h && rock; dy++){
					for (int dx = -1; dx <= w && rock; dx++){
						int t = ow.map[x + dx + (y + dy) * W];
						rock = t == Terrain.WALL || t == Terrain.WALL_DECO;
					}
				}
				if (rock) return x + y * W;
			}
		}
		return -1;
	}

	@Test
	public void sevenHundredCellsAreNoRoomAndTheFloodStopsPastSixHundred(){
		int[] o = new int[1];
		OverworldLevel ow = site( 4, 4, o );
		//a ring round 28 x 25 = 700 floor cells, wholly roofed, laid by force wherever it falls
		int iw = 28, ih = 25;
		int ox = W / 2 - iw / 2 - 1, oy = H / 2 - ih / 2 - 1, corner = ox + oy * W;
		for (int y = 0; y < ih + 2; y++){
			for (int x = 0; x < iw + 2; x++){
				int c = corner + x + y * W;
				boolean edge = x == 0 || y == 0 || x == iw + 1 || y == ih + 1;
				build( ow, c, edge ? (x == 1 && y == 0 ? "door" : "wall_stone") : "floor_stone" );
			}
		}
		int inside = corner + 5 + 5 * W;
		assertEquals( Rooms.Verdict.TOO_BIG, Rooms.check( ow, inside ) );
		assertNull( Rooms.at( ow, inside ) );
		assertEquals( "the flood kept the first 601 cells it reached and no more",
				Rooms.MAX_CELLS + 1, cached( ow ).size() );

		//walled down to 28 x 21 = 588: a room once roofed
		for (int x = 1; x <= iw; x++) build( ow, corner + x + 22 * W, "wall_stone" );
		Rooms.forget( ow );
		assertEquals( Rooms.Verdict.UNROOFED, Rooms.check( ow, inside ) );
		for (int y = 1; y <= 21; y++){
			for (int x = 1; x <= iw; x++) build( ow, corner + x + y * W, "roof_slate" );
		}
		Rooms.Room room = Rooms.at( ow, inside );
		assertNotNull( room );
		assertEquals( 28 * 21, room.size() );
	}

	@Test
	public void aChangeForgetsOnlyTheRoomsBesideIt(){
		int[] o = new int[1];
		OverworldLevel ow = site( 16, 5, o );
		house( ow, o[0], 7, 5, "wall_brick", "door" );
		roof( ow, o[0], 7, 5, "roof_tile" );
		int b = o[0] + 9;
		house( ow, b, 7, 5, "wall_stone", "door" );
		roof( ow, b, 7, 5, "roof_slate" );
		Rooms.Room a = Rooms.at( ow, o[0] + 3 + 2 * W );
		Rooms.Room other = Rooms.at( ow, b + 3 + 2 * W );
		assertNotNull( a );
		assertNotNull( other );

		//a wall of the first comes down: it is no room, the other is the very same answer
		Level.set( o[0] + 6 + 2 * W, Terrain.GRASS, ow );
		assertNull( Rooms.at( ow, o[0] + 3 + 2 * W ) );
		assertSame( other, Rooms.at( ow, b + 3 + 2 * W ) );

		//the window moving forgets them all
		int wx = ow.worldX + b % W, wy = ow.worldY + b / W;
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, wx + 40, wy - 30 );
		int moved = ow.localCell( wx, wy ) + 3 + 2 * W;
		Rooms.Room again = Rooms.at( ow, moved );
		assertNotNull( again );
		assertNotEquals( other, again );
		assertEquals( other.id, again.id );
	}

	// ------------------------------------------------------------------ shelter: the air

	@Test
	public void theAirInsideFollowsTheWallsAndTheFire(){
		int[] o = new int[1];
		OverworldLevel ow = site( 23, 5, o );
		int log = o[0], stone = o[0] + 8, brick = o[0] + 16;
		house( ow, log, 7, 5, "wall_log", "door" );
		house( ow, stone, 7, 5, "wall_stone", "door" );
		house( ow, brick, 7, 5, "wall_brick", "door" );
		roof( ow, log, 7, 5, "roof_thatch" );
		roof( ow, stone, 7, 5, "roof_slate" );
		roof( ow, brick, 7, 5, "roof_tile" );
		ClimateManager.debugTempOverride = -20f;
		ClimateManager.debugWindOverride = 15f;

		int outside = o[0] + 7 + 2 * W - 2 * W - W;
		assertNull( Rooms.at( ow, outside ) );
		float open = TileTemperature.tileTemp( outside );
		assertEquals( -20f, open, 0.01f );

		float[] felt = new float[3];
		int i = 0;
		for (int house : new int[]{ log, stone, brick }){
			int c = house + 3 + 2 * W;
			Rooms.Room r = Rooms.at( ow, c );
			assertNotNull( r );
			assertEquals( -20f + 35f * r.insulation, TileTemperature.tileTemp( c ), 0.01f );
			//no wind chill inside: the felt air is the room's
			assertEquals( TileTemperature.tileTemp( c ), TileTemperature.feelsLikeAt( c, null ), 0.01f );
			felt[i++] = TileTemperature.tileTemp( c );
		}
		assertTrue( felt[0] < felt[1] && felt[1] < felt[2] );
		//outside, the wind bites
		assertTrue( TileTemperature.feelsLikeAt( outside, null ) < open );

		//a fire in the brick room: its heat on the air, the room heated, the hero comfy in it
		int hearth = brick + 1 + W;
		HeatSources.Provider fire = ( level, cell ) -> cell == hearth ? 5f : 0f;
		HeatSources.register( fire );
		try {
			int c = brick + 3 + 2 * W;
			assertEquals( felt[2] + 5f, TileTemperature.tileTemp( c ), 0.01f );
			assertEquals( 5f, Rooms.at( ow, c ).heat(), 0.001f );
			assertTrue( Shelter.heated( ow, c ) );
			assertFalse( Shelter.heated( ow, stone + 3 + 2 * W ) );
			hero.pos = c;
			Comfy.check( hero );
			assertNotNull( hero.buff( Comfy.class ) );
			hero.pos = outside;
			Comfy.check( hero );
			assertNull( hero.buff( Comfy.class ) );
		} finally {
			HeatSources.unregister( fire );
		}
		assertFalse( Shelter.heated( ow, brick + 3 + 2 * W ) );
	}

	@Test
	public void nothingFallsUnderARoofButThroughThatch(){
		int[] o = new int[1];
		OverworldLevel ow = site( 12, 5, o );
		house( ow, o[0], 7, 5, "wall_stone", "door" );
		roof( ow, o[0], 7, 5, "roof_slate" );
		int inside = o[0] + 3 + 2 * W;
		//a lean-to: a wall and the roof beside it, no room
		int wall = o[0] + 9 + 2 * W, slate = wall + 1, thatch = wall + W + 1;
		build( ow, wall, "wall_log" );
		build( ow, wall + W, "wall_log" );
		build( ow, slate, "roof_slate" );
		build( ow, thatch, "roof_thatch" );
		int open = o[0] + 11 + 4 * W;
		assertNull( Rooms.at( ow, slate ) );
		assertFalse( Shelter.precipitationReaches( ow, inside ) );
		assertFalse( Shelter.precipitationReaches( ow, slate ) );
		assertTrue( Shelter.precipitationReaches( ow, open ) );
		assertFalse( Shelter.windReaches( ow, inside ) );
		assertTrue( "a roof keeps the rain off, not the wind", Shelter.windReaches( ow, slate ) );
		int through = 0;
		for (int i = 0; i < 2000; i++) if (Shelter.precipitationReaches( ow, thatch )) through++;
		assertTrue( "about one drop in five through thatch: " + through, through > 300 && through < 500 );

		//the hero in the rain and the gale: drenched and windswept outside, neither in the room
		ClimateManager.debugTempOverride = 10f;
		ClimateManager.debugPrecipTypeOverride = PrecipType.RAIN;
		ClimateManager.debugPrecipOverride = 1f;
		ClimateManager.debugWindOverride = 20f;
		stand( ow, inside );
		call( hero, "checkWeatherEffects", new Class<?>[0] );
		assertNull( hero.buff( Drenched.class ) );
		assertNull( hero.buff( Windswept.class ) );
		stand( ow, slate );
		call( hero, "checkWeatherEffects", new Class<?>[0] );
		assertNull( "dry under slate", hero.buff( Drenched.class ) );
		assertNotNull( "but the wind blows", hero.buff( Windswept.class ) );
		stand( ow, open );
		call( hero, "checkWeatherEffects", new Class<?>[0] );
		assertNotNull( hero.buff( Drenched.class ) );

		//nothing grows by itself in the room, under the roofs or on what is built
		assertTrue( Shelter.growthBarred( ow, inside ) );
		assertTrue( Shelter.growthBarred( ow, slate ) );
		assertTrue( Shelter.growthBarred( ow, wall ) );
		assertFalse( Shelter.growthBarred( ow, open ) );
	}

	// ------------------------------------------------------------------ no wildlife in a home

	@Test
	public void noBeastIsSetDownInARoomUnderARoofOrNearAHearthstone(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		house( ow, o[0], 7, 5, "wall_stone", "door" );
		roof( ow, o[0], 7, 5, "roof_slate" );
		int inside = o[0] + 3 + 2 * W;
		assertTrue( Shelter.wildlifeBarred( ow, inside ) );

		//a hearthstone 30 cells off: nothing within ten of it
		int stone = -1;
		for (int c = o[0] + 30; c < ow.length(); c++){
			if (ow.passable[c] && c % W > 20 && c % W < W - 20){
				stone = c;
				break;
			}
		}
		assertTrue( stone != -1 );
		int far = stone + 11;
		assertFalse( HomeAnchors.near( ow, stone ) );
		HomesteadWorld.addHome( new HomesteadWorld.Home( WorldLayers.SURFACE_DEPTH,
				ow.worldX + stone % W, ow.worldY + stone / W, "Test", 0 ) );
		assertTrue( HomeAnchors.near( ow, stone ) );
		assertTrue( HomeAnchors.near( ow, stone + 10 + 10 * W ) );
		assertFalse( HomeAnchors.near( ow, far ) );
		assertTrue( Shelter.wildlifeBarred( ow, stone + 5 ) );

		//a lit hearth anchors too (a provider)
		int hearth = stone + 20 * W;
		HomeAnchors.Provider lit = ( level, cells ) -> cells.accept( hearth );
		HomeAnchors.register( lit );
		try {
			assertTrue( HomeAnchors.near( ow, hearth - 4 ) );
		} finally {
			HomeAnchors.unregister( lit );
		}
		assertFalse( HomeAnchors.near( ow, hearth - 4 ) );

		//the wild set down and come back to life, again and again: never in the home
		hero.pos = W / 2 + 5 * W;
		for (int round = 0; round < 25; round++){
			Actor.clear();
			ow.mobs.clear();
			call( ow, "populateFauna", new Class<?>[0] );
			for (Mob m : ow.mobs) assertFalse( m.getClass().getSimpleName() + " in a home", Shelter.wildlifeBarred( ow, m.pos ) );
		}
		for (int i = 0; i < 2000; i++){
			int c = ow.randomRespawnCell( null );
			if (c != -1) assertFalse( Shelter.wildlifeBarred( ow, c ) );
		}
	}

	// ------------------------------------------------------------------ the weather's harm

	@Test
	public void theColdAndTheHeatOnTheSlicesScaleByTheDeepestFloor(){
		Statistics.deepestFloor = 12;
		Dungeon.level = new OverworldLevel( 0 );
		Dungeon.depth = WorldLayers.SURFACE_DEPTH;
		assertEquals( "not 1 + 97/10", 2, Hypothermia.damagePerHit() );
		assertEquals( "not 1 + 97/20", 1.6f, Heatstroke.depthScale(), 0.0001f );
		Dungeon.level = new OverworldLevel( -8 );
		Dungeon.depth = WorldLayers.depthOf( -8 );
		assertEquals( 2, Hypothermia.damagePerHit() );
		assertEquals( 1.6f, Heatstroke.depthScale(), 0.0001f );

		//the dungeon's floors as ever
		Dungeon.level = null;
		Dungeon.depth = 23;
		assertEquals( 1 + 23 / 10, Hypothermia.damagePerHit() );
		assertEquals( 1f + 23 / 20f, Heatstroke.depthScale(), 0.0001f );
	}

	// ------------------------------------------------------------------ the bedroll

	@Test
	public void aSleeperOutInTheWorldIsFoundByItsOwnWildAndNeverInASealedRoom(){
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		int found = 0;
		for (int c = W * 20; c < ow.length() - W * 20; c += 97){
			if (!ow.passable[c]) continue;
			for (Mob m : Bedroll.ambushers( ow, c )){
				found++;
				assertEquals( m.getClass().getSimpleName(), Char.Alignment.ENEMY, m.alignment );
				assertTrue( m.getClass().getSimpleName() + " is the world's", OverworldFauna.isFauna( m ) );
			}
		}
		assertTrue( "some of the wild finds a sleeper at night", found > 0 );

		//in the caves: the slice's own band
		OverworldLevel cave = windowAt( -5, 0, 0 );
		int caveFound = 0;
		for (int c = W * 20; c < cave.length() - W * 20; c += 53){
			if (!cave.passable[c]) continue;
			for (Mob m : Bedroll.ambushers( cave, c )){
				caveFound++;
				assertTrue( m.getClass().getSimpleName(), OverworldFauna.kindsAt( -5 ).contains( m.getClass() ) );
			}
		}
		assertTrue( caveFound > 0 );

		//a bed in a sealed room: nobody finds the sleeper; the door open, they may
		Dungeon.level = ow;
		int door = house( ow, o[0], 7, 5, "wall_stone", "door" );
		roof( ow, o[0], 7, 5, "roof_slate" );
		Bedroll bed = new Bedroll();
		bed.pos = o[0] + 2 + 2 * W;
		assertTrue( bed.safe() );
		Level.set( door, Terrain.BUILT_DOOR_OPEN, ow );
		assertFalse( bed.safe() );
		Level.set( door, Terrain.BUILT_DOOR, ow );
		assertTrue( bed.safe() );
		bed.pos = o[0] - 2 * W;
		assertFalse( bed.safe() );
		bed.shelter = true;
		assertTrue( "a waystation's bed as ever", bed.safe() );
	}

	// ------------------------------------------------------------------ the beacon

	@Test
	public void aBeaconSetInTheWorldKeepsItsWorldCellAcrossTheWindowMoving(){
		OverworldLevel ow = windowAt( 0, 300, 300 );
		int cell = W / 2 + 7 + (H / 2 - 5) * W;
		int wx = ow.worldX + cell % W, wy = ow.worldY + cell / W;
		BeaconOfReturning.BeaconTracker t = new BeaconOfReturning.BeaconTracker();
		t.aim( ow, WorldLayers.SURFACE_DEPTH, 0, cell );
		assertTrue( t.world );
		assertEquals( cell, t.returnCell( ow, WorldLayers.SURFACE_DEPTH, 0 ) );
		assertEquals( -1, t.returnCell( ow, WorldLayers.depthOf( -1 ), 0 ) );

		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, wx + 40, wy - 30 );
		int now = t.returnCell( ow, WorldLayers.SURFACE_DEPTH, 0 );
		assertNotEquals( "the window cell has moved", cell, now );
		assertEquals( wx, ow.worldX + now % W );
		assertEquals( wy, ow.worldY + now / W );
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, wx + 900, wy );
		assertEquals( "off the window: a far trip", -1, t.returnCell( ow, WorldLayers.SURFACE_DEPTH, 0 ) );

		Bundle b = new Bundle();
		t.storeInBundle( b );
		BeaconOfReturning.BeaconTracker back = new BeaconOfReturning.BeaconTracker();
		back.restoreFromBundle( b );
		assertTrue( back.world );
		assertEquals( wx, back.returnWX );
		assertEquals( wy, back.returnWY );

		//one set on a dungeon floor is its cell, as ever
		BeaconOfReturning.BeaconTracker floor = new BeaconOfReturning.BeaconTracker();
		floor.aim( null, 3, 0, 123 );
		assertFalse( floor.world );
		assertEquals( 123, floor.returnCell( null, 3, 0 ) );
	}

	// ------------------------------------------------------------------ the registries

	@Test
	public void aHomeIsSettledWhenTheHeroComesBackAfterAFullDay(){
		OverworldLevel ow = windowAt( 0, 300, 300 );
		int stone = W / 2 + (H / 2) * W;
		HomesteadWorld.Home home = new HomesteadWorld.Home( WorldLayers.SURFACE_DEPTH,
				ow.worldX + stone % W, ow.worldY + stone / W, "Oakhold", 0 );
		HomesteadWorld.addHome( home );
		final int[] settled = { 0, 0 };
		AwaySettlers.Settler pests = ( h, days ) -> {
			settled[0]++;
			settled[1] = days;
			return Collections.singletonList( "The rabbits ate " + days + " cabbages." );
		};
		AwaySettlers.register( pests );
		try {
			hero.pos = stone + 3;
			Dungeon.cycleTurn = 100;
			AwaySettlers.visit( ow );
			assertEquals( "home all day", 0, settled[0] );
			assertEquals( 100, home.lastVisitTurn );

			//away two days and a bit: nothing while he is still away
			hero.pos = stone + 30;
			Dungeon.cycleTurn = 100 + 2 * DayNightCycle.FULL_CYCLE + 50;
			AwaySettlers.visit( ow );
			assertEquals( 0, settled[0] );
			//back
			hero.pos = stone - 2 * W;
			AwaySettlers.visit( ow );
			assertEquals( 1, settled[0] );
			assertEquals( 2, settled[1] );
			List<HomesteadWorld.Entry> ledger = home.ledger();
			assertEquals( 1, ledger.size() );
			assertEquals( "The rabbits ate 2 cabbages.", ledger.get( 0 ).text );
			assertEquals( 2, ledger.get( 0 ).day );
			AwaySettlers.visit( ow );
			assertEquals( "settled once", 1, settled[0] );

			//the ledger in its window's words, and the menu's own actions
			assertTrue( HomeActions.ledgerText( home ).contains( "The rabbits ate 2 cabbages." ) );
			List<HomeActions.Action> actions = HomeActions.forHome( home, hero );
			assertEquals( "ledger", actions.get( 0 ).id );
			assertEquals( "rename", actions.get( 1 ).id );
			for (HomeActions.Action a : actions) assertFalse( a.label().contains( "NO TEXT" ) );
			assertTrue( "a guest's hero is offered no built-in action", HomeActions.forHome( home, new Hero() ).isEmpty() );

			//kept with the run
			Bundle b = new Bundle();
			HomesteadWorld.storeInBundle( b );
			HomesteadWorld.reset();
			HomesteadWorld.restoreFromBundle( b );
			HomesteadWorld.Home back = HomesteadWorld.homeAt( home.depth, home.wx, home.wy );
			assertEquals( home.lastVisitTurn, back.lastVisitTurn );
			assertEquals( "The rabbits ate 2 cabbages.", back.ledger().get( 0 ).text );
		} finally {
			AwaySettlers.unregister( pests );
		}
	}

	@Test
	public void aRoomsTraitsNameItAndActOnTheHeroInIt(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 5, o );
		house( ow, o[0], 7, 5, "wall_stone", "door" );
		roof( ow, o[0], 7, 5, "roof_slate" );
		int inside = o[0] + 3 + 2 * W;
		final int[] acted = { 0 };
		RoomTraits.Provider beds = ( level, room, out ) -> {
			if (room.contains( inside )){
				out.label( "bedroom", "Bedroom" );
				out.line( "A bed stands here." );
				out.effect( ( l, r, h ) -> acted[0]++ );
			}
		};
		RoomTraits.register( beds );
		try {
			Rooms.Room room = Rooms.at( ow, inside );
			assertTrue( RoomTraits.is( ow, room, "bedroom" ) );
			assertEquals( "Bedroom", RoomTraits.of( ow, room ).name() );
			String look = Rooms.describe( ow, inside );
			assertTrue( look, look.startsWith( "Bedroom" ) );
			assertTrue( look, look.contains( "A bed stands here." ) );
			assertFalse( look, look.contains( "NO TEXT" ) );
			hero.pos = inside;
			RoomTraits.worldTurn( ow );
			assertEquals( 1, acted[0] );
			hero.pos = o[0] - 2 * W;
			RoomTraits.worldTurn( ow );
			assertEquals( "only for the hero in the room", 1, acted[0] );
		} finally {
			RoomTraits.unregister( beds );
		}
		for (Rooms.Verdict v : Rooms.Verdict.values()) assertFalse( v.name(), v.message().contains( "NO TEXT" ) );
		String plain = Rooms.describe( ow, inside );
		assertTrue( plain, plain.startsWith( "A room" ) );
		assertFalse( plain, plain.contains( "NO TEXT" ) );
		assertEquals( Rooms.Verdict.SOLID.message(), Rooms.describe( ow, o[0] ) );
		assertEquals( RoofTiles.SLATE, Homestead.roofAt( ow, inside ) );
	}
}

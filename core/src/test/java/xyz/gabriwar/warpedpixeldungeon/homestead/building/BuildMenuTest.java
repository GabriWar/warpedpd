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

package xyz.gabriwar.warpedpixeldungeon.homestead.building;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.items.BuildersTool;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.RoofLayers;
import xyz.gabriwar.warpedpixeldungeon.ui.BuildBar;
import xyz.gabriwar.warpedpixeldungeon.ui.CostChips;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBuild;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBuildInspect;
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
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The build menu and bar against a real window of the surface: the cells tinted as taking a piece
 * are exactly those Homestead.canPlace allows (and the ones taking down or mending would act on),
 * a shape's plan pays for as many pieces as the satchel holds, the Recent list survives a save, a
 * look at a built cell tells its material, health, fire and roof, a room's roof fades without the
 * porch joined to it (and from its doorway too), and every word the menu and the bar say is
 * written. A roof laid over a whole room in one go is planned whole, from its walls inward; a
 * wall line keeps the cells it hides behind itself; the ghosts of a screen gone are left alone.
 */
public class BuildMenuTest {

	private static final long SEED = 0x5EED0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;

	private int depth, branch, challenges, startDay, turn;
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
		override = DayNightCycle.debugPhaseOverride;
		shift = WorldModel.seasonShift();
		shiftHeld = WorldModel.seasonShiftHeld();
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Random.pushGenerator( 4243 );
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
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
		DayNightCycle.debugPhaseOverride = override;
		WorldModel.releaseSeasonShift();
		if (shiftHeld) WorldModel.holdSeasonShift( shift );
		else WorldModel.setSeasonShift( shift );
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
	}

	// ------------------------------------------------------------------ helpers (as HomesteadEngineTest's)

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

	//the top-left of a w x h stretch of open, unedited, unprotected ground (a cell round it too)
	private static int openGround( OverworldLevel ow, int w, int h ){
		int mid = W / 2 + (H / 2) * W;
		for (int r = 0; r < 60; r++){
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

	private void stand( OverworldLevel ow, int cell ){
		hero.pos = cell;
		hero.fieldOfView = new boolean[ow.length()];
		Arrays.fill( hero.fieldOfView, true );
	}

	private void give( Class<? extends Item> cls, int n ){
		Item i = com.watabou.utils.Reflection.newInstance( cls );
		i.quantity( n );
		assertTrue( i.collect( hero.belongings.backpack ) );
	}

	private static void build( OverworldLevel ow, int cell, String id ){
		Homestead.build( ow, cell, BuildCatalog.byId( id ), null );
	}

	//every cell within reach of the hero where `ok` says yes
	private HashSet<Integer> within( OverworldLevel ow, java.util.function.IntPredicate ok ){
		HashSet<Integer> out = new HashSet<>();
		int hx = hero.pos % W, hy = hero.pos / W;
		for (int y = hy - Homestead.RANGE; y <= hy + Homestead.RANGE; y++){
			for (int x = hx - Homestead.RANGE; x <= hx + Homestead.RANGE; x++){
				if (x < 0 || y < 0 || x >= W || y >= H) continue;
				if (ok.test( x + y * W )) out.add( x + y * W );
			}
		}
		return out;
	}

	private static HashSet<Integer> set( int[] cells ){
		HashSet<Integer> out = new HashSet<>();
		for (int c : cells) assertTrue( "each cell once", out.add( c ) );
		return out;
	}

	// ------------------------------------------------------------------ the tint

	@Test
	public void theTintedCellsAreExactlyWhereThePieceGoesUp(){
		int[] o = new int[1];
		OverworldLevel ow = site( 9, 9, o );
		stand( ow, o[0] + 4 + 4 * W );
		Actor.add( hero );
		BuildPiece wall = BuildCatalog.byId( "wall_stone" );

		assertEquals( "no stone, nothing to tint", 0,
				BuildPlan.validAround( hero, ow, BuildSelection.Mode.SINGLE, wall ).length );
		new BuildersSatchel().collect( hero.belongings.backpack );
		give( RoughStone.class, 3 );
		HashSet<Integer> tinted = set( BuildPlan.validAround( hero, ow, BuildSelection.Mode.SINGLE, wall ) );
		assertEquals( within( ow, c -> Homestead.canPlace( hero, c, wall ).ok() ), tinted );
		assertTrue( tinted.size() > 40 );
		assertFalse( "not under the hero", tinted.contains( hero.pos ) );
		assertEquals( "line and area tint the same", tinted,
				set( BuildPlan.validAround( hero, ow, BuildSelection.Mode.LINE, wall ) ) );
		assertEquals( 0, BuildPlan.validAround( hero, ow, BuildSelection.Mode.SINGLE, null ).length );
		assertEquals( 0, BuildPlan.validAround( hero, ow, BuildSelection.Mode.INSPECT, wall ).length );

		//take down: the built cells only; mend: the damaged ones only
		int a = o[0] + 2 + 2 * W, b = o[0] + 6 + 2 * W;
		build( ow, a, "wall_stone" );
		build( ow, b, "wall_log" );
		assertEquals( new HashSet<>( Arrays.asList( a, b ) ),
				set( BuildPlan.validAround( hero, ow, BuildSelection.Mode.DEMOLISH, null ) ) );
		assertEquals( 0, BuildPlan.validAround( hero, ow, BuildSelection.Mode.REPAIR, null ).length );
		Homestead.damage( ow, a, 50, null );
		give( RoughStone.class, 1 );
		assertArrayEquals( new int[]{ a }, BuildPlan.validAround( hero, ow, BuildSelection.Mode.REPAIR, null ) );
		assertFalse( set( BuildPlan.validAround( hero, ow, BuildSelection.Mode.SINGLE, wall ) ).contains( a ) );
	}

	@Test
	public void aLinesPlanPaysForWhatTheSatchelHolds(){
		int[] o = new int[1];
		OverworldLevel ow = site( 9, 5, o );
		stand( ow, o[0] + 4 + 4 * W );
		Actor.add( hero );
		new BuildersSatchel().collect( hero.belongings.backpack );
		give( RoughStone.class, 7 );
		BuildPiece wall = BuildCatalog.byId( "wall_stone" );
		int[] line = BuildShapes.line( W, H, o[0] + 1 + W, o[0] + 7 + W );
		assertEquals( 7, line.length );
		//one cell of it already holds the wall
		build( ow, line[1], "wall_stone" );
		BuildPlan p = BuildPlan.of( hero, wall, line );
		assertEquals( Homestead.Reason.ALREADY_THERE, p.reasons[1] );
		assertEquals( 6, p.valid );
		assertEquals( "seven stones, three a wall", 2, p.affordable );
		assertArrayEquals( new int[]{ line[0], line[2] }, p.buildableCells() );
	}

	//a stone shell of w x h (its rim) at o, a door in the middle of its south side
	private static int shell( OverworldLevel ow, int o, int w, int h ){
		int door = -1;
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				int c = o + x + y * W;
				if (x != 0 && y != 0 && x != w - 1 && y != h - 1) continue;
				if (x == w / 2 && y == h - 1){
					build( ow, c, "door" );
					door = c;
				} else build( ow, c, "wall_stone" );
			}
		}
		return door;
	}

	@Test
	public void aRoofOverAWholeRoomIsPlannedFromItsWallsInAndBuiltWhole(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 7, o );
		shell( ow, o[0], 7, 7 );
		int a = o[0] + 1 + W, b = o[0] + 5 + 5 * W, mid = o[0] + 3 + 3 * W;
		stand( ow, mid );
		Actor.add( hero );
		new BuildersSatchel().collect( hero.belongings.backpack );
		BuildPiece roof = BuildCatalog.byId( "roof_thatch" );
		for (BuildPiece.Cost c : roof.cost) give( c.material, c.quantity * 25 );
		int[] area = BuildShapes.rect( W, a, b );
		assertEquals( 25, area.length );
		//the middle rests on nothing yet: alone it would not stand
		assertEquals( Homestead.Reason.UNSUPPORTED, Homestead.canPlaceSite( hero, mid, roof ) );

		BuildPlan p = BuildPlan.of( hero, roof, area );
		assertEquals( "every cell, the middle too", 25, p.valid );
		assertEquals( 25, p.affordable );
		int[] order = p.buildableCells();
		assertEquals( 25, order.length );
		assertEquals( "each cell once", 25, set( order ).size() );
		//built in that order, each cell finds what holds it up already there
		for (int c : order){
			assertEquals( Homestead.Reason.OK, Homestead.canPlace( hero, c, roof ) );
			assertEquals( Homestead.Reason.OK, Homestead.place( hero, c, roof ) );
		}
		for (int c : area) assertEquals( roof.roofKind, Homestead.roofAt( ow, c ) );
		Rooms.Room room = Rooms.at( ow, mid );
		assertNotNull( room );
		assertEquals( 1f, room.roofFraction, 0f );
		assertTrue( room.sealed() );
	}

	@Test
	public void aCellPlannedInSightIsBuiltThoughAWallOfTheSameShapeHidesItNow(){
		int[] o = new int[1];
		OverworldLevel ow = site( 6, 3, o );
		stand( ow, o[0] + W );
		Actor.add( hero );
		new BuildersSatchel().collect( hero.belongings.backpack );
		give( RoughStone.class, 30 );
		BuildPiece wall = BuildCatalog.byId( "wall_stone" );
		int[] line = BuildShapes.line( W, H, o[0] + 1 + W, o[0] + 4 + W );
		BuildPlan p = BuildPlan.of( hero, wall, line );
		assertEquals( 4, p.affordable );
		//the first wall up hides the rest of the line from where the hero stands
		assertEquals( Homestead.Reason.OK, Homestead.place( hero, line[0], wall ) );
		for (int i = 1; i < line.length; i++) hero.fieldOfView[line[i]] = false;
		assertEquals( Homestead.Reason.NOT_IN_SIGHT, Homestead.canPlace( hero, line[1], wall ) );
		for (int i = 1; i < line.length; i++){
			assertEquals( Homestead.Reason.OK, Homestead.canPlaceUnseen( hero, line[i], wall ) );
			assertEquals( Homestead.Reason.OK, Homestead.placeUnseen( hero, line[i], wall ) );
		}
		for (int c : line) assertEquals( wall, Homestead.pieceAt( ow, c ) );
		//sight is all it leaves out: out of reach, or already there, is still refused
		assertEquals( Homestead.Reason.ALREADY_THERE, Homestead.canPlaceUnseen( hero, line[2], wall ) );
		assertEquals( Homestead.Reason.OUT_OF_RANGE, Homestead.canPlaceUnseen( hero, o[0] + W + 6, wall ) );
	}

	@Test
	public void theGhostsOfAScreenGoneAreLeftAlone(){
		xyz.gabriwar.warpedpixeldungeon.effects.BuildGhost ghost = new xyz.gabriwar.warpedpixeldungeon.effects.BuildGhost();
		com.watabou.noosa.Group screen = new com.watabou.noosa.Group();
		screen.add( ghost );
		//the scene torn down (a phone turned): its members destroyed with it
		screen.destroy();
		ghost.reset();
		ghost.tint( new int[]{ 1, 2 }, 10 );
		//and placing, forgotten with it, is no longer on
		BuildPlacer.forget();
		assertNull( BuildPlacer.current() );
	}

	// ------------------------------------------------------------------ what lasts and what is said

	@Test
	public void theRecentListIsSavedWithTheTool() throws Exception {
		BuildersTool tool = new BuildersTool();
		tool.used( "wall_log" );
		tool.used( "door" );
		tool.used( "roof_slate" );
		Bundle b = new Bundle();
		b.put( "tool", tool );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		Bundle read = Bundle.read( new ByteArrayInputStream( out.toByteArray() ) );
		BuildersTool back = (BuildersTool) read.get( "tool" );
		assertEquals( Arrays.asList( "roof_slate", "door", "wall_log" ), back.recent );
		assertEquals( BuildCatalog.byId( "roof_slate" ), BuildRecent.pieces( back.recent ).get( 0 ) );
	}

	@Test
	public void aLookAtABuiltCellTellsItsMaterialHealthFireAndRoof(){
		int[] o = new int[1];
		OverworldLevel ow = site( 5, 3, o );
		int stone = o[0] + 1 + W, log = o[0] + 3 + W, bare = o[0] + 2 + 2 * W;
		build( ow, stone, "wall_stone" );
		build( ow, log, "wall_log" );
		build( ow, stone + W, "roof_slate" );
		Homestead.damage( ow, stone, 70, null );
		String s = BuildInfo.cellLines( ow, stone );
		assertTrue( s, s.contains( Messages.get( BuildInfo.class, "made_of", BuildInfo.material( RoughStone.class ) ) ) );
		assertTrue( s, s.contains( Messages.get( BuildInfo.class, "health", 50, 120 ) ) );
		assertFalse( s, s.contains( Messages.get( BuildInfo.class, "burns" ) ) );
		assertTrue( BuildInfo.cellLines( ow, log ).contains( Messages.get( BuildInfo.class, "burns" ) ) );
		assertTrue( BuildInfo.cellLines( ow, log ).contains( BuildInfo.material( Log.class ) ) );
		String roof = BuildInfo.cellLines( ow, stone + W );
		assertNotNull( roof );
		assertTrue( roof, roof.contains( "60" ) );
		assertNull( BuildInfo.cellLines( ow, bare ) );
		assertNull( "not on a dungeon level", BuildInfo.cellLines( null, 0 ) );
	}

	@Test
	public void everyWordTheMenuAndTheBarSayIsWritten(){
		String[][] keys = {
				{ "status_job", "status_choose", "status_single", "status_line_start", "status_line_end", "status_area_start",
						"status_area_end", "status_too_big", "status_plan", "status_demolish", "status_rubble",
						"status_demolish_none", "status_demolish_target", "status_repair", "status_repair_target", "status_inspect",
						"no_line", "no_area", "confirm_station", "confirm_yes", "confirm_no", "job_stopped", "job_danger",
						"job_built", "job_skipped", "job_short" },
				{ "cost", "made_of", "health", "unbreakable", "burns", "roof", "roof_plain", "roof_burning" },
				{ "confirm", "cancel", "free", "count", "mode_single", "mode_line", "mode_area", "mode_demolish", "mode_repair",
						"mode_inspect", "btn_single", "btn_line", "btn_area", "btn_demolish", "btn_repair", "btn_inspect",
						"btn_menu", "btn_done" },
				{ "demolish", "repair", "inspect", "empty", "health", "unbreakable", "turns", "burns", "cost", "missing",
						"place", "line", "area", "free", "count" },
				{ "nothing_built", "room" },
				{ "free" }
		};
		Class<?>[] owners = { BuildPlacer.class, BuildInfo.class, BuildBar.class, WndBuild.class, WndBuildInspect.class, CostChips.class };
		for (int i = 0; i < owners.length; i++){
			for (String k : keys[i]){
				assertNotEquals( owners[i].getSimpleName() + "." + k, Messages.NO_TEXT_FOUND, Messages.get( owners[i], k ) );
			}
		}
		for (BuildPiece.Placement p : BuildPiece.Placement.values()){
			BuildPiece piece = BuildPiece.terrain( "test_rule", xyz.gabriwar.warpedpixeldungeon.homestead.BuildCategory.WALLS,
					Terrain.BUILT_WALL_LOG ).placement( p ).done();
			assertFalse( p.name(), BuildInfo.placement( piece ).contains( "NO TEXT" ) );
		}
		for (String k : new String[]{ "name", "desc", "ac_build", "ac_demolish", "ac_repair", "ac_inspect", "not_here", "host_only" }){
			assertNotEquals( k, Messages.NO_TEXT_FOUND, Messages.get( BuildersTool.class, k ) );
		}
	}

	private static void assertNotEquals( String what, Object a, Object b ){
		assertFalse( what, a.equals( b ) );
	}

	// ------------------------------------------------------------------ the roof over a room

	@Test
	public void aRoomsRoofFadesButNotThePorchJoinedToIt(){
		int[] o = new int[1];
		OverworldLevel ow = site( 7, 7, o );
		int w = 7, h = 5, door = -1;
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				int c = o[0] + x + y * W;
				boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
				if (!edge) build( ow, c, "floor_plank" );
				else if (x == w / 2 && y == h - 1){
					build( ow, c, "door_barred" );
					door = c;
				} else build( ow, c, "wall_stone" );
			}
		}
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) build( ow, o[0] + x + y * W, "roof_slate" );
		int porch = door + W;
		build( ow, porch, "roof_plank" );
		int inside = o[0] + 2 + 2 * W;
		assertNotNull( "a room", Rooms.at( ow, inside ) );

		boolean[] joined = RoofLayers.connected( ow.homestead(), ow.worldX, ow.worldY, W, H, inside );
		assertTrue( "the porch is joined to the roof", joined[porch] );
		boolean[] hidden = RoofLayers.hiddenFor( ow, ow.homestead(), ow.worldX, ow.worldY, W, H, inside );
		assertTrue( hidden[inside] );
		assertTrue( "the roof over its walls", hidden[o[0]] );
		assertTrue( hidden[door] );
		assertFalse( "but not the porch", hidden[porch] );
		//in the doorway: the room it closes, not everything joined to it
		boolean[] inDoor = RoofLayers.hiddenFor( ow, ow.homestead(), ow.worldX, ow.worldY, W, H, door );
		assertTrue( inDoor[inside] );
		assertFalse( "the porch stays", inDoor[porch] );
		//under the porch, no room: the roofs joined to it
		boolean[] underPorch = RoofLayers.hiddenFor( ow, ow.homestead(), ow.worldX, ow.worldY, W, H, porch );
		assertTrue( underPorch[porch] );
		assertTrue( underPorch[inside] );
		assertNull( RoofLayers.hiddenFor( ow, ow.homestead(), ow.worldX, ow.worldY, W, H, porch + W ) );
	}
}

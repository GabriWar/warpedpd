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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Clay;
import xyz.gabriwar.warpedpixeldungeon.items.materials.FiredBrick;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Material;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.plants.PlantGrowthManager;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.HomesteadTiles;
import xyz.gabriwar.warpedpixeldungeon.tiles.HomesteadVisuals;
import xyz.gabriwar.warpedpixeldungeon.tiles.RoofLayers;
import xyz.gabriwar.warpedpixeldungeon.tiles.RoofTiles;
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The homesteads' engine (homestead/Homestead, HomesteadStore, BuildCatalog, the built terrain):
 * what may go up where and why not, putting a piece up and taking it down, its health through
 * window moves and a real save, the damage path (blows, fire, wrecking, the roof falling), the
 * barred door no monster's path crosses, the materials and the satchel, the roofs' layers and the
 * built walls' own fog. Every window is a real one of the surface, derived from a seed.
 */
public class HomesteadEngineTest {

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
		Random.pushGenerator( 4242 );
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

	private static Object call( Object o, String name, Class<?>[] types, Object... args ){
		try {
			Method m = OverworldLevel.class.getDeclaredMethod( name, types );
			m.setAccessible( true );
			return m.invoke( o, args );
		} catch (Exception e){
			throw new AssertionError( e.getCause() != null ? e.getCause() : e );
		}
	}

	//the top-left cell of a w x h stretch of open, unedited, unprotected ground (and a cell round
	//it) in the window, nearest its middle
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

	//a window with open ground in it, and that ground's top-left cell
	private int[] site( int w, int h ){
		for (int[] at : new int[][]{ { 300, 300 }, { -400, 260 }, { 600, -500 }, { -900, -700 }, { 1500, 900 } }){
			OverworldLevel ow = windowAt( at[0], at[1] );
			int o = openGround( ow, w, h );
			if (o != -1) return new int[]{ at[0], at[1], o };
		}
		throw new AssertionError( "no open ground in any test window" );
	}

	//the hero standing on a cell, seeing everything
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

	//a ring of walls (stone) w x h at o, its south middle a barred door, its inside plank floor
	private static int house( OverworldLevel ow, int o, int w, int h ){
		int door = -1;
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				int c = o + x + y * W;
				boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
				if (!edge) build( ow, c, "floor_plank" );
				else if (x == w / 2 && y == h - 1){
					build( ow, c, "door_barred" );
					door = c;
				} else build( ow, c, "wall_stone" );
			}
		}
		return door;
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

	// ------------------------------------------------------------------ the catalog

	@Test
	public void theCorePiecesAreListedWithTheirCosts(){
		BuildPiece wall = BuildCatalog.byId( "wall_brick" );
		assertNotNull( wall );
		assertEquals( BuildCategory.WALLS, wall.category );
		assertEquals( 160, wall.maxHp );
		assertEquals( 3, wall.buildTurns );
		assertEquals( 2, wall.cost.size() );
		assertEquals( FiredBrick.class, wall.cost.get( 0 ).material );
		assertEquals( 2, wall.cost.get( 0 ).quantity );
		assertEquals( Clay.class, wall.cost.get( 1 ).material );
		assertEquals( 0, BuildCatalog.byId( "path" ).maxHp );
		assertEquals( BuildPiece.Placement.WATER, BuildCatalog.byId( "bridge" ).placement );
		assertEquals( RoofTiles.THATCH, BuildCatalog.byId( "roof_thatch" ).roofKind );
		assertTrue( BuildCatalog.byId( "roof_thatch" ).flammable );
		assertEquals( BuildCatalog.byId( "door_barred" ), BuildCatalog.forTerrain( Terrain.BUILT_DOOR_BARRED_OPEN ) );
		//the core's 22 come first, the other parts' pieces after them
		assertTrue( BuildCatalog.all().size() >= 22 );
		assertEquals( "wall_log", BuildCatalog.all().get( 0 ).id );
		assertEquals( "roof_glass", BuildCatalog.all().get( 21 ).id );
		for (BuildPiece p : BuildCatalog.all()){
			assertFalse( p.category.menuOnly() );
			assertFalse( p.id + " has a name", p.name().contains( "NO TEXT" ) );
			assertFalse( p.id + " has a description", p.desc().contains( "NO TEXT" ) );
		}
		for (BuildCategory c : BuildCategory.values()) assertFalse( c.title().contains( "NO TEXT" ) );
		for (int t = Terrain.HOMESTEAD_FIRST; t <= Terrain.HOMESTEAD_LAST; t++){
			assertFalse( t + " is named", BuiltTerrain.name( t ).contains( "NO TEXT" ) );
			assertFalse( t + " is described", BuiltTerrain.desc( t ).contains( "NO TEXT" ) );
			assertNotEquals( 0, Terrain.flags[t] );
		}
		for (Homestead.Reason r : Homestead.Reason.values()) assertFalse( r.message().contains( "NO TEXT" ) );
	}

	// ------------------------------------------------------------------ the stores

	@Test
	public void theStoreComesBackFromItsBundle() throws Exception {
		HomesteadStore s = new HomesteadStore();
		s.setBuilt( 10, -20, "wall_log", 33 );
		s.setBuilt( -5000, 7000, "door", 30 );
		s.setRoof( 10, -21, RoofTiles.THATCH, 12 );
		s.roofAt( 10, -21 ).burning = 3;
		Bundle b = new Bundle();
		s.storeInBundle( b );
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertTrue( Bundle.write( b, out, false ) );
		HomesteadStore back = new HomesteadStore();
		back.restoreFromBundle( Bundle.read( new ByteArrayInputStream( out.toByteArray() ) ) );
		assertEquals( "wall_log", back.builtAt( 10, -20 ).piece );
		assertEquals( 33, back.builtAt( 10, -20 ).hp );
		assertEquals( "door", back.builtAt( -5000, 7000 ).piece );
		assertEquals( RoofTiles.THATCH, back.roofAt( 10, -21 ).kind );
		assertEquals( 12, back.roofAt( 10, -21 ).hp );
		assertEquals( 3, back.roofAt( 10, -21 ).burning );
		assertEquals( 2, back.builtCount() );

		HomesteadWorld.reset();
		HomesteadWorld.addHome( new HomesteadWorld.Home( 97, 12, -4, "Oakhold", 9 ) );
		Bundle g = new Bundle();
		HomesteadWorld.storeInBundle( g );
		HomesteadWorld.reset();
		assertTrue( HomesteadWorld.homes().isEmpty() );
		HomesteadWorld.restoreFromBundle( g );
		HomesteadWorld.Home h = HomesteadWorld.homeAt( 97, 12, -4 );
		assertNotNull( h );
		assertEquals( "Oakhold", h.name );
		assertEquals( 9, h.lastVisitDay );
		HomesteadWorld.reset();
	}

	// ------------------------------------------------------------------ persistence

	@Test
	public void aBuiltWallOutlastsTheWindowMovingAwayAndASave() throws Exception {
		int[] at = site( 3, 3 );
		OverworldLevel ow = windowAt( at[0], at[1] );
		int c = at[2] + 1 + W;
		int wx = ow.worldX + c % W, wy = ow.worldY + c / W;
		build( ow, c, "wall_stone" );
		build( ow, c + 1, "roof_slate" );
		assertEquals( Terrain.BUILT_WALL_STONE, ow.map[c] );
		assertTrue( Homestead.damage( ow, c, 20, null ) );
		assertEquals( 100, Homestead.hpAt( ow, c ) );

		//far away, and back
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, wx + 900, wy );
		assertEquals( -1, ow.localCell( wx, wy ) );
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, wx + 40, wy - 30 );
		int back = ow.localCell( wx, wy );
		assertNotEquals( "a different cell of the new window", c, back );
		assertEquals( Terrain.BUILT_WALL_STONE, ow.map[back] );
		assertEquals( 100, Homestead.hpAt( ow, back ) );
		assertEquals( RoofTiles.SLATE, Homestead.roofAt( ow, back + 1 ) );

		OverworldLevel loaded = saveAndLoad( ow );
		int cell = loaded.localCell( wx, wy );
		assertEquals( Terrain.BUILT_WALL_STONE, loaded.map[cell] );
		assertEquals( 100, Homestead.hpAt( loaded, cell ) );
		assertEquals( RoofTiles.SLATE, Homestead.roofAt( loaded, cell + 1 ) );
		for (xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap t : loaded.customWalls){
			if (t instanceof RoofLayers.Tiles) return;
		}
		throw new AssertionError( "the loaded window lays its roofs again" );
	}

	@Test
	public void natureNeverTakesBuiltTerrainBack() throws Exception {
		Method decay = OverworldLevel.class.getDeclaredMethod( "naturalDecay", int.class, int.class );
		decay.setAccessible( true );
		for (int pristine : new int[]{ Terrain.GRASS, Terrain.HIGH_GRASS, Terrain.FURROWED_GRASS, Terrain.SHRUB }){
			for (int t = Terrain.HOMESTEAD_FIRST; t <= Terrain.HOMESTEAD_LAST; t++){
				assertFalse( "a built " + t + " over " + pristine + " is the player's edit", (Boolean) decay.invoke( null, pristine, t ) );
			}
		}

		int[] at = site( 6, 1 );
		OverworldLevel ow = windowAt( at[0], at[1] );
		int o = at[2];
		build( ow, o, "floor_plank" );
		build( ow, o + 1, "path" );
		build( ow, o + 2, "wall_log" );
		ow.map[o + 3] = Terrain.FARMLAND;
		ow.map[o + 4] = Terrain.STUMP_OAK;
		ow.map[o + 5] = Terrain.RUBBLE_WOOD;
		ow.buildFlagMaps();
		int[] before = Arrays.copyOfRange( ow.map, o, o + 6 );

		//a downpour's mending, a spring's regrowth, a summer of turns
		xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.debugPrecipTypeOverride = xyz.gabriwar.warpedpixeldungeon.actors.PrecipType.RAIN;
		xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.debugPrecipOverride = 0.6f;
		try {
			for (int i = 0; i < 300; i++) ow.rainMends();
		} finally {
			xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.debugPrecipTypeOverride = null;
			xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.debugPrecipOverride = Float.NaN;
		}
		Method regrow = PlantGrowthManager.class.getDeclaredMethod( "processGrassRegrowth",
				xyz.gabriwar.warpedpixeldungeon.levels.Level.class, int.class, boolean.class );
		regrow.setAccessible( true );
		for (int i = 0; i < 200; i++) regrow.invoke( null, ow, 200, false );
		assertTrue( Arrays.equals( before, Arrays.copyOfRange( ow.map, o, o + 6 ) ) );

		//kept as edits through the window moving, never let go as decay
		int wx = ow.worldX + o % W, wy = ow.worldY + o / W;
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, wx + 900, wy );
		call( ow, "jumpWindowTo", new Class<?>[]{ int.class, int.class }, wx, wy );
		int b = ow.localCell( wx, wy );
		assertTrue( Arrays.equals( before, Arrays.copyOfRange( ow.map, b, b + 6 ) ) );
	}

	// ------------------------------------------------------------------ placing

	@Test
	public void theHeroBuildsWithinReachAndPays(){
		int[] at = site( 11, 5 );
		OverworldLevel ow = windowAt( at[0], at[1] );
		int o = at[2];
		int cell = o + 2 + W;
		stand( ow, o + W );
		Actor.add( hero );
		BuildPiece wall = BuildCatalog.byId( "wall_log" );
		assertEquals( Homestead.Reason.NO_MATERIALS, Homestead.canPlace( hero, cell, wall ) );
		new BuildersSatchel().collect( hero.belongings.backpack );
		give( Log.class, 3 );
		assertEquals( 3, Material.count( hero, Log.class ) );
		assertEquals( Homestead.Reason.OK, Homestead.canPlace( hero, cell, wall ) );
		Arrays.fill( hero.fieldOfView, false );
		assertEquals( Homestead.Reason.NOT_IN_SIGHT, Homestead.canPlace( hero, cell, wall ) );
		Arrays.fill( hero.fieldOfView, true );
		assertEquals( Homestead.Reason.OUT_OF_RANGE, Homestead.canPlace( hero, cell + Homestead.RANGE + 3, wall ) );
		assertEquals( Homestead.Reason.WRONG_GROUND, Homestead.canPlace( hero, cell, BuildCatalog.byId( "bridge" ) ) );
		assertEquals( Homestead.Reason.OCCUPIED, Homestead.canPlace( hero, hero.pos, wall ) );

		assertEquals( Homestead.Reason.OK, Homestead.place( hero, cell, wall ) );
		assertEquals( Terrain.BUILT_WALL_LOG, ow.map[cell] );
		assertEquals( 1, Material.count( hero, Log.class ) );
		assertEquals( wall, Homestead.pieceAt( ow, cell ) );
		assertEquals( 40, Homestead.hpAt( ow, cell ) );
		assertEquals( Homestead.Reason.ALREADY_THERE, Homestead.canPlace( hero, cell, wall ) );
		give( Log.class, 5 );
		assertEquals( "not over another piece", Homestead.Reason.WRONG_GROUND,
				Homestead.canPlace( hero, cell, BuildCatalog.byId( "palisade" ) ) );

		//a roof needs a wall within four steps; a roof joined to it reaches on
		give( Thatch.class, 40 );
		BuildPiece thatch = BuildCatalog.byId( "roof_thatch" );
		assertEquals( Homestead.Reason.OK, Homestead.canPlace( hero, cell + 1, thatch ) );
		int far = o + 9 + 3 * W;
		stand( ow, far - 2 );
		assertEquals( Homestead.Reason.UNSUPPORTED, Homestead.canPlace( hero, far, thatch ) );
	}

	@Test
	public void takingDownGivesHalfBackAndTheLandItsGround(){
		int[] at = site( 5, 3 );
		OverworldLevel ow = windowAt( at[0], at[1] );
		int o = at[2];
		int cell = o + 2 + W;
		int ground = ow.map[cell];
		stand( ow, o + W );
		give( FiredBrick.class, 2 );
		give( Clay.class, 1 );
		assertEquals( Homestead.Reason.OK, Homestead.place( hero, cell, BuildCatalog.byId( "wall_brick" ) ) );
		assertEquals( 0, Material.count( hero, FiredBrick.class ) );
		assertEquals( 0, Material.count( hero, Clay.class ) );
		assertEquals( Homestead.Reason.OK, Homestead.remove( hero, cell ) );
		assertEquals( "half the bricks", 1, Material.count( hero, FiredBrick.class ) );
		assertEquals( "the single clay is spent", 0, Material.count( hero, Clay.class ) );
		assertEquals( "the land's own ground", ground, ow.map[cell] );
		assertEquals( ow.pristineAt( cell ), ow.map[cell] );
		assertNull( Homestead.pieceAt( ow, cell ) );
		assertEquals( Homestead.Reason.NOTHING_THERE, Homestead.canRemove( hero, cell ) );

		//a single plank's fence gives nothing back; a log wall over felled ground leaves grass
		give( Plank.class, 1 );
		assertEquals( Homestead.Reason.OK, Homestead.place( hero, cell, BuildCatalog.byId( "fence" ) ) );
		assertEquals( Homestead.Reason.OK, Homestead.remove( hero, cell ) );
		assertEquals( 0, Material.count( hero, Plank.class ) );
		assertTrue( Homestead.refundOf( BuildCatalog.byId( "fence" ) ).isEmpty() );
		assertEquals( 2, Homestead.refundOf( BuildCatalog.byId( "door" ) ).get( 0 ).quantity );
		assertEquals( "a mending costs a quarter, rounded up", 1, Homestead.repairCost( BuildCatalog.byId( "wall_log" ) ).get( 0 ).quantity );
	}

	@Test
	public void protectedCellsRefuseAnyPiece(){
		BuildPiece floor = BuildCatalog.byId( "floor_stone" );
		give( RoughStone.class, 10 );

		//the town
		int tx = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), ty = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
		OverworldLevel town = windowAt( tx, ty );
		int plaza = town.localCell( tx, ty );
		stand( town, plaza + 1 );
		assertTrue( town.inTown( plaza ) );
		assertEquals( Homestead.Reason.PROTECTED, Homestead.canPlace( hero, plaza, floor ) );

		//a human village's house wall, and its door
		boolean wall = false, door = false;
		for (int r = 0; r < 12 && !(wall && door); r++){
			for (int sy = -r; sy <= r && !(wall && door); sy++){
				for (int sx = -r; sx <= r && !(wall && door); sx++){
					if (Math.max( Math.abs( sx ), Math.abs( sy ) ) != r) continue;
					if (WorldStructures.siteType( SEED, sx, sy ) != WorldStructures.Site.VILLAGE) continue;
					OverworldLevel v = windowAt( WorldStructures.siteX( SEED, sx, sy ), WorldStructures.siteY( SEED, sx, sy ) );
					for (int c = W + 1; c < v.length() - W - 1; c++){
						int p = v.pristineAt( c );
						if (p == Terrain.WALL && WorldStructures.wallSite( SEED, v.worldX + c % W, v.worldY + c / W ) == WorldStructures.Site.VILLAGE){
							stand( v, c );
							assertEquals( Homestead.Reason.PROTECTED, Homestead.canPlace( hero, c, floor ) );
							wall = true;
						} else if (p == Terrain.DOOR && !v.inTown( c )){
							stand( v, c );
							assertEquals( Homestead.Reason.PROTECTED, Homestead.canPlace( hero, c, floor ) );
							door = true;
						}
					}
				}
			}
		}
		assertTrue( "a village wall was tried", wall );
		assertTrue( "a village door was tried", door );

		//a way between the slices
		boolean link = false;
		for (int k = 0; k < 40 && !link; k++){
			OverworldLevel l = windowAt( 1000 + 176 * (k % 8), -800 + 176 * (k / 8) );
			for (int c = W + 1; c < l.length() - W - 1 && !link; c++){
				if (l.map[c] == Terrain.EXIT || l.map[c] == Terrain.ENTRANCE){
					stand( l, c );
					assertEquals( Homestead.Reason.PROTECTED, Homestead.canPlace( hero, c, floor ) );
					link = true;
				}
			}
		}
		assertTrue( "a slice link was tried", link );
	}

	// ------------------------------------------------------------------ damage

	@Test
	public void aWreckedPieceLeavesRubbleAndItsRoofFalls(){
		int[] at = site( 9, 3 );
		OverworldLevel ow = windowAt( at[0], at[1] );
		int o = at[2] + W;
		int wall = o + 1;
		build( ow, wall, "wall_stone" );
		build( ow, wall, "roof_slate" );
		//a roof held up only by that wall, two cells off
		build( ow, wall + 1, "roof_plank" );
		build( ow, wall + 2, "roof_plank" );
		//and a log wall on its own
		build( ow, o + 6, "wall_log" );
		assertEquals( RoofTiles.PLANK, Homestead.roofAt( ow, wall + 2 ) );

		assertTrue( Homestead.damage( ow, wall, 119, null ) );
		assertEquals( 1, Homestead.hpAt( ow, wall ) );
		assertTrue( Homestead.damage( ow, wall, 5, null ) );
		assertEquals( Terrain.RUBBLE_STONE, ow.map[wall] );
		assertNull( Homestead.pieceAt( ow, wall ) );
		assertEquals( "its own roof fell", -1, Homestead.roofAt( ow, wall ) );
		assertEquals( "the roofs it held fell too", -1, Homestead.roofAt( ow, wall + 1 ) );
		assertEquals( -1, Homestead.roofAt( ow, wall + 2 ) );

		int ground = ow.pristineAt( o + 6 );
		Homestead.damage( ow, o + 6, 1000, null );
		assertEquals( Terrain.RUBBLE_WOOD, ow.map[o + 6] );
		assertTrue( ow.passable[o + 6] );
		//the rubble cleared away gives the land its ground back
		stand( ow, o + 7 );
		assertEquals( Homestead.Reason.OK, Homestead.remove( hero, o + 6 ) );
		assertEquals( ground, ow.map[o + 6] );

		//a path takes nothing
		build( ow, o + 4, "path" );
		assertFalse( Homestead.damage( ow, o + 4, 1000, null ) );
		assertEquals( Terrain.PATH_STONE, ow.map[o + 4] );
	}

	@Test
	public void fireBurnsALogWallDownOverTurnsAndLeavesStone(){
		int[] at = site( 5, 4 );
		OverworldLevel ow = windowAt( at[0], at[1] );
		int log = at[2] + 1 + 3 * W, stone = at[2] + 3 + 3 * W;
		build( ow, log, "wall_log" );
		build( ow, stone, "wall_stone" );
		stand( ow, at[2] );
		Fire fire = xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob.seed( log, 4, Fire.class, ow );
		xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob.seed( stone, 4, Fire.class, ow );
		int turns = 0;
		int lastHp = Homestead.hpAt( ow, log );
		while (Homestead.isBuilt( ow, log ) && turns < 40){
			fire.act();
			turns++;
			if (Homestead.isBuilt( ow, log )){
				int hp = Homestead.hpAt( ow, log );
				assertTrue( "it burns down a little a turn", hp < lastHp );
				lastHp = hp;
			}
		}
		assertTrue( "not at once: " + turns, turns >= 4 );
		assertEquals( Terrain.RUBBLE_WOOD, ow.map[log] );
		assertEquals( "the stone wall stands whole", 120, Homestead.hpAt( ow, stone ) );
		assertEquals( Terrain.BUILT_WALL_STONE, ow.map[stone] );
	}

	// ------------------------------------------------------------------ doors

	@Test
	public void noMonsterPathCrossesABarredDoor(){
		int[] at = site( 7, 9 );
		OverworldLevel ow = windowAt( at[0], at[1] );
		int o = at[2];
		int door = house( ow, o, 7, 5 );
		int inside = o + 3 + 2 * W;
		int outside = door + 3 * W;
		stand( ow, inside );
		Actor.add( hero );

		Rat rat = new Rat();
		rat.pos = outside;
		ow.mobs.add( rat );
		Actor.add( rat );
		assertNull( "no way in for a monster",
				Dungeon.findPath( rat, inside, ow.passable, ow.heroFOV, false ) );

		Rat ally = new Rat();
		ally.alignment = Char.Alignment.ALLY;
		ally.pos = outside + 1;
		ow.mobs.add( ally );
		Actor.add( ally );
		PathFinder.Path p = Dungeon.findPath( ally, inside, ow.passable, ow.heroFOV, false );
		assertNotNull( "the hero's ally finds its way in", p );
		assertTrue( "through the door", p.contains( door ) );

		//the ally opens it stepping in, and it shuts behind once the doorway is clear
		ally.pos = door;
		ow.occupyCell( ally );
		assertEquals( Terrain.BUILT_DOOR_BARRED_OPEN, ow.map[door] );
		assertTrue( ow.passable[door] );
		Homestead.worldTurn( ow );
		assertEquals( "not while it stands in it", Terrain.BUILT_DOOR_BARRED_OPEN, ow.map[door] );
		ally.pos = inside - 1;
		advance( Homestead.OPEN_GRACE + 1 );
		Homestead.worldTurn( ow );
		assertEquals( Terrain.BUILT_DOOR_BARRED, ow.map[door] );

		//the hero lifts the bar by hand; it waits for him to step through
		assertTrue( Homestead.openByHand( hero, door ) );
		Homestead.worldTurn( ow );
		assertEquals( Terrain.BUILT_DOOR_BARRED_OPEN, ow.map[door] );
		advance( Homestead.OPEN_GRACE + 1 );
		Homestead.worldTurn( ow );
		assertEquals( Terrain.BUILT_DOOR_BARRED, ow.map[door] );
		assertFalse( "a monster opens nothing", Homestead.opensByHand( rat ) );
	}

	private static void advance( float turns ){
		try {
			Field now = Actor.class.getDeclaredField( "now" );
			now.setAccessible( true );
			now.setFloat( null, now.getFloat( null ) + turns );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	// ------------------------------------------------------------------ materials

	@Test
	public void materialsStackAndRideInTheSatchel(){
		BuildersSatchel satchel = new BuildersSatchel();
		assertTrue( satchel.collect( hero.belongings.backpack ) );
		give( Log.class, 3 );
		give( Log.class, 4 );
		give( RoughStone.class, 2 );
		Log logs = hero.belongings.getItem( Log.class );
		assertEquals( "one stack", 7, logs.quantity() );
		assertTrue( "in the satchel", satchel.items.contains( logs ) );
		assertFalse( "nothing else goes in", satchel.canHold( new xyz.gabriwar.warpedpixeldungeon.items.food.Food() ) );
		for (Class<? extends Material> cls : Material.ALL){
			Item m = com.watabou.utils.Reflection.newInstance( cls );
			assertTrue( cls.getSimpleName() + " stacks", m.stackable );
			assertTrue( cls.getSimpleName() + " sells", m.value() > 0 );
			assertTrue( cls.getSimpleName() + " fits the satchel", satchel.canHold( m ) );
		}
		assertTrue( Material.spend( hero, Log.class, 5 ) );
		assertEquals( 2, Material.count( hero, Log.class ) );
		assertFalse( "never more than there is", Material.spend( hero, Log.class, 3 ) );
		assertEquals( 2, Material.count( hero, Log.class ) );
		assertEquals( 2 * 3, new Log().quantity( 2 ).value() );
	}

	// ------------------------------------------------------------------ drawing

	@Test
	public void builtWallsKeepTheirOwnFogAndStitch(){
		int[] map = new int[5 * 5];
		Arrays.fill( map, Terrain.GRASS );
		for (int y = 1; y <= 3; y++) for (int x = 1; x <= 3; x++) map[x + y * 5] = Terrain.BUILT_WALL_STONE;
		//a solid block's heart is no void: its own fog, its top painted
		assertTrue( HomesteadVisuals.ownFog( map[2 + 2 * 5] ) );
		assertTrue( DungeonTileSheet.wallStitcheable( Terrain.BUILT_WINDOW_LOG ) );
		assertTrue( DungeonTileSheet.doorTile( Terrain.BUILT_DOOR_BARRED ) );
		assertFalse( HomesteadVisuals.ownFog( Terrain.WALL ) );
		assertFalse( HomesteadVisuals.ownFog( Terrain.FLOOR_PLANK ) );
		assertEquals( HomesteadTiles.FLOOR_PLANK, HomesteadVisuals.previewFrame( Terrain.FLOOR_PLANK ) );
	}

	@Test
	public void aFelledTreeLosesItsDressAndAMirrorSeesTheRoofs() throws Exception {
		OverworldLevel ow = null;
		int tree = -1;
		for (int[] at : new int[][]{ { 300, 300 }, { -400, 260 }, { 600, -500 }, { -900, -700 }, { 1500, 900 } }){
			ow = windowAt( at[0], at[1] );
			for (int c = 3 * W; c < ow.length() - 3 * W && tree == -1; c++){
				if (ow.map[c] == Terrain.TREE_OAK && ow.map[c - W] != Terrain.TREE_OAK && ow.map[c - W] != Terrain.TREE_PINE) tree = c;
			}
			if (tree != -1) break;
		}
		assertNotEquals( "an oak in some window", -1, tree );
		Field edges = OverworldLevel.class.getDeclaredField( "dressEdges" ), canopy = OverworldLevel.class.getDeclaredField( "dressCanopy" );
		edges.setAccessible( true );
		canopy.setAccessible( true );
		xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer body = (xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer) edges.get( ow );
		xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer crown = (xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.Layer) canopy.get( ow );
		assertNotEquals( "the oak's trunk is drawn", -1, body.get( tree ) );
		assertNotEquals( "its crown hangs over the cell above", -1, crown.get( tree - W ) );
		xyz.gabriwar.warpedpixeldungeon.levels.Level.set( tree, Terrain.STUMP_OAK, ow );
		ow.redressAround( tree );
		assertEquals( "felled: no trunk", -1, body.get( tree ) );
		assertEquals( "and no crown", -1, crown.get( tree - W ) );

		//the host's pieces and roofs, as his guest's mirror takes them
		build( ow, tree, "roof_glass" );
		String shared = ow.sharedHomestead();
		OverworldLevel mirror = OverworldLevel.forNetwork( 0, SEED, ow.worldX, ow.worldY, WorldModel.calendarShift(),
				GameCalendar.season(), ow.map.clone(), W, H );
		assertNotNull( mirror );
		mirror.adoptSharedHomestead( shared );
		assertEquals( RoofTiles.GLASS, Homestead.roofAt( mirror, tree ) );
		assertFalse( "a mirror builds nothing", Homestead.allowedHere( mirror ) );
	}

	@Test
	public void theRoofOverTheHeroFades(){
		HomesteadStore s = new HomesteadStore();
		int ox = 100, oy = 200, w = 20, h = 20;
		for (int y = 5; y <= 9; y++) for (int x = 5; x <= 11; x++) s.setRoof( ox + x, oy + y, RoofTiles.SLATE, 60 );
		s.setRoof( ox + 15, oy + 5, RoofTiles.THATCH, 20 );
		int n = w * h;
		int[] slopes = new int[n], ridges = new int[n], faded = new int[n];
		RoofLayers.compute( s, ox, oy, w, h, -1, slopes, ridges, faded );
		int corner = 5 + 5 * w;
		assertEquals( RoofTiles.index( RoofTiles.SLATE, RoofTiles.E | RoofTiles.S, true ), slopes[corner] );
		assertEquals( "the ridge on the middle row", RoofTiles.ridge( RoofTiles.SLATE, RoofTiles.RIDGE_OPEN_W ), ridges[5 + 7 * w] );
		assertEquals( RoofTiles.index( RoofTiles.SLATE, RoofTiles.N | RoofTiles.E | RoofTiles.W, false ), slopes[8 + 9 * w] );
		assertEquals( -1, faded[corner] );

		RoofLayers.compute( s, ox, oy, w, h, 8 + 7 * w, slopes, ridges, faded );
		assertEquals( -1, slopes[corner] );
		assertNotEquals( -1, faded[corner] );
		assertEquals( "a roof apart stays", RoofTiles.index( RoofTiles.THATCH, 0, false ), slopes[15 + 5 * w] );
	}
}

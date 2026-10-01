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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.SparseArray;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.BellowsDraft;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CounterweightPlates;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ElementalLock;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ForgeCoals;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.PedigreeWard;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.EmptyRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.StandardRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.ThermalSpringRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.standard.WayfarersCampRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.BellowsRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.BlackMarketRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.BreakersBenchRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.CounterweightVaultRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.ElementalLockRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.FrozenCacheRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.HothouseRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.IncubatorNestRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.PedigreeHallRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.RivalGalleryRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.SquirrelsHoardRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.TemperingForgeRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import xyz.gabriwar.warpedpixeldungeon.messages.Languages;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The rooms Warped adds, with no screen: every one of them painted with its door on each
 * of the four walls at its smallest and its largest, and the mechanisms that run them
 * driven turn by turn on a bare floor.
 */
public class WarpedRoomsTest {

	private static final Class<?>[] ROOMS = {
			BlackMarketRoom.class, RivalGalleryRoom.class, TemperingForgeRoom.class,
			BreakersBenchRoom.class, PedigreeHallRoom.class, FrozenCacheRoom.class,
			BellowsRoom.class, ElementalLockRoom.class, HothouseRoom.class,
			IncubatorNestRoom.class, CounterweightVaultRoom.class, SquirrelsHoardRoom.class,
			ThermalSpringRoom.class, WayfarersCampRoom.class
	};

	private static final class AssetFiles implements com.badlogic.gdx.Files {
		private final File root = new File( "src/main/assets" );
		private FileHandle at( String path ){ return new FileHandle( new File( root, path ) ); }
		@Override public FileHandle getFileHandle( String path, FileType type ){ return at( path ); }
		@Override public FileHandle classpath( String path ){ return at( path ); }
		@Override public FileHandle internal( String path ){ return at( path ); }
		@Override public FileHandle external( String path ){ return at( path ); }
		@Override public FileHandle absolute( String path ){ return new FileHandle( new File( path ) ); }
		@Override public FileHandle local( String path ){ return at( path ); }
		@Override public String getExternalStoragePath(){ return root.getAbsolutePath(); }
		@Override public boolean isExternalStorageAvailable(){ return false; }
		@Override public String getLocalStoragePath(){ return root.getAbsolutePath(); }
		@Override public boolean isLocalStorageAvailable(){ return false; }
	}

	@BeforeClass
	public static void boot(){
		GdxNativesLoader.load();
		Gdx.files = new AssetFiles();
		final HashMap<String, Object> store = new HashMap<>();
		final Preferences prefs = (Preferences) Proxy.newProxyInstance( Preferences.class.getClassLoader(),
				new Class<?>[]{ Preferences.class }, (proxy, m, args) -> {
					String n = m.getName();
					if (n.startsWith( "put" )){ if (args.length == 2) store.put( (String) args[0], args[1] ); return proxy; }
					if (n.equals( "contains" )) return store.containsKey( (String) args[0] );
					if (n.startsWith( "get" ) && args != null && args.length >= 1){
						Object v = store.get( (String) args[0] );
						if (v != null) return v;
						if (args.length == 2) return args[1];
					}
					Class<?> r = m.getReturnType();
					if (r == boolean.class) return false;
					if (r == int.class) return 0;
					if (r == long.class) return 0L;
					if (r == float.class) return 0f;
					return r == String.class ? "" : null;
				} );
		Gdx.app = (Application) Proxy.newProxyInstance( Application.class.getClassLoader(),
				new Class<?>[]{ Application.class }, (proxy, m, args) -> {
					if (m.getName().equals( "getPreferences" )) return prefs;
					if (m.getName().equals( "getType" )) return Application.ApplicationType.Desktop;
					Class<?> r = m.getReturnType();
					if (r == int.class || r == long.class) return 0;
					if (r == boolean.class) return false;
					return null;
				} );
		Game.version = "test";
		//a game with no scene: Level.drop asks the live scene whether to press the cell
		if (Game.instance == null) new Game( null, null );
		Messages.setup( Languages.ENGLISH );
	}

	private Level level;

	private static Level bareLevel( int w, int h ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
			@Override public boolean waterCanFreeze(){ return true; }
		};
		l.setSize( w, h );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		return l;
	}

	@Before
	public void setUp(){
		Dungeon.depth = 3;
		Dungeon.branch = 0;
		Dungeon.hero = new Hero();
		ClimateManager.debugTempOverride = 15f;
		level = bareLevel( 40, 40 );
		Dungeon.level = level;
		Dungeon.hero.pos = 1 + level.width();
	}

	// ---------------------------------------------------------------- painting

	@Test
	public void everyRoomPaintsWithItsDoorOnAnyWall() throws Exception {
		for (Class<?> type : ROOMS){
			for (int wall = 0; wall < 4; wall++){
				for (boolean big : new boolean[]{ false, true }){
					setUp();
					Room room = (Room) type.getConstructor().newInstance();
					int w = big ? room.maxWidth() : room.minWidth();
					int h = big ? room.maxHeight() : room.minHeight();
					if (room instanceof StandardRoom){
						w = big ? 10 : room.minWidth();
						h = big ? 10 : room.minHeight();
					}
					room.set( 12, 12, 12 + w - 1, 12 + h - 1 );
					Painter.fill( level, 0, 0, 40, 40, Terrain.EMPTY );

					Room.Door door;
					switch (wall){
						case 0:  door = new Room.Door( room.left, room.top + 2 ); break;
						case 1:  door = new Room.Door( room.left + 2, room.top ); break;
						case 2:  door = new Room.Door( room.right, room.bottom - 2 ); break;
						default: door = new Room.Door( room.right - 2, room.bottom ); break;
					}
					room.connected.put( new EmptyRoom(), door );

					String what = type.getSimpleName() + " wall " + wall + (big ? " max" : " min");
					//a floor is painted before it is anybody's current level (Dungeon.newLevel)
					Dungeon.level = null;
					try {
						room.paint( level );
					} catch (Throwable t){
						t.printStackTrace();
						fail( what + " threw " + t );
					}
					Dungeon.level = level;
					Painter.set( level, door.x, door.y, Terrain.DOOR );
					level.buildFlagMaps();

					//the way in is open, and everything that lives in the room can be walked up to
					boolean[] open = new boolean[level.length()];
					for (int i = 0; i < open.length; i++) open[i] = level.passable[i];
					for (Mob mob : level.mobs) open[mob.pos] = false;
					int doorCell = level.pointToCell( new Point( door.x, door.y ) );
					PathFinder.buildDistanceMap( doorCell, open );
					for (Mob mob : level.mobs){
						boolean reachable = false;
						for (int ofs : PathFinder.NEIGHBOURS8){
							int n = mob.pos + ofs;
							if (n >= 0 && n < open.length && PathFinder.distance[n] != Integer.MAX_VALUE) reachable = true;
						}
						assertTrue( what + ": nobody can reach its " + mob.getClass().getSimpleName(), reachable );
						assertTrue( what + ": " + mob.getClass().getSimpleName() + " is outside the room",
								room.inside( level.cellToPoint( mob.pos ) ) );
					}
					//every picture it laid names itself
					for (CustomTilemap tile : level.customTiles){
						if (!(tile instanceof WarpedRoomTiles.Deco)) continue;
						String name = tile.name( 0, 0 ), desc = tile.desc( 0, 0 );
						assertNotNull( what, name );
						assertFalse( what + ": a tile has no name", name.contains( Messages.NO_TEXT_FOUND ) );
						assertFalse( what + ": a tile has no description", desc.contains( Messages.NO_TEXT_FOUND ) );
						if (tile instanceof WarpedRoomTiles.Switch){
							((WarpedRoomTiles.Switch) tile).on = true;
							assertFalse( what + ": a switched tile has no description",
									tile.desc( 0, 0 ).contains( Messages.NO_TEXT_FOUND ) );
						}
					}
					//and so does everyone in it
					for (Mob mob : level.mobs){
						assertFalse( what + ": " + mob.getClass().getSimpleName() + " has no name",
								mob.name().contains( Messages.NO_TEXT_FOUND ) );
						assertFalse( what + ": " + mob.getClass().getSimpleName() + " has no description",
								mob.description().contains( Messages.NO_TEXT_FOUND ) );
					}
				}
			}
		}
	}

	// ---------------------------------------------------------------- mechanisms

	private void floor(){
		Painter.fill( level, 1, 1, 38, 38, Terrain.EMPTY );
		level.buildFlagMaps();
	}

	@Test
	public void coalsCatchBurnHeatAndGoOut(){
		floor();
		int cell = 20 + 20 * level.width();
		ForgeCoals coals = Blob.seed( cell, 1, ForgeCoals.class, level );
		coals.act();
		assertFalse( "cold coals burn by themselves", coals.burning( cell ) );

		Fire fire = Blob.seed( cell, 2, Fire.class, level );
		fire.act();
		coals.act();
		assertTrue( "a flame on the bed lights it", coals.burning( cell ) );
		//the flame that lit it is gone again: from here the bed burns on its own
		fire.fullyClear();

		for (int i = 0; i < 8; i++){
			coals.act();
			TileTemperature.stepDiffusion( level );
		}
		assertTrue( "the anvil's side of a lit forge reaches working heat: " + TileTemperature.tileTemp( cell + 1 ),
				TileTemperature.tileTemp( cell + 1 ) >= 80f );

		for (int i = 0; i < 40; i++){
			coals.act();
			TileTemperature.stepDiffusion( level );
		}
		assertFalse( "the burn ends, and its own leftover heat does not relight it", coals.burning( cell ) );
		assertTrue( "and the bed is still there to be lit again", coals.volume > 0 );
	}

	@Test
	public void elementalLockOpensWhenEveryRuneIsAnswered(){
		floor();
		int w = level.width();
		int ember = 10 + 10 * w, rime = 14 + 10 * w, tide = 18 + 10 * w, gate = 14 + 14 * w;
		Level.set( gate, Terrain.CUSTOM_DECO, level );
		ElementalLock lock = Blob.seed( ember, ElementalLock.EMBER, ElementalLock.class, level );
		Blob.seed( rime, ElementalLock.RIME, ElementalLock.class, level );
		Blob.seed( tide, ElementalLock.TIDE, ElementalLock.class, level );
		lock.gate = gate;

		lock.act();
		assertEquals( "nothing answered, nothing opens", Terrain.CUSTOM_DECO, level.map[gate] );

		level.tileHeat[ember] = 80f;
		level.tileHeat[rime] = -40f;
		lock.act();
		assertEquals( "two of three is not open", Terrain.CUSTOM_DECO, level.map[gate] );

		//the heat goes away again: an awake rune stays awake
		level.tileHeat[ember] = 0f;
		level.tileHeat[rime] = 0f;
		Level.set( tide + 1, Terrain.WATER, level );
		lock.act();
		assertNotEquals( "every rune answered opens the gate", Terrain.CUSTOM_DECO, level.map[gate] );
		assertEquals( "and the lock is spent", 0, lock.volume );
	}

	@Test
	public void counterweightBarsFollowTheWeight(){
		floor();
		int w = level.width();
		int a = 10 + 10 * w, b = 16 + 10 * w, gate = 13 + 14 * w, inside = 13 + 15 * w;
		Level.set( gate, Terrain.CUSTOM_DECO, level );
		CounterweightPlates plates = Blob.seed( a, 1, CounterweightPlates.class, level );
		Blob.seed( b, 1, CounterweightPlates.class, level );
		plates.gate = gate;
		plates.vault = new int[]{ inside };

		level.drop( new Sword(), a );
		plates.act();
		assertEquals( "one plate is not two", Terrain.CUSTOM_DECO, level.map[gate] );

		level.drop( new Gold( 10 ), b );
		plates.act();
		assertEquals( "a few coins weigh nothing", Terrain.CUSTOM_DECO, level.map[gate] );

		level.drop( new Gold( CounterweightPlates.HEAVY_GOLD ), b );
		plates.act();
		assertNotEquals( "gear on one and a real pile of gold on the other", Terrain.CUSTOM_DECO, level.map[gate] );

		level.heaps.get( a ).destroy();
		level.heaps.remove( a );
		plates.act();
		assertEquals( "the weight comes off, the bars come down", Terrain.CUSTOM_DECO, level.map[gate] );
	}

	@Test
	public void pedigreeWardBreaksTheTwoNotTaken(){
		floor();
		int w = level.width();
		int[] plinths = { 10 + 10 * w, 12 + 10 * w, 14 + 10 * w };
		PedigreeWard ward = null;
		for (int cell : plinths){
			level.drop( new Sword(), cell );
			ward = Blob.seed( cell, 1, PedigreeWard.class, level );
		}
		ward.act();
		for (int cell : plinths) assertNotNull( "untouched, all three stay", level.heaps.get( cell ) );

		level.heaps.get( plinths[1] ).pickUp();
		level.heaps.remove( plinths[1] );
		ward.act();
		assertTrue( "the other two are gone",
				(level.heaps.get( plinths[0] ) == null || level.heaps.get( plinths[0] ).isEmpty())
				&& (level.heaps.get( plinths[2] ) == null || level.heaps.get( plinths[2] ).isEmpty()) );
		assertEquals( 0, ward.volume );
	}

	@Test
	public void theBellowsCarriesGasDownwindAndBendsThrows(){
		floor();
		int w = level.width();
		BellowsDraft draft = new BellowsDraft();
		level.blobs.put( BellowsDraft.class, draft );
		for (int x = 5; x <= 25; x++) draft.mark( level, x + 10 * w, 0 );

		ToxicGas gas = Blob.seed( 6 + 10 * w, 200, ToxicGas.class, level );
		for (int turn = 0; turn < 6; turn++){
			draft.act();
			gas.act();
		}
		int upwind = 0, downwind = 0;
		for (int x = 5; x <= 25; x++){
			if (x <= 7) upwind += gas.cur[x + 10 * w]; else downwind += gas.cur[x + 10 * w];
		}
		assertTrue( "the cloud has gone down the gallery (" + upwind + " behind, " + downwind + " ahead)", downwind > upwind * 3 );

		assertTrue( "a throw with the wind hits harder", ClimateManager.windProjectileFactor( 6 + 10 * w, 20 + 10 * w, true ) > 1.2f );
		assertTrue( "a throw into it hits softer", ClimateManager.windProjectileFactor( 20 + 10 * w, 6 + 10 * w, true ) < 0.8f );
		assertEquals( "still air is the weather's business", 1f, ClimateManager.windProjectileFactor( 6 + 30 * w, 20 + 30 * w, true ), 0.2f );
	}

	@Test
	public void aCleanTemperMaxesTheWeakestLineAndOvercookingBurnsOneOut(){
		Item sword = new Sword();
		WarpedRooms.forceQuality( sword, Rarity.RARE, ItemType.GAMMA );
		Quality q = Quality.of( sword );
		assertEquals( 2, q.lines.size() );
		q.rolls.set( 0, 0.9f );
		q.rolls.set( 1, 0.6f );

		assertEquals( 1, q.temper( sword ) );
		assertEquals( "the weakest line is the one raised", 1f, q.rolls.get( 1 ), 0f );
		assertEquals( 0.9f, q.rolls.get( 0 ), 0f );

		assertTrue( q.scorch( sword ) );
		assertEquals( 1, q.lines.size() );

		q.rolls.set( 0, 1f );
		int before = q.masterwork;
		assertEquals( "nothing left to raise: a free masterwork step", 2, q.temper( sword ) );
		assertEquals( before + 1, q.masterwork );
	}

	//a rare sword with two middling lines, in the hero's pack
	private Item spareSword(){
		Item sword = new Sword();
		WarpedRooms.forceQuality( sword, Rarity.RARE, ItemType.GAMMA );
		Quality.of( sword ).rolls.set( 0, 0.8f );
		Quality.of( sword ).rolls.set( 1, 0.5f );
		Dungeon.hero.belongings.backpack.items.add( sword );
		return sword;
	}

	private xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil forge( int coal ){
		Blob.seed( coal, 1, ForgeCoals.class, level );
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil anvil
				= new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil();
		anvil.pos = coal + 1;
		level.mobs.add( anvil );
		int trough = coal + 1 + 3 * level.width();
		Level.set( trough, Terrain.WATER, level );
		anvil.trough = new int[]{ trough };
		return anvil;
	}

	//light the bed and run the forge until the piece is white-hot (or the turns run out)
	private int heatUp( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil anvil, int coal, int turns ){
		ForgeCoals coals = (ForgeCoals) level.blobs.get( ForgeCoals.class );
		Fire fire = Blob.seed( coal, 2, Fire.class, level );
		fire.act();
		coals.act();
		fire.fullyClear();
		for (int t = 1; t <= turns; t++){
			coals.act();
			TileTemperature.stepDiffusion( level );
			anvil.workTurn();
			if (anvil.state() == xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil.HOT) return t;
		}
		return -1;
	}

	@Test
	public void theForgeTempersWhatIsQuenchedInTime(){
		floor();
		int coal = 20 + 20 * level.width();
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil anvil = forge( coal );
		Item sword = spareSword();

		assertTrue( anvil.place( Dungeon.hero, sword ) );
		assertFalse( "it is on the anvil now, not in the pack", Dungeon.hero.belongings.backpack.items.contains( sword ) );
		for (int i = 0; i < 10; i++) anvil.workTurn();
		assertEquals( "a cold forge does nothing", xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil.COLD, anvil.state() );

		int took = heatUp( anvil, coal, 20 );
		assertTrue( "one lighting brings it to white heat, in spring, within the burn: " + took, took > 0 && took <= 14 );

		//the trough is cool water in this weather: the quench takes
		anvil.quenchInTrough( null );
		assertTrue( "the anvil is spent by its one good temper", anvil.spent() );
		assertEquals( null, anvil.held() );
		assertEquals( "the weakest line went to its maximum", 1f, Quality.of( sword ).rolls.get( 1 ), 0f );
		assertEquals( 0.8f, Quality.of( sword ).rolls.get( 0 ), 0f );
	}

	@Test
	public void theForgeBurnsWhatIsLeftInTheHeat(){
		floor();
		int coal = 20 + 20 * level.width();
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil anvil = forge( coal );
		Item sword = spareSword();
		anvil.place( Dungeon.hero, sword );
		assertTrue( heatUp( anvil, coal, 20 ) > 0 );

		//held at working heat and never quenched
		int turns = 0;
		while (Quality.of( sword ).lines.size() == 2 && turns < 40){
			level.tileHeat[anvil.pos] = 200f;
			anvil.workTurn();
			turns++;
		}
		assertEquals( "a line is burnt out of it", 1, Quality.of( sword ).lines.size() );
		assertTrue( "after a fair warning's worth of turns, not at once: " + turns, turns >= 12 );
		assertFalse( "but the anvil is not spent by a failure", anvil.spent() );
		assertEquals( "and the piece is back to cold iron",
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil.COLD, anvil.state() );
	}

	@Test
	public void theForgeWillNotQuenchInAWarmOrFrozenTrough(){
		floor();
		int coal = 20 + 20 * level.width();
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil anvil = forge( coal );
		Item sword = spareSword();
		anvil.place( Dungeon.hero, sword );
		assertTrue( heatUp( anvil, coal, 20 ) > 0 );

		//a heat wave: the trough is as warm as the day
		ClimateManager.debugTempOverride = 40f;
		anvil.quenchInTrough( null );
		assertFalse( "warm water sets nothing", anvil.spent() );
		assertEquals( "and the piece has to be brought back to heat",
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil.COLD, anvil.state() );

		//a hard winter: the trough is ice
		ClimateManager.debugTempOverride = 15f;
		level.tileHeat[anvil.pos] = 200f;
		for (int i = 0; i < 4; i++){ level.tileHeat[anvil.pos] = 200f; anvil.workTurn(); }
		assertEquals( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil.HOT, anvil.state() );
		Level.set( anvil.trough[0], Terrain.FROZEN_WATER, level );
		anvil.quenchInTrough( null );
		assertEquals( "ice quenches nothing: still hot, still waiting",
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TemperingAnvil.HOT, anvil.state() );

		//frost on the anvil itself does the same work as the trough
		level.tileHeat[anvil.pos] = -60f;
		anvil.workTurn();
		assertTrue( "a crash of cold under the piece is a quench", anvil.spent() );
	}

	@Test
	public void anIceBlockHoldsOutAgainstTheWeatherButNotAgainstFire(){
		floor();
		int cell = 20 + 20 * level.width();
		Level.set( cell, Terrain.FROZEN_WATER, level );
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceBlock block = new xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceBlock();
		block.encase( new Sword() );
		block.pos = cell;
		level.mobs.add( block );

		//a mild floor: the block keeps itself, for as long as anyone cares to wait
		ClimateManager.debugTempOverride = 15f;
		boolean melted = false;
		for (int i = 0; i < 300 && !melted; i++){
			melted = block.meltTurn();
			TileTemperature.stepDiffusion( level );
		}
		assertFalse( "a mild season never thaws it (melt " + block.melt() + ")", melted );

		//high summer is the free key, slowly
		ClimateManager.debugTempOverride = 28f;
		int summer = 0;
		while (!melted && summer < 400){
			melted = block.meltTurn();
			TileTemperature.stepDiffusion( level );
			summer++;
		}
		assertTrue( "a hot summer thaws it by itself, but not quickly: " + summer, melted && summer >= 60 );

		//and fire beside it is the quick one, in any season
		ClimateManager.debugTempOverride = 15f;
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceBlock second = new xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceBlock();
		second.encase( new Sword() );
		second.pos = cell + 10;
		Level.set( second.pos, Terrain.FROZEN_WATER, level );
		level.mobs.add( second );
		int turns = 0;
		boolean thawed = false;
		while (!thawed && turns < 80){
			TileTemperature.applyFireHeat( second.pos - 1 );
			TileTemperature.applyFireHeat( second.pos + 1 );
			thawed = second.meltTurn();
			TileTemperature.stepDiffusion( level );
			turns++;
		}
		assertTrue( "fire on both sides thaws it in a reasonable time: " + turns, thawed && turns >= 5 && turns <= 45 );
	}

	@Test
	public void theMarketKeepsTheNight(){
		floor();
		int w = level.width();
		int fire = 10 + 10 * w;
		xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket market
				= Blob.seed( fire, 1, xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket.class, level );
		market.home = 14 + 14 * w;
		market.posts = new int[]{ 12 + 12 * w, 16 + 12 * w };
		WarpedRooms.initForRun();

		xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.debugPhaseOverride
				= xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.DAY;
		try {
			market.act();
			assertEquals( "nobody by day", null, market.dealer() );

			xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.debugPhaseOverride
					= xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.NIGHT;
			market.act();
			assertNotNull( "the dealer sets up at night", market.dealer() );
			assertEquals( "with his two guards", 3, level.mobs.size() );
			assertFalse( "and something to sell", market.stock.isEmpty() );
			for (xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket.Offer offer : market.stock){
				assertTrue( "nothing on the counter is free",
						xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket.priceFor( offer, Dungeon.hero ) > 0 );
				if (offer.kind == xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket.Offer.GEAR){
					assertEquals( Rarity.LEGENDARY.ordinal(), Math.min( Rarity.LEGENDARY.ordinal(),
							Quality.of( offer.item ).rarity.ordinal() ) );
					assertFalse( "gear comes unidentified", offer.item.cursedKnown );
				}
			}

			//the ledger survives a save
			Bundle bundle = new Bundle();
			market.storeInBundle( bundle );
			xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket loaded
					= new xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket();
			loaded.restoreFromBundle( bundle );
			assertEquals( market.stock.size(), loaded.stock.size() );
			assertEquals( market.home, loaded.home );

			xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.debugPhaseOverride
					= xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.DAWN;
			market.act();
			assertEquals( "and is gone by dawn, guards and all", 0, level.mobs.size() );

			//steel drawn: gone for good, and the guards turn
			xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.debugPhaseOverride
					= xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase.NIGHT;
			market.act();
			market.provoke();
			assertTrue( WarpedRooms.banned );
			assertEquals( null, market.dealer() );
			for (Mob mob : level.mobs) assertEquals( xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ENEMY, mob.alignment );
			market.act();
			assertEquals( "no dealer sets up for a banned hero", null, market.dealer() );
		} finally {
			xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.debugPhaseOverride = null;
			WarpedRooms.initForRun();
		}
	}

	@Test
	public void theNestBroodsOnWhateverTheTileIs(){
		floor();
		int nest = 20 + 20 * level.width();
		xyz.gabriwar.warpedpixeldungeon.actors.blobs.NestBrood brood
				= Blob.seed( nest, 1, xyz.gabriwar.warpedpixeldungeon.actors.blobs.NestBrood.class, level );
		xyz.gabriwar.warpedpixeldungeon.items.Egg egg = new xyz.gabriwar.warpedpixeldungeon.items.Egg();
		level.drop( egg, nest );

		for (int i = 0; i < 12; i++) brood.act();
		assertEquals( "a mild tile broods nothing", 0, egg.burns + egg.freezes + egg.poisons + egg.lits );

		level.tileHeat[nest] = 60f;
		for (int i = 0; i < 12; i++) brood.act();
		assertEquals( "three brooding ticks of heat", 3, egg.burns );

		level.tileHeat[nest] = -40f;
		for (int i = 0; i < 8; i++) brood.act();
		assertEquals( "two of frost", 2, egg.freezes );
		assertFalse( brood.tileDesc().contains( Messages.NO_TEXT_FOUND ) );
	}

	@Test
	public void theHothouseGrowsOnItsOwnClock(){
		floor();
		int w = level.width();
		xyz.gabriwar.warpedpixeldungeon.actors.blobs.HothouseGlass glass = null;
		for (int x = 10; x <= 14; x++){
			for (int y = 10; y <= 12; y++){
				glass = Blob.seed( x + y * w, 1, xyz.gabriwar.warpedpixeldungeon.actors.blobs.HothouseGlass.class, level );
			}
		}
		Dungeon.cycleTurn = 1000;
		glass.act();
		assertEquals( "the first turn only starts the clock", 0, level.plants.size );

		Dungeon.cycleTurn += 125 * 3;
		glass.act();
		assertEquals( "three growth ticks, three plants", 3, level.plants.size );
		for (int cell : level.plants.keyArray()){
			assertTrue( "and only in the beds", xyz.gabriwar.warpedpixeldungeon.actors.blobs.HothouseGlass.covers( cell ) );
		}

		Dungeon.cycleTurn += 125 * 100;
		glass.act();
		assertTrue( "a long absence is caught up, to a cap", level.plants.size <= 3 + 8 );
	}

	// ---------------------------------------------------------------- the slot

	@Test
	public void theSlotPacesItsRoomsAndTheMarketComesOncePerChapter(){
		WarpedRooms.initForRun();
		int markets = 0, slotRooms = 0, lastSlot = -10;
		HashSet<Class<?>> seen = new HashSet<>();
		for (int depth = 1; depth <= 24; depth++){
			Dungeon.depth = depth;
			if (Dungeon.bossLevel( depth )) continue;
			ArrayList<Room> rooms = new ArrayList<>();
			WarpedRooms.addRooms( level, rooms );
			//a floor that fails to build asks again, and must get the same answer
			ArrayList<Room> again = new ArrayList<>();
			WarpedRooms.addRooms( level, again );
			assertEquals( "depth " + depth + " is not stable across a rebuild", rooms.size(), again.size() );
			for (int i = 0; i < rooms.size(); i++) assertEquals( rooms.get( i ).getClass(), again.get( i ).getClass() );

			for (Room r : rooms){
				if (r instanceof BlackMarketRoom){
					markets++;
					int floor = (depth - 1) % 5 + 1;
					assertTrue( "the market is on floor 2-4 of its chapter, not " + floor, floor >= 2 && floor <= 4 );
				} else {
					slotRooms++;
					assertTrue( "slot rooms come at least two floors apart (depth " + depth + ")", depth - lastSlot >= 2 );
					lastSlot = depth;
					assertTrue( "no repeats before the queue is through: " + r.getClass().getSimpleName(), seen.add( r.getClass() ) );
				}
			}
		}
		assertEquals( "one market per chapter", 5, markets );
		assertTrue( "a run sees most of the slot's rooms: " + slotRooms, slotRooms >= 6 && slotRooms <= 10 );
	}

	@Test
	public void theSlotSurvivesASave(){
		WarpedRooms.initForRun();
		WarpedRooms.addHeat( 2 );
		WarpedRooms.paid( 1234 );
		Dungeon.depth = 3;
		WarpedRooms.addRooms( level, new ArrayList<Room>() );

		Bundle bundle = new Bundle();
		WarpedRooms.storeInBundle( bundle );
		ArrayList<Class<? extends Room>> queue = new ArrayList<>( WarpedRooms.runQueue );

		WarpedRooms.initForRun();
		WarpedRooms.restoreFromBundle( bundle );
		assertEquals( queue, WarpedRooms.runQueue );
		assertEquals( 2, WarpedRooms.heat );
		assertEquals( 1234, WarpedRooms.goldSpent );
		assertFalse( WarpedRooms.banned );
	}
}

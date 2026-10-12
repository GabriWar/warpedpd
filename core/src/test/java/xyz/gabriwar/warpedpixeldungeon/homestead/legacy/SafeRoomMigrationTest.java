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

package xyz.gabriwar.warpedpixeldungeon.homestead.legacy;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokoban;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Spider;
import xyz.gabriwar.warpedpixeldungeon.items.BuildersTool;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.OtilukesJournal;
import xyz.gabriwar.warpedpixeldungeon.items.PortableChest;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.FadedSafeSpotPage;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfIdentify;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shortsword;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.DeadEndLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WornDartTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WindowGenerator;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.LoadingSplash;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The safe room's removal (homestead/legacy/SafeRoomMigration) on saves from before it: the old
 * room's file emptied into one chest in the town (heaps, chests packed with their contents, the
 * crops' seeds, the amenities' gold, the pet), once and only once; the hero standing in it, or a
 * beacon set there, landing on the town's plaza; the old page traded for a builder's tool or its
 * worth in gold, from the pack and from Otiluke's Journal, whose other rooms keep their numbers
 * and whose way home still works. A whole old save is written and loaded through Dungeon's own
 * saveGame / loadGame.
 */
public class SafeRoomMigrationTest {

	private static final long SEED = 0x5AFE0F7EA7L;
	private static final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
	//a slot of its own, in the build's test-saves folder
	private static final int SLOT = 9;
	private static final String OLD_PAGE = "xyz.gabriwar.warpedpixeldungeon.items.journalpages.SafeSpotPage";

	private int depth, branch, gold, deepest, startDay;
	private DayNightCycle.Phase override;
	private xyz.gabriwar.warpedpixeldungeon.levels.Level savedLevel;
	private Hero savedHero;
	private Hero hero;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
		SafeRoomMigration.registerAliases();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		gold = Dungeon.gold;
		deepest = Statistics.deepestFloor;
		startDay = Dungeon.calendarStartDay;
		override = DayNightCycle.debugPhaseOverride;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Random.pushGenerator( 5050 );
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Dungeon.gold = 0;
		Dungeon.calendarStartDay = 0;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		Statistics.deepestFloor = 12;
		hero = new Hero();
		hero.lvl = 10;
		Dungeon.hero = hero;
		Notes.reset();
		SafeRoomMigration.reset();
	}

	@After
	public void tearDown(){
		Random.popGenerator();
		Actor.clear();
		SafeRoomMigration.reset();
		clearPendingArrival();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.gold = gold;
		Statistics.deepestFloor = deepest;
		Dungeon.calendarStartDay = startDay;
		DayNightCycle.debugPhaseOverride = override;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		Dungeon.generatedLevels.remove( Integer.valueOf( SafeRoomMigration.DEPTH ) );
		for (String f : FileUtils.filesInDir( GamesInProgress.gameFolder( SLOT ) )){
			FileUtils.deleteFile( GamesInProgress.gameFolder( SLOT ) + "/" + f );
		}
	}

	// ------------------------------------------------------------------ helpers

	private static final String OLD_LEVEL = "xyz.gabriwar.warpedpixeldungeon.levels.SafeLevel";

	//the old room as the removed SafeLevel saved itself: Level.storeInBundle's own keys (heaps,
	//mobs, plants, traps, blobs, the map...) and then its own (the prize pool and the switch
	//spots). Written through Dungeon.saveLevel's "level" key and renamed to the old class
	public static class OldSafeRoom extends DeadEndLevel {
		{
			transitions = new ArrayList<>();
			mobs = new java.util.HashSet<>();
			heaps = new com.watabou.utils.SparseArray<>();
			blobs = new java.util.HashMap<>();
			plants = new com.watabou.utils.SparseArray<>();
			traps = new com.watabou.utils.SparseArray<>();
			customTiles = new ArrayList<>();
			customTerrain = new ArrayList<>();
			customWalls = new ArrayList<>();
			setSize( 96, 96 );
		}

		@Override
		public void storeInBundle( Bundle bundle ){
			super.storeInBundle( bundle );
			//the room's prize pool: never the hero's (the room's own key, a meal for a revisit)
			bundle.put( "heapstogen", new ArrayList<Item>( Arrays.asList( new IronKey( SafeRoomMigration.DEPTH ), new Food() ) ) );
			for (String k : new String[]{ "heapgenspots", "teleportspots", "portswitchspots",
					"destinationspots", "destinationassign", "teleportassign" }){
				bundle.put( k, new int[10] );
			}
			bundle.put( "prizeNo", 2 );
		}
	}

	//what the old room might have held: two heaps, a golden chest with two things in it, a crop,
	//one of the room's sheep, a pet, an ally that is not the pet, a plant, a trap, the prize pool,
	//an alchemy pot and an enchanting pedestal
	private static Bundle oldRoom(){
		OldSafeRoom room = new OldSafeRoom();
		Arrays.fill( room.map, Terrain.EMPTY );
		room.map[700] = Terrain.ALCHEMY;
		room.map[800] = Terrain.PEDESTAL;
		room.map[900] = Terrain.PEDESTAL;
		for (Heap h : roomHeaps()) room.heaps.put( h.pos, h );
		room.mobs.addAll( roomMobs() );
		Rat ally = new Rat();
		ally.pos = 650;
		ally.alignment = Char.Alignment.ALLY;
		room.mobs.add( ally );
		Sungrass plant = new Sungrass();
		plant.pos = 750;
		room.plants.put( plant.pos, plant );
		WornDartTrap trap = new WornDartTrap();
		trap.pos = 850;
		room.traps.put( trap.pos, trap );

		Bundle file = new Bundle();
		file.put( "level", room );
		return renamed( roundTrip( file ), OldSafeRoom.class.getName(), OLD_LEVEL );
	}

	//the same room as the debug scene writes it (SafeRoomMigration.roomFile)
	private static Bundle sceneRoom(){
		int[] map = new int[96 * 96];
		Arrays.fill( map, Terrain.EMPTY );
		map[700] = Terrain.ALCHEMY;
		map[800] = Terrain.PEDESTAL;
		map[900] = Terrain.PEDESTAL;

		return roundTrip( SafeRoomMigration.roomFile( roomHeaps(), roomMobs(), map ) );
	}

	private static ArrayList<Heap> roomHeaps(){
		Heap a = new Heap();
		a.pos = 100;
		a.drop( new Shortsword() );
		a.drop( new Gold( 250 ) );
		Heap b = new Heap();
		b.pos = 200;
		b.type = Heap.Type.CHEST;
		b.drop( new PotionOfHealing() );
		return new ArrayList<>( Arrays.asList( a, b ) );
	}

	private static ArrayList<Mob> roomMobs(){
		StorageChest chest = new StorageChest();
		chest.pos = 300;
		chest.type = PortableChest.GOLDEN;
		chest.storage.cap = PortableChest.capacityFor( PortableChest.GOLDEN );
		chest.storage.items.add( new ScrollOfIdentify() );
		chest.storage.items.add( new Food() );

		FarmCrop crop = new FarmCrop();
		crop.pos = 400;
		crop.sow( new Sungrass.Seed() );

		SheepSokoban sheep = new SheepSokoban();
		sheep.pos = 500;

		Spider pet = new Spider();
		pet.pos = 600;
		pet.type = 1;
		pet.level = 4;
		pet.kills = 17;
		pet.HP = 21;
		pet.experience = 33;
		pet.cooldown = 5;
		return new ArrayList<>( Arrays.asList( chest, crop, sheep, pet ) );
	}

	//as a file would give it back: written out and read in again
	private static Bundle roundTrip( Bundle b ){
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Bundle.write( b, out );
			return Bundle.read( new ByteArrayInputStream( out.toByteArray() ) );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	//a bundle read back with a removed class's name where a live one was written
	private static Bundle renamed( Bundle b, String from, String to ){
		try {
			String text = b.toString().replace( from, to );
			return Bundle.read( new ByteArrayInputStream( text.getBytes( StandardCharsets.UTF_8 ) ) );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	private static int count( Iterable<Item> items, Class<? extends Item> cls ){
		int n = 0;
		for (Item i : items) if (cls.isInstance( i )) n++;
		return n;
	}

	private static Item first( Iterable<Item> items, Class<? extends Item> cls ){
		for (Item i : items) if (cls.isInstance( i )) return i;
		return null;
	}

	private static void clearPendingArrival(){
		try {
			Field f = OverworldLevel.class.getDeclaredField( "pendingArrival" );
			f.setAccessible( true );
			f.set( null, false );
		} catch (Exception e){
			throw new AssertionError( e );
		}
	}

	//the live surface window round a world cell, made the host's own
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
		return ow;
	}

	private static Bundle waitingBundle( Item... items ){
		Bundle b = new Bundle();
		b.put( "safe_room_migrated", true );
		b.put( "safe_room_belongings", new ArrayList<>( Arrays.asList( items ) ) );
		return roundTrip( b );
	}

	// ------------------------------------------------------------------ tests

	@Test
	public void everyLineTheHeroReadsIsWritten(){
		String[][] keys = {
				{ "homestead.legacy.saferoommigration", "moved" }, { "homestead.legacy.saferoommigration", "waiting" },
				{ "homestead.legacy.saferoommigration", "delivered" }, { "homestead.legacy.saferoommigration", "refund" },
				{ "homestead.legacy.saferoommigration", "pet" }, { "homestead.legacy.saferoommigration", "page_tool" },
				{ "homestead.legacy.saferoommigration", "page_gold" },
				{ "items.journalpages.fadedsafespotpage", "name" }, { "items.journalpages.fadedsafespotpage", "desc" },
				{ "items.journalpages.fadedsafespotpage", "ac_read" },
				{ "items.builderstool", "not_here" }, { "items.builderstool", "desc" },
				{ "items.portablechest", "only_world" }, { "items.portablechest", "desc" },
				{ "levels.features.chasm", "safe_fall" } };
		for (String[] k : keys){
			String text = Messages.get( k[0] + "." + k[1] );
			assertNotEquals( k[0] + "." + k[1], Messages.NO_TEXT_FOUND, text );
			assertFalse( k[0] + "." + k[1], text.toLowerCase( java.util.Locale.ENGLISH ).contains( "safe zone" ) );
		}
		assertEquals( "faded safe spot page", new FadedSafeSpotPage().name() );
		assertNotEquals( Messages.NO_TEXT_FOUND, new FadedSafeSpotPage().desc() );
		for (String gone : new String[]{ "items.builderstool.not_safe", "items.portablechest.only_safe",
				"items.portablechest.need_tool", "items.otilukesjournal.room_safe",
				"journal.document.adventurers_guide.safezone.title" }){
			assertEquals( gone, Messages.NO_TEXT_FOUND, Messages.get( gone ) );
		}
	}

	@Test
	public void anOldSavesPageAndLevelClassStillRestore(){
		Bundle b = new Bundle();
		b.put( "page", new FadedSafeSpotPage() );
		b.put( "level", DeadEndLevel.class );
		Bundle old = renamed( b, FadedSafeSpotPage.class.getName(), OLD_PAGE );
		old = renamed( old, DeadEndLevel.class.getName(), "xyz.gabriwar.warpedpixeldungeon.levels.SafeLevel" );
		assertTrue( old.toString().contains( OLD_PAGE ) );
		assertTrue( "the old page comes back faded", old.get( "page" ) instanceof FadedSafeSpotPage );
		assertEquals( "the old level class restores as a dead end", DeadEndLevel.class, old.getClass( "level" ) );
	}

	@Test
	public void theRoomsFileGivesUpEverythingTheHeroKeptThere(){
		SafeRoomMigration.Haul haul = SafeRoomMigration.collect( oldRoom() );

		//both heaps, whole
		assertNotNull( first( haul.items, Shortsword.class ) );
		assertNotNull( first( haul.items, PotionOfHealing.class ) );
		//the chest, packed with all it held and its own room
		PortableChest kit = (PortableChest) first( haul.items, PortableChest.class );
		assertNotNull( "the storage chest comes back as its kit", kit );
		assertEquals( PortableChest.GOLDEN, kit.type );
		assertEquals( 2, kit.storage.items.size() );
		assertNotNull( first( kit.storage.items, ScrollOfIdentify.class ) );
		assertNotNull( first( kit.storage.items, Food.class ) );
		assertEquals( PortableChest.capacityFor( PortableChest.GOLDEN ), kit.storage.capacity() );
		//the crop's seed
		assertNotNull( "the crop's seed", first( haul.items, Sungrass.Seed.class ) );
		//one pot and two pedestals, in gold, beside the heap's own 250
		assertEquals( SafeRoomMigration.ALCHEMY_REFUND + 2 * SafeRoomMigration.PEDESTAL_REFUND, haul.refund );
		int goldSum = 0;
		for (Item i : haul.items) if (i instanceof Gold) goldSum += i.quantity();
		assertEquals( 250 + haul.refund, goldSum );
		//the pet, as it was
		assertNotNull( haul.pet );
		assertEquals( Arrays.toString( new int[]{ 1, 4, 17, 21, 33, 5 } ), Arrays.toString( haul.pet ) );
		//nothing else: the sheep, the ally that is not the pet, the plant, the trap and the room's
		//prize pool stay behind, and nothing turns into anything
		assertEquals( 3 + 1 + 1 + 1, haul.items.size() );
		assertNull( first( haul.items, IronKey.class ) );
		assertEquals( "the only food is the chest's own", 0, count( haul.items, Food.class ) );
	}

	@Test
	public void theDebugScenesFileReadsAsAnOldSavesFile(){
		SafeRoomMigration.Haul real = SafeRoomMigration.collect( oldRoom() );
		SafeRoomMigration.Haul scene = SafeRoomMigration.collect( sceneRoom() );
		assertEquals( real.items.size(), scene.items.size() );
		assertEquals( real.refund, scene.refund );
		assertEquals( Arrays.toString( real.pet ), Arrays.toString( scene.pet ) );
		for (Item i : real.items){
			assertEquals( i.getClass().getName(), count( real.items, i.getClass() ), count( scene.items, i.getClass() ) );
		}
	}

	@Test
	public void anEmptyOrForeignFileGivesNothing(){
		SafeRoomMigration.Haul haul = SafeRoomMigration.collect( new Bundle() );
		assertTrue( haul.items.isEmpty() );
		assertNull( haul.pet );
		assertEquals( 0, haul.refund );
	}

	@Test
	public void theFileIsEmptiedOnceAndGoesWithTheNextSave() throws Exception {
		String file = GamesInProgress.depthFile( SLOT, SafeRoomMigration.DEPTH, 0 );
		FileUtils.bundleToFile( file, oldRoom() );
		Dungeon.generatedLevels.add( SafeRoomMigration.DEPTH );
		Notes.add( new IronKey( SafeRoomMigration.DEPTH ) );
		Notes.add( new IronKey( 3 ) );
		hero.haspet = true;
		hero.petfollow = false;
		//a save from before: no flag
		SafeRoomMigration.restoreFromBundle( new Bundle() );
		assertFalse( SafeRoomMigration.done() );

		SafeRoomMigration.migrate( SLOT );

		assertTrue( SafeRoomMigration.done() );
		assertEquals( 6, SafeRoomMigration.waiting().size() );
		assertFalse( "the old depth is never loaded again", Dungeon.levelHasBeenGenerated( SafeRoomMigration.DEPTH, 0 ) );
		assertEquals( "its key is forgotten", 0, Notes.keyCount( new IronKey( SafeRoomMigration.DEPTH ) ) );
		assertEquals( "other floors' keys stay", 1, Notes.keyCount( new IronKey( 3 ) ) );
		assertTrue( "the pet follows again", hero.petfollow );
		assertEquals( 4, hero.petLevel );
		assertEquals( 17, hero.petKills );
		//no chest has been laid yet, and the file stays until a whole save carries its things
		assertTrue( FileUtils.fileExists( file ) );
		assertFalse( SafeRoomMigration.afterSaveAll( SLOT + 1 ) );
		assertTrue( "another slot's save leaves it", FileUtils.fileExists( file ) );
		assertFalse( "nothing laid: the game's file stands", SafeRoomMigration.afterSaveAll( SLOT ) );
		assertFalse( "this slot's save takes it", FileUtils.fileExists( file ) );

		//a second load finds nothing more to carry
		FileUtils.bundleToFile( file, oldRoom() );
		SafeRoomMigration.migrate( SLOT );
		assertEquals( 6, SafeRoomMigration.waiting().size() );
		SafeRoomMigration.afterSaveAll( SLOT );
		assertTrue( "a migrated save never touches the file again", FileUtils.fileExists( file ) );
	}

	@Test
	public void whatWaitsIsSavedWithTheGame(){
		SafeRoomMigration.restoreFromBundle( waitingBundle( new Shortsword(), new Gold( 40 ) ) );
		Bundle b = new Bundle();
		SafeRoomMigration.storeInBundle( b );
		SafeRoomMigration.reset();
		assertTrue( SafeRoomMigration.waiting().isEmpty() );
		SafeRoomMigration.restoreFromBundle( roundTrip( b ) );
		assertTrue( SafeRoomMigration.done() );
		assertEquals( 2, SafeRoomMigration.waiting().size() );
		assertNotNull( first( new ArrayList<>( SafeRoomMigration.waiting() ), Shortsword.class ) );
	}

	@Test
	public void onlyTheOldRoomSendsTheHeroToTheTown(){
		Dungeon.depth = 51;
		assertFalse( SafeRoomMigration.relocate() );
		assertEquals( 51, Dungeon.depth );
		Dungeon.depth = SafeRoomMigration.DEPTH;
		Dungeon.branch = 1;
		assertFalse( SafeRoomMigration.relocate() );
		Dungeon.branch = 0;
		assertFalse( OverworldLevel.hasPendingArrival() );
		assertTrue( SafeRoomMigration.relocate() );
		assertEquals( OverworldLevel.DEPTH, Dungeon.depth );
		assertTrue( "he lands on the plaza", OverworldLevel.hasPendingArrival() );
	}

	@Test
	public void aPageBecomesAToolThenGold(){
		assertTrue( new FadedSafeSpotPage().collect( hero.belongings.backpack ) );
		OtilukesJournal journal = new OtilukesJournal();
		journal.journalLevel = 3;
		journal.rooms[0] = true;
		journal.firsts[0] = true;
		journal.rooms[2] = true;
		assertTrue( journal.collect( hero.belongings.backpack ) );

		SafeRoomMigration.settle( hero );

		assertNull( "the page is gone", hero.belongings.getItem( FadedSafeSpotPage.class ) );
		assertNotNull( "the first page bought a builder's tool", hero.belongings.getItem( BuildersTool.class ) );
		assertEquals( "the second, its worth in gold at the town's scale", SafeRoomMigration.pageGold(), Dungeon.gold );
		assertEquals( SafeRoomMigration.PAGE_GOLD * (12 / 5 + 1), SafeRoomMigration.pageGold() );
		assertFalse( journal.rooms[0] );
		assertFalse( journal.firsts[0] );
		assertTrue( "the other rooms keep their numbers", journal.rooms[2] );
		assertEquals( 2, journal.journalLevel );

		//settling again changes nothing
		int before = Dungeon.gold;
		SafeRoomMigration.settle( hero );
		assertEquals( before, Dungeon.gold );
		assertEquals( 1, hero.belongings.getAllItems( BuildersTool.class ).size() );
	}

	@Test
	public void aToolWaitingInTheTownCountsAsOwned(){
		PortableChest kit = new PortableChest( PortableChest.WOOD );
		kit.storage.items.add( new BuildersTool() );
		SafeRoomMigration.restoreFromBundle( waitingBundle( kit ) );
		SafeRoomMigration.redeem( hero );
		assertNull( hero.belongings.getItem( BuildersTool.class ) );
		assertEquals( SafeRoomMigration.pageGold(), Dungeon.gold );
	}

	@Test
	public void pickingUpAFadedPageTradesItIn(){
		FadedSafeSpotPage page = new FadedSafeSpotPage();
		assertEquals( "read it to trade it in by hand", FadedSafeSpotPage.AC_READ, page.defaultAction() );
		assertTrue( page.actions( hero ).contains( FadedSafeSpotPage.AC_READ ) );
		assertTrue( page.doPickUp( hero, hero.pos ) );
		assertNull( "never kept", hero.belongings.getItem( FadedSafeSpotPage.class ) );
		assertNotNull( hero.belongings.getItem( BuildersTool.class ) );

		assertTrue( new FadedSafeSpotPage().doPickUp( hero, hero.pos ) );
		assertNull( hero.belongings.getItem( FadedSafeSpotPage.class ) );
		assertEquals( 1, hero.belongings.getAllItems( BuildersTool.class ).size() );
		assertEquals( SafeRoomMigration.pageGold(), Dungeon.gold );
	}

	@Test
	public void theJournalStillLeadsHomeAndKeepsItsRooms(){
		//away through the old room: the hero woke in the town, the journal still knows his way back
		OtilukesJournal journal = new OtilukesJournal();
		journal.returnDepth = 7;
		journal.returnPos = 321;
		journal.journalLevel = 2;
		journal.rooms[0] = true;
		assertTrue( journal.collect( hero.belongings.backpack ) );
		SafeRoomMigration.settle( hero );
		Dungeon.depth = OverworldLevel.DEPTH;
		assertTrue( "RETURN is offered in the town", journal.actions( hero ).contains( OtilukesJournal.AC_RETURN ) );
		assertEquals( 7, journal.returnDepth );
		//no room left: no READ, however charged
		journal.charge = OtilukesJournal.FULL_CHARGE;
		assertFalse( journal.actions( hero ).contains( OtilukesJournal.AC_PORT ) );

		//the other pages keep their depths; the old room's slot leads nowhere it used to
		int[] want = { 0, 51, 52, 53, 54, 97, 66, 67 };
		for (int page = 1; page < want.length; page++){
			assertEquals( "page " + page, want[page], LoadingSplash.portalDepth( InterlevelScene.Mode.JOURNAL, page ) );
		}
		assertNotEquals( SafeRoomMigration.DEPTH, LoadingSplash.portalDepth( InterlevelScene.Mode.JOURNAL, 0 ) );
	}

	@Test
	public void theChestIsLaidOnceBesideThePlaza(){
		SafeRoomMigration.restoreFromBundle( waitingBundle( new Shortsword(), new Sungrass.Seed(), new Gold( 2300 ) ) );
		int px = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), py = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );

		//not on a slice above or below, nor out of the plaza's sight
		OverworldLevel far = windowAt( px + 400, py + 400 );
		assertEquals( -1, SafeRoomMigration.chestCell( far ) );
		assertFalse( SafeRoomMigration.deliver( far ) );
		assertEquals( 3, SafeRoomMigration.waiting().size() );

		OverworldLevel ow = windowAt( px, py );
		int plaza = ow.localCell( px, py );
		int cell = SafeRoomMigration.chestCell( ow );
		assertNotEquals( -1, cell );
		assertNotEquals( "never on the plaza itself", plaza, cell );
		assertTrue( "beside it", ow.distance( plaza, cell ) <= SafeRoomMigration.REACH );
		assertTrue( ow.passable[cell] );
		assertEquals( "the same cell every time", cell, SafeRoomMigration.chestCell( ow ) );

		assertTrue( SafeRoomMigration.deliver( ow ) );
		Heap heap = ow.heaps.get( cell );
		assertNotNull( heap );
		assertEquals( Heap.Type.CHEST, heap.type );
		assertEquals( 3, heap.items.size() );
		assertNotNull( first( heap.items, Shortsword.class ) );
		assertNotNull( first( heap.items, Sungrass.Seed.class ) );
		assertEquals( 2300, first( heap.items, Gold.class ).quantity() );
		assertTrue( SafeRoomMigration.waiting().isEmpty() );
		assertFalse( "once", SafeRoomMigration.deliver( ow ) );
		assertFalse( SafeRoomMigration.notes().isEmpty() );
	}

	@Test
	public void aWholeOldSaveLoadsIntoTheTown() throws Exception {
		Dungeon.seed = 0x5AFEL;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.init();
		Statistics.deepestFloor = 12;
		Dungeon.hero.haspet = true;
		Dungeon.hero.petfollow = false;
		assertTrue( new FadedSafeSpotPage().collect( Dungeon.hero.belongings.backpack ) );
		assertNull( Dungeon.hero.belongings.getItem( BuildersTool.class ) );
		Dungeon.saveGame( SLOT );

		//the save as the last version wrote it: the hero in the safe room, with its page in his
		//pack, the room's file beside it and no migration flag
		Bundle game = renamed( FileUtils.bundleFromFile( GamesInProgress.gameFile( SLOT ) ),
				FadedSafeSpotPage.class.getName(), OLD_PAGE );
		game.put( "depth", SafeRoomMigration.DEPTH );
		game.put( "branch", 0 );
		game.remove( "safe_room_migrated" );
		int[] generated = game.getIntArray( "generated_levels" );
		int[] withRoom = Arrays.copyOf( generated, generated.length + 1 );
		withRoom[generated.length] = SafeRoomMigration.DEPTH;
		game.put( "generated_levels", withRoom );
		FileUtils.bundleToFile( GamesInProgress.gameFile( SLOT ), game );
		String room = GamesInProgress.depthFile( SLOT, SafeRoomMigration.DEPTH, 0 );
		FileUtils.bundleToFile( room, oldRoom() );
		SafeRoomMigration.reset();
		Dungeon.gold = 0;

		Dungeon.loadGame( SLOT );

		assertEquals( "the hero wakes on the surface", OverworldLevel.DEPTH, Dungeon.depth );
		assertTrue( "on the town's plaza", OverworldLevel.hasPendingArrival() );
		assertTrue( SafeRoomMigration.done() );
		assertFalse( Dungeon.levelHasBeenGenerated( SafeRoomMigration.DEPTH, 0 ) );
		assertEquals( "everything the room held waits in the town", 6, SafeRoomMigration.waiting().size() );
		assertNull( "the page is traded in", Dungeon.hero.belongings.getItem( FadedSafeSpotPage.class ) );
		assertNotNull( "for a builder's tool", Dungeon.hero.belongings.getItem( BuildersTool.class ) );
		assertTrue( "the pet follows again", Dungeon.hero.petfollow );
		assertTrue( "the hero hears of it once the game shows", SafeRoomMigration.notes().size() >= 2 );

		//the next save carries the chest; the old file goes once the level is written too
		Dungeon.saveGame( SLOT );
		assertTrue( "the game's file alone is not the whole save", FileUtils.fileExists( room ) );
		assertFalse( "nothing laid yet: the game's file stands", SafeRoomMigration.afterSaveAll( SLOT ) );
		assertFalse( FileUtils.fileExists( room ) );
		Bundle saved = FileUtils.bundleFromFile( GamesInProgress.gameFile( SLOT ) );
		assertTrue( saved.getBoolean( "safe_room_migrated" ) );
		int carried = 0;
		for (Bundlable b : saved.getCollection( "safe_room_belongings" )) if (b instanceof Item) carried++;
		assertEquals( 6, carried );

		//and loading that save again carries nothing twice
		clearPendingArrival();
		Dungeon.loadGame( SLOT );
		assertEquals( 6, SafeRoomMigration.waiting().size() );
		assertEquals( 1, Dungeon.hero.belongings.getAllItems( BuildersTool.class ).size() );
	}

	@Test
	public void whatWasLaidStaysInTheGamesSaveUntilTheLevelIsWritten(){
		SafeRoomMigration.restoreFromBundle( waitingBundle( new Shortsword(), new Gold( 90 ) ) );
		int px = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), py = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
		OverworldLevel ow = windowAt( px, py );
		assertTrue( SafeRoomMigration.deliver( ow ) );
		assertTrue( SafeRoomMigration.waiting().isEmpty() );

		//the game's file written before the level's: it still holds what the chest holds
		Bundle game = new Bundle();
		SafeRoomMigration.storeInBundle( game );
		game = roundTrip( game );
		int kept = 0;
		for (Bundlable b : game.getCollection( "safe_room_belongings" )) if (b instanceof Item) kept++;
		assertEquals( 2, kept );

		//a crash before the level's file is written: the next load lays the chest again
		SafeRoomMigration.restoreFromBundle( game );
		assertEquals( 2, SafeRoomMigration.waiting().size() );
		OverworldLevel again = windowAt( px, py );
		int cell = SafeRoomMigration.chestCell( again );
		assertTrue( SafeRoomMigration.deliver( again ) );
		assertEquals( 2, again.heaps.get( cell ).items.size() );

		//a save of another level lets nothing go; the save of the chest's own does, once
		Dungeon.level = ow;
		assertFalse( SafeRoomMigration.afterSaveAll( SLOT ) );
		Dungeon.level = again;
		assertTrue( "the game's file is written again without them", SafeRoomMigration.afterSaveAll( SLOT ) );
		assertFalse( SafeRoomMigration.afterSaveAll( SLOT ) );
		Bundle after = new Bundle();
		SafeRoomMigration.storeInBundle( after );
		assertFalse( after.contains( "safe_room_belongings" ) );
	}

	@Test
	public void anItemAChallengeForbidsNeverSwallowsTheRest(){
		int challenges = Dungeon.challenges;
		try {
			Dungeon.challenges = xyz.gabriwar.warpedpixeldungeon.Challenges.NO_HERBALISM;
			SafeRoomMigration.restoreFromBundle( waitingBundle(
					new xyz.gabriwar.warpedpixeldungeon.items.Dewdrop(), new Shortsword(), new Gold( 60 ) ) );
			int px = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), py = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
			OverworldLevel ow = windowAt( px, py );
			int cell = SafeRoomMigration.chestCell( ow );
			assertTrue( SafeRoomMigration.deliver( ow ) );
			Heap heap = ow.heaps.get( cell );
			assertNotNull( "the chest is laid", heap );
			assertEquals( Heap.Type.CHEST, heap.type );
			assertEquals( 2, heap.items.size() );
			assertNotNull( first( heap.items, Shortsword.class ) );
			assertNull( first( heap.items, xyz.gabriwar.warpedpixeldungeon.items.Dewdrop.class ) );
		} finally {
			Dungeon.challenges = challenges;
		}
	}

	@Test
	public void theChestIsLaidWhenTheWindowWalksOntoThePlaza(){
		SafeRoomMigration.restoreFromBundle( waitingBundle( new Shortsword(), new Gold( 70 ) ) );
		int px = WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ), py = WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA );
		//the plaza 12 cells west of the window: one rebase west brings it 20 cells in
		OverworldLevel ow = windowAt( px + W / 2 + 12, py );
		assertEquals( -1, ow.localCell( px, py ) );
		assertFalse( SafeRoomMigration.deliver( ow ) );

		//the hero walks into the west margin, past half of it: the window moves at once
		hero.pos = -1;
		for (int x = 8; x < 30 && hero.pos == -1; x++){
			for (int dy = 0; dy < 40 && hero.pos == -1; dy++){
				for (int sgn : new int[]{ 1, -1 }){
					int c = x + (H / 2 + sgn * dy) * W;
					if (ow.passable[c] && !ow.pit[c] && !ow.avoid[c] && Actor.findChar( c ) == null
							&& ow.getTransition( c ) == null){
						hero.pos = c;
						break;
					}
				}
			}
		}
		assertNotEquals( -1, hero.pos );
		hero.viewDistance = ow.viewDistance;
		int wx = ow.worldX;
		ow.occupyCell( hero );
		assertNotEquals( "the window moved", wx, ow.worldX );

		int plaza = ow.localCell( px, py );
		assertNotEquals( -1, plaza );
		assertTrue( "laid with the move", SafeRoomMigration.waiting().isEmpty() );
		Heap chest = null;
		for (Heap h : ow.heaps.valueList()){
			if (h.type == Heap.Type.CHEST && first( h.items, Shortsword.class ) != null) chest = h;
		}
		assertNotNull( "the chest stands beside the plaza", chest );
		assertTrue( ow.distance( plaza, chest.pos ) <= SafeRoomMigration.REACH );
		assertEquals( 70, first( chest.items, Gold.class ).quantity() );
	}

	@Test
	public void nothingBuiltLaysRockAPickWouldMineAgain(){
		//the old tool laid walls and crystal for free, which a pick then mined for ore and gems
		//again: no piece the build menu offers may lay the land's own rock or a crystal seam
		for (BuildPiece piece : BuildCatalog.all()){
			if (piece.kind != BuildPiece.Kind.TERRAIN) continue;
			int t = piece.terrain;
			assertFalse( piece.id, t == Terrain.WALL || t == Terrain.WALL_DECO || t == Terrain.MINE_CRYSTAL );
		}
	}

	@Test
	public void theArchersLootKeepsItsMarksWithoutThePage(){
		Class<?> cls = xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollArcher.class;
		int before = 0;
		for (int slot = 0; slot < 8; slot++) if (xyz.gabriwar.warpedpixeldungeon.journal.Bestiary.isLootSlotSeen( cls, slot )) before |= 1 << slot;
		try {
			//an old journal: the sewers key (3) and the forest darts (6) seen, then the page (5)
			xyz.gabriwar.warpedpixeldungeon.journal.Bestiary.restore( journal( cls, (1 << 3) | (1 << 6), false ) );
			assertTrue( xyz.gabriwar.warpedpixeldungeon.journal.Bestiary.isLootSlotSeen( cls, 3 ) );
			assertTrue( "the darts' mark moves to their slot", xyz.gabriwar.warpedpixeldungeon.journal.Bestiary.isLootSlotSeen( cls, 5 ) );
			assertFalse( xyz.gabriwar.warpedpixeldungeon.journal.Bestiary.isLootSlotSeen( cls, 6 ) );
			xyz.gabriwar.warpedpixeldungeon.journal.Bestiary.restore( journal( cls, 1 << 5, false ) );
			assertFalse( "the page's own mark goes", xyz.gabriwar.warpedpixeldungeon.journal.Bestiary.isLootSlotSeen( cls, 5 ) );

			//a journal saved since: as it is
			xyz.gabriwar.warpedpixeldungeon.journal.Bestiary.restore( journal( cls, 1 << 5, true ) );
			assertTrue( xyz.gabriwar.warpedpixeldungeon.journal.Bestiary.isLootSlotSeen( cls, 5 ) );
			Bundle saved = new Bundle();
			xyz.gabriwar.warpedpixeldungeon.journal.Bestiary.store( saved );
			assertTrue( saved.getBoolean( "bestiary_archer_loot_shifted" ) );
		} finally {
			xyz.gabriwar.warpedpixeldungeon.journal.Bestiary.restore( journal( cls, before, true ) );
		}
	}

	private static Bundle journal( Class<?> cls, int loot, boolean shifted ){
		Bundle b = new Bundle();
		b.put( "bestiary_classes", new Class[]{ cls } );
		b.put( "bestiary_seen", new boolean[]{ true } );
		b.put( "bestiary_encounters", new int[]{ 3 } );
		b.put( "bestiary_loot_seen", new int[]{ loot } );
		if (shifted) b.put( "bestiary_archer_loot_shifted", true );
		return roundTrip( b );
	}
}

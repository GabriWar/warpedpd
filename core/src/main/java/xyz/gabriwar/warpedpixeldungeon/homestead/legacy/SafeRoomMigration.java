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

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.StorageChest;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownLedger;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.PET;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.Crop;
import xyz.gabriwar.warpedpixeldungeon.items.BuildersTool;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.OtilukesJournal;
import xyz.gabriwar.warpedpixeldungeon.items.PortableChest;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.FadedSafeSpotPage;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.DeadEndLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The safe room (depth 50, reached through Otiluke's Journal's first page) is gone: the
 * homesteads out in the world replace it. A save from before keeps all it had there, once:
 *
 * - a hero standing in the room wakes on the town's plaza instead (relocate, from
 *   Dungeon.loadGame), and so does a beacon or a journal that still points at it (relocate,
 *   from InterlevelScene.returnTo);
 * - the room's level file is read once (migrate, from Dungeon.loadGame): every heap's items, every
 *   storage chest standing there packed back into its kit with all it held, the seed of every
 *   crop growing there, the gold an alchemy pot or an enchanting pedestal cost to build there, and
 *   a pet left there, who follows the hero again. The things wait (saved with the game) until
 *   the town's plaza is in the surface's window, and are laid there as one chest on a free cell
 *   beside the plaza (deliver: from Dungeon.switchLevel, and from OverworldLevel.shiftWindow when
 *   the window walks onto the plaza). The room's other things stay behind: its sheep, its prize
 *   pool (heapstogen, the room's own key and food), its plants, and any ally but the pet;
 * - nothing is ever only in memory: the things laid in the chest are still saved with the game
 *   until the surface's level file holding the chest is written too (afterSaveAll, from
 *   Dungeon.saveAll, which then writes the game again without them), and the old room's file
 *   goes only then. A crash between the two files loses nothing (the next load lays the chest
 *   again); the worst a crash in the instant after can do is lay it twice;
 * - a safe spot page comes back as a FadedSafeSpotPage (the Bundle alias in WarpedPixelDungeon):
 *   the hero's own, and the one bound into his journal, are traded at once (settle) for a
 *   builder's tool, or for PAGE_GOLD gold scaled as the town's prices are when he already has one.
 *
 * The flag is saved with the game; a new run starts with it set. Host only: a co-op guest never
 * loads the game's save. Nothing here reads SafeLevel: the class is gone, and its file is read
 * as plain bundles.
 */
public final class SafeRoomMigration {

	private SafeRoomMigration(){}

	/** The old safe room's depth (branch 0). Nothing is built there any more. */
	public static final int DEPTH = 50;
	/** What a safe spot page is worth when the hero already has a builder's tool, before the
	 *  town's scaling (TownLedger.scaled). */
	public static final int PAGE_GOLD = 300;
	/** What the builder's tool charged for an alchemy pot and an enchanting pedestal in the
	 *  safe room: given back for each one it held, since neither can be carried out. */
	public static final int ALCHEMY_REFUND = 800, PEDESTAL_REFUND = 1500;
	/** How far from the plaza the chest may be laid, in cells. */
	public static final int REACH = 4;

	//has this run's safe room been dealt with? set on a new run, saved with the game
	private static boolean done = true;
	//what the room held, waiting to be laid beside the town plaza
	private static final ArrayList<Item> waiting = new ArrayList<>();
	//what was laid in the chest, still saved with the game until the level holding the chest is
	//written (afterSaveAll), and the level it was laid on
	private static final ArrayList<Item> laid = new ArrayList<>();
	private static Level laidOn = null;
	//the save slot whose old room file goes once the next save of that slot is written, or -1
	private static int deleteFromSlot = -1;
	//did the load just move the hero out of the room? (he hears it once, on the plaza)
	private static boolean relocated = false;
	//what the hero is told while a load runs, before there is a game log to show it: told by
	//GameScene once it stands (tell)
	private static final ArrayList<String> notes = new ArrayList<>();

	private static final String DONE    = "safe_room_migrated";
	private static final String WAITING = "safe_room_belongings";

	// ------------------------------------------------------------------ old classes

	/** The removed classes an old save names (WarpedPixelDungeon's constructor): the page comes
	 *  back faded, to be traded in; the level class never loads again (a save standing there
	 *  wakes in the town, its file is read as plain bundles), but a stray bundle of it restores
	 *  as a dead end rather than failing the load. */
	public static void registerAliases(){
		Bundle.addAlias( FadedSafeSpotPage.class,
				"xyz.gabriwar.warpedpixeldungeon.items.journalpages.SafeSpotPage" );
		Bundle.addAlias( DeadEndLevel.class,
				"xyz.gabriwar.warpedpixeldungeon.levels.SafeLevel" );
	}

	// ------------------------------------------------------------------ the run's state

	/** A new run: there was never a safe room to empty. */
	public static void reset(){
		done = true;
		waiting.clear();
		laid.clear();
		laidOn = null;
		deleteFromSlot = -1;
		relocated = false;
		notes.clear();
	}

	/** What is still to reach the chest, and what was laid in it since the level holding it was
	 *  last written: a load from this save lays all of it (again). */
	public static void storeInBundle( Bundle bundle ){
		bundle.put( DONE, done );
		if (!waiting.isEmpty() || !laid.isEmpty()){
			ArrayList<Item> kept = new ArrayList<>( waiting );
			kept.addAll( laid );
			bundle.put( WAITING, kept );
		}
	}

	/** A save from before this existed has no flag: its room (if it ever had one) is still to empty. */
	public static void restoreFromBundle( Bundle bundle ){
		done = bundle.getBoolean( DONE );
		waiting.clear();
		laid.clear();
		laidOn = null;
		deleteFromSlot = -1;
		if (bundle.contains( WAITING )){
			for (Bundlable b : bundle.getCollection( WAITING )){
				if (b instanceof Item) waiting.add( (Item) b );
			}
		}
	}

	/** Has the old room been emptied (or was there never one)? */
	public static boolean done(){
		return done;
	}

	/** What waits for the hero beside the town plaza. */
	public static List<Item> waiting(){
		return Collections.unmodifiableList( waiting );
	}

	/** Debug scenes and tests: the room is to be emptied again, on the next migrate. */
	public static void rearm(){
		done = false;
	}

	// ------------------------------------------------------------------ where the hero is

	/**
	 * Is Dungeon.depth/branch still the old room? Then it becomes the surface, landing on the
	 * town's plaza. True when it moved: Dungeon.loadGame calls it for the depth a save stood
	 * on, InterlevelScene.returnTo for one a beacon or a journal kept.
	 */
	public static boolean relocate(){
		if (Dungeon.depth != DEPTH || Dungeon.branch != 0) return false;
		Dungeon.depth = OverworldLevel.DEPTH;
		OverworldLevel.arriveInTown( WorldStructures.TOWN_PLAZA );
		return true;
	}

	/** Dungeon.loadGame, once the hero's depth is read: relocate, remembered for the log. */
	public static void relocateOnLoad(){
		relocated = relocate();
	}

	// ------------------------------------------------------------------ the load

	/**
	 * Dungeon.loadGame, once everything is restored: empty the old room's file into the waiting
	 * chest (once), forget the depth, and trade in the safe spot pages the hero holds.
	 */
	public static void migrate( int slot ){
		Hero hero = Dungeon.hero;
		if (!done){
			String file = GamesInProgress.depthFile( slot, DEPTH, 0 );
			if (FileUtils.fileExists( file )){
				try {
					Haul haul = collect( FileUtils.bundleFromFile( file ) );
					absorb( haul, hero );
					deleteFromSlot = slot;
				} catch (IOException e){
					//a room that cannot be read is no room to empty: its file stays, unread
					Game.reportException( e );
				}
			}
			Dungeon.generatedLevels.remove( Integer.valueOf( DEPTH ) );
			Notes.forgetDepth( DEPTH );
			done = true;
		}
		if (hero != null) settle( hero );
		if (relocated){
			say( GLog.WARNING, Messages.get( SafeRoomMigration.class, "moved" ) );
		} else if (!waiting.isEmpty() && deleteFromSlot == slot){
			say( GLog.POSITIVE, Messages.get( SafeRoomMigration.class, "waiting" ) );
		}
		relocated = false;
	}

	/** GameScene, once it stands: what the hero was told while the game loaded. */
	public static void tell(){
		for (String note : notes) GLog.i( note );
		notes.clear();
	}

	/** What the hero is still to be told (tests). */
	public static List<String> notes(){
		return Collections.unmodifiableList( notes );
	}

	//told now in a running game, kept for GameScene while a load runs (the log is not up yet:
	//a line sent then reaches nobody)
	private static void say( String kind, String text ){
		if (WarpedPixelDungeon.scene() instanceof GameScene) GLog.i( kind + text );
		else notes.add( kind + text );
	}

	/**
	 * Dungeon.saveAll, once both the game's file and the current level's are written. The chest
	 * laid on that level is in its file now, so what was laid leaves the game's save: true when
	 * the game's file is to be written again for it. The old room's file goes once a whole save
	 * holds what it had.
	 */
	public static boolean afterSaveAll( int slot ){
		boolean rewrite = false;
		if (!laid.isEmpty() && laidOn == Dungeon.level){
			laid.clear();
			laidOn = null;
			rewrite = true;
		}
		if (deleteFromSlot == slot){
			FileUtils.deleteFile( GamesInProgress.depthFile( slot, DEPTH, 0 ) );
			deleteFromSlot = -1;
		}
		return rewrite;
	}

	/** What the old room held: read from its level file, without its level class. */
	public static final class Haul {
		/** Every heap's items, every chest packed up, every crop's seed, the refunds in gold. */
		public final ArrayList<Item> items = new ArrayList<>();
		/** The gold refunded for the alchemy pots and enchanting pedestals built there. */
		public int refund;
		/** The pet left there: {type, level, kills, HP, experience, cooldown}, or null. */
		public int[] pet;
	}

	//the level file's keys (Dungeon.saveLevel, Level.storeInBundle, Char, PET, StorageChest, FarmCrop)
	private static final String LEVEL = "level";
	private static final String HEAPS = "heaps", MOBS = "mobs", MAP = "map";
	private static final String CLASS_NAME = "__className";

	/** A level file in the old room's format, for debug scenes and tests: the heaps, the mobs
	 *  standing there and its terrain, under the old level's class name. */
	public static Bundle roomFile( java.util.Collection<Heap> heaps, java.util.Collection<? extends Bundlable> mobs, int[] map ){
		Bundle level = new Bundle();
		level.put( CLASS_NAME, "xyz.gabriwar.warpedpixeldungeon.levels.SafeLevel" );
		level.put( HEAPS, heaps );
		level.put( MOBS, mobs );
		level.put( MAP, map );
		Bundle file = new Bundle();
		file.put( LEVEL, level );
		return file;
	}

	/** The old room's level file (as Dungeon.saveLevel wrote it), emptied into a haul. Pure: it
	 *  reads the bundle and changes nothing else. */
	public static Haul collect( Bundle file ){
		Haul haul = new Haul();
		Bundle level = file.getBundle( LEVEL );
		if (level.isNull()) return haul;

		if (level.contains( HEAPS )){
			for (Bundlable b : level.getCollection( HEAPS )){
				if (!(b instanceof Heap)) continue;
				for (Item item : ((Heap) b).items){
					if (item != null) haul.items.add( item );
				}
			}
		}

		//only the mobs that hold the hero's things are made: the room's sheep and anything else
		//never come to life outside it
		if (level.contains( MOBS )){
			for (Bundle mob : level.getBundleArray( MOBS )){
				Class<?> cls = mob.getClass( CLASS_NAME );
				if (cls == null) continue;
				if (StorageChest.class.isAssignableFrom( cls )){
					PortableChest kit = new PortableChest( mob.getInt( "type" ) );
					Bundlable storage = mob.get( "storage" );
					if (storage instanceof PortableChest.StorageSpace){
						kit.storage = (PortableChest.StorageSpace) storage;
						kit.storage.cap = PortableChest.capacityFor( kit.type );
					}
					haul.items.add( kit );
				} else if (FarmCrop.class.isAssignableFrom( cls )){
					//the room's crops saved their seed itself; a crop of the world's fields
					//(homestead/farming/Crop) keeps only its seed's class
					Bundlable seed = mob.get( "seed" );
					Bundlable crop = mob.get( "crop" );
					if (seed instanceof Item){
						haul.items.add( (Item) seed );
					} else if (crop instanceof Crop && ((Crop) crop).seed != null){
						haul.items.add( Reflection.newInstance( ((Crop) crop).seed ) );
					}
				} else if (PET.class.isAssignableFrom( cls ) && haul.pet == null){
					haul.pet = new int[]{ mob.getInt( "type" ), mob.getInt( "level" ), mob.getInt( "kills" ),
							mob.getInt( "HP" ), mob.getInt( "experience" ), mob.getInt( "cooldown" ) };
				}
			}
		}

		//the room's own layout had neither: every one standing there was bought
		int[] map = level.getIntArray( MAP );
		if (map != null){
			for (int t : map){
				if (t == Terrain.ALCHEMY) haul.refund += ALCHEMY_REFUND;
				else if (t == Terrain.PEDESTAL) haul.refund += PEDESTAL_REFUND;
			}
		}
		if (haul.refund > 0) haul.items.add( new Gold( haul.refund ) );
		return haul;
	}

	//the haul joins what waits for the hero in the town; his pet follows him again
	private static void absorb( Haul haul, Hero hero ){
		waiting.addAll( haul.items );
		if (haul.pet != null && hero != null && hero.haspet && !hero.petfollow){
			hero.petType       = haul.pet[0];
			hero.petLevel      = haul.pet[1];
			hero.petKills      = haul.pet[2];
			hero.petHP         = haul.pet[3];
			hero.petExperience = haul.pet[4];
			hero.petCooldown   = haul.pet[5];
			hero.petfollow     = true;
			say( GLog.POSITIVE, Messages.get( SafeRoomMigration.class, "pet" ) );
		}
		if (haul.refund > 0){
			say( "", Messages.get( SafeRoomMigration.class, "refund", haul.refund ) );
		}
	}

	// ------------------------------------------------------------------ the pages

	/** Every safe spot page the hero holds, in his pack or bound into his journal, traded in. */
	public static void settle( Hero hero ){
		for (FadedSafeSpotPage page : hero.belongings.getAllItems( FadedSafeSpotPage.class )){
			page.detachAll( hero.belongings.backpack );
			redeem( hero );
		}
		for (OtilukesJournal journal : hero.belongings.getAllItems( OtilukesJournal.class )){
			settleJournal( journal, hero );
		}
	}

	/** A journal with the safe room's page bound in: the page comes out and is traded in, and the
	 *  journal is one page lighter (the other rooms keep their numbers). */
	public static void settleJournal( OtilukesJournal journal, Hero hero ){
		if (journal.rooms == null || journal.rooms.length == 0 || !journal.rooms[0]) return;
		journal.rooms[0] = false;
		if (journal.firsts != null && journal.firsts.length > 0) journal.firsts[0] = false;
		journal.journalLevel = Math.max( 1, journal.journalLevel - 1 );
		redeem( hero );
	}

	/** One safe spot page's worth: a builder's tool when the hero has none (nor one waiting in
	 *  the town), else PAGE_GOLD gold at the town's scale. */
	public static void redeem( Hero hero ){
		if (!hasTool( hero )){
			BuildersTool tool = new BuildersTool();
			//a full pack: the tool waits with the rest in the town
			if (!tool.collect( hero.belongings.backpack )) waiting.add( tool );
			say( GLog.POSITIVE, Messages.get( SafeRoomMigration.class, "page_tool" ) );
		} else {
			int gold = pageGold();
			Dungeon.gold += gold;
			say( GLog.POSITIVE, Messages.get( SafeRoomMigration.class, "page_gold", gold ) );
		}
	}

	/** A page's worth in gold, at the town's scale. */
	public static int pageGold(){
		return TownLedger.scaled( PAGE_GOLD );
	}

	private static boolean hasTool( Hero hero ){
		if (hero.belongings.getItem( BuildersTool.class ) != null) return true;
		for (Item item : waiting) if (holdsTool( item )) return true;
		return false;
	}

	//a tool, or a bag or a packed chest with one inside
	private static boolean holdsTool( Item item ){
		if (item instanceof BuildersTool) return true;
		ArrayList<Item> inside = item instanceof Bag ? ((Bag) item).items
				: item instanceof PortableChest && ((PortableChest) item).storage != null
					? ((PortableChest) item).storage.items : null;
		if (inside != null) for (Item i : inside) if (holdsTool( i )) return true;
		return false;
	}

	// ------------------------------------------------------------------ the chest in the town

	/**
	 * Dungeon.switchLevel, and OverworldLevel.shiftWindow once a moved window is peopled: on the
	 * surface, the host's own, with the town's plaza in the window, what waits is laid as one
	 * chest beside the plaza. True when it was. The chest is made here, not through Level.drop,
	 * whose dummy heap for an item a challenge forbids would swallow everything after it: such an
	 * item is left out, as a drop would leave it.
	 */
	public static boolean deliver( Level level ){
		if (waiting.isEmpty() || !(level instanceof OverworldLevel)) return false;
		OverworldLevel ow = (OverworldLevel) level;
		if (ow.isMirror() || ow.altitude() != 0) return false;
		int cell = chestCell( ow );
		if (cell == -1) return false;
		Heap heap = ow.heaps.get( cell );
		if (heap == null){
			heap = new Heap();
			heap.pos = cell;
			heap.seen = Dungeon.level == ow && ow.heroFOV != null && ow.heroFOV[cell];
		} else if (heap.type != Heap.Type.HEAP && heap.type != Heap.Type.CHEST){
			//the plaza itself, with something on it that is no plain pile (a stall's wares, a
			//locked chest): never turned into the hero's chest, it waits for a free cell
			return false;
		}
		heap.type = Heap.Type.CHEST;
		for (Item item : waiting){
			//as Level.drop gives anything its rarity and type on the way into the world
			Quality.roll( item );
			if (Challenges.isItemBlocked( item )) continue;
			heap.drop( item );
			laid.add( item );
		}
		waiting.clear();
		if (heap.isEmpty()) return true;
		laidOn = ow;
		if (ow.heaps.get( cell ) == null){
			ow.heaps.put( cell, heap );
			GameScene.add( heap );
		}
		say( GLog.POSITIVE, Messages.get( SafeRoomMigration.class, "delivered" ) );
		return true;
	}

	/**
	 * Where the chest goes: the first free cell of plain ground beside the town plaza (west, east,
	 * south, north, then ring by ring out to REACH), never the plaza itself, a doorway, a stair or
	 * a cell with a heap, a trap, a plant or anyone on it. -1 when the plaza is not in the window;
	 * the plaza itself when nothing near is free.
	 */
	public static int chestCell( OverworldLevel ow ){
		int plaza = ow.localCell( WorldStructures.townWorldX( WorldStructures.TOWN_PLAZA ),
				WorldStructures.townWorldY( WorldStructures.TOWN_PLAZA ) );
		if (plaza == -1) return -1;
		int w = ow.width();
		for (int n : new int[]{ -1, 1, w, -w }){
			if (free( ow, plaza + n )) return plaza + n;
		}
		int px = plaza % w, py = plaza / w;
		for (int r = 1; r <= REACH; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = px + dx, y = py + dy;
					if (x <= 0 || y <= 0 || x >= w - 1 || y >= ow.height() - 1) continue;
					if (free( ow, x + y * w )) return x + y * w;
				}
			}
		}
		return plaza;
	}

	private static boolean free( OverworldLevel ow, int cell ){
		if (cell < 0 || cell >= ow.length()) return false;
		int t = ow.map[cell];
		return ow.passable[cell] && !ow.pit[cell] && !ow.avoid[cell]
				&& (Terrain.flags[t] & Terrain.LIQUID) == 0
				&& t != Terrain.DOOR && t != Terrain.OPEN_DOOR
				&& ow.heaps.get( cell ) == null && Actor.findChar( cell ) == null
				&& ow.traps.get( cell ) == null && ow.plants.get( cell ) == null
				&& ow.getTransition( cell ) == null
				&& !touchesDoorway( ow, cell );
	}

	//a doorway's mouth stays clear: nobody has to walk round a chest to get in
	private static boolean touchesDoorway( OverworldLevel ow, int cell ){
		for (int n : PathFinder.NEIGHBOURS4){
			int c = cell + n;
			if (c < 0 || c >= ow.length()) continue;
			int t = ow.map[c];
			if (t == Terrain.DOOR || t == Terrain.OPEN_DOOR || ow.getTransition( c ) != null) return true;
		}
		return false;
	}
}

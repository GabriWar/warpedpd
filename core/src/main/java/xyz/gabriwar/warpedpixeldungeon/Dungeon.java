/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CagedKobold;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Dewcharge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Awareness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Dread;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSight;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.RevealedArea;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.DivineSense;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Ghost;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Imp;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Wandmaker;
import xyz.gabriwar.warpedpixeldungeon.items.Amulet;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TalismanOfForesight;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfWarding;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.BattleLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.CatacombLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.CavesBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.CavesLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.ChasmLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.CityBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.CityLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.CrabBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.DeadEndLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.DevRoomsLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.DragonCaveLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.FieldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.FishingLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.FortressLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.HallsBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.HallsLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.FrozenLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.SpiderNestLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TempleChasmLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TempleNewLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.FrozenBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.InfestBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.LastLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.MineLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.MinesBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.MiningLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.PrisonBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.PrisonLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.RegularLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.SafeLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.SewerBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.SewerLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.SkeletonBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.SokobanCastle;
import xyz.gabriwar.warpedpixeldungeon.levels.SokobanIntroLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.SokobanPuzzlesLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.SokobanTeleportLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.SokobanVaultLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.SproutedVaultLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TenguDenLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.ThiefBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.ThiefCatchLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownChurchLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownCinemaLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownLibraryLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownShopLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownFortuneLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownInnLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.VaultLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.ZotBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SpecialRoom;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.plants.PlantGrowthManager;
import xyz.gabriwar.warpedpixeldungeon.ui.QuickSlotButton;
import xyz.gabriwar.warpedpixeldungeon.ui.Toolbar;
import xyz.gabriwar.warpedpixeldungeon.utils.DungeonSeed;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndResurrect;
import com.watabou.noosa.Game;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

public class Dungeon {

	//first depth of Sprouted post-game content (Forest level)
	public static final int POSTGAME_DEPTH = 27;

	//enum of items which have limited spawns, records how many have spawned
	//could all be their own separate numbers, but this allows iterating, much nicer for bundling/initializing.
	public static enum LimitedDrops {
		//limited world drops
		STRENGTH_POTIONS,
		UPGRADE_SCROLLS,
		ARCANE_STYLI,
		ENCH_STONE,
		INT_STONE,
		TRINKET_CATA,
		BULLET_BELT,
		LAB_ROOM, //actually a room, but logic is the same

		//Health potion sources
		//enemies
		SWARM_HP,
		NECRO_HP,
		BAT_HP,
		WARLOCK_HP,
		//Demon spawners are already limited in their spawnrate, no need to limit their health drops
		//alchemy
		COOKING_HP,
		BLANDFRUIT_SEED,

		//Other limited enemy drops
		SLIME_WEP,
		SKELE_WEP,
		THEIF_MISC,
		GUARD_ARM,
		SHAMAN_WAND,
		DM200_EQUIP,
		GOLEM_EQUIP,

		//containers
		VELVET_POUCH,
		SCROLL_HOLDER,
		POTION_BANDOLIER,
		MAGICAL_HOLSTER,
		ANKH_CHAIN,
		KEY_RING,
		FOOD_POUCH,

		//Sprouted-specific limited drops
		NORNSTONES,
		PRISON_KEY,
		CITY_KEY,
		HALLS_KEY,
		BONE,
		ANCIENT_COIN,
		RING_OF_WEALTH,
		ARMBAND,
		SPORK,
		ROYAL_SPORK,
		SEWER_KEY,
		CAVES_KEY,
		VAULT_PAGE,
		CONCH_SHELL,
		TENGU_KEY,
		JOURNAL,
		SAFE_SPOT_PAGE,
		DRAGON_CAVE,
		BERRIES,
		SCORPIO_HP,

		//lore documents
		LORE_SEWERS,
		LORE_PRISON,
		LORE_CAVES,
		LORE_CITY,
		LORE_HALLS;

		public int count = 0;

		//for items which can only be dropped once, should directly access count otherwise.
		public boolean dropped(){
			return count != 0;
		}
		public void drop(){
			count = 1;
		}

		public static void reset(){
			for (LimitedDrops lim : values()){
				lim.count = 0;
			}
		}

		public static void store( Bundle bundle ){
			for (LimitedDrops lim : values()){
				bundle.put(lim.name(), lim.count);
			}
		}

		public static void restore( Bundle bundle ){
			for (LimitedDrops lim : values()){
				if (bundle.contains(lim.name())){
					lim.count = bundle.getInt(lim.name());
				} else {
					lim.count = 0;
				}
				
			}
		}

	}

	public static int challenges;
	public static float mobsToChampion;

	public static Hero hero;
	public static Level level;

	public static QuickSlot quickslot = new QuickSlot();
	
	public static int depth;
	//determines path the hero is on. Current uses:
	// 0 is the default path
	// 1 is for quest sub-floors
	public static int branch;

	//Transient: carries the journal first-visit flag from OtilukesJournal.portToRoom
	//into the next newLevel() so createItems() sees it before create() runs.
	//NOT persisted — do not add to storeGameState/restore.
	public static boolean nextLevelFirstVisit = false;

	//keeps track of what levels the game should try to load instead of creating fresh
	public static ArrayList<Integer> generatedLevels = new ArrayList<>();

	public static int gold;
	public static int bullet;
	public static boolean templeCompleted;
	public static int energy;
	
	public static HashSet<Integer> chapters;

	public static SparseArray<ArrayList<Item>> droppedItems;

	//first variable is only assigned when game is started, second is updated every time game is saved
	public static int initialVersion;
	public static int version;

	public static boolean daily;
	public static boolean dailyReplay;
	public static String customSeedText = "";
	public static long seed;
	public static long lastPlayed;

	// Sprouted-specific flags
	public static boolean dewDraw;
	//depths that already granted a dew charge this game, so revisiting a floor
	//(going up and back down) doesn't re-trigger the charge window
	public static HashSet<Integer> dewChargedDepths = new HashSet<>();
	public static boolean dewWater;
	public static boolean wings;
	public static boolean sporkAvail;
	public static boolean sanchikarah;
	public static boolean sanchikarahdeath;
	public static boolean sanchikarahlife;
	public static boolean sanchikarahtranscend;
	public static boolean orbofzotdropped;
	public static boolean orbofzotshopsold;
	public static boolean shadowyogkilled;
	public static boolean crabkingkilled;
	public static boolean banditkingkilled;
	public static boolean skeletonkingkilled;
	public static int shellCharge = 20;
	public static int petHasteLevel;
	public static int zotDrains;
	public static int ratChests;
	public static boolean earlygrass;
	public static boolean playtest;
	public static boolean gnollspawned;
	public static boolean skeletonspawned;
	public static boolean goldthiefspawned;
	public static boolean tengukilled;
	public static boolean tengudenkilled;
	public static int transmutation;

	// Day/Night cycle turn counter (increments each hero turn)
	public static int cycleTurn;
	// Timestamp of game start (for clock-based calendar)
	public static long gameStartTime;
	// Calendar starting day (absolute day number, derived from seed)
	public static int calendarStartDay;
	public static int[] pars;

	//we initialize the seed separately so that things like interlevelscene can access it early
	public static void initSeed(){
		if (daily) {
			//Ensures that daily seeds are not in the range of user-enterable seeds
			seed = WPDSettings.lastDaily() + DungeonSeed.TOTAL_SEEDS;
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			customSeedText = format.format(new Date(WPDSettings.lastDaily()));
		} else if (!WPDSettings.customSeed().isEmpty()){
			customSeedText = WPDSettings.customSeed();
			seed = DungeonSeed.convertFromText(customSeedText);
		} else {
			customSeedText = "";
			seed = DungeonSeed.randomSeed();
		}
	}
	
	public static void init() {

		initialVersion = version = Game.versionCode;
		challenges = WPDSettings.challenges();
		mobsToChampion = 1;

		Actor.clear();
		Actor.resetNextID();

		//offset seed slightly to avoid output patterns
		Random.pushGenerator( seed+1 );

			Scroll.initLabels();
			Potion.initColors();
			Ring.initGems();

			SpecialRoom.initForRun();
			SecretRoom.initForRun();

			Generator.fullReset();

		Random.resetGenerators();
		
		Statistics.reset();
		Notes.reset();
		Portals.reset();

		quickslot.reset();
		QuickSlotButton.reset();
		Toolbar.swappedQuickslots = false;
		
		//every run opens in the town, on the surface (OverworldLevel.build
		//lands the hero on the plaza); the town stairs lead down to floor 1
		depth = xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.DEPTH;
		branch = 0;
		generatedLevels.clear();

		gold = 0;
		bullet = 0;
		templeCompleted = false;
		CagedKobold.Quest.reset();
		energy = 0;

		// Sprouted flags
		dewDraw = false;
		dewChargedDepths.clear();
		dewWater = false;
		wings = false;
		sporkAvail = false;
		sanchikarah = false;
		sanchikarahdeath = false;
		sanchikarahlife = false;
		sanchikarahtranscend = false;
		orbofzotdropped = false;
		orbofzotshopsold = false;
		shadowyogkilled = false;
		crabkingkilled = false;
		banditkingkilled = false;
		skeletonkingkilled = false;
		shellCharge = 20;
		petHasteLevel = 0;
		zotDrains = 0;
		ratChests = 0;
		earlygrass = false;
		playtest = false;
		gnollspawned = false;
		skeletonspawned = false;
		goldthiefspawned = false;
		tengukilled = false;
		tengudenkilled = false;
		transmutation = Random.IntRange( 6, 14 );
		// Derive initial time-of-day from seed so every seed enters at a unique phase
		cycleTurn = (int)(new java.util.Random(seed ^ 0xD1CE71D1CE71L).nextInt(DayNightCycle.FULL_CYCLE));
		gameStartTime = System.currentTimeMillis();
		calendarStartDay = GameCalendar.generateStartDay(seed);
		DayNightCycle.syncPhase();
		ClimateManager.onNewGame(seed);
		pars = new int[100];

		droppedItems = new SparseArray<>();

		LimitedDrops.reset();
		
		chapters = new HashSet<>();
		
		Ghost.Quest.reset();
		Wandmaker.Quest.reset();
		Blacksmith.Quest.reset();
		Imp.Quest.reset();

		hero = new Hero();
		hero.live();

		if (xyz.gabriwar.warpedpixeldungeon.scenes.HeroSelectScene.pendingCustomName != null) {
			hero.customName = xyz.gabriwar.warpedpixeldungeon.scenes.HeroSelectScene.pendingCustomName;
			xyz.gabriwar.warpedpixeldungeon.scenes.HeroSelectScene.pendingCustomName = null;
		}

		Badges.reset();

		GamesInProgress.selectedClass.initHero( hero );
	}

	public static boolean isChallenged( int mask ) {
		return (challenges & mask) != 0;
	}

	public static boolean levelHasBeenGenerated(int depth, int branch){
		return generatedLevels.contains(depth + 1000*branch);
	}
	
	public static Level newLevel() {
		
		Dungeon.level = null;
		Actor.clear();
		
		Level level;
		if (branch == 0) {
			switch (depth) {
				case 1:
				case 2:
				case 3:
				case 4:
					level = new SewerLevel();
					break;
				case 5:
					level = new SewerBossLevel();
					break;
				case 6:
				case 7:
				case 8:
				case 9:
					level = new PrisonLevel();
					break;
				case 10:
					level = new PrisonBossLevel();
					break;
				case 11:
				case 12:
				case 13:
				case 14:
					level = new CavesLevel();
					break;
				case 15:
					level = new CavesBossLevel();
					break;
				case 16:
				case 17:
				case 18:
				case 19:
					level = new CityLevel();
					break;
				case 20:
					level = new CityBossLevel();
					break;
				case 21:
				case 22:
				case 23:
				case 24:
					level = new HallsLevel();
					break;
				case 25:
					level = new HallsBossLevel();
					break;
				case 26:
					level = new LastLevel();
					break;
				// Sprouted: post-game levels
				case 27:
					level = new FieldLevel();
					break;
				case 28:
					level = new BattleLevel();
					break;
				case 29:
					level = new FishingLevel();
					break;
				case 30:
					level = new SproutedVaultLevel();
					break;
				case 31:
					level = new CatacombLevel();
					break;
				case 32:
					level = new FortressLevel();
					break;
				case 33:
					level = new ChasmLevel();
					break;
				case 35:
					level = new InfestBossLevel();
					break;
				case 36:
					level = new TenguDenLevel();
					break;
				case 37:
					level = new SkeletonBossLevel();
					break;
				case 38:
					level = new CrabBossLevel();
					break;
				case 40:
					level = new ThiefBossLevel();
					break;
				case 41:
					level = new ThiefCatchLevel();
					break;
				case 50:
					level = new SafeLevel();
					break;
				case 51:
					level = new SokobanIntroLevel();
					break;
				case 52:
					level = new SokobanCastle();
					break;
				case 53:
					level = new SokobanTeleportLevel();
					break;
				case 54:
					level = new SokobanPuzzlesLevel();
					break;
				case 56:
				case 57:
				case 58:
				case 59:
				case 60:
				case 61:
				case 62:
				case 63:
				case 64:
					level = new MineLevel();
					break;
				case 65:
					level = new MinesBossLevel();
					break;
				case 66:
					level = new SokobanVaultLevel();
					break;
				case 67:
					level = new DragonCaveLevel();
					break;
				case 97:
					//the overworld (phase 1): infinite streamed world, debug entry
					level = new xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel();
					break;
				case 98:
					//dev-only rooms showcase, reached via the debug travel tab
					level = new DevRoomsLevel();
					break;
				case 99:
					level = new ZotBossLevel();
					break;
				default:
					level = new DeadEndLevel();
			}
		} else if (branch == 6) {
			//Remixed PD town building interiors, entered from the town square doorways
			switch (depth) {
				case 1: level = new TownChurchLevel();  break;
				case 2: level = new TownCinemaLevel();  break;
				case 3: level = new TownLibraryLevel(); break;
				case 4: level = new TownShopLevel();    break;
				case 5: level = new TownFortuneLevel(); break;
				case 6: level = new TownInnLevel();     break;
				default:
					level = new DeadEndLevel();
			}
		} else if (branch == xyz.gabriwar.warpedpixeldungeon.levels.VillageHouseLevel.BRANCH) {
			//the one room behind an overworld village door, keyed by the door's hash id
			level = new xyz.gabriwar.warpedpixeldungeon.levels.VillageHouseLevel();
		} else if (branch == 5) {
			//Spider Nest (Remixed PD reimplementation): a looping farm branch off the prison
			switch (depth) {
				case 6: case 7: case 8: case 9: case 10:
					level = new SpiderNestLevel();
					break;
				default:
					level = new DeadEndLevel();
			}
		} else if (branch == 3) {
			//Temple sub-region (Re-ARranged port), hanging off the caves
			switch (depth) {
				case 14:
					level = new TempleNewLevel();
					break;
				default:
					level = new DeadEndLevel();
			}
		} else if (branch == 4) {
			switch (depth) {
				case 14:
					level = new TempleChasmLevel();
					break;
				default:
					level = new DeadEndLevel();
			}
		} else if (branch == 2) {
			//Frozen region (Unleashed PD port): a cold branch running alongside the Halls
			switch (depth) {
				case 21:
				case 22:
				case 23:
				case 24:
					level = new FrozenLevel();
					break;
				case 25:
					level = new FrozenBossLevel();
					break;
				default:
					level = new DeadEndLevel();
			}
		} else if (branch == 1) {
			switch (depth) {
				case 11:
				case 12:
				case 13:
				case 14:
					level = new MiningLevel();
					break;
				case 16:
				case 17:
				case 18:
				case 19:
					level = new VaultLevel();
					break;
				default:
					level = new DeadEndLevel();
			}
		} else {
			level = new DeadEndLevel();
		}

		//dead end levels (and vault levels for now!) get cleared, don't count as generated
		if (!(level instanceof DeadEndLevel || level instanceof VaultLevel)){
			//this assumes that we will never have a depth value outside the range 0 to 999
			// or -500 to 499, etc.
			if (!generatedLevels.contains(depth + 1000*branch)) {
				generatedLevels.add(depth + 1000 * branch);
			}

			//only the dungeon proper counts: not the town/mines act, not the surface
			if (depth > Statistics.deepestFloor && branch == 0 && depth <= 26) {
				Statistics.deepestFloor = depth;

				//snapshot kills-so-far on first reaching the prison, for the
				//Wandmaker's non-mage pacifist AdamantWand bonus (Sprouted parity)
				if (depth == 6) Statistics.sewerKills = Statistics.enemiesSlain;

				if (Statistics.qualifiedForNoKilling) {
					Statistics.completedWithNoKilling = true;
				} else {
					Statistics.completedWithNoKilling = false;
				}
			}
		}

		Statistics.qualifiedForBossRemainsBadge = false;

		//stamp + consume the journal first-visit carrier before create()/createItems().
		//For non-journal creation the carrier is false → identical to default behaviour.
		level.firstVisit = nextLevelFirstVisit;
		nextLevelFirstVisit = false;

		level.create();
		
		if (branch == 0) Statistics.qualifiedForNoKilling = !bossLevel();
		Statistics.qualifiedForBossChallengeBadge = false;
		
		return level;
	}
	

	// Sprouted: direct level creation methods called from items and InterlevelScene
	public static Level newSproutedLevel(int targetDepth) {
		Dungeon.level = null;
		Actor.clear();
		depth = targetDepth;
		if (depth > Statistics.realdeepestFloor) {
			Statistics.realdeepestFloor = depth;
		}
		return newLevel();
	}

	public static Level newFieldLevel() {
		return newSproutedLevel(27);
	}

	public static Level newBattleLevel() {
		return newSproutedLevel(28);
	}

	public static Level newFishLevel() {
		return newSproutedLevel(29);
	}

	public static Level newVaultLevel() {
		return newSproutedLevel(30);
	}

	public static Level newCatacombLevel() {
		return newSproutedLevel(31);
	}

	public static Level newFortressLevel() {
		return newSproutedLevel(32);
	}

	public static Level newChasmLevel() {
		return newSproutedLevel(33);
	}

	public static Level newInfestLevel() {
		Level level = newSproutedLevel(35);
		if (Statistics.deepestFloor > 24) {
			Statistics.deepestFloor = depth;
		}
		return level;
	}

	public static Level newTenguHideoutLevel() {
		return newSproutedLevel(36);
	}

	public static Level newSkeletonBossLevel() {
		return newSproutedLevel(37);
	}

	public static Level newCrabBossLevel() {
		return newSproutedLevel(38);
	}

	public static Level newThiefBossLevel() {
		return newSproutedLevel(40);
	}

	public static Level newMineBossLevel() {
		depth++;
		return newSproutedLevel(depth);
	}

	public static Level newZotBossLevel() {
		return newSproutedLevel(99);
	}

	public static Level newJournalLevel(int page, boolean first) {
		int targetDepth = 50 + page;
		if (page == 6) targetDepth = 66;
		if (page == 7) targetDepth = 67;
		Level level = newSproutedLevel(targetDepth);
		level.firstVisit = first;
		return level;
	}

	public static void resetLevel() {
		
		Actor.clear();
		
		level.reset();
		switchLevel( level, level.entrance() );
	}

	public static long seedCurDepth(){
		return seedForDepth(depth, branch);
	}

	public static long seedForDepth(int depth, int branch){
		int lookAhead = depth;
		lookAhead += 30*branch; //Assumes depth is always 1-30, and branch is always 0 or higher

		Random.pushGenerator( seed );

			for (int i = 0; i < lookAhead; i ++) {
				Random.Long(); //we don't care about these values, just need to go through them
			}
			long result = Random.Long();

		Random.popGenerator();
		return result;
	}
	
	public static boolean shopOnLevel() {
		return depth == 6 || depth == 11 || depth == 16;
	}
	
	public static boolean bossLevel() {
		return bossLevel( depth );
	}
	
	public static boolean bossLevel( int depth ) {
		return depth == 5 || depth == 10 || depth == 15 || depth == 20 || depth == 25 || depth == 36 || depth == 41;
	}

	//the town (55) and the mines act below it (56-64, boss on 65). Unlike the rest of the
	//postgame these descend by staircase, so they need to opt out of the depth < 26 gating.
	/** The name of where the hero stands, for the HUD: region of the dungeon,
	 *  biome (or settlement) on the surface, building name in the town. */
	public static String placeName() {
		if (level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel){
			return ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) level)
					.placeNameAt( hero != null ? hero.pos : 0 );
		}
		if (branch == xyz.gabriwar.warpedpixeldungeon.levels.TownInteriorLevel.BRANCH){
			switch (depth){
				case 1: return "Church";
				case 2: return "Cinema";
				case 3: return "Library";
				case 4: return "Shop";
				case 5: return "Fortune Teller";
				case 6: return "Inn";
			}
		}
		if (depth >= 1 && depth <= 5)   return "Sewers";
		if (depth >= 6 && depth <= 10)  return "Prison";
		if (depth >= 11 && depth <= 15) return "Caves";
		if (depth >= 16 && depth <= 20) return "City";
		if (depth >= 21 && depth <= 26) return "Halls";
		if (depth == 27) return "Fields";
		if (depth == 33) return "Chasm";
		if (depth == 50) return "Safe Room";
		if (depth >= 51 && depth <= 54) return "Sokoban";
		if (depth >= 56 && depth <= 65) return "Mines";
		if (depth == 66) return "Vault";
		if (depth == 67) return "Dragon Cave";
		return "Depth";
	}

	public static boolean townCheck( int depth ) {
		return depth > 54 && depth < 66;
	}

	//value used for scaling of damage values and other effects.
	//is usually the dungeon depth, but can be set to 26 when ascending
	public static int scalingDepth(){
		if (Dungeon.hero != null && Dungeon.hero.buff(AscensionChallenge.class) != null){
			return 26;
		} else {
			return depth;
		}
	}

	public static boolean interfloorTeleportAllowed(){
		if (Dungeon.level.locked
				|| Dungeon.level instanceof MiningLevel
				|| (Dungeon.hero != null && Dungeon.hero.belongings.getItem(Amulet.class) != null)){
			return false;
		}
		return true;
	}
	
	public static void switchLevel( final Level level, int pos ) {

		//Position of -2 specifically means trying to place the hero the exit
		if (pos == -2){
			LevelTransition t = level.getTransition(LevelTransition.Type.REGULAR_EXIT);
			if (t != null) pos = t.cell();
		}

		//Place hero at the entrance if they are out of the map (often used for pos = -1)
		// or if they are in invalid terrain terrain (except in the mining level, where that happens normally)
		if (pos < 0 || pos >= level.length() || level.invalidHeroPos(pos)){
			pos = level.getTransition(null).cell();
		}
		
		PathFinder.setMapSize(level.width(), level.height());
		
		Dungeon.level = level;
		PlantGrowthManager.catchUpGrowth(level);
		hero.pos = pos;

		if (hero.buff(AscensionChallenge.class) != null){
			hero.buff(AscensionChallenge.class).onLevelSwitch();
		}

		// Sprouted: grant Dewcharge buff the FIRST time you reach each floor when
		// dewDraw is active. Only once per depth per game - revisiting a floor no
		// longer re-charges it. Duration 30 (base) + kills last floor, capped at 70.
		if (dewDraw && depth >= 1 && depth <= 24 && !bossLevel() && dewChargedDepths.add(depth)) {
			int charge = Math.min(70, 30 + Statistics.prevfloormoves);
			Buff.prolong(hero, Dewcharge.class, charge);
			GLog.p("You feel the dungeon charge with dew!");
		}

		Mob.restoreAllies( level, pos );

		Actor.init();

		level.addRespawner();
		//re-materialise a carried pet on arrival (Sprouted): respawnerPet() re-spawns
		//it next to the hero and resets petfollow. Without this, Level.beforeTransition
		///pickUpPet would destroy the pet on departure with nothing to bring it back.
		Actor.add( level.respawnerPet() );

		for(Mob m : level.mobs){
			if (m.pos == hero.pos && !Char.hasProp(m, Char.Property.IMMOVABLE)){
				//displace mob
				for(int i : PathFinder.NEIGHBOURS8){
					if (Actor.findChar(m.pos+i) == null && level.passable[m.pos + i]){
						m.pos += i;
						break;
					}
				}
			}
		}
		
		Light light = hero.buff( Light.class );
		hero.viewDistance = light == null ? level.viewDistance : Math.max( Light.DISTANCE, level.viewDistance );
		
		hero.curAction = hero.lastAction = null;

		observe();
		try {
			saveAll();
		} catch (IOException e) {
			WarpedPixelDungeon.reportException(e);
			/*This only catches IO errors. Yes, this means things can go wrong, and they can go wrong catastrophically.
			But when they do the user will get a nice 'report this issue' dialogue, and I can fix the bug.*/
		}
	}

	public static void dropToChasm( Item item ) {
		int depth = Dungeon.depth + 1;
		ArrayList<Item> dropped = Dungeon.droppedItems.get( depth );
		if (dropped == null) {
			Dungeon.droppedItems.put( depth, dropped = new ArrayList<>() );
		}
		dropped.add( item );
	}

	public static boolean posNeeded() {
		//2 POS each floor set
		int posLeftThisSet = 2 - (LimitedDrops.STRENGTH_POTIONS.count - (depth / 5) * 2);
		if (posLeftThisSet <= 0) return false;

		int floorThisSet = (depth % 5);

		//pos drops every two floors, (numbers 1-2, and 3-4) with a 50% chance for the earlier one each time.
		int targetPOSLeft = 2 - floorThisSet/2;
		if (floorThisSet % 2 == 1 && Random.Int(2) == 0) targetPOSLeft --;

		if (targetPOSLeft < posLeftThisSet) return true;
		else return false;

	}
	
	public static boolean souNeeded() {
		int souLeftThisSet;
		//3 SOU each floor set
		souLeftThisSet = 3 - (LimitedDrops.UPGRADE_SCROLLS.count - (depth / 5) * 3);
		if (souLeftThisSet <= 0) return false;

		int floorThisSet = (depth % 5);
		//chance is floors left / scrolls left
		return Random.Int(5 - floorThisSet) < souLeftThisSet;
	}
	
	public static boolean asNeeded() {
		//1 AS each floor set
		int asLeftThisSet = 1 - (LimitedDrops.ARCANE_STYLI.count - (depth / 5));
		if (asLeftThisSet <= 0) return false;

		int floorThisSet = (depth % 5);
		//chance is floors left / scrolls left
		return Random.Int(5 - floorThisSet) < asLeftThisSet;
	}

	public static boolean beltNeeded() {
		//1 bullet belt each floor set, the bulk ammo source for guns and bows
		int beltLeftThisSet = 1 - (LimitedDrops.BULLET_BELT.count - (depth / 5));
		if (beltLeftThisSet <= 0) return false;

		int floorThisSet = (depth % 5);
		//chance is floors left / belts left
		return Random.Int(5 - floorThisSet) < beltLeftThisSet;
	}

	public static boolean enchStoneNeeded(){
		//1 enchantment stone, spawns on chapter 2 or 3
		if (!LimitedDrops.ENCH_STONE.dropped()){
			int region = 1+depth/5;
			if (region > 1){
				int floorsVisited = depth - 5;
				if (floorsVisited > 4) floorsVisited--; //skip floor 10
				return Random.Int(9-floorsVisited) == 0; //1/8 chance each floor
			}
		}
		return false;
	}

	public static boolean intStoneNeeded(){
		//one stone on floors 1-3
		return depth < 5 && !LimitedDrops.INT_STONE.dropped() && Random.Int(4-depth) == 0;
	}

	public static boolean trinketCataNeeded(){
		//one trinket catalyst on floors 1-3
		return depth < 5 && !LimitedDrops.TRINKET_CATA.dropped() && Random.Int(4-depth) == 0;
	}

	public static boolean labRoomNeeded(){
		//one laboratory each floor set, in floor 3 or 4, 1/2 chance each floor
		int region = 1+depth/5;
		if (region > LimitedDrops.LAB_ROOM.count){
			int floorThisRegion = depth%5;
			if (floorThisRegion >= 4 || (floorThisRegion == 3 && Random.Int(2) == 0)){
				return true;
			}
		}
		return false;
	}

	private static final String INIT_VER	= "init_ver";
	public  static final String VERSION		= "version";
	private static final String SEED		= "seed";
	private static final String CUSTOM_SEED	= "custom_seed";
	private static final String DAILY	    = "daily";
	private static final String DAILY_REPLAY= "daily_replay";
	private static final String LAST_PLAYED = "last_played";
	private static final String CHALLENGES	= "challenges";
	private static final String MOBS_TO_CHAMPION	= "mobs_to_champion";
	private static final String HERO		= "hero";
	private static final String DEPTH		= "depth";
	private static final String BRANCH		= "branch";
	private static final String GENERATED_LEVELS    = "generated_levels";
	private static final String GOLD		= "gold";
	private static final String BULLET		= "bullet";
	private static final String TEMPLE_DONE	= "temple_completed";
	private static final String ENERGY		= "energy";
	private static final String DROPPED     = "dropped%d";
	private static final String PORTED      = "ported%d";
	private static final String LEVEL		= "level";
	private static final String LIMDROPS    = "limited_drops";
	private static final String CHAPTERS	= "chapters";
	private static final String QUESTS		= "quests";
	private static final String BADGES		= "badges";

	// Sprouted bundle keys
	private static final String DEW_DRAW          = "dewDraw";
	private static final String DEW_CHARGED       = "dew_charged_depths";
	private static final String DEW_WATER         = "dewWater";
	private static final String WINGS             = "wings";
	private static final String SPORK_AVAIL       = "sporkAvail";
	private static final String SANCHIKARAH       = "sanchikarah";
	private static final String SANCHIKARAH_DEATH = "sanchikarahDeath";
	private static final String SANCHIKARAH_LIFE  = "sanchikarahLife";
	private static final String SANCHIKARAH_TRANS = "sanchikarahTranscend";
	private static final String ORB_OF_ZOT_DROPPED = "orb_of_zot_dropped";
	private static final String ORB_OF_ZOT_SHOP    = "orb_of_zot_shop";
	private static final String SHADOW_YOG_KILLED = "shadowYogKilled";
	private static final String CRAB_KING_KILLED  = "crabKingKilled";
	private static final String BANDIT_KING_KILLED= "banditKingKilled";
	private static final String SKELETON_KING_KILLED = "skeletonKingKilled";
	private static final String SHELL_CHARGE      = "shellCharge";
	private static final String PET_HASTE_LEVEL   = "petHasteLevel";
	private static final String ZOT_DRAINS        = "zotDrains";
	private static final String RAT_CHESTS        = "ratChests";
	private static final String EARLYGRASS        = "earlygrass";
	private static final String PLAYTEST          = "playtest";
	private static final String GNOLLSPAWN        = "gnollspawned";
	private static final String SKELETONSPAWN     = "skeletonspawned";
	private static final String THIEFSPAWN        = "goldthiefspawned";
	private static final String TENGUKILL         = "tengukilled";
	private static final String TENGUDENKILL      = "tengudenkilled";
	private static final String TRANSMUTATION     = "transmutation";
	private static final String PARS              = "pars";
	private static final String CYCLE_TURN        = "cycleTurn";
	private static final String GAME_START_TIME   = "gameStartTime";
	private static final String CALENDAR_START   = "calendarStartDay";

	public static void saveGame( int save ) {
		try {
			Bundle bundle = new Bundle();

			bundle.put( INIT_VER, initialVersion );
			bundle.put( VERSION, version = Game.versionCode );
			bundle.put( SEED, seed );
			bundle.put( CUSTOM_SEED, customSeedText );
			bundle.put( DAILY, daily );
			bundle.put( DAILY_REPLAY, dailyReplay );
			bundle.put( LAST_PLAYED, lastPlayed = Game.realTime);
			bundle.put( CHALLENGES, challenges );
			bundle.put( MOBS_TO_CHAMPION, mobsToChampion );
			bundle.put( HERO, hero );
			bundle.put( DEPTH, depth );
			bundle.put( BRANCH, branch );

			bundle.put( GOLD, gold );
		bundle.put( BULLET, bullet );
		bundle.put( TEMPLE_DONE, templeCompleted );
		CagedKobold.Quest.storeInBundle( bundle );
			bundle.put( ENERGY, energy );

			// Sprouted flags
			bundle.put( DEW_DRAW, dewDraw );
			int[] charged = new int[dewChargedDepths.size()];
			int ci = 0;
			for (int d : dewChargedDepths) charged[ci++] = d;
			bundle.put( DEW_CHARGED, charged );
			bundle.put( DEW_WATER, dewWater );
			bundle.put( WINGS, wings );
			bundle.put( SPORK_AVAIL, sporkAvail );
			bundle.put( SANCHIKARAH, sanchikarah );
			bundle.put( SANCHIKARAH_DEATH, sanchikarahdeath );
			bundle.put( SANCHIKARAH_LIFE, sanchikarahlife );
			bundle.put( SANCHIKARAH_TRANS, sanchikarahtranscend );
			bundle.put( ORB_OF_ZOT_DROPPED, orbofzotdropped );
			bundle.put( ORB_OF_ZOT_SHOP, orbofzotshopsold );
			bundle.put( SHADOW_YOG_KILLED, shadowyogkilled );
			bundle.put( CRAB_KING_KILLED, crabkingkilled );
			bundle.put( BANDIT_KING_KILLED, banditkingkilled );
			bundle.put( SKELETON_KING_KILLED, skeletonkingkilled );
			bundle.put( SHELL_CHARGE, shellCharge );
			bundle.put( PET_HASTE_LEVEL, petHasteLevel );
			bundle.put( ZOT_DRAINS, zotDrains );
			bundle.put( RAT_CHESTS, ratChests );
			bundle.put( EARLYGRASS, earlygrass );
			bundle.put( GNOLLSPAWN, gnollspawned );
			bundle.put( SKELETONSPAWN, skeletonspawned );
			bundle.put( THIEFSPAWN, goldthiefspawned );
			bundle.put( TENGUKILL, tengukilled );
			bundle.put( TENGUDENKILL, tengudenkilled );
			bundle.put( PLAYTEST, playtest );
			bundle.put( PARS, pars );
			bundle.put( TRANSMUTATION, transmutation );
			bundle.put( CYCLE_TURN, cycleTurn );
			bundle.put( GAME_START_TIME, gameStartTime );
			bundle.put( CALENDAR_START, calendarStartDay );
			ClimateManager.storeInBundle( bundle );

			for (int d : droppedItems.keyArray()) {
				bundle.put(Messages.format(DROPPED, d), droppedItems.get(d));
			}

			quickslot.storePlaceholders( bundle );

			Bundle limDrops = new Bundle();
			LimitedDrops.store( limDrops );
			bundle.put ( LIMDROPS, limDrops );
			
			int count = 0;
			int ids[] = new int[chapters.size()];
			for (Integer id : chapters) {
				ids[count++] = id;
			}
			bundle.put( CHAPTERS, ids );
			
			Bundle quests = new Bundle();
			Ghost		.Quest.storeInBundle( quests );
			Wandmaker	.Quest.storeInBundle( quests );
			Blacksmith	.Quest.storeInBundle( quests );
			Imp			.Quest.storeInBundle( quests );
			bundle.put( QUESTS, quests );
			
			SpecialRoom.storeRoomsInBundle( bundle );
			SecretRoom.storeRoomsInBundle( bundle );
			
			Statistics.storeInBundle( bundle );
			Notes.storeInBundle( bundle );
			Generator.storeInBundle( bundle );
			Portals.storeInBundle( bundle );

			int[] bundleArr = new int[generatedLevels.size()];
			for (int i = 0; i < generatedLevels.size(); i++){
				bundleArr[i] = generatedLevels.get(i);
			}
			bundle.put( GENERATED_LEVELS, bundleArr);
			
			Scroll.save( bundle );
			Potion.save( bundle );
			Ring.save( bundle );

			Actor.storeNextID( bundle );
			
			Bundle badges = new Bundle();
			Badges.saveLocal( badges );
			bundle.put( BADGES, badges );
			
			FileUtils.bundleToFile( GamesInProgress.gameFile(save), bundle);
			
		} catch (IOException e) {
			GamesInProgress.setUnknown( save );
			WarpedPixelDungeon.reportException(e);
		}
	}
	
	public static void saveLevel( int save ) throws IOException {
		Bundle bundle = new Bundle();
		bundle.put( LEVEL, level );
		
		FileUtils.bundleToFile(GamesInProgress.depthFile( save, depth, branch ), bundle);
	}
	
	public static void saveAll() throws IOException {
		if (hero != null && (hero.isAlive() || WndResurrect.instance != null)) {
			
			Actor.fixTime();
			updateLevelExplored();
			saveGame( GamesInProgress.curSlot );
			saveLevel( GamesInProgress.curSlot );

			GamesInProgress.set( GamesInProgress.curSlot );

		}
	}
	
	public static void loadGame( int save ) throws IOException {
		loadGame( save, true );
	}
	
	public static void loadGame( int save, boolean fullLoad ) throws IOException {
		
		Bundle bundle = FileUtils.bundleFromFile( GamesInProgress.gameFile( save ) );

		initialVersion = bundle.getInt( INIT_VER );
		version = bundle.getInt( VERSION );

		seed = bundle.contains( SEED ) ? bundle.getLong( SEED ) : DungeonSeed.randomSeed();
		customSeedText = bundle.getString( CUSTOM_SEED );
		daily = bundle.getBoolean( DAILY );
		dailyReplay = bundle.getBoolean( DAILY_REPLAY );

		Actor.clear();
		Actor.restoreNextID( bundle );

		quickslot.reset();
		QuickSlotButton.reset();
		Toolbar.swappedQuickslots = false;

		Dungeon.challenges = bundle.getInt( CHALLENGES );
		Dungeon.mobsToChampion = bundle.getFloat( MOBS_TO_CHAMPION );
		
		Dungeon.level = null;
		Dungeon.depth = -1;
		
		Scroll.restore( bundle );
		Potion.restore( bundle );
		Ring.restore( bundle );

		quickslot.restorePlaceholders( bundle );
		
		if (fullLoad) {
			
			LimitedDrops.restore( bundle.getBundle(LIMDROPS) );

			chapters = new HashSet<>();
			int ids[] = bundle.getIntArray( CHAPTERS );
			if (ids != null) {
				for (int id : ids) {
					chapters.add( id );
				}
			}
			
			Bundle quests = bundle.getBundle( QUESTS );
			if (!quests.isNull()) {
				Ghost.Quest.restoreFromBundle( quests );
				Wandmaker.Quest.restoreFromBundle( quests );
				Blacksmith.Quest.restoreFromBundle( quests );
				Imp.Quest.restoreFromBundle( quests );
			} else {
				Ghost.Quest.reset();
				Wandmaker.Quest.reset();
				Blacksmith.Quest.reset();
				Imp.Quest.reset();
			}
			
			SpecialRoom.restoreRoomsFromBundle(bundle);
			SecretRoom.restoreRoomsFromBundle(bundle);

			generatedLevels.clear();
			for (int i : bundle.getIntArray(GENERATED_LEVELS)){
				generatedLevels.add(i);
			}

			droppedItems = new SparseArray<>();
			for (int i=1; i <= 26; i++) {

				//dropped items
				ArrayList<Item> items = new ArrayList<>();
				if (bundle.contains(Messages.format( DROPPED, i )))
					for (Bundlable b : bundle.getCollection( Messages.format( DROPPED, i ) ) ) {
						items.add( (Item)b );
					}
				if (!items.isEmpty()) {
					droppedItems.put( i, items );
				}

			}
		}
		
		Bundle badges = bundle.getBundle(BADGES);
		if (!badges.isNull()) {
			Badges.loadLocal( badges );
		} else {
			Badges.reset();
		}
		
		Notes.restoreFromBundle( bundle );
		
		hero = null;
		hero = (Hero)bundle.get( HERO );
		
		depth = bundle.getInt( DEPTH );
		branch = bundle.getInt( BRANCH );
		//saves parked in the old separate town level: it lives on the surface now
		if (depth == 55 && branch == 0) {
			depth = xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.DEPTH;
			xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel.arriveInTown(
					xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures.TOWN_PLAZA );
		}

		gold = bundle.getInt( GOLD );
		bullet = bundle.getInt( BULLET );
		templeCompleted = bundle.getBoolean( TEMPLE_DONE );
		CagedKobold.Quest.restoreFromBundle( bundle );
		energy = bundle.getInt( ENERGY );

		// Sprouted flags
		dewDraw             = bundle.getBoolean( DEW_DRAW );
		dewChargedDepths.clear();
		if (bundle.contains( DEW_CHARGED )){
			for (int d : bundle.getIntArray( DEW_CHARGED )) dewChargedDepths.add( d );
		}
		dewWater            = bundle.getBoolean( DEW_WATER );
		wings               = bundle.getBoolean( WINGS );
		sporkAvail          = bundle.getBoolean( SPORK_AVAIL );
		sanchikarah         = bundle.getBoolean( SANCHIKARAH );
		sanchikarahdeath    = bundle.getBoolean( SANCHIKARAH_DEATH );
		sanchikarahlife     = bundle.getBoolean( SANCHIKARAH_LIFE );
		sanchikarahtranscend= bundle.getBoolean( SANCHIKARAH_TRANS );
		orbofzotdropped     = bundle.getBoolean( ORB_OF_ZOT_DROPPED );
		orbofzotshopsold = bundle.getBoolean( ORB_OF_ZOT_SHOP );
		shadowyogkilled     = bundle.getBoolean( SHADOW_YOG_KILLED );
		crabkingkilled      = bundle.getBoolean( CRAB_KING_KILLED );
		banditkingkilled    = bundle.getBoolean( BANDIT_KING_KILLED );
		skeletonkingkilled  = bundle.getBoolean( SKELETON_KING_KILLED );
		shellCharge         = bundle.getInt( SHELL_CHARGE );
		petHasteLevel       = bundle.getInt( PET_HASTE_LEVEL );
		zotDrains           = bundle.getInt( ZOT_DRAINS );
		ratChests           = bundle.getInt( RAT_CHESTS );
		earlygrass          = bundle.getBoolean( EARLYGRASS );
		gnollspawned        = bundle.getBoolean( GNOLLSPAWN );
		skeletonspawned     = bundle.getBoolean( SKELETONSPAWN );
		goldthiefspawned    = bundle.getBoolean( THIEFSPAWN );
		tengukilled         = bundle.getBoolean( TENGUKILL );
		tengudenkilled      = bundle.getBoolean( TENGUDENKILL );
		playtest            = bundle.getBoolean( PLAYTEST );
		pars                = bundle.getIntArray( PARS );
		transmutation       = bundle.getInt( TRANSMUTATION );
		cycleTurn           = bundle.getInt( CYCLE_TURN );
		gameStartTime       = bundle.getLong( GAME_START_TIME );
		if (gameStartTime <= 0) gameStartTime = System.currentTimeMillis();
		calendarStartDay    = bundle.getInt( CALENDAR_START );
		if (calendarStartDay <= 0) calendarStartDay = GameCalendar.generateStartDay(seed);
		DayNightCycle.syncPhase();
		ClimateManager.restoreFromBundle( bundle );

		Statistics.restoreFromBundle( bundle );
		Generator.restoreFromBundle( bundle );
		Portals.restoreFromBundle( bundle );

	}
	
	public static Level loadLevel( int save ) throws IOException {
		
		Dungeon.level = null;
		Actor.clear();

		Bundle bundle = FileUtils.bundleFromFile( GamesInProgress.depthFile( save, depth, branch ));

		Level level = (Level)bundle.get( LEVEL );

		if (level == null){
			throw new IOException();
		} else {
			return level;
		}
	}
	
	public static void deleteGame( int save, boolean deleteLevels ) {

		if (deleteLevels) {
			String folder = GamesInProgress.gameFolder(save);
			for (String file : FileUtils.filesInDir(folder)){
				if (file.contains("depth")){
					FileUtils.deleteFile(folder + "/" + file);
				}
			}
		}

		FileUtils.overwriteFile(GamesInProgress.gameFile(save), 1);
		
		GamesInProgress.delete( save );
	}
	
	public static void preview( GamesInProgress.Info info, Bundle bundle ) {
		info.depth = bundle.getInt( DEPTH );
		info.version = bundle.getInt( VERSION );
		info.challenges = bundle.getInt( CHALLENGES );
		info.seed = bundle.getLong( SEED );
		info.customSeed = bundle.getString( CUSTOM_SEED );
		info.daily = bundle.getBoolean( DAILY );
		info.dailyReplay = bundle.getBoolean( DAILY_REPLAY );
		info.lastPlayed = bundle.getLong( LAST_PLAYED );

		Hero.preview( info, bundle.getBundle( HERO ) );
		Statistics.preview( info, bundle );
	}
	
	public static void fail( Object cause ) {
		if (WndResurrect.instance == null) {
			updateLevelExplored();
			Statistics.gameWon = false;
			Rankings.INSTANCE.submit( false, cause );
		}
	}
	
	public static void win( Object cause ) {

		updateLevelExplored();
		Statistics.gameWon = true;

		hero.belongings.identify();

		Rankings.INSTANCE.submit( true, cause );
	}

	public static void updateLevelExplored(){
		if (branch == 0 && level instanceof RegularLevel && !Dungeon.bossLevel()){
			Statistics.floorsExplored.put( depth, level.levelExplorePercent(depth));
		}
	}

	//default to recomputing based on max hero vision, in case vision just shrank/grew
	public static void observe(){
		int dist = Math.max(Dungeon.hero.viewDistance, 8);
		dist *= 1f + 0.25f*Dungeon.hero.pointsInTalent(Talent.FARSIGHT);

		if (Dungeon.hero.buff(MagicalSight.class) != null){
			dist = Math.max( dist, MagicalSight.DISTANCE );
		}

		observe( dist+1 );
	}
	
	//debug toggle: the whole level stays visible
	public static boolean debugNoFog = false;
	//debug toggles: the hero's invisibility never dispels; every hero attack lands and kills
	public static boolean debugInvisible = false;
	public static boolean debugOneHitKill = false;

	public static void observe( int dist ) {

		if (level == null) {
			return;
		}
		
		level.updateFieldOfView(hero, level.heroFOV);

		if (debugNoFog || level.noFogOfWar()){
			//fov itself is filled inside updateFieldOfView (so heaps get seen)
			java.util.Arrays.fill( level.visited, true );
			GameScene.updateFog();
		}

		int x = hero.pos % level.width();
		int y = hero.pos / level.width();
	
		//left, right, top, bottom
		int l = Math.max( 0, x - dist );
		int r = Math.min( x + dist, level.width() - 1 );
		int t = Math.max( 0, y - dist );
		int b = Math.min( y + dist, level.height() - 1 );
	
		int width = r - l + 1;
		int height = b - t + 1;
		
		int pos = l + t * level.width();
	
		for (int i = t; i <= b; i++) {
			BArray.or( level.visited, level.heroFOV, pos, width, level.visited );
			pos+=level.width();
		}

		//always visit adjacent tiles, even if they aren't seen
		for (int i : PathFinder.NEIGHBOURS9){
			level.visited[hero.pos+i] = true;
		}
	
		GameScene.updateFog(l, t, width, height);

		if (hero.buff(MindVision.class) != null || hero.buff(DivineSense.DivineSenseTracker.class) != null){
			for (Mob m : level.mobs.toArray(new Mob[0])){
				if (m instanceof Mimic && m.alignment == Char.Alignment.NEUTRAL && ((Mimic) m).stealthy()){
					continue;
				}

				BArray.or( level.visited, level.heroFOV, m.pos - 1 - level.width(), 3, level.visited );
				BArray.or( level.visited, level.heroFOV, m.pos - 1, 3, level.visited );
				BArray.or( level.visited, level.heroFOV, m.pos - 1 + level.width(), 3, level.visited );
				//updates adjacent cells too
				GameScene.updateFog(m.pos, 2);
			}
		}

		if (hero.buff(Awareness.class) != null){
			for (Heap h : level.heaps.valueList()){
				BArray.or( level.visited, level.heroFOV, h.pos - 1 - level.width(), 3, level.visited );
				BArray.or( level.visited, level.heroFOV, h.pos - 1, 3, level.visited );
				BArray.or( level.visited, level.heroFOV, h.pos - 1 + level.width(), 3, level.visited );
				GameScene.updateFog(h.pos, 2);
			}
		}

		for (TalismanOfForesight.CharAwareness c : hero.buffs(TalismanOfForesight.CharAwareness.class)){
			Char ch = (Char) Actor.findById(c.charID);
			if (ch == null || !ch.isAlive()) continue;
			BArray.or( level.visited, level.heroFOV, ch.pos - 1 - level.width(), 3, level.visited );
			BArray.or( level.visited, level.heroFOV, ch.pos - 1, 3, level.visited );
			BArray.or( level.visited, level.heroFOV, ch.pos - 1 + level.width(), 3, level.visited );
			GameScene.updateFog(ch.pos, 2);
		}

		for (TalismanOfForesight.HeapAwareness h : hero.buffs(TalismanOfForesight.HeapAwareness.class)){
			if (Dungeon.depth != h.depth || Dungeon.branch != h.branch) continue;
			BArray.or( level.visited, level.heroFOV, h.pos - 1 - level.width(), 3, level.visited );
			BArray.or( level.visited, level.heroFOV, h.pos - 1, 3, level.visited );
			BArray.or( level.visited, level.heroFOV, h.pos - 1 + level.width(), 3, level.visited );
			GameScene.updateFog(h.pos, 2);
		}

		for (RevealedArea a : hero.buffs(RevealedArea.class)){
			if (Dungeon.depth != a.depth || Dungeon.branch != a.branch) continue;
			BArray.or( level.visited, level.heroFOV, a.pos - 1 - level.width(), 3, level.visited );
			BArray.or( level.visited, level.heroFOV, a.pos - 1, 3, level.visited );
			BArray.or( level.visited, level.heroFOV, a.pos - 1 + level.width(), 3, level.visited );
			GameScene.updateFog(a.pos, 2);
		}

		for (Char ch : Actor.chars()){
			if (ch instanceof WandOfWarding.Ward
					|| ch instanceof WandOfRegrowth.Lotus
					|| ch instanceof SpiritHawk.HawkAlly
					|| ch.buff(PowerOfMany.PowerBuff.class) != null){
				x = ch.pos % level.width();
				y = ch.pos / level.width();

				//left, right, top, bottom
				dist = ch.viewDistance+1;
				l = Math.max( 0, x - dist );
				r = Math.min( x + dist, level.width() - 1 );
				t = Math.max( 0, y - dist );
				b = Math.min( y + dist, level.height() - 1 );

				width = r - l + 1;
				height = b - t + 1;

				pos = l + t * level.width();

				for (int i = t; i <= b; i++) {
					BArray.or( level.visited, level.heroFOV, pos, width, level.visited );
					pos+=level.width();
				}
				GameScene.updateFog(ch.pos, dist);
			}
		}

		GameScene.afterObserve();
	}

	//we store this to avoid having to re-allocate the array with each pathfind
	private static boolean[] passable;

	private static void setupPassable(){
		if (passable == null || passable.length != Dungeon.level.length())
			passable = new boolean[Dungeon.level.length()];
		else
			BArray.setFalse(passable);
	}

	public static boolean[] findPassable(Char ch, boolean[] pass, boolean[] vis, boolean chars){
		return findPassable(ch, pass, vis, chars, chars);
	}

	public static boolean[] findPassable(Char ch, boolean[] pass, boolean[] vis, boolean chars, boolean considerLarge){
		setupPassable();
		if (ch.flying || ch.buff( Amok.class ) != null) {
			BArray.or( pass, Dungeon.level.avoid, passable );
		} else {
			System.arraycopy( pass, 0, passable, 0, Dungeon.level.length() );
		}

		if (considerLarge && Char.hasProp(ch, Char.Property.LARGE)){
			BArray.and( passable, Dungeon.level.openSpace, passable );
		}

		ch.modifyPassable(passable);

		if (chars) {
			for (Char c : Actor.chars()) {
				if (vis[c.pos]) {
					passable[c.pos] = false;
				}
			}
		}

		return passable;
	}

	public static PathFinder.Path findPath(Char ch, int to, boolean[] pass, boolean[] vis, boolean chars) {

		return PathFinder.find( ch.pos, to, findPassable(ch, pass, vis, chars) );

	}
	
	public static int findStep(Char ch, int to, boolean[] pass, boolean[] visible, boolean chars ) {

		if (Dungeon.level.adjacent( ch.pos, to )) {
			return Actor.findChar( to ) == null && pass[to] ? to : -1;
		}

		return PathFinder.getStep( ch.pos, to, findPassable(ch, pass, visible, chars) );

	}

	public static int flee( Char ch, int from, boolean[] pass, boolean[] visible, boolean chars ) {
		boolean[] passable = findPassable(ch, pass, visible, false, true);
		passable[ch.pos] = true;

		//chars affected by terror have a shorter lookahead and can't approach the fear source
		boolean canApproachFromPos = ch.buff(Terror.class) == null && ch.buff(Dread.class) == null;
		int step = PathFinder.getStepBack( ch.pos, from, canApproachFromPos ? 8 : 4, passable, canApproachFromPos );

		//only consider chars impassable if our retreat step runs into them
		while (step != -1 && Actor.findChar(step) != null && chars){
			passable[step] = false;
			step = PathFinder.getStepBack( ch.pos, from, canApproachFromPos ? 8 : 4, passable, canApproachFromPos );
		}
		return step;

	}

}

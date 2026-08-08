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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Bones;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.items.journal.DescentPage;
import xyz.gabriwar.warpedpixeldungeon.journal.GuideGraph;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.SacrificialFire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.SmokeScreen;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Web;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WellWater;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Awareness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ChampionEnemy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.LockedFloor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSight;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.PinCushion;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Regeneration;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.RevealedArea;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Shadows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.DivineSense;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.Stasis;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollGeomancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MobSpawner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Piranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogFist;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Sheep;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Bee;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.BlueDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Fairy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.GreenDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.PET;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.RedDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Scorpion;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.ShadowDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Spider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.SugarplumFairy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Velocirooster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.VioletDragon;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SacrificialParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.WindParticle;
import xyz.gabriwar.warpedpixeldungeon.items.BulletBelt;
import xyz.gabriwar.warpedpixeldungeon.items.Dewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.LevelDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.RedDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.Stylus;
import xyz.gabriwar.warpedpixeldungeon.items.VioletDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.YellowDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TalismanOfForesight;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TimekeepersHourglass;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfChallenge;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfEnchantment;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfIntuition;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.DimensionalSundial;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.EyeOfNewt;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.MossyClump;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrapMechanism;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrinketCatalyst;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfWarding;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Door;
import xyz.gabriwar.warpedpixeldungeon.levels.features.HighGrass;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.mechanics.ShadowCaster;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Swiftthistle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public abstract class Level implements Bundlable {
	
	public static enum Feeling {
		NONE,
		CHASM,
		WATER,
		GRASS,
		DARK,
		LARGE,
		TRAPS,
		SECRETS;

		public String title(){
			return Messages.get(this, name()+"_title");
		}

		public String desc() {
			return Messages.get(this, name()+"_desc");
		}
	}

	protected int width;
	protected int height;
	protected int length;
	
	protected static final float TIME_TO_RESPAWN	= 50;
	protected static final int PET_TICK = 1;

	public int version;
	
	public int[] map;
	public boolean[] visited;
	public boolean[] mapped;
	public boolean[] discoverable;

	public int viewDistance = Dungeon.isChallenged( Challenges.DARKNESS ) ? 2 : 8;
	
	public boolean[] heroFOV;
	
	public boolean[] passable;
	public boolean[] losBlocking;
	public boolean[] flamable;
	public boolean[] secret;
	public boolean[] solid;
	public boolean[] avoid;
	public boolean[] water;
	public boolean[] pit;

	public boolean[] openSpace;

	public float[] tileHeat;

	public Feeling feeling = Feeling.NONE;
	
	public int entrance;
	public int exit;

	public ArrayList<LevelTransition> transitions;

	//when a boss level has become locked.
	public boolean locked = false;

	// Sprouted: set by WndAscend/WndDescend to allow hero to proceed
	public boolean forcedone = false;

	// Sprouted: tracks first visit to journal/sokoban levels for one-time item drops
	public boolean firstVisit = false;

	// Sprouted: tracks whether all original-generation mobs are dead
	public boolean cleared = false;
	// Sprouted: prevents escape from the level when true
	public boolean sealedlevel = false;

	// Sprouted: counts hero moves on this level, and par/goal for the level
	public int currentmoves = 0;
	public int currentkills = 0;  // mobs killed on this floor (for Dewcharge scaling)
	public int movepar = 0;

	public HashSet<Mob> mobs;
	public SparseArray<Heap> heaps;
	public HashMap<Class<? extends Blob>,Blob> blobs;
	public SparseArray<Plant> plants;
	public SparseArray<Trap> traps;

	// Passive plant growth tracking
	public int lastGrowthTurn = -1;
	public int lastGrassRegrowthTurn = -1;
	public ArrayList<Integer> naturalPlantOrder = new ArrayList<>();
	public ArrayList<CustomTilemap> customTiles;
	public ArrayList<CustomTilemap> customWalls;
	public SparseArray<xyz.gabriwar.warpedpixeldungeon.tiles.butters.Butter> butter = new SparseArray<>();
	
	protected ArrayList<Item> itemsToSpawn = new ArrayList<>();

	protected Group visuals;
	protected Group wallVisuals;
	
	public int color1 = 0x004400;
	public int color2 = 0x88CC44;

	private static final String VERSION     = "version";
	private static final String WIDTH       = "width";
	private static final String HEIGHT      = "height";
	private static final String MAP			= "map";
	private static final String VISITED		= "visited";
	private static final String MAPPED		= "mapped";
	private static final String TRANSITIONS	= "transitions";
	private static final String LOCKED      = "locked";
	private static final String FIRST_VISIT = "firstVisit";
	private static final String CLEARED     = "cleared";
	private static final String SEALEDLEVEL = "sealedlevel";
	private static final String MOVES       = "currentmoves";
	private static final String KILLS       = "currentkills";
	private static final String MOVEPAR     = "movepar";
	private static final String HEAPS		= "heaps";
	private static final String PLANTS		= "plants";
	private static final String TRAPS       = "traps";
	private static final String CUSTOM_TILES= "customTiles";
	private static final String CUSTOM_WALLS= "customWalls";
	private static final String MOBS		= "mobs";
	private static final String NET_HEROES  = "netHeroes";
	private static final String BLOBS		= "blobs";
	private static final String FEELING		= "feeling";
	private static final String BUTTER      = "butter";
	private static final String LAST_GROWTH_TURN       = "last_growth_turn";
	private static final String LAST_GRASS_REGROWTH_TURN = "last_grass_regrowth_turn";
	private static final String NATURAL_PLANT_ORDER    = "natural_plant_order";
	private static final String TILE_HEAT              = "tile_heat";

	public void create() {

		Random.pushGenerator( Dungeon.seedCurDepth() );

		//TODO maybe just make this part of RegularLevel?
		if (!Dungeon.bossLevel() && Dungeon.branch == 0) {

			addItemToSpawn(Generator.random(Generator.Category.FOOD));

			if (Dungeon.posNeeded()) {
				Dungeon.LimitedDrops.STRENGTH_POTIONS.count++;
				addItemToSpawn( new PotionOfStrength() );
			}
			if (Dungeon.souNeeded()) {
				Dungeon.LimitedDrops.UPGRADE_SCROLLS.count++;
				//every 2nd scroll of upgrade is removed with forbidden runes challenge on
				//TODO while this does significantly reduce this challenge's levelgen impact, it doesn't quite remove it
				//for 0 levelgen impact, we need to do something like give the player all SOU, but nerf them
				//or give a random scroll (from a separate RNG) instead of every 2nd SOU
				if (!Dungeon.isChallenged(Challenges.NO_SCROLLS) || Dungeon.LimitedDrops.UPGRADE_SCROLLS.count%2 != 0){
					addItemToSpawn(new ScrollOfUpgrade());
				}
			}
			if (Dungeon.asNeeded()) {
				Dungeon.LimitedDrops.ARCANE_STYLI.count++;
				addItemToSpawn( new Stylus() );
			}
			if ( Dungeon.enchStoneNeeded() ){
				Dungeon.LimitedDrops.ENCH_STONE.drop();
				addItemToSpawn( new StoneOfEnchantment() );
			}
			if ( Dungeon.intStoneNeeded() ){
				Dungeon.LimitedDrops.INT_STONE.drop();
				addItemToSpawn( new StoneOfIntuition() );
			}
			if ( Dungeon.trinketCataNeeded() ){
				Dungeon.LimitedDrops.TRINKET_CATA.drop();
				addItemToSpawn( new TrinketCatalyst());
			}
			if ( Dungeon.beltNeeded() ){
				Dungeon.LimitedDrops.BULLET_BELT.count++;
				addItemToSpawn( new BulletBelt());
			}
			
			if (Dungeon.depth > 1) {
				//50% chance of getting a level feeling
				//~7.15% chance for each feeling
				switch (Random.Int( 14 )) {
					case 0:
						feeling = Feeling.CHASM;
						break;
					case 1:
						feeling = Feeling.WATER;
						break;
					case 2:
						feeling = Feeling.GRASS;
						break;
					case 3:
						feeling = Feeling.DARK;
						viewDistance = Math.round(5*viewDistance/8f);
						break;
					case 4:
						feeling = Feeling.LARGE;
						addItemToSpawn(Generator.random(Generator.Category.FOOD));
						break;
					case 5:
						feeling = Feeling.TRAPS;
						break;
					case 6:
						feeling = Feeling.SECRETS;
						break;
					default:
						//if-else statements are fine here as only one chance can be above 0 at a time
						if (Random.Float() < MossyClump.overrideNormalLevelChance()){
							feeling = MossyClump.getNextFeeling();
						} else if (Random.Float() < TrapMechanism.overrideNormalLevelChance()) {
							feeling = TrapMechanism.getNextFeeling();
						} else {
							feeling = Feeling.NONE;
						}
				}
			}
		}
		
		do {
			width = height = length = 0;

			transitions = new ArrayList<>();

			mobs = new HashSet<>();
			heaps = new SparseArray<>();
			blobs = new HashMap<>();
			plants = new SparseArray<>();
			traps = new SparseArray<>();
			customTiles = new ArrayList<>();
			customWalls = new ArrayList<>();
			butter = new SparseArray<>();
			naturalPlantOrder = new ArrayList<>();

		} while (!build());
		
		buildFlagMaps();
		cleanWalls();

		createMobs();
		createItems();

		//Descent Guide pages this level should hold, if not yet found
		for (String key : GuideGraph.pagesToSpawn( Dungeon.depth, Dungeon.branch )){
			int cell = randomDescentPageCell();
			if (cell != -1){
				DescentPage p = new DescentPage();
				p.page( key );
				drop( p, cell );
			}
		}

		lastGrowthTurn = Dungeon.cycleTurn;
		lastGrassRegrowthTurn = Dungeon.cycleTurn;

		Random.popGenerator();
	}

	private int randomDescentPageCell(){
		int tries = 300;
		while (tries-- > 0){
			int cell = Random.Int( length() );
			if (passable[cell] && !avoid[cell] && heaps.get( cell ) == null && findMob( cell ) == null){
				return cell;
			}
		}
		return -1;
	}
	
	public void setSize(int w, int h){
		
		width = w;
		height = h;
		length = w * h;
		
		map = new int[length];
		Arrays.fill( map, feeling == Level.Feeling.CHASM ? Terrain.CHASM : Terrain.WALL );
		
		visited     = new boolean[length];
		mapped      = new boolean[length];
		
		heroFOV     = new boolean[length];
		
		passable	= new boolean[length];
		losBlocking	= new boolean[length];
		flamable	= new boolean[length];
		secret		= new boolean[length];
		solid		= new boolean[length];
		avoid		= new boolean[length];
		water		= new boolean[length];
		pit			= new boolean[length];

		openSpace   = new boolean[length];

		tileHeat    = new float[length];

		PathFinder.setMapSize(w, h);
	}
	
	public void reset() {
		
		for (Mob mob : mobs.toArray( new Mob[0] )) {
			if (!mob.reset()) {
				mobs.remove( mob );
			}
		}
		createMobs();
	}

	public void playLevelMusic(){
		//do nothing by default
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {

		version = bundle.getInt( VERSION );
		
		//saves from before v2.5.4 are not supported
		if (version < WarpedPixelDungeon.v2_5_4){
			throw new RuntimeException("old save");
		}

		setSize( bundle.getInt(WIDTH), bundle.getInt(HEIGHT));

		if (bundle.contains(TILE_HEAT)) {
			tileHeat = bundle.getFloatArray(TILE_HEAT);
		}

		mobs = new HashSet<>();
		heaps = new SparseArray<>();
		blobs = new HashMap<>();
		plants = new SparseArray<>();
		traps = new SparseArray<>();
		customTiles = new ArrayList<>();
		customWalls = new ArrayList<>();
		butter = new SparseArray<>();

		map		= bundle.getIntArray( MAP );

		visited	= bundle.getBooleanArray( VISITED );
		mapped	= bundle.getBooleanArray( MAPPED );

		transitions = new ArrayList<>();
		for (Bundlable b : bundle.getCollection( TRANSITIONS )){
			transitions.add((LevelTransition) b);
		}

		locked      = bundle.getBoolean( LOCKED );
		firstVisit  = bundle.getBoolean( FIRST_VISIT );
		cleared     = bundle.getBoolean( CLEARED );
		sealedlevel = bundle.getBoolean( SEALEDLEVEL );
		currentmoves = bundle.getInt( MOVES );
		currentkills = bundle.getInt( KILLS );
		movepar     = bundle.getInt( MOVEPAR );

		Collection<Bundlable> collection = bundle.getCollection( HEAPS );
		for (Bundlable h : collection) {
			Heap heap = (Heap)h;
			if (!heap.isEmpty())
				heaps.put( heap.pos, heap );
		}
		
		collection = bundle.getCollection( PLANTS );
		for (Bundlable p : collection) {
			Plant plant = (Plant)p;
			plants.put( plant.pos, plant );
		}

		lastGrowthTurn = bundle.contains(LAST_GROWTH_TURN)
				? bundle.getInt(LAST_GROWTH_TURN) : 0;
		lastGrassRegrowthTurn = bundle.contains(LAST_GRASS_REGROWTH_TURN)
				? bundle.getInt(LAST_GRASS_REGROWTH_TURN) : 0;
		naturalPlantOrder = new ArrayList<>();
		if (bundle.contains(NATURAL_PLANT_ORDER)) {
			for (int pos : bundle.getIntArray(NATURAL_PLANT_ORDER)) {
				naturalPlantOrder.add(pos);
			}
		} else {
			// Old save fallback: reconstruct from non-player-planted plants (order is arbitrary)
			for (Plant p : plants.valueList()) {
				if (!p.playerPlanted) naturalPlantOrder.add(p.pos);
			}
		}

		collection = bundle.getCollection( TRAPS );
		for (Bundlable p : collection) {
			Trap trap = (Trap)p;
			traps.put( trap.pos, trap );
		}

		if (bundle.contains( BUTTER )) {
			collection = bundle.getCollection( BUTTER );
			for (Bundlable b : collection) {
				xyz.gabriwar.warpedpixeldungeon.tiles.butters.Butter butEntry =
						(xyz.gabriwar.warpedpixeldungeon.tiles.butters.Butter) b;
				butter.put( butEntry.pos, butEntry );
			}
		}

		collection = bundle.getCollection( CUSTOM_TILES );
		for (Bundlable p : collection) {
			CustomTilemap vis = (CustomTilemap)p;
			customTiles.add(vis);
		}

		collection = bundle.getCollection( CUSTOM_WALLS );
		for (Bundlable p : collection) {
			CustomTilemap vis = (CustomTilemap)p;
			customWalls.add(vis);
		}
		
		collection = bundle.getCollection( MOBS );
		for (Bundlable m : collection) {
			Mob mob = (Mob)m;
			if (mob != null) {
				mobs.add( mob );
			}
		}
		
		collection = bundle.getCollection( BLOBS );
		for (Bundlable b : collection) {
			Blob blob = (Blob)b;
			blobs.put( blob.getClass(), blob );
		}

		feeling = bundle.getEnum( FEELING, Feeling.class );
		if (feeling == Feeling.DARK) {
			viewDistance = Math.round(5 * viewDistance / 8f);
		}

		if (bundle.contains( "mobs_to_spawn" )) {
			for (Class<? extends Mob> mob : bundle.getClassArray("mobs_to_spawn")) {
				if (mob != null) mobsToSpawn.add(mob);
			}
		}

		if (bundle.contains( "respawner" )){
			respawner = (MobSpawner) bundle.get("respawner");
		}

		// Net MP host: restore persisted netHeroes. Re-Actor.add them and stash by
		// owner name so reconnecting players can claim back their hero state.
		if (bundle.contains(NET_HEROES)
				&& xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()) {
			for (Bundlable b : bundle.getCollection(NET_HEROES)) {
				if (b instanceof xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero) {
					xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero h
							= (xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero) b;
					xyz.gabriwar.warpedpixeldungeon.actors.Actor.add(h);
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.registerLoadedNetHero(h);
				}
			}
		}

		buildFlagMaps();
		cleanWalls();

	}
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		bundle.put( VERSION, Game.versionCode );
		bundle.put( WIDTH, width );
		bundle.put( HEIGHT, height );
		bundle.put( MAP, map );
		bundle.put( VISITED, visited );
		bundle.put( MAPPED, mapped );
		bundle.put( TRANSITIONS, transitions );
		bundle.put( LOCKED, locked );
		bundle.put( FIRST_VISIT, firstVisit );
		bundle.put( CLEARED, cleared );
		bundle.put( SEALEDLEVEL, sealedlevel );
		bundle.put( MOVES, currentmoves );
		bundle.put( KILLS, currentkills );
		bundle.put( MOVEPAR, movepar );
		bundle.put( HEAPS, heaps.valueList() );
		bundle.put( PLANTS, plants.valueList() );
		bundle.put( TRAPS, traps.valueList() );
		bundle.put( LAST_GROWTH_TURN, lastGrowthTurn );
		bundle.put( LAST_GRASS_REGROWTH_TURN, lastGrassRegrowthTurn );
		int[] npo = new int[naturalPlantOrder.size()];
		for (int i = 0; i < npo.length; i++) npo[i] = naturalPlantOrder.get(i);
		bundle.put( NATURAL_PLANT_ORDER, npo );
		bundle.put( BUTTER, butter.valueList() );
		bundle.put( CUSTOM_TILES, customTiles );
		bundle.put( CUSTOM_WALLS, customWalls );
		bundle.put( MOBS, mobs );
		// Net MP host: persist remote players' Hero state (claimed + unclaimed)
		// alongside the level so reconnects (and host quit/relaunch) preserve their
		// items, lvl, exp, pos, buffs. Skipped on clients (NetManager.isHost == false).
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()) {
			java.util.Collection<xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero> nh
					= xyz.gabriwar.warpedpixeldungeon.net.NetManager.allKnownNetHeroes();
			if (!nh.isEmpty()) bundle.put( NET_HEROES, nh );
		}
		bundle.put( BLOBS, blobs.values() );
		if (tileHeat != null) {
			boolean hasHeat = false;
			for (float v : tileHeat) { if (v != 0) { hasHeat = true; break; } }
			if (hasHeat) bundle.put( TILE_HEAT, tileHeat );
		}
		bundle.put( FEELING, feeling );
		bundle.put( "mobs_to_spawn", mobsToSpawn.toArray(new Class[0]));
		bundle.put( "respawner", respawner );
	}
	
	public int tunnelTile() {
		return feeling == Feeling.CHASM ? Terrain.EMPTY_SP : Terrain.EMPTY;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public int length() {
		return length;
	}
	
	public String tilesTex() {
		return null;
	}
	
	public String waterTex() {
		return null;
	}

	// Returns frozen water texture for this level, or null if water here is
	// lava/unfrozen (City and Halls have lava, not water — they never freeze)
	public String frozenWaterTex() {
		return null;
	}

	// Whether water on this level can freeze (false for lava levels)
	public boolean waterCanFreeze() {
		return frozenWaterTex() != null;
	}

	abstract protected boolean build();
	
	private ArrayList<Class<?extends Mob>> mobsToSpawn = new ArrayList<>();
	
	public Mob createMob() {
		if (mobsToSpawn == null || mobsToSpawn.isEmpty()) {
			mobsToSpawn = MobSpawner.getMobRotation(Dungeon.depth);
		}

		Mob m = Reflection.newInstance(mobsToSpawn.remove(0));
		ChampionEnemy.rollForChampion(m);
		return m;
	}

	abstract protected void createMobs();

	abstract protected void createItems();

	//The Sprouted floors (mines, catacombs, chasm, fortress) are hand-built caves rather than
	//RegularLevel room graphs, so they can't lean on its room-based scatter - and porting them
	//that way silently dropped everything RegularLevel supplied. This is the core of it,
	//placed with the room-free picker: random loot, chests, skeletons, gold, itemsToSpawn, bones.
	protected void scatterCaveLoot() {
		int nItems = 3 + Random.chances(new float[]{6, 3, 1}); // 3/4/5 @ 60/30/10%
		for (int i = 0; i < nItems; i++) {
			Item toDrop = Generator.random();
			if (toDrop == null) continue;

			int cell = randomDestination(null);
			Heap.Type type;
			switch (Random.Int(20)) {
				case 0:
					type = Heap.Type.SKELETON; break;
				case 1: case 2: case 3: case 4: case 5:
					type = Heap.Type.CHEST; break;
				default:
					type = Heap.Type.HEAP; break;
			}
			Heap dropped = drop(toDrop, cell);
			dropped.type = type;
			if (type == Heap.Type.SKELETON) dropped.setHauntedIfCursed();
		}

		int nGold = Random.IntRange(1, 3);
		for (int i = 0; i < nGold; i++) {
			drop(new Gold(), randomDestination(null));
		}

		for (Item item : itemsToSpawn) {
			drop(item, randomDestination(null)).type = Heap.Type.HEAP;
		}

		ArrayList<Item> bonesItems = Bones.get();
		if (bonesItems != null) {
			int cell = randomDestination(null);
			for (Item i : bonesItems) {
				drop(i, cell).setHauntedIfCursed().type = Heap.Type.REMAINS;
			}
		}
	}

	public int entrance(){
		LevelTransition l = getTransition(null);
		if (l != null){
			return l.cell();
		}
		return 0;
	}

	public int exit(){
		LevelTransition l = getTransition(LevelTransition.Type.REGULAR_EXIT);
		if (l != null){
			return l.cell();
		}
		return 0;
	}

	public LevelTransition getTransition(LevelTransition.Type type){
		if (transitions.isEmpty()){
			return null;
		}
		for (LevelTransition transition : transitions){
			//if we don't specify a type, prefer to return any entrance
			if (type == null &&
					(transition.type == LevelTransition.Type.REGULAR_ENTRANCE
							|| transition.type == LevelTransition.Type.BRANCH_ENTRANCE
							|| transition.type == LevelTransition.Type.SURFACE)){
				return transition;
			} else if (transition.type == type){
				return transition;
			}
		}
		return type != null ? getTransition(null) : transitions.get(0);
	}

	public LevelTransition getTransition(int cell){
		for (LevelTransition transition : transitions){
			if (transition.inside(cell)){
				return transition;
			}
		}
		return null;
	}

	//returns true if we immediately transition, false otherwise
	public boolean activateTransition(Hero hero, LevelTransition transition){
		if (locked){
			return false;
		}

		beforeTransition();
		InterlevelScene.curTransition = transition;
		if (transition.type == LevelTransition.Type.REGULAR_EXIT
				|| transition.type == LevelTransition.Type.BRANCH_EXIT) {
			InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		} else {
			InterlevelScene.mode = InterlevelScene.Mode.ASCEND;
		}
		Game.switchScene(InterlevelScene.class);
		return true;
	}

	//some buff effects have special logic or are cancelled from the hero before transitioning levels
	public static void beforeTransition(){

		//time freeze effects need to resolve their pressed cells before transitioning
		TimekeepersHourglass.timeFreeze timeFreeze = Dungeon.hero.buff(TimekeepersHourglass.timeFreeze.class);
		if (timeFreeze != null) timeFreeze.disarmPresses();
		Swiftthistle.TimeBubble timeBubble = Dungeon.hero.buff(Swiftthistle.TimeBubble.class);
		if (timeBubble != null) timeBubble.disarmPresses();

		//iron stomach and challenge arena do not persist between floors
		Talent.WarriorFoodImmunity foodImmune = Dungeon.hero.buff(Talent.WarriorFoodImmunity.class);
		if (foodImmune != null) foodImmune.detach();
		ScrollOfChallenge.ChallengeArena arena = Dungeon.hero.buff(ScrollOfChallenge.ChallengeArena.class);
		if (arena != null) arena.detach();
		//awareness also doesn't, honestly it's weird that it's a buff
		Awareness awareness = Dungeon.hero.buff(Awareness.class);
		if (awareness != null) awareness.detach();

		Char ally = Stasis.getStasisAlly();
		if (Char.hasProp(ally, Char.Property.IMMOVABLE)){
			Dungeon.hero.buff(Stasis.StasisBuff.class).act();
			GLog.w(Messages.get(Stasis.StasisBuff.class, "left_behind"));
		}

		// Sprouted: save kills from this floor so next floor's Dewcharge is proportional
		if (Dungeon.dewDraw) Statistics.prevfloormoves = Dungeon.level.currentkills;

		// Sprouted: evaporate all dewdrops on the current level when leaving
		for (Heap heap : Dungeon.level.heaps.valueList()) {
			heap.dryup();
		}

		// Sprouted: save pet stats before level transition
		Dungeon.hero.pickUpPet();

		//spend the hero's partial turns,  so the hero cannot take partial turns between floors
		Dungeon.hero.spendToWhole();
		for (Actor a : Actor.all()){
			//also adjust any other actors that are now ahead of the hero due to this
			if (a.cooldown() < Dungeon.hero.cooldown()){
				a.spendToWhole();
			}
		}
	}

	public void seal(){
		if (!locked) {
			locked = true;
			Buff.affect(Dungeon.hero, LockedFloor.class);
		}
	}

	public void unseal(){
		if (locked) {
			locked = false;
			if (Dungeon.hero.buff(LockedFloor.class) != null){
				Dungeon.hero.buff(LockedFloor.class).detach();
			}
		}
	}

	public ArrayList<Item> getItemsToPreserveFromSealedResurrect(){
		ArrayList<Item> items = new ArrayList<>();
		for (Heap h : heaps.valueList()){
			if (h.type == Heap.Type.HEAP) {
				for (Item i : h.items){
					if (i instanceof Bomb){
						((Bomb) i).fuse = null;
					}
					items.add(i);
				}
			}
		}
		for (Mob m : mobs){
			for (PinCushion b : m.buffs(PinCushion.class)){
				items.addAll(b.getStuckItems());
			}
		}
		for (HeavyBoomerang.CircleBack b : Dungeon.hero.buffs(HeavyBoomerang.CircleBack.class)){
			if (b.activeDepth() == Dungeon.depth) items.add(b.cancel());
		}
		return items;
	}

	public Group addVisuals() {
		if (visuals == null || visuals.parent == null){
			visuals = new Group();
		} else {
			visuals.clear();
			visuals.camera = null;
		}
		for (int i=0; i < length(); i++) {
			if (pit[i]) {
				visuals.add( new WindParticle.Wind( i ) );
				if (i >= width() && water[i-width()]) {
					visuals.add( new FlowParticle.Flow( i - width() ) );
				}
			}
		}
		return visuals;
	}

	//for visual effects that should render above wall overhang tiles
	public Group addWallVisuals(){
		if (wallVisuals == null || wallVisuals.parent == null){
			wallVisuals = new Group();
		} else {
			wallVisuals.clear();
			wallVisuals.camera = null;
		}
		return wallVisuals;
	}

	
	public int mobLimit() {
		return 0;
	}

	public int mobCount(){
		float count = 0;
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
			if (mob.alignment == Char.Alignment.ENEMY && !mob.properties().contains(Char.Property.MINIBOSS)) {
				count += mob.spawningWeight();
			}
		}
		return Math.round(count);
	}

	public Mob findMob( int pos ){
		for (Mob mob : mobs){
			if (mob.pos == pos){
				return mob;
			}
		}
		return null;
	}

	private MobSpawner respawner;

	public Actor addRespawner() {
		if (respawner == null){
			respawner = new MobSpawner();
			Actor.addDelayed(respawner, respawnCooldown());
		} else {
			Actor.add(respawner);
			if (respawner.cooldown() > respawnCooldown()){
				respawner.resetCooldown();
			}
		}
		return respawner;
	}

	public float respawnCooldown(){
		float cooldown;
		if (Statistics.amuletObtained){
			if (Dungeon.depth == 1){
				//very fast spawns on floor 1! 0/2/4/6/8/10/12, etc.
				cooldown = (Dungeon.level.mobCount()) * (TIME_TO_RESPAWN / 25f);
			} else {
				//respawn time is 5/5/10/15/20/25/25, etc.
				cooldown = Math.round(GameMath.gate( TIME_TO_RESPAWN/10f, Dungeon.level.mobCount() * (TIME_TO_RESPAWN / 10f), TIME_TO_RESPAWN / 2f));
			}
		} else if (Dungeon.level.feeling == Feeling.DARK){
			cooldown = 2*TIME_TO_RESPAWN/3f;
		} else {
			cooldown = TIME_TO_RESPAWN;
		}
		cooldown *= DayNightCycle.spawnRateMultiplier();
		return cooldown / DimensionalSundial.spawnMultiplierAtCurrentTime();
	}

	public boolean spawnMob(int disLimit){
		PathFinder.buildDistanceMap(Dungeon.hero.pos, BArray.or(passable, avoid, null));

		Mob mob = createMob();
		if (mob.state != mob.PASSIVE) {
			mob.state = mob.WANDERING;
		}
		int tries = 30;
		do {
			mob.pos = randomRespawnCell(mob);
			tries--;
		} while ((mob.pos == -1 || PathFinder.distance[mob.pos] < disLimit) && tries > 0);

		if (Dungeon.hero.isAlive() && mob.pos != -1 && PathFinder.distance[mob.pos] >= disLimit) {
			GameScene.add( mob );
			if (!mob.buffs(ChampionEnemy.class).isEmpty()){
				GLog.w(Messages.get(ChampionEnemy.class, "warn"));
			}

			//very small chance to also spawn a piranha in a large water area
			if (Dungeon.depth < Dungeon.POSTGAME_DEPTH && Random.Int(500) == 0) {
				trySpawnWaterPiranha(disLimit);
			}

			return true;
		} else {
			return false;
		}
	}

	private void trySpawnWaterPiranha(int disLimit) {
		int tries = 30;
		int cell;
		do {
			cell = Random.Int(length());
			tries--;
		} while (tries > 0 && (
				!water[cell]
				|| heroFOV[cell]
				|| Actor.findChar(cell) != null
				|| !isWaterPoolLargeEnough(cell)
				|| PathFinder.distance[cell] < disLimit));

		if (tries > 0) {
			Piranha piranha = Piranha.random();
			piranha.pos = cell;
			piranha.state = piranha.WANDERING;
			GameScene.add(piranha);
		}
	}

	private boolean isWaterPoolLargeEnough(int cell) {
		int waterNeighbors = 0;
		for (int n : PathFinder.NEIGHBOURS8) {
			if (water[cell + n]) waterNeighbors++;
		}
		return waterNeighbors >= 5;
	}
	
	public int randomRespawnCell( Char ch ) {
		int cell;
		int count = 0;
		do {

			if (++count > 30) {
				return -1;
			}

			cell = Random.Int( length() );

		} while ((Dungeon.level == this && heroFOV[cell])
				|| !passable[cell]
				|| (Char.hasProp(ch, Char.Property.LARGE) && !openSpace[cell])
				|| Actor.findChar( cell ) != null);
		return cell;
	}
	
	public int randomDestination( Char ch ) {
		int cell;
		do {
			cell = Random.Int( length() );
		} while (!passable[cell]
				|| (Char.hasProp(ch, Char.Property.LARGE) && !openSpace[cell]));
		return cell;
	}
	
	public void addItemToSpawn( Item item ) {
		if (item != null) {
			itemsToSpawn.add( item );
		}
	}

	public Item findPrizeItem(){ return findPrizeItem(null); }

	public Item findPrizeItem(Class<?extends Item> match){
		if (itemsToSpawn.size() == 0)
			return null;

		if (match == null){
			//if we have a trinket catalyst, always return that first
			for (Item item : itemsToSpawn){
				if (item instanceof TrinketCatalyst){
					itemsToSpawn.remove(item);
					return item;
				}
			}

			Item item = Random.element(itemsToSpawn);
			itemsToSpawn.remove(item);
			return item;
		}

		for (Item item : itemsToSpawn){
			if (match.isInstance(item)){
				itemsToSpawn.remove( item );
				return item;
			}
		}

		return null;
	}

	public void buildFlagMaps() {

		//sokoban-style trap terrains create their Trap on the fly when stepped
		//on, so nothing was registered for the renderer - they showed as plain
		//ground. register visual-only trap objects for them (idempotent)
		for (int i = 0; i < length(); i++){
			if (traps.get(i) != null) continue;
			if (map[i] == Terrain.FLEECING_TRAP){
				xyz.gabriwar.warpedpixeldungeon.levels.traps.FleecingTrap t =
						new xyz.gabriwar.warpedpixeldungeon.levels.traps.FleecingTrap();
				t.pos = i;
				t.visible = t.active = true;
				traps.put(i, t);
			} else if (map[i] == Terrain.CHANGE_SHEEP_TRAP){
				xyz.gabriwar.warpedpixeldungeon.levels.traps.ChangeSheepTrap t =
						new xyz.gabriwar.warpedpixeldungeon.levels.traps.ChangeSheepTrap();
				t.pos = i;
				t.visible = t.active = true;
				traps.put(i, t);
			}
		}
		
		for (int i=0; i < length(); i++) {
			int flags = Terrain.flags[map[i]];
			passable[i]		= (flags & Terrain.PASSABLE) != 0;
			losBlocking[i]	= (flags & Terrain.LOS_BLOCKING) != 0;
			flamable[i]		= (flags & Terrain.FLAMABLE) != 0;
			secret[i]		= (flags & Terrain.SECRET) != 0;
			solid[i]		= (flags & Terrain.SOLID) != 0;
			avoid[i]		= (flags & Terrain.AVOID) != 0;
			water[i]		= (flags & Terrain.LIQUID) != 0;
			pit[i]			= (flags & Terrain.PIT) != 0;
		}

		for (Blob b : blobs.values()){
			b.onBuildFlagMaps(this);
		}
		
		int lastRow = length() - width();
		for (int i=0; i < width(); i++) {
			passable[i] = avoid[i] = false;
			losBlocking[i] = solid[i] = true;
			passable[lastRow + i] = avoid[lastRow + i] = false;
			losBlocking[lastRow + i] = solid[lastRow + i] = true;
		}
		for (int i=width(); i < lastRow; i += width()) {
			passable[i] = avoid[i] = false;
			losBlocking[i] = solid[i] = true;
			passable[i + width()-1] = avoid[i + width()-1] = false;
			losBlocking[i + width()-1] = solid[i + width()-1] = true;
		}

		//an open space is large enough to fit large mobs. A space is open when it is not solid
		// and there is an open corner with both adjacent cells opens
		for (int i=0; i < length(); i++) {
			if (solid[i]){
				openSpace[i] = false;
			} else {
				for (int j = 1; j < PathFinder.CIRCLE8.length; j += 2){
					if (solid[i+PathFinder.CIRCLE8[j]]) {
						openSpace[i] = false;
					} else if (!solid[i+PathFinder.CIRCLE8[(j+1)%8]]
							&& !solid[i+PathFinder.CIRCLE8[(j+2)%8]]){
						openSpace[i] = true;
						break;
					}
				}
			}
		}

	}

	//updates open space both on the cell itself and adjacent cells
	public void updateOpenSpace(int cell){
		for (int i : PathFinder.NEIGHBOURS9) {
			if (solid[cell+i]){
				openSpace[cell+i] = false;
			} else {
				for (int j = 1; j < PathFinder.CIRCLE8.length; j += 2){
					if (solid[cell+i+PathFinder.CIRCLE8[j]]) {
						openSpace[cell+i] = false;
					} else if (!solid[cell+i+PathFinder.CIRCLE8[(j+1)%8]]
							&& !solid[cell+i+PathFinder.CIRCLE8[(j+2)%8]]){
						openSpace[cell+i] = true;
						break;
					}
				}
			}
		}
	}

	public void destroy( int pos ) {
		//if raw tile type is flammable or empty
		int terr = map[pos];
		if (terr == Terrain.EMPTY || terr == Terrain.EMPTY_DECO
				|| (Terrain.flags[map[pos]] & Terrain.FLAMABLE) != 0) {
			set(pos, Terrain.EMBERS);
		}
		Blob web = blobs.get(Web.class);
		if (web != null){
			web.clear(pos);
		}
	}

	//true for levels that are always fully lit and mapped (the town's buildings)
	public boolean noFogOfWar() {
		return false;
	}

	//true where a cell is drawn with no fog at all (the town on the surface)
	public boolean plainFogAt( int cell ) {
		return noFogOfWar();
	}

	public void cleanWalls() {
		if (discoverable == null || discoverable.length != length) {
			discoverable = new boolean[length()];
		}

		if (noFogOfWar()) {
			java.util.Arrays.fill( discoverable, true );
			return;
		}

		for (int i=0; i < length(); i++) {
			
			boolean d = false;
			
			for (int j=0; j < PathFinder.NEIGHBOURS9.length; j++) {
				int n = i + PathFinder.NEIGHBOURS9[j];
				if (n >= 0 && n < length() && map[n] != Terrain.WALL && map[n] != Terrain.WALL_DECO) {
					d = true;
					break;
				}
			}
			
			discoverable[i] = d;
		}
	}
	
	public static void set( int cell, int terrain ){
		set( cell, terrain, Dungeon.level );
	}
	
	public static void set( int cell, int terrain, Level level ) {
		Painter.set( level, cell, terrain );
		if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()) {
			xyz.gabriwar.warpedpixeldungeon.net.StateSerializer.markCellDirty(cell);
		}

		if (terrain != Terrain.TRAP && terrain != Terrain.SECRET_TRAP && terrain != Terrain.INACTIVE_TRAP){
			level.traps.remove( cell );
		}

		int flags = Terrain.flags[terrain];
		level.passable[cell]		= (flags & Terrain.PASSABLE) != 0;
		level.losBlocking[cell]	    = (flags & Terrain.LOS_BLOCKING) != 0;
		level.flamable[cell]		= (flags & Terrain.FLAMABLE) != 0;
		level.secret[cell]		    = (flags & Terrain.SECRET) != 0;
		level.solid[cell]			= (flags & Terrain.SOLID) != 0;
		level.avoid[cell]			= (flags & Terrain.AVOID) != 0;
		level.pit[cell]			    = (flags & Terrain.PIT) != 0;
		level.water[cell]			= terrain == Terrain.WATER;

		if (level instanceof SewerLevel){
			if (level.map[cell] == Terrain.REGION_DECO || level.map[cell] == Terrain.REGION_DECO_ALT){
				level.flamable[cell] = true;
			}
		}

		for (int i : PathFinder.NEIGHBOURS9){
			i = cell + i;
			if (level.solid[i]){
				level.openSpace[i] = false;
			} else {
				for (int j = 1; j < PathFinder.CIRCLE8.length; j += 2){
					if (level.solid[i+PathFinder.CIRCLE8[j]]) {
						level.openSpace[i] = false;
					} else if (!level.solid[i+PathFinder.CIRCLE8[(j+1)%8]]
							&& !level.solid[i+PathFinder.CIRCLE8[(j+2)%8]]){
						level.openSpace[i] = true;
						break;
					}
				}
			}
		}
	}
	
	public Heap drop( Item item, int cell ) {

		if (item == null || Challenges.isItemBlocked(item)){

			//create a dummy heap, give it a dummy sprite, don't add it to the game, and return it.
			//effectively nullifies whatever the logic calling this wants to do, including dropping items.
			Heap heap = new Heap();
			ItemSprite sprite = heap.sprite = new ItemSprite();
			sprite.link(heap);
			return heap;

		}
		
		Heap heap = heaps.get( cell );
		if (heap == null) {
			
			heap = new Heap();
			heap.seen = Dungeon.level == this && heroFOV[cell];
			heap.pos = cell;
			heap.drop(item);
			if (map[cell] == Terrain.CHASM || (Dungeon.level != null && pit[cell])) {
				Dungeon.dropToChasm( item );
				GameScene.discard( heap );
			} else {
				heaps.put( cell, heap );
				GameScene.add( heap );
			}
			
		} else if (heap.type == Heap.Type.LOCKED_CHEST || heap.type == Heap.Type.CRYSTAL_CHEST) {
			
			int n;
			do {
				n = cell + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
			} while (!passable[n] && !avoid[n]);
			return drop( item, n );
			
		} else {
			heap.drop(item);
		}
		
		if (Dungeon.level != null && WarpedPixelDungeon.scene() instanceof GameScene) {
			pressCell( cell );
		}
		
		return heap;
	}
	
	public Plant plant( Plant.Seed seed, int pos ) {

		//in the safe zone, tilled soil grows a persistent farm crop instead of a
		//normal one-shot plant: it ripens over game time and clones its own seed
		if (this instanceof SafeLevel
				&& map[pos] == Terrain.FURROWED_GRASS
				&& !Dungeon.isChallenged( Challenges.NO_HERBALISM )){

			//the crop is an immovable actor so it can't share a cell. when the target
			//is free, sow it there; when the hero is standing on the plot (the Plant
			//verb targets the hero's own cell) redirect to an adjacent tilled cell
			int sowAt = -1;
			if (Actor.findChar( pos ) == null){
				sowAt = pos;
			} else if (Actor.findChar( pos ) == Dungeon.hero){
				for (int d : PathFinder.NEIGHBOURS8){
					int c = pos + d;
					if (c >= 0 && c < length()
							&& map[c] == Terrain.FURROWED_GRASS
							&& Actor.findChar( c ) == null){
						sowAt = c;
						break;
					}
				}
			}

			if (sowAt != -1){
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop crop
						= new xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop();
				crop.pos = sowAt;
				crop.sow( (Plant.Seed) com.watabou.utils.Reflection.newInstance( seed.getClass() ) );
				GameScene.add( crop );
				return null;
			}

			//no free tilled cell to sow (hero boxed in): give the seed back rather
			//than consuming it silently, and hint to make room
			GLog.w( Messages.get( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop.class, "no_room" ) );
			drop( com.watabou.utils.Reflection.newInstance( seed.getClass() ), pos );
			return null;
		}

		Plant plant = plants.get( pos );
		if (plant != null) {
			plant.wither();
		}

		if (map[pos] == Terrain.HIGH_GRASS ||
				map[pos] == Terrain.FURROWED_GRASS ||
				map[pos] == Terrain.EMPTY ||
				map[pos] == Terrain.EMBERS ||
				map[pos] == Terrain.EMPTY_DECO) {
			set(pos, Terrain.GRASS, this);
			GameScene.updateMap(pos);
		}

		//we have to get this far as grass placement has RNG implications in levelgen
		if (Dungeon.isChallenged(Challenges.NO_HERBALISM)){
			return null;
		}
		
		plant = seed.couch( pos, this );
		plants.put( pos, plant );
		
		GameScene.plantSeed( pos );

		for (Char ch : Actor.chars()){
			if (ch instanceof WandOfRegrowth.Lotus
					&& ((WandOfRegrowth.Lotus) ch).inRange(pos)
					&& Actor.findChar(pos) != null){
				plant.trigger();
				return null;
			}
		}
		
		return plant;
	}
	
	public void uproot( int pos ) {
		plants.remove(pos);
		naturalPlantOrder.remove(Integer.valueOf(pos));
		GameScene.updateMap( pos );
	}

	public void plantNatural( Plant plant, int pos ) {
		Plant existing = plants.get(pos);
		if (existing != null) {
			existing.wither();
		}

		plant.pos = pos;
		plant.playerPlanted = false;
		plants.put(pos, plant);
		naturalPlantOrder.add(pos);
		GameScene.updateMap(pos);
	}

	public Trap setTrap( Trap trap, int pos ){
		Trap existingTrap = traps.get(pos);
		if (existingTrap != null){
			traps.remove( pos );
		}
		trap.set( pos );
		traps.put( pos, trap );
		GameScene.updateMap( pos );
		return trap;
	}

	public void setButters(xyz.gabriwar.warpedpixeldungeon.tiles.butters.Butter b, int pos) {
		butter.remove(pos);
		b.set(pos);
		butter.put(pos, b);
		GameScene.updateMap(pos);
	}

	public void disarmTrap( int pos ) {
		set(pos, Terrain.INACTIVE_TRAP);
		GameScene.updateMap(pos);
	}

	public void discover( int cell ) {
		set( cell, Terrain.discover( map[cell] ) );
		Trap trap = traps.get( cell );
		if (trap != null)
			trap.reveal();
		GameScene.updateMap( cell );
	}

	public boolean setCellToWater( boolean includeTraps, int cell ){
		Point p = cellToPoint(cell);

		//if a custom tilemap is over that cell, don't put water there
		for (CustomTilemap cust : customTiles){
			Point custPoint = new Point(p);
			custPoint.x -= cust.tileX;
			custPoint.y -= cust.tileY;
			if (custPoint.x >= 0 && custPoint.y >= 0
					&& custPoint.x < cust.tileW && custPoint.y < cust.tileH){
				if (cust.image(custPoint.x, custPoint.y) != null){
					return false;
				}
			}
		}

		int terr = map[cell];
		if (terr == Terrain.EMPTY || terr == Terrain.GRASS ||
				terr == Terrain.EMBERS || terr == Terrain.EMPTY_SP ||
				terr == Terrain.HIGH_GRASS || terr == Terrain.FURROWED_GRASS
				|| terr == Terrain.EMPTY_DECO){
			set(cell, Terrain.WATER);
			GameScene.updateMap(cell);
			return true;
		} else if (includeTraps && (terr == Terrain.SECRET_TRAP ||
				terr == Terrain.TRAP || terr == Terrain.INACTIVE_TRAP)){
			set(cell, Terrain.WATER);
			Dungeon.level.traps.remove(cell);
			GameScene.updateMap(cell);
			return true;
		}

		return false;
	}
	
	public int fallCell( boolean fallIntoPit ) {
		int result;
		do {
			result = randomRespawnCell( null );
			if (result == -1) return -1;
		} while (traps.get(result) != null
				|| findMob(result) != null);
		return result;
	}
	
	public void occupyCell( Char ch ){
		if (!ch.isImmune(Web.class) && Blob.volumeAt(ch.pos, Web.class) > 0){
			blobs.get(Web.class).clear(ch.pos);
			Web.affectChar( ch );
		}

		if (Blob.volumeAt(ch.pos, SacrificialFire.class) > 0 && ch.buff( SacrificialFire.Marked.class ) == null){
			if (Dungeon.level.heroFOV[ch.pos]) {
				CellEmitter.get(ch.pos).burst( SacrificialParticle.FACTORY, 5 );
			}
			Buff.prolong( ch, SacrificialFire.Marked.class, SacrificialFire.Marked.DURATION );
		}

		if (!ch.flying){

			//we call act here instead of detach in case the debuffs haven't managed to deal dmg once yet
			if (map[ch.pos] == Terrain.WATER){
				if (ch.buff(Burning.class) != null){
					ch.buff(Burning.class).act();
				}
				if (ch.buff(Ooze.class) != null){
					ch.buff(Ooze.class).act();
				}
			}

			if ( (map[ch.pos] == Terrain.GRASS || map[ch.pos] == Terrain.EMBERS)
					&& ch == Dungeon.hero && Dungeon.hero.hasTalent(Talent.REJUVENATING_STEPS)
					&& ch.buff(Talent.RejuvenatingStepsCooldown.class) == null){

				if (!Regeneration.regenOn()){
					set(ch.pos, Terrain.FURROWED_GRASS);
				} else if (ch.buff(Talent.RejuvenatingStepsFurrow.class) != null && ch.buff(Talent.RejuvenatingStepsFurrow.class).count() >= 200) {
					set(ch.pos, Terrain.FURROWED_GRASS);
				} else {
					set(ch.pos, Terrain.HIGH_GRASS);
					Buff.count(ch, Talent.RejuvenatingStepsFurrow.class, 3 - Dungeon.hero.pointsInTalent(Talent.REJUVENATING_STEPS));
				}
				GameScene.updateMap(ch.pos);
				Buff.affect(ch, Talent.RejuvenatingStepsCooldown.class, 15f - 5f*Dungeon.hero.pointsInTalent(Talent.REJUVENATING_STEPS));
			}
			
			if (pit[ch.pos]){
				if (ch == Dungeon.hero) {
					Chasm.heroFall(ch.pos);
				} else if (ch instanceof Mob) {
					Chasm.mobFall( (Mob)ch );
				}
				return;
			}
			
			//characters which are not the hero or a sheep 'soft' press cells
			pressCell( ch.pos, ch instanceof Hero || ch instanceof Sheep);
		} else {
			if (map[ch.pos] == Terrain.DOOR){
				Door.enter( ch.pos );
			}
		}

		if (ch.isAlive() && ch instanceof Piranha && !water[ch.pos]){
			((Piranha) ch).dieOnLand();
		}
	}
	
	//public method for forcing the hard press of a cell. e.g. when an item lands on it
	public void pressCell( int cell ){
		pressCell( cell, true );
	}
	
	//a 'soft' press ignores hidden traps
	//a 'hard' press triggers all things
	private void pressCell( int cell, boolean hard ) {

		Trap trap = null;
		
		switch (map[cell]) {
		
		case Terrain.SECRET_TRAP:
			if (hard) {
				trap = traps.get( cell );
				GLog.i(Messages.get(Level.class, "hidden_trap", trap.name()));
			}
			break;
			
		case Terrain.TRAP:
			trap = traps.get( cell );
			break;
			
		case Terrain.HIGH_GRASS:
		case Terrain.FURROWED_GRASS:
		case Terrain.SOIL_CORNWHEAT:
		case Terrain.SOIL_GREENWHEAT:
		case Terrain.SOIL_STRAWWHEAT:
		case Terrain.SOIL_WATERWHEAT:
			// Crouching hero moves through grass without trampling it
			if (Dungeon.hero != null && Dungeon.hero.pos == cell
					&& Dungeon.hero.buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Crouching.class) != null) {
				break;
			}
			HighGrass.trample( this, cell);
			break;
			
		case Terrain.WELL:
			WellWater.affectCell( cell );
			break;
			
		case Terrain.DOOR:
			Door.enter( cell );
			break;
		}

		// Overgrown: butter tile effect
		xyz.gabriwar.warpedpixeldungeon.tiles.butters.Butter b = butter.get(cell);
		if (b != null) {
			Char ch = Actor.findChar(cell);
			if (ch != null) {
				b.stepOnEffect(ch);
			}
		}

		TimekeepersHourglass.timeFreeze timeFreeze =
				Dungeon.hero.buff(TimekeepersHourglass.timeFreeze.class);

		Swiftthistle.TimeBubble bubble =
				Dungeon.hero.buff(Swiftthistle.TimeBubble.class);

		if (trap != null) {
			if (bubble != null){
				Sample.INSTANCE.play(Assets.Sounds.TRAP);
				discover(cell);
				bubble.setDelayedPress(cell);
				
			} else if (timeFreeze != null){
				Sample.INSTANCE.play(Assets.Sounds.TRAP);
				discover(cell);
				timeFreeze.setDelayedPress(cell);
				
			} else {
				if (Dungeon.hero.pos == cell) {
					Dungeon.hero.interrupt();
					//skill tree: Lock Smith may jam the mechanism outright
					if (Dungeon.hero.heroSkills.anyDisableTrap()){
						trap.reveal();
						trap.disarm();
						return;
					}
				}
				trap.trigger();

			}
		}
		
		Plant plant = plants.get( cell );
		if (plant != null) {
			if (bubble != null){
				Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1, Random.Float( 0.96f, 1.05f ) );
				bubble.setDelayedPress(cell);

			} else if (timeFreeze != null){
				Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1, Random.Float( 0.96f, 1.05f ) );
				timeFreeze.setDelayedPress(cell);

			} else {
				plant.trigger();

			}
		}

		if (hard && Blob.volumeAt(cell, Web.class) > 0){
			blobs.get(Web.class).clear(cell);
		}
	}

	private static boolean[] heroMindFov;

	private static boolean[] modifiableBlocking;

	public void updateFieldOfView( Char c, boolean[] fieldOfView ) {

		int cx = c.pos % width();
		int cy = c.pos / width();
		
		boolean sighted = c.buff( Blindness.class ) == null && c.buff( Shadows.class ) == null
						&& c.isAlive();
		if (sighted) {
			boolean[] blocking = null;

			if (modifiableBlocking == null || modifiableBlocking.length != Dungeon.level.losBlocking.length){
				modifiableBlocking = new boolean[Dungeon.level.losBlocking.length];
			}

			//grass is see-through by some specific entities, but not during the fungi quest
			if (!(Dungeon.level instanceof  MiningLevel) || Blacksmith.Quest.Type() != Blacksmith.Quest.FUNGI){
				if ((c instanceof Hero && ((Hero) c).subClass == HeroSubClass.WARDEN)
						|| c instanceof YogFist.SoiledFist || c instanceof GnollGeomancer) {
					if (blocking == null) {
						System.arraycopy(Dungeon.level.losBlocking, 0, modifiableBlocking, 0, modifiableBlocking.length);
						blocking = modifiableBlocking;
					}
					for (int i = 0; i < blocking.length; i++) {
						if (blocking[i] && (Dungeon.level.map[i] == Terrain.HIGH_GRASS || Dungeon.level.map[i] == Terrain.FURROWED_GRASS)) {
							blocking[i] = false;
						}
					}
				}
			}

			//allies and specific enemies can see through shrouding fog
			if ((c.alignment != Char.Alignment.ALLY && !(c instanceof GnollGeomancer))
					&& Dungeon.level.blobs.containsKey(SmokeScreen.class)
					&& Dungeon.level.blobs.get(SmokeScreen.class).volume > 0) {
				if (blocking == null) {
					System.arraycopy(Dungeon.level.losBlocking, 0, modifiableBlocking, 0, modifiableBlocking.length);
					blocking = modifiableBlocking;
				}
				Blob s = Dungeon.level.blobs.get(SmokeScreen.class);
				for (int i = 0; i < blocking.length; i++){
					if (!blocking[i] && s.cur[i] > 0){
						blocking[i] = true;
					}
				}
			}

			if (blocking == null){
				blocking = Dungeon.level.losBlocking;
			}

			float viewDist = c.viewDistance;
			if (c instanceof Hero){
				viewDist *= 1f + 0.25f*((Hero) c).pointsInTalent(Talent.FARSIGHT);
				viewDist *= EyeOfNewt.visionRangeMultiplier();
				viewDist = Math.max( 2, viewDist + DayNightCycle.viewDistanceModifier() );
			} else if (c instanceof Mob) {
				// Dark/heat creatures have darkvision — unaffected by light level or weather
				boolean darkvision = c.properties().contains(Char.Property.UNDEAD)
						|| c.properties().contains(Char.Property.DEMONIC)
						|| c.properties().contains(Char.Property.FIERY);
				if (!darkvision) {
					viewDist = Math.max(2, viewDist + DayNightCycle.viewDistanceModifier());
				}
			}
			
			ShadowCaster.castShadow( cx, cy, width(), fieldOfView, blocking, Math.round(viewDist) );
		} else {
			BArray.setFalse(fieldOfView);
		}
		
		int sense = 1;
		//Currently only the hero can get mind vision
		if (c.isAlive() && c == Dungeon.hero) {
			for (Buff b : c.buffs( MindVision.class )) {
				sense = Math.max( ((MindVision)b).distance, sense );
			}
			if (c.buff(MagicalSight.class) != null){
				sense = Math.max( MagicalSight.DISTANCE, sense );
			}
		}
		
		//uses rounding
		if (!sighted || sense > 1) {
			
			int[][] rounding = ShadowCaster.rounding;
			
			int left, right;
			int pos;
			for (int y = Math.max(0, cy - sense); y <= Math.min(height()-1, cy + sense); y++) {
				if (rounding[sense][Math.abs(cy - y)] < Math.abs(cy - y)) {
					left = cx - rounding[sense][Math.abs(cy - y)];
				} else {
					left = sense;
					while (rounding[sense][left] < rounding[sense][Math.abs(cy - y)]){
						left--;
					}
					left = cx - left;
				}
				right = Math.min(width()-1, cx + cx - left);
				left = Math.max(0, left);
				pos = left + y * width();
				System.arraycopy(discoverable, pos, fieldOfView, pos, right - left + 1);
			}
		}

		if (c instanceof SpiritHawk.HawkAlly && Dungeon.hero.pointsInTalent(Talent.EAGLE_EYE) >= 3){
			int range = 1+(Dungeon.hero.pointsInTalent(Talent.EAGLE_EYE)-2);
			for (Mob mob : mobs) {
				int p = mob.pos;
				if (!fieldOfView[p] && distance(c.pos, p) <= range) {
					for (int i : PathFinder.NEIGHBOURS9) {
						fieldOfView[mob.pos + i] = true;
					}
				}
			}
		}

		//Currently only the hero can get mind vision or awareness
		if (c.isAlive() && c == Dungeon.hero) {

			if (heroMindFov == null || heroMindFov.length != length()){
				heroMindFov = new boolean[length];
			} else {
				BArray.setFalse(heroMindFov);
			}

			Dungeon.hero.mindVisionEnemies.clear();
			if (c.buff( MindVision.class ) != null) {
				for (Mob mob : mobs) {
					if (mob instanceof Mimic && mob.alignment == Char.Alignment.NEUTRAL&& ((Mimic) mob).stealthy()){
						continue;
					}
					for (int i : PathFinder.NEIGHBOURS9) {
						heroMindFov[mob.pos + i] = true;
					}
				}
			} else {

				int mindVisRange = 0;
				if (((Hero) c).hasTalent(Talent.HEIGHTENED_SENSES)){
					mindVisRange = 1+((Hero) c).pointsInTalent(Talent.HEIGHTENED_SENSES);
				}
				if (c.buff(DivineSense.DivineSenseTracker.class) != null){
					if (((Hero) c).heroClass == HeroClass.CLERIC){
						mindVisRange = 4+4*((Hero) c).pointsInTalent(Talent.DIVINE_SENSE);
					} else {
						mindVisRange = 1+2*((Hero) c).pointsInTalent(Talent.DIVINE_SENSE);
					}
				}
				mindVisRange = Math.max(mindVisRange, EyeOfNewt.mindVisionRange());

				//power of many's life link spell allows allies to get divine sense
				Char ally = PowerOfMany.getPoweredAlly();
				if (ally != null && ally.buff(DivineSense.DivineSenseTracker.class) == null){
					ally = null;
				}

				if (mindVisRange >= 1) {
					for (Mob mob : mobs) {
						if (mob instanceof Mimic && mob.alignment == Char.Alignment.NEUTRAL && ((Mimic) mob).stealthy()){
							continue;
						}
						int p = mob.pos;
						if (!fieldOfView[p] && (distance(c.pos, p) <= mindVisRange || (ally != null && distance(ally.pos, p) <= mindVisRange))) {
							for (int i : PathFinder.NEIGHBOURS9) {
								heroMindFov[mob.pos + i] = true;
							}
						}
					}
				}
			}
			
			if (c.buff( Awareness.class ) != null) {
				for (Heap heap : heaps.valueList()) {
					int p = heap.pos;
					for (int i : PathFinder.NEIGHBOURS9) heroMindFov[p+i] = true;
				}
			}

			for (TalismanOfForesight.CharAwareness a : c.buffs(TalismanOfForesight.CharAwareness.class)){
				Char ch = (Char) Actor.findById(a.charID);
				if (ch == null || !ch.isAlive()) {
					continue;
				}
				int p = ch.pos;
				for (int i : PathFinder.NEIGHBOURS9) heroMindFov[p+i] = true;
			}

			for (TalismanOfForesight.HeapAwareness h : c.buffs(TalismanOfForesight.HeapAwareness.class)){
				if (Dungeon.depth != h.depth || Dungeon.branch != h.branch) continue;
				for (int i : PathFinder.NEIGHBOURS9) heroMindFov[h.pos+i] = true;
			}

			for (Mob m : mobs){
				if (m instanceof WandOfWarding.Ward
						|| m instanceof WandOfRegrowth.Lotus
						|| m instanceof SpiritHawk.HawkAlly
						|| m.buff(PowerOfMany.PowerBuff.class) != null){
					if (m.fieldOfView == null || m.fieldOfView.length != length()){
						m.fieldOfView = new boolean[length()];
						Dungeon.level.updateFieldOfView( m, m.fieldOfView );
					}
					BArray.or(heroMindFov, m.fieldOfView, heroMindFov);
				}
			}

			for (RevealedArea a : c.buffs(RevealedArea.class)){
				if (Dungeon.depth != a.depth || Dungeon.branch != a.branch) continue;
				for (int i : PathFinder.NEIGHBOURS9) heroMindFov[a.pos+i] = true;
			}

			//set mind vision chars
			for (Mob mob : mobs) {
				if (heroMindFov[mob.pos] && !fieldOfView[mob.pos]){
					Dungeon.hero.mindVisionEnemies.add(mob);
				}
			}

			BArray.or(heroMindFov, fieldOfView, fieldOfView);

		}

		//debug no-fog, or a level with no fog at all: the hero sees everything, before heaps get marked
		if (c == Dungeon.hero && (Dungeon.debugNoFog || noFogOfWar())) {
			java.util.Arrays.fill( fieldOfView, true );
		}

		if (c == Dungeon.hero) {
			for (Heap heap : heaps.valueList())
				if (!heap.seen && fieldOfView[heap.pos])
					heap.seen = true;
		}

	}

	public float levelExplorePercent( int depth ){
		return 0;
	}
	
	public int distance( int a, int b ) {
		int ax = a % width();
		int ay = a / width();
		int bx = b % width();
		int by = b / width();
		return Math.max( Math.abs( ax - bx ), Math.abs( ay - by ) );
	}
	
	public boolean adjacent( int a, int b ) {
		return distance( a, b ) == 1;
	}
	
	//uses pythagorean theorum for true distance, as if there was no movement grid
	public float trueDistance(int a, int b){
		int ax = a % width();
		int ay = a / width();
		int bx = b % width();
		int by = b / width();
		return (float)Math.sqrt(Math.pow(Math.abs( ax - bx ), 2) + Math.pow(Math.abs( ay - by ), 2));
	}

	//usually just if a cell is solid, but other cases exist too
	public boolean invalidHeroPos( int tile ){
		return !passable[tile] && !avoid[tile];
	}

	//returns true if the input is a valid tile within the level
	public boolean insideMap( int tile ){
				//top and bottom row and beyond
		return !((tile < width || tile >= length - width) ||
				//left and right column
				(tile % width == 0 || tile % width == width-1));
	}

	public Point cellToPoint( int cell ){
		return new Point(cell % width(), cell / width());
	}

	public int pointToCell( Point p ){
		return p.x + p.y*width();
	}
	
	public String tileName( int tile ) {
		
		switch (tile) {
			case Terrain.CHASM:
				return Messages.get(Level.class, "chasm_name");
			case Terrain.EMPTY:
			case Terrain.EMPTY_SP:
			case Terrain.EMPTY_DECO:
			case Terrain.CUSTOM_DECO_EMPTY:
			case Terrain.SECRET_TRAP:
				return Messages.get(Level.class, "floor_name");
			case Terrain.GRASS:
				return Messages.get(Level.class, "grass_name");
			case Terrain.WATER:
				return Messages.get(Level.class, "water_name");
			case Terrain.WALL:
			case Terrain.WALL_DECO:
			case Terrain.SECRET_DOOR:
				return Messages.get(Level.class, "wall_name");
			case Terrain.DOOR:
				return Messages.get(Level.class, "closed_door_name");
			case Terrain.OPEN_DOOR:
				return Messages.get(Level.class, "open_door_name");
			case Terrain.ENTRANCE:
			case Terrain.ENTRANCE_SP:
				return Messages.get(Level.class, "entrace_name");
			case Terrain.EXIT:
				return Messages.get(Level.class, "exit_name");
			case Terrain.EMBERS:
				return Messages.get(Level.class, "embers_name");
			case Terrain.FROZEN_WATER:
				return Messages.get(Level.class, "frozen_water_name");
			case Terrain.FURROWED_GRASS:
				return Messages.get(Level.class, "furrowed_grass_name");
			case Terrain.LOCKED_DOOR:
			case Terrain.HERO_LKD_DR:
				return Messages.get(Level.class, "locked_door_name");
			case Terrain.CRYSTAL_DOOR:
				return Messages.get(Level.class, "crystal_door_name");
			case Terrain.PEDESTAL:
				return Messages.get(Level.class, "pedestal_name");
			case Terrain.BARRICADE:
				return Messages.get(Level.class, "barricade_name");
			case Terrain.HIGH_GRASS:
				return Messages.get(Level.class, "high_grass_name");
			case Terrain.LOCKED_EXIT:
				return Messages.get(Level.class, "locked_exit_name");
			case Terrain.UNLOCKED_EXIT:
				return Messages.get(Level.class, "unlocked_exit_name");
			case Terrain.WELL:
				return Messages.get(Level.class, "well_name");
			case Terrain.EMPTY_WELL:
				return Messages.get(Level.class, "empty_well_name");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(Level.class, "statue_name");
			case Terrain.INACTIVE_TRAP:
				return Messages.get(Level.class, "inactive_trap_name");
			case Terrain.BOOKSHELF:
				return Messages.get(Level.class, "bookshelf_name");
			case Terrain.EMPTY_BOOKSHELF:
				return Messages.get(Level.class, "bookshelf_name");
			case Terrain.ALCHEMY:
				return Messages.get(Level.class, "alchemy_name");
			default:
				return Messages.get(Level.class, "default_name");
		}
	}
	
	/** Extra examine text that depends on the CELL, not just the tile type
	 *  (tileDesc has no position). Null for most levels. */
	public String cellDescExtra( int cell ){
		return null;
	}

	public String tileDesc( int tile ) {
		
		switch (tile) {
			case Terrain.CHASM:
				return Messages.get(Level.class, "chasm_desc");
			case Terrain.WATER:
				return Messages.get(Level.class, "water_desc");
			case Terrain.ENTRANCE:
			case Terrain.ENTRANCE_SP:
				return Messages.get(Level.class, "entrance_desc");
			case Terrain.EXIT:
			case Terrain.UNLOCKED_EXIT:
				return Messages.get(Level.class, "exit_desc");
			case Terrain.EMBERS:
				return Messages.get(Level.class, "embers_desc");
			case Terrain.FROZEN_WATER:
				return Messages.get(Level.class, "frozen_water_desc");
			case Terrain.HIGH_GRASS:
			case Terrain.FURROWED_GRASS:
				return Messages.get(Level.class, "high_grass_desc");
			case Terrain.LOCKED_DOOR:
			case Terrain.HERO_LKD_DR:
				return Messages.get(Level.class, "locked_door_desc");
			case Terrain.CRYSTAL_DOOR:
				return Messages.get(Level.class, "crystal_door_desc");
			case Terrain.LOCKED_EXIT:
				return Messages.get(Level.class, "locked_exit_desc");
			case Terrain.BARRICADE:
				return Messages.get(Level.class, "barricade_desc");
			case Terrain.INACTIVE_TRAP:
				return Messages.get(Level.class, "inactive_trap_desc");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(Level.class, "statue_desc");
			case Terrain.ALCHEMY:
				return Messages.get(Level.class, "alchemy_desc");
			case Terrain.EMPTY_WELL:
				return Messages.get(Level.class, "empty_well_desc");
			default:
				return "";
		}
	}

	// Sprouted: true if any heap on this level contains a dewdrop of any type
	public boolean hasDew() {
		for (Heap heap : heaps.valueList()) {
			for (Item item : heap.items) {
				if (item instanceof Dewdrop || item instanceof YellowDewdrop
						|| item instanceof RedDewdrop || item instanceof VioletDewdrop) {
					return true;
				}
			}
		}
		return false;
	}

	// Sprouted: scatter basic dewdrops around a position (e.g. on mob death)
	public void explodeDew(int cell) {
		if (Dungeon.dewDraw) {
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 2);

			for (int n : PathFinder.NEIGHBOURS9) {
				int c = cell + n;
				if (c >= 0 && c < length() && passable[c]) {
					if (Random.Int(10) == 1) {
						Dungeon.level.drop(new RedDewdrop(), c).sprite.drop();
					} else if (Random.Int(3) == 1) {
						Dungeon.level.drop(new YellowDewdrop(), c).sprite.drop();
					}
				}
			}
		}
	}

	// Sprouted: scatter higher-tier dewdrops around a position
	public void explodeDewHigh(int cell) {
		if (Dungeon.dewDraw) {
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 2);

			for (int n : PathFinder.NEIGHBOURS9) {
				int c = cell + n;
				if (c >= 0 && c < length() && passable[c]) {
					if (Random.Int(8) == 1) {
						Dungeon.level.drop(new VioletDewdrop(), c).sprite.drop();
					} else if (Random.Int(2) == 1) {
						Dungeon.level.drop(new RedDewdrop(), c).sprite.drop();
					}
				}
			}
		}
	}

	// Sprouted: checks if any original-generation mobs remain alive
	public boolean checkOriginalGenMobs() {
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob.originalgen) {
				return true;
			}
		}
		return false;
	}

	// Sprouted: returns the move par including previous floor moves
	public int movepar() {
		return movepar + Statistics.prevfloormoves;
	}

	// Sprouted: periodic Actor that spawns/manages the hero's pet
	public Actor respawnerPet() {
		return new Actor() {
			@Override
			protected boolean act() {
				int petpos = -1;
				int heropos = Dungeon.hero.pos;
				if (Actor.findChar(heropos) != null && Dungeon.hero.petfollow) {
					ArrayList<Integer> candidates = new ArrayList<>();
					boolean[] pass = Dungeon.level.passable;

					for (int n : PathFinder.NEIGHBOURS8) {
						int c = heropos + n;
						if (pass[c] && Actor.findChar(c) == null) {
							candidates.add(c);
						}
					}

					petpos = candidates.size() > 0 ? Random.element(candidates) : -1;
				}

				if (petpos != -1 && Dungeon.hero.haspet && Dungeon.hero.petfollow) {

					PET petCheck = checkpet();
					if (petCheck != null) { petCheck.destroy(); petCheck.sprite.killAndErase(); }

					if (Dungeon.hero.petType == 1) {
						spawnPet(new Spider(), petpos, heropos);
					} else if (Dungeon.hero.petType == 2) {
						spawnPet(new Bee(), petpos, heropos);
					} else if (Dungeon.hero.petType == 3) {
						spawnPet(new Velocirooster(), petpos, heropos);
					} else if (Dungeon.hero.petType == 4) {
						spawnPet(new RedDragon(), petpos, heropos);
					} else if (Dungeon.hero.petType == 5) {
						spawnPet(new GreenDragon(), petpos, heropos);
					} else if (Dungeon.hero.petType == 6) {
						spawnPet(new VioletDragon(), petpos, heropos);
					} else if (Dungeon.hero.petType == 7) {
						spawnPet(new BlueDragon(), petpos, heropos);
					} else if (Dungeon.hero.petType == 8) {
						spawnPet(new Scorpion(), petpos, heropos);
					} else if (Dungeon.hero.petType == 9) {
						spawnPet(new Bunny(), petpos, heropos);
					} else if (Dungeon.hero.petType == 10) {
						spawnPet(new Fairy(), petpos, heropos);
					} else if (Dungeon.hero.petType == 11) {
						spawnPet(new SugarplumFairy(), petpos, heropos);
					} else if (Dungeon.hero.petType == 12) {
						spawnPet(new ShadowDragon(), petpos, heropos);
					}
				}

				if (!Dungeon.hero.haspet && checkpet() != null) {
					PET petCheck = checkpet();
					if (petCheck != null) { petCheck.destroy(); petCheck.sprite.killAndErase(); }
				}

				spend(PET_TICK);
				return true;
			}
		};
	}

	private PET checkpet() {
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof PET) {
				return (PET) mob;
			}
		}
		return null;
	}

	public void spawnPet(PET pet, int petpos, int heropos) {
		pet.spawn(Dungeon.hero.petLevel);
		pet.HP = Dungeon.hero.petHP;
		pet.pos = petpos;
		pet.state = pet.HUNTING;
		pet.kills = Dungeon.hero.petKills;
		pet.experience = Dungeon.hero.petExperience;
		pet.cooldown = Dungeon.hero.petCooldown;

		GameScene.add(pet);
		Actor.addDelayed(new Pushing(pet, heropos, petpos), -1f);
		Dungeon.hero.petfollow = false;
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.bounty;

import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bananaspider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Crab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Scorpio;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Slime;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownLedger;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.catching.CaughtFish;
import xyz.gabriwar.warpedpixeldungeon.items.food.Fish;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Clay;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.PineLog;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Sand;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.items.ore.CopperOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.IronOre;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoamerEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEvents;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel.Biome;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The villages' notice boards: what each posts, as a pure function of the world. Every human village
 * keeps a board by its well (BountySlice puts it down, NoticeBoard is the board). A board shows
 * BOARD_NOTICES notices (SMALL_NOTICES in a hamlet) that turn over every BOARD_DAYS days - each
 * village on a day of its own (phase) - and every notice is a pure function of (seed, village, period,
 * slot): two screens, a reload or a co-op guest always read the same board. What the hero has done
 * about them (taken, done, his standing) is BountyWorld's; what he does is BountyActions'.
 *
 * Three kinds, at most one of each while the land allows all three:
 * HUNT - kill some of a beast that lives round the village (PREY: the hostile rows of the surface's
 * fauna, OverworldFauna, for the biomes within SAMPLE_RINGS of the well), within HUNT_REACH of its well.
 * GATHER - bring some of a good the land round it gives (GOODS: hides, logs, stone, thatch, clay, sand,
 * ore, the bridge fishermen's fish and the tall grass's seeds, each only where its land is near).
 * DELIVER - carry a parcel to an elder of another human village within DELIVER_SECTORS sectors that has
 * one (elderHouse), in DELIVER_BASE + DELIVER_PER_CELL turns a cell between the wells.
 *
 * The reward is `coin` gold before the world's prices (TownLedger.scaled: the step up per five floors
 * reached the shops use), fixed when the notice is taken; one notice in BONUS_ODDS brings a potion, a
 * scroll or a runestone besides. Each bounty finished raises the village's standing by one: RANK_POINTS
 * are where it becomes known, trusted and honoured, each rank RANK_DISCOUNT off its traders' prices
 * (OverworldShopkeeper.priceFactor), its guards friendlier with every rank (BountyActions.guardLine).
 */
public final class Bounties {

	private Bounties(){}

	/** Days a board's notices stay up. */
	public static final int BOARD_DAYS = 3;
	/** Notices on a board, and on a hamlet's (fewer than SMALL_HOUSES houses). */
	public static final int BOARD_NOTICES = 3, SMALL_NOTICES = 2, SMALL_HOUSES = 4;
	/** Notices the hero carries at once. */
	public static final int MAX_TAKEN = 5;
	/** A kill counts for a village's hunt within this many cells of its well (Chebyshev), on the surface. */
	public static final int HUNT_REACH = 128;
	/** A parcel goes to a village within this many sectors (Chebyshev) of the one sending it. */
	public static final int DELIVER_SECTORS = 2;
	/** World turns to deliver a parcel: a base and a few for every cell between the wells. */
	public static final int DELIVER_BASE = 600, DELIVER_PER_CELL = 5;
	/** The log warns of a parcel when this share of its time is left (1/DUE_SHARE). */
	public static final int DUE_SHARE = 4;
	/** One notice in this many pays a potion, a scroll or a runestone besides its gold. */
	public static final int BONUS_ODDS = 3;
	/** Bounties finished for a village at which it knows, trusts and honours the hero. */
	public static final int[] RANK_POINTS = { 1, 3, 6 };
	/** Off the village's traders' prices, per rank. */
	public static final float RANK_DISCOUNT = 0.05f;
	/** The rings round the well (cells) whose land says what lives and grows near a village. */
	public static final int[] SAMPLE_RINGS = { 20, 36, 52 };

	private static final long PHASE_SALT = 0xB0A7D5A5EL, OFFER_SALT = 0xB0B7E110FL, ID_SALT = 0xB0A7D1DL,
			KIND_SALT = 0x5EED0B07L, TARGET_SALT = 0x7A26E7B0L, COUNT_SALT = 0xC0B07B0L, BONUS_SALT = 0xB0B05B0L,
			BEAST_SALT = 0xB0B7BEA5L;
	/** A great beast's notice (beastOffer): its coin, and the target its kind is named by ("beast_wolf"...). */
	public static final int BEAST_COIN = 120;
	static final String BEAST_TARGET = "beast_";

	// ------------------------------------------------------------ what is asked

	/** A beast a HUNT asks for: its kind (any subclass counts), how many, the gold a kill is worth and
	 *  the land it lives on (OverworldFauna's surface table: its hostile rows). */
	public static final class Prey {
		public final String id;
		public final Class<? extends Mob> cls;
		public final int min, max, coin, weight;
		public final EnumSet<Biome> land;

		Prey( String id, Class<? extends Mob> cls, int min, int max, int coin, int weight, Biome first, Biome... rest ){
			this.id = id;
			this.cls = cls;
			this.min = min;
			this.max = max;
			this.coin = coin;
			this.weight = weight;
			this.land = EnumSet.of( first, rest );
		}
	}

	/** A good a GATHER asks for: its item (any subclass counts), how many, the gold one is worth and
	 *  the land that gives it (null: any village's, the caves under it or the rock round it). */
	public static final class Good {
		public final String id;
		public final Class<? extends Item> cls;
		public final int min, max, coin, weight;
		public final EnumSet<Biome> land;
		//another item that counts as the good (or): a fish caught on a rod is a fish
		private Class<? extends Item> also;

		Good( String id, Class<? extends Item> cls, int min, int max, int coin, int weight, Biome... land ){
			this.id = id;
			this.cls = cls;
			this.min = min;
			this.max = max;
			this.coin = coin;
			this.weight = weight;
			this.land = land.length == 0 ? null : EnumSet.of( land[0], land );
		}

		Good or( Class<? extends Item> other ){
			also = other;
			return this;
		}

		/** Is this item the good (any subclass counts)? */
		public boolean takes( Item i ){
			return cls.isInstance( i ) || (also != null && also.isInstance( i ));
		}
	}

	//ids are saved with a taken notice: never rename one
	public static final Prey[] PREY = {
			new Prey( "albino_rat",   AlbinoRat.class,    4, 6,  6, 3, Biome.PLAINS, Biome.MEADOW, Biome.TUNDRA, Biome.SNOWFIELD ),
			new Prey( "gray_wolf",    GrayWolf.class,     3, 5, 16, 3, Biome.TUNDRA, Biome.SNOWFIELD, Biome.FOOTHILLS ),
			new Prey( "brown_wolf",   BrownWolf.class,    3, 5, 16, 3, Biome.FOREST ),
			new Prey( "bat",          Bat.class,          3, 4, 12, 2, Biome.FOREST, Biome.FOOTHILLS ),
			new Prey( "yeti",         Yeti.class,         1, 2, 30, 1, Biome.TUNDRA, Biome.SNOWFIELD, Biome.MOUNTAIN ),
			new Prey( "snake",        Snake.class,        3, 5,  8, 3, Biome.DESERT, Biome.SWAMP ),
			new Prey( "scorpio",      Scorpio.class,      2, 3, 20, 2, Biome.DESERT ),
			new Prey( "slime",        Slime.class,        3, 4, 10, 2, Biome.SWAMP ),
			new Prey( "bananaspider", Bananaspider.class, 2, 3, 12, 2, Biome.SWAMP ),
			new Prey( "crab",         Crab.class,         3, 5,  8, 3, Biome.BEACH ),
	};

	//ids are saved with a taken notice: never rename one
	public static final Good[] GOODS = {
			new Good( "hide",     Hide.class,        2,  3, 14, 3, Biome.MEADOW, Biome.FOREST, Biome.FOOTHILLS,
					Biome.TUNDRA, Biome.SNOWFIELD, Biome.MOUNTAIN ),
			new Good( "log",      Log.class,         6,  9,  6, 3, Biome.FOREST, Biome.MEADOW, Biome.SWAMP ),
			new Good( "pine_log", PineLog.class,     6,  9,  6, 2, Biome.FOREST, Biome.SNOWFIELD ),
			new Good( "stone",    RoughStone.class,  6,  9,  5, 2 ),
			new Good( "thatch",   Thatch.class,      8, 12,  3, 3, Biome.MEADOW, Biome.FOREST, Biome.SWAMP ),
			new Good( "clay",     Clay.class,        4,  6,  6, 2, Biome.SWAMP, Biome.RIVER ),
			new Good( "sand",     Sand.class,        4,  6,  4, 2, Biome.BEACH, Biome.DESERT ),
			new Good( "copper",   CopperOre.class,   3,  5, 12, 2 ),
			new Good( "iron",     IronOre.class,     2,  4, 20, 2 ),
			new Good( "fish",     Fish.class,        2,  3, 22, 2, Biome.RIVER ).or( CaughtFish.class ),
			new Good( "seeds",    Plant.Seed.class,  3,  4, 14, 2, Biome.MEADOW, Biome.FOREST, Biome.SWAMP ),
	};

	public static Prey prey( String id ){
		for (Prey p : PREY) if (p.id.equals( id )) return p;
		return null;
	}

	public static Good good( String id ){
		for (Good g : GOODS) if (g.id.equals( id )) return g;
		return null;
	}

	/** Does this build know what the notice asks for? (an older save's unknown one is dropped) */
	static boolean known( Bounty b ){
		if (b.kind == null) return false;
		switch (b.kind){
			case HUNT:   return prey( b.target ) != null || beastKind( b ) != null;
			case GATHER: return good( b.target ) != null;
			default:     return b.dest != Long.MIN_VALUE;
		}
	}

	// ------------------------------------------------------------ the villages (pure)

	/** Does the settlement of this sector keep a board: a village of people (no gnoll clan, no outlaws)? */
	public static boolean keepsBoard( long seed, int sx, int sy ){
		return WorldStructures.siteType( seed, sx, sy ) == WorldStructures.Site.VILLAGE
				&& WorldStructures.faction( seed, sx, sy ) == WorldStructures.Faction.HUMAN;
	}

	public static boolean keepsBoard( long seed, long key ){
		return key != Long.MIN_VALUE && keepsBoard( seed, (int)(key >> 32), (int) key );
	}

	public static int wellX( long seed, long key ){
		return WorldStructures.siteX( seed, (int)(key >> 32), (int) key );
	}

	public static int wellY( long seed, long key ){
		return WorldStructures.siteY( seed, (int)(key >> 32), (int) key );
	}

	public static String villageName( long seed, long key ){
		return WorldStructures.villageName( seed, (int)(key >> 32), (int) key );
	}

	/** How many houses the village of a key has. */
	public static int houses( long seed, long key ){
		VillageRoutine.Settlement s = VillageRoutine.settlement( seed, key );
		return s == null ? 0 : s.houses();
	}

	/** The village with a board whose well is within `reach` cells (Chebyshev) of a world cell, or
	 *  Long.MIN_VALUE: a board on a co-op guest's screen knows its village by where it stands. */
	public static long villageAt( long seed, int wx, int wy, int reach ){
		int sx0 = Math.floorDiv( wx, WorldStructures.SECTOR ), sy0 = Math.floorDiv( wy, WorldStructures.SECTOR );
		for (int sy = sy0 - 1; sy <= sy0 + 1; sy++){
			for (int sx = sx0 - 1; sx <= sx0 + 1; sx++){
				if (!keepsBoard( seed, sx, sy )) continue;
				if (Math.max( Math.abs( WorldStructures.siteX( seed, sx, sy ) - wx ),
						Math.abs( WorldStructures.siteY( seed, sx, sy ) - wy ) ) <= reach){
					return WorldStructures.sectorOf( sx, sy );
				}
			}
		}
		return Long.MIN_VALUE;
	}

	//what is cached per world: the land round each village, its elder and where its parcels go
	private static long cachedSeed = Long.MIN_VALUE;
	private static final Map<Long, EnumSet<Biome>> landCache = new ConcurrentHashMap<>();
	private static final Map<Long, Integer> elderCache = new ConcurrentHashMap<>();
	private static final Map<Long, List<Long>> targetCache = new ConcurrentHashMap<>();

	private static synchronized void checkSeed( long seed ){
		if (seed != cachedSeed){
			cachedSeed = seed;
			landCache.clear();
			elderCache.clear();
			targetCache.clear();
		}
	}

	/** The land round a village's well: the biomes, at the annual mean, of eight points on each of
	 *  SAMPLE_RINGS and of the well itself. */
	public static EnumSet<Biome> land( long seed, long key ){
		checkSeed( seed );
		EnumSet<Biome> cached = landCache.get( key );
		if (cached != null) return cached;
		int cx = wellX( seed, key ), cy = wellY( seed, key );
		EnumSet<Biome> near = EnumSet.of( WorldModel.baseBiomeAt( seed, cx, cy ) );
		for (int r : SAMPLE_RINGS){
			for (int d = 0; d < 8; d++){
				double a = d * Math.PI / 4;
				near.add( WorldModel.baseBiomeAt( seed, cx + (int)Math.round( Math.cos( a ) * r ),
						cy + (int)Math.round( Math.sin( a ) * r ) ) );
			}
		}
		landCache.put( key, near );
		return near;
	}

	/** The first house (VillageRoutine's layout index) whose family has an elder in it, or -1: the
	 *  families live in the houses past the vendors' (populateSettlement), 1 to 3 to a house, each
	 *  member's trade rolled from the family's hash (VillageRoutine.roleOf). */
	public static int elderHouse( long seed, long key ){
		checkSeed( seed );
		Integer cached = elderCache.get( key );
		if (cached != null) return cached;
		int found = -1;
		VillageRoutine.Settlement s = keepsBoard( seed, key ) ? VillageRoutine.settlement( seed, key ) : null;
		for (int h = s == null ? 0 : s.firstFamily(); s != null && found == -1 && h < s.lastFamily(); h++){
			long fh = VillageRoutine.familyHash( s.sx, s.sy, h );
			int members = 1 + (int) Math.floorMod( fh, 3L );
			for (int m = 0; m < members; m++){
				if (VillageRoutine.roleOf( fh, m, false ) == VillageRoutine.Role.ELDER){
					found = h;
					break;
				}
			}
		}
		elderCache.put( key, found );
		return found;
	}

	/** Where a village's parcels may go: every other village with a board and an elder within
	 *  DELIVER_SECTORS sectors, by sector key. */
	public static List<Long> deliverTargets( long seed, long key ){
		checkSeed( seed );
		List<Long> cached = targetCache.get( key );
		if (cached != null) return cached;
		ArrayList<Long> out = new ArrayList<>();
		int sx0 = (int)(key >> 32), sy0 = (int) key;
		for (int sy = sy0 - DELIVER_SECTORS; sy <= sy0 + DELIVER_SECTORS; sy++){
			for (int sx = sx0 - DELIVER_SECTORS; sx <= sx0 + DELIVER_SECTORS; sx++){
				if (sx == sx0 && sy == sy0) continue;
				long k = WorldStructures.sectorOf( sx, sy );
				if (keepsBoard( seed, sx, sy ) && elderHouse( seed, k ) != -1) out.add( k );
			}
		}
		Collections.sort( out );
		List<Long> result = Collections.unmodifiableList( out );
		targetCache.put( key, result );
		return result;
	}

	/** The distance between two villages' wells, in whole cells. */
	public static int distance( long seed, long a, long b ){
		long dx = wellX( seed, a ) - wellX( seed, b ), dy = wellY( seed, a ) - wellY( seed, b );
		return (int) Math.round( Math.sqrt( dx * dx + dy * dy ) );
	}

	// ------------------------------------------------------------ the boards' days (pure)

	/** The day of BOARD_DAYS a village's notices turn over on. */
	public static int phase( long seed, long key ){
		return (int) Math.floorMod( WorldEvents.mix( seed ^ PHASE_SALT, key, 0 ), (long) BOARD_DAYS );
	}

	/** The period of a village's board a day (WorldClock.day) falls in. */
	public static int period( long seed, long key, int day ){
		return Math.floorDiv( day + phase( seed, key ), BOARD_DAYS );
	}

	/** The first day of the period after this one: when its notices come down. */
	public static int periodEnd( long seed, long key, int period ){
		return (period + 1) * BOARD_DAYS - phase( seed, key );
	}

	/** A notice's identity: the village, the period and its place on the board. */
	public static long offerId( long key, int period, int slot ){
		return WorldEvents.mix( key ^ ID_SALT, period, slot );
	}

	// ------------------------------------------------------------ the notices (pure)

	/** The notices a village's board shows in a period, by slot: none for a village with no board. */
	public static ArrayList<Bounty> offers( long seed, long key, int period ){
		ArrayList<Bounty> out = new ArrayList<>();
		if (!keepsBoard( seed, key )) return out;
		EnumSet<Biome> land = land( seed, key );
		List<Long> targets = deliverTargets( seed, key );
		long h = WorldEvents.mix( seed ^ OFFER_SALT, key, period );

		ArrayList<Bounty.Kind> kinds = new ArrayList<>();
		if (anyPrey( land )) kinds.add( Bounty.Kind.HUNT );
		kinds.add( Bounty.Kind.GATHER );
		if (!targets.isEmpty()) kinds.add( Bounty.Kind.DELIVER );
		//shuffled by the period's hash: which kind tops the board turns over too
		for (int i = kinds.size() - 1; i > 0; i--){
			int j = (int) Math.floorMod( WorldEvents.mix( h, KIND_SALT, i ), (long)(i + 1) );
			Collections.swap( kinds, i, j );
		}

		int n = houses( seed, key ) < SMALL_HOUSES ? SMALL_NOTICES : BOARD_NOTICES;
		HashSet<String> used = new HashSet<>();
		for (int slot = 0; slot < n; slot++){
			long sh = WorldEvents.mix( h, slot, TARGET_SALT );
			Bounty.Kind kind;
			if (slot < kinds.size()){
				kind = kinds.get( slot );
			} else {
				//a fourth notice where a kind is missing: another hunt or gathering, of something else
				kind = anyPrey( land ) && (sh & 1L) == 0 ? Bounty.Kind.HUNT : Bounty.Kind.GATHER;
			}
			//a hunt with nothing left to hunt is a gathering
			Prey p = kind == Bounty.Kind.HUNT ? pickPrey( land, sh >>> 8, used ) : null;
			if (kind == Bounty.Kind.HUNT && p == null) kind = Bounty.Kind.GATHER;
			Bounty b = new Bounty();
			b.id = offerId( key, period, slot );
			b.village = key;
			b.period = period;
			b.slot = slot;
			b.kind = kind;
			long ch = WorldEvents.mix( sh, COUNT_SALT, slot );
			if (kind == Bounty.Kind.HUNT){
				b.target = p.id;
				b.need = p.min + (int) Math.floorMod( ch, (long)(p.max - p.min + 1) );
				b.coin = 20 + b.need * p.coin;
			} else if (kind == Bounty.Kind.GATHER){
				Good g = pickGood( land, sh >>> 8, used );
				b.target = g.id;
				b.need = g.min + (int) Math.floorMod( ch, (long)(g.max - g.min + 1) );
				b.coin = 20 + b.need * g.coin;
			} else {
				b.dest = targets.get( (int) Math.floorMod( sh >>> 8, (long) targets.size() ) );
				b.dist = distance( seed, key, b.dest );
				b.need = 1;
				b.coin = 25 + b.dist / 3;
			}
			used.add( b.target );
			long bh = WorldEvents.mix( sh, BONUS_SALT, slot );
			if (Math.floorMod( bh, (long) BONUS_ODDS ) == 0){
				b.bonus = Bounty.Bonus.values()[1 + (int) Math.floorMod( bh >>> 16, 3L )];
			}
			out.add( b );
		}
		return out;
	}

	private static boolean anyPrey( EnumSet<Biome> land ){
		for (Prey p : PREY) if (lives( p, land )) return true;
		return false;
	}

	private static boolean lives( Prey p, EnumSet<Biome> land ){
		for (Biome b : p.land) if (land.contains( b )) return true;
		return false;
	}

	private static boolean grows( Good g, EnumSet<Biome> land ){
		if (g.land == null) return true;
		for (Biome b : g.land) if (land.contains( b )) return true;
		return false;
	}

	//a weighted pick among the beasts of the land not already on the board (null: none left)
	private static Prey pickPrey( EnumSet<Biome> land, long h, HashSet<String> used ){
		int total = 0;
		for (Prey p : PREY) if (lives( p, land ) && !used.contains( p.id )) total += p.weight;
		if (total == 0) return null;
		int pick = (int) Math.floorMod( h, (long) total );
		for (Prey p : PREY){
			if (!lives( p, land ) || used.contains( p.id )) continue;
			pick -= p.weight;
			if (pick < 0) return p;
		}
		return null;
	}

	//a weighted pick among the goods of the land not already on the board (stone and ore grow anywhere)
	private static Good pickGood( EnumSet<Biome> land, long h, HashSet<String> used ){
		int total = 0;
		for (Good g : GOODS) if (grows( g, land ) && !used.contains( g.id )) total += g.weight;
		int pick = (int) Math.floorMod( h, (long) Math.max( 1, total ) );
		for (Good g : GOODS){
			if (!grows( g, land ) || used.contains( g.id )) continue;
			pick -= g.weight;
			if (pick < 0) return g;
		}
		return GOODS[3];
	}

	/** The notices still up on a board on a day: its period's offers but those taken, done or failed. */
	public static ArrayList<Bounty> openOffers( long seed, long key, int day, BountyWorld world ){
		ArrayList<Bounty> out = offers( seed, key, period( seed, key, day ) );
		if (world != null) out.removeIf( b -> world.isClosed( b.id ) );
		return out;
	}

	/** The notices up on a board on the surface now: its period's (openOffers), and below them the hunt
	 *  for a great beast roaming near the village (beastOffer), unless it is taken or done with. */
	public static ArrayList<Bounty> openOffers( OverworldLevel ow, long key, int day, BountyWorld world ){
		ArrayList<Bounty> out = openOffers( ow.worldSeed, key, day, world );
		Bounty beast = beastOffer( ow, key, period( ow.worldSeed, key, day ) );
		if (beast != null && (world == null || (!world.isClosed( beast.id ) && world.takenById( beast.id ) == null))){
			out.add( beast );
		}
		return out;
	}

	/**
	 * The hunt a village's board posts for a great beast (RoamerEvent) roaming within the reach of its
	 * folk's talk (RoamerEvent.RUMOUR sectors) of its well, the nearest one, while it is abroad and
	 * alive; null for none. Its id is the village's and the beast's, the same in every period of its
	 * days; it asks for that one beast, killed anywhere before its days end. Any thread.
	 */
	public static Bounty beastOffer( OverworldLevel ow, long key, int period ){
		if (ow == null || ow.altitude() != 0 || !keepsBoard( ow.worldSeed, key )) return null;
		long seed = ow.worldSeed;
		ArrayList<RoamerEvent.Sighting> near = RoamerEvent.abroad( ow, wellX( seed, key ), wellY( seed, key ), RoamerEvent.RUMOUR );
		if (near.isEmpty()) return null;
		RoamerEvent.Sighting s = near.get( 0 );
		Bounty b = new Bounty();
		b.id = WorldEvents.mix( key ^ BEAST_SALT, s.id, 0 );
		b.village = key;
		b.period = period;
		b.slot = BOARD_NOTICES;
		b.kind = Bounty.Kind.HUNT;
		b.target = BEAST_TARGET + s.kind.key();
		b.beast = s.id;
		b.need = 1;
		b.deadline = s.endTurn;
		b.coin = BEAST_COIN;
		//a great beast's notice always adds something
		b.bonus = Bounty.Bonus.values()[1 + (int) Math.floorMod( WorldEvents.mix( b.id, BONUS_SALT, 0 ), 3L )];
		return b;
	}

	/** The kind of great beast a notice hunts, or null for any other notice. */
	public static RoamerEvent.Kind beastKind( Bounty b ){
		if (b.kind != Bounty.Kind.HUNT || b.beast == Long.MIN_VALUE || b.target == null) return null;
		for (RoamerEvent.Kind k : RoamerEvent.Kind.values()) if ((BEAST_TARGET + k.key()).equals( b.target )) return k;
		return null;
	}

	/** A great beast's own name, "Old Greymane, the great wolf" (beastKind not null). */
	public static String beastName( Bounty b ){
		RoamerEvent.Kind k = beastKind( b );
		return Messages.get( Bounties.class, "beast_named", RoamerEvent.title( k, RoamerEvent.nameOf( b.beast ) ),
				Messages.get( Bounties.class, b.target ) );
	}

	// ------------------------------------------------------------ the rules

	/** The gold a notice pays if taken now: its coin at the world's prices. */
	public static int reward( Bounty b ){
		return TownLedger.scaled( b.coin );
	}

	/** World turns a parcel has to reach a well this many cells off. */
	public static int deliverTime( int dist ){
		return DELIVER_BASE + DELIVER_PER_CELL * dist;
	}

	/** The village's standing at so many bounties finished: 0 a stranger, 1 known, 2 trusted, 3 honoured. */
	public static int rank( int points ){
		int r = 0;
		for (int p : RANK_POINTS) if (points >= p) r++;
		return r;
	}

	/** What a village's traders charge the hero at a rank, against the shelf price. */
	public static float priceAt( int rank ){
		return 1f - RANK_DISCOUNT * rank;
	}

	// ------------------------------------------------------------ words

	/** The picture of a notice of a kind (items.png, the bounty row). */
	public static int icon( Bounty.Kind kind ){
		switch (kind){
			case HUNT:   return ItemSpriteSheet.NOTICE_HUNT;
			case GATHER: return ItemSpriteSheet.NOTICE_GATHER;
			default:     return ItemSpriteSheet.NOTICE_DELIVER;
		}
	}

	/** "4 grey wolves", "6 oak logs", "a parcel for Oakton". */
	public static String what( long seed, Bounty b ){
		switch (b.kind){
			case HUNT:
				if (beastKind( b ) != null) return beastName( b );
				return Messages.get( Bounties.class, "count", b.need,
						Messages.get( Bounties.class, "prey_" + b.target + (b.need == 1 ? "_1" : "") ) );
			case GATHER:
				return Messages.get( Bounties.class, "count", b.need, Messages.get( Bounties.class, "good_" + b.target ) );
			default:
				return Messages.get( Bounties.class, "parcel_for", villageName( seed, b.dest ) );
		}
	}

	/** "Hunt: 4 grey wolves". */
	public static String title( long seed, Bounty b ){
		return Messages.get( Bounties.class, "title_" + b.kind.name().toLowerCase( java.util.Locale.ENGLISH ), what( seed, b ) );
	}

	/** Where to look or what it takes: the prey's haunts, the good's source, the parcel's road. */
	public static String hint( long seed, Bounty b ){
		switch (b.kind){
			case HUNT:
				if (beastKind( b ) != null) return Messages.get( Bounties.class, "hint_beast", RoamerEvent.RUMOUR,
						hours( Math.max( 0, b.deadline - WorldClock.turn() ) ) );
				return Messages.get( Bounties.class, "hint_prey_" + b.target ) + " "
						+ Messages.get( Bounties.class, "hunt_reach", HUNT_REACH );
			case GATHER:
				return Messages.get( Bounties.class, "hint_good_" + b.target ) + " "
						+ Messages.get( Bounties.class, "gather_hand_in" );
			default:
				return Messages.get( Bounties.class, "hint_deliver", villageName( seed, b.dest ),
						direction( wellX( seed, b.dest ) - wellX( seed, b.village ), wellY( seed, b.dest ) - wellY( seed, b.village ) ),
						b.dist, hours( deliverTime( b.dist ) ) );
		}
	}

	/** "84 gold", "84 gold and a potion". */
	public static String pay( int gold, Bounty.Bonus bonus ){
		if (bonus == Bounty.Bonus.NONE) return Messages.get( Bounties.class, "pay", gold );
		return Messages.get( Bounties.class, "pay_bonus", gold,
				Messages.get( Bounties.class, "bonus_" + bonus.name().toLowerCase( java.util.Locale.ENGLISH ) ) );
	}

	/** A rank's name: "known", "trusted", "honoured" (a stranger has none: WndNoticeBoard says so). */
	public static String rankName( int rank ){
		return Messages.get( Bounties.class, "rank_" + Math.max( 1, Math.min( RANK_POINTS.length, rank ) ) );
	}

	/** World turns as hours of the day (a day of the world is 24 of them, on either clock), at least one. */
	public static int hours( int turns ){
		return Math.max( 1, (int) Math.ceil( turns * 24.0 / WorldEvents.FC ) );
	}

	/** One of the eight ways from a point to another (dx east, dy south): "north", "south-west"... */
	public static String direction( int dx, int dy ){
		double a = Math.atan2( -dy, dx );   //0 east, counterclockwise, north up
		int oct = (int) Math.floorMod( Math.round( a / (Math.PI / 4) ), 8L );
		String[] names = { "e", "ne", "n", "nw", "w", "sw", "s", "se" };
		return Messages.get( Bounties.class, "dir_" + names[oct] );
	}
}

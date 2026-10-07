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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalGuardian;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalWisp;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfLich;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FossilSkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FungalSentry;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FungalSpinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RiftElemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShrinePiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaveMiner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.DwarvenForge;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.LakeShrine;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.items.EnergyCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.BlueMilk;
import xyz.gabriwar.warpedpixeldungeon.items.food.DeathCap;
import xyz.gabriwar.warpedpixeldungeon.items.food.Earthstar;
import xyz.gabriwar.warpedpixeldungeon.items.food.GoldenJelly;
import xyz.gabriwar.warpedpixeldungeon.items.food.JackOLantern;
import xyz.gabriwar.warpedpixeldungeon.items.food.PixieParasol;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.Torch;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The places of the caves: an old mine, a mushroom grotto, a crystal cavern, a miners' camp, a
 * lake shrine, and deep down a burning rift with an ancient forge and a sealed dwarven tomb.
 *
 * Every place is a PURE function of (seed, altitude, sector), like WorldStructures' sites: each
 * cave slice is cut into SECTOR-wide squares, a hash says whether one holds a place and which
 * kind, and the place is set down on the first of a few probe points near the sector's middle
 * where the natural cave (WorldModel's field, nothing of a window) has room for it - open chamber
 * floor, clear of the ways between slices and their landings, its chamber still one chamber once
 * it is laid. Nothing about a place is stored: every window, every reload, the preparation
 * workers and every network mirror lay the very same cells. A place never leaves its sector, so
 * two never touch.
 *
 * What the generator lays (overlay, from WindowGenerator.caves after its ways): the terrain - a
 * gallery carved into the rock, a camp's fire, crystal studs, a tomb's walls and iron door - and
 * a mine's shaft down, whose bottom the slice below lays. What the dressing draws (dress): rails,
 * timbers, the camp's furniture, giant mushrooms, lava cracks, the tomb's dressed stone - each
 * only while its cell still holds the terrain the place laid, so a player's change hides it.
 * What the level peoples (populate, through OverworldLevel.populateLayerSites): the miners who
 * never left, the grotto's fungi, the cavern's wisps, the trader, the idol, the forge, the
 * tomb's lich; the caches, once each (claimHoard), and the guards again each day while their
 * cache still lies there (dueOnce). What the host ticks (tick): the rift's heat and its flares.
 *
 * Caching: one map for every slice and seed, filled with get, compute outside, putIfAbsent - a
 * resolution may ask the slice above (a mine's shaft lands on this one), and a recursive
 * computeIfAbsent spins forever on Android's map. The render thread never resolves: it reads the
 * level's resolved list (OverworldLevel.layerSites) and its found sites.
 */
public final class CaveSites {

	private CaveSites(){}

	public static final int SECTOR = 72;

	/** The kinds of place, shallowest altitude to deepest each may lie on. NEVER reorder: the
	 *  ordinal is the kind code of the found-sites registry (OverworldLevel.foundSites). */
	public enum Type {
		//      deepest, shallowest, the world map's pin (clear of the hero's red and the waypoint's blue)
		MINE(    -6,  -1, 0xFFC08A48 ),
		GROTTO(  -12, -1, 0xFF46DCC8 ),
		CRYSTAL( -12, -3, 0xFFB490F0 ),
		CAMP(    -8,  -2, 0xFFFF9A3C ),
		SHRINE(  -10, -2, 0xFF50C0FF ),
		RIFT(    -12, OverworldFauna.DEEP_CAVES, 0xFF8C1028 ),
		TOMB(    -12, -8, 0xFFD2CCB4 );

		public final int deepest, shallowest, pin;

		Type( int deepest, int shallowest, int pin ){
			this.deepest = deepest;
			this.shallowest = shallowest;
			this.pin = pin;
		}

		public String key(){ return name().toLowerCase( Locale.ENGLISH ); }

		public boolean at( int altitude ){ return altitude >= deepest && altitude <= shallowest; }
	}

	/** A place's name on the world map and in the HUD: "Old mine". */
	public static String mapName( Type t ){
		return Messages.get( CaveSites.class, "place_" + t.key() );
	}

	//the props the dressing draws: the tile, the top on the cell above (or -1), the layer (the
	//edges stand over the ground's transitions, the ground lies under them), the terrain the place
	//laid there (drawn only while the cell still holds it) and the examine name
	enum Prop {
		RAILS_EW(   OverworldDress.CAVE_RAILS_EW,      -1, false, Terrain.EMPTY,             "prop_rails" ),
		RAILS_NS(   OverworldDress.CAVE_RAILS_NS,      -1, false, Terrain.EMPTY,             "prop_rails" ),
		RAIL_END_E( OverworldDress.CAVE_RAIL_END_E,    -1, false, Terrain.EMPTY,             "prop_rails" ),
		RAIL_END_W( OverworldDress.CAVE_RAIL_END_W,    -1, false, Terrain.EMPTY,             "prop_rails" ),
		RAIL_END_S( OverworldDress.CAVE_RAIL_END_S,    -1, false, Terrain.EMPTY,             "prop_rails" ),
		RAIL_END_N( OverworldDress.CAVE_RAIL_END_N,    -1, false, Terrain.EMPTY,             "prop_rails" ),
		CART(       OverworldDress.CAVE_CART,          -1, true,  Terrain.CUSTOM_DECO,       "prop_cart" ),
		TIMBER(     OverworldDress.CAVE_TIMBER, OverworldDress.CAVE_TIMBER_TOP, true, Terrain.CUSTOM_DECO, "prop_timber" ),
		LANTERN(    OverworldDress.CAVE_LANTERN,       -1, true,  Terrain.CUSTOM_DECO,       "prop_lantern" ),
		CRATES(     OverworldDress.CAVE_CRATES,        -1, true,  Terrain.CUSTOM_DECO,       "prop_crates" ),
		BARREL(     OverworldDress.CAVE_BARREL,        -1, true,  Terrain.CUSTOM_DECO,       "prop_barrel" ),
		SACKS(      OverworldDress.CAVE_SACKS,         -1, true,  Terrain.CUSTOM_DECO,       "prop_sacks" ),
		TOOL_RACK(  OverworldDress.CAVE_TOOL_RACK,     -1, true,  Terrain.CUSTOM_DECO,       "prop_tools" ),
		FURNACE(    OverworldDress.CAVE_FURNACE,       -1, true,  Terrain.CUSTOM_DECO,       "prop_furnace" ),
		CAMPFIRE(   OverworldDress.CAVE_CAMPFIRE,      -1, true,  Terrain.EMBERS,            "prop_campfire" ),
		BEDROLL(    OverworldDress.CAVE_BEDROLL,       -1, false, Terrain.CUSTOM_DECO_EMPTY, "prop_bedroll" ),
		LOG_SEAT(   OverworldDress.CAVE_LOG_SEAT,      -1, false, Terrain.CUSTOM_DECO_EMPTY, "prop_log" ),
		SARCOPHAGUS( OverworldDress.CAVE_SARCO, OverworldDress.CAVE_SARCO_TOP, true, Terrain.CUSTOM_DECO, "prop_sarcophagus" ),
		MUSH_TEAL(  OverworldDress.CAVE_MUSH_STEM[0], OverworldDress.CAVE_MUSH_CAP[0], true, Terrain.CUSTOM_DECO, "prop_mushroom" ),
		MUSH_VIOLET( OverworldDress.CAVE_MUSH_STEM[1], OverworldDress.CAVE_MUSH_CAP[1], true, Terrain.CUSTOM_DECO, "prop_mushroom" ),
		//a rift's crack: a molten line meeting both edges of its cell on its axis, two versions each
		LAVA_EW_0(  OverworldDress.CAVE_LAVA_EW[0],    -1, false, Terrain.EMBERS,            "prop_rift" ),
		LAVA_EW_1(  OverworldDress.CAVE_LAVA_EW[1],    -1, false, Terrain.EMBERS,            "prop_rift" ),
		LAVA_NS_0(  OverworldDress.CAVE_LAVA_NS[0],    -1, false, Terrain.EMBERS,            "prop_rift" ),
		LAVA_NS_1(  OverworldDress.CAVE_LAVA_NS[1],    -1, false, Terrain.EMBERS,            "prop_rift" ),
		//a crystal cavern's floor, strewn with shards
		SHARDS_0(   OverworldDress.CAVE_SHARDS[0],     -1, false, Terrain.EMPTY,             "prop_shards" ),
		SHARDS_1(   OverworldDress.CAVE_SHARDS[1],     -1, false, Terrain.EMPTY,             "prop_shards" ),
		//a tomb's ring: dressed stone where its face shows, carved with a rune now and then
		TOMB_FACE(  OverworldDress.CAVE_TOMB_FACE,     -1, false, Terrain.WALL,              "tomb_wall" ),
		TOMB_RUNE(  OverworldDress.CAVE_TOMB_RUNE,     -1, false, Terrain.WALL,              "tomb_wall" );

		final int tile, top, on;
		final boolean edges;
		final String name;

		Prop( int tile, int top, boolean edges, int on, String name ){
			this.tile = tile;
			this.top = top;
			this.edges = edges;
			this.on = on;
			this.name = name;
		}
	}

	//who keeps a place
	enum Guard {
		FOSSIL( FossilSkeleton.class ), SKELETON( Skeleton.class ), SENTRY( FungalSentry.class ),
		SPINNER( FungalSpinner.class ), WISP( CrystalWisp.class ), ELEMENTAL( RiftElemental.class ),
		LICH( DwarfLich.class ), GUARDIAN( CrystalGuardian.class );

		final Class<? extends Mob> cls;

		Guard( Class<? extends Mob> cls ){
			this.cls = cls;
		}
	}

	/** One place, as the world has it: immutable, shared by every window and thread. */
	public static final class Site {
		public final Type type;
		public final int altitude, sx, sy;
		//the anchor (the place's heart) and its world key: the found-sites registry's key
		public final int cx, cy;
		public final long key;
		//the reach (world, inclusive): every cell laid or dressed, every guard's post
		public final int x0, y0, x1, y1;
		//where a hero is set down to see it; the trader's, idol's or forge's cell; the cache;
		//the tomb's key cache; a mine's shaft (MIN_VALUE when there is none)
		public final int standX, standY, npcX, npcY, lootX, lootY, keyX, keyY, shaftX, shaftY;
		//the terrain laid, in order (world keys, terrains)
		final long[] cells;
		final int[] terrain;
		final Map<Long, Prop> props;
		//{wx, wy, kind, dy of the prop it stands on} per light; the camp's fire {wx, wy}
		final int[] lights, smoke;
		//{wx, wy, Guard ordinal} per guard post
		final int[] guards;
		//a rift's crack (world keys)
		final long[] fissure;

		private Site( Plan p, Type type, int altitude, int sx, int sy ){
			this.type = type;
			this.altitude = altitude;
			this.sx = sx;
			this.sy = sy;
			cx = p.cx;
			cy = p.cy;
			key = OverworldLevel.worldKey( cx, cy );
			standX = p.standX; standY = p.standY;
			npcX = p.npcX; npcY = p.npcY;
			lootX = p.lootX; lootY = p.lootY;
			keyX = p.keyX; keyY = p.keyY;
			shaftX = p.shaftX; shaftY = p.shaftY;
			cells = new long[p.cells.size()];
			terrain = new int[p.cells.size()];
			int i = 0;
			for (Map.Entry<Long, Integer> e : p.cells.entrySet()){
				cells[i] = e.getKey();
				terrain[i++] = e.getValue();
			}
			props = Collections.unmodifiableMap( new LinkedHashMap<>( p.props ) );
			lights = flat( p.lights );
			smoke = flat( p.smoke );
			guards = flat( p.guards );
			fissure = new long[p.fissure.size()];
			for (i = 0; i < fissure.length; i++) fissure[i] = p.fissure.get( i );
			int[] b = p.box();
			x0 = b[0]; y0 = b[1]; x1 = b[2]; y1 = b[3];
		}

		//the empty sector's sentinel
		private Site(){
			type = null;
			altitude = sx = sy = cx = cy = x0 = y0 = x1 = y1 = 0;
			standX = standY = npcX = npcY = lootX = lootY = keyX = keyY = shaftX = shaftY = 0;
			key = 0;
			cells = new long[0];
			terrain = new int[0];
			props = Collections.emptyMap();
			lights = smoke = guards = new int[0];
			fissure = new long[0];
		}

		public boolean hasShaft(){ return shaftX != Integer.MIN_VALUE; }

		/** Does its reach hold this world cell? */
		public boolean holds( int wx, int wy ){
			return wx >= x0 && wy >= y0 && wx <= x1 && wy <= y1;
		}

		//how far the wildlife is kept (OverworldLevel.populateFauna): the reach and four cells round
		//it, a rift's reach alone (its heat is the beasts' own: CONTRACT fauna HEAT)
		boolean inBerth( int wx, int wy ){
			int m = type == Type.RIFT ? 0 : 4;
			return wx >= x0 - m && wy >= y0 - m && wx <= x1 + m && wy <= y1 + m;
		}

		private static int[] flat( List<int[]> l ){
			int n = l.isEmpty() ? 0 : l.get( 0 ).length;
			int[] out = new int[l.size() * n];
			for (int i = 0; i < l.size(); i++) System.arraycopy( l.get( i ), 0, out, i * n, n );
			return out;
		}
	}

	// ------------------------------------------------------------ the cache

	private static final ConcurrentHashMap<Long, Site> CACHE = new ConcurrentHashMap<>();
	private static final Site NONE = new Site();
	private static volatile long cachedSeed;
	private static volatile boolean seeded = false;

	//a new world forgets the old one's places (a new game, a co-op guest joining another host)
	static synchronized void checkSeed( long seed ){
		if (!seeded || cachedSeed != seed){
			CACHE.clear();
			cachedSeed = seed;
			seeded = true;
		}
	}

	private static long cacheKey( int altitude, int sx, int sy ){
		return ((long)(altitude + 128) << 52) | ((sx & 0x3FFFFFFL) << 26) | (sy & 0x3FFFFFFL);
	}

	/** The place of a sector of a cave slice, or null. Cached; pure. */
	public static Site site( long seed, int altitude, int sx, int sy ){
		if (altitude >= 0 || !WorldLayers.exists( altitude )) return null;
		if (!seeded || cachedSeed != seed) checkSeed( seed );
		long k = cacheKey( altitude, sx, sy );
		Site s = CACHE.get( k );
		if (s == null){
			s = resolve( seed, altitude, sx, sy );
			if (s == null) s = NONE;
			if (CACHE.size() > 6000) CACHE.clear();
			Site won = CACHE.putIfAbsent( k, s );
			if (won != null) s = won;
		}
		return s == NONE ? null : s;
	}

	/** The places whose reach meets a world rect (inclusive) on a slice. Pure; resolves. */
	public static ArrayList<Site> inRect( long seed, int altitude, int x0, int y0, int x1, int y1 ){
		ArrayList<Site> out = new ArrayList<>();
		for (int sy = Math.floorDiv( y0, SECTOR ); sy <= Math.floorDiv( y1, SECTOR ); sy++){
			for (int sx = Math.floorDiv( x0, SECTOR ); sx <= Math.floorDiv( x1, SECTOR ); sx++){
				Site s = site( seed, altitude, sx, sy );
				if (s != null && s.x1 >= x0 && s.y1 >= y0 && s.x0 <= x1 && s.y0 <= y1) out.add( s );
			}
		}
		return out;
	}

	// --------------------------------------------------- which place, where

	//the share (in %) of sectors a place is looked for in: most of those find room for it, so a
	//slice holds a place in about two sectors of five
	private static final int CANDIDATES = 55;

	private static long hash( long seed, int altitude, int sx, int sy, long salt ){
		return WindowGenerator.dressHash( seed ^ 0xCA7E5173L ^ (altitude * 0x9E3779B97F4A7C15L), sx, sy, salt );
	}

	//the sector's own middle the probes look round, jittered
	private static int jitterX( long h0, int sx ){ return sx * SECTOR + 22 + (int) Math.floorMod( h0 >>> 8, 28 ); }
	private static int jitterY( long h0, int sy ){ return sy * SECTOR + 22 + (int) Math.floorMod( h0 >>> 24, 28 ); }

	/** The kind of place a sector would hold, or null: the cheap first answer - one hash and one
	 *  sample - asked before anything is validated. Whether there is room for it is site()'s. */
	static Type typeOf( long seed, int altitude, int sx, int sy ){
		if (altitude >= 0 || !WorldLayers.exists( altitude )) return null;
		long h0 = hash( seed, altitude, sx, sy, 0x5173L );
		if (Math.floorMod( h0, 100 ) >= CANDIDATES) return null;
		int[] w = new int[Type.values().length];
		int total = 0;
		for (Type t : Type.values()){
			if (!t.at( altitude )) continue;
			switch (t){
				case MINE:    w[t.ordinal()] = 3; break;
				//the damp rock grows them twice as often
				case GROTTO:  w[t.ordinal()] = WorldModel.caveDampness( seed, jitterX( h0, sx ), jitterY( h0, sy ), altitude ) < 0.45f ? 4 : 2; break;
				case CRYSTAL: w[t.ordinal()] = altitude <= -7 ? 3 : 2; break;
				default:      w[t.ordinal()] = 2; break;
			}
			total += w[t.ordinal()];
		}
		if (total == 0) return null;
		int pick = (int) Math.floorMod( h0 >>> 40, total );
		for (Type t : Type.values()){
			pick -= w[t.ordinal()];
			if (pick < 0) return t;
		}
		return null;
	}

	//the probe points round the jittered middle, nearest first: a 9x9 grid three cells apart, the
	//inner 5x5 asked first. a probe outside HEART_MIN..HEART_MAX of its sector is skipped, so two
	//neighbouring sectors' hearts stay 32 apart at least and every place keeps inside its own
	private static final int[][] PROBES;
	private static final int HEART_MIN = 16, HEART_MAX = SECTOR - 17;
	static {
		ArrayList<int[]> p = new ArrayList<>();
		for (int dy = -12; dy <= 12; dy += 3) for (int dx = -12; dx <= 12; dx += 3) p.add( new int[]{ dx, dy } );
		Collections.sort( p, ( a, b ) -> {
			int d = (a[0] * a[0] + a[1] * a[1]) - (b[0] * b[0] + b[1] * b[1]);
			if (d != 0) return d;
			return a[1] != b[1] ? a[1] - b[1] : a[0] - b[0];
		} );
		PROBES = p.toArray( new int[0][] );
	}

	private static Site resolve( long seed, int altitude, int sx, int sy ){
		Type type = typeOf( seed, altitude, sx, sy );
		if (type == null) return null;
		long h0 = hash( seed, altitude, sx, sy, 0x5173L );
		int jx = jitterX( h0, sx ), jy = jitterY( h0, sy );
		Nat n = new Nat( seed, altitude, sx, sy );
		for (int[] o : PROBES){
			int cx = jx + o[0], cy = jy + o[1];
			int lx = cx - sx * SECTOR, ly = cy - sy * SECTOR;
			if (lx < HEART_MIN || ly < HEART_MIN || lx > HEART_MAX || ly > HEART_MAX) continue;
			//the one-sample reject first: a probe in the rock is no place's heart
			if (!floorish( n.t( cx, cy ) )) continue;
			Plan p = build( type, n, cx, cy, h0 );
			if (p == null || !p.connected( n ) || !n.dry( p )) continue;
			return new Site( p, type, altitude, sx, sy );
		}
		return null;
	}

	private static Plan build( Type type, Nat n, int cx, int cy, long h0 ){
		switch (type){
			case MINE:    return mine( n, cx, cy );
			case GROTTO:  return grotto( n, cx, cy );
			case CRYSTAL: return crystal( n, cx, cy );
			case CAMP:    return camp( n, cx, cy );
			case SHRINE:  return shrine( n, cx, cy );
			case RIFT:    return rift( n, cx, cy );
			default:      return tomb( n, cx, cy );
		}
	}

	// ------------------------------------------------------- the natural cave

	//what a natural cave cell is, coarsely: the classes validation reads
	private static final byte ROCK = 1, CRYSTAL_ROCK = 2, FLOOR = 3, DECO = 4, MUSH = 5, BOULDER = 6, WATER = 7;

	private static byte classOf( int terrain ){
		switch (terrain){
			case Terrain.WALL:           return ROCK;
			case Terrain.MINE_CRYSTAL:   return CRYSTAL_ROCK;
			case Terrain.EMPTY_DECO:     return DECO;
			case Terrain.MUSHROOM_PATCH: return MUSH;
			case Terrain.BOULDER: case Terrain.MINE_BOULDER: return BOULDER;
			case Terrain.WATER: case Terrain.DEEP_WATER:     return WATER;
			default:                     return FLOOR;
		}
	}

	private static int terrainOf( byte cls ){
		switch (cls){
			case ROCK:         return Terrain.WALL;
			case CRYSTAL_ROCK: return Terrain.MINE_CRYSTAL;
			case DECO:         return Terrain.EMPTY_DECO;
			case MUSH:         return Terrain.MUSHROOM_PATCH;
			case BOULDER:      return Terrain.BOULDER;
			case WATER:        return Terrain.WATER;
			default:           return Terrain.EMPTY;
		}
	}

	//chamber ground a place may stand on (a boulder is cleared off it)
	private static boolean floorish( byte c ){ return c == FLOOR || c == DECO || c == MUSH || c == BOULDER; }
	private static boolean rock( byte c ){ return c == ROCK || c == CRYSTAL_ROCK; }
	//walkable as the cave lies, before anything is laid
	private static boolean walkable( byte c ){ return c == FLOOR || c == DECO || c == MUSH || c == WATER; }

	/**
	 * One resolution's view of the natural cave round a sector: each cell's cave class and
	 * whether it is free of the ways between slices, asked once and kept (a probe reads its
	 * neighbours' cells over and over). Never shared between resolutions or threads.
	 */
	static final class Nat {
		final long seed;
		final int altitude, x0, y0;
		static final int S = SECTOR + 8;
		final byte[] cls = new byte[S * S], free = new byte[S * S];
		final WorldModel.CaveSample cs = new WorldModel.CaveSample();

		Nat( long seed, int altitude, int sx, int sy ){
			this.seed = seed;
			this.altitude = altitude;
			x0 = sx * SECTOR - 4;
			y0 = sy * SECTOR - 4;
		}

		private int idx( int wx, int wy ){
			int x = wx - x0, y = wy - y0;
			return x < 0 || y < 0 || x >= S || y >= S ? -1 : x + y * S;
		}

		//the cave field's own cell: no surface water (dry() asks that, coarsely, at the end)
		byte t( int wx, int wy ){
			int i = idx( wx, wy );
			if (i >= 0 && cls[i] != 0) return cls[i];
			WorldModel.caveSample( seed, wx, wy, altitude, cs );
			byte c = classOf( WorldModel.caveTerrain( seed, wx, wy, altitude, cs ) );
			if (i >= 0) cls[i] = c;
			return c;
		}

		//no way between slices on it, no landing of one from the slice above
		boolean free( int wx, int wy ){
			int i = idx( wx, wy );
			if (i >= 0 && free[i] != 0) return free[i] == 1;
			boolean f = !WindowGenerator.wayAt( seed, altitude, wx, wy )
					&& !WindowGenerator.pitFromAbove( seed, altitude, wx, wy )
					&& !reservedLanding( seed, altitude, wx, wy );
			if (i >= 0) free[i] = (byte)(f ? 1 : 2);
			return f;
		}

		//not under the surface's water: the lakes reach down to -4 (WorldModel.WATER_TIERS) and
		//flood the cave under them, where the overlay lays nothing - a gallery's walls would be lake.
		//every cell the place lays, and every point it is seen, kept or peopled from, must be dry
		boolean dry( Plan p ){
			if (-altitude >= WorldModel.WATER_TIERS) return true;
			for (long k : p.cells.keySet()){
				if (flooded( (int)(k & 0xFFFFFFFFL), (int)(k >> 32) )) return false;
			}
			if (flooded( p.standX, p.standY )) return false;
			if (p.npcX != Integer.MIN_VALUE && flooded( p.npcX, p.npcY )) return false;
			if (p.lootX != Integer.MIN_VALUE && flooded( p.lootX, p.lootY )) return false;
			if (p.keyX != Integer.MIN_VALUE && flooded( p.keyX, p.keyY )) return false;
			for (int[] g : p.guards) if (flooded( g[0], g[1] )) return false;
			return true;
		}

		private boolean flooded( int wx, int wy ){
			return WindowGenerator.surfaceWaterTier( seed, wx, wy ) > -altitude;
		}
	}

	/** Is this cell (within one of it) where a mine of the slice above sinks its shaft? Only mines
	 *  have shafts, and only -1..-6 have mines: anywhere else this is never asked further. */
	static boolean reservedLanding( long seed, int altitude, int wx, int wy ){
		int up = altitude + 1;
		if (!Type.MINE.at( up )) return false;
		int sx = Math.floorDiv( wx, SECTOR ), sy = Math.floorDiv( wy, SECTOR );
		if (typeOf( seed, up, sx, sy ) != Type.MINE) return false;
		Site m = site( seed, up, sx, sy );
		return m != null && m.hasShaft() && Math.abs( wx - m.shaftX ) <= 1 && Math.abs( wy - m.shaftY ) <= 1;
	}

	// ----------------------------------------------------------- the plans

	//a place being laid out: everything it writes, in order, and its points
	static final class Plan {
		final int cx, cy;
		final LinkedHashMap<Long, Integer> cells = new LinkedHashMap<>();
		final LinkedHashMap<Long, Prop> props = new LinkedHashMap<>();
		final ArrayList<int[]> lights = new ArrayList<>(), smoke = new ArrayList<>(), guards = new ArrayList<>();
		final ArrayList<Long> fissure = new ArrayList<>();
		int standX, standY, npcX = Integer.MIN_VALUE, npcY, lootX = Integer.MIN_VALUE, lootY,
				keyX = Integer.MIN_VALUE, keyY, shaftX = Integer.MIN_VALUE, shaftY;
		//a tomb: its walled box is shut on purpose, and the connectivity check leaves it be
		boolean sealedBox;
		//extra reach beyond what is written (a shrine's pool, where its piranha swims)
		int reach = 0;

		Plan( int cx, int cy ){
			this.cx = cx;
			this.cy = cy;
		}

		void set( int x, int y, int t ){ cells.put( OverworldLevel.worldKey( x, y ), t ); }

		void prop( int x, int y, Prop p ){
			set( x, y, p.on );
			props.put( OverworldLevel.worldKey( x, y ), p );
		}

		void light( int x, int y, int kind, int anchorDY ){ lights.add( new int[]{ x, y, kind, anchorDY } ); }

		void guard( int x, int y, Guard g ){ guards.add( new int[]{ x, y, g.ordinal() } ); }

		Integer laid( int x, int y ){ return cells.get( OverworldLevel.worldKey( x, y ) ); }

		int after( Nat n, int x, int y ){
			Integer t = laid( x, y );
			return t != null ? t : terrainOf( n.t( x, y ) );
		}

		//x0, y0, x1, y1: everything laid or dressed (a top on the cell above), lit or posted
		int[] box(){
			int[] b = { cx - reach, cy - reach, cx + reach, cy + reach };
			for (long k : cells.keySet()) grow( b, (int)(k & 0xFFFFFFFFL), (int)(k >> 32) );
			for (Map.Entry<Long, Prop> e : props.entrySet()){
				if (e.getValue().top != -1) grow( b, (int)(e.getKey() & 0xFFFFFFFFL), (int)(e.getKey() >> 32) - 1 );
			}
			for (int[] l : lights) grow( b, l[0], l[1] );
			for (int[] g : guards) grow( b, g[0], g[1] );
			grow( b, standX, standY );
			if (npcX != Integer.MIN_VALUE) grow( b, npcX, npcY );
			if (keyX != Integer.MIN_VALUE) grow( b, keyX, keyY );
			return b;
		}

		private static void grow( int[] b, int x, int y ){
			b[0] = Math.min( b[0], x ); b[1] = Math.min( b[1], y );
			b[2] = Math.max( b[2], x ); b[3] = Math.max( b[3], y );
		}

		/**
		 * The chamber is still one chamber: within the reach (and two round it) every cell the
		 * hero could walk to from the stand cell before the place was laid, and can still stand on,
		 * he can still walk to - no prop, wall or crystal cuts a way off - and the places a hero
		 * must reach (the trader, the cache, the key) are reached. A tomb's own walled box is
		 * shut on purpose and left out.
		 */
		boolean connected( Nat n ){
			int[] b = box();
			int bx = b[0] - 2, by = b[1] - 2, w = b[2] - b[0] + 5, h = b[3] - b[1] + 5;
			boolean[] before = flood( n, bx, by, w, h, false ), now = flood( n, bx, by, w, h, true );
			for (int y = 0; y < h; y++){
				for (int x = 0; x < w; x++){
					int wx = bx + x, wy = by + y;
					if (sealedBox && Math.abs( wx - cx ) <= 3 && Math.abs( wy - cy ) <= 3) continue;
					int i = x + y * w;
					if (before[i] && passable( after( n, wx, wy ) ) && !now[i]) return false;
				}
			}
			if (!now[(standX - bx) + (standY - by) * w]) return false;
			if (npcX != Integer.MIN_VALUE && !reachedBeside( now, bx, by, w, h, npcX, npcY )) return false;
			if (lootX != Integer.MIN_VALUE && !sealedBox && !now[(lootX - bx) + (lootY - by) * w]) return false;
			if (keyX != Integer.MIN_VALUE && !now[(keyX - bx) + (keyY - by) * w]) return false;
			return true;
		}

		//an immovable keeper is reached by standing beside him
		private static boolean reachedBeside( boolean[] r, int bx, int by, int w, int h, int x, int y ){
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int lx = x + dx - bx, ly = y + dy - by;
					if (lx >= 0 && ly >= 0 && lx < w && ly < h && r[lx + ly * w]) return true;
				}
			}
			return false;
		}

		private boolean[] flood( Nat n, int bx, int by, int w, int h, boolean laid ){
			boolean[] seen = new boolean[w * h];
			ArrayDeque<Integer> q = new ArrayDeque<>();
			int s = (standX - bx) + (standY - by) * w;
			seen[s] = true;
			q.add( s );
			while (!q.isEmpty()){
				int c = q.poll();
				int x = c % w, y = c / w;
				for (int[] d : DIRS4){
					int nx = x + d[0], ny = y + d[1];
					if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
					int i = nx + ny * w;
					if (seen[i]) continue;
					int wx = bx + nx, wy = by + ny;
					boolean ok = laid ? passable( after( n, wx, wy ) ) : walkable( n.t( wx, wy ) );
					if (!ok) continue;
					seen[i] = true;
					q.add( i );
				}
			}
			return seen;
		}
	}

	private static boolean passable( int terrain ){
		return (Terrain.flags[terrain] & Terrain.PASSABLE) != 0;
	}

	private static final int[][] DIRS4 = { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };

	//(dx, dy) turned a quarter clockwise r times
	private static int[] rot( int r, int dx, int dy ){
		for (int i = 0; i < Math.floorMod( r, 4 ); i++){
			int t = dx;
			dx = -dy;
			dy = t;
		}
		return new int[]{ dx, dy };
	}

	private static long cellHash( Nat n, int x, int y, long salt ){
		return WindowGenerator.dressHash( n.seed ^ (n.altitude * 0x9E37L), x, y, salt );
	}

	//every cell of a disc (Euclid <= r) is chamber ground; and at least `share` of a wider disc
	private static boolean ground( Nat n, int cx, int cy, double r ){
		int R = (int) Math.ceil( r );
		for (int dy = -R; dy <= R; dy++){
			for (int dx = -R; dx <= R; dx++){
				if (dx * dx + dy * dy <= r * r && !floorish( n.t( cx + dx, cy + dy ) )) return false;
			}
		}
		return true;
	}

	private static boolean square( Nat n, int cx, int cy, int r, boolean free ){
		for (int dy = -r; dy <= r; dy++){
			for (int dx = -r; dx <= r; dx++){
				if (free ? !n.free( cx + dx, cy + dy ) : !floorish( n.t( cx + dx, cy + dy ) )) return false;
			}
		}
		return true;
	}

	private static boolean freeDisc( Nat n, int cx, int cy, double r ){
		int R = (int) Math.ceil( r );
		for (int dy = -R; dy <= R; dy++){
			for (int dx = -R; dx <= R; dx++){
				if (dx * dx + dy * dy <= r * r && !n.free( cx + dx, cy + dy )) return false;
			}
		}
		return true;
	}

	private static float share( Nat n, int cx, int cy, double r, boolean wet ){
		int R = (int) Math.ceil( r ), all = 0, ok = 0;
		for (int dy = -R; dy <= R; dy++){
			for (int dx = -R; dx <= R; dx++){
				if (dx * dx + dy * dy > r * r) continue;
				all++;
				byte c = n.t( cx + dx, cy + dy );
				if (floorish( c ) || (wet && c == WATER)) ok++;
			}
		}
		return ok / (float) all;
	}

	//boulders on the ground a place stands on are cleared away (never on a way or a landing)
	private static void clearBoulders( Plan p, Nat n, int cx, int cy, int r ){
		for (int dy = -r; dy <= r; dy++){
			for (int dx = -r; dx <= r; dx++){
				if (n.t( cx + dx, cy + dy ) == BOULDER && p.laid( cx + dx, cy + dy ) == null && n.free( cx + dx, cy + dy )){
					p.set( cx + dx, cy + dy, Terrain.EMPTY_DECO );
				}
			}
		}
	}

	//inside the sector, a cell clear of its edge: no place reaches its neighbour's
	private static boolean inSector( Nat n, int x, int y ){
		int lx = x - n.x0 - 4, ly = y - n.y0 - 4;
		return lx >= 1 && ly >= 1 && lx <= SECTOR - 2 && ly <= SECTOR - 2;
	}

	/**
	 * An old mine (-1..-6): a yard on chamber floor with a cart, a rack of tools and a lantern,
	 * and a gallery three wide carved straight into the rock from it, nine to fifteen long, rails
	 * down its middle, timber props along its wall, a second lantern still burning halfway; at its
	 * end a shaft down to the slice below, when the rock there opens onto a chamber. The gallery
	 * is the one carving of rock any place does (a deviation from "open chamber cells only" the
	 * developer approved with the plan).
	 */
	private static Plan mine( Nat n, int cx, int cy ){
		if (!square( n, cx, cy, 3, false ) || !square( n, cx, cy, 4, true )) return null;
		int r0 = (int) Math.floorMod( hash( n.seed, n.altitude, Math.floorDiv( cx, SECTOR ), Math.floorDiv( cy, SECTOR ), 0xD12L ), 4 );
		for (int i = 0; i < 4; i++){
			int[] d = DIRS4[(r0 + i) % 4];
			int px = -d[1], py = d[0];
			int kEnd = 2, rock = 0;
			for (int k = 3; k <= 15; k++){
				int lx = cx + k * d[0], ly = cy + k * d[1];
				boolean ok = inSector( n, lx + 2 * px, ly + 2 * py ) && inSector( n, lx - 2 * px, ly - 2 * py )
						&& inSector( n, lx + d[0], ly + d[1] ) && n.free( lx + d[0], ly + d[1] );
				for (int j = -2; j <= 2 && ok; j++){
					if (!n.free( lx + j * px, ly + j * py )) ok = false;
					else if (Math.abs( j ) <= 1 && n.t( lx + j * px, ly + j * py ) == WATER) ok = false;
				}
				if (!ok) break;
				kEnd = k;
				if (rock( n.t( lx, ly ) )) rock++;
			}
			//a gallery is cut into the rock: half its length at least, or it is a track over a floor
			if (kEnd >= 9 && rock >= (kEnd - 2) / 2) return mineAlong( n, cx, cy, d, kEnd );
		}
		return null;
	}

	private static Plan mineAlong( Nat n, int cx, int cy, int[] d, int kEnd ){
		Plan p = new Plan( cx, cy );
		int px = -d[1], py = d[0];
		boolean across = d[1] == 0;
		//the side whose cell above is not the gallery's own lane: a prop's top over the rails would
		//read as a thing standing in the way. east-west galleries prop their north wall only
		int[] wall = across ? new int[]{ 0, -1 } : new int[]{ px, py };
		//the yard's own "aside": south of an east-west gallery, else its side
		int qx = across ? 0 : px, qy = across ? 1 : py;
		for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) p.set( cx + dx, cy + dy, Terrain.EMPTY );
		clearBoulders( p, n, cx, cy, 3 );
		for (int k = 3; k <= kEnd; k++){
			for (int j = -1; j <= 1; j++) p.set( cx + k * d[0] + j * px, cy + k * d[1] + j * py, Terrain.EMPTY );
		}
		//a timber holds up a roof: it stands only where plain rock lies beyond it (above an east-west
		//gallery's north post, where its cap beam is drawn; beside a north-south one's), moved a cell
		//along the gallery to find it, never beside the lantern, and left out where there is none
		for (int want : new int[]{ 5, 9, 13 }){
			for (int side : across ? new int[]{ 1 } : new int[]{ 1, -1 }){
				int wx = across ? wall[0] : side * px, wy = across ? wall[1] : side * py;
				for (int k : new int[]{ want, want + 1, want - 1 }){
					if (k < 4 || k >= kEnd || Math.abs( k - 7 ) <= 1) continue;
					int lx = cx + k * d[0], ly = cy + k * d[1];
					int bx = lx + 2 * wx, by = ly + 2 * wy;
					if (n.t( bx, by ) != ROCK || !n.free( bx, by )) continue;
					p.prop( lx + wx, ly + wy, Prop.TIMBER );
					break;
				}
			}
		}
		int lx7 = cx + 7 * d[0] + wall[0], ly7 = cy + 7 * d[1] + wall[1];
		p.prop( lx7, ly7, Prop.LANTERN );
		p.light( lx7, ly7, SettlementLights.KIND_LANTERN, 0 );
		p.prop( cx + 2 * qx, cy + 2 * qy, Prop.CART );
		p.prop( cx - 2 * qx - d[0], cy - 2 * qy - d[1], Prop.LANTERN );
		p.light( cx - 2 * qx - d[0], cy - 2 * qy - d[1], SettlementLights.KIND_LANTERN, 0 );
		p.prop( cx - 2 * d[0], cy - 2 * d[1], Prop.TOOL_RACK );

		//the shaft: down to the slice below, where its bottom opens onto a chamber
		int sx = cx + kEnd * d[0], sy = cy + kEnd * d[1];
		boolean shaft = WorldLayers.exists( n.altitude - 1 ) && shaftLands( n, sx, sy );
		if (shaft){
			p.set( sx, sy, Terrain.EXIT );
			p.shaftX = sx;
			p.shaftY = sy;
		}
		Prop rails = across ? Prop.RAILS_EW : Prop.RAILS_NS;
		//the rails run into the shaft, or end at a buffer at the gallery's face
		int last = shaft ? kEnd - 1 : kEnd;
		for (int k = -1; k <= last; k++){
			Prop r = rails;
			if (k == -1) r = endTowards( -d[0], -d[1] );
			else if (k == last && !shaft) r = endTowards( d[0], d[1] );
			p.prop( cx + k * d[0], cy + k * d[1], r );
		}
		p.standX = cx - d[0];
		p.standY = cy - d[1];
		p.lootX = cx;
		p.lootY = cy;
		p.guard( cx + d[0] + qx, cy + d[1] + qy, Guard.FOSSIL );
		p.guard( cx + 3 * d[0], cy + 3 * d[1], Guard.SKELETON );
		if (n.altitude <= -4) p.guard( cx + 6 * d[0] - wall[0], cy + 6 * d[1] - wall[1], Guard.FOSSIL );
		return p;
	}

	private static Prop endTowards( int dx, int dy ){
		if (dx > 0) return Prop.RAIL_END_E;
		if (dx < 0) return Prop.RAIL_END_W;
		return dy > 0 ? Prop.RAIL_END_S : Prop.RAIL_END_N;
	}

	/**
	 * A mine's shaft bottom on the slice below: dry, clear of that slice's own ways, and opening
	 * onto a real chamber - forty open cells at least within ten of it - never into a sealed
	 * pocket of rock.
	 */
	private static boolean shaftLands( Nat above, int x, int y ){
		long seed = above.seed;
		int alt = above.altitude - 1;
		boolean open = false;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if (WindowGenerator.wayAt( seed, alt, x + dx, y + dy )) return false;
				if (WorldModel.caveWet( seed, x + dx, y + dy, alt )) return false;
				if (-alt < WorldModel.WATER_TIERS && WindowGenerator.surfaceWaterTier( seed, x + dx, y + dy ) > -alt) return false;
				open |= WorldModel.caveOpen( seed, x + dx, y + dy, alt );
			}
		}
		if (!open) return false;
		final int R = 10, W = 2 * R + 1;
		byte[] seen = new byte[W * W];   //0 unasked, 1 open, 2 rock
		ArrayDeque<Integer> q = new ArrayDeque<>();
		int reached = 0;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				int i = (dx + R) + (dy + R) * W;
				seen[i] = 1;
				q.add( i );
			}
		}
		while (!q.isEmpty() && reached < 40){
			int c = q.poll();
			reached++;
			int lx = c % W, ly = c / W;
			for (int[] d : DIRS4){
				int nx = lx + d[0], ny = ly + d[1];
				if (nx < 0 || ny < 0 || nx >= W || ny >= W) continue;
				int i = nx + ny * W;
				if (seen[i] != 0) continue;
				boolean o = WorldModel.caveOpen( seed, x + nx - R, y + ny - R, alt );
				seen[i] = (byte)(o ? 1 : 2);
				if (o) q.add( i );
			}
		}
		return reached >= 40;
	}

	/**
	 * A mushroom grotto (any slice): a chamber carpeted in mushrooms round a heart of bare floor
	 * where the cache lies, up to four giant mushrooms glowing among them, their spinners and,
	 * from -3 down, a sentry.
	 */
	private static Plan grotto( Nat n, int cx, int cy ){
		if (!square( n, cx, cy, 1, false )) return null;
		if (share( n, cx, cy, 5.5, true ) < 0.70f) return null;
		if (!freeDisc( n, cx, cy, 6.5 )) return null;
		int r = (int) Math.floorMod( cellHash( n, cx, cy, 0x2077L ), 4 );
		Plan p = new Plan( cx, cy );
		for (int dy = -6; dy <= 6; dy++){
			for (int dx = -6; dx <= 6; dx++){
				if (dx * dx + dy * dy > 5.5 * 5.5) continue;
				int x = cx + dx, y = cy + dy;
				if (!floorish( n.t( x, y ) )) continue;
				p.set( x, y, Math.floorMod( cellHash( n, x, y, 0x6E0L ), 5 ) == 0 ? Terrain.EMPTY_DECO : Terrain.MUSHROOM_PATCH );
			}
		}
		p.set( cx, cy, Terrain.EMPTY_DECO );
		ArrayList<int[]> giants = new ArrayList<>();
		for (int[] o : new int[][]{ { -3, -2 }, { 3, -1 }, { -1, 3 }, { 2, 3 }, { 0, -4 } }){
			if (giants.size() >= 4) break;
			int[] ro = rot( r, o[0], o[1] );
			int x = cx + ro[0], y = cy + ro[1];
			boolean ok = floorish( n.t( x, y ) ) && floorish( n.t( x, y - 1 ) );
			for (int[] g : giants) ok &= Math.max( Math.abs( g[0] - x ), Math.abs( g[1] - y ) ) > 1;
			for (int dy = -1; dy <= 1 && ok; dy++) for (int dx = -1; dx <= 1 && ok; dx++) ok = floorish( n.t( x + dx, y + dy ) );
			if (!ok) continue;
			giants.add( new int[]{ x, y } );
			boolean teal = (cellHash( n, x, y, 0x5B0L ) & 1) == 0;
			p.prop( x, y, teal ? Prop.MUSH_TEAL : Prop.MUSH_VIOLET );
			//the spores glow on the cap, the cell above the stem, in the cap's own colour
			p.light( x, y - 1, teal ? SettlementLights.KIND_SPORES : SettlementLights.KIND_SPORES_VIOLET, 1 );
		}
		int[] st = rot( r, 0, 4 );
		boolean far = floorish( n.t( cx + st[0], cy + st[1] ) ) && p.props.get( OverworldLevel.worldKey( cx + st[0], cy + st[1] ) ) == null;
		if (!far) st = rot( r, 0, 3 );
		p.standX = cx + st[0];
		p.standY = cy + st[1];
		p.lootX = cx;
		p.lootY = cy;
		if (n.altitude <= -3 && !giants.isEmpty()){
			int[] g = giants.get( 0 );
			p.guard( g[0], g[1] + 1, Guard.SENTRY );
		}
		int[] a = rot( r, 2, 0 );
		p.guard( cx + a[0], cy + a[1], Guard.SPINNER );
		if (n.altitude <= -5) p.guard( cx - a[0], cy - a[1], Guard.SPINNER );
		return p;
	}

	/**
	 * A crystal cavern (-3..-12): a chamber ringed with crystal - the rock round it grown thick
	 * with seams, the gem pockets of Ores - and studded with crystal outcrops on its floor; the
	 * cache of gems at its heart, kept by wisps.
	 */
	private static Plan crystal( Nat n, int cx, int cy ){
		if (!square( n, cx, cy, 2, false )) return null;
		if (share( n, cx, cy, 4.5, false ) < 0.75f) return null;
		//a cavern: rock close round it to grow the crystal on, not the middle of a great hall
		int walls = 0;
		for (int dy = -8; dy <= 8; dy++){
			for (int dx = -8; dx <= 8; dx++){
				double d2 = dx * dx + dy * dy;
				if (d2 <= 4.5 * 4.5 || d2 > 8.5 * 8.5 || n.t( cx + dx, cy + dy ) != ROCK) continue;
				for (int[] o : N8){
					if (floorish( n.t( cx + dx + o[0], cy + dy + o[1] ) )){
						walls++;
						break;
					}
				}
			}
		}
		if (walls < 18) return null;
		//its heart clear of the ways; the crystal of its ring grows only where no way is beside it
		if (!freeDisc( n, cx, cy, 4.5 )) return null;
		int r = (int) Math.floorMod( cellHash( n, cx, cy, 0x2077L ), 4 );
		Plan p = new Plan( cx, cy );
		for (int dy = -9; dy <= 9; dy++){
			for (int dx = -9; dx <= 9; dx++){
				double d2 = dx * dx + dy * dy;
				int x = cx + dx, y = cy + dy;
				byte c = n.t( x, y );
				if (d2 <= 3.5 * 3.5){
					if (floorish( c )) p.set( x, y, Terrain.EMPTY );
				} else if (d2 <= 8.5 * 8.5 && c == ROCK && clearOfWays( n, x, y )
						&& Math.floorMod( cellHash( n, x, y, 0xC4157L ), 3 ) != 0){
					boolean faces = false;
					for (int[] o : N8) faces |= walkable( n.t( x + o[0], y + o[1] ) ) || n.t( x + o[0], y + o[1] ) == BOULDER;
					if (faces) p.set( x, y, Terrain.MINE_CRYSTAL );
				}
			}
		}
		clearBoulders( p, n, cx, cy, 4 );
		ArrayList<int[]> studs = new ArrayList<>();
		for (int[] o : new int[][]{ { -2, -2 }, { 2, -2 }, { 0, 3 }, { -3, 1 }, { 3, 2 } }){
			int[] ro = rot( r, o[0], o[1] );
			int x = cx + ro[0], y = cy + ro[1];
			boolean ok = floorish( n.t( x, y ) ) && clearOfWays( n, x, y );
			for (int[] s : studs) ok &= Math.max( Math.abs( s[0] - x ), Math.abs( s[1] - y ) ) > 1;
			for (int dy = -1; dy <= 1 && ok; dy++) for (int dx = -1; dx <= 1 && ok; dx++) ok = floorish( n.t( x + dx, y + dy ) );
			if (!ok) continue;
			studs.add( new int[]{ x, y } );
			p.set( x, y, Terrain.MINE_CRYSTAL );
		}
		int[] st = rot( r, 0, 3 );
		if (p.laid( cx + st[0], cy + st[1] ) != null && p.laid( cx + st[0], cy + st[1] ) == Terrain.MINE_CRYSTAL) st = rot( r, 0, 2 );
		p.standX = cx + st[0];
		p.standY = cy + st[1];
		p.lootX = cx;
		p.lootY = cy;
		//what only a cavern has: its floor strewn with shards fallen off the crystal, glittering
		//cold enough to be seen from across the dark (three of them lit, those nearest its heart)
		ArrayList<int[]> shards = new ArrayList<>();
		for (int dy = -3; dy <= 3; dy++){
			for (int dx = -3; dx <= 3; dx++){
				int x = cx + dx, y = cy + dy;
				Integer t = p.laid( x, y );
				if (t == null || t != Terrain.EMPTY || (x == cx && y == cy) || (x == p.standX && y == p.standY)) continue;
				if (dx * dx + dy * dy > 3.5 * 3.5 || Math.floorMod( cellHash( n, x, y, 0x5A4DL ), 5 ) >= 2) continue;
				p.prop( x, y, (cellHash( n, x, y, 0x5A4EL ) & 1) == 0 ? Prop.SHARDS_0 : Prop.SHARDS_1 );
				shards.add( new int[]{ x, y, dx * dx + dy * dy } );
			}
		}
		Collections.sort( shards, ( a, b ) -> a[2] != b[2] ? a[2] - b[2] : a[1] != b[1] ? a[1] - b[1] : a[0] - b[0] );
		for (int i = 0; i < Math.min( 3, shards.size() ); i++){
			p.light( shards.get( i )[0], shards.get( i )[1], SettlementLights.KIND_SHARDS, 0 );
		}
		int wisps = n.altitude <= -9 ? 3 : n.altitude <= -6 ? 2 : 1;
		int[][] posts = { rot( r, 2, 1 ), rot( r, -2, 1 ), rot( r, 0, -2 ) };
		for (int i = 0; i < wisps; i++) p.guard( cx + posts[i][0], cy + posts[i][1], Guard.WISP );
		//deep down, a guardian of living crystal sleeps by the cache
		if (n.altitude <= GUARDIAN_DEPTH){
			int[] g = rot( r, 1, 1 );
			p.guard( cx + g[0], cy + g[1], Guard.GUARDIAN );
		}
		return p;
	}

	//from this slice down a crystal cavern's cache is kept by a crystal guardian too
	static final int GUARDIAN_DEPTH = -8;

	//a cell nothing solid may be laid on: a way or a landing on it or beside it
	private static boolean clearOfWays( Nat n, int x, int y ){
		if (!n.free( x, y )) return false;
		for (int[] d : DIRS4) if (!n.free( x + d[0], y + d[1] )) return false;
		return true;
	}

	//the eight neighbours as offsets: never PathFinder's, which are the live level's width
	private static final int[][] N8 = { { -1, -1 }, { 0, -1 }, { 1, -1 }, { -1, 0 }, { 1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 } };

	/**
	 * A miners' camp (-2..-8): a fire on bare ground with bedrolls and a seat round it, stores and
	 * two lanterns at its edges, and a dwarf miner who trades there. The fire's smoke rises north
	 * from its flames, so nothing anyone stands at is north of it: the camp is only ever mirrored,
	 * never turned.
	 */
	private static Plan camp( Nat n, int cx, int cy ){
		if (!square( n, cx, cy, 3, false ) || !square( n, cx, cy, 4, true )) return null;
		int m = (cellHash( n, cx, cy, 0x2077L ) & 1) == 0 ? 1 : -1;
		Plan p = new Plan( cx, cy );
		for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) p.set( cx + dx, cy + dy, Terrain.EMPTY );
		clearBoulders( p, n, cx, cy, 4 );
		p.prop( cx, cy, Prop.CAMPFIRE );
		p.light( cx, cy, SettlementLights.KIND_CAMPFIRE, 0 );
		p.smoke.add( new int[]{ cx, cy } );
		p.prop( cx - m, cy + 2, Prop.BEDROLL );
		p.prop( cx + m, cy + 2, Prop.BEDROLL );
		p.prop( cx + 2 * m, cy, Prop.LOG_SEAT );
		p.prop( cx - 3 * m, cy + 2, Prop.CRATES );
		p.prop( cx + 3 * m, cy + 2, Prop.BARREL );
		p.prop( cx - m, cy - 3, Prop.SACKS );
		p.prop( cx - 3 * m, cy - 2, Prop.LANTERN );
		p.light( cx - 3 * m, cy - 2, SettlementLights.KIND_LANTERN, 0 );
		p.prop( cx + 3 * m, cy - 2, Prop.LANTERN );
		p.light( cx + 3 * m, cy - 2, SettlementLights.KIND_LANTERN, 0 );
		p.npcX = cx - 2 * m;
		p.npcY = cy;
		p.standX = cx;
		p.standY = cy + 3;
		return p;
	}

	/**
	 * A lake shrine (-2..-10): a small idol on dry ground at the edge of a still pool - eight
	 * cells of water at least within four of it - where miners toss coins for luck; something
	 * pale lives in the deep water.
	 */
	private static Plan shrine( Nat n, int cx, int cy ){
		if (!square( n, cx, cy, 1, false ) || !square( n, cx, cy, 3, true )) return null;
		int wet = 0, near = 99;
		for (int dy = -5; dy <= 5; dy++){
			for (int dx = -5; dx <= 5; dx++){
				if (n.t( cx + dx, cy + dy ) != WATER) continue;
				if (dx * dx + dy * dy <= 4.5 * 4.5) wet++;
				near = Math.min( near, Math.max( Math.abs( dx ), Math.abs( dy ) ) );
			}
		}
		if (wet < 8 || near > 2) return null;
		//and a deep part, open water all round, where the pale thing of its tale lives
		if (!deepPool( n, cx, cy )) return null;
		Plan p = new Plan( cx, cy );
		p.set( cx, cy, Terrain.EMPTY );
		int best = -1, bestDry = -1;
		for (int i = 0; i < 8; i++){
			int[] o = N8[i];
			int x = cx + o[0], y = cy + o[1];
			p.set( x, y, Terrain.EMPTY );
			//the side farthest from the water: where a hero stands to look at it
			int dry = 99;
			for (int dy = -4; dy <= 4; dy++){
				for (int dx = -4; dx <= 4; dx++){
					if (n.t( x + dx, y + dy ) == WATER) dry = Math.min( dry, Math.max( Math.abs( dx ), Math.abs( dy ) ) );
				}
			}
			if (dry > bestDry){
				bestDry = dry;
				best = i;
			}
		}
		p.standX = cx + N8[best][0];
		p.standY = cy + N8[best][1];
		p.npcX = cx;
		p.npcY = cy;
		p.reach = 5;
		return p;
	}

	//a water cell within six of the idol with water all round it (deepWater's own rule, on the
	//cave as it lies)
	private static boolean deepPool( Nat n, int cx, int cy ){
		for (int y = cy - 6; y <= cy + 6; y++){
			for (int x = cx - 6; x <= cx + 6; x++){
				if (n.t( x, y ) != WATER) continue;
				boolean open = true;
				for (int[] o : N8) open &= n.t( x + o[0], y + o[1] ) == WATER;
				if (open) return true;
			}
		}
		return false;
	}

	/**
	 * A burning rift (-5..-12): a crack of molten rock straight across a chamber from one wall to
	 * the other, glowing and hot, scorched wide at its heart, flaring now and then (tick); on one
	 * side of it an ancient dwarven forge - a furnace built into the rift's heat, a rack of tools
	 * and the anvil - and on the other the fire elementals it feeds. The crack runs east-west or
	 * north-south only, so its cells join into one line (CaveSites' lava tiles meet edge to edge).
	 * Nothing that burns is left within its reach: no Fire is ever seeded there.
	 */
	private static Plan rift( Nat n, int cx, int cy ){
		if (!square( n, cx, cy, 2, false )) return null;
		if (share( n, cx, cy, 7.5, false ) < 0.70f) return null;
		if (!freeDisc( n, cx, cy, 3 )) return null;
		long h = cellHash( n, cx, cy, 0x21F7L );
		int first = (int) Math.floorMod( h, 2 );
		for (int i = 0; i < 2; i++){
			Plan p = riftAlong( n, cx, cy, (first + i) % 2 == 0 ? new int[]{ 1, 0 } : new int[]{ 0, 1 }, h );
			if (p != null) return p;
		}
		return null;
	}

	//how far a rift's crack may run each way from its heart to the rock that ends it
	private static final int RIFT_RUN = 13;

	private static Plan riftAlong( Nat n, int cx, int cy, int[] a, long h ){
		//the crack: from the heart both ways until it runs under the rock, over open ground alone
		int[] ends = new int[2];
		for (int e = 0; e < 2; e++){
			int sgn = e == 0 ? 1 : -1, k = 1;
			for (;; k++){
				if (k > RIFT_RUN) return null;
				int x = cx + sgn * k * a[0], y = cy + sgn * k * a[1];
				byte c = n.t( x, y );
				if (rock( c )) break;
				if (!floorish( c ) || !n.free( x, y )) return null;
			}
			if (k - 1 < 4) return null;
			ends[e] = k - 1;
		}
		int nx = -a[1], ny = a[0];
		HashSet<Long> crack = new HashSet<>(), scorch = new HashSet<>();
		for (int k = -ends[1]; k <= ends[0]; k++){
			crack.add( OverworldLevel.worldKey( cx + k * a[0], cy + k * a[1] ) );
			//its heart is wide: the ground either side of it scorched to embers
			if (Math.abs( k ) > 2) continue;
			for (int sd = -1; sd <= 1; sd += 2){
				int x = cx + k * a[0] + sd * nx, y = cy + k * a[1] + sd * ny;
				if (floorish( n.t( x, y ) ) && n.free( x, y )) scorch.add( OverworldLevel.worldKey( x, y ) );
			}
		}
		int step = 3;
		int fx = 0, fy = 0, side = ((h >>> 4) & 1) == 0 ? 1 : -1;
		boolean found = false;
		for (int tries = 0; tries < 2 && !found; tries++, side = -side){
			fx = cx + side * nx * step;
			fy = cy + side * ny * step;
			found = true;
			for (int j = -1; j <= 1 && found; j++){
				int x = fx + j * a[0], y = fy + j * a[1];
				found = clearOfWays( n, x, y );
				for (int dy = -1; dy <= 1 && found; dy++){
					for (int dx = -1; dx <= 1 && found; dx++){
						long k = OverworldLevel.worldKey( x + dx, y + dy );
						found = floorish( n.t( x + dx, y + dy ) ) && !crack.contains( k ) && !scorch.contains( k );
					}
				}
			}
			if (found) break;
		}
		if (!found) return null;
		Plan p = new Plan( cx, cy );
		//nothing that burns within reach of the crack: the floor is bared (a mushroom patch would
		//only catch from a fire elemental's touch) round its heart and two cells either side of it
		//all its length; a way's own cell keeps its ground, so a mushroom there is no rift's
		for (int dy = -RIFT_RUN - 2; dy <= RIFT_RUN + 2; dy++){
			for (int dx = -RIFT_RUN - 2; dx <= RIFT_RUN + 2; dx++){
				int along = dx * a[0] + dy * a[1], across = Math.abs( dx * nx + dy * ny );
				boolean near = dx * dx + dy * dy <= 8.5 * 8.5 || (across <= 2 && along >= -ends[1] - 1 && along <= ends[0] + 1);
				if (!near) continue;
				int x = cx + dx, y = cy + dy;
				byte c = n.t( x, y );
				if (!floorish( c )) continue;
				if (!n.free( x, y )){
					if (c == MUSH) return null;
					continue;
				}
				p.set( x, y, Math.floorMod( cellHash( n, x, y, 0xA5EL ), 4 ) == 0 ? Terrain.EMPTY_DECO : Terrain.EMPTY );
			}
		}
		boolean ew = a[0] != 0;
		for (int k = -ends[1]; k <= ends[0]; k++){
			int x = cx + k * a[0], y = cy + k * a[1];
			boolean alt = (cellHash( n, x, y, 0x1A7AL ) & 1) == 0;
			p.prop( x, y, ew ? (alt ? Prop.LAVA_EW_0 : Prop.LAVA_EW_1) : (alt ? Prop.LAVA_NS_0 : Prop.LAVA_NS_1) );
			p.fissure.add( OverworldLevel.worldKey( x, y ) );
		}
		for (long k : scorch){
			p.set( (int)(k & 0xFFFFFFFFL), (int)(k >> 32), Terrain.EMBERS );
			p.fissure.add( k );
		}
		p.set( fx, fy, Terrain.EMPTY );
		p.prop( fx + a[0], fy + a[1], Prop.FURNACE );
		p.light( fx + a[0], fy + a[1], SettlementLights.KIND_FURNACE, 0 );
		p.prop( fx - a[0], fy - a[1], Prop.TOOL_RACK );
		p.npcX = fx;
		p.npcY = fy;
		p.standX = fx + side * nx;
		p.standY = fy + side * ny;
		int gx = cx - side * nx * 3, gy = cy - side * ny * 3;
		p.guard( gx + a[0], gy + a[1], Guard.ELEMENTAL );
		p.guard( gx - a[0], gy - a[1], Guard.ELEMENTAL );
		if (n.altitude <= -9) p.guard( gx, gy, Guard.ELEMENTAL );
		return p;
	}

	/**
	 * A sealed dwarven tomb (-8..-12): a walled room of dressed stone shut by an iron door that
	 * faces south (the side the camera sees), a stone coffin inside and the chest it was buried
	 * with, its lich; behind it, the iron key in a dead dwarf's bones, two fossils over them. No
	 * way between slices comes down anywhere near it (free(): no pit of the slice above, no ladder,
	 * no shaft lands within six of it); its walls refuse the pick and the bombs (OverworldLevel
	 * .unbreakable), and the slice above refuses to dig down onto it (OverworldLevel.sealedBelow).
	 */
	private static Plan tomb( Nat n, int cx, int cy ){
		//its room on chamber floor; its walls may stand on floor or be cut into the rock, never in
		//water (the overlay would leave a hole in them)
		if (!square( n, cx, cy, 2, false ) || !square( n, cx, cy, 6, true )) return null;
		for (int d = -3; d <= 3; d++){
			for (int[] c : new int[][]{ { d, -3 }, { d, 3 }, { -3, d }, { 3, d } }){
				if (n.t( cx + c[0], cy + c[1] ) == WATER) return null;
			}
		}
		//the way to the door
		for (int[] o : new int[][]{ { 0, 4 }, { 0, 5 } }){
			if (!floorish( n.t( cx + o[0], cy + o[1] ) )) return null;
		}
		Plan p = new Plan( cx, cy );
		p.sealedBox = true;
		for (int dy = -3; dy <= 3; dy++){
			for (int dx = -3; dx <= 3; dx++){
				int x = cx + dx, y = cy + dy;
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) == 3){
					if (dx == 0 && dy == 3){
						p.set( x, y, Terrain.LOCKED_DOOR );
					} else {
						p.prop( x, y, (dy == -3 && dx == 0) || (dy == 3 && Math.abs( dx ) == 2) ? Prop.TOMB_RUNE : Prop.TOMB_FACE );
					}
				} else {
					p.set( x, y, Terrain.EMPTY_SP );
				}
			}
		}
		clearBoulders( p, n, cx, cy, 5 );
		for (int[] o : new int[][]{ { 0, 4 }, { 0, 5 } }){
			p.set( cx + o[0], cy + o[1], Terrain.EMPTY );
		}
		p.prop( cx, cy - 1, Prop.SARCOPHAGUS );
		p.standX = cx;
		p.standY = cy + 5;
		p.lootX = cx;
		p.lootY = cy + 1;
		p.guard( cx + 2, cy - 1, Guard.LICH );
		//a tomb nobody could open is no tomb
		return hideKey( p, n, cx, cy ) ? p : null;
	}

	//the tomb's key is not left at its door: it lies in the bones of whoever carried it, in a
	//pocket of open cave somewhere near - any open 5x5 of floor 8 to 14 cells off the tomb's
	//middle (clear of its walls and its approach), the tomb's own hash picking which, the first
	//of them a hero can walk to from the door, inside the tomb's own sector like everything of a
	//place. Two fossil guards keep it. A tomb with no such pocket keeps it just behind its back
	//wall, as it always did; with room for neither, there is no tomb there
	private static final int KEY_NEAR = 8, KEY_FAR = 14, KEY_WALKS = 4;
	private static final long KEY_SALT = 0x70B4E7L;

	private static boolean hideKey( Plan p, Nat n, int cx, int cy ){
		//the spots in the tomb's own order first (no terrain read), then each looked at in turn
		//until one is an open pocket that can be walked to: most tombs look at a handful
		ArrayList<int[]> spots = new ArrayList<>();
		for (int dy = -KEY_FAR; dy <= KEY_FAR; dy++){
			for (int dx = -KEY_FAR; dx <= KEY_FAR; dx++){
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) < KEY_NEAR) continue;
				int px = cx + dx, py = cy + dy;
				if (!inSector( n, px - 2, py - 2 ) || !inSector( n, px + 2, py + 2 )) continue;
				spots.add( new int[]{ px, py, (int) (hash( n.seed, n.altitude, px, py, KEY_SALT ) >>> 33) } );
			}
		}
		spots.sort( ( a, b ) -> Integer.compare( a[2], b[2] ) );
		//each spot's 25 cells read only as far as its first that is not open floor, and only the
		//first few open pockets walked to from the door: reading the whole search area of the cave
		//field doubled the cost of resolving a deep slice's places
		int walks = 0;
		for (int[] k : spots){
			if (!square( n, k[0], k[1], 2, false ) || !square( n, k[0], k[1], 2, true )) continue;
			if (walks++ >= KEY_WALKS) break;
			p.keyX = k[0];
			p.keyY = k[1];
			if (p.connected( n )){
				clearBoulders( p, n, k[0], k[1], 2 );
				p.guard( k[0] - 2, k[1], Guard.FOSSIL );
				p.guard( k[0] + 2, k[1], Guard.FOSSIL );
				return true;
			}
		}
		//no pocket within reach: behind the tomb, where its back wall shelters it
		for (int[] o : new int[][]{ { 0, -5 }, { -2, -5 }, { 2, -5 } }){
			if (!floorish( n.t( cx + o[0], cy + o[1] ) ) || !n.free( cx + o[0], cy + o[1] )) return false;
		}
		for (int[] o : new int[][]{ { 0, -5 }, { -2, -5 }, { 2, -5 } }){
			p.set( cx + o[0], cy + o[1], Terrain.EMPTY );
		}
		p.keyX = cx;
		p.keyY = cy - 5;
		p.guard( cx - 2, cy - 5, Guard.FOSSIL );
		p.guard( cx + 2, cy - 5, Guard.FOSSIL );
		return true;
	}

	/** Is this world cell one of a tomb's walls (its iron door included)? Pure; resolves. */
	public static boolean tombWall( long seed, int altitude, int wx, int wy ){
		Site s = tombAt( seed, altitude, wx, wy );
		return s != null && Math.max( Math.abs( wx - s.cx ), Math.abs( wy - s.cy ) ) == 3;
	}

	/** Is this world cell inside a tomb, walls included? Pure; resolves. */
	public static boolean tombBox( long seed, int altitude, int wx, int wy ){
		Site s = tombAt( seed, altitude, wx, wy );
		return s != null && Math.max( Math.abs( wx - s.cx ), Math.abs( wy - s.cy ) ) <= 3;
	}

	private static Site tombAt( long seed, int altitude, int wx, int wy ){
		int sx = Math.floorDiv( wx, SECTOR ), sy = Math.floorDiv( wy, SECTOR );
		if (typeOf( seed, altitude, sx, sy ) != Type.TOMB) return null;
		Site s = site( seed, altitude, sx, sy );
		return s != null && s.type == Type.TOMB ? s : null;
	}

	/** Debug scenes and tests: the place of this kind on this slice nearest (wx, wy), searching
	 *  rings of sectors out to `rings` (capped at 12), the kind read off each sector's hash before
	 *  anything is validated. null when there is none that near. */
	public static Site nearest( long seed, Type type, int altitude, int wx, int wy, int rings ){
		return nearest( seed, type, altitude, wx, wy, rings, false );
	}

	/** ...a mine with its shaft down, when `shaft` asks for one. */
	public static Site nearest( long seed, Type type, int altitude, int wx, int wy, int rings, boolean shaft ){
		if (!type.at( altitude )) return null;
		int sx0 = Math.floorDiv( wx, SECTOR ), sy0 = Math.floorDiv( wy, SECTOR );
		for (int r = 0; r <= Math.min( 12, rings ); r++){
			Site best = null;
			long bestD = Long.MAX_VALUE;
			for (int sy = sy0 - r; sy <= sy0 + r; sy++){
				for (int sx = sx0 - r; sx <= sx0 + r; sx++){
					if (Math.max( Math.abs( sx - sx0 ), Math.abs( sy - sy0 ) ) != r) continue;
					if (typeOf( seed, altitude, sx, sy ) != type) continue;
					Site s = site( seed, altitude, sx, sy );
					if (s == null || (shaft && !s.hasShaft())) continue;
					long d = (long)(s.cx - wx) * (s.cx - wx) + (long)(s.cy - wy) * (s.cy - wy);
					if (d < bestD){
						bestD = d;
						best = s;
					}
				}
			}
			if (best != null) return best;
		}
		return null;
	}

	// ------------------------------------------------------- into a window

	/**
	 * WindowGenerator.caves, after its ways: every place meeting the window laid into its terrain
	 * (never on a way, never on water), the shafts the mines of the slice above sink into it, and
	 * what the slice below keeps sealed under it (a tomb's box: no digging down onto it). Returns
	 * the window's places, an immutable list. Pure.
	 */
	static List<Object> overlay( long seed, int altitude, int ox, int oy, WindowGenerator.Window w ){
		final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
		ArrayList<Object> out = new ArrayList<>();
		int sx0 = Math.floorDiv( ox, SECTOR ), sx1 = Math.floorDiv( ox + W - 1, SECTOR );
		int sy0 = Math.floorDiv( oy, SECTOR ), sy1 = Math.floorDiv( oy + H - 1, SECTOR );
		for (int sy = sy0; sy <= sy1; sy++){
			for (int sx = sx0; sx <= sx1; sx++){
				Site s = site( seed, altitude, sx, sy );
				if (s == null || s.x1 < ox || s.y1 < oy || s.x0 > ox + W - 1 || s.y0 > oy + H - 1) continue;
				out.add( s );
				for (int i = 0; i < s.cells.length; i++){
					int c = local( s.cells[i], ox, oy );
					if (c == -1 || w.link[c] != WindowGenerator.LINK_NONE || w.terrain[c] == Terrain.WATER) continue;
					w.terrain[c] = s.terrain[i];
					w.waterDepth[c] = 0;
					if (s.hasShaft() && s.cells[i] == OverworldLevel.worldKey( s.shaftX, s.shaftY )){
						w.link[c] = WindowGenerator.LINK_LADDER_DOWN;
					}
				}
			}
		}
		//the bottoms of the shafts the mines above sank
		int up = altitude + 1;
		if (Type.MINE.at( up )){
			for (int sy = sy0; sy <= sy1; sy++){
				for (int sx = sx0; sx <= sx1; sx++){
					if (typeOf( seed, up, sx, sy ) != Type.MINE) continue;
					Site m = site( seed, up, sx, sy );
					if (m == null || !m.hasShaft()) continue;
					int c = local( OverworldLevel.worldKey( m.shaftX, m.shaftY ), ox, oy );
					if (c == -1 || w.link[c] != WindowGenerator.LINK_NONE) continue;
					w.terrain[c] = Terrain.ENTRANCE;
					w.link[c] = WindowGenerator.LINK_LADDER_UP;
					w.waterDepth[c] = 0;
					//the window's own offsets: PathFinder's are the live level's, whatever it is
					for (int[] o : N8){
						int nx = c % W + o[0], ny = c / W + o[1];
						if (nx <= 0 || ny <= 0 || nx >= W - 1 || ny >= H - 1) continue;
						int nc = nx + ny * W;
						if (w.link[nc] != WindowGenerator.LINK_NONE) continue;
						int t = w.terrain[nc];
						if (t == Terrain.WALL || t == Terrain.MINE_CRYSTAL || t == Terrain.BOULDER || t == Terrain.MINE_BOULDER){
							w.terrain[nc] = Terrain.EMPTY_DECO;
						}
					}
				}
			}
		}
		//the tombs of the slice below: their boxes, which nobody digs down onto
		int down = altitude - 1;
		ArrayList<int[]> sealed = new ArrayList<>();
		if (Type.TOMB.at( down )){
			for (int sy = sy0; sy <= sy1; sy++){
				for (int sx = sx0; sx <= sx1; sx++){
					if (typeOf( seed, down, sx, sy ) != Type.TOMB) continue;
					Site t = site( seed, down, sx, sy );
					if (t != null) sealed.add( new int[]{ t.cx - 3, t.cy - 3, t.cx + 3, t.cy + 3 } );
				}
			}
		}
		w.sealedBelow = new int[sealed.size() * 4];
		for (int i = 0; i < sealed.size(); i++) System.arraycopy( sealed.get( i ), 0, w.sealedBelow, 4 * i, 4 );
		return Collections.unmodifiableList( out );
	}

	//a world key's window cell, interior only (the ring is always the window's own wall), or -1
	private static int local( long key, int ox, int oy ){
		int x = (int)(key & 0xFFFFFFFFL) - ox, y = (int)(key >> 32) - oy;
		if (x <= 0 || y <= 0 || x >= WindowGenerator.WIDTH - 1 || y >= WindowGenerator.HEIGHT - 1) return -1;
		return x + y * WindowGenerator.WIDTH;
	}

	/**
	 * WindowGenerator.dress: the places' props on the window's layers - each only while its cell
	 * still holds what the place laid (a player's pick or bomb takes the prop with the terrain), a
	 * tomb's dressed stone only where its face shows, a top (a timber's cap beam, a mushroom's cap,
	 * the coffin's head) on the cell above unless that cell is a doorway or a stair. Pure.
	 */
	static void dress( List<Object> sites, int ox, int oy, int[] map, int[] ground, int[] edges, int[] canopy ){
		final int W = WindowGenerator.WIDTH;
		for (Object o : sites){
			if (!(o instanceof Site)) continue;
			for (Map.Entry<Long, Prop> e : ((Site) o).props.entrySet()){
				int c = local( e.getKey(), ox, oy );
				Prop p = e.getValue();
				if (c == -1 || map[c] != p.on) continue;
				if (p.on == Terrain.WALL && !Ores.showsFace( map, c )) continue;
				if (p.edges) edges[c] = p.tile;
				else ground[c] = p.tile;
				int above = c - W;
				if (p.top != -1 && above > W && !WindowGenerator.blocksSight( map[above] ) && canopy[above] == -1){
					canopy[above] = p.top;
				}
			}
		}
	}

	// ---------------------------------------------------------- the level's

	/** The examine name's key of a place's cell (a prop, the rift's crack, a tomb wall), or null. */
	static String cellKind( List<Object> sites, int wx, int wy, int terrain ){
		for (Object o : sites){
			if (!(o instanceof Site) || !((Site) o).holds( wx, wy )) continue;
			Prop p = ((Site) o).props.get( OverworldLevel.worldKey( wx, wy ) );
			if (p != null && terrain == p.on) return p.name;
		}
		return null;
	}

	/** Is the cell within a place's berth (its reach and four round it; a rift's reach)? */
	static boolean inBerth( List<Object> sites, int wx, int wy ){
		for (Object o : sites){
			if (o instanceof Site && ((Site) o).inBerth( wx, wy )) return true;
		}
		return false;
	}

	public static final int LOOT = 1, GUARDS = 2, KEY = 3, PIRANHA = 4;

	/** A place's once-only (and once-a-day) keys: its cache, its guards, the tomb's key, the
	 *  shrine's piranha. Every slice keeps its own (OverworldLevel's hoards_laid, layer_due_*). */
	public static long lootKey( Site s, int part ){
		return OverworldLevel.structHash( s.key ^ 0xCA7E5175L, part );
	}

	/**
	 * What burns round a hero on a cave slice (SettlementAmbience.gather): the lanterns, the
	 * camp's fire with its smoke, the furnace, the giant mushrooms' spores - at full strength at
	 * every hour (there is no day underground), each while the prop it hangs from still stands.
	 * Reads the level's resolved places alone: the render thread's Mirror calls it too.
	 */
	static void collectLights( OverworldLevel ow, int pos, HashSet<Long> live, ArrayList<SettlementAmbience.Light> lights,
			ArrayList<SettlementAmbience.Smoke> smoke, HashSet<Long> keys ){
		final int S = DungeonTilemap.SIZE, w = ow.width();
		int hwx = ow.worldX() + pos % w, hwy = ow.worldY() + pos / w;
		final int[] hm = SettlementLights.HEARTH_MOUTH;
		for (Site s : ow.caveSites()){
			for (int i = 0; i < s.lights.length; i += 4){
				int wx = s.lights[i], wy = s.lights[i + 1];
				int c = ow.localCell( wx, wy ), anchor = ow.localCell( wx, wy + s.lights[i + 3] );
				if (c == -1 || anchor == -1) continue;
				Prop p = s.props.get( OverworldLevel.worldKey( wx, wy + s.lights[i + 3] ) );
				if (p == null || ow.map[anchor] != p.on) continue;
				long key = OverworldLevel.worldKey( wx, wy );
				if (SettlementAmbience.wanted( live, key, wx, wy, hwx, hwy, keys )){
					lights.add( new SettlementAmbience.Light( key, s.lights[i + 2], (c % w) * S, (c / w) * S, 1f ) );
				}
			}
			for (int i = 0; i < s.smoke.length; i += 2){
				int wx = s.smoke[i], wy = s.smoke[i + 1];
				int c = ow.localCell( wx, wy );
				if (c == -1 || ow.map[c] != Terrain.EMBERS) continue;
				long key = OverworldLevel.worldKey( wx, wy );
				if (SettlementAmbience.wanted( live, key, wx, wy, hwx, hwy, keys )){
					smoke.add( new SettlementAmbience.Smoke( key, (c % w) * S + hm[0], (c / w) * S + hm[1], hm[2], hm[3],
							CAMP_SMOKE, 0, 0, true ) );
				}
			}
		}
	}

	//the camp's fire is banked low: a thin column
	static final float CAMP_SMOKE = 0.45f;

	/**
	 * Peoples a place wholly inside the window (OverworldLevel.populateLayerSites, actor thread,
	 * never on a mirror): its keeper (the trader, the idol, the forge) whenever none is about; its
	 * cache and the tomb's key once ever; its guards once a day, while what they keep still lies
	 * there (the rift's always: it is never emptied), each kind topped up to its number. Guards are
	 * wildlife - parked, and pruned when stale like any - which is why they come back.
	 */
	static void populate( OverworldLevel level, Site s ){
		int x0 = s.x0 - 8, y0 = s.y0 - 8, x1 = s.x1 + 8, y1 = s.y1 + 8;
		switch (s.type){
			case MINE:
				if (level.claimHoard( lootKey( s, LOOT ) )) lay( level, s.lootX, s.lootY, Heap.Type.HEAP, mineCache( s ) );
				if (heapAt( level, s.lootX, s.lootY )) guard( level, s );
				break;
			case GROTTO:
				if (level.claimHoard( lootKey( s, LOOT ) )) lay( level, s.lootX, s.lootY, Heap.Type.HEAP, grottoCache( s ) );
				if (heapAt( level, s.lootX, s.lootY )) guard( level, s );
				break;
			case CRYSTAL:
				if (level.claimHoard( lootKey( s, LOOT ) )) lay( level, s.lootX, s.lootY, Heap.Type.HEAP, crystalCache( s ) );
				if (heapAt( level, s.lootX, s.lootY )) guard( level, s );
				break;
			case CAMP:
				if (level.census( CaveMiner.class, x0, y0, x1, y1 ) == 0){
					CaveMiner miner = new CaveMiner();
					miner.openShop();
					place( level, miner, s.npcX, s.npcY, 1 );
				}
				break;
			case SHRINE:
				if (level.census( LakeShrine.class, s.npcX - 2, s.npcY - 2, s.npcX + 2, s.npcY + 2 ) == 0){
					place( level, new LakeShrine(), s.npcX, s.npcY, 0 );
				}
				//its once-only key spent only on the day there is open water to set it in
				int deep = deepWater( level, s );
				if (deep != -1 && level.claimHoard( lootKey( s, PIRANHA ) )) level.addMob( new ShrinePiranha(), deep );
				break;
			case RIFT:
				if (level.census( DwarvenForge.class, s.npcX - 2, s.npcY - 2, s.npcX + 2, s.npcY + 2 ) == 0){
					place( level, new DwarvenForge(), s.npcX, s.npcY, 0 );
				}
				guard( level, s );
				break;
			case TOMB:
				if (level.claimHoard( lootKey( s, KEY ) )){
					lay( level, s.keyX, s.keyY, Heap.Type.SKELETON, new IronKey( WorldLayers.depthOf( s.altitude ) ),
							new Gold( Random.IntRange( 40, 80 ) ) );
				}
				if (level.claimHoard( lootKey( s, LOOT ) )) lay( level, s.lootX, s.lootY, Heap.Type.CHEST, tombCache( s ) );
				int door = level.localCell( s.cx, s.cy + 3 );
				if (heapAt( level, s.keyX, s.keyY ) || (door != -1 && level.map[door] == Terrain.LOCKED_DOOR)) guard( level, s );
				break;
		}
	}

	private static boolean heapAt( OverworldLevel level, int wx, int wy ){
		int c = level.localCell( wx, wy );
		return c != -1 && level.heaps.get( c ) != null;
	}

	private static void lay( OverworldLevel level, int wx, int wy, Heap.Type type, Item... items ){
		int c = level.localCell( wx, wy );
		if (c == -1) return;
		Heap h = null;
		for (Item it : items){
			if (it != null) h = level.drop( it, c );
		}
		if (h != null) h.type = type;
	}

	private static void place( OverworldLevel level, Mob m, int wx, int wy, int radius ){
		int c = level.localCell( wx, wy );
		if (c == -1) return;
		int at = level.freeSpotWithin( c, radius );
		if (at != -1) level.addMob( m, at );
	}

	//the day's guard: each kind topped up to its number round the place (the ones parked by the
	//window counted too), once a world day
	private static void guard( OverworldLevel level, Site s ){
		if (s.guards.length == 0 || !level.dueOnce( lootKey( s, GUARDS ), 1 )) return;
		int[] want = new int[Guard.values().length];
		for (int i = 0; i < s.guards.length; i += 3) want[s.guards[i + 2]]++;
		int[] have = new int[want.length];
		for (Guard g : Guard.values()){
			if (want[g.ordinal()] > 0) have[g.ordinal()] = level.census( g.cls, s.x0 - 8, s.y0 - 8, s.x1 + 8, s.y1 + 8 );
		}
		for (int i = 0; i < s.guards.length; i += 3){
			Guard g = Guard.values()[s.guards[i + 2]];
			if (have[g.ordinal()]-- > 0) continue;
			Mob m = Reflection.newInstance( g.cls );
			if (m != null) place( level, m, s.guards[i], s.guards[i + 1], 2 );
		}
	}

	//the shrine's pool: the first open water, row by row within six of the idol, with water all round
	private static int deepWater( OverworldLevel level, Site s ){
		for (int y = s.npcY - 6; y <= s.npcY + 6; y++){
			for (int x = s.npcX - 6; x <= s.npcX + 6; x++){
				int c = level.localCell( x, y );
				if (c == -1 || !level.water[c] || level.occupied( c )) continue;
				boolean open = true;
				for (int[] o : N8){
					int nc = level.localCell( x + o[0], y + o[1] );
					open &= nc != -1 && level.water[nc];
				}
				if (open) return c;
			}
		}
		return -1;
	}

	private static Item[] mineCache( Site s ){
		ArrayList<Item> out = new ArrayList<>();
		out.add( Ores.lumps( Ores.commonKind( s.altitude ), 3 + (-s.altitude) / 2 ) );
		if (Math.floorMod( OverworldLevel.structHash( s.key, 7 ), 3 ) == 0) out.add( new Torch().quantity( 2 ) );
		if (Math.floorMod( OverworldLevel.structHash( s.key, 8 ), 4 ) == 0){
			//a pick for a hero who has none (it is unique); a hero who has one finds torches
			if (Dungeon.hero != null && Dungeon.hero.belongings.getItem( Pickaxe.class ) == null){
				Item pick = new Pickaxe();
				pick.identify( false );
				out.add( pick );
			} else {
				out.add( new Torch().quantity( 3 ) );
			}
		}
		return out.toArray( new Item[0] );
	}

	private static Item[] grottoCache( Site s ){
		ArrayList<Item> out = new ArrayList<>();
		Class<?>[] caps = { BlueMilk.class, DeathCap.class, Earthstar.class, GoldenJelly.class, JackOLantern.class, PixieParasol.class };
		for (int i = 0; i < 3; i++) out.add( (Item) Reflection.newInstance( (Class<?>) Random.element( caps ) ) );
		out.add( Generator.randomUsingDefaults( Generator.Category.SEED ) );
		if (s.altitude <= -5) out.add( Generator.randomUsingDefaults( Generator.Category.SEED ) );
		return out.toArray( new Item[0] );
	}

	private static Item[] crystalCache( Site s ){
		ArrayList<Item> out = new ArrayList<>();
		int gems = 2 + (-s.altitude) / 3;
		for (int i = 0; i < gems; i++){
			Item gem = Reflection.newInstance( Ores.rollGem( s.altitude, Random.Int( 100 ) ).item );
			if (gem != null) out.add( gem );
		}
		out.add( new EnergyCrystal( 4 + (-s.altitude) / 2 ) );
		return out.toArray( new Item[0] );
	}

	private static Item[] tombCache( Site s ){
		ArrayList<Item> out = new ArrayList<>();
		Item relic = Generator.randomArtifact();
		out.add( relic != null ? relic : Generator.random( Generator.Category.RING ) );
		if (Random.Int( 2 ) == 0) out.add( Generator.random( Generator.Category.WAND ) );
		out.add( new Gold( 300 + 60 * (-s.altitude) ) );
		out.add( Ores.lumps( Ores.richestKind( s.altitude ), 3 ) );
		return out.toArray( new Item[0] );
	}

	// ---------------------------------------------------------- the rift's

	//the heat a crack cell gives the air each turn a hero is near: on top of the embers' own, it
	//keeps the cells beside the crack (the forge's included) warm, never faint-hot (CaveSitesTest)
	static final float RIFT_HEAT = 3f;

	/**
	 * The host's tick on a cave slice (OverworldLevel.tickLayer): the burning rifts near a hero
	 * warm the air over their cracks, and flare. A flare picks a crack cell - one a hero stands
	 * on when any does - and is seen and told first: it glows, and whoever stands there hears
	 * "move!". The next turn it bursts, and whoever is still on it takes a few points of fire,
	 * the same at every depth. Never a Fire: nothing spreads, nothing scales.
	 */
	static void tick( OverworldLevel level, int heroPos ){
		if (Dungeon.level != level) return;
		ArrayList<Hero> heroes = OverworldLevel.heroesOn( level );
		if (heroes.isEmpty()) return;
		float now = Actor.now();
		for (Site s : level.caveSites()){
			if (s.type != Type.RIFT) continue;
			boolean near = false;
			for (Hero h : heroes){
				int hx = level.worldX() + h.pos % level.width(), hy = level.worldY() + h.pos / level.width();
				near |= Math.max( Math.abs( hx - s.cx ), Math.abs( hy - s.cy ) ) <= 12;
			}
			if (!near) continue;
			ArrayList<Integer> cells = new ArrayList<>();
			for (long k : s.fissure){
				int c = level.localCell( (int)(k & 0xFFFFFFFFL), (int)(k >> 32) );
				if (c == -1 || level.map[c] != Terrain.EMBERS) continue;
				boolean close = false;
				for (Hero h : heroes) close |= level.distance( c, h.pos ) <= 8;
				if (!close) continue;
				cells.add( c );
				TileTemperature.depositHeat( c, RIFT_HEAT );
			}
			flare( level, s, cells, heroes, now );
		}
	}

	private static void flare( OverworldLevel level, Site s, ArrayList<Integer> cells, ArrayList<Hero> heroes, float now ){
		if (level.riftFlareKey != Long.MIN_VALUE && now >= level.riftFlareAt){
			int c = level.localCell( (int)(level.riftFlareKey & 0xFFFFFFFFL), (int)(level.riftFlareKey >> 32) );
			level.riftFlareKey = Long.MIN_VALUE;
			if (c != -1){
				CellEmitter.get( c ).burst( FlameParticle.FACTORY, 8 );
				Char ch = Actor.findChar( c );
				if (ch != null && !Char.hasProp( ch, Char.Property.FIERY )){
					int dmg = Math.max( 0, Random.NormalIntRange( 3, 8 ) - ch.drRoll() );
					if (dmg > 0) ch.damage( dmg, CaveSites.class );
					if (ch == Dungeon.hero && !ch.isAlive()){
						Dungeon.fail( CaveSites.class );
						GLog.n( Messages.get( CaveSites.class, "rift_death" ) );
					}
				}
			}
			return;
		}
		if (level.riftFlareKey != Long.MIN_VALUE || cells.isEmpty() || now < level.riftNextFlare) return;
		level.riftNextFlare = now + Random.IntRange( 3, 6 );
		//a hero on the crack draws it; with none on it, it flares where it will
		int target = -1;
		for (Hero h : heroes){
			if (cells.contains( h.pos )) target = h.pos;
		}
		if (target == -1) target = cells.get( Random.Int( cells.size() ) );
		level.riftFlareKey = OverworldLevel.worldKey( level.worldX() + target % level.width(), level.worldY() + target / level.width() );
		level.riftFlareAt = now + 1f;
		CellEmitter.get( target ).burst( FlameParticle.FACTORY, 4 );
		for (Hero h : heroes){
			if (h.pos == target) NetManager.heroLog( h, GLog.WARNING + Messages.get( CaveSites.class, "rift_flare" ) );
		}
	}

	// ---------------------------------------------------------- the rumours

	/**
	 * What the camp's miner has heard (actor thread, CaveMiner.interact; cached on the level until
	 * the window moves): the two nearest places of his slice that are not his own camp - the
	 * window's and his neighbouring sectors' - the nearest of the slice below round him, and the
	 * richest vein near him. All from the pure functions, so every word of it is true.
	 */
	public static String rumours( OverworldLevel level, int cell ){
		int w = level.width();
		int wx = level.worldX() + cell % w, wy = level.worldY() + cell / w;
		StringBuilder sb = new StringBuilder( Messages.get( CaveMiner.class, "news_intro" ) );
		int lines = 0;
		for (Site s : rumoured( level, cell )){
			sb.append( "\n\n" ).append( Messages.get( CaveMiner.class, s.altitude == level.altitude() ? "rumour_line" : "rumour_below",
					phrase( s.type ), direction( s.cx - wx, s.cy - wy ), distance( s.cx - wx, s.cy - wy ) ) );
			lines++;
		}
		int[] vein = Ores.notableVein( level.worldSeed(), level.altitude(), wx, wy, 48 );
		if (vein != null){
			Ores.Kind k = Ores.Kind.values()[vein[2]];
			sb.append( "\n\n" ).append( Messages.get( CaveMiner.class, "rumour_vein",
					direction( vein[0] - wx, vein[1] - wy ), distance( vein[0] - wx, vein[1] - wy ),
					Messages.get( k.item, "name" ) ) );
			lines++;
		}
		if (lines == 0) return Messages.get( CaveMiner.class, "rumour_none" );
		return sb.toString();
	}

	/** The places a miner on this cell talks of, in the order he does: the two nearest of his own
	 *  slice that are not his own camp, then the nearest of the slice below. */
	static ArrayList<Site> rumoured( OverworldLevel level, int cell ){
		int w = level.width();
		int wx = level.worldX() + cell % w, wy = level.worldY() + cell / w;
		long seed = level.worldSeed();
		int alt = level.altitude();
		int sx0 = Math.floorDiv( wx, SECTOR ), sy0 = Math.floorDiv( wy, SECTOR );
		ArrayList<Site> here = new ArrayList<>();
		for (Site s : level.caveSites()) if (!here.contains( s )) here.add( s );
		for (int sy = sy0 - 1; sy <= sy0 + 1; sy++){
			for (int sx = sx0 - 1; sx <= sx0 + 1; sx++){
				Site s = site( seed, alt, sx, sy );
				if (s != null && !here.contains( s )) here.add( s );
			}
		}
		Site own = null;
		for (Site s : here) if (s.type == Type.CAMP && Math.max( Math.abs( s.npcX - wx ), Math.abs( s.npcY - wy ) ) <= 6) own = s;
		here.remove( own );
		sortByDistance( here, wx, wy );
		ArrayList<Site> out = new ArrayList<>( here.subList( 0, Math.min( 2, here.size() ) ) );
		if (WorldLayers.exists( alt - 1 )){
			ArrayList<Site> below = new ArrayList<>();
			for (int sy = sy0 - 1; sy <= sy0 + 1; sy++){
				for (int sx = sx0 - 1; sx <= sx0 + 1; sx++){
					Site s = site( seed, alt - 1, sx, sy );
					if (s != null) below.add( s );
				}
			}
			sortByDistance( below, wx, wy );
			if (!below.isEmpty()) out.add( below.get( 0 ) );
		}
		return out;
	}

	private static void sortByDistance( ArrayList<Site> l, int wx, int wy ){
		Collections.sort( l, ( a, b ) -> Long.compare(
				(long)(a.cx - wx) * (a.cx - wx) + (long)(a.cy - wy) * (a.cy - wy),
				(long)(b.cx - wx) * (b.cx - wx) + (long)(b.cy - wy) * (b.cy - wy) ) );
	}

	/** The rumour's phrase for a kind of place: "an old mine". */
	public static String phrase( Type t ){
		return Messages.get( CaveSites.class, "rumour_" + t.key() );
	}

	private static final String[] DIRS8 = { "e", "ne", "n", "nw", "w", "sw", "s", "se" };

	/** "to the north-east", from (dx, dy) in world cells (y grows southward). */
	public static String direction( int dx, int dy ){
		double a = Math.atan2( -dy, dx );
		int oct = (int) Math.floorMod( Math.round( a / (Math.PI / 4) ), 8 );
		return Messages.get( CaveSites.class, "dir_" + DIRS8[oct] );
	}

	/** How far, as a miner says it: under 48 cells "not far off", under 110 "a fair walk away". */
	public static String distance( int dx, int dy ){
		int d = Math.max( Math.abs( dx ), Math.abs( dy ) );
		return Messages.get( CaveSites.class, d < 48 ? "dist_near" : d < 110 ? "dist_mid" : "dist_far" );
	}

	/** The line a hero hears on first seeing a place. */
	static String foundLine( Site s ){
		return Messages.get( CaveSites.class, "found_" + s.type.key() );
	}
}

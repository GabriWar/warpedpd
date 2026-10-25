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

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle.Phase;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The places of the world's wet ground (docs/wetlands.md): a swamp witch's hut standing in the
 * bog, a shrine half sunk in it, and rock pools on a beach that the tide fills and empties. Like
 * every other site they live on WorldStructures' sector grid (Site.WITCH_HUT, SUNKEN_SHRINE,
 * TIDE_POOLS), on the sectors no older site claimed, and everything here is a pure function of
 * (seed, sector): the same in every window, reload and co-op mirror.
 *
 * Swamps are rare and small (a few cells in a thousand of the land), so a place is not asked of
 * its sector's anchor alone: its centre is sought on a lattice inside the sector, spiralling out
 * from the anchor, at the first spot whose ground (at the year's mean) is a swamp with room for
 * a hut or a shrine, or a beach by the sea with room for pools. Every swamp sector with room
 * keeps a place (a hut or a shrine by the sector's parity, so a swamp spanning two has both);
 * one beach sector in three keeps pools. A place keeps clear of the roads,
 * the town, every older site's grounds and every village's fields (WorldStructures.baseType:
 * the older sites never look back at these, so none of them moves).
 *
 * The rest of this class is the places' rules, pure so the tests can pin them: the tide, the
 * pools' refill, the shrine's blessing, the swamp's mist and the bog's drag on the hero's legs.
 */
public final class WetlandSites {

	private WetlandSites(){}

	// ------------------------------------------------------------ where they lie

	//the wetlands' own salt: no older site, road, village or barrow reads it
	static final long SALT = 0x3E71A4D5EL;
	//the lattice a place's centre is sought on, never nearer its sector's edge than INSET cells:
	//its grounds (REACH at most) stay inside the sector, and two places never come near
	static final int INSET = 16, STEP = 6;
	/** How far a place's grounds reach from its centre (Chebyshev): the hut's boardwalk, the widest. */
	public static final int REACH = 6;
	//how far a place keeps from the grounds of an older site (its anchor), and from the town
	static final int SITE_CLEAR = 8 + REACH, TOWN_CLEAR = WorldStructures.TOWN_SIZE / 2 + 16 + REACH;
	//of the beach sectors with room for pools, how many keep them: one in three (swamps are rare,
	//and every swamp sector with room keeps its place)
	static final int BEACH_KEEP = 1, BEACH_OF = 3;
	//how much of the ground round a hut or a shrine (a 5x5 lattice two cells apart) must be swamp
	static final int SWAMP_SHARE = 9;

	/** A wetland place: its kind, its sector and where it stands. */
	public static final class Place {
		public final WorldStructures.Site kind;
		public final int sx, sy, cx, cy;
		//the pools and the rocks round them of a beach's tide pools, world cells as {x, y, x, y...}
		final int[] pools, rocks;

		Place( WorldStructures.Site kind, int sx, int sy, int cx, int cy, int[] pools, int[] rocks ){
			this.kind = kind;
			this.sx = sx;
			this.sy = sy;
			this.cx = cx;
			this.cy = cy;
			this.pools = pools;
			this.rocks = rocks;
		}

		/** The key it is kept by in every store: its sector's (WorldStructures.sectorOf). */
		public long key(){
			return WorldStructures.sectorOf( sx, sy );
		}

		/** The pools' world cells, {x, y, ...} (tide pools only, empty otherwise). */
		public int[] pools(){
			return pools.clone();
		}

		/** Is this world cell within its grounds (Chebyshev REACH of its centre)? */
		public boolean holds( int wx, int wy ){
			return Math.max( Math.abs( wx - cx ), Math.abs( wy - cy ) ) <= REACH;
		}
	}

	private static final int[] NONE = new int[0];
	//per (seed, sector): the place laid, or the NOWHERE marker
	private static final Place NOWHERE = new Place( WorldStructures.Site.NONE, 0, 0, 0, 0, NONE, NONE );
	private static final Map<Long, Place> cache = new ConcurrentHashMap<>();
	private static volatile long cachedSeed = Long.MIN_VALUE;

	/** Is this one of the wetlands' kinds? */
	public static boolean isWetland( WorldStructures.Site s ){
		return s == WorldStructures.Site.WITCH_HUT || s == WorldStructures.Site.SUNKEN_SHRINE
				|| s == WorldStructures.Site.TIDE_POOLS;
	}

	/**
	 * The place a sector holds, or null: only ever asked of a sector no older site claimed
	 * (WorldStructures.computeSiteType). Pure, cached per seed; any thread.
	 */
	public static Place place( long seed, int sx, int sy ){
		if (seed != cachedSeed){
			synchronized (cache){
				if (seed != cachedSeed){
					cache.clear();
					cachedSeed = seed;
				}
			}
		}
		Long key = WorldStructures.sectorOf( sx, sy );
		Place p = cache.get( key );
		if (p == null){
			p = resolve( seed, sx, sy );
			cache.put( key, p == null ? NOWHERE : p );
		}
		return p == NOWHERE ? null : p;
	}

	//the first lattice spot, spiralling out from the sector's anchor, with room for a place
	private static Place resolve( long seed, int sx, int sy ){
		long h = hash( seed ^ SALT, sx, sy );
		int ax = WorldStructures.siteX( seed, sx, sy ), ay = WorldStructures.siteY( seed, sx, sy );
		int x0 = sx * WorldStructures.SECTOR + INSET, y0 = sy * WorldStructures.SECTOR + INSET;
		int n = (WorldStructures.SECTOR - 2 * INSET) / STEP + 1;
		//the lattice nearest the anchor first, ties by a hash so no corner is favoured
		ArrayList<int[]> spots = new ArrayList<>( n * n );
		for (int j = 0; j < n; j++){
			for (int i = 0; i < n; i++){
				int x = x0 + i * STEP, y = y0 + j * STEP;
				long d = (long)(x - ax) * (x - ax) + (long)(y - ay) * (y - ay);
				spots.add( new int[]{ x, y, (int) Math.min( Integer.MAX_VALUE, d ), (int)(hash( h, x, y ) & 0xFFFF) } );
			}
		}
		spots.sort( (a, b) -> a[2] != b[2] ? Integer.compare( a[2], b[2] ) : Integer.compare( a[3], b[3] ) );
		WorldModel.Sample smp = new WorldModel.Sample();
		for (int[] s : spots){
			WorldModel.Biome b = biome( seed, s[0], s[1], smp );
			if (b != WorldModel.Biome.SWAMP && b != WorldModel.Biome.BEACH) continue;
			if (!clearOfOthers( seed, sx, sy, s[0], s[1] )) continue;
			Place p;
			if (b == WorldModel.Biome.SWAMP){
				if (!swampFits( seed, s[0], s[1], smp )) continue;
				//neighbouring swamp sectors keep the other kind: a swamp that spans two has both
				WorldStructures.Site kind = ((sx + sy) & 1) == 0 ? WorldStructures.Site.WITCH_HUT
						: WorldStructures.Site.SUNKEN_SHRINE;
				p = new Place( kind, sx, sy, s[0], s[1], NONE, NONE );
			} else {
				p = tidePools( seed, sx, sy, s[0], s[1], h, smp );
				if (p == null) continue;
				if (Math.floorMod( h >>> 8, (long) BEACH_OF ) >= BEACH_KEEP) return null;
			}
			if (!clearOfRoads( seed, p.cx, p.cy )) continue;
			return p;
		}
		return null;
	}

	//the biome at the year's mean: what the ground is, season aside
	private static WorldModel.Biome biome( long seed, int wx, int wy, WorldModel.Sample smp ){
		return WorldModel.sample( seed, wx, wy, 0f, smp ).biome;
	}

	//clear of the town, of every older site's grounds and of every village's houses, fence, lane
	//and fields (all of them within the layout's radius plus FIELD_REACH of its well)
	private static boolean clearOfOthers( long seed, int sx, int sy, int cx, int cy ){
		if (Math.max( Math.abs( cx ), Math.abs( cy ) ) <= TOWN_CLEAR) return false;
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				WorldStructures.Site t = WorldStructures.baseType( seed, sx + dx, sy + dy );
				if (t == WorldStructures.Site.NONE) continue;
				int reach = t == WorldStructures.Site.VILLAGE
						? WorldStructures.settlementLayout( seed, sx + dx, sy + dy )[0] + WorldStructures.FIELD_REACH + REACH + 2
						: SITE_CLEAR;
				int d = Math.max( Math.abs( WorldStructures.siteX( seed, sx + dx, sy + dy ) - cx ),
						Math.abs( WorldStructures.siteY( seed, sx + dx, sy + dy ) - cy ) );
				if (d <= reach) return false;
			}
		}
		return true;
	}

	//no road on the grounds or a cell round them
	private static boolean clearOfRoads( long seed, int cx, int cy ){
		for (int y = cy - REACH - 1; y <= cy + REACH + 1; y++){
			for (int x = cx - REACH - 1; x <= cx + REACH + 1; x++){
				if (WorldStructures.onRoad( seed, x, y )) return false;
			}
		}
		return true;
	}

	//a hut or a shrine: the bog round it is swamp a good part over, no sea, river or mountain
	//lies on the house or the shrine's ring (a river may run past the bog's edge), and the bank
	//the hut's boardwalk comes to is dry
	private static boolean swampFits( long seed, int cx, int cy, WorldModel.Sample smp ){
		int swamp = 0;
		for (int dy = -4; dy <= 4; dy += 2){
			for (int dx = -4; dx <= 4; dx += 2){
				WorldModel.sample( seed, cx + dx, cy + dy, 0f, smp );
				WorldModel.Biome b = smp.biome;
				if (b == WorldModel.Biome.MOUNTAIN || WorldLayers.band( smp.elev ) != 0) return false;
				if (Math.max( Math.abs( dx ), Math.abs( dy ) ) <= 2
						&& (b == WorldModel.Biome.OCEAN || b == WorldModel.Biome.RIVER)) return false;
				if (b == WorldModel.Biome.SWAMP) swamp++;
			}
		}
		if (swamp < SWAMP_SHARE) return false;
		WorldModel.sample( seed, cx, cy + WALK, 0f, smp );
		return smp.biome != WorldModel.Biome.OCEAN && smp.biome != WorldModel.Biome.RIVER;
	}

	//the spots round a beach's centre a pool may lie on, a pool's own shape (its cells after the
	//first, by hash) and the most pools a beach keeps
	private static final int[][] POOL_SPOTS = { {0, 0}, {-3, -2}, {3, -2}, {-3, 2}, {3, 2}, {0, -4}, {0, 4}, {-5, 0}, {5, 0} };
	private static final int[][] POOL_GROW = { {1, 0}, {0, 1}, {-1, 0}, {0, -1} };
	static final int MAX_POOLS = 5, MIN_POOLS = 3;

	//rock pools on a beach by the sea: three to five little pools on the sand, each two to four
	//cells, rocks round them with a way in left open, the sea within reach, no river mouth
	private static Place tidePools( long seed, int sx, int sy, int cx, int cy, long h, WorldModel.Sample smp ){
		boolean sea = false;
		for (int dy = -REACH; dy <= REACH; dy += 2){
			for (int dx = -REACH; dx <= REACH; dx += 2){
				WorldModel.sample( seed, cx + dx, cy + dy, 0f, smp );
				if (smp.biome == WorldModel.Biome.RIVER || WorldLayers.band( smp.elev ) != 0) return null;
				if (smp.biome == WorldModel.Biome.OCEAN) sea = true;
			}
		}
		if (!sea) return null;
		ArrayList<Integer> pools = new ArrayList<>(), rocks = new ArrayList<>();
		java.util.HashSet<Long> pooled = new java.util.HashSet<>();
		int laid = 0;
		for (int k = 0; k < POOL_SPOTS.length && laid < MAX_POOLS; k++){
			int px = cx + POOL_SPOTS[k][0], py = cy + POOL_SPOTS[k][1];
			if (!dryBeach( seed, px, py, smp )) continue;
			long ph = hash( h, px, py );
			int size = 2 + (int) Math.floorMod( ph, 3L );
			ArrayList<int[]> cells = new ArrayList<>();
			cells.add( new int[]{ px, py } );
			for (int g = 0; g < 4 && cells.size() < size; g++){
				int[] d = POOL_GROW[(int) Math.floorMod( (ph >>> 4) + g, 4L )];
				int gx = px + d[0], gy = py + d[1];
				if (dryBeach( seed, gx, gy, smp )) cells.add( new int[]{ gx, gy } );
			}
			for (int[] c : cells){
				pools.add( c[0] );
				pools.add( c[1] );
				pooled.add( OverworldLevel.worldKey( c[0], c[1] ) );
			}
			laid++;
		}
		if (laid < MIN_POOLS) return null;
		//rocks round the pools: three in five of the dry sand cells beside a pool, never on another
		//pool, and every pool keeps the sand south of its first cell open, the way in
		java.util.HashSet<Long> open = new java.util.HashSet<>();
		for (int i = 0; i < pools.size(); i += 2){
			open.add( OverworldLevel.worldKey( pools.get( i ), pools.get( i + 1 ) + 1 ) );
		}
		java.util.HashSet<Long> rocked = new java.util.HashSet<>();
		for (int i = 0; i < pools.size(); i += 2){
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int rx = pools.get( i ) + dx, ry = pools.get( i + 1 ) + dy;
					long rk = OverworldLevel.worldKey( rx, ry );
					if (pooled.contains( rk ) || open.contains( rk ) || rocked.contains( rk )) continue;
					if (Math.max( Math.abs( rx - cx ), Math.abs( ry - cy ) ) > REACH) continue;
					if (Math.floorMod( hash( h ^ 0x70C5L, rx, ry ), 5L ) >= 3 || !dryBeach( seed, rx, ry, smp )) continue;
					rocked.add( rk );
					rocks.add( rx );
					rocks.add( ry );
				}
			}
		}
		return new Place( WorldStructures.Site.TIDE_POOLS, sx, sy, cx, cy, toArray( pools ), toArray( rocks ) );
	}

	//sand above the sea, at the year's mean
	private static boolean dryBeach( long seed, int wx, int wy, WorldModel.Sample smp ){
		return WorldModel.sample( seed, wx, wy, 0f, smp ).biome == WorldModel.Biome.BEACH;
	}

	private static int[] toArray( ArrayList<Integer> list ){
		int[] a = new int[list.size()];
		for (int i = 0; i < a.length; i++) a[i] = list.get( i );
		return a;
	}

	static long hash( long seed, long x, long y ){
		long h = seed ^ 0x6E7A1D5L;
		h ^= x * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft( h, 29 );
		h ^= y * 0xC2B2AE3D27D4EB4FL;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 29;
		return h;
	}

	// ------------------------------------------------------------ what they lay

	/** The hut's boardwalk runs south from its door to the bank, WALK cells out. */
	public static final int WALK = 6;

	/**
	 * The terrain a place lays on a world cell, or -1 where the land keeps its own (WorldStructures
	 * .terrainAt). The hut: a five-by-five house of the villages' kind (a door in the south wall)
	 * standing in a moat of bog water, a boardwalk from its door to the bank, pools of the bog's
	 * water scattered round it. The shrine: its idol in the middle of a flooded floor, the broken
	 * ring of its walls round it with a statue at each corner, the way in from the south, the
	 * bog's water deep round it. The pools: the water and the rocks laid at placing.
	 */
	public static int terrain( Place p, int wx, int wy ){
		if (p == null) return -1;
		int dx = wx - p.cx, dy = wy - p.cy;
		int r = Math.max( Math.abs( dx ), Math.abs( dy ) );
		if (r > REACH) return -1;
		switch (p.kind){
			case WITCH_HUT: {
				if (dx == 0 && dy > 2) return dy == WALK ? Terrain.GRASS : Terrain.BRIDGE;
				if (r <= 1) return Terrain.EMPTY_SP;
				if (r == 2) return dx == 0 && dy == 2 ? Terrain.DOOR : Terrain.WALL;
				if (r == 3) return Terrain.WATER;
				long hh = hash( SALT ^ 0xB06L, wx, wy );
				if (r == 4) return (hh & 1) == 0 ? Terrain.WATER : -1;
				return (hh & 3) == 0 ? Terrain.WATER : -1;
			}
			case SUNKEN_SHRINE: {
				//the floor and the idol's plinth lie under the water now
				if (r <= 1) return Terrain.WATER;
				long hh = hash( SALT ^ 0x5A1L, wx, wy );
				if (r == 2){
					//a statue of the old faith at each corner, the broken ring of its walls between
					//them (gaps where it fell, the way in to the south always one): plain stone, as
					//the hut's walls are (the surface's dressed wall is the caves' gold ore)
					if (Math.abs( dx ) == 2 && Math.abs( dy ) == 2) return Terrain.STATUE;
					if (dx == 0 && dy == 2) return Terrain.WATER;
					return Math.floorMod( hh, 10L ) < 6 ? Terrain.WALL : Terrain.WATER;
				}
				if (r == 3) return Math.floorMod( hh, 10L ) < 7 ? Terrain.WATER : -1;
				if (r == 4) return Math.floorMod( hh, 10L ) < 3 ? Terrain.WATER : -1;
				return -1;
			}
			case TIDE_POOLS: {
				for (int i = 0; i < p.pools.length; i += 2){
					if (p.pools[i] == wx && p.pools[i + 1] == wy) return Terrain.WATER;
				}
				for (int i = 0; i < p.rocks.length; i += 2){
					if (p.rocks[i] == wx && p.rocks[i + 1] == wy) return Terrain.BOULDER;
				}
				return -1;
			}
			default:
				return -1;
		}
	}

	/** The place of this kind nearest the world cell (wx, wy), ring by ring of sectors out to
	 *  `sectors`, or null. Pure, and slow far out: debug scenes and tests, off the render thread. */
	public static Place nearest( long seed, WorldStructures.Site kind, int wx, int wy, int sectors ){
		int sx0 = Math.floorDiv( wx, WorldStructures.SECTOR ), sy0 = Math.floorDiv( wy, WorldStructures.SECTOR );
		Place best = null;
		long bestD = Long.MAX_VALUE;
		for (int r = 0; r <= sectors; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					if (WorldStructures.siteType( seed, sx0 + dx, sy0 + dy ) != kind) continue;
					Place p = place( seed, sx0 + dx, sy0 + dy );
					long d = (long)(p.cx - wx) * (p.cx - wx) + (long)(p.cy - wy) * (p.cy - wy);
					if (d < bestD){
						bestD = d;
						best = p;
					}
				}
			}
			//a ring further out can still hold a nearer one only while it is within the best's reach
			if (best != null && (long)(r - 1) * WorldStructures.SECTOR * (r - 1) * WorldStructures.SECTOR > bestD) break;
		}
		return best;
	}

	/** The wetland places whose grounds reach the world rect [x0,x1] x [y0,y1], in sector order. */
	public static ArrayList<Place> placesIn( long seed, int x0, int y0, int x1, int y1 ){
		ArrayList<Place> out = new ArrayList<>();
		int sx0 = Math.floorDiv( x0 - REACH, WorldStructures.SECTOR ), sx1 = Math.floorDiv( x1 + REACH, WorldStructures.SECTOR );
		int sy0 = Math.floorDiv( y0 - REACH, WorldStructures.SECTOR ), sy1 = Math.floorDiv( y1 + REACH, WorldStructures.SECTOR );
		for (int sy = sy0; sy <= sy1; sy++){
			for (int sx = sx0; sx <= sx1; sx++){
				if (!isWetland( WorldStructures.siteType( seed, sx, sy ) )) continue;
				Place p = place( seed, sx, sy );
				if (p == null || p.cx + REACH < x0 || p.cx - REACH > x1 || p.cy + REACH < y0 || p.cy - REACH > y1) continue;
				out.add( p );
			}
		}
		return out;
	}

	/**
	 * The witch's hut in a window's dressing (WindowGenerator.dress, on the window worker): the
	 * village house's two windows in its front wall and a chimney stack over its back wall
	 * (SettlementLights' very cells, by the window's map, so the lights and the smoke find them).
	 */
	static void dressFittings( long seed, int ox, int oy, int[] map, int[] ground, int[] canopy ){
		int w = WindowGenerator.WIDTH, h = WindowGenerator.HEIGHT;
		for (Place p : placesIn( seed, ox, oy, ox + w - 1, oy + h - 1 )){
			if (p.kind != WorldStructures.Site.WITCH_HUT) continue;
			int style = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.VILLAGE_WINDOWS[SettlementLights.windowStyle( seed, p.cx, p.cy )];
			for (int[] o : SettlementLights.HOUSE_WINDOWS){
				int c = SettlementLights.glassCell( seed, map, w, h, ox, oy, p.cx + o[0], p.cy + o[1] );
				if (c != -1) ground[c] = style;
			}
			int c = SettlementLights.chimneyCell( seed, map, w, h, ox, oy, p.cx, p.cy );
			if (c != -1) canopy[c] = xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress.VILLAGE_CHIMNEY;
		}
	}

	/** The cell the witch keeps her shelf from: the middle of the hut's back row, her wares on
	 *  the cells round her (Shopkeeper.restock), the row inside the door left free. */
	public static int[] witchCell( Place p ){
		return new int[]{ p.cx, p.cy - 1 };
	}

	/** Her cauldron: in the corner east of the door, out of her shelf's reach. */
	public static int[] cauldronCell( Place p ){
		return new int[]{ p.cx + 1, p.cy + 1 };
	}

	/** The idol of a sunken shrine: in the middle of its flooded floor. */
	public static int[] idolCell( Place p ){
		return new int[]{ p.cx, p.cy };
	}

	/** Where a place is first seen from: the middle of its grounds. */
	public static int[] seeCell( Place p ){
		return new int[]{ p.cx, p.cy };
	}

	// ------------------------------------------------------------ the tide

	/** A tide comes in and goes out twice a lunar day, 24 hours 50 minutes of the world's turns
	 *  (WorldClock.turn, DayNightCycle.FULL_CYCLE to a day): it turns a little later each day, so
	 *  its low water walks round the clock over a month. */
	public static final float TIDE_PERIOD = DayNightCycle.FULL_CYCLE * (24f + 50f / 60f) / 24f / 2f;

	/** The tide's height at a world turn, -1 (low water) to 1 (high water). */
	public static float tideLevel( long turn ){
		double t = turn / (double) TIDE_PERIOD;
		t -= Math.floor( t );
		return (float) Math.cos( 2 * Math.PI * t );
	}

	/** Is the tide out at this world turn: the pools lie open, their shells and pearls in them? */
	public static boolean tideOut( long turn ){
		return tideLevel( turn ) < 0f;
	}

	/** The world turns until the tide next turns (out to in, or in to out), at least 1. */
	public static int turnsToTurn( long turn ){
		boolean out = tideOut( turn );
		int t = 1;
		while (t < TIDE_PERIOD && tideOut( turn + t ) == out) t++;
		return t;
	}

	// ------------------------------------------------------------ the pools' haul

	/** What the pools hold at most, and how much the sea brings back a day. */
	public static final int POOL_CAP = 6, POOL_REFILL = 2;
	/** One find in this many is a pearl, the rest shells. */
	public static final int PEARL_ONE_IN = 9;
	/** A pearl oyster settles in a pool at most once in this many days. */
	public static final int OYSTER_DAYS = 3;

	/** What the pools hold on `today`, from what they held on `lastDay`: POOL_REFILL more a day,
	 *  never over POOL_CAP. A first visit finds them full. */
	public static int refilled( int stock, int lastDay, int today ){
		if (lastDay == Integer.MIN_VALUE) return POOL_CAP;
		long days = Math.max( 0L, (long) today - lastDay );
		return (int) Math.min( POOL_CAP, stock + days * POOL_REFILL );
	}

	// ------------------------------------------------------------ the shrine

	/** The shrine takes an offering once in this many world days. */
	public static final int SHRINE_DAYS = 3;
	/** The blessing's length, in turns: a fifth of a day for gold, more for a finer gift. */
	public static final float BLESS_TURNS = 500f, BLESS_MAX = 1000f;
	/** The least gold the water takes, and the least an item must be worth to take its place. */
	public static final int OFFER_GOLD = 120, OFFER_WORTH = 20;

	/** The days until the shrine takes another offering after one on `lastDay`, 0 when it would now. */
	public static int shrineWait( int lastDay, int today ){
		if (lastDay == Integer.MIN_VALUE) return 0;
		return (int) Math.max( 0L, (long) lastDay + SHRINE_DAYS - today );
	}

	/** The blessing an item of this worth (Item.value) buys: BLESS_TURNS at OFFER_WORTH, more
	 *  for more, up to BLESS_MAX at ten times it. */
	public static float blessTurns( int worth ){
		float f = Math.max( 0f, Math.min( 1f, (worth - OFFER_WORTH) / (9f * OFFER_WORTH) ) );
		return BLESS_TURNS + f * (BLESS_MAX - BLESS_TURNS);
	}

	/** How many drowned acolytes rise to guard a shrine on a night: two, three one night in three. */
	public static int guardCount( long key, int night ){
		return Math.floorMod( hash( key ^ 0x6A2DL, night, 1 ), 3L ) == 0 ? 3 : 2;
	}

	// ------------------------------------------------------------ the mist and the bog

	/** How thick the swamp's mist lies at an hour, 0..1: a low haze all day, thickening through
	 *  the dusk into the night, thickest at dawn, burning off as the sun comes up. */
	public static float mistDensity( Phase phase, float progress ){
		float p = Math.max( 0f, Math.min( 1f, progress ) );
		switch (phase){
			case DAWN:  return 1f - 0.3f * smooth( (p - 0.5f) / 0.5f );
			case DAY:   return 0.7f - 0.35f * smooth( p / 0.15f ) + 0.05f * smooth( (p - 0.85f) / 0.15f );
			case DUSK:  return 0.4f + 0.35f * smooth( p );
			default:    return 0.75f + 0.25f * smooth( (p - 0.6f) / 0.4f );
		}
	}

	private static float smooth( float t ){
		t = Math.max( 0f, Math.min( 1f, t ) );
		return t * t * (3f - 2f * t);
	}

	/** The mist that counts as fog (ClimateManager.isFoggy): the night's and the dawn's. */
	public static final float THICK = 0.7f;

	/** How many cells the swamp's mist takes off the sight of someone standing in it: 1 by day,
	 *  2 at dusk and by night, 3 in the dawn's thickest. */
	public static int mistSight( float density ){
		return density <= 0f ? 0 : Math.max( 1, Math.round( 3f * density ) );
	}

	/** How much slower a step into the bog's water goes (Hero.speed): a quarter. */
	public static final float BOG_SPEED = 0.75f;

	/** Hero.speed's share for the bog: BOG_SPEED for a walker standing in a swamp's water on the
	 *  surface, unless the shrine's blessing (BogBlessing) or a wading balm (Wading) is on him; 1
	 *  anywhere else, and for a flier. */
	public static float bogFactor( xyz.gabriwar.warpedpixeldungeon.actors.Char ch ){
		xyz.gabriwar.warpedpixeldungeon.levels.Level level = xyz.gabriwar.warpedpixeldungeon.Dungeon.level;
		if (!(level instanceof OverworldLevel) || ch == null || ch.flying) return 1f;
		OverworldLevel ow = (OverworldLevel) level;
		if (ow.altitude() != 0 || ch.pos < 0 || ch.pos >= ow.length()) return 1f;
		if (ow.map[ch.pos] != Terrain.WATER || !ow.swampAt( ch.pos )) return 1f;
		if (ch.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.BogBlessing.class ) != null
				|| ch.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Wading.class ) != null) return 1f;
		return BOG_SPEED;
	}

	/**
	 * What the swamp's mist takes off the sight of someone standing in it (Level.updateFieldOfView),
	 * on top of what the day's light and the weather took (DayNightCycle.viewDistanceModifier, whose
	 * fog may already be this very mist: ClimateManager.isFoggy, then its two cells are not taken
	 * twice). A hero carrying a bog lantern's light (MarshLight) sees through it: what the local
	 * mist took as fog is given back.
	 */
	public static int mistPenalty( xyz.gabriwar.warpedpixeldungeon.levels.Level level, xyz.gabriwar.warpedpixeldungeon.actors.Char ch ){
		if (!(level instanceof OverworldLevel) || ch == null) return 0;
		OverworldLevel ow = (OverworldLevel) level;
		if (ow.altitude() != 0 || !ow.swampAt( ch.pos )) return 0;
		int fog = xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.isFoggy() ? 2 : 0;
		if (ch.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.MarshLight.class ) != null){
			return xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager.fogIsLocal() ? -fog : 0;
		}
		int mist = mistSight( mistDensity( DayNightCycle.phase(), DayNightCycle.phaseProgress() ) );
		return Math.max( 0, mist - fog );
	}

	/** A place's dot on the world map (WorldChart) and the colour of its name there: the witch's
	 *  hut a witch's purple, the sunken shrine the bog's moss, the tide pools the shallows' blue. */
	public static int dot( WorldStructures.Site kind ){
		switch (kind){
			case WITCH_HUT:     return 0xA678D8;
			case SUNKEN_SHRINE: return 0x86B05A;
			default:            return 0x7CC8EC;
		}
	}

	/** A place's name, as the world map and the HUD give it. */
	public static String mapName( WorldStructures.Site kind ){
		return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( WetlandSites.class, "map_" + kind.name().toLowerCase() );
	}
}

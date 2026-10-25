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

import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Paths;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The magic map's march, planned: a route in world cells from the hero to the waypoint, worked out
 * once and then followed, instead of a fresh greedy leg toward the waypoint's bearing every turn
 * (which paced from side to side in front of anything concave: a lake, a bay, a ridge, a wood).
 *
 * The search is A* (octile costs, an octile heuristic weighted to a typical step: HEURISTIC) over
 * blocks of B x B world cells. A block whose
 * ground is known exactly - the live window's, and every window the route was planned in before -
 * is split into its walkable components (8-connected, as the hero steps), so a fence, a wall or a
 * river running through a block cuts it as it cuts the ground; each component is a node, joined to
 * the components of the next block that touch it. Every other block is read from the untouched
 * world (Ground): four cells on a 2-cell lattice, one node, passable unless two of the four are a
 * barrier (deep water, rock, a drop, a fence) - a road or a bridge through it always lets it pass.
 * Trees, boulders and shrubs are scattered, not barriers: they make a block dearer, not shut.
 *
 * Costs are the hero's time: roads, paths and bridges a tenth quicker (Paths.SPEED_BONUS), open
 * ground 1, shallow water and tall grass DEAR, and every block dearer for the share of it that is
 * solid (the way winds round it). The search is bounded (a region round the hero and the waypoint)
 * and budgeted (node expansions); out of budget it hands back the way to the node nearest the
 * waypoint so far (PARTIAL), and with nothing left to expand, the nearest reachable point (NEAREST).
 *
 * Plans run on one worker thread ("ow-march"); the sampled world is kept by chunk, per seed, slice
 * and season. Following a route (OverworldLevel.marchDestination) is advance() and leg().
 */
public final class MarchPlanner {

	private MarchPlanner(){}

	// --------------------------------------------------------------- who walks

	/**
	 * What a marcher can stand on and how dear each step is. The land walker is the hero on foot; a
	 * walker aboard a boat would answer for open water instead (DEEP_WATER passable, the land not)
	 * and hand that to the same planner.
	 */
	public interface Walker {

		float BLOCKED = 0f;

		/** The time a step onto this terrain takes (1 open ground), BLOCKED where this walker cannot stand. */
		float cost( int terrain );

		/** ...on a cell of a live window, where more than the terrain can shut a cell (a web, a barred door). */
		default float costOn( Level level, int cell ){
			return level.passable[cell] ? cost( level.map[cell] ) : BLOCKED;
		}

		/** Is this terrain scattered clutter (a tree, a boulder) rather than a barrier, at the coarse grid's scale? */
		default boolean scatter( int terrain ){
			switch (terrain){
				case Terrain.TREE_PINE: case Terrain.TREE_OAK: case Terrain.BOULDER:
				case Terrain.SHRUB: case Terrain.MINE_BOULDER:
					return true;
				default:
					return false;
			}
		}
	}

	/** A road, a path or a bridge: a tenth quicker going. */
	public static final float ROAD = 1f / Paths.SPEED_BONUS;
	/** Shallow water, tall grass and crops: slower to wade through and blind, avoided where it costs little. */
	public static final float DEAR = 1.3f;
	//how much dearer a block is for the share of it that is solid
	static final float CLUTTER = 0.6f;

	/** The hero on foot: anything passable, the roads quicker, water and tall grass a little dearer. */
	public static final Walker LAND = terrain -> {
		int f = Terrain.flags[terrain];
		if ((f & Terrain.PASSABLE) == 0) return Walker.BLOCKED;
		if (Paths.isPath( terrain )) return ROAD;
		if ((f & Terrain.LIQUID) != 0 || ((f & Terrain.LOS_BLOCKING) != 0 && (f & Terrain.SOLID) == 0)) return DEAR;
		return 1f;
	};

	// --------------------------------------------------------------- the grid

	/**
	 * Cells per side of a block. Four: a river's deep core runs one to five cells wide and a fence or
	 * a house wall one, so a barrier cuts a block rather than swallowing it whole, and a 4x4 block has
	 * at most four components (one per quadrant). A 600-cell walk is 150 blocks.
	 */
	public static final int B = 4;

	//a block's word: its walkable cells (bit x + 4y), the road and the dear ones among them; for a block
	//read from the untouched world, all or none of its cells, its cost (x32) and the cell it stands for
	static final long WALK = 0xFFFFL;
	static final int ROAD_SHIFT = 16, DEAR_SHIFT = 32, COST_SHIFT = 48, MIDDLE_SHIFT = 56;
	static final long PRISTINE = 1L << 62;

	public static long blockKey( int bx, int by ){
		return ((long)bx << 32) | (by & 0xFFFFFFFFL);
	}

	public static int keyX( long key ){
		return (int)(key >> 32);
	}

	public static int keyY( long key ){
		return (int)key;
	}

	/** The exact word of a block from its walker's reading of each cell (cost of cell x + 4y). */
	static long realWord( float[] costs ){
		long walk = 0, road = 0, dear = 0;
		for (int i = 0; i < B * B; i++){
			float c = costs[i];
			if (c <= 0) continue;
			walk |= 1L << i;
			if (c < 1f) road |= 1L << i;
			else if (c > 1f) dear |= 1L << i;
		}
		return walk | road << ROAD_SHIFT | dear << DEAR_SHIFT;
	}

	/**
	 * The exact blocks of a live window - those whole inside its ring of rock - read cell by cell by
	 * the walker, into `into` (each replacing what was known of it). Actor thread: reads the level.
	 */
	public static void readWindow( Level level, int worldX, int worldY, Walker walker, LongLongMap into ){
		int w = level.width(), h = level.height();
		int bx0 = Math.floorDiv( worldX + 1 + B - 1, B ), bx1 = Math.floorDiv( worldX + w - 1, B ) - 1;
		int by0 = Math.floorDiv( worldY + 1 + B - 1, B ), by1 = Math.floorDiv( worldY + h - 1, B ) - 1;
		float[] costs = new float[B * B];
		for (int by = by0; by <= by1; by++){
			for (int bx = bx0; bx <= bx1; bx++){
				int x0 = bx * B - worldX, y0 = by * B - worldY;
				for (int i = 0; i < B * B; i++){
					costs[i] = walker.costOn( level, (x0 + i % B) + (y0 + i / B) * w );
				}
				into.put( blockKey( bx, by ), realWord( costs ) );
			}
		}
	}

	/** Takes a cell of a block known exactly out of its walkable ones: someone stands in it. */
	public static void shutCell( LongLongMap ground, int wx, int wy ){
		int bx = Math.floorDiv( wx, B ), by = Math.floorDiv( wy, B );
		long k = blockKey( bx, by ), w = ground.get( k, -1 );
		if (w == -1 || (w & PRISTINE) != 0) return;
		long bit = 1L << ((wx - bx * B) + (wy - by * B) * B);
		ground.put( k, w & ~(bit | bit << ROAD_SHIFT | bit << DEAR_SHIFT) );
	}

	// --------------------------------------------------------------- components

	//for every set of a block's cells: its 8-connected components, four 16-bit lanes, ordered by their
	//lowest cell; and the cell of a set nearest the block's middle (ties to the lower index)
	private static final class Tables {
		static final long[] COMPS = new long[1 << 16];
		static final byte[] MIDDLE = new byte[1 << 16];
		static {
			int[] stack = new int[16];
			for (int m = 1; m < (1 << 16); m++){
				int left = m, lane = 0;
				long lanes = 0;
				while (left != 0){
					int seed = Integer.numberOfTrailingZeros( left );
					int comp = 1 << seed, sp = 0;
					stack[sp++] = seed;
					while (sp > 0){
						int c = stack[--sp], cx = c % B, cy = c / B;
						for (int dy = -1; dy <= 1; dy++){
							for (int dx = -1; dx <= 1; dx++){
								int x = cx + dx, y = cy + dy;
								if (x < 0 || y < 0 || x >= B || y >= B) continue;
								int j = x + y * B;
								if ((m >> j & 1) != 0 && (comp >> j & 1) == 0){
									comp |= 1 << j;
									stack[sp++] = j;
								}
							}
						}
					}
					lanes |= (long)comp << (16 * lane++);
					left &= ~comp;
				}
				COMPS[m] = lanes;
				int best = -1;
				float bestD = Float.MAX_VALUE;
				for (int i = 0; i < 16; i++){
					if ((m >> i & 1) == 0) continue;
					float dx = i % B - 1.5f, dy = i / B - 1.5f, d = dx * dx + dy * dy;
					if (d < bestD){
						bestD = d;
						best = i;
					}
				}
				MIDDLE[m] = (byte)best;
			}
		}
	}

	/** The cells of component c of a block's word (a pristine block is one component, all of it). */
	static int compCells( long word, int c ){
		if ((word & PRISTINE) != 0) return c == 0 && (word & WALK) != 0 ? 0xFFFF : 0;
		return (int)(Tables.COMPS[(int)(word & WALK)] >>> (16 * c)) & 0xFFFF;
	}

	static int compCount( long word ){
		if ((word & PRISTINE) != 0) return (word & WALK) != 0 ? 1 : 0;
		long lanes = Tables.COMPS[(int)(word & WALK)];
		int n = 0;
		while (n < 4 && ((lanes >>> (16 * n)) & 0xFFFF) != 0) n++;
		return n;
	}

	/** The component of a block's word holding cell i, -1 when the cell is not walkable. */
	static int compOf( long word, int i ){
		if ((word & WALK) == 0 || (word >> i & 1) == 0) return -1;
		if ((word & PRISTINE) != 0) return 0;
		for (int c = 0; c < 4; c++){
			if ((compCells( word, c ) >> i & 1) != 0) return c;
		}
		return -1;
	}

	/** The cell (x + 4y) a component stands for, a point of a route through it: its cell nearest the
	 *  block's middle, on the road where a road runs through it (the hero walks the road, point to point). */
	static int compMiddle( long word, int c ){
		if ((word & PRISTINE) != 0) return (int)(word >>> MIDDLE_SHIFT) & 0xF;
		int cells = compCells( word, c );
		int road = cells & (int)(word >>> ROAD_SHIFT);
		return Tables.MIDDLE[Integer.bitCount( road ) >= B ? road : cells];
	}

	/** The time a step into a component takes: its cells' own - a road's where a road runs through it,
	 *  which is walked along - dearer for the solid share of the block. */
	static float compCost( long word, int c ){
		if ((word & PRISTINE) != 0) return ((word >>> COST_SHIFT) & 0xFF) / 32f;
		int cells = compCells( word, c );
		int n = Integer.bitCount( cells );
		int road = Integer.bitCount( cells & (int)(word >>> ROAD_SHIFT) );
		int dear = Integer.bitCount( cells & (int)(word >>> DEAR_SHIFT) );
		float base = road >= B ? ROAD : (road * ROAD + dear * DEAR + (n - road - dear)) / n;
		int solid = B * B - Long.bitCount( word & WALK );
		return base * (1f + CLUTTER * solid / (B * B));
	}

	/** The cell of a set (x + 4y) nearest a point given in block cells. */
	static int nearestCell( int cells, int x, int y ){
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int i = 0; i < B * B; i++){
			if ((cells >> i & 1) == 0) continue;
			int d = octile8( Math.abs( i % B - x ), Math.abs( i / B - y ) );
			if (d < bestD){
				bestD = d;
				best = i;
			}
		}
		return best;
	}

	// --------------------------------------------------------------- the untouched world

	/**
	 * The untouched world under the coarse grid: the terrain of the cells of a 2-cell lattice (four per
	 * block), sampled a chunk of CHUNK x CHUNK blocks at a time and kept. Open water two tiers deep or
	 * more comes as DEEP_WATER; the shelf (the first water off any shore is wadeable,
	 * WindowGenerator.tierWater) is worked out here, on the lattice.
	 */
	public abstract static class Ground {

		static final int CHUNK = 8;
		static final int CELLS = CHUNK * B;
		static final int LAT = CELLS / 2;
		//the chunks kept: about the world chart's span (2048 cells a side) of 32-cell chunks
		private static final int MAX_CHUNKS = 4096;

		private final Map<Long, short[]> chunks = new LinkedHashMap<Long, short[]>( 256, 0.75f, true ){
			@Override
			protected boolean removeEldestEntry( Map.Entry<Long, short[]> eldest ){
				return size() > MAX_CHUNKS;
			}
		};
		private int sampled = 0;

		/** The untouched terrain of a world cell (DEEP_WATER for water two tiers deep or more). */
		protected abstract int terrain( int wx, int wy, Object scratch );

		/** What one chunk's samples share (a sector view), for the world rect it reads. */
		protected Object scratch( int wx0, int wy0, int wx1, int wy1 ){
			return null;
		}

		/** How many chunks have been sampled so far. */
		public synchronized int sampled(){
			return sampled;
		}

		//the lattice of a chunk: LAT x LAT terrains, sample (i, j) at cell (1 + 2i, 1 + 2j) of the chunk
		synchronized short[] chunk( int cx, int cy ){
			Long key = blockKey( cx, cy );
			short[] c = chunks.get( key );
			if (c == null){
				c = sample( cx, cy );
				chunks.put( key, c );
				sampled++;
			}
			return c;
		}

		private short[] sample( int cx, int cy ){
			int x0 = cx * CELLS, y0 = cy * CELLS;
			//a ring of samples round the chunk, for the shelf
			int n = LAT + 2;
			Object scratch = scratch( x0 - 1, y0 - 1, x0 + CELLS + 1, y0 + CELLS + 1 );
			int[] raw = new int[n * n];
			for (int j = 0; j < n; j++){
				for (int i = 0; i < n; i++){
					raw[i + j * n] = terrain( x0 - 1 + 2 * i, y0 - 1 + 2 * j, scratch );
				}
			}
			short[] out = new short[LAT * LAT];
			for (int j = 1; j <= LAT; j++){
				for (int i = 1; i <= LAT; i++){
					int t = raw[i + j * n];
					if (t == Terrain.DEEP_WATER){
						//deep only where the lattice round it is all water: a sample beside the shore is the shelf
						check:
						for (int dy = -1; dy <= 1; dy++){
							for (int dx = -1; dx <= 1; dx++){
								int o = raw[(i + dx) + (j + dy) * n];
								if (o != Terrain.WATER && o != Terrain.DEEP_WATER){
									t = Terrain.WATER;
									break check;
								}
							}
						}
					}
					out[(i - 1) + (j - 1) * LAT] = (short)t;
				}
			}
			return out;
		}

		/** A block's word as this walker reads the untouched world there. */
		long word( int bx, int by, Walker walker ){
			short[] c = chunk( Math.floorDiv( bx, CHUNK ), Math.floorDiv( by, CHUNK ) );
			int lx = Math.floorMod( bx, CHUNK ) * 2, ly = Math.floorMod( by, CHUNK ) * 2;
			int walk = 0, road = 0, scatter = 0, hard = 0;
			//the sample the block stands for: a road's, else open ground's (cell x + 4y of the block)
			int middle = -1, open = -1;
			float sum = 0;
			for (int dy = 0; dy < 2; dy++){
				for (int dx = 0; dx < 2; dx++){
					int t = c[(lx + dx) + (ly + dy) * LAT];
					int cell = (1 + 2 * dx) + (1 + 2 * dy) * B;
					float cost = walker.cost( t );
					if (cost > 0){
						walk++;
						sum += cost;
						if (open == -1) open = cell;
						if (cost < 1f){
							road++;
							if (middle == -1) middle = cell;
						}
					} else if (walker.scatter( t )){
						scatter++;
					} else {
						hard++;
					}
				}
			}
			if (middle == -1) middle = open != -1 ? open : 1 + B;
			//two barrier samples of four shut a block (a river two lattice cells across, a shore); a road
			//through it keeps it open (a bridge)
			if (hard >= 2 && road == 0) return PRISTINE;
			float base = road > 0 ? ROAD : walk > 0 ? sum / walk : 1f;
			float cost = base * (1f + CLUTTER * (scatter + hard) / 4f);
			return PRISTINE | WALK | (long)Math.min( 255, Math.round( cost * 32 ) ) << COST_SHIFT
					| (long)middle << MIDDLE_SHIFT;
		}
	}

	/** The world's own ground, slice by slice: what WindowGenerator lays before any edit. */
	static final class WorldGround extends Ground {

		final long seed;
		final int altitude;
		final float shift;

		WorldGround( long seed, int altitude, float shift ){
			this.seed = seed;
			this.altitude = altitude;
			this.shift = shift;
		}

		private static final class Scratch {
			final WorldModel.Sample smp = new WorldModel.Sample();
			final WorldStructures.SectorView view;
			Scratch( WorldStructures.SectorView view ){
				this.view = view;
			}
		}

		@Override
		protected Object scratch( int wx0, int wy0, int wx1, int wy1 ){
			return new Scratch( altitude == 0 ? WorldStructures.view( seed, wx0, wy0, wx1, wy1 ) : null );
		}

		//the cell as the generator's row pass makes it (WindowGenerator.surface, mountain, caves),
		//without the places laid on the slices and the ways between them: one cell each, not barriers
		@Override
		protected int terrain( int wx, int wy, Object scratch ){
			Scratch s = (Scratch)scratch;
			if (altitude == 0){
				WorldModel.sample( seed, wx, wy, shift, s.smp );
				int wild = WorldModel.wildTerrain( seed, wx, wy, s.smp );
				int structure = WorldStructures.terrainAt( seed, wx, wy, wild, s.view );
				int t = structure != -1 ? structure : wild;
				if (structure == -1 && t == Terrain.WALL && WorldModel.tunnelAt( seed, wx, wy, 0 )) return Terrain.DIRT_PATH;
				if (t == Terrain.WATER && WorldModel.waterTier( s.smp.elev ) >= 2) return Terrain.DEEP_WATER;
				return t;
			} else if (altitude > 0){
				WorldModel.sample( seed, wx, wy, shift, s.smp );
				int b = WorldLayers.band( s.smp.elev );
				if (b < altitude) return Terrain.CHASM;
				if (b > altitude) return WorldModel.tunnelAt( seed, wx, wy, altitude ) ? Terrain.DIRT_PATH : Terrain.WALL;
				int t = WorldModel.alpineTerrain( seed, wx, wy, altitude, s.smp );
				if (t == Terrain.WATER && WorldModel.tarnTier( seed, wx, wy, altitude ) >= 2) return Terrain.DEEP_WATER;
				return t;
			} else {
				int t = WindowGenerator.naturalCaveTerrain( seed, altitude, wx, wy );
				//a pool is deep where the slice below is water too
				if (t == Terrain.WATER && WorldLayers.exists( altitude - 1 )){
					int surfaceTier = -altitude < WorldModel.WATER_TIERS ? WindowGenerator.surfaceWaterTier( seed, wx, wy ) : 0;
					if (surfaceTier > 1 - altitude || WorldModel.caveWet( seed, wx, wy, altitude - 1 )) return Terrain.DEEP_WATER;
				}
				return t;
			}
		}
	}

	private static WorldGround world;

	/** The untouched ground of a slice, as of a seasonal shift: kept while the slice and the season hold. */
	public static synchronized Ground ground( long seed, int altitude, float shift ){
		if (world == null || world.seed != seed || world.altitude != altitude || world.shift != shift){
			world = new WorldGround( seed, altitude, shift );
		}
		return world;
	}

	// --------------------------------------------------------------- a plan

	/** How a plan ended. */
	public enum Status {
		/** The way to the waypoint (or as near it as its own block lets). */
		REACHED,
		/** Nothing left to search: the waypoint cannot be reached, this is the nearest point that can. */
		NEAREST,
		/** Out of budget: the way to the nearest point found so far. */
		PARTIAL,
		/** No way to start (cancelled, or the planner failed). */
		FAILED
	}

	/** A planned route: world cells, the hero's own first, a block or so apart. */
	public static final class Route {

		public final int[] xs, ys;
		//the octile length of the route up to each point
		public final float[] along;
		public final Status status;
		//does it end within a step of the waypoint
		public final boolean endsAtGoal;
		//what it took: nodes expanded, microseconds
		public final int expanded;
		public final long micros;

		Route( int[] xs, int[] ys, Status status, boolean endsAtGoal, int expanded, long micros ){
			this.xs = xs;
			this.ys = ys;
			this.status = status;
			this.endsAtGoal = endsAtGoal;
			this.expanded = expanded;
			this.micros = micros;
			along = new float[xs.length];
			for (int i = 1; i < xs.length; i++){
				along[i] = along[i - 1] + octile( Math.abs( xs[i] - xs[i - 1] ), Math.abs( ys[i] - ys[i - 1] ) );
			}
		}

		static Route failed( long micros ){
			return new Route( new int[0], new int[0], Status.FAILED, false, 0, micros );
		}

		public int size(){
			return xs.length;
		}

		public int last(){
			return xs.length - 1;
		}
	}

	/** Everything a plan reads, owned by it: the worker never touches the level. */
	public static final class Request {

		final Ground ground;
		final Walker walker;
		final int sx, sy, gx, gy;
		//the blocks known exactly (the live window's and those the route learned), and the ones it found shut
		final LongLongMap real, shut;
		final int budget;
		volatile boolean cancelled;

		public Request( Ground ground, Walker walker, int sx, int sy, int gx, int gy,
		                LongLongMap real, LongLongMap shut, int budget ){
			this.ground = ground;
			this.walker = walker;
			this.sx = sx;
			this.sy = sy;
			this.gx = gx;
			this.gy = gy;
			this.real = real;
			this.shut = shut;
			this.budget = budget;
		}
	}

	//a plan that does not reach the waypoint ends at the node nearest it, a cell nearer worth this many
	//cells of the way there: a twentieth, so it never walks round a lake to be one cell nearer
	static final float REACH = 0.05f;

	/** Node expansions a plan may take: a few tens of milliseconds where the world is sampled already. */
	public static final int BUDGET = 150_000;
	//the region a plan searches: the box round the hero and the waypoint and half their distance
	//round it, at least MIN_MARGIN blocks (room to go round a big lake on a short walk) and at most
	//MAX_MARGIN
	static final int MIN_MARGIN = 64, MAX_MARGIN = 128;
	//a plan still short of the waypoint after PROBE expansions looks from the waypoint's side: when its
	//piece of the world closes round within ENCLOSED nodes (an island, a walled-in valley) the hero is not
	//on it, the waypoint cannot be reached, and the nearest point found so far stands
	static final int PROBE = 20_000, ENCLOSED = 4096;
	//how far from an unwalkable waypoint's block the walkable block nearest it is looked for
	static final int RETARGET = 32;

	/**
	 * What the heuristic counts a cell of distance as: a typical step (open ground with some clutter),
	 * not the cheapest one (a road, ROAD). Counted at the cheapest the search spreads over an ellipse;
	 * counted at this it runs down a corridor. Measured on twelve 600-cell trips over a real world: a
	 * sixth of the expansions and of the time, and the routes no longer (5466 cells summed, against
	 * 5475). A route can come out longer than the best by at most this over ROAD, and is planned again
	 * at every window anyway.
	 */
	static final float HEURISTIC = 1.25f;

	private static final ThreadLocal<Search> SEARCH = ThreadLocal.withInitial( Search::new );

	/** Plans a route, on the calling thread. Never throws: a failure is a FAILED route. */
	public static Route plan( Request q ){
		long t0 = System.nanoTime();
		try {
			Route r = SEARCH.get().run( q, t0 );
			return r != null ? r : Route.failed( (System.nanoTime() - t0) / 1000 );
		} catch (Throwable e){
			System.out.println( "[march] plan failed: " + e );
			e.printStackTrace( System.out );
			return Route.failed( (System.nanoTime() - t0) / 1000 );
		}
	}

	// --------------------------------------------------------------- the search

	static final int[] DX = { 1, 1, 0, -1, -1, -1, 0, 1 };
	static final int[] DY = { 0, 1, 1, 1, 0, -1, -1, -1 };

	//the octile distance in cells, and an integer one for small offsets
	static float octile( int dx, int dy ){
		return Math.max( dx, dy ) + 0.41421356f * Math.min( dx, dy );
	}

	private static int octile8( int dx, int dy ){
		return 10 * Math.max( dx, dy ) + 4 * Math.min( dx, dy );
	}

	//the 4 cells (bit per position along it) a set holds on the side of a block facing direction d
	private static int side( int cells, int d ){
		int out = 0;
		for (int k = 0; k < B; k++){
			int i;
			switch (d){
				case 0:  i = (B - 1) + k * B; break;    //east: column 3
				case 4:  i = k * B; break;              //west: column 0
				case 2:  i = k + (B - 1) * B; break;    //south: row 3
				default: i = k; break;                  //north: row 0
			}
			out |= (cells >> i & 1) << k;
		}
		return out;
	}

	//the corner cell of a block facing diagonal direction d
	private static int corner( int d ){
		return (DX[d] > 0 ? B - 1 : 0) + (DY[d] > 0 ? B - 1 : 0) * B;
	}

	private static final class Search {

		//the nodes: key -> index by open addressing, stamped with the plan's generation
		private long[] tKey = new long[1 << 14];
		private int[] tGen = new int[1 << 14], tIdx = new int[1 << 14];
		private int tSize, gen;

		private int n;
		private long[] key = new long[1 << 12];
		private float[] g = new float[1 << 12];
		private int[] parent = new int[1 << 12], px = new int[1 << 12], py = new int[1 << 12];
		private boolean[] closed = new boolean[1 << 12];

		//the open set: a binary heap of (f, node)
		private int hn;
		private float[] hf = new float[1 << 12];
		private int[] hi = new int[1 << 12];

		//the blocks read by this plan
		private final LongLongMap words = new LongLongMap();

		private Request q;
		private int rx0, ry0, rx1, ry1;

		//the word of a block for this plan: exact where known, the untouched world elsewhere, shut where
		//the route found it so (and outside the region)
		private long word( int bx, int by ){
			if (bx < rx0 || by < ry0 || bx > rx1 || by > ry1) return 0;
			long k = blockKey( bx, by );
			long w = words.get( k, -1 );
			if (w == -1){
				if (q.shut.contains( k )) w = 0;
				else {
					w = q.real.get( k, -1 );
					if (w == -1) w = q.ground.word( bx, by, q.walker );
				}
				words.put( k, w );
			}
			return w;
		}

		private static long nodeKey( int bx, int by, int c ){
			return ((long)bx << 34) | ((by & 0xFFFFFFFFL) << 2) | c;
		}

		private static int mix( long k ){
			k ^= k >>> 33;
			k *= 0xFF51AFD7ED558CCDL;
			k ^= k >>> 33;
			return (int)k;
		}

		private int find( long k ){
			int m = tKey.length - 1;
			for (int i = mix( k ) & m; tGen[i] == gen; i = (i + 1) & m){
				if (tKey[i] == k) return tIdx[i];
			}
			return -1;
		}

		private void index( long k, int idx ){
			if (tSize * 2 >= tKey.length) grow();
			int m = tKey.length - 1, i = mix( k ) & m;
			while (tGen[i] == gen) i = (i + 1) & m;
			tKey[i] = k;
			tGen[i] = gen;
			tIdx[i] = idx;
			tSize++;
		}

		private void grow(){
			long[] ok = tKey;
			int[] og = tGen, oi = tIdx;
			tKey = new long[ok.length * 2];
			tGen = new int[ok.length * 2];
			tIdx = new int[ok.length * 2];
			int m = tKey.length - 1;
			for (int j = 0; j < ok.length; j++){
				if (og[j] != gen) continue;
				int i = mix( ok[j] ) & m;
				while (tGen[i] == gen) i = (i + 1) & m;
				tKey[i] = ok[j];
				tGen[i] = gen;
				tIdx[i] = oi[j];
			}
		}

		private int add( long k, int x, int y ){
			if (n == key.length){
				int c = n * 2;
				key = java.util.Arrays.copyOf( key, c );
				g = java.util.Arrays.copyOf( g, c );
				parent = java.util.Arrays.copyOf( parent, c );
				px = java.util.Arrays.copyOf( px, c );
				py = java.util.Arrays.copyOf( py, c );
				closed = java.util.Arrays.copyOf( closed, c );
			}
			key[n] = k;
			px[n] = x;
			py[n] = y;
			closed[n] = false;
			index( k, n );
			return n++;
		}

		private void push( int node, float f ){
			if (hn == hf.length){
				hf = java.util.Arrays.copyOf( hf, hn * 2 );
				hi = java.util.Arrays.copyOf( hi, hn * 2 );
			}
			int i = hn++;
			while (i > 0){
				int p = (i - 1) >> 1;
				if (hf[p] <= f) break;
				hf[i] = hf[p];
				hi[i] = hi[p];
				i = p;
			}
			hf[i] = f;
			hi[i] = node;
		}

		private int pop(){
			int top = hi[0];
			float f = hf[--hn];
			int node = hi[hn], i = 0;
			while (true){
				int c = 2 * i + 1;
				if (c >= hn) break;
				if (c + 1 < hn && hf[c + 1] < hf[c]) c++;
				if (hf[c] >= f) break;
				hf[i] = hf[c];
				hi[i] = hi[c];
				i = c;
			}
			hf[i] = f;
			hi[i] = node;
			return top;
		}

		//how near a node's component comes to the waypoint: its nearest cell's octile distance
		private float nearness( int u ){
			long k = key[u];
			int bx = (int)(k >> 34), by = (int)(k << 30 >> 32);
			int cell = nearestCell( compCells( word( bx, by ), (int)(k & 3) ), q.gx - bx * B, q.gy - by * B );
			return octile( Math.abs( bx * B + cell % B - q.gx ), Math.abs( by * B + cell / B - q.gy ) );
		}

		private float h( int x, int y ){
			return octile( Math.abs( x - q.gx ), Math.abs( y - q.gy ) ) * HEURISTIC;
		}

		private void relax( int u, int bx, int by, int c, long w ){
			long k = nodeKey( bx, by, c );
			int v = find( k );
			int x, y;
			if (v == -1){
				int mid = compMiddle( w, c );
				x = bx * B + mid % B;
				y = by * B + mid / B;
			} else {
				if (closed[v]) return;
				x = px[v];
				y = py[v];
			}
			float ng = g[u] + octile( Math.abs( x - px[u] ), Math.abs( y - py[u] ) ) * compCost( w, c );
			if (v == -1){
				v = add( k, x, y );
			} else if (ng >= g[v]){
				return;
			}
			g[v] = ng;
			parent[v] = u;
			push( v, ng + h( x, y ) );
		}

		//forward, a node's neighbours are relaxed; reversed (enclosed), they are counted as reached
		private boolean reverse;
		private int expanding;
		private java.util.HashSet<Long> reached;
		private long[] queue;
		private int queued;

		private void expand( int u ){
			expanding = u;
			long k = key[u];
			neighbours( (int)(k >> 34), (int)(k << 30 >> 32), (int)(k & 3) );
		}

		private void joined( int bx, int by, int c, long w ){
			if (!reverse){
				relax( expanding, bx, by, c, w );
			} else if (reached.add( nodeKey( bx, by, c ) )){
				if (queued == queue.length) queue = java.util.Arrays.copyOf( queue, queued * 2 );
				queue[queued++] = nodeKey( bx, by, c );
			}
		}

		//does the piece of the world round component c of block (bx, by) close within `limit` nodes?
		private boolean enclosed( int bx, int by, int c, int limit ){
			reverse = true;
			reached = new java.util.HashSet<>();
			queue = new long[256];
			queued = 0;
			try {
				joined( bx, by, c, 0 );
				for (int i = 0; i < queued; i++){
					if (queued > limit) return false;
					long k = queue[i];
					neighbours( (int)(k >> 34), (int)(k << 30 >> 32), (int)(k & 3) );
				}
				return true;
			} finally {
				reverse = false;
				reached = null;
				queue = null;
			}
		}

		//every node (block, component) joined to component c of block (bx, by)
		private void neighbours( int bx, int by, int c ){
			long wu = word( bx, by );
			int cells = compCells( wu, c );
			boolean pu = (wu & PRISTINE) != 0;
			for (int d = 0; d < 8; d++){
				int nbx = bx + DX[d], nby = by + DY[d];
				long wv = word( nbx, nby );
				int walk = (int)(wv & WALK);
				if (walk == 0) continue;
				boolean pv = (wv & PRISTINE) != 0;
				if (DX[d] != 0 && DY[d] != 0){
					//a diagonal step between blocks is a step between their corner cells; read from the
					//untouched world it must not cut between two shut blocks
					if ((pu || pv) && ((word( bx + DX[d], by ) & WALK) == 0 || (word( bx, by + DY[d] ) & WALK) == 0)) continue;
					if ((cells >> corner( d ) & 1) == 0) continue;
					int opposite = corner( (d + 4) & 7 );
					if ((walk >> opposite & 1) == 0) continue;
					joined( nbx, nby, compOf( wv, opposite ), wv );
				} else {
					int mine = side( cells, d );
					if (mine == 0) continue;
					int reach = (mine | mine << 1 | mine >> 1) & 0xF;
					int count = compCount( wv );
					for (int cv = 0; cv < count; cv++){
						if ((side( compCells( wv, cv ), (d + 4) & 7 ) & reach) != 0) joined( nbx, nby, cv, wv );
					}
				}
			}
		}

		Route run( Request req, long t0 ){
			q = req;
			gen++;
			if (gen == 0){
				java.util.Arrays.fill( tGen, 0 );
				gen = 1;
			}
			tSize = 0;
			n = 0;
			hn = 0;
			words.clear();

			int sbx = Math.floorDiv( q.sx, B ), sby = Math.floorDiv( q.sy, B );
			int gbx = Math.floorDiv( q.gx, B ), gby = Math.floorDiv( q.gy, B );
			int margin = Math.max( MIN_MARGIN, Math.min( MAX_MARGIN,
					Math.max( Math.abs( gbx - sbx ), Math.abs( gby - sby ) ) / 2 ) );
			rx0 = Math.min( sbx, gbx ) - margin;
			ry0 = Math.min( sby, gby ) - margin;
			rx1 = Math.max( sbx, gbx ) + margin;
			ry1 = Math.max( sby, gby ) + margin;

			//the hero stands where he stands: his cell counts as ground whatever it is
			int si = (q.sx - sbx * B) + (q.sy - sby * B) * B;
			long sw = word( sbx, sby );
			if ((sw & PRISTINE) == 0){
				sw |= 1L << si;
				words.put( blockKey( sbx, sby ), sw );
			} else if ((sw & WALK) == 0){
				sw = PRISTINE | WALK | (32L << COST_SHIFT);
				words.put( blockKey( sbx, sby ), sw );
			}
			int sc = compOf( sw, si );
			if (sc < 0) return null;

			//the goal: the component of the waypoint's block holding it (or nearest it); a waypoint whose
			//block is all barrier is gone for by the walkable block nearest it
			int tbx = gbx, tby = gby, tc = -1;
			long gw = word( gbx, gby );
			if ((gw & WALK) == 0){
				float best = Float.MAX_VALUE;
				for (int r = 1; r <= RETARGET && best == Float.MAX_VALUE; r++){
					for (int dy = -r; dy <= r; dy++){
						for (int dx = -r; dx <= r; dx++){
							if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
							long w = word( gbx + dx, gby + dy );
							if ((w & WALK) == 0) continue;
							float d = octile( Math.abs( dx ), Math.abs( dy ) );
							if (d < best){
								best = d;
								tbx = gbx + dx;
								tby = gby + dy;
							}
						}
					}
				}
				gw = word( tbx, tby );
			}
			if ((gw & WALK) != 0){
				int gi = Math.max( 0, Math.min( B - 1, q.gx - tbx * B ) ) + Math.max( 0, Math.min( B - 1, q.gy - tby * B ) ) * B;
				tc = compOf( gw, gi );
				if (tc < 0){
					int best = Integer.MAX_VALUE;
					for (int c = 0; c < compCount( gw ); c++){
						int cell = nearestCell( compCells( gw, c ), gi % B, gi / B );
						int d = octile8( Math.abs( cell % B - gi % B ), Math.abs( cell / B - gi / B ) );
						if (d < best){
							best = d;
							tc = c;
						}
					}
				}
			}

			int start = add( nodeKey( sbx, sby, sc ), q.sx, q.sy );
			g[start] = 0;
			parent[start] = -1;
			push( start, h( q.sx, q.sy ) );

			//the node nearest the waypoint so far, for a plan that does not get there: by the cell of its
			//component nearest the waypoint, a near one dear to reach giving way to one a little further
			int found = -1, best = start, expanded = 0;
			float bestNear = Float.MAX_VALUE;
			Status status = Status.NEAREST;
			while (hn > 0){
				int u = pop();
				if (closed[u]) continue;
				closed[u] = true;
				expanded++;
				float near = nearness( u ) + REACH * g[u];
				if (near < bestNear){
					bestNear = near;
					best = u;
				}
				long k = key[u];
				if (tc >= 0 && (int)(k >> 34) == tbx && (int)(k << 30 >> 32) == tby && (int)(k & 3) == tc){
					found = u;
					status = Status.REACHED;
					break;
				}
				if (expanded >= q.budget){
					status = Status.PARTIAL;
					break;
				}
				if (expanded == PROBE && tc >= 0 && enclosed( tbx, tby, tc, ENCLOSED )) break;
				if ((expanded & 1023) == 0 && q.cancelled) return null;
				expand( u );
			}
			int end = found != -1 ? found : best;

			int len = 0;
			for (int v = end; v != -1; v = parent[v]) len++;
			int[] xs = new int[len + 1], ys = new int[len + 1];
			int i = len;
			for (int v = end; v != -1; v = parent[v]){
				i--;
				xs[i] = px[v];
				ys[i] = py[v];
			}
			//the last point: the cell of the end's component nearest the waypoint, exactly where the
			//block is known, the waypoint itself where it is not
			long ek = key[end];
			int ebx = (int)(ek >> 34), eby = (int)(ek << 30 >> 32), ec = (int)(ek & 3);
			long ew = word( ebx, eby );
			int lx, ly;
			if ((ew & PRISTINE) != 0 && status == Status.REACHED && ebx == gbx && eby == gby){
				lx = q.gx;
				ly = q.gy;
			} else {
				int cell = nearestCell( compCells( ew, ec ), q.gx - ebx * B, q.gy - eby * B );
				lx = ebx * B + cell % B;
				ly = eby * B + cell / B;
			}
			int count = len;
			if (lx != xs[len - 1] || ly != ys[len - 1]){
				xs[len] = lx;
				ys[len] = ly;
				count = len + 1;
			}
			boolean atGoal = Math.max( Math.abs( xs[count - 1] - q.gx ), Math.abs( ys[count - 1] - q.gy ) ) <= 1;
			return new Route( java.util.Arrays.copyOf( xs, count ), java.util.Arrays.copyOf( ys, count ),
					status, atGoal, expanded, (System.nanoTime() - t0) / 1000 );
		}
	}

	// --------------------------------------------------------------- following

	//the next points of a route a walker's place on it is looked for among: a block or so each
	static final int NEXT = 16;
	//a segment further on is taken for his place only this much nearer for each cell of route it skips:
	//a route that doubles back round a spit of water passes close by itself
	static final float SKIP = 0.1f;

	/**
	 * Where along a route a walker at (hx, hy) is: the start of the segment (point i to i + 1) nearest
	 * him among the next few from `from`, never back before it. Walking his own way a few cells off the
	 * route he is still along it.
	 */
	public static int advance( Route r, int from, int hx, int hy ){
		int at = from;
		float best = Float.MAX_VALUE;
		int to = Math.min( r.last(), from + NEXT );
		for (int i = from; i <= to; i++){
			float d = offRoute( r, i, hx, hy ) + SKIP * (r.along[i] - r.along[from]);
			if (d < best){
				best = d;
				at = i;
			}
		}
		return at;
	}

	//how far (x, y) lies from the route's segment from point i to i + 1 (from point i, the last)
	private static float offRoute( Route r, int i, float x, float y ){
		float t = foot( r, i, x, y );
		float fx = i >= r.last() ? r.xs[i] : r.xs[i] + (r.xs[i + 1] - r.xs[i]) * t;
		float fy = i >= r.last() ? r.ys[i] : r.ys[i] + (r.ys[i + 1] - r.ys[i]) * t;
		return (float)Math.hypot( x - fx, y - fy );
	}

	//how far along the segment from point i to i + 1 the foot of (x, y) on it lies, 0..1
	private static float foot( Route r, int i, float x, float y ){
		if (i >= r.last()) return 0;
		float vx = r.xs[i + 1] - r.xs[i], vy = r.ys[i + 1] - r.ys[i], len2 = vx * vx + vy * vy;
		return len2 == 0 ? 0 : Math.max( 0, Math.min( 1, ((x - r.xs[i]) * vx + (y - r.ys[i]) * vy) / len2 ) );
	}

	/** The point of a route a walker along it at segment `at` makes for next, a step off him or more. */
	public static int toward( Route r, int at, int hx, int hy ){
		int next = Math.min( at + 1, r.last() );
		if (next < r.last() && Math.max( Math.abs( r.xs[next] - hx ), Math.abs( r.ys[next] - hy ) ) <= 1) next++;
		return next;
	}

	//the route points a leg is chosen from, past where a walker is along it
	static final int AHEAD = 96;
	//a leg may run this much longer than the route between its ends, plus a few cells
	static final float DETOUR = 1.5f;
	static final int DETOUR_SLACK = 6;

	/**
	 * The route point a leg runs to: the furthest one ahead of a walker at (hx, hy), along it at segment
	 * `from`, that he can walk to on his own distance map (dist, steps over a window of w x h at world
	 * origin (ox, oy), Integer.MAX_VALUE where he cannot) without going a long way round, never a point
	 * he stands on. -1 when there is none: the way ahead is cut.
	 */
	public static int leg( Route r, int from, int hx, int hy, int[] dist, int ox, int oy, int w, int h ){
		int best = -1;
		int to = Math.min( r.last(), from + AHEAD );
		float base = octile( Math.abs( r.xs[from] - hx ), Math.abs( r.ys[from] - hy ) );
		for (int i = Math.min( from + 1, r.last() ); i <= to; i++){
			int x = r.xs[i] - ox, y = r.ys[i] - oy;
			if (x < 1 || y < 1 || x >= w - 1 || y >= h - 1) continue;
			int d = dist[x + y * w];
			if (d <= 0 || d == Integer.MAX_VALUE) continue;
			if (d > DETOUR * (r.along[i] - r.along[from] + base) + DETOUR_SLACK) continue;
			best = i;
		}
		return best;
	}

	/** The first route point past `from` that the distance map does not reach: where the way ahead is cut. */
	public static int cut( Route r, int from, int[] dist, int ox, int oy, int w, int h ){
		int to = Math.min( r.last(), from + AHEAD );
		for (int i = from + 1; i <= to; i++){
			int x = r.xs[i] - ox, y = r.ys[i] - oy;
			if (x < 1 || y < 1 || x >= w - 1 || y >= h - 1) return i;
			int d = dist[x + y * w];
			if (d == Integer.MAX_VALUE) return i;
		}
		return Math.min( from + 1, r.last() );
	}

	// --------------------------------------------------------------- the worker

	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor( r -> {
		Thread t = new Thread( r, "ow-march" );
		t.setDaemon( true );
		return t;
	} );

	/** A plan in flight on the worker. */
	public static final class Job {

		final Request request;
		/** The march cannot go on until this plan is in (none to walk meanwhile). */
		public final boolean urgent;
		/** A plan from the end of a route that stopped short of the waypoint, to check the end. */
		public final boolean confirm;
		private final Future<Route> future;

		Job( Request request, boolean urgent, boolean confirm ){
			this.request = request;
			this.urgent = urgent;
			this.confirm = confirm;
			future = WORKER.submit( () -> {
				Route r = plan( request );
				//the console's word on each plan, as the window preparation's ([OW] window prep)
				System.out.println( "[OW] march plan " + r.status + ": " + r.size() + " points, "
						+ r.expanded + " nodes, " + r.micros / 1000 + "ms" );
				return r;
			} );
		}

		public boolean done(){
			return future.isDone();
		}

		/** The route if the plan is done, else null. */
		public Route poll(){
			return future.isDone() ? await( 0 ) : null;
		}

		/** Waits up to ms for the route; null if it is not in by then. */
		public Route await( long ms ){
			try {
				return future.get( ms, TimeUnit.MILLISECONDS );
			} catch (TimeoutException e){
				return null;
			} catch (InterruptedException e){
				Thread.currentThread().interrupt();
				return null;
			} catch (ExecutionException e){
				return Route.failed( 0 );
			}
		}

		public void cancel(){
			request.cancelled = true;
		}
	}

	/** Plans on the worker thread. */
	public static Job submit( Request request, boolean urgent, boolean confirm ){
		return new Job( request, urgent, confirm );
	}

	// --------------------------------------------------------------- a long -> long map

	/** A small open-addressing map of block keys (never Long.MIN_VALUE) to words. */
	public static final class LongLongMap {

		private static final long EMPTY = Long.MIN_VALUE;
		private long[] keys;
		private long[] vals;
		private int size;

		public LongLongMap(){
			this( 64 );
		}

		private LongLongMap( int capacity ){
			keys = new long[capacity];
			vals = new long[capacity];
			java.util.Arrays.fill( keys, EMPTY );
		}

		private static int mix( long k ){
			k ^= k >>> 31;
			k *= 0x9E3779B97F4A7C15L;
			k ^= k >>> 29;
			return (int)k;
		}

		public long get( long k, long missing ){
			int m = keys.length - 1;
			for (int i = mix( k ) & m; keys[i] != EMPTY; i = (i + 1) & m){
				if (keys[i] == k) return vals[i];
			}
			return missing;
		}

		public boolean contains( long k ){
			int m = keys.length - 1;
			for (int i = mix( k ) & m; keys[i] != EMPTY; i = (i + 1) & m){
				if (keys[i] == k) return true;
			}
			return false;
		}

		public void put( long k, long v ){
			if ((size + 1) * 2 > keys.length) rehash( keys.length * 2 );
			int m = keys.length - 1, i = mix( k ) & m;
			while (keys[i] != EMPTY){
				if (keys[i] == k){
					vals[i] = v;
					return;
				}
				i = (i + 1) & m;
			}
			keys[i] = k;
			vals[i] = v;
			size++;
		}

		private void rehash( int capacity ){
			long[] ok = keys, ov = vals;
			keys = new long[capacity];
			vals = new long[capacity];
			java.util.Arrays.fill( keys, EMPTY );
			size = 0;
			for (int i = 0; i < ok.length; i++){
				if (ok[i] != EMPTY) put( ok[i], ov[i] );
			}
		}

		public void clear(){
			if (size == 0) return;
			java.util.Arrays.fill( keys, EMPTY );
			size = 0;
		}

		public int size(){
			return size;
		}

		/** The keys held, in no particular order. */
		public long[] keys(){
			long[] out = new long[size];
			int n = 0;
			for (long k : keys) if (k != EMPTY) out[n++] = k;
			return out;
		}

		public LongLongMap copy(){
			LongLongMap c = new LongLongMap( keys.length );
			System.arraycopy( keys, 0, c.keys, 0, keys.length );
			System.arraycopy( vals, 0, c.vals, 0, vals.length );
			c.size = size;
			return c;
		}
	}
}

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

import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The places of the sea (docs/sea.md): islands out in the open ocean, wrecks on the shores and
 * on the islands, lighthouses on the islands and on the headlands. Every answer is a pure function
 * of (seed, sector), on WorldStructures' 96-cell sector grid, cached per seed: the window worker,
 * the actor thread and a co-op guest's mirror all get the same places.
 *
 * Nothing that stood before moves:
 *  - a place takes a sector only when the land had nothing there (WorldStructures.landType NONE:
 *    the world's own sites, the wetlands' and the desert's places), each kind rolled with its own
 *    salt;
 *  - an island stands only on open ocean that keeps at least CLEAR cells of deep water between any
 *    of its cells and any land of the world (no shore a player could have walked or looked out
 *    from changes) and that never freezes, even in the coldest snap of a winter (no ice a player
 *    could have walked out on changes). It is laid by the window's surface pass alone, after the
 *    world's own terrain: WorldModel and WorldStructures.terrainAt know nothing of it, so the caves
 *    under the sea (whose water hangs off the surface's terrain) and every other place stay as
 *    they were;
 *  - a wreck or a lighthouse on a mainland shore lies only on beach, shallows or dry land the
 *    roads, the villages, the town, the barrows and the ways between the slices leave alone, never
 *    on deep water (the caves' flooding reads it), and fields never reach a beach.
 */
public final class SeaSites {

	private SeaSites(){}

	// ------------------------------------------------------------------ the rules

	/** Deep water kept clear round an island: no land of the world lies within this of any cell
	 *  it lays (a hero on a shore sees 20 cells out). */
	public static final int CLEAR = 52;
	/** An island's base radius, from MIN_RADIUS to MAX_RADIUS; its edge wanders up to SHAPE_MAX
	 *  times as far out. */
	public static final int MIN_RADIUS = 5, MAX_RADIUS = 10;
	static final float SHAPE_MAX = 1.25f, SHAPE_MIN = 0.6f;
	/** The wadeable shallows round an island, cells past its edge. */
	public static final float SHELF = 2.2f;
	/** How far an island's cells reach from its middle, at the most. */
	public static final int REACH = (int)Math.ceil( MAX_RADIUS * SHAPE_MAX + SHELF ) + 2;
	/** The beach round an island's edge, cells deep. */
	static final float BEACH = 1.6f;
	//what counts as open sea in the clearance test: well under the wadeable shelf, so no land can
	//rise between two of its samples
	static final float DEEP = WorldModel.SEA - 0.025f;
	//the lattice the clearance is sampled on
	static final int CLEAR_STEP = 4;
	//the sea never freezes there: the winter's coldest snap leaves it this far above the line
	static final float THAW_MARGIN = 0.03f;
	//the town's own ground: no place of the sea within this of the world's origin
	static final int TOWN_KEEP = WorldModel.TOWN_LAND * 2;

	//the chances, per hundred, of a sector that may hold each
	static final int ISLAND_CHANCE = 60;
	static final int ISLAND_LIGHT = 20, ISLAND_WRECK = 30;
	static final int TREASURE_CHANCE = 45;
	static final int SHORE_WRECK_CHANCE = 40, HEADLAND_LIGHT_CHANCE = 45;

	//the salts: none shared with any other roll of the world
	private static final long ISLAND_SALT   = 0x15_1A_4D_5EL;
	private static final long SHAPE_SALT    = 0x5_4A_9E_15L;
	private static final long HOST_SALT     = 0x40_57_15L;
	private static final long TREASURE_SALT = 0x7_2EA5_0BL;
	private static final long SCATTER_SALT  = 0x15_C477E2L;
	private static final long WRECK_SALT    = 0x3_2EC4_51L;
	private static final long LIGHT_SALT    = 0x1_16_47L;
	private static final long NAME_SALT     = 0x5EA_4A3EL;

	/** What grows on an island: palms on the warm seas, broadleaf woods on the mild, pines on the cool. */
	public enum Climate { PALM, WOOD, PINE }

	//annual warmth (WorldModel.Sample.annualTemperature) at an island's middle for each climate
	static final float PALM_FROM = 0.58f, WOOD_FROM = 0.45f;

	// ------------------------------------------------------------------ hashing

	private static long hash( long seed, long x, long y ){
		long h = seed ^ 0x5EA5_17E5L;
		h ^= x * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft( h, 31 );
		h ^= y * 0xC2B2AE3D27D4EB4FL;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return h;
	}

	private static int roll( long seed, long salt, int sx, int sy, int mod ){
		return (int) Math.floorMod( hash( seed ^ salt, sx, sy ), (long) mod );
	}

	private static float unit( long h ){
		return (h >>> 40) / (float)(1L << 24);
	}

	// ------------------------------------------------------------------ the places

	/** A wreck's hull: four cells by two lying east-west (its bow east or west) or two by four lying
	 *  north-south (its bow north). Its hold, the one cell inside it open to the outside, is where
	 *  its salvage lies; an east-west hull opens south, a north-south one east. */
	public static final class Wreck {
		public final int x0, y0;
		public final boolean upright;
		//east-west: the bow to the west
		public final boolean bowWest;
		public final int holdX, holdY;
		public final boolean onIsland;

		Wreck( int x0, int y0, boolean upright, boolean bowWest, boolean onIsland ){
			this.x0 = x0;
			this.y0 = y0;
			this.upright = upright;
			this.bowWest = bowWest;
			this.onIsland = onIsland;
			if (upright){
				holdX = x0 + 1;
				holdY = y0 + 1;
			} else {
				holdX = x0 + (bowWest ? 2 : 1);
				holdY = y0 + 1;
			}
		}

		public int width(){ return upright ? 2 : 4; }
		public int height(){ return upright ? 4 : 2; }
		public int x1(){ return x0 + width() - 1; }
		public int y1(){ return y0 + height() - 1; }

		public boolean contains( int wx, int wy ){
			return wx >= x0 && wy >= y0 && wx <= x1() && wy <= y1();
		}

		/** The terrain the hull lays on a cell: the hold open, the rest solid; -1 off it. */
		public int terrain( int wx, int wy ){
			if (!contains( wx, wy )) return -1;
			return wx == holdX && wy == holdY ? Terrain.CUSTOM_DECO_EMPTY : Terrain.CUSTOM_DECO;
		}

		/** The cells outside the hull beside its hold, on its open side: where the hold is reached from. */
		public int[][] mouth(){
			if (upright) return new int[][]{ { holdX + 1, holdY - 1 }, { holdX + 1, holdY }, { holdX + 1, holdY + 1 } };
			return new int[][]{ { holdX - 1, holdY + 1 }, { holdX, holdY + 1 }, { holdX + 1, holdY + 1 } };
		}

		/** Its key in the world's keyed stores: its hold's cell. */
		public long key(){
			return OverworldLevel.worldKey( holdX, holdY );
		}
	}

	/** A lighthouse: its base on (x, y) and its shaft on the cell above, both solid; its lamp is
	 *  drawn over the cell above that, and the three cells before its door are an open apron, where
	 *  its keeper stands. */
	public static final class Light {
		public final int x, y;
		public final boolean onIsland;

		Light( int x, int y, boolean onIsland ){
			this.x = x;
			this.y = y;
			this.onIsland = onIsland;
		}

		/** Is the cell its base or its shaft (solid)? */
		public boolean contains( int wx, int wy ){
			return wx == x && (wy == y || wy == y - 1);
		}

		/** The terrain it lays on a cell: the tower solid, the apron open; -1 off it. */
		public int terrain( int wx, int wy ){
			if (contains( wx, wy )) return Terrain.CUSTOM_DECO;
			if (wy == y + 1 && Math.abs( wx - x ) <= 1) return Terrain.CUSTOM_DECO_EMPTY;
			return -1;
		}

		public long key(){
			return OverworldLevel.worldKey( x, y );
		}
	}

	/** An island of the open sea. */
	public static final class Island {
		public final int sx, sy;
		public final int cx, cy, radius;
		public final Climate climate;
		//the shape: three waves round its edge, amplitude and phase each
		private final float a1, p1, a2, p2, a3, p3;
		/** What stands on it: WorldStructures.Site.NONE, SHIPWRECK or LIGHTHOUSE. */
		public final WorldStructures.Site host;
		public final Wreck wreck;
		public final Light light;
		/** The buried treasure's cell (a mark in the sand), Integer.MIN_VALUE for none. */
		public final int digX, digY;
		/** Every cell it lays, shallows included. */
		public final int x0, y0, x1, y1;

		Island( long seed, int sx, int sy, int cx, int cy, Climate climate ){
			this.sx = sx;
			this.sy = sy;
			this.cx = cx;
			this.cy = cy;
			this.climate = climate;
			long h = hash( seed ^ SHAPE_SALT, sx, sy );
			radius = MIN_RADIUS + (int) Math.floorMod( h, (long)(MAX_RADIUS - MIN_RADIUS + 1) );
			a1 = 0.10f + 0.14f * unit( h * 31 );
			a2 = 0.05f + 0.09f * unit( h * 57 );
			a3 = 0.03f + 0.05f * unit( h * 83 );
			p1 = (float)(2 * Math.PI * unit( h * 101 ));
			p2 = (float)(2 * Math.PI * unit( h * 127 ));
			p3 = (float)(2 * Math.PI * unit( h * 151 ));
			x0 = cx - REACH;
			y0 = cy - REACH;
			x1 = cx + REACH;
			y1 = cy + REACH;

			int hostRoll = roll( seed, HOST_SALT, sx, sy, 100 );
			WorldStructures.Site hs = WorldStructures.Site.NONE;
			Wreck wr = null;
			Light li = null;
			if (hostRoll < ISLAND_LIGHT){
				hs = WorldStructures.Site.LIGHTHOUSE;
				li = new Light( cx, cy, true );
			} else if (hostRoll < ISLAND_LIGHT + ISLAND_WRECK){
				hs = WorldStructures.Site.SHIPWRECK;
				wr = shoreWreck( seed );
			}
			host = hs;
			wreck = wr;
			light = li;

			int tx = Integer.MIN_VALUE, ty = Integer.MIN_VALUE;
			if (hs != WorldStructures.Site.LIGHTHOUSE && roll( seed, TREASURE_SALT, sx, sy, 100 ) < TREASURE_CHANCE){
				//a cell of the beach, on the side away from any wreck
				double ang = 2 * Math.PI * unit( hash( seed ^ TREASURE_SALT, sx * 7 + 3, sy * 13 + 5 ) );
				if (wr != null){
					double wa = Math.atan2( (wr.y0 + wr.height() / 2f) - cy, (wr.x0 + wr.width() / 2f) - cx );
					ang = wa + Math.PI * (0.6 + 0.8 * unit( hash( seed ^ TREASURE_SALT, sx, sy * 3 + 1 ) ));
				}
				for (int tries = 0; tries < 12 && tx == Integer.MIN_VALUE; tries++){
					double a = ang + tries * 0.5;
					float r = edge( (float) a ) - 1.0f;
					int x = cx + Math.round( (float)Math.cos( a ) * r );
					int y = cy + Math.round( (float)Math.sin( a ) * r );
					if (beachAt( x, y ) && (wr == null || farFrom( wr, x, y, 2 ))){
						tx = x;
						ty = y;
					}
				}
			}
			digX = tx;
			digY = ty;
		}

		//the wreck on its shore: a hull lying along the shoreline at a hashed bearing
		private Wreck shoreWreck( long seed ){
			double ang = 2 * Math.PI * unit( hash( seed ^ WRECK_SALT, sx, sy ) );
			float r = edge( (float) ang ) - 0.5f;
			float px = cx + (float)Math.cos( ang ) * r, py = cy + (float)Math.sin( ang ) * r;
			//the shore faces east or west: the hull lies north-south along it, else east-west
			boolean upright = Math.abs( Math.cos( ang ) ) >= Math.abs( Math.sin( ang ) );
			boolean bowWest = (hash( seed ^ WRECK_SALT, sx * 5, sy * 11 ) & 1) == 0;
			int w = upright ? 2 : 4, hgt = upright ? 4 : 2;
			int x0 = Math.round( px - w / 2f ), y0 = Math.round( py - hgt / 2f );
			return new Wreck( x0, y0, upright, bowWest, true );
		}

		/** How far its edge lies from its middle at a bearing (radians, screen axes). */
		public float edge( float angle ){
			float k = 1f + a1 * (float)Math.sin( 2 * angle + p1 ) + a2 * (float)Math.sin( 3 * angle + p2 )
					+ a3 * (float)Math.sin( 5 * angle + p3 );
			k = Math.max( SHAPE_MIN, Math.min( SHAPE_MAX, k ) );
			return radius * k;
		}

		//how far a cell lies from the middle, and the edge's distance at its bearing
		private float dist( int wx, int wy ){
			float dx = wx - cx, dy = wy - cy;
			return (float)Math.sqrt( dx * dx + dy * dy );
		}

		private float edgeAt( int wx, int wy ){
			return edge( (float)Math.atan2( wy - cy, wx - cx ) );
		}

		/** Is the cell its land (beach and inland)? */
		public boolean landAt( int wx, int wy ){
			if (wx < x0 || wy < y0 || wx > x1 || wy > y1) return false;
			return dist( wx, wy ) <= edgeAt( wx, wy );
		}

		//its beach ring (the land's outer BEACH cells)
		boolean beachAt( int wx, int wy ){
			float d = dist( wx, wy ), e = edgeAt( wx, wy );
			return d <= e && d > e - BEACH;
		}

		private static boolean farFrom( Wreck w, int x, int y, int gap ){
			return x < w.x0 - gap || y < w.y0 - gap || x > w.x1() + gap || y > w.y1() + gap;
		}

		/** Does it lay anything on this cell (land, a hull, or its shallows)? */
		public boolean covers( int wx, int wy ){
			return terrainCore( wx, wy ) != -1;
		}

		//the ground (or water) of a cell before what grows on it: -1 off the island
		private int terrainCore( int wx, int wy ){
			if (wx < x0 || wy < y0 || wx > x1 || wy > y1) return -1;
			if (wreck != null){
				int t = wreck.terrain( wx, wy );
				if (t != -1) return t;
			}
			if (light != null){
				int t = light.terrain( wx, wy );
				if (t != -1) return t;
			}
			float d = dist( wx, wy ), e = edgeAt( wx, wy );
			if (d <= e) return d > e - BEACH ? Terrain.EMPTY_SP : Terrain.GRASS;
			//the shallows: past its edge, and round a wreck lying in them
			if (d <= e + SHELF) return Terrain.WATER;
			if (wreck != null && !farFrom( wreck, wx, wy, 2 )) return Terrain.WATER;
			return -1;
		}

		/** The terrain it lays on a cell, -1 off it. Pure: the window's surface pass lays it. */
		public int terrain( long seed, int wx, int wy ){
			int t = terrainCore( wx, wy );
			if (t != Terrain.EMPTY_SP && t != Terrain.GRASS) return t;
			if (wx == digX && wy == digY) return Terrain.EMPTY_SP;
			//kept clear: round the lighthouse, round the dig, along the wreck
			if (light != null && Math.abs( wx - light.x ) <= 2 && wy >= light.y - 3 && wy <= light.y + 2){
				return t == Terrain.EMPTY_SP ? t : Terrain.GRASS;
			}
			if (digX != Integer.MIN_VALUE && Math.abs( wx - digX ) <= 1 && Math.abs( wy - digY ) <= 1) return t;
			if (wreck != null && !farFrom( wreck, wx, wy, 1 )) return t;
			long h = hash( seed ^ SCATTER_SALT, wx, wy );
			int r = (int) Math.floorMod( h >>> 16, 100L );
			if (t == Terrain.EMPTY_SP){
				//the beach: a bush now and then above the tide line, never on the cold isles' sand
				return r < 4 && climate != Climate.PINE ? Terrain.SHRUB : t;
			}
			switch (climate){
				case PALM:
					//sand drifts in among the palms, and the middle is grass
					if (r < 20) return Terrain.TREE_OAK;
					if (r < 26) return Terrain.SHRUB;
					if (r < 30) return Terrain.FLOWER_PATCH;
					return r < 48 && dist( wx, wy ) > radius * 0.45f ? Terrain.EMPTY_SP : Terrain.GRASS;
				case WOOD:
					if (r < 18) return Terrain.TREE_OAK;
					if (r < 23) return Terrain.TREE_PINE;
					if (r < 30) return Terrain.SHRUB;
					if (r < 40) return Terrain.HIGH_GRASS;
					if (r < 43) return Terrain.FLOWER_PATCH;
					return r < 45 ? Terrain.BOULDER : Terrain.GRASS;
				case PINE: default:
					if (r < 22) return Terrain.TREE_PINE;
					if (r < 28) return Terrain.SHRUB;
					if (r < 32) return Terrain.BOULDER;
					return r < 50 ? Terrain.EMPTY : Terrain.GRASS;
			}
		}

		/** Its key in the world's keyed stores: its middle's cell. */
		public long key(){
			return OverworldLevel.worldKey( cx, cy );
		}
	}

	/** Everything the sea holds in one sector: an island (with what stands on it), or a wreck or a
	 *  lighthouse on a mainland shore, or nothing. */
	public static final class Sector {
		public final Island island;
		public final Wreck wreck;
		public final Light light;
		public final WorldStructures.Site type;

		Sector( Island island, Wreck wreck, Light light ){
			this.island = island;
			this.wreck = wreck;
			this.light = light;
			type = light != null ? WorldStructures.Site.LIGHTHOUSE
					: wreck != null ? WorldStructures.Site.SHIPWRECK : WorldStructures.Site.NONE;
		}

		static final Sector EMPTY = new Sector( null, null, null );
	}

	// ------------------------------------------------------------------ the cache

	private static long cachedSeed = Long.MIN_VALUE;
	private static final Map<Long, Sector> cache = new ConcurrentHashMap<>();

	private static synchronized void checkSeed( long seed ){
		if (seed != cachedSeed){
			cachedSeed = seed;
			cache.clear();
		}
	}

	/** What the sea holds in a sector. Any thread: get, compute outside, putIfAbsent. */
	public static Sector sector( long seed, int sx, int sy ){
		checkSeed( seed );
		Long key = WorldStructures.sectorOf( sx, sy );
		Sector s = cache.get( key );
		if (s != null) return s;
		s = compute( seed, sx, sy );
		Sector had = cache.putIfAbsent( key, s );
		return had != null ? had : s;
	}

	/** The site a sector's sea holds (WorldStructures.siteType asks it of a sector it left empty). */
	public static WorldStructures.Site siteOf( long seed, int sx, int sy ){
		return sector( seed, sx, sy ).type;
	}

	/** The island of a sector, or null. */
	public static Island island( long seed, int sx, int sy ){
		return sector( seed, sx, sy ).island;
	}

	private static Sector compute( long seed, int sx, int sy ){
		if (WorldStructures.landType( seed, sx, sy ) != WorldStructures.Site.NONE) return Sector.EMPTY;
		int cx = WorldStructures.siteX( seed, sx, sy ), cy = WorldStructures.siteY( seed, sx, sy );
		if (Math.max( Math.abs( cx ), Math.abs( cy ) ) < TOWN_KEEP) return Sector.EMPTY;
		WorldModel.Sample s = new WorldModel.Sample();
		float e = WorldModel.elevationOf( seed, cx, cy, s );
		if (e < DEEP && roll( seed, ISLAND_SALT, sx, sy, 100 ) < ISLAND_CHANCE){
			Island isl = makeIsland( seed, sx, sy, cx, cy, s );
			if (isl != null) return new Sector( isl, isl.wreck, isl.light );
		}
		//no island: a mainland shore crossing the sector may hold a wreck or a lighthouse
		return shore( seed, sx, sy, cx, cy );
	}

	//an island on (cx, cy), when the sea there is open and never freezes
	private static Island makeIsland( long seed, int sx, int sy, int cx, int cy, WorldModel.Sample s ){
		if (!clearSea( seed, cx, cy, REACH + CLEAR, s )) return null;
		//never ice: the coldest winter snap at its middle and round its rim
		float coldest = WorldModel.WINTER_SHIFT - WorldModel.CLIMATE_SHIFT;
		int[][] probe = { { 0, 0 }, { REACH, 0 }, { -REACH, 0 }, { 0, REACH }, { 0, -REACH } };
		for (int[] p : probe){
			WorldModel.sample( seed, cx + p[0], cy + p[1], coldest, s );
			if (s.temperature < WorldModel.FREEZE + THAW_MARGIN) return null;
		}
		WorldModel.sample( seed, cx, cy, 0f, s );
		Climate c = s.annualTemperature >= PALM_FROM ? Climate.PALM
				: s.annualTemperature >= WOOD_FROM ? Climate.WOOD : Climate.PINE;
		return new Island( seed, sx, sy, cx, cy, c );
	}

	/** Is every sample of a lattice within r of (cx, cy) open deep sea (DEEP)? The rim first, then a
	 *  coarse lattice, then the fine one: a coast is found in a few samples. */
	static boolean clearSea( long seed, int cx, int cy, int r, WorldModel.Sample s ){
		for (int i = 0; i < 32; i++){
			double a = i * Math.PI / 16;
			if (WorldModel.elevationOf( seed, cx + (int)Math.round( Math.cos( a ) * r ),
					cy + (int)Math.round( Math.sin( a ) * r ), s ) >= DEEP) return false;
		}
		for (int step : new int[]{ 4 * CLEAR_STEP, CLEAR_STEP }){
			for (int dy = -r; dy <= r; dy += step){
				for (int dx = -r; dx <= r; dx += step){
					if (dx * dx + dy * dy > r * r) continue;
					if (step == CLEAR_STEP && dx % (4 * CLEAR_STEP) == 0 && dy % (4 * CLEAR_STEP) == 0) continue;
					if (WorldModel.elevationOf( seed, cx + dx, cy + dy, s ) >= DEEP) return false;
				}
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ the mainland's shores

	//a shore's ground, from the elevation alone (WorldModel.elevationOf: no climate, half a sample):
	//the sea's wadeable shelf (tier 1), low dry ground by the water, the open sea (tier 3 and down)
	static boolean shelfOrShore( float e ){
		return e >= WorldModel.SEA - 0.012f && e < WorldModel.SEA + 0.03f;
	}

	static boolean dry( float e ){
		return e >= WorldModel.SEA;
	}

	static boolean openSea( float e ){
		return e < WorldModel.SEA - 0.024f;
	}

	//the coarse lattice a sector's middle is searched on, and how far out from its middle half
	private static final int LATTICE = 4, MARGIN = 8;
	//shore points of a lattice tried, nearest the candidate first
	private static final int SHORE_TRIES = 10;

	//a wreck or a lighthouse on the mainland's shore in a sector's middle, each rolled on its own:
	//its shoreline found on a coarse lattice, the places fitted to it cell by cell
	private static Sector shore( long seed, int sx, int sy, int cx, int cy ){
		boolean wantWreck = roll( seed, WRECK_SALT, sx, sy, 100 ) < SHORE_WRECK_CHANCE;
		boolean wantLight = roll( seed, LIGHT_SALT, sx, sy, 100 ) < HEADLAND_LIGHT_CHANCE;
		if (!wantWreck && !wantLight) return Sector.EMPTY;
		WorldModel.Sample s = new WorldModel.Sample();
		int bx = sx * WorldStructures.SECTOR + WorldStructures.SECTOR / 4 - MARGIN;
		int by = sy * WorldStructures.SECTOR + WorldStructures.SECTOR / 4 - MARGIN;
		int n = (WorldStructures.SECTOR / 2 + 2 * MARGIN) / LATTICE + 1;
		float[] lat = new float[n * n];
		boolean sea = false, land = false;
		for (int j = 0; j < n; j++){
			for (int i = 0; i < n; i++){
				float e = WorldModel.elevationOf( seed, bx + i * LATTICE, by + j * LATTICE, s );
				lat[i + j * n] = e;
				if (openSea( e )) sea = true;
				if (dry( e )) land = true;
			}
		}
		if (!sea || !land) return Sector.EMPTY;
		//the lattice's shore points: dry, low ground with the open sea within two steps of it
		ArrayList<int[]> points = new ArrayList<>();
		for (int j = 0; j < n; j++){
			for (int i = 0; i < n; i++){
				float e = lat[i + j * n];
				if (!dry( e ) || e >= WorldModel.SEA + 0.03f) continue;
				boolean wet = false;
				for (int dj = -2; dj <= 2 && !wet; dj++){
					for (int di = -2; di <= 2 && !wet; di++){
						int ii = i + di, jj = j + dj;
						if (ii >= 0 && jj >= 0 && ii < n && jj < n && openSea( lat[ii + jj * n] )) wet = true;
					}
				}
				if (wet) points.add( new int[]{ bx + i * LATTICE, by + j * LATTICE } );
			}
		}
		if (points.isEmpty()) return Sector.EMPTY;
		Collections.sort( points, ( a, b ) -> {
			long da = (long)(a[0] - cx) * (a[0] - cx) + (long)(a[1] - cy) * (a[1] - cy);
			long db = (long)(b[0] - cx) * (b[0] - cx) + (long)(b[1] - cy) * (b[1] - cy);
			if (da != db) return Long.compare( da, db );
			if (a[1] != b[1]) return Integer.compare( a[1], b[1] );
			return Integer.compare( a[0], b[0] );
		} );
		if (wantWreck){
			boolean firstUpright = (hash( seed ^ WRECK_SALT, sx * 3, sy * 7 ) & 1) == 0;
			boolean bowWest = (hash( seed ^ WRECK_SALT, sx * 5, sy * 11 ) & 1) == 0;
			for (int p = 0; p < points.size() && p < SHORE_TRIES; p++){
				Wreck w = wreckNear( seed, sx, sy, points.get( p ), firstUpright, bowWest, s );
				if (w != null) return new Sector( null, w, null );
			}
		}
		if (wantLight){
			for (int p = 0; p < points.size() && p < SHORE_TRIES; p++){
				Light l = headlandNear( seed, sx, sy, points.get( p ), s );
				if (l != null) return new Sector( null, null, l );
			}
		}
		return Sector.EMPTY;
	}

	//the offsets a place is fitted at round a shore point, nearest first
	private static final int[][] NEAR;
	static {
		ArrayList<int[]> offs = new ArrayList<>();
		for (int dy = -3; dy <= 3; dy++){
			for (int dx = -3; dx <= 3; dx++) offs.add( new int[]{ dx, dy } );
		}
		Collections.sort( offs, ( a, b ) -> {
			int da = a[0] * a[0] + a[1] * a[1], db = b[0] * b[0] + b[1] * b[1];
			if (da != db) return Integer.compare( da, db );
			if (a[1] != b[1]) return Integer.compare( a[1], b[1] );
			return Integer.compare( a[0], b[0] );
		} );
		NEAR = offs.toArray( new int[0][] );
	}

	//a hull fitted round a shore point: on the shelf and the low ground by it alone (never deep
	//water, which the caves read), mostly ashore, its hold reachable, nothing of the world round it
	private static Wreck wreckNear( long seed, int sx, int sy, int[] pt, boolean firstUpright, boolean bowWest,
			WorldModel.Sample s ){
		for (int[] o : NEAR){
			for (int k = 0; k < 2; k++){
				boolean upright = k == 0 ? firstUpright : !firstUpright;
				int w = upright ? 2 : 4, h = upright ? 4 : 2;
				Wreck wr = new Wreck( pt[0] + o[0] - w / 2, pt[1] + o[1] - h / 2, upright, bowWest, false );
				if (wreckFits( seed, sx, sy, wr, s )) return wr;
			}
		}
		return null;
	}

	private static boolean wreckFits( long seed, int sx, int sy, Wreck w, WorldModel.Sample s ){
		int ashore = 0;
		for (int y = w.y0; y <= w.y1(); y++){
			for (int x = w.x0; x <= w.x1(); x++){
				float e = WorldModel.elevationOf( seed, x, y, s );
				if (!shelfOrShore( e )) return false;
				if (dry( e )) ashore++;
			}
		}
		if (ashore < 3) return false;
		boolean reachable = false;
		for (int[] m : w.mouth()){
			if (shelfOrShore( WorldModel.elevationOf( seed, m[0], m[1], s ) )) reachable = true;
		}
		if (!reachable) return false;
		//the sea's own shore, never a river's channel or a swamp's pool: the biome, last (a full sample)
		for (int y = w.y0 - 1; y <= w.y1() + 1; y++){
			for (int x = w.x0 - 1; x <= w.x1() + 1; x++){
				if (x != w.x0 - 1 && x != w.x1() + 1 && y != w.y0 - 1 && y != w.y1() + 1 && (x + y) % 2 != 0) continue;
				WorldModel.sample( seed, x, y, 0f, s );
				if (s.biome == WorldModel.Biome.RIVER || s.biome == WorldModel.Biome.SWAMP) return false;
			}
		}
		return clearOfTheWorld( seed, sx, sy, w.x0 - 1, w.y0 - 1, w.x1() + 1, w.y1() + 1 );
	}

	//a lighthouse fitted round a shore point: on dry, low ground of the surface's own band, the sea
	//within four and round more than half of a ring nine out
	private static Light headlandNear( long seed, int sx, int sy, int[] pt, WorldModel.Sample s ){
		for (int[] o : NEAR){
			int x = pt[0] + o[0], y = pt[1] + o[1];
			//its base and shaft, the apron before it and a cell round them: dry, low, no rock near
			boolean ok = true;
			for (int dy = -3; dy <= 2 && ok; dy++){
				for (int dx = -2; dx <= 2 && ok; dx++){
					float e = WorldModel.elevationOf( seed, x + dx, y + dy, s );
					if (Math.abs( dx ) <= 1 && dy >= -2 && dy <= 1){
						if (!dry( e ) || e > WorldModel.SEA + 0.12f) ok = false;
					} else if (WorldLayers.band( e ) != 0){
						ok = false;
					}
				}
			}
			if (!ok) continue;
			boolean shore = false;
			for (int i = 0; i < 16 && !shore; i++){
				double a = i * Math.PI / 8;
				if (!dry( WorldModel.elevationOf( seed, x + (int)Math.round( Math.cos( a ) * 4 ),
						y + (int)Math.round( Math.sin( a ) * 4 ), s ) )) shore = true;
			}
			if (!shore) continue;
			int wet = 0;
			for (int i = 0; i < 16; i++){
				double a = i * Math.PI / 8;
				if (!dry( WorldModel.elevationOf( seed, x + (int)Math.round( Math.cos( a ) * 9 ),
						y + (int)Math.round( Math.sin( a ) * 9 ), s ) )) wet++;
			}
			if (wet < 8) continue;
			//the ground itself: no river's or swamp's bank, no cave mouth under it
			boolean wild = false;
			for (int dy = -2; dy <= 1 && !wild; dy++){
				for (int dx = -1; dx <= 1 && !wild; dx++){
					WorldModel.sample( seed, x + dx, y + dy, 0f, s );
					if (s.biome == WorldModel.Biome.RIVER || s.biome == WorldModel.Biome.SWAMP
							|| s.biome == WorldModel.Biome.OCEAN) wild = true;
					if (mouthOdds( seed, x + dx, y + dy )) wild = true;
				}
			}
			if (wild) continue;
			if (!clearOfTheWorld( seed, sx, sy, x - 2, y - 3, x + 2, y + 2 )) continue;
			return new Light( x, y, false );
		}
		return null;
	}

	private static boolean mouthOdds( long seed, int wx, int wy ){
		return Math.floorMod( WorldModel.linkHash( seed ^ 0x300DL, wx, wy, 0 ), 900 ) == 0;
	}

	//nothing of the world in the box: the town's ground, a road, or any place of the sectors round
	//(read from WorldStructures' own places, never from the sea's: a sector of the sea never asks
	//another's)
	private static boolean clearOfTheWorld( long seed, int sx, int sy, int x0, int y0, int x1, int y1 ){
		for (int y = y0; y <= y1; y++){
			for (int x = x0; x <= x1; x++){
				if (WorldStructures.townCell( x, y ) != -1) return false;
				if (WorldModel.townInfluence( seed, x, y, WorldModel.TOWN_LAND ) > 0f) return false;
				if (WorldStructures.onRoad( seed, x, y )) return false;
				if (WorldStructures.landTerrainAround( seed, sx, sy, x, y )) return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ the terrain

	/** What a mainland place of the sea lays on a cell (WorldStructures.terrainAt asks it for a
	 *  sector of its); -1 off it, and always -1 for an island's (the surface pass lays islands). */
	public static int siteTerrain( long seed, int sx, int sy, int wx, int wy ){
		Sector s = sector( seed, sx, sy );
		if (s.island != null) return -1;
		if (s.wreck != null){
			int t = s.wreck.terrain( wx, wy );
			if (t != -1) return t;
			//the way into its hold kept open: the dry ground before it (its shallows are open anyway)
			for (int[] m : s.wreck.mouth()){
				if (m[0] == wx && m[1] == wy && dry( WorldModel.elevation( seed, wx, wy ) )) return Terrain.CUSTOM_DECO_EMPTY;
			}
			return -1;
		}
		if (s.light != null) return s.light.terrain( wx, wy );
		return -1;
	}

	/** The islands a window of the surface reaches, resolved once for its whole surface pass. */
	public static final class View {
		public final List<Island> islands;

		View( List<Island> islands ){
			this.islands = islands;
		}

		/** The island laying a cell, null for none. */
		public Island at( int wx, int wy ){
			for (Island i : islands){
				if (wx >= i.x0 && wy >= i.y0 && wx <= i.x1 && wy <= i.y1 && i.covers( wx, wy )) return i;
			}
			return null;
		}
	}

	/** The islands whose cells may fall in the world rect [x0, x1] x [y0, y1]. */
	public static View view( long seed, int x0, int y0, int x1, int y1 ){
		ArrayList<Island> out = new ArrayList<>();
		int sx0 = Math.floorDiv( x0 - REACH, WorldStructures.SECTOR ), sy0 = Math.floorDiv( y0 - REACH, WorldStructures.SECTOR );
		int sx1 = Math.floorDiv( x1 + REACH, WorldStructures.SECTOR ), sy1 = Math.floorDiv( y1 + REACH, WorldStructures.SECTOR );
		for (int sy = sy0; sy <= sy1; sy++){
			for (int sx = sx0; sx <= sx1; sx++){
				Island i = island( seed, sx, sy );
				if (i != null && i.x1 >= x0 && i.y1 >= y0 && i.x0 <= x1 && i.y0 <= y1) out.add( i );
			}
		}
		return new View( Collections.unmodifiableList( out ) );
	}

	/** Every sector's sea in the world rect of sectors [sx0, sx1] x [sy0, sy1] that holds anything. */
	public static ArrayList<Sector> sectors( long seed, int sx0, int sy0, int sx1, int sy1 ){
		ArrayList<Sector> out = new ArrayList<>();
		for (int sy = sy0; sy <= sy1; sy++){
			for (int sx = sx0; sx <= sx1; sx++){
				Sector s = sector( seed, sx, sy );
				if (s != Sector.EMPTY) out.add( s );
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ the names

	private static final String[] NAME_A = { "Gull", "Tern", "Kelp", "Salt", "Coral", "Drift", "Heron", "Pearl",
			"Amber", "Shell", "Foam", "Brine", "Cormorant", "Seal", "Wren", "Morrow" };
	private static final String[] NAME_B = { "Cay", "Key", "Holm", "Rock", "Isle", "Skerry", "Islet", "Haven" };
	private static final String[] SHIPS = { "Merrow", "Saltwind", "Grey Lady", "Tern", "Heron's Wake", "Lantern",
			"Morning Star", "Whitecap", "Pelican", "Sea Hare", "Wanderer", "Mermaid's Purse" };

	/** The name an island goes by, the same in every game of the seed. */
	public static String islandName( long seed, int sx, int sy ){
		long h = hash( seed ^ NAME_SALT, sx, sy );
		return NAME_A[(int) Math.floorMod( h, (long) NAME_A.length )] + " "
				+ NAME_B[(int) Math.floorMod( h >>> 12, (long) NAME_B.length )];
	}

	/** A lighthouse's name: its island's, or the headland's own. */
	public static String lightName( long seed, int sx, int sy ){
		long h = hash( seed ^ NAME_SALT, sx * 3 + 1, sy * 5 + 2 );
		return NAME_A[(int) Math.floorMod( h, (long) NAME_A.length )];
	}

	/** The ship a wreck once was. */
	public static String shipName( long seed, int sx, int sy ){
		long h = hash( seed ^ NAME_SALT, sx * 7 + 3, sy * 11 + 4 );
		return SHIPS[(int) Math.floorMod( h, (long) SHIPS.length )];
	}
}

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

import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The places only a climber finds: a hermit's hut, an eagles' eyrie on a cliff's lip, a cairn on
 * every summit, hot springs steaming in the snow, a frozen climber's remains, a broken watchtower
 * on a ridge and a waystation at the mouth of a pass through the rock.
 *
 * Everything here is a pure function of the world seed, the sector and the slice (WorldStructures'
 * way): each 64-cell sector of a slice may roll one site, laid on the slice's own ground well
 * inside the sector, so two sites of a slice never touch and every window that holds a site lays
 * it the same. A summit's cairn is not rolled: it stands on the highest cell of a peak (see
 * summitOf). WindowGenerator.mountain lays them into the window's terrain as its last pass,
 * validating against the window's own bands and sampling the world only off the window; the
 * dressing adds their fittings; OverworldLevel peoples them. Resolved sites are cached per seed
 * (never computeIfAbsent: a resolution can ask for another one).
 *
 * The ways between the slices are never touched: every cell a site lays has nothing but its own
 * slice's ground within two cells, so no cliff stair, its foot, a tunnel mouth or the steps of the
 * slice above stand on or beside it (WorldLayersTest pins the stairs).
 */
public final class MountainSites {

	private MountainSites(){}

	public static final int SECTOR = 64;
	//one sector in this many (percent) per slice rolls a site
	static final int SITE_CHANCE = 40;
	//a site's anchor is sought on a lattice of the sector's middle, sector*64 + JITTER0 + STEP*i
	//for i < SPOTS: with the widest layout and its margin it never leaves its sector
	static final int JITTER0 = 10, STEP = 3, SPOTS = 15;
	//no way between the slices stands this close to a cell a site lays
	static final int MARGIN = 2;
	//a summit: the highest cell (elevation, then a hash for the clamped tops) within this radius
	static final int SUMMIT_R = 16, SUMMIT_MIN_BAND = 2, CLIMB_CAP = 64;
	/** a hot spring's water and the ground within two cells of it, in degrees: warm enough to
	 *  bring a frozen climber round, under the heatstroke line */
	public static final float POOL_C = 28f, STEAM_C = 12f;
	/** how far the land shows from a summit's cairn and from a watchtower's top */
	public static final int SUMMIT_VIEW = 40, TOWER_VIEW = 26;
	/** wildlife keeps this far off a site's reach (a berth) */
	public static final int BERTH = 4;
	//a resolved-site cache past this size is dropped and starts over
	private static final int CACHE_CAP = 6000;

	/** What a site is. Never reorder: the ordinals are saved (found sites). */
	public enum Kind {
		HERMIT ( 1,  5, 2, 0xFFC8884A ),
		EYRIE  ( 3,  8, 3, 0xFFE8C860 ),
		SPRINGS( 1,  6, 2, 0xFF70D8FF ),
		CLIMBER( 4, 10, 3, 0xFFB8B8C8 ),
		TOWER  ( 1,  4, 3, 0xFFA88868 ),
		//down to +9: a shelter wherever the thin air makes one needed
		PASS   ( 1,  9, 2, 0xFFFF9838 ),
		//not rolled: every summit has one (summitOf)
		CAIRN  ( 2, 10, 0, 0xFFFFFFFF );

		public final int lo, hi, weight, pin;

		Kind( int lo, int hi, int weight, int pin ){
			this.lo = lo;
			this.hi = hi;
			this.weight = weight;
			this.pin = pin;
		}

		public String lower(){
			return name().toLowerCase( Locale.ENGLISH );
		}
	}

	/** One site of one slice. Immutable. */
	public static final class Site {
		public final Kind kind;
		public final int altitude;
		/** the anchor: the hut's, spring's, climber's, tower's or shelter's centre, the nest, the summit */
		public final int wx, wy;
		/** 0 N, 1 E, 2 S, 3 W: the eyrie's drop, the tower's gap, the shelter's doorway */
		public final int dir;
		/** a shelter's: where its path meets the pass. A cairn's ax: the summit's height in feet */
		public final int ax, ay;
		/** a spring's extra pool cells */
		public final int bits;
		public final long key;
		/** the reach box (world, inclusive): every cell the site lays */
		public final int x0, y0, x1, y1;
		/** the cell whose sight finds the site: the anchor, the hut's doorstep for the hermit */
		public final int seeX, seeY;

		Site( Kind kind, int altitude, int wx, int wy, int dir, int ax, int ay, int bits,
				int x0, int y0, int x1, int y1 ){
			this.kind = kind;
			this.altitude = altitude;
			this.wx = wx;
			this.wy = wy;
			this.dir = dir;
			this.ax = ax;
			this.ay = ay;
			this.bits = bits;
			this.x0 = x0;
			this.y0 = y0;
			this.x1 = x1;
			this.y1 = y1;
			this.key = OverworldLevel.structHash( 0x5173E0L + kind.ordinal(), OverworldLevel.worldKey( wx, wy ) );
			this.seeX = wx;
			this.seeY = kind == Kind.HERMIT ? wy + 3 : wy;
		}

		/** a cairn's summit height in feet */
		public int feet(){
			return kind == Kind.CAIRN ? ax : 0;
		}

		@Override
		public boolean equals( Object o ){
			if (!(o instanceof Site)) return false;
			Site s = (Site) o;
			return s.kind == kind && s.altitude == altitude && s.wx == wx && s.wy == wy && s.dir == dir
					&& s.ax == ax && s.ay == ay && s.bits == bits;
		}

		@Override
		public int hashCode(){
			return (int) key;
		}
	}

	private static final Site NONE = new Site( Kind.CAIRN, 0, Integer.MIN_VALUE, Integer.MIN_VALUE, 0, 0, 0, 0, 0, 0, 0, 0 );

	// ------------------------------------------------------------ geometry

	//N, E, S, W
	static final int[] DX = { 0, 1, 0, -1 }, DY = { -1, 0, 1, 0 };

	/** Is the world cell inside a site's reach box? */
	public static boolean inReach( Site s, int wx, int wy ){
		return wx >= s.x0 && wx <= s.x1 && wy >= s.y0 && wy <= s.y1;
	}

	/** ...inside its berth, where no wildlife is placed: the reach grown by BERTH. */
	public static boolean inBerth( Site s, int wx, int wy ){
		return wx >= s.x0 - BERTH && wx <= s.x1 + BERTH && wy >= s.y0 - BERTH && wy <= s.y1 + BERTH;
	}

	private static boolean touches( Site s, int x0, int y0, int x1, int y1 ){
		return s.x1 >= x0 && s.x0 <= x1 && s.y1 >= y0 && s.y0 <= y1;
	}

	/** A hot spring's pool, as world {x, y, x, y, ...}: the 3x3 at its heart and the extra cells its bits pick. */
	public static int[] poolCells( Site s ){
		int[] extra = { 2, 0, -2, 0, 0, 2, 0, -2 };
		int n = 9 + Integer.bitCount( s.bits & 15 );
		int[] out = new int[n * 2];
		int i = 0;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				out[i++] = s.wx + dx;
				out[i++] = s.wy + dy;
			}
		}
		for (int b = 0; b < 4; b++){
			if ((s.bits & (1 << b)) == 0) continue;
			out[i++] = s.wx + extra[2*b];
			out[i++] = s.wy + extra[2*b + 1];
		}
		return out;
	}

	private static boolean inPool( Site s, int wx, int wy ){
		int dx = wx - s.wx, dy = wy - s.wy;
		if (Math.abs( dx ) <= 1 && Math.abs( dy ) <= 1) return true;
		return (dx == 2 && dy == 0 && (s.bits & 1) != 0) || (dx == -2 && dy == 0 && (s.bits & 2) != 0)
				|| (dx == 0 && dy == 2 && (s.bits & 4) != 0) || (dx == 0 && dy == -2 && (s.bits & 8) != 0);
	}

	/** A waystation's hearth, world {x, y}: in a corner under the north wall, where its chimney
	 *  can rise above that wall (chimneyCell) - a back corner for a door to the south, east or
	 *  west, beside the doorway for a door to the north. */
	public static int[] hearthCell( long seed, Site s ){
		if (s.dir == 1 || s.dir == 3) return new int[]{ s.wx - DX[s.dir], s.wy - 1 };
		return new int[]{ s.wx + SettlementLights.chimneySide( seed, s.wx, s.wy ), s.wy - 1 };
	}

	/** The window cell a waystation's chimney stands on: above the north wall straight over its
	 *  hearth, on open ground, else -1 (no stack anywhere else: the smoke rises over the fire). */
	public static int chimneyCell( long seed, Site s, int[] map, int w, int h, int ox, int oy ){
		int[] hearth = hearthCell( seed, s );
		int x = hearth[0] - ox, y = s.wy - 3 - oy;
		if (x < 1 || x >= w - 1 || y < 1 || y >= h - 2) return -1;
		int c = x + y * w;
		if (map[c + w] != Terrain.WALL) return -1;
		int t = map[c];
		if ((Terrain.flags[t] & Terrain.SOLID) != 0 || WindowGenerator.blocksSight( t )) return -1;
		return c;
	}

	/** A waystation's bedroll, world {x, y}: against the back wall, clear of the doorway. */
	public static int[] bedrollCell( Site s ){
		return new int[]{ s.wx - DX[s.dir], s.wy - DY[s.dir] };
	}

	//a waystation's signpost, world {x, y}: outside, beside the path from its doorway
	private static int[] signCell( long seed, Site s ){
		int side = SettlementLights.chimneySide( seed, s.wx, s.wy );
		if (s.dir == 0 || s.dir == 2) return new int[]{ s.wx - side, s.wy + 3 * DY[s.dir] };
		return new int[]{ s.wx + 3 * DX[s.dir], s.wy + 1 };
	}

	//the tower's ring (Chebyshev 2) clockwise from the north-west, corners left out
	private static final int[][] TOWER_RING = {
			{ -1, -2 }, { 0, -2 }, { 1, -2 }, { 2, -1 }, { 2, 0 }, { 2, 1 },
			{ 1, 2 }, { 0, 2 }, { -1, 2 }, { -2, 1 }, { -2, 0 }, { -2, -1 } };

	//the one stretch of a tower's ring, besides its corners, that has come down
	private static int[] towerBreach( long seed, Site s ){
		long h = WindowGenerator.dressHash( seed, s.wx, s.wy, 0x70E3L );
		int pick = (int) Math.floorMod( h >>> 4, 11L ), k = 0;
		for (int[] r : TOWER_RING){
			if (r[0] == 2 * DX[s.dir] && r[1] == 2 * DY[s.dir]) continue;
			if (k++ == pick) return r;
		}
		return TOWER_RING[0];
	}

	/**
	 * The solid terrain a site builds on a world cell - the walls of a hut, a tower or a shelter,
	 * a door, the tower's fallen stones, a signpost - or -1. The natural scatter a site leaves or
	 * clears is not its own.
	 */
	static int solidAt( long seed, Site s, int wx, int wy ){
		int dx = wx - s.wx, dy = wy - s.wy;
		int r = Math.max( Math.abs( dx ), Math.abs( dy ) );
		switch (s.kind){
			case HERMIT:
				if (r != 2) return -1;
				return dx == 0 && dy == 2 ? Terrain.DOOR : Terrain.WALL;
			case PASS: {
				int[] sign = signCell( seed, s );
				if (wx == sign[0] && wy == sign[1]) return Terrain.SIGN;
				if (r != 2 || (dx == 2 * DX[s.dir] && dy == 2 * DY[s.dir])) return -1;
				return Terrain.WALL;
			}
			case TOWER: {
				if (r == 2){
					if (dx == 2 * DX[s.dir] && dy == 2 * DY[s.dir]) return -1;
					if (Math.abs( dx ) == 2 && Math.abs( dy ) == 2) return Terrain.BOULDER;
					int[] breach = towerBreach( seed, s );
					return dx == breach[0] && dy == breach[1] ? Terrain.BOULDER : Terrain.WALL;
				}
				if (r == 3 && !towerApproach( s, dx, dy )
						&& Math.floorMod( WindowGenerator.dressHash( seed, wx, wy, 0x7B1L ), 4L ) == 0){
					return Terrain.BOULDER;
				}
				return -1;
			}
			default:
				return -1;
		}
	}

	//the three cells before a tower's gap, kept clear of its fallen stones
	private static boolean towerApproach( Site s, int dx, int dy ){
		int gx = 3 * DX[s.dir], gy = 3 * DY[s.dir];
		return Math.abs( dx - gx ) + Math.abs( dy - gy ) <= 1 && (dx == gx || dy == gy);
	}

	// ------------------------------------------------------------ the world, probed

	/**
	 * What the resolvers read of the world: a cell's band and elevation. Inside the window being
	 * generated they come from its own sampled arrays (neither depends on the season), off it
	 * from a fresh sample. One per thread at a time.
	 */
	static final class Probe {
		final long seed;
		final byte[] band;
		final float[] elev;
		final int ox, oy;
		final WorldModel.Sample smp = new WorldModel.Sample();
		//the last cell sampled: a band read off the window is usually followed by its elevation
		private int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE;
		private float lastElev;

		Probe( long seed, byte[] band, float[] elev, int ox, int oy ){
			this.seed = seed;
			this.band = band;
			this.elev = elev;
			this.ox = ox;
			this.oy = oy;
		}

		/** sampling only: no window */
		Probe( long seed ){
			this( seed, null, null, 0, 0 );
		}

		private int local( int wx, int wy ){
			if (band == null) return -1;
			int x = wx - ox, y = wy - oy;
			return x >= 0 && y >= 0 && x < WindowGenerator.WIDTH && y < WindowGenerator.HEIGHT ? x + y * WindowGenerator.WIDTH : -1;
		}

		int band( int wx, int wy ){
			int c = local( wx, wy );
			return c >= 0 ? band[c] : WorldLayers.band( elev( wx, wy ) );
		}

		float elev( int wx, int wy ){
			int c = local( wx, wy );
			if (c >= 0) return elev[c];
			if (wx != lastX || wy != lastY){
				lastElev = WorldModel.elevationOf( seed, wx, wy, smp );
				lastX = wx;
				lastY = wy;
			}
			return lastElev;
		}

		//the slice's annual-mean cold here: snow lies most of the year
		boolean frozenMean( int wx, int wy, int a ){
			return WorldModel.alpineTemperature( WorldModel.sample( seed, wx, wy, 0f, smp ), a ) < WorldModel.FREEZE;
		}

		//...and at midsummer, the warmest the season gets: snow lies all year
		boolean frozenAllYear( int wx, int wy, int a ){
			return WorldModel.alpineTemperature( WorldModel.sample( seed, wx, wy, WorldModel.SUMMER_SHIFT, smp ), a ) < WorldModel.FREEZE;
		}

		private boolean nextTo( int wx, int wy, int b ){
			return band( wx - 1, wy ) == b || band( wx + 1, wy ) == b || band( wx, wy - 1 ) == b || band( wx, wy + 1 ) == b;
		}

		//the cliff steps down from slice a (its EXIT, WindowGenerator.mountain)
		boolean stairDown( int wx, int wy, int a ){
			return band( wx, wy ) == a && nextTo( wx, wy, a - 1 ) && WindowGenerator.stairHash( seed, wx, wy, a - 1 );
		}

		//...and up from it (its ENTRANCE, carved in the rock of the band above)
		boolean stairUp( int wx, int wy, int a ){
			return band( wx, wy ) == a + 1 && nextTo( wx, wy, a ) && WindowGenerator.stairHash( seed, wx, wy, a );
		}

		boolean way( int wx, int wy, int a ){
			return stairDown( wx, wy, a ) || stairUp( wx, wy, a );
		}

		//a tunnel's floor through the rock of slice a
		boolean tunnel( int wx, int wy, int a ){
			return band( wx, wy ) > a && WorldModel.tunnelAt( seed, wx, wy, a );
		}
	}

	/**
	 * Can a site lay the rect [x0, x1] x [y0, y1] of slice a? Every cell is the slice's own
	 * ground with no tarn, no way between the slices stands within MARGIN of it, and a site that
	 * builds walls keeps them a cell off any tunnel through the rock.
	 */
	private static boolean fits( Probe p, int a, int x0, int y0, int x1, int y1, boolean walls ){
		for (int y = y0; y <= y1; y++){
			for (int x = x0; x <= x1; x++){
				if (p.band( x, y ) != a) return false;
			}
		}
		for (int y = y0; y <= y1; y++){
			for (int x = x0; x <= x1; x++){
				if (WorldModel.tarnAt( p.seed, x, y, a )) return false;
			}
		}
		for (int y = y0 - MARGIN; y <= y1 + MARGIN; y++){
			for (int x = x0 - MARGIN; x <= x1 + MARGIN; x++){
				if (p.way( x, y, a )) return false;
			}
		}
		if (walls){
			for (int y = y0 - 1; y <= y1 + 1; y++){
				for (int x = x0 - 1; x <= x1 + 1; x++){
					if (p.tunnel( x, y, a )) return false;
				}
			}
		}
		return true;
	}

	//plain ground of slice a for a single cell, with no way beside it
	private static boolean ground( Probe p, int a, int wx, int wy ){
		return fits( p, a, wx, wy, wx, wy, false );
	}

	// ------------------------------------------------------------ the caches

	private static long cachedSeed = Long.MIN_VALUE;
	private static final List<ConcurrentHashMap<Long, Site>> ROLLED = new ArrayList<>();
	private static final ConcurrentHashMap<Long, Site> SUMMITS = new ConcurrentHashMap<>();
	//each sector's lattice of bands (see lattice)
	private static final ConcurrentHashMap<Long, byte[]> LATTICE = new ConcurrentHashMap<>();
	static {
		for (int a = 0; a <= WorldLayers.MAX_ABOVE; a++) ROLLED.add( new ConcurrentHashMap<>() );
	}

	/** A new world drops every resolved site (tests time the resolution cold this way). */
	static synchronized void checkSeed( long seed ){
		if (seed != cachedSeed){
			cachedSeed = seed;
			for (ConcurrentHashMap<Long, Site> m : ROLLED) m.clear();
			SUMMITS.clear();
			LATTICE.clear();
		}
	}

	private static long sectorKey( int sx, int sy ){
		return (((long) sx) << 32) | (sy & 0xFFFFFFFFL);
	}

	private static Site cached( ConcurrentHashMap<Long, Site> m, long key, Site computed ){
		if (m.size() > CACHE_CAP) m.clear();
		Site was = m.putIfAbsent( key, computed == null ? NONE : computed );
		Site s = was != null ? was : computed;
		return s == NONE ? null : s;
	}

	/** The bands of a sector's 8x8 lattice, (sx*64 + 4 + 8i, sy*64 + 4 + 8j) at i + 8j: what a
	 *  summit is climbed from, and which slices' ground a sector shows at all. */
	static byte[] lattice( long seed, int sx, int sy, Probe p ){
		long key = sectorKey( sx, sy );
		byte[] l = LATTICE.get( key );
		if (l != null) return l;
		l = new byte[64];
		for (int j = 0; j < 8; j++){
			for (int i = 0; i < 8; i++){
				l[i + 8 * j] = (byte) p.band( sx * SECTOR + 4 + 8 * i, sy * SECTOR + 4 + 8 * j );
			}
		}
		if (LATTICE.size() > CACHE_CAP) LATTICE.clear();
		byte[] was = LATTICE.putIfAbsent( key, l );
		return was != null ? was : l;
	}

	// ------------------------------------------------------------ rolled sites

	/** The site a sector of slice a holds, or null. */
	public static Site siteOf( long seed, int sx, int sy, int a ){
		return siteOf( seed, sx, sy, a, new Probe( seed ) );
	}

	static Site siteOf( long seed, int sx, int sy, int a, Probe p ){
		if (a < 1 || a > WorldLayers.MAX_ABOVE) return null;
		checkSeed( seed );
		ConcurrentHashMap<Long, Site> m = ROLLED.get( a );
		long key = sectorKey( sx, sy );
		Site s = m.get( key );
		if (s != null) return s == NONE ? null : s;
		return cached( m, key, roll( seed, sx, sy, a, p ) );
	}

	private static Site roll( long seed, int sx, int sy, int a, Probe p ){
		long h = WindowGenerator.dressHash( seed, sx, sy, 0x51AE5L ^ (a * 0x9E3779B97F4A7C15L) );
		if (Math.floorMod( h, 100L ) >= SITE_CHANCE) return null;
		int total = 0;
		for (Kind k : Kind.values()){
			if (k.weight > 0 && k.lo <= a && a <= k.hi) total += k.weight;
		}
		if (total == 0) return null;
		int pick = (int) Math.floorMod( h >>> 8, (long) total );
		Kind kind = null;
		for (Kind k : Kind.values()){
			if (k.weight <= 0 || k.lo > a || a > k.hi) continue;
			if (pick < k.weight){
				kind = k;
				break;
			}
			pick -= k.weight;
		}
		//a sector holds a slice's site only where that slice's ground shows on its lattice
		boolean shows = false;
		for (byte b : lattice( seed, sx, sy, p )) shows |= b == a;
		if (!shows) return null;
		//the anchor spots in a hashed order, the first that takes the site holding it
		int n = SPOTS * SPOTS;
		long start = WindowGenerator.dressHash( seed, sx, sy, 0x51D0L + a * 31L );
		for (int k = 0; k < n; k++){
			int i = (int) Math.floorMod( start + k * 97L, (long) n );
			int px = sx * SECTOR + JITTER0 + STEP * (i % SPOTS);
			int py = sy * SECTOR + JITTER0 + STEP * (i / SPOTS);
			Site s = validate( kind, seed, a, px, py, p );
			if (s == null) continue;
			//a rolled site keeps three cells off its sector's summit cairn on the same slice
			Site top = summitOf( seed, sx, sy, p );
			if (top != null && top.altitude == a && top.wx >= s.x0 - 3 && top.wx <= s.x1 + 3
					&& top.wy >= s.y0 - 3 && top.wy <= s.y1 + 3) continue;
			return s;
		}
		return null;
	}

	private static Site validate( Kind kind, long seed, int a, int cx, int cy, Probe p ){
		//the cheapest reject first: the anchor stands on the slice's own ground
		if (p.band( cx, cy ) != a) return null;
		switch (kind){
			case HERMIT:
				//a hut by a garden, on the alpine meadow below the snow line (at the year's mean)
				if (!fits( p, a, cx - 3, cy - 3, cx + 7, cy + 3, true )) return null;
				if (p.frozenMean( cx, cy, a )) return null;
				return new Site( kind, a, cx, cy, 2, 0, 0, 0, cx - 3, cy - 3, cx + 7, cy + 3 );
			case EYRIE:
				return eyrie( seed, a, cx, cy, p );
			case SPRINGS: {
				if (!fits( p, a, cx - 4, cy - 4, cx + 4, cy + 4, false )) return null;
				if (!p.frozenMean( cx, cy, a )) return null;
				int bits = (int) (WindowGenerator.dressHash( seed, cx, cy, 0x5B1L ) & 15);
				return new Site( kind, a, cx, cy, 0, 0, 0, bits, cx - 4, cy - 4, cx + 4, cy + 4 );
			}
			case CLIMBER:
				if (!fits( p, a, cx - 1, cy - 1, cx + 2, cy + 1, false )) return null;
				//frozen in: the snow lies on him and his pack through midsummer
				if (!p.frozenAllYear( cx, cy, a ) || !p.frozenAllYear( cx + 1, cy, a )) return null;
				return new Site( kind, a, cx, cy, 0, 0, 0, 0, cx - 1, cy - 1, cx + 2, cy + 1 );
			case TOWER: {
				//on a ridge: the ground falls away nine cells off on both sides, one way or the other
				boolean ew = p.band( cx + 9, cy ) < a && p.band( cx - 9, cy ) < a;
				boolean ns = p.band( cx, cy + 9 ) < a && p.band( cx, cy - 9 ) < a;
				if (!ew && !ns) return null;
				if (!fits( p, a, cx - 3, cy - 3, cx + 3, cy + 3, true )) return null;
				int dir = (int) Math.floorMod( WindowGenerator.dressHash( seed, cx, cy, 0x70E3L ), 4L );
				return new Site( kind, a, cx, cy, dir, 0, 0, 0, cx - 3, cy - 3, cx + 3, cy + 3 );
			}
			case PASS:
				return pass( seed, a, cx, cy, p );
			default:
				return null;
		}
	}

	//the nest: the first cell spiralling out from the spot that stands on the lip of a drop -
	//its own ground with open air beside it - with ground behind it for the second eagle
	private static Site eyrie( long seed, int a, int px, int py, Probe p ){
		for (int r = 0; r <= 8; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int nx = px + dx, ny = py + dy;
					if (p.band( nx, ny ) != a) continue;
					int dir = -1;
					for (int d = 0; d < 4 && dir == -1; d++){
						if (p.band( nx + DX[d], ny + DY[d] ) < a) dir = d;
					}
					if (dir == -1) continue;
					int ix = nx - DX[dir], iy = ny - DY[dir];
					if (!fits( p, a, Math.min( nx, ix ), Math.min( ny, iy ), Math.max( nx, ix ), Math.max( ny, iy ), false )) continue;
					return new Site( Kind.EYRIE, a, nx, ny, dir, 0, 0, 0,
							Math.min( nx, ix ), Math.min( ny, iy ), Math.max( nx, ix ), Math.max( ny, iy ) );
				}
			}
		}
		return null;
	}

	//the waystation: a shelter on open ground five to nine cells from where a tunnel through the
	//rock comes out onto the slice, its doorway turned to the tunnel
	private static Site pass( long seed, int a, int cx, int cy, Probe p ){
		if (!fits( p, a, cx - 3, cy - 3, cx + 3, cy + 3, true )) return null;
		for (int r = 5; r <= 9; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int mx = cx + dx, my = cy + dy;
					if (!p.tunnel( mx, my, a )) continue;
					for (int d = 0; d < 4; d++){
						int qx = mx + DX[d], qy = my + DY[d];
						if (!ground( p, a, qx, qy )) continue;
						int dir = Math.abs( dx ) >= Math.abs( dy ) ? (dx > 0 ? 1 : 3) : (dy > 0 ? 2 : 0);
						return new Site( Kind.PASS, a, cx, cy, dir, qx, qy, 0,
								Math.min( cx - 3, qx ), Math.min( cy - 3, qy ), Math.max( cx + 3, qx ), Math.max( cy + 3, qy ) );
					}
				}
			}
		}
		return null;
	}

	// ------------------------------------------------------------ summits

	//the order of the land's cells: by elevation, the clamped tops told apart by a hash
	private static long rank( long seed, int wx, int wy ){
		return WindowGenerator.dressHash( seed, wx, wy, 0xCA1A5L );
	}

	private static boolean above( float e, long h, float be, long bh ){
		return e > be || (e == be && h > bh);
	}

	/**
	 * The summit a sector owns, with its cairn, or null. From the highest of an 8x8 lattice of the
	 * sector's cells, climb to the highest neighbour until none is higher, then check every cell
	 * within SUMMIT_R: the summit is higher than all of them (elevation, then a hash, so a peak
	 * whose top is clamped flat still has exactly one); a higher one found instead is climbed on
	 * from. The sector the climb ends in must be this one, the summit's band at least
	 * SUMMIT_MIN_BAND, and it no tarn and no way down: the cairn stands on the slice of its band.
	 */
	public static Site summitOf( long seed, int sx, int sy ){
		return summitOf( seed, sx, sy, new Probe( seed ) );
	}

	static Site summitOf( long seed, int sx, int sy, Probe p ){
		checkSeed( seed );
		long key = sectorKey( sx, sy );
		Site s = SUMMITS.get( key );
		if (s != null) return s == NONE ? null : s;
		return cached( SUMMITS, key, summit( seed, sx, sy, p ) );
	}

	private static Site summit( long seed, int sx, int sy, Probe p ){
		int best = 0;
		byte[] bands = lattice( seed, sx, sy, p );
		for (byte b : bands) best = Math.max( best, b );
		if (best < SUMMIT_MIN_BAND) return null;
		int cx = 0, cy = 0;
		float ce = -1f;
		long ch = 0;
		for (int j = 0; j < 8; j++){
			for (int i = 0; i < 8; i++){
				if (bands[i + 8 * j] != best) continue;
				int x = sx * SECTOR + 4 + 8 * i, y = sy * SECTOR + 4 + 8 * j;
				float e = p.elev( x, y );
				long h = rank( seed, x, y );
				if (ce < 0 || above( e, h, ce, ch )){
					cx = x; cy = y; ce = e; ch = h;
				}
			}
		}
		int steps = 0;
		while (true){
			//up the steepest way first
			boolean moved = true;
			while (moved){
				moved = false;
				int bx = cx, by = cy;
				float be = ce;
				long bh = ch;
				int cb = WorldLayers.band( ce );
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						if (dx == 0 && dy == 0) continue;
						int x = cx + dx, y = cy + dy;
						if (p.band( x, y ) < cb) continue;
						float e = p.elev( x, y );
						long h = rank( seed, x, y );
						if (above( e, h, be, bh )){
							bx = x; by = y; be = e; bh = h;
						}
					}
				}
				if (bx != cx || by != cy){
					cx = bx; cy = by; ce = be; ch = bh;
					moved = true;
					if (++steps >= CLIMB_CAP) return null;
				}
			}
			//then the whole top round it: a higher cell anywhere within reach goes on from there
			int cb = WorldLayers.band( ce );
			int bx = cx, by = cy;
			float be = ce;
			long bh = ch;
			for (int dy = -SUMMIT_R; dy <= SUMMIT_R; dy++){
				for (int dx = -SUMMIT_R; dx <= SUMMIT_R; dx++){
					if (dx == 0 && dy == 0) continue;
					int x = cx + dx, y = cy + dy;
					if (p.band( x, y ) < cb) continue;
					float e = p.elev( x, y );
					long h = rank( seed, x, y );
					if (above( e, h, be, bh )){
						bx = x; by = y; be = e; bh = h;
					}
				}
			}
			if (bx == cx && by == cy) break;
			cx = bx; cy = by; ce = be; ch = bh;
			if (++steps >= CLIMB_CAP) return null;
		}
		if (Math.floorDiv( cx, SECTOR ) != sx || Math.floorDiv( cy, SECTOR ) != sy) return null;
		int b = WorldLayers.band( ce );
		if (b < SUMMIT_MIN_BAND) return null;
		//no tarn on the top, and the cairn two cells off any way down
		if (WorldModel.tarnAt( seed, cx, cy, b )) return null;
		for (int dy = -MARGIN; dy <= MARGIN; dy++){
			for (int dx = -MARGIN; dx <= MARGIN; dx++){
				if (p.way( cx + dx, cy + dy, b )) return null;
			}
		}
		return new Site( Kind.CAIRN, b, cx, cy, 0, feet( ce ), 0, 0, cx - 1, cy - 1, cx + 1, cy + 1 );
	}

	/** A summit's height above the sea in feet, to the hundred: the mountain line (0.70) is some
	 *  six and a half thousand, the highest tops (1.0) twelve. */
	public static int feet( float elev ){
		return Math.round( (elev - WorldModel.SEA) * 180f ) * 100;
	}

	// ------------------------------------------------------------ in a window

	/**
	 * The sites of slice a whose reach meets the world rect [x0, x1] x [y0, y1]: each sector's
	 * rolled site, then its summit's cairn when that stands on this slice. In sector order.
	 */
	public static ArrayList<Site> sitesIn( long seed, int a, int x0, int y0, int x1, int y1 ){
		return sitesIn( seed, a, x0, y0, x1, y1, new Probe( seed ) );
	}

	static ArrayList<Site> sitesIn( long seed, int a, int x0, int y0, int x1, int y1, Probe p ){
		ArrayList<Site> out = new ArrayList<>();
		if (a < 1) return out;
		//a summit's cairn clears a cell round it, which may be the next sector's
		int sx0 = Math.floorDiv( x0 - 1, SECTOR ), sx1 = Math.floorDiv( x1 + 1, SECTOR );
		int sy0 = Math.floorDiv( y0 - 1, SECTOR ), sy1 = Math.floorDiv( y1 + 1, SECTOR );
		for (int sy = sy0; sy <= sy1; sy++){
			for (int sx = sx0; sx <= sx1; sx++){
				Site s = siteOf( seed, sx, sy, a, p );
				if (s != null && touches( s, x0, y0, x1, y1 )) out.add( s );
				Site top = summitOf( seed, sx, sy, p );
				if (top != null && top.altitude == a && touches( top, x0, y0, x1, y1 )) out.add( top );
			}
		}
		return out;
	}

	/**
	 * WindowGenerator.mountain's last pass: every site of the window laid into its terrain (the
	 * interior only), the spring pools' water depth and thawed ground, the hut's garden as a
	 * field; the window keeps the list (Window.sites). Reads the window's own bands and elevations.
	 */
	static void lay( long seed, int a, int ox, int oy, WindowGenerator.Window w ){
		long t = LagMonitor.begin();
		Probe p = new Probe( seed, w.band, w.elev, ox, oy );
		final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
		ArrayList<Site> sites = sitesIn( seed, a, ox + 1, oy + 1, ox + W - 2, oy + H - 2, p );
		for (Site s : sites){
			for (int wy = s.y0; wy <= s.y1; wy++){
				for (int wx = s.x0; wx <= s.x1; wx++){
					int x = wx - ox, y = wy - oy;
					if (x < 1 || y < 1 || x > W - 2 || y > H - 2) continue;
					layCell( seed, s, wx, wy, x + y * W, w, p, sites );
				}
			}
		}
		w.sites = Collections.unmodifiableList( sites );
		LagMonitor.end( "MountainSites.lay", t );
	}

	//the scatter a site's ground is cleared of: rocks, pines and bushes become the snow or the
	//grass they stood in
	private static int clearScatter( int t, boolean frozenHere ){
		if (t == Terrain.BOULDER || t == Terrain.TREE_PINE || t == Terrain.SHRUB || t == Terrain.MINE_BOULDER){
			return frozenHere ? Terrain.SNOW : Terrain.GRASS;
		}
		return t;
	}

	private static int bare( boolean frozenHere ){
		return frozenHere ? Terrain.SNOW : Terrain.EMPTY;
	}

	private static void layCell( long seed, Site s, int wx, int wy, int c, WindowGenerator.Window w, Probe p, List<Site> all ){
		int[] t = w.terrain;
		boolean frozenHere = w.frozen[c];
		int dx = wx - s.wx, dy = wy - s.wy;
		int r = Math.max( Math.abs( dx ), Math.abs( dy ) );
		int solid = solidAt( seed, s, wx, wy );
		if (solid != -1){
			t[c] = solid;
			return;
		}
		switch (s.kind){
			case HERMIT:
				if (r <= 1){
					t[c] = Terrain.EMPTY_SP;
				} else if (dx == 0 && dy == 3){
					t[c] = Terrain.DIRT_PATH;
				} else if (dx >= 4 && dx <= 6 && dy >= 0 && dy <= 1){
					//the hermit's garden
					t[c] = Terrain.FURROWED_GRASS;
					w.field[c] = true;
				} else {
					t[c] = clearScatter( t[c], frozenHere );
				}
				break;
			case EYRIE:
				//the nest on the lip, and the ground behind it
				t[c] = wx == s.wx && wy == s.wy ? bare( frozenHere ) : clearScatter( t[c], frozenHere );
				break;
			case SPRINGS:
				if (inPool( s, wx, wy )){
					t[c] = Terrain.WATER;
					w.waterDepth[c] = 1;
					w.frozen[c] = false;
				} else if (dx * dx + dy * dy <= 16){
					//the snow melted off round the pools: the meadow under it
					int m = WorldModel.alpineMeadow( seed, wx, wy, s.altitude );
					if ((m == Terrain.BOULDER || m == Terrain.SHRUB) && besidePool( s, wx, wy )) m = Terrain.GRASS;
					//flowers keep a square of their own against the snow: none on the ring's rim
					if (m == Terrain.FLOWER_PATCH && dx * dx + dy * dy > 9) m = Terrain.GRASS;
					t[c] = m;
					w.frozen[c] = false;
				}
				break;
			case CLIMBER:
				t[c] = (dx == 0 || dx == 1) && dy == 0 ? bare( frozenHere ) : clearScatter( t[c], frozenHere );
				break;
			case TOWER:
				//roofless: the snow lies in the ring as round it (the stump of its stair marks the
				//top cell, dressFittings)
				t[c] = r <= 2 ? (frozenHere ? Terrain.SNOW : Terrain.EMPTY_DECO) : clearScatter( t[c], frozenHere );
				break;
			case PASS:
				if (r <= 2){
					int[] hearth = hearthCell( seed, s );
					t[c] = wx == hearth[0] && wy == hearth[1] ? Terrain.EMBERS : Terrain.EMPTY_SP;
				} else if (r == 3){
					t[c] = dx == 3 * DX[s.dir] && dy == 3 * DY[s.dir] ? Terrain.DIRT_PATH : clearScatter( t[c], frozenHere );
				} else {
					//the path on from the shelter to the pass: a beaten track over the slice's own
					//ground, never onto a tarn, a way down or another site
					if (onRoute( s, wx, wy ) && ground( p, s.altitude, wx, wy ) && !inAnyRect( all, s, wx, wy )){
						t[c] = Terrain.DIRT_PATH;
					}
				}
				break;
			case CAIRN:
				if (wx == s.wx && wy == s.wy){
					t[c] = bare( frozenHere );
				} else if (Math.abs( dx ) + Math.abs( dy ) == 1 && ground( p, s.altitude, wx, wy )){
					t[c] = clearScatter( t[c], frozenHere );
				}
				break;
		}
	}

	private static boolean besidePool( Site s, int wx, int wy ){
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if (inPool( s, wx + dx, wy + dy )) return true;
			}
		}
		return false;
	}

	//is the cell laid by another site? (a shelter's own reach runs out along its path: only its
	//shell and yard count)
	private static boolean inAnyRect( List<Site> all, Site self, int wx, int wy ){
		for (Site o : all){
			if (o == self) continue;
			boolean in = o.kind == Kind.PASS
					? Math.abs( wx - o.wx ) <= 3 && Math.abs( wy - o.wy ) <= 3
					: inReach( o, wx, wy );
			if (in) return true;
		}
		return false;
	}

	//a cell of the line from a shelter's path, three cells out of its doorway, to the pass
	private static boolean onRoute( Site s, int wx, int wy ){
		int x0 = s.wx + 3 * DX[s.dir], y0 = s.wy + 3 * DY[s.dir];
		int x1 = s.ax, y1 = s.ay;
		int dx = Math.abs( x1 - x0 ), dy = -Math.abs( y1 - y0 );
		int stx = x0 < x1 ? 1 : -1, sty = y0 < y1 ? 1 : -1;
		int err = dx + dy, x = x0, y = y0;
		while (true){
			if (x == wx && y == wy) return !(x == x0 && y == y0);
			if (x == x1 && y == y1) return false;
			int e2 = 2 * err;
			if (e2 >= dy){ err += dy; x += stx; }
			if (e2 <= dx){ err += dx; y += sty; }
		}
	}

	/**
	 * The sites' fittings in a window's dressing (WindowGenerator.dress): the hermit's windows and
	 * chimney - the village house's own (SettlementLights) - the waystation's chimney and fire
	 * ring, the eyrie's nest, the climber's pack. From the window's list, by its live map.
	 */
	static void dressFittings( long seed, int ox, int oy, int[] map, WindowGenerator.Window base,
			int[] ground, int[] edges, int[] canopy ){
		final int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
		for (Object laid : base.sites){
			if (!(laid instanceof Site)) continue;
			Site s = (Site) laid;
			switch (s.kind){
				case HERMIT: {
					int style = OverworldDress.VILLAGE_WINDOWS[SettlementLights.windowStyle( seed, s.wx, s.wy )];
					for (int[] o : SettlementLights.HOUSE_WINDOWS){
						int c = SettlementLights.glassCell( seed, map, W, H, ox, oy, s.wx + o[0], s.wy + o[1] );
						if (c != -1) ground[c] = style;
					}
					int c = SettlementLights.chimneyCell( seed, map, W, H, ox, oy, s.wx, s.wy );
					if (c != -1) canopy[c] = OverworldDress.VILLAGE_CHIMNEY;
					break;
				}
				case PASS: {
					int c = chimneyCell( seed, s, map, W, H, ox, oy );
					if (c != -1) canopy[c] = OverworldDress.VILLAGE_CHIMNEY;
					int[] h = hearthCell( seed, s );
					int hc = cellIn( h[0], h[1], ox, oy );
					if (hc != -1 && map[hc] == Terrain.EMBERS) edges[hc] = OverworldDress.GNOLL_HEARTH;
					break;
				}
				case EYRIE: {
					int c = cellIn( s.wx, s.wy, ox, oy );
					if (c != -1 && passable( map[c] )) edges[c] = base.frozen[c] ? OverworldDress.NEST_SNOW : OverworldDress.NEST;
					break;
				}
				case CLIMBER: {
					int c = cellIn( s.wx + 1, s.wy, ox, oy );
					if (c != -1 && passable( map[c] )) edges[c] = OverworldDress.CLIMBER_PACK;
					break;
				}
				case TOWER: {
					//what is left of the stair: where to stand for the view (OverworldLevel.mountainStep)
					int c = cellIn( s.wx, s.wy, ox, oy );
					if (c != -1 && passable( map[c] )) edges[c] = OverworldDress.TOWER_STAIR;
					break;
				}
				default:
			}
		}
	}

	private static boolean passable( int t ){
		return (Terrain.flags[t] & Terrain.PASSABLE) != 0 && (Terrain.flags[t] & Terrain.LIQUID) == 0;
	}

	//a window interior cell of a world cell, or -1
	private static int cellIn( int wx, int wy, int ox, int oy ){
		int x = wx - ox, y = wy - oy;
		if (x < 1 || y < 1 || x > WindowGenerator.WIDTH - 2 || y > WindowGenerator.HEIGHT - 2) return -1;
		return x + y * WindowGenerator.WIDTH;
	}

	/** A window's spring heat: 2 on a hot spring's water, 1 on the ground within two cells of it, 0 elsewhere. */
	public static byte[] springHeat( List<Site> sites, int ox, int oy ){
		byte[] heat = new byte[WindowGenerator.WIDTH * WindowGenerator.HEIGHT];
		for (Site s : sites){
			if (s.kind != Kind.SPRINGS) continue;
			int[] pool = poolCells( s );
			for (int i = 0; i < pool.length; i += 2){
				for (int dy = -2; dy <= 2; dy++){
					for (int dx = -2; dx <= 2; dx++){
						int c = cellIn( pool[i] + dx, pool[i+1] + dy, ox, oy );
						if (c != -1 && heat[c] == 0) heat[c] = 1;
					}
				}
			}
			for (int i = 0; i < pool.length; i += 2){
				int c = cellIn( pool[i], pool[i+1], ox, oy );
				if (c != -1) heat[c] = 2;
			}
		}
		return heat;
	}

	// ------------------------------------------------------------ names and keys

	/** Does a site of slice a build solid terrain (a wall, a door, a fallen stone, a sign) on this cell? */
	public static boolean builtAt( long seed, int a, int wx, int wy ){
		for (Site s : sitesIn( seed, a, wx, wy, wx, wy )){
			if (solidAt( seed, s, wx, wy ) != -1) return true;
		}
		return false;
	}

	/** The string key of a built wall on a world cell, from a window's sites: "hut_wall" for the
	 *  hermit's hut and a waystation, "tower_wall" for a watchtower's ring, null for the rock. */
	public static String wallKey( long seed, List<Site> sites, int wx, int wy ){
		for (Site s : sites){
			if (!inReach( s, wx, wy ) || solidAt( seed, s, wx, wy ) != Terrain.WALL) continue;
			return s.kind == Kind.TOWER ? "tower_wall" : "hut_wall";
		}
		return null;
	}

	/** The string key naming a site's cell (an eyrie's nest, a waystation's hearth, a spring's
	 *  water, a tower's top), from a window's sites, or null. */
	public static String cellKey( long seed, List<Site> sites, int wx, int wy, int terrain ){
		for (Site s : sites){
			if (!inReach( s, wx, wy )) continue;
			switch (s.kind){
				case EYRIE:
					if (wx == s.wx && wy == s.wy) return "nest";
					break;
				case PASS: {
					int[] h = hearthCell( seed, s );
					if (wx == h[0] && wy == h[1] && terrain == Terrain.EMBERS) return "hearth";
					break;
				}
				case SPRINGS:
					if (terrain == Terrain.WATER && inPool( s, wx, wy )) return "spring";
					break;
				case TOWER:
					if (wx == s.wx && wy == s.wy) return "tower_top";
					break;
				default:
			}
		}
		return null;
	}

	/** Is this world cell a waystation's signpost? The site, or null. */
	public static Site signOf( long seed, List<Site> sites, int wx, int wy ){
		for (Site s : sites){
			if (s.kind != Kind.PASS) continue;
			int[] sign = signCell( seed, s );
			if (sign[0] == wx && sign[1] == wy) return s;
		}
		return null;
	}

	private static final String[] PASS_A = { "Grey", "Wind", "Hawk", "Frost", "Raven", "Iron",
			"Cold", "High", "Eagle", "Stone", "Thorn", "Wolf" };
	private static final String[] PASS_B = { "Gap", "Notch", "Saddle", "Col", "Pass", "Gate" };

	/** A pass's name, by its waystation (a proper noun, like a village's). */
	public static String passName( long seed, int wx, int wy ){
		long h = WindowGenerator.dressHash( seed, wx, wy, 0x9A55L );
		return PASS_A[(int) Math.floorMod( h, (long) PASS_A.length )] + " "
				+ PASS_B[(int) Math.floorMod( h >>> 8, (long) PASS_B.length )];
	}

	/** What the world map calls a site. */
	public static String mapName( long seed, Kind kind, int wx, int wy ){
		return kind == Kind.PASS ? passName( seed, wx, wy ) : Messages.get( MountainSites.class, "map_" + kind.lower() );
	}

	// ------------------------------------------------------------ rumours

	/**
	 * The places a hermit at home on (homeX, homeY) of slice a knows of: every site of the slices
	 * within two of his, in his sector and the ones round it, and the summits there - never his
	 * own. Resolves sites: the actor thread only (Hermit.interact), and cached by the hermit.
	 */
	static ArrayList<Site> rumourCandidates( long seed, int a, int homeX, int homeY, long ownKey ){
		ArrayList<Site> out = new ArrayList<>();
		int hsx = Math.floorDiv( homeX, SECTOR ), hsy = Math.floorDiv( homeY, SECTOR );
		for (int sy = hsy - 1; sy <= hsy + 1; sy++){
			for (int sx = hsx - 1; sx <= hsx + 1; sx++){
				for (int alt = Math.max( 1, a - 2 ); alt <= Math.min( WorldLayers.MAX_ABOVE, a + 2 ); alt++){
					Site s = siteOf( seed, sx, sy, alt );
					if (s != null && s.key != ownKey) out.add( s );
				}
				Site top = summitOf( seed, sx, sy );
				if (top != null && top.key != ownKey) out.add( top );
			}
		}
		return out;
	}

	/** ...and the places under the mountain he has heard of: the first slice of the caves' (CaveSites)
	 *  in his sector and the ones round it. */
	static ArrayList<CaveSites.Site> caveCandidates( long seed, int homeX, int homeY ){
		ArrayList<CaveSites.Site> out = new ArrayList<>();
		int csx = Math.floorDiv( homeX, CaveSites.SECTOR ), csy = Math.floorDiv( homeY, CaveSites.SECTOR );
		for (int sy = csy - 1; sy <= csy + 1; sy++){
			for (int sx = csx - 1; sx <= csx + 1; sx++){
				CaveSites.Site s = CaveSites.site( seed, -1, sx, sy );
				if (s != null) out.add( s );
			}
		}
		return out;
	}

	/** The day's rumour of a hermit: one of his candidates - the heights' places, the caves' under
	 *  them, the richest vein of his slice near his home (Ores.notableVein) - the same all day, told
	 *  with its bearing, its distance in paces and, for a place of the heights, its height from his. */
	public static String rumour( long seed, int a, int homeX, int homeY, long ownKey, int day ){
		long t = LagMonitor.begin();
		ArrayList<Site> all = rumourCandidates( seed, a, homeX, homeY, ownKey );
		ArrayList<CaveSites.Site> caves = caveCandidates( seed, homeX, homeY );
		int[] vein = Ores.notableVein( seed, a, homeX, homeY, VEIN_REACH );
		int n = all.size() + caves.size() + (vein != null ? 1 : 0);
		String text;
		if (n == 0){
			text = Messages.get( MountainSites.class, "rumour_none" );
		} else {
			int i = (int) Math.floorMod( OverworldLevel.structHash( ownKey, day ), (long) n );
			if (i < all.size()){
				text = rumourOf( seed, all.get( i ), a, homeX, homeY );
			} else if (i < all.size() + caves.size()){
				CaveSites.Site c = caves.get( i - all.size() );
				int dx = c.cx - homeX, dy = c.cy - homeY;
				text = Messages.get( MountainSites.class, "rumour_cave", Messages.get( MountainSites.class, "dir_" + compass( dx, dy ) ),
						paces( dx, dy ), CaveSites.phrase( c.type ) );
			} else {
				int dx = vein[0] - homeX, dy = vein[1] - homeY;
				text = Messages.get( MountainSites.class, "rumour_vein", Messages.get( MountainSites.class, "dir_" + compass( dx, dy ) ),
						paces( dx, dy ), Messages.get( Ores.Kind.values()[vein[2]].item, "name" ) );
			}
		}
		LagMonitor.end( "MountainSites.rumour", t );
		return text;
	}

	/** how far round his home a hermit knows the rock: the miners' own reach (CaveSites.rumours) */
	static final int VEIN_REACH = 48;

	static String rumourOf( long seed, Site s, int a, int homeX, int homeY ){
		int dx = s.seeX - homeX, dy = s.seeY - homeY;
		String dir = Messages.get( MountainSites.class, "dir_" + compass( dx, dy ) );
		int paces = paces( dx, dy );
		String height = heightPhrase( s.altitude - a );
		if (s.kind == Kind.PASS){
			return Messages.get( MountainSites.class, "rumour_pass", dir, paces, height, passName( seed, s.wx, s.wy ) );
		}
		return Messages.get( MountainSites.class, "rumour_" + s.kind.lower(), dir, paces, height );
	}

	private static final String[] COMPASS = { "n", "ne", "e", "se", "s", "sw", "w", "nw" };

	/** The eight-way bearing of an offset (y runs south), as a dir_ key suffix. */
	static String compass( int dx, int dy ){
		double ang = Math.atan2( dx, -dy );
		int k = (int) Math.floorMod( Math.round( ang / (Math.PI / 4) ), 8L );
		return COMPASS[k];
	}

	/** A distance told in paces: to the nearest ten, never under ten. */
	static int paces( int dx, int dy ){
		return Math.max( 10, (int) Math.round( Math.sqrt( (double) dx * dx + (double) dy * dy ) / 10.0 ) * 10 );
	}

	private static String heightPhrase( int d ){
		if (d == 0) return Messages.get( MountainSites.class, "height_same" );
		if (d == 1) return Messages.get( MountainSites.class, "height_up_1" );
		if (d > 1) return Messages.get( MountainSites.class, "height_up", d );
		if (d == -1) return Messages.get( MountainSites.class, "height_down_1" );
		return Messages.get( MountainSites.class, "height_down", -d );
	}

	// ------------------------------------------------------------ debug and tooling

	//how long nearest() looks before it gives up (debug scenes run it off the render thread)
	public static final int NEAREST_SECONDS = 45;

	/**
	 * Debug scenes and tests: the site of a kind nearest a world cell, searching outward ring by
	 * ring of sectors up to maxRing and taking the nearest of the first ring holding one; null when
	 * there is none or the search ran past NEAREST_SECONDS.
	 */
	public static Site nearest( long seed, Kind kind, int fromWX, int fromWY, int maxRing ){
		long deadline = System.currentTimeMillis() + NEAREST_SECONDS * 1000L;
		int fsx = Math.floorDiv( fromWX, SECTOR ), fsy = Math.floorDiv( fromWY, SECTOR );
		Probe p = new Probe( seed );
		for (int r = 0; r <= maxRing; r++){
			Site best = null;
			long bestD = Long.MAX_VALUE;
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					if (System.currentTimeMillis() > deadline) return null;
					int sx = fsx + dx, sy = fsy + dy;
					ArrayList<Site> found = new ArrayList<>();
					if (kind == Kind.CAIRN){
						//a sector with no mountain anywhere on a 3x3 grid of it is not climbed
						boolean hills = false;
						for (int k = 0; k < 9 && !hills; k++){
							hills = p.band( sx * SECTOR + 11 + 21 * (k % 3), sy * SECTOR + 11 + 21 * (k / 3) ) >= 1;
						}
						if (!hills) continue;
						Site top = summitOf( seed, sx, sy, p );
						if (top != null) found.add( top );
					} else {
						for (int a = kind.lo; a <= kind.hi; a++){
							Site s = siteOf( seed, sx, sy, a, p );
							if (s != null && s.kind == kind) found.add( s );
						}
					}
					for (Site s : found){
						long d = (long) (s.wx - fromWX) * (s.wx - fromWX) + (long) (s.wy - fromWY) * (s.wy - fromWY);
						if (d < bestD){
							bestD = d;
							best = s;
						}
					}
				}
			}
			if (best != null) return best;
		}
		return null;
	}

	/**
	 * Where a debug scene stands the hero to look at a site, world {x, y}: a spot the player would
	 * walk up to it from - before the hut's door, inland of the eyrie out of its eagles' reach,
	 * below the summit, in the snow by the springs, before the tower's gap, out on the shelter's
	 * path - moved on along the same line to the first plain ground of the slice (never rock: an
	 * arrival in rock carves it), then any such ground round the site.
	 */
	public static int[] standCell( long seed, Site s ){
		Probe p = new Probe( seed );
		int a = s.altitude;
		int sx, sy, ux, uy;
		switch (s.kind){
			case HERMIT:  sx = s.wx; sy = s.wy + 5; ux = 0; uy = 1; break;
			case EYRIE:   sx = s.wx - DX[s.dir] * 6; sy = s.wy - DY[s.dir] * 6; ux = -DX[s.dir]; uy = -DY[s.dir]; break;
			case SPRINGS: sx = s.wx; sy = s.wy + 6; ux = 0; uy = 1; break;
			case CLIMBER: sx = s.wx; sy = s.wy + 3; ux = 0; uy = 1; break;
			case TOWER:   sx = s.wx + DX[s.dir] * 5; sy = s.wy + DY[s.dir] * 5; ux = DX[s.dir]; uy = DY[s.dir]; break;
			case PASS:    sx = s.wx + DX[s.dir] * 5; sy = s.wy + DY[s.dir] * 5; ux = DX[s.dir]; uy = DY[s.dir]; break;
			default:      sx = s.wx; sy = s.wy + 3; ux = 0; uy = 1; break;
		}
		for (int k = 0; k <= 8; k++){
			int x = sx + ux * k, y = sy + uy * k;
			if (standable( seed, p, s, a, x, y )) return new int[]{ x, y };
		}
		for (int r = 3; r <= 12; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					if (standable( seed, p, s, a, s.wx + dx, s.wy + dy )) return new int[]{ s.wx + dx, s.wy + dy };
				}
			}
		}
		return new int[]{ sx, sy };
	}

	private static boolean standable( long seed, Probe p, Site s, int a, int x, int y ){
		if (!ground( p, a, x, y ) || builtAt( seed, a, x, y )) return false;
		//out of the eagles' reach of their nest
		return s.kind != Kind.EYRIE || Math.max( Math.abs( x - s.wx ), Math.abs( y - s.wy ) ) > 3;
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.audio;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Comfy;
import xyz.gabriwar.warpedpixeldungeon.levels.CrabBossLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.TownChurchLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.ambience.Place;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

import java.util.Arrays;
import java.util.Locale;

/**
 * Where the hero hears the game's effects from, for room acoustics (RoomAcoustics): the space
 * round him as a sound finds it, and from that the reverb it gives a sound, whether a sound
 * reaches him clear, through one door or through rock, and where an echo comes back from.
 *
 * The space is the open cells a sound gets to from him with no door between, walking all eight
 * ways over the 33 by 33 cells round him (never squeezing between two walls that meet at a
 * corner): how many there are, and how many of their sides touch a wall or a door. Few cells, or
 * few for their walls (a corridor, a tunnel), ring like a small room; more like a hall; a big
 * space of rock, or one filling most of the window, like a cavern; the open sky not at all.
 * Bookshelves, snow, grass and rugs soak a tail up, water brightens it, and each place has its own
 * colour. The doors round that space lead to the next, and theirs to a third; a sound the open
 * ground brings round only the long way is heard through a door that is nearer.
 *
 * Reads the level's map, nothing else, from any thread: worked out on the first placed sound after
 * the level or the hero's cell changes, or after half a second (doors, digging, the places that
 * write the map straight), and kept whole and unchanging till then. Never touches the dungeon's
 * seeded Random or PathFinder.
 */
public final class Earshot {

	private Earshot(){}

	/** The space round the hero, as a sound hears it. */
	public enum Space { OPEN, CHURCH, HOUSE, TIGHT, ROOM, LARGE, HUGE }

	/** The reverb a space gives a sound: none, a small room's, a big hall's or a cavern's. */
	public enum Tail { NONE, ROOM, HALL, CAVERN }

	/** How a sound reaches the hero: clear, through a closed door, or through rock. */
	public enum Relation { CLEAR, DOOR, WALL }

	/** How far round the hero (cells each way) a sound is followed: past it, the straight line decides. */
	public static final int RADIUS = 16;
	static final int SIDE = 2 * RADIUS + 1, CELLS = SIDE * SIDE;
	//a snapshot this old (s) is worked out again for the next sound
	static final double MAX_AGE = 0.5;
	//a way round this many cells longer than the straight line is as good as a wall
	static final int DETOUR = 6;
	//a space of stone this big rings as long as a cavern: two thirds of the window, 27 by 27 and up,
	//twice as wide as the smallest that rings as a hall (13 by 13) and so near twice as long
	static final int VAST = 700;
	//one trim for every tail's level, dB
	static final float TRIM_DB = 0f;

	//what a terrain is to a sound: it passes, it passes muffled (one door), or it stops
	static final byte OPEN_CELL = 0, DOOR_CELL = 1, WALL_CELL = 2;
	static final byte[] CLASS = new byte[256];
	//how much of a sound the floor of an open cell soaks up: 0 (stone, water) to 1 (snow)
	static final float[] ABSORB = new float[256];
	static final boolean[] WATER = new boolean[256];
	static {
		//a hidden door is a wall till it is found: never given away by ear
		for (int t : new int[]{ Terrain.WALL, Terrain.WALL_DECO, Terrain.SECRET_DOOR, Terrain.TOWN_SOLID,
				Terrain.BOOKSHELF, Terrain.EMPTY_BOOKSHELF }) CLASS[t] = WALL_CELL;
		for (int t : new int[]{ Terrain.DOOR, Terrain.LOCKED_DOOR, Terrain.HERO_LKD_DR, Terrain.CRYSTAL_DOOR,
				Terrain.BARRICADE, Terrain.LOCKED_EXIT }) CLASS[t] = DOOR_CELL;
		ABSORB[Terrain.SNOW] = 1f;
		for (int t : new int[]{ Terrain.HIGH_GRASS, Terrain.FURROWED_GRASS, Terrain.SOIL_CORNWHEAT, Terrain.SOIL_GREENWHEAT,
				Terrain.SOIL_STRAWWHEAT, Terrain.SOIL_WATERWHEAT, Terrain.WOOL_RUG, Terrain.TREE_PINE, Terrain.TREE_OAK }) ABSORB[t] = 0.7f;
		ABSORB[Terrain.CHASM] = 0.5f;
		for (int t : new int[]{ Terrain.GRASS, Terrain.FLOWER_PATCH, Terrain.MUSHROOM_PATCH }) ABSORB[t] = 0.35f;
		for (int t : new int[]{ Terrain.WATER, Terrain.DEEP_WATER, Terrain.CUSTOM_DECO_WTR }) WATER[t] = true;
	}

	/** Seconds, for a snapshot's age: a test may set its own. */
	interface Clock {
		double now();
	}

	static Clock clock = () -> System.nanoTime() / 1e9;

	private static final Object LOCK = new Object();
	private static volatile Snapshot last;

	//the walk's scratch, under LOCK
	private static final int[] terrain = new int[CELLS];
	private static final byte[] kind = new byte[CELLS];
	private static final int[] dist = new int[CELLS];
	private static final int[] queue = new int[CELLS];
	private static final int[] doorsIn = new int[CELLS], doorsOut = new int[CELLS];
	//the doors round the hero's space, nearest first, and how far each cell is by way of one of them
	private static final int[] spaceDoors = new int[CELLS], doorDist = new int[CELLS];
	private static final short[] doorVia = new short[CELLS];

	private static final int[] DX = { -1, 0, 1, -1, 1, -1, 0, 1 };
	private static final int[] DY = { -1, -1, -1, 0, 0, 1, 1, 1 };
	//of those, the four that share a side
	private static final int[] SIDES = { 1, 3, 4, 6 };
	private static final byte UNREACHED = (byte) (3 << 6);

	/**
	 * The space round the hero standing on `hero`, as it is now (at most half a second old), or
	 * null when it cannot be told: no level, the hero off its map, or the map between two frames
	 * (OverworldLevel's window moving).
	 */
	public static Snapshot of( Level level, int hero ){
		if (level == null || hero < 0 || hero >= level.length() || level.fogHeld()) return null;
		double now = clock.now();
		Snapshot s = last;
		if (s != null && s.fresh( level, hero, now )) return s;
		synchronized (LOCK){
			s = last;
			if (s != null && s.fresh( level, hero, now )) return s;
			s = work( level, hero, now );
			last = s;
			return s;
		}
	}

	/** Is the hero under the open sky: the overworld's surface and heights (out of a shelter), the
	 *  postgame field and shore, the crab's beach. Never by the tiles, which alias the sewers'. */
	static boolean openAir( Level level, int hero ){
		if (level instanceof OverworldLevel){
			OverworldLevel ow = (OverworldLevel) level;
			return ow.openSky() && !ow.shelterAt( hero );
		}
		Place p = level.ambience();
		return p == Place.MEADOW || p == Place.SHORE || level instanceof CrabBossLevel;
	}

	/** Whose sound a level has: its place, or, with none (a boss floor, a guest's copy of a floor),
	 *  the place its tiles are only ever used for. The sewers', the city's and the halls' tiles also
	 *  stand for the forest, the beach, the town, the vault and more (Assets): those tell nothing. */
	static Place family( Level level ){
		Place p = level.ambience();
		if (p != null) return p;
		String t = level.tilesTex();
		if (t == null) return null;
		if (t.equals( Assets.Environment.TILES_CAVES ) || t.equals( Assets.Environment.TILES_CAVES_CRYSTAL )
				|| t.equals( Assets.Environment.TILES_CAVES_GNOLL )) return Place.CAVES;
		if (t.equals( Assets.Environment.TILES_FROZEN )) return Place.FROZEN;
		if (t.equals( Assets.Environment.TILES_SPIDERNEST )) return Place.NEST;
		if (t.equals( Assets.Environment.TILES_PRISON )) return Place.PRISON;
		if (t.equals( Assets.Environment.TILES_TEMPLE )) return Place.TEMPLE;
		return null;
	}

	//what a place does to a tail, dB
	private static float colour( Place p ){
		if (p == null) return 0f;
		switch (p){
			case SEWERS: case CAVES: case MINES: case CAVE_SLICES: case CITY: case HALLS:
				return 1f;
			case FROZEN:
				return 2f;
			case NEST:
				return -3f;
			default:
				return 0f;
		}
	}

	private static byte classOf( int t ){
		return t >= 0 && t < 256 ? CLASS[t] : WALL_CELL;
	}

	/**
	 * The worst a sound meets on the straight line from one cell to another, the two ends not
	 * counted: any wall makes it WALL, doors alone DOOR. The line one way and the line back can
	 * differ by a cell where they cross a diagonal: a caller wanting the fairer takes the better.
	 */
	static Relation line( int[] map, int width, int from, int to ){
		if (from == to) return Relation.CLEAR;
		int x = from % width, y = from / width, x1 = to % width, y1 = to / width;
		int dx = Math.abs( x1 - x ), dy = -Math.abs( y1 - y ), sx = x < x1 ? 1 : -1, sy = y < y1 ? 1 : -1;
		int err = dx + dy;
		Relation worst = Relation.CLEAR;
		while (true){
			int e2 = 2 * err;
			if (e2 >= dy){ err += dy; x += sx; }
			if (e2 <= dx){ err += dx; y += sy; }
			if (x == x1 && y == y1) return worst;
			byte c = classOf( map[x + y * width] );
			if (c == WALL_CELL) return Relation.WALL;
			if (c == DOOR_CELL) worst = Relation.DOOR;
		}
	}

	private static float pan( float dx ){
		return Math.max( -SpatialSound.MAX_PAN, Math.min( SpatialSound.MAX_PAN, dx / SpatialSound.PAN_CELLS ) );
	}

	private static byte cellInfo( int doors, int path ){
		return (byte) ((doors << 6) | Math.min( path, 63 ));
	}

	//works a snapshot out, under LOCK; null when the level's map is not all there
	private static Snapshot work( Level level, int hero, double now ){
		int[] map = level.map;
		int w = level.width(), h = level.height();
		if (map == null || w <= 0 || h <= 0 || map.length < w * h || hero >= w * h) return null;
		int hx = hero % w, hy = hero / w, ox = hx - RADIUS, oy = hy - RADIUS;

		for (int wy = 0; wy < SIDE; wy++){
			int y = oy + wy;
			for (int wx = 0; wx < SIDE; wx++){
				int x = ox + wx, i = wx + wy * SIDE;
				terrain[i] = x < 0 || y < 0 || x >= w || y >= h ? -1 : map[x + y * w];
				kind[i] = classOf( terrain[i] );
			}
		}

		//the walk: the hero's space (no door), then what lies through one door, then through two;
		//each from the doors the one before met, nearest first
		byte[] info = new byte[CELLS];
		short[] via = new short[CELLS];
		Arrays.fill( info, UNREACHED );
		Arrays.fill( via, (short) -1 );
		Arrays.fill( dist, -1 );
		int centre = RADIUS + RADIUS * SIDE;
		int[] in = doorsIn, out = doorsOut;
		int nIn = 0, nSpaceDoors = 0;
		for (int layer = 0; layer <= 2; layer++){
			int head = 0, tail = 0, s = 0, nOut = 0;
			if (layer == 0){
				dist[centre] = 0;
				info[centre] = 0;
				queue[tail++] = centre;
			}
			while (head < tail || s < nIn){
				int c = s < nIn && (head >= tail || dist[in[s]] <= dist[queue[head]]) ? in[s++] : queue[head++];
				int cx = c % SIDE, cy = c / SIDE, d = dist[c] + 1;
				for (int k = 0; k < 8; k++){
					int nx = cx + DX[k], ny = cy + DY[k];
					if (nx < 0 || ny < 0 || nx >= SIDE || ny >= SIDE) continue;
					int n = nx + ny * SIDE;
					byte nk = kind[n];
					if (dist[n] >= 0 || nk == WALL_CELL || (nk == DOOR_CELL && layer == 2)) continue;
					//no squeezing between two walls (or doors) that meet at a corner
					if (DX[k] != 0 && DY[k] != 0 && kind[nx + cy * SIDE] != OPEN_CELL && kind[cx + ny * SIDE] != OPEN_CELL) continue;
					dist[n] = d;
					if (nk == DOOR_CELL){
						info[n] = cellInfo( layer + 1, d );
						via[n] = layer == 0 ? (short) n : via[c];
						out[nOut++] = n;
					} else {
						info[n] = cellInfo( layer, d );
						via[n] = via[c];
						queue[tail++] = n;
					}
				}
			}
			if (layer == 0){
				System.arraycopy( out, 0, spaceDoors, 0, nOut );
				nSpaceDoors = nOut;
			}
			int[] t = in;
			in = out;
			out = t;
			nIn = nOut;
		}

		//the hero's space: its cells, the sides of them the walls and doors show it (not the ones
		//the window cuts off), what of those is bookshelf, and its floor
		OverworldLevel ow = level instanceof OverworldLevel ? (OverworldLevel) level : null;
		int area = 0, faces = 0, soft = 0, wet = 0;
		float soak = 0f;
		for (int i = 0; i < CELLS; i++){
			if (dist[i] < 0 || (info[i] & UNREACHED) != 0) continue;
			area++;
			int t = terrain[i], x = i % SIDE, y = i / SIDE;
			float a = t >= 0 && t < 256 ? ABSORB[t] : 0f;
			if (ow != null && ow.frozenAt( ox + x + (oy + y) * w )) a = 1f;
			soak += a;
			if (t >= 0 && t < 256 && WATER[t]) wet++;
			for (int k : SIDES){
				int nx = x + DX[k], ny = y + DY[k];
				if (nx < 0 || ny < 0 || nx >= SIDE || ny >= SIDE) continue;
				int n = nx + ny * SIDE;
				if (kind[n] == OPEN_CELL) continue;
				faces++;
				if (terrain[n] == Terrain.BOOKSHELF || terrain[n] == Terrain.EMPTY_BOOKSHELF) soft++;
			}
		}
		throughDoors( info, via, nSpaceDoors );

		float softShare = faces == 0 ? 0f : soft / (float) faces;
		float absorb = soak / area, water = wet / (float) area;
		float thin = faces == 0 ? Float.POSITIVE_INFINITY : area / (float) faces;

		boolean openAir = openAir( level, hero );
		Place family = family( level );
		boolean rock = family == Place.CAVES || family == Place.MINES || family == Place.CAVE_SLICES;
		Space space;
		Tail tail;
		float base;
		if (openAir){
			space = Space.OPEN;
			tail = Tail.NONE;
			base = 0f;
		} else if (level instanceof TownChurchLevel){
			space = Space.CHURCH;
			tail = Tail.HALL;
			base = -9f;
		} else if (Comfy.indoors( level ) || (ow != null && ow.shelterAt( hero ))){
			space = Space.HOUSE;
			tail = Tail.ROOM;
			base = -17f;
		} else if (area < 20 || thin < 0.9f){
			space = Space.TIGHT;
			tail = Tail.ROOM;
			base = -12f;
		} else if (area < 70){
			space = Space.ROOM;
			tail = rock ? Tail.HALL : Tail.ROOM;
			base = rock ? -12f : -11f;
		} else if (area < 160){
			space = Space.LARGE;
			tail = Tail.HALL;
			base = -11f;
		} else {
			space = Space.HUGE;
			//rock rings a size up, and stone as vast as a cavern as long. The cavern's tail spreads
			//its sound over near twice the hall's time: 2 dB under the hall's, the next blow half a
			//second on meets as much of it as of a hall's, and it rings on twice as long
			tail = rock || area >= VAST ? Tail.CAVERN : Tail.HALL;
			base = tail == Tail.CAVERN ? -10f : -8f;
		}
		float db = 0f;
		if (tail != Tail.NONE){
			db = base + colour( family ) - 6f * softShare - 8f * absorb + (water >= 0.3f ? 1f : 0f) + TRIM_DB;
			db = Math.max( -24f, Math.min( -6f, db ) );
			if (db < -20f) tail = Tail.NONE;
		}

		float[] echo = space == Space.OPEN ? skyEcho( map, w, h, hx, hy, absorb )
				: space == Space.HUGE && rock ? caveEcho( map, w, h, hx, hy, absorb ) : null;

		return new Snapshot( level, hero, now, map, w, h, ox, oy, info, via, kind.clone(),
				space, tail, db, rock, openAir, echo, area, faces, softShare, absorb, water );
	}

	//a cell of the hero's space the open ground reaches only the long way round (a wall's worth, see
	//DETOUR), that one door brings nearer, is heard through that door: two rooms a loop of open
	//ground joins still hear each other through the door between them. The second walk goes from the
	//doors round the space through open ground, the space's own too (never a second door), nearest
	//first: a way through a door and back out on the side it came from is never the shorter. Under LOCK
	private static void throughDoors( byte[] info, short[] via, int nDoors ){
		boolean longWay = false;
		for (int i = 0; i < CELLS && !longWay; i++) longWay = (info[i] & UNREACHED) == 0 && (info[i] & 63) - straight( i ) >= DETOUR;
		if (!longWay || nDoors == 0) return;
		Arrays.fill( doorDist, -1 );
		int head = 0, tail = 0, s = 0;
		while (head < tail || s < nDoors){
			int c;
			if (s < nDoors && (head >= tail || dist[spaceDoors[s]] <= doorDist[queue[head]])){
				c = spaceDoors[s++];
				doorDist[c] = dist[c];
				doorVia[c] = (short) c;
			} else {
				c = queue[head++];
			}
			int cx = c % SIDE, cy = c / SIDE, d = doorDist[c] + 1;
			for (int k = 0; k < 8; k++){
				int nx = cx + DX[k], ny = cy + DY[k];
				if (nx < 0 || ny < 0 || nx >= SIDE || ny >= SIDE) continue;
				int n = nx + ny * SIDE;
				if (kind[n] != OPEN_CELL || doorDist[n] >= 0) continue;
				if (DX[k] != 0 && DY[k] != 0 && kind[nx + cy * SIDE] != OPEN_CELL && kind[cx + ny * SIDE] != OPEN_CELL) continue;
				doorDist[n] = d;
				doorVia[n] = doorVia[c];
				queue[tail++] = n;
			}
		}
		for (int i = 0; i < CELLS; i++){
			if ((info[i] & UNREACHED) != 0 || doorDist[i] < 0 || doorDist[i] >= dist[i] || (info[i] & 63) - straight( i ) < DETOUR) continue;
			info[i] = cellInfo( 1, doorDist[i] );
			via[i] = doorVia[i];
		}
	}

	//how far a window cell is from the hero's, as the crow flies over the grid
	private static int straight( int i ){
		return Math.max( Math.abs( i % SIDE - RADIUS ), Math.abs( i / SIDE - RADIUS ) );
	}

	//16 rays from the hero's cell in half-cell steps, `reach` cells at most: the cell each stopped
	//on (-1 for none, or off the map) and how far its middle is from the hero's
	private static void rays( int[] map, int w, int h, int hx, int hy, int reach, boolean sky, int[] hit, float[] len ){
		for (int k = 0; k < 16; k++){
			double a = k * Math.PI / 8, cos = Math.cos( a ), sin = Math.sin( a );
			hit[k] = -1;
			len[k] = 0f;
			for (int step = 1; step <= 2 * reach; step++){
				int x = (int) Math.round( hx + 0.5 * step * cos ), y = (int) Math.round( hy + 0.5 * step * sin );
				if (x < 0 || y < 0 || x >= w || y >= h) break;
				int c = x + y * w;
				if (sky ? reflection( map[c] ) >= 0f : classOf( map[c] ) != OPEN_CELL){
					hit[k] = c;
					len[k] = (float) Math.hypot( x - hx, y - hy );
					break;
				}
			}
		}
	}

	//what a cell gives back of a sound under the open sky: rock most, the town's walls less, a
	//tree stops it and gives nothing; -1 lets it pass
	private static float reflection( int t ){
		switch (t){
			case Terrain.WALL: case Terrain.WALL_DECO: return 0.95f;
			case Terrain.TOWN_SOLID: return 0.8f;
			case Terrain.TREE_PINE: case Terrain.TREE_OAK: return 0f;
			default: return -1f;
		}
	}

	//the nearest face of what the rays met, 11 cells off or more: two or more rays side by side that
	//`in` takes, as far as the nearest of them. That ray, or -1 for none. The runs are walked from
	//just past a ray it does not take, so none is cut in two; when it takes every ray, from ray 0,
	//all the way round as one face
	private static int face( boolean[] in, float[] len ){
		int start = 0;
		for (int k = 0; k < 16; k++) if (!in[k]) start = (k + 1) % 16;
		float best = Float.MAX_VALUE;
		int bestRay = -1;
		int k = 0;
		while (k < 16){
			int run = 0, near = -1;
			while (k + run < 16 && in[(start + k + run) % 16]){
				int r = (start + k + run) % 16;
				if (near < 0 || len[r] < len[near]) near = r;
				run++;
			}
			if (run >= 2 && len[near] >= 11f && len[near] < best){
				best = len[near];
				bestRay = near;
			}
			k += Math.max( run, 1 );
		}
		return bestRay;
	}

	//an echo off a rock face under the open sky: the nearest face 11 cells off or more of what gives
	//sound back. { delay s, level, pan }, or null
	private static float[] skyEcho( int[] map, int w, int h, int hx, int hy, float absorb ){
		int[] hit = new int[16];
		float[] len = new float[16];
		rays( map, w, h, hx, hy, 40, true, hit, len );
		float[] give = new float[16];
		boolean[] gives = new boolean[16];
		for (int k = 0; k < 16; k++){
			give[k] = hit[k] < 0 ? 0f : reflection( map[hit[k]] );
			gives[k] = give[k] > 0f;
		}
		int ray = face( gives, len );
		if (ray < 0) return null;
		return new float[]{
				Math.max( 0.10f, Math.min( 0.40f, 0.00875f * len[ray] ) ),
				0.25f * give[ray] * (1f - 0.6f * absorb),
				0.7f * pan( hit[ray] % w - hx ) };
	}

	//an echo off the far rock of a big rock space: its nearest face 11 to 24 cells off, rays side by
	//side that each meet a wall or a door that far. The near walls round the hero (most of them, in
	//the caves' tunnels and chambers) ring in the tail; a far face answers as well.
	//{ delay s, level, pan }, or null
	private static float[] caveEcho( int[] map, int w, int h, int hx, int hy, float absorb ){
		int[] hit = new int[16];
		float[] len = new float[16];
		rays( map, w, h, hx, hy, 24, false, hit, len );
		boolean[] far = new boolean[16];
		for (int k = 0; k < 16; k++) far[k] = hit[k] >= 0 && len[k] >= 11f;
		int ray = face( far, len );
		if (ray < 0) return null;
		return new float[]{
				Math.max( 0.10f, Math.min( 0.25f, 0.00875f * len[ray] ) ),
				0.16f * (1f - 0.6f * absorb),
				0.5f * pan( hit[ray] % w - hx ) };
	}

	/** The space round the hero at one moment: whole, never changed, safe to read from any thread. */
	public static final class Snapshot {

		public final Space space;
		public final Tail tail;
		/** the tail's level against its dry sound, dB (no tail under -20) */
		public final float level;
		/** in a cave, a mine or the overworld's caves; under the open sky */
		public final boolean rock, openAir;
		/** the echo: its delay (s, 0 for none), its level against the dry sound and its pan, as
		 *  spatial sound would have it (RoomAcoustics centres it while spatial sound is off) */
		public final float echoDelay, echoLevel, echoPan;
		/** the hero's space: its cells, the sides of them that touch a wall or a door, the share of
		 *  those that are bookshelves, how much its floor soaks up and the share of it under water */
		public final int area, faces;
		public final float soft, absorb, water;

		private final Level owner;
		private final int hero;
		private final double made;
		private final int[] map;
		private final int width, height, ox, oy;
		private final byte[] info, kind;
		private final short[] via;

		private Snapshot( Level owner, int hero, double made, int[] map, int width, int height, int ox, int oy,
		                  byte[] info, short[] via, byte[] kind, Space space, Tail tail, float level, boolean rock,
		                  boolean openAir, float[] echo, int area, int faces, float soft, float absorb, float water ){
			this.owner = owner; this.hero = hero; this.made = made; this.map = map;
			this.width = width; this.height = height; this.ox = ox; this.oy = oy;
			this.info = info; this.via = via; this.kind = kind;
			this.space = space; this.tail = tail; this.level = level; this.rock = rock; this.openAir = openAir;
			this.echoDelay = echo == null ? 0f : echo[0];
			this.echoLevel = echo == null ? 0f : echo[1];
			this.echoPan = echo == null ? 0f : echo[2];
			this.area = area; this.faces = faces; this.soft = soft; this.absorb = absorb; this.water = water;
		}

		boolean fresh( Level level, int hero, double now ){
			return owner == level && this.hero == hero && now >= made && now - made < MAX_AGE;
		}

		/** How a sound on this cell reaches the hero. One standing in a wall or a door (digging, a
		 *  door opening) is heard from its side the hero hears best. Past the cells the snapshot
		 *  covers, the straight line between them decides. */
		public Relation relation( int cell ){
			if (cell < 0 || cell >= width * height) return Relation.CLEAR;
			int i = window( cell );
			if (i < 0) return beyond( cell );
			i = judged( i );
			return i < 0 ? Relation.WALL : judge( i );
		}

		/** The door a sound on this cell is heard through, -1 when it is not heard through one door. */
		public int door( int cell ){
			if (cell < 0 || cell >= width * height) return -1;
			int i = window( cell );
			if (i < 0) return -1;
			i = judged( i );
			if (i < 0 || (info[i] >> 6 & 3) != 1 || via[i] < 0) return -1;
			return ox + via[i] % SIDE + (oy + via[i] / SIDE) * width;
		}

		private int window( int cell ){
			int x = cell % width - ox, y = cell / width - oy;
			return x < 0 || y < 0 || x >= SIDE || y >= SIDE ? -1 : x + y * SIDE;
		}

		//the cell a sound on window cell i is judged from: itself, or for one in a wall or a door
		//its neighbour the hero hears best; -1 when none of them is reached
		private int judged( int i ){
			if (kind[i] == OPEN_CELL || i == RADIUS + RADIUS * SIDE) return i;
			int best = -1;
			int x = i % SIDE, y = i / SIDE;
			for (int k = 0; k < 8; k++){
				int nx = x + DX[k], ny = y + DY[k];
				if (nx < 0 || ny < 0 || nx >= SIDE || ny >= SIDE) continue;
				int n = nx + ny * SIDE;
				if (info[n] == UNREACHED) continue;
				if (best < 0 || judge( n ).ordinal() < judge( best ).ordinal()
						|| (judge( n ) == judge( best ) && (info[n] & 63) < (info[best] & 63))) best = n;
			}
			return best;
		}

		private Relation judge( int i ){
			int doors = info[i] >> 6 & 3;
			if (doors == 0) return (info[i] & 63) - straight( i ) < DETOUR ? Relation.CLEAR : Relation.WALL;
			return doors == 1 ? Relation.DOOR : Relation.WALL;
		}

		private Relation beyond( int cell ){
			Relation a = line( map, width, cell, hero ), b = line( map, width, hero, cell );
			return a.ordinal() <= b.ordinal() ? a : b;
		}

		/** One line for a log: the space, what it is measured at, its tail and its echo. */
		public String describe(){
			return String.format( Locale.ENGLISH,
					"%s%s%s: %d cells, %d faces (%.2f a face), soft %.2f, absorb %.2f, water %.0f%%; %s; %s",
					space, rock ? ", rock" : "", openAir ? ", open sky" : "", area, faces,
					faces == 0 ? 0f : area / (float) faces, soft, absorb, 100f * water,
					tail == Tail.NONE ? String.format( Locale.ENGLISH, "no tail (%.1f dB)", level )
							: String.format( Locale.ENGLISH, "%s tail at %.1f dB", tail, level ),
					echoDelay <= 0f ? "no echo" : String.format( Locale.ENGLISH, "echo %.2f s at %.1f dB, pan %+.2f",
							echoDelay, 20 * Math.log10( echoLevel ), echoPan ) );
		}
	}
}

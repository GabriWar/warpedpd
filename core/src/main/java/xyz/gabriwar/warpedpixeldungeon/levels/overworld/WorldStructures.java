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

/**
 * The deterministic civilization layer over the wilderness: villages, the
 * roads that join them, and ruined dungeon entrances. Like the terrain, every
 * query is a pure function of (seed, wx, wy) so structures stream with the
 * window.
 *
 * Sites live on a jittered sector grid (one candidate per SECTOR x SECTOR
 * world cells); a candidate becomes a real village only if it lands on
 * friendly ground (plains/meadow, in a flat elevation band). Roads run from
 * each village to its nearest neighbouring village, bent by noise so they
 * wander like footpaths; they ford rivers but stop at ocean and mountain.
 */
public class WorldStructures {

	public static final int SECTOR = 96;

	// ------------------------------------------------------------- the town

	//the town is part of the world: its 32x32 layout (TownLayouts.TOWN_LAYOUT_REMIXED)
	//sits centred on world origin, so world cell (TOWN_X0 + lx, TOWN_Y0 + ly) is
	//layout cell (lx, ly). Walkable through any of the layout's openings; the
	//OverworldLevel paints the town's own art over it, wires its doorways and
	//stairs, and keeps its folk
	public static final int TOWN_SIZE = 32;
	public static final int TOWN_X0 = -TOWN_SIZE/2, TOWN_Y0 = -TOWN_SIZE/2;

	//layout cells with a fixed meaning (see TownLevel history)
	public static final int TOWN_PLAZA      = 752;  //where you arrive (new game, journal, beacon): two south of the plaza guards, on the town's axis
	public static final int TOWN_MINE_GATE     = 899;  //the plaza staircase, down into the kupua mines
	public static final int TOWN_DUNGEON_GATE  = 141;  //the north gate: the dungeon's front door (floor 1)
	public static final int TOWN_TEMPLE_DOOR = 412;
	public static final int TOWN_ALTAR      = 350;  //norn-stone altar
	//doorways into the building interiors (branch 6), indexed by interior depth 1..6
	public static final int[] TOWN_DOORS = { -1, 296, 490, 146, 530, 440, 340 };

	/** Layout cell of a world cell, or -1 outside the town. */
	public static int townCell( int wx, int wy ){
		int lx = wx - TOWN_X0, ly = wy - TOWN_Y0;
		if (lx < 0 || ly < 0 || lx >= TOWN_SIZE || ly >= TOWN_SIZE) return -1;
		return lx + ly * TOWN_SIZE;
	}

	public static int townWorldX( int cell ){ return TOWN_X0 + cell % TOWN_SIZE; }
	public static int townWorldY( int cell ){ return TOWN_Y0 + cell / TOWN_SIZE; }

	//every cell a player has to walk through to use the town: the doorways,
	//the stairs, the gate, the arrival tile - and a two-cell apron around each,
	//so nothing ever sprouts in a gateway or in front of one
	private static boolean townApproach( int wx, int wy ){
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				int c = townCell( wx + dx, wy + dy );
				if (c == -1) continue;
				if (c == TOWN_MINE_GATE || c == TOWN_DUNGEON_GATE || c == TOWN_TEMPLE_DOOR
						|| c == TOWN_PLAZA || c == TOWN_ALTAR) return true;
				for (int d : TOWN_DOORS) if (c == d) return true;
			}
		}
		return false;
	}

	//town footprint terrain, or -1 outside it. building walls are solid but
	//artless (TOWN_SOLID): fog, wall-blocking and the raised/overhang passes
	//all leave them alone, so nothing pokes through the authored layers
	private static int townTerrain( long seed, int wx, int wy ){
		int cell = townCell( wx, wy );
		if (cell == -1) return -1;
		if (cell == TOWN_MINE_GATE) return Terrain.EXIT;
		if (cell == TOWN_DUNGEON_GATE || cell == TOWN_TEMPLE_DOOR) return Terrain.DOOR;
		if (cell == TOWN_ALTAR) return Terrain.PEDESTAL;
		for (int d : TOWN_DOORS) if (cell == d) return Terrain.DOOR;
		int t = xyz.gabriwar.warpedpixeldungeon.levels.TownLayouts.TOWN_LAYOUT_REMIXED[cell];
		//snow ground in the art is the world's own snow, and the authored tree
		//rows are RANDOMIZED: some thin out, fresh pines seed the open snow -
		//the tree line stops reading as ruler-straight rows
		long h = hash( seed ^ 0x73EE5EEDL, wx, wy );
		if (t == Terrain.WALL){
			if (xyz.gabriwar.warpedpixeldungeon.tiles.TownRemixedTiles.outdoor( cell )){
				//never by a gateway (a pine there hangs over the arch) and never on
				//the town's side of the tree line: the wood stays outside
				if (townApproach( wx, wy )
						|| xyz.gabriwar.warpedpixeldungeon.tiles.TownRemixedTiles.bordersInside( cell )){
					return Terrain.SNOW;
				}
				return (h & 3) == 0 ? Terrain.SNOW : Terrain.TREE_PINE;
			}
			//the ruin around the mine staircase: crumbled blocks vanish under
			//the snow, a boulder of masonry surviving here and there
			if (xyz.gabriwar.warpedpixeldungeon.tiles.TownRemixedTiles.brokenNearMine( cell )){
				return (h & 7) == 0 ? Terrain.BOULDER : Terrain.SNOW;
			}
			return Terrain.TOWN_SOLID;
		}
		if (xyz.gabriwar.warpedpixeldungeon.tiles.TownRemixedTiles.snowGround( cell )){
			//nothing sprouts on the roads, nothing ever blocks a doorway, and the
			//streets inside the fence stay clear: the wood is what surrounds the town
			return (h & 15) == 0 && t == Terrain.EMPTY && !townApproach( wx, wy )
					&& !xyz.gabriwar.warpedpixeldungeon.tiles.TownRemixedTiles.insideWalls( cell )
					? Terrain.TREE_PINE : Terrain.SNOW;
		}
		return t;
	}

	// ---------------------------------------------------------- names

	//seed-anchored settlement names: syllables picked by the sector hash, so
	//every game (and every map, sign and journal entry) agrees on them
	private static final String[] NAME_A = { "Bel", "Cor", "Dun", "Eld", "Fal", "Gran", "Hol", "Iron",
			"Kes", "Lorn", "Mar", "Nor", "Oak", "Pell", "Quar", "Rav", "Stone", "Thorn", "Umber", "Wyn" };
	private static final String[] NAME_B = { "a", "e", "i", "o", "u", "ar", "en", "il", "or", "un" };
	private static final String[] NAME_C = { "burg", "by", "dale", "fell", "ford", "gate", "ham", "haven",
			"hollow", "mere", "moor", "mouth", "stead", "ton", "watch", "wick" };

	/** The name of the settlement in this sector. */
	public static String villageName( long seed, int sx, int sy ){
		long h = hash( seed ^ 0x9ABE5L, sx, sy );
		String a = NAME_A[(int)Math.floorMod( h, NAME_A.length )];
		String c = NAME_C[(int)Math.floorMod( h >> 8, NAME_C.length )];
		//half the names take a middle syllable
		if (((h >> 16) & 1) == 0){
			return a + NAME_B[(int)Math.floorMod( h >> 20, NAME_B.length )] + c;
		}
		return a + c;
	}

	//the road mesh's town node: the point just outside the town's east opening
	static float townRoadX(){ return TOWN_X0 + TOWN_SIZE + 2; }
	static float townRoadY(){ return TOWN_Y0 + 23; }

	//what kind of site a sector hosts
	public enum Site { NONE, VILLAGE, RUIN, DRAGON, CAMP, STONES, BIGTREE, DUNGEON }

	//who lives in a settlement
	public enum Faction { HUMAN, GNOLL, BANDIT }

	/** The faction of the settlement in this sector. */
	public static Faction faction( long seed, int sx, int sy ){
		long h = hash( seed ^ 0xFAC710L, sx, sy );
		int roll = (int)(h & 15);
		if (roll < 10) return Faction.HUMAN;
		if (roll < 14) return Faction.GNOLL;
		return Faction.BANDIT;
	}

	private static long hash( long seed, long x, long y ){
		long h = seed ^ 0x57A7C7E5L;
		h ^= x * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft( h, 31 );
		h ^= y * 0xC2B2AE3D27D4EB4FL;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		return h;
	}

	// ------------------------------------------------------------- sites

	/** World x of the site candidate of sector (sx, sy). */
	public static int siteX( long seed, int sx, int sy ){
		return sx * SECTOR + SECTOR/4 + (int)((hash( seed, sx, sy ) >>> 8) % (SECTOR/2));
	}

	public static int siteY( long seed, int sx, int sy ){
		return sy * SECTOR + SECTOR/4 + (int)((hash( seed, sx, sy ) >>> 24) % (SECTOR/2));
	}

	//sector lookups are pure functions but expensive (siteType runs the whole
	//biome pipeline); every cell consults dozens of sectors, so cache them.
	//keyed by sector coords - cleared when the seed changes
	private static long cachedSeed = Long.MIN_VALUE;
	private static final java.util.Map<Long, Site> siteCache = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.Map<Long, Long> nearCache = new java.util.concurrent.ConcurrentHashMap<>();

	private static long sectorKey( int sx, int sy ){
		return (((long)sx) << 32) | (sy & 0xFFFFFFFFL);
	}

	private static synchronized void checkSeed( long seed ){
		if (seed != cachedSeed){
			cachedSeed = seed;
			siteCache.clear();
			nearCache.clear();
			layoutCache.clear();
			segCache.clear();
			fieldCache.clear();
		}
	}

	/** What actually stands at this sector's site. */
	public static Site siteType( long seed, int sx, int sy ){
		checkSeed( seed );
		Long key = sectorKey( sx, sy );
		Site cached = siteCache.get( key );
		if (cached != null) return cached;
		Site result = computeSiteType( seed, sx, sy );
		siteCache.put( key, result );
		return result;
	}

	private static Site computeSiteType( long seed, int sx, int sy ){
		int wx = siteX( seed, sx, sy ), wy = siteY( seed, sx, sy );
		//nothing settles on the town's doorstep
		if (Math.abs( wx ) < WorldModel.TOWN_IN + 8 && Math.abs( wy ) < WorldModel.TOWN_IN + 8){
			return Site.NONE;
		}
		//the ANNUAL-MEAN biome: a village must not flicker in and out of
		//existence as the seasonal snow line crosses its sector (and this
		//answer is cached per seed - it has to be the same whenever asked)
		WorldModel.Biome b = WorldModel.baseBiomeAt( seed, wx, wy );

		long h = hash( seed ^ 0x1A11L, sx, sy );

		//villages settle friendly open ground
		if ((b == WorldModel.Biome.PLAINS || b == WorldModel.Biome.MEADOW)
				&& (h & 3) != 0){          //3 in 4 suitable sectors have one
			return Site.VILLAGE;
		}
		//ruined dungeon mouths favour wilder places
		if ((b == WorldModel.Biome.FOREST || b == WorldModel.Biome.FOOTHILLS
				|| b == WorldModel.Biome.TUNDRA || b == WorldModel.Biome.DESERT)
				&& (h & 7) == 0){          //1 in 8
			return Site.RUIN;
		}
		//dragons lair in the high wastes
		if ((b == WorldModel.Biome.FOOTHILLS || b == WorldModel.Biome.MOUNTAIN)
				&& (h & 15) == 0){         //1 in 16
			return Site.DRAGON;
		}
		//an abandoned camp in the woods, its owners long gone
		if ((b == WorldModel.Biome.FOREST || b == WorldModel.Biome.MEADOW)
				&& (h & 7) == 1){          //1 in 8
			return Site.CAMP;
		}
		//the great oak: one forest sector in sixteen grew a giant
		if (b == WorldModel.Biome.FOREST && (h & 15) == 2){
			return Site.BIGTREE;
		}
		//a circle of standing stones on the open wastes
		if ((b == WorldModel.Biome.PLAINS || b == WorldModel.Biome.TUNDRA
				|| b == WorldModel.Biome.SNOWFIELD)
				&& (h & 15) == 2){         //1 in 16
			return Site.STONES;
		}
		//a sealed barrow, one of the world's other dungeons (Delves): on dry, level land, in
		//one sector in six of those nothing else claimed. Its own hash, so no other site moves
		if (b != WorldModel.Biome.OCEAN && b != WorldModel.Biome.RIVER && b != WorldModel.Biome.BEACH
				&& b != WorldModel.Biome.SWAMP && b != WorldModel.Biome.MOUNTAIN
				&& Math.floorMod( hash( seed ^ 0xBA770L, sx, sy ), 6 ) == 0){
			return Site.DUNGEON;
		}
		return Site.NONE;
	}

	/** Is this world cell the stairway of a barrow (Site.DUNGEON)? Its transition is the
	 *  barrow's own (OverworldLevel), never a way between the world's slices. */
	public static boolean delveGate( long seed, int wx, int wy ){
		int sx = Math.floorDiv( wx, SECTOR ), sy = Math.floorDiv( wy, SECTOR );
		return siteType( seed, sx, sy ) == Site.DUNGEON
				&& siteX( seed, sx, sy ) == wx && siteY( seed, sx, sy ) == wy;
	}

	// ------------------------------------------------------------- roads

	//nearest village neighbour of the village in (sx, sy), among the 5x5
	//surrounding sectors; returns packed sector coords or Long.MIN_VALUE
	private static long nearestVillage( long seed, int sx, int sy ){
		checkSeed( seed );
		Long key = sectorKey( sx, sy );
		Long cached = nearCache.get( key );
		if (cached != null) return cached;
		long result = computeNearestVillage( seed, sx, sy );
		nearCache.put( key, result );
		return result;
	}

	private static long computeNearestVillage( long seed, int sx, int sy ){
		int bx = 0, by = 0;
		long best = Long.MAX_VALUE;
		int x0 = siteX( seed, sx, sy ), y0 = siteY( seed, sx, sy );
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				if (dx == 0 && dy == 0) continue;
				if (siteType( seed, sx+dx, sy+dy ) != Site.VILLAGE) continue;
				long ddx = siteX( seed, sx+dx, sy+dy ) - x0;
				long ddy = siteY( seed, sx+dx, sy+dy ) - y0;
				long d2 = ddx*ddx + ddy*ddy;
				if (d2 < best){
					best = d2; bx = sx+dx; by = sy+dy;
				}
			}
		}
		return best == Long.MAX_VALUE ? Long.MIN_VALUE : (((long)bx) << 32) | (by & 0xFFFFFFFFL);
	}

	//squared distance from p to segment a-b (no sqrt on the hot path)
	private static float segDistSq( float px, float py, float ax, float ay, float bx, float by ){
		float vx = bx-ax, vy = by-ay;
		float len2 = vx*vx + vy*vy;
		float t = len2 == 0 ? 0 : ((px-ax)*vx + (py-ay)*vy) / len2;
		t = t < 0 ? 0 : t > 1 ? 1 : t;
		float dx = px - (ax + vx*t), dy = py - (ay + vy*t);
		return dx*dx + dy*dy;
	}

	//road segments (ax,ay,bx,by per segment) that can pass through a sector's
	//neighbourhood, cached per sector - most wilderness sectors cache EMPTY,
	//which lets onRoad bail before evaluating any noise at all
	private static final java.util.Map<Long, float[]> segCache = new java.util.concurrent.ConcurrentHashMap<>();

	private static float[] roadSegments( long seed, int sx, int sy ){
		Long key = sectorKey( sx, sy );
		float[] cached = segCache.get( key );
		if (cached != null) return cached;

		java.util.ArrayList<float[]> segs = new java.util.ArrayList<>();
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				int vx = sx+dx, vy = sy+dy;
				if (siteType( seed, vx, vy ) != Site.VILLAGE) continue;
				long n = nearestVillage( seed, vx, vy );
				if (n == Long.MIN_VALUE) continue;
				int nx = (int)(n >> 32), ny = (int)n;
				segs.add( new float[]{
						siteX( seed, vx, vy ), siteY( seed, vx, vy ),
						siteX( seed, nx, ny ), siteY( seed, nx, ny ) } );
			}
		}
		//the town joins the mesh: every village within two sectors of the
		//origin also runs a road to the town's east door
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				int vx = sx+dx, vy = sy+dy;
				if (siteType( seed, vx, vy ) != Site.VILLAGE) continue;
				long vwx = siteX( seed, vx, vy ), vwy = siteY( seed, vx, vy );
				if (vwx*vwx + vwy*vwy < (long)(2.2f*SECTOR) * (long)(2.2f*SECTOR)){
					segs.add( new float[]{ vwx, vwy, townRoadX(), townRoadY() } );
				}
			}
		}

		float[] flat = new float[segs.size() * 4];
		for (int i = 0; i < segs.size(); i++){
			System.arraycopy( segs.get( i ), 0, flat, i*4, 4 );
		}
		segCache.put( key, flat );
		return flat;
	}

	/** Is this world cell on a road? */
	public static boolean onRoad( long seed, int wx, int wy ){
		return onRoad( seed, wx, wy, roadSegments( seed, Math.floorDiv( wx, SECTOR ), Math.floorDiv( wy, SECTOR ) ) );
	}

	//...against the road segments of the cell's sector (see roadSegments)
	private static boolean onRoad( long seed, int wx, int wy, float[] segs ){
		if (segs.length == 0) return false;   //no villages anywhere near: free
		//footpath wobble: the whole road field is sampled through a small warp
		float ox = 3.5f * (WorldModel.pathWobble( seed, wx, wy, 0 ) - 0.5f) * 2f;
		float oy = 3.5f * (WorldModel.pathWobble( seed, wx, wy, 1 ) - 0.5f) * 2f;
		float px = wx + ox, py = wy + oy;
		for (int i = 0; i < segs.length; i += 4){
			if (segDistSq( px, py, segs[i], segs[i+1], segs[i+2], segs[i+3] ) < 1.1f * 1.1f){
				return true;
			}
		}
		return false;
	}

	// --------------------------------------------------------- rendering

	/** Compat overload: computes the wilderness itself (preview tools). */
	public static int terrainAt( long seed, int wx, int wy ){
		return terrainAt( seed, wx, wy, WorldModel.wildTerrainAt( seed, wx, wy ) );
	}

	/**
	 * The sector lookups a window's cells share, resolved once per window: the
	 * per-cell path used to consult nine cached sector maps (boxing a Long each
	 * time) for every one of 31k cells. Built on the generating thread, read
	 * from any number of row workers.
	 */
	public static final class SectorView {
		final long seed;
		final int sx0, sy0, cols, rows;
		final Site[] types;
		final float[][] segments;
		final int[][] fields;
		SectorView( long seed, int sx0, int sy0, int sx1, int sy1 ){
			this.seed = seed;
			this.sx0 = sx0;
			this.sy0 = sy0;
			cols = sx1 - sx0 + 1;
			rows = sy1 - sy0 + 1;
			types = new Site[cols * rows];
			segments = new float[cols * rows][];
			fields = new int[cols * rows][];
			for (int sy = sy0; sy <= sy1; sy++){
				for (int sx = sx0; sx <= sx1; sx++){
					int i = (sx - sx0) + (sy - sy0) * cols;
					types[i] = siteType( seed, sx, sy );
					segments[i] = roadSegments( seed, sx, sy );
					fields[i] = types[i] == Site.VILLAGE ? fieldPlots( seed, sx, sy ) : NO_FIELDS;
				}
			}
		}
		int[] fields( int sx, int sy ){
			int x = sx - sx0, y = sy - sy0;
			if (x < 0 || y < 0 || x >= cols || y >= rows) return fieldPlots( seed, sx, sy );
			return fields[x + y * cols];
		}
		Site type( int sx, int sy ){
			int x = sx - sx0, y = sy - sy0;
			if (x < 0 || y < 0 || x >= cols || y >= rows) return siteType( seed, sx, sy );
			return types[x + y * cols];
		}
		float[] roads( int sx, int sy ){
			int x = sx - sx0, y = sy - sy0;
			if (x < 0 || y < 0 || x >= cols || y >= rows) return roadSegments( seed, sx, sy );
			return segments[x + y * cols];
		}
	}

	/** The view covering every sector whose sites can reach the world rect [wx0,wx1]x[wy0,wy1]. */
	public static SectorView view( long seed, int wx0, int wy0, int wx1, int wy1 ){
		return new SectorView( seed,
				Math.floorDiv( wx0, SECTOR ) - 1, Math.floorDiv( wy0, SECTOR ) - 1,
				Math.floorDiv( wx1, SECTOR ) + 1, Math.floorDiv( wy1, SECTOR ) + 1 );
	}

	/** The site whose built wall stands on a world cell (a village house's shell, a lair's rim),
	 *  or null when the wall there is the land's own rock. Examine text only, not a hot path. */
	public static Site wallSite( long seed, int wx, int wy ){
		int sx = Math.floorDiv( wx, SECTOR ), sy = Math.floorDiv( wy, SECTOR );
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				Site type = siteType( seed, sx+dx, sy+dy );
				if (type == Site.NONE) continue;
				if (siteTerrain( seed, sx+dx, sy+dy, type, wx, wy ) == Terrain.WALL) return type;
			}
		}
		return null;
	}

	/**
	 * The structure terrain at a world cell, or -1 for "no structure here".
	 * Overrides the wilderness except where the land itself forbids it. The
	 * caller passes the already-computed wilderness terrain so roads never
	 * trigger a second full generator evaluation.
	 */
	public static int terrainAt( long seed, int wx, int wy, int wild ){
		return terrainAt( seed, wx, wy, wild, null );
	}

	/** ...with the window's sector view, or null to consult the caches per cell. */
	public static int terrainAt( long seed, int wx, int wy, int wild, SectorView view ){
		int town = townTerrain( seed, wx, wy );
		if (town != -1) return town;
		int sx = Math.floorDiv( wx, SECTOR ), sy = Math.floorDiv( wy, SECTOR );
		//the site footprint of this or any adjacent sector can reach this cell
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				Site type = view != null ? view.type( sx+dx, sy+dy ) : siteType( seed, sx+dx, sy+dy );
				if (type == Site.NONE) continue;
				int t = siteTerrain( seed, sx+dx, sy+dy, type, wx, wy );
				//a village's fence is not driven into a lake: the water closes the ring there
				//(open or frozen alike, so the gap does not come and go with the seasons)
				if (t == Terrain.BARRICADE && (wild == Terrain.WATER || wild == Terrain.FROZEN_WATER)) continue;
				if (t != -1) return t;
			}
		}
		//the human villages' fields (fieldPlots): laid on dry, level farmland of the annual
		//mean, so they are ploughed over whatever a season grows on them (a cold winter's
		//puddles of ice, a warm summer's reeds and pools) - only a cliff never is
		if (wild != Terrain.WALL){
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int[] plots = view != null ? view.fields( sx+dx, sy+dy )
							: siteType( seed, sx+dx, sy+dy ) == Site.VILLAGE ? fieldPlots( seed, sx+dx, sy+dy ) : NO_FIELDS;
					if (inField( plots, wx, wy )) return Terrain.FURROWED_GRASS;
				}
			}
		}
		//roads stop at mountains and walk straight over ice; open water they
		//cross on plank bridges
		if (wild == Terrain.FROZEN_WATER || wild == Terrain.WALL){
			return -1;
		}
		float[] segs = view != null ? view.roads( sx, sy ) : roadSegments( seed, sx, sy );
		if (onRoad( seed, wx, wy, segs )){
			return wild == Terrain.WATER ? Terrain.BRIDGE : Terrain.DIRT_PATH;
		}
		//the town's tree line bleeds outward: a pine cloud on the surrounding
		//snow, dense by the walls and thinning to nothing over ~14 cells
		if (wild == Terrain.SNOW){
			int lx = Math.max( 0, Math.max( TOWN_X0 - wx, wx - (TOWN_X0 + TOWN_SIZE - 1) ) );
			int ly = Math.max( 0, Math.max( TOWN_Y0 - wy, wy - (TOWN_Y0 + TOWN_SIZE - 1) ) );
			int d = Math.max( lx, ly );
			if (d > 0 && d <= 14 && !townApproach( wx, wy ) && !onRoad( seed, wx, wy, segs )){
				long h = hash( seed ^ 0x7C10CDL, wx, wy );
				if ((int)Math.floorMod( h, 10 + d * 2 ) < 3){
					return Terrain.TREE_PINE;
				}
			}
		}
		return -1;
	}
	//village: huts on a ring around a well; ruin: broken stone shell
	private static int siteTerrain( long seed, int sx, int sy, Site type, int wx, int wy ){
		int cx = siteX( seed, sx, sy ), cy = siteY( seed, sx, sy );
		int dx = wx - cx, dy = wy - cy;

		if (type == Site.DRAGON){
			//a scorched lair: a rocky bowl with a hoard heart
			int adx = Math.abs( dx ), ady = Math.abs( dy );
			if (adx > 5 || ady > 5) return -1;
			if (adx <= 3 && ady <= 3) return Terrain.EMPTY_SP;
			//jagged rock rim with an entrance gap to the south
			if (dx == 0 && dy == 5) return Terrain.EMPTY;
			long hh = hash( seed ^ 0xD4A60L, wx, wy );
			if ((hh & 3) == 0) return Terrain.EMPTY;
			return Terrain.WALL;
		}

		if (type == Site.CAMP){
			//an abandoned camp: a cold firepit, a flattened sleeping patch,
			//and whatever its owners left behind (see OverworldLevel)
			int adx = Math.abs( dx ), ady = Math.abs( dy );
			if (adx > 2 || ady > 2) return -1;
			if (dx == 0 && dy == 0) return Terrain.EMBERS;
			if (adx <= 1 && ady <= 1) return Terrain.EMPTY_DECO;
			return -1;
		}

		if (type == Site.BIGTREE){
			//the great oak: a 3x3 trunk with a hollow opening south, standing
			//in its own ring of flowers and long grass
			int adx = Math.abs( dx ), ady = Math.abs( dy );
			if (adx > 2 || ady > 2) return -1;
			if (adx <= 1 && ady <= 1){
				return (dx == 0 && dy == 1) ? Terrain.EMPTY_DECO : Terrain.TREE_OAK;
			}
			return (hash( seed ^ 0xB160A5L, wx, wy ) & 3) == 0
					? Terrain.FLOWER_PATCH : Terrain.GRASS;
		}

		if (type == Site.STONES){
			//a circle of eight standing stones around a low altar
			int adx = Math.abs( dx ), ady = Math.abs( dy );
			if (adx > 2 || ady > 2) return -1;
			if (dx == 0 && dy == 0) return Terrain.PEDESTAL;
			if (adx == 2 || ady == 2){
				boolean stone = (dx == 0 || dy == 0) || (adx == 2 && ady == 2);
				return stone ? Terrain.STATUE : Terrain.EMPTY;
			}
			return Terrain.EMPTY;
		}

		if (type == Site.DUNGEON){
			//a sealed barrow: a square of dressed stone, open to the south, statues in its
			//corners and its stairway in the middle
			int adx = Math.abs( dx ), ady = Math.abs( dy );
			if (adx > 3 || ady > 3) return -1;
			if (dx == 0 && dy == 0) return Terrain.EXIT;
			if (adx == 3 || ady == 3) return (dx == 0 && dy == 3) ? Terrain.EMPTY_SP : Terrain.WALL_DECO;
			if (adx == 2 && ady == 2) return Terrain.STATUE;
			return Terrain.EMPTY_SP;
		}

		if (type == Site.RUIN){
			int adx = Math.abs( dx ), ady = Math.abs( dy );
			if (adx > 3 || ady > 3) return -1;
			//a broken 7x7 stone shell around a pedestal heart
			if (dx == 0 && dy == 0) return Terrain.PEDESTAL;
			if (adx <= 1 && ady <= 1) return Terrain.EMPTY_SP;
			if (adx == 3 || ady == 3){
				//crumbled wall: gaps where the hash says so
				long h = hash( seed ^ 0xDEAD1L, wx, wy );
				if ((h & 3) == 0) return Terrain.EMPTY_DECO;
				return Terrain.WALL_DECO;
			}
			return Terrain.EMPTY_DECO;
		}

		//SETTLEMENT: houses scattered on an organic spiral around a well,
		//joined to the centre (and so to each other) by dirt footpaths.
		//size varies from a couple of huts to a sprawling city
		int[] layout = settlementLayout( seed, sx, sy );
		int radius = layout[0];

		int adx = Math.abs( dx ), ady = Math.abs( dy );
		if (adx > radius + 3 || ady > radius + 3) return -1;

		//the village signpost: on the road out, just past the houses,
		//pointing at this village's road neighbour
		long nv = nearestVillage( seed, sx, sy );
		if (nv != Long.MIN_VALUE){
			int nx = (int)(nv >> 32), ny = (int)nv;
			float ddx = siteX( seed, nx, ny ) - cx, ddy = siteY( seed, nx, ny ) - cy;
			float len = (float)Math.sqrt( ddx*ddx + ddy*ddy );
			if (len > 0){
				int sgx = Math.round( ddx / len * (radius + 2) );
				int sgy = Math.round( ddy / len * (radius + 2) ) + 1;
				if (dx == sgx && dy == sgy) return Terrain.SIGN;
			}
		}

		//the paling fence: a square ring two cells out from the settlement's
		//reach, broken where a road runs through it and wherever the palings
		//have fallen in. it is real terrain, so it actually pens the village in
		if (Math.max( adx, ady ) == radius + 2
				&& !onRoad( seed, wx, wy )
				&& Math.floorMod( hash( seed ^ 0xFE7CEL, wx, wy ), 10 ) != 0){
			return Terrain.BARRICADE;
		}

		//houses: 5x5 shells at the layout's positions, door facing the well
		int nearPath = Integer.MAX_VALUE;   //squared dist to the closest footpath
		for (int i = 1; i + 1 < layout.length; i += 2){
			int hcx = layout[i], hcy = layout[i+1];
			int lx = dx - hcx, ly = dy - hcy;
			if (Math.abs( lx ) <= 2 && Math.abs( ly ) <= 2){
				//door: middle of the wall on the axis that faces the well
				if (lx == houseDoorDX( hcx, hcy ) - hcx && ly == houseDoorDY( hcx, hcy ) - hcy){
					return Terrain.DOOR;
				}
				if (Math.abs( lx ) == 2 || Math.abs( ly ) == 2) return Terrain.WALL;
				return Terrain.EMPTY_SP;   //wooden-ish interior
			}
			//footpath: door -> well segment
			boolean xAxis = Math.abs( hcx ) >= Math.abs( hcy );
			float doorWx = hcx + (xAxis ? (hcx > 0 ? -2.5f : 2.5f) : 0);
			float doorWy = hcy + (xAxis ? 0 : (hcy > 0 ? -2.5f : 2.5f));
			//wobble the sample point so paths meander a little
			float wob = 1.6f * (WorldModel.pathWobble( seed, wx, wy, 0 ) - 0.5f) * 2f;
			int d2 = (int)segDistSq( dx + wob, dy - wob, doorWx, doorWy, 0, 0 );
			if (d2 < nearPath) nearPath = d2;
		}

		//footpaths themselves
		if (nearPath <= 1) return Terrain.DIRT_PATH;

		//patchy trampled ground: scattered dirt near the paths and houses,
		//fading out with distance - never a solid slab
		if (nearPath <= 16){
			long h = hash( seed ^ 0x9A7C1L, wx, wy );
			int chance = 2 + nearPath / 4;   //1-in-2 by the path .. 1-in-6 further out
			if ((h & 0xFF) % chance == 0) return Terrain.DIRT_PATH;
		}
		return -1;
	}

	// ------------------------------------------------------------- fields

	//a human village's ploughed plots lie beyond its paling fence (radius + 2) and the lane
	//outside it: from ring radius + FIELD_RING0 out, each at most FIELD_DEPTH rings deep
	public static final int FIELD_RING0 = 4, FIELD_DEPTH = 4;
	//the outermost ring past its radius any of a village's fields reaches
	public static final int FIELD_REACH = FIELD_RING0 + FIELD_DEPTH - 1;
	//the land a field is ploughed on (the annual mean's: a field never comes and goes with the seasons)
	private static final java.util.EnumSet<WorldModel.Biome> FARMLAND = java.util.EnumSet.of(
			WorldModel.Biome.PLAINS, WorldModel.Biome.MEADOW, WorldModel.Biome.FOREST, WorldModel.Biome.FOOTHILLS );
	//how far the grounds of a place other than a village reach from its site (a dragon's lair, the widest)
	private static final int SITE_REACH = 6;
	//tries at a plot, and the salt they are rolled with
	private static final int FIELD_TRIES = 24;
	private static final long FIELD_SALT = 0xF1E1D5L;
	static final int[] NO_FIELDS = new int[0];
	private static final java.util.Map<Long, int[]> fieldCache = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * A human village's fields: two to four ploughed plots outside its fence, 4x3 to 6x4 cells
	 * with the long side along the fence, as {x0, y0, x1, y1, ...} (world cells, inclusive), in
	 * the order they were laid out - none for a gnoll or outlaw village. Pure (seed, sector),
	 * read at the annual mean, so they never come and go with the seasons: each lies on
	 * farmland on open, level ground (no water, ice or rock, nothing of the mountains' bands),
	 * a cell clear of every road and of the signpost, clear of the town and of every other
	 * place's grounds and fields, and a cell clear of the village's other plots. Where the
	 * land allows fewer, the village has fewer.
	 */
	public static int[] fieldPlots( long seed, int sx, int sy ){
		checkSeed( seed );
		Long key = sectorKey( sx, sy );
		int[] cached = fieldCache.get( key );
		if (cached != null) return cached;
		int[] plots = computeFieldPlots( seed, sx, sy );
		fieldCache.put( key, plots );
		return plots;
	}

	/** Is the world cell in one of these plots (fieldPlots)? */
	public static boolean inField( int[] plots, int wx, int wy ){
		for (int i = 0; i + 3 < plots.length; i += 4){
			if (wx >= plots[i] && wy >= plots[i+1] && wx <= plots[i+2] && wy <= plots[i+3]) return true;
		}
		return false;
	}

	private static int[] computeFieldPlots( long seed, int sx, int sy ){
		if (siteType( seed, sx, sy ) != Site.VILLAGE || faction( seed, sx, sy ) != Faction.HUMAN) return NO_FIELDS;
		int cx = siteX( seed, sx, sy ), cy = siteY( seed, sx, sy );
		int ring = settlementLayout( seed, sx, sy )[0] + FIELD_RING0;
		long h = hash( seed ^ FIELD_SALT, sx, sy );
		int want = 2 + (int) Math.floorMod( h, 3L );
		java.util.ArrayList<int[]> found = new java.util.ArrayList<>();
		WorldModel.Sample smp = new WorldModel.Sample();
		for (int tries = 0; tries < FIELD_TRIES && found.size() < want; tries++){
			long th = hash( h, tries, FIELD_SALT );
			int side = (int)(th & 3L);
			int len = 4 + (int) Math.floorMod( th >>> 2, 3L );    //along the fence
			int depth = 3 + (int)((th >>> 5) & 1L);              //out from it
			int along = -ring + (int) Math.floorMod( th >>> 8, (long)(2 * ring + 2 - len) );
			int x0, y0, x1, y1;
			switch (side){
				case 0:   //north
					x0 = cx + along; x1 = x0 + len - 1; y1 = cy - ring; y0 = y1 - depth + 1;
					break;
				case 1:   //east
					y0 = cy + along; y1 = y0 + len - 1; x0 = cx + ring; x1 = x0 + depth - 1;
					break;
				case 2:   //south
					x0 = cx + along; x1 = x0 + len - 1; y0 = cy + ring; y1 = y0 + depth - 1;
					break;
				default:  //west
					y0 = cy + along; y1 = y0 + len - 1; x1 = cx - ring; x0 = x1 - depth + 1;
			}
			if (fieldClear( seed, sx, sy, x0, y0, x1, y1, found, smp )) found.add( new int[]{ x0, y0, x1, y1 } );
		}
		int[] out = new int[found.size() * 4];
		for (int i = 0; i < found.size(); i++) System.arraycopy( found.get( i ), 0, out, i * 4, 4 );
		return out;
	}

	//may a plot of the village in (sx, sy) lie on [x0..x1]x[y0..y1]? the cheap questions first
	private static boolean fieldClear( long seed, int sx, int sy, int x0, int y0, int x1, int y1,
			java.util.ArrayList<int[]> others, WorldModel.Sample smp ){
		//a cell clear of the village's other plots
		for (int[] o : others){
			if (x0 - 1 <= o[2] && o[0] <= x1 + 1 && y0 - 1 <= o[3] && o[1] <= y1 + 1) return false;
		}
		//clear of the town and the pines it spills into (terrainAt: fourteen cells round its walls)
		if (rectNear( x0, y0, x1, y1, 0, 0, TOWN_SIZE / 2 + 16 )) return false;
		//clear of every other place's grounds, and of every other village's fields
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				if (dx == 0 && dy == 0) continue;
				Site t = siteType( seed, sx+dx, sy+dy );
				if (t == Site.NONE) continue;
				int reach = t == Site.VILLAGE ? settlementLayout( seed, sx+dx, sy+dy )[0] + FIELD_REACH + 1 : SITE_REACH;
				if (rectNear( x0, y0, x1, y1, siteX( seed, sx+dx, sy+dy ), siteY( seed, sx+dx, sy+dy ), reach )) return false;
			}
		}
		//a cell clear of the roads and of the village's signpost (siteTerrain)
		int cx = siteX( seed, sx, sy ), cy = siteY( seed, sx, sy );
		int radius = settlementLayout( seed, sx, sy )[0];
		long nv = nearestVillage( seed, sx, sy );
		int signX = Integer.MIN_VALUE, signY = Integer.MIN_VALUE;
		if (nv != Long.MIN_VALUE){
			float ddx = siteX( seed, (int)(nv >> 32), (int) nv ) - cx, ddy = siteY( seed, (int)(nv >> 32), (int) nv ) - cy;
			float dl = (float) Math.sqrt( ddx*ddx + ddy*ddy );
			if (dl > 0){
				signX = cx + Math.round( ddx / dl * (radius + 2) );
				signY = cy + Math.round( ddy / dl * (radius + 2) ) + 1;
			}
		}
		for (int wy = y0 - 1; wy <= y1 + 1; wy++){
			for (int wx = x0 - 1; wx <= x1 + 1; wx++){
				if (wx == signX && wy == signY) return false;
				if (onRoad( seed, wx, wy )) return false;
			}
		}
		//farmland, open and level, at the annual mean
		for (int wy = y0; wy <= y1; wy++){
			for (int wx = x0; wx <= x1; wx++){
				WorldModel.sample( seed, wx, wy, 0f, smp );
				if (!FARMLAND.contains( smp.biome ) || WorldLayers.band( smp.elev ) != 0) return false;
				int wild = WorldModel.wildTerrain( seed, wx, wy, smp );
				if (wild == Terrain.WATER || wild == Terrain.DEEP_WATER || wild == Terrain.FROZEN_WATER
						|| wild == Terrain.WALL) return false;
			}
		}
		return true;
	}

	//does the rect come within `reach` cells (Chebyshev) of (x, y)?
	private static boolean rectNear( int x0, int y0, int x1, int y1, int x, int y, int reach ){
		int dx = x < x0 ? x0 - x : x > x1 ? x - x1 : 0;
		int dy = y < y0 ? y0 - y : y > y1 ? y - y1 : 0;
		return Math.max( dx, dy ) <= reach;
	}

	/** The doorway of the house at layout offset (hcx, hcy), relative to the
	 *  settlement centre: the middle of the wall on the axis facing the well. */
	public static int houseDoorDX( int hcx, int hcy ){
		boolean xAxis = Math.abs( hcx ) >= Math.abs( hcy );
		return hcx + (xAxis ? (hcx > 0 ? -2 : 2) : 0);
	}

	public static int houseDoorDY( int hcx, int hcy ){
		boolean xAxis = Math.abs( hcx ) >= Math.abs( hcy );
		return hcy + (xAxis ? 0 : (hcy > 0 ? -2 : 2));
	}

	/** The sector of this village's road neighbour (packed), or Long.MIN_VALUE. */
	public static long roadNeighbour( long seed, int sx, int sy ){
		return nearestVillage( seed, sx, sy );
	}

	/** Packed sector coordinates, the key sites and caravans are indexed by. */
	public static long sectorOf( int sx, int sy ){
		return sectorKey( sx, sy );
	}

	/** The packed sector of the settlement whose house door stands on this world
	 *  cell, or Long.MIN_VALUE when no house door is there. */
	public static long houseSector( long seed, int wx, int wy ){
		int sx0 = Math.floorDiv( wx, SECTOR ), sy0 = Math.floorDiv( wy, SECTOR );
		//a big settlement's houses reach into the neighbouring sectors
		for (int sy = sy0-1; sy <= sy0+1; sy++){
			for (int sx = sx0-1; sx <= sx0+1; sx++){
				if (siteType( seed, sx, sy ) != Site.VILLAGE) continue;
				int cx = siteX( seed, sx, sy ), cy = siteY( seed, sx, sy );
				int[] layout = settlementLayout( seed, sx, sy );
				for (int i = 1; i + 1 < layout.length; i += 2){
					if (cx + houseDoorDX( layout[i], layout[i+1] ) == wx
							&& cy + houseDoorDY( layout[i], layout[i+1] ) == wy){
						return sectorKey( sx, sy );
					}
				}
			}
		}
		return Long.MIN_VALUE;
	}

	/** The interior depth of the village house whose door stands on this world
	 *  cell: a stable id in 1..16384, so every house keeps its own saved room. */
	public static int houseInteriorDepth( long seed, int wx, int wy ){
		return 1 + (int)(hash( seed ^ 0x40025EL, wx, wy ) & 0x3FFFL);
	}

	//cached per-sector settlement layout: [radius, hx0, hy0, hx1, hy1, ...]
	private static final java.util.Map<Long, int[]> layoutCache = new java.util.concurrent.ConcurrentHashMap<>();

	static int[] settlementLayout( long seed, int sx, int sy ){
		Long key = sectorKey( sx, sy );
		int[] cached = layoutCache.get( key );
		if (cached != null) return cached;

		long h = hash( seed ^ 0x5E77L, sx, sy );
		//size classes: hamlet 2-3, village 4-7, town 9-15, city 18-36,
		//and the rare 1-in-64 METROPOLIS - a genuinely enormous sprawl
		int roll = (int)(h & 63);
		int houses;
		if (roll < 20)      houses = 2 + (int)((h >> 8) % 2);     //hamlet
		else if (roll < 42) houses = 4 + (int)((h >> 8) % 4);     //village
		else if (roll < 56) houses = 9 + (int)((h >> 8) % 7);     //town
		else if (roll < 63) houses = 18 + (int)((h >> 8) % 19);   //city
		else                houses = 45 + (int)((h >> 8) % 46);   //metropolis!

		int radius = Math.max( 10, (int)(4.8 * Math.sqrt( houses ) + 3) );

		int[] layout = new int[1 + houses * 2];
		//Fermat spiral with per-house jitter, then a deterministic outward
		//push until every house keeps a 6-cell (Chebyshev) berth from the
		//others - houses are 5x5, so 6 guarantees they never intersect
		double golden = 2.39996;
		int maxR = radius;
		for (int i = 0; i < houses; i++){
			double ang = i * golden + ((hash( seed ^ 0x6A11L, sx * 64 + i, sy ) & 0xFF) / 255.0 - 0.5) * 0.7;
			double dist = 6.5 + (radius - 8) * Math.sqrt( (i + 0.5) / houses );
			int hx = 0, hy = 0;
			for (int push = 0; push < 24; push++){
				hx = (int)Math.round( Math.cos( ang ) * dist );
				hy = (int)Math.round( Math.sin( ang ) * dist );
				boolean clear = Math.max( Math.abs( hx ), Math.abs( hy ) ) >= 5; //clear of the well
				for (int j = 0; clear && j < i; j++){
					if (Math.max( Math.abs( hx - layout[1 + j*2] ),
							Math.abs( hy - layout[1 + j*2 + 1] ) ) < 6){
						clear = false;
					}
				}
				if (clear) break;
				dist += 2.0;
			}
			layout[1 + i*2]     = hx;
			layout[1 + i*2 + 1] = hy;
			maxR = Math.max( maxR, Math.max( Math.abs( hx ), Math.abs( hy ) ) + 3 );
		}
		layout[0] = maxR;
		layoutCache.put( key, layout );
		return layout;
	}

	/** How many of a settlement's houses have folk in them: the most central ones (the layout
	 *  runs centre-out), at most ten, so a city's crowd stays capped. The families
	 *  (OverworldLevel.populateSettlement) and the lit hearths (SettlementLights) both read it. */
	public static int populatedHouses( int houses ){
		return Math.min( houses, 10 );
	}

	/**
	 * The settlements around a world cell: every VILLAGE site within `sectors` sectors
	 * (Chebyshev) of the cell's own, of the faction asked (null for any) and at least
	 * minHouses houses, nearest first (squared distance from the cell to the well, ties by
	 * sector key). Each is {sx, sy, cx, cy, houses}. Debug scenes and tests find their
	 * villages here.
	 */
	public static java.util.ArrayList<int[]> settlementsNear( long seed, int wx, int wy, Faction faction,
			int minHouses, int sectors ){
		int sx0 = Math.floorDiv( wx, SECTOR ), sy0 = Math.floorDiv( wy, SECTOR );
		java.util.ArrayList<int[]> out = new java.util.ArrayList<>();
		for (int sy = sy0 - sectors; sy <= sy0 + sectors; sy++){
			for (int sx = sx0 - sectors; sx <= sx0 + sectors; sx++){
				if (siteType( seed, sx, sy ) != Site.VILLAGE) continue;
				if (faction != null && faction( seed, sx, sy ) != faction) continue;
				int houses = (settlementLayout( seed, sx, sy ).length - 1) / 2;
				if (houses < minHouses) continue;
				out.add( new int[]{ sx, sy, siteX( seed, sx, sy ), siteY( seed, sx, sy ), houses } );
			}
		}
		out.sort( (a, b) -> {
			long da = (long)(a[2] - wx) * (a[2] - wx) + (long)(a[3] - wy) * (a[3] - wy);
			long db = (long)(b[2] - wx) * (b[2] - wx) + (long)(b[3] - wy) * (b[3] - wy);
			if (da != db) return Long.compare( da, db );
			return Long.compare( sectorOf( a[0], a[1] ), sectorOf( b[0], b[1] ) );
		} );
		return out;
	}
}

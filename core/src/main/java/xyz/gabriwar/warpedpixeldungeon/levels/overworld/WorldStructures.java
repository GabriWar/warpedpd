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
	public static final int TOWN_PLAZA      = 766;  //where you arrive by magic (journal, beacon, new game)
	public static final int TOWN_STAIRS     = 899;  //down into the dungeon (floor 1)
	public static final int TOWN_MINE_GATE  = 141;  //the barred north gate into the kupua mines
	public static final int TOWN_TEMPLE_DOOR = 412;
	public static final int TOWN_PEDESTAL   = 222;  //enchanting station
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
				if (c == TOWN_STAIRS || c == TOWN_MINE_GATE || c == TOWN_TEMPLE_DOOR
						|| c == TOWN_PLAZA || c == TOWN_PEDESTAL || c == TOWN_ALTAR) return true;
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
		if (cell == TOWN_STAIRS) return Terrain.EXIT;
		if (cell == TOWN_MINE_GATE || cell == TOWN_TEMPLE_DOOR) return Terrain.DOOR;
		if (cell == TOWN_PEDESTAL || cell == TOWN_ALTAR) return Terrain.PEDESTAL;
		for (int d : TOWN_DOORS) if (cell == d) return Terrain.DOOR;
		int t = xyz.gabriwar.warpedpixeldungeon.levels.TownLayouts.TOWN_LAYOUT_REMIXED[cell];
		//snow ground in the art is the world's own snow, and the authored tree
		//rows are RANDOMIZED: some thin out, fresh pines seed the open snow -
		//the tree line stops reading as ruler-straight rows
		long h = hash( seed ^ 0x73EE5EEDL, wx, wy );
		if (t == Terrain.WALL){
			if (xyz.gabriwar.warpedpixeldungeon.tiles.TownRemixedTiles.outdoor( cell )){
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
			//nothing sprouts on the roads, and nothing ever blocks a doorway
			return (h & 15) == 0 && t == Terrain.EMPTY && !townApproach( wx, wy )
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
	public enum Site { NONE, VILLAGE, RUIN, DRAGON, CAMP, STONES, BIGTREE }

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
		return Site.NONE;
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
		int sx = Math.floorDiv( wx, SECTOR ), sy = Math.floorDiv( wy, SECTOR );

		float[] segs = roadSegments( seed, sx, sy );
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
	 * The structure terrain at a world cell, or -1 for "no structure here".
	 * Overrides the wilderness except where the land itself forbids it. The
	 * caller passes the already-computed wilderness terrain so roads never
	 * trigger a second full generator evaluation.
	 */
	public static int terrainAt( long seed, int wx, int wy, int wild ){

		int town = townTerrain( seed, wx, wy );
		if (town != -1) return town;

		int sx = Math.floorDiv( wx, SECTOR ), sy = Math.floorDiv( wy, SECTOR );

		//the site footprint of this or any adjacent sector can reach this cell
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				Site type = siteType( seed, sx+dx, sy+dy );
				if (type == Site.NONE) continue;
				int t = siteTerrain( seed, sx+dx, sy+dy, type, wx, wy );
				if (t != -1) return t;
			}
		}

		//roads stop at mountains and walk straight over ice; open water they
		//cross on plank bridges
		if (wild == Terrain.FROZEN_WATER || wild == Terrain.WALL){
			return -1;
		}
		if (onRoad( seed, wx, wy )){
			return wild == Terrain.WATER ? Terrain.BRIDGE : Terrain.DIRT_PATH;
		}

		//the town's tree line bleeds outward: a pine cloud on the surrounding
		//snow, dense by the walls and thinning to nothing over ~14 cells
		if (wild == Terrain.SNOW){
			int lx = Math.max( 0, Math.max( TOWN_X0 - wx, wx - (TOWN_X0 + TOWN_SIZE - 1) ) );
			int ly = Math.max( 0, Math.max( TOWN_Y0 - wy, wy - (TOWN_Y0 + TOWN_SIZE - 1) ) );
			int d = Math.max( lx, ly );
			if (d > 0 && d <= 14 && !townApproach( wx, wy ) && !onRoad( seed, wx, wy )){
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

		if (dx == 0 && dy == 0) return Terrain.WELL;

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
}

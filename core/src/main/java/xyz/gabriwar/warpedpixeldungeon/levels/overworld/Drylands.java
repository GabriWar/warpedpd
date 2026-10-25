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

/**
 * The desert's own places, laid on the sector grid of WorldStructures in the sectors its cascade
 * left empty: the OASIS (a pool of fresh water ringed with grass and date palms, a trader's rug on
 * its bank) and the BURIED_TEMPLE (the top of a sandstone temple sticking out of the dunes, its
 * stairwell leading down into a short crypt: levels/Delves, SandTempleLevel). Everything here is
 * a pure function of (seed, sector, cell), like the rest of the world's structures.
 *
 * Where one may stand (site): the sector's site candidate on desert at the annual mean, its own
 * hash saying so (an oasis in one empty desert sector of three, a temple in one of two of the
 * rest), and its whole reach and a ring round it desert ground of the surface's band, clear of
 * every road, of every place the cascade laid (a village with its fields) and of every cell a cave
 * mouth would open on. So nothing that stood before moves or loses a cell: the cascade, the roads
 * and the fields never read these places (WorldStructures.baseType), and the mouths are kept.
 *
 * Their props (the palms, the rug, the temple's slabs, stair, obelisks and statues) are CUSTOM_DECO
 * terrain, solid or walkable, drawn by tiles/DrylandsArt from its own sheet.
 */
public final class Drylands {

	private Drylands(){}

	//the sites' own hashes: nothing else on the world rolls with them
	static final long OASIS_SALT = 0x0A515EEDL, TEMPLE_SALT = 0x7E3B1E5EL;
	private static final long SHAPE_SALT = 0x5A4D0A5L, CELL_SALT = 0xD7E5A1CEL, PALM_SALT = 0x9A1F7EEL,
			SIDE_SALT = 0x51DE5A1L, BURY_SALT = 0xB0111EDL, LOOK_SALT = 0x10C4A11L;

	/** How far a place's cells reach from its site, Chebyshev: the oasis's ring, the temple's top
	 *  (and the obelisks' tips over the row above it). */
	public static final int OASIS_REACH = 7, TEMPLE_REACH = 4;

	/** The reach of a desert place's kind, 0 for any other site. */
	public static int reach( WorldStructures.Site type ){
		return type == WorldStructures.Site.OASIS ? OASIS_REACH
				: type == WorldStructures.Site.BURIED_TEMPLE ? TEMPLE_REACH : 0;
	}

	static long hash( long seed, long x, long y ){
		long h = seed ^ 0x6D1A2B3CL;
		h ^= x * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft( h, 29 );
		h ^= y * 0xC2B2AE3D27D4EB4FL;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return h;
	}

	// ------------------------------------------------------------------ placement

	/** The desert place of a sector the cascade left empty (WorldStructures.siteType), or NONE. */
	static WorldStructures.Site site( long seed, int sx, int sy ){
		WorldStructures.Site kind = kindRolled( seed, sx, sy );
		if (kind == WorldStructures.Site.NONE) return kind;
		int cx = WorldStructures.siteX( seed, sx, sy ), cy = WorldStructures.siteY( seed, sx, sy );
		//nothing settles on the town's doorstep (as the cascade has it)
		if (Math.abs( cx ) < WorldModel.TOWN_IN + 8 && Math.abs( cy ) < WorldModel.TOWN_IN + 8) return WorldStructures.Site.NONE;
		WorldModel.Sample smp = new WorldModel.Sample();
		if (WorldModel.sample( seed, cx, cy, 0f, smp ).biome != WorldModel.Biome.DESERT) return WorldStructures.Site.NONE;
		int reach = reach( kind );
		if (!clearOfPlaces( seed, sx, sy, cx, cy, reach )) return WorldStructures.Site.NONE;
		if (!landClear( seed, cx, cy, reach + 1, smp )) return WorldStructures.Site.NONE;
		if (!clearOfRoads( seed, cx, cy, reach + 1 )) return WorldStructures.Site.NONE;
		return kind;
	}

	//what the sector's own hashes ask for, before the land is looked at
	static WorldStructures.Site kindRolled( long seed, int sx, int sy ){
		if (Math.floorMod( hash( seed ^ OASIS_SALT, sx, sy ), 3 ) == 0) return WorldStructures.Site.OASIS;
		if (Math.floorMod( hash( seed ^ TEMPLE_SALT, sx, sy ), 2 ) == 0) return WorldStructures.Site.BURIED_TEMPLE;
		return WorldStructures.Site.NONE;
	}

	//every cell within r: desert of the surface's band at the annual mean, and no cell a cave mouth
	//would open on (the mouths keep their cells: WindowGenerator.mouthCandidate)
	private static boolean landClear( long seed, int cx, int cy, int r, WorldModel.Sample smp ){
		for (int y = cy - r; y <= cy + r; y++){
			for (int x = cx - r; x <= cx + r; x++){
				WorldModel.sample( seed, x, y, 0f, smp );
				if (smp.biome != WorldModel.Biome.DESERT || WorldLayers.band( smp.elev ) != 0) return false;
				if (WindowGenerator.mouthCandidate( seed, x, y )) return false;
			}
		}
		return true;
	}

	//no road within r (a road keeps every cell it had)
	private static boolean clearOfRoads( long seed, int cx, int cy, int r ){
		for (int y = cy - r; y <= cy + r; y++){
			for (int x = cx - r; x <= cx + r; x++){
				if (WorldStructures.onRoad( seed, x, y )) return false;
			}
		}
		return true;
	}

	//clear of the grounds of every place the cascade laid round it (a village's with its fields);
	//two sites of different sectors stand 49 cells apart at least, so two desert places never meet
	private static boolean clearOfPlaces( long seed, int sx, int sy, int cx, int cy, int reach ){
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				if (dx == 0 && dy == 0) continue;
				WorldStructures.Site t = WorldStructures.baseType( seed, sx + dx, sy + dy );
				if (t == WorldStructures.Site.NONE) continue;
				int other = t == WorldStructures.Site.VILLAGE
						? WorldStructures.settlementLayout( seed, sx + dx, sy + dy )[0] + WorldStructures.FIELD_REACH + 1
						: WorldStructures.SITE_REACH;
				int d = Math.max( Math.abs( WorldStructures.siteX( seed, sx + dx, sy + dy ) - cx ),
						Math.abs( WorldStructures.siteY( seed, sx + dx, sy + dy ) - cy ) );
				if (d <= other + reach + 2) return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ the cells

	/** The terrain a desert place lays on a world cell, -1 where it lays none. */
	static int terrain( long seed, int sx, int sy, WorldStructures.Site type, int wx, int wy ){
		int cx = WorldStructures.siteX( seed, sx, sy ), cy = WorldStructures.siteY( seed, sx, sy );
		if (type == WorldStructures.Site.OASIS) return oasisTerrain( seed, sx, sy, cx, cy, wx, wy );
		if (type == WorldStructures.Site.BURIED_TEMPLE) return templeTerrain( seed, cx, cy, wx, wy );
		return -1;
	}

	/** Is this world cell the stairwell of a buried temple? Its way is the temple's own
	 *  (OverworldLevel), never a way between the world's slices. */
	public static boolean templeGate( long seed, int wx, int wy ){
		int sx = Math.floorDiv( wx, WorldStructures.SECTOR ), sy = Math.floorDiv( wy, WorldStructures.SECTOR );
		return WorldStructures.siteType( seed, sx, sy ) == WorldStructures.Site.BURIED_TEMPLE
				&& WorldStructures.siteX( seed, sx, sy ) == wx && WorldStructures.siteY( seed, sx, sy ) == wy;
	}

	// ---- the oasis

	/** The pool's half-widths, east-west and north-south, of the oasis in this sector. */
	static float[] poolRadii( long seed, int sx, int sy ){
		long h = hash( seed ^ SHAPE_SALT, sx, sy );
		float a = 2.8f + ((h >>> 8) & 7) * 0.15f, b = 2.0f + ((h >>> 12) & 7) * 0.12f;
		return ((h >>> 16) & 1) == 0 ? new float[]{ a, b } : new float[]{ b, a };
	}

	//how far a cell lies out from the pool's middle, the pool's rim at 1: its ellipse, roughened
	//by a hash of the cell so the shore never runs as a ruler-drawn oval
	private static float oasisNorm( long seed, float rx, float ry, int dx, int dy, int wx, int wy ){
		float ex = dx / rx, ey = dy / ry;
		float wob = ((hash( seed ^ CELL_SALT, wx, wy ) >>> 24) & 0xFF) / 255f * 0.24f - 0.12f;
		return (float) Math.sqrt( ex * ex + ey * ey ) + wob;
	}

	//the outer edge of the grass ring, in the pool's units: about two cells and a half out
	static float ringOuter( float rx, float ry ){
		return 1f + 2.4f / ((rx + ry) * 0.5f);
	}

	/** The world cell the oasis trader's rug is laid on: on the ring, east or west of the pool. */
	public static int[] rugCell( long seed, int sx, int sy ){
		float[] r = poolRadii( seed, sx, sy );
		int side = (hash( seed ^ SIDE_SALT, sx, sy ) & 1L) == 0 ? 1 : -1;
		return new int[]{ WorldStructures.siteX( seed, sx, sy ) + side * ((int) Math.ceil( r[0] ) + 1),
				WorldStructures.siteY( seed, sx, sy ) };
	}

	private static int oasisTerrain( long seed, int sx, int sy, int cx, int cy, int wx, int wy ){
		int dx = wx - cx, dy = wy - cy;
		if (Math.abs( dx ) > OASIS_REACH || Math.abs( dy ) > OASIS_REACH) return -1;
		float[] r = poolRadii( seed, sx, sy );
		float n = oasisNorm( seed, r[0], r[1], dx, dy, wx, wy );
		if (n < 1f) return Terrain.WATER;
		float outer = ringOuter( r[0], r[1] );
		int[] rug = rugCell( seed, sx, sy );
		if (wx == rug[0] && wy == rug[1]) return Terrain.CUSTOM_DECO_EMPTY;
		if (n >= outer) return -1;
		if (palmAt( seed, r, outer, cx, cy, rug, wx, wy )) return Terrain.CUSTOM_DECO;
		//the rug's surroundings and the way in from the south stay plain grass: the trader's goods
		//are laid out round him, and a hero walks up to the water
		if (Math.max( Math.abs( wx - rug[0] ), Math.abs( wy - rug[1] ) ) <= 1 || approach( dx, dy )) return Terrain.GRASS;
		long ch = hash( seed ^ CELL_SALT, wx, wy );
		if (Math.floorMod( ch, 5 ) == 0) return Terrain.HIGH_GRASS;
		if (Math.floorMod( ch >>> 8, 13 ) == 0) return Terrain.FLOWER_PATCH;
		return Terrain.GRASS;
	}

	//the way down to the water from the south: no palm and no tall grass in it
	private static boolean approach( int dx, int dy ){
		return dy > 0 && Math.abs( dx ) <= 1;
	}

	//a cell of the ring the hash plants with a palm, before its neighbours have their say
	private static boolean palmCandidate( long seed, float[] r, float outer, int cx, int cy, int[] rug, int wx, int wy ){
		int dx = wx - cx, dy = wy - cy;
		if (Math.abs( dx ) > OASIS_REACH || Math.abs( dy ) > OASIS_REACH || approach( dx, dy )) return false;
		if (Math.max( Math.abs( wx - rug[0] ), Math.abs( wy - rug[1] ) ) <= 1) return false;
		float n = oasisNorm( seed, r[0], r[1], dx, dy, wx, wy );
		if (n < 1.18f || n > outer - 0.08f) return false;
		return Math.floorMod( hash( seed ^ PALM_SALT, wx, wy ), 3 ) == 0;
	}

	//a palm, where a candidate has none before it (in reading order) among its eight neighbours:
	//no two palms ever stand side by side
	private static boolean palmAt( long seed, float[] r, float outer, int cx, int cy, int[] rug, int wx, int wy ){
		if (!palmCandidate( seed, r, outer, cx, cy, rug, wx, wy )) return false;
		for (int[] d : EARLIER){
			if (palmCandidate( seed, r, outer, cx, cy, rug, wx + d[0], wy + d[1] )) return false;
		}
		return true;
	}

	//the neighbours that come before a cell in reading order
	private static final int[][] EARLIER = { {-1, -1}, {0, -1}, {1, -1}, {-1, 0} };

	/** The world cell a hero walks up to a desert place from: just south of an oasis' ring, on the
	 *  way down to its water; just south of a temple's top, below its stairwell. */
	public static int[] approach( long seed, Place p ){
		if (p.type == WorldStructures.Site.OASIS){
			float[] r = poolRadii( seed, p.sx, p.sy );
			return new int[]{ p.cx, p.cy + (int) Math.ceil( r[1] * ringOuter( r[0], r[1] ) ) + 1 };
		}
		return new int[]{ p.cx, p.cy + TOP_H + 2 };
	}

	/** Is this world cell in the shade of an oasis: its water, its ring and a cell round them, where
	 *  the air is cooler (TileTemperature, OverworldLevel.oasisAir)? */
	static boolean shadeOf( long seed, int sx, int sy, int wx, int wy ){
		int cx = WorldStructures.siteX( seed, sx, sy ), cy = WorldStructures.siteY( seed, sx, sy );
		int dx = wx - cx, dy = wy - cy;
		if (Math.abs( dx ) > OASIS_REACH + 1 || Math.abs( dy ) > OASIS_REACH + 1) return false;
		float[] r = poolRadii( seed, sx, sy );
		return oasisNorm( seed, r[0], r[1], dx, dy, wx, wy ) < ringOuter( r[0], r[1] ) + 0.35f;
	}

	// ---- the buried temple

	//the top of the temple: a platform of slabs seven cells wide and five deep round its stairwell,
	//obelisks at its back corners, two statues at its front, its edges here and there under the sand
	static final int TOP_W = 3, TOP_H = 2;

	private static int templeTerrain( long seed, int cx, int cy, int wx, int wy ){
		int dx = wx - cx, dy = wy - cy;
		if (Math.abs( dx ) > TOP_W || Math.abs( dy ) > TOP_H) return -1;
		if (dx == 0 && dy == 0) return Terrain.EXIT;
		if (obelisk( dx, dy ) || statue( dx, dy )) return Terrain.CUSTOM_DECO;
		if (buried( seed, dx, dy, wx, wy )) return -1;
		return Terrain.CUSTOM_DECO_EMPTY;
	}

	static boolean obelisk( int dx, int dy ){
		return dy == -TOP_H && Math.abs( dx ) == TOP_W;
	}

	static boolean statue( int dx, int dy ){
		return dy == TOP_H && Math.abs( dx ) == TOP_W - 1;
	}

	//the slabs the dunes have covered: the front corners always, the side edges and the back row's
	//outer slabs by the cell's hash; never the stairwell's ring nor the way up to it
	private static boolean buried( long seed, int dx, int dy, int wx, int wy ){
		if (dy == TOP_H && Math.abs( dx ) == TOP_W) return true;
		long h = hash( seed ^ BURY_SALT, wx, wy );
		if (Math.abs( dx ) == TOP_W && dy > -TOP_H) return Math.floorMod( h, 3 ) == 0;
		if (dy == -TOP_H && Math.abs( dx ) == TOP_W - 1) return Math.floorMod( h, 3 ) == 0;
		return false;
	}

	/** Is a world cell part of the temple's top round (cx, cy): its slabs, stairwell and stones? */
	static boolean onTop( long seed, int cx, int cy, int wx, int wy ){
		return templeTerrain( seed, cx, cy, wx, wy ) != -1;
	}

	// ------------------------------------------------------------------ the places of an area

	/** A desert place: its kind, sector and site. */
	public static final class Place {
		public final WorldStructures.Site type;
		public final int sx, sy, cx, cy;

		Place( WorldStructures.Site type, int sx, int sy, int cx, int cy ){
			this.type = type;
			this.sx = sx;
			this.sy = sy;
			this.cx = cx;
			this.cy = cy;
		}

		/** The key it is kept by (WorldStructures.sectorOf). */
		public long key(){
			return WorldStructures.sectorOf( sx, sy );
		}

		/** Does its reach cover this world cell? */
		public boolean reaches( int wx, int wy ){
			int r = reach( type );
			return Math.abs( wx - cx ) <= r && Math.abs( wy - cy ) <= r;
		}
	}

	/** Every desert place whose reach meets the world rect [wx0, wx1] x [wy0, wy1]. */
	public static ArrayList<Place> places( long seed, int wx0, int wy0, int wx1, int wy1 ){
		ArrayList<Place> out = new ArrayList<>();
		int sx0 = Math.floorDiv( wx0, WorldStructures.SECTOR ) - 1, sy0 = Math.floorDiv( wy0, WorldStructures.SECTOR ) - 1;
		int sx1 = Math.floorDiv( wx1, WorldStructures.SECTOR ) + 1, sy1 = Math.floorDiv( wy1, WorldStructures.SECTOR ) + 1;
		for (int sy = sy0; sy <= sy1; sy++){
			for (int sx = sx0; sx <= sx1; sx++){
				WorldStructures.Site t = WorldStructures.siteType( seed, sx, sy );
				if (t != WorldStructures.Site.OASIS && t != WorldStructures.Site.BURIED_TEMPLE) continue;
				int cx = WorldStructures.siteX( seed, sx, sy ), cy = WorldStructures.siteY( seed, sx, sy );
				int r = reach( t ) + 1;
				if (cx + r < wx0 || cx - r > wx1 || cy + r < wy0 || cy - r > wy1) continue;
				out.add( new Place( t, sx, sy, cx, cy ) );
			}
		}
		return out;
	}

	/** The desert place whose own cells hold this world cell (not merely its reach), or null. */
	public static Place placeAt( long seed, int wx, int wy ){
		int sx0 = Math.floorDiv( wx, WorldStructures.SECTOR ), sy0 = Math.floorDiv( wy, WorldStructures.SECTOR );
		for (int sy = sy0 - 1; sy <= sy0 + 1; sy++){
			for (int sx = sx0 - 1; sx <= sx0 + 1; sx++){
				WorldStructures.Site t = WorldStructures.siteType( seed, sx, sy );
				if (t != WorldStructures.Site.OASIS && t != WorldStructures.Site.BURIED_TEMPLE) continue;
				if (terrain( seed, sx, sy, t, wx, wy ) == -1) continue;
				return new Place( t, sx, sy, WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ) );
			}
		}
		return null;
	}

	/** The desert places of a kind within `sectors` sectors of a world cell, nearest first. */
	public static ArrayList<Place> near( long seed, int wx, int wy, WorldStructures.Site kind, int sectors ){
		int sx0 = Math.floorDiv( wx, WorldStructures.SECTOR ), sy0 = Math.floorDiv( wy, WorldStructures.SECTOR );
		ArrayList<Place> out = new ArrayList<>();
		for (int sy = sy0 - sectors; sy <= sy0 + sectors; sy++){
			for (int sx = sx0 - sectors; sx <= sx0 + sectors; sx++){
				//the cheap hashes first: most sectors never ask the land
				if (kindRolled( seed, sx, sy ) != kind) continue;
				if (WorldStructures.siteType( seed, sx, sy ) != kind) continue;
				out.add( new Place( kind, sx, sy, WorldStructures.siteX( seed, sx, sy ), WorldStructures.siteY( seed, sx, sy ) ) );
			}
		}
		out.sort( (a, b) -> Long.compare( dist2( a, wx, wy ), dist2( b, wx, wy ) ) );
		return out;
	}

	private static long dist2( Place p, int wx, int wy ){
		long dx = p.cx - wx, dy = p.cy - wy;
		return dx * dx + dy * dy;
	}

	// ------------------------------------------------------------------ the look

	/** The look of a desert place's cell (tiles/DrylandsArt), with the variants chosen by the cell's
	 *  hash: {ground tile, canopy tile over the cell above} (-1 for none of either), or null when
	 *  the place lays nothing there. */
	public static int[] look( long seed, Place p, int wx, int wy ){
		int t = terrain( seed, p.sx, p.sy, p.type, wx, wy );
		if (t == -1) return null;
		long h = hash( seed ^ LOOK_SALT, wx, wy );
		int dx = wx - p.cx, dy = wy - p.cy;
		if (p.type == WorldStructures.Site.OASIS){
			if (t == Terrain.CUSTOM_DECO){
				int v = (int) Math.floorMod( h, 3 );
				return new int[]{ ART_PALM_TRUNK + v, ART_PALM_CROWN + v };
			}
			if (t == Terrain.CUSTOM_DECO_EMPTY) return new int[]{ ART_RUG, -1 };
			return null;
		}
		if (t == Terrain.EXIT) return new int[]{ ART_STAIR, -1 };
		if (obelisk( dx, dy )) return new int[]{ ART_OBELISK, ART_OBELISK_TOP };
		if (statue( dx, dy )) return new int[]{ dx < 0 ? ART_STATUE : ART_STATUE_EAST, dx < 0 ? ART_STATUE_TOP : ART_STATUE_TOP_EAST };
		//a slab: its sides that meet the sand drifted over, the ring round the stairwell carved
		int mask = 0;
		if (!onTop( seed, p.cx, p.cy, wx, wy - 1 )) mask |= 1;
		if (!onTop( seed, p.cx, p.cy, wx + 1, wy )) mask |= 2;
		if (!onTop( seed, p.cx, p.cy, wx, wy + 1 )) mask |= 4;
		if (!onTop( seed, p.cx, p.cy, wx - 1, wy )) mask |= 8;
		if (mask != 0) return new int[]{ ART_SLAB_EDGE + mask, -1 };
		if (Math.abs( dx ) <= 1 && Math.abs( dy ) <= 1) return new int[]{ ART_SLAB_CARVED + (dx + 1) + (dy + 1) * 3, -1 };
		int v = (int) Math.floorMod( h, 7 );
		return new int[]{ v == 0 ? ART_SLAB_CRACKED : ART_SLAB + (v & 1), -1 };
	}

	/**
	 * The props of the desert places a window holds, for tiles/DrylandsArt: {ground, canopy}, a tile
	 * of drylands.png per window cell (-1 for none), the canopy's hung over the cell above the one
	 * that holds it; null when no desert place reaches the window. A prop shows only while its cell
	 * still holds what its place laid there. Pure: a network mirror lays the host's.
	 */
	public static int[][] art( long seed, int ox, int oy, int[] map, int w, int h ){
		ArrayList<Place> ps = places( seed, ox, oy, ox + w - 1, oy + h - 1 );
		if (ps.isEmpty()) return null;
		int[] ground = new int[w * h], canopy = new int[w * h];
		java.util.Arrays.fill( ground, -1 );
		java.util.Arrays.fill( canopy, -1 );
		for (Place p : ps){
			int r = reach( p.type );
			for (int wy = p.cy - r; wy <= p.cy + r; wy++){
				for (int wx = p.cx - r; wx <= p.cx + r; wx++){
					int x = wx - ox, y = wy - oy;
					if (x < 0 || y < 0 || x >= w || y >= h) continue;
					int cell = x + y * w;
					int[] look = look( seed, p, wx, wy );
					if (look == null || map[cell] != terrain( seed, p.sx, p.sy, p.type, wx, wy )) continue;
					ground[cell] = look[0];
					if (look[1] != -1 && y > 0) canopy[cell - w] = look[1];
				}
			}
		}
		return new int[][]{ ground, canopy };
	}

	/** The window cells in the shade of an oasis (shadeOf), 1 for each; null when there are none. */
	public static byte[] shade( long seed, int ox, int oy, int w, int h ){
		byte[] out = null;
		for (Place p : places( seed, ox, oy, ox + w - 1, oy + h - 1 )){
			if (p.type != WorldStructures.Site.OASIS) continue;
			int r = OASIS_REACH + 1;
			for (int wy = p.cy - r; wy <= p.cy + r; wy++){
				for (int wx = p.cx - r; wx <= p.cx + r; wx++){
					int x = wx - ox, y = wy - oy;
					if (x < 0 || y < 0 || x >= w || y >= h || !shadeOf( seed, p.sx, p.sy, wx, wy )) continue;
					if (out == null) out = new byte[w * h];
					out[x + y * w] = 1;
				}
			}
		}
		return out;
	}

	//the tiles of environment/custom_tiles/drylands.png (tools/assets/drylands_art.py draws them)
	public static final int ART_PALM_TRUNK = 0;       //3 versions
	public static final int ART_PALM_CROWN = 3;       //3 versions, over the cell above the trunk
	public static final int ART_RUG = 6;
	public static final int ART_STAIR = 7;
	public static final int ART_SLAB = 8;             //2 versions
	public static final int ART_SLAB_CRACKED = 10;
	public static final int ART_OBELISK = 11, ART_OBELISK_TOP = 12;
	public static final int ART_STATUE = 13, ART_STATUE_TOP = 14;
	public static final int ART_STATUE_EAST = 15;
	public static final int ART_SLAB_EDGE = 16;       //16 + side mask (1 N, 2 E, 4 S, 8 W) for 1..15
	public static final int ART_STATUE_TOP_EAST = 32;
	public static final int ART_SLAB_CARVED = 33;     //33..41: the stairwell's ring, row by row
}

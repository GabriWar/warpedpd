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
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;

import java.util.function.IntPredicate;

/**
 * Where the dangers of the world's slices lie, and how hard they bite: pure rules over the
 * world's fields and a window's map, never game state, so they are the same in every window,
 * on every reload, on the preparation worker and on a co-op guest's mirror. HazardWatch acts
 * on them.
 *
 * - Firedamp: gas pockets in the low, damp, narrow tunnels and the dips of the chamber floors
 *   of the deep caves (FIREDAMP_TOP and below), a 16x16 world block at a time, never near a
 *   way between slices.
 * - Loose rock: patches of the caves' rock where a pick brings the roof down more readily,
 *   more of them deeper; and a mined cell left with nothing to hold the roof up.
 * - Thin ice: lake ice near its shore, or any lake ice on a mild day; never in deep cold.
 * - Gusty ridges: crests of the high slices (HIGH and up) with a drop on both sides.
 */
public final class LayerHazards {

	private LayerHazards(){}

	//thin air and gusts from this slice up
	public static final int HIGH = 7;
	//the shallowest cave slice with firedamp
	public static final int FIREDAMP_TOP = -4;
	//a pocket is the gas of one world block this wide (the key a spent pocket is kept by)
	public static final int POCKET_BLOCK = 16;
	//the damp floor band gas gathers on, in a tunnel and in a chamber (the cave field's floor value;
	//below WorldModel.POOL_LEVEL it is a pool)
	static final float LOW_TUNNEL = 0.47f, LOW_CHAMBER = 0.40f;
	//enclosed: at most this many of the 16 cells two steps round a cell are open
	static final int ENCLOSED_MAX = 6;
	//a dip in a chamber's floor: at least this many of the 8 cells three steps round it lie higher
	//(or are rock)
	static final int HOLLOW_MIN = 7;
	//no gas this close (Chebyshev) to a pit, a ladder or any other way: a dizzy hero never staggers into one
	public static final int WAY_CLEARANCE = 5;
	//...nor this close to a cave place's reach (CaveSites): its lamps and fires would set it off
	public static final int SITE_CLEARANCE = 2;
	//the heat (Level.tileHeat) on a gas cell that sets it off: a fire on the cell, not one beside it
	public static final float IGNITE_HEAT = 40f;
	//a spent pocket fills again after three days of the world
	public static final int REFILL = 3 * DayNightCycle.FULL_CYCLE;
	//turns in the gas before the head swims
	public static final int DIZZY_TURNS = 3;
	//lake ice at or below this is thick everywhere; at or above the other, thin everywhere
	public static final float THICK_C = -10f, WARM_C = -2f;
	//a crack nobody stands on freezes over again after half a day
	public static final int CRACK_HEAL = DayNightCycle.FULL_CYCLE / 2;
	//turns standing on cracked ice before it groans, and before it gives
	public static final int STRAIN_GROAN = 2, STRAIN_BREAK = 3;
	//what a plunge through the ice takes off the body's warmth
	public static final float PLUNGE_CHILL = 6f;
	//the wind it takes to gust
	public static final float GUST_WIND = 6f;
	static final float LOOSE_SCALE = 34f;

	// ------------------------------------------------------------- firedamp

	/** Does this 16x16 world block of a deep cave slice hold a pocket? More of them deeper. */
	static boolean pocketBlock( long seed, int bx, int by, int altitude ){
		if (altitude > FIREDAMP_TOP || !WorldLayers.exists( altitude )) return false;
		int odds = Math.max( 4, 10 + (altitude - FIREDAMP_TOP) );
		return Math.floorMod( WorldModel.linkHash( seed ^ 0xF1DA4BL, bx, by, altitude ), odds ) == 0;
	}

	/** The key a pocket is spent by: its world block (each slice keeps its own store). */
	public static long pocketKey( int wx, int wy ){
		return OverworldLevel.worldKey( Math.floorDiv( wx, POCKET_BLOCK ), Math.floorDiv( wy, POCKET_BLOCK ) );
	}

	//a way between slices within r of the cell: a pit or a ladder down (open here and below), or a
	//ladder up (open here and above). the hashes are asked first; the field only where one hits
	static boolean nearWay( long seed, int wx, int wy, int altitude, int r ){
		boolean below = WorldLayers.exists( altitude - 1 ), above = altitude + 1 < 0;
		for (int y = wy - r; y <= wy + r; y++){
			for (int x = wx - r; x <= wx + r; x++){
				boolean down = below && (WindowGenerator.pitHash( seed, x, y, altitude )
						|| WindowGenerator.ladderHash( seed, x, y, altitude - 1 ));
				boolean up = above && WindowGenerator.ladderHash( seed, x, y, altitude );
				if (!down && !up) continue;
				if (!WorldModel.caveOpen( seed, x, y, altitude )) continue;
				if (down && WorldModel.caveOpen( seed, x, y, altitude - 1 )) return true;
				if (up && WorldModel.caveOpen( seed, x, y, altitude + 1 )) return true;
			}
		}
		return false;
	}

	//shut in: few of the cells two steps round it are open (a narrow tunnel, a dead end)
	static boolean enclosed( long seed, int wx, int wy, int altitude ){
		int open = 0;
		for (int d = -2; d <= 2; d++){
			if (WorldModel.caveOpen( seed, wx + d, wy - 2, altitude )) open++;
			if (WorldModel.caveOpen( seed, wx + d, wy + 2, altitude )) open++;
			if (d > -2 && d < 2){
				if (WorldModel.caveOpen( seed, wx - 2, wy + d, altitude )) open++;
				if (WorldModel.caveOpen( seed, wx + 2, wy + d, altitude )) open++;
			}
			if (open > ENCLOSED_MAX) return false;
		}
		return true;
	}

	//a dip in a chamber's floor the gas settles in: the ground three steps round it lies higher
	//or is rock nearly all the way round (a pool's shore runs on down into the water, so it is none)
	static boolean hollow( long seed, int wx, int wy, int altitude, float floor ){
		int higher = 0;
		WorldModel.CaveSample s = new WorldModel.CaveSample();
		for (int i = 0; i < 8; i++){
			WorldModel.caveSample( seed, wx + 3 * DX[i], wy + 3 * DY[i], altitude, s );
			if (!s.open || s.floor > floor) higher++;
			else if (i - higher >= 8 - HOLLOW_MIN) return false;
		}
		return higher >= HOLLOW_MIN;
	}

	/**
	 * Is this cave cell (its sample and generated terrain given) in firedamp? Deep slices only,
	 * plain floor, in a pocket block, on the damp low floor, in a shut-in tunnel or a dip of a
	 * chamber's floor, and never within WAY_CLEARANCE of a way between slices. The cheap tests
	 * come first: most cells leave on the block hash.
	 */
	public static boolean gassy( long seed, int wx, int wy, int altitude, WorldModel.CaveSample cs, int t ){
		if (altitude > FIREDAMP_TOP || !WorldLayers.exists( altitude ) || !cs.open) return false;
		if (t != Terrain.EMPTY && t != Terrain.EMPTY_DECO && t != Terrain.MUSHROOM_PATCH) return false;
		if (!pocketBlock( seed, Math.floorDiv( wx, POCKET_BLOCK ), Math.floorDiv( wy, POCKET_BLOCK ), altitude )) return false;
		boolean chamber = cs.chamber > WorldModel.chamberLevel( altitude );
		if (cs.floor >= (chamber ? LOW_CHAMBER : LOW_TUNNEL)) return false;
		if (chamber ? !hollow( seed, wx, wy, altitude, cs.floor ) : !enclosed( seed, wx, wy, altitude )) return false;
		return !nearWay( seed, wx, wy, altitude, WAY_CLEARANCE );
	}

	/**
	 * After the caves' ways a window's gas keeps WAY_CLEARANCE clear of every way cell the
	 * window holds (gassy already keeps it off the pits and ladders), of every mine shaft's top
	 * and foot (CaveSites: a mine of this slice sinks one, a mine of the slice above lands one
	 * here), and SITE_CLEARANCE clear of the reach of every place (a camp's fire or a rift's
	 * embers stand on what was the cave's floor). The shafts and places are asked of the world,
	 * not of the window, out to the clearance past its rim: the same cell has the same gas in
	 * every window. The window's origin is ox, oy.
	 */
	static void clearMask( WindowGenerator.Window w, long seed, int altitude, int ox, int oy ){
		boolean[] gas = w.firedamp;
		if (gas == null) return;
		int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
		for (int c = 0; c < gas.length; c++){
			if (w.link[c] == WindowGenerator.LINK_NONE && w.terrain[c] != Terrain.CHASM) continue;
			int cx = c % W, cy = c / W;
			clear( gas, cx - WAY_CLEARANCE, cy - WAY_CLEARANCE, cx + WAY_CLEARANCE, cy + WAY_CLEARANCE );
		}
		int r = WAY_CLEARANCE;
		int x0 = ox - r, y0 = oy - r, x1 = ox + W - 1 + r, y1 = oy + H - 1 + r;
		for (CaveSites.Site s : CaveSites.inRect( seed, altitude, x0, y0, x1, y1 )){
			clear( gas, s.x0 - SITE_CLEARANCE - ox, s.y0 - SITE_CLEARANCE - oy, s.x1 + SITE_CLEARANCE - ox, s.y1 + SITE_CLEARANCE - oy );
			if (s.hasShaft()) clear( gas, s.shaftX - r - ox, s.shaftY - r - oy, s.shaftX + r - ox, s.shaftY + r - oy );
		}
		int up = altitude + 1;
		if (CaveSites.Type.MINE.at( up )){
			for (int sy = Math.floorDiv( y0, CaveSites.SECTOR ); sy <= Math.floorDiv( y1, CaveSites.SECTOR ); sy++){
				for (int sx = Math.floorDiv( x0, CaveSites.SECTOR ); sx <= Math.floorDiv( x1, CaveSites.SECTOR ); sx++){
					if (CaveSites.typeOf( seed, up, sx, sy ) != CaveSites.Type.MINE) continue;
					CaveSites.Site m = CaveSites.site( seed, up, sx, sy );
					if (m == null || !m.hasShaft()) continue;
					clear( gas, m.shaftX - r - ox, m.shaftY - r - oy, m.shaftX + r - ox, m.shaftY + r - oy );
				}
			}
		}
	}

	//no gas in a window rect (inclusive, window cells), clipped to the window
	private static void clear( boolean[] gas, int x0, int y0, int x1, int y1 ){
		int W = WindowGenerator.WIDTH, H = WindowGenerator.HEIGHT;
		for (int y = Math.max( 0, y0 ); y <= Math.min( H - 1, y1 ); y++){
			for (int x = Math.max( 0, x0 ); x <= Math.min( W - 1, x1 ); x++){
				gas[x + y * W] = false;
			}
		}
	}

	// ----------------------------------------------------- loose rock, cave-ins

	/** A patch of loose rock in the caves: wider patches deeper down. */
	public static boolean looseRock( long seed, int wx, int wy, int altitude ){
		if (altitude >= 0 || !WorldLayers.exists( altitude )) return false;
		return WorldModel.fbm( seed ^ 0x100E5EL ^ (altitude * 0x9E37L), wx / LOOSE_SCALE, wy / LOOSE_SCALE, 2 )
				> looseLevel( altitude );
	}

	static float looseLevel( int altitude ){
		return 0.66f - 0.012f * (-altitude - 1);
	}

	static boolean openCell( int t ){
		return (Terrain.flags[t] & Terrain.SOLID) == 0;
	}

	//rock that bears the roof: anything solid but water, boulders (loose themselves) and doors
	static boolean holdsRoof( int t ){
		int f = Terrain.flags[t];
		return (f & Terrain.SOLID) != 0 && (f & Terrain.LIQUID) == 0
				&& t != Terrain.BOULDER && t != Terrain.MINE_BOULDER
				&& t != Terrain.DOOR && t != Terrain.LOCKED_DOOR;
	}

	/**
	 * A cell just mined out (map[cell] is already its floor) that leaves the roof unpropped:
	 * open floor on both sides of it, along a row or a column, and at most two of its eight
	 * neighbours still rock that bears the roof - a pillar, or a thin wall between two ways.
	 */
	public static boolean unsupported( int[] map, int width, int cell ){
		boolean span = (openCell( map[cell - 1] ) && openCell( map[cell + 1] ))
				|| (openCell( map[cell - width] ) && openCell( map[cell + width] ));
		if (!span) return false;
		int held = 0;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if ((dx != 0 || dy != 0) && holdsRoof( map[cell + dx + dy * width] )) held++;
			}
		}
		return held <= 2;
	}

	/** The chance a blow of the pick brings the roof down: caves only, worse deeper, at most 60%. */
	public static float collapseChance( boolean loose, boolean unsupported, int altitude ){
		if (altitude >= 0) return 0f;
		int d = -altitude - 1;
		float p = (unsupported ? 0.20f + 0.02f * d : 0f) + (loose ? 0.08f + 0.015f * d : 0f);
		return Math.min( 0.6f, p );
	}

	// ------------------------------------------------------------- thin ice

	private static boolean wet( int t ){
		return t == Terrain.FROZEN_WATER || t == Terrain.WATER || t == Terrain.DEEP_WATER;
	}

	/**
	 * Is this ice thin at this temperature? Only lake ice (a frozen puddle bears anyone), never
	 * in deep cold; on a mild day all of it, otherwise the ice by the shore - a cell with two
	 * or more land cells round it, so a wide lake keeps a thick core.
	 */
	public static boolean thinIce( int[] map, int width, int cell, float ambientC ){
		if (map[cell] != Terrain.FROZEN_WATER || ambientC <= THICK_C) return false;
		int water = 0, land = 0;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if (dx == 0 && dy == 0) continue;
				if (wet( map[cell + dx + dy * width] )) water++; else land++;
			}
		}
		if (water < 3) return false;
		return ambientC >= WARM_C || land >= 2;
	}

	// ------------------------------------------------------- ridges and gusts

	//the eight ways round a cell, from north clockwise: index = compass degrees / 45
	static final int[] DX = { 0, 1, 1, 1, 0, -1, -1, -1 };
	static final int[] DY = { -1, -1, 0, 1, 1, 1, 0, -1 };

	private static boolean pit( int t ){
		return (Terrain.flags[t] & Terrain.PIT) != 0;
	}

	/**
	 * A crest of the heights: walkable ground with a drop on both sides of it (or on half its
	 * neighbours at least), no higher rock beside it to break the wind.
	 */
	public static boolean ridgeCell( int[] map, int width, int cell ){
		int f = Terrain.flags[map[cell]];
		if ((f & Terrain.PASSABLE) == 0 || (f & (Terrain.PIT | Terrain.LIQUID)) != 0) return false;
		for (int n : new int[]{ cell - width, cell + 1, cell + width, cell - 1 }){
			if (map[n] == Terrain.WALL || map[n] == Terrain.WALL_DECO) return false;
		}
		int pits = 0;
		boolean[] p = new boolean[8];
		for (int i = 0; i < 8; i++){
			p[i] = pit( map[cell + DX[i] + DY[i] * width] );
			if (p[i]) pits++;
		}
		boolean crest = (p[2] && p[6]) || (p[0] && p[4]) || (p[7] && p[3]) || (p[1] && p[5]);
		return crest || pits >= 4;
	}

	/** The way (index into DX/DY) a wind blowing towards these compass degrees pushes. */
	public static int windDir( float degrees ){
		return Math.round( degrees / 45f ) & 7;
	}

	/**
	 * Where a gust blowing `dir` pushes someone standing on `cell`: the cell downwind, else the
	 * ones 45 degrees either side of it; never a pit, never anything solid, a way between
	 * slices or a cell `blocked` refuses. -1 when there is nowhere (he staggers in place).
	 */
	public static int gustTarget( int[] map, int width, int cell, int dir, IntPredicate blocked ){
		int height = map.length / width;
		for (int k : new int[]{ dir, (dir + 1) & 7, (dir + 7) & 7 }){
			int x = cell % width + DX[k], y = cell / width + DY[k];
			if (x <= 0 || y <= 0 || x >= width - 1 || y >= height - 1) continue;
			int c = x + y * width;
			int t = map[c];
			int f = Terrain.flags[t];
			if ((f & Terrain.PASSABLE) == 0 || (f & (Terrain.PIT | Terrain.SOLID)) != 0) continue;
			if (t == Terrain.ENTRANCE || t == Terrain.EXIT) continue;
			if (blocked != null && blocked.test( c )) continue;
			return c;
		}
		return -1;
	}

	/** The chance a hero on a ridge is warned of a gust this turn: the high slices, in a wind. */
	public static float gustChance( int altitude, float wind ){
		if (altitude < HIGH || wind < GUST_WIND) return 0f;
		return Math.min( 0.35f, (0.03f + 0.012f * (wind - GUST_WIND)) * (1f + 0.25f * (altitude - HIGH)) );
	}

	// ------------------------------------------------------------ thin air

	/** How short of breath one can get on this slice: none below HIGH, 2..5 from HIGH up. */
	public static int maxStacks( int altitude ){
		return altitude < HIGH ? 0 : Math.min( 5, altitude - HIGH + 2 );
	}

	/** Turns on this slice before the breath grows shorter by one more. */
	public static int stackEvery( int altitude ){
		return 40 - 5 * (altitude - HIGH);
	}

	// ------------------------------------------------- debug scenes and tests

	private static boolean plainFloor( int t ){
		return t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.MUSHROOM_PATCH;
	}

	/**
	 * The firedamp nearest the world origin on a deep slice, searching blocks out to `rings`
	 * blocks: {standX, standY, gasX, gasY}, the stand cell plain floor beside the gas and clear
	 * of it. Read off the field, not a window (no flooding from the surface, so for slices of
	 * FIREDAMP_TOP and below, where the surface's lakes never reach). null when there is none.
	 */
	public static int[] findFiredamp( long seed, int altitude, int rings ){
		WorldModel.CaveSample cs = new WorldModel.CaveSample(), ns = new WorldModel.CaveSample();
		for (int r = 0; r <= rings; r++){
			for (int by = -r; by <= r; by++){
				for (int bx = -r; bx <= r; bx++){
					if (Math.max( Math.abs( bx ), Math.abs( by ) ) != r || !pocketBlock( seed, bx, by, altitude )) continue;
					for (int y = by * POCKET_BLOCK; y < (by + 1) * POCKET_BLOCK; y++){
						for (int x = bx * POCKET_BLOCK; x < (bx + 1) * POCKET_BLOCK; x++){
							WorldModel.caveSample( seed, x, y, altitude, cs );
							if (!gassy( seed, x, y, altitude, cs, WorldModel.caveTerrain( seed, x, y, altitude, cs ) )) continue;
							for (int i = 0; i < 8; i += 2){
								int sx = x + DX[i], sy = y + DY[i];
								WorldModel.caveSample( seed, sx, sy, altitude, ns );
								int t = WorldModel.caveTerrain( seed, sx, sy, altitude, ns );
								if (plainFloor( t ) && !gassy( seed, sx, sy, altitude, ns, t )) return new int[]{ sx, sy, x, y };
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * The loose, thin wall nearest the world origin on a cave slice, searching rings out to
	 * `rings` cells: {x, y} of plain floor with a WALL of loose rock east of it and plain floor
	 * beyond, the wall standing alone enough that mining it leaves the roof unpropped. null
	 * when there is none.
	 */
	public static int[] findLooseWall( long seed, int altitude, int rings ){
		WorldModel.CaveSample cs = new WorldModel.CaveSample();
		int[] m = new int[9];
		for (int r = 0; r <= rings; r++){
			for (int y = -r; y <= r; y++){
				for (int x = -r; x <= r; x++){
					if (Math.max( Math.abs( x ), Math.abs( y ) ) != r) continue;
					if (!looseRock( seed, x + 1, y, altitude ) || WorldModel.caveOpen( seed, x + 1, y, altitude )) continue;
					if (WorldModel.caveTerrain( seed, x + 1, y, altitude, WorldModel.caveSample( seed, x + 1, y, altitude, cs ) ) != Terrain.WALL) continue;
					for (int dy = -1; dy <= 1; dy++){
						for (int dx = -1; dx <= 1; dx++){
							int wx = x + 1 + dx, wy = y + dy;
							m[(dx + 1) + (dy + 1) * 3] = WorldModel.caveTerrain( seed, wx, wy, altitude, WorldModel.caveSample( seed, wx, wy, altitude, cs ) );
						}
					}
					if (!plainFloor( m[3] ) || !plainFloor( m[5] )) continue;
					m[4] = Terrain.EMPTY_DECO;
					if (unsupported( m, 3, 4 )) return new int[]{ x, y };
				}
			}
		}
		return null;
	}

	//the window origins on a 176-cell grid about the world origin, ring by ring, where at
	//least a few samples of the ground stand on this mountain slice's band or above
	private interface WindowTest {
		int[] test( WindowGenerator.Window w, int ox, int oy );
	}

	private static int[] searchWindows( long seed, int altitude, float shift, int rings, int maxWindows, WindowTest t ){
		int W = WindowGenerator.WIDTH, made = 0;
		for (int r = 0; r <= rings; r++){
			for (int gy = -r; gy <= r; gy++){
				for (int gx = -r; gx <= r; gx++){
					if (Math.max( Math.abs( gx ), Math.abs( gy ) ) != r) continue;
					int ox = gx * W - W / 2, oy = gy * W - W / 2;
					int high = 0;
					for (int sy = 8; sy < W; sy += 16){
						for (int sx = 8; sx < W; sx += 16){
							if (WorldLayers.band( WorldModel.elevation( seed, ox + sx, oy + sy ) ) >= altitude) high++;
						}
					}
					if (high < 12) continue;
					int[] hit = t.test( WindowGenerator.generate( seed, altitude, ox, oy, shift ), ox, oy );
					if (hit != null) return hit;
					if (++made >= maxWindows) return null;
				}
			}
		}
		return null;
	}

	/**
	 * Lake ice of a mountain slice that is thin at this temperature, nearest the world origin:
	 * {x, y} of the land beside it, at least 8 cells inside its window. Generates at most
	 * `maxWindows` windows. null when there is none.
	 */
	public static int[] findThinIce( long seed, int altitude, float shift, float ambientC, int rings, int maxWindows ){
		final int W = WindowGenerator.WIDTH;
		return searchWindows( seed, altitude, shift, rings, maxWindows, (w, ox, oy) -> {
			for (int y = 8; y < W - 8; y++){
				for (int x = 8; x < W - 8; x++){
					int c = x + y * W, t = w.terrain[c];
					int f = Terrain.flags[t];
					if ((f & Terrain.PASSABLE) == 0 || (f & (Terrain.PIT | Terrain.LIQUID)) != 0 || t == Terrain.FROZEN_WATER) continue;
					for (int n : new int[]{ c - W, c + 1, c + W, c - 1 }){
						if (thinIce( w.terrain, W, n, ambientC )) return new int[]{ ox + x, oy + y };
					}
				}
			}
			return null;
		} );
	}

	/** A ridge of a high slice nearest the world origin, at least 8 cells inside its window: {x, y}. */
	public static int[] findRidge( long seed, int altitude, float shift, int rings, int maxWindows ){
		final int W = WindowGenerator.WIDTH;
		return searchWindows( seed, altitude, shift, rings, maxWindows, (w, ox, oy) -> {
			for (int y = 8; y < W - 8; y++){
				for (int x = 8; x < W - 8; x++){
					if (ridgeCell( w.terrain, W, x + y * W )) return new int[]{ ox + x, oy + y };
				}
			}
			return null;
		} );
	}
}

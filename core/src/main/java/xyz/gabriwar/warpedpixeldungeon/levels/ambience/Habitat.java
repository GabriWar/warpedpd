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

package xyz.gabriwar.warpedpixeldungeon.levels.ambience;

import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

import java.util.Arrays;
import java.util.function.IntPredicate;

/**
 * Where the dungeon's small life is found, read off a floor's map: wall bases, room corners,
 * water's edges and open water, the lights moths circle, statues birds perch on, bookshelves,
 * the cold lava's edge, moss and webbing; and the ways a creature runs along its haunt or away
 * into the dark. Pure functions over (map, width...), so the tests pin them on hand-built maps.
 *
 * The floor's critters are drawn under the walls (GameScene.floorEffect), and everything the
 * cell below draws over its upper neighbour - a wall's lip, a statue's or a door's top, a tuft
 * of long grass - would hide part of one standing there; so a critter of the floor only ever
 * stands where nothing is drawn over it (clearBelow).
 */
final class Habitat {

	private Habitat(){}

	private static final int SIZE = DungeonTilemap.SIZE;

	/** One cell in from the map's edge: every neighbour is on the map. */
	static boolean inside( int w, int h, int cell ){
		int x = cell % w, y = cell / w;
		return cell >= 0 && x >= 1 && y >= 1 && x <= w - 2 && y <= h - 2;
	}

	/**
	 * Dry floor a small creature stands on and is seen on: bare floor, moss and stains, grass,
	 * burnt ground, a spent trap - and a hidden one, which is drawn as the bare floor: a critter
	 * that shied from it would give it away. Never water, long grass (drawn over it), a door, the
	 * stairs, a trap in plain sight, a pedestal or a well.
	 */
	static boolean ground( int t ){
		return t == Terrain.EMPTY || t == Terrain.EMPTY_SP || t == Terrain.EMPTY_DECO || t == Terrain.GRASS
				|| t == Terrain.EMBERS || t == Terrain.INACTIVE_TRAP || t == Terrain.SECRET_TRAP;
	}

	static boolean liquid( int t ){
		return (Terrain.flags[t] & Terrain.LIQUID) != 0;
	}

	/** Rock that shows a face over the floor below it: a wall, a decorated one, a hidden door. */
	static boolean rock( int t ){
		return t == Terrain.WALL || t == Terrain.WALL_DECO || t == Terrain.SECRET_DOOR;
	}

	/**
	 * Nothing in the cell below is drawn over this one: no wall's lip, no door's, statue's,
	 * pedestal's, scaffold's or barricade's top, no tuft of long grass, no custom art standing
	 * two cells tall (the nest's silk pillars).
	 */
	static boolean clearBelow( int[] map, int w, int cell ){
		int below = cell + w;
		if (below >= map.length) return false;
		int t = map[below];
		if (DungeonTileSheet.wallStitcheable( t )) return false;
		switch (t){
			case Terrain.DOOR: case Terrain.OPEN_DOOR: case Terrain.LOCKED_DOOR: case Terrain.HERO_LKD_DR:
			case Terrain.CRYSTAL_DOOR: case Terrain.STATUE: case Terrain.STATUE_SP: case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT: case Terrain.MINE_CRYSTAL: case Terrain.MINE_BOULDER: case Terrain.ALCHEMY:
			case Terrain.BARRICADE: case Terrain.HIGH_GRASS: case Terrain.FURROWED_GRASS:
			case Terrain.CUSTOM_DECO: case Terrain.CUSTOM_DECO_WTR:
				return false;
			default:
				return true;
		}
	}

	/** Ground a critter of the floor stands on: dry, and nothing drawn over it from below. */
	static boolean stand( int[] map, int w, int cell ){
		return ground( map[cell] ) && clearBelow( map, w, cell );
	}

	/** At the foot of a wall's face: rock straight above. */
	static boolean wallBase( int[] map, int w, int cell ){
		return cell >= w && rock( map[cell - w] );
	}

	/** Against rock to the west or the east. */
	static boolean wallSide( int[] map, int w, int cell ){
		return rock( map[cell - 1] ) || rock( map[cell + 1] );
	}

	/** A room's corner: rock above or below it, and rock to one side. */
	static boolean corner( int[] map, int w, int cell ){
		return (rock( map[cell - w] ) || rock( map[cell + w] )) && wallSide( map, w, cell );
	}

	//north, east, south, west: the order a creature at the edge looks for the water
	private static int[] sides( int w ){
		return new int[]{ -w, 1, w, -1 };
	}

	/** The water beside a cell that a creature at its edge slips into (north, east, south, west), or -1. Ice is no water. */
	static int waterBeside( int[] map, int w, int cell ){
		for (int o : sides( w )){
			int c = cell + o;
			if (c >= 0 && c < map.length && liquid( map[c] )) return c;
		}
		return -1;
	}

	/** Dry ground with water beside it: a frog's bank, a newt's. In the halls the water is the cold lava, so this is its edge too. */
	static boolean waterEdge( int[] map, int w, int cell ){
		return !liquid( map[cell] ) && waterBeside( map, w, cell ) >= 0;
	}

	/** The cold lava's edge: the halls' lava is water to the map (HallsLevel), so it is the water's edge rule. */
	static boolean lavaEdge( int[] map, int w, int cell ){
		return waterEdge( map, w, cell );
	}

	/** Open water: liquid, and five of its eight neighbours too (Level's rule for a pool big enough to hold a piranha). Never a frozen pool. */
	static boolean openWater( int[] map, int w, int h, int cell ){
		if (!inside( w, h, cell ) || !liquid( map[cell] )) return false;
		int n = 0;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if ((dx != 0 || dy != 0) && liquid( map[cell + dx + dy * w] )) n++;
			}
		}
		return n >= 5;
	}

	/** A drain pipe: a decorated wall spilling into the water below it (the sewers' WALL_DECO). */
	static boolean pipe( int[] map, int w, int cell ){
		return map[cell] == Terrain.WALL_DECO && cell + w < map.length && liquid( map[cell + w] );
	}

	/** A light on a wall over open ground: the prison's torches, the frozen branch's glimmering ice seams. */
	static boolean torch( int[] map, int w, int cell ){
		return map[cell] == Terrain.WALL_DECO && cell + w < map.length
				&& (Terrain.flags[map[cell + w]] & Terrain.PASSABLE) != 0 && !liquid( map[cell + w] );
	}

	/** The city's green flame on its pedestal. */
	static boolean flame( int t ){
		return t == Terrain.REGION_DECO || t == Terrain.REGION_DECO_ALT;
	}

	/** The warm floor under a smoke vent (the city's decorated walls breathe smoke). */
	static boolean ventBase( int[] map, int w, int cell ){
		return cell >= w && map[cell - w] == Terrain.WALL_DECO;
	}

	/** A statue whose top is drawn into open air, where a bird can sit: rock above would hide it. */
	static boolean perch( int[] map, int w, int cell ){
		int t = map[cell];
		return (t == Terrain.STATUE || t == Terrain.STATUE_SP) && cell >= w
				&& !DungeonTileSheet.wallStitcheable( map[cell - w] );
	}

	/** At the foot of a bookshelf. */
	static boolean shelfBase( int[] map, int w, int cell ){
		return cell >= w && (map[cell - w] == Terrain.BOOKSHELF || map[cell - w] == Terrain.EMPTY_BOOKSHELF);
	}

	/**
	 * Moss as each place has it: the sewers' (and the catacombs') wet yellow moss is their floor
	 * decoration; the caves' fluorescent moss, the halls' embermoss and the frozen branch's weeds
	 * are their grass.
	 */
	static boolean moss( Place p, int t ){
		if (p == Place.SEWERS || p == Place.CATACOMB) return t == Terrain.EMPTY_DECO;
		if (p == Place.CAVES || p == Place.HALLS || p == Place.FROZEN || p == Place.MINES) return t == Terrain.GRASS;
		return false;
	}

	/** The nest's webbing: its grass, and the dense webbing of its long grass. */
	static boolean webbing( int t ){
		return t == Terrain.GRASS || t == Terrain.HIGH_GRASS;
	}

	/**
	 * Beside the dwarves' metal scaffolding, minecart tracks along its top: the caves' REGION_DECO
	 * and REGION_DECO_ALT, one structure drawn and named alike (and the ALT the one most of their
	 * rooms lay).
	 */
	static boolean byScaffold( int[] map, int w, int h, int cell ){
		if (!inside( w, h, cell )) return false;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				int t = map[cell + dx + dy * w];
				if ((dx != 0 || dy != 0) && (t == Terrain.REGION_DECO || t == Terrain.REGION_DECO_ALT)) return true;
			}
		}
		return false;
	}

	/** A face of rock over open floor with fluorescent moss at its foot: where the glow-worms cling. */
	static boolean glowWall( int[] map, int w, int h, int cell ){
		int below = cell + w;
		if (!rock( map[cell] ) || !inside( w, h, below ) || (Terrain.flags[map[below]] & Terrain.PASSABLE) == 0) return false;
		for (int dx = -1; dx <= 1; dx++){
			if (map[below + dx] == Terrain.GRASS) return true;
		}
		return false;
	}

	/** Floor with room about it (five of its eight neighbours open): a chamber's still air, not a passage. */
	static boolean roomy( int[] map, int w, int h, int cell ){
		if (!inside( w, h, cell ) || (Terrain.flags[map[cell]] & Terrain.PASSABLE) == 0) return false;
		int n = 0;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if ((dx != 0 || dy != 0) && (Terrain.flags[map[cell + dx + dy * w]] & Terrain.PASSABLE) != 0) n++;
			}
		}
		return n >= 5;
	}

	/**
	 * Open ground a flock comes down on: standing ground (or, where `ice`, a frozen pool), and
	 * no rock alongside (a bird would sit under the wall's face).
	 */
	static boolean flockGround( int[] map, int w, int h, int cell, boolean ice ){
		if (!inside( w, h, cell )) return false;
		if (!stand( map, w, cell ) && !(ice && map[cell] == Terrain.FROZEN_WATER && clearBelow( map, w, cell ))) return false;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if (DungeonTileSheet.wallStitcheable( map[cell + dx + dy * w] )) return false;
			}
		}
		return true;
	}

	/** Where a hare hides: long grass, or the frozen branch's frost-rimed thickets (its long grass too). */
	static boolean cover( int t ){
		return t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS;
	}

	/** Ground a hare can cross: dry floor, ice, or the cover it dives into. */
	static boolean runnable( int t ){
		return ground( t ) || t == Terrain.FROZEN_WATER || cover( t );
	}

	/** The first cover beside a cell (north, then clockwise), or -1: where a hare hops out from. */
	static int coverBeside( int[] map, int w, int cell ){
		int[] around = { -w, -w + 1, 1, w + 1, w, w - 1, -1, -w - 1 };
		for (int o : around){
			int c = cell + o;
			if (c >= 0 && c < map.length && cover( map[c] )) return c;
		}
		return -1;
	}

	/** Is (px, py) within r of the segment from (ax, ay) to (bx, by)? */
	static boolean nearSegment( float px, float py, float ax, float ay, float bx, float by, float r ){
		float vx = bx - ax, vy = by - ay, l2 = vx * vx + vy * vy;
		float t = l2 <= 0f ? 0f : Math.max( 0f, Math.min( 1f, ((px - ax) * vx + (py - ay) * vy) / l2 ) );
		float qx = ax + vx * t - px, qy = ay + vy * t - py;
		return qx * qx + qy * qy <= r * r;
	}

	// ------------------------------------------------------------ who lives where

	/** May this kind turn up on (or, for what flies or hangs, at) this cell of this place? */
	static boolean groundFor( Fauna.Kind k, Place p, int[] map, int w, int h, int cell ){
		if (!inside( w, h, cell )) return false;
		int t = map[cell];
		switch (k){
			case FROG:
				return stand( map, w, cell ) && (waterEdge( map, w, cell ) || moss( p, t ));
			case ROACH: case CENTIPEDE:
				return stand( map, w, cell ) && wallBase( map, w, cell );
			case MOUSE:
				return stand( map, w, cell ) && corner( map, w, cell );
			case MOTH:
				return mothLight( p, map, w, h, cell ) || (!lit( p ) && restingWall( map, w, cell ));
			case SPIDER:
				//it hangs on its silk in front of the plain face above this cell: never over a
				//torch, a pipe or a vent
				return wallBase( map, w, cell ) && map[cell - w] != Terrain.WALL_DECO
						&& (Terrain.flags[t] & (Terrain.PASSABLE | Terrain.LIQUID)) != 0
						&& t != Terrain.DOOR && t != Terrain.OPEN_DOOR;
			case SPIDERLING:
				return stand( map, w, cell ) && t == Terrain.GRASS;
			case NEWT: case SALAMANDER:
				return stand( map, w, cell ) && waterEdge( map, w, cell );
			case LIZARD:
				return stand( map, w, cell ) && ventBase( map, w, cell );
			case SNAIL:
				return stand( map, w, cell ) && moss( p, t );
			case BEETLE:
				return stand( map, w, cell ) && (p == Place.CAVES ? byScaffold( map, w, h, cell )
						: wallBase( map, w, cell ) || wallSide( map, w, cell ));
			case EMBER_BEETLE:
				return stand( map, w, cell ) && moss( p, t );
			case SILVERFISH:
				return stand( map, w, cell ) && shelfBase( map, w, cell );
			case STRIDER: case FISH:
				return openWater( map, w, h, cell );
			case FLY:
				//stuck in the flat webbing, where it can be seen struggling
				return stand( map, w, cell ) && t == Terrain.GRASS;
			case SWIFT: case CROW:
				return perch( map, w, cell );
			case BUNTING:
				return flockGround( map, w, h, cell, true );
			case FINCH: case GULL:
				return flockGround( map, w, h, cell, false );
			case HARE:
				return (stand( map, w, cell ) || (t == Terrain.FROZEN_WATER && clearBelow( map, w, cell )))
						&& !wallBase( map, w, cell );
			case BUTTERFLY:
				//the city's over its high blooming flowers, the meadow's over any of its grass
				return t == Terrain.HIGH_GRASS || (p == Place.MEADOW && t == Terrain.GRASS);
			default:
				return false;
		}
	}

	/** Does this place light its moths: torches, flames, glowing lava? Elsewhere they rest on the walls. */
	static boolean lit( Place p ){
		return p == Place.PRISON || p == Place.CITY || p == Place.VAULT || p == Place.HALLS || p == Place.FROZEN;
	}

	/**
	 * The light a moth of this place circles: a torch or an ice seam, a green flame, the halls'
	 * cold lava and embers.
	 */
	static boolean mothLight( Place p, int[] map, int w, int h, int cell ){
		if (!inside( w, h, cell )) return false;
		switch (p){
			case PRISON: case FROZEN:
				return torch( map, w, cell );
			case CITY: case VAULT:
				return flame( map[cell] );
			case HALLS:
				return liquid( map[cell] ) || map[cell] == Terrain.EMBERS;
			default:
				return false;
		}
	}

	/** Floor under a plain rock face a moth rests on, wings flat against the stone. */
	static boolean restingWall( int[] map, int w, int cell ){
		return wallBase( map, w, cell ) && map[cell - w] != Terrain.WALL_DECO
				&& (Terrain.flags[map[cell]] & Terrain.PASSABLE) != 0
				&& map[cell] != Terrain.DOOR && map[cell] != Terrain.OPEN_DOOR;
	}

	/**
	 * May this air hang over this cell of this place? Bones and remains lying about are heaps,
	 * not terrain: DungeonLife adds those.
	 */
	static boolean airFor( Fauna.Air a, Place p, int[] map, int w, int h, int cell ){
		if (!inside( w, h, cell )) return false;
		int t = map[cell];
		switch (a){
			case GNATS:
				return liquid( t );
			case DRIPS:
				if (p == Place.SEWERS || p == Place.CATACOMB) return liquid( t );
				//the caves and the mines drip into their pools and by their rock
				return liquid( t ) || (stand( map, w, cell ) && (wallBase( map, w, cell ) || wallSide( map, w, cell )));
			case FLIES:
				//the prison's blood stains, the catacombs' wet moss by the walls, and the halls'
				//remains that stand: their pillars, made of real skulls
				return ((p == Place.PRISON || p == Place.CATACOMB) && t == Terrain.EMPTY_DECO)
						|| (p == Place.HALLS && (t == Terrain.STATUE || t == Terrain.STATUE_SP));
			case GLOW_WORMS:
				return glowWall( map, w, h, cell );
			case SPORES:
				return t == Terrain.HIGH_GRASS;
			case EMBERS:
				return t == Terrain.GRASS || t == Terrain.EMBERS;
			case SILK:
				return webbing( t ) || t == Terrain.EMPTY_DECO;
			case DUST:
				return roomy( map, w, h, cell ) && !liquid( t );
			case FIREFLIES:
				return t == Terrain.GRASS || t == Terrain.HIGH_GRASS;
			default:
				return false;
		}
	}

	// ------------------------------------------------------------ ways

	/** The ground point (feet) on a cell's lane, scene px: at the foot of the face above, or a little below the cell's middle. */
	static float laneY( int[] map, int w, int cell ){
		return (cell / w) * SIZE + (wallBase( map, w, cell ) ? 4f : 11f);
	}

	/** Up to `cells` steps from `start` by `dir` (1 or -1 along a row, w or -w down a column) while `ok` holds: the cells stepped on. */
	static int[] along( int start, int dir, int cells, IntPredicate ok ){
		int[] out = new int[cells];
		int n = 0, c = start;
		while (n < cells){
			c += dir;
			if (!ok.test( c )) break;
			out[n++] = c;
		}
		return Arrays.copyOf( out, n );
	}

	//a step to a neighbour: never through rock at a corner, both sides of a diagonal must be open
	private static boolean step( int[] map, int w, int h, int from, int dx, int dy, IntPredicate ok ){
		int to = from + dx + dy * w;
		if (!inside( w, h, to ) || !ok.test( to )) return false;
		return dx == 0 || dy == 0 || (ok.test( from + dx ) && ok.test( from + dy * w ));
	}

	/**
	 * Away from (sx, sy), in cell coordinates, over ground `ok` allows: each step to the open
	 * neighbour that gains the most distance from it, keeping to the creature's lane where it can
	 * (a roach's wall base, a mouse's walls), never cutting a corner of rock. Up to `cells` steps,
	 * stopping where no step gains ground; empty when hemmed in.
	 */
	static int[] away( int[] map, int w, int h, int start, float sx, float sy, int cells, IntPredicate ok, IntPredicate lane ){
		int[] out = new int[cells];
		int n = 0, c = start;
		while (n < cells){
			float cx = c % w + 0.5f, cy = c / w + 0.5f;
			float here = (cx - sx) * (cx - sx) + (cy - sy) * (cy - sy);
			int best = -1;
			float bestScore = 0f;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					if (dx == 0 && dy == 0) continue;
					if (!step( map, w, h, c, dx, dy, ok )) continue;
					int to = c + dx + dy * w;
					float tx = cx + dx, ty = cy + dy;
					float gain = (tx - sx) * (tx - sx) + (ty - sy) * (ty - sy) - here;
					if (gain <= 0f) continue;
					//a diagonal gains more by its length alone: weigh by the step, and favour the lane
					float score = gain / (dx != 0 && dy != 0 ? 1.41f : 1f) + (lane.test( to ) ? 2f : 0f);
					if (best < 0 || score > bestScore){
						best = to;
						bestScore = score;
					}
				}
			}
			if (best < 0) break;
			out[n++] = best;
			c = best;
		}
		return Arrays.copyOf( out, n );
	}

	/** A walker's own haunt: what it keeps to when it potters about, and prefers when it flees. */
	static IntPredicate haunt( Fauna.Kind k, Place p, int[] map, int w, int h ){
		switch (k){
			case ROACH: case CENTIPEDE:
				return c -> stand( map, w, c ) && wallBase( map, w, c );
			case SILVERFISH:
				return c -> stand( map, w, c ) && (shelfBase( map, w, c ) || wallBase( map, w, c ));
			case MOUSE:
				return c -> stand( map, w, c ) && (wallBase( map, w, c ) || wallSide( map, w, c ));
			case SPIDERLING:
				return c -> stand( map, w, c ) && map[c] == Terrain.GRASS;
			case NEWT: case SALAMANDER:
				return c -> stand( map, w, c ) && waterEdge( map, w, c );
			case LIZARD:
				return c -> stand( map, w, c ) && (ventBase( map, w, c ) || wallBase( map, w, c ));
			default:
				return c -> inside( w, h, c ) && groundFor( k, p, map, w, h, c );
		}
	}

	/** A walker's next little way about its haunt from `start`: along a wall base it runs the row; elsewhere a step or two. Empty: it stays. */
	static int[] wander( Fauna.Kind k, Place p, int[] map, int w, int h, int start, java.util.Random rng ){
		IntPredicate ok = haunt( k, p, map, w, h );
		int far = k == Fauna.Kind.ROACH || k == Fauna.Kind.SILVERFISH ? 3 : 2;
		int[] dirs;
		if (k == Fauna.Kind.ROACH || k == Fauna.Kind.CENTIPEDE || k == Fauna.Kind.SILVERFISH){
			dirs = new int[]{ 1, -1 };
		} else {
			dirs = new int[]{ 1, -1, w, -w };
		}
		//a random way first, the others if it leads nowhere
		int first = rng.nextInt( dirs.length );
		for (int i = 0; i < dirs.length; i++){
			int dir = dirs[(first + i) % dirs.length];
			int[] way = along( start, dir, 1 + rng.nextInt( far ), c -> inside( w, h, c ) && ok.test( c ) );
			if (way.length > 0) return way;
		}
		return new int[0];
	}

	/** A walker's flight from (sx, sy), cell coordinates: over any standing ground, along its haunt where it can. */
	static int[] flee( Fauna.Kind k, Place p, int[] map, int w, int h, int start, float sx, float sy ){
		int cells = k == Fauna.Kind.MOUSE || k == Fauna.Kind.ROACH ? 4 : 3;
		return away( map, w, h, start, sx, sy, cells, c -> stand( map, w, c ), haunt( k, p, map, w, h ) );
	}

	/** A hare's dash: waypoints in cell coordinates (centres at +0.5), x0,y0,x1,y1..., and whether it ends in cover. */
	static final class Dash {
		float[] pts;
		boolean cover;
	}

	/**
	 * Away from (srcX, srcY), in cell coordinates: legs of about a cell and a half, zig-zagging,
	 * ending in the first cover straight ahead or a cell or two to the side (three to seven cells
	 * on), or with none seven cells out, where it is gone into the dark. Every leg runs over ground
	 * a hare can cross; a leg that would not stops the dash short of it.
	 */
	static Dash dash( int[] map, int w, int h, int start, float srcX, float srcY, java.util.Random rng ){
		float sx = start % w + 0.5f, sy = start / w + 0.5f;
		float dx = sx - srcX, dy = sy - srcY;
		float len = (float)Math.hypot( dx, dy );
		if (len < 0.01f){
			double a = rng.nextDouble() * Math.PI * 2;
			dx = (float)Math.cos( a );
			dy = (float)Math.sin( a );
		} else {
			dx /= len;
			dy /= len;
		}
		float px = -dy, py = dx;
		Dash d = new Dash();
		float tx = 0f, ty = 0f;
		search:
		for (int k = 3; k <= 7; k++){
			for (int lat : new int[]{ 0, -1, 1, -2, 2 }){
				int cx = (int)Math.floor( sx + dx * k + px * lat ), cy = (int)Math.floor( sy + dy * k + py * lat );
				if (cx < 1 || cy < 1 || cx > w - 2 || cy > h - 2) continue;
				if (cover( map[cx + cy * w] )){
					tx = cx + 0.5f;
					ty = cy + 0.5f;
					d.cover = true;
					break search;
				}
			}
		}
		if (!d.cover){
			tx = Math.max( 1.5f, Math.min( w - 1.5f, sx + dx * 7f ) );
			ty = Math.max( 1.5f, Math.min( h - 1.5f, sy + dy * 7f ) );
		}
		float dist = (float)Math.hypot( tx - sx, ty - sy );
		int legs = Math.max( 2, Math.min( 5, Math.round( dist / 1.6f ) ) );
		float[] pts = new float[legs * 2];
		int n = 0;
		float side = rng.nextBoolean() ? 1f : -1f;
		float lx = sx, ly = sy;
		for (int i = 1; i <= legs; i++){
			float f = i / (float)legs;
			float bx = sx + (tx - sx) * f, by = sy + (ty - sy) * f;
			float zig = i == legs ? 0f : side * (0.4f + rng.nextFloat() * 0.5f);
			side = -side;
			float wx = bx + px * zig, wy = by + py * zig;
			if (!legRunnable( map, w, h, lx, ly, wx, wy )){
				//the zig would cross something: straight on instead, or stop short of it
				wx = bx;
				wy = by;
				if (!legRunnable( map, w, h, lx, ly, wx, wy )){
					d.cover = false;
					break;
				}
			}
			pts[n++] = wx;
			pts[n++] = wy;
			lx = wx;
			ly = wy;
		}
		//hemmed in: a startled jump on the spot, and it is gone there
		d.pts = n == 0 ? new float[]{ sx + dx * 0.3f, sy + dy * 0.3f } : Arrays.copyOf( pts, n );
		return d;
	}

	//every half cell along the leg is ground a hare can cross
	static boolean legRunnable( int[] map, int w, int h, float ax, float ay, float bx, float by ){
		int steps = Math.max( 1, (int)Math.ceil( Math.hypot( bx - ax, by - ay ) * 2 ) );
		for (int i = 1; i <= steps; i++){
			float f = i / (float)steps;
			int cx = (int)Math.floor( ax + (bx - ax) * f ), cy = (int)Math.floor( ay + (by - ay) * f );
			if (cx < 1 || cy < 1 || cx > w - 2 || cy > h - 2 || !runnable( map[cx + cy * w] )) return false;
		}
		return true;
	}
}

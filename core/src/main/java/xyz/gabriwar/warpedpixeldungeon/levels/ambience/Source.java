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

/**
 * Where a voice's sound comes from (Voice.from): a rule over the map around a cell, which the
 * scheduler (Soundscape) tries on random cells near the hero until one fits. The same terrain
 * is a different thing from place to place - a wall decoration is the sewers' drain pipe, the
 * prison's torch, the city's smoke vent, the frozen branch's ice seam - so each place's voices
 * (AmbientSounds.voices) name the rule for what their sound comes from there.
 *
 * The rules read a cell's eight neighbours: the scheduler only tries cells inside the map's
 * outer ring.
 */
enum Source {

	/** no cell: the floor's own air, heard centred */
	AIR,
	/** where a drop from the roof lands: water, or dry floor beside the rock */
	DRIP,
	/** a drain pipe: a wall decoration with water under it (the sewers', the catacombs') */
	PIPE,
	/** a drain pipe that pours, a sheet of water into its pool, where the rest trickle: one pipe in three */
	PIPE_POUR,
	/** a drain: a pipe, or water against the wall */
	DRAIN,
	/** the water's edge: water with dry ground beside it, or dry ground beside water */
	SHORE,
	/** open water: liquid, and five of its eight neighbours too */
	BIG_WATER,
	/** a body of water's edge, where it laps: liquid with dry ground beside it, and three liquid neighbours or more */
	LAPPING,
	/** grass, long grass, flowers, crops and bushes */
	GRASS,
	/** dry floor at the foot of a wall */
	WALL_BASE,
	/** dry open floor */
	FLOOR,
	/** rock showing over open ground (a wall with floor or water below it), or a boulder */
	ROCK_FACE,
	/** a chasm */
	CHASM,
	/** the prison's torches: its wall decorations */
	TORCH,
	/** a cage, standing or hanging, or a door (the prison's cells) */
	CAGE,
	/** a cage hanging on its chain over a chasm (the prison's alternate decoration) */
	HANGING,
	/** the dwarves' metal structures with their minecart tracks (the caves' decorations) */
	SCAFFOLD,
	/** the green flames on their pedestals (the city's decorations) */
	FLAME,
	/** the city's smoke vents: its wall decorations */
	VENT,
	/** a statue, a ledge for a dove */
	STATUE,
	/** the halls' cold lava: their water */
	LAVA,
	/** the halls' embermoss and emberfungi, embers, and the books smouldering on their shelves */
	EMBERS,
	/** the frozen branch's ice: frozen water, the ice clusters, the ice seams in the rock */
	ICE,
	/** the overworld's ice: frozen water, or open ground frozen over */
	FROST,
	/** where meltwater falls: open ground or water under an ice seam (the frozen branch) */
	MELT,
	/** the nest's webbing, and the husks wrapped in it */
	WEB,
	/** a tree or a bush */
	TREE;

	/** Does this cell of the ground fit the rule? The air fits none. */
	boolean test( Soundscape.Ground g, int cell ){
		int[] map = g.map;
		int w = g.w, t = map[cell];
		switch (this){
			case DRIP:         return drip( map, w, cell );
			case PIPE:         return pipe( map, w, cell );
			case PIPE_POUR:    return pipe( map, w, cell ) && pours( cell );
			case DRAIN:        return drain( map, w, cell );
			case SHORE:        return shore( map, w, cell );
			case BIG_WATER:    return liquid( t ) && liquidAround( map, w, cell ) >= 5;
			case LAPPING:      return lapping( map, w, cell );
			case GRASS:        return grass( t );
			case WALL_BASE:    return wallBase( map, w, cell );
			case FLOOR:        return dry( t );
			case ROCK_FACE:    return rockFace( map, w, cell );
			case CHASM:        return (Terrain.flags[t] & Terrain.PIT) != 0;
			case TORCH:
			case VENT:         return t == Terrain.WALL_DECO;
			case CAGE:         return t == Terrain.REGION_DECO || t == Terrain.REGION_DECO_ALT
					|| t == Terrain.DOOR || t == Terrain.OPEN_DOOR;
			case HANGING:      return t == Terrain.REGION_DECO_ALT;
			case SCAFFOLD:
			case FLAME:        return t == Terrain.REGION_DECO || t == Terrain.REGION_DECO_ALT;
			case STATUE:       return t == Terrain.STATUE || t == Terrain.STATUE_SP;
			case LAVA:         return t == Terrain.WATER;
			case EMBERS:       return t == Terrain.GRASS || t == Terrain.HIGH_GRASS
					|| t == Terrain.EMBERS || t == Terrain.BOOKSHELF;
			case ICE:          return t == Terrain.FROZEN_WATER || t == Terrain.REGION_DECO || t == Terrain.WALL_DECO;
			case FROST:        return frost( t, g.frozen( cell ) );
			case MELT:         return melt( map, w, cell );
			case WEB:          return t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.EMPTY_DECO;
			case TREE:         return t == Terrain.TREE_OAK || t == Terrain.TREE_PINE || t == Terrain.SHRUB;
			default:           return false;
		}
	}

	// ------------------------------------------------------------ the rules, over a map w wide

	static boolean liquid( int t ){
		return (Terrain.flags[t] & Terrain.LIQUID) != 0;
	}

	/** Ground to stand on, out of the water. */
	static boolean dry( int t ){
		int f = Terrain.flags[t];
		return (f & Terrain.PASSABLE) != 0 && (f & Terrain.LIQUID) == 0;
	}

	static boolean rock( int t ){
		return t == Terrain.WALL || t == Terrain.WALL_DECO;
	}

	//to walk on or swim in
	private static boolean open( int t ){
		return (Terrain.flags[t] & (Terrain.PASSABLE | Terrain.LIQUID)) != 0;
	}

	static boolean grass( int t ){
		return t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS
				|| t == Terrain.FLOWER_PATCH || t == Terrain.SHRUB;
	}

	/** How many of the cell's eight neighbours are liquid. */
	static int liquidAround( int[] map, int w, int cell ){
		int n = 0;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if ((dx != 0 || dy != 0) && liquid( map[cell + dx + dy * w] )) n++;
			}
		}
		return n;
	}

	private static boolean dryAround( int[] map, int w, int cell ){
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if ((dx != 0 || dy != 0) && dry( map[cell + dx + dy * w] )) return true;
			}
		}
		return false;
	}

	private static boolean rockAround( int[] map, int w, int cell ){
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				if ((dx != 0 || dy != 0) && rock( map[cell + dx + dy * w] )) return true;
			}
		}
		return false;
	}

	//rock straight beside it: left, right, above or below
	private static boolean rockBeside( int[] map, int w, int cell ){
		return rock( map[cell - 1] ) || rock( map[cell + 1] ) || rock( map[cell - w] ) || rock( map[cell + w] );
	}

	/** A drop from the roof lands here: in water, or on dry floor beside the rock it runs down. */
	static boolean drip( int[] map, int w, int cell ){
		int t = map[cell];
		if (liquid( t )) return (Terrain.flags[t] & Terrain.PASSABLE) != 0;
		return dry( t ) && rockAround( map, w, cell );
	}

	/** A drain pipe: the sewers' painter puts its wall decorations only on walls over the water. */
	static boolean pipe( int[] map, int w, int cell ){
		return map[cell] == Terrain.WALL_DECO && cell + w < map.length && liquid( map[cell + w] );
	}

	/** Does the pipe at this cell pour, a sheet of water into its pool, rather than trickle?
	 *  One in three does - always the same ones, so a pipe never changes its voice. */
	static boolean pours( int cell ){
		int h = cell * 0x9E3779B1;
		h ^= h >>> 15;
		h *= 0x85EBCA6B;
		h ^= h >>> 13;
		return Math.floorMod( h, 3 ) == 0;
	}

	/** A drain: a pipe, or water against a wall, where air works back up through the grate. */
	static boolean drain( int[] map, int w, int cell ){
		return pipe( map, w, cell ) || (liquid( map[cell] ) && rockBeside( map, w, cell ));
	}

	/** The water's edge: shallow water with dry ground beside it, or dry ground beside water. */
	static boolean shore( int[] map, int w, int cell ){
		int t = map[cell];
		if (liquid( t )) return (Terrain.flags[t] & Terrain.PASSABLE) != 0 && dryAround( map, w, cell );
		return dry( t ) && liquidAround( map, w, cell ) > 0;
	}

	/** Where a body of water laps at its edge: liquid with dry ground beside it, three of its
	 *  neighbours liquid or more - a river's or a lake's bank, never a puddle. */
	static boolean lapping( int[] map, int w, int cell ){
		return liquid( map[cell] ) && dryAround( map, w, cell ) && liquidAround( map, w, cell ) >= 3;
	}

	/** Dry floor at the foot of a wall, where small things run. */
	static boolean wallBase( int[] map, int w, int cell ){
		return dry( map[cell] ) && rockBeside( map, w, cell );
	}

	/** Rock showing over open ground, where a stone works loose or a bat hangs: a wall with floor
	 *  or water below it, or a boulder. */
	static boolean rockFace( int[] map, int w, int cell ){
		int t = map[cell];
		if (t == Terrain.BOULDER) return true;
		return rock( t ) && cell + w < map.length && open( map[cell + w] );
	}

	/**
	 * Where a source's sound comes out into the open: for one set in the rock - a pipe, a torch, a
	 * vent, a seam, a rock face, all on a wall's face over open ground - the open cell before it,
	 * so the wall it is in stands between it and a hero behind that wall; for any other, its cell.
	 */
	static int face( int[] map, int w, int cell ){
		return rock( map[cell] ) && rockFace( map, w, cell ) ? cell + w : cell;
	}

	/** The overworld's ice: frozen water, or open ground frozen over (OverworldLevel.frozenAt). */
	static boolean frost( int t, boolean frozen ){
		return t == Terrain.FROZEN_WATER || (frozen && (Terrain.flags[t] & Terrain.PASSABLE) != 0);
	}

	/** Where an ice seam's meltwater falls: open ground or water right under it. */
	static boolean melt( int[] map, int w, int cell ){
		return open( map[cell] ) && cell - w >= 0 && map[cell - w] == Terrain.WALL_DECO;
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.tiles;

/**
 * Where the homesteads' built terrain is drawn (tools/homestead_art_gen.py paints it at these
 * cells; docs/homesteads-art-world.md says how each piece maps onto the four layers).
 *
 * The walls, windows, doors, bridges, farmland and floors sit in rows 82-107 of BOTH
 * tiles_overworld.png (the surface and the mountain slices) and tiles_caves.png (the cave slices),
 * at the same cells, so one index reads right on whichever sheet a slice draws with. They are cut
 * in the shapes of the dungeon's own wall set, so they take the same stitch numbers its functions
 * compute (DungeonTileSheet.getRaisedWallTile's +1 open right / +2 open left,
 * stitchInternalWallTile's mask, stitchWallOverhangTile's +1 open down-right / +2 open down-left).
 *
 * The standing pieces drawn over the ground - palisades, fences, gates, cobbles, stumps, saplings,
 * rubble - are rows 19-22 of terrain_features.png, the overlay sheet TerrainFeaturesTilemap draws
 * on every level; their cells have clear backgrounds.
 */
public class HomesteadTiles {

	private static final int WIDTH = 16;

	// ------------------------------------------------------------- the tile sheets: walls

	public static final int LOG   = 0;
	public static final int PLANK = 1;
	public static final int STONE = 2;
	public static final int BRICK = 3;
	public static final int MATERIALS = 4;

	public static final int FIRST_ROW = 82;
	private static final int ROWS_PER_MATERIAL = 5;

	private static int base( int material ){
		return (FIRST_ROW + ROWS_PER_MATERIAL * material) * WIDTH;
	}

	/** A wall's face (RAISED_WALL): stitch +1 open right, +2 open left; alt: the second look. */
	public static int face( int material, int stitch, boolean alt ){
		return base( material ) + (alt ? 4 : 0) + (stitch & 3);
	}

	/** The face of a wall over a doorway in a north-south wall (RAISED_WALL_DOOR). */
	public static int faceOverDoor( int material, int stitch ){
		return base( material ) + 8 + (stitch & 3);
	}

	/** A wall's lip on the cell north of it (WALL_OVERHANG): +1 open down-right, +2 open down-left. */
	public static int overhang( int material, int stitch ){
		return base( material ) + 12 + (stitch & 3);
	}

	/** A wall's top with wall in front of it (WALLS_INTERNAL): stitchInternalWallTile's mask. */
	public static int internal( int material, int mask ){
		return base( material ) + WIDTH + (mask & 15);
	}

	/** A window's glazed top, as internal(). */
	public static int windowInternal( int material, int mask ){
		return base( material ) + 2 * WIDTH + (mask & 15);
	}

	/** A window's face, as face(). */
	public static int windowFace( int material, int stitch ){
		return base( material ) + 3 * WIDTH + (stitch & 3);
	}

	/** A window's lip, as overhang(). */
	public static int windowOverhang( int material, int stitch ){
		return base( material ) + 3 * WIDTH + 4 + (stitch & 3);
	}

	/** The flat pictures (flat mode, the terrain pictures in windows): wall, its second look, window. */
	public static int flat( int material, boolean alt ){
		return base( material ) + 3 * WIDTH + (alt ? 9 : 8);
	}

	public static int flatWindow( int material ){
		return base( material ) + 3 * WIDTH + 10;
	}

	/** This wall's lip over a doorway in a north-south wall (DOOR_SIDEWAYS_OVERHANG and its
	 *  _CLOSED twin): the wall below the door's material, +1 / +2 as overhang(). */
	public static int sideDoorOverhang( int material, boolean barred, boolean open, int stitch ){
		return base( material ) + 4 * WIDTH + (barred ? 8 : 0) + (open ? 0 : 4) + (stitch & 3);
	}

	// ------------------------------------------------------------- the tile sheets: doors, the same in any wall

	public static final int DOOR_ROW = FIRST_ROW + ROWS_PER_MATERIAL * MATERIALS;      //102

	public static final int DOOR                    = DOOR_ROW * WIDTH;
	public static final int DOOR_OPEN               = DOOR + 1;
	public static final int BARRED_DOOR             = DOOR + 2;
	public static final int BARRED_DOOR_OPEN        = DOOR + 3;
	public static final int DOOR_OVERHANG           = DOOR + 4;
	public static final int DOOR_OVERHANG_OPEN      = DOOR + 5;
	public static final int BARRED_OVERHANG         = DOOR + 6;
	public static final int BARRED_OVERHANG_OPEN    = DOOR + 7;
	public static final int DOOR_SIDEWAYS           = DOOR + 8;
	public static final int BARRED_SIDEWAYS         = DOOR + 9;
	public static final int DOOR_THRESHOLD          = DOOR + 10;

	// ------------------------------------------------------------- the tile sheets: grounds

	public static final int BRIDGE_EW = (DOOR_ROW + 1) * WIDTH;
	public static final int BRIDGE_NS = (DOOR_ROW + 2) * WIDTH;
	//a bridge's ends: that neighbour along the span is no bridge
	public static final int END_FIRST = 1, END_LAST = 2;
	//a bridge's rails: that side across the span is no bridge (open water)
	public static final int RAIL_FIRST = 1, RAIL_LAST = 2;

	/** A plank bridge running east-west. ends: END_FIRST west, END_LAST east; rails: RAIL_FIRST
	 *  north, RAIL_LAST south. */
	public static int bridgeEW( int ends, int rails ){
		return BRIDGE_EW + (ends & 3) + 4 * (rails & 3);
	}

	/** A plank bridge running north-south. ends: END_FIRST north, END_LAST south; rails:
	 *  RAIL_FIRST west, RAIL_LAST east. */
	public static int bridgeNS( int ends, int rails ){
		return BRIDGE_NS + (ends & 3) + 4 * (rails & 3);
	}

	public static final int FARMLAND     = (DOOR_ROW + 3) * WIDTH;
	public static final int FARMLAND_WET = (DOOR_ROW + 4) * WIDTH;

	/** Tilled furrows by the 4-neighbour mask of farmland around (1 N, 2 E, 4 S, 8 W); wet: watered. */
	public static int farmland( int mask, boolean wet ){
		return (wet ? FARMLAND_WET : FARMLAND) + (mask & 15);
	}

	public static final int FLOOR_PLANK       = (DOOR_ROW + 5) * WIDTH;
	public static final int FLOOR_PLANK_ALT   = FLOOR_PLANK + 1;
	public static final int FLOOR_PLANK_ALT_2 = FLOOR_PLANK + 2;
	public static final int FLOOR_STONE       = FLOOR_PLANK + 3;
	public static final int FLOOR_STONE_ALT   = FLOOR_PLANK + 4;
	public static final int FLOOR_STONE_ALT_2 = FLOOR_PLANK + 5;

	public static final int LAST_ROW = DOOR_ROW + 5;      //107

	// ------------------------------------------------------------- terrain_features.png: the overlays

	public static final int FEATURES_ROW = 19;

	//by the 4-neighbour mask of what each joins (1 N, 2 E, 4 S, 8 W)
	public static final int PALISADE = FEATURES_ROW * WIDTH;
	public static final int FENCE    = PALISADE + WIDTH;

	public static int palisade( int mask ){
		return PALISADE + (mask & 15);
	}

	public static int fence( int mask ){
		return FENCE + (mask & 15);
	}

	private static final int MISC = PALISADE + 2 * WIDTH;
	//a gate in an east-west run of fence, and in a north-south one
	public static final int GATE_EW         = MISC;
	public static final int GATE_EW_OPEN    = MISC + 1;
	public static final int GATE_NS         = MISC + 2;
	public static final int GATE_NS_OPEN    = MISC + 3;
	//cobbles joined on all four sides, three looks
	public static final int PATH_STONE_FULL       = MISC + 4;
	public static final int PATH_STONE_FULL_ALT   = MISC + 5;
	public static final int PATH_STONE_FULL_ALT_2 = MISC + 6;
	public static final int STUMP_OAK       = MISC + 7;
	public static final int STUMP_PINE      = MISC + 8;
	public static final int SAPLING_OAK     = MISC + 9;
	public static final int SAPLING_PINE    = MISC + 10;
	public static final int RUBBLE_WOOD     = MISC + 11;
	public static final int RUBBLE_WOOD_ALT = MISC + 12;
	public static final int RUBBLE_STONE    = MISC + 13;
	public static final int RUBBLE_STONE_ALT = MISC + 14;
	public static final int SAPLING_SHRUB   = MISC + 15;

	public static final int PATH_STONE = PALISADE + 3 * WIDTH;

	/** Cobbles by the mask of path cells beside them; an open side thins out into loose stones. */
	public static int pathStone( int mask ){
		return PATH_STONE + (mask & 15);
	}
}

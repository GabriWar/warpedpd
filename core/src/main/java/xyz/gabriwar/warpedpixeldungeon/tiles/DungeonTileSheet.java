/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith;
import xyz.gabriwar.warpedpixeldungeon.levels.MiningLevel;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.util.Arrays;
import java.util.HashSet;

public class DungeonTileSheet {

	private static final int WIDTH = 16;

	private static int xy(int x, int y){
		x -= 1; y -= 1;
		return x + WIDTH*y;
	}

	//used in cases like map-edge decision making.
	public static final int NULL_TILE       = -1;



	/**********************************************************************
	 * Floor Tiles
	 **********************************************************************/

	private static final int GROUND         =                               xy(1, 1);   //24 slots
	public static final int FLOOR           = GROUND +0;
	public static final int FLOOR_DECO      = GROUND +1;
	public static final int GRASS           = GROUND +2;
	public static final int EMBERS          = GROUND +3;
	public static final int FLOOR_SP        = GROUND +4;

	public static final int MINE_FLOOR_DECO_HEAVY   = GROUND +5;

	public static final int FLOOR_ALT_1     = GROUND +6;
	public static final int FLOOR_DECO_ALT  = GROUND +7;
	public static final int GRASS_ALT       = GROUND +8;
	public static final int EMBERS_ALT      = GROUND +9;
	public static final int FLOOR_SP_ALT    = GROUND +10;

	public static final int MINE_FLOOR_DECO_HEAVY_ALT   = GROUND +11;

	public static final int FLOOR_ALT_2     = GROUND +12;

	public static final int ENTRANCE        = GROUND +16;
	public static final int EXIT            = GROUND +17;
	public static final int WELL            = GROUND +18;
	public static final int EMPTY_WELL      = GROUND +19;
	public static final int PEDESTAL        = GROUND +20;

	public static final int ENTRANCE_SP     = GROUND +22;

	public static final int CHASM           =                               xy(9, 2);   //8 slots
	//chasm stitching visuals...
	public static final int CHASM_FLOOR     = CHASM+1;
	public static final int CHASM_FLOOR_SP  = CHASM+2;
	public static final int CHASM_WALL      = CHASM+3;
	public static final int CHASM_WATER     = CHASM+4;

	//tiles that can stitch with chasms (from above), and which visual represents the stitching
	public static SparseArray<Integer> chasmStitcheable = new SparseArray<>();
	static {
		//floor
		chasmStitcheable.put( Terrain.EMPTY,        CHASM_FLOOR );
		chasmStitcheable.put( Terrain.GRASS,        CHASM_FLOOR );
		chasmStitcheable.put( Terrain.EMBERS,       CHASM_FLOOR );
		chasmStitcheable.put( Terrain.EMPTY_WELL,   CHASM_FLOOR );
		chasmStitcheable.put( Terrain.HIGH_GRASS,      CHASM_FLOOR );
		chasmStitcheable.put( Terrain.FURROWED_GRASS,  CHASM_FLOOR );
		chasmStitcheable.put( Terrain.SOIL_CORNWHEAT,  CHASM_FLOOR );
		chasmStitcheable.put( Terrain.SOIL_GREENWHEAT, CHASM_FLOOR );
		chasmStitcheable.put( Terrain.SOIL_STRAWWHEAT, CHASM_FLOOR );
		chasmStitcheable.put( Terrain.SOIL_WATERWHEAT, CHASM_FLOOR );
		chasmStitcheable.put( Terrain.EMPTY_DECO,   CHASM_FLOOR );
		chasmStitcheable.put( Terrain.CUSTOM_DECO,  CHASM_FLOOR );
		chasmStitcheable.put( Terrain.EMPTY_WELL,   CHASM_FLOOR );
		chasmStitcheable.put( Terrain.WELL,         CHASM_FLOOR );
		chasmStitcheable.put( Terrain.STATUE,       CHASM_FLOOR );
		chasmStitcheable.put( Terrain.REGION_DECO,  CHASM_FLOOR );
		chasmStitcheable.put( Terrain.SECRET_TRAP,  CHASM_FLOOR );
		chasmStitcheable.put( Terrain.INACTIVE_TRAP,CHASM_FLOOR );
		chasmStitcheable.put( Terrain.TRAP,         CHASM_FLOOR );
		chasmStitcheable.put( Terrain.BOOKSHELF,    CHASM_FLOOR );
		chasmStitcheable.put( Terrain.EMPTY_BOOKSHELF, CHASM_FLOOR );
		chasmStitcheable.put( Terrain.BARRICADE,    CHASM_FLOOR );
		chasmStitcheable.put( Terrain.PEDESTAL,     CHASM_FLOOR );
		chasmStitcheable.put( Terrain.CUSTOM_DECO_EMPTY,CHASM_FLOOR );
		chasmStitcheable.put( Terrain.MINE_BOULDER, CHASM_FLOOR );
		chasmStitcheable.put( Terrain.MINE_CRYSTAL, CHASM_FLOOR );

		// Sprouted: sokoban floor terrains
		chasmStitcheable.put( Terrain.FLEECING_TRAP,        CHASM_FLOOR );
		chasmStitcheable.put( Terrain.WOOL_RUG,             CHASM_FLOOR );
		chasmStitcheable.put( Terrain.CHANGE_SHEEP_TRAP,    CHASM_FLOOR );
		chasmStitcheable.put( Terrain.SOKOBAN_SHEEP,        CHASM_FLOOR );
		chasmStitcheable.put( Terrain.CORNER_SOKOBAN_SHEEP, CHASM_FLOOR );
		chasmStitcheable.put( Terrain.SWITCH_SOKOBAN_SHEEP, CHASM_FLOOR );
		chasmStitcheable.put( Terrain.SOKOBAN_ITEM_REVEAL,  CHASM_FLOOR );
		chasmStitcheable.put( Terrain.SOKOBAN_HEAP,         CHASM_FLOOR );
		chasmStitcheable.put( Terrain.BLACK_SOKOBAN_SHEEP,  CHASM_FLOOR );
		chasmStitcheable.put( Terrain.PORT_WELL,            CHASM_FLOOR );
		chasmStitcheable.put( Terrain.SOKOBAN_PORT_SWITCH,  CHASM_FLOOR );

		//special floor
		chasmStitcheable.put( Terrain.EMPTY_SP,     CHASM_FLOOR_SP );
		chasmStitcheable.put( Terrain.STATUE_SP,    CHASM_FLOOR_SP );

		//wall
		chasmStitcheable.put( Terrain.WALL,         CHASM_WALL );
		chasmStitcheable.put( Terrain.DOOR,         CHASM_WALL );
		chasmStitcheable.put( Terrain.OPEN_DOOR,    CHASM_WALL );
		chasmStitcheable.put( Terrain.LOCKED_DOOR,  CHASM_WALL );
		chasmStitcheable.put( Terrain.HERO_LKD_DR,  CHASM_WALL );
		chasmStitcheable.put( Terrain.SECRET_DOOR,  CHASM_WALL );
		chasmStitcheable.put( Terrain.WALL_DECO,    CHASM_WALL );

		//water
		chasmStitcheable.put( Terrain.WATER,        CHASM_WATER );
		//a frozen pool keeps its fall: the waterfall base, with the icefall laid over it
		//by IceFringeTilemap
		chasmStitcheable.put( Terrain.FROZEN_WATER, CHASM_WATER );
	}

	public static int stitchChasmTile(int above){
		//alt region deco has different visuals per region, but most commonly FLOOR_SP
		if (above == Terrain.REGION_DECO_ALT){
			if (Dungeon.depth <= 5)     return CHASM_FLOOR_SP;
			if (Dungeon.depth <= 10)    return CHASM;
			if (Dungeon.depth <= 20)    return CHASM_FLOOR_SP;
			else                        return CHASM_FLOOR;
		}
		return chasmStitcheable.get(above, CHASM);
	}


	/**********************************************************************
	 * Water Tiles
	 **********************************************************************/

	public static final int WATER =                                         xy(1, 3);   //16 slots
	//next 15 slots are all water stitching with ground.

	// Frozen water stitching tiles — row 17 of each tileset (y=256..271).
	// Same 16 variants as WATER but with ice fill instead of transparency.
	// +1 ground above, +2 ground right, +4 ground below, +8 ground left.
	public static final int FROZEN_WATER =                                  xy(1, 17);  //16 slots

	// Warped custom tiles — row 18 of each tileset (y=272..287).
	// Slot 0: shrub (Sprouted art over the region's grass), slot 1: wool rug.
	public static final int WARPED_TILES =                                  xy(1, 18);  //16 slots
	public static final int SHRUB_TILE    = WARPED_TILES+0;
	public static final int WOOL_RUG_TILE = WARPED_TILES+1;
	//tilled farm soil (safe zone visual for FURROWED_GRASS), 3 variants
	public static final int TILLED_SOIL       = WARPED_TILES+2;
	public static final int TILLED_SOIL_ALT   = WARPED_TILES+3;
	public static final int TILLED_SOIL_RARE  = WARPED_TILES+4;
	public static final int SNOW_TILE         = WARPED_TILES+5;
	public static final int DIRT_PATH_TILE    = WARPED_TILES+6;
	public static final int WAYSTONE_TILE     = WARPED_TILES+7;
	public static final int BRIDGE_TILE       = WARPED_TILES+8;
	//a surface village's field by the season (tools/field_tiles_gen.py, overworld tiles only):
	//shoots in spring, green wheat in summer, ripe gold in autumn, snow in the furrows in winter
	public static final int FIELD_SPRING      = WARPED_TILES+9;
	public static final int FIELD_SPRING_ALT  = WARPED_TILES+10;
	public static final int FIELD_SUMMER      = WARPED_TILES+11;
	public static final int FIELD_SUMMER_ALT  = WARPED_TILES+12;
	public static final int FIELD_AUTUMN      = WARPED_TILES+13;
	public static final int FIELD_AUTUMN_ALT  = WARPED_TILES+14;
	public static final int FIELD_WINTER      = WARPED_TILES+15;

	/** Is the FURROWED_GRASS on this cell of the level farm ground rather than the warden's
	 *  furrowed grass - the safe zone's tilled plots, a surface village's fields? Then none of
	 *  the grass's raised tuft, its overhang on the wall above or its stage dressing is drawn
	 *  over it: every one of the four layers that draw it asks here. */
	public static boolean tilledSoil( int pos ){
		xyz.gabriwar.warpedpixeldungeon.levels.Level l = Dungeon.level;
		return l instanceof xyz.gabriwar.warpedpixeldungeon.levels.SafeLevel
				|| (l instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
					&& ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) l).fieldAt( pos ));
	}

	/** A surface village's field in a season (OverworldLevel.fieldSeason). */
	public static int fieldTile( xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar.Season season, int pos ){
		switch (season){
			case SPRING: return getVisualWithAlts( FIELD_SPRING, pos );
			case SUMMER: return getVisualWithAlts( FIELD_SUMMER, pos );
			case AUTUMN: return getVisualWithAlts( FIELD_AUTUMN, pos );
			default:     return FIELD_WINTER;
		}
	}

	//These tiles can stitch with water
	public static HashSet<Integer> waterStitcheable = new HashSet<>(Arrays.asList(
			Terrain.EMPTY, Terrain.GRASS, Terrain.EMPTY_WELL,
			Terrain.ENTRANCE, Terrain.EXIT, Terrain.EMBERS,
			Terrain.BARRICADE, Terrain.HIGH_GRASS, Terrain.FURROWED_GRASS, Terrain.SECRET_TRAP,
			Terrain.SOIL_CORNWHEAT, Terrain.SOIL_GREENWHEAT, Terrain.SOIL_STRAWWHEAT, Terrain.SOIL_WATERWHEAT,
			Terrain.TRAP, Terrain.INACTIVE_TRAP, Terrain.EMPTY_DECO,
			Terrain.CUSTOM_DECO, Terrain.WELL, Terrain.STATUE, Terrain.REGION_DECO, Terrain.ALCHEMY,
			Terrain.CUSTOM_DECO_EMPTY, Terrain.MINE_CRYSTAL, Terrain.MINE_BOULDER,
			Terrain.DOOR, Terrain.OPEN_DOOR, Terrain.LOCKED_DOOR, Terrain.HERO_LKD_DR, Terrain.CRYSTAL_DOOR,
			// Sprouted: sokoban floor terrains
			Terrain.FLEECING_TRAP, Terrain.WOOL_RUG, Terrain.CHANGE_SHEEP_TRAP,
			Terrain.SOKOBAN_SHEEP, Terrain.CORNER_SOKOBAN_SHEEP, Terrain.SWITCH_SOKOBAN_SHEEP,
			Terrain.SOKOBAN_ITEM_REVEAL, Terrain.SOKOBAN_HEAP, Terrain.BLACK_SOKOBAN_SHEEP,
			Terrain.PORT_WELL, Terrain.SOKOBAN_PORT_SWITCH
	));

	//the surface's own grounds: snow, sand, roads and whatever stands on open
	//ground there. water and ice lying against them get a shore, not a square edge
	public static HashSet<Integer> OVERWORLD_BANKS = new HashSet<>(Arrays.asList(
			Terrain.SNOW, Terrain.EMPTY_SP, Terrain.DIRT_PATH, Terrain.SHRUB, Terrain.BOULDER,
			Terrain.TREE_PINE, Terrain.TREE_OAK, Terrain.FLOWER_PATCH, Terrain.MUSHROOM_PATCH,
			Terrain.SIGN
	));

	public static boolean waterStitcheable(int tile){
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
				&& OVERWORLD_BANKS.contains(tile)) return true;
		// These ported regions use ground-backed special floors too. Without
		// treating them as banks, a pool inside a special room is a hard square.
		if ((tile == Terrain.EMPTY_SP || tile == Terrain.STATUE_SP) && Dungeon.level != null) {
			String texture = Dungeon.level.tilesTex();
			if (xyz.gabriwar.warpedpixeldungeon.Assets.Environment.TILES_FROZEN.equals(texture)
					|| xyz.gabriwar.warpedpixeldungeon.Assets.Environment.TILES_SPIDERNEST.equals(texture)) return true;
		}
		//alt region deco has different visuals per region, is stitcheable in demon halls
		if (tile == Terrain.REGION_DECO_ALT){
			if (Dungeon.depth <= 20)    return false;
			else                        return true;
		}
		return waterStitcheable.contains(tile);
	}

	//+1 for ground above, +2 for ground right, +4 for ground below, +8 for ground left.
	public static int stitchWaterTile(int top, int right, int bottom, int left){
		int result = WATER;
		if (waterStitcheable(top))      result += 1;
		if (waterStitcheable(right))    result += 2;
		if (waterStitcheable(bottom))   result += 4;
		if (waterStitcheable(left))     result += 8;
		return result;
	}

	//the surface's shores in the grounds they lie against, side by side (tiles_overworld rows
	//18-57, tools/water_shore_gen.py): row 2's shapes with each bank's lip in that bank's own
	//ground, so a pond is not framed in dark green on a road square, a beach or the snow, nor
	//green on its sandy side where grass lies on another. for side mask m the tiles start at
	//WATER_SHORE + SHORE_OFFSET[m], and each bank side (top, right, bottom, left order) is one
	//base-4 digit of the ground: 0 grass, 1 dirt, 2 sand, 3 snow
	public static final int WATER_SHORE = xy(1, 19);   //625 slots
	//open water seen from high above, for the view down from a mountain (tools/below_view_gen.py:
	//a still, darker crop of the surface's water texture; WindowGenerator.belowTile)
	public static final int BELOW_WATER = xy(1, 59);
	//the shade a drop's rims cast into it, by the mask of its eight neighbours that stand
	//higher (1 N, 2 NE, 4 E, 8 SE, 16 S, 32 SW, 64 W, 128 NW; 1..255): the 16 rows under the water
	public static int belowShade( int mask ){
		return xy(1, 60) + mask;
	}

	//the overworld's rock seen as the land it is (tools/rock_top_gen.py): an earthen scarp's face
	//and lip, the rims of the rock's tops, and the lips over them showing their ground
	private static final int EARTH_FACES     = xy(1, 76);  //RAISED_WALL +0..3, then RAISED_WALL_ALT +0..3
	private static final int EARTH_OVERHANGS = xy(9, 76);  //WALL_OVERHANG +0..3
	private static final int ROCK_RIMS       = xy(1, 77);  //stone, by the internal stitch's mask; earthen the row under
	private static final int ROCK_TOP_LIPS   = xy(1, 79);  //per ground 8: stone lips +0..3, earthen +4..7
	private static final int[] ROCK_TOP_GROUNDS = { GRASS, FLOOR, FLOOR_SP, SNOW_TILE, DIRT_PATH_TILE, FROZEN_WATER, BELOW_WATER };

	/** A stone scarp's face or lip as the same piece in earth; any other visual as it is. */
	public static int earthen( int visual ){
		if (visual >= RAISED_WALL && visual < RAISED_WALL + 4)         return EARTH_FACES + (visual - RAISED_WALL);
		if (visual >= RAISED_WALL_ALT && visual < RAISED_WALL_ALT + 4) return EARTH_FACES + 4 + (visual - RAISED_WALL_ALT);
		if (visual >= WALL_OVERHANG && visual < WALL_OVERHANG + 4)     return EARTH_OVERHANGS + (visual - WALL_OVERHANG);
		return visual;
	}

	//stitchInternalWallTile's own bits
	public static final int RIM_EAST = 1, RIM_EAST_BELOW = 2, RIM_WEST_BELOW = 4, RIM_WEST = 8;

	/** The rim of a rock top: the internal wall's own stitch on a clear roof, so the top's own
	 *  ground shows through. */
	public static int rockRim( int mask, boolean earth ){
		return ROCK_RIMS + (earth ? 16 : 0) + (mask & 15);
	}

	/** A wall's lip (stitchWallOverhangTile) over rock whose top shows this ground, earthen or
	 *  not; any other visual as it is. */
	public static int rockTopLip( int lip, int topGround, boolean earth ){
		int m = lip - WALL_OVERHANG;
		if (m < 0 || m > 3) return lip;
		for (int i = 0; i < ROCK_TOP_GROUNDS.length; i++){
			if (ROCK_TOP_GROUNDS[i] == topGround) return ROCK_TOP_LIPS + i * 8 + (earth ? 4 : 0) + m;
		}
		return earth ? earthen( lip ) : lip;
	}


	private static final int[] SHORE_OFFSET = new int[16];
	static {
		int total = 0;
		for (int m = 0; m < 16; m++){
			SHORE_OFFSET[m] = total;
			total += 1 << (2 * Integer.bitCount(m));
		}
	}

	//the shore ground a bank terrain shows: the surface's bare grounds, else its grass
	private static int shoreGround(int t){
		return t == Terrain.DIRT_PATH ? 1 : t == Terrain.EMPTY_SP ? 2 : t == Terrain.SNOW ? 3 : 0;
	}

	/** A surface water tile: the shape stitchWaterTile gives, each bank in its own ground. */
	public static int stitchOverworldWater(int top, int right, int bottom, int left){
		int mask = stitchWaterTile(top, right, bottom, left) - WATER;
		if (mask == 0) return WATER;
		int[] sides = { top, right, bottom, left };
		int combo = 0, digit = 1;
		for (int i = 0; i < 4; i++){
			if ((mask & (1 << i)) == 0) continue;
			combo += shoreGround(sides[i]) * digit;
			digit *= 4;
		}
		return WATER_SHORE + SHORE_OFFSET[mask] + combo;
	}

	// Frozen water uses the same stitching logic and stitcheable set as regular water,
	// but maps to the ice tile row (row 17) instead of the water tile row (row 3).
	public static int stitchFrozenWaterTile(int top, int right, int bottom, int left){
		int result = FROZEN_WATER;
		if (waterStitcheable(top))      result += 1;
		if (waterStitcheable(right))    result += 2;
		if (waterStitcheable(bottom))   result += 4;
		if (waterStitcheable(left))     result += 8;
		return result;
	}

	public static boolean floorTile(int tile){
		return tile == Terrain.WATER || tile == Terrain.FROZEN_WATER
				|| directVisuals.get(tile, CHASM) < CHASM;
	}


	/**********************************************************************
	 Flat Tiles
	 **********************************************************************/

	private static final int FLAT_WALLS         =                           xy(1, 4);   //16 slots
	public static final int FLAT_WALL           = FLAT_WALLS+0;
	public static final int FLAT_WALL_DECO      = FLAT_WALLS+1;
	public static final int FLAT_BOOKSHELF      = FLAT_WALLS+2;

	public static final int FLAT_WALL_ALT       = FLAT_WALLS+4;
	public static final int FLAT_WALL_DECO_ALT  = FLAT_WALLS+5;
	public static final int FLAT_BOOKSHELF_ALT  = FLAT_WALLS+6;

	public static final int FLAT_DOOR           = FLAT_WALLS+8;
	public static final int FLAT_DOOR_OPEN      = FLAT_WALLS+9;
	public static final int FLAT_DOOR_LOCKED    = FLAT_WALLS+10;
	public static final int FLAT_DOOR_CRYSTAL   = FLAT_WALLS+11;
	public static final int UNLOCKED_EXIT       = FLAT_WALLS+12;
	public static final int LOCKED_EXIT         = FLAT_WALLS+13;

	public static final int FLAT_OTHER          =                           xy(1, 5);   //16 slots
	public static final int FLAT_ALCHEMY_POT    = FLAT_OTHER+0;
	public static final int FLAT_BARRICADE      = FLAT_OTHER+1;
	public static final int FLAT_HIGH_GRASS     = FLAT_OTHER+2;
	public static final int FLAT_FURROWED_GRASS = FLAT_OTHER+3;

	public static final int FLAT_HIGH_GRASS_ALT = FLAT_OTHER+5;
	public static final int FLAT_FURROWED_ALT   = FLAT_OTHER+6;

	public static final int FLAT_STATUE         = FLAT_OTHER+8;
	public static final int FLAT_STATUE_SP      = FLAT_OTHER+9;
	public static final int FLAT_REGION_DECO    = FLAT_OTHER+10;
	public static final int FLAT_REGION_DECO_ALT= FLAT_OTHER+11;

	public static final int FLAT_MINE_CRYSTAL         = FLAT_OTHER+12;
	public static final int FLAT_MINE_CRYSTAL_ALT     = FLAT_OTHER+13;
	public static final int FLAT_MINE_CRYSTAL_ALT_2   = FLAT_OTHER+14;
	public static final int FLAT_MINE_BOULDER         = FLAT_OTHER+12;
	public static final int FLAT_MINE_BOULDER_ALT     = FLAT_OTHER+13;
	public static final int FLAT_MINE_BOULDER_ALT_2   = FLAT_OTHER+14;

	/**********************************************************************
	 * Raised Tiles, Lower Layer
	 **********************************************************************/

	private static final int RAISED_WALLS               =                   xy(1, 6);   //32 slots
	//+1 for open to the right, +2 for open to the left
	public static final int RAISED_WALL                 = RAISED_WALLS+0;
	public static final int RAISED_WALL_DECO            = RAISED_WALLS+4;
	//wall that appears behind a top/bottom doorway
	public static final int RAISED_WALL_DOOR            = RAISED_WALLS+8;
	public static final int RAISED_WALL_BOOKSHELF       = RAISED_WALLS+12;

	public static final int RAISED_WALL_ALT             = RAISED_WALLS+16;
	public static final int RAISED_WALL_DECO_ALT        = RAISED_WALLS+20;
	//a shelf with the books taken out: the same boards and frame, an empty back
	public static final int RAISED_WALL_BOOKSHELF_EMPTY = RAISED_WALLS+24;
	public static final int RAISED_WALL_BOOKSHELF_ALT   = RAISED_WALLS+28;

	//we use an array instead of a collection because the small element count
	// makes array traversal much faster than something like HashSet.contains.

	//These tiles count as wall for the purposes of wall stitching
	private static int[] wallStitcheable = new int[]{
			Terrain.WALL, Terrain.WALL_DECO, Terrain.SECRET_DOOR,
			Terrain.LOCKED_EXIT, Terrain.UNLOCKED_EXIT, Terrain.BOOKSHELF, Terrain.EMPTY_BOOKSHELF, NULL_TILE
	};

	public static boolean wallStitcheable(int tile){
		for (int i : wallStitcheable)
			if (tile == i)
				return true;
		return false;
	}

	public static int getRaisedWallTile(int tile, int pos, int right, int below, int left){
		int result;
		
		if (below == -1 || wallStitcheable(below))                      return -1;
		else if (doorTile(below))                                       result = RAISED_WALL_DOOR;
		else if (tile == Terrain.WALL || tile == Terrain.SECRET_DOOR)   result = RAISED_WALL;
		else if (tile == Terrain.WALL_DECO)                             result = RAISED_WALL_DECO;
		else if (tile == Terrain.BOOKSHELF)                             result = RAISED_WALL_BOOKSHELF;
		else if (tile == Terrain.EMPTY_BOOKSHELF)                       result = RAISED_WALL_BOOKSHELF_EMPTY;
		else                                                            return -1;

		result = getVisualWithAlts(result, pos);

		if (!wallStitcheable(right))   result += 1;
		if (!wallStitcheable(left))    result += 2;
		return result;
	}

	private static final int RAISED_DOORS           =                       xy(1, 8);  //8 slots
	public static final int RAISED_DOOR             = RAISED_DOORS+0;
	public static final int RAISED_DOOR_OPEN        = RAISED_DOORS+1;
	public static final int RAISED_DOOR_LOCKED      = RAISED_DOORS+2;
	public static final int RAISED_DOOR_CRYSTAL     = RAISED_DOORS+3;
	//floor tile that appears on a top/bottom doorway
	public static final int RAISED_DOOR_SIDEWAYS    = RAISED_DOORS+4;


	public static int getRaisedDoorTile(int tile, int below){
		if (wallStitcheable(below))             return RAISED_DOOR_SIDEWAYS;
		else if (tile == Terrain.DOOR)          return DungeonTileSheet.RAISED_DOOR;
		else if (tile == Terrain.OPEN_DOOR)     return DungeonTileSheet.RAISED_DOOR_OPEN;
		else if (tile == Terrain.LOCKED_DOOR)   return DungeonTileSheet.RAISED_DOOR_LOCKED;
		else if (tile == Terrain.HERO_LKD_DR)   return DungeonTileSheet.RAISED_DOOR_LOCKED;
		else if (tile == Terrain.CRYSTAL_DOOR)  return DungeonTileSheet.RAISED_DOOR_CRYSTAL;
		else return -1;
	}

	private static int[] doorTiles = new int[]{
			Terrain.DOOR, Terrain.LOCKED_DOOR, Terrain.HERO_LKD_DR, Terrain.CRYSTAL_DOOR, Terrain.OPEN_DOOR
	};

	public static boolean doorTile(int tile){
		for (int i : doorTiles)
			if (tile == i)
				return true;
		return false;
	}

	private static final int RAISED_OTHER           =                       xy(9, 8);  //24 slots
	public static final int RAISED_ALCHEMY_POT      = RAISED_OTHER+0;
	public static final int RAISED_BARRICADE        = RAISED_OTHER+1;
	public static final int RAISED_HIGH_GRASS       = RAISED_OTHER+2;
	public static final int RAISED_FURROWED_GRASS   = RAISED_OTHER+3;

	public static final int RAISED_HIGH_GRASS_ALT   = RAISED_OTHER+5;
	public static final int RAISED_FURROWED_ALT     = RAISED_OTHER+6;

	public static final int RAISED_STATUE           = RAISED_OTHER+8;
	public static final int RAISED_STATUE_SP        = RAISED_OTHER+9;
	public static final int RAISED_REGION_DECO      = RAISED_OTHER+10;
	public static final int RAISED_REGION_DECO_ALT  = RAISED_OTHER+11;

	public static final int RAISED_MINE_CRYSTAL_BLUE_1  = RAISED_OTHER+12; //blue1 is the default
	public static final int RAISED_MINE_CRYSTAL_BLUE_2  = RAISED_OTHER+13;
	public static final int RAISED_MINE_CRYSTAL_GREEN_1 = RAISED_OTHER+14;
	public static final int RAISED_MINE_CRYSTAL_GREEN_2 = RAISED_OTHER+15;
	public static final int RAISED_MINE_CRYSTAL_RED_1   = RAISED_OTHER+16;
	public static final int RAISED_MINE_CRYSTAL_RED_2   = RAISED_OTHER+17;

	public static final int RAISED_MINE_BOULDER     = RAISED_OTHER+12;
	public static final int RAISED_MINE_BOULDER_ALT = RAISED_OTHER+13;
	public static final int RAISED_MINE_BOULDER_ALT_2=RAISED_OTHER+14;

	public static final int SOKOBAN_ITEM_REVEAL_TILE = RAISED_OTHER+15;
	public static final int SOKOBAN_PORT_SWITCH_TILE  = RAISED_OTHER+16;


	/**********************************************************************
	 * Raised Tiles, Upper Layer
	 **********************************************************************/

	//+1 for open right, +2 for open right-below, +4 for open left-below, +8 for open left.
	public static final int WALLS_INTERNAL              =                   xy(1, 10);  //48 slots
	private static final int WALL_INTERNAL              = WALLS_INTERNAL+0;
	private static final int WALL_INTERNAL_DECO         = WALLS_INTERNAL+16;
	private static final int WALL_INTERNAL_WOODEN       = WALLS_INTERNAL+32;

	public static int stitchInternalWallTile(int tile, int right, int rightBelow, int below, int leftBelow, int left){
		int result;

		if (tile == Terrain.BOOKSHELF || below == Terrain.BOOKSHELF
				|| tile == Terrain.EMPTY_BOOKSHELF || below == Terrain.EMPTY_BOOKSHELF) result = WALL_INTERNAL_WOODEN;
		//TODO currently this line on triggers on mining floors, do we want to make it universal?
		else if (Dungeon.branch == 1 && tile == Terrain.WALL_DECO)          result = WALL_INTERNAL_DECO;
		else                                                                result = WALL_INTERNAL;

		if (!wallStitcheable(right))        result += 1;
		if (!wallStitcheable(rightBelow))   result += 2;
		if (!wallStitcheable(leftBelow))    result += 4;
		if (!wallStitcheable(left))         result += 8;
		return result;
	}

	//+1 for open to the down-right, +2 for open to the down-left
	private static final int WALLS_OVERHANG             =                   xy(1, 13);  //32 slots
	public static final int WALL_OVERHANG                   = WALLS_OVERHANG+0;
	public static final int WALL_OVERHANG_DECO              = WALLS_OVERHANG+4;
	public static final int WALL_OVERHANG_WOODEN            = WALLS_OVERHANG+8;
	public static final int DOOR_SIDEWAYS_OVERHANG          = WALLS_OVERHANG+16;
	public static final int DOOR_SIDEWAYS_OVERHANG_CLOSED   = WALLS_OVERHANG+20;
	public static final int DOOR_SIDEWAYS_OVERHANG_LOCKED   = WALLS_OVERHANG+24;
	public static final int DOOR_SIDEWAYS_OVERHANG_CRYSTAL  = WALLS_OVERHANG+28;


	public static int stitchWallOverhangTile(int tile, int rightBelow, int below, int leftBelow){
		int visual;
		if (tile == Terrain.OPEN_DOOR)                              visual = DOOR_SIDEWAYS_OVERHANG;
		else if (tile == Terrain.DOOR)                              visual = DOOR_SIDEWAYS_OVERHANG_CLOSED;
		else if (tile == Terrain.LOCKED_DOOR)                       visual = DOOR_SIDEWAYS_OVERHANG_LOCKED;
		else if (tile == Terrain.HERO_LKD_DR)                       visual = DOOR_SIDEWAYS_OVERHANG_LOCKED;
		else if (tile == Terrain.CRYSTAL_DOOR)                      visual = DOOR_SIDEWAYS_OVERHANG_CRYSTAL;
		//TODO currently this line on triggers on mining floors, do we want to make it universal?
		else if (Dungeon.branch == 1 && below == Terrain.WALL_DECO) visual = WALL_OVERHANG_DECO;
		else if (below == Terrain.BOOKSHELF || below == Terrain.EMPTY_BOOKSHELF) visual = WALL_OVERHANG_WOODEN;
		else                                                        visual = WALL_OVERHANG;

		if (!wallStitcheable(rightBelow))  visual += 1;
		if (!wallStitcheable(leftBelow))   visual += 2;

		return visual;
	}

	public static final int DOOR_OVERHANG               =                   xy(1, 15);  //8 slots
	public static final int DOOR_OVERHANG_OPEN          = DOOR_OVERHANG+1;
	public static final int DOOR_OVERHANG_CRYSTAL       = DOOR_OVERHANG+2;
	public static final int DOOR_SIDEWAYS               = DOOR_OVERHANG+3;
	public static final int DOOR_SIDEWAYS_LOCKED        = DOOR_OVERHANG+4;
	public static final int DOOR_SIDEWAYS_CRYSTAL       = DOOR_OVERHANG+5;
	//exit visuals are rendered flat atm, so they actually underhang
	public static final int EXIT_UNDERHANG              = DOOR_OVERHANG+6;


	private static final int OTHER_OVERHANG             =                   xy(9, 15);  //24 slots
	public static final int ALCHEMY_POT_OVERHANG        = OTHER_OVERHANG+0;
	public static final int BARRICADE_OVERHANG          = OTHER_OVERHANG+1;
	public static final int HIGH_GRASS_OVERHANG         = OTHER_OVERHANG+2;
	public static final int FURROWED_OVERHANG           = OTHER_OVERHANG+3;

	public static final int HIGH_GRASS_OVERHANG_ALT     = OTHER_OVERHANG+5;
	public static final int FURROWED_OVERHANG_ALT       = OTHER_OVERHANG+6;

	public static final int STATUE_OVERHANG             = OTHER_OVERHANG+8;
	public static final int STATUE_SP_OVERHANG          = OTHER_OVERHANG+9;
	public static final int REGION_DECO_OVERHANG        = OTHER_OVERHANG+10;
	public static final int REGION_DECO_ALT_OVERHANG    = OTHER_OVERHANG+11;

	public static final int MINE_CRYSTAL_OVERHANG_BLUE  = OTHER_OVERHANG+12;
	public static final int MINE_CRYSTAL_OVERHANG_GREEN = OTHER_OVERHANG+13;
	public static final int MINE_CRYSTAL_OVERHANG_RED   = OTHER_OVERHANG+14;
	public static final int MINE_BOULDER_OVERHANG       = OTHER_OVERHANG+12;
	public static final int MINE_BOULDER_OVERHANG_ALT   = OTHER_OVERHANG+13;
	public static final int MINE_BOULDER_OVERHANG_ALT_2 = OTHER_OVERHANG+14;

	/**********************************************************************
	 * Logic for the selection of tile visuals
	 **********************************************************************/

	//These visuals always directly represent a game tile with no stitching required
	public static SparseArray<Integer> directVisuals = new SparseArray<>();
	static {
		directVisuals.put(Terrain.EMPTY,            FLOOR);
		directVisuals.put(Terrain.GRASS,            GRASS);
		directVisuals.put(Terrain.EMPTY_WELL,       EMPTY_WELL);
		directVisuals.put(Terrain.ENTRANCE,         ENTRANCE);
		directVisuals.put(Terrain.EXIT,             EXIT);
		directVisuals.put(Terrain.EMBERS,           EMBERS);
		directVisuals.put(Terrain.PEDESTAL,         PEDESTAL);
		directVisuals.put(Terrain.EMPTY_SP,         FLOOR_SP);
		directVisuals.put(Terrain.ENTRANCE_SP,      ENTRANCE_SP);

		directVisuals.put(Terrain.SECRET_TRAP,      directVisuals.get(Terrain.EMPTY));
		directVisuals.put(Terrain.TRAP,             directVisuals.get(Terrain.EMPTY));
		directVisuals.put(Terrain.INACTIVE_TRAP,    directVisuals.get(Terrain.EMPTY));
		directVisuals.put(Terrain.CUSTOM_DECO,      directVisuals.get(Terrain.EMPTY));
		directVisuals.put(Terrain.CUSTOM_DECO_EMPTY,directVisuals.get(Terrain.EMPTY));
		directVisuals.put(Terrain.CUSTOM_DECO_WTR,  WATER);

		directVisuals.put(Terrain.EMPTY_DECO,       FLOOR_DECO);
		directVisuals.put(Terrain.EMPTY_DECO,       FLOOR_DECO);
		directVisuals.put(Terrain.LOCKED_EXIT,      LOCKED_EXIT);
		directVisuals.put(Terrain.UNLOCKED_EXIT,    UNLOCKED_EXIT);
		directVisuals.put(Terrain.WELL,             WELL);
		// FROZEN_WATER uses stitched tiles (row 17), handled in DungeonTerrainTilemap

		// Sprouted: Sokoban puzzle terrain — render as plain floor
		directVisuals.put(Terrain.SOKOBAN_SHEEP,        directVisuals.get(Terrain.EMPTY));
		directVisuals.put(Terrain.CORNER_SOKOBAN_SHEEP, directVisuals.get(Terrain.EMPTY));
		directVisuals.put(Terrain.SWITCH_SOKOBAN_SHEEP, directVisuals.get(Terrain.EMPTY));
		directVisuals.put(Terrain.SOKOBAN_ITEM_REVEAL,  SOKOBAN_ITEM_REVEAL_TILE);
		directVisuals.put(Terrain.SOKOBAN_HEAP,         directVisuals.get(Terrain.EMPTY));
		directVisuals.put(Terrain.BLACK_SOKOBAN_SHEEP,  directVisuals.get(Terrain.EMPTY));
		directVisuals.put(Terrain.SOKOBAN_PORT_SWITCH,  SOKOBAN_PORT_SWITCH_TILE);
		directVisuals.put(Terrain.FLEECING_TRAP,        directVisuals.get(Terrain.EMPTY));
		directVisuals.put(Terrain.WOOL_RUG,             WOOL_RUG_TILE);
		//the shrub's ground layer is plain grass; the bush itself is drawn by
		//TerrainFeaturesTilemap as a transparent overlay (cell 15) on top
		directVisuals.put(Terrain.SHRUB,                GRASS);
		directVisuals.put(Terrain.SNOW,                 SNOW_TILE);
		//deco terrains: ground layer here, the artwork is a features overlay
		directVisuals.put(Terrain.TREE_PINE,            GRASS);
		directVisuals.put(Terrain.TREE_OAK,             GRASS);
		directVisuals.put(Terrain.BOULDER,              FLOOR);
		directVisuals.put(Terrain.FLOWER_PATCH,         GRASS);
		directVisuals.put(Terrain.MUSHROOM_PATCH,       GRASS);
		directVisuals.put(Terrain.DIRT_PATH,            DIRT_PATH_TILE);
		directVisuals.put(Terrain.BRIDGE,               BRIDGE_TILE);
		//the signpost art is a dressing overlay; the ground under it is grass/snow
		directVisuals.put(Terrain.SIGN,                 GRASS);
		//solid ground under the town's painted props: snow, so art with
		//transparent pixels never lets the water plane bleed through
		directVisuals.put(Terrain.TOWN_SOLID,           SNOW_TILE);
		directVisuals.put(Terrain.CHANGE_SHEEP_TRAP,    directVisuals.get(Terrain.EMPTY));
		directVisuals.put(Terrain.PORT_WELL,            EMPTY_WELL);

	}

	//These visuals directly represent game tiles (no stitching) when terrain is being shown as flat
	public static SparseArray<Integer> directFlatVisuals = new SparseArray<>();
	static {
		directFlatVisuals.put(Terrain.WALL,             FLAT_WALL);
		directFlatVisuals.put(Terrain.DOOR,             FLAT_DOOR);
		directFlatVisuals.put(Terrain.OPEN_DOOR,        FLAT_DOOR_OPEN);
		directFlatVisuals.put(Terrain.LOCKED_DOOR,      FLAT_DOOR_LOCKED);
		directFlatVisuals.put(Terrain.HERO_LKD_DR,      FLAT_DOOR_LOCKED);
		directFlatVisuals.put(Terrain.CRYSTAL_DOOR,     FLAT_DOOR_CRYSTAL);
		directFlatVisuals.put(Terrain.WALL_DECO,        FLAT_WALL_DECO);
		directFlatVisuals.put(Terrain.BOOKSHELF,        FLAT_BOOKSHELF);
		directFlatVisuals.put(Terrain.EMPTY_BOOKSHELF,  FLAT_BOOKSHELF);
		directFlatVisuals.put(Terrain.ALCHEMY,          FLAT_ALCHEMY_POT);
		directFlatVisuals.put(Terrain.BARRICADE,        FLAT_BARRICADE);
		directFlatVisuals.put(Terrain.HIGH_GRASS,        FLAT_HIGH_GRASS);
		directFlatVisuals.put(Terrain.FURROWED_GRASS,   FLAT_FURROWED_GRASS);
		// Overgrown: soil terrain renders as HIGH_GRASS
		directFlatVisuals.put(Terrain.SOIL_CORNWHEAT,   FLAT_HIGH_GRASS);
		directFlatVisuals.put(Terrain.SOIL_GREENWHEAT,  FLAT_HIGH_GRASS);
		directFlatVisuals.put(Terrain.SOIL_STRAWWHEAT,  FLAT_HIGH_GRASS);
		directFlatVisuals.put(Terrain.SOIL_WATERWHEAT,  FLAT_HIGH_GRASS);

		directFlatVisuals.put(Terrain.STATUE,           FLAT_STATUE);
		directFlatVisuals.put(Terrain.STATUE_SP,        FLAT_STATUE_SP);
		directFlatVisuals.put(Terrain.REGION_DECO,      FLAT_REGION_DECO);
		directFlatVisuals.put(Terrain.REGION_DECO_ALT,  FLAT_REGION_DECO_ALT);

		directFlatVisuals.put(Terrain.MINE_CRYSTAL,     FLAT_MINE_CRYSTAL);
		directFlatVisuals.put(Terrain.MINE_BOULDER,     FLAT_MINE_BOULDER);

		directFlatVisuals.put(Terrain.SECRET_DOOR,      directFlatVisuals.get(Terrain.WALL));
		// FROZEN_WATER uses stitched tiles (row 17), handled in DungeonTerrainTilemap
	}


	/**********************************************************************
	 * Logic for the selection of alternate tile visuals
	 **********************************************************************/

	public static byte[] tileVariance;

	public static void setupVariance(int size, long seed){
		Random.pushGenerator( seed );

			tileVariance = new byte[size];
			for (int i = 0; i < tileVariance.length; i++) {
				tileVariance[i] = (byte) Random.Int(100);
			}

		Random.popGenerator();
		updateAltVariants();
	}

	private static class tileAlt {
		float[] chances;
		int[] alts;
		public tileAlt(float[] chances, int... alts){
			this.chances = chances;
			this.alts = alts;
		}
	}

	private static SparseArray<tileAlt> tileAltVisuals = new SparseArray<>();
	static {
		tileAltVisuals.put(FLOOR,           new tileAlt(new float[]{52.5f, 5f}, FLOOR_ALT_1, FLOOR_ALT_2));
		tileAltVisuals.put(GRASS,           new tileAlt(new float[]{50f}, GRASS_ALT));
		//Warped: tilled farm soil, 47.5% common alt / 5% rare alt like the floor
		tileAltVisuals.put(TILLED_SOIL,     new tileAlt(new float[]{52.5f, 5f}, TILLED_SOIL_ALT, TILLED_SOIL_RARE));
		tileAltVisuals.put(FIELD_SPRING,    new tileAlt(new float[]{50f}, FIELD_SPRING_ALT));
		tileAltVisuals.put(FIELD_SUMMER,    new tileAlt(new float[]{50f}, FIELD_SUMMER_ALT));
		tileAltVisuals.put(FIELD_AUTUMN,    new tileAlt(new float[]{50f}, FIELD_AUTUMN_ALT));
		tileAltVisuals.put(FLAT_WALL,       new tileAlt(new float[]{50f}, FLAT_WALL_ALT));
		tileAltVisuals.put(EMBERS,          new tileAlt(new float[]{50f}, EMBERS_ALT));
		tileAltVisuals.put(FLAT_WALL_DECO,  new tileAlt(new float[]{50f}, FLAT_WALL_DECO_ALT));
		tileAltVisuals.put(FLOOR_SP,        new tileAlt(new float[]{50f}, FLOOR_SP_ALT));
		tileAltVisuals.put(FLOOR_DECO,      new tileAlt(new float[]{50f}, FLOOR_DECO_ALT));
		tileAltVisuals.put(GRASS,           new tileAlt(new float[]{50f}, GRASS_ALT));

		tileAltVisuals.put(FLAT_BOOKSHELF,      new tileAlt(new float[]{50f}, FLAT_BOOKSHELF_ALT));
		tileAltVisuals.put(FLAT_HIGH_GRASS,     new tileAlt(new float[]{50f}, FLAT_HIGH_GRASS_ALT));
		tileAltVisuals.put(FLAT_FURROWED_GRASS, new tileAlt(new float[]{50f}, FLAT_FURROWED_ALT));

		tileAltVisuals.put(RAISED_WALL,             new tileAlt(new float[]{50f}, RAISED_WALL_ALT));
		tileAltVisuals.put(RAISED_WALL_DECO,        new tileAlt(new float[]{50f}, RAISED_WALL_DECO_ALT));
		tileAltVisuals.put(RAISED_WALL_BOOKSHELF,   new tileAlt(new float[]{50f}, RAISED_WALL_BOOKSHELF_ALT));

		tileAltVisuals.put(RAISED_HIGH_GRASS,       new tileAlt(new float[]{50f}, RAISED_HIGH_GRASS_ALT));
		tileAltVisuals.put(RAISED_FURROWED_GRASS,   new tileAlt(new float[]{50f}, RAISED_FURROWED_ALT));
		tileAltVisuals.put(HIGH_GRASS_OVERHANG,     new tileAlt(new float[]{50f}, HIGH_GRASS_OVERHANG_ALT));
		tileAltVisuals.put(FURROWED_OVERHANG,       new tileAlt(new float[]{50f}, FURROWED_OVERHANG_ALT));
	}

	public static void updateAltVariants(){
		if (Dungeon.level instanceof MiningLevel){
			if (Blacksmith.Quest.Type() == Blacksmith.Quest.CRYSTAL){
				tileAltVisuals.put(FLAT_MINE_CRYSTAL,       new tileAlt(new float[]{66.7f, 33.3f}, FLAT_MINE_CRYSTAL_ALT, FLAT_MINE_CRYSTAL_ALT_2));
				tileAltVisuals.put(RAISED_MINE_CRYSTAL_BLUE_1,     new tileAlt(new float[]{83.3f, 66.7f, 50f, 33.3f, 16.7f},
						RAISED_MINE_CRYSTAL_BLUE_2, RAISED_MINE_CRYSTAL_GREEN_1, RAISED_MINE_CRYSTAL_GREEN_2, RAISED_MINE_CRYSTAL_RED_1, RAISED_MINE_CRYSTAL_RED_2));
				tileAltVisuals.put(MINE_CRYSTAL_OVERHANG_BLUE,   new tileAlt(new float[]{66.7f, 33.3f}, MINE_CRYSTAL_OVERHANG_GREEN, MINE_CRYSTAL_OVERHANG_RED));
			} else if (Blacksmith.Quest.Type() == Blacksmith.Quest.GNOLL){
				tileAltVisuals.put(MINE_FLOOR_DECO_HEAVY,   new tileAlt(new float[]{50f}, MINE_FLOOR_DECO_HEAVY_ALT));
				tileAltVisuals.put(FLAT_MINE_BOULDER,       new tileAlt(new float[]{66.7f, 33.3f}, FLAT_MINE_BOULDER_ALT, FLAT_MINE_BOULDER_ALT_2));
				tileAltVisuals.put(RAISED_MINE_BOULDER,     new tileAlt(new float[]{66.7f, 33.3f}, RAISED_MINE_BOULDER_ALT, RAISED_MINE_BOULDER_ALT_2));
				tileAltVisuals.put(MINE_BOULDER_OVERHANG,   new tileAlt(new float[]{66.7f, 33.3f}, MINE_BOULDER_OVERHANG_ALT, MINE_BOULDER_OVERHANG_ALT_2));
			}
		}
	}

	//alt visual priority is back to front, so put lowest chance ones last
	//e.g. a 67% and a 33% chance work out to 33% chance each with a 33% chance of no alt
	public static int getVisualWithAlts(int visual, int pos){
		if (tileAltVisuals.containsKey(visual)){
			tileAlt alts = tileAltVisuals.get(visual);
			for (int i = 0; i < alts.chances.length; i++){
				if (tileVariance[pos] < alts.chances[i]){
					visual = alts.alts[i];
				}
			}
		}
		return visual;
	}

}

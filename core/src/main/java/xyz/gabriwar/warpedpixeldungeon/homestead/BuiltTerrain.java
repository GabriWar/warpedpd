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

package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

/**
 * What each of the homesteads' terrains (Terrain.BUILT_WALL_LOG..FLOOR_STONE) is: a wall or a
 * window and of which material, a door and in which state, a ground piece, a standing piece drawn
 * over the ground, what is left of it when it breaks, and its name and description. Pure lookups
 * on terrain ids: the render layers (tiles/HomesteadVisuals), the damage path (Homestead) and the
 * examine window all read it.
 */
public final class BuiltTerrain {

	private BuiltTerrain(){}

	/** A built wall, any material (no window). */
	public static boolean wall( int t ){
		return t >= Terrain.BUILT_WALL_LOG && t <= Terrain.BUILT_WALL_BRICK;
	}

	/** A built window, any material. */
	public static boolean window( int t ){
		return t >= Terrain.BUILT_WINDOW_LOG && t <= Terrain.BUILT_WINDOW_BRICK;
	}

	/** A wall or a window: what stitches as wall (DungeonTileSheet.wallStitcheable). */
	public static boolean wallLike( int t ){
		return wall( t ) || window( t );
	}

	/** The material a wall or window is drawn in (HomesteadTiles.LOG..BRICK), -1 for anything else. */
	public static int material( int t ){
		if (wall( t )) return t - Terrain.BUILT_WALL_LOG;
		if (window( t )) return t - Terrain.BUILT_WINDOW_LOG;
		return -1;
	}

	/** A built door, plain or barred, shut or open. */
	public static boolean door( int t ){
		return t >= Terrain.BUILT_DOOR && t <= Terrain.BUILT_DOOR_BARRED_OPEN;
	}

	public static boolean barred( int t ){
		return t == Terrain.BUILT_DOOR_BARRED || t == Terrain.BUILT_DOOR_BARRED_OPEN;
	}

	public static boolean gate( int t ){
		return t == Terrain.FENCE_GATE || t == Terrain.FENCE_GATE_OPEN;
	}

	/** A door or gate standing open. */
	public static boolean open( int t ){
		return t == Terrain.BUILT_DOOR_OPEN || t == Terrain.BUILT_DOOR_BARRED_OPEN || t == Terrain.FENCE_GATE_OPEN;
	}

	/** The open state of a shut door or gate, the same terrain for anything else. */
	public static int opened( int t ){
		switch (t){
			case Terrain.BUILT_DOOR:        return Terrain.BUILT_DOOR_OPEN;
			case Terrain.BUILT_DOOR_BARRED: return Terrain.BUILT_DOOR_BARRED_OPEN;
			case Terrain.FENCE_GATE:        return Terrain.FENCE_GATE_OPEN;
			default:                        return t;
		}
	}

	/** The shut state of an open door or gate, the same terrain for anything else. */
	public static int closed( int t ){
		switch (t){
			case Terrain.BUILT_DOOR_OPEN:        return Terrain.BUILT_DOOR;
			case Terrain.BUILT_DOOR_BARRED_OPEN: return Terrain.BUILT_DOOR_BARRED;
			case Terrain.FENCE_GATE_OPEN:        return Terrain.FENCE_GATE;
			default:                             return t;
		}
	}

	/** Shut to every path, opened by the hero's hand or an ally's step: a barred door, a gate. */
	public static boolean handOpened( int t ){
		return t == Terrain.BUILT_DOOR_BARRED || t == Terrain.FENCE_GATE;
	}

	/** Laid on the ground and walked over: floors, the cobbled path, bridges, farmland. */
	public static boolean ground( int t ){
		return t == Terrain.FLOOR_PLANK || t == Terrain.FLOOR_STONE || t == Terrain.PATH_STONE
				|| t == Terrain.BRIDGE_PLANK || t == Terrain.FARMLAND || t == Terrain.FARMLAND_WET;
	}

	/** A floor laid indoors. */
	public static boolean floor( int t ){
		return t == Terrain.FLOOR_PLANK || t == Terrain.FLOOR_STONE;
	}

	public static boolean farmland( int t ){
		return t == Terrain.FARMLAND || t == Terrain.FARMLAND_WET;
	}

	public static boolean stump( int t ){
		return t == Terrain.STUMP_OAK || t == Terrain.STUMP_PINE;
	}

	public static boolean sapling( int t ){
		return t == Terrain.SAPLING_OAK || t == Terrain.SAPLING_PINE;
	}

	public static boolean rubble( int t ){
		return t == Terrain.RUBBLE_WOOD || t == Terrain.RUBBLE_STONE;
	}

	/** The stump a felled tree leaves (Terrain.TREE_OAK / TREE_PINE), -1 for anything else: for
	 *  the gathering's felling, which lays it. */
	public static int stumpOf( int tree ){
		if (tree == Terrain.TREE_OAK) return Terrain.STUMP_OAK;
		if (tree == Terrain.TREE_PINE) return Terrain.STUMP_PINE;
		return -1;
	}

	/** Drawn on terrain_features.png over the ground the cells around it show (HomesteadVisuals). */
	public static boolean overlay( int t ){
		return t == Terrain.PALISADE || t == Terrain.FENCE || gate( t ) || t == Terrain.PATH_STONE
				|| stump( t ) || sapling( t ) || rubble( t );
	}

	/** Made of wood: what breaks into wooden rubble and what fire can take. */
	public static boolean wooden( int t ){
		switch (t){
			case Terrain.BUILT_WALL_LOG: case Terrain.BUILT_WALL_PLANK:
			case Terrain.BUILT_WINDOW_LOG: case Terrain.BUILT_WINDOW_PLANK:
			case Terrain.BUILT_DOOR: case Terrain.BUILT_DOOR_OPEN:
			case Terrain.BUILT_DOOR_BARRED: case Terrain.BUILT_DOOR_BARRED_OPEN:
			case Terrain.PALISADE: case Terrain.FENCE: case Terrain.FENCE_GATE: case Terrain.FENCE_GATE_OPEN:
			case Terrain.BRIDGE_PLANK: case Terrain.FLOOR_PLANK:
			//a built bookshelf (homestead/pieces/RoomPieces) is wood too
			case Terrain.BOOKSHELF: case Terrain.EMPTY_BOOKSHELF:
				return true;
			default:
				return false;
		}
	}

	/** What a piece leaves when it is wrecked: splinters for wood, stone chunks for the rest. */
	public static int rubbleOf( int t ){
		return wooden( t ) ? Terrain.RUBBLE_WOOD : Terrain.RUBBLE_STONE;
	}

	/** The string key of a homestead terrain (homestead.properties: builtterrain.KEY_name / _desc). */
	public static String key( int t ){
		switch (t){
			case Terrain.BUILT_WALL_LOG:         return "wall_log";
			case Terrain.BUILT_WALL_PLANK:       return "wall_plank";
			case Terrain.BUILT_WALL_STONE:       return "wall_stone";
			case Terrain.BUILT_WALL_BRICK:       return "wall_brick";
			case Terrain.BUILT_WINDOW_LOG:       return "window_log";
			case Terrain.BUILT_WINDOW_PLANK:     return "window_plank";
			case Terrain.BUILT_WINDOW_STONE:     return "window_stone";
			case Terrain.BUILT_WINDOW_BRICK:     return "window_brick";
			case Terrain.BUILT_DOOR:             return "door";
			case Terrain.BUILT_DOOR_OPEN:        return "door_open";
			case Terrain.BUILT_DOOR_BARRED:      return "door_barred";
			case Terrain.BUILT_DOOR_BARRED_OPEN: return "door_barred_open";
			case Terrain.PALISADE:               return "palisade";
			case Terrain.FENCE:                  return "fence";
			case Terrain.FENCE_GATE:             return "gate";
			case Terrain.FENCE_GATE_OPEN:        return "gate_open";
			case Terrain.PATH_STONE:             return "path";
			case Terrain.BRIDGE_PLANK:           return "bridge";
			case Terrain.FARMLAND:               return "farmland";
			case Terrain.FARMLAND_WET:           return "farmland_wet";
			case Terrain.STUMP_OAK:              return "stump_oak";
			case Terrain.STUMP_PINE:             return "stump_pine";
			case Terrain.SAPLING_OAK:            return "sapling_oak";
			case Terrain.SAPLING_PINE:           return "sapling_pine";
			case Terrain.RUBBLE_WOOD:            return "rubble_wood";
			case Terrain.RUBBLE_STONE:           return "rubble_stone";
			case Terrain.FLOOR_PLANK:            return "floor_plank";
			case Terrain.FLOOR_STONE:            return "floor_stone";
			default:                             return null;
		}
	}

	/** The examine name of a homestead terrain, null for any other. */
	public static String name( int t ){
		String k = key( t );
		return k == null ? null : Messages.get( BuiltTerrain.class, k + "_name" );
	}

	/** The examine description of a homestead terrain, null for any other. */
	public static String desc( int t ){
		String k = key( t );
		return k == null ? null : Messages.get( BuiltTerrain.class, k + "_desc" );
	}
}

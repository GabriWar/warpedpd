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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

/**
 * How the homesteads' terrains are drawn on the four render layers (docs/homesteads-art-world.md
 * sections 3 to 6): the ground layer (DungeonTerrainTilemap), the walls drawn over the mobs
 * (DungeonWallsTilemap) and the overlay sheet (TerrainFeaturesTilemap). The raised layer draws
 * nothing for them: it only carries the grass's tufts. Each layer asks here first and falls back
 * to its own rules when the answer is NONE.
 *
 * The walls take the dungeon's stitch numbers (+1 open right, +2 open left; the internal mask;
 * the lip's +1 down-right, +2 down-left) and pick their material's art with them. Everything else
 * reads a 4-neighbour mask, 1 N, 2 E, 4 S, 8 W, the bit set where that neighbour joins it.
 */
public final class HomesteadVisuals {

	/** Not one of ours: the layer draws the cell by its own rules. */
	public static final int NONE = Integer.MIN_VALUE;

	private HomesteadVisuals(){}

	private static boolean ws( int t ){
		return DungeonTileSheet.wallStitcheable( t );
	}

	//the world-anchored variance of a cell (tileVariance), 0..99
	private static int variance( int pos ){
		byte[] v = DungeonTileSheet.tileVariance;
		return v == null || pos < 0 || pos >= v.length ? 0 : v[pos];
	}

	// ------------------------------------------------------------ the ground layer

	/**
	 * DungeonTerrainTilemap.getTileVisual for a homestead terrain: a wall's or window's face (or
	 * nothing, where wall stands in front of it: its top is the walls layer's), a door, a bridge,
	 * farmland, a floor, or under a standing piece the ground the cells around it show.
	 */
	static int terrain( DungeonTerrainTilemap layer, int[] map, int w, int size, int pos, int tile, boolean flat ){
		if (BuiltTerrain.wallLike( tile )){
			int m = BuiltTerrain.material( tile );
			boolean window = BuiltTerrain.window( tile );
			if (flat) return window ? HomesteadTiles.flatWindow( m ) : HomesteadTiles.flat( m, variance( pos ) < 50 );
			int below = pos + w < size ? map[pos + w] : -1;
			if (below == -1 || ws( below )) return -1;
			int s = 0;
			if (!ws( (pos + 1) % w != 0 ? map[pos + 1] : -1 )) s += 1;
			if (!ws( pos % w != 0 ? map[pos - 1] : -1 )) s += 2;
			if (DungeonTileSheet.doorTile( below )) return HomesteadTiles.faceOverDoor( m, s );
			return window ? HomesteadTiles.windowFace( m, s ) : HomesteadTiles.face( m, s, variance( pos ) < 50 );
		}
		if (BuiltTerrain.door( tile )){
			if (!flat && pos >= w && ws( map[pos - w] )) return HomesteadTiles.DOOR_THRESHOLD;
			switch (tile){
				case Terrain.BUILT_DOOR:             return HomesteadTiles.DOOR;
				case Terrain.BUILT_DOOR_OPEN:        return HomesteadTiles.DOOR_OPEN;
				case Terrain.BUILT_DOOR_BARRED:      return HomesteadTiles.BARRED_DOOR;
				default:                             return HomesteadTiles.BARRED_DOOR_OPEN;
			}
		}
		switch (tile){
			case Terrain.FLOOR_PLANK:
				return DungeonTileSheet.getVisualWithAlts( HomesteadTiles.FLOOR_PLANK, pos );
			case Terrain.FLOOR_STONE:
				return DungeonTileSheet.getVisualWithAlts( HomesteadTiles.FLOOR_STONE, pos );
			case Terrain.FARMLAND: case Terrain.FARMLAND_WET:
				return HomesteadTiles.farmland( mask( map, w, size, pos, HomesteadVisuals::farm ), tile == Terrain.FARMLAND_WET );
			case Terrain.BRIDGE_PLANK:
				return bridge( map, w, size, pos );
		}
		//a standing piece (palisade, fence, gate, path, stump, sapling, rubble): its art is an
		//overlay, under it the ground the cells around show
		int ground = layer.sliceGroundAround( pos );
		if (ground != -1) return layer.getTileVisual( ground, map[ground], flat );
		if (Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).frozenAt( pos )){
			return DungeonTileSheet.SNOW_TILE;
		}
		return DungeonTileSheet.getVisualWithAlts( tile == Terrain.PATH_STONE || BuiltTerrain.rubble( tile )
				? DungeonTileSheet.FLOOR : DungeonTileSheet.GRASS, pos );
	}

	private interface Joins {
		boolean joins( int t );
	}

	//the 4-neighbour mask of what joins this cell: 1 N, 2 E, 4 S, 8 W
	private static int mask( int[] map, int w, int size, int pos, Joins j ){
		int m = 0;
		if (pos >= w && j.joins( map[pos - w] )) m |= 1;
		if ((pos + 1) % w != 0 && j.joins( map[pos + 1] )) m |= 2;
		if (pos + w < size && j.joins( map[pos + w] )) m |= 4;
		if (pos % w != 0 && j.joins( map[pos - 1] )) m |= 8;
		return m;
	}

	private static boolean farm( int t ){
		return BuiltTerrain.farmland( t );
	}

	private static boolean bridgeAt( int t ){
		return t == Terrain.BRIDGE_PLANK;
	}

	//a plank bridge: east-west if the bridge runs on east or west, or if it runs on neither north
	//nor south; its ends where the span stops, its rails along the sides open to the water
	private static int bridge( int[] map, int w, int size, int pos ){
		int m = mask( map, w, size, pos, HomesteadVisuals::bridgeAt );
		boolean n = (m & 1) != 0, e = (m & 2) != 0, s = (m & 4) != 0, wst = (m & 8) != 0;
		if (e || wst || !(n || s)){
			int ends = (wst ? 0 : HomesteadTiles.END_FIRST) | (e ? 0 : HomesteadTiles.END_LAST);
			int rails = (n ? 0 : HomesteadTiles.RAIL_FIRST) | (s ? 0 : HomesteadTiles.RAIL_LAST);
			return HomesteadTiles.bridgeEW( ends, rails );
		}
		int ends = (n ? 0 : HomesteadTiles.END_FIRST) | (s ? 0 : HomesteadTiles.END_LAST);
		int rails = (wst ? 0 : HomesteadTiles.RAIL_FIRST) | (e ? 0 : HomesteadTiles.RAIL_LAST);
		return HomesteadTiles.bridgeNS( ends, rails );
	}

	// ------------------------------------------------------------ the walls layer

	/**
	 * DungeonWallsTilemap.getTileVisual, asked first: a wall's top (the internal stitch) or the
	 * leaf of a shut built door edge-on over the wall above it; NONE for everything else.
	 */
	static int wallTop( int[] map, int w, int size, int pos, int tile ){
		if (!ws( tile )) return NONE;
		int below = pos + w < size ? map[pos + w] : -1;
		//a built door below any wall: its leaf edge-on, nothing when it stands open
		if (BuiltTerrain.door( below )){
			if (BuiltTerrain.open( below )) return DungeonTileSheet.NULL_TILE;
			return BuiltTerrain.barred( below ) ? HomesteadTiles.BARRED_SIDEWAYS : HomesteadTiles.DOOR_SIDEWAYS;
		}
		if (!BuiltTerrain.wallLike( tile )) return NONE;
		int m = BuiltTerrain.material( tile );
		if (!ws( below )){
			//the face is the ground layer's; a dungeon door below keeps the dungeon's leaf
			switch (below){
				case Terrain.DOOR:         return DungeonTileSheet.DOOR_SIDEWAYS;
				case Terrain.LOCKED_DOOR:
				case Terrain.HERO_LKD_DR:  return DungeonTileSheet.DOOR_SIDEWAYS_LOCKED;
				case Terrain.CRYSTAL_DOOR: return DungeonTileSheet.DOOR_SIDEWAYS_CRYSTAL;
				default:                   return DungeonTileSheet.NULL_TILE;
			}
		}
		boolean right = (pos + 1) % w != 0, left = pos % w != 0;
		int mask = 0;
		if (!ws( right ? map[pos + 1] : -1 )) mask |= DungeonTileSheet.RIM_EAST;
		if (!ws( right && pos + 1 + w < size ? map[pos + 1 + w] : -1 )) mask |= DungeonTileSheet.RIM_EAST_BELOW;
		if (!ws( left && pos - 1 + w < size ? map[pos - 1 + w] : -1 )) mask |= DungeonTileSheet.RIM_WEST_BELOW;
		if (!ws( left ? map[pos - 1] : -1 )) mask |= DungeonTileSheet.RIM_WEST;
		return BuiltTerrain.window( tile ) ? HomesteadTiles.windowInternal( m, mask ) : HomesteadTiles.internal( m, mask );
	}

	/**
	 * DungeonWallsTilemap.getTileVisual for a cell that is no wall: the lip of a built wall below
	 * it in that wall's material (a door's own lip over it, when the cell is a door), or the
	 * lintel over a built door below; NONE for everything else.
	 */
	static int lip( int[] map, int w, int size, int pos, int tile ){
		if (pos + w >= size) return NONE;
		int below = map[pos + w];
		if (BuiltTerrain.wallLike( below )){
			int mb = BuiltTerrain.material( below );
			int s = 0;
			if (!ws( (pos + 1) % w != 0 ? map[pos + 1 + w] : -1 )) s += 1;
			if (!ws( pos % w != 0 ? map[pos - 1 + w] : -1 )) s += 2;
			if (BuiltTerrain.door( tile )) return HomesteadTiles.sideDoorOverhang( mb, BuiltTerrain.barred( tile ), BuiltTerrain.open( tile ), s );
			//the dungeon's own doors keep their lip
			if (DungeonTileSheet.doorTile( tile )) return NONE;
			return BuiltTerrain.window( below ) ? HomesteadTiles.windowOverhang( mb, s ) : HomesteadTiles.overhang( mb, s );
		}
		switch (below){
			case Terrain.BUILT_DOOR:             return HomesteadTiles.DOOR_OVERHANG;
			case Terrain.BUILT_DOOR_OPEN:        return HomesteadTiles.DOOR_OVERHANG_OPEN;
			case Terrain.BUILT_DOOR_BARRED:      return HomesteadTiles.BARRED_OVERHANG;
			case Terrain.BUILT_DOOR_BARRED_OPEN: return HomesteadTiles.BARRED_OVERHANG_OPEN;
			default:                             return NONE;
		}
	}

	// ------------------------------------------------------------ the overlay sheet

	/** TerrainFeaturesTilemap's picture of a standing piece, -1 for a homestead terrain with none. */
	static int feature( int[] map, int w, int size, int pos, int tile ){
		switch (tile){
			case Terrain.PALISADE:
				return HomesteadTiles.palisade( mask( map, w, size, pos, HomesteadVisuals::palisadeJoins ) );
			case Terrain.FENCE:
				return HomesteadTiles.fence( mask( map, w, size, pos, HomesteadVisuals::fenceJoins ) );
			case Terrain.FENCE_GATE: case Terrain.FENCE_GATE_OPEN: {
				int m = mask( map, w, size, pos, HomesteadVisuals::fenceJoins );
				boolean ew = (m & (2 | 8)) != 0 || (m & (1 | 4)) == 0;
				boolean open = tile == Terrain.FENCE_GATE_OPEN;
				if (ew) return open ? HomesteadTiles.GATE_EW_OPEN : HomesteadTiles.GATE_EW;
				return open ? HomesteadTiles.GATE_NS_OPEN : HomesteadTiles.GATE_NS;
			}
			case Terrain.PATH_STONE: {
				int m = mask( map, w, size, pos, HomesteadVisuals::pathJoins );
				if (m != 15) return HomesteadTiles.pathStone( m );
				int v = variance( pos );
				return v < 33 ? HomesteadTiles.PATH_STONE_FULL_ALT : v < 66 ? HomesteadTiles.PATH_STONE_FULL_ALT_2 : HomesteadTiles.PATH_STONE_FULL;
			}
			case Terrain.STUMP_OAK:    return HomesteadTiles.STUMP_OAK;
			case Terrain.STUMP_PINE:   return HomesteadTiles.STUMP_PINE;
			case Terrain.SAPLING_OAK:  return HomesteadTiles.SAPLING_OAK;
			case Terrain.SAPLING_PINE: return HomesteadTiles.SAPLING_PINE;
			case Terrain.RUBBLE_WOOD:  return variance( pos ) < 50 ? HomesteadTiles.RUBBLE_WOOD_ALT : HomesteadTiles.RUBBLE_WOOD;
			case Terrain.RUBBLE_STONE: return variance( pos ) < 50 ? HomesteadTiles.RUBBLE_STONE_ALT : HomesteadTiles.RUBBLE_STONE;
			default:                   return -1;
		}
	}

	//a palisade runs into palisades, gates and built walls
	private static boolean palisadeJoins( int t ){
		return t == Terrain.PALISADE || BuiltTerrain.gate( t ) || BuiltTerrain.wallLike( t );
	}

	//a fence (and a gate's posts) runs into fences, gates and palisades
	private static boolean fenceJoins( int t ){
		return t == Terrain.FENCE || BuiltTerrain.gate( t ) || t == Terrain.PALISADE;
	}

	//a cobbled path runs into paths, doors and bridges
	private static boolean pathJoins( int t ){
		return t == Terrain.PATH_STONE || DungeonTileSheet.doorTile( t )
				|| t == Terrain.BRIDGE_PLANK || t == Terrain.BRIDGE;
	}

	// ------------------------------------------------------------ pictures for the menus

	/** The cell of one of the land's own terrains (no homestead terrain: a rug, a statue, a shelf,
	 *  the alchemy pot) on the surface's tile sheet, standing alone as a build piece lays it: its
	 *  raised picture where it has one, else its flat one, else plain ground. */
	public static int landFrame( int tile ){
		switch (tile){
			case Terrain.ALCHEMY:         return DungeonTileSheet.RAISED_ALCHEMY_POT;
			case Terrain.STATUE:          return DungeonTileSheet.RAISED_STATUE;
			case Terrain.STATUE_SP:       return DungeonTileSheet.RAISED_STATUE_SP;
			case Terrain.BARRICADE:       return DungeonTileSheet.RAISED_BARRICADE;
			//a shelf's face open on both sides (DungeonTileSheet.getRaisedWallTile)
			case Terrain.BOOKSHELF:       return DungeonTileSheet.RAISED_WALL_BOOKSHELF + 3;
			case Terrain.EMPTY_BOOKSHELF: return DungeonTileSheet.RAISED_WALL_BOOKSHELF_EMPTY + 3;
		}
		Integer direct = DungeonTileSheet.directVisuals.get( tile );
		if (direct != null) return direct;
		Integer flat = DungeonTileSheet.directFlatVisuals.get( tile );
		return flat != null ? flat : DungeonTileSheet.FLOOR;
	}

	/** The sheet a homestead terrain's picture comes from: the overlay sheet for the standing
	 *  pieces, the surface's tile sheet for the rest (the cave sheet holds the same cells). */
	public static String previewTexture( int tile ){
		return BuiltTerrain.overlay( tile ) ? xyz.gabriwar.warpedpixeldungeon.Assets.Environment.TERRAIN_FEATURES
				: xyz.gabriwar.warpedpixeldungeon.Assets.Environment.TILES_OVERWORLD;
	}

	/** The cell of a homestead terrain's picture on its sheet (previewTexture), standing alone:
	 *  a wall's face open both sides, a fence in an east-west run, a path's full cobbles. */
	public static int previewFrame( int tile ){
		if (BuiltTerrain.wall( tile )) return HomesteadTiles.face( BuiltTerrain.material( tile ), 3, false );
		if (BuiltTerrain.window( tile )) return HomesteadTiles.windowFace( BuiltTerrain.material( tile ), 3 );
		switch (tile){
			case Terrain.BUILT_DOOR:             return HomesteadTiles.DOOR;
			case Terrain.BUILT_DOOR_OPEN:        return HomesteadTiles.DOOR_OPEN;
			case Terrain.BUILT_DOOR_BARRED:      return HomesteadTiles.BARRED_DOOR;
			case Terrain.BUILT_DOOR_BARRED_OPEN: return HomesteadTiles.BARRED_DOOR_OPEN;
			case Terrain.BRIDGE_PLANK:           return HomesteadTiles.bridgeEW( HomesteadTiles.END_FIRST | HomesteadTiles.END_LAST,
					HomesteadTiles.RAIL_FIRST | HomesteadTiles.RAIL_LAST );
			case Terrain.FARMLAND:               return HomesteadTiles.farmland( 0, false );
			case Terrain.FARMLAND_WET:           return HomesteadTiles.farmland( 0, true );
			case Terrain.FLOOR_PLANK:            return HomesteadTiles.FLOOR_PLANK;
			case Terrain.FLOOR_STONE:            return HomesteadTiles.FLOOR_STONE;
			case Terrain.PALISADE:               return HomesteadTiles.palisade( 2 | 8 );
			case Terrain.FENCE:                  return HomesteadTiles.fence( 2 | 8 );
			case Terrain.FENCE_GATE:             return HomesteadTiles.GATE_EW;
			case Terrain.FENCE_GATE_OPEN:        return HomesteadTiles.GATE_EW_OPEN;
			case Terrain.PATH_STONE:             return HomesteadTiles.PATH_STONE_FULL;
			case Terrain.STUMP_OAK:              return HomesteadTiles.STUMP_OAK;
			case Terrain.STUMP_PINE:             return HomesteadTiles.STUMP_PINE;
			case Terrain.SAPLING_OAK:            return HomesteadTiles.SAPLING_OAK;
			case Terrain.SAPLING_PINE:           return HomesteadTiles.SAPLING_PINE;
			case Terrain.RUBBLE_WOOD:            return HomesteadTiles.RUBBLE_WOOD;
			case Terrain.RUBBLE_STONE:           return HomesteadTiles.RUBBLE_STONE;
			//the one land terrain a piece lays (homestead/pieces/StationPieces): the alchemy pot
			case Terrain.ALCHEMY:                return DungeonTileSheet.RAISED_ALCHEMY_POT;
			default: throw new IllegalArgumentException( "not a homestead terrain: " + tile );
		}
	}

	// ------------------------------------------------------------ the fog

	/** Fogged by its own sight like a floor, never blacked out by the wall rules (FogOfWar): a
	 *  built wall's top is painted, not the dungeon's void, and the rules would black out a thick
	 *  block of it and the inside corners of a house. */
	public static boolean ownFog( int tile ){
		return BuiltTerrain.wallLike( tile );
	}

	/** ownFog for a cell of the shown level: a built wall or window, and on a world slice a
	 *  bookshelf too - the world makes none, so every one there is a piece of furniture the hero put
	 *  in a room (homestead/pieces/RoomPieces), and a row of them along a wall would be blacked out by
	 *  the rules as the inside of a wall. */
	public static boolean ownFog( xyz.gabriwar.warpedpixeldungeon.levels.Level level, int cell ){
		int t = level.map[cell];
		return ownFog( t ) || (level instanceof OverworldLevel
				&& (t == Terrain.BOOKSHELF || t == Terrain.EMPTY_BOOKSHELF));
	}
}

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
import com.watabou.noosa.Image;
import com.watabou.utils.PathFinder;

public class DungeonTerrainTilemap extends DungeonTilemap {

	static DungeonTerrainTilemap instance;

	public DungeonTerrainTilemap(){
		super(Dungeon.level.tilesTex());

		map( Dungeon.level.map, Dungeon.level.width() );

		instance = this;
	}

	@Override
	protected int getTileVisual(int pos, int tile, boolean flat) {
		//in the safe zone, tilled soil gets its own farm-plot art instead of
		//the furrowed grass visual used by the warden in the dungeon.
		//NUANCE: a terrain's look is composed by FOUR tilemaps, and changing it
		//means gating every layer, not just this one:
		//  1. DungeonTerrainTilemap (here) - the ground, flat+raised branches
		//  2. RaisedTerrainTilemap - the raised tuft/underhang drawn over it
		//  3. DungeonWallsTilemap - the overhang drawn on the wall above it
		//  4. TerrainFeaturesTilemap - per-region stage dressing (grass/embers)
		//miss one and the old art keeps peeking through (see the SafeLevel
		//FURROWED_GRASS gates in each of those classes)
		if (tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.FURROWED_GRASS
				&& Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.SafeLevel){
			return DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.TILLED_SOIL, pos);
		}
		//on the surface's frozen ground, whatever grows or lies there sits on snow
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
				&& (tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.TREE_PINE
					|| tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.TREE_OAK
					|| tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.SHRUB
					|| tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.BOULDER
					|| tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.GRASS
					|| tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.SIGN)
				&& ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).frozenAt( pos )){
			return DungeonTileSheet.SNOW_TILE;
		}
		//surface waystones get their own monolith art instead of the region's
		//special-entrance stairs
		if (tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.ENTRANCE_SP
				&& Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel){
			return DungeonTileSheet.WAYSTONE_TILE;
		}
		int visual = DungeonTileSheet.directVisuals.get(tile, -1);
		if (visual != -1) return DungeonTileSheet.getVisualWithAlts(visual, pos);

		if (tile == Terrain.WATER || tile == Terrain.DEEP_WATER) {
			return DungeonTileSheet.stitchWaterTile(
					safeTile(pos + PathFinder.CIRCLE4[0]),
					safeTile(pos + PathFinder.CIRCLE4[1]),
					safeTile(pos + PathFinder.CIRCLE4[2]),
					safeTile(pos + PathFinder.CIRCLE4[3])
			);

		} else if (tile == Terrain.FROZEN_WATER) {
			return DungeonTileSheet.stitchFrozenWaterTile(
					safeTile(pos + PathFinder.CIRCLE4[0]),
					safeTile(pos + PathFinder.CIRCLE4[1]),
					safeTile(pos + PathFinder.CIRCLE4[2]),
					safeTile(pos + PathFinder.CIRCLE4[3])
			);

		} else if (tile == Terrain.CHASM) {
			return DungeonTileSheet.stitchChasmTile( pos > mapWidth ? map[pos - mapWidth] : -1);
		}

		if (!flat) {
			if ((DungeonTileSheet.doorTile(tile))) {
				return DungeonTileSheet.getRaisedDoorTile(tile, map[pos - mapWidth]);
			} else if (DungeonTileSheet.wallStitcheable(tile)){
				return DungeonTileSheet.getRaisedWallTile(
						tile,
						pos,
						(pos+1) % mapWidth != 0 ?   map[pos + 1] : -1,
						pos + mapWidth < size ?     map[pos + mapWidth] : -1,
						pos % mapWidth != 0 ?       map[pos - 1] : -1
						);
			} else if (tile == Terrain.STATUE) {
				return DungeonTileSheet.RAISED_STATUE;
			} else if (tile == Terrain.STATUE_SP) {
				return DungeonTileSheet.RAISED_STATUE_SP;
			} else if (tile == Terrain.REGION_DECO) {
				return DungeonTileSheet.RAISED_REGION_DECO;
			} else if (tile == Terrain.REGION_DECO_ALT) {
				return DungeonTileSheet.RAISED_REGION_DECO_ALT;
			} else if (tile == Terrain.MINE_CRYSTAL) {
				return DungeonTileSheet.getVisualWithAlts(
						DungeonTileSheet.RAISED_MINE_CRYSTAL,
						pos);
			} else if (tile == Terrain.MINE_BOULDER) {
				return DungeonTileSheet.getVisualWithAlts(
						DungeonTileSheet.RAISED_MINE_BOULDER,
						pos);
			} else if (tile == Terrain.ALCHEMY) {
				return DungeonTileSheet.RAISED_ALCHEMY_POT;
			} else if (tile == Terrain.BARRICADE) {
				return DungeonTileSheet.RAISED_BARRICADE;
			} else if (tile == Terrain.HIGH_GRASS
					|| tile == Terrain.SOIL_CORNWHEAT
					|| tile == Terrain.SOIL_GREENWHEAT
					|| tile == Terrain.SOIL_STRAWWHEAT
					|| tile == Terrain.SOIL_WATERWHEAT) {
				return DungeonTileSheet.getVisualWithAlts(
						DungeonTileSheet.RAISED_HIGH_GRASS,
						pos);
			} else if (tile == Terrain.FURROWED_GRASS) {
				return DungeonTileSheet.getVisualWithAlts(
						DungeonTileSheet.RAISED_FURROWED_GRASS,
						pos);
			} else {
				return DungeonTileSheet.NULL_TILE;
			}
		} else {
			Integer flatVisual = DungeonTileSheet.directFlatVisuals.get(tile);
			if (flatVisual == null) return DungeonTileSheet.NULL_TILE;
			return DungeonTileSheet.getVisualWithAlts(flatVisual, pos);
		}

	}

	//a neighbour read that can fall off the map (water on an edge row) returns
	//-1, which no stitcheable set contains - same convention as the chasm
	private int safeTile(int pos){
		return pos >= 0 && pos < map.length ? map[pos] : -1;
	}

	public static Image tile(int pos, int tile ) {
		Image img = new Image( instance.texture );
		img.frame( instance.tileset.get( instance.getTileVisual( pos, tile, true ) ) );
		return img;
	}

	@Override
	protected boolean needsRender(int pos) {
		return super.needsRender(pos)
				&& data[pos] != DungeonTileSheet.WATER;
	}
}

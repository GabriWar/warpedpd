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
import xyz.gabriwar.warpedpixeldungeon.levels.MiningLevel;
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
		//the homesteads' walls, doors, grounds and the ground under their standing pieces
		if (Terrain.homestead(tile)) {
			return HomesteadVisuals.terrain(this, map, mapWidth, size, pos, tile, flat);
		}
		//a surface village's fields get their crop of the season instead of
		//the furrowed grass visual used by the warden in the dungeon.
		//NUANCE: a terrain's look is composed by FOUR tilemaps, and changing it
		//means gating every layer, not just this one:
		//  1. DungeonTerrainTilemap (here) - the ground, flat+raised branches
		//  2. RaisedTerrainTilemap - the raised tuft/underhang drawn over it
		//  3. DungeonWallsTilemap - the overhang drawn on the wall above it
		//  4. TerrainFeaturesTilemap - per-region stage dressing (grass/embers)
		//miss one and the old art keeps peeking through (see the
		//DungeonTileSheet.tilledSoil gates in each of those classes)
		if (tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.FURROWED_GRASS
				&& DungeonTileSheet.tilledSoil( pos )){
			//tilledSoil is only ever true on the world's slices
			return DungeonTileSheet.fieldTile(
					((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).fieldSeason(), pos );
		}
		//a boulder in the world lies on the ground its neighbours show, never on a
		//square of its own (snow in a green field, grass in a snowfield)
		if (tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.BOULDER
				&& Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel){
			int ground = ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).groundUnderRock( pos );
			return DungeonTileSheet.getVisualWithAlts( DungeonTileSheet.directVisuals.get( ground, DungeonTileSheet.FLOOR ), pos );
		}
		//on the surface's frozen ground, whatever grows there sits on snow, and so does the floor
		//left where rock was mined away (Terrain.EMPTY_DECO)
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
				&& (tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.TREE_PINE
					|| tile == Terrain.EMPTY
					|| tile == Terrain.EMPTY_DECO
					|| tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.TREE_OAK
					|| tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.SHRUB
					|| tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.GRASS
					|| tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.SIGN)
				&& ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).frozenAt( pos )){
			return DungeonTileSheet.SNOW_TILE;
		}
		//a way between the slices is an arrow on the ground (the dressing), not a flight of stairs
		if ((tile == Terrain.ENTRANCE || tile == Terrain.EXIT)
				&& Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
				&& ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).arrowLink( pos )){
			int ground = sliceGroundAround( pos );
			if (ground != -1) return getTileVisual( ground, map[ground], flat );
		}
		//surface waystones get their own monolith art instead of the region's
		//special-entrance stairs
		if (tile == xyz.gabriwar.warpedpixeldungeon.levels.Terrain.ENTRANCE_SP
				&& Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel){
			return DungeonTileSheet.WAYSTONE_TILE;
		}
		//a custom decoration has one fixed visual (plain stone floor), so a prop laid on a
		//special floor or on grass used to sit on a grey square of its own. Take the ground
		//from the cells around it instead, so the furniture stands on the room's own floor
		if (tile == Terrain.CUSTOM_DECO || tile == Terrain.CUSTOM_DECO_EMPTY) {
			int ground = groundAround(pos);
			if (ground != -1) return DungeonTileSheet.getVisualWithAlts(ground, pos);
		}

		int visual = DungeonTileSheet.directVisuals.get(tile, -1);
		if (visual != -1) {
			if (visual == DungeonTileSheet.FLOOR_DECO && Dungeon.level instanceof MiningLevel) {
				for (int i : PathFinder.NEIGHBOURS4) {
					if (map[pos + i] == Terrain.MINE_BOULDER) {
						visual = DungeonTileSheet.MINE_FLOOR_DECO_HEAVY;
						break;
					}
				}
			}
			return DungeonTileSheet.getVisualWithAlts(visual, pos);
		}

		if (tile == Terrain.WATER || tile == Terrain.DEEP_WATER) {
			//under the open sky the shore takes the colour of its bank (the shore rows exist
			//on the surface's tileset only - a cave slice draws the caves' water)
			if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
					&& xyz.gabriwar.warpedpixeldungeon.Assets.Environment.TILES_OVERWORLD.equals( Dungeon.level.tilesTex() )){
				return DungeonTileSheet.stitchOverworldWater(
						safeTile(pos + PathFinder.CIRCLE4[0]),
						safeTile(pos + PathFinder.CIRCLE4[1]),
						safeTile(pos + PathFinder.CIRCLE4[2]),
						safeTile(pos + PathFinder.CIRCLE4[3])
				);
			}
			return DungeonTileSheet.stitchWaterTile(
					safeTile(pos + PathFinder.CIRCLE4[0]),
					safeTile(pos + PathFinder.CIRCLE4[1]),
					safeTile(pos + PathFinder.CIRCLE4[2]),
					safeTile(pos + PathFinder.CIRCLE4[3])
			);

		} else if (tile == Terrain.FROZEN_WATER) {
			//in the world the ice is drawn plain and the ground around it spills
			//over its edges (the overworld dressing's blend layers)
			if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel){
				return DungeonTileSheet.FROZEN_WATER;
			}
			return DungeonTileSheet.stitchFrozenWaterTile(
					safeTile(pos + PathFinder.CIRCLE4[0]),
					safeTile(pos + PathFinder.CIRCLE4[1]),
					safeTile(pos + PathFinder.CIRCLE4[2]),
					safeTile(pos + PathFinder.CIRCLE4[3])
			);

		} else if (tile == Terrain.CHASM) {
			int chasm = DungeonTileSheet.stitchChasmTile( pos > mapWidth ? map[pos - mapWidth] : -1);
			if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
					&& ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).seesBelow( pos )){
				//a mountain's drop that shows the ground far below (tiles/SliceGroundLayer) draws no
				//edge of its own: the plain chasm, which needsRender skips
				if (((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).openSky()){
					return DungeonTileSheet.CHASM;
				}
				//a cave's pit keeps the face of what drops away into it, the floor below showing where
				//its dark was; the picture of an examined pit keeps the chasm whole
				if (!flat) return DungeonTileSheet.pitFace( chasm );
			}
			return chasm;
		}

		if (!flat) {
			if ((DungeonTileSheet.doorTile(tile))) {
				return DungeonTileSheet.getRaisedDoorTile(tile, map[pos - mapWidth]);
			} else if (DungeonTileSheet.wallStitcheable(tile)){
				int face = DungeonTileSheet.getRaisedWallTile(
						tile,
						pos,
						(pos+1) % mapWidth != 0 ?   map[pos + 1] : -1,
						pos + mapWidth < size ?     map[pos + mapWidth] : -1,
						pos % mapWidth != 0 ?       map[pos - 1] : -1
						);
				//an earthen scarp of the overworld's rock (WorldModel.earthenScarp)
				if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
						&& ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).earthAt(pos)){
					face = DungeonTileSheet.earthen(face);
				}
				return face;
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
						DungeonTileSheet.RAISED_MINE_CRYSTAL_BLUE_1,
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

	//the floor the cells around pos are showing, or -1 when none of them is a plain floor.
	//The commonest one wins, so a prop by a door keeps the room's floor and not the
	//corridor's. Only bare ground counts: water, embers, traps and the like are skipped
	//the neighbour whose plain ground is the commonest round this cell (the slices' floors:
	//snow, dirt, grass, paths), so a cell can be drawn as it; -1 if none is plain ground
	int sliceGroundAround(int pos){
		int best = -1, bestCount = 0;
		for (int i : PathFinder.NEIGHBOURS8){
			int n = pos + i;
			int t = safeTile(n);
			if (!sliceGround(t)) continue;
			int count = 0;
			for (int j : PathFinder.NEIGHBOURS8) if (safeTile(pos + j) == t) count++;
			if (count > bestCount){
				best = n;
				bestCount = count;
			}
		}
		return best;
	}

	private static boolean sliceGround(int t){
		switch (t){
			case Terrain.EMPTY: case Terrain.EMPTY_DECO: case Terrain.EMPTY_SP:
			case Terrain.GRASS: case Terrain.HIGH_GRASS: case Terrain.FURROWED_GRASS:
			case Terrain.SNOW: case Terrain.DIRT_PATH: case Terrain.MUD:
				return true;
			default:
				return false;
		}
	}

	private int groundAround(int pos){
		int plain = 0, special = 0, grass = 0;
		for (int i : PathFinder.NEIGHBOURS8){
			switch (safeTile(pos + i)){
				case Terrain.EMPTY: case Terrain.EMPTY_DECO:
					plain++; break;
				case Terrain.EMPTY_SP:
					special++; break;
				case Terrain.GRASS: case Terrain.HIGH_GRASS: case Terrain.FURROWED_GRASS:
					grass++; break;
			}
		}
		if (grass >= special && grass >= plain && grass > 0) return DungeonTileSheet.GRASS;
		if (special >= plain && special > 0)                 return DungeonTileSheet.FLOOR_SP;
		if (plain > 0)                                       return DungeonTileSheet.FLOOR;
		return -1;
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
				&& data[pos] != DungeonTileSheet.WATER
				//a mountain's drop shows the ground below it (tiles/SliceGroundLayer), not the void, and so does
				//a cave's pit with no face of its own (DungeonTileSheet.pitFace)
				&& !(data[pos] == DungeonTileSheet.CHASM
					&& Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
					&& ((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).seesBelow( pos ));
	}
}

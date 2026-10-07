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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.LastShopLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

import java.util.HashSet;

public class RaisedTerrainTilemap extends DungeonTilemap {
	
	public RaisedTerrainTilemap() {
		super(textureForRegion());
		skipCells.clear();
		map( Dungeon.level.map, Dungeon.level.width() );
	}

    static String textureForRegion() {
        String tiles = Dungeon.level.tilesTex();
        if (Assets.Environment.TILES_FROZEN.equals(tiles)) return Assets.Environment.RAISED_FROZEN;
        if (Assets.Environment.TILES_SPIDERNEST.equals(tiles)) return Assets.Environment.RAISED_SPIDERNEST;
        return Assets.Environment.RAISED_TERRAIN;
    }

	public static HashSet<Integer> skipCells = new HashSet<>();

	/** Rebase fast path: skipCells hold window indices - slide them with the
	 *  window instead of leaving them stale over the shifted content. */
	public static void shiftSkipCells( int dcx, int dcy, int w, int h ){
		HashSet<Integer> shifted = new HashSet<>();
		for (int c : skipCells){
			int x = c % w - dcx, y = c / w - dcy;
			if (x > 0 && y > 0 && x < w-1 && y < h-1){
				shifted.add( x + y * w );
			}
		}
		skipCells.clear();
		skipCells.addAll( shifted );
	}

	//the raised grass sheet holds one row per region. Warped levels pick their
	//own ground texture, so the row follows the texture first and depth second
	public static int region(){
		String tex = Dungeon.level.tilesTex();
		if (Assets.Environment.TILES_FROZEN.equals(tex)
				|| Assets.Environment.TILES_SPIDERNEST.equals(tex)) return 0;
		if (Assets.Environment.TILES_SEWERS.equals(tex))        return 0;
		if (Assets.Environment.TILES_PRISON.equals(tex))        return 1;
		if (Assets.Environment.TILES_CAVES.equals(tex)
				|| Assets.Environment.TILES_CAVES_CRYSTAL.equals(tex)
				|| Assets.Environment.TILES_CAVES_GNOLL.equals(tex)) return 2;
		if (Assets.Environment.TILES_CITY.equals(tex))          return 3;
		if (Assets.Environment.TILES_HALLS.equals(tex))         return 4;
		//custom sheets: the overworld wants sewers green, the rest follow depth
		if (Dungeon.level instanceof OverworldLevel) return 0;
		int region = (Dungeon.depth-1)/5;
		if (Dungeon.depth == 21 && Dungeon.level instanceof LastShopLevel) region--;
		return Math.max(0, Math.min(region, 4));
	}

	@Override
	protected int getTileVisual(int pos, int tile, boolean flat) {
		
		if (flat) return -1;

		if (skipCells.contains(pos)){
			return -1;
		}

		int regionOffset = region()*4;

		if (tile == Terrain.HIGH_GRASS){
			if (DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_HIGH_GRASS, pos) == DungeonTileSheet.RAISED_HIGH_GRASS_ALT){
				return regionOffset + 2;
			} else {
				return regionOffset;
			}
		} else if (tile == Terrain.FURROWED_GRASS
				&& !DungeonTileSheet.tilledSoil( pos )){
			//tilled soil and a village's field are flat farm ground - no grass tuft on top
			if (DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_FURROWED_GRASS, pos) == DungeonTileSheet.RAISED_FURROWED_ALT){
				return regionOffset + 1 + 2;
			} else {
				return regionOffset + 1;
			}
		}
		
		return -1;
	}
}

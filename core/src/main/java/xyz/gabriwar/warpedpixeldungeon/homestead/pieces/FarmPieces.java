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

package xyz.gabriwar.warpedpixeldungeon.homestead.pieces;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCategory;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.TiePost;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.CompostBin;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;

/**
 * The fields' and the animals' pieces (BuildCatalog.PROVIDERS), in the FARMING group after the
 * core's fence and gate:
 *
 * - farm_plot: natural ground tilled into FARMLAND, free, 2 turns, over a whole area at once;
 * - channel: an irrigation channel, WATER dug into natural ground beside water (or a well, or a
 *   channel already dug), free, 4 turns, along a line; taking it down gives the ground back;
 * - well: a stone well (the villages' WELL), 8 rough stone and a rope: the farmland within two
 *   cells of it counts as watered;
 * - compost_bin: 4 planks, turns food and seeds into plant food (CompostBin);
 * - tie_post: 2 planks, a post to tie a roped animal to (TiePost).
 *
 * Watering, sowing and the rest are homestead/farming's: a piece going up or coming down waters or
 * dries the farmland round it (FarmSlice.cellChanged).
 */
public class FarmPieces implements BuildCatalog.Provider {

	@Override
	public void register(){
		BuildCatalog.register( BuildPiece.terrain( "farm_plot", BuildCategory.FARMING, Terrain.FARMLAND )
				.turns( 2 ).area().done() );
		BuildCatalog.register( BuildPiece.terrain( "channel", BuildCategory.FARMING, Terrain.WATER )
				.turns( 4 ).placement( BuildPiece.Placement.NEXT_TO_WATER ).line()
				.preview( Assets.Environment.WATER_SEWERS, 0 ).done() );
		BuildCatalog.register( BuildPiece.terrain( "well", BuildCategory.FARMING, Terrain.WELL )
				.cost( RoughStone.class, 8 ).cost( Rope.class, 1 ).turns( 6 ).hp( 100 )
				.preview( Assets.Environment.TILES_OVERWORLD, DungeonTileSheet.WELL ).done() );
		BuildCatalog.register( BuildPiece.actor( "compost_bin", BuildCategory.FARMING, CompostBin::new, HomesteadSprites.COMPOST_BIN )
				.cost( Plank.class, 4 ).turns( 4 ).hp( 30 ).flammable().done() );
		BuildCatalog.register( BuildPiece.actor( "tie_post", BuildCategory.FARMING, TiePost::new, HomesteadSprites.TIE_POST )
				.cost( Plank.class, 2 ).turns( 2 ).hp( 20 ).flammable().done() );
	}
}

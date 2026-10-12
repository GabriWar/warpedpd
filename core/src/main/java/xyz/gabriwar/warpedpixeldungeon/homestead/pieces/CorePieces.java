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

import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCategory;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.items.materials.FiredBrick;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Clay;
import xyz.gabriwar.warpedpixeldungeon.items.materials.CutStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GlassPane;
import xyz.gabriwar.warpedpixeldungeon.items.materials.IronFittings;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.tiles.RoofTiles;

/**
 * The homesteads' own pieces, first in the build menu (BuildCatalog.PROVIDERS): the four walls and
 * their windows, the plain and the barred door, the palisade, the fence and its gate, the two
 * floors, the cobbled path, the plank bridge and the five roofs. Costs are per cell; the burning
 * pieces are the wooden ones (their terrains are FLAMABLE).
 */
public class CorePieces implements BuildCatalog.Provider {

	@Override
	public void register(){
		//walls: three turns each, laid along a line or over a whole rectangle
		BuildCatalog.register( BuildPiece.terrain( "wall_log", BuildCategory.WALLS, Terrain.BUILT_WALL_LOG )
				.cost( Log.class, 2 ).turns( 3 ).hp( 40 ).flammable().line().area().done() );
		BuildCatalog.register( BuildPiece.terrain( "wall_plank", BuildCategory.WALLS, Terrain.BUILT_WALL_PLANK )
				.cost( Plank.class, 3 ).turns( 3 ).hp( 60 ).flammable().line().area().done() );
		BuildCatalog.register( BuildPiece.terrain( "wall_stone", BuildCategory.WALLS, Terrain.BUILT_WALL_STONE )
				.cost( RoughStone.class, 3 ).turns( 3 ).hp( 120 ).line().area().done() );
		BuildCatalog.register( BuildPiece.terrain( "wall_brick", BuildCategory.WALLS, Terrain.BUILT_WALL_BRICK )
				.cost( FiredBrick.class, 2 ).cost( Clay.class, 1 ).turns( 3 ).hp( 160 ).line().area().done() );

		//windows: a pane in the wall's own material; sight through, no way through
		BuildCatalog.register( BuildPiece.terrain( "window_log", BuildCategory.WALLS, Terrain.BUILT_WINDOW_LOG )
				.cost( GlassPane.class, 1 ).cost( Log.class, 1 ).turns( 3 ).hp( 20 ).line().done() );
		BuildCatalog.register( BuildPiece.terrain( "window_plank", BuildCategory.WALLS, Terrain.BUILT_WINDOW_PLANK )
				.cost( GlassPane.class, 1 ).cost( Plank.class, 1 ).turns( 3 ).hp( 20 ).line().done() );
		BuildCatalog.register( BuildPiece.terrain( "window_stone", BuildCategory.WALLS, Terrain.BUILT_WINDOW_STONE )
				.cost( GlassPane.class, 1 ).cost( RoughStone.class, 1 ).turns( 3 ).hp( 20 ).line().done() );
		BuildCatalog.register( BuildPiece.terrain( "window_brick", BuildCategory.WALLS, Terrain.BUILT_WINDOW_BRICK )
				.cost( GlassPane.class, 1 ).cost( FiredBrick.class, 1 ).turns( 3 ).hp( 20 ).line().done() );

		//doors: anyone opens the plain one; only the hero and his allies the barred one
		BuildCatalog.register( BuildPiece.terrain( "door", BuildCategory.DOORS, Terrain.BUILT_DOOR )
				.cost( Plank.class, 4 ).turns( 3 ).hp( 30 ).flammable().done() );
		BuildCatalog.register( BuildPiece.terrain( "door_barred", BuildCategory.DOORS, Terrain.BUILT_DOOR_BARRED )
				.cost( Plank.class, 4 ).cost( IronFittings.class, 2 ).turns( 3 ).hp( 60 ).flammable().done() );

		//floors and the path: no health, nothing breaks them
		BuildCatalog.register( BuildPiece.terrain( "floor_plank", BuildCategory.FLOORS, Terrain.FLOOR_PLANK )
				.cost( Plank.class, 1 ).turns( 1 ).line().area().done() );
		BuildCatalog.register( BuildPiece.terrain( "floor_stone", BuildCategory.FLOORS, Terrain.FLOOR_STONE )
				.cost( RoughStone.class, 1 ).turns( 1 ).line().area().done() );
		BuildCatalog.register( BuildPiece.terrain( "path", BuildCategory.FLOORS, Terrain.PATH_STONE )
				.cost( RoughStone.class, 1 ).turns( 1 ).line().area().done() );

		//fences: a pen's sides and its gate
		BuildCatalog.register( BuildPiece.terrain( "fence", BuildCategory.FARMING, Terrain.FENCE )
				.cost( Plank.class, 1 ).turns( 1 ).hp( 20 ).flammable().line().done() );
		BuildCatalog.register( BuildPiece.terrain( "gate", BuildCategory.FARMING, Terrain.FENCE_GATE )
				.cost( Plank.class, 3 ).cost( Rope.class, 1 ).turns( 2 ).hp( 20 ).flammable().done() );

		BuildCatalog.register( BuildPiece.terrain( "palisade", BuildCategory.DEFENCE, Terrain.PALISADE )
				.cost( Log.class, 2 ).turns( 3 ).hp( 80 ).flammable().line().done() );

		BuildCatalog.register( BuildPiece.terrain( "bridge", BuildCategory.UTILITY, Terrain.BRIDGE_PLANK )
				.cost( Plank.class, 3 ).turns( 3 ).hp( 40 ).placement( BuildPiece.Placement.WATER ).line().done() );

		//roofs, a cell at a time: over a room, a roof makes it indoors
		BuildCatalog.register( BuildPiece.roof( "roof_thatch", RoofTiles.THATCH )
				.cost( Thatch.class, 2 ).turns( 2 ).hp( 20 ).flammable().line().area().done() );
		BuildCatalog.register( BuildPiece.roof( "roof_plank", RoofTiles.PLANK )
				.cost( Plank.class, 2 ).turns( 2 ).hp( 30 ).flammable().line().area().done() );
		BuildCatalog.register( BuildPiece.roof( "roof_slate", RoofTiles.SLATE )
				.cost( CutStone.class, 1 ).turns( 2 ).hp( 60 ).line().area().done() );
		BuildCatalog.register( BuildPiece.roof( "roof_tile", RoofTiles.TILE )
				.cost( FiredBrick.class, 2 ).turns( 2 ).hp( 60 ).line().area().done() );
		BuildCatalog.register( BuildPiece.roof( "roof_glass", RoofTiles.GLASS )
				.cost( GlassPane.class, 1 ).cost( Plank.class, 1 ).turns( 2 ).hp( 20 ).line().area().done() );
	}
}

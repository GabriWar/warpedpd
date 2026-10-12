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
import xyz.gabriwar.warpedpixeldungeon.homestead.rooms.HomeBed;
import xyz.gabriwar.warpedpixeldungeon.items.materials.CutStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Leather;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;

/**
 * The rooms' furniture (BuildCatalog.PROVIDERS, after the core pieces), all in the FURNITURE group:
 *   - three beds (homestead/rooms/HomeBed), each a bedroom's: straw, wood and fur;
 *   - the decor a room's comfort counts (homestead/rooms/Comfort): a rug, a bookshelf and a statue,
 *     laid as the land's own terrains (WOOL_RUG, EMPTY_BOOKSHELF - an empty shelf, nothing to
 *     rummage, with open floor in front of it - and STATUE). The statue makes a shrine (RoomTypes).
 * The beds burn, a turn in the flames at a time (HomeBed.smoulder), as the wooden shelf does.
 */
public class RoomPieces implements BuildCatalog.Provider {

	@Override
	public void register(){
		BuildCatalog.register( BuildPiece.actor( "bed_straw", BuildCategory.FURNITURE,
				() -> HomeBed.of( HomeBed.Kind.STRAW ), HomesteadSprites.BED_STRAW )
				.cost( Thatch.class, 4 ).turns( 3 ).hp( 20 ).flammable().done() );
		BuildCatalog.register( BuildPiece.actor( "bed_wood", BuildCategory.FURNITURE,
				() -> HomeBed.of( HomeBed.Kind.WOOD ), HomesteadSprites.BED_WOOD )
				.cost( Plank.class, 6 ).cost( Thatch.class, 2 ).turns( 5 ).hp( 40 ).flammable().done() );
		BuildCatalog.register( BuildPiece.actor( "bed_fur", BuildCategory.FURNITURE,
				() -> HomeBed.of( HomeBed.Kind.FUR ), HomesteadSprites.BED_FUR )
				.cost( Plank.class, 4 ).cost( Leather.class, 3 ).turns( 5 ).hp( 40 ).flammable().done() );

		//decor: pictured as the world's own sheet draws them (BuildPiece.terrain)
		BuildCatalog.register( BuildPiece.terrain( "rug", BuildCategory.FURNITURE, Terrain.WOOL_RUG )
				.cost( Leather.class, 2 ).turns( 2 ).line().area().done() );
		//a shelf is drawn as a wall with a face: it needs open floor in front of it to show one
		BuildCatalog.register( BuildPiece.terrain( "bookshelf", BuildCategory.FURNITURE, Terrain.EMPTY_BOOKSHELF )
				.cost( Plank.class, 4 ).turns( 4 ).hp( 30 ).flammable().placement( BuildPiece.Placement.OPEN_FRONT ).line().done() );
		BuildCatalog.register( BuildPiece.terrain( "statue", BuildCategory.FURNITURE, Terrain.STATUE )
				.cost( CutStone.class, 4 ).turns( 6 ).hp( 80 ).done() );
	}
}

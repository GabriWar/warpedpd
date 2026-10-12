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
import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Hearthstone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.CutStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;

/**
 * The travel part's pieces (BuildCatalog.PROVIDERS, last): the HEARTHSTONE, which makes the place it
 * stands a home (homestead/travel/Hearthstone). Ten rough stones and two cut ones, ten turns' work,
 * 200 health, no fire takes it; at most MAX_HOMES standing at once, and the build menu asks before
 * one is taken down, since the home goes with it.
 */
public class TravelPieces implements BuildCatalog.Provider {

	/** The most homes the hero keeps at once. */
	public static final int MAX_HOMES = 3;

	@Override
	public void register(){
		BuildCatalog.register( BuildPiece.actor( "hearthstone", BuildCategory.TRAVEL, Hearthstone::new, HomesteadSprites.HEARTHSTONE )
				.cost( RoughStone.class, 10 ).cost( CutStone.class, 2 ).turns( 10 ).hp( 200 )
				.limit( new BuildPiece.Limit(){
					@Override public int max(){ return MAX_HOMES; }
					@Override public int count(){ return HomesteadWorld.homes().size(); }
				} )
				.confirmRemoval()
				.listener( new Hearthstone.Founding() )
				.done() );
	}
}

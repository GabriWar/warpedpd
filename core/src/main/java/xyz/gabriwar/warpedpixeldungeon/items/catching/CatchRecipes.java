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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.homestead.stations.CookingPot;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Hearth;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.StationRecipe;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Workbench;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.IronFittings;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the homesteads' stations make for the catching (homestead/stations/StationRecipes lists them
 * after its own):
 *
 *   workbench      a plank + a coil of rope -> a bug net (6 turns); a plank + a coil of rope + iron
 *                  fittings -> a fishing rod (10)
 *   hearth (fire)  a raw fish -> the same fish grilled (5), as any fire grills it (Heap.burn)
 *   cooking pot    a raw fish -> the same fish poached (10): a meal of the pot, cooked in a kitchen
 *                  if the pot stands in one
 */
public final class CatchRecipes {

	private CatchRecipes(){}

	/** The catching's recipes, in the order the stations list them. */
	public static List<StationRecipe> all(){
		ArrayList<StationRecipe> out = new ArrayList<>();
		out.add( StationRecipe.at( "bug_net", Workbench.class ).in( Plank.class, 1 ).in( Rope.class, 1 )
				.out( BugNet.class, 1 ).turns( 6 ).done() );
		out.add( StationRecipe.at( "fishing_rod", Workbench.class ).in( Plank.class, 1 ).in( Rope.class, 1 )
				.in( IronFittings.class, 1 ).out( FishingRod.class, 1 ).turns( 10 ).done() );
		out.add( StationRecipe.at( "grilled_fish", Hearth.class ).inAny( 1, "raw_fish", CaughtFish.class )
				.out( cooked( false ) ).named( "grilled_fish" ).turns( 5 ).heat().done() );
		out.add( StationRecipe.at( "poached_fish", CookingPot.class ).inAny( 1, "raw_fish", CaughtFish.class )
				.out( cooked( true ) ).named( "poached_fish" ).turns( 10 ).done() );
		return out;
	}

	//the fish that went in, cooked: grilled or poached, its species kept
	private static StationRecipe.Output cooked( final boolean poached ){
		return new StationRecipe.Output(){
			@Override
			public List<Item> make( List<Item> used ){
				for (Item i : used){
					if (i instanceof CaughtFish) return Collections.singletonList( ((CaughtFish) i).cooked( poached ) );
				}
				return Collections.emptyList();
			}

			@Override
			public Item sample(){
				return new CookedFish( FishSpecies.TROUT, poached );
			}
		};
	}
}

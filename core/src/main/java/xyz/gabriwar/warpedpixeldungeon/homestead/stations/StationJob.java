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

package xyz.gabriwar.warpedpixeldungeon.homestead.stations;

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * Work queued at a station: a recipe, how many batches of it are still to make, the work done on
 * the batch in hand (in turns), and everything the hero handed over for the batches still to make
 * (held: each finished batch uses up its own share, StationRecipe.useBatch). Saved with its
 * station; a job whose recipe no longer exists gives back what it holds (HomesteadStation).
 */
public class StationJob implements Bundlable {

	public String recipe;
	public int batches;
	public float progress;
	public final ArrayList<Item> held = new ArrayList<>();

	/** For the bundle. */
	public StationJob(){}

	public StationJob( String recipe, int batches, ArrayList<Item> held ){
		this.recipe = recipe;
		this.batches = batches;
		this.held.addAll( held );
	}

	/** Its recipe, or null when it no longer exists. */
	public StationRecipe recipe(){
		return StationRecipes.byId( recipe );
	}

	/** How far the batch in hand is, 0..1. */
	public float fraction(){
		StationRecipe r = recipe();
		return r == null ? 0f : Math.max( 0f, Math.min( 1f, progress / r.turns ) );
	}

	private static final String RECIPE   = "recipe";
	private static final String BATCHES  = "batches";
	private static final String PROGRESS = "progress";
	private static final String HELD     = "held";

	@Override
	public void storeInBundle( Bundle bundle ){
		bundle.put( RECIPE, recipe );
		bundle.put( BATCHES, batches );
		bundle.put( PROGRESS, progress );
		bundle.put( HELD, held );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		recipe = bundle.getString( RECIPE );
		batches = bundle.getInt( BATCHES );
		progress = bundle.getFloat( PROGRESS );
		held.clear();
		for (Bundlable b : bundle.getCollection( HELD )){
			if (b instanceof Item) held.add( (Item) b );
		}
	}
}

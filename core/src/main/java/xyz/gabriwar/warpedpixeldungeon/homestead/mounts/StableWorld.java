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

package xyz.gabriwar.warpedpixeldungeon.homestead.mounts;

import xyz.gabriwar.warpedpixeldungeon.homestead.WorldSection;
import com.watabou.utils.Bundle;

import java.util.HashSet;

/**
 * The horses' run-wide record (HomesteadWorld's stables section): which villages have had their
 * stable set up (Stables), by the settlement's sector key. Once only, as with the carpenter: a
 * stablehand driven off is gone for good, and a village the hero knew before the stables gets one
 * the next time it is in view.
 */
public class StableWorld extends WorldSection {

	private final HashSet<Long> stables = new HashSet<>();

	public StableWorld(){
	}

	public synchronized boolean has( long settlement ){
		return stables.contains( settlement );
	}

	public synchronized void set( long settlement ){
		stables.add( settlement );
	}

	private static final String STABLES = "stables";

	@Override
	public synchronized void storeInBundle( Bundle bundle ){
		long[] keys = new long[stables.size()];
		int i = 0;
		for (long k : stables) keys[i++] = k;
		bundle.put( STABLES, keys );
	}

	@Override
	public synchronized void restoreFromBundle( Bundle bundle ){
		stables.clear();
		if (bundle.contains( STABLES )){
			for (long k : bundle.getLongArray( STABLES )) stables.add( k );
		}
	}
}

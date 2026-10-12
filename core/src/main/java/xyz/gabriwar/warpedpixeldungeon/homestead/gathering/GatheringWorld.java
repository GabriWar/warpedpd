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

package xyz.gabriwar.warpedpixeldungeon.homestead.gathering;

import xyz.gabriwar.warpedpixeldungeon.homestead.WorldSection;
import com.watabou.utils.Bundle;

import java.util.HashSet;

/**
 * The gathering's run-wide record (HomesteadWorld's gathering section): which villages have had
 * their carpenter set up (Carpentry), by the settlement's sector key. Once only: a carpenter
 * driven off is gone for good, as any of a village's traders is, and a village the hero knew
 * before the homesteads gets its carpenter the next time it is in view.
 */
public class GatheringWorld extends WorldSection {

	private final HashSet<Long> carpenters = new HashSet<>();

	public GatheringWorld(){
	}

	public synchronized boolean hasCarpenter( long settlement ){
		return carpenters.contains( settlement );
	}

	public synchronized void carpenterSet( long settlement ){
		carpenters.add( settlement );
	}

	private static final String CARPENTERS = "carpenters";

	@Override
	public synchronized void storeInBundle( Bundle bundle ){
		long[] keys = new long[carpenters.size()];
		int i = 0;
		for (long k : carpenters) keys[i++] = k;
		bundle.put( CARPENTERS, keys );
	}

	@Override
	public synchronized void restoreFromBundle( Bundle bundle ){
		carpenters.clear();
		if (bundle.contains( CARPENTERS )){
			for (long k : bundle.getLongArray( CARPENTERS )) carpenters.add( k );
		}
	}
}

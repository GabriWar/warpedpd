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

import xyz.gabriwar.warpedpixeldungeon.homestead.travel.MapMarks;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.List;

/**
 * The world map's pins on the caches the bottles' notes mark (BottleCaches, loaded by this name in
 * MapMarks.PROVIDERS): an amber dot on each, named, until it is dug up. They all lie on the surface;
 * seen from another slice they are faded and say so. The host's record (CatchWorld): a co-op guest's
 * map shows none.
 */
public class BottleMarks implements MapMarks.Provider {

	/** The amber of a cache's mark. */
	public static final int COLOR = 0xF0B040;

	@Override
	public void marks( int depth, List<MapMarks.Mark> out ){
		CatchWorld world = CatchWorld.get();
		if (world == null) return;
		boolean here = depth == WorldLayers.SURFACE_DEPTH;
		String name = Messages.get( this, here ? "mark" : "mark_surface" );
		for (int[] c : world.caches()){
			out.add( new MapMarks.Mark( MapMarks.Icon.DOT, WorldLayers.SURFACE_DEPTH, c[0], c[1], COLOR, name, true,
					here ? 1f : MapMarks.OTHER_SLICE, false ) );
		}
	}
}

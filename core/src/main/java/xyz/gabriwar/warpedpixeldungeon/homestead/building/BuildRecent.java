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

package xyz.gabriwar.warpedpixeldungeon.homestead.building;

import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;

import java.util.ArrayList;
import java.util.List;

/**
 * The build menu's "Recent" group: the ids of the last pieces built this run, newest first, at
 * most {@link #MAX}. The Builder's Tool keeps the list (BuildersTool.recent) and saves it with
 * itself, so it lasts as long as the run's tool does.
 */
public final class BuildRecent {

	/** How many pieces the list keeps; keys 1 to 8 pick them on the build bar. */
	public static final int MAX = 8;

	private BuildRecent(){}

	/** A piece was built: its id goes to the front (out of wherever it was), the oldest falls off. */
	public static void use( List<String> ids, String id ){
		if (id == null) return;
		ids.remove( id );
		ids.add( 0, id );
		while (ids.size() > MAX) ids.remove( ids.size() - 1 );
	}

	/** The pieces of the list, in its order, leaving out any id no piece has any more. */
	public static List<BuildPiece> pieces( List<String> ids ){
		ArrayList<BuildPiece> out = new ArrayList<>();
		for (String id : ids){
			BuildPiece p = BuildCatalog.byId( id );
			if (p != null) out.add( p );
		}
		return out;
	}
}

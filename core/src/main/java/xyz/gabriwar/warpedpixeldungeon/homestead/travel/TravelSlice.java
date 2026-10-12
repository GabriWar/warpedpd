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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.StoreSection;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import com.watabou.utils.Bundle;

import java.util.HashSet;

/**
 * The waystones of a slice (the surface's: no other slice has any): which have been put down. Each
 * window laid on the surface (materialize: built, read back, slid, jumped to, re-seasoned) puts down
 * the town's and every big settlement's waystone (Waystones) whose well lies inside it and that is
 * not down yet - exactly once each, ever: a waystone is an NPC, so from then on the slice keeps it,
 * parking it with the window and putting it back like the settlements' people. Saved with the
 * slice, so the record and the stones themselves are always saved together.
 */
public class TravelSlice extends StoreSection {

	//the keys (Waystones.key) of the waystones put down on this slice
	private final HashSet<Long> placed = new HashSet<>();

	@Override
	public void materialize( OverworldLevel level ){
		if (level.altitude() != 0 || level.isMirror()) return;
		long seed = level.worldSeed;
		//a stone already standing (a save that knew it before this record did) is counted, not doubled
		for (Mob m : level.mobs) if (m instanceof Waystone) placed.add( ((Waystone) m).key );

		if (!placed.contains( Waystones.TOWN )){
			int c = Waystones.townCell( level );
			if (c != -1) put( level, Waystones.TOWN, c, Waystones.townWX(), Waystones.townWY() );
		}

		int w = level.width(), h = level.height();
		int sx0 = Math.floorDiv( level.worldX, WorldStructures.SECTOR ) - 1;
		int sy0 = Math.floorDiv( level.worldY, WorldStructures.SECTOR ) - 1;
		int sx1 = Math.floorDiv( level.worldX + w, WorldStructures.SECTOR ) + 1;
		int sy1 = Math.floorDiv( level.worldY + h, WorldStructures.SECTOR ) + 1;
		for (int sy = sy0; sy <= sy1; sy++){
			for (int sx = sx0; sx <= sx1; sx++){
				long key = WorldStructures.sectorOf( sx, sy );
				if (placed.contains( key ) || !Waystones.keepsOne( seed, sx, sy )) continue;
				//its well inside the window, as a settlement's folk are put down
				int cx = WorldStructures.siteX( seed, sx, sy ) - level.worldX;
				int cy = WorldStructures.siteY( seed, sx, sy ) - level.worldY;
				if (cx < 2 || cy < 2 || cx >= w - 2 || cy >= h - 2) continue;
				int c = Waystones.settlementCell( level, sx, sy );
				if (c != -1) put( level, key, c, level.worldX + c % w, level.worldY + c / w );
			}
		}
	}

	private void put( OverworldLevel level, long key, int cell, int wx, int wy ){
		Waystone stone = new Waystone();
		stone.key = key;
		stone.wx = wx;
		stone.wy = wy;
		stone.place = Waystones.placeName( level.worldSeed, key );
		Waystones.put( level, stone, cell );
		placed.add( key );
		TravelWorld tw = TravelWorld.get();
		if (tw != null) tw.placed( key, wx, wy );
	}

	/** Has this slice put down the waystone of a key? */
	public boolean placed( long key ){
		return placed.contains( key );
	}

	private static final String PLACED = "placed";

	@Override
	public void storeInBundle( Bundle bundle ){
		long[] keys = new long[placed.size()];
		int i = 0;
		for (long k : placed) keys[i++] = k;
		bundle.put( PLACED, keys );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		placed.clear();
		if (bundle.contains( PLACED )) for (long k : bundle.getLongArray( PLACED )) placed.add( k );
	}
}

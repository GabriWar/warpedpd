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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.BreakersBench;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SpecialRoom;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import com.watabou.utils.Point;

/**
 * The Breaker's Bench: a cramped salvage shop. The bench stands against the back wall
 * with tool racks either side of it and heaps of scrap where earlier work was swept.
 * Everything that matters here is the bench itself (BreakersBench).
 */
public class BreakersBenchRoom extends SpecialRoom {

	@Override
	public int maxWidth(){ return 8; }
	@Override
	public int maxHeight(){ return 8; }

	@Override
	public void paint( Level level ){
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY_SP );

		RoomFrame f = new RoomFrame( this, entrance() );
		int mid = f.span / 2;
		int back = f.depth - 1;

		BreakersBench bench = new BreakersBench();
		bench.pos = f.cell( level, back, mid );
		level.mobs.add( bench );

		for (int s = 0; s < f.span; s++){
			if (s == mid) continue;
			int cell = f.cell( level, back, s );
			Painter.set( level, cell, Terrain.CUSTOM_DECO );
			WarpedRoomTiles.place( level, cell, WarpedRoomTiles.TOOL_RACK, "tool_rack" );
		}

		//swept-up scrap in the near corners, never where it would block the way in
		for (int s : new int[]{ 0, f.span - 1 }){
			if (s == f.doorSide) continue;
			int cell = f.cell( level, 0, s );
			Painter.set( level, cell, Terrain.CUSTOM_DECO_EMPTY );
			WarpedRoomTiles.place( level, cell, WarpedRoomTiles.SCRAP, "scrap" );
		}

		entrance().set( Door.Type.REGULAR );
	}

	@Override
	public boolean canPlaceWater( Point p ){
		return false;
	}

	@Override
	public boolean canPlaceGrass( Point p ){
		return false;
	}
}

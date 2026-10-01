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

import xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret.SecretRoom;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import com.watabou.utils.Point;

/**
 * The Undercroft: a cellar behind a hidden door where, after dark, a dealer sells what
 * the lawful shops may not. Found by day it is only a room somebody uses - stalls with
 * nothing on them, a rug, a log to sit on - and says nothing about when to come back.
 * Nothing burns down here: the lanterns by the door are the only light.
 *
 * The room paints the furniture and hands the market's ledger (the BlackMarket blob)
 * three cells: where the dealer stands, and where his two guards stand. The blob does
 * the rest, every night, for as long as the floor exists.
 */
public class BlackMarketRoom extends SecretRoom {

	@Override
	public int minWidth(){ return 9; }
	@Override
	public int maxWidth(){ return 11; }
	@Override
	public int minHeight(){ return 9; }
	@Override
	public int maxHeight(){ return 11; }

	@Override
	public void paint( Level level ){
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY_SP );

		RoomFrame f = new RoomFrame( this, entrance() );
		int mid = f.span / 2;
		int back = f.depth - 1;

		//the stalls along the back wall: a table under a cloth in the middle, the rest
		//of the run stacked with whatever came down the road
		for (int s = 0; s < f.span; s++){
			int art;
			String key;
			if (Math.abs( s - mid ) <= 1)      { art = WarpedRoomTiles.STALL;  key = "stall"; }
			else if (s == 0 || s == f.span - 1){ art = WarpedRoomTiles.CRATES; key = "crates"; }
			else if (s < mid)                  { art = WarpedRoomTiles.BARREL; key = "barrel"; }
			else                               { art = WarpedRoomTiles.SACKS;  key = "sacks"; }
			solid( level, f, back, s, art, key );
		}
		solid( level, f, back - 1, 0, WarpedRoomTiles.CRATES, "crates" );
		solid( level, f, back - 1, f.span - 1, WarpedRoomTiles.SACKS, "sacks" );

		//the dealer's rug, three by three, in front of the table. the picture is laid by
		//map position, not door position, so the pattern always reads the right way up
		int rugFront = back - 4;
		Point a = f.at( rugFront, mid - 1 ), b = f.at( rugFront + 2, mid + 1 );
		int rx = Math.min( a.x, b.x ), ry = Math.min( a.y, b.y );
		for (int x = 0; x < 3; x++){
			for (int y = 0; y < 3; y++){
				int cell = level.pointToCell( new Point( rx + x, ry + y ) );
				Painter.set( level, cell, Terrain.CUSTOM_DECO_EMPTY );
				WarpedRoomTiles.place( level, cell, WarpedRoomTiles.RUG + x + y * 3, "rug" );
			}
		}

		//lanterns in the corners by the way in
		solid( level, f, 0, 0, WarpedRoomTiles.LANTERN, "lantern" );
		solid( level, f, 0, f.span - 1, WarpedRoomTiles.LANTERN, "lantern" );

		//a log to sit on, on whichever side the door is not. There is no fire in here:
		//a cellar full of contraband is no place for one
		int seatSide = f.doorSide <= mid ? f.span - 2 : 1;
		int seat = f.cell( level, 2, seatSide );
		Painter.set( level, seat, Terrain.CUSTOM_DECO_EMPTY );
		WarpedRoomTiles.place( level, seat, WarpedRoomTiles.LOG_SEAT, "log_seat" );

		//the ledger is anchored on the log: a blob needs a cell, and this one is only a
		//marker - nothing is drawn or done on it
		BlackMarket market = Blob.seed( seat, 1, BlackMarket.class, level );
		market.home = f.cell( level, back - 1, mid );
		market.posts = new int[]{
				f.cell( level, rugFront + 1, mid - 2 ),
				f.cell( level, rugFront + 1, mid + 2 )
		};

		entrance().set( Door.Type.HIDDEN );
	}

	//a piece of furniture nobody can walk through - never on the two cells in front of
	//the door, where it would wall the room off
	private void solid( Level level, RoomFrame f, int forward, int side, int art, String key ){
		if (forward <= 1 && side == f.doorSide) return;
		int cell = f.cell( level, forward, side );
		Painter.set( level, cell, Terrain.CUSTOM_DECO );
		WarpedRoomTiles.place( level, cell, art, key );
	}
}

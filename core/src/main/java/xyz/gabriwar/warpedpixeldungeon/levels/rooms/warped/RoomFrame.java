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

import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import com.watabou.utils.Point;

/**
 * A room seen from its own door. A layout that has a front and a back (a dealer facing
 * the way in, a vault at the far end, wind blowing away from the door) would otherwise
 * be written four times, once per wall the door can land on. Here it is written once in
 * door coordinates and this turns them into map cells:
 *
 *   forward  0 is the interior row just inside the door's wall, depth-1 the far row
 *   side     0 .. span-1 runs along that wall
 *
 * so {@code at( depth-1, span/2 )} is always "the middle of the back wall".
 */
public class RoomFrame {

	private final Room room;
	private final int wall;   //which wall holds the door: Room.LEFT, TOP, RIGHT or BOTTOM

	/** interior cells from the door's wall to the opposite one */
	public final int depth;
	/** interior cells along the door's wall */
	public final int span;
	/** the door's own place along that wall, in side coordinates */
	public final int doorSide;

	public RoomFrame( Room room, Point door ){
		this.room = room;
		if (door == null || door.x == room.left)  wall = Room.LEFT;
		else if (door.x == room.right)            wall = Room.RIGHT;
		else if (door.y == room.top)              wall = Room.TOP;
		else                                      wall = Room.BOTTOM;

		boolean horizontal = wall == Room.LEFT || wall == Room.RIGHT;
		depth = (horizontal ? room.width() : room.height()) - 2;
		span  = (horizontal ? room.height() : room.width()) - 2;
		if (door == null)      doorSide = span / 2;
		else if (horizontal)   doorSide = door.y - (room.top + 1);
		else                   doorSide = door.x - (room.left + 1);
	}

	public Point at( int forward, int side ){
		switch (wall){
			case Room.LEFT:  default: return new Point( room.left + 1 + forward, room.top + 1 + side );
			case Room.RIGHT:          return new Point( room.right - 1 - forward, room.top + 1 + side );
			case Room.TOP:            return new Point( room.left + 1 + side, room.top + 1 + forward );
			case Room.BOTTOM:         return new Point( room.left + 1 + side, room.bottom - 1 - forward );
		}
	}

	public int cell( Level level, int forward, int side ){
		return level.pointToCell( at( forward, side ) );
	}

	/** true when the door is a vertical slot in a side wall, so "forward" runs along x */
	public boolean horizontal(){
		return wall == Room.LEFT || wall == Room.RIGHT;
	}

	/** one step forward, as a map offset */
	public int forwardStep( Level level ){
		switch (wall){
			case Room.LEFT:  default: return +1;
			case Room.RIGHT:          return -1;
			case Room.TOP:            return +level.width();
			case Room.BOTTOM:         return -level.width();
		}
	}

	/** the forward direction as 0 east, 1 south, 2 west, 3 north */
	public int heading(){
		switch (wall){
			case Room.LEFT:  default: return 0;
			case Room.TOP:            return 1;
			case Room.RIGHT:          return 2;
			case Room.BOTTOM:         return 3;
		}
	}

	/** keeps a side coordinate inside the room */
	public int clampSide( int side ){
		return Math.max( 0, Math.min( span - 1, side ) );
	}
}

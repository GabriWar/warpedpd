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

package xyz.gabriwar.warpedpixeldungeon.homestead.rooms;

import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.homestead.Rooms;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.Locale;

/**
 * How COMFORTABLE a room is, 0..MAX, from five things:
 *   - SIZE: a point for every CELLS_PER_POINT cells of it, SIZE_MAX at most;
 *   - WALLS: the average of its walls' material - log 1, plank 2, rock 2, stone 3, brick 4 (its
 *     windows and doors left out);
 *   - FLOORS: how much of its floor is laid (built floors, rugs), FLOOR_MAX for all of it;
 *   - DECOR: a point for each rug, bookshelf, statue and decoration in it, DECOR_MAX at most;
 *   - HEAT: HEAT_POINTS while a fire warms it (Rooms.Room.heated).
 * Read as a word (Level: bare, plain, cosy, comfortable, fine). A bed's night heals more, and the
 * Well Rested it leaves lasts longer, the higher it is (HomeBed, WellRested).
 */
public final class Comfort {

	private Comfort(){}

	public static final int CELLS_PER_POINT = 6, SIZE_MAX = 4, WALL_MAX = 4, FLOOR_MAX = 4, DECOR_MAX = 5, HEAT_POINTS = 3;
	/** The most comfort a room can have. */
	public static final int MAX = SIZE_MAX + WALL_MAX + FLOOR_MAX + DECOR_MAX + HEAT_POINTS;

	/** The words for it, each from its least score. */
	public enum Level {
		BARE( 0 ), PLAIN( 4 ), COSY( 8 ), COMFORTABLE( 12 ), FINE( 16 );

		public final int from;

		Level( int from ){
			this.from = from;
		}

		public String title(){
			return Messages.get( Comfort.class, name().toLowerCase( Locale.ENGLISH ) );
		}
	}

	/** A room's comfort now, 0..MAX. */
	public static int score( OverworldLevel level, Rooms.Room room ){
		return score( level, room, RoomTypes.Contents.of( level, room ) );
	}

	//with what it holds already counted (RoomKinds counts once for the types and the comfort)
	static int score( OverworldLevel level, Rooms.Room room, RoomTypes.Contents contents ){
		return size( room ) + walls( level, room ) + floors( level, room ) + decor( contents ) + heat( room );
	}

	/** The word for a score. */
	public static Level levelOf( int score ){
		Level out = Level.BARE;
		for (Level l : Level.values()) if (score >= l.from) out = l;
		return out;
	}

	/** Comfort's share of the most there is, 0..1. */
	public static float fraction( int score ){
		return Math.max( 0, Math.min( MAX, score ) ) / (float) MAX;
	}

	static int size( Rooms.Room room ){
		return Math.min( SIZE_MAX, room.size() / CELLS_PER_POINT );
	}

	static int walls( OverworldLevel level, Rooms.Room room ){
		int sum = 0, n = 0;
		for (int cell : room.walls){
			int t = level.map[cell];
			if (BuiltTerrain.wall( t )){
				//HomesteadTiles.LOG..BRICK: 0..3
				sum += BuiltTerrain.material( t ) + 1;
				n++;
			} else if (t == Terrain.WALL || t == Terrain.WALL_DECO || t == Terrain.MINE_CRYSTAL){
				sum += 2;
				n++;
			}
		}
		return n == 0 ? 0 : Math.round( sum / (float) n );
	}

	static int floors( OverworldLevel level, Rooms.Room room ){
		int laid = 0, open = 0;
		for (int cell : room.cells){
			int t = level.map[cell];
			//the furniture standing in it is no floor
			if (Rooms.furnishing( t )) continue;
			open++;
			if (BuiltTerrain.floor( t ) || t == Terrain.WOOL_RUG) laid++;
		}
		return open == 0 ? 0 : Math.round( FLOOR_MAX * laid / (float) open );
	}

	static int decor( RoomTypes.Contents contents ){
		return Math.min( DECOR_MAX, contents.decor );
	}

	static int heat( Rooms.Room room ){
		return room.heated() ? HEAT_POINTS : 0;
	}
}

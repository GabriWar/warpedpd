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


package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.tiles.RoofTiles;
import com.watabou.utils.Random;

/**
 * Shelter on a cell of a world slice: what a room (Rooms) and a roof keep off whoever stands
 * there. The climate itself (ClimateManager) stays the open air's - the sky, the rain falling on
 * screen, the wind in the trees - and every reader of it that acts on one cell asks here:
 *   - INSIDE A ROOM: no wind, nothing falls (no Drenched, no soaked shoes, no Windswept, no
 *     weather cloud - snow, storm, mist - laid in it), and the air is the room's (Rooms.Room.air:
 *     drawn toward Rooms.SHELTER_TEMP by its walls' insulation, plus its heat). A heated room (any
 *     heat) makes the hero Comfy, as an inn's room does.
 *   - UNDER A ROOF, in no room: nothing falls (and no weather cloud is laid there), but a thatch
 *     roof lets one drop in THATCH_LEAK through; the wind blows as outside.
 * Nothing grows by itself on a built cell, under a roof or in a room (growthBarred), and no wild
 * animal is set down in a room, under a roof or near a home's anchor (wildlifeBarred).
 *
 * Off the world's slices every answer is the open air's: nothing is built there.
 */
public final class Shelter {

	private Shelter(){}

	/** One drop in this many gets through a thatch roof. */
	public static final int THATCH_LEAK = 5;

	/** In a room, or under any roof: nothing falls here, nothing grows here by itself. */
	public static boolean covered( Level level, int cell ){
		return Homestead.roofAt( level, cell ) != -1 || Rooms.at( level, cell ) != null;
	}

	/** The air on a cell with the open air at `outside` (C): a room's own inside one. */
	public static float air( Level level, int cell, float outside ){
		Rooms.Room room = Rooms.at( level, cell );
		return room == null ? outside : room.air( outside );
	}

	/** Does the wind reach this cell (no room round it)? */
	public static boolean windReaches( Level level, int cell ){
		return Rooms.at( level, cell ) == null;
	}

	/**
	 * Does what is falling reach this cell now: never in a room, never under a roof but thatch, one
	 * time in THATCH_LEAK under thatch (a roll each time it is asked), always in the open.
	 */
	public static boolean precipitationReaches( Level level, int cell ){
		if (!(level instanceof OverworldLevel)) return true;
		if (Rooms.at( level, cell ) != null) return false;
		int roof = Homestead.roofAt( level, cell );
		if (roof == -1) return true;
		return roof == RoofTiles.THATCH && Random.Int( THATCH_LEAK ) == 0;
	}

	/** Is the cell in a room a fire warms (Rooms.Room.heated)? */
	public static boolean heated( Level level, int cell ){
		Rooms.Room room = Rooms.at( level, cell );
		return room != null && room.heated();
	}

	/** Nothing grows here by itself (PlantGrowthManager): a built cell, a roofed one, a room's. */
	public static boolean growthBarred( Level level, int cell ){
		return level instanceof OverworldLevel && (Terrain.homestead( level.map[cell] ) || covered( level, cell ));
	}

	/** No wild animal is set down here (OverworldLevel.populateFauna, randomRespawnCell): in a room,
	 *  under a roof, or within HomeAnchors.RADIUS of a home's anchor. */
	public static boolean wildlifeBarred( Level level, int cell ){
		return level instanceof OverworldLevel
				&& (covered( level, cell ) || HomeAnchors.near( (OverworldLevel) level, cell ));
	}
}

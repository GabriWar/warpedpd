/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.levels;

public class Terrain {

	public static final int CHASM			= 0;
	public static final int EMPTY			= 1;
	public static final int GRASS			= 2;
	public static final int EMPTY_WELL		= 3;
	public static final int WALL			= 4;
	public static final int DOOR			= 5;
	public static final int OPEN_DOOR		= 6;
	public static final int ENTRANCE		= 7;
	public static final int ENTRANCE_SP		= 37;
	public static final int EXIT			= 8;
	public static final int EMBERS			= 9;
	public static final int LOCKED_DOOR		= 10;
	public static final int HERO_LKD_DR     = 38; //a door that was locked by the skeleton key
	public static final int CRYSTAL_DOOR	= 31;
	public static final int PEDESTAL		= 11;
	public static final int WALL_DECO		= 12;
	public static final int BARRICADE		= 13;
	public static final int EMPTY_SP		= 14;
	public static final int HIGH_GRASS		= 15;
	public static final int FURROWED_GRASS	= 30;
	//overworld: open water too deep to wade into - only the shore band is walkable
	public static final int DEEP_WATER      = 88;
	//overworld: planks carrying a road over a river
	public static final int BRIDGE          = 89;
	//overworld: a road signpost pointing at the nearby settlements
	public static final int SIGN            = 90;
	//overworld: solid ground under the town's hand-painted buildings - no
	//terrain art of its own, so nothing pokes through the authored layers
	public static final int TOWN_SOLID      = 91;

	public static final int SECRET_DOOR	    = 16;
	public static final int SECRET_TRAP     = 17;
	public static final int TRAP            = 18;
	public static final int INACTIVE_TRAP   = 19;

	public static final int EMPTY_DECO		= 20;
	public static final int LOCKED_EXIT		= 21;
	public static final int UNLOCKED_EXIT	= 22;
	public static final int WELL			= 24;
	public static final int BOOKSHELF		= 27;
	public static final int ALCHEMY			= 28;

	public static final int CUSTOM_DECO_EMPTY = 32; //regular empty tile that can't be overridden, used for custom visuals mainly
	//solid environment decorations
	public static final int CUSTOM_DECO	    = 23; //invisible decoration that will also be a custom visual, re-uses the old terrain ID for signs
	public static final int STATUE			= 25;
	public static final int STATUE_SP		= 26;
	//These decorations are environment-specific
	public static final int REGION_DECO		= 33;
	public static final int REGION_DECO_ALT = 34; //alt visual for region deco, sometimes SP, sometimes other
	public static final int MINE_CRYSTAL    = 35;
	public static final int MINE_BOULDER    = 36;

	public static final int WATER		    = 29;
	public static final int FROZEN_WATER    = 39; // ice: walkable, not liquid, slippery

	// Sprouted: vegetation
	public static final int SHRUB                 = 47;

	// Sprouted: Sokoban puzzle terrain types
	public static final int FLEECING_TRAP         = 65;
	public static final int WOOL_RUG              = 66;
	public static final int SOKOBAN_SHEEP         = 67;
	public static final int CORNER_SOKOBAN_SHEEP  = 68;
	public static final int SWITCH_SOKOBAN_SHEEP  = 69;
	public static final int CHANGE_SHEEP_TRAP     = 70;
	public static final int SOKOBAN_ITEM_REVEAL   = 71;
	public static final int SOKOBAN_HEAP          = 72;
	public static final int BLACK_SOKOBAN_SHEEP   = 73;
	public static final int PORT_WELL             = 74;
	public static final int SOKOBAN_PORT_SWITCH   = 75;

	// Overgrown: soil terrain types (render as HIGH_GRASS)
	public static final int SOIL_CORNWHEAT        = 76;
	public static final int SOIL_GREENWHEAT       = 77;
	public static final int SOIL_STRAWWHEAT       = 78;
	public static final int SOIL_WATERWHEAT       = 79;

	// Overgrown: empty bookshelf (looted bookshelf)
	public static final int EMPTY_BOOKSHELF       = 80;

	// Warped: overworld snowfield ground
	public static final int SNOW                  = 81;
	// Warped: overworld packed-dirt road/plaza
	public static final int DIRT_PATH             = 82;
	// Warped: overworld decoration terrains (features-layer overlays)
	public static final int TREE_PINE             = 83;
	public static final int TREE_OAK              = 84;
	public static final int BOULDER               = 85;
	public static final int FLOWER_PATCH          = 86;
	public static final int MUSHROOM_PATCH        = 87;

	public static final int PASSABLE		= 0x01;
	public static final int LOS_BLOCKING	= 0x02;
	public static final int FLAMABLE		= 0x04;
	public static final int SECRET			= 0x08;
	public static final int SOLID			= 0x10;
	public static final int AVOID			= 0x20;
	public static final int LIQUID			= 0x40;
	public static final int PIT				= 0x80;
	
	public static final int[] flags = new int[256];
	static {
		flags[CHASM]		= AVOID	| PIT;
		flags[EMPTY]		= PASSABLE;
		flags[GRASS]		= PASSABLE | FLAMABLE;
		flags[EMPTY_WELL]	= PASSABLE;
		flags[WATER]		= PASSABLE | LIQUID;
		flags[DEEP_WATER]	= SOLID | LIQUID;
		flags[BRIDGE]		= PASSABLE;
		flags[SIGN]			= SOLID;
		flags[TOWN_SOLID]	= SOLID;
		flags[FROZEN_WATER] = PASSABLE; // walkable ice, not liquid
		flags[WALL]			= LOS_BLOCKING | SOLID;
		flags[DOOR]			= PASSABLE | LOS_BLOCKING | FLAMABLE | SOLID;
		flags[OPEN_DOOR]	= PASSABLE | FLAMABLE;
		flags[ENTRANCE]		= PASSABLE;
		flags[ENTRANCE_SP]	= flags[ENTRANCE];
		flags[EXIT]			= PASSABLE;
		flags[EMBERS]		= PASSABLE;
		flags[LOCKED_DOOR]	= LOS_BLOCKING | SOLID;
		flags[HERO_LKD_DR]  = flags[LOCKED_DOOR];
		flags[CRYSTAL_DOOR]	= SOLID;
		flags[PEDESTAL]		= PASSABLE;
		flags[WALL_DECO]	= flags[WALL];
		flags[BARRICADE]	= FLAMABLE | SOLID | LOS_BLOCKING;
		flags[EMPTY_SP]		= flags[EMPTY];
		flags[HIGH_GRASS]	= PASSABLE | LOS_BLOCKING | FLAMABLE;
		flags[FURROWED_GRASS]= flags[HIGH_GRASS];
		flags[SNOW]          = PASSABLE;
		flags[DIRT_PATH]     = PASSABLE;
		flags[TREE_PINE]     = FLAMABLE | SOLID | LOS_BLOCKING;
		flags[TREE_OAK]      = FLAMABLE | SOLID | LOS_BLOCKING;
		flags[BOULDER]       = SOLID;
		flags[FLOWER_PATCH]  = PASSABLE | FLAMABLE;
		flags[MUSHROOM_PATCH]= PASSABLE | FLAMABLE;

		flags[SECRET_DOOR]  = flags[WALL]  | SECRET;
		flags[SECRET_TRAP]  = flags[EMPTY] | SECRET;
		flags[TRAP]         = AVOID;
		flags[INACTIVE_TRAP]= flags[EMPTY];

		flags[EMPTY_DECO]	= flags[EMPTY];
		flags[LOCKED_EXIT]	= SOLID;
		flags[UNLOCKED_EXIT]= PASSABLE;
		flags[WELL]			= AVOID;
		flags[BOOKSHELF]	= flags[BARRICADE];
		flags[ALCHEMY]		= SOLID;

		flags[CUSTOM_DECO_EMPTY] = flags[EMPTY];
		flags[CUSTOM_DECO] = SOLID;
		flags[STATUE] = SOLID;
		flags[STATUE_SP] = flags[STATUE];

		flags[REGION_DECO] = flags[STATUE];
		flags[REGION_DECO_ALT] = flags[STATUE_SP];
		flags[MINE_CRYSTAL] = SOLID;
		flags[MINE_BOULDER] = SOLID;

		//solid but see-through: you can't walk through a bush, but you can
		//see over it - trees are the sight blockers
		flags[SHRUB]                = FLAMABLE | SOLID;

		// Sprouted: Sokoban terrain flags
		flags[FLEECING_TRAP]        = AVOID;
		flags[WOOL_RUG]             = PASSABLE;
		flags[SOKOBAN_SHEEP]        = PASSABLE;
		flags[CORNER_SOKOBAN_SHEEP] = PASSABLE;
		flags[SWITCH_SOKOBAN_SHEEP] = PASSABLE;
		flags[CHANGE_SHEEP_TRAP]    = PASSABLE;
		flags[SOKOBAN_ITEM_REVEAL]  = PASSABLE;
		flags[SOKOBAN_HEAP]         = PASSABLE;
		flags[BLACK_SOKOBAN_SHEEP]  = PASSABLE;
		flags[PORT_WELL]            = PASSABLE;
		flags[SOKOBAN_PORT_SWITCH]  = PASSABLE;

		// Overgrown: soil terrain flags (same as HIGH_GRASS)
		flags[SOIL_CORNWHEAT]   = PASSABLE | LOS_BLOCKING | FLAMABLE;
		flags[SOIL_GREENWHEAT]  = PASSABLE | LOS_BLOCKING | FLAMABLE;
		flags[SOIL_STRAWWHEAT]  = PASSABLE | LOS_BLOCKING | FLAMABLE;
		flags[SOIL_WATERWHEAT]  = PASSABLE | LOS_BLOCKING | FLAMABLE;

		// Overgrown: empty bookshelf has same flags as bookshelf
		flags[EMPTY_BOOKSHELF]  = flags[BARRICADE];

	}

	public static int discover( int terr ) {
		switch (terr) {
		case SECRET_DOOR:
			return DOOR;
		case SECRET_TRAP:
			return TRAP;
		default:
			return terr;
		}
	}

}

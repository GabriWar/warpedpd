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

package xyz.gabriwar.warpedpixeldungeon.levels.ambience;

/**
 * Whose ambience a floor has: which small life lives on it (DungeonLife) and what it sounds
 * like (AmbientSounds). A level names its own (Level.ambience()); boss floors, arenas and
 * interiors name none and stay quiet. Chosen by the level's class, never by its depth number:
 * the barrows, the side branches and the postgame reuse the region classes at other depths.
 */
public enum Place {
	/** the sewers, floors 1-4: murky water, drain pipes, moss */
	SEWERS,
	/** the prison, 6-9: cells, torches, blood stains, cages over chasms */
	PRISON,
	/** the caves, 11-14 (and the blacksmith's mines): ore veins, cold water, glowing moss */
	CAVES,
	/** the dwarven city, 16-19: libraries, statues, smoke vents, green flames */
	CITY,
	/** the demon halls, 21-24: cold lava, embermoss, skulls */
	HALLS,
	/** the frozen branch off the halls: ice, meltwater, snow */
	FROZEN,
	/** the spider nest off the prison: webbing, husks */
	NEST,
	/** the dwarves' vault off the city: the city's life, kept quiet (it is a stealth floor) */
	VAULT,
	/** the temple off the caves: still air, old stone */
	TEMPLE,
	/** the kupua mines under the town, 56-64 */
	MINES,
	/** the postgame's open field (27) */
	MEADOW,
	/** the postgame's fishing shore (29) */
	SHORE,
	/** the postgame's catacombs (31) */
	CATACOMB,
	/** the overworld's surface */
	SURFACE,
	/** the overworld's mountain slices */
	PEAKS,
	/** the overworld's cave slices: their life and sounds are CaveLife's own */
	CAVE_SLICES;

	/** Is this a floor of the dungeon (DungeonLife's), rather than the overworld's? */
	public boolean underground(){
		return this != SURFACE && this != PEAKS && this != CAVE_SLICES;
	}
}

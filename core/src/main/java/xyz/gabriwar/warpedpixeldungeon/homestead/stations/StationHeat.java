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

package xyz.gabriwar.warpedpixeldungeon.homestead.stations;

import xyz.gabriwar.warpedpixeldungeon.homestead.HeatSources;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

/**
 * The stations' heat in a room (HeatSources' provider): a lit hearth gives Hearth.ROOM_HEAT, a lit
 * forge Forge.ROOM_HEAT and a lit kiln Kiln.ROOM_HEAT to the room it stands in, read off the
 * slice's register of stations (StationSlice), one cell at a time.
 */
public class StationHeat implements HeatSources.Provider {

	@Override
	public float heatAt( OverworldLevel level, int cell ){
		StationSlice s = StationSlice.of( level );
		return s == null ? 0f : s.heatAt( level, cell );
	}
}

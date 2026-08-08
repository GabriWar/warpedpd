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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Employee;
import xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.TownInteriors;

//Remixed PD's cinema interior, reached from the town square.
public class TownCinemaLevel extends TownInteriorLevel {

	@Override protected int[] terrain()      { return TownInteriors.cinemaMap(); }
	@Override protected int mapWidth()       { return TownInteriors.CINEMA_W; }
	@Override protected int mapHeight()      { return TownInteriors.CINEMA_H; }
	@Override protected int entranceCell()   { return TownInteriors.CINEMA_ENTRANCE; }

	@Override
	protected CustomTilemap[] groundLayers() {
		return new CustomTilemap[]{
				new TownInteriors.CinemaBase(),
				new TownInteriors.CinemaDeco(),
				new TownInteriors.CinemaDeco2() };
	}

	@Override
	protected CustomTilemap roofLayer() {
		return new TownInteriors.CinemaRoof();
	}

	@Override
	protected void spawnFolk() {
		place( new Employee(), 162 );
	}
}

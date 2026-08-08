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

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Librarian;
import xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.TownInteriors;

//Remixed PD's library interior, reached from the town square.
public class TownLibraryLevel extends TownInteriorLevel {

	@Override protected int[] terrain()      { return TownInteriors.libraryMap(); }
	@Override protected int mapWidth()       { return TownInteriors.LIBRARY_W; }
	@Override protected int mapHeight()      { return TownInteriors.LIBRARY_H; }
	@Override protected int entranceCell()   { return TownInteriors.LIBRARY_ENTRANCE; }

	@Override
	protected CustomTilemap[] groundLayers() {
		return new CustomTilemap[]{
				new TownInteriors.LibraryBase(),
				new TownInteriors.LibraryDeco(),
				new TownInteriors.LibraryDeco2() };
	}

	@Override
	protected CustomTilemap roofLayer() {
		return new TownInteriors.LibraryRoof();
	}

	@Override
	protected void spawnFolk() {
		place( new Librarian(), 164 );
	}
}

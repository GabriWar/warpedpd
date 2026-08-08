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

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Bard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Drunkard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.InnKeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.InnServant;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Mercenary;
import xyz.gabriwar.warpedpixeldungeon.tiles.CustomTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.TownInteriors;

//Remixed PD's inn interior, reached from the town square.
public class TownInnLevel extends TownInteriorLevel {

	@Override protected int[] terrain()      { return TownInteriors.innMap(); }
	@Override protected int mapWidth()       { return TownInteriors.INN_W; }
	@Override protected int mapHeight()      { return TownInteriors.INN_H; }
	@Override protected int entranceCell()   { return TownInteriors.INN_ENTRANCE; }

	@Override
	protected CustomTilemap[] groundLayers() {
		return new CustomTilemap[]{
				new TownInteriors.InnBase(),
				new TownInteriors.InnDeco(),
				new TownInteriors.InnDeco2() };
	}

	@Override
	protected CustomTilemap roofLayer() {
		return new TownInteriors.InnRoof();
	}

	@Override
	protected void spawnFolk() {
		place( new InnKeeper(), 166 );
		place( new Drunkard(), 235 );
		place( new Mercenary(), 192 );
		place( new InnServant(), 314 );
		place( new Bard(), 246 );
	}
}

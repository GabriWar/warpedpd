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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.special;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Point;

/**
 * An arcane library with an enchanting pedestal at its heart. The shelves
 * around the room feed the pedestal a slow stream of glyphs; the player can
 * lay a weapon on the stone to empower its enchantments or weave in a second.
 */
public class EnchantingLibraryRoom extends SpecialRoom {

	//fixed 13x13, the scale of a large study room. odd size keeps the
	//pedestal on the exact center tile
	@Override
	public int minWidth()  { return 13; }
	@Override
	public int minHeight() { return 13; }
	@Override
	public int maxWidth()  { return 13; }
	@Override
	public int maxHeight() { return 13; }

	public void paint( Level level ) {

		//styled after the study: a full bookshelf ring hugging the walls,
		//plus L-shaped shelf pillars in the corners
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.BOOKSHELF );
		Painter.fill( level, this, 2, Terrain.EMPTY_SP );

		Door entrance = entrance();
		Painter.drawInside( level, this, entrance, 2, Terrain.EMPTY_SP );

		int pillarW = (width()-7)/2;
		int pillarH = (height()-7)/2;

		Painter.fill(level, left+3, top+3, pillarW, 1, Terrain.BOOKSHELF);
		Painter.fill(level, left+3, top+3, 1, pillarH, Terrain.BOOKSHELF);

		Painter.fill(level, left+3, bottom-2-1, pillarW, 1, Terrain.BOOKSHELF);
		Painter.fill(level, left+3, bottom-2-pillarH, 1, pillarH, Terrain.BOOKSHELF);

		Painter.fill(level, right-2-pillarW, top+3, pillarW, 1, Terrain.BOOKSHELF);
		Painter.fill(level, right-2-1, top+3, 1, pillarH, Terrain.BOOKSHELF);

		Painter.fill(level, right-2-pillarW, bottom-2-1, pillarW, 1, Terrain.BOOKSHELF);
		Painter.fill(level, right-2-1, bottom-2-pillarH, 1, pillarH, Terrain.BOOKSHELF);

		//the pedestal sits at the room's center
		Point c = center();
		Painter.set( level, c, Terrain.PEDESTAL );

		EnchantingStation station = new EnchantingStation();
		station.pos = level.pointToCell( c );
		level.mobs.add( station );

		//a stray scroll left behind by past scribes
		int pos;
		do {
			pos = level.pointToCell(random());
		} while (level.map[pos] != Terrain.EMPTY_SP
				|| level.heaps.get( pos ) != null
				|| pos == station.pos);
		level.drop( Generator.random( Generator.Category.SCROLL ), pos );

		entrance.set( Door.Type.REGULAR );
	}
}

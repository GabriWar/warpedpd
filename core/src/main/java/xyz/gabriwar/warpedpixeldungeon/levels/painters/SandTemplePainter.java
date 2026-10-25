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

package xyz.gabriwar.warpedpixeldungeon.levels.painters;

import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.Room;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The buried temples' floors (levels/SandTempleLevel): the sand that has sifted down drifted against
 * the walls (EMPTY_DECO, thicker the more walls a floor has round it), and the walls carved with
 * reliefs here and there (WALL_DECO, on faces the hero sees).
 */
public class SandTemplePainter extends RegularPainter {

	@Override
	protected void decorate( Level level, ArrayList<Room> rooms ){
		int[] map = level.map;
		int w = level.width();
		int l = level.length();
		for (int i = w + 1; i < l - w - 1; i++){
			if (map[i] == Terrain.EMPTY){
				int walls = (map[i + 1] == Terrain.WALL ? 1 : 0) + (map[i - 1] == Terrain.WALL ? 1 : 0)
						+ (map[i + w] == Terrain.WALL ? 1 : 0) + (map[i - w] == Terrain.WALL ? 1 : 0);
				if (Random.Int( 12 ) < 1 + walls * 2) map[i] = Terrain.EMPTY_DECO;
			} else if (map[i] == Terrain.WALL && !DungeonTileSheet.wallStitcheable( map[i + w] )
					&& Random.Int( 6 ) == 0){
				map[i] = Terrain.WALL_DECO;
			}
		}
	}
}

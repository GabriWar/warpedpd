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

package xyz.gabriwar.warpedpixeldungeon.levels.rooms.secret;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;

public class SecretArtilleryRoom extends SecretRoom {

	@Override
	public void paint(Level level) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.EMPTY_SP);

		Painter.set(level, center(), Terrain.STATUE_SP);

		//living-world twist: a strong wind topside carried extra ordnance in
		int drops = ClimateManager.surfaceWindSpeed() > 15f ? 4 : 3;

		for (int i = 0; i < drops; i++){
			int itemPos;
			do{
				itemPos = level.pointToCell(random());
			} while ( level.map[itemPos] != Terrain.EMPTY_SP
					|| level.heaps.get(itemPos) != null);

			if( i == 0 ){
				level.drop(new Bomb.DoubleBomb(), itemPos);
			} else {
				level.drop(Generator.randomMissile(true), itemPos);
			}
		}

		entrance().set(Door.Type.HIDDEN);
	}
}

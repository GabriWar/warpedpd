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

package xyz.gabriwar.warpedpixeldungeon.levels.templeChambers;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Piranha;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Point;

public class PiranhaPoolChamber extends Chamber {

    {
        isBuildWithStructure = false;
    }

    @Override
    public void build() {
        super.build();

        Painter.fill(level, innerRoom, 1, Terrain.EMPTY_SP);
        Painter.fill(level, innerRoom, 2, Terrain.WATER);
        for (int i = 0; i < 4; i++) {
            Painter.drawLine(level, doorPoint(i), center, Terrain.EMPTY_SP);
        }

        Painter.set(level, center, Terrain.PEDESTAL);

        Piranha piranha1 = Piranha.random();
        piranha1.pos = level.pointToCell(new Point(center.x-2, center.y-2));
        level.mobs.add(piranha1);

        Piranha piranha2 = Piranha.random();
        piranha2.pos = level.pointToCell(new Point(center.x+2, center.y-2));
        level.mobs.add(piranha2);

        Piranha piranha3 = Piranha.random();
        piranha3.pos = level.pointToCell(new Point(center.x-2, center.y+2));
        level.mobs.add(piranha3);

        Piranha piranha4 = Piranha.random();
        piranha4.pos = level.pointToCell(new Point(center.x+2, center.y+2));
        level.mobs.add(piranha4);
    }
}
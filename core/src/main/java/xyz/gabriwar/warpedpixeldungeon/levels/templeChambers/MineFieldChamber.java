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

import static xyz.gabriwar.warpedpixeldungeon.levels.TempleNewLevel.CHAMBER_HEIGHT;
import static xyz.gabriwar.warpedpixeldungeon.levels.TempleNewLevel.CHAMBER_WIDTH;

import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ExplosiveTrap;
import com.watabou.utils.Point;

import java.util.ArrayList;

public class MineFieldChamber extends Chamber {

    {
        isBuildWithStructure = false;
    }

    @Override
    public void build() {
        super.build();

        ArrayList<Point> floorPoint = new ArrayList<>();

        int[] offsets = {-1, 0, 1};
        for (int offset : offsets) {
            floorPoint.add(new Point(center.x + offset, center.y - CHAMBER_HEIGHT / 2));
            floorPoint.add(new Point(center.x + offset, center.y + CHAMBER_HEIGHT / 2));
            floorPoint.add(new Point(center.x - CHAMBER_WIDTH / 2, center.y + offset));
            floorPoint.add(new Point(center.x + CHAMBER_WIDTH / 2, center.y + offset));
        }

        for (Point p : floorPoint) {
            Painter.set(level, p, Terrain.EMPTY_SP);
        }

        ArrayList<Integer> emberCellArray = randomRoomPos(100);
        for (int emberCell : emberCellArray) {
            if (!floorPoint.contains(level.cellToPoint(emberCell)) && emberCell != level.pointToCell(center)) {
                Painter.set(level, emberCell, Terrain.EMBERS);
            }
        }

        for (int trapCell : randomRoomPos(100)) {
            if (!floorPoint.contains(level.cellToPoint(trapCell)) && !emberCellArray.contains(trapCell) && trapCell != level.pointToCell(center)) {
                Painter.set(level, trapCell, Terrain.SECRET_TRAP);
                level.setTrap(new ExplosiveTrap().hide(), trapCell);
            }
        }
    }
}
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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ThunderCloud;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.Point;

public class MoistureChamber extends Chamber {
    {
        isBuildWithStructure = false;
    }

    @Override
    public void build() {
        super.build();

        Painter.fill(level, innerRoom, Terrain.WATER);
        Painter.fill(level, innerRoom, 1, Terrain.BOOKSHELF);
        Painter.fill(level, innerRoom, 3, Terrain.WATER);
        Painter.fill(level, innerRoom, 5, Terrain.BOOKSHELF);
        Painter.fill(level, innerRoom, 7, Terrain.WATER);
        Painter.drawLine(level, center, topDoor, Terrain.WATER);
        Painter.drawLine(level, center, bottomDoor, Terrain.WATER);
        Painter.drawLine(level, center, leftDoor, Terrain.WATER);
        Painter.drawLine(level, center, rightDoor, Terrain.WATER);
        Painter.set(level, center, Terrain.PEDESTAL);

        for (Point p : innerRoom.getPoints()){
            int cell = level.pointToCell(p);
            if (level.map[cell] == Terrain.WATER) {
                //as if gas has been spreading in the room for a while
                Blob.seed(cell, 30, ThunderCloud.class, level);
                Blob.seed(cell, 12, ThunderCloudSeed.class, level);
            }
        }
    }

    public static class ThunderCloudSeed extends Blob {

        @Override
        protected void evolve() {
            int cell;
            ThunderCloud gas = (ThunderCloud) Dungeon.level.blobs.get(ThunderCloud.class);
            for (int i=area.top-1; i <= area.bottom; i++) {
                for (int j = area.left-1; j <= area.right; j++) {
                    cell = j + i* Dungeon.level.width();
                    if (Dungeon.level.insideMap(cell)) {
                        if (Dungeon.level.map[cell] != Terrain.WATER){
                            off[cell] = 0;
                            continue;
                        }

                        off[cell] = cur[cell];
                        volume += off[cell];

                        if (gas == null || gas.volume == 0){
                            GameScene.add(Blob.seed(cell, off[cell], ThunderCloud.class));
                        } else if (gas.cur[cell] <= 9*off[cell]){
                            GameScene.add(Blob.seed(cell, off[cell], ThunderCloud.class));
                        }
                    }
                }
            }
        }

    }
}

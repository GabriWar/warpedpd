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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ConfusionGas;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

import java.util.ArrayList;

public class ConfusionChamber extends Chamber {
    {
        isBuildWithStructure = true;
    }

    @Override
    public int[] roomStructure() {
        return new int[] {
                1, 1, 1, 1, 1, 1, 1, 25, 1, 25, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4, 5, 4, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 25, 25, 25, 1, 1, 1, 1, 1, 1, 1, 25, 4, 1, 1, 1, 1, 25, 14, 14, 14, 25, 1, 1, 1, 1, 4, 25, 1, 5, 1, 1, 1, 1, 25, 14, 11, 14, 25, 1, 1, 1, 1, 5, 1, 25, 4, 1, 1, 1, 1, 25, 14, 14, 14, 25, 1, 1, 1, 1, 4, 25, 1, 1, 1, 1, 1, 1, 1, 25, 25, 25, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4, 5, 4, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 25, 1, 25, 1, 1, 1, 1, 1, 1, 1
        };
    }

    @Override
    public void build() {
        super.build();

        int[][] exceptionCells = {
                //top
                {-1, -8}, {0, -8}, {1, -8},
                {-1, -7}, {0, -7}, {1, -7},
                //bottom
                {-1, 7}, {0, 7}, {1, 7},
                {-1, 8}, {0, 8}, {1, 8},
                //left
                {-8, -1}, {-8, 0}, {-8, 1},
                {-7, -1}, {-7, 0}, {-7, 1},
                //right
                {7, -1}, {7, 0}, {7, 1},
                {8, -1}, {8, 0}, {8, 1},
                //center
                            {-1, -2},   {0, -2},    {1, -2},
                {-2, -1},   {-1, -1},   {0, -1},    {1, -1},    {2, -1},
                {-2, 0},    {-1, 0},    {0, 0},     {1, 0},     {2, 0},
                {-2, 1},    {-1, 1},    {0, 1},     {1, 1},     {2, 1},
                            {-1, 2},    {0, 2},     {1, 2},
        };

        ArrayList<Integer> exception = new ArrayList<>(customOffsetArray(exceptionCells));

        final int TRAP_NUM = 60;
        for (int pos : randomRoomPos(TRAP_NUM, exception)) {
            level.setTrap(new ConfusionVent().reveal(), pos);
            Blob.seed(pos, 20, ConfusionGasSeed.class, level);
            Painter.set(level, pos, Terrain.INACTIVE_TRAP);
        }
    }

    public static class ConfusionGasSeed extends Blob {

        @Override
        protected void evolve() {
            int cell;
            ConfusionGas gas = (ConfusionGas) Dungeon.level.blobs.get(ConfusionGas.class);
            for (int i=area.top-1; i <= area.bottom; i++) {
                for (int j = area.left-1; j <= area.right; j++) {
                    cell = j + i* Dungeon.level.width();
                    if (Dungeon.level.insideMap(cell)) {
                        if (Dungeon.level.map[cell] != Terrain.INACTIVE_TRAP){
                            off[cell] = 0;
                            continue;
                        }

                        off[cell] = cur[cell];
                        volume += off[cell];

                        if (gas == null || gas.volume == 0){
                            GameScene.add(Blob.seed(cell, off[cell], ConfusionGas.class));
                        } else if (gas.cur[cell] <= 9*off[cell]){
                            GameScene.add(Blob.seed(cell, off[cell], ConfusionGas.class));
                        }
                    }
                }
            }
        }

    }

    public static class ConfusionVent extends Trap {

        {
            color = BLACK;
            shape = GRILL;

            canBeHidden = false;
            active = false;
        }

        @Override
        public void activate() {
            //does nothing, this trap is just decoration and is always deactivated
        }

    }
}

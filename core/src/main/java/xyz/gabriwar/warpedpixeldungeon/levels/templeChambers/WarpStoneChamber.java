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
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfBlink;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FlashingTrap;
import com.watabou.utils.Reflection;

public class WarpStoneChamber extends Chamber {
    {
        isBuildWithStructure = true;
    }

    @Override
    public int[] roomStructure() {
        return new int[] {
                //this is made by sandbox pd
                25, 26, 25, 25, 11, 25, 25, 26, 14, 25, 25, 25, 25, 25, 25, 25, 25, 25, 18, 18, 18, 18, 18, 18, 18, 11, 25, 18, 18, 26, 18, 18, 18, 26, 25, 18, 18, 18, 25, 25, 25, 25, 14, 25, 18, 18, 18, 11, 18, 18, 25, 25, 18, 11, 18, 18, 18, 25, 14, 25, 25, 18, 18, 18, 18, 18, 18, 25, 25, 26, 18, 18, 25, 11, 25, 26, 18, 25, 18, 18, 25, 18, 25, 18, 11, 25, 18, 18, 18, 18, 18, 18, 18, 18, 25, 18, 18, 11, 18, 25, 18, 25, 25, 18, 18, 18, 18, 18, 18, 18, 18, 25, 18, 18, 25, 25, 25, 18, 25, 25, 25, 25, 25, 25, 25, 25, 25, 14, 25, 18, 18, 26, 14, 25, 18, 25, 14, 11, 14, 25, 18, 18, 18, 14, 11, 14, 18, 18, 18, 25, 14, 11, 14, 26, 18, 25, 14, 26, 18, 18, 25, 14, 25, 25, 25, 25, 25, 25, 25, 25, 25, 18, 25, 25, 25, 18, 18, 25, 18, 18, 18, 18, 18, 18, 18, 18, 25, 25, 18, 25, 18, 11, 18, 18, 25, 18, 18, 18, 18, 18, 18, 18, 18, 25, 11, 18, 25, 18, 25, 18, 18, 25, 18, 26, 25, 11, 25, 18, 18, 26, 25, 25, 18, 18, 18, 18, 18, 18, 25, 25, 14, 25, 18, 18, 18, 11, 18, 25, 25, 18, 18, 11, 18, 18, 18, 25, 14, 25, 25, 25, 25, 18, 18, 18, 25, 26, 18, 18, 18, 26, 18, 18, 25, 11, 18, 18, 18, 18, 18, 18, 18, 25, 25, 25, 25, 25, 25, 25, 25, 25, 14, 26, 25, 25, 11, 25, 25, 26, 25
        };
    }

    @Override
    public void build() {
        super.build();

        int[][] offsets = {
                {-3, -4}, {-6, -5}, {-4, -8},
                {4, -3}, {5, -6}, {8, -4},
                {3, 4}, {6, 5}, {4, 8},
                {-4, 3}, {-5, 6}, {-8, 4},
                {0, 0}, {0, -7}, {7, 0}, {0, 7}, {-7, 0},
        };

        for (int pos : customOffsetArray(offsets)) {
            level.drop(new StoneOfBlink(), pos);
        }


        for (int cell : innerRoomPos()) {
            if (level.map[cell] == Terrain.TRAP) {
                level.setTrap(Reflection.newInstance(FlashingTrap.class).reveal(), cell);
            }
        }

    }
}
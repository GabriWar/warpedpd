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

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollGuard;

public class SpearGnollChamber extends Chamber {
    {
        isBuildWithStructure = true;
    }

    @Override
    public int[] roomStructure() {
        return new int[] {
                14, 14, 14, 14, 14, 4, 14, 5, 14, 5, 14, 14, 14, 14, 14, 14, 14, 14, 0, 0, 0, 14, 4, 14, 4, 14, 4, 0, 0, 0, 0, 0, 0, 14, 14, 0, 11, 0, 14, 4, 0, 4, 26, 4, 11, 0, 11, 0, 11, 0, 14, 14, 0, 0, 0, 14, 4, 27, 4, 14, 4, 0, 0, 0, 0, 0, 0, 14, 14, 0, 11, 0, 14, 4, 14, 5, 14, 5, 14, 14, 14, 14, 14, 14, 14, 14, 0, 0, 0, 14, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 14, 0, 11, 0, 14, 4, 27, 27, 15, 27, 27, 4, 14, 27, 0, 14, 14, 5, 4, 4, 4, 5, 4, 27, 15, 15, 15, 27, 4, 5, 4, 4, 4, 5, 14, 14, 26, 14, 14, 5, 15, 15, 11, 15, 15, 5, 14, 14, 26, 14, 14, 5, 4, 4, 4, 5, 4, 27, 15, 15, 15, 27, 4, 5, 4, 4, 4, 5, 14, 14, 0, 27, 14, 4, 27, 27, 15, 27, 27, 4, 14, 0, 11, 0, 14, 4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 14, 0, 0, 0, 14, 14, 14, 14, 14, 14, 14, 14, 5, 14, 5, 14, 4, 14, 0, 11, 0, 14, 14, 0, 0, 0, 0, 0, 0, 4, 14, 4, 27, 4, 14, 0, 0, 0, 14, 14, 0, 11, 0, 11, 0, 11, 4, 26, 4, 0, 4, 14, 0, 11, 0, 14, 14, 0, 0, 0, 0, 0, 0, 4, 14, 4, 14, 4, 14, 0, 0, 0, 14, 14, 14, 14, 14, 14, 14, 14, 5, 14, 5, 14, 4, 14, 14, 14, 14, 14
        };
    }

    @Override
    public void build() {
        super.build();

        int[][] offsets = {
                {-6, -2}, {-6, -4}, {-6, -6},
                {2, -6}, {4, -6}, {6, -6},
                {6, 2}, {6, 4}, {6, 6},
                {-2, 6}, {-4, 6}, {-6, 6},
        };

        for (int pos : customOffsetArray(offsets)) {
            GnollGuard gnollGuard = new GnollGuard();
            gnollGuard.pos = pos;
            level.mobs.add(gnollGuard);
        }
    }
}
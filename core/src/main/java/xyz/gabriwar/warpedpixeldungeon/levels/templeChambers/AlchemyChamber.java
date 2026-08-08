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

import xyz.gabriwar.warpedpixeldungeon.items.EnergyCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

public class AlchemyChamber extends Chamber {

    {
        isBuildWithStructure = true;
    }

    @Override
    public int[] roomStructure() {
        int n = -1;
        int E = Terrain.EMPTY_SP;
        int B = Terrain.BOOKSHELF;
        int P = Terrain.PEDESTAL;
        int A = Terrain.ALCHEMY;
        int S = Terrain.STATUE_SP;
        return new int[] {
                E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
                E, B, B, B, B, B, B, B, E, B, B, B, B, B, B, B, E,
                E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
                E, B, B, B, B, B, B, B, E, B, B, B, B, B, B, B, E,
                E, E, E, E, E, E, E, E, E, E, P, E, E, E, P, E, E,
                E, B, B, B, B, B, B, B, E, E, P, E, A, E, P, E, E,
                E, E, E, E, E, E, E, E, E, E, P, E, E, E, P, E, E,
                E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
                E, E, E, E, E, E, E, E, n, E, E, E, E, E, E, E, E,
                E, E, E, E, E, E, E, E, E, E, S, E, S, E, S, E, E,
                E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
                E, B, B, B, B, B, B, B, E, B, B, B, B, B, B, B, E,
                E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
                E, B, B, B, B, B, B, B, E, B, B, B, B, B, B, B, E,
                E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
                E, B, B, B, B, B, B, B, E, B, B, B, B, B, B, B, E,
                E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
        };
    }

    @Override
    public void build() {
        super.build();
        for (int i = 2; i < 5; i++) {
            Point pedestalPos = new Point(center.x+2, center.y-i);
            if (i == 2) {
                level.drop(Generator.randomUsingDefaults(Generator.Category.FOOD), level.pointToCell(pedestalPos));
            } else {
                level.drop(new EnergyCrystal().quantity(Random.IntRange(6,10)), level.pointToCell(pedestalPos));
            }
        }
        for (int i = 2; i < 5; i++) {
            Point pedestalPos = new Point(center.x+6, center.y-i);
            level.drop(Generator.randomUsingDefaults(Generator.Category.POTION), level.pointToCell(pedestalPos));
        }
    }
}
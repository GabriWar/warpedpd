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

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.TempleSentry;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import com.watabou.utils.Point;

import java.util.ArrayList;

public class SentryChamber extends Chamber {

    {
        isBuildWithStructure = true;
    }

    @Override
    public int[] roomStructure() {
        int n = -1;
        int F = Terrain.EMPTY;
        int W = Terrain.WALL;
        int E = Terrain.EMPTY_SP;
        int S = Terrain.STATUE_SP;
        int P = Terrain.PEDESTAL;
        int M = Terrain.EMBERS;
        return new int[] {
                W, W, W, W, P, P, P, W, F, W, P, P, P, W, W, W, W,
                W, W, W, W, S, S, S, W, F, W, S, S, S, W, W, W, W,
                W, W, E, E, E, E, E, E, E, E, E, E, E, E, E, W, W,
                W, W, E, E, E, E, E, E, E, E, E, E, E, E, E, W, W,
                P, S, E, E, E, E, E, E, E, E, E, E, E, E, E, S, P,
                P, S, E, E, E, E, E, S, S, S, E, E, E, E, E, S, P,
                P, S, E, E, E, E, E, E, E, E, E, E, E, E, E, S, P,
                W, W, E, E, E, S, E, M, M, M, E, S, E, E, E, W, W,
                F, F, E, E, E, S, E, M, P, M, E, S, E, E, E, F, F,
                W, W, E, E, E, S, E, M, M, M, E, S, E, E, E, W, W,
                P, S, E, E, E, E, E, E, E, E, E, E, E, E, E, S, P,
                P, S, E, E, E, E, E, S, S, S, E, E, E, E, E, S, P,
                P, S, E, E, E, E, E, E, E, E, E, E, E, E, E, S, P,
                W, W, E, E, E, E, E, E, E, E, E, E, E, E, E, W, W,
                W, W, E, E, E, E, E, E, E, E, E, E, E, E, E, W, W,
                W, W, W, W, S, S, S, W, F, W, S, S, S, W, W, W, W,
                W, W, W, W, P, P, P, W, F, W, P, P, P, W, W, W, W,
        };
    }

    @Override
    public void build() {
        super.build();
        ArrayList<Point> sentryPoint = new ArrayList<>();

        int[] offsets = {-4, -3, -2, 2, 3, 4};
        for (int offset : offsets) {
            // 상단 및 하단 센트리 위치
            sentryPoint.add(new Point(center.x + offset, center.y - CHAMBER_HEIGHT / 2));
            sentryPoint.add(new Point(center.x + offset, center.y + CHAMBER_HEIGHT / 2));

            // 좌측 및 우측 센트리 위치
            sentryPoint.add(new Point(center.x - CHAMBER_WIDTH / 2, center.y + offset));
            sentryPoint.add(new Point(center.x + CHAMBER_WIDTH / 2, center.y + offset));
        }

        for (Point p : sentryPoint) {
            int sentryCell = level.pointToCell(p);
            TempleSentry sentry = new TempleSentry();
            sentry.pos = sentryCell;
            sentry.initialChargeDelay = sentry.curChargeDelay = 3f + 0.1f;
            level.mobs.add( sentry );
        }

    }
}
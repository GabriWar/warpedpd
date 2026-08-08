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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.keys.CrystalKey;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.TempleSentry;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.MagicalFireRoom;
import com.watabou.utils.Point;

import java.util.ArrayList;

public class SentryMazeChamber extends Chamber {

    {
        isBuildWithStructure = true;
    }

    @Override
    public int[] roomStructure() {
        return new int[] {
                //this is made by sandbox pd
                1, 1, 4, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 4, 1, 1, 1, 1, 4, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 13, 1, 1, 4, 0, 4, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 4, 4, 13, 0, 1, 1, 25, 0, 0, 1, 0, 1, 0, 1, 0, 0, 25, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 14, 14, 14, 14, 14, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 14, 11, 31, 11, 14, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 14, 31, 11, 31, 14, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 14, 11, 31, 11, 14, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 14, 14, 14, 14, 14, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 14, 0, 14, 14, 14, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 0, 14, 0, 14, 0, 14, 14, 0, 0, 0, 0, 25, 1, 1, 1, 0, 1, 0, 14, 0, 14, 26, 0, 14, 0, 4, 4, 4, 1, 1, 0, 0, 0, 1, 0, 14, 14, 14, 0, 4, 14, 4, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 14, 0, 0, 4, 1, 1, 1, 1, 4, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 4, 1, 1
        };
    }

    @Override
    public void build() {
        super.build();

        int eternalFireCell = level.pointToCell(new Point(center.x-6, center.y+7));
        int cell1 = level.pointToCell(new Point(center.x-CHAMBER_WIDTH/2, center.y+CHAMBER_HEIGHT/2));
        int cell2 = level.pointToCell(new Point(center.x+CHAMBER_WIDTH/2, center.y+CHAMBER_HEIGHT/2));
        int cell3 = level.pointToCell(new Point(center.x+CHAMBER_WIDTH/2, center.y-CHAMBER_HEIGHT/2));
        int cell4 = level.pointToCell(new Point(center.x-CHAMBER_WIDTH/2, center.y-CHAMBER_HEIGHT/2));

        Blob.seed(eternalFireCell, 1, MagicalFireRoom.EternalFire.class, level);

        level.drop(new CrystalKey(Dungeon.depth), cell1).type = Heap.Type.CHEST;
        level.drop(new CrystalKey(Dungeon.depth), cell2).type = Heap.Type.CHEST;
        level.drop(new CrystalKey(Dungeon.depth), cell3).type = Heap.Type.CHEST;
        level.drop(new CrystalKey(Dungeon.depth), cell4).type = Heap.Type.CHEST;

        ArrayList<Point> sentryPos = new ArrayList<>();
        sentryPos.add(new Point(center.x-1, center.y-1));
        sentryPos.add(new Point(center.x+1, center.y-1));
        sentryPos.add(new Point(center.x-1, center.y+1));
        sentryPos.add(new Point(center.x+1, center.y+1));

        int dangerDist = 10;
        for (Point p : sentryPos) {
            int sentryCell = level.pointToCell(p);
            TempleSentry sentry = new TempleSentry();
            sentry.pos = sentryCell;
            sentry.initialChargeDelay = sentry.curChargeDelay = dangerDist / 3f + 0.1f;
            level.mobs.add( sentry );
        }
    }
}
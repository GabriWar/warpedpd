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

import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Rect;

import java.util.ArrayList;

public class Chamber {
    //these variable doesn't need to be saved(with storeInBundle()), because these are only used when generating the level
    public Level level;
    public Point center;
    public Rect innerRoom;
    private Rect outerRoom;
    public Point topDoor;
    public Point bottomDoor;
    public Point leftDoor;
    public Point rightDoor;
    private ArrayList<Integer> innerRoomPos = new ArrayList<>();
    public boolean isBuildWithStructure = false; //make this false if you want to make room with Painter

    public void set(Level level, int left, int top, int right, int bottom, Point center) {
        this.level      = level;
        this.center     = center;
        this.outerRoom  = new Rect( //this includes the wall
                left,
                top,
                right,
                bottom);
        this.innerRoom = new Rect( //this excludes the wall
                left+1,
                top+1,
                right-1,
                bottom-1);

        Point leftTopPoint = new Point(center.x-(CHAMBER_WIDTH/2-1), center.y-(CHAMBER_HEIGHT/2));
        for (int y = 0; y < CHAMBER_HEIGHT; y++) {
            for (int x = 0; x < CHAMBER_WIDTH; x++) {
                Point p = new Point((leftTopPoint.x-1)+x, leftTopPoint.y+y);
                innerRoomPos.add(this.level.pointToCell(p));
            }
        }

        this.topDoor    = new Point(center.x, center.y - CHAMBER_HEIGHT/2);
        this.bottomDoor = new Point(center.x, center.y + CHAMBER_HEIGHT/2);
        this.leftDoor   = new Point(center.x - CHAMBER_WIDTH/2, center.y);
        this.rightDoor  = new Point(center.x + CHAMBER_WIDTH/2, center.y);
    }

    public int[] roomStructure() {
        return new int[] {}; //must be overridden if isBuildWithStructure is true
    }

    public void build() {
        Painter.fill(level, outerRoom, Terrain.WALL);
        Painter.fill(level, outerRoom, 1, Terrain.EMPTY);
        Painter.set(level, center, Terrain.PEDESTAL);

            /*
                if the build does not work well, check the roomStructure() is correctly overridden
                if roomStructure() is too short, it will make uncompleted room
                if roomStructure() is too long, it will throw ArrayIndexOutOfBoundsException
            */
        if (isBuildWithStructure) {
            int index = 0;
            try {
                for (int pos : innerRoomPos) {
                    if (roomStructure()[index] != -1) level.map[pos] = roomStructure()[index];
                    index++;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<Integer> innerRoomPos() {
        return this.innerRoomPos;
    }

    //returns random pos in the room
    public ArrayList<Integer> randomRoomPos(int num) {
        ArrayList<Integer> result = new ArrayList<>();
        while (result.size() < num) {
            int randomResult = Random.element(innerRoomPos);
            if (!result.contains(randomResult)) {
                result.add(randomResult);
            }
        }

        return result;
    }

    //returns random pos in the room
    public ArrayList<Integer> randomRoomPos(int num, ArrayList<Integer> exception) {
        ArrayList<Integer> result = new ArrayList<>();
        while (result.size() < num) {
            int randomResult = Random.element(innerRoomPos);
            if (!result.contains(randomResult) && !exception.contains(randomResult)) {
                result.add(randomResult);
            }
        }

        return result;
    }

    //This returns the position of the array that is separated by the x, y coordinates from the center.
    public ArrayList<Integer> customOffsetArray(int[][] offsets) {
        ArrayList<Integer> resultArray = new ArrayList<>();

        for (int[] offset : offsets) {
            resultArray.add(level.pointToCell(new Point(center.x + offset[0], center.y + offset[1])));
        }

        return resultArray;
    }

    public Point doorPoint(int direction) {
        //direction: 0=top, 1=left, 2=bottom, 3=right, other=top (counterclockwise)
        switch (direction) {
            default: case 0:
                return new Point(center.x, center.y-CHAMBER_HEIGHT/2);
            case 1:
                return new Point(center.x-CHAMBER_WIDTH/2, center.y);
            case 2:
                return new Point(center.x, center.y+CHAMBER_HEIGHT/2);
            case 3:
                return new Point(center.x+CHAMBER_WIDTH/2, center.y);
        }
    }
}

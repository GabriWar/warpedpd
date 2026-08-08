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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.painters.Painter;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Rect;

public class TempleChasmLevel extends Level {

    {
        color1 = 0x64bb4c;
        color2 = 0x569545;
    }

    public static final String[] CAVES_TRACK_LIST
            = new String[]{Assets.Music.CAVES_1, Assets.Music.CAVES_2, Assets.Music.CAVES_2,
            Assets.Music.CAVES_1, Assets.Music.CAVES_3, Assets.Music.CAVES_3};
    public static final float[] CAVES_TRACK_CHANCES = new float[]{1f, 1f, 0.5f, 0.25f, 1f, 0.5f};

    @Override
    public void playLevelMusic() {
        if (Statistics.amuletObtained || Dungeon.templeCompleted){
            Music.INSTANCE.play(Assets.Music.CAVES_TENSE, true);
        } else {
            Music.INSTANCE.playTracks(CAVES_TRACK_LIST, CAVES_TRACK_CHANCES, false);
        }
    }

    public static final int WIDTH = 7;
    public static final int HEIGHT = 7;
    Rect rect = new Rect(0, 0, WIDTH, HEIGHT);

    private int entranceCell() {
        return (int)(WIDTH/2f)+WIDTH*(HEIGHT-4);
    }

    private int centerCell() {
        return (int)(WIDTH/2f)+WIDTH*(int)(WIDTH/2f);
    }

    @Override
    protected boolean build() {
        setSize(WIDTH, HEIGHT);

        transitions.add(new LevelTransition(this,
                entranceCell(),
                LevelTransition.Type.BRANCH_EXIT,
                Dungeon.depth,
                3,
                LevelTransition.Type.BRANCH_ENTRANCE));

        Painter.fill(this, rect, Terrain.WALL);
        Painter.fill(this, rect, 1, Terrain.EMPTY);
        Painter.set(this, centerCell(), Terrain.ENTRANCE);
        return true;
    }

    @Override
    public String tilesTex() {
        return Assets.Environment.TILES_TEMPLE;
    }

    @Override
    public String waterTex() {
        return Assets.Environment.WATER_TEMPLE;
    }

    @Override
    protected void createMobs() {

    }

    @Override
    protected void createItems() {

    }
}

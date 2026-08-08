/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

package xyz.gabriwar.warpedpixeldungeon.items.potions;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfDirt extends Potion {

    {
        icon = ItemSpriteSheet.Icons.POTION_DIRT;
    }

    private static final int[] SOIL_TYPES = {
        Terrain.SOIL_CORNWHEAT,
        Terrain.SOIL_WATERWHEAT,
        Terrain.SOIL_STRAWWHEAT,
        Terrain.SOIL_GREENWHEAT
    };

    @Override
    public void apply(Hero hero) {
        identify();
        shatter(hero.pos);
    }

    @Override
    public void shatter(int cell) {
        splash(cell);
        if (Dungeon.level.heroFOV[cell]) {
            identify();
            Sample.INSTANCE.play(Assets.Sounds.SHATTER);
        }
        for (int offset : PathFinder.NEIGHBOURS9) {
            int pos = cell + offset;
            if (!Dungeon.level.solid[pos]) {
                int t = Dungeon.level.map[pos];
                if (t == Terrain.EMPTY || t == Terrain.EMPTY_SP || t == Terrain.EMPTY_DECO
                        || t == Terrain.EMBERS || t == Terrain.GRASS
                        || t == Terrain.FURROWED_GRASS || t == Terrain.HIGH_GRASS) {
                    Level.set(pos, SOIL_TYPES[Random.Int(SOIL_TYPES.length)]);
                    GameScene.updateMap(pos);
                }
            }
        }
        Dungeon.observe();
    }

    @Override
    public int value() {
        return isKnown() ? 30 * quantity : super.value();
    }
}

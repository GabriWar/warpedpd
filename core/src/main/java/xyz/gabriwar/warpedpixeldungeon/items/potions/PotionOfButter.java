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
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.tiles.butters.PotionButterVariant1;
import xyz.gabriwar.warpedpixeldungeon.tiles.butters.PotionButterVariant2;
import xyz.gabriwar.warpedpixeldungeon.tiles.butters.PotionButterVariant3;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfButter extends Potion {

    {
        icon = ItemSpriteSheet.Icons.POTION_BUTTER;
    }

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
            if (Dungeon.level.passable[pos]
                    && (Dungeon.level.map[pos] == Terrain.EMPTY
                    || Dungeon.level.map[pos] == Terrain.EMPTY_DECO
                    || Dungeon.level.map[pos] == Terrain.EMPTY_SP)
                    && Dungeon.level.plants.get(pos) == null) {
                xyz.gabriwar.warpedpixeldungeon.tiles.butters.Butter variant;
                int choice = Random.Int(3);
                if (choice == 0) {
                    variant = new PotionButterVariant1();
                } else if (choice == 1) {
                    variant = new PotionButterVariant2();
                } else {
                    variant = new PotionButterVariant3();
                }
                Dungeon.level.setButters(variant, pos);
            }
        }
    }

    @Override
    public int value() {
        return isKnown() ? 30 * quantity : super.value();
    }
}

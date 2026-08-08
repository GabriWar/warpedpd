/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2024 Gabriel Batista
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
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfFlora extends Potion {

    {
        icon = ItemSpriteSheet.Icons.POTION_FLORA;
    }

    @Override
    public void apply(Hero hero) {
        identify();
        ArrayList<Integer> candidates = new ArrayList<>();
        for (int offset : PathFinder.NEIGHBOURS8) {
            int cell = hero.pos + offset;
            if (Dungeon.level.passable[cell]) candidates.add(cell);
        }
        int count = Math.min(8, candidates.size());
        for (int i = 0; i < count; i++) {
            int pos = Random.element(candidates);
            candidates.remove((Integer) pos);
            Plant.Seed seed = (Plant.Seed) Generator.random(Generator.Category.SEED);
            Dungeon.level.plant(seed, pos);
        }
        GLog.i(Messages.get(this, "msg"));
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
            if (Dungeon.level.passable[pos]) {
                Plant.Seed seed = (Plant.Seed) Generator.random(Generator.Category.SEED);
                Dungeon.level.plant(seed, pos);
            }
        }
    }

    @Override
    public int value() {
        return isKnown() ? 30 * quantity : super.value();
    }
}

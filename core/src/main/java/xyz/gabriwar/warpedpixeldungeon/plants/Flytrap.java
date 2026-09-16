/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

package xyz.gabriwar.warpedpixeldungeon.plants;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.UpgradeBlobRed;
import xyz.gabriwar.warpedpixeldungeon.items.UpgradeBlobViolet;
import xyz.gabriwar.warpedpixeldungeon.items.UpgradeBlobYellow;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Flytrap extends Plant {

    {
        image = 16;
        seedClass = Seed.class;
    }

    @Override
    public void trigger() {
        // Stay visible when stepped on; only feeding consumes this plant.
        activate(null);
    }

    @Override
    public void activate(Char ch) {
        // Random plant effects must not feed an absent upgrade eater.
        if (Dungeon.level.plants.get(pos) != this) return;
        Heap heap = Dungeon.level.heaps.get(pos);
        if (heap == null) return;
        Item item = heap.peek();
        Item result;
        if (item.isUpgradable()) {
            int ups = item.level();
            if (Random.Float() < ups / 10f) {
                result = new UpgradeBlobViolet();
            } else if (Random.Float() < ups / 5f) {
                result = new UpgradeBlobRed();
            } else if (Random.Float() < ups / 3f) {
                result = new UpgradeBlobYellow();
            } else {
                result = Generator.random(Generator.Category.SEED);
            }
        } else if (item instanceof Scroll || item instanceof Potion) {
            result = Random.Float() < 0.1f ? new UpgradeBlobYellow()
                    : Generator.random(Generator.Category.SEED);
        } else {
            return;
        }
        if (item.quantity() > 1) {
            item.quantity(item.quantity() - 1);
            heap.drop(result);
        } else {
            heap.replace(item, result);
        }
        heap.sprite.link();
        wither();
    }

    @Override
    public void spiceEffect(Char ch) {
        if (ch == null) return;
        for (int tries = 0; tries < 8; tries++) {
            int cell = ch.pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
            if (Dungeon.level.insideMap(cell) && Dungeon.level.passable[cell]
                    && Dungeon.level.plants.get(cell) == null) {
                Dungeon.level.plant(new Seed(), cell);
                return;
            }
        }
    }

    public static class Seed extends Plant.Seed {
        {
            image = ItemSpriteSheet.SEED_FLYTRAP;
            plantClass = Flytrap.class;
        }
    }
}

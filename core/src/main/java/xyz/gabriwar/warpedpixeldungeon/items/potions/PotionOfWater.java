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
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

/**
 * The bottle of water: plain water, always known, in a clear bottle of its own rather than one of
 * the potions' unknown colours. Filled at any water from an empty bottle (EmptyBottle), or brewed
 * from one in the alchemy pot.
 */
public class PotionOfWater extends Potion {

    {
        icon = ItemSpriteSheet.Icons.POTION_WATER;
    }

    @Override
    public void reset() {
        super.reset();
        image = ItemSpriteSheet.BOTTLE_OF_WATER;
        //known from the start, in the potions' records too (catalog, badges)
        if (handler != null && handler.contains( this ) && !handler.isKnown( this )) handler.know( this );
    }

    @Override
    public boolean isKnown() {
        return true;
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
            SpatialSound.play(Assets.Sounds.SHATTER, cell);
        }
        for (int offset : PathFinder.NEIGHBOURS9) {
            int pos = cell + offset;
            if (!Dungeon.level.solid[pos]) {
                Level.set(pos, Terrain.WATER);
                GameScene.updateMap(pos);
            }
        }
        Dungeon.observe();
    }

    @Override
    public int value() {
        return isKnown() ? 30 * quantity : super.value();
    }
}

/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2022-2025 Overgrown Team
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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.UnfilteredSunlight;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfUltraviolett extends Potion {

    {
        icon = ItemSpriteSheet.Icons.POTION_ULTRAVIOLET;
    }

    @Override
    public void apply(Hero hero) {
        identify();
        for (int offset : PathFinder.NEIGHBOURS9) {
            if (!Dungeon.level.solid[hero.pos + offset]) {
                GameScene.add(Blob.seed(hero.pos + offset, 2, UnfilteredSunlight.class));
            }
        }
    }

    @Override
    public void shatter(int cell) {
        splash(cell);
        if (Dungeon.level.heroFOV[cell]) {
            identify();
            Sample.INSTANCE.play(Assets.Sounds.SHATTER);
        }
        for (int offset : PathFinder.NEIGHBOURS9) {
            if (!Dungeon.level.solid[cell + offset]) {
                GameScene.add(Blob.seed(cell + offset, 2, UnfilteredSunlight.class));
            }
        }
    }

    @Override
    public int value() {
        return isKnown() ? 30 * quantity : super.value();
    }
}

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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfEgg extends Potion {

    {
        icon = ItemSpriteSheet.Icons.POTION_EGG;
    }

    @Override
    public void apply(Hero hero) {
        boolean restocked = false;
        for (int i = 0; i < Dungeon.level.map.length; i++) {
            if (Dungeon.level.map[i] == Terrain.EMPTY_BOOKSHELF) {
                if (Dungeon.level.heroFOV[i]) {
                    Level.set(i, Terrain.BOOKSHELF);
                    GameScene.updateMap(i);
                    restocked = true;
                }
            }
        }
        if (restocked) GLog.p(Messages.get(this, "restocked"));
        setKnown();
    }

    @Override
    public int value() {
        return isKnown() ? 30 * quantity : super.value();
    }
}

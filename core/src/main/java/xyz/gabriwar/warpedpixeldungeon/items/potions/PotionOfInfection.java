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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Beetle;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfInfection extends Potion {

    {
        icon = ItemSpriteSheet.Icons.POTION_INFECTION;
    }

    @Override
    public void apply(Hero hero) {
        identify();
        spawnAt(hero.pos);
    }

    @Override
    public void shatter(int cell) {
        splash(cell);
        if (Dungeon.level.heroFOV[cell]) {
            identify();
            Sample.INSTANCE.play(Assets.Sounds.SHATTER);
        }
        spawnAt(cell);
    }

    private void spawnAt(int cell) {
        Beetle beetle = new Beetle();
        beetle.pos = cell;
        beetle.spawn(Dungeon.depth);
        GameScene.add(beetle);
        Actor.addDelayed(new Pushing(beetle, cell, cell), -1f);
        beetle.sprite.alpha(0);
        beetle.sprite.parent.add(new AlphaTweener(beetle.sprite, 1, 0.15f));
    }

    @Override
    public int value() {
        return isKnown() ? 30 * quantity : super.value();
    }
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfSun extends Potion {

    {
        icon = ItemSpriteSheet.Icons.POTION_SUN;
    }

    @Override
    public void apply(Hero hero) {
        identify();
        killUndead();
    }

    @Override
    public void shatter(int cell) {
        if (Dungeon.level.heroFOV[cell]) {
            identify();
            splash(cell);
            Sample.INSTANCE.play(Assets.Sounds.SHATTER);
        }
        killUndead();
    }

    private void killUndead() {
        GameScene.flash(0x80FFFFFF);
        Sample.INSTANCE.play(Assets.Sounds.BLAST);
        ArrayList<Mob> undead = new ArrayList<>();
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob.properties().contains(Char.Property.UNDEAD)) {
                undead.add(mob);
            }
        }
        for (Mob mob : undead) {
            mob.die(this);
        }
    }

    @Override
    public int value() {
        return isKnown() ? 30 * quantity : super.value();
    }
}

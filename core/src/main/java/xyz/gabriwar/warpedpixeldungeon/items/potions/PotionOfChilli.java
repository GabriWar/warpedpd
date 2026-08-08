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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoulFire;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SoulElemental;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class PotionOfChilli extends Potion {

    {
        icon = ItemSpriteSheet.Icons.POTION_CHILLI;
    }

    @Override
    public void apply(Hero hero) {
        identify();
        Buff.prolong(hero, SoulFire.class, SoulFire.DURATION);
        if (Float.isNaN(hero.bodyTemp)) hero.bodyTemp = 20f;
        hero.bodyTemp += 10f;
    }

    @Override
    public void shatter(int cell) {
        if (Dungeon.level.heroFOV[cell]) {
            identify();
            splash(cell);
            Sample.INSTANCE.play(Assets.Sounds.SHATTER);
        }

        SoulElemental soulElemental = new SoulElemental();
        soulElemental.pos = cell;

        GameScene.add( soulElemental );
        Actor.addDelayed( new Pushing( soulElemental, cell, cell ), -1f );

        soulElemental.sprite.alpha( 0 );
        soulElemental.sprite.parent.add( new AlphaTweener( soulElemental.sprite, 1, 0.15f ) );

        GLog.p(Messages.get(this, "summon"));
    }

    @Override
    public int value() {
        return isKnown() ? 30 * quantity : super.value();
    }
}

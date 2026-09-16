/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.quick;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

public class QuickWeapon extends MeleeWeapon {

    public static final String AC_SLASH	= "SLASH";

    {
        defaultAction = AC_SLASH;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.remove(AC_EQUIP);
        actions.add(AC_SLASH);
        return actions;
    }

    //Re-ARranged weapon: never equipped, and no Duelist ability was designed for it
    @Override
    public boolean hasDuelistAbility() {
        return false;
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_SLASH)) {
            usesTargeting = true;
            curUser = hero;
            curItem = this;
            GameScene.selectCell(slasher);
        }
    }

    private CellSelector.Listener slasher = new CellSelector.Listener() {
        @Override
        public void onSelect( Integer target ) {
            if (target != null) {
                Char ch = Actor.findChar(target);
                Hero hero = Dungeon.hero;
                if (ch != null && ch.alignment == Char.Alignment.ENEMY) {
                    KindOfWeapon herosWeapon = hero.belongings.weapon; //기존에 사용하던 무기를 저장
                    hero.belongings.weapon = QuickWeapon.this; //공격에 사용할 무기를 이 무기로 변경

                    if (!hero.canAttack(ch)) {
                        GLog.w(Messages.get(QuickWeapon.class, "cannot_reach"));
                    } else {
                        hero.sprite.zap(ch.pos);
                        hero.busy();
                        hero.spendAndNext(hero.attackDelay());
                        hero.attack(ch, 1, 0, 1);
                        Invisibility.dispel();
                    }

                    hero.belongings.weapon = herosWeapon; //영웅의 무기를 원래 무기로 되돌림
                } else {
                    GLog.w(Messages.get(QuickWeapon.class, "no_enemy"));
                }
            }
        }
        @Override
        public String prompt() {
            return Messages.get(SpiritBow.class, "prompt");
        }
    };
}

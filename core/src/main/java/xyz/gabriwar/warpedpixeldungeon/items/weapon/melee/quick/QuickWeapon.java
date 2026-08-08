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

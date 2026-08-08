/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2021 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Evolution;
import xyz.gabriwar.warpedpixeldungeon.items.spells.UpgradeDust;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Katana;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;

import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;
import java.util.Arrays;

public class SharpKatana extends MeleeWeapon implements AlchemyWeapon {

    {
        image = ItemSpriteSheet.SHARP_KATANA;
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 1.3f;

        tier = 6;
    }

    @Override
    public boolean doPickUp( Hero hero, int pos ) {
        return super.doPickUp(hero, pos);
    }

    @Override
    public int min(int lvl) {
        return 1;
    }

    @Override
    public int max(int lvl) {
        return  5*(tier+2) +
                lvl*(tier+2);
    }

    @Override
    public String targetingPrompt() {
        return Messages.get(this, "prompt");
    }

    @Override
    protected void duelistAbility(Hero hero, Integer target) {
        flashSlashAbility(hero, target, 0.35f, this);
    }

    @Override
    public String abilityInfo() {
        if (levelKnown){
            return Messages.get(this, "ability_desc", Messages.decimalFormat("#.##", 0.35f));
        } else {
            return Messages.get(this, "typical_ability_desc", Messages.decimalFormat("#.##", 0.35f));
        }
    }

    @Override
    public ArrayList<Class<?extends Item>> weaponRecipe() {
        return new ArrayList<>(Arrays.asList(Katana.class, UpgradeDust.class, Evolution.class));
    }

    @Override
    public String discoverHint() {
        return AlchemyWeapon.hintString(weaponRecipe());
    }


    @Override
    public String desc() {
        String info = super.desc();

        info += "\n\n" + AlchemyWeapon.hintString(weaponRecipe());

        return info;
    }

    //inlined from Re-ARranged's NormalKatana (katana tier line not ported)
    public static void flashSlashAbility(Hero hero, Integer target, float energy, MeleeWeapon wep){
        if (target == null) {
            return;
        }

        Char enemy = Actor.findChar(target);
        if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
            GLog.w(Messages.get(MeleeWeapon.class, "ability_no_target"));
            return;
        }

        hero.belongings.abilityWeapon = wep;
        if (!hero.canAttack(enemy)){
            GLog.w(Messages.get(MeleeWeapon.class, "ability_target_range"));
            hero.belongings.abilityWeapon = null;
            return;
        }
        hero.belongings.abilityWeapon = null;

        hero.sprite.attack(enemy.pos, new Callback() {
            @Override
            public void call() {
                wep.beforeAbilityUsed(hero, enemy);
                AttackIndicator.target(enemy);
                if (hero.attack(enemy)) {
                    Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
                    if (!enemy.isAlive()){
                        wep.onAbilityKill(hero, enemy);
                        Buff.affect(hero, Charger.class).gainCharge(energy);
                    }
                }
                Invisibility.dispel();
                hero.next();
                wep.afterAbilityUsed(hero);
            }
        });
    }
}

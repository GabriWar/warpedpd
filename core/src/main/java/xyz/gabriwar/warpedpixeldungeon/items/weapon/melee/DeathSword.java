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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee;

import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

public class DeathSword extends MeleeWeapon {

    {
        image = ItemSpriteSheet.DEATHS_SWORD;
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 1f;
        levelKnown = true;

        tier = 4;
        unique = true;
        bones = false;
    }

    @Override
    public int max(int lvl) {
        return  5*(tier) +                	//20 base, down from 25
                Math.round(lvl*(tier+1));	//+5 per level
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (attacker instanceof Hero
                && damage >= defender.HP
                && defender instanceof Mob
                && ((Hero) attacker).lvl <= ((Mob) defender).maxLvl + 2) {
            Buff.affect(attacker, MaxHPBoost.class).kill();
        }
        return super.proc(attacker, defender, damage);
    }

    @Override
    public int level() {
        int level = Dungeon.hero == null ? 0 : Dungeon.hero.lvl/5;
        if (curseInfusionBonus) level += 1 + level/6;
        return level;
    }

    @Override
    public String desc() {
        if (Dungeon.hero != null && Dungeon.hero.buff(MaxHPBoost.class) != null) {
            int HTBoost = Dungeon.hero.buff(MaxHPBoost.class).HTBonus();
            return Messages.get(this, "desc_hero", HTBoost, 15+Dungeon.hero.lvl*5);
        } else {
            return Messages.get(this, "desc");
        }
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    //Re-ARranged weapon: no Duelist ability was ever designed for it

    public static class MaxHPBoost extends Buff {
        {
            type = buffType.POSITIVE;
        }

        private int stack = 0;

        public void kill() {
            stack = Math.min(stack+1, 15+Dungeon.hero.lvl*5);
            Dungeon.hero.updateHT(false);
        }

        public int HTBonus() {
            return stack;
        }

        private static final String STACK = "stack";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(STACK, stack);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            stack = bundle.getInt(STACK);
        }
    }

    @Override
    protected int baseChargeUse(Hero hero, Char target){
        return 2;
    }

    @Override
    public String targetingPrompt() {
        return Messages.get(this, "prompt");
    }

    private int boost(){ return augment.damageFactor( 6 + 2 * buffedLvl() ); }

    /** a reaping: a heavy certain cut, and a quarter of what it takes flows back into the wielder */
    @Override
    protected void duelistAbility( Hero hero, Integer target ){
        final Char enemy = aim( hero, target );
        if (enemy == null) return;
        final int boost = boost();
        hero.sprite.attack( enemy.pos, new Callback() {
            @Override
            public void call() {
                beforeAbilityUsed( hero, enemy );
                AttackIndicator.target( enemy );
                int before = enemy.HP + enemy.shielding();
                if (hero.attack( enemy, 1f, boost, Char.INFINITE_ACCURACY )){
                    int dealt = Math.max( 0, before - (enemy.isAlive() ? enemy.HP + enemy.shielding() : 0) );
                    int heal = Math.min( dealt / 4, hero.HT - hero.HP );
                    if (heal > 0){
                        hero.HP += heal;
                        hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), FloatingText.HEALING );
                    }
                    if (enemy.sprite != null) enemy.sprite.emitter().burst( ShadowParticle.UP, 6 );
                    Wound.hit( enemy );
                    SpatialSound.play( Assets.Sounds.HIT_STRONG, enemy, 1f, 0.8f );
                    SpatialSound.play( Assets.Sounds.GHOST, enemy, 0.7f, 1.2f );
                    if (!enemy.isAlive()) onAbilityKill( hero, enemy );
                }
                Invisibility.dispel();
                hero.spendAndNext( hero.attackDelay() );
                afterAbilityUsed( hero );
            }
        } );
    }

    @Override
    public String abilityInfo() {
        int b = 6 + 2 * buffedLvl();
        return Messages.get(this, "ability_desc", augment.damageFactor(min()+b), augment.damageFactor(max()+b));
    }

    public String upgradeAbilityStat(int level){
        int b = 6 + 2 * level;
        return augment.damageFactor(min(level)+b) + "-" + augment.damageFactor(max(level)+b);
    }

    //the enemy under the cursor, if it is one the hero can reach; null (with a message) if not
    private Char aim( Hero hero, Integer target ){
        if (target == null) return null;
        Char enemy = Actor.findChar( target );
        if (enemy == null || enemy == hero || hero.isCharmedBy( enemy ) || !Dungeon.level.heroFOV[target]){
            GLog.w( Messages.get( this, "ability_no_target" ) );
            return null;
        }
        hero.belongings.abilityWeapon = this;
        boolean can = hero.canAttack( enemy );
        hero.belongings.abilityWeapon = null;
        if (!can){
            GLog.w( Messages.get( this, "ability_target_range" ) );
            return null;
        }
        return enemy;
    }
}

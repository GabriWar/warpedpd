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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee;

import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfClairvoyance;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class Spade extends Shovel {
    {
        defaultAction = AC_DIG;

        image = ItemSpriteSheet.BATTLE_SHOVEL;
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 1.1f;

        tier = 5;
        DLY = 0.8f; //1.25x speed

        unique = true;
        bones = false;
    }

    @Override
    public int max(int lvl) {
        return  5*(tier+1) +
                lvl*(tier+1);
    }

    // ---- Duelist: Excavate. A boulder is dug out of the floor and hurled at an ----
    // ---- enemy up to 3 tiles off: a heavy hit that shoves the target back a tile ----
    // ---- and buries the ground in dirt. Shovel has none; the battle spade earns one. ----

    private static final int THROW_RANGE = 3;

    @Override
    public boolean hasDuelistAbility() {
        return true;
    }

    @Override
    protected int baseChargeUse(Hero hero, Char target){
        return 2;
    }

    @Override
    public String targetingPrompt() {
        return Messages.get(this, "prompt");
    }

    protected int boulderBoost(){
        return 6 + buffedLvl();
    }

    @Override
    protected void duelistAbility(Hero hero, Integer target) {
        if (target == null) return;
        Char enemy = Actor.findChar(target);
        if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
            GLog.w(Messages.get(this, "ability_no_target"));
            return;
        }
        Ballistica traj = new Ballistica(hero.pos, target, Ballistica.PROJECTILE);
        if (traj.collisionPos.intValue() != target.intValue() || Dungeon.level.distance(hero.pos, target) > THROW_RANGE){
            GLog.w(Messages.get(this, "ability_target_range"));
            return;
        }
        beforeAbilityUsed(hero, enemy);

        //dug out of the floor, then thrown: dirt at the feet, the stone in flight
        CellEmitter.bottom(hero.pos).burst(EarthParticle.FACTORY, 8);
        Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1f, 0.9f);
        hero.sprite.zap(target);
        Item look = new StoneOfClairvoyance();
        int dmg = augment.damageFactor(damageRoll(hero)) + augment.damageFactor(boulderBoost());
        dmg = proc(hero, enemy, dmg);
        enemy.damage(dmg, this);
        SkillFX.streak(hero.sprite, target, look, () -> {
            SkillFX.flash(enemy);
            Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 1.1f);
        });
        CellEmitter.get(target).burst(EarthParticle.FACTORY, 6);
        Camera.main.shake(2, 0.3f);
        if (enemy.isAlive()){
            Ballistica push = new Ballistica(enemy.pos, enemy.pos + (enemy.pos - hero.pos), Ballistica.MAGIC_BOLT);
            WandOfBlastWave.throwChar(enemy, push, 1, false, true, this);
        } else {
            onAbilityKill(hero, enemy);
        }

        Invisibility.dispel();
        hero.spendAndNext(hero.attackDelay());
        afterAbilityUsed(hero);
    }

    @Override
    public String abilityInfo() {
        int boost = levelKnown ? boulderBoost() : 6;
        if (levelKnown){
            return Messages.get(this, "ability_desc", augment.damageFactor(min()+boost), augment.damageFactor(max()+boost), THROW_RANGE);
        } else {
            return Messages.get(this, "typical_ability_desc", min(0)+boost, max(0)+boost, THROW_RANGE);
        }
    }

    @Override
    public String upgradeAbilityStat(int level){
        int boost = 6 + level;
        return augment.damageFactor(min(level)+boost) + "-" + augment.damageFactor(max(level)+boost);
    }
}

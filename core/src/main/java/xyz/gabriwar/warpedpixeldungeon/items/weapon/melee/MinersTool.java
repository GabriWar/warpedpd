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

import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfClairvoyance;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import com.watabou.utils.PathFinder;
import com.watabou.noosa.Camera;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class MinersTool extends Spade {
    {
        defaultAction = AC_DIG;

        image = ItemSpriteSheet.MINERS_TOOL;
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 1.1f;

        tier = 5;
        DLY = 0.8f; //1.25x speed

        unique = true;
        bones = false;
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        int dmg = super.proc( attacker, defender, damage );
        if (defender.properties().contains(Char.Property.INORGANIC)) dmg *= 1.25f;
        return dmg;
    }

    // ---- Duelist: Collapse. The pick is driven into the ceiling and the roof comes ----
    // ---- down on every enemy within 2 tiles: rocks fall, the screen shakes, and ----
    // ---- what stands under the rubble is crippled. Stronger against the inorganic. ----

    private static final int COLLAPSE_RANGE = 2;

    @Override
    public String targetingPrompt() {
        return null;
    }

    private int rockBoost(){
        return 4 + buffedLvl();
    }

    @Override
    protected void duelistAbility(Hero hero, Integer target) {
        ArrayList<Char> struck = new ArrayList<>();
        for (Char ch : Actor.chars()){
            if (ch != hero && ch.alignment == Char.Alignment.ENEMY && !hero.isCharmedBy(ch)
                    && ch.isAlive() && Dungeon.level.heroFOV[ch.pos]
                    && Dungeon.level.distance(hero.pos, ch.pos) <= COLLAPSE_RANGE){
                struck.add(ch);
            }
        }
        if (struck.isEmpty()){
            GLog.w(Messages.get(this, "ability_no_target"));
            return;
        }
        beforeAbilityUsed(hero, null);

        hero.sprite.operate(hero.pos);
        SpatialSound.play(Assets.Sounds.ROCKS, hero, 1f, 0.8f);
        Camera.main.shake(4, 0.6f);
        Item look = new StoneOfClairvoyance();
        for (int n : PathFinder.NEIGHBOURS9){
            int c = hero.pos + n;
            if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c] && Dungeon.level.heroFOV[c]){
                CellEmitter.get(c).burst(EarthParticle.FACTORY, 3);
            }
        }
        for (Char ch : struck){
            SkillFX.rain(ch.pos, look, 2, null);
            CellEmitter.get(ch.pos).burst(Speck.factory(Speck.ROCK), 4);
            int dmg = Math.round(augment.damageFactor(damageRoll(hero)) * 0.75f) + augment.damageFactor(rockBoost());
            dmg = proc(hero, ch, dmg);
            ch.damage(dmg, this);
            SkillFX.flash(ch);
            if (ch.isAlive()){
                Buff.prolong(ch, Cripple.class, 2f);
            } else {
                onAbilityKill(hero, ch);
            }
        }

        Invisibility.dispel();
        hero.spendAndNext(hero.attackDelay());
        afterAbilityUsed(hero);
    }

    @Override
    public String abilityInfo() {
        int boost = levelKnown ? rockBoost() : 4;
        if (levelKnown){
            return Messages.get(this, "ability_desc", Math.round(augment.damageFactor(min())*0.75f)+boost, Math.round(augment.damageFactor(max())*0.75f)+boost, COLLAPSE_RANGE);
        } else {
            return Messages.get(this, "typical_ability_desc", Math.round(min(0)*0.75f)+boost, Math.round(max(0)*0.75f)+boost, COLLAPSE_RANGE);
        }
    }

    @Override
    public String upgradeAbilityStat(int level){
        int boost = 4 + level;
        return (Math.round(augment.damageFactor(min(level))*0.75f)+boost) + "-" + (Math.round(augment.damageFactor(max(level))*0.75f)+boost);
    }
}

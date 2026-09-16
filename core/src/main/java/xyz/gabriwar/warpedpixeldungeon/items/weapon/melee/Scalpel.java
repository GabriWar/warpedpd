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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

public class Scalpel extends MeleeWeapon {

    {
        image = ItemSpriteSheet.SCALPEL;
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 1.1f;

        tier = 1;

        bones = false;
    }

    @Override
    public int max(int lvl) {
        return  4*(tier+1) +
                lvl*(tier+1);
    }

    @Override
    public int proc(Char attacker, Char defender, int damage ) {
        Buff.affect(defender, Bleeding.class).set(Random.NormalIntRange(1, 3));
        return super.proc( attacker, defender, damage );
    }

    @Override
    public String targetingPrompt() {
        return Messages.get(this, "prompt");
    }

    @Override
    protected void duelistAbility(Hero hero, Integer target) {
        int bleedAmt = augment.damageFactor(Math.round(6f + 2f*buffedLvl()));
        Sickle.harvestAbility(hero, target, 0f, bleedAmt, this);
    }

    @Override
    public String abilityInfo() {
        int bleedAmt = levelKnown ? Math.round(6f + 2f*buffedLvl()) : 6;
        if (levelKnown){
            return Messages.get(this, "ability_desc", augment.damageFactor(bleedAmt));
        } else {
            return Messages.get(this, "typical_ability_desc", bleedAmt);
        }
    }

    @Override
    public String upgradeAbilityStat(int level) {
        return Integer.toString(augment.damageFactor(Math.round(6f + 2f*buffedLvl())));
    }
}

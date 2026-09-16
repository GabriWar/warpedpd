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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.alchemy;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.ThunderBolt;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.ShockingBrew;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfSharpshooting;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRecharging;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ForceCube;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class PotOThunder extends MissileWeapon {
    {
        image = ItemSpriteSheet.THUNDERBOLT;
        hitSound = Assets.Sounds.LIGHTNING;
        hitSoundPitch = 1.2f;

        tier = 5;
        sticky = true;

        baseUses = 10;
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        Buff.affect(defender, Paralysis.class, Random.NormalIntRange(3, 5));

        defender.damage(magicDamage(this.buffedLvl()), new Electricity());
        ThunderBolt.thunderEffect(defender.sprite);

        CharSprite s = defender.sprite;
        if (s != null && s.parent != null) {
            ArrayList<Lightning.Arc> arcs = new ArrayList<>();
            arcs.add(new Lightning.Arc(new PointF(s.x, s.y + s.height / 2), new PointF(s.x + s.width, s.y + s.height / 2)));
            arcs.add(new Lightning.Arc(new PointF(s.x + s.width / 2, s.y), new PointF(s.x + s.width / 2, s.y + s.height)));
            s.parent.add(new Lightning(arcs, null));
        }

        return super.proc(attacker, defender, damage);
    }

    @Override
    public String info() {
        String info = super.info();

        info += "\n\n" + Messages.get(this, "magic_damage", magicMin(buffedLvl()), magicMax(buffedLvl()));

        return info;
    }

    @Override
    public int min(int lvl) {
        return 10;
    }

    @Override
    public int max(int lvl) {
        return  10;
    }

    public int magicMin(int lvl) {
        if (Dungeon.hero != null){
            return tier+lvl+RingOfSharpshooting.levelDamageBonus(Dungeon.hero);
        } else {
            return tier+lvl;
        }
    }

    public int magicMax(int lvl) {
        if (Dungeon.hero != null){
            return (5+lvl+RingOfSharpshooting.levelDamageBonus(Dungeon.hero))*tier;
        } else {
            return (5+lvl)*tier;
        }
    }

    public int magicDamage(int lvl) { //magic damage
        return Random.NormalIntRange(magicMin(lvl), magicMax(lvl));
    }

    public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {
        {
            inputs =  new Class[]{ShockingBrew.class, ScrollOfRecharging.class, ForceCube.class};
            inQuantity = new int[]{1, 1, 1};

            cost = 3;

            output = PotOThunder.class;
            outQuantity = 1;
        }
    }
}

package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
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
    @Override
    public boolean hasDuelistAbility() {
        return false;
    }

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
}

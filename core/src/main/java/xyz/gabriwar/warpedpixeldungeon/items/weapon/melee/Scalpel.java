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

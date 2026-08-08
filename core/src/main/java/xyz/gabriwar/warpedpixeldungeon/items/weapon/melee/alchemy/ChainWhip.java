package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Evolution;
import xyz.gabriwar.warpedpixeldungeon.items.spells.UpgradeDust;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Glaive;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Whip;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

public class ChainWhip extends MeleeWeapon implements AlchemyWeapon {

    {
        image = ItemSpriteSheet.CHAIN_WHIP;
        hitSound = Assets.Sounds.HIT_CRUSH;
        hitSoundPitch = 1f;

        tier = 5;
        RCH = 4;    //lots of extra reach
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        if (Random.Float() < 0.1f+0.02f*buffedLvl()) {
            Buff.affect( defender, Paralysis.class, 3f);
        }
        return super.proc( attacker, defender, damage );
    }

    @Override
    public int max(int lvl) {
        return  5*(tier-1) +
                lvl*(tier-1);
    }

    @Override
    protected void duelistAbility(Hero hero, Integer target) {

        ArrayList<Char> targets = new ArrayList<>();
        Char closest = null;

        hero.belongings.abilityWeapon = this;
        for (Char ch : Actor.chars()){
            if (ch.alignment == Char.Alignment.ENEMY
                    && !hero.isCharmedBy(ch)
                    && Dungeon.level.heroFOV[ch.pos]
                    && hero.canAttack(ch)){
                targets.add(ch);
                if (closest == null || Dungeon.level.trueDistance(hero.pos, closest.pos) > Dungeon.level.trueDistance(hero.pos, ch.pos)){
                    closest = ch;
                }
            }
        }
        hero.belongings.abilityWeapon = null;

        if (targets.isEmpty()) {
            GLog.w(Messages.get(this, "ability_no_target"));
            return;
        }

        throwSound();
        Char finalClosest = closest;
        hero.sprite.attack(hero.pos, new Callback() {
            @Override
            public void call() {
                beforeAbilityUsed(hero, finalClosest);
                //roughly +10% base damage
                int dmgBoost = augment.damageFactor(Math.round(0.1f*max(buffedLvl())));
                for (Char ch : targets) {
                    hero.attack(ch, 1, dmgBoost, Char.INFINITE_ACCURACY);
                    if (!ch.isAlive()){
                        onAbilityKill(hero, ch);
                    }
                }
                Invisibility.dispel();
                hero.spendAndNext(hero.attackDelay());
                afterAbilityUsed(hero);
            }
        });
    }

    @Override
    public String abilityInfo() {
        int dmgBoost = levelKnown ? Math.round(0.1f*max()) : Math.round(0.1f*max(0));
        if (levelKnown){
            return Messages.get(this, "ability_desc", augment.damageFactor(min()+dmgBoost), augment.damageFactor(max()+dmgBoost));
        } else {
            return Messages.get(this, "typical_ability_desc", min(0)+dmgBoost, max(0)+dmgBoost);
        }
    }

    @Override
    public ArrayList<Class<?extends Item>> weaponRecipe() {
        return new ArrayList<>(Arrays.asList(Whip.class, UpgradeDust.class, Evolution.class));
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

}

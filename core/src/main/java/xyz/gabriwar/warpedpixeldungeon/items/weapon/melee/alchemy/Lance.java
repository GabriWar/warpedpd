package xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy;

import static xyz.gabriwar.warpedpixeldungeon.Dungeon.hero;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Evolution;
import xyz.gabriwar.warpedpixeldungeon.items.spells.UpgradeDust;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Glaive;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RunicBlade;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Door;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class Lance extends MeleeWeapon implements AlchemyWeapon {

    {
        image = ItemSpriteSheet.LANCE;
        hitSound = Assets.Sounds.HIT_STAB;
        hitSoundPitch = 1.2f;

        tier = 6;
        RCH = 2;
    }

    @Override
    public int max(int lvl) {
        return  4*(tier) +    //24 base
                lvl*(tier);     //scaling +6 per +1
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        LanceBuff buff = attacker.buff(LanceBuff.class);
        if (buff != null) {
            damage *= 1+buff.getDamageFactor();
            buff.detach();
        }
        return super.proc( attacker, defender, damage );
    }

    @Override
    public String targetingPrompt() {
        return Messages.get(this, "prompt");
    }

    @Override
    protected void duelistAbility(Hero hero, Integer target) {
        usesTargeting = false;
        dashAbility(hero, target, this);
    }

    public static void dashAbility(Hero hero, Integer target, MeleeWeapon wep){
        if (target == null || target == -1){
            return;
        }

        int range = 3+wep.buffedLvl();

        if (Dungeon.level.distance(hero.pos, target) > range){
            GLog.w(Messages.get(Lance.class, "ability_bad_position"));
            return;
        }

        Ballistica dash = new Ballistica(hero.pos, target, Ballistica.PROJECTILE);

        if (!dash.collisionPos.equals(target)
                || Actor.findChar(target) != null
                || (Dungeon.level.solid[target] && !Dungeon.level.passable[target])){
            GLog.w(Messages.get(Lance.class, "ability_bad_position"));
            return;
        }
        wep.beforeAbilityUsed(hero, null);

        Buff.affect(hero, LanceBuff.class).setDamageFactor(1+Dungeon.level.distance(hero.pos, target), hero.belongings.secondWep == wep);

        hero.busy();
        Sample.INSTANCE.play(Assets.Sounds.MISS);
        hero.sprite.emitter().start(Speck.factory(Speck.JET), 0.01f, Math.round(4 + 2*Dungeon.level.trueDistance(hero.pos, target)));
        hero.sprite.jump(hero.pos, target, 0, 0.1f, new Callback() {
            @Override
            public void call() {
                if (Dungeon.level.map[hero.pos] == Terrain.OPEN_DOOR) {
                    Door.leave( hero.pos );
                }
                hero.pos = target;
                Dungeon.level.occupyCell(hero);
                hero.next();
            }
        });
        wep.afterAbilityUsed(hero);
    }

    @Override
    public String abilityInfo() {
        int range = levelKnown ? 3+buffedLvl() : 3;
        if (levelKnown){
            return Messages.get(this, "ability_desc", range);
        } else {
            return Messages.get(this, "typical_ability_desc", range);
        }
    }

    public static class LanceBuff extends Buff {

        {
            type = buffType.NEUTRAL;
            announced = false;
        }

        float damageFactor = 0;
        float maxDamage    = 2f;

        boolean secondWep = false;
        public void setDamageFactor(float amount, boolean isSecond) {
            secondWep = isSecond;
            if (damageFactor < maxDamage && (damageFactor + 0.05f * amount) < maxDamage ) {
                damageFactor += 0.05f * amount;
            } else {
                damageFactor = maxDamage;
            }
        }

        public float getDamageFactor() {
            return damageFactor;
        }

        public float duration() {
            return visualcooldown();
        }

        @Override
        public int icon() {
            return BuffIndicator.WEAPON;
        }

        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0.2f, 1f, 0.2f);
        }

        @Override
        public float iconFadePercent() {
            return Math.max(0, (maxDamage - damageFactor / maxDamage) - 1);
        }

        Item item = null;
        @Override
        public boolean act() {
            damageFactor-=0.05f*TICK;
            spend(TICK);
            if (damageFactor <= 0) {
                detach();
            }
            if (item == null) {
                if (secondWep) {
                    item = hero.belongings.secondWep;
                } else {
                    item = hero.belongings.weapon;
                }
            }
            if (item != hero.belongings.weapon && item != hero.belongings.secondWep) {
                detach();
            }
            return true;
        }

        @Override
        public String toString() {
            return Messages.get(this, "name");
        }

        @Override
        public String desc() {
            return Messages.get(this, "desc", new DecimalFormat("#.##").format(1+getDamageFactor()));
        }

        private static final String DAMAGE =        "damage";
        private static final String MAXDAMAGE =     "maxdamage";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(DAMAGE, damageFactor);
            bundle.put(MAXDAMAGE, maxDamage);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            damageFactor = bundle.getFloat(DAMAGE);
            maxDamage = bundle.getFloat(MAXDAMAGE);
        }
    }

    @Override
    public ArrayList<Class<?extends Item>> weaponRecipe() {
        return new ArrayList<>(Arrays.asList(Glaive.class, UpgradeDust.class, Evolution.class));
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

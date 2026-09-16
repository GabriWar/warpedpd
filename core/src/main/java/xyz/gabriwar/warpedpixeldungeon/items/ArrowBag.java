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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.Artifact;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.bags.PotionBandolier;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfMastery;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HolyWater;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArrowBag extends Artifact {
    {
        image = ItemSpriteSheet.ARROW_BAG;
        levelKnown = true;
        unique = true;
        bones = false;
        levelCap = 10;
        defaultAction = AC_INJECT;
    }

    private static final String AC_INJECT = "INJECT", AC_EFFECTS = "EFFECTS", AC_MODE = "MODE";
    public static final int MAX_EFFECTS = 4;
    private ArrayList<Effect> effects = new ArrayList<>();
    private boolean randomMode;
    private int cycleIndex;
    private transient boolean processingInfusion;

    public static class Effect implements Bundlable {
        public Item ingredient;
        public int charges;
        public Effect() {}
        Effect(Item ingredient, int charges) { this.ingredient = ingredient; this.charges = charges; }
        public String name() {
            String name = Messages.get(ingredient.getClass(), "name");
            return name.contains("NO TEXT FOUND") ? ingredient.getClass().getSimpleName() : name;
        }
        public String description() {
            if (ingredient instanceof Potion) return ((Potion)ingredient).infusionDesc();
            if (ingredient instanceof Plant.Seed) return ((Plant.Seed)ingredient).coatingDesc();
            return Messages.get(ArrowBag.class, "holy_effect");
        }
        public int color() {
            if (ingredient instanceof Potion) {
                ItemSprite.Glowing glow = ((Potion)ingredient).potionGlowing();
                return glow == null ? 0xB77CFF : glow.color;
            }
            if (ingredient instanceof Plant.Seed && ((Plant.Seed)ingredient).poisonEmitterClass() != null)
                return ((Plant.Seed)ingredient).poisonEmitterClass().getColor();
            return 0xFFF2AD;
        }
        @Override public void storeInBundle(Bundle bundle) { bundle.put("ingredient", ingredient); bundle.put("charges", charges); }
        @Override public void restoreFromBundle(Bundle bundle) { ingredient = (Item)bundle.get("ingredient"); charges = bundle.getInt("charges"); }
    }

    public List<Effect> effects() { return Collections.unmodifiableList(effects); }
    public int usesPerInfusion() { return 15 + 3 * level(); }
    public boolean infused() { return !effects.isEmpty(); }
    public boolean randomMode() { return randomMode; }
    public void randomMode(boolean enabled) { randomMode = enabled; updateQuickslot(); }
    public int conservationChance() { return Math.max(0, Math.min(100, level() * 10)); }
    protected boolean preserveCharge() { return Random.Int(100) < conservationChance(); }

    private static boolean isIngredient(Item item) {
        return item instanceof Potion || item instanceof Plant.Seed || item instanceof HolyWater;
    }
    private Effect matching(Item ingredient) {
        for (Effect effect : effects) if (effect.ingredient.getClass() == ingredient.getClass()) return effect;
        return null;
    }
    public boolean canInfuse(Item item) {
        if (!isIngredient(item)) return false;
        if (item instanceof PotionOfStrength || item instanceof PotionOfMastery) return level() < levelCap;
        return matching(item) != null || effects.size() < MAX_EFFECTS;
    }
    /** The caller supplies one already-detached ingredient. Same-type refills preserve slot order. */
    public boolean addEffect(Item item) {
        if (!canInfuse(item)) return false;
        if (item instanceof PotionOfStrength || item instanceof PotionOfMastery) { upgrade(); return true; }
        int charges = item instanceof Potion ? ((Potion)item).infusionUses(usesPerInfusion()) : usesPerInfusion();
        Effect match = matching(item);
        if (match != null) match.charges = (int)Math.min(Integer.MAX_VALUE, (long)match.charges + charges);
        else effects.add(new Effect(item, charges));
        updateQuickslot();
        return true;
    }
    public void removeEffect(int index) {
        if (index < 0 || index >= effects.size()) return;
        effects.remove(index);
        if (index < cycleIndex) cycleIndex--;
        if (cycleIndex >= effects.size()) cycleIndex = 0;
        updateQuickslot();
    }

    @Override public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_INJECT); actions.add(AC_EFFECTS); actions.add(AC_MODE);
        return actions;
    }
    @Override public void execute(Hero hero, String action) {
        super.execute(hero, action);
        if (action.equals(AC_INJECT)) GameScene.selectItem(itemSelector);
        else if (action.equals(AC_EFFECTS)) manageEffects();
        else if (action.equals(AC_MODE)) GameScene.show(new WndOptions(new ItemSprite(this), name(),
                Messages.get(this, "mode_desc"), Messages.get(this, "cycle"), Messages.get(this, "random")) {
            @Override protected void onSelect(int index) { if (index == 0 || index == 1) randomMode(index == 1); }
        });
    }
    private void manageEffects() {
        if (effects.isEmpty()) {
            GameScene.show(new WndTitledMessage(new ItemSprite(this), name(), Messages.get(this, "empty")));
            return;
        }
        final ArrayList<Effect> snapshot = new ArrayList<>(effects);
        String[] labels = new String[snapshot.size()];
        for (int i=0; i<labels.length; i++) labels[i] = snapshot.get(i).name() + " (" + snapshot.get(i).charges + ")";
        GameScene.show(new WndOptions(new ItemSprite(this), name(), Messages.get(this, "manage_desc"), labels) {
            @Override protected boolean hasInfo(int index) { return index >= 0 && index < snapshot.size(); }
            @Override protected void onInfo(int index) {
                Effect effect = snapshot.get(index);
                GameScene.show(new WndTitledMessage(new ItemSprite(effect.ingredient), effect.name(), effect.description()));
            }
            @Override protected void onSelect(int index) {
                if (index < 0 || index >= snapshot.size()) return;
                Effect effect = snapshot.get(index);
                GameScene.show(new WndOptions(new ItemSprite(effect.ingredient), effect.name(),
                        Messages.get(ArrowBag.class, "remove_desc", effect.charges),
                        Messages.get(ArrowBag.class, "remove"), Messages.get(ArrowBag.class, "cancel")) {
                    @Override protected void onSelect(int choice) {
                        if (choice == 0) removeEffect(effects.indexOf(effect));
                    }
                });
            }
        });
    }

    @Override public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put("effects", effects); bundle.put("random_mode", randomMode); bundle.put("cycle_index", cycleIndex);
    }
    @Override public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        effects = new ArrayList<>();
        if (bundle.contains("effects")) {
            for (Bundlable saved : bundle.getCollection("effects")) {
                if (!(saved instanceof Effect)) continue;
                Effect effect = (Effect)saved;
                if (isIngredient(effect.ingredient) && effect.charges > 0 && effects.size() < MAX_EFFECTS) effects.add(effect);
            }
        } else {
            // Preserve the infusion and remaining charges from pre-four-slot saves.
            Item ingredient = (Item)bundle.get("potion");
            if (ingredient == null) ingredient = (Item)bundle.get("seed");
            if (ingredient == null && bundle.getBoolean("holy")) ingredient = new HolyWater();
            int charges = bundle.getInt("uses");
            if (ingredient != null && charges > 0) effects.add(new Effect(ingredient, charges));
        }
        randomMode = bundle.getBoolean("random_mode");
        cycleIndex = effects.isEmpty() ? 0 : Math.floorMod(bundle.getInt("cycle_index"), effects.size());
    }
    @Override public ItemSprite.Glowing glowing() {
        return infused() ? new ItemSprite.Glowing(effects.get(Math.min(cycleIndex, effects.size()-1)).color(), .8f) : null;
    }

    public int proc(Hero hero, Char enemy, int damage) {
        if (processingInfusion || enemy == null || !infused() || !isEquipped(hero)) return damage;
        processingInfusion = true;
        int index = randomMode ? Random.Int(effects.size()) : cycleIndex;
        Effect effect = effects.get(index);
        try {
            if (!preserveCharge()) effect.charges--;
            if (Dungeon.level.heroFOV[enemy.pos]) {
                xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter.center(enemy.pos).burst(
                        xyz.gabriwar.warpedpixeldungeon.effects.particles.InfusionParticle.factory(effect.color()), 12);
            }
            Item ingredient = effect.ingredient;
            if (ingredient instanceof Potion) ((Potion)ingredient).potionProc(hero, enemy, damage);
            else if (ingredient instanceof Plant.Seed) ((Plant.Seed)ingredient).onProc(hero, enemy, damage);
            else HolyWater.blessProc(hero, enemy, damage);
            float multiplier = 1.1f + .02f * level();
            if (level() < levelCap && ++exp >= 10 + 5 * level()) {
                exp = 0; upgrade(); GLog.p(Messages.get(this, "levelup"));
            }
            return Math.round(damage * multiplier);
        } finally {
            if (effect.charges <= 0) {
                effects.remove(index);
                cycleIndex = effects.isEmpty() ? 0 : index % effects.size();
            } else cycleIndex = (index + 1) % effects.size();
            processingInfusion = false;
            updateQuickslot();
        }
    }

    @Override public String desc() {
        String desc = super.desc() + "\n\n" + Messages.get(this, "settings", effects.size(), MAX_EFFECTS,
                Messages.get(this, randomMode ? "random" : "cycle"), conservationChance());
        for (int i=0; i<effects.size(); i++) {
            Effect effect = effects.get(i);
            desc += "\n\n" + (i+1) + ". " + effect.name() + " (" + effect.charges + ")";
            if (!randomMode && i == cycleIndex) desc += " " + Messages.get(this, "next");
            desc += "\n" + effect.description();
        }
        return desc;
    }
    protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {
        @Override public String textPrompt() { return Messages.get(ArrowBag.class, "select_title"); }
        @Override public Class<? extends Bag> preferredBag() { return PotionBandolier.class; }
        @Override public boolean itemSelectable(Item item) { return canInfuse(item); }
        @Override public void onSelect(Item item) {
            if (item == null || !canInfuse(item) || !Dungeon.hero.belongings.contains(ArrowBag.this)) return;
            item = item.detach(Dungeon.hero.belongings.backpack);
            if (item == null) return;
            addEffect(item);
            Dungeon.hero.sprite.operate(Dungeon.hero.pos);
            Sample.INSTANCE.play(Assets.Sounds.DRINK);
            updateQuickslot();
        }
    };
    @Override public boolean isUpgradable() { return false; }
    @Override public boolean isIdentified() { return true; }
    @Override public int value() { return 50; }
    @Override public String status() {
        long charges = 0;
        for (Effect effect : effects) charges += effect.charges;
        return Long.toString(charges);
    }
}

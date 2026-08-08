package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.bags.PotionBandolier;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfExperience;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHaste;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfInvisibility;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLevitation;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLiquidFlame;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMindVision;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfParalyticGas;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfPurity;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfToxicGas;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.AquaBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.BlizzardBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.Brew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.CausticBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.InfernalBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.ShockingBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.UnstableBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.Elixir;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.ExoticPotion;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfCleansing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfCorrosiveGas;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfDivineInspiration;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfDragonsBreath;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfEarthenArmor;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfMagicalSight;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfMastery;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfShielding;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfShroudingFog;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfSnapFreeze;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfStamina;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfStormClouds;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class ArrowBag extends Item {
    {
        image = ItemSpriteSheet.ARROW_BAG;
        levelKnown = true;
        unique = true;
        bones = false;

        defaultAction = AC_INJECT;
    }

    private static final String AC_INJECT = "INJECT";

    private static final String TXT_STATUS	= "%d";

    public Potion potion;
    public int uses;

    @Override
    public ArrayList<String> actions(Hero hero ) {
        ArrayList<String> actions = super.actions( hero );
        actions.add(AC_INJECT);
        return actions;
    }

    @Override
    public void execute( Hero hero, String action ) {

        super.execute( hero, action );

        if (action.equals(AC_INJECT)) {
            GameScene.selectItem(itemSelector);
        }
    }

    private static final String POTION	    = "potion";
    private static final String USES	    = "uses";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);

        bundle.put( POTION, potion );
        bundle.put( USES, uses );
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);

        potion = (Potion)bundle.get( POTION );
        uses = bundle.getInt( USES );
    }

    @Override
    public ItemSprite.Glowing glowing() {
        if (potion != null) {
            return potion.potionGlowing();
        } else {
            return null;
        }
    }

    public int proc(Hero hero, Char enemy, int damage) {
        float dmg = damage;
        if (potion != null) {
            potion.potionProc(hero, enemy, dmg);
            uses--;
            dmg *= (1.1f);
        }

        if (uses <= 0) {
            potion = null;
            updateQuickslot();
        }

        return Math.round(dmg);
    }

    @Override
    public String desc() {
        String desc = super.desc();

        if (potion != null) {
            desc += "\n\n" + Messages.get(this, "potion_desc", potion.name(), uses);
            desc += "\n";

            if (potion instanceof ExoticPotion) {
                if (potion instanceof PotionOfShielding) {
                    desc += Messages.get(this, "desc_shielding");
                }
                if (potion instanceof PotionOfMagicalSight) {
                    desc += Messages.get(this, "desc_magicalsight");
                }
                if (potion instanceof PotionOfSnapFreeze) {
                    desc += Messages.get(this, "desc_snapfreeze");
                }
                if (potion instanceof PotionOfDragonsBreath) {
                    desc += Messages.get(this, "desc_dragonsbreath");
                }
                if (potion instanceof PotionOfCorrosiveGas) {
                    desc += Messages.get(this, "desc_corrosivegas");
                }
                if (potion instanceof PotionOfStamina) {
                    desc += Messages.get(this, "desc_stamina");
                }
                if (potion instanceof PotionOfShroudingFog) {
                    desc += Messages.get(this, "desc_shroudingfog");
                }
                if (potion instanceof PotionOfStormClouds) {
                    desc += Messages.get(this, "desc_stormclouds");
                }
                if (potion instanceof PotionOfEarthenArmor) {
                    desc += Messages.get(this, "desc_earthenarmor");
                }
                if (potion instanceof PotionOfCleansing) {
                    desc += Messages.get(this, "desc_cleansing");
                }
                if (potion instanceof PotionOfDivineInspiration) {
                    desc += Messages.get(this, "desc_divineinspiration");
                }
            } else if (potion instanceof Brew) {
                if (potion instanceof AquaBrew) {
                    desc += Messages.get(this, "desc_aqua");
                }
                if (potion instanceof BlizzardBrew) {
                    desc += Messages.get(this, "desc_blizzard");
                }
                if (potion instanceof CausticBrew) {
                    desc += Messages.get(this, "desc_caustic");
                }
                if (potion instanceof InfernalBrew) {
                    desc += Messages.get(this, "desc_infernal");
                }
                if (potion instanceof ShockingBrew) {
                    desc += Messages.get(this, "desc_shocking");
                }
                if (potion instanceof UnstableBrew) {
                    desc += Messages.get(this, "desc_unstable");
                }
            } else {
                if (potion instanceof PotionOfHealing) {
                    desc += Messages.get(this, "desc_healing");
                }
                if (potion instanceof PotionOfMindVision) {
                    desc += Messages.get(this, "desc_mindvision");
                }
                if (potion instanceof PotionOfFrost) {
                    desc += Messages.get(this, "desc_frost");
                }
                if (potion instanceof PotionOfLiquidFlame) {
                    desc += Messages.get(this, "desc_liquidflame");
                }
                if (potion instanceof PotionOfToxicGas) {
                    desc += Messages.get(this, "desc_toxicgas");
                }
                if (potion instanceof PotionOfHaste) {
                    desc += Messages.get(this, "desc_haste");
                }
                if (potion instanceof PotionOfInvisibility) {
                    desc += Messages.get(this, "desc_invisibility");
                }
                if (potion instanceof PotionOfLevitation) {
                    desc += Messages.get(this, "desc_levitation");
                }
                if (potion instanceof PotionOfParalyticGas) {
                    desc += Messages.get(this, "desc_paralyticgas");
                }
                if (potion instanceof PotionOfPurity) {
                    desc += Messages.get(this, "desc_purity");
                }
                if (potion instanceof PotionOfExperience) {
                    desc += Messages.get(this, "desc_experience");
                }
            }
        }
        return desc;
    }

    protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

        @Override
        public String textPrompt() {
            return Messages.get(ArrowBag.class, "select_title");
        }

        @Override
        public Class<? extends Bag> preferredBag() {
            return PotionBandolier.class;
        }

        @Override
        public boolean itemSelectable(Item item) {
            return item.isIdentified()
                    && item instanceof Potion
                    && !(item instanceof Elixir)
                    && !(item instanceof ExoticPotion || item instanceof Brew);
        }

        @Override
        public void onSelect(Item item) {
            if (item == null) return;

            item.detach(Dungeon.hero.belongings.backpack);
            if (item instanceof PotionOfStrength || item instanceof PotionOfMastery) {
                upgrade();
            } else {
                potion = (Potion)item;
                uses = Math.max(1, 15 + 3 * level());
            }

            Dungeon.hero.sprite.operate(Dungeon.hero.pos);
            Sample.INSTANCE.play(Assets.Sounds.DRINK);

            updateQuickslot();
        }
    };

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    @Override
    public int value() {
        //Re-ARranged handed this out with the Archer's starting kit and left it priceless (-1).
        //Warped sells it instead, so it needs a real price, in line with the container bags.
        return 50;
    }

    @Override
    public String status() {
        return Messages.format( TXT_STATUS, uses );
    }
}

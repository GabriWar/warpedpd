/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items.food;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Coughing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.Artifact;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.HornOfPlenty;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.bags.VelvetPouch;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class Food extends Item {

	public static final float TIME_TO_EAT	= 3f;
	public static final float TIME_TO_SPICE	= 2f;

	public static final String AC_EAT	= "EAT";
	public static final String AC_SPICE	= "SPICE";

	public float energy = Hunger.HUNGRY;

	// Seed used to spice this food; eating applies the seed's spiceEffect
	public Plant.Seed seed = null;

	private static final String SEED = "SPICE";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(SEED, seed);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		seed = (Plant.Seed) bundle.get(SEED);
	}

	{
		stackable = true;
		image = ItemSpriteSheet.RATION;

		defaultAction = AC_EAT;

		bones = true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_EAT );
		if (seed == null) actions.add( AC_SPICE );
		return actions;
	}

	@Override
	public String name() {
		if (seed != null) {
			return super.name() + " " + Messages.get(Food.class, "spiced_with").toLowerCase() + " " + seed.name();
		}
		return super.name();
	}

	@Override
	public boolean isSimilar(Item item) {
		// spiced food doesn't stack with unspiced food
		return super.isSimilar(item)
				&& (seed == null) == (((Food) item).seed == null)
				&& (seed == null || seed.getClass() == ((Food) item).seed.getClass());
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_EAT )) {

			if (hero.buff(Coughing.class) != null){
				GLog.n(Messages.get(this, "coughing"));
				return;
			}

			detach( hero.belongings.backpack );
			Catalog.countUse(getClass());

			satisfy(hero);
			GLog.i( Messages.get(this, "eat_msg") );

			eatEffect( hero );

			hero.sprite.operate( hero.pos );
			hero.busy();
			SpellSprite.show( hero, SpellSprite.FOOD );
			eatSFX();

			hero.spend( eatingTime() );

			Talent.onFoodEaten(hero, energy, this);

			Statistics.foodEaten++;
			Badges.validateFoodEaten();

		}

		if (action.equals( AC_SPICE )) {
			GameScene.selectItem( spiceSelector );
		}
	}

	protected static WndBag.ItemSelector spiceSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return Messages.get(Food.class, "inv_title");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return VelvetPouch.class;
		}

		@Override
		public boolean itemSelectable( Item item ) {
			return item instanceof Plant.Seed;
		}

		@Override
		public void onSelect( Item item ) {
			if (item instanceof Plant.Seed && curItem instanceof Food) {
				Food food = (Food) curItem;
				if (food.quantity() > 1) {
					food = (Food) food.detach(curUser.belongings.backpack);
					food.seed = (Plant.Seed) Reflection.newInstance(item.getClass()).quantity(1);
					item.detach(curUser.belongings.backpack);
					if (!food.collect(curUser.belongings.backpack)) {
						Dungeon.level.drop(food, curUser.pos).sprite.drop();
					}
				} else {
					food.seed = (Plant.Seed) Reflection.newInstance(item.getClass()).quantity(1);
					item.detach(curUser.belongings.backpack);
				}
				curUser.spend( TIME_TO_SPICE );
				curUser.busy();
				curUser.sprite.operate(curUser.pos);
				updateQuickslot();
			}
		}
	};

	// Applies the spicing seed's effect when the food is eaten
	protected void eatEffect( Hero hero ){
		if (seed != null && seed.getPlantClass() != null) {
			Plant plant = Reflection.newInstance(seed.getPlantClass());
			if (plant != null) {
				plant.spiceEffect(hero);
			}
		}
	}

	protected void eatSFX(){
		Sample.INSTANCE.play( Assets.Sounds.EAT );
	}

	protected float eatingTime(){
		if (Dungeon.hero.hasTalent(Talent.IRON_STOMACH)
			|| Dungeon.hero.hasTalent(Talent.ENERGIZING_MEAL)
			|| Dungeon.hero.hasTalent(Talent.MYSTICAL_MEAL)
			|| Dungeon.hero.hasTalent(Talent.INVIGORATING_MEAL)
			|| Dungeon.hero.hasTalent(Talent.FOCUSED_MEAL)
			|| Dungeon.hero.hasTalent(Talent.ENLIGHTENING_MEAL)){
			return TIME_TO_EAT - 2;
		} else {
			return TIME_TO_EAT;
		}
	}

	protected void satisfy( Hero hero ){
		float foodVal = energy;
		if (Dungeon.isChallenged(Challenges.NO_FOOD)){
			foodVal /= 3f;
		}

		Artifact.ArtifactBuff buff = hero.buff( HornOfPlenty.hornRecharge.class );
		if (buff != null && buff.isCursed()){
			foodVal *= 0.67f;
			GLog.n( Messages.get(Hunger.class, "cursedhorn") );
		}

		Buff.affect(hero, Hunger.class).satisfy(foodVal);
	}

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
		return 10 * quantity;
	}
}

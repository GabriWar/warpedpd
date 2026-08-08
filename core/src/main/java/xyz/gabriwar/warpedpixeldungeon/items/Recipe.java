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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.items.BulletItem;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.alchemy.PotOThunder;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.alchemy.Cross;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.MeatPie;
import xyz.gabriwar.warpedpixeldungeon.items.food.StewedMeat;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.AquaBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.BlizzardBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.CausticBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.InfernalBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.ShockingBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.UnstableBrew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfMight;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfToxicEssence;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.ExoticPotion;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ExoticScroll;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Alchemize;
import xyz.gabriwar.warpedpixeldungeon.items.spells.AquaBlast;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Evolution;
import xyz.gabriwar.warpedpixeldungeon.items.spells.UpgradeDust;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.AlchemyWeaponRecipe;
import xyz.gabriwar.warpedpixeldungeon.items.spells.CrimsonEpithet;
import xyz.gabriwar.warpedpixeldungeon.items.spells.DoomCall;
import xyz.gabriwar.warpedpixeldungeon.items.spells.EnchantmentInfusion;
import xyz.gabriwar.warpedpixeldungeon.items.spells.FeatherFall;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Forcefield;
import xyz.gabriwar.warpedpixeldungeon.items.spells.ForcePush;
import xyz.gabriwar.warpedpixeldungeon.items.spells.HolyBlast;
import xyz.gabriwar.warpedpixeldungeon.items.spells.MagicalPorter;
import xyz.gabriwar.warpedpixeldungeon.items.spells.NaturesLullaby;
import xyz.gabriwar.warpedpixeldungeon.items.spells.PlantSummon;
import xyz.gabriwar.warpedpixeldungeon.items.spells.SeasonChange;
import xyz.gabriwar.warpedpixeldungeon.items.spells.SpontaneousCombustion;
import xyz.gabriwar.warpedpixeldungeon.items.spells.BeaconOfReturning;
import xyz.gabriwar.warpedpixeldungeon.items.spells.CurseInfusion;
import xyz.gabriwar.warpedpixeldungeon.items.spells.MagicalInfusion;
import xyz.gabriwar.warpedpixeldungeon.items.spells.PhaseShift;
import xyz.gabriwar.warpedpixeldungeon.items.spells.ReclaimTrap;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Recycle;
import xyz.gabriwar.warpedpixeldungeon.items.spells.SummonElemental;
import xyz.gabriwar.warpedpixeldungeon.items.spells.TelekineticGrab;
import xyz.gabriwar.warpedpixeldungeon.items.spells.UnstableSpell;
import xyz.gabriwar.warpedpixeldungeon.items.spells.WildEnergy;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.Trinket;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrinketCatalyst;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public abstract class Recipe {
	
	public abstract boolean testIngredients(ArrayList<Item> ingredients);
	
	public abstract int cost(ArrayList<Item> ingredients);
	
	public abstract Item brew(ArrayList<Item> ingredients);
	
	public abstract Item sampleOutput(ArrayList<Item> ingredients);
	
	//subclass for the common situation of a recipe with static inputs and outputs
	public static abstract class SimpleRecipe extends Recipe {
		
		//*** These elements must be filled in by subclasses
		protected Class<?extends Item>[] inputs; //each class should be unique
		protected int[] inQuantity;
		
		protected int cost;
		
		protected Class<?extends Item> output;
		protected int outQuantity;
		//***
		
		//gets a simple list of items based on inputs
		public ArrayList<Item> getIngredients() {
			ArrayList<Item> result = new ArrayList<>();
			for (int i = 0; i < inputs.length; i++) {
				Item ingredient = Reflection.newInstance(inputs[i]);
				ingredient.quantity(inQuantity[i]);
				result.add(ingredient);
			}
			return result;
		}
		
		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			
			int[] needed = inQuantity.clone();
			
			for (Item ingredient : ingredients){
				if (!ingredient.isIdentified()) return false;
				for (int i = 0; i < inputs.length; i++){
					if (ingredient.getClass() == inputs[i]){
						needed[i] -= ingredient.quantity();
						break;
					}
				}
			}
			
			for (int i : needed){
				if (i > 0){
					return false;
				}
			}
			
			return true;
		}
		
		public int cost(ArrayList<Item> ingredients){
			return cost;
		}
		
		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			
			int[] needed = inQuantity.clone();
			
			for (Item ingredient : ingredients){
				for (int i = 0; i < inputs.length; i++) {
					if (ingredient.getClass() == inputs[i] && needed[i] > 0) {
						if (needed[i] <= ingredient.quantity()) {
							ingredient.quantity(ingredient.quantity() - needed[i]);
							needed[i] = 0;
						} else {
							needed[i] -= ingredient.quantity();
							ingredient.quantity(0);
						}
					}
				}
			}
			
			//sample output and real output are identical in this case.
			return sampleOutput(null);
		}
		
		//ingredients are ignored, as output doesn't vary
		public Item sampleOutput(ArrayList<Item> ingredients){
			try {
				Item result = Reflection.newInstance(output);
				result.quantity(outQuantity);
				return result;
			} catch (Exception e) {
				WarpedPixelDungeon.reportException( e );
				return null;
			}
		}
	}
	
	
	//*******
	// Static members
	//*******

	private static Recipe[] variableRecipes = new Recipe[]{
			//none for now
	};
	
	private static Recipe[] oneIngredientRecipes = new Recipe[]{
		new BulletItem.Recipe(), //gun ammo from liquid metal (Re-ARranged)
		new BulletBelt.Recipe(), //bulk ammo, the only other way to stock up on bullets
		new Scroll.ScrollToStone(),
		new ExoticPotion.PotionToExotic(),
		new ExoticScroll.ScrollToExotic(),
		new ArcaneResin.Recipe(),
		new LiquidMetal.Recipe(),
		new BlizzardBrew.Recipe(),
		new InfernalBrew.Recipe(),
		new AquaBrew.Recipe(),
		new ShockingBrew.Recipe(),
		new ElixirOfDragonsBlood.Recipe(),
		new ElixirOfIcyTouch.Recipe(),
		new ElixirOfToxicEssence.Recipe(),
		new ElixirOfMight.Recipe(),
		new ElixirOfFeatherFall.Recipe(),
		new MagicalInfusion.Recipe(),
		new BeaconOfReturning.Recipe(),
		new PhaseShift.Recipe(),
		new Recycle.Recipe(),
		new TelekineticGrab.Recipe(),
		new SummonElemental.Recipe(),
		new StewedMeat.oneMeat(),
		new StewedMeat.oneMonsterMeat(),
		new TrinketCatalyst.Recipe(),
		new Trinket.UpgradeTrinket(),
		new xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HolyWater.Recipe()
	};
	
	private static Recipe[] twoIngredientRecipes = new Recipe[]{
		new Blandfruit.CookFruit(),
		new Bomb.EnhanceBomb(),
		new UnstableBrew.Recipe(),
		new CausticBrew.Recipe(),
		new ElixirOfArcaneArmor.Recipe(),
		new ElixirOfAquaticRejuvenation.Recipe(),
		new ElixirOfHoneyedHealing.Recipe(),
		new UnstableSpell.Recipe(),
		new Alchemize.Recipe(),
		new CurseInfusion.Recipe(),
		new ReclaimTrap.Recipe(),
		new WildEnergy.Recipe(),
		new StewedMeat.twoMeat(),
		new StewedMeat.twoMonsterMeat(),
		//OvergrownPD-ported spells (kept in OV's registration order — MagicalPorter
		//must precede CrimsonEpithet since they share the Teleport+MagicMapping pair)
		new AquaBlast.Recipe(),
		new MagicalPorter.Recipe(),
		new CrimsonEpithet.Recipe(),
		new DoomCall.Recipe(),
		new EnchantmentInfusion.Recipe(),
		new FeatherFall.Recipe(),
		new Forcefield.Recipe(),
		new ForcePush.Recipe(),
		new HolyBlast.Recipe(),
		new NaturesLullaby.Recipe(),
		new PlantSummon.Recipe(),
		new SeasonChange.Recipe(),
		new SpontaneousCombustion.Recipe(),
		new SeekingBombItem.Recipe(),
		//Re-ARranged-ported alchemy weapon catalysts
		//GunSmithingTool shares UpgradeDust's exact ingredient pair, as it does upstream:
		//the pot offers both outputs side by side (AlchemyScene has three combine slots)
		new GunSmithingTool.Recipe(),
		new Evolution.Recipe(),
		new UpgradeDust.Recipe()
	};
	
	private static Recipe[] threeIngredientRecipes = new Recipe[]{
		new Potion.SeedToPotion(),
		new StewedMeat.threeMeat(),
		new StewedMeat.threeMonsterMeat(),
		new MeatPie.Recipe(),
		//Re-ARranged-ported alchemy weapons (weapon + weapon/dust + Evolution)
		new AlchemyWeaponRecipe(),
		new Cross.Recipe(),
		new PotOThunder.Recipe()
	};
	
	public static ArrayList<Recipe> findRecipes(ArrayList<Item> ingredients){

		ArrayList<Recipe> result = new ArrayList<>();

		for (Recipe recipe : variableRecipes){
			if (recipe.testIngredients(ingredients)){
				result.add(recipe);
			}
		}

		if (ingredients.size() == 1){
			for (Recipe recipe : oneIngredientRecipes){
				if (recipe.testIngredients(ingredients)){
					result.add(recipe);
				}
			}
			
		} else if (ingredients.size() == 2){
			for (Recipe recipe : twoIngredientRecipes){
				if (recipe.testIngredients(ingredients)){
					result.add(recipe);
				}
			}
			
		} else if (ingredients.size() == 3){
			for (Recipe recipe : threeIngredientRecipes){
				if (recipe.testIngredients(ingredients)){
					result.add(recipe);
				}
			}
		}
		
		return result;
	}
	
	public static boolean usableInRecipe(Item item){
		//only upgradeable thrown/melee weapons and wands allowed among equipment items
		//(Re-ARranged lets melee weapons in, that's what the alchemy weapon recipes eat)
		if (item instanceof EquipableItem){
			return item.cursedKnown && !item.cursed &&
					(item instanceof MissileWeapon || item instanceof MeleeWeapon)
					&& item.isUpgradable() && !item.isEquipped( Dungeon.hero );
		} else if (item instanceof Wand) {
			return item.cursedKnown && !item.cursed;
		} else {
			//other items can be unidentified, but not cursed
			return !item.cursed;
		}
	}
}



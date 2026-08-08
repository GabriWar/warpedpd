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

package xyz.gabriwar.warpedpixeldungeon.items.potions.exotic;

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.Recipe;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfBall;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfBanana;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfBlessing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfButter;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfChilli;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfDew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfDigesting;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfDirt;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfEgg;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfExperience;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfEye;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFirelightning;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFirestorm;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFlora;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfGlowing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfGoo;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfGrass;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHarvest;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHaste;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHoney;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMana;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMending;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMight;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfOverHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHunger;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHydrogenFire;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHypno;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfIceStorm;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfInfection;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfInvisibility;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfKiwi;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLantern;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLevitation;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLightning;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLiquidFlame;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLove;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMindVision;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMuscle;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfParalyticGas;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfParasites;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfPeanuts;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfPepper;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfProtection;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfPurity;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSeed;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfShadows;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfShield;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSlowness;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSmoke;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSnowstorm;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSoda;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSteam;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSun;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfTime;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfTomatoSoup;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfToxicGas;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfUltraviolett;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfVine;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfWater;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfWine;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfWithering;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ExoticPotion extends Potion {

	{
		//sprite = equivalent potion sprite but one row down
	}

	public static final LinkedHashMap<Class<?extends Potion>, Class<?extends ExoticPotion>> regToExo = new LinkedHashMap<>();
	public static final LinkedHashMap<Class<?extends ExoticPotion>, Class<?extends Potion>> exoToReg = new LinkedHashMap<>();
	static {
		// SPD vanilla mappings
		regToExo.put(PotionOfStrength.class, PotionOfMastery.class);
		exoToReg.put(PotionOfMastery.class, PotionOfStrength.class);

		regToExo.put(PotionOfHealing.class, PotionOfShielding.class);
		exoToReg.put(PotionOfShielding.class, PotionOfHealing.class);

		regToExo.put(PotionOfMindVision.class, PotionOfMagicalSight.class);
		exoToReg.put(PotionOfMagicalSight.class, PotionOfMindVision.class);

		regToExo.put(PotionOfFrost.class, PotionOfSnapFreeze.class);
		exoToReg.put(PotionOfSnapFreeze.class, PotionOfFrost.class);

		regToExo.put(PotionOfLiquidFlame.class, PotionOfDragonsBreath.class);
		exoToReg.put(PotionOfDragonsBreath.class, PotionOfLiquidFlame.class);

		regToExo.put(PotionOfToxicGas.class, PotionOfCorrosiveGas.class);
		exoToReg.put(PotionOfCorrosiveGas.class, PotionOfToxicGas.class);

		regToExo.put(PotionOfHaste.class, PotionOfStamina.class);
		exoToReg.put(PotionOfStamina.class, PotionOfHaste.class);

		regToExo.put(PotionOfInvisibility.class, PotionOfShroudingFog.class);
		exoToReg.put(PotionOfShroudingFog.class, PotionOfInvisibility.class);

		regToExo.put(PotionOfLevitation.class, PotionOfStormClouds.class);
		exoToReg.put(PotionOfStormClouds.class, PotionOfLevitation.class);

		regToExo.put(PotionOfParalyticGas.class, PotionOfEarthenArmor.class);
		exoToReg.put(PotionOfEarthenArmor.class, PotionOfParalyticGas.class);

		regToExo.put(PotionOfPurity.class, PotionOfCleansing.class);
		exoToReg.put(PotionOfCleansing.class, PotionOfPurity.class);

		regToExo.put(PotionOfExperience.class, PotionOfDivineInspiration.class);
		exoToReg.put(PotionOfDivineInspiration.class, PotionOfExperience.class);

		// OV mappings
		regToExo.put(PotionOfBall.class, PotionOfSpiral.class);
		exoToReg.put(PotionOfSpiral.class, PotionOfBall.class);

		regToExo.put(PotionOfBanana.class, PotionOfProtain.class);
		exoToReg.put(PotionOfProtain.class, PotionOfBanana.class);

		regToExo.put(PotionOfBlessing.class, PotionOfHoly.class);
		exoToReg.put(PotionOfHoly.class, PotionOfBlessing.class);

		regToExo.put(PotionOfButter.class, PotionOfButterbread.class);
		exoToReg.put(PotionOfButterbread.class, PotionOfButter.class);

		regToExo.put(PotionOfChilli.class, PotionOfTears.class);
		exoToReg.put(PotionOfTears.class, PotionOfChilli.class);

		regToExo.put(PotionOfDew.class, PotionOfSuperdew.class);
		exoToReg.put(PotionOfSuperdew.class, PotionOfDew.class);

		regToExo.put(PotionOfDigesting.class, PotionOfStomach.class);
		exoToReg.put(PotionOfStomach.class, PotionOfDigesting.class);

		regToExo.put(PotionOfDirt.class, PotionOfSoil.class);
		exoToReg.put(PotionOfSoil.class, PotionOfDirt.class);

		regToExo.put(PotionOfEgg.class, PotionOfOrb.class);
		exoToReg.put(PotionOfOrb.class, PotionOfEgg.class);

		regToExo.put(PotionOfEye.class, PotionOfAllSeeing.class);
		exoToReg.put(PotionOfAllSeeing.class, PotionOfEye.class);

		regToExo.put(PotionOfFirelightning.class, PotionOfBallLightning.class);
		exoToReg.put(PotionOfBallLightning.class, PotionOfFirelightning.class);

		regToExo.put(PotionOfFirestorm.class, PotionOfHellstorm.class);
		exoToReg.put(PotionOfHellstorm.class, PotionOfFirestorm.class);

		regToExo.put(PotionOfFlora.class, PotionOfFlower.class);
		exoToReg.put(PotionOfFlower.class, PotionOfFlora.class);

		regToExo.put(PotionOfGlowing.class, PotionOfRadiation.class);
		exoToReg.put(PotionOfRadiation.class, PotionOfGlowing.class);

		regToExo.put(PotionOfGoo.class, PotionOfGloop.class);
		exoToReg.put(PotionOfGloop.class, PotionOfGoo.class);

		regToExo.put(PotionOfGrass.class, PotionOfHighgrass.class);
		exoToReg.put(PotionOfHighgrass.class, PotionOfGrass.class);

		regToExo.put(PotionOfHarvest.class, PotionOfAutumn.class);
		exoToReg.put(PotionOfAutumn.class, PotionOfHarvest.class);

		regToExo.put(PotionOfHoney.class, PotionOfBee.class);
		exoToReg.put(PotionOfBee.class, PotionOfHoney.class);

		regToExo.put(PotionOfHunger.class, PotionOfStarving.class);
		exoToReg.put(PotionOfStarving.class, PotionOfHunger.class);

		regToExo.put(PotionOfHydrogenFire.class, PotionOfMagicFire.class);
		exoToReg.put(PotionOfMagicFire.class, PotionOfHydrogenFire.class);

		regToExo.put(PotionOfHypno.class, PotionOfControl.class);
		exoToReg.put(PotionOfControl.class, PotionOfHypno.class);

		regToExo.put(PotionOfIceStorm.class, PotionOfAbsoluteZero.class);
		exoToReg.put(PotionOfAbsoluteZero.class, PotionOfIceStorm.class);

		regToExo.put(PotionOfInfection.class, PotionOfPlague.class);
		exoToReg.put(PotionOfPlague.class, PotionOfInfection.class);

		regToExo.put(PotionOfKiwi.class, PotionOfTerror.class);
		exoToReg.put(PotionOfTerror.class, PotionOfKiwi.class);

		regToExo.put(PotionOfLantern.class, PotionOfBeacon.class);
		exoToReg.put(PotionOfBeacon.class, PotionOfLantern.class);

		regToExo.put(PotionOfLightning.class, PotionOfAthmosphericCompression.class);
		exoToReg.put(PotionOfAthmosphericCompression.class, PotionOfLightning.class);

		regToExo.put(PotionOfLove.class, PotionOfReproduction.class);
		exoToReg.put(PotionOfReproduction.class, PotionOfLove.class);

		regToExo.put(PotionOfMuscle.class, PotionOfBrain.class);
		exoToReg.put(PotionOfBrain.class, PotionOfMuscle.class);

		regToExo.put(PotionOfParasites.class, PotionOfWorm.class);
		exoToReg.put(PotionOfWorm.class, PotionOfParasites.class);

		regToExo.put(PotionOfPeanuts.class, PotionOfNuts.class);
		exoToReg.put(PotionOfNuts.class, PotionOfPeanuts.class);

		regToExo.put(PotionOfPepper.class, PotionOfHotness.class);
		exoToReg.put(PotionOfHotness.class, PotionOfPepper.class);

		regToExo.put(PotionOfProtection.class, PotionOfArmor.class);
		exoToReg.put(PotionOfArmor.class, PotionOfProtection.class);

		regToExo.put(PotionOfRegrowth.class, PotionOfImmortality.class);
		exoToReg.put(PotionOfImmortality.class, PotionOfRegrowth.class);

		regToExo.put(PotionOfSeed.class, PotionOfSowing.class);
		exoToReg.put(PotionOfSowing.class, PotionOfSeed.class);

		regToExo.put(PotionOfShadows.class, PotionOfSleepParalysis.class);
		exoToReg.put(PotionOfSleepParalysis.class, PotionOfShadows.class);

		regToExo.put(PotionOfShield.class, PotionOfIronSkin.class);
		exoToReg.put(PotionOfIronSkin.class, PotionOfShield.class);

		regToExo.put(PotionOfSlowness.class, PotionOfSlime.class);
		exoToReg.put(PotionOfSlime.class, PotionOfSlowness.class);

		regToExo.put(PotionOfSmoke.class, PotionOfAsh.class);
		exoToReg.put(PotionOfAsh.class, PotionOfSmoke.class);

		regToExo.put(PotionOfSnowstorm.class, PotionOfHail.class);
		exoToReg.put(PotionOfHail.class, PotionOfSnowstorm.class);

		regToExo.put(PotionOfSoda.class, PotionOfSugar.class);
		exoToReg.put(PotionOfSugar.class, PotionOfSoda.class);

		regToExo.put(PotionOfSteam.class, PotionOfPressure.class);
		exoToReg.put(PotionOfPressure.class, PotionOfSteam.class);

		regToExo.put(PotionOfSun.class, PotionOfSupernova.class);
		exoToReg.put(PotionOfSupernova.class, PotionOfSun.class);

		regToExo.put(PotionOfTime.class, PotionOfRelativity.class);
		exoToReg.put(PotionOfRelativity.class, PotionOfTime.class);

		regToExo.put(PotionOfTomatoSoup.class, PotionOfSwelling.class);
		exoToReg.put(PotionOfSwelling.class, PotionOfTomatoSoup.class);

		regToExo.put(PotionOfUltraviolett.class, PotionOfLaserbeam.class);
		exoToReg.put(PotionOfLaserbeam.class, PotionOfUltraviolett.class);

		regToExo.put(PotionOfVine.class, PotionOfStrung.class);
		exoToReg.put(PotionOfStrung.class, PotionOfVine.class);

		regToExo.put(PotionOfWater.class, PotionOfTsunami.class);
		exoToReg.put(PotionOfTsunami.class, PotionOfWater.class);

		regToExo.put(PotionOfWine.class, PotionOfAlcohol.class);
		exoToReg.put(PotionOfAlcohol.class, PotionOfWine.class);

		regToExo.put(PotionOfWithering.class, PotionOfDeath.class);
		exoToReg.put(PotionOfDeath.class, PotionOfWithering.class);

		//These four exotics had no native base potion (their OvergrownPD partners
		//are taken/absent in WPD), so they were orphaned: unobtainable + an NPE in
		//value()/isKnown() from exoToReg.get()==null. Pair them with the four
		//WPD-specific base potions that had no exotic counterpart.
		regToExo.put(PotionOfMight.class, PotionOfAdrenalineSurge.class);
		exoToReg.put(PotionOfAdrenalineSurge.class, PotionOfMight.class);

		regToExo.put(PotionOfOverHealing.class, PotionOfHolyFuror.class);
		exoToReg.put(PotionOfHolyFuror.class, PotionOfOverHealing.class);

		regToExo.put(PotionOfMending.class, PotionOfBleeding.class);
		exoToReg.put(PotionOfBleeding.class, PotionOfMending.class);

		regToExo.put(PotionOfMana.class, PotionOfQuantumsoup.class);
		exoToReg.put(PotionOfQuantumsoup.class, PotionOfMana.class);
	}

	@Override
	public boolean isKnown() {
		return anonymous || (handler != null && handler.isKnown( exoToReg.get(this.getClass()) ));
	}

	@Override
	public void setKnown() {
		if (!isKnown()) {
			handler.know(exoToReg.get(this.getClass()));
			updateQuickslot();
		}
	}

	@Override
	public void reset() {
		super.reset();
		if (handler != null && handler.contains(exoToReg.get(this.getClass()))) {
			image = handler.image(exoToReg.get(this.getClass())) + 16;
			color = handler.label(exoToReg.get(this.getClass()));
		}
	}

	@Override
	//20 gold more than its none-exotic equivalent
	public int value() {
		return (Reflection.newInstance(exoToReg.get(getClass())).value() + 20) * quantity;
	}

	@Override
	//4 more energy than its none-exotic equivalent
	public int energyVal() {
		return (Reflection.newInstance(exoToReg.get(getClass())).energyVal() + 4) * quantity;
	}

	public static class PotionToExotic extends Recipe {

		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			if (ingredients.size() == 1 && regToExo.containsKey(ingredients.get(0).getClass())) {
				return true;
			}
			return false;
		}

		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 4;
		}

		@Override
		public Item brew(ArrayList<Item> ingredients) {
			for (Item i : ingredients) {
				i.quantity(i.quantity()-1);
			}
			return Reflection.newInstance(regToExo.get(ingredients.get(0).getClass()));
		}

		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			return Reflection.newInstance(regToExo.get(ingredients.get(0).getClass()));
		}
	}
}

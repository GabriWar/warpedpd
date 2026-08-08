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

package xyz.gabriwar.warpedpixeldungeon.journal;

import xyz.gabriwar.warpedpixeldungeon.items.artifacts.CandleOfMindVision;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.AssassinsSpear;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.BeamSaber;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.ChainFlail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.ChainWhip;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.DualGreatSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.ForceGlove;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.HolySword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.HugeSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.Lance;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.LanceNShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.MeisterHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.ObsidianShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.SharpKatana;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.SpearNShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.TrueRunicBlade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.UnformedBlade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.UnholyBible;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.alchemy.Cross;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.alchemy.PotOThunder;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Evolution;
import xyz.gabriwar.warpedpixeldungeon.items.spells.UpgradeDust;
import xyz.gabriwar.warpedpixeldungeon.items.ArrowItem;
import xyz.gabriwar.warpedpixeldungeon.items.BulletBelt;
import xyz.gabriwar.warpedpixeldungeon.items.BulletItem;
import xyz.gabriwar.warpedpixeldungeon.items.ArrowBag;
import xyz.gabriwar.warpedpixeldungeon.items.GunSmithingTool;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.Amulet;
import xyz.gabriwar.warpedpixeldungeon.items.Ankh;
import xyz.gabriwar.warpedpixeldungeon.items.ArcaneResin;
import xyz.gabriwar.warpedpixeldungeon.items.BrokenSeal;
import xyz.gabriwar.warpedpixeldungeon.items.Dewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.EnergyCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Honeypot;
import xyz.gabriwar.warpedpixeldungeon.items.KingsCrown;
import xyz.gabriwar.warpedpixeldungeon.items.LiquidMetal;
import xyz.gabriwar.warpedpixeldungeon.items.Stylus;
import xyz.gabriwar.warpedpixeldungeon.items.TengusMask;
import xyz.gabriwar.warpedpixeldungeon.items.Torch;
import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.bags.AnkhChain;
import xyz.gabriwar.warpedpixeldungeon.items.bags.MagicalHolster;
import xyz.gabriwar.warpedpixeldungeon.items.bags.PotionBandolier;
import xyz.gabriwar.warpedpixeldungeon.items.bags.ScrollHolder;
import xyz.gabriwar.warpedpixeldungeon.items.bags.FoodPouch;
import xyz.gabriwar.warpedpixeldungeon.items.bags.VelvetPouch;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.ArcaneBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.ClusterBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.DizzyBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.DumplingBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Firebomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.FlashBangBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.FrostBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.HolyBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.HolyHandGrenade;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Noisemaker;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.RegrowthBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.ShrapnelBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.SmartBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.SmokeBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.WoollyBomb;
import xyz.gabriwar.warpedpixeldungeon.items.food.Berry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blackberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blueberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.BlueMilk;
import xyz.gabriwar.warpedpixeldungeon.items.food.ChargrilledMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cloudberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.DeathCap;
import xyz.gabriwar.warpedpixeldungeon.items.food.Earthstar;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.food.FrozenCarpaccio;
import xyz.gabriwar.warpedpixeldungeon.items.food.FullMoonberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.GoldenJelly;
import xyz.gabriwar.warpedpixeldungeon.items.food.GoldenNut;
import xyz.gabriwar.warpedpixeldungeon.items.food.JackOLantern;
import xyz.gabriwar.warpedpixeldungeon.items.food.MeatPie;
import xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.Moonberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.Nut;
import xyz.gabriwar.warpedpixeldungeon.items.food.Pasty;
import xyz.gabriwar.warpedpixeldungeon.items.food.PhantomMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.PixieParasol;
import xyz.gabriwar.warpedpixeldungeon.items.food.PotionOfConstitution;
import xyz.gabriwar.warpedpixeldungeon.items.food.SmallRation;
import xyz.gabriwar.warpedpixeldungeon.items.food.StewedMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.SupplyRation;
import xyz.gabriwar.warpedpixeldungeon.items.food.ToastedNut;
import xyz.gabriwar.warpedpixeldungeon.items.keys.CrystalKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.GoldenKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.WornKey;
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
import xyz.gabriwar.warpedpixeldungeon.items.quest.CeremonialCandle;
import xyz.gabriwar.warpedpixeldungeon.items.quest.CorpseDust;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DarkGold;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DwarfToken;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Embers;
import xyz.gabriwar.warpedpixeldungeon.items.quest.GooBlob;
import xyz.gabriwar.warpedpixeldungeon.items.quest.MetalShard;
import xyz.gabriwar.warpedpixeldungeon.items.remains.BowFragment;
import xyz.gabriwar.warpedpixeldungeon.items.remains.BrokenHilt;
import xyz.gabriwar.warpedpixeldungeon.items.remains.BrokenStaff;
import xyz.gabriwar.warpedpixeldungeon.items.remains.CloakScrap;
import xyz.gabriwar.warpedpixeldungeon.items.remains.SealShard;
import xyz.gabriwar.warpedpixeldungeon.items.remains.TornPage;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ExoticScroll;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Alchemize;
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
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrinketCatalyst;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantArmor;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantRing;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWand;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.ArmorKit;
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo2;
import xyz.gabriwar.warpedpixeldungeon.items.InactiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.OrbOfZot;
import xyz.gabriwar.warpedpixeldungeon.items.InactiveMrDestructo2;
import xyz.gabriwar.warpedpixeldungeon.items.Mushroom;
import xyz.gabriwar.warpedpixeldungeon.items.PuddingCup;
import xyz.gabriwar.warpedpixeldungeon.items.Rice;
import xyz.gabriwar.warpedpixeldungeon.items.StoneOre;
import xyz.gabriwar.warpedpixeldungeon.items.Towel;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.RingOfDisintegration;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.RingOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.keys.GoldenSkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.misc.AutoPotion;
import xyz.gabriwar.warpedpixeldungeon.items.misc.Spectacles;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.BlueNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.GreenNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.OrangeNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.PurpleNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.YellowNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.DeathSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MinersTool;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Saber;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shovel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.quick.PocketKnife;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Chainsaw;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spork;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.AresSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.CromCruachAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.LokisFlail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.NeptunusTrident;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.TippedDart;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;

//For items, but includes a few item-like effects, such as enchantments
public enum Catalog {

	//EQUIPMENT
	MELEE_WEAPONS,
	ARMOR,
	ENCHANTMENTS,
	GLYPHS,
	THROWN_WEAPONS,
	WANDS,
	RINGS,
	ARTIFACTS,
	TRINKETS,
	MISC_EQUIPMENT,

	//CONSUMABLES
	POTIONS,
	SEEDS,
	SCROLLS,
	STONES,
	FOOD,
	EXOTIC_POTIONS,
	EXOTIC_SCROLLS,
	BOMBS,
	TIPPED_DARTS,
	BREWS_ELIXIRS,
	SPELLS,
	MISC_CONSUMABLES;

	//tracks whether an item has been collected while identified
	private final LinkedHashMap<Class<?>, Boolean> seen = new LinkedHashMap<>();
	//tracks upgrades spent for equipment, uses for consumables
	private final LinkedHashMap<Class<?>, Integer> useCount = new LinkedHashMap<>();
	
	public Collection<Class<?>> items(){
		return seen.keySet();
	}

	//should only be used when initializing
	private void addItems( Class<?>... items){
		for (Class<?> item : items){
			seen.put(item, false);
			useCount.put(item, 0);
		}
	}

	public String title(){
		return Messages.get(this, name() + ".title");
	}

	public int totalItems(){
		return seen.size();
	}

	public int totalSeen(){
		int seenTotal = 0;
		for (boolean itemSeen : seen.values()){
			if (itemSeen) seenTotal++;
		}
		return seenTotal;
	}

	static {

		MELEE_WEAPONS.addItems(Generator.Category.WEP_T1.classes);
		MELEE_WEAPONS.addItems(Generator.Category.WEP_T2.classes);
		MELEE_WEAPONS.addItems(Generator.Category.WEP_T3.classes);
		MELEE_WEAPONS.addItems(Generator.Category.WEP_T4.classes);
		MELEE_WEAPONS.addItems(Generator.Category.WEP_T5.classes);

		//ported content that is craft-only or otherwise outside the loot tables
		MELEE_WEAPONS.addItems(Saber.class, Shovel.class, Spade.class, MinersTool.class, PocketKnife.class, DeathSword.class, AssassinsSpear.class, BeamSaber.class, ChainFlail.class, ChainWhip.class, DualGreatSword.class, ForceGlove.class, HolySword.class, HugeSword.class, Lance.class, LanceNShield.class, MeisterHammer.class, ObsidianShield.class, SharpKatana.class, SpearNShield.class, TrueRunicBlade.class, UnformedBlade.class, UnholyBible.class);

		ARMOR.addItems(Generator.Category.ARMOR.classes);

		THROWN_WEAPONS.addItems(Generator.Category.MIS_T1.classes);
		THROWN_WEAPONS.addItems(Generator.Category.MIS_T2.classes);
		THROWN_WEAPONS.addItems(Generator.Category.MIS_T3.classes);
		THROWN_WEAPONS.addItems(Generator.Category.MIS_T4.classes);
		THROWN_WEAPONS.addItems(Generator.Category.MIS_T5.classes);
		THROWN_WEAPONS.addItems(Cross.class, PotOThunder.class);

		ENCHANTMENTS.addItems(Weapon.Enchantment.common);
		ENCHANTMENTS.addItems(Weapon.Enchantment.uncommon);
		ENCHANTMENTS.addItems(Weapon.Enchantment.rare);
		ENCHANTMENTS.addItems(Weapon.Enchantment.curses);

		GLYPHS.addItems(Armor.Glyph.common);
		GLYPHS.addItems(Armor.Glyph.uncommon);
		GLYPHS.addItems(Armor.Glyph.rare);
		GLYPHS.addItems(Armor.Glyph.curses);

		WANDS.addItems(Generator.Category.WAND.classes);

		RINGS.addItems(Generator.Category.RING.classes);

		ARTIFACTS.addItems(Generator.Category.ARTIFACT.classes);
		ARTIFACTS.addItems(CandleOfMindVision.class);
		ARTIFACTS.addItems(RingOfDisintegration.class, RingOfFrost.class);

		TRINKETS.addItems(Generator.Category.TRINKET.classes);

		MISC_EQUIPMENT.addItems(BrokenSeal.class, SpiritBow.class, Waterskin.class, VelvetPouch.class,
				PotionBandolier.class, ScrollHolder.class, MagicalHolster.class, Amulet.class,
				AnkhChain.class, FoodPouch.class,
				AutoPotion.class, Spectacles.class,
				AresSword.class, CromCruachAxe.class, LokisFlail.class, NeptunusTrident.class,
				Chainsaw.class, Spork.class, ArmorKit.class );



		POTIONS.addItems(Generator.Category.POTION.classes);

		SCROLLS.addItems(Generator.Category.SCROLL.classes);

		SEEDS.addItems(Generator.Category.SEED.classes);

		STONES.addItems(Generator.Category.STONE.classes);

		FOOD.addItems( Food.class, Pasty.class, MysteryMeat.class, ChargrilledMeat.class,
				StewedMeat.class, FrozenCarpaccio.class, SmallRation.class, Berry.class,
				SupplyRation.class, Blandfruit.class, PhantomMeat.class, MeatPie.class,
				MonsterMeat.class, Nut.class, ToastedNut.class, GoldenNut.class,
				Blackberry.class, Cloudberry.class, Blueberry.class, Moonberry.class,
				FullMoonberry.class, DeathCap.class, Earthstar.class, GoldenJelly.class,
				JackOLantern.class, BlueMilk.class, PixieParasol.class, PotionOfConstitution.class );

		EXOTIC_POTIONS.addItems(ExoticPotion.exoToReg.keySet().toArray(new Class[0]));

		EXOTIC_SCROLLS.addItems(ExoticScroll.exoToReg.keySet().toArray(new Class[0]));

		BOMBS.addItems( Bomb.class, FrostBomb.class, Firebomb.class, SmokeBomb.class, RegrowthBomb.class,
				WoollyBomb.class, Noisemaker.class, FlashBangBomb.class, HolyBomb.class, ArcaneBomb.class, ShrapnelBomb.class,
				SmartBomb.class, ClusterBomb.class, DizzyBomb.class, HolyHandGrenade.class, DumplingBomb.class );

		TIPPED_DARTS.addItems(TippedDart.types.values().toArray(new Class[0]));

		BREWS_ELIXIRS.addItems( UnstableBrew.class, InfernalBrew.class, BlizzardBrew.class,
				ShockingBrew.class, CausticBrew.class, AquaBrew.class, ElixirOfHoneyedHealing.class,
				ElixirOfAquaticRejuvenation.class, ElixirOfArcaneArmor.class, ElixirOfDragonsBlood.class,
				ElixirOfIcyTouch.class, ElixirOfToxicEssence.class, ElixirOfMight.class, ElixirOfFeatherFall.class);

		SPELLS.addItems( UnstableSpell.class, WildEnergy.class, TelekineticGrab.class, PhaseShift.class,
				Alchemize.class, CurseInfusion.class, MagicalInfusion.class, Recycle.class,
				ReclaimTrap.class, SummonElemental.class, BeaconOfReturning.class);
		SPELLS.addItems(Evolution.class, UpgradeDust.class);

		MISC_CONSUMABLES.addItems(ArrowItem.class, BulletItem.class, BulletBelt.class, ArrowBag.class, GunSmithingTool.class);
		MISC_CONSUMABLES.addItems( Gold.class, EnergyCrystal.class, Dewdrop.class,
				IronKey.class, GoldenKey.class, CrystalKey.class, WornKey.class,
				TrinketCatalyst.class, Stylus.class, Torch.class, Honeypot.class, Ankh.class,
				CorpseDust.class, Embers.class, CeremonialCandle.class, DarkGold.class, DwarfToken.class,
				GooBlob.class, TengusMask.class, MetalShard.class, KingsCrown.class,
				LiquidMetal.class, ArcaneResin.class,
				SealShard.class, BrokenStaff.class, CloakScrap.class, BowFragment.class, BrokenHilt.class, TornPage.class,
				SkeletonKey.class, GoldenSkeletonKey.class,
				Rice.class, Mushroom.class, PuddingCup.class, StoneOre.class,
				BlueNornStone.class, GreenNornStone.class, OrangeNornStone.class,
				PurpleNornStone.class, YellowNornStone.class,
				AdamantArmor.class, AdamantRing.class, AdamantWand.class, AdamantWeapon.class,
				InactiveMrDestructo.class, InactiveMrDestructo2.class,
				ActiveMrDestructo.class, ActiveMrDestructo2.class, OrbOfZot.class,
				Towel.class );

	}

	//old badges for pre-2.5
	public static LinkedHashMap<Catalog, Badges.Badge> catalogBadges = new LinkedHashMap<>();
	static {
		catalogBadges.put(MELEE_WEAPONS, Badges.Badge.ALL_WEAPONS_IDENTIFIED);
		catalogBadges.put(ARMOR, Badges.Badge.ALL_ARMOR_IDENTIFIED);
		catalogBadges.put(WANDS, Badges.Badge.ALL_WANDS_IDENTIFIED);
		catalogBadges.put(RINGS, Badges.Badge.ALL_RINGS_IDENTIFIED);
		catalogBadges.put(ARTIFACTS, Badges.Badge.ALL_ARTIFACTS_IDENTIFIED);
		catalogBadges.put(POTIONS, Badges.Badge.ALL_POTIONS_IDENTIFIED);
		catalogBadges.put(SCROLLS, Badges.Badge.ALL_SCROLLS_IDENTIFIED);
	}

	public static ArrayList<Catalog> equipmentCatalogs = new ArrayList<>();
	static {
		equipmentCatalogs.add(MELEE_WEAPONS);
		equipmentCatalogs.add(ARMOR);
		equipmentCatalogs.add(ENCHANTMENTS);
		equipmentCatalogs.add(GLYPHS);
		equipmentCatalogs.add(THROWN_WEAPONS);
		equipmentCatalogs.add(WANDS);
		equipmentCatalogs.add(RINGS);
		equipmentCatalogs.add(ARTIFACTS);
		equipmentCatalogs.add(TRINKETS);
		equipmentCatalogs.add(MISC_EQUIPMENT);
	}

	public static ArrayList<Catalog> consumableCatalogs = new ArrayList<>();
	static {
		consumableCatalogs.add(POTIONS);
		consumableCatalogs.add(SCROLLS);
		consumableCatalogs.add(SEEDS);
		consumableCatalogs.add(STONES);
		consumableCatalogs.add(FOOD);
		consumableCatalogs.add(EXOTIC_POTIONS);
		consumableCatalogs.add(EXOTIC_SCROLLS);
		consumableCatalogs.add(BOMBS);
		consumableCatalogs.add(TIPPED_DARTS);
		consumableCatalogs.add(BREWS_ELIXIRS);
		consumableCatalogs.add(SPELLS);
		consumableCatalogs.add(MISC_CONSUMABLES);
	}
	
	public static boolean isSeen(Class<?> cls){
		for (Catalog cat : values()) {
			if (cat.seen.containsKey(cls)) {
				return cat.seen.get(cls);
			}
		}
		return false;
	}

	public static boolean isTracked(Class<?> cls){
		for (Catalog cat : values()) {
			if (cat.seen.containsKey(cls)) {
				return true;
			}
		}
		return false;
	}
	
	public static void setSeen(Class<?> cls){
		for (Catalog cat : values()) {
			if (cat.seen.containsKey(cls) && !cat.seen.get(cls)) {
				cat.seen.put(cls, true);
				Journal.saveNeeded = true;
			}
		}
		Badges.validateCatalogBadges();
	}

	public static int useCount(Class<?> cls){
		for (Catalog cat : values()) {
			if (cat.useCount.containsKey(cls)) {
				return cat.useCount.get(cls);
			}
		}
		return 0;
	}

	public static void countUse(Class<?> cls){
		countUses(cls, 1);
	}

	public static void countUses(Class<?> cls, int uses){
		//TODO currently uses of items in vault tester are don't count
		if (Dungeon.depth > 15 && Dungeon.branch > 0){
			return;
		}
		for (Catalog cat : values()) {
			if (cat.useCount.containsKey(cls) && cat.useCount.get(cls) != Integer.MAX_VALUE) {
				cat.useCount.put(cls, cat.useCount.get(cls)+uses);
				if (cat.useCount.get(cls) < -1_000_000_000){ //to catch cases of overflow
					cat.useCount.put(cls, Integer.MAX_VALUE);
				}
				Journal.saveNeeded = true;
			}
		}
	}

	private static final String CATALOG_CLASSES = "catalog_classes";
	private static final String CATALOG_SEEN    = "catalog_seen";
	private static final String CATALOG_USES    = "catalog_uses";
	
	public static void store( Bundle bundle ){

		ArrayList<Class<?>> classes = new ArrayList<>();
		ArrayList<Boolean> seen = new ArrayList<>();
		ArrayList<Integer> uses = new ArrayList<>();
		
		for (Catalog cat : values()) {
			for (Class<?> item : cat.items()) {
				if (cat.seen.get(item) || cat.useCount.get(item) > 0){
					classes.add(item);
					seen.add(cat.seen.get(item));
					uses.add(cat.useCount.get(item));
				}
			}
		}

		Class<?>[] storeCls = new Class[classes.size()];
		boolean[] storeSeen = new boolean[seen.size()];
		int[] storeUses = new int[uses.size()];

		for (int i = 0; i < storeCls.length; i++){
			storeCls[i] = classes.get(i);
			storeSeen[i] = seen.get(i);
			storeUses[i] = uses.get(i);
		}
		
		bundle.put( CATALOG_CLASSES, storeCls );
		bundle.put( CATALOG_SEEN, storeSeen );
		bundle.put( CATALOG_USES, storeUses );
		
	}

	//pre-v2.5
	private static final String CATALOG_ITEMS = "catalog_items";
	
	public static void restore( Bundle bundle ){

		//old logic for pre-v2.5 catalog-specific badges
		Badges.loadGlobal();
		for (Catalog cat : values()){
			if (Badges.isUnlocked(catalogBadges.get(cat))){
				for (Class<?> item : cat.items()){
					cat.seen.put(item, true);
				}
			}
		}
		if (bundle.contains(CATALOG_ITEMS)) {
			for (Class<?> cls : Arrays.asList(bundle.getClassArray(CATALOG_ITEMS))){
				for (Catalog cat : values()) {
					if (cat.seen.containsKey(cls)) {
						cat.seen.put(cls, true);
					}
				}
			}
		}
		//end of old logic

		if (bundle.contains(CATALOG_CLASSES)){
			Class<?>[] classes = bundle.getClassArray(CATALOG_CLASSES);
			boolean[] seen = bundle.getBooleanArray(CATALOG_SEEN);
			int[] uses = bundle.getIntArray(CATALOG_USES);

			for (int i = 0; i < classes.length; i++){
				for (Catalog cat : values()) {
					if (cat.seen.containsKey(classes[i])) {
						cat.seen.put(classes[i], seen[i]);
						cat.useCount.put(classes[i], uses[i]);
					}
				}

			}
		}

	}
	
}

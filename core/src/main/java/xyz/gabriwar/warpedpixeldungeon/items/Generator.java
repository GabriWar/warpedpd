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

import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfSearching;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfSating;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.LG.LG;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.FT.FT;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.Bow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shovel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MinersTool;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Saber;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.DeathSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.quick.PocketKnife;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.LargeSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Bible;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Knife;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Scalpel;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClericArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.DuelistArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.HuntressArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.MageArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.MailArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.RogueArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ScaleArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.WarriorArmor;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.AlchemistsToolkit;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.Artifact;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.ChaliceOfBlood;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.CloakOfShadows;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.DriedRose;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.EtherealChains;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.HolyTome;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.HornOfPlenty;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.MasterThievesArmband;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.SandalsOfNature;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TalismanOfForesight;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TimekeepersHourglass;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.RingOfDisintegration;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.RingOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.UnstableSpellbook;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blackberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blueberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.BlueMilk;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cloudberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.DeathCap;
import xyz.gabriwar.warpedpixeldungeon.items.food.Earthstar;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.food.FullMoonberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.GoldenJelly;
import xyz.gabriwar.warpedpixeldungeon.items.food.GoldenNut;
import xyz.gabriwar.warpedpixeldungeon.items.food.JackOLantern;
import xyz.gabriwar.warpedpixeldungeon.items.food.Moonberry;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.Nut;
import xyz.gabriwar.warpedpixeldungeon.items.food.Pasty;
import xyz.gabriwar.warpedpixeldungeon.items.food.PixieParasol;
import xyz.gabriwar.warpedpixeldungeon.items.food.PotionOfConstitution;
import xyz.gabriwar.warpedpixeldungeon.items.food.ToastedNut;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfBall;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfBlessing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfChilli;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfDew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfExperience;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfEye;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfGlowing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfGrass;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHaste;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHoney;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfInvisibility;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLantern;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLevitation;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLiquidFlame;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMindVision;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfParalyticGas;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfParasites;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfPeanuts;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfPepper;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfProtection;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfPurity;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMana;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMending;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMight;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfOverHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfShadows;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfShield;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfToxicGas;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfWine;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfWithering;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfBanana;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfEgg;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFlora;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHunger;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLove;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMuscle;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSeed;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfTime;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfTomatoSoup;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFirelightning;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLightning;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfDigesting;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFirestorm;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHydrogenFire;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHypno;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfIceStorm;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfKiwi;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSlowness;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSmoke;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSnowstorm;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSoda;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSteam;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfUltraviolett;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfButter;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfDirt;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfGoo;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHarvest;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfInfection;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSun;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfVine;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfWater;
import xyz.gabriwar.warpedpixeldungeon.items.potions.brews.Brew;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.Elixir;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.ExoticPotion;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfAccuracy;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfArcana;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfElements;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfEnergy;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfEvasion;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfFuror;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfHaste;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMight;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfSharpshooting;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfTenacity;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfWealth;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfIdentify;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfLullaby;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMirrorImage;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRage;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRecharging;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRetribution;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTerror;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTransmutation;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicalInfusion;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMultiUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ExoticScroll;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Alchemize;
import xyz.gabriwar.warpedpixeldungeon.items.spells.AquaBlast;
import xyz.gabriwar.warpedpixeldungeon.items.spells.BeaconOfReturning;
import xyz.gabriwar.warpedpixeldungeon.items.spells.CrimsonEpithet;
import xyz.gabriwar.warpedpixeldungeon.items.spells.CurseInfusion;
import xyz.gabriwar.warpedpixeldungeon.items.spells.DoomCall;
import xyz.gabriwar.warpedpixeldungeon.items.spells.EnchantmentInfusion;
import xyz.gabriwar.warpedpixeldungeon.items.spells.FeatherFall;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Forcefield;
import xyz.gabriwar.warpedpixeldungeon.items.spells.ForcePush;
import xyz.gabriwar.warpedpixeldungeon.items.spells.HolyBlast;
import xyz.gabriwar.warpedpixeldungeon.items.spells.MagicalInfusion;
import xyz.gabriwar.warpedpixeldungeon.items.spells.MagicalPorter;
import xyz.gabriwar.warpedpixeldungeon.items.spells.NaturesLullaby;
import xyz.gabriwar.warpedpixeldungeon.items.spells.PhaseShift;
import xyz.gabriwar.warpedpixeldungeon.items.spells.PlantSummon;
import xyz.gabriwar.warpedpixeldungeon.items.spells.ReclaimTrap;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Recycle;
import xyz.gabriwar.warpedpixeldungeon.items.spells.SeasonChange;
import xyz.gabriwar.warpedpixeldungeon.items.spells.Spell;
import xyz.gabriwar.warpedpixeldungeon.items.spells.SpontaneousCombustion;
import xyz.gabriwar.warpedpixeldungeon.items.spells.SummonElemental;
import xyz.gabriwar.warpedpixeldungeon.items.spells.TelekineticGrab;
import xyz.gabriwar.warpedpixeldungeon.items.spells.UnstableSpell;
import xyz.gabriwar.warpedpixeldungeon.items.spells.WildEnergy;
import xyz.gabriwar.warpedpixeldungeon.items.stones.Runestone;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfAggression;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfAugmentation;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfBlast;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfBlink;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfClairvoyance;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfDeepSleep;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfDetectMagic;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfEnchantment;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfFear;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfFlock;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfIntuition;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfShock;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ChaoticCenser;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.CrackedSpyglass;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.DimensionalSundial;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ExoticCrystals;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.EyeOfNewt;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.FerretTuft;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.MimicTooth;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.MossyClump;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ParchmentScrap;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.PetrifiedSeed;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.RatSkull;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.SaltCube;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ShardOfOblivion;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ThirteenLeafClover;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrapMechanism;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.Trinket;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrinketCatalyst;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.VialOfBlood;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.WondrousResin;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfCorrosion;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfCorruption;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfDisintegration;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFireblast;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfLightning;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfLivingEarth;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfPrismaticLight;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfTransfusion;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfAmok;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfAvalanche;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlink;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfDisintegration2;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFirebolt;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFlock;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfPoison;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfSlowness;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfTelekinesis;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfWarding;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.AssassinsBlade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.AssassinsKnife;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Axe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.BattleAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.BroadSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Crossbow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Cudgel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Dagger;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Dirk;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Gauntlet;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Glaive;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Gloves;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greataxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greatshield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greatsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Katana;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Longsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Mace;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MageStaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Quarterstaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Rapier;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RoundShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RunicBlade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sai;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Scimitar;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shortsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spear;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarScythe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Whip;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Brick;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.BottleFire;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HoneyArrow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingWave;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingSkull;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.EmpBola;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MindArrow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.SmallChakram;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HugeShuriken;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LifeArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PerformerArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.AsceticArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.FollowerArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.CeramicsArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.SoldierArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.BulletArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.MachineArmor;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Triangolo;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MageBook;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.TrickSand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.FightGloves;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.DualKnife;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flute;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MirrorDoll;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Nunchakus;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Wardrum;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HolyWater;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WindBottle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Trumpet;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.PrayerWheel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandLight;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Harp;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.StoneCross;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WornShortsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Bolas;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.CurareShuriken;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.FishingSpear;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ForceCube;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ForestDart;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.IncendiaryShuriken;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Javelin;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Kunai;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.RiceBall;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Shuriken;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Skull;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingClub;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingKnife;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingSpear;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingSpike;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ThrowingStone;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Tomahawk;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Trident;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Wave;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.plants.Apricobush;
import xyz.gabriwar.warpedpixeldungeon.plants.Ballcrop;
import xyz.gabriwar.warpedpixeldungeon.plants.Bananabean;
import xyz.gabriwar.warpedpixeldungeon.plants.Blackholeflower;
import xyz.gabriwar.warpedpixeldungeon.plants.Blindweed;
import xyz.gabriwar.warpedpixeldungeon.plants.Blueeyedsusan;
import xyz.gabriwar.warpedpixeldungeon.plants.Butterlion;
import xyz.gabriwar.warpedpixeldungeon.plants.Chandaliertail;
import xyz.gabriwar.warpedpixeldungeon.plants.Chillisnapper;
import xyz.gabriwar.warpedpixeldungeon.plants.Clockcypress;
import xyz.gabriwar.warpedpixeldungeon.plants.Cocostuft;
import xyz.gabriwar.warpedpixeldungeon.plants.Combflower;
import xyz.gabriwar.warpedpixeldungeon.plants.Crimsoncrown;
import xyz.gabriwar.warpedpixeldungeon.plants.Clitbalm;
import xyz.gabriwar.warpedpixeldungeon.plants.Cornwheat;
import xyz.gabriwar.warpedpixeldungeon.plants.Crimsonpepper;
import xyz.gabriwar.warpedpixeldungeon.plants.Dirtdaisy;
import xyz.gabriwar.warpedpixeldungeon.plants.Dreamfoil;
import xyz.gabriwar.warpedpixeldungeon.plants.Earthroot;
import xyz.gabriwar.warpedpixeldungeon.plants.Eggbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Eyeeuonymus;
import xyz.gabriwar.warpedpixeldungeon.plants.Fadeleaf;
import xyz.gabriwar.warpedpixeldungeon.plants.Feelerfern;
import xyz.gabriwar.warpedpixeldungeon.plants.Dewcatcher;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Firefoxglove;
import xyz.gabriwar.warpedpixeldungeon.plants.Flowertree;
import xyz.gabriwar.warpedpixeldungeon.plants.Gobgrape;
import xyz.gabriwar.warpedpixeldungeon.plants.Goograss;
import xyz.gabriwar.warpedpixeldungeon.plants.Grasslilly;
import xyz.gabriwar.warpedpixeldungeon.plants.Flytrap;
import xyz.gabriwar.warpedpixeldungeon.plants.Frostcorn;
import xyz.gabriwar.warpedpixeldungeon.plants.Grassvine;
import xyz.gabriwar.warpedpixeldungeon.plants.Hypnohemp;
import xyz.gabriwar.warpedpixeldungeon.plants.Icecap;
import xyz.gabriwar.warpedpixeldungeon.plants.Kiwivetch;
import xyz.gabriwar.warpedpixeldungeon.plants.Larvaleaf;
import xyz.gabriwar.warpedpixeldungeon.plants.Lavenderlantern;
import xyz.gabriwar.warpedpixeldungeon.plants.Lightninglily;
import xyz.gabriwar.warpedpixeldungeon.plants.Mageroyal;
import xyz.gabriwar.warpedpixeldungeon.plants.Musclemoss;
import xyz.gabriwar.warpedpixeldungeon.plants.Parasiteshrub;
import xyz.gabriwar.warpedpixeldungeon.plants.Peanutpetal;
import xyz.gabriwar.warpedpixeldungeon.plants.Poppoplar;
import xyz.gabriwar.warpedpixeldungeon.plants.Sourpitcher;
import xyz.gabriwar.warpedpixeldungeon.plants.Suncarnivore;
import xyz.gabriwar.warpedpixeldungeon.plants.Tankcabbage;
import xyz.gabriwar.warpedpixeldungeon.plants.Tomatobush;
import xyz.gabriwar.warpedpixeldungeon.plants.Venusflytrap;
import xyz.gabriwar.warpedpixeldungeon.plants.Waterweed;
import xyz.gabriwar.warpedpixeldungeon.plants.Nightshadeonion;
import xyz.gabriwar.warpedpixeldungeon.plants.Phaseshift;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Rose;
import xyz.gabriwar.warpedpixeldungeon.plants.Rotberry;
import xyz.gabriwar.warpedpixeldungeon.plants.Shadowbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Snowhedge;
import xyz.gabriwar.warpedpixeldungeon.plants.Sorrowmoss;
import xyz.gabriwar.warpedpixeldungeon.plants.Starflower;
import xyz.gabriwar.warpedpixeldungeon.plants.Steamweed;
import xyz.gabriwar.warpedpixeldungeon.plants.Stormvine;
import xyz.gabriwar.warpedpixeldungeon.plants.Sunbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.plants.Swiftthistle;
import xyz.gabriwar.warpedpixeldungeon.plants.Willowcane;
import xyz.gabriwar.warpedpixeldungeon.plants.Witherfennel;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Generator {

	public enum Category {
		TRINKET ( 0, 0, Trinket.class),

		WEAPON	( 2, 2, MeleeWeapon.class),
		WEP_T1	( 0, 0, MeleeWeapon.class),
		WEP_T2	( 0, 0, MeleeWeapon.class),
		WEP_T3	( 0, 0, MeleeWeapon.class),
		WEP_T4	( 0, 0, MeleeWeapon.class),
		WEP_T5	( 0, 0, MeleeWeapon.class),
		
		ARMOR	( 2, 1, Armor.class ),
		
		MISSILE ( 1, 2, MissileWeapon.class ),
		MIS_T1  ( 0, 0, MissileWeapon.class ),
		MIS_T2  ( 0, 0, MissileWeapon.class ),
		MIS_T3  ( 0, 0, MissileWeapon.class ),
		MIS_T4  ( 0, 0, MissileWeapon.class ),
		MIS_T5  ( 0, 0, MissileWeapon.class ),
		
		WAND	( 1, 1, Wand.class ),
		RING	( 1, 0, Ring.class ),
		ARTIFACT( 0, 1, Artifact.class),
		
		FOOD	( 0, 0, Food.class ),
		
		POTION	( 8, 8, Potion.class ),
		SEED	( 1, 1, Plant.Seed.class ),
		
		SCROLL	( 8, 8, Scroll.class ),
		SPELLS  ( 0, 0, Spell.class ),
		STONE   ( 1, 1, Runestone.class),

		GOLD	( 10, 10,   Gold.class );
		
		public Class<?>[] classes;

		//some item types use a deck-based system, where the probs decrement as items are picked
		// until they are all 0, and then they reset. Those generator classes should define
		// defaultProbs. If defaultProbs is null then a deck system isn't used.
		//Artifacts in particular don't reset, no duplicates!
		public float[] probs;
		public float[] defaultProbs = null;

		//some items types have two decks and swap between them
		// this enforces more consistency while still allowing for better precision
		public float[] defaultProbs2 = null;
		public boolean using2ndProbs = false;
		//but in such cases we still need a reference to the full deck in case of non-deck generation
		public float[] defaultProbsTotal = null;

		//These variables are used as a part of the deck system, to ensure that drops are consistent
		// regardless of when they occur (either as part of seeded levelgen, or random item drops)
		public Long seed = null;
		public int dropped = 0;

		//game has two decks of 35 items for overall category probs
		//one deck has a ring and extra armor, the other has an artifact and extra thrown weapon
		//Note that pure random drops only happen as part of levelgen atm, so no seed is needed here
		public float firstProb;
		public float secondProb;
		public Class<? extends Item> superClass;
		
		private Category( float firstProb, float secondProb, Class<? extends Item> superClass ) {
			this.firstProb = firstProb;
			this.secondProb = secondProb;
			this.superClass = superClass;
		}

		//some generator categories can have ordering within that category as well
		// note that sub category ordering doesn't need to always include items that belong
		// to that categories superclass, e.g. bombs are ordered within thrown weapons
		private static HashMap<Class, ArrayList<Class>> subOrderings = new HashMap<>();
		static {
			subOrderings.put(Trinket.class, new ArrayList<>(Arrays.asList(Trinket.class, TrinketCatalyst.class)));
			subOrderings.put(MissileWeapon.class, new ArrayList<>(Arrays.asList(MissileWeapon.class, Bomb.class)));
			subOrderings.put(Potion.class, new ArrayList<>(Arrays.asList(Waterskin.class, Potion.class, ExoticPotion.class, Brew.class, Elixir.class, LiquidMetal.class)));
			subOrderings.put(Scroll.class, new ArrayList<>(Arrays.asList(Scroll.class, ExoticScroll.class, Spell.class, ArcaneResin.class)));
		}

		//in case there are multiple matches, this will return the latest match
		public static int order( Item item ) {
			int catResult = -1, subResult = 0;
			for (int i=0; i < values().length; i++) {
				ArrayList<Class> subOrdering = subOrderings.get(values()[i].superClass);
				if (subOrdering != null){
					for (int j=0; j < subOrdering.size(); j++){
						if (subOrdering.get(j).isInstance(item)){
							catResult = i;
							subResult = j;
						}
					}
				} else {
					if (values()[i].superClass.isInstance(item)) {
						catResult = i;
						subResult = 0;
					}
				}
			}
			if (catResult != -1) return catResult*100 + subResult;

			//items without a category-defined order are sorted based on the spritesheet
			return Short.MAX_VALUE+item.image();
		}

		static {
			GOLD.classes = new Class<?>[]{
					Gold.class };
			GOLD.probs = new float[]{ 1 };
			
			POTION.classes = new Class<?>[]{
					PotionOfStrength.class, //2 drop every chapter, see Dungeon.posNeeded()
					PotionOfHealing.class,
					PotionOfMindVision.class,
					PotionOfFrost.class,
					PotionOfLiquidFlame.class,
					PotionOfToxicGas.class,
					PotionOfHaste.class,
					PotionOfInvisibility.class,
					PotionOfLevitation.class,
					PotionOfParalyticGas.class,
					PotionOfPurity.class,
					PotionOfExperience.class,
					PotionOfOverHealing.class,
					PotionOfMending.class,
					PotionOfMight.class,
					PotionOfMana.class,
					// Overgrown PD Group 1 potions
					PotionOfBall.class,
					PotionOfBlessing.class,
					PotionOfChilli.class,
					PotionOfDew.class,
					PotionOfEye.class,
					PotionOfGlowing.class,
					PotionOfGrass.class,
					PotionOfHoney.class,
					PotionOfLantern.class,
					PotionOfParasites.class,
					PotionOfPeanuts.class,
					PotionOfPepper.class,
					PotionOfProtection.class,
					PotionOfRegrowth.class,
					PotionOfShadows.class,
					PotionOfShield.class,
					PotionOfWine.class,
					PotionOfWithering.class,
					// Overgrown PD Group 2 potions
					PotionOfBanana.class,
					PotionOfEgg.class,
					PotionOfFlora.class,
					PotionOfHunger.class,
					PotionOfLove.class,
					PotionOfMuscle.class,
					PotionOfSeed.class,
					PotionOfTime.class,
					PotionOfTomatoSoup.class,
					PotionOfFirelightning.class,
					PotionOfLightning.class,
					PotionOfDigesting.class,
					PotionOfFirestorm.class,
					PotionOfHydrogenFire.class,
					PotionOfHypno.class,
					PotionOfIceStorm.class,
					PotionOfKiwi.class,
					PotionOfSlowness.class,
					PotionOfSmoke.class,
					PotionOfSnowstorm.class,
					PotionOfSoda.class,
					PotionOfSteam.class,
					PotionOfUltraviolett.class,
					// Overgrown PD Group 4 potions
					PotionOfButter.class,
					PotionOfDirt.class,
					PotionOfGoo.class,
					PotionOfHarvest.class,
					PotionOfInfection.class,
					PotionOfSun.class,
					PotionOfVine.class,
					PotionOfWater.class};
			POTION.defaultProbs  = new float[]{ 0, 3, 2, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1,
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
					1, 1, 1, 1, 1, 1, 1, 1 };
			POTION.defaultProbs2 = new float[]{ 0, 3, 2, 2, 1, 2, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1,
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
					1, 1, 1, 1, 1, 1, 1, 1 };
			POTION.probs = POTION.defaultProbs.clone();
			
			SEED.classes = new Class<?>[]{
					Rotberry.Seed.class, //quest item
					Sungrass.Seed.class,
					Fadeleaf.Seed.class,
					Icecap.Seed.class,
					Firebloom.Seed.class,
					Sorrowmoss.Seed.class,
					Swiftthistle.Seed.class,
					Blindweed.Seed.class,
					Stormvine.Seed.class,
					Earthroot.Seed.class,
					Mageroyal.Seed.class,
					Starflower.Seed.class,
					Dreamfoil.Seed.class,
					Dewcatcher.Seed.class,
					Phaseshift.Seed.class,
					Flytrap.Seed.class,
					Snowhedge.Seed.class,
					Frostcorn.Seed.class,
					Willowcane.Seed.class,
					Rose.Seed.class,
					Clitbalm.Seed.class,
					Hypnohemp.Seed.class,
					Sunbloom.Seed.class,
					Dirtdaisy.Seed.class,
					Grassvine.Seed.class,
					Witherfennel.Seed.class,
					Crimsonpepper.Seed.class,
					Nightshadeonion.Seed.class,
					Steamweed.Seed.class,
					Kiwivetch.Seed.class,
					Gobgrape.Seed.class,
					Peanutpetal.Seed.class,
					Eggbloom.Seed.class,
					Cornwheat.Seed.class,
					Musclemoss.Seed.class,
					Butterlion.Seed.class,
					Flowertree.Seed.class,
					Grasslilly.Seed.class,
					Goograss.Seed.class,
					Sourpitcher.Seed.class,
					Tomatobush.Seed.class,
					Blueeyedsusan.Seed.class,
					Lavenderlantern.Seed.class,
					Apricobush.Seed.class,
					Ballcrop.Seed.class,
					Bananabean.Seed.class,
					Blackholeflower.Seed.class,
					Chandaliertail.Seed.class,
					Chillisnapper.Seed.class,
					Clockcypress.Seed.class,
					Cocostuft.Seed.class,
					Combflower.Seed.class,
					Crimsoncrown.Seed.class,
					Eyeeuonymus.Seed.class,
					Feelerfern.Seed.class,
					Firefoxglove.Seed.class,
					Larvaleaf.Seed.class,
					Lightninglily.Seed.class,
					Parasiteshrub.Seed.class,
					Poppoplar.Seed.class,
					Shadowbloom.Seed.class,
					Suncarnivore.Seed.class,
					Tankcabbage.Seed.class,
					Venusflytrap.Seed.class,
					Waterweed.Seed.class};
			//65 probs to match the 65 SEED.classes above - one extra trailing weight
			//let Random.chances return an out-of-bounds index and crash worldgen
			SEED.defaultProbs = new float[]{ 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 1, 1,
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
			SEED.probs = SEED.defaultProbs.clone();
			
			SCROLL.classes = new Class<?>[]{
					ScrollOfUpgrade.class, //3 drop every chapter, see Dungeon.souNeeded()
					ScrollOfIdentify.class,
					ScrollOfRemoveCurse.class,
					ScrollOfMirrorImage.class,
					ScrollOfRecharging.class,
					ScrollOfTeleportation.class,
					ScrollOfLullaby.class,
					ScrollOfMagicMapping.class,
					ScrollOfRage.class,
					ScrollOfRetribution.class,
					ScrollOfTerror.class,
					ScrollOfTransmutation.class,
					ScrollOfMagicalInfusion.class,
					ScrollOfMultiUpgrade.class,
					ScrollOfRegrowth.class
			};
			//Regrowth is a strict upgrade of MagicMapping - it maps the floor and finds the
			//secrets exactly as MagicMapping does, and floods the level on top. At equal
			//weight there was no reason to ever read MagicMapping, so keep it a rare find.
			SCROLL.defaultProbs  = new float[]{ 0, 3, 2, 1, 2, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0.2f };
			SCROLL.defaultProbs2 = new float[]{ 0, 3, 2, 2, 1, 2, 1, 1, 1, 1, 1, 0, 0, 0, 0.2f };
			SCROLL.probs = SCROLL.defaultProbs.clone();

			SPELLS.classes = new Class<?>[]{
					Alchemize.class,
					AquaBlast.class,
					BeaconOfReturning.class,
					CrimsonEpithet.class,
					CurseInfusion.class,
					DoomCall.class,
					EnchantmentInfusion.class,
					FeatherFall.class,
					Forcefield.class,
					ForcePush.class,
					HolyBlast.class,
					MagicalInfusion.class,
					MagicalPorter.class,
					NaturesLullaby.class,
					PhaseShift.class,
					PlantSummon.class,
					ReclaimTrap.class,
					Recycle.class,
					SeasonChange.class,
					SpontaneousCombustion.class,
					SummonElemental.class,
					TelekineticGrab.class,
					UnstableSpell.class,
					WildEnergy.class,
			};
			SPELLS.probs = new float[]{
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
					1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			};

			STONE.classes = new Class<?>[]{
					StoneOfEnchantment.class,   //1 is guaranteed to drop on floors 6-19
					StoneOfIntuition.class,     //1 additional stone is also dropped on floors 1-3
					StoneOfDetectMagic.class,
					StoneOfFlock.class,
					StoneOfShock.class,
					StoneOfBlink.class,
					StoneOfDeepSleep.class,
					StoneOfClairvoyance.class,
					StoneOfAggression.class,
					StoneOfBlast.class,
					StoneOfFear.class,
					StoneOfAugmentation.class  //1 is sold in each shop
			};
			STONE.defaultProbs = new float[]{ 0, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0 };
			STONE.probs = STONE.defaultProbs.clone();

			WAND.classes = new Class<?>[]{
					WandOfMagicMissile.class,
					WandOfLightning.class,
					WandOfDisintegration.class,
					WandOfFireblast.class,
					WandOfCorrosion.class,
					WandOfBlastWave.class,
					WandOfLivingEarth.class,
					WandOfFrost.class,
					WandOfPrismaticLight.class,
					WandOfWarding.class,
					WandOfTransfusion.class,
					WandOfCorruption.class,
					WandOfRegrowth.class,
					WandOfAmok.class,
					WandOfAvalanche.class,
					WandOfBlink.class,
					WandOfDisintegration2.class,
					WandOfFirebolt.class,
					WandOfFlock.class,
					WandOfPoison.class,
					WandOfSlowness.class,
					WandOfTelekinesis.class,
					WandOfTeleportation.class };
			WAND.defaultProbs = new float[]{ 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3 };
			WAND.probs = WAND.defaultProbs.clone();
			
			//see generator.randomWeapon
			WEAPON.classes = new Class<?>[]{};
			WEAPON.probs = new float[]{};
			
			WEP_T1.classes = new Class<?>[]{
					WornShortsword.class,
					MagesStaff.class,
					Dagger.class,
					Gloves.class,
					Rapier.class,
					Cudgel.class,
					Triangolo.class,
					MageBook.class,
					TrickSand.class,
				Scalpel.class,
				Shovel.class,
				Saber.class,
				PocketKnife.class
			};
			WEP_T1.defaultProbs = new float[]{ 2, 0, 2, 2, 2, 2, 1, 1, 1, 2, 1, 1, 1 };
			WEP_T1.probs = WEP_T1.defaultProbs.clone();
			
			WEP_T2.classes = new Class<?>[]{
					Shortsword.class,
					HandAxe.class,
					Spear.class,
					Quarterstaff.class,
					Dirk.class,
					Sickle.class,
					Pickaxe.class,
					AssassinsKnife.class,
					MageStaff.class
			,
					FightGloves.class,
					DualKnife.class,
					Flute.class,
					MirrorDoll.class,
					Knife.class
			};
			WEP_T2.defaultProbs = new float[]{ 2, 2, 2, 2, 2, 2, 0, 2, 2, 1, 1, 1, 1, 2 };
			WEP_T2.probs = WEP_T2.defaultProbs.clone();
			
			WEP_T3.classes = new Class<?>[]{
					Sword.class,
					Mace.class,
					Scimitar.class,
					RoundShield.class,
					Sai.class,
					Whip.class,
					BroadSword.class
			,
					Nunchakus.class,
					Wardrum.class,
					WindBottle.class,
				Bible.class,
				Bow.class
			};
			WEP_T3.defaultProbs = new float[]{ 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 2, 2 };
			WEP_T3.probs = WEP_T3.defaultProbs.clone();
			
			WEP_T4.classes = new Class<?>[]{
					Longsword.class,
					BattleAxe.class,
					Flail.class,
					RunicBlade.class,
					AssassinsBlade.class,
					Crossbow.class,
					Katana.class,
					Axe.class
			,
					Trumpet.class,
					PrayerWheel.class,
					HandLight.class,
					DeathSword.class
			};
			WEP_T4.defaultProbs = new float[]{ 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1 };
			WEP_T4.probs = WEP_T4.defaultProbs.clone();
			
			WEP_T5.classes = new Class<?>[]{
					Greatsword.class,
					WarHammer.class,
					Glaive.class,
					Greataxe.class,
					Greatshield.class,
					Gauntlet.class,
					WarScythe.class
			,
					Harp.class,
					StoneCross.class,
				LargeSword.class,
				FT.class,
				LG.class,
				Spade.class,
				MinersTool.class
			};
			WEP_T5.defaultProbs = new float[]{ 2, 2, 2, 2, 2, 2, 2, 1, 1, 2, 1, 1, 1, 1 };
			WEP_T5.probs = WEP_T5.defaultProbs.clone();
			
			//see Generator.randomArmor
			ARMOR.classes = new Class<?>[]{
					ClothArmor.class,
					LeatherArmor.class,
					MailArmor.class,
					ScaleArmor.class,
					PlateArmor.class,
					WarriorArmor.class,
					MageArmor.class,
					RogueArmor.class,
					HuntressArmor.class,
					DuelistArmor.class,
					ClericArmor.class
			};
			ARMOR.probs = new float[]{ 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0 };
			
			//see Generator.randomMissile
			MISSILE.classes = new Class<?>[]{};
			MISSILE.probs = new float[]{};
			
			MIS_T1.classes = new Class<?>[]{
					ThrowingStone.class,
					ThrowingKnife.class,
					ThrowingSpike.class,
					Dart.class,
					Skull.class,
					Wave.class,
					//no RiceBall: it is a Sprouted reward drop, not generic missile loot
					Brick.class,
					BottleFire.class,
					HoneyArrow.class,
					ThrowingWave.class,
					ThrowingSkull.class
			};
			MIS_T1.defaultProbs = new float[]{ 3, 3, 3, 0, 2, 2, 1, 1, 1, 1, 1 };
			MIS_T1.probs = MIS_T1.defaultProbs.clone();
			
			MIS_T2.classes = new Class<?>[]{
					FishingSpear.class,
					ThrowingClub.class,
					Shuriken.class,
					CurareShuriken.class,
					IncendiaryShuriken.class,
					ForestDart.class,
					EmpBola.class,
					MindArrow.class,
					HolyWater.class
			};
			MIS_T2.defaultProbs = new float[]{ 3, 3, 3, 2, 2, 2, 1, 1, 3 };
			MIS_T2.probs = MIS_T2.defaultProbs.clone();
			
			MIS_T3.classes = new Class<?>[]{
					ThrowingSpear.class,
					Kunai.class,
					Bolas.class,
					SmallChakram.class
			};
			MIS_T3.defaultProbs = new float[]{ 3, 3, 3, 2 };
			MIS_T3.probs = MIS_T3.defaultProbs.clone();
			
			MIS_T4.classes = new Class<?>[]{
					Javelin.class,
					Tomahawk.class,
					HeavyBoomerang.class
			};
			MIS_T4.defaultProbs = new float[]{ 3, 3, 3 };
			MIS_T4.probs = MIS_T4.defaultProbs.clone();
			
			MIS_T5.classes = new Class<?>[]{
					Trident.class,
					ThrowingHammer.class,
					ForceCube.class,
					HugeShuriken.class
			};
			MIS_T5.defaultProbs = new float[]{ 3, 3, 3, 2 };
			MIS_T5.probs = MIS_T5.defaultProbs.clone();
			
			FOOD.classes = new Class<?>[]{
					Food.class,
					Pasty.class,
					MysteryMeat.class,
					Blackberry.class,
					Blueberry.class,
					BlueMilk.class,
					Cloudberry.class,
					DeathCap.class,
					Earthstar.class,
					FullMoonberry.class,
					GoldenJelly.class,
					//no GoldenNut here: it grants up to +5 STR and +50 HT permanently, and its
					//only source is meant to be the CityKey reward, gated behind 99 kills of
					//four mob types. At 1/20 of the food pool it was worth ~1.25 free nuts a
					//run, farmable, which is a different game.
					JackOLantern.class,
					Moonberry.class,
					Nut.class,
					ToastedNut.class,
					PixieParasol.class,
					PotionOfConstitution.class };
			FOOD.defaultProbs = new float[]{ 4, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
			FOOD.probs = FOOD.defaultProbs.clone();
			
			RING.classes = new Class<?>[]{
					RingOfAccuracy.class,
					RingOfArcana.class,
					RingOfElements.class,
					RingOfEnergy.class,
					RingOfEvasion.class,
					RingOfForce.class,
					RingOfFuror.class,
					RingOfHaste.class,
					RingOfMight.class,
					RingOfSharpshooting.class,
					RingOfTenacity.class,
					RingOfWealth.class,
					RingOfMagic.class,
					RingOfSating.class,
					RingOfSearching.class};
			RING.defaultProbs = new float[]{ 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3 };
			RING.probs = RING.defaultProbs.clone();
			
			ARTIFACT.classes = new Class<?>[]{
					AlchemistsToolkit.class,
					ChaliceOfBlood.class,
					CloakOfShadows.class,
					DriedRose.class,
					EtherealChains.class,
					HolyTome.class,
					HornOfPlenty.class,
					MasterThievesArmband.class,
					SandalsOfNature.class,
					SkeletonKey.class,
					TalismanOfForesight.class,
					TimekeepersHourglass.class,
					UnstableSpellbook.class,
					RingOfDisintegration.class,
					RingOfFrost.class
			};
			ARTIFACT.defaultProbs = new float[]{ 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
			ARTIFACT.probs = ARTIFACT.defaultProbs.clone();

			//Trinkets are unique like artifacts, but unlike them you can only have one at once
			//So we don't need the same enforcement of uniqueness
			TRINKET.classes = new Class<?>[]{
					RatSkull.class,
					ParchmentScrap.class,
					PetrifiedSeed.class,
					ExoticCrystals.class,
					MossyClump.class,
					DimensionalSundial.class,
					ThirteenLeafClover.class,
					TrapMechanism.class,
					MimicTooth.class,
					WondrousResin.class,
					EyeOfNewt.class,
					SaltCube.class,
					VialOfBlood.class,
					ShardOfOblivion.class,
					ChaoticCenser.class,
					FerretTuft.class,
					CrackedSpyglass.class
			};
			TRINKET.defaultProbs = new float[]{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
			TRINKET.probs = TRINKET.defaultProbs.clone();

			for (Category cat : Category.values()){
				if (cat.defaultProbs2 != null){
					cat.defaultProbsTotal = new float[cat.defaultProbs.length];
					for (int i = 0; i < cat.defaultProbs.length; i++){
						cat.defaultProbsTotal[i] = cat.defaultProbs[i] + cat.defaultProbs2[i];
					}
				}
			}
		}
	}

	private static final float[][] floorSetTierProbs = new float[][] {
			{0, 75, 20,  4,  1},
			{0, 25, 50, 20,  5},
			{0,  0, 40, 50, 10},
			{0,  0, 20, 40, 40},
			{0,  0,  0, 20, 80}
	};

	private static boolean usingFirstDeck = false;
	private static HashMap<Category,Float> defaultCatProbs = new LinkedHashMap<>();
	private static HashMap<Category,Float> categoryProbs = new LinkedHashMap<>();

	public static void fullReset() {
		usingFirstDeck = Random.Int(2) == 0;
		generalReset();
		for (Category cat : Category.values()) {
			cat.using2ndProbs =  cat.defaultProbs2 != null && Random.Int(2) == 0;
			reset(cat);
			if (cat.defaultProbs != null) {
				cat.seed = Random.Long();
				cat.dropped = 0;
			}
		}
	}

	public static void generalReset(){
		for (Category cat : Category.values()) {
			categoryProbs.put( cat, usingFirstDeck ? cat.firstProb : cat.secondProb );
			defaultCatProbs.put( cat, cat.firstProb + cat.secondProb );
		}
	}

	public static void reset(Category cat){
		if (cat.defaultProbs != null) {
			if (cat.defaultProbs2 != null){
				cat.using2ndProbs = !cat.using2ndProbs;
				cat.probs = cat.using2ndProbs ? cat.defaultProbs2.clone() : cat.defaultProbs.clone();
			} else {
				cat.probs = cat.defaultProbs.clone();
			}
		}
	}

	//reverts changes to drop chances generates by this item
	//equivalent of shuffling the card back into the deck, does not preserve order!
	public static void undoDrop(Item item){
		undoDrop(item.getClass());
	}

	public static void undoDrop(Class cls){
		for (Category cat : Category.values()){
			if (cls.isAssignableFrom(cat.superClass)){
				if (cat.defaultProbs == null) continue;
				for (int i = 0; i < cat.classes.length; i++){
					if (cls == cat.classes[i]){
						cat.probs[i]++;
					}
				}
			}
		}
	}
	
	public static Item random() {
		Category cat = Random.chances( categoryProbs );
		if (cat == null){
			usingFirstDeck = !usingFirstDeck;
			generalReset();
			cat = Random.chances( categoryProbs );
		}
		categoryProbs.put( cat, categoryProbs.get( cat ) - 1);

		if (cat == Category.SEED) {
			//We specifically use defaults for seeds here because, unlike other item categories
			// their predominant source of drops is grass, not levelgen. This way the majority
			// of seed drops still use a deck, but the few that are spawned by levelgen are consistent
			return randomUsingDefaults(cat);
		} else {
			return random(cat);
		}
	}

	public static Item randomUsingDefaults(){
		return randomUsingDefaults(Random.chances( defaultCatProbs ));
	}
	
	public static Item random( Category cat ) {
		switch (cat) {
			case ARMOR:
				return randomArmor();
			case WEAPON:
				return randomWeapon();
			case MISSILE:
				return randomMissile();
			case ARTIFACT:
				Item item = randomArtifact();
				//if we're out of artifacts, return a ring instead.
				return item != null ? item : random(Category.RING);
			default:
				if (cat.defaultProbs != null && cat.seed != null){
					Random.pushGenerator(cat.seed);
					for (int i = 0; i < cat.dropped; i++) Random.Long();
				}

				int i = Random.chances(cat.probs);
				if (i == -1) {
					reset(cat);
					i = Random.chances(cat.probs);
				}
				if (cat.defaultProbs != null) cat.probs[i]--;
				Class<?> itemCls = cat.classes[i];

				if (cat.defaultProbs != null && cat.seed != null){
					Random.popGenerator();
					cat.dropped++;
				}

				if (ExoticPotion.regToExo.containsKey(itemCls)){
					if (Random.Float() < ExoticCrystals.consumableExoticChance()){
						itemCls = ExoticPotion.regToExo.get(itemCls);
					}
				} else if (ExoticScroll.regToExo.containsKey(itemCls)){
					if (Random.Float() < ExoticCrystals.consumableExoticChance()){
						itemCls = ExoticScroll.regToExo.get(itemCls);
					}
				}

				return ((Item) Reflection.newInstance(itemCls)).random();
		}
	}

	//overrides any deck systems and always uses default probs
	// except for artifacts, which must always use a deck
	public static Item randomUsingDefaults( Category cat ){
		if (cat == Category.WEAPON){
			return randomWeapon(true);
		} else if (cat == Category.MISSILE){
			return randomMissile(true);
		} else if (cat.defaultProbs == null || cat == Category.ARTIFACT) {
			return random(cat);
		} else if (cat.defaultProbsTotal != null){
			return ((Item) Reflection.newInstance(cat.classes[Random.chances(cat.defaultProbsTotal)])).random();
		} else {
			Class<?> itemCls = cat.classes[Random.chances(cat.defaultProbs)];

			if (ExoticPotion.regToExo.containsKey(itemCls)){
				if (Random.Float() < ExoticCrystals.consumableExoticChance()){
					itemCls = ExoticPotion.regToExo.get(itemCls);
				}
			} else if (ExoticScroll.regToExo.containsKey(itemCls)){
				if (Random.Float() < ExoticCrystals.consumableExoticChance()){
					itemCls = ExoticScroll.regToExo.get(itemCls);
				}
			}

			return ((Item) Reflection.newInstance(itemCls)).random();
		}
	}
	
	public static Item random( Class<? extends Item> cl ) {
		return Reflection.newInstance(cl).random();
	}

	public static Armor randomArmor(){
		return randomArmor(Dungeon.depth / 5);
	}
	
	//special armors dropped alongside the vanilla tier armors, matched by tier
	private static final Class<?>[][] SPECIAL_ARMORS = new Class<?>[][]{
			{LifeArmor.class},
			{PerformerArmor.class},
			{AsceticArmor.class, FollowerArmor.class, CeramicsArmor.class},
			{SoldierArmor.class},
			{BulletArmor.class, MachineArmor.class}
	};

	public static Armor randomArmor(int floorSet) {

		floorSet = (int)GameMath.gate(0, floorSet, floorSetTierProbs.length-1);

		int tierIdx = Random.chances(floorSetTierProbs[floorSet]);

		//1 in 10 armor drops roll a ported special of the same tier
		if (tierIdx < SPECIAL_ARMORS.length && Random.Int(10) == 0){
			Armor sp = (Armor)Reflection.newInstance(
					(Class<? extends Armor>) Random.oneOf(SPECIAL_ARMORS[tierIdx]));
			sp.random();
			return sp;
		}

		Armor a = (Armor)Reflection.newInstance(Category.ARMOR.classes[tierIdx]);
		a.random();
		return a;
	}

	public static final Category[] wepTiers = new Category[]{
			Category.WEP_T1,
			Category.WEP_T2,
			Category.WEP_T3,
			Category.WEP_T4,
			Category.WEP_T5
	};

	public static MeleeWeapon randomWeapon(){
		return randomWeapon(Dungeon.depth / 5);
	}

	public static MeleeWeapon randomWeapon(int floorSet) {
		return randomWeapon(floorSet, false);
	}

	public static MeleeWeapon randomWeapon(boolean useDefaults) {
		return randomWeapon(Dungeon.depth / 5, useDefaults);
	}
	
	public static MeleeWeapon randomWeapon(int floorSet, boolean useDefaults) {

		floorSet = (int)GameMath.gate(0, floorSet, floorSetTierProbs.length-1);

		MeleeWeapon w;
		if (useDefaults){
			w = (MeleeWeapon) randomUsingDefaults(wepTiers[Random.chances(floorSetTierProbs[floorSet])]);
		} else {
			w = (MeleeWeapon) random(wepTiers[Random.chances(floorSetTierProbs[floorSet])]);
		}
		return w;
	}
	
	public static final Category[] misTiers = new Category[]{
			Category.MIS_T1,
			Category.MIS_T2,
			Category.MIS_T3,
			Category.MIS_T4,
			Category.MIS_T5
	};
	
	public static MissileWeapon randomMissile(){
		return randomMissile(Dungeon.depth / 5);
	}

	public static MissileWeapon randomMissile(int floorSet) {
		return randomMissile(floorSet, false);
	}

	public static MissileWeapon randomMissile(boolean useDefaults) {
		return randomMissile(Dungeon.depth / 5, useDefaults);
	}

	public static MissileWeapon randomMissile(int floorSet, boolean useDefaults) {
		
		floorSet = (int)GameMath.gate(0, floorSet, floorSetTierProbs.length-1);

		MissileWeapon w;
		if (useDefaults){
			w = (MissileWeapon)randomUsingDefaults(misTiers[Random.chances(floorSetTierProbs[floorSet])]);
		} else {
			w = (MissileWeapon)random(misTiers[Random.chances(floorSetTierProbs[floorSet])]);
		}
		return w;
	}

	//enforces uniqueness of artifacts throughout a run.
	public static Artifact randomArtifact() {

		Category cat = Category.ARTIFACT;

		if (cat.defaultProbs != null && cat.seed != null){
			Random.pushGenerator(cat.seed);
			for (int i = 0; i < cat.dropped; i++) Random.Long();
		}

		int i = Random.chances( cat.probs );

		if (cat.defaultProbs != null && cat.seed != null){
			Random.popGenerator();
			cat.dropped++;
		}

		//if no artifacts are left, return null
		if (i == -1){
			return null;
		}

		cat.probs[i]--;
		return (Artifact) Reflection.newInstance((Class<? extends Artifact>) cat.classes[i]).random();

	}

	public static boolean removeArtifact(Class<?extends Artifact> artifact) {
		Category cat = Category.ARTIFACT;
		for (int i = 0; i < cat.classes.length; i++){
			if (cat.classes[i].equals(artifact) && cat.probs[i] > 0) {
				cat.probs[i] = 0;
				return true;
			}
		}
		return false;
	}

	private static final String FIRST_DECK = "first_deck";
	private static final String GENERAL_PROBS = "general_probs";
	private static final String CATEGORY_PROBS = "_probs";
	private static final String CATEGORY_USING_PROBS2 = "_using_probs2";
	private static final String CATEGORY_SEED = "_seed";
	private static final String CATEGORY_DROPPED = "_dropped";

	public static void storeInBundle(Bundle bundle) {
		bundle.put(FIRST_DECK, usingFirstDeck);

		Float[] genProbs = categoryProbs.values().toArray(new Float[0]);
		float[] storeProbs = new float[genProbs.length];
		for (int i = 0; i < storeProbs.length; i++){
			storeProbs[i] = genProbs[i];
		}
		bundle.put( GENERAL_PROBS, storeProbs);

		for (Category cat : Category.values()){
			if (cat.defaultProbs == null) continue;

			bundle.put(cat.name().toLowerCase() + CATEGORY_PROBS, cat.probs);

			if (cat.defaultProbs2 != null){
				bundle.put(cat.name().toLowerCase() + CATEGORY_USING_PROBS2, cat.using2ndProbs);
			}

			if (cat.seed != null) {
				bundle.put(cat.name().toLowerCase() + CATEGORY_SEED, cat.seed);
				bundle.put(cat.name().toLowerCase() + CATEGORY_DROPPED, cat.dropped);
			}
		}
	}

	public static void restoreFromBundle(Bundle bundle) {
		fullReset();

		usingFirstDeck = bundle.getBoolean(FIRST_DECK);

		if (bundle.contains(GENERAL_PROBS)){
			float[] probs = bundle.getFloatArray(GENERAL_PROBS);
			if (probs.length == Category.values().length) {
				for (int i = 0; i < probs.length; i++) {
					categoryProbs.put(Category.values()[i], probs[i]);
				}
			}
		}

		for (Category cat : Category.values()){
			if (bundle.contains(cat.name().toLowerCase() + CATEGORY_PROBS)){
				float[] probs = bundle.getFloatArray(cat.name().toLowerCase() + CATEGORY_PROBS);
				if (cat.defaultProbs != null && probs.length == cat.defaultProbs.length){
					cat.probs = probs;
				}
				if (bundle.contains(cat.name().toLowerCase() + CATEGORY_USING_PROBS2)){
					cat.using2ndProbs = bundle.getBoolean(cat.name().toLowerCase() + CATEGORY_USING_PROBS2);
				} else {
					cat.using2ndProbs = false;
				}
				if (bundle.contains(cat.name().toLowerCase() + CATEGORY_SEED)){
					cat.seed = bundle.getLong(cat.name().toLowerCase() + CATEGORY_SEED);
					cat.dropped = bundle.getInt(cat.name().toLowerCase() + CATEGORY_DROPPED);
				}

				//pre-v3.0.0 and pre-v3.3.0 conversion for artifacts (addition of tome and key)
				if (cat == Category.ARTIFACT && probs.length != cat.defaultProbs.length){
					int tomeIDX = 5;
					int keyIDX = 9;
					int j = 0;
					for (int i = 0; i < probs.length; i++){
						//we do a specific check here for holy tome pre-v3.0.0
						if (j == tomeIDX && probs.length == cat.defaultProbs.length-2){
							cat.probs[j] = 0;
							j++;
						} else if (j == keyIDX){
							cat.probs[j] = 1;
							j++;
						}
						cat.probs[j] = probs[i];
						j++;
					}

				}

			}
		}
		
	}
}

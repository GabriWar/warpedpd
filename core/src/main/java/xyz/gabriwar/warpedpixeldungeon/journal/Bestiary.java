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

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderQueen;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderNest;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderEgg;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderExploding;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderMind;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderServant;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceGuardianCore;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceGuardian;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CagedKobold;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.KoboldIcemancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ColdSpirit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.TempleSentry;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.TempleEbonyMimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SlimeBrown;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SlimeRed;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zombie;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ClayGolem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderBot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tinkerer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Minotaur;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ChaosMage;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LostSoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceDemon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DemonLord;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Squid;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Acidic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AdultDragonViolet;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Albino;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoPiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Assassin;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ArmoredBrute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ArmoredStatue;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BanditKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bandit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bee;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BlueCat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BlueWraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrokenRobot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownBat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Brute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CausticSlime;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ControlPanel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Crab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrabKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalGuardian;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalMimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalSpire;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalWisp;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DemonGoo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM100;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM200;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM201;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM300;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DemonSpawner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKingTomb;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfLich;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.EbonyMimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Elemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eye;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FetidRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FishProtector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FlyingProtector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ForestProtector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FossilSkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Ghoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollArcher;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollExile;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GoldThief;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GraveProtector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreyOni;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreyRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gullin;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollGeomancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollSapper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollTrickster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GoldenMimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Golem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreatCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Guard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HermitCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShellCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Kupua;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Lichen;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LitTower;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MagicEye;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MineSentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MonsterBox;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MossySkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MrDestructo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MrDestructo2dot0;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Necromancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Oni;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OrbOfZotMob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Otiluke;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.PhantomPiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Piranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.PoisonGoo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Pylon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RatBoss;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RedWraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RipperDemon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RotHeart;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RotLasher;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Scorpio;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SeekingBomb;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SeekingClusterBomb;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Senior;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Sentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShadowYog;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shell;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkeletonHand1;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkeletonHand2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkeletonKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SokobanSentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpectralRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shaman;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Slime;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpectralNecromancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Spinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SteelBee;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Succubus;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Swarm;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Thief;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ThiefKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.TormentedSpirit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tower;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.VaultProtector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Warlock;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Wraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogDzewa;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogFist;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ZotPhase;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LivingPlant;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Ghost;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Imp;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.MirrorImage;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OtilukeNPC;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PrismaticImage;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RatKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Sheep;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer1;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer3;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer4;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer5;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TownGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Bard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Bishop;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Drunkard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.FortuneTellerFolk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.InnKeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Librarian;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Mercenary;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.InnServant;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Employee;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownGuardFolk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownsfolkMovie;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Townsfolk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownsfolkSilent;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Wandmaker;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.BlueDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Fairy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.GreenDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.RedDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Scorpion;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.ShadowDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.SugarplumFairy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Velocirooster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.VioletDragon;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.DriedRose;
import xyz.gabriwar.warpedpixeldungeon.items.quest.CorpseDust;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfLivingEarth;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfWarding;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.SentryRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.AlarmTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.BlazingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.BurningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ChillingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ConfusionTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.CorrosionTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.CursingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.DisarmingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.DisintegrationTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.DistortionTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ExplosiveTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FlashingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FlockTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FrostTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GatewayTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GeyserTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GnollRockfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GrimTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GrippingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GuardianTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.OozeTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.PitfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.PoisonDartTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.RockfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ShockingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.StormTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SummoningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.TeleportationTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.TenguDartTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ToxicTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WarpingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WeakeningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WornDartTrap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Blindweed;
import xyz.gabriwar.warpedpixeldungeon.plants.Blueeyedsusan;
import xyz.gabriwar.warpedpixeldungeon.plants.Butterlion;
import xyz.gabriwar.warpedpixeldungeon.plants.Apricobush;
import xyz.gabriwar.warpedpixeldungeon.plants.Ballcrop;
import xyz.gabriwar.warpedpixeldungeon.plants.Bananabean;
import xyz.gabriwar.warpedpixeldungeon.plants.Blackholeflower;
import xyz.gabriwar.warpedpixeldungeon.plants.Chandaliertail;
import xyz.gabriwar.warpedpixeldungeon.plants.Chillisnapper;
import xyz.gabriwar.warpedpixeldungeon.plants.Clitbalm;
import xyz.gabriwar.warpedpixeldungeon.plants.Clockcypress;
import xyz.gabriwar.warpedpixeldungeon.plants.Cocostuft;
import xyz.gabriwar.warpedpixeldungeon.plants.Combflower;
import xyz.gabriwar.warpedpixeldungeon.plants.Cornwheat;
import xyz.gabriwar.warpedpixeldungeon.plants.Crimsoncrown;
import xyz.gabriwar.warpedpixeldungeon.plants.Crimsonpepper;
import xyz.gabriwar.warpedpixeldungeon.plants.Dewcatcher;
import xyz.gabriwar.warpedpixeldungeon.plants.Dirtdaisy;
import xyz.gabriwar.warpedpixeldungeon.plants.Dreamfoil;
import xyz.gabriwar.warpedpixeldungeon.plants.Earthroot;
import xyz.gabriwar.warpedpixeldungeon.plants.Eggbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Eyeeuonymus;
import xyz.gabriwar.warpedpixeldungeon.plants.Flytrap;
import xyz.gabriwar.warpedpixeldungeon.plants.Fadeleaf;
import xyz.gabriwar.warpedpixeldungeon.plants.Feelerfern;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Firefoxglove;
import xyz.gabriwar.warpedpixeldungeon.plants.Flowertree;
import xyz.gabriwar.warpedpixeldungeon.plants.Gobgrape;
import xyz.gabriwar.warpedpixeldungeon.plants.Goograss;
import xyz.gabriwar.warpedpixeldungeon.plants.Grasslilly;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

//contains all the game's various entities, mostly enemies, NPCS, and allies, but also traps and plants
public enum Bestiary {

	REGIONAL,
	BOSSES,
	UNIVERSAL,
	RARE,
	QUEST,
	NEUTRAL,
	ALLY,
	TRAP,
	PLANT;

	//tracks whether an entity has been encountered
	private final LinkedHashMap<Class<?>, Boolean> seen = new LinkedHashMap<>();
	//tracks enemy kills, trap activations, plant tramples, or just sets to 1 for seen on allies
	private final LinkedHashMap<Class<?>, Integer> encounterCount = new LinkedHashMap<>();
	//bitmask tracking which loot slots have been seen to drop (bit 0 = primary, bit 1 = secondary, bit 2 = third)
	private final LinkedHashMap<Class<?>, Integer> lootSeen = new LinkedHashMap<>();

	//should only be used when initializing
	private void addEntities(Class<?>... classes ){
		for (Class<?> cls : classes){
			seen.put(cls, false);
			encounterCount.put(cls, 0);
			lootSeen.put(cls, 0);
		}
	}

	public Collection<Class<?>> entities(){
		return seen.keySet();
	}

	public String title(){
		return Messages.get(this, name() + ".title");
	}

	public int totalEntities(){
		return seen.size();
	}

	public int totalSeen(){
		int seenTotal = 0;
		for (boolean entitySeen : seen.values()){
			if (entitySeen) seenTotal++;
		}
		return seenTotal;
	}

	static {

		REGIONAL.addEntities(Rat.class, Snake.class, Gnoll.class, Swarm.class, Crab.class, Slime.class,
				Skeleton.class, Thief.class, DM100.class, Guard.class, Necromancer.class,
				Bat.class, Brute.class, Shaman.RedShaman.class, Shaman.BlueShaman.class, Shaman.PurpleShaman.class, Spinner.class, DM200.class,
				Ghoul.class, Elemental.FireElemental.class, Elemental.FrostElemental.class, Elemental.ShockElemental.class, Warlock.class, Monk.class, Golem.class,
				RipperDemon.class, DemonSpawner.class, Succubus.class, Eye.class, Scorpio.class,
				BrownBat.class, GreyRat.class, SpectralRat.class,
				BlueCat.class, GoldThief.class, Assassin.class, BanditKing.class,
				BlueWraith.class, RedWraith.class, FossilSkeleton.class, MossySkeleton.class,
				BrokenRobot.class, MagicEye.class, Oni.class, GreyOni.class,
				DemonGoo.class, PoisonGoo.class, DwarfLich.class, AlbinoPiranha.class,
				Sentinel.class, MineSentinel.class, SokobanSentinel.class,
				GnollArcher.class, FlyingProtector.class, ForestProtector.class, FishProtector.class, GraveProtector.class,
				VaultProtector.class, MonsterBox.class, RatBoss.class,
				//Unleashed PD ports
				SlimeBrown.class, SlimeRed.class, Zombie.class,
				ClayGolem.class, SpiderBot.class, Tinkerer.class, Minotaur.class,
				ChaosMage.class, LostSoul.class,
				BrownWolf.class, GrayWolf.class, Yeti.class, IceDemon.class,
				ColdSpirit.class, KoboldIcemancer.class, IceGuardian.class, IceGuardianCore.class,
				SpiderServant.class, SpiderGuard.class, SpiderMind.class, SpiderExploding.class,
				SpiderEgg.class, SpiderNest.class, SpiderQueen.class);

		BOSSES.addEntities(Goo.class,
				Tengu.class,
				Pylon.class, DM300.class,
				DwarfKing.class,
				YogDzewa.Larva.class, YogFist.BurningFist.class, YogFist.SoiledFist.class, YogFist.RottingFist.class, YogFist.RustedFist.class,YogFist.BrightFist.class, YogFist.DarkFist.class, YogDzewa.class,
				CrabKing.class, SkeletonKing.class, ThiefKing.class,
				Otiluke.class, Gullin.class, Kupua.class,
				DwarfKingTomb.class, AdultDragonViolet.class,
				Shell.class, ShellCrab.class, SkeletonHand1.class, SkeletonHand2.class,
				ShadowYog.class, Zot.class, ZotPhase.class,
				DemonLord.class);

		UNIVERSAL.addEntities(Wraith.class, Piranha.class, Squid.class, TempleEbonyMimic.class, TempleSentry.class, Mimic.class, GoldenMimic.class, EbonyMimic.class, Statue.class, GuardianTrap.Guardian.class, SentryRoom.Sentry.class);

		RARE.addEntities(Albino.class, GnollExile.class, HermitCrab.class, CausticSlime.class,
				Bandit.class, SpectralNecromancer.class,
				ArmoredBrute.class, DM201.class,
				Elemental.ChaosElemental.class, Senior.class,
				Acidic.class,
				TormentedSpirit.class, PhantomPiranha.class, CrystalMimic.class, ArmoredStatue.class);

		QUEST.addEntities(FetidRat.class, GnollTrickster.class, GreatCrab.class,
				Elemental.NewbornFireElemental.class, RotLasher.class, RotHeart.class,
				CrystalWisp.class, CrystalGuardian.class, CrystalSpire.class, GnollGuard.class, GnollSapper.class, GnollGeomancer.class);

		NEUTRAL.addEntities(CagedKobold.class, Ghost.class, RatKing.class, Shopkeeper.class, Wandmaker.class, Blacksmith.class, Imp.class, Sheep.class, Bee.class,
				Tower.class, LitTower.class, ControlPanel.class,
				Tinkerer1.class, Tinkerer2.class, Tinkerer3.class, Tinkerer4.class, Tinkerer5.class,
				TownGuard.class, Blacksmith2.class, OtilukeNPC.class,
				Bard.class, Bishop.class, Drunkard.class, FortuneTellerFolk.class, InnKeeper.class, Librarian.class, Mercenary.class, InnServant.class, Employee.class, TownGuardFolk.class, TownsfolkMovie.class, Townsfolk.class, TownsfolkSilent.class);

		ALLY.addEntities(MirrorImage.class, PrismaticImage.class,
				DriedRose.GhostHero.class,
				WandOfWarding.Ward.class, WandOfWarding.Ward.WardSentry.class, WandOfLivingEarth.EarthGuardian.class,
				ShadowClone.ShadowAlly.class, SmokeBomb.NinjaLog.class, SpiritHawk.HawkAlly.class, PowerOfMany.LightAlly.class,
				Lichen.class, MrDestructo.class, MrDestructo2dot0.class, OrbOfZotMob.class, SteelBee.class,
				SeekingBomb.class, SeekingClusterBomb.class,
				BlueDragon.class, GreenDragon.class, RedDragon.class, VioletDragon.class, ShadowDragon.class,
				Fairy.class, SugarplumFairy.class,
				Bunny.class, Scorpion.class, Velocirooster.class,
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Spider.class,
				xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Bee.class);

		TRAP.addEntities(WornDartTrap.class, PoisonDartTrap.class, DisintegrationTrap.class, GatewayTrap.class,
				ChillingTrap.class, BurningTrap.class, ShockingTrap.class, AlarmTrap.class, GrippingTrap.class, TeleportationTrap.class, OozeTrap.class,
				FrostTrap.class, BlazingTrap.class, StormTrap.class, GuardianTrap.class, FlashingTrap.class, WarpingTrap.class,
				ConfusionTrap.class, ToxicTrap.class, CorrosionTrap.class,
				FlockTrap.class, SummoningTrap.class, WeakeningTrap.class, CursingTrap.class,
				GeyserTrap.class, ExplosiveTrap.class, RockfallTrap.class, PitfallTrap.class,
				DistortionTrap.class, DisarmingTrap.class, GrimTrap.class);

		PLANT.addEntities(Rotberry.class, Sungrass.class, Fadeleaf.class, Icecap.class,
				Firebloom.class, Sorrowmoss.class, Swiftthistle.class, Blindweed.class,
				Stormvine.class, Earthroot.class, Mageroyal.class, Starflower.class,
				BlandfruitBush.class, Dreamfoil.class, Dewcatcher.class, Phaseshift.class, Flytrap.class,
				WandOfRegrowth.Dewcatcher.class, WandOfRegrowth.Seedpod.class, WandOfRegrowth.Lotus.class,
				Snowhedge.class, Frostcorn.class, Willowcane.class, Rose.class,
				Clitbalm.class, Hypnohemp.class, Sunbloom.class, Dirtdaisy.class,
				Grassvine.class, Witherfennel.class, Crimsonpepper.class, Nightshadeonion.class,
				Steamweed.class, Kiwivetch.class,
				Gobgrape.class, Peanutpetal.class, Eggbloom.class, Cornwheat.class,
				Musclemoss.class, Butterlion.class, Flowertree.class, Grasslilly.class,
				Goograss.class, Sourpitcher.class, Tomatobush.class, Blueeyedsusan.class,
				Lavenderlantern.class,
				Apricobush.class, Ballcrop.class, Bananabean.class, Blackholeflower.class,
				Chandaliertail.class, Chillisnapper.class, Clockcypress.class, Cocostuft.class,
				Combflower.class, Crimsoncrown.class, Eyeeuonymus.class,
				Feelerfern.class, Firefoxglove.class, Larvaleaf.class, Lightninglily.class,
				Parasiteshrub.class, Poppoplar.class, Shadowbloom.class, Suncarnivore.class,
				Tankcabbage.class, Venusflytrap.class, Waterweed.class,
				LivingPlant.class);

	}

	//some mobs and traps have different internal classes in some cases, so need to convert here
	private static final HashMap<Class<?>, Class<?>> classConversions = new HashMap<>();
	static {
		classConversions.put(CorpseDust.DustWraith.class,      Wraith.class);

		classConversions.put(Necromancer.NecroSkeleton.class,  Skeleton.class);

		classConversions.put(TenguDartTrap.class,              PoisonDartTrap.class);
		classConversions.put(GnollRockfallTrap.class,          RockfallTrap.class);

		classConversions.put(DwarfKing.DKGhoul.class,          Ghoul.class);
		classConversions.put(DwarfKing.DKWarlock.class,        Warlock.class);
		classConversions.put(DwarfKing.DKMonk.class,           Monk.class);
		classConversions.put(DwarfKing.DKGolem.class,          Golem.class);

		classConversions.put(YogDzewa.YogRipper.class,         RipperDemon.class);
		classConversions.put(YogDzewa.YogEye.class,            Eye.class);
		classConversions.put(YogDzewa.YogScorpio.class,        Scorpio.class);
	}

	public static boolean isSeen(Class<?> cls){
		for (Bestiary cat : values()) {
			if (cat.seen.containsKey(cls)) {
				return cat.seen.get(cls);
			}
		}
		return false;
	}

	public static void setSeen(Class<?> cls){
		if (classConversions.containsKey(cls)){
			cls = classConversions.get(cls);
		}
		for (Bestiary cat : values()) {
			if (cat.seen.containsKey(cls) && !cat.seen.get(cls)) {
				cat.seen.put(cls, true);
				Journal.saveNeeded = true;
			}
		}

		//facing a guide boss opens its chapter in the Descent Guide
		String guideKey = GuideGraph.keyForMob(cls);
		if (guideKey != null){
			GuideProgress.findPage(guideKey);
		}

		Badges.validateCatalogBadges();
	}

	public static int encounterCount(Class<?> cls) {
		for (Bestiary cat : values()) {
			if (cat.encounterCount.containsKey(cls)) {
				return cat.encounterCount.get(cls);
			}
		}
		return 0;
	}

	//used primarily when bosses are killed and need to clean up their minions
	public static boolean skipCountingEncounters = false;

	public static void countEncounter(Class<?> cls){
		countEncounters(cls, 1);
	}

	public static void countEncounters(Class<?> cls, int encounters){
		if (skipCountingEncounters){
			return;
		}
		if (classConversions.containsKey(cls)){
			cls = classConversions.get(cls);
		}
		for (Bestiary cat : values()) {
			if (cat.encounterCount.containsKey(cls) && cat.encounterCount.get(cls) != Integer.MAX_VALUE){
				cat.encounterCount.put(cls, cat.encounterCount.get(cls)+encounters);
				if (cat.encounterCount.get(cls) < -1_000_000_000){ //to catch cases of overflow
					cat.encounterCount.put(cls, Integer.MAX_VALUE);
				}
				Journal.saveNeeded = true;
			}
		}
	}

	public static boolean isLootSlotSeen(Class<?> cls, int slot){
		for (Bestiary cat : values()) {
			if (cat.lootSeen.containsKey(cls)) {
				return (cat.lootSeen.get(cls) & (1 << slot)) != 0;
			}
		}
		return false;
	}

	public static void setLootSlotSeen(Class<?> cls, int slot){
		if (classConversions.containsKey(cls)){
			cls = classConversions.get(cls);
		}
		for (Bestiary cat : values()) {
			if (cat.lootSeen.containsKey(cls)) {
				int current = cat.lootSeen.get(cls);
				int updated = current | (1 << slot);
				if (current != updated) {
					cat.lootSeen.put(cls, updated);
					Journal.saveNeeded = true;
				}
			}
		}
	}

	private static final String BESTIARY_CLASSES    = "bestiary_classes";
	private static final String BESTIARY_SEEN       = "bestiary_seen";
	private static final String BESTIARY_ENCOUNTERS = "bestiary_encounters";
	private static final String BESTIARY_LOOT_SEEN  = "bestiary_loot_seen";

	public static void store( Bundle bundle ){

		ArrayList<Class<?>> classes = new ArrayList<>();
		ArrayList<Boolean> seen = new ArrayList<>();
		ArrayList<Integer> encounters = new ArrayList<>();
		ArrayList<Integer> lootSeenList = new ArrayList<>();

		for (Bestiary cat : values()) {
			for (Class<?> entity : cat.entities()) {
				if (cat.seen.get(entity) || cat.encounterCount.get(entity) > 0 || cat.lootSeen.get(entity) > 0){
					classes.add(entity);
					seen.add(cat.seen.get(entity));
					encounters.add(cat.encounterCount.get(entity));
					lootSeenList.add(cat.lootSeen.get(entity));
				}
			}
		}

		Class<?>[] storeCls = new Class[classes.size()];
		boolean[] storeSeen = new boolean[seen.size()];
		int[] storeEncounters = new int[encounters.size()];
		int[] storeLootSeen = new int[lootSeenList.size()];

		for (int i = 0; i < storeCls.length; i++){
			storeCls[i] = classes.get(i);
			storeSeen[i] = seen.get(i);
			storeEncounters[i] = encounters.get(i);
			storeLootSeen[i] = lootSeenList.get(i);
		}

		bundle.put( BESTIARY_CLASSES, storeCls );
		bundle.put( BESTIARY_SEEN, storeSeen );
		bundle.put( BESTIARY_ENCOUNTERS, storeEncounters );
		bundle.put( BESTIARY_LOOT_SEEN, storeLootSeen );

	}

	public static void restore( Bundle bundle ){

		if (bundle.contains(BESTIARY_CLASSES)
				&& bundle.contains(BESTIARY_SEEN)
				&& bundle.contains(BESTIARY_ENCOUNTERS)){
			Class<?>[] classes = bundle.getClassArray(BESTIARY_CLASSES);
			boolean[] seen = bundle.getBooleanArray(BESTIARY_SEEN);
			int[] encounters = bundle.getIntArray(BESTIARY_ENCOUNTERS);
			int[] lootSeenArr = bundle.contains(BESTIARY_LOOT_SEEN)
					? bundle.getIntArray(BESTIARY_LOOT_SEEN) : null;

			for (int i = 0; i < classes.length; i++){
				for (Bestiary cat : values()){
					if (cat.seen.containsKey(classes[i])){
						cat.seen.put(classes[i], seen[i]);
						cat.encounterCount.put(classes[i], encounters[i]);
						if (lootSeenArr != null && i < lootSeenArr.length) {
							cat.lootSeen.put(classes[i], lootSeenArr[i]);
						}
					}
				}
			}
		}

	}

}

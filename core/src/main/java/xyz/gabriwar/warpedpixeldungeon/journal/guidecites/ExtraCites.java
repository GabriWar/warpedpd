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

package xyz.gabriwar.warpedpixeldungeon.journal.guidecites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AuroraBless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BloodMoonBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.CountDown;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Crouching;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Doom;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.RainbowBlessing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoakedShoes;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SolarEclipseBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpaceTimePowers;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.TimeStasis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Albino;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoPiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Assassin;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BanditKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BlueWraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CausticSlime;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Collector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrabKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM300;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eye;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FetidRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollArcher;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollTrickster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GoldThief;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GoldenMimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreatCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreyOni;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Guard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gullin;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HermitCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LivingPlant;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MagicEye;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MossySkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OrbOfZotMob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Otiluke;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Piranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RatBoss;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RedWraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RivalStatue;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShadowYog;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shell;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShellCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkeletonHand1;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkeletonHand2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SkeletonKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SokobanSentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpectralNecromancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpectralRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Thief;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ThiefKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.TormentedSpirit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Wraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogDzewa;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ZotPhase;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.BlackMarketDealer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Ghost;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Imp;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.ImpShopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OtilukeNPC;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RatKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RatKingDen;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokoban;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanBlack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanCorner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanStop;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanSwitch;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer1;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Wandmaker;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Mercenary;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.BlueDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Fairy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.GreenDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.RedDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Scorpion;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Spider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.Velocirooster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.VioletDragon;
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantArmor;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantRing;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWand;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.Amulet;
import xyz.gabriwar.warpedpixeldungeon.items.AncientCoin;
import xyz.gabriwar.warpedpixeldungeon.items.Ankh;
import xyz.gabriwar.warpedpixeldungeon.items.Bone;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfLife;
import xyz.gabriwar.warpedpixeldungeon.items.BrokenSeal;
import xyz.gabriwar.warpedpixeldungeon.items.BulletBelt;
import xyz.gabriwar.warpedpixeldungeon.items.ConchShell;
import xyz.gabriwar.warpedpixeldungeon.items.Dewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.Egg;
import xyz.gabriwar.warpedpixeldungeon.items.EnergyCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.KingsCrown;
import xyz.gabriwar.warpedpixeldungeon.items.Mushroom;
import xyz.gabriwar.warpedpixeldungeon.items.OrbOfZot;
import xyz.gabriwar.warpedpixeldungeon.items.OtilukesJournal;
import xyz.gabriwar.warpedpixeldungeon.items.Palantir;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarah;
import xyz.gabriwar.warpedpixeldungeon.items.SeekingClusterBombItem;
import xyz.gabriwar.warpedpixeldungeon.items.SewersKey;
import xyz.gabriwar.warpedpixeldungeon.items.StarFragment;
import xyz.gabriwar.warpedpixeldungeon.items.Stylus;
import xyz.gabriwar.warpedpixeldungeon.items.TenguKey;
import xyz.gabriwar.warpedpixeldungeon.items.TengusMask;
import xyz.gabriwar.warpedpixeldungeon.items.Towel;
import xyz.gabriwar.warpedpixeldungeon.items.TownReturnBeacon;
import xyz.gabriwar.warpedpixeldungeon.items.UpgradeBlobYellow;
import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.items.Whistle;
import xyz.gabriwar.warpedpixeldungeon.items.YellowDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.MailArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.CloakOfShadows;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.HolyTome;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.LloydsBeacon;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.journal.DescentPage;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.DragonCave;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.Sokoban1;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.Town;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.Vault;
import xyz.gabriwar.warpedpixeldungeon.items.keys.GoldenSkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.misc.AutoPotion;
import xyz.gabriwar.warpedpixeldungeon.items.misc.Spectacles;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.NornStone;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLevitation;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLiquidFlame;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMight;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfUltraviolett;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfEarthenArmor;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DarkGold;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.TypeShifter;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfWealth;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfIdentify;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicalInfusion;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.spells.SeasonChange;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfAugmentation;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfEnchantment;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfIntuition;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.RatSkull;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrinketCatalyst;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Chainsaw;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Gloves;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Rapier;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spork;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.AresSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ChangeSheepTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FleecingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SummoningTrap;
import xyz.gabriwar.warpedpixeldungeon.plants.Blackholeflower;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Clockcypress;
import xyz.gabriwar.warpedpixeldungeon.plants.Earthroot;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Flytrap;
import xyz.gabriwar.warpedpixeldungeon.plants.Hypnohemp;
import xyz.gabriwar.warpedpixeldungeon.plants.Icecap;
import xyz.gabriwar.warpedpixeldungeon.plants.Phaseshift;
import xyz.gabriwar.warpedpixeldungeon.plants.Rotberry;
import xyz.gabriwar.warpedpixeldungeon.plants.Starflower;
import xyz.gabriwar.warpedpixeldungeon.plants.Suncarnivore;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;

//Cites for the side chapters: the boss dens, Sokoban, Zot's level and the topics.
public final class ExtraCites {

	private ExtraCites(){}

	public static void register( CiteBook book ){
		dens( book );
		sokoban( book );
		zot( book );
		topics( book );
	}

	//an enchantment or a glyph has no art of its own: the weapon or armor in its glow, as the catalog draws it
	private static ItemSprite enchanted( Weapon.Enchantment e ){
		return new ItemSprite( ItemSpriteSheet.WORN_SHORTSWORD, e.glowing() );
	}

	// ------------------------------------------------------------------ the boss dens

	private static void dens( CiteBook book ){
		book.page( "dens_overview" )
				.item( SanChikarah.class, "SanChikarah" )
				.item( Bone.class, "Inscribed Bone" )
				.item( ConchShell.class, "Conch Shell" )
				.item( AncientCoin.class, "Ancient Coin" )
				.item( TenguKey.class, "Tengu Ancient Key" )
				.mob( GnollArcher.class, "gnoll archer" )
				.mob( Tengu.class, "Tengu" )
				.mob( MossySkeleton.class, "moss covered skeleton" )
				.mob( AlbinoPiranha.class, "albino piranha" )
				.mob( GoldThief.class, "gold thief", "gold thieves" )
				.mob( Blacksmith.class, "blacksmith" )
				.item( AdamantRing.class, "adamantite ring" )
				.item( AdamantWeapon.class, "adamantite weapon" )
				.item( AdamantArmor.class, "adamantite armor" )
				//not "gold": that would set it under the gold thieves, a paragraph up
				.item( Gold.class, "gold coins", "4,000 gold" )
				.item( OrbOfZot.class, "Orb of Zot" )
				.mob( Blacksmith2.class, "town smith" )
				.mob( SkeletonKing.class, "Skeleton King" )
				.mob( ThiefKing.class, "Thief King" )
				.mob( CrabKing.class, "Crab King" )
				.item( LloydsBeacon.class, "Lloyd's Beacon" );

		book.page( "den_infest" )
				.item( SanChikarah.class, "SanChikarah" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.PEDESTAL, "pedestal" )
				.mob( ShadowYog.class, "Shadow Yog" ).primary()
				.mob( GreyOni.class, "grey oni" )
				.mob( SpectralRat.class, "spectral rat" )
				.buff( Weakness.class, "weakness", "weaken" )
				.trap( SummoningTrap.class, "summoning trap" )
				.item( LloydsBeacon.class, "Lloyd's Beacon" );

		book.page( "den_tengu" )
				.water( Assets.Environment.WATER_CAVES, "water" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.GRASS, "grass" )
				.mob( Tengu.class, "Tengu" ).primary()
				.sprite( ItemSpriteSheet.TENGU_BOMB, "bomb" )
				.buff( Roots.class, "roots" )
				.buff( Blindness.class, "blindness" )
				.buff( Terror.class, "terror" )
				.buff( Poison.class, "poison" )
				.effect( GuideEffect.TOXIC_GAS, "toxic gas" )
				.item( Gold.class, "gold" )
				.item( AdamantRing.class, "adamantite ring" )
				.item( TenguKey.class, "Tengu Ancient Key", "key" )
				.item( LloydsBeacon.class, "Lloyd's Beacon" )
				.mob( RatKingDen.class, "Rat King" )
				.item( Starflower.Seed.class, "seed" )
				.item( Egg.class, "egg" )
				.item( Food.class, "food" )
				.item( SeekingClusterBombItem.class, "cluster bomb" )
				.item( ActiveMrDestructo.class, "Mr Destructo" )
				.mob( Mimic.class, "mimic" );

		book.page( "den_skeleton" )
				.terrain( Assets.Environment.TILES_HALLS, DungeonTileSheet.FLAT_STATUE, "statue" )
				.terrain( Assets.Environment.TILES_HALLS, DungeonTileSheet.SHRUB_TILE, "shrub" )
				.terrain( Assets.Environment.TILES_HALLS, DungeonTileSheet.FLAT_HIGH_GRASS, "tall grass" )
				.water( Assets.Environment.WATER_PRISON, "water" )
				.item( PotionOfLiquidFlame.class, "potion of liquid flame" )
				.effect( GuideEffect.FIRE, "fire" )
				.mob( SkeletonKing.class, "Skeleton King" ).primary()
				.mob( SkeletonHand1.class, "burning hand", "skeleton hand" )
				//the second of the "two giant skeleton hands"
				.mob( SkeletonHand2.class, "rooting hand", "hand" )
				.item( Gold.class, "gold" )
				.item( AdamantWeapon.class, "adamantite weapon" )
				.item( Bone.class, "Inscribed Bone" );

		book.page( "den_crab" )
				.item( ConchShell.class, "Conch Shell" )
				.water( Assets.Environment.WATER_PRISON, "water" )
				.mob( Shell.class, "lightning shell" )
				.item( PotionOfLevitation.class, "potion of levitation" )
				.mob( ShellCrab.class, "shell crab" )
				.mob( CrabKing.class, "Crab King" ).primary()
				.item( Gold.class, "gold" )
				.item( AdamantArmor.class, "adamantite armor" )
				.item( GoldenSkeletonKey.class, "golden skeleton key" );

		book.page( "den_thief" )
				.item( AncientCoin.class, "Ancient Coin", "coin" )
				.terrain( Assets.Environment.TILES_CITY, DungeonTileSheet.CHASM, "chasm" )
				.mob( ThiefKing.class, "Thief King" ).primary()
				.mob( BanditKing.class, "bandit king" )
				.effect( GuideEffect.FIRE, "fire" )
				.buff( Poison.class, "poison" )
				.item( Gold.class, "gold" )
				.item( AdamantRing.class, "adamantite ring" )
				.sprite( ItemSpriteSheet.SCROLL_HOLDER, "scroll" );

		book.page( "den_thiefcatch" )
				.terrain( Assets.Environment.TILES_CITY, DungeonTileSheet.CHASM, "chasm" )
				.item( LloydsBeacon.class, "Lloyd's Beacon" )
				.item( SanChikarah.class, "SanChikarah" )
				.water( Assets.Environment.WATER_CAVES, "water" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.GRASS, "moss" )
				.mob( Tengu.class, "Tengu" )
				.mob( BanditKing.class, "bandit king" );

		book.page( "boss_shadowyog" )
				.mob( ShadowYog.class, "Shadow Yog" )
				.trap( SummoningTrap.class, "summoning trap" )
				.mob( SpectralRat.class, "spectral rat" )
				.buff( Terror.class, "terror" )
				.buff( Amok.class, "amok" )
				.buff( Charm.class, "charm" )
				.buff( Sleep.class, "sleep" )
				.effect( GuideEffect.FIRE, "fire" )
				.effect( GuideEffect.TOXIC_GAS, "toxic gas" )
				.buff( Vertigo.class, "vertigo" )
				.custom( () -> enchanted( new Grim() ), "Grim" )
				.buff( Roots.class, "roots" )
				.buff( Cripple.class, "cripple" )
				.buff( Slow.class, "slow" )
				.buff( Chill.class, "cold" )
				.buff( Doom.class, "Doom" )
				.item( OrbOfZot.class, "Orb of Zot" )
				//the Orb, thrown, stands as a turret
				.mob( OrbOfZotMob.class, "turret" )
				.item( OtilukesJournal.class, "Otiluke's Journal" )
				.mob( GreyOni.class, "oni" )
				.item( SanChikarah.class, "SanChikarah" );

		book.page( "boss_skeletonking" )
				.mob( SkeletonKing.class, "Skeleton King" )
				.buff( Weakness.class, "weakness", "weaken" )
				.mob( SkeletonHand1.class, "burning hand" )
				.buff( Burning.class, "on fire" )
				.mob( SkeletonHand2.class, "rooting hand" )
				.buff( Roots.class, "roots" )
				.terrain( Assets.Environment.TILES_HALLS, DungeonTileSheet.SHRUB_TILE, "brush" )
				.item( PotionOfLiquidFlame.class, "liquid flame" )
				.water( Assets.Environment.WATER_PRISON, "water" );

		book.page( "boss_crabking" )
				.mob( CrabKing.class, "Crab King" )
				.mob( Shell.class, "lightning shell" )
				.mob( ShellCrab.class, "shell crab" )
				.buff( Poison.class, "poison" )
				.effect( GuideEffect.TOXIC_GAS, "toxic gas" )
				.custom( () -> enchanted( new Grim() ), "Grim" );

		book.page( "boss_thiefking" )
				.mob( ThiefKing.class, "Thief King" )
				.terrain( Assets.Environment.TILES_CITY, DungeonTileSheet.CHASM, "chasm" )
				.mob( BanditKing.class, "bandit king" )
				.buff( CountDown.class, "Countdown" )
				.buff( Roots.class, "roots" )
				.item( Spork.class, "spork" );
	}

	// ------------------------------------------------------------------ Otiluke's folded rooms

	private static void sokoban( CiteBook book ){
		book.page( "sokoban_overview" )
				.item( OtilukesJournal.class, "Otiluke's Journal" )
				.mob( Shopkeeper.class, "shop" )
				.mob( DM300.class, "DM-300" )
				.mob( DwarfKing.class, "Dwarf King" )
				.item( Sokoban1.class, "page" )
				.mob( Goo.class, "Goo" )
				.mob( Tengu.class, "Tengu" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.FLAT_BOOKSHELF, "bookshelf", "bookshelves" )
				.item( OrbOfZot.class, "Orb of Zot" )
				.item( Spectacles.class, "Spectacles" )
				.mob( SheepSokoban.class, "sheep" )
				.trap( FleecingTrap.class, "Fleecing trap" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.WOOL_RUG_TILE, "rug" )
				.mob( SokobanSentinel.class, "animated statue" );

		book.page( "sok_practice" )
				.mob( SheepSokoban.class, "plain sheep" ).primary()
				.mob( SheepSokobanCorner.class, "corner sheep" )
				.mob( SheepSokobanSwitch.class, "switch sheep" )
				.mob( SheepSokobanBlack.class, "black sheep" )
				.trap( FleecingTrap.class, "fleecing trap" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.SOKOBAN_ITEM_REVEAL_TILE, "prize tile" )
				.item( IronKey.class, "iron key" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.FLAT_DOOR_LOCKED, "locked door" )
				.item( Towel.class, "Towel" )
				.buff( Bleeding.class, "bleeding" )
				.buff( Ooze.class, "caustic ooze" )
				.trap( ChangeSheepTrap.class, "change trap" )
				//what a corner sheep becomes on a change trap
				.mob( SheepSokobanStop.class, "stuck sheep", "never moves" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.EMPTY_WELL, "well" )
				//"switch" alone would also find the switch sheep, three paragraphs up
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.SOKOBAN_PORT_SWITCH_TILE, "well switch", "its switch" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.item( Gold.class, "gold" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" );

		book.page( "sok_castle" )
				.mob( SheepSokobanCorner.class, "corner sheep" )
				.trap( FleecingTrap.class, "fleecing trap" )
				.trap( ChangeSheepTrap.class, "change trap" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.SOKOBAN_ITEM_REVEAL_TILE, "prize tile" )
				.item( IronKey.class, "iron key" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.FLAT_DOOR_LOCKED, "locked door" )
				.item( Phaseshift.Seed.class, "Phaseshift seed" )
				.item( Starflower.Seed.class, "Starflower seed" )
				.item( ScrollOfMagicalInfusion.class, "Scroll of Magical Infusion" )
				.item( ScrollOfRegrowth.class, "Scroll of Regrowth" )
				.item( Spectacles.class, "Spectacles" ).primary()
				.item( OtilukesJournal.class, "journal" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.EMPTY_WELL, "well" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.SOKOBAN_PORT_SWITCH_TILE, "switch" )
				.mob( SokobanSentinel.class, "animated statue" )
				.buff( Poison.class, "poison" )
				.custom( () -> enchanted( new Vampiric() ), "vampiric weapon" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.item( Gold.class, "gold" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" )
				.item( PotionOfLiquidFlame.class, "Potion of Liquid Flame" );

		book.page( "sok_portals" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.EMPTY_WELL, "well" )
				.mob( SheepSokoban.class, "sheep" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.SOKOBAN_PORT_SWITCH_TILE, "switch" )
				.item( IronKey.class, "iron key" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.FLAT_DOOR_LOCKED, "locked door" )
				.item( Starflower.Seed.class, "Starflower seed" )
				.item( Phaseshift.Seed.class, "Phaseshift seed" )
				.item( ScrollOfMagicalInfusion.class, "Scroll of Magical Infusion" )
				.item( ScrollOfRegrowth.class, "Scroll of Regrowth" )
				.item( AutoPotion.class, "auto potion charm" ).primary()
				.item( PotionOfHealing.class, "Potion of Healing" )
				.mob( SokobanSentinel.class, "animated statue" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.item( Gold.class, "gold" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" );

		book.page( "sok_puzzles" )
				.mob( SheepSokobanSwitch.class, "switch sheep" )
				.trap( ChangeSheepTrap.class, "change trap" )
				.mob( SheepSokoban.class, "plain sheep" )
				.mob( SheepSokobanCorner.class, "corner sheep" )
				.item( IronKey.class, "iron key" )
				.mob( SokobanSentinel.class, "animated statue" )
				.item( ScrollOfMagicalInfusion.class, "Scroll of Magical Infusion" )
				.item( Phaseshift.Seed.class, "Phaseshift seed" )
				.item( Flytrap.Seed.class, "Flytrap seed" )
				.item( Starflower.Seed.class, "Starflower seed" )
				.item( Egg.class, "Egg" )
				.item( Whistle.class, "Whistle" ).primary()
				//"switch" alone would also find the switch sheep of the first paragraph
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.SOKOBAN_PORT_SWITCH_TILE, "well switch", "switch in the southwest" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.EMPTY_WELL, "well" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.item( PotionOfLiquidFlame.class, "Potion of Liquid Flame" );

		book.page( "sok_vault" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.FLAT_BOOKSHELF, "bookshelf" )
				.buff( MindVision.class, "mind vision" )
				.item( Vault.class, "Vault page" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.GRASS, "grass" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.SHRUB_TILE, "shrub" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.item( Gold.class, "gold" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" )
				.item( RingOfWealth.class, "Ring of Wealth" ).primary()
				.item( ScrollOfMagicMapping.class, "Magic mapping" )
				.item( DragonCave.class, "Dragon Cave page" );
	}

	// ------------------------------------------------------------------ Zot's prison

	private static void zot( CiteBook book ){
		book.page( "zot_overview" )
				.mob( Zot.class, "Zot" )
				.item( Palantir.class, "Palantir" )
				.mob( Otiluke.class, "stone golem" )
				.item( AresSword.class, "relic weapon" )
				.item( NornStone.class, "Norn Stone" )
				.mob( Gullin.class, "Gullin" );

		book.page( "zot_level" )
				.terrain( Assets.Environment.TILES_HALLS, DungeonTileSheet.PEDESTAL, "pedestal" )
				.item( Palantir.class, "Palantir" )
				.trap( SummoningTrap.class, "summoning trap" )
				.mob( Eye.class, "evil eye" )
				.mob( Zot.class, "Zot" ).primary()
				.mob( OtilukeNPC.class, "Otiluke" )
				.item( TownReturnBeacon.class, "Town Return Beacon" );

		book.page( "boss_zot" )
				.mob( Zot.class, "Zot" )
				.item( AresSword.class, "relic weapon" )
				.item( JupitersWraith.class, "Jupiter's Wraith" )
				.item( AutoPotion.class, "auto potion charm" )
				.mob( ZotPhase.class, "Zot phase" )
				.hero( HeroClass.HUNTRESS, "huntress" )
				.water( Assets.Environment.WATER_HALLS, "water" )
				.sprite( ItemSpriteSheet.SCROLL_HOLDER, "scroll" )
				.buff( Paralysis.class, "paralysis", "paralyse", "paralysed" )
				.mob( MagicEye.class, "magic eye" )
				.effect( GuideEffect.FIRE, "fire" )
				.effect( GuideEffect.TOXIC_GAS, "gas" )
				.buff( Sleep.class, "sleep" )
				.buff( Terror.class, "terror" )
				.buff( Amok.class, "amok" )
				.buff( Charm.class, "charm" )
				.buff( Vertigo.class, "vertigo" )
				.custom( () -> enchanted( new Vampiric() ), "vampiric weapon" )
				.buff( Poison.class, "poison" );
	}

	// ------------------------------------------------------------------ the topics

	private static void topics( CiteBook book ){
		book.page( "topic_meta" )
				.item( DescentPage.class, "page" )
				.item( Amulet.class, "Amulet of Yendor" )
				.mob( Shopkeeper.class, "shop" )
				.mob( ImpShopkeeper.class, "imp" )
				.item( OtilukesJournal.class, "Otiluke's Journal" )
				//the floor feelings, by the icons the game names them with
				.icon( Icons.DEPTH_WATER, "flooded" )
				.icon( Icons.DEPTH_GRASS, "overgrown" )
				.icon( Icons.DEPTH_DARK, "dark" )
				.icon( Icons.DEPTH_LARGE, "large" )
				.icon( Icons.DEPTH_TRAPS, "trapped" )
				.icon( Icons.DEPTH_SECRETS, "secret" )
				.icon( Icons.DEPTH_CHASM, "chasm" )
				.item( IronKey.class, "iron key" )
				.buff( Bleeding.class, "bleeding" );

		book.page( "topic_heroes" )
				.hero( HeroClass.WARRIOR, "warrior" )
				.hero( HeroClass.MAGE, "mage" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" )
				.hero( HeroClass.ROGUE, "rogue" )
				.hero( HeroClass.HUNTRESS, "huntress" )
				.hero( HeroClass.DUELIST, "duelist" )
				.hero( HeroClass.CLERIC, "cleric" )
				.item( BrokenSeal.class, "Broken Seal" )
				.item( MagesStaff.class, "staff" )
				.item( CloakOfShadows.class, "Cloak of Shadows" )
				.item( Gloves.class, "gloves" )
				.item( SpiritBow.class, "spirit bow" )
				.item( Rapier.class, "rapier" )
				.item( HolyTome.class, "holy tome" )
				.mob( Tengu.class, "Tengu" )
				.item( TengusMask.class, "mask" )
				.mob( DwarfKing.class, "Dwarf King" )
				.item( KingsCrown.class, "crown" )
				.mob( RatKing.class, "Rat King" )
				.item( RingOfMagic.class, "Ring of Magic" )
				.buff( Crouching.class, "crouch" )
				.buff( Sleepiness.class, BuffIndicator.SLEEPINESS, "drowsy" );

		book.page( "topic_weather" )
				.icon( Icons.CALENDAR, "calendar" )
				.effect( GuideEffect.BLIZZARD, "blizzard" )
				.effect( GuideEffect.STORM_CLOUD, "storm cloud" )
				.buff( Hypothermia.class, "hypothermia" )
				.buff( Heatstroke.class, "heatstroke" )
				.item( ClothArmor.class, "cloth armor" )
				.item( LeatherArmor.class, "leather" )
				.item( MailArmor.class, "mail" )
				.item( PlateArmor.class, "plate" )
				.custom( () -> new ItemSprite( ItemSpriteSheet.ARMOR_CLOTH, new Brimstone().glowing() ), "Brimstone" )
				.buff( Drenched.class, "soaked" )
				.buff( SoakedShoes.class, "soaked boots", "boots" )
				.buff( BloodMoonBuff.class, "blood moon" )
				.buff( SolarEclipseBuff.class, "solar eclipse" )
				.buff( AuroraBless.class, "aurora" )
				.buff( RainbowBlessing.class, "rainbow" )
				.item( SeasonChange.class, "Season Change" )
				.item( ScrollOfMagicMapping.class, "Scroll of Magic Mapping" )
				.item( PotionOfEarthenArmor.class, "Potion of Earthen Armor" )
				.item( PotionOfUltraviolett.class, "Potion of Ultraviolet" )
				.plant( Suncarnivore.class, "suncarnivore" );

		book.page( "topic_quests" )
				.mob( Ghost.class, "sad ghost" )
				.mob( GnollArcher.class, "gnoll archer" )
				.mob( FetidRat.class, "fetid rat" )
				.mob( GnollTrickster.class, "gnoll trickster" )
				.mob( GreatCrab.class, "great crab" )
				.mob( Wandmaker.class, "wandmaker" )
				.item( AdamantWand.class, "adamantite wand" )
				.mob( Blacksmith.class, "blacksmith" )
				.item( DarkGold.class, "dark gold" )
				.mob( Imp.class, "Ambitious Imp" )
				.mob( RatKing.class, "Rat King" )
				.item( Mushroom.class, "Toadstool mushroom" )
				.mob( Tinkerer1.class, "tinkerer" )
				.item( Waterskin.class, "waterskin" )
				.item( Egg.class, "egg" )
				.mob( BlueDragon.class, "blue dragon" )
				.mob( VioletDragon.class, "violet dragon", "violet one" )
				.mob( GreenDragon.class, "green dragon", "green one" )
				.mob( RedDragon.class, "red dragon", "red one" )
				.mob( Velocirooster.class, "velocirooster" )
				.mob( Fairy.class, "fairy" )
				.mob( Spider.class, "spider" )
				.mob( Scorpion.class, "scorpion" )
				.item( Whistle.class, "Whistle" );

		book.page( "topic_plants" )
				.plant( Firebloom.class, "Firebloom" )
				.plant( Sungrass.class, "Sungrass" )
				//a wild plant torn loose ("wild plant" would set it under the first paragraph, on spreading)
				.mob( LivingPlant.class, "living plant", "tears loose" )
				.plant( BlandfruitBush.class, "Blandfruit bush" )
				.plant( Rotberry.class, "Rotberry" )
				.sprite( ItemSpriteSheet.SEED_HOLDER, "seed" )
				.item( PotionOfStrength.class, "Potion of Strength" )
				.plant( Icecap.class, "Icecap" )
				.plant( Blackholeflower.class, "Blackholeflower" )
				.buff( SpaceTimePowers.class, "Space Time Powers" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_HIGH_GRASS, "tall grass" )
				.item( Dewdrop.class, "dewdrop" )
				.item( YellowDewdrop.class, "colored dewdrop" )
				.item( Waterskin.class, "waterskin" )
				.effect( GuideEffect.FIRE, "fire" )
				.plant( Clockcypress.class, "Clockcypress" )
				.buff( TimeStasis.class, "stasis" )
				.plant( Hypnohemp.class, "Hypnohemp" )
				.plant( Earthroot.class, "Earthroot" )
				.plant( Starflower.class, "Starflower" );

		book.page( "topic_skills" )
				.icon( Icons.TALENT, "talent" )
				.mob( Mercenary.class, "mercenary" )
				.hero( HeroClass.WARRIOR, "warrior" )
				.hero( HeroClass.MAGE, "mage" )
				.hero( HeroClass.ROGUE, "rogue" )
				.hero( HeroClass.HUNTRESS, "huntress" )
				.hero( HeroClass.DUELIST, "duelist" )
				.hero( HeroClass.CLERIC, "cleric" )
				.item( KingsCrown.class, "Dwarf King's crown" );

		book.page( "topic_rarity" )
				//shown as the colour of a piece's name and the letter in its corner, and a line of its
				//rarity tab: no picture of their own
				.plain( "rarity", "type", "vestige", "trait" )
				.sprite( ItemSpriteSheet.WEAPON_HOLDER, "weapon" )
				.sprite( ItemSpriteSheet.ARMOR_HOLDER, "armor" )
				.sprite( ItemSpriteSheet.MISSILE_HOLDER, "thrown weapon" )
				.sprite( ItemSpriteSheet.WAND_HOLDER, "wand" )
				.sprite( ItemSpriteSheet.RING_HOLDER, "ring" )
				.sprite( ItemSpriteSheet.TRINKET_HOLDER, "trinket" )
				.sprite( ItemSpriteSheet.ARTIFACT_HOLDER, "artifact" )
				.item( TypeShifter.class, "type shifter" )
				.item( MasterworkCore.class, "masterwork core", "Masterwork" )
				.mob( Blacksmith.class, "blacksmith" )
				.mob( Blacksmith2.class, "town smith" )
				.item( StarFragment.class, "star fragment" )
				.icon( Icons.BREAKERS_BENCH, "breaker's bench" );

		book.page( "topic_loot" )
				.item( PotionOfStrength.class, "Potion of Strength" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" )
				.item( Stylus.class, "arcane stylus", "stylus" )
				.item( BulletBelt.class, "bullet belt" )
				.item( Food.class, "meal" )
				.icon( Icons.ALCHEMY, "laboratory" )
				.item( EnergyCrystal.class, "energy crystal" )
				.item( StoneOfIntuition.class, "Stone of Intuition" )
				.item( TrinketCatalyst.class, "trinket catalyst" )
				.item( StoneOfEnchantment.class, "Stone of Enchantment" )
				.mob( Mimic.class, "mimic" )
				.mob( GoldenMimic.class, "golden mimic", "golden ones" )
				.item( Ankh.class, "ankh" )
				.item( StoneOfAugmentation.class, "Stone of Augmentation" )
				.item( ScrollOfIdentify.class, "Scroll of Identify" )
				.item( Chainsaw.class, "chainsaw" )
				.item( ActiveMrDestructo.class, "Mr Destructo" )
				.item( Mushroom.class, "toadstool" )
				.item( MasterworkCore.class, "masterwork core" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.WELL, "magic well" )
				.item( PotionOfMight.class, "Potion of Might" )
				.plant( Flytrap.class, "Flytrap" )
				.item( UpgradeBlobYellow.class, "upgrade blob" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_BOOKSHELF, "bookshelf", "bookshelves" );

		book.page( "topic_rooms" )
				.icon( Icons.RIVAL_GALLERY, "Rival's gallery" )
				.item( IronKey.class, "iron key" )
				.mob( RivalStatue.class, "statue" )
				.icon( Icons.TEMPERING_FORGE, "Tempering forge" )
				.icon( Icons.BREAKERS_BENCH, "Breaker's bench" )
				.item( MasterworkCore.class, "masterwork core" )
				.icon( Icons.PEDIGREE_HALL, "Pedigree hall" )
				.icon( Icons.FROZEN_CACHE, "Frozen cache" )
				.icon( Icons.BELLOWS, "Bellows" )
				.icon( Icons.ELEMENTAL_LOCK, "Elemental lock" )
				.icon( Icons.HOTHOUSE, "Hothouse" )
				.icon( Icons.INCUBATOR_NEST, "Incubator nest" )
				.item( Egg.class, "egg" )
				.icon( Icons.COUNTERWEIGHT_VAULT, "Counterweight vault" )
				.item( Gold.class, "gold" )
				.icon( Icons.THERMAL_SPRING, "thermal spring" )
				.icon( Icons.WAYFARERS_CAMP, "wayfarer's camp" )
				.icon( Icons.SQUIRRELS_HOARD, "squirrel's hoard" )
				.icon( Icons.BLACK_MARKET, "Undercroft" )
				.mob( BlackMarketDealer.class, "dealer" )
				.item( TypeShifter.class, "type shifter" )
				.item( GoldenSkeletonKey.class, "golden skeleton key" )
				.mob( Collector.class, "collector" );

		book.page( "topic_bestiary" )
				.mob( RatBoss.class, "rat boss" )
				.mob( Assassin.class, "shinobi" )
				.mob( Albino.class, "albino rat" )
				.buff( Bleeding.class, "bleed" )
				.mob( CausticSlime.class, "slime" )
				.buff( Ooze.class, "caustic ooze" )
				.mob( HermitCrab.class, "hermit crab" )
				.item( RatSkull.class, "Rat Skull" )
				.mob( Skeleton.class, "skeleton" )
				.mob( Thief.class, "thief", "thieves" )
				.mob( Guard.class, "guard" )
				.mob( Wraith.class, "wraith" )
				.mob( SpectralNecromancer.class, "spectral necromancer" )
				.mob( RedWraith.class, "red wraith" )
				.mob( BlueWraith.class, "blue wraith" )
				.mob( SpectralRat.class, "spectral rat" )
				.mob( TormentedSpirit.class, "tormented spirit" )
				.item( ScrollOfRemoveCurse.class, "Scroll of Remove Curse" )
				.mob( Piranha.class, "piranha" );

		book.page( "topic_endings" )
				.item( Amulet.class, "Amulet of Yendor" )
				.mob( YogDzewa.class, "Yog-Dzewa" )
				.item( AresSword.class, "relic weapon" )
				.item( NornStone.class, "Norn Stone" )
				.mob( Otiluke.class, "stone golem" )
				.item( Palantir.class, "Palantir" )
				.mob( Zot.class, "Zot" )
				.mob( OtilukeNPC.class, "Otiluke" )
				.item( SewersKey.class, "ancient key" )
				//one of the three books
				.item( BookOfLife.class, "book" )
				.item( SanChikarah.class, "SanChikarah" )
				.item( OrbOfZot.class, "Orb of Zot" )
				.item( Town.class, "journal page" );
	}
}

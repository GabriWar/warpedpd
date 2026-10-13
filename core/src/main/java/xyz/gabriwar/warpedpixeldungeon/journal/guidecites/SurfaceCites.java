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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Breathless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Comfy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Levitation;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Light;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Luck;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Starving;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WellFed;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Windswept;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoPiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Brute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ClayGolem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ColdSpirit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalGuardian;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalWisp;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfLich;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eagle;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Elemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FossilSkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FungalSentry;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FungalSpinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Golem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreyOni;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gullin;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceDemon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.KoboldIcemancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Kupua;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LitTower;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LostSoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MineSentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Minotaur;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Otiluke;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldDragon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Raider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RiftElemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RuinSkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shaman;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShrinePiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Slime;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpectralRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Spinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Swarm;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Wraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogDzewa;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Bedroll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaravanTrain;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CaveMiner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.DwarvenForge;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.GnollVillager;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Hermit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.LakeShrine;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OtilukeNPC;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummitCairn;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer1;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer5;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TownGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Traveller;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Villager;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Bard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Bishop;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Employee;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.FortuneTellerFolk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.InnKeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.InnServant;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Librarian;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Mercenary;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownGuardFolk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Townsfolk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.ShadowDragon;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.TiePost;
import xyz.gabriwar.warpedpixeldungeon.homestead.farming.CompostBin;
import xyz.gabriwar.warpedpixeldungeon.homestead.rooms.ShrineBlessing;
import xyz.gabriwar.warpedpixeldungeon.homestead.rooms.WellRested;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.CookingPot;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Forge;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Hearth;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Kiln;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.MasonsBench;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Quern;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.TanningRack;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Workbench;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.AlarmBell;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.HomeRaider;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.SiegeGrayWolf;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.ThreatSlice;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.Watchtower;
import xyz.gabriwar.warpedpixeldungeon.homestead.torches.StandingTorch;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Hearthstone;
import xyz.gabriwar.warpedpixeldungeon.homestead.travel.Waystone;
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.Amulet;
import xyz.gabriwar.warpedpixeldungeon.items.BuildersTool;
import xyz.gabriwar.warpedpixeldungeon.items.Dewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.DiaryPage;
import xyz.gabriwar.warpedpixeldungeon.items.Egg;
import xyz.gabriwar.warpedpixeldungeon.items.Fertilizer;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.HallsKey;
import xyz.gabriwar.warpedpixeldungeon.items.Mushroom;
import xyz.gabriwar.warpedpixeldungeon.items.OtilukesJournal;
import xyz.gabriwar.warpedpixeldungeon.items.Palantir;
import xyz.gabriwar.warpedpixeldungeon.items.ShadowDragonEgg;
import xyz.gabriwar.warpedpixeldungeon.items.StarFragment;
import xyz.gabriwar.warpedpixeldungeon.items.StoneOre;
import xyz.gabriwar.warpedpixeldungeon.items.Torch;
import xyz.gabriwar.warpedpixeldungeon.items.TownReturnBeacon;
import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.items.Whistle;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.bags.BuildersSatchel;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.Bread;
import xyz.gabriwar.warpedpixeldungeon.items.food.EagleEgg;
import xyz.gabriwar.warpedpixeldungeon.items.food.Flour;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.food.MeatPie;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.PlantEgg;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.DragonCave;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.Vault;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Charcoal;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Clay;
import xyz.gabriwar.warpedpixeldungeon.items.materials.CutStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.FiredBrick;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GlassPane;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.materials.IronFittings;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Leather;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.PineLog;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Resin;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Sand;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.BlueNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.GreenNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.NornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.OrangeNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.PurpleNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.nornstone.YellowNornStone;
import xyz.gabriwar.warpedpixeldungeon.items.ore.CopperOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.GoldOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.IronOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.SilverOre;
import xyz.gabriwar.warpedpixeldungeon.items.ore.Skyiron;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfChilli;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfWarmth;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DarkGold;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.treeseeds.Acorn;
import xyz.gabriwar.warpedpixeldungeon.items.treeseeds.BushSeed;
import xyz.gabriwar.warpedpixeldungeon.items.treeseeds.PineCone;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MasonsHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shovel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WoodcuttersAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.AresSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.CromCruachAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.LokisFlail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.NeptunusTrident;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FleecingTrap;
import xyz.gabriwar.warpedpixeldungeon.plants.Apricobush;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Cornwheat;
import xyz.gabriwar.warpedpixeldungeon.plants.Eggbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Gobgrape;
import xyz.gabriwar.warpedpixeldungeon.plants.Tomatobush;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.HomesteadVisuals;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.BuildIcons;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;

//Cites for the world above: the surface, the peaks, the deep caves, homesteads, the town and its mines.
public final class SurfaceCites {

	private SurfaceCites(){}

	//the world's own sheets: the surface's tiles, and its dressing (trees, rocks, the ways between
	//the slices, the places of the mountains and the caves)
	private static final String LAND = Assets.Environment.TILES_OVERWORLD;
	private static final String DRESS = Assets.Environment.OVERWORLD_DRESS;
	//the kupua mines are cave tiles, their sealed last floor and the dragon cave prison tiles
	private static final String MINES = Assets.Environment.TILES_CAVES;
	private static final String SEALED = Assets.Environment.TILES_PRISON;

	public static void register( CiteBook book ){
		surface( book );
		peaks( book );
		deep( book );
		homestead( book );
		town( book );
		mines( book );
	}

	//a homestead piece as the build menu shows it (BuildPiece.preview)
	private static Cite.Picture piece( String id ){
		return () -> BuildCatalog.byId( id ).preview();
	}

	//a standing homestead terrain as its sheet draws it alone (a stump, a sapling, rubble)
	private static int feature( int terrain ){
		return HomesteadVisuals.previewFrame( terrain );
	}

	private static void surface( CiteBook book ){
		book.page( "surface_overview" )
				.terrain( LAND, DungeonTileSheet.GRASS, "meadow" )
				.terrain( DRESS, OverworldDress.SUMMER_PINES[0], "forest" )
				.water( Assets.Environment.WATER_SEWERS, "shallow water", "river" )
				.terrain( LAND, DungeonTileSheet.FROZEN_WATER, "ice", "freeze" )
				.terrain( LAND, DungeonTileSheet.SNOW_TILE, "snow line" )
				.buff( Drenched.class, "rain" )
				.buff( Windswept.class, "wind" )
				.effect( GuideEffect.FIRE, "fire" )
				.terrain( LAND, DungeonTileSheet.DIRT_PATH_TILE, "road" )
				.mob( Traveller.class, "traveler" )
				.mob( CaravanTrain.class, "caravan" )
				.mob( OverworldBandit.class, "bandit" )
				.mob( GrayWolf.class, "wolves" )
				.icon( Icons.COMPASS, "world map" )
				.terrain( DRESS, OverworldDress.LINK_STAIR_UP, "cliff stairs" )
				.terrain( DRESS, OverworldDress.LINK_CAVE_MOUTH, "cave mouth" );

		book.page( "surface_villages" )
				.mob( Villager.class, "villager", "families" )
				.mob( GnollVillager.class, "gnoll clan" )
				.mob( OverworldBandit.class, "bandit hold" )
				.mob( OverworldShopkeeper.class, "vendor" )
				.mob( OverworldGuard.class, "guard" )
				.terrain( LAND, DungeonTileSheet.WELL, "well" )
				.terrain( LAND, DungeonTileSheet.FLAT_DOOR, "house door" )
				.buff( Hypothermia.class, "hypothermia" )
				.buff( Heatstroke.class, "heatstroke" )
				.mob( TravellingMerchant.class, "traveling market" )
				.mob( Raider.class, "bandit raid" );

		book.page( "surface_wilds" )
				.icon( Icons.COMPASS, "world map" )
				.terrain( LAND, DungeonTileSheet.FLAT_WALL_DECO, "ruins" )
				.mob( RuinSkeleton.class, "skeleton" )
				.sprite( ItemSpriteSheet.CHEST, "abandoned camp", "chest" )
				.terrain( DRESS, OverworldDress.OAK_SUMMER_TOP, "great oak" )
				.sprite( ItemSpriteSheet.SEED_HOLDER, "seed" )
				.item( Dewdrop.class, "dewdrop" )
				.terrain( LAND, DungeonTileSheet.FLAT_STATUE, "standing stones" )
				.sprite( ItemSpriteSheet.SCROLL_HOLDER, "scroll" )
				.mob( OverworldDragon.class, "dragon lair", "dragon" )
				.item( Gold.class, "gold" )
				.icon( Icons.STAIRS_TRAPS, "barrows" )
				.item( Amulet.class, "Amulet of Yendor" );

		book.page( "surface_events" )
				.effect( GuideEffect.LIGHT, "falling star" )
				.item( StarFragment.class, "star fragment" )
				.mob( Blacksmith2.class, "town smith" )
				.item( MasterworkCore.class, "masterwork core" )
				.mob( TravellingMerchant.class, "traveling market", "stall" )
				.sprite( ItemSpriteSheet.RING_HOLDER, "ring" )
				.sprite( ItemSpriteSheet.WAND_HOLDER, "wand" )
				.sprite( ItemSpriteSheet.ARTIFACT_HOLDER, "artifact" )
				.sprite( ItemSpriteSheet.POTION_HOLDER, "exotic potion" )
				.sprite( ItemSpriteSheet.SCROLL_HOLDER, "exotic scroll" )
				.mob( Raider.class, "raid", "raider" )
				.item( Gold.class, "gold" )
				.sprite( ItemSpriteSheet.WEAPON_HOLDER, "weapon" )
				.sprite( ItemSpriteSheet.ARMOR_HOLDER, "armor" )
				.mob( GrayWolf.class, "wolf pack", "wolves" )
				.mob( Deer.class, "deer" )
				.item( MysteryMeat.class, "meat" );

		book.page( "surface_barrows" )
				.item( Amulet.class, "Amulet of Yendor" )
				.icon( Icons.STAIRS_TRAPS, "barrow" )
				.icon( Icons.COMPASS, "world map" )
				.item( Gold.class, "gold" )
				.sprite( ItemSpriteSheet.WEAPON_HOLDER, "weapon" )
				.sprite( ItemSpriteSheet.ARMOR_HOLDER, "armor" )
				.sprite( ItemSpriteSheet.WAND_HOLDER, "wand" )
				.sprite( ItemSpriteSheet.RING_HOLDER, "ring" )
				.sprite( ItemSpriteSheet.ARTIFACT_HOLDER, "artifact" )
				.item( MasterworkCore.class, "masterwork core" )
				.item( PotionOfStrength.class, "Potion of Strength" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" );

		book.page( "boss_dragon" )
				.mob( OverworldDragon.class, "dragon" )
				.buff( Poison.class, "poison" )
				.terrain( LAND, DungeonTileSheet.FLAT_WALL, "rock" )
				.item( Gold.class, "gold", "hoard" );
	}

	private static void peaks( CiteBook book ){
		book.page( "peaks_overview" )
				.terrain( DRESS, OverworldDress.LINK_STAIR_UP, "cliff stairs" )
				.terrain( LAND, DungeonTileSheet.SNOW_TILE, "snow" )
				.terrain( DRESS, OverworldDress.WINTER_PINES[0], "pine" )
				.water( Assets.Environment.WATER_SEWERS, "tarn" )
				.item( Pickaxe.class, "pickaxe" )
				.terrain( LAND, DungeonTileSheet.CHASM, "drop" )
				.effect( GuideEffect.FIRE, "fire" )
				.item( ClothArmor.class, "cloth armor" )
				.item( ElixirOfWarmth.class, "Elixir of Warmth" )
				.mob( Bunny.class, "hare" )
				.mob( Goat.class, "mountain goat" )
				.mob( GrayWolf.class, "grey wolf", "wolves" )
				.mob( Yeti.class, "yeti" )
				.mob( KoboldIcemancer.class, "kobold icemancer" )
				.buff( Slow.class, "slow" )
				.mob( ColdSpirit.class, "cold spirit" )
				.buff( Frost.class, "freeze" )
				.mob( IceDemon.class, "ice demon" )
				.mob( Elemental.FrostElemental.class, "frost elemental" )
				.item( CopperOre.class, "copper" )
				.item( IronOre.class, "iron" )
				.item( SilverOre.class, "silver" )
				.item( GoldOre.class, "gold" )
				.item( Skyiron.class, "skyiron" );

		book.page( "peaks_places" )
				.mob( Hermit.class, "hermit", "Hermit's hut" )
				.terrain( DRESS, OverworldDress.NEST, "eyrie", "nest" )
				.item( Gold.class, "gold" )
				.item( EagleEgg.class, "eagle egg" )
				.mob( Eagle.class, "eagle" )
				.mob( SummitCairn.class, "cairn" )
				.buff( Bless.class, "Bless" )
				.effect( GuideEffect.STEAM, "hot spring" )
				.buff( Chill.class, "chill" )
				.buff( Frost.class, "frost" )
				.buff( Drenched.class, "wet" )
				.terrain( DRESS, OverworldDress.CLIMBER_PACK, "frozen climber" )
				.item( ElixirOfWarmth.class, "Elixir of Warmth" )
				.item( PotionOfChilli.class, "Potion of Chilli" )
				.item( Food.class, "food" )
				.item( Pickaxe.class, "pickaxe" )
				.terrain( DRESS, OverworldDress.TOWER_STAIR, "watchtower", "stair" )
				.mob( Yeti.class, "yeti" )
				.mob( OverworldBandit.class, "bandit" )
				.custom( piece( "hearth_stone" ), "waystation", "hearth" )
				.mob( Bedroll.class, "bedroll" )
				.buff( Breathless.class, "Breathless" );

		book.page( "peaks_heights" )
				.buff( Breathless.class, "Breathless" )
				.buff( Hunger.class, BuffIndicator.HUNGER, "hungry" )
				.effect( GuideEffect.FIRE, "fire" )
				.custom( piece( "roof_thatch" ), "roof" )
				.mob( Hermit.class, "hermit's hut" )
				.custom( piece( "hearth_stone" ), "waystation" )
				.buff( Windswept.class, "wind" )
				.buff( Daze.class, "daze", "dazes" )
				.terrain( LAND, DungeonTileSheet.CHASM, "drop" )
				.terrain( LAND, DungeonTileSheet.FROZEN_WATER, "lake ice", "ice" )
				.water( Assets.Environment.WATER_SEWERS, "water" )
				.buff( Drenched.class, "soaked" )
				.buff( Chill.class, "chilled" );
	}

	private static void deep( CiteBook book ){
		book.page( "deep_overview" )
				.terrain( DRESS, OverworldDress.LINK_CAVE_MOUTH, "cave mouth" )
				.item( Pickaxe.class, "pickaxe" )
				.mob( CaveMiner.class, "dwarf" )
				.mob( Bat.class, "bat" )
				.mob( AlbinoRat.class, "rat" )
				.mob( Slime.class, "slime" )
				.mob( Snake.class, "snake" )
				.mob( Swarm.class, "swarm" )
				.mob( Spinner.class, "spinner" )
				.mob( Brute.class, "gnoll brute" )
				.mob( Shaman.RedShaman.class, "shaman" )
				.mob( ClayGolem.class, "clay golem" )
				.mob( FossilSkeleton.class, "fossil skeleton" )
				.mob( CrystalWisp.class, "crystal wisp" )
				.mob( AlbinoPiranha.class, "albino piranha" )
				.mob( Golem.class, "golem" )
				.mob( FungalSpinner.class, "fungal spinner" )
				.mob( Wraith.class, "wraith" )
				.mob( Minotaur.class, "minotaur" )
				.mob( GreyOni.class, "grey oni" )
				.mob( Elemental.FireElemental.class, "fire elemental" )
				.mob( LostSoul.class, "lost soul" )
				.terrain( DRESS, OverworldDress.ORE_FACE, "ore vein" )
				.item( MasterworkCore.class, "masterwork core" );

		book.page( "deep_places" )
				.terrain( DRESS, OverworldDress.CAVE_CART, "old mine", "cart" )
				.mob( Skeleton.class, "skeleton" )
				.item( Torch.class, "torch" )
				.item( Pickaxe.class, "pickaxe" )
				.terrain( DRESS, OverworldDress.CAVE_MUSH_CAP[0], "mushroom grotto" )
				.mob( FungalSentry.class, "fungal guard" )
				.terrain( DRESS, OverworldDress.CAVE_SHARDS[0], "crystal cavern", "glowing shard" )
				.mob( CrystalWisp.class, "crystal wisp" )
				.mob( CrystalGuardian.class, "crystal guardian" )
				.terrain( DRESS, OverworldDress.CAVE_CAMPFIRE, "miners' camp", "fire" )
				.terrain( DRESS, OverworldDress.CAVE_BEDROLL, "bedroll" )
				.mob( CaveMiner.class, "dwarf trader" )
				.item( Food.class, "ration" )
				.mob( LakeShrine.class, "lake shrine", "idol" )
				.buff( Bless.class, "Bless" )
				.mob( ShrinePiranha.class, "piranha" )
				.terrain( DRESS, OverworldDress.CAVE_LAVA_EW[0], "burning rift", "lava" )
				.mob( RiftElemental.class, "rift elemental" )
				.mob( DwarvenForge.class, "dwarven forge" )
				.terrain( DRESS, OverworldDress.CAVE_TOMB_RUNE, "sealed tomb" )
				.terrain( DRESS, OverworldDress.CAVE_SARCO, "sarcophagus" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.mob( DwarfLich.class, "lich" )
				.item( IronKey.class, "key" );

		book.page( "deep_hazards" )
				//the pocket's faint haze (FiredampParticle is no window-safe preset): the closest pour
				.effect( GuideEffect.STENCH, "firedamp", "haze" )
				.buff( Vertigo.class, "dizzy" )
				.item( Torch.class, "torch" )
				.buff( Burning.class, "burning" )
				.effect( GuideEffect.FIRE, "fire" )
				.item( Pickaxe.class, "mining" )
				.effect( GuideEffect.ROCKS, "rocks fall" )
				.buff( Cripple.class, "crippled" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.FLAT_MINE_BOULDER, "rubble" )
				.water( Assets.Environment.WATER_CAVES, "cave water" );
	}

	private static void homestead( CiteBook book ){
		book.page( "hs_overview" )
				.mob( Shopkeeper.class, "general store" )
				.item( BuildersTool.class, "builder's tool" )
				.item( BuildersSatchel.class, "builder's satchel" )
				.item( WoodcuttersAxe.class, "woodcutter's axe" )
				.item( MasonsHammer.class, "mason's hammer" )
				.item( Shovel.class, "shovel" )
				.item( Sickle.class, "sickle" )
				.item( Rope.class, "rope" )
				.mob( OverworldShopkeeper.class, "carpenter" )
				.terrain( LAND, DungeonTileSheet.WELL, "well" )
				.item( Plank.class, "plank" )
				.item( Thatch.class, "thatch" )
				.item( IronFittings.class, "iron fittings" )
				.item( GlassPane.class, "glass pane" )
				.mob( Hearthstone.class, "hearthstone" );

		book.page( "hs_gathering" )
				//section headings: what follows them is cited
				.plain( "Felling", "Planting" )
				.terrain( DRESS, OverworldDress.OAK_SUMMER_TOP, "tree" )
				.item( WoodcuttersAxe.class, "woodcutter's axe" )
				.item( Log.class, "oak log" )
				.item( PineLog.class, "pine log" )
				.item( Resin.class, "resin" )
				.item( Acorn.class, "acorn" )
				.item( PineCone.class, "pine cone" )
				.terrain( Assets.Environment.TERRAIN_FEATURES, feature( Terrain.STUMP_OAK ), "stump" )
				.terrain( Assets.Environment.TERRAIN_FEATURES, feature( Terrain.SAPLING_OAK ), "sapling" )
				.terrain( LAND, DungeonTileSheet.SHRUB_TILE, "bush" )
				.item( BushSeed.class, "bush seed" )
				.terrain( DRESS, OverworldDress.ROCK_BODY[0], "boulder" )
				.item( RoughStone.class, "rough stone", "Stone" )
				.item( Pickaxe.class, "pickaxe" )
				.item( MasonsHammer.class, "mason's hammer" )
				.item( Shovel.class, "shovel" )
				.custom( piece( "farm_plot" ), "farmland" )
				.item( Clay.class, "clay" )
				.item( Sand.class, "sand" )
				.item( Sickle.class, "sickle" )
				.terrain( LAND, DungeonTileSheet.FLAT_HIGH_GRASS, "tall grass" )
				.item( Thatch.class, "thatch" )
				.mob( Deer.class, "deer" )
				.item( Hide.class, "hide" );

		book.page( "hs_building" )
				.item( BuildersTool.class, "builder's tool" )
				.custom( BuildIcons.MENU::get, "build menu", "The menu" )
				.custom( BuildIcons.RECENT::get, "Recent" )
				.custom( BuildIcons.PLACE::get, "build bar" )
				.custom( BuildIcons.LINE::get, "line" )
				.custom( BuildIcons.AREA::get, "area" )
				.custom( BuildIcons.DEMOLISH::get, "Demolish" )
				.custom( BuildIcons.REPAIR::get, "Repair" )
				.custom( BuildIcons.INSPECT::get, "Inspect" )
				.custom( piece( "wall_plank" ), "walls" )
				.custom( piece( "window_plank" ), "window" )
				.custom( piece( "door" ), "door" )
				.custom( piece( "door_barred" ), "barred door" )
				.custom( piece( "floor_plank" ), "floor" )
				.custom( piece( "path" ), "cobbled path" )
				.custom( piece( "fence" ), "fence" )
				.custom( piece( "gate" ), "gate" )
				.custom( piece( "palisade" ), "palisade" )
				.custom( piece( "bridge" ), "plank bridge" )
				.custom( piece( "roof_thatch" ), "roofs" )
				.terrain( Assets.Environment.TERRAIN_FEATURES, feature( Terrain.RUBBLE_WOOD ), "rubble" )
				.item( Pickaxe.class, "pickaxe" )
				.mob( StandingTorch.class, "torches" );

		book.page( "hs_stations" )
				.custom( BuildIcons.STATIONS::get, "stations" )
				.mob( Workbench.class, "workbench" )
				.mob( MasonsBench.class, "mason's bench" )
				.mob( Kiln.class, "kiln" )
				.mob( Hearth.class, "hearth" )
				.mob( CookingPot.class, "cooking pot" )
				.mob( TanningRack.class, "tanning rack" )
				.mob( Forge.class, "forge" )
				.mob( Quern.class, "quern" )
				.custom( piece( "alchemy_pot" ), "alchemy pot" )
				.mob( EnchantingStation.class, "enchanting pedestal" )
				.item( Plank.class, "plank" )
				.item( Rope.class, "rope" )
				.item( Torch.class, "torch" )
				.item( CutStone.class, "cut stone" )
				.item( FiredBrick.class, "fired brick" )
				.item( GlassPane.class, "glass pane" )
				.item( Charcoal.class, "charcoal" )
				.item( Leather.class, "leather" )
				.item( IronFittings.class, "iron fittings" )
				.item( Flour.class, "flour" )
				.item( Bread.class, "bread" )
				.item( MeatPie.class, "meat pie" )
				//a room with a workbench and another station: a kind of room, with no picture of its own
				.plain( "workshop" );

		book.page( "hs_rooms" )
				//what a room is and what it scores: ways of reading built pieces, no things of their own
				.plain( "room", "sealed", "Comfort", "Room types" )
				.custom( piece( "wall_stone" ), "walls" )
				.custom( piece( "window_plank" ), "window" )
				.custom( piece( "door" ), "door" )
				.custom( piece( "roof_thatch" ), "roof" )
				.custom( piece( "fence" ), "fence" )
				.custom( piece( "gate" ), "gate" )
				.custom( piece( "palisade" ), "palisade" )
				.terrain( DRESS, OverworldDress.OAK_SUMMER_TOP, "tree" )
				.custom( piece( "bed_straw" ), "straw bed", "straw" )
				.custom( piece( "bed_wood" ), "wooden bed", "Beds", "wooden" )
				.custom( piece( "bed_fur" ), "fur bed", "fur" )
				.mob( Bedroll.class, "bedroll" )
				.effect( GuideEffect.FIRE, "fire" )
				.buff( WellRested.class, "well rested" )
				.custom( piece( "rug" ), "rug" )
				.custom( piece( "bookshelf" ), "bookshelf" )
				.custom( piece( "statue" ), "statue" )
				.mob( Hearth.class, "hearth" )
				.mob( CookingPot.class, "cooking pot" )
				.item( MeatPie.class, "meat pie" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.buff( ShrineBlessing.class, "shrine", "bless" )
				.mob( Workbench.class, "workbench" )
				.custom( piece( "roof_glass" ), "glass" );

		book.page( "hs_shelter" )
				//the closed walls the page works with, read as a whole (hs_rooms shows what makes one)
				.plain( "room" )
				.custom( piece( "roof_thatch" ), "roof", "thatch" )
				.buff( Drenched.class, "rain" )
				.terrain( LAND, DungeonTileSheet.SNOW_TILE, "snow" )
				.buff( Windswept.class, "wind" )
				.custom( piece( "wall_log" ), "log" )
				.custom( piece( "wall_plank" ), "plank" )
				.custom( piece( "wall_stone" ), "stone" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.FLAT_WALL, "rock" )
				.custom( piece( "wall_brick" ), "brick" )
				.custom( piece( "window_plank" ), "window" )
				.custom( piece( "door" ), "door" )
				.terrain( DRESS, OverworldDress.CAVE_CAMPFIRE, "camp fire" )
				.mob( Hearth.class, "hearth" )
				.mob( Forge.class, "forge" )
				.mob( Kiln.class, "kiln" )
				.custom( BuildIcons.INSPECT::get, "INSPECT" )
				.buff( Comfy.class, "comfy" )
				.buff( Hypothermia.class, "cold" )
				.buff( Heatstroke.class, "heat" )
				.effect( GuideEffect.STORM_CLOUD, "lightning" )
				.mob( Hearthstone.class, "hearthstone" );

		book.page( "hs_farming" )
				//headings over what follows them, which is cited
				.plain( "Seasons", "Pests" )
				.item( Shovel.class, "shovel" )
				.custom( piece( "farm_plot" ), "farmland", "farm plot" )
				.mob( FarmCrop.class, "crop" )
				.custom( piece( "roof_glass" ), "greenhouse", "glass" )
				.water( Assets.Environment.WATER_SEWERS, "Water" )
				.custom( piece( "channel" ), "irrigation channel" )
				.custom( piece( "well" ), "well" )
				.item( Sickle.class, "sickle" )
				.plant( BlandfruitBush.class, "blandfruit bush" )
				.item( Blandfruit.class, "blandfruit" )
				.plant( Cornwheat.class, "cornwheat" )
				.plant( Apricobush.class, "apricot bush" )
				.plant( Gobgrape.class, "gobgrape" )
				.plant( Eggbloom.class, "eggbloom" )
				.item( PlantEgg.class, "plant egg" )
				.plant( Tomatobush.class, "tomatobush" )
				.sprite( ItemSpriteSheet.SEED_HOLDER, "seed" )
				.terrain( LAND, DungeonTileSheet.FIELD_WINTER, "Frost", "winter" )
				.mob( Bunny.class, "bunny" )
				.mob( Deer.class, "deer" )
				.custom( piece( "fence" ), "fence" )
				.item( Fertilizer.class, "Plant food" )
				.mob( CompostBin.class, "compost bin" );

		book.page( "hs_animals" )
				//an action, not a thing: what it leaves is cited
				.plain( "Butchering" )
				.item( Rope.class, "rope" )
				.mob( Deer.class, "deer" )
				.mob( Goat.class, "goat" )
				.mob( Bunny.class, "bunny" )
				.custom( piece( "fence" ), "fence", "pen" )
				.custom( piece( "palisade" ), "palisade" )
				.mob( TiePost.class, "tie post", "TIE" )
				.custom( piece( "wall_stone" ), "built walls" )
				.custom( piece( "gate" ), "gate" )
				.item( MysteryMeat.class, "mystery meat" )
				.item( Hide.class, "hide" );

		book.page( "hs_homes" )
				//a home's score and the time you spend away: no things
				.plain( "notice", "While you are away" )
				.mob( Hearthstone.class, "hearthstone", "home" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.item( Gold.class, "gold" )
				.mob( FarmCrop.class, "crop" )
				.effect( GuideEffect.FIRE, "fire" )
				.custom( piece( "window_plank" ), "window" )
				.custom( piece( "wall_stone" ), "stone and brick walls" )
				.custom( piece( "door_barred" ), "barred door" )
				.custom( piece( "palisade" ), "palisade" )
				.mob( SiegeGrayWolf.class, "wolf sieges", "wolves" )
				.custom( piece( "fence" ), "fence" )
				.custom( piece( "gate" ), "gate" )
				.mob( HomeRaider.class, "bandit raids", "raiders" )
				.mob( Yeti.class, "yeti", "yetis" )
				.mob( Brute.class, "brute" )
				.custom( BuildIcons.DEFENCE::get, "Defences" )
				.mob( Watchtower.class, "watchtower" )
				.mob( AlarmBell.class, "alarm bell" )
				.custom( piece( ThreatSlice.SPIKE_PIECE ), "spike traps" )
				.buff( Cripple.class, "cripple" )
				.icon( Icons.COMPASS, "world map" );

		book.page( "hs_travel" )
				.mob( Hearthstone.class, "hearthstone" )
				.item( RoughStone.class, "rough stone" )
				.item( CutStone.class, "cut stone" )
				.mob( Waystone.class, "waystones" )
				.buff( Hunger.class, BuffIndicator.HUNGER, "hunger" )
				.buff( WellFed.class, "Well Fed" )
				.buff( Starving.class, "starving" )
				.buff( Levitation.class, "floating" )
				.buff( Roots.class, "rooted" )
				.item( Rope.class, "rope" )
				.icon( Icons.COMPASS, "world map" )
				.terrain( LAND, DungeonTileSheet.DIRT_PATH_TILE, "road" )
				.custom( piece( "path" ), "cobbled path" )
				.custom( piece( "bridge" ), "bridge" );
	}

	private static void town( CiteBook book ){
		book.page( "town_overview" )
				.icon( Icons.STAIRS, "staircase" )
				.item( Palantir.class, "Palantir" )
				.mob( Zot.class, "Zot" )
				.mob( Kupua.class, "kupua" )
				.mob( Gullin.class, "gullin" )
				.item( AresSword.class, "relic weapon" )
				.item( NornStone.class, "Norn Stones" )
				.icon( Icons.ALTAR_SHRINE, "altar" )
				.mob( OtilukeNPC.class, "Otiluke" )
				.mob( SpectralRat.class, "spectral rat" )
				.item( DragonCave.class, "Dragon Cave journal page" )
				.item( ShadowDragonEgg.class, "shadow dragon egg" );

		book.page( "town_dolyahaven" )
				.mob( Shopkeeper.class, "shop", "keeper" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" )
				.item( StoneOre.class, "stone ore" )
				.mob( Bishop.class, "church", "bishop" )
				.mob( EnchantingStation.class, "pedestal" )
				.mob( FortuneTellerFolk.class, "fortune teller" )
				.mob( Librarian.class, "library" )
				.item( DiaryPage.class, "miner's diary page" )
				.mob( Employee.class, "cinema" )
				.buff( Luck.class, "luck" )
				.mob( InnKeeper.class, "inn" )
				.mob( InnServant.class, "servant" )
				.mob( Bard.class, "bard" )
				.mob( Mercenary.class, "mercenary" )
				.mob( TownGuardFolk.class, "guard" )
				.mob( Townsfolk.class, "well-dressed local" )
				.mob( Tinkerer5.class, "villager" )
				.item( TownReturnBeacon.class, "Town Return Beacon" )
				.mob( Tinkerer1.class, "tinkerer" )
				.item( Mushroom.class, "toadstool mushroom" )
				.item( Waterskin.class, "waterskin" )
				.item( ActiveMrDestructo.class, "Mr Destructo" )
				.mob( Blacksmith2.class, "town smith" )
				.item( DarkGold.class, "dark gold" );

		book.page( "town_dragoncave" )
				.item( DragonCave.class, "Dragon Cave page" )
				.item( OtilukesJournal.class, "journal" )
				.trap( FleecingTrap.class, "fleecing traps" )
				.buff( Levitation.class, "Levitating" )
				.sprite( ItemSpriteSheet.BONES, "skeleton" )
				.item( Gold.class, "gold" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" )
				.item( ShadowDragonEgg.class, "shadow dragon egg" ).primary()
				.mob( ShadowDragon.class, "shadow dragon" );
	}

	private static void mines( CiteBook book ){
		book.page( "mines_f1" )
				.mob( Kupua.class, "kupua" ).primary()
				.buff( Light.class, "light" )
				.terrain( MINES, DungeonTileSheet.FLAT_BARRICADE, "barricades" )
				.effect( GuideEffect.FIRE, "burn" )
				.item( StoneOre.class, "stone ore" )
				.item( Gold.class, "gold" )
				.item( AresSword.class, "relic weapon" )
				.effect( GuideEffect.CORROSIVE_GAS, "corrupting gas" )
				.mob( Gullin.class, "gullin" )
				.item( NornStone.class, "Norn Stones" );

		book.page( "mines_f2" )
				.item( TownReturnBeacon.class, "Town Return Beacon" ).primary()
				.item( StoneOre.class, "ore" )
				.mob( TownGuard.class, "town guard" )
				.item( OtilukesJournal.class, "Otiluke's Journal" );

		book.page( "mines_f3" )
				.item( NornStone.class, "Norn Stones" ).primary()
				.mob( Gullin.class, "gullin" )
				.item( AresSword.class, "relic" )
				.icon( Icons.ALTAR_SHRINE, "altar" )
				.item( BlueNornStone.class, "blue norn stone", "Blue" )
				.item( OrangeNornStone.class, "orange norn stone", "Orange" )
				.item( PurpleNornStone.class, "purple norn stone", "Purple" )
				.item( YellowNornStone.class, "yellow norn stone", "Yellow" )
				.item( GreenNornStone.class, "green norn stone", "Green" )
				.item( CromCruachAxe.class, "Crom Cruach Axe" )
				.item( LokisFlail.class, "Loki's Flail" )
				.item( NeptunusTrident.class, "Neptunus Trident" )
				.item( JupitersWraith.class, "Jupiter's Wraith" )
				.mob( Kupua.class, "kupua" )
				.mob( MineSentinel.class, "mine guardian" )
				.mob( Otiluke.class, "golem" )
				.mob( Zot.class, "Zot" );

		book.page( "mines_f4" )
				.mob( Kupua.class, "kupua" )
				.mob( Gullin.class, "gullin" )
				.item( AresSword.class, "relic" )
				.effect( GuideEffect.CORROSIVE_GAS, "corrupting gas", "cloud" );

		book.page( "mines_f5" )
				.item( StoneOre.class, "ore" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.sprite( ItemSpriteSheet.BONES, "skeleton" )
				.item( Gold.class, "gold" )
				.hero( HeroClass.ROGUE, "rogue" )
				.item( Bomb.class, "bomb" )
				.mob( Kupua.class, "kupua" )
				.terrain( DRESS, OverworldDress.ORE_FACE, "ore vein" )
				.terrain( MINES, DungeonTileSheet.FLAT_BARRICADE, "barricades" )
				.effect( GuideEffect.FIRE, "burn" )
				.terrain( MINES, DungeonTileSheet.GRASS, "moss" );

		book.page( "mines_f6" )
				.mob( SpectralRat.class, "spectral rats" ).primary()
				//a night of the moon's cycle: no picture of its own
				.plain( "full-moon" )
				.buff( Weakness.class, "weakens" );

		book.page( "mines_f7" )
				.mob( Kupua.class, "kupua" )
				.item( Whistle.class, "Whistle" )
				.item( Egg.class, "Egg" )
				.mob( Tengu.class, "Tengu" )
				.icon( Icons.INCUBATOR_NEST, "incubator nest room" );

		book.page( "mines_f8" )
				.item( OtilukesJournal.class, "Otiluke's journal", "Otiluke" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_BOOKSHELF, "bookshelf" )
				.item( Vault.class, "Vault" )
				.item( DragonCave.class, "Dragon Cave" )
				.buff( MindVision.class, "mind vision" )
				.effect( GuideEffect.FIRE, "burn" )
				.item( ShadowDragonEgg.class, "shadow dragon egg" );

		book.page( "mines_f9" )
				.item( TownReturnBeacon.class, "Town Return Beacon" )
				.item( StoneOre.class, "ore" )
				.mob( Otiluke.class, "golem" )
				.item( AresSword.class, "relic weapon" )
				.item( PotionOfHealing.class, "healing" )
				.mob( MineSentinel.class, "mine guardians" )
				.mob( LitTower.class, "lit towers" );

		book.page( "mines_boss" )
				.mob( Otiluke.class, "Otiluke", "stone golem", "golem" )
				.mob( Zot.class, "Zot" )
				.item( Palantir.class, "Palantir" )
				.terrain( SEALED, DungeonTileSheet.FLAT_DOOR_LOCKED, "locked door" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.item( IronKey.class, "iron key" )
				.mob( MineSentinel.class, "mine guardians" )
				.item( HallsKey.class, "Halls Key" )
				.mob( YogDzewa.class, "Yog-Dzewa" )
				.item( SkeletonKey.class, "Skeleton Key" )
				.mob( LitTower.class, "lit towers" );

		book.page( "boss_otiluke" )
				.mob( Otiluke.class, "golem" )
				.item( AresSword.class, "relic weapon" )
				.icon( Icons.ALTAR_SHRINE, "town altar" )
				.mob( LitTower.class, "lit tower" )
				.mob( MineSentinel.class, "guardian" )
				.buff( Weakness.class, "weaken" )
				.sprite( ItemSpriteSheet.POTION_HOLDER, "potion" )
				.item( Palantir.class, "Palantir" )
				.mob( Zot.class, "Zot" );
	}
}

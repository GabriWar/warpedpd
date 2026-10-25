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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Alligator;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bison;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Brute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Camel;
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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Pike;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Raider;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RiftElemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RuinSkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shaman;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShrinePiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shark;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Slime;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpectralRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Spinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Swarm;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Vulture;
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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.LighthouseKeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OtilukeNPC;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Horse;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OverworldShopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummitCairn;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer1;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer5;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TownGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Traveller;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TravellingMerchant;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.VillageFisher;
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
import xyz.gabriwar.warpedpixeldungeon.bounty.NoticeBoard;
import xyz.gabriwar.warpedpixeldungeon.bounty.Parcel;
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
import xyz.gabriwar.warpedpixeldungeon.homestead.mounts.Riding;
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
import xyz.gabriwar.warpedpixeldungeon.items.food.Fish;
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
import xyz.gabriwar.warpedpixeldungeon.items.food.Aprico;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cornwheatshaft;
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
import xyz.gabriwar.warpedpixeldungeon.items.materials.GatorTooth;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GlassPane;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.catching.BugNet;
import xyz.gabriwar.warpedpixeldungeon.items.catching.CritterJar;
import xyz.gabriwar.warpedpixeldungeon.items.catching.Earthworm;
import xyz.gabriwar.warpedpixeldungeon.items.catching.FishingRod;
import xyz.gabriwar.warpedpixeldungeon.items.catching.MessageBottle;
import xyz.gabriwar.warpedpixeldungeon.items.catching.OldBoot;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.items.MagicWorldMap;
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
import xyz.gabriwar.warpedpixeldungeon.items.materials.VultureFeather;
import xyz.gabriwar.warpedpixeldungeon.items.misc.Saddle;
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
import xyz.gabriwar.warpedpixeldungeon.items.sea.Raft;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Sailboat;
import xyz.gabriwar.warpedpixeldungeon.items.sea.SeaChart;
import xyz.gabriwar.warpedpixeldungeon.items.sea.SeaLantern;
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
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.FishingSpear;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FleecingTrap;
import xyz.gabriwar.warpedpixeldungeon.plants.Apricobush;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Cornwheat;
import xyz.gabriwar.warpedpixeldungeon.plants.Eggbloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Gobgrape;
import xyz.gabriwar.warpedpixeldungeon.plants.Tomatobush;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.sea.Afloat;
import xyz.gabriwar.warpedpixeldungeon.sea.Swimming;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.HomesteadVisuals;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;
import xyz.gabriwar.warpedpixeldungeon.tiles.SeaArt;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.BuildIcons;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import com.watabou.noosa.Image;

//Cites for the world above: the surface, the peaks, the deep caves, homesteads, the town and its mines.
public final class SurfaceCites {

	private SurfaceCites(){}

	//the world's own sheets: the surface's tiles, and its dressing (trees, rocks, the ways between
	//the slices, the places of the mountains and the caves)
	private static final String LAND = Assets.Environment.TILES_OVERWORLD;
	private static final String DRESS = Assets.Environment.OVERWORLD_DRESS;
	//the desert places' props (tiles/DrylandsArt)
	private static final String DRY = Assets.Environment.DRYLANDS_PROPS;
	//the kupua mines are cave tiles, their sealed last floor and the dragon cave prison tiles
	private static final String MINES = Assets.Environment.TILES_CAVES;
	private static final String SEALED = Assets.Environment.TILES_PRISON;

	public static void register( CiteBook book ){
		surface( book );
		peaks( book );
		sea( book );
		deep( book );
		wetlands( book );
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

		book.page( "surface_bounties" )
				.mob( NoticeBoard.class, "notice board" ).primary()
				.sprite( ItemSpriteSheet.NOTICE_HUNT, "hunt" )
				.mob( GrayWolf.class, "gray wolves" )
				.mob( AlbinoRat.class, "albino rats" )
				.sprite( ItemSpriteSheet.NOTICE_GATHER, "gathering" )
				.item( Hide.class, "hides" )
				.item( Log.class, "oak logs" )
				.item( IronOre.class, "iron ore" )
				.item( Fish.class, "fish" )
				.sprite( ItemSpriteSheet.NOTICE_DELIVER, "delivery" )
				.item( Parcel.class, "parcel" )
				.mob( Villager.class, "elder" )
				.item( Gold.class, "gold" )
				.mob( OverworldGuard.class, "guards" );

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

		book.page( "surface_beasts" )
				.mob( Bison.class, "Bison" ).primary()
				.item( Hide.class, "hide" )
				.item( MysteryMeat.class, "mystery meat" )
				.mob( Camel.class, "Camels" )
				.buff( Daze.class, "dazed" )
				.mob( Deer.class, "deer" )
				.mob( Vulture.class, "Vultures" )
				.item( VultureFeather.class, "vulture feathers" )
				.mob( Alligator.class, "Alligators" )
				.water( Assets.Environment.WATER_SEWERS, "pools", "water" )
				.item( GatorTooth.class, "alligator tooth" )
				.mob( Pike.class, "Pike" )
				.item( FishingSpear.class, "fishing spear" )
				.mob( Workbench.class, "workbench" )
				.item( Plank.class, "plank" )
				.item( Dart.class, "darts" );
		book.page( "surface_nature" )
				.sprite( ItemSpriteSheet.EV_WILDFIRE, "wildfire" ).primary()
				.terrain( Assets.Environment.TERRAIN_FEATURES, feature( Terrain.STUMP_PINE ), "stumps" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfWater.class, "bottle of water" )
				.terrain( Assets.Environment.TERRAIN_FEATURES, feature( Terrain.SAPLING_PINE ), "saplings" )
				.item( Charcoal.class, "charcoal" )
				.sprite( ItemSpriteSheet.EV_FLOOD, "flood" )
				.terrain( LAND, DungeonTileSheet.MUD_TILE, "mud" )
				.sprite( ItemSpriteSheet.EV_ECLIPSE, "eclipse" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfCorruption.class, "wand of corruption" );
		book.page( "surface_festivals" )
				.plain( "festival", "great beast", "tracks" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FestivalCook.class, "festival cooks" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FestivalChampion.class, "village champion" )
				.sprite( ItemSpriteSheet.WEAPON_HOLDER, "weapon" )
				.sprite( ItemSpriteSheet.ARMOR_HOLDER, "armor" )
				.sprite( ItemSpriteSheet.WAND_HOLDER, "wand" )
				.sprite( ItemSpriteSheet.RING_HOLDER, "ring" )
				.mob( Deer.class, "deer" )
				.mob( GrayWolf.class, "wolves" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreatWolf.class, "great wolf" ).primary()
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.ElderYeti.class, "elder yeti" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.GiantScorpio.class, "giant scorpion" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.RoamerTrophy.class, "trophy" )
				.item( Gold.class, "gold" );

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

		//the desert (levels/overworld/Drylands, actors/Sandstorm, levels/SandTempleLevel)
		book.page( "surface_oasis" )
				.water( Assets.Environment.WATER_SEWERS, "oasis" )
				.terrain( DRY, xyz.gabriwar.warpedpixeldungeon.levels.overworld.Drylands.ART_PALM_CROWN, "date palms" ).primary()
				.buff( Heatstroke.class, "heatstroke" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.EmptyBottle.class, "empty bottles" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.food.Dates.class, "dates" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.OasisTrader.class, "oasis trader" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.misc.SandGoggles.class, "sand goggles" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.misc.DesertScarf.class, "desert scarf" )
				.mob( Deer.class, "deer" );

		book.page( "surface_sandstorm" )
				.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sandblown.class, "sandstorm" )
				.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness.class, "blind" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.misc.SandGoggles.class, "sand goggles" ).primary()
				.item( xyz.gabriwar.warpedpixeldungeon.items.misc.DesertScarf.class, "desert scarf" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.materials.GlassPane.class, "glass pane" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.materials.Leather.class, "leather" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch.class, "thatch" )
				.plain( "lee" );

		book.page( "surface_temples" )
				.terrain( DRY, xyz.gabriwar.warpedpixeldungeon.levels.overworld.Drylands.ART_STAIR, "buried temples" )
				.terrain( DRY, xyz.gabriwar.warpedpixeldungeon.levels.overworld.Drylands.ART_OBELISK_TOP, "obelisks" )
				.terrain( DRY, xyz.gabriwar.warpedpixeldungeon.levels.overworld.Drylands.ART_STATUE, "jackal statues" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake.class, "snakes" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mummy.class, "mummies" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton.class, "skeletons" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Scorpio.class, "scorpions" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk.class, "monks" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue.class, "statue" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.trinkets.SunScarab.class, "Sun Scarab" ).primary();

		book.page( "boss_dragon" )
				.mob( OverworldDragon.class, "dragon" )
				.buff( Poison.class, "poison" )
				.terrain( LAND, DungeonTileSheet.FLAT_WALL, "rock" )
				.item( Gold.class, "gold", "hoard" );
	}

	//the sea's places as their sheet draws them (tiles/SeaArt)
	private static final String SEA = SeaArt.TEXTURE;

	private static void sea( CiteBook book ){
		book.page( "sea_sailing" )
				.item( Raft.class, "raft" ).primary()
				.item( Sailboat.class, "sailboat" )
				.mob( Workbench.class, "workbench" )
				.mob( Forge.class, "forge" )
				.item( Log.class, "log" )
				.item( Rope.class, "rope" )
				.item( IronFittings.class, "iron fittings" )
				.item( Hide.class, "hide" )
				.item( Leather.class, "leather" )
				.mob( VillageFisher.class, "fisher" )
				.buff( Afloat.class, BuffIndicator.BOAT, "Aboard", "moored" )
				.icon( Icons.COMPASS, "Speed", "wind" )
				.buff( Drenched.class, "Weather", "soak" )
				.item( Plank.class, "Repair", "plank" )
				.buff( Swimming.class, "Swimming" )
				.plain( "Launching" );

		book.page( "sea_islands" )
				.terrain( SEA, SeaArt.PALM_FRONDS[0], "Islands", "palm" ).primary()
				.item( SeaChart.class, "sea chart" )
				.terrain( SEA, SeaArt.DIG, "cross" )
				.sprite( ItemSpriteSheet.CHEST, "buried chest" )
				.item( Shovel.class, "shovel" )
				.item( Gold.class, "gold" )
				.sprite( ItemSpriteSheet.RING_HOLDER, "ring" )
				.sprite( ItemSpriteSheet.WAND_HOLDER, "wand" )
				.sprite( ItemSpriteSheet.ARTIFACT_HOLDER, "artifact" )
				.terrain( SEA, SeaArt.WRECK_EAST + 2, "shipwreck" )
				.terrain( SEA, SeaArt.SHAFT, "lighthouse" )
				.icon( Icons.COMPASS, "world map" );

		book.page( "sea_wrecks" )
				.terrain( SEA, SeaArt.WRECK_EAST + 2, "Shipwrecks" ).primary()
				.terrain( SEA, SeaArt.WRECK_EAST + 1 + SeaArt.ROW, "hold" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.item( Plank.class, "plank" )
				.item( Rope.class, "rope" )
				.item( Gold.class, "gold" )
				.sprite( ItemSpriteSheet.RING_HOLDER, "ring" )
				.sprite( ItemSpriteSheet.WAND_HOLDER, "wand" )
				.sprite( ItemSpriteSheet.ARTIFACT_HOLDER, "artifact" )
				.icon( Icons.COMPASS, "world map" );

		book.page( "sea_lighthouses" )
				.terrain( SEA, SeaArt.SHAFT, "Lighthouses" ).primary()
				.terrain( SEA, SeaArt.LAMP, "lamp" )
				.mob( LighthouseKeeper.class, "keeper" )
				.terrain( SEA, SeaArt.APRON[1], "paving" )
				.item( SeaChart.class, "Sea chart" )
				.item( SeaLantern.class, "Sea lantern" )
				.item( Torch.class, "torch" )
				.mob( Shark.class, "shark" )
				.icon( Icons.COMPASS, "world map" );

		book.page( "sea_sharks" )
				.mob( Shark.class, "Sharks" ).primary()
				.buff( Bleeding.class, "bleeding" )
				.item( Raft.class, "raft" )
				.item( Sailboat.class, "sailboat" )
				.item( SeaLantern.class, "sea lantern" )
				.mob( LighthouseKeeper.class, "lighthouse keepers" );
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

	//the swamps and the shores (levels/overworld/WetlandSites)
	private static void wetlands( CiteBook book ){
		book.page( "wet_swamps" )
				.terrain( LAND, DungeonTileSheet.FLAT_HIGH_GRASS, "Swamps" )
				.effect( GuideEffect.STEAM, "low mist" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.wetlands.BogLantern.class, "bog lantern" )
				.water( Assets.Environment.WATER_SEWERS, "water" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.wetlands.WadingBalm.class, "wading balm" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SunkenIdol.class, "sunken shrine" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SwampWitch.class, "swamp witch" )
				.icon( Icons.COMPASS, "world map" );

		book.page( "wet_witch" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SwampWitch.class, "swamp witch" ).primary()
				.terrain( DRESS, OverworldDress.BRIDGE_NS[0], "boardwalk" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.wetlands.WitchsRemedy.class, "witch's remedy" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.wetlands.BogLantern.class, "bog lantern" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.wetlands.WadingBalm.class, "wading balm" )
				.item( xyz.gabriwar.warpedpixeldungeon.plants.Sungrass.Seed.class, "herbs" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.food.PixieParasol.class, "mushrooms" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat.class, "critter parts" )
				.sprite( ItemSpriteSheet.POTION_HOLDER, "potion" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse.class, "curse" )
				.plain( "brews", "exotic" );

		book.page( "wet_shrine" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SunkenIdol.class, "sunken idol", "sunken shrine" ).primary()
				.terrain( LAND, DungeonTileSheet.FLAT_STATUE, "statue" )
				.item( Gold.class, "gold" )
				.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.BogBlessing.class, "bog's blessing" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.DrownedAcolyte.class, "drowned acolyte" )
				.plain( "offering" );

		book.page( "wet_tidepools" )
				.water( Assets.Environment.WATER_SEWERS, "tide pools" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.wetlands.Seashell.class, "seashell" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.wetlands.Pearl.class, "pearl" ).primary()
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Crab.class, "crab" )
				.mob( xyz.gabriwar.warpedpixeldungeon.actors.mobs.PearlOyster.class, "pearl oyster" )
				.plain( "tide" );
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
				.item( IronOre.class, "ore" )
				.item( xyz.gabriwar.warpedpixeldungeon.items.ore.Amethyst.class, "gem" )
				.item( StoneOre.class, "stone ore" )
				.item( DarkGold.class, "dark gold" )
				.item( NornStone.class, "norn stone" )
				.item( MasterworkCore.class, "masterwork core" )
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
				.mob( Bison.class, "bison" )
				.mob( Camel.class, "camel" )
				.custom( piece( "fence" ), "fence", "pen" )
				.custom( piece( "palisade" ), "palisade" )
				.mob( TiePost.class, "tie post", "TIE" )
				.custom( piece( "wall_stone" ), "built walls" )
				.custom( piece( "gate" ), "gate" )
				.item( MysteryMeat.class, "mystery meat" )
				.item( Hide.class, "hide" );

		book.page( "hs_horses" )
				.mob( Horse.class, "wild horses" ).primary()
				//an action, not a thing: what it takes is cited
				.plain( "Taming" )
				.item( Thatch.class, "thatch" )
				.item( Cornwheatshaft.class, "cornwheat shaft" )
				.item( Aprico.class, "aprico" )
				.item( Blandfruit.class, "blandfruit" )
				.item( Rope.class, "rope" )
				.mob( TiePost.class, "tie post" )
				.item( Saddle.class, "saddle" )
				.mob( TanningRack.class, "tanning rack" )
				.mob( OverworldShopkeeper.class, "stablehand" )
				.buff( Riding.class, "Riding" );

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

		catching( book );
	}

	//the bug net and the fishing rod (items/catching): their pages sit in the homesteads' chapter
	private static void catching( CiteBook book ){
		book.page( "hs_catching" )
				//a rule, not a thing
				.plain( "Wary critters" )
				.item( BugNet.class, "bug net" ).primary()
				.item( CritterJar.class, "jar" )
				.custom( butterfly(), "butterflies" )
				.custom( () -> new ItemSprite( ItemSpriteSheet.JAR_WINGS, new ItemSprite.Glowing( 0xE8F060 ) ), "fireflies" )
				.custom( smallLife( DungeonCritterSprite.ROW_MOTH ), "moths" )
				.custom( smallLife( DungeonCritterSprite.ROW_FLY ), "flies" )
				.custom( smallLife( DungeonCritterSprite.ROW_BEETLE ), "beetles" )
				.custom( smallLife( DungeonCritterSprite.ROW_SNAIL ), "snails" )
				.custom( smallLife( DungeonCritterSprite.ROW_FROG ), "frogs" )
				.item( FishingRod.class, "fishing rod" )
				.mob( Workbench.class, "workbench" )
				.item( Plank.class, "plank" )
				.item( Rope.class, "rope" );

		book.page( "hs_fishing" )
				.item( FishingRod.class, "fishing rod" ).primary()
				.terrain( LAND, DungeonTileSheet.FROZEN_WATER, "ice" )
				.item( Earthworm.class, "earthworms", "bait" )
				.item( Shovel.class, "shovel" )
				.item( BugNet.class, "bug net" )
				.custom( () -> new Image( Assets.Effects.FISHING, 12, 0, 5, 7 ), "bobber" )
				.mob( Hearth.class, "hearth" )
				.mob( CookingPot.class, "cooking pot" )
				.mob( Villager.class, "fishers" )
				.item( IronFittings.class, "iron fittings" );

		book.page( "hs_fish" )
				.sprite( ItemSpriteSheet.FISH_TROUT, "brook trout" ).primary()
				.sprite( ItemSpriteSheet.FISH_PERCH, "perch" )
				.sprite( ItemSpriteSheet.FISH_PIKE, "pike" )
				.sprite( ItemSpriteSheet.FISH_CATFISH, "catfish" )
				.sprite( ItemSpriteSheet.FISH_MACKEREL, "mackerel" )
				.sprite( ItemSpriteSheet.FISH_CHAR, "arctic char" )
				.sprite( ItemSpriteSheet.FISH_CAVEFISH, "cave fish" )
				.item( OldBoot.class, "old boot" )
				.sprite( ItemSpriteSheet.GOLD, "sodden purse" )
				.item( MessageBottle.class, "message in a bottle" )
				.item( MagicWorldMap.class, "world map" )
				.sprite( ItemSpriteSheet.CHEST, "chest" );
	}

	//a butterfly of the surface's critters (effects/CritterSprite: its row, the brimstone's yellow)
	private static Cite.Picture butterfly(){
		return () -> {
			Image i = new Image( Assets.Effects.CRITTERS, 0, 7 * 16, 16, 16 );
			i.hardlight( 0xF4DC50 );
			return i;
		};
	}

	//one of the dungeon's small life, as its sheet draws it at rest (effects/DungeonCritterSprite)
	private static Cite.Picture smallLife( int row ){
		return () -> new Image( Assets.Effects.DUNGEON_CRITTERS, 0, row * 16, 16, 16 );
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

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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Dewcharge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hex;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.LockedFloor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.Ratmogrify;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ArmoredBrute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Assassin;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bee;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrokenRobot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownBat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Brute;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ClayGolem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Crab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalGuardian;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.CrystalSpire;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM100;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM200;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM201;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM300;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Elemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FetidRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FossilSkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FungalCore;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FungalSentry;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FungalSpinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Ghoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollArcher;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollExile;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollGeomancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollSapper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollTrickster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Goo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreatCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GreyRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Guard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HermitCrab;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MossySkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Necromancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Piranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.PoisonGoo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Pylon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RatBoss;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RedWraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RotHeart;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RotLasher;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Scorpio;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shaman;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Slime;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SlimeBrown;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SlimeRed;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpectralNecromancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderEgg;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderExploding;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderGuard;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderMind;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderNest;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderQueen;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderServant;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Spinner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Swarm;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.TempleSentry;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Thief;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tower;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Velocirooster;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Wraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Zombie;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith2;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Ghost;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.MirrorImage;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.RatKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer1;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Wandmaker;
import xyz.gabriwar.warpedpixeldungeon.items.ActiveMrDestructo;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantWand;
import xyz.gabriwar.warpedpixeldungeon.items.Ankh;
import xyz.gabriwar.warpedpixeldungeon.items.CavesKey;
import xyz.gabriwar.warpedpixeldungeon.items.Dewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.EnergyCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.KingsCrown;
import xyz.gabriwar.warpedpixeldungeon.items.Mushroom;
import xyz.gabriwar.warpedpixeldungeon.items.OtilukesJournal;
import xyz.gabriwar.warpedpixeldungeon.items.PrisonKey;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarah;
import xyz.gabriwar.warpedpixeldungeon.items.SewersKey;
import xyz.gabriwar.warpedpixeldungeon.items.SpiderCharm;
import xyz.gabriwar.warpedpixeldungeon.items.Stylus;
import xyz.gabriwar.warpedpixeldungeon.items.TengusMask;
import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.DriedRose;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.LloydsBeacon;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.Sokoban1;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.Sokoban2;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.Sokoban3;
import xyz.gabriwar.warpedpixeldungeon.items.keys.CrystalKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.GoldenKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.WornKey;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLiquidFlame;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMending;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.quest.CeremonialCandle;
import xyz.gabriwar.warpedpixeldungeon.items.quest.CorpseDust;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DarkGold;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Embers;
import xyz.gabriwar.warpedpixeldungeon.items.quest.GooBlob;
import xyz.gabriwar.warpedpixeldungeon.items.quest.MetalShard;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRecharging;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfBlink;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfIntuition;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrinketCatalyst;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Chainsaw;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.AlarmTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.BurningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ChillingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.CorrosionTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ExplosiveTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FrostTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GrippingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.PitfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.PoisonDartTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ShockingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.StormTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.SummoningTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.TeleportationTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.TenguDartTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.ToxicTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WornDartTrap;
import xyz.gabriwar.warpedpixeldungeon.plants.Rotberry;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.DM300Sprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuidePuppet;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIcon;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.HeroIcon;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import xyz.gabriwar.warpedpixeldungeon.ui.guide.LiveCrop;
import com.watabou.noosa.Image;

//Cites for the first regions: the sewers, the prison and its spider nest, the caves with their mine and temple.
public final class EarlyCites {

	private EarlyCites(){}

	//custom tiles, by their place in the sheet (16x16 cells, 8 a row in caves_boss, 16 in prison_quest)
	private static final int WIRES = 37, GATE = 34;          //CavesBossLevel.ArenaVisuals
	private static final int RITUAL_MARK = 66;               //the middle of RitualSiteRoom.RitualMarker
	private static final int MASS_GRAVE = 73;                //the middle of MassGraveRoom.MassGraveDeco
	//the branch stairways' looks (custom_tiles/branch_stairs.png): the nest's webbed hole, the temple's stair
	private static final int NEST_STAIRS = 0, TEMPLE_STAIRS = 1;

	public static void register( CiteBook book ){
		sewers( book );
		prison( book );
		nest( book );
		caves( book );
	}

	private static void sewers( CiteBook book ){
		book.page( "sewers_overview" )
				.mob( Rat.class, "rat" )
				.mob( Gnoll.class, "gnoll" )
				.mob( Crab.class, "crab" )
				.icon( Icons.DEPTH_SECRETS, "secret room" )
				.mob( Shopkeeper.class, "shop" )
				.effect( GuideEffect.BLIZZARD, "snow" )
				.water( Assets.Environment.WATER_SEWERS, "water" )
				.terrain( Assets.Environment.FROZEN_SEWERS, 0, "ice" )
				.mob( Goo.class, "Goo" )
				.mob( Wraith.class, "wraith" );

		book.page( "sewers_f1" )
				.mob( Snake.class, "sewer snake" ).primary()
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.ENTRANCE, "stairs up" )
				.mob( Rat.class, "rat" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_DOOR, "doorway", "door" )
				.mob( BrownBat.class, "brown bat" )
				.buff( Blindness.class, "blind" )
				.mob( GreyRat.class, "grey rat" )
				.buff( Poison.class, "poison" )
				.buff( Burning.class, "fire" )
				.buff( MagicalSleep.class, "sleep" )
				.buff( Terror.class, "terror" )
				.mob( RatBoss.class, "rat boss" )
				.trap( WornDartTrap.class, "worn dart trap" )
				//"guide page" reads the same before and after the tome joins the guide
				.sprite( ItemSpriteSheet.GUIDE_PAGE, "guide page", "page on searching" )
				.item( Food.class, "food" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" )
				.item( StoneOfIntuition.class, "Stone of Intuition" )
				.item( TrinketCatalyst.class, "trinket catalyst", "catalyst" )
				.sprite( ItemSpriteSheet.LOCKED_CHEST, "locked chest" )
				.item( GoldenKey.class, "golden key" );

		book.page( "sewers_f2" )
				.mob( Tinkerer1.class, "tinkerer" ).primary()
				.item( Mushroom.class, "toadstool mushroom", "mushroom" )
				.item( Waterskin.class, "waterskin" )
				//the condenser is no buff of its own: the dew it draws is the dew charge's icon
				.custom( () -> new BuffIcon( new Dewcharge(), true ), "Dew Condenser" )
				.item( Dewdrop.class, "dewdrop", "Measured Draught" )
				.mob( Gnoll.class, "gnoll scout" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.mob( Mimic.class, "mimic" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_DOOR, "door" )
				.trap( SummoningTrap.class, "summoning trap" )
				.mob( Ghost.class, "sad ghost" )
				.mob( FetidRat.class, "fetid rat" )
				.mob( GnollArcher.class, "gnoll archer" )
				.item( SewersKey.class, "Sewers Ancient Key" )
				.icon( Icons.DEPTH_GRASS, "Field" )
				.mob( GnollExile.class, "gnoll exile", "exile" );

		book.page( "sewers_f3" )
				.mob( Swarm.class, "swarm of flies", "swarm" ).primary()
				.mob( Crab.class, "sewer crab", "crab" )
				.mob( Velocirooster.class, "velocirooster" )
				.effect( GuideEffect.FIRE, "fire" )
				.buff( Poison.class, "poison" )
				.sprite( ItemSpriteSheet.WAND_HOLDER, "wand" )
				.effect( GuideEffect.TOXIC_GAS, "gas" )
				.item( PotionOfMending.class, "Potion of Mending" )
				.mob( HermitCrab.class, "hermit crab" )
				.mob( Ghost.class, "ghost" )
				.mob( GnollTrickster.class, "gnoll trickster" )
				.item( Dart.class, "dart" )
				.item( StoneOfIntuition.class, "Stone of Intuition" )
				.item( TrinketCatalyst.class, "trinket catalyst" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_ALCHEMY_POT, "alchemy laboratory" );

		book.page( "sewers_f4" )
				.mob( GreatCrab.class, "great crab" ).primary()
				.mob( Goo.class, "Goo" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" )
				.item( PotionOfStrength.class, "Potion of Strength" )
				.item( Stylus.class, "stylus" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_ALCHEMY_POT, "laboratory" )
				.mob( Ghost.class, "ghost" )
				.buff( Invisibility.class, "invisible" )
				.buff( Paralysis.class, "paralysed" )
				.item( DriedRose.class, "Dried Rose" )
				.mob( Crab.class, "crab" )
				.mob( Slime.class, "slime" )
				.mob( SlimeBrown.class, "brown slime", "brown" )
				.mob( SlimeRed.class, "red slime", "red one" )
				.buff( Poison.class, "poison" )
				.mob( Thief.class, "crazy thief" );

		book.page( "sewers_f5" )
				.mob( Goo.class, "Goo" ).primary()
				.water( Assets.Environment.WATER_SEWERS, "water", "pool" )
				.item( WornKey.class, "worn key" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.LOCKED_EXIT, "stairs down" )
				.terrain( Assets.Environment.FROZEN_SEWERS, 0, "ice" )
				.mob( RatKing.class, "Rat King" )
				.item( Gold.class, "gold" )
				.item( KingsCrown.class, "King's Crown" )
				.mob( DwarfKing.class, "Dwarf King" )
				.custom( () -> new HeroIcon( new Ratmogrify() ), "Ratmogrify" );

		book.page( "boss_goo" )
				.mob( Goo.class, "Goo" )
				.water( Assets.Environment.WATER_SEWERS, "water" )
				.mob( PoisonGoo.class, "poison goo" )
				.buff( Poison.class, "poison" )
				.item( PotionOfMending.class, "Potion of Mending" )
				//the black drops it pumps out while it charges
				.effect( GuideEffect.GOO, "charge", "charging" )
				.buff( Ooze.class, "ooze" )
				.buff( LockedFloor.class, "sealed floor", "floor is sealed" )
				.item( WornKey.class, "worn key" )
				.item( GooBlob.class, "goo blob" )
				.item( Sokoban1.class, "Sokoban Practice page" )
				.item( OtilukesJournal.class, "Otiluke's Journal" )
				.item( ActiveMrDestructo.class, "Mr Destructo" )
				.item( Chainsaw.class, "chainsaw hand" )
				.item( Dewdrop.class, "dew" )
				.item( Waterskin.class, "waterskin" );
	}

	private static void prison( CiteBook book ){
		book.page( "prison_overview" )
				.mob( Skeleton.class, "skeleton" )
				.mob( Guard.class, "prison guard" )
				.mob( DM100.class, "DM-100" )
				.mob( Necromancer.class, "necromancer" )
				.mob( Shopkeeper.class, "shop" )
				.custom( () -> branchStairs( NEST_STAIRS ), "spider nest", "web-covered hole" )
				.icon( Icons.DEPTH_SECRETS, "secret room" )
				.mob( Tengu.class, "Tengu" )
				.water( Assets.Environment.WATER_PRISON, "water" )
				.effect( GuideEffect.FIRE, "fire" )
				.buff( Ooze.class, "ooze" )
				.buff( Frost.class, "frost" )
				.trap( ChillingTrap.class, "chilling trap", "chilling" )
				.trap( ShockingTrap.class, "shocking trap", "shocking" )
				.trap( ToxicTrap.class, "toxic gas trap", "toxic gas" )
				.trap( BurningTrap.class, "burning trap", "burning" )
				.trap( PoisonDartTrap.class, "poison dart trap", "poison dart" )
				.trap( AlarmTrap.class, "alarm trap", "alarm" )
				.trap( GrippingTrap.class, "gripping trap", "grip" )
				.trap( TeleportationTrap.class, "teleportation trap", "teleport" )
				.mob( Wraith.class, "wraith" )
				.mob( SpectralNecromancer.class, "spectral necromancer" );

		book.page( "prison_f1" )
				.mob( Shopkeeper.class, "shop", "shopkeeper" ).primary()
				.item( OtilukesJournal.class, "Otiluke's Journal" )
				.mob( Goo.class, "Goo" )
				.mob( Tengu.class, "Tengu" )
				.mob( DM300.class, "DM-300" )
				.item( Ankh.class, "Ankh" )
				.item( LloydsBeacon.class, "Lloyd's Beacon" )
				.mob( DwarfKing.class, "Dwarf King" )
				.mob( PortalGate.class, "portal gate" )
				.item( Gold.class, "gold" )
				.buff( Hunger.class, BuffIndicator.HUNGER, "hunger" )
				.mob( Skeleton.class, "skeleton" )
				.mob( Thief.class, "crazy thief", "thief" )
				.mob( Wandmaker.class, "wandmaker" )
				.item( AdamantWand.class, "adamantite wand" )
				.terrain( Assets.Environment.PRISON_QUEST, MASS_GRAVE, "mass grave" )
				.mob( Blacksmith2.class, "town smith" )
				.hero( HeroClass.MAGE, "mage" );

		book.page( "prison_f2" )
				.mob( Wandmaker.class, "wandmaker" ).primary()
				.mob( DM100.class, "DM-100" )
				.mob( Guard.class, "prison guard" )
				.buff( Cripple.class, "cripple" )
				.mob( FossilSkeleton.class, "fossil skeleton" )
				.mob( Zombie.class, "zombie", "Zombies" )   //"zombies" stems to "zomby"
				.mob( Assassin.class, "shinobi" )
				.terrain( Assets.Environment.PRISON_QUEST, MASS_GRAVE, "Mass grave" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.FLAT_BARRICADE, "barricade" )
				.item( PotionOfLiquidFlame.class, "liquid flame potion" )
				.item( CorpseDust.class, "corpse dust" )
				.mob( Wraith.class, "wraith" )
				.terrain( Assets.Environment.PRISON_QUEST, RITUAL_MARK, "Ritual site", "marked tile" )
				.item( CeremonialCandle.class, "candle" )
				.mob( Elemental.NewbornFireElemental.class, "newborn fire elemental" )
				.item( Embers.class, "embers" )
				.buff( Chill.class, "chill" )
				.buff( Frost.class, "frost" )
				//the garden is the tall grass its lashers hide in
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.FLAT_HIGH_GRASS, "Rot garden", "grass" )
				.mob( RotHeart.class, "rot heart", "heart" )
				.effect( GuideEffect.TOXIC_GAS, "toxic gas" )
				.mob( RotLasher.class, "rot lasher", "lasher" )
				.effect( GuideEffect.FIRE, "fire" )
				.item( Rotberry.Seed.class, "rotberry seed" );

		book.page( "prison_f3" )
				.mob( Necromancer.class, "necromancer" ).primary()
				.mob( Necromancer.NecroSkeleton.class, "skeleton" )
				.item( PotionOfHealing.class, "Potion of Healing" )
				.mob( SpectralNecromancer.class, "spectral necromancer" )
				.mob( Wraith.class, "wraith" )
				.item( ScrollOfRemoveCurse.class, "Scroll of Remove Curse" )
				.mob( FossilSkeleton.class, "fossil skeleton" )
				.mob( Assassin.class, "shinobi" )
				.mob( Wandmaker.class, "wandmaker" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.FLAT_ALCHEMY_POT, "laboratory" );

		book.page( "prison_f4" )
				.mob( MossySkeleton.class, "moss covered skeleton" ).primary()
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" )
				.item( PotionOfStrength.class, "Potion of Strength" )
				.item( Stylus.class, "stylus" )
				.mob( Tengu.class, "Tengu" )
				.mob( Wandmaker.class, "wandmaker" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.FLAT_ALCHEMY_POT, "laboratory" )
				.item( PrisonKey.class, "Prison Ancient Key" )
				.buff( Poison.class, "poison" )
				.effect( GuideEffect.TOXIC_GAS, "toxic gas" )
				//the Battle's own page card (pg_battle)
				.icon( Icons.DEPTH_GRASS, "Battle" );

		book.page( "prison_f5" )
				.mob( Tengu.class, "Tengu" ).primary()
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.FLAT_DOOR_LOCKED, "locked cell", "door" )
				.item( IronKey.class, "iron key" )
				.item( Ankh.class, "Ankh" )
				.trap( TenguDartTrap.class, "dart trap" )
				.sprite( ItemSpriteSheet.TENGU_BOMB, "bomb" )
				.sprite( ItemSpriteSheet.TENGU_SHOCKER, "lightning post" )
				.effect( GuideEffect.FIRE, "fire" )
				.icon( Icons.BADGES, "badge" )
				.item( TengusMask.class, "Tengu's Mask" )
				.item( Sokoban2.class, "Sokoban Castle page" )
				.item( OtilukesJournal.class, "Otiluke's Journal" )
				.item( LloydsBeacon.class, "Lloyd's Beacon" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.CHASM, "chasm" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.EXIT, "stairs down" )
				.mob( Shopkeeper.class, "shop" );

		book.page( "boss_tengu" )
				.mob( Tengu.class, "Tengu" )
				//the fight's two halves, as headings
				.plain( "Phase 1, his cell:", "Phase 2, an oval arena with no cover:" )
				.trap( TenguDartTrap.class, "poison dart trap" )
				//a trap being sown flashes with these specks (PrisonBossLevel.placeTrapsInTenguCell)
				.effect( GuideEffect.LIGHT, "flash" )
				.sprite( ItemSpriteSheet.TENGU_BOMB, "bomb" )
				.sprite( ItemSpriteSheet.TENGU_SHOCKER, "lightning post" )
				.effect( GuideEffect.FIRE, "fire" )
				.buff( LockedFloor.class, "sealed floor", "floor is sealed" )
				.buff( Roots.class, "roots" )
				.buff( Blindness.class, "blindness" )
				.buff( Terror.class, "terror" )
				.mob( MirrorImage.class, "mirror image" );
	}

	private static void nest( CiteBook book ){
		book.page( "nest_overview" )
				.custom( () -> branchStairs( NEST_STAIRS ), "spider nest", "web-covered hole" )
				.mob( SpiderNest.class, "egg sac" )
				.mob( SpiderServant.class, "spider servant" )
				.buff( Poison.class, "poison" )
				.mob( SpiderGuard.class, "spider guard" )
				.buff( Paralysis.class, "paralyse" )
				.mob( SpiderExploding.class, "bloated spider" )
				.mob( SpiderMind.class, "spider mind" )
				.mob( SpiderQueen.class, "spider queen" );

		book.page( "boss_spiderqueen" )
				.mob( SpiderQueen.class, "spider queen" )
				.buff( Poison.class, "poison" )
				.effect( GuideEffect.FIRE, "fire" )
				.buff( LockedFloor.class, "sealed floor", "floor seal" )
				.mob( SpiderEgg.class, "egg" )
				.mob( SpiderServant.class, "spider servant", "spider", "brood" )
				.item( PotionOfHealing.class, "Potion of Healing" )
				.sprite( ItemSpriteSheet.WAND_HOLDER, "wand" )
				.item( SpiderCharm.class, "spider charm" );
	}

	private static void caves( CiteBook book ){
		book.page( "caves_overview" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.FLAT_WALL_DECO, "ore vein" )
				.item( DarkGold.class, "dark gold" )
				//the mine's own page card (caves_mine)
				.icon( Icons.DEPTH_SECRETS, "Blacksmith's Mine" )
				.mob( Shopkeeper.class, "shop" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.CHASM, "chasm" )
				.buff( Cripple.class, "crippled" )
				.buff( Bleeding.class, "bleeding" )
				.effect( GuideEffect.BLIZZARD, "snow" )
				.mob( RedWraith.class, "red wraith" )
				.trap( BurningTrap.class, "fire trap", "fire" )
				.trap( PoisonDartTrap.class, "poison dart trap", "poison dart" )
				.trap( FrostTrap.class, "frost trap", "frost" )
				.trap( StormTrap.class, "storm trap", "storm" )
				.trap( CorrosionTrap.class, "corrosion trap", "corrosion" )
				.trap( PitfallTrap.class, "pitfall trap", "pitfall" );

		book.page( "caves_f1" )
				.mob( Brute.class, "gnoll brute" ).primary()
				.mob( Bat.class, "vampire bat" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.FLAT_DOOR, "doorway" )
				.buff( Slow.class, "slow" )
				.item( PotionOfMending.class, "Potion of Mending" )
				.buff( Brute.BruteRage.class, "rage", "enrage" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.CHASM, "chasm" )
				.buff( Terror.class, "terror" )
				.mob( ArmoredBrute.class, "armored brute" )
				.mob( Shaman.RedShaman.class, "gnoll shaman" )
				.buff( Weakness.class, "weaken" )
				.buff( Vulnerable.class, "vulnerable" )
				.buff( Hex.class, "hex" )
				.mob( Shopkeeper.class, "shop" )
				.item( Mushroom.class, "toadstool mushroom" )
				.mob( Blacksmith.class, "blacksmith" );

		book.page( "caves_f2" )
				.mob( Blacksmith.class, "troll blacksmith" ).primary()
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.PEDESTAL, "pedestal" )
				.item( Pickaxe.class, "Pickaxe" )
				.item( DarkGold.class, "dark gold" )
				.item( Food.class, "food" )
				.item( SanChikarah.class, "SanChikarah" )
				.hero( HeroClass.DUELIST, "duelist" )
				.mob( Crab.class, "crab" )
				.mob( Scorpio.class, "scorpion" )
				.mob( Bee.class, "bee" )
				.mob( Swarm.class, "swarm" )
				.buff( Vulnerable.class, "vulnerable" )
				.mob( Spinner.class, "cave spinner", "spinner" )
				.buff( Poison.class, "poison" )
				.mob( BrokenRobot.class, "broken robot" );

		book.page( "caves_mine" )
				.item( DarkGold.class, "dark gold" ).primary()
				.item( Pickaxe.class, "Pickaxe" )
				.buff( Hunger.class, BuffIndicator.HUNGER, "hunger" )
				.terrain( Assets.Environment.TILES_CAVES_CRYSTAL, DungeonTileSheet.FLAT_MINE_CRYSTAL, "crystal" )
				.terrain( Assets.Environment.TILES_CAVES_GNOLL, DungeonTileSheet.FLAT_MINE_BOULDER, "boulder" )
				.item( Gold.class, "gold" )
				.item( Food.class, "food" )
				.icon( Icons.DEPTH_SECRETS, "secret room" )
				//the three kinds of mine, as headings: their creatures are cited by name
				.plain( "Gnolls", "Fungi" )
				.mob( GnollSapper.class, "gnoll sapper" )
				.mob( GnollGuard.class, "gnoll guard" )
				.effect( GuideEffect.ROCKS, "rockfall" )
				.terrain( Assets.Environment.TILES_CAVES_GNOLL, DungeonTileSheet.FLAT_BARRICADE, "barricade" )
				.mob( GnollGeomancer.class, "gnoll geomancer" )
				.mob( CrystalGuardian.class, "crystal guardian" )
				.mob( CrystalSpire.class, "crystal spire" )
				.buff( Cripple.class, "cripple" )
				.mob( FungalSentry.class, "fungal sentry" )
				.buff( Poison.class, "poison" )
				.mob( FungalSpinner.class, "fungal spinner" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.FLAT_HIGH_GRASS, "tall grass" )
				.mob( FungalCore.class, "fungal core" );

		book.page( "caves_f3" )
				.mob( DM200.class, "DM-200" ).primary()
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.FLAT_DOOR, "doorway" )
				.effect( GuideEffect.TOXIC_GAS, "toxic gas", "gas" )
				.mob( BrokenRobot.class, "broken robot" )
				.item( ScrollOfRecharging.class, "Scroll of Recharging" )
				.mob( DM201.class, "DM-201" )
				.effect( GuideEffect.CORROSIVE_GAS, "caustic gas" )
				.item( MetalShard.class, "metal shard" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.FLAT_ALCHEMY_POT, "laboratory" )
				.mob( Blacksmith.class, "blacksmith" );

		book.page( "caves_f4" )
				.mob( ClayGolem.class, "clay golem" ).primary()
				.mob( DM200.class, "DM-200" )
				.buff( Terror.class, "terror" )
				.buff( MagicalSleep.class, "sleep" )
				.mob( Ghoul.class, "ghoul" )
				.mob( Blacksmith.class, "troll blacksmith", "smith" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.FLAT_ALCHEMY_POT, "laboratory" )
				.item( PotionOfStrength.class, "Potion of Strength" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" )
				.item( Stylus.class, "stylus" )
				.custom( () -> branchStairs( TEMPLE_STAIRS ), "temple" )
				.item( CavesKey.class, "Caves Ancient Key" )
				.mob( Piranha.class, "giant piranha", "piranha" );

		book.page( "temple_overview" )
				.custom( () -> branchStairs( TEMPLE_STAIRS ), "stair" )
				.terrain( Assets.Environment.TILES_TEMPLE, DungeonTileSheet.FLAT_HIGH_GRASS, "grass" )
				.mob( Mimic.class, "mimic" )
				.water( Assets.Environment.WATER_TEMPLE, "pool", "water" )
				.mob( Piranha.class, "piranha" )
				.mob( TempleSentry.class, "temple sentry", "sentry" )
				.sprite( ItemSpriteSheet.CRYSTAL_CHEST, "chest" )
				.item( CrystalKey.class, "crystal key" )
				.trap( ExplosiveTrap.class, "explosive trap" )
				.item( StoneOfBlink.class, "Stone of Blink" )
				.mob( GnollGuard.class, "gnoll guard" )
				.effect( GuideEffect.CONFUSION_GAS, "confusion vent" )
				.effect( GuideEffect.STORM_CLOUD, "thundercloud" )
				.item( Food.class, "food" )
				.item( EnergyCrystal.class, "energy crystal" )
				//the remains every room's middle holds
				.sprite( ItemSpriteSheet.BONES, "skeleton" )
				.item( Gold.class, "gold" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" );

		book.page( "caves_f5" )
				.mob( Pylon.class, "power pylon", "pylon" ).primary()
				.terrain( Assets.Environment.CAVES_BOSS, WIRES, "exposed wires", "wire" )
				.mob( DM300.class, "DM-300" )
				.buff( Barrier.class, "shield" )
				.water( Assets.Environment.WATER_CAVES, "water" )
				.terrain( Assets.Environment.CAVES_BOSS, GATE, "gate" )
				.terrain( Assets.Environment.TILES_CAVES, DungeonTileSheet.EXIT, "stairs" );

		book.page( "boss_dm300" )
				.mob( DM300.class, "DM-300" )
				.custom( EarlyCites::supercharged, "supercharge" )
				.mob( Pylon.class, "power pylon", "pylon" )
				.terrain( Assets.Environment.CAVES_BOSS, WIRES, "exposed wires", "wire" )
				.water( Assets.Environment.WATER_CAVES, "water" )
				.effect( GuideEffect.TOXIC_GAS, "Toxic gas" )
				.effect( GuideEffect.ROCKS, "Rockfall", "rock" )
				.buff( Paralysis.class, "stun" )
				.mob( Tower.class, "tower" )
				.mob( BrokenRobot.class, "broken robot" )
				.buff( MagicalSleep.class, "sleep" )
				.buff( Terror.class, "terror" )
				.buff( Charm.class, "charm" )
				.buff( Vertigo.class, "vertigo" )
				.buff( Cripple.class, "cripple" )
				.buff( Chill.class, "chill" )
				.buff( Frost.class, "frost" )
				.buff( Roots.class, "roots" )
				.buff( Slow.class, "slow" )
				.item( MetalShard.class, "metal shard" )
				.item( Sokoban3.class, "Sokoban Portals page" )
				.item( OtilukesJournal.class, "Otiluke's Journal" )
				.item( LloydsBeacon.class, "Lloyd's Beacon" )
				.icon( Icons.BADGES, "badge" );
	}

	//one of the branch stairways' looks, as WarpedUpdates draws them
	private static Image branchStairs( int frame ){
		Image i = new Image( Assets.Environment.BRANCH_STAIRS );
		i.frame( i.texture.uvRectBySize( 16 * frame, 0, 16, 16 ) );
		return i;
	}

	//DM-300 in its charged colours, alive
	private static Image supercharged(){
		CharSprite s = GuidePuppet.make( DM300.class );
		if (s == null) return null;
		((DM300Sprite) s).updateChargeState( true );
		return new LiveCrop( s, 18, 18, 3.5f, 7f );
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Crouching;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Foresight;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Shaman;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Warlock;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.FortuneTellerFolk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.Mercenary;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.items.Amulet;
import xyz.gabriwar.warpedpixeldungeon.items.Ankh;
import xyz.gabriwar.warpedpixeldungeon.items.BulletBelt;
import xyz.gabriwar.warpedpixeldungeon.items.EnergyCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.Fertilizer;
import xyz.gabriwar.warpedpixeldungeon.items.KingsCrown;
import xyz.gabriwar.warpedpixeldungeon.items.LostBackpack;
import xyz.gabriwar.warpedpixeldungeon.items.Stylus;
import xyz.gabriwar.warpedpixeldungeon.items.TengusMask;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.food.Pasty;
import xyz.gabriwar.warpedpixeldungeon.items.food.SmallRation;
import xyz.gabriwar.warpedpixeldungeon.items.keys.CrystalKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.GoldenKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfExperience;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMight;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfSating;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfSearching;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfIdentify;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfEnchantment;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfAugmentation;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfEnchantment;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfIntuition;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.TrinketCatalyst;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greatsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shovel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WornShortsword;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WornDartTrap;
import xyz.gabriwar.warpedpixeldungeon.plants.BlandfruitBush;
import xyz.gabriwar.warpedpixeldungeon.plants.Firebloom;
import xyz.gabriwar.warpedpixeldungeon.plants.Rotberry;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;

//Cites for the first chapter, the basics (ch_basics: the tome of dungeon mastery's subjects).
public final class BasicsCites {

	private BasicsCites(){}

	public static void register( CiteBook book ){

		book.page( "basics_intro" )
				.sprite( ItemSpriteSheet.GUIDE_PAGE, "torn page", "page" ).primary()
				.icon( Icons.JOURNAL, "journal button" )
				.icon( Icons.MAGNIFY, "magnifier" );

		book.page( "basics_examining" )
				.icon( Icons.MAGNIFY, "magnifier", "examine key" )
				.mob( Snake.class, "sewer snake" ).primary()
				.plant( Firebloom.class, "plant" )
				.trap( WornDartTrap.class, "trap" )
				.icon( Icons.BUFFS, "buff" );

		book.page( "basics_searching" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_DOOR, "secret door" )
				.trap( WornDartTrap.class, "hidden trap" )
				.icon( Icons.MAGNIFY, "magnifier" )
				.hero( HeroClass.ROGUE, "rogue" )
				.item( RingOfSearching.class, "Ring of Searching" )
				.buff( Foresight.class, "foresight" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_BOOKSHELF, "bookshelf", "bookshelves" );

		book.page( "basics_surprise" )
				.mob( Snake.class, "sewer snake" ).primary()
				.buff( Invisibility.class, "invisible" )
				.buff( Paralysis.class, "paralysed" )
				.item( Flail.class, "flail" )
				.hero( HeroClass.ROGUE, "rogue" );

		book.page( "basics_positioning" )
				//a stretch of floor, no thing of its own
				.plain( "corridor" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_DOOR, "door", "doorway" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_HIGH_GRASS, "tall grass" )
				.hero( HeroClass.HUNTRESS, "huntress" )
				.buff( Crouching.class, "crouch" )
				.water( Assets.Environment.WATER_SEWERS, "water" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.CHASM, "chasm" )
				.trap( WornDartTrap.class, "trap" );

		book.page( "basics_hunger" )
				//its own icon is none while you are fed: the one it shows once you are hungry
				.buff( Hunger.class, BuffIndicator.HUNGER, "hunger", "hungry" )
				.buff( Hunger.class, BuffIndicator.STARVATION, "starving" )
				.item( RingOfSating.class, "Ring of Sating" )
				.item( Food.class, "ration of food", "meal" ).primary()
				.item( Pasty.class, "pasty" )
				.item( SmallRation.class, "small food ration" )
				.buff( Sleepiness.class, BuffIndicator.SLEEPINESS, "drowsy" );

		book.page( "basics_identifying" )
				.icon( Icons.CATALOG, "catalog" )
				.item( ScrollOfIdentify.class, "Scroll of Identify" ).primary()
				.item( StoneOfIntuition.class, "Stone of Intuition" )
				.item( ScrollOfRemoveCurse.class, "Scroll of Remove Curse" )
				.mob( FortuneTellerFolk.class, "fortune teller" );

		book.page( "basics_strength" )
				//your own number, no thing of its own
				.plain( "strength" )
				.item( PotionOfStrength.class, "Potion of Strength" ).primary()
				.item( Rotberry.Seed.class, "Rotberry seed" )
				.item( PotionOfMight.class, "Potion of Might" )
				.item( WornShortsword.class, "tier 1" )
				.item( Greatsword.class, "tier 5" );

		book.page( "basics_upgrades" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" ).primary()
				.sprite( ItemSpriteSheet.WAND_HOLDER, "wand" )
				.sprite( ItemSpriteSheet.RING_HOLDER, "ring" )
				.sprite( ItemSpriteSheet.ARTIFACT_HOLDER, "artifact" )
				.item( StoneOfAugmentation.class, "Stone of Augmentation" )
				.icon( Icons.TEMPERING_FORGE, "tempering forge" );

		book.page( "basics_levelling" )
				.icon( Icons.TALENT, "talents" )
				.item( PotionOfExperience.class, "Potion of Experience" );

		book.page( "basics_skills" )
				//a pool of your own, no thing of its own
				.plain( "mana" )
				.icon( Icons.TALENT, "talent" )
				.icon( Icons.BUFFS, "skill" )
				.item( TengusMask.class, "Tengu's mask" )
				.item( KingsCrown.class, "Dwarf King's crown" )
				.mob( Mercenary.class, "mercenary" );

		book.page( "basics_magic" )
				.item( WandOfMagicMissile.class, "wand" ).primary()
				.mob( Shaman.RedShaman.class, "gnoll shaman" )
				.mob( Warlock.class, "dwarf warlock" );

		book.page( "basics_looting" )
				.item( PotionOfStrength.class, "Potion of Strength" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" )
				.item( Stylus.class, "arcane stylus", "stylus" )
				.item( BulletBelt.class, "bullet belt" )
				.item( IronKey.class, "iron key" )
				.item( GoldenKey.class, "golden key" )
				.sprite( ItemSpriteSheet.LOCKED_CHEST, "locked chest" )
				.item( CrystalKey.class, "crystal key" ).primary()
				.sprite( ItemSpriteSheet.CRYSTAL_CHEST, "crystal chest" )
				.mob( Mimic.class, "mimic" );

		book.page( "basics_alchemy" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_ALCHEMY_POT, "alchemy pot", "pot" )
				.item( EnergyCrystal.class, "energy crystal" )
				.sprite( ItemSpriteSheet.ALCH_PAGE, "alchemy guide" )
				.item( TrinketCatalyst.class, "trinket catalyst", "catalyst" ).primary()
				.sprite( ItemSpriteSheet.TRINKET_HOLDER, "trinket" );

		book.page( "basics_enchanting" )
				//an enchantment and a glyph have no art of their own: gear in their glow, as the catalog draws it
				.custom( () -> new ItemSprite( ItemSpriteSheet.SWORD, new Blazing().glowing() ), "enchantment" )
				.custom( () -> new ItemSprite( ItemSpriteSheet.ARMOR_LEATHER, new Brimstone().glowing() ), "glyph" )
				.mob( EnchantingStation.class, "enchanting pedestal", "pedestal" ).primary()
				//the pedestal's three works, no things of their own
				.plain( "weave", "empower", "reroll" )
				.item( Stylus.class, "arcane stylus", "stylus" )
				.item( ScrollOfEnchantment.class, "Scroll of Enchantment" )
				.item( StoneOfEnchantment.class, "Stone of Enchantment" );

		book.page( "basics_farming" )
				//the build menu's own pictures of the farm plot and the fence
				.custom( () -> BuildCatalog.byId( "farm_plot" ).preview(), "farmland", "farm plot" )
				.item( Shovel.class, "shovel" )
				.item( BlandfruitBush.Seed.class, "seed" ).primary()
				.plant( BlandfruitBush.class, "crop", "blandfruit bush" )
				.item( Sickle.class, "sickle" )
				.item( Blandfruit.class, "blandfruit" )
				.custom( () -> BuildCatalog.byId( "fence" ).preview(), "fence" )
				.item( Fertilizer.class, "plant food" );

		book.page( "basics_defeat" )
				.sprite( ItemSpriteSheet.TOMB, "death" ).primary()
				.icon( Icons.RANKINGS, "rankings" )
				.icon( Icons.BADGES, "badges" )
				.sprite( ItemSpriteSheet.REMAINS, "remains" )
				.item( Ankh.class, "ankh" )
				.item( LostBackpack.class, "pack" )
				.item( Amulet.class, "Amulet of Yendor" );
	}
}

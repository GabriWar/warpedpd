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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.TextureFilm;

public class ItemSpriteSheet {

	public static final int SIZE = 16;

	private static final int WIDTH = 256 / SIZE;

	public static TextureFilm film = new TextureFilm( Assets.Sprites.ITEMS, SIZE, SIZE );

	private static int xy(int x, int y){
		x -= 1; y -= 1;
		return x + WIDTH*y;
	}

	private static void assignItemRect( int item, int width, int height ){
		int x = (item % WIDTH) * SIZE;
		int y = (item / WIDTH) * SIZE;
		film.add( item, x, y, x+width, y+height);
	}

	private static final int PLACEHOLDERS        =                               xy(1, 1);   //18 slots
	public static final int SOMETHING       = PLACEHOLDERS+0;
	public static final int WEAPON_HOLDER   = PLACEHOLDERS+1;
	public static final int ARMOR_HOLDER    = PLACEHOLDERS+2;
	public static final int MISSILE_HOLDER  = PLACEHOLDERS+3;
	public static final int WAND_HOLDER     = PLACEHOLDERS+4;
	public static final int RING_HOLDER     = PLACEHOLDERS+5;
	public static final int ARTIFACT_HOLDER = PLACEHOLDERS+6;
	public static final int TRINKET_HOLDER  = PLACEHOLDERS+7;
	public static final int FOOD_HOLDER     = PLACEHOLDERS+8;
	public static final int BOMB_HOLDER     = PLACEHOLDERS+9;
	public static final int POTION_HOLDER   = PLACEHOLDERS+10;
	public static final int SEED_HOLDER     = PLACEHOLDERS+11;
	public static final int SCROLL_HOLDER   = PLACEHOLDERS+12;
	public static final int STONE_HOLDER    = PLACEHOLDERS+13;
	public static final int ELIXIR_HOLDER   = PLACEHOLDERS+14;
	public static final int SPELL_HOLDER    = PLACEHOLDERS+15;
	public static final int MOB_HOLDER      = PLACEHOLDERS+16;
	public static final int DOCUMENT_HOLDER = PLACEHOLDERS+17;
	static{
		assignItemRect(SOMETHING      ,  8, 13);
		assignItemRect(WEAPON_HOLDER  , 14, 14);
		assignItemRect(ARMOR_HOLDER   , 14, 12);
		assignItemRect(MISSILE_HOLDER , 15, 15);
		assignItemRect(WAND_HOLDER    , 14, 14);
		assignItemRect(RING_HOLDER    ,  8, 10);
		assignItemRect(ARTIFACT_HOLDER, 15, 15);
		assignItemRect(TRINKET_HOLDER , 16, 11);
		assignItemRect(FOOD_HOLDER    , 15, 11);
		assignItemRect(BOMB_HOLDER    , 10, 13);
		assignItemRect(POTION_HOLDER  , 12, 14);
		assignItemRect(SEED_HOLDER    , 10, 10);
		assignItemRect(SCROLL_HOLDER  , 15, 14);
		assignItemRect(STONE_HOLDER   , 14, 12);
		assignItemRect(ELIXIR_HOLDER  , 12, 14);
		assignItemRect(SPELL_HOLDER   ,  8, 16);
		assignItemRect(MOB_HOLDER     , 15, 14);
		assignItemRect(DOCUMENT_HOLDER, 10, 11);
	}

	private static final int UNCOLLECTIBLE       =                               xy(3, 2);   //14 slots
	public static final int GOLD              = UNCOLLECTIBLE+0;
	public static final int ENERGY            = UNCOLLECTIBLE+1;
	public static final int DEWDROP           = UNCOLLECTIBLE+3;
	public static final int PETAL             = UNCOLLECTIBLE+4;
	public static final int SANDBAG           = UNCOLLECTIBLE+5;
	public static final int SPIRIT_ARROW      = UNCOLLECTIBLE+6;
	public static final int TENGU_BOMB        = UNCOLLECTIBLE+8;
	public static final int TENGU_SHOCKER     = UNCOLLECTIBLE+9;
	public static final int GEO_BOULDER       = UNCOLLECTIBLE+10;
	public static final int UPGRADEGOO_YELLOW = UNCOLLECTIBLE+11;
	public static final int UPGRADEGOO_RED    = UNCOLLECTIBLE+12;
	public static final int UPGRADEGOO_VIOLET = UNCOLLECTIBLE+13;
	static{
		assignItemRect(GOLD             , 15, 13);
		assignItemRect(ENERGY           , 16, 16);
		assignItemRect(DEWDROP          , 10, 10);
		assignItemRect(PETAL            ,  8,  8);
		assignItemRect(SANDBAG          , 10, 10);
		assignItemRect(SPIRIT_ARROW     , 11, 11);
		assignItemRect(TENGU_BOMB       , 10, 10);
		assignItemRect(TENGU_SHOCKER    , 10, 10);
		assignItemRect(GEO_BOULDER      , 16, 14);
		assignItemRect(UPGRADEGOO_YELLOW, 14, 11);
		assignItemRect(UPGRADEGOO_RED   , 14, 11);
		assignItemRect(UPGRADEGOO_VIOLET, 14, 11);
	}

	private static final int CONTAINERS          =                               xy(1, 3);   //16 slots
	public static final int BONES               = CONTAINERS+0;
	public static final int REMAINS             = CONTAINERS+1;
	public static final int TOMB                = CONTAINERS+2;
	public static final int GRAVE               = CONTAINERS+3;
	public static final int CHEST               = CONTAINERS+4;
	public static final int LOCKED_CHEST        = CONTAINERS+5;
	public static final int CRYSTAL_CHEST       = CONTAINERS+6;
	public static final int EBONY_CHEST         = CONTAINERS+7;
	public static final int STEEL_HONEYPOT_ITEM = CONTAINERS+8;
	public static final int STEEL_SHATTPOT_ITEM = CONTAINERS+9;
	static{
		assignItemRect(BONES              , 14, 11);
		assignItemRect(REMAINS            , 14, 11);
		assignItemRect(TOMB               , 14, 15);
		assignItemRect(GRAVE              , 14, 15);
		assignItemRect(CHEST              , 16, 14);
		assignItemRect(LOCKED_CHEST       , 16, 14);
		assignItemRect(CRYSTAL_CHEST      , 16, 14);
		assignItemRect(EBONY_CHEST        , 16, 14);
		assignItemRect(STEEL_HONEYPOT_ITEM, 14, 12);
		assignItemRect(STEEL_SHATTPOT_ITEM, 14, 12);
	}

	private static final int MISC_CONSUMABLE     =                               xy(1, 4);   //32 slots
	public static final int ANKH              = MISC_CONSUMABLE+0;
	public static final int STYLUS            = MISC_CONSUMABLE+1;
	public static final int SEAL              = MISC_CONSUMABLE+2;
	public static final int TORCH             = MISC_CONSUMABLE+3;
	public static final int BEACON            = MISC_CONSUMABLE+4;
	public static final int HONEYPOT          = MISC_CONSUMABLE+5;
	public static final int SHATTPOT          = MISC_CONSUMABLE+6;
	public static final int IRON_KEY          = MISC_CONSUMABLE+7;
	public static final int GOLDEN_KEY        = MISC_CONSUMABLE+8;
	public static final int CRYSTAL_KEY       = MISC_CONSUMABLE+9;
	public static final int WORN_KEY          = MISC_CONSUMABLE+10;
	public static final int MASK              = MISC_CONSUMABLE+11;
	public static final int CROWN             = MISC_CONSUMABLE+12;
	public static final int AMULET            = MISC_CONSUMABLE+13;
	public static final int MASTERY           = MISC_CONSUMABLE+14;
	public static final int KIT               = MISC_CONSUMABLE+15;
	public static final int SEAL_SHARD        = MISC_CONSUMABLE+16;
	public static final int BROKEN_STAFF      = MISC_CONSUMABLE+17;
	public static final int CLOAK_SCRAP       = MISC_CONSUMABLE+18;
	public static final int BOW_FRAGMENT      = MISC_CONSUMABLE+19;
	public static final int BROKEN_HILT       = MISC_CONSUMABLE+20;
	public static final int TORN_PAGE         = MISC_CONSUMABLE+21;
	public static final int TRINKET_CATA      = MISC_CONSUMABLE+22;
	public static final int LIQUID_METAL      = MISC_CONSUMABLE+23;
	public static final int OTILUKES_SPECS    = MISC_CONSUMABLE+24;
	public static final int TOWEL_ITEM        = MISC_CONSUMABLE+25;
	public static final int DWARF_HAMMER      = MISC_CONSUMABLE+26;
	public static final int WHISTLE_ITEM      = MISC_CONSUMABLE+27;
	public static final int SPROUTED_BEACON   = MISC_CONSUMABLE+28;
	public static final int TOWN_BEACON       = MISC_CONSUMABLE+29;
	public static final int ANCIENT_KEY       = MISC_CONSUMABLE+30;
	public static final int ANCIENT_COIN_ITEM = MISC_CONSUMABLE+31;
	static{
		assignItemRect(ANKH             , 10, 16);
		assignItemRect(STYLUS           , 12, 13);
		assignItemRect(SEAL             , 13, 13);
		assignItemRect(TORCH            , 12, 15);
		assignItemRect(BEACON           , 16, 15);
		assignItemRect(HONEYPOT         , 14, 12);
		assignItemRect(SHATTPOT         , 14, 12);
		assignItemRect(IRON_KEY         ,  8, 14);
		assignItemRect(GOLDEN_KEY       ,  8, 14);
		assignItemRect(CRYSTAL_KEY      ,  8, 14);
		assignItemRect(WORN_KEY         ,  8, 14);
		assignItemRect(MASK             , 11,  9);
		assignItemRect(CROWN            , 13,  7);
		assignItemRect(AMULET           , 16, 16);
		assignItemRect(MASTERY          , 13, 16);
		assignItemRect(KIT              , 16, 15);
		assignItemRect(SEAL_SHARD       , 12, 12);
		assignItemRect(BROKEN_STAFF     , 14, 10);
		assignItemRect(CLOAK_SCRAP      ,  9,  9);
		assignItemRect(BOW_FRAGMENT     , 12,  9);
		assignItemRect(BROKEN_HILT      ,  9,  9);
		assignItemRect(TORN_PAGE        , 11, 13);
		assignItemRect(TRINKET_CATA     , 12, 11);
		assignItemRect(LIQUID_METAL     ,  8, 15);
		assignItemRect(OTILUKES_SPECS   , 15,  8);
		assignItemRect(TOWEL_ITEM       , 14, 16);
		assignItemRect(DWARF_HAMMER     , 15, 15);
		assignItemRect(WHISTLE_ITEM     ,  8, 16);
		assignItemRect(SPROUTED_BEACON  , 16, 15);
		assignItemRect(TOWN_BEACON      , 16, 15);
		assignItemRect(ANCIENT_KEY      ,  8, 14);
		assignItemRect(ANCIENT_COIN_ITEM, 10, 10);
	}

	private static final int BOMBS               =                               xy(1, 6);   //32 slots
	public static final int BOMB                 = BOMBS+0;
	public static final int DBL_BOMB             = BOMBS+1;
	public static final int FIRE_BOMB            = BOMBS+2;
	public static final int FROST_BOMB           = BOMBS+3;
	public static final int REGROWTH_BOMB        = BOMBS+4;
	public static final int SMOKE_BOMB           = BOMBS+5;
	public static final int FLASHBANG            = BOMBS+6;
	public static final int HOLY_BOMB            = BOMBS+7;
	public static final int WOOLY_BOMB           = BOMBS+8;
	public static final int NOISEMAKER           = BOMBS+9;
	public static final int ARCANE_BOMB          = BOMBS+10;
	public static final int SHRAPNEL_BOMB        = BOMBS+11;
	public static final int DUMPLING_BOMB        = BOMBS+12;
	public static final int HOLY_HAND_GRENADE    = BOMBS+13;
	public static final int CLUSTER_BOMB         = BOMBS+14;
	public static final int DIZZY_BOMB           = BOMBS+15;
	public static final int SMART_BOMB           = BOMBS+16;
	public static final int SEEKING_BOMB         = BOMBS+17;
	public static final int SEEKING_CLUSTER_BOMB = BOMBS+18;
	static{
		assignItemRect(BOMB                , 10, 13);
		assignItemRect(DBL_BOMB            , 14, 13);
		assignItemRect(FIRE_BOMB           , 13, 12);
		assignItemRect(FROST_BOMB          , 13, 12);
		assignItemRect(REGROWTH_BOMB       , 13, 12);
		assignItemRect(SMOKE_BOMB          , 13, 12);
		assignItemRect(FLASHBANG           , 10, 13);
		assignItemRect(HOLY_BOMB           , 10, 13);
		assignItemRect(WOOLY_BOMB          , 10, 13);
		assignItemRect(NOISEMAKER          , 10, 13);
		assignItemRect(ARCANE_BOMB         , 10, 13);
		assignItemRect(SHRAPNEL_BOMB       , 10, 13);
		assignItemRect(DUMPLING_BOMB       , 15, 12);
		assignItemRect(HOLY_HAND_GRENADE   , 10, 13);
		assignItemRect(CLUSTER_BOMB        , 10, 13);
		assignItemRect(DIZZY_BOMB          , 10, 13);
		assignItemRect(SMART_BOMB          , 10, 13);
		assignItemRect(SEEKING_BOMB        , 10, 13);
		assignItemRect(SEEKING_CLUSTER_BOMB, 10, 13);
	}

	private static final int WEP_TIER1           =                               xy(1, 8);   //8 slots
	public static final int WORN_SHORTSWORD = WEP_TIER1+0;
	public static final int CUDGEL          = WEP_TIER1+1;
	public static final int GLOVES          = WEP_TIER1+2;
	public static final int RAPIER          = WEP_TIER1+3;
	public static final int DAGGER          = WEP_TIER1+4;
	public static final int MAGES_STAFF     = WEP_TIER1+5;
	static{
		assignItemRect(WORN_SHORTSWORD, 13, 13);
		assignItemRect(CUDGEL         , 15, 15);
		assignItemRect(GLOVES         , 12, 16);
		assignItemRect(RAPIER         , 13, 14);
		assignItemRect(DAGGER         , 12, 13);
		assignItemRect(MAGES_STAFF    , 15, 16);
	}

	private static final int WEP_TIER2           =                               xy(9, 8);   //8 slots
	public static final int SHORTSWORD   = WEP_TIER2+0;
	public static final int HAND_AXE     = WEP_TIER2+1;
	public static final int SPEAR        = WEP_TIER2+2;
	public static final int QUARTERSTAFF = WEP_TIER2+3;
	public static final int DIRK         = WEP_TIER2+4;
	public static final int SICKLE       = WEP_TIER2+5;
	static{
		assignItemRect(SHORTSWORD  , 13, 13);
		assignItemRect(HAND_AXE    , 12, 14);
		assignItemRect(SPEAR       , 16, 16);
		assignItemRect(QUARTERSTAFF, 16, 16);
		assignItemRect(DIRK        , 13, 14);
		assignItemRect(SICKLE      , 15, 15);
	}

	private static final int WEP_TIER3           =                               xy(1, 9);   //8 slots
	public static final int SWORD        = WEP_TIER3+0;
	public static final int MACE         = WEP_TIER3+1;
	public static final int SCIMITAR     = WEP_TIER3+2;
	public static final int ROUND_SHIELD = WEP_TIER3+3;
	public static final int SAI          = WEP_TIER3+4;
	public static final int WHIP         = WEP_TIER3+5;
	static{
		assignItemRect(SWORD       , 14, 14);
		assignItemRect(MACE        , 15, 15);
		assignItemRect(SCIMITAR    , 13, 16);
		assignItemRect(ROUND_SHIELD, 16, 16);
		assignItemRect(SAI         , 16, 16);
		assignItemRect(WHIP        , 14, 14);
	}

	private static final int WEP_TIER4           =                               xy(9, 9);   //8 slots
	public static final int LONGSWORD       = WEP_TIER4+0;
	public static final int BATTLE_AXE      = WEP_TIER4+1;
	public static final int FLAIL           = WEP_TIER4+2;
	public static final int RUNIC_BLADE     = WEP_TIER4+3;
	public static final int ASSASSINS_BLADE = WEP_TIER4+4;
	public static final int CROSSBOW        = WEP_TIER4+5;
	public static final int KATANA          = WEP_TIER4+6;
	static{
		assignItemRect(LONGSWORD      , 15, 15);
		assignItemRect(BATTLE_AXE     , 16, 16);
		assignItemRect(FLAIL          , 14, 14);
		assignItemRect(RUNIC_BLADE    , 14, 14);
		assignItemRect(ASSASSINS_BLADE, 14, 15);
		assignItemRect(CROSSBOW       , 15, 15);
		assignItemRect(KATANA         , 15, 16);
	}

	private static final int WEP_TIER5           =                               xy(1, 10);   //8 slots
	public static final int GREATSWORD  = WEP_TIER5+0;
	public static final int WAR_HAMMER  = WEP_TIER5+1;
	public static final int GLAIVE      = WEP_TIER5+2;
	public static final int GREATAXE    = WEP_TIER5+3;
	public static final int GREATSHIELD = WEP_TIER5+4;
	public static final int GAUNTLETS   = WEP_TIER5+5;
	public static final int WAR_SCYTHE  = WEP_TIER5+6;
	static{
		assignItemRect(GREATSWORD , 16, 16);
		assignItemRect(WAR_HAMMER , 16, 16);
		assignItemRect(GLAIVE     , 16, 16);
		assignItemRect(GREATAXE   , 12, 16);
		assignItemRect(GREATSHIELD, 12, 16);
		assignItemRect(GAUNTLETS  , 13, 15);
		assignItemRect(WAR_SCYTHE , 14, 15);
	}

	private static final int SPROUTED_MELEE      =                               xy(9, 10);   //8 slots
	public static final int ASSASSINS_KNIFE = SPROUTED_MELEE+0;
	public static final int AXE_SPROUTED    = SPROUTED_MELEE+1;
	public static final int BROAD_SWORD     = SPROUTED_MELEE+2;
	public static final int MAGE_STAFF      = SPROUTED_MELEE+4;
	public static final int SPORK           = SPROUTED_MELEE+5;
	public static final int ROYAL_SPORK     = SPROUTED_MELEE+6;
	public static final int CHAINSAW        = SPROUTED_MELEE+7;
	static{
		assignItemRect(ASSASSINS_KNIFE, 13, 13);
		assignItemRect(AXE_SPROUTED   , 12, 16);
		assignItemRect(BROAD_SWORD    , 13, 14);
		assignItemRect(MAGE_STAFF     , 15, 15);
		assignItemRect(SPORK          , 15, 15);
		assignItemRect(ROYAL_SPORK    , 15, 15);
		assignItemRect(CHAINSAW       , 13, 13);
	}

	private static final int RELIC_WEAPONS       =                               xy(1, 11);   //16 slots
	public static final int RELIC_TRIDENT    = RELIC_WEAPONS+0;
	public static final int RELIC_ARESSWORD  = RELIC_WEAPONS+1;
	public static final int RELIC_CROMAXE    = RELIC_WEAPONS+2;
	public static final int RELIC_LOKISFLAIL = RELIC_WEAPONS+3;
	public static final int JUPITERS_WRAITH  = RELIC_WEAPONS+4;
	static{
		assignItemRect(RELIC_TRIDENT   , 16, 16);
		assignItemRect(RELIC_ARESSWORD , 14, 14);
		assignItemRect(RELIC_CROMAXE   , 15, 15);
		assignItemRect(RELIC_LOKISFLAIL, 16, 14);
		assignItemRect(JUPITERS_WRAITH , 15, 15);
	}

	private static final int MISSILE_WEP         =                               xy(1, 12);   //16 slots
	public static final int SPIRIT_BOW      = MISSILE_WEP+0;
	public static final int THROWING_SPIKE  = MISSILE_WEP+1;
	public static final int THROWING_KNIFE  = MISSILE_WEP+2;
	public static final int THROWING_STONE  = MISSILE_WEP+3;
	public static final int FISHING_SPEAR   = MISSILE_WEP+4;
	public static final int SHURIKEN        = MISSILE_WEP+5;
	public static final int THROWING_CLUB   = MISSILE_WEP+6;
	public static final int THROWING_SPEAR  = MISSILE_WEP+7;
	public static final int BOLAS           = MISSILE_WEP+8;
	public static final int KUNAI           = MISSILE_WEP+9;
	public static final int JAVELIN         = MISSILE_WEP+10;
	public static final int TOMAHAWK        = MISSILE_WEP+11;
	public static final int BOOMERANG       = MISSILE_WEP+12;
	public static final int TRIDENT         = MISSILE_WEP+13;
	public static final int THROWING_HAMMER = MISSILE_WEP+14;
	public static final int FORCE_CUBE      = MISSILE_WEP+15;
	static{
		assignItemRect(SPIRIT_BOW     , 16, 16);
		assignItemRect(THROWING_SPIKE , 11, 10);
		assignItemRect(THROWING_KNIFE , 12, 13);
		assignItemRect(THROWING_STONE , 12, 10);
		assignItemRect(FISHING_SPEAR  , 11, 11);
		assignItemRect(SHURIKEN       , 12, 12);
		assignItemRect(THROWING_CLUB  , 12, 12);
		assignItemRect(THROWING_SPEAR , 13, 13);
		assignItemRect(BOLAS          , 15, 14);
		assignItemRect(KUNAI          , 15, 15);
		assignItemRect(JAVELIN        , 16, 16);
		assignItemRect(TOMAHAWK       , 13, 13);
		assignItemRect(BOOMERANG      , 14, 14);
		assignItemRect(TRIDENT        , 16, 16);
		assignItemRect(THROWING_HAMMER, 12, 12);
		assignItemRect(FORCE_CUBE     , 11, 12);
	}

	private static final int DARTS               =                               xy(1, 13);   //16 slots
	public static final int DART            = DARTS+0;
	public static final int ROT_DART        = DARTS+1;
	public static final int INCENDIARY_DART = DARTS+2;
	public static final int ADRENALINE_DART = DARTS+3;
	public static final int HEALING_DART    = DARTS+4;
	public static final int CHILLING_DART   = DARTS+5;
	public static final int SHOCKING_DART   = DARTS+6;
	public static final int POISON_DART     = DARTS+7;
	public static final int CLEANSING_DART  = DARTS+8;
	public static final int PARALYTIC_DART  = DARTS+9;
	public static final int HOLY_DART       = DARTS+10;
	public static final int DISPLACING_DART = DARTS+11;
	public static final int BLINDING_DART   = DARTS+12;
	public static final int SKULL_MISSILE   = DARTS+13;
	public static final int WAVE_MISSILE    = DARTS+14;
	static{
		assignItemRect(DART           , 15, 15);
		assignItemRect(ROT_DART       , 15, 15);
		assignItemRect(INCENDIARY_DART, 15, 15);
		assignItemRect(ADRENALINE_DART, 15, 15);
		assignItemRect(HEALING_DART   , 15, 15);
		assignItemRect(CHILLING_DART  , 15, 15);
		assignItemRect(SHOCKING_DART  , 15, 15);
		assignItemRect(POISON_DART    , 15, 15);
		assignItemRect(CLEANSING_DART , 15, 15);
		assignItemRect(PARALYTIC_DART , 15, 15);
		assignItemRect(HOLY_DART      , 15, 15);
		assignItemRect(DISPLACING_DART, 15, 15);
		assignItemRect(BLINDING_DART  , 15, 15);
		assignItemRect(SKULL_MISSILE  , 16, 11);
		assignItemRect(WAVE_MISSILE   , 12, 12);
	}

	private static final int ARMOR               =                               xy(1, 14);   //16 slots
	public static final int ARMOR_CLOTH    = ARMOR+0;
	public static final int ARMOR_LEATHER  = ARMOR+1;
	public static final int ARMOR_MAIL     = ARMOR+2;
	public static final int ARMOR_SCALE    = ARMOR+3;
	public static final int ARMOR_PLATE    = ARMOR+4;
	public static final int ARMOR_WARRIOR  = ARMOR+5;
	public static final int ARMOR_MAGE     = ARMOR+6;
	public static final int ARMOR_ROGUE    = ARMOR+7;
	public static final int ARMOR_HUNTRESS = ARMOR+8;
	public static final int ARMOR_DUELIST  = ARMOR+9;
	public static final int ARMOR_CLERIC   = ARMOR+10;
	public static final int ADAMANT_ARMOR  = ARMOR+11;
	static{
		assignItemRect(ARMOR_CLOTH   , 15, 12);
		assignItemRect(ARMOR_LEATHER , 14, 13);
		assignItemRect(ARMOR_MAIL    , 14, 12);
		assignItemRect(ARMOR_SCALE   , 14, 11);
		assignItemRect(ARMOR_PLATE   , 12, 12);
		assignItemRect(ARMOR_WARRIOR , 12, 12);
		assignItemRect(ARMOR_MAGE    , 15, 15);
		assignItemRect(ARMOR_ROGUE   , 14, 12);
		assignItemRect(ARMOR_HUNTRESS, 13, 15);
		assignItemRect(ARMOR_DUELIST , 12, 13);
		assignItemRect(ARMOR_CLERIC  , 13, 14);
		assignItemRect(ADAMANT_ARMOR , 16, 14);
	}

	private static final int WANDS               =                               xy(1, 15);   //16 slots
	public static final int WAND_MAGIC_MISSILE   = WANDS+0;
	public static final int WAND_FIREBOLT        = WANDS+1;
	public static final int WAND_FROST           = WANDS+2;
	public static final int WAND_LIGHTNING       = WANDS+3;
	public static final int WAND_DISINTEGRATION  = WANDS+4;
	public static final int WAND_PRISMATIC_LIGHT = WANDS+5;
	public static final int WAND_CORROSION       = WANDS+6;
	public static final int WAND_LIVING_EARTH    = WANDS+7;
	public static final int WAND_BLAST_WAVE      = WANDS+8;
	public static final int WAND_CORRUPTION      = WANDS+9;
	public static final int WAND_WARDING         = WANDS+10;
	public static final int WAND_REGROWTH        = WANDS+11;
	public static final int WAND_TRANSFUSION     = WANDS+12;
	public static final int ADAMANT_WAND         = WANDS+13;
	static {
		for (int i = WANDS; i < WANDS+16; i++)
			assignItemRect(i, 14, 14);
	}

	private static final int RINGS               =                               xy(1, 17);   //16 slots
	public static final int RING_GARNET     = RINGS+0;
	public static final int RING_RUBY       = RINGS+1;
	public static final int RING_TOPAZ      = RINGS+2;
	public static final int RING_EMERALD    = RINGS+3;
	public static final int RING_ONYX       = RINGS+4;
	public static final int RING_OPAL       = RINGS+5;
	public static final int RING_TOURMALINE = RINGS+6;
	public static final int RING_SAPPHIRE   = RINGS+7;
	public static final int RING_AMETHYST   = RINGS+8;
	public static final int RING_QUARTZ     = RINGS+9;
	public static final int RING_AGATE      = RINGS+10;
	public static final int RING_DIAMOND    = RINGS+11;
	public static final int ADAMANT_RING    = RINGS+12;
	public static final int RING_PEARL      = RINGS+13;
	public static final int RING_JADE       = RINGS+14;   //generated for Unleashed ring ports
	static {
		for (int i = RINGS; i < RINGS+16; i++)
			assignItemRect(i, 8, 10);
	}

	private static final int ARTIFACTS           =                               xy(1, 18);   //32 slots
	public static final int ARTIFACT_CLOAK          = ARTIFACTS+0;
	public static final int ARTIFACT_ARMBAND        = ARTIFACTS+1;
	public static final int ARTIFACT_CAPE           = ARTIFACTS+2;
	public static final int ARTIFACT_TALISMAN       = ARTIFACTS+3;
	public static final int ARTIFACT_HOURGLASS      = ARTIFACTS+4;
	public static final int ARTIFACT_TOOLKIT        = ARTIFACTS+5;
	public static final int ARTIFACT_SPELLBOOK      = ARTIFACTS+6;
	public static final int ARTIFACT_BEACON         = ARTIFACTS+7;
	public static final int ARTIFACT_CHAINS         = ARTIFACTS+8;
	public static final int ARTIFACT_HORN1          = ARTIFACTS+9;
	public static final int ARTIFACT_HORN2          = ARTIFACTS+10;
	public static final int ARTIFACT_HORN3          = ARTIFACTS+11;
	public static final int ARTIFACT_HORN4          = ARTIFACTS+12;
	public static final int ARTIFACT_CHALICE1       = ARTIFACTS+13;
	public static final int ARTIFACT_CHALICE2       = ARTIFACTS+14;
	public static final int ARTIFACT_CHALICE3       = ARTIFACTS+15;
	public static final int ARTIFACT_SANDALS        = ARTIFACTS+16;
	public static final int ARTIFACT_SHOES          = ARTIFACTS+17;
	public static final int ARTIFACT_BOOTS          = ARTIFACTS+18;
	public static final int ARTIFACT_GREAVES        = ARTIFACTS+19;
	public static final int ARTIFACT_ROSE1          = ARTIFACTS+20;
	public static final int ARTIFACT_ROSE2          = ARTIFACTS+21;
	public static final int ARTIFACT_ROSE3          = ARTIFACTS+22;
	public static final int ARTIFACT_TOME           = ARTIFACTS+23;
	public static final int ARTIFACT_KEY            = ARTIFACTS+24;
	public static final int ARTIFACT_DISINTEGRATION = ARTIFACTS+25;
	public static final int ARTIFACT_FROST          = ARTIFACTS+26;
	static{
		assignItemRect(ARTIFACT_CLOAK         ,  9, 15);
		assignItemRect(ARTIFACT_ARMBAND       , 16, 13);
		assignItemRect(ARTIFACT_CAPE          , 16, 14);
		assignItemRect(ARTIFACT_TALISMAN      , 15, 13);
		assignItemRect(ARTIFACT_HOURGLASS     , 13, 16);
		assignItemRect(ARTIFACT_TOOLKIT       , 15, 13);
		assignItemRect(ARTIFACT_SPELLBOOK     , 13, 16);
		assignItemRect(ARTIFACT_BEACON        , 16, 16);
		assignItemRect(ARTIFACT_CHAINS        , 16, 16);
		assignItemRect(ARTIFACT_HORN1         , 15, 15);
		assignItemRect(ARTIFACT_HORN2         , 15, 15);
		assignItemRect(ARTIFACT_HORN3         , 15, 15);
		assignItemRect(ARTIFACT_HORN4         , 15, 15);
		assignItemRect(ARTIFACT_CHALICE1      , 12, 15);
		assignItemRect(ARTIFACT_CHALICE2      , 12, 15);
		assignItemRect(ARTIFACT_CHALICE3      , 12, 15);
		assignItemRect(ARTIFACT_SANDALS       , 16,  6);
		assignItemRect(ARTIFACT_SHOES         , 16,  6);
		assignItemRect(ARTIFACT_BOOTS         , 16,  9);
		assignItemRect(ARTIFACT_GREAVES       , 16, 14);
		assignItemRect(ARTIFACT_ROSE1         , 14, 14);
		assignItemRect(ARTIFACT_ROSE2         , 14, 14);
		assignItemRect(ARTIFACT_ROSE3         , 14, 14);
		assignItemRect(ARTIFACT_TOME          , 14, 16);
		assignItemRect(ARTIFACT_KEY           ,  8, 16);
		assignItemRect(ARTIFACT_DISINTEGRATION, 12, 11);
		assignItemRect(ARTIFACT_FROST         , 10, 10);
	}

	private static final int TRINKETS            =                               xy(1, 20);   //32 slots
	public static final int RAT_SKULL       = TRINKETS+0;
	public static final int PARCHMENT_SCRAP = TRINKETS+1;
	public static final int PETRIFIED_SEED  = TRINKETS+2;
	public static final int EXOTIC_CRYSTALS = TRINKETS+3;
	public static final int MOSSY_CLUMP     = TRINKETS+4;
	public static final int SUNDIAL         = TRINKETS+5;
	public static final int CLOVER          = TRINKETS+6;
	public static final int TRAP_MECHANISM  = TRINKETS+7;
	public static final int MIMIC_TOOTH     = TRINKETS+8;
	public static final int WONDROUS_RESIN  = TRINKETS+9;
	public static final int EYE_OF_NEWT     = TRINKETS+10;
	public static final int SALT_CUBE       = TRINKETS+11;
	public static final int BLOOD_VIAL      = TRINKETS+12;
	public static final int OBLIVION_SHARD  = TRINKETS+13;
	public static final int CHAOTIC_CENSER  = TRINKETS+14;
	public static final int FERRET_TUFT     = TRINKETS+15;
	public static final int SPYGLASS        = TRINKETS+16;
	static{
		assignItemRect(RAT_SKULL      , 16, 11);
		assignItemRect(PARCHMENT_SCRAP, 10, 14);
		assignItemRect(PETRIFIED_SEED , 10, 10);
		assignItemRect(EXOTIC_CRYSTALS, 14, 13);
		assignItemRect(MOSSY_CLUMP    , 12, 11);
		assignItemRect(SUNDIAL        , 16, 12);
		assignItemRect(CLOVER         , 11, 15);
		assignItemRect(TRAP_MECHANISM , 13, 15);
		assignItemRect(MIMIC_TOOTH    ,  8, 15);
		assignItemRect(WONDROUS_RESIN , 12, 11);
		assignItemRect(EYE_OF_NEWT    , 12, 12);
		assignItemRect(SALT_CUBE      , 12, 13);
		assignItemRect(BLOOD_VIAL     ,  6, 15);
		assignItemRect(OBLIVION_SHARD ,  7, 14);
		assignItemRect(CHAOTIC_CENSER , 13, 15);
		assignItemRect(FERRET_TUFT    , 16, 15);
		assignItemRect(SPYGLASS       , 15, 15);
	}

	private static final int SCROLLS             =                               xy(1, 22);   //16 slots
	public static final int SCROLL_KAUNAN   = SCROLLS+0;
	public static final int SCROLL_SOWILO   = SCROLLS+1;
	public static final int SCROLL_LAGUZ    = SCROLLS+2;
	public static final int SCROLL_YNGVI    = SCROLLS+3;
	public static final int SCROLL_GYFU     = SCROLLS+4;
	public static final int SCROLL_RAIDO    = SCROLLS+5;
	public static final int SCROLL_ISAZ     = SCROLLS+6;
	public static final int SCROLL_MANNAZ   = SCROLLS+7;
	public static final int SCROLL_NAUDIZ   = SCROLLS+8;
	public static final int SCROLL_BERKANAN = SCROLLS+9;
	public static final int SCROLL_ODAL     = SCROLLS+10;
	public static final int SCROLL_TIWAZ    = SCROLLS+11;
	public static final int SCROLL_ANSUZ    = SCROLLS+12;
	public static final int ARCANE_RESIN    = SCROLLS+13;
	public static final int SCROLL_HAGALAZ  = SCROLLS+14;
	public static final int SCROLL_THURISAZ = SCROLLS+15;
	static{
		assignItemRect(SCROLL_KAUNAN  , 15, 14);
		assignItemRect(SCROLL_SOWILO  , 15, 14);
		assignItemRect(SCROLL_LAGUZ   , 15, 14);
		assignItemRect(SCROLL_YNGVI   , 15, 14);
		assignItemRect(SCROLL_GYFU    , 15, 14);
		assignItemRect(SCROLL_RAIDO   , 15, 14);
		assignItemRect(SCROLL_ISAZ    , 15, 14);
		assignItemRect(SCROLL_MANNAZ  , 15, 14);
		assignItemRect(SCROLL_NAUDIZ  , 15, 14);
		assignItemRect(SCROLL_BERKANAN, 15, 14);
		assignItemRect(SCROLL_ODAL    , 15, 14);
		assignItemRect(SCROLL_TIWAZ   , 15, 14);
		assignItemRect(SCROLL_ANSUZ   , 15, 14);
		assignItemRect(ARCANE_RESIN   , 12, 11);
		assignItemRect(SCROLL_HAGALAZ , 15, 14);
		assignItemRect(SCROLL_THURISAZ, 15, 14);
	}

	private static final int EXOTIC_SCROLLS      =                               xy(1, 23);   //16 slots
	public static final int EXOTIC_KAUNAN   = EXOTIC_SCROLLS+0;
	public static final int EXOTIC_SOWILO   = EXOTIC_SCROLLS+1;
	public static final int EXOTIC_LAGUZ    = EXOTIC_SCROLLS+2;
	public static final int EXOTIC_YNGVI    = EXOTIC_SCROLLS+3;
	public static final int EXOTIC_GYFU     = EXOTIC_SCROLLS+4;
	public static final int EXOTIC_RAIDO    = EXOTIC_SCROLLS+5;
	public static final int EXOTIC_ISAZ     = EXOTIC_SCROLLS+6;
	public static final int EXOTIC_MANNAZ   = EXOTIC_SCROLLS+7;
	public static final int EXOTIC_NAUDIZ   = EXOTIC_SCROLLS+8;
	public static final int EXOTIC_BERKANAN = EXOTIC_SCROLLS+9;
	public static final int EXOTIC_ODAL     = EXOTIC_SCROLLS+10;
	public static final int EXOTIC_TIWAZ    = EXOTIC_SCROLLS+11;
	public static final int EXOTIC_ANSUZ    = EXOTIC_SCROLLS+12;
	public static final int EXOTIC_HAGALAZ  = EXOTIC_SCROLLS+14;
	public static final int EXOTIC_THURISAZ = EXOTIC_SCROLLS+15;
	static {
		for (int i = EXOTIC_SCROLLS; i < EXOTIC_SCROLLS+16; i++)
			assignItemRect(i, 15, 14);
	}

	private static final int STONES              =                               xy(1, 24);   //16 slots
	public static final int STONE_AGGRESSION   = STONES+0;
	public static final int STONE_AUGMENTATION = STONES+1;
	public static final int STONE_FEAR         = STONES+2;
	public static final int STONE_BLAST        = STONES+3;
	public static final int STONE_BLINK        = STONES+4;
	public static final int STONE_CLAIRVOYANCE = STONES+5;
	public static final int STONE_SLEEP        = STONES+6;
	public static final int STONE_DETECT       = STONES+7;
	public static final int STONE_ENCHANT      = STONES+8;
	public static final int STONE_FLOCK        = STONES+9;
	public static final int STONE_INTUITION    = STONES+10;
	public static final int STONE_SHOCK        = STONES+11;
	public static final int NORN_GREEN         = STONES+12;
	public static final int NORN_BLUE          = STONES+13;
	public static final int NORN_ORANGE        = STONES+14;
	public static final int NORN_PURPLE        = STONES+15;
	static{
		assignItemRect(STONE_AGGRESSION  , 14, 12);
		assignItemRect(STONE_AUGMENTATION, 14, 12);
		assignItemRect(STONE_FEAR        , 14, 12);
		assignItemRect(STONE_BLAST       , 14, 12);
		assignItemRect(STONE_BLINK       , 14, 12);
		assignItemRect(STONE_CLAIRVOYANCE, 14, 12);
		assignItemRect(STONE_SLEEP       , 14, 12);
		assignItemRect(STONE_DETECT      , 14, 12);
		assignItemRect(STONE_ENCHANT     , 14, 12);
		assignItemRect(STONE_FLOCK       , 14, 12);
		assignItemRect(STONE_INTUITION   , 14, 12);
		assignItemRect(STONE_SHOCK       , 14, 12);
		assignItemRect(NORN_GREEN        ,  7,  9);
		assignItemRect(NORN_BLUE         ,  7,  9);
		assignItemRect(NORN_ORANGE       ,  7,  9);
		assignItemRect(NORN_PURPLE       ,  7,  9);
	}

	private static final int STONES_EXT          =                               xy(1, 25);   //16 slots (stones overflow + adamant)
	public static final int NORN_YELLOW    = STONES_EXT+0;
	public static final int STONE_ORE      = STONES_EXT+1;
	public static final int ADAMANT_WEAPON = STONES_EXT+2;
	static{
		assignItemRect(NORN_YELLOW   ,  7,  9);
		assignItemRect(STONE_ORE     ,  7,  9);
		assignItemRect(ADAMANT_WEAPON, 14, 14);
	}

	private static final int POTIONS             =                               xy(1, 26);   //16 slots
	public static final int POTION_CRIMSON      = POTIONS+0;
	public static final int POTION_AMBER        = POTIONS+1;
	public static final int POTION_GOLDEN       = POTIONS+2;
	public static final int POTION_JADE         = POTIONS+3;
	public static final int POTION_TURQUOISE    = POTIONS+4;
	public static final int POTION_AZURE        = POTIONS+5;
	public static final int POTION_INDIGO       = POTIONS+6;
	public static final int POTION_MAGENTA      = POTIONS+7;
	public static final int POTION_BISTRE       = POTIONS+8;
	public static final int POTION_CHARCOAL     = POTIONS+9;
	public static final int POTION_SILVER       = POTIONS+10;
	public static final int POTION_IVORY        = POTIONS+11;
	public static final int POTION_POISON_GREEN = POTIONS+12;
	public static final int POTION_SNOW_WHITE   = POTIONS+13;
	public static final int POTION_MUDDY_GREEN  = POTIONS+14;
	public static final int POTION_MUDDY_YELLOW = POTIONS+15;
	static {
		for (int i = POTIONS; i < POTIONS+16; i++)
			assignItemRect(i, 12, 14);
	}

	private static final int EXOTIC_POTIONS      =                               xy(1, 27);   //16 slots
	public static final int EXOTIC_CRIMSON      = EXOTIC_POTIONS+0;
	public static final int EXOTIC_AMBER        = EXOTIC_POTIONS+1;
	public static final int EXOTIC_GOLDEN       = EXOTIC_POTIONS+2;
	public static final int EXOTIC_JADE         = EXOTIC_POTIONS+3;
	public static final int EXOTIC_TURQUOISE    = EXOTIC_POTIONS+4;
	public static final int EXOTIC_AZURE        = EXOTIC_POTIONS+5;
	public static final int EXOTIC_INDIGO       = EXOTIC_POTIONS+6;
	public static final int EXOTIC_MAGENTA      = EXOTIC_POTIONS+7;
	public static final int EXOTIC_BISTRE       = EXOTIC_POTIONS+8;
	public static final int EXOTIC_CHARCOAL     = EXOTIC_POTIONS+9;
	public static final int EXOTIC_SILVER       = EXOTIC_POTIONS+10;
	public static final int EXOTIC_IVORY        = EXOTIC_POTIONS+11;
	public static final int EXOTIC_POISON_GREEN = EXOTIC_POTIONS+12;
	public static final int EXOTIC_SNOW_WHITE   = EXOTIC_POTIONS+13;
	public static final int EXOTIC_MUDDY_GREEN  = EXOTIC_POTIONS+14;
	public static final int EXOTIC_MUDDY_YELLOW = EXOTIC_POTIONS+15;
	static {
		for (int i = EXOTIC_POTIONS; i < EXOTIC_POTIONS+16; i++)
			assignItemRect(i, 12, 13);
	}

	private static final int SEEDS               =                               xy(1, 36);   //16 slots
	public static final int SEED_ROTBERRY     = SEEDS+0;
	public static final int SEED_FIREBLOOM    = SEEDS+1;
	public static final int SEED_SWIFTTHISTLE = SEEDS+2;
	public static final int SEED_SUNGRASS     = SEEDS+3;
	public static final int SEED_ICECAP       = SEEDS+4;
	public static final int SEED_STORMVINE    = SEEDS+5;
	public static final int SEED_SORROWMOSS   = SEEDS+6;
	public static final int SEED_MAGEROYAL    = SEEDS+7;
	public static final int SEED_EARTHROOT    = SEEDS+8;
	public static final int SEED_STARFLOWER   = SEEDS+9;
	public static final int SEED_FADELEAF     = SEEDS+10;
	public static final int SEED_BLINDWEED    = SEEDS+11;
	public static final int SEED_DREAMFOIL    = SEEDS+12;
	public static final int SEED_DEWCATCHER   = SEEDS+13;
	public static final int SEED_PHASEPITCHER = SEEDS+14;
	public static final int SEED_FLYTRAP      = SEEDS+15;
	static {
		for (int i = SEEDS; i < SEEDS+16; i++)
			assignItemRect(i, 10, 10);
	}

	private static final int BREWS               =                               xy(1, 41);   //8 slots
	public static final int BREW_INFERNAL = BREWS+0;
	public static final int BREW_BLIZZARD = BREWS+1;
	public static final int BREW_SHOCKING = BREWS+2;
	public static final int BREW_CAUSTIC  = BREWS+3;
	public static final int BREW_AQUA     = BREWS+4;
	public static final int BREW_UNSTABLE = BREWS+5;
	static{
		assignItemRect(BREW_INFERNAL, 12, 14);
		assignItemRect(BREW_BLIZZARD, 12, 14);
		assignItemRect(BREW_SHOCKING, 12, 14);
		assignItemRect(BREW_CAUSTIC , 12, 14);
		assignItemRect(BREW_AQUA    ,  9, 11);
		assignItemRect(BREW_UNSTABLE, 12, 14);
	}

	private static final int ELIXIRS             =                               xy(9, 41);   //8 slots
	public static final int ELIXIR_HONEY   = ELIXIRS+0;
	public static final int ELIXIR_AQUA    = ELIXIRS+1;
	public static final int ELIXIR_MIGHT   = ELIXIRS+2;
	public static final int ELIXIR_DRAGON  = ELIXIRS+3;
	public static final int ELIXIR_TOXIC   = ELIXIRS+4;
	public static final int ELIXIR_ICY     = ELIXIRS+5;
	public static final int ELIXIR_ARCANE  = ELIXIRS+6;
	public static final int ELIXIR_FEATHER = ELIXIRS+7;
	static {
		for (int i = ELIXIRS; i < ELIXIRS+8; i++)
			assignItemRect(i, 12, 14);
	}

	private static final int SPELLS              =                               xy(1, 42);   //16 slots
	public static final int WILD_ENERGY    = SPELLS+0;
	public static final int PHASE_SHIFT    = SPELLS+1;
	public static final int TELE_GRAB      = SPELLS+2;
	public static final int UNSTABLE_SPELL = SPELLS+3;
	public static final int CURSE_INFUSE   = SPELLS+5;
	public static final int MAGIC_INFUSE   = SPELLS+6;
	public static final int ALCHEMIZE      = SPELLS+7;
	public static final int RECYCLE        = SPELLS+8;
	public static final int RECLAIM_TRAP   = SPELLS+10;
	public static final int RETURN_BEACON  = SPELLS+11;
	public static final int SUMMON_ELE     = SPELLS+12;
	public static final int AQUA_BLAST       = SPELLS+1;
	public static final int MAGIC_PORTER     = SPELLS+3;
	public static final int FEATHER_FALL     = SPELLS+5;
	public static final int ENCHANT_INFUSE   = SPELLS+10;
	public static final int CRIMSON_EPITHET  = SPELLS+11;
	public static final int FORCEFIELD       = SPELLS+12;
	public static final int HOLYBLAST        = SPELLS+13;
	public static final int DOOMCALL         = SPELLS+14;
	public static final int SEASONCHANGE     = SPELLS+15;
	static{
		assignItemRect(WILD_ENERGY   , 12, 11);
		assignItemRect(PHASE_SHIFT   , 12, 11);
		assignItemRect(TELE_GRAB     , 12, 11);
		assignItemRect(UNSTABLE_SPELL, 12, 13);
		assignItemRect(CURSE_INFUSE  , 10, 15);
		assignItemRect(MAGIC_INFUSE  , 10, 15);
		assignItemRect(ALCHEMIZE     , 10, 15);
		assignItemRect(RECYCLE       , 10, 15);
		assignItemRect(RECLAIM_TRAP  ,  8, 16);
		assignItemRect(RETURN_BEACON ,  8, 16);
		assignItemRect(SUMMON_ELE    ,  8, 16);
		// AQUA_BLAST=SPELLS+1, MAGIC_PORTER=SPELLS+3, FEATHER_FALL=SPELLS+5,
		// ENCHANT_INFUSE=SPELLS+10, CRIMSON_EPITHET=SPELLS+11, FORCEFIELD=SPELLS+12
		// share tiles with the originals above — no separate assignItemRect needed
		assignItemRect(HOLYBLAST    , 14, 13);
		assignItemRect(DOOMCALL     ,  8, 16);
		assignItemRect(SEASONCHANGE , 12, 12);
	}

	private static final int FOOD                =                               xy(1, 44);   //48 slots (3 rows)
	public static final int MEAT            = FOOD+0;
	public static final int STEAK           = FOOD+1;
	public static final int STEWED          = FOOD+2;
	public static final int OVERPRICED      = FOOD+3;
	public static final int CARPACCIO       = FOOD+4;
	public static final int RATION          = FOOD+5;
	public static final int PASTY           = FOOD+6;
	public static final int MEAT_PIE        = FOOD+7;
	public static final int BLANDFRUIT      = FOOD+8;
	public static final int BLAND_CHUNKS    = FOOD+9;
	public static final int BERRY           = FOOD+10;
	public static final int PHANTOM_MEAT    = FOOD+11;
	public static final int SUPPLY_RATION   = FOOD+12;
	public static final int MONSTER_MEAT    = FOOD+13;
	public static final int DUNGEON_NUT     = FOOD+14;
	public static final int TOASTED_NUT     = FOOD+15;
	public static final int GOLDEN_NUT      = FOOD+16;
	public static final int BLACKBERRY_FOOD = FOOD+17;
	public static final int CLOUDBERRY_FOOD = FOOD+18;
	public static final int BLUEBERRY_FOOD  = FOOD+19;
	public static final int MOONBERRY_FOOD  = FOOD+20;
	public static final int FULLMOONBERRY   = FOOD+21;
	public static final int DEATHCAP        = FOOD+22;
	public static final int EARTHSTAR       = FOOD+23;
	public static final int GOLDEN_JELLY    = FOOD+24;
	public static final int JACK_O_LANTERN  = FOOD+25;
	public static final int BLUE_MILK       = FOOD+26;
	public static final int PIXIE_PARASOL   = FOOD+27;
	public static final int HONEY           = FOOD+28;
	public static final int MUSHROOM_ITEM   = FOOD+29;
	public static final int PUDDING_CUP     = FOOD+30;
	public static final int RICE_BALL       = FOOD+31;
	public static final int SEED_RICE       = FOOD+32;
	static{
		assignItemRect(MEAT           , 15, 11);
		assignItemRect(STEAK          , 15, 11);
		assignItemRect(STEWED         , 15, 11);
		assignItemRect(OVERPRICED     , 14, 11);
		assignItemRect(CARPACCIO      , 15, 11);
		assignItemRect(RATION         , 16, 12);
		assignItemRect(PASTY          , 16, 11);
		assignItemRect(MEAT_PIE       , 16, 12);
		assignItemRect(BLANDFRUIT     ,  9, 12);
		assignItemRect(BLAND_CHUNKS   , 14,  6);
		assignItemRect(BERRY          ,  9, 11);
		assignItemRect(PHANTOM_MEAT   , 15, 11);
		assignItemRect(SUPPLY_RATION  , 16, 12);
		assignItemRect(MONSTER_MEAT   , 15, 11);
		assignItemRect(DUNGEON_NUT    , 10, 10);
		assignItemRect(TOASTED_NUT    , 10, 10);
		assignItemRect(GOLDEN_NUT     , 10, 10);
		assignItemRect(BLACKBERRY_FOOD, 12, 13);
		assignItemRect(CLOUDBERRY_FOOD, 13, 13);
		assignItemRect(BLUEBERRY_FOOD , 12, 13);
		assignItemRect(MOONBERRY_FOOD , 12, 13);
		assignItemRect(FULLMOONBERRY  , 12, 13);
		assignItemRect(DEATHCAP       , 11, 12);
		assignItemRect(EARTHSTAR      , 16, 15);
		assignItemRect(GOLDEN_JELLY   , 12, 11);
		assignItemRect(JACK_O_LANTERN , 12, 13);
		assignItemRect(BLUE_MILK      , 12, 12);
		assignItemRect(PIXIE_PARASOL  , 11, 12);
		assignItemRect(HONEY          , 10, 14);
		assignItemRect(MUSHROOM_ITEM  , 13, 14);
		assignItemRect(PUDDING_CUP    , 12, 11);
		assignItemRect(RICE_BALL      , 15, 11);
		assignItemRect(SEED_RICE      , 10, 10);
	}

	private static final int HOLIDAY_FOOD        =                               xy(1, 47);   //16 slots
	public static final int STEAMED_FISH     = HOLIDAY_FOOD+0;
	public static final int FISH_LEFTOVER    = HOLIDAY_FOOD+1;
	public static final int CHOC_AMULET      = HOLIDAY_FOOD+2;
	public static final int EASTER_EGG       = HOLIDAY_FOOD+3;
	public static final int RAINBOW_POTION   = HOLIDAY_FOOD+4;
	public static final int SHATTERED_CAKE   = HOLIDAY_FOOD+5;
	public static final int PUMPKIN_PIE      = HOLIDAY_FOOD+6;
	public static final int VANILLA_CAKE     = HOLIDAY_FOOD+7;
	public static final int CANDY_CANE       = HOLIDAY_FOOD+8;
	public static final int SPARKLING_POTION = HOLIDAY_FOOD+9;
	static{
		assignItemRect(STEAMED_FISH    , 16, 12);
		assignItemRect(FISH_LEFTOVER   , 16, 12);
		assignItemRect(CHOC_AMULET     , 16, 16);
		assignItemRect(EASTER_EGG      , 12, 14);
		assignItemRect(RAINBOW_POTION  , 12, 14);
		assignItemRect(SHATTERED_CAKE  , 14, 13);
		assignItemRect(PUMPKIN_PIE     , 16, 12);
		assignItemRect(VANILLA_CAKE    , 14, 13);
		assignItemRect(CANDY_CANE      , 13, 16);
		assignItemRect(SPARKLING_POTION,  7, 16);
	}

	private static final int QUEST               =                               xy(1, 50);   //32 slots
	public static final int DUST                  = QUEST+1;
	public static final int CANDLE                = QUEST+2;
	public static final int EMBER                 = QUEST+3;
	public static final int PICKAXE               = QUEST+4;
	public static final int ORE                   = QUEST+5;
	public static final int TOKEN                 = QUEST+6;
	public static final int BLOB                  = QUEST+7;
	public static final int SHARD                 = QUEST+8;
	public static final int ESCAPE                = QUEST+9;
	public static final int ORB_OF_ZOT            = QUEST+10;
	public static final int PALANTIR              = QUEST+11;
	public static final int CONCH_SHELL_ITEM      = QUEST+12;
	public static final int BONE_ITEM             = QUEST+13;
	public static final int EASTER_EGG_ITEM       = QUEST+14;
	public static final int SANCHIKARAH           = QUEST+15;
	public static final int SANCHIKARAH_DEATH     = QUEST+16;
	public static final int SANCHIKARAH_LIFE      = QUEST+17;
	public static final int SANCHIKARAH_TRANSCEND = QUEST+18;
	static{
		assignItemRect(DUST                 , 12, 11);
		assignItemRect(CANDLE               , 12, 12);
		assignItemRect(EMBER                , 12, 11);
		assignItemRect(PICKAXE              , 14, 14);
		assignItemRect(ORE                  , 15, 15);
		assignItemRect(TOKEN                , 12, 12);
		assignItemRect(BLOB                 , 10,  9);
		assignItemRect(SHARD                ,  8, 10);
		assignItemRect(ESCAPE               ,  8, 16);
		assignItemRect(ORB_OF_ZOT           , 14, 14);
		assignItemRect(PALANTIR             , 14, 14);
		assignItemRect(CONCH_SHELL_ITEM     ,  9, 14);
		assignItemRect(BONE_ITEM            , 11, 11);
		assignItemRect(EASTER_EGG_ITEM      , 11, 14);
		assignItemRect(SANCHIKARAH          , 16, 10);
		assignItemRect(SANCHIKARAH_DEATH    , 16, 10);
		assignItemRect(SANCHIKARAH_LIFE     , 16, 10);
		assignItemRect(SANCHIKARAH_TRANSCEND, 16, 10);
	}

	private static final int BAGS                =                               xy(1, 53);   //16 slots
	public static final int WATERSKIN = BAGS+0;
	public static final int BACKPACK  = BAGS+1;
	public static final int POUCH     = BAGS+2;
	public static final int HOLDER    = BAGS+3;
	public static final int BANDOLIER = BAGS+4;
	public static final int HOLSTER   = BAGS+5;
	public static final int VIAL      = BAGS+6;
	public static final int KEYRING   = BAGS+7;
	public static final int CHAIN      = BAGS+8;
	public static final int FOOD_POUCH = BAGS+9;
	static{
		assignItemRect(WATERSKIN  , 16, 14);
		assignItemRect(BACKPACK   , 16, 16);
		assignItemRect(POUCH      , 14, 15);
		assignItemRect(HOLDER     , 16, 16);
		assignItemRect(BANDOLIER  , 15, 16);
		assignItemRect(HOLSTER    , 15, 16);
		assignItemRect(VIAL       , 12, 12);
		assignItemRect(KEYRING    , 16, 16);
		assignItemRect(CHAIN      , 16, 16);
		assignItemRect(FOOD_POUCH , 14, 15);
	}

	private static final int DOCUMENTS           =                               xy(1, 54);   //16 slots
	public static final int GUIDE_PAGE        = DOCUMENTS+0;
	public static final int ALCH_PAGE         = DOCUMENTS+1;
	public static final int SEWER_PAGE        = DOCUMENTS+2;
	public static final int PRISON_PAGE       = DOCUMENTS+3;
	public static final int CAVES_PAGE        = DOCUMENTS+4;
	public static final int CITY_PAGE         = DOCUMENTS+5;
	public static final int HALLS_PAGE        = DOCUMENTS+6;
	public static final int BOOK_OF_DEAD      = DOCUMENTS+7;
	public static final int BOOK_OF_LIFE      = DOCUMENTS+8;
	public static final int BOOK_OF_TRANSCEND = DOCUMENTS+9;
	public static final int OTILUKES_JOURNAL  = DOCUMENTS+10;
	public static final int JOURNAL_PAGE      = DOCUMENTS+11;
	static{
		assignItemRect(GUIDE_PAGE       , 10, 11);
		assignItemRect(ALCH_PAGE        , 10, 11);
		assignItemRect(SEWER_PAGE       , 10, 11);
		assignItemRect(PRISON_PAGE      , 10, 11);
		assignItemRect(CAVES_PAGE       , 10, 11);
		assignItemRect(CITY_PAGE        , 10, 11);
		assignItemRect(HALLS_PAGE       , 10, 11);
		assignItemRect(BOOK_OF_DEAD     , 13, 16);
		assignItemRect(BOOK_OF_LIFE     , 13, 16);
		assignItemRect(BOOK_OF_TRANSCEND, 13, 16);
		assignItemRect(OTILUKES_JOURNAL , 14, 15);
		assignItemRect(JOURNAL_PAGE     , 15, 14);
	}

	//--- Ported from OvergrownPD rows 31-47 → shattered rows 38-54 ---

	private static final int SEEDS_OV            =                               xy(1, 37);   //16 slots
	public static final int SEED_ROTBERRY_OV     = SEEDS_OV+0;
	public static final int SEED_FIREBLOOM_OV    = SEEDS_OV+1;
	public static final int SEED_SWIFTTHISTLE_OV = SEEDS_OV+2;
	public static final int SEED_SUNGRASS_OV     = SEEDS_OV+3;
	public static final int SEED_ICECAP_OV       = SEEDS_OV+4;
	public static final int SEED_STORMVINE_OV    = SEEDS_OV+5;
	public static final int SEED_SORROWMOSS_OV   = SEEDS_OV+6;
	public static final int SEED_DREAMFOIL_OV    = SEEDS_OV+7;
	public static final int SEED_EARTHROOT_OV    = SEEDS_OV+8;
	public static final int SEED_STARFLOWER_OV   = SEEDS_OV+9;
	public static final int SEED_FADELEAF_OV     = SEEDS_OV+10;
	public static final int SEED_BLINDWEED_OV    = SEEDS_OV+11;
	public static final int SEED_FEELERFERN      = SEEDS_OV+12;
	public static final int SEED_LARVALEAVE      = SEEDS_OV+13;
	public static final int SEED_TANKCABBAGE     = SEEDS_OV+14;
	public static final int SEED_CORNWHEAT       = SEEDS_OV+15;
	static {
		for (int i = SEEDS_OV; i < SEEDS_OV+16; i++)
			assignItemRect(i, 10, 10);
	}

	private static final int POTIONS2            =                               xy(1, 28);   //16 slots
	public static final int POTION_HONEY         = POTIONS2+0;
	public static final int POTION_BLOODY        = POTIONS2+1;
	public static final int POTION_ORANGE        = POTIONS2+2;
	public static final int POTION_VIOLETT       = POTIONS2+3;
	public static final int POTION_YELLOW        = POTIONS2+4;
	public static final int POTION_WHITE         = POTIONS2+5;
	public static final int POTION_BROWN         = POTIONS2+6;
	public static final int POTION_BRIGHT_BLUE   = POTIONS2+7;
	public static final int POTION_RAINBOW       = POTIONS2+8;
	public static final int POTION_BRIGHT_ORANGE = POTIONS2+9;
	public static final int POTION_DARK_BLUE     = POTIONS2+10;
	public static final int POTION_BLACK         = POTIONS2+11;
	public static final int POTION_YELLOW_ORANGE = POTIONS2+12;
	public static final int POTION_GRASS_GREEN   = POTIONS2+13;
	public static final int POTION_SKY_BLUE      = POTIONS2+14;
	public static final int POTION_GREEN_BLUE    = POTIONS2+15;
	static {
		for (int i = POTIONS2; i < POTIONS2+16; i++)
			assignItemRect(i, 12, 14);
	}

	private static final int EXOTIC_POTIONS2     =                               xy(1, 29);   //16 slots
	public static final int EXOTIC_HONEY         = EXOTIC_POTIONS2+0;
	public static final int EXOTIC_BLOODY        = EXOTIC_POTIONS2+1;
	public static final int EXOTIC_ORANGE        = EXOTIC_POTIONS2+2;
	public static final int EXOTIC_VIOLETT       = EXOTIC_POTIONS2+3;
	public static final int EXOTIC_YELLOW        = EXOTIC_POTIONS2+4;
	public static final int EXOTIC_WHITE         = EXOTIC_POTIONS2+5;
	public static final int EXOTIC_BROWN         = EXOTIC_POTIONS2+6;
	public static final int EXOTIC_BRIGHTBLUE    = EXOTIC_POTIONS2+7;
	public static final int EXOTIC_RAINBOW       = EXOTIC_POTIONS2+8;
	public static final int EXOTIC_BRIGHTORANGE  = EXOTIC_POTIONS2+9;
	public static final int EXOTIC_DARKBLUE      = EXOTIC_POTIONS2+10;
	public static final int EXOTIC_BLACK         = EXOTIC_POTIONS2+11;
	public static final int EXOTIC_YELLOW_ORANGE = EXOTIC_POTIONS2+12;
	public static final int EXOTIC_GRASS_GREEN   = EXOTIC_POTIONS2+13;
	public static final int EXOTIC_SKY_BLUE      = EXOTIC_POTIONS2+14;
	public static final int EXOTIC_GREEN_BLUE    = EXOTIC_POTIONS2+15;
	static {
		for (int i = EXOTIC_POTIONS2; i < EXOTIC_POTIONS2+16; i++)
			assignItemRect(i, 12, 13);
	}

	private static final int SEEDS2              =                               xy(1, 38);   //16 slots
	public static final int SEED_SUNBLOOM        = SEEDS2+0;
	public static final int SEED_TOMATOBUSH      = SEEDS2+1;
	public static final int SEED_FIREFOXGLOVE    = SEEDS2+2;
	public static final int SEED_MUSCLEMOSS      = SEEDS2+3;
	public static final int SEED_BUTTERLION      = SEEDS2+4;
	public static final int SEED_SNOWHEDGE       = SEEDS2+5;
	public static final int SEED_STEAMWEED       = SEEDS2+6;
	public static final int SEED_SEEDPOD         = SEEDS2+8;
	public static final int SEED_CHANDALIERTAIL  = SEEDS2+9;
	public static final int SEED_NIGHTSHADEONION = SEEDS2+10;
	public static final int SEED_BLACKHOLEFLOWER = SEEDS2+11;
	public static final int SEED_COMBFLOWER      = SEEDS2+12;
	public static final int SEED_BALLCROP        = SEEDS2+13;
	public static final int SEED_BLUEEYEDSUSAN   = SEEDS2+14;
	public static final int SEED_COCOSTUFT       = SEEDS2+15;
	static {
		for (int i = SEEDS2; i < SEEDS2+16; i++)
			assignItemRect(i, 10, 10);
	}

	private static final int POTIONS3            =                               xy(1, 30);   //16 slots
	public static final int POTION_BLUE          = POTIONS3+0;
	public static final int POTION_FLAT_BLUE     = POTIONS3+1;
	public static final int POTION_PARASITIC     = POTIONS3+2;
	public static final int POTION_MAROON     = POTIONS3+3;
	public static final int POTION_PUNCH         = POTIONS3+4;
	public static final int POTION_BEIGE         = POTIONS3+5;
	public static final int POTION_SCARLET   = POTIONS3+6;
	public static final int POTION_WATER_BLUE    = POTIONS3+7;
	public static final int POTION_BRIGHT_GREEN  = POTIONS3+8;
	public static final int POTION_INDIGO_PURPLE = POTIONS3+9;
	public static final int POTION_LIME_GREEN    = POTIONS3+10;
	public static final int POTION_ROSE          = POTIONS3+11;
	public static final int POTION_BRIGHT_PURPLE = POTIONS3+12;
	public static final int POTION_DARK_ROSE     = POTIONS3+13;
	public static final int POTION_CORN_YELLOW   = POTIONS3+14;
	public static final int POTION_DARK_PURPLE   = POTIONS3+15;
	static {
		for (int i = POTIONS3; i < POTIONS3+16; i++)
			assignItemRect(i, 12, 14);
	}

	private static final int EXOTIC_POTIONS3     =                               xy(1, 31);   //16 slots
	public static final int EXOTIC_BLUE          = EXOTIC_POTIONS3+0;
	public static final int EXOTIC_FLATBLUE      = EXOTIC_POTIONS3+1;
	public static final int EXOTIC_PARASITIC     = EXOTIC_POTIONS3+2;
	public static final int EXOTIC_MAROON     = EXOTIC_POTIONS3+3;
	public static final int EXOTIC_PUNCH         = EXOTIC_POTIONS3+4;
	public static final int EXOTIC_BEIGE         = EXOTIC_POTIONS3+5;
	public static final int EXOTIC_SCARLET   = EXOTIC_POTIONS3+6;
	public static final int EXOTIC_WATERBLUE     = EXOTIC_POTIONS3+7;
	public static final int EXOTIC_BRIGHT_GREEN  = EXOTIC_POTIONS3+8;
	public static final int EXOTIC_INDIGO_PURPLE = EXOTIC_POTIONS3+9;
	public static final int EXOTIC_LIMEGREEN     = EXOTIC_POTIONS3+10;
	public static final int EXOTIC_ROSE          = EXOTIC_POTIONS3+11;
	public static final int EXOTIC_BRIGHT_PURPLE = EXOTIC_POTIONS3+12;
	public static final int EXOTIC_DARK_ROSE     = EXOTIC_POTIONS3+13;
	public static final int EXOTIC_CORN_YELLOW   = EXOTIC_POTIONS3+14;
	public static final int EXOTIC_DARK_PURPLE   = EXOTIC_POTIONS3+15;
	static {
		for (int i = EXOTIC_POTIONS3; i < EXOTIC_POTIONS3+16; i++)
			assignItemRect(i, 12, 13);
	}

	private static final int SEEDS3              =                               xy(1, 39);   //16 slots
	public static final int SEED_FROSTCORN       = SEEDS3+0;
	public static final int SEED_WILLOWCANE      = SEEDS3+1;
	public static final int SEED_PARASITESHRUB   = SEEDS3+2;
	public static final int SEED_CRIMSONPEPPER   = SEEDS3+3;
	public static final int SEED_APRICOBUSH      = SEEDS3+4;
	public static final int SEED_WITHERFENNEL    = SEEDS3+5;
	public static final int SEED_CHILLISNAPPER   = SEEDS3+6;
	public static final int SEED_WATERWEED       = SEEDS3+7;
	public static final int SEED_GRASSLILLY      = SEEDS3+8;
	public static final int SEED_PEANUTPETAL     = SEEDS3+9;
	public static final int SEED_KIWIVETCH       = SEEDS3+10;
	public static final int SEED_ROSE            = SEEDS3+11;
	public static final int SEED_VENUSFLYTRAP    = SEEDS3+12;
	public static final int SEED_SUNCARNIVORE    = SEEDS3+13;
	public static final int SEED_EGGBLOOM        = SEEDS3+15;
	static {
		for (int i = SEEDS3; i < SEEDS3+16; i++)
			assignItemRect(i, 10, 10);
	}

	private static final int POTIONS4            =                               xy(1, 32);   //16 slots
	public static final int POTION_LIGHTNING_BLUE   = POTIONS4+0;
	public static final int POTION_VINE_RED         = POTIONS4+1;
	public static final int POTION_PURE_WHITE       = POTIONS4+2;
	public static final int POTION_LIGHT_LIME_GREEN = POTIONS4+3;
	public static final int POTION_PALE_PURPLE      = POTIONS4+4;
	public static final int POTION_LIGHT_VIOLETT    = POTIONS4+5;
	public static final int POTION_BLACK_WHITE      = POTIONS4+6;
	public static final int POTION_WHITE_BLACK      = POTIONS4+7;
	public static final int POTION_PALE_VIOLETT     = POTIONS4+8;
	public static final int POTION_BROWN_GREEN      = POTIONS4+9;
	public static final int POTION_DARK_BROWN_GREEN = POTIONS4+10;
	public static final int POTION_MOSSY_GREEN      = POTIONS4+11;
	public static final int POTION_PURPLE_VIOLETT   = POTIONS4+12;
	public static final int POTION_LIGHT_BLUE_GREEN = POTIONS4+13;
	public static final int POTION_LIGHT_ROSE       = POTIONS4+14;
	public static final int POTION_YELLOW_PURPLE    = POTIONS4+15;
	static {
		for (int i = POTIONS4; i < POTIONS4+16; i++)
			assignItemRect(i, 12, 14);
	}

	private static final int EXOTIC_POTIONS4     =                               xy(1, 33);   //16 slots
	public static final int EXOTIC_LIGHTNING_BLUE   = EXOTIC_POTIONS4+0;
	public static final int EXOTIC_DARK_BLOODY      = EXOTIC_POTIONS4+1;
	public static final int EXOTIC_PURE_WHITE       = EXOTIC_POTIONS4+2;
	public static final int EXOTIC_LIME_GREEN2      = EXOTIC_POTIONS4+3;
	public static final int EXOTIC_PALE_PURPLE      = EXOTIC_POTIONS4+4;
	public static final int EXOTIC_LIGHT_VIOLETT    = EXOTIC_POTIONS4+5;
	public static final int EXOTIC_BLACK_WHITE      = EXOTIC_POTIONS4+6;
	public static final int EXOTIC_WHITE_BLACK      = EXOTIC_POTIONS4+7;
	public static final int EXOTIC_PALE_VIOLETT     = EXOTIC_POTIONS4+8;
	public static final int EXOTIC_BROWN_GREEN      = EXOTIC_POTIONS4+9;
	public static final int EXOTIC_DARK_BROWN_GREEN = EXOTIC_POTIONS4+10;
	public static final int EXOTIC_MOSSY_GREEN      = EXOTIC_POTIONS4+11;
	public static final int EXOTIC_PURPLE_VIOLETT   = EXOTIC_POTIONS4+12;
	public static final int EXOTIC_LIGHT_BLUE_GREEN = EXOTIC_POTIONS4+13;
	public static final int EXOTIC_LIGHT_ROSE       = EXOTIC_POTIONS4+14;
	public static final int EXOTIC_YELLOW_PURPLE    = EXOTIC_POTIONS4+15;
	static {
		for (int i = EXOTIC_POTIONS4; i < EXOTIC_POTIONS4+16; i++)
			assignItemRect(i, 12, 13);
	}

	private static final int SEEDS4              =                               xy(1, 40);   //16 slots
	public static final int SEED_LIGHTNINGLILY   = SEEDS4+0;
	public static final int SEED_GOOGRASS        = SEEDS4+1;
	public static final int SEED_POPPOPLAR       = SEEDS4+2;
	public static final int SEED_SOURPITCHER     = SEEDS4+3;
	public static final int SEED_CLITBALM        = SEEDS4+4;
	public static final int SEED_HYPNOHEMP       = SEEDS4+5;
	public static final int SEED_CLOCKCYPRESS    = SEEDS4+6;
	public static final int SEED_EYEEUONYMUS     = SEEDS4+7;
	public static final int SEED_GOBGRAPE        = SEEDS4+8;
	public static final int SEED_BANANABEAN      = SEEDS4+9;
	public static final int SEED_DIRTDAISY       = SEEDS4+10;
	public static final int SEED_GRASSVINE       = SEEDS4+11;
	public static final int SEED_LAVENDERLANTERN = SEEDS4+12;
	public static final int SEED_FLOWERTREE      = SEEDS4+13;
	public static final int SEED_CRIMSONCROWN    = SEEDS4+14;
	public static final int SEED_SHADOWBLOOM     = SEEDS4+15;
	static {
		for (int i = SEEDS4; i < SEEDS4+16; i++)
			assignItemRect(i, 10, 10);
	}

	private static final int POTIONS5            =                               xy(1, 34);   //1 slot
	public static final int POTION_TEAL         = POTIONS5+0;
	static {
		assignItemRect(POTION_TEAL, 12, 14);
	}

	private static final int EXOTIC_POTIONS5    =                               xy(1, 35);   //1 slot
	public static final int EXOTIC_TEAL         = EXOTIC_POTIONS5+0;
	static {
		assignItemRect(EXOTIC_TEAL, 12, 13);
	}

	//--- Sprouted unique items (ported references) ---
	private static final int SPROUTED_UNIQUE      =                               xy(1, 55);   //16 slots
	public static final int INACTIVE_MRD    = SPROUTED_UNIQUE+0;
	public static final int ACTIVE_MRD      = SPROUTED_UNIQUE+1;
	public static final int PET_EGG         = SPROUTED_UNIQUE+2;
	public static final int SHADOW_DRAGON_EGG = SPROUTED_UNIQUE+3;
	public static final int DEWDROP_YELLOW  = SPROUTED_UNIQUE+4;
	public static final int DEWDROP_RED     = SPROUTED_UNIQUE+5;
	public static final int DEWDROP_VIOLET  = SPROUTED_UNIQUE+6;
	public static final int INACTIVE_MRD2   = SPROUTED_UNIQUE+7;
	public static final int ACTIVE_MRD2     = SPROUTED_UNIQUE+8;
	public static final int SEED_LARVALEAF  = SPROUTED_UNIQUE+9;
	static {
		assignItemRect(INACTIVE_MRD    , 14, 14);
		assignItemRect(ACTIVE_MRD      , 15, 14);
		for (int i = SPROUTED_UNIQUE+2; i < SPROUTED_UNIQUE+7; i++)
			assignItemRect(i, 16, 16);
		assignItemRect(INACTIVE_MRD2   , 14, 14);
		assignItemRect(ACTIVE_MRD2     , 14, 14);
		assignItemRect(SEED_LARVALEAF  , 10, 10);
	}



	//--- Ported from OvergrownPD (items not in shattered) ---

	private static final int PORT_WANDS                     =                                xy(1, 16);   //2 slots
	public static final int WAND_FLOCK                               = PORT_WANDS+0;
	public static final int WAND_POISON                              = PORT_WANDS+1;
	static {
		for (int i = PORT_WANDS; i < PORT_WANDS+2; i++)
			assignItemRect(i, 14, 14);
	}





	private static final int PORT_SPELLS_ROW2               =                                xy(1, 43);   //5 slots
	public static final int PLANT_SUMMON                             = PORT_SPELLS_ROW2+0;
	public static final int NATURES_LULLABY                          = PORT_SPELLS_ROW2+1;
	public static final int SPONTANEOUS_COMBUSTION                   = PORT_SPELLS_ROW2+2;
	public static final int FORCE_PUSH                               = PORT_SPELLS_ROW2+3;
	static {
		assignItemRect(PLANT_SUMMON                  , 11, 14);
		assignItemRect(NATURES_LULLABY               , 10, 10);
		assignItemRect(SPONTANEOUS_COMBUSTION        ,  6, 14);
		assignItemRect(FORCE_PUSH                    ,  6, 14);
	}

	private static final int PORT_FOOD                      =                                xy(1, 48);   //4 slots
	public static final int PEANUT                                   = PORT_FOOD+0;
	public static final int APRICO                                   = PORT_FOOD+1;
	public static final int CORNWHEATSHAFT                           = PORT_FOOD+2;
	public static final int EGG                                      = PORT_FOOD+3;
	static {
		assignItemRect(PEANUT                        ,  8,  9);
		assignItemRect(APRICO                        , 11, 11);
		assignItemRect(CORNWHEATSHAFT                ,  9,  9);
		assignItemRect(EGG                           , 11, 13);
	}

	private static final int PORT_FOOD2                     =                                xy(1, 49);   //5 slots
	public static final int GRAPE                                    = PORT_FOOD2+0;
	static {
		assignItemRect(GRAPE                         ,  9,  9);
	}

	private static final int WARPED_CONSUMABLES             =                                xy(1, 57);   //16 slots
	public static final int COFFEE                                   = WARPED_CONSUMABLES+0;
	public static final int FERTILIZER                              = WARPED_CONSUMABLES+1;
	public static final int WORLD_MAP                               = WARPED_CONSUMABLES+2;
	static {
		assignItemRect(COFFEE                        , 12, 13);
		assignItemRect(FERTILIZER , 10, 13);
		assignItemRect(WORLD_MAP  , 14, 11);
	}

	/* Sprite sheet nuances (learned the hard way porting SPS-PD weapons):
	 * - xy() is 1-BASED: xy(1,56) is the first column of the 56th row, i.e.
	 *   pixel (0, 55*16). Computing cells 0-based lands one column right and
	 *   one row down - which is how these weapons once overwrote the QUEST
	 *   block's reserved rows (token/shards/escape art showed on weapons).
	 * - Blocks reserve whole slot RANGES, not just the cells with art. Always
	 *   check the declared block spans above, not pixel emptiness, before
	 *   claiming space. When in doubt, extend the sheet with a fresh row.
	 * - Art must be anchored to the TOP-LEFT of its 16x16 cell, trimmed of
	 *   empty borders: assignItemRect(item, w, h) crops the top-left w x h of
	 *   the cell, and ItemSprite/ItemSlot center the frame afterwards. A
	 *   narrow sprite (e.g. the 6x16 flute) looks left-aligned in the raw
	 *   sheet but renders centered in game. */
	//SPS-PD weapon ports (new sheet row)
	private static final int WARPED_WEAPONS                 =                                xy(1, 56);   //16 slots
	public static final int MAGEBOOK                                 = WARPED_WEAPONS+0;
	public static final int FIGHTGLOVES                              = WARPED_WEAPONS+1;
	public static final int NUNCHAKUS                                = WARPED_WEAPONS+2;
	public static final int DUAL_KNIFE                               = WARPED_WEAPONS+3;
	public static final int TRIANGOLO                                = WARPED_WEAPONS+4;
	public static final int FLUTE                                    = WARPED_WEAPONS+5;
	public static final int WARDRUM                                  = WARPED_WEAPONS+6;
	public static final int TRUMPET                                  = WARPED_WEAPONS+7;
	public static final int HARP                                     = WARPED_WEAPONS+8;
	public static final int HOLY_WATER                               = WARPED_WEAPONS+9;
	public static final int PRAYER_WHEEL                             = WARPED_WEAPONS+10;
	public static final int STONE_CROSS                              = WARPED_WEAPONS+11;
	public static final int TRICK_SAND                               = WARPED_WEAPONS+12;
	public static final int MIRROR_DOLL                              = WARPED_WEAPONS+13;
	public static final int WIND_BOTTLE                              = WARPED_WEAPONS+14;
	public static final int HAND_LIGHT                               = WARPED_WEAPONS+15;
	static {
		assignItemRect(MAGEBOOK                      , 14, 16);
		assignItemRect(FIGHTGLOVES                   , 14, 14);
		assignItemRect(NUNCHAKUS                     , 14, 15);
		assignItemRect(DUAL_KNIFE                    , 14, 13);
		assignItemRect(TRIANGOLO                     , 16, 13);
		assignItemRect(FLUTE                         ,  6, 16);
		assignItemRect(WARDRUM                       , 14, 13);
		assignItemRect(TRUMPET                       , 12, 14);
		assignItemRect(HARP                          , 15, 14);
		assignItemRect(HOLY_WATER                    , 11, 16);
		assignItemRect(PRAYER_WHEEL                  , 15, 15);
		assignItemRect(STONE_CROSS                   , 16, 16);
		assignItemRect(TRICK_SAND                    , 14, 14);
		assignItemRect(MIRROR_DOLL                   , 14, 16);
		assignItemRect(WIND_BOTTLE                   , 11, 15);
		assignItemRect(HAND_LIGHT                    , 15, 16);
	}

	private static final int WARPED_PORTS_1          =                                xy(1, 58);   //16 slots
	public static final int BRICK                            = WARPED_PORTS_1+0;
	public static final int HUGE_SHURIKEN                    = WARPED_PORTS_1+1;
	public static final int SMALL_CHAKRAM                    = WARPED_PORTS_1+2;
	public static final int BOTTLE_FIRE                      = WARPED_PORTS_1+3;
	public static final int EMP_BOLA                         = WARPED_PORTS_1+4;
	public static final int HONEY_ARROW                      = WARPED_PORTS_1+5;
	public static final int MIND_ARROW                       = WARPED_PORTS_1+6;
	public static final int THROWING_WAVE                    = WARPED_PORTS_1+7;
	public static final int THROWING_SKULL                   = WARPED_PORTS_1+8;
	public static final int LIFE_ARMOR                       = WARPED_PORTS_1+9;
	public static final int SOLDIER_ARMOR                    = WARPED_PORTS_1+10;
	public static final int PERFORMER_ARMOR                  = WARPED_PORTS_1+11;
	public static final int ASCETIC_ARMOR                    = WARPED_PORTS_1+12;
	public static final int FOLLOWER_ARMOR                   = WARPED_PORTS_1+13;
	public static final int CERAMICS_ARMOR                   = WARPED_PORTS_1+14;
	public static final int MACHINE_ARMOR                    = WARPED_PORTS_1+15;
	static {
		assignItemRect(BRICK                        , 15, 14);
		assignItemRect(HUGE_SHURIKEN                , 16, 16);
		assignItemRect(SMALL_CHAKRAM                , 14, 14);
		assignItemRect(BOTTLE_FIRE                  , 12, 13);
		assignItemRect(EMP_BOLA                     , 14, 13);
		assignItemRect(HONEY_ARROW                  , 12, 12);
		assignItemRect(MIND_ARROW                   , 12, 12);
		assignItemRect(THROWING_WAVE                , 14, 16);
		assignItemRect(THROWING_SKULL               ,  8, 8);
		assignItemRect(LIFE_ARMOR                   , 14, 13);
		assignItemRect(SOLDIER_ARMOR                , 10, 13);
		assignItemRect(PERFORMER_ARMOR              , 14, 13);
		assignItemRect(ASCETIC_ARMOR                , 13, 16);
		assignItemRect(FOLLOWER_ARMOR               , 15, 13);
		assignItemRect(CERAMICS_ARMOR               , 14, 13);
		assignItemRect(MACHINE_ARMOR                , 16, 13);
	}

	private static final int WARPED_PORTS_2          =                                xy(1, 59);   //16 slots
	public static final int BULLET_ARMOR                     = WARPED_PORTS_2+0;
	public static final int ELF_BOW                          = WARPED_PORTS_2+1;
	public static final int TAURCEN_BOW                      = WARPED_PORTS_2+2;
	public static final int MINI_GUN                         = WARPED_PORTS_2+3;
	public static final int SHOOT_GUN                        = WARPED_PORTS_2+4;
	public static final int MEGA_CANNON                      = WARPED_PORTS_2+5;
	public static final int ELF_BOW_AMMO                     = WARPED_PORTS_2+6;
	public static final int TAURCEN_BOW_AMMO                 = WARPED_PORTS_2+7;
	public static final int MINI_GUN_AMMO                    = WARPED_PORTS_2+8;
	public static final int SHOOT_GUN_AMMO                   = WARPED_PORTS_2+9;
	public static final int MEGA_CANNON_AMMO                 = WARPED_PORTS_2+10;
	static {
		assignItemRect(BULLET_ARMOR                 , 14, 11);
		assignItemRect(ELF_BOW                      , 16, 16);
		assignItemRect(TAURCEN_BOW                  , 16, 16);
		assignItemRect(MINI_GUN                     , 14, 16);
		assignItemRect(SHOOT_GUN                    , 14, 16);
		assignItemRect(MEGA_CANNON                  , 13, 7);
		assignItemRect(ELF_BOW_AMMO                 , 12, 12);
		assignItemRect(TAURCEN_BOW_AMMO             , 12, 12);
		assignItemRect(MINI_GUN_AMMO                ,  6, 6);
		assignItemRect(SHOOT_GUN_AMMO               , 10, 7);
		assignItemRect(MEGA_CANNON_AMMO             , 15, 14);
	}

	private static final int PORT_QUEST                     =                                xy(1, 52);   //2 slots
	public static final int SKULL                                    = PORT_QUEST+0;
	static {
		assignItemRect(SKULL                         , 16, 11);
	}

	//for smaller 8x8 icons that often accompany an item sprite
	public static class Icons {

		private static final int WIDTH = 16;
		public static final int SIZE = 8;

		public static TextureFilm film = new TextureFilm( Assets.Sprites.ITEM_ICONS, SIZE, SIZE );

		private static int xy(int x, int y){
			x -= 1; y -= 1;
			return x + WIDTH*y;
		}

		private static void assignIconRect( int item, int width, int height ){
			int x = (item % WIDTH) * SIZE;
			int y = (item / WIDTH) * SIZE;
			film.add( item, x, y, x+width, y+height);
		}

		private static final int RINGS          =                            xy(1, 1);  //16 slots
		public static final int RING_ACCURACY   = RINGS+0;
		public static final int RING_ARCANA     = RINGS+1;
		public static final int RING_ELEMENTS   = RINGS+2;
		public static final int RING_ENERGY     = RINGS+3;
		public static final int RING_EVASION    = RINGS+4;
		public static final int RING_FORCE      = RINGS+5;
		public static final int RING_FUROR      = RINGS+6;
		public static final int RING_HASTE      = RINGS+7;
		public static final int RING_MIGHT      = RINGS+8;
		public static final int RING_SHARPSHOOT = RINGS+9;
		public static final int RING_TENACITY   = RINGS+10;
		public static final int RING_WEALTH     = RINGS+11;
		public static final int RING_MAGIC      = RINGS+12;
		//Unleashed PD ports (claim previously-unused drawn icon slots)
		public static final int RING_SATING     = RINGS+13;
		public static final int RING_SEARCHING  = RINGS+15;
		static {
			assignIconRect( RING_ACCURACY,      7, 7 );
			assignIconRect( RING_ARCANA,        7, 7 );
			assignIconRect( RING_ELEMENTS,      7, 7 );
			assignIconRect( RING_ENERGY,        7, 5 );
			assignIconRect( RING_EVASION,       7, 7 );
			assignIconRect( RING_FORCE,         5, 6 );
			assignIconRect( RING_FUROR,         7, 6 );
			assignIconRect( RING_HASTE,         6, 6 );
			assignIconRect( RING_MIGHT,         7, 7 );
			assignIconRect( RING_SHARPSHOOT,    7, 7 );
			assignIconRect( RING_TENACITY,      6, 6 );
			assignIconRect( RING_WEALTH,        7, 6 );
			assignIconRect( RING_MAGIC,         7, 7 );
			assignIconRect( RING_SATING,        7, 7 );
			assignIconRect( RING_SEARCHING,     7, 7 );
		}

		                                                                                //16 free slots

		private static final int SCROLLS        =                            xy(1, 3);  //16 slots
		public static final int SCROLL_UPGRADE  = SCROLLS+0;
		public static final int SCROLL_IDENTIFY = SCROLLS+1;
		public static final int SCROLL_REMCURSE = SCROLLS+2;
		public static final int SCROLL_MIRRORIMG= SCROLLS+3;
		public static final int SCROLL_RECHARGE = SCROLLS+4;
		public static final int SCROLL_TELEPORT = SCROLLS+5;
		public static final int SCROLL_LULLABY  = SCROLLS+6;
		public static final int SCROLL_MAGICMAP = SCROLLS+7;
		public static final int SCROLL_RAGE     = SCROLLS+8;
		public static final int SCROLL_RETRIB   = SCROLLS+9;
		public static final int SCROLL_TERROR   = SCROLLS+10;
		public static final int SCROLL_TRANSMUTE= SCROLLS+11;
		public static final int SCROLL_MAGICINFUSE    = SCROLLS+12;
		public static final int SCROLL_MULTIUPGRADE   = SCROLLS+13;
		public static final int SCROLL_REGROWTH       = SCROLLS+14;
		static {
			assignIconRect( SCROLL_UPGRADE,     7, 7 );
			assignIconRect( SCROLL_IDENTIFY,    4, 7 );
			assignIconRect( SCROLL_REMCURSE,    7, 7 );
			assignIconRect( SCROLL_MIRRORIMG,   7, 5 );
			assignIconRect( SCROLL_RECHARGE,    7, 5 );
			assignIconRect( SCROLL_TELEPORT,    7, 7 );
			assignIconRect( SCROLL_LULLABY,     7, 6 );
			assignIconRect( SCROLL_MAGICMAP,    7, 7 );
			assignIconRect( SCROLL_RAGE,        6, 6 );
			assignIconRect( SCROLL_RETRIB,      5, 6 );
			assignIconRect( SCROLL_TERROR,      5, 7 );
			assignIconRect( SCROLL_TRANSMUTE,   7, 7 );
			assignIconRect( SCROLL_MAGICINFUSE    , 5, 5 );
			assignIconRect( SCROLL_MULTIUPGRADE   , 5, 6 );
			assignIconRect( SCROLL_REGROWTH       , 5, 6 );
		}

		private static final int EXOTIC_SCROLLS =                            xy(1, 4);  //16 slots
		public static final int SCROLL_ENCHANT  = EXOTIC_SCROLLS+0;
		public static final int SCROLL_DIVINATE = EXOTIC_SCROLLS+1;
		public static final int SCROLL_ANTIMAGIC= EXOTIC_SCROLLS+2;
		public static final int SCROLL_PRISIMG  = EXOTIC_SCROLLS+3;
		public static final int SCROLL_MYSTENRG = EXOTIC_SCROLLS+4;
		public static final int SCROLL_PASSAGE  = EXOTIC_SCROLLS+5;
		public static final int SCROLL_SIREN    = EXOTIC_SCROLLS+6;
		public static final int SCROLL_FORESIGHT= EXOTIC_SCROLLS+7;
		public static final int SCROLL_CHALLENGE= EXOTIC_SCROLLS+8;
		public static final int SCROLL_PSIBLAST = EXOTIC_SCROLLS+9;
		public static final int SCROLL_DREAD    = EXOTIC_SCROLLS+10;
		public static final int SCROLL_METAMORPH= EXOTIC_SCROLLS+11;
		static {
			assignIconRect( SCROLL_ENCHANT,     7, 7 );
			assignIconRect( SCROLL_DIVINATE,    7, 6 );
			assignIconRect( SCROLL_ANTIMAGIC,   7, 7 );
			assignIconRect( SCROLL_PRISIMG,     5, 7 );
			assignIconRect( SCROLL_MYSTENRG,    7, 5 );
			assignIconRect( SCROLL_PASSAGE,     5, 7 );
			assignIconRect( SCROLL_SIREN,       7, 6 );
			assignIconRect( SCROLL_FORESIGHT,   7, 5 );
			assignIconRect( SCROLL_CHALLENGE,   7, 7 );
			assignIconRect( SCROLL_PSIBLAST,    5, 6 );
			assignIconRect( SCROLL_DREAD,       5, 7 );
			assignIconRect( SCROLL_METAMORPH,   7, 7 );
		}

		                                                                                //16 free slots

		private static final int POTIONS        =                            xy(1, 6);  //16 slots
		public static final int POTION_STRENGTH = POTIONS+0;
		public static final int POTION_HEALING  = POTIONS+1;
		public static final int POTION_MINDVIS  = POTIONS+2;
		public static final int POTION_FROST    = POTIONS+3;
		public static final int POTION_LIQFLAME = POTIONS+4;
		public static final int POTION_TOXICGAS = POTIONS+5;
		public static final int POTION_HASTE    = POTIONS+6;
		public static final int POTION_INVIS    = POTIONS+7;
		public static final int POTION_LEVITATE = POTIONS+8;
		public static final int POTION_PARAGAS  = POTIONS+9;
		public static final int POTION_PURITY   = POTIONS+10;
		public static final int POTION_EXP      = POTIONS+11;
		static {
			assignIconRect( POTION_STRENGTH,    7, 7 );
			assignIconRect( POTION_HEALING,     6, 7 );
			assignIconRect( POTION_MINDVIS,     7, 5 );
			assignIconRect( POTION_FROST,       7, 7 );
			assignIconRect( POTION_LIQFLAME,    5, 7 );
			assignIconRect( POTION_TOXICGAS,    7, 7 );
			assignIconRect( POTION_HASTE,       6, 6 );
			assignIconRect( POTION_INVIS,       5, 7 );
			assignIconRect( POTION_LEVITATE,    6, 7 );
			assignIconRect( POTION_PARAGAS,     7, 7 );
			assignIconRect( POTION_PURITY,      5, 7 );
			assignIconRect( POTION_EXP,         7, 7 );
		}

		private static final int EXOTIC_POTIONS =                            xy(1, 7);  //16 slots
		public static final int POTION_MASTERY  = EXOTIC_POTIONS+0;
		public static final int POTION_SHIELDING= EXOTIC_POTIONS+1;
		public static final int POTION_MAGISIGHT= EXOTIC_POTIONS+2;
		public static final int POTION_SNAPFREEZ= EXOTIC_POTIONS+3;
		public static final int POTION_DRGBREATH= EXOTIC_POTIONS+4;
		public static final int POTION_CORROGAS = EXOTIC_POTIONS+5;
		public static final int POTION_STAMINA  = EXOTIC_POTIONS+6;
		public static final int POTION_SHROUDFOG= EXOTIC_POTIONS+7;
		public static final int POTION_STRMCLOUD= EXOTIC_POTIONS+8;
		public static final int POTION_EARTHARMR= EXOTIC_POTIONS+9;
		public static final int POTION_CLEANSE  = EXOTIC_POTIONS+10;
		public static final int POTION_DIVINE   = EXOTIC_POTIONS+11;
		static {
			assignIconRect( POTION_MASTERY,     7, 7 );
			assignIconRect( POTION_SHIELDING,   6, 6 );
			assignIconRect( POTION_MAGISIGHT,   7, 5 );
			assignIconRect( POTION_SNAPFREEZ,   7, 7 );
			assignIconRect( POTION_DRGBREATH,   7, 7 );
			assignIconRect( POTION_CORROGAS,    7, 7 );
			assignIconRect( POTION_STAMINA,     6, 6 );
			assignIconRect( POTION_SHROUDFOG,   7, 7 );
			assignIconRect( POTION_STRMCLOUD,   7, 7 );
			assignIconRect( POTION_EARTHARMR,   6, 6 );
			assignIconRect( POTION_CLEANSE,     7, 7 );
			assignIconRect( POTION_DIVINE,      7, 7 );
		}

		                                                                                //16 free slots

		private static final int CUSTOM_POTIONS =                            xy(1, 9);  //64 slots
		public static final int POTION_MENDING        = CUSTOM_POTIONS+0;
		public static final int POTION_MIGHT          = CUSTOM_POTIONS+1;
		public static final int POTION_OVERHEALING    = CUSTOM_POTIONS+2;
		public static final int POTION_MANA           = CUSTOM_POTIONS+3;
		public static final int POTION_BALL           = CUSTOM_POTIONS+4;
		public static final int POTION_BLESSING       = CUSTOM_POTIONS+5;
		public static final int POTION_EYE            = CUSTOM_POTIONS+6;
		public static final int POTION_GLOWING        = CUSTOM_POTIONS+7;
		public static final int POTION_HONEY          = CUSTOM_POTIONS+8;
		public static final int POTION_PARASITES      = CUSTOM_POTIONS+9;
		public static final int POTION_PEANUTS        = CUSTOM_POTIONS+10;
		public static final int POTION_PEPPER         = CUSTOM_POTIONS+11;
		public static final int POTION_PROTECTION     = CUSTOM_POTIONS+12;
		public static final int POTION_REGROWTH       = CUSTOM_POTIONS+13;
		public static final int POTION_SHIELD         = CUSTOM_POTIONS+14;
		public static final int POTION_WINE           = CUSTOM_POTIONS+15;
		public static final int POTION_WITHERING      = CUSTOM_POTIONS+16;
		public static final int POTION_BANANA         = CUSTOM_POTIONS+17;
		public static final int POTION_HUNGER         = CUSTOM_POTIONS+18;
		public static final int POTION_MUSCLE         = CUSTOM_POTIONS+19;
		public static final int POTION_TOMATOSOUP     = CUSTOM_POTIONS+20;
		public static final int POTION_KIWI           = CUSTOM_POTIONS+21;
		public static final int POTION_LOVE           = CUSTOM_POTIONS+22;
		public static final int POTION_FLORA          = CUSTOM_POTIONS+23;
		public static final int POTION_SUN            = CUSTOM_POTIONS+24;
		public static final int POTION_WATER          = CUSTOM_POTIONS+25;
		public static final int POTION_GOO            = CUSTOM_POTIONS+26;
		public static final int POTION_INFECTION      = CUSTOM_POTIONS+27;
		public static final int POTION_VINE           = CUSTOM_POTIONS+28;
		public static final int POTION_HARVEST        = CUSTOM_POTIONS+29;
		public static final int POTION_DIRT           = CUSTOM_POTIONS+30;
		public static final int POTION_BUTTER         = CUSTOM_POTIONS+31;
		public static final int POTION_SODA           = CUSTOM_POTIONS+32;
		public static final int POTION_ULTRAVIOLET    = CUSTOM_POTIONS+33;
		public static final int POTION_DEW            = CUSTOM_POTIONS+34;
		public static final int POTION_SEED           = CUSTOM_POTIONS+35;
		public static final int POTION_DIGESTING      = CUSTOM_POTIONS+36;
		public static final int POTION_HYPNO          = CUSTOM_POTIONS+37;
		public static final int POTION_SLOWNESS       = CUSTOM_POTIONS+38;
		public static final int POTION_SMOKE          = CUSTOM_POTIONS+39;
		public static final int POTION_GRASS          = CUSTOM_POTIONS+40;
		public static final int POTION_EGG            = CUSTOM_POTIONS+41;
		public static final int POTION_TIME           = CUSTOM_POTIONS+42;
		public static final int POTION_LIGHTNING      = CUSTOM_POTIONS+43;
		public static final int POTION_LANTERN        = CUSTOM_POTIONS+44;
		public static final int POTION_SHADOWS        = CUSTOM_POTIONS+45;
		public static final int POTION_CHILLI         = CUSTOM_POTIONS+46;
		public static final int POTION_FIRESTORM      = CUSTOM_POTIONS+47;
		public static final int POTION_HYDROFIRE      = CUSTOM_POTIONS+48;
		public static final int POTION_ICESTORM       = CUSTOM_POTIONS+49;
		public static final int POTION_SNOWSTORM      = CUSTOM_POTIONS+50;
		public static final int POTION_STEAM          = CUSTOM_POTIONS+51;
		public static final int POTION_FIRELIGHT      = CUSTOM_POTIONS+52;
		static {
			assignIconRect( POTION_MENDING        , 6, 6 );
			assignIconRect( POTION_MIGHT          , 6, 6 );
			assignIconRect( POTION_OVERHEALING    , 7, 6 );
			assignIconRect( POTION_MANA           , 6, 5 );
			assignIconRect( POTION_BALL           , 6, 5 );
			assignIconRect( POTION_BLESSING       , 5, 6 );
			assignIconRect( POTION_EYE            , 6, 5 );
			assignIconRect( POTION_GLOWING        , 5, 5 );
			assignIconRect( POTION_HONEY          , 6, 6 );
			assignIconRect( POTION_PARASITES      , 7, 6 );
			assignIconRect( POTION_PEANUTS        , 5, 6 );
			assignIconRect( POTION_PEPPER         , 5, 6 );
			assignIconRect( POTION_PROTECTION     , 5, 5 );
			assignIconRect( POTION_REGROWTH       , 5, 6 );
			assignIconRect( POTION_SHIELD         , 5, 5 );
			assignIconRect( POTION_WINE           , 5, 6 );
			assignIconRect( POTION_WITHERING      , 4, 6 );
			assignIconRect( POTION_BANANA         , 5, 6 );
			assignIconRect( POTION_HUNGER         , 5, 6 );
			assignIconRect( POTION_MUSCLE         , 7, 3 );
			assignIconRect( POTION_TOMATOSOUP     , 5, 5 );
			assignIconRect( POTION_KIWI           , 6, 5 );
			assignIconRect( POTION_LOVE           , 7, 6 );
			assignIconRect( POTION_FLORA          , 5, 6 );
			assignIconRect( POTION_SUN            , 5, 5 );
			assignIconRect( POTION_WATER          , 7, 5 );
			assignIconRect( POTION_GOO            , 6, 5 );
			assignIconRect( POTION_INFECTION      , 5, 5 );
			assignIconRect( POTION_VINE           , 5, 6 );
			assignIconRect( POTION_HARVEST        , 5, 6 );
			assignIconRect( POTION_DIRT           , 6, 4 );
			assignIconRect( POTION_BUTTER         , 6, 4 );
			assignIconRect( POTION_SODA           , 6, 6 );
			assignIconRect( POTION_ULTRAVIOLET    , 5, 5 );
			assignIconRect( POTION_DEW            , 5, 6 );
			assignIconRect( POTION_SEED           , 5, 4 );
			assignIconRect( POTION_DIGESTING      , 6, 3 );
			assignIconRect( POTION_HYPNO          , 5, 5 );
			assignIconRect( POTION_SLOWNESS       , 6, 5 );
			assignIconRect( POTION_SMOKE          , 7, 4 );
			assignIconRect( POTION_GRASS          , 6, 4 );
			assignIconRect( POTION_EGG            , 4, 5 );
			assignIconRect( POTION_TIME           , 5, 6 );
			assignIconRect( POTION_LIGHTNING      , 5, 6 );
			assignIconRect( POTION_LANTERN        , 5, 6 );
			assignIconRect( POTION_SHADOWS        , 4, 5 );
			assignIconRect( POTION_CHILLI         , 5, 6 );
			assignIconRect( POTION_FIRESTORM      , 6, 6 );
			assignIconRect( POTION_HYDROFIRE      , 6, 6 );
			assignIconRect( POTION_ICESTORM       , 7, 7 );
			assignIconRect( POTION_SNOWSTORM      , 6, 5 );
			assignIconRect( POTION_STEAM          , 5, 6 );
			assignIconRect( POTION_FIRELIGHT      , 6, 6 );
		}

		private static final int CUSTOM_EXOTIC  =                            xy(1, 13); //64 slots
		public static final int POTION_ADRENALINE     = CUSTOM_EXOTIC+0;
		public static final int POTION_ALCOHOL        = CUSTOM_EXOTIC+1;
		public static final int POTION_ARMOR          = CUSTOM_EXOTIC+2;
		public static final int POTION_BUTTERBREAD    = CUSTOM_EXOTIC+3;
		public static final int POTION_CONTROL        = CUSTOM_EXOTIC+4;
		public static final int POTION_HOLYFUROR      = CUSTOM_EXOTIC+5;
		public static final int POTION_SPIRAL         = CUSTOM_EXOTIC+6;
		public static final int POTION_STOMACH        = CUSTOM_EXOTIC+7;
		public static final int POTION_ALLSEEING      = CUSTOM_EXOTIC+8;
		public static final int POTION_SUGAR          = CUSTOM_EXOTIC+9;
		public static final int POTION_SWELLING       = CUSTOM_EXOTIC+10;
		public static final int POTION_ATMOCOMPRESS   = CUSTOM_EXOTIC+11;
		public static final int POTION_NUTS           = CUSTOM_EXOTIC+12;
		public static final int POTION_AUTUMN         = CUSTOM_EXOTIC+13;
		public static final int POTION_BEACON         = CUSTOM_EXOTIC+14;
		public static final int POTION_BLEEDING       = CUSTOM_EXOTIC+15;
		public static final int POTION_BRAIN          = CUSTOM_EXOTIC+16;
		public static final int POTION_DEATH          = CUSTOM_EXOTIC+17;
		public static final int POTION_BALLLIGHTNING  = CUSTOM_EXOTIC+18;
		public static final int POTION_HIGHGRASS      = CUSTOM_EXOTIC+19;
		public static final int POTION_SUPERDEW       = CUSTOM_EXOTIC+20;
		public static final int POTION_HELLSTORM      = CUSTOM_EXOTIC+21;
		public static final int POTION_MAGICFIRE      = CUSTOM_EXOTIC+22;
		public static final int POTION_SOIL           = CUSTOM_EXOTIC+23;
		public static final int POTION_SUPERNOVA      = CUSTOM_EXOTIC+24;
		public static final int POTION_SOWING         = CUSTOM_EXOTIC+25;
		public static final int POTION_TSUNAMI        = CUSTOM_EXOTIC+26;
		public static final int POTION_PRESSURE       = CUSTOM_EXOTIC+27;
		public static final int POTION_ORB            = CUSTOM_EXOTIC+28;
		public static final int POTION_SLEEPPARALYSIS = CUSTOM_EXOTIC+29;
		public static final int POTION_RELATIVITY     = CUSTOM_EXOTIC+30;
		public static final int POTION_QUANTUMSOUP    = CUSTOM_EXOTIC+31;
		public static final int POTION_GLOOP          = CUSTOM_EXOTIC+32;
		public static final int POTION_FLOWER         = CUSTOM_EXOTIC+33;
		public static final int POTION_REPRODUCTION   = CUSTOM_EXOTIC+34;
		public static final int POTION_TERROR         = CUSTOM_EXOTIC+35;
		public static final int POTION_ABSOLUTEZERO   = CUSTOM_EXOTIC+36;
		public static final int POTION_ASH            = CUSTOM_EXOTIC+37;
		public static final int POTION_PLAGUE         = CUSTOM_EXOTIC+38;
		public static final int POTION_HAIL           = CUSTOM_EXOTIC+39;
		public static final int POTION_STARVING       = CUSTOM_EXOTIC+40;
		public static final int POTION_PROTAIN        = CUSTOM_EXOTIC+41;
		public static final int POTION_RADIATION      = CUSTOM_EXOTIC+42;
		public static final int POTION_TEARS          = CUSTOM_EXOTIC+43;
		public static final int POTION_SLIME          = CUSTOM_EXOTIC+44;
		public static final int POTION_HOTNESS        = CUSTOM_EXOTIC+45;
		public static final int POTION_BEE            = CUSTOM_EXOTIC+46;
		public static final int POTION_HOLY           = CUSTOM_EXOTIC+47;
		public static final int POTION_IMMORTALITY    = CUSTOM_EXOTIC+48;
		public static final int POTION_IRONSKIN       = CUSTOM_EXOTIC+49;
		public static final int POTION_LASERBEAM      = CUSTOM_EXOTIC+50;
		public static final int POTION_STRUNG         = CUSTOM_EXOTIC+51;
		public static final int POTION_WORM           = CUSTOM_EXOTIC+52;
		static {
			assignIconRect( POTION_ADRENALINE     , 5, 6 );
			assignIconRect( POTION_ALCOHOL        , 6, 5 );
			assignIconRect( POTION_ARMOR          , 5, 5 );
			assignIconRect( POTION_BUTTERBREAD    , 6, 5 );
			assignIconRect( POTION_CONTROL        , 5, 5 );
			assignIconRect( POTION_HOLYFUROR      , 5, 6 );
			assignIconRect( POTION_SPIRAL         , 6, 5 );
			assignIconRect( POTION_STOMACH        , 6, 6 );
			assignIconRect( POTION_ALLSEEING      , 6, 4 );
			assignIconRect( POTION_SUGAR          , 7, 3 );
			assignIconRect( POTION_SWELLING       , 7, 7 );
			assignIconRect( POTION_ATMOCOMPRESS   , 7, 7 );
			assignIconRect( POTION_NUTS           , 7, 5 );
			assignIconRect( POTION_AUTUMN         , 5, 6 );
			assignIconRect( POTION_BEACON         , 5, 6 );
			assignIconRect( POTION_BLEEDING       , 6, 5 );
			assignIconRect( POTION_BRAIN          , 7, 6 );
			assignIconRect( POTION_DEATH          , 6, 6 );
			assignIconRect( POTION_BALLLIGHTNING  , 6, 5 );
			assignIconRect( POTION_HIGHGRASS      , 6, 5 );
			assignIconRect( POTION_SUPERDEW       , 6, 6 );
			assignIconRect( POTION_HELLSTORM      , 6, 6 );
			assignIconRect( POTION_MAGICFIRE      , 6, 6 );
			assignIconRect( POTION_SOIL           , 7, 4 );
			assignIconRect( POTION_SUPERNOVA      , 7, 7 );
			assignIconRect( POTION_SOWING         , 7, 6 );
			assignIconRect( POTION_TSUNAMI        , 7, 6 );
			assignIconRect( POTION_PRESSURE       , 6, 5 );
			assignIconRect( POTION_ORB            , 5, 5 );
			assignIconRect( POTION_SLEEPPARALYSIS , 7, 6 );
			assignIconRect( POTION_RELATIVITY     , 5, 6 );
			assignIconRect( POTION_QUANTUMSOUP    , 6, 5 );
			assignIconRect( POTION_GLOOP          , 5, 6 );
			assignIconRect( POTION_FLOWER         , 6, 6 );
			assignIconRect( POTION_REPRODUCTION   , 7, 4 );
			assignIconRect( POTION_TERROR         , 6, 6 );
			assignIconRect( POTION_ABSOLUTEZERO   , 5, 6 );
			assignIconRect( POTION_ASH            , 7, 4 );
			assignIconRect( POTION_PLAGUE         , 6, 6 );
			assignIconRect( POTION_HAIL           , 6, 6 );
			assignIconRect( POTION_STARVING       , 6, 6 );
			assignIconRect( POTION_PROTAIN        , 4, 4 );
			assignIconRect( POTION_RADIATION      , 7, 5 );
			assignIconRect( POTION_TEARS          , 6, 6 );
			assignIconRect( POTION_SLIME          , 5, 4 );
			assignIconRect( POTION_HOTNESS        , 5, 6 );
			assignIconRect( POTION_BEE            , 5, 5 );
			assignIconRect( POTION_HOLY           , 5, 6 );
			assignIconRect( POTION_IMMORTALITY    , 5, 6 );
			assignIconRect( POTION_IRONSKIN       , 5, 5 );
			assignIconRect( POTION_LASERBEAM      , 7, 3 );
			assignIconRect( POTION_STRUNG         , 4, 6 );
			assignIconRect( POTION_WORM           , 5, 5 );
		}

	}


	//Re-ARranged PD ported items (cells appended below row 59 of items.png)
	private static final int REARRANGED   =                            xy(1, 60);   //29 slots
	public static final int ASSASSINS_SPEAR     = REARRANGED+0;
	public static final int BEAM_SABER          = REARRANGED+1;
	public static final int BIBLE               = REARRANGED+2;
	public static final int CHAIN_FLAIL         = REARRANGED+3;
	public static final int CHAIN_WHIP          = REARRANGED+4;
	public static final int CROSS               = REARRANGED+5;
	public static final int DEATHS_SWORD        = REARRANGED+6;
	public static final int DUAL_GREATSWORD     = REARRANGED+7;
	public static final int EVOLUTION           = REARRANGED+8;
	public static final int FORCE_GLOVE         = REARRANGED+9;
	public static final int HOLYSWORD           = REARRANGED+10;
	public static final int HOLYSWORD_TRUE      = REARRANGED+11;
	public static final int HUGE_SWORD          = REARRANGED+12;
	public static final int KNIFE               = REARRANGED+13;
	public static final int LANCE               = REARRANGED+14;
	public static final int LANCE_N_SHIELD      = REARRANGED+15;
	public static final int LARGE_SHORD         = REARRANGED+16;
	public static final int MEISTER_HAMMER      = REARRANGED+17;
	public static final int OBSIDIAN_SHIELD     = REARRANGED+18;
	public static final int POCKET_KNIFE        = REARRANGED+19;
	public static final int SABER               = REARRANGED+20;
	public static final int SCALPEL             = REARRANGED+21;
	public static final int SHARP_KATANA        = REARRANGED+22;
	public static final int SPEAR_N_SHIELD      = REARRANGED+23;
	public static final int THUNDERBOLT         = REARRANGED+24;
	public static final int TRUE_RUNIC_BLADE    = REARRANGED+25;
	public static final int UNFORMED_BLADE      = REARRANGED+26;
	public static final int UNHOLY_BIBLE        = REARRANGED+27;
	public static final int UPGRADE_DUST        = REARRANGED+28;
	static {
		assignItemRect(ASSASSINS_SPEAR     , 16, 16);
		assignItemRect(BEAM_SABER          , 16, 15);
		assignItemRect(BIBLE               , 13, 16);
		assignItemRect(CHAIN_FLAIL         , 16, 16);
		assignItemRect(CHAIN_WHIP          , 14, 14);
		assignItemRect(CROSS               , 14, 14);
		assignItemRect(DEATHS_SWORD        , 14, 16);
		assignItemRect(DUAL_GREATSWORD     , 16, 16);
		assignItemRect(EVOLUTION           , 10, 15);
		assignItemRect(FORCE_GLOVE         , 13, 15);
		assignItemRect(HOLYSWORD           , 16, 16);
		assignItemRect(HOLYSWORD_TRUE      , 16, 16);
		assignItemRect(HUGE_SWORD          , 16, 16);
		assignItemRect(KNIFE               , 12, 13);
		assignItemRect(LANCE               , 15, 15);
		assignItemRect(LANCE_N_SHIELD      , 16, 15);
		assignItemRect(LARGE_SHORD         , 14, 16);
		assignItemRect(MEISTER_HAMMER      , 16, 16);
		assignItemRect(OBSIDIAN_SHIELD     , 12, 16);
		assignItemRect(POCKET_KNIFE        , 11, 12);
		assignItemRect(SABER               , 13, 15);
		assignItemRect(SCALPEL             , 13, 13);
		assignItemRect(SHARP_KATANA        , 12, 16);
		assignItemRect(SPEAR_N_SHIELD      , 16, 15);
		assignItemRect(THUNDERBOLT         , 13, 13);
		assignItemRect(TRUE_RUNIC_BLADE    , 14, 14);
		assignItemRect(UNFORMED_BLADE      , 14, 15);
		assignItemRect(UNHOLY_BIBLE        , 13, 16);
		assignItemRect(UPGRADE_DUST        , 15, 11);
	}

	//Re-ARranged PD ported items, round 2 (guns/bow/mining)
	private static final int REARRANGED2  =                            xy(1, 62);   //12 slots
	public static final int FT_T5               = REARRANGED2+0;
	public static final int LG_T5               = REARRANGED2+1;
	public static final int NO_BULLET           = REARRANGED2+2;
	public static final int NORMAL_ARROW        = REARRANGED2+3;
	public static final int BOW                 = REARRANGED2+4;
	public static final int ADVENTURER_SHOVEL   = REARRANGED2+5;
	public static final int BATTLE_SHOVEL       = REARRANGED2+6;
	public static final int MINERS_TOOL         = REARRANGED2+7;
	public static final int BULLET              = REARRANGED2+8;
	public static final int ARROW_BAG           = REARRANGED2+9;
	public static final int GUNSMITHING_TOOL    = REARRANGED2+10;
	public static final int BULLET_BELT         = REARRANGED2+11;
	static {
		assignItemRect(FT_T5               , 14, 15);
		assignItemRect(LG_T5               , 14, 16);
		assignItemRect(NO_BULLET           , 0, 0);
		assignItemRect(NORMAL_ARROW        , 14, 14);
		assignItemRect(BOW                 , 15, 15);
		assignItemRect(ADVENTURER_SHOVEL   , 16, 16);
		assignItemRect(BATTLE_SHOVEL       , 16, 16);
		assignItemRect(MINERS_TOOL         , 16, 16);
		assignItemRect(BULLET              , 13, 13);
		assignItemRect(ARROW_BAG           , 14, 15);
		assignItemRect(GUNSMITHING_TOOL    , 16, 13);
		assignItemRect(BULLET_BELT         , 15, 15);
	}
}

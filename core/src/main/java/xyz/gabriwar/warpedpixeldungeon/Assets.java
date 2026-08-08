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

package xyz.gabriwar.warpedpixeldungeon;

public class Assets {

	public static class Effects {
		public static final String EFFECTS      = "effects/effects.png";
		public static final String FIREBALL     = "effects/fireball.png";
		public static final String SPECKS       = "effects/specks.png";
		public static final String SPELL_ICONS  = "effects/spell_icons.png";
		public static final String TEXT_ICONS   = "effects/text_icons.png";
	}

	public static class Environment {
		public static final String TERRAIN_FEATURES = "environment/terrain_features.png";

		public static final String VISUAL_GRID  = "environment/visual_grid.png";
		public static final String WALL_BLOCKING= "environment/wall_blocking.png";

		public static final String TILES_SEWERS = "environment/tiles_sewers.png";
		public static final String TILES_PRISON = "environment/tiles_prison.png";
		public static final String TILES_CAVES  = "environment/tiles_caves.png";

						//Spider Nest branch (Remixed PD reimplementation)
		public static final String TILES_SPIDERNEST = "environment/tiles_spidernest.png";

//Temple sub-region (Re-ARranged port)
		public static final String TILES_TEMPLE  = "environment/tiles_temple.png";
		public static final String WATER_TEMPLE  = "environment/water_temple.png";

//Frozen region (Unleashed PD port; tileset recoloured from caves)
		public static final String TILES_FROZEN  = "environment/tiles_frozen.png";
		public static final String WATER_FROZEN  = "environment/water_frozen.png";
		public static final String TILES_CITY   = "environment/tiles_city.png";
		public static final String TILES_HALLS  = "environment/tiles_halls.png";

		public static final String TILES_CAVES_CRYSTAL  = "environment/tiles_caves_crystal.png";
		public static final String TILES_CAVES_GNOLL    = "environment/tiles_caves_gnoll.png";

		// Sprouted custom tilesets — TODO: need pixel art in modern Shattered layout (256x256)
		// Cursed PD tilesets are incompatible (different tile index layout)
		// For now, levels use closest matching standard tileset as fallback
		public static final String TILES_FOREST     = TILES_SEWERS;  // TODO: green/natural theme
		//the overworld: the ported forest sheet with the log walls turned to rock
		public static final String TILES_OVERWORLD  = "environment/tiles_overworld.png";
		public static final String TILES_BEACH      = TILES_SEWERS;  // TODO: sand/beach theme
		public static final String TILES_TOWN       = TILES_CITY;    // TODO: village theme
		public static final String TILES_TOWN_REMIXED = "environment/tiles_town_remixed.png";
		public static final String TILES_TOWN_SUMMER  = "environment/tiles_town_summer.png";
		public static final String OVERWORLD_DRESS    = "environment/overworld_dress.png";
		public static final String OVERWORLD_VILLAGES = "environment/overworld_villages.png";
		public static final String TILES_TOWN_INSIDE  = "environment/tiles_town_inside.png";
		public static final String TILES_TOWN_BLANK   = "environment/tiles_town_blank.png";
		public static final String TILES_VAULT      = TILES_CITY;    // TODO: golden/vault theme
		public static final String TILES_SKELETON   = TILES_HALLS;   // TODO: bone/dark theme
		public static final String TILES_MAGIC_CAVE = TILES_HALLS;   // TODO: purple/magic theme

		public static final String WATER_SEWERS = "environment/water0.png";
		public static final String WATER_PRISON = "environment/water1.png";
		public static final String WATER_CAVES  = "environment/water2.png";
		public static final String WATER_CITY   = "environment/water3.png";
		public static final String WATER_HALLS  = "environment/water4.png";

		// Frozen water overlays (only for real water regions, not lava)
		// City (water3) and Halls (water4) are lava — they never freeze
		public static final String FROZEN_SEWERS = "environment/frozen0.png";
		public static final String FROZEN_PRISON = "environment/frozen1.png";
		public static final String FROZEN_CAVES  = "environment/frozen2.png";

		public static final String WEAK_FLOOR       = "environment/custom_tiles/weak_floor.png";
		public static final String SEWER_BOSS       = "environment/custom_tiles/sewer_boss.png";
		public static final String PRISON_QUEST     = "environment/custom_tiles/prison_quest.png";
		public static final String PRISON_EXIT      = "environment/custom_tiles/prison_exit.png";
		public static final String CAVES_QUEST      = "environment/custom_tiles/caves_quest.png";
		public static final String CAVES_BOSS       = "environment/custom_tiles/caves_boss.png";
		public static final String CITY_QUEST        = "environment/custom_tiles/city_quest.png";
		public static final String CITY_BOSS        = "environment/custom_tiles/city_boss.png";
		public static final String HALLS_SP         = "environment/custom_tiles/halls_special.png";

		public static final String PORTAL_GATE      = "environment/portal_gate.png";
	}
	
	//TODO include other font assets here? Some are platform specific though...
	public static class Fonts {
		public static final String PIXELFONT= "fonts/pixel_font.png";
	}

	public static class Interfaces {
		public static final String ARCS_BG  = "interfaces/arcs1.png";
		public static final String ARCS_FG  = "interfaces/arcs2.png";

		public static final String BANNERS  = "interfaces/banners.png";
		public static final String BADGES   = "interfaces/badges.png";
		public static final String LOCKED   = "interfaces/locked_badge.png";

		public static final String CHROME   = "interfaces/chrome.png";
		public static final String ICONS    = "interfaces/icons.png";
		public static final String STATUS   = "interfaces/status_pane.png";
		public static final String MENU     = "interfaces/menu_pane.png";
		public static final String MENU_BTN = "interfaces/menu_button.png";
		public static final String TOOLBAR  = "interfaces/toolbar.png";
		public static final String SHADOW   = "interfaces/shadow.png";
		public static final String BOSSHP   = "interfaces/boss_hp.png";
		public static final String PORTAL_ICON = "interfaces/portal_icon.png";
		public static final String APP_ICON = "interfaces/app_icon.png";

		public static final String SURFACE  = "interfaces/surface.png";

		public static final String BUFFS_SMALL      = "interfaces/buffs.png";
		public static final String BUFFS_LARGE      = "interfaces/large_buffs.png";

		//sun, 24 interpolated moon phases, solar + lunar eclipse; 9x9 frames
		public static final String SUNDIAL_TOGGLE   = "interfaces/sundial_toggle.png";

		public static final String TALENT_ICONS     = "interfaces/talent_icons.png";
		public static final String TALENT_BUTTON    = "interfaces/talent_button.png";

		public static final String HERO_ICONS       = "interfaces/hero_icons.png";

		public static final String RADIAL_MENU      = "interfaces/radial_menu.png";
	}

	//these points to resource bundles, not raw asset files
	public static class Messages {
		public static final String ACTORS   = "messages/actors/actors";
		public static final String GUIDE    = "messages/guide/guide";
		public static final String ITEMS    = "messages/items/items";
		public static final String JOURNAL  = "messages/journal/journal";
		public static final String LEVELS   = "messages/levels/levels";
		public static final String MISC     = "messages/misc/misc";
		public static final String PLANTS   = "messages/plants/plants";
		public static final String SCENES   = "messages/scenes/scenes";
		public static final String SKILLS   = "messages/skills/skills";
		public static final String UI       = "messages/ui/ui";
		public static final String WINDOWS  = "messages/windows/windows";
	}

	public static class Music {
		public static final String THEME_1              = "music/theme_1.ogg";
		public static final String THEME_2              = "music/theme_2.ogg";
		public static final String THEME_FINALE         = "music/theme_finale.ogg";

		public static final String SEWERS_1             = "music/sewers_1.ogg";
		public static final String SEWERS_2             = "music/sewers_2.ogg";
		public static final String SEWERS_3             = "music/sewers_3.ogg";
		public static final String SEWERS_TENSE         = "music/sewers_tense.ogg";
		public static final String SEWERS_BOSS          = "music/sewers_boss.ogg";

		public static final String PRISON_1             = "music/prison_1.ogg";
		public static final String PRISON_2             = "music/prison_2.ogg";
		public static final String PRISON_3             = "music/prison_3.ogg";
		public static final String PRISON_TENSE         = "music/prison_tense.ogg";
		public static final String PRISON_BOSS          = "music/prison_boss.ogg";

		public static final String CAVES_1              = "music/caves_1.ogg";
		public static final String CAVES_2              = "music/caves_2.ogg";
		public static final String CAVES_3              = "music/caves_3.ogg";
		public static final String CAVES_TENSE          = "music/caves_tense.ogg";
		public static final String CAVES_BOSS           = "music/caves_boss.ogg";
		public static final String CAVES_BOSS_FINALE    = "music/caves_boss_finale.ogg";

		public static final String CITY_1               = "music/city_1.ogg";
		public static final String CITY_2               = "music/city_2.ogg";
		public static final String CITY_3               = "music/city_3.ogg";
		public static final String CITY_TENSE           = "music/city_tense.ogg";
		public static final String CITY_BOSS            = "music/city_boss.ogg";
		public static final String CITY_BOSS_FINALE     = "music/city_boss_finale.ogg";

		public static final String HALLS_1              = "music/halls_1.ogg";
		public static final String HALLS_2              = "music/halls_2.ogg";
		public static final String HALLS_3              = "music/halls_3.ogg";
		public static final String HALLS_TENSE          = "music/halls_tense.ogg";
		public static final String HALLS_BOSS           = "music/halls_boss.ogg";
		public static final String HALLS_BOSS_FINALE    = "music/halls_boss_finale.ogg";
	}

	public static class Sounds {
		public static final String CLICK    = "sounds/click.mp3";
		public static final String BADGE    = "sounds/badge.mp3";
		public static final String GOLD     = "sounds/gold.mp3";

		public static final String OPEN     = "sounds/door_open.mp3";
		public static final String UNLOCK   = "sounds/unlock.mp3";
		public static final String ITEM     = "sounds/item.mp3";
		public static final String DEWDROP  = "sounds/dewdrop.mp3";
		public static final String STEP     = "sounds/step.mp3";
		public static final String WATER    = "sounds/water.mp3";
		public static final String GRASS    = "sounds/grass.mp3";
		public static final String TRAMPLE  = "sounds/trample.mp3";
		public static final String STURDY   = "sounds/sturdy.mp3";

		public static final String HIT              = "sounds/hit.mp3";
		public static final String MISS             = "sounds/miss.mp3";
		public static final String HIT_SLASH        = "sounds/hit_slash.mp3";
		public static final String HIT_STAB         = "sounds/hit_stab.mp3";
		public static final String HIT_CRUSH        = "sounds/hit_crush.mp3";
		public static final String HIT_MAGIC        = "sounds/hit_magic.mp3";
		public static final String HIT_STRONG       = "sounds/hit_strong.mp3";
		public static final String HIT_PARRY        = "sounds/hit_parry.mp3";
		public static final String HIT_ARROW        = "sounds/hit_arrow.mp3";
		public static final String ATK_SPIRITBOW    = "sounds/atk_spiritbow.mp3";
		public static final String ATK_CROSSBOW     = "sounds/atk_crossbow.mp3";
		public static final String HEALTH_WARN      = "sounds/health_warn.mp3";
		public static final String HEALTH_CRITICAL  = "sounds/health_critical.mp3";

		public static final String DESCEND  = "sounds/descend.mp3";
		public static final String EAT      = "sounds/eat.mp3";
		public static final String READ     = "sounds/read.mp3";
		public static final String LULLABY  = "sounds/lullaby.mp3";
		public static final String DRINK    = "sounds/drink.mp3";
		public static final String SHATTER  = "sounds/shatter.mp3";
		public static final String ZAP      = "sounds/zap.mp3";
		public static final String LIGHTNING= "sounds/lightning.mp3";
		public static final String LEVELUP  = "sounds/levelup.mp3";
		public static final String DEATH    = "sounds/death.mp3";
		public static final String CHALLENGE= "sounds/challenge.mp3";
		public static final String CURSED   = "sounds/cursed.mp3";
		public static final String TRAP     = "sounds/trap.mp3";
		public static final String EVOKE    = "sounds/evoke.mp3";
		public static final String TOMB     = "sounds/tomb.mp3";
		public static final String ALERT    = "sounds/alert.mp3";
		public static final String MELD     = "sounds/meld.mp3";
		public static final String BOSS     = "sounds/boss.mp3";
		public static final String BLAST    = "sounds/blast.mp3";
		public static final String PLANT    = "sounds/plant.mp3";
		public static final String RAY      = "sounds/ray.mp3";
		public static final String BEACON   = "sounds/beacon.mp3";
		public static final String TELEPORT = "sounds/teleport.mp3";
		public static final String CHARMS   = "sounds/charms.mp3";
		public static final String MASTERY  = "sounds/mastery.mp3";
		public static final String PUFF     = "sounds/puff.mp3";
		public static final String ROCKS    = "sounds/rocks.mp3";
		public static final String ROCKS_LIGHT = "sounds/rocks_light.mp3";
		public static final String BURNING  = "sounds/burning.mp3";
		public static final String FALLING  = "sounds/falling.mp3";
		public static final String GHOST    = "sounds/ghost.mp3";
		public static final String SECRET   = "sounds/secret.mp3";
		public static final String BONES    = "sounds/bones.mp3";
		public static final String BEE      = "sounds/bee.mp3";
		public static final String DEGRADE  = "sounds/degrade.mp3";
		public static final String MIMIC    = "sounds/mimic.mp3";
		public static final String DEBUFF   = "sounds/debuff.mp3";
		public static final String CHARGEUP = "sounds/chargeup.mp3";
		public static final String GAS      = "sounds/gas.mp3";
		public static final String CHAINS   = "sounds/chains.mp3";
		public static final String SCAN     = "sounds/scan.mp3";
		public static final String SHEEP    = "sounds/sheep.mp3";
		public static final String MINE    = "sounds/mine.mp3";

		public static final String[] all = new String[]{
				CLICK, BADGE, GOLD,

				OPEN, UNLOCK, ITEM, DEWDROP, STEP, WATER, GRASS, TRAMPLE, STURDY,

				HIT, MISS, HIT_SLASH, HIT_STAB, HIT_CRUSH, HIT_MAGIC, HIT_STRONG, HIT_PARRY,
				HIT_ARROW, ATK_SPIRITBOW, ATK_CROSSBOW, HEALTH_WARN, HEALTH_CRITICAL,

				DESCEND, EAT, READ, LULLABY, DRINK, SHATTER, ZAP, LIGHTNING, LEVELUP, DEATH,
				CHALLENGE, CURSED, TRAP, EVOKE, TOMB, ALERT, MELD, BOSS, BLAST, PLANT, RAY, BEACON,
				TELEPORT, CHARMS, MASTERY, PUFF, ROCKS, ROCKS_LIGHT, BURNING, FALLING, GHOST, SECRET, BONES,
				BEE, DEGRADE, MIMIC, DEBUFF, CHARGEUP, GAS, CHAINS, SCAN, SHEEP, MINE
		};
	}

	public static class Splashes {
		public static final String WARRIOR  = "splashes/warrior.jpg";
		public static final String MAGE     = "splashes/mage.jpg";
		public static final String ROGUE    = "splashes/rogue.jpg";
		public static final String HUNTRESS = "splashes/huntress.jpg";
		public static final String DUELIST  = "splashes/duelist.jpg";
		public static final String CLERIC   = "splashes/cleric.jpg";

		public static final String SEWERS   = "splashes/sewers.jpg";
		public static final String PRISON   = "splashes/prison.jpg";
		public static final String CAVES    = "splashes/caves.jpg";
		public static final String CITY     = "splashes/city.jpg";
		public static final String HALLS    = "splashes/halls.jpg";

		public static class Title {
			public static final String ARCHS         = "splashes/title/archs.png";
			public static final String BACK_CLUSTERS = "splashes/title/back_clusters.png";
			public static final String MID_MIXED     = "splashes/title/mid_mixed.png";
			public static final String FRONT_SMALL   = "splashes/title/front_small.png";
		}
	}

	public static class Sprites {
		public static final String ITEMS        = "sprites/items.png";
		public static final String ITEM_ICONS   = "sprites/item_icons.png";

		public static final String WARRIOR  = "sprites/warrior.png";
		public static final String MAGE     = "sprites/mage.png";
		public static final String ROGUE    = "sprites/rogue.png";
		public static final String HUNTRESS = "sprites/huntress.png";
		public static final String DUELIST  = "sprites/duelist.png";
		public static final String CLERIC   = "sprites/cleric.png";
		public static final String AVATARS  = "sprites/avatars.png";
		public static final String HERO_SKILLS = "sprites/hero_skills.png";
		public static final String PET      = "sprites/pet.png";
		public static final String AMULET   = "sprites/amulet.png";

		public static final String RAT      = "sprites/rat.png";

				public static final String NEW_SENTRY     = "sprites/new_sentry.png";

		//Remixed PD Ice Caves mobs
		public static final String RM_COLD_SPIRIT       = "sprites/rm_cold_spirit.png";
		public static final String RM_CAGED_KOBOLD      = "sprites/rm_caged_kobold.png";
		public static final String RM_ICE_GUARDIAN      = "sprites/rm_ice_guardian.png";
		public static final String RM_SPIDER_SERVANT     = "sprites/rm_spider_servant.png";
		public static final String RM_SPIDER_MIND        = "sprites/rm_spider_mind.png";
		public static final String RM_SPIDER_QUEEN       = "sprites/rm_spider_queen.png";
		public static final String RM_SPIDER_EXPLODING   = "sprites/rm_spider_exploding.png";
		public static final String RM_SPIDER_EGG         = "sprites/rm_spider_egg.png";
		//Remixed PD town folk
		public static final String RM_BARD             = "sprites/rm_town_townsfolk_bard.png";
		public static final String RM_BISHOP           = "sprites/rm_town_bishop3.png";
		public static final String RM_DRUNKARD         = "sprites/rm_town_townsfolk_drunkard.png";
		public static final String RM_FORTUNE_TELLER   = "sprites/rm_fortuneteller_npc.png";
		public static final String RM_INN_KEEPER       = "sprites/rm_town_townsfolk_innkeeper.png";
		public static final String RM_LIBRARIAN        = "sprites/rm_town_librarian.png";
		public static final String RM_MERCENARY        = "sprites/rm_town_townsfolk_mercenary.png";
		public static final String RM_INN_SERVANT      = "sprites/rm_town_townsfolk_servant.png";
		public static final String RM_EMPLOYEE         = "sprites/rm_town_service_man.png";
		public static final String RM_TOWN_GUARD_RM    = "sprites/rm_guards.png";
		public static final String RM_TOWNSFOLK_MOVIE  = "sprites/rm_town_townsfolk_man_brown.png";
		public static final String RM_TOWNSFOLK        = "sprites/rm_town_townsfolk_man.png";
		public static final String RM_TOWNSFOLK_SILENT = "sprites/rm_town_townsfolk_man_bald.png";
		public static final String RM_KOBOLD_ICEMANCER  = "sprites/rm_kobold_icemancer.png";

//Unleashed PD ported mob sprites
		public static final String UL_KING         = "sprites/ul_king.png";
		public static final String UL_CHAOS        = "sprites/chaos_mage.png";
		public static final String UL_LOSTSOUL     = "sprites/lost_souls.png";
		public static final String UL_MINOTAUR     = "sprites/minotaur.png";
		public static final String UL_EVILLORD     = "sprites/demonlord.png";
		public static final String UL_ICEDEMON     = "sprites/icedemon.png";
		public static final String UL_SPIDERBOT    = "sprites/spiderbot.png";
		public static final String UL_WOLF         = "sprites/wolf.png";
		public static final String UL_SQUID        = "sprites/squid.png";
		public static final String UL_ZOMBIE       = "sprites/zombie.png";
		public static final String UL_ELEMENTALS2  = "sprites/elementals2.png";
		public static final String UL_GOLEM        = "sprites/ul_golem.png";
		public static final String UL_SWARM        = "sprites/ul_swarm.png";
		public static final String UL_LARVA        = "sprites/ul_larva.png";
		public static final String UL_SLIME        = "sprites/ul_slime.png";
		public static final String BEETLE   = "sprites/beetle.png";
		public static final String BRUTE    = "sprites/brute.png";
		public static final String SPINNER  = "sprites/spinner.png";
		public static final String DM300    = "sprites/dm300.png";
		public static final String WRAITH   = "sprites/wraith.png";
		public static final String UNDEAD   = "sprites/undead.png";
		public static final String KING     = "sprites/king.png";
		public static final String PIRANHA  = "sprites/piranha.png";
		public static final String EYE      = "sprites/eye.png";
		public static final String GNOLL    = "sprites/gnoll.png";
		public static final String CRAB     = "sprites/crab.png";
		public static final String GOO      = "sprites/goo.png";
		public static final String SWARM    = "sprites/swarm.png";
		public static final String SKELETON = "sprites/skeleton.png";
		public static final String SHAMAN   = "sprites/shaman.png";
		public static final String THIEF    = "sprites/thief.png";
		public static final String TENGU    = "sprites/tengu.png";
		public static final String SHEEP    = "sprites/sheep.png";
		public static final String KEEPER   = "sprites/shopkeeper.png";
		public static final String BAT      = "sprites/bat.png";
		public static final String ELEMENTAL= "sprites/elemental.png";
		public static final String MONK     = "sprites/monk.png";
		public static final String WARLOCK  = "sprites/warlock.png";
		public static final String GOLEM    = "sprites/golem.png";
		public static final String STATUE   = "sprites/statue.png";
		public static final String SUCCUBUS = "sprites/succubus.png";
		public static final String SCORPIO  = "sprites/scorpio.png";
		public static final String FISTS    = "sprites/yog_fists.png";
		public static final String YOG      = "sprites/yog.png";
		public static final String LARVA    = "sprites/larva.png";
		public static final String GHOST    = "sprites/ghost.png";
		public static final String MAKER    = "sprites/wandmaker.png";
		public static final String TROLL    = "sprites/blacksmith.png";
		public static final String IMP      = "sprites/demon.png";
		public static final String RATKING  = "sprites/ratking.png";
		public static final String BEE      = "sprites/bee.png";
		public static final String BANANASPIDER = "sprites/bananaspider.png";
		public static final String COCOCLAM     = "sprites/cococlam.png";
		public static final String MIMIC    = "sprites/mimic.png";
		public static final String ROT_LASH = "sprites/rot_lasher.png";
		public static final String ROT_HEART= "sprites/rot_heart.png";
		public static final String GUARD    = "sprites/guard.png";
		public static final String WARDS    = "sprites/wards.png";
		public static final String GUARDIAN = "sprites/guardian.png";
		public static final String SLIME    = "sprites/slime.png";
		public static final String SNAKE    = "sprites/snake.png";
		public static final String NECRO    = "sprites/necromancer.png";
		public static final String GHOUL    = "sprites/ghoul.png";
		public static final String RIPPER   = "sprites/ripper.png";
		public static final String SPAWNER  = "sprites/spawner.png";
		public static final String DM100    = "sprites/dm100.png";
		public static final String PYLON    = "sprites/pylon.png";
		public static final String DM200    = "sprites/dm200.png";
		public static final String LOTUS    = "sprites/lotus.png";
		public static final String NINJA_LOG        = "sprites/ninja_log.png";
		public static final String SPIRIT_HAWK      = "sprites/spirit_hawk.png";
		public static final String RED_SENTRY       = "sprites/red_sentry.png";
		public static final String CRYSTAL_WISP     = "sprites/crystal_wisp.png";
		public static final String CRYSTAL_GUARDIAN = "sprites/crystal_guardian.png";
		public static final String CRYSTAL_SPIRE    = "sprites/crystal_spire.png";
		public static final String GNOLL_ARCHER     = "sprites/gnoll_archer.png";
		public static final String GNOLL_GUARD      = "sprites/gnoll_guard.png";
		public static final String GNOLL_SAPPER     = "sprites/gnoll_sapper.png";
		public static final String GNOLL_GEOMANCER  = "sprites/gnoll_geomancer.png";
		public static final String FUNGAL_SPINNER   = "sprites/fungal_spinner.png";
		public static final String FUNGAL_SENTRY    = "sprites/fungal_sentry.png";
		public static final String FUNGAL_CORE      = "sprites/fungal_core.png";

		public static final String MR_DESTRUCTO    = "sprites/mrdestructo.png";
		public static final String MR_DESTRUCTO2   = "sprites/mrdestructo2.png";
		public static final String SEEKING_BOMB    = "sprites/seekingbomb.png";
		public static final String ALBINO_PIRANHA  = "sprites/albinopiranha.png";
		public static final String ORB_OF_ZOT      = "sprites/orbofzot.png";
		public static final String GREY_ONI        = "sprites/greyoni.png";
		public static final String ONI             = "sprites/oni.png";
		public static final String FOSSIL_SKELETON = "sprites/fossilskeleton.png";
		public static final String MOSSY_SKELETON  = "sprites/mossyskeleton.png";
		public static final String BLUE_WRAITH     = "sprites/bluewraith.png";
		public static final String RED_WRAITH      = "sprites/redwraith.png";
		public static final String GOLD_THIEF      = "sprites/goldthief.png";
		public static final String SPECTRAL_RAT    = "sprites/spectralrat.png";
		public static final String POISON_GOO      = "sprites/poisongoo.png";
		public static final String DEMON_GOO       = "sprites/demongoo.png";
		public static final String GREY_RAT        = "sprites/greyrat.png";
		public static final String BROWN_BAT       = "sprites/brownbat.png";
		public static final String STEEL_BEE       = "sprites/steelbee.png";
		public static final String RAT_BOSS        = "sprites/ratboss.png";
		public static final String BANDIT_KING     = "sprites/banditking.png";
		public static final String THIEF_KING      = "sprites/thiefking.png";
		public static final String MONSTER_BOX     = "sprites/monsterbox.png";
		public static final String KUPUA           = "sprites/kupua.png";
		public static final String GULLIN          = "sprites/gullin.png";
		public static final String TOWER           = "sprites/tower.png";
		public static final String LIT_TOWER       = "sprites/littower.png";
		public static final String SENTINEL        = "sprites/sentinel.png";
		public static final String MAGIC_EYE       = "sprites/magiceye.png";
		public static final String BROKEN_ROBOT    = "sprites/brokenrobot.png";
		public static final String DEW_PROTECTOR   = "sprites/dewprotector.png";
		public static final String FLYING_PROTECTOR = "sprites/flyingprotector.png";
		public static final String ASSASSIN        = "sprites/assassin.png";
		public static final String ADULT_DRAGON_VIOLET = "sprites/adultdragonviolet.png";
		public static final String CRAB_KING       = "sprites/crabking.png";
		public static final String SKELETON_KING   = "sprites/skeletonking.png";
		public static final String SKELETON_HAND   = "sprites/skeletonhand.png";
		public static final String DWARF_LICH      = "sprites/dwarflich.png";
		public static final String DWARF_KING_TOMB = "sprites/dwarfkingtomb.png";
		public static final String SHELL           = "sprites/shell.png";
		public static final String OTILUKE_STONE   = "sprites/otilukestone.png";
		public static final String OTILUKE_NPC     = "sprites/otiluke.png";
		public static final String TINKERER        = "sprites/tinkerer.png";
		public static final String SOKOBAN_SHEEP   = "sprites/sokobansheep.png";
		public static final String ZOT             = "sprites/zot.png";
		public static final String ZOT_PHASE       = "sprites/zotphase.png";
		public static final String SHADOW_YOG      = "sprites/shadowyog.png";

		public static final String PET_DRAGON     = "sprites/pet_dragon.png";
		public static final String FAIRY          = "sprites/fairy.png";
		public static final String BUNNY          = "sprites/bunny.png";
		public static final String VELOCIROOSTER  = "sprites/velocirooster.png";

		public static final String LIVING_PLANTS  = "mobs/livingplants.png";
		public static final String PITCHER_PLANT  = "mobs/pitcherplant.png";
		public static final String MAGIC_ORB      = "mobs/magicorb.png";
	}
}

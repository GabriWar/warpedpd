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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Degrade;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drowsy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.LifeLink;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Ooze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoakedShoes;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Acidic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoPiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BlueWraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.BrownWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ChaosMage;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ClayGolem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ColdSpirit;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DemonGoo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DemonLord;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DemonSpawner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKingTomb;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfLich;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Elemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eye;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FishProtector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.FlyingProtector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ForestProtector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Ghoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GnollArcher;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GoldThief;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Golem;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GraveProtector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GrayWolf;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceDemon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceGuardian;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceGuardianCore;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.KoboldIcemancer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Larva;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LostSoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Minotaur;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MossySkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Oni;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Piranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RipperDemon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Scorpio;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Senior;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Sentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShadowYog;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpectralRat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.SpiderBot;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Succubus;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tengu;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tinkerer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.VaultProtector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Warlock;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Wraith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Yeti;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogDzewa;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogFist;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Blacksmith;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.CagedKobold;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Ghost;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Imp;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.ImpShopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer3;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.VaultTokenDoor;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.quest.vault.VaultBossElemental;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.quest.vault.VaultSkeleton;
import xyz.gabriwar.warpedpixeldungeon.items.Amulet;
import xyz.gabriwar.warpedpixeldungeon.items.AncientCoin;
import xyz.gabriwar.warpedpixeldungeon.items.Ankh;
import xyz.gabriwar.warpedpixeldungeon.items.ArmorKit;
import xyz.gabriwar.warpedpixeldungeon.items.Bone;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfDead;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfLife;
import xyz.gabriwar.warpedpixeldungeon.items.BookOfTranscendence;
import xyz.gabriwar.warpedpixeldungeon.items.CavesKey;
import xyz.gabriwar.warpedpixeldungeon.items.CityKey;
import xyz.gabriwar.warpedpixeldungeon.items.ConchShell;
import xyz.gabriwar.warpedpixeldungeon.items.Dewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.KingsCrown;
import xyz.gabriwar.warpedpixeldungeon.items.Mushroom;
import xyz.gabriwar.warpedpixeldungeon.items.OtilukesJournal;
import xyz.gabriwar.warpedpixeldungeon.items.PrisonKey;
import xyz.gabriwar.warpedpixeldungeon.items.RedDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.Rice;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarah;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarahDeath;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarahLife;
import xyz.gabriwar.warpedpixeldungeon.items.SanChikarahTranscend;
import xyz.gabriwar.warpedpixeldungeon.items.SewersKey;
import xyz.gabriwar.warpedpixeldungeon.items.Stylus;
import xyz.gabriwar.warpedpixeldungeon.items.TenguKey;
import xyz.gabriwar.warpedpixeldungeon.items.Torch;
import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.items.YellowDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ScaleArmor;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.CandleOfMindVision;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.ClimateCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.LloydsBeacon;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.DumplingBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.FishingBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.HolyHandGrenade;
import xyz.gabriwar.warpedpixeldungeon.items.food.Berry;
import xyz.gabriwar.warpedpixeldungeon.items.food.GoldenNut;
import xyz.gabriwar.warpedpixeldungeon.items.food.Nut;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.FadedSafeSpotPage;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.Sokoban4;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IceKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.WornKey;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfExperience;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfLevitation;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMending;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfStrength;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DwarfToken;
import xyz.gabriwar.warpedpixeldungeon.items.quest.EscapeCrystal;
import xyz.gabriwar.warpedpixeldungeon.items.quest.ImpStatue;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfIdentify;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicalInfusion;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTransmutation;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfUpgrade;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfAugmentation;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.LokisFlail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ForestDart;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.RiceBall;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Shuriken;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.BlazingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.CorrosionTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.CursingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.DisintegrationTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FlashingTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.FrostTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GrimTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.GuardianTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.RockfallTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.StormTrap;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.WarpingTrap;
import xyz.gabriwar.warpedpixeldungeon.plants.Phaseshift;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.Icons;
import com.watabou.noosa.Image;

//Cites for the last regions: the dwarven city, the demon halls and their frozen halls, and the postgame.
public final class LateCites {

	private LateCites(){}

	public static void register( CiteBook book ){
		city( book );
		cityBoss( book );
		halls( book );
		frozen( book );
		postgame( book );
	}

	//the city's chasms, seen from the floor at their edge
	private static final int CHASM = DungeonTileSheet.CHASM_FLOOR;

	//the frozen halls' own stairway (BranchStairs.Frozen: the third frame of the branch stairs sheet)
	private static Image frozenStairs(){
		Image i = new Image( Assets.Environment.BRANCH_STAIRS );
		i.frame( i.texture.uvRectBySize( 32, 0, 16, 16 ) );
		return i;
	}

	private static void city( CiteBook book ){
		book.page( "city_overview" )
				.mob( Ghoul.class, "dwarven ghoul", "ghoul" )
				.mob( Warlock.class, "dwarf warlock", "warlock" )
				.mob( Monk.class, "dwarf monk", "monk" )
				.mob( Elemental.FireElemental.class, "elemental" )
				.mob( Golem.class, "golem" )
				.mob( Minotaur.class, "minotaur" )
				.mob( Tinkerer.class, "mad tinkerer", "tinkerer" )
				.mob( Shopkeeper.class, "shop" )
				.mob( Imp.class, "Ambitious Imp" )
				.mob( DwarfKing.class, "King of Dwarves", "Dwarf King" )
				.buff( Heatstroke.class, "heatstroke" )
				.effect( GuideEffect.FIRE, "fire" )
				.trap( FrostTrap.class, "frost trap", "frost" )
				.trap( StormTrap.class, "storm trap", "storm" )
				.trap( CorrosionTrap.class, "corrosion trap", "corrosion" )
				.trap( BlazingTrap.class, "blazing trap", "blazing" )
				.trap( DisintegrationTrap.class, "disintegration trap", "disintegration" )
				.trap( RockfallTrap.class, "rockfall trap", "rockfall" )
				.trap( FlashingTrap.class, "flashing trap", "flashing" )
				.trap( GuardianTrap.class, "guardian trap", "guardian" )
				.trap( WarpingTrap.class, "warping trap", "warping" )
				.trap( CursingTrap.class, "cursing trap", "cursing" )
				.mob( BlueWraith.class, "blue wraith" )
				.mob( DwarfLich.class, "dwarf lich" );

		book.page( "city_f1" )
				.mob( Ghoul.class, "dwarven ghoul", "Ghouls" ).primary()
				.mob( Shopkeeper.class, "shop" )
				.item( ScaleArmor.class, "scale armor" )
				.sprite( ItemSpriteSheet.LOCKED_CHEST, "golden portable chest" )
				.item( StoneOfAugmentation.class, "Stone of Augmentation" )
				.item( Ankh.class, "Ankh" )
				.item( PotionOfHealing.class, "Potion of Healing" )
				.item( ScrollOfIdentify.class, "Scroll of Identify", "Scrolls of Identify" )
				.item( ScrollOfRemoveCurse.class, "Scroll of Remove Curse", "Remove Curse" )
				.item( ScrollOfMagicMapping.class, "Scroll of Magic Mapping", "Magic Mapping" )
				.terrain( Assets.Environment.TILES_CITY, CHASM, "chasm" )
				.mob( Elemental.FireElemental.class, "fire elemental", "Elementals" )
				.mob( Elemental.FrostElemental.class, "frost elemental", "frost" )
				.mob( Elemental.ShockElemental.class, "shock elemental", "shock" )
				.mob( Elemental.ChaosElemental.class, "chaos elemental" )
				.item( ScrollOfTransmutation.class, "Scroll of Transmutation" )
				.mob( Warlock.class, "dwarf warlock", "Warlocks" )
				.buff( Degrade.class, "degraded", "degrade" )
				.mob( Tinkerer.class, "mad tinkerer", "Tinkerers" )
				.item( ScrollOfMagicalInfusion.class, "Scroll of Magical Infusion" )
				.mob( ClayGolem.class, "clay golem" )
				.mob( SpiderBot.class, "metal spider", "spider bot" );

		book.page( "city_f2" )
				.mob( Monk.class, "dwarf monk", "Monks" ).primary()
				.buff( Monk.Focus.class, "focus" )
				.mob( Senior.class, "senior monk" )
				.mob( Elemental.FireElemental.class, "fire elemental", "elementals" )
				.mob( Elemental.FrostElemental.class, "frost elemental" )
				.mob( Elemental.ShockElemental.class, "shock elemental" )
				.water( Assets.Environment.WATER_CITY, "water" )
				.buff( Frost.class, "frozen", "freezes" )
				.mob( Imp.class, "Ambitious Imp" );

		book.page( "city_vault" )
				.item( ImpStatue.class, "obsidian statuette" ).primary()
				.mob( Imp.class, "Ambitious Imp" )
				.terrain( Assets.Environment.TILES_CITY, CHASM, "pit" )
				.item( ClothArmor.class, "cloth armor" )
				.item( EscapeCrystal.class, "Escape Crystal" )
				.item( Gold.class, "gold" )
				.mob( VaultSkeleton.class, "dwarven skeleton", "guard" )
				.item( DwarfToken.class, "dwarf token" )
				.mob( VaultTokenDoor.class, "strange door", "token door" )
				.item( PotionOfHealing.class, "Potion of Healing", "healing potion" )
				.mob( VaultBossElemental.class, "greater elemental", "vault elemental" )
				.sprite( ItemSpriteSheet.ARTIFACT_HOLDER, "artifact" )
				.sprite( ItemSpriteSheet.RING_HOLDER, "ring" )
				.sprite( ItemSpriteSheet.WEAPON_HOLDER, "weapon" )
				.item( PlateArmor.class, "plate armor" )
				.sprite( ItemSpriteSheet.WAND_HOLDER, "wand" )
				.mob( ImpShopkeeper.class, "shop" )
				.mob( DwarfKing.class, "King of Dwarves", "Dwarf King" );

		book.page( "city_f3" )
				.mob( Golem.class, "golem" ).primary()
				.buff( Terror.class, "terrified", "terror" )
				.buff( Sleep.class, "asleep", "sleep" )
				.buff( Amok.class, "amok" )
				.mob( Minotaur.class, "minotaur" )
				.buff( Bleeding.class, "bleeding" )
				.buff( Cripple.class, "crippled" )
				.mob( DwarfLich.class, "dwarf lich" )
				.item( PotionOfHealing.class, "Potion of Healing", "healing potion" )
				.mob( Imp.class, "Ambitious Imp", "imp" );

		book.page( "city_f4" )
				.mob( GoldThief.class, "crazy gold thief", "gold thief" ).primary()
				.mob( Golem.class, "golem" )
				.mob( Monk.class, "dwarf monk", "monk" )
				.mob( Warlock.class, "dwarf warlock", "warlock" )
				.mob( Elemental.FireElemental.class, "elemental" )
				.mob( Minotaur.class, "minotaur" )
				.item( CityKey.class, "City Ancient Key" )
				.item( Gold.class, "gold", "coins" )
				.item( Shuriken.class, "shuriken" )
				.mob( Succubus.class, "succubus" )
				.mob( Imp.class, "Ambitious Imp", "imp" )
				.item( PotionOfStrength.class, "Potion of Strength", "strength potion" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade", "upgrade scroll" )
				.item( Stylus.class, "arcane stylus", "stylus" );

		book.page( "city_f5" )
				.terrain( Assets.Environment.TILES_CITY, DungeonTileSheet.FLAT_BOOKSHELF, "bookshelves" )
				.terrain( Assets.Environment.TILES_CITY, DungeonTileSheet.PEDESTAL, "summoning pedestal", "pedestal" )
				.mob( DwarfKing.class, "King of Dwarves", "Dwarf King" )
				.sprite( ItemSpriteSheet.REMAINS, "remains", "fallen hero" )
				.item( Ankh.class, "Ankh" )
				.mob( Ghoul.class, "dwarven ghoul", "ghoul" )
				.effect( GuideEffect.GREEN_FIRE, "green flame" )
				.mob( Monk.class, "dwarf monk", "monk" )
				.mob( Warlock.class, "dwarf warlock", "warlock" )
				.mob( Golem.class, "golem" )
				.terrain( Assets.Environment.TILES_CITY, CHASM, "chasm" )
				.mob( ImpShopkeeper.class, "ambitious imp", "imp" )
				.item( PlateArmor.class, "plate armor" )
				.item( Torch.class, "torch" )
				.sprite( ItemSpriteSheet.WEAPON_HOLDER, "tier 5 weapon", "tier 5 weapons" )
				.sprite( ItemSpriteSheet.CRYSTAL_CHEST, "crystal portable chest" )
				.item( BookOfDead.class, "book of the dead" )
				.item( BookOfLife.class, "book of the living", "living" )
				.item( BookOfTranscendence.class, "book of the heavens", "heavens" );
	}

	private static void cityBoss( CiteBook book ){
		book.page( "boss_dwarfking" )
				.mob( DwarfKing.class, "King of Dwarves", "Dwarf King" )
				.mob( Ghoul.class, "dwarven ghoul", "ghoul" )
				.mob( Monk.class, "dwarf monk", "monk" )
				.mob( Warlock.class, "dwarf warlock", "warlock" )
				.terrain( Assets.Environment.TILES_CITY, DungeonTileSheet.PEDESTAL, "pedestal" )
				.buff( LifeLink.class, "life link", "link his life" )
				.item( KingsCrown.class, "Dwarf King's crown", "King's Crown" )
				.item( Sokoban4.class, "sokoban puzzles page", "Sokoban Puzzles page" )
				.item( OtilukesJournal.class, "Otiluke's Journal" )
				.item( LloydsBeacon.class, "Lloyd's Beacon" )
				.buff( Degrade.class, "degraded", "degrade" )
				//the three phases are headings: what each does is in its paragraph's cites
				.plain( "Phase 1", "Phase 2", "Phase 3" );

		book.page( "boss_dwarftomb" )
				.mob( DwarfKingTomb.class, "dwarf king tomb", "tomb" )
				.mob( DwarfKing.class, "King of Dwarves", "Dwarf King" )
				.mob( DwarfLich.class, "dwarf lich" )
				.mob( Wraith.class, "wraith" )
				.item( Gold.class, "gold" )
				.item( SkeletonKey.class, "skeleton key" )
				.item( ArmorKit.class, "armor kit" )
				.item( KingsCrown.class, "Dwarf King's crown", "crown" );
	}

	private static void halls( CiteBook book ){
		book.page( "halls_overview" )
				.item( Torch.class, "torch", "two torches" )
				.mob( DemonSpawner.class, "demon spawner" )
				.mob( RipperDemon.class, "ripper demon", "ripper" )
				.mob( YogDzewa.class, "Yog-Dzewa" )
				.mob( Larva.class, "god's larva", "larva" )
				.buff( Heatstroke.class, "heatstroke" )
				.effect( GuideEffect.FIRE, "fire" )
				.water( Assets.Environment.WATER_HALLS, "cold lava" )
				.icon( Icons.ALCHEMY, "laboratory" )
				.trap( GrimTrap.class, "grim trap" )
				.mob( BlueWraith.class, "blue wraith" )
				.mob( SpectralRat.class, "spectral rat" )
				.item( Amulet.class, "Amulet of Yendor" );

		book.page( "halls_f1" )
				.mob( Succubus.class, "succubus", "Succubi" ).primary()
				.buff( Charm.class, "charmed", "charm" )
				.mob( Eye.class, "evil eye" )
				.mob( ChaosMage.class, "chaos mage" )
				.buff( Vertigo.class, "vertigo", "confuse" )
				.buff( Poison.class, "poisoned", "poison" )
				.buff( Burning.class, "burning", "burn" )
				.buff( Blindness.class, "blinded", "blind" )
				.buff( Slow.class, "slowed", "slow" )
				.item( PotionOfExperience.class, "Potion of Experience" )
				.mob( DemonSpawner.class, "demon spawner" )
				.mob( RipperDemon.class, "ripper demon", "ripper" )
				.item( PotionOfHealing.class, "Potion of Healing" );

		book.page( "halls_f2" )
				.mob( DemonSpawner.class, "demon spawner" ).primary()
				.mob( Succubus.class, "succubus" )
				.mob( Eye.class, "evil eye" )
				.mob( ChaosMage.class, "chaos mage" )
				.mob( RipperDemon.class, "ripper demon", "ripper" )
				.item( Bomb.class, "bomb" )
				.effect( GuideEffect.FIRE, "fire" )
				.item( PotionOfHealing.class, "Potion of Healing" )
				.custom( LateCites::frozenStairs, "Frozen Halls" );

		book.page( "halls_f3" )
				.mob( Scorpio.class, "scorpio" ).primary()
				.buff( Cripple.class, "crippled", "cripple" )
				.item( PotionOfHealing.class, "Potion of Healing", "healing potion" )
				.mob( Acidic.class, "acidic scorpio" )
				.buff( Ooze.class, "caustic ooze", "oozes" )
				.sprite( ItemSpriteSheet.WAND_HOLDER, "wand" )
				.mob( LostSoul.class, "lost soul" )
				.buff( Burning.class, "burning", "burn" )
				.effect( GuideEffect.FIRE, "fire" )
				.buff( Frost.class, "frozen", "frost" )
				.buff( Chill.class, "chilled", "chill" )
				.water( Assets.Environment.WATER_HALLS, "water", "cold lava" )
				.mob( DemonGoo.class, "demon goo" )
				.item( PotionOfMending.class, "Potion of Mending" )
				.mob( DemonSpawner.class, "demon spawner", "spawner" )
				.mob( RipperDemon.class, "ripper demon", "ripper" )
				.icon( Icons.ALCHEMY, "laboratory" );

		book.page( "halls_f4" )
				.mob( Scorpio.class, "scorpio" )
				.mob( Eye.class, "evil eye" )
				.mob( Succubus.class, "succubus" )
				.mob( LostSoul.class, "lost soul" )
				.mob( DemonGoo.class, "demon goo" )
				.buff( Invisibility.class, "invisible", "invisibility" )
				.mob( DemonSpawner.class, "demon spawner", "spawner" )
				.mob( RipperDemon.class, "ripper demon", "ripper" )
				.mob( YogDzewa.class, "Yog-Dzewa" )
				.mob( Larva.class, "god's larva", "larva" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade", "Scrolls of Upgrade" )
				.item( PotionOfHealing.class, "Potion of Healing", "healing" )
				.item( Torch.class, "torch" );

		book.page( "halls_f5" )
				.mob( YogDzewa.class, "Yog-Dzewa", "Yog" )
				.sprite( ItemSpriteSheet.REMAINS, "remains", "fallen hero" )
				.item( Torch.class, "torch" )
				.water( Assets.Environment.WATER_HALLS, "water" )
				.mob( YogFist.BurningFist.class, "burning fist" )
				.mob( YogFist.RottingFist.class, "rotting fist" )
				.item( Amulet.class, "Amulet of Yendor", "Amulet" );

		book.page( "halls_end" )
				.item( Amulet.class, "Amulet of Yendor", "Amulet" ).primary()
				.terrain( Assets.Environment.TILES_HALLS, DungeonTileSheet.PEDESTAL, "pedestal" )
				.buff( AscensionChallenge.class, "amulet's curse", "Ascension", "curse" )
				.buff( Slow.class, "slowed" );

		book.page( "boss_yog" )
				.mob( YogDzewa.class, "Yog-Dzewa", "Yog" )
				.mob( YogFist.BurningFist.class, "burning fist", "Burning" )
				.mob( YogFist.SoiledFist.class, "soiled fist", "Soiled" )
				.mob( YogFist.RottingFist.class, "rotting fist", "Rotting" )
				.mob( YogFist.RustedFist.class, "rusted fist", "Rusted" )
				.mob( YogFist.BrightFist.class, "bright fist", "Bright" )
				.mob( YogFist.DarkFist.class, "dark fist", "Dark" )
				.mob( RipperDemon.class, "ripper demon", "ripper" )
				.mob( DemonSpawner.class, "demon spawner" )
				.mob( Larva.class, "god's larva", "larvae", "larva" )
				.buff( Frost.class, "frozen", "frost" )
				.water( Assets.Environment.WATER_HALLS, "water" )
				.effect( GuideEffect.FIRE, "fire" )
				.terrain( Assets.Environment.TILES_HALLS, DungeonTileSheet.FLAT_HIGH_GRASS, "tall grass" )
				.buff( Bleeding.class, "bleeding" )
				.buff( Blindness.class, "blinded", "blinds" )
				.item( Torch.class, "torch" )
				//headings of the page's parts: the beams, the summons and the fists are each their paragraph's
				.plain( "Beams", "Summons", "Fists" );
	}

	private static void frozen( CiteBook book ){
		book.page( "frozen_overview" )
				.custom( LateCites::frozenStairs, "Frozen Halls", "frost-rimmed opening" )
				.mob( DemonLord.class, "demon lord" )
				.buff( Hypothermia.class, "hypothermia" )
				.buff( SoakedShoes.class, "soaked shoes", "soaks your boots" )
				.mob( BrownWolf.class, "brown wolf", "brown" )
				.mob( GrayWolf.class, "gray wolf", "gray" )
				.mob( Yeti.class, "yeti", "yetis" )
				.mob( ClayGolem.class, "clay golem" )
				.mob( ColdSpirit.class, "cold spirit" )
				.mob( KoboldIcemancer.class, "kobold icemancer" )
				.mob( CagedKobold.class, "caged kobold" )
				.item( IceKey.class, "ice key" )
				.item( CandleOfMindVision.class, "Candle of Mind Vision" )
				.mob( IceGuardianCore.class, "ice guardian core" )
				.mob( IceGuardian.class, "ice guardian", "guardian" )
				.item( WandOfFrost.class, "Wand of Frost" );

		book.page( "boss_demonlord" )
				//the page calls him only "he"
				.mob( DemonLord.class, "demon lord", "he" )
				.buff( Chill.class, "chilled", "chill" )
				.water( Assets.Environment.WATER_FROZEN, "water" )
				.terrain( Assets.Environment.TILES_FROZEN, DungeonTileSheet.FLAT_WALL_DECO, "ice pillar" )
				.mob( IceDemon.class, "ice demon" )
				.buff( Sleep.class, "asleep", "sleep" )
				.buff( Terror.class, "terrified", "terror" )
				.buff( Vertigo.class, "vertigo" )
				.buff( Burning.class, "burning" )
				.item( WornKey.class, "worn key" )
				.item( ClimateCrystal.class, "climate crystal" )
				//a heading: his breath is the paragraph's own words
				.plain( "Breath" );
	}

	private static void postgame( CiteBook book ){
		book.page( "postgame_overview" )
				.item( SewersKey.class, "ancient key", "key" )
				.item( BookOfDead.class, "book of the dead", "book" )
				.mob( GnollArcher.class, "gnoll archer", "archer" )
				.mob( MossySkeleton.class, "moss covered skeleton", "skeleton" )
				.mob( GoldThief.class, "crazy gold thief", "thieves" )
				.sprite( ItemSpriteSheet.CHEST, "supply chest" )
				.item( Gold.class, "gold" )
				.item( PotionOfStrength.class, "Potion of Strength" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" )
				.item( SanChikarah.class, "SanChikarah" )
				.terrain( Assets.Environment.TILES_CITY, DungeonTileSheet.PEDESTAL, "exit pedestal" )
				.mob( Blacksmith.class, "troll blacksmith", "blacksmith" )
				.mob( SpectralRat.class, "spectral rat" );

		book.page( "pg_field" )
				.mob( GnollArcher.class, "gnoll archer" ).primary()
				.item( SewersKey.class, "Sewers Ancient Key" )
				.mob( Ghost.class, "sad ghost" )
				.mob( ForestProtector.class, "forest protector" )
				.item( ForestDart.class, "forest dart" )
				.item( FadedSafeSpotPage.class, "faded safe spot page", "Safe Spot Page" )
				.item( OtilukesJournal.class, "Otiluke's Journal" )
				.mob( Tengu.class, "Tengu" )
				.item( TenguKey.class, "Tengu Ancient Key" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.FLAT_HIGH_GRASS, "tall grass" )
				.item( Nut.class, "dungeon nut", "nuts" )
				.item( Berry.class, "berry" )
				.item( Dewdrop.class, "dewdrop", "dew" );

		book.page( "pg_battle" )
				.mob( MossySkeleton.class, "moss covered skeleton", "skeleton" ).primary()
				.item( PrisonKey.class, "Prison Ancient Key" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.item( HolyHandGrenade.class, "holy hand grenade" )
				.buff( Terror.class, "terrified", "terrifies" )
				.mob( GraveProtector.class, "grave protector" )
				.item( Bone.class, "inscribed bone" )
				.item( YellowDewdrop.class, "yellow dewdrop", "yellow dew" )
				.item( RedDewdrop.class, "red dewdrop", "red" )
				.item( GoldenNut.class, "golden dungeon nut" );

		book.page( "pg_fishing" )
				.mob( AlbinoPiranha.class, "albino giant piranha" ).primary()
				.mob( Piranha.class, "giant piranha", "piranha" )
				.item( CavesKey.class, "Caves Ancient Key" )
				.water( Assets.Environment.WATER_PRISON, "water" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.item( FishingBomb.class, "fishing bomb" )
				.item( PotionOfLevitation.class, "Potion of Levitation" )
				.mob( FishProtector.class, "fish protector" )
				.item( ConchShell.class, "conch shell" )
				.item( Berry.class, "berry" )
				.item( LokisFlail.class, "Loki's Flail" )
				.buff( Invisibility.class, "invisible" );

		book.page( "pg_vault" )
				.mob( GoldThief.class, "crazy gold thief", "gold thief" ).primary()
				.item( CityKey.class, "City Ancient Key" )
				.item( Gold.class, "gold", "coins" )
				.item( Shuriken.class, "shuriken" )
				.mob( VaultProtector.class, "vault protector" )
				.item( AncientCoin.class, "Ancient Coin" )
				.mob( GnollArcher.class, "gnoll archer" )
				.mob( MossySkeleton.class, "moss covered skeleton" )
				.mob( AlbinoPiranha.class, "albino giant piranha", "albino piranha" )
				.item( GoldenNut.class, "golden dungeon nut" );

		book.page( "pg_catacomb" )
				.mob( BlueWraith.class, "blue wraith" ).primary()
				.mob( ImpShopkeeper.class, "ambitious imp", "imp" )
				.item( BookOfDead.class, "Book of the Dead" )
				.item( BookOfLife.class, "Book of the Living" )
				.item( BookOfTranscendence.class, "Book of the Heavens" )
				.item( Amulet.class, "Amulet of Yendor" )
				.terrain( Assets.Environment.TILES_SEWERS, DungeonTileSheet.PEDESTAL, "pedestal" )
				.item( SanChikarahDeath.class, "SanChikarah of Death" )
				.buff( Vertigo.class, "vertigo" )
				.buff( Terror.class, "terrified", "terror" )
				.item( HolyHandGrenade.class, "Holy Hand Grenade" )
				.item( RedDewdrop.class, "red dewdrop", "red dew" )
				.mob( DwarfLich.class, "dwarf lich" )
				.item( PotionOfHealing.class, "Potion of Healing" )
				.sprite( ItemSpriteSheet.CHEST, "chest" )
				.sprite( ItemSpriteSheet.REMAINS, "remains" )
				.item( Gold.class, "gold" )
				.item( PotionOfStrength.class, "Potion of Strength" )
				.item( ScrollOfUpgrade.class, "Scroll of Upgrade" );

		book.page( "pg_fortress" )
				.mob( Oni.class, "oni" ).primary()
				.item( BookOfLife.class, "Book of the Living" )
				.buff( Terror.class, "terrified", "terror" )
				.buff( Amok.class, "amok" )
				.item( Rice.class, "grain of magic rice", "magic rice" )
				.item( RiceBall.class, "rice ball" )
				.buff( Drowsy.class, "drowsy" )
				.item( DumplingBomb.class, "dumpling bomb" )
				.mob( Tinkerer3.class, "tinkerer" )
				.item( Mushroom.class, "toadstool mushroom" )
				.item( Waterskin.class, "waterskin" )
				.item( Dewdrop.class, "dewdrop", "dew" )
				.item( SanChikarahLife.class, "SanChikarah of Life" )
				.terrain( Assets.Environment.TILES_CITY, DungeonTileSheet.PEDESTAL, "exit pedestal" );

		book.page( "pg_chasm" )
				.mob( Sentinel.class, "sentinel" ).primary()
				.item( BookOfTranscendence.class, "Book of the Heavens" )
				.terrain( Assets.Environment.TILES_PRISON, CHASM, "pit" )
				.item( PotionOfLevitation.class, "Potion of Levitation" )
				.item( Phaseshift.Seed.class, "seed of phaseshift", "phaseshift seed" )
				.sprite( ItemSpriteSheet.WEAPON_HOLDER, "weapon" )
				.mob( FlyingProtector.class, "flying protector" )
				.buff( Bleeding.class, "bleeding" )
				.buff( Cripple.class, "crippled" )
				.terrain( Assets.Environment.TILES_PRISON, DungeonTileSheet.PEDESTAL, "exit pedestal" )
				.item( SanChikarahTranscend.class, "SanChikarah of Transcendence" )
				.item( SanChikarah.class, "SanChikarah" )
				.mob( Blacksmith.class, "troll blacksmith", "blacksmith" )
				.mob( ShadowYog.class, "Shadow Yog-Dzewa", "Shadow Yog" );
	}
}

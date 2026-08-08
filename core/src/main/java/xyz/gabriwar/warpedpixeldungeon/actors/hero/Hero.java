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

package xyz.gabriwar.warpedpixeldungeon.actors.hero;

import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfSearching;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Bones;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.SacrificialFire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AdrenalineSurge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ArtifactRecharge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Awareness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barkskin;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Berserk;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ArmorEnhance;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Dewcharge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Combo;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Crouching;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drowsy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Foresight;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.GreaterHaste;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.HeroDisguise;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Coughing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heavy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.HoldFast;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleepiness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invulnerability;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Levitation;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.LostInventory;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Momentum;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MonkEnergy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.PhysicalEmpower;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Recharging;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ManaRegen;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Regeneration;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SnipersMark;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.TimeStasis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drunk;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoakedShoes;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AuroraBless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BloodMoonBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FireflyGlow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.RainbowBlessing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SolarEclipseBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpringBloom;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Strength;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WeaponEnhance;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Windswept;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.ArmorAbility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.Challenge;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.warrior.Endure;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.BodyForm;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HallowedGround;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyWard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyWeapon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.Smite;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mimic;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets.PET;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Snake;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.CheckedCell;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.Ankh;
import xyz.gabriwar.warpedpixeldungeon.items.Dewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.EasterEgg;
import xyz.gabriwar.warpedpixeldungeon.items.Egg;
import xyz.gabriwar.warpedpixeldungeon.items.EquipableItem;
import xyz.gabriwar.warpedpixeldungeon.items.Heap.Type;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.OtilukesJournal;
import xyz.gabriwar.warpedpixeldungeon.items.ShadowDragonEgg;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClassArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Stone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Viscosity;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.AlchemistsToolkit;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.CapeOfThorns;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.CloakOfShadows;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.DriedRose;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.EtherealChains;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.HolyTome;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.HornOfPlenty;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.MasterThievesArmband;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TalismanOfForesight;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TimekeepersHourglass;
import xyz.gabriwar.warpedpixeldungeon.items.bags.MagicalHolster;
import xyz.gabriwar.warpedpixeldungeon.items.journal.Guidebook;
import xyz.gabriwar.warpedpixeldungeon.items.keys.CrystalKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.GoldenKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.GoldenSkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.items.keys.IronKey;
import xyz.gabriwar.warpedpixeldungeon.items.keys.Key;
import xyz.gabriwar.warpedpixeldungeon.items.keys.WornKey;
import xyz.gabriwar.warpedpixeldungeon.items.misc.AutoPotion;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfExperience;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs.ElixirOfMight;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfDivineInspiration;
import xyz.gabriwar.warpedpixeldungeon.items.quest.DarkGold;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfAccuracy;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfEvasion;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfFuror;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfHaste;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMight;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfTenacity;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.Scroll;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfChallenge;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ThirteenLeafClover;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfLivingEarth;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Crossbow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.DeathSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.LargeSword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Quarterstaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.UnholyBible;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RoundShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sai;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Scimitar;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WornShortsword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.journal.Document;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.MiningLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.special.WeakFloorRoom;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.mechanics.ShadowCaster;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.AlchemyScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.AttackIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.QuickSlotButton;
import xyz.gabriwar.warpedpixeldungeon.ui.StatusPane;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndHero;
import xyz.gabriwar.warpedpixeldungeon.windows.WndResurrect;
import xyz.gabriwar.warpedpixeldungeon.windows.WndTradeItem;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

public class Hero extends Char {

	{
		actPriority = HERO_PRIO;
		
		alignment = Alignment.ALLY;
	}
	
	public static final int MAX_LEVEL = 30;

	public static final int STARTING_STR = 10;
	
	private static final float TIME_TO_REST		    = 1f;
	private static final float TIME_TO_SEARCH	    = 2f;
	private static final float HUNGER_FOR_SEARCH	= 6f;
	
	public HeroClass heroClass = HeroClass.ROGUE;
	public HeroSubClass subClass = HeroSubClass.NONE;
	public String customName = null;
	public ArmorAbility armorAbility = null;
	public ArrayList<LinkedHashMap<Talent, Integer>> talents = new ArrayList<>();
	public LinkedHashMap<Talent, Talent> metamorphedTalents = new LinkedHashMap<>();
	
	private int attackSkill = 10;
	private int defenseSkill = 5;

	public boolean ready = false;
	public boolean damageInterrupt = true;
	public HeroAction curAction = null;
	public HeroAction lastAction = null;

	//reference to the enemy the hero is currently in the process of attacking
	private Char attackTarget;

	public Char enemy(){
		return attackTarget;
	}

	//set by weapons that resolve one shot as several attacks (multi-target/multi-pellet
	//guns). The ranged skill hooks cost mana per invocation, so they must only fire for
	//the first attack of a shot, not once per char hit.
	public boolean extraShotAttack = false;

	public boolean resting = false;
	
	public Belongings belongings;
	
	public int STR;

	// Net MP: remote player hero on host side.
	// pendingActionCell is set by NetManager when PLAYER_ACTION arrives;
	// Hero.act() consumes it on actor thread so handle() stays single-threaded.
	public transient boolean isRemote = false;
	public transient boolean remoteWaiting = false;
	public transient volatile int pendingActionCell = -1;
	// Optional typed action (e.g. "search", "drop", "throw", "use") set by NetManager
	// when the player triggers an action that doesn't reduce to a cell click.
	public transient volatile String pendingActionType = null;
	// Item identifier (item.name()) for inventory actions (drop/throw/use). Host looks
	// it up on the netHero's belongings.
	public transient volatile String pendingActionItem = null;
	// For "use" type, the item action name to invoke (DRINK/EAT/READ/EQUIP/...).
	public transient volatile String pendingActionItemAction = null;
	// Optional int payload for typed actions (e.g. portal_travel destination depth).
	public transient volatile int pendingActionExtra = -1;
	// Net MP: a host→client interaction dialog the player resolved (e.g. picked a quest
	// reward). Set by NetManager when DIALOG_CHOICE arrives; consumed on the actor thread
	// in act() so the resolution mutates game state single-threaded. Turn-independent —
	// a reward can be claimed after the interacting turn already ended.
	public transient volatile int pendingDialogId = -1;
	public transient volatile String pendingDialogChoice = null;

	// Stable per-player identity. Persisted in the level save bundle so that on
	// reconnect (even after host quit/relaunch) the player can claim their old
	// netHero by matching their multiplayerName. Empty for the host's own hero.
	public String netOwnerName = "";
	//the secret its owner proves themselves with when reclaiming this hero.
	//saved with the hero: the host's session map dies with the process, and a
	//token that does not survive a reload is a hero anyone can walk off with
	public String netSessionToken = "";

	// Net MP turn telemetry — incremented every time this hero completes an act()
	// that advanced game time. Persisted so reconnects keep the running count.
	public int turnsTaken = 0;
	// Mirror of "queued" from delta (steps remaining in a multi-step Move). Host
	// computes from curAction + pos; client stores it for indicator display.
	public transient int queuedSteps = 0;

	// Net MP: hero is parked on a level transition cell, waiting for every other
	// online claimed hero to also reach an exit. While true the hero is invisible,
	// untargetable, doesn't appear in pathfinding, and doesn't take turns. Cleared
	// on group descent (which teleports them to the next floor) or by clicking
	// "Leave exit" in the WndAtExit panel.
	// volatile — written by actor thread on step-onto-exit, read by network thread
	// in NetManager.countAtExit() and triggerGroupDescent(). Without volatile the
	// network thread can read stale cached values and stall (or wrongly fire) the gate.
	public transient volatile boolean atExit = false;
	public transient volatile int atExitCell = -1;

	public float awareness;
	
	public int lvl = 1;
	public int exp = 0;
	
	public int HTBoost = 0;

	// Skillful PD skill tree (see actors/hero/skills/)
	public xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills heroSkills =
			xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills.WARRIOR;

	// Sprouted stat system
	public boolean levelup = false;
	public int magicLevel = 0;
	public int speedLevel = 1;
	public int MP = 5;
	public int MT = 5;

	// Sprouted pet system - cross-level persistence
	public boolean haspet = false;
	public boolean petfollow = false;
	public int petType = 0;
	public int petLevel = 0;
	public int petKills = 0;
	public int petHP = 0;
	public int petExperience = 0;
	public int petCooldown = 0;
	public int petCount = 0;


	private ArrayList<Mob> visibleEnemies;

	//This list is maintained so that some logic checks can be skipped
	// for enemies we know we aren't seeing normally, resulting in better performance
	public ArrayList<Mob> mindVisionEnemies = new ArrayList<>();

	public Hero() {
		super();

		HP = HT = 20;
		STR = STARTING_STR;
		
		belongings = new Belongings( this );
		
		visibleEnemies = new ArrayList<>();
	}
	
	public void updateHT( boolean boostHP ){
		int curHT = HT;
		
		HT = 20 + 5*(lvl-1) + HTBoost;
		float multiplier = RingOfMight.HTMultiplier(this);
		HT = Math.round(multiplier * HT);
		
		if (buff(ElixirOfMight.HTBoost.class) != null){
			HT += buff(ElixirOfMight.HTBoost.class).boost();
		}

		if (buff(DeathSword.MaxHPBoost.class) != null) {
			HT += buff(DeathSword.MaxHPBoost.class).HTBonus();
		}

		if (boostHP){
			HP += Math.max(HT - curHT, 0);
		}
		HP = Math.min(HP, HT);
	}

	public int STR() {
		int strBonus = 0;

		strBonus += RingOfMight.strengthBonus( this );
		
		AdrenalineSurge buff = buff(AdrenalineSurge.class);
		if (buff != null){
			strBonus += buff.boost();
		}

		if (hasTalent(Talent.STRONGMAN)){
			strBonus += (int)Math.floor(STR * (0.03f + 0.05f*pointsInTalent(Talent.STRONGMAN)));
		}

		return STR + strBonus;
	}

	private static final String CUSTOM_NAME = "customName";
	private static final String CLASS       = "class";
	private static final String SUBCLASS    = "subClass";
	private static final String ABILITY     = "armorAbility";

	private static final String ATTACK		= "attackSkill";
	private static final String DEFENSE		= "defenseSkill";
	private static final String STRENGTH	= "STR";
	private static final String LEVEL		= "lvl";
	private static final String EXPERIENCE	= "exp";
	private static final String HTBOOST     = "htboost";

	private static final String LEVELUP      = "levelup";
	private static final String MAGICLEVEL   = "magicLevel";
	private static final String SPEEDLEVEL   = "speedLevel";
	private static final String MANAPOINTS   = "MP";
	private static final String MANATOTAL    = "MT";
	private static final String SKILLS_AVAILABLE = "skillsavailable";

	private static final String HASPET       = "haspet";
	private static final String PETFOLLOW    = "petfollow";
	private static final String PETTYPE      = "petType";
	private static final String PETLEVEL     = "petLevel";
	private static final String PETKILLS     = "petKills";
	private static final String PETHP        = "petHP";
	private static final String PETEXP       = "petExperience";
	private static final String PETCOOLDOWN  = "petCooldown";
	private static final String PETCOUNT     = "petCount";
	private static final String THERMAL_WARN = "thermalWarnBand";

	private static final String NET_OWNER    = "netOwnerName";
	private static final String NET_TOKEN    = "netSessionToken";
	private static final String TURNS_TAKEN  = "turnsTaken";

	@Override
	public void storeInBundle( Bundle bundle ) {

		super.storeInBundle( bundle );

		if (customName != null) bundle.put( CUSTOM_NAME, customName );
		if (netOwnerName != null && !netOwnerName.isEmpty()) bundle.put( NET_OWNER, netOwnerName );
		if (netSessionToken != null && !netSessionToken.isEmpty()) bundle.put( NET_TOKEN, netSessionToken );
		bundle.put( TURNS_TAKEN, turnsTaken );
		bundle.put( CLASS, heroClass );
		bundle.put( SUBCLASS, subClass );
		bundle.put( ABILITY, armorAbility );
		Talent.storeTalentsInBundle( bundle, this );
		
		bundle.put( ATTACK, attackSkill );
		bundle.put( DEFENSE, defenseSkill );
		
		bundle.put( STRENGTH, STR );
		
		bundle.put( LEVEL, lvl );
		bundle.put( EXPERIENCE, exp );
		
		bundle.put( HTBOOST, HTBoost );

		bundle.put(LEVELUP, levelup);
		bundle.put(MAGICLEVEL, magicLevel);
		bundle.put(SPEEDLEVEL, speedLevel);
		bundle.put(MANAPOINTS, MP);
		bundle.put(MANATOTAL, MT);

		bundle.put(SKILLS_AVAILABLE, xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.availableSkill);
		heroSkills.storeInBundle(bundle);

		bundle.put(HASPET, haspet);
		bundle.put(PETFOLLOW, petfollow);
		bundle.put(PETTYPE, petType);
		bundle.put(PETLEVEL, petLevel);
		bundle.put(PETKILLS, petKills);
		bundle.put(PETHP, petHP);
		bundle.put(PETEXP, petExperience);
		bundle.put(PETCOOLDOWN, petCooldown);
		bundle.put(PETCOUNT, petCount);
		bundle.put(THERMAL_WARN, thermalWarnBand);
		bundle.put(COLD_GRACE, coldGrace);

		belongings.storeInBundle( bundle );
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {

		lvl = bundle.getInt( LEVEL );
		exp = bundle.getInt( EXPERIENCE );

		HTBoost = bundle.getInt(HTBOOST);

		super.restoreFromBundle( bundle );

		customName = bundle.contains( CUSTOM_NAME ) ? bundle.getString( CUSTOM_NAME ) : null;
		netOwnerName = bundle.contains( NET_OWNER ) ? bundle.getString( NET_OWNER ) : "";
		netSessionToken = bundle.contains( NET_TOKEN ) ? bundle.getString( NET_TOKEN ) : "";
		turnsTaken = bundle.contains( TURNS_TAKEN ) ? bundle.getInt( TURNS_TAKEN ) : 0;
		heroClass = bundle.getEnum( CLASS, HeroClass.class );
		subClass = bundle.getEnum( SUBCLASS, HeroSubClass.class );
		armorAbility = (ArmorAbility)bundle.get( ABILITY );
		Talent.restoreTalentsFromBundle( bundle, this );
		
		attackSkill = bundle.getInt( ATTACK );
		defenseSkill = bundle.getInt( DEFENSE );
		
		STR = bundle.getInt( STRENGTH );

		levelup = bundle.getBoolean(LEVELUP);
		magicLevel = bundle.getInt(MAGICLEVEL);
		speedLevel = bundle.getInt(SPEEDLEVEL);
		if (speedLevel == 0) speedLevel = 1;
		MP = bundle.getInt(MANAPOINTS);
		MT = bundle.getInt(MANATOTAL);

		if (bundle.contains(SKILLS_AVAILABLE)){
			xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.availableSkill = bundle.getInt(SKILLS_AVAILABLE);
		} else {
			//save from before the skill system: grant points retroactively
			xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.availableSkill =
					xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.STARTING_SKILL + (lvl - 1) * 3;
		}
		heroSkills = xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills.forHero(this);
		heroSkills.init(this);
		if (subClass != HeroSubClass.NONE){
			heroSkills.initSubclassBranch(this);
		}
		heroSkills.restoreSkillsFromBundle(bundle);
		if (MT == 0) MT = 5;
		if (MP > MT) MP = MT;

		haspet = bundle.getBoolean(HASPET);
		petfollow = bundle.getBoolean(PETFOLLOW);
		petType = bundle.getInt(PETTYPE);
		petLevel = bundle.getInt(PETLEVEL);
		petKills = bundle.getInt(PETKILLS);
		petHP = bundle.getInt(PETHP);
		petExperience = bundle.getInt(PETEXP);
		petCooldown = bundle.getInt(PETCOOLDOWN);
		petCount = bundle.getInt(PETCOUNT);
		thermalWarnBand = bundle.getInt(THERMAL_WARN);
		coldGrace = bundle.getInt(COLD_GRACE);

		belongings.restoreFromBundle( bundle );
	}
	
	public static void preview( GamesInProgress.Info info, Bundle bundle ) {
		info.level = bundle.getInt( LEVEL );
		info.str = bundle.getInt( STRENGTH );
		info.exp = bundle.getInt( EXPERIENCE );
		info.hp = bundle.getInt( Char.TAG_HP );
		info.ht = bundle.getInt( Char.TAG_HT );
		info.shld = bundle.getInt( Char.TAG_SHLD );
		info.heroClass = bundle.getEnum( CLASS, HeroClass.class );
		info.subClass = bundle.getEnum( SUBCLASS, HeroSubClass.class );
		info.heroName = bundle.contains( CUSTOM_NAME ) ? bundle.getString( CUSTOM_NAME ) : null;
		Belongings.preview( info, bundle );
	}

	public boolean hasTalent( Talent talent ){
		return pointsInTalent(talent) > 0;
	}

	public int pointsInTalent( Talent talent ){
		for (LinkedHashMap<Talent, Integer> tier : talents){
			for (Talent f : tier.keySet()){
				if (f == talent) return tier.get(f);
			}
		}
		return 0;
	}

	public void upgradeTalent( Talent talent ){
		for (LinkedHashMap<Talent, Integer> tier : talents){
			for (Talent f : tier.keySet()){
				if (f == talent) tier.put(talent, tier.get(talent)+1);
			}
		}
		//fused economy: talents draw from the shared skill point pool
		xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.availableSkill =
				Math.max(0, xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.availableSkill - 1);
		Talent.onTalentUpgraded(this, talent);
	}

	public int talentPointsSpent(int tier){
		int total = 0;
		for (int i : talents.get(tier-1).values()){
			total += i;
		}
		return total;
	}

	//FUSED ECONOMY: talents and skills share one wallet - Skill.availableSkill.
	//A tier still unlocks by level/subclass/ability, but how much you can pour
	//into it is only limited by the pool and the talents' own max levels.
	public int talentPointsAvailable(int tier){
		if (lvl < (Talent.tierLevelThresholds[tier] - 1)
			|| (tier == 3 && subClass == HeroSubClass.NONE)
			|| (tier == 4 && armorAbility == null)) {
			return 0;
		}
		int capacity = 0;
		for (Talent t : talents.get(tier-1).keySet()){
			capacity += t.maxPoints() - talents.get(tier-1).get(t);
		}
		return Math.min( capacity,
				xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.availableSkill );
	}

	public int bonusTalentPoints(int tier){
		if (lvl < (Talent.tierLevelThresholds[tier]-1)
				|| (tier == 3 && subClass == HeroSubClass.NONE)
				|| (tier == 4 && armorAbility == null)) {
			return 0;
		} else if (buff(PotionOfDivineInspiration.DivineInspirationTracker.class) != null
					&& buff(PotionOfDivineInspiration.DivineInspirationTracker.class).isBoosted(tier)) {
			return 2;
		} else {
			return 0;
		}
	}
	
	public String className() {
		return subClass == null || subClass == HeroSubClass.NONE ? heroClass.title() : subClass.title();
	}

	@Override
	public String name(){
		if (buff(HeroDisguise.class) != null) {
			return buff(HeroDisguise.class).getDisguise().title();
		} else if (customName != null && !customName.isEmpty()) {
			return customName;
		} else {
			return className();
		}
	}

	@Override
	public void hitSound(float pitch) {
		if (!RingOfForce.fightingUnarmed(this)) {
			belongings.attackingWeapon().hitSound(pitch);
		} else if (RingOfForce.getBuffedBonus(this, RingOfForce.Force.class) > 0) {
			//pitch deepens by 2.5% (additive) per point of strength, down to 75%
			super.hitSound( pitch * GameMath.gate( 0.75f, 1.25f - 0.025f*STR(), 1f) );
		} else {
			super.hitSound(pitch * 1.1f);
		}
	}

	@Override
	public boolean blockSound(float pitch) {
		if ( belongings.weapon() != null && belongings.weapon().defenseFactor(this) >= 4 ){
			Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 1, pitch);
			return true;
		}
		return super.blockSound(pitch);
	}

	public void live() {
		for (Buff b : buffs()){
			if (!b.revivePersists) b.detach();
		}
		Buff.affect( this, Regeneration.class );
		Buff.affect( this, Hunger.class );
		Buff.affect( this, Sleepiness.class );
		Buff.affect( this, ManaRegen.class );
		//skill tree production passives (no-ops until leveled)
		Buff.affect( this, xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillFletching.class );
		Buff.affect( this, xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillHunting.class );
	}
	
	public int tier() {
		Armor armor = belongings.armor();
		if (armor instanceof ClassArmor){
			return 6;
		} else if (armor != null){
			return armor.tier;
		} else {
			return 0;
		}
	}
	
	public boolean shoot( Char enemy, MissileWeapon wep ) {

		attackTarget = enemy;
		boolean wasEnemy = enemy.alignment == Alignment.ENEMY
				|| (enemy instanceof Mimic && enemy.alignment == Alignment.NEUTRAL);

		//temporarily set the hero's weapon to the missile weapon being used
		//TODO improve this!
		belongings.thrownWeapon = wep;
		boolean hit = attack( enemy );
		Invisibility.dispel();
		belongings.thrownWeapon = null;

		if (hit && subClass == HeroSubClass.GLADIATOR && wasEnemy){
			Buff.affect( this, Combo.class ).hit( enemy );
		}

		if (hit && heroClass == HeroClass.DUELIST && wasEnemy){
			Buff.affect( this, Sai.ComboStrikeTracker.class).addHit();
		}

		attackTarget = null;
		return hit;
	}
	
	@Override
	public boolean attack(Char enemy, float dmgMulti, float dmgBonus, float accMulti) {
		//one swing, one activation: see skillDamageModifier()
		swingDamageModifier = NO_SWING;
		boolean result = super.attack(enemy, dmgMulti, dmgBonus, accMulti);
		swingDamageModifier = NO_SWING;
		if (!(belongings.attackingWeapon() instanceof MissileWeapon)){
			//melee exertion adds a small amount of tiredness
			Sleepiness tired = buff(Sleepiness.class);
			if (tired != null) tired.exert();
			if (buff(Talent.PreciseAssaultTracker.class) != null){
				buff(Talent.PreciseAssaultTracker.class).detach();
			} else if (buff(Talent.LiquidAgilACCTracker.class) != null
						&& buff(Talent.LiquidAgilACCTracker.class).uses <= 0){
				buff(Talent.LiquidAgilACCTracker.class).detach();
			}
		}
		return result;
	}

	public void adjustAttackSkill(int delta) { attackSkill += delta; }
	public void adjustDefenseSkill(int delta) { defenseSkill += delta; }

	@Override
	public int attackSkill( Char target ) {
		KindOfWeapon wep = belongings.attackingWeapon();
		
		float accuracy = 1;
		accuracy *= RingOfAccuracy.accuracyMultiplier( this );

		// Ambient weather accuracy bonuses
		if (buff(AuroraBless.class) != null) accuracy *= AuroraBless.ACCURACY_MULT;
		if (buff(RainbowBlessing.class) != null) accuracy *= RainbowBlessing.ACCURACY_MULT;
		if (buff(SolarEclipseBuff.class) != null) accuracy *= SolarEclipseBuff.ACCURACY_MULT;

		//precise assault and liquid agility
		if (!(wep instanceof MissileWeapon)) {
			if ((hasTalent(Talent.PRECISE_ASSAULT) || hasTalent(Talent.LIQUID_AGILITY))
					//does not trigger on ability attacks
					&& belongings.abilityWeapon != wep && buff(MonkEnergy.MonkAbility.UnarmedAbilityTracker.class) == null){

				//non-duelist benefit for precise assault, can stack with liquid agility
				if (heroClass != HeroClass.DUELIST) {
					//persistent +10%/20%/30% ACC for other heroes
					accuracy *= 1f + 0.1f * pointsInTalent(Talent.PRECISE_ASSAULT);
				}

				if (wep instanceof Flail && buff(Flail.SpinAbilityTracker.class) != null){
					//do nothing, this is not a regular attack so don't consume talent fx
				} else if (wep instanceof Crossbow && buff(Crossbow.ChargedShot.class) != null){
					//do nothing, this is not a regular attack so don't consume talent fx
				} else if (buff(Talent.PreciseAssaultTracker.class) != null) {
					// 2x/5x/inf. ACC for duelist if she just used a weapon ability
					switch (pointsInTalent(Talent.PRECISE_ASSAULT)){
						default: case 1:
							accuracy *= 2; break;
						case 2:
							accuracy *= 5; break;
						case 3:
							accuracy *= Float.POSITIVE_INFINITY; break;
					}
				} else if (buff(Talent.LiquidAgilACCTracker.class) != null){
					// 3x/inf. ACC, depending on talent level
					accuracy *= pointsInTalent(Talent.LIQUID_AGILITY) == 2 ? Float.POSITIVE_INFINITY : 3f;
					Talent.LiquidAgilACCTracker buff = buff(Talent.LiquidAgilACCTracker.class);
					buff.uses--;
				}
			}
		} else {
			if (buff(Momentum.class) != null && buff(Momentum.class).freerunning()){
				accuracy *= 1f + pointsInTalent(Talent.PROJECTILE_MOMENTUM)/2f;
			}
			// Wind reduces ranged accuracy (Windswept buff)
			Windswept windBuff = buff(Windswept.class);
			if (windBuff != null) {
				accuracy *= windBuff.rangedAccuracyFactor();
			}
			// Wind direction: very subtle head/tailwind accuracy modifier (±5 % max)
			if (target != null) {
				accuracy *= ClimateManager.windProjectileFactor(pos, target.pos, false);
			}
		}

		if (buff(Scimitar.SwordDance.class) != null){
			accuracy *= 1.50f;
		}

		if (buff(LargeSword.LargeSwordBuff.class) != null) {
			accuracy *= buff(LargeSword.LargeSwordBuff.class).getAccuracyFactor();
		}

		if (buff(UnholyBible.Demon.class) != null) {
			return INFINITE_ACCURACY;
		}

		//skill tree: Firm Hand flat bonus (melee), Accuracy multiplier and
		//Aimed Shot's no-miss shot (ranged)
		int atkBase = attackSkill;
		if (wep instanceof MissileWeapon){
			accuracy *= heroSkills.allToHitModifier();
			//extraShotAttack: a multi-target shot must not re-roll (and re-charge) the skill per victim
			if (!extraShotAttack && heroSkills.anyAimedShot()){
				return INFINITE_ACCURACY;
			}
			atkBase += heroSkills.subToHitBonus();
		} else {
			//allToHitBonus() already covers the subclass branch
			atkBase += heroSkills.allToHitBonus();
		}

		if (!RingOfForce.fightingUnarmed(this)) {
			return Math.max(1, Math.round(atkBase * accuracy * wep.accuracyFactor( this, target )));
		} else {
			return Math.max(1, Math.round(atkBase * accuracy));
		}
	}
	
	@Override
	public int defenseSkill( Char enemy ) {

		//skill tree: Awareness - chance to dodge a ranged attack outright
		if (enemy != null
				&& !Dungeon.level.adjacent(pos, enemy.pos)
				&& heroSkills.anyDodge()){
			return INFINITE_EVASION;
		}

		if (buff(Combo.ParryTracker.class) != null){
			if (canAttack(enemy) && !isCharmedBy(enemy)){
				Buff.affect(this, Combo.RiposteTracker.class).enemy = enemy;
			}
			return INFINITE_EVASION;
		}

		if (buff(RoundShield.GuardTracker.class) != null){
			return INFINITE_EVASION;
		}
		
		float evasion = defenseSkill;
		
		evasion *= RingOfEvasion.evasionMultiplier( this );

		// Firefly glow evasion bonus
		if (buff(FireflyGlow.class) != null) evasion *= FireflyGlow.EVASION_MULT;
		if (buff(SolarEclipseBuff.class) != null) evasion *= SolarEclipseBuff.EVASION_MULT;

		if (buff(Talent.LiquidAgilEVATracker.class) != null){
			if (pointsInTalent(Talent.LIQUID_AGILITY) == 1){
				evasion *= 3f;
			} else if (pointsInTalent(Talent.LIQUID_AGILITY) == 2){
				return INFINITE_EVASION;
			}
		}

		if (buff(Quarterstaff.DefensiveStance.class) != null){
			evasion *= 3;
		}

		if (buff(UnholyBible.Demon.class) != null) {
			evasion /= 2;
		}

		if (paralysed > 0) {
			evasion /= 2;
		}

		if (belongings.armor() != null) {
			evasion = belongings.armor().evasionFactor(this, evasion);

			//stone specifically overrides to 0 always, guaranteed hit
			if (belongings.armor().hasGlyph(Stone.class, this) && !Stone.testingEvasion()){
				return 0;
			}
		}

		evasion *= sleepPenaltyFactor();

		return Math.max(1, Math.round(evasion));
	}

	@Override
	public String defenseVerb() {
		Combo.ParryTracker parry = buff(Combo.ParryTracker.class);
		if (parry != null){
			parry.parried = true;
			if (buff(Combo.class) == null || buff(Combo.class).getComboCount() < 9 || pointsInTalent(Talent.ENHANCED_COMBO) < 2){
				parry.detach();
			}
			return Messages.get(Monk.class, "parried");
		}

		if (buff(RoundShield.GuardTracker.class) != null){
			buff(RoundShield.GuardTracker.class).hasBlocked = true;
			BuffIndicator.refreshHero();
			Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1, Random.Float(0.96f, 1.05f));
			return Messages.get(RoundShield.GuardTracker.class, "guarded");
		}

		if (buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class) != null){
			buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class).detach();
			if (sprite != null && sprite.visible) {
				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1, Random.Float(0.96f, 1.05f));
			}
			return Messages.get(Monk.class, "parried");
		}

		return super.defenseVerb();
	}

	@Override
	public int drRoll() {
		int dr = super.drRoll();

		if (belongings.armor() != null) {
			int armDr = Random.NormalIntRange( belongings.armor().DRMin(), belongings.armor().DRMax());
			if (STR() < belongings.armor().STRReq()){
				armDr -= 2*(belongings.armor().STRReq() - STR());
			}
			if (armDr > 0) dr += armDr;
		}
		if (belongings.weapon() != null && !RingOfForce.fightingUnarmed(this))  {
			int wepDr = Random.NormalIntRange( 0 , belongings.weapon().defenseFactor( this ) );
			if (STR() < ((Weapon)belongings.weapon()).STRReq()){
				wepDr -= 2*(((Weapon)belongings.weapon()).STRReq() - STR());
			}
			if (wepDr > 0) dr += wepDr;
		}

		if (buff(HoldFast.class) != null){
			dr += buff(HoldFast.class).armorBonus();
		}
		
		return dr;
	}
	
	@Override
	public int damageRoll() {
		KindOfWeapon wep = belongings.attackingWeapon();
		int dmg;

		if (!RingOfForce.fightingUnarmed(this)) {
			dmg = wep.damageRoll( this );

			if (!(wep instanceof MissileWeapon)){
				dmg += RingOfForce.armedDamageBonus(this);
				dmg += weaponSkillLevelDamage(wep);
			}
		} else {
			dmg = RingOfForce.damageRoll(this);
			if (RingOfForce.unarmedGetsWeaponAugment(this)){
				dmg = ((Weapon)belongings.attackingWeapon()).augment.damageFactor(dmg);
			}
		}

		PhysicalEmpower emp = buff(PhysicalEmpower.class);
		if (emp != null){
			dmg += emp.dmgBoost;
			emp.left--;
			if (emp.left <= 0) {
				emp.detach();
			}
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.75f, 1.2f);
		}

		if (heroClass != HeroClass.DUELIST
				&& hasTalent(Talent.WEAPON_RECHARGING)
				&& (buff(Recharging.class) != null || buff(ArtifactRecharge.class) != null)){
			dmg = Math.round(dmg * 1.025f + (.025f*pointsInTalent(Talent.WEAPON_RECHARGING)));
		}

		//moon fury, from the moonberries. Detaching through Buff.detach routes into
		//Strength.detach(), which is what lets FullMoonStrength eat the hit instead.
		if (buff(Strength.class) != null) {
			dmg = Math.round(dmg * 4f);
			Buff.detach(this, Strength.class);
		}

		dmg = Math.round(dmg * sleepPenaltyFactor());

		//skill tree damage hooks: actives spend mana inside their modifiers
		dmg = Math.round(dmg * skillDamageModifier(wep instanceof MissileWeapon));

		if (dmg < 0) dmg = 0;
		return dmg;
	}

	//damageRoll() runs several times per swing (enchantment procs, the extra hits in
	//attackProc), but an active skill spends its mana inside damageModifier(): resolve
	//the multiplier on the first roll of an attack and reuse it until that attack ends
	private static final float NO_SWING = -1f;
	private float swingDamageModifier = NO_SWING;

	private float skillDamageModifier( boolean ranged ){
		if (swingDamageModifier < 0) swingDamageModifier = heroSkills.allDamageModifier(ranged);
		return swingDamageModifier;
	}

	//skill tree: Mastery/Blade Mastery/Sacred Weapon swing the hero's own melee weapon
	//as if it carried extra upgrade levels
	private int weaponSkillLevelDamage( KindOfWeapon wep ){
		int bonus = Math.max(0, heroSkills.allWeaponLevelBonus());
		if (bonus == 0) return 0;
		int lvl = Math.max(0, wep.buffedLvl());
		return heroDamageIntRange(wep.min(lvl + bonus) - wep.min(lvl), wep.max(lvl + bonus) - wep.max(lvl));
	}

	//damage rolls that come from the hero can have their RNG influenced by clover
	public static int heroDamageIntRange(int min, int max ){
		if (Random.Float() < ThirteenLeafClover.alterHeroDamageChance()){
			return ThirteenLeafClover.alterDamageRoll(min, max);
		} else {
			return Random.NormalIntRange(min, max);
		}
	}
	
	//multiplier (1.0 down to 0.85) applied to speed/evasion/damage as tiredness climbs
	//from DROWSY toward COMATOSE; 1.0 (no penalty) while well-rested.
	private float sleepPenaltyFactor() {
		Sleepiness tired = buff(Sleepiness.class);
		if (tired == null || tired.level() < Sleepiness.DROWSY) {
			return 1f;
		}
		float progress = (tired.level() - Sleepiness.DROWSY) / (Sleepiness.COMATOSE - Sleepiness.DROWSY);
		return 1f - 0.15f * Math.min(1f, progress);
	}

	@Override
	public float speed() {

		float speed = super.speed();

		if (buff(Crouching.class) != null) {
			speed *= 0.5f;
		}

		speed *= RingOfHaste.speedMultiplier(this);
		
		if (belongings.armor() != null) {
			speed = belongings.armor().speedFactor(this, speed);
		}
		
		Momentum momentum = buff(Momentum.class);
		if (momentum != null){
			((HeroSprite)sprite).sprint( momentum.freerunning() ? 1.5f : 1f );
			speed *= momentum.speedMultiplier();
		} else {
			((HeroSprite)sprite).sprint( 1f );
		}

		NaturesPower.naturesPowerTracker natStrength = buff(NaturesPower.naturesPowerTracker.class);
		if (natStrength != null){
			speed *= (2f + 0.25f*pointsInTalent(Talent.GROWING_POWER));
		}

		speed *= sleepPenaltyFactor();

		speed = AscensionChallenge.modifyHeroSpeed(speed);

		// Sprouted: cap speed when pet is following and petHasteLevel is active
		if (haspet && Dungeon.petHasteLevel > 0 && checkpet() != null) {
			float baseSpeed = super.speed();
			if (speed > baseSpeed * 2f) {
				speed = baseSpeed * 2f;
			}
		}

		return speed;
		
	}

	@Override
	public boolean canSurpriseAttack(){
		KindOfWeapon w = belongings.attackingWeapon();
		if (!(w instanceof Weapon))             return true;
		if (RingOfForce.fightingUnarmed(this))  return true;
		if (STR() < ((Weapon)w).STRReq())       return false;
		if (w instanceof Flail)                 return false;

		return super.canSurpriseAttack();
	}

	public boolean canAttack(Char enemy){
		if (enemy == null || pos == enemy.pos || !Actor.chars().contains(enemy)) {
			return false;
		}

		//can always attack adjacent enemies
		if (Dungeon.level.adjacent(pos, enemy.pos)) {
			return true;
		}

		KindOfWeapon wep = Dungeon.hero.belongings.attackingWeapon();

		if (wep != null){
			return wep.canReach(this, enemy.pos);
		} else if (buff(AscendedForm.AscendBuff.class) != null) {
			boolean[] passable = BArray.not(Dungeon.level.solid, null);
			for (Char ch : Actor.chars()) {
				if (ch != this) passable[ch.pos] = false;
			}

			PathFinder.buildDistanceMap(enemy.pos, passable, 3);

			return PathFinder.distance[pos] <= 3;
		} else {
			return false;
		}
	}
	
	public float attackDelay() {
		if (buff(Talent.LethalMomentumTracker.class) != null){
			buff(Talent.LethalMomentumTracker.class).detach();
			return 0;
		}

		float delay = 1f;

		if (!RingOfForce.fightingUnarmed(this)) {
			
			return delay * belongings.attackingWeapon().delayFactor( this );
			
		} else {
			//Normally putting furor speed on unarmed attacks would be unnecessary
			//But there's going to be that one guy who gets a furor+force ring combo
			//This is for that one guy, you shall get your fists of fury!
			float speed = RingOfFuror.attackSpeedMultiplier(this);

			//ditto for furor + sword dance!
			if (buff(Scimitar.SwordDance.class) != null){
				speed += 0.6f;
			}

			//and augments + brawler's stance! My goodness, so many options now compared to 2014!
			if (RingOfForce.unarmedGetsWeaponAugment(this)){
				delay = ((Weapon)belongings.weapon).augment.delayFactor(delay);
			}

			return delay/speed;
		}
	}

	@Override
	public void spend( float time ) {
		super.spend(time);
	}

	@Override
	public void spendConstant(float time) {
		super.spendConstant(time);
	}

	public void spendAndNextConstant(float time ) {
		busy();
		spendConstant( time );
		next();
		turnsTaken++;
	}

	public void spendAndNext( float time ) {
		busy();
		spend( time );
		next();
		turnsTaken++;
	}
	
	@Override
	public boolean act() {

		//the waypoint march does not stop for ANYTHING - whenever the hero
		//would act with no action queued, the march seats a fresh Move RIGHT
		//HERE on the actor thread, so this same act executes the step. (the
		//old ready()-hook re-aim notified the actor thread BEFORE it parked -
		//a missed wakeup that froze the hero after interrupts)
		if (isAlive()
				&& Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel){
			xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel ow
					= (xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level;
			if (ow.waypointActive && ow.waypointMarching){
				//no action queued: seat a fresh leg. current leg ARRIVED (the
				//hero stands on its dst): renew it - otherwise getCloser fails
				//on the spot and the march parks at the end of every leg
				if (curAction == null){
					int dst = ow.marchDestination();
					if (dst != -1){
						curAction = new HeroAction.Move( dst );
						lastAction = null;
					}
				} else if (curAction instanceof HeroAction.Move
						&& curAction.dst == pos){
					int dst = ow.marchDestination();
					if (dst != -1 && dst != pos){
						curAction.dst = dst;
					}
				}
			}
		}

		// Net MP: the surface window may have slid since the last turn, which relabels
		// every cell index on the level. Runs before the at-exit park below on purpose:
		// a hero parked on an exit that just scrolled away has to be released, and its
		// own act() is the only one still being scheduled.
		xyz.gabriwar.warpedpixeldungeon.net.NetManager.checkWorldWindow();

		// Net MP: hero is parked at an exit waiting for the group. Don't take a
		// turn — just bump our cooldown so the actor loop yields to mobs / other
		// heroes. turnsTaken intentionally NOT incremented (player isn't playing).
		if (atExit) {
			spend( TICK );
			next();
			return true;
		}

		if (isRemote) {
			boolean claimed = xyz.gabriwar.warpedpixeldungeon.net.NetManager.isClaimedNetHero(this);
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id() + " act() VERY-START pos=" + pos
					+ " owner=" + netOwnerName
					+ " claimed=" + claimed
					+ " curAction=" + (curAction == null ? "null" : curAction.getClass().getSimpleName())
					+ " paralysed=" + paralysed + " ready=" + ready + " resting=" + resting
					+ " spriteNull=" + (sprite == null));
			// Net hero sprite can vanish if GameScene rebuilds (level transition, scene refresh)
			// — netHeroes aren't in Dungeon.level.mobs so they don't get re-sprited automatically.
			// Re-attach a sprite lazily so getCloser/actMove don't NPE.
			if (sprite == null) {
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id() + " sprite NULL, re-attaching");
				xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.addNetHero(this);
			}
			// Unclaimed remote hero (owner disconnected, not yet reconnected): NPC-skip
			// the turn so the actor loop keeps moving and other actors aren't blocked
			// waiting for input that will never come.
			if (!claimed) {
				curAction = null;
				spendAndNext( TICK );
				return false;
			}
		}

		// Advance the world clock once per game turn — NOT once per hero. These are
		// world ticks, not personal ones: with a party of four, running them for every
		// hero would push the day/night cycle, the seasons, the snow line, the weather
		// fronts and the shop restock timers along four times as fast as single-player.
		// The host's own hero is the single heartbeat; remote heroes only act.
		if (!isRemote) {
			// Advance the day/night cycle each hero turn
			DayNightCycle.onHeroTurn();
			if (Dungeon.dewDraw) Dungeon.level.currentmoves++;

			// Thermal diffusion: decay and spread persistent tile heat
			TileTemperature.stepDiffusion(Dungeon.level);
		}

		// --- Weather gameplay effects ---
		checkWeatherEffects();

		//calls to dungeon.observe will also update hero's local FOV.
		// Net MP: remote heroes don't share heroFOV with the host — they need
		// their own array, recomputed from their own pos. Otherwise handle()
		// gates like `fieldOfView[cell] && ch instanceof Mob` (Attack) read
		// the host's POV and we get bogus action picks (e.g. PickUp on a cell
		// occupied by a mob the host can't see), which deadlocks act().
		if (isRemote) {
			if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
				fieldOfView = new boolean[Dungeon.level.length()];
			}
			Dungeon.level.updateFieldOfView(this, fieldOfView);
		} else {
			fieldOfView = Dungeon.level.heroFOV;
		}

		if (buff(Endure.EndureTracker.class) != null){
			buff(Endure.EndureTracker.class).endEnduring();
		}

		if (!ready) {
			if (isRemote) {
				// Already updated above; skip Dungeon.observe() — that would
				// blow away the host's heroFOV with the net hero's POV.
			} else if (!resting || buff(MindVision.class) != null || buff(Awareness.class) != null) {
				//do a full observe (including fog update) if not resting.
				Dungeon.observe();
			} else {
				//otherwise just directly re-calculate FOV
				Dungeon.level.updateFieldOfView(this, fieldOfView);
			}
		}
		
		checkVisibleMobs();
		BuffIndicator.refreshHero();
		BuffIndicator.refreshBoss();
		
		if (paralysed > 0) {
			if (isRemote) xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id() + " EARLY-RETURN paralysed pos=" + pos);
			curAction = null;

			spendAndNext( TICK );
			return false;
		}

		if (isRemote) {
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id() + " act() reached pre-isRemote-block pos=" + pos
					+ " curAction=" + (curAction == null ? "null" : curAction.getClass().getSimpleName())
					+ " pending=" + pendingActionCell + " remoteWaiting=" + remoteWaiting + " ready=" + ready);
		}
		
		// Sprouted: increment egg move counters and journal charge each turn
		Egg egg = belongings.getItem(Egg.class);
		if (egg != null) egg.moves++;
		EasterEgg egg2 = belongings.getItem(EasterEgg.class);
		if (egg2 != null) egg2.moves++;
		ShadowDragonEgg egg3 = belongings.getItem(ShadowDragonEgg.class);
		if (egg3 != null) egg3.moves++;
		OtilukesJournal journal = belongings.getItem(OtilukesJournal.class);
		if (journal != null && (Dungeon.depth < 26 || Dungeon.depth == 55)
				&& (journal.journalLevel > 1 || journal.rooms[0])
				&& journal.charge < OtilukesJournal.FULL_CHARGE) {
			journal.charge++;
			if (journal.charge >= OtilukesJournal.FULL_CHARGE) {
				GLog.p(Messages.get(OtilukesJournal.class, "fully_charged"));
			}
		}

		// Net MP: remote hero consumes pending cell here (single-threaded on actor thread).
		// If a cell arrived while we were sleeping, run handle() now to set curAction,
		// then fall through to the normal curAction != null branch below.
		if (isRemote && curAction == null) {
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id() + " act() entry pos=" + pos
					+ " pending=" + pendingActionCell + " type=" + pendingActionType
					+ " remoteWaiting=" + remoteWaiting);
			// Resolve a pending interaction-dialog choice (e.g. quest reward pick) first.
			// Turn-independent and free: claiming doesn't consume the hero's turn, so we
			// resolve and re-enter act() without spending time.
			if (pendingDialogId >= 0) {
				int dialogId;
				String choice;
				synchronized (this) {
					dialogId = pendingDialogId;
					choice = pendingDialogChoice;
					pendingDialogId = -1;
					pendingDialogChoice = null;
				}
				xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.resolve(this, dialogId, choice);
				return false;
			}
			// Typed action (search/drop/throw/use): consume first.
			if (pendingActionType != null) {
				// Snapshot + null-out under the same lock the network thread takes when
				// writing the compound action — guarantees we see a coherent (type, item,
				// useAction, cell) tuple instead of a half-written mix of two actions.
				String type;
				String itemName;
				int targetCell;
				String useActionLocal;
				int extra;
				synchronized (this) {
					type = pendingActionType;
					itemName = pendingActionItem;
					targetCell = pendingActionCell;
					useActionLocal = pendingActionItemAction;
					extra = pendingActionExtra;
					pendingActionType = null;
					pendingActionItem = null;
					pendingActionItemAction = null;
					pendingActionExtra = -1;
					if ("throw".equals(type)) pendingActionCell = -1; // consumed as target
				}
				remoteWaiting = false;
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id() + " consuming type=" + type
						+ " item=" + itemName + " targetCell=" + targetCell);
				if ("search".equals(type)) {
					search(true);
					return false;
				} else if ("drop".equals(type) && itemName != null) {
					Item it = findInventoryItemByName(itemName);
					if (it != null) {
						it.doDrop(this);
					} else {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] drop: item not found name=" + itemName);
						// Don't silently drop the client's action: advance time and
						// re-prompt so input is never lost (would otherwise leave the
						// hero idle until the actor loop re-cycles it).
						spendAndNext(TICK);
					}
					return false;
				} else if ("throw".equals(type) && itemName != null && targetCell >= 0) {
					Item it = findInventoryItemByName(itemName);
					if (it != null) {
						it.cast(this, targetCell);
					} else {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] throw: item not found name=" + itemName);
						spendAndNext(TICK);
					}
					return false;
				} else if ("use".equals(type) && itemName != null) {
					String useAction = useActionLocal;
					Item it = findInventoryItemByName(itemName);
					if (it != null && useAction != null
							&& xyz.gabriwar.warpedpixeldungeon.net.NetManager.isPartyTravelAction(it, useAction)) {
						// Beacon / journal warps drag the whole party across the world without
						// the at-exit gate, and read Dungeon.hero (the host) while doing it.
						// Refuse with a message instead of silently teleporting everyone.
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.refusePartyTravel(this);
						return false;
					}
					if (it != null && useAction != null) {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] use: " + itemName + "." + useAction);
						it.execute(this, useAction);
					} else {
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] use: missing item=" + itemName + " action=" + useAction);
						spendAndNext(TICK);
					}
					return false;
				} else if ("portal_open".equals(type)) {
					// Client opened a portal gate UI — just register discovery so the
					// gate shows in everyone's travel list. No turn cost.
					xyz.gabriwar.warpedpixeldungeon.windows.WndPortal.hostOpen(this);
					return false;
				} else if ("portal_pay".equals(type)) {
					xyz.gabriwar.warpedpixeldungeon.windows.WndPortal.hostPay(this);
					return false;
				} else if ("portal_travel".equals(type)) {
					xyz.gabriwar.warpedpixeldungeon.windows.WndPortal.hostTravel(this, extra);
					return false;
				} else if ("shop_open".equals(type)) {
					// no-op — interaction was already triggered when the host sent
					// SHOW_DIALOG. Client uses this for symmetry / future analytics.
					return false;
				} else if ("shop_sell".equals(type)) {
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper.hostSell(this, itemName);
					return false;
				} else if ("shop_sell_all".equals(type)) {
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper.hostSellAll(this, itemName);
					return false;
				} else if ("shop_buyback".equals(type)) {
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper.hostBuyback(this, extra);
					return false;
				} else if ("shop_buy".equals(type)) {
					xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper.hostBuyFromHeap(this, extra);
					return false;
				} else if ("blacksmith_pickaxe".equals(type)) {
					xyz.gabriwar.warpedpixeldungeon.net.BlacksmithService.pickaxe(this);
					return false;
				} else if ("blacksmith_cashout".equals(type)) {
					xyz.gabriwar.warpedpixeldungeon.net.BlacksmithService.cashout(this);
					return false;
				} else if ("blacksmith_reforge".equals(type)) {
					String n1 = itemName, n2 = null;
					if (itemName != null && itemName.contains("|")) {
						int bar = itemName.indexOf('|');
						n1 = itemName.substring(0, bar);
						n2 = itemName.substring(bar + 1);
					}
					xyz.gabriwar.warpedpixeldungeon.net.BlacksmithService.reforge(this, n1, n2);
					return false;
				} else if ("blacksmith_harden".equals(type)) {
					xyz.gabriwar.warpedpixeldungeon.net.BlacksmithService.harden(this, itemName);
					return false;
				} else if ("blacksmith_upgrade".equals(type)) {
					xyz.gabriwar.warpedpixeldungeon.net.BlacksmithService.upgrade(this, itemName);
					return false;
				} else if ("blacksmith_smith_open".equals(type)) {
					xyz.gabriwar.warpedpixeldungeon.net.BlacksmithService.smithOpen(this);
					return false;
				} else if ("blacksmith_smith".equals(type)) {
					xyz.gabriwar.warpedpixeldungeon.net.BlacksmithService.smith(this, extra);
					return false;
				}
				// unknown type → fall through to signal
			}
			if (pendingActionCell >= 0) {
				int cell = pendingActionCell;
				pendingActionCell = -1;
				remoteWaiting = false;
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id() + " consuming cell=" + cell);
				handle(cell);
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id() + " after handle: curAction="
						+ (curAction == null ? "null" : curAction.getClass().getSimpleName()));
				// Click on own cell w/ no useful action (no heap/chest/trap → handle() just made
				// a Move(dst=self)). actMove only advances time when canSelfTrample is true,
				// otherwise it'd loop. Force rest() so time always advances and turn yields.
				if (cell == pos
						&& (curAction == null
							|| (curAction instanceof HeroAction.Move
								&& ((HeroAction.Move) curAction).dst == pos))) {
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id() + " WAIT (own-cell click, no pickup)");
					curAction = null;
					rest(false);
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id()
							+ " act() RETURN-EARLY after rest pos=" + pos);
					return false;
				}
			}
			if (curAction == null) {
				if (!remoteWaiting) {
					remoteWaiting = true;
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.signalRemoteTurn(this);
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id() + " signaled turn @pos=" + pos);
				} else {
					xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id() + " already waiting, skipping signal");
				}
				return false;
			}
		}

		boolean actResult;
		if (curAction == null) {

			if (resting) {
				spendConstant( TIME_TO_REST );
				next();
			} else {
				ready();
			}

			//if we just loaded into a level and have a search buff, make sure to process them
			if(Actor.now() == 0){
				if (buff(Foresight.class) != null){
					search(false);
				} else if (buff(TalismanOfForesight.Foresight.class) != null){
					buff(TalismanOfForesight.Foresight.class).checkAwareness();
				}
			}
			
			actResult = false;
			
		} else {
			
			resting = false;
			
			ready = false;
			
			if (curAction instanceof HeroAction.Move) {
				actResult = actMove( (HeroAction.Move)curAction );
				
			} else if (curAction instanceof HeroAction.Interact) {
				actResult = actInteract( (HeroAction.Interact)curAction );
				
			} else if (curAction instanceof HeroAction.Buy) {
				actResult = actBuy( (HeroAction.Buy)curAction );
				
			}else if (curAction instanceof HeroAction.PickUp) {
				actResult = actPickUp( (HeroAction.PickUp)curAction );
				
			} else if (curAction instanceof HeroAction.OpenChest) {
				actResult = actOpenChest( (HeroAction.OpenChest)curAction );
				
			} else if (curAction instanceof HeroAction.Unlock) {
				actResult = actUnlock((HeroAction.Unlock) curAction);
				
			} else if (curAction instanceof HeroAction.Mine) {
				actResult = actMine( (HeroAction.Mine)curAction );

			}else if (curAction instanceof HeroAction.LvlTransition) {
				actResult = actTransition( (HeroAction.LvlTransition)curAction );
				
			} else if (curAction instanceof HeroAction.Attack) {
				actResult = actAttack( (HeroAction.Attack)curAction );
				
			} else if (curAction instanceof HeroAction.Alchemy) {
				actResult = actAlchemy( (HeroAction.Alchemy)curAction );

			} else if (curAction instanceof HeroAction.InteractPet) {
				actResult = actInteractPet( (HeroAction.InteractPet)curAction );

			} else if (curAction instanceof HeroAction.CrouchToggle) {
				actResult = actCrouchToggle();

			} else {
				actResult = false;
			}
		}
		
		if(hasTalent(Talent.BARKSKIN) && Dungeon.level.map[pos] == Terrain.FURROWED_GRASS){
			Barkskin.conditionallyAppend(this, (lvl*pointsInTalent(Talent.BARKSKIN))/2, 1 );
		}

		if (isRemote) {
			xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id() + " act() RETURN actResult=" + actResult
					+ " pos=" + pos
					+ " curActionAfter=" + (curAction == null ? "null" : curAction.getClass().getSimpleName())
					+ " ready=" + ready);
			// If a Move chain completed mid-act() and ready() cleared curAction,
			// we still need to signal the client for the next turn — the start-of-act
			// isRemote block didn't fire this round because curAction was non-null.
			if (!actResult && curAction == null && !remoteWaiting) {
				remoteWaiting = true;
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.signalRemoteTurn(this);
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.log("[NET-HOST] netHero " + id()
						+ " post-action re-signal (move chain ended) @pos=" + pos);
			}
		}

		return actResult;
	}
	
	public void busy() {
		ready = false;
	}

	/** Net MP: find an item on this hero's belongings by display name. Equipped slots
	 *  first (matches client serialization order), then backpack. Used by the host to
	 *  resolve drop/throw/use actions sent by remote players (identifier is the
	 *  client-side displayed item name; first match wins for stacked duplicates). */
	public Item findInventoryItemByName(String name) {
		if (name == null || belongings == null) return null;
		Item[] equipped = { belongings.weapon(), belongings.armor(), belongings.artifact(),
				belongings.misc(), belongings.ring() };
		for (Item it : equipped) {
			if (it != null && name.equals(it.name())) return it;
		}
		for (Item it : belongings.backpack.items) {
			if (name.equals(it.name())) return it;
		}
		return null;
	}

	private void ready() {
		if (sprite != null && sprite.looping()) sprite.idle();
		curAction = null;
		damageInterrupt = true;
		waitOrPickup = false;
		ready = true;
		canSelfTrample = true;

		AttackIndicator.updateState();
		
		GameScene.ready();
	}
	
	public void interrupt() {
		if (isAlive() && curAction != null &&
			((curAction instanceof HeroAction.Move && curAction.dst != pos) ||
			(curAction instanceof HeroAction.LvlTransition))) {
			lastAction = curAction;
		}
		curAction = null;
		GameScene.resetKeyHold();
		resting = false;
	}
	
	public void resume() {
		curAction = lastAction;
		lastAction = null;
		damageInterrupt = false;
		next();
	}

	private boolean canSelfTrample = false;
	public boolean canSelfTrample(){
		return canSelfTrample && !rooted && !flying &&
				//standing in high grass
				(Dungeon.level.map[pos] == Terrain.HIGH_GRASS ||
				//standing in furrowed grass and not huntress
				(heroClass != HeroClass.HUNTRESS && Dungeon.level.map[pos] == Terrain.FURROWED_GRASS) ||
				//standing on a plant
				Dungeon.level.plants.get(pos) != null);
	}

	//the first turns of a run are spent in a snowbound town: a hero who walks
	//out of the character screen straight into hypothermia never gets to play.
	//counts down on every thermal tick and holds the cold off while it lasts
	public int coldGrace = 0;
	private static final String COLD_GRACE = "coldGrace";

	//which thermal danger band the hero was last warned about:
	//-1 cold (feels-like below Hypothermia.WARN_TEMP), 1 hot (above Heatstroke.WARN_TEMP), 0 neither.
	//one warning per crossing, with a 2°C hysteresis so a border-line reading does not spam
	private int thermalWarnBand = 0;

	private void checkThermalWarnings( float feelsLike ) {
		int band = thermalWarnBand;
		if (band == -1 && feelsLike > Hypothermia.WARN_TEMP + 2f) band = 0;
		if (band ==  1 && feelsLike < Heatstroke.WARN_TEMP - 2f)  band = 0;
		if (band == 0) {
			if (feelsLike < Hypothermia.WARN_TEMP)     band = -1;
			else if (feelsLike > Heatstroke.WARN_TEMP) band = 1;
		}
		if (band == thermalWarnBand) return;

		if (band == -1) {
			if (buff(Hypothermia.class) == null) {
				GLog.w(Messages.get(this, "cold_warn"));
				if (sprite != null) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "cold_warn_status"));
			}
		} else if (band == 1) {
			if (buff(Heatstroke.class) == null) {
				GLog.w(Messages.get(this, "heat_warn"));
				if (sprite != null) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "heat_warn_status"));
			}
		} else if (thermalWarnBand == -1) {
			GLog.i(Messages.get(this, "cold_ease"));
		} else {
			GLog.i(Messages.get(this, "heat_ease"));
		}
		thermalWarnBand = band;
	}

	/**
	 * Per-turn weather checks: apply or clear temperature/rain/wind buffs
	 * based on current ClimateManager state.
	 */
	private void checkWeatherEffects() {
		if (Dungeon.level == null || !isAlive()) return;

		//feels-like already includes the worn armor's thermalOffset (TileTemperature.feelsLikeAt)
		float feelsLike = TileTemperature.feelsLikeAt(pos);

		checkThermalWarnings(feelsLike);

		//a fire at your side, or the torch in your hand, dries you off
		xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched drenched =
				buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched.class);
		if (drenched != null) drenched.dryOff();

		// Armor modifies bodyTemp convergence:
		// - thermalMass: plate retains temperature (slow rate); mail conducts fast (high rate)
		float armorMass   = 1f;
		if (belongings.armor != null) {
			armorMass   = belongings.armor.thermalMass();
		}

		// --- Body temperature: converges gradually toward feels-like ---
		if (Float.isNaN(bodyTemp)) {
			bodyTemp = feelsLike; // first tick: snap to ambient
		} else {
			float diff = feelsLike - bodyTemp;
			// Rate scales with the magnitude of the difference so that intense
			// fire/ice tiles (which are short-lived) impact bodyTemp quickly.
			// Base: 0.8°C/turn warming, 0.4°C/turn cooling.
			// Extreme temps (|diff| > 20) ramp up to ~15°C/turn at |diff| = 60.
			float baseRate = diff > 0 ? 0.8f : 0.4f;
			float rate = baseRate + Math.max(0f, Math.abs(diff) - 20f) * 0.20f;
			rate *= armorMass;
			bodyTemp += Math.signum(diff) * Math.min(Math.abs(diff), rate);
		}

		// --- Temperature → Hypothermia (uses bodyTemp, not raw tile) ---
		if (coldGrace > 0) coldGrace--;
		if (bodyTemp < -5f
				&& coldGrace <= 0
				&& buff(Hypothermia.class) == null
				&& buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.FireImbue.class) == null) {
			boolean isNight = DayNightCycle.phase() == DayNightCycle.Phase.NIGHT;
			boolean isWindy = ClimateManager.localWindSpeed() > 8f;
			boolean isSnowing = ClimateManager.isSnowing();
			boolean isDrenched = buff(Drenched.class) != null;

			if (isNight || isWindy || isSnowing || isDrenched) {
				Buff.affect(this, Hypothermia.class);
				GLog.w(Messages.get(Hypothermia.class, "onset"));
			}
		}

		if (bodyTemp > 35f
				&& buff(Heatstroke.class) == null
				&& buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost.class) == null) {
			Buff.affect(this, Heatstroke.class);
			GLog.w(Messages.get(Heatstroke.class, "onset"));
		}

		// --- Rain → Drenched ---
		if (ClimateManager.localPrecipRate() > 0.3f && ClimateManager.isRaining()) {
			Buff.prolong(this, Drenched.class, Drenched.DURATION);
		}

		// --- Wind → Windswept ---
		float wind = ClimateManager.localWindSpeed();
		if (wind > 10f) {
			Buff.prolong(this, Windswept.class, Windswept.DURATION);
		}

		// --- Ambient weather blessings ---
		ClimateManager.WeatherOverlayAmbient ambient = ClimateManager.ambientType();
		switch (ambient) {
			case AURORA:
				Buff.prolong(this, AuroraBless.class, AuroraBless.DURATION);
				break;
			case FIREFLIES:
				Buff.prolong(this, FireflyGlow.class, FireflyGlow.DURATION);
				break;
			case SPRING_PETALS:
				Buff.prolong(this, SpringBloom.class, SpringBloom.DURATION);
				break;
			case RAINBOW:
				Buff.prolong(this, RainbowBlessing.class, RainbowBlessing.DURATION);
				break;
			case CORONA: // solar eclipse
				Buff.prolong(this, SolarEclipseBuff.class, SolarEclipseBuff.DURATION);
				break;
			default:
				break;
		}

		// Lunar eclipse (blood moon) — not tied to ambient type
		if (ClimateManager.isLunarEclipse()) {
			Buff.prolong(this, BloodMoonBuff.class, BloodMoonBuff.DURATION);
		}
	}
	
	private boolean actMove( HeroAction.Move action ) {

		if (getCloser( action.dst )) {
			canSelfTrample = false;
			return true;

		//Hero moves in place if there is grass to trample
		} else if (pos == action.dst && canSelfTrample()){
			canSelfTrample = false;
			Dungeon.level.pressCell(pos);
			spendAndNext( 1 / speed() );
			return false;
		} else {
			//a marching waypoint leg with no path (fog detours, mobs, shifted
			//ground) must NOT park silently: wait one turn and retry a fresh
			//leg. marchDestination counts the strikes - three of them pause
			//the march with its message, and only then does the hero go idle
			if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel){
				xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel ow
						= (xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level;
				if (ow.waypointActive && ow.waypointMarching){
					int nd = ow.marchDestination();
					if (nd != -1){
						action.dst = nd;
						spendAndNext( TICK );
						return false;
					}
				}
			}
			ready();
			return false;
		}
	}
	
	private boolean actInteract( HeroAction.Interact action ) {
		
		Char ch = action.ch;

		if (ch.isAlive() && ch.canInteract(this)) {
			
			ready();
			sprite.turnTo( pos, ch.pos );
			return ch.interact(this);
			
		} else {
			
			if (fieldOfView[ch.pos] && getCloser( ch.pos )) {

				return true;

			} else {
				ready();
				return false;
			}
			
		}
	}
	
	private boolean actBuy( HeroAction.Buy action ) {
		int dst = action.dst;
		if (pos == dst) {

			ready();
			
			Heap heap = Dungeon.level.heaps.get( dst );
			if (heap != null && heap.type == Type.FOR_SALE && heap.size() == 1) {
				if (isRemote) {
					// netHero arrived at a FOR_SALE heap — route the purchase prompt to
					// the owning client instead of opening WndTradeItem on the host's screen.
					Item item = heap.peek();
					if (xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper.closedForNight()) {
						// Don't hand out a Buy button the host will refuse: hostBuyFromHeap
						// only logs the rejection on the host's screen, so the client would
						// see the click do nothing at all.
						xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog(this,
								Messages.get(WndTradeItem.class, "closed"));
					} else if (item != null) {
						try {
							org.json.JSONObject payload = new org.json.JSONObject();
							payload.put("cell", heap.pos);
							payload.put("name",
									xyz.gabriwar.warpedpixeldungeon.messages.Messages.titleCase(item.title()));
							payload.put("image", item.image());
							payload.put("price",
									xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper.sellPrice(item));
							payload.put("value", item.value());
							payload.put("lvl", item.level());
							payload.put("gold", Dungeon.gold);
							xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.request(this,
									xyz.gabriwar.warpedpixeldungeon.net.NetDialogs.KIND_SHOP_BUY,
									payload, false);
						} catch (Exception ignored) {}
					}
				} else {
					Game.runOnRenderThread(new Callback() {
						@Override
						public void call() {
							GameScene.show( new WndTradeItem( heap ) );
						}
					});
				}
			}

			return false;

		} else if (getCloser( dst )) {

			return true;

		} else {
			ready();
			return false;
		}
	}

	private boolean actAlchemy( HeroAction.Alchemy action ) {
		int dst = action.dst;
		if (Dungeon.level.distance(dst, pos) <= 1) {

			ready();
			
			AlchemistsToolkit.kitEnergy kit = buff(AlchemistsToolkit.kitEnergy.class);
			if (kit != null && kit.isCursed()){
				GLog.w( Messages.get(AlchemistsToolkit.class, "cursed"));
				return false;
			}

			AlchemyScene.clearToolkit();
			WarpedPixelDungeon.switchScene(AlchemyScene.class);
			return false;

		} else if (getCloser( dst )) {

			return true;

		} else {
			ready();
			return false;
		}
	}

	//used to keep track if the wait/pickup action was used
	// so that the hero spends a turn even if the fail to pick up an item
	public boolean waitOrPickup = false;

	private boolean actPickUp( HeroAction.PickUp action ) {
		int dst = action.dst;
		if (pos == dst) {
			
			Heap heap = Dungeon.level.heaps.get( pos );
			if (heap != null) {
				Item item = heap.peek();
				if (item.doPickUp( this )) {
					heap.pickUp();

					if (item instanceof Dewdrop
							|| item instanceof TimekeepersHourglass.sandBag
							|| item instanceof DriedRose.Petal
							|| item instanceof Key
							|| item instanceof Guidebook
							|| (item instanceof MissileWeapon && !MissileWeapon.UpgradedSetTracker.pickupValid(this, (MissileWeapon) item))) {
						//Do Nothing
					} else if (item instanceof DarkGold) {
						DarkGold existing = belongings.getItem(DarkGold.class);
						if (existing != null){
							if (existing.quantity() >= 40) {
								GLog.p(Messages.get(DarkGold.class, "you_now_have", existing.quantity()));
							} else {
								GLog.i(Messages.get(DarkGold.class, "you_now_have", existing.quantity()));
							}
						}
					} else {

						//TODO make all unique items important? or just POS / SOU?
						boolean important = item.unique && item.isIdentified() &&
								(item instanceof Scroll || item instanceof Potion);
						if (important) {
							GLog.p( Messages.capitalize(Messages.get(this, "you_now_have", item.name())) );
						} else {
							GLog.i( Messages.capitalize(Messages.get(this, "you_now_have", item.name())) );
						}
					}
					
					curAction = null;
				} else {

					if (waitOrPickup) {
						spendAndNextConstant(TIME_TO_REST);
					}

					//allow the hero to move between levels even if they can't collect the item
					if (Dungeon.level.getTransition(pos) != null){
						throwItems();
					} else {
						heap.sprite.drop();
					}

					if (item instanceof Dewdrop
							|| item instanceof TimekeepersHourglass.sandBag
							|| item instanceof DriedRose.Petal
							|| item instanceof Key) {
						//Do Nothing
					} else {
						GLog.newLine();
						GLog.n(Messages.capitalize(Messages.get(this, "you_cant_have", item.name())));
					}

					ready();
				}
			} else {
				ready();
			}

			return false;

		} else if (getCloser( dst )) {

			return true;

		} else {
			ready();
			return false;
		}
	}
	
	private boolean actOpenChest( HeroAction.OpenChest action ) {
		int dst = action.dst;
		if (Dungeon.level.adjacent( pos, dst ) || pos == dst) {
			path = null;
			
			Heap heap = Dungeon.level.heaps.get( dst );
			if (heap != null && (heap.type != Type.HEAP && heap.type != Type.FOR_SALE)) {
				
				//GoldenSkeletonKey is a master key: it opens any lock from any
				//floor, after the specific key is tried and the player confirms.
				if ((heap.type == Type.LOCKED_CHEST
							&& Notes.keyCount(new GoldenKey(Dungeon.depth)) < 1
							&& !GoldenSkeletonKey.anyInJournal())
					|| (heap.type == Type.CRYSTAL_CHEST
							&& Notes.keyCount(new CrystalKey(Dungeon.depth)) < 1
							&& !GoldenSkeletonKey.anyInJournal())){

						GLog.w( Messages.get(this, "locked_chest") );
						ready();
						return false;

				}

				//about to spend the golden key on this chest: ask first
				if (((heap.type == Type.LOCKED_CHEST && Notes.keyCount(new GoldenKey(Dungeon.depth)) < 1)
						|| (heap.type == Type.CRYSTAL_CHEST && Notes.keyCount(new CrystalKey(Dungeon.depth)) < 1))
						&& GoldenSkeletonKey.confirmedCell != dst){
					promptGoldenKey( new HeroAction.OpenChest( dst ), dst );
					return false;
				}

				switch (heap.type) {
				case TOMB:
					Sample.INSTANCE.play( Assets.Sounds.TOMB );
					PixelScene.shake( 1, 0.5f );
					break;
				case SKELETON:
				case REMAINS:
					break;
				default:
					Sample.INSTANCE.play( Assets.Sounds.UNLOCK );
				}
				
				sprite.operate( dst );
				
			} else {
				ready();
			}

			return false;

		} else if (getCloser( dst )) {

			return true;

		} else {
			ready();
			return false;
		}
	}
	
	//asks before spending a golden skeleton key on a lock, then re-issues the action
	private void promptGoldenKey( final HeroAction action, final int cell ){
		ready();
		GameScene.show( new WndOptions(
				new ItemSprite( ItemSpriteSheet.GOLDEN_KEY ),
				Messages.titleCase( Messages.get(GoldenSkeletonKey.class, "name") ),
				Messages.get(GoldenSkeletonKey.class, "confirm"),
				Messages.get(GoldenSkeletonKey.class, "confirm_yes"),
				Messages.get(GoldenSkeletonKey.class, "confirm_no") ){
			@Override
			protected void onSelect( int index ) {
				if (index == 0){
					GoldenSkeletonKey.confirmedCell = cell;
					curAction = action;
					next();
				}
			}
		} );
	}

	private boolean actUnlock( HeroAction.Unlock action ) {
		int doorCell = action.dst;
		if (Dungeon.level.adjacent( pos, doorCell )) {
			path = null;
			
			boolean hasKey = false;
			int door = Dungeon.level.map[doorCell];
			
			if (door == Terrain.LOCKED_DOOR
					&& Notes.keyCount(new IronKey(Dungeon.depth)) > 0) {
				
				hasKey = true;

			} else if (door == Terrain.HERO_LKD_DR){

				if (belongings.getItem(SkeletonKey.class) != null
						&& !belongings.getItem(SkeletonKey.class).cursed){
					GLog.i(Messages.get(SkeletonKey.class, "locked_with_key"));
					ready();
					return false;
				} else {
					hasKey = true;
				}
				
			} else if (door == Terrain.CRYSTAL_DOOR
					&& Notes.keyCount(new CrystalKey(Dungeon.depth)) > 0) {

				hasKey = true;

			} else if (door == Terrain.LOCKED_EXIT
					&& Notes.keyCount(new WornKey(Dungeon.depth)) > 0) {

				hasKey = true;
				
			}
			
			if (hasKey) {

				sprite.operate( doorCell );

				Sample.INSTANCE.play( Assets.Sounds.UNLOCK );

			} else if ((door == Terrain.LOCKED_DOOR || door == Terrain.CRYSTAL_DOOR || door == Terrain.LOCKED_EXIT)
					&& GoldenSkeletonKey.anyInJournal()){

				if (GoldenSkeletonKey.confirmedCell == doorCell){
					sprite.operate( doorCell );
					Sample.INSTANCE.play( Assets.Sounds.UNLOCK );
				} else {
					promptGoldenKey( new HeroAction.Unlock( doorCell ), doorCell );
				}

			} else {
				GLog.w( Messages.get(this, "locked_door") );
				ready();
			}

			return false;

		} else if (getCloser( doorCell )) {

			return true;

		} else {
			ready();
			return false;
		}
	}

	private boolean actMine(HeroAction.Mine action){
		if (Dungeon.level.adjacent(pos, action.dst)){
			path = null;
			if ((Dungeon.level.map[action.dst] == Terrain.WALL
					|| Dungeon.level.map[action.dst] == Terrain.WALL_DECO
					|| Dungeon.level.map[action.dst] == Terrain.MINE_CRYSTAL
					|| Dungeon.level.map[action.dst] == Terrain.MINE_BOULDER)
				&& Dungeon.level.insideMap(action.dst)){
				sprite.attack(action.dst, new Callback() {
					@Override
					public void call() {

						boolean crystalAdjacent = false;
						for (int i : PathFinder.NEIGHBOURS8) {
							if (Dungeon.level.map[action.dst + i] == Terrain.MINE_CRYSTAL){
								crystalAdjacent = true;
								break;
							}
						}

						//1 hunger spent total
						if (Dungeon.level.map[action.dst] == Terrain.WALL_DECO){
							DarkGold gold = new DarkGold();
							// Credit the acting hero (Hero.this), not Dungeon.hero — a remote
							// miner must collect into THEIR own backpack/favor.
							if (gold.doPickUp( Hero.this )) {
								DarkGold existing = Hero.this.belongings.getItem(DarkGold.class);
								if (existing != null && existing.quantity()%5 == 0){
									if (existing.quantity() >= 40) {
										xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog(Hero.this, GLog.POSITIVE + Messages.get(DarkGold.class, "you_now_have", existing.quantity()));
									} else {
										xyz.gabriwar.warpedpixeldungeon.net.NetManager.heroLog(Hero.this, Messages.get(DarkGold.class, "you_now_have", existing.quantity()));
									}
								}
								spend(-Actor.TICK); //picking up the gold doesn't spend a turn here
							} else {
								Dungeon.level.drop( gold, pos ).sprite.drop();
							}
							PixelScene.shake(0.5f, 0.5f);
							CellEmitter.center( action.dst ).burst( Speck.factory( Speck.STAR ), 7 );
							Sample.INSTANCE.play( Assets.Sounds.EVOKE );
							Level.set( action.dst, Terrain.EMPTY_DECO );

							//mining gold doesn't break crystals
							crystalAdjacent = false;

						//4 hunger spent total
						} else if (Dungeon.level.map[action.dst] == Terrain.WALL){
							buff(Hunger.class).affectHunger(-3);
							PixelScene.shake(0.5f, 0.5f);
							CellEmitter.get( action.dst ).burst( Speck.factory( Speck.ROCK ), 2 );
							Sample.INSTANCE.play( Assets.Sounds.MINE );
							Level.set( action.dst, Terrain.EMPTY_DECO );

						//1 hunger spent total
						} else if (Dungeon.level.map[action.dst] == Terrain.MINE_CRYSTAL){
							Splash.at(action.dst, 0xFFFFFF, 5);
							Sample.INSTANCE.play( Assets.Sounds.SHATTER );
							Level.set( action.dst, Terrain.EMPTY );

						//1 hunger spent total
						} else if (Dungeon.level.map[action.dst] == Terrain.MINE_BOULDER){
							Splash.at(action.dst, 0x555555, 5);
							Sample.INSTANCE.play( Assets.Sounds.MINE, 0.6f );
							Level.set( action.dst, Terrain.EMPTY_DECO );
						}

						for (int i : PathFinder.NEIGHBOURS9) {
							Dungeon.level.discoverable[action.dst + i] = true;
						}
						for (int i : PathFinder.NEIGHBOURS9) {
							GameScene.updateMap( action.dst+i );
						}

						if (crystalAdjacent){
							sprite.parent.add(new Delayer(0.2f){
								@Override
								protected void onComplete() {
									boolean broke = false;
									for (int i : PathFinder.NEIGHBOURS8) {
										if (Dungeon.level.map[action.dst+i] == Terrain.MINE_CRYSTAL){
											Splash.at(action.dst+i, 0xFFFFFF, 5);
											Level.set( action.dst+i, Terrain.EMPTY );
											broke = true;
										}
									}
									if (broke){
										Sample.INSTANCE.play( Assets.Sounds.SHATTER );
									}

									for (int i : PathFinder.NEIGHBOURS9) {
										GameScene.updateMap( action.dst+i );
									}
									spendAndNext(TICK);
									ready();
								}
							});
						} else {
							spendAndNext(TICK);
							ready();
						}

						Dungeon.observe();
					}
				});
			} else {
				ready();
			}
			return false;
		} else if (getCloser( action.dst )) {

			return true;

		} else {
			ready();
			return false;
		}
	}
	
	private boolean actTransition(HeroAction.LvlTransition action ) {
		int stairs = action.dst;
		LevelTransition transition = Dungeon.level.getTransition(stairs);

		if (rooted) {
			PixelScene.shake(1, 1f);
			ready();
			return false;

		} else if (!Dungeon.level.locked && transition != null && transition.inside(pos)) {

			// Sprouted: warn before leaving if dewDraw is on and dew/charge would be lost
			if (Dungeon.dewDraw && !Dungeon.level.forcedone
					&& transition.type == xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition.Type.REGULAR_EXIT) {
				boolean hasDew = Dungeon.level.hasDew();
				boolean hasCharge = buff(Dewcharge.class) != null;
				if (hasDew || hasCharge) {
					Game.runOnRenderThread(new Callback() {
						@Override
						public void call() {
							GameScene.show(new xyz.gabriwar.warpedpixeldungeon.windows.WndDescend());
						}
					});
					ready();
					return false;
				}
			}

			// Net MP: don't actually descend. Park the hero at the transition
			// cell and let NetManager check whether everyone else is also waiting.
			// When all online claimed heroes are atExit, NetManager triggers the
			// real activateTransition for the group.
			if (xyz.gabriwar.warpedpixeldungeon.net.NetManager.isActive()) {
				atExit = true;
				atExitCell = pos;
				curAction = null;
				ready();
				if (sprite != null) sprite.visible = false;
				// On host, show the WndAtExit panel directly for the local hero.
				// (Remote players get it via the delta -> applyMainHeroDelta path.)
				if (this == Dungeon.hero
						&& xyz.gabriwar.warpedpixeldungeon.net.NetManager.isHost()) {
					xyz.gabriwar.warpedpixeldungeon.net.ui.WndAtExit.show();
				}
				xyz.gabriwar.warpedpixeldungeon.net.NetManager.onHeroReachedExit(this);
				return false;
			}

			if (Dungeon.level.activateTransition(this, transition)){
				curAction = null;
			} else {
				ready();
			}

			return false;

		} else if (getCloser( stairs )) {

			return true;

		} else {
			ready();
			return false;
		}
	}
	
	// Sprouted: pet interaction action
	private boolean actInteractPet(HeroAction.InteractPet action) {
		Char pet = action.pet;
		if (Dungeon.level.adjacent(pos, pet.pos)) {
			ready();
			sprite.turnTo(pos, pet.pos);
			if (pet instanceof PET) {
				((PET) pet).interact(this);
			}
			return false;
		} else {
			if (fieldOfView[pet.pos] && getCloser(pet.pos)) {
				return true;
			} else {
				ready();
				return false;
			}
		}
	}

	private boolean actCrouchToggle() {
		Crouching crouching = buff(Crouching.class);
		//dropping into a crouch is a quick single turn; only standing back up is
		//slow ("slow to rise"). keeps toggling from being a needless hunger sink.
		float cost;
		if (crouching != null) {
			crouching.detach();
			GLog.i(Messages.get(Hero.class, "crouch_stand"));
			cost = 2 * TICK;
		} else {
			Buff.affect(this, Crouching.class);
			GLog.i(Messages.get(Hero.class, "crouch_down"));
			cost = TICK;
		}
		spendAndNextConstant(cost);
		ready();
		return false;
	}

	// Sprouted: find the active pet on the current level
	public PET checkpet() {
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof PET) {
				return (PET) mob;
			}
		}
		return null;
	}

	// Sprouted: check if a pet is adjacent to the hero
	public boolean checkpetNear() {
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = pos + n;
			if (Actor.findChar(c) instanceof PET) {
				return true;
			}
		}
		return false;
	}

	// Sprouted: save pet stats before level transition, destroy mob, set petfollow
	public void pickUpPet() {
		PET pet = checkpet();
		if (pet != null && checkpetNear()) {
			petType = pet.type;
			petLevel = pet.level;
			petKills = pet.kills;
			petHP = pet.HP;
			petExperience = pet.experience;
			petCooldown = pet.cooldown;
			pet.destroy();
			Actor.remove(pet);
			petfollow = true;
		} else if (haspet && petfollow) {
			petfollow = true;
		} else {
			petfollow = false;
		}
	}

	private boolean actAttack( HeroAction.Attack action ) {

		attackTarget = action.target;

		if (isCharmedBy(attackTarget)){
			GLog.w( Messages.get(Charm.class, "cant_attack"));
			ready();
			return false;
		}

		if (attackTarget.isAlive() && canAttack(attackTarget) && attackTarget.invisible == 0) {

			if (heroClass != HeroClass.DUELIST
					&& hasTalent(Talent.AGGRESSIVE_BARRIER)
					&& buff(Talent.AggressiveBarrierCooldown.class) == null
					&& (HP / (float)HT) <= 0.5f){
				int shieldAmt = 1 + 2*pointsInTalent(Talent.AGGRESSIVE_BARRIER);
				Buff.affect(this, Barrier.class).setShield(shieldAmt);
				sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shieldAmt), FloatingText.SHIELDING);
				Buff.affect(this, Talent.AggressiveBarrierCooldown.class, 50f);

			}
			//attack target cleared on onAttackComplete
			sprite.attack( attackTarget.pos );

			return false;

		} else {

			if (fieldOfView[attackTarget.pos] && getCloser( attackTarget.pos )) {

				attackTarget = null;
				return true;

			} else {
				ready();
				attackTarget = null;
				return false;
			}

		}
	}

	public Char attackTarget(){
		return attackTarget;
	}
	
	public void rest( boolean fullRest ) {
		spendAndNextConstant( TIME_TO_REST );
		if (hasTalent(Talent.HOLD_FAST)){
			Buff.affect(this, HoldFast.class).pos = pos;
		}
		if (hasTalent(Talent.PATIENT_STRIKE)){
			Buff.affect(Dungeon.hero, Talent.PatientStrikeTracker.class).pos = Dungeon.hero.pos;
		}
		if (!fullRest) {
			if (sprite != null) {
				sprite.showStatus(CharSprite.DEFAULT, Messages.get(this, "wait"));
			}
		}
		resting = fullRest;
	}
	
	@Override
	public int attackProc( final Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );

		KindOfWeapon wep;
		if (RingOfForce.fightingUnarmed(this) && !RingOfForce.unarmedGetsWeaponEnchantment(this)){
			wep = null;
		} else {
			wep = belongings.attackingWeapon();
		}

		damage = Talent.onAttackProc( this, enemy, damage );

		if (buff(WeaponEnhance.class) != null) {
			buff(WeaponEnhance.class).attackProc();
		}

		if (wep != null) {
			damage = wep.proc( this, enemy, damage );
		} else {
			boolean wasEnemy = enemy.alignment == Alignment.ENEMY;
			if (buff(BodyForm.BodyFormBuff.class) != null
					&& buff(BodyForm.BodyFormBuff.class).enchant() != null){
				damage = buff(BodyForm.BodyFormBuff.class).enchant().proc(new WornShortsword(), this, enemy, damage);
			}
			if (!wasEnemy || enemy.alignment == Alignment.ENEMY) {
				if (buff(HolyWeapon.HolyWepBuff.class) != null) {
					int dmg = subClass == HeroSubClass.PALADIN ? 6 : 2;
					enemy.damage(Math.round(dmg * Weapon.Enchantment.genericProcChanceMultiplier(this)), HolyWeapon.INSTANCE);
				}
				if (buff(Smite.SmiteTracker.class) != null) {
					enemy.damage(Smite.bonusDmg(this, enemy), Smite.INSTANCE);
				}
			}
		}
		
		//skill tree on-hit hooks
		boolean skillRanged = wep instanceof MissileWeapon;
		damage = heroSkills.allOnHit(enemy, damage, skillRanged);
		if (!skillRanged){
			//Warrior: KnockBack - shove the target away
			if (heroSkills.anyKnocksBack() && enemy.isAlive() && enemy.pos != pos){
				int oppositeHero = enemy.pos + (enemy.pos - pos);
				Ballistica trajectory = new Ballistica(enemy.pos, oppositeHero, Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(enemy, trajectory, 1, true, false, this);
			}
			//Warrior: Rampage - splash the whole ring
			if (heroSkills.anyAoEDamage()){
				for (int n : PathFinder.NEIGHBOURS8){
					Char ch = Actor.findChar(pos + n);
					if (ch != null && ch != enemy && ch.alignment == Alignment.ENEMY && ch.isAlive()){
						ch.damage(damage, this);
					}
				}
			}
			//Rogue: Venom
			xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill venomSkill = heroSkills.rollVenomous();
			if (venomSkill != null && enemy.isAlive()){
				//Scorpion deepens whatever venom lands
				Buff.affect(enemy, Poison.class).set(2 + venomSkill.level + heroSkills.allVenomBonus());
			}
			//Rogue: Silent Death - only rolled against sleeping prey
			if (enemy instanceof Mob && ((Mob) enemy).state == ((Mob) enemy).SLEEPING
					&& heroSkills.anyInstantKill() && enemy.isAlive()){
				enemy.die(this);
			}
			//Rogue: Double Strike
			if (heroSkills.anyDoubleStab() && enemy.isAlive()){
				enemy.damage(damageRoll(), this);
			}
		} else {
			//Huntress: Knee Shot
			xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill crippleSkill = heroSkills.rollCripple();
			if (crippleSkill != null && enemy.isAlive()){
				Buff.prolong(enemy, Cripple.class, 3 + crippleSkill.level);
			}
			//Huntress: Double Shot - a second projectile's worth of damage
			if (!extraShotAttack && heroSkills.anyDoubleShot() && enemy.isAlive()){
				enemy.damage(damageRoll(), this);
			}
			//Huntress: Bombvoyage - the projectile carries a bomb
			if (!extraShotAttack && heroSkills.anyArrowToBomb()){
				new Bomb().explode(enemy.pos);
			}
			//Huntress: Iron Tip - punch through to whoever stands behind
			xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill pierceSkill = heroSkills.rollPassThrough();
			if (pierceSkill != null && enemy.pos != pos){
				int beyond = enemy.pos + (enemy.pos - pos);
				Ballistica path = new Ballistica(enemy.pos, beyond, Ballistica.PROJECTILE);
				for (int c : path.subPath(1, pierceSkill.passThroughTargets(false))){
					Char next = Actor.findChar(c);
					if (next != null && next.alignment == Alignment.ENEMY){
						next.damage(damage, this);
						break;
					}
				}
			}
		}

		switch (subClass) {
		case SNIPER:
			if (wep instanceof MissileWeapon && !(wep instanceof SpiritBow.SpiritArrow) && enemy != this) {
				Actor.add(new Actor() {
					
					{
						actPriority = VFX_PRIO;
					}
					
					@Override
					protected boolean act() {
						if (enemy.isAlive()) {
							if (hasTalent(Talent.SHARED_UPGRADES)){
								int levelBonus = Math.min( 2*pointsInTalent(Talent.SHARED_UPGRADES), wep.buffedLvl() );
								// bonus dmg is 16.67% x weapon level, max of 2/4/6
								float bonusDmg = levelBonus/6f;
								Buff.prolong(Hero.this, SnipersMark.class, SnipersMark.DURATION + levelBonus).set(enemy.id(), bonusDmg);
							} else {
								Buff.prolong(Hero.this, SnipersMark.class, SnipersMark.DURATION).set(enemy.id(), 0);
							}
						}
						Actor.remove(this);
						return true;
					}
				});
			}
			break;
		default:
		}
		
		return damage;
	}
	
	@Override
	public int defenseProc( Char enemy, int damage ) {

		//skill tree: subclass reactive skills (thorns, retribution...)
		damage = heroSkills.allOnDefend(enemy, damage);

		if (damage > 0 && subClass == HeroSubClass.BERSERKER){
			Berserk berserk = Buff.affect(this, Berserk.class);
			berserk.damage(damage);
		}

		if (buff(ArmorEnhance.class) != null) {
			buff(ArmorEnhance.class).defenseProc();
		}

		if (belongings.armor() != null) {
			damage = belongings.armor().proc( enemy, this, damage );
		} else {
			if (buff(BodyForm.BodyFormBuff.class) != null
				&& buff(BodyForm.BodyFormBuff.class).glyph() != null){
				damage = buff(BodyForm.BodyFormBuff.class).glyph().proc(new ClothArmor(), enemy, this, damage);
			}
			if (buff(HolyWard.HolyArmBuff.class) != null){
				int blocking = subClass == HeroSubClass.PALADIN ? 3 : 1;
				damage -= Math.round(blocking * Armor.Glyph.genericProcChanceMultiplier(enemy));
			}
		}

		WandOfLivingEarth.RockArmor rockArmor = buff(WandOfLivingEarth.RockArmor.class);
		if (rockArmor != null) {
			damage = rockArmor.absorb(damage);
		}
		
		return super.defenseProc( enemy, damage );
	}

	@Override
	public int glyphLevel(Class<? extends Armor.Glyph> cls) {
		if (belongings.armor() != null && belongings.armor().hasGlyph(cls, this)){
			return Math.max(super.glyphLevel(cls), belongings.armor.buffedLvl());
		} else if (buff(BodyForm.BodyFormBuff.class) != null
				&& buff(BodyForm.BodyFormBuff.class).glyph() != null
				&& buff(BodyForm.BodyFormBuff.class).glyph().getClass() == cls){
			return belongings.armor() != null ? belongings.armor.buffedLvl() : 0;
		} else {
			return super.glyphLevel(cls);
		}
	}

	@Override
	public float glyphPower(Class<? extends Armor.Glyph> cls) {
		if (belongings.armor() != null && belongings.armor().getGlyph(cls) != null){
			return belongings.armor().getGlyph(cls).power();
		} else {
			return super.glyphPower(cls);
		}
	}

	@Override
	public void damage( int dmg, Object src ) {

		//skill tree: incoming damage passes through every learned skill
		if (dmg > 0){
			dmg = heroSkills.allIncomingDamage(dmg);
		}

		if (buff(TimekeepersHourglass.timeStasis.class) != null
				|| buff(TimeStasis.class) != null) {
			return;
		}

		//TODO hero cannot take damage in the vault tester area
		if (Dungeon.depth > 15 && Dungeon.branch == 1){
			dmg = 0;
		}

		//regular damage interrupt, triggers on any damage except specific mild DOT effects
		// unless the player recently hit 'continue moving', in which case this is ignored
		if (!(src instanceof Hunger || src instanceof Viscosity.DeferedDamage) && damageInterrupt) {
			interrupt();
		}

		if (this.buff(Drowsy.class) != null){
			Buff.detach(this, Drowsy.class);
			GLog.w( Messages.get(this, "pain_resist") );
		}

		//temporarily assign to a float to avoid rounding a bunch
		float damage = dmg;

		Endure.EndureTracker endure = buff(Endure.EndureTracker.class);
		if (!(src instanceof Char)){
			//reduce damage here if it isn't coming from a character (if it is we already reduced it)
			if (endure != null){
				damage = endure.adjustDamageTaken(dmg);
			}
			//the same also applies to challenge scroll damage reduction
			if (buff(ScrollOfChallenge.ChallengeArena.class) != null){
				damage *= 0.67f;
			}
			//and to monk meditate damage reduction
			if (buff(MonkEnergy.MonkAbility.Meditate.MeditateResistance.class) != null){
				damage *= 0.2f;
			}
		}

		//unused, could be removed
		CapeOfThorns.Thorns thorns = buff( CapeOfThorns.Thorns.class );
		if (thorns != null) {
			damage = thorns.proc((int)damage, (src instanceof Char ? (Char)src : null),  this);
		}

		if (buff(Talent.WarriorFoodImmunity.class) != null){
			if (pointsInTalent(Talent.IRON_STOMACH) == 1)       damage /= 4f;
			else if (pointsInTalent(Talent.IRON_STOMACH) == 2)  damage = 0;
		}

		dmg = Math.round(damage);

		LargeSword.LargeSwordBuff largeSwordBuff = buff(LargeSword.LargeSwordBuff.class);
		if (largeSwordBuff != null) {
			dmg = (int)Math.ceil(dmg * largeSwordBuff.getDefenseFactor());
		}

		//we ceil this one to avoid letting the player easily take 0 dmg from tenacity early
		dmg = (int)Math.ceil(dmg * RingOfTenacity.damageMultiplier( this ));

		int preHP = HP + shielding();
		if (src instanceof Hunger) preHP -= shielding();
		super.damage( dmg, src );
		int postHP = HP + shielding();
		if (src instanceof Hunger) postHP -= shielding();
		int effectiveDamage = preHP - postHP;

		//has to fire on the blow itself, not on the hero's next turn
		AutoPotion.trigger(this);

		if (effectiveDamage <= 0) return;

		if (buff(Challenge.DuelParticipant.class) != null){
			buff(Challenge.DuelParticipant.class).addDamage(effectiveDamage);
		}

		//flash red when hit for serious damage.
		float percentDMG = effectiveDamage / (float)preHP; //percent of current HP that was taken
		float percentHP = 1 - ((HT - postHP) / (float)HT); //percent health after damage was taken
		// The flash intensity increases primarily based on damage taken and secondarily on missing HP.
		float flashIntensity = 0.25f * (percentDMG * percentDMG) / percentHP;
		//if the intensity is very low don't flash at all
		if (flashIntensity >= 0.05f){
			flashIntensity = Math.min(1/3f, flashIntensity); //cap intensity at 1/3
			GameScene.flash( (int)(0xFF*flashIntensity) << 16 );
			if (isAlive()) {
				if (flashIntensity >= 1/6f) {
					Sample.INSTANCE.play(Assets.Sounds.HEALTH_CRITICAL, 1/3f + flashIntensity * 2f);
				} else {
					Sample.INSTANCE.play(Assets.Sounds.HEALTH_WARN, 1/3f + flashIntensity * 4f);
				}
				//hero gets interrupted on taking serious damage, regardless of any other factor
				interrupt();
				damageInterrupt = true;
			}
		}
	}
	
	public void checkVisibleMobs() {
		ArrayList<Mob> visible = new ArrayList<>();

		boolean newMob = false;

		Mob target = null;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (fieldOfView[ m.pos ] && m.landmark() != null){
				Notes.add(m.landmark());
			}

			if (fieldOfView[ m.pos ] && m.alignment == Alignment.ENEMY) {
				visible.add(m);
				if (!visibleEnemies.contains( m )) {
					newMob = true;
				}

				//only do a simple check for mind visioned enemies, better performance
				if ((!mindVisionEnemies.contains(m) && QuickSlotButton.autoAim(m) != -1)
						|| (mindVisionEnemies.contains(m) && new Ballistica( pos, m.pos, Ballistica.PROJECTILE ).collisionPos == m.pos)) {
					if (target == null) {
						target = m;
					} else if (distance(target) > distance(m)) {
						target = m;
					}
					if (m instanceof Snake && Dungeon.level.distance(m.pos, pos) <= 4
							&& !Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_EXAMINING)){
						GameScene.flashForDocument(Document.ADVENTURERS_GUIDE, Document.GUIDE_EXAMINING);
						//we set to read here to prevent this message popping up a bunch
						Document.ADVENTURERS_GUIDE.readPage(Document.GUIDE_EXAMINING);
					}
				}
			}
		}

		Char lastTarget = QuickSlotButton.lastTarget;
		if (target != null && (lastTarget == null ||
							!lastTarget.isAlive() || !lastTarget.isActive() ||
							lastTarget.alignment == Alignment.ALLY ||
							!fieldOfView[lastTarget.pos])){
			QuickSlotButton.target(target);
		}
		
		if (newMob) {
			if (resting){
				Dungeon.observe();
			}
			interrupt();
		}

		visibleEnemies = visible;

		//we also scan for blob landmarks here
		for (Blob b : Dungeon.level.blobs.values().toArray(new Blob[0])){
			if (b.volume > 0 && b.landmark() != null && !Notes.contains(b.landmark())){
				int cell;
				boolean found = false;
				//if a single cell within the blob is visible, we add the landmark
				for (int i=b.area.top; i < b.area.bottom; i++) {
					for (int j = b.area.left; j < b.area.right; j++) {
						cell = j + i* Dungeon.level.width();
						if (fieldOfView[cell] && b.cur[cell] > 0) {
							Notes.add( b.landmark() );
							found = true;
							break;
						}
					}
					if (found) break;
				}

				//Clear blobs that only exist for landmarks.
				// Might want to make this a properly if it's used more
				if (found && b instanceof WeakFloorRoom.WellID){
					b.fullyClear();
				}
			}
		}
	}
	
	public int visibleEnemies() {
		return visibleEnemies.size();
	}
	
	public Mob visibleEnemy( int index ) {
		return visibleEnemies.get(index % visibleEnemies.size());
	}

	public ArrayList<Mob> getVisibleEnemies(){
		return new ArrayList<>(visibleEnemies);
	}
	
	private boolean walkingToVisibleTrapInFog = false;
	
	private boolean getCloser( final int target ) {

		if (target == pos)
			return false;

		if (rooted) {
			PixelScene.shake( 1, 1f );
			return false;
		}

		// Heavy (Butterlion warden): each step shakes the ground, crushing adjacent enemies
		if (buff(Heavy.class) != null) {
			CellEmitter.get( pos ).start( Speck.factory( Speck.ROCK ), 0.07f, 10 );
			PixelScene.shake( 3, 0.7f );
			Sample.INSTANCE.play( Assets.Sounds.ROCKS );
			for (int i : PathFinder.NEIGHBOURS8) {
				Char ch = Actor.findChar(pos + i);
				if (ch instanceof Mob && ch.alignment != Alignment.ALLY) {
					ch.damage(STR(), this);
				}
			}
		}

		if (buff(Coughing.class) != null && Random.Float() < 0.3f && !isImmune(Coughing.class)) {
			GLog.n(Messages.get(this, "cough"));
		}

		int step = -1;
		
		if (Dungeon.level.adjacent( pos, target )) {

			path = null;

			if (Actor.findChar( target ) == null) {
				if (Dungeon.level.passable[target] || Dungeon.level.avoid[target]) {
					step = target;
				}
				if (walkingToVisibleTrapInFog
						&& Dungeon.level.traps.get(target) != null
						&& Dungeon.level.traps.get(target).visible
						&& Dungeon.level.traps.get(target).active){
					return false;
				}
			}
			
		} else {

			boolean newPath = false;
			if (path == null || path.isEmpty() || !Dungeon.level.adjacent(pos, path.getFirst()))
				newPath = true;
			else if (path.getLast() != target)
				newPath = true;
			else {
				if (!Dungeon.level.passable[path.get(0)] || Actor.findChar(path.get(0)) != null) {
					newPath = true;
				}
			}

			if (newPath) {

				int len = Dungeon.level.length();
				boolean[] p = Dungeon.level.passable;
				boolean[] v = Dungeon.level.visited;
				boolean[] m = Dungeon.level.mapped;
				boolean[] passable = new boolean[len];
				for (int i = 0; i < len; i++) {
					passable[i] = p[i] && (v[i] || m[i]);
				}

				// Avoid auto-walking on plants (same idea as traps using avoid terrain).
				// Direct adjacent steps still work — only pathfinding routes around them.
				for (int i = 0; i < len; i++) {
					if (passable[i] && Dungeon.level.plants.get(i) != null) {
						passable[i] = false;
					}
				}
				// Keep target passable so the hero can still path TO a plant cell
				passable[target] = p[target] && (v[target] || m[target]);

				PathFinder.Path newpath = Dungeon.findPath(this, target, passable, fieldOfView, true);
				if (newpath != null && path != null && newpath.size() > 2*path.size()){
					path = null;
				} else {
					path = newpath;
				}
			}

			if (path == null) return false;
			step = path.removeFirst();

		}

		if (step != -1) {

			float delay = 1;

			if (buff(GreaterHaste.class) != null){
				delay = 0;
			}

			if (Dungeon.level.pit[step] && !Dungeon.level.solid[step]
					&& (!flying || buff(Levitation.class) != null && buff(Levitation.class).detachesWithinDelay(delay / speed()))){
				if (!Chasm.jumpConfirmed){
					Chasm.heroJump(this);
					interrupt();
				} else {
					flying = false;
					remove(buff(Levitation.class)); //directly remove to prevent cell pressing
					Chasm.heroFall(target);
				}
				canSelfTrample = false;
				return false;
			}

			if (buff(GreaterHaste.class) != null){
				buff(GreaterHaste.class).spendMove();
			}

			if (subClass == HeroSubClass.FREERUNNER){
				Buff.affect(this, Momentum.class).gainStack();
			}
			
			sprite.move(pos, step);
			move(step);

			spend( delay / speed() );
			
			search(false);

			return true;

		} else {

			return false;
			
		}

	}
	
	public boolean handle( int cell ) {
		
		if (cell == -1) {
			return false;
		}

		//a real player tap while marching to a map waypoint cancels the march
		//(the map's own aim calls are flagged and pass through)
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel){
			xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel ow
					= (xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level;
			if (ow.waypointActive && ow.waypointMarching && !ow.aimingWaypoint
					&& System.currentTimeMillis() - ow.waypointSetAt > 400){
				//pause the march, keep the waypoint - the hud compass resumes it
				ow.waypointMarching = false;
				xyz.gabriwar.warpedpixeldungeon.utils.GLog.i(
						Messages.get( xyz.gabriwar.warpedpixeldungeon.windows.WndWorldMap.class, "waypoint_paused" ) );
			}
		}

		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
			Dungeon.level.updateFieldOfView( this, fieldOfView );
		}

		if (!Dungeon.level.visited[cell] && !Dungeon.level.mapped[cell]
				&& Dungeon.level.traps.get(cell) != null
				&& Dungeon.level.traps.get(cell).visible
				&& Dungeon.level.traps.get(cell).active) {
			walkingToVisibleTrapInFog = true;
		} else {
			walkingToVisibleTrapInFog = false;
		}
		
		Char ch = Actor.findChar( cell );
		Heap heap = Dungeon.level.heaps.get( cell );


		//a road signpost is read on CLICK when the hero is beside it
		if (Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
				&& Dungeon.level.map[cell] == Terrain.SIGN
				&& Dungeon.level.adjacent( cell, pos )){
			((xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel) Dungeon.level).readSign( cell );
			ready();
			return false;
		}

		if (Dungeon.level.map[cell] == Terrain.ALCHEMY && cell != pos) {
			
			curAction = new HeroAction.Alchemy( cell );
			
		} else if (fieldOfView[cell] && ch instanceof Mob) {

			if (((Mob) ch).heroShouldInteract()) {
				curAction = new HeroAction.Interact( ch );
			} else {
				curAction = new HeroAction.Attack( ch );
			}

		//TODO perhaps only trigger this if hero is already adjacent? reducing mistaps
		} else if (Dungeon.level instanceof MiningLevel &&
					belongings.getItem(Pickaxe.class) != null &&
				(Dungeon.level.map[cell] == Terrain.WALL
						|| Dungeon.level.map[cell] == Terrain.WALL_DECO
						|| Dungeon.level.map[cell] == Terrain.MINE_CRYSTAL
						|| Dungeon.level.map[cell] == Terrain.MINE_BOULDER)){

			curAction = new HeroAction.Mine( cell );

		} else if (heap != null
				//moving to an item doesn't auto-pickup when enemies are near...
				&& (visibleEnemies.size() == 0 || cell == pos ||
				//...but only for standard heaps. Chests and similar open as normal.
				(heap.type != Type.HEAP && heap.type != Type.FOR_SALE))) {

			switch (heap.type) {
			case HEAP:
				curAction = new HeroAction.PickUp( cell );
				break;
			case FOR_SALE:
				curAction = heap.size() == 1 && heap.peek().value() > 0 ?
					new HeroAction.Buy( cell ) :
					new HeroAction.PickUp( cell );
				break;
			default:
				curAction = new HeroAction.OpenChest( cell );
			}
			
		} else if (Dungeon.level.map[cell] == Terrain.LOCKED_DOOR
				|| Dungeon.level.map[cell] == Terrain.HERO_LKD_DR
				|| Dungeon.level.map[cell] == Terrain.CRYSTAL_DOOR
				|| Dungeon.level.map[cell] == Terrain.LOCKED_EXIT) {
			
			curAction = new HeroAction.Unlock( cell );
			
		} else if (Dungeon.level.getTransition(cell) != null
				//moving to a transition doesn't automatically trigger it when enemies are near
				&& (visibleEnemies.size() == 0 || cell == pos)
				&& !Dungeon.level.locked
				&& !Dungeon.level.plants.containsKey(cell)
				//the town and the mines below it are a normal descending act, so their exits
				//have to stay clickable past 26 like any other staircase; so are the
				//surface town's doorways and stairs
				&& (Dungeon.depth < 26
					|| Dungeon.townCheck(Dungeon.depth)
					|| Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel
					|| Dungeon.level.getTransition(cell).type == LevelTransition.Type.REGULAR_ENTRANCE) ) {

			curAction = new HeroAction.LvlTransition( cell );
			
		}  else {
			
			curAction = new HeroAction.Move( cell );
			lastAction = null;
			
		}

		return true;
	}
	
	public void earnExp( int exp, Class source ) {

		//xp granted by ascension challenge is only for on-exp gain effects
		if (source != AscensionChallenge.class) {
			this.exp += exp;
		}
		float percent = exp/(float)maxExp();

		EtherealChains.chainsRecharge chains = buff(EtherealChains.chainsRecharge.class);
		if (chains != null) chains.gainExp(percent);

		HornOfPlenty.hornRecharge horn = buff(HornOfPlenty.hornRecharge.class);
		if (horn != null) horn.gainCharge(percent);
		
		AlchemistsToolkit.kitEnergy kit = buff(AlchemistsToolkit.kitEnergy.class);
		if (kit != null) kit.gainCharge(percent);

		MasterThievesArmband.Thievery armband = buff(MasterThievesArmband.Thievery.class);
		if (armband != null) armband.gainCharge(percent);

		Berserk berserk = buff(Berserk.class);
		if (berserk != null) berserk.recover(percent);
		
		if (source != PotionOfExperience.class) {
			for (Item i : belongings) {
				i.onHeroGainExp(percent, this);
			}
			if (buff(Talent.RejuvenatingStepsFurrow.class) != null){
				buff(Talent.RejuvenatingStepsFurrow.class).countDown(percent*200f);
				if (buff(Talent.RejuvenatingStepsFurrow.class).count() <= 0){
					buff(Talent.RejuvenatingStepsFurrow.class).detach();
				}
			}
			if (buff(ElementalStrike.ElementalStrikeFurrowCounter.class) != null){
				buff(ElementalStrike.ElementalStrikeFurrowCounter.class).countDown(percent*20f);
				if (buff(ElementalStrike.ElementalStrikeFurrowCounter.class).count() <= 0){
					buff(ElementalStrike.ElementalStrikeFurrowCounter.class).detach();
				}
			}
			if (buff(HallowedGround.HallowedFurrowTracker.class) != null){
				buff(HallowedGround.HallowedFurrowTracker.class).countDown(percent*100f);
				if (buff(HallowedGround.HallowedFurrowTracker.class).count() <= 0){
					buff(HallowedGround.HallowedFurrowTracker.class).detach();
				}
			}
		}
		
		boolean levelUp = false;
		while (this.exp >= maxExp()) {
			this.exp -= maxExp();

			if (buff(Talent.WandPreservationCounter.class) != null
				&& pointsInTalent(Talent.WAND_PRESERVATION) == 2){
				buff(Talent.WandPreservationCounter.class).detach();
			}

			// no level cap: leveling is uncapped, only the XP curve (maxExp) gets harder
			lvl++;
			levelUp = true;

			if (buff(ElixirOfMight.HTBoost.class) != null){
				buff(ElixirOfMight.HTBoost.class).onLevelUp();
			}

			updateHT( true );
			attackSkill++;
			defenseSkill++;

			// skills system: every class grows a mana pool as it levels,
			// on top of the WndLevelUp magic choice (+2 MT each)
			MT += 2;
			MP += 2;
			//3 per level: this wallet now pays for talents AND skills
			xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill.availableSkill += 3;
			GLog.p( "Gained 3 skill points!" );

			// Sprouted: Warlock subclass HP overfill on level-up
			if (subClass == HeroSubClass.WARLOCK) {
				int overfill = lvl;
				if (overfill > 0) {
					HP = HT + overfill;
					sprite.emitter().burst(Speck.factory(Speck.HEALING), 1);
					GLog.w(Messages.get(this, "overfill", overfill));
				}
			}

		}
		
		if (levelUp) {

			if (sprite != null) {
				GLog.newLine();
				GLog.p( Messages.get(this, "new_level") );
				sprite.showStatus( CharSprite.POSITIVE, Messages.get(Hero.class, "level_up") );
				Sample.INSTANCE.play( Assets.Sounds.LEVELUP );
				CellEmitter.center(pos).burst(Speck.factory(Speck.STAR), 10);
				sprite.emitter().burst(Speck.factory(Speck.LIGHT), 4);
				//every level now grants a talent point (post-30 ones go to the
				//shared any-tier pool), so always flag it
				GLog.newLine();
				GLog.p( Messages.get(this, "new_talent") );
				StatusPane.talentBlink = 10f;
				WndHero.lastIdx = 1;
			}
			
			Item.updateQuickslot();
			
			Badges.validateLevelReached();
		}
	}
	
	public int maxExp() {
		return maxExp( lvl );
	}
	
	public static int maxExp( int lvl ){
		int base = 5 + lvl * 5;
		// vanilla linear curve up to MAX_LEVEL so all talents stay earnable at the normal pace
		if (lvl <= MAX_LEVEL) {
			return base;
		}
		// past the old cap leveling is uncapped but the XP requirement grows exponentially
		double scaled = base * Math.pow( 1.15, lvl - MAX_LEVEL );
		return scaled >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
	}
	
	public boolean isStarving() {
		return Buff.affect(this, Hunger.class).isStarving();
	}
	
	@Override
	public boolean add( Buff buff ) {

		if (buff.type == Buff.buffType.NEGATIVE &&
				(buff(TimekeepersHourglass.timeStasis.class) != null || buff(TimeStasis.class) != null)) {
			return false;
		}

		boolean added = super.add( buff );

		if (sprite != null && added) {
			String msg = buff.heroMessage();
			if (msg != null){
				GLog.w(msg);
			}

			if (buff instanceof Paralysis || buff instanceof Vertigo || buff instanceof Drunk) {
				interrupt();
			}

		}
		
		BuffIndicator.refreshHero();

		return added;
	}
	
	@Override
	public boolean remove( Buff buff ) {
		if (super.remove( buff )) {
			BuffIndicator.refreshHero();
			return true;
		}
		return false;
	}
	
	@Override
	protected synchronized void onRemove() {
		//same as super, except we retain charger for rankings purposes
		for (Buff buff : buffs()) {
			if (buff instanceof MeleeWeapon.Charger){
				Actor.remove(buff);
			} else {
				buff.detach();
			}
		}
	}

	@Override
	public void die( Object cause ) {
		
		curAction = null;

		Ankh ankh = null;

		//look for ankhs in player inventory, prioritize ones which are blessed.
		for (Ankh i : belongings.getAllItems(Ankh.class)){
			if (ankh == null || i.isBlessed()) {
				ankh = i;
			}
		}

		if (ankh != null) {
			interrupt();

			if (ankh.isBlessed()) {
				this.HP = HT / 4;

				PotionOfHealing.cure(this);
				Buff.prolong(this, Invulnerability.class, Invulnerability.DURATION);

				SpellSprite.show(this, SpellSprite.ANKH);
				GameScene.flash(0x80FFFF40);
				Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
				GLog.w(Messages.get(this, "revive"));
				Statistics.ankhsUsed++;
				Catalog.countUse(Ankh.class);

				ankh.detach(belongings.backpack);

				for (Char ch : Actor.chars()) {
					if (ch instanceof DriedRose.GhostHero) {
						((DriedRose.GhostHero) ch).sayAnhk();
						return;
					}
				}
			} else {

				//this is hacky, basically we want to declare that a wndResurrect exists before
				//it actually gets created. This is important so that the game knows to not
				//delete the run or submit it to rankings, because a WndResurrect is about to exist
				//this is needed because the actual creation of the window is delayed here
				WndResurrect.instance = new Object();
				Ankh finalAnkh = ankh;
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show( new WndResurrect(finalAnkh) );
					}
				});

				if (cause instanceof Hero.Doom) {
					((Hero.Doom)cause).onDeath();
				}

				SacrificialFire.Marked sacMark = buff(SacrificialFire.Marked.class);
				if (sacMark != null){
					sacMark.detach();
				}

			}
			return;
		}
		
		Actor.fixTime();
		super.die( cause );
		reallyDie( cause );
	}
	
	public static void reallyDie( Object cause ) {
		
		int length = Dungeon.level.length();
		int[] map = Dungeon.level.map;
		boolean[] visited = Dungeon.level.visited;
		boolean[] discoverable = Dungeon.level.discoverable;
		
		for (int i=0; i < length; i++) {
			
			int terr = map[i];
			
			if (discoverable[i]) {
				
				visited[i] = true;
				if ((Terrain.flags[terr] & Terrain.SECRET) != 0) {
					Dungeon.level.discover( i );
				}
			}
		}
		
		Bones.leave();
		
		Dungeon.observe();
		GameScene.updateFog();
				
		Dungeon.hero.belongings.identify();

		int pos = Dungeon.hero.pos;

		ArrayList<Integer> passable = new ArrayList<>();
		for (Integer ofs : PathFinder.NEIGHBOURS8) {
			int cell = pos + ofs;
			if ((Dungeon.level.passable[cell] || Dungeon.level.avoid[cell]) && Dungeon.level.heaps.get( cell ) == null) {
				passable.add( cell );
			}
		}
		Collections.shuffle( passable );

		ArrayList<Item> items = new ArrayList<>(Dungeon.hero.belongings.backpack.items);
		for (Integer cell : passable) {
			if (items.isEmpty()) {
				break;
			}

			Item item = Random.element( items );
			Dungeon.level.drop( item, cell ).sprite.drop( pos );
			items.remove( item );
		}

		for (Char c : Actor.chars()){
			if (c instanceof DriedRose.GhostHero){
				((DriedRose.GhostHero) c).sayHeroKilled();
			}
		}

		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.gameOver();
				Sample.INSTANCE.play( Assets.Sounds.DEATH );
			}
		});

		if (cause instanceof Hero.Doom) {
			((Hero.Doom)cause).onDeath();
		}

		Dungeon.deleteGame( GamesInProgress.curSlot, true );
	}

	//effectively cache this buff to prevent having to call buff(...) a bunch.
	//This is relevant because we call isAlive during drawing, which has both performance
	//and thread coordination implications if that method calls buff(...) frequently
	private Berserk berserk;

	@Override
	public boolean isAlive() {
		
		if (HP <= 0){
			if (berserk == null) berserk = buff(Berserk.class);
			return berserk != null && berserk.berserking();
		} else {
			berserk = null;
			return super.isAlive();
		}
	}

	@Override
	public void move(int step, boolean travelling) {
		boolean wasHighGrass = Dungeon.level.map[step] == Terrain.HIGH_GRASS;

		super.move( step, travelling);
		
		if (!flying && travelling) {
			if (Dungeon.level.water[pos]) {
				Sample.INSTANCE.play( Assets.Sounds.WATER, 1, Random.Float( 0.8f, 1.25f ) );
				// 2% chance per water step to soak shoes; already soaked adds duration
				SoakedShoes soaked = buff(SoakedShoes.class);
				if (soaked != null) {
					soaked.add(SoakedShoes.STEP_REFRESH);
				} else if (Random.Float() < 0.02f) {
					Buff.affect(this, SoakedShoes.class).add(SoakedShoes.STEP_DURATION);
					GLog.w(Messages.get(SoakedShoes.class, "onset"));
				}
			} else if (Dungeon.level.map[pos] == Terrain.FROZEN_WATER) {
				// Ice step: high-pitched crunchy sound
				Sample.INSTANCE.play( Assets.Sounds.SHATTER, 0.3f, Random.Float( 1.5f, 2.0f ) );
			} else if (Dungeon.level.map[pos] == Terrain.EMPTY_SP) {
				Sample.INSTANCE.play( Assets.Sounds.STURDY, 1, Random.Float( 0.96f, 1.05f ) );
			} else if (Dungeon.level.map[pos] == Terrain.GRASS
					|| Dungeon.level.map[pos] == Terrain.EMBERS
					|| Dungeon.level.map[pos] == Terrain.FURROWED_GRASS){
				if (step == pos && wasHighGrass) {
					Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1, Random.Float( 0.96f, 1.05f ) );
				} else {
					Sample.INSTANCE.play( Assets.Sounds.GRASS, 1, Random.Float( 0.96f, 1.05f ) );
				}
			} else {
				Sample.INSTANCE.play( Assets.Sounds.STEP, 1, Random.Float( 0.96f, 1.05f ) );
			}
		}
	}
	
	@Override
	public void onAttackComplete() {

		if (attackTarget == null){
			curAction = null;
			super.onAttackComplete();
			return;
		}
		
		AttackIndicator.target(attackTarget);
		boolean wasEnemy = attackTarget.alignment == Alignment.ENEMY
				|| (attackTarget instanceof Mimic && attackTarget.alignment == Alignment.NEUTRAL);

		boolean hit = attack(attackTarget);
		
		Invisibility.dispel();
		spend( attackDelay() );

		if (hit && subClass == HeroSubClass.GLADIATOR && wasEnemy){
			Buff.affect( this, Combo.class ).hit(attackTarget);
		}

		if (hit && heroClass == HeroClass.DUELIST && wasEnemy){
			Buff.affect( this, Sai.ComboStrikeTracker.class).addHit();
		}

		curAction = null;
		attackTarget = null;

		super.onAttackComplete();
	}
	
	@Override
	public void onMotionComplete() {
		GameScene.checkKeyHold();
	}
	
	@Override
	public void onOperateComplete() {
		
		if (curAction instanceof HeroAction.Unlock) {

			int doorCell = ((HeroAction.Unlock)curAction).dst;
			int door = Dungeon.level.map[doorCell];

			SkeletonKey.keyRecharge skele = buff(SkeletonKey.keyRecharge.class);
			SkeletonKey.KeyReplacementTracker keyUseTrack = buff(SkeletonKey.KeyReplacementTracker.class);

			if (skele != null && skele.isCursed() && Random.Int(6) != 0){
				GLog.n(Messages.get(this, "key_distracted"));
				spendAndNext(2*Key.TIME_TO_UNLOCK);
				Buff.affect(this, Hunger.class).affectHunger(-4);
			} else if (Dungeon.level.distance(pos, doorCell) <= 1) {
				boolean hasKey = true;
				if (door == Terrain.LOCKED_DOOR) {
					hasKey = Notes.remove(new IronKey(Dungeon.depth));
					if (hasKey && keyUseTrack != null){
						keyUseTrack.processIronLockOpened();
					}
					if (!hasKey && GoldenSkeletonKey.confirmedCell == doorCell){
						hasKey = GoldenSkeletonKey.removeAny();
					}
					if (hasKey) {
						Level.set(doorCell, Terrain.DOOR);
					}
				} else if (door == Terrain.HERO_LKD_DR) {
					hasKey = true;
					Level.set(doorCell, Terrain.DOOR);
					GLog.i( Messages.get(SkeletonKey.class, "force_lock"));
				} else if (door == Terrain.CRYSTAL_DOOR) {
					hasKey = Notes.remove(new CrystalKey(Dungeon.depth));
					if (hasKey && keyUseTrack != null){
						keyUseTrack.processCrystalLockOpened();
					}
					if (!hasKey && GoldenSkeletonKey.confirmedCell == doorCell){
						hasKey = GoldenSkeletonKey.removeAny();
					}
					if (hasKey) {
						Level.set(doorCell, Terrain.EMPTY);
						Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
						CellEmitter.get( doorCell ).start( Speck.factory( Speck.DISCOVER ), 0.025f, 20 );
					}
				} else {
					hasKey = Notes.remove(new WornKey(Dungeon.depth));
					if (!hasKey && GoldenSkeletonKey.confirmedCell == doorCell){
						hasKey = GoldenSkeletonKey.removeAny();
					}
					if (hasKey) {
						Level.set(doorCell, Terrain.UNLOCKED_EXIT);
					}
				}
				
				if (hasKey) {
					GameScene.updateKeyDisplay();
					GameScene.updateMap(doorCell);
					spend(Key.TIME_TO_UNLOCK);
				}
			}
			
		} else if (curAction instanceof HeroAction.OpenChest) {
			
			Heap heap = Dungeon.level.heaps.get( ((HeroAction.OpenChest)curAction).dst );
			SkeletonKey.keyRecharge skele = buff(SkeletonKey.keyRecharge.class);
			SkeletonKey.KeyReplacementTracker keyUseTrack = buff(SkeletonKey.KeyReplacementTracker.class);

			if (skele != null && skele.isCursed()
					&& (heap.type == Type.LOCKED_CHEST || heap.type == Type.CRYSTAL_CHEST)
					&& Random.Int(6) != 0){
				GLog.n(Messages.get(this, "key_distracted"));
				spend(2*Key.TIME_TO_UNLOCK);
				Buff.affect(this, Hunger.class).affectHunger(-4);
			} else if (Dungeon.level.distance(pos, heap.pos) <= 1){
				boolean hasKey = true;
				if (heap.type == Type.SKELETON || heap.type == Type.REMAINS) {
					Sample.INSTANCE.play( Assets.Sounds.BONES );
				} else if (heap.type == Type.LOCKED_CHEST){
					hasKey = Notes.remove(new GoldenKey(Dungeon.depth));
					if (hasKey && keyUseTrack != null){
						keyUseTrack.processGoldLockOpened();
					}
					//fall back to the confirmed master key
					if (!hasKey && GoldenSkeletonKey.confirmedCell == heap.pos){
						hasKey = GoldenSkeletonKey.removeAny();
					}
				} else if (heap.type == Type.CRYSTAL_CHEST){
					hasKey = Notes.remove(new CrystalKey(Dungeon.depth));
					if (hasKey && keyUseTrack != null){
						keyUseTrack.processCrystalLockOpened();
					}
					//fall back to the confirmed master key
					if (!hasKey && GoldenSkeletonKey.confirmedCell == heap.pos){
						hasKey = GoldenSkeletonKey.removeAny();
					}
				}

				if (hasKey) {
					GameScene.updateKeyDisplay();
					heap.open(this);
					spend(Key.TIME_TO_UNLOCK);
				}
			}
			
		}
		GoldenSkeletonKey.confirmedCell = -1;
		curAction = null;

		if (!ready) {
			super.onOperateComplete();
		}
	}

	public boolean search( boolean intentional ) {
		
		if (!isAlive()) return false;
		
		boolean smthFound = false;

		boolean circular = pointsInTalent(Talent.WIDE_SEARCH) == 1;
		int distance = heroClass == HeroClass.ROGUE ? 2 : 1;
		if (hasTalent(Talent.WIDE_SEARCH)) distance++;

		//Unleashed PD port: ring of searching widens deliberate searches
		if (intentional) distance += RingOfSearching.searchDistanceBonus(this);
		distance = Math.max(1, distance);
		
		boolean foresight = buff(Foresight.class) != null;
		boolean foresightScan = foresight && !Dungeon.level.mapped[pos];

		if (foresightScan){
			Dungeon.level.mapped[pos] = true;
		}

		if (foresight) {
			distance = Foresight.DISTANCE;
			circular = true;
		}

		Point c = Dungeon.level.cellToPoint(pos);

		TalismanOfForesight.Foresight talisman = buff( TalismanOfForesight.Foresight.class );
		boolean cursed = talisman != null && talisman.isCursed();

		int[] rounding = ShadowCaster.rounding[distance];

		int left, right;
		int curr;
		for (int y = Math.max(0, c.y - distance); y <= Math.min(Dungeon.level.height()-1, c.y + distance); y++) {
			if (!circular){
				left = c.x - distance;
			} else if (rounding[Math.abs(c.y - y)] < Math.abs(c.y - y)) {
				left = c.x - rounding[Math.abs(c.y - y)];
			} else {
				left = distance;
				while (rounding[left] < rounding[Math.abs(c.y - y)]){
					left--;
				}
				left = c.x - left;
			}
			right = Math.min(Dungeon.level.width()-1, c.x + c.x - left);
			left = Math.max(0, left);
			for (curr = left + y * Dungeon.level.width(); curr <= right + y * Dungeon.level.width(); curr++){

				if ((foresight || fieldOfView[curr]) && curr != pos) {

					if ((foresight && (!Dungeon.level.mapped[curr] || foresightScan))){
						GameScene.effectOverFog(new CheckedCell(curr, foresightScan ? pos : curr));
					} else if (intentional) {
						GameScene.effectOverFog(new CheckedCell(curr, pos));
					}

					if (foresight){
						Dungeon.level.mapped[curr] = true;
					}
					
					if (Dungeon.level.secret[curr]){
						
						Trap trap = Dungeon.level.traps.get( curr );
						float chance;

						//searches aided by foresight always succeed, even if trap isn't searchable
						if (foresight){
							chance = 1f;

						//otherwise if the trap isn't searchable, searching always fails
						} else if (trap != null && !trap.canBeSearched){
							chance = 0f;

						//intentional searches always succeed against regular traps and doors
						} else if (intentional){
							chance = 1f;
						
						//unintentional searches always fail with a cursed talisman
						} else if (cursed) {
							chance = 0f;
							
						//unintentional trap detection scales from 40% at floor 0 to 30% at floor 25
						} else if (Dungeon.level.map[curr] == Terrain.SECRET_TRAP) {
							chance = 0.4f - (Dungeon.depth / 250f);
							
						//unintentional door detection scales from 20% at floor 0 to 0% at floor 20
						} else {
							chance = 0.2f - (Dungeon.depth / 100f);
						}

						//don't want to let the player search though hidden doors in tutorial
						if (WPDSettings.intro()){
							chance = 0;
						}
						
						if (Random.Float() < chance) {
						
							int oldValue = Dungeon.level.map[curr];
							
							GameScene.discoverTile( curr, oldValue );
							
							Dungeon.level.discover( curr );
							
							ScrollOfMagicMapping.discover( curr );
							
							if (fieldOfView[curr]) smthFound = true;
	
							if (talisman != null){
								if (oldValue == Terrain.SECRET_TRAP){
									talisman.charge(2);
								} else if (oldValue == Terrain.SECRET_DOOR){
									talisman.charge(10);
								}
							}
						}
					}
				}
			}
		}
		
		if (intentional) {
			sprite.showStatus( CharSprite.DEFAULT, Messages.get(this, "search") );
			sprite.operate( pos );
			if (!Dungeon.level.locked) {
				if (cursed) {
					GLog.n(Messages.get(this, "search_distracted"));
					Buff.affect(this, Hunger.class).affectHunger(TIME_TO_SEARCH - (2 * HUNGER_FOR_SEARCH));
				} else {
					Buff.affect(this, Hunger.class).affectHunger(TIME_TO_SEARCH - HUNGER_FOR_SEARCH);
				}
			}
			spendAndNext(TIME_TO_SEARCH);
			
		}
		
		if (smthFound) {
			GLog.w( Messages.get(this, "noticed_smth") );
			Sample.INSTANCE.play( Assets.Sounds.SECRET );
			interrupt();
		}

		if (foresight){
			GameScene.updateFog(pos, Foresight.DISTANCE+1);
		}

		if (talisman != null){
			talisman.checkAwareness();
		}
		
		return smthFound;
	}
	
	public void resurrect() {
		HP = HT;
		live();

		MagicalHolster holster = belongings.getItem(MagicalHolster.class);

		Buff.affect(this, LostInventory.class);
		Buff.affect(this, Invisibility.class, 3f);
		//lost inventory is dropped in interlevelscene

		//activate items that persist after lost inventory
		//FIXME this is very messy, maybe it would be better to just have one buff that
		// handled all items that recharge over time?
		for (Item i : belongings){
			if (i instanceof EquipableItem && i.isEquipped(this)){
				((EquipableItem) i).activate(this);
			} else if (i instanceof CloakOfShadows && i.keptThroughLostInventory() && hasTalent(Talent.LIGHT_CLOAK)) {
				((CloakOfShadows) i).activate(this);
			} else if (i instanceof HolyTome  && i.keptThroughLostInventory() && hasTalent(Talent.LIGHT_READING)) {
				((HolyTome) i).activate(this);
			} else if (i instanceof Wand && i.keptThroughLostInventory()){
				if (holster != null && holster.contains(i)){
					((Wand) i).charge(this, MagicalHolster.HOLSTER_SCALE_FACTOR);
				} else {
					((Wand) i).charge(this);
				}
			} else if (i instanceof MagesStaff && i.keptThroughLostInventory()){
				((MagesStaff) i).applyWandChargeBuff(this);
			}
		}

		updateHT(false);
	}

	@Override
	public void next() {
		if (isAlive())
			super.next();
	}

	public static interface Doom {
		public void onDeath();
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Adrenaline;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AllyBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AuroraBless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BloodMoonBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ClarityBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ChampionEnemy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Corruption;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Dewcharge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Dread;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Glowing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.GreaterHaste;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MonkEnergy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Preparation;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.RainbowBlessing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoulMark;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Speed;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SwarmIntelTracker;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.ArmorAbility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.Feint;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.ClericSpell;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.GuidingLight;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.Stasis;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Lichen;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LivingPlant;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.RotLasher;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.DirectableAlly;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownGuardFolk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town.TownLedger;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.Surprise;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.RedDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.VioletDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.YellowDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.MasterThievesArmband;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TalismanOfForesight;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TimekeepersHourglass;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.ExoticPotion;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfWealth;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ExoticScroll;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfAggression;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ExoticCrystals;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ShardOfOblivion;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFireblast;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfFrost;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.journal.Bestiary;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.VaultLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.levels.traps.Trap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Swiftthistle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Arrays;

public abstract class Mob extends Char {

	{
		actPriority = MOB_PRIO;
		
		alignment = Alignment.ENEMY;
	}

	public AiState SLEEPING     = new Sleeping();
	public AiState HUNTING		= new Hunting();
	public AiState INVESTIGATING= new Investigating();
	public AiState WANDERING	= new Wandering();
	public AiState FLEEING		= new Fleeing();
	public AiState PASSIVE		= new Passive();
	public AiState state = SLEEPING;

	/** the state a debug spawn starts in. A mob whose whole behaviour is "wakes once,
	 *  then leaves" would delete itself before it could be looked at (HoardSquirrel) */
	public AiState debugSpawnState(){
		return WANDERING;
	}
	
	public Class<? extends CharSprite> spriteClass;
	
	protected int target = -1;
	
	public int defenseSkill = 0;
	
	public int EXP = 1;
	public int maxLvl = Hero.MAX_LEVEL-1;

	//the depth a depth-scaled monster draws its strength from: the floor it stands on, unless it was
	//tuned to another - a world slice's fauna (OverworldFauna): the slices' own depth numbers are not floors
	private int statDepth = -1;
	public int statDepth(){ return statDepth >= 0 ? statDepth : Dungeon.depth; }
	/** Was it tuned to a depth of its own (setStatDepth)? */
	public boolean statTuned(){ return statDepth >= 0; }
	/** Tunes it to a depth: the depth-scaled ones re-roll what they were born with (overrides). */
	public void setStatDepth( int depth ){ statDepth = depth; }
	
	protected Char enemy;
	protected int enemyID = -1; //used for save/restore
	protected boolean enemySeen;
	protected boolean alerted = false;

	public Char getEnemy() {
		return enemy;
	}

	//Sprouted: tracks whether this mob was part of the original level generation
	public boolean originalgen = false;

	protected static final float TIME_TO_WAKE_UP = 1f;

	protected boolean firstAdded = true;
	protected void onAdd(){
		if (firstAdded) {
			//modify health for ascension challenge if applicable, only on first add
			float percent = HP / (float) HT;
			HT = Math.round(HT * AscensionChallenge.statModifier(this));
			HP = Math.round(HT * percent);
			firstAdded = false;
		}
	}

	private static final String STATE	= "state";
	private static final String SEEN	= "seen";
	private static final String TARGET	= "target";
	private static final String MAX_LVL	= "max_lvl";
	private static final String ORIGINAL	= "originalgen";
	private static final String STAT_DEPTH	= "stat_depth";

	private static final String ENEMY_ID	= "enemy_id";

	private static final String SWARM_TIME = "swarm_time";

	//for stealth gameplay
	private static final String USING_STEALTH = "using_stealth";
	private static final String INVEST_TURNS = "invest_turns";
	private static final String WANDER_POSITIONS = "wander_positions";
	private static final String WANDER_POS_IDX = "wander_pos_idx";
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		
		super.storeInBundle( bundle );

		if (state == SLEEPING) {
			bundle.put( STATE, Sleeping.TAG );
		} else if (state == WANDERING) {
			bundle.put( STATE, Wandering.TAG );
		} else if (state == INVESTIGATING) {
			bundle.put( STATE, Investigating.TAG );
		} else if (state == HUNTING) {
			bundle.put( STATE, Hunting.TAG );
		} else if (state == FLEEING) {
			bundle.put( STATE, Fleeing.TAG );
		} else if (state == PASSIVE) {
			bundle.put( STATE, Passive.TAG );
		}
		bundle.put( SEEN, enemySeen );
		bundle.put( TARGET, target );
		bundle.put( MAX_LVL, maxLvl );
		bundle.put( ORIGINAL, originalgen );
		if (statDepth >= 0) bundle.put( STAT_DEPTH, statDepth );

		if (enemy != null) {
			bundle.put(ENEMY_ID, enemy.id() );
		}

		bundle.put(SWARM_TIME, timeSeenAt);

		bundle.put( USING_STEALTH, usingStealthGamePlay );
		if (usingStealthGamePlay){
			bundle.put(INVEST_TURNS, investigatingTurns);
			if (wanderPositions != null) {
				bundle.put(WANDER_POSITIONS, wanderPositions);
				bundle.put(WANDER_POS_IDX, wanderPosIdx);
			}
		}
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		
		super.restoreFromBundle( bundle );

		if (bundle.getBoolean(USING_STEALTH)) {
			activateSteathGameplayBehaviour();

			investigatingTurns = bundle.getInt(INVEST_TURNS);
			if (bundle.contains(WANDER_POSITIONS)) {
				wanderPositions = bundle.getIntArray(WANDER_POSITIONS);
				wanderPosIdx = bundle.getInt(WANDER_POS_IDX);
			}
		}

		String state = bundle.getString( STATE );
		if (state.equals( Sleeping.TAG )) {
			this.state = SLEEPING;
		} else if (state.equals( Wandering.TAG )) {
			this.state = WANDERING;
		} else if (state.equals( Investigating.TAG )) {
			this.state = INVESTIGATING;
		} else if (state.equals( Hunting.TAG )) {
			this.state = HUNTING;
		} else if (state.equals( Fleeing.TAG )) {
			this.state = FLEEING;
		} else if (state.equals( Passive.TAG )) {
			this.state = PASSIVE;
		}

		enemySeen = bundle.getBoolean( SEEN );

		target = bundle.getInt( TARGET );

		maxLvl = bundle.getInt(MAX_LVL);

		originalgen = bundle.getBoolean(ORIGINAL);

		statDepth = bundle.contains( STAT_DEPTH ) ? bundle.getInt( STAT_DEPTH ) : -1;

		if (bundle.contains(ENEMY_ID)) {
			enemyID = bundle.getInt(ENEMY_ID);
		}

		timeSeenAt = bundle.getFloat( SWARM_TIME );

		//no need to actually save this, must be false
		firstAdded = false;
	}

	//mobs need to remember their targets after every actor is added
	public void restoreEnemy(){
		if (enemyID != -1 && enemy == null) enemy = (Char)Actor.findById(enemyID);
	}
	
	public CharSprite sprite() {
		return Reflection.newInstance(spriteClass);
	}
	
	private void checkAmbientEffects() {
		//NPCs are exempt from weather/climate reactions entirely
		if (this instanceof NPC) return;
		if (!isAlive()) return;

		// --- Light: UNDEAD/DEMONIC empowered by darkness, weakened by sunlight ---
		// Use the same quadratic sky-light curve as viewDistanceModifier so the
		// buffs ramp in/out at the same pace that visibility changes.
		float sun  = ClimateManager.sunLight();
		float moon = ClimateManager.moonLight();
		float skyLight = Math.min(1f, sun + moon * 0.5f);

		final float DARK_THRESHOLD   = 0.35f; // below this: darkness grows
		final float BRIGHT_THRESHOLD = 0.6f;  // above this: sun strong enough to weaken

		// darkFactor: 0 at skyLight≥threshold, 1 at full dark — quadratic for natural curve
		float darkFactor = Math.max(0f, 1f - skyLight / DARK_THRESHOLD);
		darkFactor = darkFactor * darkFactor;

		// sunFactor: 0 at sun≤0.6, 1 at full sun — linear, penalty only in bright daylight
		float sunFactor = Math.max(0f, (sun - BRIGHT_THRESHOLD) / (1f - BRIGHT_THRESHOLD));

		boolean darkSensitive = properties().contains(Property.UNDEAD)
				|| properties().contains(Property.DEMONIC);

		if (darkSensitive) {
			if (darkFactor > 0f) {
				// Darkness empowers: prolong Speed proportional to how dark it is.
				// darkFactor*3f > 1f when darkFactor > 0.33 → buff maintained, else decays.
				Buff.prolong(this, Speed.class, darkFactor * 3f);
			} else if (sunFactor > 0f) {
				// Strong sunlight weakens: Slow scales with solar intensity.
				Buff.prolong(this, Slow.class, sunFactor * 3f);
			}
		}

		// --- Precipitation: organic mobs get Drenched in heavy rain ---
		if (ClimateManager.localPrecipRate() > 0.3f
				&& ClimateManager.isRaining()
				&& !properties().contains(Property.INORGANIC)
				&& !properties().contains(Property.FIERY)) {
			Buff.prolong(this, Drenched.class, Drenched.DURATION);
		}
	}

	//how a body handles the weather. The dungeon's temperature reaches every creature,
	//but a snow wolf should not freeze in its own snow and a bare skeleton should not
	//feel the snow at all: each nature names the band its owner lives in, and what the
	//far side of that band costs. -20C is the game's extreme cold
	public enum Thermal {
		//              freezes below   overheats above
		NORMAL       (          -5f,             35f ),
		COLD_DWELLER (         -35f,             25f ),  //fur, blubber, ice: the thaw is the danger
		HEAT_DWELLER (           5f,             60f ),  //forge and volcano: the chill is
		INSENSATE    (         -45f,             80f );  //bone, metal, stone: nothing to disturb

		public final float coldPoint, heatPoint;

		Thermal( float coldPoint, float heatPoint ){
			this.coldPoint = coldPoint;
			this.heatPoint = heatPoint;
		}

		private static final Class[] FIRE = { Burning.class, Blazing.class,
				WandOfFireblast.class, Elemental.FireElemental.class };
		private static final Class[] FROST = { Frost.class, Chill.class,
				WandOfFrost.class, Elemental.FrostElemental.class };

		//the element it lives in, shrugged off when it comes as a weapon
		public HashSet<Class> resistances(){
			switch (this){
				case COLD_DWELLER: return new HashSet<Class>( Arrays.asList( FROST ) );
				case HEAT_DWELLER: return new HashSet<Class>( Arrays.asList( FIRE ) );
				default:           return new HashSet<>();
			}
		}

		//and the element on the far side of its band, which lands the harder for it
		public HashSet<Class> vulnerabilities(){
			switch (this){
				case COLD_DWELLER: return new HashSet<Class>( Arrays.asList( FIRE ) );
				case HEAT_DWELLER: return new HashSet<Class>( Arrays.asList( FROST ) );
				default:           return new HashSet<>();
			}
		}
	}

	//set in the instance block of the mobs that are not ordinary flesh, the way
	//properties are; never changes at runtime, so it needs no bundling
	public Thermal thermal = Thermal.NORMAL;

	//a creature shrugs off the element it lives in, and suffers more from the one on
	//the far side of its band
	@Override
	public float resist( Class effect ){
		float result = super.resist( effect );
		for (Class c : thermal.resistances()){
			if (c.isAssignableFrom( effect )) result *= 0.5f;
		}
		for (Class c : thermal.vulnerabilities()){
			if (c.isAssignableFrom( effect )) result *= 1.5f;
		}
		return result;
	}

	private void checkTileTemperatureEffects() {
		if (!isAlive() || Dungeon.level == null) return;
		//NPCs (shopkeeper, portals, quest givers) never freeze or overheat -
		//climate damage reads as an attack and makes them flee/despawn
		if (this instanceof NPC) return;
		float feelsLike = TileTemperature.feelsLikeAt(pos, this);

		// Converge body temperature — rate scales with difference magnitude
		// so intense fire/ice tiles impact mobs quickly even if short-lived
		if (Float.isNaN(bodyTemp)) {
			bodyTemp = feelsLike;
		} else {
			float diff = feelsLike - bodyTemp;
			float baseRate = diff > 0 ? 0.8f : 0.4f;
			float rate = baseRate + Math.max(0f, Math.abs(diff) - 20f) * 0.20f;
			bodyTemp += Math.signum(diff) * Math.min(Math.abs(diff), rate);
		}

		if (bodyTemp < thermal.coldPoint
				&& !properties().contains(Property.FIERY)
				&& !properties().contains(Property.ICY)
				&& buff(Hypothermia.class) == null) {
			Buff.affect(this, Hypothermia.class);
		}

		if (bodyTemp > thermal.heatPoint
				&& !properties().contains(Property.FIERY)
				&& buff(Heatstroke.class) == null) {
			Buff.affect(this, Heatstroke.class);
		}

		checkSpecialTempEffects();
	}

	/**
	 * Mob-type-specific temperature reactions beyond generic Hypothermia/Heatstroke.
	 */
	private void checkSpecialTempEffects() {

		// --- FIERY mobs: devastated by cold and rain ---
		// Fire elementals etc. are weakened when chilled; being drenched is nearly lethal.
		if (properties().contains(Property.FIERY)) {
			boolean drenched = buff(Drenched.class) != null;
			if (bodyTemp < 5f || drenched) {
				// Cold environment drains a fire creature's essence
				float weakDur = drenched ? Weakness.DURATION * 2f : Weakness.DURATION;
				Buff.prolong(this, Weakness.class, weakDur);
				if (bodyTemp < 0f || drenched) {
					Buff.prolong(this, Chill.class, drenched ? 6f : 3f);
				}
			}
		}

		// --- ICY mobs: weakened by warmth, slowed by heat ---
		// Frost elementals struggle when their environment is warm.
		if (properties().contains(Property.ICY)) {
			if (bodyTemp > 20f) {
				Buff.prolong(this, Weakness.class, Weakness.DURATION);
				if (bodyTemp > 35f) {
					Buff.prolong(this, Slow.class, Slow.DURATION);
				}
			}
		}

		// --- INORGANIC constructs: extreme temps seize metal joints ---
		// Robots and golems suffer in temperature extremes (expansion/contraction).
		if (properties().contains(Property.INORGANIC)
				&& !properties().contains(Property.FIERY)
				&& !properties().contains(Property.ICY)
				&& !properties().contains(Property.STATIC)) {
			if (bodyTemp < -15f || bodyTemp > 50f) {
				Buff.prolong(this, Slow.class, Slow.DURATION);
			}
		}

		// --- UNDEAD: cold environments empower them, extreme heat weakens ---
		if (properties().contains(Property.UNDEAD)
				&& !properties().contains(Property.INORGANIC)) {
			if (bodyTemp > 40f) {
				// Undead flesh withers in extreme heat
				Buff.prolong(this, Weakness.class, Weakness.DURATION);
			}
		}

		// --- Plant-type mobs: thrive in rain, wither in heat ---
		if (this instanceof LivingPlant || this instanceof Lichen || this instanceof RotLasher) {
			if (ClimateManager.isRaining() && ClimateManager.localPrecipRate() > 0.3f) {
				Buff.prolong(this, Haste.class, 3f);
			}
			if (bodyTemp > 38f) {
				Buff.prolong(this, Weakness.class, Weakness.DURATION);
			}
		}
	}

	@Override
	protected boolean act() {

		super.act();
		checkAmbientEffects();
		checkTileTemperatureEffects();

		boolean justAlerted = alerted;
		alerted = false;
		
		if (justAlerted){
			sprite.showAlert();
		} else {
			sprite.hideAlert();
			sprite.hideLost();
			sprite.hideInvestigate();
		}

		// Glowing chars draw every mob's attention
		for (Char ch : Actor.chars()) {
			if (ch != this && ch.buff(Glowing.class) != null) {
				enemy = ch;
				return state.act(true, justAlerted);
			}
		}

		if (paralysed > 0) {
			enemySeen = false;
			processSwarmIntel(false);
			spend( TICK );
			return true;
		}

		if (buff(Terror.class) != null || buff(Dread.class) != null ){
			state = FLEEING;
		}
		
		enemy = chooseEnemy();
		
		boolean enemyInFOV = enemy != null && enemy.isAlive() && fieldOfView[enemy.pos] && enemy.invisible <= 0;

		//prevents action, but still updates enemy seen status
		if (buff(Feint.AfterImage.FeintConfusion.class) != null){
			enemySeen = enemyInFOV;
			spend( TICK );
			return true;
		}

		AiState curState = state;
		boolean result = state.act( enemyInFOV, justAlerted );

		//if we just swapped into hunting, this gets processed again
		processSwarmIntel(enemyInFOV && state == curState);

		//for updating hero FOV
		if (buff(PowerOfMany.PowerBuff.class) != null){
			Dungeon.level.updateFieldOfView( this, fieldOfView );
			GameScene.updateFog(pos, viewDistance+(int)Math.ceil(speed()));
		}

		return result;
	}

	private float timeSeenAt = Float.MAX_VALUE;

	protected void processSwarmIntel( boolean enemyInFOV ){
		if (alignment == Alignment.ENEMY && state == HUNTING
				&& Dungeon.isChallenged(Challenges.SWARM_INTELLIGENCE)
				&& enemyInFOV && enemy != null && enemy.alignment == Alignment.ALLY) {

			if (timeSeenAt >= now()){
				timeSeenAt = now()-1; //starts at 2
			}

			int range = swarmAlertRange();
			for (Mob mob : Dungeon.level.mobs) {
				if (mob.alignment == Alignment.ENEMY
						&& mob.paralysed <= 0
						&& Dungeon.level.distance(pos, mob.pos) <= range
						&& mob.state != mob.HUNTING) {
					mob.beckon(	enemy.pos);
				}
			}
			Buff.affect( Dungeon.hero, SwarmIntelTracker.class );
		} else {
			timeSeenAt = Float.MAX_VALUE;
		}
	}

	public int swarmAlertRange(){
		int range = 2*(int)Math.max(now() - timeSeenAt, 0);
		return (int)GameMath.gate(0, range, 12);
	}

	@Override
	public void fixTime(float decrement) {
		if (swarmAlertRange() > 0){
			timeSeenAt -= decrement;
		}
		super.fixTime(decrement);
	}

	//FIXME this is sort of a band-aid correction for allies needing more intelligent behaviour
	protected boolean intelligentAlly = false;
	
	protected Char chooseEnemy() {

		Dread dread = buff( Dread.class );
		if (dread != null) {
			Char source = (Char)Actor.findById( dread.object );
			if (source != null) {
				return source;
			}
		}

		Terror terror = buff( Terror.class );
		if (terror != null) {
			Char source = (Char)Actor.findById( terror.object );
			if (source != null) {
				return source;
			}
		}
		
		//if we are an alert enemy, auto-hunt a target that is affected by aggression, even another enemy
		if ((alignment == Alignment.ENEMY || buff(Amok.class) != null ) && state != PASSIVE && state != SLEEPING) {
			if (enemy != null && enemy.buff(StoneOfAggression.Aggression.class) != null){
				state = HUNTING;
				return enemy;
			}
			for (Char ch : Actor.chars()) {
				if (ch != this && fieldOfView[ch.pos] &&
						ch.buff(StoneOfAggression.Aggression.class) != null) {
					state = HUNTING;
					return ch;
				}
			}
		}

		//find a new enemy if..
		boolean newEnemy = false;
		//we have no enemy, or the current one is dead/missing
		if ( enemy == null || !enemy.isAlive() || !Actor.chars().contains(enemy) || state == WANDERING) {
			newEnemy = true;
		//We are amoked and current enemy is the hero
		} else if (buff( Amok.class ) != null && enemy == Dungeon.hero) {
			newEnemy = true;
		//We are charmed and current enemy is what charmed us
		} else if (buff(Charm.class) != null && buff(Charm.class).object == enemy.id()) {
			newEnemy = true;
		}

		//additionally, if we are an ally, find a new enemy if...
		if (!newEnemy && alignment == Alignment.ALLY){
			//current enemy is also an ally
			if (enemy.alignment == Alignment.ALLY){
				newEnemy = true;
			//current enemy is invulnerable
			} else if (enemy.isInvulnerable(getClass())){
				newEnemy = true;
			}
		}

		if ( newEnemy ) {

			HashSet<Char> enemies = new HashSet<>();

			//if we are amoked...
			if ( buff(Amok.class) != null) {
				//try to find an enemy mob to attack first.
				for (Mob mob : Dungeon.level.mobs)
					if (mob.alignment == Alignment.ENEMY && mob != this
							&& fieldOfView[mob.pos] && mob.invisible <= 0) {
						enemies.add(mob);
					}
				
				if (enemies.isEmpty()) {
					//try to find ally mobs to attack second.
					for (Mob mob : Dungeon.level.mobs)
						if (mob.alignment == Alignment.ALLY && mob != this
								&& fieldOfView[mob.pos] && mob.invisible <= 0) {
							enemies.add(mob);
						}
					
					if (enemies.isEmpty()) {
						//try to find the hero third
						if (fieldOfView[Dungeon.hero.pos] && Dungeon.hero.invisible <= 0) {
							enemies.add(Dungeon.hero);
						}
					}
				}
				
			//if we are an ally...
			} else if ( alignment == Alignment.ALLY ) {
				//look for hostile mobs to attack
				for (Mob mob : Dungeon.level.mobs)
					if (mob.alignment == Alignment.ENEMY && fieldOfView[mob.pos]
							&& mob.invisible <= 0 && !mob.isInvulnerable(getClass()))
						//do not target passive mobs
						//intelligent allies also don't target mobs which are wandering or asleep
						if (mob.state != mob.PASSIVE &&
								(!intelligentAlly || (mob.state != mob.SLEEPING && mob.state != mob.WANDERING))) {
							enemies.add(mob);
						}
				
			//if we are an enemy...
			} else if (alignment == Alignment.ENEMY) {
				//look for ally mobs to attack
				for (Mob mob : Dungeon.level.mobs)
					if (mob.alignment == Alignment.ALLY && fieldOfView[mob.pos] && mob.invisible <= 0)
						enemies.add(mob);

				//and look for the hero
				if (fieldOfView[Dungeon.hero.pos] && Dungeon.hero.invisible <= 0) {
					enemies.add(Dungeon.hero);
				}
				
			}

			//do not target anything that's charming us
			Charm charm = buff( Charm.class );
			if (charm != null){
				Char source = (Char)Actor.findById( charm.object );
				if (source != null && enemies.contains(source) && enemies.size() > 1){
					enemies.remove(source);
				}
			}

			//neutral characters in particular do not choose enemies.
			if (enemies.isEmpty()){
				return null;
			} else {
				//go after the closest potential enemy, preferring enemies that can be reached/attacked, and the hero if two are equidistant
				PathFinder.buildDistanceMap(pos, Dungeon.findPassable(this, Dungeon.level.passable, fieldOfView, true));
				Char closest = null;
				int closestDist = Integer.MAX_VALUE;

				for (Char curr : enemies){
					int currDist = Integer.MAX_VALUE;
					//we aren't trying to move into the target, just toward them
					for (int i : PathFinder.NEIGHBOURS8){
						if (PathFinder.distance[curr.pos+i] < currDist){
							currDist = PathFinder.distance[curr.pos+i];
						}
					}
					if (closest == null){
						closest = curr;
						closestDist = currDist;
					} else if (canAttack(closest) && !canAttack(curr)){
						continue;
					} else if ((canAttack(curr) && !canAttack(closest))
							|| (currDist < closestDist)){
						closest = curr;
					} else if ( curr == Dungeon.hero &&
							(currDist == closestDist) || (canAttack(curr) && canAttack(closest))){
						closest = curr;
					}
				}
				//if we were going to target the hero, but an afterimage exists, target that instead
				if (closest == Dungeon.hero){
					for (Char ch : enemies){
						if (ch instanceof Feint.AfterImage){
							closest = ch;
							break;
						}
					}
				}

				return closest;
			}

		} else
			return enemy;
	}
	
	@Override
	public boolean add( Buff buff ) {
		if (super.add( buff )) {
			if (buff instanceof Amok || buff instanceof AllyBuff) {
				state = HUNTING;
			} else if (buff instanceof Terror || buff instanceof Dread) {
				state = FLEEING;
			} else if (buff instanceof Sleep) {
				state = SLEEPING;
				postpone(Sleep.SWS);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean remove( Buff buff ) {
		if (super.remove( buff )) {
			if (state == FLEEING && ((buff instanceof Terror && buff(Dread.class) == null)
					|| (buff instanceof Dread && buff(Terror.class) == null))) {
				if (enemySeen) {
					sprite.showStatus(CharSprite.WARNING, Messages.get(this, "rage"));
					state = HUNTING;
				} else {
					state = WANDERING;
				}
			}
			return true;
		}
		return false;
	}
	
	protected boolean canAttack( Char enemy ) {
		if (Dungeon.level.adjacent( pos, enemy.pos )){
			return true;
		}
		for (ChampionEnemy buff : buffs(ChampionEnemy.class)){
			if (buff.canAttackWithExtraReach( enemy )){
				return true;
			}
		}
		return false;
	}

	private boolean cellIsPathable( int cell ){
		if (!Dungeon.level.passable[cell]){
			if (flying || buff(Amok.class) != null){
				if (!Dungeon.level.avoid[cell]){
					return false;
				}
			} else {
				return false;
			}
		}
		if (Char.hasProp(this, Char.Property.LARGE) && !Dungeon.level.openSpace[cell]){
			return false;
		}
		if (Actor.findChar(cell) != null){
			return false;
		}

		return true;
	}

	protected boolean getCloser( int target ) {
		
		if (rooted || target == pos || !Dungeon.level.insideMap(target)) {
			return false;
		}

		int step = -1;

		if (Dungeon.level.adjacent( pos, target )) {

			path = null;

			if (cellIsPathable(target)) {
				step = target;
			}

		} else {

			boolean newPath = false;
			float longFactor = state == WANDERING ? 2f : 1.33f;
			//scrap the current path if it's empty, no longer connects to the current location
			//or if it's quite inefficient and checking again may result in a much better path
			//mobs are much more tolerant of inefficient paths if wandering
			if (path == null || path.isEmpty()
					|| !Dungeon.level.adjacent(pos, path.getFirst())
					|| path.size() > longFactor*Dungeon.level.distance(pos, target))
				newPath = true;
			else if (path.getLast() != target) {
				//if the new target is adjacent to the end of the path, adjust for that
				//rather than scrapping the whole path.
				if (Dungeon.level.adjacent(target, path.getLast())) {
					int last = path.removeLast();

					if (path.isEmpty()) {

						//shorten for a closer one
						if (Dungeon.level.adjacent(target, pos)) {
							path.add(target);
						//extend the path for a further target
						} else {
							path.add(last);
							path.add(target);
						}

					} else {
						//if the new target is simply 1 earlier in the path shorten the path
						if (path.getLast() == target) {

						//if the new target is closer/same, need to modify end of path
						} else if (Dungeon.level.adjacent(target, path.getLast())) {
							path.add(target);

						//if the new target is further away, need to extend the path
						} else {
							path.add(last);
							path.add(target);
						}
					}

				} else {
					newPath = true;
				}

			}

			//checks if the next cell along the current path can be stepped into
			if (!newPath) {
				int nextCell = path.removeFirst();
				if (!cellIsPathable(nextCell)) {

					newPath = true;
					//If the next cell on the path can't be moved into, see if there is another cell that could replace it
					if (!path.isEmpty()) {
						for (int i : PathFinder.NEIGHBOURS8) {
							if (Dungeon.level.adjacent(pos, nextCell + i) && Dungeon.level.adjacent(nextCell + i, path.getFirst())) {
								if (cellIsPathable(nextCell+i)){
									path.addFirst(nextCell+i);
									newPath = false;
									break;
								}
							}
						}
					}
				} else {
					path.addFirst(nextCell);
				}
			}

			//generate a new path
			if (newPath) {
				//If we aren't hunting, always take a full path
				PathFinder.Path full = Dungeon.findPath(this, target, Dungeon.level.passable, fieldOfView, true);
				if (state != HUNTING && state != INVESTIGATING){
					path = full;
				} else {
					//otherwise, check if other characters are forcing us to take a very slow route
					// and don't try to go around them yet in response, basically assume their blockage is temporary
					PathFinder.Path ignoreChars = Dungeon.findPath(this, target, Dungeon.level.passable, fieldOfView, false);
					if (ignoreChars != null && (full == null || full.size() > 2*ignoreChars.size())){
						//check if first cell of shorter path is valid. If it is, use new shorter path. Otherwise do nothing and wait.
						path = ignoreChars;
						if (!cellIsPathable(ignoreChars.getFirst())) {
							return false;
						}
					} else {
						path = full;
					}
				}
			}

			if (path != null) {
				step = path.removeFirst();
			} else {
				return false;
			}
		}
		if (step != -1) {
			move( step );
			return true;
		} else {
			return false;
		}
	}
	
	protected boolean getFurther( int target ) {
		if (rooted || target == pos) {
			return false;
		}
		
		int step = Dungeon.flee( this, target, Dungeon.level.passable, fieldOfView, true );
		if (step != -1) {
			move( step );
			return true;
		} else {
			return false;
		}
	}

	CharSprite movementShadow;
	AlphaTweener shadowFade;

	@Override
	public void move(int step, boolean travelling) {
		int from = pos;
		super.move(step, travelling);
		if (usingStealthGamePlay
				&& travelling
				&& sprite != null
				&& !sprite.visible
				&& Dungeon.level.distance(pos, Dungeon.hero.pos) <= 6){
			if (!Dungeon.level.visited[pos] && !Dungeon.level.mapped[pos]){
				//also reveal this cell in the fog of war if it's within a 6 tile path
				PathFinder.buildDistanceMap(Dungeon.hero.pos, Dungeon.level.passable, 6);
				if (PathFinder.distance[pos] != Integer.MAX_VALUE) {
					Dungeon.level.visited[pos] = true;
					GameScene.updateFog(pos, 1);
				}
			}
			//if we don't have a shadow, or it was removed from the scene (e.g. from scene reset)
			if (movementShadow == null || movementShadow.parent == null){
				movementShadow = sprite();
				sprite.parent.add(movementShadow);
			}
			movementShadow.point(DungeonTilemap.raisedTileCenterToWorld(from));
			movementShadow.x -= movementShadow.width()/2f;
			movementShadow.y -= movementShadow.height()/2f;
			movementShadow.move(from, pos);
			movementShadow.alpha(sprite.alpha());
			if (shadowFade == null){
				shadowFade = new AlphaTweener( movementShadow, 0, 1 ) {
					@Override
					protected void updateValues(float progress) {
						if (sprite.visible){
							elapsed = progress = 1;
						}
						progress = (float)Math.pow(progress, 2); //alpha fades slowly then quickly
						super.updateValues(progress);
					}

					@Override
					protected void onComplete() {
						shadowFade = null;
					}
				};
				sprite.parent.add(shadowFade);
			} else {
				shadowFade.elapsed = 0;
			}
		}
	}

	//the dots over a creature's head: the hero's next blow would land as a surprise
	//(surprisedBy), hostile or not, wandering or hunting someone who has lost sight of him.
	//Asleep shows its own zzz; allies are never struck; townsfolk never notice anyone; and a
	//passive one may be a mimic or a statue biding its time, which the dots would give away
	public boolean showsUnaware() {
		return isAlive() && Dungeon.hero != null && Dungeon.hero.isAlive()
				&& alignment != Alignment.ALLY && !(this instanceof NPC)
				&& state != SLEEPING && state != PASSIVE
				&& surprisedBy( Dungeon.hero, false );
	}

	@Override
	public void updateSpriteState() {
		super.updateSpriteState();
		if (Dungeon.hero.buff(TimekeepersHourglass.timeFreeze.class) != null
				|| Dungeon.hero.buff(Swiftthistle.TimeBubble.class) != null)
			sprite.add( CharSprite.State.PARALYSED );
	}
	
	public float attackDelay() {
		float delay = 1f;
		if ( buff(Adrenaline.class) != null) delay /= 1.5f;
		return delay;
	}
	
	protected boolean doAttack( Char enemy ) {
		
		if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
			sprite.attack( enemy.pos );
			return false;
			
		} else {
			attack( enemy );
			Invisibility.dispel(this);
			spend( attackDelay() );
			return true;
		}
	}
	
	@Override
	public void onAttackComplete() {
		attack( enemy );
		Invisibility.dispel(this);
		spend( attackDelay() );
		super.onAttackComplete();
	}
	
	@Override
	public int defenseSkill( Char enemy ) {
		if (buff(GuidingLight.Illuminated.class) != null && Dungeon.hero.heroClass == HeroClass.CLERIC){
			//if the attacker is the cleric, they must be using a weapon they have the str for
			if (enemy instanceof Hero){
				Hero h = (Hero) enemy;
				if (!(h.belongings.attackingWeapon() instanceof Weapon)
						|| ((Weapon) h.belongings.attackingWeapon()).STRReq() <= h.STR()){
					return 0;
				}
			} else {
				return 0;
			}
		}

		if ( !surprisedBy(enemy)
				&& paralysed == 0
				&& !(alignment == Alignment.ALLY && enemy == Dungeon.hero)) {
			return this.defenseSkill;
		} else {
			return 0;
		}
	}
	
	//what the blow being resolved found: defenseProc wakes the mob and turns it on its attacker before
	//the attacker's on-hit hooks run, so skills (Silent Death, Ambush) read the state from here
	private float blowAt = -1f;
	private boolean blowFoundAsleep = false;
	private boolean blowWasSurprise = false;

	/** true during the attack that is hitting this mob if it was SLEEPING when that blow arrived */
	public boolean wasAsleepBeforeBlow(){
		return blowFoundAsleep && blowAt == Actor.now();
	}

	/** true during the attack that is hitting this mob if that blow was a surprise attack */
	public boolean wasSurprisedByBlow(){
		return blowWasSurprise && blowAt == Actor.now();
	}

	@Override
	public int defenseProc( Char enemy, int damage ) {

		blowAt = Actor.now();
		blowFoundAsleep = state == SLEEPING;
		blowWasSurprise = surprisedBy(enemy);
		
		if (enemy instanceof Hero
				&& ((Hero) enemy).belongings.attackingWeapon() instanceof MissileWeapon){
			Statistics.thrownAttacks++;
			Badges.validateHuntressUnlock();
		}
		
		if (surprisedBy(enemy)) {
			Statistics.sneakAttacks++;
			Badges.validateRogueUnlock();
			//TODO this is somewhat messy, it would be nicer to not have to manually handle delays here
			// playing the strong hit sound might work best as another property of weapon?
			if (Dungeon.hero.belongings.attackingWeapon() instanceof SpiritBow.SpiritArrow
				|| Dungeon.hero.belongings.attackingWeapon() instanceof Dart){
				Sample.INSTANCE.playDelayed(Assets.Sounds.HIT_STRONG, 0.125f);
			} else {
				Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
			}
			if (enemy.buff(Preparation.class) != null) {
				Wound.hit(this);
			} else {
				Surprise.hit(this);
			}
		}

		//if attacked by something else than current target, and that thing is closer, switch targets
		//or if attacked by target, simply update target position
		if (state != FLEEING) {
			if (state != HUNTING) {
				aggro(enemy);
				target = enemy.pos;
			} else {
				recentlyAttackedBy.add(enemy);
			}
		}

		if (buff(SoulMark.class) != null) {
			int restoration = Math.min(damage, HP+shielding());
			
			//physical damage that doesn't come from the hero is less effective
			if (enemy != Dungeon.hero){
				restoration = Math.round(restoration * 0.4f*Dungeon.hero.pointsInTalent(Talent.SOUL_SIPHON)/3f);
			}
			if (restoration > 0) {
				Buff.affect(Dungeon.hero, Hunger.class).affectHunger(restoration*Dungeon.hero.pointsInTalent(Talent.SOUL_EATER)/3f);

				if (Dungeon.hero.HP < Dungeon.hero.HT) {
					int heal = (int)Math.ceil(restoration * 0.4f);
					Dungeon.hero.HP = Math.min(Dungeon.hero.HT, Dungeon.hero.HP + heal);
					Dungeon.hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
			}
		}

		return super.defenseProc(enemy, damage);
	}

	@Override
	public float speed() {
		return super.speed() * AscensionChallenge.enemySpeedModifier(this);
	}

	public final boolean surprisedBy( Char enemy ){
		return surprisedBy( enemy, true);
	}

	public boolean surprisedBy( Char enemy, boolean attacking ){
		return enemy == Dungeon.hero
				&& (enemy.invisible > 0 || !enemySeen || (fieldOfView != null && fieldOfView.length == Dungeon.level.length() && !fieldOfView[enemy.pos]))
				&& (!attacking || enemy.canSurpriseAttack());
	}

	//whether the hero should interact with the mob (true) or attack it (false)
	public boolean heroShouldInteract(){
		return alignment != Alignment.ENEMY && buff(Amok.class) == null;
	}

	public void aggro( Char ch ) {
		enemy = ch;
		if (state != PASSIVE){
			state = HUNTING;
		}
	}

	//Sprouted: depth-based stat scaling for mobs
	// type 0: depth, type 1: depth/2, type 2: depth/4, type 3: depth*2
	public int adj(int type){
		switch (type){
			case 0:  return Dungeon.depth;
			case 1:  return Dungeon.depth / 2;
			case 2:  return Dungeon.depth / 4;
			case 3:  return Dungeon.depth * 2;
			default: return 1;
		}
	}

	public void clearEnemy(){
		enemy = null;
		enemySeen = false;
		if (state == HUNTING) state = WANDERING;
	}
	
	public boolean isTargeting( Char ch){
		return enemy == ch;
	}

	@Override
	public void damage( int dmg, Object src ) {

		if (!isInvulnerable(src.getClass())) {
			if (state == SLEEPING) {
				state = WANDERING;
			}
			if (!(src instanceof Corruption) && state != FLEEING) {
				if (state != HUNTING) {
					alerted = true;
					//assume the hero is hitting us in these common cases
					if (src instanceof Wand || src instanceof ClericSpell || src instanceof ArmorAbility) {
						aggro(Dungeon.hero);
						target = Dungeon.hero.pos;
					}
				} else {
					if (src instanceof Wand || src instanceof ClericSpell || src instanceof ArmorAbility) {
						recentlyAttackedBy.add(Dungeon.hero);
					}
				}
			}
		}
		
		super.damage( dmg, src );
	}
	
	
	@Override
	public void destroy() {
		
		super.destroy();
		
		Dungeon.level.mobs.remove( this );

		if (Dungeon.hero.buff(MindVision.class) != null){
			Dungeon.observe();
			GameScene.updateFog(pos, 2);
		}

		if (Dungeon.hero.isAlive()) {
			
			if (alignment == Alignment.ENEMY) {
				Statistics.enemiesSlain++;
				Badges.validateMonstersSlain();
				if (DayNightCycle.isNight()) {
					Statistics.nightKills++;
					Badges.validateNightHunter();
				}
				Statistics.qualifiedForNoKilling = false;
				Bestiary.setSeen(getClass());
				Bestiary.countEncounter(getClass());

				AscensionChallenge.processEnemyKill(this);
				
				//top-tier mobs (maxLvl at the old ceiling) keep feeding full xp forever,
				//enabling infinite leveling. Out-levelled mobs still pay a quarter (at
				//least 1) so every kill counts; a negative maxLvl means never (summons)
				boolean outLevelled = Dungeon.hero.lvl > maxLvl && maxLvl < Hero.MAX_LEVEL - 1;
				int exp = !outLevelled ? EXP
						: (maxLvl < 0 || EXP <= 0) ? 0
						: Math.max( 1, EXP / 4 );
				//a barrow's enemies are never out-levelled: they pay full experience, grown with
				//their power, however far the hero has gone (summons, maxLvl < 0, still pay none)
				if (xyz.gabriwar.warpedpixeldungeon.levels.Delves.inDelve() && maxLvl >= 0 && EXP > 0){
					exp = Math.round( EXP * xyz.gabriwar.warpedpixeldungeon.levels.Delves.power( this ) );
				}

				//during ascent, under-levelled enemies grant 10 xp each until level 30
				// after this enemy kills which reduce the amulet curse still grant 10 effective xp
				// for the purposes of on-exp effects, see AscensionChallenge.processEnemyKill
				if (Dungeon.hero.buff(AscensionChallenge.class) != null &&
						outLevelled && maxLvl > 0 && EXP > 0 && Dungeon.hero.lvl < Hero.MAX_LEVEL){
					exp = Math.round(10 * spawningWeight());
				}

				//elixir of clarity: the next few experience-granting kills grant half again as much
				ClarityBuff clarity = Dungeon.hero.buff(ClarityBuff.class);
				if (clarity != null) exp = clarity.boost(exp);

				if (exp > 0) {
					Dungeon.hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(exp), FloatingText.EXPERIENCE);
				}
				Dungeon.hero.earnExp(exp, getClass());
				//in the vault level we manually progress ring IDing. 5 enemies defeated to an ID
				if (exp == 0 && Dungeon.level instanceof VaultLevel){
					Item ring = Dungeon.hero.belongings.ring();
					if (ring != null){
						ring.onHeroGainExp(0.2f, Dungeon.hero);
					}
					Item misc = Dungeon.hero.belongings.misc();
					if (misc instanceof Ring){
						misc.onHeroGainExp(0.2f, Dungeon.hero);
					}
				}

				if (Dungeon.hero.subClass == HeroSubClass.MONK){
					Buff.affect(Dungeon.hero, MonkEnergy.class).gainEnergy(this);
				}
			}
		}
	}
	
	@Override
	public void die( Object cause ) {

		//skill tree: the hero's kills feed Fletching and Hunting
		//kills by the hero's own blows and by any hero skill damage source count; see CurrentSkills.creditKill
		if (Dungeon.hero != null && Dungeon.hero.heroSkills != null
				&& alignment == Alignment.ENEMY && !(this instanceof xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC)){
			Dungeon.hero.heroSkills.creditKill( this, cause );
			Dungeon.hero.heroSkills.onEnemyDeath( this, cause );
		}
		if (Dungeon.hero != null && Dungeon.level != null) xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality.onEnemyKilled( this, cause );
		//a barrow's guardian pays out its hoard and seals the barrow
		if (Dungeon.level != null) xyz.gabriwar.warpedpixeldungeon.levels.Delves.onDeath( this );

		if (cause == Chasm.class){
			//50% chance to round up, 50% to round down
			if (EXP % 2 == 1) EXP += Random.Int(2);
			EXP /= 2;
		}

		if (alignment == Alignment.ENEMY){
			if (buff(Trap.HazardAssistTracker.class) != null){
				Statistics.hazardAssistedKills++;
				Badges.validateHazardAssists();
			}

			rollToDropLoot();

			if (cause == Dungeon.hero || cause instanceof Weapon || cause instanceof Weapon.Enchantment){
				//the town guard's bounty: the posted creature, brought down by the hero's hand
				if (TownLedger.bountyTarget == getClass()){
					Dungeon.level.drop( new Gold( TownLedger.bountyReward ), pos ).sprite.drop();
					GLog.p( Messages.get( TownGuardFolk.class, "bounty_paid", TownLedger.bountyReward ) );
					TownLedger.bountyTarget = null;
					TownLedger.used( TownLedger.BOUNTY );
					Badges.validateBountyHunter();
				}
				if (Dungeon.hero.hasTalent(Talent.LETHAL_MOMENTUM)
						&& Random.Float() < 0.34f + 0.33f* Dungeon.hero.pointsInTalent(Talent.LETHAL_MOMENTUM)){
					Buff.affect(Dungeon.hero, Talent.LethalMomentumTracker.class, 0f);
					//at +3 the momentum carries: two turns of adrenaline after the kill
					if (Dungeon.hero.pointsInTalent(Talent.LETHAL_MOMENTUM) >= 3){
						Buff.prolong(Dungeon.hero, xyz.gabriwar.warpedpixeldungeon.actors.buffs.Adrenaline.class, 2f);
					}
				}
				if (Dungeon.hero.heroClass != HeroClass.DUELIST
						&& Dungeon.hero.hasTalent(Talent.LETHAL_HASTE)
						&& Dungeon.hero.buff(Talent.LethalHasteCooldown.class) == null){
					Buff.affect(Dungeon.hero, Talent.LethalHasteCooldown.class, 100f);
					Buff.affect(Dungeon.hero, GreaterHaste.class).set(2 + 2*Dungeon.hero.pointsInTalent(Talent.LETHAL_HASTE));
				}
			}

		}

		if (Dungeon.hero.isAlive() && !Dungeon.level.heroFOV[pos]) {
			GLog.i( Messages.get(this, "died") );
		}

		boolean soulMarked = buff(SoulMark.class) != null;

		super.die( cause );

		if (!(this instanceof Wraith)
				&& soulMarked
				&& Random.Float() < (0.4f*Dungeon.hero.pointsInTalent(Talent.NECROMANCERS_MINIONS)/3f)){
			Wraith w = Wraith.spawnAt(pos, Wraith.class);
			if (w != null) {
				Buff.affect(w, Corruption.class);
				if (Dungeon.level.heroFOV[pos]) {
					CellEmitter.get(pos).burst(ShadowParticle.CURSE, 6);
					Sample.INSTANCE.play(Assets.Sounds.CURSED);
				}
			}
		}
	}

	public float lootChance(){
		float lootChance = this.lootChance;

		float dropBonus = RingOfWealth.dropChanceMultiplier( Dungeon.hero );

		Talent.BountyHunterTracker bhTracker = Dungeon.hero.buff(Talent.BountyHunterTracker.class);
		if (bhTracker != null){
			Preparation prep = Dungeon.hero.buff(Preparation.class);
			if (prep != null){
				// 2/4/8/16% per prep level, multiplied by talent points
				float bhBonus = 0.02f * (float)Math.pow(2, prep.attackLevel()-1);
				bhBonus *= Dungeon.hero.pointsInTalent(Talent.BOUNTY_HUNTER);
				dropBonus += bhBonus;
			}
		}

		dropBonus += ShardOfOblivion.lootChanceMultiplier()-1f;

		// Season and weekday loot modifiers
		dropBonus *= GameCalendar.seasonLootMultiplier();
		dropBonus *= GameCalendar.weekdayLootMultiplier();

		// Ambient weather luck buffs
		AuroraBless aurora = Dungeon.hero.buff(AuroraBless.class);
		if (aurora != null) dropBonus *= aurora.dropChanceMultiplier();
		RainbowBlessing rainbow = Dungeon.hero.buff(RainbowBlessing.class);
		if (rainbow != null) dropBonus *= rainbow.dropChanceMultiplier();
		BloodMoonBuff bloodMoon = Dungeon.hero.buff(BloodMoonBuff.class);
		if (bloodMoon != null) dropBonus *= bloodMoon.dropChanceMultiplier();

		return lootChance * dropBonus;
	}
	
	public void rollToDropLoot(){
		//bosses always drop their loot - several postgame bosses have low
		//maxLvl values and were dropping nothing for high-level heroes
		if (Dungeon.hero.lvl > maxLvl + 2 && !properties().contains(Property.BOSS)
				&& !lootIgnoresLevel()
				//nor is a barrow's loot ever out-levelled
				&& !xyz.gabriwar.warpedpixeldungeon.levels.Delves.inDelve()) {
			if (extraLootIgnoresLevel()) dropExtraLoot();
			return;
		}

		MasterThievesArmband.StolenTracker stolen = buff(MasterThievesArmband.StolenTracker.class);
		if (stolen == null || !stolen.itemWasStolen()) {
			if (Random.Float() < lootChance()) {
				Item loot = createLoot();
				if (loot != null) {
					Dungeon.level.drop(loot, pos).sprite.drop();
					Bestiary.setLootSlotSeen(getClass(), 0);
				}
			}
		}

		//Sprouted: second loot slot
		if (lootOther != null && Random.Float() < lootChanceOther) {
			Item secondLoot = createLootOther();
			if (secondLoot != null) {
				Dungeon.level.drop(secondLoot, pos).sprite.drop();
				Bestiary.setLootSlotSeen(getClass(), 1);
			}
		}

		//Sprouted: third loot slot
		if (lootThird != null && Random.Float() < lootChanceThird) {
			Item thirdLoot = createLootThird();
			if (thirdLoot != null) {
				Dungeon.level.drop(thirdLoot, pos).sprite.drop();
				Bestiary.setLootSlotSeen(getClass(), 2);
			}
		}

		//Sprouted: extra loot slots (4th+) declared via declareExtraLoot/dropExtraLoot
		dropExtraLoot();

		//ring of wealth logic
		if (Ring.getBuffedBonus(Dungeon.hero, RingOfWealth.Wealth.class) > 0) {
			int rolls = 1;
			if (properties.contains(Property.BOSS)) rolls = 15;
			else if (properties.contains(Property.MINIBOSS)) rolls = 5;
			ArrayList<Item> bonus = RingOfWealth.tryForBonusDrop(Dungeon.hero, rolls);
			if (bonus != null && !bonus.isEmpty()) {
				for (Item b : bonus) Dungeon.level.drop(b, pos).sprite.drop();
				RingOfWealth.showFlareForBonusDrop(sprite);
			}
		}
		
		//lucky enchant logic
		if (buff(Lucky.LuckProc.class) != null){
			Dungeon.level.drop(buff(Lucky.LuckProc.class).genLoot(), pos).sprite.drop();
			Lucky.showFlare(sprite);
		}

		//soul eater talent
		if (buff(SoulMark.class) != null &&
				Random.Int(10) < Dungeon.hero.pointsInTalent(Talent.SOUL_EATER)){
			Talent.onFoodEaten(Dungeon.hero, 0, null);
		}

	}
	
	protected Object loot = null;
	protected float lootChance = 0;

	//Sprouted: second and third loot slots
	protected Object lootOther = null;
	protected float lootChanceOther = 0;
	protected Object lootThird = null;
	protected float lootChanceThird = 0;

	//Sprouted: dynamic extra loot slots (4th+), declared by subclasses for Bestiary display
	private ArrayList<Object> extraLootSlots = null;
	private ArrayList<Float> extraLootChanceSlots = null;

	// Call in subclass {} initializer to register extra drop slots for Bestiary display.
	// loot can be an Item instance, Class<? extends Item>, or Generator.Category.
	protected final void declareExtraLoot(Object loot, float chance) {
		if (extraLootSlots == null) {
			extraLootSlots = new ArrayList<>();
			extraLootChanceSlots = new ArrayList<>();
		}
		extraLootSlots.add(loot);
		extraLootChanceSlots.add(chance);
	}

	// Override to implement conditional drop logic. Call trackedDrop() inside.
	// Called from rollToDropLoot() for ENEMY-aligned mobs.
	protected void dropExtraLoot() {}

	//Opt in for progression rewards originally dropped directly from die().
	//Normal loot rolls keep their level cap and existing chances.
	protected boolean extraLootIgnoresLevel() { return false; }

	//Sprouted enemies whose regular loot includes a guaranteed reward.
	protected boolean lootIgnoresLevel() { return false; }

	// Drop an item at pos and mark the corresponding Bestiary slot as seen.
	// extraSlotIndex 0 = slot 3, 1 = slot 4, etc.
	protected final void trackedDrop(Item item, int extraSlotIndex) {
		Dungeon.level.drop(item, pos).sprite.drop();
		Bestiary.setLootSlotSeen(getClass(), 3 + extraSlotIndex);
	}

	// Drop an item at a custom cell and mark the Bestiary slot as seen.
	protected final void trackedDrop(Item item, int cell, int extraSlotIndex) {
		Dungeon.level.drop(item, cell).sprite.drop(pos);
		Bestiary.setLootSlotSeen(getClass(), 3 + extraSlotIndex);
	}
	
	@SuppressWarnings("unchecked")
	public Item createLoot() {
		Item item;
		if (loot instanceof Generator.Category) {

			item = Generator.randomUsingDefaults( (Generator.Category)loot );

		} else if (loot instanceof Class<?>) {

			if (ExoticPotion.regToExo.containsKey(loot)){
				if (Random.Float() < ExoticCrystals.consumableExoticChance()){
					return Generator.random(ExoticPotion.regToExo.get(loot));
				}
			} else if (ExoticScroll.regToExo.containsKey(loot)){
				if (Random.Float() < ExoticCrystals.consumableExoticChance()){
					return Generator.random(ExoticScroll.regToExo.get(loot));
				}
			}

			item = Generator.random( (Class<? extends Item>)loot );

		} else {

			item = (Item)loot;

		}
		return item;
	}

	//Sprouted: second loot slot
	@SuppressWarnings("unchecked")
	protected Item createLootOther() {
		Item item;
		if (lootOther instanceof Generator.Category) {
			item = Generator.randomUsingDefaults( (Generator.Category)lootOther );
		} else if (lootOther instanceof Class<?>) {
			item = Generator.random( (Class<? extends Item>)lootOther );
		} else {
			item = (Item)lootOther;
		}
		return item;
	}

	//Sprouted: third loot slot
	@SuppressWarnings("unchecked")
	protected Item createLootThird() {
		Item item;
		if (lootThird instanceof Generator.Category) {
			item = Generator.randomUsingDefaults( (Generator.Category)lootThird );
		} else if (lootThird instanceof Class<?>) {
			item = Generator.random( (Class<? extends Item>)lootThird );
		} else {
			item = (Item)lootThird;
		}
		return item;
	}

	//how many mobs this one should count as when determining spawning totals
	public float spawningWeight(){
		return 1;
	}
	
	public boolean reset() {
		return false;
	}
	
	public void beckon( int cell ) {
		
		notice();
		
		if (state != HUNTING && state != FLEEING) {
			state = WANDERING;
		}
		target = cell;
	}
	
	public String description() {
		return Messages.get(this, "desc");
	}

	@SuppressWarnings("unchecked")
	public String dropsInfo() {
		if (loot == null && lootOther == null && lootThird == null) {
			return "";
		}

		StringBuilder info = new StringBuilder();

		try {
			float seasonMod = GameCalendar.seasonLootMultiplier();
			float weekdayMod = GameCalendar.weekdayLootMultiplier();
			if (seasonMod != 1f) {
				int pct = Math.round((seasonMod - 1f) * 100);
				String sign = pct > 0 ? "+" : "";
				info.append("\n_").append(Messages.get(Mob.class, "drops_season_mod",
						Messages.get(GameCalendar.class, GameCalendar.season().name().toLowerCase()),
						sign + pct + "%")).append("_");
			}
			if (weekdayMod != 1f) {
				int pct = Math.round((weekdayMod - 1f) * 100);
				String sign = pct > 0 ? "+" : "";
				info.append("\n_").append(Messages.get(Mob.class, "drops_weekday_mod",
						Messages.get(GameCalendar.class, "day_" + GameCalendar.weekday().name().toLowerCase()),
						sign + pct + "%")).append("_");
			}
		} catch (Exception ignored) {
			// Calendar may not be available outside of active game
		}

		return info.toString();
	}

	public static class DropInfo {
		public final Item item;
		public final String name;
		public final float chance;
		public final boolean slotSeen;
		public final boolean isCategory;

		public DropInfo(Item item, String name, float chance, boolean slotSeen, boolean isCategory) {
			this.item = item;
			this.name = name;
			this.chance = chance;
			this.slotSeen = slotSeen;
			this.isCategory = isCategory;
		}
	}

	public ArrayList<DropInfo> getDrops() {
		ArrayList<DropInfo> drops = new ArrayList<>();
		Class<?> cls = getClass();
		if (loot != null) {
			drops.add(new DropInfo(lootToItem(loot), lootName(loot), lootChance,
					Bestiary.isLootSlotSeen(cls, 0), loot instanceof Generator.Category));
		}
		if (lootOther != null) {
			drops.add(new DropInfo(lootToItem(lootOther), lootName(lootOther), lootChanceOther,
					Bestiary.isLootSlotSeen(cls, 1), lootOther instanceof Generator.Category));
		}
		if (lootThird != null) {
			drops.add(new DropInfo(lootToItem(lootThird), lootName(lootThird), lootChanceThird,
					Bestiary.isLootSlotSeen(cls, 2), lootThird instanceof Generator.Category));
		}
		if (extraLootSlots != null) {
			for (int i = 0; i < extraLootSlots.size(); i++) {
				Object lootObj = extraLootSlots.get(i);
				float chance = extraLootChanceSlots.get(i);
				drops.add(new DropInfo(lootToItem(lootObj), lootName(lootObj), chance,
						Bestiary.isLootSlotSeen(cls, 3 + i), lootObj instanceof Generator.Category));
			}
		}
		return drops;
	}

	private static Item lootToItem(Object lootObj) {
		if (lootObj instanceof Item) {
			return (Item) lootObj;
		} else if (lootObj instanceof Class<?>) {
			try {
				return (Item) Reflection.newInstance((Class<?>) lootObj);
			} catch (Exception e) {
				return null;
			}
		} else if (lootObj instanceof Generator.Category) {
			Generator.Category cat = (Generator.Category) lootObj;
			if (cat.classes != null && cat.classes.length > 0) {
				try {
					return (Item) Reflection.newInstance(cat.classes[0]);
				} catch (Exception e) {
					return null;
				}
			}
		}
		return null;
	}

	public static String lootName(Object lootObj) {
		if (lootObj instanceof Generator.Category) {
			Generator.Category cat = (Generator.Category) lootObj;
			switch (cat) {
				case WEAPON: case WEP_T1: case WEP_T2: case WEP_T3: case WEP_T4: case WEP_T5:
					return Messages.get(Mob.class, "drops_cat_weapon");
				case ARMOR:
					return Messages.get(Mob.class, "drops_cat_armor");
				case MISSILE: case MIS_T1: case MIS_T2: case MIS_T3: case MIS_T4: case MIS_T5:
					return Messages.get(Mob.class, "drops_cat_missile");
				case POTION:
					return Messages.get(Mob.class, "drops_cat_potion");
				case SCROLL:
					return Messages.get(Mob.class, "drops_cat_scroll");
				case SEED:
					return Messages.get(Mob.class, "drops_cat_seed");
				case RING:
					return Messages.get(Mob.class, "drops_cat_ring");
				case WAND:
					return Messages.get(Mob.class, "drops_cat_wand");
				case GOLD:
					return Messages.get(Mob.class, "drops_cat_gold");
				default:
					return Messages.get(Mob.class, "drops_cat_item");
			}
		} else if (lootObj instanceof Class<?>) {
			try {
				Item item = (Item) Reflection.newInstance((Class<?>) lootObj);
				return item != null ? Messages.titleCase(item.name()) : "???";
			} catch (Exception e) {
				return "???";
			}
		} else if (lootObj instanceof Item) {
			return Messages.titleCase(((Item) lootObj).name());
		}
		return "???";
	}

	public static String formatChance(float chance) {
		float pct = chance * 100f;
		int rounded = Math.round(pct);
		if (Math.abs(pct - rounded) < 0.1f) {
			return rounded + "%";
		}
		int tenths = Math.round(pct * 10f);
		return (tenths / 10) + "." + (tenths % 10) + "%";
	}

	public String info(){
		String desc = description();

		for (Buff b : buffs(ChampionEnemy.class)){
			desc += "\n\n_" + Messages.titleCase(b.name()) + "_\n" + b.desc();
		}

		return desc;
	}
	
	public void notice() {
		sprite.showAlert();
	}
	
	public void yell( String str ) {
		GLog.newLine();
		GLog.n( "%s: \"%s\" ", Messages.titleCase(name()), str );
	}

	//some mobs have an associated landmark entry, which is added when the hero sees them
	//mobs may also remove this landmark in some cases, such as when a quest is complete or they die
	public Notes.Landmark landmark(){
		return null;
	}

	public interface AiState {
		boolean act( boolean enemyInFOV, boolean justAlerted );
	}

	protected class Sleeping implements AiState {

		public static final String TAG	= "SLEEPING";

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {

			//debuffs cause mobs to wake as well
			for (Buff b : buffs()){
				if (b.type == Buff.buffType.NEGATIVE){
					awaken(enemyInFOV);
					if (state == SLEEPING){
						spend(TICK); //wait if we can't wake up for some reason
					}
					return true;
				}
			}

			//can be awoken by the least stealthy hostile present, not necessarily just our target
			if (enemyInFOV || (enemy != null && enemy.invisible > 0)) {

				float highestChance = Float.POSITIVE_INFINITY;
				Char closestHostile = null;

				for (Char ch : Actor.chars()){
					if (fieldOfView[ch.pos] && ch.invisible == 0 && ch.alignment != alignment && ch.alignment != Alignment.NEUTRAL){
						float bestChance = detectionChance(ch);
						//silent steps rogue talent, which also applies to rogue's shadow clone
						if ((ch instanceof Hero || ch instanceof ShadowClone.ShadowAlly)
								&& Dungeon.hero.hasTalent(Talent.SILENT_STEPS)){
							if (distance(ch) >= 4 - Dungeon.hero.pointsInTalent(Talent.SILENT_STEPS)) {
								bestChance = Float.POSITIVE_INFINITY;
							}
						}
						//flying characters are naturally stealthy
						if (ch.flying && distance(ch) >= 2){
							bestChance = Float.POSITIVE_INFINITY;
						}
						if (bestChance < highestChance){
							highestChance = bestChance;
							closestHostile = ch;
						}
					}
				}

				if (closestHostile != null && Random.Float() < detectionChance(closestHostile)
						&& !(closestHostile == Dungeon.hero && Dungeon.hero.heroSkills != null
							&& Dungeon.hero.heroSkills.anyPreventsWaking(Mob.this))) {
					awaken(enemyInFOV);
					if (state == SLEEPING){
						spend(TICK); //wait if we can't wake up for some reason
					}
					return true;
				}

			}

			enemySeen = false;
			spend( TICK );

			return true;
		}

		//chance is 1 in (distance + stealth)
		protected float detectionChance( Char enemy ){
			return 1 / (distance( enemy ) + enemy.stealth());
		}

		protected void awaken( boolean enemyInFOV ){
			if (enemyInFOV) {
				enemySeen = true;
				notice();
				state = HUNTING;
				target = enemy.pos;
				if (enemy == Dungeon.hero) xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.heroNoticed(Mob.this, true);
			} else {
				notice();
				state = WANDERING;
				target = Dungeon.level.randomDestination( Mob.this );
			}

			spend(TIME_TO_WAKE_UP);
		}
	}

	protected class Wandering implements AiState {

		public static final String TAG	= "WANDERING";

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (enemyInFOV && (justAlerted || Random.Float() < detectionChance(enemy))) {

				return noticeEnemy();

			} else {

				return continueWandering();

			}
		}

		//chance is 1 in (distance/2 + stealth)
		protected float detectionChance( Char enemy ){
			return 1 / (distance( enemy ) / 2f + enemy.stealth());
		}

		protected boolean noticeEnemy(){
			enemySeen = true;
			
			notice();
			alerted = true;
			state = HUNTING;
			target = enemy.pos;
			if (enemy == Dungeon.hero) xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.heroNoticed(Mob.this, false);
			
			return true;
		}
		
		protected boolean continueWandering(){
			enemySeen = false;
			
			int oldPos = pos;
			if (target != -1 && getCloser( target )) {
				spend( 1 / speed() );
				return moveSprite( oldPos, pos );
			} else {
				target = randomDestination();
				spend( TICK );
			}
			
			return true;
		}

		protected int randomDestination(){
			return Dungeon.level.randomDestination( Mob.this );
		}
		
	}

	//we keep a list of characters we were recently hit by, so we can switch targets if needed
	protected ArrayList<Char> recentlyAttackedBy = new ArrayList<>();

	protected class Hunting implements AiState {

		public static final String TAG	= "HUNTING";

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			if (enemyInFOV && !isCharmedBy( enemy ) && canAttack( enemy )) {

				recentlyAttackedBy.clear();
				target = enemy.pos;
				return doAttack( enemy );

			} else {

				//if we cannot attack our target, but were hit by something else that
				// is visible and attackable or closer, swap targets
				if (handleRecentAttackers()){
					return act( true, justAlerted );
				}

				if (enemyInFOV) {
					target = enemy.pos;
				} else if (enemy == null) {
					sprite.showLost();
					state = WANDERING;
					target = ((Mob.Wandering)WANDERING).randomDestination();
					spend( TICK );
					return true;
				}
				
				int oldPos = pos;
				if (target != -1 && getCloser( target )) {
					
					spend( 1 / speed() );
					return moveSprite( oldPos,  pos );

				} else {

					return handleUnreachableTarget(enemyInFOV, justAlerted);
				}
			}
		}

		protected boolean handleRecentAttackers(){
			boolean swapped = false;
			if (!recentlyAttackedBy.isEmpty()){
				for (Char ch : recentlyAttackedBy){
					if (ch != null && ch.isActive() && Actor.chars().contains(ch) && alignment != ch.alignment && fieldOfView[ch.pos] && ch.invisible == 0 && !isCharmedBy(ch)) {
						if (canAttack(ch) || enemy == null || Dungeon.level.distance(pos, ch.pos) < Dungeon.level.distance(pos, enemy.pos)) {
							enemy = ch;
							target = ch.pos;
							swapped = true;
						}
					}
				}
				recentlyAttackedBy.clear();
			}
			return swapped;
		}

		//prevents rare infinite loop cases
		protected boolean recursing = false;

		//Try to switch targets to another enemy that is closer or reachable
		//unless we have already done that and still can't move toward them, then move on.
		protected boolean handleUnreachableTarget(boolean enemyInFOV, boolean justAlerted){
			if (!recursing) {
				Char oldEnemy = enemy;
				enemy = null;
				enemy = chooseEnemy();
				if (enemy != null && enemy != oldEnemy) {
					recursing = true;
					boolean result = act(enemyInFOV, justAlerted);
					recursing = false;
					return result;
				}
			}

			spend( TICK );
			if (!enemyInFOV) {
				sprite.showLost();
				state = WANDERING;
				target = ((Mob.Wandering)WANDERING).randomDestination();
			}
			return true;
		}
	}

	//essentially a more aggressive version of wandering, where target pos is updated like hunting.
	//not currently used directly by mobs outside of the vault, which also add more behaviour here
	protected class Investigating extends Wandering {

		public static final String TAG	= "INVESTIGATING";

		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (enemyInFOV){
				target = enemy.pos;
			} else {
				//we lose our target BEFORE reaching their last known position
				if (Dungeon.level.distance(pos, target) <= 1){
					sprite.showLost();
					state = WANDERING;
					target = ((Mob.Wandering)WANDERING).randomDestination();
					spend( TICK );
					return true;
				}
			}
			return super.act(enemyInFOV, justAlerted);
		}

		//same detection chance as wandering

	}

	protected class Fleeing implements AiState {

		public static final String TAG	= "FLEEING";

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			//triggers escape logic when 0-dist rolls a 6 or greater.
			if (enemy == null || !enemyInFOV && 1 + Random.Int(Dungeon.level.distance(pos, target)) >= 6){
				escaped();
				if (state != FLEEING){
					spend( TICK );
					return true;
				}
			
			//if enemy isn't in FOV, keep running from their previous position.
			} else if (enemyInFOV) {
				target = enemy.pos;
			}

			int oldPos = pos;
			if (target != -1 && getFurther( target )) {

				spend( 1 / speed() );
				return moveSprite( oldPos, pos );

			} else {

				spend( TICK );
				nowhereToRun();

				return true;
			}
		}

		protected void escaped(){
			//does nothing by default, some enemies have special logic for this
		}

		//enemies will turn and fight if they have nowhere to run and aren't affect by terror
		protected void nowhereToRun() {
			if (buff( Terror.class ) == null && buff( Dread.class ) == null) {
				if (enemySeen) {
					sprite.showStatus(CharSprite.WARNING, Messages.get(Mob.class, "rage"));
					state = HUNTING;
				} else {
					state = WANDERING;
				}
			}
		}
	}

	protected class Passive implements AiState {

		public static final String TAG	= "PASSIVE";

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			spend( TICK );
			return true;
		}
	}


	// *** Alternative investigating, wandering, and sleeping behaviour for stealth gameplay ***
	// Currently exclusively used in the dwarven vault

	//swaps AI logic for stealth gameplay.
	// This is persisted over save/load, mobs with custom AI logic may need to override this
	public void activateSteathGameplayBehaviour(){
		if (!usingStealthGamePlay) {
			usingStealthGamePlay = true;
			AiState oldInvestigate = INVESTIGATING;
			INVESTIGATING = new StealthGameplayInvestigating();
			if (state == oldInvestigate) {
				state = INVESTIGATING;
			}

			AiState oldWander = WANDERING;
			WANDERING = new StealthGameplayWandering();
			if (state == oldWander) {
				state = WANDERING;
			}

			AiState oldSleep = SLEEPING;
			SLEEPING = new StealthGameplaySleeping();
			if (state == oldSleep){
				state = SLEEPING;
			}
		}
	}

	private boolean usingStealthGamePlay;

	//detection chance is lower on the first turn of investigating
	private int investigatingTurns;

	protected class StealthGameplayInvestigating extends Investigating {

		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (enemyInFOV){
				investigatingTurns++;
			} else {
				investigatingTurns = 0;
			}
			return super.act(enemyInFOV, justAlerted);
		}

		@Override
		//chance is 1 in (distance/2 + stealth) at base (classic wandering), but reduced if enemy was only just seen
		protected float detectionChance( Char enemy ){
			float chance = 1 / (distance( enemy ) / 2f + enemy.stealth());
			if (investigatingTurns == 1 && chance <= 1){
				chance -= 0.33f;
			}
			return chance;
		}
	}

	public void setupStealthGameplayWanderPositions(int[] wanderPositions, int startingIdx){
		this.wanderPositions = wanderPositions;
		wanderPosIdx = startingIdx;
	}

	//in stealth gameplay mobs wander to more consistent pre-determined locations
	private int wanderPosIdx = 0;
	private int[] wanderPositions;

	protected class StealthGameplayWandering extends Wandering {

		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			return super.act(enemyInFOV, justAlerted);
		}

		@Override
		protected float detectionChance( Char enemy ){
			//defaults to 1 in (distance + stealth) (classic sleeping detection)
			if (!Dungeon.level.adjacent(pos, previousPos)){
				return 1 / (distance( enemy ) + enemy.stealth());
			}

			float movementDir = PointF.angle(Dungeon.level.cellToPoint(previousPos), Dungeon.level.cellToPoint(pos))/PointF.G2R;;
			float enemyDir = PointF.angle(Dungeon.level.cellToPoint(pos), Dungeon.level.cellToPoint(enemy.pos))/PointF.G2R;
			//classic wandering detection if enemy is touching a 75 degree cone of vision and within 6 tiles
			if (Math.abs(enemyDir - movementDir) <= 37.5f && Dungeon.level.distance(pos, enemy.pos) <= 6){
				return 1 / (distance( enemy ) / 2f + enemy.stealth());
			//classic sleeping (i.e. default) detection if enemy is touching a 150 degree vision cone
			} else if (Math.abs(enemyDir - movementDir) <= 75f){
				return 1 / (distance( enemy ) + enemy.stealth());
			//otherwise uses very low chance detection (1/8 at 2 tiles, 0% at 3+)
			} else {
				float chance = 1 / (float)Math.pow((distance( enemy ) + enemy.stealth()), 3);
				if (chance < 0.1f){
					return 0;
				} else {
					return chance;
				}
			}
		}

		@Override
		protected boolean noticeEnemy() {
			super.noticeEnemy();
			alerted = false;
			state = INVESTIGATING;
			investigatingTurns = 0;
			sprite.showInvestigate();
			spend(TICK);
			//hero must know if they are detected
			if (!Dungeon.level.heroFOV[pos]){
				Buff.affect(Dungeon.hero, TalismanOfForesight.CharAwareness.class, 1f).charID = id();
			}
			return true;
		}

		@Override
		protected int randomDestination() {
			//stay still by default if given no other wandering behaviour
			if (wanderPositions == null){
				wanderPositions = new int[1];
				wanderPositions[0] = pos;
			}

			int wanderPos = wanderPositions[wanderPosIdx];
			if (wanderPos == pos) {
				if (wanderPositions.length > 1) {
					wanderPosIdx++;
					if (wanderPosIdx == wanderPositions.length) {
						wanderPosIdx = 0;
					}
					wanderPos = wanderPositions[wanderPosIdx];
				} else {
					//reset this, representing the mob looking around in place
					previousPos = pos;
					sprite.idle();
				}
			}
			return wanderPos;
		}
	}

	protected class StealthGameplaySleeping extends Sleeping {

		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			//stay still by default if given no other wandering behaviour
			if (wanderPositions == null){
				wanderPositions = new int[1];
				wanderPositions[0] = pos;
			}
			return super.act(enemyInFOV, justAlerted);
		}

		protected void awaken(boolean enemyInFOV) {
			super.awaken(enemyInFOV);
			if (state == HUNTING){
				alerted = false;
				state = INVESTIGATING;
				investigatingTurns = 0;
				sprite.showInvestigate();
				//hero must know if they are detected
				if (!Dungeon.level.heroFOV[pos]){
					Buff.affect(Dungeon.hero, TalismanOfForesight.CharAwareness.class, 1f).charID = id();
				}
			}
		}

		//chance is 1 in (distance + stealth)^2
		//set to 0 if below 10% (usually happens at 4+ distance)
		@Override
		protected float detectionChance( Char enemy ){
			float chance = 1 / (float)Math.pow((distance( enemy ) + enemy.stealth()), 2);
			if (chance < 0.1f){
				return 0;
			} else {
				return chance;
			}
		}
	}
	
	private static ArrayList<Mob> heldAllies = new ArrayList<>();

	public static void holdAllies( Level level ){
		holdAllies(level, Dungeon.hero.pos);
	}

	public static void holdAllies( Level level, int holdFromPos ){
		heldAllies.clear();
		for (Mob mob : level.mobs.toArray( new Mob[0] )) {
			//preserve directable allies or empowered intelligent allies no matter where they are
			if (mob instanceof DirectableAlly
				|| (mob.intelligentAlly && PowerOfMany.getPoweredAlly() == mob)) {
				if (mob instanceof DirectableAlly) {
					((DirectableAlly) mob).clearDefensingPos();
				}
				level.mobs.remove( mob );
				heldAllies.add(mob);
				
			//preserve other intelligent allies if they are near the hero
			} else if (mob.alignment == Alignment.ALLY
					&& mob.intelligentAlly
					&& Dungeon.level.distance(holdFromPos, mob.pos) <= 5){
				level.mobs.remove( mob );
				heldAllies.add(mob);
			}
		}
	}

	public static void restoreAllies( Level level, int pos ){
		restoreAllies(level, pos, -1);
	}

	public static void restoreAllies( Level level, int pos, int gravitatePos ){
		if (!heldAllies.isEmpty()){
			
			ArrayList<Integer> candidatePositions = new ArrayList<>();
			for (int i : PathFinder.NEIGHBOURS8) {
				if (!Dungeon.level.solid[i+pos] && !Dungeon.level.avoid[i+pos] && level.findMob(i+pos) == null){
					candidatePositions.add(i+pos);
				}
			}

			//gravitate pos sets a preferred location for allies to be closer to
			if (gravitatePos == -1) {
				Collections.shuffle(candidatePositions);
			} else {
				Collections.sort(candidatePositions, new Comparator<Integer>() {
					@Override
					public int compare(Integer t1, Integer t2) {
						return Dungeon.level.distance(gravitatePos, t1) -
								Dungeon.level.distance(gravitatePos, t2);
					}
				});
			}

			//can only have one empowered ally at once, prioritize incoming ally
			if (Stasis.getStasisAlly() != null){
				for (Mob mob : level.mobs.toArray( new Mob[0] )) {
					if (mob.buff(PowerOfMany.PowerBuff.class) != null){
						mob.buff(PowerOfMany.PowerBuff.class).detach();
					}
				}
			}
			
			for (Mob ally : heldAllies) {

				//can only have one empowered ally at once, prioritize incoming ally
				if (ally.buff(PowerOfMany.PowerBuff.class) != null){
					for (Mob mob : level.mobs.toArray( new Mob[0] )) {
						if (mob.buff(PowerOfMany.PowerBuff.class) != null){
							mob.buff(PowerOfMany.PowerBuff.class).detach();
						}
					}
				}

				level.mobs.add(ally);
				ally.state = ally.WANDERING;
				
				if (!candidatePositions.isEmpty()){
					ally.pos = candidatePositions.remove(0);
				} else {
					ally.pos = pos;
				}
				if (ally.sprite != null) ally.sprite.place(ally.pos);

				if (ally.fieldOfView == null || ally.fieldOfView.length != level.length()){
					ally.fieldOfView = new boolean[level.length()];
				}
				Dungeon.level.updateFieldOfView( ally, ally.fieldOfView );
				
			}
		}
		heldAllies.clear();
	}
	
	public static void clearHeldAllies(){
		heldAllies.clear();
	}
}

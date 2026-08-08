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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum CurrentSkills {

	WARRIOR, MAGE, ROGUE, HUNTRESS, DUELIST, CLERIC;

	public enum BRANCHES { PASSIVEA, PASSIVEB, ACTIVE, FOURTH, SUBCLASS }

	public static final String TYPE = "TYPE";

	//branches hold any number of skills; the numbered fields below stay as
	//aliases onto the first three so the tree UI and the toggle code that
	//reads e.g. active2 keep working
	public Skill branchPA = null;
	public final List<Skill> passiveASkills = new ArrayList<>();
	public Skill passiveA1 = null;
	public Skill passiveA2 = null;
	public Skill passiveA3 = null;

	public Skill branchPB = null;
	public final List<Skill> passiveBSkills = new ArrayList<>();
	public Skill passiveB1 = null;
	public Skill passiveB2 = null;
	public Skill passiveB3 = null;

	public Skill branchA = null;
	public final List<Skill> activeSkills = new ArrayList<>();
	public Skill active1 = null;
	public Skill active2 = null;
	public Skill active3 = null;

	//a fourth core branch, themed per class
	public Skill branchD = null;
	public final List<Skill> fourthSkills = new ArrayList<>();

	//subclass branch: only lives once a subclass is chosen
	public Skill branchS = null;
	public final List<Skill> subSkills = new ArrayList<>();
	public Skill sub1 = null;
	public Skill sub2 = null;
	public Skill sub3 = null;

	/** every core (non-subclass) skill, in tree order; rebuilt by buildTree() */
	private final List<Skill> coreSkills = new ArrayList<>();
	/** core + subclass, cached because damageRoll/attackProc run per swing */
	private final List<Skill> allSkills = new ArrayList<>();

	public Skill lastUsed = null;

	CurrentSkills(){
		//eagerly build the tree so the fields are never null, even before
		//a run properly init()s them (e.g. status pane peeking at a hero
		//that is still being constructed)
		buildTree();
	}

	public void init(){
		init(Dungeon.hero);
	}

	public void init(Hero hero){
		hero.heroSkills = this;
		lastUsed = null;
		buildTree();
	}

	private void buildTree(){
		switch (this) {
			case WARRIOR: {
				Skill stone = new StoneSkin(), blood = new Bloodthirst();
				Skill iron = new IronStance(), reckless = new RecklessFury();
				branchPA = new WarriorPassiveA();
				setBranch(passiveASkills, new Endurance(), new Regeneration(), new Toughness(), new LastStand(), stone, blood);
				branchPB = new WarriorPassiveB();
				setBranch(passiveBSkills, new FirmHand(), new Aggression(), new Mastery(), new Warbreaker());
				branchA = new WarriorActive();
				setBranch(activeSkills, new Smash(), new KnockBack(), new Rampage(), new Earthshatter(), iron, reckless);
				branchD = new WarriorFourth();
				setBranch(fourthSkills, new Hamstring(), new Demoralize(), new Shieldbearer());
				linkExclusive(stone, blood);
				linkExclusive(iron, reckless);
				break;
			}

			case MAGE: {
				Skill serene = new SereneFocus(), willOfIron = new WillOfIron();
				Skill pyre = new PyreAffinity(), rime = new RimeAffinity();
				branchPA = new MagePassiveA();
				setBranch(passiveASkills, new Spirituality(), new Meditation(), new SpiritArmor(), new Transcendence(), serene, willOfIron);
				branchPB = new MagePassiveB();
				setBranch(passiveBSkills, new Wizard(), new Sorcerer(), new Summoner(), new SoulTether());
				branchA = new MageActive();
				setBranch(activeSkills, new SummonRat(), new Spark(), new SummonSkeleton(), new SoulDetonation());
				branchD = new MageFourth();
				setBranch(fourthSkills, new CinderTrail(), new FrostNova(), new StormCall(), pyre, rime);
				linkExclusive(serene, willOfIron);
				linkExclusive(pyre, rime);
				break;
			}

			case ROGUE: {
				Skill ash = new AshVeil(), bloodDance = new BloodDance();
				Skill panic = new PanicHarvest(), howl = new DreadHowl();
				branchPA = new RoguePassiveA();
				setBranch(passiveASkills, new Bandit(), new Stealth(), new LockSmith(), new MasterThief());
				branchPB = new RoguePassiveB();
				setBranch(passiveBSkills, new Venom(), new Scorpion(), new SilentDeath(), new Necrotoxin());
				branchA = new RogueActive();
				setBranch(activeSkills, new DoubleStab(), new NinjaBomb(), new ShadowClone(), new PhantomStrike(), ash, bloodDance);
				branchD = new RogueFourth();
				setBranch(fourthSkills, new CreepingDread(), new Predation(), new Blackout(), panic, howl);
				linkExclusive(ash, bloodDance);
				linkExclusive(panic, howl);
				break;
			}

			case HUNTRESS: {
				Skill lone = new LoneWolf(), poacher = new Poacher();
				Skill frost = new FrostArrows(), ember = new EmberArrows();
				branchPA = new HuntressPassiveA();
				setBranch(passiveASkills, new Fletching(), new Awareness(), new Hunting(), new SixthSense(), lone, poacher);
				branchPB = new HuntressPassiveB();
				setBranch(passiveBSkills, new Accuracy(), new KneeShot(), new IronTip(), new Heartseeker());
				branchA = new HuntressActive();
				setBranch(activeSkills, new AimedShot(), new DoubleShot(), new Bombvoyage(), new ArrowStorm(), frost, ember);
				branchD = new HuntressFourth();
				setBranch(fourthSkills, new Groundwork(), new BearTrap(), new Deadfall());
				linkExclusive(lone, poacher);
				linkExclusive(frost, ember);
				break;
			}

			case DUELIST: {
				Skill finesse = new FinesseGrip(), brutal = new BrutalGrip();
				Skill pirouette = new Pirouette(), bind = new BladeBind();
				branchPA = new DuelistPassiveA();
				setBranch(passiveASkills, new Conditioning(), new Poise(), new ParryStance(), new Aplomb());
				branchPB = new DuelistPassiveB();
				setBranch(passiveBSkills, new Precision(), new WeaponBond(), new BladeMastery(), new TrueEdge(), finesse, brutal);
				branchA = new DuelistActive();
				setBranch(activeSkills, new Lunge(), new RiposteStance(), new WhirlingFlurry(), new ImpalingThrust());
				branchD = new DuelistFourth();
				setBranch(fourthSkills, new Sidestep(), new Fleche(), new CounterTime(), pirouette, bind);
				linkExclusive(finesse, brutal);
				linkExclusive(pirouette, bind);
				break;
			}

			case CLERIC: {
				Skill ascetic = new AsceticVow(), tithe = new BloodTithe();
				Skill zeal = new CrusadersZeal(), faithShield = new ShieldOfTheFaithful();
				branchPA = new ClericPassiveA();
				setBranch(passiveASkills, new Faith(), new Grace(), new Sanctuary(), new LastRites(), ascetic, tithe);
				branchPB = new ClericPassiveB();
				setBranch(passiveBSkills, new RighteousStrikes(), new ZealPassive(), new SacredWeapon(), new DivineWrath(), zeal, faithShield);
				branchA = new ClericActive();
				setBranch(activeSkills, new HolySmite(), new HealingPrayer(), new GuardianSpirit(), new AvatarOfLight());
				branchD = new ClericFourth();
				setBranch(fourthSkills, new Condemn(), new ScouringFlame(), new Reckoning());
				linkExclusive(zeal, faithShield);
				linkExclusive(ascetic, tithe);
				break;
			}
		}
		branchS = null;
		subSkills.clear();
		indexTree();
		indexSub();
	}

	private void setBranch( List<Skill> branch, Skill... skills ){
		branch.clear();
		for (Skill s : skills) branch.add(s);
	}

	/** the two halves of a fork: spending in one permanently locks the other out */
	private static void linkExclusive( Skill a, Skill b ){
		a.exclusiveWith = b;
		b.exclusiveWith = a;
	}

	/** the active branch holds toggles: turning one on turns every other one off */
	public void deactivateOtherToggles( Skill keep ){
		for (Skill s : activeSkills){
			if (s != keep) s.active = false;
		}
	}

	/** re-points the legacy 1/2/3 fields and rebuilds the core/all caches */
	private void indexTree(){
		passiveA1 = at(passiveASkills, 0); passiveA2 = at(passiveASkills, 1); passiveA3 = at(passiveASkills, 2);
		passiveB1 = at(passiveBSkills, 0); passiveB2 = at(passiveBSkills, 1); passiveB3 = at(passiveBSkills, 2);
		active1   = at(activeSkills, 0);   active2   = at(activeSkills, 1);   active3   = at(activeSkills, 2);
		coreSkills.clear();
		coreSkills.addAll(passiveASkills);
		coreSkills.addAll(passiveBSkills);
		coreSkills.addAll(activeSkills);
		coreSkills.addAll(fourthSkills);
		rebuildAll();
	}

	private void indexSub(){
		sub1 = at(subSkills, 0); sub2 = at(subSkills, 1); sub3 = at(subSkills, 2);
		rebuildAll();
	}

	private void rebuildAll(){
		allSkills.clear();
		allSkills.addAll(coreSkills);
		allSkills.addAll(subSkills);
	}

	private static Skill at( List<Skill> list, int i ){
		return i < list.size() ? list.get(i) : null;
	}

	/** builds the 4th branch once a subclass is chosen (or on restore) */
	public void initSubclassBranch( Hero hero ){
		SubBranch b = new SubBranch();
		switch (hero.subClass){
			case BERSERKER:
				setBranch(subSkills, new Carnage(), new BloodRush(), new UndyingWill());
				break;
			case GLADIATOR:
				setBranch(subSkills, new ComboOpener(), new WarCry(), new FinishingBlow());
				break;
			case BATTLEMAGE:
				setBranch(subSkills, new SpellBlade(), new ManaShield(), new ArcaneEcho());
				break;
			case WARLOCK:
				setBranch(subSkills, new SoulDrain(), new Hex(), new DarkPact());
				break;
			case ASSASSIN:
				setBranch(subSkills, new Ambush(), new Garrote(), new Vanish());
				break;
			case FREERUNNER:
				setBranch(subSkills, new Slipstream(), new Parkour(), new MomentumMaster());
				break;
			case SNIPER:
				setBranch(subSkills, new Overwatch(), new HeadShot(), new PiercingFocus());
				break;
			case WARDEN:
				setBranch(subSkills, new Thorns(), new Symbiosis(), new WildCall());
				break;
			case CHAMPION:
				setBranch(subSkills, new Banner(), new SecondWind(), new Challenge());
				break;
			case MONK:
				setBranch(subSkills, new PressurePoint(), new InnerPeace(), new DragonKick());
				break;
			case PRIEST:
				setBranch(subSkills, new Litany(), new MassHeal(), new Purify());
				break;
			case PALADIN:
				setBranch(subSkills, new Aegis(), new HolyCharge(), new Retribution());
				break;
			default:
				subSkills.clear();
				indexSub();
				return;
		}
		b.name = Messages.get(SubBranch.class, hero.subClass.name() + ".name");
		b.desc = Messages.get(SubBranch.class, hero.subClass.name() + ".desc");
		branchS = b;
		indexSub();
	}

	// ---- aggregates so hero hooks can query the subclass branch as one ----

	public int subToHitBonus(){
		int b = 0;
		for (Skill s : subSkills) b += s.toHitBonus();
		return b;
	}

	// ---- aggregates over every skill in the tree (core branches + subclass) ----

	/** how far a named skill is trained anywhere in this hero's tree, 0 if the tree has no such node.
	 *  cross-skill synergies read this rather than a passiveA1/active2-style slot index, which the
	 *  branch lists made meaningless as soon as a branch grew past three nodes */
	public static int skillLevel( Class<? extends Skill> cls ){
		if (Dungeon.hero == null || Dungeon.hero.heroSkills == null) return 0;
		for (Skill s : Dungeon.hero.heroSkills.allSkills){
			if (cls.isInstance(s)) return s.level;
		}
		return 0;
	}

	public float allDamageModifier( boolean ranged ){
		float m = 1f;
		for (Skill s : allSkills) m *= ranged ? s.rangedDamageModifier() : s.damageModifier();
		return m;
	}

	public int allIncomingDamage( int dmg ){
		for (Skill s : allSkills){
			if (dmg <= 0) break;                 //mirrors Hero's `if (dmg > 0)` guard: don't burn mana on 0 dmg
			dmg = Math.round(dmg * s.incomingDamageModifier());
			dmg -= s.incomingDamageReduction(dmg);
		}
		return Math.max(0, dmg);
	}

	public int allOnHit( Char enemy, int damage, boolean ranged ){
		for (Skill s : allSkills) damage = s.onHitProc(enemy, damage, ranged);
		return damage;
	}

	public int allOnDefend( Char enemy, int damage ){
		for (Skill s : allSkills) damage = s.onDefendProc(enemy, damage);
		return damage;
	}

	public int allToHitBonus(){
		int b = 0;
		for (Skill s : allSkills) b += s.toHitBonus();
		return b;
	}

	public float allToHitModifier(){
		float m = 1f;
		for (Skill s : allSkills) m *= s.toHitModifier();
		return m;
	}

	/** the regeneration hooks are exponents (delay /= 1.2^bonus), so the sum is capped:
	 *  1.2^6 is already a threefold speed-up, and the stacked kickers can reach 7 on their own */
	public static final int MAX_REGEN_BONUS = 6;

	/**
	 * The skills worth putting on the quick panel: learned, and either
	 * toggleable right now or costing mana to fire. A pure passive never
	 * shows up; a cast you cannot currently afford still does, greyed out,
	 * so the panel does not shuffle around as mana moves.
	 */
	public List<Skill> usableNow( Hero hero ){
		List<Skill> out = new ArrayList<>();
		for (Skill s : allSkills){
			if (s.level <= 0) continue;
			if (!s.actions( hero ).isEmpty() || s.mana > 0) out.add(s);
		}
		return out;
	}

	public int allHealthRegen(){
		int b = 0;
		for (Skill s : allSkills) b += s.healthRegenerationBonus();
		return Math.min( b, MAX_REGEN_BONUS );
	}

	public int allManaRegen(){
		int b = 0;
		for (Skill s : allSkills) b += s.manaRegenerationBonus();
		return Math.min( b, MAX_REGEN_BONUS );
	}

	public int allStealth(){
		int b = 0;
		for (Skill s : allSkills) b += s.stealthBonus();
		return b;
	}

	public int allWeaponLevelBonus(){
		int b = 0;
		for (Skill s : allSkills) b += s.weaponLevelBonus();
		return b;
	}

	/** extra turns of poison the venom procs are worth (Rogue: Scorpion) */
	public int allVenomBonus(){
		int b = 0;
		for (Skill s : allSkills) b += s.venomBonus();
		return b;
	}

	public int allSummonLimit(){
		int b = 0;
		for (Skill s : allSkills) b += s.summoningLimitBonus();
		return b;
	}

	public float allWandDamage(){
		float m = 1f;
		for (Skill s : allSkills) m *= s.wandDamageBonus();
		return m;
	}

	public float allWandRecharge(){
		float m = 1f;
		for (Skill s : allSkills) m *= s.wandRechargeSpeedReduction();
		return m;
	}

	public int allLootBonus( int gold ){
		int b = 0;
		for (Skill s : allSkills) b += s.lootBonus(gold);
		return b;
	}

	public int allFletching(){
		int b = 0;
		for (Skill s : allSkills) b += s.fletching();
		return b;
	}

	public int allHunting(){
		int b = 0;
		for (Skill s : allSkills) b += s.hunting();
		return b;
	}

	// ---- chance procs: skip un-learned skills (they yell on failed rolls), stop at the first hit ----

	public boolean anyKnocksBack(){
		for (Skill s : allSkills) if (s.level > 0 && s.knocksBack()) return true;
		return false;
	}

	public boolean anyAoEDamage(){
		for (Skill s : allSkills) if (s.level > 0 && s.AoEDamage()) return true;
		return false;
	}

	public boolean anyInstantKill(){
		for (Skill s : allSkills) if (s.level > 0 && s.instantKill()) return true;
		return false;
	}

	public boolean anyDodge(){
		for (Skill s : allSkills) if (s.level > 0 && s.dodgeChance()) return true;
		return false;
	}

	public boolean anyDisableTrap(){
		for (Skill s : allSkills) if (s.level > 0 && s.disableTrap()) return true;
		return false;
	}

	public boolean anyAimedShot(){
		for (Skill s : allSkills) if (s.level > 0 && s.aimedShot()) return true;
		return false;
	}

	public boolean anyDoubleShot(){
		for (Skill s : allSkills) if (s.level > 0 && s.doubleShot()) return true;
		return false;
	}

	public boolean anyDoubleStab(){
		for (Skill s : allSkills) if (s.level > 0 && s.doubleStab()) return true;
		return false;
	}

	public boolean anyArrowToBomb(){
		for (Skill s : allSkills) if (s.level > 0 && s.arrowToBomb()) return true;
		return false;
	}

	// ---- rollers whose caller needs the skill that fired (its level / a second read) ----

	/** the skill whose venom proc landed, or null */
	public Skill rollVenomous(){
		for (Skill s : allSkills) if (s.level > 0 && s.venomousAttack()) return s;
		return null;
	}

	/** the skill whose cripple proc landed, or null */
	public Skill rollCripple(){
		for (Skill s : allSkills) if (s.level > 0 && s.cripple()) return s;
		return null;
	}

	/** the skill whose pass-through proc landed (caller then reads passThroughTargets(false)), or null */
	public Skill rollPassThrough(){
		for (Skill s : allSkills) if (s.level > 0 && s.passThroughTargets(true) > 0) return s;
		return null;
	}

	public static CurrentSkills forHero( Hero hero ){
		switch (hero.heroClass){
			case WARRIOR: default: return WARRIOR;
			case MAGE:     return MAGE;
			case ROGUE:    return ROGUE;
			case HUNTRESS: return HUNTRESS;
			case DUELIST:  return DUELIST;
			case CLERIC:   return CLERIC;
		}
	}

	public List<Skill> branchSkills( BRANCHES branch ){
		switch (branch){
			case PASSIVEA: return passiveASkills;
			case PASSIVEB: return passiveBSkills;
			case ACTIVE:   return activeSkills;
			case FOURTH:   return fourthSkills;
			case SUBCLASS: return subSkills;
		}
		return Collections.emptyList();
	}

	public int totalSpent(BRANCHES branch){
		int spent = 0;
		for (Skill s : branchSkills(branch)) spent += s.level * s.upgradeCost();
		return spent;
	}

	/** a skill is up for advancement unless it is maxed or its fork sibling was taken */
	private static boolean upgradeable( Skill s ){
		return s.level < Skill.MAX_LEVEL && !(s.exclusiveWith != null && s.exclusiveWith.level > 0);
	}

	public boolean canUpgrade(BRANCHES branch){
		for (Skill s : branchSkills(branch)) if (upgradeable(s)) return true;
		return false;
	}

	public int nextUpgradeCost(BRANCHES branch){
		List<Skill> b = branchSkills(branch);
		if (b.isEmpty()) return 0;
		for (Skill s : b) if (upgradeable(s)) return s.upgradeCost();
		return b.get(b.size()-1).upgradeCost();
	}

	/** an untouched fork: picking a half is permanent, so only the player may resolve it */
	private static boolean pendingChoice( Skill s ){
		return s.exclusiveWith != null && s.level == 0 && s.exclusiveWith.level == 0;
	}

	public void advance(BRANCHES branch){
		List<Skill> b = branchSkills(branch);
		if (b.isEmpty()) return;
		Skill fork = null;
		for (Skill s : b){
			if (!upgradeable(s)) continue;
			if (pendingChoice(s)){
				//never spend into a fork on the player's behalf, look for something else first
				if (fork == null) fork = s;
				continue;
			}
			s.requestUpgrade();
			return;
		}
		if (fork != null){
			GLog.i( Messages.get(Skill.class, "pending_choice", fork.name(), fork.exclusiveWith.name()) );
			return;
		}
		b.get(b.size()-1).requestUpgrade();
	}

	public void storeInBundle( Bundle bundle ){
		bundle.put( TYPE, toString() );
		for (Skill s : allSkills) s.storeInBundle(bundle);
	}

	public static CurrentSkills restoreFromBundle( Bundle bundle ){
		String value = bundle.getString( TYPE );
		try {
			return valueOf( value );
		} catch (Exception e) {
			return WARRIOR;
		}
	}

	public void restoreSkillsFromBundle( Bundle bundle ){
		//a save from before a skill was added to a branch simply has no key for it
		for (Skill s : allSkills){
			if (bundle.contains( Skill.SKILL_LEVEL + " " + s.tag )){
				s.restoreInBundle(bundle);
			} else {
				s.level = 0;
			}
		}
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

public class Skill implements SkillInteractions.HeroDamageSource {

	public static final String AC_ACTIVATE   = "Activate";
	public static final String AC_DEACTIVATE = "Deactivate";
	public static final String AC_SUMMON     = "Summon";
	public static final String AC_CAST       = "Cast";
	public static final String AC_MARK       = "Mark target";

	public static final String SKILL_LEVEL = "LEVEL";
	public static final String SKILL_ACTIVE = "ACTIVE";

	public String tag = "";

	public static final int MAX_LEVEL = 3;

	public static final int STARTING_SKILL = 2;

	public static int availableSkill = STARTING_SKILL;

	public static final float TIME_TO_USE = 1f;

	//name/castText are the English fallbacks for name()/castText(); the bundle wins
	//when a key exists, exactly as for every other display string in the game
	public String name = "Skill";
	public String castText = "";
	public int level = 0;
	public int tier = 1;
	public int mana = 0;
	public int image = 159;

	public boolean active = false;

	/** true for the skills you switch on and off: the tree drains the colour
	 *  out of their icon while they are off, so "on" is visible at a glance */
	public boolean toggleable(){ return false; }

	public boolean multiTargetActive = false;

	/** the other half of a mutually exclusive fork; investing in one locks the other */
	public Skill exclusiveWith = null;

	public boolean requestUpgrade(){
		if (exclusiveWith != null && exclusiveWith.level > 0){
			GLog.w( Messages.get(Skill.class, "exclusive_choice", exclusiveWith.name()) );
			return false;
		}
		if (availableSkill >= tier && level < MAX_LEVEL){
			if (upgrade()){
				level++;
				availableSkill -= tier;
				return true;
			}
		} else {
			GLog.w( Messages.get(Skill.class, "fail_advance") );
		}
		return false;
	}

	protected boolean upgrade(){
		return false;
	}

	public void setLevel(int level){
		this.level = level;
	}

	public float incomingDamageModifier(){ return 1f; }

	public float damageModifier(){ return 1f; }

	public float rangedDamageModifier(){ return 1f; }

	public boolean aimedShot(){ return false; }

	public int damageBonus(int hp){ return damageBonus(hp, false); }

	public int damageBonus(int hp, boolean castText){ return 0; }

	public int toHitBonus(){ return 0; }

	public int healthRegenerationBonus(){ return 0; }

	public int weaponLevelBonus(){ return 0; }

	public int fletching(){ return 0; }

	public boolean knocksBack(){ return false; }

	public boolean AoEDamage(){ return false; }

	public int manaRegenerationBonus(){ return 0; }

	public int incomingDamageReduction(int damage){ return 0; }

	public int incomingDamageReduction(int damage, Object source){
		return incomingDamageReduction(damage);
	}

	//true for skills that only step in against a killing blow; they are the only ones
	//still allowed to touch a tick of damage over time, and only the tick that would kill
	public boolean savesFromDeath(){ return false; }

	/** true for skills whose incomingDamageReduction must see the blow after every other skill has
	 *  shrunk it (a rally or a death save judged on what is really left). CurrentSkills runs these in a
	 *  second pass, in tree order; Conditioning and Last Rites are always treated as last */
	public boolean resolvesIncomingLast(){ return savesFromDeath(); }

	/** kills and Vulnerable: a kill made with this skill as the damage source is credited to the hero
	 *  (Mob.die -> onKill); this says whether that kill counts as a ranged one */
	@Override
	public boolean rangedSource(){ return false; }

	//poison, bleeding, burning, ooze, corrosion, gas and starvation: small repeated ticks
	//that any flat or percentage reduction would erase, so skill mitigation leaves them alone
	public static boolean isTickDamage(Object source){
		return source instanceof xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.DOTbuff
				|| source instanceof xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger
				|| source instanceof xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas
				|| source instanceof xyz.gabriwar.warpedpixeldungeon.actors.blobs.CorruptGas;
	}

	public int image(){ return image; }

	public com.watabou.noosa.Image quickslotIcon(){
		return new xyz.gabriwar.warpedpixeldungeon.sprites.SkillSprite(image());
	}

	public String quickslotStatus(){
		return getManaCost() > 0 ? Integer.toString(getManaCost()) : "";
	}

	/** bundle-backed display name, falling back to the hardcoded field when no key exists */
	public String name(){
		String name = Messages.get(this, "name");
		return name.equals(Messages.NO_TEXT_FOUND) ? this.name : name;
	}

	/** bundle-backed cast shout, falling back to the hardcoded field when no key exists */
	public String castText(){
		String text = Messages.get(this, "cast");
		return text.equals(Messages.NO_TEXT_FOUND) ? castText : text;
	}

	public String info(){ return Messages.get(this, "desc") + "\n" + costUpgradeInfo(); }

	public ArrayList<String> actions( Hero hero ){
		return new ArrayList<>();
	}

	/** true for skills whose damage comes from the weapon or strength: those already grow with the
	 *  hero, so the hero-damage bonus on flat skill damage leaves them alone */
	public boolean weaponScaled(){ return false; }

	/** Quickslot second tap, handled before the toolbar cancels cell selection. */
	public boolean confirmTarget(Hero hero) { return false; }

	public void execute( Hero hero, String action ){
	}

	public float getAlpha(){
		return 0.1f + level * 0.3f;
	}

	public int upgradeCost(){ return tier; }

	protected int totalSpent(){ return 0; }

	protected int nextUpgradeCost(){ return 0; }

	protected boolean canUpgrade(){ return false; }

	public String costUpgradeInfo(){
		return Messages.get(Skill.class, "level_info", name(), level) + "\n"
				+ (level < Skill.MAX_LEVEL ? Messages.get(Skill.class, "upgrade_cost", upgradeCost(), name())
										   : Messages.get(Skill.class, "maxed", name()))
				+ (level > 0 && mana > 0 ? "\n" + Messages.get(Skill.class, "mana_cost", name(), getManaCost()) : "");
	}

	public int getManaCost(){ return mana; }

	public void castTextYell(){
        xyz.gabriwar.warpedpixeldungeon.effects.SkillCastFX.play(this,Dungeon.hero);
		if (!castText().equals("") && Dungeon.hero.sprite != null){
			Dungeon.hero.sprite.showStatus( CharSprite.NEUTRAL, castText() );
		}
	}

	public float wandRechargeSpeedReduction(){ return 1f; }

	public int summoningLimitBonus(){ return 0; }

	public float wandDamageBonus(){ return 1f; }

	public int lootBonus(int gold){ return 0; }

	public int stealthBonus(){ return 0; }

	public boolean disableTrap(){ return false; }

	/** the hero just killed an enemy; ranged is true when a thrown weapon or arrow did it */
	public void onKill( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob, boolean ranged ){}

	public boolean venomousAttack(){ return false; }

	public int venomBonus(){ return 0; }

	public boolean instantKill(){ return false; }

	/** melee blow on a mob that was asleep before this blow (boss/miniboss already excluded), with the
	 *  blow's final damage; true kills it outright, credited to the hero. Default keeps the no-arg roll */
	public boolean instantKill( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int damage ){ return instantKill(); }

	public boolean dodgeChance(){ return false; }
	/** asked by Hero.defenseSkill for every attacker, adjacent or not; true turns the attack into a
	 *  real miss (INFINITE_EVASION). The answer is rolled once per attacker per game time and cached.
	 *  The default keeps the old rule: only attackers beyond arm's reach reach the no-arg roll */
	public boolean dodgeChance( xyz.gabriwar.warpedpixeldungeon.actors.Char attacker ){
		Hero hero = Dungeon.hero;
		return attacker != null && hero != null && Dungeon.level != null
				&& !Dungeon.level.adjacent( hero.pos, attacker.pos ) && dodgeChance();
	}
	/** fired once when this skill's dodge is the one that turned an attack aside. The roll
	 *  itself must stay a pure predicate: defenseSkill() is asked several times per swing,
	 *  twice of them only so the damage tooltip can show a breakdown */
	public void onDodge(){}
	public void onDodge( xyz.gabriwar.warpedpixeldungeon.actors.Char attacker ){ onDodge(); }
	/** and once when it rolled to turn the attack aside and did not */
	public void onDodgeFailed(){}
	public void onDodgeFailed( xyz.gabriwar.warpedpixeldungeon.actors.Char attacker ){ onDodgeFailed(); }

	/** pure predicate, asked by Hero.attackSkill (several times per swing): true makes the hero's attack on target never miss */
	public boolean sureHit( xyz.gabriwar.warpedpixeldungeon.actors.Char target ){ return false; }

	/** pure predicate, asked once per hero weapon blow in Char.attack: true sets the target's armour roll to 0 */
	public boolean ignoresArmor( xyz.gabriwar.warpedpixeldungeon.actors.Char target ){ return false; }

	public float toHitModifier(){ return 1f; }

	public boolean cripple(){ return false; }

	public int passThroughTargets(boolean shout){ return 0; }

	public boolean doubleShot(){ return false; }

	public boolean doubleStab(){ return false; }

	public boolean arrowToBomb(){ return false; }

	public boolean goToSleep(){ return false; }

	/** generic on-hit hook for subclass skills; return the (possibly grown) damage */
	public int onHitProc( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int damage, boolean ranged ){ return damage; }

	/** generic on-defend hook for subclass skills; return the (possibly shrunk) damage */
	public int onDefendProc( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int damage ){ return damage; }

	// ---- event hooks (see sanity/apply/HOOKS.md for ordering and deferral) ----

	/** deferred: an attack from attacker just missed the hero; melee = the attacker stood adjacent when it swung */
	public void onHeroMissed( xyz.gabriwar.warpedpixeldungeon.actors.Char attacker, boolean melee ){}

	/** deferred: an enemy's own step (not a push or teleport) ended adjacent to the hero from a tile that was not */
	public void onEnemyStepsAdjacent( xyz.gabriwar.warpedpixeldungeon.actors.Char enemy, int from ){}

	/** synchronous, inside Char.move: any char (hero included) changed tiles. Only read state or
	 *  queue work with SkillInteractions.defer here; never damage, move or kill directly */
	public void onCharMoved( xyz.gabriwar.warpedpixeldungeon.actors.Char ch, int from, boolean travelling ){}

	/** synchronous, inside a sleeping mob's turn: it just rolled to wake up because it noticed the hero; true keeps it asleep */
	public boolean preventsWaking( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob ){ return false; }

	/** deferred: mob just noticed the hero (woke up facing it, or spotted it while wandering) */
	public void onHeroNoticed( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob, boolean wasSleeping ){}

	/** synchronous, end of Hero.damage: the hero is alive and really lost hpLost health and shieldLost
	 *  shielding to this source. Never fired for damage over time or hunger */
	public void onDamageTaken( int hpLost, int shieldLost, Object source ){}

	/** synchronous, just before a hero wand zap or bolt spell resolves on target */
	public void beforeMagicHit( xyz.gabriwar.warpedpixeldungeon.actors.Char target, Object source ){}

	/** synchronous, right after a hero wand zap or bolt spell took damage (> 0) off an enemy target (it may be dead) */
	public void onMagicDamage( xyz.gabriwar.warpedpixeldungeon.actors.Char target, int damage, Object source ){}

	/** synchronous, in Mob.die, after kill credit: any enemy died, whoever killed it */
	public void onEnemyDeath( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob, Object cause ){}

	/** synchronous, in Char.add while the game is running: a NEGATIVE buff is about to attach to the hero; true stops it */
	public boolean shrugsOffDebuff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff buff ){ return false; }

	/** synchronous, in Hero.actAttack: the hero bumps an adjacent enemy to attack it. Return true only after
	 *  taking the action over completely, including hero.spendAndNext(...); the attack is then not made */
	public boolean onHeroBump( Hero hero, xyz.gabriwar.warpedpixeldungeon.actors.Char enemy ){ return false; }

	/** synchronous: a SkillInteractions.Mark was detached from ch (expired, spent or removed; not on death) */
	public void onSkillMarkEnded( xyz.gabriwar.warpedpixeldungeon.actors.Char ch, SkillInteractions.Mark mark ){}

	/** synchronous, once per turn from SkillField.act, for the skill-driven kinds (RIGGED, JAWS) only */
	public void onFieldTick( xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField field ){}

	/** a skill was cast and paid for through payMana (never for toggles) */
	public void onSkillCast( Skill skill ){}

	/** Blood Tithe-style: the hero is missing mana to cast casting; return true if this skill can pay the
	 *  missing amount another way. Pay it only when commit is true; with commit false only answer */
	public boolean coversManaShortfall( Hero hero, Skill casting, int missing, boolean commit ){ return false; }

	/** whether the hero can pay cost now, counting skills that cover a shortfall */
	public boolean canPayMana( Hero hero, int cost ){
		if (hero == null) return false;
		if (hero.MP >= cost) return true;
		return hero.heroSkills != null && hero.heroSkills.coverManaShortfall( hero, this, cost - hero.MP, false );
	}

	/** spends cost mana, the missing part through a covering skill; false and nothing spent when it
	 *  can't be paid. A successful payment by a non-toggle fires onSkillCast on every skill */
	public boolean payMana( Hero hero, int cost ){
		if (!canPayMana( hero, cost )) return false;
		if (hero.MP >= cost){
			hero.MP -= cost;
		} else {
			int missing = cost - hero.MP;
			if (!hero.heroSkills.coverManaShortfall( hero, this, missing, true )) return false;
			hero.MP = 0;
		}
		if (!toggleable() && hero.heroSkills != null) hero.heroSkills.onSkillCast( this );
		return true;
	}

	public void storeInBundle(Bundle bundle){
		bundle.put( SKILL_LEVEL + " " + tag, level );
		bundle.put( SKILL_ACTIVE + " " + tag, active );
	}

	public void restoreInBundle(Bundle bundle){
		level = bundle.getInt( SKILL_LEVEL + " " + tag );
		//a save from before the stance was bundled simply has no key: the skill starts off
		active = bundle.getBoolean( SKILL_ACTIVE + " " + tag );
	}
}

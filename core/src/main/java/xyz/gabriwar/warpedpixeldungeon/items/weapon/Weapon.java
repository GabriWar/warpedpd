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

package xyz.gabriwar.warpedpixeldungeon.items.weapon;

import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Hunting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Holy;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vicious;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Berserk;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicImmune;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WeaponEnhance;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.BodyForm;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyWeapon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.Smite;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.MirrorImage;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfArcana;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfFuror;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ParchmentScrap;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ShardOfOblivion;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Dazzling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Displacing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Explosive;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Friendly;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Polarized;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Sacrificial;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Wayward;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blooming;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Corrupting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Elastic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Horror;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Luck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Slashing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Unstable;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Surging;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Parasitic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RunicBlade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Scimitar;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.TrueRunicBlade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;

abstract public class Weapon extends KindOfWeapon {

	public float    ACC = 1f;	// Accuracy modifier
	public float	DLY	= 1f;	// Speed modifier
	public int      RCH = 1;    // Reach modifier (only applies to melee hits)

	public enum Augment {
		SPEED   (0.7f, 2/3f),
		DAMAGE  (1.5f, 5/3f),
		NONE	(1.0f, 1f);

		private float damageFactor;
		private float delayFactor;

		Augment(float dmg, float dly){
			damageFactor = dmg;
			delayFactor = dly;
		}

		public int damageFactor(int dmg){
			return Math.round(dmg * damageFactor);
		}

		public float damageFactor(float dmg){
			return dmg * damageFactor;
		}

		public float delayFactor(float dly){
			return dly * delayFactor;
		}
	}
	
	public Augment augment = Augment.NONE;

	protected int usesToID(){
		return 20;
	}
	protected float usesLeftToID = usesToID();
	protected float availableUsesToID = usesToID()/2f;
	
	//weapons can carry up to two enchantments. the primary slot is filled by drops,
	//scrolls and infusions as always; the secondary slot only ever comes from an
	//enchanting station. invariant: enchantment2 is null unless enchantment is set.
	public Enchantment enchantment;
	public Enchantment enchantment2;
	public boolean enchantHardened = false;
	public boolean curseInfusionBonus = false;
	public boolean masteryPotionBonus = false;
	
	@Override
	public int proc( Char attacker, Char defender, int damage ) {

		boolean becameAlly = false;
		boolean wasAlly = defender.alignment == Char.Alignment.ALLY;
		if (attacker.buff(MagicImmune.class) == null) {
			Enchantment trinityEnchant = null;
			//only when it's the hero or a char that uses the hero's weapon
			if (Dungeon.hero.buff(BodyForm.BodyFormBuff.class) != null && this instanceof MeleeWeapon
					&& (attacker == Dungeon.hero || attacker instanceof MirrorImage || attacker instanceof ShadowClone.ShadowAlly)){
				trinityEnchant = Dungeon.hero.buff(BodyForm.BodyFormBuff.class).enchant();
				if (enchantment != null && trinityEnchant != null && trinityEnchant.getClass() == enchantment.getClass()){
					trinityEnchant = null;
				}
				if (enchantment2 != null && trinityEnchant != null && trinityEnchant.getClass() == enchantment2.getClass()){
					trinityEnchant = null;
				}
			}

			if (attacker instanceof Hero && isEquipped((Hero) attacker)
					&& attacker.buff(HolyWeapon.HolyWepBuff.class) != null){
				if (enchantment != null &&
						(((Hero) attacker).subClass == HeroSubClass.PALADIN || enchantment.curse())){
					damage = enchantment.proc(this, attacker, defender, damage);
					if (defender.alignment == Char.Alignment.ALLY && !wasAlly){
						becameAlly = true;
					}
				}
				if (defender.isAlive() && !becameAlly && enchantment2 != null &&
						(((Hero) attacker).subClass == HeroSubClass.PALADIN || enchantment2.curse())){
					damage = enchantment2.proc(this, attacker, defender, damage);
					if (defender.alignment == Char.Alignment.ALLY && !wasAlly){
						becameAlly = true;
					}
				}
				if (defender.isAlive() && !becameAlly && trinityEnchant != null){
					damage = trinityEnchant.proc(this, attacker, defender, damage);
				}
				if (defender.isAlive() && !becameAlly) {
					int dmg = ((Hero) attacker).subClass == HeroSubClass.PALADIN ? 6 : 2;
					defender.damage(Math.round(dmg * Enchantment.genericProcChanceMultiplier(attacker)), HolyWeapon.INSTANCE);
				}

			} else {
				if (enchantment != null) {
					damage = enchantment.proc(this, attacker, defender, damage);
					if (defender.alignment == Char.Alignment.ALLY && !wasAlly) {
						becameAlly = true;
					}
				}

				if (defender.isAlive() && !becameAlly && enchantment2 != null) {
					damage = enchantment2.proc(this, attacker, defender, damage);
					if (defender.alignment == Char.Alignment.ALLY && !wasAlly) {
						becameAlly = true;
					}
				}

				if (defender.isAlive() && !becameAlly && trinityEnchant != null){
					damage = trinityEnchant.proc(this, attacker, defender, damage);
				}
			}

			if (attacker instanceof Hero && isEquipped((Hero) attacker) &&
					attacker.buff(Smite.SmiteTracker.class) != null && !becameAlly){
				defender.damage(Smite.bonusDmg((Hero) attacker, defender), Smite.INSTANCE);
			}
		}

		//do not progress toward ID in the specific case of a missile weapon with no parent using
		// up it's last shot, as in this case there's nothing left to ID anyway
		if (this instanceof MissileWeapon
				&& ((MissileWeapon) this).durabilityLeft() <= ((MissileWeapon) this).durabilityPerUse()
				&& ((MissileWeapon) this).parent == null){
			return damage;
		}
		
		if (!levelKnown && attacker == Dungeon.hero) {
			float uses = Math.min( availableUsesToID, Talent.itemIDSpeedFactor(Dungeon.hero, this) );
			availableUsesToID -= uses;
			usesLeftToID -= uses;
			if (usesLeftToID <= 0) {
				if (ShardOfOblivion.passiveIDDisabled()){
					if (usesLeftToID > -1){
						GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready"), name());
					}
					setIDReady();
				} else {
					identify();
					GLog.p(Messages.get(Weapon.class, "identify"));
					Badges.validateItemLevelAquired(this);
				}
			}
		}

		return damage;
	}
	
	public void onHeroGainExp( float levelPercent, Hero hero ){
		levelPercent *= Talent.itemIDSpeedFactor(hero, this);
		if (!levelKnown && (isEquipped(hero) || this instanceof MissileWeapon)
				&& availableUsesToID <= usesToID()/2f) {
			//gains enough uses to ID over 0.5 levels
			availableUsesToID = Math.min(usesToID()/2f, availableUsesToID + levelPercent * usesToID());
		}
	}
	
	private static final String USES_LEFT_TO_ID = "uses_left_to_id";
	private static final String AVAILABLE_USES  = "available_uses";
	private static final String ENCHANTMENT	    = "enchantment";
	private static final String ENCHANTMENT_2  = "enchantment2";
	private static final String PENDING_WEAVE   = "pending_weave_opts";
	private static final String PENDING_REROLL  = "pending_reroll_opts";
	private static final String PENDING_REROLL_SLOT = "pending_reroll_slot";
	private static final String ENCHANT_HARDENED = "enchant_hardened";
	private static final String CURSE_INFUSION_BONUS = "curse_infusion_bonus";
	private static final String MASTERY_POTION_BONUS = "mastery_potion_bonus";
	private static final String AUGMENT	        = "augment";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( USES_LEFT_TO_ID, usesLeftToID );
		bundle.put( AVAILABLE_USES, availableUsesToID );
		bundle.put( ENCHANTMENT, enchantment );
		bundle.put( ENCHANTMENT_2, enchantment2 );
		if (pendingWeaveOpts != null)  bundle.put( PENDING_WEAVE, java.util.Arrays.asList( pendingWeaveOpts ) );
		if (pendingRerollOpts != null) bundle.put( PENDING_REROLL, java.util.Arrays.asList( pendingRerollOpts ) );
		bundle.put( PENDING_REROLL_SLOT, pendingRerollSlot );
		bundle.put( ENCHANT_HARDENED, enchantHardened );
		bundle.put( CURSE_INFUSION_BONUS, curseInfusionBonus );
		bundle.put( MASTERY_POTION_BONUS, masteryPotionBonus );
		bundle.put( AUGMENT, augment );
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		usesLeftToID = bundle.getFloat( USES_LEFT_TO_ID );
		availableUsesToID = bundle.getFloat( AVAILABLE_USES );
		enchantment = (Enchantment)bundle.get( ENCHANTMENT );
		enchantment2 = (Enchantment)bundle.get( ENCHANTMENT_2 );
		pendingWeaveOpts = restorePendingOpts( bundle, PENDING_WEAVE );
		pendingRerollOpts = restorePendingOpts( bundle, PENDING_REROLL );
		pendingRerollSlot = bundle.contains( PENDING_REROLL_SLOT ) ? bundle.getInt( PENDING_REROLL_SLOT ) : -1;
		enchantHardened = bundle.getBoolean( ENCHANT_HARDENED );
		curseInfusionBonus = bundle.getBoolean( CURSE_INFUSION_BONUS );
		masteryPotionBonus = bundle.getBoolean( MASTERY_POTION_BONUS );

		augment = bundle.getEnum(AUGMENT, Augment.class);
	}
	
	//options already rolled at an enchanting pedestal but not yet chosen.
	//persisted so cancelling (or reloading) can't fish for a better trio
	public Enchantment[] pendingWeaveOpts  = null;
	public Enchantment[] pendingRerollOpts = null;
	public int pendingRerollSlot = -1;

	private static Enchantment[] restorePendingOpts( Bundle bundle, String key ){
		if (!bundle.contains( key )) return null;
		java.util.ArrayList<Enchantment> opts = new java.util.ArrayList<>();
		for (com.watabou.utils.Bundlable b : bundle.getCollection( key )){
			opts.add( (Enchantment) b );
		}
		return opts.size() == 3 ? opts.toArray( new Enchantment[0] ) : null;
	}

	@Override
	public void reset() {
		super.reset();
		usesLeftToID = usesToID();
		availableUsesToID = usesToID()/2f;
	}

	@Override
	public boolean collect(Bag container) {
		if(super.collect(container)){
			if (Dungeon.hero != null && Dungeon.hero.isAlive() && isIdentified() && enchantment != null){
				Catalog.setSeen(enchantment.getClass());
				Statistics.itemTypesDiscovered.add(enchantment.getClass());
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Item identify(boolean byHero) {
		if (enchantment != null && byHero && Dungeon.hero != null && Dungeon.hero.isAlive()){
			Catalog.setSeen(enchantment.getClass());
			Statistics.itemTypesDiscovered.add(enchantment.getClass());
		}
		return super.identify(byHero);
	}

	public void setIDReady(){
		usesLeftToID = -1;
	}

	public boolean readyToIdentify(){
		return !isIdentified() && usesLeftToID <= 0;
	}
	
	@Override
	public float accuracyFactor(Char owner, Char target) {
		
		int encumbrance = 0;
		
		if( owner instanceof Hero ){
			encumbrance = STRReq() - ((Hero)owner).STR();
		}

		float ACC = this.ACC;

		if (owner.buff(Wayward.WaywardBuff.class) != null && enchantment instanceof Wayward){
			ACC /= 5;
		}

		return encumbrance > 0 ? (float)(ACC / Math.pow( 1.5, encumbrance )) : ACC;
	}
	
	@Override
	public float delayFactor( Char owner ) {
		return baseDelay(owner) * (1f/speedMultiplier(owner));
	}

	protected float baseDelay( Char owner ){
		float delay = augment.delayFactor(this.DLY);
		if (owner instanceof Hero) {
			int encumbrance = STRReq() - ((Hero)owner).STR();
			if (encumbrance > 0){
				delay *= Math.pow( 1.2, encumbrance );
			}
		}

		return delay;
	}

	protected float speedMultiplier(Char owner ){
		float multi = RingOfFuror.attackSpeedMultiplier(owner);

		if (owner.buff(Scimitar.SwordDance.class) != null){
			multi += 0.6f;
		}

		return multi;
	}

	@Override
	public int reachFactor(Char owner) {
		int reach = RCH;
		if (owner instanceof Hero && RingOfForce.fightingUnarmed((Hero) owner)){
			reach = 1; //brawlers stance benefits from enchantments, but not innate reach
			if (!RingOfForce.unarmedGetsWeaponEnchantment((Hero) owner)){
				return reach;
			}
		}
		if (owner instanceof Hero && owner.buff(AscendedForm.AscendBuff.class) != null){
			reach += 2;
		}
		if (hasEnchant(Projecting.class, owner)){
			int bonus = Math.round(Enchantment.genericProcChanceMultiplier(owner));
			//an empowered projecting reaches further still (+1 tile at level 3+)
			Enchantment proj = getEnchant(Projecting.class);
			if (proj != null && proj.level() >= 3) bonus += 1;
			return reach + bonus;
		} else {
			return reach;
		}
	}

	public int STRReq(){
		return STRReq(level());
	}

	public abstract int STRReq(int lvl);

	protected static int STRReq(int tier, int lvl){
		lvl = Math.max(0, lvl);

		//strength req decreases at +1,+3,+6,+10,etc.
		return (8 + tier * 2) - (int)(Math.sqrt(8 * lvl + 1) - 1)/2;
	}

	@Override
	public int level() {
		int level = super.level();
		if (curseInfusionBonus) level += 1 + level/6;
		return level;
	}

	@Override
	public int buffedLvl() {
		int lvl = super.buffedLvl();
		if (Dungeon.hero != null) {
			WeaponEnhance weaponEnhance = Dungeon.hero.buff(WeaponEnhance.class);
			if (weaponEnhance != null && isEquipped(Dungeon.hero)) {
				lvl = weaponEnhance.weaponLevel(lvl);
			}
		}
		return lvl;
	}

	@Override
	public Item upgrade() {
		return upgrade(false);
	}
	
	public Item upgrade(boolean enchant ) {

		if (enchant){
			if (enchantment == null){
				enchant(Enchantment.random());
			}
		} else if (enchantment != null) {
			//upgrades never strip enchantments in warped. curses still have
			//a static 33% chance to be cleansed
			if (hasCurseEnchant()) {
				if (Random.Int(3) == 0) removeCurseEnchants();
			}
		}
		
		cursed = false;

		return super.upgrade();
	}
	
	@Override
	public String name() {
		if (isEquipped(Dungeon.hero) && !hasCurseEnchant() && Dungeon.hero.buff(HolyWeapon.HolyWepBuff.class) != null
			&& (Dungeon.hero.subClass != HeroSubClass.PALADIN || enchantment == null)){
				return Messages.get(HolyWeapon.class, "ench_name", super.name());
			} else {
				//both enchantments prefix the name: "blazing shocking sword"
				String name = super.name();
				if (enchantment2 != null && (cursedKnown || !enchantment2.curse())){
					name = enchantment2.name( name );
				}
				if (enchantment != null && (cursedKnown || !enchantment.curse())){
					name = enchantment.name( name );
				}
				return name;
		}
	}
	
	@Override
	public Item random() {
		//+0: 75% (3/4)
		//+1: 20% (4/20)
		//+2: 5%  (1/20)
		int n = 0;
		if (Random.Int(4) == 0) {
			n++;
			if (Random.Int(5) == 0) {
				n++;
			}
		}
		level(n);

		//we use a separate RNG here so that variance due to things like parchment scrap
		//does not affect levelgen
		Random.pushGenerator(Random.Long());

			//30% chance to be cursed
			//10% chance to be enchanted
			float effectRoll = Random.Float();
			if (effectRoll < 0.3f * ParchmentScrap.curseChanceMultiplier()) {
				enchant(Enchantment.randomCurse());
				cursed = true;
			} else if (effectRoll >= 1f - (0.1f * ParchmentScrap.enchantChanceMultiplier())){
				enchant();
			}

		Random.popGenerator();

		return this;
	}
	
	public Weapon enchant( Enchantment ench ) {
		if (ench == null || !ench.curse()) curseInfusionBonus = false;
		enchantment = ench;
		if (ench == null){
			//a full disenchant (prize generation, curse cleansing) clears both slots
			enchantment2 = null;
		} else if (enchantment2 != null && enchantment2.getClass() == ench.getClass()){
			//never carry the same enchantment twice
			enchantment2 = null;
		}
		updateQuickslot();
		if (ench != null && isIdentified() && Dungeon.hero != null
				&& Dungeon.hero.isAlive() && Dungeon.hero.belongings.contains(this)){
			Catalog.setSeen(ench.getClass());
			Statistics.itemTypesDiscovered.add(ench.getClass());
		}
		return this;
	}

	public Weapon enchant() {

		Class<? extends Enchantment> oldEnchantment = enchantment != null ? enchantment.getClass() : null;
		Enchantment ench = Enchantment.random( oldEnchantment );

		return enchant( ench );
	}

	//places the enchantment into the first free slot; replaces the primary if both
	//are taken. used by the enchanting station to infuse a second enchantment.
	public Weapon addEnchant( Enchantment ench ){
		if (ench == null) return enchant( null );
		if (enchantment == null){
			return enchant( ench );
		}
		if (enchantment.getClass() == ench.getClass() || enchantment2 != null){
			//replace the primary (also handles both-slots-full)
			return enchant( ench );
		}
		enchantment2 = ench;
		updateQuickslot();
		if (isIdentified() && Dungeon.hero != null
				&& Dungeon.hero.isAlive() && Dungeon.hero.belongings.contains(this)){
			Catalog.setSeen(ench.getClass());
			Statistics.itemTypesDiscovered.add(ench.getClass());
		}
		return this;
	}

	//non-null enchantments, primary first
	public Enchantment[] enchantments(){
		if (enchantment != null && enchantment2 != null){
			return new Enchantment[]{ enchantment, enchantment2 };
		} else if (enchantment != null){
			return new Enchantment[]{ enchantment };
		} else if (enchantment2 != null){
			return new Enchantment[]{ enchantment2 };
		} else {
			return new Enchantment[0];
		}
	}

	//keeps the invariant that the secondary slot is only filled behind the primary
	protected void promoteEnchantSlots(){
		if (enchantment == null && enchantment2 != null){
			enchantment = enchantment2;
			enchantment2 = null;
		}
	}

	//strips cursed enchantments only, keeping any good ones (used by remove curse)
	public void removeCurseEnchants(){
		if (enchantment != null && enchantment.curse())   enchantment = null;
		if (enchantment2 != null && enchantment2.curse()) enchantment2 = null;
		curseInfusionBonus = false;
		promoteEnchantSlots();
		updateQuickslot();
	}

	public boolean hasEnchant(Class<?extends Enchantment> type, Char owner) {
		if (owner.buff(MagicImmune.class) != null) {
			return false;
		} else if (hasGoodEnchant()
				&& owner instanceof Hero
				&& isEquipped((Hero) owner)
				&& owner.buff(HolyWeapon.HolyWepBuff.class) != null
				&& ((Hero) owner).subClass != HeroSubClass.PALADIN) {
			return false;
		} else if (owner.buff(BodyForm.BodyFormBuff.class) != null
				&& owner.buff(BodyForm.BodyFormBuff.class).enchant() != null
				&& owner.buff(BodyForm.BodyFormBuff.class).enchant().getClass().equals(type)){
			return true;
		} else if (enchantment != null && enchantment.getClass() == type) {
			return true;
		} else if (enchantment2 != null && enchantment2.getClass() == type) {
			return true;
		} else {
			return false;
		}
	}

	//returns the matching enchantment instance on this weapon, or null
	public Enchantment getEnchant(Class<?extends Enchantment> type){
		if (enchantment != null && enchantment.getClass() == type)   return enchantment;
		if (enchantment2 != null && enchantment2.getClass() == type) return enchantment2;
		return null;
	}

	//these are not used to process specific enchant effects, so magic immune doesn't affect them
	public boolean hasGoodEnchant(){
		return (enchantment != null && !enchantment.curse())
				|| (enchantment2 != null && !enchantment2.curse());
	}

	public boolean hasCurseEnchant(){
		return (enchantment != null && enchantment.curse())
				|| (enchantment2 != null && enchantment2.curse());
	}

	private static ItemSprite.Glowing HOLY = new ItemSprite.Glowing( 0xFFFF00 );

	@Override
	public ItemSprite.Glowing glowing() {
		if (isEquipped(Dungeon.hero) && !hasCurseEnchant() && Dungeon.hero.buff(HolyWeapon.HolyWepBuff.class) != null
				&& (Dungeon.hero.subClass != HeroSubClass.PALADIN || enchantment == null)){
			return HOLY;
		} else {
			ItemSprite.Glowing g1 = enchantment != null && (cursedKnown || !enchantment.curse())
					? enchantment.glowing() : null;
			ItemSprite.Glowing g2 = enchantment2 != null && (cursedKnown || !enchantment2.curse())
					? enchantment2.glowing() : null;
			if (g1 != null && g2 != null){
				//with two enchantments the glow blends both colors
				int r = (((g1.color >> 16) & 0xFF) + ((g2.color >> 16) & 0xFF)) / 2;
				int g = (((g1.color >> 8)  & 0xFF) + ((g2.color >> 8)  & 0xFF)) / 2;
				int b = ((g1.color & 0xFF) + (g2.color & 0xFF)) / 2;
				return new ItemSprite.Glowing( (r << 16) | (g << 8) | b );
			}
			return g1 != null ? g1 : g2;
		}
	}

	public static abstract class Enchantment implements Bundlable {

		public static final Class<?>[] common = new Class<?>[]{
				Blazing.class, Chilling.class, Kinetic.class, Shocking.class};

		public static final Class<?>[] uncommon = new Class<?>[]{
				Blocking.class, Blooming.class, Elastic.class,
				Lucky.class, Projecting.class, Unstable.class,
				Horror.class, Luck.class, Paralysis.class, Poison.class, Slashing.class,
				Surging.class,
				//Unleashed PD ports
				Midas.class, Hunting.class, Holy.class};

		public static final Class<?>[] rare = new Class<?>[]{
				Corrupting.class, Grim.class, Vampiric.class, Parasitic.class,
				//Unleashed PD ports
				Vicious.class, Ancient.class};

		public static final float[] typeChances = new float[]{
				50, //12.5% each
				40, //6.67% each
				10  //3.33% each
		};

		public static final Class<?>[] curses = new Class<?>[]{
				Annoying.class, Displacing.class, Dazzling.class, Explosive.class,
				Sacrificial.class, Wayward.class, Polarized.class, Friendly.class
		};
		
			
		public abstract int proc( Weapon weapon, Char attacker, Char defender, int damage );

		//enchantments can be empowered at an enchanting station. levels raise both
		//proc frequency (via procChanceMultiplier) and each enchantment's own
		//effect magnitudes (via power(), applied per-enchantment).
		public static final int MAX_LEVEL = 5;

		private int enchLevel = 0;

		public int level(){
			return enchLevel;
		}

		public void level( int value ){
			enchLevel = Math.max( 0, Math.min( MAX_LEVEL, value ));
		}

		//standard magnitude scaling: +40% effect strength per level.
		//level 0 is exactly the vanilla enchantment; each level is a big step
		public float power(){
			return 1f + 0.4f * enchLevel;
		}

		protected float procChanceMultiplier( Char attacker ){
			//+20% proc rate per enchantment level
			return genericProcChanceMultiplier( attacker ) + 0.2f * enchLevel;
		}

		public static float genericProcChanceMultiplier( Char attacker ){
			float multi = RingOfArcana.enchantPowerMultiplier(attacker);
			Berserk rage = attacker.buff(Berserk.class);
			if (rage != null) {
				multi = rage.enchantFactor(multi);
			}

			if (attacker.buff(RunicBlade.RunicSlashTracker.class) != null){
				multi += attacker.buff(RunicBlade.RunicSlashTracker.class).boost;
				attacker.buff(RunicBlade.RunicSlashTracker.class).detach();
			}

			if (attacker.buff(Smite.SmiteTracker.class) != null){
				multi += 3f;
			}

			if (attacker.buff(TrueRunicBlade.TrueRunicSlashTracker.class) != null){
				multi += 3f;
				attacker.buff(TrueRunicBlade.TrueRunicSlashTracker.class).detach();
			}

			if (attacker.buff(ElementalStrike.DirectedPowerTracker.class) != null){
				multi += attacker.buff(ElementalStrike.DirectedPowerTracker.class).enchBoost;
				attacker.buff(ElementalStrike.DirectedPowerTracker.class).detach();
			}

			if (attacker.buff(Talent.SpiritBladesTracker.class) != null
					&& ((Hero)attacker).pointsInTalent(Talent.SPIRIT_BLADES) == 4){
				multi += 0.1f;
			}
			if (attacker.buff(Talent.StrikingWaveTracker.class) != null
					&& ((Hero)attacker).pointsInTalent(Talent.STRIKING_WAVE) == 4){
				multi += 0.2f;
			}

			return multi;
		}

		public String name() {
			if (!curse())
				return name( Messages.get(this, "enchant"));
			else
				return name( Messages.get(Item.class, "curse"));
		}

		public String name( String weaponName ) {
			return Messages.get(this, "name", weaponName);
		}

		public String desc() {
			return Messages.get(this, "desc") + levelDesc();
		}

		//appended to desc when the enchantment has been empowered
		protected String levelDesc() {
			if (enchLevel > 0){
				return "\n\n" + Messages.get(Enchantment.class, "level_desc", enchLevel);
			}
			return "";
		}

		public boolean curse() {
			return false;
		}

		private static final String ENCH_LEVEL = "ench_level";

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			enchLevel = bundle.getInt( ENCH_LEVEL );
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			bundle.put( ENCH_LEVEL, enchLevel );
		}
		
		public abstract ItemSprite.Glowing glowing();
		
		@SuppressWarnings("unchecked")
		public static Enchantment random( Class<? extends Enchantment> ... toIgnore ) {
			switch(Random.chances(typeChances)){
				case 0: default:
					return randomCommon( toIgnore );
				case 1:
					return randomUncommon( toIgnore );
				case 2:
					return randomRare( toIgnore );
			}
		}
		
		@SuppressWarnings("unchecked")
		public static Enchantment randomCommon( Class<? extends Enchantment> ... toIgnore ) {
			ArrayList<Class<?>> enchants = new ArrayList<>(Arrays.asList(common));
			enchants.removeAll(Arrays.asList(toIgnore));
			if (enchants.isEmpty()) {
				return random();
			} else {
				return (Enchantment) Reflection.newInstance(Random.element(enchants));
			}
		}
		
		@SuppressWarnings("unchecked")
		public static Enchantment randomUncommon( Class<? extends Enchantment> ... toIgnore ) {
			ArrayList<Class<?>> enchants = new ArrayList<>(Arrays.asList(uncommon));
			enchants.removeAll(Arrays.asList(toIgnore));
			if (enchants.isEmpty()) {
				return random();
			} else {
				return (Enchantment) Reflection.newInstance(Random.element(enchants));
			}
		}
		
		@SuppressWarnings("unchecked")
		public static Enchantment randomRare( Class<? extends Enchantment> ... toIgnore ) {
			ArrayList<Class<?>> enchants = new ArrayList<>(Arrays.asList(rare));
			enchants.removeAll(Arrays.asList(toIgnore));
			if (enchants.isEmpty()) {
				return random();
			} else {
				return (Enchantment) Reflection.newInstance(Random.element(enchants));
			}
		}

		@SuppressWarnings("unchecked")
		public static Enchantment randomCurse( Class<? extends Enchantment> ... toIgnore ){
			ArrayList<Class<?>> enchants = new ArrayList<>(Arrays.asList(curses));
			enchants.removeAll(Arrays.asList(toIgnore));
			if (enchants.isEmpty()) {
				return random();
			} else {
				return (Enchantment) Reflection.newInstance(Random.element(enchants));
			}
		}
		
	}
}

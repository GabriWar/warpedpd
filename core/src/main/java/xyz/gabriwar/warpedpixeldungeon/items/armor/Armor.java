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

package xyz.gabriwar.warpedpixeldungeon.items.armor;

import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Satisfying;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Mirrorimage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Afterimage;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ArmorEnhance;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicImmune;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Momentum;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Talent;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.AuraOfProtection;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.BodyForm;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HolyWard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.LifeLinkSpell;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PrismaticImage;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.BrokenSeal;
import xyz.gabriwar.warpedpixeldungeon.items.EquipableItem;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.AntiEntropy;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Bulk;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Corrosion;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Displacement;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Metabolism;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Multiplicity;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Overgrowth;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Stench;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Affection;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.AntiMagic;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Bounce;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Mirroring;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Phasing;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Camouflage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Entanglement;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Flow;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Obfuscation;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Potential;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Repulsion;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Stone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Swiftness;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Adaptation;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Crystal;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Siphoning;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Purity;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Viscosity;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfArcana;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ParchmentScrap;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.ShardOfOblivion;
import xyz.gabriwar.warpedpixeldungeon.journal.Catalog;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;

public class Armor extends EquipableItem {

	protected static final String AC_DETACH       = "DETACH";
	
	public enum Augment {
		EVASION (2f , -1f),
		DEFENSE (-2f, 1f),
		NONE	(0f   ,  0f);
		
		private float evasionFactor;
		private float defenceFactor;
		
		Augment(float eva, float df){
			evasionFactor = eva;
			defenceFactor = df;
		}
		
		public int evasionFactor(int level){
			return Math.round((2 + level) * evasionFactor);
		}
		
		public int defenseFactor(int level){
			return Math.round((2 + level) * defenceFactor);
		}
	}
	
	public Augment augment = Augment.NONE;
	
	public Glyph glyph;
	//a second glyph slot, only ever filled behind the primary (see promoteGlyphSlots)
	public Glyph glyph2;
	public boolean glyphHardened = false;
	public boolean curseInfusionBonus = false;
	public boolean masteryPotionBonus = false;
	
	protected BrokenSeal seal;
	
	public int tier;
	
	private static final int USES_TO_ID = 10;
	private float usesLeftToID = USES_TO_ID;
	private float availableUsesToID = USES_TO_ID/2f;
	
	public Armor( int tier ) {
		this.tier = tier;
	}

	/**
	 * Comfort offset in °C: cloth traps warm air, giving a small ambient buffer.
	 * Does NOT apply to metal armors — their thermal behaviour is governed by thermalMass().
	 * Brimstone glyph adds warmth on top of the base offset.
	 */
	public float thermalOffset() {
		float base;
		switch (tier) {
			case 1:  base = 1.5f; break; // cloth   — trapped warm air
			case 2:  base = 0.5f; break; // leather — light insulation
			default: base = 0.0f; break; // metal armors: no offset, controlled by mass
		}
		Glyph brim = getGlyph(xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone.class);
		if (brim != null) {
			base += 2.0f * brim.power();
		}
		return base;
	}

	/**
	 * Thermal mass factor: multiplies the bodyTemp convergence rate.
	 * < 1 = slow convergence (high mass → retains temperature longer, resists change).
	 * > 1 = fast convergence (low mass → quickly takes on ambient temperature).
	 *
	 * Plate: very slow — stays warm if warm, stays cold if cold (thick metal + padding).
	 * Mail:  very fast — metal links conduct temperature rapidly in both directions.
	 * Cloth: moderate — thin and responsive.
	 */
	public float thermalMass() {
		switch (tier) {
			case 1:  return 1.0f;  // cloth   — thin, responds fairly quickly
			case 2:  return 0.85f; // leather — slightly dampened
			case 3:  return 1.40f; // mail    — open links, conducts fast
			case 4:  return 0.70f; // scale   — heavy, moderately slow
			case 5:  return 0.40f; // plate   — very high mass, retains temp strongly
			default: return 1.0f;
		}
	}
	
	private static final String USES_LEFT_TO_ID = "uses_left_to_id";
	private static final String AVAILABLE_USES  = "available_uses";
	private static final String GLYPH			= "glyph";
	private static final String GLYPH_2			= "glyph2";
	private static final String GLYPH_HARDENED	= "glyph_hardened";
	private static final String PENDING_WEAVE   = "pending_weave_opts";
	private static final String PENDING_REROLL  = "pending_reroll_opts";
	private static final String PENDING_REROLL_SLOT = "pending_reroll_slot";
	private static final String CURSE_INFUSION_BONUS = "curse_infusion_bonus";
	private static final String MASTERY_POTION_BONUS = "mastery_potion_bonus";
	private static final String SEAL            = "seal";
	private static final String AUGMENT			= "augment";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( USES_LEFT_TO_ID, usesLeftToID );
		bundle.put( AVAILABLE_USES, availableUsesToID );
		bundle.put( GLYPH, glyph );
		bundle.put( GLYPH_2, glyph2 );
		if (pendingWeaveOpts != null)  bundle.put( PENDING_WEAVE, java.util.Arrays.asList( pendingWeaveOpts ) );
		if (pendingRerollOpts != null) bundle.put( PENDING_REROLL, java.util.Arrays.asList( pendingRerollOpts ) );
		bundle.put( PENDING_REROLL_SLOT, pendingRerollSlot );
		bundle.put( GLYPH_HARDENED, glyphHardened );
		bundle.put( CURSE_INFUSION_BONUS, curseInfusionBonus );
		bundle.put( MASTERY_POTION_BONUS, masteryPotionBonus );
		bundle.put( SEAL, seal);
		bundle.put( AUGMENT, augment);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		usesLeftToID = bundle.getInt( USES_LEFT_TO_ID );
		availableUsesToID = bundle.getInt( AVAILABLE_USES );
		inscribe((Glyph) bundle.get(GLYPH));
		glyph2 = (Glyph) bundle.get(GLYPH_2);
		if (glyph2 != null && (glyph == null || glyph2.getClass() == glyph.getClass())){
			promoteGlyphSlots();
		}
		pendingWeaveOpts = restorePendingOpts( bundle, PENDING_WEAVE );
		pendingRerollOpts = restorePendingOpts( bundle, PENDING_REROLL );
		pendingRerollSlot = bundle.contains( PENDING_REROLL_SLOT ) ? bundle.getInt( PENDING_REROLL_SLOT ) : -1;
		glyphHardened = bundle.getBoolean(GLYPH_HARDENED);
		curseInfusionBonus = bundle.getBoolean( CURSE_INFUSION_BONUS );
		masteryPotionBonus = bundle.getBoolean( MASTERY_POTION_BONUS );
		seal = (BrokenSeal)bundle.get(SEAL);
		
		augment = bundle.getEnum(AUGMENT, Augment.class);
	}

	@Override
	public void reset() {
		super.reset();
		usesLeftToID = USES_TO_ID;
		availableUsesToID = USES_TO_ID/2f;
		//armor can be kept in bones between runs, the seal cannot.
		seal = null;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (seal != null) actions.add(AC_DETACH);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_DETACH) && seal != null){
			BrokenSeal detaching = detachSeal();
			GLog.i( Messages.get(Armor.class, "detach_seal") );
			hero.sprite.operate(hero.pos);
			if (!detaching.collect()){
				Dungeon.level.drop(detaching, hero.pos);
			}
			updateQuickslot();
		}
	}

	@Override
	public boolean collect(Bag container) {
		if(super.collect(container)){
			if (Dungeon.hero != null && Dungeon.hero.isAlive() && isIdentified() && glyph != null){
				Catalog.setSeen(glyph.getClass());
				Statistics.itemTypesDiscovered.add(glyph.getClass());
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Item identify(boolean byHero) {
		if (glyph != null && byHero && Dungeon.hero != null && Dungeon.hero.isAlive()){
			Catalog.setSeen(glyph.getClass());
			Statistics.itemTypesDiscovered.add(glyph.getClass());
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
	public boolean doEquip( Hero hero ) {

		// 15/25% chance
		if (hero.heroClass != HeroClass.CLERIC && hero.hasTalent(Talent.HOLY_INTUITION)
				&& cursed && !cursedKnown
				&& Random.Int(20) < 1 + 2*hero.pointsInTalent(Talent.HOLY_INTUITION)){
			cursedKnown = true;
			GLog.p(Messages.get(this, "curse_detected"));
			return false;
		}

		detach(hero.belongings.backpack);

		Armor oldArmor = hero.belongings.armor;
		if (hero.belongings.armor == null || hero.belongings.armor.doUnequip( hero, true, false )) {
			
			hero.belongings.armor = this;
			
			cursedKnown = true;
			if (cursed) {
				equipCursed( hero );
				GLog.n( Messages.get(Armor.class, "equip_cursed") );
			}
			
			((HeroSprite)hero.sprite).updateArmor();
			activate(hero);
			Talent.onItemEquipped(hero, this);
			hero.spend( timeToEquip( hero ) );

			if (Dungeon.hero.heroClass == HeroClass.WARRIOR && checkSeal() == null){
				BrokenSeal seal = oldArmor != null ? oldArmor.checkSeal() : null;
				if (seal != null && (!cursed || (seal.getGlyph() != null && seal.getGlyph().curse()))){

					GameScene.show(new WndOptions(new ItemSprite(ItemSpriteSheet.SEAL),
							Messages.titleCase(seal.title()),
							Messages.get(Armor.class, "seal_transfer"),
							Messages.get(Armor.class, "seal_transfer_yes"),
							Messages.get(Armor.class, "seal_transfer_no")){
						@Override
						protected void onSelect(int index) {
							super.onSelect(index);
							if (index == 0){
								seal.affixToArmor(Armor.this, oldArmor);
								updateQuickslot();
							}
							super.hide();
						}

						@Override
						public void hide() {
							//do nothing, must press button
						}
					});
				} else {
					hero.next();
				}
			} else {
				hero.next();
			}
			return true;
			
		} else {
			
			collect( hero.belongings.backpack );
			return false;
			
		}
	}

	@Override
	public void activate(Char ch) {
		if (seal != null) Buff.affect(ch, BrokenSeal.WarriorShield.class).setArmor(this);
	}

	public void affixSeal(BrokenSeal seal){
		this.seal = seal;
		if (seal.level() > 0){
			//doesn't trigger upgrading logic such as affecting curses/glyphs
			int newLevel = trueLevel()+1;
			level(newLevel);
			Badges.validateItemLevelAquired(this);
		}
		if (seal.getGlyph() != null){
			inscribe(seal.getGlyph());
		}
		if (isEquipped(Dungeon.hero)){
			Buff.affect(Dungeon.hero, BrokenSeal.WarriorShield.class).setArmor(this);
		}
	}

	public BrokenSeal detachSeal(){
		if (seal != null){

			if (isEquipped(Dungeon.hero)) {
				BrokenSeal.WarriorShield sealBuff = Dungeon.hero.buff(BrokenSeal.WarriorShield.class);
				if (sealBuff != null) sealBuff.setArmor(null);
			}

			BrokenSeal detaching = seal;
			seal = null;

			if (detaching.level() > 0){
				degrade();
			}
			if (detaching.canTransferGlyph()){
				inscribe(null);
			} else {
				detaching.setGlyph(null);
			}
			return detaching;
		} else {
			return null;
		}
	}

	public BrokenSeal checkSeal(){
		return seal;
	}

	@Override
	public boolean doUnequip( Hero hero, boolean collect, boolean single ) {
		if (super.doUnequip( hero, collect, single )) {

			hero.belongings.armor = null;
			((HeroSprite)hero.sprite).updateArmor();

			BrokenSeal.WarriorShield sealBuff = hero.buff(BrokenSeal.WarriorShield.class);
			if (sealBuff != null) sealBuff.setArmor(null);

			return true;

		} else {

			return false;

		}
	}
	
	@Override
	public boolean isEquipped( Hero hero ) {
		return hero != null && hero.belongings.armor() == this;
	}

	public final int DRMax(){
		return DRMax(buffedLvl());
	}

	public int DRMax(int lvl){
		if (Dungeon.isChallenged(Challenges.NO_ARMOR)){
			return 1 + tier + lvl + augment.defenseFactor(lvl);
		}

		int upgradefactor = tier;
		if (Dungeon.hero != null && hasGlyph(Afterimage.class, Dungeon.hero)) {
			upgradefactor --;
		}

		int max = upgradefactor * (2 + lvl) + augment.defenseFactor(lvl);
		if (lvl > max){
			return ((lvl - max)+1)/2;
		} else {
			return max;
		}
	}

	public final int DRMin(){
		return DRMin(buffedLvl());
	}

	public int DRMin(int lvl){
		if (Dungeon.isChallenged(Challenges.NO_ARMOR)){
			return 0;
		}

		int max = DRMax(lvl);
		if (lvl >= max){
			return (lvl - max);
		} else {
			return lvl;
		}
	}

	//This exists so we can test what a char's base evasion would be without armor affecting it
	//more ugly static vars yaaay~
	public static boolean testingNoArmDefSkill = false;
	
	public float evasionFactor( Char owner, float evasion ){
		if (testingNoArmDefSkill) return evasion;
		
		if (hasGlyph(Stone.class, owner) && !Stone.testingEvasion()){
			return 0;
		}
		
		if (owner instanceof Hero){
			int aEnc = STRReq() - ((Hero) owner).STR();
			if (aEnc > 0) evasion /= Math.pow(1.5, aEnc);
			
			Momentum momentum = owner.buff(Momentum.class);
			if (momentum != null){
				evasion += momentum.evasionBonus(((Hero) owner).lvl, Math.max(0, -aEnc));
			}

			if (hasGlyph(Afterimage.class, owner)){
				evasion *= Math.pow(1.2f, this.buffedLvl());
			}
		}

		return evasion + augment.evasionFactor(buffedLvl());
	}
	
	public float speedFactor( Char owner, float speed ){
		
		if (owner instanceof Hero) {
			int aEnc = STRReq() - ((Hero) owner).STR();
			if (aEnc > 0) speed /= Math.pow(1.2, aEnc);
		}
		
		return speed;
		
	}
	
	@Override
	public int level() {
		int level = super.level();
		//TODO warrior's seal upgrade should probably be considered here too
		// instead of being part of true level
		if (curseInfusionBonus) level += 1 + level/6;
		return level;
	}

	@Override
	public int buffedLvl() {
		int lvl = super.buffedLvl();
		if (Dungeon.hero != null) {
			ArmorEnhance armorEnhance = Dungeon.hero.buff(ArmorEnhance.class);
			if (armorEnhance != null && isEquipped( Dungeon.hero )) {
				lvl = armorEnhance.armorLevel(lvl);
			}
		}
		return lvl;
	}

	@Override
	public Item upgrade() {
		return upgrade( false );
	}
	
	public Item upgrade( boolean inscribe ) {

		if (inscribe){
			if (glyph == null){
				inscribe( Glyph.random() );
			}
		} else if (glyph != null) {
			//upgrades never strip glyphs in warped. curses still have a
			//static 33% chance to be cleansed
			if (hasCurseGlyph()){
				if (Random.Int(3) == 0) removeCurseGlyphs();
			}
		}
		
		cursed = false;

		if (seal != null && seal.level() == 0)
			seal.upgrade();

		return super.upgrade();
	}
	
	public int proc( Char attacker, Char defender, int damage ) {

		if (defender.buff(MagicImmune.class) == null) {
			Glyph trinityGlyph = null;
			//only when it's the hero or a char that uses the hero's armor
			if (Dungeon.hero.buff(BodyForm.BodyFormBuff.class) != null
					&& (defender == Dungeon.hero || defender instanceof PrismaticImage || defender instanceof ShadowClone.ShadowAlly)){
				trinityGlyph = Dungeon.hero.buff(BodyForm.BodyFormBuff.class).glyph();
				if (glyph != null && trinityGlyph != null && trinityGlyph.getClass() == glyph.getClass()){
					trinityGlyph = null;
				}
			}

			if (defender instanceof Hero && isEquipped((Hero) defender)
					&& defender.buff(HolyWard.HolyArmBuff.class) != null){
				//holy ward suppresses glyphs unless the wearer is a paladin, but curses always proc
				if (glyph != null &&
						(((Hero) defender).subClass == HeroSubClass.PALADIN || glyph.curse())){
					damage = glyph.proc( this, attacker, defender, damage );
				}
				if (glyph2 != null &&
						(((Hero) defender).subClass == HeroSubClass.PALADIN || glyph2.curse())){
					damage = glyph2.proc( this, attacker, defender, damage );
				}
				if (trinityGlyph != null){
					damage = trinityGlyph.proc( this, attacker, defender, damage );
				}
				int blocking = ((Hero) defender).subClass == HeroSubClass.PALADIN ? 3 : 1;
				damage -= Math.round(blocking * Glyph.genericProcChanceMultiplier(defender));

			} else {
				if (glyph != null) {
					damage = glyph.proc(this, attacker, defender, damage);
				}
				if (glyph2 != null) {
					damage = glyph2.proc(this, attacker, defender, damage);
				}
				if (trinityGlyph != null){
					damage = trinityGlyph.proc( this, attacker, defender, damage );
				}
				//so that this effect procs for allies using this armor via aura of protection
				if (defender.alignment == Dungeon.hero.alignment
						&& Dungeon.hero.buff(AuraOfProtection.AuraBuff.class) != null
						&& (Dungeon.level.distance(defender.pos, Dungeon.hero.pos) <= 2 || defender.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null)
						&& Dungeon.hero.buff(HolyWard.HolyArmBuff.class) != null) {
					int blocking = Dungeon.hero.subClass == HeroSubClass.PALADIN ? 3 : 1;
					damage -= Math.round(blocking * Glyph.genericProcChanceMultiplier(defender));
				}
			}
			damage = Math.max(damage, 0);
		}
		
		if (!levelKnown && defender == Dungeon.hero) {
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
					GLog.p(Messages.get(Armor.class, "identify"));
					Badges.validateItemLevelAquired(this);
				}
			}
		}
		
		return damage;
	}
	
	@Override
	public void onHeroGainExp(float levelPercent, Hero hero) {
		levelPercent *= Talent.itemIDSpeedFactor(hero, this);
		if (!levelKnown && isEquipped(hero) && availableUsesToID <= USES_TO_ID/2f) {
			//gains enough uses to ID over 0.5 levels
			availableUsesToID = Math.min(USES_TO_ID/2f, availableUsesToID + levelPercent * USES_TO_ID);
		}
	}
	
	@Override
	public String name() {
		if (isEquipped(Dungeon.hero) && !hasCurseGlyph() && Dungeon.hero.buff(HolyWard.HolyArmBuff.class) != null
			&& (Dungeon.hero.subClass != HeroSubClass.PALADIN || glyph == null)){
				return Messages.get(HolyWard.class, "glyph_name", super.name());
			} else {
				String name = super.name();
				if (glyph != null && (cursedKnown || !glyph.curse()))   name = glyph.name( name );
				if (glyph2 != null && (cursedKnown || !glyph2.curse())) name = glyph2.name( name );
				return name;

		}
	}
	
	@Override
	public String info() {
		String info = super.info();
		
		if (levelKnown) {

			info += "\n\n" + Messages.get(Armor.class, "curr_absorb", tier, DRMin(), DRMax(), STRReq());
			
			if (Dungeon.hero != null && STRReq() > Dungeon.hero.STR()) {
				info += " " + Messages.get(Armor.class, "too_heavy");
			}
		} else {
			info += "\n\n" + Messages.get(Armor.class, "avg_absorb", tier, DRMin(0), DRMax(0), STRReq(0));

			if (Dungeon.hero != null && STRReq(0) > Dungeon.hero.STR()) {
				info += " " + Messages.get(Armor.class, "probably_too_heavy");
			}
		}

		switch (augment) {
			case EVASION:
				info += " " + Messages.get(Armor.class, "evasion");
				break;
			case DEFENSE:
				info += " " + Messages.get(Armor.class, "defense");
				break;
			case NONE:
		}

		//climate behaviour: warmth offset (includes brimstone) and heat retention
		float warmth = thermalOffset();
		if (warmth > 0){
			info += "\n\n" + Messages.get(Armor.class, "thermal_offset",
					new java.text.DecimalFormat("#.#").format(warmth));
		} else {
			info += "\n\n" + Messages.get(Armor.class, "thermal_none");
		}
		float mass = thermalMass();
		if (mass <= 0.45f) {
			info += " " + Messages.get(Armor.class, "thermal_retain_very");
		} else if (mass <= 0.75f) {
			info += " " + Messages.get(Armor.class, "thermal_retain");
		} else if (mass <= 1.05f) {
			info += " " + Messages.get(Armor.class, "thermal_neutral");
		} else {
			info += " " + Messages.get(Armor.class, "thermal_conduct");
		}

		if (isEquipped(Dungeon.hero) && !hasCurseGlyph() && Dungeon.hero.buff(HolyWard.HolyArmBuff.class) != null
				&& (Dungeon.hero.subClass != HeroSubClass.PALADIN || glyph == null)){
			info += "\n\n" + Messages.capitalize(Messages.get(Armor.class, "inscribed", Messages.get(HolyWard.class, "glyph_name", Messages.get(Glyph.class, "glyph"))));
			info += " " + Messages.get(HolyWard.class, "glyph_desc");
		} else if (glyph != null  && (cursedKnown || !glyph.curse())) {
			info += "\n\n" +  Messages.capitalize(Messages.get(Armor.class, "inscribed", glyph.name()));
			if (glyphHardened) info += " " + Messages.get(Armor.class, "glyph_hardened");
			info += " " + glyph.desc();
			if (glyph2 != null && (cursedKnown || !glyph2.curse())){
				info += "\n\n" + Messages.capitalize(Messages.get(Armor.class, "inscribed", glyph2.name()));
				info += " " + glyph2.desc();
			}
		} else if (glyphHardened){
			info += "\n\n" + Messages.get(Armor.class, "hardened_no_glyph");
		}
		
		if (cursed && isEquipped( Dungeon.hero )) {
			info += "\n\n" + Messages.get(Armor.class, "cursed_worn");
		} else if (cursedKnown && cursed) {
			info += "\n\n" + Messages.get(Armor.class, "cursed");
		} else if (!isIdentified() && cursedKnown){
			if (glyph != null && glyph.curse()) {
				info += "\n\n" + Messages.get(Armor.class, "weak_cursed");
			} else {
				info += "\n\n" + Messages.get(Armor.class, "not_cursed");
			}
		}

		if (seal != null) {
			info += "\n\n" + Messages.get(Armor.class, "seal_attached", seal.maxShield(tier, level()));
		}
		
		return info;
	}

	@Override
	public Emitter emitter() {
		if (seal == null) return super.emitter();
		Emitter emitter = new Emitter();
		emitter.pos(ItemSpriteSheet.film.width(image)/2f + 2f, ItemSpriteSheet.film.height(image)/3f);
		emitter.fillTarget = false;
		emitter.pour(Speck.factory( Speck.RED_LIGHT ), 0.6f);
		return emitter;
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
			//15% chance to be inscribed
			float effectRoll = Random.Float();
			if (effectRoll < 0.3f * ParchmentScrap.curseChanceMultiplier()) {
				inscribe(Glyph.randomCurse());
				cursed = true;
			} else if (effectRoll >= 1f - (0.15f * ParchmentScrap.enchantChanceMultiplier())){
				inscribe();
			}

		Random.popGenerator();

		return this;
	}

	public int STRReq(){
		return STRReq(level());
	}

	public int STRReq(int lvl){
		int req = STRReq(tier, lvl);
		if (masteryPotionBonus){
			req -= 2;
		}
		return req;
	}

	protected static int STRReq(int tier, int lvl){
		lvl = Math.max(0, lvl);

		//strength req decreases at +1,+3,+6,+10,etc.
		return (8 + Math.round(tier * 2)) - (int)(Math.sqrt(8 * lvl + 1) - 1)/2;
	}
	
	@Override
	public int value() {
		if (seal != null) return 0;

		int price = 20 * tier;
		if (hasGoodGlyph()) {
			price *= 1.5;
		}
		if (cursedKnown && (cursed || hasCurseGlyph())) {
			price /= 2;
		}
		if (levelKnown && level() > 0) {
			price *= (level() + 1);
		}
		if (price < 1) {
			price = 1;
		}
		return price;
	}

	public Armor inscribe( Glyph glyph ) {
		if (glyph == null || !glyph.curse()) curseInfusionBonus = false;
		this.glyph = glyph;
		if (glyph == null){
			//a full uninscribe (curse cleansing, prize generation) clears both slots
			glyph2 = null;
		} else if (glyph2 != null && glyph2.getClass() == glyph.getClass()){
			//never carry the same glyph twice
			glyph2 = null;
		}
		updateQuickslot();
		//the hero needs runic transference to actually transfer, but we still attach the glyph here
		// in case they take that talent in the future
		if (seal != null){
			seal.setGlyph(glyph);
		}
		if (glyph != null && isIdentified() && Dungeon.hero != null
				&& Dungeon.hero.isAlive() && Dungeon.hero.belongings.contains(this)){
			Catalog.setSeen(glyph.getClass());
			Statistics.itemTypesDiscovered.add(glyph.getClass());
		}
		return this;
	}

	public Armor inscribe() {

		Class<? extends Glyph> oldGlyphClass = glyph != null ? glyph.getClass() : null;
		Glyph gl = Glyph.random( oldGlyphClass );

		return inscribe( gl );
	}

	//places the glyph into the first free slot; replaces the primary if both
	//are taken. used by the enchanting pedestal to weave a second glyph.
	public Armor addGlyph( Glyph gl ){
		if (gl == null) return inscribe( null );
		if (glyph == null){
			return inscribe( gl );
		}
		if (glyph.getClass() == gl.getClass() || glyph2 != null){
			//replace the primary (also handles both-slots-full)
			return inscribe( gl );
		}
		glyph2 = gl;
		updateQuickslot();
		if (isIdentified() && Dungeon.hero != null
				&& Dungeon.hero.isAlive() && Dungeon.hero.belongings.contains(this)){
			Catalog.setSeen(gl.getClass());
			Statistics.itemTypesDiscovered.add(gl.getClass());
		}
		return this;
	}

	//non-null glyphs, primary first
	public Glyph[] glyphs(){
		if (glyph != null && glyph2 != null){
			return new Glyph[]{ glyph, glyph2 };
		} else if (glyph != null){
			return new Glyph[]{ glyph };
		} else if (glyph2 != null){
			return new Glyph[]{ glyph2 };
		} else {
			return new Glyph[0];
		}
	}

	//keeps the invariant that the secondary slot is only filled behind the primary
	protected void promoteGlyphSlots(){
		if (glyph == null && glyph2 != null){
			glyph = glyph2;
			glyph2 = null;
		}
	}

	//strips cursed glyphs only, keeping any good ones (used by remove curse)
	public void removeCurseGlyphs(){
		if (glyph != null && glyph.curse())   glyph = null;
		if (glyph2 != null && glyph2.curse()) glyph2 = null;
		curseInfusionBonus = false;
		promoteGlyphSlots();
		if (seal != null) seal.setGlyph(glyph);
		updateQuickslot();
	}

	//returns the matching glyph instance on this armor, or null
	public Glyph getGlyph(Class<?extends Glyph> type){
		if (glyph != null && glyph.getClass() == type)   return glyph;
		if (glyph2 != null && glyph2.getClass() == type) return glyph2;
		return null;
	}

	public boolean hasGlyph(Class<?extends Glyph> type, Char owner) {
		if (owner.buff(MagicImmune.class) != null) {
			return false;
		} else if (hasGoodGlyph()
				&& owner instanceof Hero
				&& isEquipped((Hero) owner)
				&& owner.buff(HolyWard.HolyArmBuff.class) != null
				&& ((Hero) owner).subClass != HeroSubClass.PALADIN){
			return false;
		} else if (owner.buff(BodyForm.BodyFormBuff.class) != null
				&& owner.buff(BodyForm.BodyFormBuff.class).glyph() != null
				&& owner.buff(BodyForm.BodyFormBuff.class).glyph().getClass().equals(type)){
			return true;
		} else if (glyph != null && glyph.getClass() == type) {
			return true;
		} else if (glyph2 != null && glyph2.getClass() == type) {
			return true;
		} else {
			return false;
		}
	}

	//these are not used to process specific glyph effects, so magic immune doesn't affect them
	public boolean hasGoodGlyph(){
		return (glyph != null && !glyph.curse())
				|| (glyph2 != null && !glyph2.curse());
	}

	public boolean hasCurseGlyph(){
		return (glyph != null && glyph.curse())
				|| (glyph2 != null && glyph2.curse());
	}

	//options already rolled at an enchanting pedestal but not yet chosen.
	//persisted so cancelling (or reloading) can't fish for a better trio
	public Glyph[] pendingWeaveOpts  = null;
	public Glyph[] pendingRerollOpts = null;
	public int pendingRerollSlot = -1;

	private static Glyph[] restorePendingOpts( Bundle bundle, String key ){
		if (!bundle.contains( key )) return null;
		java.util.ArrayList<Glyph> opts = new java.util.ArrayList<>();
		for (com.watabou.utils.Bundlable b : bundle.getCollection( key )){
			opts.add( (Glyph) b );
		}
		return opts.size() == 3 ? opts.toArray( new Glyph[0] ) : null;
	}

	private static ItemSprite.Glowing HOLY = new ItemSprite.Glowing( 0xFFFF00 );

	@Override
	public ItemSprite.Glowing glowing() {
		if (isEquipped(Dungeon.hero) && !hasCurseGlyph() && Dungeon.hero.buff(HolyWard.HolyArmBuff.class) != null
				&& (Dungeon.hero.subClass != HeroSubClass.PALADIN || glyph == null)){
			return HOLY;
		} else {
			ItemSprite.Glowing g1 = glyph != null && (cursedKnown || !glyph.curse())
					? glyph.glowing() : null;
			ItemSprite.Glowing g2 = glyph2 != null && (cursedKnown || !glyph2.curse())
					? glyph2.glowing() : null;
			if (g1 != null && g2 != null){
				//with two glyphs the glow blends both colors
				int r = (((g1.color >> 16) & 0xFF) + ((g2.color >> 16) & 0xFF)) / 2;
				int g = (((g1.color >> 8)  & 0xFF) + ((g2.color >> 8)  & 0xFF)) / 2;
				int b = ((g1.color & 0xFF) + (g2.color & 0xFF)) / 2;
				return new ItemSprite.Glowing( (r << 16) | (g << 8) | b );
			}
			return g1 != null ? g1 : g2;
		}
	}
	
	public static abstract class Glyph implements Bundlable {
		
		public static final Class<?>[] common = new Class<?>[]{
				Obfuscation.class, Swiftness.class, Viscosity.class, Potential.class,
				Mirrorimage.class };

		public static final Class<?>[] uncommon = new Class<?>[]{
				Brimstone.class, Stone.class, Entanglement.class,
				Repulsion.class, Camouflage.class, Flow.class,
				Bounce.class,
				Phasing.class, Mirroring.class,
				Adaptation.class, Crystal.class,
				Afterimage.class };

		public static final Class<?>[] rare = new Class<?>[]{
				Affection.class, AntiMagic.class, Thorns.class,
				Siphoning.class, Purity.class,
				Satisfying.class };

		public static final float[] typeChances = new float[]{
				50, //12.5% each
				40, //6.67% each
				10  //3.33% each
		};

		public static final Class<?>[] curses = new Class<?>[]{
				AntiEntropy.class, Corrosion.class, Displacement.class, Metabolism.class,
				Multiplicity.class, Stench.class, Overgrowth.class, Bulk.class
		};
		
		public abstract int proc( Armor armor, Char attacker, Char defender, int damage );

		//glyphs can be empowered at an enchanting pedestal. levels raise both
		//proc frequency (via procChanceMultiplier) and each glyph's own
		//effect magnitudes (via power(), applied per-glyph).
		public static final int MAX_LEVEL = 5;

		private int enchLevel = 0;

		public int level(){
			return enchLevel;
		}

		public void level( int value ){
			enchLevel = Math.max( 0, Math.min( MAX_LEVEL, value ));
		}

		//standard magnitude scaling: +40% effect strength per level.
		//level 0 is exactly the vanilla glyph; each level is a big step
		public float power(){
			return 1f + 0.4f * enchLevel;
		}

		protected float procChanceMultiplier( Char defender ){
			//+20% proc rate per glyph level
			return genericProcChanceMultiplier( defender ) + 0.2f * enchLevel;
		}

		public static float genericProcChanceMultiplier( Char defender ){
			float multi = RingOfArcana.enchantPowerMultiplier(defender);

			if (Dungeon.hero.alignment == defender.alignment
					&& Dungeon.hero.buff(AuraOfProtection.AuraBuff.class) != null
					&& (Dungeon.level.distance(defender.pos, Dungeon.hero.pos) <= 2 || defender.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null)){
				multi += 0.25f + 0.25f*Dungeon.hero.pointsInTalent(Talent.AURA_OF_PROTECTION);
			}

			return multi;
		}
		
		public String name() {
			if (!curse())
				return name( Messages.get(this, "glyph") );
			else
				return name( Messages.get(Item.class, "curse"));
		}
		
		public String name( String armorName ) {
			return Messages.get(this, "name", armorName);
		}

		public String desc() {
			return Messages.get(this, "desc") + levelDesc();
		}

		//appended to desc when the glyph has been empowered
		protected String levelDesc() {
			if (enchLevel > 0){
				return "\n\n" + Messages.get(Glyph.class, "level_desc", enchLevel);
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
		public static Glyph random( Class<? extends Glyph> ... toIgnore ) {
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
		public static Glyph randomCommon( Class<? extends Glyph> ... toIgnore ){
			ArrayList<Class<?>> glyphs = new ArrayList<>(Arrays.asList(common));
			glyphs.removeAll(Arrays.asList(toIgnore));
			if (glyphs.isEmpty()) {
				return random();
			} else {
				return (Glyph) Reflection.newInstance(Random.element(glyphs));
			}
		}
		
		@SuppressWarnings("unchecked")
		public static Glyph randomUncommon( Class<? extends Glyph> ... toIgnore ){
			ArrayList<Class<?>> glyphs = new ArrayList<>(Arrays.asList(uncommon));
			glyphs.removeAll(Arrays.asList(toIgnore));
			if (glyphs.isEmpty()) {
				return random();
			} else {
				return (Glyph) Reflection.newInstance(Random.element(glyphs));
			}
		}
		
		@SuppressWarnings("unchecked")
		public static Glyph randomRare( Class<? extends Glyph> ... toIgnore ){
			ArrayList<Class<?>> glyphs = new ArrayList<>(Arrays.asList(rare));
			glyphs.removeAll(Arrays.asList(toIgnore));
			if (glyphs.isEmpty()) {
				return random();
			} else {
				return (Glyph) Reflection.newInstance(Random.element(glyphs));
			}
		}
		
		@SuppressWarnings("unchecked")
		public static Glyph randomCurse( Class<? extends Glyph> ... toIgnore ){
			ArrayList<Class<?>> glyphs = new ArrayList<>(Arrays.asList(curses));
			glyphs.removeAll(Arrays.asList(toIgnore));
			if (glyphs.isEmpty()) {
				return random();
			} else {
				return (Glyph) Reflection.newInstance(Random.element(glyphs));
			}
		}
		
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.items.rarity;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClassArmor;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The rarity / type / masterwork state of one item. Lives in {@link Item#quality}, null
 * for items the system does not touch. Everything the rest of the game asks is a static
 * helper here, so the hooks in Weapon, Armor, Wand and Mob stay one line each.
 */
public class Quality implements Bundlable {

	public enum Family { WEAPON, ARMOR, MISSILE, WAND, RING, TRINKET, ARTIFACT }

	public static final int MASTERWORK_MAX = 10;
	//how many masterwork steps a common-to-rare base can take; the last needs legendary or exotic
	public static final int MASTERWORK_CAP_LOW = 9;

	public Rarity rarity = Rarity.COMMON;
	public ItemType type = ItemType.GAMMA;
	public int masterwork = 0;
	public int kills = 0;
	//the one-shot enchantment a legendary base is born with, so it is never handed twice
	public boolean granted = false;

	public ArrayList<RarityLine> lines = new ArrayList<>();
	public ArrayList<Float> rolls = new ArrayList<>();

	//weapon perk state: consecutive hits on one target (momentum, flurry), a free follow-up
	//shot (marksman); see WeaponPerk
	public int hitTarget = -1;
	public int hitCount = 0;
	public boolean freeNext = false;
	//throw (missile perk): the roll just made was a max-damage hit
	public boolean keenHit = false;
	//the turn a cooldown-gated alpha twist is ready again; not saved, resets on load
	public float twistReady = 0f;
	//the turn a fully-masterworked kill may pay out again, kept apart from twistReady so
	//a weapon's twist and its kill reward never share one cooldown
	public float killReady = 0f;

	//perk state that lives only for the run: ramps, banked amounts and the stamps of the
	//twists that need a memory. None of it is bundled - a load starts them clean, which is
	//what a ramp should do anyway
	public int swiftStacks = 0;
	public int dodgeStacks = 0;
	public float dodgeUntil = 0f;
	public int bankedBlock = 0;
	public Class<?> wardImmune = null;
	public float wardImmuneUntil = 0f;
	public boolean sureNext = false;
	public int assaultStacks = 0;
	public float assaultUntil = 0f;
	public int nextZapLevels = 0;
	public boolean throwSave = false;
	//CRUSH's armour shred, tracked on the one target it is eating through
	public int shredTarget = -1;
	public int shredStacks = 0;
	public float shredUntil = 0f;

	// ---------------------------------------------------------------- lookup

	public static Family family( Item item ){
		if (item instanceof MissileWeapon) return Family.MISSILE;
		//melee weapons and the guns and bows that extend Weapon directly
		if (item instanceof Weapon) return Family.WEAPON;
		if (item instanceof Armor && !(item instanceof ClassArmor)) return Family.ARMOR;
		if (item instanceof Wand) return Family.WAND;
		if (item instanceof xyz.gabriwar.warpedpixeldungeon.items.rings.Ring) return Family.RING;
		if (item instanceof xyz.gabriwar.warpedpixeldungeon.items.trinkets.Trinket
				&& !(item instanceof xyz.gabriwar.warpedpixeldungeon.items.trinkets.Trinket.PlaceHolder)) return Family.TRINKET;
		if (item instanceof xyz.gabriwar.warpedpixeldungeon.items.artifacts.Artifact) return Family.ARTIFACT;
		return null;
	}

	/** rarity tiers, bonus lines and masterwork are for weapons, armor and wands only;
	 *  rings, trinkets and artifacts carry just a type */
	public static boolean hasRarity( Item item ){
		Family f = family( item );
		return f == Family.WEAPON || f == Family.ARMOR || f == Family.MISSILE || f == Family.WAND;
	}

	public static boolean eligible( Item item ){
		return item != null && family( item ) != null;
	}

	/** the item's quality, or null when the item is outside the system */
	public static Quality of( Item item ){
		return item == null ? null : item.quality;
	}

	// ---------------------------------------------------------------- generation

	/** rolls rarity, type and lines for a freshly generated item; a no-op for items that
	 *  already carry a quality (loaded, copied) or are outside the system */
	public static void roll( Item item ){
		roll( item, null, null, 0 );
	}

	/** the same roll with any part forced (null = rolled by depth); used by the debug picker */
	public static void roll( Item item, Rarity rarity, ItemType type, int masterwork ){
		if (!eligible( item ) || item.quality != null) return;
		//a barrow rolls by the depth its floor counts as (Delves.lootDepth)
		int depth = Math.max( 1, xyz.gabriwar.warpedpixeldungeon.levels.Delves.lootDepth() );
		Quality q = new Quality();
		boolean tiered = hasRarity( item );
		q.rarity = !tiered ? Rarity.COMMON : rarity != null ? rarity : Rarity.roll( depth );
		q.type = type != null ? type : ItemType.roll( depth );
		if (tiered) q.rollLines( item );
		item.quality = q;
		q.onGenerated( item );
		for (int i = 0; tiered && i < masterwork; i++){
			if (q.masterworkCost() == 0) break;
			q.masterworkStep( item );
		}
		q.applyDerived( item );
	}

	private void rollLines( Item item ){
		lines.clear();
		rolls.clear();
		ArrayList<RarityLine> pool = RarityLine.pool( family( item ), item );
		Random.shuffle( pool );
		for (int i = 0; i < rarity.lines && i < pool.size(); i++){
			lines.add( pool.get( i ) );
			rolls.add( rarity.rollLine() );
		}
	}

	//stats that are stored on the item rather than computed each time
	private void applyDerived( Item item ){
		if (item instanceof Wand){
			Wand w = (Wand) item;
			w.maxCharges = Math.min( w.initialCharges() + w.level() + chargeBonus( w ), chargeCap() );
			w.curCharges = Math.min( w.curCharges, w.maxCharges );
		}
		empower( item );
	}

	/**
	 * A legendary or exotic item empowers whatever magic it carries, and keeps doing so as
	 * it is masterworked. Called again every time the magic itself changes - a fresh
	 * enchantment is born at level 0, so without this a re-enchant would quietly throw the
	 * tier's empowerment away. Never lowers what the player already built.
	 */
	public static void empower( Item item ){
		Quality q = of( item );
		if (q == null || q.rarity.ordinal() < Rarity.LEGENDARY.ordinal()) return;
		//one level of empowerment per whole rarity step of potency, counted down, so the
		//channel keeps growing as the item is masterworked instead of stalling at the top
		//tier: legendary 1 -> 2, exotic 2 -> 3, each gaining its level late in the work.
		//The small offset is there so an exact 3.0 cannot fall to 2 on float wobble
		int lv = (int) (q.potency() / Rarity.RARE.potency() + 0.1f);
		if (lv <= 0) return;
		//both slots: a weapon can carry two enchantments and armor two glyphs
		if (item instanceof Weapon){
			for (Weapon.Enchantment e : ((Weapon) item).enchantments()){
				if (e != null) e.level( Math.max( e.level(), lv ) );
			}
		} else if (item instanceof Armor){
			Armor a = (Armor) item;
			if (a.glyph != null) a.glyph.level( Math.max( a.glyph.level(), lv ) );
			if (a.glyph2 != null) a.glyph2.level( Math.max( a.glyph2.level(), lv ) );
		}
	}

	/**
	 * Fired exactly once, when the item is first rolled. A legendary or exotic weapon or
	 * armor comes out of the world already carrying magic - which is why this cannot live
	 * in applyDerived, that also runs on every masterwork step and every reroll, and would
	 * hand out a fresh enchantment each time or overwrite one the player chose.
	 */
	private void onGenerated( Item item ){
		if (granted || rarity.ordinal() < Rarity.LEGENDARY.ordinal()) return;
		granted = true;
		if (item instanceof Weapon && ((Weapon) item).enchantment == null){
			((Weapon) item).enchant();
		} else if (item instanceof Armor && ((Armor) item).glyph == null){
			((Armor) item).inscribe();
		}
	}

	// ---------------------------------------------------------------- line values

	/** effective roll of a line: each masterwork step closes a share of the gap left to the
	 *  line's maximum, so a step always pays - a floor did nothing to an already-high roll */
	public float roll( RarityLine kind ){
		int i = lines.indexOf( kind );
		if (i < 0) return 0f;
		float base = rolls.get( i );
		return base + (masterwork / (float) MASTERWORK_MAX) * rarity.closure() * (1f - base);
	}

	public boolean has( RarityLine kind ){
		return lines.contains( kind );
	}

	/** the bonus of a line: multiplier fraction, degrees for WARMTH, 0 when absent */
	public float value( RarityLine kind ){
		return has( kind ) ? kind.max() * roll( kind ) : 0f;
	}

	public static float value( Item item, RarityLine kind ){
		Quality q = of( item );
		return q == null ? 0f : q.value( kind );
	}

	/** the tier's own share of the item's number, grown by masterworking it */
	public float potency(){
		return rarity.potency() * (1f + 0.5f * masterwork / (float) MASTERWORK_MAX);
	}

	public static float potency( Item item ){
		Quality q = of( item );
		return q == null || !hasRarity( item ) ? 0f : q.potency();
	}

	/**
	 * A base stat of the item with its rarity worked in: the rolled line and the tier's
	 * potency add inside one parenthesis, so the two can never multiply each other. This
	 * is the ONLY place the rarity multiplier is written.
	 */
	public static int scale( Item item, RarityLine kind, int base ){
		Quality q = of( item );
		if (q == null || !hasRarity( item )) return base;
		return Math.round( base * (1f + q.value( kind ) + q.potency()) );
	}

	/** how many whole points of strength requirement a rolled STR_REQ line takes off */
	public static int strReqStep( int req, float value ){
		return Math.min( 2, Math.round( req * value ) );
	}

	public static int strReqBonus( Item item, int req ){
		Quality q = of( item );
		return q == null ? 0 : strReqStep( req, q.value( RarityLine.STR_REQ ) );
	}

	/** how many whole charges a rolled CHARGES line adds */
	public static int chargeStep( int initial, float value ){
		return Math.round( initial * value );
	}

	public int chargeBonus( Wand w ){
		return chargeStep( w.initialCharges(), value( RarityLine.CHARGES ) );
	}

	/** the hard ceiling on a wand's bar: an exotic wand simply holds more */
	public int chargeCap(){
		return 10 + Math.round( 10f * second() );
	}

	/**
	 * Exotic alone changes the shape of the combat maths rather than the size of a number:
	 * half its potency again, spent on cutting through armor, on turning blows aside, or on
	 * a wand's bar. It joins the channels that are already capped once, so three sources can
	 * never erase a hit.
	 */
	public float second(){
		return rarity == Rarity.EXOTIC ? 0.5f * potency() : 0f;
	}

	public static float second( Item item ){
		Quality q = of( item );
		return q == null || !hasRarity( item ) ? 0f : q.second();
	}

	// ---------------------------------------------------------------- masterwork

	public boolean fullyMasterworked(){
		return masterwork >= MASTERWORK_MAX;
	}

	public int masterworkCap(){
		return rarity.ordinal() >= Rarity.LEGENDARY.ordinal() ? MASTERWORK_MAX : MASTERWORK_CAP_LOW;
	}

	/** cores the next step costs, 0 when no step is possible */
	public int masterworkCost(){
		if (masterwork >= masterworkCap()) return 0;
		int next = masterwork + 1;
		return next <= 5 ? 1 : next <= 9 ? 2 : 3;
	}

	/** one masterwork step: lines rise through {@link #roll}; a common base gains a line
	 *  on its first step so masterworking is never empty */
	public void masterworkStep( Item item ){
		if (masterworkCost() == 0) return;
		masterwork++;
		if (lines.isEmpty()){
			ArrayList<RarityLine> pool = RarityLine.pool( family( item ) );
			if (!pool.isEmpty()){
				lines.add( Random.element( pool ) );
				rolls.add( 0f );
			}
		}
		applyDerived( item );
	}

	/** a new rarity with fresh lines; the type stays, and the masterwork too as far
	 *  as the new tier allows (a rare base stops at nine) */
	public void rerollRarity( Item item, Rarity to ){
		if (!hasRarity( item )) return;
		rarity = to;
		rollLines( item );
		masterwork = Math.min( masterwork, masterworkCap() );
		if (masterwork > 0 && lines.isEmpty()){
			//a masterworked common base keeps the line its first step gave it
			ArrayList<RarityLine> pool = RarityLine.pool( family( item ) );
			if (!pool.isEmpty()){
				lines.add( Random.element( pool ) );
				rolls.add( 0f );
			}
		}
		applyDerived( item );
	}

	// ---------------------------------------------------------------- the tempering forge

	/** what a good temper would do to this item: 1 a line driven to its maximum, 2 a free
	 *  masterwork step (every line already at its best), 0 nothing left to gain */
	public int temperGain(){
		for (float r : rolls) if (r < 1f) return 1;
		return masterworkCost() > 0 ? 2 : 0;
	}

	/** A clean quench on the Tempering Forge: the weakest rolled line goes to its maximum,
	 *  or, with nothing left to raise, the item takes a masterwork step no core paid for.
	 *  Returns what {@link #temperGain} promised. */
	public int temper( Item item ){
		int gain = temperGain();
		if (gain == 1){
			int weakest = 0;
			for (int i = 1; i < rolls.size(); i++){
				if (rolls.get( i ) < rolls.get( weakest )) weakest = i;
			}
			rolls.set( weakest, 1f );
			applyDerived( item );
		} else if (gain == 2){
			masterworkStep( item );
		}
		return gain;
	}

	/** Left on the forge too long: the last line is burnt out of the metal. False when
	 *  there was no line to lose (a common piece only blackens). */
	public boolean scorch( Item item ){
		if (lines.isEmpty()) return false;
		lines.remove( lines.size() - 1 );
		rolls.remove( rolls.size() - 1 );
		applyDerived( item );
		return true;
	}

	public boolean shiftType(){
		if (type.alpha()) return false;
		type = type.next();
		return true;
	}

	// ---------------------------------------------------------------- perks

	/** the weapon a hit counts for: the item itself, or, for a launcher's projectile that
	 *  carries no type of its own, the hero's typed launcher */
	private static Weapon perkWeapon( Weapon weapon, Char owner ){
		if (of( weapon ) != null || !(weapon instanceof MissileWeapon) || !(owner instanceof Hero)) return weapon;
		Item held = ((Hero) owner).belongings.weapon();
		return held instanceof Weapon && held != weapon && of( held ) != null
				&& WeaponPerk.of( held ) == WeaponPerk.SHOT ? (Weapon) held : weapon;
	}

	/** the weapon's perk on a landed blow; see {@link WeaponPerk} */
	public static int weaponHit( Weapon weapon, Char attacker, Char defender, int damage ){
		Weapon w = perkWeapon( weapon, attacker );
		Quality q = of( w );
		if (q == null) return damage;
		return WeaponPerk.of( w ).onHit( q, w, attacker, defender, damage );
	}

	public static boolean sureHit( Weapon weapon, Char owner, Char target ){
		if (!(owner instanceof Hero)) return false;
		//an alpha Insight artifact promises the swing after a miss, whatever is held
		if (GearPerk.sureNext( (Hero) owner )) return true;
		Weapon w = perkWeapon( weapon, owner );
		Quality q = of( w );
		return q != null && WeaponPerk.of( w ).sureHit( q, owner, target );
	}

	public static float perkSpeed( Weapon weapon ){
		Quality q = of( weapon );
		return q == null ? 1f : WeaponPerk.of( weapon ).speedFactor( q, weapon );
	}

	// ---------------------------------------------------------------- damage channels

	/** every rarity fraction that softens a blow is summed here and capped once: the
	 *  weapon's Guard, the armor's Bulwark, the artifact's Warden. They are taken AFTER
	 *  armor, where a fraction is worth what it says, and the cap is what keeps three
	 *  sources from erasing a hit outright */
	public static final float MITIGATE_CAP = 0.30f;

	/** the same for armor the hero's blows ignore: Crush, Ambush, a long Marksman shot
	 *  and an alpha Striker ring, summed and capped as one channel */
	public static final float PIERCE_CAP = 0.60f;

	public static float mitigation( Hero hero ){
		if (hero == null || hero.belongings == null) return 0f;
		float f = GearPerk.heroBonus( hero, GearPerk.Stat.MITIGATE );
		Item w = hero.belongings.weapon();
		Quality q = of( w );
		if (q != null && w instanceof Weapon) f += WeaponPerk.of( w ).guardFraction( q, (Weapon) w );
		//an exotic armor turns blows aside by its tier alone
		f += second( hero.belongings.armor() );
		return Math.min( MITIGATE_CAP, Math.max( 0f, f ) );
	}

	/** the blow after the rarity mitigation channel; only blows from a character are
	 *  softened, the way worn armor already ignores fire, hunger and falls */
	public static float mitigate( Hero hero, float damage, Object src ){
		if (!(src instanceof Char) || damage <= 0 || hero == null || hero.belongings == null) return damage;
		Item w = hero.belongings.weapon();
		Quality q = of( w );
		if (q != null && w instanceof Weapon){
			damage = WeaponPerk.of( w ).riposte( q, (Weapon) w, hero, damage, src );
			if (damage <= 0) return 0f;
		}
		float f = mitigation( hero );
		return f <= 0f ? damage : damage * (1f - f);
	}

	/** the share of the defender's armor roll this attacker's rarity effects ignore */
	public static float drPierce( Char attacker, Char defender ){
		if (!(attacker instanceof Hero) || defender == null) return 0f;
		Hero hero = (Hero) attacker;
		if (hero.belongings == null) return 0f;
		float f = GearPerk.heroBonus( hero, GearPerk.Stat.PIERCE );
		Item w = hero.belongings.attackingWeapon();
		Quality q = of( w );
		if (q != null && w instanceof Weapon) f += WeaponPerk.of( w ).pierce( q, (Weapon) w, hero, defender );
		//an exotic weapon cuts through armor by its tier alone
		f += second( w );
		return Math.min( PIERCE_CAP, Math.max( 0f, f ) );
	}

	//a reflected blow must never reflect back: one guard for every thorn in the system
	private static boolean reflecting = false;

	/** damage handed back to whoever struck: Guard's riposte and Warden's thorns */
	public static void reflect( Char attacker, Char target, int amount ){
		if (reflecting || amount <= 0 || attacker == null || target == null || !attacker.isAlive()) return;
		reflecting = true;
		try {
			attacker.damage( amount, target );
			if (attacker.sprite != null) attacker.sprite.showStatus( CharSprite.NEGATIVE, Integer.toString( amount ) );
		} finally {
			reflecting = false;
		}
	}

	/** the one tile of reach in the system: an alpha Reach weapon, and nothing else */
	public static int reachBonus( Item weapon, Char owner ){
		Quality q = of( weapon );
		return q != null && q.type.alpha() && weapon instanceof Weapon
				&& WeaponPerk.of( weapon ) == WeaponPerk.REACH ? 1 : 0;
	}

	/** a fractional per-hit amount as a whole number that averages to it: 2.3 is 2, or 3
	 *  three times in ten */
	public static int chanceRound( float x ){
		if (x <= 0f) return 0;
		int i = (int) x;
		return Random.Float() < x - i ? i + 1 : i;
	}

	/** an independent copy, for items split off or rebuilt from a typed item */
	public Quality copy(){
		Bundle b = new Bundle();
		storeInBundle( b );
		Quality q = new Quality();
		q.restoreFromBundle( b );
		return q;
	}

	/** whether two items carry the same quality, so a stack never merges different rolls */
	public static boolean sameQuality( Item a, Item b ){
		Quality qa = of( a ), qb = of( b );
		if (qa == null || qb == null) return qa == qb;
		return qa.rarity == qb.rarity && qa.type == qb.type && qa.masterwork == qb.masterwork
				&& qa.lines.equals( qb.lines ) && qa.rolls.equals( qb.rolls );
	}

	/** missile Dart: a tipped dart's hit may keep its tip */
	public static boolean keepTip( MissileWeapon missile ){
		Quality q = of( missile );
		return q != null && missile instanceof xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.TippedDart
				&& WeaponPerk.of( missile ) == WeaponPerk.DART && Random.Float() < 0.15f * q.type.rank;
	}

	/** true once: the hero's coming attack takes no time */
	public static boolean freeAttack( Item weapon ){
		Quality q = of( weapon );
		return q != null && weapon instanceof Weapon && WeaponPerk.of( weapon ).consumeFreeAttack( q );
	}

	/** missile Throw: 5/10/15% of throws deal max damage */
	public static int throwRoll( MissileWeapon missile, Char owner, int damage, int max ){
		Quality q = of( missile );
		if (q == null || !(owner instanceof Hero)) return damage;
		return WeaponPerk.of( missile ).throwRoll( q, damage, max );
	}

	// ---------------------------------------------------------------- kills

	/** called from Mob.die for every enemy: masterwork perks and the core / shifter drops */
	public static void onEnemyKilled( Mob mob, Object cause ){
		Hero hero = Dungeon.hero;
		if (hero == null || mob.alignment != Char.Alignment.ENEMY || mob instanceof NPC) return;

		boolean heroBlow = cause == hero;
		if (heroBlow){
			Item weapon = hero.belongings.thrownWeapon != null ? hero.belongings.thrownWeapon : hero.belongings.weapon();
			Quality q = of( weapon );
			if (q != null && weapon instanceof Weapon) WeaponPerk.of( weapon ).onKill( q, hero );
			if (q != null && q.fullyMasterworked()){
				q.kills++;
				if (hero.HP < hero.HT && killReady( q )) hero.heal( Math.max( 1, Math.round( hero.HT * 0.05f ) ) );
			}
		}
		if (cause instanceof Wand){
			Quality q = of( (Wand) cause );
			if (q != null && q.fullyMasterworked()){
				q.kills++;
				Wand w = (Wand) cause;
				//a fifth of the bar rather than one charge, so a big wand is paid in its own scale
				w.curCharges = Math.min( w.maxCharges, w.curCharges + Math.max( 1, Math.round( 0.20f * w.maxCharges ) ) );
				w.updateQuickslot();
			}
		}
		if (heroBlow || cause instanceof Wand || SkillInteractions.heroSkillSource( cause )){
			Quality armor = of( hero.belongings.armor );
			if (armor != null && armor.fullyMasterworked() && killReady( armor )){
				Buff.affect( hero, Barrier.class ).incShield( Math.max( 1, Math.round( hero.HT * 0.05f ) ) );
			}
		}

		if (Char.hasProp( mob, Char.Property.BOSS )){
			Dungeon.level.drop( new MasterworkCore().quantity( 1 + Random.Int( 2 ) ), mob.pos ).sprite.drop();
		} else if (heroBlow && Random.Float() < 0.0007f){
			//0.07% per kill of the hero's own, on top of the boss drops
			Dungeon.level.drop( new MasterworkCore(), mob.pos ).sprite.drop();
		} else if (heroBlow && !xyz.gabriwar.warpedpixeldungeon.levels.Delves.inDelve() && Dungeon.depth >= 3 && Dungeon.LimitedDrops.TYPE_SHIFTER.count < Dungeon.depth
				&& Random.Float() < 0.0001f){
			//0.01% per kill, from the hero's own kills only, and never more than one per floor reached
			Dungeon.LimitedDrops.TYPE_SHIFTER.count = Dungeon.depth;
			Dungeon.level.drop( new TypeShifter(), mob.pos ).sprite.drop();
		}
	}

	/** a fully-masterworked kill reward pays at most once every ten turns, so a room full
	 *  of weak enemies cannot heal the hero to full */
	private static boolean killReady( Quality q ){
		float now = Actor.now();
		if (now < q.killReady && q.killReady - now <= 10f) return false;
		q.killReady = now + 10f;
		return true;
	}

	// ---------------------------------------------------------------- text

	/** the block appended to an item's description: tier, lines, perk, masterwork */
	public String describe( Item item ){
		Family fam = family( item );
		StringBuilder sb = new StringBuilder();
		sb.append( Messages.get( this, "header", rarity.title(), type.title() ) );
		if (!lines.isEmpty()){
			sb.append( "\n" );
			for (int i = 0; i < lines.size(); i++){
				sb.append( "\n_" ).append( lines.get( i ).describe( item, roll( lines.get( i ) ) ) ).append( "_" );
			}
		}
		if (hasRarity( item )) sb.append( "\n" ).append( Messages.get( this, "potency", Math.round( potency() * 100f ) ) );
		if (fam != null) sb.append( "\n\n" ).append( perkText( item, type ) );
		if (masterwork > 0){
			sb.append( "\n\n" ).append( Messages.get( this, fullyMasterworked() ? "masterwork_full" : "masterwork", masterwork, MASTERWORK_MAX, kills ) );
		}
		return sb.toString();
	}

	/** the item's group perk at a type: weapons and thrown weapons, or any other gear */
	public static String perkText( Item item, ItemType t ){
		Family fam = family( item );
		if (fam == Family.WEAPON || fam == Family.MISSILE) return WeaponPerk.of( item ).describe( item, t );
		GearPerk p = GearPerk.of( item );
		return p == null ? "" : p.describe( item, t );
	}

	/** the rarity tab of {@link xyz.gabriwar.warpedpixeldungeon.windows.WndItemQuality} */
	public String describeRarity( Item item ){
		StringBuilder sb = new StringBuilder( Messages.get( this, "tab_rarity", rarity.title(), rarity.lines ) );
		if (!lines.isEmpty()){
			sb.append( "\n" );
			for (int i = 0; i < lines.size(); i++){
				sb.append( "\n_" ).append( lines.get( i ).describe( item, roll( lines.get( i ) ) ) ).append( "_" );
			}
		}
		sb.append( "\n" ).append( Messages.get( this, "potency", Math.round( potency() * 100f ) ) );
		sb.append( "\n\n" ).append( Messages.get( this, fullyMasterworked() ? "masterwork_full" : "masterwork", masterwork, MASTERWORK_MAX, kills ) );
		if (masterworkCost() > 0) sb.append( " " ).append( Messages.get( this, "masterwork_next", masterworkCost() ) );
		return sb.toString();
	}

	/** a rarity tab for a tier the item does not have: what that tier would give */
	public String describeRarityTier( Rarity r ){
		int from = r == Rarity.EXOTIC ? 15 : r == Rarity.LEGENDARY ? 5 : 1;
		return Messages.get( this, "tab_rarity_other", r.title(), r.lines,
				Math.round( r.minRoll * 100 ), Math.round( r.maxRoll * 100 ), from,
				Math.round( r.potency() * 100f ) );
	}

	/** one type tab: what the item's perk does at that type, marked when it is the current one */
	public String describeType( Item item, ItemType t ){
		Family fam = family( item );
		StringBuilder sb = new StringBuilder( Messages.get( this, t == type ? "tab_type_current" : "tab_type", t.title() ) );
		if (fam != null) sb.append( "\n\n" ).append( perkText( item, t ) );
		if (t.ordinal() > type.ordinal()){
			int cost = 0;
			for (ItemType s = type; s != t; s = s.next()) cost += s.shiftCost();
			sb.append( "\n\n" ).append( Messages.get( this, "tab_type_cost", cost ) );
		}
		return sb.toString();
	}

	/** the colour an item's name takes in windows: its rarity's, white-gold once masterworked */
	public int titleColor(){
		return fullyMasterworked() ? Rarity.MASTERWORK_COLOR : rarity.color;
	}

	// ---------------------------------------------------------------- bundle

	private static final String RARITY = "rarity";
	private static final String TYPE = "type";
	private static final String MASTERWORK = "masterwork";
	private static final String KILLS = "kills";
	private static final String GRANTED = "granted";
	private static final String LINES = "lines";
	private static final String ROLLS = "rolls";

	@Override
	public void storeInBundle( Bundle bundle ){
		bundle.put( RARITY, rarity );
		bundle.put( TYPE, type );
		bundle.put( MASTERWORK, masterwork );
		bundle.put( KILLS, kills );
		bundle.put( GRANTED, granted );
		String[] names = new String[lines.size()];
		float[] values = new float[lines.size()];
		for (int i = 0; i < lines.size(); i++){
			names[i] = lines.get( i ).name();
			values[i] = rolls.get( i );
		}
		bundle.put( LINES, names );
		bundle.put( ROLLS, values );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		rarity = bundle.getEnum( RARITY, Rarity.class );
		type = bundle.getEnum( TYPE, ItemType.class );
		masterwork = bundle.getInt( MASTERWORK );
		kills = bundle.getInt( KILLS );
		granted = bundle.getBoolean( GRANTED );
		lines.clear();
		rolls.clear();
		String[] names = bundle.getStringArray( LINES );
		float[] values = bundle.getFloatArray( ROLLS );
		if (names != null && values != null){
			for (int i = 0; i < names.length && i < values.length; i++){
				lines.add( RarityLine.valueOf( names[i] ) );
				rolls.add( values[i] );
			}
		}
	}
}

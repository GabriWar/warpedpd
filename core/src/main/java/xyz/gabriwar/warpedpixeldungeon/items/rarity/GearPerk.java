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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Digesting;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.LostInventory;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Starving;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.Artifact;
import xyz.gabriwar.warpedpixeldungeon.items.rings.Ring;
import xyz.gabriwar.warpedpixeldungeon.items.trinkets.Trinket;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

import java.util.Arrays;
import java.util.HashSet;

/**
 * The type perk of everything that is not a weapon: armor, rings, wands, trinkets and
 * artifacts. Each group of items shares one simple perk that scales x1 / x2 / x3 with
 * the type (gamma / beta / alpha); alpha adds the group's twist. No perk is a flat
 * number: armor values follow the armor's own block, wand values its level, and gear
 * with no stat of its own (rings, artifacts, trinkets, robes) follows the hero's max
 * health or mana, so every perk stays worth having from the first floor to the last.
 * Hero-wide stats from worn and carried gear are summed in {@link #heroBonus}.
 */
public enum GearPerk {

	//armor, by weight
	MENDING, NIMBLE, BULWARK, STALWART,
	//rings, by what they do
	STRIKER, WARD, ATTUNE, PROVISION,
	//wands, by what they do
	RESONANCE, BINDING, WELLSPRING, LEGION,
	//trinkets
	FORTUNE, OMEN, VITALITY,
	//artifacts
	ESCAPE, ASSAULT, INSIGHT, SUSTAIN, WARDEN;

	private static HashSet<String> names( String... n ){
		return new HashSet<>( Arrays.asList( n ) );
	}

	private static final HashSet<String> ROBES     = names( "AsceticArmor", "FollowerArmor" );

	private static final HashSet<String> R_STRIKER = names( "RingOfAccuracy", "RingOfFuror", "RingOfForce", "RingOfMight", "RingOfSharpshooting" );
	private static final HashSet<String> R_WARD    = names( "RingOfElements", "RingOfEvasion", "RingOfTenacity" );
	private static final HashSet<String> R_ATTUNE  = names( "RingOfArcana", "RingOfEnergy", "RingOfMagic", "RingOfHaste" );

	private static final HashSet<String> W_BINDING = names( "WandOfFrost", "WandOfSlowness", "WandOfPoison", "WandOfCorrosion", "WandOfCorruption", "WandOfAmok" );
	private static final HashSet<String> W_WELL    = names( "WandOfBlink", "WandOfTelekinesis", "WandOfTeleportation", "WandOfTransfusion", "WandOfRegrowth" );
	private static final HashSet<String> W_LEGION  = names( "WandOfFlock", "WandOfLivingEarth", "WandOfWarding" );

	private static final HashSet<String> T_OMEN    = names( "MossyClump", "TrapMechanism", "DimensionalSundial", "ChaoticCenser", "WondrousResin", "ThirteenLeafClover" );
	private static final HashSet<String> T_VITAL   = names( "EyeOfNewt", "SaltCube", "VialOfBlood", "FerretTuft" );

	private static final HashSet<String> A_ESCAPE  = names( "CloakOfShadows", "TimekeepersHourglass", "EtherealChains", "LloydsBeacon", "SkeletonKey" );
	private static final HashSet<String> A_ASSAULT = names( "RingOfDisintegration", "RingOfFrost", "UnstableSpellbook", "HolyTome", "DriedRose" );
	private static final HashSet<String> A_SUSTAIN = names( "ChaliceOfBlood", "HornOfPlenty", "SandalsOfNature", "AutoPotion" );
	private static final HashSet<String> A_WARDEN  = names( "CapeOfThorns", "ArrowBag", "Spectacles" );

	/** the item's group perk, or null for items outside these families */
	public static GearPerk of( Item item ){
		if (item == null) return null;
		String n = item.getClass().getSimpleName();
		if (item instanceof Armor){
			int tier = ((Armor) item).tier;
			if (tier <= 1 || ROBES.contains( n )) return MENDING;
			if (tier == 2) return NIMBLE;
			if (tier <= 4) return BULWARK;
			return STALWART;
		}
		if (item instanceof Ring){
			if (R_STRIKER.contains( n )) return STRIKER;
			if (R_WARD.contains( n )) return WARD;
			if (R_ATTUNE.contains( n )) return ATTUNE;
			return PROVISION;
		}
		if (item instanceof Wand){
			if (W_BINDING.contains( n )) return BINDING;
			if (W_WELL.contains( n )) return WELLSPRING;
			if (W_LEGION.contains( n )) return LEGION;
			return RESONANCE;
		}
		if (item instanceof Trinket){
			if (T_OMEN.contains( n )) return OMEN;
			if (T_VITAL.contains( n )) return VITALITY;
			return FORTUNE;
		}
		if (item instanceof Artifact){
			if (A_ESCAPE.contains( n )) return ESCAPE;
			if (A_ASSAULT.contains( n )) return ASSAULT;
			if (A_SUSTAIN.contains( n )) return SUSTAIN;
			if (A_WARDEN.contains( n )) return WARDEN;
			return INSIGHT;
		}
		return null;
	}

	// ---------------------------------------------------------------- text

	public String title(){
		return Messages.get( this, name().toLowerCase() + "_name" );
	}

	/** the perk line for this item at a type, with the twist appended at alpha */
	public String describe( Item item, ItemType type ){
		String s = Messages.get( this, name().toLowerCase(), title(), amount( item, type ), extra( item, type ) );
		if (type.alpha()) s += " " + Messages.get( this, name().toLowerCase() + "_twist", extra( item, type ) );
		return s;
	}

	private static int trinketLvl( Item item ){
		return Math.max( 0, Math.min( 3, item.level() ) );
	}

	private static int lvlOf( Item item ){
		return item == null ? 0 : Math.max( 0, item.buffedLvl() );
	}

	private static String pct( float f ){
		return Math.round( f * 100f ) + "%";
	}

	/** the number the perk line shows, read from the same coefficients the live hooks use */
	private String amount( Item item, ItemType type ){
		int r = type.rank;
		int lvl = lvlOf( item );
		switch (this){
			case MENDING:   return pct( 0.15f * r );
			case NIMBLE:    return pct( Math.min( 0.25f, 0.04f * r * (1f + 0.05f * lvl) ) );
			case BULWARK:   return pct( Math.min( 0.15f, 0.03f * r ) );
			case STALWART:  return pct( 0.10f + 0.10f * r );
			case STRIKER:   return pct( Math.min( 0.30f, 0.04f * r * (1f + 0.05f * lvl) ) );
			case WARD:      return pct( 0.06f * r );
			case ATTUNE:    return pct( Math.min( 0.30f, 0.05f * r * (1f + 0.05f * lvl) ) );
			case PROVISION: return pct( Math.min( 0.40f, 0.08f * r * (1f + Dungeon.depth / 25f) ) );
			case RESONANCE: return pct( Math.min( 0.40f, 0.08f * r + 0.01f * r * lvl ) );
			case BINDING:   return pct( 0.10f * r );
			case WELLSPRING:return pct( Math.min( 0.60f, 0.08f * r + 0.01f * r * lvl ) );
			case LEGION:    return Integer.toString( legionLevels( r, lvl ) );
			case FORTUNE:   return pct( fortuneGold( r, trinketLvl( item ) ) / 100f );
			case OMEN:      return pct( omenEvasionAt( type, item ) );
			case VITALITY:  return pct( 0.05f * r * (1 + trinketLvl( item )) );
			case ESCAPE:    return pct( 0.04f * r );
			case ASSAULT:   return pct( 0.04f * r );
			case INSIGHT:   return pct( 0.04f * r );
			case SUSTAIN:   return pct( 0.08f * r );
			case WARDEN:    return pct( Math.min( 0.25f, 0.04f * r * (1f + 0.1f * lvl) ) );
		}
		return "";
	}

	/** the second number a line shows, where it has one */
	private String extra( Item item, ItemType type ){
		int r = type.rank;
		switch (this){
			case MENDING:   return pct( 0.05f * r );
			case NIMBLE:    return pct( 0.03f * r );
			case BULWARK:   return pct( 0.10f * r );
			case STALWART:  return Integer.toString( r );
			case STRIKER:   return pct( Math.min( 0.50f, 0.15f * r ) );
			case WARD:      return Integer.toString( 2 * r );
			case ATTUNE:    return Integer.toString( Math.min( 3, r ) );
			case PROVISION: return pct( 0.05f * r );
			case RESONANCE: return Integer.toString( r );
			case BINDING:   return pct( 0.5f );
			case WELLSPRING:return Integer.toString( r );
			case LEGION:    return pct( 0.05f * r );
			case FORTUNE:   return pct( 0.05f * r );
			case VITALITY:  return "";
			case ESCAPE:    return pct( 0.5f * r );
			case ASSAULT:   return pct( 0.01f * r );
			case SUSTAIN:   return pct( 0.05f * r );
			case WARDEN:    return pct( Math.min( 0.50f, 0.15f * r ) );
			default:        return "";
		}
	}

	// ---------------------------------------------------------------- scaling formulas

	/** Fortune: gold found, in percent: 4% + 2% per level, per rank */
	private static int fortuneGold( int rank, int level ){
		return rank * (4 + 2 * level);
	}

	/** Omen: evasion as a fraction, 2% per rank per effective trinket level + 1 */
	private static float omenEvasion( int rank, Trinket trinket ){
		int l = trinketLevel( trinket, trinketLvl( trinket ) );
		return 0.02f * rank * (1 + l);
	}

	/** Omen as a type tab shows it: the alpha level comes from the tab, not the item */
	private static float omenEvasionAt( ItemType type, Item item ){
		int l = trinketLvl( item );
		if (type.alpha() && l < 3) l++;
		return 0.02f * type.rank * (1 + l);
	}

	/** Legion: the wand works this many levels higher, a quarter of its own level per rank */
	private static int legionLevels( int rank, int level ){
		return Math.min( 6, Math.max( 1, Math.round( 0.25f * rank * level ) ) );
	}

	/** a cooldown twist is ready once its time has come, or when the turn clock was pulled
	 *  back under it (saves and level changes rebase time toward 0) */
	private static boolean twistReady( Quality q, float cooldown ){
		float now = Actor.now();
		return now >= q.twistReady || q.twistReady - now > cooldown;
	}

	// ---------------------------------------------------------------- hero-wide stats

	public enum Stat { DAMAGE, ACCURACY, EVASION, SPEED, REGEN, HUNGER, GOLD, WAND_CHARGE,
		ARTIFACT_CHARGE, MANA, DEBUFF_RESIST, MITIGATE, PIERCE }

	/** the summed bonus of every worn ring, artifact and armor and the carried trinket for
	 *  one hero stat. Every stat is a FRACTION (0.10 = +10%); there are no point values left */
	public static float heroBonus( Char ch, Stat stat ){
		if (!(ch instanceof Hero) || ((Hero) ch).belongings == null) return 0f;
		Hero hero = (Hero) ch;
		float total = 0f;
		for (Item i : new Item[]{ hero.belongings.armor(), hero.belongings.ring(), hero.belongings.misc(),
				hero.belongings.artifact(), hero.belongings.getItem( Trinket.class ) }){
			Quality q = Quality.of( i );
			GearPerk p = of( i );
			if (q != null && p != null) total += p.bonus( i, q, hero, stat );
		}
		return total;
	}

	private float bonus( Item item, Quality q, Hero hero, Stat stat ){
		ItemType t = q.type;
		int r = t.rank;
		boolean a = t.alpha();
		int lvl = lvlOf( item );
		switch (this){
			//armor: Nimble and Stalwart are carried by the armor's own hooks below
			case MENDING:   return stat == Stat.REGEN ? 0.15f * r : 0f;
			case BULWARK:   return stat == Stat.MITIGATE ? bulwarkFraction( r, a ) : 0f;
			case STRIKER:
				if (stat == Stat.DAMAGE) return Math.min( 0.30f, 0.04f * r * (1f + 0.05f * lvl) );
				return stat == Stat.PIERCE && a ? Math.min( 0.50f, 0.15f * r ) : 0f;
			case WARD:      return stat == Stat.DEBUFF_RESIST ? 0.06f * r : 0f;
			case ATTUNE:
				if (stat == Stat.WAND_CHARGE || stat == Stat.ARTIFACT_CHARGE) return Math.min( 0.30f, 0.05f * r * (1f + 0.05f * lvl) );
				return stat == Stat.MANA ? 0.05f * r : 0f;
			case PROVISION:
				if (stat == Stat.GOLD) return Math.min( 0.40f, 0.08f * r * (1f + Dungeon.depth / 25f) );
				return stat == Stat.HUNGER ? 0.05f * r : 0f;
			case FORTUNE:   return stat == Stat.GOLD ? fortuneGold( r, trinketLvl( item ) ) / 100f : 0f;
			case OMEN:      return stat == Stat.EVASION && item instanceof Trinket ? omenEvasion( r, (Trinket) item ) : 0f;
			case VITALITY:  return stat == Stat.REGEN ? 0.05f * r * (1 + trinketLvl( item )) : 0f;
			case ESCAPE:
				//panic: for a few turns after a blow lands on you, the artifact runs harder
				return stat == Stat.SPEED ? 0.04f * r * (a && Actor.now() < q.twistReady ? 1f + 0.5f * r : 1f) : 0f;
			case ASSAULT:
				if (stat != Stat.DAMAGE) return 0f;
				int stacks = a && Actor.now() < q.assaultUntil ? Math.min( 3, q.assaultStacks ) : 0;
				return 0.04f * r + 0.01f * r * stacks;
			case INSIGHT:   return stat == Stat.ACCURACY ? 0.04f * r : 0f;
			case SUSTAIN:
				if (stat == Stat.REGEN) return 0.08f * r;
				return stat == Stat.HUNGER ? 0.05f * r : 0f;
			case WARDEN:
				if (stat == Stat.EVASION) return Math.min( 0.25f, 0.04f * r * (1f + 0.1f * lvl) );
				return stat == Stat.MITIGATE ? Math.min( 0.12f, 0.03f * r ) : 0f;
			default:        return 0f;
		}
	}

	/** Bulwark: the wall sets as you hold still, and an alpha wall is never fully down */
	private static float bulwarkFraction( int rank, boolean alpha ){
		float f = 0.03f * rank * Math.min( 1f, SkillInteractions.turnsStill() / 2f );
		if (alpha) f = Math.max( f, 0.015f * rank );
		return Math.min( 0.15f, f );
	}

	// ---------------------------------------------------------------- armor

	/** Nimble: extra evasion from light armor, ramping while you keep slipping blows */
	public static float armorEvasion( Armor armor ){
		Quality q = Quality.of( armor );
		if (q == null || of( armor ) != NIMBLE) return 0f;
		int r = q.type.rank;
		int stacks = q.type.alpha() && Actor.now() < q.dodgeUntil ? Math.min( 3, q.dodgeStacks ) : 0;
		return Math.min( 0.25f, 0.04f * r * (1f + 0.05f * armor.buffedLvl()) + 0.03f * r * stacks );
	}

	/** Stalwart: the armor's minimum block is never less than this share of its maximum */
	public static float armorMinFraction( Armor armor ){
		Quality q = Quality.of( armor );
		return q != null && of( armor ) == STALWART ? 0.10f + 0.10f * q.type.rank : 0f;
	}

	/** Stalwart, as Armor.DRMin asks it: the points that share is worth above the roll */
	public static int armorMinBonus( Armor armor, int max ){
		float f = armorMinFraction( armor );
		return f <= 0f ? 0 : Math.round( max * f );
	}

	/** alpha Stalwart: this many points of missing strength are ignored when moving */
	public static int armorStrengthGrace( Armor armor ){
		Quality q = Quality.of( armor );
		return q != null && q.type.alpha() && of( armor ) == STALWART ? q.type.rank : 0;
	}

	/** alpha Nimble: a dodge builds toward the next one */
	public static void heroDodged( Hero hero ){
		Armor armor = hero.belongings.armor();
		Quality q = Quality.of( armor );
		if (q == null || !q.type.alpha() || of( armor ) != NIMBLE) return;
		if (Actor.now() >= q.dodgeUntil) q.dodgeStacks = 0;
		q.dodgeStacks = Math.min( 3, q.dodgeStacks + 1 );
		q.dodgeUntil = Actor.now() + 5f;
	}

	/** the hero's own swing missed: the ramps that reward landing blows lose what they
	 *  built, and an alpha Insight artifact promises the next one */
	public static void heroMissed( Hero hero ){
		if (hero == null || hero.belongings == null) return;
		Item w = hero.belongings.attackingWeapon();
		Quality wq = Quality.of( w );
		if (wq != null && w instanceof xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon){
			WeaponPerk.of( w ).loseStacks( wq );
		}
		for (Item i : new Item[]{ hero.belongings.artifact(), hero.belongings.misc() }){
			Quality q = Quality.of( i );
			if (q != null && q.type.alpha() && of( i ) == INSIGHT) q.sureNext = true;
		}
	}

	/** a blow landed on the hero: what the rarity gear does about it. {@code prevented} is
	 *  the damage the mitigation channel already turned aside */
	public static void heroDamaged( Hero hero, int prevented, Char source ){
		if (hero == null || hero.belongings == null) return;
		//the armor banks what it stopped and spends it on the next swing
		Armor armor = hero.belongings.armor();
		Quality aq = Quality.of( armor );
		if (aq != null && aq.type.alpha() && of( armor ) == BULWARK && prevented > 0){
			aq.bankedBlock = Math.min( Math.round( hero.HT * 0.10f * aq.type.rank ), aq.bankedBlock + prevented );
		}
		for (Item i : new Item[]{ hero.belongings.artifact(), hero.belongings.misc() }){
			Quality q = Quality.of( i );
			GearPerk p = of( i );
			if (q == null || p == null || !q.type.alpha()) continue;
			int r = q.type.rank;
			if (p == WARDEN && prevented > 0 && source != null){
				//thorns of what the perk turned aside, never of the whole blow
				Quality.reflect( source, hero, Math.round( prevented * Math.min( 0.50f, 0.15f * r ) ) );
			} else if (p == ESCAPE){
				//panic: the stamp bonus() reads for the next few turns
				q.twistReady = Actor.now() + r;
			} else if (p == SUSTAIN && hero.HP * 3 < hero.HT && twistReady( q, 30f - 5f * r )){
				//last meal: the body spends what it has stored
				q.twistReady = Actor.now() + 30f - 5f * r;
				Buff.affect( hero, Hunger.class ).affectHunger( -Hunger.HUNGRY * 0.15f );
				hero.heal( Math.max( 1, Math.round( hero.HT * 0.05f * r ) ) );
			}
		}
	}

	// ---------------------------------------------------------------- rings

	/** every damage fraction the worn gear grants, plus the wall's banked blow */
	public static int heroHit( Hero hero, Char enemy, int damage ){
		float mult = 1f + Math.min( 0.30f, heroBonus( hero, Stat.DAMAGE ) );
		int banked = 0;
		Armor armor = hero.belongings.armor();
		Quality aq = Quality.of( armor );
		if (aq != null && aq.bankedBlock > 0 && of( armor ) == BULWARK){
			banked = aq.bankedBlock;
			aq.bankedBlock = 0;
		}
		//an alpha Assault keeps count of the turns you press the attack
		for (Item i : new Item[]{ hero.belongings.artifact(), hero.belongings.misc() }){
			Quality q = Quality.of( i );
			if (q == null || !q.type.alpha() || of( i ) != ASSAULT) continue;
			if (Actor.now() >= q.assaultUntil) q.assaultStacks = 0;
			q.assaultStacks = Math.min( 3, q.assaultStacks + 1 );
			q.assaultUntil = Actor.now() + 2f;
		}
		return Math.round( damage * mult ) + banked;
	}

	/** an alpha Insight promises the blow after a miss */
	public static boolean sureNext( Hero hero ){
		if (hero == null || hero.belongings == null) return false;
		for (Item i : new Item[]{ hero.belongings.artifact(), hero.belongings.misc() }){
			Quality q = Quality.of( i );
			if (q != null && q.sureNext && of( i ) == INSIGHT){
				q.sureNext = false;
				return true;
			}
		}
		return false;
	}

	/** Ward: a debuff may slide off, the more readily the more you are already suffering
	 *  (at most half of them); alpha makes you immune to that kind for a while.
	 *  Penalties that are not status effects cannot be resisted */
	public static boolean resistsDebuff( Char ch, Buff buff ){
		if (!(ch instanceof Hero) || buff.type != Buff.buffType.NEGATIVE) return false;
		if (buff instanceof LostInventory || buff instanceof Starving || buff instanceof Digesting
				|| buff instanceof Hypothermia || buff instanceof Heatstroke) return false;
		Hero hero = (Hero) ch;
		//an alpha Ward that has already shrugged this off keeps shrugging it off
		for (Item i : new Item[]{ hero.belongings.ring(), hero.belongings.misc() }){
			Quality q = Quality.of( i );
			if (q != null && q.type.alpha() && of( i ) == WARD
					&& q.wardImmune == buff.getClass() && Actor.now() < q.wardImmuneUntil){
				return true;
			}
		}
		float chance = Math.min( 0.50f, heroBonus( ch, Stat.DEBUFF_RESIST ) * (1f + 0.25f * negativeBuffs( ch )) );
		if (chance <= 0f || Random.Float() >= chance) return false;
		for (Item i : new Item[]{ hero.belongings.ring(), hero.belongings.misc() }){
			Quality q = Quality.of( i );
			if (q != null && q.type.alpha() && of( i ) == WARD){
				q.wardImmune = buff.getClass();
				q.wardImmuneUntil = Actor.now() + 2f * q.type.rank;
				break;
			}
		}
		if (ch.sprite != null) ch.sprite.showStatus( CharSprite.POSITIVE, Messages.get( GearPerk.class, "resisted" ) );
		return true;
	}

	private static int negativeBuffs( Char ch ){
		int n = 0;
		for (Buff b : ch.buffs()) if (b.type == Buff.buffType.NEGATIVE) n++;
		return n;
	}

	// ---------------------------------------------------------------- wands

	/** Resonance: a share of zaps keep their charge; an alpha echo empowers the next one */
	public static boolean resonance( Wand wand ){
		Quality q = Quality.of( wand );
		if (q == null || of( wand ) != RESONANCE) return false;
		int r = q.type.rank;
		float chance = Math.min( 0.40f, 0.08f * r + 0.01f * r * Math.max( 0, wand.buffedLvl() ) );
		if (Random.Float() >= chance) return false;
		if (q.type.alpha()) q.nextZapLevels = r;
		return true;
	}

	/** Binding: a zapped enemy may be crippled, longer as the wand levels; an alpha bind
	 *  spreads to the nearest other enemy at half length */
	public static void wandHit( Wand wand, Char target ){
		Quality q = Quality.of( wand );
		if (q == null || of( wand ) != BINDING || target == null || target == Dungeon.hero
				|| !target.isAlive() || target.alignment == Char.Alignment.ALLY) return;
		int r = q.type.rank;
		if (Random.Float() < 0.10f * r){
			int lvl = Math.max( 0, wand.buffedLvl() );
			float dur = 1f + 0.5f * r * (1f + lvl / 3f);
			Buff.prolong( target, Cripple.class, dur );
			if (q.type.alpha()){
				Char next = nearestOther( target );
				if (next != null) Buff.prolong( next, Cripple.class, dur * 0.5f );
			}
		}
	}

	private static Char nearestOther( Char from ){
		if (Dungeon.level == null) return null;
		Char best = null;
		int bestDist = 3;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m == from || !m.isAlive() || m.alignment != Char.Alignment.ENEMY) continue;
			int d = Dungeon.level.distance( from.pos, m.pos );
			if (d < bestDist){ bestDist = d; best = m; }
		}
		return best;
	}

	/** Wellspring: faster recharge for this wand */
	public static float wandRecharge( Wand wand ){
		Quality q = Quality.of( wand );
		if (q == null || of( wand ) != WELLSPRING) return 1f;
		int r = q.type.rank;
		return 1f + Math.min( 0.60f, 0.08f * r + 0.01f * r * Math.max( 0, wand.buffedLvl() ) );
	}

	/** how far past its maximum an alpha Wellspring may be charged */
	public static int overcharge( Wand wand ){
		Quality q = Quality.of( wand );
		return q != null && q.type.alpha() && of( wand ) == WELLSPRING ? q.type.rank : 0;
	}

	/** a zap went out: an alpha Wellspring spills into its overcharge, an alpha Legion
	 *  shields the allies that can see it */
	public static void wandZapped( Wand wand, boolean wasFull ){
		Quality q = Quality.of( wand );
		if (q == null || Dungeon.hero == null) return;
		//an alpha Attunement ring empowers the zap that follows a full bar, whatever wand it is
		if (wasFull && Dungeon.hero.belongings != null){
			for (Item i : new Item[]{ Dungeon.hero.belongings.ring(), Dungeon.hero.belongings.misc() }){
				Quality rq = Quality.of( i );
				if (rq != null && rq.type.alpha() && of( i ) == ATTUNE){
					q.nextZapLevels = Math.max( q.nextZapLevels, Math.min( 3, rq.type.rank ) );
					break;
				}
			}
		}
		if (!q.type.alpha()) return;
		GearPerk p = of( wand );
		int r = q.type.rank;
		if (p == WELLSPRING && wasFull){
			wand.curCharges = Math.min( wand.maxCharges + r, wand.curCharges + 1 );
		} else if (p == LEGION && Dungeon.level != null && twistReady( q, 10f - 2f * r )){
			q.twistReady = Actor.now() + 10f - 2f * r;
			for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
				if (m.alignment == Char.Alignment.ALLY && m.isAlive() && Dungeon.level.heroFOV[m.pos]){
					Buff.affect( m, Barrier.class ).incShield( Math.max( 1, Math.round( m.HT * 0.05f * r ) ) );
				}
			}
		}
	}

	/** Legion: summoning wands work higher, a quarter of the wand's own level per rank */
	public static int wandLevel( Wand wand ){
		Quality q = Quality.of( wand );
		//level(), not buffedLvl(): buffedLvl adds this bonus
		return q == null || of( wand ) != LEGION ? 0 : legionLevels( q.type.rank, Math.max( 0, wand.level() ) );
	}

	// ---------------------------------------------------------------- trinkets

	/** alpha Omen: the trinket works one level higher, never past its cap of 3 */
	public static int trinketLevel( Trinket trinket, int level ){
		Quality q = Quality.of( trinket );
		return q != null && q.type.alpha() && of( trinket ) == OMEN && level >= 0 && level < 3 ? level + 1 : level;
	}

	/** alpha Fortune: shops sell to its carrier cheaper, the better the trinket */
	public static int shopPrice( Hero buyer, int price ){
		if (buyer == null || buyer.belongings == null) return price;
		Trinket t = buyer.belongings.getItem( Trinket.class );
		Quality q = Quality.of( t );
		if (q == null || !q.type.alpha() || of( t ) != FORTUNE) return price;
		return Math.round( price * (1f - Math.min( 0.30f, 0.02f * q.type.rank * (2 + trinketLvl( t )) )) );
	}

	/** gold in hand: an alpha Fortune doubles a pile now and then, an alpha Provision
	 *  turns the coin into a meal */
	public static int goldFound( Hero hero, int amount ){
		if (hero == null || hero.belongings == null) return amount;
		Trinket t = hero.belongings.getItem( Trinket.class );
		Quality tq = Quality.of( t );
		if (tq != null && tq.type.alpha() && of( t ) == FORTUNE && Random.Float() < 0.05f * tq.type.rank){
			amount *= 2;
		}
		for (Item i : new Item[]{ hero.belongings.ring(), hero.belongings.misc() }){
			Quality q = Quality.of( i );
			if (q != null && q.type.alpha() && of( i ) == PROVISION){
				//coin and bread: the money is food
				Buff.affect( hero, Hunger.class ).affectHunger(
						Math.min( Hunger.HUNGRY * 0.10f, amount * 0.02f * q.type.rank ) );
				break;
			}
		}
		return amount;
	}

	// ---------------------------------------------------------------- regeneration

	/** alpha Mending: regeneration at full health is banked rather than wasted */
	public static boolean regenOverflow( Hero hero ){
		if (hero == null || hero.belongings == null || hero.HP < hero.HT) return false;
		Armor armor = hero.belongings.armor();
		Quality q = Quality.of( armor );
		return q != null && q.type.alpha() && of( armor ) == MENDING
				&& Buff.affect( hero, Barrier.class ).shielding() < Math.round( hero.HT * 0.05f * q.type.rank );
	}

	public static void bankRegen( Hero hero, int amount ){
		if (amount > 0) Buff.affect( hero, Barrier.class ).incShield( amount );
	}

	/** alpha Vitality: a hard-pressed hero's own healing washes a wound out with it */
	public static void regenTick( Hero hero ){
		if (hero == null || hero.belongings == null || hero.HP * 3 >= hero.HT) return;
		Trinket t = hero.belongings.getItem( Trinket.class );
		Quality q = Quality.of( t );
		if (q == null || !q.type.alpha() || of( t ) != VITALITY) return;
		Bleeding bleed = hero.buff( Bleeding.class );
		if (bleed != null){ bleed.detach(); return; }
		Poison poison = hero.buff( Poison.class );
		if (poison != null) poison.detach();
	}

	// ---------------------------------------------------------------- artifacts

	/** Escape / Assault / Insight / Sustain / Warden: faster artifact charge */
	public static float artifactCharge( Artifact artifact ){
		Quality q = Quality.of( artifact );
		GearPerk p = of( artifact );
		if (q == null || p == null) return 1f;
		switch (p){
			case ESCAPE: case ASSAULT: case INSIGHT:
				return 1f + 0.10f * q.type.rank;
			case SUSTAIN: case WARDEN:
				return 1f + 0.06f * q.type.rank;
			default:
				return 1f;
		}
	}
}

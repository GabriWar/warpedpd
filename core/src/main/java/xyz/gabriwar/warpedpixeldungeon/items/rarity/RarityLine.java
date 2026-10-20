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

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.wands.DamageWand;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MiniGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ShootGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.BowWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.Gun;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

import java.util.ArrayList;

/**
 * One rolled bonus stat on an item. Each kind belongs to a family pool and has a maximum;
 * the item's roll (0..1) times that maximum is the bonus, see {@link Quality#value}.
 * Flag kinds (STR_REQ, CHARGES) are all-or-nothing: they apply once the roll is at least
 * a half.
 */
public enum RarityLine {

	ACCURACY   ( Quality.Family.WEAPON, Quality.Family.MISSILE ),
	DAMAGE_MIN ( Quality.Family.WEAPON, Quality.Family.MISSILE ),
	DAMAGE_MAX ( Quality.Family.WEAPON, Quality.Family.MISSILE ),
	SPEED      ( Quality.Family.WEAPON ),
	STR_REQ    ( Quality.Family.WEAPON, Quality.Family.MISSILE, Quality.Family.ARMOR ),
	DEFENSE_MIN( Quality.Family.ARMOR ),
	DEFENSE_MAX( Quality.Family.ARMOR ),
	EVASION    ( Quality.Family.ARMOR ),
	WARMTH     ( Quality.Family.ARMOR ),
	DURABILITY ( Quality.Family.MISSILE ),
	CHARGES    ( Quality.Family.WAND ),
	RECHARGE   ( Quality.Family.WAND ),
	ZAP_DAMAGE ( Quality.Family.WAND ),
	//rings, trinkets and artifacts have no number of their own: their lines are hero-wide
	//stats, summed with the type perks in GearPerk.heroBonus
	MIGHT      ( Quality.Family.RING, Quality.Family.ARTIFACT ),
	AIM        ( Quality.Family.RING, Quality.Family.ARTIFACT, Quality.Family.TRINKET ),
	DODGE      ( Quality.Family.RING, Quality.Family.ARTIFACT, Quality.Family.TRINKET ),
	VIGOR      ( Quality.Family.RING, Quality.Family.ARTIFACT, Quality.Family.TRINKET ),
	STRIDE     ( Quality.Family.RING, Quality.Family.ARTIFACT ),
	CHANNEL    ( Quality.Family.RING, Quality.Family.ARTIFACT ),
	//on armor only heavy armor rolls it (fits)
	RESOLVE    ( Quality.Family.RING, Quality.Family.TRINKET, Quality.Family.ARMOR ),
	SATIETY    ( Quality.Family.ARTIFACT, Quality.Family.TRINKET ),
	PROSPECT   ( Quality.Family.TRINKET ),
	//weapons and thrown weapons: a chance of half again the blow, a chance to drain a third of it
	CRIT       ( Quality.Family.WEAPON, Quality.Family.MISSILE ),
	LIFESTEAL  ( Quality.Family.WEAPON, Quality.Family.MISSILE ),
	//launchers only, see fits( item ): a shot that spends nothing, a reload that takes no
	//time, a flamethrower's or laser's longer reach
	AMMO       ( Quality.Family.WEAPON ),
	RELOAD     ( Quality.Family.WEAPON ),
	RANGE      ( Quality.Family.WEAPON ),
	//a legendary or exotic weapon's vestigial enchantment or armor's vestigial glyph
	//(Quality.vestige): no family, so it never rolls with the others, it is given at index 0 by
	//Quality.rollVestige
	VESTIGE    (),
	//weapon traits, each offered by two kinds of weapon (kindTraits). Warm grip fights the cold
	//while the weapon is in hand (TileTemperature.feelsLikeAt); the next five lift a blow by who is
	//fighting where, summed into one edge before the crit (Quality.edge): Slayer against the family
	//it rolled (Quality.slayer), Executioner against a foe under half health, Moonlit by the hour
	//(Quality.moonFactor, accuracy too), Stormborn under open falling weather, Defiant while the
	//wielder is under half health. Hunter: a chance a slain beast leaves meat (Quality.onEnemyKilled).
	//Steady: a share of the wind's spoiling of a shot or throw undone (Hero.attackSkill)
	WARM_GRIP  ( Quality.Family.WEAPON ),
	SLAYER     ( Quality.Family.WEAPON, Quality.Family.MISSILE ),
	EXECUTIONER( Quality.Family.WEAPON, Quality.Family.MISSILE ),
	MOONLIT    ( Quality.Family.WEAPON ),
	STORMBORN  ( Quality.Family.WEAPON ),
	DEFIANT    ( Quality.Family.WEAPON ),
	HUNTER     ( Quality.Family.WEAPON, Quality.Family.MISSILE ),
	STEADY     ( Quality.Family.WEAPON, Quality.Family.MISSILE ),
	//armor traits: light armor (cloth, leather, the robes) keeps you quick, quiet, dry and cool,
	//heavy armor keeps the wind off and takes a fall. Fleet: movement speed, capped on its own
	//(GearPerk.heroBonus). Hushed: stealth, and wet boots make no sound (Char.stealth). Rainproof:
	//rain takes turns to soak you, and puddles soak your boots less often (Hero). Cooling: up to its
	//degrees off the heat, never below 25 C (TileTemperature.feelsLikeAt). Windbreak: a share of
	//the wind chill blocked (feelsLikeAt). Surefoot: a fall hurts less (Chasm.heroLand)
	FLEET      ( Quality.Family.ARMOR ),
	HUSHED     ( Quality.Family.ARMOR ),
	RAINPROOF  ( Quality.Family.ARMOR ),
	COOLING    ( Quality.Family.ARMOR ),
	WINDBREAK  ( Quality.Family.ARMOR ),
	SUREFOOT   ( Quality.Family.ARMOR );

	private final Quality.Family[] families;

	RarityLine( Quality.Family... families ){
		this.families = families;
	}

	/**
	 * The bonus at a full roll. Most lines are fractions of the item's own number; the
	 * countable ones are whole steps: STR_REQ takes 1 or 2 points by its roll (Quality.strReqStep),
	 * CHARGES a share of the wand's charges (Quality.chargeBonus), and WARMTH is degrees, because a percentage of a
	 * temperature means nothing to the climate that reads it.
	 */
	public float max(){
		switch (this){
			//weapons: the tier's own potency now carries most of the weight, so the lines
			//that used to be the whole of a rarity are worth about half what they were
			case ACCURACY:                     return 0.12f;
			case SPEED:                        return 0.10f;
			case DAMAGE_MIN: case DAMAGE_MAX:  return 0.12f;
			//armor: about 1.4x the weapon maxima, the measured worth of one armor level
			case DEFENSE_MIN: case DEFENSE_MAX:return 0.17f;
			case EVASION:                      return 0.12f;
			case DURABILITY:                   return 0.50f;
			case RECHARGE:                     return 0.30f;
			case ZAP_DAMAGE:                   return 0.20f;
			case STR_REQ:                      return 0.15f;
			case CHARGES:                      return 0.90f;
			case WARMTH:                       return 3f;
			//hero-wide: about what one gamma-to-beta type step of the matching perk gives
			case MIGHT:                        return 0.08f;
			case AIM: case DODGE:              return 0.10f;
			case VIGOR:                        return 0.25f;
			case STRIDE:                       return 0.06f;
			case CHANNEL:                      return 0.12f;
			case RESOLVE:                      return 0.08f;
			case SATIETY:                      return 0.15f;
			case PROSPECT:                     return 0.20f;
			//a 12% chance of x1.5 is worth a DAMAGE line; a third of the blow back at 10% is
			//well under Vampiric
			case CRIT:                         return 0.12f;
			case LIFESTEAL:                    return 0.10f;
			case AMMO: case RELOAD:            return 0.15f;
			//a share of the gun's own reach, in whole tiles (Quality.rangeStep)
			case RANGE:                        return 0.20f;
			//how often the vestige fires, as a share of how often its enchantment would
			case VESTIGE:                      return 0.30f;
			//degrees, as WARMTH
			case WARM_GRIP:                    return 3f;
			//conditional damage: the narrower the condition, the larger the share
			case SLAYER: case DEFIANT:         return 0.30f;
			case EXECUTIONER: case MOONLIT:    return 0.15f;
			case STORMBORN:                    return 0.25f;
			//a chance of meat on a beast's kill
			case HUNTER:                       return 0.25f;
			//the share of the wind's aim penalty undone: a full masterwork cancels it
			case STEADY:                       return 0.50f;
			//movement speed, a little under STRIDE's worth once it meets its own cap (FLEET_CAP)
			case FLEET:                        return 0.08f;
			//stealth points, as a crouch gives
			case HUSHED:                       return 1f;
			//25 turns of rain a whole point; the share of puddle soakings spared
			case RAINPROOF:                    return 0.40f;
			//degrees, as WARMTH, on the hot side
			case COOLING:                      return 5f;
			//shares of the wind chill and of a fall's hurt
			case WINDBREAK:                    return 0.50f;
			case SUREFOOT:                     return 0.40f;
			default:                           return 1f;
		}
	}

	/** whether this line adds to a hero-wide stat, and so is read by GearPerk.heroBonus */
	public boolean feeds( GearPerk.Stat stat ){
		switch (this){
			case MIGHT:    return stat == GearPerk.Stat.DAMAGE;
			case AIM:      return stat == GearPerk.Stat.ACCURACY;
			case DODGE:    return stat == GearPerk.Stat.EVASION;
			case VIGOR:    return stat == GearPerk.Stat.REGEN;
			case STRIDE: case FLEET: return stat == GearPerk.Stat.SPEED;
			case CHANNEL:  return stat == GearPerk.Stat.WAND_CHARGE || stat == GearPerk.Stat.ARTIFACT_CHARGE;
			case RESOLVE:  return stat == GearPerk.Stat.DEBUFF_RESIST;
			case SATIETY:  return stat == GearPerk.Stat.HUNGER;
			case PROSPECT: return stat == GearPerk.Stat.GOLD;
			default:       return false;
		}
	}

	/** a trait: a line that does something of its own (a chance, a hook, warmth) rather than lift
	 *  one of the item's numbers. A weapon's, thrown weapon's or armor's tier fills its lines by
	 *  slot (see Quality.rollLines): a common one a plain bonus, an uncommon one a trait, a rare or
	 *  better one a trait and a plain bonus */
	public boolean trait(){
		switch (this){
			case CRIT: case LIFESTEAL: case AMMO: case RELOAD: case RANGE: case WARMTH: case RESOLVE:
			case WARM_GRIP: case SLAYER: case EXECUTIONER: case MOONLIT: case STORMBORN: case DEFIANT:
			case HUNTER: case STEADY:
			case FLEET: case HUSHED: case RAINPROOF: case COOLING: case WINDBREAK: case SUREFOOT:
				return true;
			default:
				return false;
		}
	}

	/** whether a family's lines roll by slot, a plain bonus and a trait (weapons, thrown weapons and
	 *  armor), rather than from the whole pool (wands, rings, trinkets and artifacts) */
	public static boolean slotted( Quality.Family family ){
		return family == Quality.Family.WEAPON || family == Quality.Family.MISSILE || family == Quality.Family.ARMOR;
	}

	public boolean fits( Quality.Family family ){
		for (Quality.Family f : families) if (f == family) return true;
		return false;
	}

	/** whether this kind of item can use the line: only a wand that deals damage can roll for more of
	 *  it, only what spends ammunition can spare it, only what reloads can reload for free, and only
	 *  a gun with a reach of its own (the flamethrower, the laser) can reach further */
	public boolean fits( Item item ){
		switch (this){
			case ZAP_DAMAGE: return item instanceof DamageWand;
			case AMMO:       return reloads( item ) || item instanceof BowWeapon;
			case RELOAD:     return reloads( item );
			case RANGE:      return item instanceof Gun && ((Gun) item).baseRange() > 0;
			case WARM_GRIP: case SLAYER: case EXECUTIONER: case MOONLIT: case STORMBORN: case DEFIANT:
			case HUNTER: case STEADY:
				return item instanceof Weapon && kindOffers( WeaponPerk.of( item ), this );
			case FLEET: case HUSHED: case RAINPROOF: case COOLING:
				return item instanceof Armor && !heavy( (Armor) item );
			case WINDBREAK: case SUREFOOT:
				return item instanceof Armor && heavy( (Armor) item );
			case RESOLVE:
				return !(item instanceof Armor) || heavy( (Armor) item );
			default:         return true;
		}
	}

	/** heavy armor (the Bulwark and Stalwart weights, mail and plate), as against light (cloth,
	 *  leather, the robes) */
	public static boolean heavy( Armor armor ){
		GearPerk p = GearPerk.of( armor );
		return p == GearPerk.BULWARK || p == GearPerk.STALWART;
	}

	/** how much movement speed a FLEET line gives at most, however high it rolls or is worked */
	public static final float FLEET_CAP = 0.10f;

	/** how many turns of rain it takes to soak a hero whose armor carries a RAINPROOF line of this
	 *  value: 0 (the first turn, as without one) when there is none */
	public static int rainTurns( float value ){
		return Math.round( 25f * value );
	}

	private static final RarityLine[] NO_LINES        = {};
	private static final RarityLine[] SLAYER_EXEC     = { SLAYER, EXECUTIONER };
	private static final RarityLine[] MOON_HUNTER     = { MOONLIT, HUNTER };
	private static final RarityLine[] MOON_WARM       = { MOONLIT, WARM_GRIP };
	private static final RarityLine[] SLAYER_DEFIANT  = { SLAYER, DEFIANT };
	private static final RarityLine[] DEFIANT_STORM   = { DEFIANT, STORMBORN };
	private static final RarityLine[] STORM_HUNTER    = { STORMBORN, HUNTER };
	private static final RarityLine[] STORM_WARM      = { STORMBORN, WARM_GRIP };
	private static final RarityLine[] EXEC_MOON       = { EXECUTIONER, MOONLIT };
	private static final RarityLine[] HUNTER_STEADY   = { HUNTER, STEADY };
	private static final RarityLine[] STEADY_ONLY     = { STEADY };
	private static final RarityLine[] STEADY_EXEC     = { STEADY, EXECUTIONER };
	private static final RarityLine[] SLAYER_HUNTER   = { SLAYER, HUNTER };
	private static final RarityLine[] STEADY_SLAYER   = { STEADY, SLAYER };
	private static final RarityLine[] EXEC_WARM       = { EXECUTIONER, WARM_GRIP };
	private static final RarityLine[] SLAYER_WARM     = { SLAYER, WARM_GRIP };
	private static final RarityLine[] MOON_STORM      = { MOONLIT, STORMBORN };
	private static final RarityLine[] HUNTER_EXEC     = { HUNTER, EXECUTIONER };

	/** the weapon traits a kind of weapon offers, beside the crit and lifesteal every weapon offers
	 *  and the launcher lines (fits) */
	private static RarityLine[] kindTraits( WeaponPerk perk ){
		switch (perk){
			case MOMENTUM:  return SLAYER_EXEC;
			case KEEN:      return MOON_HUNTER;
			case SWIFT:     return MOON_WARM;
			case CRUSH:     return SLAYER_DEFIANT;
			case WILD:      return DEFIANT_STORM;
			case REACH:     return STORM_HUNTER;
			case GUARD:     return SLAYER_DEFIANT;
			case SIPHON:    return STORM_WARM;
			case SCYTHE:    return EXEC_MOON;
			case SHOT:      return HUNTER_STEADY;
			case GUN:       return STEADY_ONLY;
			case DART:      return HUNTER_STEADY;
			case THROW:     return STEADY_EXEC;
			case IMPACT:    return SLAYER_HUNTER;
			case BOOMERANG: return STEADY_SLAYER;
			case WOODSMAN:  return EXEC_WARM;
			case QUARRY:    return SLAYER_WARM;
			case BARD:      return MOON_STORM;
			case LASH:      return HUNTER_EXEC;
			default:        return NO_LINES;
		}
	}

	private static boolean kindOffers( WeaponPerk perk, RarityLine line ){
		for (RarityLine l : kindTraits( perk )) if (l == line) return true;
		return false;
	}

	private static boolean reloads( Item item ){
		return item instanceof Gun || item instanceof MiniGun || item instanceof ShootGun;
	}

	/** the pool an item can actually roll: every line of its family that fits it */
	public static ArrayList<RarityLine> pool( Quality.Family family, Item item ){
		ArrayList<RarityLine> out = new ArrayList<>();
		for (RarityLine l : values()) if (l.fits( family ) && l.fits( item )) out.add( l );
		return out;
	}

	/** the item's pool less its traits: the lines that lift one of its numbers */
	public static ArrayList<RarityLine> plain( Quality.Family family, Item item ){
		ArrayList<RarityLine> out = pool( family, item );
		for (int i = out.size() - 1; i >= 0; i--) if (out.get( i ).trait()) out.remove( i );
		return out;
	}

	/** the traits the item's pool holds */
	public static ArrayList<RarityLine> traits( Quality.Family family, Item item ){
		ArrayList<RarityLine> out = pool( family, item );
		for (int i = out.size() - 1; i >= 0; i--) if (!out.get( i ).trait()) out.remove( i );
		return out;
	}

	/** a trait's name in a list, e.g. "critical hits" (the catalog's slot sentence) */
	public String shortName(){
		return Messages.get( this, name().toLowerCase() + "_short" );
	}

	/** the line as shown under the item's stats, e.g. "+12% accuracy" */
	public String describe( Item item, float roll ){
		switch (this){
			case WARMTH: case WARM_GRIP:
				return Messages.get( this, name().toLowerCase(), Math.round( max() * roll * 10f ) / 10f );
			case SLAYER:
				return Messages.get( this, "slayer", Math.round( max() * roll * 100f ), Quality.of( item ).slayer.title() );
			case MOONLIT:
				//the night's share, and the third of it lost around noon (Quality.moonFactor)
				return Messages.get( this, "moonlit", Math.round( max() * roll * 100f ), Math.round( max() * roll / 3f * 100f ) );
			case FLEET:
				return Messages.get( this, "fleet", Math.round( Math.min( FLEET_CAP, max() * roll ) * 100f ) );
			case HUSHED: case COOLING:
				return Messages.get( this, name().toLowerCase(), Math.round( max() * roll * 10f ) / 10f );
			case RAINPROOF:
				return Messages.get( this, "rainproof", rainTurns( max() * roll ), Math.round( max() * roll * 100f ) );
			case STR_REQ:
				return Messages.get( this, "str_req", Quality.strReqStep( max() * roll ) );
			case CHARGES:
				return Messages.get( this, "charges", Quality.chargeStep(
						item instanceof Wand ? ((Wand) item).initialCharges() : 2, max() * roll ) );
			case RANGE:
				return Messages.get( this, "range", Quality.rangeStep(
						item instanceof Gun ? ((Gun) item).baseRange() : 0, max() * roll ) );
			case VESTIGE: {
				if (item instanceof Armor){
					Armor.Glyph g = Quality.glyphVestigeOf( item );
					return Quality.vestigeSilent( (Armor) item, g, null )
							? Messages.get( this, "vestige_silent_glyph", g.name() )
							: Messages.get( this, "vestige", g.name(), Math.round( max() * roll * 100f ) );
				}
				Weapon w = (Weapon) item;
				Weapon.Enchantment v = Quality.vestigeOf( item );
				if (Quality.vestigeSilent( w, v, null )){
					//kin under another name (fortunate beside slashing) is named, so the player sees why
					Weapon.Enchantment twin = Quality.kin( v, w.enchantment ) ? w.enchantment : w.enchantment2;
					return twin.name().equals( v.name() )
							? Messages.get( this, "vestige_silent", v.name() )
							: Messages.get( this, "vestige_silent_kin", v.name(), twin.name() );
				}
				return Messages.get( this, "vestige", v.name(), Math.round( max() * roll * 100f ) );
			}
			default:
				return Messages.get( this, name().toLowerCase(), Math.round( max() * roll * 100f ) );
		}
	}
}

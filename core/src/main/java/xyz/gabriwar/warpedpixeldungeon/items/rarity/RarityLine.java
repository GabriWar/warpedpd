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
import xyz.gabriwar.warpedpixeldungeon.items.wands.DamageWand;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
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
	RESOLVE    ( Quality.Family.RING, Quality.Family.TRINKET ),
	SATIETY    ( Quality.Family.ARTIFACT, Quality.Family.TRINKET ),
	PROSPECT   ( Quality.Family.TRINKET );

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
			case STRIDE:   return stat == GearPerk.Stat.SPEED;
			case CHANNEL:  return stat == GearPerk.Stat.WAND_CHARGE || stat == GearPerk.Stat.ARTIFACT_CHARGE;
			case RESOLVE:  return stat == GearPerk.Stat.DEBUFF_RESIST;
			case SATIETY:  return stat == GearPerk.Stat.HUNGER;
			case PROSPECT: return stat == GearPerk.Stat.GOLD;
			default:       return false;
		}
	}

	public boolean fits( Quality.Family family ){
		for (Quality.Family f : families) if (f == family) return true;
		return false;
	}

	public static ArrayList<RarityLine> pool( Quality.Family family ){
		ArrayList<RarityLine> out = new ArrayList<>();
		for (RarityLine l : values()) if (l.fits( family )) out.add( l );
		return out;
	}

	/** the pool an item can actually use: only a wand that deals damage can roll for more of it */
	public static ArrayList<RarityLine> pool( Quality.Family family, Item item ){
		ArrayList<RarityLine> out = pool( family );
		if (!(item instanceof DamageWand)) out.remove( ZAP_DAMAGE );
		return out;
	}

	/** the line as shown under the item's stats, e.g. "+12% accuracy" */
	public String describe( Item item, float roll ){
		switch (this){
			case WARMTH:
				return Messages.get( this, "warmth", Math.round( max() * roll * 10f ) / 10f );
			case STR_REQ:
				return Messages.get( this, "str_req", Quality.strReqStep( max() * roll ) );
			case CHARGES:
				return Messages.get( this, "charges", Quality.chargeStep(
						item instanceof Wand ? ((Wand) item).initialCharges() : 2, max() * roll ) );
			default:
				return Messages.get( this, name().toLowerCase(), Math.round( max() * roll * 100f ) );
		}
	}
}

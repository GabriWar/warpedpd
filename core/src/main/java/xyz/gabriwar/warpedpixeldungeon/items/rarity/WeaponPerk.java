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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Bible;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Crossbow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MageBook;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MageStaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greatshield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RoundShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.LanceNShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.ObsidianShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.SpearNShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.UnholyBible;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.BowWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.Gun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

/**
 * The type perk of a weapon. Groups follow what the weapon is, read from its base class
 * and stats rather than its name: launchers shoot, weapons that block guard, long reach
 * keeps enemies at bay, fast weapons flurry, slow or steady ones crush, erratic ones land
 * wild blows, light blades strike from hiding, casters' weapons drain mana, and the rest
 * build momentum. Thrown weapons split into darts, light throws and heavy throws. Each
 * perk scales x1 / x2 / x3 with the type; alpha adds the group's twist.
 */
public enum WeaponPerk {

	SHOT, GUARD, REACH, SWIFT, CRUSH, WILD, KEEN, SIPHON, MOMENTUM,
	DART, THROW, IMPACT;

	public static WeaponPerk of( Item item ){
		if (item instanceof MissileWeapon){
			if (item instanceof Dart) return DART;
			return ((MissileWeapon) item).tier >= 3 ? IMPACT : THROW;
		}
		if (!(item instanceof Weapon)) return MOMENTUM;
		Weapon w = (Weapon) item;
		if (w instanceof Gun || w instanceof BowWeapon || w instanceof Crossbow || w instanceof SpiritBow
				|| !(w instanceof MeleeWeapon)) return SHOT;
		if (w instanceof MagesStaff || w instanceof MageStaff || w instanceof MageBook
				|| w instanceof Bible || w instanceof UnholyBible) return SIPHON;
		//shields are pinned: the stance weapons report no defence in their attack stance
		if (w instanceof RoundShield || w instanceof Greatshield || w instanceof ObsidianShield
				|| w instanceof LanceNShield || w instanceof SpearNShield) return GUARD;
		if (Dungeon.hero != null && w.defenseFactor( Dungeon.hero ) > 0) return GUARD;
		if (w.RCH >= 2) return REACH;
		if (w.DLY < 1f) return SWIFT;
		if (w.DLY > 1f || w.ACC > 1f) return CRUSH;
		if (w.ACC < 1f) return WILD;
		if (((MeleeWeapon) w).tier <= 2) return KEEN;
		return MOMENTUM;
	}

	public String title(){
		return Messages.get( this, name().toLowerCase() + "_name" );
	}

	/** the perk line for this item at a type, with the twist appended at alpha */
	public String describe( Item item, ItemType type ){
		String s = Messages.get( this, name().toLowerCase(), title(), amount( item, type ), extra( item, type ) );
		if (type.alpha()) s += " " + Messages.get( this, name().toLowerCase() + "_twist", extra( item, type ) );
		return s;
	}

	/** the number shown for this item, from the same coefficients the live hooks use, so a
	 *  printed number can never drift from the applied one */
	private String amount( Item item, ItemType type ){
		int r = type.rank;
		Weapon w = item instanceof Weapon ? (Weapon) item : null;
		int lvl = w == null ? 0 : Math.max( 0, w.buffedLvl() );
		switch (this){
			case SHOT:      return pct( 0.15f * r );
			case REACH:     return pct( 0.18f * r );
			case WILD:      return pct( 0.10f * r );
			case MOMENTUM:  return pct( 0.04f * r * 5 );
			case KEEN:      return pct( 0.15f * r );
			case IMPACT:    return pct( 0.20f * r );
			case DART:      return pct( 0.30f );
			case THROW:     return pct( 0.15f * r );
			case SIPHON:    return pct( 0.04f * r );
			case CRUSH:     return pct( 0.05f * r );
			case SWIFT:
				return pct( Math.min( 0.30f, 0.04f * r + 0.005f * lvl + 0.01f * r * 5 ) );
			case GUARD: {
				int tier = w instanceof MeleeWeapon ? ((MeleeWeapon) w).tier : 1;
				return pct( Math.min( 0.15f, 0.02f * r + 0.004f * (tier + lvl) ) );
			}
		}
		return "";
	}

	/** the second number a perk line shows, where it has one (a pierce, a window, a cap) */
	private String extra( Item item, ItemType type ){
		int r = type.rank;
		switch (this){
			case CRUSH: case KEEN: return pct( Math.min( 0.45f, 0.12f * r ) );
			case SHOT:             return Integer.toString( 6 - r );
			case WILD:             return pct( 0.3f + 0.2f * r );
			case IMPACT:           return Integer.toString( r );
			case DART:             return Integer.toString( r );
			case MOMENTUM:         return pct( 0.04f * r );
			case SWIFT:            return Integer.toString( r );
			case REACH:            return pct( 0.06f * r );
			case THROW:            return pct( 0.25f * r );
			case SIPHON:           return Integer.toString( r );
			case GUARD:            return pct( 0.25f * r );
			default:               return "";
		}
	}

	private static String pct( float f ){
		return Math.round( f * 100f ) + "%";
	}

	// ---------------------------------------------------------------- hooks

	/** a landed blow: the perk's on-hit effect, returning the damage to deal. Every perk
	 *  here multiplies the blow; nothing adds points to it, and nothing strips armour from
	 *  in here - that rides the one capped channel in {@link Quality#drPierce} */
	public int onHit( Quality q, Weapon weapon, Char attacker, Char defender, int damage ){
		if (!(attacker instanceof Hero) || defender == null) return damage;
		Hero hero = (Hero) attacker;
		int r = q.type.rank;
		boolean alpha = q.type.alpha();
		int dist = Dungeon.level.distance( attacker.pos, defender.pos );
		switch (this){
			case SHOT:
				//the further the shot, the more it is worth, up to its own ceiling
				if (dist >= 3){
					damage = Math.round( damage * (1f + Math.min( 0.15f * r, 0.06f * r + 0.02f * r * (dist - 2) )) );
					burst( defender, 2 );
				}
				return damage;
			case REACH:
				if (dist >= 2){
					damage = Math.round( damage * (1f + Math.min( 0.18f * r, 0.06f * r * (dist - 1) )) );
					burst( defender, 2 );
				}
				return damage;
			case SWIFT:
				//the ramp the speed bonus reads; a miss drops it, see loseStacks
				q.swiftStacks = Math.min( 5, q.swiftStacks + 1 );
				if (alpha && ++q.hitCount >= 5){
					q.hitCount = 0;
					q.freeNext = true;
					burst( hero, 3 );
				}
				return damage;
			case CRUSH:
				damage = Math.round( damage * (1f + 0.05f * r) );
				if (alpha){
					//sunder: the armour it has already broken open, remembered per target
					if (q.shredTarget != defender.id() || Actor.now() >= q.shredUntil) q.shredStacks = 0;
					q.shredTarget = defender.id();
					q.shredStacks = Math.min( 3, q.shredStacks + 1 );
					q.shredUntil = Actor.now() + 3f + r;
					burst( defender, 2 );
				}
				return damage;
			case WILD:
				if (Random.Float() < 0.10f * r){
					//the payoff grows with the type too, not only how often it lands
					damage = Math.round( damage * (1.3f + 0.2f * r) );
					crit( defender );
					if (alpha) Buff.affect( defender, Bleeding.class ).set( Math.max( 1, Math.round( damage * 0.10f * r ) ) );
				}
				return damage;
			case KEEN:
				if (defender instanceof Mob && ((Mob) defender).surprisedBy( attacker, true )){
					damage = Math.round( damage * (1f + 0.15f * r) );
					crit( defender );
					//execute: a surprise blow finishes anything already on its last legs
					if (alpha && !Char.hasProp( defender, Char.Property.BOSS )
							&& !Char.hasProp( defender, Char.Property.MINIBOSS )
							&& defender.HP - damage <= Math.round( defender.HT * (0.05f + 0.03f * r) )){
						damage = Math.max( damage, defender.HP );
						q.freeNext = true;
					}
				}
				return damage;
			case SIPHON: {
				//a share of the mana pool per hit, so it keeps pace as the pool grows
				int cap = hero.MT + xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic.manaBonus( hero );
				int mana = Quality.chanceRound( Math.min( 3f, 0.04f * r * cap ) );
				if (mana > 0){
					if (hero.MP < cap){
						hero.MP = Math.min( cap, hero.MP + mana );
						if (hero.sprite != null) hero.sprite.showStatus( 0x44CCFF, "+" + mana + " MP" );
					} else if (alpha){
						//overflow: a full pool spills into a barrier instead of being thrown away
						Buff.affect( hero, Barrier.class ).incShield( mana );
					}
				}
				return damage;
			}
			case MOMENTUM:
				if (q.hitTarget != defender.id()){ q.hitTarget = defender.id(); q.hitCount = 0; }
				damage = Math.round( damage * (1f + 0.04f * r * Math.min( 5, q.hitCount )) );
				if (q.hitCount < 5) q.hitCount++;
				else burst( defender, 1 + r );
				return damage;
			case DART: {
				//a dart is worth more the more the target is already suffering
				int debuffs = 0;
				for (Buff b : defender.buffs()) if (b.type == Buff.buffType.NEGATIVE) debuffs++;
				damage = Math.round( damage * (1f + Math.min( 0.30f, 0.06f * r + 0.04f * r * debuffs )) );
				if (alpha){
					int tier = weapon instanceof MissileWeapon ? ((MissileWeapon) weapon).tier : 1;
					Buff.prolong( defender, Roots.class, 0.5f * r * (1 + tier) );
				}
				return damage;
			}
			case THROW:
				if (q.keenHit){
					q.keenHit = false;
					damage = Math.round( damage * (1f + 0.25f * r) );
					crit( defender );
					//true flight: the throw that lands keenest is the one you get back
					if (alpha) q.throwSave = true;
				}
				return damage;
			case IMPACT: {
				//worth the most on an untouched target, fading as it bleeds
				float hpFrac = defender.HT > 0 ? defender.HP / (float) defender.HT : 0f;
				float b = 0.20f * r * hpFrac;
				if (b > 0.01f){
					damage = Math.round( damage * (1f + b) );
					burst( defender, 3 );
					if (alpha) Buff.prolong( defender, Cripple.class, 1f + 0.5f * r * hpFrac );
				}
				return damage;
			}
			default:
				return damage;
		}
	}

	/** Keen: a share of throws land at their keenest */
	public int throwRoll( Quality q, int damage, int max ){
		if (this != THROW) return damage;
		//the payoff is taken in onHit as a multiplier: clamping to max() here was a no-op
		//on any hero with strength to spare
		q.keenHit = Random.Float() < 0.15f * q.type.rank;
		return damage;
	}

	/** attack-speed multiplier (>1 = faster), ramping while the blows keep landing */
	public float speedFactor( Quality q, Weapon w ){
		if (this != SWIFT) return 1f;
		int lvl = w == null ? 0 : Math.max( 0, w.buffedLvl() );
		return 1f + Math.min( 0.30f, 0.04f * q.type.rank + 0.005f * lvl
				+ 0.01f * q.type.rank * Math.min( 5, q.swiftStacks ) );
	}

	/** the share of a blow this weapon softens, summed with the armor's and the
	 *  artifact's and capped once in {@link Quality#mitigation} */
	public float guardFraction( Quality q, Weapon w ){
		if (this != GUARD || w == null) return 0f;
		int tier = w instanceof MeleeWeapon ? ((MeleeWeapon) w).tier : 1;
		float f = 0.02f * q.type.rank + 0.004f * (tier + Math.max( 0, w.buffedLvl() ));
		//standing your ground sets the guard, rather than moving breaking it outright
		if (q.type.alpha()) f *= 1f + 0.25f * Math.min( 1f, SkillInteractions.turnsStill() / 2f );
		return Math.min( 0.15f, f );
	}

	/** riposte: an alpha Guard that has held its ground turns the next blow aside whole
	 *  and answers it. Returns what is left of the blow */
	public float riposte( Quality q, Weapon w, Hero hero, float damage, Object src ){
		if (this != GUARD || !q.type.alpha() || damage <= 0 || !(src instanceof Char)) return damage;
		if (SkillInteractions.turnsStill() < 2) return damage;
		float cooldown = 15f - 3f * q.type.rank;
		float now = Actor.now();
		if (now < q.twistReady && q.twistReady - now <= cooldown) return damage;
		q.twistReady = now + cooldown;
		Quality.reflect( (Char) src, hero, Math.round( damage * 0.25f * q.type.rank ) );
		if (hero.sprite != null) hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( WeaponPerk.class, "riposte" ) );
		return 0f;
	}

	/** the share of the target's armor this weapon's blows ignore, summed with the hero's
	 *  rings and capped once in {@link Quality#drPierce} */
	public float pierce( Quality q, Weapon w, Hero hero, Char defender ){
		int r = q.type.rank;
		switch (this){
			case CRUSH: {
				float f = Math.min( 0.45f, 0.12f * r );
				//sunder's stacks ride the same channel, so armor is never shredded twice
				if (q.type.alpha() && defender != null && q.shredTarget == defender.id() && Actor.now() < q.shredUntil){
					f += 0.05f * r * q.shredStacks;
				}
				return f;
			}
			case KEEN:
				return defender instanceof Mob && ((Mob) defender).surprisedBy( hero, true )
						? Math.min( 0.45f, 0.12f * r ) : 0f;
			case SHOT:
				return q.type.alpha() && defender != null
						&& Dungeon.level.distance( hero.pos, defender.pos ) >= 6 - r
						? Math.min( 0.40f, 0.10f * r ) : 0f;
			default:
				return 0f;
		}
	}

	/** alpha Marksman: a shot taken from far enough out cannot miss, and the window widens
	 *  with the type */
	public boolean sureHit( Quality q, Char owner, Char target ){
		return this == SHOT && q.type.alpha() && target != null
				&& Dungeon.level.distance( owner.pos, target.pos ) >= 6 - q.type.rank;
	}

	/** an attack that takes no time: Flurry's fifth hit, Momentum's kill, Ambush's execute */
	public boolean consumeFreeAttack( Quality q ){
		if ((this == SWIFT || this == MOMENTUM || this == KEEN) && q.freeNext){
			q.freeNext = false;
			return true;
		}
		return false;
	}

	/** a missed swing: the ramps that reward landing blows lose what they built */
	public void loseStacks( Quality q ){
		if (this == SWIFT) q.swiftStacks = 0;
		else if (this == MOMENTUM) q.hitCount = 0;
	}

	/** kills: Momentum carries what it built on to the next enemy */
	public void onKill( Quality q, Hero hero ){
		if (!q.type.alpha()) return;
		if (this == MOMENTUM && q.hitCount >= 3){
			q.freeNext = true;
			q.hitTarget = -1;
		}
	}

	private void crit( Char ch ){
		if (ch.sprite != null) ch.sprite.showStatus( CharSprite.NEGATIVE, Messages.get( WeaponPerk.class, "crit" ) );
		burst( ch, 4 );
	}

	private static void burst( Char ch, int n ){
		if (ch != null && ch.sprite != null && ch.sprite.visible) ch.sprite.emitter().burst( Speck.factory( Speck.STAR ), n );
	}
}

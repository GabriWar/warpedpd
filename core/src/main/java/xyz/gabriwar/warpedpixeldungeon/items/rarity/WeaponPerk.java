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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bless;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Daze;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Disarmed;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.TypeFx;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.quest.Pickaxe;
import xyz.gabriwar.warpedpixeldungeon.items.wands.Wand;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfBlastWave;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.SpiritBow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.AssassinsBlade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Axe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.BattleAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Bible;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Crossbow;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.FightGloves;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flute;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greataxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Harp;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Katana;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MageBook;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MageStaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MagesStaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MasonsHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Greatshield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MiniGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Quarterstaff;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Rapier;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.RoundShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Saber;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.ShootGun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shovel;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sickle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Triangolo;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Trumpet;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarScythe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Wardrum;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Whip;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WoodcuttersAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.ChainFlail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.ChainWhip;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.LanceNShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.MeisterHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.ObsidianShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.SpearNShield;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.UnformedBlade;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.alchemy.UnholyBible;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.BowWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.gun.Gun;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.CromCruachAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.LokisFlail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Bolas;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ForestDart;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Kunai;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.MissileWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.SmallChakram;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * The type perk of a weapon. Groups follow what the weapon is: its class where the class
 * says it (guns, bows, casters' weapons, shields and parrying blades, scythes, axes, hammers
 * and digging tools, instruments, whips, flails, assassins' blades, fists), its stats for the
 * rest: guns reload on the run, bows and the other launchers shoot, weapons that block guard,
 * scythes sweep and reap, axes splinter armor, hammers and picks daze, instruments play
 * chords, whips drag, long reach keeps enemies at bay, fast weapons flurry, slow or steady
 * ones crush, erratic ones land wild blows, light blades strike from hiding, casters'
 * weapons drain mana, and the rest build momentum. Thrown weapons split into darts,
 * boomerangs, light throws and heavy throws. Each perk scales x1 / x2 / x3 with the type;
 * alpha adds the group's twist. The full table is docs/weapon-types.md.
 */
public enum WeaponPerk {

	//KEEN is titled "Ambush" and THROW "Keen": saved by name, so they cannot be renamed
	//appended only: the rival statue saves its perk by name
	SHOT, GUARD, REACH, SWIFT, CRUSH, WILD, KEEN, SIPHON, MOMENTUM,
	DART, THROW, IMPACT,
	GUN, BOOMERANG, SCYTHE,
	WOODSMAN, QUARRY, BARD, LASH;

	public static WeaponPerk of( Item item ){
		if (item instanceof MissileWeapon){
			if (item instanceof Dart || item instanceof ForestDart) return DART;
			if (item instanceof HeavyBoomerang || item instanceof JupitersWraith) return BOOMERANG;
			//a sneak knife, a light entangler and a chakram that leaps back are light throws
			if (item instanceof Kunai || item instanceof Bolas || item instanceof SmallChakram) return THROW;
			return ((MissileWeapon) item).tier >= 3 ? IMPACT : THROW;
		}
		if (!(item instanceof Weapon)) return MOMENTUM;
		Weapon w = (Weapon) item;
		if (w instanceof Gun || w instanceof MiniGun || w instanceof ShootGun) return GUN;
		if (w instanceof BowWeapon || w instanceof Crossbow || w instanceof SpiritBow
				|| !(w instanceof MeleeWeapon)) return SHOT;
		if (w instanceof MagesStaff || w instanceof MageStaff || w instanceof MageBook
				|| w instanceof Bible || w instanceof UnholyBible) return SIPHON;
		//shields are pinned: the stance weapons report no defence in their attack stance. The
		//parrying blades are pinned too, so the group never reads a hero (the catalog has none)
		if (w instanceof RoundShield || w instanceof Greatshield || w instanceof ObsidianShield
				|| w instanceof LanceNShield || w instanceof SpearNShield
				|| w instanceof Katana || w instanceof Rapier || w instanceof Saber || w instanceof Quarterstaff) return GUARD;
		if (w instanceof Sickle || w instanceof WarScythe) return SCYTHE;
		if (w instanceof WoodcuttersAxe || w instanceof HandAxe || w instanceof Axe || w instanceof BattleAxe
				|| w instanceof Greataxe || w instanceof CromCruachAxe) return WOODSMAN;
		//the spade and the miner's tool are shovels
		if (w instanceof Pickaxe || w instanceof MasonsHammer || w instanceof WarHammer
				|| w instanceof MeisterHammer || w instanceof Shovel) return QUARRY;
		if (w instanceof Triangolo || w instanceof Flute || w instanceof Wardrum
				|| w instanceof Trumpet || w instanceof Harp) return BARD;
		if (w instanceof Whip || w instanceof ChainWhip) return LASH;
		if (w instanceof Flail || w instanceof ChainFlail || w instanceof LokisFlail) return WILD;
		if (w instanceof AssassinsBlade || w instanceof UnformedBlade) return KEEN;
		if (w instanceof FightGloves) return SWIFT;
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
		String s = Messages.get( this, name().toLowerCase(), title(), amount( item, type ), extra( item, type ),
				extra2( type ) );
		if (type.alpha()) s += " " + Messages.get( this, name().toLowerCase() + "_twist", extra( item, type ) );
		return s;
	}

	/** the number shown for this item, from the same coefficients the live hooks use, so a
	 *  printed number can never drift from the applied one */
	String amount( Item item, ItemType type ){
		int r = type.rank;
		Weapon w = item instanceof Weapon ? (Weapon) item : null;
		int lvl = w == null ? 0 : Math.max( 0, w.buffedLvl() );
		switch (this){
			case SHOT:      return pct( 0.15f * r );
			case REACH:     return pct( 0.10f * r );
			case WILD:      return pct( 0.10f * r );
			case MOMENTUM:  return pct( 0.05f * r * 4 );
			case KEEN:      return pct( 0.15f * r );
			case IMPACT:    return pct( 0.50f * r );
			case GUN:       return pct( 0.15f * r );
			case BOOMERANG: return Integer.toString( r );
			case SCYTHE:    return pct( 0.15f * r );
			case DART:      return pct( 0.30f );
			case THROW:     return pct( 0.15f * r );
			case SIPHON:    return pct( 0.04f * r );
			case CRUSH:     return pct( 0.05f * r );
			case WOODSMAN:  return pct( 0.06f * r );
			case QUARRY:    return pct( 0.10f * r );
			case BARD:      return Integer.toString( 1 + r );
			case LASH:      return pct( 0.06f * r );
			case SWIFT:
				return pct( Math.min( 0.30f, 0.04f * r + 0.005f * lvl + 0.01f * r * 5 ) );
			case GUARD: {
				int tier = w instanceof MeleeWeapon ? ((MeleeWeapon) w).tier : 1;
				return pct( Math.min( 0.15f, 0.035f * r + 0.004f * (tier + lvl) ) );
			}
		}
		return "";
	}

	/** the second number a perk line shows, where it has one (a pierce, a window, a cap) */
	String extra( Item item, ItemType type ){
		int r = type.rank;
		switch (this){
			case CRUSH: case KEEN: return pct( Math.min( 0.45f, 0.12f * r ) );
			case SHOT:             return Integer.toString( 6 - r );
			case WILD:             return pct( 0.3f + 0.2f * r );
			case IMPACT:           return pct( 0.40f );
			case GUN:              return pct( 0.10f * r );
			case BOOMERANG:        return pct( 0.25f * r );
			case SCYTHE:           return pct( 0.20f * r );
			case DART:             return pct( 0.15f * r );
			case MOMENTUM:         return pct( 0.05f * r );
			case SWIFT:            return Integer.toString( r );
			case THROW:            return pct( 0.25f * r );
			case SIPHON:           return Integer.toString( r );
			case GUARD:            return pct( 0.25f * r );
			//the gathering half: the share of strokes a felling or quarrying job is spared
			case WOODSMAN: case QUARRY: return pct( 0.20f * r );
			case LASH:             return pct( 0.15f * r );
			default:               return "";
		}
	}

	/** the third number, where a line has one: Ambush's sense radius, and the share of
	 *  felled trees or broken stone a Woodsman's or a Quarry's tool gives one more of */
	String extra2( ItemType type ){
		int r = type.rank;
		switch (this){
			case KEEN:                  return Messages.get( WeaponPerk.class, r == 1 ? "tile" : "tiles", r );
			case WOODSMAN: case QUARRY: return pct( 0.15f * r );
			default:                    return "";
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
			case SHOT: case GUN:
				//the further the shot, the more it is worth, up to its own ceiling at five tiles
				if (dist >= 3){
					damage = Math.round( damage * (1f + Math.min( 0.15f * r, 0.06f * r + 0.03f * r * (dist - 2) )) );
					burst( defender );
					TypeFx.longshot( attacker, defender, dist );
				}
				return damage;
			case REACH:
				//a blow at the weapon's length: the same bonus at every range past one tile
				if (dist >= 2){
					damage = Math.round( damage * (1f + 0.10f * r) );
					burst( defender );
					TypeFx.reach( attacker, defender );
				}
				return damage;
			case SWIFT:
				//the ramp the speed bonus reads; a miss drops it, see loseStacks
				q.swiftStacks = Math.min( 5, q.swiftStacks + 1 );
				if (alpha && ++q.hitCount >= 5){
					q.hitCount = 0;
					q.freeNext = true;
					burst( hero );
					TypeFx.flurry( hero, defender );
				}
				return damage;
			case CRUSH:
				damage = Math.round( damage * (1f + 0.05f * r) );
				if (alpha){
					//sunder: the armour it has already broken open, remembered per target
					if (q.shredTarget != defender.id() || Actor.now() >= q.shredUntil) q.shredStacks = 0;
					q.shredTarget = defender.id();
					boolean added = q.shredStacks < 3;
					q.shredStacks = Math.min( 3, q.shredStacks + 1 );
					q.shredUntil = Actor.now() + 3f + r;
					burst( defender );
					TypeFx.sunder( attacker, defender, added );
				}
				return damage;
			case WILD:
				if (Random.Float() < 0.10f * r){
					//the payoff grows with the type too, not only how often it lands
					damage = Math.round( damage * (1.3f + 0.2f * r) );
					crit( q, attacker, defender );
					if (alpha) Buff.affect( defender, Bleeding.class ).set( Math.max( 1, Math.round( damage * 0.10f * r ) ) );
				}
				return damage;
			case KEEN:
				if (defender instanceof Mob && ((Mob) defender).surprisedBy( attacker, true )){
					damage = Math.round( damage * (1f + 0.15f * r) );
					crit( q, attacker, defender );
					//execute: a surprise blow finishes anything already on its last legs
					if (alpha && !Char.hasProp( defender, Char.Property.BOSS )
							&& !Char.hasProp( defender, Char.Property.MINIBOSS )
							&& defender.HP - damage <= Math.round( defender.HT * (0.05f + 0.03f * r) )){
						damage = xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.to( defender, xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step.ATTACKER, xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step.SET, xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.EXECUTE, damage, Math.max( damage, defender.HP ) );
						xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.executed( defender );
						q.freeNext = true;
						Blow.chip( defender, FloatingText.EXECUTE, 0 );
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
						TypeFx.siphon( hero, defender );
						if (hero.sprite != null) hero.sprite.showStatusWithIcon( TypeFx.MANA, "+" + mana, FloatingText.MANA );
					} else if (alpha){
						//overflow: a full pool spills into a barrier instead of being thrown away
						Buff.affect( hero, Barrier.class ).incShield( mana );
					}
				}
				return damage;
			}
			case MOMENTUM: {
				//the stacks a kill carried over land on whoever is struck next; a new enemy
				//otherwise starts the count over
				if (q.hitTarget == CARRY) q.hitTarget = defender.id();
				else if (q.hitTarget != defender.id()){ q.hitTarget = defender.id(); q.hitCount = 0; }
				boolean full = q.hitCount >= 4;
				//this hit counts too: the first one in a row is already a stack
				q.hitCount = Math.min( 4, q.hitCount + 1 );
				damage = Math.round( damage * (1f + 0.05f * r * q.hitCount) );
				if (full) burst( defender );
				TypeFx.momentum( attacker, defender, q.hitCount );
				return damage;
			}
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
					crit( q, attacker, defender );
					//true flight: the throw that lands keenest flies back to the hand that threw it
					q.returnNext = true;
				}
				return damage;
			case IMPACT: {
				//a heave: a throw with a second turn of the hero's weight behind it
				//only on the one it was thrown at: a Force Cube's blast strikes its neighbours
				//first, and they keep their plain share
				Heave heave = hero.buff( Heave.class );
				if (heave == null || heave.cell != defender.pos) return damage;
				heave.detach();
				damage = Math.round( damage * (1f + 0.50f * r) );
				Blow.chip( defender, FloatingText.HEAVE, 0 );
				xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.heaveHit( attacker, defender, weapon );
				int wave = Math.round( damage * 0.40f );
				if (alpha && wave >= 1){
					//the shockwave: every enemy beside the target takes a share of the blow
					ArrayList<Char> shaken = beside( defender.pos, attacker, defender );
					for (Char ch : shaken) Quality.splash( attacker, ch, wave );
					xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.shockwave( attacker, defender, weapon, shaken );
				}
				return damage;
			}
			case BOOMERANG:
				//a strike on the way back is worth a share of a hit (HeavyBoomerang.CircleBack)
				if (q.returnStrike) damage = Math.round( damage * 0.25f * r );
				return damage;
			case SCYTHE: {
				//the sweep: every other enemy beside the wielder takes a share of the blow
				int share = Math.round( damage * 0.15f * r );
				if (share < 1) return damage;
				ArrayList<Char> swept = beside( attacker.pos, attacker, defender );
				for (Char ch : swept){
					Quality.splash( attacker, ch, share );
					if (alpha && ch.isAlive()) Buff.affect( ch, Bleeding.class ).set( Math.max( 1, share / 2 ) );
				}
				xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.sweep( attacker, defender, weapon, swept );
				return damage;
			}
			case WOODSMAN:
				//splinter: every hit on the same enemy opens its armor a little more, read by
				//pierce on the blows after it (the armor roll comes before this hook). Its bark flies on
				//every hit, as Crush's sunder flakes, the chip only when a stack is added
				if (q.shredTarget != defender.id() || Actor.now() >= q.shredUntil) q.shredStacks = 0;
				q.shredTarget = defender.id();
				q.shredUntil = Actor.now() + 3f + r;
				boolean added = q.shredStacks < 3;
				if (added) q.shredStacks++;
				TypeFx.splinter( attacker, defender, added );
				//the felling blow: every third hit on the same enemy throws it back. Only an alpha
				//counts, so the count never runs on a weapon without the twist
				if (alpha){
					if (q.hitTarget != defender.id()){ q.hitTarget = defender.id(); q.hitCount = 0; }
					if (++q.hitCount >= 3){
						q.hitCount = 0;
						//no collision damage, so the throw draws nothing
						TypeFx.fell( attacker, defender );
						WandOfBlastWave.throwChar( defender, new Ballistica( defender.pos,
								defender.pos + (defender.pos - attacker.pos), Ballistica.MAGIC_BOLT ), 2, true, false, weapon, TypeFx.WOOD );
					}
				}
				return damage;
			case QUARRY:
				//daze: drawn only for an enemy that can be dazed, never a boss
				if (!Char.hasProp( defender, Char.Property.BOSS ) && Random.Float() < 0.10f * r){
					Buff.prolong( defender, Daze.class, 2f );
					if (alpha) crack( attacker, defender );
					else TypeFx.daze( attacker, defender );
				}
				return damage;
			case BARD:
				//every landed hit is a note and every fourth note a chord: a rally, then a dread.
				//Nothing here is left to chance
				if (++q.notes >= 4){
					q.notes = 0;
					chord( q, hero, defender, alpha, r );
					q.dreadNext = !q.dreadNext;
				}
				//the note in the colour of the chord coming (of the one it played, when it played one)
				TypeFx.note( hero, q.notes == 0 ? !q.dreadNext : q.dreadNext );
				return damage;
			case LASH:
				if (dist >= 2){
					//the crack of the lash: a blow at its full length lands harder, and may drag
					damage = Math.round( damage * (1f + 0.06f * r) );
					drag( weapon, attacker, defender, r );
				}
				//the disarm: any landed hit, drawn after the drag's
				if (alpha && !Char.hasProp( defender, Char.Property.BOSS ) && Random.Float() < 0.20f){
					Buff.prolong( defender, Disarmed.class, Disarmed.DURATION );
					TypeFx.unarm( attacker, defender );
				}
				return damage;
			default:
				return damage;
		}
	}

	//Quarry's cracked floor: the dazed enemy and every enemy beside it are crippled
	private static void crack( Char attacker, Char defender ){
		ArrayList<Char> crippled = beside( defender.pos, attacker, defender );
		crippled.add( 0, defender );
		for (Char ch : crippled) Buff.prolong( ch, Cripple.class, 3f );
		TypeFx.crack( attacker, defender, crippled );
	}

	//Bard's chord, played by the hero: an alpha's charm first, then the rally or the dread.
	//The characters are taken by id, so the outcome never depends on the order they are kept in
	private static void chord( Quality q, Hero hero, Char defender, boolean alpha, int r ){
		ArrayList<Char> near = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch.isAlive() && Dungeon.level.distance( hero.pos, ch.pos ) <= 3) near.add( ch );
		}
		near.sort( Comparator.comparingInt( Char::id ) );

		//the charm: the enemy with the least health left, the first by id on a tie
		Char charmed = null;
		if (alpha){
			for (Char ch : near){
				if (ch.alignment != Char.Alignment.ENEMY || Char.hasProp( ch, Char.Property.BOSS )
						|| Char.hasProp( ch, Char.Property.MINIBOSS ) || ch.isImmune( Charm.class )) continue;
				if (charmed == null || ch.HP < charmed.HP) charmed = ch;
			}
			if (charmed != null){
				Charm c = Buff.prolong( charmed, Charm.class, 5f );
				c.object = hero.id();
				//the blow that played the chord must not break it
				if (charmed == defender) c.ignoreNextHit = true;
			}
		}

		//who the chord reaches, gathered first and then played on, so the charmed one, the blessed
		//and the terrified are all known by the end of the chord
		ArrayList<Char> blessed = new ArrayList<>(), terrified = new ArrayList<>();
		if (!q.dreadNext){
			//the rally: the hero, its allies and any other hero within 3 tiles are blessed
			for (Char ch : near){
				if (ch.alignment == Char.Alignment.ALLY) blessed.add( ch );
			}
			for (Char ch : blessed) Buff.prolong( ch, Bless.class, 1 + r );
		} else {
			//the dread: every enemy within 2 tiles is terrified, bosses and the charmed one aside
			for (Char ch : near){
				if (ch == charmed || ch.alignment != Char.Alignment.ENEMY || Dungeon.level.distance( hero.pos, ch.pos ) > 2
						|| Char.hasProp( ch, Char.Property.BOSS ) || Char.hasProp( ch, Char.Property.MINIBOSS )) continue;
				terrified.add( ch );
			}
			for (Char ch : terrified){
				Terror t = Buff.prolong( ch, Terror.class, 1 + r );
				t.object = hero.id();
				if (ch == defender) t.ignoreNextHit = true;
			}
		}
		TypeFx.chord( hero, defender, hero.belongings.attackingWeapon(), q.dreadNext, blessed, terrified, charmed );
	}

	//Lash's drag: the enemy struck at the whip's length is pulled one tile in and crippled. Every
	//gate is checked before the draw, so a pull that could never happen rolls nothing
	private static void drag( Weapon weapon, Char attacker, Char defender, int r ){
		if (Char.hasProp( defender, Char.Property.BOSS ) || defender.rooted
				|| Char.hasProp( defender, Char.Property.IMMOVABLE )) return;
		Ballistica b = new Ballistica( defender.pos, attacker.pos, Ballistica.PROJECTILE );
		if (b.dist < 1) return;
		int next = b.path.get( 1 );
		if (!Dungeon.level.passable[next] || Dungeon.level.pit[next] || Actor.findChar( next ) != null
				|| (Char.hasProp( defender, Char.Property.LARGE ) && !Dungeon.level.openSpace[next])) return;
		if (Random.Float() >= 0.15f * r) return;
		WandOfBlastWave.throwChar( defender, b, 1, false, false, weapon );
		Buff.prolong( defender, Cripple.class, 2f );
		TypeFx.lash( attacker, defender );
	}

	//the living enemies on the 8 cells around a cell, the attacker and the struck target aside
	private static ArrayList<Char> beside( int cell, Char attacker, Char defender ){
		ArrayList<Char> out = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar( cell + n );
			if (ch != null && ch != attacker && ch != defender && ch.isAlive()
					&& ch.alignment == Char.Alignment.ENEMY) out.add( ch );
		}
		return out;
	}

	/** Keen: a share of throws land at their keenest */
	public int throwRoll( Quality q, int damage, int max ){
		if (this != THROW) return damage;
		//every throw starts clean: a return earned and never flown (a vial that reformed in
		//the pack) must not carry over to the next
		q.returnNext = false;
		//the payoff is taken in onHit as a multiplier: clamping to max() here was a no-op
		//on any hero with strength to spare
		q.keenHit = Random.Float() < 0.15f * q.type.rank;
		return damage;
	}

	/** Heave: whether this thrown weapon can be heaved. An unidentified one would give its
	 *  perk away, which its type tab keeps hidden until then */
	public static boolean heaveable( Item item ){
		return item instanceof MissileWeapon && item.quality != null && item.isIdentified()
				&& of( item ) == IMPACT;
	}

	/** Arc: how many more enemies a typed boomerang's throw flies through, its type rank;
	 *  0 for anything else */
	public static int arcRank( Item item ){
		Quality q = Quality.of( item );
		return q != null && of( item ) == BOOMERANG ? q.type.rank : 0;
	}

	/** Arc: how many turns a boomerang takes to come back, 2 at alpha instead of 5 */
	public static int returnTurns( Item item ){
		Quality q = Quality.of( item );
		return q != null && q.type.alpha() && of( item ) == BOOMERANG ? 2 : 5;
	}

	/** Keen alpha: a throw at a character up to 3 tiles away reaches it through walls */
	public static boolean wallThrow( Item item ){
		Quality q = Quality.of( item );
		return q != null && q.type.alpha() && of( item ) == THROW;
	}

	/** An armed heave (AC_HEAVE): the throw it is spent on takes a turn longer and lands
	 *  harder. One turn long, so a heave that never lands cannot linger */
	public static class Heave extends FlavourBuff {
		public static final float DURATION = 1f;
		/** the cell the heaved throw comes down on, set as it lands (MissileWeapon.aimHeave):
		 *  only the blow at whoever stands there is heaved. Not saved, as a save cannot
		 *  fall between a throw landing and its blows */
		public int cell = -1;
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
		float f = 0.035f * q.type.rank + 0.004f * (tier + Math.max( 0, w.buffedLvl() ));
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
		TypeFx.parry( (Char) src, hero, Math.round( damage ) );
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
			case WOODSMAN:
				//the splinters on the one enemy it has been chopping at, while they last
				return defender != null && q.shredTarget == defender.id() && Actor.now() < q.shredUntil
						? 0.06f * r * q.shredStacks : 0f;
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

	//Momentum's target after a kill at three stacks or more: the next one struck, whoever it is,
	//takes the stacks over instead of starting them again
	private static final int CARRY = -2;

	/** kills: Momentum carries what it built on to the next enemy */
	public void onKill( Quality q, Hero hero ){
		if (!q.type.alpha()) return;
		if (this == MOMENTUM && q.hitCount >= 3){
			q.freeNext = true;
			q.hitTarget = CARRY;
		}
	}

	/** a critical hit on this blow, a perk's or the CRIT line's: marks it so the blow crits once.
	 *  It raises no word: its look over the struck says it */
	static void crit( Quality q, Char attacker, Char defender ){
		q.critBlow = true;
		xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger.crit( defender );
		burst( defender );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceFx.crit( attacker, defender );
	}

	//the old Speck bursts drew the game's Random once as their emitter started; the pictures are the hit card's now,
	//so this keeps that one draw where it was (Emitter.start: one Random.Float(interval) whatever the quantity)
	private static void burst( Char ch ){
		if (ch != null && ch.sprite != null && ch.sprite.visible) Random.Float();
	}
}

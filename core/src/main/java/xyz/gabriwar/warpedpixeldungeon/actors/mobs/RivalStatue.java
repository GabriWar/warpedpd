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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.WeaponPerk;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.RivalStatueSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * The animated statue of the Rival's Gallery. It carries a weapon of the hero's own
 * perk group (WeaponPerk.of), one rarity above theirs and already alpha - and because a
 * weapon's perk only fires in a hero's hands, the statue plays the alpha perk itself:
 * the same coefficients at rank three, and the group's alpha twist, turned on the hero.
 * So the fight is the tutorial for the twist, and the weapon it drops is the twist.
 *
 * What each group does here follows WeaponPerk.onHit and its _twist line:
 *
 *   Marksman  fights from up to six tiles, hits harder the further off, cannot miss from three
 *   Guard     sheds 15% of every blow; two turns unmoved and the next one is thrown back
 *   Reach     strikes one tile further than the weapon is long, harder with every tile
 *   Flurry    swings faster the longer its blows keep landing; every fifth costs no time
 *   Crush     hits 15% harder and leaves armor sundered on the third blow in a row
 *   Wild      three blows in ten come out wild, nearly double, and open a bleed
 *   Ambush    its first blow, and any blow on a hero who cannot act, lands 45% harder
 *   Siphon    every hit drains mana, and what it drains becomes its own shielding
 *   Momentum  each blow in a row is 12% heavier than the last, five deep
 */
public class RivalStatue extends Statue {

	{
		spriteClass = RivalStatueSprite.class;

		//the gallery's centrepiece is a sterner thing than a corridor statue
		HP = HT = Math.round( (15 + WarpedRooms.threat() * 5) * 1.25f );
		defenseSkill = 4 + WarpedRooms.threat() * 2;
		EXP = 6 + WarpedRooms.threat() * 2;

		levelGenStatue = false;
	}

	private static final int RANK = ItemType.ALPHA.rank;

	private WeaponPerk perk = WeaponPerk.MOMENTUM;

	private int streak = 0;          //blows landed in a row
	private int heldTurns = 0;       //turns spent on the same cell (Guard)
	private int lastPos = -1;
	private boolean freeSwing = false;
	private boolean opened = false;  //the ambush blow is spent

	/** Arms the statue against this hero: a weapon of their own perk group, a rarity
	 *  above what they carry (legendary at most), alpha. Decks are left alone - the search
	 *  throws most of what it rolls away. */
	public void armAgainst( Hero hero ){
		Item held = hero == null ? null : hero.belongings.weapon();
		WeaponPerk wanted = held instanceof Weapon ? WeaponPerk.of( held ) : WeaponPerk.MOMENTUM;
		//a bow cannot be put in stone hands: its rival is whatever reaches furthest
		if (wanted == WeaponPerk.SHOT || wanted == WeaponPerk.DART
				|| wanted == WeaponPerk.THROW || wanted == WeaponPerk.IMPACT){
			wanted = WeaponPerk.SHOT;
		}

		//rolled for the floor's real threat, not its number (the test floor sits at 86)
		int floorSet = WarpedRooms.threat() / 5;
		MeleeWeapon found = null;
		for (int tries = 0; tries < 120 && found == null; tries++){
			MeleeWeapon w = Generator.randomWeapon( floorSet, true );
			WeaponPerk group = WeaponPerk.of( w );
			if (group == wanted || (wanted == WeaponPerk.SHOT && group == WeaponPerk.REACH)){
				found = w;
			}
		}
		if (found == null) found = Generator.randomWeapon( floorSet, true );

		Quality mine = Quality.of( held );
		Rarity rarity = WarpedRooms.stepAbove( mine == null ? Rarity.COMMON : mine.rarity );

		found.cursed = false;
		WarpedRooms.forceQuality( found, rarity, ItemType.ALPHA );
		if (found.enchantment == null) found.enchant( Weapon.Enchantment.random() );

		weapon = found;
		//it fights the way the weapon in its hands fights, which is also what it drops
		perk = WeaponPerk.of( found );
	}

	public WeaponPerk perk(){
		return perk;
	}

	// ---------------------------------------------------------------- reach and range

	@Override
	protected boolean canAttack( Char enemy ){
		if (super.canAttack( enemy )) return true;
		int dist = Dungeon.level.distance( pos, enemy.pos );
		if (perk == WeaponPerk.SHOT){
			return dist <= 6
					&& new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE ).collisionPos == enemy.pos;
		}
		if (perk == WeaponPerk.REACH){
			//the alpha twist: one tile further than its length allows
			return dist <= weapon.reachFactor( this ) + 1
					&& new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE ).collisionPos == enemy.pos;
		}
		return false;
	}

	@Override
	public int attackSkill( Char target ){
		if (perk == WeaponPerk.SHOT && target != null
				&& Dungeon.level.distance( pos, target.pos ) >= 6 - RANK){
			return INFINITE_ACCURACY;
		}
		return (int) ((9 + WarpedRooms.threat()) * weapon.accuracyFactor( this, target ));
	}

	// ---------------------------------------------------------------- tempo

	@Override
	public float attackDelay(){
		if (freeSwing){
			freeSwing = false;
			return 0f;
		}
		float delay = super.attackDelay();
		if (perk == WeaponPerk.SWIFT){
			delay *= 1f - Math.min( 0.30f, 0.04f * RANK + 0.01f * RANK * streak );
		}
		return delay;
	}

	//a rival made any other way than by its gallery (the debug spawner, a summon) is
	//armed the same way, against whoever the hero is
	@Override
	public void createWeapon( boolean useDecks ){
		armAgainst( Dungeon.hero );
	}

	@Override
	protected boolean act(){
		if (weapon == null) armAgainst( Dungeon.hero );
		if (pos == lastPos) heldTurns++;
		else heldTurns = 0;
		lastPos = pos;
		return super.act();
	}

	// ---------------------------------------------------------------- the blow

	@Override
	public int drRoll(){
		return Random.NormalIntRange( 0, WarpedRooms.threat() + weapon.defenseFactor( this ) );
	}

	@Override
	public int attackProc( Char enemy, int damage ){
		int dist = Dungeon.level.distance( pos, enemy.pos );
		float mult = 1f;
		switch (perk){
			case SHOT:
				if (dist >= 3) mult += Math.min( 0.15f * RANK, 0.06f * RANK + 0.02f * RANK * (dist - 2) );
				break;
			case REACH:
				if (dist >= 2) mult += Math.min( 0.18f * RANK, 0.06f * RANK * (dist - 1) );
				break;
			case CRUSH:
				mult += 0.05f * RANK;
				if (streak >= 2){
					Buff.prolong( enemy, Vulnerable.class, 3f );
					say( enemy, "sunder" );
				}
				break;
			case WILD:
				if (Random.Float() < 0.10f * RANK){
					mult += 0.3f + 0.2f * RANK;
					Buff.affect( enemy, Bleeding.class ).set( Math.max( 1, damage / 4 ) );
					say( enemy, "wild" );
				}
				break;
			case KEEN:
				if (!opened || helpless( enemy )){
					mult += 0.15f * RANK;
					say( enemy, "ambush" );
				}
				break;
			case SIPHON:
				if (enemy instanceof Hero){
					Hero h = (Hero) enemy;
					int drained = Math.min( h.MP, Math.max( 1, Math.round( h.MT * 0.04f * RANK ) ) );
					if (drained > 0){
						h.MP -= drained;
						Buff.affect( this, Barrier.class ).incShield( drained * 2 );
						say( enemy, "siphon" );
					}
				}
				break;
			case MOMENTUM:
				mult += 0.04f * RANK * Math.min( 5, streak );
				break;
			default:
				break;
		}
		opened = true;

		streak++;
		if (perk == WeaponPerk.SWIFT && streak % 5 == 0) freeSwing = true;

		return super.attackProc( enemy, Math.round( damage * mult ) );
	}

	private static boolean helpless( Char ch ){
		return ch.paralysed > 0 || ch.buff( Paralysis.class ) != null
				|| ch.buff( Blindness.class ) != null || ch.buff( Roots.class ) != null;
	}

	private void say( Char over, String key ){
		if (over.sprite != null && Dungeon.level.heroFOV[over.pos]){
			over.sprite.showStatus( CharSprite.WARNING, Messages.get( RivalStatue.class, key ) );
		}
	}

	//a blow that did not land breaks whatever was building
	@Override
	public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ){
		boolean hit = super.attack( enemy, dmgMulti, dmgBonus, accMulti );
		if (!hit) streak = 0;
		return hit;
	}

	// ---------------------------------------------------------------- the guard

	@Override
	public int defenseProc( Char enemy, int damage ){
		if (perk == WeaponPerk.GUARD && damage > 0){
			if (heldTurns >= 2){
				//held its ground: the blow is turned aside whole and three quarters of it
				//comes straight back
				heldTurns = 0;
				int back = Math.round( damage * 0.25f * RANK );
				if (enemy != null && enemy.isAlive() && back > 0
						&& Dungeon.level.adjacent( pos, enemy.pos )){
					enemy.damage( back, this );
					say( enemy, "riposte" );
					if (enemy == Dungeon.hero && !enemy.isAlive()){
						Dungeon.fail( this );
					}
				}
				return super.defenseProc( enemy, 0 );
			}
			damage = Math.round( damage * (1f - 0.15f) );
		}
		return super.defenseProc( enemy, damage );
	}

	// ---------------------------------------------------------------- everything else

	@Override
	public Notes.Landmark landmark(){
		return null;
	}

	@Override
	public String description(){
		String desc = Messages.get( this, "desc" );
		if (weapon != null){
			desc += "\n\n" + Messages.get( this, "desc_weapon", weapon.name(), perk.title() );
			desc += "\n\n" + perk.describe( weapon, ItemType.ALPHA );
		}
		return desc;
	}

	private static final String PERK    = "perk";
	private static final String STREAK  = "streak";
	private static final String HELD    = "held_turns";
	private static final String LASTPOS = "last_pos";
	private static final String OPENED  = "opened";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( PERK, perk );
		bundle.put( STREAK, streak );
		bundle.put( HELD, heldTurns );
		bundle.put( LASTPOS, lastPos );
		bundle.put( OPENED, opened );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		perk = bundle.getEnum( PERK, WeaponPerk.class );
		streak = bundle.getInt( STREAK );
		heldTurns = bundle.getInt( HELD );
		lastPos = bundle.getInt( LASTPOS );
		opened = bundle.getBoolean( OPENED );
	}
}

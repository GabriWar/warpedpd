/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.HallsKey;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.RelicMeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.sprites.SentinelSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class MineSentinel extends Mob {

	{
		spriteClass = SentinelSprite.class;

		HP = HT = 500 + Dungeon.depth * 20;
		defenseSkill = 20;

		EXP = 25;

		state = PASSIVE;

		properties.add( Property.INORGANIC );

		immunities.add( Terror.class );
		immunities.add( Amok.class );
		immunities.add( Charm.class );
		immunities.add( Sleep.class );
		immunities.add( Burning.class );
		immunities.add( ToxicGas.class );
		immunities.add( Vertigo.class );
		immunities.add( Vampiric.class );
		immunities.add( Grim.class );
		immunities.add( ScrollOfPsionicBlast.class );

		resistances.add( Poison.class );

		// HallsKey drop is handled conditionally in dropExtraLoot() (depth 24 only)

		//no metabolism to disturb: only the extremes reach it
		thermal = Thermal.INSENSATE;
	}

	private static final int REGENERATION = 100;

	protected MeleeWeapon weapon;

	public MineSentinel() {
		super();

		weapon = (MeleeWeapon) Generator.random( Generator.Category.WEAPON );
		weapon.cursed = false;
		weapon.enchant();
		weapon.upgrade( 10 );
		weapon.identify();
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public int damageRoll() {
		if (weapon != null) {
			return weapon.damageRoll( this ) * 2;
		}
		return Random.NormalIntRange( 10, 20 );
	}

	@Override
	public int attackSkill( Char target ) {
		return (int) ((30 + Dungeon.depth * 2) * (weapon != null ? weapon.accuracyFactor(this, target) : 1f));
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 3 );
	}

	@Override
	public float attackDelay() {
		return super.attackDelay() * (weapon != null ? weapon.delayFactor( this ) : 1f);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (weapon != null) {
			damage = weapon.proc( this, enemy, damage );
		}
		return damage;
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (state == PASSIVE) {
			state = HUNTING;
		}

		if (!(src instanceof RelicMeleeWeapon) && !(src instanceof JupitersWraith)) {
			dmg = Random.Int( 1, Math.max( 1, Math.round( dmg * 0.25f ) ) );
		}
		super.damage( dmg, src );
	}

	@Override
	public boolean reset() {
		state = PASSIVE;
		return true;
	}

	@Override
	protected boolean act() {
		if (state == HUNTING) {
			// one neighbouring sentinel at a time is nudged awake
			for (int n : PathFinder.NEIGHBOURS8) {
				int cell = pos + n;
				Char ch = Actor.findChar( cell );
				if (ch instanceof MineSentinel && Random.Int( 10 ) < 2) {
					MineSentinel sentinel = (MineSentinel) ch;
					sentinel.damage( 1, this );
					if (sentinel.state == sentinel.PASSIVE) {
						sentinel.state = sentinel.HUNTING;
					}
					break;
				}
			}
		}

		if (Dungeon.hero != null && !heroNear() && Random.Float() < 0.50f && state == HUNTING) {
			ArrayList<Integer> spawnPoints = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8) {
				int cell = Dungeon.hero.pos + n;
				if (Actor.findChar( cell ) == null
						&& (Dungeon.level.passable[cell] || Dungeon.level.avoid[cell])) {
					spawnPoints.add( cell );
				}
			}
			if (!spawnPoints.isEmpty()) {
				blink( Random.element( spawnPoints ) );
			}
		} else if (HP < HT / 4 && Random.Float() < 0.50f && state != PASSIVE) {
			int newPos = Dungeon.level.randomRespawnCell( this );
			if (newPos != -1) {
				blink( newPos );
				HP = Math.min( HP + REGENERATION, HT );
			}
		}

		return super.act();
	}

	private void blink( int newPos ) {
		if (Dungeon.level.heroFOV[pos]) {
			CellEmitter.get( pos ).start( Speck.factory( Speck.LIGHT ), 0.2f, 3 );
		}
		ScrollOfTeleportation.appear( this, newPos );
	}

	private boolean heroNear() {
		return Dungeon.level.distance( pos, Dungeon.hero.pos ) <= 2;
	}

	@Override
	protected boolean extraLootIgnoresLevel() {
		return true;
	}

	@Override
	protected void dropExtraLoot() {
		trackedDrop(weapon, 0);
		if (!Dungeon.LimitedDrops.HALLS_KEY.dropped() && Dungeon.depth == 65) {
			Dungeon.LimitedDrops.HALLS_KEY.drop();
			trackedDrop(new HallsKey(), 1);
		}
	}

	@Override
	public void beckon( int cell ) {
		// Do nothing
	}

	private static final String WEAPON = "weapon";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( WEAPON, weapon );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		weapon = (MeleeWeapon) bundle.get( WEAPON );
	}
}

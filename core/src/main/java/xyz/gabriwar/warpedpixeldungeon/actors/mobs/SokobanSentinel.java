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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.sprites.SokobanSentinelSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class SokobanSentinel extends Mob {

	{
		spriteClass = SokobanSentinelSprite.class;

		HP = HT = 500;
		defenseSkill = 40;
		baseSpeed = 0.5f;

		EXP = 18;

		state = PASSIVE;

		properties.add( Property.INORGANIC );

		resistances.add( ToxicGas.class );
		resistances.add( Poison.class );
		resistances.add( Grim.class );

		immunities.add( Vampiric.class );
	}

	protected MeleeWeapon weapon;

	public SokobanSentinel() {
		super();

		weapon = (MeleeWeapon) Generator.random( Generator.Category.WEAPON );
		weapon.cursed = false;
		weapon.enchant();
		weapon.upgrade( 5 );
		weapon.identify();
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public int damageRoll() {
		if (weapon != null) {
			return weapon.damageRoll( this );
		}
		return Random.NormalIntRange( 10, 20 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 40;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, Dungeon.depth );
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
	protected boolean act() {
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView(this, fieldOfView);
		enemy = chooseEnemy();
		if (state == HUNTING
				&& (enemy == null || !enemy.isAlive() || !fieldOfView[enemy.pos] || enemy.invisible > 0)) {
			state = WANDERING;
		} else if (state == PASSIVE
				&& enemy != null && enemy.isAlive() && fieldOfView[enemy.pos] && enemy.invisible <= 0) {
			state = HUNTING;
		}
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (state == PASSIVE) {
			state = HUNTING;
		}
		super.damage( dmg, src );
	}

	@Override
	public void die( Object cause ) {
		// Sprouted intentionally does NOT drop the weapon
		super.die( cause );
	}

	@Override
	public void beckon( int cell ) {
		//ignores beckon
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

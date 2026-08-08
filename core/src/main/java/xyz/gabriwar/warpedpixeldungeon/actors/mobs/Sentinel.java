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
import xyz.gabriwar.warpedpixeldungeon.sprites.SentinelSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Sentinel extends Mob {

	{
		spriteClass = SentinelSprite.class;

		HP = HT = 15 + Dungeon.depth * 8;
		defenseSkill = 4 + Dungeon.depth * 2;

		EXP = 18;

		state = PASSIVE;

		properties.add(Property.INORGANIC);

		resistances.add(ToxicGas.class);
		resistances.add(Poison.class);
		resistances.add(Grim.class);

		immunities.add(Vampiric.class);
	}

	protected MeleeWeapon weapon;

	public Sentinel() {
		super();

		weapon = (MeleeWeapon) Generator.random(Generator.Category.WEAPON);
		weapon.cursed = false;
		weapon.enchant();
		weapon.upgrade(3);
		weapon.identify();
	}

	@Override
	public int damageRoll() {
		if (weapon != null) {
			return weapon.damageRoll(this);
		}
		return Random.NormalIntRange(10, 20);
	}

	@Override
	public int attackSkill(Char target) {
		return (int) ((9 + Dungeon.depth) * (weapon != null ? weapon.accuracyFactor(this, target) : 1f));
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, Dungeon.depth);
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public void damage(int dmg, Object src) {
		if (state == PASSIVE) {
			state = HUNTING;
		}
		super.damage(dmg, src);
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
	}

	@Override
	protected void dropExtraLoot() {
		explodeDew(pos);
		trackedDrop(weapon, 0);
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		if (weapon != null) {
			damage = weapon.proc(this, enemy, damage);
		}
		return damage;
	}

	@Override
	public float attackDelay() {
		return super.attackDelay() * (weapon != null ? weapon.delayFactor(this) : 1f);
	}

	@Override
	public void beckon(int cell) {
		// Do nothing, always ignores beckon
	}

	@Override
	public boolean reset() {
		state = PASSIVE;
		return true;
	}

	private static final String WEAPON = "weapon";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(WEAPON, weapon);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		weapon = (MeleeWeapon) bundle.get(WEAPON);
	}
}

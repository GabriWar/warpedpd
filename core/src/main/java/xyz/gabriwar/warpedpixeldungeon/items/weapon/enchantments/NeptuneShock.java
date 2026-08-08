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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.RelicMeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class NeptuneShock extends Weapon.Enchantment {

	private static ItemSprite.Glowing BLUE = new ItemSprite.Glowing(0x0044FF);
	private static final int CHARGE_COST = 10;

	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		if (!(weapon instanceof RelicMeleeWeapon)) {
			return damage;
		}

		RelicMeleeWeapon relic = (RelicMeleeWeapon) weapon;

		if (relic.charge < CHARGE_COST) {
			return damage;
		}
		relic.charge -= CHARGE_COST;

		int level = Math.max(0, weapon.buffedLvl());
		int distance = 1 + level * 2 + level()/2; //scales with enchantment level

		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		boolean procced = false;

		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (Dungeon.level.distance(attacker.pos, mob.pos) < distance
					&& mob.isAlive()
					&& mob.alignment == Char.Alignment.ENEMY
					&& Random.Int(10) < 5) {

				arcs.add(new Lightning.Arc(attacker.sprite.center(), mob.sprite.center()));

				mob.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
				mob.sprite.flash();

				int mobDmg = Math.round(damage * power()); //scales with enchantment level
				if (Dungeon.level.water[mob.pos] && !mob.flying) {
					mobDmg *= 2;
				}

				if (mob.isAlive()) {
					if (mobDmg < mob.HP) {
						mob.damage(mobDmg, this);
					} else {
						mob.damage(mob.HP - 2, this);
						Buff.prolong(mob, Paralysis.class,
								Random.Float(1, 1.5f + level) * power()); //scales with enchantment level
					}
				}

				procced = true;
			}
		}

		if (procced) {
			attacker.sprite.parent.addToFront(new Lightning(arcs, null));
			Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
			Camera.main.shake(2, 0.3f);
		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return BLUE;
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.relic.RelicMeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.JupitersWraith;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.OtilukeSprite;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class Otiluke extends Mob implements Callback {

	{
		spriteClass = OtilukeSprite.class;

		HP = HT = 7000;
		defenseSkill = 50;

		EXP = 101;

		state = PASSIVE;

		loot = Generator.Category.POTION;
		lootChance = 0.83f;

		properties.add(Property.BOSS);
		properties.add(Property.INORGANIC);

		immunities.add(Terror.class);
		immunities.add(Amok.class);
		immunities.add(Charm.class);
		immunities.add(Sleep.class);
		immunities.add(Burning.class);
		immunities.add(ToxicGas.class);
		immunities.add(Vertigo.class);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(250, 350);
	}

	@Override
	public int attackSkill(Char target) {
		return 250;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 8);
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public void notice() {
		super.notice();
		yell(Messages.get(this, "notice"));
	}

	@Override
	protected boolean canAttack(Char enemy) {
		return new Ballistica(pos, enemy.pos, Ballistica.STOP_SOLID).collisionPos == enemy.pos;
	}

	@Override
	protected boolean doAttack(Char enemy) {
		if (Dungeon.level.adjacent(pos, enemy.pos)) {
			return super.doAttack(enemy);
		} else {
			boolean visible = Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[enemy.pos];
			if (visible) {
				sprite.zap(enemy.pos);
			}
			spend(attackDelay());

			if (hit(this, enemy, true)) {
				int dmg = Random.Int(100, 160);
				enemy.damage(dmg, this);
				if (Random.Int(2) == 0) {
					Buff.prolong(enemy, Weakness.class, Weakness.DURATION);
				}
			} else {
				enemy.sprite.showStatus(CharSprite.NEUTRAL, enemy.defenseVerb());
			}
			return !visible;
		}
	}

	@Override
	public void damage(int dmg, Object src) {
		if (state == PASSIVE) {
			state = HUNTING;
		}

		if (state == HUNTING) {
			for (Mob mob : Dungeon.level.mobs) {
				if (mob instanceof MineSentinel && Random.Int(20) < 2) {
					if (mob.state == mob.PASSIVE) {
						mob.damage(1, this);
						mob.state = mob.HUNTING;
					}
					break;
				}
			}
		}

		// Reduce non-relic damage to 25%
		if (!(src instanceof RelicMeleeWeapon) && !(src instanceof JupitersWraith)) {
			dmg = Random.Int(1, Math.max(1, Math.round(dmg * 0.25f)));
		}

		super.damage(dmg, src);
	}

	@Override
	public void die(Object cause) {
		Dungeon.level.unseal();
		super.die(cause);
	}

	public void onZapComplete() {
		next();
	}

	@Override
	public void call() {
		next();
	}
}

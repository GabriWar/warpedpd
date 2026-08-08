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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.pets;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.SugarplumFairySprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class SugarplumFairy extends PET implements Callback {

	private static final float TIME_TO_ZAP = 2f;

	{
		spriteClass = SugarplumFairySprite.class;
		flying = true;
		type = 11;
		level = 1;
		cooldown = 1000;
		regen = 1;
		regenChance = 0.2f;
	}

	@Override
	public float attackDelay() {
		return 0.5f;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, level * 5);
	}

	@Override
	public void adjustStats(int level) {
		this.level = level;
		HT = level * 10;
		HP = Math.min(HP, HT);
		defenseSkill = 5 + level * level;
	}

	@Override
	public int attackSkill(Char target) {
		return defenseSkill;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(defenseSkill / 2, defenseSkill);
	}

	@Override
	protected boolean act() {
		if (cooldown > 0) {
			cooldown = Math.max(cooldown - (level * level), 0);
			if (cooldown == 0) {
				yell(Messages.get(this, "ready"));
			}
		}
		if (cooldown == 0 && Dungeon.level.adjacent(pos, Dungeon.hero.pos) && Random.Int(1) == 0) {
			int healAmt = Random.Int(level * level);
			if (healAmt > 0) {
				Dungeon.hero.HP = Math.min(Dungeon.hero.HT, Dungeon.hero.HP + healAmt);
				Dungeon.hero.sprite.emitter().start(Speck.factory(Speck.HEALING), 0.4f, 1);
				Dungeon.hero.sprite.showStatus(0x00FF00, Integer.toString(healAmt));
				if (Dungeon.level.heroFOV[pos]) {
					GLog.p(Messages.get(this, "heal"));
				}
			}

			if (Random.Float() < 0.05f) {
				Dungeon.hero.earnExp(5, this.getClass());
				cooldown = 1000;
				if (Dungeon.level.heroFOV[pos]) {
					GLog.p(Messages.get(this, "exp"));
				}
			}

			if (Random.Float() < 0.01f) {
				Dungeon.hero.HT++;
				cooldown = 1000;
				if (Dungeon.level.heroFOV[pos]) {
					GLog.p(Messages.get(this, "ht_boost"));
				}
			}
		}
		return super.act();
	}

	@Override
	public boolean canAttack(Char enemy) {
		Ballistica bolt = new Ballistica(pos, enemy.pos, Ballistica.MAGIC_BOLT);
		return bolt.collisionPos == enemy.pos;
	}

	@Override
	protected boolean doAttack(Char enemy) {
		if (Dungeon.level.adjacent(pos, enemy.pos)) {
			return super.doAttack(enemy);
		} else {
			spend(TIME_TO_ZAP);

			if (Dungeon.level.heroFOV[pos]) {
				GLog.w(Messages.get(this, "zap"));
			}

			if (sprite != null && sprite.visible) {
				sprite.parent.addToFront(new Lightning(pos, enemy.pos, this));
			}

			if (enemy != null && enemy.isAlive()) {
				if (Char.hit(this, enemy, true)) {
					int dmg = damageRoll() * 2;
					if (Dungeon.level.water[enemy.pos] && !enemy.flying) {
						dmg = (int) (dmg * 1.5f);
					}
					enemy.damage(dmg, new Electricity());

					CellEmitter.center(enemy.pos).burst(SparkParticle.FACTORY, 3);
				} else {
					enemy.sprite.showStatus(0xFFFFFF, enemy.defenseVerb());
				}
			}

			return true;
		}
	}

	@Override
	public void call() {
		next();
	}
}

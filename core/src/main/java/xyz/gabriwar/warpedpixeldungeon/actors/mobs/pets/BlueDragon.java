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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.BlueDragonSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.Calendar;

public class BlueDragon extends PET implements Callback {

	private static final float TIME_TO_ZAP = 1f;

	{
		spriteClass = BlueDragonSprite.class;
		flying = true;
		type = 7;
		level = 1;
		cooldown = 1000;
		regen = 1;
		regenChance = 0.1f;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, level * 4);
	}

	@Override
	public void adjustStats(int level) {
		this.level = level;
		HT = (3 + level) * 12;
		HP = Math.min(HP, HT);
		defenseSkill = 1 + level * level;
	}

	@Override
	public int attackSkill(Char target) {
		return defenseSkill;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(HT / 5, HT / 2);
	}

	@Override
	protected boolean act() {
		cooldown = Math.max(cooldown - (level * level), 0);
		if (cooldown == 0 && Dungeon.level.heroFOV[pos]) {
			int month = Calendar.getInstance().get(Calendar.MONTH);
			if (month == Calendar.DECEMBER) {
				GLog.p(Messages.get(this, "ready_december"));
			} else {
				GLog.p(Messages.get(this, "ready"));
			}
		}
		return super.act();
	}

	@Override
	public boolean canAttack(Char enemy) {
		if (cooldown > 0) {
			return Dungeon.level.adjacent(pos, enemy.pos);
		} else {
			Ballistica bolt = new Ballistica(pos, enemy.pos, Ballistica.MAGIC_BOLT);
			return bolt.collisionPos == enemy.pos;
		}
	}

	@Override
	protected boolean doAttack(Char enemy) {
		if (Dungeon.level.adjacent(pos, enemy.pos)) {
			return super.doAttack(enemy);
		} else {
			if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
				((BlueDragonSprite) sprite).zap(enemy.pos);
				return false;
			} else {
				zap();
				return true;
			}
		}
	}

	private void zap() {
		spend(TIME_TO_ZAP);
		cooldown = 1000;

		if (Dungeon.level.heroFOV[pos]) {
			int month = Calendar.getInstance().get(Calendar.MONTH);
			if (month == Calendar.DECEMBER) {
				GLog.w(Messages.get(this, "zap_december"));
			} else {
				GLog.w(Messages.get(this, "zap"));
			}
		}

		if (enemy != null && enemy.isAlive()) {
			if (Char.hit(this, enemy, true)) {
				int dmg = damageRoll() * 2;
				enemy.damage(dmg, this);

				Buff.prolong(enemy, Frost.class, Frost.DURATION * Random.Float(1f, 1.5f));
			} else {
				enemy.sprite.showStatus(0xFFFFFF, enemy.defenseVerb());
			}
		}
	}

	public void onZapComplete() {
		zap();
		next();
	}

	@Override
	public void call() {
		next();
	}
}

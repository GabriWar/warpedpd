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
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.items.VioletDewdrop;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GraveProtectorSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class GraveProtector extends Mob implements Callback {

	private static final float TIME_TO_ZAP = 2f;

	{
		spriteClass = GraveProtectorSprite.class;
		HP = HT = 55;
		defenseSkill = 15;
		EXP = 10;
		state = HUNTING;
		flying = true;

		loot = new VioletDewdrop();
		lootChance = 1f;

		resistances.add(Electricity.class);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(8 + Math.round(Statistics.skeletonsKilled / 10), 15 + Math.round(Statistics.skeletonsKilled / 5));
	}

	@Override
	public int attackSkill(Char target) {
		return 20;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 8);
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
			spend(TIME_TO_ZAP);

			if (hit(this, enemy, true)) {
				int dmg = Random.NormalIntRange(10 + Math.round(Statistics.skeletonsKilled / 10), 25 + Math.round(Statistics.skeletonsKilled / 5));
				if (Dungeon.level.water[enemy.pos] && !enemy.flying) {
					dmg *= 1.5f;
				}
				enemy.damage(dmg, this);
				enemy.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
				enemy.sprite.flash();

				if (enemy == Dungeon.hero) {
					Camera.main.shake(2, 0.3f);
					if (!enemy.isAlive()) {
						Dungeon.fail(this);
						GLog.n(Messages.get(this, "lightning_kill"));
					}
				}
			} else {
				enemy.sprite.showStatus(CharSprite.NEUTRAL, enemy.defenseVerb());
			}
			return !visible;
		}
	}

	@Override
	public void call() {
		next();
	}

	@Override
	public void notice() {
		super.notice();
		yell(Messages.get(this, "notice"));
	}

	@Override
	public float spawningWeight() {
		return 0;
	}
}

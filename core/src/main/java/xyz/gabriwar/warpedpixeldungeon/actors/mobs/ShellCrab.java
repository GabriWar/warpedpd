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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.AscensionChallenge;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.items.food.Berry;
import xyz.gabriwar.warpedpixeldungeon.items.keys.GoldenSkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HermitCrabSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class ShellCrab extends Mob implements Callback {

	private static final float TIME_TO_ZAP = 2f;

	{
		spriteClass = HermitCrabSprite.class;

		HP = HT = 20;
		defenseSkill = 22;

		EXP = 6;

		loot = Berry.class;
		lootChance = 0.33f;

		properties.add(Property.ELECTRIC);

		declareExtraLoot(GoldenSkeletonKey.class, 1f);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(10, 20);
	}

	@Override
	public int attackSkill( Char target ) {
		return 25;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 4);
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (dmg > HT / 4 && !(src instanceof DM100.LightningBolt)) {
			for (Mob mob : Dungeon.level.mobs) {
				if (mob instanceof Shell && mob.isAlive()) {
					Dungeon.shellCharge += dmg;
					GLog.n(Messages.get(this, "shell_absorb"));
					GLog.n(Messages.get(this, "shell_charge", dmg));
					dmg = 1;
					break;
				}
			}
		}
		super.damage(dmg, src);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return super.canAttack(enemy)
				|| new Ballistica(pos, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos == enemy.pos;
	}

	@Override
	protected boolean doAttack( Char enemy ) {

		if (Dungeon.level.adjacent(pos, enemy.pos)
				|| new Ballistica(pos, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos != enemy.pos) {

			return super.doAttack(enemy);

		} else {

			spend(TIME_TO_ZAP);

			Invisibility.dispel(this);
			if (hit(this, enemy, true)) {
				int dmg = Random.NormalIntRange(15, 30);
				dmg = Math.round(dmg * AscensionChallenge.statModifier(this));
				if (Dungeon.level.water[enemy.pos] && !enemy.flying) {
					dmg = Math.round(dmg * 1.5f);
				}
				enemy.damage(dmg, new DM100.LightningBolt());

				if (enemy.sprite.visible) {
					enemy.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
					enemy.sprite.flash();
				}

				if (enemy == Dungeon.hero) {
					PixelScene.shake(2, 0.3f);

					if (!enemy.isAlive()) {
						Dungeon.fail(this);
						GLog.n(Messages.get(this, "zap_kill"));
					}
				}
			} else {
				enemy.sprite.showStatus(CharSprite.NEUTRAL, enemy.defenseVerb());
			}

			if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
				sprite.zap(enemy.pos);
				return false;
			} else {
				return true;
			}
		}
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);
	}

	@Override
	protected void dropExtraLoot() {
		if (Random.Int(1) == 0) {
			trackedDrop(new GoldenSkeletonKey(Dungeon.depth), 0);
		}
	}

	@Override
	public void call() {
		next();
	}
}

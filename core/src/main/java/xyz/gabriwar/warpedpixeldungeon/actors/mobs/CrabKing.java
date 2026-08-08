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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Doom;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.AdamantArmor;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CrabKingSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

public class CrabKing extends Mob {

	private static final int JUMP_DELAY = 5;
	private int timeToJump = JUMP_DELAY;

	{
		spriteClass = CrabKingSprite.class;

		HP = HT = 300;
		defenseSkill = 30;
		baseSpeed = 2f;

		EXP = 20;

		properties.add(Property.BOSS);

		resistances.add(ToxicGas.class);
		resistances.add(Poison.class);
		resistances.add(Grim.class);
		resistances.add(Doom.class);

		declareExtraLoot(AdamantArmor.class, 1f);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(20, 50);
	}

	@Override
	public int attackSkill(Char target) {
		return 35;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 10);
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	protected boolean canAttack(Char enemy) {
		return new Ballistica(pos, enemy.pos, Ballistica.STOP_SOLID).collisionPos == enemy.pos;
	}

	@Override
	protected boolean act() {
		if (HP < HT) {
			int regen = Math.round(Dungeon.shellCharge / 10);
			if (regen > 0) {
				HP = Math.min(HT, HP + regen);
				Dungeon.shellCharge -= regen;
				if (Dungeon.shellCharge < 0) Dungeon.shellCharge = 0;
				if (Dungeon.level.heroFOV[pos]) {
					sprite.emitter().burst(Speck.factory(Speck.HEALING), 1);
				}
			}
		}
		return super.act();
	}

	@Override
	public void die(Object cause) {
		GameScene.bossSlain();
		Dungeon.crabkingkilled = true;
		super.die(cause);
		yell( Messages.get(this, "die") );
	}

	@Override
	protected void dropExtraLoot() {
		trackedDrop(new Gold(Random.IntRange(1900, 4000)), 0);
		trackedDrop(new AdamantArmor(), 1);
	}

	@Override
	public void notice() {
		super.notice();
		if (enemy == null) return;
		yell( Messages.get(this, "notice") );
	}

	@Override
	protected boolean getCloser( int target ) {
		if (enemy != null && fieldOfView[target]) {
			jump();
			return true;
		} else {
			return super.getCloser(target);
		}
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		timeToJump--;
		if (timeToJump <= 0 && Dungeon.level.adjacent(pos, enemy.pos)) {
			jump();
			return true;
		} else {
			return super.doAttack(enemy);
		}
	}

	private void jump() {
		if (enemy == null) return;
		timeToJump = JUMP_DELAY;

		int newPos = -1;
		for (int i = 0; i < 20; i++) {
			int candidate = Random.Int(Dungeon.level.length());
			if (Dungeon.level.passable[candidate]
					&& Dungeon.level.heroFOV[candidate]
					&& Actor.findChar(candidate) == null
					&& !Dungeon.level.adjacent(candidate, enemy.pos)) {
				newPos = candidate;
				break;
			}
		}

		if (newPos != -1) {
			if (Dungeon.level.heroFOV[pos]) {
				CellEmitter.get(pos).burst(Speck.factory(Speck.WOOL), 6);
			}
			sprite.move(pos, newPos);
			move(newPos);
			if (Dungeon.level.heroFOV[newPos]) {
				CellEmitter.get(newPos).burst(Speck.factory(Speck.WOOL), 6);
			}
			Sample.INSTANCE.play(Assets.Sounds.PUFF);
			spend(1 / speed());
		}
	}
}

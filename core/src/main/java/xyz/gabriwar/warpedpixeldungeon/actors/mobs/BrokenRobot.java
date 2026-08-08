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
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PurpleParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.RedDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRecharging;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.BrokenRobotSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class BrokenRobot extends Mob implements Callback {

	private static final float SPAWN_DELAY = 2f;

	{
		spriteClass = BrokenRobotSprite.class;

		HP = HT = 75 + (Dungeon.depth * Random.NormalIntRange(1, 2));
		defenseSkill = 20;

		EXP = 13;

		loot = new ScrollOfRecharging();
		lootChance = 0.5f;

		state = HUNTING;

		immunities.add(Terror.class);
		immunities.add(ToxicGas.class);

		resistances.add(Grim.class);
		resistances.add(Vampiric.class);

		declareExtraLoot(RedDewdrop.class, 0.5f);
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(10, 20);
	}

	@Override
	public int attackSkill( Char target ) {
		return 20;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 5);
	}

	@Override
	public float attackDelay() {
		return 3f;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		Ballistica beam = new Ballistica(pos, enemy.pos, Ballistica.STOP_SOLID);
		return beam.path.contains(enemy.pos);
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		spend(attackDelay());

		Ballistica beam = new Ballistica(pos, enemy.pos, Ballistica.STOP_SOLID);

		boolean rayVisible = false;
		for (int cell : beam.path) {
			if (Dungeon.level.heroFOV[cell]) {
				rayVisible = true;
				break;
			}
		}

		if (rayVisible) {
			sprite.attack(beam.collisionPos);
		}

		// Piercing beam — hits all chars along the ray
		for (int i = 1; i < beam.path.size(); i++) {
			int cell = beam.path.get(i);
			Char ch = Actor.findChar(cell);
			if (ch == null) continue;

			if (hit(this, ch, true)) {
				ch.damage(Random.NormalIntRange(10, 12), this);

				if (Dungeon.level.heroFOV[cell]) {
					ch.sprite.flash();
					CellEmitter.center(cell).burst(PurpleParticle.BURST,
							Random.IntRange(1, 2));
				}
			} else {
				ch.sprite.showStatus(CharSprite.NEUTRAL, ch.defenseVerb());
			}
		}

		return !rayVisible;
	}

	@Override
	protected boolean act() {
		if (enemySeen) {
			if (Random.Int(50) == 0) {
				GLog.n( Messages.get(this, "malfunction") );
				explode(pos);
				if (HP < 1) {
					die(this);
				}
				return true;
			}
		}
		return super.act();
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);
	}

	@Override
	protected void dropExtraLoot() {
		if (Random.Float() < 0.5f) {
			trackedDrop(new RedDewdrop(), 0);
		}
	}

	public void explode( int cell ) {
		Sample.INSTANCE.play( Assets.Sounds.BLAST, 2 );

		if (Dungeon.level.heroFOV[cell]) {
			CellEmitter.center(cell).burst(BlastParticle.FACTORY, 30);
		}

		boolean terrainAffected = false;
		for (int n : PathFinder.NEIGHBOURS9) {
			int c = cell + n;
			if (c >= 0 && c < Dungeon.level.length()) {
				if (Dungeon.level.heroFOV[c]) {
					CellEmitter.get(c).burst(SmokeParticle.FACTORY, 4);
				}

				if (Dungeon.level.flamable[c]) {
					Dungeon.level.destroy(c);
					GameScene.updateMap(c);
					terrainAffected = true;
				}

				// destroys items / triggers bombs caught in the blast
				Heap heap = Dungeon.level.heaps.get(c);
				if (heap != null) {
					heap.explode();
				}

				Char ch = Actor.findChar(c);
				if (ch != null) {
					int minDamage = c == cell ? Dungeon.depth + 5 : 1;
					int maxDamage = 10 + Dungeon.depth * 2;

					int dmg = Random.NormalIntRange(minDamage, maxDamage) - ch.drRoll();
					if (dmg > 0) {
						ch.damage(dmg, this);
					}

					if (ch == this && HP < 1) {
						die(this);
					}
				}
			}
		}

		if (terrainAffected) {
			Dungeon.observe();
		}
	}

	public static void spawnAround( int pos ) {
		for (int n : PathFinder.NEIGHBOURS4) {
			int cell = pos + n;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) {
				spawnAt(cell);
			}
		}
	}

	public static BrokenRobot spawnAt( int pos ) {
		BrokenRobot b = new BrokenRobot();
		b.pos = pos;
		b.state = b.HUNTING;
		GameScene.add(b, SPAWN_DELAY);
		return b;
	}

	@Override
	public void call() {
		next();
	}
}

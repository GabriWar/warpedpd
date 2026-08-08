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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Doom;
import xyz.gabriwar.warpedpixeldungeon.items.RedDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.ShellSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Shell extends Mob implements Callback {

	{
		spriteClass = ShellSprite.class;

		HP = HT = 600;
		defenseSkill = 0;

		EXP = 25;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.ELECTRIC);

		loot = RedDewdrop.class;
		lootChance = 1f;

		immunities.add(ToxicGas.class);
		immunities.add(Terror.class);

		resistances.add(Grim.class);
		resistances.add(Doom.class);
		resistances.add(Electricity.class);
	}

	@Override
	public int damageRoll() {
		return 0;
	}

	@Override
	public int attackSkill(Char target) {
		return 100;
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
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	public void beckon(int cell) {
		// ignore
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
			// Ranged lightning attack using shellCharge
			yell("ZZZZZAAAAAAPPPPPP!!!!!");
			if (Dungeon.shellCharge > 0 && Char.hit(this, enemy, true)) {
				int dmg = Random.Int(
						Math.round(Dungeon.shellCharge / 4f),
						Math.max(Math.round(Dungeon.shellCharge / 4f) + 1, Math.round(Dungeon.shellCharge / 2f)));
				Dungeon.shellCharge -= dmg;
				if (Dungeon.shellCharge < 0) Dungeon.shellCharge = 0;

				if (Dungeon.level.water[enemy.pos] && !enemy.flying) {
					dmg = Math.round(dmg * 1.5f);
				}
				enemy.damage(dmg, this);
				enemy.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
				enemy.sprite.flash();
			}
			Camera.main.shake(2, 0.3f);
			zapAllMobs();
			spend(2f);
			return true;
		}
	}

	@Override
	public void damage(int dmg, Object src) {
		super.damage(dmg, src);
		if (Dungeon.shellCharge > 0) {
			zapAdjacentCells();
		}
	}

	@Override
	protected boolean act() {
		if (Dungeon.shellCharge > 20 && Random.Int(Dungeon.shellCharge) > 20 && Dungeon.hero.isAlive()) {
			zapAllMobs();
		}
		return super.act();
	}

	private void zapAdjacentCells() {
		for (int n : PathFinder.NEIGHBOURS8) {
			int cell = pos + n;
			Char ch = Actor.findChar(cell);
			if (ch != null && ch.isAlive()) {
				int dmg;
				if (ch instanceof Hero) {
					// Hero gets heavier shellCharge-based damage
					dmg = Random.Int(Dungeon.shellCharge, Math.max(Dungeon.shellCharge + 1, Dungeon.shellCharge * 2));
					Dungeon.shellCharge = Math.max(0, Dungeon.shellCharge - 1);
				} else {
					dmg = Random.Int(1, 2 + Math.round(Dungeon.shellCharge / 4f));
				}
				if (Dungeon.level.water[ch.pos] && !ch.flying) {
					dmg = Math.round(dmg * 1.5f);
				}
				ch.damage(dmg, this);
				ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
				ch.sprite.flash();
			}
		}
		Camera.main.shake(2, 0.3f);
	}

	private void zapAllMobs() {
		int mobDmg = Random.Int(1, 2 + Math.round(Dungeon.shellCharge / 4f));
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob != this && mob.isAlive()) {
				int dmg = mobDmg;
				if (Dungeon.level.water[mob.pos] && !mob.flying) {
					dmg = Math.round(dmg * 1.5f);
				}
				mob.damage(dmg, this);
			}
		}
		// Also zap hero if far enough
		Hero hero = Dungeon.hero;
		if (hero.isAlive() && !Dungeon.level.adjacent(pos, hero.pos)) {
			if (Dungeon.shellCharge > 0) {
				int heroDmg = Random.Int(
						Math.round(Dungeon.shellCharge / 4f),
						Math.max(Math.round(Dungeon.shellCharge / 4f) + 1, Math.round(Dungeon.shellCharge / 2f)));
				Dungeon.shellCharge -= heroDmg;
				if (Dungeon.shellCharge < 0) Dungeon.shellCharge = 0;

				if (Dungeon.level.water[hero.pos] && !hero.flying) {
					heroDmg = Math.round(heroDmg * 1.5f);
				}
				hero.damage(heroDmg, this);
			}
		}
	}

	@Override
	public void die(Object cause) {
		Dungeon.shellCharge = 0;
		super.die(cause);
	}

	@Override
	public void call() {
		next();
	}
}

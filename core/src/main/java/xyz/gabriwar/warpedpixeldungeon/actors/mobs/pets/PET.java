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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class PET extends Mob {

	{
		HP = HT = 1;
		EXP = 0;

		alignment = Alignment.ALLY;
		state = HUNTING;
	}

	public int level;
	public int kills;
	public int type;
	public int experience;
	public int cooldown;
	public int goaways = 0;
	public boolean callback = false;
	public boolean stay = false;

	protected int regen = 1;
	protected float regenChance = 0.1f;

	private static final String KILLS = "kills";
	private static final String LEVEL = "level";
	private static final String TYPE = "type";
	private static final String EXPERIENCE = "experience";
	private static final String COOLDOWN = "cooldown";
	private static final String GOAWAYS = "goaways";
	private static final String CALLBACK = "callback";
	private static final String STAY = "stay";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(KILLS, kills);
		bundle.put(LEVEL, level);
		bundle.put(TYPE, type);
		bundle.put(EXPERIENCE, experience);
		bundle.put(COOLDOWN, cooldown);
		bundle.put(GOAWAYS, goaways);
		bundle.put(CALLBACK, callback);
		bundle.put(STAY, stay);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		kills = bundle.getInt(KILLS);
		level = bundle.getInt(LEVEL);
		type = bundle.getInt(TYPE);
		experience = bundle.getInt(EXPERIENCE);
		cooldown = bundle.getInt(COOLDOWN);
		goaways = bundle.getInt(GOAWAYS);
		callback = bundle.getBoolean(CALLBACK);
		stay = bundle.getBoolean(STAY);
		adjustStats(level);
	}

	protected void throwItem() {
		Heap heap = Dungeon.level.heaps.get(pos);
		if (heap != null) {
			int n;
			do {
				n = pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
			} while (!Dungeon.level.passable[n] && !Dungeon.level.avoid[n]);
			Dungeon.level.drop(heap.pickUp(), n).sprite.drop(pos);
		}
	}

	public void adjustStats(int level) {
	}

	public void spawn(int level) {
		this.level = level;
		adjustStats(level);
	}

	public void syncToHero() {
		Hero hero = Dungeon.hero;
		if (hero != null) {
			hero.petType = type;
			hero.petLevel = level;
			hero.petKills = kills;
			hero.petHP = HP;
			hero.petExperience = experience;
			hero.petCooldown = cooldown;
		}
	}

	public void syncFromHero() {
		Hero hero = Dungeon.hero;
		if (hero != null) {
			level = hero.petLevel;
			kills = hero.petKills;
			HP = hero.petHP;
			experience = hero.petExperience;
			cooldown = hero.petCooldown;
			adjustStats(level);
		}
	}

	@Override
	public float speed() {
		float speed = super.speed();
		int hasteLevel = Math.min(Dungeon.petHasteLevel, 10);
		if (hasteLevel != 0) {
			speed *= (float) Math.pow(1.2, hasteLevel);
		}
		return speed;
	}

	@Override
	protected boolean act() {
		syncToHero();
		if (Random.Float() < regenChance && HP < HT) {
			HP += regen;
			HP = Math.min(HP, HT);
		}
		return super.act();
	}

	@Override
	public void damage(int dmg, Object src) {
		if (src instanceof Hero) {
			goaways++;
			GLog.n(Messages.get(this, "hit_warning"));
		}
		if (goaways > 2) {
			flee();
			return;
		}
		super.damage(dmg, src);
	}

	@Override
	public void die(Object cause) {
		syncToHero();
		Dungeon.hero.haspet = false;
		Dungeon.hero.petCount++;
		GLog.n(Messages.get(this, "dies"));
		super.die(cause);
	}

	public void flee() {
		Dungeon.hero.haspet = false;
		GLog.n(Messages.get(this, "flees"));
		destroy();
		sprite.killAndErase();
		CellEmitter.get(pos).burst(ElmoParticle.FACTORY, 6);
	}

	@Override
	protected Char chooseEnemy() {
		if (enemy != null && !enemy.isAlive() && enemy instanceof Mob) {
			kills++;
			experience += ((Mob) enemy).EXP;
		}

		if (experience >= level * (level + level) && level < 20) {
			level++;
			GLog.p(Messages.get(this, "level_up"));
			adjustStats(level);
			experience = 0;
		}

		if (enemy == null || !enemy.isAlive()) {
			HashSet<Mob> enemies = new HashSet<>();
			for (Mob mob : Dungeon.level.mobs) {
				if (mob.alignment == Alignment.ENEMY && Dungeon.level.heroFOV[mob.pos]) {
					enemies.add(mob);
				}
			}
			enemy = enemies.size() > 0 ? Random.element(enemies) : null;
		}

		return enemy;
	}

	@Override
	protected boolean getCloser(int target) {
		if (stay) {
			return false;
		}

		if (enemy != null && enemy.isAlive() && !callback) {
			target = enemy.pos;
		} else if (Dungeon.level.adjacent(pos, Dungeon.hero.pos)) {
			target = wanderLocation() != -1 ? wanderLocation() : Dungeon.hero.pos;
			callback = false;
		} else if (Dungeon.hero.invisible == 0) {
			target = Dungeon.hero.pos;
		} else {
			target = wanderLocation() != -1 ? wanderLocation() : pos;
		}

		return super.getCloser(target);
	}

	public int wanderLocation() {
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = pos + n;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null) {
				candidates.add(c);
			}
		}
		return candidates.size() > 0 ? Random.element(candidates) : -1;
	}

	@Override
	public void aggro(Char ch) {
	}

	@Override
	public void beckon(int cell) {
	}

	@Override
	public boolean interact(Char c) {
		if (this.buff(MagicalSleep.class) != null) {
			Buff.detach(this, MagicalSleep.class);
		}
		if (state == SLEEPING) {
			state = WANDERING;
		}
		if (buff(Paralysis.class) != null) {
			Buff.detach(this, Paralysis.class);
		}

		if (c == Dungeon.hero) {
			int curPos = pos;

			moveSprite(pos, Dungeon.hero.pos);
			move(Dungeon.hero.pos);

			Dungeon.hero.sprite.move(Dungeon.hero.pos, curPos);
			Dungeon.hero.move(curPos);

			Dungeon.hero.spend(1 / Dungeon.hero.speed());
			Dungeon.hero.busy();
		}
		return true;
	}
}

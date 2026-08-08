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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ConfusionGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.RedDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.keys.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.TowerSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Tower extends Mob implements Callback {

	{
		spriteClass = TowerSprite.class;

		HP = HT = 300 + Dungeon.depth * Random.NormalIntRange(2, 5);
		defenseSkill = 0;

		EXP = 25;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		loot = new RedDewdrop();
		lootChance = 1f;

		declareExtraLoot(SkeletonKey.class, 1f);
		declareExtraLoot(Gold.class, 1f);

		properties.add(Property.IMMOVABLE);

		immunities.add(ToxicGas.class);
		immunities.add(Terror.class);
		immunities.add(ConfusionGas.class);

		resistances.add(Electricity.class);
		resistances.add(ScrollOfPsionicBlast.class);
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	@Override
	public void beckon(int cell) {
		// Do nothing
	}

	@Override
	public int damageRoll() {
		return 0;
	}

	@Override
	public int attackSkill( Char target ) {
		return 0;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 10);
	}

	@Override
	protected boolean act() {
		switch (Random.Int(4)) {
			case 1:
				for (Mob mob : Dungeon.level.mobs) {
					if (mob instanceof Tower && mob != this) {
						mob.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
						mob.sprite.flash();
					}
				}
				break;
			case 2:
				if (Dungeon.level.mobs.size() < 10) {
					for (int n : PathFinder.NEIGHBOURS4) {
						int cell = pos + n;
						if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null) {
							BrokenRobot robot = new BrokenRobot();
							robot.pos = cell;
							robot.state = robot.HUNTING;
							GameScene.add(robot, 1f);
						}
					}
					GLog.n(Messages.get(this, "print_robots"));
				}
				break;
		}
		return super.act();
	}

	@Override
	public void damage(int dmg, Object src) {
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			mob.beckon(Dungeon.hero.pos);
		}

		GLog.w(Messages.get(this, "alert"));
		CellEmitter.center(pos).start(
				Speck.factory(Speck.SCREAM), 0.3f, 3);
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);

		super.damage(dmg, src);
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	public void call() {
		next();
	}

	@Override
	public void die(Object cause) {
		super.die(cause);

		explode(pos);
		dropExtraLoot();
	}

	@Override
	protected void dropExtraLoot() {
		explodeDew(pos);

		int bossAlive = 0;
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof Tower || mob instanceof DM300) {
				bossAlive++;
			}
		}

		if (bossAlive == 0) {
			GameScene.bossSlain();
			trackedDrop(new SkeletonKey(Dungeon.depth), 0);
			trackedDrop(new Gold(Random.IntRange(3000, 6000)), 1);
		}
	}

	public void explode(int cell) {
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 2);

		if (Dungeon.level.heroFOV[cell]) {
			CellEmitter.center(cell).burst(BlastParticle.FACTORY, 30);
		}

		for (int n : PathFinder.NEIGHBOURS8) {
			int c = cell + n;
			if (c >= 0 && c < Dungeon.level.length()) {
				if (Dungeon.level.heroFOV[c]) {
					CellEmitter.get(c).burst(SmokeParticle.FACTORY, 4);
				}

				if ((Terrain.flags[Dungeon.level.map[c]] & Terrain.FLAMABLE) != 0) {
					Dungeon.level.set(c, Terrain.EMBERS);
					GameScene.updateMap(c);
				}

				Char ch = Actor.findChar(c);
				if (ch != null) {
					int minDamage = c == cell ? Dungeon.depth + 5 : 1;
					int maxDamage = 10 + Dungeon.depth * 2;
					int dmg = Random.NormalIntRange(minDamage, maxDamage)
							- ch.drRoll();
					if (dmg > 0) {
						ch.damage(dmg, this);
					}
				}
			}
		}
	}
}

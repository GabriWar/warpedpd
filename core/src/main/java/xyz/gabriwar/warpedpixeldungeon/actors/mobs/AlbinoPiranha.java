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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BlobImmunity;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.items.ConchShell;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.AlbinoPiranhaSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class AlbinoPiranha extends Mob {

	{
		spriteClass = AlbinoPiranhaSprite.class;

		baseSpeed = 2f;

		EXP = 0;

		loot = MonsterMeat.class;
		lootChance = 0.005f;

		SLEEPING = new Sleeping();
		WANDERING = new Wandering();
		HUNTING = new Hunting();

		state = SLEEPING;

		declareExtraLoot(ConchShell.class, 0.1f);
	}

	public AlbinoPiranha() {
		super();

		HP = HT = 10 + Dungeon.depth * 5;
		defenseSkill = 10 + Dungeon.depth * 2;
	}

	@Override
	protected boolean act() {
		if (!Dungeon.level.water[pos]) {
			dieOnLand();
			return true;
		} else {
			return super.act();
		}
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( Dungeon.depth, 4 + Dungeon.depth * 2 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 20 + Dungeon.depth * 2;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, Dungeon.depth);
	}

	public void dieOnLand() {
		damage( HT, this );
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );

		Statistics.albinoPiranhasKilled++;
		GLog.i( Messages.get(this, "kill_count", Statistics.albinoPiranhasKilled) );
	}

	@Override
	protected void dropExtraLoot() {
		explodeDew(pos);
		if (Random.Int(105 - Math.min(Statistics.albinoPiranhasKilled, 100)) == 0) {
			Item mushroom = Generator.random(Generator.Category.FOOD);
			trackedDrop(mushroom, 0);
		}

		if (!Dungeon.LimitedDrops.CONCH_SHELL.dropped()) {
			if (Statistics.albinoPiranhasKilled > 100) {
				Dungeon.LimitedDrops.CONCH_SHELL.drop();
				trackedDrop(new ConchShell(), 1);
			} else if (Statistics.albinoPiranhasKilled > 50 && Random.Int(10) == 0) {
				Dungeon.LimitedDrops.CONCH_SHELL.drop();
				trackedDrop(new ConchShell(), 1);
			}
		}
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	protected boolean getCloser( int target ) {
		if (rooted) {
			return false;
		}

		int step = Dungeon.findStep( this, target, BArray.and(Dungeon.level.water, Dungeon.level.passable, null), fieldOfView, true );
		if (step != -1) {
			move( step );
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected boolean getFurther( int target ) {
		int step = Dungeon.flee( this, target, BArray.and(Dungeon.level.water, Dungeon.level.passable, null), fieldOfView, true );
		if (step != -1) {
			move( step );
			return true;
		} else {
			return false;
		}
	}

	{
		for (Class c : new BlobImmunity().immunities()) {
			immunities.add(c);
		}
		immunities.add( Burning.class );
		immunities.add( Paralysis.class );
		immunities.add( Roots.class );
		immunities.add( Frost.class );
	}

	private class Sleeping extends Mob.Sleeping {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (enemyInFOV) {
				PathFinder.buildDistanceMap(enemy.pos, Dungeon.level.water, viewDistance);
				enemyInFOV = PathFinder.distance[pos] != Integer.MAX_VALUE;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	private class Wandering extends Mob.Wandering {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (enemyInFOV) {
				PathFinder.buildDistanceMap(enemy.pos, Dungeon.level.water, viewDistance);
				enemyInFOV = PathFinder.distance[pos] != Integer.MAX_VALUE;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (enemyInFOV) {
				PathFinder.buildDistanceMap(enemy.pos, Dungeon.level.water, viewDistance);
				enemyInFOV = PathFinder.distance[pos] != Integer.MAX_VALUE;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}
}

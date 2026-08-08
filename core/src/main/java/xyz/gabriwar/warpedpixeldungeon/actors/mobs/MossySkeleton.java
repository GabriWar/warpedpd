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

import xyz.gabriwar.warpedpixeldungeon.items.YellowDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.RedDewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.PrisonKey;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.items.Bone;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.MossySkeletonSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class MossySkeleton extends Mob {

	{
		spriteClass = MossySkeletonSprite.class;

		HP = HT = 35 + 10 * Random.NormalIntRange(7, 10);
		defenseSkill = 15;

		EXP = 1;
		maxLvl = 10;

		loot = new YellowDewdrop();
		lootChance = 0.5f;

		lootThird = new RedDewdrop();
		lootChanceThird = 0.1f;

		properties.add(Property.UNDEAD);
		properties.add(Property.INORGANIC);

		declareExtraLoot(PrisonKey.class, 1f);
		declareExtraLoot(Bone.class, 0.1f);
	}

	@Override
	public float attackDelay() {
		return super.attackDelay() * Math.max(0.5f, 1.5f - Statistics.skeletonsKilled * 0.01f);
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 20 + Math.round(Statistics.skeletonsKilled / 10), 45 + Math.round(Statistics.skeletonsKilled / 5) );
	}

	@Override
	public int attackSkill( Char target ) {
		return 28;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 27);
	}

	@Override
	public void die( Object cause ) {
		Statistics.skeletonsKilled++;
		GLog.i( Messages.get(this, "kill_count", Statistics.skeletonsKilled) );

		super.die( cause );

		if (cause == Chasm.class) return;

		boolean heroKilled = false;
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			Char ch = findChar( pos + PathFinder.NEIGHBOURS8[i] );
			if (ch != null && ch.isAlive()) {
				int damage = Math.max( 0, Random.NormalIntRange(3, 8) - (ch.drRoll() + ch.drRoll()) );
				ch.damage( damage, this );
				if (ch == Dungeon.hero && !ch.isAlive()) {
					heroKilled = true;
				}
			}
		}

		if (Dungeon.level.heroFOV[pos]) {
			Sample.INSTANCE.play( Assets.Sounds.BONES );
		}

		if (heroKilled) {
			Dungeon.fail( this );
			GLog.n( Messages.get(this, "explo_kill") );
		}
	}

	@Override
	protected void dropExtraLoot() {
		explodeDew(pos);
		if (!Dungeon.LimitedDrops.PRISON_KEY.dropped() && Dungeon.depth < Dungeon.POSTGAME_DEPTH) {
			Dungeon.LimitedDrops.PRISON_KEY.drop();
			trackedDrop(new PrisonKey(), 0);
		}

		if (!Dungeon.LimitedDrops.BONE.dropped() && Statistics.skeletonsKilled > 100) {
			Dungeon.LimitedDrops.BONE.drop();
			trackedDrop(new Bone(), 1);
		} else if (!Dungeon.LimitedDrops.BONE.dropped() && Statistics.skeletonsKilled > 50 && Random.Int(10) == 0) {
			Dungeon.LimitedDrops.BONE.drop();
			trackedDrop(new Bone(), 1);
		}
	}

}

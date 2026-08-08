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
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.SewersKey;
import xyz.gabriwar.warpedpixeldungeon.items.TenguKey;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.SafeSpotPage;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.ForestDart;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.GnollArcherSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

public class GnollArcher extends Mob {

	{
		spriteClass = GnollArcherSprite.class;

		HP = HT = 20;
		defenseSkill = 5;

		EXP = 1;

		baseSpeed = 1.5f - (Dungeon.depth / (float) Dungeon.POSTGAME_DEPTH);

		state = WANDERING;

		loot = Gold.class;
		lootChance = 0.01f;

		lootOther = Gold.class;
		lootChanceOther = 0.01f;

		declareExtraLoot(SewersKey.class, 1f);
		declareExtraLoot(TenguKey.class, 0.1f);
		declareExtraLoot(SafeSpotPage.class, 0.1f);
		declareExtraLoot(ForestDart.class, 1f);
	}

	@Override
	public int attackSkill( Char target ) {
		return 26;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return !Dungeon.level.adjacent( pos, enemy.pos )
				&& new Ballistica( pos, enemy.pos, Ballistica.STOP_SOLID ).collisionPos == enemy.pos;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(
				1 + Math.round( Statistics.archersKilled / 10f ),
				8 + Math.round( Statistics.archersKilled / 5f ) );
	}

	@Override
	protected boolean getCloser( int target ) {
		if (enemy != null && Dungeon.level.adjacent( pos, enemy.pos )) {
			return getFurther( target );
		} else {
			return super.getCloser( target );
		}
	}

	@Override
	public void die( Object cause ) {
		Statistics.archersKilled++;
		GLog.i( Messages.get(this, "kill_count", Statistics.archersKilled) );

		super.die( cause );
	}

	@Override
	protected void dropExtraLoot() {
		if (!Dungeon.LimitedDrops.SEWER_KEY.dropped() && Dungeon.depth < Dungeon.POSTGAME_DEPTH) {
			Dungeon.LimitedDrops.SEWER_KEY.drop();
			trackedDrop(new SewersKey(), 0);
		}

		if (!Dungeon.LimitedDrops.TENGU_KEY.dropped() && Dungeon.tengukilled) {
			if (Statistics.archersKilled > 100) {
				Dungeon.LimitedDrops.TENGU_KEY.drop();
				trackedDrop(new TenguKey(), 1);
			} else if (Statistics.archersKilled > 50 && Random.Int(10) == 0) {
				Dungeon.LimitedDrops.TENGU_KEY.drop();
				trackedDrop(new TenguKey(), 1);
			}
		}

		if (!Dungeon.LimitedDrops.SAFE_SPOT_PAGE.dropped()
				&& Statistics.archersKilled > 20 && Random.Int(10) == 0) {
			Dungeon.LimitedDrops.SAFE_SPOT_PAGE.drop();
			trackedDrop(new SafeSpotPage(), 2);
		}

		if (Dungeon.depth > 25) {
			trackedDrop(new ForestDart().quantity(3), 3);
		}

		if (!Dungeon.tengukilled && Statistics.archersKilled > 70 && Dungeon.depth >= Dungeon.POSTGAME_DEPTH) {
			GLog.w( Messages.get(this, "no_keys") );
		}
	}

	@Override
	public float spawningWeight() {
		return 0;
	}
}

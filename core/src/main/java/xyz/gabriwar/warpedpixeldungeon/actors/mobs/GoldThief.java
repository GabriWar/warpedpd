/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2014-2015 Evan Debenham
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.AncientCoin;
import xyz.gabriwar.warpedpixeldungeon.items.CityKey;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Shuriken;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GoldThiefSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class GoldThief extends Mob {

	{
		spriteClass = GoldThiefSprite.class;

		HP = HT = 30 + Statistics.goldThievesKilled;
		defenseSkill = 26;

		EXP = 1;

		loot = Shuriken.class;
		lootChance = 1f;

		FLEEING = new Fleeing();

		declareExtraLoot(CityKey.class, 1f);
		declareExtraLoot(AncientCoin.class, 0.1f);
	}

	private int stolenGold = 0;

	private static final String STOLEN_GOLD = "stolen_gold";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( STOLEN_GOLD, stolenGold );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		stolenGold = bundle.getInt( STOLEN_GOLD );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 30, 50 );
	}

	@Override
	public float attackDelay() {
		return 0.5f;
	}

	@Override
	public int attackSkill( Char target ) {
		return 32;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 13 + Math.round((Statistics.goldThievesKilled + 1) / 10 + 1) );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		if (stolenGold == 0 && enemy instanceof Hero) {
			int goldToSteal = Math.min( Random.IntRange(100, 500), Dungeon.gold );
			if (goldToSteal > 0) {
				stolenGold = goldToSteal;
				Dungeon.gold -= stolenGold;
				GLog.w( Messages.get(this, "stole", stolenGold) );
				state = FLEEING;
			}
		}
		return damage;
	}

	@Override
	public int defenseProc( Char enemy, int damage ) {
		if (state == FLEEING) {
			Dungeon.level.drop( new Gold(Random.IntRange(1, 10)), pos ).sprite.drop();
		}
		return super.defenseProc( enemy, damage );
	}

	@Override
	public void die( Object cause ) {
		Statistics.goldThievesKilled++;
		GLog.i( Messages.get(this, "kill_count", Statistics.goldThievesKilled) );

		super.die( cause );
	}

	@Override
	protected void dropExtraLoot() {
		explodeDew(pos);
		if (!Dungeon.LimitedDrops.CITY_KEY.dropped() && Dungeon.depth < Dungeon.POSTGAME_DEPTH) {
			Dungeon.LimitedDrops.CITY_KEY.drop();
			trackedDrop(new CityKey(), 0);
		}

		if (!Dungeon.LimitedDrops.ANCIENT_COIN.dropped() && Statistics.goldThievesKilled > 100) {
			Dungeon.LimitedDrops.ANCIENT_COIN.drop();
			trackedDrop(new AncientCoin(), 1);
		} else if (!Dungeon.LimitedDrops.ANCIENT_COIN.dropped() && Statistics.goldThievesKilled > 50 && Random.Int(10) == 0) {
			Dungeon.LimitedDrops.ANCIENT_COIN.drop();
			trackedDrop(new AncientCoin(), 1);
		}

		if (stolenGold > 0) {
			trackedDrop(new Gold(stolenGold), 2);
		}
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	private class Fleeing extends Mob.Fleeing {
		@Override
		protected void nowhereToRun() {
			if (buff( Terror.class ) == null) {
				sprite.showStatus( CharSprite.NEGATIVE, Messages.get(Mob.class, "rage") );
				state = HUNTING;
			} else {
				super.nowhereToRun();
			}
		}
	}
}

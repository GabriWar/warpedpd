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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.CountDown;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Spork;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.BanditKingSprite;
import com.watabou.utils.Random;

public class BanditKing extends Thief {

	{
		spriteClass = BanditKingSprite.class;

		HP = HT = 200;
		defenseSkill = 20;
		baseSpeed = 2f;

		EXP = 10;
		maxLvl = 25;

		flying = true;

		lootChance = 0.333f;

		declareExtraLoot(Spork.class, 1f);
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 20);
	}

	@Override
	protected boolean steal( Hero hero ) {
		if (super.steal(hero)) {
			if (Dungeon.depth < 25) {
				Buff.prolong(hero, Blindness.class, Random.IntRange(5, 12));
				Buff.affect(hero, Poison.class).set(Random.IntRange(5, 7));
				Buff.prolong(hero, Cripple.class, Cripple.DURATION);
				Dungeon.observe();
			} else if (hero.buff(CountDown.class) == null) {
				Buff.affect(hero, CountDown.class);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		if (enemy.buff(CountDown.class) == null) {
			Buff.affect(enemy, CountDown.class);
			state = FLEEING;
		}
		return damage;
	}

	@Override
	public void notice() {
		super.notice();
		if (enemy == null) return;
		yell(Messages.get(this, "notice"));
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);
		if (Dungeon.depth < 25) {
			yell(Messages.get(this, "die"));
		}
	}

	@Override
	protected void dropExtraLoot() {
		if (Dungeon.depth < 25 && !Dungeon.LimitedDrops.SPORK.dropped()) {
			Dungeon.LimitedDrops.SPORK.drop();
			trackedDrop(new Spork(), 0);
			Dungeon.sporkAvail = false;
		}
	}
}

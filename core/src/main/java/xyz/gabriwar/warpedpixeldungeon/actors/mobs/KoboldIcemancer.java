/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFrost;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.KoboldIcemancerSprite;
import com.watabou.utils.Random;

//ported from Remixed PD's Ice Caves: a kobold caster that hurls ice from range
//and slows whatever it hits.
public class KoboldIcemancer extends Mob {

	{
		spriteClass = KoboldIcemancerSprite.class;

		HP = HT = 70;
		defenseSkill = 18;

		EXP = 11;
		maxLvl = 21;

		loot = PotionOfFrost.class;
		lootChance = 0.2f;

		properties.add( Property.ICY );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 15, 17 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 25;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 11 );
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (Dungeon.level.adjacent( pos, enemy.pos )) {
			return super.canAttack( enemy );
		}
		return new Ballistica( pos, enemy.pos, Ballistica.MAGIC_BOLT ).collisionPos == enemy.pos;
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (Dungeon.level.adjacent( pos, enemy.pos )) {
			return super.doAttack( enemy );
		}

		if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
			((KoboldIcemancerSprite) sprite).zap( enemy.pos );
			return false;
		} else {
			onZapComplete();
			return true;
		}
	}

	public void onZapComplete() {
		Char enemy = this.enemy;
		if (enemy != null && hit( this, enemy, true )) {
			int dmg = Random.NormalIntRange( 8, 14 );
			enemy.damage( dmg, this );

			if (Random.Int( 2 ) == 0) {
				Buff.prolong( enemy, Slow.class, 1f );
			} else {
				Buff.affect( enemy, Chill.class, 3f );
			}

			if (!enemy.isAlive() && enemy == Dungeon.hero) {
				Dungeon.fail( this );
			}
		}
		next();
	}
}

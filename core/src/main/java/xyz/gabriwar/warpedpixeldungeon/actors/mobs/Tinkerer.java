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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfMagicalInfusion;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.sprites.TinkererSprite;
import com.watabou.utils.Random;

//ported from Unleashed PD: keeps its distance and lobs things at you
public class Tinkerer extends Mob {

	{
		spriteClass = TinkererSprite.class;

		HP = HT = 85;
		defenseSkill = 25;

		EXP = 11;
		maxLvl = 25;

		state = HUNTING;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 16, 20 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 25;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 8 );
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (Dungeon.level.adjacent( pos, enemy.pos )) {
			return false;
		}
		Ballistica attack = new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE );
		return attack.collisionPos == enemy.pos;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );

		if (!Dungeon.level.adjacent( pos, enemy.pos ) && Random.Int( 2 ) == 0) {
			Buff.prolong( enemy, Cripple.class, Cripple.DURATION );
			damage += Random.NormalIntRange( 4, 20 );
		}

		return damage;
	}

	@Override
	protected boolean getCloser( int target ) {
		//it would rather be anywhere else: while hunting it backs away instead of closing
		if (state == HUNTING) {
			return enemySeen && getFurther( target );
		} else {
			return super.getCloser( target );
		}
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		Dungeon.level.drop( new ScrollOfMagicalInfusion(), pos ).sprite.drop();
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.sprites.SteelBeeSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class SteelBee extends Mob {

	{
		spriteClass = SteelBeeSprite.class;

		alignment = Alignment.ALLY;
		state = HUNTING;
		viewDistance = 8;

		flying = true;

		immunities.add( Poison.class );
		immunities.add( Amok.class );
	}

	private int level;

	private static final String LEVEL = "level";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEVEL, level );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		spawn( bundle.getInt( LEVEL ) );
	}

	public void spawn( int level ) {
		this.level = level;
		HT = (10 + level) * 10;
		HP = HT;
		defenseSkill = 9 + level * 2;
	}

	@Override
	public int attackSkill( Char target ) {
		return defenseSkill;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( HT / 10, HT / 4 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		if (enemy instanceof Mob) {
			((Mob) enemy).aggro( this );
		}
		return damage;
	}

	@Override
	protected Char chooseEnemy() {
		if (enemy == null || !enemy.isAlive() || !fieldOfView[enemy.pos]) {
			for (Mob mob : Dungeon.level.mobs) {
				if (mob.alignment == Alignment.ENEMY && fieldOfView[mob.pos]) {
					return mob;
				}
			}
			return null;
		}
		return enemy;
	}

	@Override
	protected boolean act() {
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView( this, fieldOfView );
		return super.act();
	}

	private int potPos = -1;
	private Char potHolder = null;

	public void setPotInfo(int potPos, Char potHolder) {
		this.potPos = potPos;
		this.potHolder = potHolder;
	}

	@Override
	protected boolean getCloser( int target ) {
		if (enemy != null) {
			target = enemy.pos;
		} else if (potHolder != null) {
			target = potHolder.pos;
		} else if (potPos != -1) {
			target = potPos;
		} else {
			target = Dungeon.hero.pos;
		}
		return super.getCloser( target );
	}

	@Override
	public float spawningWeight() {
		return 0;
	}
}

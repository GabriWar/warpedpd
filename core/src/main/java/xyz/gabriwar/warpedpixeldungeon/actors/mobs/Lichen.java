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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Roots;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.MrDestructoSprite;
import com.watabou.utils.Random;

public class Lichen extends Mob {

	private static final float SPAWN_DELAY = 0.1f;

	{
		spriteClass = MrDestructoSprite.class;

		alignment = Alignment.ALLY;
		state = HUNTING;
		HP = HT = 100;
		defenseSkill = 3;
		viewDistance = 1;

		WANDERING = new Hunting();

		properties.add( Property.IMMOVABLE );
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1, 3 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 20 + Dungeon.depth;
	}

	@Override
	public float attackDelay() {
		return 0.5f;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 5 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		Buff.prolong( enemy, Roots.class, 10 );
		return damage;
	}

	@Override
	protected boolean act() {
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView( this, fieldOfView );
		return super.act();
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
	public void beckon( int cell ) {
		// ignore
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	public static Lichen spawnAt( int pos ) {
		if (Dungeon.level.passable[pos] && Actor.findChar( pos ) == null) {
			Lichen l = new Lichen();
			l.pos = pos;
			l.state = l.HUNTING;
			GameScene.add( l, SPAWN_DELAY );
			return l;
		}
		return null;
	}

	private class Hunting extends Mob.Wandering {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			if (enemyInFOV && canAttack( enemy )) {
				return doAttack( enemy );
			} else {
				enemy = chooseEnemy();
				if (enemy != null && canAttack( enemy )) {
					return doAttack( enemy );
				}
				spend( TICK );
				return true;
			}
		}
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.SeekingClusterBombSprite;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class SeekingClusterBomb extends Mob {

	{
		spriteClass = SeekingClusterBombSprite.class;

		alignment = Alignment.ALLY;
		state = HUNTING;
		HP = HT = 10;
		defenseSkill = 3;

		EXP = 0;

		immunities.add( Terror.class );
		immunities.add( ToxicGas.class );

		resistances.add( Grim.class );
		resistances.add( Vampiric.class );
	}

	private static final float SPAWN_DELAY = 0.1f;

	@Override
	public int attackSkill( Char target ) {
		return 99;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		int dmg = super.attackProc( enemy, damage );

		// Cluster explosion: scatter bombs across 5x5 area (distance-2 ring)
		// with 1/3 chance each cell, matching Sprouted's NEIGHBOURS8DIST2
		Bomb bomb = new Bomb();
		int w = Dungeon.level.width();
		int[] dist2 = {
			-2*w-2, -2*w-1, -2*w, -2*w+1, -2*w+2,
			-w-2, -w-1, -w, -w+1, -w+2,
			-2, -1, +1, +2,
			+w-2, +w-1, +w, +w+1, +w+2,
			+2*w-2, +2*w-1, +2*w, +2*w+1, +2*w+2
		};
		for (int n : dist2) {
			int c = pos + n;
			if (Random.Int(3) == 0 && Dungeon.level.insideMap(c)) {
				bomb.explode( c );
			}
		}

		yell( Messages.get(this, "explode") );

		destroy();
		sprite.die();

		return dmg;
	}

	@Override
	protected Char chooseEnemy() {
		if (enemy == null || !enemy.isAlive()) {
			java.util.HashSet<Mob> enemies = new java.util.HashSet<>();
			for (Mob mob : Dungeon.level.mobs) {
				if (mob.alignment == Alignment.ENEMY && fieldOfView[mob.pos]) {
					enemies.add(mob);
				}
			}
			return enemies.size() > 0 ? Random.element(enemies) : null;
		}
		return enemy;
	}

	@Override
	public void beckon( int cell ) {
		// seeking cluster bombs ignore beckoning
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	@Override
	public void die( Object cause ) {
		// Cluster explosion on death, matching Sprouted
		Bomb bomb = new Bomb();
		int w = Dungeon.level.width();
		int[] dist2 = {
			-2*w-2, -2*w-1, -2*w, -2*w+1, -2*w+2,
			-w-2, -w-1, -w, -w+1, -w+2,
			-2, -1, +1, +2,
			+w-2, +w-1, +w, +w+1, +w+2,
			+2*w-2, +2*w-1, +2*w, +2*w+1, +2*w+2
		};
		for (int n : dist2) {
			int c = pos + n;
			if (Random.Int(3) == 0 && Dungeon.level.insideMap(c)) {
				bomb.explode( c );
			}
		}

		yell( Messages.get(this, "explode") );

		super.die( cause );
	}

	public static SeekingClusterBomb spawnAt( int pos ) {
		SeekingClusterBomb b = new SeekingClusterBomb();
		b.pos = pos;
		b.state = b.HUNTING;
		GameScene.add( b, SPAWN_DELAY );
		return b;
	}
}

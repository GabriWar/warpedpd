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
import xyz.gabriwar.warpedpixeldungeon.sprites.SeekingBombSprite;
import com.watabou.utils.Random;

public class SeekingBomb extends Mob {

	{
		spriteClass = SeekingBombSprite.class;

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

		Bomb bomb = new Bomb();
		bomb.explode( pos );
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
		// seeking bombs ignore beckoning
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	@Override
	public void die( Object cause ) {
		Bomb bomb = new Bomb();
		bomb.explode( pos );

		super.die( cause );
	}

	public static SeekingBomb spawnAt( int pos ) {
		SeekingBomb b = new SeekingBomb();
		b.pos = pos;
		b.state = b.HUNTING;
		GameScene.add( b, SPAWN_DELAY );
		return b;
	}
}

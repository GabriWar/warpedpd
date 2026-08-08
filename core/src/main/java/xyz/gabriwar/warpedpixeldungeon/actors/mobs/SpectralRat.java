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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Weakness;
import xyz.gabriwar.warpedpixeldungeon.mechanics.Ballistica;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.SpectralRatSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

public class SpectralRat extends Mob {

	private static final float TIME_TO_ZAP = 1f;
	private static final float SPAWN_DELAY = 2f;

	{
		spriteClass = SpectralRatSprite.class;
		baseSpeed = 4f;

		HP = HT = 80 + (Dungeon.depth * 3);
		defenseSkill = 2;

		EXP = 0;

		properties.add(Property.UNDEAD);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( Dungeon.depth / 2, Dungeon.depth );
	}

	@Override
	public int attackSkill( Char target ) {
		return 50;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 10 );
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		Ballistica bolt = new Ballistica( pos, enemy.pos, Ballistica.STOP_SOLID );
		return bolt.collisionPos == enemy.pos;
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (Dungeon.level.adjacent( pos, enemy.pos )) {
			return super.doAttack( enemy );
		} else {
			spend( TIME_TO_ZAP );

			boolean visible = Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[enemy.pos];
			if (visible) {
				sprite.zap( enemy.pos );
				return false;
			} else {
				zap();
				return true;
			}
		}
	}

	private void zap() {
		if (hit( this, enemy, true )) {
			int dmg = Random.NormalIntRange( 20, 45 );
			enemy.damage( dmg, this );

			if (enemy == Dungeon.hero && Random.Int(5) == 0) {
				Buff.prolong( enemy, Weakness.class, Weakness.DURATION );
			}

			if (!enemy.isAlive() && enemy == Dungeon.hero) {
				Dungeon.fail( this );
				GLog.n( Messages.get(this, "bolt_kill") );
			}
		} else {
			enemy.sprite.showStatus( CharSprite.NEUTRAL, enemy.defenseVerb() );
		}
	}

	public void onZapComplete() {
		zap();
		next();
	}

	@Override
	public float spawningWeight() {
		return 0;
	}

	public static SpectralRat spawnAt( int pos ) {
		SpectralRat r = new SpectralRat();
		r.pos = pos;
		r.state = r.HUNTING;
		GameScene.add( r, SPAWN_DELAY );
		return r;
	}
}

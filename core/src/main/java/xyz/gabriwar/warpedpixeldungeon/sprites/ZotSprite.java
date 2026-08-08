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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.Skull;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;

public class ZotSprite extends MobSprite {

	private Animation cast;

	public ZotSprite() {
		super();

		texture( Assets.Sprites.ZOT );

		TextureFilm frames = new TextureFilm( texture, 18, 18 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1, 0 );

		run = new Animation( 8, false );
		run.frames( frames, 0, 1, 2 );

		attack = new Animation( 8, false );
		attack.frames( frames, 0, 2, 2 );

		cast = new Animation( 8, false );
		cast.frames( frames, 2, 3, 4 );

		die = new Animation( 8, false );
		die.frames( frames, 0, 5, 6, 7, 8, 9, 8 );

		play( run.clone() );
	}

	@Override
	public void move( int from, int to ) {
		place( to );
		play( run );
		turnTo( from, to );
		isMoving = true;

		if (Dungeon.level.water[to]) {
			GameScene.ripple( to );
		}

		ch.onMotionComplete();
	}

	@Override
	public void attack( int cell ) {
		if (!Dungeon.level.adjacent( cell, ch.pos )) {
			((MissileSprite) parent.recycle( MissileSprite.class )).reset( ch.pos,
					cell, new Skull(), new Callback() {
						@Override
						public void call() {
							ch.onAttackComplete();
						}
					} );

			play( cast );
			turnTo( ch.pos, cell );
		} else {
			super.attack( cell );
		}
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == run) {
			isMoving = false;
			idle();
		} else {
			super.onComplete( anim );
		}
	}
}

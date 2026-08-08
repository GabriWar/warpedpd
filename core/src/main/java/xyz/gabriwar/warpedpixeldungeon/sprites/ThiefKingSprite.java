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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;

public class ThiefKingSprite extends MobSprite {

	private Animation cast;

	public ThiefKingSprite() {
		super();

		texture( Assets.Sprites.THIEF_KING );

		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 0 );

		run = new Animation( 15, true );
		run.frames( frames, 1, 2, 3, 4, 5 );

		attack = new Animation( 15, false );
		attack.frames( frames, 6, 7, 8 );

		cast = new Animation( 15, false );
		cast.frames( frames, 9, 10 );

		die = new Animation( 8, false );
		die.frames( frames, 11, 12, 13, 14 );

		play( idle );
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );
		add( State.LEVITATING );
	}

	@Override
	public void die() {
		super.die();
		remove( State.LEVITATING );
	}

	@Override
	public void attack( int cell ) {
		if (!Dungeon.level.adjacent(cell, ch.pos)) {
			play( cast );
			turnTo( ch.pos, cell );
			// Fire a missile dart
			((MissileSprite)parent.recycle( MissileSprite.class )).
				reset( ch.pos, cell, new Dart(), new Callback() {
					@Override
					public void call() {
						ch.onAttackComplete();
					}
				} );
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

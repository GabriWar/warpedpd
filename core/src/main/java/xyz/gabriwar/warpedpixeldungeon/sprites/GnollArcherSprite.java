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
import xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;

public class GnollArcherSprite extends MobSprite {

	private Animation cast;

	public GnollArcherSprite() {
		super();

		texture( Assets.Sprites.GNOLL_ARCHER );

		TextureFilm frames = new TextureFilm( texture, 12, 15 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1, 0, 0, 1, 1 );

		run = new Animation( 12, true );
		run.frames( frames, 4, 5, 6, 7 );

		attack = new Animation( 12, false );
		attack.frames( frames, 2, 3, 0 );

		cast = attack.clone();

		die = new Animation( 12, false );
		die.frames( frames, 8, 9, 10 );

		play( idle );
	}

	@Override
	public void attack( int cell ) {
		if (!Dungeon.level.adjacent( cell, ch.pos )) {

			((MissileSprite) parent.recycle( MissileSprite.class )).
					reset( this, cell, new Dart(), new Callback() {
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
}

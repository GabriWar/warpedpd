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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.TextureFilm;

/**
 * The Breaker's Bench (tools/warped_rooms_art.py, 16x16): a heavy bench with a vice and
 * a maul resting on it; two more frames bring the maul down for the moment a piece is
 * broken.
 */
public class BreakersBenchSprite extends MobSprite {

	private final Animation strike;

	public BreakersBenchSprite(){
		super();

		texture( Assets.Sprites.BREAKERS_BENCH );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 1, true );
		idle.frames( frames, 0 );

		strike = new Animation( 10, false );
		strike.frames( frames, 1, 2, 2, 1, 0 );

		run = idle.clone();
		attack = idle.clone();
		die = idle.clone();

		play( idle );
	}

	public void strike(){
		play( strike );
	}

	@Override
	public void onComplete( Animation anim ){
		if (anim == strike){
			idle();
		} else {
			super.onComplete( anim );
		}
	}

	@Override
	public int blood(){
		return 0xFF7A4A24;
	}
}

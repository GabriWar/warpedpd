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

/** The mountain goat of the high slices (tools/goat_gen.py). Frames: stand, graze, two of the
 *  run, the butt's wind-up and the lunge, the fall, the carcass. */
public class GoatSprite extends MobSprite {

	//the head dropped and the horns levelled, held: a cornered goat's one warning before it butts
	private Animation brace;

	public GoatSprite(){
		super();
		texture( Assets.Sprites.GOAT );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		//head down to graze now and then
		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 0, 1, 1, 1, 0 );

		run = new Animation( 12, true );
		run.frames( frames, 2, 3 );

		//its own object: the head dropped and the lunge, never the looped idle
		attack = new Animation( 12, false );
		attack.frames( frames, 4, 5, 5, 4 );

		die = new Animation( 10, false );
		die.frames( frames, 6, 7 );

		brace = new Animation( 1, false );
		brace.frames( frames, 4 );

		play( idle );
	}

	/** A cornered goat braces to butt: the wind-up frame, held until it next moves or strikes,
	 *  turned to the one it means to butt (it fled with its back to him) */
	public synchronized void brace( int cell ){
		turnTo( ch.pos, cell );
		play( brace );
	}
}

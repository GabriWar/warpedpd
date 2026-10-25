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

/** A bison of the plains and meadows (tools/ow_fauna_gen.py, 20x16 frames): stand, graze, two of
 *  the gallop, the gore's wind-up with the head levelled and the toss, the fall onto its knees and
 *  the carcass. Big enough to spill a little past its cell. */
public class BisonSprite extends WildSprite {

	public BisonSprite(){
		super();
		texture( Assets.Sprites.BISON );
		TextureFilm frames = new TextureFilm( texture, 20, 16 );

		//the head down to graze, then up to look about
		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 0, 1, 1, 1, 0 );

		run = new Animation( 12, true );
		run.frames( frames, 2, 0, 3, 0 );

		//its own object: the head levelled, the toss, never the looped idle
		attack = new Animation( 12, false );
		attack.frames( frames, 4, 5, 5, 0 );

		die = new Animation( 10, false );
		die.frames( frames, 6, 7 );

		play( idle );
	}
}

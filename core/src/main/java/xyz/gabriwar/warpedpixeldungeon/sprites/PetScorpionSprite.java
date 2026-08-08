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
import com.watabou.noosa.TextureFilm;

public class PetScorpionSprite extends MobSprite {

	public PetScorpionSprite() {
		super();

		texture( Assets.Sprites.SCORPIO );

		TextureFilm frames = new TextureFilm( texture, 18, 17 );

		idle = new Animation( 12, true );
		idle.frames( frames, 28, 28, 28, 28, 28, 28, 28, 28, 29, 30, 29, 30, 29, 30 );

		run = new Animation( 8, true );
		run.frames( frames, 33, 33, 34, 34 );

		attack = new Animation( 15, false );
		attack.frames( frames, 28, 31, 32 );

		die = new Animation( 12, false );
		die.frames( frames, 28, 35, 36, 37, 38 );

		play( idle );
	}

	@Override
	public int blood() {
		return 0xFF44FF22;
	}
}

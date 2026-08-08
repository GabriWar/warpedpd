/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2025-2026 GabriWar
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

public class MagicOrbSprite extends MobSprite {

	public MagicOrbSprite() {
		super();

		texture( Assets.Sprites.MAGIC_ORB );

		TextureFilm frames = new TextureFilm( texture, 12, 16 );

		idle = new Animation( 14, true );
		idle.frames( frames, 0, 1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1 );

		run = new Animation( 28, true );
		run.frames( frames, 0, 1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1 );

		attack = new Animation( 24, false );
		attack.frames( frames, 0, 1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1 );

		die = new Animation( 24, false );
		die.frames( frames, 0, 0, 0, 0, 0, 0, 0 );

		play( idle );
	}

	@Override
	public int blood() {
		return 0x66CCFF;
	}
}

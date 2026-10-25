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

/** A pearl oyster in a tide pool (actors/mobs/PearlOyster): a rough grey-brown shell, barnacled,
 *  that now and then gapes a little and shows the pearl's gleam inside (tools/assets/wetland_art.py);
 *  dying it falls open, empty. */
public class PearlOysterSprite extends MobSprite {

	public PearlOysterSprite(){
		super();

		texture( Assets.Sprites.PEARL_OYSTER );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 3, true );
		idle.frames( frames, 0, 0, 0, 0, 0, 0, 1, 2, 2, 1 );

		run = new Animation( 1, false );
		run.frames( frames, 0 );
		attack = new Animation( 1, false );
		attack.frames( frames, 0 );
		operate = new Animation( 1, false );
		operate.frames( frames, 0 );

		die = new Animation( 6, false );
		die.frames( frames, 1, 3 );

		play( idle );
	}

	@Override
	public void turnTo( int from, int to ){
		//a shell faces nowhere
	}

	@Override
	public int blood(){
		return 0xFFD8D0C0;
	}
}

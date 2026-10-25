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

/** The swamp witch (actors/mobs/npcs/SwampWitch): a hunched old woman in a moss-green shawl and a
 *  pointed hat with a crooked tip, grey hair, green-grey skin, a hooked nose, a ladle in her hand
 *  and a bundle of herbs at her belt (tools/assets/wetland_art.py). Idle she breathes and now and
 *  then cackles; she stirs (operate) as she brews. */
public class SwampWitchSprite extends MobSprite {

	public SwampWitchSprite(){
		super();

		texture( Assets.Sprites.SWAMP_WITCH );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 3, true );
		idle.frames( frames, 0, 0, 1, 0, 0, 0, 2, 0 );

		run = new Animation( 8, true );
		run.frames( frames, 3, 4 );

		attack = new Animation( 12, false );
		attack.frames( frames, 5, 6, 0 );

		operate = new Animation( 8, false );
		operate.frames( frames, 7, 8, 9, 8, 7 );

		die = new Animation( 8, false );
		die.frames( frames, 10, 11, 12 );

		play( idle );
	}

	@Override
	public int blood(){
		return 0xFF6A8A50;
	}
}

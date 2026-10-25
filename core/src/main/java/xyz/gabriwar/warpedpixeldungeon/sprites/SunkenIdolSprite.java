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

/** A sunken shrine's idol (actors/mobs/npcs/SunkenIdol): a squat old god of mossy green stone, its
 *  arms folded on its belly, standing to its waist in the water of its flooded floor, the coins
 *  left to it glinting on the water at its feet (tools/assets/wetland_art.py). Now and then its
 *  hollow eyes catch the light, now and then a coin does. */
public class SunkenIdolSprite extends MobSprite {

	public SunkenIdolSprite(){
		super();

		texture( Assets.Sprites.SUNKEN_IDOL );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 0, 1, 0, 0, 0, 2, 0 );

		run = new Animation( 1, false );
		run.frames( frames, 0 );
		attack = new Animation( 1, false );
		attack.frames( frames, 0 );
		operate = new Animation( 1, false );
		operate.frames( frames, 0 );
		die = new Animation( 1, false );
		die.frames( frames, 0 );

		play( idle );
	}

	@Override
	public void turnTo( int from, int to ){
		//the idol faces the way its shrine was built
	}

	@Override
	public int blood(){
		return 0xFF6E7A70;
	}
}

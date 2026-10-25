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

/** A drowned acolyte of a sunken shrine (actors/mobs/DrownedAcolyte): a sodden robed figure, grey
 *  and bloated, weed hanging off its hood and arms, water running off it (tools/assets/wetland_art.py).
 *  It rises out of the water (rise) where it is laid by night. */
public class DrownedAcolyteSprite extends MobSprite {

	private Animation rise;

	public DrownedAcolyteSprite(){
		super();

		texture( Assets.Sprites.DROWNED_ACOLYTE );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 1, 1 );

		run = new Animation( 8, true );
		run.frames( frames, 2, 3, 4, 5 );

		attack = new Animation( 12, false );
		attack.frames( frames, 6, 7, 8 );

		die = new Animation( 8, false );
		die.frames( frames, 9, 10, 11, 12 );

		rise = new Animation( 8, false );
		rise.frames( frames, 13, 14, 15, 16, 0 );

		play( idle );
	}

	/** Comes up out of the water it stands in. */
	public void rise(){
		play( rise );
	}

	@Override
	public void onComplete( Animation anim ){
		if (anim == rise){
			idle();
			return;
		}
		super.onComplete( anim );
	}

	@Override
	public int blood(){
		return 0xFF5C7A6A;
	}
}

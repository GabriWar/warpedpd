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

/** A road caravan on the move (actors/mobs/npcs/CaravanTrain): the caravaneer on the bench of
 *  his covered cart behind his mule, one 28x16 sheet (tools/caravan_train_gen.py) - two
 *  standing frames, a four-frame walk with the wheel turning, two of the driver's raised
 *  hand. Wider than a cell, and centred on it like every sprite. */
public class CaravanTrainSprite extends MobSprite {

	//the driver's hand raised from the bench to whoever hails him
	private Animation wave;

	public CaravanTrainSprite(){
		super();

		texture( Assets.Sprites.CARAVAN_TRAIN );

		TextureFilm frames = new TextureFilm( texture, 28, 16 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1, 0, 0, 0, 1, 1 );

		run = new Animation( 8, true );
		run.frames( frames, 2, 3, 4, 5 );

		attack = new Animation( 6, false );
		attack.frames( frames, 6, 7, 6, 0 );

		die = new Animation( 4, false );
		die.frames( frames, 0, 1 );

		wave = new Animation( 6, false );
		wave.frames( frames, 6, 7, 6, 7, 0 );

		play( idle );
	}

	public void wave(){
		if (!isMoving) play( wave );
	}

	@Override
	public synchronized void onComplete( Animation anim ){
		if (anim == wave){
			idle();
			return;
		}
		super.onComplete( anim );
	}
}

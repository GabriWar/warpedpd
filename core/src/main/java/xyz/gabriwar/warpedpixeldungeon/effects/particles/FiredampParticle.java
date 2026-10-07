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


package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;

/**
 * Firedamp's haze (levels/overworld/HazardWatch): sulphur-yellow wisps that hang in the foul air
 * of a deep cave, drifting and drawing out as they go, laid on every gas cell about the hero
 * each turn he is in it, so he can see where it lies. Yellower than toxic gas's green, warmer
 * than the grey of mist, and soft and moving where the caves' moss is hard spots on the floor.
 */
public class FiredampParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			((FiredampParticle) emitter.recycle( FiredampParticle.class )).reset( x, y );
		}
	};

	public FiredampParticle(){
		super();
		color( 0xD4D06C );
		lifespan = 3f;
	}

	public void reset( float x, float y ){
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float( 2.5f, 4f );
		frame( Random.Int( 3 ) == 0 ? WeatherSprites.WISP_XL : WeatherSprites.WISP_L );
		color( Random.Int( 2 ) == 0 ? 0xD4D06C : 0xBCC45C );
		speed.set( Random.Float( -4f, 4f ), Random.Float( -3f, -1f ) );
		acc.set( 0, 0 );
	}

	@Override
	public void update(){
		super.update();
		am = envelope( 0.3f, 0.4f, 0.7f );
		//drawn out sideways as it drifts
		scale.x = 1f + 0.5f * (1f - left / lifespan);
		fov();
	}
}

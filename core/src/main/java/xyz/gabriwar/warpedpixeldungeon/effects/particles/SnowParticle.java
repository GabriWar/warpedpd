/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

/** The flakes of a frost: they fall, or rise off something freezing. */
public class SnowParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((SnowParticle)emitter.recycle( SnowParticle.class )).reset( x, y, false );
		}
	};
	public static final Emitter.Factory RISING_FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((SnowParticle)emitter.recycle( SnowParticle.class )).reset( x, y, true );
		}
	};

	private boolean rising;

	public SnowParticle() {
		super();
		lifespan = 1.2f;
		color( 0xFFFFFF );
	}

	public void reset( float x, float y, boolean rising ) {
		revive();
		this.rising = rising;
		float spd = Random.Float( 5, 8 );
		speed.set( 0, rising ? -spd : spd );
		this.x = x;
		this.y = rising ? y + spd * lifespan : y - spd * lifespan;
		frame( Random.Float() < 0.6f ? WeatherSprites.FLAKE_1 : WeatherSprites.FLAKE_2 );
		left = lifespan;
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;
		am = (p < 0.5f ? p : 1 - p) * 1.5f;
	}
}

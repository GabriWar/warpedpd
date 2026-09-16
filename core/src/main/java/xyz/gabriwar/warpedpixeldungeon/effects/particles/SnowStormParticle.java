/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2022-2025 Overgrown Team
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

import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;

/** The snow of a snowstorm blob: real flakes, wobbling down through the cloud. */
public class SnowStormParticle extends WeatherParticle {

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((SnowStormParticle)emitter.recycle( SnowStormParticle.class )).reset( x, y );
		}
	};

	/** the whirlwind's snow: flakes circling where they rose, spiralling outward */
	public static final Emitter.Factory WHIRL = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((SnowStormParticle)emitter.recycle( SnowStormParticle.class )).resetWhirl( x, y );
		}
	};

	private float wobblePhase;
	private boolean whirl;
	private float cx, cy, radius, theta, spin;

	public SnowStormParticle() {
		super();
		speed.set( 0, Random.Float( 5, 8 ) );
		lifespan = 2f;
		color( 0xFFFFFF );
	}

	public void resetWhirl( float x, float y ) {
		reset( x, y );
		whirl = true;
		cx = x; cy = y;
		this.x = x; this.y = y;
		radius = Random.Float( 1f, 3f );
		theta = Random.Float( (float)(Math.PI * 2) );
		spin = Random.Float( 4f, 7f ) * (Random.Int( 2 ) == 0 ? -1 : 1);
		speed.set( 0, 0 );
		left = lifespan = Random.Float( 1.2f, 2f );
	}

	public void reset( float x, float y ) {
		revive();
		whirl = false;
		speed.set( 0, Random.Float( 5, 8 ) );
		this.x = x;
		this.y = y - speed.y * lifespan;
		float r = Random.Float();
		frame( r < 0.45f ? WeatherSprites.FLAKE_1 : (r < 0.8f ? WeatherSprites.FLAKE_2 : WeatherSprites.FLAKE_3) );
		wobblePhase = Random.Float( (float)(Math.PI * 2) );
		left = lifespan;
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;
		if (whirl) {
			//round and round, wider each turn, lifting a little
			theta += spin * Game.elapsed;
			radius += 5f * Game.elapsed;
			cy -= 6f * Game.elapsed;
			x = cx + (float)Math.cos( theta ) * radius;
			y = cy + (float)Math.sin( theta ) * radius * 0.6f;
		} else {
			wobblePhase += Game.elapsed * 2.5f;
			speed.x = (float)Math.sin( wobblePhase ) * 3f;
		}
		am = (p < 0.5f ? p : 1 - p) * 1.5f;
	}
}

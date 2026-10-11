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

import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;

/**
 * The snow of a snowstorm blob: real flakes, wobbling down through the cloud, on whole pixels;
 * a falling one that ends over water leaves the faintest ring (S, 0.3, dressing). The whirlwind's
 * circle where they rose, spiralling out and lifting. Its randomness is the effects' own.
 */
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
	private float px, py, vy;

	public SnowStormParticle() {
		super();
		lifespan = 2f;
		color( 0xFFFFFF );
	}

	public void resetWhirl( float x, float y ) {
		reset( x, y );
		whirl = true;
		cx = x; cy = y;
		px = x; py = y;
		radius = FxRandom.Float( 1f, 3f );
		theta = FxRandom.angle();
		spin = FxRandom.Float( 4f, 7f ) * FxRandom.sign();
		left = lifespan = FxRandom.Float( 1.2f, 2f );
		place();
	}

	public void reset( float x, float y ) {
		revive();
		whirl = false;
		lifespan = 2f;
		speed.set( 0, 0 );
		vy = FxRandom.Float( 5, 8 );
		px = x;
		py = y - vy * lifespan;
		float r = FxRandom.Float();
		frame( r < 0.45f ? WeatherSprites.FLAKE_1 : (r < 0.8f ? WeatherSprites.FLAKE_2 : WeatherSprites.FLAKE_3) );
		wobblePhase = FxRandom.angle();
		left = lifespan;
		place();
	}

	private void place() {
		x = Math.round( px - origin.x );
		y = Math.round( py - origin.y );
	}

	@Override
	public void update() {
		super.update();
		if (!alive) {
			if (!whirl && WaterFX.onWater( px, py )) {
				WaterFX.ring( px, py, WaterFX.S, WaterFX.liquid().foam, SnowParticle.RING_ALPHA, FxBudget.P3 );
			}
			return;
		}
		float p = left / lifespan;
		float dt = Game.elapsed;
		if (whirl) {
			//round and round, wider each turn, lifting a little
			theta += spin * dt;
			radius += 5f * dt;
			cy -= 6f * dt;
			px = cx + (float)Math.cos( theta ) * radius;
			py = cy + (float)Math.sin( theta ) * radius * 0.6f;
		} else {
			wobblePhase += dt * 2.5f;
			px += (float)Math.sin( wobblePhase ) * 3f * dt;
			py += vy * dt;
		}
		place();
		am = (p < 0.5f ? p : 1 - p) * 1.5f;
	}
}

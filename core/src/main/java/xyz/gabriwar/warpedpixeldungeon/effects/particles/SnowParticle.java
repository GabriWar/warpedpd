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

import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;

/**
 * The flakes of a frost: they fall, swaying a pixel or two, or rise off something freezing; one
 * in four catches the light as it turns (its frame winking between one and two pixels). Slow as
 * they are, they sit on whole pixels. A falling flake that ends over water leaves the faintest
 * ring there (S, 0.3, dressing). Its randomness is the effects' own (FxRandom).
 */
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

	/** A falling flake's ring where it ends on water: its alpha. */
	public static final float RING_ALPHA = 0.3f;

	private boolean rising, winks;
	private float px, py, vx, vy, phase, sway, wink;

	public SnowParticle() {
		super();
		lifespan = 1.2f;
		color( 0xFFFFFF );
	}

	public void reset( float x, float y, boolean rising ) {
		revive();
		this.rising = rising;
		float spd = FxRandom.Float( 5, 8 );
		vx = 0;
		vy = rising ? -spd : spd;
		speed.set( 0, 0 );
		px = x;
		py = rising ? y + spd * lifespan : y - spd * lifespan;
		frame( FxRandom.Float() < 0.6f ? WeatherSprites.FLAKE_1 : WeatherSprites.FLAKE_2 );
		phase = FxRandom.angle();
		sway = FxRandom.Float( 1f, 2.5f );
		winks = FxRandom.chance( 0.25f );
		wink = FxRandom.Float( 0.3f );
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
			if (!rising && WaterFX.onWater( px, py )) {
				WaterFX.ring( px, py, WaterFX.S, WaterFX.liquid().foam, RING_ALPHA, FxBudget.P3 );
			}
			return;
		}
		float dt = Game.elapsed;
		phase += dt * 2.5f;
		px += (vx + (float)Math.sin( phase ) * sway) * dt;
		py += vy * dt;
		if (winks && (wink -= dt) <= 0) {
			wink = 0.3f;
			frame( frame == WeatherSprites.FLAKE_1 ? WeatherSprites.FLAKE_2 : WeatherSprites.FLAKE_1 );
		}
		place();
		float p = left / lifespan;
		am = (p < 0.5f ? p : 1 - p) * 1.5f;
	}
}

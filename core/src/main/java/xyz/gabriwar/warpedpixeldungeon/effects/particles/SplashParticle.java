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

import com.watabou.noosa.Group;

import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;

/** Where a drop lands: a crown of water that opens and is gone in a blink. On the water a small
 *  ring spreads in the water's own surface instead (WaterFX: under the night's tint and the
 *  shore's lip), never one of the weather's drawn over everything. */
public class SplashParticle extends WeatherParticle {

	private static final float LIFE = 0.28f;

	private int shown = -1;
	private float peak;

	/** a splash at a landing point, in the drop's colour, if the group has room; on the water a
	 *  small ring in the water's foam */
	public static void splash( Group in, float x, float y, int color, float alpha ) {
		if (WaterFX.onWater( x, y )) {
			WaterFX.ring( x, y, WaterFX.S, WaterFX.liquid().foam, Math.min( alpha, WaterFX.alphaOf( WaterFX.S ) ), FxBudget.P2 );
			return;
		}
		if (in == null) return;
		SplashParticle s = (SplashParticle) in.recycle( SplashParticle.class );
		s.revive();
		s.x = x;
		s.y = y;
		s.color( color );
		s.peak = alpha;
		s.speed.set( 0, 0 );
		s.acc.set( 0, 0 );
		s.left = s.lifespan = LIFE;
		s.shown = -1;
		s.show( 0 );
	}

	private void show( int f ) {
		if (f != shown) {
			shown = f;
			frame( WeatherSprites.SPLASH[f] );
			//the crown stands on the landing point
			origin.set( frame[2] / 2f, frame[3] );
		}
	}

	@Override
	public void update() {
		super.update();
		float p = 1f - left / lifespan;
		show( p < 0.35f ? 0 : (p < 0.7f ? 1 : 2) );
		am = peak * (1f - p * 0.6f);
		fov();
	}
}

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

import com.watabou.noosa.particles.PixelParticle;

import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;

/**
 * A particle that is a drawing rather than a scaled square: one frame of the
 * weather sprite sheet, centred on its position, at native pixel size. The
 * subclasses keep everything a PixelParticle has (lifespan, speed, colour) and
 * just pick their frame.
 */
public class WeatherParticle extends PixelParticle {

	protected int[] frame;

	public WeatherParticle() {
		super();
		texture( WeatherSprites.get() );
	}

	protected void frame( int[] f ) {
		frame = f;
		frame( f[0], f[1], f[2], f[3] );
		origin.set( f[2] / 2f, f[3] / 2f );
		scale.set( 1 );
		angle = 0;
		//nothing shows until the first update has set the alpha for this life
		am = 0;
	}

	/** hidden where the hero cannot see; the fog above masks the rest */
	protected void fov() {
		if (!WeatherSprites.visible( x, y )) am = 0;
	}

	/** the same test, but against the cell the particle belongs to rather than
	 *  where it is drawn: a cloud floats above its own ground and must not be
	 *  hidden by the unseen wall it happens to hang over */
	protected void fovAt( float sx, float sy ) {
		if (!WeatherSprites.visible( sx, sy )) am = 0;
	}

	/** an alpha that rises over the first `in` of the life, holds at peak, and
	 *  falls over the last `out` */
	protected float envelope( float in, float out, float peak ) {
		float p = 1f - left / lifespan;
		if (p < in) return peak * (p / in);
		if (p > 1f - out) return peak * ((1f - p) / out);
		return peak;
	}
}

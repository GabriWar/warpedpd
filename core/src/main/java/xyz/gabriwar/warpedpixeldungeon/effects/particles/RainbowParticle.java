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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

//a mote of the prism: one of its seven saturated hues (never a random RGB), turning to the next
//once a HUE_TURN, washing toward white over its last 30%, drifting up, 1, 2, then 1 px
public class RainbowParticle extends PixelParticle {

	public static final Emitter.Factory BURST = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((RainbowParticle)emitter.recycle( RainbowParticle.class )).resetBurst( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	/** Seconds a hue holds before it turns to the next (its half-second life sees one turn). */
	public static final float HUE_TURN = 0.25f;
	/** Where it starts washing toward white, a share of its life. */
	public static final float WHITE_FROM = 0.7f;
	/** How fast it drifts up, px/s. */
	public static final float DRIFT = 6f;

	private int hue;

	public RainbowParticle() {
		super();
		lifespan = 0.5f;
	}

	/** The prism's hue i (0..6), wrapping. */
	public static int hue( int i ){
		Ramp r = Element.PRISM.ramp;
		return r.stop( ((i % r.size()) + r.size()) % r.size() );
	}

	public void reset( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		speed.set( FxRandom.Float(-5, +5), FxRandom.Float( -5, +5 ) - DRIFT );

		left = lifespan;
		pick();
	}

	public void resetBurst( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		float a = FxRandom.angle(), s = FxRandom.Float( 16, 32 );
		speed.set( (float)Math.cos( a ) * s, (float)Math.sin( a ) * s - DRIFT );

		left = lifespan;
		pick();
	}

	private void pick(){
		hue = FxRandom.Int( Element.PRISM.ramp.size() );
		am = 1;
		size( 1 );
		color( hue( hue ) );
	}

	@Override
	public void update() {
		super.update();
		float age = lifespan - left, p = age / lifespan;
		int c = hue( hue + (int)(age / HUE_TURN) );
		if (p > WHITE_FROM) c = Ramp.lerp( c, 0xFFFFFF, (p - WHITE_FROM) / (1f - WHITE_FROM) );
		color( c );
		am = 1f - p * p;
		size( p < 0.25f || p > 0.75f ? 1 : 2 );
	}
}

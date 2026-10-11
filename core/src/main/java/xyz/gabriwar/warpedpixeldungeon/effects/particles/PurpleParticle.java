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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;
import com.watabou.noosa.particles.PixelParticle;

//the disintegration's motes: white-hot for their first fifth, then cooling from a hot rose to a
//deep violet, growing in whole pixels 1, 3, 5 as they fade (visual randomness from FxRandom)
public class PurpleParticle extends PixelParticle {

	public static final Emitter.Factory MISSILE = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((PurpleParticle)emitter.recycle( PurpleParticle.class )).reset( x, y );
		}
	};

	public static final Emitter.Factory BURST = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((PurpleParticle)emitter.recycle( PurpleParticle.class )).resetBurst( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	/** White-hot, the rose it cools to, the violet it dies in. */
	public static final int HOT = 0xFFFFFF, ROSE = 0xFF0044, VIOLET = 0x220066;
	/** The share of its life it stays white-hot, and its sizes' steps. */
	public static final float HOT_FOR = 0.2f;
	public static final int[] SIZES = { 1, 3, 5 };

	public PurpleParticle() {
		super();

		lifespan = 0.5f;
	}

	public void reset( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		speed.set( FxRandom.Float( -5, +5 ), FxRandom.Float( -5, +5 ) );

		left = lifespan;
		paint();
	}

	public void resetBurst( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		float a = FxRandom.angle(), s = FxRandom.Float( 16, 32 );
		speed.set( (float)Math.cos( a ) * s, (float)Math.sin( a ) * s );

		left = lifespan;
		paint();
	}

	@Override
	public void update() {
		super.update();
		paint();
	}

	//its size in whole steps, its colour white-hot then cooling, its alpha fading
	private void paint(){
		float p = 1f - left / lifespan;
		am = 1f - p;
		size( sizeAt( p ) );
		color( colorAt( p ) );
	}

	/** Its size at a share of its life: 1, then 3, then 5 px. */
	public static int sizeAt( float p ){
		return SIZES[Math.min( SIZES.length - 1, (int)(Math.max( 0f, p ) * SIZES.length) )];
	}

	/** Its colour at a share of its life. */
	public static int colorAt( float p ){
		if (p < HOT_FOR) return Ramp.lerp( HOT, ROSE, p / HOT_FOR );
		return Ramp.lerp( ROSE, VIOLET, (p - HOT_FOR) / (1f - HOT_FOR) );
	}
}

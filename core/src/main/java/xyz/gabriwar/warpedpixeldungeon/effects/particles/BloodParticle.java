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
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PointF;

//a drop of blood: a square of whole pixels shrinking a pixel at a time (4, 3, 2, 1 at each quarter of
//its life, never between) and darkening as it goes, falling a little; or (BURST) flung out from a
//wound. Matter: drawn as it is, never added. Its randomness the effects' own (it is made on the
//render thread). Used in the world and on the boss's health bar alike
public class BloodParticle extends PixelParticle {

	/** Its colours: fresh, then darker for the second half of its life. */
	public static final int FRESH = 0xCC0000, DARK = 0x8A0A14;

	public static final Emitter.Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((BloodParticle)emitter.recycle( BloodParticle.class )).reset( x, y );
		}
	};

	public static final Emitter.Factory BURST = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((BloodParticle)emitter.recycle( BloodParticle.class )).resetBurst( x, y );
		}
	};

	private boolean burst;

	public BloodParticle() {
		super();

		color( FRESH );
		lifespan = 0.8f;

		acc.set( 0, +40 );
	}

	public void reset( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		lifespan = 0.8f;
		left = lifespan;
		burst = false;

		size( size = 4 );
		color( FRESH );
		speed.set( 0 );
	}

	public void resetBurst( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		speed.polar( FxRandom.Float( PointF.PI2 ), FxRandom.Float( 16, 32 ) );
		lifespan = 0.5f;
		left = lifespan;
		burst = true;
		size( size = 4 );
		color( FRESH );
	}

	/** Its side with `p` of its life left: 4, 3, 2, 1 px. */
	public static int side( float p ){
		return p > 0.75f ? 4 : p > 0.5f ? 3 : p > 0.25f ? 2 : 1;
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;
		int s = side( p );
		if (s != size) size( size = s );
		color( p > 0.5f ? FRESH : DARK );
		am = burst ? Math.min( 1f, p * 2.5f ) : (p > 0.6f ? (1 - p) * 2.5f : 1);
	}
}

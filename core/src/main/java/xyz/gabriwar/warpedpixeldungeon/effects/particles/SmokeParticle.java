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
import com.watabou.noosa.particles.Emitter.Factory;
import com.watabou.utils.PointF;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.Flames;

/**
 * Smoke: a puff of warm grey (the kit's painted puffs, small to middling to large as it spreads),
 * fresh soot-dark and paling as it thins, rising and slowing, leaning with a third of an open
 * sky's wind, never more than 0.55 opaque. FACTORY (net id "smoke") puffs where it is asked; SPEW
 * (Tengu's) throws them up and away to the right.
 */
public class SmokeParticle extends FxParticle {

	/** Its colours: fresh, then paling as it thins. Its alpha at most. */
	public static final Ramp RAMP = new Ramp( 0x5A5048, 0x6E665C, 0x8A847C );
	public static final float ALPHA = 0.55f;

	//a puff's sizes, one frame each, as the stages it grows through
	private static final int[][][] M = { { FxFrames.PUFF_M[0] }, { FxFrames.PUFF_M[1] } };
	private static final int[][][] L = { { FxFrames.PUFF_L[0] }, { FxFrames.PUFF_L[1] } };

	public static final Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!emitter.visible || !FxBudget.keep( FxBudget.P2, x, y )) return;
			puff( Flames.make( emitter, SmokeParticle.class, x, y ), FxRandom.Float( -6f, 6f ), -FxRandom.Float( 8f, 14f ) );
		}
	};

	public static final Factory SPEW = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!emitter.visible || !FxBudget.keep( FxBudget.P2, x, y )) return;
			float a = FxRandom.Float( PointF.PI * 1.7f, PointF.PI * 1.8f ), v = FxRandom.Float( 30f, 60f );
			puff( Flames.make( emitter, SmokeParticle.class, x, y ), (float)Math.cos( a ) * v, (float)Math.sin( a ) * v,
					FxRandom.Float( 0.8f, 1.2f ) ).drag( 2f ).gravity( -8f );
		}
	};

	/** Sets `p` up as a puff off at (vx, vy): growing, paling, slowing and rising, 1.4-2.4 s.
	 *  Matter, P2. */
	public static FxParticle puff( FxParticle p, float vx, float vy ){
		return puff( p, vx, vy, FxRandom.Float( 1.4f, 2.4f ) );
	}

	/** The same, living `life` s. */
	public static FxParticle puff( FxParticle p, float vx, float vy, float life ){
		int v = FxRandom.Int( 2 );
		return p.look( FxFrames.PUFF_S[v] ).then( 0.35f, M[v] ).then( 0.7f, L[v] ).ramp( RAMP, false ).smooth()
				.life( life ).alpha( ALPHA, life * 0.2f, life * 0.45f ).vel( vx, vy ).drag( 0.8f ).gravity( -6f )
				.turbulence( FxRandom.Float( 1f, 3f ), FxRandom.Float( 2f, 5f ) ).wind( 0.35f ).priority( FxBudget.P2 );
	}
}

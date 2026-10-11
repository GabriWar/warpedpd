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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.watabou.noosa.particles.Emitter;

/**
 * A poppolar pod bursting (PopGas's pop, NetVisuals' "poppolar"; the burst keeps its 60): its first
 * emit a white-hot core for 0.05 s, every other a spark of the pod's red thrown out at 32-72 px/s,
 * a 1x3 streak turned to its way and a dot once slow, flowing from white through the pod's reds as
 * it falls (120 px/s^2, drag 1). Light, added; the first 12 are the burst's body (P1), the rest its
 * spray (P2). The effects' own randomness.
 */
public class PoppolarExplosionParticle extends FxParticle {

	/** The pod's sparks, hottest first. */
	public static final Ramp POP = new Ramp( 0xFFFFFF, 0xF0F0FF, 0xE04040, 0xAC2121, 0x6A1010 );
	/** The burst's body: this many sparks of it are never thinned below P1. */
	public static final int BODY = 12;

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			int p = index == 0 ? FxBudget.P0 : (index < BODY ? FxBudget.P1 : FxBudget.P2);
			if (!FxBudget.keep( p, x, y )) return;
			PoppolarExplosionParticle s = (PoppolarExplosionParticle)emitter.recycle( PoppolarExplosionParticle.class );
			if (index == 0) s.resetCore( x, y );
			else s.resetSpark( x, y, p );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	public void resetCore( float x, float y ) {
		reset( Math.round( x ), Math.round( y ) );
		look( FxFrames.CORE_FLASH ).paint( 0xFFF0F0 ).light( true ).life( 0.05f ).alpha( 1f, 0, 0.05f ).snap()
				.priority( FxBudget.P0 );
	}

	public void resetSpark( float x, float y, int priority ) {
		reset( x, y );
		look( FxFrames.SPARK_3 ).orient().slow( FxFrames.MOTE_1, 15f ).ramp( POP, true ).life( FxRandom.Float( 0.3f, 0.9f ) )
				.alpha( 1f, 0, 0.15f ).polar( FxRandom.angle(), FxRandom.Float( 32f, 72f ) ).gravity( 120f ).drag( 1f )
				.priority( priority );
	}
}

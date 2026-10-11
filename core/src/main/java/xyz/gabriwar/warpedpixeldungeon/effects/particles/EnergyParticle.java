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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.Arc;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;

//energy streaming into a point (a recharge, a wand's charge): a 1x2 spark of light pointing at
//where it is going, twinkling smoothly, warming from white to a pale gold; where the stream lands
//a small glow pops (at most one a POP_GAP). Drawn from the light layer, over the night's tint.
public class EnergyParticle extends FxParticle {

	public static final Emitter.Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			Emitter host = Fx.lightCarrier( EnergyParticle.class );
			((EnergyParticle)(host != null ? host : emitter).recycle( EnergyParticle.class )).resetEnergy( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	/** White, then the pale gold it always was, then a deeper gold as it lands. */
	public static final Ramp RAMP = new Ramp( 0xFFFFFF, 0xFFFFAA, 0xF0D060 );
	/** Its life, its speed, its twinkle (a share of its alpha, rad/s). */
	public static final float LIFE = 1f, SPEED_MIN = 24f, SPEED_MAX = 32f, TWINKLE = 0.3f, TWINKLE_RATE = 18f;
	/** The fewest seconds between two landing pops. */
	public static final float POP_GAP = 0.12f;

	private static float lastPop = -1;
	private float phase;

	public EnergyParticle() {
		super();
	}

	/** Off from a point on a circle, streaming into (x, y) over its life. */
	public void resetEnergy( float x, float y ) {
		float a = FxRandom.angle(), s = FxRandom.Float( SPEED_MIN, SPEED_MAX );
		float vx = (float)Math.cos( a ) * s, vy = (float)Math.sin( a ) * s;
		reset( x - vx * LIFE, y - vy * LIFE ).look( FxFrames.SPARK_2 ).orient().ramp( RAMP, true )
				.life( LIFE ).alpha( 1f, 0.35f, 0.15f ).vel( vx, vy ).source( x, y ).priority( FxBudget.P1 )
				.fate( LANDED );
		phase = FxRandom.angle();
	}

	@Override
	public void update() {
		super.update();
		if (alive) am *= 1f - TWINKLE * (0.5f + 0.5f * (float)Math.sin( phase + age * TWINKLE_RATE ));
	}

	//where the stream lands, a pop of light now and then
	private static final Fate LANDED = ( p, landed ) -> {
		if (Game.timeTotal - lastPop < POP_GAP && lastPop >= 0) return;
		lastPop = Game.timeTotal;
		xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight l = Arc.glow( p.px, p.py, 11, 0xFFF0A0, 0.45f );
		if (l != null) l.life( 0, 0.03f, 0.15f );
	};
}

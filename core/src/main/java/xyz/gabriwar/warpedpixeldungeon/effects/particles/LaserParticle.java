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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.watabou.noosa.particles.Emitter;

//a laser's sparks: a 1x2 streak of light turned to its way, twinkling on a smooth sine, white
//cooling to the laser's cyan; FACTORY streams them into a point (a charge), BURST throws them out.
//Drawn from the light layer, over the night's tint, hidden where the hero does not see.
public class LaserParticle extends FxParticle {
    public static final Emitter.Factory FACTORY = new Emitter.Factory() {
        @Override
        public void emit( Emitter emitter, int index, float x, float y ) {
            ((LaserParticle)host( emitter ).recycle( LaserParticle.class )).resetIn( x, y );
        }
        @Override
        public boolean lightMode() {
            return true;
        }
    };

    public static final Emitter.Factory BURST = new Emitter.Factory() {
        @Override
        public void emit( Emitter emitter, int index, float x, float y ) {
            ((LaserParticle)host( emitter ).recycle( LaserParticle.class )).resetOut( x, y );
        }
        @Override
        public boolean lightMode() {
            return true;
        }
    };

    /** White to the laser's cyan. */
    public static final Ramp RAMP = new Ramp( 0xFFFFFF, 0xB8F0FF, 0x5CDAFF );
    /** The twinkle: a share of its alpha, at rad/s. */
    public static final float TWINKLE = 0.35f, TWINKLE_RATE = 16f;

    private float phase;

    private static Emitter host( Emitter caller ){
        Emitter e = Fx.lightCarrier( LaserParticle.class );
        return e != null ? e : caller;
    }

    public LaserParticle() {
        super();
    }

    /** Streaming into (x, y) over a second from 24-32 px off. */
    public void resetIn( float x, float y ) {
        float a = FxRandom.angle(), s = FxRandom.Float( 24, 32 );
        float vx = (float)Math.cos( a ) * s, vy = (float)Math.sin( a ) * s;
        reset( x - vx, y - vy ).look( FxFrames.SPARK_2 ).orient().ramp( RAMP, true ).life( 1f )
                .alpha( 1f, 0.4f, 0.2f ).vel( vx, vy ).source( x, y ).priority( FxBudget.P2 );
        phase = FxRandom.angle();
    }

    /** Thrown out of (x, y) at 16-32 px/s, slowing, half a second. */
    public void resetOut( float x, float y ) {
        reset( x, y ).look( FxFrames.SPARK_2 ).orient().slow( FxFrames.MOTE_1, 6f ).ramp( RAMP, true )
                .life( FxRandom.Float( 0.5f, 0.9f ) ).alpha( 1f, 0, 0.3f ).polar( FxRandom.angle(), FxRandom.Float( 16, 32 ) )
                .drag( 1.5f ).source( x, y ).priority( FxBudget.P2 );
        phase = FxRandom.angle();
    }

    @Override
    public void update() {
        super.update();
        if (alive) am *= 1f - TWINKLE * (0.5f + 0.5f * (float)Math.sin( phase + age * TWINKLE_RATE ));
    }
}

/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

package xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;
import com.watabou.noosa.particles.PixelParticle;

public class BallcropPoisonParticle extends PixelParticle {

	/** Its plant's colour (what its plant bursts on a sprite, without making one of these). */
	public static final int COLOR = 0x2F791F;

	public static final Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((BallcropPoisonParticle)emitter.recycle( BallcropPoisonParticle.class )).reset( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	//its flicker: a phase of its own, run at a rate of its own
	private float phase, rate;

	public BallcropPoisonParticle() {
		size( 2.0f );
		color( COLOR );
		this.acc.set( 0.0f, 50.0f );
	}

	public void reset( float f, float f2 ) {
		revive();
		this.x = f;
		this.y = f2;
		f = FxRandom.Float( 0.5f, 1.0f );
		this.lifespan = f;
		this.left = f;
		this.speed.polar( -FxRandom.Float( 3.1415925f ), FxRandom.Float( 20.0f, 40.0f ) );
		phase = FxRandom.angle();
		rate = FxRandom.Float( 18f, 30f );
	}

	@Override
	public void update() {
		super.update();
		//flickering between nothing and its shrinking most, in whole pixels
		phase += Game.elapsed * rate;
		int s = Math.round( (5.0f * this.left / this.lifespan) * (0.5f + 0.5f * (float)Math.sin( phase )) );
		visible = s > 0;
		size( s );
	}
}

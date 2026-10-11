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

package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;

/**
 * A twinkle of light lifting off: a dot opening to a plus and closing again (MOTE_1, PLUS_3,
 * MOTE_1 over its 0.35 s), pale gold (FEFFBE), rising and speeding up, on whole pixels, in the
 * light layer.
 */
public class LightParticle extends FxParticle {

	public static final int COLOR = 0xFEFFBE;
	public static final float LIFE = 0.35f, RISE = -40f;
	private static final int[][] TWINKLE = { FxFrames.MOTE_1, FxFrames.PLUS_3, FxFrames.MOTE_1 };

	public static final Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxBudget.keep( FxBudget.P3, x, y )) return;
			Emitter carrier = Fx.lightCarrier( LightParticle.class );
			((LightParticle)(carrier != null ? carrier : emitter).recycle( LightParticle.class )).reset( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	@Override
	public LightParticle reset( float x, float y ) {
		super.reset( x, y );
		look( TWINKLE, 0 ).paint( COLOR ).light( true ).life( LIFE * FxRandom.Float( 0.85f, 1.15f ) )
				.alpha( 1f, 0, 0.12f ).vel( 0, -FxRandom.Float( 2f, 6f ) ).gravity( RISE ).snap().priority( FxBudget.P3 );
		return this;
	}
}

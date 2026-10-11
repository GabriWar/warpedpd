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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import com.watabou.noosa.particles.Emitter;

/**
 * Poison. SPLASH (NetVisuals' "poison"): drops of venom flung up and out, turned to their way, a
 * white glint for their first 0.05 s and then stepping from green to yellow-green as they fall,
 * landing 8 px below - a splat for 0.15 s on the floor, a small ring on the water. Matter, drawn
 * normally (it was added). MISSILE: a bolt's trail of venom light, flowing down the poison's ramp.
 * The effects' own randomness; thinned by their budget.
 */
public class PoisonParticle extends FxParticle {

	/** A splash drop: white for its first share of life, then green stepping to yellow-green. */
	public static final Ramp SPLASH_RAMP = new Ramp( 0xFFFFFF, 0x6CCC3C, 0x8ADE4E, 0xB8F060 ).keyed( 0f, 0.08f, 0.5f, 1f );
	/** A missile's trail: the poison's light down its ramp. */
	public static final Ramp MISSILE_RAMP = new Ramp( 0xF0FFB0, 0xB8F060, 0x6CCC3C, 0x2F8A2A );
	/** A splash drop's life (its first 0.05 s white), and how far below it lands. */
	public static final float SPLASH_LIFE = 0.6f, LANDS = 8f;
	private static final int[][] DROPS = { FxFrames.DROP_2, FxFrames.DROP_3 };

	public static final Emitter.Factory MISSILE = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			((PoisonParticle)emitter.recycle( PoisonParticle.class )).resetMissile( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	public static final Emitter.Factory SPLASH = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxBudget.keep( FxBudget.P1, x, y )) return;
			((PoisonParticle)emitter.recycle( PoisonParticle.class )).resetSplash( x, y );
		}
	};

	//a splash drop's landing: a ring on water, a moment's splat on the floor
	private static final Fate LANDING = ( p, landed ) -> {
		if (!landed) return;
		if (WaterFX.onWater( p.px, p.py )){
			WaterFX.drop( p.px, p.py, 0.1f, Element.POISON.ramp.stop( 2 ) );
			return;
		}
		Emitter e = (Emitter)p.parent;
		if (e == null || !FxBudget.keep( FxBudget.P3 )) return;
		((FxParticle)e.recycle( FxParticle.class )).reset( p.px, p.py ).look( FxFrames.SPLAT[0] ).paint( p.colorNow() )
				.life( 0.15f ).alpha( 0.9f, 0, 0.1f ).snap().priority( FxBudget.P3 );
	};

	public void resetMissile( float x, float y ) {
		reset( x, y );
		float life = FxRandom.Float( 0.4f, 0.6f );
		look( FxRandom.chance( 0.6f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).ramp( MISSILE_RAMP, true ).life( life )
				.alpha( 1f, 0, life * 0.6f ).polar( -FxRandom.Float( (float)Math.PI ), FxRandom.Float( 6f ) ).gravity( 20f )
				.priority( FxBudget.P2 );
	}

	public void resetSplash( float x, float y ) {
		reset( x, y );
		look( FxRandom.element( DROPS ) ).orient().ramp( SPLASH_RAMP, false ).life( SPLASH_LIFE ).alpha( 1f, 0, 0.1f )
				.polar( -FxRandom.Float( 0.1f, 0.9f ) * (float)Math.PI, FxRandom.Float( 20f, 40f ) ).gravity( 160f )
				.ground( y + LANDS, DIE, 0 ).fate( LANDING ).priority( FxBudget.P1 );
	}
}

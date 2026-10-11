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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import com.watabou.noosa.particles.Emitter;

/**
 * Acid. SPLASH (NetVisuals' "corrosion"): drops of acid flung up and out, stepping down the acid's
 * ramp, landing 8 px below (a moment's splat, a ring on the water); the burst's first also hisses -
 * two sizzle sparks (FFB040, added over the night's tint, 0.2 s) and a grey puff of fumes. MISSILE:
 * a trail of acid specks with a puff of fumes about every 1.2 s of a bolt's flight. Matter, drawn
 * normally. The effects' own randomness; thinned by their budget.
 */
public class CorrosionParticle extends FxParticle {

	/** The fumes' colour and alpha, and the sizzle's colour. */
	public static final int FUMES = 0x9A968A, SIZZLE = 0xFFB040;
	public static final float FUMES_ALPHA = 0.3f;
	/** A trail's chance of a puff of fumes an emit (a bolt pours one every 0.03 s: one a 1.2 s). */
	public static final float TRAIL_FUMES = 1 / 40f;
	private static final int[][] DROPS = { FxFrames.DROP_2, FxFrames.DROP_3 };

	public static final Emitter.Factory MISSILE = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			((CorrosionParticle)emitter.recycle( CorrosionParticle.class )).resetMissile( x, y );
			if (FxRandom.Float() < TRAIL_FUMES) fumes( emitter, x, y );
		}
		@Override
		public boolean lightMode() {
			return false;
		}
	};

	public static final Emitter.Factory SPLASH = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxBudget.keep( FxBudget.P1, x, y )) return;
			((CorrosionParticle)emitter.recycle( CorrosionParticle.class )).resetSplash( x, y );
			if (index == 0){
				sizzle( x, y );
				fumes( emitter, x, y );
			}
		}
		@Override
		public boolean lightMode() {
			return false;
		}
	};

	//a splash drop's landing: a ring on water, a moment's splat on the floor
	private static final Fate LANDING = ( p, landed ) -> {
		if (!landed) return;
		if (WaterFX.onWater( p.px, p.py )){
			WaterFX.drop( p.px, p.py, 0.1f, Element.ACID.ramp.stop( 2 ) );
			return;
		}
		Emitter e = (Emitter)p.parent;
		if (e == null || !FxBudget.keep( FxBudget.P3 )) return;
		((FxParticle)e.recycle( FxParticle.class )).reset( p.px, p.py ).look( FxFrames.SPLAT[0] ).paint( p.colorNow() )
				.life( 0.15f ).alpha( 0.9f, 0, 0.1f ).snap().priority( FxBudget.P3 );
	};

	/** Two sparks of sizzle over the night's tint. */
	static void sizzle( float x, float y ){
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		for (int i = 0; i < 2; i++){
			if (!FxBudget.keep( FxBudget.P3, x, y )) return;
			((FxParticle)glow.recycle( FxParticle.class )).reset( x, y ).look( FxFrames.MOTE_1 ).paint( SIZZLE ).light( true )
					.life( 0.2f ).alpha( 1f, 0, 0.1f ).polar( FxRandom.Float( (float)-Math.PI, 0f ), FxRandom.Float( 12f, 24f ) )
					.gravity( 60f ).priority( FxBudget.P3 );
		}
	}

	/** A grey puff of fumes rising off the acid. */
	static void fumes( Emitter e, float x, float y ){
		if (!FxBudget.keep( FxBudget.P2, x, y )) return;
		float life = FxRandom.Float( 0.9f, 1.3f );
		((FxParticle)e.recycle( FxParticle.class )).reset( x, y - 2 ).look( FxFrames.PUFF_S[FxRandom.Int( 2 )] ).paint( FUMES )
				.smooth().life( life ).alpha( FUMES_ALPHA, life * 0.2f, life * 0.5f ).vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 8f, 12f ) )
				.drag( 0.08f ).turbulence( 1f, 3f ).wind( 0.35f ).priority( FxBudget.P2 );
	}

	public void resetMissile( float x, float y ) {
		reset( x, y );
		float life = FxRandom.Float( 0.4f, 0.6f );
		look( FxRandom.chance( 0.6f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).ramp( Element.ACID.ramp, false ).life( life )
				.alpha( 1f, 0, life * 0.5f ).polar( -FxRandom.Float( (float)Math.PI ), FxRandom.Float( 6f ) ).gravity( 30f )
				.priority( FxBudget.P2 );
	}

	public void resetSplash( float x, float y ) {
		reset( x, y );
		look( FxRandom.element( DROPS ) ).orient().ramp( Element.ACID.ramp, false ).life( 0.6f ).alpha( 1f, 0, 0.1f )
				.polar( -FxRandom.Float( 0.1f, 0.9f ) * (float)Math.PI, FxRandom.Float( 20f, 40f ) ).gravity( 160f )
				.ground( y + 8f, DIE, 0 ).fate( LANDING ).priority( FxBudget.P1 );
	}
}

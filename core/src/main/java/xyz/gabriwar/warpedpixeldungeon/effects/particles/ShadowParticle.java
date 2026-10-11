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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.gas.Shade;
import com.watabou.noosa.particles.Emitter;

/**
 * Shadow, as ink: one painted frame, its rim a violet that catches what light there is and its
 * core near black, so it reads on the darkest floor without being light (matter, drawn normally).
 * UP ("shadow_up"): a two-frame curl of ink rising 24-36 px/s, swaying. CURSE ("shadow_curse"):
 * strands drawn in from 16-32 px, turned to their way; the burst's first, as it arrives, flashes a
 * violet star (PLUS_5, added over the night's tint, 0.15 s) once. MISSILE ("shadow_missile"): blots
 * of ink shrinking to specks. The effects' own randomness; thinned by their budget.
 */
public class ShadowParticle extends FxParticle {

	/** Ink's rim and core, and the curse's flash. */
	public static final int RIM = 0x9A5AD0, INK = 0x1A0C2C, FLASH = 0xA070E0;
	/** A curse's strands come in from this far, px, over this long. */
	public static final float CURSE_FROM_MIN = 16f, CURSE_FROM_MAX = 32f, CURSE_LIFE = 0.5f, FLASH_LIFE = 0.15f;
	private static final int[][] CURL_A = { FxFrames.INK_WISP[0], FxFrames.INK_WISP[1] },
			CURL_B = { FxFrames.INK_WISP[1], FxFrames.INK_WISP[2] };
	private static final int[][] BLOT = { FxFrames.INK_CORE }, SPECK = { FxFrames.MOTE_2 };

	public static final Emitter.Factory MISSILE = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			((ShadowParticle)emitter.recycle( ShadowParticle.class )).resetMissile( x, y );
		}
	};

	public static final Emitter.Factory CURSE = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (index != 0 && !FxBudget.keep( FxBudget.P2, x, y )) return;
			((ShadowParticle)emitter.recycle( ShadowParticle.class )).resetCurse( x, y, index == 0 );
		}
	};

	public static final Emitter.Factory UP = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			((ShadowParticle)emitter.recycle( ShadowParticle.class )).resetUp( x, y );
		}
	};

	//the curse's first strand arriving: a violet star, once
	private static final Fate FLASHES = ( p, landed ) -> {
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		((FxParticle)glow.recycle( FxParticle.class )).reset( Math.round( p.px ), Math.round( p.py ) ).look( FxFrames.PLUS_5 )
				.paint( FLASH ).light( true ).life( FLASH_LIFE ).alpha( 1f, 0, FLASH_LIFE ).snap().priority( FxBudget.P0 );
	};

	//ink: its rim violet, its core near black
	private void ink(){
		Shade.map( this, RIM, Shade.CROWN, INK, Shade.RIM );
	}

	/** A blot of ink: INK_CORE shrinking to a speck, drifting, 0.5 s. */
	public void resetMissile( float x, float y ) {
		reset( x, y );
		look( BLOT, 0 ).then( 0.5f, SPECK ).life( 0.5f ).alpha( 0.9f, 0.05f, 0.25f )
				.vel( FxRandom.Float( -5f, 5f ), FxRandom.Float( -5f, 5f ) ).priority( FxBudget.P2 );
		ink();
	}

	public void resetCurse( float x, float y, boolean first ) {
		reset( x, y );
		float a = FxRandom.angle(), speed = FxRandom.Float( CURSE_FROM_MIN, CURSE_FROM_MAX ) / CURSE_LIFE;
		look( FxFrames.INK_WISP[FxRandom.Int( FxFrames.INK_WISP.length )] ).orient().life( CURSE_LIFE ).alpha( 0.95f, 0.15f, 0.1f )
				.at( x - (float)Math.cos( a ) * speed * CURSE_LIFE, y - (float)Math.sin( a ) * speed * CURSE_LIFE )
				.polar( a, speed ).priority( first ? FxBudget.P0 : FxBudget.P2 );
		if (first) fate( FLASHES );
		ink();
	}

	public void resetUp( float x, float y ) {
		reset( x, y );
		look( FxRandom.chance( 0.5f ) ? CURL_A : CURL_B, FxRandom.Float( 5f, 7f ) ).life( 1f ).alpha( 0.9f, 0.25f, 0.5f ).smooth()
				.vel( FxRandom.Float( -6f, 6f ), -FxRandom.Float( 24f, 36f ) ).drag( 0.6f ).sway( FxRandom.Float( 4f, 8f ), FxRandom.Float( 4f, 7f ) )
				.priority( FxBudget.P2 );
		if (FxRandom.chance( 0.5f )) scale.x = -1;
		ink();
	}
}

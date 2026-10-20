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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * DREAD, dread pouring in and an eye opening (Eldritch, Horror, Corrupting, Jupiter's horror,
 * Obfuscation): a ring of the look's rim closing in on the feet in 0.2 s (23 px to 9) and a pool of
 * ink spreading under them for 1.2 s (wider from STRONG); wisps of ink peeling up off the body
 * (the rest of the tally) and, from MID, an eye of light opening over the head for 0.4 s, in the
 * look's eye colour (its rim's unless the row names one: Eldritch's red); the body shivering,
 * sinking a pixel and darkened a moment. Dread is in the mind: it leaves nothing on the floor. Its death accent: the ink collapsing into a wide dark pool, wisps
 * rising. Gained: ink pouring into the item from both sides and the eye opening on it; ended with
 * ink closing in over the owner.
 */
public final class DreadFx {

	private DreadFx(){}

	/** The ring closing in, widest first; the eye opening and closing over its 0.4 s. */
	public static final int[][] CLOSING = { FxFrames.RING_23, FxFrames.RING_17, FxFrames.RING_13, FxFrames.RING_9 };
	public static final int[][] EYE = { FxFrames.Light.EYE_7[0], FxFrames.Light.EYE_7[1], FxFrames.Light.EYE_7[1], FxFrames.Light.EYE_7[0] };
	public static final int DARK = 0x1A0C2C;
	public static final float EYE_FOR = 0.4f;

	static int ink( Beat b, int stop ){
		return b.c( b.el.ramp.stop( Math.min( stop, b.el.ramp.size() - 1 ) ) );
	}

	//ink peeling off (x, y), rising slowly and curling
	static FxParticle wisp( Beat b, float x, float y, int pri ){
		FxParticle p = b.mat( x, y, pri );
		if (p == null) return null;
		return p.look( FxFrames.INK_WISP, 3f ).paint( ink( b, 3 ) ).smooth().vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 6f, 10f ) )
				.turbulence( 1.2f, 5f ).life( FxRandom.Float( 0.8f, 1.2f ) ).alpha( b.a( 0.85f ), 0.1f, 0.5f );
	}

	/** An eye of light opening over (x, y) in a colour. */
	public static Flash eye( Beat b, float x, float y, int color ){
		return b.flash( true, EYE, 0f, x, y, color, 0.9f, EYE_FOR );
	}

	public static void play( Beat b ){
		int t = b.tier;
		if (!b.join && !b.curse){
			b.flash( false, true, CLOSING, 0f, b.cx, b.feet, b.look.rim, 0.75f, 0.2f );
			FxParticle p = b.mat( b.cx, b.feet + 1f, FxBudget.P1 );
			if (p != null){
				p.look( FxFrames.INK_CORE ).paint( ink( b, 4 ) ).life( 1.2f ).alpha( b.a( 0.7f ), 0.15f, 0.6f ).snap();
				p.scale.set( t >= STRONG ? 3 : 2, t >= STRONG ? 2 : 1 );
			}
		}
		if (t >= MID && !b.join && !b.echo) eye( b, b.cx, b.top - 4f, b.look.eye );
		int k = b.burst( b.n( t + 2 ), FxBudget.P2 );
		for (int i = 0; i < k; i++) wisp( b, b.cx + FxRandom.Float( -5f, 5f ), b.cy + FxRandom.Float( -2f, 5f ), FxBudget.P2 );
		b.react( DARK, 0.25f, Beat.SHIVER, 1 );
		if (!b.echo && !b.join && !b.minimal && b.ds != null){
			final Beat sink = b;
			later( REACT_AT + 0.15f, () -> sink.moveNow( sink.ds, Beat.SINK, 1 ) );
		}
	}

	/** The ink collapsing: a wide dark pool where the body stood, wisps rising off it. */
	public static void finisher( Beat b ){
		FxParticle p = b.mat( b.cx, b.feet + 1f, FxBudget.P0 );
		if (p != null){
			p.look( FxFrames.INK_CORE ).paint( ink( b, 5 ) ).life( 1.4f ).alpha( b.a( 0.8f ), 0.1f, 0.7f ).snap();
			p.scale.set( 3, 2 );
		}
		int n = Math.max( 2, Math.round( 4 * b.scale ) );
		for (int i = 0; i < n; i++) wisp( b, b.cx + (i - (n - 1) / 2f) * 3f, b.feet - 2f, FxBudget.P1 );
	}

	/** Ink pouring into (x, y) from both sides over `seconds`, the eye opening on it at the end. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		final Beat b = Beat.at( l, x, y, GATHER_CAP );
		for (int i = 0; i < 6; i++){
			float side = i % 2 == 0 ? -1f : 1f, ox = side * (8f + 2f * (i / 2)), oy = FxRandom.Float( -3f, 3f );
			FxParticle p = b.mat( x + ox, y + oy, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.INK_WISP, 3f ).paint( ink( b, 3 ) ).smooth().vel( -ox / seconds, -oy / seconds ).life( seconds )
					.alpha( 0.9f, seconds * 0.2f, 0.15f );
		}
		final Beat e = b.split( 1 );
		later( seconds * 0.7f, () -> eye( e, x, y - 8f, l.eye ) );
	}

	/** Ink closing in over the owner at (x, y): its ring and wisps. */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 3, Math.round( FINALE_CAP * scale ) ) );
		b.flash( false, true, CLOSING, 0f, x, b.feet, l.rim, 0.7f, 0.25f );
		int n = Math.max( 2, Math.round( 4 * scale ) );
		for (int i = 0; i < n; i++) wisp( b, x + FxRandom.Float( -5f, 5f ), y + FxRandom.Float( 0f, 5f ), FxBudget.P1 );
	}
}

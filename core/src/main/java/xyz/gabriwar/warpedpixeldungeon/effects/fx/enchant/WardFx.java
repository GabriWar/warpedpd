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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * WARD, a ward locking into runes (Blocking, Crystal's glass, AntiMagic, the crystal glyph): the
 * quarter of a shield's rim on the side the blow came from (a glyph's ward arc comes first, from
 * EnchantFx) and a bubble of light two frames round the body; runes snapping one by one into a ring
 * round it, 0.08 s apart, each fading in 0.3 s (two at WEAK, up to four); a ring at the feet. No
 * motion: a ward holds. Its death accent: the rune ring snapping shut. Gained: runes locking on the
 * item one by one; ended with a ring of runes rising round the owner.
 */
public final class WardFx {

	private WardFx(){}

	public static final float RUNE_GAP = 0.08f, RUNE_FADE = 0.3f;
	static final int[] RUNES = { 2, 3, 4, 4 };
	static final int[][] BUBBLE_23 = { FxFrames.Light.BUBBLE_23 };

	//a rune locking onto the ring round (x, y), `late` s on
	static FxParticle rune( Beat b, float x, float y, float a, float rx, float ry, float late, int pri ){
		FxParticle p = b.lit( x + (float)Math.cos( a ) * rx, y + (float)Math.sin( a ) * ry, pri );
		if (p == null) return null;
		return p.look( FxFrames.RUNE_3[FxRandom.Int( FxFrames.RUNE_3.length )] ).ramp( b.ramp(), true )
				.life( late + RUNE_FADE + 0.1f ).alpha( b.a( 0.95f ), late, RUNE_FADE ).snap();
	}

	public static void play( Beat b ){
		int t = b.tier;
		if (!b.join && !b.curse){
			if (!b.ward){
				Flash arc = b.flash( true, ARCS[HitFx.octant( -b.dx, -b.dy )], 0f, b.cx, b.cy, b.look.light, 0.85f, 0.3f );
				if (arc != null) arc.envelope( 0f, 0.067f );
			}
			Flash bubble = b.flash( true, BUBBLE_23, 0f, b.cx, b.cy, b.look.light, b.echo ? 0.3f : 0.45f, 0.14f );
			if (bubble != null) bubble.envelope( 0f, 0.067f );
		}
		int n = Math.min( b.left(), b.n( RUNES[t] ) );
		float a0 = FxRandom.angle();
		for (int i = 0; i < n; i++){
			if (rune( b, b.cx, b.cy, a0 + (float)(2 * Math.PI * i / Math.max( 1, n )), 8f, 4f, RUNE_GAP * i, FxBudget.P1 ) == null) break;
		}
		if (t >= MID) b.ring( b.cx, b.feet, 13, 17, 0.4f, b.look.light, 0.6f, true );
	}

	/** The rune ring snapping shut on the body. */
	public static void finisher( Beat b ){
		int n = Math.max( 3, Math.round( 6 * b.scale ) );
		float a0 = FxRandom.angle();
		for (int i = 0; i < n; i++){
			float a = a0 + (float)(2 * Math.PI * i / n);
			FxParticle p = b.lit( b.cx + (float)Math.cos( a ) * 9f, b.cy + (float)Math.sin( a ) * 4.5f, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.RUNE_3[i % FxFrames.RUNE_3.length] ).ramp( b.ramp(), true )
					.vel( -(float)Math.cos( a ) * 9f / 0.2f, -(float)Math.sin( a ) * 4.5f / 0.2f ).life( 0.22f ).alpha( b.a( 1f ), 0, 0.06f );
		}
	}

	/** Runes locking onto (x, y) one by one over `seconds`. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		Beat b = Beat.at( l, x, y, GATHER_CAP );
		int n = 6;
		for (int i = 0; i < n; i++){
			float late = seconds * 0.8f * i / n;
			FxParticle p = rune( b, x, y, (float)(2 * Math.PI * i / n), 10f, 4f, late, FxBudget.P1 );
			if (p != null) p.life( seconds + 0.1f ).alpha( 0.95f, late, 0.15f );
		}
	}

	/** A ring of runes rising round the owner at (x, y). */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 3, Math.round( FINALE_CAP * scale ) ) );
		int n = Math.max( 3, Math.round( 6 * scale ) );
		for (int i = 0; i < n; i++){
			FxParticle p = rune( b, x, b.feet - 2f, (float)(2 * Math.PI * i / n), 9f, 3.5f, 0f, FxBudget.P1 );
			if (p != null) p.vel( 0, -18f ).life( 0.6f ).alpha( 0.95f, 0.05f, 0.3f );
		}
	}
}

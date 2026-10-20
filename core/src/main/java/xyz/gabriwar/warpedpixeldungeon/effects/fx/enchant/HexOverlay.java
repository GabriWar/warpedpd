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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * The hex, over any curse's family (EnchantFx.CURSE): where an enchantment's core would flash, light
 * is eaten instead - a VOID implosion, a ring closing from 17 px to 5 at the feet and a spot of ink
 * under them; two or three VOID motes drawn back into the wielder (or the wearer: the curse bites
 * back); a VOID etch on the floor for 2 s; the wielder darkened a moment in VOID's light. Gained (a
 * curse taking hold): motes leaving the item, then ink closing on it; ended with VOID motes drawn
 * into the owner and a closing ring.
 */
public final class HexOverlay {

	private HexOverlay(){}

	/** The ring closing in, widest first. */
	public static final int[][] CLOSING = { FxFrames.RING_17, FxFrames.RING_13, FxFrames.RING_9, FxFrames.RING_5 };
	public static final float ETCH_HOLD = 2f, CLOSE_FOR = 0.25f, TINT_FOR = 0.15f;

	static int voids( Beat b, int stop ){
		return b.c( Element.VOID.ramp.stop( stop ) );
	}

	//a VOID mote at (x, y) drawn into (tx, ty) over `life` s
	static FxParticle inward( Beat b, float x, float y, float tx, float ty, float life, int pri ){
		FxParticle p = b.mat( x, y, pri );
		if (p == null) return null;
		return p.look( FxFrames.MOTE_2 ).paint( voids( b, 1 ) ).vel( (tx - x) / life, (ty - y) / life ).life( life )
				.alpha( b.a( 1f ), 0.05f, 0.1f ).snap();
	}

	public static void play( Beat b ){
		CharSprite wielder = b.ward ? b.ds : b.as;
		Flash ring = b.flash( false, true, CLOSING, 0f, b.cx, b.feet, Element.VOID.light, 0.8f, CLOSE_FOR );
		if (ring != null) ring.envelope( 0.02f, CLOSE_FOR * 0.7f );
		FxParticle ink = b.mat( b.hx, b.hy, FxBudget.P0 );
		if (ink != null) ink.look( FxFrames.INK_CORE ).paint( voids( b, 4 ) ).life( 0.5f ).alpha( b.a( 0.85f ), 0, 0.3f ).snap();
		float tx = wielder != null ? wielder.x + wielder.width() / 2f : b.cx, ty = wielder != null ? wielder.y + wielder.height() / 2f : b.cy;
		int n = b.tier >= STRONG ? 3 : 2;
		for (int i = 0; i < n; i++){
			inward( b, b.hx + FxRandom.Float( -4f, 4f ), b.hy + FxRandom.Float( -4f, 4f ), tx, ty, 0.3f + 0.05f * i, FxBudget.P1 );
		}
		if (!b.onWater( b.cx, b.feet )) b.decal( b.cx, b.feet + 1f, FxFrames.ETCH, voids( b, 3 ), 0.6f, ETCH_HOLD, 0.6f );
		if (!b.echo && wielder != null){
			final CharSprite w = wielder;
			later( REACT_AT, () -> {
				if (w.parent != null) w.afterglow( Element.VOID.light, TINT_FOR );
			} );
		}
	}

	/** A curse taking hold of (x, y): motes leaving it, then ink closing on it. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		final Beat b = Beat.at( l, x, y, 5 );
		for (int i = 0; i < 3; i++){
			float a = (float)(2 * Math.PI * i / 3) + FxRandom.Float( -0.4f, 0.4f );
			FxParticle p = b.mat( x, y, FxBudget.P1 );
			if (p != null) p.look( FxFrames.MOTE_2 ).paint( voids( b, 0 ) ).polar( a, 14f ).drag( 1.5f ).life( seconds * 0.5f )
					.alpha( 1f, 0, 0.2f ).snap();
		}
		final Beat ink = b.split( 1 );
		later( seconds * 0.6f, () -> {
			FxParticle p = ink.mat( x, y, FxBudget.P1 );
			if (p != null) p.look( FxFrames.INK_CORE ).paint( voids( ink, 5 ) ).life( seconds * 0.4f + 0.2f ).alpha( 0.8f, 0.1f, 0.2f ).snap();
		} );
	}

	/** VOID motes drawn into the owner at (x, y), a ring closing at his feet. */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 3, Math.round( 7 * scale ) ) );
		b.flash( false, true, CLOSING, 0f, x, b.feet, Element.VOID.light, 0.7f, CLOSE_FOR );
		int n = Math.max( 2, Math.round( 6 * scale ) );
		for (int i = 0; i < n; i++){
			float a = (float)(2 * Math.PI * i / n);
			inward( b, x + (float)Math.cos( a ) * 10f, y + (float)Math.sin( a ) * 6f, x, y, 0.35f, FxBudget.P1 );
		}
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * FORTUNE, fortune glinting and spinning (Lucky, Unstable, Midas, Luck, Crom's luck; Wondrous under
 * the hex): a star winking over the body (the light sheet's four-point star, along the axes then
 * turned); glints popping out in an arc over it, each turning a quarter every 0.1 s (the rest of the
 * tally) - coins hopping off and bouncing once for a coin look (Midas, Crom's luck, Lucky's clover
 * aside), prism glints for a prism one. Nothing on the floor, no motion: luck does not hurt. Its
 * death accent: a fountain of coins or glints and a pool of gold. Gained: glints orbiting the item
 * faster and faster; ended with a fountain round the owner.
 */
public final class FortuneFx {

	private FortuneFx(){}

	static boolean coins( Beat b ){
		return b.look.rune == FxFrames.COIN_6[0];
	}

	//a glint popping up off (x, y) along an arc, turning as it goes
	static FxParticle glint( Beat b, float x, float y, float a, float speed, int pri ){
		FxParticle p = b.lit( x, y, pri );
		if (p == null) return null;
		boolean prism = b.el == Element.PRISM && !b.echo;
		p.look( FxRandom.chance( 0.5f ) ? FxFrames.STAR4_5 : FxFrames.PLUS_3 );
		if (prism) p.paint( Element.PRISM.ramp.stop( FxRandom.Int( Element.PRISM.ramp.size() ) ) );
		else p.ramp( b.echo ? b.look.pale : (b.el.ember != null ? b.el.ember : b.el.ramp), true );
		return p.polar( a, speed ).drag( 4f ).spin( 0.1f ).life( 0.45f ).alpha( b.a( 1f ), 0, 0.2f ).snap();
	}

	//a coin hopping off (x, y), flipping, bouncing once where it falls
	static FxParticle coin( Beat b, float x, float y, float a, float speed, int pri ){
		FxParticle p = b.mat( x, y, pri );
		if (p == null) return null;
		return p.look( FxFrames.COIN_6, 12f ).paint( b.c( Element.GOLD.ramp.stop( 2 ) ) ).polar( a, speed ).gravity( 160f )
				.ground( b.feet + FxRandom.Float( 0f, 3f ), FxParticle.BOUNCE, 0.4f ).life( 0.8f ).alpha( b.a( 1f ), 0, 0.25f );
	}

	static FxParticle bit( Beat b, float x, float y, float a, float speed, int pri ){
		return coins( b ) ? coin( b, x, y, a, speed * 0.9f, pri ) : glint( b, x, y, a, speed, pri );
	}

	public static void play( Beat b ){
		int t = b.tier;
		if (!b.join && !b.curse){
			Flash w = b.flash( true, STAR_WINK, 0f, b.cx, b.top - 2f, b.look.light, b.echo ? 0.5f : 0.9f, 0.2f );
			if (w != null) w.envelope( 0.02f, 0.08f );
		}
		int k = b.burst( b.n( t + 3 ), FxBudget.P2 );
		for (int i = 0; i < k; i++){
			float a = (float)(-Math.PI / 2) + (i - (k - 1) / 2f) * 0.45f;
			bit( b, b.cx, b.top + 2f, a, FxRandom.Float( 24f, 34f ), FxBudget.P2 );
		}
	}

	/** A fountain of coins or glints, a pool of gold under it. */
	public static void finisher( Beat b ){
		FxLight pool = b.pool( b.cx, b.feet + 2f, 23, Element.GOLD.light, 0.35f );
		if (pool != null) pool.life( 0.05f, 0.2f, 0.4f );
		int n = Math.max( 3, Math.round( 6 * b.scale ) );
		for (int i = 0; i < n; i++){
			bit( b, b.cx, b.cy, (float)(-Math.PI / 2) + FxRandom.Float( -0.7f, 0.7f ), FxRandom.Float( 40f, 60f ), FxBudget.P1 );
		}
	}

	/** Glints orbiting (x, y) faster and faster, tightening, over `seconds`. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		Beat b = Beat.at( l, x, y, GATHER_CAP );
		for (int i = 0; i < 6; i++){
			float a = (float)(2 * Math.PI * i / 6), d = 10f;
			float ox = (float)Math.cos( a ) * d, oy = (float)Math.sin( a ) * d * 0.5f;
			FxParticle p = b.lit( x + ox, y + oy, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.STAR4_5 ).ramp( b.ramp(), true ).vel( -ox / seconds - oy * 2.4f, -oy / seconds + ox * 0.6f )
					.life( seconds ).alpha( 1f, seconds * 0.2f, 0.1f ).spin( 0.15f );
		}
	}

	/** A fountain of coins or glints round the owner at (x, y). */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 3, Math.round( FINALE_CAP * scale ) ) );
		int n = Math.max( 3, Math.round( 7 * scale ) );
		for (int i = 0; i < n; i++){
			bit( b, x, y, (float)(-Math.PI / 2) + (i - (n - 1) / 2f) * 0.35f, FxRandom.Float( 34f, 48f ), FxBudget.P2 );
		}
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * VENOM, dripping and eating the floor (Venomous, Poison, Loki's poison, Viscosity; Corrosion and
 * Stench under the hex): an arc of droplets thrown out and landing (two at WEAK, up to four),
 * bubbles rising off the body and popping (from MID), an etch eaten into the floor for 3 s with a
 * bubble fizzing on it half a second on (from MID; on water a cloud of its colour and a ring), the
 * body sinking a pixel, lit a moment in its light. Its death accent: the body dissolving into
 * bubbles and fumes, an etch. Gained: drips rising from the floor into the item; ended with
 * bubbles popping on the owner.
 */
public final class VenomFx {

	private VenomFx(){}

	public static final float ETCH_HOLD = 3f;
	static final int[] DROPS = { 2, 3, 4, 4 };

	static int color( Beat b ){
		return b.c( MotifFx.matterColor( b.look ) );
	}

	static int dark( Beat b ){
		return b.c( b.el.ramp.stop( Math.min( 3, b.el.ramp.size() - 1 ) ) );
	}

	//a drop thrown off (x, y) on an arc, gone where it lands (the water answers once, in play)
	static FxParticle drop( Beat b, float x, float y, float angle, float speed, int pri ){
		FxParticle p = b.mat( x, y, pri );
		if (p == null) return null;
		return p.look( FxFrames.DROP_3 ).paint( color( b ) ).orient().polar( angle, speed ).gravity( 240f )
				.ground( b.feet + FxRandom.Float( -1f, 3f ), FxParticle.DIE, 0 )
				.life( 0.8f ).alpha( b.a( 1f ), 0, 0.15f );
	}

	//a bubble off (x, y): rising, growing, popping
	static FxParticle bubble( Beat b, float x, float y, int pri ){
		FxParticle p = b.mat( x, y, pri );
		if (p == null) return null;
		return p.look( MotifFx.BUBBLE_RUN, 0 ).paint( color( b ) ).vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 8f, 12f ) )
				.life( FxRandom.Float( 0.5f, 0.7f ) ).alpha( b.a( 1f ), 0.05f, 0.1f ).snap();
	}

	public static void play( Beat b ){
		int t = b.tier;
		if (!b.join && !b.curse){
			int n = b.n( DROPS[t] );
			for (int i = 0; i < n; i++){
				float a = (float)(-Math.PI / 2) + (i - (n - 1) / 2f) * 0.5f + FxRandom.Float( -0.15f, 0.15f );
				if (drop( b, b.hx, b.hy, a, FxRandom.Float( 32f, 50f ), i == 0 ? FxBudget.P0 : FxBudget.P1 ) == null) break;
			}
		}
		if (t >= MID){
			if (b.onWater( b.cx, b.feet )){
				FxParticle c = b.mat( b.cx, b.feet, FxBudget.P2 );
				if (c != null){
					c.look( FxFrames.EDGE_WISP, 0 ).paint( color( b ) ).smooth().vel( FxRandom.Float( -2f, 2f ), 0 ).life( 1.5f )
							.alpha( b.a( 0.35f ), 0.2f, 0.8f );
				}
				if (!b.echo && b.takeAll( 1 )) WaterFX.ring( b.cx, b.feet, WaterFX.S, color( b ), 0.6f );
			} else if (b.decal( b.cx, b.feet + 1f, FxFrames.ETCH, dark( b ), 0.6f, ETCH_HOLD, 0.8f ) != null){
				final Beat fizz = b.split( 1 );
				later( 0.5f, () -> bubble( fizz, fizz.cx + FxRandom.Float( -2f, 2f ), fizz.feet, FxBudget.P2 ) );
			}
		}
		int k = b.burst( b.n( t == WEAK ? 1 : t + 1 ), FxBudget.P2 );
		for (int i = 0; i < k; i++) bubble( b, b.cx + FxRandom.Float( -4f, 4f ), b.cy + FxRandom.Float( -3f, 3f ), FxBudget.P2 );
		b.react( b.look.light, 0.2f, Beat.SINK, 1 );
	}

	/** The body dissolving: bubbles boiling off it, a puff of fumes, an etch. */
	public static void finisher( Beat b ){
		FxParticle f = b.mat( b.cx, b.cy, FxBudget.P1 );
		if (f != null){
			f.look( FxFrames.PUFF_S, 0 ).ramp( b.el.smoke != null && !b.echo ? b.el.smoke : b.look.pale, false ).smooth()
					.vel( 0, -6f ).life( 0.9f ).alpha( b.a( 0.55f ), 0.1f, 0.5f );
		}
		int n = Math.max( 1, Math.round( 5 * b.scale ) );
		for (int i = 0; i < n; i++) bubble( b, b.cx + FxRandom.Float( -5f, 5f ), b.cy + FxRandom.Float( -4f, 5f ), FxBudget.P1 );
		if (!b.onWater( b.cx, b.feet )) b.decal( b.cx, b.feet + 1f, FxFrames.ETCH, dark( b ), 0.6f, ETCH_HOLD, 0.8f );
	}

	/** Drips rising from the floor into (x, y) over `seconds`. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		Beat b = Beat.at( l, x, y, GATHER_CAP );
		for (int i = 0; i < 5; i++){
			float ox = (i - 2) * 3f, oy = 10f + FxRandom.Float( 0f, 3f );
			FxParticle p = b.mat( x + ox, y + oy, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.DROP_2 ).paint( color( b ) ).vel( -ox / seconds, -oy / seconds ).life( seconds )
					.alpha( 1f, seconds * (0.2f + 0.1f * i), 0.1f ).snap();
		}
	}

	/** Bubbles popping on the owner at (x, y). */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 3, Math.round( FINALE_CAP * scale ) ) );
		int n = Math.max( 2, Math.round( 5 * scale ) );
		for (int i = 0; i < n; i++) bubble( b, x + FxRandom.Float( -5f, 5f ), y + FxRandom.Float( -3f, 5f ), FxBudget.P1 );
	}
}

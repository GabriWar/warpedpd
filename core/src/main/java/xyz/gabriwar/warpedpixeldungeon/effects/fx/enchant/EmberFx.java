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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * EMBER, fire licking upward (Blazing, Brimstone; Explosive's fuse under the hex): tongues
 * climbing the body from its feet to its head over 0.3 s (one small at WEAK, a middling one at
 * MID, two from STRONG), embers rising off it (the rest of the tally), a scorch left on the floor
 * (small at MID, middling from STRONG; on water a hiss of steam and a ring instead), and the body
 * glowing FF8418 a moment and starting back a pixel. Its death accent: the body flares in a large
 * tongue, embers rise, a middling scorch. Gained: embers drawn into the item, which catches with two
 * licks; ended with a ring of embers and a lick on the owner.
 */
public final class EmberFx {

	private EmberFx(){}

	public static final int AFTERGLOW = 0xFF8418;
	/** How long the tongues take to climb from the feet to the head, s. */
	public static final float CLIMB = 0.3f;
	static final int[] EMBERS = { 3, 4, 6, 7 };

	static Ramp embers( Beat b ){
		return b.echo ? b.look.pale : (b.el.ember != null ? b.el.ember : FxFactories.EMBER);
	}

	public static void play( Beat b ){
		int t = b.tier;
		float rise = Math.max( 6f, b.feet - b.top );
		if (!b.join && !b.curse){
			int n = t == WEAK ? 1 : t == MID ? 1 : 2;
			for (int i = 0; i < n; i++){
				float x = b.cx + (n > 1 ? (i == 0 ? -2f : 2f) : 0f);
				FxParticle p = b.lit( x, b.feet - 2f, i == 0 ? FxBudget.P0 : FxBudget.P1 );
				if (p == null) break;
				p.look( t == WEAK ? FxFrames.TONGUE_S : FxFrames.TONGUE_M, 12f ).ramp( b.ramp(), true )
						.vel( FxRandom.Float( -2f, 2f ), -rise / CLIMB ).drag( 1f ).wind( 0.15f )
						.life( CLIMB + 0.05f ).alpha( b.a( 1f ), 0, 0.12f );
			}
		}
		//its mark: a scorch, or steam over water
		if (t >= MID){
			if (b.onWater( b.cx, b.feet )){
				FxParticle s = b.mat( b.cx, b.feet - 1f, FxBudget.P2 );
				if (s != null){
					s.look( FxFrames.STEAM_CURL, 6f ).paint( b.c( FxFactories.STEAM ) ).smooth().vel( FxRandom.Float( -2f, 2f ), -10f )
							.life( 0.8f ).alpha( b.a( 0.5f ), 0.1f, 0.5f );
				}
				if (!b.echo && b.takeAll( 1 )) WaterFX.ring( b.cx, b.feet, WaterFX.S );
			} else {
				b.decal( b.cx, b.feet + 1f, t >= STRONG ? FxFrames.SCORCH_M : FxFrames.SCORCH_S, FxRecipes.SCORCH_COLOR, 0.7f,
						FxRecipes.SCORCH_HOLD, FxRecipes.SCORCH_FADE );
			}
		}
		//embers: the rest of what it may make
		int k = b.burst( b.n( EMBERS[t] ), FxBudget.P2 );
		for (int i = 0; i < k; i++) ember( b, b.hx + FxRandom.Float( -4f, 4f ), b.hy + FxRandom.Float( -3f, 3f ), 1f );
		b.react( AFTERGLOW, 0.2f, Beat.NUDGE, 1 );
	}

	//one ember rising off (x, y)
	static FxParticle ember( Beat b, float x, float y, float speed ){
		FxParticle p = b.lit( x, y, FxBudget.P2 );
		if (p == null) return null;
		return p.look( FxRandom.chance( 0.6f ) ? FxFrames.EMBER_1 : FxFrames.EMBER_2 ).ramp( embers( b ), true )
				.vel( FxRandom.Float( -12f, 12f ) * speed, -FxRandom.Float( 18f, 34f ) * speed ).gravity( -8f ).drag( 1.2f )
				.sway( 8f, FxRandom.Float( 6f, 10f ) ).wind( 0.4f ).life( FxRandom.Float( 0.5f, 0.9f ) ).alpha( b.a( 1f ), 0, 0.3f );
	}

	/** The body flaring: a large tongue on it, embers rising, a middling scorch. */
	public static void finisher( Beat b ){
		FxParticle p = b.lit( b.cx, b.cy + 2f, FxBudget.P0 );
		if (p != null){
			p.look( FxFrames.TONGUE_L, 10f ).ramp( b.ramp(), true ).vel( 0, -14f ).wind( 0.15f ).life( 0.45f )
					.alpha( b.a( 1f ), 0, 0.2f );
		}
		int n = Math.round( 5 * b.scale );
		for (int i = 0; i < n; i++) ember( b, b.cx + FxRandom.Float( -5f, 5f ), b.cy + FxRandom.Float( -4f, 4f ), 1.2f );
		if (!b.onWater( b.cx, b.feet )){
			b.decal( b.cx, b.feet + 1f, FxFrames.SCORCH_M, FxRecipes.SCORCH_COLOR, 0.7f, FxRecipes.SCORCH_HOLD, FxRecipes.SCORCH_FADE );
		}
	}

	/** Embers drawn from all round into (x, y) over `seconds`; the item catches with two licks. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		final Beat b = Beat.at( l, x, y, GATHER_CAP );
		for (int i = 0; i < 6; i++){
			float a = (float)(2 * Math.PI * i / 6) + FxRandom.Float( -0.3f, 0.3f ), d = FxRandom.Float( 9f, 12f );
			float ox = (float)Math.cos( a ) * d, oy = (float)Math.sin( a ) * d * 0.7f;
			FxParticle p = b.lit( x + ox, y + oy, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.EMBER_2 ).ramp( embers( b ), true ).vel( -ox / seconds, -oy / seconds ).life( seconds )
					.alpha( 1f, seconds * 0.3f, 0.1f );
		}
		final Beat licks = b.split( 2 );
		later( seconds, () -> {
			for (int i = 0; i < 2; i++){
				FxParticle p = licks.lit( x + (i == 0 ? -2f : 2f), y + 2f, FxBudget.P1 );
				if (p != null) p.look( FxFrames.LICK, 10f ).ramp( licks.ramp(), true ).vel( 0, -12f ).life( 0.3f ).alpha( 1f, 0, 0.15f );
			}
		} );
	}

	/** A ring of embers thrown out round the owner at (x, y), a lick climbing him. */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 4, Math.round( FINALE_CAP * scale ) ) );
		int n = Math.max( 3, Math.round( 6 * scale ) );
		for (int i = 0; i < n; i++){
			float a = (float)(2 * Math.PI * i / n);
			FxParticle p = b.lit( x, y + 2f, FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.EMBER_2 ).ramp( embers( b ), true ).polar( a, 26f ).drag( 3f ).gravity( -10f )
					.life( 0.6f ).alpha( 1f, 0, 0.3f );
		}
		FxParticle lick = b.lit( x, y + 4f, FxBudget.P1 );
		if (lick != null) lick.look( FxFrames.TONGUE_S, 12f ).ramp( b.ramp(), true ).vel( 0, -16f ).life( 0.35f ).alpha( 1f, 0, 0.15f );
	}
}

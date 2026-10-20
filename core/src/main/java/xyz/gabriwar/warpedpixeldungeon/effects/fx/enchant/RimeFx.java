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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * RIME, frost locking, then cracking (Chilling; AntiEntropy's cold under the hex): a white lock of
 * frost at the hit (a PLUS_5 for two frames), ice shards flung off and bouncing (the rest of the
 * tally), a mist of frost lifting (one, two from STRONG), rime spreading from the middle over the
 * floor (from MID, held 2 s; a white frost ring on water instead), the body shivering a pixel
 * either way and paling to BCE0FF. Its death accent: the body shatters, ice and glass flung out, a
 * larger rime. Gained: snowflakes spiralling in; ended with rime blooming at the owner's feet and
 * snow falling round him.
 */
public final class RimeFx {

	private RimeFx(){}

	public static final int AFTERGLOW = 0xBCE0FF;
	/** How long its rime lies, s (an area's row may make it longer). */
	public static final float RIME_HOLD = 2f, RIME_FADE = 1f;
	static final int[] SHARDS = { 2, 3, 5, 6 };

	static int frost( Beat b, int stop ){
		return b.c( Element.FROST.ramp.stop( stop ) );
	}

	public static void play( Beat b ){
		int t = b.tier;
		if (!b.join && !b.curse && !b.echo){
			FxParticle p = b.lit( b.hx, b.hy, FxBudget.P0 );
			if (p != null) p.look( FxFrames.PLUS_5 ).paint( 0xFFFFFF ).life( 0.08f ).alpha( 1f, 0, 0.03f ).snap();
		}
		int mist = t >= STRONG ? 2 : (t == MID ? 1 : 0);
		for (int i = 0; i < b.n( mist ); i++){
			FxParticle p = b.mat( b.cx + FxRandom.Float( -4f, 4f ), b.cy + FxRandom.Float( 0f, 4f ), FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.PUFF_S, 0 ).ramp( b.echo ? b.look.pale : Element.FROST.smoke, false ).smooth()
					.vel( FxRandom.Float( -3f, 3f ), -FxRandom.Float( 5f, 9f ) ).drag( 0.8f ).life( 0.9f ).alpha( b.a( 0.55f ), 0.1f, 0.5f );
		}
		if (t >= MID) rime( b, b.cx, b.feet + 1f, t >= STRONG ? 2 : 1 );
		int k = b.burst( b.n( SHARDS[t] ), FxBudget.P2 );
		for (int i = 0; i < k; i++) shard( b, b.hx, b.hy, i % 3 == 2 );
		b.react( AFTERGLOW, 0.2f, Beat.SHIVER, 1 );
	}

	//rime on the floor revealed from its middle, or a white ring on water
	static void rime( Beat b, float x, float y, int size ){
		if (b.onWater( x, y )){
			//a middling ring and its second
			if (!b.echo && b.takeAll( 2 )) WaterFX.ring( x, y, WaterFX.M, 0xFFFFFF, 0.6f );
			return;
		}
		FxDecal d = b.decal( x, y, FxFrames.RIME[Math.min( FxFrames.RIME.length - 1, size )], frost( b, 2 ), 0.65f,
				RIME_HOLD, RIME_FADE );
		if (d != null) d.revealCrop( FxDecal.FROM_MIDDLE, 0.3f );
	}

	//a shard of ice (or glass) flung off (x, y), bouncing once where it falls
	static FxParticle shard( Beat b, float x, float y, boolean glass ){
		FxParticle p = b.mat( x, y, FxBudget.P2 );
		if (p == null) return null;
		float a = (float)(-Math.PI / 2) + FxRandom.Float( -1.4f, 1.4f );
		return p.look( glass ? FxFrames.SHARD_GLASS : FxFrames.SHARD_ICE, 0 ).paint( frost( b, glass ? 1 : 2 ) )
				.polar( a, FxRandom.Float( 30f, 60f ) ).gravity( 160f ).spin( 0.1f )
				.ground( b.feet + FxRandom.Float( -1f, 2f ), FxParticle.BOUNCE, 0.3f ).life( FxRandom.Float( 0.45f, 0.65f ) )
				.alpha( b.a( 1f ), 0, 0.2f );
	}

	/** The body shattering: ice and glass flung out all round, a larger rime. */
	public static void finisher( Beat b ){
		int n = Math.max( 2, Math.round( 6 * b.scale ) );
		for (int i = 0; i < n; i++) shard( b, b.cx + FxRandom.Float( -3f, 3f ), b.cy + FxRandom.Float( -3f, 3f ), i >= n - 2 );
		rime( b, b.cx, b.feet + 1f, 2 );
	}

	/** Snowflakes spiralling into (x, y) over `seconds`. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		Beat b = Beat.at( l, x, y, GATHER_CAP );
		for (int i = 0; i < 6; i++){
			float a = (float)(2 * Math.PI * i / 6), d = 11f;
			float ox = (float)Math.cos( a ) * d, oy = (float)Math.sin( a ) * d * 0.7f;
			FxParticle p = b.lit( x + ox, y + oy, FxBudget.P1 );
			if (p == null) break;
			//inward, and round a quarter turn as it comes
			p.look( FxFrames.SNOWFLAKE_5, 4f ).paint( frost( b, 1 ) ).vel( -ox / seconds - oy * 1.2f, -oy / seconds + ox * 0.6f )
					.drag( 0.5f ).life( seconds ).alpha( 1f, seconds * 0.3f, 0.15f ).snap();
		}
	}

	/** Rime blooming at the owner's feet (x, y his middle) and snow falling round him. */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 4, Math.round( FINALE_CAP * scale ) ) );
		rime( b, x, b.feet + 1f, scale >= 0.5f ? 1 : 0 );
		int n = Math.max( 2, Math.round( 6 * scale ) );
		for (int i = 0; i < n; i++){
			FxParticle p = b.lit( x + FxRandom.Float( -8f, 8f ), y - FxRandom.Float( 4f, 10f ), FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.SNOWFLAKE_5, 3f ).paint( frost( b, 0 ) ).vel( FxRandom.Float( -2f, 2f ), FxRandom.Float( 6f, 10f ) )
					.life( 1.1f ).alpha( 0.9f, 0.1f, 0.4f ).snap();
		}
	}
}

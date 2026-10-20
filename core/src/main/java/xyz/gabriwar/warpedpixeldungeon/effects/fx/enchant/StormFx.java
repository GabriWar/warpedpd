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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * STORM, lightning forking (Shocking and Neptune's shock beside their own lightning, Paralysis's
 * stun, Surging, Potential; Polarized under the hex): a fork of static at the hit, three legs (two
 * at WEAK) of 6 px thrown out from it for two frames, two twigs running down the body to the
 * ground (from MID), sparks bouncing off (the rest of the tally), an etched mark of soot at the
 * feet (from MID, 1.5 s; on water a wash of its light and two rings instead), the body jittering
 * and lit a moment in its light. Its death accent: the body crackling with static for 0.3 s and a
 * puff of smoke. Gained: three twigs strike the item from above; ended with two twigs to the ground and
 * sparks.
 */
public final class StormFx {

	private StormFx(){}

	public static final int ETCH = 0x2A2A40;
	public static final float ETCH_HOLD = 1.5f;
	static final int[] SPARKS = { 2, 3, 5, 6 };

	//a leg of static: a 1x3 line drawn twice as long, pointing its way from (x, y)
	static FxParticle leg( Beat b, float x, float y, float angle, float len, int pri ){
		FxParticle p = b.lit( x + (float)Math.cos( angle ) * len / 2f, y + (float)Math.sin( angle ) * len / 2f, pri );
		if (p == null) return null;
		p.look( FxFrames.SPARK_3 ).paint( b.c( b.el.glint ) ).life( 0.07f ).alpha( b.a( 1f ), 0, 0.03f ).snap();
		p.angle = (float)Math.toDegrees( angle ) - 90f;
		p.scale.y = Math.max( 1f, Math.round( len / 3f ) );
		return p;
	}

	public static void play( Beat b ){
		int t = b.tier;
		if (!b.join && !b.curse){
			int legs = t == WEAK ? 2 : 3;
			float a0 = FxRandom.angle();
			for (int i = 0; i < legs; i++){
				if (leg( b, b.hx, b.hy, a0 + (float)(2 * Math.PI * i / legs) + FxRandom.Float( -0.3f, 0.3f ), 6f,
						i == 0 ? FxBudget.P0 : FxBudget.P1 ) == null) break;
			}
		}
		if (t >= MID){
			final Beat twigs = b.split( b.n( 2 ) );
			later( 0.06f, () -> {
				for (int i = 0; i < 2; i++){
					float x = twigs.cx + (i == 0 ? -3f : 3f);
					leg( twigs, x, twigs.cy + 1f, (float)(Math.PI / 2) + FxRandom.Float( -0.35f, 0.35f ), 6f, FxBudget.P1 );
				}
			} );
			if (b.onWater( b.cx, b.feet )){
				int cell = Fx.cellAt( b.cx, b.feet );
				if (!b.echo && cell >= 0 && b.takeAll( 2 )){
					WaterFX.wash( cell, b.look.light, 0.5f, 0.6f );
					WaterFX.ring( b.cx - 3f, b.feet, WaterFX.S );
				}
			} else {
				b.decal( b.cx, b.feet + 1f, FxFrames.ETCH, ETCH, 0.6f, ETCH_HOLD, 0.5f );
			}
		}
		int k = b.burst( b.n( SPARKS[t] ), FxBudget.P2 );
		for (int i = 0; i < k; i++) spark( b, b.hx, b.hy );
		b.react( b.look.light, 0.15f, Beat.SHIVER, 1 );
	}

	static FxParticle spark( Beat b, float x, float y ){
		FxParticle p = b.lit( x, y, FxBudget.P2 );
		if (p == null) return null;
		return p.look( FxFrames.SPARK_2 ).orient().slow( FxFrames.MOTE_1, 15f )
				.ramp( b.echo ? b.look.pale : (b.el.ember != null ? b.el.ember : Element.ELECTRIC.ember), true )
				.polar( FxRandom.Float( (float)-Math.PI, 0f ), FxRandom.Float( 30f, 70f ) ).gravity( 120f ).drag( 1.2f )
				.ground( b.feet + FxRandom.Float( -1f, 3f ), FxParticle.BOUNCE, 0.35f ).life( FxRandom.Float( 0.25f, 0.45f ) )
				.alpha( b.a( 1f ), 0, 0.1f );
	}

	/** The body crackling for 0.3 s (a leg of static thrown anew on it every 0.1 s), then a puff of
	 *  smoke off it. */
	public static void finisher( Beat b ){
		final Beat crackle = b.split( Math.max( 1, Math.round( 3 * b.scale ) ) );
		for (int i = 0; i < 3; i++){
			later( 0.1f * i, () -> leg( crackle, crackle.cx + FxRandom.Float( -4f, 4f ), crackle.cy + FxRandom.Float( -4f, 4f ),
					FxRandom.angle(), FxRandom.Float( 4f, 6f ), FxBudget.P1 ) );
		}
		final Beat puff = b.split( 1 );
		later( 0.3f, () -> {
			FxParticle p = puff.mat( puff.cx, puff.cy - 2f, FxBudget.P2 );
			if (p != null){
				p.look( FxFrames.PUFF_S, 0 ).ramp( Element.SMOKE.smoke, false ).smooth().vel( 0, -8f ).life( 0.7f )
						.alpha( puff.a( 0.5f ), 0.05f, 0.4f );
			}
		} );
		for (int i = 0; i < Math.round( 2 * b.scale ); i++) spark( b, b.cx, b.cy );
	}

	/** Three twigs striking (x, y) from above, a third of `seconds` apart. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		final Beat b = Beat.at( l, x, y, GATHER_CAP );
		for (int i = 0; i < 3; i++){
			final float ox = (i - 1) * 3f;
			later( seconds * (0.2f + 0.3f * i), () -> {
				leg( b, x + ox, y - 9f, (float)(Math.PI / 2) + FxRandom.Float( -0.3f, 0.3f ), 9f, FxBudget.P1 );
				spark( b, x + ox, y );
			} );
		}
	}

	/** Two twigs from the owner (x, y) to the ground, sparks off them. */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 4, Math.round( FINALE_CAP * scale ) ) );
		for (int i = 0; i < 2; i++){
			leg( b, x + (i == 0 ? -3f : 3f), y + 1f, (float)(Math.PI / 2) + FxRandom.Float( -0.3f, 0.3f ), 6f, FxBudget.P1 );
		}
		int n = Math.max( 1, Math.round( 4 * scale ) );
		for (int i = 0; i < n; i++) spark( b, x, b.feet - 2f );
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * FORCE, force ringing out (Kinetic, Elastic, Ancient, Bounce, Repulsion, Stone; Annoying,
 * Pressurized and Bulk under the hex): a shock ring racing out over the ground at the feet, past the
 * beat's own ring (23 px at WEAK, 31, 41 from STRONG), dust kicked up and pebbles bouncing once (the
 * rest of the tally), a crack in the floor (from MID; on water a large ring and droplets instead),
 * the body thrown back two pixels. A glyph's rings out from the wearer. Its death accent: a thud, a
 * wide crack and a ring of dust. Gained: rings closing in on the item, then one going out; ended
 * with a ring on the ground and dust round the owner.
 */
public final class ForceFx {

	private ForceFx(){}

	public static final int CRACK = 0x3A2C1C;
	/** The rings closing in on a gained item, widest first. */
	public static final int[][] CLOSING = { FxFrames.RING_31, FxFrames.RING_23, FxFrames.RING_17, FxFrames.RING_13, FxFrames.RING_9 };

	static FxParticle dust( Beat b, float x, float y ){
		FxParticle p = b.mat( x, y, FxBudget.P2 );
		if (p == null) return null;
		return p.look( FxFrames.PUFF_S, 0 ).ramp( b.echo ? b.look.pale : Element.EARTH.smoke, false ).smooth()
				.vel( FxRandom.Float( -10f, 10f ), -FxRandom.Float( 3f, 7f ) ).drag( 2f ).life( 0.6f ).alpha( b.a( 0.5f ), 0.03f, 0.4f );
	}

	static FxParticle pebble( Beat b, float x, float y ){
		FxParticle p = b.mat( x, y, FxBudget.P2 );
		if (p == null) return null;
		return p.look( FxFrames.PEBBLE_2 ).paint( b.c( Element.EARTH.ember.stop( 1 + FxRandom.Int( 3 ) ) ) )
				.polar( (float)(-Math.PI / 2) + FxRandom.Float( -1.2f, 1.2f ), FxRandom.Float( 25f, 45f ) ).gravity( 180f )
				.ground( b.feet + FxRandom.Float( 0f, 2f ), FxParticle.BOUNCE, 0.35f ).spin( 0.12f ).life( 0.6f )
				.alpha( b.a( 1f ), 0, 0.2f );
	}

	public static void play( Beat b ){
		int t = b.tier;
		if (!b.join && !b.curse){
			b.ring( b.cx, b.feet, t >= STRONG ? 13 : 9, t == WEAK ? 23 : t == MID ? 31 : 41, 0.3f, b.look.light, 0.85f,
					b.look.emissive );
		}
		if (t >= MID){
			if (b.onWater( b.cx, b.feet )){
				//a large ring and its second; and, with room, two droplets thrown (each ringing where it falls)
				if (!b.echo){
					if (b.takeAll( 8 )){
						WaterFX.ring( b.cx, b.feet, WaterFX.L );
						WaterFX.droplets( b.cx, b.feet, 2, 30f, 50f, 70f, WaterFX.liquid().foam );
					} else if (b.takeAll( 2 )){
						WaterFX.ring( b.cx, b.feet, WaterFX.L );
					}
				}
			} else {
				b.decal( b.cx, b.feet + 1f, t >= STRONG ? FxFrames.CRACK_17[FxRandom.Int( 2 )] : FxFrames.CRACK_11[FxRandom.Int( 2 )],
						CRACK, 0.6f, 2.5f, 1f );
			}
		}
		int k = b.burst( b.n( t + 2 ), FxBudget.P2 );
		for (int i = 0; i < k; i++){
			if (i % 2 == 0) dust( b, b.cx + FxRandom.Float( -6f, 6f ), b.feet - 1f );
			else pebble( b, b.cx + FxRandom.Float( -3f, 3f ), b.feet - 3f );
		}
		b.react( -1, 0f, Beat.NUDGE, 2 );
	}

	/** A thud: a wide crack, a ring of dust, a puff either side. */
	public static void finisher( Beat b ){
		if (!b.onWater( b.cx, b.feet )) b.decal( b.cx, b.feet + 1f, FxFrames.CRACK_17[0], CRACK, 0.7f, 3f, 1f );
		b.ring( b.cx, b.feet, 9, 31, 0.45f, Element.EARTH.ramp.stop( 1 ), 0.6f, false );
		int n = Math.max( 1, Math.round( 3 * b.scale ) );
		for (int i = 0; i < n; i++) dust( b, b.cx + (i - (n - 1) / 2f) * 6f, b.feet - 1f );
	}

	/** Rings closing in on (x, y) over `seconds`, then one going out. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		final Beat b = Beat.at( l, x, y, GATHER_CAP );
		b.flash( false, true, CLOSING, 0f, x, y + 4f, l.light, 0.7f, seconds * 0.8f );
		final Beat out = b.split( 1 );
		later( seconds * 0.85f, () -> out.ring( x, y + 4f, 9, 23, 0.35f, l.light, 0.7f, l.emissive ) );
	}

	/** A ring on the ground round the owner at (x, y), dust kicked up. */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 3, Math.round( FINALE_CAP * scale ) ) );
		b.ring( x, b.feet, 9, scale >= 0.5f ? 31 : 23, 0.4f, l.light, 0.7f, l.emissive );
		int n = Math.max( 2, Math.round( 4 * scale ) );
		for (int i = 0; i < n; i++) dust( b, x + FxRandom.Float( -7f, 7f ), b.feet - 1f );
	}
}

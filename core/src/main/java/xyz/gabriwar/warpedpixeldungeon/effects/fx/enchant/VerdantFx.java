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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.Nature;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * VERDANT, growth sprouting from below (Blooming, Parasitic, Adaptation, Camouflage,
 * Entanglement; Overgrowth under the hex): blades of grass shooting up round the feet in 0.15 s,
 * one after the other (two at WEAK, up to four), petals and leaves fluttering off (the rest of the
 * tally; floating on water), a ring of green rolling outward over 0.5 s (from MID), the body lit a
 * moment in green. Its death accent: a flower of six petals opening on the corpse's cell. Gained:
 * leaves spiralling in and a vine wrapping the item; ended with blades sprouting round the owner's
 * feet.
 */
public final class VerdantFx {

	private VerdantFx(){}

	static final int[] BLADES = { 2, 3, 4, 4 };
	static final int[][] BLADE = { FxFrames.BLADE_3, FxFrames.BLADE_4, FxFrames.BLADE_5 };

	//the leaves' colour: GENERAL's greens (Nature.generalLeaf), never the level's - an event's
	//leaves in a city's white or the halls' brown read as nothing of growth
	static int leaf( Beat b ){
		return b.c( Nature.generalLeaf() );
	}

	static int green( Beat b, int stop ){
		return b.c( Element.NATURE.ramp.stop( stop ) );
	}

	//a blade shooting up at (x, y), its feet, and standing a moment
	static FxParticle blade( Beat b, float x, float y, int size, float late, int pri ){
		FxParticle p = b.mat( x, y, pri );
		if (p == null) return null;
		return p.look( BLADE[Math.max( 0, Math.min( 2, size ) )] ).paint( green( b, 1 + FxRandom.Int( 3 ) ) )
				.vel( FxRandom.Float( -2f, 2f ), -36f ).drag( 7f ).life( 0.6f + late ).alpha( b.a( 1f ), late, 0.3f ).snap();
	}

	//a petal or a leaf fluttering off (x, y); on water it floats
	static FxParticle petal( Beat b, float x, float y, int pri ){
		FxParticle p = b.mat( x, y, pri );
		if (p == null) return null;
		boolean bloom = FxRandom.chance( 0.5f );
		if (bloom) p.look( FxFrames.PETAL_B ).paint( b.c( Element.NATURE.ember.step( FxRandom.Float() ) ) );
		else p.look( FxFrames.LEAF_D, 4f ).paint( leaf( b ) );
		if (b.onWater( x, b.feet )){
			return p.vel( FxRandom.Float( -6f, 6f ), -FxRandom.Float( 10f, 16f ) ).gravity( 60f ).sway( 4f, 3f )
					.ground( b.feet + 1f, FxParticle.SETTLE, 0 ).life( 1.6f ).alpha( b.a( 1f ), 0, 0.5f );
		}
		return p.vel( FxRandom.Float( -14f, 14f ), -FxRandom.Float( 16f, 26f ) ).gravity( 45f ).drag( 1.2f )
				.sway( 7f, FxRandom.Float( 4f, 6f ) ).ground( b.feet + FxRandom.Float( 0f, 2f ), FxParticle.SETTLE, 0 )
				.life( 1.2f ).alpha( b.a( 1f ), 0, 0.35f );
	}

	public static void play( Beat b ){
		int t = b.tier;
		if (!b.join && !b.curse){
			int n = b.n( BLADES[t] );
			for (int i = 0; i < n; i++){
				float x = b.cx + (i - (n - 1) / 2f) * 3f + FxRandom.Float( -1f, 1f );
				if (blade( b, x, b.feet, Math.min( 2, t + (i % 2) ), 0.04f * i, i == 0 ? FxBudget.P0 : FxBudget.P1 ) == null) break;
			}
		}
		if (t >= MID){
			b.ring( b.cx, b.feet, 9, t >= STRONG ? 41 : 31, 0.5f, Element.NATURE.light, 0.55f, false );
		}
		int k = b.burst( b.n( t + 1 ), FxBudget.P2 );
		for (int i = 0; i < k; i++) petal( b, b.cx + FxRandom.Float( -4f, 4f ), b.cy + FxRandom.Float( -2f, 4f ), FxBudget.P2 );
		b.react( Element.NATURE.light, 0.12f, Beat.STILL, 0 );
	}

	/** A flower of six petals opening where the body fell. */
	public static void finisher( Beat b ){
		int n = Math.max( 3, Math.round( 6 * b.scale ) );
		for (int i = 0; i < n; i++){
			float a = (float)(2 * Math.PI * i / n);
			FxParticle p = b.mat( b.cx, b.feet, i == 0 ? FxBudget.P0 : FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.PETAL_B ).paint( b.c( Element.NATURE.ember.stop( i % 2 == 0 ? 1 : 2 ) ) )
					.polar( a, 18f ).drag( 5f ).ground( b.feet + 2f, FxParticle.SETTLE, 0 ).life( 1.4f ).alpha( b.a( 1f ), 0, 0.5f ).snap();
		}
	}

	/** Leaves spiralling into (x, y) over `seconds`, a vine wrapping it at the end. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		final Beat b = Beat.at( l, x, y, GATHER_CAP );
		for (int i = 0; i < 5; i++){
			float a = (float)(2 * Math.PI * i / 5), d = 11f;
			float ox = (float)Math.cos( a ) * d, oy = (float)Math.sin( a ) * d * 0.7f;
			FxParticle p = b.mat( x + ox, y + oy, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.LEAF_D, 4f ).paint( leaf( b ) ).vel( -ox / seconds - oy, -oy / seconds + ox * 0.5f ).life( seconds )
					.alpha( 1f, seconds * 0.25f, 0.15f );
		}
		final Beat vine = b.split( 1 );
		later( seconds * 0.6f, () -> {
			Flash f = vine.flash( false, false, FxFrames.VINE, 6f, x, y, green( vine, 3 ), 0.9f, seconds * 0.4f + 0.2f );
			if (f != null) f.envelope( 0.08f, seconds * 0.4f );
		} );
	}

	/** Blades sprouting round the owner's feet (x, y his middle). */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 4, Math.round( FINALE_CAP * scale ) ) );
		int n = Math.max( 3, Math.round( 7 * scale ) );
		for (int i = 0; i < n; i++){
			float a = (float)(2 * Math.PI * i / n);
			blade( b, x + (float)Math.cos( a ) * 6f, b.feet + (float)Math.sin( a ) * 2f, i % 3, 0.03f * i, FxBudget.P1 );
		}
	}
}

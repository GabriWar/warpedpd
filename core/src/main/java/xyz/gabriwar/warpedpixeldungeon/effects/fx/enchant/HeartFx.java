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

import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * HEART, a heart floating up (Affection, Nomnom; Friendly under the hex): hearts rising off the
 * target (the Speck's own heart, through a kit emitter that draws nothing of the game's Random; an
 * echo's are pale motes instead), two to four by tier, and pink motes (the rest of the tally).
 * Nothing on the floor, no motion. Its death accent: jaws snapping shut on the body (two crescents
 * closing, Nomnom's) and hearts. Gained: hearts converging on the item; ended with hearts round the
 * owner.
 */
public final class HeartFx {

	private HeartFx(){}

	static final int[] HEARTS = { 2, 2, 3, 4 };

	//n hearts off (x, y); an echo's pale motes instead
	static int hearts( Beat b, float x, float y, int n ){
		if (b.echo){
			int made = 0;
			for (int i = 0; i < n; i++) if (mote( b, x, y ) != null) made++;
			return made;
		}
		int k = b.take( n );
		FxEmitter e = GameScene.fxEmitter();
		if (e == null || k <= 0) return 0;
		e.pos( x - 4f, y - 4f, 8f, 6f );
		e.burst( Speck.factory( Speck.HEART ), k );
		return k;
	}

	static FxParticle mote( Beat b, float x, float y ){
		FxParticle p = b.lit( x + FxRandom.Float( -4f, 4f ), y + FxRandom.Float( -3f, 3f ), FxBudget.P2 );
		if (p == null) return null;
		return p.look( FxFrames.MOTE_2 ).ramp( b.ramp(), true ).vel( FxRandom.Float( -5f, 5f ), -FxRandom.Float( 12f, 20f ) )
				.drag( 0.8f ).life( 0.7f ).alpha( b.a( 0.9f ), 0.05f, 0.3f );
	}

	public static void play( Beat b ){
		int t = b.tier;
		if (!b.join && !b.curse) hearts( b, b.cx, b.top + 3f, b.n( HEARTS[t] ) );
		int k = b.burst( b.n( 2 ), FxBudget.P2 );
		for (int i = 0; i < k; i++) mote( b, b.cx, b.cy );
	}

	/** Jaws snapping shut on the body, a heart off it. */
	public static void finisher( Beat b ){
		for (int i = 0; i < 2; i++){
			int k = i == 0 ? 6 : 2;
			FxParticle p = b.lit( b.cx, b.cy + (i == 0 ? -3f : 3f), FxBudget.P0 );
			if (p == null) break;
			p.look( new int[][]{ FxFrames.SLASH_16[(k + 1) % 8], FxFrames.SLASH_16[k] }, 0 ).ramp( b.ramp(), true )
					.vel( 0, i == 0 ? 20f : -20f ).life( 0.1f ).alpha( b.a( 1f ), 0, 0.05f ).snap();
		}
		hearts( b, b.cx, b.top + 3f, Math.max( 1, Math.round( 2 * b.scale ) ) );
		b.moveNow( b.ds, Beat.NUDGE, 1 );
	}

	/** Hearts converging on (x, y) over `seconds`. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		Beat b = Beat.at( l, x, y, GATHER_CAP );
		for (int i = 0; i < 5; i++){
			float a = (float)(2 * Math.PI * i / 5), d = 10f;
			float ox = (float)Math.cos( a ) * d, oy = (float)Math.sin( a ) * d * 0.6f;
			FxParticle p = b.lit( x + ox, y + oy, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.MOTE_2 ).ramp( b.ramp(), true ).vel( -ox / seconds, -oy / seconds ).life( seconds ).alpha( 1f, seconds * 0.3f, 0.1f );
		}
	}

	/** Hearts round the owner at (x, y). */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 2, Math.round( FINALE_CAP * scale ) ) );
		hearts( b, x, y - 2f, Math.max( 2, Math.round( 4 * scale ) ) );
	}
}

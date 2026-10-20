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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * RADIANCE, radiance falling from above (Holy, Purity, Satisfying; Dazzling under the hex): a shaft
 * of light dropping from 20 px above onto the body, bright two frames, gone in 0.25 s, and from MID
 * a star of light over its head; holy motes rising off it (the rest of the tally); a pool of holy
 * light under it for half a second; the body lit FFE9A8. Its death accent: the body burning away in
 * light, motes and a taller shaft. Gained: a shaft descending onto the item; ended with a shaft onto
 * the owner and motes.
 */
public final class RadianceFx {

	private RadianceFx(){}

	public static final int AFTERGLOW = 0xFFE9A8;
	/** How far above the body the shaft starts, px; how long it takes down. */
	public static final float DROP = 20f, DROP_FOR = 0.25f;
	static final int[][] SHAFT_24 = { FxFrames.Light.SHAFT_24 }, SHAFT_32 = { FxFrames.Light.SHAFT_32 };
	static final int[][] STAR = { FxFrames.Light.STAR4_15_A, FxFrames.Light.STAR4_15_B };

	//a shaft of light dropping from `drop` px above (x, y) onto it
	static Flash shaft( Beat b, int[][] frame, float x, float y, float drop, float alpha ){
		Flash f = b.flash( true, frame, 0f, x, y - drop - frame[0][3] / 2f + 4f, b.look.light, alpha, DROP_FOR );
		if (f != null) f.vel( 0, drop / DROP_FOR ).envelope( 0f, 0.067f );
		return f;
	}

	static FxParticle mote( Beat b, float x, float y, float speed ){
		FxParticle p = b.lit( x, y, FxBudget.P2 );
		if (p == null) return null;
		return p.look( FxRandom.chance( 0.6f ) ? FxFrames.EMBER_1 : FxFrames.MOTE_2 )
				.ramp( b.echo ? b.look.pale : (b.el.ember != null ? b.el.ember : b.el.ramp), true )
				.vel( FxRandom.Float( -4f, 4f ) * speed, -FxRandom.Float( 8f, 14f ) * speed ).sway( 5f, 4f ).drag( 0.4f )
				.life( FxRandom.Float( 0.6f, 0.9f ) ).alpha( b.a( 0.95f ), 0.05f, 0.4f );
	}

	public static void play( Beat b ){
		int t = b.tier;
		if (!b.join && !b.curse){
			shaft( b, SHAFT_24, b.cx, b.cy, DROP, b.echo ? 0.5f : 0.9f );
			if (t >= MID && !b.echo){
				Flash s = b.flash( true, STAR, 8f, b.cx, b.top - 2f, b.look.light, 0.7f, 0.3f );
				if (s != null) s.envelope( 0.03f, 0.1f );
			}
		}
		FxLight pool = b.pool( b.cx, b.feet + 2f, 31, b.look.light, 0.35f );
		if (pool != null) pool.life( 0.05f, 0.15f, 0.35f );
		int k = b.burst( b.n( t + 2 ), FxBudget.P2 );
		for (int i = 0; i < k; i++) mote( b, b.cx + FxRandom.Float( -5f, 5f ), b.cy + FxRandom.Float( -2f, 6f ), 1f );
		b.react( AFTERGLOW, 0.2f, Beat.STILL, 0 );
	}

	/** The body burning away in light: a taller shaft and motes rising off it. */
	public static void finisher( Beat b ){
		shaft( b, SHAFT_32, b.cx, b.cy, DROP * 0.6f, 0.9f );
		int n = Math.max( 2, Math.round( 5 * b.scale ) );
		for (int i = 0; i < n; i++) mote( b, b.cx + FxRandom.Float( -5f, 5f ), b.cy + FxRandom.Float( -4f, 6f ), 1.5f );
	}

	/** A shaft descending onto (x, y) over half of `seconds`, motes gathering under it. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		final Beat b = Beat.at( l, x, y, GATHER_CAP );
		later( seconds * 0.5f, () -> shaft( b, SHAFT_24, x, y, DROP, 0.8f ) );
		for (int i = 0; i < 4; i++){
			float ox = (i - 1.5f) * 4f;
			FxParticle p = b.lit( x + ox, y + 6f, FxBudget.P2 );
			if (p != null) p.look( FxFrames.MOTE_1 ).ramp( b.ramp(), true ).vel( -ox / seconds, -6f / seconds ).life( seconds )
					.alpha( 0.9f, seconds * 0.3f, 0.1f );
		}
	}

	/** A shaft onto the owner at (x, y), motes rising off him. */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 3, Math.round( FINALE_CAP * scale ) ) );
		shaft( b, SHAFT_24, x, y, DROP, 0.85f );
		int n = Math.max( 2, Math.round( 5 * scale ) );
		for (int i = 0; i < n; i++) mote( b, x + FxRandom.Float( -5f, 5f ), y + FxRandom.Float( -2f, 6f ), 1f );
	}
}

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

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * REAPER, death falling (Grim): a scythe's stroke coming down through the body (the large crescent
 * in DEATH's ramp, sweeping down in 0.12 s) and, from MID, a flash of DEATH's glint at its heart;
 * ash rising off the body (the rest of the tally), the body drawn toward ink for 0.3 s. Nothing on
 * the floor. Its death accent: a skull rising 6 px off the body and fading, a soul curling 16 px
 * up, a little ash; the camera shakes harder for it (EnchantFx). Gained: a sliver of a scythe's
 * edge sweeping the item; ended with the scythe's glint over the owner.
 */
public final class ReaperFx {

	private ReaperFx(){}

	/** The stroke: the 24 px crescent turning down. */
	public static final int[][] STROKE = { FxFrames.SLASH_24[1], FxFrames.SLASH_24[2] };
	public static final int INK = 0x22182A;

	static FxParticle ash( Beat b, float x, float y, int pri ){
		FxParticle p = b.mat( x, y, pri );
		if (p == null) return null;
		return p.look( FxRandom.chance( 0.5f ) ? FxFrames.MOTE_2 : FxFrames.MOTE_1 )
				.ramp( b.echo ? b.look.pale : Element.DEATH.smoke, false ).vel( FxRandom.Float( -3f, 3f ), -FxRandom.Float( 8f, 14f ) )
				.sway( 5f, 4f ).life( FxRandom.Float( 0.8f, 1.2f ) ).alpha( b.a( 0.9f ), 0.1f, 0.4f );
	}

	public static void play( Beat b ){
		int t = b.tier;
		if (!b.join && !b.curse){
			FxParticle s = b.lit( b.cx, b.cy - 2f, FxBudget.P0 );
			if (s != null) s.look( STROKE, 0 ).ramp( b.ramp(), true ).life( 0.12f ).alpha( b.a( 0.95f ), 0, 0.06f ).snap();
			if (t >= MID && !b.echo){
				FxParticle c = b.lit( b.hx, b.hy, FxBudget.P1 );
				if (c != null) c.look( FxFrames.CORE_FLASH ).paint( b.el.glint ).life( 0.08f ).alpha( 0.8f, 0, 0.04f ).snap();
			}
		}
		int k = b.burst( b.n( t + 2 ), FxBudget.P2 );
		for (int i = 0; i < k; i++) ash( b, b.cx + FxRandom.Float( -5f, 5f ), b.cy + FxRandom.Float( -3f, 5f ), FxBudget.P2 );
		b.react( INK, 0.3f, Beat.STILL, 0 );
	}

	/** A skull rising off the body and fading, a soul curling up after it, ash. */
	public static void finisher( Beat b ){
		FxParticle s = b.mat( b.cx, b.cy - 2f, FxBudget.P0 );
		if (s != null){
			s.look( FxFrames.SKULL_5 ).paint( b.c( Element.DEATH.ramp.stop( 1 ) ) ).vel( 0, -10f ).drag( 0.5f ).life( 0.7f )
					.alpha( b.a( 1f ), 0.05f, 0.35f ).snap();
		}
		FxParticle soul = b.lit( b.cx, b.cy - 4f, FxBudget.P1 );
		if (soul != null){
			soul.look( FxFrames.STEAM_CURL, 5f ).paint( b.c( Element.DEATH.glint ) ).vel( FxRandom.Float( -2f, 2f ), -24f )
					.drag( 0.4f ).life( 0.75f ).alpha( b.a( 0.7f ), 0.1f, 0.4f );
		}
		int n = Math.round( 3 * b.scale );
		for (int i = 0; i < n; i++) ash( b, b.cx + FxRandom.Float( -4f, 4f ), b.cy, FxBudget.P2 );
	}

	/** A sliver of a scythe's edge sweeping (x, y), then another, over `seconds`. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		final Beat b = Beat.at( l, x, y, GATHER_CAP );
		for (int i = 0; i < 2; i++){
			final int from = i * 3;
			later( seconds * (0.25f + 0.4f * i), () -> b.flash( true, new int[][]{ FxFrames.Light.RIM_ARC_23[from],
					FxFrames.Light.RIM_ARC_23[(from + 1) % 8], FxFrames.Light.RIM_ARC_23[(from + 2) % 8] }, 0f, x, y,
					b.look.rim, 0.7f, 0.2f ) );
		}
	}

	/** The scythe's glint over the owner at (x, y): a stroke and a star. */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 3, Math.round( FINALE_CAP * scale ) ) );
		FxParticle s = b.lit( x, y - 4f, FxBudget.P1 );
		if (s != null) s.look( STROKE, 0 ).ramp( b.ramp(), true ).life( 0.15f ).alpha( 0.8f, 0, 0.07f ).snap();
		FxParticle g = b.lit( x + 4f, y - 9f, FxBudget.P1 );
		if (g != null) g.look( MotifFx.GLINT_RUN, 0 ).paint( b.el.glint ).life( 0.3f ).alpha( 1f, 0.05f, 0.12f ).snap();
		int n = Math.round( 3 * scale );
		for (int i = 0; i < n; i++) ash( b, x + FxRandom.Float( -4f, 4f ), y + 2f, FxBudget.P2 );
	}
}

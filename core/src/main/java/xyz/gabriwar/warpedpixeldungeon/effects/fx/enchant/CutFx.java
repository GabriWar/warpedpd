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

import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * CUT, a blade opening the body (Vorpal, Hunting, Vicious, Slashing, Buzz saw, Thorns): a wound
 * gashed along the swing (Wound) and a crescent across it, white to the creature's blood; drops of
 * its blood thrown along the blow (the rest of the tally), the first one or two (from MID) landing
 * as a splat; the body thrown back two pixels. Its death accent: a second crescent crossing the
 * first (an X), a large splat, drops. Gained: three cuts crossing the item, beads dripping; ended
 * with a pair of splats at the owner's feet.
 */
public final class CutFx {

	private CutFx(){}

	static final int[] DROPS = { 2, 3, 4, 5 };

	static int blood( Beat b ){
		return b.ds != null ? b.c( b.ds.blood() & 0xFFFFFF ) : b.c( b.look.glow );
	}

	//a crescent across the blow (its octant k), white to the blood
	static FxParticle crescent( Beat b, float x, float y, int k, boolean wide, int pri ){
		FxParticle p = b.lit( x, y, pri );
		if (p == null) return null;
		int[][] set = wide ? FxFrames.SLASH_24 : FxFrames.SLASH_16;
		return p.look( new int[][]{ set[(k + 7) % 8], set[k] }, 0 ).ramp( new Ramp( 0xFFFFFF, blood( b ) ), true )
				.life( 0.12f ).alpha( b.a( 0.95f ), 0, 0.06f ).snap();
	}

	//a drop of blood thrown along the blow; a splat where it lands if it may leave one
	static FxParticle drop( Beat b, float x, float y, boolean splat ){
		FxParticle p = b.mat( x, y, FxBudget.P2 );
		if (p == null) return null;
		float a = (float)Math.atan2( b.dy, b.dx ) + FxRandom.Float( -0.6f, 0.6f ) - 0.3f;
		return p.look( FxRandom.chance( 0.5f ) ? FxFrames.DROP_3 : FxFrames.DROP_2 ).paint( blood( b ) ).orient()
				.polar( a, FxRandom.Float( 30f, 60f ) ).gravity( 240f )
				.ground( b.feet + FxRandom.Float( 0f, 3f ), splat ? FxParticle.SPLAT : FxParticle.DIE, 0 )
				.mark( blood( b ) ).life( 0.8f ).alpha( b.a( 1f ), 0, 0.15f );
	}

	public static void play( Beat b ){
		int t = b.tier;
		int k = HitFx.octant( b.dx, b.dy );
		if (!b.join && !b.curse){
			if (!b.echo && b.ds != null && b.ds.ch != null && b.ds.parent != null && b.takeAll( 1 )){
				Wound.hit( b.ds.ch, (float)Math.toDegrees( Math.atan2( b.dy, b.dx ) ) );
			}
			crescent( b, b.cx, b.cy, k, t >= STRONG, FxBudget.P0 );
		}
		int splats = t >= MID && !b.onWater( b.cx, b.feet ) ? (t >= STRONG ? 2 : 1) : 0;
		int n = b.burst( b.n( DROPS[t] ), FxBudget.P2 );
		for (int i = 0; i < n; i++){
			boolean s = i < splats && b.takeDecal();
			drop( b, b.hx, b.hy, s );
		}
		b.react( -1, 0f, Beat.NUDGE, 2 );
	}

	/** A second crescent crossing the first, a splat, drops. */
	public static void finisher( Beat b ){
		int k = HitFx.octant( b.dx, b.dy );
		crescent( b, b.cx, b.cy, (k + 2) % 8, true, FxBudget.P0 );
		if (!b.onWater( b.cx, b.feet )) b.decal( b.cx, b.feet + 1f, FxFrames.SPLAT[2], blood( b ), 0.6f, 3f, 3f );
		int n = Math.max( 1, Math.round( 4 * b.scale ) );
		for (int i = 0; i < n; i++) drop( b, b.cx, b.cy, false );
	}

	/** Three cuts crossing (x, y) over `seconds`, beads dripping off it. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		final Beat b = Beat.at( l, x, y, GATHER_CAP );
		for (int i = 0; i < 3; i++){
			final int k = (i * 3 + 1) % 8;
			later( seconds * (0.2f + 0.25f * i), () -> {
				crescent( b, x, y, k, false, FxBudget.P1 );
				FxParticle p = b.mat( x + FxRandom.Float( -3f, 3f ), y + 3f, FxBudget.P2 );
				if (p != null) p.look( MotifFx.DRIP_RUN, 0 ).paint( blood( b ) ).gravity( 30f ).life( 0.6f ).alpha( 1f, 0.1f, 0.15f );
			} );
		}
	}

	/** A pair of splats at the owner's feet (x, y his middle), drops off him. */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 3, Math.round( FINALE_CAP * scale ) ) );
		if (!b.onWater( x, b.feet )){
			b.decal( x - 3f, b.feet + 1f, FxFrames.SPLAT[1], blood( b ), 0.5f, 2f, 2f );
		}
		int n = Math.max( 2, Math.round( 4 * scale ) );
		for (int i = 0; i < n; i++){
			FxParticle p = b.mat( x, y, FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.DROP_2 ).paint( blood( b ) ).orient().polar( (float)(-Math.PI / 2) + FxRandom.Float( -1.2f, 1.2f ), 30f )
					.gravity( 200f ).ground( b.feet + 1f, FxParticle.DIE, 0 ).life( 0.7f ).alpha( 1f, 0, 0.15f );
		}
	}
}

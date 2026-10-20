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

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * MIRAGE, a mirage leaving ghosts (Projecting, Afterimage, Flow, Mirror image, Mirroring, Phasing,
 * Swiftness; Displacing, Wayward, Displacement and Multiplicity under the hex): an afterimage of
 * the body left in the look's light, three pixels back toward the blow, and an upright ring (a flat
 * one turned a quarter: space folding, the sign only mirages draw); a ghost's streak along the blow
 * and motes off it (the rest of the tally). Nothing on the floor, no motion. Its death accent: two
 * afterimages of the body pulled apart sideways. Gained: three ghost rings converging on the item;
 * ended with an afterimage double of the owner.
 */
public final class MirageFx {

	private MirageFx(){}

	static final int[][] RING_9 = { FxFrames.RING_9 };

	/** An upright ring at (x, y): space folding. */
	public static Flash fold( Beat b, float x, float y, float alpha, float life ){
		Flash f = b.flash( false, true, RING_9, 0f, x, y, b.look.light, alpha, life );
		if (f != null) f.turn( 90f );
		return f;
	}

	public static void play( Beat b ){
		int t = b.tier;
		if (!b.join && !b.curse){
			if (!b.echo) b.after( b.ds, b.look.light, 0.35f, 0.35f, -b.dx * 3f, -b.dy * 3f );
			fold( b, b.hx, b.hy, 0.8f, 0.3f );
		}
		if (t >= MID){
			FxParticle s = b.lit( b.cx - b.dx * 6f, b.cy - b.dy * 6f, FxBudget.P1 );
			if (s != null){
				s.look( FxFrames.STREAK_TAPER ).paint( b.c( b.look.light ) ).vel( b.dx * 30f, b.dy * 30f )
						.life( 0.2f ).alpha( b.a( 0.45f ), 0, 0.12f );
				s.angle = (float)Math.toDegrees( Math.atan2( b.dy, b.dx ) );
			}
		}
		int k = b.burst( b.n( t + 1 ), FxBudget.P2 );
		for (int i = 0; i < k; i++){
			FxParticle p = b.lit( b.cx + FxRandom.Float( -4f, 4f ), b.cy + FxRandom.Float( -4f, 4f ), FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.MOTE_1 ).paint( b.c( b.look.light ) ).vel( -b.dx * 10f + FxRandom.Float( -4f, 4f ), FxRandom.Float( -6f, 2f ) )
					.drag( 1f ).life( 0.5f ).alpha( b.a( 0.8f ), 0.05f, 0.3f );
		}
	}

	/** Two afterimages of the body pulled apart sideways, then two more further out. */
	public static void finisher( Beat b ){
		if (b.ds == null) return;
		final Beat far = b.split( 2 );
		b.after( b.ds, b.look.light, 0.45f, 0.35f, -3f, 0 );
		b.after( b.ds, b.look.light, 0.45f, 0.35f, 3f, 0 );
		if (b.scale < 1f) return;
		later( 0.08f, () -> {
			far.after( far.ds, far.look.light, 0.3f, 0.3f, -6f, 0 );
			far.after( far.ds, far.look.light, 0.3f, 0.3f, 6f, 0 );
		} );
	}

	/** Three ghost rings converging on (x, y) from six pixels out over `seconds`. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		Beat b = Beat.at( l, x, y, GATHER_CAP );
		for (int i = 0; i < 3; i++){
			float ox = (i - 1) * 6f, oy = i == 1 ? -5f : 0f;
			Flash f = fold( b, x + ox, y + oy, 0.6f, seconds );
			if (f != null) f.vel( -ox / seconds, -oy / seconds ).envelope( seconds * 0.2f, seconds * 0.8f );
		}
	}

	/** An afterimage double of the owner standing at (x, y), else a fold over the point. */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		Beat b = Beat.at( l, x, y, Math.max( 2, Math.round( FINALE_CAP * scale ) ) );
		int cell = Fx.cellAt( x, y );
		Char c = cell >= 0 ? Actor.findChar( cell ) : null;
		CharSprite s = c != null ? c.sprite : null;
		if (s != null){
			b.after( s, l.light, 0.4f, 0.4f, -3f, 0 );
			b.after( s, l.light, 0.4f, 0.4f, 3f, 0 );
		} else {
			fold( b, x, y, 0.7f, 0.35f );
		}
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Osmose;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx.*;

/**
 * DRAIN, a drain drawing home (Vampiric, Ares' leech, Osmose, Siphoning; Sacrificial and
 * Metabolism under the hex): a bead of light on the wound and a puff of the defender's own blood
 * (a skeleton's bone-white, a slime's green), then the lifeline - beads of that blood, 3/5/7 by
 * tier, arcing from the wound up through the weapon side (raised 6 px) into the wielder's middle
 * in 0.35 s, each a little after the last, turning from the blood to LIFE's pink
 * as they go, and a mote of life where they land; the defender pales a moment. A glyph's (a ward)
 * runs from the attacker into the wearer. Its death accent: every bead at once and a soul curling
 * up. Gained: beads circling the item and tightening; ended with a heartbeat of light under the
 * owner (a pool, then a second, fainter).
 */
public final class DrainFx {

	private DrainFx(){}

	/** How long a bead takes, s, and how much later each next one comes; how far above the way it rises, px. */
	public static final float LINE_FOR = 0.35f, STAGGER = 0.04f, RISE = 6f;
	public static final int[] BEADS = { 3, 5, 7, 7 };
	public static final int LIFE = 0xFF90C8, PALE = 0xE8E0E8;
	/** Vampiric's beads more from STRONG (ShadowRows.VAMPIRIC), and how long after its line they start. */
	public static final int MORE = 3;
	public static final float MORE_AT = STAGGER * 7;

	/** When the last of n beads of a line lands, s after it starts; -1 for none. */
	public static float landing( int n ){
		return n <= 0 ? -1f : LINE_FOR + STAGGER * (n - 1);
	}

	/** The beads of a death accent at its scale: every bead at once. */
	static int finisherBeads( float scale ){
		return Math.max( 2, Math.round( 5 * scale ) );
	}

	/**
	 * When the last bead a drain proc's beat sends its wielder lands, s after the beat (BlowPresenter
	 * holds his heal's card till then; ProcPiece.home), -1 when it sends none: a ward's (it drains into
	 * the wearer), a minimal beat's (a star and two motes), one with no wielder and no twist carrying
	 * it. Its lifeline (BEADS by tier, as the beat's mode thins it) or, for an echo's kill, the death
	 * accent's beads; a kill's every-bead-at-once; Vampiric's MORE from STRONG; Osmose's mana beads into
	 * the hero. Counted as the beat plans them: when the kit thins a line it lands sooner, never later.
	 */
	public static float home( Beat b, Twist twist ){
		if (b.fam != Family.DRAIN || b.ward || b.minimal) return -1f;
		boolean wielded = b.as != null && b.as != b.ds;
		boolean full = !b.join;
		float at = -1f;
		if (wielded){
			//an echo's kill draws its death accent at half scale in place of the family
			if (!(b.echo && b.killed)) at = landing( b.n( BEADS[b.tier] ) );
			if (b.killed) at = Math.max( at, landing( finisherBeads( b.echo ? 0.5f : 1f ) ) );
			if (full && twist == ShadowRows.VAMPIRIC && b.tier >= STRONG) at = Math.max( at, MORE_AT + landing( MORE ) );
		}
		if (full && twist != null && b.look == EnchantLooks.of( Osmose.class )) at = Math.max( at, landing( BEADS[b.tier] ) );
		return at;
	}

	static int blood( Beat b, boolean fromAttacker ){
		if (b.echo) return EnchantLook.GHOST;
		if (fromAttacker && b.as != null) return b.as.blood() & 0xFFFFFF;
		if (!fromAttacker && b.ds != null) return b.ds.blood() & 0xFFFFFF;
		return MotifFx.matterColor( b.look );
	}

	/**
	 * A lifeline of n beads from (x0, y0) to (x1, y1), arching RISE px above the higher of them:
	 * each a parabola (straight across, falling back as it rises), beads LINE_FOR s on the way,
	 * each STAGGER s slower than the last, from `from` to `to` in colour. How many were made.
	 */
	public static int lifeline( Beat b, float x0, float y0, float x1, float y1, int n, int from, int to ){
		return lifeline( b, x0, y0, x1, y1, Float.NaN, Float.NaN, n, from, to );
	}

	/**
	 * The same through (vx, vy) on its way (the weapon side, raised RISE px: the blood drunk through
	 * the blade), when that lies well between the two ends across; else arching as above.
	 */
	public static int lifeline( Beat b, float x0, float y0, float x1, float y1, float vx, float vy, int n, int from, int to ){
		Ramp ramp = new Ramp( b.c( from ), b.c( to ) );
		float cy = Math.min( y0, y1 ) - RISE;
		//across, the beads go straight (x linear in time), so the way meets vx at u of it; the
		//parabola's control height is then the one that puts it at vy there
		float u = Float.isNaN( vx ) || Math.abs( x1 - x0 ) < 4f ? -1f : (vx - x0) / (x1 - x0);
		if (u >= 0.15f && u <= 0.85f){
			cy = (vy - (1 - u) * (1 - u) * y0 - u * u * y1) / (2 * u * (1 - u));
		}
		int made = 0;
		for (int i = 0; i < n; i++){
			FxParticle p = b.lit( x0, y0, i == 0 ? FxBudget.P1 : FxBudget.P2 );
			if (p == null) break;
			float t = LINE_FOR + STAGGER * i;
			p.look( FxRandom.chance( 0.6f ) ? FxFrames.MOTE_2 : FxFrames.MOTE_1 ).ramp( ramp, true )
					.vel( (x1 - x0) / t, 2f * (cy - y0) / t ).gravity( 2f * (y0 - 2f * cy + y1) / (t * t) )
					.life( t ).alpha( b.a( 1f ), 0.03f, 0.05f );
			made++;
		}
		return made;
	}

	public static void play( Beat b ){
		int t = b.tier;
		boolean reverse = b.ward;
		float x0, y0, x1, y1;
		if (reverse){
			x0 = b.sx; y0 = b.sy; x1 = b.cx; y1 = b.cy;
		} else {
			x0 = b.hx; y0 = b.hy; x1 = b.sx; y1 = b.sy;
		}
		int from = blood( b, reverse );
		if (!b.join && !b.curse){
			FxParticle g = b.lit( x0, y0, FxBudget.P0 );
			if (g != null) g.look( FxFrames.MOTE_2 ).paint( b.c( Ramp.lerp( from, 0xFFFFFF, 0.5f ) ) ).life( 0.2f ).alpha( b.a( 1f ), 0, 0.1f );
			FxParticle puff = b.mat( x0, y0, FxBudget.P2 );
			if (puff != null){
				puff.look( FxFrames.PUFF_S, 0 ).paint( from ).smooth().vel( b.dx * 6f, -4f ).drag( 1f ).life( 0.45f )
						.alpha( b.a( 0.45f ), 0.03f, 0.3f );
			}
		}
		boolean home = b.as != null && b.as != b.ds;
		if (home){
			final Beat land = b.split( 1 );
			int n = lifeline( b, x0, y0, x1, y1, b.wx, b.wy - RISE, b.n( BEADS[t] ), from, LIFE );
			if (n > 0 && !reverse) b.home( landing( n ) );
			if (n > 0){
				final float lx = x1, ly = y1;
				later( landing( n ), () -> {
					FxParticle m = land.lit( lx, ly, FxBudget.P2 );
					if (m != null) m.look( FxFrames.PLUS_3 ).paint( land.c( Element.LIFE.light ) ).vel( 0, -6f ).life( 0.35f )
							.alpha( land.a( 1f ), 0, 0.2f ).snap();
				} );
			}
		}
		b.react( PALE, 0.15f, Beat.STILL, 0 );
	}

	/** Every bead at once and a soul curling up off the body. */
	public static void finisher( Beat b ){
		int n = finisherBeads( b.scale );
		if (b.as != null && b.as != b.ds){
			b.home( landing( lifeline( b, b.cx, b.cy, b.sx, b.sy, b.wx, b.wy - RISE, n, blood( b, false ), LIFE ) ) );
		}
		FxParticle soul = b.lit( b.cx, b.cy - 3f, FxBudget.P1 );
		if (soul != null){
			soul.look( FxFrames.STEAM_CURL, 5f ).paint( b.c( Ramp.lerp( blood( b, false ), 0xFFFFFF, 0.6f ) ) ).vel( 0, -22f )
					.drag( 0.4f ).life( 0.7f ).alpha( b.a( 0.6f ), 0.1f, 0.4f );
		}
	}

	/** Beads circling (x, y) and tightening into it over `seconds`. */
	public static void gather( float x, float y, EnchantLook l, float seconds ){
		Beat b = Beat.at( l, x, y, GATHER_CAP );
		int c = MotifFx.matterColor( l );
		for (int i = 0; i < 6; i++){
			float a = (float)(2 * Math.PI * i / 6), d = 10f;
			float ox = (float)Math.cos( a ) * d, oy = (float)Math.sin( a ) * d * 0.6f;
			FxParticle p = b.lit( x + ox, y + oy, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.MOTE_2 ).ramp( new Ramp( c, LIFE ), true ).vel( -ox / seconds - oy * 1.4f, -oy / seconds + ox * 0.8f )
					.life( seconds ).alpha( 1f, seconds * 0.2f, 0.1f );
		}
	}

	/** A heartbeat of light under the owner at (x, y): a pool, and a fainter one 0.22 s on. */
	public static void finale( float x, float y, EnchantLook l, float scale ){
		final Beat b = Beat.at( l, x, y, Math.max( 2, Math.round( FINALE_CAP * scale ) ) );
		final int c = Ramp.lerp( MotifFx.matterColor( l ), LIFE, 0.5f );
		FxLight lub = b.pool( x, b.feet, 23, c, 0.4f );
		if (lub != null) lub.life( 0f, 0.06f, 0.2f );
		later( 0.22f, () -> {
			FxLight dub = b.pool( x, b.feet, 23, c, 0.3f );
			if (dub != null) dub.life( 0f, 0.05f, 0.25f );
		} );
		int n = Math.max( 1, Math.round( 4 * scale ) );
		for (int i = 0; i < n; i++){
			FxParticle p = b.lit( x + FxRandom.Float( -5f, 5f ), b.feet - 1f, FxBudget.P2 );
			if (p == null) break;
			p.look( FxFrames.MOTE_1 ).paint( c ).vel( 0, -FxRandom.Float( 10f, 16f ) ).life( 0.6f ).alpha( 1f, 0.05f, 0.3f );
		}
	}
}

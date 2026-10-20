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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.watabou.noosa.Game;

/**
 * The thirteen item motifs (Motif) drawn in the world, as kit particles: one mote of a look's
 * motif lying on a heap (HEAP, P3, thinned by its distance from the hero), shed by a flying item
 * (FLIGHT, P2), at a wielder's weapon side (WIELD, P3), thrown in a burst (BURST, P2) or along a
 * swing's stroke (TRAIL, P2). Light (embers, sparks, motes, glints, runes) is added over the
 * night's tint; matter (ink, drops, bubbles, blades, pebbles, coins) is drawn under it. A curse's
 * motes come from 3-4 px out and are drawn inward; an echo's are pale. Render thread, FxRandom
 * only; null (or 0) off the scene or when the budget keeps none.
 *
 * The UI's motes in slots and windows are ench-item-looks' own (parametric, never kit particles).
 */
public final class MotifFx {

	private MotifFx(){}

	/** Where a mote is made. */
	public static final int HEAP = 0, FLIGHT = 1, WIELD = 2, BURST = 3, TRAIL = 4;

	/** One mote of the look's motif at (x, y), moving (vx, vy) besides its own way. */
	public static FxParticle mote( EnchantLook l, int where, float x, float y, float vx, float vy ){
		if (l == null) return null;
		int pri = where == HEAP || where == WIELD ? FxBudget.P3 : FxBudget.P2;
		boolean light = light( l );
		FxEmitter e = light ? Fx.glowAir() : Fx.air();
		if (e == null || !FxBudget.keep( pri, x, y )) return null;
		FxParticle p = ((FxParticle) e.recycle( FxParticle.class )).reset( x, y ).light( light ).priority( pri );
		return shape( p, l, where, false, vx, vy );
	}

	/** One mote of a beat's look, counted in its tally (an echo's pale). */
	public static FxParticle mote( EnchantFx.Beat b, int where, float x, float y, float vx, float vy ){
		FxParticle p = b.particle( light( b.look ), x, y, where == HEAP || where == WIELD ? FxBudget.P3 : FxBudget.P2 );
		return p == null ? null : shape( p, b.look, where, b.echo, vx, vy );
	}

	/** A mote lying on a heap at (x, y) (the item's middle). */
	public static FxParticle heap( EnchantLook l, float x, float y ){
		return mote( l, HEAP, x, y, 0, 0 );
	}

	/** A mote shed by an item flying at (vx, vy): left behind, drifting a little after it. */
	public static FxParticle flight( EnchantLook l, float x, float y, float vx, float vy ){
		return mote( l, FLIGHT, x, y, vx * 0.08f, vy * 0.08f );
	}

	/** A mote at a wielder's weapon side (x, y). */
	public static FxParticle wield( EnchantLook l, float x, float y ){
		return mote( l, WIELD, x, y, 0, 0 );
	}

	/** n motes thrown out from (x, y) all round; how many were made. */
	public static int burst( EnchantLook l, float x, float y, int n ){
		if (l == null) return 0;
		int k = FxBudget.allow( n, FxBudget.P2 ), made = 0;
		float a0 = FxRandom.angle();
		for (int i = 0; i < k; i++){
			float a = a0 + (float)(2 * Math.PI * i / Math.max( 1, k )) + FxRandom.Float( -0.3f, 0.3f );
			float v = FxRandom.Float( 14f, 24f );
			if (mote( l, BURST, x, y, (float)Math.cos( a ) * v, (float)Math.sin( a ) * v * 0.6f ) != null) made++;
		}
		return made;
	}

	/** The same in a beat's tally. */
	public static int burst( EnchantFx.Beat b, float x, float y, int n ){
		int k = b.burst( n, FxBudget.P2 ), made = 0;
		float a0 = FxRandom.angle();
		for (int i = 0; i < k; i++){
			float a = a0 + (float)(2 * Math.PI * i / Math.max( 1, k )) + FxRandom.Float( -0.3f, 0.3f );
			float v = FxRandom.Float( 14f, 24f );
			if (mote( b, BURST, x, y, (float)Math.cos( a ) * v, (float)Math.sin( a ) * v * 0.6f ) != null) made++;
		}
		return made;
	}

	// ------------------------------------------------------------------ the shapes

	//the runs of frames a mote steps through over its life
	static final int[][] DRIP_RUN = { FxFrames.MOTE_1, FxFrames.DROP_2, FxFrames.DROP_3, FxFrames.POP_5 };
	static final int[][] SIP_RUN = { FxFrames.DROP_2, FxFrames.MOTE_1 };
	static final int[][] BUBBLE_RUN = { FxFrames.BUBBLE_2, FxFrames.BUBBLE_2, FxFrames.BUBBLE_3, FxFrames.POP_5 };
	static final int[][] GLINT_RUN = { FxFrames.STAR4_5, FxFrames.PLUS_3 };
	static final int[][] THROB_RUN = { FxFrames.RING_5, FxFrames.RING_9 };

	/** Whether a look's motes are light (added) or matter. */
	public static boolean light( EnchantLook l ){
		switch (l.motif){
			case SEEP: case DRIP: case SIP: case BUBBLE: case SPROUT:
				return false;
			case CRACKLE: case GLINT: case GHOST: case CYCLE:
				return true;
			case ORBIT:
				return !coins( l );
			case FALL:
				return l.fam != Family.FORCE && l.fam != Family.VERDANT;
			default:
				return l.emissive || l.fam == Family.RADIANCE || l.fam == Family.HEART;
		}
	}

	private static boolean coins( EnchantLook l ){
		return l.fam == Family.FORTUNE && l.el == Element.GOLD && l.rune == FxFrames.COIN_6[0];
	}

	/** A colour a matter mote of the look shows: its glow, or its element's middle where the glow
	 *  is too dark to read (a curse's, a dark family's). */
	public static int matterColor( EnchantLook l ){
		int g = l.glow;
		int r = (g >> 16) & 0xFF, gg = (g >> 8) & 0xFF, b = g & 0xFF;
		return Math.max( r, Math.max( gg, b ) ) < 0x50 ? l.el.ramp.stop( Math.min( 2, l.el.ramp.size() - 1 ) ) : g;
	}

	//an ink wisp's colour: its element's smoke (ash, soot, ink) drawn a third toward its rim, so it
	//reads on a dark floor; the look's own where its element sheds none
	private static int seep( EnchantLook l, boolean echo ){
		int c = l.el.smoke != null ? Ramp.lerp( l.el.smoke.stop( 0 ), l.rim, 0.35f ) : matterColor( l );
		return echo ? EnchantLook.paled( c ) : c;
	}

	private static FxParticle shape( FxParticle p, EnchantLook l, int where, boolean echo, float vx, float vy ){
		Ramp ramp = echo ? l.pale : (l.el.ember != null && l.emissive ? l.el.ember : l.el.ramp);
		int mc = echo ? EnchantLook.paled( matterColor( l ) ) : matterColor( l );
		float k = where == TRAIL ? 0.6f : 1f;
		switch (l.motif){
			case RISE:
				p.look( l.fam == Family.EMBER ? (FxRandom.chance( 0.5f ) ? FxFrames.EMBER_2 : FxFrames.EMBER_1)
						: l.fam == Family.HEART ? FxFrames.MOTE_2 : FxFrames.MOTE_1 )
						.ramp( ramp, true ).vel( vx, vy - FxRandom.Float( 6f, 9f ) ).sway( 12f, 12.6f )
						.life( l.motif.life * k ).alpha( 0.9f, 0.1f, 0.4f );
				break;
			case FALL:
				if (l.fam == Family.RIME){
					p.look( FxFrames.SNOWFLAKE_5, 3f ).ramp( ramp, true );
				} else if (l.fam == Family.FORCE){
					p.look( FxFrames.PEBBLE_2 ).paint( mc );
				} else if (l.fam == Family.VERDANT){
					p.look( FxFrames.BARK, 3f ).paint( mc );
				} else {
					p.look( FxFrames.PLUS_3 ).ramp( ramp, true );
				}
				p.vel( vx + FxRandom.Float( -2f, 2f ), vy + FxRandom.Float( 3f, 5f ) ).life( l.motif.life * k ).alpha( 0.8f, 0.15f, 0.4f );
				break;
			case DRIP:
				p.look( DRIP_RUN, 0 ).paint( mc )
						.vel( vx, vy ).gravity( 25f ).life( l.motif.life * k ).alpha( 1f, 0.2f, 0.2f );
				break;
			case SIP:
				p.look( SIP_RUN, 0 ).paint( mc )
						.vel( vx, vy - 8f ).life( l.motif.life * k ).alpha( 1f, 0.3f, 0.25f );
				break;
			case BUBBLE:
				p.look( BUBBLE_RUN, 0 ).paint( mc )
						.vel( vx, vy - 4f ).life( l.motif.life * k ).alpha( 1f, 0.15f, 0.15f );
				break;
			case SEEP:
				p.look( FxFrames.INK_WISP, 3f ).paint( seep( l, echo ) ).smooth().vel( vx + FxRandom.Float( -1f, 1f ), vy - 3f )
						.turbulence( 1f, 4f ).life( l.motif.life * k ).alpha( 0.8f, 0.3f, 0.5f );
				break;
			case CRACKLE:
				p.look( FxFrames.SPARK_3 ).paint( echo ? EnchantLook.paled( l.el.glint ) : l.el.glint ).vel( vx, vy ).snap()
						.life( 0.08f ).alpha( 1f, 0, 0.03f );
				p.angle = FxRandom.Float( 360f );
				break;
			case ORBIT: {
				int[] f = l.fam == Family.WARD ? FxFrames.RUNE_3[FxRandom.Int( FxFrames.RUNE_3.length )]
						: l.fam == Family.CUT ? FxFrames.SPARK_2 : l.el == Element.PRISM ? FxFrames.STAR4_5 : FxFrames.MOTE_2;
				if (coins( l )){
					p.look( FxFrames.COIN_6, 8f ).paint( mc );
				} else {
					p.look( f ).ramp( ramp, true );
				}
				//round it a little: across its front, a pixel or two up
				float side = FxRandom.sign();
				p.vel( vx + side * 8f, vy - 2f ).drag( 1.5f ).life( 0.7f * k ).alpha( 0.9f, 0.15f, 0.3f ).snap();
				break;
			}
			case GLINT:
				p.look( GLINT_RUN, 0 ).ramp( ramp, true ).vel( vx + 10f, vy - 10f )
						.life( l.motif.life ).alpha( 1f, 0, 0.12f ).snap();
				break;
			case SPROUT:
				p.look( l.rune == FxFrames.SEED ? FxFrames.SEED : (l.fam == Family.VERDANT && FxRandom.chance( 0.2f )
						? FxFrames.PETAL_B : FxFrames.BLADE_3) ).paint( mc )
						.vel( vx, vy - 10f ).gravity( 30f ).ground( p.py + 1f, FxParticle.SETTLE, 0 ).life( l.motif.life * k )
						.alpha( 1f, 0.1f, 0.4f );
				break;
			case THROB:
				p.look( THROB_RUN, 0 ).ramp( ramp, true ).vel( vx, vy ).snap()
						.life( l.motif.life ).alpha( 0.7f, 0, 0.2f );
				break;
			case GHOST:
				p.look( FxFrames.STREAK_TAPER ).paint( echo ? EnchantLook.GHOST : l.rim ).vel( vx + 4f, vy ).snap()
						.life( 0.6f * k ).alpha( 0.3f, 0.2f, 0.3f );
				break;
			case CYCLE:
				p.look( FxFrames.MOTE_2 ).paint( echo ? EnchantLook.GHOST : Profile.cycleColor( l.cycle, Game.timeTotal, 1f ) )
						.vel( vx, vy - 5f ).life( 0.8f * k ).alpha( 0.9f, 0.1f, 0.3f );
				break;
		}
		if (l.curse && (where == HEAP || where == WIELD)){
			//drawn in: from 3-4 px out toward the point
			float a = FxRandom.angle(), d = FxRandom.Float( 3f, 4f );
			float ox = (float)Math.cos( a ) * d, oy = (float)Math.sin( a ) * d * 0.7f;
			p.at( p.px + ox, p.py + oy ).vel( -ox / 0.5f, -oy / 0.5f ).drag( 0f );
		}
		return p;
	}
}

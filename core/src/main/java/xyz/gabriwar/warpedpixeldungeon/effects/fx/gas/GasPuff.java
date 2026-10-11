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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.gas;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

/**
 * A body of gas: one painted cloud frame (GAS_S stepping to GAS_L a quarter through its life; smoke
 * rolls from PUFF_M or PUFF_L up a size or two, each step's variant drawn afresh, so a screen is
 * not rows of one shape, and larger the fuller the gases' share is) whose greys are lit from its gas's crown down to its underside (Shade), so a
 * cloud has a lit top and a deep belly in its own colours. It drifts down the cloud's volume,
 * curls a pixel or two, turns over (a mirror flip) now and then, and fades in and out smoothly.
 * A corrosive one turns toward rust late in its life; a confusion one goes round the wheel.
 *
 * All of the gases' bodies, wisps and puffs together are held to SHARE of the effects' budget (its
 * SOFT: 144 on a phone, 216 on a desktop); with the details they shed (fewer than half as many) a
 * cloud however big stays under the load where the budget starts thinning, leaving a
 * fight's sparks, fire and lightning their room. Past it a cloud makes nothing (and takes no
 * spawns) until some die.
 */
public class GasPuff extends FxParticle {

	/** The most gas bodies alive at once, all gases together, as a share of the budget's SOFT. */
	public static final float SHARE = 0.4f;
	/** A gas body steps from small to large this far through its life (a quarter: the cloud reads as
	 *  one body, not as specks), and fades in and out over these shares of it (smoke in faster: a
	 *  screen that hides what is behind it). */
	public static final float GROWN = 0.25f, FADE_IN = 0.15f, FADE_OUT = 0.3f, SMOKE_FADE_IN = 0.1f;
	/** A body's frames, one each, by variant: small, then large. */
	static final int[][][] GAS_S = single( FxFrames.GAS_S ), GAS_L = single( FxFrames.GAS_L ),
			PUFF_M = single( FxFrames.PUFF_M ), PUFF_L = single( FxFrames.PUFF_L ), PUFF_XL = single( FxFrames.PUFF_XL );

	private static int live;

	private boolean counted;
	private GasStyle style;
	//its turn over: every this many seconds a mirror flip
	private float flipMin, flipMax, flipIn;
	//the wheel's offset, s (anywhere round it: a cloud shows its colours at once); its crown and
	//underside as lit now
	private float wheelAt;
	private boolean shaded;
	private float hiValue, loValue;

	static int[][][] single( int[][] frames ){
		int[][][] out = new int[frames.length][][];
		for (int i = 0; i < frames.length; i++) out[i] = new int[][]{ frames[i] };
		return out;
	}

	/** A new scene: none of the last one's alive. */
	public static synchronized void reset(){
		live = 0;
	}

	/** How many are alive, all gases together. */
	public static synchronized int live(){
		return live;
	}

	/** The cap as it stands on this device (SHARE of the budget's SOFT, desktop's scale and all). */
	public static int cap(){
		return Math.round( SHARE * FxBudget.soft() );
	}

	/** Whether there is room for one more. */
	public static boolean room(){
		return live() < cap();
	}

	/** One from an emitter's pool, at (x, y). */
	public static GasPuff make( Emitter e, float x, float y ){
		GasPuff p = (GasPuff) e.recycle( GasPuff.class );
		p.reset( x, y );
		p.style = null;
		p.flipMin = 0;
		p.shaded = false;
		return p;
	}

	@Override
	public void revive(){
		super.revive();
		if (!counted){
			counted = true;
			synchronized (GasPuff.class){
				live++;
			}
		}
	}

	@Override
	public void kill(){
		super.kill();
		if (counted){
			counted = false;
			synchronized (GasPuff.class){
				live--;
			}
		}
	}

	/**
	 * A body of the gas in a cell holding `cur`, drifting (dx, dy) px/s down the cloud's volume:
	 * its frames, life, alpha, motion and colour.
	 */
	public GasPuff body( GasStyle s, int cur, float dx, float dy ){
		style = s;
		int v = FxRandom.Int( 2 );
		float life;
		float alpha = GasStyle.thickness( s.peak + FxRandom.Float( s.peakHi - s.peak ), cur );
		if (s.smoke){
			//the fuller the gases' share, the larger the puffs, so a big screen still closes up
			boolean small = FxRandom.chance( 0.5f * (1f - Math.min( 1f, live() / (float)cap() )) );
			int[][][] grown = small && FxRandom.chance( 0.5f ) ? PUFF_L : PUFF_XL;
			look( (small ? PUFF_M : PUFF_L)[v], 0 ).then( GROWN, grown[FxRandom.Int( 2 )] );
			life = FxRandom.Float( GasStyle.SMOKE_LIFE_MIN, GasStyle.SMOKE_LIFE_MAX );
			turbulence( 1.5f, (float)(Math.PI * 2 * 0.5) );
			flips( 0.6f, 1f );
			//soft: its crown on the frame's body grey, its underside on the rim, so the top band
			//lifts only a little over the rest (a screen of smoke, not lit slabs)
			grey( Shade.BODY, Shade.RIM );
		} else {
			look( GAS_S[v], 0 ).then( GROWN, GAS_L[v] );
			life = FxRandom.Float( GasStyle.LIFE_MIN, GasStyle.LIFE_MAX );
			turbulence( FxRandom.Float( 1f, 2f ), (float)(Math.PI * 2 * 0.4) );
			flips( 1.2f, 2f );
			grey( Shade.BODY, Shade.RIM );
		}
		float vx = dx + FxRandom.Float( -1f, 1f ), vy = dy + FxRandom.Float( -0.5f, 0.5f );
		if (s.hug) vy = 1f;
		if (s.rise > 0) vy = -s.rise * FxRandom.Float( 0.8f, 1.2f );
		life( life ).alpha( alpha, life * (s.smoke ? SMOKE_FADE_IN : FADE_IN), life * FADE_OUT ).smooth().light( s.light )
				.vel( vx, vy ).wind( 0.15f ).priority( FxBudget.P1 );
		if (s.rise > 0) drag( 0.07f );
		if (FxRandom.chance( 0.5f )) scale.x = -1;
		wheelAt = FxRandom.Float( GasStyle.WHEEL_SECONDS );
		recolor();
		return this;
	}

	/**
	 * A wisp curling off the cloud's edge: EDGE_WISP lit in the gas's colours at 0.7 of its body,
	 * outward at 4 px/s (along `ox`, `oy`), 1.2-1.8 s.
	 */
	public GasPuff wisp( GasStyle s, int cur, float ox, float oy ){
		style = s;
		float life = FxRandom.Float( 1.2f, 1.8f );
		look( FxFrames.EDGE_WISP[FxRandom.Int( FxFrames.EDGE_WISP.length )] );
		grey( Shade.CROWN, Shade.RIM );
		flips( 0, 0 );
		life( life ).alpha( GasStyle.thickness( s.peak, cur ) * 0.7f, life * 0.25f, life * 0.5f ).smooth().light( s.light )
				.vel( ox * 4f, oy * 4f ).drag( 0.3f ).turbulence( 1f, 3f ).wind( 0.15f ).priority( FxBudget.P2 );
		if (ox < 0) scale.x = -1;
		wheelAt = FxRandom.Float( GasStyle.WHEEL_SECONDS );
		recolor();
		return this;
	}

	/** Thrown out at `speed` px/s along `angle` (radians), slowing hard: a cloud welling up, or
	 *  pushed aside by someone walking through it. */
	public GasPuff pushed( float angle, float speed, float slowing ){
		polar( angle, speed );
		drag( slowing );
		return this;
	}

	//a mirror flip every min-max s (0: none)
	private void flips( float min, float max ){
		flipMin = min;
		flipMax = max;
		flipIn = min > 0 ? FxRandom.Float( min, max ) : 0;
	}

	//which two greys of its frames take its crown and its underside
	private void grey( float hi, float lo ){
		hiValue = hi;
		loValue = lo;
		shaded = true;
	}

	//its colours now
	private void recolor(){
		GasStyle s = style;
		if (s == null || !shaded) return;
		int hi = s.crown, lo = s.under;
		if (s.wheel){
			hi = GasStyle.wheel( Game.timeTotal + wheelAt );
			lo = Shade.darker( hi, 0.45f );
		} else if (s.turn != -1){
			float p = progress();
			if (p > GasStyle.TURN_AT){
				float k = (p - GasStyle.TURN_AT) / (1f - GasStyle.TURN_AT);
				hi = Ramp.lerp( hi, s.turn, k );
				lo = Ramp.lerp( lo, Shade.darker( s.turn, 0.5f ), k );
			}
		}
		Shade.map( this, hi, hiValue, lo, loValue );
	}

	@Override
	public void update(){
		super.update();
		if (!alive) return;
		if (flipMin > 0 && (flipIn -= Game.elapsed) <= 0){
			flipIn = FxRandom.Float( flipMin, flipMax );
			scale.x = -scale.x;
		}
		if (style != null && (style.wheel || style.turn != -1)) recolor();
	}
}

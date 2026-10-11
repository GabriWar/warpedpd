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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import com.watabou.noosa.particles.Emitter;

/**
 * The faint glow of a luminous gas over the night's tint: a GLOW_15 of the light sheet in the
 * gas's colour, breathing in over 0.5 s and out over 0.6, 1.6 s in all, at its level times the
 * night's share (Fx.nightMul, a persistent light's 0.35 by day), only where the hero sees it.
 * Dressing (P3), at most CAP at once (all gases), carried in the light layer
 * (Fx.lightCarrier) so it adds over the tint.
 */
public class GasGlow extends FxParticle {

	/** The most at once, all gases together. */
	public static final int CAP = 24;
	public static final float LIFE = 1.6f;

	private static int live;
	private boolean counted;

	public GasGlow(){
		super();
		texture( Assets.Effects.FX_LIGHT );
	}

	/** A new scene: none of the last one's alive. */
	public static synchronized void reset(){
		live = 0;
	}

	public static synchronized int live(){
		return live;
	}

	/** A glow at (x, y) in `color` at `level` (0..1); none past the cap or the budget, or off the
	 *  game's scene. Render thread. */
	public static GasGlow at( float x, float y, int color, float level ){
		if (live() >= CAP || !FxBudget.keep( FxBudget.P3, x, y )) return null;
		Emitter carrier = Fx.lightCarrier( GasGlow.class );
		if (carrier == null) return null;
		GasGlow g = (GasGlow) carrier.recycle( GasGlow.class );
		g.reset( x, y );
		g.look( FxFrames.Light.GLOW_15 ).paint( color ).light( true ).life( LIFE )
				.alpha( level * Fx.nightMul( Fx.DAY_PERSISTENT ), 0.5f, 0.6f ).source( x, y ).snap().priority( FxBudget.P3 );
		return g;
	}

	@Override
	public void revive(){
		super.revive();
		if (!counted){
			counted = true;
			synchronized (GasGlow.class){
				live++;
			}
		}
	}

	@Override
	public void kill(){
		super.kill();
		if (counted){
			counted = false;
			synchronized (GasGlow.class){
				live--;
			}
		}
	}
}

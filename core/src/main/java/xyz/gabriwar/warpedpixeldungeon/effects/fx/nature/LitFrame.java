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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.nature;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import com.watabou.noosa.Game;

/**
 * A kit particle drawn from the light sheet (FxFrames.Light: shafts, pools, glows, stars,
 * bubbles) instead of the particle sheet: everything FxParticle does, as light, with a wait
 * before its life starts in which it is hidden and costs no draw (delay), and a flicker of its
 * alpha (breath: +-8% at 0.3-0.6 Hz; shimmer: +-6% at 18-24 Hz).
 */
public class LitFrame extends FxParticle {

	//seconds before its life starts: hidden, unmoving
	protected float wait;
	//its alpha's flicker: share either way, Hz, phase
	private float flickAmp, flickHz, flickPhase, flickT;

	public LitFrame(){
		super();
		texture( Assets.Effects.FX_LIGHT );
	}

	@Override
	public LitFrame reset( float x, float y ){
		super.reset( x, y );
		wait = 0;
		flickAmp = 0;
		light( true );
		return this;
	}

	/** Its alpha flickering by `amp` either way at `hz` (breath 0.08 at 0.3-0.6; shimmer 0.06 at
	 *  18-24), from a phase of its own. */
	public LitFrame flicker( float amp, float hz ){
		flickAmp = amp;
		flickHz = hz;
		flickPhase = FxRandom.angle();
		flickT = 0;
		return this;
	}

	/** Hidden for `seconds` before its life starts. */
	public LitFrame delay( float seconds ){
		wait = Math.max( 0f, seconds );
		if (wait > 0) visible = false;
		return this;
	}

	/** Whether it is still waiting for its life to start. */
	public boolean waiting(){
		return wait > 0;
	}

	@Override
	public void update(){
		if (wait > 0){
			wait -= Game.elapsed;
			visible = false;
			if (wait > 0) return;
			started();
		}
		super.update();
		if (flickAmp != 0 && alive){
			flickT += Game.elapsed;
			am *= 1f + flickAmp * (float)Math.sin( 2 * Math.PI * flickHz * flickT + flickPhase );
		}
	}

	/** Its life starts (after its delay; never called for one with none). */
	protected void started(){}
}

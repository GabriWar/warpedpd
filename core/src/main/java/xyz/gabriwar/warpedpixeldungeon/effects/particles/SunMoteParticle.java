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

package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.Nature;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

/**
 * A sunlit mote: a speck of pollen hanging in the light of a clear spring or summer day over the
 * surface's grass (the weather overlay's MOTES, and among the spring's petals). One warm white
 * pixel (the ad's FFF7C8, the kit's pollen or the sun's white), rising a little in the warm air and
 * swaying as it drifts slowly with the wind (WIND of its pull, Fx.windX), now and then turning to
 * the sun: a soft swell of light, GLINT s up and down to GLINT_GAIN more of its alpha, never a
 * flicker. It fades in and out over a second and more of its five to eight; on whole pixels;
 * hidden where the hero does not see; dressing to the budget (FxBudget.P3: thinned with the load
 * and away from the hero, none at MINIMAL).
 */
public class SunMoteParticle extends FxParticle {

	/** Its colours: the ad's warm white, the kit's pollen, the sun's white. */
	public static final int[] COLORS = { 0xFFF7C8, Nature.POLLEN, Nature.SUN };
	/** Its alpha between glints; a glint's length (s) and how much more of it the glint swells to;
	 *  its share of the wind. */
	public static final float PEAK = 0.5f, GLINT = 0.8f, GLINT_GAIN = 0.8f, WIND = 0.35f;

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P3, x, y )) return;
			((SunMoteParticle) emitter.recycle( SunMoteParticle.class )).drift( x, y );
		}
	};

	//the seconds to its next glint, and how far into the one it is in (-1: none)
	private float glintIn, glintAt = -1f;

	/** Afloat at (x, y): a fresh life. */
	public SunMoteParticle drift( float x, float y ){
		reset( x, y );
		look( FxFrames.MOTE_1 ).paint( FxRandom.element( COLORS ) ).light( true )
				.life( FxRandom.Float( 5f, 8f ) ).alpha( PEAK, 1.2f, 1.5f )
				.vel( FxRandom.Float( -1.5f, 1.5f ), FxRandom.Float( -2f, 0.5f ) )
				.sway( FxRandom.Float( 2f, 3.5f ), FxRandom.Float( 0.8f, 1.4f ) )
				.wind( WIND ).snap().priority( FxBudget.P3 );
		glintIn = FxRandom.Float( 0.5f, 3f );
		glintAt = -1f;
		return this;
	}

	@Override
	public void update(){
		super.update();
		if (!alive) return;
		float swell = 0f;
		if (glintAt >= 0f){
			glintAt += Game.elapsed;
			if (glintAt >= GLINT){
				glintAt = -1f;
				glintIn = FxRandom.Float( 2f, 5f );
			} else {
				swell = (float)Math.sin( Math.PI * glintAt / GLINT );
			}
		} else if ((glintIn -= Game.elapsed) <= 0f){
			glintAt = 0f;
		}
		am = Math.min( 1f, am * (1f + GLINT_GAIN * swell) );
		//hidden where the hero does not see it (the fog dims what he has seen)
		if (visible && !Fx.seen( px, py )) visible = false;
	}
}

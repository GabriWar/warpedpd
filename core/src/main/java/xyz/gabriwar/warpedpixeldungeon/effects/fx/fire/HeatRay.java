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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.fire;

import xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;

/**
 * The shimmer standing over great heat (a big fire, an inferno, the forge's coals, a baking
 * crater, a hot haze, a fire elemental): the weather sheet's 3x14 ray, added faint (0.15 by
 * default) and pale (FFE8C0), rising slowly and swaying, mirrored left for right every 0.3-0.5 s
 * so it wavers like air does - a painted swap, never a stretch. Light, P3. Made only on the render
 * thread (its sheet is drawn the first time it is asked for).
 */
public class HeatRay extends FxParticle {

	/** Its colour and alpha by default, and its life, s. */
	public static final int COLOR = 0xFFE8C0;
	public static final float ALPHA = 0.15f, LIFE = 1.2f;
	/** How often it mirrors, s. */
	public static final float FLIP_MIN = 0.3f, FLIP_MAX = 0.5f;

	private float flipIn;

	/** A ray where an emitter asks (a baking crater's): rise() as a factory. Light. */
	public static final Emitter.Factory FACTORY = new Emitter.Factory(){
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			rise( e, x, y );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};

	public HeatRay(){
		super();
		texture( WeatherSprites.get() );
	}

	/** A ray of shimmer rising from (x, y) (its foot) in a colour at an alpha. */
	public static HeatRay rise( Emitter e, float x, float y, int color, float alpha ){
		if (!FxBudget.keep( FxBudget.P3, x, y )) return null;
		HeatRay r = (HeatRay) e.recycle( HeatRay.class );
		r.reset( x, y - 7 );
		r.look( WeatherSprites.HEAT_RAY ).paint( color ).light( true ).life( LIFE * FxRandom.Float( 0.85f, 1.15f ) )
				.alpha( alpha, 0.3f, 0.5f ).vel( 0, -FxRandom.Float( 4f, 8f ) ).drag( 0.5f )
				.sway( FxRandom.Float( 1f, 2.5f ), FxRandom.Float( 3f, 6f ) ).snap().priority( FxBudget.P3 );
		r.flipIn = FxRandom.Float( FLIP_MIN, FLIP_MAX );
		return r;
	}

	public static HeatRay rise( Emitter e, float x, float y ){
		return rise( e, x, y, COLOR, ALPHA );
	}

	@Override
	public void update(){
		super.update();
		if (!alive) return;
		if ((flipIn -= Game.elapsed) <= 0){
			flipIn = FxRandom.Float( FLIP_MIN, FLIP_MAX );
			flipHorizontal = !flipHorizontal;
			updateFrame();
		}
	}
}

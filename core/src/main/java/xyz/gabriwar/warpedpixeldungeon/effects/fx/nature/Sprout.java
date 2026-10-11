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

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import com.watabou.noosa.particles.Emitter;

/**
 * A sprout: a blade of grass growing where it stands, three px, four, then five (BLADE_3, 4, 5,
 * each a third of its life), its foot kept on the ground, in the level's grass, fading over its
 * last painted steps. Matter, on the floor.
 */
public class Sprout extends FxParticle {

	/** Its life, s. */
	public static final float LIFE = 0.6f;
	private static final int[][] GROW = { FxFrames.BLADE_3, FxFrames.BLADE_4, FxFrames.BLADE_5 };

	private float foot;

	/** A sprout with its foot at (x, y), in `color`, for `life` s. */
	public Sprout sprout( float x, float y, int color, float life ){
		reset( x, y );
		foot = y;
		look( GROW, 0 ).paint( color ).life( life ).alpha( 1f, 0, life * 0.4f ).snap().priority( FxBudget.P2 );
		return this;
	}

	@Override
	public void update(){
		//its foot stays put as it grows: its middle half its height above it
		int h = GROW[Math.min( GROW.length - 1, (int)(progress() * GROW.length) )][3];
		py = foot - h / 2f;
		super.update();
	}

	/** n sprouts scattered over a cell whose top-left is (x, y), into an emitter on the floor. */
	public static void scatter( Emitter e, float x, float y, int n ){
		for (int i = 0; i < n; i++){
			if (!FxBudget.keep( FxBudget.P2, x + 8, y + 8 )) continue;
			((Sprout) e.recycle( Sprout.class )).sprout( x + FxRandom.IntRange( 2, 13 ), y + FxRandom.IntRange( 7, 15 ),
					Nature.blade(), LIFE * FxRandom.Float( 0.85f, 1.15f ) );
		}
	}
}

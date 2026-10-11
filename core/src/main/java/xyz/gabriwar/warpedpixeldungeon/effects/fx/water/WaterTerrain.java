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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.water;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * Water coming and going (docs/fx/water-life.md, "The terrain"), as the render side sees a cell
 * change (the kit's TerrainWatch, so a co-op guest sees it alike), where the hero sees it:
 *
 *  - Ground become water (a flood, a melted wall of ice is the cold's, not this): a middling ring
 *    at its middle and foam along its four sides for 0.5 s (FOAM_H, FOAM_V at 0.55, in the
 *    liquid's foam). On the lava, the ring and two sparks.
 *  - Water gone dry: three droplets thrown up and a puff of mist (PUFF_S in E4F0FA at 0.35, rising
 *    4 px/s for 0.8 s); the lava's drained cell three embers.
 *  - Water to ice and back is the cold's (the gases-cold-earth area): nothing here.
 */
public final class WaterTerrain {

	private WaterTerrain(){}

	private static final float SIZE = DungeonTilemap.SIZE;

	/** The new water's foam: its life and alpha. */
	public static final float FOAM_LIFE = 0.5f, FOAM_ALPHA = 0.55f;
	/** The mist of water gone dry. */
	public static final int MIST = 0xE4F0FA;
	public static final float MIST_ALPHA = 0.35f, MIST_LIFE = 0.8f, MIST_RISE = 4f;

	private static final int[][] FOAM_H = { FxFrames.FOAM_H[0], FxFrames.FOAM_H[1], FxFrames.FOAM_H[2], FxFrames.FOAM_H[1], FxFrames.FOAM_H[0] };
	private static final int[][] FOAM_V = { FxFrames.FOAM_V[0], FxFrames.FOAM_V[1], FxFrames.FOAM_V[2], FxFrames.FOAM_V[1], FxFrames.FOAM_V[0] };

	static boolean liquid( int terrain ){
		return terrain >= 0 && terrain < Terrain.flags.length && (Terrain.flags[terrain] & Terrain.LIQUID) != 0;
	}

	/** A cell's terrain changed from `was` to `now` (render thread, once a change). */
	public static void changed( int cell, int was, int now ){
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length() || level.heroFOV == null || !level.heroFOV[cell]) return;
		//ice is the cold's
		if (was == Terrain.FROZEN_WATER || now == Terrain.FROZEN_WATER) return;
		boolean wasWet = liquid( was ), isWet = liquid( now );
		if (wasWet == isWet) return;
		int w = level.width();
		float cx = (cell % w + 0.5f) * SIZE, cy = (cell / w + 0.5f) * SIZE;
		Element.Liquid l = WaterFX.liquid();
		if (isWet){
			WaterFX.ring( cx, cy, WaterFX.M );
			if (l.ringAdditive){
				FxRecipes.sparks( cx, cy - 1, Element.LAVA, 2 );
				return;
			}
			float x0 = (cell % w) * SIZE, y0 = (cell / w) * SIZE;
			foam( x0 + 8, y0 + 2, FOAM_H, l );
			foam( x0 + 8, y0 + SIZE - 2, FOAM_H, l );
			foam( x0 + 2, y0 + 8, FOAM_V, l );
			foam( x0 + SIZE - 2, y0 + 8, FOAM_V, l );
		} else {
			if (l.ringAdditive){
				FxRecipes.embers( cx, cy, Element.LAVA, 3 );
				return;
			}
			WaterFX.droplets( cx, cy, 3, 25f, 50f, 100f, l.foam );
			FxEmitter air = Fx.air();
			if (air == null || !FxBudget.keep( FxBudget.P2, cx, cy )) return;
			((FxParticle) air.recycle( FxParticle.class )).reset( cx, cy - 2 ).look( FxFrames.PUFF_S, 0 ).paint( MIST )
					.vel( FxRandom.Float( -1f, 1f ), -MIST_RISE ).life( MIST_LIFE ).alpha( MIST_ALPHA, 0.1f, 0.5f ).smooth()
					.snap().priority( FxBudget.P2 );
		}
	}

	private static void foam( float x, float y, int[][] frames, Element.Liquid l ){
		Marks.Mark m = Marks.make( Marks.FOAM, false, x, y );
		if (m != null) m.look( frames, 0 ).paint( l.foam ).smooth().life( FOAM_LIFE ).alpha( FOAM_ALPHA, 0.1f, 0.25f )
				.snap().priority( FxBudget.P2 );
	}
}

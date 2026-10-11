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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Steps;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.TerrainFeaturesTilemap;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;

/**
 * High grass trampled, as the render side sees it go (high grass to grass or furrowed grass: a
 * guest sees it through the map's changes): the high grass's picture parting, flattened from its
 * top (16, 12, 8 rows over 0.15 s) as it fades over the grass now there, and four to six blades
 * flung away from the step that broke it (20-40 px/s, falling at 120, turned along their flight
 * as line frames are, lying and fading over 0.4 s). HighGrass's own burst adds its two leaves. Render thread; FxRandom only.
 */
public final class Trample {

	private Trample(){}

	/** Blades a step, their speed px/s, gravity, how far below the step's middle they lie, their
	 *  fade, and the fan they are flung in either side of the step's way (rad). */
	public static final int BLADES_MIN = 4, BLADES_MAX = 6;
	public static final float SPEED_MIN = 20f, SPEED_MAX = 40f, GRAVITY = 120f, FADE = 0.4f, FAN = 1.1f;

	private static final int[][] BLADES = { FxFrames.BLADE_3, FxFrames.BLADE_4 };
	private static final int[] steps = new int[2 * 6];

	/** Whether a change of terrain is high grass trampled. */
	public static boolean trampled( int from, int to ){
		return from == Terrain.HIGH_GRASS && (to == Terrain.GRASS || to == Terrain.FURROWED_GRASS);
	}

	/** High grass trampled at `cell` (render thread). */
	public static void at( int cell ){
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length() || !level.heroFOV[cell]) return;
		int w = level.width();
		float x = (cell % w) * DungeonTilemap.SIZE, y = (cell / w) * DungeonTilemap.SIZE;

		//the old grass parting, on the floor under everyone
		Group floor = GameScene.layer( GameScene.Layer.FLOOR );
		Image src = floor != null ? TerrainFeaturesTilemap.tile( cell, Terrain.HIGH_GRASS ) : null;
		if (src != null) floor.add( new TileCrop( src, x, y, TileCrop.PART, 1f, false ) );

		//the way the step came, if one was seen into this cell
		float way = Float.NaN;
		int n = Steps.seenAfter( Math.max( 0, Steps.last() - 6 ), steps );
		for (int k = n - 1; k >= 0; k--){
			if (steps[2 * k + 1] != cell) continue;
			int from = steps[2 * k];
			way = (float)Math.atan2( (cell / w) - (from / w), (cell % w) - (from % w) );
			break;
		}

		Emitter air = Nature.air( null );
		if (air == null) return;
		float cx = x + 8, cy = y + 11;
		int blades = FxBudget.allow( FxRandom.IntRange( BLADES_MIN, BLADES_MAX ), FxBudget.P2 );
		for (int i = 0; i < blades; i++){
			//away from the foot: on along the step's way, else anywhere
			float a = Float.isNaN( way ) ? FxRandom.angle() : way + FxRandom.Float( -FAN, FAN );
			float s = FxRandom.Float( SPEED_MIN, SPEED_MAX );
			Nature.make( air, cx + FxRandom.Float( -3f, 3f ), cy + FxRandom.Float( -2f, 2f ) )
					.look( FxRandom.element( BLADES ) ).paint( Nature.blade() )
					.vel( (float)Math.cos( a ) * s, (float)Math.sin( a ) * s * 0.5f - FxRandom.Float( 18f, 30f ) )
					.gravity( GRAVITY ).orient()
					.ground( cy + FxRandom.Float( 1f, 5f ), FxParticle.SETTLE, 0 )
					.life( 1.2f ).alpha( 1f, 0, FADE ).priority( FxBudget.P2 );
		}
	}
}

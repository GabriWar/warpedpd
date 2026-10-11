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
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;

/**
 * A change of season sweeping out from the caster: the plants in view it took (the nearest MOST)
 * lit in turn by a pool of the season's colour, with two motes of it rising, as the sweep reaches
 * them at SPEED px/s - across a screen in about a third of a second, while the leaves their
 * uprooting threw are still in the air. Any thread (handed to the render thread).
 */
public final class SeasonSweep {

	private SeasonSweep(){}

	public static final float SPEED = 400f, LIFE = 0.9f, ALPHA = 0.3f;
	public static final int MOST = 24;

	/** The colour of a season's light: spring's blossom, summer's green, autumn's gold, winter's
	 *  frost. */
	public static int color( GameCalendar.Season s ){
		switch (s){
			case SPRING: return 0xFFC8DC;
			case SUMMER: return 0xC8F070;
			case AUTUMN: return 0xE0A050;
			default:     return 0xC8E0FF;
		}
	}

	/** The sweep from `caster` over `cells` (the plants it took), in `color`. */
	public static void sweep( final int caster, final int[] cells, final int color ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> sweep( caster, cells, color ) );
			return;
		}
		Level level = Dungeon.level;
		Emitter glow = Nature.glow( null );
		if (level == null || glow == null || caster < 0) return;
		int w = level.width();
		float ox = (caster % w + 0.5f) * DungeonTilemap.SIZE, oy = (caster / w + 0.5f) * DungeonTilemap.SIZE;
		//the ones in view, nearest first
		int[] seen = new int[cells.length];
		float[] far = new float[cells.length];
		int k = 0;
		for (int cell : cells){
			if (cell < 0 || cell >= level.length() || !level.heroFOV[cell]) continue;
			float d = (float)Math.hypot( (cell % w + 0.5f) * DungeonTilemap.SIZE - ox, (cell / w + 0.5f) * DungeonTilemap.SIZE - oy );
			int i = k++;
			for (; i > 0 && far[i - 1] > d; i--){
				seen[i] = seen[i - 1];
				far[i] = far[i - 1];
			}
			seen[i] = cell;
			far[i] = d;
		}
		for (int j = 0; j < k && j < MOST; j++){
			int cell = seen[j];
			float cx = (cell % w + 0.5f) * DungeonTilemap.SIZE, cy = (cell / w + 0.5f) * DungeonTilemap.SIZE;
			float wait = far[j] / SPEED;
			Nature.lit( glow, cx, cy + 4 ).delay( wait ).look( FxFrames.Light.POOL_15 ).paint( color )
					.life( LIFE ).alpha( ALPHA, 0.1f, 0.6f ).snap().priority( FxBudget.P2 );
			for (int i = 0; i < 2; i++){
				if (!FxBudget.keep( FxBudget.P3, cx, cy )) continue;
				Nature.lit( glow, cx + FxRandom.Float( -5f, 5f ), cy + FxRandom.Float( -2f, 3f ) ).delay( wait )
						.look( FxFrames.Light.GLOW_7 ).paint( color ).vel( 0, -FxRandom.Float( 8f, 12f ) )
						.life( 0.7f ).alpha( 0.45f, 0.1f, 0.3f ).snap().priority( FxBudget.P3 );
			}
		}
	}
}

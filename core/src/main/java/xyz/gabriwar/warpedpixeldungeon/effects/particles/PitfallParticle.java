/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.gas.CellOverlay;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;

/**
 * The floor giving way into a pit: crumbs of floor falling in toward the cell's middle, stepping
 * smaller and darker as they drop (3x2, 2x2, 1x1), and on the burst's first a dark mouth on the
 * floor (CellOverlay.PIT) with dust off its rim. FACTORY4 ("pitfall4") is the warning, the turn
 * before: small crumbs (2x2, 1x1) from three of every four emits, the mouth to 0.75, held through
 * the turn. FACTORY8 ("pitfall8") is the fall: big crumbs from one emit in two, the mouth to 1.
 * About 60 pictures each for a nine-cell trap (each was about 100). Matter; the effects' own
 * randomness.
 */
public class PitfallParticle extends FxParticle {

	/** The crumbs going dark as they fall. */
	public static final Ramp CRUMBS = new Ramp( 0xA8A498, 0x7C786E, 0x52504A, 0x2E2C2A );
	/** The pit's mouth (WASH_16, the whole cell, so a trap's nine make one hole): its alpha (the
	 *  warning's, the fall's), how long it deepens (the warning almost at once: it is read), holds
	 *  (the warning through the turn before the fall) and fades. */
	public static final float WARN_ALPHA = 0.75f, MOUTH_ALPHA = 1f, MOUTH_IN = 0.5f, WARN_IN = 0.2f, WARN_HOLD = 1.2f, MOUTH_HOLD = 0.6f, MOUTH_FADE = 0.8f;
	private static final int[][] STEP_2 = { FxFrames.PEBBLE_2 }, STEP_1 = { FxFrames.MOTE_1 };

	public static final Emitter.Factory FACTORY4 = new Crumbling( false );
	public static final Emitter.Factory FACTORY8 = new Crumbling( true );

	private static final class Crumbling extends Emitter.Factory {
		private final boolean fall;

		Crumbling( boolean fall ) {
			this.fall = fall;
		}

		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			int cell = Fx.cellAt( x, y );
			if (index == 0) {
				mouth( cell, fall ? MOUTH_ALPHA : WARN_ALPHA, fall ? MOUTH_IN : WARN_IN, fall ? MOUTH_HOLD : WARN_HOLD );
				EarthParticle.dust( emitter, x, cellTop( y ) + 2, 0.3f );
			}
			boolean skip = fall ? index % 2 == 1 : index % 4 == 3;
			if (skip || !FxBudget.keep( FxBudget.P2, x, y )) return;
			((PitfallParticle)emitter.recycle( PitfallParticle.class )).resetCrumb( x, y, cell, fall );
		}
	}

	private static float cellTop( float y ) {
		return (float)Math.floor( y / DungeonTilemap.SIZE ) * DungeonTilemap.SIZE;
	}

	//the pit's dark mouth opening on its cell
	private static void mouth( int cell, float alpha, float in, float hold ) {
		if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) return;
		int w = Dungeon.level.width();
		CellOverlay.keep( CellOverlay.PIT, cell, (cell % w + 0.5f) * DungeonTilemap.SIZE, (cell / w + 0.5f) * DungeonTilemap.SIZE,
				FxFrames.Light.WASH_16, true, 0x000000, alpha, in, hold, MOUTH_FADE, false );
	}

	public void resetCrumb( float x, float y, int cell, boolean big ) {
		reset( x, y );
		float life = FxRandom.Float( 0.6f, 1f );
		float cx = x, cy = y;
		if (cell >= 0 && Dungeon.level != null) {
			int w = Dungeon.level.width();
			cx = (cell % w + 0.5f) * DungeonTilemap.SIZE;
			cy = (cell / w + 0.5f) * DungeonTilemap.SIZE;
		}
		if (big) look( FxFrames.PEBBLE_3 ).then( 0.33f, STEP_2 ).then( 0.66f, STEP_1 );
		else look( FxFrames.PEBBLE_2 ).then( 0.5f, STEP_1 );
		ramp( CRUMBS, false ).life( life ).alpha( 1f, 0, 0.2f )
				.vel( (cx - x) * 0.8f / life, (cy - y) * 0.8f / life + 4f ).gravity( 10f ).snap().priority( FxBudget.P2 );
	}
}

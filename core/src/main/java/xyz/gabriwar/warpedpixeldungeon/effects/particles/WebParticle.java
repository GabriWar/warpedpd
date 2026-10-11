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
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.gas.CellOverlay;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;

/**
 * A web as it is drawn: one painted web strung across each cell of it (CellOverlay.WEB, on the
 * floor, its look and mirror by the cell's parity, at 0.55), held while the web is there and seen,
 * fading half a second after; it shakes when someone moves through it and catches a dew glint
 * every 6-10 s. One picture a cell where the web used to keep some 24 strands alive. The factory
 * keeps its name (NetVisuals' "web") and its object; it is the gameplay's own obstacle, so it is
 * never thinned (P0): a blob's emitter holds it on every cell it sees (BlobEmitter.Held) without
 * taking any of the frame's spawns.
 */
public final class WebParticle {

	private WebParticle(){}

	/** A web's alpha, how long it comes in, holds past its last tick and fades. */
	public static final float ALPHA = 0.55f, IN = 0.4f, HOLD = 1f, FADE = 0.5f;

	public static final Emitter.Factory FACTORY = new Strung();

	private static final class Strung extends Emitter.Factory implements FxBudget.Prioritized, BlobEmitter.Aftermath, BlobEmitter.Held {

		@Override
		public int priority(){
			return FxBudget.P0;
		}

		//a blob's web is kept through held(): its emitter makes nothing else, so takes no spawns
		@Override
		public float density( int cur ){
			return 0f;
		}

		@Override
		public void held( int cell, int cur ){
			strung( cell );
		}

		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			strung( Fx.cellAt( x, y ) );
		}

		@Override
		public void dying( int cell ){
			CellOverlay.release( CellOverlay.WEB, cell );
		}
	}

	/** A web kept on a cell (made the first time, held after). Render thread. */
	public static void strung( int cell ){
		if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) return;
		int w = Dungeon.level.width();
		int k = ((cell % w) + (cell / w)) & 1;
		float cx = (cell % w + 0.5f) * DungeonTilemap.SIZE, cy = (cell / w + 0.5f) * DungeonTilemap.SIZE;
		CellOverlay o = CellOverlay.keep( CellOverlay.WEB, cell, cx, cy, FxFrames.WEB[k], false, CellOverlay.SILK,
				ALPHA, IN, HOLD, FADE, ((cell / w) & 1) == 1 );
		if (o != null && o.isNew()) o.alt( FxFrames.WEB_B[k] );
	}
}

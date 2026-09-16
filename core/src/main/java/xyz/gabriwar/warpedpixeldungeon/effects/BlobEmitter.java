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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;

public class BlobEmitter extends Emitter {
	
	private Blob blob;
	
	public BlobEmitter( Blob blob ) {
		
		super();
		
		this.blob = blob;
		blob.use( this );
	}

	public RectF bound = new RectF(0, 0, 1, 1);

	//below this much blob left in a cell, its particles start to thin out
	private static final int FADE_VOLUME = 5;
	
	@Override
	protected void emit( int index ) {
		
		if (blob.volume <= 0) {
			return;
		}

		if (blob.area.isEmpty())
			blob.setupArea();
		
		int[] map = blob.cur;
		float size = DungeonTilemap.SIZE;

		int cell;
		for (int i = blob.area.left; i < blob.area.right; i++) {
			for (int j = blob.area.top; j < blob.area.bottom; j++) {
				cell = i + j*Dungeon.level.width();
				boolean visible = cell < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[cell];
				if (blob.alwaysVisible && cell < Dungeon.level.length()) {
					visible = visible || Dungeon.level.mapped[cell] || Dungeon.level.visited[cell];
				}
				if (visible && map[cell] > 0) {
					//a cell running out of blob emits less and less, so fire, frost
					//and the rest thin away instead of stopping between two frames
					if (map[cell] < FADE_VOLUME
							&& Random.Float() * FADE_VOLUME > map[cell]) {
						continue;
					}
					float x = (i + Random.Float(bound.left, bound.right)) * size;
					float y = (j + Random.Float(bound.top, bound.bottom)) * size;
					factory.emit(this, index, x, y);
				}
			}
		}
	}
}

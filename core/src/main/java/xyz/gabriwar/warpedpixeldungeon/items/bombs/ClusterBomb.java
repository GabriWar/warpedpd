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

package xyz.gabriwar.warpedpixeldungeon.items.bombs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

public class ClusterBomb extends Bomb {

	{
		image = ItemSpriteSheet.CLUSTER_BOMB;
	}

	@Override
	protected Fuse createFuse() {
		return new ClusterFuse();
	}

	@Override
	public int value() {
		return 20 * quantity;
	}

	public class ClusterFuse extends Fuse {

		@Override
		protected void trigger(xyz.gabriwar.warpedpixeldungeon.items.Heap heap) {
			heap.remove(ClusterBomb.this);

			// primary explosion
			ClusterBomb.this.explodeStandard(heap.pos);

			// secondary explosions across the surrounding 5x5 (minus the centre),
			// each at 33% chance. (The old `pos + n*2` scaling walked off the ring
			// and wrapped rows on a linear grid — only ~8 valid cells, avg ~2.7.)
			int w = Dungeon.level.width();
			int px = heap.pos % w;
			int py = heap.pos / w;
			for (int dy = -2; dy <= 2; dy++) {
				for (int dx = -2; dx <= 2; dx++) {
					if (dx == 0 && dy == 0) continue;
					int nx = px + dx;
					int ny = py + dy;
					if (nx < 0 || nx >= w || ny < 0 || ny >= Dungeon.level.height()) continue;
					if (Random.Int(3) == 0) {
						ClusterBomb.this.explodeStandard(nx + ny * w);
					}
				}
			}

			snuff();
		}
	}

	// Standard bomb explosion (delegates to parent)
	public void explodeStandard(int cell) {
		super.explode(cell);
	}
}

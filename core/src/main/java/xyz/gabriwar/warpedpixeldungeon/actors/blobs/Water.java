/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

public class Water extends Blob {

	@Override
	protected void evolve() {
		super.evolve();

		if (volume > 0) {

			boolean mapUpdated = false;

			int cell;
			for (int i = area.left; i < area.right; i++) {
				for (int j = area.top; j < area.bottom; j++) {
					cell = i + j * Dungeon.level.width();
					if (cur[cell] > 0) {
						int c = Dungeon.level.map[cell];
						if (c == Terrain.EMPTY || c == Terrain.EMBERS
								|| c == Terrain.EMPTY_DECO) {

							Level.set(cell, cur[cell] > 9 ? Terrain.HIGH_GRASS
									: Terrain.GRASS);
							mapUpdated = true;
							if (Dungeon.level.heroFOV[cell]) {
								CellEmitter.get(cell).burst(LeafParticle.LEVEL_SPECIFIC, cur[cell] > 9 ? 6 : 3);
							}

						} else if (c == Terrain.GRASS && cur[cell] > 9) {

							Level.set(cell, Terrain.HIGH_GRASS);
							mapUpdated = true;
							if (Dungeon.level.heroFOV[cell]) {
								CellEmitter.get(cell).burst(LeafParticle.LEVEL_SPECIFIC, 4);
							}
						}
					}
				}
			}

			if (mapUpdated) {
				GameScene.updateMap();
				Dungeon.observe();
			}
		}
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);

		emitter.start(LeafParticle.LEVEL_SPECIFIC, 0.2f, 0);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}

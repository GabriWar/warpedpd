/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LightParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

public class Sunlight extends Blob {

	@Override
	protected void evolve() {
		super.evolve();

		if (volume > 0) {
			int cell;
			for (int i = area.left; i < area.right; i++) {
				for (int j = area.top; j < area.bottom; j++) {
					cell = i + j*Dungeon.level.width();
					if (off[cell] > 0) {
						if (Dungeon.level.plants.get(cell) == null) {
							//as in OvergrownPD: the couched plant is created but never
							//registered in level.plants, so no plant actually appears
							Plant.Seed seed = (Plant.Seed) Generator.random(Generator.Category.SEED);
							seed.couch(cell, null);
							GameScene.updateMap( cell );
						}
					}
				}
			}
			Dungeon.observe();
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );

		emitter.start( LightParticle.FACTORY, 0.2f, 20 );
	}
}

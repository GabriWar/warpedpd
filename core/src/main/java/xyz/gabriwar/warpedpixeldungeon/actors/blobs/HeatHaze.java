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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.Random;

public class HeatHaze extends Blob {

	@Override
	protected void evolve() {
		super.evolve();

		int cell;

		for (int i = area.left; i < area.right; i++) {
			for (int j = area.top; j < area.bottom; j++) {
				cell = i + j * Dungeon.level.width();
				if (cur[cell] > 0) {

					TileTemperature.depositHeat(cell, cur[cell] * 0.10f);

					// Chance to ignite grass tiles
					int terrain = Dungeon.level.map[cell];
					if ((terrain == Terrain.GRASS || terrain == Terrain.HIGH_GRASS
							|| terrain == Terrain.FURROWED_GRASS)
							&& Random.Float() < 0.05f) {
						Level.set(cell, Terrain.EMBERS);
						GameScene.updateMap(cell);
						GameScene.add(Blob.seed(cell, 2, Fire.class));
					}

					// Characters in the haze take light burn damage
					Char ch = Actor.findChar(cell);
					if (ch != null && !ch.isImmune(getClass())
							&& !Char.hasProp(ch, Char.Property.FIERY)) {
						if (Random.Float() < 0.15f) {
							Buff.affect(ch, Burning.class).reignite(ch, 3f);
						}
					}
				}
			}
		}
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);
		emitter.pour(Speck.factory(Speck.INFERNO, true), 0.6f);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}

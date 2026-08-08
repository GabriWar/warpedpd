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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Shadows;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class FallenLeaves extends Blob {

	@Override
	protected void evolve() {

		int cell;

		Fire fire = (Fire) Dungeon.level.blobs.get(Fire.class);

		for (int i = area.left; i < area.right; i++) {
			for (int j = area.top; j < area.bottom; j++) {
				cell = i + j * Dungeon.level.width();
				if (cur[cell] > 0) {

					// Fire burns away leaves and spreads through them
					if (fire != null && fire.volume > 0 && fire.cur[cell] > 0) {
						// Leaves catch fire and spread to adjacent leaf-covered tiles
						for (int offset : PathFinder.NEIGHBOURS4) {
							int adj = cell + offset;
							if (adj >= 0 && adj < Dungeon.level.length()
									&& cur[adj] > 0
									&& (fire.cur == null || fire.cur[adj] <= 0)) {
								if (Random.Float() < 0.5f) {
									GameScene.add(Blob.seed(adj, 3, Fire.class));
								}
							}
						}
						off[cell] = cur[cell] = 0;
						continue;
					}

					// Slow decay
					off[cell] = cur[cell] - 1;
					volume += off[cell];

					// Hero gets stealth while standing on leaves
					Hero hero = Dungeon.hero;
					if (hero.isAlive() && hero.pos == cell) {
						Shadows s = Buff.affect(hero, Shadows.class);
						if (s != null) {
							s.prolong();
						}
					}

					// Mobs stepping on leaves alert the hero
					Char ch = Actor.findChar(cell);
					if (ch instanceof Mob && ch.fieldOfView != null) {
						if (Dungeon.level.heroFOV[cell]) {
							Dungeon.hero.interrupt();
						}
					}

				} else {
					off[cell] = 0;
				}
			}
		}
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);
		emitter.start(LeafParticle.GENERAL, 0.8f, 0);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}

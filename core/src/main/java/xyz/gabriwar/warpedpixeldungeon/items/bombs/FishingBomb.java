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

package xyz.gabriwar.warpedpixeldungeon.items.bombs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoPiranha;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;


public class FishingBomb extends Bomb {

	{
		image = ItemSpriteSheet.DUMPLING_BOMB;
	}

	@Override
	public boolean explodesDestructively() {
		return false;
	}

	@Override
	public void explode(int cell) {
		if (fuse != null) {
			fuse.snuff();
			this.fuse = null;
		}

		Sample.INSTANCE.play(Assets.Sounds.BLAST);

		int w = Dungeon.level.width();
		for (int dy = -2; dy <= 2; dy++) {
			for (int dx = -2; dx <= 2; dx++) {
				int c = cell + dx + dy * w;
				if (c >= 0 && c < Dungeon.level.length()) {
					if (Dungeon.level.heroFOV[c]) {
						CellEmitter.get(c).burst(SmokeParticle.FACTORY, 4);
					}

					Char ch = Actor.findChar(c);
					if (ch instanceof AlbinoPiranha) {
						//beach the piranha on DRY land (out of water it suffocates).
						//randomRespawnCell is water-only on the FishingLevel, so pick
						//a dry passable cell directly.
						int count = 30;
						int pos = -1;
						do {
							int c2 = com.watabou.utils.Random.Int(Dungeon.level.length());
							if (Dungeon.level.passable[c2]
									&& Dungeon.level.map[c2] != xyz.gabriwar.warpedpixeldungeon.levels.Terrain.WATER
									&& Actor.findChar(c2) == null) {
								pos = c2;
								break;
							}
						} while (count-- > 0);

						if (pos != -1) {
							ch.pos = pos;
							ch.sprite.place(ch.pos);
							ch.sprite.visible = Dungeon.level.heroFOV[pos];
							GLog.i(Messages.get(this, "teleported"));
						} else {
							GLog.w(Messages.get(this, "no_teleport"));
						}
					}
				}
			}
		}
	}

	@Override
	public int value() {
		return 20 * quantity;
	}
}

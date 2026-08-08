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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class DizzyBomb extends Bomb {

	{
		image = ItemSpriteSheet.DIZZY_BOMB;
	}

	@Override
	public void explode(int cell) {
		if (fuse != null) {
			fuse.snuff();
			this.fuse = null;
		}

		Sample.INSTANCE.play(Assets.Sounds.BLAST);

		if (Dungeon.level.heroFOV[cell]) {
			CellEmitter.center(cell).burst(BlastParticle.FACTORY, 30);
		}

		boolean terrainAffected = false;
		for (int n : PathFinder.NEIGHBOURS9) {
			int c = cell + n;
			if (c >= 0 && c < Dungeon.level.length()) {
				if (Dungeon.level.heroFOV[c]) {
					CellEmitter.get(c).burst(SmokeParticle.FACTORY, 4);
				}

				if (Dungeon.level.flamable[c]) {
					Dungeon.level.destroy(c);
					GameScene.updateMap(c);
					terrainAffected = true;
				}

				Heap heap = Dungeon.level.heaps.get(c);
				if (heap != null) heap.explode();

				Char ch = Actor.findChar(c);
				if (ch != null) {
					int minDamage = c == cell ? Dungeon.scalingDepth() + 2 : 1;
					int maxDamage = 10 + Dungeon.scalingDepth();
					int dmg = Random.NormalIntRange(minDamage, maxDamage) - ch.drRoll();
					if (dmg > 0) {
						ch.damage(dmg, this);
						if (!(ch instanceof NPC) && ch != Dungeon.hero) {
							Buff.affect(ch, Vertigo.class, Vertigo.DURATION);
						}
					}

					if (ch == Dungeon.hero && !ch.isAlive()) {
						GLog.n(Messages.get(Bomb.class, "ondeath"));
						Dungeon.fail(this);
					}
				}
			}
		}

		if (terrainAffected) {
			Dungeon.observe();
		}
	}

	@Override
	public int value() {
		return 20 * quantity;
	}
}

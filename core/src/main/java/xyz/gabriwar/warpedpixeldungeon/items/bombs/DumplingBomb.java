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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drowsy;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

public class DumplingBomb extends Bomb {

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

		for (int n : PathFinder.NEIGHBOURS9) {
			int c = cell + n;
			if (c >= 0 && c < Dungeon.level.length()) {
				if (Dungeon.level.heroFOV[c]) {
					CellEmitter.get(c).burst(SmokeParticle.FACTORY, 4);
				}

				Char ch = Actor.findChar(c);
				if (ch != null && ch != Dungeon.hero && !(ch instanceof NPC)
						&& ch instanceof Mob && !((Mob) ch).properties().contains(Char.Property.BOSS)
						&& !ch.properties().contains(Char.Property.UNDEAD)) {

					Buff.affect(ch, Drowsy.class);
					ch.sprite.centerEmitter().start(Speck.factory(Speck.NOTE), 0.3f, 5);

					//Sprouted only relocated mobs still at full HP (a wounded mob
					//stays put and just gets drowsy)
					if (ch.HP == ch.HT) {
						// teleport to random position
						int count = 10;
						int pos;
						do {
							pos = Dungeon.level.randomRespawnCell(null);
							if (count-- <= 0) break;
						} while (pos == -1);

						if (pos != -1) {
							ch.pos = pos;
							ch.sprite.place(ch.pos);
							ch.sprite.visible = Dungeon.level.heroFOV[pos];
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

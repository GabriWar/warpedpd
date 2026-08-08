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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

public class HolyHandGrenade extends Bomb {

	{
		image = ItemSpriteSheet.HOLY_HAND_GRENADE;
	}

	@Override
	public boolean explodesDestructively() {
		return false;
	}

	@Override
	protected int explosionRange() {
		return 2;
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
					if (ch != null && ch != Dungeon.hero
							&& ch.properties().contains(Char.Property.UNDEAD)) {
						Buff.affect(ch, Terror.class, Terror.DURATION).object = Dungeon.hero.id();
						int dmg = Random.NormalIntRange(75, 155);
						if (dmg > 0) {
							ch.damage(dmg, this);
						}
					}
				}
			}
		}

		Dungeon.observe();
	}

	@Override
	public int value() {
		return 20 * quantity;
	}
}

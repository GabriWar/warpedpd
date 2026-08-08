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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

public class MistCloud extends Blob {

	@Override
	protected void evolve() {
		super.evolve();

		int cell;

		Fire fire = (Fire) Dungeon.level.blobs.get(Fire.class);
		Inferno inf = (Inferno) Dungeon.level.blobs.get(Inferno.class);

		for (int i = area.left; i < area.right; i++) {
			for (int j = area.top; j < area.bottom; j++) {
				cell = i + j * Dungeon.level.width();
				if (cur[cell] > 0) {

					// Fire and inferno clear mist
					if (fire != null && fire.volume > 0 && fire.cur[cell] > 0) {
						off[cell] = cur[cell] = 0;
						continue;
					}
					if (inf != null && inf.volume > 0 && inf.cur[cell] > 0) {
						off[cell] = cur[cell] = 0;
						continue;
					}

					Char ch = Actor.findChar(cell);
					if (ch != null && !ch.isImmune(getClass())) {
						// Thick mist blinds
						if (cur[cell] > 3) {
							Buff.prolong(ch, Blindness.class, 2f);
						}
						// Mist dampens — applies Drenched
						Buff.prolong(ch, Drenched.class, 3f);
					}
				}
			}
		}
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);
		emitter.pour(Speck.factory(Speck.STEAM), 0.4f);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}

/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2024-2026 Gabriwar
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
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.journal.Notes;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

/**
 * A persistent marker blob seeded at the altar pedestal.
 * Its sole purpose is to register the ALTAR_SHRINE landmark in the journal
 * when the hero first sees the room, and emit a faint golden glow.
 */
public class AltarShrine extends Blob {

	@Override
	protected void evolve() {
		// Single-cell blob — just keep it alive indefinitely
		int cell;
		for (int i = area.left; i < area.right; i++) {
			for (int j = area.top; j < area.bottom; j++) {
				cell = i + j * Dungeon.level.width();
				if (cur[cell] > 0) {
					off[cell] = cur[cell];
					volume += off[cell];
				} else {
					off[cell] = 0;
				}
			}
		}
	}

	@Override
	public Notes.Landmark landmark() {
		return Notes.Landmark.ALTAR_SHRINE;
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);
		emitter.start(Speck.factory(Speck.LIGHT), 0.6f, 0);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}

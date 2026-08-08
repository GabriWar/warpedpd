/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2022-2025 Overgrown Team
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Coughing;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

public class Smoke extends Blob {

	@Override
	protected void evolve() {
		super.evolve();

		Char ch;
		int cell;

		Level l = Dungeon.level;
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*l.width();
				l.losBlocking[cell] = off[cell] > 0 || (Terrain.flags[l.map[cell]] & Terrain.LOS_BLOCKING) != 0;
				if (cur[cell] > 0 && (ch = Actor.findChar( cell )) != null) {
					if (!ch.isImmune(this.getClass())) {
						Buff.prolong(ch, Coughing.class, 2);
					}
				}
			}
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.pour( Speck.factory( Speck.SMOKE ), 0.1f );
	}

	@Override
	public void clear(int cell) {
		super.clear(cell);
		Level l = Dungeon.level;
		l.losBlocking[cell] = cur[cell] > 0 || (Terrain.flags[l.map[cell]] & Terrain.LOS_BLOCKING) != 0;
	}

	@Override
	public void fullyClear() {
		super.fullyClear();
		Dungeon.level.buildFlagMaps();
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

}

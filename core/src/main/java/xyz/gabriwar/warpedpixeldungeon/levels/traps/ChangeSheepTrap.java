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

package xyz.gabriwar.warpedpixeldungeon.levels.traps;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokoban;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanCorner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanStop;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanSwitch;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ElmoParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

public class ChangeSheepTrap extends Trap {

	{
		color = WHITE;
		shape = DIAMOND;

		// Trap stays active so sheep can be cycled repeatedly
		disarmedByActivation = false;
	}

	private static final float SPAWN_DELAY = 0.2f;

	@Override
	public void activate() {

		Char ch = Actor.findChar(pos);

		if (ch == null || ch == Dungeon.hero) {
			return;
		}

		// Cycle: SheepSokoban -> SheepSokobanCorner -> SheepSokobanStop -> (cycle)
		//        SheepSokobanSwitch -> SheepSokoban
		Mob replacement = null;

		if (ch instanceof SheepSokoban) {
			replacement = new SheepSokobanCorner();
		} else if (ch instanceof SheepSokobanCorner) {
			replacement = new SheepSokobanStop();
		} else if (ch instanceof SheepSokobanSwitch) {
			replacement = new SheepSokoban();
		}

		if (replacement != null) {
			ch.destroy();
			ch.sprite.killAndErase();
			CellEmitter.get(pos).burst(ElmoParticle.FACTORY, 6);

			replacement.pos = pos;
			GameScene.add(replacement, SPAWN_DELAY);
			Dungeon.level.occupyCell(replacement);
		}
	}
}

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

	//the sheep a change sets down on this cell is not changed again by the same press: the
	//level's occupyCell would fire the trap under it once more, and now that every sheep is one
	//that can still change, that went on until the stack overflowed
	private static boolean changing = false;

	@Override
	public void activate() {

		Char ch = Actor.findChar(pos);

		if (changing || ch == null || ch == Dungeon.hero) {
			return;
		}

		// Cycle: SheepSokoban -> SheepSokobanCorner -> SheepSokobanSwitch -> SheepSokoban,
		// every one of them a sheep that can still be moved. The corner sheep used to become a
		// SheepSokobanStop, which nothing moves or changes again, and the puzzle was lost
		Mob replacement = null;

		if (ch instanceof SheepSokoban) {
			replacement = new SheepSokobanCorner();
		} else if (ch instanceof SheepSokobanCorner) {
			replacement = new SheepSokobanSwitch();
		} else if (ch instanceof SheepSokobanSwitch) {
			replacement = new SheepSokoban();
		}

		if (replacement != null) {
			ch.destroy();
			ch.sprite.killAndErase();
			if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(ElmoParticle.FACTORY, 6);

			replacement.pos = pos;
			GameScene.add(replacement, SPAWN_DELAY);
			changing = true;
			try {
				Dungeon.level.occupyCell(replacement);
			} finally {
				changing = false;
			}
		}
	}
}

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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

public class SokobanPortalTrap extends Trap {

	{
		color = TEAL;
		shape = CROSSHAIR;

		// Portal traps remain active for repeated use
		disarmedByActivation = false;
		canBeHidden = false;
	}

	// The destination cell this portal sends the hero to.
	// Must be set during level construction.
	public int destPos = -1;

	@Override
	public void activate() {

		Char ch = Actor.findChar(pos);

		if (ch instanceof Hero && destPos != -1) {
			// Teleport the hero to the destination cell
			if (ScrollOfTeleportation.teleportToLocation(ch, destPos)) {
				Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
				CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 4);
				CellEmitter.get(destPos).burst(Speck.factory(Speck.LIGHT), 4);
			}
		}
	}

	private static final String DEST_POS = "dest_pos";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(DEST_POS, destPos);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		destPos = bundle.getInt(DEST_POS);
	}
}

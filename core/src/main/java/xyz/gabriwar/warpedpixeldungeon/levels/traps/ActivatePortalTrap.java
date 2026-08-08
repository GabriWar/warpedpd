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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokoban;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanCorner;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SheepSokobanSwitch;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import com.watabou.utils.Bundle;

public class ActivatePortalTrap extends Trap {

	{
		color = TEAL;
		shape = STARS;

		// This trap stays active after triggering so it can be re-used
		disarmedByActivation = false;
	}

	// In Sprouted this was a static flag checked by external systems.
	// We keep it as an instance field and persist it.
	public boolean portalActivated = false;
	public int portalPos = 0;

	@Override
	public void activate() {

		Char ch = Actor.findChar(pos);

		if (ch instanceof SheepSokoban
				|| ch instanceof SheepSokobanCorner
				|| ch instanceof SheepSokobanSwitch) {
			portalActivated = true;

			if (Dungeon.level.heroFOV[pos]) {
				CellEmitter.center(pos).burst(Speck.factory(Speck.LIGHT), 6);
			}
		}
	}

	private static final String PORTAL_ACTIVATED = "portal_activated";
	private static final String PORTAL_POS       = "portal_pos";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(PORTAL_ACTIVATED, portalActivated);
		bundle.put(PORTAL_POS, portalPos);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		portalActivated = bundle.getBoolean(PORTAL_ACTIVATED);
		portalPos = bundle.getInt(PORTAL_POS);
	}
}

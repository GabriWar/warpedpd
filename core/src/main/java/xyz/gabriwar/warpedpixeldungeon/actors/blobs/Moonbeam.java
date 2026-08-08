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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

public class Moonbeam extends Blob {

	@Override
	protected void evolve() {

		// Moonbeams vanish when moon sets or heavy clouds roll in
		if (!DayNightCycle.isNight() || ClimateManager.moonLight() < 0.01f) {
			fullyClear();
			return;
		}

		// Moon intensity scales damage and decay rate
		float moonStr = ClimateManager.moonLight();

		int cell;

		for (int i = area.left; i < area.right; i++) {
			for (int j = area.top; j < area.bottom; j++) {
				cell = i + j * Dungeon.level.width();
				if (cur[cell] > 0) {

					off[cell] = cur[cell] - 1;
					volume += off[cell];

					Char ch = Actor.findChar(cell);

					// Undead take holy damage scaled by moonlight intensity
					if (ch != null && !ch.isImmune(getClass())
							&& Char.hasProp(ch, Char.Property.UNDEAD)) {
						int baseDmg = 2 + Dungeon.scalingDepth() / 4;
						int dmg = Math.max(1, Math.round(baseDmg * moonStr * 4f));
						ch.damage(dmg, this);
					}

					// Demonic creatures are also weakened
					if (ch != null && !ch.isImmune(getClass())
							&& Char.hasProp(ch, Char.Property.DEMONIC)) {
						int baseDmg = 1 + Dungeon.scalingDepth() / 6;
						int dmg = Math.max(1, Math.round(baseDmg * moonStr * 3f));
						ch.damage(dmg, this);
					}

				} else {
					off[cell] = 0;
				}
			}
		}
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);
		emitter.start(ShaftParticle.FACTORY, 0.6f, 0);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}

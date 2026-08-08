/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

/**
 * Blessed water splashed by a shattered holy water vial. It does not spread,
 * it just soaks the ground for a few turns, scalding anything that stands in
 * it - twice as hard if that thing is undead or demonic.
 */
public class HolyWaterPool extends Blob {

	@Override
	protected void evolve() {

		int cell;

		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*Dungeon.level.width();
				off[cell] = cur[cell] > 0 ? cur[cell] - 1 : 0;

				volume += off[cell];

				if (off[cell] > 0){
					Char ch = Actor.findChar(cell);
					if (ch != null && !ch.isImmune(getClass())){
						int dmg = Random.NormalIntRange(1, 4);
						if (ch.properties().contains(Char.Property.UNDEAD)
								|| ch.properties().contains(Char.Property.DEMONIC)){
							dmg *= 2;
						}
						ch.damage(dmg, this);
					}
				}
			}
		}
	}

	@Override
	public void use(BlobEmitter emitter) {
		super.use(emitter);
		emitter.pour(Speck.factory(Speck.BUBBLE), 0.4f);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}

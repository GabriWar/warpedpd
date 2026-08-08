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
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SunlightParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

public class UnfilteredSunlight extends Blob {

	@Override
	protected void evolve() {
		super.evolve();

		Char ch;
		int cell;

		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j* Dungeon.level.width();
				if (cur[cell] > 0) {
					TileTemperature.depositHeat(cell, cur[cell] * 0.08f);
					if ((ch = Actor.findChar( cell )) != null && !ch.isImmune(this.getClass())) {
						if (ch instanceof Mob) {
							if (ch.alignment != Char.Alignment.ALLY && ch.alignment != Char.Alignment.NEUTRAL) {
								Buff.prolong( ch, Blindness.class, Random.IntRange( 2, 5 ) );
								if (Random.Int(2) == 0) {
									ch.damage(ch.HP, this);
								}
							}
						}
						if (ch instanceof Hero) {
							Buff.prolong( ch, Blindness.class, Random.IntRange( 2, 5 ) );
						}
					}
				}
			}
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.start( SunlightParticle.FACTORY, 0.5f, 10 );
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}

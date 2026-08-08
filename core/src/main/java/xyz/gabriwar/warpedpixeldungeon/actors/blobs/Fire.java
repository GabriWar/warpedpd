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
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

public class Fire extends Blob {

	@Override
	protected void evolve() {

		boolean[] flamable = Dungeon.level.flamable;
		int cell;
		int fire;
		
		Freezing freeze = (Freezing)Dungeon.level.blobs.get( Freezing.class );

		boolean observe = false;

		for (int i = area.left-1; i <= area.right; i++) {
			for (int j = area.top-1; j <= area.bottom; j++) {
				cell = i + j*Dungeon.level.width();
				if (cur[cell] > 0) {

					if (freeze != null && freeze.volume > 0 && freeze.cur[cell] > 0){
						freeze.clear(cell);
						off[cell] = cur[cell] = 0;
						continue;
					}

					TileTemperature.depositHeat(cell, cur[cell] * 15.0f);
					burn( cell );

					fire = cur[cell] - 1;
					if (fire <= 0 && flamable[cell]) {

						boolean wasBookshelf = Dungeon.level.map[cell] == Terrain.BOOKSHELF;
						Dungeon.level.destroy( cell );

						//Sprouted: burning bookshelves with MindVision can reveal journal pages
						if (wasBookshelf && com.watabou.utils.Random.Float() < 0.02f
								&& Dungeon.hero.buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision.class) != null) {
							if (!Dungeon.LimitedDrops.VAULT_PAGE.dropped()) {
								Dungeon.level.drop(new xyz.gabriwar.warpedpixeldungeon.items.journalpages.Vault(), cell);
								Dungeon.LimitedDrops.VAULT_PAGE.drop();
							}
						}
						if (wasBookshelf && com.watabou.utils.Random.Float() < 0.02f
								&& Dungeon.hero.buff(xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision.class) != null) {
							if (!Dungeon.LimitedDrops.DRAGON_CAVE.dropped()) {
								Dungeon.level.drop(new xyz.gabriwar.warpedpixeldungeon.items.journalpages.DragonCave(), cell);
								Dungeon.LimitedDrops.DRAGON_CAVE.drop();
							}
						}

						observe = true;
						GameScene.updateMap( cell );

					}

				} else if (freeze == null || freeze.volume <= 0 || freeze.cur[cell] <= 0) {

					if (flamable[cell]
							&& (cur[cell-1] > 0
							|| cur[cell+1] > 0
							|| cur[cell-Dungeon.level.width()] > 0
							|| cur[cell+Dungeon.level.width()] > 0)
							//living overworld vegetation resists catching: ignition is
							//probabilistic and subcritical there, so a fire burns a patch
							//and gutters out instead of consuming the whole map
							&& (!(Dungeon.level instanceof xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel)
									|| com.watabou.utils.Random.Int(8) == 0)) {
						fire = 4;
						burn( cell );
						area.union(i, j);
					} else {
						fire = 0;
					}

				} else {
					fire = 0;
				}

				volume += (off[cell] = fire);
			}
		}

		if (observe) {
			Dungeon.observe();
		}
	}
	
	public static void burn( int pos ) {
		Char ch = Actor.findChar( pos );
		if (ch != null && !ch.isImmune(Fire.class)) {
			Buff.affect( ch, Burning.class ).reignite( ch );
		}
		
		Heap heap = Dungeon.level.heaps.get( pos );
		if (heap != null) {
			heap.burn();
		}

		Plant plant = Dungeon.level.plants.get( pos );
		if (plant != null){
			plant.wither();
		}
	}
	
	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.pour( FlameParticle.FACTORY, 0.03f );
	}
	
	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}

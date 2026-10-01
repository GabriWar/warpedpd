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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

/**
 * A bed of coals: fuel that any fire can light, that then burns by itself for a fixed
 * number of turns, hotter than open flame and without needing anything to eat. It is a
 * heat source for the temperature simulation and nothing more - what is done with the
 * heat (a weapon on the forge's anvil, an egg in the incubator's nest) is the room's
 * business, read off TileTemperature like any other heat.
 *
 * Each marked cell carries its own state in its volume:
 *   1            cold, ready to be lit
 *   2 .. 1+COOL  burnt out and cooling; its own leftover heat must not relight it
 *   100+n        burning, n turns left
 * A cell lights when open fire touches it, when a lit bed beside it creeps into it, or
 * when the air over it climbs past LIGHT_TEMP.
 */
public abstract class CoalBed extends Blob {

	private static final int COLD = 1;
	private static final int COOL = 14;
	private static final int BURNING = 100;
	/** tile temperature (C) at which cold coals catch without a flame */
	public static final float LIGHT_TEMP = 60f;

	{
		//after the fire blob, so a flame seeded this turn is seen this turn
		actPriority = BLOB_PRIO - 1;
	}

	/** turns one lighting burns for */
	protected abstract int burnTurns();
	/** heat laid on the coals' own cell every burning turn */
	protected abstract float heat();
	/** heat laid on each open neighbour every burning turn */
	protected abstract float radiate();

	public boolean burning( int cell ){
		return volume > 0 && cur != null && cur[cell] > BURNING;
	}

	@Override
	protected void evolve(){
		Fire fire = (Fire) Dungeon.level.blobs.get( Fire.class );
		int cell;
		for (int i = area.top - 1; i <= area.bottom; i++){
			for (int j = area.left - 1; j <= area.right; j++){
				cell = j + i * Dungeon.level.width();
				if (!Dungeon.level.insideMap( cell )) continue;
				int v = cur[cell];
				if (v <= 0){
					off[cell] = 0;
					continue;
				}

				if (v == COLD){
					boolean flame = fire != null && fire.volume > 0 && fire.cur[cell] > 0;
					//a lit bed creeps into the cold one beside it, a turn in four
					boolean crept = false;
					for (int ofs : PathFinder.NEIGHBOURS8){
						int n = cell + ofs;
						if (Dungeon.level.insideMap( n ) && cur[n] > BURNING && Random.Int( 4 ) == 0){
							crept = true;
							break;
						}
					}
					if (flame || crept || airTemp( cell ) >= LIGHT_TEMP){
						v = BURNING + burnTurns();
						WarpedRoomTiles.flip( Dungeon.level, cell, true );
						if (Dungeon.level.heroFOV[cell]){
							Sample.INSTANCE.play( Assets.Sounds.BURNING );
							GLog.i( Messages.get( CoalBed.class, "lit" ) );
						}
					}
				} else if (v > BURNING){
					burn( cell );
					v--;
					if (v == BURNING){
						v = COLD + COOL;
						WarpedRoomTiles.flip( Dungeon.level, cell, false );
						if (Dungeon.level.heroFOV[cell]) GLog.i( Messages.get( CoalBed.class, "out" ) );
					}
				} else {
					//cooling: counts down to cold
					v--;
				}

				off[cell] = v;
				volume += v;
			}
		}
	}

	//the air over the bed: the floor's climate plus whatever heat has been brought to the
	//tile. Not tileTemp(), which counts a bed of embers as warm in itself - the coals sit
	//on one, and would light themselves on any hot floor
	private static float airTemp( int cell ){
		return ClimateManager.localTemp() + Dungeon.level.tileHeat[cell];
	}

	private void burn( int cell ){
		TileTemperature.depositHeat( cell, heat() );
		for (int ofs : PathFinder.NEIGHBOURS8){
			int n = cell + ofs;
			if (Dungeon.level.insideMap( n ) && !Dungeon.level.solid[n]){
				TileTemperature.depositHeat( n, radiate() );
			}
		}

		Char ch = Actor.findChar( cell );
		if (ch != null && !ch.isImmune( Burning.class )){
			Buff.affect( ch, Burning.class ).reignite( ch );
		}
		Heap heap = Dungeon.level.heaps.get( cell );
		if (heap != null) heap.burn();

		if (Dungeon.level.heroFOV[cell]){
			CellEmitter.get( cell ).burst( FlameParticle.FACTORY, 3 );
		}
	}

	@Override
	public String tileDesc(){
		return null;
	}
}

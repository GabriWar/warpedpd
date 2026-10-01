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
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Egg;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

/**
 * The Incubator Nest's brooding. An egg is shaped by what happens to it (Egg: burns,
 * freezes, poisons, lightning), and out in the dungeon those only come one accident at a
 * time. An egg left lying in the nest is brooded steadily instead: every BROOD_EVERY
 * turns the nest reads the tile it sits on and counts whatever is true of it towards
 * the egg -
 *
 *   HOT degrees or more      one burn        (thirty for a red dragon, one for a rooster)
 *   freezing or less         one freeze      (five for a blue dragon)
 *   a poisonous cloud on it  one poisoning   (five for a violet dragon)
 *   live sparks on it        one lightning   (five for a green dragon)
 *
 * - so what hatches is chosen by whoever controls the room's temperature and air, not by
 * luck. The nest never harms the egg, and it does nothing for anything that is not one.
 */
public class NestBrood extends Blob {

	public static final float HOT = 45f;
	private static final int BROOD_EVERY = 4;

	private static final Class<?>[] POISONS = {
			ToxicGas.class, CorrosiveGas.class, PoisonGas.class, StenchGas.class, Miasma.class
	};

	private int turns = 0;

	@Override
	protected void evolve(){
		int nest = -1;
		int cell;
		for (int i = area.top - 1; i <= area.bottom; i++){
			for (int j = area.left - 1; j <= area.right; j++){
				cell = j + i * Dungeon.level.width();
				if (!Dungeon.level.insideMap( cell )) continue;
				off[cell] = cur[cell];
				volume += off[cell];
				if (cur[cell] > 0) nest = cell;
			}
		}
		if (nest == -1 || ++turns % BROOD_EVERY != 0) return;

		Egg egg = eggAt( nest );
		if (egg != null) brood( egg, nest );
	}

	private static Egg eggAt( int cell ){
		Heap heap = Dungeon.level.heaps.get( cell );
		if (heap == null || heap.type != Heap.Type.HEAP) return null;
		for (Item item : heap.items){
			if (item instanceof Egg) return (Egg) item;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void brood( Egg egg, int cell ){
		boolean seen = Dungeon.level.heroFOV[cell];
		float temp = TileTemperature.tileTemp( cell );

		if (temp >= HOT){
			egg.burns++;
			if (seen) CellEmitter.get( cell ).burst( FlameParticle.FACTORY, 2 );
			announce( seen, egg.burns, Egg.RED_DRAGON, "stir_fire" );
		} else if (temp <= 0f){
			egg.freezes++;
			if (seen) CellEmitter.get( cell ).burst( SnowParticle.FACTORY, 3 );
			announce( seen, egg.freezes, Egg.BLUE_DRAGON, "stir_frost" );
		}

		for (Class<?> type : POISONS){
			Blob gas = Dungeon.level.blobs.get( (Class<? extends Blob>) type );
			if (gas != null && gas.volume > 0 && gas.cur[cell] > 0){
				egg.poisons++;
				if (seen) CellEmitter.get( cell ).burst( PoisonParticle.SPLASH, 3 );
				announce( seen, egg.poisons, Egg.VIOLET_DRAGON, "stir_poison" );
				break;
			}
		}

		Blob sparks = Dungeon.level.blobs.get( Electricity.class );
		if (sparks != null && sparks.volume > 0 && sparks.cur[cell] > 0){
			egg.lits++;
			if (seen) CellEmitter.get( cell ).burst( SparkParticle.FACTORY, 3 );
			announce( seen, egg.lits, Egg.GREEN_DRAGON, "stir_spark" );
		}
	}

	//said once, on the tick a count first reaches what it takes to hatch that way
	private void announce( boolean seen, int count, int needed, String key ){
		if (seen && count == needed) GLog.p( Messages.get( this, key ) );
	}

	@Override
	public String tileDesc(){
		int nest = -1;
		if (cur != null){
			for (int i = 0; i < cur.length; i++) if (cur[i] > 0){ nest = i; break; }
		}
		Egg egg = nest == -1 ? null : eggAt( nest );
		if (egg == null) return Messages.get( this, "desc_empty" );
		return Messages.get( this, "desc_egg", egg.burns, egg.freezes, egg.poisons, egg.lits );
	}

	private static final String TURNS = "turns";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( TURNS, turns );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		turns = bundle.getInt( TURNS );
	}
}

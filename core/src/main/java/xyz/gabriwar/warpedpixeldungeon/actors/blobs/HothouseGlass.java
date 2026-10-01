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

import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.PlantGrowthManager;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * The glass over the Hothouse's beds. It marks the planting cells and keeps a season of
 * its own under it:
 *
 *   warmth   a bed colder than MILD is nudged back towards it every turn, so the beds do
 *            not freeze in a winter that kills everything outside them
 *   growth   every GROW_EVERY turns (spring's pace - half the summer interval the rest of
 *            the floor grows at, PlantGrowthManager) something happens in the beds: more
 *            often than not a plant already there spreads to the bed beside it, and
 *            otherwise a new one comes up from the floor's own spring tables. Turns
 *            missed while the hero was off the floor are caught up, to the same cap the
 *            floor's growth uses
 *   seed     a plant that withers under the glass leaves its seed SEED_BONUS more often
 *            (Plant.wither asks covers())
 *
 * So a seed planted here is worth coming back for: it spreads, and what spreads pays
 * out in seeds when it is harvested. The beds' plants are planted, not natural - the
 * floor's winter kill and its eviction of old growth never touch them.
 */
public class HothouseGlass extends Blob {

	public static final float SEED_BONUS = 0.5f;
	private static final float MILD = 18f;
	private static final int GROW_EVERY = 125;
	private static final int MAX_CATCHUP = 8;

	private int lastGrowth = -1;

	@Override
	protected void evolve(){
		int cell;
		for (int i = area.top - 1; i <= area.bottom; i++){
			for (int j = area.left - 1; j <= area.right; j++){
				cell = j + i * Dungeon.level.width();
				if (!Dungeon.level.insideMap( cell )) continue;
				off[cell] = cur[cell];
				volume += off[cell];
				if (cur[cell] > 0 && TileTemperature.tileTemp( cell ) < MILD){
					TileTemperature.depositHeat( cell, 2f );
				}
			}
		}

		if (Dungeon.isChallenged( Challenges.NO_HERBALISM )) return;
		if (lastGrowth < 0){
			lastGrowth = Dungeon.cycleTurn;
			return;
		}
		int due = Math.min( MAX_CATCHUP, (Dungeon.cycleTurn - lastGrowth) / GROW_EVERY );
		if (due <= 0) return;
		lastGrowth = Dungeon.cycleTurn;
		for (int i = 0; i < due; i++) grow();
	}

	public static boolean covers( int cell ){
		if (Dungeon.level == null) return false;
		HothouseGlass glass = (HothouseGlass) Dungeon.level.blobs.get( HothouseGlass.class );
		return glass != null && glass.volume > 0 && glass.cur != null
				&& cell >= 0 && cell < glass.cur.length && glass.cur[cell] > 0;
	}

	private boolean free( int cell ){
		return cur[cell] > 0 && Dungeon.level.plants.get( cell ) == null
				&& Actor.findChar( cell ) == null && Dungeon.level.heaps.get( cell ) == null;
	}

	private void grow(){
		ArrayList<Integer> planted = new ArrayList<>();
		ArrayList<Integer> open = new ArrayList<>();
		for (int cell = 0; cell < cur.length; cell++){
			if (cur[cell] <= 0) continue;
			if (Dungeon.level.plants.get( cell ) != null) planted.add( cell );
			else if (free( cell )) open.add( cell );
		}
		if (open.isEmpty()) return;

		//three times in five, something already growing spreads to the bed next to it
		if (!planted.isEmpty() && Random.Int( 5 ) < 3){
			Random.shuffle( planted );
			for (int from : planted){
				ArrayList<Integer> beside = new ArrayList<>();
				for (int ofs : PathFinder.NEIGHBOURS8){
					int n = from + ofs;
					if (Dungeon.level.insideMap( n ) && free( n )) beside.add( n );
				}
				if (beside.isEmpty()) continue;
				Plant parent = Dungeon.level.plants.get( from );
				if (parent == null || parent.seedClass() == null) continue;
				sow( parent.seedClass(), Random.element( beside ) );
				return;
			}
		}

		Class<? extends Plant.Seed> fresh = PlantGrowthManager.selectPlantClass(
				Dungeon.level, GameCalendar.Season.SPRING );
		if (fresh != null) sow( fresh, Random.element( open ) );
	}

	private void sow( Class<? extends Plant.Seed> seedClass, int cell ){
		Plant.Seed seed = Reflection.newInstance( seedClass );
		if (seed == null) return;
		//Level.plant raises the plant's sprite itself; nothing more to show
		Dungeon.level.plant( seed, cell );
	}

	@Override
	public String tileDesc(){
		return Messages.get( this, "desc" );
	}

	private static final String LAST_GROWTH = "last_growth";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( LAST_GROWTH, lastGrowth );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		lastGrowth = bundle.getInt( LAST_GROWTH );
	}
}

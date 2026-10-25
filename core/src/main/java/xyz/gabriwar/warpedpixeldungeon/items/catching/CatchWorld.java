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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.WorldSection;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The catching's run-wide record (HomesteadWorld's catching section), saved with the game:
 *
 * - how many critters the net took in each stretch of land today (CritterCatch.areaKey), for the
 *   thinning (CritterCatch.thinned); a day gone by is forgotten;
 * - the caches the notes in the bottles mark (BottleCaches), world cells of the surface, until each
 *   is dug up; and how many notes were read, which places each new one apart from the last.
 *
 * Every key is optional: a save from before the catching loads with none of it.
 */
public class CatchWorld extends WorldSection {

	//by stretch of land: { the day, how many netted that day }
	private final HashMap<Long, int[]> netted = new HashMap<>();
	//the bottles' caches still in the ground: { wx, wy }
	private final ArrayList<int[]> caches = new ArrayList<>();
	private int notesRead;

	public CatchWorld(){
	}

	/** This run's record, or null when the homesteads' world is not built (a test's). */
	public static CatchWorld get(){
		return HomesteadWorld.section( CatchWorld.class );
	}

	/** How many the net took today in this stretch of land. */
	public synchronized int nettedOn( long area, int day ){
		int[] e = netted.get( area );
		return e == null || e[0] != day ? 0 : e[1];
	}

	/** One more taken today in this stretch of land; the days gone by are forgotten. */
	public synchronized void netted( long area, int day ){
		for (Iterator<Map.Entry<Long, int[]>> it = netted.entrySet().iterator(); it.hasNext(); ){
			if (it.next().getValue()[0] != day) it.remove();
		}
		int[] e = netted.get( area );
		if (e == null){
			netted.put( area, new int[]{ day, 1 } );
		} else {
			e[1]++;
		}
	}

	/** The caches still in the ground, { wx, wy } on the surface, oldest first. */
	public synchronized List<int[]> caches(){
		ArrayList<int[]> out = new ArrayList<>();
		for (int[] c : caches) out.add( c.clone() );
		return out;
	}

	public synchronized void addCache( int wx, int wy ){
		for (int[] c : caches) if (c[0] == wx && c[1] == wy) return;
		caches.add( new int[]{ wx, wy } );
	}

	/** The cache within a step of the world cell (wx, wy), or null. */
	public synchronized int[] cacheNear( int wx, int wy ){
		for (int[] c : caches){
			if (Math.abs( c[0] - wx ) <= 1 && Math.abs( c[1] - wy ) <= 1) return c.clone();
		}
		return null;
	}

	/** Dug up: true when there was one there. */
	public synchronized boolean removeCache( int wx, int wy ){
		for (int i = 0; i < caches.size(); i++){
			if (caches.get( i )[0] == wx && caches.get( i )[1] == wy){
				caches.remove( i );
				return true;
			}
		}
		return false;
	}

	/** A note read: its number, from 0 (each places its cache apart from the others'). */
	public synchronized int readNote(){
		return notesRead++;
	}

	private static final String NET_AREAS = "net_areas", NET_DAYS = "net_days", NET_COUNTS = "net_counts",
			CACHE_X = "cache_x", CACHE_Y = "cache_y", NOTES = "notes_read";

	@Override
	public synchronized void storeInBundle( Bundle bundle ){
		long[] areas = new long[netted.size()];
		int[] days = new int[netted.size()], counts = new int[netted.size()];
		int i = 0;
		for (Map.Entry<Long, int[]> e : netted.entrySet()){
			areas[i] = e.getKey();
			days[i] = e.getValue()[0];
			counts[i] = e.getValue()[1];
			i++;
		}
		bundle.put( NET_AREAS, areas );
		bundle.put( NET_DAYS, days );
		bundle.put( NET_COUNTS, counts );
		int[] xs = new int[caches.size()], ys = new int[caches.size()];
		for (int k = 0; k < xs.length; k++){
			xs[k] = caches.get( k )[0];
			ys[k] = caches.get( k )[1];
		}
		bundle.put( CACHE_X, xs );
		bundle.put( CACHE_Y, ys );
		bundle.put( NOTES, notesRead );
	}

	@Override
	public synchronized void restoreFromBundle( Bundle bundle ){
		netted.clear();
		caches.clear();
		if (bundle.contains( NET_AREAS ) && bundle.contains( NET_DAYS ) && bundle.contains( NET_COUNTS )){
			long[] areas = bundle.getLongArray( NET_AREAS );
			int[] days = bundle.getIntArray( NET_DAYS ), counts = bundle.getIntArray( NET_COUNTS );
			for (int i = 0; i < Math.min( areas.length, Math.min( days.length, counts.length ) ); i++){
				netted.put( areas[i], new int[]{ days[i], counts[i] } );
			}
		}
		if (bundle.contains( CACHE_X ) && bundle.contains( CACHE_Y )){
			int[] xs = bundle.getIntArray( CACHE_X ), ys = bundle.getIntArray( CACHE_Y );
			for (int i = 0; i < Math.min( xs.length, ys.length ); i++) caches.add( new int[]{ xs[i], ys[i] } );
		}
		notesRead = bundle.contains( NOTES ) ? bundle.getInt( NOTES ) : 0;
	}
}

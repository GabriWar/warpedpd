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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.WorldSection;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The waystones the hero knows of, for the whole run (HomesteadWorld's travel section): every one
 * he has SEEN (the world map pins it, dim) and every one he has ATTUNED by touching it (pinned
 * bright, and a place to travel to). A waystone is known by its settlement (Waystones.key: the
 * settlement's sector, or TOWN for the town's own), with the world cell it stands on and its
 * settlement's name, so the map and the travel window need nothing loaded to show it. All of them
 * stand on the surface. Host-owned, as everything of the homesteads is.
 */
public class TravelWorld extends WorldSection {

	/** A waystone the hero knows of. */
	public static final class Stone {
		public final long key;
		public final int wx, wy;
		public final String name;
		public boolean attuned;

		Stone( long key, int wx, int wy, String name, boolean attuned ){
			this.key = key;
			this.wx = wx;
			this.wy = wy;
			this.name = name;
			this.attuned = attuned;
		}
	}

	private final LinkedHashMap<Long, Stone> stones = new LinkedHashMap<>();

	/** This run's section (null only when the travel part is missing, which it never is here). */
	public static TravelWorld get(){
		return HomesteadWorld.section( TravelWorld.class );
	}

	/** The hero has seen a waystone (no change once he has). */
	public synchronized void see( long key, int wx, int wy, String name ){
		if (!stones.containsKey( key )) stones.put( key, new Stone( key, wx, wy, name, false ) );
	}

	/** The hero touched a waystone: it is attuned for the rest of the run. True when it was not yet. */
	public synchronized boolean attune( long key, int wx, int wy, String name ){
		Stone s = stones.get( key );
		if (s == null){
			stones.put( key, new Stone( key, wx, wy, name, true ) );
			return true;
		}
		if (s.attuned) return false;
		s.attuned = true;
		return true;
	}

	/** A waystone was put down on a world cell: one known already (attuned before it stood, from
	 *  afar) is known by that cell from now on. */
	public synchronized void placed( long key, int wx, int wy ){
		Stone s = stones.get( key );
		if (s != null && (s.wx != wx || s.wy != wy)) stones.put( key, new Stone( key, wx, wy, s.name, s.attuned ) );
	}

	public synchronized boolean isAttuned( long key ){
		Stone s = stones.get( key );
		return s != null && s.attuned;
	}

	public synchronized boolean isSeen( long key ){
		return stones.containsKey( key );
	}

	public synchronized Stone stone( long key ){
		return stones.get( key );
	}

	/** Every waystone known, in the order they were found. */
	public synchronized List<Stone> known(){
		return Collections.unmodifiableList( new ArrayList<>( stones.values() ) );
	}

	/** The attuned ones, in the order they were found. */
	public synchronized List<Stone> attuned(){
		ArrayList<Stone> out = new ArrayList<>();
		for (Stone s : stones.values()) if (s.attuned) out.add( s );
		return out;
	}

	private static final String KEYS = "keys", XS = "xs", YS = "ys", NAMES = "names", ATTUNED = "attuned";

	@Override
	public synchronized void storeInBundle( Bundle bundle ){
		int n = stones.size(), i = 0;
		long[] keys = new long[n];
		int[] xs = new int[n], ys = new int[n];
		String[] names = new String[n];
		boolean[] att = new boolean[n];
		for (Stone s : stones.values()){
			keys[i] = s.key;
			xs[i] = s.wx;
			ys[i] = s.wy;
			names[i] = s.name == null ? "" : s.name;
			att[i] = s.attuned;
			i++;
		}
		bundle.put( KEYS, keys );
		bundle.put( XS, xs );
		bundle.put( YS, ys );
		bundle.put( NAMES, names );
		bundle.put( ATTUNED, att );
	}

	@Override
	public synchronized void restoreFromBundle( Bundle bundle ){
		stones.clear();
		if (!bundle.contains( KEYS )) return;
		long[] keys = bundle.getLongArray( KEYS );
		int[] xs = bundle.getIntArray( XS ), ys = bundle.getIntArray( YS );
		String[] names = bundle.getStringArray( NAMES );
		boolean[] att = bundle.getBooleanArray( ATTUNED );
		int n = Math.min( Math.min( keys.length, xs.length ), Math.min( Math.min( ys.length, names.length ), att.length ) );
		for (int i = 0; i < n; i++) stones.put( keys[i], new Stone( keys[i], xs[i], ys[i], names[i], att[i] ) );
	}
}

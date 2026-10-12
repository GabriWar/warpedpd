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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A map keyed by world cell (OverworldLevel.worldKey) that also knows which keys lie in each
 * square SECTOR of the world, so whatever lies near a window is found in the time it takes to
 * walk the sectors the window touches, not every entry in the world: the overworld's diff store
 * (the player's terrain edits, laid over every window derived) and the homesteads' stores.
 *
 * Still a HashMap for everything that reads it. Write it through put, putAll, putIfAbsent, remove
 * and clear, which keep the index; the views (entrySet, keySet, values) are read-only, so no
 * iterator can drop a key behind the index's back. Map's compute and merge defaults would bypass
 * the index: do not call them on it.
 */
public class SectorMap<V> extends HashMap<Long, V> {

	/** The side of a sector, in cells. A 176-cell window touches 4x4 of them, 5x5 at most. */
	public static final int SECTOR = 64;

	private final HashMap<Long, HashSet<Long>> sectors = new HashMap<>();

	public SectorMap(){
		super();
	}

	/** A copy of another map's entries, indexed. */
	public SectorMap( Map<Long, ? extends V> from ){
		super();
		putAll( from );
	}

	public static int wx( long key ){
		return (int) (key & 0xFFFFFFFFL);
	}

	public static int wy( long key ){
		return (int) (key >> 32);
	}

	private static long sectorOf( long key ){
		return OverworldLevel.worldKey( Math.floorDiv( wx( key ), SECTOR ), Math.floorDiv( wy( key ), SECTOR ) );
	}

	@Override
	public V put( Long key, V value ){
		V old = super.put( key, value );
		long sector = sectorOf( key );
		HashSet<Long> in = sectors.get( sector );
		if (in == null) sectors.put( sector, in = new HashSet<>() );
		in.add( key );
		return old;
	}

	@Override
	public void putAll( Map<? extends Long, ? extends V> m ){
		for (Map.Entry<? extends Long, ? extends V> e : m.entrySet()) put( e.getKey(), e.getValue() );
	}

	@Override
	public V putIfAbsent( Long key, V value ){
		V had = get( key );
		if (had == null) put( key, value );
		return had;
	}

	@Override
	public V remove( Object key ){
		if (!(key instanceof Long) || !containsKey( key )) return null;
		V old = super.remove( key );
		long sector = sectorOf( (Long) key );
		HashSet<Long> in = sectors.get( sector );
		if (in != null){
			in.remove( key );
			if (in.isEmpty()) sectors.remove( sector );
		}
		return old;
	}

	@Override
	public boolean remove( Object key, Object value ){
		if (!containsKey( key ) || !java.util.Objects.equals( get( key ), value )) return false;
		remove( key );
		return true;
	}

	@Override
	public void clear(){
		super.clear();
		sectors.clear();
	}

	@Override
	public Set<Map.Entry<Long, V>> entrySet(){
		return Collections.unmodifiableSet( super.entrySet() );
	}

	@Override
	public Set<Long> keySet(){
		return Collections.unmodifiableSet( super.keySet() );
	}

	@Override
	public Collection<V> values(){
		return Collections.unmodifiableCollection( super.values() );
	}

	/** What a visitor of the entries in a rect is handed: the world cell and its value. */
	public interface Visitor<V> {
		void visit( int wx, int wy, V value );
	}

	/** Every entry in the world rect [x0, x1) x [y0, y1), sector by sector. */
	public void forEachIn( int x0, int y0, int x1, int y1, Visitor<V> visitor ){
		if (isEmpty() || x1 <= x0 || y1 <= y0) return;
		int sx0 = Math.floorDiv( x0, SECTOR ), sy0 = Math.floorDiv( y0, SECTOR );
		int sx1 = Math.floorDiv( x1 - 1, SECTOR ), sy1 = Math.floorDiv( y1 - 1, SECTOR );
		for (int sy = sy0; sy <= sy1; sy++){
			for (int sx = sx0; sx <= sx1; sx++){
				HashSet<Long> in = sectors.get( OverworldLevel.worldKey( sx, sy ) );
				if (in == null) continue;
				for (Long key : in){
					int wx = wx( key ), wy = wy( key );
					if (wx < x0 || wy < y0 || wx >= x1 || wy >= y1) continue;
					visitor.visit( wx, wy, get( key ) );
				}
			}
		}
	}

	/** A plain copy of the entries in the world rect [x0, x1) x [y0, y1): what a worker thread may
	 *  be handed, nothing in it shared with this map. */
	public HashMap<Long, V> snapshotIn( int x0, int y0, int x1, int y1 ){
		HashMap<Long, V> out = new HashMap<>();
		forEachIn( x0, y0, x1, y1, (wx, wy, v) -> out.put( OverworldLevel.worldKey( wx, wy ), v ) );
		return out;
	}

	/** How many sectors hold an entry (the tests read it). */
	public int sectorCount(){
		return sectors.size();
	}
}

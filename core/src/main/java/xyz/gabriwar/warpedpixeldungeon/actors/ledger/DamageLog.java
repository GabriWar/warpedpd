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

package xyz.gabriwar.warpedpixeldungeon.actors.ledger;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * The hero's two lists of hits, the last KEEP of each, newest first: what he dealt and what he
 * took. Combat writes it from the actor thread and the render thread, the hero window reads it:
 * every access holds the lock, and what it hands out are copies of the lists (the records
 * themselves are not changed once committed, but for the card the hit showed, which the render
 * thread adds a moment later).
 */
public final class DamageLog implements Bundlable {

	public static final int KEEP = 100;

	private final ArrayList<HitRecord> dealt = new ArrayList<>(), taken = new ArrayList<>();
	private int version;

	/** A copy of the hits dealt, newest first. */
	public synchronized List<HitRecord> dealt(){
		return new ArrayList<>( dealt );
	}

	/** A copy of the hits taken, newest first. */
	public synchronized List<HitRecord> taken(){
		return new ArrayList<>( taken );
	}

	public synchronized void clear(){
		dealt.clear();
		taken.clear();
		version++;
	}

	/** Grows by one every time a list changes (a hit added, a run grown, a card seen). */
	public synchronized int version(){
		return version;
	}

	synchronized void add( HitRecord r ){
		ArrayList<HitRecord> l = r.dealt ? dealt : taken;
		l.add( 0, r );
		while (l.size() > KEEP) l.remove( l.size() - 1 );
		version++;
	}

	/** The newest record of a list, null when it is empty. */
	synchronized HitRecord newest( boolean ofDealt ){
		ArrayList<HitRecord> l = ofDealt ? dealt : taken;
		return l.isEmpty() ? null : l.get( 0 );
	}

	/** The newest record of a list, swapped for its grown copy (a DOT run's next tick). */
	synchronized void replaceNewest( boolean ofDealt, HitRecord with ){
		ArrayList<HitRecord> l = ofDealt ? dealt : taken;
		if (l.isEmpty()) l.add( with );
		else l.set( 0, with );
		version++;
	}

	synchronized void changed(){
		version++;
	}

	/** The blow with this serial in either list, newest first; null when it is not (or no longer) kept. */
	synchronized HitRecord blow( int serial ){
		if (serial == 0) return null;
		for (HitRecord r : dealt) if (r.serial == serial && r.kind == HitRecord.BLOW) return r;
		for (HitRecord r : taken) if (r.serial == serial && r.kind == HitRecord.BLOW) return r;
		return null;
	}

	// ------------------------------------------------------------------ bundle

	//each list as a bundle of its records by place ("0" the newest), with their count: no class
	//name on every record
	private static final String DEALT = "dealt", TAKEN = "taken", COUNT = "n";

	@Override
	public synchronized void storeInBundle( Bundle bundle ){
		bundle.put( DEALT, write( dealt ) );
		bundle.put( TAKEN, write( taken ) );
	}

	private static Bundle write( ArrayList<HitRecord> list ){
		Bundle out = new Bundle();
		out.put( COUNT, list.size() );
		for (int i = 0; i < list.size(); i++){
			Bundle r = new Bundle();
			list.get( i ).storeInBundle( r );
			out.put( Integer.toString( i ), r );
		}
		return out;
	}

	@Override
	public synchronized void restoreFromBundle( Bundle bundle ){
		dealt.clear();
		taken.clear();
		read( bundle, DEALT, dealt, true );
		read( bundle, TAKEN, taken, false );
		version++;
	}

	private static void read( Bundle bundle, String key, ArrayList<HitRecord> into, boolean ofDealt ){
		if (!bundle.contains( key )) return;
		Bundle list = bundle.getBundle( key );
		int n = Math.min( KEEP, list.getInt( COUNT ) );
		for (int i = 0; i < n; i++){
			String k = Integer.toString( i );
			if (!list.contains( k )) continue;
			HitRecord r = new HitRecord();
			r.restoreFromBundle( list.getBundle( k ) );
			r.dealt = ofDealt;
			into.add( r );
		}
	}
}

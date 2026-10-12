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

package xyz.gabriwar.warpedpixeldungeon.homestead.threats;

import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.WorldSection;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * The threats' run-wide records, one for every home (by HomeThreats.key), saved with the game:
 * its notice as last worked out (Notice); the night up to which every night has had its siege and
 * its raid rolled, live with the hero there or settled after (ThreatSettler) - a night is rolled
 * once, never both, and none is skipped; what was settled that the hero has not been told yet, and
 * whether this absence has cost a chest already; and the events under way there (a siege or a
 * raid), with their tally for the ledger. Host-owned.
 */
public class ThreatWorld extends WorldSection {

	/** A siege or a raid under way at a home: its night, its mobs' id, and the tally so far. */
	public static final class Event {
		public static final int SIEGE = 0, RAID = 1;

		public final int kind, night;
		/** What its mobs carry (SiegeWolf.event, HomeRaider.event): one id a night and a kind. */
		public final long id;
		public int came, slain, stolen;
		/** A raid's target: the world key (OverworldLevel.worldKey) of the chest its band came for,
		 *  NO_CHEST for none. */
		public long chest = NO_CHEST;
		public static final long NO_CHEST = Long.MIN_VALUE;

		Event( int kind, int night, long id ){
			this.kind = kind;
			this.night = night;
			this.id = id;
		}
	}

	//a home's records
	static final class Record {
		int[] notice;
		int rolled = Integer.MIN_VALUE;
		//lines settled and not yet told (the hero still away), and a chest lost in this absence
		final ArrayList<String> untold = new ArrayList<>();
		boolean robbed;
		final ArrayList<Event> events = new ArrayList<>();
	}

	private final HashMap<String, Record> records = new HashMap<>();

	/** The run's records, null when the threats are not built. */
	public static ThreatWorld get(){
		return HomesteadWorld.section( ThreatWorld.class );
	}

	private Record record( HomesteadWorld.Home home ){
		Record r = records.get( HomeThreats.key( home ) );
		if (r == null){
			r = new Record();
			records.put( HomeThreats.key( home ), r );
		}
		return r;
	}

	// ----------------------------------------------------------------- the notice

	/** The home's notice as last worked out (none yet: nothing). */
	public synchronized Notice.Score notice( HomesteadWorld.Home home ){
		Record r = records.get( HomeThreats.key( home ) );
		return Notice.Score.of( r == null ? null : r.notice );
	}

	synchronized void keepNotice( HomesteadWorld.Home home, Notice.Score score ){
		record( home ).notice = score.toArray();
	}

	// ----------------------------------------------------------------- the nights

	/** The last night rolled at the home, live or settled: every night up to it is done with
	 *  (Integer.MIN_VALUE before the threats first see the home: settledTo). */
	public synchronized int rolled( HomesteadWorld.Home home ){
		Record r = records.get( HomeThreats.key( home ) );
		return r == null ? Integer.MIN_VALUE : r.rolled;
	}

	/**
	 * The last night done with at the home (rolled), worked out the first time the threats see it:
	 * the night before the first one whose dark had not ended when the hero was last there
	 * (ThreatSettler.firstNight), so a new home owes nothing and one he left owes every night since.
	 */
	public synchronized int settledTo( HomesteadWorld.Home home ){
		Record r = record( home );
		if (r.rolled == Integer.MIN_VALUE) r.rolled = ThreatSettler.firstNight( home.lastVisitTurn ) - 1;
		return r.rolled;
	}

	synchronized void roll( HomesteadWorld.Home home, int night ){
		Record r = record( home );
		r.rolled = Math.max( r.rolled, night );
	}

	// ----------------------------------------------------------------- what is told on his return

	/** Has this absence cost the home a chest already (ThreatSettler: never more than one)? */
	synchronized boolean robbed( HomesteadWorld.Home home ){
		return record( home ).robbed;
	}

	synchronized void setRobbed( HomesteadWorld.Home home ){
		record( home ).robbed = true;
	}

	/** Lines settled while the hero is away, kept to be told when he is back. */
	synchronized void keepUntold( HomesteadWorld.Home home, List<String> lines ){
		record( home ).untold.addAll( lines );
	}

	/** The hero is back: every line kept for him, oldest first, and the absence over. */
	synchronized List<String> tell( HomesteadWorld.Home home ){
		Record r = records.get( HomeThreats.key( home ) );
		if (r == null) return new ArrayList<>();
		ArrayList<String> out = new ArrayList<>( r.untold );
		r.untold.clear();
		r.robbed = false;
		return out;
	}

	// ----------------------------------------------------------------- the events

	/** A siege or a raid begins at a home: its record, its id for the mobs it brings. */
	synchronized Event begin( HomesteadWorld.Home home, int kind, int night ){
		long id = ((long) night << 1 | kind) ^ ((long) HomeThreats.key( home ).hashCode() << 32);
		Event e = new Event( kind, night, id );
		record( home ).events.add( e );
		return e;
	}

	/** The event of an id at a home, or null when it is over. */
	public synchronized Event event( String homeKey, long id ){
		Record r = records.get( homeKey );
		if (r == null) return null;
		for (Event e : r.events) if (e.id == id) return e;
		return null;
	}

	/** The events under way at a home. */
	public synchronized ArrayList<Event> events( HomesteadWorld.Home home ){
		Record r = records.get( HomeThreats.key( home ) );
		return r == null ? new ArrayList<>() : new ArrayList<>( r.events );
	}

	synchronized void end( HomesteadWorld.Home home, Event e ){
		Record r = records.get( HomeThreats.key( home ) );
		if (r != null) r.events.remove( e );
	}

	/** One of an event's mobs fell. */
	public synchronized void slain( String homeKey, long id ){
		Event e = event( homeKey, id );
		if (e != null) e.slain++;
	}

	/** One of a raid's band got away with this many things. */
	public synchronized void stolen( String homeKey, long id, int things ){
		Event e = event( homeKey, id );
		if (e != null) e.stolen += things;
	}

	// ----------------------------------------------------------------- the bundle

	private static final String KEYS = "threat_keys", NOTICE = "threat_notice_", ROLLED = "threat_rolled",
			EVENTS = "threat_events_", CHESTS = "threat_chests_", UNTOLD = "threat_untold_", ROBBED = "threat_robbed";

	@Override
	public synchronized void storeInBundle( Bundle bundle ){
		//a record of a home that is gone goes with it
		records.keySet().removeIf( k -> HomeThreats.home( k ) == null );
		String[] keys = records.keySet().toArray( new String[0] );
		int[] rolled = new int[keys.length];
		boolean[] robbed = new boolean[keys.length];
		for (int i = 0; i < keys.length; i++){
			Record r = records.get( keys[i] );
			rolled[i] = r.rolled;
			robbed[i] = r.robbed;
			if (r.notice != null) bundle.put( NOTICE + i, r.notice );
			if (!r.untold.isEmpty()) bundle.put( UNTOLD + i, r.untold.toArray( new String[0] ) );
			int[] ev = new int[r.events.size() * 5];
			long[] ids = new long[r.events.size()];
			long[] chests = new long[r.events.size()];
			for (int k = 0; k < r.events.size(); k++){
				Event e = r.events.get( k );
				ev[k * 5] = e.kind;
				ev[k * 5 + 1] = e.night;
				ev[k * 5 + 2] = e.came;
				ev[k * 5 + 3] = e.slain;
				ev[k * 5 + 4] = e.stolen;
				ids[k] = e.id;
				chests[k] = e.chest;
			}
			bundle.put( EVENTS + i, ev );
			bundle.put( EVENTS + "ids_" + i, ids );
			bundle.put( CHESTS + i, chests );
		}
		bundle.put( KEYS, keys );
		bundle.put( ROLLED, rolled );
		bundle.put( ROBBED, robbed );
	}

	@Override
	public synchronized void restoreFromBundle( Bundle bundle ){
		records.clear();
		if (!bundle.contains( KEYS )) return;
		String[] keys = bundle.getStringArray( KEYS );
		int[] rolled = bundle.getIntArray( ROLLED );
		boolean[] robbed = bundle.contains( ROBBED ) ? bundle.getBooleanArray( ROBBED ) : null;
		for (int i = 0; i < keys.length; i++){
			Record r = new Record();
			if (rolled != null && i < rolled.length) r.rolled = rolled[i];
			if (robbed != null && i < robbed.length) r.robbed = robbed[i];
			if (bundle.contains( NOTICE + i )) r.notice = bundle.getIntArray( NOTICE + i );
			if (bundle.contains( UNTOLD + i )) r.untold.addAll( Arrays.asList( bundle.getStringArray( UNTOLD + i ) ) );
			if (bundle.contains( EVENTS + i ) && bundle.contains( EVENTS + "ids_" + i )){
				int[] ev = bundle.getIntArray( EVENTS + i );
				long[] ids = bundle.getLongArray( EVENTS + "ids_" + i );
				long[] chests = bundle.contains( CHESTS + i ) ? bundle.getLongArray( CHESTS + i ) : null;
				for (int k = 0; k < ids.length && k * 5 + 4 < ev.length; k++){
					Event e = new Event( ev[k * 5], ev[k * 5 + 1], ids[k] );
					e.came = ev[k * 5 + 2];
					e.slain = ev[k * 5 + 3];
					e.stolen = ev[k * 5 + 4];
					if (chests != null && k < chests.length) e.chest = chests[k];
					r.events.add( e );
				}
			}
			records.put( keys[i], r );
		}
	}
}

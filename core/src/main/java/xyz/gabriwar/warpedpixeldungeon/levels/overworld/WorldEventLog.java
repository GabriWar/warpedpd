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

import com.watabou.utils.Bundle;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * What the players have done about the world's timed events (WorldEvents), the only part of
 * them a save holds: placement is a pure function, so the log keeps the events heard of, the
 * ones under way on the ground (a hunt's pack loosed), the ones settled for good (a star's
 * fragment picked up, a market chased off), where each star's fragment was laid, and the events
 * a debug scene forced. Each entry carries the turn it can
 * be forgotten at (prune), so the log never grows past the events still on.
 *
 * It also keeps the events' clock: the furthest world turn they have been seen at. A real-clock
 * run's wall clock can run back (a time zone crossed, the device's clock turned back), and an
 * event forgotten as over must never come round again with its loot and its stock - so the
 * events read the world's turn through clock(), which never goes back.
 *
 * The heard-of and settled maps and the forced list are read by the world map on the render
 * thread, and are what a co-op host ships to his guests' maps (storeShared), so they are
 * concurrent; everything else is the actor thread's.
 */
public class WorldEventLog {

	//event id -> the turn it is over: heard of (announced once, pinned on the map)
	public final ConcurrentHashMap<Long, Integer> announced = new ConcurrentHashMap<>();
	//event id -> the turn it may be forgotten: settled for good (looted, finished, failed)
	public final ConcurrentHashMap<Long, Integer> resolved = new ConcurrentHashMap<>();
	//event id -> the turn it is over: under way on the ground (a hunt's pack loosed). A hunt is
	//heard of from afar before it begins, and only one that began can be over for want of beasts
	public final HashMap<Long, Integer> begun = new HashMap<>();
	//star id -> world key of the heap its fragment was laid in, and the turn the fragment is taken back
	public final HashMap<Long, Long> lootAt = new HashMap<>();
	public final HashMap<Long, Integer> lootEnds = new HashMap<>();
	//the events debug scenes forced (WorldEvents.forcedStar, forcedMarket): read by eventsNear like any other
	public final CopyOnWriteArrayList<WorldEvents.Event> forced = new CopyOnWriteArrayList<>();
	//bumped whenever the forced list changes, so a cached enumeration knows to look again
	private volatile int forcedVersion;
	//the furthest world turn the events have been seen at (Integer.MIN_VALUE: none yet)
	private volatile int clock = Integer.MIN_VALUE;

	/** The events' turn: the world's (WorldClock.turn), but never behind the furthest it has
	 *  been, so nothing over comes round again. Actor thread: it moves the mark on. */
	public int clock( int worldTurn ){
		if (worldTurn > clock) clock = worldTurn;
		return clock;
	}

	/** The same for a reader off the actor thread (the world map): the mark is left as it is. */
	public int peekClock( int worldTurn ){
		return Math.max( worldTurn, clock );
	}

	public int forcedVersion(){
		return forcedVersion;
	}

	public void force( WorldEvents.Event e ){
		forced.add( e );
		forcedVersion++;
	}

	/** Forgets everything whose time is past: nothing can enumerate it again. A forced star stays
	 *  as long as its crater does. */
	public void prune( int turn ){
		announced.values().removeIf( end -> end <= turn );
		resolved.values().removeIf( end -> end <= turn );
		begun.values().removeIf( end -> end <= turn );
		if (forced.removeIf( e -> e.lingersUntil() <= turn )) forcedVersion++;
	}

	private static final String ANNOUNCED_IDS  = "event_announced_ids";
	private static final String ANNOUNCED_ENDS = "event_announced_ends";
	private static final String RESOLVED_IDS   = "event_resolved_ids";
	private static final String RESOLVED_ENDS  = "event_resolved_ends";
	private static final String LOOT_IDS       = "event_loot_ids";
	private static final String LOOT_KEYS      = "event_loot_keys";
	private static final String LOOT_ENDS      = "event_loot_ends";
	private static final String FORCED_IDS     = "event_forced_ids";
	private static final String FORCED_TYPES   = "event_forced_types";
	private static final String FORCED_DATA    = "event_forced_data";
	private static final String CLOCK          = "event_clock";
	private static final String BEGUN_IDS      = "event_begun_ids";
	private static final String BEGUN_ENDS     = "event_begun_ends";
	//sx, sy, startDay, startTurn, endTurn, wx, wy
	private static final int FORCED_INTS = 7;

	public void storeInBundle( Bundle bundle ){
		storeShared( bundle );
		bundle.put( CLOCK, clock );

		long[] lootIds = new long[lootAt.size()], lootKeys = new long[lootAt.size()];
		int[] lootEnd = new int[lootAt.size()];
		int i = 0;
		for (HashMap.Entry<Long, Long> e : lootAt.entrySet()){
			Integer end = lootEnds.get( e.getKey() );
			lootIds[i] = e.getKey();
			lootKeys[i] = e.getValue();
			lootEnd[i] = end != null ? end : 0;
			i++;
		}
		bundle.put( LOOT_IDS, lootIds );
		bundle.put( LOOT_KEYS, lootKeys );
		bundle.put( LOOT_ENDS, lootEnd );

		long[] begunIds = new long[begun.size()];
		int[] begunEnds = new int[begun.size()];
		i = 0;
		for (HashMap.Entry<Long, Integer> e : begun.entrySet()){
			begunIds[i] = e.getKey();
			begunEnds[i] = e.getValue();
			i++;
		}
		bundle.put( BEGUN_IDS, begunIds );
		bundle.put( BEGUN_ENDS, begunEnds );
	}

	/** What a co-op guest's world map is built from (OverworldLevel.mapEvents): the events heard
	 *  of, the settled and the forced - the concurrent part, so any thread may take it (a host's
	 *  network thread writes a joiner's first state). */
	public void storeShared( Bundle bundle ){
		storeMap( bundle, announced, ANNOUNCED_IDS, ANNOUNCED_ENDS );
		storeMap( bundle, resolved, RESOLVED_IDS, RESOLVED_ENDS );

		WorldEvents.Event[] all = forced.toArray( new WorldEvents.Event[0] );
		long[] ids = new long[all.length];
		String[] types = new String[all.length];
		int[] data = new int[all.length * FORCED_INTS];
		for (int i = 0; i < all.length; i++){
			WorldEvents.Event e = all[i];
			ids[i] = e.id;
			types[i] = e.type.name();
			int o = i * FORCED_INTS;
			data[o] = e.sx;
			data[o + 1] = e.sy;
			data[o + 2] = e.startDay;
			data[o + 3] = e.startTurn;
			data[o + 4] = e.endTurn;
			data[o + 5] = e.wx;
			data[o + 6] = e.wy;
		}
		bundle.put( FORCED_IDS, ids );
		bundle.put( FORCED_TYPES, types );
		bundle.put( FORCED_DATA, data );
	}

	/** Reads what a save holds; a save from before the events, or with a group half there, leaves
	 *  that group empty (a missing array read would report an error, so every key is asked first). */
	public void restoreFromBundle( Bundle bundle ){
		restoreShared( bundle );
		clock = bundle.contains( CLOCK ) ? bundle.getInt( CLOCK ) : Integer.MIN_VALUE;
		lootAt.clear();
		lootEnds.clear();

		if (bundle.contains( LOOT_IDS ) && bundle.contains( LOOT_KEYS ) && bundle.contains( LOOT_ENDS )){
			long[] ids = bundle.getLongArray( LOOT_IDS ), keys = bundle.getLongArray( LOOT_KEYS );
			int[] ends = bundle.getIntArray( LOOT_ENDS );
			int n = Math.min( ids.length, Math.min( keys.length, ends.length ) );
			for (int i = 0; i < n; i++){
				lootAt.put( ids[i], keys[i] );
				lootEnds.put( ids[i], ends[i] );
			}
		}

		begun.clear();
		if (bundle.contains( BEGUN_IDS ) && bundle.contains( BEGUN_ENDS )){
			long[] ids = bundle.getLongArray( BEGUN_IDS );
			int[] ends = bundle.getIntArray( BEGUN_ENDS );
			int n = Math.min( ids.length, ends.length );
			for (int i = 0; i < n; i++) begun.put( ids[i], ends[i] );
		} else {
			//a save from before hunts were heard of from afar: a hunt was heard of only as it began
			begun.putAll( announced );
		}
	}

	/** Takes the shared part over whole (storeShared): a save's, or on a co-op guest's mirror the
	 *  host's as shipped. A group missing, or half there, is left empty. */
	public void restoreShared( Bundle bundle ){
		announced.clear();
		resolved.clear();
		forced.clear();
		forcedVersion++;

		restoreMap( bundle, announced, ANNOUNCED_IDS, ANNOUNCED_ENDS );
		restoreMap( bundle, resolved, RESOLVED_IDS, RESOLVED_ENDS );

		if (bundle.contains( FORCED_IDS ) && bundle.contains( FORCED_TYPES ) && bundle.contains( FORCED_DATA )){
			long[] ids = bundle.getLongArray( FORCED_IDS );
			String[] types = bundle.getStringArray( FORCED_TYPES );
			int[] data = bundle.getIntArray( FORCED_DATA );
			int n = Math.min( ids.length, Math.min( types.length, data.length / FORCED_INTS ) );
			for (int i = 0; i < n; i++){
				WorldEvents.Type type = null;
				for (WorldEvents.Type t : WorldEvents.Type.values()){
					if (t.name().equals( types[i] )) type = t;
				}
				//a kind this build no longer knows is dropped
				if (type == null) continue;
				int o = i * FORCED_INTS;
				forced.add( new WorldEvents.Event( type, data[o], data[o + 1], data[o + 2], data[o + 3], data[o + 4],
						data[o + 5], data[o + 6], ids[i] ) );
			}
		}
	}

	/** A fingerprint of the shared part: a co-op host ships it again only when this changed. */
	public long sharedSig(){
		long sig = forced.size();
		for (java.util.Map.Entry<Long, Integer> e : announced.entrySet()) sig += WorldEvents.mix( e.getKey(), e.getValue(), 1 );
		for (java.util.Map.Entry<Long, Integer> e : resolved.entrySet()) sig += WorldEvents.mix( e.getKey(), e.getValue(), 2 );
		for (WorldEvents.Event e : forced) sig += WorldEvents.mix( e.id, e.startTurn, 3 );
		return sig;
	}

	private static void storeMap( Bundle bundle, ConcurrentHashMap<Long, Integer> map, String idsKey, String endsKey ){
		HashMap<Long, Integer> copy = new HashMap<>( map );
		long[] ids = new long[copy.size()];
		int[] ends = new int[copy.size()];
		int i = 0;
		for (HashMap.Entry<Long, Integer> e : copy.entrySet()){
			ids[i] = e.getKey();
			ends[i] = e.getValue();
			i++;
		}
		bundle.put( idsKey, ids );
		bundle.put( endsKey, ends );
	}

	private static void restoreMap( Bundle bundle, ConcurrentHashMap<Long, Integer> map, String idsKey, String endsKey ){
		if (!bundle.contains( idsKey ) || !bundle.contains( endsKey )) return;
		long[] ids = bundle.getLongArray( idsKey );
		int[] ends = bundle.getIntArray( endsKey );
		int n = Math.min( ids.length, ends.length );
		for (int i = 0; i < n; i++) map.put( ids[i], ends[i] );
	}
}

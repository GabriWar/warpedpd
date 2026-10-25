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

package xyz.gabriwar.warpedpixeldungeon.bounty;

import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.WorldSection;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * The villages' bounties for the whole run (a section of HomesteadWorld, saved with the game): the
 * notices the hero has TAKEN and not yet finished (at most Bounties.MAX_TAKEN), the notices CLOSED on
 * their boards - taken, done or failed - each with the day its period ends, after which it is
 * forgotten, and the hero's STANDING in each village: the bounties he has finished for it. The offers
 * themselves are a pure function of the world (Bounties.offers) and are never saved.
 *
 * Written by the host (the window's choices, a kill, the turn's notices) and read on the render thread
 * (the window, the journal, the boards' looks), so every method is synchronized; the notices handed out
 * are the live ones, read and written only through here. A co-op guest reads the host's copy
 * (BountyShare). Old saves have no section and start with nothing taken and no standing anywhere.
 */
public class BountyWorld extends WorldSection {

	private final ArrayList<Bounty> taken = new ArrayList<>();
	//offer id -> the first day it may be forgotten (its period's end)
	private final HashMap<Long, Integer> closed = new HashMap<>();
	//village key -> bounties finished for it
	private final HashMap<Long, Integer> standing = new HashMap<>();
	//bumped by every change a guest's copy or a board's look has to follow
	private int version;

	public BountyWorld(){
	}

	/** This run's section (null only outside a run's world, which a test may be). */
	public static BountyWorld get(){
		return HomesteadWorld.section( BountyWorld.class );
	}

	/** Changes whenever anything here does. */
	public synchronized int version(){
		return version;
	}

	/** The notices taken, oldest first: the live ones. */
	public synchronized List<Bounty> taken(){
		return Collections.unmodifiableList( new ArrayList<>( taken ) );
	}

	public synchronized int takenCount(){
		return taken.size();
	}

	/** Is any HUNT under way (Bounties.onKill asks first)? */
	public synchronized boolean hunting(){
		for (Bounty b : taken) if (b.kind == Bounty.Kind.HUNT) return true;
		return false;
	}

	/** The taken notice of an offer id, or null. */
	public synchronized Bounty takenById( long id ){
		for (Bounty b : taken) if (b.id == id) return b;
		return null;
	}

	/** Is this offer off its board for now: taken, done or failed in its period? */
	public synchronized boolean isClosed( long id ){
		return closed.containsKey( id );
	}

	/** The hero takes a notice down: off its board until `forgetDay`. */
	public synchronized void take( Bounty b, int forgetDay ){
		taken.add( b );
		closed.put( b.id, forgetDay );
		version++;
	}

	/** A notice is done with: done (its village's standing rises by one when `credit`) or failed. It
	 *  stays off its board for the rest of its period. */
	public synchronized void finish( Bounty b, boolean credit ){
		if (!taken.remove( b )) return;
		if (credit) standing.merge( b.village, 1, Integer::sum );
		version++;
	}

	/** A notice given up: it goes back up on its board if its period is still on. */
	public synchronized void abandon( Bounty b ){
		if (!taken.remove( b )) return;
		closed.remove( b.id );
		version++;
	}

	/** HUNT: one more kill, never past what it asks. True when it counted. */
	public synchronized boolean count( Bounty b ){
		if (!taken.contains( b ) || b.progress >= b.need) return false;
		b.progress++;
		version++;
		return true;
	}

	/** A notice's log line said (Bounty.told). */
	public synchronized void tell( Bounty b, int bit, boolean on ){
		int was = b.told;
		b.told = on ? b.told | bit : b.told & ~bit;
		if (was != b.told) version++;
	}

	/** DELIVER: its deadline has passed. */
	public synchronized void lateNow( Bounty b ){
		if (b.late) return;
		b.late = true;
		version++;
	}

	/** Bounties finished for a village. */
	public synchronized int points( long village ){
		Integer p = standing.get( village );
		return p == null ? 0 : p;
	}

	/** Forgets every closed offer whose period is over by `day`. */
	public synchronized void prune( int day ){
		if (closed.values().removeIf( d -> d <= day )) version++;
	}

	/** Debug: a village's standing set outright. */
	public synchronized void setPoints( long village, int points ){
		if (points <= 0) standing.remove( village );
		else standing.put( village, points );
		version++;
	}

	private static final String TAKEN = "taken", CLOSED_IDS = "closed_ids", CLOSED_DAYS = "closed_days",
			STANDING_KEYS = "standing_keys", STANDING_POINTS = "standing_points", VERSION = "version";

	@Override
	public synchronized void storeInBundle( Bundle bundle ){
		bundle.put( TAKEN, taken );
		long[] ids = new long[closed.size()];
		int[] days = new int[closed.size()];
		int i = 0;
		for (HashMap.Entry<Long, Integer> e : closed.entrySet()){
			ids[i] = e.getKey();
			days[i] = e.getValue();
			i++;
		}
		bundle.put( CLOSED_IDS, ids );
		bundle.put( CLOSED_DAYS, days );
		long[] keys = new long[standing.size()];
		int[] points = new int[standing.size()];
		i = 0;
		for (HashMap.Entry<Long, Integer> e : standing.entrySet()){
			keys[i] = e.getKey();
			points[i] = e.getValue();
			i++;
		}
		bundle.put( STANDING_KEYS, keys );
		bundle.put( STANDING_POINTS, points );
		bundle.put( VERSION, version );
	}

	@Override
	public synchronized void restoreFromBundle( Bundle bundle ){
		taken.clear();
		closed.clear();
		standing.clear();
		if (bundle.contains( TAKEN )){
			for (Bundlable b : bundle.getCollection( TAKEN )){
				//a notice of a kind, prey or good this build does not know is dropped
				if (b instanceof Bounty && Bounties.known( (Bounty) b )) taken.add( (Bounty) b );
			}
		}
		if (bundle.contains( CLOSED_IDS ) && bundle.contains( CLOSED_DAYS )){
			long[] ids = bundle.getLongArray( CLOSED_IDS );
			int[] days = bundle.getIntArray( CLOSED_DAYS );
			for (int i = 0; i < Math.min( ids.length, days.length ); i++) closed.put( ids[i], days[i] );
		}
		if (bundle.contains( STANDING_KEYS ) && bundle.contains( STANDING_POINTS )){
			long[] keys = bundle.getLongArray( STANDING_KEYS );
			int[] points = bundle.getIntArray( STANDING_POINTS );
			for (int i = 0; i < Math.min( keys.length, points.length ); i++) standing.put( keys[i], points[i] );
		}
		//a guest's copy changes as a whole: its version moves on past the host's last
		version = bundle.getInt( VERSION ) + 1;
	}
}

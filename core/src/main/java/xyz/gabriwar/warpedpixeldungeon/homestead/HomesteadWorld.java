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

package xyz.gabriwar.warpedpixeldungeon.homestead;

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The homesteads' run-wide data, across every slice of the world, saved with the game
 * (Dungeon.saveGame / loadGame, reset for a new run): the HOMES - every hearthstone standing, on
 * which slice and world cell, its name, when it was last visited and its ledger of what happened
 * there while the hero was away (AwaySettlers) - and the sections the
 * homesteads' parts keep for the whole run (WorldSection). A home anchors the waystones' travel,
 * the threat on it and what happened while the hero was away.
 */
public final class HomesteadWorld {

	/** A hearthstone standing in the world. */
	public static final class Home implements Bundlable {
		/** The slice it stands on (WorldLayers.depthOf of its altitude). */
		public int depth;
		public int wx, wy;
		public String name;
		/** WorldClock.day() the hero was last there. */
		public int lastVisitDay;
		/** WorldClock.turn() the hero was last there (AwaySettlers: a full day away is a whole
		 *  DayNightCycle.FULL_CYCLE of the world's turns). */
		public int lastVisitTurn;

		/** What happened there while the hero was away (AwaySettlers), oldest first, at most
		 *  LEDGER_MAX entries. */
		private final ArrayList<Entry> ledger = new ArrayList<>();

		public Home(){
		}

		public Home( int depth, int wx, int wy, String name, int lastVisitDay ){
			this.depth = depth;
			this.wx = wx;
			this.wy = wy;
			this.name = name;
			this.lastVisitDay = lastVisitDay;
			//the dawn of that day: a visit that day can only have been later
			this.lastVisitTurn = lastVisitDay * DayNightCycle.FULL_CYCLE;
		}

		public boolean at( int depth, int wx, int wy ){
			return this.depth == depth && this.wx == wx && this.wy == wy;
		}

		/** Its name, or the plain name of a home when it was given none. */
		public String title(){
			return name == null || name.isEmpty() ? Messages.get( HomesteadWorld.class, "home" ) : name;
		}

		/** The hero is there now (AwaySettlers.visit). */
		public void visited(){
			lastVisitDay = WorldClock.day();
			lastVisitTurn = WorldClock.turn();
		}

		/** Writes a line in the ledger for a day; the oldest goes when it is full. */
		public synchronized void note( int day, String text ){
			ledger.add( new Entry( day, text ) );
			while (ledger.size() > LEDGER_MAX) ledger.remove( 0 );
		}

		/** The ledger, oldest first. */
		public synchronized List<Entry> ledger(){
			return Collections.unmodifiableList( new ArrayList<>( ledger ) );
		}

		private static final String DEPTH = "depth", WX = "wx", WY = "wy", NAME = "name", VISIT = "visit",
				VISIT_TURN = "visit_turn", LEDGER_DAYS = "ledger_days", LEDGER_TEXT = "ledger_text";

		@Override
		public synchronized void storeInBundle( Bundle bundle ){
			bundle.put( DEPTH, depth );
			bundle.put( WX, wx );
			bundle.put( WY, wy );
			bundle.put( NAME, name == null ? "" : name );
			bundle.put( VISIT, lastVisitDay );
			bundle.put( VISIT_TURN, lastVisitTurn );
			int[] days = new int[ledger.size()];
			String[] text = new String[ledger.size()];
			for (int i = 0; i < days.length; i++){
				days[i] = ledger.get( i ).day;
				text[i] = ledger.get( i ).text;
			}
			bundle.put( LEDGER_DAYS, days );
			bundle.put( LEDGER_TEXT, text );
		}

		@Override
		public synchronized void restoreFromBundle( Bundle bundle ){
			depth = bundle.getInt( DEPTH );
			wx = bundle.getInt( WX );
			wy = bundle.getInt( WY );
			name = bundle.getString( NAME );
			lastVisitDay = bundle.getInt( VISIT );
			lastVisitTurn = bundle.contains( VISIT_TURN ) ? bundle.getInt( VISIT_TURN ) : lastVisitDay * DayNightCycle.FULL_CYCLE;
			ledger.clear();
			if (bundle.contains( LEDGER_DAYS ) && bundle.contains( LEDGER_TEXT )){
				int[] days = bundle.getIntArray( LEDGER_DAYS );
				String[] text = bundle.getStringArray( LEDGER_TEXT );
				for (int i = 0; i < Math.min( days.length, text.length ); i++) ledger.add( new Entry( days[i], text[i] ) );
			}
		}
	}

	/** A line of a home's ledger: the day it was settled (WorldClock.day) and what happened. */
	public static final class Entry {
		public final int day;
		public final String text;

		public Entry( int day, String text ){
			this.day = day;
			this.text = text;
		}
	}

	/** The most lines a home's ledger keeps. */
	public static final int LEDGER_MAX = 40;

	private static final String SECTION_PKG = "xyz.gabriwar.warpedpixeldungeon.homestead.";

	/** The run-wide sections, by class name (see WorldSection); a missing class is skipped. */
	public static final String[] SECTIONS = {
			SECTION_PKG + "gathering.GatheringWorld",
			SECTION_PKG + "threats.ThreatWorld",     //the raids' ledgers, what was settled on return
			SECTION_PKG + "travel.TravelWorld",      //the waystones attuned
			"xyz.gabriwar.warpedpixeldungeon.items.catching.CatchWorld",  //the bug net's catches today, the bottles' caches
			SECTION_PKG + "mounts.StableWorld",      //the villages' stables set up
			"xyz.gabriwar.warpedpixeldungeon.bounty.BountyWorld"   //the villages' notices taken, the standing earned
	};

	private static final ArrayList<Home> homes = new ArrayList<>();
	//bumped whenever a home goes up or comes down (HomeAnchors keeps its answers by it)
	private static int version;
	private static final LinkedHashMap<String, WorldSection> sections = new LinkedHashMap<>();

	static {
		reset();
	}

	private HomesteadWorld(){}

	/** A new run: no homes, every section fresh. */
	public static synchronized void reset(){
		homes.clear();
		version++;
		sections.clear();
		for (String name : SECTIONS){
			Class<?> cls;
			try {
				cls = Class.forName( name );
			} catch (ClassNotFoundException e){
				continue;
			}
			try {
				sections.put( name, (WorldSection) cls.getConstructor().newInstance() );
			} catch (ReflectiveOperationException | ClassCastException e){
				throw new IllegalStateException( "homestead world section " + name + " is not a public WorldSection", e );
			}
		}
	}

	/** The run's section of a class, or null when that part of the homesteads is not built. */
	public static synchronized <T extends WorldSection> T section( Class<T> cls ){
		return cls.cast( sections.get( cls.getName() ) );
	}

	public static synchronized Collection<WorldSection> sections(){
		return Collections.unmodifiableCollection( new ArrayList<>( sections.values() ) );
	}

	/** Changes whenever a home goes up or comes down. */
	public static synchronized int version(){
		return version;
	}

	/** Every home, oldest first. */
	public static synchronized List<Home> homes(){
		return Collections.unmodifiableList( new ArrayList<>( homes ) );
	}

	/** The home on that slice's world cell, or null. */
	public static synchronized Home homeAt( int depth, int wx, int wy ){
		for (Home h : homes) if (h.at( depth, wx, wy )) return h;
		return null;
	}

	/** A hearthstone went up; a second one on the same cell replaces the first. */
	public static synchronized void addHome( Home home ){
		removeHome( home.depth, home.wx, home.wy );
		homes.add( home );
		version++;
	}

	/** A hearthstone came down: true when there was one. */
	public static synchronized boolean removeHome( int depth, int wx, int wy ){
		for (int i = 0; i < homes.size(); i++){
			if (homes.get( i ).at( depth, wx, wy )){
				homes.remove( i );
				version++;
				return true;
			}
		}
		return false;
	}

	private static final String HOMES = "homestead_homes";
	private static final String SECTION_BUNDLE = "homestead_world";

	public static synchronized void storeInBundle( Bundle bundle ){
		bundle.put( HOMES, homes );
		Bundle s = new Bundle();
		for (WorldSection section : sections.values()){
			Bundle b = new Bundle();
			section.storeInBundle( b );
			s.put( section.getClass().getName(), b );
		}
		bundle.put( SECTION_BUNDLE, s );
	}

	/** Restores a run's homes and sections; a save from before the homesteads has none. */
	public static synchronized void restoreFromBundle( Bundle bundle ){
		reset();
		if (bundle.contains( HOMES )){
			for (Bundlable b : bundle.getCollection( HOMES )) homes.add( (Home) b );
		}
		if (!bundle.contains( SECTION_BUNDLE )) return;
		Bundle s = bundle.getBundle( SECTION_BUNDLE );
		for (WorldSection section : sections.values()){
			String name = section.getClass().getName();
			if (s.contains( name )) section.restoreFromBundle( s.getBundle( name ) );
		}
	}
}

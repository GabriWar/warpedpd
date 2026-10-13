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

package xyz.gabriwar.warpedpixeldungeon.journal;

import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;

import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

//Global record of Descent Guide pages the player has picked up off the
//dungeon floor, across all runs. Like the rest of the journal: knowledge
//must be found, page by page. Pages are kept in the order they were found
//(the newest is the one the guide opens on), and the guide remembers which
//of them have been read, so a fresh page can stand out until it is.
public class GuideProgress {

	private static final String PAGES = "guide_pages";
	private static final String READ  = "guide_read";
	private static final String DESCENDED = "guide_descended";
	private static final String DUNGEON_TURNS = "guide_dungeon_turns";

	private static final LinkedHashSet<String> foundPages = new LinkedHashSet<>();
	private static final HashSet<String> readPages = new HashSet<>();
	//whether a hero has ever set foot in the dungeon: before that (the tutorial in the town above)
	//no page but the intro's writes itself, so the guide does not start on the world above
	private static boolean descended = false;
	//turns spent in the dungeon, up to a full day (GuideGraph.onHeroTurn)
	private static int dungeonTurns = 0;

	/** The tome of dungeon mastery's pages (their names in old journals and on old torn pages) and the
	 *  guide pages they became. */
	public static final LinkedHashMap<String, String> LEGACY_TOME = new LinkedHashMap<>();
	static {
		LEGACY_TOME.put( "Intro",            GuideGraph.BASICS_INTRO );
		LEGACY_TOME.put( "Examining",        GuideGraph.BASICS_EXAMINING );
		LEGACY_TOME.put( "Surprise_Attacks", GuideGraph.BASICS_SURPRISE );
		LEGACY_TOME.put( "Identifying",      GuideGraph.BASICS_IDENTIFYING );
		LEGACY_TOME.put( "Food",             GuideGraph.BASICS_HUNGER );
		LEGACY_TOME.put( "Alchemy",          GuideGraph.BASICS_ALCHEMY );
		LEGACY_TOME.put( "Enchanting",       GuideGraph.BASICS_ENCHANTING );
		LEGACY_TOME.put( "Farming",          GuideGraph.BASICS_FARMING );
		LEGACY_TOME.put( "Skills",           GuideGraph.BASICS_SKILLS );
		LEGACY_TOME.put( "Dieing",           GuideGraph.BASICS_DEFEAT );
		LEGACY_TOME.put( "Searching",        GuideGraph.BASICS_SEARCHING );
		LEGACY_TOME.put( "Strength",         GuideGraph.BASICS_STRENGTH );
		LEGACY_TOME.put( "Upgrades",         GuideGraph.BASICS_UPGRADES );
		LEGACY_TOME.put( "Looting",          GuideGraph.BASICS_LOOTING );
		LEGACY_TOME.put( "Levelling",        GuideGraph.BASICS_LEVELLING );
		LEGACY_TOME.put( "Positioning",      GuideGraph.BASICS_POSITIONING );
		LEGACY_TOME.put( "Magic",            GuideGraph.BASICS_MAGIC );
	}

	//where the tome kept its pages in the journal file (Document's bundle), and its page states
	private static final String DOCUMENTS = "documents";
	private static final String TOME = "ADVENTURERS_GUIDE";
	private static final int TOME_FOUND = 1, TOME_READ = 2;

	/** Records a page as found. True when it was not found before. */
	public static boolean findPage( String key ){
		//pages write themselves wherever a game runs, and a spectator goes from the lobby
		//straight into one: the journal is read from disk first, or the next save would put
		//this page alone in place of everything the journal held
		Journal.loadGlobal();
		if (!descended && !GuideGraph.BASICS_INTRO.equals( key )) return false;
		if (foundPages.add( key )){
			Journal.saveNeeded = true;
			return true;
		}
		return false;
	}

	/** A hero has set foot in the dungeon: from now on pages write themselves. */
	public static void descend(){
		//as findPage: the journal on disk first, or loading it later would undo this
		Journal.loadGlobal();
		if (!descended){
			descended = true;
			Journal.saveNeeded = true;
		}
	}

	public static boolean descended(){
		return descended;
	}

	/** A turn spent in the dungeon. True once: on the turn they add up to a full day. */
	public static boolean dungeonTurn(){
		if (dungeonTurns >= DayNightCycle.FULL_CYCLE) return false;
		dungeonTurns++;
		if (dungeonTurns % 100 == 0) Journal.saveNeeded = true;
		return dungeonTurns >= DayNightCycle.FULL_CYCLE;
	}

	public static boolean pageFound( String key ){
		return foundPages.contains( key );
	}

	public static void markRead( String key ){
		if (key != null && readPages.add( key )){
			Journal.saveNeeded = true;
		}
	}

	public static boolean isRead( String key ){
		return readPages.contains( key );
	}

	/** The most recently found page that has not been read yet, or null. */
	public static String newestUnread(){
		String newest = null;
		for (String key : foundPages){
			if (!readPages.contains( key )) newest = key;
		}
		return newest;
	}

	public static int foundCount(){
		return foundPages.size();
	}

	/** The page is neither found nor read any more (the tutorial's pages, laid out again). */
	public static void forget( String key ){
		if (foundPages.remove( key ) | readPages.remove( key )){
			Journal.saveNeeded = true;
		}
	}

	//debug: forget every found page
	public static void clear(){
		if (!foundPages.isEmpty() || !readPages.isEmpty() || descended){
			foundPages.clear();
			readPages.clear();
			descended = false;
			dungeonTurns = 0;
			Journal.saveNeeded = true;
		}
	}

	public static void store( Bundle bundle ){
		bundle.put( PAGES, foundPages.toArray( new String[0] ) );
		bundle.put( READ, readPages.toArray( new String[0] ) );
		bundle.put( DESCENDED, descended );
		bundle.put( DUNGEON_TURNS, dungeonTurns );
	}

	public static void restore( Bundle bundle ){
		foundPages.clear();
		readPages.clear();
		if (bundle.contains( PAGES )){
			for (String key : bundle.getStringArray( PAGES )){
				foundPages.add( key );
			}
		}
		if (bundle.contains( READ )){
			for (String key : bundle.getStringArray( READ )){
				readPages.add( key );
			}
		} else {
			//a journal from before pages were marked read: everything already found counts
			//as read, so an old player's guide doesn't light up like a fresh one
			readPages.addAll( foundPages );
		}
		restoreTome( bundle );
		//a journal from before this was kept: any page past the intro's means a hero went down
		descended = bundle.getBoolean( DESCENDED );
		HashSet<String> known = new HashSet<>( foundPages );
		known.addAll( readPages );
		known.remove( GuideGraph.BASICS_INTRO );
		if (!known.isEmpty()) descended = true;
		dungeonTurns = bundle.getInt( DUNGEON_TURNS );
	}

	//a journal from before the tome of dungeon mastery became the guide's first chapter: what was
	//found or read in it is found or read here. Its pages go in before the guide's own, so none of
	//them becomes the newest; Document no longer writes the tome, so this runs once
	private static void restoreTome( Bundle bundle ){
		if (!bundle.contains( DOCUMENTS )) return;
		Bundle docs = bundle.getBundle( DOCUMENTS );
		if (!docs.contains( TOME )) return;
		Bundle pages = docs.getBundle( TOME );
		ArrayList<String> found = new ArrayList<>();
		for (String old : LEGACY_TOME.keySet()){
			if (!pages.contains( old )) continue;
			int state = pages.getInt( old );
			String key = LEGACY_TOME.get( old );
			if (state >= TOME_FOUND) found.add( key );
			if (state >= TOME_READ) readPages.add( key );
		}
		if (found.isEmpty()) return;
		for (String key : foundPages){
			if (!found.contains( key )) found.add( key );
		}
		foundPages.clear();
		foundPages.addAll( found );
		Journal.saveNeeded = true;
	}
}

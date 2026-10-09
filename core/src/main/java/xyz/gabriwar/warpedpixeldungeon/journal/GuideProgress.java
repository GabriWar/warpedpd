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

import com.watabou.utils.Bundle;

import java.util.HashSet;
import java.util.LinkedHashSet;

//Global record of Descent Guide pages the player has picked up off the
//dungeon floor, across all runs. Like the rest of the journal: knowledge
//must be found, page by page. Pages are kept in the order they were found
//(the newest is the one the guide opens on), and the guide remembers which
//of them have been read, so a fresh page can stand out until it is.
public class GuideProgress {

	private static final String PAGES = "guide_pages";
	private static final String READ  = "guide_read";

	private static final LinkedHashSet<String> foundPages = new LinkedHashSet<>();
	private static final HashSet<String> readPages = new HashSet<>();

	/** Records a page as found. True when it was not found before. */
	public static boolean findPage( String key ){
		//pages write themselves wherever a game runs, and a spectator goes from the lobby
		//straight into one: the journal is read from disk first, or the next save would put
		//this page alone in place of everything the journal held
		Journal.loadGlobal();
		if (foundPages.add( key )){
			Journal.saveNeeded = true;
			return true;
		}
		return false;
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

	//debug: forget every found page
	public static void clear(){
		if (!foundPages.isEmpty() || !readPages.isEmpty()){
			foundPages.clear();
			readPages.clear();
			Journal.saveNeeded = true;
		}
	}

	public static void store( Bundle bundle ){
		bundle.put( PAGES, foundPages.toArray( new String[0] ) );
		bundle.put( READ, readPages.toArray( new String[0] ) );
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
	}
}

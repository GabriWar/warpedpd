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

//Global record of Descent Guide pages the player has picked up off the
//dungeon floor, across all runs. Like the rest of the journal: knowledge
//must be found, page by page.
public class GuideProgress {

	private static final String PAGES = "guide_pages";

	private static final HashSet<String> foundPages = new HashSet<>();

	public static void findPage( String key ){
		if (foundPages.add( key )){
			Journal.saveNeeded = true;
		}
	}

	public static boolean pageFound( String key ){
		return foundPages.contains( key );
	}

	//debug: forget every found page
	public static void clear(){
		if (!foundPages.isEmpty()){
			foundPages.clear();
			Journal.saveNeeded = true;
		}
	}

	public static void store( Bundle bundle ){
		bundle.put( PAGES, foundPages.toArray( new String[0] ) );
	}

	public static void restore( Bundle bundle ){
		foundPages.clear();
		if (bundle.contains( PAGES )){
			for (String key : bundle.getStringArray( PAGES )){
				foundPages.add( key );
			}
		}
	}
}

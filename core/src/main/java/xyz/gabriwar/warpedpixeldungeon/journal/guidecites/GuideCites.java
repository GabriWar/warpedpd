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

package xyz.gabriwar.warpedpixeldungeon.journal.guidecites;

import com.watabou.noosa.Game;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

//Every guide page's cites, registered once by the groups below. Each group writes into a book of
//its own, merged in after it finishes: a group that throws loses only its own cites, never the guide.
public final class GuideCites {

	private GuideCites(){}

	/** The groups, in the order they register, and the chapters (with their sub-chapters) whose pages each cites. */
	public static final String[][] GROUPS = {
		{ "SurfaceCites", "ch_surface", "ch_homestead", "ch_town" },     //ch_peaks, ch_deep, ch_mines are inside
		{ "EarlyCites",   "ch_sewers", "ch_prison", "ch_caves" },        //ch_nest is inside ch_prison
		{ "LateCites",    "ch_city", "ch_halls", "ch_postgame" },        //ch_frozen is inside ch_halls
		{ "ExtraCites",   "ch_dens", "ch_sokoban", "ch_zot", "ch_topics" },  //the root "guide" has no text
		{ "BasicsCites",  "ch_basics" },
	};

	private static CiteBook book;
	//what went wrong while the groups registered, for GuideCitesTest: a group that threw, or a page
	//two groups both wrote
	static final LinkedHashMap<String, String> failures = new LinkedHashMap<>();

	/** A page's cites in registration order: unmodifiable, empty when none. */
	public static synchronized List<Cite> of( String key ){
		CiteBook.Page p = book().pages.get( key );
		return p == null ? Collections.<Cite>emptyList() : Collections.unmodifiableList( p.cites );
	}

	/** A page's highlights that name no thing. */
	public static synchronized List<String> plain( String key ){
		CiteBook.Page p = book().pages.get( key );
		return p == null ? Collections.<String>emptyList() : Collections.unmodifiableList( p.plain );
	}

	/** The cite that stands for a page on its card, or null. */
	public static synchronized Cite primary( String key ){
		for (Cite c : of( key )){
			if (c.primary) return c;
		}
		return null;
	}

	/** Everything, for GuideCitesTest. */
	static synchronized CiteBook book(){
		if (book == null){
			CiteBook all = new CiteBook();
			for (String[] g : GROUPS){
				CiteBook part = new CiteBook();
				part.group = g[0];
				try {
					register( g[0], part );
				} catch (Exception e){
					failures.put( g[0], e.toString() );
					Game.reportException( e );
					continue;
				}
				for (CiteBook.Page p : part.pages.values()){
					if (all.pages.containsKey( p.key )){
						failures.put( g[0] + "/" + p.key, "already written by " + all.pages.get( p.key ).group );
					} else {
						all.pages.put( p.key, p );
					}
				}
			}
			book = all;
		}
		return book;
	}

	private static void register( String group, CiteBook part ){
		switch (group){
			case "SurfaceCites": SurfaceCites.register( part ); break;
			case "EarlyCites":   EarlyCites.register( part ); break;
			case "LateCites":    LateCites.register( part ); break;
			case "ExtraCites":   ExtraCites.register( part ); break;
			case "BasicsCites":  BasicsCites.register( part ); break;
			default: throw new IllegalStateException( "no group " + group );
		}
	}
}

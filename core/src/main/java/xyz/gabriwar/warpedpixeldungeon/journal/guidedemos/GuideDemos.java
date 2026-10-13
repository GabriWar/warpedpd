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

package xyz.gabriwar.warpedpixeldungeon.journal.guidedemos;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

//Every guide page's demos, registered once by the groups below, the same groups as the cites. Each
//group writes into a book of its own, merged in after it finishes: a group that throws loses only
//its own demos, never the guide. Each group's words (captions, the words that rise over a creature)
//live in a bundle of its own, so no two groups ever write the same file.
public final class GuideDemos {

	private GuideDemos(){}

	/** The groups, in the order they register, and the chapters (with their sub-chapters) whose pages each acts out. */
	public static final String[][] GROUPS = {
		{ "SurfaceDemos", "ch_surface", "ch_homestead", "ch_town" },     //ch_peaks, ch_deep, ch_mines are inside
		{ "EarlyDemos",   "ch_sewers", "ch_prison", "ch_caves" },        //ch_nest is inside ch_prison
		{ "LateDemos",    "ch_city", "ch_halls", "ch_postgame" },        //ch_frozen is inside ch_halls
		{ "ExtraDemos",   "ch_dens", "ch_sokoban", "ch_zot", "ch_topics" },  //the root "guide" has no text
		{ "BasicsDemos",  "ch_basics" },
	};

	/** Each group's own words, English only, in GROUPS' order (Messages loads them all). */
	public static final String[] BUNDLES = {
		Assets.Messages.DEMOS_SURFACE,
		Assets.Messages.DEMOS_EARLY,
		Assets.Messages.DEMOS_LATE,
		Assets.Messages.DEMOS_EXTRA,
		Assets.Messages.DEMOS_BASICS,
	};

	private static DemoBook book;
	//what went wrong while the groups registered, for GuideDemosTest: a group that threw
	static final LinkedHashMap<String, String> failures = new LinkedHashMap<>();

	/** A page's demos in the order they were written: unmodifiable, empty when none. */
	public static synchronized List<Demo> of( String key ){
		ArrayList<Demo> out = null;
		for (Demo d : book().demos){
			if (!d.key.equals( key )) continue;
			if (out == null) out = new ArrayList<>();
			out.add( d );
		}
		return out == null ? Collections.<Demo>emptyList() : Collections.unmodifiableList( out );
	}

	/** Everything, for GuideDemosTest. */
	static synchronized DemoBook book(){
		if (book == null){
			DemoBook all = new DemoBook();
			for (String[] g : GROUPS){
				DemoBook part = new DemoBook();
				part.group = g[0];
				try {
					register( g[0], part );
				} catch (Exception e){
					failures.put( g[0], e.toString() );
					Game.reportException( e );
					continue;
				}
				all.demos.addAll( part.demos );
			}
			book = all;
		}
		return book;
	}

	private static void register( String group, DemoBook part ){
		switch (group){
			case "SurfaceDemos": SurfaceDemos.register( part ); break;
			case "EarlyDemos":   EarlyDemos.register( part ); break;
			case "LateDemos":    LateDemos.register( part ); break;
			case "ExtraDemos":   ExtraDemos.register( part ); break;
			case "BasicsDemos":  BasicsDemos.register( part ); break;
			default: throw new IllegalStateException( "no group " + group );
		}
	}
}

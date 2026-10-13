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

import java.util.ArrayList;

//The guide's acted-out scenes, page by page: the groups (GuideDemos.GROUPS) write into it.
public final class DemoBook {

	DemoBook(){}

	String group;                                   //set by GuideDemos before each group registers
	final ArrayList<Demo> demos = new ArrayList<>();

	/** A new demo for a guide page, by its key (guide.<key>); a page may have more than one, played in
	 *  the order they are written. */
	public Demo demo( String key ){
		if (key == null || key.trim().isEmpty()) throw new IllegalArgumentException( "a demo needs its page's key" );
		int index = 0;
		for (Demo d : demos) if (d.key.equals( key )) index++;
		Demo d = new Demo( key, group, index );
		demos.add( d );
		return d;
	}
}

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

import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.ui.BuildIcons;

import java.util.Locale;

/** The build menu's groups of pieces, in menu order, each with its icon. RECENT is the menu's own
 *  list of the last pieces used: no piece belongs to it. */
public enum BuildCategory {

	RECENT( BuildIcons.RECENT ),
	WALLS( BuildIcons.WALLS ),
	FLOORS( BuildIcons.FLOORS ),
	DOORS( BuildIcons.DOORS ),
	ROOFS( BuildIcons.ROOFS ),
	FURNITURE( BuildIcons.FURNITURE ),
	STATIONS( BuildIcons.STATIONS ),
	FARMING( BuildIcons.FARMING ),
	DEFENCE( BuildIcons.DEFENCE ),
	TRAVEL( BuildIcons.TRAVEL ),
	UTILITY( BuildIcons.UTILITY );

	public final BuildIcons icon;

	BuildCategory( BuildIcons icon ){
		this.icon = icon;
	}

	/** The menu's own list, never a piece's category. */
	public boolean menuOnly(){
		return this == RECENT;
	}

	public String title(){
		return Messages.get( BuildCategory.class, name().toLowerCase( Locale.ENGLISH ) );
	}
}

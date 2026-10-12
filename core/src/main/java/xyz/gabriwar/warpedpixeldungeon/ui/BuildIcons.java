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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.Image;

/**
 * The build menu's icons (interfaces/build_icons.png, drawn by tools/build_icons_gen.py, which
 * fails when a rect here differs from the art): the piece categories, the placing modes and the
 * two cost badges. Each knows its rect on the sheet, as x, y, width, height in pixels.
 */
public enum BuildIcons {

	//the piece categories, 16x16 spacing
	RECENT(     0,  0, 14, 14 ),
	WALLS(     16,  0, 16, 13 ),
	FLOORS(    32,  0, 14, 12 ),
	DOORS(     48,  0, 14, 14 ),
	ROOFS(     64,  0, 16, 13 ),
	FURNITURE( 80,  0, 11, 14 ),
	STATIONS(  96,  0, 16, 12 ),
	FARMING(  112,  0, 14, 13 ),
	DEFENCE(    0, 16, 15, 14 ),
	TRAVEL(    16, 16, 11, 14 ),
	UTILITY(   32, 16, 11, 13 ),

	//the cost badges, a met cost and a missing one, 9x9
	HAVE(      48, 16,  9,  9 ),
	NEED(      64, 16,  9,  9 ),

	//the placing modes, 16x16 spacing
	PLACE(      0, 32, 14, 14 ),
	LINE(      16, 32, 16, 10 ),
	AREA(      32, 32, 14, 13 ),
	DEMOLISH(  48, 32, 14, 13 ),
	REPAIR(    64, 32, 15, 12 ),
	INSPECT(   80, 32, 14, 14 ),
	DONE(      96, 32, 14, 11 ),
	MENU(     112, 32, 13, 10 );

	private final int x, y, width, height;

	BuildIcons( int x, int y, int width, int height ){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public Image get() {
		return get( this );
	}

	public static Image get( BuildIcons type ) {
		Image icon = new Image( Assets.Interfaces.BUILD_ICONS );
		icon.frame( icon.texture.uvRectBySize( type.x, type.y, type.width, type.height ) );
		return icon;
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.tiles;

import xyz.gabriwar.warpedpixeldungeon.Assets;

/**
 * The homesteads' roofs (environment/roofs.png, painted by tools/homestead_art_gen.py, which keeps
 * the same cell numbers as the constants here): 16x16 cells, sixteen to a row.
 *
 * Every kind has two rows: the slope that faces the sun (the ridge and everything south of it)
 * and the slope behind the ridge, a shade darker. A cell is chosen by the 4-neighbour mask of the
 * roof cells around it - {@link #N} | {@link #E} | {@link #S} | {@link #W}, a bit set where that
 * side is more of the same roof - and each open side gets its edge: a lit verge north and west, a
 * shaded one east, and the eaves along the south. The ridge caps are an overlay, drawn over the
 * roof cells of the ridge row. The glass roof's panes are mostly transparent: the ground and
 * whatever grows under a greenhouse show through it.
 */
public class RoofTiles {

	public static final String TEXTURE = Assets.Environment.ROOFS;

	public static final int THATCH  = 0;
	public static final int PLANK   = 1;
	public static final int SLATE   = 2;
	public static final int TILE    = 3;
	public static final int GLASS   = 4;
	public static final int KINDS   = 5;

	//the neighbour mask: that side is more roof
	public static final int N = 1, E = 2, S = 4, W = 8;

	//the first cell of each kind (its sunward slope); the north slope is the row under it
	public static final int THATCH_BASE = 0;
	public static final int PLANK_BASE  = 32;
	public static final int SLATE_BASE  = 64;
	public static final int TILE_BASE   = 96;
	public static final int GLASS_BASE  = 128;
	private static final int NORTH_SLOPE = 16;

	//the ridge caps: RIDGE + kind * 4 + the open ends
	public static final int RIDGE = 160;
	public static final int RIDGE_OPEN_E = 1, RIDGE_OPEN_W = 2;

	/** A roof cell on the slope facing the sun: kind, and the mask of roof around it. */
	public static int index( int kind, int mask ){
		return index( kind, mask, false );
	}

	/** A roof cell; north: on the slope behind the ridge. */
	public static int index( int kind, int mask, boolean north ){
		return kind * 32 + (north ? NORTH_SLOPE : 0) + (mask & 15);
	}

	/** The ridge cap over a ridge cell: ends is RIDGE_OPEN_E | RIDGE_OPEN_W where the ridge stops. */
	public static int ridge( int kind, int ends ){
		return RIDGE + kind * 4 + (ends & 3);
	}
}

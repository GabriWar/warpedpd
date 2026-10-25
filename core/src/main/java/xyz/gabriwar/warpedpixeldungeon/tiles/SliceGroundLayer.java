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
import com.watabou.noosa.Tilemap;

/**
 * A window-sized layer of a view of another band, not the slice's own ground: one depth of the
 * view down (WindowGenerator.belowLayers, under the terrain, hazed by its depth) through a
 * mountain's drops or a cave's pits, the shade their rims cast, or the tops of the rock above the
 * slice (WindowGenerator.Window.top, under the walls' rims). Each view is four parts: its ground's
 * tiles and the shores of its open water (the slice's tileset: tiles_overworld under the sky,
 * tiles_caves below), then the dressing's blends and corner roundings over them (overworld_dress,
 * WindowGenerator.View). Refilled on every slide like the dressing (OverworldDress.Layer); put on
 * screen by GameScene.addBelow and GameScene.addRockTops.
 */
public class SliceGroundLayer extends CustomTilemap {

	/** the rock tops */
	public static final int TOPS = -1;
	/** the parts of a view, drawn in this order */
	public static final int TILES = 0, SHORES = 1, BLENDS = 2, CORNERS = 3;
	public static final int PARTS = 4;

	/** 0: one band down, then deeper; WindowGenerator.BELOW_LAYERS: the shade the rims cast;
	 *  TOPS: the tops of the rock */
	public final int depth;
	/** TILES, SHORES, BLENDS or CORNERS */
	public final int part;

	private int[] data;
	//no cell holds a tile: worked out as the data is handed over (the actor thread adopting a window),
	//so the render thread never scans for it
	private boolean empty = true;

	/** A part of a view of the surface or the peaks, or the rims' shade (its tiles are the surface's). */
	public SliceGroundLayer( int depth, int part ){
		this( depth, part, Assets.Environment.TILES_OVERWORLD );
	}

	/** ...whose tiles and shores are drawn from this tileset (a pit's view of the caves: tiles_caves). */
	public SliceGroundLayer( int depth, int part, String tiles ){
		this.depth = depth;
		this.part = part;
		texture = part == TILES || part == SHORES ? tiles : Assets.Environment.OVERWORLD_DRESS;
	}

	public void setData( int[] data ){
		this.data = data;
		boolean none = true;
		if (data != null) for (int i = 0; i < data.length && none; i++) none = data[i] < 0;
		empty = none;
	}

	/** Nothing to draw: no cell held a tile when the data was handed over (GameScene.addBelow puts up
	 *  no tilemap for a part of the view down that has never had anything to draw: a cave's pits are
	 *  few, and most of its view's parts stay empty). */
	public boolean empty(){
		return empty;
	}

	/** the tile shown at this cell, -1 for none */
	public int at( int cell ){
		return data == null || cell < 0 || cell >= data.length ? -1 : data[cell];
	}

	/** one cell's tile changed (the rock under it mined, OverworldLevel.retop): written into the
	 *  data the tilemap draws from, and only that cell rebuilt */
	public void setCell( int cell, int tile ){
		int[] d = data;
		if (d == null || cell < 0 || cell >= d.length || d[cell] == tile) return;
		d[cell] = tile;
		if (tile >= 0) empty = false;
		if (vis != null && vis.alive){
			int x = cell % tileW, y = cell / tileW;
			vis.updateMapRect( new com.watabou.utils.Rect( x, y, x + 1, y + 1 ) );
		}
	}

	@Override
	public Tilemap create(){
		if (data == null){
			data = new int[tileW * tileH];
			java.util.Arrays.fill( data, -1 );
		}
		return create( data, tileW );
	}
}

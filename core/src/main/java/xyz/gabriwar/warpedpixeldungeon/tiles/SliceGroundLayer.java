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
 * view down from a mountain (WindowGenerator.belowLayers, under the terrain, where the slice is
 * open air, hazed by its depth), the shade the drops' rims cast, or the tops of the rock above
 * the slice (WindowGenerator.Window.top, under the walls' rims). Each view is three parts: its
 * ground's tiles (tiles_overworld), then the dressing's blends and corner roundings over them
 * (overworld_dress, WindowGenerator.View). Refilled on every slide like the dressing
 * (OverworldDress.Layer); put on screen by GameScene.addBelow and GameScene.addRockTops.
 */
public class SliceGroundLayer extends CustomTilemap {

	/** the rock tops */
	public static final int TOPS = -1;
	/** the parts of a view, drawn in this order */
	public static final int TILES = 0, BLENDS = 1, CORNERS = 2;

	/** 0: one band down, then deeper; WindowGenerator.BELOW_LAYERS: the shade the rims cast;
	 *  TOPS: the tops of the rock */
	public final int depth;
	/** TILES, BLENDS or CORNERS */
	public final int part;

	private int[] data;

	public SliceGroundLayer( int depth, int part ){
		this.depth = depth;
		this.part = part;
		texture = part == TILES ? Assets.Environment.TILES_OVERWORLD : Assets.Environment.OVERWORLD_DRESS;
	}

	public void setData( int[] data ){
		this.data = data;
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

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
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.Drylands;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.noosa.Tilemap;

import java.util.Arrays;

/**
 * The props of the desert's places (levels/overworld/Drylands) on the overworld: the oasis' date
 * palms and its trader's rug, the buried temple's slabs, stairwell, obelisks and jackal statues.
 * Two window-sized layers from their own sheet (custom_tiles/drylands.png, drawn by
 * tools/assets/drylands_art.py), laid and slid with the window exactly as the dressing's
 * (OverworldDress.Layer): the ground one under the hero, among the custom tiles (it names the
 * cells it covers when they are examined), the canopy one over him - the palms' crowns, swaying in
 * the wind as the oaks' do, the obelisks' tips and the statues' heads, over the cells above theirs.
 * Never bundled: OverworldLevel lays them from the world again.
 */
public class DrylandsArt extends CustomTilemap {

	{
		texture = Assets.Environment.DRYLANDS_PROPS;
	}

	private int[] data;

	//how many of a tile's top rows the wind sways (WindSway): a palm crown's fronds and the top of
	//its trunk, the trunk standing on the cell below
	private static final int[] SWAY_ROWS = new int[Drylands.ART_PALM_CROWN + 3];
	static {
		for (int i = 0; i < 3; i++) SWAY_ROWS[Drylands.ART_PALM_CROWN + i] = 11;
	}

	public void setData( int[] data ){
		this.data = data;
	}

	/** The tile on one window cell, -1 for none. */
	public int get( int cell ){
		int[] d = data;
		return d == null || cell < 0 || cell >= d.length ? -1 : d[cell];
	}

	/** One cell's tile changed in play: written into the data drawn from, that cell alone rebuilt. */
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
		int[] d = data;
		if (d == null){
			d = new int[tileW * tileH];
			Arrays.fill( d, -1 );
		}
		return create( d, tileW );
	}

	@Override
	protected Tilemap tilemap(){
		WindSway.Batch sway = WindSway.batch( SWAY_ROWS );
		return sway == null ? super.tilemap() : new OverworldDress.Swaying( texture, sway );
	}

	//the key a tile is named by
	private static String kind( int tile ){
		if (tile < 0) return null;
		if (tile < Drylands.ART_PALM_CROWN + 3) return "palm";
		if (tile == Drylands.ART_RUG) return "rug";
		if (tile == Drylands.ART_STAIR) return "stair";
		if (tile == Drylands.ART_OBELISK || tile == Drylands.ART_OBELISK_TOP) return "obelisk";
		if (tile == Drylands.ART_STATUE || tile == Drylands.ART_STATUE_TOP
				|| tile == Drylands.ART_STATUE_EAST || tile == Drylands.ART_STATUE_TOP_EAST) return "statue";
		return "slab";
	}

	@Override
	public String name( int tileX, int tileY ){
		String k = kind( get( tileX + tileY * tileW ) );
		return k == null ? null : Messages.get( DrylandsArt.class, k );
	}

	@Override
	public String desc( int tileX, int tileY ){
		String k = kind( get( tileX + tileY * tileW ) );
		return k == null ? null : Messages.get( DrylandsArt.class, k + "_desc" );
	}
}

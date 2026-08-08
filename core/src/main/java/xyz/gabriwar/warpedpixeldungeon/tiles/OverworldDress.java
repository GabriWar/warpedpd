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
 * The overworld's dressing, drawn from ONE spritesheet (overworld_dress.png,
 * composed by tools/dress_sheet.py): biome edge transitions, tree variety and
 * village details. Two window-sized layers - Ground below the hero, Canopy above
 * (walk-behind treetops and the winter well cap). OverworldLevel computes the data per
 * window; the ids here mirror the sheet layout.
 */
public class OverworldDress {

	//row 0/1/2: snow/sand/grass edge overlays; id = row*16 + 4-bit side mask (1=N,2=E,4=S,8=W)
	public static final int BLEND_SNOW  = 0;
	public static final int BLEND_SAND  = 16;
	public static final int BLEND_GRASS = 32;

	public static final int[] WINTER_PINES = { 48, 49, 50, 51, 52, 53 };
	public static final int[] SUMMER_PINES = { 54, 55, 56 };
	public static final int OAK_SUMMER_TOP = 57, OAK_SUMMER_TRUNK = 58;
	public static final int OAK_AUTUMN_TOP = 59, OAK_AUTUMN_TRUNK = 60;
	public static final int OAK_WINTER_TOP = 61, OAK_WINTER_TRUNK = 62;

	public static final int[] FOREST_FILL   = { 64, 65, 66, 67, 68, 69, 70, 71 };
	public static final int[] FOREST_TOPS   = { 72, 73, 74 };
	public static final int[] FOREST_TRUNKS = { 75, 76, 77 };

	public static final int SIGNPOST = 78;

	//rows 5-7: corner overlays, id = base + 4-bit corner mask (1=NE,2=SE,4=SW,8=NW)
	public static final int CORNER_SNOW  = 80;
	public static final int CORNER_SAND  = 96;
	public static final int CORNER_GRASS = 112;

	//deep water, darker the further from shore
	public static final int[] DEEP_SHADES = { 128, 129, 130, 131 };

	//the seasons' canopies: autumn (orange-brown) and snowed (winter, on
	//forests whose ground is not frozen - frozen ground has the winter pines)
	public static final int[] AUTUMN_FILL   = { 144, 145, 146, 147, 148, 149, 150, 151 };
	public static final int[] AUTUMN_TOPS   = { 152, 153, 154 };
	public static final int[] AUTUMN_TRUNKS = { 155, 156, 157 };
	public static final int[] SNOWED_FILL   = { 160, 161, 162, 163, 164, 165, 166, 167 };
	public static final int[] SNOWED_TOPS   = { 168, 169, 170 };
	public static final int[] SNOWED_TRUNKS = { 171, 172, 173 };

	//ground overlays by season: spring blossoms on meadow/plains grass, autumn
	//leaves fallen by the forests, a trodden snow cap on the frozen roads
	public static final int[] SPRING_FLOWERS = { 176, 177, 178, 179 };
	public static final int[] FALLEN_LEAVES  = { 180, 181, 182, 183 };
	public static final int[] ROAD_SNOW      = { 184, 185, 186, 187 };
	//a full tile: the bridge planks with snow banked along the rails
	public static final int BRIDGE_SNOW = 188;

	//overworld_villages.png (tools/village_sheet.py): 5x3 roofs; +c +16*r
	public static final int ROOF_SUMMER = 0;
	public static final int ROOF_WINTER = 6;
	//the settlement well, re-cut to sit centred in one tile column. bare stone,
	//so the same two tiles serve every season
	public static final int WELL_TOP = 48, WELL_BASE = 49;

	private OverworldDress(){}

	public static class Layer extends CustomTilemap {

		{
			texture = Assets.Environment.OVERWORLD_DRESS;
		}

		//the village dressing draws from its own sheet
		public static class Village extends Layer {
			{
				texture = Assets.Environment.OVERWORLD_VILLAGES;
			}
		}

		private int[] data;

		public void setData( int[] data ){
			this.data = data;
		}

		@Override
		public Tilemap create(){
			//the overworld rebuilds its dressing on every window slide: refill
			//the tilemap already on screen rather than mint a new vertex buffer
			return create( data != null ? data : new int[tileW * tileH], tileW );
		}
	}
}

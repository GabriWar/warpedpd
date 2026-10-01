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
 * standing rocks. Three window-sized layers - Ground and Edges below the hero,
 * Canopy above (walk-behind treetops and rock peaks). OverworldLevel computes the
 * data per window; the ids here mirror the sheet layout.
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
	//rows 14-17: where the water deepens, the darker shade spills onto the lighter cell in
	//loose pixels as dark as shade band k; id = SHADE_EDGE + 16*k + side mask
	public static final int SHADE_EDGE = 224;

	//the edges come in versions so neighbouring cells never repeat (tools/dress_sheet.py):
	//version 0 sits at the ids above, the rest in the rows below 288
	public static final int BIOME_VARIANTS = 8, SHADE_VARIANTS = 4;
	private static final int BIOME_VAR_BASE = 288, SHADE_VAR_BASE = 960;

	/** a biome side edge: the material's row (BLEND_SNOW/SAND/GRASS), side mask, version */
	public static int blend( int row, int mask, int version ){
		if (version <= 0) return row + mask;
		return BIOME_VAR_BASE + (version - 1) * 96 + row + mask;
	}

	/** a biome corner rounding: the material's row (CORNER_SNOW/SAND/GRASS), corner bits, version */
	public static int corner( int row, int bits, int version ){
		if (version <= 0) return row + bits;
		return BIOME_VAR_BASE + (version - 1) * 96 + 48 + (row - CORNER_SNOW) + bits;
	}

	/** a deep-water shade edge as dark as band k: side mask, version */
	public static int shadeEdge( int k, int mask, int version ){
		if (version <= 0) return SHADE_EDGE + 16 * k + mask;
		return SHADE_VAR_BASE + (version - 1) * 64 + 16 * k + mask;
	}
	//standing rocks (tall boulders): the body on the rock's cell, the peak over
	//the cell above; two shapes, bare and snow-capped
	public static final int[] ROCK_BODY      = { 132, 133 };
	public static final int[] ROCK_TOP       = { 134, 135 };
	public static final int[] ROCK_BODY_SNOW = { 136, 137 };
	public static final int[] ROCK_TOP_SNOW  = { 138, 139 };

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
	//plank bridges (full tiles): a span running east-west or north-south, as
	//mid span, first end, last end; and the same six dusted with snow
	public static final int[] BRIDGE_EW      = { 192, 193, 194 };
	public static final int[] BRIDGE_NS      = { 195, 196, 197 };
	public static final int[] BRIDGE_SNOW_EW = { 198, 199, 200 };
	public static final int[] BRIDGE_SNOW_NS = { 201, 202, 203 };
	//the ways between the world's slices, drawn over their stairs terrain
	public static final int LINK_CAVE_MOUTH = 208, LINK_CAVE_EXIT = 209;
	public static final int LINK_STAIR_UP = 210, LINK_STAIR_DOWN = 211;
	public static final int LINK_SHAFT_DOWN = 212, LINK_SHAFT_UP = 213;

	//overworld_villages.png (tools/village_sheet.py): 5x3 roofs; +c +16*r
	public static final int ROOF_SUMMER = 0;
	public static final int ROOF_WINTER = 6;

	private OverworldDress(){}

	public static class Layer extends CustomTilemap {

		{
			texture = Assets.Environment.OVERWORLD_DRESS;
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

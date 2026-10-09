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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

/**
 * Where every frame of the kit's two sheets lies, { x, y, w, h } in pixels (a sequence as an
 * array of them), as tools/fx_sheet_gen.py and tools/fx_light_gen.py print them with --java: run
 * the tool again and paste its table here whenever a frame changes.
 *
 * The constants here are effects/fx_sheet.png's (Assets.Effects.FX_SHEET): light frames white in
 * four alpha levels, matter frames grey-shaded at full alpha (docs/fx/README.md). Light holds
 * effects/fx_light.png's (Assets.Effects.FX_LIGHT): soft white light in 16 alpha steps, drawn
 * additively.
 */
public final class FxFrames {

	private FxFrames(){}

	// fire: light
	public static final int[][] TONGUE_S = { { 199, 98, 3, 5 }, { 203, 98, 3, 5 }, { 207, 98, 3, 5 }, { 211, 98, 3, 5 } };
	public static final int[][] TONGUE_M = { { 76, 88, 5, 8 }, { 82, 88, 5, 8 }, { 88, 88, 5, 8 }, { 94, 88, 5, 8 } };
	public static final int[][] TONGUE_L = { { 60, 76, 7, 11 }, { 68, 76, 7, 11 }, { 76, 76, 7, 11 }, { 84, 76, 7, 11 } };
	public static final int[][] LICK = { { 245, 105, 2, 3 }, { 248, 105, 2, 3 } };
	public static final int[][] STREAK_FLAME = { { 14, 110, 6, 2 }, { 21, 110, 6, 2 } };
	public static final int[] EMBER_1 = { 69, 110, 1, 1 };
	public static final int[] EMBER_2 = { 49, 110, 2, 2 };
	public static final int[] CORE_FLASH = { 112, 59, 15, 15 };

	// matter: puffs, gas, steam, ink
	public static final int[][] PUFF_S = { { 0, 98, 9, 6 }, { 10, 98, 9, 6 } };
	public static final int[][] PUFF_M = { { 164, 76, 13, 9 }, { 178, 76, 13, 9 } };
	public static final int[][] PUFF_L = { { 24, 76, 17, 11 }, { 42, 76, 17, 11 } };
	public static final int[][] PUFF_XL = { { 146, 59, 20, 13 }, { 167, 59, 20, 13 } };
	public static final int[][] GAS_S = { { 20, 98, 8, 6 }, { 29, 98, 8, 6 } };
	public static final int[][] GAS_L = { { 192, 76, 12, 9 }, { 205, 76, 12, 9 } };
	public static final int[][] STEAM_CURL = { { 237, 76, 6, 9 }, { 244, 76, 6, 9 }, { 0, 88, 6, 9 } };
	public static final int[][] EDGE_WISP = { { 137, 105, 10, 3 }, { 148, 105, 10, 3 } };
	public static final int[][] INK_WISP = { { 152, 88, 3, 7 }, { 156, 88, 3, 7 }, { 160, 88, 3, 7 } };
	public static final int[] INK_CORE = { 181, 98, 5, 5 };

	// twinkles: light
	public static final int[] SPARK_2 = { 60, 110, 1, 2 };
	public static final int[] SPARK_3 = { 4, 110, 1, 3 };
	public static final int[] MOTE_1 = { 71, 110, 1, 1 };
	public static final int[] MOTE_2 = { 52, 110, 2, 2 };
	public static final int[] PLUS_3 = { 217, 105, 3, 3 };
	public static final int[] PLUS_5 = { 187, 98, 5, 5 };
	public static final int[] CROSS_5 = { 175, 98, 5, 5 };
	public static final int[] STAR4_5 = { 193, 98, 5, 5 };
	public static final int[] STAR4_7 = { 144, 88, 7, 7 };
	public static final int[][] RUNE_3 = { { 221, 105, 3, 3 }, { 225, 105, 3, 3 }, { 229, 105, 3, 3 }, { 233, 105, 3, 3 } };

	// rings: 2:1 outlines, the near arc 255, the sides 200, the far arc 150
	public static final int[] RING_5 = { 185, 105, 5, 3 };
	public static final int[] RING_9 = { 127, 98, 9, 5 };
	public static final int[] RING_13 = { 100, 88, 13, 7 };
	public static final int[] RING_17 = { 128, 76, 17, 9 };
	public static final int[] RING_23 = { 0, 76, 23, 11 };
	public static final int[] RING_31 = { 80, 59, 31, 15 };
	public static final int[] RING_41 = { 75, 34, 41, 19 };
	public static final int[] RING_55 = { 74, 0, 55, 25 };
	public static final int[] RING_73 = { 0, 0, 73, 33 };

	// water: matter but the reflections (light)
	public static final int[] DROP_2 = { 58, 110, 1, 2 };
	public static final int[] DROP_3 = { 0, 110, 1, 3 };
	public static final int[] PLINK = { 2, 110, 1, 3 };
	public static final int[][] CROWN = { { 28, 105, 5, 4 }, { 167, 98, 7, 5 }, { 117, 98, 9, 5 } };
	public static final int[][] WAKE_H = { { 147, 98, 9, 5 }, { 114, 88, 13, 7 }, { 146, 76, 17, 9 } };
	public static final int[][] WAKE_V = { { 10, 105, 9, 4 }, { 91, 98, 13, 5 }, { 164, 88, 17, 6 } };
	public static final int[][] WAKE_D = { { 38, 98, 7, 6 }, { 218, 76, 11, 9 }, { 214, 59, 15, 12 } };
	public static final int[][] COLLAR_9 = { { 159, 105, 9, 3 }, { 169, 105, 9, 3 } };
	public static final int[][] COLLAR_13 = { { 109, 105, 13, 3 }, { 123, 105, 13, 3 } };
	public static final int[][] COLLAR_17 = { { 217, 98, 17, 4 }, { 235, 98, 17, 4 } };
	public static final int[][] FOAM_H = { { 58, 105, 16, 3 }, { 75, 105, 16, 3 }, { 92, 105, 16, 3 } };
	public static final int[][] FOAM_V = { { 68, 59, 3, 16 }, { 72, 59, 3, 16 }, { 76, 59, 3, 16 } };
	public static final int[][] WHITECAP = { { 28, 110, 5, 2 }, { 6, 110, 7, 2 }, { 62, 110, 3, 1 } };
	public static final int[] BUBBLE_2 = { 46, 110, 2, 2 };
	public static final int[] BUBBLE_3 = { 213, 105, 3, 3 };
	public static final int[] POP_5 = { 179, 105, 5, 3 };
	public static final int[][] REFLECT = { { 128, 59, 5, 14 }, { 134, 59, 5, 14 }, { 140, 59, 5, 14 } };

	// nature: matter
	public static final int[][] LEAF_D = { { 203, 105, 4, 3 }, { 208, 105, 4, 3 } };
	public static final int[][] LEAF_E = { { 42, 105, 3, 4 }, { 46, 105, 3, 4 } };
	public static final int[] BLADE_3 = { 254, 105, 1, 3 };
	public static final int[] BLADE_4 = { 56, 105, 1, 4 };
	public static final int[] BLADE_5 = { 215, 98, 1, 5 };
	public static final int[] SEED = { 66, 110, 2, 1 };
	public static final int[] PETAL_B = { 42, 110, 3, 2 };
	public static final int[][] BARK = { { 34, 105, 3, 4 }, { 38, 105, 3, 4 } };
	public static final int[] FEATHER = { 34, 110, 3, 2 };
	public static final int[][] VINE = { { 182, 88, 14, 6 }, { 197, 88, 14, 6 }, { 212, 88, 14, 6 } };

	// debris: matter
	public static final int[] PEBBLE_2 = { 55, 110, 2, 2 };
	public static final int[] PEBBLE_3 = { 38, 110, 3, 2 };
	public static final int[] PEBBLE_3V = { 251, 105, 2, 3 };
	public static final int[][] SHARD_GLASS = { { 237, 105, 3, 3 }, { 241, 105, 3, 3 } };
	public static final int[][] SHARD_ICE = { { 50, 105, 2, 4 }, { 53, 105, 2, 4 } };

	// combat: light, the exclamation matter
	public static final int[][] IMPACT_STAR = { { 128, 88, 7, 7 }, { 136, 88, 7, 7 } };
	public static final int[][] SLASH_16 = { { 117, 34, 16, 16 }, { 134, 34, 16, 16 }, { 151, 34, 16, 16 }, { 168, 34, 16, 16 }, { 185, 34, 16, 16 }, { 202, 34, 16, 16 }, { 219, 34, 16, 16 }, { 236, 34, 16, 16 } };
	public static final int[][] SLASH_24 = { { 130, 0, 24, 24 }, { 155, 0, 24, 24 }, { 180, 0, 24, 24 }, { 205, 0, 24, 24 }, { 230, 0, 24, 24 }, { 0, 34, 24, 24 }, { 25, 34, 24, 24 }, { 50, 34, 24, 24 } };
	public static final int[] STREAK_TAPER = { 74, 98, 16, 5 };
	public static final int[][] EXCLAIM = { { 230, 59, 6, 12 }, { 230, 76, 6, 9 } };

	// decals: matter
	public static final int[] SCORCH_S = { 105, 98, 11, 5 };
	public static final int[] SCORCH_M = { 7, 88, 17, 8 };
	public static final int[] SCORCH_L = { 188, 59, 25, 12 };
	public static final int[][] SPLAT = { { 191, 105, 5, 3 }, { 20, 105, 7, 4 }, { 137, 98, 9, 5 } };
	public static final int[] WET_5 = { 197, 105, 5, 3 };
	public static final int[] WET_9 = { 157, 98, 9, 5 };
	public static final int[] ETCH = { 0, 105, 9, 4 };
	public static final int[][] RIME = { { 25, 88, 16, 8 }, { 42, 88, 16, 8 }, { 59, 88, 16, 8 } };
	public static final int[][] CRACK_11 = { { 227, 88, 11, 6 }, { 239, 88, 11, 6 } };
	public static final int[][] CRACK_17 = { { 92, 76, 17, 9 }, { 110, 76, 17, 9 } };
	public static final int[][] WEB = { { 0, 59, 16, 16 }, { 17, 59, 16, 16 } };
	public static final int[][] WEB_B = { { 34, 59, 16, 16 }, { 51, 59, 16, 16 } };

	// skills: matter
	public static final int[][] COIN_6 = { { 46, 98, 6, 6 }, { 53, 98, 6, 6 }, { 60, 98, 6, 6 }, { 67, 98, 6, 6 } };
	/** The rings, smallest first, and their widths. */
	public static final int[][] RINGS = { RING_5, RING_9, RING_13, RING_17, RING_23, RING_31, RING_41, RING_55, RING_73 };
	public static final int[] RING_WIDTHS = { 5, 9, 13, 17, 23, 31, 41, 55, 73 };

	/** The index of the ring of this width in RINGS, or of the nearest narrower one. */
	public static int ringIndex( int width ){
		int i = 0;
		while (i + 1 < RING_WIDTHS.length && RING_WIDTHS[i + 1] <= width) i++;
		return i;
	}

	/** The light sheet's frames (Assets.Effects.FX_LIGHT). */
	public static final class Light {

		private Light(){}

		// round glows, on a source in the air
		public static final int[] GLOW_7 = { 201, 209, 7, 7 };
		public static final int[] GLOW_11 = { 136, 209, 11, 11 };
		public static final int[] GLOW_15 = { 32, 209, 15, 15 };
		public static final int[] GLOW_23 = { 144, 153, 23, 23 };
		public static final int[] GLOW_31 = { 160, 105, 31, 31 };
		public static final int[] GLOW_47 = { 126, 0, 47, 47 };
		public static final int[] GLOW_63 = { 14, 0, 63, 63 };

		// 2:1 pools on the ground under it
		public static final int[] POOL_15 = { 185, 209, 15, 7 };
		public static final int[] POOL_23 = { 112, 209, 23, 11 };
		public static final int[] POOL_31 = { 0, 209, 31, 15 };
		public static final int[] POOL_47 = { 72, 153, 47, 23 };
		public static final int[] POOL_63 = { 64, 105, 63, 31 };

		// stars: A along the axes, B turned
		public static final int[] STAR4_9_A = { 148, 209, 9, 9 };
		public static final int[] STAR4_9_B = { 158, 209, 9, 9 };
		public static final int[] STAR4_15_A = { 48, 209, 15, 15 };
		public static final int[] STAR4_15_B = { 64, 209, 15, 15 };
		public static final int[] STAR4_23_A = { 120, 185, 23, 23 };
		public static final int[] STAR4_23_B = { 144, 185, 23, 23 };
		public static final int[] STAR4_31_A = { 192, 105, 31, 31 };
		public static final int[] STAR4_31_B = { 224, 105, 31, 31 };
		public static final int[] STAR8_15_A = { 80, 209, 15, 15 };
		public static final int[] STAR8_15_B = { 96, 209, 15, 15 };
		public static final int[] STAR8_23_A = { 168, 185, 23, 23 };
		public static final int[] STAR8_23_B = { 192, 185, 23, 23 };
		public static final int[] STAR8_31_A = { 0, 153, 31, 31 };
		public static final int[] STAR8_31_B = { 32, 153, 31, 31 };
		public static final int[] STAR8_47_A = { 174, 0, 47, 47 };
		public static final int[] STAR8_47_B = { 0, 105, 47, 47 };

		// bubbles and their rims
		public static final int[] BUBBLE_23 = { 120, 153, 23, 23 };
		public static final int[] BUBBLE_31 = { 128, 105, 31, 31 };
		public static final int[] BUBBLE_47 = { 78, 0, 47, 47 };
		public static final int[][] RIM_ARC_23 = { { 168, 153, 23, 23 }, { 192, 153, 23, 23 }, { 216, 153, 23, 23 }, { 0, 185, 23, 23 }, { 24, 185, 23, 23 }, { 48, 185, 23, 23 }, { 72, 185, 23, 23 }, { 96, 185, 23, 23 } };

		// bands, beams, shafts and pillars
		public static final int[] BAND_7 = { 168, 209, 16, 7 };
		public static final int[] BAND_21 = { 216, 185, 16, 21 };
		public static final int[] CORE_BAND_1 = { 40, 225, 16, 1 };
		public static final int[] CORE_BAND_3 = { 226, 209, 16, 3 };
		public static final int[] CORE_BAND_5 = { 209, 209, 16, 5 };
		public static final int[] SHAFT_24 = { 64, 153, 7, 24 };
		public static final int[] SHAFT_32 = { 56, 105, 7, 32 };
		public static final int[] SHAFT_40 = { 48, 105, 7, 40 };
		public static final int[] PILLAR = { 0, 0, 9, 104 };
		public static final int[] PILLAR_CORE = { 10, 0, 3, 104 };

		// washes, dots and the prism (its rows coloured)
		public static final int[] WASH_16 = { 233, 185, 16, 16 };
		public static final int[] PULSE_DOT = { 34, 225, 5, 3 };
		public static final int[] PRISM_WARM = { 17, 225, 16, 3 };
		public static final int[] PRISM_COOL = { 0, 225, 16, 3 };
		public static final int[][] GLOWS = { GLOW_7, GLOW_11, GLOW_15, GLOW_23, GLOW_31, GLOW_47, GLOW_63 };
		public static final int[] GLOW_SIZES = { 7, 11, 15, 23, 31, 47, 63 };
		public static final int[][] POOLS = { POOL_15, POOL_23, POOL_31, POOL_47, POOL_63 };
		public static final int[] POOL_SIZES = { 15, 23, 31, 47, 63 };

		/** The glow of this size, or of the nearest size there is. */
		public static int[] glow( int size ){
			return GLOWS[nearest( GLOW_SIZES, size )];
		}

		/** The pool of this width, or of the nearest width there is. */
		public static int[] pool( int size ){
			return POOLS[nearest( POOL_SIZES, size )];
		}

		static int nearest( int[] sizes, int size ){
			int best = 0;
			for (int i = 1; i < sizes.length; i++){
				if (Math.abs( sizes[i] - size ) < Math.abs( sizes[best] - size )) best = i;
			}
			return best;
		}
	}
}

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
	public static final int[][] TONGUE_S = { { 100, 105, 3, 5 }, { 104, 105, 3, 5 }, { 108, 105, 3, 5 }, { 112, 105, 3, 5 } };
	public static final int[][] TONGUE_M = { { 117, 88, 5, 8 }, { 123, 88, 5, 8 }, { 129, 88, 5, 8 }, { 135, 88, 5, 8 } };
	public static final int[][] TONGUE_L = { { 70, 76, 7, 11 }, { 78, 76, 7, 11 }, { 86, 76, 7, 11 }, { 94, 76, 7, 11 } };
	public static final int[][] LICK = { { 167, 111, 2, 3 }, { 170, 111, 2, 3 } };
	public static final int[][] STREAK_FLAME = { { 192, 111, 6, 2 }, { 199, 111, 6, 2 } };
	public static final int[] EMBER_1 = { 247, 111, 1, 1 };
	public static final int[] EMBER_2 = { 227, 111, 2, 2 };
	public static final int[] CORE_FLASH = { 112, 59, 15, 15 };

	// matter: puffs, gas, steam, ink
	public static final int[][] PUFF_S = { { 87, 98, 9, 6 }, { 97, 98, 9, 6 } };
	public static final int[][] PUFF_M = { { 174, 76, 13, 9 }, { 188, 76, 13, 9 } };
	public static final int[][] PUFF_L = { { 24, 76, 17, 11 }, { 42, 76, 17, 11 } };
	public static final int[][] PUFF_XL = { { 146, 59, 20, 13 }, { 167, 59, 20, 13 } };
	public static final int[][] GAS_S = { { 107, 98, 8, 6 }, { 116, 98, 8, 6 } };
	public static final int[][] GAS_L = { { 202, 76, 12, 9 }, { 215, 76, 12, 9 } };
	public static final int[][] STEAM_CURL = { { 27, 88, 6, 9 }, { 34, 88, 6, 9 }, { 41, 88, 6, 9 } };
	public static final int[][] EDGE_WISP = { { 45, 111, 10, 3 }, { 56, 111, 10, 3 } };
	public static final int[][] INK_WISP = { { 231, 88, 3, 7 }, { 235, 88, 3, 7 }, { 239, 88, 3, 7 } };
	public static final int[] INK_CORE = { 40, 105, 5, 5 };

	// twinkles: light
	public static final int[] SPARK_2 = { 238, 111, 1, 2 };
	public static final int[] SPARK_3 = { 182, 111, 1, 3 };
	public static final int[] MOTE_1 = { 249, 111, 1, 1 };
	public static final int[] MOTE_2 = { 230, 111, 2, 2 };
	public static final int[] PLUS_3 = { 135, 111, 3, 3 };
	public static final int[] PLUS_5 = { 46, 105, 5, 5 };
	public static final int[] CROSS_5 = { 34, 105, 5, 5 };
	public static final int[] STAR4_5 = { 94, 105, 5, 5 };
	public static final int[] STAR4_7 = { 223, 88, 7, 7 };
	public static final int[][] RUNE_3 = { { 139, 111, 3, 3 }, { 143, 111, 3, 3 }, { 147, 111, 3, 3 }, { 151, 111, 3, 3 } };
	public static final int[][] RUNE_5 = { { 52, 105, 5, 5 }, { 58, 105, 5, 5 }, { 64, 105, 5, 5 }, { 70, 105, 5, 5 } };
	// the concentrated core: solid light squares, and the steps a core takes as it fades (look(STEP_x, 0))
	public static final int[] SQ_3 = { 163, 111, 3, 3 };
	public static final int[] SQ_4 = { 188, 105, 4, 4 };
	public static final int[][] STEP_4 = { SQ_4, SQ_3, MOTE_2, MOTE_1 };
	public static final int[][] STEP_3 = { SQ_3, MOTE_2, MOTE_1 };

	// rings: 2:1 outlines, the near arc 255, the sides 200, the far arc 150
	public static final int[] RING_5 = { 103, 111, 5, 3 };
	public static final int[] RING_9 = { 230, 98, 9, 5 };
	public static final int[] RING_13 = { 141, 88, 13, 7 };
	public static final int[] RING_17 = { 138, 76, 17, 9 };
	public static final int[] RING_23 = { 0, 76, 23, 11 };
	public static final int[] RING_31 = { 80, 59, 31, 15 };
	public static final int[] RING_41 = { 79, 34, 41, 19 };
	public static final int[] RING_55 = { 74, 0, 55, 25 };
	public static final int[] RING_73 = { 0, 0, 73, 33 };

	// water: matter but the reflections (light)
	public static final int[] DROP_2 = { 236, 111, 1, 2 };
	public static final int[] DROP_3 = { 178, 111, 1, 3 };
	public static final int[] PLINK = { 180, 111, 1, 3 };
	public static final int[][] CROWN = { { 182, 105, 5, 4 }, { 20, 105, 7, 5 }, { 210, 98, 9, 5 } };
	public static final int[][] WAKE_H = { { 0, 105, 9, 5 }, { 155, 88, 13, 7 }, { 156, 76, 17, 9 } };
	public static final int[][] WAKE_V = { { 164, 105, 9, 4 }, { 184, 98, 13, 5 }, { 0, 98, 17, 6 } };
	public static final int[][] WAKE_D = { { 125, 98, 7, 6 }, { 228, 76, 11, 9 }, { 214, 59, 15, 12 } };
	public static final int[][] COLLAR_9 = { { 77, 111, 9, 3 }, { 87, 111, 9, 3 } };
	public static final int[][] COLLAR_13 = { { 17, 111, 13, 3 }, { 31, 111, 13, 3 } };
	public static final int[][] COLLAR_17 = { { 118, 105, 17, 4 }, { 136, 105, 17, 4 } };
	public static final int[][] FOAM_H = { { 217, 105, 16, 3 }, { 234, 105, 16, 3 }, { 0, 111, 16, 3 } };
	public static final int[][] FOAM_V = { { 68, 59, 3, 16 }, { 72, 59, 3, 16 }, { 76, 59, 3, 16 } };
	public static final int[][] WHITECAP = { { 206, 111, 5, 2 }, { 184, 111, 7, 2 }, { 240, 111, 3, 1 } };
	public static final int[] BUBBLE_2 = { 224, 111, 2, 2 };
	public static final int[] BUBBLE_3 = { 131, 111, 3, 3 };
	public static final int[] POP_5 = { 97, 111, 5, 3 };
	public static final int[][] REFLECT = { { 128, 59, 5, 14 }, { 134, 59, 5, 14 }, { 140, 59, 5, 14 } };

	// nature: matter
	public static final int[][] LEAF_D = { { 121, 111, 4, 3 }, { 126, 111, 4, 3 } };
	public static final int[][] LEAF_E = { { 201, 105, 3, 4 }, { 205, 105, 3, 4 } };
	public static final int[] BLADE_3 = { 176, 111, 1, 3 };
	public static final int[] BLADE_4 = { 215, 105, 1, 4 };
	public static final int[] BLADE_5 = { 116, 105, 1, 5 };
	public static final int[] SEED = { 244, 111, 2, 1 };
	public static final int[] PETAL_B = { 220, 111, 3, 2 };
	public static final int[][] BARK = { { 193, 105, 3, 4 }, { 197, 105, 3, 4 } };
	public static final int[] FEATHER = { 212, 111, 3, 2 };
	public static final int[][] VINE = { { 18, 98, 14, 6 }, { 33, 98, 14, 6 }, { 48, 98, 14, 6 } };

	// debris: matter
	public static final int[] PEBBLE_2 = { 233, 111, 2, 2 };
	public static final int[] PEBBLE_3 = { 216, 111, 3, 2 };
	public static final int[] PEBBLE_3V = { 173, 111, 2, 3 };
	public static final int[][] SHARD_GLASS = { { 155, 111, 3, 3 }, { 159, 111, 3, 3 } };
	public static final int[][] SHARD_ICE = { { 209, 105, 2, 4 }, { 212, 105, 2, 4 } };

	// combat: light, the exclamation matter
	public static final int[][] IMPACT_STAR = { { 199, 88, 7, 7 }, { 207, 88, 7, 7 } };
	public static final int[][] SLASH_16 = { { 121, 34, 16, 16 }, { 138, 34, 16, 16 }, { 155, 34, 16, 16 }, { 172, 34, 16, 16 }, { 189, 34, 16, 16 }, { 206, 34, 16, 16 }, { 223, 34, 16, 16 }, { 240, 34, 16, 16 } };
	public static final int[][] SLASH_24 = { { 130, 0, 24, 24 }, { 155, 0, 24, 24 }, { 180, 0, 24, 24 }, { 205, 0, 24, 24 }, { 230, 0, 24, 24 }, { 0, 34, 24, 24 }, { 25, 34, 24, 24 }, { 50, 34, 24, 24 } };
	public static final int[] STREAK_TAPER = { 167, 98, 16, 5 };
	public static final int[][] EXCLAIM = { { 230, 59, 6, 12 }, { 20, 88, 6, 9 } };
	// the crit's red X (matter: B is A mirrored, drawn over it) and a musical note (light)
	public static final int[] X_9_A = { 0, 88, 9, 9 };
	public static final int[] X_9_B = { 10, 88, 9, 9 };
	public static final int[] NOTE_5 = { 161, 98, 5, 6 };

	// decals: matter
	public static final int[] SCORCH_S = { 198, 98, 11, 5 };
	public static final int[] SCORCH_M = { 48, 88, 17, 8 };
	public static final int[] SCORCH_L = { 188, 59, 25, 12 };
	public static final int[][] SPLAT = { { 109, 111, 5, 3 }, { 174, 105, 7, 4 }, { 240, 98, 9, 5 } };
	public static final int[] WET_5 = { 115, 111, 5, 3 };
	public static final int[] WET_9 = { 10, 105, 9, 5 };
	public static final int[] ETCH = { 154, 105, 9, 4 };
	public static final int[][] RIME = { { 66, 88, 16, 8 }, { 83, 88, 16, 8 }, { 100, 88, 16, 8 } };
	public static final int[][] CRACK_11 = { { 63, 98, 11, 6 }, { 75, 98, 11, 6 } };
	public static final int[][] CRACK_17 = { { 102, 76, 17, 9 }, { 120, 76, 17, 9 } };
	public static final int[][] WEB = { { 0, 59, 16, 16 }, { 17, 59, 16, 16 } };
	public static final int[][] WEB_B = { { 34, 59, 16, 16 }, { 51, 59, 16, 16 } };

	// skills: matter
	public static final int[][] COIN_6 = { { 133, 98, 6, 6 }, { 140, 98, 6, 6 }, { 147, 98, 6, 6 }, { 154, 98, 6, 6 } };
	// the enchantments': a snowflake (A along the axes, B turned) and a four-leaf clover (light),
	// a skull (matter)
	public static final int[][] SNOWFLAKE_5 = { { 82, 105, 5, 5 }, { 88, 105, 5, 5 } };
	public static final int[] CLOVER_5 = { 28, 105, 5, 5 };
	public static final int[] SKULL_5 = { 76, 105, 5, 5 };
	// the skills' kit: light (the lance's head, a spirit arrow, a wing, a standard's pole and its cloth
	// waving A, B, C) and matter (a deadfall's counterweight, a falling stone)
	public static final int[] LANCE_HEAD_9 = { 220, 98, 9, 5 };
	public static final int[] ARROW_9 = { 67, 111, 9, 3 };
	public static final int[] WING_9 = { 60, 76, 9, 11 };
	public static final int[] BANNER_POLE = { 75, 34, 3, 20 };
	public static final int[][] BANNER_CLOTH = { { 169, 88, 9, 7 }, { 179, 88, 9, 7 }, { 189, 88, 9, 7 } };
	public static final int[] WEIGHT_9 = { 240, 76, 9, 9 };
	public static final int[] METEOR_7 = { 215, 88, 7, 7 };
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
		public static final int[] CORE_BAND_1 = { 57, 225, 16, 1 };
		public static final int[] CORE_BAND_3 = { 0, 225, 16, 3 };
		public static final int[] CORE_BAND_5 = { 209, 209, 16, 5 };
		public static final int[] SHAFT_24 = { 64, 153, 7, 24 };
		public static final int[] SHAFT_32 = { 56, 105, 7, 32 };
		public static final int[] SHAFT_40 = { 48, 105, 7, 40 };
		public static final int[] PILLAR = { 0, 0, 9, 104 };
		public static final int[] PILLAR_CORE = { 10, 0, 3, 104 };

		// washes, dots and the prism (its rows coloured)
		public static final int[] WASH_16 = { 233, 185, 16, 16 };
		public static final int[] PULSE_DOT = { 51, 225, 5, 3 };
		public static final int[] PRISM_WARM = { 34, 225, 16, 3 };
		public static final int[] PRISM_COOL = { 17, 225, 16, 3 };

		// the enchantments': an eye of light, half open then open
		public static final int[][] EYE_7 = { { 226, 209, 7, 4 }, { 234, 209, 7, 4 } };
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

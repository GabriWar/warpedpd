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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.water;

/**
 * The live water's pure geometry and timing (docs/fx/water-life.md), apart from the scene so the
 * tests pin it: the box of water cells a view holds (what the sheens are cut to), where a picture
 * that tiles at `size` px must start its texture to stay put in the world, the palette cycle's
 * frame at a time and how hard the night and the weather take a sheen. Nothing here allocates.
 */
public final class WaterGeom {

	private WaterGeom(){}

	/** The cycle's frames, the art's tile and the sheens' tile, px. */
	public static final int CYCLE_FRAMES = 8, CYCLE_TILE = 32, SHEEN_TILE = 64;

	/**
	 * The box (in cells, inclusive) of the water cells within the cells [x0..x1] x [y0..y1] of a
	 * map `w` x `h` (clipped to it), into out[0..3] as minX, minY, maxX, maxY; false (out untouched)
	 * when there is none. A row scan, no allocation.
	 */
	public static boolean waterBox( boolean[] water, int w, int h, int x0, int y0, int x1, int y1, int[] out ){
		if (water == null) return false;
		x0 = Math.max( 0, x0 );
		y0 = Math.max( 0, y0 );
		x1 = Math.min( w - 1, x1 );
		y1 = Math.min( h - 1, y1 );
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
		for (int y = y0; y <= y1; y++){
			int row = y * w;
			int first = -1, last = -1;
			for (int x = x0; x <= x1; x++){
				if (water[row + x]){
					if (first < 0) first = x;
					last = x;
				}
			}
			if (first < 0) continue;
			if (minY == Integer.MAX_VALUE) minY = y;
			maxY = y;
			minX = Math.min( minX, first );
			maxX = Math.max( maxX, last );
		}
		if (maxY < 0) return false;
		out[0] = minX;
		out[1] = minY;
		out[2] = maxX;
		out[3] = maxY;
		return true;
	}

	/** Where a picture tiling at `size` px, put at world `pos` and scrolled `scroll` px, starts its
	 *  texture: (pos + scroll) mod size, in 0..size. The same world point always shows the same
	 *  texel, wherever the picture is cut. */
	public static float anchor( float pos, float scroll, int size ){
		float v = (pos + scroll) % size;
		return v < 0 ? v + size : v;
	}

	/** The palette cycle's frame `t` seconds in at `fps` (0: frame 0, the cycle stopped). */
	public static int cycleFrame( float t, int fps ){
		if (fps <= 0 || t <= 0) return 0;
		return (int)(t * fps) % CYCLE_FRAMES;
	}

	/** How far into the night it is for the water, 0 by day to 1 at the night's full tint (0.54). */
	public static float night( float nightTint ){
		return Math.max( 0f, Math.min( 1f, nightTint / 0.54f ) );
	}

	/** A sheen's share underground (under a roof: no sky's light, but the motion must still read). */
	public static final float UNDERGROUND = 0.8f;

	/**
	 * A sheen's share of its liquid's alpha: under an open sky dimmed toward half as clouds cover
	 * it (0.4 to 0.8 cover), underground 0.8, at night toward 0.6 of that; and its breath, +-12% at
	 * 0.25 Hz from `phase` (the two sheens half a cycle apart).
	 */
	public static float sheenShare( boolean openSky, float cloudCover, float nightTint, float t, float phase ){
		float sky = openSky ? 1f - 0.5f * smooth( (cloudCover - 0.4f) / 0.4f ) : UNDERGROUND;
		float n = 1f - 0.4f * night( nightTint );
		float breath = 1f + 0.12f * (float)Math.sin( 2 * Math.PI * 0.25f * t + phase );
		return sky * n * breath;
	}

	// ------------------------------------------------------------------ the water's ambience

	/** Glints a second per 100 water cells in view: a clear day's, under clouds or a roof, at night. */
	public static final float GLINTS_CLEAR = 6f, GLINTS_DULL = 0.3f, GLINTS_NIGHT = 0.5f;

	/**
	 * Glints a second over `waterCells` cells in view: six per hundred on a clear day (one a second
	 * at least, while there is any water), 0.3 of that under clouds or a roof, half a one per hundred
	 * at night (the night's tint eases between them); none in rain of 0.4 or more; the city's oil
	 * 0.3 and the caves' 0.4 of it, the lava none.
	 */
	public static float glintRate( int waterCells, boolean openSky, float cloudCover, float nightTint, float rain, float liquidShare ){
		if (waterCells <= 0 || rain >= POCKS_FROM || liquidShare <= 0f) return 0f;
		float per = waterCells / 100f;
		float day = GLINTS_CLEAR * per;
		if (openSky){
			day *= 1f - (1f - GLINTS_DULL) * smooth( (cloudCover - 0.4f) / 0.4f );
			day = Math.max( day, cloudCover < 0.6f ? 1f : 0f );
		} else {
			day *= GLINTS_DULL;
		}
		float n = night( nightTint );
		return liquidShare * (day + (GLINTS_NIGHT * per - day) * n);
	}

	/** A glint's alpha: 0.75 by day, 0.35 at night. */
	public static float glintAlpha( float nightTint ){
		return 0.75f - 0.4f * night( nightTint );
	}

	// ------------------------------------------------------------------ the rain

	/** Below this rate the rain lands as rings one by one; from it, the pocks take over (and the
	 *  glints are gone). */
	public static final float POCKS_FROM = 0.4f;

	/** The pocks' alpha at a rain rate: none below POCKS_FROM, 0.15 there, rising to 0.45 at 1. */
	public static float pockAlpha( float rate ){
		if (rate < POCKS_FROM) return 0f;
		return 0.15f + 0.30f * Math.min( 1f, (rate - POCKS_FROM) / (1f - POCKS_FROM) );
	}

	/** How much of the sheens' calm shine the rain leaves: all of it to 0.2, easing down to 0.3 of
	 *  it in a downpour (1), so a beaten surface never reads as still water. */
	public static float rainDim( float rate ){
		return 1f - 0.7f * smooth( (rate - 0.2f) / 0.8f );
	}

	/** What the rain's marks on the water (its rings, its pocks) are lifted by at night, so the
	 *  night's tint over them leaves them to be seen: 1 by day, 1 + `lift` at the night's full tint. */
	public static float nightLift( float nightTint, float lift ){
		return 1f + lift * night( nightTint );
	}

	/** The rain's rings on the water at once: twice the water cells in view, 24 at most. */
	public static int rainRings( int waterCells ){
		return Math.min( 24, 2 * waterCells );
	}

	// ------------------------------------------------------------------ wading

	/** The wakes: horizontal, vertical, diagonal. */
	public static final int WAKE_H = 0, WAKE_V = 1, WAKE_D = 2;

	/** The wake a step of (dx, dy) cells leaves: WAKE_H, WAKE_V or WAKE_D. */
	public static int wakeKind( int dx, int dy ){
		if (dx != 0 && dy != 0) return WAKE_D;
		return dy != 0 ? WAKE_V : WAKE_H;
	}

	/** Whether the wake is mirrored left to right: the painted wakes trail a wader heading east
	 *  (and, slanting, south-east), their point at its feet; one heading west is mirrored. */
	public static boolean wakeFlipX( int dx ){
		return dx < 0;
	}

	/** Whether the wake is mirrored top to bottom: the painted ones trail a wader heading south;
	 *  one heading north is mirrored. */
	public static boolean wakeFlipY( int dy ){
		return dy < 0;
	}

	/** The collar a wader of this width wears: 9, 13 or 17 px. */
	public static int collarWidth( float spriteWidth ){
		return spriteWidth <= 12 ? 9 : spriteWidth <= 16 ? 13 : 17;
	}

	/** A wader's bob on the water `t` s in, from its own phase: 0 or -1 px (a pixel up), at 1.2 Hz. */
	public static int bob( float t, float phase ){
		return Math.sin( 2 * Math.PI * 1.2f * t + phase ) > 0 ? -1 : 0;
	}

	static float smooth( float x ){
		x = Math.max( 0f, Math.min( 1f, x ) );
		return x * x * (3 - 2 * x);
	}
}

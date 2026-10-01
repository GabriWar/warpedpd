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

package xyz.gabriwar.warpedpixeldungeon.levels.overworld;

/**
 * The world is a stack of horizontal slices, each an OverworldLevel of its own
 * with its own sliding window. Slices are numbered by ALTITUDE: 0 is the
 * surface as it always was (biomes, villages, roads); +1..+MAX_ABOVE are the
 * mountains cut every {@link #STEP} of the elevation noise above the mountain
 * line, so a peak is climbed one contour at a time; -1..-MAX_BELOW are cave
 * slices cut from a 3D cave field, each a step deeper under the surface.
 *
 * Every altitude maps to a dungeon depth so the level machinery (save files,
 * transitions, falls) needs nothing new: the surface keeps 97, the mountains
 * count down from 96, the caves count up from 101 (98 and 99 belong to other
 * levels).
 */
public final class WorldLayers {

	public static final int SURFACE_DEPTH = 97;
	public static final int MAX_ABOVE = 10;
	public static final int MAX_BELOW = 12;

	//the elevation of the mountain line (the surface's MOUNTAIN biome starts
	//here) and the elevation each mountain slice climbs above it
	public static final float BASE = 0.700f;
	public static final float STEP = 0.030f;

	private WorldLayers(){}

	/** The dungeon depth of a slice. */
	public static int depthOf( int altitude ){
		if (altitude == 0) return SURFACE_DEPTH;
		return altitude > 0 ? SURFACE_DEPTH - altitude : 100 - altitude;
	}

	/** The altitude a depth stands for, or Integer.MIN_VALUE when the depth is not a slice. */
	public static int altitudeOf( int depth ){
		if (depth == SURFACE_DEPTH) return 0;
		if (depth < SURFACE_DEPTH && depth >= SURFACE_DEPTH - MAX_ABOVE) return SURFACE_DEPTH - depth;
		if (depth > 100 && depth <= 100 + MAX_BELOW) return 100 - depth;
		return Integer.MIN_VALUE;
	}

	public static boolean isLayerDepth( int depth ){
		return altitudeOf( depth ) != Integer.MIN_VALUE;
	}

	/** Is there a slice this many steps up or down from here? */
	public static boolean exists( int altitude ){
		return altitude >= -MAX_BELOW && altitude <= MAX_ABOVE;
	}

	/** Sky overhead: the surface and the mountains; the caves have rock. */
	public static boolean openSky( int altitude ){
		return altitude >= 0;
	}

	/**
	 * The mountain band an elevation belongs to: 0 at or below the mountain
	 * line (the surface owns it), 1..MAX_ABOVE above it. Every slice above the
	 * surface is the set of cells whose band is its altitude; higher bands are
	 * rock through that slice, lower ones open air.
	 */
	public static int band( float elev ){
		if (elev <= BASE) return 0;
		int b = 1 + (int)((elev - BASE) / STEP);
		return Math.min( MAX_ABOVE, b );
	}
}

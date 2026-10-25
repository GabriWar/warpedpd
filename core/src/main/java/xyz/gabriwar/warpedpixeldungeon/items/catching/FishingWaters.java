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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldModel;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The waters a line goes into on the world's slices, and what kind each is (the fish of each:
 * FishSpecies): a RIVER, a LAKE, the SEA, a SWAMP's pools, a hole cut in ICE, a CAVE's pool down in
 * the rock, a mountain TARN up on the peaks. Open water is water or deep water; ice is frozen water,
 * whatever froze it. On the surface the kind is the biome's: a river's channel, a swamp; the sea's
 * biome is the sea where it runs on past LAKE_REACH cells and a lake where it ends within them (lake);
 * a pool anywhere else is still water, a lake's. Pure functions of the world, so the same water is
 * the same kind every time and on every machine.
 */
public final class FishingWaters {

	private FishingWaters(){}

	public enum Kind { RIVER, LAKE, SEA, SWAMP, ICE, CAVE, TARN }

	/** Open water a line is cast into. */
	public static boolean openWater( int terrain ){
		return terrain == Terrain.WATER || terrain == Terrain.DEEP_WATER;
	}

	/** Ice a hole is cut in. */
	public static boolean ice( int terrain ){
		return terrain == Terrain.FROZEN_WATER;
	}

	/** The water on a window cell of a slice of the world, or null when there is none to fish. */
	public static Kind kindAt( OverworldLevel ow, int cell ){
		if (cell < 0 || cell >= ow.length()) return null;
		int t = ow.map[cell];
		if (!openWater( t ) && !ice( t )) return null;
		int w = ow.width();
		return kindOf( ow.worldSeed(), ow.altitude(), t, ow.altitude() == 0 ? ow.biomeAtCell( cell ) : null,
				ow.worldX() + cell % w, ow.worldY() + cell / w );
	}

	/**
	 * The kind from the slice (its altitude: the caves below, the peaks above), the terrain, the
	 * surface's biome on the cell and, for the sea's biome, whether the water there is a lake. Null
	 * for anything that is no water.
	 */
	public static Kind kindOf( long seed, int altitude, int terrain, WorldModel.Biome biome, int wx, int wy ){
		if (ice( terrain )) return Kind.ICE;
		if (!openWater( terrain )) return null;
		if (altitude < 0) return Kind.CAVE;
		if (altitude > 0) return Kind.TARN;
		if (biome == null) return Kind.LAKE;
		switch (biome){
			case RIVER: return Kind.RIVER;
			case SWAMP: return Kind.SWAMP;
			case OCEAN: return lake( seed, wx, wy ) ? Kind.LAKE : Kind.SEA;
			//a pool away from the great waters: still water
			default:    return Kind.LAKE;
		}
	}

	/** The lake test's step (cells between its samples) and how far it looks, each way. */
	public static final int LAKE_STEP = 6, LAKE_REACH = 168;

	//the last answers, by the sample square the cell is in
	private static final int CACHED = 64;
	private static final Map<Long, Boolean> answers = new LinkedHashMap<Long, Boolean>( 16, 0.75f, true ){
		@Override
		protected boolean removeEldestEntry( Map.Entry<Long, Boolean> eldest ){
			return size() > CACHED;
		}
	};

	/**
	 * Is the sea's water at (wx, wy) a lake - a body of water that ends within LAKE_REACH cells every
	 * way - rather than the sea? The water is followed outwards on the world's own samples every
	 * LAKE_STEP cells, at the year's mean (WorldModel.elevationOf: the elevation alone, no season),
	 * from the samples at the corners of its square: should it run out to the edge of the search it
	 * is the sea. A river's channel stops the search: a lake a river feeds is still a lake. A pool too
	 * small to hold a sample is a lake.
	 */
	public static boolean lake( long seed, int wx, int wy ){
		int gx0 = Math.floorDiv( wx, LAKE_STEP ), gy0 = Math.floorDiv( wy, LAKE_STEP );
		long key = seed * 0x9E3779B97F4A7C15L ^ ((long) gx0 << 32) ^ (gy0 & 0xFFFFFFFFL);
		synchronized (answers){
			Boolean known = answers.get( key );
			if (known != null) return known;
		}
		boolean lake = search( seed, gx0, gy0 );
		synchronized (answers){
			answers.put( key, lake );
		}
		return lake;
	}

	private static boolean search( long seed, int gx0, int gy0 ){
		int r = LAKE_REACH / LAKE_STEP, n = 2 * r + 1;
		//0 not looked at, 1 water, 2 land (or a river)
		byte[] seen = new byte[n * n];
		ArrayDeque<Integer> open = new ArrayDeque<>();
		WorldModel.Sample s = new WorldModel.Sample();
		for (int dy = 0; dy <= 1; dy++){
			for (int dx = 0; dx <= 1; dx++){
				int i = r + dx, j = r + dy;
				boolean water = sea( seed, (gx0 + dx) * LAKE_STEP, (gy0 + dy) * LAKE_STEP, s );
				seen[i + j * n] = (byte)(water ? 1 : 2);
				if (water) open.add( i + j * n );
			}
		}
		int[] di = { 1, -1, 0, 0 }, dj = { 0, 0, 1, -1 };
		while (!open.isEmpty()){
			int k = open.poll();
			int i = k % n, j = k / n;
			if (i == 0 || j == 0 || i == n - 1 || j == n - 1) return false;
			for (int d = 0; d < 4; d++){
				int ni = i + di[d], nj = j + dj[d], nk = ni + nj * n;
				if (seen[nk] != 0) continue;
				boolean water = sea( seed, (gx0 + ni - r) * LAKE_STEP, (gy0 + nj - r) * LAKE_STEP, s );
				seen[nk] = (byte)(water ? 1 : 2);
				if (water) open.add( nk );
			}
		}
		return true;
	}

	//open water of the sea's biome at the year's mean: under the sea line and no river's channel
	//(WorldModel.sample's own test, without the edge's dither)
	private static boolean sea( long seed, int wx, int wy, WorldModel.Sample s ){
		float elev = WorldModel.elevationOf( seed, wx, wy, s );
		if (elev >= WorldModel.SEA) return false;
		return !(s.river > 0.35f && s.elevRaw > WorldModel.SEA + 0.03f);
	}
}

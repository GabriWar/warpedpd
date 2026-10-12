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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;

/**
 * Where the world's WAYSTONES stand: one in the town, beside its plaza (TOWN_CELL of its layout), and
 * one in every settlement of MIN_HOUSES houses or more (a town, a city, a metropolis; whoever lives
 * there - people, gnolls or outlaws), on a free cell of the ring right round its well (the
 * settlement's centre), the ring's cells tried in an order hashed from the world seed and the
 * settlement - the second ring after, clear of the guards' posts - so the same world always puts it
 * in the same place. A waystone is known by its
 * KEY: its settlement's sector (WorldStructures.sectorOf), or TOWN.
 *
 * TravelSlice puts them down, once each, as the surface's windows are laid.
 */
public final class Waystones {

	private Waystones(){}

	/** The key of the town's own waystone (no settlement's sector is ever this). */
	public static final long TOWN = Long.MIN_VALUE;
	/** The fewest houses a settlement keeps a waystone with. */
	public static final int MIN_HOUSES = 9;
	/** The town's waystone: two cells east of the plaza, on open ground the town's art leaves bare. */
	public static final int TOWN_CELL = WorldStructures.TOWN_PLAZA + 2;

	/** Does the settlement of this sector keep a waystone: one of MIN_HOUSES houses or more? (A
	 *  bandit camp's too: its outlaws must be dealt with before the hero can travel from it.) */
	public static boolean keepsOne( long seed, int sx, int sy ){
		return WorldStructures.siteType( seed, sx, sy ) == WorldStructures.Site.VILLAGE
				&& houses( seed, sx, sy ) >= MIN_HOUSES;
	}

	/** How many houses the settlement of a sector has. */
	public static int houses( long seed, int sx, int sy ){
		//the settlements of the sector the well stands in: that sector's own, if it has one
		for (int[] s : WorldStructures.settlementsNear( seed, WorldStructures.siteX( seed, sx, sy ),
				WorldStructures.siteY( seed, sx, sy ), null, 0, 0 )){
			if (s[0] == sx && s[1] == sy) return s[4];
		}
		return 0;
	}

	/** The name of the place a waystone stands in: the town's, or its settlement's. */
	public static String placeName( long seed, long key ){
		if (key == TOWN) return Messages.get( Waystones.class, "town" );
		return WorldStructures.villageName( seed, (int)(key >> 32), (int)key );
	}

	public static int townWX(){
		return WorldStructures.townWorldX( TOWN_CELL );
	}

	public static int townWY(){
		return WorldStructures.townWorldY( TOWN_CELL );
	}

	//the cells round a well, nearest first: the ring of 8, then the ring of 16 without the four
	//guard posts (OverworldLevel.populateSettlement: two cells out at NEIGHBOURS8 0, 2, 4, 6), each
	//ring started at a place hashed from the world and the settlement
	static int[][] candidates( long seed, int sx, int sy ){
		int[][] ring1 = ring( 1 ), ring2 = ring( 2 );
		long h = mix( seed, sx, sy );
		int[][] out = new int[ring1.length + ring2.length - 4][];
		int n = 0;
		int s1 = (int)Math.floorMod( h, ring1.length );
		for (int i = 0; i < ring1.length; i++) out[n++] = ring1[(s1 + i) % ring1.length];
		int s2 = (int)Math.floorMod( h >>> 16, ring2.length );
		for (int i = 0; i < ring2.length; i++){
			int[] d = ring2[(s2 + i) % ring2.length];
			if (guardPost( d[0], d[1] )) continue;
			out[n++] = d;
		}
		return out;
	}

	//the offsets of the square ring at a distance, clockwise from the north-west corner
	private static int[][] ring( int r ){
		int[][] out = new int[8 * r][];
		int n = 0;
		for (int dx = -r; dx < r; dx++) out[n++] = new int[]{ dx, -r };
		for (int dy = -r; dy < r; dy++) out[n++] = new int[]{ r, dy };
		for (int dx = r; dx > -r; dx--) out[n++] = new int[]{ dx, r };
		for (int dy = r; dy > -r; dy--) out[n++] = new int[]{ -r, dy };
		return out;
	}

	private static boolean guardPost( int dx, int dy ){
		return (dx == -2 && dy == -2) || (dx == 2 && dy == -2) || (dx == 2 && dy == 0) || (dx == 0 && dy == 2);
	}

	private static long mix( long seed, int sx, int sy ){
		long h = seed ^ 0x3A757073L;
		h ^= sx * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft( h, 29 );
		h ^= sy * 0xC2B2AE3D27D4EB4FL;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		return h;
	}

	/** The window cell a settlement's waystone goes on now, or -1 when none of its cells will take
	 *  one (or they lie off the window's populated band). */
	static int settlementCell( OverworldLevel level, int sx, int sy ){
		long seed = level.worldSeed;
		int cx = WorldStructures.siteX( seed, sx, sy ), cy = WorldStructures.siteY( seed, sx, sy );
		for (int[] d : candidates( seed, sx, sy )){
			int c = takes( level, cx + d[0], cy + d[1] );
			if (c != -1) return c;
		}
		return -1;
	}

	/** The window cell the town's waystone goes on now (its own, else the nearest free one within
	 *  two), or -1. */
	static int townCell( OverworldLevel level ){
		int c = takes( level, townWX(), townWY() );
		if (c != -1) return c;
		for (int r = 1; r <= 2; r++){
			for (int[] d : ring( r )){
				c = takes( level, townWX() + d[0], townWY() + d[1] );
				if (c != -1 && level.inTown( c )) return c;
			}
		}
		return -1;
	}

	//the window cell of a world cell when a waystone may stand there now: inside the window's
	//populated band (where the settlements' folk are put down), open dry ground, no way between the
	//slices, nothing standing or lying on it; -1 otherwise
	private static int takes( OverworldLevel level, int wx, int wy ){
		int c = level.localCell( wx, wy );
		if (c == -1) return -1;
		int w = level.width(), x = c % w, y = c / w;
		if (x < 2 || y < 2 || x > w - 3 || y > level.height() - 3) return -1;
		int t = level.map[c];
		if (!level.passable[c] || level.pit[c] || (Terrain.flags[t] & Terrain.LIQUID) != 0
				|| t == Terrain.FROZEN_WATER || t == Terrain.BRIDGE || Terrain.homestead( t )
				|| t == Terrain.ENTRANCE || t == Terrain.EXIT || t == Terrain.DOOR || t == Terrain.OPEN_DOOR
				|| level.getTransition( c ) != null || level.heaps.get( c ) != null) return -1;
		for (Mob m : level.mobs) if (m.pos == c) return -1;
		return c;
	}

	/** Puts a mob down on a slice's window cell: through the scene when the slice is on show, else
	 *  straight into its mobs (a slice being laid, or read back from its save). */
	static void put( OverworldLevel level, Mob m, int cell ){
		m.pos = cell;
		if (Dungeon.level == level && Game.scene() instanceof GameScene) GameScene.add( m );
		else level.mobs.add( m );
	}
}

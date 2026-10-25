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

package xyz.gabriwar.warpedpixeldungeon.bounty;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.StoreSection;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEvents;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import java.util.HashSet;

/**
 * The notice boards of a slice (the surface's: no other slice has a village) - which have been put
 * down - and the bounties' turn. Each window laid on the surface (built, read back, slid, jumped to,
 * re-seasoned) puts down the board of every village of people (Bounties.keepsBoard) whose well and
 * the two rings round it lie inside the window's populated band and that has none yet: exactly once
 * each, ever. A board is an NPC, so from then on the slice keeps it, parking it with the window and
 * putting it back like the villagers. Saved with the slice, so the record and the boards are always
 * saved together. Every hook runs on the host alone.
 *
 * Where it stands (candidates): beside the well, on its second ring, the cells taken by the guards'
 * posts by day and the corners by the carpenter's stall (homestead/gathering/Carpentry) left out,
 * started at a place hashed from the world and the village; failing those, on the ring right round
 * the well. The waystone of a big village is put down first (HomesteadStore.SECTIONS keeps the travel
 * section before this one), and nothing is ever put on a cell anyone stands on.
 */
public class BountySlice extends StoreSection {

	//the keys (sector keys) of the villages whose board this slice has put down
	private final HashSet<Long> placed = new HashSet<>();

	private static final long CELL_SALT = 0xB0A7DCE11L;
	//the guards' posts by day (VillageRoutine.GUARD_POSTS) and the second ring's corners, where the
	//carpenter's goods are laid round his stall at (±3, ±3)
	private static final int[][] SKIP = { {-2,-2}, {2,-2}, {2,0}, {0,2}, {2,2}, {-2,2} };

	@Override
	public void materialize( OverworldLevel level ){
		if (level.altitude() != 0 || level.isMirror()) return;
		long seed = level.worldSeed;
		//a board already standing (a save that knew it before this record did) is counted, not doubled
		for (Mob m : level.mobs) if (m instanceof NoticeBoard) placed.add( ((NoticeBoard) m).key );

		int w = level.width(), h = level.height();
		int sx0 = Math.floorDiv( level.worldX, WorldStructures.SECTOR ) - 1;
		int sy0 = Math.floorDiv( level.worldY, WorldStructures.SECTOR ) - 1;
		int sx1 = Math.floorDiv( level.worldX + w, WorldStructures.SECTOR ) + 1;
		int sy1 = Math.floorDiv( level.worldY + h, WorldStructures.SECTOR ) + 1;
		for (int sy = sy0; sy <= sy1; sy++){
			for (int sx = sx0; sx <= sx1; sx++){
				long key = WorldStructures.sectorOf( sx, sy );
				if (placed.contains( key ) || !Bounties.keepsBoard( seed, sx, sy )) continue;
				//its well and both rings round it inside the band the settlements' folk are put down in
				int cx = WorldStructures.siteX( seed, sx, sy ) - level.worldX;
				int cy = WorldStructures.siteY( seed, sx, sy ) - level.worldY;
				if (cx < 4 || cy < 4 || cx >= w - 4 || cy >= h - 4) continue;
				int c = cellFor( level, sx, sy );
				if (c != -1) put( level, key, c );
			}
		}
	}

	@Override
	public void worldTurn( OverworldLevel level ){
		BountyActions.tick( level );
	}

	/** The cells round a well a board may stand on, nearest the hashed start first: the second ring
	 *  but the guards' posts and the carpenter's corners, then the first ring (offsets from the well). */
	public static int[][] candidates( long seed, int sx, int sy ){
		int[][] ring2 = ring( 2 ), ring1 = ring( 1 );
		long h = WorldEvents.mix( seed ^ CELL_SALT, sx, sy );
		int[][] out = new int[ring2.length - SKIP.length + ring1.length][];
		int n = 0;
		int s2 = (int) Math.floorMod( h, (long) ring2.length );
		for (int i = 0; i < ring2.length; i++){
			int[] d = ring2[(s2 + i) % ring2.length];
			if (!skipped( d )) out[n++] = d;
		}
		int s1 = (int) Math.floorMod( h >>> 20, (long) ring1.length );
		for (int i = 0; i < ring1.length; i++) out[n++] = ring1[(s1 + i) % ring1.length];
		return out;
	}

	private static boolean skipped( int[] d ){
		for (int[] s : SKIP) if (s[0] == d[0] && s[1] == d[1]) return true;
		return false;
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

	/** The window cell a village's board goes on now, or -1 when none of its cells takes one. */
	static int cellFor( OverworldLevel level, int sx, int sy ){
		long seed = level.worldSeed;
		int cx = WorldStructures.siteX( seed, sx, sy ), cy = WorldStructures.siteY( seed, sx, sy );
		for (int[] d : candidates( seed, sx, sy )){
			int c = takes( level, cx + d[0], cy + d[1] );
			if (c != -1) return c;
		}
		return -1;
	}

	//the window cell of a world cell when a board may stand there now: open dry ground, no way between
	//the slices, no doorway, nothing standing or lying on it; -1 otherwise
	private static int takes( OverworldLevel level, int wx, int wy ){
		int c = level.localCell( wx, wy );
		if (c == -1) return -1;
		int t = level.map[c];
		if (!level.passable[c] || level.pit[c] || (Terrain.flags[t] & Terrain.LIQUID) != 0
				|| t == Terrain.FROZEN_WATER || t == Terrain.BRIDGE || Terrain.homestead( t )
				|| t == Terrain.ENTRANCE || t == Terrain.EXIT || t == Terrain.DOOR || t == Terrain.OPEN_DOOR
				|| level.getTransition( c ) != null || level.heaps.get( c ) != null) return -1;
		for (Mob m : level.mobs) if (m.pos == c) return -1;
		//nor a hero's cell, who is no mob of the level: a guest's, or the host's - on a level being read
		//back too (an older save left by a well), before it is the game's own and he stands in it
		Hero h = Dungeon.hero;
		if (h != null && h.pos == c && (Dungeon.level == level || Dungeon.level == null)) return -1;
		if (OverworldLevel.remoteHeroDistance( level, c ) == 0) return -1;
		return c;
	}

	private void put( OverworldLevel level, long key, int cell ){
		NoticeBoard board = new NoticeBoard();
		board.key = key;
		board.wx = level.worldX + cell % level.width();
		board.wy = level.worldY + cell / level.width();
		board.place = Bounties.villageName( level.worldSeed, key );
		board.pos = cell;
		if (Dungeon.level == level && Game.scene() instanceof GameScene) GameScene.add( board );
		else level.mobs.add( board );
		placed.add( key );
	}

	/** Has this slice put down the board of a village? */
	public boolean placed( long key ){
		return placed.contains( key );
	}

	private static final String PLACED = "placed";

	@Override
	public void storeInBundle( Bundle bundle ){
		long[] keys = new long[placed.size()];
		int i = 0;
		for (long k : placed) keys[i++] = k;
		bundle.put( PLACED, keys );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		placed.clear();
		if (bundle.contains( PLACED )) for (long k : bundle.getLongArray( PLACED )) placed.add( k );
	}
}

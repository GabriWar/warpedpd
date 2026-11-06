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

package xyz.gabriwar.warpedpixeldungeon.items.artifacts;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The skeleton key's tracker kept Shattered's 26 depths: picking up a key or opening a lock
 * on Sokoban (depth 51) or deeper threw on the spot. Its counts now reach any depth.
 */
public class KeyTrackerDepthTest {

	private int depth;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@After
	public void back(){
		Dungeon.depth = depth;
	}

	@Test
	public void sokobanKeysAreCounted(){
		depth = Dungeon.depth;
		Dungeon.depth = 51;
		SkeletonKey.KeyReplacementTracker tracker = new SkeletonKey.KeyReplacementTracker();

		tracker.processExcessKeys();
		tracker.clearDepth();
		assertEquals( -1, tracker.ironKeysNeeded[51] );

		Bundle bundle = new Bundle();
		tracker.storeInBundle( bundle );
		SkeletonKey.KeyReplacementTracker loaded = new SkeletonKey.KeyReplacementTracker();
		loaded.restoreFromBundle( bundle );
		Dungeon.depth = 112;
		loaded.processExcessKeys();
		assertEquals( -1, loaded.crystalKeysNeeded[112] );
		assertEquals( "a depth Shattered has keeps its count", -1, loaded.goldenKeysNeeded[3] );
	}
}

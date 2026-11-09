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
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Lloyd's beacon kept only the depth: set on the surface (depth 97) and used in a barrow (depth 97,
 * branch 104, a 7x7 floor) it read the surface cell 17334 on the barrow's floor and threw. It
 * keeps the branch now, and on a world slice the world cell, as the beacon of returning does.
 */
public class LloydsBeaconBranchTest {

	private int depth, branch;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
	}

	@After
	public void tearDown(){
		Dungeon.depth = depth;
		Dungeon.branch = branch;
	}

	private static Level floor( int w, int h ){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( w, h );
		return l;
	}

	@Test
	public void aSurfaceBeaconIsNotOnABarrowFloorOfTheSameDepth(){
		LloydsBeacon beacon = new LloydsBeacon();
		beacon.returnDepth = 97;
		beacon.returnBranch = 0;
		beacon.returnPos = 17334;

		Dungeon.depth = 97;
		Dungeon.branch = 104;
		assertEquals( "another branch: a trip, not a cell", -1, beacon.returnCell( floor( 7, 7 ) ) );

		Dungeon.branch = 0;
		Level same = floor( 7, 7 );
		beacon.returnPos = 24;
		assertEquals( "its own floor: its cell", 24, beacon.returnCell( same ) );

		beacon.returnPos = 17334;
		assertEquals( "a cell off the floor (an older save's) is not used", -1, beacon.returnCell( same ) );
	}

	@Test
	public void aWorldBeaconFollowsTheWindow(){
		LloydsBeacon beacon = new LloydsBeacon();
		beacon.returnDepth = 97;
		beacon.returnBranch = 0;
		beacon.returnWorld = true;
		beacon.returnWX = 1010;
		beacon.returnWY = 2020;

		OverworldLevel ow = new OverworldLevel();
		ow.setSize( 64, 64 );
		ow.worldX = 1000;
		ow.worldY = 2000;
		Dungeon.depth = 97;
		Dungeon.branch = 0;
		assertEquals( "the world cell where the window has it now", 10 + 20 * 64, beacon.returnCell( ow ) );

		ow.worldX = 5000;
		assertEquals( "off the window: a far trip", -1, beacon.returnCell( ow ) );
	}
}

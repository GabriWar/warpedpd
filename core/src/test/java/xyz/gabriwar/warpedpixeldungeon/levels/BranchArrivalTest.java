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

package xyz.gabriwar.warpedpixeldungeon.levels;

import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertSame;

/** Coming up out of a branch lands on that branch's own stairs, even when the floor holds two. */
public class BranchArrivalTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Test
	public void aFloorWithTwoBranchStairwaysSendsEachArrivalToItsOwn(){
		Level level = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		level.setSize( 20, 20 );
		level.transitions = new ArrayList<>();
		//the temple's stairs listed first, the smith's mines second, as on depth 14
		LevelTransition temple = new LevelTransition( level, 5 + 5 * 20, LevelTransition.Type.BRANCH_EXIT,
				14, 3, LevelTransition.Type.BRANCH_ENTRANCE );
		LevelTransition mines = new LevelTransition( level, 14 + 14 * 20, LevelTransition.Type.BRANCH_EXIT,
				14, 1, LevelTransition.Type.BRANCH_ENTRANCE );
		level.transitions.add( temple );
		level.transitions.add( mines );

		assertSame( "out of the mines (branch 1): the mines' stairs", mines,
				level.getTransition( LevelTransition.Type.BRANCH_EXIT, 14, 1 ) );
		assertSame( "out of the temple (branch 3): the temple's", temple,
				level.getTransition( LevelTransition.Type.BRANCH_EXIT, 14, 3 ) );
		assertSame( "from nowhere either leads: the first, as before", temple,
				level.getTransition( LevelTransition.Type.BRANCH_EXIT, 99, 9 ) );
	}
}

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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.features.LevelTransition;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A barrow's floors, built the way the game builds them (Dungeon.newLevel on the barrow's
 * branch), for every region: each has its way in, only the deepest lacks a way down and holds
 * the guardian, and no floor carries anything that grows the hero for good.
 */
public class DelvesFloorsTest {

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
		//Dungeon.init reads the global badges: keep the files a run touches in the build folder
		com.watabou.utils.FileUtils.setDefaultFileProperties( com.badlogic.gdx.Files.FileType.Absolute,
				new java.io.File( "build/test-saves" ).getAbsolutePath() + "/" );
	}

	@Test
	public void everyRegionsFloorsBuild(){
		Dungeon.seed = 0xBA770L;
		xyz.gabriwar.warpedpixeldungeon.GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.init();

		//every region, short and long: a long one runs on through the regions below it
		int[][] shapes = { {0, 4}, {1, 6}, {2, 9}, {3, 4}, {4, 4}, {0, 15}, {4, 15} };
		for (int k = 0; k < shapes.length; k++){
			int region = shapes[k][0];
			Delves.Entry e = Delves.open( Dungeon.seed, k, 7 );
			e.region = region;
			e.floors = shapes[k][1];
			e.level = 1 + region * 5 + (region == 4 ? 40 : 0);
			Dungeon.branch = e.branch;
			for (int f = 0; f < e.floors; f++){
				int depth = e.depthOf( f );
				Dungeon.depth = depth;
				String what = "barrow " + k + " region " + region + " floor " + f + " depth " + depth;
				Level level = Dungeon.newLevel();
				assertFalse( what, level instanceof DeadEndLevel );
				assertNotNull( what + ": no way in", level.getTransition( LevelTransition.Type.REGULAR_ENTRANCE ) );

				//its stairs lead to the barrow's floors above and below it, skipping the boss depths
				boolean down = false;
				for (LevelTransition t : level.transitions){
					if (t.type == LevelTransition.Type.REGULAR_EXIT || t.type == LevelTransition.Type.BRANCH_EXIT){
						down = true;
						assertEquals( what + ": way down", e.depthOf( f + 1 ), t.destDepth );
						assertEquals( what, e.branch, t.destBranch );
					} else if (f > 0){
						assertEquals( what + ": way up", e.depthOf( f - 1 ), t.destDepth );
					}
				}
				int guardians = 0;
				for (Mob m : level.mobs) if (m.buff( Delves.Guardian.class ) != null) guardians++;
				if (depth == e.lastDepth()){
					assertFalse( what + ": the deepest floor has a way down", down );
					assertEquals( what, 1, guardians );
				} else {
					assertTrue( what + ": no way down", down );
					assertEquals( what, 0, guardians );
				}
				for (Heap heap : level.heaps.valueList()){
					for (Item item : heap.items){
						assertFalse( what + ": " + item.getClass().getSimpleName(), Delves.banned( item ) );
					}
				}

				//the guardian's death pays out a hoard chest and seals the barrow
				if (depth == e.lastDepth()){
					Dungeon.level = level;
					Mob guardian = null;
					for (Mob m : level.mobs) if (m.buff( Delves.Guardian.class ) != null) guardian = m;
					assertFalse( e.cleared );
					Delves.onDeath( guardian );
					assertTrue( what, e.cleared );
					Heap hoard = level.heaps.get( guardian.pos );
					assertNotNull( what + ": no hoard", hoard );
					assertEquals( Heap.Type.CHEST, hoard.type );
					assertTrue( what + ": a thin hoard", hoard.items.size() >= 3 );
					for (Item item : hoard.items){
						assertFalse( what + ": cursed " + item.getClass().getSimpleName(), item.cursed );
					}
					Dungeon.level = null;
				}
			}
		}
		Dungeon.branch = 0;
		Delves.reset();
	}
}

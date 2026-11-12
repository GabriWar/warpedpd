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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/** Every run meets the troll blacksmith exactly once, on depth 12, 13 or 14. */
public class BlacksmithEveryRunTest {

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Test
	public void oneSmithOnTheCavesFloors(){
		ArrayList<String> missing = new ArrayList<>();
		int[] at = new int[15];
		for (long seed = 1; seed <= 40; seed++){
			GamesInProgress.selectedClass = HeroClass.WARRIOR;
			Dungeon.seed = seed * 7919L;
			Dungeon.init();
			int smiths = 0;
			for (int d = 11; d <= 14; d++){
				Dungeon.depth = d;
				Dungeon.branch = 0;
				Actor.clear();
				Level level = Dungeon.newLevel();
				for (Mob m : level.mobs) if (m instanceof Blacksmith){ smiths++; at[d]++; }
			}
			if (smiths != 1) missing.add( "seed " + seed + ": " + smiths + " smiths" );
		}
		System.out.println( "smith on 12/13/14: " + at[12] + "/" + at[13] + "/" + at[14] + "  bad: " + missing );
		assertEquals( "runs without exactly one smith: " + missing, 0, missing.size() );
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.ShadowYog;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The infested arena: ten shadow yogs come in when the hero first steps out of the entrance room,
 * on cells out of his sight. With the whole arena in sight the hunt for one never ended and the
 * turn loop froze on the step out (1 report, 11370: "one act() running for 20s", depth 35).
 */
public class InfestArenaTest {

	private int depth, branch;
	private Level saved;
	private Hero savedHero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.depth = 35;
		Dungeon.branch = 0;
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	//the arena as Level.create builds it, without its loot or guide pages
	private static InfestBossLevel arena(){
		InfestBossLevel l = new InfestBossLevel();
		do {
			l.transitions = new ArrayList<>();
			l.mobs = new HashSet<>();
			l.heaps = new SparseArray<>();
			l.blobs = new HashMap<>();
			l.plants = new SparseArray<>();
			l.traps = new SparseArray<>();
			l.customTiles = new ArrayList<>();
			l.customTerrain = new ArrayList<>();
			l.customWalls = new ArrayList<>();
		} while (!l.build());
		l.buildFlagMaps();
		Dungeon.level = l;
		return l;
	}

	@Test(timeout = 20000)
	public void theYogsComeInWhenTheWholeArenaIsInSight(){
		for (long seed = 1; seed <= 4; seed++){
			Random.pushGenerator( seed );
			try {
				Actor.clear();
				InfestBossLevel l = arena();
				Hero hero = new Hero();
				//a floor cell outside the entrance room and its walls (columns and rows 21 to 27)
				for (int c = 0; c < l.length(); c++){
					int x = c % l.width(), y = c / l.width();
					if (l.map[c] == Terrain.EMPTY && (x < 21 || x > 27 || y < 21 || y > 27)){
						hero.pos = c;
						break;
					}
				}
				Dungeon.hero = hero;
				Actor.add( hero );
				Arrays.fill( l.heroFOV, true );

				l.occupyCell( hero );

				int yogs = 0;
				for (Mob m : l.mobs){
					if (!(m instanceof ShadowYog)) continue;
					yogs++;
					assertTrue( l.passable[m.pos] );
					assertTrue( "seed " + seed + ": a yog on the hero", m.pos != hero.pos );
				}
				assertEquals( "seed " + seed, 10, yogs );
			} finally {
				Random.popGenerator();
			}
		}
	}
}

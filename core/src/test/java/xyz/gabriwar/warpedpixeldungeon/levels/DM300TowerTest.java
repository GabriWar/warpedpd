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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DM300;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Tower;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * DM-300's arena: when the fight starts one tower stands beside the boss (Sprouted stood two),
 * with 130 health, and once it falls nothing keeps DM-300 from being finished.
 */
public class DM300TowerTest {

	private int depth, branch, challenges;
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
		challenges = Dungeon.challenges;
		saved = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.depth = 15;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.hero = new Hero();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.challenges = challenges;
		Dungeon.level = saved;
		Dungeon.hero = savedHero;
	}

	//the arena as Level.create builds it, without the pylons, loot or guide pages
	private static CavesBossLevel arena(){
		CavesBossLevel l = new CavesBossLevel();
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

	private static <T extends Mob> ArrayList<T> all( Level l, Class<T> type ){
		ArrayList<T> found = new ArrayList<>();
		for (Mob m : l.mobs) if (type.isInstance( m )) found.add( type.cast( m ) );
		return found;
	}

	@Test
	public void theFightStandsOneTowerWithHalfItsOldHealth(){
		for (long seed = 1; seed <= 8; seed++){
			Random.pushGenerator( seed );
			try {
				CavesBossLevel l = arena();
				l.spawnFight();

				assertEquals( 1, all( l, DM300.class ).size() );
				ArrayList<Tower> towers = all( l, Tower.class );
				assertEquals( "one tower, not two", 1, towers.size() );

				Tower t = towers.get( 0 );
				assertEquals( 130, t.HT );
				assertEquals( 130, t.HP );
				assertTrue( "on open floor in the arena", l.openSpace[t.pos]
						&& CavesBossLevel.mainArena.inside( l.cellToPoint( t.pos ) ) );
			} finally {
				Random.popGenerator();
			}
		}
	}

	@Test
	public void dm300CanBeFinishedOnceItsTowerFalls(){
		CavesBossLevel l = arena();
		l.spawnFight();
		DM300 boss = all( l, DM300.class ).get( 0 );
		Tower tower = all( l, Tower.class ).get( 0 );

		//both pylons already broken: the last stretch of the fight
		boss.pylonsActivated = boss.totalPylonsToActivate();

		//no ground round the wreck: a dewdrop dropped with no game scene has no sprite to fall with
		for (int n : PathFinder.NEIGHBOURS9) l.passable[tower.pos + n] = false;
		tower.HP = 0;
		tower.die( Dungeon.hero );

		assertTrue( all( l, Tower.class ).isEmpty() );
		assertTrue( l.mobs.contains( boss ) );
		assertTrue( boss.isAlive() );
		assertFalse( "no second tower to wait for: it can be hurt", boss.isInvulnerable( Hero.class ) );

		boss.HP = 0;
		assertFalse( "and at 0 health it is dead", boss.isAlive() );
	}
}

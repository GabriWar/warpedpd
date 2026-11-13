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
import xyz.gabriwar.warpedpixeldungeon.GamesInProgress;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.OtilukesJournal;
import xyz.gabriwar.warpedpixeldungeon.items.journalpages.Sokoban4;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import com.watabou.noosa.Game;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The Dwarf King killed while seated on his throne dropped his journal page and Otiluke's journal
 * (and any ring of wealth or lucky bonus) on the throne itself: a solid cell no one can step on, so
 * the loot could never be picked up. What falls on the throne lands at its foot now, and loot an
 * older version left there is laid at its foot when the floor loads.
 */
public class DwarfKingThroneLootTest {

	private long seed;
	private int depth, branch;
	private Level level;

	@BeforeClass
	public static void boot() throws Exception {
		WorldEventsTest.boot();
	}

	@Before
	public void setUp(){
		seed = Dungeon.seed; depth = Dungeon.depth; branch = Dungeon.branch; level = Dungeon.level;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.seed = 0xD3A2FL;
		Dungeon.init();
	}

	@After
	public void tearDown(){
		Dungeon.seed = seed; Dungeon.depth = depth; Dungeon.branch = branch; Dungeon.level = level;
		Actor.clear();
	}

	private static CityBossLevel throneRoom(){
		Dungeon.depth = 20; Dungeon.branch = 0;
		Actor.clear();
		Dungeon.level = Dungeon.newLevel();
		return (CityBossLevel) Dungeon.level;
	}

	//a cell the hero can stand on and walk to from the floor's entrance
	private static boolean reachable( Level l, int cell ){
		PathFinder.setMapSize( l.width(), l.height() );
		PathFinder.buildDistanceMap( l.entrance(), BArray.or( l.passable, l.avoid, null ) );
		return l.passable[cell] && PathFinder.distance[cell] != Integer.MAX_VALUE;
	}

	@Test
	public void theKingsLootCanBePickedUpWhenHeDiesOnHisThrone(){
		CityBossLevel l = throneRoom();
		assertFalse( "the throne is no cell to stand on", l.passable[CityBossLevel.throne] );

		//what DwarfKing.dropExtraLoot drops where he sits, after his crown
		Item page = new Sokoban4();
		Item journal = new OtilukesJournal();
		Heap h = l.drop( page, CityBossLevel.throne );
		l.drop( journal, CityBossLevel.throne );

		assertNull( "nothing lies on the throne", l.heaps.get( CityBossLevel.throne ) );
		assertTrue( "the page is in a heap the hero can reach", h.items.contains( page ) && reachable( l, h.pos ) );
		Heap withJournal = l.heaps.get( h.pos );
		assertTrue( "and the journal with it", withJournal != null && withJournal.items.contains( journal ) );
		assertEquals( "at the throne's foot, where the crown goes", CityBossLevel.throne + l.width(), h.pos );
	}

	@Test
	public void lootLeftOnTheThroneIsLaidAtItsFootOnLoad(){
		CityBossLevel l = throneRoom();
		Heap stranded = new Heap();
		stranded.pos = CityBossLevel.throne;
		stranded.drop( new Sokoban4() );
		l.heaps.put( CityBossLevel.throne, stranded );
		//something already lying at the foot: the two heaps become one
		Heap foot = new Heap();
		foot.pos = CityBossLevel.throne + l.width();
		foot.drop( new Gold( 30 ) );
		l.heaps.put( foot.pos, foot );

		//saved and loaded as a game of this version (headless the game has no version code)
		int versionCode = Game.versionCode;
		Game.versionCode = WarpedPixelDungeon.v4_0_0;
		CityBossLevel loaded = new CityBossLevel();
		try {
			Bundle b = new Bundle();
			l.storeInBundle( b );
			loaded.restoreFromBundle( b );
		} finally {
			Game.versionCode = versionCode;
		}

		assertNull( "nothing is left on the throne", loaded.heaps.get( CityBossLevel.throne ) );
		Heap atFoot = loaded.heaps.get( CityBossLevel.throne + loaded.width() );
		assertNotNull( atFoot );
		boolean page = false, gold = false;
		for (Item i : atFoot.items){
			page |= i instanceof Sokoban4;
			gold |= i instanceof Gold;
		}
		assertTrue( "the stranded page lies at the foot", page );
		assertTrue( "beside what was there already", gold );
		assertTrue( reachable( loaded, atFoot.pos ) );
	}
}

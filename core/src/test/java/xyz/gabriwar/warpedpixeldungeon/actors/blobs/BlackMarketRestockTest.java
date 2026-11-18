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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import com.watabou.utils.PathFinder;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The black market turned its stock over only as its dealer set up at nightfall. The market trades
 * only at night, so a hero who shopped and left kept the dealer on the floor's saved mobs, and on any
 * later night he came back the dealer was already standing there: nobody set up, and the counter
 * stayed as he had left it, bought bare, however many weeks went by. The stock turns over each
 * week the market trades, whether its dealer is just arriving or still there from a night before.
 */
public class BlackMarketRestockTest {

	private static final int W = 30;

	private int depth, branch, turn, startDay, challenges;
	private DayNightCycle.Phase override;
	private Level savedLevel;
	private Hero savedHero;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		turn = Dungeon.cycleTurn;
		startDay = Dungeon.calendarStartDay;
		challenges = Dungeon.challenges;
		override = DayNightCycle.debugPhaseOverride;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.depth = 3;
		Dungeon.branch = 0;
		Dungeon.challenges = 0;
		Dungeon.calendarStartDay = 0;
		Dungeon.cycleTurn = 100;
		Dungeon.hero = new Hero();
		Dungeon.hero.pos = 1 + W;
		WarpedRooms.initForRun();
		floor();
	}

	@After
	public void tearDown(){
		WarpedRooms.initForRun();
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.cycleTurn = turn;
		Dungeon.calendarStartDay = startDay;
		Dungeon.challenges = challenges;
		DayNightCycle.debugPhaseOverride = override;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	@Test
	public void aDealerLeftFromAnEarlierNightSellsThisWeeksStock(){
		BlackMarket market = Blob.seed( 10 + 10 * W, 1, BlackMarket.class, Dungeon.level );
		market.home = 14 + 14 * W;
		market.posts = new int[]{ 12 + 12 * W, 16 + 12 * W };

		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.NIGHT;
		market.act();
		assertNotNull( "the dealer sets up at night", market.dealer() );
		for (BlackMarket.Offer offer : market.stock) offer.sold = true;

		//the hero leaves at night: the floor keeps the dealer with its mobs. A later night, the
		//same week: what was bought stays bought
		Dungeon.cycleTurn += DayNightCycle.FULL_CYCLE / 10;
		market.act();
		assertFalse( "the week's stock is the week's", unsold( market ) );

		//ten days on he comes back, at night again, and the dealer is still standing there
		Dungeon.cycleTurn += 10 * DayNightCycle.FULL_CYCLE;
		market.act();
		assertNotNull( market.dealer() );
		assertTrue( "a new week has a new stock", unsold( market ) );
	}

	private static boolean unsold( BlackMarket market ){
		for (BlackMarket.Offer offer : market.stock){
			if (!offer.sold) return true;
		}
		return false;
	}

	//open floor walled round
	private static void floor(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( W, W );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		l.transitions = new ArrayList<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int x = 0; x < W; x++) l.map[x] = l.map[x + (W - 1) * W] = Terrain.WALL;
		for (int y = 0; y < W; y++) l.map[y * W] = l.map[W - 1 + y * W] = Terrain.WALL;
		l.buildFlagMaps();
		PathFinder.setMapSize( W, W );
		Dungeon.level = l;
	}
}

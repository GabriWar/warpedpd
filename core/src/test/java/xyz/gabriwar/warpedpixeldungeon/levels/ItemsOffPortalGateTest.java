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
import xyz.gabriwar.warpedpixeldungeon.Portals;
import xyz.gabriwar.warpedpixeldungeon.WarpedPixelDungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldEventsTest;
import com.watabou.noosa.Game;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A shop's portal gate never moves and no one can step onto its cell, so an item that ended up
 * there could never be taken: the weekly restock laid fresh stock on the free cells beside the
 * shopkeeper, the gate's among them, and a throw that struck the gate came down under it. Nothing
 * lands on a gate's cell now (the nearest free floor takes it), and an item an older version left
 * there is laid beside it when the floor loads, as the Dwarf King's throne does with his loot.
 */
public class ItemsOffPortalGateTest {

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
		Dungeon.seed = 0x6A7E5L;
		Dungeon.init();
		Portals.reset();
	}

	@After
	public void tearDown(){
		Portals.reset();
		Dungeon.seed = seed; Dungeon.depth = depth; Dungeon.branch = branch; Dungeon.level = level;
		Actor.clear();
	}

	//depth 6, whose shop's gate stands beside its keeper
	private static Level shopFloor(){
		Dungeon.depth = 6; Dungeon.branch = 0;
		Actor.clear();
		Dungeon.level = Dungeon.newLevel();
		return Dungeon.level;
	}

	private static PortalGate gate( Level l ){
		for (Mob m : l.mobs) if (m instanceof PortalGate) return (PortalGate) m;
		throw new AssertionError( "no gate on the shop's floor" );
	}

	private static Shopkeeper keeper( Level l ){
		for (Mob m : l.mobs) if (m instanceof Shopkeeper) return (Shopkeeper) m;
		throw new AssertionError( "no shopkeeper" );
	}

	//a cell the hero can stand on and walk to from the floor's entrance
	private static boolean reachable( Level l, int cell ){
		PathFinder.setMapSize( l.width(), l.height() );
		PathFinder.buildDistanceMap( l.entrance(), BArray.or( l.passable, l.avoid, null ) );
		return l.passable[cell] && l.findMob( cell ) == null && PathFinder.distance[cell] != Integer.MAX_VALUE;
	}

	@Test
	public void theRestockLaysNothingUnderTheGate(){
		Level l = shopFloor();
		PortalGate gate = gate( l );
		Shopkeeper keeper = keeper( l );
		assertTrue( "the gate stands beside its keeper", l.adjacent( gate.pos, keeper.pos ) );
		assertNull( "the shop's own stock keeps off it", l.heaps.get( gate.pos ) );

		//the stock round the keeper sold, a week's worth of fresh goods at a time comes until his
		//every free neighbour holds some
		for (int n : PathFinder.NEIGHBOURS8) l.heaps.remove( keeper.pos + n );
		for (int week = 0; week < 3; week++) keeper.restock();

		assertNull( "nothing for sale under the gate", l.heaps.get( gate.pos ) );
		int stocked = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = keeper.pos + n;
			if (c == gate.pos || !l.passable[c]) continue;
			Heap h = l.heaps.get( c );
			assertNotNull( "every other free cell beside him has some", h );
			assertEquals( "one thing for sale on each, to be bought", 1, h.size() );
			assertEquals( Heap.Type.FOR_SALE, h.type );
			stocked++;
		}
		assertTrue( stocked > 0 );
	}

	//how far the nearest free floor lies from a cell: open ground no one stands on and nothing lies
	//on (a full shop's stock can take every cell round its gate)
	private static float nearestFree( Level l, int cell ){
		float best = Float.MAX_VALUE;
		for (int i = 0; i < l.length(); i++){
			if (i == cell || !l.passable[i] || l.avoid[i] || l.findMob( i ) != null || l.heaps.get( i ) != null) continue;
			best = Math.min( best, l.trueDistance( cell, i ) );
		}
		return best;
	}

	@Test
	public void aThrowAtTheGateComesDownBesideIt(){
		Level l = shopFloor();
		PortalGate gate = gate( l );
		float near = nearestFree( l, gate.pos );

		//what Item.onThrow drops where a throw strikes the gate
		Heap h = l.drop( new Gold( 25 ), gate.pos );

		assertNull( "nothing lies under the gate", l.heaps.get( gate.pos ) );
		assertNotEquals( gate.pos, h.pos );
		assertEquals( "but on the nearest free floor", near, l.trueDistance( gate.pos, h.pos ), 0.001f );
		assertTrue( "where the hero can pick it up", reachable( l, h.pos ) );
		assertEquals( Heap.Type.HEAP, h.type );
	}

	@Test
	public void whatAnOlderVersionLeftUnderTheGateIsLaidBesideItOnLoad(){
		Level l = shopFloor();
		PortalGate gate = gate( l );
		float near = nearestFree( l, gate.pos );
		HashSet<Integer> before = new HashSet<>();
		for (Heap h : l.heaps.valueList()) before.add( h.pos );
		Heap stranded = new Heap();
		stranded.pos = gate.pos;
		stranded.type = Heap.Type.FOR_SALE;
		stranded.drop( new Food() );
		l.heaps.put( gate.pos, stranded );

		//saved and loaded as a game of this version (headless the game has no version code)
		int versionCode = Game.versionCode;
		Game.versionCode = WarpedPixelDungeon.v4_0_0;
		Level loaded = Reflection.newInstance( l.getClass() );
		try {
			Bundle b = new Bundle();
			l.storeInBundle( b );
			loaded.restoreFromBundle( b );
		} finally {
			Game.versionCode = versionCode;
		}

		PortalGate loadedGate = gate( loaded );
		assertEquals( gate.pos, loadedGate.pos );
		assertNull( "nothing is left under the gate", loaded.heaps.get( loadedGate.pos ) );
		Heap moved = null;
		for (Heap h : loaded.heaps.valueList()){
			if (!before.contains( h.pos )) moved = h;
		}
		assertNotNull( "the stranded heap lies somewhere new", moved );
		assertTrue( moved.peek() instanceof Food );
		assertEquals( "still for sale", Heap.Type.FOR_SALE, moved.type );
		assertEquals( "on the nearest free floor", near, loaded.trueDistance( loadedGate.pos, moved.pos ), 0.001f );
		assertTrue( "a cell the hero can reach", reachable( loaded, moved.pos ) );
	}
}

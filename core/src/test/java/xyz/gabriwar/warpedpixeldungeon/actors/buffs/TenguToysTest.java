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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TenguToysTest {

	private static final int W = 15, MID = 7 * W + 7;

	private Level previous;
	private String previousVersion;
	private Hero previousHero;
	private int previousDepth, previousBranch;
	private Hero rogue;

	@BeforeClass public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before public void setup(){
		previous = Dungeon.level;
		previousVersion = Game.version;
		previousHero = Dungeon.hero;
		previousDepth = Dungeon.depth;
		previousBranch = Dungeon.branch;
		Actor.clear();
		Game.version = "test";
		Level floor = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){}
			@Override protected void createItems(){}
		};
		floor.setSize( W, W );
		floor.traps = new com.watabou.utils.SparseArray<>();
		floor.plants = new com.watabou.utils.SparseArray<>();
		floor.heaps = new com.watabou.utils.SparseArray<>();
		floor.blobs = new java.util.HashMap<>();
		java.util.Arrays.fill( floor.solid, true );
		for (int y = 1; y < W - 1; y++) for (int x = 1; x < W - 1; x++) Level.set( y * W + x, Terrain.EMPTY, floor );
		Dungeon.level = floor;
		Dungeon.depth = 4;
		Dungeon.branch = 0;
		//the rogue who threw them, two steps off
		rogue = new Hero();
		rogue.pos = MID - 2;
		Dungeon.hero = rogue;
	}

	@After public void cleanup(){
		Actor.clear();
		Dungeon.level = previous;
		Dungeon.hero = previousHero;
		Dungeon.depth = previousDepth;
		Dungeon.branch = previousBranch;
		Game.version = previousVersion;
		if (previous != null) PathFinder.setMapSize( previous.width(), previous.height() );
	}

	@Test public void shockerArcsEveryWayTwoTilesOut(){
		ArrayList<Integer> cells = TenguShockBomb.arcCells( MID );
		assertEquals( 16, cells.size() );
		for (int dir : PathFinder.NEIGHBOURS8){
			assertTrue( cells.contains( MID + dir ) );
			assertTrue( cells.contains( MID + 2 * dir ) );
		}
		assertFalse( cells.contains( MID + 2 * W + 1 ) ); //a knight's move is not on any line
	}

	@Test public void shockerArcsStopAtWalls(){
		Level.set( MID + 1, Terrain.WALL, Dungeon.level );
		ArrayList<Integer> cells = TenguShockBomb.arcCells( MID );
		assertFalse( cells.contains( MID + 1 ) );
		assertFalse( cells.contains( MID + 2 ) );
		assertEquals( 14, cells.size() );
	}

	@Test public void fireBombReachesTwoTilesOnlyAtMastery(){
		assertEquals( 9, TenguFireBomb.fireCells( MID, 2 ).size() );
		ArrayList<Integer> mastered = TenguFireBomb.fireCells( MID, 3 );
		assertEquals( 25, mastered.size() );
		assertTrue( mastered.contains( MID + 2 * W + 2 ) );
	}

	/** a toy of `kind` counting down on `cell`, as a save brings it back: no scene needed */
	private <T extends Buff> T ticking( Class<T> kind, int cell ){
		T bomb = com.watabou.utils.Reflection.newInstance( kind );
		Bundle b = new Bundle();
		b.put( "cell", cell );
		b.put( "depth", Dungeon.depth );
		b.put( "branch", Dungeon.branch );
		b.put( "fuse", 2 );
		b.put( "turns", 3 );
		b.put( "level", 1 );
		bomb.restoreFromBundle( b );
		assertTrue( bomb.attachTo( rogue ) );
		return bomb;
	}

	private static Item[] toys(){
		return new Item[]{ new TenguSmokeBomb.RogueBombItem(), new TenguFireBomb.RogueFireBombItem(),
				new TenguShockBomb.RogueShockerItem() };
	}

	@Test public void aToyStaysPutWhileItsFuseRuns(){
		ticking( TenguSmokeBomb.class, MID );
		ticking( TenguFireBomb.class, MID + 1 );
		ticking( TenguShockBomb.class, MID + 2 );
		Item[] toys = toys();
		for (int i = 0; i < toys.length; i++){
			Dungeon.level.drop( toys[i], MID + i );
			rogue.pos = MID + i;
			assertFalse( toys[i].name(), toys[i].doPickUp( rogue, MID + i ) );
			assertTrue( Dungeon.level.heaps.get( MID + i ).items.contains( toys[i] ) );
		}
	}

	@Test public void aToyWithNoFuseLeftComesAwayAndIsNotKept(){
		//a toy lying where no bomb of its kind still counts down: its bomb went off elsewhere, or
		//burnt out when the rogue left the floor. It used to lie there for good, and on a stair
		//or a doorway he could never take it, as nothing he could not pick up is moved off one
		Item[] toys = toys();
		for (int i = 0; i < toys.length; i++){
			Heap heap = Dungeon.level.drop( toys[i], MID + i );
			rogue.pos = MID + i;
			assertTrue( toys[i].name(), toys[i].doPickUp( rogue, MID + i ) );
			heap.pickUp();
			assertTrue( heap.isEmpty() );
			assertTrue( rogue.belongings.backpack.items.isEmpty() );
		}
		assertEquals( "Spent, the shocker comes apart in your hands.",
				xyz.gabriwar.warpedpixeldungeon.messages.Messages.get( TenguToy.class, "spent", toys[2].name() ) );
	}

	@Test public void aToyLeftOnAFloorTheRogueLeftComesAway(){
		//thrown, then the rogue went down the stairs before it went off: the bomb fizzles there...
		TenguShockBomb shock = ticking( TenguShockBomb.class, MID );
		Item toy = new TenguShockBomb.RogueShockerItem();
		Dungeon.level.drop( toy, MID );
		Dungeon.depth++;
		shock.act();
		assertTrue( rogue.buffs( TenguShockBomb.class ).isEmpty() );
		//...and back up, its toy is still there, and comes away
		Dungeon.depth--;
		rogue.pos = MID;
		assertTrue( toy.doPickUp( rogue, MID ) );
	}

	@Test public void aToyMovedOffItsCellComesAway(){
		//the bomb counts down on its own cell; a toy that ended up elsewhere is nobody's
		ticking( TenguFireBomb.class, MID );
		Item toy = new TenguFireBomb.RogueFireBombItem();
		Dungeon.level.drop( toy, MID + 1 );
		rogue.pos = MID + 1;
		assertTrue( toy.doPickUp( rogue, MID + 1 ) );
	}

	@Test public void aToyOnTheGroundWithNoBombLeftIsSweptAway(){
		//a ticking toy stays; the one its rogue left mid-fuse is gone when he comes back to the floor
		ticking( TenguSmokeBomb.class, MID );
		Item live = new TenguSmokeBomb.RogueBombItem();
		Dungeon.level.drop( live, MID );
		TenguShockBomb shock = ticking( TenguShockBomb.class, MID + 2 );
		Item spent = new TenguShockBomb.RogueShockerItem();
		Dungeon.level.drop( spent, MID + 2 );
		Dungeon.depth++;
		shock.act();
		Dungeon.depth--;
		TenguToy.sweep( Dungeon.level );
		assertTrue( Dungeon.level.heaps.get( MID ).items.contains( live ) );
		assertTrue( Dungeon.level.heaps.get( MID + 2 ) == null || !Dungeon.level.heaps.get( MID + 2 ).items.contains( spent ) );
	}

	@Test public void aBombFollowsItsGroundAsTheWindowSlides(){
		TenguFireBomb fire = ticking( TenguFireBomb.class, MID );
		fire.slide( 1, 1, W, W );
		assertTrue( fire.ticksOn( MID - 1 - W ) );
		assertFalse( fire.ticksOn( MID ) );
		//slid out of the window: it fizzles, and its toy is swept away
		TenguSmokeBomb smoke = ticking( TenguSmokeBomb.class, W + 1 );
		Item toy = new TenguSmokeBomb.RogueBombItem();
		Dungeon.level.drop( toy, W + 1 );
		smoke.slide( 2, 0, W, W );
		assertTrue( rogue.buffs( TenguSmokeBomb.class ).isEmpty() );
		TenguToy.sweep( Dungeon.level );
		assertTrue( Dungeon.level.heaps.get( W + 1 ) == null );
	}
}

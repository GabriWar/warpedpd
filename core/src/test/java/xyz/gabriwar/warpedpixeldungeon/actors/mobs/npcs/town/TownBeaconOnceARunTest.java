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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.town;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.NPC;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Tinkerer5;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.TownGuard;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.TownReturnBeacon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MobSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The guard and the villager handed a town return beacon to whoever talked to them first, but the
 * town's people are not kept from one day to the next: each evening they walk off to the inn and
 * each morning TownCommute brings out a fresh one of each, who had never handed anything over. So
 * every day of the run each of them gave another beacon. The town hands its beacon out once a run,
 * as Sprouted's villager does, whichever of the two the hero talks to first. The beacon itself works
 * where Sprouted's does, and its description now says where that is.
 */
public class TownBeaconOnceARunTest {

	private static final int W = 8, H = 8;
	private static final int HERO = 3 + 3 * W;

	private int depth, branch;
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
		override = DayNightCycle.debugPhaseOverride;
		savedLevel = Dungeon.level;
		savedHero = Dungeon.hero;
		Actor.clear();
		Dungeon.depth = 97;
		Dungeon.branch = 0;
		DayNightCycle.debugPhaseOverride = DayNightCycle.Phase.DAY;
		TownLedger.reset();
		floor();
		Hero hero = new Hero();
		hero.pos = HERO;
		Dungeon.hero = hero;
	}

	@After
	public void tearDown(){
		TownLedger.reset();
		Actor.clear();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		DayNightCycle.debugPhaseOverride = override;
		Dungeon.level = savedLevel;
		Dungeon.hero = savedHero;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	@Test
	public void theGuardHandsOutOneBeaconARun(){
		talk( TownGuard.class );
		assertEquals( "the first word with the guard", 1, beacons() );
		//the next morning: TownCommute brings out a new guard
		talk( TownGuard.class );
		talk( TownGuard.class );
		assertEquals( "no beacon on the days after", 1, beacons() );
	}

	@Test
	public void theVillagerHandsOutOneBeaconARun(){
		talk( Tinkerer5.class );
		assertEquals( "the first word with the villager", 1, beacons() );
		talk( Tinkerer5.class );
		assertEquals( "no beacon on the days after", 1, beacons() );
	}

	@Test
	public void theTownHasOneBeaconBetweenTheTwo(){
		talk( TownGuard.class );
		talk( Tinkerer5.class );
		assertEquals( "the villager has none once the guard gave his", 1, beacons() );
	}

	@Test
	public void theHandoutIsSavedWithTheRun(){
		talk( TownGuard.class );
		Bundle bundle = new Bundle();
		TownLedger.storeInBundle( bundle );
		TownLedger.reset();
		TownLedger.restoreFromBundle( bundle );
		talk( Tinkerer5.class );
		assertEquals( "a loaded run remembers it", 1, beacons() );
	}

	//as Sprouted's: it takes the hero home from past the town (the mines, Zot's floor, the
	//dragon cave), and from the town back down; in the dungeon and on the world it does nothing
	@Test
	public void theBeaconWorksOnlyPastTheTown(){
		TownReturnBeacon beacon = new TownReturnBeacon();
		Dungeon.depth = 5;
		assertFalse( beacon.actions( Dungeon.hero ).contains( TownReturnBeacon.AC_RETURNTOWN ) );
		Dungeon.depth = 97;
		assertFalse( beacon.actions( Dungeon.hero ).contains( TownReturnBeacon.AC_RETURNTOWN ) );
		Dungeon.depth = 60;
		assertTrue( beacon.actions( Dungeon.hero ).contains( TownReturnBeacon.AC_RETURNTOWN ) );
	}

	// ------------------------------------------------------------------ helpers

	//a word with a townsperson as TownCommute makes them each morning: a fresh instance
	private static void talk( Class<? extends NPC> cls ){
		NPC npc = Reflection.newInstance( cls );
		npc.pos = HERO + 1;
		npc.sprite = stub();
		Dungeon.level.mobs.add( npc );
		npc.interact( Dungeon.hero );
		Dungeon.level.mobs.remove( npc );
	}

	private static int beacons(){
		int n = 0;
		for (Item i : Dungeon.level.heaps.get( HERO ).items){
			if (i instanceof TownReturnBeacon) n++;
		}
		return n;
	}

	//a sprite that draws nothing
	private static MobSprite stub(){
		MobSprite sp = new MobSprite(){
			@Override public void turnTo( int from, int to ){ }
			@Override public void update(){ }
		};
		sp.visible = false;
		return sp;
	}

	//open floor walled round, a heap with a sprite under the hero for what is dropped at his feet
	private static void floor(){
		Level l = new Level(){
			@Override protected boolean build(){ return true; }
			@Override protected void createMobs(){ }
			@Override protected void createItems(){ }
		};
		l.setSize( W, H );
		l.mobs = new HashSet<>();
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.plants = new SparseArray<>();
		l.traps = new SparseArray<>();
		l.customTiles = new ArrayList<>();
		l.customTerrain = new ArrayList<>();
		l.customWalls = new ArrayList<>();
		Arrays.fill( l.map, Terrain.EMPTY );
		for (int x = 0; x < W; x++) l.map[x] = l.map[x + (H - 1) * W] = Terrain.WALL;
		for (int y = 0; y < H; y++) l.map[y * W] = l.map[W - 1 + y * W] = Terrain.WALL;
		l.buildFlagMaps();
		PathFinder.setMapSize( W, H );
		Dungeon.level = l;

		Heap heap = new Heap();
		heap.pos = HERO;
		//it draws nothing, and there is no scene to drop anything into
		heap.sprite = new ItemSprite(){
			@Override public void place( int cell ){ }
			@Override public void drop(){ }
		};
		l.heaps.put( HERO, heap );
	}
}

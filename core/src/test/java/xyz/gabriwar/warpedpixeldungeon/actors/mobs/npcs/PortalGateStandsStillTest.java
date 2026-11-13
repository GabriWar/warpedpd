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
import xyz.gabriwar.warpedpixeldungeon.Portals;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Corruption;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Dread;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drowsy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MagicalSleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Sleep;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * A scroll of lullaby and then a scroll of rage in a shop brought its portal gate to life: the
 * lullaby put it to sleep, the rage's amok woke it hunting (Mob.add), and it walked off round the
 * floor until another lullaby stopped it. Terror sent it running the same way, and the caged kobold
 * of the frozen halls went walking with any of them. No spell takes hold of either now, and a gate a
 * spell set walking in an older save goes back to its shop's cell.
 */
public class PortalGateStandsStillTest {

	private static final int W = 12, H = 12;

	//every spell that sends a mob into another frame of mind, or holds it where it stands
	private static final List<Class<? extends Buff>> SPELLS = Arrays.asList(
			Drowsy.class, MagicalSleep.class, Sleep.class, Amok.class, Terror.class, Dread.class,
			Charm.class, Paralysis.class, Corruption.class, Vertigo.class );

	private int depth, branch;
	private Level savedLevel;

	@BeforeClass
	public static void boot(){
		WarpedRoomsTest.boot();
	}

	@Before
	public void setUp(){
		depth = Dungeon.depth;
		branch = Dungeon.branch;
		savedLevel = Dungeon.level;
		Actor.clear();
		Portals.reset();
	}

	@After
	public void tearDown(){
		Actor.clear();
		Portals.reset();
		Dungeon.depth = depth;
		Dungeon.branch = branch;
		Dungeon.level = savedLevel;
		if (savedLevel != null) PathFinder.setMapSize( savedLevel.width(), savedLevel.height() );
	}

	@Test
	public void noSpellTakesHoldOfAShopGate(){
		standsThroughEverySpell( new PortalGate() );
	}

	@Test
	public void noSpellTakesHoldOfTheCagedKobold(){
		standsThroughEverySpell( new CagedKobold() );
	}

	private static void standsThroughEverySpell( Mob m ){
		for (Class<? extends Buff> spell : SPELLS){
			Buff.affect( m, spell );
			assertNull( "no " + spell.getSimpleName() + " on it", m.buff( spell ) );
			assertSame( "it stays passive through " + spell.getSimpleName(), m.PASSIVE, m.state );
			assertEquals( "nothing holds it", 0, m.paralysed );
		}
	}

	@Test
	public void aGateASpellSetWalkingGoesBackToItsShop(){
		floor();
		Dungeon.depth = 11;
		Dungeon.branch = 0;
		int home = 3 + 3 * W;
		Portals.register( Portals.keyHere(), home );
		PortalGate gate = new PortalGate();
		gate.pos = 8 + 7 * W;

		gate.returnToShop();
		assertEquals( "back on its shop's cell", home, gate.pos );
	}

	@Test
	public void aGateWaitsWhileItsCellIsTaken(){
		floor();
		Dungeon.depth = 11;
		Dungeon.branch = 0;
		int home = 3 + 3 * W, stray = 8 + 7 * W;
		Portals.register( Portals.keyHere(), home );
		Sheep sheep = new Sheep();
		sheep.pos = home;
		Actor.add( sheep );
		PortalGate gate = new PortalGate();
		gate.pos = stray;

		gate.returnToShop();
		assertEquals( "nobody is pushed off its cell", stray, gate.pos );
	}

	@Test
	public void theTownGateStaysWhereTheSurfaceHasIt(){
		floor();
		Dungeon.depth = OverworldLevel.DEPTH;
		Dungeon.branch = 0;
		Portals.register( Portals.TOWN, 3 + 3 * W );
		PortalGate gate = new PortalGate();
		gate.pos = 8 + 7 * W;

		gate.returnToShop();
		assertEquals( 8 + 7 * W, gate.pos );
	}

	//an open floor walled round
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
	}
}
